import React, { useEffect, useState, useMemo } from 'react';
import { Button, Input, Select } from 'antd';
import { SearchOutlined, ReloadOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { mockGetAllCandidates } from '../../candidates/candidateMock';
import { Candidate, RecruitmentStage, POSITIONS } from '../../candidates/candidateTypes';
import StatusBadge from '../../../shared/components/StatusBadge';
import '../../../App.css';

const USE_MOCK = true;
const ELIGIBLE_STAGES: RecruitmentStage[] = ['R1_CLEARED', 'R2_CLEARED'];

type SortField = 'name' | 'position' | 'experience' | 'stage' | 'createdAt';
type SortDir = 'asc' | 'desc';

interface SortState {
  field: SortField;
  dir: SortDir;
}

const SortIcon: React.FC<{ field: SortField; sortState: SortState }> = ({ field, sortState }) => {
  if (sortState.field !== field) return <span style={{ color: 'var(--text-muted)', marginLeft: 4 }}>↕</span>;
  return sortState.dir === 'asc'
    ? <ArrowUpOutlined style={{ color: 'var(--accent)', marginLeft: 4, fontSize: '0.7rem' }} />
    : <ArrowDownOutlined style={{ color: 'var(--accent)', marginLeft: 4, fontSize: '0.7rem' }} />;
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    day: '2-digit', month: 'short', year: 'numeric',
  });
}

const FeedbackListPage: React.FC = () => {
  const navigate = useNavigate();
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [search, setSearch] = useState('');
  const [stageFilter, setStageFilter] = useState<string>('');
  const [positionFilter, setPositionFilter] = useState<string>('');
  const [sortState, setSortState] = useState<SortState>({ field: 'name', dir: 'asc' });

  const handleSortChange = (field: SortField) => {
    setSortState(prev =>
      prev.field === field
        ? { field, dir: prev.dir === 'asc' ? 'desc' : 'asc' }
        : { field, dir: 'asc' },
    );
  };

  const loadCandidates = () => {
    if (USE_MOCK) {
      const eligible = mockGetAllCandidates().filter(c => ELIGIBLE_STAGES.includes(c.stage));
      setCandidates(eligible);
    }
  };

  useEffect(() => { loadCandidates(); }, []);

  const filtered = useMemo(() => {
    let list = candidates;
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(c =>
        c.name.toLowerCase().includes(q) || c.email.toLowerCase().includes(q),
      );
    }
    if (stageFilter) {
      list = list.filter(c => c.stage === stageFilter);
    }
    if (positionFilter) {
      list = list.filter(c => c.position === positionFilter);
    }

    const sorted = [...list].sort((a, b) => {
      const dir = sortState.dir === 'asc' ? 1 : -1;
      switch (sortState.field) {
        case 'name':
          return dir * a.name.localeCompare(b.name);
        case 'position':
          return dir * a.position.localeCompare(b.position);
        case 'experience':
          return dir * (Number(a.experience) - Number(b.experience));
        case 'stage':
          return dir * a.stage.localeCompare(b.stage);
        case 'createdAt':
          return dir * (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        default:
          return 0;
      }
    });

    return sorted;
  }, [candidates, search, stageFilter, positionFilter, sortState]);

  const handleGiveFeedback = (c: Candidate) => {
    const round = c.stage === 'R2_CLEARED' ? 2 : 1;
    navigate(`/feedback/new?candidateId=${c.id}&candidateName=${encodeURIComponent(c.name)}&round=${round}`);
  };

  return (
    <div className="feedback-root">
      <div className="feedback-list-header">
        <div className="feedback-eyebrow">FEEDBACK</div>
        <h2>Submit Feedback</h2>
      </div>

      <div className="fl-filter-bar">
        <Input
          prefix={<SearchOutlined style={{ color: 'var(--text-muted)' }} />}
          placeholder="Search by name or email..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          allowClear
          className="fl-search-input"
        />
        <Select
          placeholder="All stages"
          value={stageFilter || undefined}
          onChange={v => setStageFilter(v ?? '')}
          allowClear
          style={{ minWidth: 140 }}
          options={ELIGIBLE_STAGES.map(s => ({
            value: s,
            label: s === 'R1_CLEARED' ? 'Round 1 Cleared' : 'Round 2 Cleared',
          }))}
        />
        <Select
          placeholder="All positions"
          value={positionFilter || undefined}
          onChange={v => setPositionFilter(v ?? '')}
          allowClear
          style={{ minWidth: 150 }}
          options={POSITIONS.map(p => ({ value: p, label: p }))}
        />
        <Button
          icon={<ReloadOutlined />}
          onClick={loadCandidates}
          className="fl-refresh-btn"
        >
          Refresh
        </Button>
      </div>

      <p className="fl-meta">
        {filtered.length} candidate{filtered.length !== 1 ? 's' : ''} · Only Round 1 cleared and Round 2 cleared are shown
      </p>

      <div className="fl-table-wrap">
        <table className="fl-table">
          <thead>
            <tr>
              <th className="fl-th-sort" onClick={() => handleSortChange('name')}>
                CANDIDATE <SortIcon field="name" sortState={sortState} />
              </th>
              <th className="fl-th-sort" onClick={() => handleSortChange('position')}>
                POSITION <SortIcon field="position" sortState={sortState} />
              </th>
              <th className="fl-th-sort" onClick={() => handleSortChange('experience')}>
                EXPERIENCE <SortIcon field="experience" sortState={sortState} />
              </th>
              <th className="fl-th-sort" onClick={() => handleSortChange('stage')}>
                STAGE <SortIcon field="stage" sortState={sortState} />
              </th>
              <th className="fl-th-sort" onClick={() => handleSortChange('createdAt')}>
                APPLIED <SortIcon field="createdAt" sortState={sortState} />
              </th>
              <th>ACTIONS</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={6} className="fl-table-empty">
                  No eligible candidates found.
                </td>
              </tr>
            ) : (
              filtered.map(c => (
                <tr key={c.id}>
                  <td>
                    <div className="fl-candidate-cell">
                      <div className="fl-candidate-avatar">
                        {c.name.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <div className="fl-candidate-name">{c.name}</div>
                        <div className="fl-candidate-email">{c.email}</div>
                      </div>
                    </div>
                  </td>
                  <td>{c.position}</td>
                  <td>{c.experience}</td>
                  <td><StatusBadge stage={c.stage} /></td>
                  <td>{formatDate(c.createdAt)}</td>
                  <td>
                    <Button
                      type="primary"
                      size="small"
                      onClick={() => handleGiveFeedback(c)}
                    >
                      Give Feedback
                    </Button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default FeedbackListPage;
