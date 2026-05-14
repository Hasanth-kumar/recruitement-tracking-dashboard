/** Mirrors backend `ScheduleRoundOneInterviewRequest` / `ScheduleRoundTwoInterviewRequest` constraints. */

export const INTERVIEW_MEETING_LINK_PATTERN = /^https?:\/\/.+/i;
export const INTERVIEW_NOTES_MAX = 1000;
export const INTERVIEW_LOCATION_MAX = 255;
export const INTERVIEW_ALLOWED_DURATIONS = [30, 45, 60] as const;

/** Minimum lead time so server `@Future` and clock skew are less likely to reject the slot. */
export const SCHEDULE_MIN_LEAD_MS = 60_000;

export function toDateTimeLocalInputValue(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Parses `datetime-local` value as the user's local wall time (same as `new Date` for ISO-like local strings). */
export function parseDateTimeLocal(value: string): Date | null {
  if (!value?.trim()) return null;
  const d = new Date(value.length === 16 && value.includes('T') ? `${value}:00` : value);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function validateRound1Schedule(input: {
  scheduledAt: string;
  meetingLink: string;
  notes: string;
  duration: number;
}): string | null {
  const when = parseDateTimeLocal(input.scheduledAt);
  if (!when) return 'Please select a valid date and time.';
  if (when.getTime() < Date.now() + SCHEDULE_MIN_LEAD_MS) {
    return 'Interview date and time must be at least one minute in the future.';
  }
  const link = input.meetingLink.trim();
  if (!link) return 'Meeting link is required.';
  if (!INTERVIEW_MEETING_LINK_PATTERN.test(link)) {
    return 'Meeting link must be a valid URL starting with http:// or https://.';
  }
  if (!(INTERVIEW_ALLOWED_DURATIONS as readonly number[]).includes(input.duration)) {
    return 'Duration must be 30, 45, or 60 minutes.';
  }
  if (input.notes.length > INTERVIEW_NOTES_MAX) {
    return `Notes must not exceed ${INTERVIEW_NOTES_MAX} characters.`;
  }
  return null;
}

export function validateRound2Schedule(input: {
  scheduledAt: string;
  location: string;
  notes: string;
  duration: number;
}): string | null {
  const when = parseDateTimeLocal(input.scheduledAt);
  if (!when) return 'Please select a valid date and time.';
  if (when.getTime() < Date.now() + SCHEDULE_MIN_LEAD_MS) {
    return 'Interview date and time must be at least one minute in the future.';
  }
  const loc = input.location.trim();
  if (!loc) return 'Location is required.';
  if (loc.length > INTERVIEW_LOCATION_MAX) {
    return `Location must not exceed ${INTERVIEW_LOCATION_MAX} characters.`;
  }
  if (!(INTERVIEW_ALLOWED_DURATIONS as readonly number[]).includes(input.duration)) {
    return 'Duration must be 30, 45, or 60 minutes.';
  }
  if (input.notes.length > INTERVIEW_NOTES_MAX) {
    return `Notes must not exceed ${INTERVIEW_NOTES_MAX} characters.`;
  }
  return null;
}

/** RTK Query `unwrap()` may throw `FetchBaseQueryError`, `SerializedError`, or a plain `Error`. */
export function getScheduleMutationErrorMessage(error: unknown): string {
  if (typeof error === 'string') return error;
  if (error instanceof Error) return error.message;
  if (error && typeof error === 'object') {
    const o = error as Record<string, unknown>;
    if (typeof o.error === 'string') return o.error;
    if (typeof o.message === 'string' && o.message.length > 0 && o.message !== 'Rejected') {
      return o.message;
    }
    const data = o.data;
    if (typeof data === 'string') return data;
    if (data && typeof data === 'object' && 'message' in data) {
      const m = (data as { message?: unknown }).message;
      if (typeof m === 'string') return m;
    }
  }
  return 'Failed to schedule interview. Please try again.';
}
