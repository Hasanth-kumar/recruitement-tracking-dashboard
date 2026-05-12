import React from 'react';
import { FeedbackResponseDto, Recommendation } from '../feedbackApi';
import '../../../App.css';

// ── Recommendation colours ───────────────────────────────────────

const REC_COLOR: Record<Recommendation, string> = {
SELECT:            '#16a34a',
REJECT:            '#dc2626',
HOLD:              '#d97706',
PROCEED_TO_ROUND_2: '#2563eb',
};

const REC_BG: Record<Recommendation, string> = {
SELECT:            '#f0fdf4',
REJECT:            '#fef2f2',
HOLD:              '#fffbeb',
PROCEED_TO_ROUND_2: '#eff4ff',
};

const REC_LABEL: Record<Recommendation, string> = {
SELECT:            'Select',
REJECT:            'Reject',
HOLD:              'Hold',
PROCEED_TO_ROUND_2: 'Proceed to Round 2',
};

// ── Helpers ──────────────────────────────────────────────────────

function avg(items: FeedbackResponseDto[], key: keyof Pick<
FeedbackResponseDto,
'technicalSkills' | 'communicationSkills' | 'problemSolving' | 'culturalFit' | 'overallRating'
>): number {
if (items.length === 0) return 0;
return items.reduce((s, f) => s + (f[key] as number), 0) / items.length;
}

function formatDate(iso: string): string {
return new Date(iso).toLocaleDateString('en-US', {
  day: '2-digit', month: 'short', year: 'numeric',
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

// ── Sub-component: single feedback card ──────────────────────────

function FeedbackCard({ fb }: { fb: FeedbackResponseDto }) {
const recColor = REC_COLOR[fb.recommendation];
const recBg    = REC_BG[fb.recommendation];
const recLabel = REC_LABEL[fb.recommendation];

return (
  <div className="feedback-card">
    {/* Card header */}
    <div className="feedback-card-header">
      <div>
        <p className="feedback-card-interviewer">{fb.interviewerName}</p>
        <p className="feedback-card-date">{formatDate(fb.submittedAt)}</p>
      </div>
      <span
        className="feedback-rec-badge"
        style={{ color: recColor, background: recBg }}
      >
        {recLabel}
      </span>
    </div>

    {/* Ratings */}
    <div className="feedback-ratings-grid">
      {[
        { label: 'Technical Skills',     value: fb.technicalSkills     },
        { label: 'Communication',        value: fb.communicationSkills },
        { label: 'Problem-Solving',      value: fb.problemSolving      },
        { label: 'Cultural Fit',         value: fb.culturalFit         },
        { label: 'Overall',              value: fb.overallRating       },
      ].map(({ label, value }) => (
        <div key={label} className="feedback-rating-row">
          <span className="feedback-rating-label">{label}</span>
          <RatingBar value={value} />
        </div>
      ))}
    </div>

    {/* Comments */}
    {fb.comments && (
      <div className="feedback-comments-wrap">
        <p className="feedback-comments-label">Comments</p>
        <p className="feedback-comments-text">{fb.comments}</p>
      </div>
    )}
  </div>
);
}

// ── Props ─────────────────────────────────────────────────────────

interface Props {
feedback:    FeedbackResponseDto[];
loading?:    boolean;
}

// ── Main component ────────────────────────────────────────────────

const FeedbackSummary: React.FC<Props> = ({ feedback, loading }) => {
if (loading) {
  return <p className="feedback-summary-loading">Loading feedback…</p>;
}

if (feedback.length === 0) {
  return (
    <div className="feedback-summary-empty">
      <p>No feedback submitted yet.</p>
    </div>
  );
}

const round1 = feedback.filter(f => f.round === 1);
const round2 = feedback.filter(f => f.round === 2);

// Overall averages across all feedback
const allAvg = {
  technicalSkills:     avg(feedback, 'technicalSkills'),
  communicationSkills: avg(feedback, 'communicationSkills'),
  problemSolving:      avg(feedback, 'problemSolving'),
  culturalFit:         avg(feedback, 'culturalFit'),
  overallRating:       avg(feedback, 'overallRating'),
};

const compositeAvg =
  Object.values(allAvg).reduce((s, v) => s + v, 0) / 5;

return (
  <div className="feedback-summary-root">

    {/* ── Aggregate strip ────────────────────────────────────── */}
    <div className="feedback-agg-strip">
      <div className="feedback-agg-score">
        <span className="feedback-agg-score-num">{compositeAvg.toFixed(1)}</span>
        <span className="feedback-agg-score-denom">/ 5</span>
        <span className="feedback-agg-score-label">Composite average</span>
      </div>

      <div className="feedback-agg-breakdown">
        {[
          { label: 'Technical',      value: allAvg.technicalSkills     },
          { label: 'Communication',  value: allAvg.communicationSkills },
          { label: 'Problem-Solving', value: allAvg.problemSolving     },
          { label: 'Cultural Fit',   value: allAvg.culturalFit         },
          { label: 'Overall',        value: allAvg.overallRating       },
        ].map(({ label, value }) => (
          <div key={label} className="feedback-agg-item">
            <span className="feedback-agg-item-label">{label}</span>
            <span className="feedback-agg-item-value">{value.toFixed(1)}</span>
          </div>
        ))}
      </div>
    </div>

    {/* ── Round 1 ────────────────────────────────────────────── */}
    {round1.length > 0 && (
      <section className="feedback-round-section">
        <h3 className="feedback-round-title">Round 1 — Online Interview</h3>
        {round1.map(fb => (
          <FeedbackCard key={fb.id} fb={fb} />
        ))}
      </section>
    )}

    {/* ── Round 2 ────────────────────────────────────────────── */}
    {round2.length > 0 && (
      <section className="feedback-round-section">
        <h3 className="feedback-round-title">Round 2 — Face-to-Face Interview</h3>
        {round2.map(fb => (
          <FeedbackCard key={fb.id} fb={fb} />
        ))}
      </section>
    )}
  </div>
);
};

export default FeedbackSummary;
