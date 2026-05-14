import React, { useState } from 'react';
import { message } from 'antd';
import { useRescheduleInterviewMutation } from '../interviewApi';
import type { Interview, RescheduleInterviewApiRequest } from '../interviewTypes';

interface RescheduleModalProps {
  interview: Interview;
  onClose: () => void;
  onSuccess?: () => void;
}

function toDateTimeBody(value: string): string {
  if (value.length === 16 && value.includes('T')) return `${value}:00`;
  return value;
}

const RescheduleModal: React.FC<RescheduleModalProps> = ({
  interview,
  onClose,
  onSuccess,
}) => {
  const [newDateTime, setNewDateTime] = useState(interview.scheduledAt.slice(0, 16));
  const [reason, setReason] = useState('');
  const [reschedule, { isLoading }] = useRescheduleInterviewMutation();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!newDateTime) {
      message.error('Please select a new date and time.');
      return;
    }
    if (newDateTime === interview.scheduledAt.slice(0, 16)) {
      message.warning('New date/time is the same as the current schedule.');
      return;
    }
    const body: RescheduleInterviewApiRequest = {
      dateTime: toDateTimeBody(newDateTime),
      durationMinutes: interview.duration,
      interviewerUsernames: interview.interviewers.map(i => i.username),
      meetingLink: interview.round === 'ROUND_1' ? interview.meetingLink ?? null : null,
      location: interview.round === 'ROUND_2' ? interview.location ?? null : null,
      notes: interview.notes ?? null,
      rescheduleReason: reason.trim() || null,
    };
    try {
      await reschedule({ id: interview.id, body }).unwrap();
      message.success('Interview rescheduled successfully.');
      onSuccess?.();
      onClose();
    } catch (e: unknown) {
      const detail =
        e instanceof Error && e.message.trim() !== '' ? e.message : 'Failed to reschedule. Please try again.';
      message.error(detail);
    }
  }

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-box">
        <div className="modal-header">
          <h2 className="modal-title">Reschedule Interview</h2>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close"
          >
            ×
          </button>
        </div>
        <div className="modal-body">
          <div className="rm-meta">
            <p className="rm-candidate">{interview.candidateName}</p>
            <span
              className={`rm-round-badge rm-round-badge--${
                interview.round === 'ROUND_1' ? 'r1' : 'r2'
              }`}
            >
              {interview.round === 'ROUND_1' ? 'Round 1' : 'Round 2'}
            </span>
          </div>
          <p className="rm-current-time">
            Current:{' '}
            <strong>
              {new Date(interview.scheduledAt).toLocaleString('en-IN', {
                dateStyle: 'medium',
                timeStyle: 'short',
              })}
            </strong>
          </p>
          <form onSubmit={handleSubmit} noValidate>
            <div className="si-field">
              <label className="si-label" htmlFor="reschedule-datetime">
                New Date & Time <span className="si-required">*</span>
              </label>
              <input
                id="reschedule-datetime"
                type="datetime-local"
                className="si-input"
                value={newDateTime}
                onChange={e => setNewDateTime(e.target.value)}
                required
              />
            </div>
            <div className="si-field">
              <label className="si-label" htmlFor="reschedule-reason">
                Reason <span className="si-optional">(optional)</span>
              </label>
              <textarea
                id="reschedule-reason"
                className="si-textarea"
                rows={2}
                placeholder="Reason for rescheduling…"
                value={reason}
                onChange={e => setReason(e.target.value)}
              />
            </div>
            <div className="modal-actions">
              <button type="button" className="si-btn si-btn--ghost" onClick={onClose}>
                Cancel
              </button>
              <button type="submit" className="si-btn si-btn--primary" disabled={isLoading}>
                {isLoading ? 'Saving…' : 'Confirm Reschedule'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default RescheduleModal;
