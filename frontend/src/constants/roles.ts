export enum Role {
    ADMIN       = 'ADMIN',
    HR_MANAGER  = 'HR_MANAGER',
    RECRUITER   = 'RECRUITER',
    INTERVIEWER = 'INTERVIEWER',
   }
   
   export const ROLE_LABELS: Record<Role, string> = {
    [Role.ADMIN]:       'Admin',
    [Role.HR_MANAGER]:  'HR Manager',
    [Role.RECRUITER]:   'Recruiter',
    [Role.INTERVIEWER]: 'Interviewer',
   };
   
   export const ROLE_COLORS: Record<Role, string> = {
    [Role.ADMIN]:       'red',
    [Role.HR_MANAGER]:  'blue',
    [Role.RECRUITER]:   'green',
    [Role.INTERVIEWER]: 'orange',
   };
   
   // ── Permissions ────────────────────────────────────────────────
   
   export type Permission =
    | 'CREATE_CANDIDATE'
    | 'READ_CANDIDATE'
    | 'UPDATE_CANDIDATE'
    | 'DELETE_CANDIDATE'
    | 'BULK_STAGE_UPDATE'
    | 'UPLOAD_RESUME'
    | 'UPLOAD_PHOTO'
    | 'SCHEDULE_INTERVIEW'
    | 'RESCHEDULE_INTERVIEW'
    | 'CANCEL_INTERVIEW'
    | 'VIEW_INTERVIEW_CALENDAR'
    | 'UPLOAD_INTERVIEW_PHOTOS'
    | 'SUBMIT_FEEDBACK'
    | 'VIEW_FEEDBACK'
    | 'VIEW_DASHBOARD'
    | 'VIEW_REPORTS'
    | 'EXPORT_REPORTS'
    | 'MANAGE_USERS'
    | 'VIEW_DELETED_CANDIDATES';
   
   const ALL: Permission[] = [
    'CREATE_CANDIDATE', 'READ_CANDIDATE', 'UPDATE_CANDIDATE', 'DELETE_CANDIDATE',
    'BULK_STAGE_UPDATE', 'UPLOAD_RESUME', 'UPLOAD_PHOTO',
    'SCHEDULE_INTERVIEW', 'RESCHEDULE_INTERVIEW', 'CANCEL_INTERVIEW',
    'VIEW_INTERVIEW_CALENDAR', 'UPLOAD_INTERVIEW_PHOTOS',
    'SUBMIT_FEEDBACK', 'VIEW_FEEDBACK',
    'VIEW_DASHBOARD', 'VIEW_REPORTS', 'EXPORT_REPORTS',
    'MANAGE_USERS', 'VIEW_DELETED_CANDIDATES',
   ];
   
   export const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
    [Role.ADMIN]: ALL,
   
    [Role.HR_MANAGER]: [
      'CREATE_CANDIDATE', 'READ_CANDIDATE', 'UPDATE_CANDIDATE',
      'BULK_STAGE_UPDATE', 'UPLOAD_RESUME', 'UPLOAD_PHOTO',
      'SCHEDULE_INTERVIEW', 'RESCHEDULE_INTERVIEW', 'CANCEL_INTERVIEW',
      'VIEW_INTERVIEW_CALENDAR', 'UPLOAD_INTERVIEW_PHOTOS',
      'SUBMIT_FEEDBACK', 'VIEW_FEEDBACK',
      'VIEW_DASHBOARD', 'VIEW_REPORTS', 'EXPORT_REPORTS',
    ],
   
    [Role.RECRUITER]: [
      'CREATE_CANDIDATE', 'READ_CANDIDATE', 'UPDATE_CANDIDATE',
      'BULK_STAGE_UPDATE', 'UPLOAD_RESUME', 'UPLOAD_PHOTO',
      'SCHEDULE_INTERVIEW', 'RESCHEDULE_INTERVIEW', 'CANCEL_INTERVIEW',
      'VIEW_INTERVIEW_CALENDAR', 'UPLOAD_INTERVIEW_PHOTOS',
      'VIEW_FEEDBACK', 'VIEW_DASHBOARD',
    ],
   
    [Role.INTERVIEWER]: [
      'READ_CANDIDATE',
      'VIEW_INTERVIEW_CALENDAR',
      'SUBMIT_FEEDBACK', 'VIEW_FEEDBACK',
      'VIEW_DASHBOARD',
    ],
   };