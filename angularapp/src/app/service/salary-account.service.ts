import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SalaryAccount, SalaryTransaction, SalaryNormalTransaction, SalaryLoginActivity, SalaryAccountStats, SalaryUpiTransaction, SalaryAdvanceRequest, SalaryFraudAlert } from '../model/salary-account/salary-account.model';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class SalaryAccountService {
  private apiUrl = `${environment.apiBaseUrl}/api/salary-accounts`;

  constructor(private http: HttpClient) {}

  // Create
  createAccount(account: SalaryAccount): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/create`, account);
  }

  // Read
  getAll(): Observable<SalaryAccount[]> {
    return this.http.get<SalaryAccount[]>(`${this.apiUrl}/all`);
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  getByAccountNumber(accountNumber: string): Observable<SalaryAccount> {
    return this.http.get<SalaryAccount>(`${this.apiUrl}/number/${accountNumber}`);
  }

  getByStatus(status: string): Observable<SalaryAccount[]> {
    return this.http.get<SalaryAccount[]>(`${this.apiUrl}/status/${status}`);
  }

  getByCompany(companyName: string): Observable<SalaryAccount[]> {
    return this.http.get<SalaryAccount[]>(`${this.apiUrl}/company/${companyName}`);
  }

  search(query: string): Observable<SalaryAccount[]> {
    return this.http.get<SalaryAccount[]>(`${this.apiUrl}/search`, { params: { q: query } });
  }

  linkEmployeeId(salaryAccountId: number, employeeId: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/link-employee/${salaryAccountId}`, { employeeId });
  }

  getByEmployeeId(employeeId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/by-employee/${employeeId}`);
  }

  // Update
  updateAccount(id: number, updates: Partial<SalaryAccount>): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/update/${id}`, updates);
  }

  freezeAccount(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/freeze/${id}`, {});
  }

  unfreezeAccount(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/unfreeze/${id}`, {});
  }

  // Salary Credit
  creditSalary(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/credit-salary/${id}`, {});
  }

  // Salary Transactions
  getTransactions(accountId: number): Observable<SalaryTransaction[]> {
    return this.http.get<SalaryTransaction[]>(`${this.apiUrl}/transactions/${accountId}`);
  }

  getTransactionsByAccountNumber(accountNumber: string): Observable<SalaryTransaction[]> {
    return this.http.get<SalaryTransaction[]>(`${this.apiUrl}/transactions/account/${accountNumber}`);
  }

  // Statistics (admin)
  getStats(): Observable<SalaryAccountStats> {
    return this.http.get<SalaryAccountStats>(`${this.apiUrl}/stats`);
  }

  // Authentication
  verifyCustomer(customerId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify-customer/${customerId}`);
  }

  signup(customerId: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/signup`, { customerId, password });
  }

  login(accountNumber: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { accountNumber, password });
  }

  // Transfer Money
  transferMoney(accountId: number, recipientAccount: string, recipientIfsc: string, amount: number, remark: string, transactionPin: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/transfer/${accountId}`, { recipientAccount, recipientIfsc, amount, remark, transactionPin });
  }

  // Verify Recipient Account
  verifyRecipientAccount(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify-recipient/${accountNumber}`);
  }

  // Withdraw Money
  withdrawMoney(accountId: number, amount: number, transactionPin: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/withdraw/${accountId}`, { amount, transactionPin });
  }

  // Normal Transactions (transfers/withdrawals)
  getNormalTransactions(accountId: number): Observable<SalaryNormalTransaction[]> {
    return this.http.get<SalaryNormalTransaction[]>(`${this.apiUrl}/normal-transactions/${accountId}`);
  }

  // Bank Charges
  getBankCharges(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/bank-charges/${accountId}`);
  }

  // Transaction PIN
  setTransactionPin(accountId: number, pin: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/set-pin/${accountId}`, { pin, password });
  }

  changeTransactionPin(accountId: number, oldPin: string, newPin: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/change-pin/${accountId}`, { oldPin, newPin });
  }

  // Login Activity
  getLoginActivity(accountId: number): Observable<SalaryLoginActivity[]> {
    return this.http.get<SalaryLoginActivity[]>(`${this.apiUrl}/login-activity/${accountId}`);
  }

  recordActivity(accountId: number, activityType: string, deviceInfo: string, browserName: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/record-activity/${accountId}`, { activityType, deviceInfo, browserName });
  }

  // Employee Dashboard Stats
  getDashboardStats(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/dashboard-stats/${accountId}`);
  }

  // Change Password
  changePassword(accountId: number, currentPassword: string, newPassword: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/change-password/${accountId}`, { currentPassword, newPassword });
  }

  // Salary Slip Data
  getSalarySlipData(accountId: number, transactionId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/salary-slip/${accountId}/${transactionId}`);
  }

  // ─── Employee Profile ──────────────────────────────────────
  updateProfile(accountId: number, updates: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/update-profile/${accountId}`, updates);
  }

  // ─── UPI Integration ──────────────────────────────────────
  enableUpi(accountId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/upi/enable/${accountId}`, {});
  }

  sendUpiPayment(accountId: number, recipientUpi: string, amount: number, remark: string, transactionPin: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/upi/pay/${accountId}`, { recipientUpi, amount, remark, transactionPin });
  }

  getUpiTransactions(accountId: number): Observable<SalaryUpiTransaction[]> {
    return this.http.get<SalaryUpiTransaction[]>(`${this.apiUrl}/upi/transactions/${accountId}`);
  }

  // ─── Loan Eligibility ─────────────────────────────────────
  getLoanEligibility(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/loan-eligibility/${accountId}`);
  }

  // ─── Auto Savings ─────────────────────────────────────────
  toggleAutoSavings(accountId: number, enable: boolean, percentage?: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auto-savings/toggle/${accountId}`, { enable, percentage });
  }

  getAutoSavingsInfo(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/auto-savings/${accountId}`);
  }

  withdrawSavings(accountId: number, amount: number, transactionPin: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auto-savings/withdraw/${accountId}`, { amount, transactionPin });
  }

  // ─── Salary Advance ───────────────────────────────────────
  requestSalaryAdvance(accountId: number, amount: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/advance/request/${accountId}`, { amount, reason });
  }

  getAdvanceHistory(accountId: number): Observable<SalaryAdvanceRequest[]> {
    return this.http.get<SalaryAdvanceRequest[]>(`${this.apiUrl}/advance/history/${accountId}`);
  }

  // ─── Blocked Account Management ────────────────────────────
  getBlockedAccounts(): Observable<SalaryAccount[]> {
    return this.http.get<SalaryAccount[]>(`${this.apiUrl}/blocked`);
  }

  unblockAccount(accountId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/unblock/${accountId}`, {});
  }

  resetAccountPassword(accountId: number, newPassword: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/reset-password/${accountId}`, { newPassword });
  }

  getAdvanceInfo(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/advance/info/${accountId}`);
  }

  // ─── Debit Card Management ────────────────────────────────
  updateDebitCardSettings(accountId: number, settings: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/debit-card/settings/${accountId}`, settings);
  }

  getDebitCardInfo(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/debit-card/${accountId}`);
  }

  generateCardDetails(accountId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/debit-card/generate/${accountId}`, {});
  }

  // ─── AI Fraud Detection ───────────────────────────────────
  getFraudAlerts(accountId: number): Observable<SalaryFraudAlert[]> {
    return this.http.get<SalaryFraudAlert[]>(`${this.apiUrl}/fraud/alerts/${accountId}`);
  }

  getFraudSummary(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/fraud/summary/${accountId}`);
  }

  resolveFraudAlert(alertId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/fraud/resolve/${alertId}`, {});
  }

  // ─── Close Account ─────────────────────────────────────────
  closeAccountPreCheck(accountId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/close-precheck/${accountId}`);
  }

  closeAccount(accountId: number, reason: string, closedBy: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/close/${accountId}`, { reason, closedBy });
  }

  // ─── Gold Loan ─────────────────────────────────────────────
  private goldLoanUrl = `${environment.apiBaseUrl}/api/gold-loans`;
  private goldRateUrl = `${environment.apiBaseUrl}/api/gold-rates`;

  getCurrentGoldRate(): Observable<any> {
    return this.http.get<any>(`${this.goldRateUrl}/current`);
  }

  getGoldRateHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${this.goldRateUrl}/history`);
  }

  updateGoldRate(ratePerGram: number, updatedBy: string): Observable<any> {
    return this.http.put<any>(`${this.goldRateUrl}/update`, null, { params: { ratePerGram: ratePerGram.toString(), updatedBy } });
  }

  calculateGoldLoan(grams: number): Observable<any> {
    return this.http.get<any>(`${this.goldLoanUrl}/calculate`, { params: { grams: grams.toString() } });
  }

  applyGoldLoan(loanData: any): Observable<any> {
    return this.http.post<any>(this.goldLoanUrl, loanData);
  }

  getGoldLoansByAccount(accountNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.goldLoanUrl}/account/${accountNumber}`);
  }

  getAllGoldLoans(): Observable<any[]> {
    return this.http.get<any[]>(this.goldLoanUrl);
  }

  approveGoldLoan(loanId: number, status: string, approvedBy: string, verificationData?: any): Observable<any> {
    let params: any = { status, approvedBy };
    if (verificationData) {
      return this.http.put<any>(`${this.goldLoanUrl}/${loanId}/approve`, verificationData, { params });
    }
    return this.http.put<any>(`${this.goldLoanUrl}/${loanId}/approve`, null, { params });
  }

  acceptGoldLoanTerms(loanId: number, acceptedBy: string): Observable<any> {
    return this.http.put<any>(`${this.goldLoanUrl}/${loanId}/accept-terms`, {}, { params: { acceptedBy } });
  }

  payGoldLoanEmi(emiId: number, accountNumber: string): Observable<any> {
    return this.http.post<any>(`${environment.apiBaseUrl}/api/emis/${emiId}/pay`, null, { params: { accountNumber } });
  }

  getGoldLoanEmiSchedule(loanAccountNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.goldLoanUrl}/emi-schedule/${loanAccountNumber}`);
  }

  calculateForeclosure(loanAccountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.goldLoanUrl}/foreclosure/${loanAccountNumber}`);
  }
}
