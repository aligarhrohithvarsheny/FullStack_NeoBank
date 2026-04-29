import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: string;
  status: string;
  createdAt: string;
  lastLogin?: string;
  permissions: string[];
}

export interface AdminStats {
  totalUsers: number;
  totalAccounts: number;
  totalLoans: number;
  totalTransactions: number;
  totalRevenue: number;
  activeSessions: number;
  totalSalaryAccounts: number;
  pendingApprovals: number;
  fraudAlerts: number;
  systemHealth: number;
}

export interface SalaryAccountAdmin {
  id: number;
  accountNumber: string;
  employeeName: string;
  employeeId: string;
  companyName: string;
  salary: number;
  status: string;
  createdAt: string;
  lastSalaryCreditDate?: string;
  balance: number;
  accountType: string;
}

export interface UserAdmin {
  id: number;
  username: string;
  email: string;
  accountNumber?: string;
  phoneNumber?: string;
  status: string;
  accountType: string;
  balance: number;
  createdAt: string;
  kycStatus: string;
}

export interface TransactionAdmin {
  id: number;
  transactionId: string;
  accountNumber: string;
  userName: string;
  type: string;
  amount: number;
  description: string;
  date: string;
  status: string;
  balance: number;
}

export interface LoanAdmin {
  id: number;
  loanNumber: string;
  accountNumber: string;
  userName: string;
  loanType: string;
  amount: number;
  interestRate: number;
  tenure: number;
  emi: number;
  status: string;
  appliedDate: string;
  approvedDate?: string;
  disbursementAmount: number;
}

export interface AdminAuditLog {
  id: number;
  adminName: string;
  action: string;
  details: string;
  timestamp: string;
  ipAddress?: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiBaseUrl}/api/admin`;

  constructor(private http: HttpClient) {}

  // ─── Dashboard Stats ──────────────────────────────────────
  getDashboardStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.apiUrl}/stats/dashboard`);
  }

  getDailyStats(date: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/daily`, { params: { date } });
  }

  getMonthlyStats(month: number, year: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/monthly`, { params: { month: month.toString(), year: year.toString() } });
  }

  // ─── User Management ──────────────────────────────────────
  getAllUsers(page?: number, limit?: number): Observable<any> {
    let params = new HttpParams();
    if (page) params = params.set('page', page.toString());
    if (limit) params = params.set('limit', limit.toString());
    return this.http.get<any>(`${this.apiUrl}/users/all`, { params });
  }

  getUserById(userId: number): Observable<UserAdmin> {
    return this.http.get<UserAdmin>(`${this.apiUrl}/users/${userId}`);
  }

  searchUsers(query: string): Observable<UserAdmin[]> {
    return this.http.get<UserAdmin[]>(`${this.apiUrl}/users/search`, { params: { q: query } });
  }

  blockUser(userId: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/users/${userId}/block`, { reason });
  }

  unblockUser(userId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/users/${userId}/unblock`, {});
  }

  updateUserStatus(userId: number, status: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/users/${userId}/status`, { status });
  }

  resetUserPassword(userId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/users/${userId}/reset-password`, {});
  }

  // ─── Salary Account Management ────────────────────────────
  getAllSalaryAccounts(page?: number, limit?: number): Observable<any> {
    let params = new HttpParams();
    if (page) params = params.set('page', page.toString());
    if (limit) params = params.set('limit', limit.toString());
    return this.http.get<any>(`${this.apiUrl}/salary-accounts/all`, { params });
  }

  getSalaryAccountById(id: number): Observable<SalaryAccountAdmin> {
    return this.http.get<SalaryAccountAdmin>(`${this.apiUrl}/salary-accounts/${id}`);
  }

  getSalaryAccountByNumber(accountNumber: string): Observable<SalaryAccountAdmin> {
    return this.http.get<SalaryAccountAdmin>(`${this.apiUrl}/salary-accounts/number/${accountNumber}`);
  }

  searchSalaryAccounts(query: string): Observable<SalaryAccountAdmin[]> {
    return this.http.get<SalaryAccountAdmin[]>(`${this.apiUrl}/salary-accounts/search`, { params: { q: query } });
  }

  approveSalaryAccount(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/salary-accounts/${id}/approve`, {});
  }

  rejectSalaryAccount(id: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/salary-accounts/${id}/reject`, { reason });
  }

  freezeSalaryAccount(id: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/salary-accounts/${id}/freeze`, { reason });
  }

  unfreezeSalaryAccount(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/salary-accounts/${id}/unfreeze`, {});
  }

  creditSalaryBulk(data: any[]): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/salary-accounts/credit-bulk`, { data });
  }

  // ─── Transaction Management ───────────────────────────────
  getAllTransactions(page?: number, limit?: number): Observable<any> {
    let params = new HttpParams();
    if (page) params = params.set('page', page.toString());
    if (limit) params = params.set('limit', limit.toString());
    return this.http.get<any>(`${this.apiUrl}/transactions/all`, { params });
  }

  getTransactionById(id: number): Observable<TransactionAdmin> {
    return this.http.get<TransactionAdmin>(`${this.apiUrl}/transactions/${id}`);
  }

  searchTransactions(query: string, type?: string, status?: string): Observable<TransactionAdmin[]> {
    let params = new HttpParams().set('q', query);
    if (type) params = params.set('type', type);
    if (status) params = params.set('status', status);
    return this.http.get<TransactionAdmin[]>(`${this.apiUrl}/transactions/search`, { params });
  }

  reverseTransaction(id: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/transactions/${id}/reverse`, { reason });
  }

  getTransactionsByAccount(accountNumber: string): Observable<TransactionAdmin[]> {
    return this.http.get<TransactionAdmin[]>(`${this.apiUrl}/transactions/account/${accountNumber}`);
  }

  // ─── Loan Management ──────────────────────────────────────
  getAllLoans(page?: number, limit?: number): Observable<any> {
    let params = new HttpParams();
    if (page) params = params.set('page', page.toString());
    if (limit) params = params.set('limit', limit.toString());
    return this.http.get<any>(`${this.apiUrl}/loans/all`, { params });
  }

  getLoanById(id: number): Observable<LoanAdmin> {
    return this.http.get<LoanAdmin>(`${this.apiUrl}/loans/${id}`);
  }

  searchLoans(query: string): Observable<LoanAdmin[]> {
    return this.http.get<LoanAdmin[]>(`${this.apiUrl}/loans/search`, { params: { q: query } });
  }

  approveLoan(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/loans/${id}/approve`, {});
  }

  rejectLoan(id: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/loans/${id}/reject`, { reason });
  }

  disburseLoan(id: number, amount: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/loans/${id}/disburse`, { amount });
  }

  // ─── Reports ──────────────────────────────────────────────
  generateDailyReport(date: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/daily`, 
      { params: { date }, responseType: 'blob' });
  }

  generateMonthlyReport(month: number, year: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/monthly`, 
      { params: { month: month.toString(), year: year.toString() }, responseType: 'blob' });
  }

  generateUserReport(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/users`, { responseType: 'blob' });
  }

  // ─── Audit Logs ───────────────────────────────────────────
  getAuditLogs(page?: number, limit?: number): Observable<any> {
    let params = new HttpParams();
    if (page) params = params.set('page', page.toString());
    if (limit) params = params.set('limit', limit.toString());
    return this.http.get<any>(`${this.apiUrl}/audit-logs/all`, { params });
  }

  searchAuditLogs(query: string): Observable<AdminAuditLog[]> {
    return this.http.get<AdminAuditLog[]>(`${this.apiUrl}/audit-logs/search`, { params: { q: query } });
  }

  // ─── Security & Fraud ─────────────────────────────────────
  getFraudAlerts(page?: number, limit?: number): Observable<any> {
    let params = new HttpParams();
    if (page) params = params.set('page', page.toString());
    if (limit) params = params.set('limit', limit.toString());
    return this.http.get<any>(`${this.apiUrl}/fraud-alerts/all`, { params });
  }

  resolveFraudAlert(id: number, action: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/fraud-alerts/${id}/resolve`, { action });
  }

  getSystemLogs(page?: number, limit?: number): Observable<any> {
    let params = new HttpParams();
    if (page) params = params.set('page', page.toString());
    if (limit) params = params.set('limit', limit.toString());
    return this.http.get<any>(`${this.apiUrl}/system-logs/all`, { params });
  }

  // ─── Configuration ────────────────────────────────────────
  getSystemSettings(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/settings`);
  }

  updateSystemSettings(settings: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/settings`, settings);
  }

  // ─── Backup & Maintenance ─────────────────────────────────
  backupDatabase(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/backup/database`, {});
  }

  getBackupHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/backup/history`);
  }

  restoreBackup(backupId: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/backup/restore`, { backupId });
  }

  // ─── Login Activity ───────────────────────────────────────
  getLoginActivity(page?: number, limit?: number): Observable<any> {
    let params = new HttpParams();
    if (page) params = params.set('page', page.toString());
    if (limit) params = params.set('limit', limit.toString());
    return this.http.get<any>(`${this.apiUrl}/login-activity/all`, { params });
  }

  // ─── Account Verification ─────────────────────────────────
  verifyUserAccount(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify/account/${accountNumber}`);
  }

  verifyKYC(userId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify/kyc/${userId}`);
  }

  approveKYC(userId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/verify/kyc/${userId}/approve`, {});
  }

  rejectKYC(userId: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/verify/kyc/${userId}/reject`, { reason });
  }
}
