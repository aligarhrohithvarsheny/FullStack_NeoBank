import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction as TransactionModel, CreateTransactionRequest, UpdateTransactionRequest, TransactionSearchRequest, TransactionStatistics } from '../model/transaction/transaction-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = `${environment.apiBaseUrl}/api/transactions`;

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createTransaction(transaction: CreateTransactionRequest): Observable<TransactionModel> {
    return this.http.post<TransactionModel>(`${this.apiUrl}`, transaction);
  }

  getTransactionById(id: number): Observable<TransactionModel> {
    return this.http.get<TransactionModel>(`${this.apiUrl}/${id}`);
  }

  updateTransaction(id: number, transactionDetails: UpdateTransactionRequest): Observable<TransactionModel> {
    return this.http.put<TransactionModel>(`${this.apiUrl}/${id}`, transactionDetails);
  }

  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Pagination and sorting
  getAllTransactions(page: number = 0, size: number = 10, sortBy: string = 'date', sortDir: string = 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    return this.http.get<any>(`${this.apiUrl}`, { params });
  }

  // Account-based operations
  getTransactionsByAccount(accountNumber: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}`, { params });
  }

  getTransactionsByUser(userName: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/user/${userName}`, { params });
  }

  // Type-based operations
  getTransactionsByType(type: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/type/${type}`, { params });
  }

  getTransactionsByStatus(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}`, { params });
  }

  // Date range operations
  getTransactionsByDateRange(startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/date-range`, { params });
  }

  getTransactionsByAccountAndDateRange(accountNumber: string, startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}/date-range`, { params });
  }

  // Search operations
  searchTransactions(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  searchTransactionsByMerchant(merchant: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('merchant', merchant)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/merchant`, { params });
  }

  searchTransactionsByDescription(description: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('description', description)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/description`, { params });
  }

  // Statistics operations
  getTransactionStatistics(): Observable<TransactionStatistics> {
    return this.http.get<TransactionStatistics>(`${this.apiUrl}/stats`);
  }

  getTransactionSummaryByAccount(accountNumber: string, type: string): Observable<any> {
    const params = new HttpParams()
      .set('accountNumber', accountNumber)
      .set('type', type);
    
    return this.http.get<any>(`${this.apiUrl}/summary`, { params });
  }

  getRecentTransactions(limit: number = 5): Observable<TransactionModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<TransactionModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Mini statement (last 5 transactions)
  getMiniStatement(accountNumber: string): Observable<TransactionModel[]> {
    return this.http.get<TransactionModel[]>(`${this.apiUrl}/mini-statement/${accountNumber}`);
  }

  // Advanced search with multiple filters
  searchTransactionsAdvanced(searchRequest: TransactionSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.type) params = params.set('type', searchRequest.type);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.merchant) params = params.set('merchant', searchRequest.merchant);
    if (searchRequest.description) params = params.set('description', searchRequest.description);
    if (searchRequest.userName) params = params.set('userName', searchRequest.userName);
    if (searchRequest.accountNumber) params = params.set('accountNumber', searchRequest.accountNumber);
    if (searchRequest.minAmount) params = params.set('minAmount', searchRequest.minAmount.toString());
    if (searchRequest.maxAmount) params = params.set('maxAmount', searchRequest.maxAmount.toString());
    if (searchRequest.minBalance) params = params.set('minBalance', searchRequest.minBalance.toString());
    if (searchRequest.maxBalance) params = params.set('maxBalance', searchRequest.maxBalance.toString());
    if (searchRequest.dateFrom) params = params.set('dateFrom', searchRequest.dateFrom);
    if (searchRequest.dateTo) params = params.set('dateTo', searchRequest.dateTo);
    if (searchRequest.transferType) params = params.set('transferType', searchRequest.transferType);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Transaction processing
  processTransaction(transactionId: string, status: string): Observable<TransactionModel> {
    const params = new HttpParams().set('status', status);
    return this.http.put<TransactionModel>(`${this.apiUrl}/process/${transactionId}`, null, { params });
  }

  // Export transactions
  exportTransactions(accountNumber: string, startDate: string, endDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('accountNumber', accountNumber)
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get(`${this.apiUrl}/export`, { 
      params, 
      responseType: 'blob' 
    });
  }
}
