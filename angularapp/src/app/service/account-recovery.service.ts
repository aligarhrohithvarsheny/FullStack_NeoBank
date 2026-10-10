import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

export type RecoveryAccountType = 'Savings' | 'Current' | 'Salary';

export interface AccountRecoveryIdentity {
  accountType: RecoveryAccountType;
  customerId: string;
  accountNumber: string;
  dob: string;
}

@Injectable({
  providedIn: 'root'
})
export class AccountRecoveryService {
  private readonly apiUrl = `${environment.apiBaseUrl}/api/account-recovery`;

  constructor(private http: HttpClient) {}

  verify(identity: AccountRecoveryIdentity): Observable<{ success: boolean; maskedName: string }> {
    return this.http.post<{ success: boolean; maskedName: string }>(`${this.apiUrl}/verify`, identity);
  }

  resetPassword(identity: AccountRecoveryIdentity, newPassword: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.apiUrl}/reset-password`, {
      ...identity,
      newPassword
    });
  }
}
