import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class ScheduledPaymentService {
  private apiUrl = `${environment.apiBaseUrl}/api/scheduled-payments`;

  constructor(private http: HttpClient) {}

  create(payment: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, payment);
  }

  getByAccountNumber(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}`);
  }

  getActiveByAccountNumber(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}/active`);
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  update(id: number, payment: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, payment);
  }

  pause(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/pause`, {});
  }

  resume(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/resume`, {});
  }

  cancel(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/cancel`, {});
  }

  delete(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  getAll(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/all`);
  }
}
