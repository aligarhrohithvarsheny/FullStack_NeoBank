import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { throwError, timer } from 'rxjs';
import { retry } from 'rxjs/operators';

const RETRYABLE_STATUSES = new Set([0, 502, 503, 504]);
const MAX_RETRIES = 3;
const BASE_DELAY_MS = 2500;

function isRetriable(error: unknown): boolean {
  if (!error) {
    return false;
  }
  if (error instanceof HttpErrorResponse) {
    return RETRYABLE_STATUSES.has(error.status);
  }
  const named = error as { name?: string; status?: number };
  if (named.name === 'TimeoutError') {
    return true;
  }
  return RETRYABLE_STATUSES.has(named.status ?? -1);
}

/**
 * Retries idempotent GET/HEAD requests when the Render backend is waking up
 * or the database pool is not ready yet (502/503/504, network errors, timeouts).
 */
export const apiRetryInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) {
    return next(req);
  }

  const method = req.method.toUpperCase();
  if (method !== 'GET' && method !== 'HEAD') {
    return next(req);
  }

  // Health ping has its own retry logic in BackendWakeupService.
  if (req.url.includes('/api/ping')) {
    return next(req);
  }

  return next(req).pipe(
    retry({
      count: MAX_RETRIES,
      delay: (error, retryCount) => {
        if (!isRetriable(error) || retryCount > MAX_RETRIES) {
          return throwError(() => error);
        }
        const delayMs = BASE_DELAY_MS * retryCount;
        console.warn(
          `[API Retry] ${method} ${req.url} failed (attempt ${retryCount}/${MAX_RETRIES}). Retrying in ${delayMs}ms...`
        );
        return timer(delayMs);
      }
    })
  );
};
