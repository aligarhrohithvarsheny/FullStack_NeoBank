import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import {
  AiSecurityDashboard,
  AiSecurityEvent,
  AiSecurityRule,
  AiThreatScore,
  AiDeviceFingerprint,
  PageResponse
} from '../model/ai-security.model';

@Injectable({ providedIn: 'root' })
export class AiSecurityService {

  private baseUrl = `${environment.apiBaseUrl}/api/ai-security`;

  constructor(private http: HttpClient) {}

  /** Get full AI security dashboard data */
  getDashboard(): Observable<AiSecurityDashboard> {
    return this.http.get<AiSecurityDashboard>(`${this.baseUrl}/dashboard`);
  }

  /** Get paginated security events with filters */
  getEvents(filters: {
    eventType?: string;
    channel?: string;
    severity?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<AiSecurityEvent>> {
    let params = new HttpParams();
    if (filters.eventType) params = params.set('eventType', filters.eventType);
    if (filters.channel) params = params.set('channel', filters.channel);
    if (filters.severity) params = params.set('severity', filters.severity);
    if (filters.status) params = params.set('status', filters.status);
    params = params.set('page', (filters.page ?? 0).toString());
    params = params.set('size', (filters.size ?? 20).toString());
    return this.http.get<PageResponse<AiSecurityEvent>>(`${this.baseUrl}/events`, { params });
  }

  /** Update event status */
  updateEventStatus(id: number, status: string, resolvedBy: string, notes: string): Observable<AiSecurityEvent> {
    return this.http.patch<AiSecurityEvent>(`${this.baseUrl}/events/${id}/status`, { status, resolvedBy, notes });
  }

  /** Get threat score for an entity */
  getThreatScore(entityId: string, entityType: string = 'USER'): Observable<AiThreatScore> {
    return this.http.get<AiThreatScore>(`${this.baseUrl}/threat-score/${entityId}`, {
      params: new HttpParams().set('entityType', entityType)
    });
  }

  /** Toggle entity watchlist */
  toggleWatchlist(entityId: string, entityType: string, watchlist: boolean, reason: string): Observable<AiThreatScore> {
    return this.http.post<AiThreatScore>(`${this.baseUrl}/watchlist`, { entityId, entityType, watchlist, reason });
  }

  /** Get device fingerprints */
  getDevices(entityId: string): Observable<AiDeviceFingerprint[]> {
    return this.http.get<AiDeviceFingerprint[]>(`${this.baseUrl}/devices/${entityId}`);
  }

  /** Trust/untrust device */
  setDeviceTrust(deviceId: number, trusted: boolean): Observable<AiDeviceFingerprint> {
    return this.http.patch<AiDeviceFingerprint>(`${this.baseUrl}/devices/${deviceId}/trust`, { trusted });
  }

  /** Get all security rules */
  getRules(): Observable<AiSecurityRule[]> {
    return this.http.get<AiSecurityRule[]>(`${this.baseUrl}/rules`);
  }

  /** Toggle rule */
  toggleRule(id: number, active: boolean): Observable<AiSecurityRule> {
    return this.http.patch<AiSecurityRule>(`${this.baseUrl}/rules/${id}/toggle`, { active });
  }

  /** Create new rule */
  createRule(rule: Partial<AiSecurityRule>): Observable<AiSecurityRule> {
    return this.http.post<AiSecurityRule>(`${this.baseUrl}/rules`, rule);
  }

  /** Analyze login (for real-time analysis) */
  analyzeLogin(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/analyze/login`, data);
  }

  /** Analyze transaction */
  analyzeTransaction(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/analyze/transaction`, data);
  }

  /** Analyze behavior */
  analyzeBehavior(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/analyze/behavior`, data);
  }
}
