import { createApi } from '@reduxjs/toolkit/query/react';
import type { BaseQueryFn } from '@reduxjs/toolkit/query';
import type { AxiosRequestConfig } from 'axios';
import axiosInstance from '../../shared/utils/axiosInstance';
import type {
  Interview,
  ScheduleRound1Dto,
  ScheduleRound2Dto,
  CancelDto,
  Interviewer,
  InterviewPhoto,
  RescheduleInterviewApiRequest,
} from './interviewTypes';
import {
  buildScheduleQueryParams,
  mapApiUserToInterviewer,
  mapInterviewApiRow,
  mapInterviewPhotoApiRow,
  scheduledAtToLocalDateTime,
} from './interviewMappers';
import {
  mockGetAllInterviews,
  mockGetInterviewById,
  mockGetInterviewsByCandidate,
  mockScheduleRound1,
  mockScheduleRound2,
  mockRescheduleInterview,
  mockCancelInterview,
  mockGetPhotosByInterview,
  mockUploadPhoto,
  mockDeletePhoto,
  MOCK_INTERVIEWERS,
} from './interviewMock';

/** Set `VITE_USE_INTERVIEW_MOCK=true` in `.env` to use sessionStorage mock data. */
const USE_MOCK = import.meta.env.VITE_USE_INTERVIEW_MOCK === 'true';

const axiosBaseQuery =
  (): BaseQueryFn<AxiosRequestConfig, unknown, string> =>
  async requestConfig => {
    try {
      const response = await axiosInstance(requestConfig);
      return { data: response.data?.data ?? response.data };
    } catch (error: unknown) {
      const msg =
        error instanceof Error ? error.message : 'Unexpected error.';
      return { error: msg };
    }
  };

function getCandidateInfoFromSession(id: string): { name: string; position: string } {
  try {
    const raw = sessionStorage.getItem('rts_candidates');
    if (raw) {
      const candidates = JSON.parse(raw) as Array<{
        id: string;
        name: string;
        position: string;
      }>;
      const found = candidates.find(c => c.id === id);
      if (found) return { name: found.name, position: found.position };
    }
  } catch {
    /* ignore */
  }
  return { name: 'Unknown Candidate', position: 'Unknown Position' };
}

async function fetchCandidateLookup(): Promise<
  Record<string, { name: string; position: string }>
> {
  const res = await axiosInstance({
    url: '/candidates',
    method: 'GET',
    params: { page: 0, size: 500 },
  });
  const paged = res.data?.data ?? res.data;
  const content = (paged?.content ?? []) as Record<string, unknown>[];
  return Object.fromEntries(
    content.map(c => [
      String(c.id),
      { name: String(c.name), position: String(c.position ?? '') },
    ]),
  );
}

export const interviewApi = createApi({
  reducerPath: 'interviewApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Interview', 'InterviewPhoto'],
  endpoints: builder => ({
    getAllInterviews: builder.query<Interview[], string | undefined>({
      queryFn: async (interviewerUsername = undefined) => {
        if (USE_MOCK) return { data: mockGetAllInterviews() };
        try {
          const [scheduleRes, lookup] = await Promise.all([
            axiosInstance({
              url: '/interviews/schedule',
              method: 'GET',
              params: buildScheduleQueryParams(interviewerUsername),
            }),
            fetchCandidateLookup(),
          ]);
          const rows = (scheduleRes.data?.data ?? scheduleRes.data) as Record<string, unknown>[];
          const list = Array.isArray(rows) ? rows : [];
          return { data: list.map(r => mapInterviewApiRow(r, lookup)) };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      providesTags: ['Interview'],
    }),
    getInterviewsByCandidate: builder.query<Interview[], string>({
      queryFn: async candidateId => {
        if (USE_MOCK) return { data: mockGetInterviewsByCandidate(candidateId) };
        try {
          const [scheduleRes, lookup] = await Promise.all([
            axiosInstance({
              url: '/interviews/schedule',
              method: 'GET',
              params: buildScheduleQueryParams(),
            }),
            fetchCandidateLookup(),
          ]);
          const rows = (scheduleRes.data?.data ?? scheduleRes.data) as Record<string, unknown>[];
          const list = Array.isArray(rows) ? rows : [];
          const mapped = list.map(r => mapInterviewApiRow(r, lookup));
          return { data: mapped.filter(iv => iv.candidateId === candidateId) };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      providesTags: ['Interview'],
    }),
    getInterviewById: builder.query<Interview | null, string>({
      queryFn: async id => {
        if (USE_MOCK) return { data: mockGetInterviewById(id) };
        try {
          const [scheduleRes, lookup] = await Promise.all([
            axiosInstance({
              url: '/interviews/schedule',
              method: 'GET',
              params: buildScheduleQueryParams(),
            }),
            fetchCandidateLookup(),
          ]);
          const rows = (scheduleRes.data?.data ?? scheduleRes.data) as Record<string, unknown>[];
          const list = Array.isArray(rows) ? rows : [];
          const mapped = list.map(r => mapInterviewApiRow(r, lookup));
          return { data: mapped.find(iv => iv.id === id) ?? null };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      providesTags: ['Interview'],
    }),
    getInterviewers: builder.query<Interviewer[], void>({
      queryFn: async () => {
        if (USE_MOCK) return { data: MOCK_INTERVIEWERS };
        try {
          const response = await axiosInstance({
            url: '/interviews/interviewer-options',
            method: 'GET',
          });
          const rows = (response.data?.data ?? response.data) as Record<string, unknown>[];
          const list = Array.isArray(rows) ? rows : [];
          return { data: list.map(mapApiUserToInterviewer) };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
    }),
    scheduleRound1: builder.mutation<Interview, ScheduleRound1Dto>({
      queryFn: async dto => {
        if (USE_MOCK) {
          const { name, position } = getCandidateInfoFromSession(dto.candidateId);
          return { data: mockScheduleRound1(dto, name, position) };
        }
        try {
          const response = await axiosInstance({
            url: '/interviews/round1',
            method: 'POST',
            data: {
              candidateId: dto.candidateId,
              dateTime: scheduledAtToLocalDateTime(dto.scheduledAt),
              durationMinutes: dto.duration,
              meetingLink: dto.meetingLink,
              interviewerUsernames: dto.interviewerUsernames,
              notes: dto.notes ?? null,
            },
          });
          const row = response.data?.data ?? response.data;
          const lookup = await fetchCandidateLookup();
          return { data: mapInterviewApiRow(row as Record<string, unknown>, lookup) };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      invalidatesTags: ['Interview'],
    }),
    scheduleRound2: builder.mutation<Interview, ScheduleRound2Dto>({
      queryFn: async dto => {
        if (USE_MOCK) {
          const { name, position } = getCandidateInfoFromSession(dto.candidateId);
          return { data: mockScheduleRound2(dto, name, position) };
        }
        try {
          const response = await axiosInstance({
            url: '/interviews/round2',
            method: 'POST',
            data: {
              candidateId: dto.candidateId,
              dateTime: scheduledAtToLocalDateTime(dto.scheduledAt),
              durationMinutes: dto.duration,
              location: dto.location,
              interviewerUsernames: dto.interviewerUsernames,
              notes: dto.notes ?? null,
            },
          });
          const row = response.data?.data ?? response.data;
          const lookup = await fetchCandidateLookup();
          return { data: mapInterviewApiRow(row as Record<string, unknown>, lookup) };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      invalidatesTags: ['Interview'],
    }),
    rescheduleInterview: builder.mutation<
      Interview | null,
      { id: string; body: RescheduleInterviewApiRequest }
    >({
      queryFn: async ({ id, body }) => {
        if (USE_MOCK) {
          return {
            data: mockRescheduleInterview(id, {
              scheduledAt: body.dateTime.slice(0, 16),
              reason: body.rescheduleReason ?? undefined,
            }),
          };
        }
        try {
          const response = await axiosInstance({
            url: `/interviews/${id}/reschedule`,
            method: 'PUT',
            data: body,
          });
          const row = response.data?.data ?? response.data;
          const lookup = await fetchCandidateLookup();
          return { data: mapInterviewApiRow(row as Record<string, unknown>, lookup) };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      invalidatesTags: ['Interview'],
    }),
    cancelInterview: builder.mutation<Interview | null, { id: string; dto: CancelDto }>({
      queryFn: async ({ id, dto }) => {
        if (USE_MOCK) return { data: mockCancelInterview(id, dto) };
        try {
          const response = await axiosInstance({
            url: `/interviews/${id}/cancel`,
            method: 'PATCH',
            data: { reason: dto.reason },
          });
          const row = response.data?.data ?? response.data;
          const lookup = await fetchCandidateLookup();
          return { data: mapInterviewApiRow(row as Record<string, unknown>, lookup) };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      invalidatesTags: ['Interview'],
    }),
    getInterviewPhotos: builder.query<InterviewPhoto[], string>({
      queryFn: async interviewId => {
        if (USE_MOCK) return { data: mockGetPhotosByInterview(interviewId) };
        try {
          const response = await axiosInstance({
            url: `/interviews/${interviewId}/photos`,
            method: 'GET',
          });
          const rows = (response.data?.data ?? response.data) as Record<string, unknown>[];
          const list = Array.isArray(rows) ? rows : [];
          return { data: list.map(mapInterviewPhotoApiRow) };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      providesTags: ['InterviewPhoto'],
    }),
    uploadInterviewPhoto: builder.mutation<
      InterviewPhoto,
      { interviewId: string; file: File; caption?: string }
    >({
      queryFn: async ({ interviewId, file, caption }) => {
        if (USE_MOCK) {
          return { data: mockUploadPhoto(interviewId, caption) };
        }
        try {
          const form = new FormData();
          form.append('files', file);
          if (caption != null && caption !== '') {
            form.append('captions', caption);
          }
          const response = await axiosInstance({
            url: `/interviews/${interviewId}/photos`,
            method: 'POST',
            data: form,
          });
          const rows = (response.data?.data ?? response.data) as Record<string, unknown>[];
          const list = Array.isArray(rows) ? rows : [];
          const mapped = list.map(mapInterviewPhotoApiRow);
          if (mapped.length === 0) {
            return { error: 'Server returned no photo data.' };
          }
          return { data: mapped[0] };
        } catch (e) {
          return { error: e instanceof Error ? e.message : 'Error' };
        }
      },
      invalidatesTags: ['InterviewPhoto'],
    }),
    deleteInterviewPhoto: builder.mutation<boolean, string>({
      queryFn: async photoId => {
        if (USE_MOCK) return { data: mockDeletePhoto(photoId) };
        return { error: 'Deleting interview photos is not supported by the server yet.' };
      },
      invalidatesTags: ['InterviewPhoto'],
    }),
  }),
});

export const {
  useGetAllInterviewsQuery,
  useGetInterviewsByCandidateQuery,
  useGetInterviewByIdQuery,
  useGetInterviewersQuery,
  useScheduleRound1Mutation,
  useScheduleRound2Mutation,
  useRescheduleInterviewMutation,
  useCancelInterviewMutation,
  useGetInterviewPhotosQuery,
  useUploadInterviewPhotoMutation,
  useDeleteInterviewPhotoMutation,
} = interviewApi;
