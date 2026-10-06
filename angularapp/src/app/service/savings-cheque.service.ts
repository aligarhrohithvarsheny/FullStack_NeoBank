import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import {
  SavingsChequeRequest,
  SavingsChequeRequestAdmin,
  SavingsChequeDrawRequest,
  SavingsChequeManagementStats,
  SavingsChequeApplyResponse,
  SavingsChequeHistoryResponse,
  SavingsChequeApprovalResponse,
  SavingsChequeLeafResponse
} from '../model/cheque/savings-cheque.model';

@Injectable({
  providedIn: 'root'
})
export class SavingsChequeService {
  private apiBaseUrl = `${environment.apiBaseUrl}/api/savings-cheques`;

  constructor(private http: HttpClient) {}

  // ─── USER OPERATIONS ───────────────────────────────────────────

  applyCheque(accountId: number, request: SavingsChequeDrawRequest): Observable<SavingsChequeApplyResponse> {
    return this.http.post<SavingsChequeApplyResponse>(
      `${this.apiBaseUrl}/draw/apply`,
      {
        accountId,
        ...request
      }
    );
  }

  getUserCheques(accountId: number, page: number = 0, size: number = 20): Observable<SavingsChequeHistoryResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<SavingsChequeHistoryResponse>(
      `${this.apiBaseUrl}/draw/user/${accountId}`,
      { params }
    );
  }

  getChequeDetails(chequeRequestId: number): Observable<SavingsChequeRequest> {
    return this.http.get<SavingsChequeRequest>(
      `${this.apiBaseUrl}/draw/${chequeRequestId}`
    );
  }

  cancelCheque(chequeRequestId: number, reason?: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/${chequeRequestId}/cancel`,
      { reason: reason || '' }
    );
  }

  /**
   * Edit a pending savings cheque request (payeeName and amount only)
   */
  editPendingCheque(chequeRequestId: number, payeeName: string, amount: number): Observable<any> {
    return this.http.put<any>(
      `${this.apiBaseUrl}/draw/${chequeRequestId}/edit`,
      { payeeName, amount }
    );
  }

  markChequeDownloaded(chequeRequestId: number): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/${chequeRequestId}/mark-downloaded`,
      {}
    );
  }

  getAvailableLeaves(accountId: number): Observable<SavingsChequeLeafResponse> {
    return this.http.get<SavingsChequeLeafResponse>(
      `${this.apiBaseUrl}/draw/leaves/${accountId}`
    );
  }

  // ─── ADMIN OPERATIONS ──────────────────────────────────────────

  getAdminCheques(
    status?: string,
    searchQuery?: string,
    page: number = 0,
    size: number = 20
  ): Observable<{ totalCount: number; items: SavingsChequeRequestAdmin[] }> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    if (searchQuery && searchQuery.trim()) {
      params = params.set('search', searchQuery);
    }

    return this.http.get<{ totalCount: number; items: SavingsChequeRequestAdmin[] }>(
      `${this.apiBaseUrl}/draw/admin/all`,
      { params }
    );
  }

  getAdminChequeDetails(chequeRequestId: number): Observable<SavingsChequeRequestAdmin> {
    return this.http.get<SavingsChequeRequestAdmin>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}`
    );
  }

  verifyPayeeAccount(payeeAccountNumber: string, payeeName: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/admin/verify-payee`,
      { payeeAccountNumber, payeeName }
    );
  }

  approveCheque(chequeRequestId: number, remarks?: string, payeeAccountNumber?: string): Observable<SavingsChequeApprovalResponse> {
    return this.http.post<SavingsChequeApprovalResponse>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/approve`,
      { remarks, payeeAccountNumber }
    );
  }

  rejectCheque(chequeRequestId: number, rejectionReason: string): Observable<SavingsChequeApprovalResponse> {
    return this.http.post<SavingsChequeApprovalResponse>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/reject`,
      { rejectionReason }
    );
  }

  markChequePickedUp(chequeRequestId: number): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/picked-up`,
      {}
    );
  }

  clearCheque(chequeRequestId: number, clearedDate?: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/clear`,
      { clearedDate }
    );
  }

  getChequeStats(): Observable<SavingsChequeManagementStats> {
    return this.http.get<SavingsChequeManagementStats>(
      `${this.apiBaseUrl}/draw/admin/stats`
    );
  }

  getChequeAuditLog(chequeRequestId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/audit-log`
    );
  }

  searchChequeNumber(chequeNumber: string): Observable<SavingsChequeRequestAdmin> {
    const params = new HttpParams().set('chequeNumber', chequeNumber);
    return this.http.get<SavingsChequeRequestAdmin>(
      `${this.apiBaseUrl}/draw/admin/search`,
      { params }
    );
  }

  exportChequesToCSV(filters?: any): Observable<Blob> {
    let params = new HttpParams();
    if (filters?.status) {
      params = params.set('status', filters.status);
    }

    return this.http.get(
      `${this.apiBaseUrl}/draw/admin/export`,
      { params, responseType: 'blob' }
    );
  }
}
