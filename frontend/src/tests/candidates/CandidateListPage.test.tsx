import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';

import CandidateListPage from '../../features/candidates/pages/CandidateListPage';
import authReducer from '../../features/auth/authSlice';
import { Role } from '../../constants/roles';
function renderPage() {
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
      <MemoryRouter>
        <CandidateListPage />
      </MemoryRouter>
    </Provider>,
  );
}

describe('CandidateListPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('requests one server page with sort and paging query params', async () => {
    const fetchMock = vi.fn<
      (input: RequestInfo | URL, init?: RequestInit) => Promise<{
        ok: boolean;
        json: () => Promise<unknown>;
      }>
    >(async () => ({
      ok: true,
      json: async () => ({
        success: true,
        message: 'ok',
        data: {
          content: [
            {
              id: 'c1',
              name: 'API Person',
              email: 'api@local.test',
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
          ],
          totalElements: 1,
          totalPages: 1,
          page: 0,
          size: 20,
          first: true,
          last: true,
        },
      }),
    }));
    vi.stubGlobal('fetch', fetchMock);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('API Person')).toBeInTheDocument();
    });

    expect(fetchMock).toHaveBeenCalled();
    const url = String(fetchMock.mock.calls[0]![0]);
    expect(url).toContain('/api/candidates');
    expect(url).toContain('page=0');
    expect(url).toContain('size=20');
    expect(url).toContain('sort=createdAt%2Cdesc');
  });
});
