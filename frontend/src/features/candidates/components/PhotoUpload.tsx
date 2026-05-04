// src/features/candidates/components/PhotoUpload.tsx
// US-3: JPG/PNG photo upload with preview — max 2MB

import React, { useRef, useState } from 'react';
import { message } from 'antd';
import { CameraOutlined, DeleteOutlined } from '@ant-design/icons';

interface Props {
 value?: string | null;       // existing photo URL or base64 preview
 onChange: (file: File, previewUrl: string) => void;
 onRemove?: () => void;
 disabled?: boolean;
}

const PhotoUpload: React.FC<Props> = ({ value, onChange, onRemove, disabled }) => {
 const [hover, setHover] = useState(false);
 const fileRef = useRef<HTMLInputElement>(null);

 const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
   const file = e.target.files?.[0];
   if (!file) return;

   if (!['image/jpeg', 'image/png'].includes(file.type)) {
     message.error('Only JPG and PNG files are allowed.');
     return;
   }
   if (file.size > 2 * 1024 * 1024) {
     message.error('Photo must be under 2 MB.');
     return;
   }

   const reader = new FileReader();
   reader.onload = ev => {
     onChange(file, ev.target?.result as string);
   };
   reader.readAsDataURL(file);
   e.target.value = '';
 };

 return (
   <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
     {/* Photo circle */}
     <div
       onClick={() => !disabled && fileRef.current?.click()}
       onMouseEnter={() => setHover(true)}
       onMouseLeave={() => setHover(false)}
       style={{
         width: 96,
         height: 96,
         borderRadius: '50%',
         border: `2px dashed ${hover && !disabled ? '#2563eb' : '#e4e4e0'}`,
         background: '#f9f9f8',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         cursor: disabled ? 'not-allowed' : 'pointer',
         overflow: 'hidden',
         position: 'relative',
         transition: 'border-color 150ms',
         flexShrink: 0,
       }}
     >
       {value ? (
         <>
           <img src={value} alt="Candidate" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
           {/* Hover overlay */}
           <div style={{
             position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)',
             display: 'flex', alignItems: 'center', justifyContent: 'center',
             opacity: hover && !disabled ? 1 : 0, transition: 'opacity 150ms',
             color: '#fff', fontSize: '1.1rem',
           }}>
             <CameraOutlined />
           </div>
         </>
       ) : (
         <div style={{ textAlign: 'center', color: '#b0b0a8' }}>
           <CameraOutlined style={{ fontSize: '1.4rem', display: 'block', marginBottom: 4 }} />
           <span style={{ fontSize: '0.7rem' }}>Photo</span>
         </div>
       )}
     </div>

     {/* Remove button */}
     {value && onRemove && !disabled && (
       <button
         type="button"
         onClick={onRemove}
         style={{
           display: 'flex', alignItems: 'center', gap: 4,
           fontSize: '0.75rem', color: '#dc2626', background: 'none',
           border: 'none', cursor: 'pointer', padding: 0,
         }}
       >
         <DeleteOutlined /> Remove
       </button>
     )}

     <p style={{ fontSize: '0.7rem', color: '#b0b0a8', textAlign: 'center', margin: 0 }}>
       JPG or PNG · Max 2 MB
     </p>

     <input
       ref={fileRef}
       type="file"
       accept="image/jpeg,image/png"
       style={{ display: 'none' }}
       onChange={handleFile}
       disabled={disabled}
     />
   </div>
 );
};

export default PhotoUpload;
 