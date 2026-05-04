// src/features/candidates/components/CandidateTable.tsx
// US-4: Paginated candidate table — sortable by name, date, position, stage
// US-7: Delete candidate with confirmation dialog

import React, { useState } from 'react';
import { Button, Modal } from 'antd';
import {
 ArrowUpOutlined,
 ArrowDownOutlined,
 EditOutlined,
 DeleteOutlined,
 EyeOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { Candidate } from '../candidateTypes';
import StatusBadge from '../../../shared/components/StatusBadge';

// ── Types ───────────────────────────────────────────────────────
export type SortField = 'name' | 'position' | 'stage' | 'createdAt';
export type SortDir   = 'asc' | 'desc';

export interface SortState {
 field: SortField;
 dir:   SortDir;
}

interface Props {
 candidates:     Candidate[];
 sortState:      SortState;
 onSortChange:   (field: SortField) => void;
 onDelete:       (id: string) => void;
 onStageUpdate:  (candidate: Candidate) => void;
 selectedIds:    string[];
 onSelectChange: (ids: string[]) => void;
}

// ── Helpers ─────────────────────────────────────────────────────
function formatDate(iso: string): string {
 return new Date(iso).toLocaleDateString('en-US', {
   day:   '2-digit',
   month: 'short',
   year:  'numeric',
 });
}

// ── Styles ──────────────────────────────────────────────────────
const s = {
 wrap:       { background: '#fff', border: '1px solid #e4e4e0', borderRadius: 12, overflow: 'hidden' as const },
 table:      { width: '100%', borderCollapse: 'collapse' as const, fontSize: '0.875rem', fontFamily: "'IBM Plex Sans', sans-serif" },
 thead:      { background: '#f9f9f8', borderBottom: '1px solid #e4e4e0' },
 th:         { padding: '10px 14px', textAlign: 'left' as const, fontSize: '0.72rem', fontWeight: 600 as const, color: '#6b6b65', letterSpacing: '0.06em', textTransform: 'uppercase' as const, whiteSpace: 'nowrap' as const },
 thSort:     { cursor: 'pointer' as const, userSelect: 'none' as const, padding: '10px 14px', textAlign: 'left' as const, fontSize: '0.72rem', fontWeight: 600 as const, color: '#6b6b65', letterSpacing: '0.06em', textTransform: 'uppercase' as const, whiteSpace: 'nowrap' as const },
 thSortActive: { color: '#2563eb' },
 td:         { padding: '12px 14px', verticalAlign: 'middle' as const, borderBottom: '1px solid #f0f0ed' },
 tdLast:     { padding: '12px 14px', verticalAlign: 'middle' as const },
 nameWrap:   { display: 'flex' as const, alignItems: 'center' as const, gap: 10 },
 avatar:     { width: 34, height: 34, borderRadius: '50%', objectFit: 'cover' as const, flexShrink: 0 },
 avatarFb:   { width: 34, height: 34, borderRadius: '50%', background: '#eff4ff', display: 'flex' as const, alignItems: 'center' as const, justifyContent: 'center' as const, fontSize: '0.8rem', fontWeight: 600 as const, color: '#2563eb', flexShrink: 0 },
 nameText:   { fontWeight: 500 as const, color: '#1a1a18', margin: 0, lineHeight: 1.3 },
 emailText:  { fontSize: '0.75rem', color: '#b0b0a8', margin: 0 },
 bodyText:   { color: '#6b6b65', margin: 0 },
 actions:    { display: 'flex' as const, gap: 4, alignItems: 'center' as const, justifyContent: 'flex-end' as const },
 emptyRow:   { textAlign: 'center' as const, padding: '3rem', color: '#b0b0a8', fontSize: '0.875rem' },
 checkbox:   { width: 15, height: 15, cursor: 'pointer' as const, accentColor: '#2563eb' },
};

const SortIcon: React.FC<{ field: SortField; sortState: SortState }> = ({ field, sortState }) => {
 if (sortState.field !== field) return <span style={{ color: '#d1d5db', marginLeft: 4 }}>↕</span>;
 return sortState.dir === 'asc'
   ? <ArrowUpOutlined style={{ color: '#2563eb', marginLeft: 4, fontSize: '0.7rem' }} />
   : <ArrowDownOutlined style={{ color: '#2563eb', marginLeft: 4, fontSize: '0.7rem' }} />;
};

// ── Component ───────────────────────────────────────────────────
const CandidateTable: React.FC<Props> = ({
 candidates,
 sortState,
 onSortChange,
 onDelete,
 onStageUpdate,
 selectedIds,
 onSelectChange,
}) => {
 const navigate = useNavigate();
 const [deleteTarget, setDeleteTarget] = useState<Candidate | null>(null);

 const allSelected  = candidates.length > 0 && candidates.every(c => selectedIds.includes(c.id));
 const someSelected = candidates.some(c => selectedIds.includes(c.id));

 const toggleAll = () => {
   if (allSelected) onSelectChange([]);
   else             onSelectChange(candidates.map(c => c.id));
 };

 const toggleOne = (id: string) => {
   if (selectedIds.includes(id)) onSelectChange(selectedIds.filter(x => x !== id));
   else                          onSelectChange([...selectedIds, id]);
 };

 const thStyle = (field: SortField) => ({
   ...(sortState.field === field ? { ...s.thSort, ...s.thSortActive } : s.thSort),
 });

 return (
   <>
     <div style={s.wrap}>
       <table style={s.table}>
         <thead style={s.thead}>
           <tr>
             {/* Checkbox */}
             <th style={{ ...s.th, width: 40, paddingRight: 0 }}>
               <input
                 type="checkbox"
                 style={s.checkbox}
                 checked={allSelected}
                 ref={el => { if (el) el.indeterminate = someSelected && !allSelected; }}
                 onChange={toggleAll}
               />
             </th>

             {/* Sortable headers */}
             <th style={thStyle('name')} onClick={() => onSortChange('name')}>
               Candidate <SortIcon field="name" sortState={sortState} />
             </th>
             <th style={thStyle('position')} onClick={() => onSortChange('position')}>
               Position <SortIcon field="position" sortState={sortState} />
             </th>
             <th style={s.th}>Experience</th>
             <th style={thStyle('stage')} onClick={() => onSortChange('stage')}>
               Stage <SortIcon field="stage" sortState={sortState} />
             </th>
             <th style={thStyle('createdAt')} onClick={() => onSortChange('createdAt')}>
               Applied <SortIcon field="createdAt" sortState={sortState} />
             </th>
             <th style={{ ...s.th, textAlign: 'right' as const }}>Actions</th>
           </tr>
         </thead>

         <tbody>
           {candidates.length === 0 ? (
             <tr>
               <td colSpan={7} style={s.emptyRow}>
                 No candidates found.
               </td>
             </tr>
           ) : (
             candidates.map((c, i) => {
               const isLast     = i === candidates.length - 1;
               const isSelected = selectedIds.includes(c.id);
               const rowTd      = isLast ? s.tdLast : s.td;

               return (
                 <tr
                   key={c.id}
                   style={{ background: isSelected ? '#f8faff' : '#fff', transition: 'background 100ms' }}
                 >
                   {/* Checkbox */}
                   <td style={{ ...rowTd, width: 40, paddingRight: 0 }}>
                     <input
                       type="checkbox"
                       style={s.checkbox}
                       checked={isSelected}
                       onChange={() => toggleOne(c.id)}
                     />
                   </td>

                   {/* Name + email */}
                   <td style={rowTd}>
                     <div style={s.nameWrap}>
                       {c.photoUrl
                         ? <img src={c.photoUrl} alt={c.name} style={s.avatar} />
                         : <div style={s.avatarFb}>{c.name[0].toUpperCase()}</div>
                       }
                       <div>
                         <p style={s.nameText}>{c.name}</p>
                         <p style={s.emailText}>{c.email}</p>
                       </div>
                     </div>
                   </td>

                   {/* Position */}
                   <td style={rowTd}>
                     <p style={s.bodyText}>{c.position}</p>
                   </td>

                   {/* Experience */}
                   <td style={rowTd}>
                     <p style={{ ...s.bodyText, color: '#b0b0a8' }}>{c.experience}</p>
                   </td>

                   {/* Stage badge */}
                   <td style={rowTd}>
                     <StatusBadge stage={c.stage} />
                   </td>

                   {/* Applied date */}
                   <td style={rowTd}>
                     <p style={{ ...s.bodyText, color: '#b0b0a8', fontSize: '0.825rem' }}>
                       {formatDate(c.createdAt)}
                     </p>
                   </td>

                   {/* Actions */}
                   <td style={rowTd}>
                     <div style={s.actions}>
                       {/* View */}
                       <Button
                         type="text"
                         size="small"
                         icon={<EyeOutlined />}
                         title="View details"
                         onClick={() => navigate(`/candidates/${c.id}`)}
                         style={{ color: '#6b6b65' }}
                       />
                       {/* Edit */}
                       <Button
                         type="text"
                         size="small"
                         icon={<EditOutlined />}
                         title="Edit candidate"
                         onClick={() => navigate(`/candidates/${c.id}/edit`)}
                         style={{ color: '#6b6b65' }}
                       />
                       {/* Stage update */}
                       <Button
                         type="text"
                         size="small"
                         title="Update stage"
                         onClick={() => onStageUpdate(c)}
                         style={{ color: '#6b6b65', fontSize: '0.75rem', fontWeight: 500, padding: '0 6px' }}
                       >
                         Stage
                       </Button>
                       {/* Delete */}
                       <Button
                         type="text"
                         size="small"
                         icon={<DeleteOutlined />}
                         title="Delete candidate"
                         onClick={() => setDeleteTarget(c)}
                         style={{ color: '#dc2626' }}
                         danger
                       />
                     </div>
                   </td>
                 </tr>
               );
             })
           )}
         </tbody>
       </table>
     </div>

     {/* Delete confirmation modal — US-7 */}
     <Modal
       open={Boolean(deleteTarget)}
       title="Delete candidate"
       okText="Delete"
       okButtonProps={{ danger: true }}
       cancelText="Cancel"
       onOk={() => {
         if (deleteTarget) {
           onDelete(deleteTarget.id);
           setDeleteTarget(null);
         }
       }}
       onCancel={() => setDeleteTarget(null)}
       width={400}
     >
       <p style={{ fontSize: '0.9rem', color: '#6b6b65', margin: 0 }}>
         Are you sure you want to delete{' '}
         <strong style={{ color: '#1a1a18' }}>{deleteTarget?.name}</strong>?
         This action marks the record as inactive and cannot be undone.
       </p>
     </Modal>
   </>
 );
};

export default CandidateTable;
 