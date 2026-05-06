// jest-dom adds custom matchers for asserting on DOM nodes.
import '@testing-library/jest-dom';

/** Node/Vitest may expose a broken `localStorage` (e.g. invalid `--localstorage-file`). Redux authSlice reads storage at import time. */
function createMemoryStorage(): Storage {
  const dict: Record<string, string> = {};
  return {
    getItem: (key: string) => (Object.prototype.hasOwnProperty.call(dict, key) ? dict[key]! : null),
    setItem: (key: string, value: string) => {
      dict[key] = String(value);
    },
    removeItem: (key: string) => {
      delete dict[key];
    },
    clear: () => {
      for (const k of Object.keys(dict)) delete dict[k];
    },
    key: (index: number) => Object.keys(dict)[index] ?? null,
    get length() {
      return Object.keys(dict).length;
    },
  };
}

Object.defineProperty(globalThis, 'localStorage', {
  value: createMemoryStorage(),
  configurable: true,
  writable: true,
});
Object.defineProperty(globalThis, 'sessionStorage', {
  value: createMemoryStorage(),
  configurable: true,
  writable: true,
});
