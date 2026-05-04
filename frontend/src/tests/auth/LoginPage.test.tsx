import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LoginPage from '../../features/auth/pages/LoginPage';
import '@testing-library/jest-dom';

// Mock window.location
delete (window as any).location;
window.location = { href: '' } as any;

describe('LoginPage Tests', () => {

 test('renders login form correctly', () => {
   render(<LoginPage />);

   expect(screen.getByText('Sign in')).toBeInTheDocument();
   expect(screen.getByPlaceholderText('you@company.com')).toBeInTheDocument();
   expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
   expect(screen.getByText('Remember me')).toBeInTheDocument();
 });

 test('shows error when fields are empty', async () => {
   render(<LoginPage />);

   fireEvent.click(screen.getByText('Sign in'));

   await waitFor(() => {
     expect(screen.getByText('Email or username is required.')).toBeInTheDocument();
   });
 });

 test('shows error when password is missing', async () => {
   render(<LoginPage />);

   fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
     target: { value: 'admin@rts.com' },
   });

   fireEvent.click(screen.getByText('Sign in'));

   await waitFor(() => {
     expect(screen.getByText('Password is required.')).toBeInTheDocument();
   });
 });

 test('successful login with mock data', async () => {
   render(<LoginPage />);

   fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
     target: { value: 'admin@rts.com' },
   });

   fireEvent.change(screen.getByPlaceholderText('••••••••'), {
     target: { value: 'Password@123' },
   });

   fireEvent.click(screen.getByText('Sign in'));

   await waitFor(() => {
     expect(window.location.href).toBe('/dashboard');
   });
 });

 test('invalid login shows error', async () => {
   render(<LoginPage />);

   fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
     target: { value: 'wrong@rts.com' },
   });

   fireEvent.change(screen.getByPlaceholderText('••••••••'), {
     target: { value: 'wrongpass' },
   });

   fireEvent.click(screen.getByText('Sign in'));

   await waitFor(() => {
     expect(
       screen.getByText('Invalid credentials. Please try again.')
     ).toBeInTheDocument();
   });
 });

 test('remember me stores token in localStorage', async () => {
   render(<LoginPage />);

   const checkbox = screen.getByRole('checkbox');
   fireEvent.click(checkbox);

   fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
     target: { value: 'admin@rts.com' },
   });

   fireEvent.change(screen.getByPlaceholderText('••••••••'), {
     target: { value: 'Password@123' },
   });

   fireEvent.click(screen.getByText('Sign in'));

   await waitFor(() => {
     expect(localStorage.getItem('rts_token')).not.toBeNull();
   });
 });

 test('without remember me uses sessionStorage', async () => {
   render(<LoginPage />);

   fireEvent.change(screen.getByPlaceholderText('you@company.com'), {
     target: { value: 'admin@rts.com' },
   });

   fireEvent.change(screen.getByPlaceholderText('••••••••'), {
     target: { value: 'Password@123' },
   });

   fireEvent.click(screen.getByText('Sign in'));

   await waitFor(() => {
     expect(sessionStorage.getItem('rts_token')).not.toBeNull();
   });
 });

 test('pressing Enter submits form', async () => {
   render(<LoginPage />);

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

 test('preset buttons autofill fields', () => {
   render(<LoginPage />);

   const adminBtn = screen.getByText('Admin');
   fireEvent.click(adminBtn);

   expect(screen.getByPlaceholderText('you@company.com')).toHaveValue('admin@rts.com');
   expect(screen.getByPlaceholderText('••••••••')).toHaveValue('Password@123');
 });

});


