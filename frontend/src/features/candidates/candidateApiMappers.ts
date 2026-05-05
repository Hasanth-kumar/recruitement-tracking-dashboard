import { Candidate, RecruitmentStage } from './candidateTypes';

export interface PagedResponseApi<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

function parseDateTime(v: unknown): string {
  if (typeof v === 'string') return v;
  return new Date().toISOString();
}

/** Maps one Spring `CandidateResponse` JSON row to a UI `Candidate`. */
export function mapApiRowToCandidate(raw: Record<string, unknown>): Candidate {
  return {
    id: String(raw.id),
    name: String(raw.name),
    email: String(raw.email),
    phone: String(raw.phone),
    position: String(raw.position),
    experience: typeof raw.experience === 'string' ? raw.experience : '',
    stage: raw.stage as RecruitmentStage,
    notes: raw.notes != null && raw.notes !== '' ? String(raw.notes) : undefined,
    createdAt: parseDateTime(raw.createdAt),
    updatedAt: parseDateTime(raw.updatedAt),
    hasPhoto: Boolean(raw.hasPhoto),
    hasResume: Boolean(raw.hasResume),
  };
}
