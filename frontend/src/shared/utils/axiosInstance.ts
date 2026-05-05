import axios, {
    type AxiosError,
    type AxiosResponse,
    type InternalAxiosRequestConfig,
   } from 'axios';
   import { authorizationBasicHeader } from './basicAuth';

   const TOKEN_KEY = 'rts_token';
   const BASE_URL  = 'http://localhost:8080';

   function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY);
   }

   function clearCredentials() {
    [localStorage, sessionStorage].forEach(s => {
      ['rts_token', 'rts_role', 'rts_user', 'rts_basic_principal'].forEach(k => s.removeItem(k));
    });
   }
   
   const axiosInstance = axios.create({
    baseURL: `${BASE_URL}/api`,
    timeout: 15_000,
    headers: { 'Content-Type': 'application/json' },
   });
   
   // ── Request — HTTP Basic (backend is stateless; no JWT from login)
   axiosInstance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = getToken();
      if (token && config.headers) {
        config.headers['Authorization'] = authorizationBasicHeader(token);
      }
      return config;
    },
    (error) => Promise.reject(error)
   );
   
   // ── Response — handle errors ───────────────────────────────────
   axiosInstance.interceptors.response.use(
    (response: AxiosResponse) => {
      // Unwrap ApiResponse { success, message, data }
      if (response.data && 'success' in response.data && !response.data.success) {
        return Promise.reject(new Error(response.data.message ?? 'Request failed.'));
      }
      return response;
    },
    (error: AxiosError<{ success: boolean; message: string }>) => {
      const status  = error.response?.status;
      const message = error.response?.data?.message;
   
      if (status === 401) {
        clearCredentials();
        window.dispatchEvent(new CustomEvent('rts:session-expired'));
        window.location.replace('/login');
      }
      if (status === 403) {
        window.location.replace('/403');
      }
   
      return Promise.reject(new Error(message ?? error.message ?? 'Unexpected error.'));
    }
   );
   
   export default axiosInstance;
