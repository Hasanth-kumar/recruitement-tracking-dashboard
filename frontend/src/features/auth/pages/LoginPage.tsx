import React, { useState } from 'react';
import { Input, Button, Alert, Checkbox } from 'antd';
import {
  UserOutlined,
  LockOutlined,
  EyeInvisibleOutlined,
  EyeTwoTone,
  MoonOutlined,
  SunOutlined,
} from '@ant-design/icons';
import { useAppDispatch } from '../../../shared/hooks/useAuth';
import { credentialsReceived } from '../authSlice';
import { mapToAuthUser } from '../authApi';
import { Role } from '../../../constants/roles';
import { useAppTheme } from '../../../shared/theme/AppThemeProvider';
import '../../../App.css';

interface FormState {
  identifier: string;
  password: string;
  rememberMe: boolean;
}

const USE_MOCK = false;

interface MockUser {
  id: string;
  username: string;
  email: string;
  role: Role;
}

const MOCK_USERS: Record<string, MockUser> = {
  'admin@rts.com': { id: '1', username: 'admin', email: 'admin@rts.com', role: Role.ADMIN },
  'hr@rts.com': { id: '2', username: 'hr', email: 'hr@rts.com', role: Role.HR_MANAGER },
  'recruiter@rts.com': { id: '3', username: 'recruiter', email: 'recruiter@rts.com', role: Role.RECRUITER },
  'interviewer@rts.com': { id: '4', username: 'interviewer', email: 'interviewer@rts.com', role: Role.INTERVIEWER },
};

function mockLogin(identifier: string, password: string): { accessToken: string; user: MockUser } {
  const user = MOCK_USERS[identifier.toLowerCase()];
  if (!user || !password) throw new Error('Invalid credentials. Please try again.');
  return { accessToken: 'mock-jwt-token', user };
}

interface ApiEnvelope<T> {
  success: boolean;
  message?: string;
  data?: T;
}

const LoginPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { themeMode, toggleTheme } = useAppTheme();
  const [form, setForm] = useState<FormState>({
    identifier: '',
    password: '',
    rememberMe: false,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (field: keyof FormState, value: string | boolean) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (error) setError(null);
  };

  const validate = (): string | null => {
    if (!form.identifier.trim()) return 'Email or username is required.';
    if (!form.password) return 'Password is required.';
    return null;
  };

  const handleSubmit = async () => {
    const err = validate();
    if (err) { setError(err); return; }

    setLoading(true);
    setError(null);

    const principal = form.identifier.trim();

    try {
      if (USE_MOCK) {
        await new Promise(r => setTimeout(r, 600));
        const { accessToken, user } = mockLogin(principal, form.password);
        dispatch(
          credentialsReceived({
            token: accessToken,
            user: mapToAuthUser(user),
            rememberMe: form.rememberMe,
          })
        );
      } else {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ usernameOrEmail: principal, password: form.password }),
        });
        const data = (await res.json()) as ApiEnvelope<{
          accessToken: string;
          user: { id: string; username: string; email: string; role: string };
        }>;

        if (!res.ok) {
          throw new Error(data.message || 'Invalid credentials. Please try again.');
        }
        if (!data.success || !data.data?.accessToken || !data.data?.user) {
          throw new Error(data.message || 'Login failed. Please try again.');
        }

        dispatch(
          credentialsReceived({
            token: data.data.accessToken,
            user: mapToAuthUser(data.data.user),
            rememberMe: form.rememberMe,
          })
        );
      }

      window.location.href = '/dashboard';
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSubmit();
  };

  return (
    <div className="login-root">

      <aside className="login-sidebar">
        <div className="login-logo">
          <div className="login-logo-mark">RTS</div>
          <span className="login-logo-text">Recruitment Tracking System</span>
        </div>

        <div className="login-sidebar-body">
          <h1 className="login-sidebar-title">
            Manage your entire recruitment pipeline in one place.
          </h1>
          <p className="login-sidebar-desc">
            Track candidates, schedule interviews, collect feedback, and generate reports — all from a single dashboard.
          </p>

          <div className="login-sidebar-stats">
            <div className="login-stat">
              <span className="login-stat-num">9</span>
              <span className="login-stat-label">Pipeline stages</span>
            </div>
            <div className="login-stat">
              <span className="login-stat-num">4</span>
              <span className="login-stat-label">User roles</span>
            </div>
            <div className="login-stat">
              <span className="login-stat-num">2</span>
              <span className="login-stat-label">Interview rounds</span>
            </div>
          </div>
        </div>
      </aside>

      <main className="login-main">
        <div className="login-form-wrap">
          <div className="login-theme-toggle-wrap">
            <Button
              type="text"
              className="theme-toggle-btn"
              icon={themeMode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
              onClick={toggleTheme}
              aria-label={`Switch to ${themeMode === 'dark' ? 'light' : 'dark'} mode`}
            />
          </div>

          <div className="login-form-header">
            <p className="login-form-eyebrow">Welcome back</p>
            <h2 className="login-form-title">Sign in</h2>
            <p className="login-form-subtitle">Enter your credentials to continue.</p>
          </div>

          {USE_MOCK && (
            <div style={{ marginBottom: '1rem', padding: '8px 12px', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 8, fontSize: '0.775rem', color: '#92400e' }}>
              <strong>Mock mode on.</strong> Use any preset below or type a preset email with any password.
            </div>
          )}

          <div className="login-card">
            {error && (
              <Alert
                className="login-error"
                type="error"
                message={error}
                showIcon
                closable
                onClose={() => setError(null)}
              />
            )}

            <div className="login-field">
              <label className="login-field-label" htmlFor="login-id">
                Email or username
              </label>
              <Input
                id="login-id"
                className="login-input"
                prefix={<UserOutlined />}
                placeholder="you@company.com"
                value={form.identifier}
                onChange={e => handleChange('identifier', e.target.value)}
                onKeyDown={onKeyDown}
                autoComplete="username"
                size="large"
              />
            </div>

            <div className="login-field">
              <label className="login-field-label" htmlFor="login-pwd">
                Password
              </label>
              <Input.Password
                id="login-pwd"
                className="login-input"
                prefix={<LockOutlined />}
                placeholder="••••••••"
                iconRender={v => v ? <EyeTwoTone /> : <EyeInvisibleOutlined />}
                value={form.password}
                onChange={e => handleChange('password', e.target.value)}
                onKeyDown={onKeyDown}
                autoComplete="current-password"
                size="large"
              />
            </div>

            <div className="login-row">
              <label className="login-remember">
                <Checkbox
                  checked={form.rememberMe}
                  onChange={e => handleChange('rememberMe', e.target.checked)}
                />
                Remember me
              </label>
              <a href="/forgot-password" className="login-forgot">Forgot password?</a>
            </div>

            <Button
              className="login-btn"
              type="primary"
              size="large"
              loading={loading}
              onClick={handleSubmit}
            >
              {loading ? 'Signing in…' : 'Sign in'}
            </Button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default LoginPage;
