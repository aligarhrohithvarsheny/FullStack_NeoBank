import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NewCardRequest as NewCardRequestModel, CreateNewCardRequest, UpdateNewCardRequest, ApproveNewCardRequest, RejectNewCardRequest, NewCardSearchRequest, NewCardStatistics } from '../model/new-card-request/new-card-request-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class NewCardRequestService {
  private apiUrl = `${environment.apiUrl}/new-card-requests`;

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createNewCardRequest(request: CreateNewCardRequest): Observable<NewCardRequestModel> {
    return this.http.post<NewCardRequestModel>(`${this.apiUrl}`, request);
  }

  getNewCardRequestById(id: number): Observable<NewCardRequestModel> {
    return this.http.get<NewCardRequestModel>(`${this.apiUrl}/${id}`);
  }

  updateNewCardRequest(id: number, requestDetails: UpdateNewCardRequest): Observable<NewCardRequestModel> {
    return this.http.put<NewCardRequestModel>(`${this.apiUrl}/${id}`, requestDetails);
  }

  deleteNewCardRequest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // User-specific operations
  getNewCardRequestsByUserId(userId: string): Observable<NewCardRequestModel[]> {
    return this.http.get<NewCardRequestModel[]>(`${this.apiUrl}/user/${userId}`);
  }

  getNewCardRequestsByAccountNumber(accountNumber: string): Observable<NewCardRequestModel[]> {
    return this.http.get<NewCardRequestModel[]>(`${this.apiUrl}/account/${accountNumber}`);
  }

  // Status-based operations
  getNewCardRequestsByStatus(status: string): Observable<NewCardRequestModel[]> {
    return this.http.get<NewCardRequestModel[]>(`${this.apiUrl}/status/${status}`);
  }

  getNewCardRequestsByStatusWithPagination(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}/paginated`, { params });
  }

  // Card type operations
  getNewCardRequestsByCardType(cardType: string): Observable<NewCardRequestModel[]> {
    return this.http.get<NewCardRequestModel[]>(`${this.apiUrl}/card-type/${cardType}`);
  }

  // Admin operations
  approveNewCardRequest(approveRequest: ApproveNewCardRequest): Observable<NewCardRequestModel> {
    return this.http.put<NewCardRequestModel>(`${this.apiUrl}/${approveRequest.requestId}/approve`, approveRequest);
  }

  rejectNewCardRequest(rejectRequest: RejectNewCardRequest): Observable<NewCardRequestModel> {
    return this.http.put<NewCardRequestModel>(`${this.apiUrl}/${rejectRequest.requestId}/reject`, rejectRequest);
  }

  getNewCardRequestsByProcessedBy(adminName: string): Observable<NewCardRequestModel[]> {
    return this.http.get<NewCardRequestModel[]>(`${this.apiUrl}/processed-by/${adminName}`);
  }

  getPendingNewCardRequestsForReview(): Observable<NewCardRequestModel[]> {
    return this.http.get<NewCardRequestModel[]>(`${this.apiUrl}/pending-review`);
  }

  // Statistics operations
  getNewCardRequestStatistics(): Observable<NewCardStatistics> {
    return this.http.get<NewCardStatistics>(`${this.apiUrl}/stats`);
  }

  getRecentNewCardRequests(limit: number = 5): Observable<NewCardRequestModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<NewCardRequestModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Advanced search with multiple filters
  searchNewCardRequestsAdvanced(searchRequest: NewCardSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.userId) params = params.set('userId', searchRequest.userId);
    if (searchRequest.userName) params = params.set('userName', searchRequest.userName);
    if (searchRequest.userEmail) params = params.set('userEmail', searchRequest.userEmail);
    if (searchRequest.accountNumber) params = params.set('accountNumber', searchRequest.accountNumber);
    if (searchRequest.cardType) params = params.set('cardType', searchRequest.cardType);
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

  // Export new card requests
  exportNewCardRequests(status?: string, cardType?: string, startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (cardType) params = params.set('cardType', cardType);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    
    return this.http.get(`${this.apiUrl}/export`, { 
      params, 
      responseType: 'blob' 
    });
  }
}
