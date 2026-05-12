import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Button, message, Pagination } from 'antd';
import { PlusOutlined, AppstoreOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

import CandidateSearchBar from '../components/CandidateSearchBar';
import CandidateFilterPanel, { FilterState, EMPTY_FILTER } from '../components/CandidateFilterPanel';
import CandidateTable, { SortField, SortState } from '../components/CandidateTable';
import StageUpdateModal from '../components/StageUpdateModal';
import BulkStageModal from '../components/BulkStageModal';

import { Candidate, RecruitmentStage } from '../candidateTypes';
import {
  mockGetAllCandidates,
  mockUpdateCandidate,
  mockDeleteCandidate,
} from '../candidateMock';
import { bearerFetchHeaders } from '../../../shared/utils/basicAuth';
import { Role } from '../../../constants/roles';
import { USE_CANDIDATE_MOCK } from '../candidatesConfig';
import { mapApiRowToCandidate, PagedResponseApi } from '../candidateApiMappers';
import { useAuth } from '../../../shared/hooks/useAuth';
import '../../../App.css';

const USE_MOCK = USE_CANDIDATE_MOCK;

const PAGE_SIZE = 20;

// ── API helpers (real path) ─────────────────────────────────────

async function apiFetchCandidatesPage(
  pageIndexZero: number,
  sort: SortState,
  searchQ: string,
  filters: FilterState,
): Promise<{ rows: Candidate[]; total: number }> {
  const params = new URLSearchParams();
  params.set('page', String(pageIndexZero));
  params.set('size', String(PAGE_SIZE));
  params.append('sort', `${sort.field},${sort.dir}`);
  if (filters.stage) params.set('stage', filters.stage);
  if (filters.position) params.set('position', filters.position);
  if (searchQ.trim()) params.set('search', searchQ.trim());
  if (filters.dateFrom) params.set('createdFrom', filters.dateFrom);
  if (filters.dateTo) params.set('createdTo', filters.dateTo);

  const res = await fetch(`/api/candidates?${params}`, {
    headers: bearerFetchHeaders(false),
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message);
  const paged = data.data as PagedResponseApi<Record<string, unknown>>;
  const rows = paged.content.map(row => mapApiRowToCandidate(row));
  return { rows, total: paged.totalElements };
}

async function apiUpdateStage(id: string, stage: RecruitmentStage): Promise<void> {
  const res = await fetch(`/api/candidates/${id}/stage`, {
    method: 'PUT',
    headers: bearerFetchHeaders(true),
    body: JSON.stringify({ stage }),
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message);
}

async function apiBulkUpdateStage(ids: string[], stage: RecruitmentStage): Promise<void> {
  const res = await fetch('/api/candidates/bulk-stage', {
    method: 'POST',
    headers: bearerFetchHeaders(true),
    body: JSON.stringify({ candidateIds: ids, stage }),
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message);
}

async function apiDeleteCandidate(id: string): Promise<void> {
  const res = await fetch(`/api/candidates/${id}`, {
    method: 'DELETE',
    headers: bearerFetchHeaders(false),
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message);
}

// ── Component ───────────────────────────────────────────────────
const CandidateListPage: React.FC = () => {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const canManageCandidates = hasRole(Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER);

  // ── Data state ────────────────────────────────────────────────
  const [allCandidates, setAllCandidates] = useState<Candidate[]>([]);
  const [serverRows, setServerRows] = useState<Candidate[]>([]);
  const [serverTotal, setServerTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  // ── UI state ──────────────────────────────────────────────────
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState<FilterState>(EMPTY_FILTER);
  const [sortState, setSortState] = useState<SortState>({ field: 'createdAt', dir: 'desc' });
  const [page, setPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  // ── Modal state ───────────────────────────────────────────────
  const [stageTarget, setStageTarget] = useState<Candidate | null>(null);
  const [stageLoading, setStageLoading] = useState(false);
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkLoading, setBulkLoading] = useState(false);

  const loadMockCandidates = useCallback(async () => {
    setLoading(true);
    try {
      const data = await new Promise<Candidate[]>(r =>
        setTimeout(() => r(mockGetAllCandidates()), 300),
      );
      setAllCandidates(data);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : 'Failed to load candidates.');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadServerCandidates = useCallback(async () => {
    setLoading(true);
    try {
      const { rows, total } = await apiFetchCandidatesPage(page - 1, sortState, search, filters);
      setServerRows(rows);
      setServerTotal(total);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : 'Failed to load candidates.');
    } finally {
      setLoading(false);
    }
  }, [page, sortState, search, filters]);

  useEffect(() => {
    if (USE_MOCK) void loadMockCandidates();
  }, [loadMockCandidates]);

  useEffect(() => {
    if (!USE_MOCK) void loadServerCandidates();
  }, [loadServerCandidates]);

  useEffect(() => {
    setSelectedIds([]);
  }, [search, filters, sortState]);

  // ── Filter + Search + Sort (mock — client-side) ───────────────
  const filtered = useMemo(() => {
    let list = [...allCandidates];

    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        c => c.name.toLowerCase().includes(q) || c.email.toLowerCase().includes(q),
      );
    }

    if (filters.stage) list = list.filter(c => c.stage === filters.stage);

    if (filters.position) list = list.filter(c => c.position === filters.position);

    if (filters.dateFrom) {
      const from = new Date(filters.dateFrom).getTime();
      list = list.filter(c => new Date(c.createdAt).getTime() >= from);
    }
    if (filters.dateTo) {
      const to = new Date(filters.dateTo).getTime() + 86400000;
      list = list.filter(c => new Date(c.createdAt).getTime() <= to);
    }

    list.sort((a, b) => {
      let valA: string;
      let valB: string;
      if (sortState.field === 'name') {
        valA = a.name;
        valB = b.name;
      } else if (sortState.field === 'position') {
        valA = a.position;
        valB = b.position;
      } else if (sortState.field === 'stage') {
        valA = a.stage;
        valB = b.stage;
      } else {
        valA = a.createdAt;
        valB = b.createdAt;
      }
      const cmp = valA.localeCompare(valB);
      return sortState.dir === 'asc' ? cmp : -cmp;
    });

    return list;
  }, [allCandidates, search, filters, sortState]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  const handleSortChange = (field: SortField) => {
    setSortState(prev =>
      prev.field === field
        ? { field, dir: prev.dir === 'asc' ? 'desc' : 'asc' }
        : { field, dir: 'asc' },
    );
    setPage(1);
  };

  const activeFilterCount = [filters.stage, filters.position, filters.dateFrom, filters.dateTo].filter(Boolean).length;

  const handleDelete = async (id: string) => {
    try {
      if (USE_MOCK) {
        mockDeleteCandidate(id);
      } else {
        await apiDeleteCandidate(id);
      }

      if (USE_MOCK) {
        setAllCandidates(prev => prev.filter(c => c.id !== id));
      } else {
        await loadServerCandidates();
      }
      setSelectedIds(prev => prev.filter(x => x !== id));
      message.success('Candidate deleted.');
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : 'Failed to delete candidate.');
    }
  };

  const handleStageConfirm = async (candidateId: string, newStage: RecruitmentStage) => {
    setStageLoading(true);
    try {
      if (USE_MOCK) {
        await new Promise(r => setTimeout(r, 400));
        mockUpdateCandidate(candidateId, { stage: newStage });
        setAllCandidates(prev =>
          prev.map(c => (c.id === candidateId ? { ...c, stage: newStage } : c)),
        );
      } else {
        await apiUpdateStage(candidateId, newStage);
        await loadServerCandidates();
      }
      message.success('Stage updated.');
      setStageTarget(null);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : 'Failed to update stage.');
    } finally {
      setStageLoading(false);
    }
  };

  const handleBulkConfirm = async (ids: string[], newStage: RecruitmentStage) => {
    setBulkLoading(true);
    try {
      if (USE_MOCK) {
        await new Promise(r => setTimeout(r, 500));
        ids.forEach(id => mockUpdateCandidate(id, { stage: newStage }));
        setAllCandidates(prev =>
          prev.map(c => (ids.includes(c.id) ? { ...c, stage: newStage } : c)),
        );
      } else {
        await apiBulkUpdateStage(ids, newStage);
        await loadServerCandidates();
      }
      message.success(`${ids.length} candidate${ids.length !== 1 ? 's' : ''} updated.`);
      setSelectedIds([]);
      setBulkModalOpen(false);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : 'Failed to bulk update stages.');
    } finally {
      setBulkLoading(false);
    }
  };

  const tableRows = USE_MOCK ? paginated : serverRows;
  const totalMatching = USE_MOCK ? filtered.length : serverTotal;
  const metaSuffix = search.trim() || activeFilterCount > 0 ? ' matching filters' : '';

  const selectedCandidates = USE_MOCK
    ? allCandidates.filter(c => selectedIds.includes(c.id))
    : serverRows.filter(c => selectedIds.includes(c.id));

  return (
    <>
      <div className="candidates-root">
        <div className="candidates-page-header">
          <div>
            <p className="candidates-eyebrow">Recruitment</p>
            <h2 className="candidates-page-title">Candidates</h2>
          </div>
          {canManageCandidates && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              size="large"
              onClick={() => navigate('/candidates/new')}
            >
              Add candidate
            </Button>
          )}
        </div>

        <div className="candidates-toolbar">
          <div className="candidates-toolbar-left">
            <CandidateSearchBar
              value={search}
              onChange={v => {
                setSearch(v);
                setPage(1);
              }}
            />
            <CandidateFilterPanel
              filters={filters}
              onChange={f => {
                setFilters(f);
                setPage(1);
              }}
              onReset={() => {
                setFilters(EMPTY_FILTER);
                setPage(1);
              }}
              activeCount={activeFilterCount}
            />
          </div>
          <div className="candidates-toolbar-right">
            <Button
              icon={<AppstoreOutlined />}
              onClick={() => {
                void (USE_MOCK ? loadMockCandidates() : loadServerCandidates());
              }}
              loading={loading}
            >
              Refresh
            </Button>
          </div>
        </div>

        {canManageCandidates && selectedIds.length > 0 && (
          <div className="candidates-bulk-bar">
            <span>
              <strong>{selectedIds.length}</strong> candidate{selectedIds.length !== 1 ? 's' : ''} selected
            </span>
            <Button size="small" type="primary" onClick={() => setBulkModalOpen(true)}>
              Update stage
            </Button>
            <Button size="small" onClick={() => setSelectedIds([])}>
              Clear selection
            </Button>
          </div>
        )}

        <p className="candidates-meta">
          {loading
            ? 'Loading…'
            : `${totalMatching} candidate${totalMatching !== 1 ? 's' : ''}${metaSuffix}`}
        </p>

        <CandidateTable
          candidates={tableRows}
          sortState={sortState}
          onSortChange={handleSortChange}
          onDelete={handleDelete}
          onStageUpdate={c => setStageTarget(c)}
          selectedIds={selectedIds}
          onSelectChange={setSelectedIds}
          canManageCandidates={canManageCandidates}
        />

        {totalMatching > PAGE_SIZE && (
          <div className="candidates-pagination">
            <Pagination
              current={page}
              total={totalMatching}
              pageSize={PAGE_SIZE}
              onChange={p => {
                setPage(p);
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
              showSizeChanger={false}
              showTotal={total => `${total} candidates`}
            />
          </div>
        )}
      </div>

      <StageUpdateModal
        candidate={stageTarget}
        onConfirm={handleStageConfirm}
        onClose={() => setStageTarget(null)}
        loading={stageLoading}
      />

      <BulkStageModal
        candidates={selectedCandidates}
        open={bulkModalOpen}
        onConfirm={handleBulkConfirm}
        onClose={() => setBulkModalOpen(false)}
        loading={bulkLoading}
      />
    </>
  );
};

export default CandidateListPage;
