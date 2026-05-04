/** Same key as auth slice: persisted base64(`usernameOrEmail:password`). */
const RTS_BASIC_SECRET_KEY = 'rts_token';

/** Base64(`usernameOrEmail:password`) for HTTP Basic auth (backend accepts username or email). */
export function encodeBasicAuth(usernameOrEmail: string, password: string): string {
  const pair = `${usernameOrEmail}:${password}`;
  const bytes = new TextEncoder().encode(pair);
  let bin = '';
  for (let i = 0; i < bytes.length; i += 1) {
    bin += String.fromCharCode(bytes[i]!);
  }
  return btoa(bin);
}

export function authorizationBasicHeader(credentialsBase64: string): string {
  return `Basic ${credentialsBase64}`;
}

/** Reads persisted Basic secret from localStorage or sessionStorage. */
export function readStoredBasicSecret(): string {
  if (typeof localStorage === 'undefined') return '';
  return localStorage.getItem(RTS_BASIC_SECRET_KEY) ?? sessionStorage.getItem(RTS_BASIC_SECRET_KEY) ?? '';
}

/** Headers object for native `fetch` calls to the RTS API. */
export function basicAuthFetchHeaders(jsonBody: boolean): Record<string, string> {
  const secret = readStoredBasicSecret();
  const headers: Record<string, string> = {};
  if (secret) headers.Authorization = authorizationBasicHeader(secret);
  if (jsonBody) headers['Content-Type'] = 'application/json';
  return headers;
}
