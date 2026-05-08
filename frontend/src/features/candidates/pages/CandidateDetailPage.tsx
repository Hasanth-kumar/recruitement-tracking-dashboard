import React, { useEffect, useState } from 'react';
import { Button, message } from 'antd';
import { ArrowLeftOutlined, EditOutlined, DownloadOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { Candidate, RecruitmentStage } from '../candidateTypes';
import { basicAuthFetchHeaders } from '../../../shared/utils/basicAuth';
import { Role } from '../../../constants/roles';
import { USE_CANDIDATE_MOCK } from '../candidatesConfig';
import { mapApiRowToCandidate } from '../candidateApiMappers';
import { mockGetCandidate } from '../candidateMock';
import StatusBadge from '../../../shared/components/StatusBadge';
import AuthenticatedCandidateAvatar from '../components/AuthenticatedCandidateAvatar';
import { useAuth } from '../../../shared/hooks/useAuth';

type StageHistoryItem = {
  stage: RecruitmentStage;
  changedAt: string;
  changedBy: string;
};

const s = {
  root: { fontFamily: "'IBM Plex Sans', sans-serif", minHeight: '100vh', background: 'var(--bg)' },
  nav: {
    background: 'var(--surface)',
    borderBottom: '1px solid var(--border)',
    padding: '0 2rem',
    height: 52,
    display: 'flex' as const,
    alignItems: 'center' as const,
    justifyContent: 'space-between' as const,
  },
  navBrand: { display: 'flex' as const, alignItems: 'center' as const, gap: 10 },
  navMark: {
    width: 28,
    height: 28,
    background: '#2563eb',
    borderRadius: 4,
    display: 'flex' as const,
    alignItems: 'center' as const,
    justifyContent: 'center' as const,
    color: '#fff',
    fontSize: '0.7rem',
    fontWeight: 600 as const,
  },
  navLinks: { display: 'flex' as const, alignItems: 'center' as const, gap: 16 },
  navLink: { fontSize: '0.85rem', color: 'var(--accent)', textDecoration: 'none' as const },
  navBtn: {
    fontSize: '0.85rem',
    color: 'var(--text-secondary)',
    background: 'none',
    border: '1px solid var(--border)',
    borderRadius: 6,
    padding: '4px 12px',
    cursor: 'pointer' as const,
    fontFamily: 'inherit',
  },
  body: { maxWidth: 720, margin: '0 auto', padding: '2.5rem 2rem' },
  backBtn: {
    display: 'flex' as const,
    alignItems: 'center' as const,
    gap: 6,
    fontSize: '0.85rem',
    color: 'var(--text-secondary)',
    background: 'none',
    border: 'none',
    cursor: 'pointer' as const,
    padding: 0,
    marginBottom: '1.5rem',
    fontFamily: 'inherit',
  },
  card: {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: 12,
    padding: '1.5rem',
    marginBottom: '1rem',
  },
  title: { fontSize: '1.25rem', fontWeight: 600 as const, color: 'var(--text-primary)', margin: '0 0 0.25rem' },
  meta: { fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '1.25rem' },
  row: { marginBottom: '0.75rem', fontSize: '0.9rem' },
  label: { color: 'var(--text-secondary)', fontWeight: 500 as const, marginRight: 8 },
  actions: { display: 'flex' as const, gap: 8, flexWrap: 'wrap' as const, marginTop: '1.25rem' },
  head: { display: 'flex' as const, alignItems: 'center' as const, gap: 16, marginBottom: '1rem' },
  timelineTitle: { fontSize: '1rem', fontWeight: 600 as const, color: 'var(--text-primary)', margin: '0 0 0.75rem' },
  timelineItem: { borderLeft: '2px solid var(--accent-subtle)', paddingLeft: '0.75rem', marginBottom: '0.85rem' },
  timelineMeta: { fontSize: '0.8rem', color: 'var(--text-secondary)' },
};

async function apiGetCandidate(id: string): Promise<Candidate> {
  const res = await fetch(`/api/candidates/${id}`, { headers: basicAuthFetchHeaders(false) });
  const data = await res.json();
  if (!data.success) throw new Error(data.message);
  return mapApiRowToCandidate(data.data as Record<string, unknown>);
}

async function apiGetStageHistory(id: string): Promise<StageHistoryItem[]> {
  const res = await fetch(`/api/candidates/${id}/stage-history`, { headers: basicAuthFetchHeaders(false) });
  const data = await res.json();
  if (!data.success) throw new Error(data.message);
  return (data.data as StageHistoryItem[]) ?? [];
}

const CandidateDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const canManageCandidates = hasRole(Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER);
  const [candidate, setCandidate] = useState<Candidate | null>(null);
  const [stageHistory, setStageHistory] = useState<StageHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      try {
        let c: Candidate | null = null;
        if (USE_CANDIDATE_MOCK) {
          await new Promise(r => setTimeout(r, 200));
          c = mockGetCandidate(id);
          setStageHistory([]);
        } else {
          const [candidateResult, historyResult] = await Promise.all([
            apiGetCandidate(id),
            apiGetStageHistory(id),
          ]);
          c = candidateResult;
          setStageHistory(historyResult);
        }
        if (cancelled) return;
        if (!c) throw new Error('Candidate not found.');
        setCandidate(c);
      } catch (e: any) {
        if (!cancelled) {
          message.error(e?.message ?? 'Failed to load candidate.');
          navigate('/candidates');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => {
      cancelled = true;
    };
  }, [id, navigate]);

  const downloadResume = async () => {
    if (!id) return;
    try {
      const res = await fetch(`/api/candidates/${id}/resume`, { headers: basicAuthFetchHeaders(false) });
      if (!res.ok) throw new Error('Could not download résumé.');
      const blob = await res.blob();
      const cd = res.headers.get('Content-Disposition');
      let filename = 'resume';
      if (cd) {
        const m = /filename\*?=(?:UTF-8'')?["']?([^"';]+)/i.exec(cd);
        if (m) filename = decodeURIComponent(m[1]!.replace(/["']/g, ''));
      }
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e: any) {
      message.error(e?.message ?? 'Download failed.');
    }
  };

  if (loading || !candidate) {
    return (
      <div style={{ ...s.root, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <p style={{ color: 'var(--text-muted)' }}>Loading…</p>
      </div>
    );
  }

  return (
    <div style={s.root}>
      <div style={s.body}>
        <button type="button" style={s.backBtn} onClick={() => navigate('/candidates')}>
          <ArrowLeftOutlined /> Back to candidates
        </button>

        <div style={s.card}>
          <div style={s.head}>
            {candidate.hasPhoto && !USE_CANDIDATE_MOCK ? (
              <AuthenticatedCandidateAvatar candidateId={candidate.id} name={candidate.name} size={56} />
            ) : candidate.photoUrl ? (
              <img
                src={candidate.photoUrl}
                alt={candidate.name}
                style={{ width: 56, height: 56, borderRadius: '50%', objectFit: 'cover' }}
              />
            ) : (
              <div
                style={{
                  width: 56,
                  height: 56,
                  borderRadius: '50%',
                  background: 'var(--accent-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.1rem',
                  fontWeight: 600,
                  color: 'var(--accent)',
                }}
              >
                {candidate.name[0]?.toUpperCase() ?? '?'}
              </div>
            )}
            <div>
              <h1 style={s.title}>{candidate.name}</h1>
              <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{candidate.email}</p>
            </div>
          </div>

          <p style={s.meta}>
            Applied {new Date(candidate.createdAt).toLocaleDateString('en-US', {
              day: '2-digit',
              month: 'short',
              year: 'numeric',
            })}
          </p>

          <div style={s.row}><span style={s.label}>Phone</span>{candidate.phone}</div>
          <div style={s.row}><span style={s.label}>Position</span>{candidate.position}</div>
          <div style={s.row}><span style={s.label}>Experience</span>{candidate.experience || '—'}</div>
          <div style={s.row}>
            <span style={s.label}>Stage</span>
            <StatusBadge stage={candidate.stage} />
          </div>
          {candidate.notes && (
            <div style={{ ...s.row, marginTop: '1rem' }}>
              <span style={{ ...s.label, display: 'block', marginBottom: 4 }}>Notes</span>
              <span style={{ color: 'var(--text-primary)', whiteSpace: 'pre-wrap' as const }}>{candidate.notes}</span>
            </div>
          )}

          <div style={s.actions}>
            {canManageCandidates && (
              <Button type="primary" icon={<EditOutlined />} onClick={() => navigate(`/candidates/${id}/edit`)}>
                Edit
              </Button>
            )}
            {candidate.hasResume && !USE_CANDIDATE_MOCK && (
              <Button icon={<DownloadOutlined />} onClick={downloadResume}>
                Download resume
              </Button>
            )}
          </div>
        </div>

        <div style={s.card}>
          <h2 style={s.timelineTitle}>Stage timeline</h2>
          {stageHistory.length === 0 ? (
            <p style={{ margin: 0, color: 'var(--text-secondary)' }}>No stage changes recorded yet.</p>
          ) : (
            stageHistory.map((entry, index) => (
              <div key={`${entry.changedAt}-${index}`} style={s.timelineItem}>
                <div><StatusBadge stage={entry.stage} /></div>
                <div style={s.timelineMeta}>
                  {new Date(entry.changedAt).toLocaleString()} by {entry.changedBy}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default CandidateDetailPage;
