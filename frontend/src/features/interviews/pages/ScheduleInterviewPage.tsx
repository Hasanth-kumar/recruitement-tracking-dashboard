import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { message } from 'antd';
import axiosInstance from '../../../shared/utils/axiosInstance';
import {
  useGetInterviewersQuery,
  useScheduleRound1Mutation,
  useScheduleRound2Mutation,
} from '../interviewApi';
import type { InterviewDuration } from '../interviewTypes';
import type { ScheduleRound1Dto, ScheduleRound2Dto } from '../interviewTypes';
import type { RecruitmentStage } from '../../candidates/candidateTypes';

interface CandidateOption {
  id: string;
  name: string;
  position: string;
  stage: string;
}

function loadCandidateOptions(): CandidateOption[] {
  try {
    const raw = sessionStorage.getItem('rts_candidates');
    if (raw) {
      const all = JSON.parse(raw) as Array<{
        id: string;
        name: string;
        position: string;
        stage: string;
        isDeleted?: boolean;
      }>;
      return all
        .filter(c => !c.isDeleted)
        .map(c => ({ id: c.id, name: c.name, position: c.position, stage: c.stage }));
    }
  } catch {
    /* fallback */
  }
  return [];
}

/** Matches `RecruitmentStage` — cleared Round 1, eligible for Round 2 scheduling */
const ROUND1_CLEARED_STAGES: RecruitmentStage[] = [
  'R1_CLEARED',
  'R2_SCHEDULED',
  'R2_CLEARED',
  'OFFERED',
  'HIRED',
];

type RoundTab = 'round1' | 'round2';
const DURATION_OPTIONS: InterviewDuration[] = [30, 45, 60];

const USE_INTERVIEW_MOCK = import.meta.env.VITE_USE_INTERVIEW_MOCK === 'true';

const ScheduleInterviewPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const defaultTab = (searchParams.get('round') as RoundTab) ?? 'round1';
  const [activeTab, setActiveTab] = useState<RoundTab>(defaultTab);
  const [candidates, setCandidates] = useState<CandidateOption[]>([]);

  const [r1CandidateId, setR1CandidateId] = useState('');
  const [r1DateTime, setR1DateTime] = useState('');
  const [r1InterviewerUsernames, setR1InterviewerUsernames] = useState<string[]>([]);
  const [r1MeetingLink, setR1MeetingLink] = useState('');
  const [r1Duration, setR1Duration] = useState<InterviewDuration>(60);
  const [r1Notes, setR1Notes] = useState('');

  const [r2CandidateId, setR2CandidateId] = useState('');
  const [r2DateTime, setR2DateTime] = useState('');
  const [r2InterviewerUsernames, setR2InterviewerUsernames] = useState<string[]>([]);
  const [r2Location, setR2Location] = useState('');
  const [r2Duration, setR2Duration] = useState<InterviewDuration>(60);
  const [r2Notes, setR2Notes] = useState('');

  const { data: interviewers = [] } = useGetInterviewersQuery();
  const [scheduleRound1, { isLoading: loadingR1 }] = useScheduleRound1Mutation();
  const [scheduleRound2, { isLoading: loadingR2 }] = useScheduleRound2Mutation();

  useEffect(() => {
    if (USE_INTERVIEW_MOCK) {
      setCandidates(loadCandidateOptions());
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const res = await axiosInstance({
          url: '/candidates',
          method: 'GET',
          params: { page: 0, size: 500 },
        });
        const paged = res.data?.data ?? res.data;
        const content = (paged?.content ?? []) as Array<{
          id: string;
          name: string;
          position: string;
          stage: string;
        }>;
        if (!cancelled) {
          setCandidates(
            content.map(c => ({
              id: c.id,
              name: c.name,
              position: c.position,
              stage: c.stage,
            })),
          );
        }
      } catch {
        if (!cancelled) message.error('Could not load candidates.');
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const round2Eligible = candidates.filter(c =>
    ROUND1_CLEARED_STAGES.includes(c.stage as RecruitmentStage),
  );

  function toggleInterviewer(
    username: string,
    selected: string[],
    setter: React.Dispatch<React.SetStateAction<string[]>>,
  ) {
    setter(prev => (prev.includes(username) ? prev.filter(u => u !== username) : [...prev, username]));
  }

  async function handleR1Submit(e: React.FormEvent) {
    e.preventDefault();
    if (!r1CandidateId) {
      message.error('Please select a candidate.');
      return;
    }
    if (!r1DateTime) {
      message.error('Please select date and time.');
      return;
    }
    if (r1InterviewerUsernames.length === 0) {
      message.error('Select at least one interviewer.');
      return;
    }
    if (!r1MeetingLink) {
      message.error('Please enter a meeting link.');
      return;
    }
    const dto: ScheduleRound1Dto = {
      candidateId: r1CandidateId,
      scheduledAt: r1DateTime,
      interviewerUsernames: r1InterviewerUsernames,
      meetingLink: r1MeetingLink,
      duration: r1Duration,
      notes: r1Notes || undefined,
    };
    try {
      await scheduleRound1(dto).unwrap();
      message.success('Round 1 interview scheduled successfully.');
      navigate('/interviews/calendar');
    } catch {
      message.error('Failed to schedule interview. Please try again.');
    }
  }

  async function handleR2Submit(e: React.FormEvent) {
    e.preventDefault();
    if (!r2CandidateId) {
      message.error('Please select a candidate.');
      return;
    }
    if (!r2DateTime) {
      message.error('Please select date and time.');
      return;
    }
    if (r2InterviewerUsernames.length === 0) {
      message.error('Select at least one interviewer.');
      return;
    }
    if (!r2Location) {
      message.error('Please enter interview location.');
      return;
    }
    const dto: ScheduleRound2Dto = {
      candidateId: r2CandidateId,
      scheduledAt: r2DateTime,
      interviewerUsernames: r2InterviewerUsernames,
      location: r2Location,
      duration: r2Duration,
      notes: r2Notes || undefined,
    };
    try {
      await scheduleRound2(dto).unwrap();
      message.success('Round 2 interview scheduled successfully.');
      navigate('/interviews/calendar');
    } catch {
      message.error('Failed to schedule interview. Please try again.');
    }
  }

  function InterviewerSelector({
    selected,
    onToggle,
  }: {
    selected: string[];
    onToggle: (username: string) => void;
  }) {
    return (
      <div className="si-interviewer-grid">
        {interviewers.map(iv => (
          <button
            key={iv.id}
            type="button"
            className={`si-interviewer-chip${
              selected.includes(iv.username) ? ' si-interviewer-chip--selected' : ''
            }`}
            onClick={() => onToggle(iv.username)}
          >
            <span className="si-interviewer-chip-avatar">
              {iv.username.charAt(0).toUpperCase()}
            </span>
            <span className="si-interviewer-chip-name">{iv.username}</span>
            {selected.includes(iv.username) && (
              <span className="si-interviewer-chip-check" aria-hidden>
                ✓
              </span>
            )}
          </button>
        ))}
        {interviewers.length === 0 && <p className="si-empty-hint">No interviewers available.</p>}
      </div>
    );
  }

  return (
    <div className="si-root">
      <div className="si-page-header">
        <div>
          <p className="si-page-eyebrow">Interviews</p>
          <h1 className="si-page-title">Schedule Interview</h1>
        </div>
        <button
          type="button"
          className="si-btn si-btn--ghost"
          onClick={() => navigate('/interviews/calendar')}
        >
          ← Back to Calendar
        </button>
      </div>

      <div className="si-tabs">
        <button
          type="button"
          className={`si-tab${activeTab === 'round1' ? ' si-tab--active' : ''}`}
          onClick={() => setActiveTab('round1')}
        >
          <span className="si-tab-badge si-tab-badge--r1">R1</span>
          Round 1 — Online
        </button>
        <button
          type="button"
          className={`si-tab${activeTab === 'round2' ? ' si-tab--active' : ''}`}
          onClick={() => setActiveTab('round2')}
        >
          <span className="si-tab-badge si-tab-badge--r2">R2</span>
          Round 2 — Face to Face
        </button>
      </div>

      {activeTab === 'round1' && (
        <form className="si-form" onSubmit={handleR1Submit} noValidate>
          <div className="si-form-card">
            <h2 className="si-section-title">Round 1 — Online Interview</h2>
            <p className="si-section-desc">
              Schedule an online screening interview. Candidate stage will be automatically updated
              to <strong>Round 1 Scheduled</strong>.
            </p>
            <div className="si-field">
              <label className="si-label" htmlFor="r1-candidate">
                Candidate <span className="si-required">*</span>
              </label>
              <select
                id="r1-candidate"
                className="si-select"
                value={r1CandidateId}
                onChange={e => setR1CandidateId(e.target.value)}
                required
              >
                <option value="">Select a candidate…</option>
                {candidates.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.name} — {c.position}
                  </option>
                ))}
              </select>
            </div>
            <div className="si-field">
              <label className="si-label" htmlFor="r1-datetime">
                Date & Time <span className="si-required">*</span>
              </label>
              <input
                id="r1-datetime"
                type="datetime-local"
                className="si-input"
                value={r1DateTime}
                onChange={e => setR1DateTime(e.target.value)}
                required
              />
            </div>
            <div className="si-field">
              <label className="si-label">
                Interviewers <span className="si-required">*</span>
              </label>
              <p className="si-hint">Select one or more interviewers.</p>
              <InterviewerSelector
                selected={r1InterviewerUsernames}
                onToggle={u => toggleInterviewer(u, r1InterviewerUsernames, setR1InterviewerUsernames)}
              />
              {r1InterviewerUsernames.length > 0 && (
                <p className="si-selection-count">
                  {r1InterviewerUsernames.length} interviewer
                  {r1InterviewerUsernames.length > 1 ? 's' : ''} selected
                </p>
              )}
            </div>
            <div className="si-field">
              <label className="si-label" htmlFor="r1-link">
                Meeting Link <span className="si-required">*</span>
              </label>
              <input
                id="r1-link"
                type="url"
                className="si-input"
                placeholder="https://meet.google.com/…"
                value={r1MeetingLink}
                onChange={e => setR1MeetingLink(e.target.value)}
                required
              />
              <p className="si-hint">Google Meet, Zoom, Microsoft Teams, etc.</p>
            </div>
            <div className="si-field">
              <label className="si-label">
                Duration <span className="si-required">*</span>
              </label>
              <div className="si-duration-group">
                {DURATION_OPTIONS.map(d => (
                  <button
                    key={d}
                    type="button"
                    className={`si-duration-btn${r1Duration === d ? ' si-duration-btn--active' : ''}`}
                    onClick={() => setR1Duration(d)}
                  >
                    {d} min
                  </button>
                ))}
              </div>
            </div>
            <div className="si-field">
              <label className="si-label" htmlFor="r1-notes">
                Notes <span className="si-optional">(optional)</span>
              </label>
              <textarea
                id="r1-notes"
                className="si-textarea"
                rows={3}
                placeholder="Topics to cover, special instructions…"
                value={r1Notes}
                onChange={e => setR1Notes(e.target.value)}
              />
            </div>
            <div className="si-form-actions">
              <button
                type="button"
                className="si-btn si-btn--ghost"
                onClick={() => navigate('/interviews/calendar')}
              >
                Cancel
              </button>
              <button type="submit" className="si-btn si-btn--primary" disabled={loadingR1}>
                {loadingR1 ? 'Scheduling…' : 'Schedule Round 1'}
              </button>
            </div>
          </div>
        </form>
      )}

      {activeTab === 'round2' && (
        <form className="si-form" onSubmit={handleR2Submit} noValidate>
          <div className="si-form-card">
            <h2 className="si-section-title">Round 2 — Face-to-Face Interview</h2>
            <p className="si-section-desc">
              Only candidates who have <strong>cleared Round 1</strong> are eligible. Candidate
              stage will be updated to <strong>Round 2 Scheduled</strong>.
            </p>
            <div className="si-field">
              <label className="si-label" htmlFor="r2-candidate">
                Candidate <span className="si-required">*</span>
              </label>
              <select
                id="r2-candidate"
                className="si-select"
                value={r2CandidateId}
                onChange={e => setR2CandidateId(e.target.value)}
                required
              >
                <option value="">Select a candidate…</option>
                {round2Eligible.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.name} — {c.position}
                  </option>
                ))}
              </select>
              {round2Eligible.length === 0 && (
                <p className="si-hint si-hint--warn">No candidates have cleared Round 1 yet.</p>
              )}
            </div>
            <div className="si-field">
              <label className="si-label" htmlFor="r2-datetime">
                Date & Time <span className="si-required">*</span>
              </label>
              <input
                id="r2-datetime"
                type="datetime-local"
                className="si-input"
                value={r2DateTime}
                onChange={e => setR2DateTime(e.target.value)}
                required
              />
            </div>
            <div className="si-field">
              <label className="si-label">
                Interviewers <span className="si-required">*</span>
              </label>
              <p className="si-hint">Select one or more interviewers.</p>
              <InterviewerSelector
                selected={r2InterviewerUsernames}
                onToggle={u => toggleInterviewer(u, r2InterviewerUsernames, setR2InterviewerUsernames)}
              />
              {r2InterviewerUsernames.length > 0 && (
                <p className="si-selection-count">
                  {r2InterviewerUsernames.length} interviewer
                  {r2InterviewerUsernames.length > 1 ? 's' : ''} selected
                </p>
              )}
            </div>
            <div className="si-field">
              <label className="si-label" htmlFor="r2-location">
                Location / Room <span className="si-required">*</span>
              </label>
              <input
                id="r2-location"
                type="text"
                className="si-input"
                placeholder="e.g. Conference Room B, 3rd Floor"
                value={r2Location}
                onChange={e => setR2Location(e.target.value)}
                required
              />
            </div>
            <div className="si-field">
              <label className="si-label">
                Duration <span className="si-required">*</span>
              </label>
              <div className="si-duration-group">
                {DURATION_OPTIONS.map(d => (
                  <button
                    key={d}
                    type="button"
                    className={`si-duration-btn${r2Duration === d ? ' si-duration-btn--active' : ''}`}
                    onClick={() => setR2Duration(d)}
                  >
                    {d} min
                  </button>
                ))}
              </div>
            </div>
            <div className="si-field">
              <label className="si-label" htmlFor="r2-notes">
                Notes <span className="si-optional">(optional)</span>
              </label>
              <textarea
                id="r2-notes"
                className="si-textarea"
                rows={3}
                placeholder="Topics to cover, special instructions…"
                value={r2Notes}
                onChange={e => setR2Notes(e.target.value)}
              />
            </div>
            <div className="si-form-actions">
              <button
                type="button"
                className="si-btn si-btn--ghost"
                onClick={() => navigate('/interviews/calendar')}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="si-btn si-btn--primary"
                disabled={loadingR2 || round2Eligible.length === 0}
              >
                {loadingR2 ? 'Scheduling…' : 'Schedule Round 2'}
              </button>
            </div>
          </div>
        </form>
      )}
    </div>
  );
};

export default ScheduleInterviewPage;
