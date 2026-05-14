import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { SalaryAccountService } from '../../../service/salary-account.service';
import { SalaryAccount, SalaryTransaction, SalaryAccountStats } from '../../../model/salary-account/salary-account.model';
import { environment } from '../../../../environment/environment';
import { DocumentVerificationComponent } from '../document-verification/document-verification';
import { EmployeeSalaryLinkComponent } from '../employee-salary-link/employee-salary-link';
import { TimeTrackingComponent } from './time-tracking';
import { forkJoin } from 'rxjs';
import * as QRCode from 'qrcode';

interface AdminFeature {
  id: string;
  name: string;
  description: string;
  category: string;
  enabled: boolean;
  icon: string;
}

@Component({
  selector: 'app-manager-dashboard',
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
  encapsulation: ViewEncapsulation.None,
  imports: [FormsModule, CommonModule, DocumentVerificationComponent, EmployeeSalaryLinkComponent, TimeTrackingComponent],
  standalone: true
})
export class ManagerDashboard implements OnInit, OnDestroy {
  managerName: string = 'Manager';
  // Manager login/session timing
  managerLoginTimeIso: string | null = null; // ISO string from sessionStorage
  managerSessionDuration: string = ''; // Human readable duration
  activeSection: string = 'feature-access';
  sidebarCollapsed: boolean = false;

  // Sidebar Search
  sidebarSearchQuery: string = '';
  managerMenuSections: { title: string; items: { section: string; icon: string; label: string; badge?: () => number; badgeDanger?: () => number }[] }[] = [
    {
      title: 'Manager Menu',
      items: [
        { section: 'feature-access', icon: 'fa-shield-alt', label: 'Feature Access Control' },
        { section: 'create-admin', icon: 'fa-user-plus', label: 'Create Admin Employee' },
        { section: 'manager-features', icon: 'fa-tools', label: 'Manager Features', badge: () => this.managerAccessibleFeatures.length },
        { section: 'blocked-admins', icon: 'fa-lock', label: 'Blocked Admins', badge: () => this.blockedAdmins.length },
        { section: 'blocked-employees', icon: 'fa-user-lock', label: 'Blocked Employees', badgeDanger: () => this.blockedEmployees.length },
        { section: 'admin-management', icon: 'fa-user-shield', label: 'Admin Management' },
        { section: 'session-history', icon: 'fa-clock', label: 'Login/Logout History' },
        { section: 'fraud-alerts', icon: 'fa-shield-alt', label: 'Fraud Alerts', badgeDanger: () => this.fraudAlertsPendingCount },
        { section: 'loans-overview', icon: 'fa-coins', label: 'Loans & Subsidies' },
        { section: 'branch-account', icon: 'fa-university', label: 'Branch Account' },
        { section: 'attendance-salary', icon: 'fa-user-check', label: 'Attendance & Salary' },
        { section: 'salary-accounts', icon: 'fa-money-check-alt', label: 'Salary Account Opening', badge: () => this.salaryAccounts.length },
        { section: 'manager-permissions', icon: 'fa-user-lock', label: 'Manager Permissions' },
        { section: 'document-verification', icon: 'fa-file-alt', label: 'Document Verification' },
        { section: 'employee-salary-link', icon: 'fa-id-badge', label: 'Employee-Salary Link' },
        { section: 'gold-loan-management', icon: 'fa-gem', label: 'Gold Loan Management' },
        { section: 'time-tracking', icon: 'fa-id-card', label: 'Time Tracking & Management' },
        { section: 'open-account-approval', icon: 'fa-user-plus', label: 'Open Account Approval' },
        { section: 'customer-data-upload', icon: 'fa-file-excel', label: 'Customer Data Upload' },
        { section: 'cibil-reports', icon: 'fa-chart-bar', label: 'CIBIL Reports Upload', badge: () => this.cibilReports.length }
      ]
    }
  ];

  // Investments, FDs, and EMIs for manager view
  investments: any[] = [];
  fixedDeposits: any[] = [];
  allEmis: any[] = [];
  isLoadingInvestments: boolean = false;
  isLoadingFDs: boolean = false;
  isLoadingEMIs: boolean = false;
  selectedInvestment: any = null;
  selectedFD: any = null;
  selectedEMI: any = null;

  // Loan & Subsidy overview for manager
  isLoadingManagerLoans: boolean = false;
  managerLoansError: string = '';
  managerLoanOverview: any[] = [];
  filteredManagerLoanOverview: any[] = [];
  loanOverviewSearchQuery: string = '';
  loanOverviewStatusFilter: string = 'ALL';
  loanOverviewTypeFilter: string = 'ALL';

  // ─── Gold Loan Management ─────────────────────────────────
  mgrGoldLoans: any[] = [];
  mgrFilteredGoldLoans: any[] = [];
  mgrGoldLoanFilter: string = 'ALL';
  isLoadingMgrGoldLoans: boolean = false;
  mgrCurrentGoldRate: any = null;
  mgrGoldRateHistory: any[] = [];
  isLoadingGoldRateHistory: boolean = false;
  mgrNewGoldRate: number | null = null;
  isUpdatingGoldRate: boolean = false;
  mgrSelectedGoldLoan: any = null;
  mgrGoldVerification: any = { goldItems: '', goldPurity: '22K', verifiedGoldGrams: null, verificationNotes: '', storageLocation: '' };
  mgrGoldLoanEmiSchedule: any[] = [];
  isLoadingMgrEmi: boolean = false;

  // ─── Customer Data Upload (Excel) ──────────────────────────
  customerDataFile: File | null = null;
  isUploadingCustomerData: boolean = false;
  uploadedCustomerData: any[] = [];
  customerDataStats: any = { total: 0, unused: 0, used: 0, batches: [] };
  isLoadingCustomerData: boolean = false;
  customerDataUploadResult: any = null;
  customerDataSearchQuery: string = '';
  customerDataViewMode: string = 'all'; // all, unused

  // ─── CIBIL Reports Upload ──────────────────────────────────
  cibilFile: File | null = null;
  cibilUploadType: string = 'excel'; // excel, pdf, image
  isUploadingCibil: boolean = false;
  cibilReports: any[] = [];
  cibilStats: any = { total: 0, approved: 0, rejected: 0, notEligible: 0, pending: 0 };
  isLoadingCibil: boolean = false;
  cibilUploadResult: any = null;
  cibilSearchQuery: string = '';
  cibilStatusFilter: string = 'ALL';
  // Image upload manual fields
  cibilImagePan: string = '';
  cibilImageName: string = '';
  cibilImageSalary: string = '';
  cibilImageScore: string = '';
  cibilImageLimit: string = '';
  cibilImageStatus: string = '';
  cibilImageRemarks: string = '';

  // Feature Access Control
  adminFeatures: AdminFeature[] = [
    { id: 'manage-users', name: 'Manage Users', description: 'Create, view, and update users', category: 'Main Menu', enabled: true, icon: 'fa-users' },
    { id: 'user-control', name: 'Full User Control', description: 'Edit all user details & accounts', category: 'Main Menu', enabled: true, icon: 'fa-user-cog' },
    { id: 'deposit-withdraw', name: 'Deposit/Withdraw', description: 'Deposit or withdraw money for users', category: 'Financial Operations', enabled: true, icon: 'fa-exchange-alt' },
    { id: 'transactions', name: 'Transactions', description: 'View all user transactions', category: 'Financial Operations', enabled: true, icon: 'fa-chart-line' },
    { id: 'loans', name: 'Loan Requests', description: 'Approve or reject loan applications', category: 'Financial Operations', enabled: true, icon: 'fa-coins' },
    { id: 'gold-loans', name: 'Gold Loans', description: 'Approve loans, view EMI & foreclosure', category: 'Financial Operations', enabled: true, icon: 'fa-gem' },
    { id: 'cheques', name: 'Cheque Management', description: 'Draw and bounce cheques', category: 'Financial Operations', enabled: true, icon: 'fa-file-invoice' },
    { id: 'cards', name: 'Manage Cards', description: 'Issue, block, or update cards', category: 'Services', enabled: true, icon: 'fa-credit-card' },
    { id: 'kyc', name: 'KYC Verification', description: 'Approve or reject KYC requests', category: 'Services', enabled: true, icon: 'fa-id-card' },
    { id: 'subsidy-claims', name: 'Subsidy Claims', description: 'Manage education loan subsidies', category: 'Services', enabled: true, icon: 'fa-graduation-cap' },
    { id: 'education-loans', name: 'Education Loans', description: 'View & edit child details, education & college info', category: 'Services', enabled: true, icon: 'fa-university' },
    { id: 'chat', name: 'Customer Support', description: 'Chat with customers', category: 'Services', enabled: true, icon: 'fa-comments' },
    { id: 'tracking', name: 'Account Tracking', description: 'Track account creation status', category: 'Management', enabled: true, icon: 'fa-search-location' },
    { id: 'aadhar-verification', name: 'Aadhar Verification', description: 'Verify Aadhar for new accounts', category: 'Management', enabled: true, icon: 'fa-id-card' },
    { id: 'gold-rate', name: 'Gold Rate Management', description: 'Update daily gold rates', category: 'Management', enabled: true, icon: 'fa-coins' },
    { id: 'login-history', name: 'User Login History', description: 'View user login details, time, location', category: 'Management', enabled: true, icon: 'fa-history' },
    { id: 'update-history', name: 'User Update History', description: 'View admin updates and changes to user profiles', category: 'Management', enabled: true, icon: 'fa-edit' },
    { id: 'daily-report', name: 'Daily Activity Report', description: 'View and print all admin activities for the day', category: 'Management', enabled: true, icon: 'fa-calendar-day' },
    { id: 'investments', name: 'Investments Management', description: 'View and update investment details', category: 'Financial Operations', enabled: true, icon: 'fa-chart-line' },
    { id: 'fixed-deposits', name: 'Fixed Deposits Management', description: 'View and update FD details', category: 'Financial Operations', enabled: true, icon: 'fa-piggy-bank' },
    { id: 'emi-management', name: 'EMI Management', description: 'View and monitor all EMIs', category: 'Financial Operations', enabled: true, icon: 'fa-calendar-check' }
  ];

  filteredFeatures: AdminFeature[] = [];
  featureFilter: string = 'ALL';
  featureSearchQuery: string = '';

  // Admin List and Selection
  allAdmins: any[] = [];
  selectedAdmin: any = null;
  isLoadingAdmins: boolean = false;
  adminFeatureAccess: Map<string, Map<string, boolean>> = new Map(); // adminEmail -> featureId -> enabled

  // Admin profile update requests (submitted by admin employees, approved by manager)
  adminProfileUpdateRequests: any[] = [];
  isLoadingAdminProfileUpdates: boolean = false;
  expandedAdminProfileRequestId: number | null = null;

  // ID card management (for selected admin)
  isGeneratingIdCard: boolean = false;
  isUpdatingIdCard: boolean = false;
  isUploadingIdCardPhoto: boolean = false;
  idCardPhotoCacheBuster: number = Date.now();
  idCardDesignationInput: string = '';
  idCardDepartmentInput: string = '';
  idCardLookupNumber: string = '';
  idCardLookupResult: any = null;
  idCardLookupError: string = '';
  isPrintingIdCard: boolean = false;

  // Manager signature (drawn during generate)
  showIdCardSignatureModal: boolean = false;
  isSavingIdCardSignature: boolean = false;
  private signatureCtx: CanvasRenderingContext2D | null = null;
  private signatureIsDrawing: boolean = false;
  private signatureHasStroke: boolean = false;

  // Blocked Admins Management
  blockedAdmins: any[] = [];
  isLoadingBlockedAdmins: boolean = false;
  selectedBlockedAdmin: any = null;
  showResetPasswordModal: boolean = false;
  newPassword: string = '';
  confirmPassword: string = '';
  isResettingPassword: boolean = false;
  isUnblocking: boolean = false;

  // Blocked Employees (Salary Accounts) Management
  blockedEmployees: SalaryAccount[] = [];
  isLoadingBlockedEmployees: boolean = false;
  isUnblockingEmployee: boolean = false;
  showEmployeeResetPasswordModal: boolean = false;
  selectedBlockedEmployee: SalaryAccount | null = null;
  employeeNewPassword: string = '';
  employeeConfirmPassword: string = '';
  isResettingEmployeePassword: boolean = false;
  
  // Password reset for any admin (not just blocked)
  showAdminPasswordResetModal: boolean = false;
  selectedAdminForPasswordReset: any = null;

  // Manager Feature Access (features disabled for admins that manager can use)
  managerAccessibleFeatures: AdminFeature[] = [];
  filteredManagerFeatures: AdminFeature[] = [];
  managerFeatureFilter: string = 'ALL';
  managerFeatureSearchQuery: string = '';

  // Branch Account (deposit account for all charges and interest)
  branchAccountSummary: any = null;
  branchAccountNumber: string = '';
  branchAccountName: string = '';
  branchAccountIfsc: string = '';
  isLoadingBranchAccount: boolean = false;
  isSavingBranchAccount: boolean = false;
  // Branch account transactions (opened bank account)
  branchAccountTransactions: any[] = [];
  branchTxnsFromDate: string = '';
  branchTxnsToDate: string = '';
  branchTxnsSearch: string = '';
  branchTxnsPage: number = 0;
  branchTxnsSize: number = 20;
  branchTxnsTotalElements: number = 0;
  branchTxnsTotalPages: number = 0;
  isLoadingBranchTxns: boolean = false;

  // Attendance & Salary
  attendanceDate: string = new Date().toISOString().substring(0, 10);
  attendanceIdCardNumber: string = '';
  attendanceSummaryRows: any[] = [];
  isLoadingAttendance: boolean = false;
  isVerifyingAttendance: boolean = false;
  isPayingSalary: boolean = false;

  // Attendance detail (per admin, full year)
  attendanceAdminDetail: any = null;
  attendanceCalendarYear: number = new Date().getFullYear();
  attendanceCalendarRows: any[] = [];
  attendanceCalendarLoading: boolean = false;
  attendanceCalendarSelected: { [date: string]: boolean } = {};
  attendanceCalendarByMonth: { [month: number]: any[] } = {};
  attendanceMonthNumbers: number[] = [1,2,3,4,5,6,7,8,9,10,11,12];
  attendanceMonthNames: string[] = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  
  // Session History
  sessionHistory: any[] = [];
  isLoadingSessionHistory: boolean = false;
  sessionHistoryInitialLoad: boolean = true;
  sessionHistoryTimer: any = null;
  sessionHistoryFilter: string = 'ALL'; // ALL, USER, ADMIN
  sessionHistorySearchQuery: string = '';
  sessionHistoryTab: string = 'all'; // all, active, previous
  sessionTypeTab: string = 'all'; // all, users, admins
  // Login Activity - Account Type Tabs
  loginAccountTypeTab: string = 'all'; // all, admin, manager, savings, current, salary
  loginActivityRealTimeTimer: any = null;

  // Cached computed values to prevent flickering
  cachedFilteredLoginActivity: any[] = [];
  cachedDownloadFilteredData: any[] = [];
  cachedActiveCount: number = 0;
  cachedPreviousCount: number = 0;
  cachedAccountTypeCounts: { [key: string]: number } = {};

  // Download Filters
  showDownloadPanel: boolean = false;
  downloadFilterFromDate: string = '';
  downloadFilterToDate: string = '';
  downloadFilterAccountType: string = 'all';
  downloadFilterStatus: string = 'all';
  downloadFilterFromTime: string = '';
  downloadFilterToTime: string = '';

  // Fraud Alerts (AI Fraud Detection)
  fraudAlerts: any[] = [];
  fraudAlertsPendingCount: number = 0;
  isLoadingFraudAlerts: boolean = false;
  fraudAlertStatusFilter: string = '';
  fraudAlertTypeFilter: string = '';
  fraudAlertSourceFilter: string = '';

  // Create Admin Employee
  newAdmin: any = {
    name: '',
    email: '',
    password: '',
    role: 'ADMIN',
    employeeId: '',
    profileComplete: false
  };
  isCreatingAdmin: boolean = false;
  adminCreationSuccess: boolean = false;
  adminCreationError: string = '';

  // Add Existing Admin feature state
  searchExistingQuery: string = '';
  isSearchingExisting: boolean = false;
  existingAdminResult: any = null;
  existingAdminError: string = '';
  isAddingExistingAdmin: boolean = false;

  // ─── Salary Account Opening ─────────────────────────────
  salaryAccounts: SalaryAccount[] = [];
  filteredSalaryAccounts: SalaryAccount[] = [];
  salaryStats: SalaryAccountStats = { totalAccounts: 0, activeAccounts: 0, frozenAccounts: 0, closedAccounts: 0, totalMonthlySalary: 0, companiesLinked: {}, totalCompanies: 0 };
  salarySearchQuery: string = '';
  salaryStatusFilter: string = 'ALL';
  showSalaryForm: boolean = false;
  isLoadingSalaryAccounts: boolean = false;
  isCreatingSalaryAccount: boolean = false;
  isSavingSalaryAccount: boolean = false;
  isLoadingSalaryTxns: boolean = false;
  selectedSalaryAccount: SalaryAccount | null = null;
  editingSalaryAccount: SalaryAccount | null = null;
  salaryTransactions: SalaryTransaction[] = [];
  newSalaryAccount: SalaryAccount = this.getEmptySalaryAccount();

  // ─── Close Account ──────────────────────────────────────
  closingAccount: SalaryAccount | null = null;
  closeAccountPreCheck: any = null;
  isLoadingClosePreCheck: boolean = false;
  isClosingAccount: boolean = false;
  closeAccountReason: string = '';

  // ─── Manager Permissions (Admin Salary Account Linking & Monitoring) ─────
  mpSelectedAdmin: any = null;
  mpAdminSalaryAccount: SalaryAccount | null = null;
  mpSalaryTransactions: any[] = [];
  mpNormalTransactions: any[] = [];
  mpSalaryReport: any = null;
  mpLinkAccountNumber: string = '';
  mpIsLinking: boolean = false;
  mpIsLoadingSalary: boolean = false;
  mpIsLoadingTxns: boolean = false;
  mpIsLoadingReport: boolean = false;
  mpActiveTab: string = 'overview'; // overview | transactions | report
  mpTxnTab: string = 'salary'; // salary | normal
  mpLinked: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService,
    private salaryAccountService: SalaryAccountService
  ) {}

  private sessionTimerHandle: any = null;

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadManagerInfo();
      this.loadManagerSessionTiming();
      this.loadAllAdmins();
      this.loadBlockedAdmins();
      this.loadBlockedEmployees();
      this.loadFeatureAccess();
      this.filterFeatures(); // Initialize filtered features
      this.loadManagerAccessibleFeatures(); // Load features manager can use
      this.checkAndCreateDefaultManager();
      this.loadSessionHistory(); // Load session history
      this.http.get<{ success: boolean; pendingCount: number }>(`${environment.apiBaseUrl}/api/fraud-alerts/count-pending`).subscribe({
        next: (res) => { if (res.pendingCount !== undefined) this.fraudAlertsPendingCount = res.pendingCount; }
      });
    }
    // Start updating session duration every 5 seconds
    if (isPlatformBrowser(this.platformId)) {
      this.sessionTimerHandle = setInterval(() => {
        this.loadManagerSessionTiming();
      }, 5000);
    }
    // Start updating session history every 5 seconds (real-time)
    if (isPlatformBrowser(this.platformId)) {
      this.sessionHistoryTimer = setInterval(() => {
        this.loadSessionHistory();
      }, 5000);
      // Real-time 1-second timer to update active session durations live
      this.loginActivityRealTimeTimer = setInterval(() => {
        this.refreshUsedTimes();
      }, 1000);
    }
  }

  ngOnDestroy(): void {
    if (this.sessionTimerHandle) {
      clearInterval(this.sessionTimerHandle);
      this.sessionTimerHandle = null;
    }
    if (this.sessionHistoryTimer) {
      clearInterval(this.sessionHistoryTimer);
      this.sessionHistoryTimer = null;
    }
    if (this.loginActivityRealTimeTimer) {
      clearInterval(this.loginActivityRealTimeTimer);
      this.loginActivityRealTimeTimer = null;
    }
  }
  
  /**
   * Load session history (login/logout for all users and admins)
   */
  loadSessionHistory(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    
    if (this.sessionHistoryInitialLoad) {
      this.isLoadingSessionHistory = true;
    }
    this.http.get<any>(`${environment.apiBaseUrl}/api/session-history/all`).subscribe({
      next: (response) => {
        if (response && response.success) {
          this.sessionHistory = response.sessions || [];
          this.isLoadingSessionHistory = false;
          this.sessionHistoryInitialLoad = false;
          this.rebuildCachedValues();
        } else {
          this.sessionHistory = [];
          this.isLoadingSessionHistory = false;
          this.sessionHistoryInitialLoad = false;
          this.rebuildCachedValues();
        }
      },
      error: (err) => {
        console.error('Error loading session history:', err);
        this.isLoadingSessionHistory = false;
        this.sessionHistoryInitialLoad = false;
        this.sessionHistory = [];
        this.rebuildCachedValues();
      }
    });
  }
  
  /**
   * Get filtered session history based on active tab
   */
  getFilteredSessionHistory(): any[] {
    let filtered = this.sessionHistory;
    
    // Filter by session status tab (active, previous, all)
    if (this.sessionHistoryTab === 'active') {
      filtered = filtered.filter(s => s.status === 'ACTIVE' && !s.logoutTime);
    } else if (this.sessionHistoryTab === 'previous') {
      filtered = filtered.filter(s => s.status === 'LOGGED_OUT' || s.logoutTime);
    }
    
    // Filter by user type tab
    if (this.sessionTypeTab === 'users') {
      filtered = filtered.filter(s => s.userType === 'USER');
    } else if (this.sessionTypeTab === 'admins') {
      // Include both ADMIN and MANAGER when filtering for admins
      filtered = filtered.filter(s => s.userType === 'ADMIN' || s.userType === 'MANAGER');
    }
    
    // Filter by user type (additional filter)
    if (this.sessionHistoryFilter !== 'ALL') {
      filtered = filtered.filter(s => s.userType === this.sessionHistoryFilter);
    }
    
    // Filter by search query
    if (this.sessionHistorySearchQuery) {
      const query = this.sessionHistorySearchQuery.toLowerCase();
      filtered = filtered.filter(s => 
        (s.email && s.email.toLowerCase().includes(query)) ||
        (s.username && s.username.toLowerCase().includes(query)) ||
        (s.accountNumber && s.accountNumber.toLowerCase().includes(query)) ||
        (s.ipAddress && s.ipAddress.toLowerCase().includes(query))
      );
    }
    
    return filtered;
  }
  
  /**
   * Get active sessions count
   */
  getActiveSessionsCount(): number {
    return this.cachedActiveCount;
  }
  
  /**
   * Get previous sessions count (same as completed)
   */
  getPreviousSessionsCount(): number {
    return this.cachedPreviousCount;
  }
  
  /**
   * Get user sessions count
   */
  getUserSessionsCount(): number {
    return this.sessionHistory.filter(s => s.userType === 'USER').length;
  }
  
  /**
   * Get admin sessions count (includes both ADMIN and MANAGER)
   */
  getAdminSessionsCount(): number {
    return this.sessionHistory.filter(s => s.userType === 'ADMIN' || s.userType === 'MANAGER').length;
  }

  /**
   * Get session count by account type
   */
  getAccountTypeCount(accountType: string): number {
    return this.cachedAccountTypeCounts[accountType] || 0;
  }

  /**
   * Get filtered session history based on all active filters including account type tab
   */
  getFilteredLoginActivity(): any[] {
    return this.cachedFilteredLoginActivity;
  }

  trackBySessionId(index: number, session: any): number {
    return session.id;
  }

  /**
   * Rebuild all cached computed values - called when data or filters change
   */
  rebuildCachedValues(): void {
    // Counts
    this.cachedActiveCount = this.sessionHistory.filter(s => s.status === 'ACTIVE' && !s.logoutTime).length;
    this.cachedPreviousCount = this.sessionHistory.filter(s => s.status === 'LOGGED_OUT' || s.logoutTime).length;
    this.cachedAccountTypeCounts = {
      all: this.sessionHistory.length,
      admin: this.sessionHistory.filter(s => s.accountType === 'ADMIN' || (s.userType === 'ADMIN' && !s.accountType)).length,
      manager: this.sessionHistory.filter(s => s.accountType === 'MANAGER' || (s.userType === 'MANAGER' && !s.accountType)).length,
      savings: this.sessionHistory.filter(s => s.accountType === 'SAVINGS' || (s.userType === 'USER' && !s.accountType)).length,
      current: this.sessionHistory.filter(s => s.accountType === 'CURRENT').length,
      salary: this.sessionHistory.filter(s => s.accountType === 'SALARY').length
    };

    // Filtered login activity
    let filtered = this.sessionHistory;

    if (this.loginAccountTypeTab !== 'all') {
      if (this.loginAccountTypeTab === 'admin') {
        filtered = filtered.filter(s => s.accountType === 'ADMIN' || (s.userType === 'ADMIN' && !s.accountType));
      } else if (this.loginAccountTypeTab === 'manager') {
        filtered = filtered.filter(s => s.accountType === 'MANAGER' || (s.userType === 'MANAGER' && !s.accountType));
      } else if (this.loginAccountTypeTab === 'savings') {
        filtered = filtered.filter(s => s.accountType === 'SAVINGS' || (s.userType === 'USER' && !s.accountType));
      } else if (this.loginAccountTypeTab === 'current') {
        filtered = filtered.filter(s => s.accountType === 'CURRENT');
      } else if (this.loginAccountTypeTab === 'salary') {
        filtered = filtered.filter(s => s.accountType === 'SALARY');
      }
    }

    if (this.sessionHistoryTab === 'active') {
      filtered = filtered.filter(s => s.status === 'ACTIVE' && !s.logoutTime);
    } else if (this.sessionHistoryTab === 'previous') {
      filtered = filtered.filter(s => s.status === 'LOGGED_OUT' || s.logoutTime);
    }

    if (this.sessionHistorySearchQuery) {
      const query = this.sessionHistorySearchQuery.toLowerCase();
      filtered = filtered.filter(s =>
        (s.email && s.email.toLowerCase().includes(query)) ||
        (s.username && s.username.toLowerCase().includes(query)) ||
        (s.accountNumber && s.accountNumber.toLowerCase().includes(query)) ||
        (s.ipAddress && s.ipAddress.toLowerCase().includes(query)) ||
        (s.browserName && s.browserName.toLowerCase().includes(query))
      );
    }

    this.cachedFilteredLoginActivity = filtered;

    // Also rebuild download filtered data
    this.rebuildDownloadFilteredData();

    // Refresh used times for active sessions
    this.refreshUsedTimes();
  }

  /**
   * Refresh only the _usedTime field on active sessions (called every 1 second)
   */
  refreshUsedTimes(): void {
    for (const s of this.cachedFilteredLoginActivity) {
      s._usedTime = this.computeUsedTime(s);
    }
  }

  /**
   * Called when any filter (tab, search, etc) changes from the template
   */
  onFilterChange(): void {
    this.rebuildCachedValues();
  }

  /**
   * Get browser icon class from browser name
   */
  getBrowserIcon(browserName: string): string {
    if (!browserName) return 'fa-globe';
    const b = browserName.toLowerCase();
    if (b.includes('chrome')) return 'fa-chrome';
    if (b.includes('firefox')) return 'fa-firefox';
    if (b.includes('safari')) return 'fa-safari';
    if (b.includes('edge')) return 'fa-edge';
    if (b.includes('opera')) return 'fa-opera';
    if (b.includes('ie')) return 'fa-internet-explorer';
    return 'fa-globe';
  }

  /**
   * Get account type label
   */
  getAccountTypeLabel(session: any): string {
    if (session.accountType) {
      const labels: any = { ADMIN: 'Admin', MANAGER: 'Manager', SAVINGS: 'Savings', CURRENT: 'Current', SALARY: 'Salary' };
      return labels[session.accountType] || session.accountType;
    }
    if (session.userType === 'ADMIN') return 'Admin';
    if (session.userType === 'MANAGER') return 'Manager';
    return 'Savings';
  }

  /**
   * Calculate used time for a session (from login to logout or now)
   */
  getUsedTime(session: any): string {
    return session._usedTime || this.computeUsedTime(session);
  }

  computeUsedTime(session: any): string {
    if (!session.loginTime) return '00:00:00';
    try {
      const login = new Date(session.loginTime);
      const isLoggedOut = session.logoutTime || session.status === 'LOGGED_OUT';
      const end = isLoggedOut && session.logoutTime ? new Date(session.logoutTime) : (isLoggedOut ? login : new Date());
      const diff = end.getTime() - login.getTime();
      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);
      return String(hours).padStart(2, '0') + ':' + String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
    } catch (e) {
      return '00:00:00';
    }
  }
  
  /**
   * Format date and time for display
   */
  formatSessionDateTime(dateTime: string): string {
    if (!dateTime) return 'N/A';
    try {
      const date = new Date(dateTime);
      return date.toLocaleString('en-IN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
      });
    } catch (e) {
      return dateTime;
    }
  }
  
  /**
   * Format date only for display
   */
  formatSessionDate(dateTime: string): string {
    if (!dateTime) return 'N/A';
    try {
      const date = new Date(dateTime);
      return date.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch (e) {
      return 'N/A';
    }
  }
  
  /**
   * Format time only for display
   */
  formatSessionTime(dateTime: string): string {
    if (!dateTime) return 'N/A';
    try {
      const date = new Date(dateTime);
      return date.toLocaleTimeString('en-IN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
      });
    } catch (e) {
      return 'N/A';
    }
  }
  
  /**
   * Get icon for login method
   */
  // ==================== Download Filter Methods ====================

  getDownloadFilteredData(): any[] {
    return this.cachedDownloadFilteredData;
  }

  rebuildDownloadFilteredData(): void {
    let filtered = this.sessionHistory;

    // Account type filter
    if (this.downloadFilterAccountType !== 'all') {
      const typeMap: any = { admin: 'ADMIN', manager: 'MANAGER', savings: 'SAVINGS', current: 'CURRENT', salary: 'SALARY' };
      const target = typeMap[this.downloadFilterAccountType];
      filtered = filtered.filter(s => {
        if (s.accountType) return s.accountType === target;
        if (target === 'ADMIN') return s.userType === 'ADMIN';
        if (target === 'MANAGER') return s.userType === 'MANAGER';
        return s.userType === 'USER';
      });
    }

    // Status filter
    if (this.downloadFilterStatus === 'active') {
      filtered = filtered.filter(s => s.status === 'ACTIVE' && !s.logoutTime);
    } else if (this.downloadFilterStatus === 'logged_out') {
      filtered = filtered.filter(s => s.status === 'LOGGED_OUT' || s.logoutTime);
    }

    // Date range filter
    if (this.downloadFilterFromDate) {
      const from = new Date(this.downloadFilterFromDate);
      from.setHours(0, 0, 0, 0);
      filtered = filtered.filter(s => s.loginTime && new Date(s.loginTime) >= from);
    }
    if (this.downloadFilterToDate) {
      const to = new Date(this.downloadFilterToDate);
      to.setHours(23, 59, 59, 999);
      filtered = filtered.filter(s => s.loginTime && new Date(s.loginTime) <= to);
    }

    // Time range filter
    if (this.downloadFilterFromTime) {
      const [fh, fm] = this.downloadFilterFromTime.split(':').map(Number);
      filtered = filtered.filter(s => {
        if (!s.loginTime) return false;
        const d = new Date(s.loginTime);
        return d.getHours() > fh || (d.getHours() === fh && d.getMinutes() >= fm);
      });
    }
    if (this.downloadFilterToTime) {
      const [th, tm] = this.downloadFilterToTime.split(':').map(Number);
      filtered = filtered.filter(s => {
        if (!s.loginTime) return false;
        const d = new Date(s.loginTime);
        return d.getHours() < th || (d.getHours() === th && d.getMinutes() <= tm);
      });
    }

    this.cachedDownloadFilteredData = filtered;
  }

  clearDownloadFilters(): void {
    this.downloadFilterFromDate = '';
    this.downloadFilterToDate = '';
    this.downloadFilterAccountType = 'all';
    this.downloadFilterStatus = 'all';
    this.downloadFilterFromTime = '';
    this.downloadFilterToTime = '';
    this.rebuildDownloadFilteredData();
  }

  downloadPDF(): void {
    const data = this.getDownloadFilteredData();
    if (data.length === 0) return;

    const title = 'Login/Logout History Report';
    const generatedAt = new Date().toLocaleString('en-IN');
    const filterInfo = this.getFilterSummaryText();

    let html = `
      <html><head><title>${title}</title>
      <style>
        body { font-family: Arial, sans-serif; margin: 20px; color: #333; }
        h1 { color: #1e3a5f; font-size: 20px; border-bottom: 2px solid #1e3a5f; padding-bottom: 8px; }
        .meta { font-size: 12px; color: #666; margin-bottom: 16px; }
        .meta span { margin-right: 20px; }
        table { width: 100%; border-collapse: collapse; font-size: 11px; }
        th { background: #1e3a5f; color: #fff; padding: 8px 6px; text-align: left; }
        td { padding: 6px; border-bottom: 1px solid #e0e0e0; }
        tr:nth-child(even) { background: #f9f9f9; }
        .active-status { color: #059669; font-weight: bold; }
        .logout-status { color: #dc2626; }
        .footer { margin-top: 20px; font-size: 10px; color: #999; text-align: center; border-top: 1px solid #e0e0e0; padding-top: 8px; }
      </style></head><body>
      <h1>${title}</h1>
      <div class="meta">
        <span><strong>Generated:</strong> ${generatedAt}</span>
        <span><strong>Total Records:</strong> ${data.length}</span>
        <span><strong>Filters:</strong> ${filterInfo}</span>
      </div>
      <table>
        <tr><th>#</th><th>Account Type</th><th>Email</th><th>Username</th><th>Account No</th><th>Browser</th><th>Login Time</th><th>Logout Time</th><th>Used Time</th><th>Method</th><th>IP Address</th><th>Status</th></tr>`;

    data.forEach((s, i) => {
      const statusClass = s.status === 'ACTIVE' ? 'active-status' : 'logout-status';
      html += `<tr>
        <td>${i + 1}</td>
        <td>${this.getAccountTypeLabel(s)}</td>
        <td>${s.email || 'N/A'}</td>
        <td>${s.username || 'N/A'}</td>
        <td>${s.accountNumber || 'N/A'}</td>
        <td>${s.browserName || 'Unknown'}</td>
        <td>${s.loginTime ? this.formatSessionDateTime(s.loginTime) : 'N/A'}</td>
        <td>${s.logoutTime ? this.formatSessionDateTime(s.logoutTime) : 'Still Active'}</td>
        <td>${this.getUsedTime(s)}</td>
        <td>${s.loginMethod || 'PASSWORD'}</td>
        <td>${s.ipAddress || 'Unknown'}</td>
        <td class="${statusClass}">${s.status || 'ACTIVE'}</td>
      </tr>`;
    });

    html += `</table><div class="footer">NeoBank Manager Dashboard &mdash; Login Activity Report &mdash; ${generatedAt}</div></body></html>`;

    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(html);
      printWindow.document.close();
      printWindow.focus();
      setTimeout(() => { printWindow.print(); }, 500);
    }
  }

  downloadXML(): void {
    const data = this.getDownloadFilteredData();
    if (data.length === 0) return;

    const escapeXml = (str: string) => {
      if (!str) return '';
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
    };

    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    xml += '<LoginLogoutHistory>\n';
    xml += '  <ReportInfo>\n';
    xml += `    <GeneratedAt>${new Date().toISOString()}</GeneratedAt>\n`;
    xml += `    <TotalRecords>${data.length}</TotalRecords>\n`;
    xml += `    <Filters>${escapeXml(this.getFilterSummaryText())}</Filters>\n`;
    xml += '  </ReportInfo>\n';
    xml += '  <Sessions>\n';

    data.forEach((s, i) => {
      xml += '    <Session>\n';
      xml += `      <SerialNo>${i + 1}</SerialNo>\n`;
      xml += `      <AccountType>${escapeXml(this.getAccountTypeLabel(s))}</AccountType>\n`;
      xml += `      <Email>${escapeXml(s.email || 'N/A')}</Email>\n`;
      xml += `      <Username>${escapeXml(s.username || 'N/A')}</Username>\n`;
      xml += `      <AccountNumber>${escapeXml(s.accountNumber || 'N/A')}</AccountNumber>\n`;
      xml += `      <Browser>${escapeXml(s.browserName || 'Unknown')}</Browser>\n`;
      xml += `      <LoginTime>${s.loginTime || 'N/A'}</LoginTime>\n`;
      xml += `      <LogoutTime>${s.logoutTime || 'Still Active'}</LogoutTime>\n`;
      xml += `      <UsedTime>${this.getUsedTime(s)}</UsedTime>\n`;
      xml += `      <LoginMethod>${escapeXml(s.loginMethod || 'PASSWORD')}</LoginMethod>\n`;
      xml += `      <IPAddress>${escapeXml(s.ipAddress || 'Unknown')}</IPAddress>\n`;
      xml += `      <Status>${escapeXml(s.status || 'ACTIVE')}</Status>\n`;
      xml += '    </Session>\n';
    });

    xml += '  </Sessions>\n';
    xml += '</LoginLogoutHistory>';

    const blob = new Blob([xml], { type: 'application/xml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `login-logout-history-${new Date().toISOString().slice(0, 10)}.xml`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  getFilterSummaryText(): string {
    const parts: string[] = [];
    if (this.downloadFilterAccountType !== 'all') parts.push('Account: ' + this.downloadFilterAccountType);
    if (this.downloadFilterStatus !== 'all') parts.push('Status: ' + this.downloadFilterStatus);
    if (this.downloadFilterFromDate) parts.push('From: ' + this.downloadFilterFromDate);
    if (this.downloadFilterToDate) parts.push('To: ' + this.downloadFilterToDate);
    if (this.downloadFilterFromTime) parts.push('Time from: ' + this.downloadFilterFromTime);
    if (this.downloadFilterToTime) parts.push('Time to: ' + this.downloadFilterToTime);
    return parts.length > 0 ? parts.join(' | ') : 'No filters applied';
  }

  getLoginMethodIcon(loginMethod: string): string {
    if (!loginMethod) return 'fa-key';
    const method = loginMethod.toUpperCase();
    if (method.includes('PASSWORD')) return 'fa-key';
    if (method.includes('GRAPHICAL')) return 'fa-draw-polygon';
    if (method.includes('OTP')) return 'fa-sms';
    if (method.includes('QR')) return 'fa-qrcode';
    if (method.includes('FINGERPRINT')) return 'fa-fingerprint';
    return 'fa-key';
  }
  
  /**
   * Calculate active session duration in real-time
   */
  calculateActiveSessionDuration(loginTime: string): string {
    if (!loginTime) return '00:00:00';
    try {
      const login = new Date(loginTime);
      const now = new Date();
      const diff = now.getTime() - login.getTime();
      
      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);
      
      return String(hours).padStart(2, '0') + ':' +
             String(minutes).padStart(2, '0') + ':' +
             String(seconds).padStart(2, '0');
    } catch (e) {
      return '00:00:00';
    }
  }

  loadManagerSessionTiming() {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      const iso = sessionStorage.getItem('adminLoginTime');
      if (iso) {
        this.managerLoginTimeIso = iso;
        const loginDate = new Date(iso);
        this.managerSessionDuration = this.formatDuration(Date.now() - loginDate.getTime());
      }
    } catch (e) {
      console.error('Error reading adminLoginTime from sessionStorage', e);
    }
  }

  // Format milliseconds to human readable HH:MM:SS or Xm Ys
  formatDuration(ms: number): string {
    if (ms <= 0) return '0s';
    const totalSec = Math.floor(ms / 1000);
    const hours = Math.floor(totalSec / 3600);
    const minutes = Math.floor((totalSec % 3600) / 60);
    const seconds = totalSec % 60;
    if (hours > 0) return `${hours}h ${minutes}m ${seconds}s`;
    if (minutes > 0) return `${minutes}m ${seconds}s`;
    return `${seconds}s`;
  }

  loadBlockedAdmins() {
    this.isLoadingBlockedAdmins = true;
    this.http.get(`${environment.apiBaseUrl}/api/admins/blocked`).subscribe({
      next: (admins: any) => {
        this.blockedAdmins = admins || [];
        this.isLoadingBlockedAdmins = false;
        console.log('Loaded blocked admins:', this.blockedAdmins);
      },
      error: (err: any) => {
        console.error('Error loading blocked admins:', err);
        this.isLoadingBlockedAdmins = false;
      }
    });
  }

  unblockAdmin(admin: any) {
    if (!confirm(`Are you sure you want to unblock ${admin.name || admin.email}?`)) {
      return;
    }

    this.isUnblocking = true;
    // Ensure ID is a number
    const adminId = typeof admin.id === 'number' ? admin.id : Number(admin.id);
    if (isNaN(adminId)) {
      this.alertService.error('Validation Error', 'Invalid admin ID');
      this.isUnblocking = false;
      return;
    }
    this.http.post(`${environment.apiBaseUrl}/api/admins/unblock/${adminId}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', `Admin ${admin.name || admin.email} has been unblocked successfully`);
          this.loadBlockedAdmins();
          this.loadAllAdmins(); // Refresh admin list
        } else {
          this.alertService.error('Error', response.message || 'Failed to unblock admin');
        }
        this.isUnblocking = false;
      },
      error: (err: any) => {
        console.error('Error unblocking admin:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to unblock admin');
        this.isUnblocking = false;
    }
    });
  }

  openResetPasswordModal(admin: any) {
    this.selectedBlockedAdmin = admin;
    this.newPassword = '';
    this.confirmPassword = '';
    this.showResetPasswordModal = true;
  }

  closeResetPasswordModal() {
    this.showResetPasswordModal = false;
    this.selectedBlockedAdmin = null;
    this.newPassword = '';
    this.confirmPassword = '';
  }

  openAdminPasswordResetModal(admin: any) {
    this.selectedAdminForPasswordReset = admin;
    this.newPassword = '';
    this.confirmPassword = '';
    this.showAdminPasswordResetModal = true;
  }

  closeAdminPasswordResetModal() {
    this.showAdminPasswordResetModal = false;
    this.selectedAdminForPasswordReset = null;
    this.newPassword = '';
    this.confirmPassword = '';
  }

  resetAnyAdminPassword() {
    if (!this.selectedAdminForPasswordReset) return;

    if (!this.newPassword || this.newPassword.length < 6) {
      this.alertService.error('Validation Error', 'Password must be at least 6 characters');
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.alertService.error('Validation Error', 'Passwords do not match');
      return;
    }

    this.isResettingPassword = true;
    // Ensure ID is a number
    const adminId = typeof this.selectedAdminForPasswordReset.id === 'number' 
      ? this.selectedAdminForPasswordReset.id 
      : Number(this.selectedAdminForPasswordReset.id);
    if (isNaN(adminId)) {
      this.alertService.error('Validation Error', 'Invalid admin ID');
      this.isResettingPassword = false;
      return;
    }
    this.http.post(`${environment.apiBaseUrl}/api/admins/reset-password/${adminId}`, {
      newPassword: this.newPassword
    }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', `Password reset successfully for ${this.selectedAdminForPasswordReset.name || this.selectedAdminForPasswordReset.email}`);
          this.closeAdminPasswordResetModal();
          this.loadAllAdmins(); // Refresh admin list
        } else {
          this.alertService.error('Error', response.message || 'Failed to reset password');
        }
        this.isResettingPassword = false;
      },
      error: (err: any) => {
        console.error('Error resetting password:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reset password');
        this.isResettingPassword = false;
      }
    });
  }

  resetAdminPassword() {
    if (!this.selectedBlockedAdmin) return;

    if (!this.newPassword || this.newPassword.length < 6) {
      this.alertService.error('Validation Error', 'Password must be at least 6 characters');
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.alertService.error('Validation Error', 'Passwords do not match');
      return;
    }

    this.isResettingPassword = true;
    // Ensure ID is a number
    const adminId = typeof this.selectedBlockedAdmin.id === 'number' 
      ? this.selectedBlockedAdmin.id 
      : Number(this.selectedBlockedAdmin.id);
    if (isNaN(adminId)) {
      this.alertService.error('Validation Error', 'Invalid admin ID');
      this.isResettingPassword = false;
      return;
    }
    this.http.post(`${environment.apiBaseUrl}/api/admins/reset-password/${adminId}`, {
      newPassword: this.newPassword
    }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', `Password reset successfully for ${this.selectedBlockedAdmin.name || this.selectedBlockedAdmin.email}`);
          this.closeResetPasswordModal();
          this.loadBlockedAdmins();
          this.loadAllAdmins(); // Refresh admin list
        } else {
          this.alertService.error('Error', response.message || 'Failed to reset password');
        }
        this.isResettingPassword = false;
      },
      error: (err: any) => {
        console.error('Error resetting password:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reset password');
        this.isResettingPassword = false;
      }
    });
  }

  // ─── Blocked Employees (Salary Accounts) Management ────────

  loadBlockedEmployees() {
    this.isLoadingBlockedEmployees = true;
    this.salaryAccountService.getBlockedAccounts().subscribe({
      next: (accounts: SalaryAccount[]) => {
        this.blockedEmployees = accounts || [];
        this.isLoadingBlockedEmployees = false;
      },
      error: (err: any) => {
        console.error('Error loading blocked employees:', err);
        this.isLoadingBlockedEmployees = false;
      }
    });
  }

  unblockEmployee(employee: SalaryAccount) {
    if (!confirm(`Are you sure you want to unblock ${employee.employeeName || employee.accountNumber}?`)) {
      return;
    }
    this.isUnblockingEmployee = true;
    this.salaryAccountService.unblockAccount(employee.id!).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', `Employee ${employee.employeeName || employee.accountNumber} has been unblocked successfully`);
          this.loadBlockedEmployees();
        } else {
          this.alertService.error('Error', response.message || 'Failed to unblock employee');
        }
        this.isUnblockingEmployee = false;
      },
      error: (err: any) => {
        console.error('Error unblocking employee:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to unblock employee');
        this.isUnblockingEmployee = false;
      }
    });
  }

  openEmployeeResetPasswordModal(employee: SalaryAccount) {
    this.selectedBlockedEmployee = employee;
    this.employeeNewPassword = '';
    this.employeeConfirmPassword = '';
    this.showEmployeeResetPasswordModal = true;
  }

  closeEmployeeResetPasswordModal() {
    this.showEmployeeResetPasswordModal = false;
    this.selectedBlockedEmployee = null;
    this.employeeNewPassword = '';
    this.employeeConfirmPassword = '';
  }

  resetEmployeePassword() {
    if (!this.selectedBlockedEmployee) return;
    if (!this.employeeNewPassword || this.employeeNewPassword.length < 6) {
      this.alertService.error('Validation Error', 'Password must be at least 6 characters');
      return;
    }
    if (this.employeeNewPassword !== this.employeeConfirmPassword) {
      this.alertService.error('Validation Error', 'Passwords do not match');
      return;
    }
    this.isResettingEmployeePassword = true;
    this.salaryAccountService.resetAccountPassword(this.selectedBlockedEmployee.id!, this.employeeNewPassword).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', `Password reset successfully for ${this.selectedBlockedEmployee?.employeeName || 'Employee'}`);
          this.closeEmployeeResetPasswordModal();
          this.loadBlockedEmployees();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reset password');
        }
        this.isResettingEmployeePassword = false;
      },
      error: (err: any) => {
        console.error('Error resetting employee password:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reset password');
        this.isResettingEmployeePassword = false;
      }
    });
  }

  loadManagerAccessibleFeatures() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get all disabled features across all admins
    const disabledFeatures = new Set<string>();
    
    // Check each admin's disabled features
    this.allAdmins.forEach(admin => {
      const savedKey = `adminFeatureAccess_${admin.email}`;
      const savedFeatures = localStorage.getItem(savedKey);
      if (savedFeatures) {
        try {
          const features = JSON.parse(savedFeatures);
          features.forEach((feature: any) => {
            if (feature.enabled === false) {
              disabledFeatures.add(feature.id);
            }
          });
        } catch (e) {
          console.error('Error parsing admin feature access:', e);
        }
      }
    });
    
    // Also check from backend if available
    this.allAdmins.forEach(admin => {
      this.http.get(`${environment.apiBaseUrl}/api/admins/feature-access/${admin.email}`).subscribe({
        next: (response: any) => {
          if (response && response.success && response.features) {
            response.features.forEach((feature: any) => {
              if (feature.enabled === false) {
                disabledFeatures.add(feature.id);
              }
            });
            this.updateManagerAccessibleFeatures(disabledFeatures);
          }
        },
        error: (err) => {
          console.error('Error loading feature access from backend:', err);
        }
      });
    });
    
    this.updateManagerAccessibleFeatures(disabledFeatures);
  }

  updateManagerAccessibleFeatures(disabledFeatureIds: Set<string>) {
    // Create list of features that are disabled for admins (manager can use these)
    this.managerAccessibleFeatures = this.adminFeatures
      .filter(f => disabledFeatureIds.has(f.id))
      .map(f => ({ ...f })); // Create copies
    
    this.filterManagerFeatures();
  }

  filterManagerFeatures() {
    let filtered = [...this.managerAccessibleFeatures];
    
    // Filter by category
    if (this.managerFeatureFilter !== 'ALL') {
      filtered = filtered.filter(f => f.category === this.managerFeatureFilter);
    }
    
    // Filter by search query
    if (this.managerFeatureSearchQuery) {
      const query = this.managerFeatureSearchQuery.toLowerCase();
      filtered = filtered.filter(f => 
        f.name.toLowerCase().includes(query) ||
        f.description.toLowerCase().includes(query) ||
        f.category.toLowerCase().includes(query)
      );
    }
    
    this.filteredManagerFeatures = filtered;
  }

  navigateToFeature(featureId: string) {
    // Map feature IDs to admin dashboard routes
    const routeMap: { [key: string]: string } = {
      'manage-users': '/admin/users',
      'user-control': '/admin/user-control',
      'deposit-withdraw': '/admin/dashboard',
      'transactions': '/admin/transactions',
      'loans': '/admin/loans',
      'gold-loans': '/admin/gold-loans',
      'cheques': '/admin/cheques',
      'cards': '/admin/cards',
      'kyc': '/admin/kyc',
      'subsidy-claims': '/admin/subsidy-claims',
      'education-loans': '/admin/education-loan-applications',
      'chat': '/admin/chat',
      'tracking': '/admin/dashboard',
      'aadhar-verification': '/admin/dashboard',
      'gold-rate': '/admin/dashboard',
      'login-history': '/admin/dashboard',
      'update-history': '/admin/dashboard',
      'daily-report': '/admin/dashboard',
      'investments': '/admin/dashboard',
      'fixed-deposits': '/admin/dashboard',
      'emi-management': '/admin/dashboard'
    };
    
    const route = routeMap[featureId] || '/admin/dashboard';
    
    // Store that this navigation is from manager dashboard
    sessionStorage.setItem('navigationSource', 'MANAGER');
    sessionStorage.setItem('managerReturnPath', '/manager/dashboard');
    
    // For dashboard features, we need to pass a section parameter
    if (route === '/admin/dashboard') {
      // Store the section to open in sessionStorage
      sessionStorage.setItem('adminDashboardSection', featureId);
    }
    
    this.router.navigate([route]);
  }

  loadAllAdmins() {
    this.isLoadingAdmins = true;
    this.http.get(`${environment.apiBaseUrl}/api/admins/all`).subscribe({
      next: (admins: any) => {
        this.allAdmins = admins || [];
        this.isLoadingAdmins = false;
        console.log('Loaded admins:', this.allAdmins);
        // Reload manager accessible features after loading admins
        this.loadManagerAccessibleFeatures();
      },
      error: (err: any) => {
        console.error('Error loading admins:', err);
        this.isLoadingAdmins = false;
      }
    });
  }

  /** Base URL for API (used for admin photo in template). */
  readonly apiBaseUrl = environment.apiBaseUrl;

  /** URL for admin profile photo on ID card (manager dashboard). */
  getAdminPhotoUrl(adminId: number): string {
    return `${this.apiBaseUrl}/api/admins/profile-photo/${adminId}`;
  }

  selectAdmin(admin: any) {
    this.selectedAdmin = admin;
    // Pre-fill ID card edit fields if data exists
    this.idCardDesignationInput = admin.idCardDesignation || '';
    this.idCardDepartmentInput = admin.idCardDepartment || '';
    this.loadAdminFeatureAccess(admin.email);
    console.log('Selected admin:', admin);
  }

  loadAdminFeatureAccess(adminEmail: string) {
    // Load saved feature access for this admin
    const savedKey = `adminFeatureAccess_${adminEmail}`;
    const savedFeatures = localStorage.getItem(savedKey);
    
    if (savedFeatures) {
      try {
        const features = JSON.parse(savedFeatures);
        // Update the adminFeatures array with saved values
        features.forEach((savedFeature: any) => {
          const feature = this.adminFeatures.find(f => f.id === savedFeature.id);
          if (feature) {
            feature.enabled = savedFeature.enabled;
          }
        });
        
        // Store in map
        const featureMap = new Map<string, boolean>();
        features.forEach((f: any) => {
          featureMap.set(f.id, f.enabled);
        });
        this.adminFeatureAccess.set(adminEmail, featureMap);
      } catch (e) {
        console.error('Error loading admin feature access:', e);
      }
    } else {
      // Default: all features enabled
      const featureMap = new Map<string, boolean>();
      this.adminFeatures.forEach(f => {
        featureMap.set(f.id, true);
      });
      this.adminFeatureAccess.set(adminEmail, featureMap);
    }
    
    // Also try to load from backend
    this.http.get(`${environment.apiBaseUrl}/api/admins/feature-access/${adminEmail}`).subscribe({
      next: (response: any) => {
        if (response && response.success && response.features && response.features.length > 0) {
          response.features.forEach((feature: any) => {
            const f = this.adminFeatures.find(af => af.id === feature.id);
            if (f) {
              f.enabled = feature.enabled;
            }
          });
          // Update localStorage
          localStorage.setItem(savedKey, JSON.stringify(response.features));
        }
      },
      error: (err) => {
        console.error('Error loading feature access from backend:', err);
      }
    });
  }

  checkAndCreateDefaultManager() {
    // This method can be used to check manager account status
    // Actual creation should be done via API endpoint
  }

  createDefaultManager() {
    if (!confirm('This will create a default manager account with:\nEmail: manager@neobank.com\nPassword: manager123\n\nContinue?')) {
      return;
    }

    this.http.post(`${environment.apiBaseUrl}/api/admins/create-default-manager`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Default manager account created successfully!\n\nLogin Details:\nEmail: manager@neobank.com\nPassword: manager123');
        } else {
          this.alertService.error('Error', response.message || 'Failed to create manager account');
        }
      },
      error: (err: any) => {
        console.error('Error creating default manager:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to create manager account');
      }
    });
  }

  loadManagerInfo() {
    const adminData = sessionStorage.getItem('admin');
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        this.managerName = admin.name || admin.username || 'Manager';
      } catch (e) {
        console.error('Error parsing admin data:', e);
      }
    }
  }

  loadFeatureAccess() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const savedFeatures = localStorage.getItem('adminFeatureAccess');
    if (savedFeatures) {
      try {
        const features = JSON.parse(savedFeatures);
        // Update enabled status from saved data
        features.forEach((savedFeature: any) => {
          const feature = this.adminFeatures.find(f => f.id === savedFeature.id);
          if (feature) {
            feature.enabled = savedFeature.enabled;
          }
        });
      } catch (e) {
        console.error('Error loading feature access:', e);
      }
    }
  }

  saveFeatureAccess() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    if (!this.selectedAdmin) {
      this.alertService.error('Error', 'Please select an admin first');
      return;
    }
    
    // Save ALL features with their current enabled/disabled status for selected admin
    const featuresToSave = this.adminFeatures.map(f => ({
      id: f.id,
      enabled: f.enabled
    }));
    
    const adminEmail = this.selectedAdmin.email;
    const savedKey = `adminFeatureAccess_${adminEmail}`;
    
    console.log(`Saving feature access for admin ${adminEmail}:`, featuresToSave);
    localStorage.setItem(savedKey, JSON.stringify(featuresToSave));
    
    // Store in map
    const featureMap = new Map<string, boolean>();
    featuresToSave.forEach(f => {
      featureMap.set(f.id, f.enabled);
    });
    this.adminFeatureAccess.set(adminEmail, featureMap);
    
    // Dispatch custom event to notify same-tab listeners (admin dashboard)
    window.dispatchEvent(new CustomEvent('featureAccessUpdated', { 
      detail: { adminEmail, features: featuresToSave } 
    }));
    
    // Also save to backend
    this.http.post(`${environment.apiBaseUrl}/api/admins/feature-access/${adminEmail}`, {
      features: featuresToSave
    }).subscribe({
      next: (response: any) => {
        console.log('Feature access saved to backend:', response);
        this.alertService.success('Success', `Feature access updated successfully for ${this.selectedAdmin.name}! Admin dashboard will reflect changes on next page load.`);
        this.showReloadMessage();
        // Reload manager accessible features after saving
        this.loadManagerAccessibleFeatures();
      },
      error: (err: any) => {
        console.error('Error saving feature access:', err);
        // Still show success since it's saved locally
        this.alertService.success('Success', `Feature access updated (saved locally) for ${this.selectedAdmin.name}. Admin dashboard will reflect changes on next page load.`);
        this.showReloadMessage();
        // Reload manager accessible features after saving
        this.loadManagerAccessibleFeatures();
      }
    });
  }

  showReloadMessage() {
    // This will be shown to inform that changes take effect after admin reloads
    console.log('Feature access saved. Admins need to reload their dashboard to see changes.');
  }

  toggleFeature(feature: AdminFeature) {
    feature.enabled = !feature.enabled;
    this.saveFeatureAccess();
  }

  toggleAllFeatures(enabled: boolean) {
    this.adminFeatures.forEach(feature => {
      feature.enabled = enabled;
    });
    this.saveFeatureAccess();
  }

  filterFeatures() {
    let filtered = [...this.adminFeatures];

    if (this.featureFilter !== 'ALL') {
      filtered = filtered.filter(f => f.category === this.featureFilter);
    }

    if (this.featureSearchQuery && this.featureSearchQuery.trim() !== '') {
      const query = this.featureSearchQuery.toLowerCase();
      filtered = filtered.filter(f =>
        f.name.toLowerCase().includes(query) ||
        f.description.toLowerCase().includes(query) ||
        f.category.toLowerCase().includes(query)
      );
    }

    this.filteredFeatures = filtered;
  }

  getCategories(): string[] {
    return ['ALL', ...new Set(this.adminFeatures.map(f => f.category))];
  }

  getEnabledCount(): number {
    return this.adminFeatures.filter(f => f.enabled).length;
  }

  getDisabledCount(): number {
    return this.adminFeatures.filter(f => !f.enabled).length;
  }

  setActiveSection(section: string) {
    if (section === 'open-account-approval') {
      this.navigateTo('admin-open-account');
      return;
    }
    this.activeSection = section;
    if (section === 'create-admin') {
      this.clearAdminForm();
    } else if (section === 'manager-features') {
      this.loadManagerAccessibleFeatures();
    } else if (section === 'blocked-admins') {
      this.loadBlockedAdmins();
    } else if (section === 'session-history' && isPlatformBrowser(this.platformId)) {
      // Reload session history when opening the session history section
      this.loadSessionHistory();
    } else if (section === 'fraud-alerts' && isPlatformBrowser(this.platformId)) {
      this.loadFraudAlerts();
    } else if (section === 'loans-overview' && isPlatformBrowser(this.platformId)) {
      this.loadManagerLoanOverview();
    } else if (section === 'branch-account' && isPlatformBrowser(this.platformId)) {
      this.loadBranchAccountSummary();
      this.loadBranchAccountTransactions();
    } else if (section === 'attendance-salary' && isPlatformBrowser(this.platformId)) {
      this.loadAttendanceSummary();
    } else if (section === 'attendance-admin' && isPlatformBrowser(this.platformId)) {
      this.loadAdminAttendanceCalendar();
    } else if (section === 'admin-management' && isPlatformBrowser(this.platformId)) {
      this.loadAdminProfileUpdateRequests();
    } else if (section === 'salary-accounts' && isPlatformBrowser(this.platformId)) {
      this.loadSalaryAccounts();
    } else if (section === 'manager-permissions' && isPlatformBrowser(this.platformId)) {
      this.mpSelectedAdmin = null;
      this.mpAdminSalaryAccount = null;
      this.mpLinked = false;
    } else if (section === 'gold-loan-management' && isPlatformBrowser(this.platformId)) {
      this.loadMgrGoldLoans();
      this.loadMgrGoldRate();
      this.loadMgrGoldRateHistory();
    } else if (section === 'customer-data-upload' && isPlatformBrowser(this.platformId)) {
      this.loadCustomerDataStats();
      this.loadUploadedCustomerData();
    } else if (section === 'cibil-reports' && isPlatformBrowser(this.platformId)) {
      this.loadCibilStats();
      this.loadCibilReports();
    }
  }

  // Sidebar Search Methods
  onSidebarSearch() {
    // Real-time filtering handled by getFilteredManagerMenu()
  }

  getFilteredManagerMenu() {
    if (!this.sidebarSearchQuery || !this.sidebarSearchQuery.trim()) {
      return this.managerMenuSections;
    }
    const query = this.sidebarSearchQuery.toLowerCase().trim();
    return this.managerMenuSections
      .map(section => ({
        title: section.title,
        items: section.items.filter(item => item.label.toLowerCase().includes(query) || item.section.toLowerCase().includes(query))
      }))
      .filter(section => section.items.length > 0);
  }

  isMenuItemMatch(item: any): boolean {
    if (!this.sidebarSearchQuery) return false;
    const query = this.sidebarSearchQuery.toLowerCase().trim();
    return item.label.toLowerCase().includes(query) || item.section.toLowerCase().includes(query);
  }

  /**
   * Load pending admin profile update requests that need manager approval.
   */
  loadAdminProfileUpdateRequests() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.isLoadingAdminProfileUpdates = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admins/admin-profile-update/pending`).subscribe({
      next: (requests) => {
        this.adminProfileUpdateRequests = requests || [];
        this.isLoadingAdminProfileUpdates = false;
      },
      error: (err) => {
        console.error('Error loading admin profile update requests:', err);
        this.adminProfileUpdateRequests = [];
        this.isLoadingAdminProfileUpdates = false;
      }
    });
  }

  approveAdminProfileUpdate(request: any) {
    if (!request || !request.id) return;
    const approvedBy = this.managerName || 'Manager';
    this.http.put(`${environment.apiBaseUrl}/api/admins/admin-profile-update/${request.id}/approve?approvedBy=${encodeURIComponent(approvedBy)}`, {})
      .subscribe({
        next: (resp: any) => {
          if (resp && resp.success) {
            this.alertService.success('Success', resp.message || 'Admin profile update approved');
            this.loadAllAdmins();
            this.loadAdminProfileUpdateRequests();
          } else {
            this.alertService.error('Error', resp?.message || 'Failed to approve admin profile update');
          }
        },
        error: (err) => {
          console.error('Error approving admin profile update:', err);
          this.alertService.error('Error', err.error?.message || 'Failed to approve admin profile update');
        }
      });
  }

  rejectAdminProfileUpdate(request: any) {
    if (!request || !request.id) return;
    const reason = prompt('Enter reason for rejection (optional):') || '';
    const rejectedBy = this.managerName || 'Manager';
    const url = `${environment.apiBaseUrl}/api/admins/admin-profile-update/${request.id}/reject?rejectedBy=${encodeURIComponent(rejectedBy)}&reason=${encodeURIComponent(reason)}`;
    this.http.put(url, {}).subscribe({
      next: (resp: any) => {
        if (resp && resp.success) {
          this.alertService.success('Success', resp.message || 'Admin profile update rejected');
          this.loadAdminProfileUpdateRequests();
        } else {
          this.alertService.error('Error', resp?.message || 'Failed to reject admin profile update');
        }
      },
      error: (err) => {
        console.error('Error rejecting admin profile update:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject admin profile update');
      }
    });
  }

  toggleAdminProfileRequestDetails(request: any) {
    if (!request || request.id == null) return;
    this.expandedAdminProfileRequestId =
      this.expandedAdminProfileRequestId === request.id ? null : Number(request.id);
  }

  private parseProfileJson(json: string | null | undefined): any {
    if (!json) {
      return {};
    }
    try {
      return JSON.parse(json);
    } catch {
      return {};
    }
  }

  getAdminProfileDiffKeys(request: any): string[] {
    if (!request) return [];
    const oldSnap = this.parseProfileJson(request.oldProfileJson);
    const newSnap = this.parseProfileJson(request.newProfileJson);
    const keys = new Set<string>();
    Object.keys(oldSnap || {}).forEach(k => keys.add(k));
    Object.keys(newSnap || {}).forEach(k => keys.add(k));
    return Array.from(keys);
  }

  getAdminProfileOldValue(request: any, key: string): any {
    const oldSnap = this.parseProfileJson(request.oldProfileJson);
    return oldSnap && Object.prototype.hasOwnProperty.call(oldSnap, key)
      ? oldSnap[key]
      : '';
  }

  getAdminProfileNewValue(request: any, key: string): any {
    const newSnap = this.parseProfileJson(request.newProfileJson);
    return newSnap && Object.prototype.hasOwnProperty.call(newSnap, key)
      ? newSnap[key]
      : '';
  }

  loadAttendanceSummary() {
    this.isLoadingAttendance = true;
    const date = this.attendanceDate || new Date().toISOString().substring(0, 10);
    this.http.get<any>(`${environment.apiBaseUrl}/api/manager/attendance/summary?date=${encodeURIComponent(date)}`).subscribe({
      next: (data) => {
        this.attendanceSummaryRows = data.rows || [];
        this.isLoadingAttendance = false;
      },
      error: () => {
        this.attendanceSummaryRows = [];
        this.isLoadingAttendance = false;
      }
    });
  }

  verifyAttendanceByIdCard() {
    if (!this.attendanceIdCardNumber || !this.attendanceIdCardNumber.trim()) {
      this.alertService.error('Error', 'Enter ID card number');
      return;
    }
    this.isVerifyingAttendance = true;
    const verifiedBy = this.managerName || 'Manager';
    const body: any = { idCardNumber: this.attendanceIdCardNumber.trim(), verifiedBy };
    this.http.post<any>(`${environment.apiBaseUrl}/api/manager/attendance/verify`, body).subscribe({
      next: (resp) => {
        this.isVerifyingAttendance = false;
        if (resp && resp.success) {
          this.alertService.success('Attendance', resp.message || 'Attendance verified');
          this.attendanceIdCardNumber = '';
          this.loadAttendanceSummary();
        } else {
          this.alertService.error('Error', resp?.details || resp?.message || 'Failed to verify attendance');
        }
      },
      error: (err) => {
        this.isVerifyingAttendance = false;
        const msg = err?.error?.details || err?.error?.message || 'Failed to verify attendance';
        this.alertService.error('Error', msg);
      }
    });
  }

  payDailySalary() {
    this.isPayingSalary = true;
    const paidBy = this.managerName || 'Manager';
    const body: any = { date: this.attendanceDate, paidBy };
    this.http.post<any>(`${environment.apiBaseUrl}/api/manager/salary/pay-daily`, body).subscribe({
      next: (resp) => {
        this.isPayingSalary = false;
        if (resp && resp.success) {
          this.alertService.success('Salary', `Paid: ${resp.successCount || 0}, Skipped: ${resp.skippedCount || 0}, Failed: ${resp.failedCount || 0}`);
          this.loadAttendanceSummary();
        } else {
          this.alertService.error('Error', resp?.message || 'Salary payout failed');
        }
      },
      error: (err) => {
        this.isPayingSalary = false;
        this.alertService.error('Error', err.error?.message || 'Salary payout failed');
      }
    });
  }

  openAdminAttendance(row: any) {
    this.attendanceAdminDetail = row;
    this.attendanceCalendarYear = new Date().getFullYear();
    this.attendanceCalendarRows = [];
    this.attendanceCalendarSelected = {};
    this.activeSection = 'attendance-admin';
    if (isPlatformBrowser(this.platformId)) {
      this.loadAdminAttendanceCalendar();
    }
  }

  loadAdminAttendanceCalendar() {
    if (!this.attendanceAdminDetail || !this.attendanceAdminDetail.adminId) return;
    this.attendanceCalendarLoading = true;
    this.attendanceCalendarSelected = {};
    this.attendanceCalendarByMonth = {};
    const id = this.attendanceAdminDetail.adminId;
    const year = this.attendanceCalendarYear;
    this.http.get<any>(`${environment.apiBaseUrl}/api/manager/attendance/admin/${id}?year=${year}`).subscribe({
      next: (data) => {
        this.attendanceCalendarRows = data.rows || [];
        // Group rows by month for easier rendering
        this.attendanceCalendarByMonth = {};
        for (const row of this.attendanceCalendarRows) {
          const m = row.month;
          if (!this.attendanceCalendarByMonth[m]) {
            this.attendanceCalendarByMonth[m] = [];
          }
          this.attendanceCalendarByMonth[m].push(row);
        }
        this.attendanceCalendarLoading = false;
      },
      error: () => {
        this.attendanceCalendarRows = [];
        this.attendanceCalendarByMonth = {};
        this.attendanceCalendarLoading = false;
      }
    });
  }

  toggleAttendanceDateSelection(date: string) {
    if (!date) return;
    const key = date;
    this.attendanceCalendarSelected[key] = !this.attendanceCalendarSelected[key];
  }

  paySelectedDays() {
    if (!this.attendanceAdminDetail || !this.attendanceAdminDetail.adminId) return;
    const selectedDates = Object.keys(this.attendanceCalendarSelected).filter(d => this.attendanceCalendarSelected[d]);
    if (selectedDates.length === 0) {
      this.alertService.error('Error', 'Select at least one present day to pay salary');
      return;
    }
    this.isPayingSalary = true;
    const body: any = {
      adminId: this.attendanceAdminDetail.adminId,
      dates: selectedDates,
      paidBy: this.managerName || 'Manager'
    };
    this.http.post<any>(`${environment.apiBaseUrl}/api/manager/salary/pay-selected`, body).subscribe({
      next: (resp) => {
        this.isPayingSalary = false;
        if (resp && resp.success) {
          this.alertService.success('Salary', `Paid: ${resp.successCount || 0}, Skipped: ${resp.skippedCount || 0}, Failed: ${resp.failedCount || 0}`);
          this.loadAdminAttendanceCalendar();
          this.loadAttendanceSummary();
        } else {
          this.alertService.error('Error', resp?.message || 'Failed to pay selected days');
        }
      },
      error: (err) => {
        this.isPayingSalary = false;
        const msg = err?.error?.details || err?.error?.message || 'Failed to pay selected days';
        this.alertService.error('Error', msg);
      }
    });
  }

  loadFraudAlerts(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.isLoadingFraudAlerts = true;
    const params: any = { page: 0, size: 100 };
    if (this.fraudAlertStatusFilter) params.status = this.fraudAlertStatusFilter;
    if (this.fraudAlertTypeFilter) params.alertType = this.fraudAlertTypeFilter;
    if (this.fraudAlertSourceFilter) params.sourceType = this.fraudAlertSourceFilter;
    const q = new URLSearchParams(params).toString();
    this.http.get<{ success: boolean; alerts: any[]; pendingCount?: number }>(`${environment.apiBaseUrl}/api/fraud-alerts?${q}`).subscribe({
      next: (res) => {
        this.fraudAlerts = res.alerts || [];
        if (res.pendingCount !== undefined) this.fraudAlertsPendingCount = res.pendingCount;
        this.isLoadingFraudAlerts = false;
      },
      error: () => {
        this.fraudAlerts = [];
        this.isLoadingFraudAlerts = false;
      }
    });
    this.http.get<{ success: boolean; pendingCount: number }>(`${environment.apiBaseUrl}/api/fraud-alerts/count-pending`).subscribe({
      next: (res) => { if (res.pendingCount !== undefined) this.fraudAlertsPendingCount = res.pendingCount; }
    });
  }

  updateFraudAlertStatus(alertId: number, status: string): void {
    const reviewedBy = this.managerName || 'Manager';
    this.http.patch<{ success: boolean; alert: any }>(`${environment.apiBaseUrl}/api/fraud-alerts/${alertId}/status`, { status, reviewedBy }).subscribe({
      next: () => { this.loadFraudAlerts(); },
      error: (err) => this.alertService.userError('Update failed', err.error?.message || 'Could not update alert')
    });
  }

  createAdminEmployee() {
    if (!this.newAdmin.name || !this.newAdmin.email || !this.newAdmin.password) {
      this.adminCreationError = 'Please fill in all required fields';
      return;
    }

    if (this.newAdmin.password.length < 6) {
      this.adminCreationError = 'Password must be at least 6 characters';
      return;
    }

    this.isCreatingAdmin = true;
    this.adminCreationError = '';
    this.adminCreationSuccess = false;

    this.http.post(`${environment.apiBaseUrl}/api/admins/create`, this.newAdmin).subscribe({
      next: (response: any) => {
        this.isCreatingAdmin = false;
        this.adminCreationSuccess = true;
        this.alertService.success('Success', `Admin employee created successfully!\n\nEmail: ${this.newAdmin.email}\nPassword: ${this.newAdmin.password}`);
        
        // Reload admin list
        this.loadAllAdmins();
        
        // Keep the form data for display, but prepare for next creation
        setTimeout(() => {
          this.clearAdminForm();
        }, 5000);
      },
      error: (err: any) => {
        this.isCreatingAdmin = false;
        this.adminCreationError = err.error?.message || 'Failed to create admin employee. Email might already exist.';
        this.alertService.error('Error', this.adminCreationError);
      }
    });
  }

  clearAdminForm() {
    this.newAdmin = {
      name: '',
      email: '',
      password: '',
      role: 'ADMIN',
      employeeId: '',
      profileComplete: false
    };
    this.adminCreationSuccess = false;
    this.adminCreationError = '';
  }

  clearExistingAdminSearch() {
    this.searchExistingQuery = '';
    this.existingAdminResult = null;
    this.existingAdminError = '';
    this.isSearchingExisting = false;
    this.isAddingExistingAdmin = false;
  }

  searchExistingAdmin() {
    const q = (this.searchExistingQuery || '').trim();
    if (!q) {
      this.existingAdminError = 'Please enter an email or ID card number to search';
      return;
    }

    this.isSearchingExisting = true;
    this.existingAdminError = '';
    this.existingAdminResult = null;

    // Decide which backend endpoint to call:
    // - If query looks like an NBID / ID card (e.g. starts with letters like 'NBID'), call id-card lookup
    // - If query is numeric, call admin by numeric id
    // - Otherwise, use the general search endpoint
    const isIdCard = /^\s*[A-Za-z]+\w*\d+\s*$/.test(q) && /[A-Za-z]/.test(q);
    const isNumericId = /^\d+$/.test(q);

    if (isIdCard) {
      const lookupUrl = `${environment.apiBaseUrl}/api/admins/id-card/lookup/${encodeURIComponent(q)}`;
      this.http.get<any>(lookupUrl).subscribe({
        next: (res) => {
          this.isSearchingExisting = false;
          if (!res || (!res.success && !res.admin && !res.result && !res.data)) {
            this.existingAdminError = 'No account found for the provided ID card number';
            return;
          }
          // backend may return { admin: ... } or { result: ... } or the admin directly
          this.existingAdminResult = res.admin || res.result || res.data || res;
        },
        error: (err) => {
          console.error('ID card lookup error', err);
          this.isSearchingExisting = false;
          this.existingAdminError = err?.error?.message || 'Failed to lookup ID card';
        }
      });
      return;
    }

    if (isNumericId) {
      const numericId = Number(q);
      const byIdUrl = `${environment.apiBaseUrl}/api/admins/${numericId}`;
      this.http.get<any>(byIdUrl).subscribe({
        next: (res) => {
          this.isSearchingExisting = false;
          if (!res) {
            this.existingAdminError = 'No admin found with provided ID';
            return;
          }
          this.existingAdminResult = res;
        },
        error: (err) => {
          console.error('Admin by id error', err);
          this.isSearchingExisting = false;
          this.existingAdminError = err?.error?.message || 'Failed to fetch admin by id';
        }
      });
      return;
    }

    const url = `${environment.apiBaseUrl}/api/admins/search?searchTerm=${encodeURIComponent(q)}&page=0&size=5`;
    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.isSearchingExisting = false;
        // Normalize possible response shapes
        let found: any = null;
        if (!res) {
          this.existingAdminError = 'No results returned from server';
          return;
        }
        if (Array.isArray(res)) {
          found = res[0] || null;
        } else if (res.content && Array.isArray(res.content)) {
          found = res.content[0] || null;
        } else if (res.admins && Array.isArray(res.admins)) {
          found = res.admins[0] || null;
        } else if (res.length && Array.isArray(res.length)) {
          // unlikely shape, skip
          found = res[0] || null;
        } else if (res.admin) {
          found = res.admin;
        } else if (res.result) {
          found = res.result;
        }

        if (!found) {
          this.existingAdminError = 'No account found for the provided email or ID card number';
          return;
        }

        this.existingAdminResult = found;
      },
      error: (err) => {
        console.error('Search error', err);
        this.isSearchingExisting = false;
        this.existingAdminError = err?.error?.message || 'Failed to search for account';
      }
    });
  }

  addExistingAdminPermanently() {
    if (!this.existingAdminResult || !this.existingAdminResult.email) {
      this.alertService.error('Error', 'No account selected to add');
      return;
    }

    this.isAddingExistingAdmin = true;

    const tempPassword = this.generateTempPassword();
    const payload: any = {
      name: this.existingAdminResult.name || this.existingAdminResult.fullName || '',
      email: this.existingAdminResult.email,
      password: tempPassword,
      role: 'ADMIN',
      employeeId: this.existingAdminResult.employeeId || this.existingAdminResult.empId || ''
    };

    this.http.post(`${environment.apiBaseUrl}/api/admins/create`, payload).subscribe({
      next: (response: any) => {
        this.isAddingExistingAdmin = false;
        this.alertService.success('Admin Added', `Admin employee added. Email: ${payload.email}\nTemporary password: ${tempPassword}`);
        this.loadAllAdmins();
        this.clearExistingAdminSearch();
      },
      error: (err: any) => {
        this.isAddingExistingAdmin = false;
        const msg = err?.error?.message || 'Failed to add admin employee. It may already exist.';
        this.alertService.error('Error', msg);
      }
    });
  }

  private generateTempPassword(length: number = 10) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#_$!';
    let out = '';
    for (let i = 0; i < length; i++) out += chars.charAt(Math.floor(Math.random() * chars.length));
    return out;
  }

  // ===== Admin ID CARD METHODS =====

  generateIdCardForSelectedAdmin() {
    if (!this.selectedAdmin || !this.selectedAdmin.id) {
      this.alertService.error('Error', 'Please select an admin first');
      return;
    }
    this.isGeneratingIdCard = true;
    const idNum = typeof this.selectedAdmin.id === 'number' ? this.selectedAdmin.id : Number(this.selectedAdmin.id);
    if (isNaN(idNum)) {
      this.alertService.error('Error', 'Invalid admin ID');
      this.isGeneratingIdCard = false;
      return;
    }
    const generatedBy = this.managerName || 'Manager';
    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/id-card/${idNum}/generate?generatedBy=${encodeURIComponent(generatedBy)}`, {})
      .subscribe({
        next: (resp) => {
          this.isGeneratingIdCard = false;
          if (resp && resp.success && resp.admin) {
            this.alertService.success('ID Card Generated', 'NeoBank admin ID card generated successfully.');
            // Update selected admin object with latest data
            this.selectedAdmin = { ...this.selectedAdmin, ...resp.admin };
            this.idCardDesignationInput = this.selectedAdmin.idCardDesignation || '';
            this.idCardDepartmentInput = this.selectedAdmin.idCardDepartment || '';
          } else {
            this.alertService.error('Error', resp?.message || 'Failed to generate ID card');
          }
        },
        error: (err) => {
          console.error('Error generating ID card', err);
          this.isGeneratingIdCard = false;
          this.alertService.error('Error', err.error?.message || 'Failed to generate ID card');
        }
      });
  }

  onIdCardPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input?.files && input.files.length ? input.files[0] : null;
    if (!file || !this.selectedAdmin?.id) {
      if (input) input.value = '';
      return;
    }
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (file.type && !allowed.includes(file.type)) {
      this.alertService.error('Validation Error', 'Please select a JPG, PNG, or WEBP image');
      input.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.alertService.error('Validation Error', 'Image must be less than 2MB');
      input.value = '';
      return;
    }
    const idNum = typeof this.selectedAdmin.id === 'number' ? this.selectedAdmin.id : Number(this.selectedAdmin.id);
    if (isNaN(idNum)) {
      this.alertService.error('Error', 'Invalid admin ID');
      input.value = '';
      return;
    }
    const formData = new FormData();
    formData.append('file', file);
    this.isUploadingIdCardPhoto = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/profile-photo/${idNum}`, formData).subscribe({
      next: (resp: any) => {
        this.isUploadingIdCardPhoto = false;
        input.value = '';
        if (resp?.success && resp.admin) {
          this.alertService.success('Photo updated', 'ID card photo uploaded successfully.');
          this.selectedAdmin = { ...this.selectedAdmin, ...resp.admin };
          this.idCardPhotoCacheBuster = Date.now();
          const idx = this.allAdmins.findIndex((a: any) => a.id === this.selectedAdmin.id);
          if (idx >= 0) {
            this.allAdmins[idx] = { ...this.allAdmins[idx], ...resp.admin };
          }
        } else {
          this.alertService.error('Error', resp?.message || 'Failed to upload photo');
        }
      },
      error: (err: any) => {
        this.isUploadingIdCardPhoto = false;
        input.value = '';
        this.alertService.error('Error', err.error?.message || 'Failed to upload photo');
      }
    });
  }

  updateIdCardMetaForSelectedAdmin() {
    if (!this.selectedAdmin || !this.selectedAdmin.id) {
      this.alertService.error('Error', 'Please select an admin first');
      return;
    }
    this.isUpdatingIdCard = true;
    const idNum = typeof this.selectedAdmin.id === 'number' ? this.selectedAdmin.id : Number(this.selectedAdmin.id);
    if (isNaN(idNum)) {
      this.alertService.error('Error', 'Invalid admin ID');
      this.isUpdatingIdCard = false;
      return;
    }
    const params: string[] = [];
    if (this.idCardDesignationInput) {
      params.push(`designation=${encodeURIComponent(this.idCardDesignationInput)}`);
    }
    if (this.idCardDepartmentInput) {
      params.push(`department=${encodeURIComponent(this.idCardDepartmentInput)}`);
    }
    params.push(`updatedBy=${encodeURIComponent(this.managerName || 'Manager')}`);
    const query = params.length ? '?' + params.join('&') : '';

    this.http.put<any>(`${environment.apiBaseUrl}/api/admins/id-card/${idNum}${query}`, {})
      .subscribe({
        next: (resp) => {
          this.isUpdatingIdCard = false;
          if (resp && resp.success && resp.admin) {
            this.alertService.success('ID Card Updated', 'ID card details updated successfully.');
            this.selectedAdmin = { ...this.selectedAdmin, ...resp.admin };
          } else {
            this.alertService.error('Error', resp?.message || 'Failed to update ID card details');
          }
        },
        error: (err) => {
          console.error('Error updating ID card meta', err);
          this.isUpdatingIdCard = false;
          this.alertService.error('Error', err.error?.message || 'Failed to update ID card details');
        }
      });
  }

  lookupAdminByIdCardNumber() {
    this.idCardLookupError = '';
    this.idCardLookupResult = null;
    if (!this.idCardLookupNumber || this.idCardLookupNumber.trim() === '') {
      this.idCardLookupError = 'Please enter an ID card number to search';
      return;
    }
    const idCardNum = this.idCardLookupNumber.trim();
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/id-card/lookup/${encodeURIComponent(idCardNum)}`)
      .subscribe({
        next: (resp) => {
          if (resp && resp.success && resp.admin) {
            this.idCardLookupResult = resp.admin;
          } else {
            this.idCardLookupError = resp?.message || 'No admin found for this ID card number';
          }
        },
        error: (err) => {
          console.error('Error looking up ID card', err);
          this.idCardLookupError = err.error?.message || 'Failed to lookup ID card number';
        }
      });
  }

  loadBranchAccountSummary() {
    this.isLoadingBranchAccount = true;
    this.branchAccountSummary = null;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/branch-account`).subscribe({
      next: (data) => {
        this.branchAccountSummary = data;
        this.isLoadingBranchAccount = false;
        if (data && (data.accountNumber || data.accountName || data.ifscCode)) {
          if (data.accountNumber) this.branchAccountNumber = String(data.accountNumber);
          if (data.accountName) this.branchAccountName = String(data.accountName);
          if (data.ifscCode) this.branchAccountIfsc = String(data.ifscCode);
        }
      },
      error: (err) => {
        console.error('Error loading branch account', err);
        this.branchAccountSummary = null;
        this.isLoadingBranchAccount = false;
      }
    });
  }

  setBranchAccount() {
    if (!this.branchAccountNumber || !this.branchAccountNumber.trim()) {
      this.alertService.error('Error', 'Account number is required');
      return;
    }
    this.isSavingBranchAccount = true;
    const body: any = {
      accountNumber: this.branchAccountNumber.trim(),
      accountName: this.branchAccountName.trim() || undefined,
      ifscCode: this.branchAccountIfsc.trim() || undefined
    };
    const adminData = sessionStorage.getItem('admin');
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        if (admin.id != null) body.adminId = String(admin.id);
      } catch (_) {}
    }
    this.http.put<any>(`${environment.apiBaseUrl}/api/admins/branch-account`, body).subscribe({
      next: (resp) => {
        this.isSavingBranchAccount = false;
        if (resp && resp.success) {
          this.alertService.success('Branch Account', resp.message || 'Branch account set successfully.');
          this.loadBranchAccountSummary();
          this.loadBranchAccountTransactions();
        } else {
          this.alertService.error('Error', resp?.message || 'Failed to set branch account');
        }
      },
      error: (err) => {
        console.error('Error setting branch account', err);
        this.isSavingBranchAccount = false;
        this.alertService.error('Error', err.error?.message || 'Failed to set branch account');
      }
    });
  }

  loadBranchAccountTransactions() {
    this.isLoadingBranchTxns = true;
    let url = `${environment.apiBaseUrl}/api/admins/branch-account/transactions?page=${this.branchTxnsPage}&size=${this.branchTxnsSize}`;
    if (this.branchTxnsFromDate) url += `&fromDate=${encodeURIComponent(this.branchTxnsFromDate)}`;
    if (this.branchTxnsToDate) url += `&toDate=${encodeURIComponent(this.branchTxnsToDate)}`;
    if (this.branchTxnsSearch?.trim()) url += `&search=${encodeURIComponent(this.branchTxnsSearch.trim())}`;
    this.http.get<any>(url).subscribe({
      next: (data) => {
        this.branchAccountTransactions = data.content || [];
        this.branchTxnsTotalElements = data.totalElements ?? 0;
        this.branchTxnsTotalPages = data.totalPages ?? 0;
        this.isLoadingBranchTxns = false;
      },
      error: () => {
        this.branchAccountTransactions = [];
        this.isLoadingBranchTxns = false;
      }
    });
  }

  applyBranchTxnsFilter() {
    this.branchTxnsPage = 0;
    this.loadBranchAccountTransactions();
  }

  branchTxnsPagePrev() {
    if (this.branchTxnsPage > 0) {
      this.branchTxnsPage--;
      this.loadBranchAccountTransactions();
    }
  }

  branchTxnsPageNext() {
    if (this.branchTxnsPage < this.branchTxnsTotalPages - 1) {
      this.branchTxnsPage++;
      this.loadBranchAccountTransactions();
    }
  }

  async printIdCardForSelectedAdmin() {
    if (!this.selectedAdmin || !this.selectedAdmin.idCardNumber) {
      this.alertService.error('Error', 'Generate an ID card first before printing');
      return;
    }
    const admin = this.selectedAdmin;
    const escapeHtml = (v: any) => {
      const s = String(v ?? '');
      return s
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    };

    const name = escapeHtml(admin.name || 'Admin Employee');
    const email = escapeHtml(admin.email || '');
    const employeeId = escapeHtml(admin.employeeId || '');
    const designation = escapeHtml(admin.idCardDesignation || admin.role || 'Administrator');
    const department = escapeHtml(admin.idCardDepartment || '');
    const idNumber = escapeHtml(admin.idCardNumber);
    const address = escapeHtml(admin.address || '');
    const issuedAt = admin.idCardGeneratedAt ? new Date(admin.idCardGeneratedAt) : null;
    const validTillDate = admin.idCardValidTill ? new Date(admin.idCardValidTill) : null;
    const dojDate = admin.dateOfJoining ? new Date(admin.dateOfJoining) : (admin.createdAt ? new Date(admin.createdAt) : null);

    const issuedAtText = issuedAt ? issuedAt.toLocaleString('en-IN') : 'N/A';
    const validTillText = validTillDate ? validTillDate.toLocaleDateString('en-IN') : 'N/A';
    const dojText = dojDate ? dojDate.toLocaleDateString('en-IN') : 'N/A';

    const photoUrl = (admin.id && admin.profilePhotoPath)
      ? `${environment.apiBaseUrl}/api/admins/profile-photo/${admin.id}?t=${new Date().getTime()}`
      : '';

    const qrPayload = JSON.stringify({
      bank: 'NEOBANK',
      idCardNumber: admin.idCardNumber,
      name: admin.name || '',
      email: admin.email || '',
      employeeId: admin.employeeId || '',
      designation: admin.idCardDesignation || admin.role || '',
      department: admin.idCardDepartment || '',
      issuedAt: admin.idCardGeneratedAt || '',
      validTill: admin.idCardValidTill || ''
    });

    let qrDataUrl = '';
    try {
      qrDataUrl = await QRCode.toDataURL(qrPayload, {
        errorCorrectionLevel: 'M',
        margin: 1,
        width: 180,
        color: { dark: '#0b3a8c', light: '#ffffff' }
      });
    } catch (e) {
      console.warn('Failed to generate QR code', e);
      qrDataUrl = '';
    }

    const html = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>NeoBank Admin ID Card</title>
  <style>
    @page {
      size: 54mm 86mm;
      margin: 0;
    }
    body {
      margin: 0;
      padding: 0;
      font-family: Arial, sans-serif;
      background: #ffffff;
    }
    .page {
      width: 54mm;
      height: 86mm;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;
    }
    .page.break {
      page-break-before: always;
    }
    /* Front — portrait PVC */
    .id-card {
      width: 50mm;
      height: 82mm;
      border-radius: 3mm;
      overflow: hidden;
      background: linear-gradient(180deg, #0f4ab8 0%, #1f7ae0 35%, #38bdf8 100%);
      color: #fff;
      display: flex;
      flex-direction: column;
      box-shadow: 0 2px 4px rgba(0,0,0,0.25);
      box-sizing: border-box;
    }
    .id-top {
      padding: 3mm 3mm 2mm;
      text-align: center;
      flex-shrink: 0;
    }
    .bank-name {
      font-weight: 800;
      letter-spacing: 2px;
      font-size: 9px;
    }
    .bank-tagline {
      font-size: 6.5px;
      opacity: 0.9;
      margin-top: 1px;
    }
    .id-photo-wrap {
      display: flex;
      justify-content: center;
      padding: 2mm 0;
      flex-shrink: 0;
    }
    .photo-circle {
      width: 26mm;
      height: 32mm;
      border-radius: 2mm;
      background: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      color: #0f4ab8;
      overflow: hidden;
      border: 2px solid rgba(255,255,255,0.85);
      box-sizing: border-box;
    }
    .photo-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .id-info {
      flex: 1;
      margin: 0 3mm 2mm;
      padding: 2.5mm 2.5mm;
      background: #ffffff;
      color: #12315b;
      border-radius: 2mm;
      font-size: 6.5px;
      line-height: 1.25;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }
    .info-name {
      font-size: 8.5px;
      font-weight: 800;
      margin-bottom: 2px;
      text-align: center;
    }
    .info-role {
      font-size: 7.5px;
      font-weight: 700;
      color: #0f4ab8;
      margin-bottom: 2px;
      text-align: center;
    }
    .info-row {
      margin-bottom: 1.5px;
      word-break: break-word;
    }
    .label {
      font-weight: 700;
      color: #0b3a8c;
    }
    .id-bottom {
      padding: 2mm 3mm 2.5mm;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1mm;
      background: #0b3a8c;
      color: #e5f0ff;
      font-size: 5.5px;
      flex-shrink: 0;
    }
    .barcode {
      display: flex;
      gap: 0.4mm;
      height: 5mm;
      align-items: flex-end;
    }
    .barcode span {
      display: inline-block;
      width: 0.35mm;
      background: #e5f0ff;
    }
    .barcode span:nth-child(2n) { height: 3.5mm; }
    .barcode span:nth-child(2n+1) { height: 5mm; }
    .dates {
      text-align: center;
      line-height: 1.35;
    }

    /* Back — portrait */
    .back-card {
      width: 50mm;
      height: 82mm;
      border-radius: 3mm;
      overflow: hidden;
      background: #ffffff;
      color: #12315b;
      display: flex;
      flex-direction: column;
      box-shadow: 0 2px 4px rgba(0,0,0,0.25);
      border: 1px solid #dbeafe;
      box-sizing: border-box;
    }
    .back-top {
      background: linear-gradient(180deg, #0f4ab8, #1f7ae0);
      color: #fff;
      padding: 3mm;
      text-align: center;
      flex-shrink: 0;
    }
    .back-title {
      font-weight: 800;
      letter-spacing: 2px;
      font-size: 9px;
    }
    .back-subtitle {
      opacity: 0.9;
      font-size: 6.5px;
      margin-top: 1px;
    }
    .back-body {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 3mm;
      gap: 3mm;
      font-size: 6.5px;
      line-height: 1.3;
    }
    .back-left {
      width: 100%;
    }
    .back-row {
      margin-bottom: 2px;
    }
    .back-row .k {
      font-weight: 700;
      color: #0b3a8c;
    }
    .back-note {
      margin-top: 2mm;
      font-size: 6px;
      color: #334155;
    }
    .qr {
      width: 28mm;
      height: 28mm;
      border: 1px solid #dbeafe;
      border-radius: 2mm;
      background: #ffffff;
      object-fit: contain;
    }
    .qr-label {
      font-size: 6px;
      color: #0b3a8c;
      text-align: center;
      font-weight: 700;
    }
    .sign-box {
      padding: 0 3mm 3mm;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2mm;
      font-size: 6px;
      color: #334155;
      flex-shrink: 0;
    }
    .sign-line {
      width: 38mm;
      border-top: 1px solid #94a3b8;
      padding-top: 2px;
      text-align: center;
      color: #64748b;
    }
  </style>
</head>
<body>
  <!-- FRONT — vertical -->
  <div class="page">
    <div class="id-card">
      <div class="id-top">
        <div class="bank-name">NEOBANK</div>
        <div class="bank-tagline">Admin Identification Card</div>
      </div>
      <div class="id-photo-wrap">
        <div class="photo-circle">
            ${photoUrl
              ? `<img class="photo-img" src="${photoUrl}" alt="Photo" />`
              : `<span>👤</span>`}
        </div>
      </div>
      <div class="id-info">
        <div class="info-name">${name}</div>
        <div class="info-role">${designation}</div>
        ${department ? `<div class="info-row" style="text-align:center;"><span class="label">Dept:</span> ${department}</div>` : ''}
        ${employeeId ? `<div class="info-row"><span class="label">Emp ID:</span> ${employeeId}</div>` : ''}
        <div class="info-row"><span class="label">ID:</span> ${idNumber}</div>
        ${email ? `<div class="info-row"><span class="label">Email:</span> ${email}</div>` : ''}
        <div class="info-row"><span class="label">Address:</span> ${address || 'N/A'}</div>
      </div>
      <div class="id-bottom">
        <div class="barcode">
          <span></span><span></span><span></span><span></span><span></span>
          <span></span><span></span><span></span><span></span><span></span>
        </div>
        <div class="dates">
          <div>Issued: ${escapeHtml(issuedAtText)}</div>
          <div>Valid Till: ${escapeHtml(validTillText)}</div>
        </div>
      </div>
    </div>
  </div>

  <!-- BACK — vertical -->
  <div class="page break">
    <div class="back-card">
      <div class="back-top">
        <div class="back-title">NEOBANK</div>
        <div class="back-subtitle">Scan QR to verify</div>
      </div>
      <div class="back-body">
        <div class="back-left">
          <div class="back-row"><span class="k">Card No:</span> ${idNumber}</div>
          <div class="back-row"><span class="k">Name:</span> ${name}</div>
          <div class="back-row"><span class="k">Designation:</span> ${designation}</div>
          ${department ? `<div class="back-row"><span class="k">Department:</span> ${department}</div>` : ''}
          <div class="back-row"><span class="k">DOJ:</span> ${escapeHtml(dojText)}</div>
          <div class="back-row"><span class="k">Valid Till:</span> ${escapeHtml(validTillText)}</div>
          <div class="back-note">
            This card is the property of NeoBank. If found, please return to the nearest NeoBank branch.
          </div>
        </div>
        ${qrDataUrl ? `<img class="qr" src="${qrDataUrl}" alt="QR" />` : `<div class="qr"></div>`}
        <div class="qr-label">NEOBANK QR</div>
      </div>
      <div class="sign-box">
        <div>Issued By: ${escapeHtml(this.managerName || 'Manager')}</div>
        <div class="sign-line">Authorized Signature</div>
      </div>
    </div>
  </div>
  <script>
    window.onload = function () {
      setTimeout(function () {
        window.print();
      }, 200);
    };
  </script>
</body>
</html>`;

    const printWindow = window.open('', '_blank', 'width=600,height=400');
    if (printWindow) {
      printWindow.document.open();
      printWindow.document.write(html);
      printWindow.document.close();
    } else {
      this.alertService.error('Error', 'Popup blocked. Please allow popups to print the ID card.');
    }
  }

  logout() {
    // Store logout timestamp
    const logoutTime = new Date();
    
    // Calculate session duration in HH:MM:SS format
    let sessionDuration = '00:00:00';
    const adminData = sessionStorage.getItem('admin');
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        const loginTime = sessionStorage.getItem('adminLoginTime');
        
        if (loginTime) {
          const loginDate = new Date(loginTime);
          const diff = logoutTime.getTime() - loginDate.getTime();
          const hours = Math.floor(diff / (1000 * 60 * 60));
          const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
          const seconds = Math.floor((diff % (1000 * 60)) / 1000);
          sessionDuration = 
            String(hours).padStart(2, '0') + ':' +
            String(minutes).padStart(2, '0') + ':' +
            String(seconds).padStart(2, '0');
        }
        
        // Store logout data in sessionStorage
        const logoutData = {
          loginTime: loginTime,
          logoutTime: logoutTime.toISOString(),
          sessionDuration: sessionDuration
        };
        sessionStorage.setItem('adminLogoutData', JSON.stringify(logoutData));
        
        // Send logout data to backend - use MANAGER as userType for managers
        this.http.post(`${environment.apiBaseUrl}/api/session-history/logout`, {
          userId: admin.id,
          userType: 'MANAGER', // Managers should have userType MANAGER
          sessionDuration: sessionDuration
        }).subscribe({
          next: (response: any) => {
            console.log('Manager logout recorded successfully:', response);
          },
          error: (err) => {
            console.error('Error recording manager logout:', err);
          }
        });
      } catch (e) {
        console.error('Error updating logout time:', e);
      }
    }
    
    // Clear session timer
    if (this.sessionTimerHandle) {
      clearInterval(this.sessionTimerHandle);
      this.sessionTimerHandle = null;
    }
    if (this.sessionHistoryTimer) {
      clearInterval(this.sessionHistoryTimer);
      this.sessionHistoryTimer = null;
    }
    
    this.alertService.logoutSuccess();
    sessionStorage.removeItem('admin');
    sessionStorage.removeItem('userRole');
    this.router.navigate(['/admin/login']);
  }

  navigateTo(path: string) {
    this.router.navigate([`/admin/${path}`]);
  }

  // Load Investments, FDs, and EMIs
  loadManagerLoanOverview() {
    if (!isPlatformBrowser(this.platformId)) return;

    this.isLoadingManagerLoans = true;
    this.managerLoansError = '';

    const loans$ = this.http.get<any[]>(`${environment.apiBaseUrl}/api/loans`);
    const goldLoans$ = this.http.get<any[]>(`${environment.apiBaseUrl}/api/gold-loans`);
    const subsidyClaims$ = this.http.get<any[]>(`${environment.apiBaseUrl}/api/education-loan-subsidy-claims`);

    forkJoin([loans$, goldLoans$, subsidyClaims$]).subscribe({
      next: ([loans, goldLoans, subsidyClaims]) => {
        const subsidyByLoanAccount = new Map<string, any>();
        (subsidyClaims || []).forEach((claim: any) => {
          if (!claim || !claim.loanAccountNumber) {
            return;
          }
          const existing = subsidyByLoanAccount.get(claim.loanAccountNumber);
          if (!existing) {
            subsidyByLoanAccount.set(claim.loanAccountNumber, claim);
          } else {
            const priority: any = { Credited: 4, Approved: 3, Pending: 2, Rejected: 1 };
            const existingScore = priority[existing.status] || 0;
            const newScore = priority[claim.status] || 0;
            if (newScore > existingScore) {
              subsidyByLoanAccount.set(claim.loanAccountNumber, claim);
            }
          }
        });

        const rows: any[] = [];

        (loans || []).forEach((loan: any) => {
          if (!loan) return;
          const subsidy = loan.loanAccountNumber ? subsidyByLoanAccount.get(loan.loanAccountNumber) : null;
          const principalPaid = loan.principalPaid || 0;
          const interestPaid = loan.interestPaid || 0;
          const remainingPrincipal = loan.remainingPrincipal || 0;
          const remainingInterest = loan.remainingInterest || 0;

          rows.push({
            category: 'Term Loan',
            loanType: loan.type || 'Loan',
            loanAccountNumber: loan.loanAccountNumber,
            sbAccountNumber: loan.accountNumber,
            customerId: loan.customerId,
            userName: loan.userName,
            status: loan.status,
            sanctionedDate: loan.approvalDate,
            sanctionedBy: loan.approvedBy,
            principalPaid,
            interestPaid,
            totalPaid: principalPaid + interestPaid,
            outstanding: remainingPrincipal + remainingInterest,
            subsidyStatus: subsidy ? subsidy.status : null,
            subsidyAmount: subsidy ? subsidy.approvedSubsidyAmount : null
          });
        });

        (goldLoans || []).forEach((loan: any) => {
          if (!loan) return;
          const principalPaid = loan.principalPaid || 0;
          const interestPaid = loan.interestPaid || 0;
          const remainingPrincipal = loan.remainingPrincipal || 0;
          const remainingInterest = loan.remainingInterest || 0;

          rows.push({
            category: 'Gold Loan',
            loanType: 'Gold Loan',
            loanAccountNumber: loan.loanAccountNumber,
            sbAccountNumber: loan.accountNumber,
            customerId: loan.customerId,
            userName: loan.userName,
            status: loan.status,
            sanctionedDate: loan.approvalDate,
            sanctionedBy: loan.approvedBy,
            principalPaid,
            interestPaid,
            totalPaid: principalPaid + interestPaid,
            outstanding: remainingPrincipal + remainingInterest,
            subsidyStatus: null,
            subsidyAmount: null
          });
        });

        this.managerLoanOverview = rows;
        this.applyLoanOverviewFilters();
        this.isLoadingManagerLoans = false;
      },
      error: (err) => {
        console.error('Error loading manager loan overview:', err);
        this.managerLoansError = err.error?.message || 'Failed to load loan overview';
        this.managerLoanOverview = [];
        this.filteredManagerLoanOverview = [];
        this.isLoadingManagerLoans = false;
      }
    });
  }

  applyLoanOverviewFilters() {
    let rows = [...this.managerLoanOverview];

    if (this.loanOverviewTypeFilter !== 'ALL') {
      rows = rows.filter(r => r.category === this.loanOverviewTypeFilter);
    }

    if (this.loanOverviewStatusFilter !== 'ALL') {
      rows = rows.filter(r => r.status === this.loanOverviewStatusFilter);
    }

    if (this.loanOverviewSearchQuery && this.loanOverviewSearchQuery.trim() !== '') {
      const q = this.loanOverviewSearchQuery.toLowerCase();
      rows = rows.filter(r =>
        (r.loanAccountNumber && r.loanAccountNumber.toLowerCase().includes(q)) ||
        (r.sbAccountNumber && r.sbAccountNumber.toLowerCase().includes(q)) ||
        (r.userName && r.userName.toLowerCase().includes(q)) ||
        (r.customerId && String(r.customerId).toLowerCase().includes(q))
      );
    }

    this.filteredManagerLoanOverview = rows;
  }

  getManagerLoanTotals() {
    const totalLoans = this.managerLoanOverview.length;
    const goldLoans = this.managerLoanOverview.filter(r => r.category === 'Gold Loan').length;
    const withSubsidy = this.managerLoanOverview.filter(r => !!r.subsidyStatus).length;
    const totalPaid = this.managerLoanOverview.reduce((sum, r) => sum + (r.totalPaid || 0), 0);
    const totalOutstanding = this.managerLoanOverview.reduce((sum, r) => sum + (r.outstanding || 0), 0);

    return {
      totalLoans,
      goldLoans,
      withSubsidy,
      totalPaid,
      totalOutstanding
    };
  }

  formatLoanDate(dateTime: string): string {
    if (!dateTime) return 'N/A';
    try {
      const date = new Date(dateTime);
      return date.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch {
      return 'N/A';
    }
  }

  loadInvestments() {
    this.isLoadingInvestments = true;
    this.http.get(`${environment.apiBaseUrl}/api/investments`).subscribe({
      next: (investments: any) => {
        this.investments = investments || [];
        this.isLoadingInvestments = false;
      },
      error: (err: any) => {
        console.error('Error loading investments:', err);
        this.isLoadingInvestments = false;
      }
    });
  }

  loadFixedDeposits() {
    this.isLoadingFDs = true;
    this.http.get(`${environment.apiBaseUrl}/api/fixed-deposits`).subscribe({
      next: (fds: any) => {
        this.fixedDeposits = fds || [];
        this.isLoadingFDs = false;
      },
      error: (err: any) => {
        console.error('Error loading fixed deposits:', err);
        this.isLoadingFDs = false;
      }
    });
  }

  loadAllEMIs() {
    this.isLoadingEMIs = true;
    this.http.get(`${environment.apiBaseUrl}/api/emis/overdue`).subscribe({
      next: (emis: any) => {
        this.allEmis = emis || [];
        this.isLoadingEMIs = false;
      },
      error: (err: any) => {
        console.error('Error loading EMIs:', err);
        this.allEmis = [];
        this.isLoadingEMIs = false;
      }
    });
  }

  updateInvestment(investmentId: number, updates: any) {
    // Ensure ID is a number
    const investmentIdNum = typeof investmentId === 'number' ? investmentId : Number(investmentId);
    if (isNaN(investmentIdNum)) {
      this.alertService.error('Validation Error', 'Invalid investment ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/investments/${investmentIdNum}`, updates).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Investment updated successfully');
          this.loadInvestments();
        } else {
          this.alertService.error('Error', response.message || 'Failed to update investment');
        }
      },
      error: (err: any) => {
        console.error('Error updating investment:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update investment');
      }
    });
  }

  updateFixedDeposit(fdId: number, updates: any) {
    // Ensure ID is a number
    const fdIdNum = typeof fdId === 'number' ? fdId : Number(fdId);
    if (isNaN(fdIdNum)) {
      this.alertService.error('Validation Error', 'Invalid fixed deposit ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/fixed-deposits/${fdIdNum}`, updates).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit updated successfully');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to update FD');
        }
      },
      error: (err: any) => {
        console.error('Error updating FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update FD');
      }
    });
  }

  copyToClipboard(text: string) {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        this.alertService.success('Copied', 'Copied to clipboard!');
      }).catch(err => {
        console.error('Failed to copy:', err);
        this.fallbackCopyToClipboard(text);
      });
    } else {
      this.fallbackCopyToClipboard(text);
    }
  }

  fallbackCopyToClipboard(text: string) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    document.body.appendChild(textArea);
    textArea.select();
    try {
      document.execCommand('copy');
      this.alertService.success('Copied', 'Copied to clipboard!');
    } catch (err) {
      console.error('Fallback copy failed:', err);
      this.alertService.error('Error', 'Failed to copy to clipboard');
    }
    document.body.removeChild(textArea);
  }

  // ═══════════════════════ SALARY ACCOUNT METHODS ═══════════════════════

  private getEmptySalaryAccount(): SalaryAccount {
    return {
      employeeName: '', dob: '', mobileNumber: '', email: '',
      aadharNumber: '', panNumber: '',
      companyName: '', companyId: '', employerAddress: '', hrContactNumber: '',
      monthlySalary: 0, salaryCreditDate: 1, designation: '',
      branchName: '', ifscCode: ''
    };
  }

  loadSalaryAccounts(): void {
    this.isLoadingSalaryAccounts = true;
    this.salaryAccountService.getAll().subscribe({
      next: (list) => {
        this.salaryAccounts = list || [];
        this.filterSalaryAccounts();
        this.isLoadingSalaryAccounts = false;
      },
      error: (err) => {
        console.error('Failed to load salary accounts', err);
        this.isLoadingSalaryAccounts = false;
      }
    });
    this.salaryAccountService.getStats().subscribe({
      next: (stats) => { this.salaryStats = stats; },
      error: () => {}
    });
  }

  filterSalaryAccounts(): void {
    let list = this.salaryAccounts;
    if (this.salaryStatusFilter && this.salaryStatusFilter !== 'ALL') {
      list = list.filter(a => a.status === this.salaryStatusFilter);
    }
    if (this.salarySearchQuery) {
      const q = this.salarySearchQuery.toLowerCase();
      list = list.filter(a =>
        (a.employeeName || '').toLowerCase().includes(q) ||
        (a.companyName || '').toLowerCase().includes(q) ||
        (a.accountNumber || '').toLowerCase().includes(q) ||
        (a.designation || '').toLowerCase().includes(q)
      );
    }
    this.filteredSalaryAccounts = list;
  }

  searchSalaryAccounts(): void {
    if (!this.salarySearchQuery) {
      this.filterSalaryAccounts();
      return;
    }
    this.isLoadingSalaryAccounts = true;
    this.salaryAccountService.search(this.salarySearchQuery).subscribe({
      next: (list) => {
        this.salaryAccounts = list || [];
        this.filterSalaryAccounts();
        this.isLoadingSalaryAccounts = false;
      },
      error: () => { this.isLoadingSalaryAccounts = false; }
    });
  }

  createSalaryAccount(): void {
    if (!this.newSalaryAccount.employeeName) {
      this.alertService.error('Validation', 'Employee Name is required');
      return;
    }
    this.isCreatingSalaryAccount = true;
    this.salaryAccountService.createAccount(this.newSalaryAccount).subscribe({
      next: (res) => {
        this.alertService.success('Account Created',
          'Salary account created successfully.\nAccount No: ' + (res.account?.accountNumber || '') +
          '\nCustomer ID: ' + (res.account?.customerId || ''));
        this.newSalaryAccount = this.getEmptySalaryAccount();
        this.showSalaryForm = false;
        this.isCreatingSalaryAccount = false;
        this.loadSalaryAccounts();
      },
      error: (err) => {
        console.error('Create salary account error', err);
        this.alertService.error('Creation Failed', err?.error?.message || 'Unable to create salary account');
        this.isCreatingSalaryAccount = false;
      }
    });
  }

  viewSalaryAccountDetails(acc: SalaryAccount): void {
    this.selectedSalaryAccount = { ...acc };
    this.salaryTransactions = [];
    this.isLoadingSalaryTxns = true;
    this.salaryAccountService.getTransactions(acc.id!).subscribe({
      next: (txns) => { this.salaryTransactions = txns || []; this.isLoadingSalaryTxns = false; },
      error: () => { this.isLoadingSalaryTxns = false; }
    });
  }

  openEditSalaryAccount(acc: SalaryAccount): void {
    this.editingSalaryAccount = { ...acc };
  }

  saveSalaryAccountEdit(): void {
    if (!this.editingSalaryAccount || !this.editingSalaryAccount.id) return;
    this.isSavingSalaryAccount = true;
    this.salaryAccountService.updateAccount(this.editingSalaryAccount.id, this.editingSalaryAccount).subscribe({
      next: () => {
        this.alertService.success('Updated', 'Salary account updated successfully');
        this.editingSalaryAccount = null;
        this.isSavingSalaryAccount = false;
        this.loadSalaryAccounts();
      },
      error: (err) => {
        this.alertService.error('Update Failed', err?.error?.message || 'Unable to update');
        this.isSavingSalaryAccount = false;
      }
    });
  }

  creditSalaryToAccount(acc: SalaryAccount): void {
    if (!confirm('Credit ₹' + (acc.monthlySalary || 0) + ' salary to ' + acc.employeeName + '?')) return;
    this.salaryAccountService.creditSalary(acc.id!).subscribe({
      next: (res) => {
        this.alertService.success('Salary Credited', 'Salary of ₹' + (acc.monthlySalary || 0) + ' credited to ' + acc.employeeName);
        this.loadSalaryAccounts();
      },
      error: (err) => {
        this.alertService.error('Credit Failed', err?.error?.message || 'Unable to credit salary');
      }
    });
  }

  toggleFreezeSalaryAccount(acc: SalaryAccount): void {
    if (acc.status === 'Frozen') {
      this.salaryAccountService.unfreezeAccount(acc.id!).subscribe({
        next: () => { this.alertService.success('Unfrozen', 'Account unfrozen'); this.loadSalaryAccounts(); },
        error: () => { this.alertService.error('Error', 'Unable to unfreeze account'); }
      });
    } else {
      if (!confirm('Freeze salary account of ' + acc.employeeName + '?')) return;
      this.salaryAccountService.freezeAccount(acc.id!).subscribe({
        next: () => { this.alertService.success('Frozen', 'Account frozen'); this.loadSalaryAccounts(); },
        error: () => { this.alertService.error('Error', 'Unable to freeze account'); }
      });
    }
  }

  // ─── Close Account Methods ────────────────────────────────

  openCloseAccountModal(acc: SalaryAccount): void {
    this.closingAccount = { ...acc };
    this.closeAccountPreCheck = null;
    this.closeAccountReason = '';
    this.isLoadingClosePreCheck = true;
    this.isClosingAccount = false;

    this.salaryAccountService.closeAccountPreCheck(acc.id!).subscribe({
      next: (data) => {
        this.closeAccountPreCheck = data;
        this.isLoadingClosePreCheck = false;
      },
      error: (err) => {
        this.isLoadingClosePreCheck = false;
        this.alertService.error('Error', err?.error?.message || 'Failed to check account closure eligibility');
        this.closingAccount = null;
      }
    });
  }

  confirmCloseAccount(): void {
    if (!this.closingAccount || !this.closeAccountPreCheck?.canClose) return;
    if (!this.closeAccountReason?.trim()) {
      this.alertService.error('Validation', 'Please enter a reason for closing the account');
      return;
    }
    if (!confirm('This action is PERMANENT. Close salary account of ' + this.closingAccount.employeeName + '?')) return;

    this.isClosingAccount = true;
    this.salaryAccountService.closeAccount(this.closingAccount.id!, this.closeAccountReason.trim(), this.managerName).subscribe({
      next: (res) => {
        this.isClosingAccount = false;
        this.alertService.success('Account Closed', 'Salary account of ' + this.closingAccount!.employeeName + ' has been closed permanently.');
        this.closingAccount = null;
        this.closeAccountPreCheck = null;
        this.closeAccountReason = '';
        this.loadSalaryAccounts();
      },
      error: (err) => {
        this.isClosingAccount = false;
        const msg = err?.error?.message || 'Unable to close account';
        const blockers = err?.error?.blockers;
        if (blockers && blockers.length) {
          this.alertService.error('Cannot Close Account', msg + '\n' + blockers.join('\n'));
        } else {
          this.alertService.error('Close Failed', msg);
        }
      }
    });
  }

  // ═══════════════════════════════════════════════════════════════
  // MANAGER PERMISSIONS — Admin Salary Account Linking & Monitoring
  // ═══════════════════════════════════════════════════════════════

  mpSelectAdmin(admin: any): void {
    this.mpSelectedAdmin = admin;
    this.mpAdminSalaryAccount = null;
    this.mpSalaryTransactions = [];
    this.mpNormalTransactions = [];
    this.mpSalaryReport = null;
    this.mpLinkAccountNumber = '';
    this.mpLinked = !!(admin.salaryAccountLinked || admin.salaryAccountNumber);
    this.mpActiveTab = 'overview';
    if (this.mpLinked) {
      this.mpLoadAdminSalaryAccount(admin.id);
    }
  }

  mpLinkSalaryAccount(): void {
    if (!this.mpSelectedAdmin || !this.mpLinkAccountNumber) {
      this.alertService.error('Validation', 'Please select an admin and enter a salary account number');
      return;
    }
    if (!confirm('This is a ONE-TIME operation. Once linked, the salary account cannot be changed. Proceed?')) return;
    this.mpIsLinking = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/link-salary-account/${this.mpSelectedAdmin.id}`, {
      salaryAccountNumber: this.mpLinkAccountNumber
    }).subscribe({
      next: (res) => {
        this.mpIsLinking = false;
        if (res.success) {
          this.alertService.success('Linked', res.message);
          this.mpLinked = true;
          this.mpSelectedAdmin.salaryAccountLinked = true;
          this.mpSelectedAdmin.salaryAccountNumber = this.mpLinkAccountNumber;
          this.mpLinkAccountNumber = '';
          this.mpLoadAdminSalaryAccount(this.mpSelectedAdmin.id);
          this.loadAllAdmins();
        } else {
          this.alertService.error('Failed', res.message);
        }
      },
      error: (err) => {
        this.mpIsLinking = false;
        this.alertService.error('Error', err.error?.message || 'Failed to link salary account');
      }
    });
  }

  mpLoadAdminSalaryAccount(adminId: number): void {
    this.mpIsLoadingSalary = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/salary-account/${adminId}`).subscribe({
      next: (res) => {
        this.mpIsLoadingSalary = false;
        if (res.success) {
          this.mpAdminSalaryAccount = res.salaryAccount;
          this.mpLinked = true;
        } else {
          this.mpLinked = false;
        }
      },
      error: () => {
        this.mpIsLoadingSalary = false;
      }
    });
  }

  mpLoadTransactions(): void {
    if (!this.mpSelectedAdmin) return;
    this.mpIsLoadingTxns = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/salary-account/${this.mpSelectedAdmin.id}/transactions`).subscribe({
      next: (res) => {
        this.mpIsLoadingTxns = false;
        if (res.success) {
          this.mpSalaryTransactions = res.salaryTransactions || [];
          this.mpNormalTransactions = res.normalTransactions || [];
        }
      },
      error: () => { this.mpIsLoadingTxns = false; }
    });
  }

  mpLoadReport(): void {
    if (!this.mpSelectedAdmin) return;
    this.mpIsLoadingReport = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/salary-account/${this.mpSelectedAdmin.id}/report`).subscribe({
      next: (res) => {
        this.mpIsLoadingReport = false;
        if (res.success) {
          this.mpSalaryReport = res;
        }
      },
      error: () => { this.mpIsLoadingReport = false; }
    });
  }

  mpSetTab(tab: string): void {
    this.mpActiveTab = tab;
    if (tab === 'transactions' && this.mpSalaryTransactions.length === 0) {
      this.mpLoadTransactions();
    } else if (tab === 'report' && !this.mpSalaryReport) {
      this.mpLoadReport();
    }
  }

  mpPrintSalaryReport(): void {
    if (!this.mpSalaryReport) return;
    const r = this.mpSalaryReport;
    const html = `
    <html><head><title>Salary Report - ${r.adminName}</title>
    <style>
      body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 40px; color: #1e293b; }
      .report { max-width: 700px; margin: 0 auto; border: 2px solid #6366f1; border-radius: 12px; overflow: hidden; }
      .report-header { background: linear-gradient(135deg, #6366f1, #4f46e5); color: white; padding: 24px 32px; }
      .report-header h1 { margin: 0; font-size: 22px; } .report-header .sub { font-size: 13px; opacity: .9; }
      .report-body { padding: 32px; }
      .report-title { text-align: center; font-size: 18px; font-weight: 700; margin-bottom: 24px; color: #6366f1; border-bottom: 2px dashed #e2e8f0; padding-bottom: 16px; }
      .report-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f1f5f9; }
      .report-row .label { color: #64748b; font-size: 14px; } .report-row .value { font-weight: 600; font-size: 14px; }
      .report-total { text-align: center; background: #f0fdf4; padding: 20px; border-radius: 10px; margin: 20px 0; border: 1px solid #bbf7d0; }
      .report-total .label { font-size: 13px; color: #16a34a; } .report-total .amount { font-size: 32px; font-weight: 700; color: #16a34a; }
      .report-footer { text-align: center; font-size: 12px; color: #94a3b8; padding: 16px 32px; border-top: 1px solid #e2e8f0; }
      @media print { body { padding: 0; } .report { border: none; } }
    </style></head><body>
    <div class="report">
      <div class="report-header"><div><h1>NEO BANK</h1><div class="sub">Manager Salary Report</div></div></div>
      <div class="report-body">
        <div class="report-title">Salary Report</div>
        <div class="report-row"><span class="label">Admin Name</span><span class="value">${r.adminName}</span></div>
        <div class="report-row"><span class="label">Email</span><span class="value">${r.adminEmail}</span></div>
        <div class="report-row"><span class="label">Account Number</span><span class="value">${r.accountNumber}</span></div>
        <div class="report-row"><span class="label">Company</span><span class="value">${r.companyName || '-'}</span></div>
        <div class="report-row"><span class="label">Designation</span><span class="value">${r.designation || '-'}</span></div>
        <div class="report-row"><span class="label">Monthly Salary</span><span class="value">&#8377;${(r.monthlySalary || 0).toLocaleString('en-IN')}</span></div>
        <div class="report-row"><span class="label">Current Balance</span><span class="value">&#8377;${(r.currentBalance || 0).toLocaleString('en-IN')}</span></div>
        <div class="report-row"><span class="label">Total Credits</span><span class="value">${r.totalCredits || 0}</span></div>
        <div class="report-row"><span class="label">Account Status</span><span class="value">${r.accountStatus || '-'}</span></div>
        <div class="report-total"><div class="label">TOTAL SALARY CREDITED</div><div class="amount">&#8377;${(r.totalSalaryCredited || 0).toLocaleString('en-IN')}</div></div>
      </div>
      <div class="report-footer">Generated on ${new Date().toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })} by Manager | NEO BANK</div>
    </div></body></html>`;
    const win = window.open('', '_blank');
    if (win) { win.document.write(html); win.document.close(); setTimeout(() => { win.print(); }, 500); }
  }

  // ─── Gold Loan Management ─────────────────────────────────

  loadMgrGoldLoans() {
    this.isLoadingMgrGoldLoans = true;
    this.salaryAccountService.getAllGoldLoans().subscribe({
      next: (loans) => {
        this.mgrGoldLoans = loans || [];
        this.filterMgrGoldLoans();
        this.isLoadingMgrGoldLoans = false;
      },
      error: () => { this.isLoadingMgrGoldLoans = false; }
    });
  }

  filterMgrGoldLoans() {
    if (this.mgrGoldLoanFilter === 'ALL') {
      this.mgrFilteredGoldLoans = this.mgrGoldLoans;
    } else {
      this.mgrFilteredGoldLoans = this.mgrGoldLoans.filter(l => l.status === this.mgrGoldLoanFilter);
    }
  }

  getMgrPendingGoldLoansCount(): number {
    return this.mgrGoldLoans.filter(l => l.status === 'Pending').length;
  }

  loadMgrGoldRate() {
    this.salaryAccountService.getCurrentGoldRate().subscribe({
      next: (rate) => { this.mgrCurrentGoldRate = rate; },
      error: () => {}
    });
  }

  loadMgrGoldRateHistory() {
    this.isLoadingGoldRateHistory = true;
    this.salaryAccountService.getGoldRateHistory().subscribe({
      next: (history) => { this.mgrGoldRateHistory = history || []; this.isLoadingGoldRateHistory = false; },
      error: () => { this.isLoadingGoldRateHistory = false; }
    });
  }

  updateMgrGoldRate() {
    if (!this.mgrNewGoldRate || this.mgrNewGoldRate <= 0) {
      this.alertService.error('Validation', 'Please enter a valid gold rate');
      return;
    }
    this.isUpdatingGoldRate = true;
    const updatedBy = this.managerName || 'Manager';
    this.salaryAccountService.updateGoldRate(this.mgrNewGoldRate, updatedBy).subscribe({
      next: () => {
        this.isUpdatingGoldRate = false;
        this.alertService.success('Gold Rate Updated', 'Gold rate has been updated successfully');
        this.mgrNewGoldRate = null;
        this.loadMgrGoldRate();
        this.loadMgrGoldRateHistory();
      },
      error: (err) => { this.isUpdatingGoldRate = false; this.alertService.error('Error', err.error?.message || 'Failed to update gold rate'); }
    });
  }

  viewMgrGoldLoanDetails(loan: any) {
    this.mgrSelectedGoldLoan = loan;
    this.mgrGoldVerification = {
      goldItems: loan.goldItems || '',
      goldPurity: loan.goldPurity || '22K',
      verifiedGoldGrams: loan.verifiedGoldGrams || loan.goldGrams,
      verificationNotes: loan.verificationNotes || '',
      storageLocation: loan.storageLocation || ''
    };
    if (loan.loanAccountNumber) {
      this.isLoadingMgrEmi = true;
      this.salaryAccountService.getGoldLoanEmiSchedule(loan.loanAccountNumber).subscribe({
        next: (schedule) => { this.mgrGoldLoanEmiSchedule = schedule || []; this.isLoadingMgrEmi = false; },
        error: () => { this.isLoadingMgrEmi = false; }
      });
    }
  }

  closeMgrGoldLoanDetails() {
    this.mgrSelectedGoldLoan = null;
    this.mgrGoldLoanEmiSchedule = [];
  }

  approveMgrGoldLoan(loan: any) {
    const approvedBy = this.managerName || 'Manager';
    const verificationData = {
      goldItems: this.mgrGoldVerification.goldItems,
      goldPurity: this.mgrGoldVerification.goldPurity,
      verifiedGoldGrams: this.mgrGoldVerification.verifiedGoldGrams,
      verificationNotes: this.mgrGoldVerification.verificationNotes,
      storageLocation: this.mgrGoldVerification.storageLocation
    };
    this.salaryAccountService.approveGoldLoan(loan.id, 'Approved', approvedBy, verificationData).subscribe({
      next: () => {
        this.alertService.success('Loan Approved', 'Gold loan has been approved successfully');
        this.mgrSelectedGoldLoan = null;
        this.loadMgrGoldLoans();
      },
      error: (err) => { this.alertService.error('Error', err.error?.message || 'Failed to approve loan'); }
    });
  }

  rejectMgrGoldLoan(loan: any) {
    const rejectedBy = this.managerName || 'Manager';
    this.salaryAccountService.approveGoldLoan(loan.id, 'Rejected', rejectedBy).subscribe({
      next: () => {
        this.alertService.success('Loan Rejected', 'Gold loan has been rejected');
        this.mgrSelectedGoldLoan = null;
        this.loadMgrGoldLoans();
      },
      error: (err) => { this.alertService.error('Error', err.error?.message || 'Failed to reject loan'); }
    });
  }

  printMgrGoldLoanReceipt(loan: any) {
    const receiptWindow = window.open('', '_blank');
    if (!receiptWindow) return;
    const html = `<!DOCTYPE html><html><head><title>Gold Loan Receipt - Manager Copy</title>
    <style>body{font-family:Arial,sans-serif;padding:40px;max-width:800px;margin:0 auto}
    .header{text-align:center;border-bottom:3px solid #1a237e;padding-bottom:20px;margin-bottom:30px}
    .header h1{color:#1a237e;margin:0;font-size:28px}
    .header p{color:#666;margin:5px 0}
    .badge{display:inline-block;padding:4px 16px;border-radius:12px;font-weight:600;font-size:13px}
    .badge.approved{background:#e8f5e9;color:#2e7d32}
    .badge.pending{background:#fff3e0;color:#e65100}
    .badge.rejected{background:#ffebee;color:#c62828}
    .section{margin-bottom:24px}
    .section h3{color:#1a237e;border-bottom:1px solid #e0e0e0;padding-bottom:8px;margin-bottom:12px}
    .grid{display:grid;grid-template-columns:1fr 1fr;gap:8px}
    .row{display:flex;justify-content:space-between;padding:8px 12px;border-bottom:1px solid #f5f5f5}
    .label{color:#666}
    .value{font-weight:600}
    .total-row{background:#e8eaf6;padding:12px;border-radius:8px;display:flex;justify-content:space-between;font-size:18px;font-weight:700;color:#1a237e;margin-top:12px}
    .footer{text-align:center;margin-top:40px;padding-top:20px;border-top:3px solid #1a237e;color:#888;font-size:11px}
    .stamp{text-align:right;margin-top:30px;font-style:italic;color:#666}
    @media print{body{padding:20px}}</style></head><body>
    <div class="header"><h1>NeoBank</h1><p>Gold Loan Receipt — Manager Copy</p>
    <p>Date: ${new Date().toLocaleDateString('en-IN', {day:'2-digit',month:'long',year:'numeric'})}</p></div>
    <div class="section"><h3>Loan Information</h3>
    <div class="row"><span class="label">Loan Account</span><span class="value">${loan.loanAccountNumber || 'Pending'}</span></div>
    <div class="row"><span class="label">Application Date</span><span class="value">${new Date(loan.applicationDate).toLocaleDateString('en-IN')}</span></div>
    <div class="row"><span class="label">Status</span><span class="badge ${(loan.status||'').toLowerCase()}">${loan.status}</span></div>
    <div class="row"><span class="label">Purpose</span><span class="value">${loan.purpose || 'Gold Loan'}</span></div></div>
    <div class="section"><h3>Gold Details</h3>
    <div class="row"><span class="label">Gold Weight (Applied)</span><span class="value">${loan.goldGrams} grams</span></div>
    ${loan.verifiedGoldGrams ? `<div class="row"><span class="label">Gold Weight (Verified)</span><span class="value">${loan.verifiedGoldGrams} grams</span></div>` : ''}
    <div class="row"><span class="label">Gold Rate/gram</span><span class="value">&#8377;${(loan.goldRatePerGram||0).toLocaleString('en-IN')}</span></div>
    <div class="row"><span class="label">Gold Value</span><span class="value">&#8377;${(loan.goldValue||0).toLocaleString('en-IN')}</span></div>
    ${loan.goldPurity ? `<div class="row"><span class="label">Gold Purity</span><span class="value">${loan.goldPurity}</span></div>` : ''}
    ${loan.goldItems ? `<div class="row"><span class="label">Gold Items</span><span class="value">${loan.goldItems}</span></div>` : ''}
    ${loan.storageLocation ? `<div class="row"><span class="label">Storage Location</span><span class="value">${loan.storageLocation}</span></div>` : ''}</div>
    <div class="section"><h3>Loan Terms</h3>
    <div class="row"><span class="label">Tenure</span><span class="value">${loan.tenure || 12} months</span></div>
    <div class="row"><span class="label">Interest Rate</span><span class="value">${loan.interestRate || 9}% p.a.</span></div>
    <div class="total-row"><span>Loan Amount (75%)</span><span>&#8377;${(loan.loanAmount||0).toLocaleString('en-IN')}</span></div></div>
    <div class="section"><h3>Applicant Details</h3>
    <div class="row"><span class="label">Name</span><span class="value">${loan.userName}</span></div>
    <div class="row"><span class="label">Account Number</span><span class="value">${loan.accountNumber}</span></div>
    <div class="row"><span class="label">Email</span><span class="value">${loan.userEmail || 'N/A'}</span></div></div>
    ${loan.approvedBy ? `<div class="section"><h3>Approval</h3><div class="row"><span class="label">Approved By</span><span class="value">${loan.approvedBy}</span></div>
    <div class="row"><span class="label">Approval Date</span><span class="value">${loan.approvalDate ? new Date(loan.approvalDate).toLocaleDateString('en-IN') : 'N/A'}</span></div></div>` : ''}
    <div class="stamp">Authorized Signatory: ____________________</div>
    <div class="footer"><p>This is a computer generated receipt from NeoBank | Manager Copy</p><p>For queries, contact support&#64;neobank.com</p></div></body></html>`;
    receiptWindow.document.write(html);
    receiptWindow.document.close();
    receiptWindow.print();
  }

  // ─── Customer Data Upload (Excel) ─────────────────────────────

  onCustomerDataFileSelect(event: any) {
    this.customerDataFile = event.target.files[0] || null;
    this.customerDataUploadResult = null;
  }

  uploadCustomerDataExcel() {
    if (!this.customerDataFile) {
      this.alertService.error('Error', 'Please select an Excel file');
      return;
    }
    const filename = this.customerDataFile.name;
    if (!filename.endsWith('.xlsx') && !filename.endsWith('.xls')) {
      this.alertService.error('Error', 'Only .xlsx and .xls files are supported');
      return;
    }
    this.isUploadingCustomerData = true;
    this.customerDataUploadResult = null;
    const formData = new FormData();
    formData.append('file', this.customerDataFile);
    formData.append('uploadedBy', this.managerName || 'Manager');
    this.http.post<any>(`${environment.apiBaseUrl}/api/preloaded-customer-data/upload-excel`, formData).subscribe({
      next: (result) => {
        this.isUploadingCustomerData = false;
        this.customerDataUploadResult = result;
        if (result.success) {
          this.alertService.success('Upload Successful', `${result.savedCount} records saved (Batch: ${result.batchId})`);
          this.customerDataFile = null;
          this.loadCustomerDataStats();
          this.loadUploadedCustomerData();
        } else {
          this.alertService.error('Upload Failed', result.message || 'Failed to process Excel');
        }
      },
      error: (err) => {
        this.isUploadingCustomerData = false;
        this.alertService.error('Upload Error', err.error?.message || 'Failed to upload Excel file');
      }
    });
  }

  loadCustomerDataStats() {
    this.http.get<any>(`${environment.apiBaseUrl}/api/preloaded-customer-data/statistics`).subscribe({
      next: (stats) => this.customerDataStats = stats,
      error: () => {}
    });
  }

  loadUploadedCustomerData() {
    this.isLoadingCustomerData = true;
    const url = this.customerDataViewMode === 'unused'
      ? `${environment.apiBaseUrl}/api/preloaded-customer-data/unused`
      : `${environment.apiBaseUrl}/api/preloaded-customer-data/all`;
    this.http.get<any[]>(url).subscribe({
      next: (data) => {
        this.uploadedCustomerData = data || [];
        this.isLoadingCustomerData = false;
      },
      error: () => {
        this.uploadedCustomerData = [];
        this.isLoadingCustomerData = false;
      }
    });
  }

  getFilteredCustomerData(): any[] {
    if (!this.customerDataSearchQuery?.trim()) return this.uploadedCustomerData;
    const q = this.customerDataSearchQuery.toLowerCase();
    return this.uploadedCustomerData.filter((d: any) =>
      (d.aadharNumber && d.aadharNumber.includes(q)) ||
      (d.panNumber && d.panNumber.toLowerCase().includes(q)) ||
      (d.fullName && d.fullName.toLowerCase().includes(q)) ||
      (d.phone && d.phone.includes(q)) ||
      (d.email && d.email.toLowerCase().includes(q))
    );
  }

  deleteAllCustomerData() {
    const total = this.customerDataStats.total || this.uploadedCustomerData.length;
    if (!confirm(`⚠️ DELETE ALL DATA PERMANENTLY\n\nThis will permanently delete all ${total} customer data records.\nThis action CANNOT be undone.\n\nAre you sure?`)) return;
    if (!confirm(`FINAL CONFIRMATION\n\nYou are about to delete ALL ${total} records permanently.\nType OK to proceed.`)) return;
    this.isLoadingCustomerData = true;
    this.http.delete<any>(`${environment.apiBaseUrl}/api/preloaded-customer-data/delete-all`).subscribe({
      next: (res) => {
        this.alertService.success('Deleted', `All ${res.deletedCount} records deleted permanently`);
        this.uploadedCustomerData = [];
        this.loadCustomerDataStats();
        this.isLoadingCustomerData = false;
      },
      error: () => {
        this.alertService.error('Error', 'Failed to delete all records');
        this.isLoadingCustomerData = false;
      }
    });
  }

  deleteCustomerDataRecord(id: number) {
    if (!confirm('Are you sure you want to delete this record?')) return;
    this.http.delete<any>(`${environment.apiBaseUrl}/api/preloaded-customer-data/${id}`).subscribe({
      next: () => {
        this.alertService.success('Deleted', 'Record deleted successfully');
        this.loadUploadedCustomerData();
        this.loadCustomerDataStats();
      },
      error: () => this.alertService.error('Error', 'Failed to delete record')
    });
  }

  downloadCustomerTemplate(accountType: string = 'Savings') {
    this.http.get(`${environment.apiBaseUrl}/api/preloaded-customer-data/download-template?accountType=${accountType}`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = accountType.toLowerCase() + '_account_template.xlsx';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.alertService.error('Error', 'Failed to download template')
    });
  }

  // ─── CIBIL Reports Upload Methods ─────────────────────────────

  onCibilFileSelect(event: any) {
    this.cibilFile = event.target.files[0] || null;
    this.cibilUploadResult = null;
  }

  uploadCibilReport() {
    if (!this.cibilFile) {
      this.alertService.error('Error', 'Please select a file');
      return;
    }

    this.isUploadingCibil = true;
    this.cibilUploadResult = null;
    const formData = new FormData();
    formData.append('file', this.cibilFile);
    formData.append('uploadedBy', this.managerName || 'Manager');

    let url = '';
    if (this.cibilUploadType === 'excel') {
      url = `${environment.apiBaseUrl}/api/cibil-reports/upload-excel`;
    } else if (this.cibilUploadType === 'pdf') {
      url = `${environment.apiBaseUrl}/api/cibil-reports/upload-pdf`;
    } else if (this.cibilUploadType === 'image') {
      url = `${environment.apiBaseUrl}/api/cibil-reports/upload-image`;
      formData.append('panNumber', this.cibilImagePan);
      if (this.cibilImageName) formData.append('name', this.cibilImageName);
      if (this.cibilImageSalary) formData.append('salary', this.cibilImageSalary);
      if (this.cibilImageScore) formData.append('cibilScore', this.cibilImageScore);
      if (this.cibilImageLimit) formData.append('approvalLimit', this.cibilImageLimit);
      if (this.cibilImageStatus) formData.append('status', this.cibilImageStatus);
      if (this.cibilImageRemarks) formData.append('remarks', this.cibilImageRemarks);
    }

    this.http.post<any>(url, formData).subscribe({
      next: (result) => {
        this.isUploadingCibil = false;
        this.cibilUploadResult = result;
        if (result.success) {
          this.alertService.success('Upload Successful', `${result.savedCount} CIBIL records processed (Batch: ${result.batchId})`);
          this.cibilFile = null;
          this.clearCibilImageFields();
          this.loadCibilStats();
          this.loadCibilReports();
        } else {
          this.alertService.error('Upload Failed', result.error || 'Failed to process file');
        }
      },
      error: (err) => {
        this.isUploadingCibil = false;
        this.alertService.error('Upload Error', err.error?.error || 'Failed to upload CIBIL file');
      }
    });
  }

  clearCibilImageFields() {
    this.cibilImagePan = '';
    this.cibilImageName = '';
    this.cibilImageSalary = '';
    this.cibilImageScore = '';
    this.cibilImageLimit = '';
    this.cibilImageStatus = '';
    this.cibilImageRemarks = '';
  }

  loadCibilStats() {
    this.http.get<any>(`${environment.apiBaseUrl}/api/cibil-reports/statistics/${this.managerName}`).subscribe({
      next: (stats) => this.cibilStats = stats,
      error: () => {}
    });
  }

  loadCibilReports() {
    this.isLoadingCibil = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/cibil-reports/manager/${this.managerName}`).subscribe({
      next: (data) => {
        this.cibilReports = data || [];
        this.isLoadingCibil = false;
      },
      error: () => {
        this.cibilReports = [];
        this.isLoadingCibil = false;
      }
    });
  }

  getFilteredCibilReports(): any[] {
    let filtered = this.cibilReports;
    if (this.cibilStatusFilter !== 'ALL') {
      filtered = filtered.filter((r: any) => r.status === this.cibilStatusFilter);
    }
    if (this.cibilSearchQuery?.trim()) {
      const q = this.cibilSearchQuery.toLowerCase();
      filtered = filtered.filter((r: any) =>
        (r.panNumber && r.panNumber.toLowerCase().includes(q)) ||
        (r.name && r.name.toLowerCase().includes(q)) ||
        (r.riskCategory && r.riskCategory.toLowerCase().includes(q))
      );
    }
    return filtered;
  }

  deleteCibilReport(id: number) {
    if (!confirm('Delete this CIBIL report?')) return;
    this.http.delete<any>(`${environment.apiBaseUrl}/api/cibil-reports/${id}`).subscribe({
      next: () => {
        this.alertService.success('Deleted', 'CIBIL report deleted');
        this.loadCibilReports();
        this.loadCibilStats();
      },
      error: () => this.alertService.error('Error', 'Failed to delete report')
    });
  }

  downloadCibilTemplate() {
    this.http.get(`${environment.apiBaseUrl}/api/cibil-reports/download-template`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'cibil_upload_template.xlsx';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.alertService.error('Error', 'Failed to download template')
    });
  }

  getCibilStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      case 'NOT_ELIGIBLE': return 'status-not-eligible';
      case 'PENDING': return 'status-pending';
      default: return '';
    }
  }

  getRiskCategoryClass(category: string): string {
    switch (category) {
      case 'LOW': return 'risk-low';
      case 'MEDIUM': return 'risk-medium';
      case 'HIGH': return 'risk-high';
      case 'VERY_HIGH': return 'risk-very-high';
      default: return '';
    }
  }
}

