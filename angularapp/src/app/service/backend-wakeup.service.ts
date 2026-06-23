import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, retry, timeout } from 'rxjs/operators';
import { timer } from 'rxjs';
import { environment } from '../../environment/environment';

/**
 * Wakes the Render backend before heavy dashboard API calls.
 * Render free tier sleeps after inactivity; the first request often returns 502/503.
 */
@Injectable({ providedIn: 'root' })
export class BackendWakeupService {
  private readonly platformId = inject(PLATFORM_ID);
  private warmupPromise: Promise<boolean> | null = null;

  constructor(private http: HttpClient) {}

  ensureAwake(): Promise<boolean> {
    if (!isPlatformBrowser(this.platformId)) {
      return Promise.resolve(true);
    }
    if (!this.warmupPromise) {
      this.warmupPromise = this.pingWithRetry().finally(() => {
        this.warmupPromise = null;
      });
    }
    return this.warmupPromise;
  }

  ping(): Observable<{ success: boolean; status: string }> {
    return this.http.get<{ success: boolean; status: string }>(`${environment.apiBaseUrl}/api/ping`).pipe(
      timeout(90000),
      catchError(() => of({ success: false, status: 'DOWN' }))
    );
  }

  private pingWithRetry(): Promise<boolean> {
    return new Promise((resolve) => {
      this.http
        .get<{ success: boolean; status: string }>(`${environment.apiBaseUrl}/api/ping`)
        .pipe(
          timeout(90000),
          retry({
            count: 4,
            delay: (_error, retryCount) => timer(3000 * retryCount)
          }),
          catchError(() => of({ success: false, status: 'DOWN' }))
        )
        .subscribe({
          next: (res) => resolve(res.success === true && res.status === 'UP'),
          error: () => resolve(false)
        });
    });
  }
}
