// src/features/candidates/components/BulkStageModal.tsx
// US-10: Bulk stage update — select multiple candidates, choose target stage

import React, { useState } from 'react';
import { Modal, Select, Alert } from 'antd';
import { Candidate, RecruitmentStage, STAGE_LABELS } from '../candidateTypes';
import StatusBadge from '../../../shared/components/StatusBadge';

interface Props {
 candidates: Candidate[];       // only the selected candidates
 open:       boolean;
 onConfirm:  (ids: string[], newStage: RecruitmentStage) => void;
 onClose:    () => void;
 loading?:   boolean;
}

const ALL_STAGES = Object.entries(STAGE_LABELS).map(([value, label]) => ({
 value: value as RecruitmentStage,
 label,
}));

const BulkStageModal: React.FC<Props> = ({ candidates, open, onConfirm, onClose, loading }) => {
 const [selectedStage, setSelectedStage] = useState<RecruitmentStage | null>(null);

 const handleClose = () => {
   setSelectedStage(null);
   onClose();
 };

 const handleOk = () => {
   if (selectedStage && candidates.length > 0) {
     onConfirm(candidates.map(c => c.id), selectedStage);
     setSelectedStage(null);
   }
 };

 return (
   <Modal
     open={open}
     title="Bulk stage update"
     okText={`Update ${candidates.length} candidate${candidates.length !== 1 ? 's' : ''}`}
     cancelText="Cancel"
     okButtonProps={{ disabled: !selectedStage || candidates.length === 0, loading }}
     onOk={handleOk}
     onCancel={handleClose}
     width={460}
     destroyOnClose
   >
     <div style={{ fontFamily: "'IBM Plex Sans', sans-serif" }}>

       {/* Count summary */}
       <div style={{
         padding:      '10px 14px',
         background:   '#eff4ff',
         border:       '1px solid #bfdbfe',
         borderRadius: 8,
         marginBottom: '1.25rem',
         fontSize:     '0.875rem',
         color:        '#2563eb',
         fontWeight:   500,
       }}>
         {candidates.length} candidate{candidates.length !== 1 ? 's' : ''} selected
       </div>

       {/* Candidate list preview — max 5 shown */}
       <div style={{ marginBottom: '1.25rem' }}>
         <p style={{ fontSize: '0.78rem', fontWeight: 500, color: '#6b6b65', marginBottom: 8 }}>
           Selected candidates
         </p>
         <div style={{
           maxHeight:    160,
           overflowY:    'auto',
           border:       '1px solid #e4e4e0',
           borderRadius: 8,
         }}>
           {candidates.map((c, i) => (
             <div
               key={c.id}
               style={{
                 display:      'flex',
                 alignItems:   'center',
                 justifyContent: 'space-between',
                 padding:      '8px 12px',
                 borderBottom: i < candidates.length - 1 ? '1px solid #f0f0ed' : 'none',
                 fontSize:     '0.85rem',
               }}
             >
               <div>
                 <span style={{ fontWeight: 500, color: '#1a1a18' }}>{c.name}</span>
                 <span style={{ color: '#b0b0a8', marginLeft: 8, fontSize: '0.775rem' }}>
                   {c.position}
                 </span>
               </div>
               <StatusBadge stage={c.stage} size="sm" />
             </div>
           ))}
         </div>
       </div>

       {/* Target stage select */}
       <div style={{ marginBottom: '1rem' }}>
         <p style={{ fontSize: '0.78rem', fontWeight: 500, color: '#6b6b65', marginBottom: 6 }}>
           Move all to stage
         </p>
         <Select
           placeholder="Select target stage"
           value={selectedStage ?? undefined}
           onChange={val => setSelectedStage(val)}
           style={{ width: '100%' }}
           size="large"
           options={ALL_STAGES}
         />
       </div>

       {candidates.length === 0 && (
         <Alert
           type="warning"
           message="No candidates selected. Please select at least one from the list."
           showIcon
           style={{ borderRadius: 8, fontSize: '0.825rem' }}
         />
       )}
     </div>
   </Modal>
 );
};

export default BulkStageModal;
