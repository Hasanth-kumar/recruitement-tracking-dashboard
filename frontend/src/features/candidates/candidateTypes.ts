// src/features/candidates/candidateTypes.ts
// Shared types for candidate module — mirrors backend DTOs

export type RecruitmentStage =
 | 'APPLICATION_RECEIVED'
 | 'RESUME_SCREENING'
 | 'ROUND_1_SCHEDULED'
 | 'ROUND_1_COMPLETED'
 | 'ROUND_2_SCHEDULED'
 | 'ROUND_2_COMPLETED'
 | 'FINAL_SELECTION'
 | 'REJECTED'
 | 'ON_HOLD';

export const STAGE_LABELS: Record<RecruitmentStage, string> = {
 APPLICATION_RECEIVED: 'Application Received',
 RESUME_SCREENING:     'Resume Screening',
 ROUND_1_SCHEDULED:    'Round 1 Scheduled',
 ROUND_1_COMPLETED:    'Round 1 Completed',
 ROUND_2_SCHEDULED:    'Round 2 Scheduled',
 ROUND_2_COMPLETED:    'Round 2 Completed',
 FINAL_SELECTION:      'Final Selection',
 REJECTED:             'Rejected',
 ON_HOLD:              'On Hold',
};

export const STAGE_COLORS: Record<RecruitmentStage, string> = {
 APPLICATION_RECEIVED: '#6b7280',
 RESUME_SCREENING:     '#2563eb',
 ROUND_1_SCHEDULED:    '#7c3aed',
 ROUND_1_COMPLETED:    '#0891b2',
 ROUND_2_SCHEDULED:    '#d97706',
 ROUND_2_COMPLETED:    '#ea580c',
 FINAL_SELECTION:      '#16a34a',
 REJECTED:             '#dc2626',
 ON_HOLD:              '#9ca3af',
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
 createdAt: string;
 updatedAt: string;
}

// Form values — used by CandidateFormPage
export interface CandidateFormValues {
 name: string;
 email: string;
 phone: string;
 position: string;
 experience: string;
 notes: string;
}
 