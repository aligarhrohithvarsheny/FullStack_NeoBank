import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class VirtualCardService {
  private apiUrl = `${environment.apiBaseUrl}/api/virtual-cards`;

  constructor(private http: HttpClient) {}

  create(accountNumber: string, cardholderName: string): Observable<any> {
    return this.http.post<any>(this.apiUrl, { accountNumber, cardholderName });
  }

  getByAccountNumber(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}`);
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  freeze(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/freeze`, {});
  }

  unfreeze(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/unfreeze`, {});
  }

  cancel(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/cancel`, {});
  }

  updateLimits(id: number, dailyLimit: number, monthlyLimit: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/limits`, { dailyLimit, monthlyLimit });
  }

  toggleOnline(id: number, enabled: boolean): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/toggle-online`, { enabled });
  }

  toggleInternational(id: number, enabled: boolean): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/toggle-international`, { enabled });
  }

  getAll(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/all`);
  }
}
