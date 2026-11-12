import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class AtmUpiWithdrawalService {
  private readonly apiUrl = `${environment.apiBaseUrl}/api/atm/upi-withdrawals`;

  constructor(private http: HttpClient) {}

  createRequest(request: {
    atmId: string;
    accountNumber: string;
    accountType: string;
    amount: number;
  }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/requests`, request);
  }

  getRequest(token: string, accountNumber: string): Observable<any> {
    const params = new HttpParams().set('accountNumber', accountNumber);
    return this.http.get<any>(`${this.apiUrl}/requests/${encodeURIComponent(token)}`, { params });
  }

  confirmRequest(token: string, accountNumber: string, pin: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/requests/${encodeURIComponent(token)}/confirm`, {
      accountNumber,
      pin
    });
  }

  cancelRequest(token: string, accountNumber: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/requests/${encodeURIComponent(token)}/cancel`, {
      accountNumber
    });
  }
}
