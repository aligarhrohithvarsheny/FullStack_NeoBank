import { HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { EMPTY } from 'rxjs';

export const ssrHttpInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);
  
  // During SSR/prerendering, prevent HTTP requests that would fail
  // Return an empty observable that completes immediately without emitting values
  if (!isPlatformBrowser(platformId)) {
    // During SSR, we can't make HTTP requests to external APIs
    // Return EMPTY to prevent errors and allow the component to render
    console.log(`[SSR] Skipping HTTP request: ${req.method} ${req.url}`);
    return EMPTY;
  }
  
  // In the browser, proceed with the request normally
  return next(req);
};

