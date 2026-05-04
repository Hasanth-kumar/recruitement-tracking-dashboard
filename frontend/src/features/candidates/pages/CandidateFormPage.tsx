// src/features/candidates/pages/CandidateFormPage.tsx
// US-1: Add new candidate
// US-2: Upload resume (PDF/DOC/DOCX, max 5MB)
// US-3: Upload candidate photo (JPG/PNG, max 2MB)
// US-6: Edit existing candidate (prefilled form)

import React, { useState, useEffect } from 'react';
import { Input, Button, Alert, Select, message } from 'antd';
import {
 ArrowLeftOutlined,
 UserOutlined,
 MailOutlined,
 PhoneOutlined,
 SaveOutlined,
} from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import PhotoUpload from '../components/PhotoUpload';
import ResumeUpload from '../components/ResumeUpload';
import { CandidateFormValues, POSITIONS, EXPERIENCE_LEVELS } from '../candidateTypes';
import {
 mockCreateCandidate,
 mockGetCandidate,
 mockUpdateCandidate,
 mockUpdateCandidatePhoto,
 mockUpdateCandidateResume,
} from '../candidateMock';
import { basicAuthFetchHeaders } from '../../../shared/utils/basicAuth';
import { Role } from '../../../constants/roles';

// ── Mock flag ───────────────────────────────────────────────────
// Set to false when Spring Boot backend is running
const USE_MOCK = false;

// ── API helpers (real path) ─────────────────────────────────────

async function apiGetCandidate(id: string) {
 const res = await fetch(`/api/candidates/${id}`, {
   headers: basicAuthFetchHeaders(false),
 });
 const data = await res.json();
 if (!data.success) throw new Error(data.message);
 return data.data;
}

async function apiCreateCandidate(payload: object) {
 const res = await fetch('/api/candidates', {
   method: 'POST',
   headers: basicAuthFetchHeaders(true),
   body: JSON.stringify(payload),
 });
 const data = await res.json();
 if (!data.success) throw new Error(data.message);
 return data.data;
}

async function apiUpdateCandidate(id: string, payload: object) {
 const res = await fetch(`/api/candidates/${id}`, {
   method: 'PUT',
   headers: basicAuthFetchHeaders(true),
   body: JSON.stringify(payload),
 });
 const data = await res.json();
 if (!data.success) throw new Error(data.message);
 return data.data;
}

async function apiUploadPhoto(id: string, file: File) {
 const fd = new FormData();
 fd.append('photo', file);
 const res = await fetch(`/api/candidates/${id}/photo`, {
   method: 'POST',
   headers: basicAuthFetchHeaders(false),
   body: fd,
 });
 const data = await res.json();
 if (!data.success) throw new Error(data.message);
}

async function apiUploadResume(id: string, file: File) {
 const fd = new FormData();
 fd.append('resume', file);
 const res = await fetch(`/api/candidates/${id}/resume`, {
   method: 'POST',
   headers: basicAuthFetchHeaders(false),
   body: fd,
 });
 const data = await res.json();
 if (!data.success) throw new Error(data.message);
}

// ── Validation ──────────────────────────────────────────────────
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^[+]?[\d\s\-().]{7,15}$/;

function validate(
 form: CandidateFormValues,
): Partial<Record<keyof CandidateFormValues, string>> {
 const errors: Partial<Record<keyof CandidateFormValues, string>> = {};
 if (!form.name.trim())                  errors.name       = 'Full name is required.';
 if (!form.email.trim())                 errors.email      = 'Email is required.';
 else if (!EMAIL_REGEX.test(form.email)) errors.email      = 'Enter a valid email address.';
 if (!form.phone.trim())                 errors.phone      = 'Phone number is required.';
 else if (!PHONE_REGEX.test(form.phone)) errors.phone      = 'Enter a valid phone number.';
 if (!form.position)                     errors.position   = 'Position is required.';
//  if (!form.experience)                   errors.experience = 'Experience level is required.';
 return errors;
}

// ── Styles ──────────────────────────────────────────────────────
const s = {
 root:      { fontFamily: "'IBM Plex Sans', sans-serif", minHeight: '100vh', background: '#f9f9f8' },
 nav:       { background: '#fff', borderBottom: '1px solid #e4e4e0', padding: '0 2rem', height: 52, display: 'flex' as const, alignItems: 'center' as const, justifyContent: 'space-between' as const },
 navBrand:  { display: 'flex' as const, alignItems: 'center' as const, gap: 10 },
 navMark:   { width: 28, height: 28, background: '#2563eb', borderRadius: 4, display: 'flex' as const, alignItems: 'center' as const, justifyContent: 'center' as const, color: '#fff', fontSize: '0.7rem', fontWeight: 600 as const },
 navLinks:  { display: 'flex' as const, alignItems: 'center' as const, gap: 16 },
 navLink:   { fontSize: '0.85rem', color: '#2563eb', textDecoration: 'none' as const },
 navBtn:    { fontSize: '0.85rem', color: '#6b6b65', background: 'none', border: '1px solid #e4e4e0', borderRadius: 6, padding: '4px 12px', cursor: 'pointer' as const, fontFamily: 'inherit' },
 body:      { maxWidth: 800, margin: '0 auto', padding: '2.5rem 2rem' },
 backBtn:   { display: 'flex' as const, alignItems: 'center' as const, gap: 6, fontSize: '0.85rem', color: '#6b6b65', background: 'none', border: 'none', cursor: 'pointer' as const, padding: 0, marginBottom: '1.5rem', fontFamily: 'inherit' },
 header:    { marginBottom: '2rem', paddingBottom: '1.25rem', borderBottom: '1px solid #e4e4e0' },
 eyebrow:   { fontSize: '0.72rem', fontWeight: 500 as const, letterSpacing: '0.1em', textTransform: 'uppercase' as const, color: '#b0b0a8', marginBottom: 4 },
 title:     { fontSize: '1.35rem', fontWeight: 600 as const, color: '#1a1a18', margin: 0 },
 layout:    { display: 'grid' as const, gridTemplateColumns: '1fr 210px', gap: '1.75rem', alignItems: 'start' as const },
 card:      { background: '#fff', border: '1px solid #e4e4e0', borderRadius: 12, padding: '1.5rem', marginBottom: '1.1rem' },
 cardTitle: { fontSize: '0.9rem', fontWeight: 600 as const, color: '#1a1a18', marginBottom: 3 },
 cardDesc:  { fontSize: '0.8rem', color: '#b0b0a8', marginBottom: '1.25rem', marginTop: 0 },
 grid2:     { display: 'grid' as const, gridTemplateColumns: '1fr 1fr', gap: '1rem' },
 field:     { display: 'flex' as const, flexDirection: 'column' as const, gap: 5 },
 fieldFull: { display: 'flex' as const, flexDirection: 'column' as const, gap: 5, gridColumn: '1 / -1' as const },
 label:     { fontSize: '0.8rem', fontWeight: 500 as const, color: '#6b6b65' },
 req:       { color: '#dc2626', marginLeft: 2 },
 errText:   { fontSize: '0.73rem', color: '#dc2626', marginTop: 2, marginBottom: 0 },
 sideCard:  { background: '#fff', border: '1px solid #e4e4e0', borderRadius: 12, padding: '1.5rem', display: 'flex' as const, flexDirection: 'column' as const, alignItems: 'center' as const, gap: '1rem', position: 'sticky' as const, top: '1.5rem' },
 sideTitle: { fontSize: '0.875rem', fontWeight: 600 as const, color: '#1a1a18', alignSelf: 'flex-start' as const, margin: 0 },
 sideDivider: { width: '100%', height: 1, background: '#f0f0ed' },
 sideHint:  { fontSize: '0.72rem', color: '#b0b0a8', textAlign: 'center' as const, margin: 0, lineHeight: 1.5 },
 actions:   { display: 'flex' as const, gap: '0.6rem', marginTop: '0.25rem' },
};

// ── Component ───────────────────────────────────────────────────
const CandidateFormPage: React.FC = () => {
 const { id }   = useParams<{ id: string }>();
 const navigate = useNavigate();
 const isEdit   = Boolean(id);

 const [form, setForm] = useState<CandidateFormValues>({
   name: '', email: '', phone: '', position: '', experience: '', notes: '',
 });
 const [errors,       setErrors]       = useState<Partial<Record<keyof CandidateFormValues, string>>>({});
 const [submitError,  setSubmitError]  = useState<string | null>(null);
 const [loading,      setLoading]      = useState(false);
 const [saving,       setSaving]       = useState(false);
 const [photoFile,    setPhotoFile]    = useState<File | null>(null);
 const [photoPreview, setPhotoPreview] = useState<string | null>(null);
 const [resumeFile,   setResumeFile]   = useState<File | null>(null);
 const [resumeName,   setResumeName]   = useState<string | null>(null);

 // ── Load candidate for edit mode ──────────────────────────────
 useEffect(() => {
   if (!isEdit || !id) return;

   const load = async () => {
     setLoading(true);
     try {
       let candidate;

       if (USE_MOCK) {
         await new Promise(r => setTimeout(r, 300));
         candidate = mockGetCandidate(id);
         if (!candidate) throw new Error('Candidate not found.');
       } else {
         candidate = await apiGetCandidate(id);
       }

       setForm({
         name:       candidate.name,
         email:      candidate.email,
         phone:      candidate.phone,
         position:   candidate.position,
         experience: candidate.experience,
         notes:      candidate.notes ?? '',
       });
       if (candidate.photoUrl)  setPhotoPreview(candidate.photoUrl);
       if (candidate.resumeUrl) setResumeName(candidate.resumeUrl.split('/').pop() ?? 'resume');
     } catch (e: any) {
       message.error(e?.message ?? 'Failed to load candidate.');
       navigate('/candidates');
     } finally {
       setLoading(false);
     }
   };

   load();
 }, [id, isEdit, navigate]);

 // ── Field change ──────────────────────────────────────────────
 const onChange = (field: keyof CandidateFormValues, value: string) => {
   setForm(p => ({ ...p, [field]: value }));
   if (errors[field])  setErrors(p => ({ ...p, [field]: undefined }));
   if (submitError)    setSubmitError(null);
 };

 // ── Submit ────────────────────────────────────────────────────
 const handleSubmit = async () => {
   const errs = validate(form);
   if (Object.keys(errs).length > 0) { setErrors(errs); return; }

   setSaving(true);
   try {
     if (USE_MOCK) {
       // ── Mock path ──────────────────────────────────────────
       await new Promise(r => setTimeout(r, 500));

       if (isEdit && id) {
         mockUpdateCandidate(id, {
           name:       form.name.trim(),
           email:      form.email.trim(),
           phone:      form.phone.trim(),
           position:   form.position,
           experience: form.experience,
           notes:      form.notes.trim() || undefined,
         });
         if (photoFile && photoPreview) mockUpdateCandidatePhoto(id, photoPreview);
         if (resumeFile)                mockUpdateCandidateResume(id, resumeFile.name);
         message.success('Candidate updated.');
       } else {
         const created = mockCreateCandidate({
           name:       form.name.trim(),
           email:      form.email.trim(),
           phone:      form.phone.trim(),
           position:   form.position,
           experience: form.experience,
           notes:      form.notes.trim() || undefined,
           photoUrl:   photoPreview  ?? undefined,
           resumeUrl:  resumeFile?.name ?? undefined,
         });
         message.success(`Candidate added — ID: ${created.id}`);
       }
     } else {
       // ── Real API path ──────────────────────────────────────
       const payload = {
         name:       form.name.trim(),
         email:      form.email.trim(),
         phone:      form.phone.trim(),
         position:   form.position,
         experience: form.experience,
         notes:      form.notes.trim() || undefined,
       };

       let candidateId = id;

       if (isEdit && id) {
         await apiUpdateCandidate(id, payload);
         message.success('Candidate updated.');
       } else {
         const created = await apiCreateCandidate(payload);
         candidateId   = created.id;
         message.success(`Candidate added — ID: ${created.id}`);
       }

       // Upload files after candidate is saved
       if (candidateId) {
         if (photoFile)  await apiUploadPhoto(candidateId, photoFile);
         if (resumeFile) await apiUploadResume(candidateId, resumeFile);
       }
     }

     navigate('/candidates');
   } catch (e: any) {
     setSubmitError(e?.message ?? 'Something went wrong. Please try again.');
   } finally {
     setSaving(false);
   }
 };

 const handleCancel = () => navigate('/candidates');

 const logout = () => {
   ['rts_token', 'rts_role', 'rts_user', 'rts_basic_principal'].forEach(k => {
     localStorage.removeItem(k);
     sessionStorage.removeItem(k);
   });
   window.location.href = '/login';
 };

 // ── Render ────────────────────────────────────────────────────
 if (loading) {
   return (
     <div style={{ ...s.root, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
       <p style={{ color: '#b0b0a8', fontSize: '0.9rem' }}>Loading candidate…</p>
     </div>
   );
 }

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
        <a href="/dashboard"  style={s.navLink}>Dashboard</a>
        <a href="/candidates" style={s.navLink}>Candidates</a>
        {(localStorage.getItem('rts_role') ?? sessionStorage.getItem('rts_role')) === Role.ADMIN && (
          <a href="/admin/users" style={s.navLink}>Users</a>
        )}
        <a href="/profile"    style={s.navLink}>Profile</a>
         <button onClick={logout} style={s.navBtn}>Sign out</button>
       </div>
     </div>

     <div style={s.body}>

       {/* Back button */}
       <button style={s.backBtn} onClick={handleCancel}>
         <ArrowLeftOutlined style={{ fontSize: '0.8rem' }} /> Back to candidates
       </button>

       {/* Page header */}
       <div style={s.header}>
         <p style={s.eyebrow}>{isEdit ? 'Edit candidate' : 'New candidate'}</p>
         <h2 style={s.title}>
           {isEdit ? 'Update candidate details' : 'Add a new candidate'}
         </h2>
       </div>

       {/* Submit error */}
       {submitError && (
         <Alert
           type="error"
           message={submitError}
           showIcon
           closable
           onClose={() => setSubmitError(null)}
           style={{ marginBottom: '1.25rem', borderRadius: 8 }}
         />
       )}

       <div style={s.layout}>

         {/* ── Left column — form fields ── */}
         <div>

           {/* Personal information */}
           <div style={s.card}>
             <h3 style={s.cardTitle}>Personal information</h3>
             <p style={s.cardDesc}>Basic contact details about the candidate.</p>

             <div style={s.grid2}>

               {/* Name — full width */}
               <div style={s.fieldFull}>
                 <label style={s.label}>
                   Full name <span style={s.req}>*</span>
                 </label>
                 <Input
                   prefix={<UserOutlined style={{ color: '#b0b0a8' }} />}
                   placeholder="Jane Smith"
                   value={form.name}
                   onChange={e => onChange('name', e.target.value)}
                   status={errors.name ? 'error' : ''}
                   size="large"
                 />
                 {errors.name && <p style={s.errText}>{errors.name}</p>}
               </div>

               {/* Email */}
               <div style={s.field}>
                 <label style={s.label}>
                   Email address <span style={s.req}>*</span>
                 </label>
                 <Input
                   prefix={<MailOutlined style={{ color: '#b0b0a8' }} />}
                   placeholder="jane@example.com"
                   type="email"
                   value={form.email}
                   onChange={e => onChange('email', e.target.value)}
                   status={errors.email ? 'error' : ''}
                   size="large"
                 />
                 {errors.email && <p style={s.errText}>{errors.email}</p>}
               </div>

               {/* Phone */}
               <div style={s.field}>
                 <label style={s.label}>
                   Phone number <span style={s.req}>*</span>
                 </label>
                 <Input
                   prefix={<PhoneOutlined style={{ color: '#b0b0a8' }} />}
                   placeholder="+1 555 000 0000"
                   value={form.phone}
                   onChange={e => onChange('phone', e.target.value)}
                   status={errors.phone ? 'error' : ''}
                   size="large"
                 />
                 {errors.phone && <p style={s.errText}>{errors.phone}</p>}
               </div>

             </div>
           </div>

           {/* Role information */}
           <div style={s.card}>
             <h3 style={s.cardTitle}>Role information</h3>
             <p style={s.cardDesc}>Position applied for and experience level.</p>

             <div style={s.grid2}>

               {/* Position */}
               <div style={s.field}>
                 <label style={s.label}>
                   Position applied <span style={s.req}>*</span>
                 </label>
                 <Select
                   placeholder="Select position"
                   value={form.position || undefined}
                   onChange={val => onChange('position', val)}
                   status={errors.position ? 'error' : ''}
                   size="large"
                   style={{ width: '100%' }}
                   options={POSITIONS.map(p => ({ label: p, value: p }))}
                 />
                 {errors.position && <p style={s.errText}>{errors.position}</p>}
               </div>

               {/* Experience */}
               <div style={s.field}>
                 <label style={s.label}>
                   Experience level <span style={s.req}>*</span>
                 </label>
                 <Select
                   placeholder="Select experience"
                   value={form.experience || undefined}
                   onChange={val => onChange('experience', val)}
                   status={errors.experience ? 'error' : ''}
                   size="large"
                   style={{ width: '100%' }}
                   options={EXPERIENCE_LEVELS.map(e => ({ label: e, value: e }))}
                 />
                 {errors.experience && <p style={s.errText}>{errors.experience}</p>}
               </div>

               {/* Notes — full width */}
               <div style={s.fieldFull}>
                 <label style={s.label}>
                   Notes{' '}
                   <span style={{ color: '#b0b0a8', fontWeight: 400 }}>(optional)</span>
                 </label>
                 <Input.TextArea
                   placeholder="Any additional notes about the candidate…"
                   value={form.notes}
                   onChange={e => onChange('notes', e.target.value)}
                   rows={3}
                   maxLength={500}
                   showCount
                   style={{ borderRadius: 8, fontSize: '0.875rem', resize: 'none', fontFamily: 'inherit' }}
                 />
               </div>

             </div>
           </div>

           {/* Resume upload */}
           <div style={s.card}>
             <h3 style={s.cardTitle}>Resume</h3>
             <p style={s.cardDesc}>PDF, DOC, or DOCX · Max 5 MB.</p>
             <ResumeUpload
               value={resumeName}
               onChange={file => { setResumeFile(file); setResumeName(file.name); }}
               onRemove={() => { setResumeFile(null); setResumeName(null); }}
             />
           </div>

           {/* Action buttons */}
           <div style={s.actions}>
             <Button
               type="primary"
               icon={<SaveOutlined />}
               loading={saving}
               onClick={handleSubmit}
               size="large"
             >
               {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Add candidate'}
             </Button>
             <Button size="large" onClick={handleCancel}>
               Cancel
             </Button>
           </div>

         </div>

         {/* ── Right column — photo ── */}
         <div style={s.sideCard}>
           <p style={s.sideTitle}>Photo</p>
           <PhotoUpload
             value={photoPreview}
             onChange={(_file, preview) => {
               setPhotoFile(_file);
               setPhotoPreview(preview);
             }}
             onRemove={() => {
               setPhotoFile(null);
               setPhotoPreview(null);
             }}
           />
           <div style={s.sideDivider} />
           <p style={s.sideHint}>
             Optional · JPG or PNG · Max 2 MB
           </p>
         </div>

       </div>
     </div>
   </div>
 );
};

export default CandidateFormPage;
 