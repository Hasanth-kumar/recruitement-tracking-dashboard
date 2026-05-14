import { configureStore } from '@reduxjs/toolkit';
import { setupListeners } from '@reduxjs/toolkit/query';
import authReducer from '../features/auth/authSlice';
import { authApi } from '../features/auth/authApi';
import { feedbackApi } from '../features/feedback/feedbackApi';
import { interviewApi } from '../features/interviews/interviewApi';

export const store = configureStore({
 reducer: {
   auth: authReducer,
   [authApi.reducerPath]: authApi.reducer,
   [feedbackApi.reducerPath]: feedbackApi.reducer,
   [interviewApi.reducerPath]: interviewApi.reducer,
 },
 middleware: (getDefaultMiddleware) =>
   getDefaultMiddleware()
     .concat(authApi.middleware)
     .concat(feedbackApi.middleware)
     .concat(interviewApi.middleware),
devTools: import.meta.env.DEV,
});

setupListeners(store.dispatch);

export type RootState  = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
