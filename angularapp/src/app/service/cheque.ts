import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cheque, CreateChequeRequest, CancelChequeRequest, DrawChequeRequest, BounceChequeRequest, RequestChequeDraw, ApproveChequeRequest, RejectChequeRequest } from '../model/cheque/cheque-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class ChequeService {
  private apiUrl = `${environment.apiUrl}/cheques`;

  constructor(private http: HttpClient) { }

  // Create cheque leaves
  createChequeLeaves(request: CreateChequeRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/create`, request);
  }

  // Get all cheques for an account
  getChequesByAccountNumber(accountNumber: string): Observable<Cheque[]> {
    return this.http.get<Cheque[]>(`${this.apiUrl}/account/${accountNumber}`);
  }

  // Get cheques by account number with pagination
  getChequesByAccountNumberPaged(accountNumber: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}/page`, { params });
  }

  // Get active cheques for an account
  getActiveCheques(accountNumber: string): Observable<Cheque[]> {
    return this.http.get<Cheque[]>(`${this.apiUrl}/account/${accountNumber}/active`);
  }

  // Get cancelled cheques for an account
  getCancelledCheques(accountNumber: string): Observable<Cheque[]> {
    return this.http.get<Cheque[]>(`${this.apiUrl}/account/${accountNumber}/cancelled`);
  }

  // Get cheque by ID
  getChequeById(id: number): Observable<Cheque> {
    return this.http.get<Cheque>(`${this.apiUrl}/${id}`);
  }

  // Get cheque by cheque number
  getChequeByChequeNumber(chequeNumber: string): Observable<Cheque> {
    return this.http.get<Cheque>(`${this.apiUrl}/number/${chequeNumber}`);
  }

  // Cancel cheque
  cancelCheque(id: number, request: CancelChequeRequest): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/cancel`, request);
  }

  // Download cheque PDF
  downloadCheque(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download`, {
      responseType: 'blob'
    });
  }

  // View cheque PDF
  viewCheque(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/view`, {
      responseType: 'blob'
    });
  }

  // Get all cheques with pagination
  getAllCheques(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}`, { params });
  }

  // Get cheques by status
  getChequesByStatus(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}`, { params });
  }

  // Get cheque statistics for an account
  getChequeStatistics(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}/statistics`);
  }

  // ==================== ADMIN METHODS ====================

  // Draw cheque (withdraw amount) - Admin only
  drawCheque(request: DrawChequeRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/draw`, request);
  }

  // Bounce cheque - Admin only
  bounceCheque(request: BounceChequeRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/bounce`, request);
  }

  // Search cheque by cheque number - Admin only
  searchChequeByNumber(chequeNumber: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('chequeNumber', chequeNumber)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/admin/search`, { params });
  }

  // Get all drawn cheques - Admin only
  getDrawnCheques(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/admin/drawn`, { params });
  }

  // Get all bounced cheques - Admin only
  getBouncedCheques(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/admin/bounced`, { params });
  }

  // Get cheque statistics (all statuses) - Admin only
  getAllChequeStatistics(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/admin/statistics`);
  }

  // ==================== REQUEST METHODS ====================

  // Request cheque drawing - User
  requestChequeDraw(id: number, request: RequestChequeDraw): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/request`, request);
  }

  // Get pending cheque requests - Admin only
  getPendingRequests(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/admin/requests/pending`, { params });
  }

  // Get approved cheque requests - Admin only
  getApprovedRequests(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/admin/requests/approved`, { params });
  }

  // Approve cheque request and draw - Admin only
  approveChequeRequest(id: number, request: ApproveChequeRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/requests/${id}/approve`, request);
  }

  // Reject cheque request - Admin only
  rejectChequeRequest(id: number, request: RejectChequeRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/requests/${id}/reject`, request);
  }

  // Get cheque requests by status - Admin only
  getChequeRequestsByStatus(requestStatus: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (requestStatus === 'REJECTED') {
      return this.http.get<any>(`${this.apiUrl}/admin/requests/rejected`, { params });
    }
    // For other statuses, use the appropriate endpoint
    return this.http.get<any>(`${this.apiUrl}/admin/requests/${requestStatus.toLowerCase()}`, { params });
  }
}

