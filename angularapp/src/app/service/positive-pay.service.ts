import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class PositivePayService {
  private readonly base = `${environment.apiBaseUrl}/api/positive-pay`;
  private readonly adminBase = `${environment.apiBaseUrl}/api/admin/positive-pay`;
  constructor(private http: HttpClient) {}
  eligible(account: string): Observable<any[]> { return this.http.get<any[]>(`${this.base}/accounts/${encodeURIComponent(account)}/eligible-cheques`); }
  create(payload: any): Observable<any> { return this.http.post(this.base, payload); }
  history(account: string): Observable<any[]> { return this.http.get<any[]>(`${this.base}/account/${encodeURIComponent(account)}`); }
  details(reference: string): Observable<any> { return this.http.get(`${this.base}/${encodeURIComponent(reference)}`); }
  cancel(reference: string): Observable<any> { return this.http.patch(`${this.base}/${encodeURIComponent(reference)}/cancel`, {}); }
  adminList(status?: string, page = 0, size = 20): Observable<any> { let params = new HttpParams().set('page', page).set('size', size); if (status) params = params.set('status', status); return this.http.get(this.adminBase, { params }); }
  adminStats(): Observable<any> { return this.http.get(`${this.adminBase}/stats`); }
  adminDetails(reference: string): Observable<any> { return this.http.get(`${this.adminBase}/${encodeURIComponent(reference)}`); }
  approve(reference: string, remark = ''): Observable<any> { return this.http.patch(`${this.adminBase}/${encodeURIComponent(reference)}/approve`, { remark }); }
  reject(reference: string, reason: string): Observable<any> { return this.http.patch(`${this.adminBase}/${encodeURIComponent(reference)}/reject`, { reason }); }
}
