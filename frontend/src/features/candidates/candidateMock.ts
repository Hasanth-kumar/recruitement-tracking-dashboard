import { Candidate, RecruitmentStage } from './candidateTypes';

const STORAGE_KEY = 'rts_mock_candidates';

function loadStore(): Candidate[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as Candidate[];
  } catch { /* ignore */ }
  return INITIAL_DATA;
}

function saveStore(candidates: Candidate[]) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(candidates));
}

function generateId(): string {
  return 'RTS-' + Math.random().toString(36).substring(2, 8).toUpperCase();
}

// ── Initial seed data ──────────────────────────────────────────
const INITIAL_DATA: Candidate[] = [
  {
    id: 'RTS-SEED01',
    name: 'Alice Johnson',
    email: 'alice@example.com',
    phone: '+1 555 100 0001',
    position: 'Frontend Developer',
    experience: '3 - 5 years',
    stage: 'R1_CLEARED',          // eligible for Round 1 feedback
    notes: 'Strong React skills. Referred by HR.',
    createdAt: new Date(Date.now() - 5 * 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 5 * 86400000).toISOString(),
  },
  {
    id: 'RTS-SEED02',
    name: 'Bob Martinez',
    email: 'bob@example.com',
    phone: '+1 555 100 0002',
    position: 'Backend Developer',
    experience: '6 - 10 years',
    stage: 'R2_CLEARED',          // eligible for Round 2 feedback
    notes: 'Java Spring Boot expert.',
    createdAt: new Date(Date.now() - 3 * 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 3 * 86400000).toISOString(),
  },
  {
    id: 'RTS-SEED03',
    name: 'Clara Wong',
    email: 'clara@example.com',
    phone: '+1 555 100 0003',
    position: 'UI/UX Designer',
    experience: '1 - 2 years',
    stage: 'APPLICATION_RECEIVED', // not eligible — will not appear in feedback list
    createdAt: new Date(Date.now() - 1 * 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 1 * 86400000).toISOString(),
  },
];

// ── CRUD operations ────────────────────────────────────────────

export function mockGetAllCandidates(): Candidate[] {
  return loadStore();
}

export function mockGetCandidate(id: string): Candidate | null {
  return loadStore().find(c => c.id === id) ?? null;
}

export function mockCreateCandidate(
  values: Omit<Candidate, 'id' | 'stage' | 'createdAt' | 'updatedAt'>,
): Candidate {
  const now = new Date().toISOString();
  const newCandidate: Candidate = {
    ...values,
    id: generateId(),
    stage: 'APPLICATION_RECEIVED' as RecruitmentStage,
    createdAt: now,
    updatedAt: now,
  };
  const store = loadStore();
  store.unshift(newCandidate);
  saveStore(store);
  return newCandidate;
}

export function mockUpdateCandidate(id: string, values: Partial<Candidate>): Candidate {
  const store = loadStore();
  const idx = store.findIndex(c => c.id === id);
  if (idx === -1) throw new Error(`Candidate ${id} not found.`);
  store[idx] = { ...store[idx], ...values, updatedAt: new Date().toISOString() };
  saveStore(store);
  return store[idx];
}

export function mockDeleteCandidate(id: string): void {
  const store = loadStore().filter(c => c.id !== id);
  saveStore(store);
}

export function mockUpdateCandidatePhoto(id: string, photoUrl: string): void {
  mockUpdateCandidate(id, { photoUrl });
}

export function mockDeleteCandidatePhoto(id: string): void {
  mockUpdateCandidate(id, { photoUrl: undefined, hasPhoto: false });
}

export function mockUpdateCandidateResume(id: string, resumeUrl: string): void {
  mockUpdateCandidate(id, { resumeUrl });
}