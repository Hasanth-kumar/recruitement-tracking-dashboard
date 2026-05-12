import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RootState } from '../../store/store';
import {
  credentialsReceived,
  profileUpdated,
  sessionExpired,
  type AuthUser,
} from './authSlice';
import { Role } from '../../constants/roles';

export interface LoginRequest {
  identifier: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginUserInfo {
  id: string;
  username: string;
  email: string;
  role: string;
}

export interface LoginResponse {
  accessToken: string;
  user: LoginUserInfo;
}

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
        { rememberMe = false },
        { dispatch, queryFulfilled }
      ) {
        try {
          const { data: res } = await queryFulfilled;
          if (res.success && res.data?.accessToken && res.data?.user) {
            dispatch(
              credentialsReceived({
                token: res.data.accessToken,
                user: mapToAuthUser(res.data.user),
                rememberMe,
              })
            );
          }
        } catch {
          /* component handles */
        }
      },
    }),

    getProfile: builder.query<ApiResponse<AuthUser>, void>({
      query: () => '/auth/me',
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
