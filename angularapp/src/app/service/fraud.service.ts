import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environment/environment';

/**
 * Fraud detection: submit behavioral biometrics (typing/mouse) and phishing scan.
 */
@Injectable({ providedIn: 'root' })
export class FraudService {

  constructor(private http: HttpClient) {}

  /** Submit behavioral metrics (typing speed, mouse movement) for anomaly detection. Fire-and-forget. */
  submitBehavioral(email: string, typingSpeedWpm?: number, mouseMovementScore?: number): void {
    this.http.post<{ success: boolean; anomalyDetected?: boolean }>(
      `${environment.apiBaseUrl}/api/fraud-alerts/behavioral`,
      { email, typingSpeedWpm, mouseMovementScore, deviceInfo: typeof navigator !== 'undefined' ? navigator.userAgent : undefined }
    ).subscribe({ next: () => {}, error: () => {} });
  }

  /** Scan text for phishing indicators. Returns { suspicious, reasons }. */
  scanPhishing(text: string, sourceId?: string) {
    return this.http.post<{ success: boolean; suspicious: boolean; reasons?: string }>(
      `${environment.apiBaseUrl}/api/fraud-alerts/scan-phishing`,
      { text, sourceId }
    );
  }
}
