const RTS_TOKEN_KEY = 'rts_token';

export function readStoredToken(): string {
  if (typeof localStorage === 'undefined') return '';
  return localStorage.getItem(RTS_TOKEN_KEY) ?? sessionStorage.getItem(RTS_TOKEN_KEY) ?? '';
}

export function authorizationBearerHeader(token: string): string {
  return `Bearer ${token}`;
}

export function bearerFetchHeaders(jsonBody: boolean): Record<string, string> {
  const token = readStoredToken();
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = authorizationBearerHeader(token);
  if (jsonBody) headers['Content-Type'] = 'application/json';
  return headers;
}
