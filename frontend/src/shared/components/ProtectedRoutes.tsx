import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Role } from '../../constants/roles';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: Role[];
}

function readToken(): string | null {
  return localStorage.getItem('rts_token') ?? sessionStorage.getItem('rts_token');
}

function readRole(): Role | null {
  const role = localStorage.getItem('rts_role') ?? sessionStorage.getItem('rts_role');
  return (role as Role) ?? null;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const location = useLocation();
  const token = readToken();
  const role = readRole();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (allowedRoles && (!role || !allowedRoles.includes(role))) {
    return <Navigate to="/403" replace state={{ from: location }} />;
  }
  return <>{children}</>;
};

export default ProtectedRoute;
 