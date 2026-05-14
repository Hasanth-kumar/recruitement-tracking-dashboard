import React, { useEffect, useState } from 'react';
import { Button } from 'antd';
import { ClockCircleOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  InterviewResponseDto,
  apiFetchInterviewSchedule,
} from '../feedbackApi';
import '../../../App.css';

function hoursAgo(iso: string): number {
  return Math.floor((Date.now() - new Date(iso).getTime()) / 3600000);
}

function formatScheduled(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function candidateDisplayLabel(interview: InterviewResponseDto): string {
  const n = interview.candidateName?.trim();
  if (n) return n;
  return `${interview.candidateId.slice(0, 8)}…`;
}

function isOverdue(interview: InterviewResponseDto): boolean {
  if (interview.status === 'CANCELLED') return false;
  const end = new Date(interview.dateTime);
  end.setMinutes(end.getMinutes() + (interview.durationMinutes ?? 60));
  const cutoff = new Date(end);
  cutoff.setHours(cutoff.getHours() + 24);
  return new Date() >= cutoff;
}

const PendingFeedbackPanel: React.FC = () => {
  const navigate = useNavigate();
  const [items, setItems] = useState<InterviewResponseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const now = new Date();
        const twoWeeksAgo = new Date(now);
        twoWeeksAgo.setDate(twoWeeksAgo.getDate() - 14);
        const data = await apiFetchInterviewSchedule(
          twoWeeksAgo.toISOString(),
          now.toISOString(),
        );
        if (!cancelled) {
          const overdue = data.filter(
            (i) => i.status !== 'CANCELLED' && isOverdue(i),
          );
          setItems(overdue);
        }
      } catch (e: unknown) {
        if (!cancelled)
          setError(
            e instanceof Error ? e.message : 'Failed to load pending feedback.',
          );
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const toFeedbackForm = (interview: InterviewResponseDto) =>
    navigate(
      `/feedback/new?interviewId=${interview.id}&candidateId=${interview.candidateId}&round=${interview.round}`,
    );

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
        Interviews completed over 24 hours ago that may need feedback.
      </p>

      <div className="pending-panel-list">
        {items.map((interview) => {
          const hours = hoursAgo(interview.dateTime);
          const roundLabel =
            interview.round === 'ROUND_2' ? 'Round 2' : 'Round 1';
          return (
            <div key={interview.id} className="pending-panel-item">
              <div className="pending-panel-item-info">
                <div className="pending-panel-item-top">
                  <span className="pending-panel-item-name">
                    {candidateDisplayLabel(interview)}
                  </span>
                  <span className="pending-panel-item-round">{roundLabel}</span>
                </div>
                <div className="pending-panel-item-meta">
                  <span>{formatScheduled(interview.dateTime)}</span>
                  <span className="pending-panel-item-overdue">
                    {hours}h ago
                  </span>
                </div>
              </div>
              <Button
                type="link"
                size="small"
                icon={<ArrowRightOutlined />}
                onClick={() => toFeedbackForm(interview)}
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
