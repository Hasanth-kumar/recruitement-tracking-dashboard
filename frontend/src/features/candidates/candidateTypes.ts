// Shared types for candidates — `RecruitmentStage` matches backend enum
// com.rts.shared.kernel.RecruitmentStage

export type RecruitmentStage =
  | 'APPLICATION_RECEIVED'
  | 'SCREENING'
  | 'SHORTLISTED'
  | 'R1_SCHEDULED'
  | 'R1_CLEARED'
  | 'R2_SCHEDULED'
  | 'R2_CLEARED'
  | 'OFFERED'
  | 'HIRED'
  | 'REJECTED';

export const STAGE_LABELS: Record<RecruitmentStage, string> = {
  APPLICATION_RECEIVED: 'Application Received',
  SCREENING: 'Screening',
  SHORTLISTED: 'Shortlisted',
  R1_SCHEDULED: 'Round 1 Scheduled',
  R1_CLEARED: 'Round 1 Cleared',
  R2_SCHEDULED: 'Round 2 Scheduled',
  R2_CLEARED: 'Round 2 Cleared',
  OFFERED: 'Offered',
  HIRED: 'Hired',
  REJECTED: 'Rejected',
};

export const STAGE_COLORS: Record<RecruitmentStage, string> = {
  APPLICATION_RECEIVED: '#6b7280',
  SCREENING: '#2563eb',
  SHORTLISTED: '#7c3aed',
  R1_SCHEDULED: '#d97706',
  R1_CLEARED: '#0891b2',
  R2_SCHEDULED: '#ea580c',
  R2_CLEARED: '#ca8a04',
  OFFERED: '#16a34a',
  HIRED: '#15803d',
  REJECTED: '#dc2626',
};

export const POSITIONS = [
  'Frontend Developer',
  'Backend Developer',
  'Full Stack Developer',
  'UI/UX Designer',
  'DevOps Engineer',
  'Data Analyst',
  'Project Manager',
  'Business Analyst',
  'QA Engineer',
  'Mobile Developer',
  'Other',
];

export const EXPERIENCE_LEVELS = [
  'Fresher (0 years)',
  '1 - 2 years',
  '3 - 5 years',
  '6 - 10 years',
  '10+ years',
];

export interface Candidate {
  id: string;
  name: string;
  email: string;
  phone: string;
  position: string;
  experience: string;
  stage: RecruitmentStage;
  notes?: string;
  resumeUrl?: string;
  photoUrl?: string;
  /** From API — use with AuthenticatedCandidateAvatar */
  hasPhoto?: boolean;
  hasResume?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CandidateFormValues {
  name: string;
  email: string;
  phone: string;
  position: string;
  experience: string;
  notes: string;
}
