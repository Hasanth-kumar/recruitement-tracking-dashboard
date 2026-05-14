import React, { useState } from 'react';
import { message } from 'antd';
import { useCancelInterviewMutation } from '../interviewApi';
import type { Interview } from '../interviewTypes';

interface CancelModalProps {
  interview: Interview;
  onClose: () => void;
  onSuccess?: () => void;
}

const CancelModal: React.FC<CancelModalProps> = ({
  interview,
  onClose,
  onSuccess,
}) => {
  const [reason, setReason] = useState('');
  const [cancel, { isLoading }] = useCancelInterviewMutation();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!reason.trim()) {
      message.error('Please provide a reason for cancellation.');
      return;
    }
    try {
      await cancel({ id: interview.id, dto: { reason } }).unwrap();
      message.success('Interview cancelled.');
      onSuccess?.();
      onClose();
    } catch {
      message.error('Failed to cancel interview. Please try again.');
    }
  }

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-box modal-box--danger">
        <div className="modal-header">
          <h2 className="modal-title">Cancel Interview</h2>
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
          <div className="cm-warn-banner">
            <span className="cm-warn-icon" aria-hidden>
              ⚠
            </span>
            This action cannot be undone. The interview will be marked as cancelled.
          </div>
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
            Scheduled:{' '}
            <strong>
              {new Date(interview.scheduledAt).toLocaleString('en-IN', {
                dateStyle: 'medium',
                timeStyle: 'short',
              })}
            </strong>
          </p>
          <form onSubmit={handleSubmit} noValidate>
            <div className="si-field">
              <label className="si-label" htmlFor="cancel-reason">
                Reason for Cancellation <span className="si-required">*</span>
              </label>
              <textarea
                id="cancel-reason"
                className="si-textarea"
                rows={3}
                placeholder="Explain why this interview is being cancelled…"
                value={reason}
                onChange={e => setReason(e.target.value)}
                required
              />
            </div>
            <div className="modal-actions">
              <button type="button" className="si-btn si-btn--ghost" onClick={onClose}>
                Go Back
              </button>
              <button
                type="submit"
                className="si-btn si-btn--danger"
                disabled={isLoading || !reason.trim()}
              >
                {isLoading ? 'Cancelling…' : 'Cancel Interview'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default CancelModal;
