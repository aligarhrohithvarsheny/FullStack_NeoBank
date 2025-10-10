import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TransferRecord as TransferModel, CreateTransferRequest, UpdateTransferRequest, ProcessTransferRequest, TransferSearchRequest, TransferStatistics, TransferType } from '../model/transfer-record/transfer-record-module';

@Injectable({
  providedIn: 'root'
})
export class TransferService {
  private apiUrl = 'http://localhost:8080/api/transfers';

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createTransfer(transfer: CreateTransferRequest): Observable<TransferModel> {
    return this.http.post<TransferModel>(`${this.apiUrl}`, transfer);
  }

  getTransferById(id: number): Observable<TransferModel> {
    return this.http.get<TransferModel>(`${this.apiUrl}/${id}`);
  }

  updateTransfer(id: number, transferDetails: UpdateTransferRequest): Observable<TransferModel> {
    return this.http.put<TransferModel>(`${this.apiUrl}/${id}`, transferDetails);
  }

  deleteTransfer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Account-specific operations
  getTransfersBySenderAccount(senderAccountNumber: string): Observable<TransferModel[]> {
    return this.http.get<TransferModel[]>(`${this.apiUrl}/sender/${senderAccountNumber}`);
  }

  getTransfersByRecipientAccount(recipientAccountNumber: string): Observable<TransferModel[]> {
    return this.http.get<TransferModel[]>(`${this.apiUrl}/recipient/${recipientAccountNumber}`);
  }

  // Status-based operations
  getTransfersByStatus(status: string): Observable<TransferModel[]> {
    return this.http.get<TransferModel[]>(`${this.apiUrl}/status/${status}`);
  }

  getTransfersByStatusWithPagination(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}/paginated`, { params });
  }

  // Type-based operations
  getTransfersByType(transferType: TransferType): Observable<TransferModel[]> {
    return this.http.get<TransferModel[]>(`${this.apiUrl}/type/${transferType}`);
  }

  // Amount range operations
  getTransfersByAmountRange(minAmount: number, maxAmount: number): Observable<TransferModel[]> {
    const params = new HttpParams()
      .set('minAmount', minAmount.toString())
      .set('maxAmount', maxAmount.toString());
    
    return this.http.get<TransferModel[]>(`${this.apiUrl}/amount-range`, { params });
  }

  // Date range operations
  getTransfersByDateRange(startDate: string, endDate: string): Observable<TransferModel[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get<TransferModel[]>(`${this.apiUrl}/date-range`, { params });
  }

  // Search operations
  searchTransfers(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Processing operations
  processTransfer(processRequest: ProcessTransferRequest): Observable<TransferModel> {
    return this.http.put<TransferModel>(`${this.apiUrl}/${processRequest.transferId}/process`, processRequest);
  }

  // Statistics operations
  getTransferStatistics(): Observable<TransferStatistics> {
    return this.http.get<TransferStatistics>(`${this.apiUrl}/stats`);
  }

  getRecentTransfers(limit: number = 5): Observable<TransferModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<TransferModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Advanced search with multiple filters
  searchTransfersAdvanced(searchRequest: TransferSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.transferType) params = params.set('transferType', searchRequest.transferType);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.senderAccountNumber) params = params.set('senderAccountNumber', searchRequest.senderAccountNumber);
    if (searchRequest.senderName) params = params.set('senderName', searchRequest.senderName);
    if (searchRequest.recipientAccountNumber) params = params.set('recipientAccountNumber', searchRequest.recipientAccountNumber);
    if (searchRequest.recipientName) params = params.set('recipientName', searchRequest.recipientName);
    if (searchRequest.phone) params = params.set('phone', searchRequest.phone);
    if (searchRequest.ifsc) params = params.set('ifsc', searchRequest.ifsc);
    if (searchRequest.minAmount) params = params.set('minAmount', searchRequest.minAmount.toString());
    if (searchRequest.maxAmount) params = params.set('maxAmount', searchRequest.maxAmount.toString());
    if (searchRequest.dateFrom) params = params.set('dateFrom', searchRequest.dateFrom);
    if (searchRequest.dateTo) params = params.set('dateTo', searchRequest.dateTo);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Export transfers
  exportTransfers(status?: string, startDate?: string, endDate?: string): Observable<Blob> {
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
