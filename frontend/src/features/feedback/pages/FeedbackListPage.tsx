import React, { useEffect, useState, useMemo } from 'react';
import { Button, Input, Select, Spin, Alert } from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  InterviewResponseDto,
  InterviewRound,
  InterviewStatus,
  apiFetchInterviewSchedule,
} from '../feedbackApi';
import '../../../App.css';

type SortField = 'candidateId' | 'round' | 'dateTime' | 'status';
type SortDir = 'asc' | 'desc';

interface SortState {
  field: SortField;
  dir: SortDir;
}

const ROUND_LABELS: Record<InterviewRound, string> = {
  ROUND_1: 'Round 1',
  ROUND_2: 'Round 2',
};

const STATUS_LABELS: Record<InterviewStatus, string> = {
  SCHEDULED: 'Scheduled',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

const STATUS_COLORS: Record<InterviewStatus, string> = {
  SCHEDULED: '#d97706',
  COMPLETED: '#16a34a',
  CANCELLED: '#dc2626',
};

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const SortIcon: React.FC<{ field: SortField; sortState: SortState }> = ({
  field,
  sortState,
}) => {
  if (sortState.field !== field)
    return <span style={{ color: 'var(--text-muted)', marginLeft: 4 }}>↕</span>;
  return sortState.dir === 'asc' ? (
    <ArrowUpOutlined
      style={{ color: 'var(--accent)', marginLeft: 4, fontSize: '0.7rem' }}
    />
  ) : (
    <ArrowDownOutlined
      style={{ color: 'var(--accent)', marginLeft: 4, fontSize: '0.7rem' }}
    />
  );
};

const FeedbackListPage: React.FC = () => {
  const navigate = useNavigate();
  const [interviews, setInterviews] = useState<InterviewResponseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [roundFilter, setRoundFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [sortState, setSortState] = useState<SortState>({
    field: 'dateTime',
    dir: 'desc',
  });

  const loadInterviews = async () => {
    setLoading(true);
    setError(null);
    try {
      const now = new Date();
      const threeMonthsAgo = new Date(now);
      threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
      const oneMonthAhead = new Date(now);
      oneMonthAhead.setMonth(oneMonthAhead.getMonth() + 1);

      const data = await apiFetchInterviewSchedule(
        threeMonthsAgo.toISOString(),
        oneMonthAhead.toISOString(),
      );
      setInterviews(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load interviews.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInterviews();
  }, []);

  const handleSortChange = (field: SortField) => {
    setSortState((prev) =>
      prev.field === field
        ? { field, dir: prev.dir === 'asc' ? 'desc' : 'asc' }
        : { field, dir: 'asc' },
    );
  };

  const filtered = useMemo(() => {
    let list = interviews;
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (i) =>
          i.candidateId.toLowerCase().includes(q) ||
          i.interviewerUsernames.some((u) => u.toLowerCase().includes(q)),
      );
    }
    if (roundFilter) {
      list = list.filter((i) => i.round === roundFilter);
    }
    if (statusFilter) {
      list = list.filter((i) => i.status === statusFilter);
    }

    return [...list].sort((a, b) => {
      const dir = sortState.dir === 'asc' ? 1 : -1;
      switch (sortState.field) {
        case 'candidateId':
          return dir * a.candidateId.localeCompare(b.candidateId);
        case 'round':
          return dir * a.round.localeCompare(b.round);
        case 'dateTime':
          return (
            dir *
            (new Date(a.dateTime).getTime() - new Date(b.dateTime).getTime())
          );
        case 'status':
          return dir * a.status.localeCompare(b.status);
        default:
          return 0;
      }
    });
  }, [interviews, search, roundFilter, statusFilter, sortState]);

  const canSubmitFeedback = (interview: InterviewResponseDto): boolean => {
    if (interview.status === 'CANCELLED') return false;
    const end = new Date(interview.dateTime);
    end.setMinutes(end.getMinutes() + (interview.durationMinutes ?? 60));
    return new Date() >= end;
  };

  const handleGiveFeedback = (interview: InterviewResponseDto) => {
    navigate(
      `/feedback/new?interviewId=${interview.id}&candidateId=${interview.candidateId}&round=${interview.round}`,
    );
  };

  return (
    <div className="feedback-root">
      <div className="feedback-list-header">
        <div className="feedback-eyebrow">FEEDBACK</div>
        <h2>Interview Feedback</h2>
      </div>

      <div className="fl-filter-bar">
        <Input
          prefix={<SearchOutlined style={{ color: 'var(--text-muted)' }} />}
          placeholder="Search by candidate ID or interviewer..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
          className="fl-search-input"
        />
        <Select
          placeholder="All rounds"
          value={roundFilter || undefined}
          onChange={(v) => setRoundFilter(v ?? '')}
          allowClear
          style={{ minWidth: 140 }}
          options={[
            { value: 'ROUND_1', label: 'Round 1' },
            { value: 'ROUND_2', label: 'Round 2' },
          ]}
        />
        <Select
          placeholder="All statuses"
          value={statusFilter || undefined}
          onChange={(v) => setStatusFilter(v ?? '')}
          allowClear
          style={{ minWidth: 140 }}
          options={[
            { value: 'SCHEDULED', label: 'Scheduled' },
            { value: 'COMPLETED', label: 'Completed' },
            { value: 'CANCELLED', label: 'Cancelled' },
          ]}
        />
        <Button
          icon={<ReloadOutlined />}
          onClick={loadInterviews}
          className="fl-refresh-btn"
        >
          Refresh
        </Button>
      </div>

      {error && (
        <Alert
          type="error"
          message={error}
          showIcon
          closable
          onClose={() => setError(null)}
          style={{ marginBottom: 16 }}
        />
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem 0' }}>
          <Spin size="large" />
        </div>
      ) : (
        <>
          <p className="fl-meta">
            {filtered.length} interview{filtered.length !== 1 ? 's' : ''}
          </p>

          <div className="fl-table-wrap">
            <table className="fl-table">
              <thead>
                <tr>
                  <th
                    className="fl-th-sort"
                    onClick={() => handleSortChange('candidateId')}
                  >
                    CANDIDATE ID{' '}
                    <SortIcon field="candidateId" sortState={sortState} />
                  </th>
                  <th
                    className="fl-th-sort"
                    onClick={() => handleSortChange('round')}
                  >
                    ROUND <SortIcon field="round" sortState={sortState} />
                  </th>
                  <th
                    className="fl-th-sort"
                    onClick={() => handleSortChange('dateTime')}
                  >
                    DATE & TIME{' '}
                    <SortIcon field="dateTime" sortState={sortState} />
                  </th>
                  <th>INTERVIEWERS</th>
                  <th
                    className="fl-th-sort"
                    onClick={() => handleSortChange('status')}
                  >
                    STATUS <SortIcon field="status" sortState={sortState} />
                  </th>
                  <th>ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="fl-table-empty">
                      No interviews found.
                    </td>
                  </tr>
                ) : (
                  filtered.map((interview) => (
                    <tr key={interview.id}>
                      <td>
                        <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                          {interview.candidateId.slice(0, 8)}…
                        </span>
                      </td>
                      <td>{ROUND_LABELS[interview.round]}</td>
                      <td>{formatDateTime(interview.dateTime)}</td>
                      <td>
                        {interview.interviewerUsernames.length > 0
                          ? interview.interviewerUsernames.join(', ')
                          : '—'}
                      </td>
                      <td>
                        <span
                          style={{
                            color: STATUS_COLORS[interview.status],
                            fontWeight: 600,
                            fontSize: '0.8rem',
                          }}
                        >
                          {STATUS_LABELS[interview.status]}
                        </span>
                      </td>
                      <td>
                        {canSubmitFeedback(interview) ? (
                          <Button
                            type="primary"
                            size="small"
                            onClick={() => handleGiveFeedback(interview)}
                          >
                            Give Feedback
                          </Button>
                        ) : interview.status === 'CANCELLED' ? (
                          <span
                            style={{
                              color: 'var(--text-muted)',
                              fontSize: '0.8rem',
                            }}
                          >
                            Cancelled
                          </span>
                        ) : (
                          <span
                            style={{
                              color: 'var(--text-muted)',
                              fontSize: '0.8rem',
                            }}
                          >
                            Not yet ended
                          </span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
};

export default FeedbackListPage;
