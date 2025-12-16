import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account as AccountModel, CreateAccountRequest, UpdateAccountRequest, AccountSearchRequest, AccountStatistics } from '../model/account/account-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = `${environment.apiBaseUrl}/api/accounts`;

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createAccount(account: CreateAccountRequest): Observable<AccountModel> {
    return this.http.post<AccountModel>(`${this.apiUrl}/create`, account);
  }

  getAccountById(id: number): Observable<AccountModel> {
    return this.http.get<AccountModel>(`${this.apiUrl}/${id}`);
  }

  getAccountByPan(pan: string): Observable<AccountModel> {
    return this.http.get<AccountModel>(`${this.apiUrl}/pan/${pan}`);
  }

  getAccountByNumber(accountNumber: string): Observable<AccountModel> {
    return this.http.get<AccountModel>(`${this.apiUrl}/number/${accountNumber}`);
  }

  getAccountByAadhar(aadharNumber: string): Observable<AccountModel> {
    return this.http.get<AccountModel>(`${this.apiUrl}/aadhar/${aadharNumber}`);
  }

  updateAccount(id: number, accountDetails: UpdateAccountRequest): Observable<AccountModel> {
    return this.http.put<AccountModel>(`${this.apiUrl}/update/${id}`, accountDetails);
  }

  deleteAccount(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Pagination and sorting
  getAllAccounts(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    return this.http.get<any>(`${this.apiUrl}/all`, { params });
  }

  // Status-based operations
  getAccountsByStatus(status: string): Observable<AccountModel[]> {
    return this.http.get<AccountModel[]>(`${this.apiUrl}/status/${status}`);
  }

  getAccountsByStatusWithPagination(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}/paginated`, { params });
  }

  // Account type operations
  getAccountsByType(accountType: string): Observable<AccountModel[]> {
    return this.http.get<AccountModel[]>(`${this.apiUrl}/type/${accountType}`);
  }

  getAccountsByTypeWithPagination(accountType: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/type/${accountType}/paginated`, { params });
  }

  // KYC verification operations
  getKycVerifiedAccounts(): Observable<AccountModel[]> {
    return this.http.get<AccountModel[]>(`${this.apiUrl}/kyc-verified`);
  }

  getVerifiedMatrixAccounts(): Observable<AccountModel[]> {
    return this.http.get<AccountModel[]>(`${this.apiUrl}/verified-matrix`);
  }

  // Search operations
  searchAccounts(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Filter operations
  getAccountsByIncomeRange(minIncome: number, maxIncome: number): Observable<AccountModel[]> {
    const params = new HttpParams()
      .set('minIncome', minIncome.toString())
      .set('maxIncome', maxIncome.toString());
    
    return this.http.get<AccountModel[]>(`${this.apiUrl}/filter/income`, { params });
  }

  getAccountsByBalanceRange(minBalance: number, maxBalance: number): Observable<AccountModel[]> {
    const params = new HttpParams()
      .set('minBalance', minBalance.toString())
      .set('maxBalance', maxBalance.toString());
    
    return this.http.get<AccountModel[]>(`${this.apiUrl}/filter/balance`, { params });
  }

  getAccountsByOccupation(occupation: string): Observable<AccountModel[]> {
    return this.http.get<AccountModel[]>(`${this.apiUrl}/filter/occupation/${occupation}`);
  }

  getAccountsByCreatedDateRange(startDate: string, endDate: string): Observable<AccountModel[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get<AccountModel[]>(`${this.apiUrl}/filter/created-date`, { params });
  }

  // Balance operations
  updateBalance(accountNumber: string, amount: number): Observable<AccountModel> {
    const params = new HttpParams().set('amount', amount.toString());
    return this.http.put<AccountModel>(`${this.apiUrl}/balance/update/${accountNumber}`, null, { params });
  }

  debitBalance(accountNumber: string, amount: number): Observable<AccountModel> {
    const params = new HttpParams().set('amount', amount.toString());
    return this.http.put<AccountModel>(`${this.apiUrl}/balance/debit/${accountNumber}`, null, { params });
  }

  creditBalance(accountNumber: string, amount: number): Observable<AccountModel> {
    const params = new HttpParams().set('amount', amount.toString());
    return this.http.put<AccountModel>(`${this.apiUrl}/balance/credit/${accountNumber}`, null, { params });
  }

  // Statistics operations
  getAccountStatistics(): Observable<AccountStatistics> {
    return this.http.get<AccountStatistics>(`${this.apiUrl}/stats`);
  }

  getRecentAccounts(limit: number = 5): Observable<AccountModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<AccountModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Validation operations
  validatePan(pan: string): Observable<{isUnique: boolean}> {
    return this.http.get<{isUnique: boolean}>(`${this.apiUrl}/validate/pan/${pan}`);
  }

  validateAadhar(aadharNumber: string): Observable<{isUnique: boolean}> {
    return this.http.get<{isUnique: boolean}>(`${this.apiUrl}/validate/aadhar/${aadharNumber}`);
  }

  validateAccountNumber(accountNumber: string): Observable<{isUnique: boolean}> {
    return this.http.get<{isUnique: boolean}>(`${this.apiUrl}/validate/account-number/${accountNumber}`);
  }

  // Advanced search with multiple filters
  searchAccountsAdvanced(searchRequest: AccountSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.accountType) params = params.set('accountType', searchRequest.accountType);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.occupation) params = params.set('occupation', searchRequest.occupation);
    if (searchRequest.minIncome) params = params.set('minIncome', searchRequest.minIncome.toString());
    if (searchRequest.maxIncome) params = params.set('maxIncome', searchRequest.maxIncome.toString());
    if (searchRequest.minBalance) params = params.set('minBalance', searchRequest.minBalance.toString());
    if (searchRequest.maxBalance) params = params.set('maxBalance', searchRequest.maxBalance.toString());
    if (searchRequest.kycVerified !== undefined) params = params.set('kycVerified', searchRequest.kycVerified.toString());
    if (searchRequest.verifiedMatrix !== undefined) params = params.set('verifiedMatrix', searchRequest.verifiedMatrix.toString());
    if (searchRequest.createdFrom) params = params.set('createdFrom', searchRequest.createdFrom);
    if (searchRequest.createdTo) params = params.set('createdTo', searchRequest.createdTo);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }
}
