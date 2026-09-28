import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class ChargeManagementService {
  private apiUrl = `${environment.apiUrl}/api/charges`;

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
    return this.http.get(`${this.apiUrl}/allocation/${allocationId}`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  /**
   * Get charges by type
   */
  getChargesByType(allocationId: number, chargeType: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/allocation/${allocationId}/type/${chargeType}`);
  }

  /**
   * Get charge summary (totals by charge type)
   */
  getChargeSummary(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/allocation/${allocationId}/summary`);
  }

  /**
   * Get daily charge report
   */
  getDailyChargeReport(allocationId: number, startDate: string, endDate: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/allocation/${allocationId}/daily-report`, {
      params: { startDate, endDate }
    });
  }

  /**
   * Get charges for specific user
   */
  getChargesForUser(userAccountNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/user/${userAccountNumber}`);
  }

  /**
   * Export charges report
   */
  exportChargesReport(allocationId: number, format: string = 'csv'): Observable<any> {
    return this.http.get(`${this.apiUrl}/allocation/${allocationId}/export`, {
      params: { format }
    });
  }

  /**
   * Get charge reconciliation status
   */
  getReconciliationStatus(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/allocation/${allocationId}/reconciliation`);
  }

  /**
   * Reverse a charge (if needed)
   */
  reverseCharge(chargeTransactionId: string, reason: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/reverse`, {
      chargeTransactionId,
      reason
    });
  }
}
