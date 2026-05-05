import React, { useEffect, useState } from 'react';
import { basicAuthFetchHeaders } from '../../../shared/utils/basicAuth';

interface Props {
  candidateId: string;
  name: string;
  size?: number;
}

const fb: React.CSSProperties = {
  background: '#eff4ff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: '0.8rem',
  fontWeight: 600,
  color: '#2563eb',
  flexShrink: 0,
};

/** Loads `/api/candidates/:id/photo` with Basic auth (plain `<img src>` cannot). */
const AuthenticatedCandidateAvatar: React.FC<Props> = ({ candidateId, name, size = 34 }) => {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
    let objectUrl: string | null = null;
    let cancelled = false;
    (async () => {
      try {
        const res = await fetch(`/api/candidates/${candidateId}/photo`, {
          headers: basicAuthFetchHeaders(false),
        });
        if (!res.ok || cancelled) return;
        const blob = await res.blob();
        objectUrl = URL.createObjectURL(blob);
        if (!cancelled) setSrc(objectUrl);
      } catch {
        /* ignore */
      }
    })();
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [candidateId]);

  const dim = { width: size, height: size, borderRadius: '50%' as const };

  if (src) {
    return (
      <img
        src={src}
        alt={name}
        style={{ ...dim, objectFit: 'cover' as const, flexShrink: 0 }}
      />
    );
  }

  return (
    <div style={{ ...dim, ...fb }}>
      {(name[0] ?? '?').toUpperCase()}
    </div>
  );
};

export default AuthenticatedCandidateAvatar;
