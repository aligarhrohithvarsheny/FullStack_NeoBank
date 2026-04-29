import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CardReplacementRequest as CardReplacementModel, CreateCardReplacementRequest, UpdateCardReplacementRequest, ApproveCardReplacementRequest, RejectCardReplacementRequest, CardReplacementSearchRequest, CardReplacementStatistics } from '../model/card-replacement-request/card-replacement-request-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class CardReplacementRequestService {
  private apiUrl = `${environment.apiBaseUrl}/api/card-replacement-requests`;

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createCardReplacementRequest(request: CreateCardReplacementRequest): Observable<CardReplacementModel> {
    return this.http.post<CardReplacementModel>(`${this.apiUrl}`, request);
  }

  getCardReplacementRequestById(id: number): Observable<CardReplacementModel> {
    return this.http.get<CardReplacementModel>(`${this.apiUrl}/${id}`);
  }

  updateCardReplacementRequest(id: number, requestDetails: UpdateCardReplacementRequest): Observable<CardReplacementModel> {
    return this.http.put<CardReplacementModel>(`${this.apiUrl}/${id}`, requestDetails);
  }

  deleteCardReplacementRequest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // User-specific operations
  getCardReplacementRequestsByUserId(userId: string): Observable<CardReplacementModel[]> {
    return this.http.get<CardReplacementModel[]>(`${this.apiUrl}/user/${userId}`);
  }

  getCardReplacementRequestsByAccountNumber(accountNumber: string): Observable<CardReplacementModel[]> {
    return this.http.get<CardReplacementModel[]>(`${this.apiUrl}/account/${accountNumber}`);
  }

  // Status-based operations
  getCardReplacementRequestsByStatus(status: string): Observable<CardReplacementModel[]> {
    return this.http.get<CardReplacementModel[]>(`${this.apiUrl}/status/${status}`);
  }

  getCardReplacementRequestsByStatusWithPagination(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}/paginated`, { params });
  }

  // Admin operations
  approveCardReplacementRequest(approveRequest: ApproveCardReplacementRequest): Observable<CardReplacementModel> {
    return this.http.put<CardReplacementModel>(`${this.apiUrl}/${approveRequest.requestId}/approve`, approveRequest);
  }

  rejectCardReplacementRequest(rejectRequest: RejectCardReplacementRequest): Observable<CardReplacementModel> {
    return this.http.put<CardReplacementModel>(`${this.apiUrl}/${rejectRequest.requestId}/reject`, rejectRequest);
  }

  getCardReplacementRequestsByProcessedBy(adminName: string): Observable<CardReplacementModel[]> {
    return this.http.get<CardReplacementModel[]>(`${this.apiUrl}/processed-by/${adminName}`);
  }

  getPendingCardReplacementRequestsForReview(): Observable<CardReplacementModel[]> {
    return this.http.get<CardReplacementModel[]>(`${this.apiUrl}/pending-review`);
  }

  // Statistics operations
  getCardReplacementRequestStatistics(): Observable<CardReplacementStatistics> {
    return this.http.get<CardReplacementStatistics>(`${this.apiUrl}/stats`);
  }

  getRecentCardReplacementRequests(limit: number = 5): Observable<CardReplacementModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<CardReplacementModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Advanced search with multiple filters
  searchCardReplacementRequestsAdvanced(searchRequest: CardReplacementSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.userId) params = params.set('userId', searchRequest.userId);
    if (searchRequest.userName) params = params.set('userName', searchRequest.userName);
    if (searchRequest.userEmail) params = params.set('userEmail', searchRequest.userEmail);
    if (searchRequest.accountNumber) params = params.set('accountNumber', searchRequest.accountNumber);
    if (searchRequest.currentCardNumber) params = params.set('currentCardNumber', searchRequest.currentCardNumber);
    if (searchRequest.reason) params = params.set('reason', searchRequest.reason);
    if (searchRequest.processedBy) params = params.set('processedBy', searchRequest.processedBy);
    if (searchRequest.requestFrom) params = params.set('requestFrom', searchRequest.requestFrom);
    if (searchRequest.requestTo) params = params.set('requestTo', searchRequest.requestTo);
    if (searchRequest.processedFrom) params = params.set('processedFrom', searchRequest.processedFrom);
    if (searchRequest.processedTo) params = params.set('processedTo', searchRequest.processedTo);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Export card replacement requests
  exportCardReplacementRequests(status?: string, startDate?: string, endDate?: string): Observable<Blob> {
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
