import { useDispatch, useSelector } from 'react-redux';
import type { TypedUseSelectorHook } from 'react-redux';
import type { RootState, AppDispatch } from '../../store/store';
import {
 selectAuthUser,
 selectRole,
 selectToken,
 selectIsAuthenticated,
 loggedOut,
} from '../../features/auth/authSlice';
import { Role, ROLE_PERMISSIONS, type Permission } from '../../constants/roles';

export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;

export function useAuth() {
 const dispatch        = useAppDispatch();
 const user            = useAppSelector(selectAuthUser);
 const role            = useAppSelector(selectRole);
 const token           = useAppSelector(selectToken);
 const isAuthenticated = useAppSelector(selectIsAuthenticated);

 // Check if current role has a given permission (US-25)
 const can = (permission: Permission): boolean => {
   if (!role) return false;
   return ROLE_PERMISSIONS[role]?.includes(permission) ?? false;
 };

 const hasRole = (...roles: Role[]): boolean => {
   if (!role) return false;
   return roles.includes(role);
 };

 const logout = () => dispatch(loggedOut());

 return {
   user,
   role,
   token,
   isAuthenticated,
   can,
   hasRole,
   logout,
   isAdmin:       role === Role.ADMIN,
   isHrManager:   role === Role.HR_MANAGER,
   isRecruiter:   role === Role.RECRUITER,
   isInterviewer: role === Role.INTERVIEWER,
 };
}
