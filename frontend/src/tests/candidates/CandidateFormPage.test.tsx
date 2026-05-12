import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { Provider } from 'react-redux';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';

import CandidateFormPage from '../../features/candidates/pages/CandidateFormPage';
import authReducer from '../../features/auth/authSlice';
import { Role } from '../../constants/roles';
function renderNewCandidateForm() {
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
      <MemoryRouter initialEntries={['/candidates/new']}>
        <Routes>
          <Route path="/candidates/new" element={<CandidateFormPage />} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );
}

describe('CandidateFormPage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('shows new candidate registration layout', () => {
    renderNewCandidateForm();
    expect(screen.getByText('New candidate')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Add candidate/i })).toBeInTheDocument();
  });
});
