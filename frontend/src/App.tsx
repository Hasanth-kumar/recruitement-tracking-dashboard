// src/App.tsx
// Route definitions — login, dashboard, profile

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './features/auth/pages/LoginPage';
import AdminUsersPage from './features/auth/pages/AdminUsersPage';
import ProfilePage from './features/auth/components/ProfilePage';
import CandidateListPage from './features/candidates/pages/CandidateListPage';
import CandidateFormPage from './features/candidates/pages/CandidateFormPage';
import { Role } from './constants/roles';
import { useAuth } from './shared/hooks/useAuth';

// ── Simple auth guard ──────────────────────────────────────────
// Checks for a token in either storage tier before allowing access

function isLoggedIn(): boolean {
  return !!(localStorage.getItem('rts_token') || sessionStorage.getItem('rts_token'));
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  return isLoggedIn() ? <>{children}</> : <Navigate to="/login" replace />;
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  if (!isLoggedIn()) return <Navigate to="/login" replace />;
  const role = localStorage.getItem('rts_role') ?? sessionStorage.getItem('rts_role');
  if (role !== Role.ADMIN) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

// ── Temporary dashboard placeholder ───────────────────────────
// Replace this with your real DashboardPage once it's built

function DashboardPlaceholder() {
  const { role, hasRole } = useAuth();

  const logout = () => {
    ['rts_token', 'rts_role', 'rts_user', 'rts_basic_principal'].forEach(k => {
      localStorage.removeItem(k);
      sessionStorage.removeItem(k);
    });
    window.location.href = '/login';
  };

  return (
    <div style={{ fontFamily: 'IBM Plex Sans, sans-serif', minHeight: '100vh', background: '#f9f9f8' }}>
      {/* Top nav */}
      <div style={{ background: '#fff', borderBottom: '1px solid #e4e4e0', padding: '0 2rem', height: 52, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ width: 28, height: 28, background: '#2563eb', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: '0.7rem', fontWeight: 600 }}>RTS</div>
          <span style={{ fontSize: '0.85rem', fontWeight: 500, color: '#6b6b65' }}>Recruitment Tracking System</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <a href="/candidates" style={{ fontSize: '0.85rem', color: '#2563eb', textDecoration: 'none' }}>Candidates</a>
          {hasRole(Role.ADMIN) && (
            <a href="/admin/users" style={{ fontSize: '0.85rem', color: '#2563eb', textDecoration: 'none' }}>Users</a>
          )}
          <a href="/profile" style={{ fontSize: '0.85rem', color: '#2563eb', textDecoration: 'none' }}>Profile</a>
          <button onClick={logout} style={{ fontSize: '0.85rem', color: '#6b6b65', background: 'none', border: '1px solid #e4e4e0', borderRadius: 6, padding: '4px 12px', cursor: 'pointer' }}>
            Sign out
          </button>
        </div>
      </div>

      {/* Body */}
      <div style={{ maxWidth: 700, margin: '4rem auto', padding: '0 2rem', textAlign: 'center' }}>
        <div style={{ display: 'inline-block', padding: '3px 10px', background: '#eff4ff', border: '1px solid #bfdbfe', borderRadius: 99, fontSize: '0.75rem', color: '#2563eb', fontWeight: 500, marginBottom: '1rem' }}>
          Signed in as {role ?? '—'}
        </div>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 600, color: '#1a1a18', marginBottom: '0.75rem' }}>
          Dashboard
        </h1>
        <p style={{ fontSize: '0.9rem', color: '#6b6b65', marginBottom: '2rem', lineHeight: 1.6 }}>
          Login is working. This is a placeholder — replace with your real dashboard once it's built.
        </p>
        <a
          href="/profile"
          style={{ display: 'inline-block', padding: '8px 20px', background: '#2563eb', color: '#fff', borderRadius: 8, fontSize: '0.875rem', fontWeight: 500, textDecoration: 'none' }}
        >
          Go to Profile →
        </a>
      </div>
    </div>
  );
}

// ── App ────────────────────────────────────────────────────────

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected */}
        <Route path="/dashboard" element={<ProtectedRoute><DashboardPlaceholder /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        <Route path="/admin/users" element={<AdminRoute><AdminUsersPage /></AdminRoute>} />
        <Route path="/candidates" element={<ProtectedRoute><CandidateListPage /></ProtectedRoute>} />
        <Route path="/candidates/new" element={<ProtectedRoute><CandidateFormPage /></ProtectedRoute>} />
        <Route path="/candidates/:id/edit" element={<ProtectedRoute><CandidateFormPage /></ProtectedRoute>} />

        {/* Fallback */}
        <Route path="*" element={<Navigate to={isLoggedIn() ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
