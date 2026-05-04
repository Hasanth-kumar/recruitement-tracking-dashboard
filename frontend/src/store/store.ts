import { configureStore } from '@reduxjs/toolkit';
import { setupListeners } from '@reduxjs/toolkit/query';
import authReducer from '../features/auth/authSlice';
import { authApi } from '../features/auth/authApi';

export const store = configureStore({
 reducer: {
   auth: authReducer,
   [authApi.reducerPath]: authApi.reducer,
 },
 middleware: (getDefaultMiddleware) =>
   getDefaultMiddleware().concat(authApi.middleware),
//  devTools: import.meta.env.DEV,
devTools : process.env.NODE_ENV ==='development',
});

setupListeners(store.dispatch);

export type RootState  = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
