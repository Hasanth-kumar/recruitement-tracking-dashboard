// src/features/candidates/pages/CandidateListPage.tsx
// US-4: Paginated candidate list with search, sort, filter
// US-7: Soft delete
// US-8: Single stage update
// US-10: Bulk stage update

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
import { basicAuthFetchHeaders } from '../../../shared/utils/basicAuth';
import { Role } from '../../../constants/roles';
import { USE_CANDIDATE_MOCK } from '../candidatesConfig';
import { mapApiRowToCandidate, PagedResponseApi } from '../candidateApiMappers';
import { useAuth } from '../../../shared/hooks/useAuth';

const USE_MOCK = USE_CANDIDATE_MOCK;

const PAGE_SIZE = 20;

// ── API helpers (real path) ─────────────────────────────────────

async function apiFetchCandidates(): Promise<Candidate[]> {
 const all: Candidate[] = [];
 let page = 0;
 for (;;) {
   const res = await fetch(
     `/api/candidates?page=${page}&size=100&sort=createdAt,desc`,
     { headers: basicAuthFetchHeaders(false) },
   );
   const data = await res.json();
   if (!data.success) throw new Error(data.message);
   const paged = data.data as PagedResponseApi<Record<string, unknown>>;
   for (const row of paged.content) {
     all.push(mapApiRowToCandidate(row));
   }
   if (paged.last) break;
   page += 1;
 }
 return all;
}

async function apiUpdateStage(id: string, stage: RecruitmentStage): Promise<void> {
 const res  = await fetch(`/api/candidates/${id}/stage`, {
   method:  'PUT',
   headers: basicAuthFetchHeaders(true),
   body:    JSON.stringify({ stage }),
 });
 const data = await res.json();
 if (!data.success) throw new Error(data.message);
}

async function apiDeleteCandidate(id: string): Promise<void> {
 const res  = await fetch(`/api/candidates/${id}`, {
   method:  'DELETE',
   headers: basicAuthFetchHeaders(false),
 });
 const data = await res.json();
 if (!data.success) throw new Error(data.message);
}

// ── Styles ──────────────────────────────────────────────────────
const s = {
 root:       { fontFamily: "'IBM Plex Sans', sans-serif", minHeight: '100vh', background: '#f9f9f8' },
 nav:        { background: '#fff', borderBottom: '1px solid #e4e4e0', padding: '0 2rem', height: 52, display: 'flex' as const, alignItems: 'center' as const, justifyContent: 'space-between' as const },
 navBrand:   { display: 'flex' as const, alignItems: 'center' as const, gap: 10 },
 navMark:    { width: 28, height: 28, background: '#2563eb', borderRadius: 4, display: 'flex' as const, alignItems: 'center' as const, justifyContent: 'center' as const, color: '#fff', fontSize: '0.7rem', fontWeight: 600 as const },
 navLinks:   { display: 'flex' as const, alignItems: 'center' as const, gap: 16 },
 navLink:    { fontSize: '0.85rem', color: '#2563eb', textDecoration: 'none' as const },
 navBtn:     { fontSize: '0.85rem', color: '#6b6b65', background: 'none', border: '1px solid #e4e4e0', borderRadius: 6, padding: '4px 12px', cursor: 'pointer' as const, fontFamily: 'inherit' },
 body:       { maxWidth: 1100, margin: '0 auto', padding: '2.5rem 2rem' },
 pageHeader: { display: 'flex' as const, alignItems: 'flex-start' as const, justifyContent: 'space-between' as const, marginBottom: '1.75rem', paddingBottom: '1.25rem', borderBottom: '1px solid #e4e4e0' },
 eyebrow:    { fontSize: '0.72rem', fontWeight: 500 as const, letterSpacing: '0.1em', textTransform: 'uppercase' as const, color: '#b0b0a8', marginBottom: 4 },
 pageTitle:  { fontSize: '1.35rem', fontWeight: 600 as const, color: '#1a1a18', margin: 0 },
 toolbar:    { display: 'flex' as const, alignItems: 'center' as const, justifyContent: 'space-between' as const, flexWrap: 'wrap' as const, gap: '0.75rem', marginBottom: '1rem' },
 toolbarLeft:  { display: 'flex' as const, alignItems: 'center' as const, gap: 10, flexWrap: 'wrap' as const },
 toolbarRight: { display: 'flex' as const, alignItems: 'center' as const, gap: 8 },
 bulkBar:    { display: 'flex' as const, alignItems: 'center' as const, gap: 10, padding: '8px 14px', background: '#eff4ff', border: '1px solid #bfdbfe', borderRadius: 8, marginBottom: '1rem', fontSize: '0.85rem', color: '#2563eb' },
 meta:       { fontSize: '0.8rem', color: '#b0b0a8', marginBottom: '0.75rem' },
 pagination: { display: 'flex' as const, justifyContent: 'flex-end' as const, marginTop: '1.25rem' },
};

// ── Component ───────────────────────────────────────────────────
const CandidateListPage: React.FC = () => {
 const navigate = useNavigate();
 const { hasRole } = useAuth();
 const canManageCandidates = hasRole(Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER);

 // ── Data state ────────────────────────────────────────────────
 const [allCandidates, setAllCandidates] = useState<Candidate[]>([]);
 const [loading,        setLoading]       = useState(false);

 // ── UI state ──────────────────────────────────────────────────
 const [search,      setSearch]      = useState('');
 const [filters,     setFilters]     = useState<FilterState>(EMPTY_FILTER);
 const [sortState,   setSortState]   = useState<SortState>({ field: 'createdAt', dir: 'desc' });
 const [page,        setPage]        = useState(1);
 const [selectedIds, setSelectedIds] = useState<string[]>([]);

 // ── Modal state ───────────────────────────────────────────────
 const [stageTarget,     setStageTarget]     = useState<Candidate | null>(null);
 const [stageLoading,    setStageLoading]    = useState(false);
 const [bulkModalOpen,   setBulkModalOpen]   = useState(false);
 const [bulkLoading,     setBulkLoading]     = useState(false);

 // ── Load candidates ───────────────────────────────────────────
 const loadCandidates = useCallback(async () => {
   setLoading(true);
   try {
     const data = USE_MOCK
       ? (await new Promise<Candidate[]>(r => setTimeout(() => r(mockGetAllCandidates()), 300)))
       : await apiFetchCandidates();
     setAllCandidates(data);
   } catch (e: any) {
     message.error(e?.message ?? 'Failed to load candidates.');
   } finally {
     setLoading(false);
   }
 }, []);

 useEffect(() => { loadCandidates(); }, [loadCandidates]);

 // ── Filter + Search + Sort ────────────────────────────────────
 const filtered = useMemo(() => {
   let list = [...allCandidates];

   // Search
   if (search.trim()) {
     const q = search.toLowerCase();
     list = list.filter(c =>
       c.name.toLowerCase().includes(q) ||
       c.email.toLowerCase().includes(q)
     );
   }

   // Stage filter
   if (filters.stage) list = list.filter(c => c.stage === filters.stage);

   // Position filter
   if (filters.position) list = list.filter(c => c.position === filters.position);

   // Date range
   if (filters.dateFrom) {
     const from = new Date(filters.dateFrom).getTime();
     list = list.filter(c => new Date(c.createdAt).getTime() >= from);
   }
   if (filters.dateTo) {
     const to = new Date(filters.dateTo).getTime() + 86400000; // inclusive
     list = list.filter(c => new Date(c.createdAt).getTime() <= to);
   }

   // Sort
   list.sort((a, b) => {
     let valA: string, valB: string;
     if      (sortState.field === 'name')      { valA = a.name;      valB = b.name; }
     else if (sortState.field === 'position')  { valA = a.position;  valB = b.position; }
     else if (sortState.field === 'stage')     { valA = a.stage;     valB = b.stage; }
     else                                       { valA = a.createdAt; valB = b.createdAt; }
     const cmp = valA.localeCompare(valB);
     return sortState.dir === 'asc' ? cmp : -cmp;
   });

   return list;
 }, [allCandidates, search, filters, sortState]);

 // Reset to page 1 when filters / search change
 useEffect(() => setPage(1), [search, filters, sortState]);

 // ── Pagination slice ──────────────────────────────────────────
 const paginated = useMemo(() => {
   const start = (page - 1) * PAGE_SIZE;
   return filtered.slice(start, start + PAGE_SIZE);
 }, [filtered, page]);

 // ── Sort toggle ───────────────────────────────────────────────
 const handleSortChange = (field: SortField) => {
   setSortState(prev =>
     prev.field === field
       ? { field, dir: prev.dir === 'asc' ? 'desc' : 'asc' }
       : { field, dir: 'asc' }
   );
 };

 // ── Active filter count (for badge) ──────────────────────────
 const activeFilterCount = [filters.stage, filters.position, filters.dateFrom, filters.dateTo]
   .filter(Boolean).length;

 // ── Delete ────────────────────────────────────────────────────
 const handleDelete = async (id: string) => {
   try {
     if (USE_MOCK) {
       mockDeleteCandidate(id);
     } else {
       await apiDeleteCandidate(id);
     }
     setAllCandidates(prev => prev.filter(c => c.id !== id));
     setSelectedIds(prev => prev.filter(x => x !== id));
     message.success('Candidate deleted.');
   } catch (e: any) {
     message.error(e?.message ?? 'Failed to delete candidate.');
   }
 };

 // ── Single stage update ───────────────────────────────────────
 const handleStageConfirm = async (candidateId: string, newStage: RecruitmentStage) => {
   setStageLoading(true);
   try {
     if (USE_MOCK) {
       await new Promise(r => setTimeout(r, 400));
       mockUpdateCandidate(candidateId, { stage: newStage });
     } else {
       await apiUpdateStage(candidateId, newStage);
     }
     setAllCandidates(prev =>
       prev.map(c => c.id === candidateId ? { ...c, stage: newStage } : c)
     );
     message.success('Stage updated.');
     setStageTarget(null);
   } catch (e: any) {
     message.error(e?.message ?? 'Failed to update stage.');
   } finally {
     setStageLoading(false);
   }
 };

 // ── Bulk stage update ─────────────────────────────────────────
 const handleBulkConfirm = async (ids: string[], newStage: RecruitmentStage) => {
   setBulkLoading(true);
   try {
     if (USE_MOCK) {
       await new Promise(r => setTimeout(r, 500));
       ids.forEach(id => mockUpdateCandidate(id, { stage: newStage }));
     } else {
       await Promise.all(ids.map(id => apiUpdateStage(id, newStage)));
     }
     setAllCandidates(prev =>
       prev.map(c => ids.includes(c.id) ? { ...c, stage: newStage } : c)
     );
     message.success(`${ids.length} candidate${ids.length !== 1 ? 's' : ''} updated.`);
     setSelectedIds([]);
     setBulkModalOpen(false);
   } catch (e: any) {
     message.error(e?.message ?? 'Failed to bulk update stages.');
   } finally {
     setBulkLoading(false);
   }
 };

 const selectedCandidates = allCandidates.filter(c => selectedIds.includes(c.id));

 const logout = () => {
   ['rts_token', 'rts_role', 'rts_user', 'rts_basic_principal'].forEach(k => {
     localStorage.removeItem(k); sessionStorage.removeItem(k);
   });
   window.location.href = '/login';
 };

 // ── Render ────────────────────────────────────────────────────
 return (
   <div style={s.root}>

     {/* Nav */}
     <div style={s.nav}>
       <div style={s.navBrand}>
         <div style={s.navMark}>RTS</div>
         <span style={{ fontSize: '0.85rem', fontWeight: 500, color: '#6b6b65' }}>
           Recruitment Tracking System
         </span>
       </div>
       <div style={s.navLinks}>
         <a href="/dashboard" style={s.navLink}>Dashboard</a>
         {(localStorage.getItem('rts_role') ?? sessionStorage.getItem('rts_role')) === Role.ADMIN && (
           <a href="/admin/users" style={s.navLink}>Users</a>
         )}
         <a href="/profile"   style={s.navLink}>Profile</a>
         <button onClick={logout} style={s.navBtn}>Sign out</button>
       </div>
     </div>

     <div style={s.body}>

       {/* Page header */}
       <div style={s.pageHeader}>
         <div>
           <p style={s.eyebrow}>Recruitment</p>
           <h2 style={s.pageTitle}>Candidates</h2>
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

       {/* Toolbar — search + filters */}
       <div style={s.toolbar}>
         <div style={s.toolbarLeft}>
           <CandidateSearchBar value={search} onChange={setSearch} />
           <CandidateFilterPanel
             filters={filters}
             onChange={setFilters}
             onReset={() => setFilters(EMPTY_FILTER)}
             activeCount={activeFilterCount}
           />
         </div>
         <div style={s.toolbarRight}>
           {/* Reload */}
           <Button
             icon={<AppstoreOutlined />}
             onClick={loadCandidates}
             loading={loading}
             style={{ fontSize: '0.825rem' }}
           >
             Refresh
           </Button>
         </div>
       </div>

       {/* Bulk action bar — appears when rows are selected */}
      {canManageCandidates && selectedIds.length > 0 && (
         <div style={s.bulkBar}>
           <span>
             <strong>{selectedIds.length}</strong> candidate{selectedIds.length !== 1 ? 's' : ''} selected
           </span>
           <Button
             size="small"
             type="primary"
             onClick={() => setBulkModalOpen(true)}
             style={{ marginLeft: 8 }}
           >
             Update stage
           </Button>
           <Button
             size="small"
             onClick={() => setSelectedIds([])}
             style={{ marginLeft: 4 }}
           >
             Clear selection
           </Button>
         </div>
       )}

       {/* Result count */}
       <p style={s.meta}>
         {loading
           ? 'Loading…'
           : `${filtered.length} candidate${filtered.length !== 1 ? 's' : ''}${
               search || activeFilterCount ? ' matching filters' : ''
             }`
         }
       </p>

       {/* Table */}
       <CandidateTable
         candidates={paginated}
         sortState={sortState}
         onSortChange={handleSortChange}
         onDelete={handleDelete}
         onStageUpdate={c => setStageTarget(c)}
         selectedIds={selectedIds}
         onSelectChange={setSelectedIds}
        canManageCandidates={canManageCandidates}
       />

       {/* Pagination — US-4: 20 per page */}
       {filtered.length > PAGE_SIZE && (
         <div style={s.pagination}>
           <Pagination
             current={page}
             total={filtered.length}
             pageSize={PAGE_SIZE}
             onChange={p => { setPage(p); window.scrollTo({ top: 0, behavior: 'smooth' }); }}
             showSizeChanger={false}
             showTotal={total => `${total} candidates`}
           />
         </div>
       )}
     </div>

     {/* Single stage update modal */}
     <StageUpdateModal
       candidate={stageTarget}
       onConfirm={handleStageConfirm}
       onClose={() => setStageTarget(null)}
       loading={stageLoading}
     />

     {/* Bulk stage update modal */}
     <BulkStageModal
       candidates={selectedCandidates}
       open={bulkModalOpen}
       onConfirm={handleBulkConfirm}
       onClose={() => setBulkModalOpen(false)}
       loading={bulkLoading}
     />
   </div>
 );
};

export default CandidateListPage;
 