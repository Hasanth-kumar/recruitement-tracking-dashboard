// src/features/candidates/components/ResumeUpload.tsx
// US-2: PDF/DOC/DOCX resume upload — max 5MB

import React, { useRef, useState } from 'react';
import { message } from 'antd';
import { PaperClipOutlined, DeleteOutlined, FileTextOutlined } from '@ant-design/icons';

interface Props {
 value?: string | null;      // existing file name or URL
 onChange: (file: File) => void;
 onRemove?: () => void;
 disabled?: boolean;
}

const ALLOWED_TYPES = [
 'application/pdf',
 'application/msword',
 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
];

const ALLOWED_EXTS = ['.pdf', '.doc', '.docx'];

const ResumeUpload: React.FC<Props> = ({ value, onChange, onRemove, disabled }) => {
 const [dragOver, setDragOver] = useState(false);
 const fileRef = useRef<HTMLInputElement>(null);

 const validate = (file: File): boolean => {
   const ext = '.' + file.name.split('.').pop()?.toLowerCase();
   if (!ALLOWED_TYPES.includes(file.type) && !ALLOWED_EXTS.includes(ext)) {
     message.error('Only PDF, DOC, and DOCX files are allowed.');
     return false;
   }
   if (file.size > 5 * 1024 * 1024) {
     message.error('Resume must be under 5 MB.');
     return false;
   }
   return true;
 };

 const handleFile = (file: File) => {
   if (validate(file)) onChange(file);
 };

 const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
   const file = e.target.files?.[0];
   if (file) handleFile(file);
   e.target.value = '';
 };

 const handleDrop = (e: React.DragEvent) => {
   e.preventDefault();
   setDragOver(false);
   if (disabled) return;
   const file = e.dataTransfer.files?.[0];
   if (file) handleFile(file);
 };

 // Extract a display name from URL or use the raw value
 const displayName = value
   ? value.startsWith('blob:') || value.startsWith('data:')
     ? 'Uploaded file'
     : value.split('/').pop() ?? value
   : null;

 return (
   <div>
     {value ? (
       // File already uploaded — show name + replace/remove options
       <div style={{
         display: 'flex', alignItems: 'center', gap: 10,
         padding: '10px 14px',
         background: '#f0f7ff',
         border: '1px solid #bfdbfe',
         borderRadius: 8,
       }}>
         <FileTextOutlined style={{ color: '#2563eb', fontSize: '1.1rem', flexShrink: 0 }} />
         <span style={{ fontSize: '0.85rem', color: '#1a1a18', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
           {displayName}
         </span>
         {!disabled && (
           <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
             <button
               type="button"
               onClick={() => fileRef.current?.click()}
               style={{ fontSize: '0.75rem', color: '#2563eb', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
             >
               Replace
             </button>
             {onRemove && (
               <button
                 type="button"
                 onClick={onRemove}
                 style={{ fontSize: '0.75rem', color: '#dc2626', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
               >
                 <DeleteOutlined />
               </button>
             )}
           </div>
         )}
       </div>
     ) : (
       // Drop zone
       <div
         onClick={() => !disabled && fileRef.current?.click()}
         onDragOver={e => { e.preventDefault(); if (!disabled) setDragOver(true); }}
         onDragLeave={() => setDragOver(false)}
         onDrop={handleDrop}
         style={{
           border: `1.5px dashed ${dragOver ? '#2563eb' : '#e4e4e0'}`,
           borderRadius: 8,
           padding: '1.25rem',
           textAlign: 'center',
           cursor: disabled ? 'not-allowed' : 'pointer',
           background: dragOver ? '#eff4ff' : '#fafafa',
           transition: 'all 150ms',
         }}
       >
         <PaperClipOutlined style={{ fontSize: '1.4rem', color: '#b0b0a8', display: 'block', marginBottom: 6 }} />
         <p style={{ fontSize: '0.85rem', color: '#6b6b65', margin: '0 0 2px' }}>
           Click or drag file here
         </p>
         <p style={{ fontSize: '0.75rem', color: '#b0b0a8', margin: 0 }}>
           PDF, DOC, DOCX · Max 5 MB
         </p>
       </div>
     )}

     <input
       ref={fileRef}
       type="file"
       accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
       style={{ display: 'none' }}
       onChange={handleInputChange}
       disabled={disabled}
     />
   </div>
 );
};

export default ResumeUpload;
 