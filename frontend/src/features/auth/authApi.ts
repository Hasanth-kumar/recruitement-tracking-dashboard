import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RootState } from '../../store/store';
import {
  credentialsReceived,
  profileUpdated,
  sessionExpired,
  type AuthUser,
} from './authSlice';
import { Role } from '../../constants/roles';
import { authorizationBasicHeader, encodeBasicAuth } from '../../shared/utils/basicAuth';

// ── DTOs (mirror backend) ──────────────────────────────────────

export interface LoginRequest {
  identifier: string;
  password: string;
  rememberMe?: boolean;
}

/** Backend LoginResponse: only nested user, no JWT. */
export interface LoginUserInfo {
  id: string;
  username: string;
  email: string;
  role: string;
}

export interface LoginResponse {
  user: LoginUserInfo;
}

/** Backend UpdateUserProfileRequest — all password fields required together when changing password. */
export interface UpdateProfileRequest {
  username?: string;
  email?: string;
  currentPassword?: string;
  newPassword?: string;
  confirmNewPassword?: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export function mapToAuthUser(data: {
  id: string;
  username: string;
  email: string;
  role: string;
}): AuthUser {
  return {
    id: data.id,
    username: data.username,
    email: data.email,
    role: data.role as Role,
  };
}

// ── Base query ─────────────────────────────────────────────────

const baseQuery = fetchBaseQuery({
  baseUrl: '/api',
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as RootState).auth.token;
    if (token) headers.set('Authorization', authorizationBasicHeader(token));
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
  tagTypes: ['Profile', 'AdminUsers'],

  endpoints: (builder) => ({
    login: builder.mutation<ApiResponse<LoginResponse>, LoginRequest>({
      query: ({ identifier, password }) => ({
        url: '/auth/login',
        method: 'POST',
        body: { usernameOrEmail: identifier.trim(), password },
      }),
      async onQueryStarted(
        { identifier, password, rememberMe = false },
        { dispatch, queryFulfilled }
      ) {
        try {
          const { data: res } = await queryFulfilled;
          if (res.success && res.data?.user) {
            const principal = identifier.trim();
            dispatch(
              credentialsReceived({
                token: encodeBasicAuth(principal, password),
                user: mapToAuthUser(res.data.user),
                rememberMe,
                basicAuthPrincipal: principal,
              })
            );
          }
        } catch {
          /* component handles */
        }
      },
    }),

    getProfile: builder.query<ApiResponse<AuthUser>, void>({
      query: () => '/users/profile',
      transformResponse: (res: ApiResponse<AuthUser>) => ({
        ...res,
        data: mapToAuthUser(res.data as unknown as LoginUserInfo),
      }),
      providesTags: ['Profile'],
    }),

    updateProfile: builder.mutation<ApiResponse<AuthUser>, UpdateProfileRequest>({
      query: (body) => ({ url: '/users/profile', method: 'PUT', body }),
      invalidatesTags: ['Profile'],
      async onQueryStarted(_patch, { dispatch, queryFulfilled }) {
        try {
          const { data: res } = await queryFulfilled;
          if (res.success) dispatch(profileUpdated(mapToAuthUser(res.data as unknown as LoginUserInfo)));
        } catch {
          /* component handles */
        }
      },
    }),

    getAdminUsers: builder.query<ApiResponse<AuthUser[]>, void>({
      query: () => '/admin/users',
      transformResponse: (res: ApiResponse<LoginUserInfo[]>) => ({
        ...res,
        data: (res.data ?? []).map((u) => mapToAuthUser(u)),
      }),
      providesTags: ['AdminUsers'],
    }),

    updateUserRole: builder.mutation<
      ApiResponse<AuthUser>,
      { userId: string; role: Role }
    >({
      query: ({ userId, role }) => ({
        url: `/admin/users/${userId}/role`,
        method: 'PUT',
        body: { role },
      }),
      invalidatesTags: ['AdminUsers'],
    }),
  }),
});

export const {
  useLoginMutation,
  useGetProfileQuery,
  useUpdateProfileMutation,
  useGetAdminUsersQuery,
  useUpdateUserRoleMutation,
} = authApi;
