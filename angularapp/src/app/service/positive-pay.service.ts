import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class PositivePayService {
  private readonly base = `${environment.apiBaseUrl}/api/positive-pay`;
  private readonly adminBase = `${environment.apiBaseUrl}/api/admin/positive-pay`;
  constructor(private http: HttpClient) {}
  private userHeaders(accountNumber?: string) {
    const keys = ['salaryEmployee', 'currentAccount', 'currentUser', 'userProfile', 'user'];
    let fallback: any = null;
    for (const key of keys) {
      const raw = sessionStorage.getItem(key);
      if (!raw) continue;
      try {
        const value = JSON.parse(raw);
        const valueAccount = value.accountNumber || value.account?.accountNumber;
        if (!fallback && (value.id || value.account?.id)) fallback = value;
        if (accountNumber && valueAccount === accountNumber) return { headers: { 'X-User-Id': String(value.id || value.account?.id) } };
      } catch {}
    }
    return fallback?.id || fallback?.account?.id ? { headers: { 'X-User-Id': String(fallback.id || fallback.account.id) } } : {};
  }
  eligible(account: string): Observable<any[]> { return this.http.get<any[]>(`${this.base}/accounts/${encodeURIComponent(account)}/eligible-cheques`, this.userHeaders(account)); }
  create(payload: any): Observable<any> { return this.http.post(this.base, payload, this.userHeaders(payload.accountNumber)); }
  history(account: string): Observable<any[]> { return this.http.get<any[]>(`${this.base}/account/${encodeURIComponent(account)}`, this.userHeaders(account)); }
  details(reference: string): Observable<any> { return this.http.get(`${this.base}/${encodeURIComponent(reference)}`, this.userHeaders()); }
  cancel(reference: string): Observable<any> { return this.http.patch(`${this.base}/${encodeURIComponent(reference)}/cancel`, {}, this.userHeaders()); }
  adminList(status?: string, page = 0, size = 20): Observable<any> { let params = new HttpParams().set('page', page).set('size', size); if (status) params = params.set('status', status); return this.http.get(this.adminBase, { params }); }
  adminStats(): Observable<any> { return this.http.get(`${this.adminBase}/stats`); }
  adminDetails(reference: string): Observable<any> { return this.http.get(`${this.adminBase}/${encodeURIComponent(reference)}`); }
  approve(reference: string, remark = ''): Observable<any> { return this.http.patch(`${this.adminBase}/${encodeURIComponent(reference)}/approve`, { remark }); }
  reject(reference: string, reason: string): Observable<any> { return this.http.patch(`${this.adminBase}/${encodeURIComponent(reference)}/reject`, { reason }); }
}
