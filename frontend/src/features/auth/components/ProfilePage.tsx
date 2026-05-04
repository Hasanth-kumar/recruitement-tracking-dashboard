import React, { useState, useRef, useEffect } from 'react';
import { Input, Button, Alert, Spin, Divider, message } from 'antd';
import {
 UserOutlined,
 MailOutlined,
 PhoneOutlined,
 LockOutlined,
 CameraOutlined,
 EyeInvisibleOutlined,
 EyeTwoTone,
} from '@ant-design/icons';
import {
 useGetProfileQuery,
 useUpdateProfileMutation,
 useChangePasswordMutation,
 useUploadAvatarMutation,
} from '../authApi';
import { useAppSelector } from '../../../shared/hooks/useAuth';
import { selectAuthUser } from '../authSlice';
import '../../../App.css';

// ── Validation ─────────────────────────────────────────────────

const EMAIL_REGEX    = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX    = /^[+]?[\d\s\-().]{7,15}$/;
const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

// ── Types ──────────────────────────────────────────────────────

interface ProfileForm {
 fullName: string;
 email: string;
 phone: string;
}

interface PasswordForm {
 current: string;
 next: string;
 confirm: string;
}

// ── Component ──────────────────────────────────────────────────

const ProfilePage: React.FC = () => {
 const reduxUser = useAppSelector(selectAuthUser);

 const { data: profileRes, isLoading }           = useGetProfileQuery();
 const [updateProfile, { isLoading: saving }]    = useUpdateProfileMutation();
 const [changePassword, { isLoading: changingPwd }] = useChangePasswordMutation();
 const [uploadAvatar,   { isLoading: uploading }]   = useUploadAvatarMutation();

 const profile = profileRes?.data ?? reduxUser;

 const [profileForm, setProfileForm] = useState<ProfileForm>({ fullName: '', email: '', phone: '' });
 const [pwdForm,     setPwdForm]     = useState<PasswordForm>({ current: '', next: '', confirm: '' });
 const [dirty,       setDirty]       = useState(false);
 const [profileErr,  setProfileErr]  = useState<string | null>(null);
 const [pwdErr,      setPwdErr]      = useState<string | null>(null);
 const [avatarPreview, setAvatarPreview] = useState<string | null>(null);

 const fileRef = useRef<HTMLInputElement>(null);

 useEffect(() => {
   if (profile) {
     setProfileForm({
       fullName: profile.fullName ?? '',
       email:    profile.email    ?? '',
       phone:    profile.phone    ?? '',
     });
   }
 }, [profile]);

 // ── Profile handlers ───────────────────────────────────────

 const onProfileChange = (field: keyof ProfileForm, val: string) => {
   setProfileForm(p => ({ ...p, [field]: val }));
   setDirty(true);
   if (profileErr) setProfileErr(null);
 };

 const validateProfile = (): string | null => {
   if (!profileForm.fullName.trim())             return 'Full name is required.';
   if (!EMAIL_REGEX.test(profileForm.email))      return 'Enter a valid email address.';
   if (profileForm.phone && !PHONE_REGEX.test(profileForm.phone))
                                                  return 'Enter a valid phone number.';
   return null;
 };

 const handleProfileSave = async () => {
   const err = validateProfile();
   if (err) { setProfileErr(err); return; }
   try {
     const res = await updateProfile({
       fullName: profileForm.fullName.trim(),
       email:    profileForm.email.trim(),
       phone:    profileForm.phone.trim() || undefined,
     }).unwrap();
     if (res.success) { message.success('Profile saved.'); setDirty(false); }
   } catch (e: any) {
     setProfileErr(e?.data?.message ?? 'Failed to save. Please try again.');
   }
 };

 const handleProfileCancel = () => {
   if (profile) setProfileForm({ fullName: profile.fullName ?? '', email: profile.email ?? '', phone: profile.phone ?? '' });
   setDirty(false);
   setProfileErr(null);
 };

 // ── Password handlers ──────────────────────────────────────

 const onPwdChange = (field: keyof PasswordForm, val: string) => {
   setPwdForm(p => ({ ...p, [field]: val }));
   if (pwdErr) setPwdErr(null);
 };

 const validatePassword = (): string | null => {
   if (!pwdForm.current)                              return 'Current password is required.';
   if (!PASSWORD_REGEX.test(pwdForm.next))            return 'Password must be 8+ chars with uppercase, number, and special character.';
   if (pwdForm.next !== pwdForm.confirm)              return 'Passwords do not match.';
   if (pwdForm.next === pwdForm.current)              return 'New password must differ from current.';
   return null;
 };

 const handlePasswordSave = async () => {
   const err = validatePassword();
   if (err) { setPwdErr(err); return; }
   try {
     const res = await changePassword({ currentPassword: pwdForm.current, newPassword: pwdForm.next }).unwrap();
     if (res.success) {
       message.success('Password updated.');
       setPwdForm({ current: '', next: '', confirm: '' });
     }
   } catch (e: any) {
     setPwdErr(e?.data?.message ?? 'Incorrect current password.');
   }
 };

 // ── Avatar handlers ────────────────────────────────────────

 const handleAvatarFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
   const file = e.target.files?.[0];
   if (!file) return;

   if (!['image/jpeg', 'image/png'].includes(file.type)) {
     message.error('Only JPG and PNG files are allowed.'); return;
   }
   if (file.size > 2 * 1024 * 1024) {
     message.error('Photo must be under 2 MB.'); return;
   }

   const reader = new FileReader();
   reader.onload = (ev) => setAvatarPreview(ev.target?.result as string);
   reader.readAsDataURL(file);

   const fd = new FormData();
   fd.append('avatar', file);
   try {
     const res = await uploadAvatar(fd).unwrap();
     if (res.success) message.success('Photo updated.');
   } catch {
     message.error('Upload failed.');
     setAvatarPreview(null);
   }
   e.target.value = '';
 };

 // ── Render ─────────────────────────────────────────────────

 if (isLoading) {
   return (
     <div className="profile-loading">
       <Spin />
     </div>
   );
 }

 const avatar   = avatarPreview ?? profile?.avatarUrl ?? null;
 const initials = profile?.fullName
   ? profile.fullName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
   : 'U';

 return (
   <div className="profile-root">

     {/* Header */}
     <div className="profile-page-header">
       <p className="profile-eyebrow">Account</p>
       <h2 className="profile-page-title">My Profile</h2>
     </div>

     <div className="profile-layout">

       {/* Avatar card */}
       <aside className="profile-avatar-card">
         <div
           className="profile-avatar"
           onClick={() => fileRef.current?.click()}
           title="Change photo"
         >
           {avatar
             ? <img src={avatar} alt="Profile" />
             : <span className="profile-avatar-initials">{initials}</span>
           }
           <div className="profile-avatar-overlay">
             {uploading ? <Spin size="small" /> : <CameraOutlined />}
           </div>
         </div>
         <input
           ref={fileRef}
           type="file"
           accept="image/jpeg,image/png"
           style={{ display: 'none' }}
           onChange={handleAvatarFile}
         />
         <p className="profile-avatar-name">{profile?.fullName ?? '—'}</p>
         <span className="profile-role-tag">{profile?.role ?? '—'}</span>
         <p className="profile-avatar-hint">JPG or PNG · Max 2 MB</p>
       </aside>

       {/* Forms */}
       <div className="profile-forms">

         {/* Personal info */}
         <section className="profile-section">
           <h3 className="profile-section-title">Personal information</h3>
           <p className="profile-section-desc">Update your name, email, and phone number.</p>

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
               <label className="profile-field-label">Full name</label>
               <Input
                 className="profile-input"
                 prefix={<UserOutlined />}
                 placeholder="Jane Smith"
                 value={profileForm.fullName}
                 onChange={e => onProfileChange('fullName', e.target.value)}
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
               {profile?.email && profileForm.email !== profile.email && (
                 <p className="profile-field-hint">A verification link will be sent to the new address.</p>
               )}
             </div>

             <div className="profile-field">
               <label className="profile-field-label">
                 Phone <span className="profile-optional">(optional)</span>
               </label>
               <Input
                 className="profile-input"
                 prefix={<PhoneOutlined />}
                 placeholder="+1 555 000 0000"
                 value={profileForm.phone}
                 onChange={e => onProfileChange('phone', e.target.value)}
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

         {/* Change password */}
         <section className="profile-section">
           <h3 className="profile-section-title">Change password</h3>
           <p className="profile-section-desc">
             Minimum 8 characters with an uppercase letter, number, and special character.
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
               loading={changingPwd}
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