import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class DemandDraftService {
  private api = `${environment.apiBaseUrl}/api/demand-drafts`;
  constructor(private http: HttpClient) {}
  verifyCheque(accountNumber: string, chequeNumber: string) { return this.http.get<any>(`${this.api}/verify-cheque?accountNumber=${encodeURIComponent(accountNumber)}&chequeNumber=${encodeURIComponent(chequeNumber)}`); }
  getByAccount(accountNumber: string) { return this.http.get<any[]>(`${this.api}/account/${encodeURIComponent(accountNumber)}`); }
  create(accountNumber: string, draft: any) { return this.http.post<any>(`${this.api}/account/${encodeURIComponent(accountNumber)}`, draft); }
  getAll() { return this.http.get<any[]>(`${this.api}/admin/all`); }
  update(id: number, draft: any, adminName: string) { return this.http.put<any>(`${this.api}/admin/${id}`, { ...draft, adminName }); }
  approve(id: number, adminName: string) { return this.http.post<any>(`${this.api}/admin/${id}/approve`, { adminName }); }
  reject(id: number, adminName: string, reason: string) { return this.http.post<any>(`${this.api}/admin/${id}/reject`, { adminName, reason }); }
  download(id: number) { return this.http.get(`${this.api}/${id}/download`, { responseType: 'blob' }); }
}
