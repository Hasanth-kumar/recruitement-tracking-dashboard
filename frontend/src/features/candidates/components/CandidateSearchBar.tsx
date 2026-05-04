// src/features/candidates/components/CandidateSearchBar.tsx
// US-4: Search candidates by name or email

import React from 'react';
import { Input } from 'antd';
import { SearchOutlined, CloseCircleOutlined } from '@ant-design/icons';

interface Props {
 value: string;
 onChange: (value: string) => void;
 placeholder?: string;
}

const CandidateSearchBar: React.FC<Props> = ({
 value,
 onChange,
 placeholder = 'Search by name or email…',
}) => {
 return (
   <Input
     prefix={<SearchOutlined style={{ color: '#b0b0a8', fontSize: '0.9rem' }} />}
     suffix={
       value
         ? <CloseCircleOutlined
             style={{ color: '#b0b0a8', cursor: 'pointer', fontSize: '0.85rem' }}
             onClick={() => onChange('')}
           />
         : null
     }
     placeholder={placeholder}
     value={value}
     onChange={e => onChange(e.target.value)}
     style={{
       width:        280,
       borderRadius: 8,
       fontSize:     '0.875rem',
       fontFamily:   "'IBM Plex Sans', sans-serif",
     }}
     size="middle"
     allowClear={false}
   />
 );
};

export default CandidateSearchBar;
 