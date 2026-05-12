import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
export type Recommendation = 'SELECT' | 'REJECT' | 'HOLD' | 'PROCEED_TO_ROUND_2';
export interface FeedbackRatings {
   technicalSkills: number;
   communicationSkills: number;
   problemSolving: number;
   culturalFit: number;
   overallRating: number;
}

export interface FeedbackSubmitDto extends FeedbackRatings {
   interviewId: string;
   candidateId: string;
   comments: string;
   recommendation: Recommendation;
}
export interface FeedbackResponseDto extends FeedbackRatings {
   id: string;
   interviewId: string;
   candidateId: string;
   interviewerName: string;
   comments: string;
   recommendation: Recommendation;
   submittedAt: string;
   round: 1 | 2;
}
export interface PendingFeedbackItem {
   interviewId: string;
   candidateId: string;
   candidateName: string;
   round: 1 | 2;
   scheduledAt: string;
   interviewerId: string;
}
function getAuthHeader(): Record<string, string> {
   const token =
       localStorage.getItem('rts_token') ?? sessionStorage.getItem('rts_token');
   return token ? { Authorization: `Basic ${token}` } : {};
}

export const feedbackApi = createApi({
   reducerPath: 'feedbackApi',
   baseQuery: fetchBaseQuery({
       baseUrl: '/api',
       prepareHeaders: headers => {
           const token = localStorage.getItem('rts_token') ?? sessionStorage.getItem('rts_token');
           if (token) headers.set('Authorization', `Basic ${token}`);
           return headers;
       },
   }),
   tagTypes: ['Feedback', 'PendingFeedback'],
   endpoints: builder => ({
       submitFeedback: builder.mutation<{ success: boolean; message: string }, FeedbackSubmitDto>({
           query: body => ({
               url: '/feedback',
               method: 'POST',
               body,
           }),
           invalidatesTags: ['Feedback', 'PendingFeedback'],
       }),

       getCandidateFeedback: builder.query<FeedbackResponseDto[], string>({
           query: candidateId => `/candidates/${candidateId}/feedback`,
           transformResponse: (res: { success: boolean; data: FeedbackResponseDto[] }) =>
               res.data ?? [],
           providesTags: ['Feedback'],
       }),

       getPendingFeedback: builder.query<PendingFeedbackItem[], void>({
           query: () => '/notifications/pending-feedback',
           transformResponse: (res: { success: boolean; data: PendingFeedbackItem[] }) =>
               res.data ?? [],
           providesTags: ['PendingFeedback'],
       }),
   }),
});
export const {
   useSubmitFeedbackMutation,
   useGetCandidateFeedbackQuery,
   useGetPendingFeedbackQuery,
} = feedbackApi;

export const MOCK_FEEDBACK: FeedbackResponseDto[] = [
   {
       id: 'FB-001',
       interviewId: 'INT-001',
       candidateId: 'RTS-SEED01',
       interviewerName: 'John Smith',
       technicalSkills: 4,
       communicationSkills: 5,
       problemSolving: 4,
       culturalFit: 5,
       overallRating: 4,
       comments: 'Strong React knowledge. Communicates clearly and asks good clarifying questions.',
       recommendation: 'PROCEED_TO_ROUND_2',
       submittedAt: new Date(Date.now() - 2 * 86400000).toISOString(),
       round: 1,
   },
   {
       id: 'FB-002',
       interviewId: 'INT-002',
       candidateId: 'RTS-SEED01',
       interviewerName: 'Priya Nair',
       technicalSkills: 3,
       communicationSkills: 4,
       problemSolving: 3,
       culturalFit: 4,
       overallRating: 4,
       comments: 'Good cultural fit. Needs improvement on system design concepts.',
       recommendation: 'SELECT',
       submittedAt: new Date(Date.now() - 1 * 86400000).toISOString(),
       round: 2,
   },
];
export const MOCK_PENDING: PendingFeedbackItem[] = [
   {
       interviewId: 'INT-003',
       candidateId: 'RTS-SEED02',
       candidateName: 'Bob Martinez',
       round: 1,
       scheduledAt: new Date(Date.now() - 26 * 3600000).toISOString(),
       interviewerId: 'user-4',
   },
   {
       interviewId: 'INT-004',
       candidateId: 'RTS-SEED03',
       candidateName: 'Clara Wong',
       round: 1,
       scheduledAt: new Date(Date.now() - 30 * 3600000).toISOString(),
       interviewerId: 'user-4',
   },
];

export async function apiFetchCandidateFeedback(
   candidateId: string,
): Promise<FeedbackResponseDto[]> {
   const res = await fetch(`/api/candidates/${candidateId}/feedback`, {
       headers: getAuthHeader(),
   });
   const data = await res.json();
   if (!data.success) throw new Error(data.message ?? 'Failed to load feedback.');
   return data.data ?? [];
}

export async function apiSubmitFeedback(
   payload: FeedbackSubmitDto,
): Promise<void> {
   const res = await fetch('/api/feedback', {
       method: 'POST',
       headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
       body: JSON.stringify(payload),
   });
   const data = await res.json();
   if (!data.success) throw new Error(data.message ?? 'Submission failed.');
}

export async function apiFetchPendingFeedback(): Promise<PendingFeedbackItem[]> {
   const res = await fetch('/api/notifications/pending-feedback', {
       headers: getAuthHeader(),
   });
   const data = await res.json();
   if (!data.success) throw new Error(data.message ?? 'Failed to load pending feedback.');
   return data.data ?? [];
}
