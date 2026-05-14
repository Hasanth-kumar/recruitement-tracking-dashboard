export type InterviewRound = 'ROUND_1' | 'ROUND_2';
export type InterviewStatus =
  | 'SCHEDULED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'RESCHEDULED';

export type InterviewDuration = 30 | 45 | 60;

export interface Interviewer {
  id: string;
  username: string;
  email: string;
}

export interface Interview {
  id: string;
  candidateId: string;
  candidateName: string;
  candidatePosition: string;
  round: InterviewRound;
  scheduledAt: string;
  interviewers: Interviewer[];
  meetingLink?: string;
  location?: string;
  duration: InterviewDuration;
  notes?: string;
  status: InterviewStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleRound1Dto {
  candidateId: string;
  scheduledAt: string;
  interviewerUsernames: string[];
  meetingLink: string;
  duration: InterviewDuration;
  notes?: string;
}

export interface ScheduleRound2Dto {
  candidateId: string;
  scheduledAt: string;
  interviewerUsernames: string[];
  location: string;
  duration: InterviewDuration;
  notes?: string;
}

/** Backend `RescheduleInterviewRequest` (PUT /interviews/{id}/reschedule) */
export interface RescheduleInterviewApiRequest {
  dateTime: string;
  durationMinutes: number;
  interviewerUsernames: string[];
  meetingLink?: string | null;
  location?: string | null;
  notes?: string | null;
  rescheduleReason?: string | null;
}

export interface RescheduleDto {
  scheduledAt: string;
  reason?: string;
}

export interface CancelDto {
  reason: string;
}

export interface InterviewPhoto {
  id: string;
  interviewId: string;
  filePath: string;
  caption?: string;
  uploadedAt: string;
}
