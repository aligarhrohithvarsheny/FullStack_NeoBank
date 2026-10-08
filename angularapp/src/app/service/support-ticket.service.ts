import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class SupportTicketService {
  private apiUrl = `${environment.apiBaseUrl}/api/support-tickets`;

  constructor(private http: HttpClient) {}

  private getAuthOptions(): { headers: HttpHeaders } {
    let authToken = '';
    if (typeof sessionStorage !== 'undefined') {
      try {
        authToken = JSON.parse(sessionStorage.getItem('currentUser') || '{}').authToken || '';
      } catch {
        authToken = '';
      }
    }
    return { headers: new HttpHeaders(authToken ? { Authorization: `Bearer ${authToken}` } : {}) };
  }

  create(ticket: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, ticket, this.getAuthOptions());
  }

  verifyTransaction(transactionId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify-transaction?transactionId=${encodeURIComponent(transactionId)}`, this.getAuthOptions());
  }

  getMine(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/mine`, this.getAuthOptions());
  }

  getByAccountNumber(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${encodeURIComponent(accountNumber)}`, this.getAuthOptions());
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`, this.getAuthOptions());
  }

  getByTicketId(ticketId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/ticket/${encodeURIComponent(ticketId)}`, this.getAuthOptions());
  }

  getByStatus(status: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/status/${encodeURIComponent(status)}`, this.getAuthOptions());
  }

  getAll(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/all`, this.getAuthOptions());
  }

  updateStatus(id: number, status: string, adminResponse?: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/status`, { status, adminResponse }, this.getAuthOptions());
  }

  assign(id: number, assignedTo: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/assign`, { assignedTo }, this.getAuthOptions());
  }

  getStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats`, this.getAuthOptions());
  }
}
