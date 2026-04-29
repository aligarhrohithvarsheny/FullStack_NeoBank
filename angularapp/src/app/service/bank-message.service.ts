import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class BankMessageService {
  private apiUrl = `${environment.apiBaseUrl}/api/bank-messages`;

  constructor(private http: HttpClient) {}

  send(message: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, message);
  }

  sendSystem(accountNumber: string, title: string, content: string, messageType: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/system`, { accountNumber, title, content, messageType });
  }

  getByAccountNumber(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}`);
  }

  getUnread(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}/unread`);
  }

  getUnreadCount(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}/unread-count`);
  }

  markAsRead(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(accountNumber: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/account/${accountNumber}/read-all`, {});
  }

  delete(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  getAll(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/all`);
  }
}
