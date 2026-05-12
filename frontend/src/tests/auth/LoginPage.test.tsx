import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { describe, test, expect, beforeEach, vi } from 'vitest';

import LoginPage from '../../features/auth/pages/LoginPage';
import authReducer from '../../features/auth/authSlice';
import { AppThemeProvider } from '../../shared/theme/AppThemeProvider';

delete (window as unknown as { location?: Location }).location;
Object.defineProperty(window, 'location', {
  configurable: true,
  writable: true,
  value: { href: '' },
});

function renderLogin() {
  const store = configureStore({
    reducer: { auth: authReducer },
    middleware: getDefaultMiddleware => getDefaultMiddleware(),
  });
  return render(
    <AppThemeProvider>
      <Provider store={store}>
        <LoginPage />
      </Provider>
    </AppThemeProvider>,
  );
}

function signInButton() {
  return screen.getByRole('button', { name: /^Sign in$/i });
}

describe('LoginPage Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    window.location.href = '';

    vi.stubGlobal(
      'fetch',
      vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
        let body: { usernameOrEmail?: string; password?: string } = {};
        if (init?.body && typeof init.body === 'string') {
          try {
            body = JSON.parse(init.body) as typeof body;
          } catch {
            /* ignore */
          }
        }

        if (body.usernameOrEmail === 'admin@rts.com' && body.password === 'Password@123') {
          return {
            ok: true,
            status: 200,
            json: async () => ({
              success: true,
              message: 'Login successful',
              data: {
                accessToken: 'mock-jwt-token',
                user: {
                  id: 'user-1',
                  username: 'admin',
                  email: 'admin@rts.com',
                  role: 'ADMIN',
                },
              },
            }),
          };
        }

        return {
          ok: false,
          status: 401,
          json: async () => ({
            success: false,
            message: 'Invalid credentials. Please try again.',
          }),
        };
      }),
    );
  });

  test('renders login form correctly', () => {
    renderLogin();

    expect(screen.getByRole('heading', { name: /^Sign in$/i })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('you@company.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    expect(screen.getByText('Remember me')).toBeInTheDocument();
  });

  test('shows error when fields are empty', async () => {
    renderLogin();

    fireEvent.click(signInButton());

    await waitFor(() => {
      expect(screen.getByText('Email or username is required.')).toBeInTheDocument();
    });
  });

  test('shows error when password is missing', async () => {
    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
      target: { value: 'admin@rts.com' },
    });

    fireEvent.click(signInButton());

    await waitFor(() => {
      expect(screen.getByText('Password is required.')).toBeInTheDocument();
    });
  });

  test('successful login with API response', async () => {
    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
      target: { value: 'admin@rts.com' },
    });

    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'Password@123' },
    });

    fireEvent.click(signInButton());

    await waitFor(() => {
      expect(window.location.href).toBe('/dashboard');
    });
  });

  test('invalid login shows error', async () => {
    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
      target: { value: 'wrong@rts.com' },
    });

    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'wrongpass' },
    });

    fireEvent.click(signInButton());

    await waitFor(() => {
      expect(screen.getByText('Invalid credentials. Please try again.')).toBeInTheDocument();
    });
  });

  test('remember me stores token in localStorage', async () => {
    renderLogin();

    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);

    fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
      target: { value: 'admin@rts.com' },
    });

    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'Password@123' },
    });

    fireEvent.click(signInButton());

    await waitFor(() => {
      expect(localStorage.getItem('rts_token')).not.toBeNull();
    });
  });

  test('without remember me uses sessionStorage', async () => {
    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
      target: { value: 'admin@rts.com' },
    });

    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'Password@123' },
    });

    fireEvent.click(signInButton());

    await waitFor(() => {
      expect(sessionStorage.getItem('rts_token')).not.toBeNull();
    });
  });

  test('pressing Enter submits form', async () => {
    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
      target: { value: 'admin@rts.com' },
    });

    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'Password@123' },
    });

    fireEvent.keyDown(screen.getByPlaceholderText('••••••••'), {
      key: 'Enter',
      code: 'Enter',
    });

    await waitFor(() => {
      expect(window.location.href).toBe('/dashboard');
    });
  });

});
