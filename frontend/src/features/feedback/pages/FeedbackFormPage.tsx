import React, { useEffect, useState } from 'react';
import { Slider, Select, Button, Alert } from 'antd';
import { ArrowLeftOutlined, SendOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { mockGetAllCandidates } from '../../candidates/candidateMock';
import { basicAuthFetchHeaders } from '../../../shared/utils/basicAuth';
import { mapApiRowToCandidate } from '../../candidates/candidateApiMappers';
import { Candidate, RecruitmentStage } from '../../candidates/candidateTypes';
import StatusBadge from '../../../shared/components/StatusBadge';
import {
  FeedbackSubmitDto,
  FeedbackRatings,
  Recommendation,
  MOCK_FEEDBACK,
  apiSubmitFeedback,
} from '../feedbackApi';
import '../../../App.css';

// ── Toggle ──────────────────────────────────────────────────────
const USE_MOCK = true;

// ── Eligible stages guard ────────────────────────────────────────
const ELIGIBLE_STAGES: RecruitmentStage[] = ['R1_CLEARED', 'R2_CLEARED'];

// ── Types ────────────────────────────────────────────────────────

type FormState = FeedbackRatings & {
  comments:       string;
  recommendation: Recommendation | '';
};

const INITIAL_FORM: FormState = {
  technicalSkills:     3,
  communicationSkills: 3,
  problemSolving:      3,
  culturalFit:         3,
  overallRating:       3,
  comments:            '',
  recommendation:      '',
};

const RECOMMENDATION_OPTIONS: { value: Recommendation; label: string }[] = [
  { value: 'SELECT',             label: 'Select'              },
  { value: 'REJECT',             label: 'Reject'              },
  { value: 'HOLD',               label: 'Hold'                },
  { value: 'PROCEED_TO_ROUND_2', label: 'Proceed to Round 2'  },
];

const CRITERIA: { key: keyof FeedbackRatings; label: string; hint: string }[] = [
  { key: 'technicalSkills',     label: 'Technical Skills',     hint: 'Domain knowledge, coding ability, tools' },
  { key: 'communicationSkills', label: 'Communication Skills', hint: 'Clarity, listening, articulation'         },
  { key: 'problemSolving',      label: 'Problem-Solving',      hint: 'Analytical thinking, approach'            },
  { key: 'culturalFit',         label: 'Cultural Fit',         hint: 'Values alignment, team compatibility'     },
  { key: 'overallRating',       label: 'Overall Rating',       hint: 'Holistic assessment'                      },
];

function ratingLabel(v: number): string {
  return ['', 'Poor', 'Below average', 'Average', 'Good', 'Excellent'][v] ?? '';
}

// ── API helper ───────────────────────────────────────────────────
async function apiFetchCandidate(id: string): Promise<Candidate> {
  const res = await fetch(`/api/candidates/${id}`, {
    headers: basicAuthFetchHeaders(false),
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message ?? 'Candidate not found.');
  return mapApiRowToCandidate(data.data as Record<string, unknown>);
}

// ── Component ────────────────────────────────────────────────────

const FeedbackFormPage: React.FC = () => {
  const navigate     = useNavigate();
  const [params]     = useSearchParams();

  const candidateId   = params.get('candidateId')   ?? '';
  const candidateName = params.get('candidateName') ?? '';
  const roundParam    = params.get('round');
  const round         = roundParam === '2' ? 2 : 1;

  const [candidate,   setCandidate]   = useState<Candidate | null>(null);
  const [loadError,   setLoadError]   = useState<string | null>(null);
  const [loadingMeta, setLoadingMeta] = useState(true);

  const [form,        setForm]        = useState<FormState>(INITIAL_FORM);
  const [errors,      setErrors]      = useState<Partial<Record<keyof FormState, string>>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [saving,      setSaving]      = useState(false);
  const [submitted,   setSubmitted]   = useState(false);

  // ── Validate candidateId and enforce stage gate ──────────────
  useEffect(() => {
    if (!candidateId) {
      setLoadError('No candidate specified. Please go back and select a candidate.');
      setLoadingMeta(false);
      return;
    }

    let cancelled = false;
    const load = async () => {
      setLoadingMeta(true);
      try {
        let c: Candidate | null = null;
        if (USE_MOCK) {
          await new Promise(r => setTimeout(r, 200));
          c = mockGetAllCandidates().find(x => x.id === candidateId) ?? null;
        } else {
          c = await apiFetchCandidate(candidateId);
        }
        if (cancelled) return;

        if (!c) {
          setLoadError('Candidate not found.');
          return;
        }

        // Stage gate — only R1_CLEARED and R2_CLEARED are permitted
        if (!ELIGIBLE_STAGES.includes(c.stage)) {
          setLoadError(
            `Feedback can only be submitted for candidates who have cleared Round 1 or Round 2. ` +
            `"${c.name}" is currently at stage "${c.stage.replace(/_/g, ' ')}".`,
          );
          return;
        }

        setCandidate(c);
      } catch (e: unknown) {
        if (!cancelled)
          setLoadError(e instanceof Error ? e.message : 'Failed to load candidate.');
      } finally {
        if (!cancelled) setLoadingMeta(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, [candidateId]);

  // ── Validation ───────────────────────────────────────────────
  function validate(): boolean {
    const errs: Partial<Record<keyof FormState, string>> = {};
    if (!form.recommendation)  errs.recommendation = 'Recommendation is required.';
    if (!form.comments.trim()) errs.comments        = 'Comments are required.';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  // ── Field helpers ─────────────────────────────────────────────
  const setRating = (key: keyof FeedbackRatings, value: number) =>
    setForm(p => ({ ...p, [key]: value }));

  const setField = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm(p => ({ ...p, [key]: value }));
    if (errors[key])  setErrors(p => ({ ...p, [key]: undefined }));
    if (submitError)  setSubmitError(null);
  };

  // ── Submit ────────────────────────────────────────────────────
  const handleSubmit = async () => {
    if (!validate()) return;
    setSaving(true);
    setSubmitError(null);
    try {
      if (USE_MOCK) {
        await new Promise(r => setTimeout(r, 600));
        MOCK_FEEDBACK.push({
          id:              'FB-' + Date.now(),
          interviewId:     '',
          candidateId,
          interviewerName: 'Current User',
          technicalSkills:     form.technicalSkills,
          communicationSkills: form.communicationSkills,
          problemSolving:      form.problemSolving,
          culturalFit:         form.culturalFit,
          overallRating:       form.overallRating,
          comments:       form.comments,
          recommendation: form.recommendation as Recommendation,
          submittedAt:    new Date().toISOString(),
          round,
        });
      } else {
        const payload: FeedbackSubmitDto = {
          interviewId:     '',
          candidateId,
          technicalSkills:     form.technicalSkills,
          communicationSkills: form.communicationSkills,
          problemSolving:      form.problemSolving,
          culturalFit:         form.culturalFit,
          overallRating:       form.overallRating,
          comments:       form.comments,
          recommendation: form.recommendation as Recommendation,
        };
        await apiSubmitFeedback(payload);
      }
      setSubmitted(true);
    } catch (e: unknown) {
      setSubmitError(
        e instanceof Error ? e.message : 'Something went wrong. Please try again.',
      );
    } finally {
      setSaving(false);
    }
  };

  // ── Loading ──────────────────────────────────────────────────

  if (loadingMeta) {
    return (
      <div className="feedback-root">
        <p className="feedback-list-state-text">Loading candidate…</p>
      </div>
    );
  }

  // ── Stage gate error ─────────────────────────────────────────

  if (loadError) {
    return (
      <div className="feedback-root">
        <button type="button" className="feedback-back-btn" onClick={() => navigate('/feedback')}>
          <ArrowLeftOutlined /> Back to feedback list
        </button>
        <Alert type="error" message={loadError} showIcon className="feedback-alert" />
        <Button onClick={() => navigate('/feedback')}>Return to feedback list</Button>
      </div>
    );
  }

  // ── Success screen ────────────────────────────────────────────

  if (submitted) {
    return (
      <div className="feedback-success-wrap">
        <div className="feedback-success-card">
          <span className="feedback-success-icon">✓</span>
          <h2 className="feedback-success-title">Feedback submitted</h2>
          <p className="feedback-success-desc">
            Your Round {round} feedback for{' '}
            <strong>{candidate?.name ?? candidateName}</strong> has been recorded.
          </p>
          <div className="feedback-success-actions">
            <Button type="primary" onClick={() => navigate('/feedback')}>
              Back to feedback list
            </Button>
            <Button onClick={() => navigate('/candidates')}>
              View candidates
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // ── Main form ────────────────────────────────────────────────

  const displayName = candidate?.name ?? candidateName;
  const avgScore = (
    (form.technicalSkills +
      form.communicationSkills +
      form.problemSolving +
      form.culturalFit +
      form.overallRating) / 5
  ).toFixed(1);

  return (
    <div className="feedback-root">

      {/* Back */}
      <button
        type="button"
        className="feedback-back-btn"
        onClick={() => navigate('/feedback')}
      >
        <ArrowLeftOutlined /> Back to feedback list
      </button>

      {/* Page header */}
      <div className="feedback-header">
        <p className="feedback-eyebrow">Round {round} · Interview feedback</p>
        <h2 className="feedback-title">Submit feedback</h2>
      </div>

      {/* Candidate identity strip */}
      {candidate && (
        <div className="feedback-candidate-strip">
          <div className="feedback-candidate-avatar">
            {displayName[0]?.toUpperCase() ?? '?'}
          </div>
          <div className="feedback-candidate-info">
            <span className="feedback-candidate-name">{displayName}</span>
            <span className="feedback-candidate-meta">
              {candidate.position}
              {candidate.experience ? ` · ${candidate.experience}` : ''}
            </span>
          </div>
          <StatusBadge stage={candidate.stage} />
        </div>
      )}

      {submitError && (
        <Alert
          type="error"
          message={submitError}
          showIcon
          closable
          onClose={() => setSubmitError(null)}
          className="feedback-alert"
        />
      )}

      <div className="feedback-layout">

        {/* ── Left: rating sliders ────────────────────────── */}
        <div className="feedback-card">
          <h3 className="feedback-card-title">Rating criteria</h3>
          <p className="feedback-card-desc">Rate each area from 1 (Poor) to 5 (Excellent).</p>

          {CRITERIA.map(({ key, label, hint }) => (
            <div key={key} className="feedback-criterion">
              <div className="feedback-criterion-header">
                <div>
                  <span className="feedback-criterion-label">{label}</span>
                  <span className="feedback-criterion-hint">{hint}</span>
                </div>
                <span className="feedback-criterion-value">
                  {form[key as keyof FeedbackRatings]} — {ratingLabel(form[key as keyof FeedbackRatings])}
                </span>
              </div>
              <Slider
                min={1}
                max={5}
                step={1}
                value={form[key as keyof FeedbackRatings]}
                onChange={val => setRating(key as keyof FeedbackRatings, val)}
                marks={{ 1: '1', 2: '2', 3: '3', 4: '4', 5: '5' }}
                className="feedback-slider"
              />
            </div>
          ))}
        </div>

        {/* ── Right: recommendation + comments + submit ──── */}
        <div>

          <div className="feedback-card">
            <h3 className="feedback-card-title">Recommendation</h3>
            <p className="feedback-card-desc">Your hiring recommendation for this candidate.</p>
            <Select
              placeholder="Select recommendation"
              value={form.recommendation || undefined}
              onChange={val => setField('recommendation', val)}
              size="large"
              className="feedback-recommendation-select"
              options={RECOMMENDATION_OPTIONS}
              status={errors.recommendation ? 'error' : ''}
            />
            {errors.recommendation && (
              <p className="feedback-field-error">{errors.recommendation}</p>
            )}
          </div>

          <div className="feedback-card">
            <h3 className="feedback-card-title">Detailed comments</h3>
            <p className="feedback-card-desc">Max 1000 characters.</p>
            <textarea
              className={`feedback-textarea${errors.comments ? ' feedback-textarea--error' : ''}`}
              placeholder="Describe the candidate's performance, strengths, and areas for improvement…"
              value={form.comments}
              onChange={e => setField('comments', e.target.value)}
              maxLength={1000}
              rows={6}
            />
            <div className="feedback-char-count">
              {form.comments.length} / 1000
            </div>
            {errors.comments && (
              <p className="feedback-field-error">{errors.comments}</p>
            )}
          </div>

          {/* Average score preview */}
          <div className="feedback-summary-strip">
            <p className="feedback-summary-strip-label">Average score</p>
            <span className="feedback-summary-strip-score">
              {avgScore}
              <span className="feedback-summary-strip-max"> / 5</span>
            </span>
          </div>

          <div className="feedback-actions">
            <Button
              type="primary"
              icon={<SendOutlined />}
              size="large"
              loading={saving}
              onClick={handleSubmit}
            >
              {saving ? 'Submitting…' : 'Submit feedback'}
            </Button>
            <Button size="large" onClick={() => navigate('/feedback')}>
              Cancel
            </Button>
          </div>

        </div>
      </div>
    </div>
  );
};

export default FeedbackFormPage;


