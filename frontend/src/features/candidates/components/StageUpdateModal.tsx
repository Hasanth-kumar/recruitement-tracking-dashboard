// src/features/candidates/components/StageUpdateModal.tsx
// US-8: Update single candidate stage with confirmation

import React, { useState, useEffect } from 'react';
import { Modal, Select, Alert } from 'antd';
import { Candidate, RecruitmentStage, STAGE_LABELS } from '../candidateTypes';
import StatusBadge from '../../../shared/components/StatusBadge';

interface Props {
 candidate: Candidate | null;     // null = modal closed
 onConfirm: (candidateId: string, newStage: RecruitmentStage) => void;
 onClose:   () => void;
 loading?:  boolean;
}

const ALL_STAGES = Object.entries(STAGE_LABELS).map(([value, label]) => ({
 value: value as RecruitmentStage,
 label,
}));

const StageUpdateModal: React.FC<Props> = ({ candidate, onConfirm, onClose, loading }) => {
 const [selectedStage, setSelectedStage] = useState<RecruitmentStage | null>(null);

 // Reset selection whenever a new candidate is opened
 useEffect(() => {
   if (candidate) setSelectedStage(candidate.stage);
 }, [candidate]);

 const unchanged = selectedStage === candidate?.stage;

 const handleOk = () => {
   if (candidate && selectedStage && !unchanged) {
     onConfirm(candidate.id, selectedStage);
   }
 };

 return (
   <Modal
     open={Boolean(candidate)}
     title="Update recruitment stage"
     okText="Update stage"
     cancelText="Cancel"
     okButtonProps={{ disabled: !selectedStage || unchanged, loading }}
     onOk={handleOk}
     onCancel={onClose}
     width={420}
     destroyOnClose
   >
     {candidate && (
       <div style={{ fontFamily: "'IBM Plex Sans', sans-serif" }}>
         {/* Candidate info */}
         <div style={{
           display:      'flex',
           alignItems:   'center',
           gap:          10,
           padding:      '10px 12px',
          background:   'var(--bg)',
           borderRadius: 8,
           marginBottom: '1.25rem',
         }}>
           <div style={{
             width: 36, height: 36, borderRadius: '50%',
            background: 'var(--accent-subtle)', display: 'flex', alignItems: 'center',
            justifyContent: 'center', fontWeight: 600, color: 'var(--accent)',
             fontSize: '0.85rem', flexShrink: 0,
           }}>
             {candidate.name[0].toUpperCase()}
           </div>
           <div>
            <p style={{ margin: 0, fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
               {candidate.name}
             </p>
            <p style={{ margin: 0, fontSize: '0.775rem', color: 'var(--text-muted)' }}>
               {candidate.position}
             </p>
           </div>
         </div>

         {/* Current → New */}
         <div style={{ marginBottom: '1rem' }}>
          <p style={{ fontSize: '0.78rem', fontWeight: 500, color: 'var(--text-secondary)', marginBottom: 6 }}>
             Current stage
           </p>
           <StatusBadge stage={candidate.stage} />
         </div>

         <div style={{ marginBottom: '1rem' }}>
          <p style={{ fontSize: '0.78rem', fontWeight: 500, color: 'var(--text-secondary)', marginBottom: 6 }}>
             New stage
           </p>
           <Select
             value={selectedStage ?? undefined}
             onChange={val => setSelectedStage(val)}
             style={{ width: '100%' }}
             size="large"
             options={ALL_STAGES}
           />
         </div>

         {unchanged && selectedStage && (
           <Alert
             type="info"
             message="This is already the current stage. Select a different one to update."
             showIcon
             style={{ borderRadius: 8, fontSize: '0.825rem' }}
           />
         )}
       </div>
     )}
   </Modal>
 );
};

export default StageUpdateModal;
 