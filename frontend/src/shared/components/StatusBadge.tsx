// Reusable stage badge — used in CandidateTable, StageTimeline

import React from 'react';
import { RecruitmentStage, STAGE_LABELS, STAGE_COLORS } from '../../features/candidates/candidateTypes';

interface Props {
  stage: RecruitmentStage;
  size?: 'sm' | 'md';
}

const STAGE_BG: Record<RecruitmentStage, string> = {
  APPLICATION_RECEIVED: '#f3f4f6',
  SCREENING: '#eff4ff',
  SHORTLISTED: '#f5f3ff',
  R1_SCHEDULED: '#fffbeb',
  R1_CLEARED: '#ecfeff',
  R2_SCHEDULED: '#fff7ed',
  R2_CLEARED: '#fefce8',
  OFFERED: '#f0fdf4',
  HIRED: '#dcfce7',
  REJECTED: '#fef2f2',
};

const StatusBadge: React.FC<Props> = ({ stage, size = 'md' }) => {
  const color = STAGE_COLORS[stage] ?? '#6b7280';
  const bg = STAGE_BG[stage] ?? '#f3f4f6';
  const label = STAGE_LABELS[stage] ?? String(stage).replace(/_/g, ' ');

  return (
    <span
      style={{
        display: 'inline-block',
        padding: size === 'sm' ? '1px 8px' : '3px 10px',
        borderRadius: 99,
        fontSize: size === 'sm' ? '0.7rem' : '0.75rem',
        fontWeight: 500,
        fontFamily: "'IBM Plex Sans', sans-serif",
        color,
        background: bg,
        border: `1px solid ${color}22`,
        whiteSpace: 'nowrap' as const,
        letterSpacing: '0.01em',
      }}
    >
      {label}
    </span>
  );
};

export default StatusBadge;
