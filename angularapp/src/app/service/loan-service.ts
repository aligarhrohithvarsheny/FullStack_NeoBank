import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Loan as LoanModel, CreateLoanRequest, UpdateLoanRequest, ApproveLoanRequest, RejectLoanRequest, LoanSearchRequest, LoanStatistics } from '../model/loan/loan-module';

@Injectable({
  providedIn: 'root'
})
export class LoanService {
  private apiUrl = 'http://localhost:8080/api/loans';

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  applyLoan(loan: CreateLoanRequest): Observable<LoanModel> {
    return this.http.post<LoanModel>(`${this.apiUrl}`, loan);
  }

  getLoanById(id: number): Observable<LoanModel> {
    return this.http.get<LoanModel>(`${this.apiUrl}/${id}`);
  }

  updateLoan(id: number, loanDetails: UpdateLoanRequest): Observable<LoanModel> {
    return this.http.put<LoanModel>(`${this.apiUrl}/${id}`, loanDetails);
  }

  deleteLoan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Get all loans
  getAllLoans(): Observable<LoanModel[]> {
    return this.http.get<LoanModel[]>(`${this.apiUrl}`);
  }

  // User-specific operations
  getLoansByAccountNumber(accountNumber: string): Observable<LoanModel[]> {
    return this.http.get<LoanModel[]>(`${this.apiUrl}/user/${accountNumber}`);
  }

  getLoansByUserEmail(userEmail: string): Observable<LoanModel[]> {
    return this.http.get<LoanModel[]>(`${this.apiUrl}/email/${userEmail}`);
  }

  // Status-based operations
  getLoansByStatus(status: string): Observable<LoanModel[]> {
    return this.http.get<LoanModel[]>(`${this.apiUrl}/status/${status}`);
  }

  getLoansByStatusWithPagination(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}/paginated`, { params });
  }

  // Type-based operations
  getLoansByType(type: string): Observable<LoanModel[]> {
    return this.http.get<LoanModel[]>(`${this.apiUrl}/type/${type}`);
  }

  getLoansByTypeAndStatus(type: string, status: string): Observable<LoanModel[]> {
    return this.http.get<LoanModel[]>(`${this.apiUrl}/type/${type}/status/${status}`);
  }

  // Amount range operations
  getLoansByAmountRange(minAmount: number, maxAmount: number): Observable<LoanModel[]> {
    const params = new HttpParams()
      .set('minAmount', minAmount.toString())
      .set('maxAmount', maxAmount.toString());
    
    return this.http.get<LoanModel[]>(`${this.apiUrl}/amount-range`, { params });
  }

  // Date range operations
  getLoansByApplicationDateRange(startDate: string, endDate: string): Observable<LoanModel[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get<LoanModel[]>(`${this.apiUrl}/application-date-range`, { params });
  }

  getLoansByApprovalDateRange(startDate: string, endDate: string): Observable<LoanModel[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get<LoanModel[]>(`${this.apiUrl}/approval-date-range`, { params });
  }

  // Search operations
  searchLoans(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Admin operations
  approveLoan(approveRequest: ApproveLoanRequest): Observable<LoanModel> {
    return this.http.put<LoanModel>(`${this.apiUrl}/${approveRequest.loanId}/approve`, approveRequest);
  }

  rejectLoan(rejectRequest: RejectLoanRequest): Observable<LoanModel> {
    return this.http.put<LoanModel>(`${this.apiUrl}/${rejectRequest.loanId}/reject`, rejectRequest);
  }

  getLoansByApprovedBy(adminName: string): Observable<LoanModel[]> {
    return this.http.get<LoanModel[]>(`${this.apiUrl}/approved-by/${adminName}`);
  }

  getPendingLoansForReview(): Observable<LoanModel[]> {
    return this.http.get<LoanModel[]>(`${this.apiUrl}/pending-review`);
  }

  // Statistics operations
  getLoanStatistics(): Observable<LoanStatistics> {
    return this.http.get<LoanStatistics>(`${this.apiUrl}/stats`);
  }

  getTotalApprovedLoanAmount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/total-approved-amount`);
  }

  getAverageLoanAmount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/average-amount`);
  }

  getRecentLoans(limit: number = 5): Observable<LoanModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<LoanModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Advanced search with multiple filters
  searchLoansAdvanced(searchRequest: LoanSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.type) params = params.set('type', searchRequest.type);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.purpose) params = params.set('purpose', searchRequest.purpose);
    if (searchRequest.minAmount) params = params.set('minAmount', searchRequest.minAmount.toString());
    if (searchRequest.maxAmount) params = params.set('maxAmount', searchRequest.maxAmount.toString());
    if (searchRequest.minTenure) params = params.set('minTenure', searchRequest.minTenure.toString());
    if (searchRequest.maxTenure) params = params.set('maxTenure', searchRequest.maxTenure.toString());
    if (searchRequest.minInterestRate) params = params.set('minInterestRate', searchRequest.minInterestRate.toString());
    if (searchRequest.maxInterestRate) params = params.set('maxInterestRate', searchRequest.maxInterestRate.toString());
    if (searchRequest.userName) params = params.set('userName', searchRequest.userName);
    if (searchRequest.userEmail) params = params.set('userEmail', searchRequest.userEmail);
    if (searchRequest.accountNumber) params = params.set('accountNumber', searchRequest.accountNumber);
    if (searchRequest.loanAccountNumber) params = params.set('loanAccountNumber', searchRequest.loanAccountNumber);
    if (searchRequest.applicationFrom) params = params.set('applicationFrom', searchRequest.applicationFrom);
    if (searchRequest.applicationTo) params = params.set('applicationTo', searchRequest.applicationTo);
    if (searchRequest.approvalFrom) params = params.set('approvalFrom', searchRequest.approvalFrom);
    if (searchRequest.approvalTo) params = params.set('approvalTo', searchRequest.approvalTo);
    if (searchRequest.approvedBy) params = params.set('approvedBy', searchRequest.approvedBy);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Loan processing
  processLoanApplication(loanId: number, action: 'approve' | 'reject', adminName: string, comments?: string): Observable<LoanModel> {
    const body = { adminName, comments };
    return this.http.put<LoanModel>(`${this.apiUrl}/${loanId}/${action}`, body);
  }

  // Generate loan account number
  generateLoanAccountNumber(): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/generate-account-number`);
  }

  // Calculate EMI
  calculateEMI(amount: number, tenure: number, interestRate: number): Observable<number> {
    const params = new HttpParams()
      .set('amount', amount.toString())
      .set('tenure', tenure.toString())
      .set('interestRate', interestRate.toString());
    
    return this.http.get<number>(`${this.apiUrl}/calculate-emi`, { params });
  }

  // Export loans
  exportLoans(status?: string, startDate?: string, endDate?: string): Observable<Blob> {
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
