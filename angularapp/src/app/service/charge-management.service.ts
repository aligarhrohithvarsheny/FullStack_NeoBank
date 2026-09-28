import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class ChargeManagementService {
  private apiUrl = `${environment.apiBaseUrl}/api/charges`;

  constructor(private http: HttpClient) {}

  /**
   * Process a single charge
   * Debit from user account -> Credit to allocation account
   */
  processCharge(chargeData: {
    allocationId: number;
    chargeType: string;  // INTEREST, CIBIL, SOUNDBOX, UPI, PAYMENT_GATEWAY
    chargeDescription: string;
    chargeAmount: number;
    userAccountNumber: string;
    userName: string;
    userProductType: string;
    linkedTransactionId?: string;
    linkedLoanId?: string;
    linkedDepositId?: string;
  }): Observable<any> {
    return this.http.post(`${this.apiUrl}/process`, chargeData);
  }

  /**
   * Process multiple charges in batch
   */
  processChargeBatch(charges: any[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/process-batch`, charges);
  }

  /**
   * Get all charges for an allocation
   */
  getChargesByAllocation(allocationId: number, page: number = 0, size: number = 20): Observable<any> {
    return this.http.get(`${this.apiUrl}/history/${allocationId}`);
  }

  getChargesByType(allocationId: number, chargeType: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/${allocationId}/type/${encodeURIComponent(chargeType)}`);
  }

  getChargeSummary(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/summary/${allocationId}`);
  }
}

