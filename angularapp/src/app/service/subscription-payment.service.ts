import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class SubscriptionPaymentService {
  private apiUrl = `${environment.apiBaseUrl}/api/subscription-payments`;

  constructor(private http: HttpClient) {}

  create(subscription: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, subscription);
  }

  getByEmployeeId(employeeId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/employee/${employeeId}`);
  }

  getByAccount(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}`);
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  update(id: number, subscription: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, subscription);
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
