import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Provider } from 'react-redux';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';

import CandidateDetailPage from '../../features/candidates/pages/CandidateDetailPage';
import authReducer from '../../features/auth/authSlice';
import { Role } from '../../constants/roles';
function renderDetail() {
  const token = 'mock-jwt-token';
  localStorage.setItem('rts_token', token);

  const store = configureStore({
    reducer: { auth: authReducer },
    middleware: getDefaultMiddleware => getDefaultMiddleware(),
    preloadedState: {
      auth: {
        token,
        user: {
          id: 'u1',
          username: 'recruiter',
          email: 'recruiter@rts.com',
          role: Role.RECRUITER,
        },
        role: Role.RECRUITER,
        isAuthenticated: true,
      },
    },
  });

  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/candidates/c1']}>
        <Routes>
          <Route path="/candidates/:id" element={<CandidateDetailPage />} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );
}

describe('CandidateDetailPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('loads candidate profile and stage history from API', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);
        if (url.includes('stage-history')) {
          return {
            ok: true,
            json: async () => ({
              success: true,
              data: [
                {
                  stage: 'APPLICATION_RECEIVED',
                  changedAt: '2026-05-01T12:00:00',
                  changedBy: 'recruiter',
                },
              ],
            }),
          };
        }
        return {
          ok: true,
          json: async () => ({
            success: true,
            data: {
              id: 'c1',
              name: 'Detail Person',
              email: 'detail@local.test',
              phone: '+12345678901',
              position: 'Engineer',
              stage: 'APPLICATION_RECEIVED',
              experience: '',
              notes: null,
              hasPhoto: false,
              hasResume: false,
              createdAt: '2026-05-01T12:00:00',
              updatedAt: '2026-05-01T12:00:00',
            },
          }),
        };
      }),
    );

    renderDetail();

    await waitFor(() => {
      expect(screen.getByText('Detail Person')).toBeInTheDocument();
    });

    expect(screen.getByText('detail@local.test')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Stage timeline')).toBeInTheDocument();
      expect(screen.getByText(/by recruiter/)).toBeInTheDocument();
    });
  });
});
