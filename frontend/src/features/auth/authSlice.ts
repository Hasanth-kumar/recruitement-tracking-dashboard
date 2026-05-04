import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '../../store/store';
import { Role } from '../../constants/roles';

// ── Types ──────────────────────────────────────────────────────

export interface AuthUser {
 id: string;
 username: string;
 email: string;
 fullName: string;
 role: Role;
 phone?: string;
 avatarUrl?: string;
}

export interface AuthState {
 token: string | null;
 user: AuthUser | null;
 role: Role | null;
 isAuthenticated: boolean;
}

// ── Storage helpers ────────────────────────────────────────────

const TOKEN_KEY = 'rts_token';
const ROLE_KEY  = 'rts_role';
const USER_KEY  = 'rts_user';

function readToken(): string | null {
 return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY);
}

function readRole(): Role | null {
 const r = localStorage.getItem(ROLE_KEY) ?? sessionStorage.getItem(ROLE_KEY);
 return (r as Role) ?? null;
}

function readUser(): AuthUser | null {
 try {
   const raw = localStorage.getItem(USER_KEY) ?? sessionStorage.getItem(USER_KEY);
   return raw ? (JSON.parse(raw) as AuthUser) : null;
 } catch {
   return null;
 }
}

function clearStorage() {
 [localStorage, sessionStorage].forEach(s => {
   s.removeItem(TOKEN_KEY);
   s.removeItem(ROLE_KEY);
   s.removeItem(USER_KEY);
 });
}

// ── Initial state ──────────────────────────────────────────────

const initialState: AuthState = {
 token:           readToken(),
 user:            readUser(),
 role:            readRole(),
 isAuthenticated: !!readToken(),
};

// ── Slice ──────────────────────────────────────────────────────

const authSlice = createSlice({
 name: 'auth',
 initialState,

 reducers: {
   // Called after successful POST /api/auth/login
   credentialsReceived(
     state,
     action: PayloadAction<{ token: string; user: AuthUser; rememberMe: boolean }>
   ) {
     const { token, user, rememberMe } = action.payload;
     const storage = rememberMe ? localStorage : sessionStorage;

     state.token           = token;
     state.user            = user;
     state.role            = user.role;
     state.isAuthenticated = true;

     storage.setItem(TOKEN_KEY, token);
     storage.setItem(ROLE_KEY,  user.role);
     storage.setItem(USER_KEY,  JSON.stringify(user));
   },

   // Called after PUT /api/users/profile
   profileUpdated(state, action: PayloadAction<Partial<AuthUser>>) {
     if (state.user) {
       state.user = { ...state.user, ...action.payload };
       const storage = localStorage.getItem(TOKEN_KEY) ? localStorage : sessionStorage;
       storage.setItem(USER_KEY, JSON.stringify(state.user));
     }
   },

   // Explicit logout
   loggedOut(state) {
     state.token = null;
     state.user  = null;
     state.role  = null;
     state.isAuthenticated = false;
     clearStorage();
   },

   // 401 from API — same as logout but semantically distinct
   sessionExpired(state) {
     state.token = null;
     state.user  = null;
     state.role  = null;
     state.isAuthenticated = false;
     clearStorage();
   },
 },
});

export const { credentialsReceived, profileUpdated, loggedOut, sessionExpired } = authSlice.actions;

// ── Selectors ──────────────────────────────────────────────────
export const selectToken           = (s: RootState) => s.auth.token;
export const selectAuthUser        = (s: RootState) => s.auth.user;
export const selectRole            = (s: RootState) => s.auth.role;
export const selectIsAuthenticated = (s: RootState) => s.auth.isAuthenticated;

export default authSlice.reducer;
