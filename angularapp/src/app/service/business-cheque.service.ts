import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import {
  BusinessChequeRequest,
  BusinessChequeRequestAdmin,
  BusinessChequeDrawRequest,
  BusinessChequeManagementStats,
  BusinessChequeApplyResponse,
  BusinessChequeHistoryResponse,
  BusinessChequeApprovalResponse,
  BusinessChequeLeafResponse
} from '../model/cheque/business-cheque.model';

@Injectable({
  providedIn: 'root'
})
export class BusinessChequeService {
  private apiBaseUrl = `${environment.apiBaseUrl}/api/business-cheques`;

  constructor(private http: HttpClient) {}

  // ─── USER OPERATIONS ───────────────────────────────────────────

  applyCheque(currentAccountId: number, request: BusinessChequeDrawRequest): Observable<BusinessChequeApplyResponse> {
    return this.http.post<BusinessChequeApplyResponse>(
      `${this.apiBaseUrl}/draw/apply`,
      {
        currentAccountId,
        ...request
      }
    );
  }

  getUserCheques(currentAccountId: number, page: number = 0, size: number = 20): Observable<BusinessChequeHistoryResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<BusinessChequeHistoryResponse>(
      `${this.apiBaseUrl}/draw/user/${currentAccountId}`,
      { params }
    );
  }

  getChequeDetails(chequeRequestId: number): Observable<BusinessChequeRequest> {
    return this.http.get<BusinessChequeRequest>(
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
   * Edit a pending business cheque request (payeeName and amount only)
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

  getAvailableLeaves(currentAccountId: number): Observable<BusinessChequeLeafResponse> {
    return this.http.get<BusinessChequeLeafResponse>(
      `${this.apiBaseUrl}/draw/leaves/${currentAccountId}`
    );
  }

  // ─── ADMIN OPERATIONS ──────────────────────────────────────────

  getAdminCheques(
    status?: string,
    searchQuery?: string,
    page: number = 0,
    size: number = 20
  ): Observable<{ totalCount: number; items: BusinessChequeRequestAdmin[] }> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    if (searchQuery && searchQuery.trim()) {
      params = params.set('search', searchQuery);
    }

    return this.http.get<{ totalCount: number; items: BusinessChequeRequestAdmin[] }>(
      `${this.apiBaseUrl}/draw/admin/all`,
      { params }
    );
  }

  getAdminChequeDetails(chequeRequestId: number): Observable<BusinessChequeRequestAdmin> {
    return this.http.get<BusinessChequeRequestAdmin>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}`
    );
  }

  verifyPayeeAccount(payeeAccountNumber: string, payeeName: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiBaseUrl}/draw/admin/verify-payee`,
      { payeeAccountNumber, payeeName }
    );
  }

  approveCheque(chequeRequestId: number, remarks?: string, payeeAccountNumber?: string): Observable<BusinessChequeApprovalResponse> {
    return this.http.post<BusinessChequeApprovalResponse>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/approve`,
      { remarks, payeeAccountNumber }
    );
  }

  rejectCheque(chequeRequestId: number, rejectionReason: string): Observable<BusinessChequeApprovalResponse> {
    return this.http.post<BusinessChequeApprovalResponse>(
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

  getChequeStats(): Observable<BusinessChequeManagementStats> {
    return this.http.get<BusinessChequeManagementStats>(
      `${this.apiBaseUrl}/draw/admin/stats`
    );
  }

  getChequeAuditLog(chequeRequestId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiBaseUrl}/draw/admin/${chequeRequestId}/audit-log`
    );
  }

  searchChequeNumber(chequeNumber: string): Observable<BusinessChequeRequestAdmin> {
    const params = new HttpParams().set('chequeNumber', chequeNumber);
    return this.http.get<BusinessChequeRequestAdmin>(
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
