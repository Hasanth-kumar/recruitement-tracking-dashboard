import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RootState } from '../../store/store';
import { credentialsReceived, profileUpdated, sessionExpired, AuthUser } from './authSlice';
// import type { AuthUser } from './authSlice';

// ── DTOs (mirror backend) ──────────────────────────────────────

export interface LoginRequest {
 identifier: string;
 password: string;
 rememberMe?: boolean;
}

export interface LoginResponse {
 token: string;
 tokenType: string;
 expiresIn: number;
 user: AuthUser;
}

export interface UpdateProfileRequest {
 fullName?: string;
 email?: string;
 phone?: string;
}

export interface ChangePasswordRequest {
 currentPassword: string;
 newPassword: string;
}

interface ApiResponse<T> {
 success: boolean;
 message: string;
 data: T;
 timestamp: string;
}

// ── Base query ─────────────────────────────────────────────────

const baseQuery = fetchBaseQuery({
 baseUrl: '/api',
 prepareHeaders: (headers, { getState }) => {
   const token = (getState() as RootState).auth.token;
   if (token) headers.set('Authorization', `Bearer ${token}`);
   headers.set('Content-Type', 'application/json');
   return headers;
 },
});

const baseQueryWithAuthGuard: typeof baseQuery = async (args, api, extra) => {
 const result = await baseQuery(args, api, extra);
 if (result.error?.status === 401) api.dispatch(sessionExpired());
 return result;
};

// ── API slice ──────────────────────────────────────────────────

export const authApi = createApi({
 reducerPath: 'authApi',
 baseQuery: baseQueryWithAuthGuard,
 tagTypes: ['Profile'],

 endpoints: (builder) => ({

   login: builder.mutation<ApiResponse<LoginResponse>, LoginRequest>({
     query: ({ identifier, password }) => ({
       url: '/auth/login',
       method: 'POST',
       body: { identifier, password },
     }),
     async onQueryStarted({ rememberMe = false }, { dispatch, queryFulfilled }) {
       try {
         const { data: res } = await queryFulfilled;
         if (res.success) {
           dispatch(credentialsReceived({ token: res.data.token, user: res.data.user, rememberMe }));
         }
       } catch { /* component handles */ }
     },
   }),

   getProfile: builder.query<ApiResponse<AuthUser>, void>({
     query: () => '/users/profile',
     providesTags: ['Profile'],
   }),

   updateProfile: builder.mutation<ApiResponse<AuthUser>, UpdateProfileRequest>({
     query: (body) => ({ url: '/users/profile', method: 'PUT', body }),
     invalidatesTags: ['Profile'],
     async onQueryStarted(_patch, { dispatch, queryFulfilled }) {
       try {
         const { data: res } = await queryFulfilled;
         if (res.success) dispatch(profileUpdated(res.data));
       } catch { /* component handles */ }
     },
   }),

   changePassword: builder.mutation<ApiResponse<null>, ChangePasswordRequest>({
     query: (body) => ({ url: '/users/password', method: 'PUT', body }),
   }),

   uploadAvatar: builder.mutation<ApiResponse<{ avatarUrl: string }>, FormData>({
     query: (formData) => ({ url: '/users/profile/avatar', method: 'POST', body: formData, formData: true }),
     invalidatesTags: ['Profile'],
     async onQueryStarted(_arg, { dispatch, queryFulfilled }) {
       try {
         const { data: res } = await queryFulfilled;
         if (res.success) dispatch(profileUpdated({ avatarUrl: res.data.avatarUrl }));
       } catch { /* component handles */ }
     },
   }),
 }),
});

export const {
 useLoginMutation,
 useGetProfileQuery,
 useUpdateProfileMutation,
 useChangePasswordMutation,
 useUploadAvatarMutation,
} = authApi;
