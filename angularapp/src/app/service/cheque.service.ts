import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import {
  ChequeRequest,
  ChequeRequestAdmin,
  ChequeDrawRequest,
  ChequeHistoryEntry,
  ChequeAdminAction,
  ChequeManagementStats,
  ChequeApplyResponse,
  ChequeHistoryResponse,
  ChequeAdminResponse,
  ChequeApprovalResponse,
  ChequeLeafResponse
} from '../model/cheque/cheque.model';

@Injectable({
  providedIn: 'root'
})
export class ChequeService {
  private apiBaseUrl = `${environment.apiBaseUrl}/api/cheques`;

  constructor(private http: HttpClient) {}

  // ─── USER OPERATIONS ───────────────────────────────────────────

  /**
   * Apply for a cheque draw request
   */
  applyCheque(salaryAccountId: number, request: ChequeDrawRequest): Observable<ChequeApplyResponse> {
    return this.http.post<ChequeApplyResponse>(
      `${this.apiBaseUrl}/draw/apply`,
      {
        salaryAccountId,
        ...request
      }
    );
  }

  /**
   * Get user's cheque request history
   */
  getUserCheques(salaryAccountId: number, page: number = 0, size: number = 20): Observable<ChequeHistoryResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<ChequeHistoryResponse>(
      `${this.apiBaseUrl}/draw/user/${salaryAccountId}`,
      { params }
    );
  }

  /**
   * Get specific cheque request details
   */
  getChequeDetails(chequeRequestId: number): Observable<ChequeRequest> {
    return this.http.get<ChequeRequest>(
      `${this.apiBaseUrl}/${chequeRequestId}`
    );
  }

  /**
   * Cancel cheque request (user can only cancel pending requests)
   */
  cancelCheque(chequeRequestId: number, reason?: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/${chequeRequestId}/cancel`,
      { reason: reason || '' }
    );
  }

  /**
   * Edit a pending cheque request (payeeName and amount only)
   */
  editPendingCheque(chequeRequestId: number, payeeName: string, amount: number): Observable<any> {
    return this.http.put<any>(
      `${this.apiBaseUrl}/draw/${chequeRequestId}/edit`,
      { payeeName, amount }
    );
  }

  // ─── ADMIN OPERATIONS ──────────────────────────────────────────

  /**
   * Get all cheque requests for admin management
   */
  getAdminCheques(
    status?: string,
    searchQuery?: string,
    page: number = 0,
    size: number = 20
  ): Observable<{ totalCount: number; items: ChequeRequestAdmin[] }> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    
    if (searchQuery && searchQuery.trim()) {
      params = params.set('search', searchQuery);
    }
    
    return this.http.get<{ totalCount: number; items: ChequeRequestAdmin[] }>(
      `${this.apiBaseUrl}/draw/admin/all`,
      { params }
    );
  }

  /**
   * Get cheque request details for admin
   */
  getAdminChequeDetails(chequeRequestId: number): Observable<ChequeRequestAdmin> {
    return this.http.get<ChequeRequestAdmin>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}`
    );
  }

  /**
   * Verify payee account number and name match
   */
  verifyPayeeAccount(payeeAccountNumber: string, payeeName: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/admin/verify-payee`,
      { payeeAccountNumber, payeeName }
    );
  }

  /**
   * Approve cheque request
   */
  approveCheque(chequeRequestId: number, remarks?: string, payeeAccountNumber?: string): Observable<ChequeApprovalResponse> {
    return this.http.post<ChequeApprovalResponse>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/approve`,
      { remarks, payeeAccountNumber }
    );
  }

  /**
   * Reject cheque request
   */
  rejectCheque(chequeRequestId: number, rejectionReason: string): Observable<ChequeApprovalResponse> {
    return this.http.post<ChequeApprovalResponse>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/reject`,
      { rejectionReason }
    );
  }

  /**
   * Mark cheque as picked up by user
   */
  markChequePickedUp(chequeRequestId: number): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/picked-up`,
      {}
    );
  }

  /**
   * Clear cheque (mark as processed)
   */
  clearCheque(chequeRequestId: number, clearedDate?: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/clear`,
      { clearedDate }
    );
  }

  /**
   * Get cheque management statistics
   */
  getChequeStats(): Observable<ChequeManagementStats> {
    return this.http.get<ChequeManagementStats>(
      `${this.apiBaseUrl}/draw/admin/stats`
    );
  }

  /**
   * Get audit log for specific cheque
   */
  getChequeAuditLog(chequeRequestId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/audit-log`
    );
  }

  /**
   * Search cheques by cheque number
   */
  searchChequeNumber(chequeNumber: string): Observable<ChequeRequestAdmin> {
    const params = new HttpParams().set('chequeNumber', chequeNumber);
    return this.http.get<ChequeRequestAdmin>(
      `${this.apiBaseUrl}/admin/search`,
      { params }
    );
  }

  /**
   * Get cheques by status filter
   */
  getChequesByStatus(status: string, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('status', status)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(
      `${this.apiBaseUrl}/admin/by-status`,
      { params }
    );
  }

  // ─── UTILITY METHODS ───────────────────────────────────────────

  /**
   * Validate serial number against cheque book
   */
  validateSerialNumber(salaryAccountId: number, serialNumber: string): Observable<{ valid: boolean; message: string }> {
    const params = new HttpParams().set('serialNumber', serialNumber);
    return this.http.get<{ valid: boolean; message: string }>(
      `${this.apiBaseUrl}/validate-serial/${salaryAccountId}`,
      { params }
    );
  }

  /**
   * Get available balance for account
   */
  getAvailableBalance(salaryAccountId: number): Observable<{ balance: number }> {
    return this.http.get<{ balance: number }>(
      `${this.apiBaseUrl}/balance/${salaryAccountId}`
    );
  }

  /**
   * Get available cheque leaves for a salary account (auto-allocates 30 if none)
   */
  getAvailableLeaves(salaryAccountId: number): Observable<ChequeLeafResponse> {
    return this.http.get<ChequeLeafResponse>(
      `${this.apiBaseUrl}/draw/leaves/${salaryAccountId}`
    );
  }

  /**
   * Mark cheque as downloaded by user
   */
  markChequeDownloaded(chequeRequestId: number): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/${chequeRequestId}/mark-downloaded`,
      {}
    );
  }

  /**
   * Export cheque requests to CSV (admin)
   */
  exportChequesToCSV(filters?: any): Observable<Blob> {
    let params = new HttpParams();
    if (filters?.status) {
      params = params.set('status', filters.status);
    }
    if (filters?.fromDate) {
      params = params.set('fromDate', filters.fromDate);
    }
    if (filters?.toDate) {
      params = params.set('toDate', filters.toDate);
    }
    
    return this.http.get(
      `${this.apiBaseUrl}/admin/export`,
      { params, responseType: 'blob' }
    );
  }
}
