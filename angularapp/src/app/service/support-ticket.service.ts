import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class SupportTicketService {
  private apiUrl = `${environment.apiBaseUrl}/api/support-tickets`;

  constructor(private http: HttpClient) {}

  create(ticket: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, ticket);
  }

  getByAccountNumber(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}`);
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  getByTicketId(ticketId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/ticket/${ticketId}`);
  }

  getByStatus(status: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/status/${status}`);
  }

  getAll(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/all`);
  }

  updateStatus(id: number, status: string, adminResponse?: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/status`, { status, adminResponse });
  }

  assign(id: number, assignedTo: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/assign`, { assignedTo });
  }

  getStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats`);
  }
}
