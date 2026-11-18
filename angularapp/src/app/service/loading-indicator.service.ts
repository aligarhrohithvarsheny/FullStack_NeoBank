import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingIndicatorService {
  readonly isVisible = signal(false);
  readonly isUploadActive = signal(false);

  private activeRequests = 0;
  private activeUploads = 0;
  private visibleSince = 0;
  private showTimer: ReturnType<typeof setTimeout> | null = null;
  private hideTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly showDelayMs = 250;
  private readonly minimumVisibleMs = 400;

  start(isUpload = false): void {
    this.activeRequests += 1;
    if (isUpload) {
      this.activeUploads += 1;
      this.isUploadActive.set(true);
    }

    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
      this.hideTimer = null;
    }
    if (this.isVisible() || this.showTimer) return;

    this.showTimer = setTimeout(() => {
      this.showTimer = null;
      if (this.activeRequests > 0) {
        this.visibleSince = Date.now();
        this.isVisible.set(true);
      }
    }, this.showDelayMs);
  }

  finish(isUpload = false): void {
    this.activeRequests = Math.max(0, this.activeRequests - 1);
    if (isUpload) {
      this.activeUploads = Math.max(0, this.activeUploads - 1);
      this.isUploadActive.set(this.activeUploads > 0);
    }

    if (this.activeRequests > 0) return;
    if (this.showTimer) {
      clearTimeout(this.showTimer);
      this.showTimer = null;
    }
    if (!this.isVisible() || this.hideTimer) return;

    const remaining = this.minimumVisibleMs - (Date.now() - this.visibleSince);
    if (remaining > 0) {
      this.hideTimer = setTimeout(() => this.hideIfIdle(), remaining);
    } else {
      this.hideIfIdle();
    }
  }

  private hideIfIdle(): void {
    this.hideTimer = null;
    if (this.activeRequests === 0) {
      this.isVisible.set(false);
      this.visibleSince = 0;
    }
  }
}
