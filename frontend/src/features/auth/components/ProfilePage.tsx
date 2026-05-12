import React, { useState, useEffect } from 'react';
import { Input, Button, Alert, Spin, Divider, message } from 'antd';
import {
  UserOutlined,
  MailOutlined,
  LockOutlined,
  EyeInvisibleOutlined,
  EyeTwoTone,
} from '@ant-design/icons';
import { useGetProfileQuery, useUpdateProfileMutation } from '../authApi';
import { useAppSelector } from '../../../shared/hooks/useAuth';
import { selectAuthUser } from '../authSlice';
import '../../../App.css';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface ProfileForm {
  username: string;
  email: string;
}

interface PasswordForm {
  current: string;
  next: string;
  confirm: string;
}

const ProfilePage: React.FC = () => {
  const reduxUser = useAppSelector(selectAuthUser);

  const { data: profileRes, isLoading } = useGetProfileQuery();
  const [updateProfile, { isLoading: saving }] = useUpdateProfileMutation();

  const profile = profileRes?.data ?? reduxUser;

  const [profileForm, setProfileForm] = useState<ProfileForm>({ username: '', email: '' });
  const [pwdForm, setPwdForm] = useState<PasswordForm>({ current: '', next: '', confirm: '' });
  const [dirty, setDirty] = useState(false);
  const [profileErr, setProfileErr] = useState<string | null>(null);
  const [pwdErr, setPwdErr] = useState<string | null>(null);

  useEffect(() => {
    if (profile) {
      setProfileForm({
        username: profile.username ?? '',
        email: profile.email ?? '',
      });
    }
  }, [profile]);

  const onProfileChange = (field: keyof ProfileForm, val: string) => {
    setProfileForm(p => ({ ...p, [field]: val }));
    setDirty(true);
    if (profileErr) setProfileErr(null);
  };

  const validateProfile = (): string | null => {
    const u = profileForm.username.trim();
    if (u.length > 0 && (u.length < 3 || u.length > 100)) {
      return 'Username must be between 3 and 100 characters.';
    }
    if (!EMAIL_REGEX.test(profileForm.email.trim())) return 'Enter a valid email address.';
    return null;
  };

  const handleProfileSave = async () => {
    const err = validateProfile();
    if (err) { setProfileErr(err); return; }
    try {
      const res = await updateProfile({
        username: profileForm.username.trim(),
        email: profileForm.email.trim().toLowerCase(),
      }).unwrap();
      if (res.success) {
        message.success('Profile saved.');
        setDirty(false);
      }
    } catch (e: unknown) {
      const msg = e && typeof e === 'object' && 'data' in e
        ? (e as { data?: { message?: string } }).data?.message
        : undefined;
      setProfileErr(msg ?? 'Failed to save. Please try again.');
    }
  };

  const handleProfileCancel = () => {
    if (profile) setProfileForm({ username: profile.username ?? '', email: profile.email ?? '' });
    setDirty(false);
    setProfileErr(null);
  };

  const onPwdChange = (field: keyof PasswordForm, val: string) => {
    setPwdForm(p => ({ ...p, [field]: val }));
    if (pwdErr) setPwdErr(null);
  };

  const validatePassword = (): string | null => {
    if (!pwdForm.current) return 'Current password is required.';
    if (pwdForm.next.length < 8) return 'New password must be at least 8 characters.';
    if (pwdForm.next !== pwdForm.confirm) return 'Passwords do not match.';
    if (pwdForm.next === pwdForm.current) return 'New password must differ from current.';
    return null;
  };

  const handlePasswordSave = async () => {
    const err = validatePassword();
    if (err) { setPwdErr(err); return; }
    try {
      const res = await updateProfile({
        username: profileForm.username.trim(),
        email: profileForm.email.trim().toLowerCase(),
        currentPassword: pwdForm.current,
        newPassword: pwdForm.next,
        confirmNewPassword: pwdForm.confirm,
      }).unwrap();
      if (res.success) {
        message.success('Password updated.');
        setPwdForm({ current: '', next: '', confirm: '' });
      }
    } catch (e: unknown) {
      const msg = e && typeof e === 'object' && 'data' in e
        ? (e as { data?: { message?: string } }).data?.message
        : undefined;
      setPwdErr(msg ?? 'Could not update password.');
    }
  };

  if (isLoading) {
    return (
      <div className="profile-loading">
        <Spin />
      </div>
    );
  }

  const initials = profile?.username
    ? profile.username.slice(0, 2).toUpperCase()
    : 'U';

  return (
    <div className="profile-root">

      <div className="profile-page-header">
        <p className="profile-eyebrow">Account</p>
        <h2 className="profile-page-title">My Profile</h2>
      </div>

      <div className="profile-layout">

        <aside className="profile-avatar-card">
          <div className="profile-avatar" title="Profile">
            <span className="profile-avatar-initials">{initials}</span>
          </div>
          <p className="profile-avatar-name">{profile?.username ?? '—'}</p>
          <span className="profile-role-tag">{profile?.role ?? '—'}</span>
        </aside>

        <div className="profile-forms">

          <section className="profile-section">
            <h3 className="profile-section-title">Account details</h3>
            <p className="profile-section-desc">Update your username and email.</p>

            {profileErr && (
              <Alert
                className="profile-alert profile-alert-error"
                type="error"
                message={profileErr}
                showIcon
                closable
                onClose={() => setProfileErr(null)}
              />
            )}

            <div className="profile-grid">
              <div className="profile-field">
                <label className="profile-field-label">Username</label>
                <Input
                  className="profile-input"
                  prefix={<UserOutlined />}
                  placeholder="jane.smith"
                  value={profileForm.username}
                  onChange={e => onProfileChange('username', e.target.value)}
                  size="large"
                />
              </div>

              <div className="profile-field">
                <label className="profile-field-label">Email address</label>
                <Input
                  className="profile-input"
                  prefix={<MailOutlined />}
                  placeholder="jane@company.com"
                  type="email"
                  value={profileForm.email}
                  onChange={e => onProfileChange('email', e.target.value)}
                  size="large"
                />
              </div>

              <div className="profile-field">
                <label className="profile-field-label">Role</label>
                <Input
                  className="profile-input profile-input-disabled"
                  value={profile?.role ?? ''}
                  readOnly
                  size="large"
                />
              </div>
            </div>

            <div className="profile-actions">
              <Button
                className="profile-btn-primary"
                type="primary"
                loading={saving}
                disabled={!dirty}
                onClick={handleProfileSave}
              >
                Save changes
              </Button>
              {dirty && (
                <Button className="profile-btn-ghost" onClick={handleProfileCancel}>
                  Cancel
                </Button>
              )}
            </div>
          </section>

          <Divider />

          <section className="profile-section">
            <h3 className="profile-section-title">Change password</h3>
            <p className="profile-section-desc">
              Enter your current password and choose a new one.
            </p>

            {pwdErr && (
              <Alert
                className="profile-alert profile-alert-error"
                type="error"
                message={pwdErr}
                showIcon
                closable
                onClose={() => setPwdErr(null)}
              />
            )}

            <div className="profile-grid">
              <div className="profile-field profile-field--full">
                <label className="profile-field-label">Current password</label>
                <Input.Password
                  className="profile-input"
                  prefix={<LockOutlined />}
                  placeholder="Current password"
                  iconRender={v => v ? <EyeTwoTone /> : <EyeInvisibleOutlined />}
                  value={pwdForm.current}
                  onChange={e => onPwdChange('current', e.target.value)}
                  autoComplete="current-password"
                  size="large"
                />
              </div>

              <div className="profile-field">
                <label className="profile-field-label">New password</label>
                <Input.Password
                  className="profile-input"
                  prefix={<LockOutlined />}
                  placeholder="New password"
                  iconRender={v => v ? <EyeTwoTone /> : <EyeInvisibleOutlined />}
                  value={pwdForm.next}
                  onChange={e => onPwdChange('next', e.target.value)}
                  autoComplete="new-password"
                  size="large"
                />
              </div>

              <div className="profile-field">
                <label className="profile-field-label">Confirm new password</label>
                <Input.Password
                  className="profile-input"
                  prefix={<LockOutlined />}
                  placeholder="Repeat password"
                  iconRender={v => v ? <EyeTwoTone /> : <EyeInvisibleOutlined />}
                  value={pwdForm.confirm}
                  onChange={e => onPwdChange('confirm', e.target.value)}
                  autoComplete="new-password"
                  size="large"
                />
              </div>
            </div>

            <div className="profile-actions">
              <Button
                className="profile-btn-primary"
                type="primary"
                loading={saving}
                onClick={handlePasswordSave}
              >
                Update password
              </Button>
              <Button
                className="profile-btn-ghost"
                onClick={() => { setPwdForm({ current: '', next: '', confirm: '' }); setPwdErr(null); }}
              >
                Cancel
              </Button>
            </div>
          </section>

        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
