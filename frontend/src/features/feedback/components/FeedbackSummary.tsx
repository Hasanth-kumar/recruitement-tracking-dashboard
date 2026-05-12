import React from 'react';
import {
  FeedbackResponseDto,
  CandidateFeedbackSummaryResponse,
  Recommendation,
} from '../feedbackApi';
import '../../../App.css';

const REC_COLOR: Record<Recommendation, string> = {
  SELECT: '#16a34a',
  REJECT: '#dc2626',
  HOLD: '#d97706',
  PROCEED: '#2563eb',
};

const REC_BG: Record<Recommendation, string> = {
  SELECT: '#f0fdf4',
  REJECT: '#fef2f2',
  HOLD: '#fffbeb',
  PROCEED: '#eff4ff',
};

const REC_LABEL: Record<Recommendation, string> = {
  SELECT: 'Select',
  REJECT: 'Reject',
  HOLD: 'Hold',
  PROCEED: 'Proceed',
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

function RatingBar({ value }: { value: number }) {
  const pct = (value / 5) * 100;
  return (
    <div className="feedback-rating-bar-wrap">
      <div className="feedback-rating-bar-track">
        <div
          className="feedback-rating-bar-fill"
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="feedback-rating-bar-value">{value.toFixed(1)}</span>
    </div>
  );
}

function FeedbackCard({ fb }: { fb: FeedbackResponseDto }) {
  const recColor = REC_COLOR[fb.recommendation];
  const recBg = REC_BG[fb.recommendation];
  const recLabel = REC_LABEL[fb.recommendation];

  return (
    <div className="feedback-card">
      <div className="feedback-card-header">
        <div>
          <p className="feedback-card-interviewer">{fb.submittedByUsername}</p>
          <p className="feedback-card-date">{formatDate(fb.submittedAt)}</p>
        </div>
        <span
          className="feedback-rec-badge"
          style={{ color: recColor, background: recBg }}
        >
          {recLabel}
        </span>
      </div>

      <div className="feedback-ratings-grid">
        {[
          { label: 'Technical Skills', value: fb.technicalRating },
          { label: 'Communication', value: fb.communicationRating },
          { label: 'Problem-Solving', value: fb.problemSolvingRating },
          { label: 'Leadership', value: fb.leadershipRating },
          { label: 'Culture Fit', value: fb.cultureRating },
        ].map(({ label, value }) => (
          <div key={label} className="feedback-rating-row">
            <span className="feedback-rating-label">{label}</span>
            <RatingBar value={value} />
          </div>
        ))}
      </div>

      {fb.comments && (
        <div className="feedback-comments-wrap">
          <p className="feedback-comments-label">Comments</p>
          <p className="feedback-comments-text">{fb.comments}</p>
        </div>
      )}
    </div>
  );
}

interface Props {
  summary: CandidateFeedbackSummaryResponse | null;
  loading?: boolean;
}

const FeedbackSummary: React.FC<Props> = ({ summary, loading }) => {
  if (loading) {
    return <p className="feedback-summary-loading">Loading feedback…</p>;
  }

  if (!summary || summary.totalFeedbackCount === 0) {
    return (
      <div className="feedback-summary-empty">
        <p>No feedback submitted yet.</p>
      </div>
    );
  }

  const { feedbacks } = summary;

  return (
    <div className="feedback-summary-root">
      <div className="feedback-agg-strip">
        <div className="feedback-agg-score">
          <span className="feedback-agg-score-num">
            {summary.overallAverageRating?.toFixed(1) ?? '—'}
          </span>
          <span className="feedback-agg-score-denom">/ 5</span>
          <span className="feedback-agg-score-label">Overall average</span>
        </div>

        <div className="feedback-agg-breakdown">
          {[
            { label: 'Technical', value: summary.averageTechnicalRating },
            { label: 'Communication', value: summary.averageCommunicationRating },
            { label: 'Problem-Solving', value: summary.averageProblemSolvingRating },
            { label: 'Leadership', value: summary.averageLeadershipRating },
            { label: 'Culture Fit', value: summary.averageCultureRating },
          ].map(({ label, value }) => (
            <div key={label} className="feedback-agg-item">
              <span className="feedback-agg-item-label">{label}</span>
              <span className="feedback-agg-item-value">
                {value?.toFixed(1) ?? '—'}
              </span>
            </div>
          ))}
        </div>
      </div>

      <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: '0 0 1rem' }}>
        {summary.totalFeedbackCount} feedback{summary.totalFeedbackCount !== 1 ? 's' : ''} submitted
      </p>

      <section className="feedback-round-section">
        {feedbacks.map((fb) => (
          <FeedbackCard key={fb.id} fb={fb} />
        ))}
      </section>
    </div>
  );
};

export default FeedbackSummary;
