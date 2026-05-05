// src/App.tsx
// Route definitions — login, dashboard, profile

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './features/auth/pages/LoginPage';
import AdminUsersPage from './features/auth/pages/AdminUsersPage';
import ProfilePage from './features/auth/components/ProfilePage';
import CandidateListPage from './features/candidates/pages/CandidateListPage';
import CandidateFormPage from './features/candidates/pages/CandidateFormPage';
import CandidateDetailPage from './features/candidates/pages/CandidateDetailPage';
import { Role } from './constants/roles';
import { useAuth } from './shared/hooks/useAuth';
import ProtectedRoute from './shared/components/ProtectedRoutes';
import ForbiddenPage from './shared/components/ForbiddenPage';
import AppLayout from './shared/components/AppLayout';

// ── Simple auth guard ──────────────────────────────────────────
// Checks for a token in either storage tier before allowing access

function isLoggedIn(): boolean {
  return !!(localStorage.getItem('rts_token') || sessionStorage.getItem('rts_token'));
}

// ── Dashboard placeholder ────────────────────────────────────────

function DashboardPage() {
  const { role } = useAuth();

  return (
    <AppLayout>
      <div className="dashboard-root">
        <div className="dashboard-header">
          <div className="dashboard-eyebrow">Dashboard</div>
          <h1 className="dashboard-title">Welcome to RTS</h1>
        </div>
        <div className="dashboard-content">
          <div className="dashboard-status-badge">
            Signed in as <strong>{role ?? '—'}</strong>
          </div>
          <p className="dashboard-description">
            Login is working. This is a placeholder — replace with your real dashboard once it's built.
          </p>
        </div>
      </div>
    </AppLayout>
  );
}

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/403" element={<ForbiddenPage />} />

        {/* Protected */}
        <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <AppLayout>
                <ProfilePage />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute allowedRoles={[Role.ADMIN]}>
              <AppLayout>
                <AdminUsersPage />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/candidates"
          element={
            <ProtectedRoute allowedRoles={[Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER]}>
              <AppLayout>
                <CandidateListPage />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/candidates/new"
          element={
            <ProtectedRoute allowedRoles={[Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER]}>
              <AppLayout>
                <CandidateFormPage />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/candidates/:id/edit"
          element={
            <ProtectedRoute allowedRoles={[Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER]}>
              <AppLayout>
                <CandidateFormPage />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/candidates/:id"
          element={
            <ProtectedRoute allowedRoles={[Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER]}>
              <AppLayout>
                <CandidateDetailPage />
              </AppLayout>
            </ProtectedRoute>
          }
        />

        {/* Fallback */}
        <Route path="*" element={<Navigate to={isLoggedIn() ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
