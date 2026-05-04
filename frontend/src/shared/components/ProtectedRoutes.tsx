// src/shared/components/ProtectedRoute.tsx
// Role-based route guard — redirects to /login if not authenticated

import React from 'react';
import { Navigate } from 'react-router-dom';

function isLoggedIn(): boolean {
 return !!(
   localStorage.getItem('rts_token') ??
   sessionStorage.getItem('rts_token')
 );
}

interface Props {
 children: React.ReactNode;
}

const ProtectedRoute: React.FC<Props> = ({ children }) => {
 return isLoggedIn() ? <>{children}</> : <Navigate to="/login" replace />;
};

export default ProtectedRoute;
 