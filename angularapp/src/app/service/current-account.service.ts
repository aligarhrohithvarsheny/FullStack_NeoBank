import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CurrentAccount, BusinessTransaction, CurrentAccountStatistics, TransactionSummary, CurrentAccountEditHistory, CurrentAccountBeneficiary, CurrentAccountChequeRequest, CurrentAccountVendorPayment, CurrentAccountInvoice, CurrentAccountBusinessLoan, CurrentAccountBusinessUser, LinkedAccount } from '../model/current-account/current-account.model';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class CurrentAccountService {
  private apiUrl = `${environment.apiBaseUrl}/api/current-accounts`;

  constructor(private http: HttpClient) {}

  // ==================== Account CRUD ====================

  createAccount(account: CurrentAccount): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/create`, account);
  }

  getAllAccounts(): Observable<CurrentAccount[]> {
    return this.http.get<CurrentAccount[]>(this.apiUrl);
  }

  getAllAccountsPaginated(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<any>(`${this.apiUrl}/paginated`, { params });
  }

  getAccountById(id: number): Observable<CurrentAccount> {
    return this.http.get<CurrentAccount>(`${this.apiUrl}/${id}`);
  }

  getAccountByNumber(accountNumber: string): Observable<CurrentAccount> {
    return this.http.get<CurrentAccount>(`${this.apiUrl}/number/${accountNumber}`);
  }

  getAccountByCustomerId(customerId: string): Observable<CurrentAccount> {
    return this.http.get<CurrentAccount>(`${this.apiUrl}/customer/${customerId}`);
  }

  getAccountsByStatus(status: string): Observable<CurrentAccount[]> {
    return this.http.get<CurrentAccount[]>(`${this.apiUrl}/status/${status}`);
  }

  searchAccounts(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  updateAccount(id: number, details: Partial<CurrentAccount>): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/update/${id}`, details);
  }

  // ==================== Account Operations ====================

  approveAccount(id: number, approvedBy: string): Observable<any> {
    const params = new HttpParams().set('approvedBy', approvedBy);
    return this.http.put<any>(`${this.apiUrl}/approve/${id}`, null, { params });
  }

  rejectAccount(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/reject/${id}`, null);
  }

  verifyKyc(id: number, verifiedBy: string): Observable<any> {
    const params = new HttpParams().set('verifiedBy', verifiedBy);
    return this.http.put<any>(`${this.apiUrl}/verify-kyc/${id}`, null, { params });
  }

  freezeAccount(id: number, reason: string, frozenBy: string): Observable<any> {
    const params = new HttpParams().set('reason', reason).set('frozenBy', frozenBy);
    return this.http.put<any>(`${this.apiUrl}/freeze/${id}`, null, { params });
  }

  unfreezeAccount(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/unfreeze/${id}`, null);
  }

  approveOverdraft(id: number, overdraftLimit: number): Observable<any> {
    const params = new HttpParams().set('overdraftLimit', overdraftLimit.toString());
    return this.http.put<any>(`${this.apiUrl}/approve-overdraft/${id}`, null, { params });
  }

  // ==================== Statistics ====================

  getStatistics(): Observable<CurrentAccountStatistics> {
    return this.http.get<CurrentAccountStatistics>(`${this.apiUrl}/statistics`);
  }

  // ==================== Transactions ====================

  processTransaction(accountNumber: string, txnType: string, amount: number, description?: string): Observable<any> {
    let params = new HttpParams()
      .set('accountNumber', accountNumber)
      .set('txnType', txnType)
      .set('amount', amount.toString());
    if (description) params = params.set('description', description);
    return this.http.post<any>(`${this.apiUrl}/transactions/process`, null, { params });
  }

  processTransfer(transferRequest: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/transactions/transfer`, transferRequest);
  }

  verifyRecipientAccount(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify-recipient/${accountNumber}`);
  }

  getTransactions(accountNumber: string): Observable<BusinessTransaction[]> {
    return this.http.get<BusinessTransaction[]>(`${this.apiUrl}/transactions/${accountNumber}`);
  }

  getTransactionsPaginated(accountNumber: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/transactions/${accountNumber}/paginated`, { params });
  }

  getTransactionSummary(accountNumber: string): Observable<TransactionSummary> {
    return this.http.get<TransactionSummary>(`${this.apiUrl}/transactions/${accountNumber}/summary`);
  }

  getRecentTransactions(accountNumber: string, count: number = 5): Observable<BusinessTransaction[]> {
    const params = new HttpParams().set('count', count.toString());
    return this.http.get<BusinessTransaction[]>(`${this.apiUrl}/transactions/${accountNumber}/recent`, { params });
  }

  // ==================== Authentication ====================

  verifyCustomer(customerId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify-customer/${customerId}`);
  }

  signup(customerId: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/signup`, { customerId, password });
  }

  login(accountNumber: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { accountNumber, password });
  }

  changePassword(accountNumber: string, currentPassword: string, newPassword: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/change-password/${accountNumber}`, { currentPassword, newPassword });
  }

  getTransactionsByDateRange(accountNumber: string, startDate: string, endDate: string): Observable<BusinessTransaction[]> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<BusinessTransaction[]>(`${this.apiUrl}/transactions/${accountNumber}/date-range`, { params });
  }

  searchTransactions(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/transactions/search`, { params });
  }

  // ==================== Edit with History ====================

  editAccountWithHistory(id: number, account: CurrentAccount, editedBy: string, document?: File): Observable<any> {
    const formData = new FormData();
    formData.append('account', new Blob([JSON.stringify(account)], { type: 'application/json' }));
    if (document) {
      formData.append('document', document);
    }
    const params = new HttpParams().set('editedBy', editedBy);
    return this.http.put<any>(`${this.apiUrl}/edit-with-history/${id}`, formData, { params });
  }

  getEditHistory(accountId: number): Observable<CurrentAccountEditHistory[]> {
    return this.http.get<CurrentAccountEditHistory[]>(`${this.apiUrl}/edit-history/${accountId}`);
  }

  getEditHistoryByAccountNumber(accountNumber: string): Observable<CurrentAccountEditHistory[]> {
    return this.http.get<CurrentAccountEditHistory[]>(`${this.apiUrl}/edit-history/account/${accountNumber}`);
  }

  // ==================== User Dashboard ====================

  getUserDashboardStats(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/user/dashboard-stats/${accountNumber}`);
  }

  updateBusinessProfile(accountNumber: string, updates: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/user/update-profile/${accountNumber}`, updates);
  }

  // ==================== Beneficiary Management ====================

  addBeneficiary(beneficiary: CurrentAccountBeneficiary): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/beneficiaries/add`, beneficiary);
  }

  getBeneficiaries(accountNumber: string): Observable<CurrentAccountBeneficiary[]> {
    return this.http.get<CurrentAccountBeneficiary[]>(`${this.apiUrl}/beneficiaries/${accountNumber}`);
  }

  getActiveBeneficiaries(accountNumber: string): Observable<CurrentAccountBeneficiary[]> {
    return this.http.get<CurrentAccountBeneficiary[]>(`${this.apiUrl}/beneficiaries/${accountNumber}/active`);
  }

  deleteBeneficiary(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/beneficiaries/${id}`);
  }

  // ==================== Cheque Requests ====================

  requestChequeBook(request: CurrentAccountChequeRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/cheque-requests/create`, request);
  }

  getChequeRequests(accountNumber: string): Observable<CurrentAccountChequeRequest[]> {
    return this.http.get<CurrentAccountChequeRequest[]>(`${this.apiUrl}/cheque-requests/${accountNumber}`);
  }

  getPendingChequeRequests(): Observable<CurrentAccountChequeRequest[]> {
    return this.http.get<CurrentAccountChequeRequest[]>(`${this.apiUrl}/cheque-requests/pending`);
  }

  approveChequeRequest(id: number, approvedBy: string): Observable<any> {
    const params = new HttpParams().set('approvedBy', approvedBy);
    return this.http.put<any>(`${this.apiUrl}/cheque-requests/approve/${id}`, null, { params });
  }

  // ==================== Vendor Payments ====================

  processVendorPayment(payment: CurrentAccountVendorPayment): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/vendor-payments/process`, payment);
  }

  getVendorPayments(accountNumber: string): Observable<CurrentAccountVendorPayment[]> {
    return this.http.get<CurrentAccountVendorPayment[]>(`${this.apiUrl}/vendor-payments/${accountNumber}`);
  }

  // ==================== Invoice Management ====================

  createInvoice(invoice: CurrentAccountInvoice): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/invoices/create`, invoice);
  }

  getInvoices(accountNumber: string): Observable<CurrentAccountInvoice[]> {
    return this.http.get<CurrentAccountInvoice[]>(`${this.apiUrl}/invoices/${accountNumber}`);
  }

  updateInvoiceStatus(id: number, status: string): Observable<any> {
    const params = new HttpParams().set('status', status);
    return this.http.put<any>(`${this.apiUrl}/invoices/status/${id}`, null, { params });
  }

  markInvoicePaid(id: number, paidAmount: number): Observable<any> {
    const params = new HttpParams().set('paidAmount', paidAmount.toString());
    return this.http.put<any>(`${this.apiUrl}/invoices/mark-paid/${id}`, null, { params });
  }

  deleteInvoice(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/invoices/${id}`);
  }

  // ==================== Business Loan Application ====================

  applyForBusinessLoan(loan: CurrentAccountBusinessLoan): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/business-loans/apply`, loan);
  }

  getBusinessLoans(accountNumber: string): Observable<CurrentAccountBusinessLoan[]> {
    return this.http.get<CurrentAccountBusinessLoan[]>(`${this.apiUrl}/business-loans/${accountNumber}`);
  }

  getPendingBusinessLoans(): Observable<CurrentAccountBusinessLoan[]> {
    return this.http.get<CurrentAccountBusinessLoan[]>(`${this.apiUrl}/business-loans/pending`);
  }

  approveBusinessLoan(id: number, approvedBy: string, approvedAmount: number, interestRate: number): Observable<any> {
    const params = new HttpParams()
      .set('approvedBy', approvedBy)
      .set('approvedAmount', approvedAmount.toString())
      .set('interestRate', interestRate.toString());
    return this.http.put<any>(`${this.apiUrl}/business-loans/approve/${id}`, null, { params });
  }

  rejectBusinessLoan(id: number, rejectedBy: string, reason: string): Observable<any> {
    const params = new HttpParams().set('rejectedBy', rejectedBy).set('reason', reason);
    return this.http.put<any>(`${this.apiUrl}/business-loans/reject/${id}`, null, { params });
  }

  // ==================== Multi-User Access ====================

  addBusinessUser(user: CurrentAccountBusinessUser): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/business-users/add`, user);
  }

  getBusinessUsers(accountNumber: string): Observable<CurrentAccountBusinessUser[]> {
    return this.http.get<CurrentAccountBusinessUser[]>(`${this.apiUrl}/business-users/${accountNumber}`);
  }

  updateUserRole(id: number, role: string): Observable<any> {
    const params = new HttpParams().set('role', role);
    return this.http.put<any>(`${this.apiUrl}/business-users/role/${id}`, null, { params });
  }

  updateUserStatus(id: number, status: string): Observable<any> {
    const params = new HttpParams().set('status', status);
    return this.http.put<any>(`${this.apiUrl}/business-users/status/${id}`, null, { params });
  }

  removeBusinessUser(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/business-users/${id}`);
  }

  businessUserLogin(email: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/business-users/login`, { email, password });
  }

  // ==================== Linked Accounts ====================

  verifySavingsAccount(accountNumber: string, customerId: string): Observable<any> {
    const params = new HttpParams()
      .set('accountNumber', accountNumber)
      .set('customerId', customerId);
    return this.http.get<any>(`${this.apiUrl}/linked-accounts/verify-savings`, { params });
  }

  linkSavingsAccount(currentAccountNumber: string, savingsAccountNumber: string, savingsCustomerId: string, linkedBy: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/linked-accounts/link`, {
      currentAccountNumber, savingsAccountNumber, savingsCustomerId, linkedBy
    });
  }

  getLinkedAccounts(currentAccountNumber: string): Observable<LinkedAccount[]> {
    return this.http.get<LinkedAccount[]>(`${this.apiUrl}/linked-accounts/${currentAccountNumber}`);
  }

  getLinkedSavingsDetails(savingsAccountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/linked-accounts/savings-details/${savingsAccountNumber}`);
  }

  unlinkSavingsAccount(linkId: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/linked-accounts/unlink/${linkId}`, null);
  }

  // ==================== Switch PIN ====================

  createSwitchPin(linkId: number, pin: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/linked-accounts/create-pin/${linkId}`, { pin });
  }

  verifySwitchPin(linkId: number, pin: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/linked-accounts/verify-pin/${linkId}`, { pin });
  }

  // ==================== User-Side Linked Account Lookups ====================

  getLinkedAccountsBySavings(savingsAccountNumber: string): Observable<LinkedAccount[]> {
    return this.http.get<LinkedAccount[]>(`${this.apiUrl}/linked-accounts/by-savings/${savingsAccountNumber}`);
  }

  getLinkedCurrentAccountDetails(currentAccountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/linked-accounts/current-details/${currentAccountNumber}`);
  }
}
