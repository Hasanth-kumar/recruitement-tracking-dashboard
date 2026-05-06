// src/App.tsx
// Route definitions — login, dashboard, profile

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { TeamOutlined, UserOutlined, UsergroupAddOutlined } from '@ant-design/icons';
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

// ── Dashboard (Sprint 1 placeholder — richer copy until Sprint 2 reporting) ──

function DashboardPage() {
  const { role, hasRole } = useAuth();
  const navigate = useNavigate();

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

          <div className="dashboard-cards">
            {hasRole(Role.ADMIN, Role.HR_MANAGER, Role.RECRUITER) ? (
              <button
                type="button"
                className="dashboard-card"
                onClick={() => navigate('/candidates')}
              >
                <TeamOutlined className="dashboard-card-icon" aria-hidden />
                <div className="dashboard-card-title">Candidates</div>
                <p className="dashboard-card-desc">
                  Search, filter, add applicants, update stages, and manage documents.
                </p>
              </button>
            ) : (
              <div className="dashboard-card dashboard-card--muted">
                <TeamOutlined className="dashboard-card-icon" aria-hidden />
                <div className="dashboard-card-title">Candidates</div>
                <p className="dashboard-card-desc">
                  Candidate management is available to Admin, HR Manager, and Recruiter roles.
                </p>
              </div>
            )}

            <button type="button" className="dashboard-card" onClick={() => navigate('/profile')}>
              <UserOutlined className="dashboard-card-icon" aria-hidden />
              <div className="dashboard-card-title">Your profile</div>
              <p className="dashboard-card-desc">
                Update your account details and password (HTTP Basic credentials).
              </p>
            </button>

            {hasRole(Role.ADMIN) && (
              <button type="button" className="dashboard-card" onClick={() => navigate('/admin/users')}>
                <UsergroupAddOutlined className="dashboard-card-icon" aria-hidden />
                <div className="dashboard-card-title">Manage users</div>
                <p className="dashboard-card-desc">Create accounts and assign roles (admin only).</p>
              </button>
            )}
          </div>
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
