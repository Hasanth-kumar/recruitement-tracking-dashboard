// src/features/candidates/components/CandidateFilterPanel.tsx
// US-4: Filter by stage, position, date range

import React from 'react';
import { Select, Button } from 'antd';
import { FilterOutlined, CloseOutlined } from '@ant-design/icons';
import {
 RecruitmentStage,
 STAGE_LABELS,
 POSITIONS,
} from '../candidateTypes';

export interface FilterState {
 stage:     RecruitmentStage | '';
 position:  string;
 dateFrom:  string;
 dateTo:    string;
}

export const EMPTY_FILTER: FilterState = {
 stage:    '',
 position: '',
 dateFrom: '',
 dateTo:   '',
};

interface Props {
 filters:   FilterState;
 onChange:  (filters: FilterState) => void;
 onReset:   () => void;
 activeCount: number;   // number of active filters — shows badge
}

const ALL_STAGES = Object.entries(STAGE_LABELS).map(([value, label]) => ({
 value,
 label,
}));

const s = {
 wrap:      { display: 'flex' as const, alignItems: 'center' as const, gap: 8, flexWrap: 'wrap' as const },
 label:     { fontSize: '0.78rem', fontWeight: 500 as const, color: '#6b6b65', whiteSpace: 'nowrap' as const },
 badge:     {
   display:        'inline-flex' as const,
   alignItems:     'center' as const,
   justifyContent: 'center' as const,
   width:          18,
   height:         18,
   borderRadius:   '50%',
   background:     '#2563eb',
   color:          '#fff',
   fontSize:       '0.65rem',
   fontWeight:     600 as const,
   marginLeft:     4,
 },
};

const CandidateFilterPanel: React.FC<Props> = ({ filters, onChange, onReset, activeCount }) => {
 const set = (key: keyof FilterState, value: string) =>
   onChange({ ...filters, [key]: value });

 return (
   <div style={s.wrap}>
     <span style={s.label}>
       <FilterOutlined style={{ marginRight: 4 }} />
       Filters
       {activeCount > 0 && <span style={s.badge}>{activeCount}</span>}
     </span>

     {/* Stage filter */}
     <Select
       placeholder="All stages"
       value={filters.stage || undefined}
       onChange={val => set('stage', val ?? '')}
       allowClear
       onClear={() => set('stage', '')}
       style={{ width: 180, fontSize: '0.875rem' }}
       size="middle"
       options={ALL_STAGES}
     />

     {/* Position filter */}
     <Select
       placeholder="All positions"
       value={filters.position || undefined}
       onChange={val => set('position', val ?? '')}
       allowClear
       onClear={() => set('position', '')}
       style={{ width: 180, fontSize: '0.875rem' }}
       size="middle"
       options={POSITIONS.map(p => ({ value: p, label: p }))}
     />

     {/* Date from */}
     <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
       <span style={{ fontSize: '0.78rem', color: '#b0b0a8' }}>From</span>
       <input
         type="date"
         value={filters.dateFrom}
         onChange={e => set('dateFrom', e.target.value)}
         style={{
           border:       '1px solid #e4e4e0',
           borderRadius: 8,
           padding:      '4px 8px',
           fontSize:     '0.825rem',
           fontFamily:   "'IBM Plex Sans', sans-serif",
           color:        '#1a1a18',
           background:   '#fff',
           cursor:       'pointer',
           outline:      'none',
         }}
       />
     </div>

     {/* Date to */}
     <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
       <span style={{ fontSize: '0.78rem', color: '#b0b0a8' }}>To</span>
       <input
         type="date"
         value={filters.dateTo}
         onChange={e => set('dateTo', e.target.value)}
         style={{
           border:       '1px solid #e4e4e0',
           borderRadius: 8,
           padding:      '4px 8px',
           fontSize:     '0.825rem',
           fontFamily:   "'IBM Plex Sans', sans-serif",
           color:        '#1a1a18',
           background:   '#fff',
           cursor:       'pointer',
           outline:      'none',
         }}
       />
     </div>

     {/* Reset */}
     {activeCount > 0 && (
       <Button
         size="small"
         icon={<CloseOutlined />}
         onClick={onReset}
         style={{
           fontSize:     '0.775rem',
           color:        '#6b6b65',
           border:       '1px solid #e4e4e0',
           borderRadius: 6,
           height:       30,
         }}
       >
         Reset
       </Button>
     )}
   </div>
 );
};

export default CandidateFilterPanel;
 