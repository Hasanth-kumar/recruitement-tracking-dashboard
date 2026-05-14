import type {
  Interview,
  InterviewDuration,
  InterviewPhoto,
  InterviewRound,
  InterviewStatus,
  Interviewer,
} from './interviewTypes';

function parseApiDateTime(value: unknown): string {
  if (value == null) return new Date().toISOString();
  if (typeof value === 'string') {
    if (value.length === 16 && value.includes('T')) return `${value}:00`;
    return value;
  }
  if (Array.isArray(value) && value.length >= 6) {
    const [y, m, d, h, min, s] = value.map(Number);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${y}-${pad(m)}-${pad(d)}T${pad(h)}:${pad(min)}:${pad(s ?? 0)}`;
  }
  return String(value);
}

function asInterviewDuration(n: unknown): InterviewDuration {
  const v = Number(n);
  if (v === 30 || v === 45 || v === 60) return v;
  return 60;
}

function asInterviewRound(v: unknown): InterviewRound {
  const s = String(v);
  return s === 'ROUND_2' ? 'ROUND_2' : 'ROUND_1';
}

function asInterviewStatus(v: unknown): InterviewStatus {
  const s = String(v);
  if (s === 'COMPLETED' || s === 'CANCELLED' || s === 'RESCHEDULED') return s;
  return 'SCHEDULED';
}

export function mapApiUserToInterviewer(raw: Record<string, unknown>): Interviewer {
  return {
    id: String(raw.id ?? raw.username),
    username: String(raw.username),
    email: String(raw.email ?? ''),
  };
}

export function mapInterviewApiRow(
  raw: Record<string, unknown>,
  candidateLookup: Record<string, { name: string; position: string }>,
): Interview {
  const candidateId = String(raw.candidateId);
  const cand = candidateLookup[candidateId] ?? { name: 'Unknown candidate', position: '—' };
  const names = (raw.interviewerUsernames as unknown[]) ?? [];
  const interviewers: Interviewer[] = names.map(u => ({
    id: String(u),
    username: String(u),
    email: '',
  }));
  const now = new Date().toISOString();
  return {
    id: String(raw.id),
    candidateId,
    candidateName: cand.name,
    candidatePosition: cand.position,
    round: asInterviewRound(raw.round),
    scheduledAt: parseApiDateTime(raw.dateTime),
    interviewers,
    meetingLink: raw.meetingLink != null ? String(raw.meetingLink) : undefined,
    location: raw.location != null ? String(raw.location) : undefined,
    duration: asInterviewDuration(raw.durationMinutes),
    notes: raw.notes != null && String(raw.notes) !== '' ? String(raw.notes) : undefined,
    status: asInterviewStatus(raw.status),
    createdAt: now,
    updatedAt: now,
  };
}

export function mapInterviewPhotoApiRow(raw: Record<string, unknown>): InterviewPhoto {
  return {
    id: String(raw.id),
    interviewId: String(raw.interviewId),
    filePath: String(raw.originalFileName ?? raw.id),
    caption: raw.caption != null && String(raw.caption) !== '' ? String(raw.caption) : undefined,
    uploadedAt: parseApiDateTime(raw.uploadedAt),
  };
}

export function buildScheduleQueryParams(interviewerUsername?: string): Record<string, string> {
  const from = new Date();
  from.setFullYear(from.getFullYear() - 1);
  const to = new Date();
  to.setFullYear(to.getFullYear() + 2);
  const pad = (n: number) => String(n).padStart(2, '0');
  const fmt = (d: Date) =>
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
  const p: Record<string, string> = {
    fromDateTime: fmt(from),
    toDateTime: fmt(to),
  };
  if (interviewerUsername) p.interviewerUsername = interviewerUsername;
  return p;
}

export function scheduledAtToLocalDateTime(scheduledAt: string): string {
  if (scheduledAt.length === 16 && scheduledAt.includes('T')) return `${scheduledAt}:00`;
  if (scheduledAt.includes('.')) return scheduledAt.split('.')[0];
  return scheduledAt;
}
