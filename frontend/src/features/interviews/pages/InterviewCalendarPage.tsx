import React, { useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import { useGetAllInterviewsQuery, useGetInterviewersQuery } from '../interviewApi';
import RescheduleModal from '../components/RescheduleModal';
import CancelModal from '../components/CancelModal';
import type { Interview } from '../interviewTypes';

type CalendarView = 'month' | 'week' | 'day';

const USE_INTERVIEW_MOCK = import.meta.env.VITE_USE_INTERVIEW_MOCK === 'true';

function startOfMonth(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

function startOfWeek(d: Date) {
  const r = new Date(d);
  r.setDate(r.getDate() - r.getDay());
  r.setHours(0, 0, 0, 0);
  return r;
}

function addDays(d: Date, n: number) {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}

function isSameDay(a: Date, b: Date) {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function fmtTime(iso: string) {
  return new Date(iso).toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  });
}

function fmtHeading(date: Date, view: CalendarView) {
  if (view === 'day') {
    return date.toLocaleDateString('en-IN', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }
  if (view === 'week') {
    const s = startOfWeek(date);
    const e = addDays(s, 6);
    return `${s.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })} – ${e.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}`;
  }
  return date.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
}

function buildMonthDays(date: Date): Date[] {
  const first = startOfMonth(date);
  const start = startOfWeek(first);
  return Array.from({ length: 42 }, (_, i) => addDays(start, i));
}

function buildWeekDays(date: Date): Date[] {
  const start = startOfWeek(date);
  return Array.from({ length: 7 }, (_, i) => addDays(start, i));
}

function exportToICal(interviews: Interview[]) {
  const fmt = (d: Date) =>
    d.toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z';
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//RTS//Interview Calendar//EN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
  ];
  interviews.forEach(iv => {
    const s = new Date(iv.scheduledAt);
    const e = new Date(s.getTime() + iv.duration * 60000);
    lines.push(
      'BEGIN:VEVENT',
      `UID:${iv.id}@rts`,
      `DTSTART:${fmt(s)}`,
      `DTEND:${fmt(e)}`,
      `SUMMARY:${iv.round === 'ROUND_1' ? '[R1]' : '[R2]'} ${iv.candidateName}`,
      `DESCRIPTION:${iv.round === 'ROUND_1' ? 'Online Interview' : 'Face-to-Face Interview'}`,
      `LOCATION:${iv.meetingLink ?? iv.location ?? ''}`,
      'END:VEVENT',
    );
  });
  lines.push('END:VCALENDAR');
  const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'rts-interviews.ics';
  a.click();
  URL.revokeObjectURL(url);
  message.success('Calendar exported as .ics file.');
}

function eventClass(iv: Interview) {
  const round = iv.round === 'ROUND_1' ? 'r1' : 'r2';
  const status =
    iv.status === 'CANCELLED'
      ? 'cancelled'
      : iv.status === 'COMPLETED'
        ? 'completed'
        : iv.status === 'RESCHEDULED'
          ? 'rescheduled'
          : '';
  return ['cal-event', `cal-event--${round}`, status ? `cal-event--${status}` : '']
    .filter(Boolean)
    .join(' ');
}

interface EventChipProps {
  iv: Interview;
  onSelect: (iv: Interview) => void;
}

const EventChip: React.FC<EventChipProps> = ({ iv, onSelect }) => (
  <button
    type="button"
    className={eventClass(iv)}
    onClick={e => {
      e.stopPropagation();
      onSelect(iv);
    }}
    title={`${iv.candidateName} — ${fmtTime(iv.scheduledAt)}`}
  >
    <span className="cal-event-dot" aria-hidden />
    <span className="cal-event-label">
      {fmtTime(iv.scheduledAt)} {iv.candidateName}
    </span>
  </button>
);

interface DrawerProps {
  iv: Interview;
  onClose: () => void;
  onReschedule: () => void;
  onCancel: () => void;
}

const DetailDrawer: React.FC<DrawerProps> = ({ iv, onClose, onReschedule, onCancel }) => {
  const canAct = iv.status === 'SCHEDULED' || iv.status === 'RESCHEDULED';
  return (
    <div className="cal-drawer-overlay" onClick={onClose}>
      <div className="cal-drawer" onClick={e => e.stopPropagation()}>
        <div className="cal-drawer-header">
          <div className="cal-drawer-title-row">
            <span
              className={`cal-drawer-round-badge cal-drawer-round-badge--${
                iv.round === 'ROUND_1' ? 'r1' : 'r2'
              }`}
            >
              {iv.round === 'ROUND_1' ? 'Round 1 — Online' : 'Round 2 — Face to Face'}
            </span>
            <button
              type="button"
              className="cal-drawer-close-btn"
              onClick={onClose}
              aria-label="Close"
            >
              ×
            </button>
          </div>
          <h2 className="cal-drawer-name">{iv.candidateName}</h2>
          <p className="cal-drawer-position">{iv.candidatePosition}</p>
        </div>
        <div className="cal-drawer-body">
          <div className="cal-drawer-field">
            <span className="cal-drawer-field-label">Date & Time</span>
            <span className="cal-drawer-field-value">
              {new Date(iv.scheduledAt).toLocaleDateString('en-IN', {
                weekday: 'short',
                day: 'numeric',
                month: 'short',
                year: 'numeric',
              })}
              {' at '}
              {fmtTime(iv.scheduledAt)}
            </span>
          </div>
          <div className="cal-drawer-field">
            <span className="cal-drawer-field-label">Duration</span>
            <span className="cal-drawer-field-value">{iv.duration} minutes</span>
          </div>
          <div className="cal-drawer-field">
            <span className="cal-drawer-field-label">
              {iv.round === 'ROUND_1' ? 'Meeting Link' : 'Location'}
            </span>
            <span className="cal-drawer-field-value">
              {iv.round === 'ROUND_1' && iv.meetingLink ? (
                <a
                  href={iv.meetingLink}
                  target="_blank"
                  rel="noreferrer"
                  className="cal-drawer-link"
                >
                  {iv.meetingLink}
                </a>
              ) : (
                (iv.location ?? '—')
              )}
            </span>
          </div>
          <div className="cal-drawer-field">
            <span className="cal-drawer-field-label">Interviewers</span>
            <span className="cal-drawer-field-value">
              {iv.interviewers.map(i => i.username).join(', ')}
            </span>
          </div>
          {iv.notes && (
            <div className="cal-drawer-field">
              <span className="cal-drawer-field-label">Notes</span>
              <span className="cal-drawer-field-value">{iv.notes}</span>
            </div>
          )}
          <div className="cal-drawer-field">
            <span className="cal-drawer-field-label">Status</span>
            <span className={`cal-status-badge cal-status-badge--${iv.status.toLowerCase()}`}>
              {iv.status}
            </span>
          </div>
        </div>
        {canAct && (
          <div className="cal-drawer-footer">
            <button
              type="button"
              className="si-btn si-btn--ghost cal-drawer-btn"
              onClick={onCancel}
            >
              Cancel Interview
            </button>
            <button
              type="button"
              className="si-btn si-btn--primary cal-drawer-btn"
              onClick={onReschedule}
            >
              Reschedule
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

const InterviewCalendarPage: React.FC = () => {
  const navigate = useNavigate();
  const today = useMemo(() => new Date(), []);
  const [view, setView] = useState<CalendarView>('month');
  const [current, setCurrent] = useState(new Date());
  const [filter, setFilter] = useState('');
  const [selected, setSelected] = useState<Interview | null>(null);
  const [showReschedule, setShowReschedule] = useState(false);
  const [showCancel, setShowCancel] = useState(false);

  const { data: interviewsRaw = [], isLoading } = useGetAllInterviewsQuery(
    USE_INTERVIEW_MOCK ? undefined : filter || undefined,
  );
  const { data: interviewers = [] } = useGetInterviewersQuery();

  const filtered = useMemo(() => {
    if (!USE_INTERVIEW_MOCK || !filter) return interviewsRaw;
    return interviewsRaw.filter(iv =>
      iv.interviewers.some(i => i.username === filter || i.id === filter),
    );
  }, [interviewsRaw, filter]);

  const ivForDay = useCallback(
    (day: Date) => filtered.filter(iv => isSameDay(new Date(iv.scheduledAt), day)),
    [filtered],
  );

  function navigateCal(dir: -1 | 1) {
    setCurrent(prev => {
      const d = new Date(prev);
      if (view === 'month') d.setMonth(d.getMonth() + dir);
      if (view === 'week') d.setDate(d.getDate() + dir * 7);
      if (view === 'day') d.setDate(d.getDate() + dir);
      return d;
    });
  }

  function openDetail(iv: Interview) {
    setSelected(iv);
    setShowReschedule(false);
    setShowCancel(false);
  }

  function closeAll() {
    setSelected(null);
    setShowReschedule(false);
    setShowCancel(false);
  }

  const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const monthDays = useMemo(() => buildMonthDays(current), [current]);
  const weekDays = useMemo(() => buildWeekDays(current), [current]);
  const dayIvs = useMemo(() => ivForDay(current), [ivForDay, current]);

  return (
    <div className="cal-root">
      <div className="cal-page-header">
        <div>
          <p className="cal-eyebrow">Interviews</p>
          <h1 className="cal-page-title">Interview Calendar</h1>
        </div>
        <div className="cal-header-actions">
          <button
            type="button"
            className="si-btn si-btn--ghost"
            onClick={() => exportToICal(filtered)}
            aria-label="Download calendar as iCalendar file"
          >
            Export .ics
          </button>
          <button
            type="button"
            className="si-btn si-btn--primary"
            onClick={() => navigate('/interviews/schedule')}
          >
            + Schedule Interview
          </button>
        </div>
      </div>

      <div className="cal-controls" aria-label="Calendar filters and view">
        <div className="cal-toolbar">
          <div className="cal-nav" role="group" aria-label="Change period">
            <button
              type="button"
              className="cal-nav-btn"
              onClick={() => navigateCal(-1)}
              aria-label="Previous period"
            >
              ‹
            </button>
            <button type="button" className="cal-today-btn" onClick={() => setCurrent(new Date())}>
              Today
            </button>
            <button
              type="button"
              className="cal-nav-btn"
              onClick={() => navigateCal(1)}
              aria-label="Next period"
            >
              ›
            </button>
            <span className="cal-heading">{fmtHeading(current, view)}</span>
          </div>
          <div className="cal-toolbar-right">
            <label className="cal-filter-label" htmlFor="cal-interviewer-filter">
              Interviewer
            </label>
            <select
              id="cal-interviewer-filter"
              className="cal-filter-select"
              value={filter}
              onChange={e => setFilter(e.target.value)}
            >
              <option value="">All interviewers</option>
              {interviewers.map(i => (
                <option key={i.id} value={i.username}>
                  {i.username}
                </option>
              ))}
            </select>
            <div className="cal-view-switcher" role="tablist" aria-label="Calendar view">
              {(['month', 'week', 'day'] as CalendarView[]).map(v => (
                <button
                  key={v}
                  type="button"
                  role="tab"
                  aria-selected={view === v}
                  className={`cal-view-btn${view === v ? ' cal-view-btn--active' : ''}`}
                  onClick={() => setView(v)}
                >
                  {v.charAt(0).toUpperCase() + v.slice(1)}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="cal-legend">
          {[
            { cls: 'r1', label: 'Round 1 — Online' },
            { cls: 'r2', label: 'Round 2 — Face to face' },
            { cls: 'completed', label: 'Completed' },
            { cls: 'cancelled', label: 'Cancelled' },
          ].map(({ cls, label }) => (
            <span key={cls} className="cal-legend-item">
              <span className={`cal-legend-dot cal-legend-dot--${cls}`} aria-hidden />
              {label}
            </span>
          ))}
        </div>
      </div>

      <div className="cal-body">
        {isLoading ? (
          <p className="cal-loading">Loading interviews…</p>
        ) : (
          <>
            {view === 'month' && (
              <div className="cal-month">
                <div className="cal-month-dow-row">
                  {DAY_NAMES.map(d => (
                    <div key={d} className="cal-month-dow">
                      {d}
                    </div>
                  ))}
                </div>
                <div className="cal-month-grid">
                  {monthDays.map((day, i) => {
                    const ivs = ivForDay(day);
                    const otherMonth = day.getMonth() !== current.getMonth();
                    const isTodayCell = isSameDay(day, today);
                    return (
                      <div
                        key={i}
                        className={[
                          'cal-month-cell',
                          otherMonth ? 'cal-month-cell--other' : '',
                          isTodayCell ? 'cal-month-cell--today' : '',
                        ]
                          .filter(Boolean)
                          .join(' ')}
                        onClick={() => {
                          setCurrent(day);
                          setView('day');
                        }}
                      >
                        <span className="cal-month-date-num">{day.getDate()}</span>
                        <div className="cal-month-chips">
                          {ivs.slice(0, 2).map(iv => (
                            <EventChip key={iv.id} iv={iv} onSelect={openDetail} />
                          ))}
                          {ivs.length > 2 && (
                            <span className="cal-month-more">+{ivs.length - 2} more</span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {view === 'week' && (
              <div className="cal-week">
                <div className="cal-week-header">
                  {weekDays.map((day, i) => (
                    <div
                      key={i}
                      className={`cal-week-col-hd${
                        isSameDay(day, today) ? ' cal-week-col-hd--today' : ''
                      }`}
                      onClick={() => {
                        setCurrent(day);
                        setView('day');
                      }}
                    >
                      <span className="cal-week-dow">
                        {day.toLocaleDateString('en-IN', { weekday: 'short' })}
                      </span>
                      <span className="cal-week-date-num">{day.getDate()}</span>
                    </div>
                  ))}
                </div>
                <div className="cal-week-body">
                  {weekDays.map((day, i) => {
                    const ivs = ivForDay(day);
                    return (
                      <div key={i} className="cal-week-col">
                        {ivs.length === 0 ? (
                          <span className="cal-week-empty">–</span>
                        ) : (
                          ivs.map(iv => <EventChip key={iv.id} iv={iv} onSelect={openDetail} />)
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {view === 'day' && (
              <div className="cal-day">
                {dayIvs.length === 0 ? (
                  <div className="cal-day-empty">
                    <p>No interviews scheduled for this day.</p>
                    <button
                      type="button"
                      className="si-btn si-btn--primary"
                      onClick={() => navigate('/interviews/schedule')}
                    >
                      Schedule Interview
                    </button>
                  </div>
                ) : (
                  <div className="cal-day-list">
                    {[...dayIvs]
                      .sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt))
                      .map(iv => (
                        <div
                          key={iv.id}
                          className={`cal-day-card cal-day-card--${
                            iv.round === 'ROUND_1' ? 'r1' : 'r2'
                          }`}
                          onClick={() => openDetail(iv)}
                        >
                          <div className="cal-day-card-time">{fmtTime(iv.scheduledAt)}</div>
                          <div className="cal-day-card-info">
                            <p className="cal-day-card-name">{iv.candidateName}</p>
                            <p className="cal-day-card-meta">
                              {iv.candidatePosition} · {iv.duration} min ·{' '}
                              <span
                                className={`cal-round-pill cal-round-pill--${
                                  iv.round === 'ROUND_1' ? 'r1' : 'r2'
                                }`}
                              >
                                {iv.round === 'ROUND_1' ? 'Round 1' : 'Round 2'}
                              </span>
                            </p>
                            <p className="cal-day-card-interviewers">
                              {iv.interviewers.map(i => i.username).join(', ')}
                            </p>
                          </div>
                          <span
                            className={`cal-status-badge cal-status-badge--${iv.status.toLowerCase()}`}
                          >
                            {iv.status}
                          </span>
                        </div>
                      ))}
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>

      {selected && !showReschedule && !showCancel && (
        <DetailDrawer
          iv={selected}
          onClose={closeAll}
          onReschedule={() => setShowReschedule(true)}
          onCancel={() => setShowCancel(true)}
        />
      )}
      {selected && showReschedule && (
        <RescheduleModal
          interview={selected}
          onClose={() => setShowReschedule(false)}
          onSuccess={closeAll}
        />
      )}
      {selected && showCancel && (
        <CancelModal
          interview={selected}
          onClose={() => setShowCancel(false)}
          onSuccess={closeAll}
        />
      )}
    </div>
  );
};

export default InterviewCalendarPage;
