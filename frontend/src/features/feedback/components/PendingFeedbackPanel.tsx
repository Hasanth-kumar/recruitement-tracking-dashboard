import React, { useEffect, useState } from 'react';
import { Button } from 'antd';
import { ClockCircleOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
PendingFeedbackItem,
MOCK_PENDING,
apiFetchPendingFeedback,
} from '../feedbackApi';
import '../../../App.css';

// ── Toggle ──────────────────────────────────────────────────────
const USE_MOCK = true;

// ── Helpers ──────────────────────────────────────────────────────

function hoursAgo(iso: string): number {
return Math.floor((Date.now() - new Date(iso).getTime()) / 3600000);
}

function formatScheduled(iso: string): string {
return new Date(iso).toLocaleString('en-US', {
  day:    '2-digit',
  month:  'short',
  hour:   '2-digit',
  minute: '2-digit',
});
}

// ── Component ───────────────────────────────────────────────────

const PendingFeedbackPanel: React.FC = () => {
const navigate = useNavigate();
const [items,   setItems]   = useState<PendingFeedbackItem[]>([]);
const [loading, setLoading] = useState(true);
const [error,   setError]   = useState<string | null>(null);

useEffect(() => {
  let cancelled = false;

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      if (USE_MOCK) {
        await new Promise(r => setTimeout(r, 300));
        if (!cancelled) setItems(MOCK_PENDING);
      } else {
        const data = await apiFetchPendingFeedback();
        if (!cancelled) setItems(data);
      }
    } catch (e: unknown) {
      if (!cancelled)
        setError(e instanceof Error ? e.message : 'Failed to load pending feedback.');
    } finally {
      if (!cancelled) setLoading(false);
    }
  };

  load();
  return () => { cancelled = true; };
}, []);

const toFeedbackForm = (item: PendingFeedbackItem) =>
  navigate(
    `/feedback/new?interviewId=${item.interviewId}&candidateId=${item.candidateId}&candidateName=${encodeURIComponent(item.candidateName)}`,
  );

// ── Loading ──────────────────────────────────────────────────

if (loading) {
  return (
    <div className="pending-panel">
      <div className="pending-panel-header">
        <ClockCircleOutlined className="pending-panel-header-icon" />
        <h3 className="pending-panel-title">Pending Feedback</h3>
      </div>
      <p className="pending-panel-loading">Loading…</p>
    </div>
  );
}

// ── Error ─────────────────────────────────────────────────────

if (error) {
  return (
    <div className="pending-panel">
      <div className="pending-panel-header">
        <ClockCircleOutlined className="pending-panel-header-icon" />
        <h3 className="pending-panel-title">Pending Feedback</h3>
      </div>
      <p className="pending-panel-error">{error}</p>
    </div>
  );
}

// ── Empty ─────────────────────────────────────────────────────

if (items.length === 0) {
  return (
    <div className="pending-panel">
      <div className="pending-panel-header">
        <ClockCircleOutlined className="pending-panel-header-icon" />
        <h3 className="pending-panel-title">Pending Feedback</h3>
      </div>
      <div className="pending-panel-empty">
        <p>All feedback submitted. You are up to date.</p>
      </div>
    </div>
  );
}

// ── List ──────────────────────────────────────────────────────

return (
  <div className="pending-panel">
    <div className="pending-panel-header">
      <div className="pending-panel-header-left">
        <ClockCircleOutlined className="pending-panel-header-icon" />
        <h3 className="pending-panel-title">Pending Feedback</h3>
      </div>
      <span className="pending-panel-count-badge">{items.length}</span>
    </div>

    <p className="pending-panel-desc">
      Interviews completed over 24 hours ago without feedback.
    </p>

    <div className="pending-panel-list">
      {items.map(item => {
        const hours = hoursAgo(item.scheduledAt);
        return (
          <div key={item.interviewId} className="pending-panel-item">
            <div className="pending-panel-item-info">
              <div className="pending-panel-item-top">
                <span className="pending-panel-item-name">{item.candidateName}</span>
                <span className="pending-panel-item-round">
                  Round {item.round}
                </span>
              </div>
              <div className="pending-panel-item-meta">
                <span>{formatScheduled(item.scheduledAt)}</span>
                <span className="pending-panel-item-overdue">
                  {hours}h overdue
                </span>
              </div>
            </div>
            <Button
              type="link"
              size="small"
              icon={<ArrowRightOutlined />}
              onClick={() => toFeedbackForm(item)}
              className="pending-panel-item-btn"
            >
              Submit
            </Button>
          </div>
        );
      })}
    </div>
  </div>
);
};

export default PendingFeedbackPanel;
