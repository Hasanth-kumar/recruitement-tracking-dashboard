import type {
  Interview,
  ScheduleRound1Dto,
  ScheduleRound2Dto,
  RescheduleDto,
  CancelDto,
  Interviewer,
  InterviewPhoto,
} from './interviewTypes';

const STORAGE_KEY = 'rts_interviews';
const PHOTO_STORAGE_KEY = 'rts_interview_photos';

export const MOCK_INTERVIEWERS: Interviewer[] = [
  { id: 'usr-001', username: 'alice_hr', email: 'alice@rts.com' },
  { id: 'usr-002', username: 'bob_tech', email: 'bob@rts.com' },
  { id: 'usr-003', username: 'carol_lead', email: 'carol@rts.com' },
  { id: 'usr-004', username: 'david_senior', email: 'david@rts.com' },
];

const SEED_INTERVIEWS: Interview[] = [
  {
    id: 'int-001',
    candidateId: 'cand-001',
    candidateName: 'Aditya Sharma',
    candidatePosition: 'Frontend Developer',
    round: 'ROUND_1',
    scheduledAt: '2026-05-15T10:00:00',
    interviewers: [MOCK_INTERVIEWERS[0], MOCK_INTERVIEWERS[1]],
    meetingLink: 'https://meet.google.com/abc-defg-hij',
    duration: 60,
    notes: 'Focus on React and TypeScript proficiency.',
    status: 'SCHEDULED',
    createdAt: '2026-05-10T08:00:00',
    updatedAt: '2026-05-10T08:00:00',
  },
  {
    id: 'int-002',
    candidateId: 'cand-002',
    candidateName: 'Priya Nair',
    candidatePosition: 'Backend Developer',
    round: 'ROUND_1',
    scheduledAt: '2026-05-16T14:00:00',
    interviewers: [MOCK_INTERVIEWERS[2]],
    meetingLink: 'https://zoom.us/j/123456789',
    duration: 45,
    status: 'COMPLETED',
    createdAt: '2026-05-09T09:00:00',
    updatedAt: '2026-05-09T09:00:00',
  },
  {
    id: 'int-003',
    candidateId: 'cand-002',
    candidateName: 'Priya Nair',
    candidatePosition: 'Backend Developer',
    round: 'ROUND_2',
    scheduledAt: '2026-05-20T11:00:00',
    interviewers: [MOCK_INTERVIEWERS[2], MOCK_INTERVIEWERS[3]],
    location: 'Conference Room B, 3rd Floor',
    duration: 60,
    notes: 'System design discussion.',
    status: 'SCHEDULED',
    createdAt: '2026-05-11T10:00:00',
    updatedAt: '2026-05-11T10:00:00',
  },
];

function loadInterviews(): Interview[] {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (raw) return JSON.parse(raw) as Interview[];
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(SEED_INTERVIEWS));
  return SEED_INTERVIEWS;
}

function saveInterviews(data: Interview[]): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

function loadPhotos(): InterviewPhoto[] {
  const raw = sessionStorage.getItem(PHOTO_STORAGE_KEY);
  return raw ? (JSON.parse(raw) as InterviewPhoto[]) : [];
}

function savePhotos(data: InterviewPhoto[]): void {
  sessionStorage.setItem(PHOTO_STORAGE_KEY, JSON.stringify(data));
}

function generateId(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
}

function now(): string {
  return new Date().toISOString();
}

export function mockGetAllInterviews(): Interview[] {
  return loadInterviews();
}

export function mockGetInterviewById(id: string): Interview | null {
  return loadInterviews().find(i => i.id === id) ?? null;
}

export function mockGetInterviewsByCandidate(candidateId: string): Interview[] {
  return loadInterviews().filter(i => i.candidateId === candidateId);
}

export function mockScheduleRound1(
  dto: ScheduleRound1Dto,
  candidateName: string,
  candidatePosition: string,
): Interview {
  const interviewers = MOCK_INTERVIEWERS.filter(u =>
    dto.interviewerUsernames.includes(u.username),
  );
  const interview: Interview = {
    id: generateId('int'),
    candidateId: dto.candidateId,
    candidateName,
    candidatePosition,
    round: 'ROUND_1',
    scheduledAt: dto.scheduledAt,
    interviewers,
    meetingLink: dto.meetingLink,
    duration: dto.duration,
    notes: dto.notes,
    status: 'SCHEDULED',
    createdAt: now(),
    updatedAt: now(),
  };
  const all = loadInterviews();
  all.push(interview);
  saveInterviews(all);
  return interview;
}

export function mockScheduleRound2(
  dto: ScheduleRound2Dto,
  candidateName: string,
  candidatePosition: string,
): Interview {
  const interviewers = MOCK_INTERVIEWERS.filter(u =>
    dto.interviewerUsernames.includes(u.username),
  );
  const interview: Interview = {
    id: generateId('int'),
    candidateId: dto.candidateId,
    candidateName,
    candidatePosition,
    round: 'ROUND_2',
    scheduledAt: dto.scheduledAt,
    interviewers,
    location: dto.location,
    duration: dto.duration,
    notes: dto.notes,
    status: 'SCHEDULED',
    createdAt: now(),
    updatedAt: now(),
  };
  const all = loadInterviews();
  all.push(interview);
  saveInterviews(all);
  return interview;
}

export function mockRescheduleInterview(
  id: string,
  dto: RescheduleDto,
): Interview | null {
  const all = loadInterviews();
  const idx = all.findIndex(i => i.id === id);
  if (idx === -1) return null;
  all[idx] = {
    ...all[idx],
    scheduledAt: dto.scheduledAt,
    status: 'RESCHEDULED',
    updatedAt: now(),
  };
  saveInterviews(all);
  return all[idx];
}

export function mockCancelInterview(
  id: string,
  _dto: CancelDto,
): Interview | null {
  const all = loadInterviews();
  const idx = all.findIndex(i => i.id === id);
  if (idx === -1) return null;
  all[idx] = { ...all[idx], status: 'CANCELLED', updatedAt: now() };
  saveInterviews(all);
  return all[idx];
}

export function mockGetPhotosByInterview(interviewId: string): InterviewPhoto[] {
  return loadPhotos().filter(p => p.interviewId === interviewId);
}

export function mockUploadPhoto(
  interviewId: string,
  caption?: string,
): InterviewPhoto {
  const photo: InterviewPhoto = {
    id: generateId('photo'),
    interviewId,
    filePath: `/uploads/interview-photos/${interviewId}-${Date.now()}.jpg`,
    caption,
    uploadedAt: now(),
  };
  const all = loadPhotos();
  all.push(photo);
  savePhotos(all);
  return photo;
}

export function mockDeletePhoto(photoId: string): boolean {
  const all = loadPhotos();
  const filtered = all.filter(p => p.id !== photoId);
  if (filtered.length === all.length) return false;
  savePhotos(filtered);
  return true;
}
