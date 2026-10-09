import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const insuranceAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  if (!isPlatformBrowser(inject(PLATFORM_ID))
      || !request.url.includes('/api/insurance/')
      || request.url.includes('/api/insurance/authenticate')) {
    return next(request);
  }

  const token = sessionStorage.getItem('insuranceAuthToken');
  if (!token) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  })).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        sessionStorage.removeItem('insuranceUser');
        sessionStorage.removeItem('insuranceGuest');
        sessionStorage.removeItem('insuranceAuthToken');
        void router.navigate(['/website/insurance-login']);
      }
      return throwError(() => error);
    })
  );
};
