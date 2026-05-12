import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export type Recommendation = 'SELECT' | 'REJECT' | 'HOLD' | 'PROCEED';

export interface SubmitFeedbackRequest {
  interviewId: string;
  technicalRating: number;
  communicationRating: number;
  problemSolvingRating: number;
  leadershipRating: number;
  cultureRating: number;
  recommendation: Recommendation;
  comments?: string;
}

export interface FeedbackResponseDto {
  id: string;
  interviewId: string;
  candidateId: string;
  submittedByUsername: string;
  technicalRating: number;
  communicationRating: number;
  problemSolvingRating: number;
  leadershipRating: number;
  cultureRating: number;
  recommendation: Recommendation;
  comments: string;
  submittedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface CandidateFeedbackSummaryResponse {
  candidateId: string;
  totalFeedbackCount: number;
  averageTechnicalRating: number | null;
  averageCommunicationRating: number | null;
  averageProblemSolvingRating: number | null;
  averageLeadershipRating: number | null;
  averageCultureRating: number | null;
  overallAverageRating: number | null;
  feedbacks: FeedbackResponseDto[];
}

export type InterviewRound = 'ROUND_1' | 'ROUND_2';
export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface InterviewResponseDto {
  id: string;
  candidateId: string;
  round: InterviewRound;
  dateTime: string;
  durationMinutes: number;
  meetingLink: string | null;
  location: string | null;
  notes: string | null;
  status: InterviewStatus;
  interviewerUsernames: string[];
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

function getAuthHeader(): Record<string, string> {
  const token =
    localStorage.getItem('rts_token') ?? sessionStorage.getItem('rts_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export const feedbackApi = createApi({
  reducerPath: 'feedbackApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api',
    prepareHeaders: (headers) => {
      const token =
        localStorage.getItem('rts_token') ?? sessionStorage.getItem('rts_token');
      if (token) headers.set('Authorization', `Bearer ${token}`);
      return headers;
    },
  }),
  tagTypes: ['Feedback', 'Interviews'],
  endpoints: (builder) => ({
    submitFeedback: builder.mutation<
      ApiResponse<FeedbackResponseDto>,
      SubmitFeedbackRequest
    >({
      query: (body) => ({
        url: '/feedback',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Feedback', 'Interviews'],
    }),

    getCandidateFeedback: builder.query<
      CandidateFeedbackSummaryResponse,
      string
    >({
      query: (candidateId) => `/candidates/${candidateId}/feedback`,
      transformResponse: (res: ApiResponse<CandidateFeedbackSummaryResponse>) =>
        res.data,
      providesTags: ['Feedback'],
    }),

    getInterviewSchedule: builder.query<
      InterviewResponseDto[],
      { from: string; to: string; interviewerUsername?: string }
    >({
      query: ({ from, to, interviewerUsername }) => {
        let url = `/interviews/schedule?fromDateTime=${from}&toDateTime=${to}`;
        if (interviewerUsername) url += `&interviewerUsername=${encodeURIComponent(interviewerUsername)}`;
        return url;
      },
      transformResponse: (res: ApiResponse<InterviewResponseDto[]>) =>
        res.data ?? [],
      providesTags: ['Interviews'],
    }),
  }),
});

export const {
  useSubmitFeedbackMutation,
  useGetCandidateFeedbackQuery,
  useGetInterviewScheduleQuery,
} = feedbackApi;

export async function apiFetchCandidateFeedback(
  candidateId: string,
): Promise<CandidateFeedbackSummaryResponse> {
  const res = await fetch(`/api/candidates/${candidateId}/feedback`, {
    headers: getAuthHeader(),
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message ?? 'Failed to load feedback.');
  return data.data;
}

export async function apiSubmitFeedback(
  payload: SubmitFeedbackRequest,
): Promise<FeedbackResponseDto> {
  const res = await fetch('/api/feedback', {
    method: 'POST',
    headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message ?? 'Submission failed.');
  return data.data;
}

export async function apiFetchInterviewSchedule(
  from: string,
  to: string,
  interviewerUsername?: string,
): Promise<InterviewResponseDto[]> {
  let url = `/api/interviews/schedule?fromDateTime=${from}&toDateTime=${to}`;
  if (interviewerUsername) url += `&interviewerUsername=${encodeURIComponent(interviewerUsername)}`;
  const res = await fetch(url, { headers: getAuthHeader() });
  const data = await res.json();
  if (!data.success) throw new Error(data.message ?? 'Failed to load interviews.');
  return data.data ?? [];
}
