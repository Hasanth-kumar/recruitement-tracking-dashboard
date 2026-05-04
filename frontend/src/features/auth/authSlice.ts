import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '../../store/store';
import { Role } from '../../constants/roles';

// ── Types ──────────────────────────────────────────────────────

/** Mirrors backend UserProfileResponse and login UserInfo. */
export interface AuthUser {
 id: string;
 username: string;
 email: string;
 role: Role;
}

export interface AuthState {
 /** Base64(`usernameOrEmail:password`) for {@code Authorization: Basic …}. */
 token: string | null;
 /** Same principal string passed to Basic auth (login identifier). */
 basicAuthPrincipal: string | null;
 user: AuthUser | null;
 role: Role | null;
 isAuthenticated: boolean;
}

// ── Storage helpers ────────────────────────────────────────────

const TOKEN_KEY     = 'rts_token';
const ROLE_KEY      = 'rts_role';
const USER_KEY      = 'rts_user';
const PRINCIPAL_KEY = 'rts_basic_principal';

function readToken(): string | null {
 return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY);
}

function readRole(): Role | null {
 const r = localStorage.getItem(ROLE_KEY) ?? sessionStorage.getItem(ROLE_KEY);
 return (r as Role) ?? null;
}

function readBasicPrincipal(): string | null {
 return localStorage.getItem(PRINCIPAL_KEY) ?? sessionStorage.getItem(PRINCIPAL_KEY);
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
   s.removeItem(PRINCIPAL_KEY);
 });
}

// ── Initial state ──────────────────────────────────────────────

const initialState: AuthState = {
 token:              readToken(),
 basicAuthPrincipal: readBasicPrincipal(),
 user:               readUser(),
 role:               readRole(),
 isAuthenticated:  !!readToken(),
};

// ── Slice ──────────────────────────────────────────────────────

const authSlice = createSlice({
 name: 'auth',
 initialState,

 reducers: {
   // After successful POST /api/auth/login (credentials = base64 for Basic auth).
   credentialsReceived(
     state,
     action: PayloadAction<{
       token: string;
       user: AuthUser;
       rememberMe: boolean;
       basicAuthPrincipal: string;
     }>
   ) {
     const { token, user, rememberMe, basicAuthPrincipal } = action.payload;
     const storage = rememberMe ? localStorage : sessionStorage;

     state.token              = token;
     state.basicAuthPrincipal = basicAuthPrincipal;
     state.user               = user;
     state.role               = user.role;
     state.isAuthenticated    = true;

     storage.setItem(TOKEN_KEY, token);
     storage.setItem(ROLE_KEY, user.role);
     storage.setItem(USER_KEY, JSON.stringify(user));
     storage.setItem(PRINCIPAL_KEY, basicAuthPrincipal);
   },

   /** After password change: same principal, new password → new Basic secret. */
   basicCredentialsRefreshed(state, action: PayloadAction<{ token: string }>) {
     state.token = action.payload.token;
     const storage = localStorage.getItem(TOKEN_KEY) ? localStorage : sessionStorage;
     storage.setItem(TOKEN_KEY, action.payload.token);
   },

   // Called after PUT /api/users/profile
   profileUpdated(state, action: PayloadAction<Partial<AuthUser>>) {
     if (state.user) {
       state.user = { ...state.user, ...action.payload };
       state.role = state.user.role;
       const storage = localStorage.getItem(TOKEN_KEY) ? localStorage : sessionStorage;
       storage.setItem(USER_KEY, JSON.stringify(state.user));
       storage.setItem(ROLE_KEY, state.user.role);
     }
   },

   // Explicit logout
   loggedOut(state) {
     state.token = null;
     state.basicAuthPrincipal = null;
     state.user  = null;
     state.role  = null;
     state.isAuthenticated = false;
     clearStorage();
   },

   // 401 from API — same as logout but semantically distinct
   sessionExpired(state) {
     state.token = null;
     state.basicAuthPrincipal = null;
     state.user  = null;
     state.role  = null;
     state.isAuthenticated = false;
     clearStorage();
   },
 },
});

export const {
 credentialsReceived,
 basicCredentialsRefreshed,
 profileUpdated,
 loggedOut,
 sessionExpired,
} = authSlice.actions;

// ── Selectors ──────────────────────────────────────────────────
export const selectToken              = (s: RootState) => s.auth.token;
export const selectBasicAuthPrincipal = (s: RootState) => s.auth.basicAuthPrincipal;
export const selectAuthUser           = (s: RootState) => s.auth.user;
export const selectRole               = (s: RootState) => s.auth.role;
export const selectIsAuthenticated    = (s: RootState) => s.auth.isAuthenticated;

export default authSlice.reducer;
