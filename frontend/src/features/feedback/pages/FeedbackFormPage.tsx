import React, { useState } from 'react';
import { Slider, Select, Button, Alert } from 'antd';
import { ArrowLeftOutlined, SendOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  SubmitFeedbackRequest,
  Recommendation,
  apiSubmitFeedback,
} from '../feedbackApi';
import '../../../App.css';

interface RatingFields {
  technicalRating: number;
  communicationRating: number;
  problemSolvingRating: number;
  leadershipRating: number;
  cultureRating: number;
}

type FormState = RatingFields & {
  comments: string;
  recommendation: Recommendation | '';
};

const INITIAL_FORM: FormState = {
  technicalRating: 3,
  communicationRating: 3,
  problemSolvingRating: 3,
  leadershipRating: 3,
  cultureRating: 3,
  comments: '',
  recommendation: '',
};

const RECOMMENDATION_OPTIONS: { value: Recommendation; label: string }[] = [
  { value: 'SELECT', label: 'Select' },
  { value: 'REJECT', label: 'Reject' },
  { value: 'HOLD', label: 'Hold' },
  { value: 'PROCEED', label: 'Proceed' },
];

const CRITERIA: { key: keyof RatingFields; label: string; hint: string }[] = [
  { key: 'technicalRating', label: 'Technical Skills', hint: 'Domain knowledge, coding ability, tools' },
  { key: 'communicationRating', label: 'Communication', hint: 'Clarity, listening, articulation' },
  { key: 'problemSolvingRating', label: 'Problem-Solving', hint: 'Analytical thinking, approach' },
  { key: 'leadershipRating', label: 'Leadership Potential', hint: 'Initiative, ownership, mentoring' },
  { key: 'cultureRating', label: 'Culture Fit', hint: 'Values alignment, team compatibility' },
];

function ratingLabel(v: number): string {
  return ['', 'Poor', 'Below average', 'Average', 'Good', 'Excellent'][v] ?? '';
}

const FeedbackFormPage: React.FC = () => {
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const interviewId = params.get('interviewId') ?? '';
  const candidateId = params.get('candidateId') ?? '';
  const roundParam = params.get('round') ?? '';
  const roundLabel = roundParam === 'ROUND_2' ? 'Round 2' : 'Round 1';

  const [form, setForm] = useState<FormState>(INITIAL_FORM);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  function validate(): boolean {
    const errs: Partial<Record<keyof FormState, string>> = {};
    if (!form.recommendation) errs.recommendation = 'Recommendation is required.';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  const setRating = (key: keyof RatingFields, value: number) =>
    setForm((p) => ({ ...p, [key]: value }));

  const setField = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((p) => ({ ...p, [key]: value }));
    if (errors[key]) setErrors((p) => ({ ...p, [key]: undefined }));
    if (submitError) setSubmitError(null);
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    if (!interviewId) {
      setSubmitError('No interview specified. Please go back and select an interview.');
      return;
    }
    setSaving(true);
    setSubmitError(null);
    try {
      const payload: SubmitFeedbackRequest = {
        interviewId,
        technicalRating: form.technicalRating,
        communicationRating: form.communicationRating,
        problemSolvingRating: form.problemSolvingRating,
        leadershipRating: form.leadershipRating,
        cultureRating: form.cultureRating,
        recommendation: form.recommendation as Recommendation,
        comments: form.comments.trim() || undefined,
      };
      await apiSubmitFeedback(payload);
      setSubmitted(true);
    } catch (e: unknown) {
      setSubmitError(
        e instanceof Error ? e.message : 'Something went wrong. Please try again.',
      );
    } finally {
      setSaving(false);
    }
  };

  if (!interviewId) {
    return (
      <div className="feedback-root">
        <button
          type="button"
          className="feedback-back-btn"
          onClick={() => navigate('/feedback')}
        >
          <ArrowLeftOutlined /> Back to feedback list
        </button>
        <Alert
          type="error"
          message="No interview specified. Please go back and select an interview to provide feedback for."
          showIcon
          className="feedback-alert"
        />
        <Button onClick={() => navigate('/feedback')}>Return to feedback list</Button>
      </div>
    );
  }

  if (submitted) {
    return (
      <div className="feedback-success-wrap">
        <div className="feedback-success-card">
          <span className="feedback-success-icon">✓</span>
          <h2 className="feedback-success-title">Feedback submitted</h2>
          <p className="feedback-success-desc">
            Your {roundLabel} feedback for candidate{' '}
            <strong>{candidateId.slice(0, 8)}…</strong> has been recorded.
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

  const avgScore = (
    (form.technicalRating +
      form.communicationRating +
      form.problemSolvingRating +
      form.leadershipRating +
      form.cultureRating) /
    5
  ).toFixed(1);

  return (
    <div className="feedback-root">
      <button
        type="button"
        className="feedback-back-btn"
        onClick={() => navigate('/feedback')}
      >
        <ArrowLeftOutlined /> Back to feedback list
      </button>

      <div className="feedback-header">
        <p className="feedback-eyebrow">{roundLabel} · Interview feedback</p>
        <h2 className="feedback-title">Submit feedback</h2>
      </div>

      <div className="feedback-candidate-strip">
        <div className="feedback-candidate-info">
          <span className="feedback-candidate-meta">
            Interview: <code>{interviewId.slice(0, 8)}…</code>
            {candidateId && (
              <> · Candidate: <code>{candidateId.slice(0, 8)}…</code></>
            )}
          </span>
        </div>
      </div>

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
        <div className="feedback-card">
          <h3 className="feedback-card-title">Rating criteria</h3>
          <p className="feedback-card-desc">
            Rate each area from 1 (Poor) to 5 (Excellent).
          </p>

          {CRITERIA.map(({ key, label, hint }) => (
            <div key={key} className="feedback-criterion">
              <div className="feedback-criterion-header">
                <div>
                  <span className="feedback-criterion-label">{label}</span>
                  <span className="feedback-criterion-hint">{hint}</span>
                </div>
                <span className="feedback-criterion-value">
                  {form[key]} — {ratingLabel(form[key])}
                </span>
              </div>
              <Slider
                min={1}
                max={5}
                step={1}
                value={form[key]}
                onChange={(val) => setRating(key, val)}
                marks={{ 1: '1', 2: '2', 3: '3', 4: '4', 5: '5' }}
                className="feedback-slider"
              />
            </div>
          ))}
        </div>

        <div>
          <div className="feedback-card">
            <h3 className="feedback-card-title">Recommendation</h3>
            <p className="feedback-card-desc">
              Your hiring recommendation for this candidate.
            </p>
            <Select
              placeholder="Select recommendation"
              value={form.recommendation || undefined}
              onChange={(val) => setField('recommendation', val)}
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
            <p className="feedback-card-desc">Max 1000 characters (optional).</p>
            <textarea
              className="feedback-textarea"
              placeholder="Describe the candidate's performance, strengths, and areas for improvement…"
              value={form.comments}
              onChange={(e) => setField('comments', e.target.value)}
              maxLength={1000}
              rows={6}
            />
            <div className="feedback-char-count">{form.comments.length} / 1000</div>
          </div>

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
