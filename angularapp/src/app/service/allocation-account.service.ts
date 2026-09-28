import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class AllocationAccountService {
  private apiUrl = `${environment.apiBaseUrl}/api`;

  constructor(private http: HttpClient) {}

  // ==================== ACCOUNT LINKING (HOD) ====================

  /**
   * Link account to allocation - HOD Dashboard
   * Step 1: Add account details (number, IFSC, holder name, etc.)
   */
  linkAccountToAllocation(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/hod/link-account`, payload);
  }

  /**
   * Add cheque details for verification - HOD Dashboard
   * Step 2: Submit cheque for verification
   */
  addChequeDetails(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/hod/add-cheque-details`, payload);
  }

  /**
   * Get accounts by city and branch - HOD Dashboard
   */
  getAccountsByLocation(city: string, branch: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/hod/accounts`, {
      params: { city, branch }
    });
  }

  /**
   * Get pending verification accounts - HOD Dashboard
   */
  getPendingVerificationAccounts(): Observable<any> {
    return this.http.get(`${this.apiUrl}/hod/pending-verification-accounts`);
  }

  /**
   * Get account details by allocation ID
   */
  getAccountDetails(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/hod/account/${allocationId}`);
  }

  // ==================== ACCOUNT VERIFICATION (ADMIN) ====================

  /**
   * Verify cheque and approve account - Admin Dashboard
   * Final step: Admin verifies and approves the account for use
   */
  verifyChequeAndApprove(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/verify-cheque`, payload);
  }

  /**
   * Reject cheque verification - Admin Dashboard
   */
  rejectChequeVerification(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/reject-cheque`, payload);
  }

  /**
   * Get all pending cheques for verification - Admin Dashboard
   */
  getPendingCheques(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/pending-cheques`);
  }

  /**
   * Get all verified accounts - Admin Dashboard
   */
  getVerifiedAccounts(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/verified-accounts`);
  }

  /**
   * Block/Unblock account - Admin Dashboard
   */
  updateAccountStatus(accountId: number, status: string, reason: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/admin/account-status`, {
      accountId,
      status,
      reason
    });
  }

  // ==================== ACCOUNT VIEW (MANAGER) ====================

  /**
   * Get account details for Manager Dashboard
   */
  getLinkedAccountForAllocation(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/manager/allocation/${allocationId}/account`);
  }

  /**
   * Get account transactions for Manager Dashboard
   */
  getAccountTransactions(allocationId: number, page: number = 0, size: number = 10): Observable<any> {
    return this.http.get(`${this.apiUrl}/manager/allocation/${allocationId}/transactions`, {
      params: { page, size }
    });
  }

  /**
   * Get account balance for Manager Dashboard
   */
  getAccountBalance(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/manager/allocation/${allocationId}/balance`);
  }

  // ==================== CHARGE MANAGEMENT ====================

  /**
   * Process single charge - DEBIT from user, CREDIT to account
   */
  processCharge(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/charges/process`, payload);
  }

  /**
   * Process multiple charges in batch
   */
  processChargeBatch(charges: any[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/charges/process-batch`, charges);
  }

  /**
   * Get charges by allocation ID
   */
  getChargesByAllocation(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/charges/allocation/${allocationId}`);
  }

  /**
   * Get charges by type and allocation
   */
  getChargesByType(allocationId: number, chargeType: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/charges/allocation/${allocationId}/type/${chargeType}`);
  }

  /**
   * Get charge summary for account
   */
  getChargeSummary(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/charges/allocation/${allocationId}/summary`);
  }

  /**
   * Verify IFSC code
   */
  verifyIfsc(ifscCode: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/verify/ifsc/${ifscCode}`);
  }

  /**
   * Verify Account Number with Bank
   */
  verifyAccountNumber(accountNumber: string, ifscCode: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/verify/account`, {
      params: { accountNumber, ifscCode }
    });
  }
}
