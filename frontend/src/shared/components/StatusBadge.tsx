// src/shared/components/StatusBadge.tsx
// Reusable stage badge — used in CandidateTable, CandidateDetailPage, StageTimeline

import React from 'react';
import { RecruitmentStage, STAGE_LABELS, STAGE_COLORS } from '../../features/candidates/candidateTypes';

interface Props {
 stage: RecruitmentStage;
 size?: 'sm' | 'md';
}

// Light background version of each stage color
const STAGE_BG: Record<RecruitmentStage, string> = {
 APPLICATION_RECEIVED: '#f3f4f6',
 RESUME_SCREENING:     '#eff4ff',
 ROUND_1_SCHEDULED:    '#f5f3ff',
 ROUND_1_COMPLETED:    '#ecfeff',
 ROUND_2_SCHEDULED:    '#fffbeb',
 ROUND_2_COMPLETED:    '#fff7ed',
 FINAL_SELECTION:      '#f0fdf4',
 REJECTED:             '#fef2f2',
 ON_HOLD:              '#f9fafb',
};

const StatusBadge: React.FC<Props> = ({ stage, size = 'md' }) => {
 const color = STAGE_COLORS[stage];
 const bg    = STAGE_BG[stage];
 const label = STAGE_LABELS[stage];

 return (
   <span style={{
     display:        'inline-block',
     padding:        size === 'sm' ? '1px 8px' : '3px 10px',
     borderRadius:   99,
     fontSize:       size === 'sm' ? '0.7rem' : '0.75rem',
     fontWeight:     500,
     fontFamily:     "'IBM Plex Sans', sans-serif",
     color,
     background:     bg,
     border:         `1px solid ${color}22`,
     whiteSpace:     'nowrap' as const,
     letterSpacing:  '0.01em',
   }}>
     {label}
   </span>
 );
};

export default StatusBadge;
 