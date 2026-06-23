import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { UserService } from './service/user';
import { AccountService } from './service/account';
import { TransactionService } from './service/transaction';
import { ssrHttpInterceptor } from './interceptor/ssr-http.interceptor';
import { apiRetryInterceptor } from './interceptor/api-retry.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes), 
    provideClientHydration(withEventReplay()),
    provideHttpClient(withInterceptors([ssrHttpInterceptor, apiRetryInterceptor]), withFetch()),
    UserService,
    AccountService,
    TransactionService
  ]
};
