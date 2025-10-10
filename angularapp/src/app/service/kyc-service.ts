import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KycRequest as KycModel, CreateKycRequest, UpdateKycRequest, ApproveKycRequest, RejectKycRequest, KycSearchRequest, KycStatistics } from '../model/kyc-request/kyc-request-module';

@Injectable({
  providedIn: 'root'
})
export class KycService {
  private apiUrl = 'http://localhost:8080/api/kyc';

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createKycRequest(kyc: CreateKycRequest): Observable<KycModel> {
    return this.http.post<KycModel>(`${this.apiUrl}`, kyc);
  }

  getKycById(id: number): Observable<KycModel> {
    return this.http.get<KycModel>(`${this.apiUrl}/${id}`);
  }

  updateKyc(id: number, kycDetails: UpdateKycRequest): Observable<KycModel> {
    return this.http.put<KycModel>(`${this.apiUrl}/${id}`, kycDetails);
  }

  deleteKyc(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // User-specific operations
  getKycByUserId(userId: string): Observable<KycModel> {
    return this.http.get<KycModel>(`${this.apiUrl}/user/${userId}`);
  }

  getKycByUserAccountNumber(userAccountNumber: string): Observable<KycModel> {
    return this.http.get<KycModel>(`${this.apiUrl}/account/${userAccountNumber}`);
  }

  getKycByPanNumber(panNumber: string): Observable<KycModel> {
    return this.http.get<KycModel>(`${this.apiUrl}/pan/${panNumber}`);
  }

  // Status-based operations
  getKycByStatus(status: string): Observable<KycModel[]> {
    return this.http.get<KycModel[]>(`${this.apiUrl}/status/${status}`);
  }

  getKycByStatusWithPagination(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}/paginated`, { params });
  }

  // Date range operations
  getKycBySubmittedDateRange(startDate: string, endDate: string): Observable<KycModel[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get<KycModel[]>(`${this.apiUrl}/submitted-date-range`, { params });
  }

  getKycByApprovedDateRange(startDate: string, endDate: string): Observable<KycModel[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get<KycModel[]>(`${this.apiUrl}/approved-date-range`, { params });
  }

  // Search operations
  searchKycRequests(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Admin operations
  approveKyc(approveRequest: ApproveKycRequest): Observable<KycModel> {
    return this.http.put<KycModel>(`${this.apiUrl}/${approveRequest.kycId}/approve`, approveRequest);
  }

  rejectKyc(rejectRequest: RejectKycRequest): Observable<KycModel> {
    return this.http.put<KycModel>(`${this.apiUrl}/${rejectRequest.kycId}/reject`, rejectRequest);
  }

  getKycByApprovedBy(adminName: string): Observable<KycModel[]> {
    return this.http.get<KycModel[]>(`${this.apiUrl}/approved-by/${adminName}`);
  }

  getPendingKycForReview(): Observable<KycModel[]> {
    return this.http.get<KycModel[]>(`${this.apiUrl}/pending-review`);
  }

  // Statistics operations
  getKycStatistics(): Observable<KycStatistics> {
    return this.http.get<KycStatistics>(`${this.apiUrl}/stats`);
  }

  getRecentKycRequests(limit: number = 5): Observable<KycModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<KycModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Advanced search with multiple filters
  searchKycAdvanced(searchRequest: KycSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.userId) params = params.set('userId', searchRequest.userId);
    if (searchRequest.userAccountNumber) params = params.set('userAccountNumber', searchRequest.userAccountNumber);
    if (searchRequest.userEmail) params = params.set('userEmail', searchRequest.userEmail);
    if (searchRequest.userName) params = params.set('userName', searchRequest.userName);
    if (searchRequest.panNumber) params = params.set('panNumber', searchRequest.panNumber);
    if (searchRequest.submittedFrom) params = params.set('submittedFrom', searchRequest.submittedFrom);
    if (searchRequest.submittedTo) params = params.set('submittedTo', searchRequest.submittedTo);
    if (searchRequest.approvedFrom) params = params.set('approvedFrom', searchRequest.approvedFrom);
    if (searchRequest.approvedTo) params = params.set('approvedTo', searchRequest.approvedTo);
    if (searchRequest.approvedBy) params = params.set('approvedBy', searchRequest.approvedBy);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Export KYC requests
  exportKycRequests(status?: string, startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    
    return this.http.get(`${this.apiUrl}/export`, { 
      params, 
      responseType: 'blob' 
    });
  }
}
