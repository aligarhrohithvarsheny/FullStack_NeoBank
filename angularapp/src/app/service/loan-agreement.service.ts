import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable, NgZone, PLATFORM_ID } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environment/environment';

export interface LoanAgreementEvent {
  agreementId: number;
  loanType: string;
  loanId: number;
  status: string;
  signedAt?: string;
  approvedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class LoanAgreementService {
  private readonly apiUrl = `${environment.apiBaseUrl}/api/loan-agreements`;
  private readonly eventsSubject = new Subject<LoanAgreementEvent>();
  private socket?: WebSocket;
  private reconnectTimer?: ReturnType<typeof setTimeout>;
  private connecting = false;

  readonly events$: Observable<LoanAgreementEvent> = this.eventsSubject.asObservable();

  constructor(
    private http: HttpClient,
    private zone: NgZone,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  getShareDetails(token: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/share/${encodeURIComponent(token)}`);
  }

  getCaptcha(token: string): Observable<{ challengeId: string; image: string; expiresAt: string }> {
    return this.http.get<{ challengeId: string; image: string; expiresAt: string }>(
      `${this.apiUrl}/share/${encodeURIComponent(token)}/captcha`
    );
  }

  getSharePdf(token: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/share/${encodeURIComponent(token)}/pdf`, {
      responseType: 'blob'
    });
  }

  sign(token: string, request: {
    challengeId: string;
    captchaAnswer: string;
    signedName: string;
    consent: boolean;
  }): Observable<any> {
    return this.http.post(`${this.apiUrl}/share/${encodeURIComponent(token)}/sign`, request);
  }

  getAdminAgreements(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl, { headers: this.staffHeaders() });
  }

  renewShareLink(agreementId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${agreementId}/share-link`, {}, {
      headers: this.staffHeaders()
    });
  }

  emailShareLink(agreementId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${agreementId}/email-link`, {}, {
      headers: this.staffHeaders()
    });
  }

  downloadAdminPdf(agreementId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${agreementId}/pdf`, {
      headers: this.staffHeaders(),
      responseType: 'blob'
    });
  }

  staffHeaders(): HttpHeaders {
    const token = isPlatformBrowser(this.platformId)
      ? sessionStorage.getItem('adminSessionToken')
      : null;
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }

  connectToUpdates(): void {
    if (!isPlatformBrowser(this.platformId) || this.connecting
      || (this.socket && this.socket.readyState <= WebSocket.OPEN)) {
      return;
    }

    this.connecting = true;
    const apiUrl = environment.apiBaseUrl.replace(/\/+$/, '');
    const socketUrl = apiUrl.replace(/^https:/, 'wss:').replace(/^http:/, 'ws:') + '/ws-chat';
    try {
      this.socket = new WebSocket(socketUrl);
    } catch (error) {
      this.connecting = false;
      console.error('Could not open loan agreement update socket.', error);
      return;
    }

    this.socket.onopen = () => {
      this.socket?.send('CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\u0000');
    };
    this.socket.onmessage = event => this.handleSocketMessage(String(event.data));
    this.socket.onerror = event => console.error('Loan agreement update socket failed.', event);
    this.socket.onclose = () => {
      this.connecting = false;
      this.socket = undefined;
      this.reconnectTimer = setTimeout(() => this.connectToUpdates(), 5000);
    };
  }

  private handleSocketMessage(frame: string): void {
    if (frame.startsWith('CONNECTED')) {
      this.socket?.send('SUBSCRIBE\nid:loan-agreements\ndestination:/topic/loan-agreements\nack:auto\n\n\u0000');
      return;
    }
    if (!frame.startsWith('MESSAGE')) return;
    const body = frame.split('\n\n').slice(1).join('\n\n').replace(/\u0000$/, '');
    try {
      const event = JSON.parse(body) as LoanAgreementEvent;
      this.zone.run(() => this.eventsSubject.next(event));
    } catch (error) {
      console.error('Received an invalid loan agreement update.', error);
    }
  }
}
