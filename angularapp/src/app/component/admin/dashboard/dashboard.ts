import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { FaceAuthService } from '../../../service/face-auth.service';
import { PaymentGatewayService } from '../../../service/payment-gateway.service';
import { FasttagAdmin } from '../fasttag/fasttag-admin';
import { environment } from '../../../../environment/environment';
import { forkJoin, of } from 'rxjs';
import { catchError, timeout, finalize } from 'rxjs/operators';
import {
  UserProfile,
  CardDetails,
  LoanDetails,
  TransactionRecord,
  DepositRequest,
  AccountTracking,
  AdminUser,
  AdminDashboardStats,
  AdminLoginActivity,
  AdminAuditLog,
  SupportTicket,
  GoldLoan,
  ProfileUpdateRequest,
  DailyActivityReport
} from '../../../model/admin/admin.model';

// Keep locally for template binding - TransactionDetails used in template
interface TransactionDetails {
  id: string;
  date: string;
  type: string;
  amount: number;
  description: string;
  balance: number;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
  encapsulation: ViewEncapsulation.None,
  imports: [FormsModule, CommonModule, FasttagAdmin],
  standalone: true
})
export class Dashboard implements OnInit, OnDestroy {
  users: UserProfile[] = [];
  selectedUser: UserProfile | null = null;
  selectedUserId: string = '';
  isLoadingInitialData: boolean = true;
  showDepositWithdrawal: boolean = false;
  operationType: 'deposit' | 'withdrawal' = 'deposit';
  amount: number = 0;
  description: string = '';
  successMessage: string = '';
  errorMessage: string = '';

  // Account verification fields
  accountNumber: string = '';
  accountType: 'regular' | 'loan' | 'goldloan' | 'cheque' | 'salary' | 'current' = 'regular';
  isVerifyingAccount: boolean = false;
  isAccountVerified: boolean = false;
  accountVerificationError: string = '';
  verifiedAccountDetails: any = null;

  // Deposit Requests
  depositRequests: DepositRequest[] = [];
  isLoadingDepositRequests: boolean = false;

  // Receipt fields
  showReceipt: boolean = false;
  receiptData: any = null;

  // Transaction history
  adminTransactionHistory: TransactionRecord[] = [];
  
  // Session tracking properties
  loginTime: Date | null = null;
  logoutTime: Date | null = null;
  sessionDuration: string = '00:00:00';
  sessionTimer: any = null;

  // User update history
  userUpdateHistory: any[] = [];
  showUpdateHistory: boolean = false;
  updateHistoryFilter: string = 'ALL'; // ALL, TODAY, WEEK, MONTH
  updateHistorySearchQuery: string = '';
  
  // Profile update requests
  profileUpdateRequests: any[] = [];
  pendingProfileUpdates: any[] = [];
  isLoadingProfileUpdates: boolean = false;
  profileUpdateFilter: string = 'PENDING'; // PENDING, ALL, APPROVED, REJECTED, COMPLETED
  profileUpdateSearchQuery: string = '';
  
  // Profile update history
  profileUpdateHistory: any[] = [];
  isLoadingProfileUpdateHistory: boolean = false;
  profileUpdateHistoryFilter: string = 'ALL'; // ALL, TODAY, WEEK, MONTH
  profileUpdateHistorySearchQuery: string = '';

  // Payment Gateway Management
  pgPendingMerchants: any[] = [];
  pgApprovedMerchants: any[] = [];
  pgIsLoading = false;
  pgActiveView = 'pending'; // pending | approved
  pgLinkAccountForm = { merchantId: '', accountNumber: '', accountType: 'CURRENT' };
  pgLinkVerifyResult: any = null;
  pgRejectReason = '';
  pgRejectingMerchant: any = null;
  // Merchant Details Drill-Down
  pgSelectedMerchant: any = null;
  pgMerchantDetailTab = 'overview'; // overview | orders | transactions | refunds
  pgMerchantOrders: any[] = [];
  pgMerchantTransactions: any[] = [];
  pgMerchantRefunds: any[] = [];
  pgMerchantAnalytics: any = null;
  pgMerchantDetailsLoading = false;

  // Daily Activity Report
  showDailyActivityReport: boolean = false;
  selectedDate: string = new Date().toISOString().split('T')[0]; // Today's date
  dailyActivities: any[] = [];
  activityCategories: string[] = ['ALL', 'UPDATES', 'DEPOSITS', 'WITHDRAWALS', 'LOANS', 'GOLD_LOANS', 'CHEQUES', 'SUBSIDY_CLAIMS', 'ACCOUNTS', 'OTHER'];
  selectedActivityCategory: string = 'ALL';

  // Sidebar Navigation
  activeSection: string = 'home'; // Current active section - defaults to home feature grid
  sidebarCollapsed: boolean = false; // For mobile responsiveness

  // Feature Search (for home grid)
  featureSearchQuery: string = '';

  // Feature Box Definitions
  featureBoxes: { section: string; icon: string; label: string; description: string; gradient: string; featureKey?: string; route?: string; action?: () => void; badge?: () => number }[] = [
    { section: 'profile', icon: 'fa-user-shield', label: 'Admin Profile', description: 'View and manage admin profile', gradient: 'linear-gradient(135deg, #667eea, #764ba2)' },
    { section: 'users', icon: 'fa-users', label: 'Manage Users', description: 'View, edit and manage all user accounts', gradient: 'linear-gradient(135deg, #f093fb, #f5576c)', featureKey: 'manage-users', route: 'users' },
    { section: 'user-control', icon: 'fa-user-cog', label: 'Full User Control', description: 'Block, unblock and control user access', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)', featureKey: 'user-control', route: 'user-control' },
    { section: 'bulk-export', icon: 'fa-file-download', label: 'Bulk Data Export', description: 'Export account data in bulk PDF/Excel', gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)', featureKey: 'manage-users' },
    { section: 'deposit-withdraw', icon: 'fa-exchange-alt', label: 'Deposit/Withdraw', description: 'Process deposits and withdrawals', gradient: 'linear-gradient(135deg, #fa709a, #fee140)', featureKey: 'deposit-withdraw' },
    { section: 'transactions', icon: 'fa-chart-line', label: 'Transactions', description: 'View all transaction history', gradient: 'linear-gradient(135deg, #a18cd1, #fbc2eb)', featureKey: 'transactions' },
    { section: 'transfers', icon: 'fa-exchange-alt', label: 'Fund Transfers', description: 'NEFT, RTGS, IMPS transfers overview', gradient: 'linear-gradient(135deg, #fbc2eb, #a6c1ee)' },
    { section: 'beneficiaries', icon: 'fa-users', label: 'Beneficiaries', description: 'Manage registered beneficiaries', gradient: 'linear-gradient(135deg, #d4fc79, #96e6a1)' },
    { section: 'loans', icon: 'fa-coins', label: 'Loan Requests', description: 'Review and approve loan applications', gradient: 'linear-gradient(135deg, #f6d365, #fda085)', featureKey: 'loans', route: 'loans' },
    { section: 'gold-loans', icon: 'fa-gem', label: 'Gold Loans', description: 'Manage gold loan applications', gradient: 'linear-gradient(135deg, #ffecd2, #fcb69f)', featureKey: 'gold-loans', route: 'gold-loans' },
    { section: 'investments', icon: 'fa-chart-line', label: 'Investments', description: 'Review investment portfolios', gradient: 'linear-gradient(135deg, #a1c4fd, #c2e9fb)', route: 'investments' },
    { section: 'fixed-deposits', icon: 'fa-piggy-bank', label: 'Fixed Deposits', description: 'Manage fixed deposit accounts', gradient: 'linear-gradient(135deg, #667eea, #764ba2)', route: 'fixed-deposits' },
    { section: 'emi-management', icon: 'fa-calendar-check', label: 'EMI Management', description: 'Track and manage EMI payments', gradient: 'linear-gradient(135deg, #89f7fe, #66a6ff)', route: 'emi-management' },
    { section: 'cheques', icon: 'fa-file-invoice', label: 'Cheque Management', description: 'Process cheque requests', gradient: 'linear-gradient(135deg, #fddb92, #d1fdff)', featureKey: 'cheques', route: 'cheques' },
    { section: 'cheque-draw', icon: 'fa-receipt', label: 'Cheque Draw Requests', description: 'Manage salary cheque draws', gradient: 'linear-gradient(135deg, #c1dfc4, #deecdd)', route: 'cheque-draw-management' },
    { section: 'business-cheque-draw', icon: 'fa-building', label: 'Business Cheque Draw', description: 'Manage business cheque requests', gradient: 'linear-gradient(135deg, #e0c3fc, #8ec5fc)', route: 'business-cheque-management' },
    { section: 'cards', icon: 'fa-credit-card', label: 'Manage Cards', description: 'Manage debit cards', gradient: 'linear-gradient(135deg, #ff9a9e, #fecfef)', featureKey: 'cards', route: 'cards' },
    { section: 'credit-cards', icon: 'fa-credit-card', label: 'Credit Cards', description: 'Manage credit card applications', gradient: 'linear-gradient(135deg, #fbc2eb, #a18cd1)', featureKey: 'cards', route: 'credit-cards' },
    { section: 'current-accounts', icon: 'fa-building', label: 'Current Accounts', description: 'Manage business current accounts', gradient: 'linear-gradient(135deg, #84fab0, #8fd3f4)', route: 'current-accounts' },
    { section: 'open-account', icon: 'fa-user-plus', label: 'Open Account (No KYC)', description: 'Admin account opening approvals', gradient: 'linear-gradient(135deg, #a6c0fe, #f68084)', route: 'admin-open-account' },
    { section: 'insurance', icon: 'fa-file-medical', label: 'Insurance', description: 'Manage insurance applications', gradient: 'linear-gradient(135deg, #fccb90, #d57eeb)', route: 'insurance-dashboard' },
    { section: 'bill-payments', icon: 'fa-file-invoice-dollar', label: 'Bill Payments', description: 'View bill payment history', gradient: 'linear-gradient(135deg, #e0c3fc, #8ec5fc)' },
    { section: 'fasttags', icon: 'fa-tag', label: 'FASTag Management', description: 'Manage FASTag registrations', gradient: 'linear-gradient(135deg, #f093fb, #f5576c)', route: 'fasttags' },
    { section: 'kyc', icon: 'fa-id-card', label: 'KYC Verification', description: 'Verify customer KYC documents', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)', featureKey: 'kyc', route: 'kyc' },
    { section: 'video-kyc', icon: 'fa-video', label: 'Video KYC', description: 'Conduct video KYC sessions', gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)', route: 'video-kyc' },
    { section: 'merchant-onboarding', icon: 'fa-store', label: 'Merchant Onboarding', description: 'Review merchant applications', gradient: 'linear-gradient(135deg, #fa709a, #fee140)', route: 'merchant-onboarding' },
    { section: 'agent-management', icon: 'fa-user-tie', label: 'Agent Management', description: 'Manage field agents', gradient: 'linear-gradient(135deg, #a18cd1, #fbc2eb)', route: 'agent-management' },
    { section: 'subsidy-claims', icon: 'fa-graduation-cap', label: 'Subsidy Claims', description: 'Review education loan subsidy claims', gradient: 'linear-gradient(135deg, #fbc2eb, #a6c1ee)', featureKey: 'subsidy-claims', route: 'subsidy-claims' },
    { section: 'education-loans', icon: 'fa-university', label: 'Education Loans', description: 'Manage education loan applications', gradient: 'linear-gradient(135deg, #d4fc79, #96e6a1)', featureKey: 'education-loans', route: 'education-loan-applications' },
    { section: 'support-tickets', icon: 'fa-comments', label: 'Customer Support', description: 'Respond to support tickets', gradient: 'linear-gradient(135deg, #f6d365, #fda085)' },
    { section: 'passbook', icon: 'fa-book', label: 'Passbook Generation', description: 'Generate customer passbooks', gradient: 'linear-gradient(135deg, #ffecd2, #fcb69f)' },
    { section: 'ai-security', icon: 'fa-shield-alt', label: 'AI Security Center', description: 'Monitor AI-powered security', gradient: 'linear-gradient(135deg, #ff0844, #ffb199)', route: 'ai-security' },
    { section: 'biometric', icon: 'fa-camera', label: 'Face ID Auth', description: 'Manage biometric authentication', gradient: 'linear-gradient(135deg, #667eea, #764ba2)' },
    { section: 'net-banking-control', icon: 'fa-power-off', label: 'Net Banking Control', description: 'Enable/disable net banking services', gradient: 'linear-gradient(135deg, #e94560, #ff6b6b)' },
    { section: 'tracking', icon: 'fa-search-location', label: 'Account Tracking', description: 'Track account applications', gradient: 'linear-gradient(135deg, #89f7fe, #66a6ff)', featureKey: 'tracking' },
    { section: 'aadhar-verification', icon: 'fa-id-card', label: 'Aadhaar Verification', description: 'Verify Aadhaar numbers', gradient: 'linear-gradient(135deg, #fddb92, #d1fdff)', featureKey: 'aadhar-verification' },
    { section: 'gold-rate', icon: 'fa-coins', label: 'Gold Rate', description: 'Set and view gold rates', gradient: 'linear-gradient(135deg, #f6d365, #fda085)', featureKey: 'gold-rate' },
    { section: 'login-history', icon: 'fa-history', label: 'Login History', description: 'View user login activity', gradient: 'linear-gradient(135deg, #c1dfc4, #deecdd)', featureKey: 'login-history' },
    { section: 'update-history', icon: 'fa-edit', label: 'Update History', description: 'Track account updates', gradient: 'linear-gradient(135deg, #e0c3fc, #8ec5fc)', featureKey: 'update-history' },
    { section: 'profile-updates', icon: 'fa-user-edit', label: 'Profile Updates', description: 'Approve profile change requests', gradient: 'linear-gradient(135deg, #ff9a9e, #fecfef)' },
    { section: 'daily-report', icon: 'fa-calendar-day', label: 'Daily Activity Report', description: 'View daily activity summary', gradient: 'linear-gradient(135deg, #84fab0, #8fd3f4)', featureKey: 'daily-report' },
    { section: 'prediction-history', icon: 'fa-brain', label: 'Loan Prediction History', description: 'AI loan prediction records', gradient: 'linear-gradient(135deg, #a6c0fe, #f68084)', featureKey: 'loans' },
    { section: 'payment-gateway', icon: 'fa-bolt', label: 'Payment Gateway', description: 'Approve merchants, manage PG access', gradient: 'linear-gradient(135deg, #6366F1, #8B5CF6)' },
    { section: 'upi-management', icon: 'fa-mobile-alt', label: 'UPI Management', description: 'Manage all UPI accounts, block/enable, view transactions', gradient: 'linear-gradient(135deg, #0d9488, #059669)' },
    { section: 'atm-management', icon: 'fa-university', label: 'ATM Management', description: 'Manage ATMs, load cash, monitor transactions & incidents', gradient: 'linear-gradient(135deg, #f97316, #ef4444)' },
    { section: 'signature-management', icon: 'fa-signature', label: 'Signature Management', description: 'Upload & verify customer signatures for existing accounts', gradient: 'linear-gradient(135deg, #7c3aed, #a78bfa)' }
  ];

  // Feature Access Control
  featureAccess: Map<string, boolean> = new Map(); // Store feature permissions
  featureAccessWarningLogged: boolean = false; // Track if warning has been logged
  featureAccessLoading: boolean = false; // Track if feature access is currently loading
  featureAccessLoaded: boolean = false; // Track if feature access has been loaded at least once
  
  // Navigation source tracking
  isFromManager: boolean = false;

  // ── Admin UPI Management ──────────────────────────────────
  adminUpiAccounts: any[] = [];
  adminUpiTransactions: any[] = [];
  adminUpiFlagged: any[] = [];
  isLoadingAdminUpiAccounts: boolean = false;
  isLoadingAdminUpiTxns: boolean = false;
  isLoadingAdminUpiFlagged: boolean = false;
  adminUpiTab: string = 'accounts';

  // ── ATM Management ──────────────────────────────────────
  atmMachines: any[] = [];
  atmTransactions: any[] = [];
  atmIncidents: any[] = [];
  atmCashLoads: any[] = [];
  atmServiceLogs: any[] = [];
  atmDashboardStats: any = {};
  isLoadingAtmData: boolean = false;
  atmActiveTab: string = 'overview';
  atmSelectedMachine: any = null;
  atmTransactionFilter: string = 'ALL';
  atmIncidentFilter: string = 'ALL';
  atmTransactionPage: number = 0;
  atmTransactionTotalPages: number = 0;
  atmIncidentPage: number = 0;
  atmIncidentTotalPages: number = 0;
  // Cash Load Form
  atmCashLoadForm: any = { atmId: '', amount: 0, notes500: 0, notes200: 0, notes100: 0, notes2000: 0, remarks: '' };
  isLoadingCashLoad: boolean = false;
  // ATM Create/Edit Form
  atmForm: any = { atmId: '', atmName: '', location: '', city: '', state: '', pincode: '', maxCapacity: 5000000, minThreshold: 100000, maxWithdrawalLimit: 25000, dailyLimit: 100000, managedBy: '', contactNumber: '' };
  showAtmForm: boolean = false;
  atmFormMode: string = 'create';
  // Incident Form
  atmIncidentForm: any = { atmId: '', incidentType: 'CARD_STUCK', accountNumber: '', cardNumber: '', userName: '', description: '', amountInvolved: 0 };
  showAtmIncidentForm: boolean = false;
  // Status change
  atmStatusChangeForm: any = { status: '', reason: '' };
  showAtmStatusModal: boolean = false;
  atmStatusChangeMachine: any = null;

  // ── Signature Management ──────────────────────────────────
  sigLookupAccountNumber: string = '';
  sigLookupResult: any = null;
  sigIsLookingUp: boolean = false;
  sigLookupError: string = '';
  sigIsUploading: boolean = false;
  sigIsVerifying: boolean = false;
  sigIsRejecting: boolean = false;
  sigIsDownloadingForm: boolean = false;
  sigSelectedFile: File | null = null;
  sigUploadPreview: string | null = null;
  sigValidationResult: any = null; // Stores auto-read validation results
  sigFormDownloaded: boolean = false; // Track if form was downloaded for current account

  // Support Tickets
  supportTickets: any[] = [];
  filteredSupportTickets: any[] = [];
  supportTicketStats: any = { openCount: 0, inProgressCount: 0, resolvedCount: 0, totalCount: 0 };
  supportTicketFilter: string = 'ALL';
  supportTicketSearchQuery: string = '';
  isLoadingSupportTickets: boolean = false;
  selectedSupportTicket: any = null;
  adminResponseText: string = '';
  supportTicketPollingInterval: any = null;

  // Net Banking Service Control
  netBankingServices: any = { SAVINGS_ACCOUNT: { enabled: true, updatedBy: '', updatedAt: '' }, CURRENT_ACCOUNT: { enabled: true, updatedBy: '', updatedAt: '' } };
  netBankingAuditHistory: any[] = [];
  isLoadingNetBankingStatus: boolean = false;
  isTogglingNetBanking: boolean = false;

  // Per-Customer Net Banking Control
  netBankingCustomerType: string = 'savings';
  netBankingCustomerSearch: string = '';
  netBankingCustomerResults: any[] = [];
  isSearchingNetBankingCustomers: boolean = false;
  isTogglingCustomerNetBanking: boolean = false;
  netBankingCustomerTotalPages: number = 0;
  netBankingCustomerCurrentPage: number = 0;
  netBankingCustomerTotalElements: number = 0;

  // Biometric Authentication Management
  biometricRegistered: boolean = false;
  biometricCredentials: any[] = [];
  isBiometricSupported: boolean = false;
  isBiometricLoading: boolean = false;
  biometricLoadingMessage: string = '';
  biometricSuccessMessage: string = '';
  biometricErrorMessage: string = '';
  showBiometricCamera: boolean = false;
  biometricCameraStream: MediaStream | null = null;
  biometricFaceStatus: string = '';
  @ViewChild('dashboardVideo') dashboardVideoRef!: ElementRef<HTMLVideoElement>;

  // Sidebar Search
  sidebarSearchQuery: string = '';
  adminMenuSections: { title: string; items: { section: string; icon: string; label: string; featureKey?: string; action?: () => void; badge?: () => number }[] }[] = [
    {
      title: 'Main Menu',
      items: [
        { section: 'profile', icon: 'fa-user-shield', label: 'Admin Profile' },
        { section: 'users', icon: 'fa-users', label: 'Manage Users', featureKey: 'manage-users' },
        { section: 'user-control', icon: 'fa-user-cog', label: 'Full User Control', featureKey: 'user-control' },
        { section: 'bulk-export', icon: 'fa-file-download', label: 'Bulk Data Export', featureKey: 'manage-users' }
      ]
    },
    {
      title: 'Financial Operations',
      items: [
        { section: 'deposit-withdraw', icon: 'fa-exchange-alt', label: 'Deposit/Withdraw', featureKey: 'deposit-withdraw' },
        { section: 'transactions', icon: 'fa-chart-line', label: 'Transactions', featureKey: 'transactions' },
        { section: 'transfers', icon: 'fa-exchange-alt', label: 'Fund Transfers' },
        { section: 'beneficiaries', icon: 'fa-users', label: 'Beneficiaries' },
        { section: 'loans', icon: 'fa-coins', label: 'Loan Requests', featureKey: 'loans' },
        { section: 'gold-loans', icon: 'fa-gem', label: 'Gold Loans', featureKey: 'gold-loans' },
        { section: 'investments', icon: 'fa-chart-line', label: 'Investments' },
        { section: 'fixed-deposits', icon: 'fa-piggy-bank', label: 'Fixed Deposits' },
        { section: 'emi-management', icon: 'fa-calendar-check', label: 'EMI Management' },
        { section: 'cheques', icon: 'fa-file-invoice', label: 'Cheque Management', featureKey: 'cheques' },
        { section: 'cheque-draw', icon: 'fa-receipt', label: 'Cheque Draw Requests', action: () => this.navigateTo('cheque-draw-management') },
        { section: 'business-cheque-draw', icon: 'fa-building', label: 'Business Cheque Draw', action: () => this.navigateTo('business-cheque-management') }
      ]
    },
    {
      title: 'Services',
      items: [
        { section: 'cards', icon: 'fa-credit-card', label: 'Manage Cards', featureKey: 'cards' },
        { section: 'credit-cards', icon: 'fa-credit-card', label: 'Credit Cards', featureKey: 'cards' },
        { section: 'current-accounts', icon: 'fa-building', label: 'Current Accounts' },
        { section: 'open-account', icon: 'fa-user-plus', label: 'Open Account (No KYC)', action: () => this.navigateTo('admin-open-account') },
        { section: 'insurance', icon: 'fa-file-medical', label: 'Insurance', action: () => this.goToInsuranceDashboard() },
        { section: 'bill-payments', icon: 'fa-file-invoice-dollar', label: 'Bill Payments' },
        { section: 'fasttags', icon: 'fa-tag', label: 'FASTag Management', action: () => this.navigateTo('fasttags') },
        { section: 'kyc', icon: 'fa-id-card', label: 'KYC Verification', featureKey: 'kyc' },
        { section: 'video-kyc', icon: 'fa-video', label: 'Video KYC', action: () => this.navigateTo('video-kyc') },
        { section: 'merchant-onboarding', icon: 'fa-store', label: 'Merchant Onboarding', action: () => this.navigateTo('merchant-onboarding') },
        { section: 'agent-management', icon: 'fa-user-tie', label: 'Agent Management', action: () => this.navigateTo('agent-management') },
        { section: 'subsidy-claims', icon: 'fa-graduation-cap', label: 'Subsidy Claims', featureKey: 'subsidy-claims' },
        { section: 'education-loans', icon: 'fa-university', label: 'Education Loans', featureKey: 'education-loans' },
        { section: 'support-tickets', icon: 'fa-comments', label: 'Customer Support', badge: () => this.supportTicketStats.openCount },
        { section: 'passbook', icon: 'fa-book', label: 'Passbook Generation' },
        { section: 'payment-gateway', icon: 'fa-bolt', label: 'Payment Gateway' }
      ]
    },
    {
      title: 'Security & AI',
      items: [
        { section: 'ai-security', icon: 'fa-shield-alt', label: 'AI Security Center', action: () => this.navigateTo('ai-security') },
        { section: 'biometric', icon: 'fa-camera', label: 'Face ID Auth' }
      ]
    },
    {
      title: 'Management',
      items: [
        { section: 'net-banking-control', icon: 'fa-power-off', label: 'Net Banking Control' },
        { section: 'tracking', icon: 'fa-search-location', label: 'Account Tracking', featureKey: 'tracking' },
        { section: 'aadhar-verification', icon: 'fa-id-card', label: 'Aadhar Verification', featureKey: 'aadhar-verification', badge: () => this.pendingAadharVerifications },
        { section: 'gold-rate', icon: 'fa-coins', label: 'Gold Rate', featureKey: 'gold-rate' },
        { section: 'login-history', icon: 'fa-history', label: 'Login History', featureKey: 'login-history' },
        { section: 'update-history', icon: 'fa-edit', label: 'Update History', featureKey: 'update-history', badge: () => this.userUpdateHistory.length },
        { section: 'profile-updates', icon: 'fa-user-edit', label: 'Profile Updates', badge: () => this.pendingProfileUpdates.length },
        { section: 'daily-report', icon: 'fa-calendar-day', label: 'Daily Activity Report', featureKey: 'daily-report' },
        { section: 'prediction-history', icon: 'fa-brain', label: 'Loan Prediction History', featureKey: 'loans' }
      ]
    }
  ];

  // Search functionality
  searchQuery: string = '';
  searchResults: UserProfile[] = [];
  selectedSearchedUser: UserProfile | null = null;
  isSearching: boolean = false;
  userCards: CardDetails[] = [];
  userLoans: LoanDetails[] = [];
  userTransactions: TransactionDetails[] = [];

  // Universal Search functionality
  universalSearchQuery: string = '';
  universalSearchResults: any = null;
  isUniversalSearching: boolean = false;

  // Account Tracking
  accountTrackings: AccountTracking[] = [];
  selectedTracking: AccountTracking | null = null;

  // Aadhar Verification
  showAadharVerificationSection: boolean = false;
  aadharSearchQuery: string = '';
  verifyingAadhar: number | null = null;
  showTrackingSection: boolean = false;
  trackingSearchQuery: string = '';
  trackingStatusFilter: string = 'ALL';
  trackingStatusOptions = ['ALL', 'PENDING', 'ADMIN_SEEN', 'ADMIN_APPROVED', 'ADMIN_SENT'];

  // Gold Rate Management
  showGoldRateSection: boolean = false;
  currentGoldRate: any = null;
  newGoldRate: number = 0;
  goldRateHistory: any[] = [];
  isLoadingGoldRate: boolean = false;
  adminName: string = 'Admin';

  /** Multi-select for bulk PDF/Excel export (key = savings account number) */
  bulkExportSelection: Record<string, boolean> = {};
  isBulkExporting = false;
  /** Shown while reloading the user list on the Bulk Data Export section */
  isLoadingBulkUserList = false;

  // Gold Loan Management
  showGoldLoanSection: boolean = false;
  goldLoans: any[] = [];
  filteredGoldLoans: any[] = [];
  selectedGoldLoan: any = null;
  goldLoanStatusFilter: string = 'Pending';
  isLoadingGoldLoans: boolean = false;
  showGoldLoanModal: boolean = false;
  showForeclosureModal: boolean = false;
  showEmiDetailsModal: boolean = false;
  foreclosureDetails: any = null;
  emiSchedule: any[] = [];
  approvingLoan: boolean = false;
  processingForeclosure: boolean = false;
  goldDetailsForm: any = {
    goldItems: '',
    goldDescription: '',
    goldPurity: '22K',
    verifiedGoldGrams: 0,
    verificationNotes: '',
    storageLocation: ''
  };

  // User Login History
  showLoginHistorySection: boolean = false;
  loginHistory: any[] = [];
  isLoadingLoginHistory: boolean = false;
  loginHistoryFilter: string = 'ALL'; // ALL, SUCCESS, FAILED
  loginHistorySearchQuery: string = '';
  
  // All Transactions
  allTransactions: any[] = [];
  isLoadingTransactions: boolean = false;
  transactionsSearchQuery: string = '';
  transactionsTypeFilter: string = 'ALL';
  transactionsCategoryFilter: string = 'ALL'; // ALL, LOAN, SUBSIDY, FD, INVESTMENT, MUTUAL_FUND
  selectedUserForTransactions: string | null = null; // Account number of selected user
  transactionsDateFilter: string = ''; // Date filter (YYYY-MM-DD format)
  transactionsPANFilter: string = ''; // PAN number filter

  // Transfers (Admin view)
  transfers: any[] = [];
  transfersSearchQuery: string = '';
  transfersTypeFilter: string = 'ALL'; // ALL, NEFT, RTGS, IMPS
  transfersStatusFilter: string = 'ALL'; // ALL, Pending, Completed, Failed, Cancelled
  isLoadingTransfers: boolean = false;

  // Beneficiaries (Admin view)
  beneficiaries: any[] = [];
  beneficiariesSearchQuery: string = '';
  isLoadingBeneficiaries: boolean = false;
  
  // Loan Prediction History
  loanPredictions: any[] = [];
  isLoadingPredictions: boolean = false;
  predictionFilter: string = 'ALL'; // ALL, Approved, Rejected, Pending Review
  predictionSearchQuery: string = '';
  predictionLoanTypeFilter: string = 'ALL'; // ALL, Personal Loan, Education Loan, Home Loan, Car Loan

  // Investments Management
  investments: any[] = [];
  isLoadingInvestments: boolean = false;
  selectedInvestment: any = null;
  investmentStatusFilter: string = 'PENDING';
  investmentSearchQuery: string = '';

  // Fixed Deposits Management
  fixedDeposits: any[] = [];
  isLoadingFDs: boolean = false;
  selectedFD: any = null;
  fdStatusFilter: string = 'PENDING';
  fdSearchQuery: string = '';

  // EMI Management
  allEmis: any[] = [];
  isLoadingAllEMIs: boolean = false;
  selectedEMI: any = null;
  emiStatusFilter: string = 'ALL';
  emiSearchQuery: string = '';

  // Credit Card Management
  showCreditCardSection: boolean = false;

  // Bill Payments Management
  showBillPaymentsSection: boolean = false;
  billPayments: any[] = [];
  isLoadingBillPayments: boolean = false;
  billPaymentsSearchQuery: string = '';
  billPaymentsTypeFilter: string = 'ALL'; // ALL, Mobile Bill, WiFi Bill
  billPaymentsStatusFilter: string = 'ALL'; // ALL, Completed, Failed, Pending
  creditCards: any[] = [];
  isLoadingCreditCards: boolean = false;
  selectedCreditCard: any = null;
  creditCardTransactions: any[] = [];
  creditCardBills: any[] = [];
  creditCardSearchQuery: string = '';
  creditCardStatusFilter: string = 'ALL';
  showApplyCreditCardModal: boolean = false;
  applyCreditCardForm = {
    accountNumber: '',
    userName: '',
    userEmail: '',
    pan: '',
    income: 0
  };
  showLimitModal: boolean = false;
  limitAction: 'increase' | 'decrease' = 'increase';
  limitChangeAmount: number = 0;
  limitChangeReason: string = '';

  // Passbook Management
  passbookAccounts: any[] = [];
  filteredPassbookAccounts: any[] = [];
  paginatedPassbookAccounts: any[] = [];
  passbookSearchQuery: string = '';
  passbookAccountTypeFilter: string = 'all';
  passbookStatusFilter: string = 'all';
  passbookSortBy: string = 'name';
  isLoadingPassbookAccounts: boolean = false;
  passbookGeneratingFor: string = '';
  isBulkGenerating: boolean = false;
  passbookStats = { savings: 0, current: 0, salary: 0, total: 0, frozen: 0, closed: 0 };
  selectedPassbookAccounts: any[] = [];
  passbookCurrentPage: number = 1;
  passbookPageSize: number = 10;
  passbookTotalPages: number = 1;
  showPassbookPreview: boolean = false;
  previewAccount: any = null;
  showPassbookHistoryModal: boolean = false;
  passbookDownloadHistory: any[] = [];

  // Account Action Management
  showAccountActionModal: boolean = false;
  accountActionType: string = ''; // freeze, unfreeze, close, delete
  accountActionTarget: any = null;
  accountActionReason: string = '';
  passbookActionLoading: string = '';

  goToInsuranceDashboard() {
    this.router.navigate(['/admin/insurance-dashboard']);
  }

  // ---- Admin UPI Management ----
  loadAdminUpiAccounts() {
    this.isLoadingAdminUpiAccounts = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/upi/savings/accounts`).subscribe({
      next: (res: any) => { this.adminUpiAccounts = res?.accounts || []; this.isLoadingAdminUpiAccounts = false; },
      error: () => { this.adminUpiAccounts = []; this.isLoadingAdminUpiAccounts = false; }
    });
  }

  loadAdminUpiTransactions() {
    this.isLoadingAdminUpiTxns = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/upi/savings/transactions`).subscribe({
      next: (res: any) => { this.adminUpiTransactions = res?.transactions || []; this.isLoadingAdminUpiTxns = false; },
      error: () => { this.adminUpiTransactions = []; this.isLoadingAdminUpiTxns = false; }
    });
  }

  loadAdminUpiFlagged() {
    this.isLoadingAdminUpiFlagged = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/upi/savings/flagged`).subscribe({
      next: (res: any) => { this.adminUpiFlagged = res?.transactions || []; this.isLoadingAdminUpiFlagged = false; },
      error: () => { this.adminUpiFlagged = []; this.isLoadingAdminUpiFlagged = false; }
    });
  }

  toggleAdminUpi(accountNumber: string, enable: boolean) {
    this.http.put(`${environment.apiBaseUrl}/api/admins/upi/savings/toggle/${accountNumber}?enable=${enable}`, {}).subscribe({
      next: () => this.loadAdminUpiAccounts(),
      error: (err: any) => this.alertService.error('Error', err?.error?.message || 'Failed to toggle UPI')
    });
  }

  // ---- Payment Gateway Management ----
  loadPgPendingMerchants() {
    this.pgIsLoading = true;
    this.pgService.getPendingMerchants().subscribe({
      next: (data: any[]) => { this.pgPendingMerchants = data || []; this.pgIsLoading = false; },
      error: () => { this.pgPendingMerchants = []; this.pgIsLoading = false; }
    });
  }

  loadPgApprovedMerchants() {
    this.pgIsLoading = true;
    this.pgService.getApprovedMerchants().subscribe({
      next: (data: any[]) => { this.pgApprovedMerchants = data || []; this.pgIsLoading = false; },
      error: () => { this.pgApprovedMerchants = []; this.pgIsLoading = false; }
    });
  }

  pgVerifyAccount(accountNumber: string) {
    if (!accountNumber) return;
    this.pgService.verifyLinkedAccount(accountNumber).subscribe({
      next: (res: any) => { this.pgLinkVerifyResult = res; },
      error: (err: any) => { this.pgLinkVerifyResult = { success: false, message: err.error?.error || 'Account not found' }; }
    });
  }

  pgApproveMerchant(merchant: any) {
    if (!this.pgLinkAccountForm.accountNumber) {
      alert('Please enter an account number to link');
      return;
    }
    const adminData = sessionStorage.getItem('admin');
    const adminName = adminData ? JSON.parse(adminData).name || 'Admin' : 'Admin';
    this.pgIsLoading = true;
    this.pgService.approveMerchant(merchant.merchantId, {
      linkedAccountNumber: this.pgLinkAccountForm.accountNumber,
      linkedAccountType: this.pgLinkAccountForm.accountType,
      approvedBy: adminName
    }).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.loadPgPendingMerchants();
          this.loadPgApprovedMerchants();
          this.pgLinkAccountForm = { merchantId: '', accountNumber: '', accountType: 'CURRENT' };
          this.pgLinkVerifyResult = null;
        }
        this.pgIsLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Approval failed'); this.pgIsLoading = false; }
    });
  }

  pgRejectMerchant(merchant: any) {
    if (!this.pgRejectReason) {
      alert('Please provide a reason for rejection');
      return;
    }
    this.pgIsLoading = true;
    this.pgService.rejectMerchant(merchant.merchantId, { reason: this.pgRejectReason }).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.loadPgPendingMerchants();
          this.pgRejectingMerchant = null;
          this.pgRejectReason = '';
        }
        this.pgIsLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Rejection failed'); this.pgIsLoading = false; }
    });
  }

  pgToggleLogin(merchant: any) {
    this.pgService.toggleMerchantLogin(merchant.merchantId, !merchant.loginEnabled).subscribe({
      next: (res: any) => {
        if (res.success) {
          merchant.loginEnabled = !merchant.loginEnabled;
          this.loadPgApprovedMerchants();
        }
      },
      error: (err: any) => { alert(err.error?.error || 'Toggle failed'); }
    });
  }

  openMerchantDetails(merchant: any) {
    this.pgSelectedMerchant = merchant;
    this.pgMerchantDetailTab = 'overview';
    this.pgMerchantOrders = [];
    this.pgMerchantTransactions = [];
    this.pgMerchantRefunds = [];
    this.pgMerchantAnalytics = null;
    this.pgMerchantDetailsLoading = true;
    forkJoin({
      orders: this.pgService.getOrdersByMerchant(merchant.merchantId).pipe(catchError(() => of([]))),
      transactions: this.pgService.getTransactionsByMerchant(merchant.merchantId).pipe(catchError(() => of([]))),
      refunds: this.pgService.getRefundsByMerchant(merchant.merchantId).pipe(catchError(() => of([]))),
      analytics: this.pgService.getMerchantAnalytics(merchant.merchantId).pipe(catchError(() => of(null)))
    }).subscribe({
      next: (result: any) => {
        this.pgMerchantOrders = result.orders || [];
        this.pgMerchantTransactions = result.transactions || [];
        this.pgMerchantRefunds = result.refunds || [];
        this.pgMerchantAnalytics = result.analytics;
        this.pgMerchantDetailsLoading = false;
      },
      error: () => { this.pgMerchantDetailsLoading = false; }
    });
  }

  closeMerchantDetails() {
    this.pgSelectedMerchant = null;
    this.pgMerchantOrders = [];
    this.pgMerchantTransactions = [];
    this.pgMerchantRefunds = [];
    this.pgMerchantAnalytics = null;
  }

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService,
    private faceAuthService: FaceAuthService,
    private pgService: PaymentGatewayService
  ) {}

  ngOnInit() {
    // Only load users in the browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      // Clean up any corrupted session data first
      this.cleanupCorruptedSessionData();
      
      // Check if profile is complete, redirect if not, and get admin name
      const adminData = sessionStorage.getItem('admin');
      if (adminData) {
        try {
          const admin = JSON.parse(adminData);
          
          // Validate obvious corruption markers only
          if (admin.email && (admin.email.includes('/') || admin.email.includes('api'))) {
            console.error('Detected corrupted admin email in session, clearing:', admin.email);
            sessionStorage.removeItem('admin');
            this.router.navigate(['/admin/login']);
            return;
          }
          
          // Set admin name
          this.adminName = admin.username || admin.name || 'Admin';
        } catch (e) {
          console.error('Error parsing admin data:', e);
          // Clear corrupted session data
          sessionStorage.removeItem('admin');
        }
      }
      
      // Check if navigation is from manager dashboard
      const navigationSource = sessionStorage.getItem('navigationSource');
      if (navigationSource === 'MANAGER') {
        this.isFromManager = true;
      }
      
      // Check if there's a section to open from manager dashboard
      const dashboardSection = sessionStorage.getItem('adminDashboardSection');
      if (dashboardSection) {
        sessionStorage.removeItem('adminDashboardSection');
        // Set the active section after a short delay to ensure component is ready
        setTimeout(() => {
          this.setActiveSection(dashboardSection);
        }, 100);
      }
      
      // Initialize session tracking
      this.initializeSessionTracking();
      this.startSessionTimer();
      
      // Load feature access permissions first
      this.loadFeatureAccess();
      
      // Set up a listener for localStorage changes (when manager updates features in another tab)
      window.addEventListener('storage', (e) => {
        if (e.key === 'adminFeatureAccess') {
          console.log('Feature access changed in localStorage, reloading...');
          this.loadFeatureAccess();
        }
      });
      
      // Also listen for custom events (for same-tab updates)
      window.addEventListener('featureAccessUpdated', (event: any) => {
        console.log('Feature access updated event received, reloading...');
        // Check if this update is for the current admin
        const adminData = sessionStorage.getItem('admin');
        if (adminData && event.detail) {
          try {
            const admin = JSON.parse(adminData);
            if (admin.email === event.detail.adminEmail) {
              console.log('Feature access updated for current admin, reloading...');
              // Force clear and reload
              this.featureAccess.clear();
              this.featureAccessLoaded = false;
              this.loadFeatureAccess();
            }
          } catch (e) {
            console.error('Error parsing admin data:', e);
          }
        } else {
          // Fallback: reload anyway
          this.featureAccess.clear();
          this.featureAccessLoaded = false;
          this.loadFeatureAccess();
        }
      });
      
      // Listen for localStorage changes (cross-tab communication)
      window.addEventListener('storage', (e: StorageEvent) => {
        if (e.key && e.key.startsWith('adminFeatureAccess_')) {
          const adminData = sessionStorage.getItem('admin');
          if (adminData) {
            try {
              const admin = JSON.parse(adminData);
              const keyEmail = e.key.replace('adminFeatureAccess_', '');
              if (admin.email === keyEmail) {
                console.log('Feature access changed in localStorage (cross-tab), reloading...');
                this.featureAccess.clear();
                this.featureAccessLoaded = false;
                this.loadFeatureAccess();
              }
            } catch (err) {
              console.error('Error in storage event listener:', err);
            }
          }
        }
      });
      
      // Load all data in parallel for better performance
      this.loadInitialData();
    }
  }

  loadFeatureAccess() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Prevent multiple simultaneous loads
    if (this.featureAccessLoading) {
      return;
    }
    
    this.featureAccessLoading = true;
    
    // Clear existing feature access
    this.featureAccess.clear();
    
    // Get current admin email from session
    const adminData = sessionStorage.getItem('admin');
    let adminEmail = '';
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        adminEmail = admin.email || '';
      } catch (e) {
        console.error('Error parsing admin data:', e);
        this.featureAccessLoading = false;
        this.featureAccessLoaded = true; // Mark as loaded to prevent warnings
        return;
      }
    }
    
    // Validate adminEmail - must be a valid email format and not contain URL paths
    // Check for corrupted email that contains URL paths
    if (!adminEmail || 
        !adminEmail.includes('@') || 
        adminEmail.includes('/') || 
        adminEmail.includes('api') ||
        adminEmail.includes('feature-access') ||
        adminEmail.length > 100 ||
        adminEmail.includes('http')) {
      console.log('Invalid admin email found, cannot load per-admin feature access:', adminEmail);
      // Clear any corrupted data
      if (adminEmail && (adminEmail.includes('/') || adminEmail.includes('api') || adminEmail.includes('feature-access'))) {
        console.error('Detected corrupted admin email containing URL paths, clearing session and localStorage');
        sessionStorage.removeItem('admin');
        sessionStorage.removeItem('userRole');
        // Clear all feature access entries
        for (let i = 0; i < localStorage.length; i++) {
          const key = localStorage.key(i);
          if (key && key.startsWith('adminFeatureAccess_')) {
            localStorage.removeItem(key);
          }
        }
      }
      this.featureAccessLoading = false;
      this.featureAccessLoaded = true; // Mark as loaded to prevent warnings
      return;
    }
    
    // Load per-admin feature access from localStorage first (fast)
    const savedKey = `adminFeatureAccess_${adminEmail}`;
    const savedFeatures = localStorage.getItem(savedKey);
    
    if (savedFeatures) {
      try {
        const features = JSON.parse(savedFeatures);
        console.log(`Loading feature access for ${adminEmail} from localStorage:`, features);
        if (Array.isArray(features)) {
          features.forEach((feature: any) => {
            this.featureAccess.set(feature.id, feature.enabled === true); // Ensure boolean
          });
          console.log(`Total features loaded from localStorage: ${this.featureAccess.size}`);
          this.featureAccessLoaded = true; // Mark as loaded
        }
      } catch (e) {
        console.error('Error loading feature access:', e);
      }
    } else {
      console.log(`No saved feature access found for ${adminEmail} in localStorage`);
    }
    
    // Also try to load from backend (this will override localStorage if backend has data)
    // Encode the email to prevent URL injection issues
    const encodedEmail = encodeURIComponent(adminEmail);
    
    // Build URL safely - ensure environment.apiBaseUrl doesn't already contain the path
    let baseUrl = environment.apiBaseUrl;
    // Remove trailing slash if present
    if (baseUrl.endsWith('/')) {
      baseUrl = baseUrl.slice(0, -1);
    }
    // Remove /api/admins/feature-access if it's already in the base URL (prevent duplication)
    if (baseUrl.includes('/api/admins/feature-access')) {
      console.error('Base URL already contains feature-access path, cleaning up');
      baseUrl = baseUrl.split('/api/admins/feature-access')[0];
    }
    
    const featureAccessUrl = `${baseUrl}/api/admins/feature-access/${encodedEmail}`;
    
    // Additional safety checks - ensure URL doesn't contain duplicate paths
    if (featureAccessUrl.includes('/api/admins/feature-access/api/admins/feature-access') ||
        featureAccessUrl.split('/api/admins/feature-access').length > 2) {
      console.error('Detected corrupted URL with duplicate paths, aborting feature access load:', featureAccessUrl);
      // Clean up corrupted session data
      sessionStorage.removeItem('admin');
      sessionStorage.removeItem('userRole');
      this.featureAccessLoading = false;
      this.featureAccessLoaded = true;
      return;
    }
    
    this.http.get(featureAccessUrl)
      .pipe(
        timeout(10000), // 10 second timeout (increased from 8)
        catchError(err => {
          console.warn('Error loading feature access from backend (non-critical):', err);
          
          let errorMessage = 'Failed to load feature access. ';
          if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
            errorMessage = 'Feature access request timed out. ';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server for feature access. ';
          } else if (err.status >= 500) {
            errorMessage = 'Server error loading feature access. ';
          }
          console.warn(errorMessage + 'Using cached data if available.');
          
          // Use localStorage data if backend fails
          this.featureAccessLoading = false;
          this.featureAccessLoaded = true; // Mark as loaded even if backend fails
          return of({ success: false });
        })
      )
      .subscribe({
        next: (response: any) => {
          this.featureAccessLoading = false;
          this.featureAccessLoaded = true;
          
          if (response && response.success && response.features && Array.isArray(response.features)) {
            console.log(`Loading feature access for ${adminEmail} from backend:`, response.features);
            // Clear and reload from backend
            this.featureAccess.clear();
            response.features.forEach((feature: any) => {
              this.featureAccess.set(feature.id, feature.enabled === true); // Ensure boolean
            });
            // Update localStorage with backend data
            localStorage.setItem(savedKey, JSON.stringify(response.features));
            console.log(`Total features loaded from backend: ${this.featureAccess.size}`);
          } else {
            console.log(`No features found in backend response for ${adminEmail}, using localStorage data`);
          }
        },
        error: () => {
          // Error already handled by catchError
          this.featureAccessLoading = false;
          this.featureAccessLoaded = true;
        }
      });
  }

  /**
   * Clean up any corrupted session or localStorage data
   */
  cleanupCorruptedSessionData() {
    try {
      // Check for corrupted admin email in session
      const adminData = sessionStorage.getItem('admin');
      if (adminData) {
        const admin = JSON.parse(adminData);
        if (admin.email && (admin.email.includes('/') || admin.email.includes('api') || admin.email.length > 100)) {
          console.error('Cleaning up corrupted admin session data');
          sessionStorage.removeItem('admin');
        }
      }
      
      // Clean up any corrupted localStorage entries
      const keys = Object.keys(localStorage);
      keys.forEach(key => {
        if (key.includes('adminFeatureAccess') && key.length > 200) {
          console.error('Removing corrupted localStorage key:', key);
          localStorage.removeItem(key);
        }
      });
    } catch (e) {
      console.error('Error during cleanup:', e);
    }
  }

  hasFeatureAccess(featureId: string): boolean {
    // If still loading, wait a bit (but don't block UI)
    if (this.featureAccessLoading && !this.featureAccessLoaded) {
      // Return true during initial load to avoid blocking UI
      return true;
    }
    
    // Check if feature is in the map
    if (this.featureAccess.has(featureId)) {
      const hasAccess = this.featureAccess.get(featureId)!;
      // Explicitly check for true (not just truthy)
      return hasAccess === true;
    }
    
    // If feature is not in the map, check if we have any features loaded
    // If we have features loaded but this one is missing, default to false (more secure)
    // If no features are loaded at all, default to true (backward compatibility)
    if (this.featureAccessLoaded && this.featureAccess.size > 0) {
      // Features were loaded but this one is missing - deny access
      return false;
    }
    
    // No features loaded yet or loading failed, default to true for backward compatibility
    // Only log once to avoid spam
    if (!this.featureAccessWarningLogged && this.featureAccessLoaded) {
      console.log('Feature access loaded but feature not found, defaulting to true for backward compatibility');
      this.featureAccessWarningLogged = true;
    }
    return true;
  }

  /**
   * Load initial data from localStorage cache for instant display
   */
  loadInitialDataFromCache() {
    if (!isPlatformBrowser(this.platformId)) return;

    // Load users from cache
    const usersData = localStorage.getItem('userProfiles');
    if (usersData) {
      try {
        this.users = JSON.parse(usersData);
        console.log(`Loaded ${this.users.length} users from cache for instant display`);
      } catch (e) {
        console.error('Error parsing cached users:', e);
        this.users = [];
      }
    }

    // Load gold rate from cache
    const goldRateData = localStorage.getItem('currentGoldRate');
    if (goldRateData) {
      try {
        this.currentGoldRate = JSON.parse(goldRateData);
        this.newGoldRate = this.currentGoldRate?.ratePerGram || 0;
        console.log('Loaded gold rate from cache');
      } catch (e) {
        console.error('Error parsing cached gold rate:', e);
      }
    }

    // Load login history from cache
    const loginHistoryData = localStorage.getItem('adminLoginHistory');
    if (loginHistoryData) {
      try {
        this.loginHistory = JSON.parse(loginHistoryData);
        console.log(`Loaded ${this.loginHistory.length} login history entries from cache`);
      } catch (e) {
        console.error('Error parsing cached login history:', e);
      }
    }
  }

  /**
   * Load all initial data in parallel for better performance
   * Uses localStorage cache first for instant display, then refreshes from API
   */
  loadInitialData() {
    if (!isPlatformBrowser(this.platformId)) {
      this.isLoadingInitialData = false;
      return;
    }

    // STEP 1: Load from localStorage FIRST for instant display (no API call, instant)
    this.loadInitialDataFromCache();

    // STEP 2: Mark as loading and refresh from API in background
    this.isLoadingInitialData = true;

    // Load support ticket stats for badge count
    this.loadSupportTicketStats();

    // Create parallel API calls with timeouts and error handling
    const apiCalls = {
      users: this.http.get(`${environment.apiBaseUrl}/api/users/admin/all?page=0&size=100`).pipe(
        timeout(15000), // 15 second timeout
        catchError(err => {
          console.error('Error loading users:', err);
          let errorMessage = 'Failed to load users. ';
          if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
            errorMessage = 'Request timed out. The server may be slow. ';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server. Please check if the backend is running. ';
          } else if (err.status >= 500) {
            errorMessage = 'Server error occurred. Please try again later. ';
          }
          console.error(errorMessage);
          // Return empty content on error, will keep cached data
          return of({ content: [] });
        })
      ),
      accountTrackings: this.http.get(`${environment.apiBaseUrl}/api/tracking?page=0&size=100&sortBy=createdAt&sortDir=desc`).pipe(
        timeout(12000), // 12 second timeout
        catchError(err => {
          console.error('Error loading account trackings:', err);
          return of([]);
        })
      ),
      goldRate: this.http.get<any>(`${environment.apiBaseUrl}/api/gold-rates/current`).pipe(
        timeout(10000), // 10 second timeout
        catchError(err => {
          console.error('Error loading gold rate:', err);
          return of(null);
        })
      ),
      loginHistory: this.http.get<any[]>(`${environment.apiBaseUrl}/api/admins/login-history/recent?limit=50`).pipe(
        timeout(10000), // 10 second timeout (reduced from 12)
        catchError(err => {
          console.error('Error loading login history:', err);
          return of([]);
        })
      )
    };

    // Execute all API calls in parallel
    forkJoin(apiCalls).subscribe({
      next: (results) => {
        // Process users data (from /api/users/admin/all - supports paginated {content} or array)
        const usersRaw = results.users as { content?: unknown[] } | unknown[] | undefined;
        const usersList = Array.isArray(usersRaw) ? usersRaw : (usersRaw?.content ?? []);
        if (usersList.length > 0) {
          console.log(`Processing ${usersList.length} users from API`);
          this.users = usersList.map((user: any) => {
            // Handle both Map format (from admin/all) and User object format
            const account = user.account || {};
            return {
              id: (user.id || '').toString(),
              name: account.name || user.username || 'Unknown User',
              email: user.email || 'No email',
              accountNumber: user.accountNumber || 'No account',
              balance: account.balance || 0,
              status: account.status || 'ACTIVE',
              phoneNumber: account.phone || user.phoneNumber || '',
              dateOfBirth: account.dob || user.dateOfBirth || '',
              address: account.address || user.address || '',
              pan: account.pan || user.pan || '',
              aadhar: account.aadharNumber || user.aadhar || '',
              customerId: account.customerId || '',
              occupation: account.occupation || user.occupation || '',
              income: account.income || user.income || 0,
              accountType: account.accountType || 'Savings Account',
              joinDate: user.joinDate || (user.createdAt ? new Date(user.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0])
            };
          });
          
          // Save to localStorage as backup
          if (isPlatformBrowser(this.platformId)) {
            localStorage.setItem('userProfiles', JSON.stringify(this.users));
          }
          console.log(`Successfully loaded ${this.users.length} users into dashboard`);
        } else {
          console.warn('No users data from API or empty array, keeping cached data');
          // Keep cached data if API failed (already loaded in loadInitialDataFromCache)
          if (this.users.length === 0 && isPlatformBrowser(this.platformId)) {
            const usersData = localStorage.getItem('userProfiles');
            if (usersData) {
              try {
                this.users = JSON.parse(usersData);
                console.log(`Loaded ${this.users.length} users from localStorage fallback`);
              } catch (e) {
                console.error('Error parsing localStorage users data:', e);
                this.users = [];
              }
            }
          }
        }

        // Process account trackings - load full list
        if (results.accountTrackings && Array.isArray(results.accountTrackings)) {
          this.accountTrackings = results.accountTrackings;
        }

        // Note: Pending Aadhar verifications are loaded on-demand when the section is opened
        // to avoid unnecessary API calls on initial load

        // Process gold rate
        if (results.goldRate) {
          this.currentGoldRate = results.goldRate;
          this.newGoldRate = results.goldRate.ratePerGram || 0;
          // Cache gold rate
          if (isPlatformBrowser(this.platformId)) {
            localStorage.setItem('currentGoldRate', JSON.stringify(results.goldRate));
          }
        }
        this.isLoadingGoldRate = false;

        // Process login history
        if (results.loginHistory && Array.isArray(results.loginHistory)) {
          this.loginHistory = results.loginHistory;
          // Cache login history
          if (isPlatformBrowser(this.platformId)) {
            localStorage.setItem('adminLoginHistory', JSON.stringify(results.loginHistory));
          }
        }
        this.isLoadingLoginHistory = false;

        // Load local storage data (non-blocking, delayed to not block initial render)
        setTimeout(() => {
          this.loadAdminTransactionHistory();
          this.loadUserUpdateHistory();
        }, 200);

        this.isLoadingInitialData = false;
        console.log('All initial data loaded successfully');
      },
      error: (err) => {
        console.error('Error loading initial data:', err);
        
        // Try to load from localStorage as fallback
        if (isPlatformBrowser(this.platformId)) {
          const usersData = localStorage.getItem('userProfiles');
          if (usersData) {
            try {
              this.users = JSON.parse(usersData);
              console.log('Using fallback users data from localStorage');
            } catch (e) {
              console.error('Error parsing localStorage users:', e);
              this.users = [];
            }
          } else {
            this.users = [];
          }
        }
        
        this.isLoadingInitialData = false;
        // Fallback to individual loads if forkJoin fails
        this.loadUsers();
        this.loadAccountTrackings();
        this.loadPendingAadharVerifications();
        this.loadCurrentGoldRate();
        this.loadLoginHistory();
        this.loadAdminTransactionHistory();
        this.loadUserUpdateHistory();
        // Load pending profile updates for badge count
        this.loadPendingProfileUpdatesForBadge();
      }
    });
  }

  // Refresh users data from database
  refreshUsersData() {
    console.log('Refreshing users data from database...');
    this.loadUsers();
  }

  // TrackBy for ngFor - reduces lag when list updates
  trackByUserId(_index: number, user: UserProfile): string {
    return user.id || user.accountNumber || String(_index);
  }

  toggleBulkExportSelection(accountNumber: string) {
    this.bulkExportSelection[accountNumber] = !this.bulkExportSelection[accountNumber];
  }

  selectAllBulkExport() {
    for (const u of this.users) {
      if (u.accountNumber && u.accountNumber !== 'No account') {
        this.bulkExportSelection[u.accountNumber] = true;
      }
    }
  }

  clearBulkExportSelection() {
    this.bulkExportSelection = {};
  }

  getSelectedBulkCount(): number {
    return this.users.filter(u => u.accountNumber && u.accountNumber !== 'No account' && this.bulkExportSelection[u.accountNumber]).length;
  }

  getBulkExportableCount(): number {
    return this.users.filter(u => u.accountNumber && u.accountNumber !== 'No account').length;
  }

  isAllBulkSelected(): boolean {
    const n = this.getBulkExportableCount();
    return n > 0 && this.getSelectedBulkCount() === n;
  }

  toggleSelectAllBulk(ev: Event) {
    const checked = (ev.target as HTMLInputElement).checked;
    if (checked) {
      this.selectAllBulkExport();
    } else {
      this.clearBulkExportSelection();
    }
  }

  downloadBulkExport(format: 'pdf' | 'excel') {
    const selected = this.users
      .filter(u => u.accountNumber && u.accountNumber !== 'No account' && this.bulkExportSelection[u.accountNumber])
      .map(u => u.accountNumber);
    if (selected.length === 0) {
      this.alertService.error('No selection', 'Select at least one user with a valid account number.');
      return;
    }
    this.isBulkExporting = true;
    this.http
      .post(
        `${environment.apiBaseUrl}/api/admin/bulk-export`,
        {
          accountNumbers: selected,
          adminName: this.adminName,
          format: format === 'excel' ? 'excel' : 'pdf'
        },
        { responseType: 'blob', observe: 'response' }
      )
      .subscribe({
        next: resp => {
          this.isBulkExporting = false;
          const blob = resp.body;
          if (!blob || blob.size === 0) {
            this.alertService.error('Export failed', 'Empty response from server.');
            return;
          }
          const cd = resp.headers.get('Content-Disposition');
          let filename = format === 'pdf' ? 'NeoBank-users-export.pdf' : 'NeoBank-users-export.xlsx';
          if (cd) {
            const m =
              /filename\*=(?:UTF-8'')?([^;\n]+)/i.exec(cd) || /filename="([^"]+)"/i.exec(cd) || /filename=([^;\s]+)/i.exec(cd);
            if (m && m[1]) {
              filename = decodeURIComponent(m[1].trim().replace(/^["']|["']$/g, ''));
            }
          }
          if (isPlatformBrowser(this.platformId)) {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            a.click();
            window.URL.revokeObjectURL(url);
          }
          this.alertService.success('Download started', filename);
        },
        error: () => {
          this.isBulkExporting = false;
          this.alertService.error('Export failed', 'Could not generate file. Check that the backend is running and you have permission.');
        }
      });
  }

  navigateTo(path: string) {
    // Preserve navigation source when navigating to other admin pages
    if (this.isFromManager) {
      sessionStorage.setItem('navigationSource', 'MANAGER');
      sessionStorage.setItem('managerReturnPath', '/manager/dashboard');
    }
    this.router.navigate([`/admin/${path}`]);
  }

  goBackToManager() {
    // Clear navigation source and return to manager dashboard
    sessionStorage.removeItem('navigationSource');
    sessionStorage.removeItem('managerReturnPath');
    this.router.navigate(['/manager/dashboard']);
  }

  // Sidebar Search Methods
  onSidebarSearch() {
    // Real-time filtering handled by getFilteredAdminMenu()
  }

  getFilteredAdminMenu() {
    if (!this.sidebarSearchQuery || !this.sidebarSearchQuery.trim()) {
      return this.adminMenuSections;
    }
    const query = this.sidebarSearchQuery.toLowerCase().trim();
    return this.adminMenuSections
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

  getFilteredFeatures() {
    const query = (this.featureSearchQuery || '').toLowerCase().trim();
    return this.featureBoxes.filter(f => {
      const matchesSearch = !query || f.label.toLowerCase().includes(query) || f.description.toLowerCase().includes(query) || f.section.toLowerCase().includes(query);
      const featureKey = f.featureKey;
      const hasAccess = !featureKey || this.hasFeatureAccess(featureKey);
      return matchesSearch && hasAccess;
    });
  }

  onFeatureClick(feature: any) {
    if (feature.action) {
      feature.action();
    } else if (feature.route) {
      this.navigateTo(feature.route);
    } else {
      this.setActiveSection(feature.section);
    }
  }

  goToHome() {
    this.activeSection = 'home';
  }

  setActiveSection(section: string) {
    console.log('Setting active section:', section);
    
    // Check feature access before navigating
    const featureAccessMap: { [key: string]: string } = {
      'transactions': 'transactions',
      'loans': 'loans',
      'cheques': 'cheques',
      'users': 'manage-users',
      'user-control': 'user-control',
      'cards': 'cards',
      'kyc': 'kyc',
      'subsidy-claims': 'subsidy-claims',
      'education-loans': 'education-loans',
      'support-tickets': 'chat',
      'gold-loans': 'gold-loans',
      'deposit-withdraw': 'deposit-withdraw',
      'tracking': 'tracking',
      'aadhar-verification': 'aadhar-verification',
      'gold-rate': 'gold-rate',
      'login-history': 'login-history',
      'update-history': 'update-history',
      'daily-report': 'daily-report',
      'bulk-export': 'manage-users'
    };
    
    const featureId = featureAccessMap[section];
    if (featureId && !this.hasFeatureAccess(featureId)) {
      this.alertService.error('Access Denied', 'This feature has been disabled by the manager.');
      return;
    }
    
    // Load transactions in dashboard if section is transactions
    if (section === 'transactions') {
      console.log('Loading transactions in dashboard');
      // Check if there's a newly created account to filter by
      const newAccountNumber = sessionStorage.getItem('newAccountCreated');
      if (newAccountNumber) {
        sessionStorage.removeItem('newAccountCreated');
        this.loadAllTransactions();
        // Use setTimeout to ensure transactions are loaded before filtering
        setTimeout(() => {
          this.filterByNewAccount(newAccountNumber);
        }, 500);
      } else {
        this.loadAllTransactions();
      }
      this.activeSection = section;
      return;
    }
    
    // Navigate to external pages for these sections
    if (section === 'loans') {
      console.log('Navigating to loans page');
      this.navigateTo('loans');
      return;
    } else if (section === 'cheques') {
      console.log('Navigating to cheques page');
      this.navigateTo('cheques');
      return;
    } else if (section === 'profile') {
      this.navigateTo('profile');
      return;
    } else if (section === 'users') {
      this.navigateTo('users');
      return;
    } else if (section === 'user-control') {
      this.navigateTo('user-control');
      return;
    } else if (section === 'bulk-export') {
      this.activeSection = section;
      this.loadUsers(200);
      return;
    } else if (section === 'cards') {
      this.navigateTo('cards');
      return;
    } else if (section === 'kyc') {
      this.navigateTo('kyc');
      return;
    } else if (section === 'subsidy-claims') {
      this.navigateTo('subsidy-claims');
      return;
    } else if (section === 'education-loans') {
      this.navigateTo('education-loan-applications');
      return;
    } else if (section === 'support-tickets') {
      this.activeSection = section;
      this.loadSupportTickets();
      this.loadSupportTicketStats();
      this.startSupportTicketPolling();
      return;
    } else if (section === 'gold-loans') {
      this.navigateToGoldLoans();
      return;
    }
    
    // For sections that stay in dashboard
    this.activeSection = section;
    
    // Handle toggle sections
    if (section === 'tracking') {
      this.showTrackingSection = true;
      this.loadAccountTrackings();
    } else if (section === 'aadhar-verification') {
      this.showAadharVerificationSection = true;
      this.loadPendingAadharVerifications();
    } else if (section === 'gold-rate') {
      this.showGoldRateSection = true;
      this.loadCurrentGoldRate();
    } else if (section === 'login-history') {
      this.showLoginHistorySection = true;
      this.loadLoginHistory();
    } else if (section === 'update-history') {
      this.showUpdateHistory = true;
      this.loadUserUpdateHistory();
    } else if (section === 'profile-updates') {
      this.activeSection = section;
      this.loadProfileUpdateRequests();
      this.loadProfileUpdateHistory();
      return;
    } else if (section === 'daily-report') {
      this.showDailyActivityReport = true;
      this.loadDailyActivities();
    } else if (section === 'prediction-history') {
      this.loadLoanPredictions();
    } else if (section === 'passbook') {
      this.loadPassbookAccounts();
    } else if (section === 'credit-cards') {
      // Navigate to separate credit card management page
      this.navigateTo('credit-cards');
      return;
    } else if (section === 'investments') {
      console.log('Navigating to investments page');
      this.navigateTo('investments');
      return;
    } else if (section === 'fixed-deposits') {
      console.log('Navigating to fixed deposits page');
      this.navigateTo('fixed-deposits');
      return;
    } else if (section === 'emi-management') {
      console.log('Navigating to EMI management page');
      this.navigateTo('emi-management');
      return;
    } else if (section === 'current-accounts') {
      console.log('Navigating to current accounts page');
      this.navigateTo('current-accounts');
      return;
    } else if (section === 'deposit-withdraw') {
      // Check feature access
      if (!this.hasFeatureAccess('deposit-withdraw')) {
        this.alertService.error('Access Denied', 'Deposit/Withdraw feature has been disabled by the manager.');
        this.activeSection = 'profile';
        return;
      }
      // Load deposit/withdraw form
      this.loadDepositRequests();
    } else if (section === 'net-banking-control') {
      this.loadNetBankingStatus();
      this.loadNetBankingAuditHistory();
    } else if (section === 'biometric') {
      this.loadBiometricCredentials();
    } else if (section === 'payment-gateway') {
      this.loadPgPendingMerchants();
      this.loadPgApprovedMerchants();
    } else if (section === 'upi-management') {
      this.adminUpiTab = 'accounts';
      this.loadAdminUpiAccounts();
      this.loadAdminUpiTransactions();
      this.loadAdminUpiFlagged();
    } else if (section === 'atm-management') {
      this.atmActiveTab = 'overview';
      this.loadAtmDashboardStats();
      this.loadAtmMachines();
    } else if (section === 'signature-management') {
      this.sigLookupAccountNumber = '';
      this.sigLookupResult = null;
      this.sigLookupError = '';
      this.sigSelectedFile = null;
      this.sigUploadPreview = null;
      this.sigValidationResult = null;
      this.sigFormDownloaded = false;
    }
    
    // Close other sections
    if (section !== 'tracking') this.showTrackingSection = false;
    if (section !== 'aadhar-verification') this.showAadharVerificationSection = false;
    if (section !== 'gold-rate') this.showGoldRateSection = false;
    if (section !== 'login-history') this.showLoginHistorySection = false;
    if (section !== 'update-history') this.showUpdateHistory = false;
    if (section !== 'daily-report') this.showDailyActivityReport = false;
    if (section !== 'credit-cards') this.showCreditCardSection = false;
    if (section !== 'bill-payments') this.showBillPaymentsSection = false;
    if (section !== 'support-tickets') this.stopSupportTicketPolling();
    if (section !== 'investments') this.selectedInvestment = null;
    if (section !== 'fixed-deposits') this.selectedFD = null;
    if (section !== 'emi-management') this.selectedEMI = null;
  }
  
  // Investment Management Methods
  loadInvestments() {
    this.isLoadingInvestments = true;
    this.http.get(`${environment.apiBaseUrl}/api/investments`).subscribe({
      next: (investments: any) => {
        this.investments = investments || [];
        this.isLoadingInvestments = false;
        this.filterInvestments();
      },
      error: (err: any) => {
        console.error('Error loading investments:', err);
        this.alertService.error('Error', 'Failed to load investments');
        this.isLoadingInvestments = false;
      }
    });
  }
  
  filterInvestments() {
    // Filtering is handled by status filter in template
  }
  
  approveInvestment(investment: any) {
    if (!confirm(`Approve investment of ₹${investment.investmentAmount} for ${investment.userName}?`)) return;
    
    // Ensure ID is a number
    const investmentId = typeof investment.id === 'number' ? investment.id : Number(investment.id);
    if (isNaN(investmentId)) {
      this.alertService.error('Validation Error', 'Invalid investment ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/investments/${investmentId}/approve?approvedBy=${this.adminName}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Investment approved successfully');
          this.loadInvestments();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve investment');
        }
      },
      error: (err: any) => {
        console.error('Error approving investment:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve investment');
      }
    });
  }
  
  rejectInvestment(investment: any) {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;
    
    // Ensure ID is a number
    const investmentId = typeof investment.id === 'number' ? investment.id : Number(investment.id);
    if (isNaN(investmentId)) {
      this.alertService.error('Validation Error', 'Invalid investment ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/investments/${investmentId}/reject?rejectedBy=${this.adminName}&reason=${encodeURIComponent(reason)}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Investment rejected');
          this.loadInvestments();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject investment');
        }
      },
      error: (err: any) => {
        console.error('Error rejecting investment:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject investment');
      }
    });
  }
  
  // Fixed Deposit Management Methods
  loadFixedDeposits() {
    this.isLoadingFDs = true;
    this.http.get(`${environment.apiBaseUrl}/api/fixed-deposits`).subscribe({
      next: (fds: any) => {
        this.fixedDeposits = fds || [];
        this.isLoadingFDs = false;
      },
      error: (err: any) => {
        console.error('Error loading fixed deposits:', err);
        this.alertService.error('Error', 'Failed to load fixed deposits');
        this.isLoadingFDs = false;
      }
    });
  }
  
  approveFixedDeposit(fd: any) {
    const reason = prompt('Enter approval reason (optional):');
    const reasonParam = reason ? `&approvalReason=${encodeURIComponent(reason)}` : '';
    
    // Ensure ID is a number
    const fdId = typeof fd.id === 'number' ? fd.id : Number(fd.id);
    if (isNaN(fdId)) {
      this.alertService.error('Validation Error', 'Invalid fixed deposit ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/fixed-deposits/${fdId}/approve?approvedBy=${this.adminName}${reasonParam}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit approved successfully');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve FD');
        }
      },
      error: (err: any) => {
        console.error('Error approving FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve FD');
      }
    });
  }
  
  rejectFixedDeposit(fd: any) {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;
    
    // Ensure ID is a number
    const fdId = typeof fd.id === 'number' ? fd.id : Number(fd.id);
    if (isNaN(fdId)) {
      this.alertService.error('Validation Error', 'Invalid fixed deposit ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/fixed-deposits/${fdId}/reject?rejectedBy=${this.adminName}&reason=${encodeURIComponent(reason)}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit rejected');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject FD');
        }
      },
      error: (err: any) => {
        console.error('Error rejecting FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject FD');
      }
    });
  }
  
  processFDMaturity(fd: any) {
    if (!confirm(`Process maturity for FD ${fd.fdAccountNumber}? Maturity amount ₹${fd.maturityAmount} will be credited to account.`)) return;
    
    // Ensure ID is a number
    const fdId = typeof fd.id === 'number' ? fd.id : Number(fd.id);
    if (isNaN(fdId)) {
      this.alertService.error('Validation Error', 'Invalid fixed deposit ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/fixed-deposits/${fdId}/process-maturity?processedBy=${this.adminName}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'FD maturity processed successfully');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to process maturity');
        }
      },
      error: (err: any) => {
        console.error('Error processing FD maturity:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to process maturity');
      }
    });
  }
  
  // EMI Management Methods
  loadAllEMIs() {
    this.isLoadingAllEMIs = true;
    // Load all EMIs from all accounts
    this.http.get(`${environment.apiBaseUrl}/api/emis`).subscribe({
      next: (emis: any) => {
        this.allEmis = emis || [];
        this.isLoadingAllEMIs = false;
      },
      error: (err: any) => {
        console.error('Error loading EMIs:', err);
        // Try loading overdue EMIs instead
        this.http.get(`${environment.apiBaseUrl}/api/emis/overdue`).subscribe({
          next: (overdueEmis: any) => {
            this.allEmis = overdueEmis || [];
            this.isLoadingAllEMIs = false;
          },
          error: (err2: any) => {
            console.error('Error loading overdue EMIs:', err2);
            this.allEmis = [];
            this.isLoadingAllEMIs = false;
          }
        });
      }
    });
  }

  logout() {
    // Store logout timestamp
    this.logoutTime = new Date();
    
    // Update sessionStorage with logout time
    const adminData = sessionStorage.getItem('admin');
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        const logoutData = {
          loginTime: this.loginTime?.toISOString(),
          logoutTime: this.logoutTime.toISOString(),
          sessionDuration: this.sessionDuration
        };
        sessionStorage.setItem('adminLogoutData', JSON.stringify(logoutData));
        
        // Send logout data to backend
        this.http.post(`${environment.apiBaseUrl}/api/session-history/logout`, {
          userId: admin.id,
          userType: 'ADMIN',
          sessionDuration: this.sessionDuration
        }).subscribe({
          next: (response: any) => {
            console.log('Admin logout recorded successfully:', response);
          },
          error: (err) => {
            console.error('Error recording admin logout:', err);
          }
        });
      } catch (e) {
        console.error('Error updating logout time:', e);
      }
    }
    
    // Clear session timer
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = null;
    }
    
    this.alertService.logoutSuccess();
    this.router.navigate(['/admin/login']);
  }
  
  /**
   * Initialize session tracking - get login time from sessionStorage
   */
  initializeSessionTracking() {
    const adminLoginTime = sessionStorage.getItem('adminLoginTime');
    if (adminLoginTime) {
      try {
        this.loginTime = new Date(adminLoginTime);
      } catch (e) {
        console.error('Error parsing admin login time:', e);
        this.loginTime = new Date();
      }
    } else {
      // If loginTime not found, set it now
      this.loginTime = new Date();
      sessionStorage.setItem('adminLoginTime', this.loginTime.toISOString());
    }
  }
  
  /**
   * Start session timer to update duration every second
   */
  startSessionTimer() {
    if (!this.loginTime) return;
    
    this.updateSessionDuration();
    this.sessionTimer = setInterval(() => {
      this.updateSessionDuration();
    }, 1000);
  }
  
  /**
   * Update session duration display
   */
  updateSessionDuration() {
    if (!this.loginTime) {
      this.sessionDuration = '00:00:00';
      return;
    }
    
    const now = new Date();
    const diff = now.getTime() - this.loginTime.getTime();
    
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);
    
    this.sessionDuration = 
      String(hours).padStart(2, '0') + ':' +
      String(minutes).padStart(2, '0') + ':' +
      String(seconds).padStart(2, '0');
  }
  
  /**
   * Format date and time for session display
   */
  formatDateTimeForSession(date: Date | null): string {
    if (!date) return 'N/A';
    return date.toLocaleString('en-IN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: true
    });
  }

  loadUsers(pageSize: number = 100) {
    // Load users from MySQL database with pagination for better performance
    const showBulkLoading = this.activeSection === 'bulk-export';
    if (showBulkLoading) {
      this.isLoadingBulkUserList = true;
    }
    this.http.get(`${environment.apiBaseUrl}/api/users/admin/all?page=0&size=${pageSize}`)
      .pipe(
        timeout(15000), // 15 second timeout
        finalize(() => {
          if (showBulkLoading) {
            this.isLoadingBulkUserList = false;
          }
        }),
        catchError(err => {
          console.error('Error loading users from MySQL:', err);
          
          let errorMessage = 'Failed to load users. ';
          if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
            errorMessage = 'Request timed out. The server may be slow. ';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server. Please check if the backend is running on port 8080. ';
          } else if (err.status >= 500) {
            errorMessage = 'Server error occurred. The backend may be experiencing issues. ';
          } else if (err.status === 401 || err.status === 403) {
            errorMessage = 'Authentication error. Please log in again. ';
          }
          
          console.error(errorMessage);
          
          // Fallback to localStorage if database fails
          if (isPlatformBrowser(this.platformId)) {
            const usersData = localStorage.getItem('userProfiles');
            if (usersData) {
              try {
                this.users = JSON.parse(usersData);
                console.log('Using fallback data from localStorage');
                return of(this.users);
              } catch (e) {
                console.error('Error parsing localStorage data:', e);
              }
            }
          }
          
          this.users = [];
          console.log('No users found in database or localStorage');
          return of({ content: [] });
        })
      )
      .subscribe({
        next: (usersData: any) => {
          const usersList = usersData?.content ?? (Array.isArray(usersData) ? usersData : []);
          if (!usersList || usersList.length === 0) {
            // If empty array from error handler, try localStorage
            if (isPlatformBrowser(this.platformId)) {
              const usersData = localStorage.getItem('userProfiles');
              if (usersData) {
                try {
                  this.users = JSON.parse(usersData);
                  console.log('Using fallback data from localStorage');
                  return;
                } catch (e) {
                  console.error('Error parsing localStorage data:', e);
                }
              }
            }
            this.users = [];
            return;
          }
          
          console.log('Users loaded from MySQL:', usersList.length, 'users');
          this.users = usersList.map((user: any) => ({
            id: user.id.toString(),
            name: user.account?.name || user.username || 'Unknown User',
            email: user.email || 'No email',
            accountNumber: user.accountNumber || 'No account',
            balance: user.account?.balance || 0,
            status: user.account?.status || 'ACTIVE',
            phoneNumber: user.account?.phone || user.phoneNumber || '',
            dateOfBirth: user.account?.dob || user.dateOfBirth || '',
            address: user.account?.address || user.address || '',
            pan: user.account?.pan || user.pan || '',
            aadhar: user.account?.aadharNumber || user.aadhar || '',
            occupation: user.account?.occupation || user.occupation || '',
            income: user.account?.income || user.income || 0,
            accountType: user.account?.accountType || 'Savings Account',
            joinDate: user.createdAt ? new Date(user.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
            customerId: user.account?.customerId || ''
          }));
          
          // Save to localStorage as backup
          if (isPlatformBrowser(this.platformId)) {
            localStorage.setItem('userProfiles', JSON.stringify(this.users));
          }
          
          console.log('Processed users for admin dashboard:', this.users.length, 'users');
        },
        error: (err: any) => {
          // This should not be reached as catchError handles errors, but just in case
          console.error('Unexpected error in loadUsers subscribe:', err);
          this.users = [];
        }
      });
  }

  selectUser(user: UserProfile) {
    this.selectedUser = user;
    // Check feature access before opening deposit/withdraw modal
    if (!this.hasFeatureAccess('deposit-withdraw')) {
      this.alertService.error('Access Denied', 'Deposit/Withdraw feature has been disabled by the manager.');
      return;
    }
    this.showDepositWithdrawal = true;
    this.resetForm();
  }

  setOperationType(type: 'deposit' | 'withdrawal') {
    this.operationType = type;
    this.resetForm();
  }

  resetForm() {
    this.amount = 0;
    this.description = '';
    this.successMessage = '';
    this.errorMessage = '';
    this.accountNumber = '';
    this.accountType = 'regular';
    this.isAccountVerified = false;
    this.accountVerificationError = '';
    this.verifiedAccountDetails = null;
    this.showReceipt = false;
    this.receiptData = null;
  }

  validateAmount(): boolean {
    if (this.amount <= 0) {
      this.errorMessage = 'Amount must be greater than 0';
      return false;
    }

    if (this.operationType === 'withdrawal') {
      const currentBalance = this.verifiedAccountDetails?.balance || 0;
      if (this.amount > currentBalance) {
        this.errorMessage = 'Insufficient balance for withdrawal';
        return false;
      }
    }

    if (this.amount > 1000000) {
      this.errorMessage = 'Amount cannot exceed ₹10,00,000';
      return false;
    }

    return true;
  }

  processTransaction() {
    // Check feature access before processing
    if (!this.hasFeatureAccess('deposit-withdraw')) {
      this.alertService.error('Access Denied', 'Deposit/Withdraw feature has been disabled by the manager.');
      return;
    }
    // Use the same implementation as processDirectTransaction
    this.processDirectTransaction();
  }

  saveUserTransaction(transaction: TransactionRecord) {
    if (isPlatformBrowser(this.platformId)) {
      const userTransactionsKey = `transactions_${this.selectedUser!.accountNumber}`;
      const existingTransactions = localStorage.getItem(userTransactionsKey);
      const transactions = existingTransactions ? JSON.parse(existingTransactions) : [];
      
      transactions.unshift(transaction);
      localStorage.setItem(userTransactionsKey, JSON.stringify(transactions));
    }
  }

  closeDepositWithdrawal() {
    this.showDepositWithdrawal = false;
    this.selectedUser = null;
    this.resetForm();
  }

  loadDepositRequests(status: string = 'PENDING') {
    this.isLoadingDepositRequests = true;
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    this.http.get(`${environment.apiBaseUrl}/api/deposit-requests`, { params }).subscribe({
      next: (requests: any) => {
        this.depositRequests = requests || [];
        this.isLoadingDepositRequests = false;
      },
      error: (err: any) => {
        console.error('Error loading deposit requests:', err);
        this.isLoadingDepositRequests = false;
      }
    });
  }

  // Transfers management (admin)
  loadAllTransfers() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.isLoadingTransfers = true;
    this.http.get(`${environment.apiBaseUrl}/api/transfers`).pipe(
      timeout(12000),
      catchError(err => {
        console.error('Error loading transfers:', err);
        this.isLoadingTransfers = false;
        return of([]);
      })
    ).subscribe({
      next: (res: any) => {
        this.transfers = Array.isArray(res) ? res : (res.content || []);
        // Cache locally
        try { localStorage.setItem('admin_transfers', JSON.stringify(this.transfers)); } catch(e) {}
        this.isLoadingTransfers = false;
      },
      error: () => { this.isLoadingTransfers = false; }
    });
  }

  getFilteredTransfers(): any[] {
    let filtered = this.transfers || [];
    if (this.transfersTypeFilter && this.transfersTypeFilter !== 'ALL') {
      filtered = filtered.filter((t: any) => (t.transferType || t.type || '').toUpperCase() === this.transfersTypeFilter.toUpperCase());
    }
    if (this.transfersStatusFilter && this.transfersStatusFilter !== 'ALL') {
      filtered = filtered.filter((t: any) => (t.status || '').toUpperCase() === this.transfersStatusFilter.toUpperCase());
    }
    if (this.transfersSearchQuery && this.transfersSearchQuery.trim() !== '') {
      const q = this.transfersSearchQuery.toLowerCase();
      filtered = filtered.filter((t: any) => {
        return (t.transferId && t.transferId.toString().toLowerCase().includes(q)) ||
               (t.senderAccountNumber && t.senderAccountNumber.toLowerCase().includes(q)) ||
               (t.recipientAccountNumber && t.recipientAccountNumber.toLowerCase().includes(q)) ||
               (t.senderName && t.senderName.toLowerCase().includes(q)) ||
               (t.recipientName && t.recipientName.toLowerCase().includes(q));
      });
    }
    return filtered;
  }

  // Beneficiaries management (admin)
  loadAllBeneficiaries() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.isLoadingBeneficiaries = true;
    this.http.get(`${environment.apiBaseUrl}/api/beneficiaries`).pipe(
      timeout(10000),
      catchError(err => {
        console.error('Error loading beneficiaries:', err);
        this.isLoadingBeneficiaries = false;
        return of([]);
      })
    ).subscribe({
      next: (res: any) => {
        this.beneficiaries = Array.isArray(res) ? res : (res.content || []);
        try { localStorage.setItem('admin_beneficiaries', JSON.stringify(this.beneficiaries)); } catch(e) {}
        this.isLoadingBeneficiaries = false;
      },
      error: () => { this.isLoadingBeneficiaries = false; }
    });
  }

  getFilteredBeneficiaries(): any[] {
    let filtered = this.beneficiaries || [];
    if (this.beneficiariesSearchQuery && this.beneficiariesSearchQuery.trim() !== '') {
      const q = this.beneficiariesSearchQuery.toLowerCase();
      filtered = filtered.filter((b: any) =>
        (b.accountNumber && b.accountNumber.toLowerCase().includes(q)) ||
        (b.name && b.name.toLowerCase().includes(q)) ||
        (b.nickname && b.nickname.toLowerCase().includes(q))
      );
    }
    return filtered;
  }

  approveDepositRequest(request: DepositRequest) {
    if (!confirm(`Approve deposit of ₹${request.amount} for ${request.userName}?`)) return;
    const adminName = this.adminName || 'Admin';
    // Ensure ID is a number
    const requestId = typeof request.id === 'number' ? request.id : Number(request.id);
    if (isNaN(requestId)) {
      this.alertService.error('Validation Error', 'Invalid deposit request ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/deposit-requests/${requestId}/approve`, {}, {
      params: { processedBy: adminName }
    }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Approved', 'Deposit credited successfully');
          this.loadDepositRequests();
          this.loadUsers(); // refresh balances
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve request');
        }
      },
      error: (err: any) => {
        console.error('Error approving deposit request:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve request');
      }
    });
  }

  rejectDepositRequest(request: DepositRequest) {
    const reason = prompt('Enter rejection reason (optional):') || '';
    const adminName = this.adminName || 'Admin';
    // Ensure ID is a number
    const requestId = typeof request.id === 'number' ? request.id : Number(request.id);
    if (isNaN(requestId)) {
      this.alertService.error('Validation Error', 'Invalid deposit request ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/deposit-requests/${requestId}/reject`, {}, {
      params: { processedBy: adminName, reason }
    }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Rejected', 'Deposit request rejected');
          this.loadDepositRequests();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject request');
        }
      },
      error: (err: any) => {
        console.error('Error rejecting deposit request:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject request');
      }
    });
  }

  get filteredUsers() {
    return this.users; // Show all users in dropdown
  }

  get userStats() {
    return {
      totalUsers: this.users.length,
      approvedUsers: this.users.filter(u => u.status === 'APPROVED').length,
      pendingUsers: this.users.filter(u => u.status === 'PENDING').length,
      totalBalance: this.users.reduce((sum, user) => sum + user.balance, 0)
    };
  }

  onUserSelect() {
    if (this.selectedUserId) {
      this.selectedUser = this.users.find(user => user.id === this.selectedUserId) || null;
    } else {
      this.selectedUser = null;
    }
    this.resetForm();
  }

  // Verify account number based on account type
  verifyAccountNumber() {
    if (!this.accountNumber || this.accountNumber.trim() === '') {
      this.isAccountVerified = false;
      this.accountVerificationError = '';
      this.verifiedAccountDetails = null;
      return;
    }

    this.isVerifyingAccount = true;
    this.accountVerificationError = '';
    this.isAccountVerified = false;
    this.verifiedAccountDetails = null;

    const accountNum = this.accountNumber.trim();

    switch (this.accountType) {
      case 'regular':
        this.verifyRegularAccount(accountNum);
        break;
      case 'loan':
        this.verifyLoanAccount(accountNum);
        break;
      case 'goldloan':
        this.verifyGoldLoanAccount(accountNum);
        break;
      case 'cheque':
        this.verifyChequeAccount(accountNum);
        break;
      case 'salary':
        this.verifySalaryAccount(accountNum);
        break;
      case 'current':
        this.verifyCurrentAccount(accountNum);
        break;
    }
  }

  verifyRegularAccount(accountNumber: string) {
    this.http.get(`${environment.apiBaseUrl}/api/accounts/number/${accountNumber}`).subscribe({
      next: (accountData: any) => {
        this.verifiedAccountDetails = {
          accountNumber: accountData.accountNumber,
          accountHolderName: accountData.name,
          balance: accountData.balance || 0,
          accountType: 'Regular Account',
          type: 'regular'
        };
        this.isAccountVerified = true;
        this.isVerifyingAccount = false;
        this.accountVerificationError = '';
      },
      error: (err: any) => {
        console.error('Error verifying regular account:', err);
        this.isAccountVerified = false;
        this.isVerifyingAccount = false;
        this.accountVerificationError = 'Account not found. Please check the account number.';
        this.verifiedAccountDetails = null;
      }
    });
  }

  verifyLoanAccount(accountNumber: string) {
    // First try to find by loan account number
    this.http.get(`${environment.apiBaseUrl}/api/loans`).subscribe({
      next: (loans: any) => {
        const loan = Array.isArray(loans) ? loans.find((l: any) => 
          l.loanAccountNumber === accountNumber || l.accountNumber === accountNumber
        ) : null;
        
        if (loan) {
          this.verifiedAccountDetails = {
            accountNumber: loan.loanAccountNumber || loan.accountNumber,
            accountHolderName: loan.userName,
            balance: loan.amount || 0,
            accountType: `${loan.type || 'Loan'} Account`,
            type: 'loan',
            loanDetails: loan
          };
          this.isAccountVerified = true;
          this.isVerifyingAccount = false;
          this.accountVerificationError = '';
        } else {
          // Try by account number
          this.http.get(`${environment.apiBaseUrl}/api/loans/account/${accountNumber}`).subscribe({
            next: (loans: any) => {
              const loanList = Array.isArray(loans) ? loans : [];
              if (loanList.length > 0) {
                const firstLoan = loanList[0];
                this.verifiedAccountDetails = {
                  accountNumber: firstLoan.loanAccountNumber || accountNumber,
                  accountHolderName: firstLoan.userName,
                  balance: firstLoan.amount || 0,
                  accountType: `${firstLoan.type || 'Loan'} Account`,
                  type: 'loan',
                  loanDetails: firstLoan
                };
                this.isAccountVerified = true;
                this.isVerifyingAccount = false;
                this.accountVerificationError = '';
              } else {
                this.isAccountVerified = false;
                this.isVerifyingAccount = false;
                this.accountVerificationError = 'Loan account not found. Please check the account number.';
                this.verifiedAccountDetails = null;
              }
            },
            error: () => {
              this.isAccountVerified = false;
              this.isVerifyingAccount = false;
              this.accountVerificationError = 'Loan account not found. Please check the account number.';
              this.verifiedAccountDetails = null;
            }
          });
        }
      },
      error: () => {
        this.isAccountVerified = false;
        this.isVerifyingAccount = false;
        this.accountVerificationError = 'Loan account not found. Please check the account number.';
        this.verifiedAccountDetails = null;
      }
    });
  }

  verifyGoldLoanAccount(accountNumber: string) {
    this.http.get(`${environment.apiBaseUrl}/api/gold-loans`)
      .pipe(
        timeout(8000),
        catchError(err => {
          console.warn('Error loading gold loans for verification:', err);
          return of([]);
        })
      )
      .subscribe({
        next: (loans: any) => {
          const loan = Array.isArray(loans) ? loans.find((l: any) => 
            l.loanAccountNumber === accountNumber || l.accountNumber === accountNumber
          ) : null;
        
        if (loan) {
          this.verifiedAccountDetails = {
            accountNumber: loan.loanAccountNumber || loan.accountNumber,
            accountHolderName: loan.userName,
            balance: loan.loanAmount || 0,
            accountType: 'Gold Loan Account',
            type: 'goldloan',
            loanDetails: loan
          };
          this.isAccountVerified = true;
          this.isVerifyingAccount = false;
          this.accountVerificationError = '';
        } else {
          // Try by account number
          this.http.get(`${environment.apiBaseUrl}/api/gold-loans/account/${accountNumber}`)
            .pipe(
              timeout(8000),
              catchError(err => {
                console.warn('Error loading gold loans by account:', err);
                return of([]);
              })
            )
            .subscribe({
            next: (loans: any) => {
              const loanList = Array.isArray(loans) ? loans : [];
              if (loanList.length > 0) {
                const firstLoan = loanList[0];
                this.verifiedAccountDetails = {
                  accountNumber: firstLoan.loanAccountNumber || accountNumber,
                  accountHolderName: firstLoan.userName,
                  balance: firstLoan.loanAmount || 0,
                  accountType: 'Gold Loan Account',
                  type: 'goldloan',
                  loanDetails: firstLoan
                };
                this.isAccountVerified = true;
                this.isVerifyingAccount = false;
                this.accountVerificationError = '';
              } else {
                this.isAccountVerified = false;
                this.isVerifyingAccount = false;
                this.accountVerificationError = 'Gold loan account not found. Please check the account number.';
                this.verifiedAccountDetails = null;
              }
            },
            error: () => {
              this.isAccountVerified = false;
              this.isVerifyingAccount = false;
              this.accountVerificationError = 'Gold loan account not found. Please check the account number.';
              this.verifiedAccountDetails = null;
            }
          });
        }
      },
      error: () => {
        this.isAccountVerified = false;
        this.isVerifyingAccount = false;
        this.accountVerificationError = 'Gold loan account not found. Please check the account number.';
        this.verifiedAccountDetails = null;
      }
    });
  }

  verifyChequeAccount(accountNumber: string) {
    this.http.get(`${environment.apiBaseUrl}/api/cheques/account/${accountNumber}`).subscribe({
      next: (cheques: any) => {
        const chequeList = Array.isArray(cheques) ? cheques : [];
        if (chequeList.length > 0) {
          const firstCheque = chequeList[0];
          this.verifiedAccountDetails = {
            accountNumber: accountNumber,
            accountHolderName: firstCheque.accountHolderName,
            balance: 0, // Cheque accounts don't have balance
            accountType: 'Cheque Account',
            type: 'cheque',
            chequeDetails: firstCheque
          };
          this.isAccountVerified = true;
          this.isVerifyingAccount = false;
          this.accountVerificationError = '';
        } else {
          this.isAccountVerified = false;
          this.isVerifyingAccount = false;
          this.accountVerificationError = 'Cheque account not found. Please check the account number.';
          this.verifiedAccountDetails = null;
        }
      },
      error: () => {
        this.isAccountVerified = false;
        this.isVerifyingAccount = false;
        this.accountVerificationError = 'Cheque account not found. Please check the account number.';
        this.verifiedAccountDetails = null;
      }
    });
  }

  onAccountNumberChange() {
    // Reset verification when account number changes
    this.isAccountVerified = false;
    this.accountVerificationError = '';
    this.verifiedAccountDetails = null;
  }

  verifySalaryAccount(accountNumber: string) {
    this.http.get(`${environment.apiBaseUrl}/api/salary-accounts/number/${accountNumber}`).subscribe({
      next: (accountData: any) => {
        this.verifiedAccountDetails = {
          accountNumber: accountData.accountNumber,
          accountHolderName: accountData.employeeName || 'Salary Account Holder',
          balance: accountData.balance || 0,
          accountType: 'Salary Account',
          type: 'salary',
          accountId: accountData.id
        };
        this.isAccountVerified = true;
        this.isVerifyingAccount = false;
        this.accountVerificationError = '';
      },
      error: (err: any) => {
        this.isAccountVerified = false;
        this.isVerifyingAccount = false;
        this.accountVerificationError = 'Salary account not found. Please check the account number.';
        this.verifiedAccountDetails = null;
      }
    });
  }

  verifyCurrentAccount(accountNumber: string) {
    this.http.get(`${environment.apiBaseUrl}/api/current-accounts/number/${accountNumber}`).subscribe({
      next: (accountData: any) => {
        this.verifiedAccountDetails = {
          accountNumber: accountData.accountNumber,
          accountHolderName: accountData.businessName || accountData.ownerName || 'Current Account Holder',
          balance: accountData.balance || 0,
          accountType: 'Current Account',
          type: 'current'
        };
        this.isAccountVerified = true;
        this.isVerifyingAccount = false;
        this.accountVerificationError = '';
      },
      error: (err: any) => {
        this.isAccountVerified = false;
        this.isVerifyingAccount = false;
        this.accountVerificationError = 'Current account not found. Please check the account number.';
        this.verifiedAccountDetails = null;
      }
    });
  }

  processDirectTransaction() {
    // Check feature access before processing
    if (!this.hasFeatureAccess('deposit-withdraw')) {
      this.alertService.error('Access Denied', 'Deposit/Withdraw feature has been disabled by the manager.');
      return;
    }
    if (!this.isAccountVerified || !this.verifiedAccountDetails) {
      this.errorMessage = 'Please verify the account number first';
      return;
    }

    if (!this.validateAmount()) {
      return;
    }

    if (!this.description.trim()) {
      this.errorMessage = 'Please enter a description';
      return;
    }

    // Disable form during processing
    const processingMessage = this.operationType === 'deposit' ? 'Processing deposit...' : 'Processing withdrawal...';
    this.successMessage = processingMessage;
    this.errorMessage = '';

    const accountNumber = this.verifiedAccountDetails.accountNumber;
    const userName = this.verifiedAccountDetails.accountHolderName;
    const currentBalance = this.verifiedAccountDetails.balance || 0;

    // Handle different account types
    if (this.accountType === 'regular') {
      this.processRegularAccountTransaction(accountNumber, userName, currentBalance);
    } else if (this.accountType === 'loan') {
      this.processLoanAccountTransaction(accountNumber, userName, currentBalance);
    } else if (this.accountType === 'goldloan') {
      this.processGoldLoanAccountTransaction(accountNumber, userName, currentBalance);
    } else if (this.accountType === 'cheque') {
      this.processChequeAccountTransaction(accountNumber, userName);
    } else if (this.accountType === 'salary') {
      this.processSalaryAccountTransaction(accountNumber, userName, currentBalance);
    } else if (this.accountType === 'current') {
      this.processCurrentAccountTransaction(accountNumber, userName, currentBalance);
    }
  }

  processRegularAccountTransaction(accountNumber: string, userName: string, currentBalance: number) {
    const balanceEndpoint = this.operationType === 'deposit' 
      ? `${environment.apiBaseUrl}/api/accounts/balance/credit/${accountNumber}?amount=${this.amount}`
      : `${environment.apiBaseUrl}/api/accounts/balance/debit/${accountNumber}?amount=${this.amount}`;

    this.http.put(balanceEndpoint, {}).subscribe({
      next: (balanceResponse: any) => {
        console.log('Balance updated in database:', balanceResponse);
        const newBalance = balanceResponse.balance;
        this.completeTransaction(accountNumber, userName, newBalance, 'regular');
      },
      error: (balanceErr: any) => {
        console.error('Error updating balance in database:', balanceErr);
        const errorMsg = balanceErr.error?.error || balanceErr.message || 'Failed to process transaction';
        this.errorMessage = errorMsg;
        this.successMessage = '';
      }
    });
  }

  processLoanAccountTransaction(accountNumber: string, userName: string, currentBalance: number) {
    // For loan accounts, we need to update the loan balance or create a transaction
    // Since loans have different structure, we'll create a transaction record
    const newBalance = this.operationType === 'deposit' 
      ? currentBalance + this.amount 
      : currentBalance - this.amount;
    
    if (this.operationType === 'withdrawal' && newBalance < 0) {
      this.errorMessage = 'Insufficient balance for withdrawal';
      this.successMessage = '';
      return;
    }

    this.completeTransaction(accountNumber, userName, newBalance, 'loan');
  }

  processGoldLoanAccountTransaction(accountNumber: string, userName: string, currentBalance: number) {
    // Similar to loan accounts
    const newBalance = this.operationType === 'deposit' 
      ? currentBalance + this.amount 
      : currentBalance - this.amount;
    
    if (this.operationType === 'withdrawal' && newBalance < 0) {
      this.errorMessage = 'Insufficient balance for withdrawal';
      this.successMessage = '';
      return;
    }

    this.completeTransaction(accountNumber, userName, newBalance, 'goldloan');
  }

  processChequeAccountTransaction(accountNumber: string, userName: string) {
    // Cheque accounts don't have balance, so we just create a transaction record
    this.completeTransaction(accountNumber, userName, 0, 'cheque');
  }

  processSalaryAccountTransaction(accountNumber: string, userName: string, currentBalance: number) {
    const newBalance = this.operationType === 'deposit'
      ? currentBalance + this.amount
      : currentBalance - this.amount;

    if (this.operationType === 'withdrawal' && newBalance < 0) {
      this.errorMessage = 'Insufficient balance for withdrawal';
      this.successMessage = '';
      return;
    }

    const accountId = this.verifiedAccountDetails?.accountId;
    if (accountId) {
      // Update balance via the update endpoint
      this.http.put(`${environment.apiBaseUrl}/api/salary-accounts/update/${accountId}`, { balance: newBalance }).subscribe({
        next: () => {
          this.completeTransaction(accountNumber, userName, newBalance, 'salary');
        },
        error: () => {
          this.completeTransaction(accountNumber, userName, newBalance, 'salary');
        }
      });
    } else {
      this.completeTransaction(accountNumber, userName, newBalance, 'salary');
    }
  }

  processCurrentAccountTransaction(accountNumber: string, userName: string, currentBalance: number) {
    const txnType = this.operationType === 'deposit' ? 'Credit' : 'Debit';
    this.http.post(
      `${environment.apiBaseUrl}/api/current-accounts/transactions/process?accountNumber=${accountNumber}&txnType=${txnType}&amount=${this.amount}&description=${encodeURIComponent(this.description)}`,
      {}
    ).subscribe({
      next: (response: any) => {
        if (response.success) {
          const newBalance = response.transaction?.balance || (this.operationType === 'deposit' ? currentBalance + this.amount : currentBalance - this.amount);
          this.completeTransaction(accountNumber, userName, newBalance, 'current');
        } else {
          this.errorMessage = response.error || 'Transaction failed';
          this.successMessage = '';
        }
      },
      error: (err: any) => {
        this.errorMessage = err.error?.error || 'Transaction failed for current account';
        this.successMessage = '';
      }
    });
  }

  completeTransaction(accountNumber: string, userName: string, newBalance: number, accountType: string) {
    const transactionId = `TXN${Date.now()}`;
    
    // Prepare transaction data for backend (without id and date - backend handles these)
    const backendTransactionData = {
      transactionId: transactionId,
      merchant: this.operationType === 'deposit' ? 'Admin Deposit' : 'Admin Withdrawal',
      amount: this.amount,
      type: this.operationType === 'deposit' ? 'Credit' : 'Debit',
      description: this.description,
      balance: newBalance,
      status: 'Completed',
      userName: userName,
      accountNumber: accountNumber
    };

    // Prepare transaction record for local storage (with id and date)
    const transactionRecord: TransactionRecord = {
      id: transactionId,
      transactionId: transactionId,
      merchant: backendTransactionData.merchant,
      amount: this.amount,
      type: backendTransactionData.type,
      description: this.description,
      balance: newBalance,
      date: new Date().toISOString(),
      status: 'Completed',
      userName: userName,
      accountNumber: accountNumber
    };

    this.http.post(`${environment.apiBaseUrl}/api/transactions`, backendTransactionData).subscribe({
      next: (transactionResponse: any) => {
        console.log('Transaction saved to database:', transactionResponse);

        // Generate receipt
        this.generateReceipt(transactionId, accountNumber, userName, newBalance, accountType);

        // Save to admin transaction history (use the record with id and date)
        this.saveAdminTransaction(transactionRecord);

        // Show success message
        this.successMessage = `${this.operationType === 'deposit' ? 'Deposit' : 'Withdrawal'} of ₹${this.amount} successful!`;
        this.errorMessage = '';

        // Refresh user data
        this.refreshUsersData();
      },
      error: (txnErr: any) => {
        console.error('Error saving transaction to database:', txnErr);
        console.error('Error details:', txnErr.error);
        // Still generate receipt even if transaction save fails
        this.generateReceipt(transactionId, accountNumber, userName, newBalance, accountType);
        this.saveAdminTransaction(transactionRecord);
        this.errorMessage = 'Transaction processed but failed to save transaction history.';
        this.successMessage = `${this.operationType === 'deposit' ? 'Deposit' : 'Withdrawal'} completed!`;
      }
    });
  }

  generateReceipt(transactionId: string, accountNumber: string, userName: string, newBalance: number, accountType: string) {
    this.receiptData = {
      transactionId: transactionId,
      accountNumber: accountNumber,
      accountHolderName: userName,
      accountType: this.verifiedAccountDetails.accountType,
      transactionType: this.operationType === 'deposit' ? 'Deposit' : 'Withdrawal',
      amount: this.amount,
      description: this.description,
      previousBalance: this.verifiedAccountDetails.balance || 0,
      newBalance: newBalance,
      date: new Date().toLocaleString('en-IN'),
      adminName: this.adminName
    };
    this.showReceipt = true;
  }

  closeReceipt() {
    this.showReceipt = false;
    this.receiptData = null;
    // Reset form after closing receipt
    setTimeout(() => {
      this.clearForm();
    }, 500);
  }

  printReceipt() {
    if (!isPlatformBrowser(this.platformId)) return;
    window.print();
  }

  saveAdminTransaction(transaction: TransactionRecord) {
    if (isPlatformBrowser(this.platformId)) {
      const adminTransactionsKey = 'adminTransactions';
      const existingTransactions = localStorage.getItem(adminTransactionsKey);
      const transactions = existingTransactions ? JSON.parse(existingTransactions) : [];
      
      transactions.unshift(transaction);
      localStorage.setItem(adminTransactionsKey, JSON.stringify(transactions));
      
      // Update local array
      this.adminTransactionHistory = transactions;
    }
  }

  loadAdminTransactionHistory() {
    if (isPlatformBrowser(this.platformId)) {
      const adminTransactionsKey = 'adminTransactions';
      const existingTransactions = localStorage.getItem(adminTransactionsKey);
      this.adminTransactionHistory = existingTransactions ? JSON.parse(existingTransactions) : [];
    }
  }

  loadUserUpdateHistory() {
    if (isPlatformBrowser(this.platformId)) {
      const historyKey = 'adminUserUpdateHistory';
      const existingHistory = localStorage.getItem(historyKey);
      this.userUpdateHistory = existingHistory ? JSON.parse(existingHistory) : [];
    }
  }

  toggleUpdateHistorySection() {
    this.showUpdateHistory = !this.showUpdateHistory;
    if (this.showUpdateHistory && this.userUpdateHistory.length === 0) {
      this.loadUserUpdateHistory();
    }
  }

  getFilteredUpdateHistory(): any[] {
    let filtered = this.userUpdateHistory;

    // Filter by date range
    const now = new Date();
    if (this.updateHistoryFilter === 'TODAY') {
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      filtered = filtered.filter(h => new Date(h.timestamp) >= today);
    } else if (this.updateHistoryFilter === 'WEEK') {
      const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
      filtered = filtered.filter(h => new Date(h.timestamp) >= weekAgo);
    } else if (this.updateHistoryFilter === 'MONTH') {
      const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
      filtered = filtered.filter(h => new Date(h.timestamp) >= monthAgo);
    }

    // Filter by search query
    if (this.updateHistorySearchQuery && this.updateHistorySearchQuery.trim() !== '') {
      const query = this.updateHistorySearchQuery.toLowerCase();
      filtered = filtered.filter(h =>
        (h.userName && h.userName.toLowerCase().includes(query)) ||
        (h.userEmail && h.userEmail.toLowerCase().includes(query)) ||
        (h.accountNumber && h.accountNumber.toLowerCase().includes(query)) ||
        (h.adminName && h.adminName.toLowerCase().includes(query)) ||
        (h.changes && h.changes.some((c: any) => 
          c.field.toLowerCase().includes(query) ||
          c.oldValue?.toLowerCase().includes(query) ||
          c.newValue?.toLowerCase().includes(query)
        ))
      );
    }

    return filtered;
  }

  // Load pending profile updates for badge count (lightweight)
  loadPendingProfileUpdatesForBadge() {
    this.http.get(`${environment.apiBaseUrl}/api/admins/profile-update/pending`).subscribe({
      next: (requests: any) => {
        this.pendingProfileUpdates = requests || [];
      },
      error: (err: any) => {
        console.error('Error loading pending profile updates for badge:', err);
        // Non-critical, just set to empty
        this.pendingProfileUpdates = [];
      }
    });
  }

  // Profile Update Request Methods
  loadProfileUpdateRequests() {
    this.isLoadingProfileUpdates = true;
    // Always load pending requests first for badge count
    this.http.get(`${environment.apiBaseUrl}/api/admins/profile-update/pending`).subscribe({
      next: (pendingRequests: any) => {
        this.pendingProfileUpdates = pendingRequests || [];
        
        // Then load based on filter
        if (this.profileUpdateFilter === 'PENDING') {
          this.profileUpdateRequests = pendingRequests || [];
          this.isLoadingProfileUpdates = false;
        } else {
          this.http.get(`${environment.apiBaseUrl}/api/admins/profile-update/all`).subscribe({
            next: (allRequests: any) => {
              this.profileUpdateRequests = allRequests || [];
              this.isLoadingProfileUpdates = false;
            },
            error: (err: any) => {
              console.error('Error loading all profile updates:', err);
              this.isLoadingProfileUpdates = false;
            }
          });
        }
      },
      error: (err: any) => {
        console.error('Error loading pending profile updates:', err);
        // Still try to load all if pending fails
        if (this.profileUpdateFilter !== 'PENDING') {
          this.http.get(`${environment.apiBaseUrl}/api/admins/profile-update/all`).subscribe({
            next: (allRequests: any) => {
              this.profileUpdateRequests = allRequests || [];
              this.isLoadingProfileUpdates = false;
            },
            error: (err2: any) => {
              console.error('Error loading all profile updates:', err2);
              this.isLoadingProfileUpdates = false;
            }
          });
        } else {
          this.isLoadingProfileUpdates = false;
        }
      }
    });
  }

  getFilteredProfileUpdates() {
    let filtered = [...this.profileUpdateRequests];

    if (this.profileUpdateFilter !== 'ALL' && this.profileUpdateFilter !== 'PENDING') {
      filtered = filtered.filter(req => req.status === this.profileUpdateFilter);
    }

    if (this.profileUpdateSearchQuery && this.profileUpdateSearchQuery.trim() !== '') {
      const query = this.profileUpdateSearchQuery.toLowerCase();
      filtered = filtered.filter(req =>
        (req.userName && req.userName.toLowerCase().includes(query)) ||
        (req.accountNumber && req.accountNumber.toLowerCase().includes(query)) ||
        (req.userEmail && req.userEmail.toLowerCase().includes(query)) ||
        (req.fieldToUpdate && req.fieldToUpdate.toLowerCase().includes(query))
      );
    }

    return filtered;
  }

  approveProfileUpdate(request: any) {
    if (!confirm(`Approve ${request.fieldToUpdate === 'ADDRESS' ? 'address' : 'phone number'} update for ${request.userName}?`)) return;
    
    const adminData = sessionStorage.getItem('admin');
    const adminName = adminData ? (JSON.parse(adminData).username || JSON.parse(adminData).name || 'Admin') : 'Admin';
    
    const requestId = typeof request.id === 'number' ? request.id : Number(request.id);
    if (isNaN(requestId)) {
      this.alertService.error('Validation Error', 'Invalid request ID');
      return;
    }
    
    this.http.put(`${environment.apiBaseUrl}/api/admins/profile-update/${requestId}/approve?approvedBy=${encodeURIComponent(adminName)}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Profile update approved successfully');
          this.loadProfileUpdateRequests();
          this.loadProfileUpdateHistory();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve update');
        }
      },
      error: (err: any) => {
        console.error('Error approving profile update:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve update');
      }
    });
  }

  rejectProfileUpdate(request: any) {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;
    
    const adminData = sessionStorage.getItem('admin');
    const adminName = adminData ? (JSON.parse(adminData).username || JSON.parse(adminData).name || 'Admin') : 'Admin';
    
    const requestId = typeof request.id === 'number' ? request.id : Number(request.id);
    if (isNaN(requestId)) {
      this.alertService.error('Validation Error', 'Invalid request ID');
      return;
    }
    
    this.http.put(`${environment.apiBaseUrl}/api/admins/profile-update/${requestId}/reject?rejectedBy=${encodeURIComponent(adminName)}&reason=${encodeURIComponent(reason)}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Profile update request rejected');
          this.loadProfileUpdateRequests();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject update');
        }
      },
      error: (err: any) => {
        console.error('Error rejecting profile update:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject update');
      }
    });
  }

  getProfileUpdateStatusClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'COMPLETED': return 'status-approved';
      case 'OTP_VERIFIED': case 'PENDING': return 'status-pending';
      case 'REJECTED': return 'status-rejected';
      default: return 'status-default';
    }
  }

  // Profile Update History Methods
  loadProfileUpdateHistory() {
    this.isLoadingProfileUpdateHistory = true;
    // Load all update requests and filter completed ones as history
    this.http.get(`${environment.apiBaseUrl}/api/admins/profile-update/all`).subscribe({
      next: (requests: any) => {
        // Get completed requests and convert to history format
        const completedRequests = (requests || []).filter((req: any) => req.status === 'COMPLETED');
        this.profileUpdateHistory = completedRequests.map((req: any) => ({
          id: req.id,
          userId: req.userId,
          accountNumber: req.accountNumber,
          userName: req.userName,
          userEmail: req.userEmail,
          fieldUpdated: req.fieldToUpdate,
          oldValue: req.oldValue,
          newValue: req.newValue,
          requestId: req.id,
          approvedBy: req.approvedBy,
          updateDate: req.completionDate || req.approvalDate || req.requestDate
        }));
        this.isLoadingProfileUpdateHistory = false;
      },
      error: (err: any) => {
        console.error('Error loading profile update history:', err);
        this.isLoadingProfileUpdateHistory = false;
      }
    });
  }

  getFilteredProfileUpdateHistory(): any[] {
    let filtered = [...this.profileUpdateHistory];

    // Filter by date range
    const now = new Date();
    if (this.profileUpdateHistoryFilter === 'TODAY') {
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      filtered = filtered.filter(h => {
        const updateDate = new Date(h.updateDate || h.completionDate);
        return updateDate >= today;
      });
    } else if (this.profileUpdateHistoryFilter === 'WEEK') {
      const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
      filtered = filtered.filter(h => {
        const updateDate = new Date(h.updateDate || h.completionDate);
        return updateDate >= weekAgo;
      });
    } else if (this.profileUpdateHistoryFilter === 'MONTH') {
      const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
      filtered = filtered.filter(h => {
        const updateDate = new Date(h.updateDate || h.completionDate);
        return updateDate >= monthAgo;
      });
    }

    // Filter by search query
    if (this.profileUpdateHistorySearchQuery && this.profileUpdateHistorySearchQuery.trim() !== '') {
      const query = this.profileUpdateHistorySearchQuery.toLowerCase();
      filtered = filtered.filter(h =>
        (h.userName && h.userName.toLowerCase().includes(query)) ||
        (h.accountNumber && h.accountNumber.toLowerCase().includes(query)) ||
        (h.userEmail && h.userEmail.toLowerCase().includes(query)) ||
        (h.fieldUpdated && h.fieldUpdated.toLowerCase().includes(query)) ||
        (h.oldValue && h.oldValue.toLowerCase().includes(query)) ||
        (h.newValue && h.newValue.toLowerCase().includes(query)) ||
        (h.approvedBy && h.approvedBy.toLowerCase().includes(query))
      );
    }

    return filtered;
  }

  // Daily Activity Report Methods
  toggleDailyActivityReport() {
    this.showDailyActivityReport = !this.showDailyActivityReport;
    if (this.showDailyActivityReport) {
      this.loadDailyActivities();
    }
  }

  loadDailyActivities() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const selectedDateObj = new Date(this.selectedDate);
    const startOfDay = new Date(selectedDateObj.setHours(0, 0, 0, 0));
    const endOfDay = new Date(selectedDateObj.setHours(23, 59, 59, 999));
    
    const allActivities: any[] = [];
    
    // Load user updates
    const updateHistory = localStorage.getItem('adminUserUpdateHistory');
    if (updateHistory) {
      const updates = JSON.parse(updateHistory);
      updates.forEach((update: any) => {
        const updateDate = new Date(update.timestamp);
        if (updateDate >= startOfDay && updateDate <= endOfDay) {
          allActivities.push({
            id: update.id,
            type: 'UPDATES',
            category: 'User Profile Update',
            timestamp: update.timestamp,
            adminName: update.adminName,
            description: `Updated user profile: ${update.userName} (${update.accountNumber}) - ${update.changeCount} field(s) changed`,
            details: {
              userName: update.userName,
              accountNumber: update.accountNumber,
              changes: update.changes
            }
          });
        }
      });
    }
    
    // Load transactions (deposits/withdrawals)
    const transactions = localStorage.getItem('adminTransactions');
    if (transactions) {
      const txns = JSON.parse(transactions);
      txns.forEach((txn: any) => {
        const txnDate = new Date(txn.date);
        if (txnDate >= startOfDay && txnDate <= endOfDay) {
          allActivities.push({
            id: txn.id || txn.transactionId,
            type: txn.type === 'Credit' ? 'DEPOSITS' : 'WITHDRAWALS',
            category: txn.type === 'Credit' ? 'Money Deposit' : 'Money Withdrawal',
            timestamp: txn.date,
            adminName: this.adminName,
            description: `${txn.type === 'Credit' ? 'Deposited' : 'Withdrew'} ₹${txn.amount} to/from account ${txn.accountNumber}`,
            details: {
              accountNumber: txn.accountNumber,
              amount: txn.amount,
              description: txn.description,
              balance: txn.balance
            }
          });
        }
      });
    }
    
    // Load from backend - loans, cheques, subsidy claims, accounts
    this.loadActivitiesFromBackend(startOfDay, endOfDay, allActivities);
  }

  loadActivitiesFromBackend(startOfDay: Date, endOfDay: Date, allActivities: any[]) {
    // Load loan approvals/rejections
    this.http.get(`${environment.apiBaseUrl}/api/loans`).subscribe({
      next: (loans: any) => {
        const loanList = Array.isArray(loans) ? loans : [];
        loanList.forEach((loan: any) => {
          if (loan.approvalDate) {
            const approvalDate = new Date(loan.approvalDate);
            if (approvalDate >= startOfDay && approvalDate <= endOfDay) {
              allActivities.push({
                id: `LOAN_${loan.id}`,
                type: 'LOANS',
                category: loan.status === 'Approved' ? 'Loan Approved' : 'Loan Rejected',
                timestamp: loan.approvalDate,
                adminName: loan.approvedBy || this.adminName,
                description: `${loan.status === 'Approved' ? 'Approved' : 'Rejected'} ${loan.type || 'Loan'} of ₹${loan.amount} for ${loan.userName}`,
                details: {
                  loanId: loan.loanAccountNumber,
                  loanType: loan.type,
                  amount: loan.amount,
                  userName: loan.userName,
                  accountNumber: loan.accountNumber,
                  status: loan.status
                }
              });
            }
          }
        });
        this.dailyActivities = [...allActivities].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
      },
      error: (err) => console.error('Error loading loans:', err)
    });

    // Load gold loan approvals/rejections
    this.http.get(`${environment.apiBaseUrl}/api/gold-loans`)
      .pipe(
        timeout(8000),
        catchError(err => {
          console.warn('Error loading gold loans for daily report:', err);
          return of([]);
        })
      )
      .subscribe({
        next: (loans: any) => {
          const loanList = Array.isArray(loans) ? loans : [];
          loanList.forEach((loan: any) => {
            if (loan.approvalDate) {
              const approvalDate = new Date(loan.approvalDate);
              if (approvalDate >= startOfDay && approvalDate <= endOfDay) {
                allActivities.push({
                  id: `GOLD_LOAN_${loan.id}`,
                  type: 'GOLD_LOANS',
                  category: loan.status === 'Approved' ? 'Gold Loan Approved' : 'Gold Loan Rejected',
                timestamp: loan.approvalDate,
                adminName: loan.approvedBy || this.adminName,
                description: `${loan.status === 'Approved' ? 'Approved' : 'Rejected'} Gold Loan of ₹${loan.loanAmount} for ${loan.userName}`,
                details: {
                  loanAccountNumber: loan.loanAccountNumber,
                  amount: loan.loanAmount,
                  userName: loan.userName,
                  accountNumber: loan.accountNumber,
                  status: loan.status
                }
              });
            }
          }
        });
          this.dailyActivities = [...allActivities].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
        }
      });

    // Load cheque operations
    this.http.get(`${environment.apiBaseUrl}/api/cheques`)
      .pipe(
        timeout(8000),
        catchError(err => {
          console.warn('Error loading cheques for daily report:', err);
          return of([]);
        })
      )
      .subscribe({
      next: (cheques: any) => {
        const chequeList = Array.isArray(cheques) ? cheques : (cheques.content || []);
        chequeList.forEach((cheque: any) => {
          if (cheque.drawnDate) {
            const drawnDate = new Date(cheque.drawnDate);
            if (drawnDate >= startOfDay && drawnDate <= endOfDay) {
              allActivities.push({
                id: `CHEQUE_DRAW_${cheque.id}`,
                type: 'CHEQUES',
                category: 'Cheque Drawn',
                timestamp: cheque.drawnDate,
                adminName: cheque.drawnBy || this.adminName,
                description: `Drew cheque ${cheque.chequeNumber} for ₹${cheque.amount || 0}`,
                details: {
                  chequeNumber: cheque.chequeNumber,
                  accountNumber: cheque.accountNumber,
                  amount: cheque.amount,
                  accountHolderName: cheque.accountHolderName
                }
              });
            }
          }
          if (cheque.bouncedDate) {
            const bouncedDate = new Date(cheque.bouncedDate);
            if (bouncedDate >= startOfDay && bouncedDate <= endOfDay) {
              allActivities.push({
                id: `CHEQUE_BOUNCE_${cheque.id}`,
                type: 'CHEQUES',
                category: 'Cheque Bounced',
                timestamp: cheque.bouncedDate,
                adminName: cheque.bouncedBy || this.adminName,
                description: `Bounced cheque ${cheque.chequeNumber} - Reason: ${cheque.bounceReason || 'N/A'}`,
                details: {
                  chequeNumber: cheque.chequeNumber,
                  accountNumber: cheque.accountNumber,
                  bounceReason: cheque.bounceReason
                }
              });
            }
          }
        });
          this.dailyActivities = [...allActivities].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
        }
      });

    // Load subsidy claims
    this.http.get(`${environment.apiBaseUrl}/api/education-loan-subsidy-claims`)
      .pipe(
        timeout(8000),
        catchError(err => {
          console.warn('Error loading subsidy claims for daily report:', err);
          return of([]);
        })
      )
      .subscribe({
      next: (claims: any) => {
        const claimList = Array.isArray(claims) ? claims : [];
        claimList.forEach((claim: any) => {
          if (claim.processedDate) {
            const processedDate = new Date(claim.processedDate);
            if (processedDate >= startOfDay && processedDate <= endOfDay) {
              allActivities.push({
                id: `SUBSIDY_${claim.id}`,
                type: 'SUBSIDY_CLAIMS',
                category: claim.status === 'Approved' ? 'Subsidy Claim Approved' : 'Subsidy Claim Rejected',
                timestamp: claim.processedDate,
                adminName: claim.processedBy || this.adminName,
                description: `${claim.status === 'Approved' ? 'Approved' : 'Rejected'} subsidy claim of ₹${claim.approvedSubsidyAmount || claim.calculatedSubsidyAmount || 0} for ${claim.userName}`,
                details: {
                  claimId: claim.id,
                  userName: claim.userName,
                  accountNumber: claim.accountNumber,
                  amount: claim.approvedSubsidyAmount || claim.calculatedSubsidyAmount,
                  status: claim.status
                }
              });
            }
          }
        });
          this.dailyActivities = [...allActivities].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
        }
      });

    // Load account openings
    // Use smaller page size (100 max) to prevent OutOfMemoryError on backend
    this.http.get(`${environment.apiBaseUrl}/api/accounts/all?page=0&size=100&sortBy=createdAt&sortDir=desc`).subscribe({
      next: (response: any) => {
        const accounts = response.content || (Array.isArray(response) ? response : []);
        accounts.forEach((account: any) => {
          if (account.createdAt) {
            const createdDate = new Date(account.createdAt);
            if (createdDate >= startOfDay && createdDate <= endOfDay) {
              allActivities.push({
                id: `ACCOUNT_${account.id}`,
                type: 'ACCOUNTS',
                category: 'Account Opened',
                timestamp: account.createdAt,
                adminName: this.adminName,
                description: `New account opened: ${account.accountNumber} - ${account.name || 'N/A'}`,
                details: {
                  accountNumber: account.accountNumber,
                  name: account.name,
                  accountType: account.accountType,
                  balance: account.balance || 0
                }
              });
            }
          }
        });
        this.dailyActivities = [...allActivities].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
      },
      error: (err) => {
        console.error('Error loading accounts:', err);
        this.dailyActivities = [...allActivities].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
      }
    });
  }

  getFilteredDailyActivities(): any[] {
    let filtered = this.dailyActivities;
    
    if (this.selectedActivityCategory !== 'ALL') {
      filtered = filtered.filter(a => a.type === this.selectedActivityCategory);
    }
    
    return filtered;
  }

  getActivityStats() {
    const activities = this.getFilteredDailyActivities();
    return {
      total: activities.length,
      updates: activities.filter(a => a.type === 'UPDATES').length,
      deposits: activities.filter(a => a.type === 'DEPOSITS').length,
      withdrawals: activities.filter(a => a.type === 'WITHDRAWALS').length,
      loans: activities.filter(a => a.type === 'LOANS').length,
      goldLoans: activities.filter(a => a.type === 'GOLD_LOANS').length,
      cheques: activities.filter(a => a.type === 'CHEQUES').length,
      subsidyClaims: activities.filter(a => a.type === 'SUBSIDY_CLAIMS').length,
      accounts: activities.filter(a => a.type === 'ACCOUNTS').length
    };
  }

  printDailyActivityReport() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const htmlContent = this.generateDailyActivityReportHTML();
    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(htmlContent);
      printWindow.document.close();
      printWindow.onload = () => {
        setTimeout(() => {
          printWindow.print();
        }, 250);
      };
    }
  }

  generateDailyActivityReportHTML(): string {
    const activities = this.getFilteredDailyActivities();
    const stats = this.getActivityStats();
    const reportDate = new Date(this.selectedDate).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
    const currentTime = new Date().toLocaleTimeString('en-IN');

    return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>NeoBank - Daily Activity Report</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            margin: 0;
            padding: 20px;
            background: #f8f9fa;
            color: #333;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }
        .header {
            text-align: center;
            border-bottom: 3px solid #0077cc;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }
        .bank-logo {
            font-size: 32px;
            font-weight: bold;
            color: #0077cc;
            margin-bottom: 10px;
        }
        .document-title {
            text-align: center;
            font-size: 24px;
            font-weight: bold;
            color: #333;
            margin: 30px 0;
            padding: 15px;
            background: linear-gradient(135deg, #e3f2fd, #bbdefb);
            border-radius: 8px;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 15px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            text-align: center;
            border-left: 4px solid #0077cc;
        }
        .stat-number {
            font-size: 24px;
            font-weight: bold;
            color: #0077cc;
        }
        .stat-label {
            font-size: 12px;
            color: #666;
            margin-top: 5px;
        }
        .activities-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        .activities-table th {
            background: linear-gradient(135deg, #0077cc, #0056b3);
            color: white;
            padding: 12px;
            text-align: left;
            font-weight: 600;
        }
        .activities-table td {
            padding: 10px 12px;
            border-bottom: 1px solid #e9ecef;
        }
        .activities-table tr:hover {
            background-color: #f8f9fa;
        }
        .activity-type {
            display: inline-block;
            padding: 4px 10px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 600;
            text-transform: uppercase;
        }
        .type-UPDATES { background: #e3f2fd; color: #1976d2; }
        .type-DEPOSITS { background: #d1fae5; color: #065f46; }
        .type-WITHDRAWALS { background: #fee2e2; color: #991b1b; }
        .type-LOANS { background: #fef3c7; color: #92400e; }
        .type-GOLD_LOANS { background: #fde68a; color: #78350f; }
        .type-CHEQUES { background: #e0e7ff; color: #3730a3; }
        .type-SUBSIDY_CLAIMS { background: #dbeafe; color: #1e40af; }
        .type-ACCOUNTS { background: #f3e8ff; color: #6b21a8; }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #e9ecef;
            text-align: center;
            color: #666;
            font-size: 12px;
        }
        @media print {
            body { margin: 0; }
            .container { box-shadow: none; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="bank-logo">🏦 NeoBank</div>
            <div style="font-size: 24px; color: #1e40af; margin-bottom: 5px;">NeoBank India Limited</div>
            <div style="font-size: 14px; color: #666; font-style: italic;">Relationship beyond banking</div>
        </div>

        <div class="document-title">
            📊 DAILY ACTIVITY REPORT
        </div>

        <div style="margin-bottom: 20px; padding: 15px; background: #e3f2fd; border-radius: 8px;">
            <p style="margin: 5px 0;"><strong>Report Date:</strong> ${reportDate}</p>
            <p style="margin: 5px 0;"><strong>Generated By:</strong> ${this.adminName}</p>
            <p style="margin: 5px 0;"><strong>Generated On:</strong> ${currentTime}</p>
        </div>

        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-number">${stats.total}</div>
                <div class="stat-label">Total Activities</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${stats.updates}</div>
                <div class="stat-label">User Updates</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${stats.deposits}</div>
                <div class="stat-label">Deposits</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${stats.withdrawals}</div>
                <div class="stat-label">Withdrawals</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${stats.loans}</div>
                <div class="stat-label">Loans</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${stats.goldLoans}</div>
                <div class="stat-label">Gold Loans</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${stats.cheques}</div>
                <div class="stat-label">Cheques</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${stats.subsidyClaims}</div>
                <div class="stat-label">Subsidy Claims</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${stats.accounts}</div>
                <div class="stat-label">Accounts Opened</div>
            </div>
        </div>

        <table class="activities-table">
            <thead>
                <tr>
                    <th>Time</th>
                    <th>Category</th>
                    <th>Activity</th>
                    <th>Admin</th>
                    <th>Details</th>
                </tr>
            </thead>
            <tbody>
                ${activities.map(activity => `
                <tr>
                    <td>${new Date(activity.timestamp).toLocaleTimeString('en-IN')}</td>
                    <td><span class="activity-type type-${activity.type}">${activity.category}</span></td>
                    <td>${activity.description}</td>
                    <td>${activity.adminName}</td>
                    <td>${this.formatActivityDetails(activity)}</td>
                </tr>
                `).join('')}
            </tbody>
        </table>

        <div class="footer">
            <p><strong>📍 Registered Office:</strong> NeoBank Tower, Financial District, Mumbai - 400001</p>
            <p><strong>📞 Customer Care:</strong> 1800-NEOBANK | <strong>📧 Email:</strong> support@neobank.in</p>
            <p><strong>📄 This is a computer generated report and does not require signature.</strong></p>
            <p>© ${new Date().getFullYear()} NeoBank. All rights reserved.</p>
        </div>
    </div>
</body>
</html>`;
  }

  formatActivityDetails(activity: any): string {
    if (!activity.details) return 'N/A';
    
    const details = activity.details;
    const parts: string[] = [];
    
    if (details.accountNumber) parts.push(`Account: ${details.accountNumber}`);
    if (details.amount) parts.push(`Amount: ₹${details.amount}`);
    if (details.userName) parts.push(`User: ${details.userName}`);
    if (details.loanId || details.loanAccountNumber) parts.push(`Loan: ${details.loanId || details.loanAccountNumber}`);
    if (details.chequeNumber) parts.push(`Cheque: ${details.chequeNumber}`);
    if (details.status) parts.push(`Status: ${details.status}`);
    
    return parts.join(' | ') || 'N/A';
  }

  clearForm() {
    this.selectedUserId = '';
    this.selectedUser = null;
    this.amount = 0;
    this.description = '';
    this.successMessage = '';
    this.errorMessage = '';
    this.accountNumber = '';
    this.accountType = 'regular';
    this.isAccountVerified = false;
    this.accountVerificationError = '';
    this.verifiedAccountDetails = null;
    this.showReceipt = false;
    this.receiptData = null;
  }

  // Update user balance in MySQL database
  updateUserBalanceInDatabase(user: UserProfile) {
    // Find user by account number
    this.http.get(`${environment.apiBaseUrl}/api/users/account/${user.accountNumber}`).subscribe({
      next: (dbUser: any) => {
        if (dbUser) {
          const updatedUser = {
            ...dbUser,
            account: {
              ...dbUser.account,
              balance: user.balance
            }
          };
          
          // Ensure ID is a number
          const userId = typeof dbUser.id === 'number' ? dbUser.id : Number(dbUser.id);
          if (isNaN(userId)) {
            console.error('Invalid user ID:', dbUser.id);
            return;
          }
          this.http.put(`${environment.apiBaseUrl}/api/users/update/${userId}`, updatedUser).subscribe({
            next: (response: any) => {
              console.log('User balance updated in MySQL:', response);
            },
            error: (err: any) => {
              console.error('Error updating user balance in database:', err);
            }
          });
        }
      },
      error: (err: any) => {
        console.error('Error finding user for balance update:', err);
      }
    });
  }

  // Search functionality methods
  searchUsers() {
    if (!this.searchQuery.trim()) {
      this.searchResults = [];
      return;
    }

    this.isSearching = true;
    const query = this.searchQuery.toLowerCase().trim();

    // Search in users array
    this.searchResults = this.users.filter(user => {
      // Search by name
      if (user.name.toLowerCase().includes(query)) {
        return true;
      }
      
      // Search by mobile number
      if (user.phoneNumber && user.phoneNumber.includes(query)) {
        return true;
      }
      
      // Search by account number
      if (user.accountNumber.includes(query)) {
        return true;
      }
      
      // Search by email
      if (user.email.toLowerCase().includes(query)) {
        return true;
      }
      
      return false;
    });

    // If searching by last 4 digits of card, search in cards
    if (query.length === 4 && /^\d{4}$/.test(query)) {
      this.searchByCardLastFourDigits(query);
    } else {
      this.isSearching = false;
    }
  }

  searchByCardLastFourDigits(lastFourDigits: string) {
    // Search for cards with matching last 4 digits
    this.http.get(`${environment.apiBaseUrl}/api/cards`).subscribe({
      next: (cards: any) => {
        const matchingCards = cards.filter((card: any) => 
          card.cardNumber && card.cardNumber.endsWith(lastFourDigits)
        );
        
        if (matchingCards.length > 0) {
          // Get users for matching cards by account number
          const accountNumbers = matchingCards.map((card: any) => card.accountNumber);
          const matchingUsers = this.users.filter(user => 
            accountNumbers.includes(user.accountNumber)
          );
          
          this.searchResults = [...this.searchResults, ...matchingUsers];
          // Remove duplicates
          this.searchResults = this.searchResults.filter((user, index, self) => 
            index === self.findIndex(u => u.id === user.id)
          );
        }
        
        this.isSearching = false;
      },
      error: (err: any) => {
        console.error('Error searching cards:', err);
        this.isSearching = false;
      }
    });
  }

  // Universal Search Methods
  performUniversalSearch() {
    if (!this.universalSearchQuery || !this.universalSearchQuery.trim()) {
      this.universalSearchResults = null;
      return;
    }

    this.isUniversalSearching = true;
    const query = this.universalSearchQuery.trim();

    this.http.get(`${environment.apiBaseUrl}/api/admin/search?q=${encodeURIComponent(query)}`)
      .pipe(
        timeout(10000), // 10 second timeout
        catchError(err => {
          console.error('Error performing universal search:', err);
          let errorMessage = 'Failed to perform search. ';
          if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
            errorMessage += 'Request timed out. The server may be slow.';
          } else if (err.status === 0) {
            errorMessage += 'Unable to connect to server. Please check if the backend is running.';
          } else if (err.status >= 500) {
            errorMessage += 'Server error. Please try again later.';
          } else if (err.error?.message) {
            errorMessage = err.error.message;
          }
          this.alertService.error('Search Error', errorMessage);
          this.isUniversalSearching = false;
          this.universalSearchResults = null;
          return of(null);
        })
      )
      .subscribe({
        next: (results: any) => {
          if (!results) return; // Error occurred
          this.universalSearchResults = results;
          this.isUniversalSearching = false;
          console.log('Universal search results:', results);
        }
      });
  }

  onSearchInputChange() {
    // Clear results when input changes
    if (!this.universalSearchQuery || this.universalSearchQuery.trim().length < 2) {
      this.universalSearchResults = null;
    }
  }

  clearUniversalSearch() {
    this.universalSearchQuery = '';
    this.universalSearchResults = null;
  }

  viewAccountDetails(accountNumber: string) {
    this.navigateTo('users');
  }

  viewUserDetails(accountNumber: string) {
    this.navigateTo('users');
  }

  viewLoanDetails(loanId: number) {
    this.navigateTo('loans');
  }

  viewChequeDetails(chequeNumber: string) {
    this.navigateTo('cheques');
  }

  viewCardDetails(cardId: number) {
    this.navigateTo('cards');
  }

  selectSearchedUser(user: UserProfile) {
    this.selectedSearchedUser = user;
    this.loadUserCompleteDetails(user);
  }

  loadUserCompleteDetails(user: UserProfile) {
    // Load user cards
    this.http.get(`${environment.apiBaseUrl}/api/cards/account/${user.accountNumber}`).subscribe({
      next: (cards: any) => {
        this.userCards = cards || [];
      },
      error: (err: any) => {
        console.error('Error loading user cards:', err);
        this.userCards = [];
      }
    });

    // Load user loans
    this.http.get(`${environment.apiBaseUrl}/api/loans/account/${user.accountNumber}`).subscribe({
      next: (loans: any) => {
        this.userLoans = loans || [];
      },
      error: (err: any) => {
        console.error('Error loading user loans:', err);
        this.userLoans = [];
      }
    });

    // Load user transactions
    this.http.get(`${environment.apiBaseUrl}/api/transactions/account/${user.accountNumber}?page=0&size=1000`).subscribe({
      next: (response: any) => {
        // Handle both Page response and direct array response
        if (response && response.content && Array.isArray(response.content)) {
          // Page response
          this.userTransactions = response.content.map((txn: any) => ({
            id: txn.id,
            type: txn.type,
            amount: txn.amount,
            balance: txn.balance,
            date: txn.date || txn.createdAt,
            description: txn.description || txn.merchant,
            merchant: txn.merchant,
            status: txn.status || 'Completed'
          }));
        } else if (Array.isArray(response)) {
          // Direct array response
          this.userTransactions = response.map((txn: any) => ({
            id: txn.id,
            type: txn.type,
            amount: txn.amount,
            balance: txn.balance,
            date: txn.date || txn.createdAt,
            description: txn.description || txn.merchant,
            merchant: txn.merchant,
            status: txn.status || 'Completed'
          }));
        } else {
          this.userTransactions = [];
        }
        console.log('User transactions loaded:', this.userTransactions.length);
      },
      error: (err: any) => {
        console.error('Error loading user transactions:', err);
        this.userTransactions = [];
      }
    });
  }

  closeUserDetails() {
    this.selectedSearchedUser = null;
    this.userCards = [];
    this.userLoans = [];
    this.userTransactions = [];
  }

  // Download individual user details as PDF
  downloadUserDetailsPDF(user: UserProfile) {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      const htmlContent = this.generateUserDetailsPDFContent(user);
      const printWindow = window.open('', '_blank');
      if (printWindow) {
        printWindow.document.write(htmlContent);
        printWindow.document.close();
        printWindow.onload = () => {
          setTimeout(() => {
            printWindow.print();
          }, 250);
        };
      }
    } catch (error) {
      console.error('Error generating user details PDF:', error);
      alert('Failed to download user details document. Please try again.');
    }
  }

  // Generate PDF content for individual user details
  private generateUserDetailsPDFContent(user: UserProfile): string {
    const currentDate = new Date().toLocaleDateString('en-IN');
    const currentTime = new Date().toLocaleTimeString('en-IN');
    const adminName = 'Admin';

    return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>NeoBank - User Details</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            margin: 0;
            padding: 20px;
            background: #f8f9fa;
            color: #333;
        }
        .container {
            max-width: 900px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }
        .header {
            text-align: center;
            border-bottom: 3px solid #0077cc;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }
        .bank-logo {
            font-size: 32px;
            font-weight: bold;
            color: #0077cc;
            margin-bottom: 10px;
        }
        .document-title {
            text-align: center;
            font-size: 20px;
            font-weight: bold;
            color: #333;
            margin: 30px 0;
            padding: 15px;
            background: linear-gradient(135deg, #e3f2fd, #bbdefb);
            border-radius: 8px;
        }
        .details-section {
            margin-bottom: 30px;
        }
        .details-section h4 {
            color: #0077cc;
            border-bottom: 2px solid #0077cc;
            padding-bottom: 10px;
            margin-bottom: 15px;
        }
        .details-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 15px;
        }
        .detail-item {
            background: #f8f9fa;
            padding: 12px;
            border-radius: 6px;
            border-left: 3px solid #0077cc;
        }
        .detail-item label {
            display: block;
            font-size: 12px;
            color: #666;
            margin-bottom: 5px;
            font-weight: 600;
        }
        .detail-item span {
            display: block;
            font-size: 14px;
            color: #333;
            font-weight: 500;
        }
        .balance-amount {
            color: #28a745;
            font-weight: 700;
            font-size: 16px;
        }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #e9ecef;
            text-align: center;
            color: #666;
            font-size: 12px;
        }
        @media print {
            body { margin: 0; }
            .container { box-shadow: none; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="bank-logo">🏦 NeoBank</div>
            <div style="font-size: 24px; color: #1e40af; margin-bottom: 5px;">NeoBank India Limited</div>
            <div style="font-size: 14px; color: #666; font-style: italic;">Relationship beyond banking</div>
        </div>

        <div class="document-title">
            👤 USER DETAILS REPORT
        </div>

        <div class="details-section">
            <h4>📋 Personal Information</h4>
            <div class="details-grid">
                <div class="detail-item">
                    <label>Full Name</label>
                    <span>${user.name || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Email</label>
                    <span>${user.email || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Account Number</label>
                    <span>${user.accountNumber || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Phone Number</label>
                    <span>${user.phoneNumber || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Date of Birth</label>
                    <span>${user.dateOfBirth || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Address</label>
                    <span>${user.address || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>PAN Number</label>
                    <span>${user.pan || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Aadhar Number</label>
                    <span>${user.aadhar || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Occupation</label>
                    <span>${user.occupation || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Annual Income</label>
                    <span>₹${user.income ? user.income.toLocaleString('en-IN', {minimumFractionDigits: 2, maximumFractionDigits: 2}) : '0.00'}</span>
                </div>
                <div class="detail-item">
                    <label>Account Type</label>
                    <span>${user.accountType || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Account Status</label>
                    <span>${user.status || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Account Opened</label>
                    <span>${user.joinDate || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Current Balance</label>
                    <span class="balance-amount">₹${user.balance ? user.balance.toLocaleString('en-IN', {minimumFractionDigits: 2, maximumFractionDigits: 2}) : '0.00'}</span>
                </div>
            </div>
        </div>

        <div class="footer">
            <p><strong>📍 Registered Office:</strong> NeoBank Tower, Financial District, Mumbai - 400001</p>
            <p><strong>📞 Customer Care:</strong> 1800-NEOBANK | <strong>📧 Email:</strong> support@neobank.in</p>
            <p><strong>🌐 Website:</strong> www.neobank.in</p>
            <p><strong>📄 This is a computer generated report and does not require signature.</strong></p>
            <p>© ${new Date().getFullYear()} NeoBank. All rights reserved.</p>
            <p><strong>Generated on:</strong> ${currentDate} at ${currentTime} | <strong>By:</strong> ${adminName}</p>
        </div>
    </div>
</body>
</html>`;
  }

  // Download all users details as PDF
  downloadAllUsersPDF() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    if (!this.users || this.users.length === 0) {
      alert('No users available to download');
      return;
    }

    try {
      const pdfContent = this.generateUsersPDFContent();
      
      // Create and download as HTML file (can be printed as PDF)
      const blob = new Blob([pdfContent], { type: 'text/html;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `NeoBank_All_Users_Details_${new Date().toISOString().split('T')[0]}.html`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      // Show instruction to user
      setTimeout(() => {
        const userChoice = confirm('Document downloaded! Would you like to open it now to save as PDF?\n\nClick OK to open, or Cancel to download later.');
        if (userChoice) {
          // Open the downloaded file in a new window for printing
          const newWindow = window.open();
          if (newWindow) {
            newWindow.document.write(pdfContent);
            newWindow.document.close();
            // Focus on the new window
            newWindow.focus();
          }
        }
      }, 500);
      
      console.log('Users PDF download initiated successfully');
    } catch (error) {
      console.error('Error downloading users PDF:', error);
      alert('Failed to download users document. Please try again.');
    }
  }

  // Generate PDF content with bank passbook style and all users details
  private generateUsersPDFContent(): string {
    const currentDate = new Date().toLocaleDateString('en-IN');
    const currentTime = new Date().toLocaleTimeString('en-IN');
    const adminName = 'Admin'; // You can get this from authentication service
    
    return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>NeoBank - All Users Details</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            margin: 0;
            padding: 20px;
            background: #f8f9fa;
            color: #333;
            position: relative;
        }
        .watermark {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%) rotate(-45deg);
            font-size: 60px;
            color: rgba(30, 64, 175, 0.1);
            font-weight: bold;
            z-index: -1;
            pointer-events: none;
            white-space: nowrap;
        }
        .watermark-logo {
            position: fixed;
            top: 20%;
            left: 20%;
            transform: rotate(-30deg);
            font-size: 40px;
            color: rgba(30, 64, 175, 0.08);
            z-index: -1;
            pointer-events: none;
        }
        .container {
            max-width: 1000px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            position: relative;
            z-index: 1;
        }
        .header {
            text-align: center;
            border-bottom: 3px solid #0077cc;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }
        .bank-logo {
            font-size: 32px;
            font-weight: bold;
            color: #0077cc;
            margin-bottom: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
        }
        .bank-name {
            font-size: 24px;
            color: #1e40af;
            margin-bottom: 5px;
        }
        .bank-tagline {
            font-size: 14px;
            color: #666;
            font-style: italic;
        }
        .document-title {
            text-align: center;
            font-size: 20px;
            font-weight: bold;
            color: #333;
            margin: 30px 0;
            padding: 15px;
            background: linear-gradient(135deg, #e3f2fd, #bbdefb);
            border-radius: 8px;
            border-left: 4px solid #0077cc;
        }
        .summary-stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: linear-gradient(135deg, #f8f9fa, #e9ecef);
            padding: 20px;
            border-radius: 12px;
            text-align: center;
            border-left: 4px solid #0077cc;
        }
        .stat-number {
            font-size: 2rem;
            font-weight: bold;
            color: #0077cc;
        }
        .stat-label {
            font-size: 0.9rem;
            color: #666;
            margin-top: 5px;
        }
        .users-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
            background: white;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .users-table th {
            background: linear-gradient(135deg, #0077cc, #0056b3);
            color: white;
            padding: 15px 10px;
            text-align: left;
            font-weight: 600;
            font-size: 0.9rem;
        }
        .users-table td {
            padding: 12px 10px;
            border-bottom: 1px solid #e9ecef;
            font-size: 0.85rem;
        }
        .users-table tr:hover {
            background-color: #f8f9fa;
        }
        .users-table tr:nth-child(even) {
            background-color: #f8f9fa;
        }
        .status-badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 600;
            text-transform: uppercase;
        }
        .status-approved {
            background: #d4edda;
            color: #155724;
        }
        .status-pending {
            background: #fff3cd;
            color: #856404;
        }
        .status-closed {
            background: #f8d7da;
            color: #721c24;
        }
        .balance-positive {
            color: #28a745;
            font-weight: 600;
        }
        .balance-zero {
            color: #6c757d;
        }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #e9ecef;
            text-align: center;
            color: #666;
            font-size: 12px;
        }
        .admin-info {
            background: #e3f2fd;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 20px;
            border-left: 4px solid #0077cc;
        }
        .admin-info h4 {
            margin: 0 0 10px 0;
            color: #0077cc;
            font-size: 1.1rem;
        }
        .admin-info p {
            margin: 5px 0;
            color: #333;
        }
        @media print {
            body { margin: 0; }
            .container { box-shadow: none; }
        }
    </style>
</head>
<body>
    <div class="watermark">NeoBank</div>
    <div class="watermark-logo">🏦</div>
    <div class="container">
        <div class="header">
            <div class="bank-logo">
                <span>🏦</span>
                <span>NeoBank</span>
            </div>
            <div class="bank-name">NeoBank India Limited</div>
            <div class="bank-tagline">Relationship beyond banking</div>
        </div>

        <div class="document-title">
            📊 COMPLETE USERS DETAILS REPORT
        </div>

        <div class="admin-info">
            <h4>📋 Report Information</h4>
            <p><strong>Generated By:</strong> ${adminName} (Administrator)</p>
            <p><strong>Generated On:</strong> ${currentDate} at ${currentTime}</p>
            <p><strong>Total Users:</strong> ${this.users.length}</p>
            <p><strong>Report Type:</strong> Complete User Details</p>
        </div>

        <div class="summary-stats">
            <div class="stat-card">
                <div class="stat-number">${this.users.length}</div>
                <div class="stat-label">Total Users</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${this.users.filter(u => u.status === 'APPROVED' || u.status === 'ACTIVE').length}</div>
                <div class="stat-label">Active Users</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${this.users.filter(u => u.status === 'PENDING').length}</div>
                <div class="stat-label">Pending Users</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">₹${this.users.reduce((sum, user) => sum + user.balance, 0).toLocaleString('en-IN')}</div>
                <div class="stat-label">Total Balance</div>
            </div>
        </div>

        <table class="users-table">
            <thead>
                <tr>
                    <th>Sr. No.</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Account Number</th>
                    <th>Phone</th>
                    <th>Balance</th>
                    <th>Status</th>
                    <th>Join Date</th>
                </tr>
            </thead>
            <tbody>
                ${this.users.map((user, index) => `
                <tr>
                    <td>${index + 1}</td>
                    <td><strong>${user.name}</strong></td>
                    <td>${user.email}</td>
                    <td>${user.accountNumber}</td>
                    <td>${user.phoneNumber || 'N/A'}</td>
                    <td class="${user.balance > 0 ? 'balance-positive' : 'balance-zero'}">₹${user.balance.toLocaleString('en-IN')}</td>
                    <td><span class="status-badge status-${user.status.toLowerCase()}">${user.status}</span></td>
                    <td>${user.joinDate || 'N/A'}</td>
                </tr>
                `).join('')}
            </tbody>
        </table>

        <div class="footer">
            <p><strong>🏦 NeoBank India Limited</strong></p>
            <p>📍 Registered Office: NeoBank Tower, Financial District, Mumbai - 400001</p>
            <p>📞 Customer Care: 1800-NEOBANK | 📧 Email: support@neobank.in</p>
            <p>🌐 Website: www.neobank.in</p>
            <p>📄 This is a computer generated report and does not require signature.</p>
            <p>© 2025 NeoBank. All rights reserved.</p>
            <p><strong>Generated on:</strong> ${currentDate} at ${currentTime} | <strong>By:</strong> ${adminName}</p>
        </div>
    </div>
</body>
</html>`;
  }

  // Account Tracking Methods
  loadAccountTrackings() {
    this.http.get(`${environment.apiBaseUrl}/api/tracking?page=0&size=100&sortBy=createdAt&sortDir=desc`)
      .pipe(
        timeout(10000),
        catchError(err => {
          console.error('Error loading account trackings:', err);
          return of({ content: [] });
        })
      )
      .subscribe({
      next: (response: any) => {
        console.log('Account trackings loaded:', response);
        if (response.content) {
          this.accountTrackings = response.content;
        } else if (Array.isArray(response)) {
          this.accountTrackings = response;
        }
      },
      error: (err: any) => {
        console.error('Error loading account trackings:', err);
        this.accountTrackings = [];
      }
    });
  }

  toggleTrackingSection() {
    this.showTrackingSection = !this.showTrackingSection;
    if (this.showTrackingSection && this.accountTrackings.length === 0) {
      this.loadAccountTrackings();
    }
  }

  get filteredTrackings(): AccountTracking[] {
    let filtered = this.accountTrackings;

    // Filter by status
    if (this.trackingStatusFilter !== 'ALL') {
      filtered = filtered.filter(t => t.status === this.trackingStatusFilter);
    }

    // Filter by search query
    if (this.trackingSearchQuery) {
      const query = this.trackingSearchQuery.toLowerCase();
      filtered = filtered.filter(t =>
        t.trackingId.toLowerCase().includes(query) ||
        t.aadharNumber.toLowerCase().includes(query) ||
        (t.mobileNumber && t.mobileNumber.toLowerCase().includes(query)) ||
        (t.user?.email && t.user.email.toLowerCase().includes(query))
      );
    }

    return filtered;
  }

  // Track by Aadhar and Mobile Number
  trackByAadharAndMobile() {
    const aadhar = prompt('Enter Aadhar Number:');
    const mobile = prompt('Enter Mobile Number:');
    
    if (!aadhar || !mobile) {
      this.alertService.userError('Invalid Input', 'Please provide both Aadhar number and mobile number.');
      return;
    }

    this.http.get(`${environment.apiBaseUrl}/api/tracking/track?aadharNumber=${aadhar}&mobileNumber=${mobile}`)
      .pipe(
        timeout(10000),
        catchError(err => {
          console.error('Error tracking account:', err);
          this.alertService.error('Error', err.error?.message || 'Failed to track account');
          return of(null);
        })
      )
      .subscribe({
      next: (response: any) => {
        if (response.success && response.tracking) {
          const tracking = response.tracking;
          this.alertService.userSuccess('Tracking Found', 
            `Tracking ID: ${tracking.trackingId}\nStatus: ${this.getStatusLabel(tracking.status)}\nAadhar: ${tracking.aadharNumber}\nMobile: ${tracking.mobileNumber}`);
          
          // Scroll to the tracking in the list if it exists
          const foundTracking = this.accountTrackings.find(t => t.id === tracking.id);
          if (foundTracking) {
            this.selectedTracking = foundTracking;
          }
        } else {
          this.alertService.userError('Not Found', 'No tracking record found with the provided details.');
        }
      },
      error: (err: any) => {
        console.error('Error tracking by Aadhar and Mobile:', err);
        this.alertService.userError('Error', err.error?.message || 'Failed to retrieve tracking information.');
      }
    });
  }

  updateTrackingStatus(tracking: AccountTracking, newStatus: string) {
    if (!tracking || !tracking.id) {
      this.alertService.userError('Update Failed', 'Invalid tracking record. Missing ID.');
      return;
    }

    if (!newStatus || newStatus.trim() === '') {
      this.alertService.userError('Update Failed', 'Status cannot be empty.');
      return;
    }

    if (!confirm(`Are you sure you want to update the status to ${newStatus}?`)) {
      return;
    }

    console.log('Updating tracking status:', {
      trackingId: tracking.id,
      trackingTrackingId: tracking.trackingId,
      newStatus: newStatus,
      currentStatus: tracking.status
    });

    // Ensure ID is a number
    const trackingId = typeof tracking.id === 'number' ? tracking.id : Number(tracking.id);
    if (isNaN(trackingId)) {
      this.alertService.error('Validation Error', 'Invalid tracking ID');
      return;
    }
    const url = `${environment.apiBaseUrl}/api/tracking/${trackingId}/status?status=${encodeURIComponent(newStatus)}&updatedBy=Admin`;
    console.log('API URL:', url);

    this.http.put(url, {}).subscribe({
      next: (response: any) => {
        console.log('Tracking status updated response:', response);
        if (response.success) {
          this.alertService.userSuccess('Status Updated', `Tracking status updated to ${newStatus}`);
          this.loadAccountTrackings();
        } else {
          this.alertService.userError('Update Failed', response.message || 'Failed to update status');
        }
      },
      error: (err: any) => {
        console.error('Error updating tracking status:', err);
        console.error('Error details:', {
          status: err.status,
          statusText: err.statusText,
          error: err.error,
          message: err.message
        });
        const errorMessage = err.error?.message || err.message || 'Failed to update tracking status';
        this.alertService.userError('Update Failed', errorMessage);
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'status-badge pending';
      case 'ADMIN_SEEN':
        return 'status-badge seen';
      case 'ADMIN_APPROVED':
        return 'status-badge approved';
      case 'ADMIN_SENT':
        return 'status-badge sent';
      default:
        return 'status-badge';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'Pending';
      case 'ADMIN_SEEN':
        return 'Admin Seen';
      case 'ADMIN_APPROVED':
        return 'Approved';
      case 'ADMIN_SENT':
        return 'Sent';
      default:
        return status;
    }
  }

  // Tracking statistics getters
  get totalTrackings(): number {
    return this.accountTrackings.length;
  }

  get pendingTrackings(): number {
    return this.accountTrackings.filter(t => t.status === 'PENDING').length;
  }

  get seenTrackings(): number {
    return this.accountTrackings.filter(t => t.status === 'ADMIN_SEEN').length;
  }

  get approvedTrackings(): number {
    return this.accountTrackings.filter(t => t.status === 'ADMIN_APPROVED').length;
  }

  get sentTrackings(): number {
    return this.accountTrackings.filter(t => t.status === 'ADMIN_SENT').length;
  }

  // Gold Rate Management Methods
  toggleGoldRateSection() {
    this.showGoldRateSection = !this.showGoldRateSection;
    if (this.showGoldRateSection) {
      this.loadCurrentGoldRate();
      this.loadGoldRateHistory();
    }
  }

  loadCurrentGoldRate() {
    this.isLoadingGoldRate = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/gold-rates/current`).subscribe({
      next: (rate) => {
        this.currentGoldRate = rate;
        this.newGoldRate = rate.ratePerGram || 0;
        this.isLoadingGoldRate = false;
      },
      error: (err) => {
        console.error('Error loading gold rate:', err);
        this.isLoadingGoldRate = false;
        this.alertService.error('Error', 'Failed to load current gold rate');
      }
    });
  }

  loadGoldRateHistory() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/gold-rates/history`).subscribe({
      next: (history) => {
        this.goldRateHistory = history;
      },
      error: (err) => {
        console.error('Error loading gold rate history:', err);
        this.alertService.error('Error', 'Failed to load gold rate history');
      }
    });
  }

  updateGoldRate() {
    if (!this.newGoldRate || this.newGoldRate <= 0) {
      this.alertService.error('Error', 'Please enter a valid gold rate');
      return;
    }

    if (this.newGoldRate === this.currentGoldRate?.ratePerGram) {
      this.alertService.error('Error', 'New rate must be different from current rate');
      return;
    }

    this.isLoadingGoldRate = true;
    const params = new HttpParams()
      .set('ratePerGram', this.newGoldRate.toString())
      .set('updatedBy', this.adminName);
    
    this.http.put<any>(`${environment.apiBaseUrl}/api/gold-rates/update`, {}, { params }).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Success', 'Gold rate updated successfully');
          this.loadCurrentGoldRate();
          this.loadGoldRateHistory();
        } else {
          this.alertService.error('Error', response.message || 'Failed to update gold rate');
          this.isLoadingGoldRate = false;
        }
      },
      error: (err) => {
        console.error('Error updating gold rate:', err);
        this.alertService.error('Error', 'Failed to update gold rate. Please try again.');
        this.isLoadingGoldRate = false;
      }
    });
  }

  formatDateTime(dateTime: string): string {
    if (!dateTime) return '';
    const date = new Date(dateTime);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatDate(date: string): string {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  // Gold Loan Management Methods
  navigateToGoldLoans() {
    this.router.navigate(['/admin/gold-loans']);
  }

  toggleGoldLoanSection() {
    this.showGoldLoanSection = !this.showGoldLoanSection;
    if (this.showGoldLoanSection) {
      this.loadGoldLoans();
    }
  }

  loadGoldLoans() {
    this.isLoadingGoldLoans = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/gold-loans`)
      .pipe(
        timeout(10000),
        catchError(err => {
          console.error('Error loading gold loans:', err);
          let errorMessage = 'Failed to load gold loans. ';
          if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
            errorMessage += 'Request timed out. The server may be slow.';
          } else if (err.status === 0) {
            errorMessage += 'Unable to connect to server. Please check if the backend is running.';
          } else if (err.status >= 500) {
            errorMessage += 'Server error occurred. Please try again later.';
          }
          this.alertService.error('Error', errorMessage);
          this.isLoadingGoldLoans = false;
          return of([]);
        })
      )
      .subscribe({
        next: (loans) => {
          this.goldLoans = loans || [];
          this.filterGoldLoans();
          this.isLoadingGoldLoans = false;
        }
      });
  }

  filterGoldLoans() {
    if (this.goldLoanStatusFilter === 'All') {
      this.filteredGoldLoans = this.goldLoans;
    } else {
      this.filteredGoldLoans = this.goldLoans.filter(loan => loan.status === this.goldLoanStatusFilter);
    }
  }

  onGoldLoanFilterChange() {
    this.filterGoldLoans();
  }

  viewGoldLoanDetails(loan: any) {
    this.selectedGoldLoan = loan;
    this.goldDetailsForm.verifiedGoldGrams = loan.goldGrams || 0;
    this.goldDetailsForm.goldPurity = loan.goldPurity || '22K';
    this.showGoldLoanModal = true;
  }

  closeGoldLoanModal() {
    this.showGoldLoanModal = false;
    this.selectedGoldLoan = null;
    this.goldDetailsForm = {
      goldItems: '',
      goldDescription: '',
      goldPurity: '22K',
      verifiedGoldGrams: 0,
      verificationNotes: '',
      storageLocation: ''
    };
  }

  approveGoldLoan() {
    if (!this.selectedGoldLoan || !this.selectedGoldLoan.id) return;

    if (!this.goldDetailsForm.verifiedGoldGrams || this.goldDetailsForm.verifiedGoldGrams <= 0) {
      this.alertService.error('Error', 'Please enter verified gold weight');
      return;
    }

    this.approvingLoan = true;
    // Ensure ID is a number
    const loanId = typeof this.selectedGoldLoan.id === 'number' ? this.selectedGoldLoan.id : Number(this.selectedGoldLoan.id);
    if (isNaN(loanId)) {
      this.alertService.error('Validation Error', 'Invalid gold loan ID');
      this.approvingLoan = false;
      return;
    }
    this.http.put<any>(`${environment.apiBaseUrl}/api/gold-loans/${loanId}/approve`, this.goldDetailsForm, {
      params: {
        status: 'Approved',
        approvedBy: this.adminName
      }
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Success', 'Gold loan approved successfully');
          this.closeGoldLoanModal();
          this.loadGoldLoans();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve loan');
          this.approvingLoan = false;
        }
      },
      error: (err) => {
        console.error('Error approving gold loan:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve loan');
        this.approvingLoan = false;
      }
    });
  }

  rejectGoldLoan(loan: any) {
    if (!confirm(`Are you sure you want to reject this gold loan application?`)) return;

    this.approvingLoan = true;
    // Ensure ID is a number
    const loanId = typeof loan.id === 'number' ? loan.id : Number(loan.id);
    if (isNaN(loanId)) {
      this.alertService.error('Validation Error', 'Invalid gold loan ID');
      this.approvingLoan = false;
      return;
    }
    this.http.put<any>(`${environment.apiBaseUrl}/api/gold-loans/${loanId}/approve`, {}, {
      params: {
        status: 'Rejected',
        approvedBy: this.adminName
      }
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Success', 'Gold loan rejected');
          this.loadGoldLoans();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject loan');
        }
        this.approvingLoan = false;
      },
      error: (err) => {
        console.error('Error rejecting gold loan:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject loan');
        this.approvingLoan = false;
      }
    });
  }

  viewForeclosure(loan: any) {
    this.selectedGoldLoan = loan;
    this.showForeclosureModal = true;
    this.loadForeclosureDetails(loan);
  }

  loadForeclosureDetails(loan: any) {
    if (!loan.loanAccountNumber) {
      this.alertService.error('Error', 'Loan account number not found');
      return;
    }

    this.http.get<any>(`${environment.apiBaseUrl}/api/gold-loans/foreclosure/${loan.loanAccountNumber}`)
      .pipe(
        timeout(10000),
        catchError(err => {
          console.error('Error loading foreclosure details:', err);
          let errorMessage = err.error?.message || 'Failed to load foreclosure details. ';
          if (err.name === 'TimeoutError') {
            errorMessage = 'Request timed out. Please try again.';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server.';
          }
          this.alertService.error('Error', errorMessage);
          return of({ success: false, message: errorMessage });
        })
      )
      .subscribe({
        next: (response) => {
          if (response && response.success) {
            this.foreclosureDetails = response;
          } else if (response) {
            this.alertService.error('Error', response.message || 'Failed to load foreclosure details');
          }
        }
      });
  }

  processForeclosure() {
    if (!this.selectedGoldLoan || !this.selectedGoldLoan.loanAccountNumber) return;
    if (!confirm(`Are you sure you want to process foreclosure for this loan?`)) return;

    this.processingForeclosure = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/gold-loans/foreclosure/${this.selectedGoldLoan.loanAccountNumber}`, {})
      .pipe(
        timeout(15000),
        catchError(err => {
          console.error('Error processing foreclosure:', err);
          let errorMessage = err.error?.message || 'Failed to process foreclosure. ';
          if (err.name === 'TimeoutError') {
            errorMessage = 'Request timed out. Please try again.';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server.';
          }
          this.alertService.error('Error', errorMessage);
          this.processingForeclosure = false;
          return of({ success: false, message: errorMessage });
        })
      )
      .subscribe({
        next: (response) => {
          if (response && response.success) {
            this.alertService.success('Success', 'Foreclosure processed successfully');
            this.closeForeclosureModal();
            this.loadGoldLoans();
          } else if (response) {
            this.alertService.error('Error', response.message || 'Failed to process foreclosure');
          }
          this.processingForeclosure = false;
        }
      });
  }

  closeForeclosureModal() {
    this.showForeclosureModal = false;
    this.selectedGoldLoan = null;
    this.foreclosureDetails = null;
  }

  viewEmiDetails(loan: any) {
    this.selectedGoldLoan = loan;
    this.showEmiDetailsModal = true;
    this.loadEmiSchedule(loan);
  }

  loadEmiSchedule(loan: any) {
    if (!loan.loanAccountNumber) {
      this.alertService.error('Error', 'Loan account number not found');
      return;
    }

    this.http.get<any[]>(`${environment.apiBaseUrl}/api/gold-loans/emi-schedule/${loan.loanAccountNumber}`)
      .pipe(
        timeout(10000),
        catchError(err => {
          console.error('Error loading EMI schedule:', err);
          let errorMessage = err.error?.message || 'Failed to load EMI schedule. ';
          if (err.name === 'TimeoutError') {
            errorMessage = 'Request timed out. Please try again.';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server.';
          }
          this.alertService.error('Error', errorMessage);
          return of([]);
        })
      )
      .subscribe({
        next: (schedule) => {
          this.emiSchedule = Array.isArray(schedule) ? schedule : [];
        }
      });
  }

  closeEmiDetailsModal() {
    this.showEmiDetailsModal = false;
    this.selectedGoldLoan = null;
    this.emiSchedule = [];
  }

  isUpcomingEmi(dueDate: string): boolean {
    if (!dueDate) return false;
    const due = new Date(dueDate);
    const today = new Date();
    const daysUntilDue = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    return daysUntilDue >= 0 && daysUntilDue <= 7;
  }

  getTimeUntilDue(dueDate: string): string {
    if (!dueDate) return '';
    const due = new Date(dueDate);
    const today = new Date();
    const daysUntilDue = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    
    if (daysUntilDue < 0) {
      return `Overdue by ${Math.abs(daysUntilDue)} day${Math.abs(daysUntilDue) !== 1 ? 's' : ''}`;
    } else if (daysUntilDue === 0) {
      return 'Due today';
    } else if (daysUntilDue === 1) {
      return 'Due tomorrow';
    } else if (daysUntilDue <= 7) {
      return `Due in ${daysUntilDue} days`;
    } else {
      const monthsUntilDue = Math.floor(daysUntilDue / 30);
      if (monthsUntilDue > 0) {
        return `Due in ${monthsUntilDue} month${monthsUntilDue !== 1 ? 's' : ''}`;
      }
      return `Due in ${daysUntilDue} days`;
    }
  }

  getGoldLoanStatusClass(status: string): string {
    switch (status) {
      case 'Approved': return 'status-approved';
      case 'Pending': return 'status-pending';
      case 'Rejected': return 'status-rejected';
      case 'Foreclosed': return 'status-foreclosed';
      case 'Paid': return 'status-paid';
      default: return '';
    }
  }

  // Aadhar Verification Methods
  toggleAadharVerificationSection() {
    this.showAadharVerificationSection = !this.showAadharVerificationSection;
    if (this.showAadharVerificationSection && this.accountTrackings.length === 0) {
      this.loadAccountTrackings();
    }
  }

  loadPendingAadharVerifications() {
    this.http.get<AccountTracking[]>(`${environment.apiBaseUrl}/tracking/status/PENDING`).subscribe({
      next: (response) => {
        console.log('Pending Aadhar verifications loaded:', response);
        // Filter to show only PENDING and ADMIN_SEEN statuses
        this.accountTrackings = response.filter(t => 
          t.status === 'PENDING' || t.status === 'ADMIN_SEEN'
        );
      },
      error: (err) => {
        console.error('Error loading pending Aadhar verifications:', err);
        this.alertService.userError('Error', 'Failed to load pending verifications');
        this.accountTrackings = [];
      }
    });
  }

  verifyAadhar(tracking: AccountTracking) {
    if (!confirm(`Are you sure you want to verify Aadhar for ${tracking.aadharNumber}?`)) {
      return;
    }

    this.verifyingAadhar = tracking.id;
    // Ensure ID is a number
    const trackingId = typeof tracking.id === 'number' ? tracking.id : Number(tracking.id);
    if (isNaN(trackingId)) {
      this.alertService.error('Validation Error', 'Invalid tracking ID');
      this.verifyingAadhar = null;
      return;
    }
    this.http.post(`${environment.apiBaseUrl}/api/tracking/${trackingId}/verify-aadhar`, {}).subscribe({
      next: (response: any) => {
        console.log('Aadhar verified successfully:', response);
        this.alertService.userSuccess('Success', 'Aadhar verified successfully!');
        this.verifyingAadhar = null;
        // Reload the list
        this.loadPendingAadharVerifications();
        this.loadAccountTrackings();
      },
      error: (err) => {
        console.error('Error verifying Aadhar:', err);
        this.alertService.userError('Error', err.error?.message || 'Failed to verify Aadhar');
        this.verifyingAadhar = null;
      }
    });
  }

  get filteredAadharVerifications(): AccountTracking[] {
    let filtered = this.accountTrackings.filter(t => 
      t.status === 'PENDING' || t.status === 'ADMIN_SEEN'
    );

    if (this.aadharSearchQuery.trim()) {
      const query = this.aadharSearchQuery.toLowerCase().trim();
      filtered = filtered.filter(t =>
        t.trackingId.toLowerCase().includes(query) ||
        t.aadharNumber.toLowerCase().includes(query) ||
        t.mobileNumber.toLowerCase().includes(query) ||
        (t.user?.email && t.user.email.toLowerCase().includes(query))
      );
    }

    return filtered;
  }

  get pendingAadharVerifications(): number {
    return this.accountTrackings.filter(t => 
      t.status === 'PENDING' || t.status === 'ADMIN_SEEN'
    ).length;
  }

  get totalAadharVerified(): number {
    return this.accountTrackings.filter(t => t.status === 'ADMIN_APPROVED').length;
  }

  // Load User Login History
  loadLoginHistory() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.isLoadingLoginHistory = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admins/login-history/recent?limit=100`).subscribe({
      next: (history) => {
        this.loginHistory = history || [];
        this.isLoadingLoginHistory = false;
      },
      error: (err) => {
        console.error('Error loading login history:', err);
        this.isLoadingLoginHistory = false;
        this.loginHistory = [];
      }
    });
  }

  // Toggle login history section
  toggleLoginHistorySection() {
    this.showLoginHistorySection = !this.showLoginHistorySection;
    if (this.showLoginHistorySection && this.loginHistory.length === 0) {
      this.loadLoginHistory();
    }
  }

  // Format date time for display
  formatLoginDateTime(dateTimeString: string): string {
    if (!dateTimeString) return '-';
    try {
      const date = new Date(dateTimeString);
      if (isNaN(date.getTime())) return dateTimeString;
      
      return date.toLocaleString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
      });
    } catch (e) {
      return dateTimeString;
    }
  }

  // Get filtered login history
  // Load all transactions including FD, Investment, Subsidy related
  loadAllTransactions() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.isLoadingTransactions = true;
    this.allTransactions = [];
    
    // Load regular transactions
    this.http.get(`${environment.apiBaseUrl}/api/transactions?page=0&size=1000`).subscribe({
      next: (response: any) => {
        // Handle both Page response and direct array response
        let transactionsData = [];
        if (response && response.content && Array.isArray(response.content)) {
          transactionsData = response.content;
        } else if (Array.isArray(response)) {
          transactionsData = response;
        }
        
        // Map transactions and get user names
        transactionsData.forEach((transaction: any) => {
          const user = this.users.find(u => u.accountNumber === transaction.accountNumber);
          this.allTransactions.push({
            id: transaction.id,
            accountNumber: transaction.accountNumber,
            userName: user ? user.name : 'Unknown User',
            userPAN: user ? user.pan : null,
            type: transaction.type,
            amount: transaction.amount,
            balance: transaction.balance,
            date: transaction.date || transaction.createdAt,
            description: transaction.description || transaction.merchant,
            merchant: transaction.merchant,
            status: transaction.status || 'Completed',
            category: this.determineTransactionCategory(transaction)
          });
        });
        
        // Load FD related transactions
        this.loadFDTransactions();
        // Load Investment transactions
        this.loadInvestmentTransactions();
        // Load Subsidy transactions
        this.loadSubsidyTransactions();
      },
      error: (err: any) => {
        console.error('Error loading transactions:', err);
        this.loadFDTransactions();
        this.loadInvestmentTransactions();
        this.loadSubsidyTransactions();
      }
    });
  }

  // Load FD related transactions
  loadFDTransactions() {
    this.http.get(`${environment.apiBaseUrl}/api/fixed-deposits/all`).subscribe({
      next: (fds: any) => {
        if (Array.isArray(fds)) {
          fds.forEach((fd: any) => {
            const user = this.users.find(u => u.accountNumber === fd.accountNumber);
            // FD Creation transaction
            if (fd.transactionId) {
              this.allTransactions.push({
                id: 'FD-' + fd.id,
                accountNumber: fd.accountNumber,
                userName: user ? user.name : fd.userName || 'Unknown User',
                userPAN: user ? user.pan : null,
                type: 'Debit',
                amount: fd.principalAmount,
                balance: fd.balanceBefore ? fd.balanceBefore - fd.principalAmount : 0,
                date: fd.applicationDate || new Date().toISOString(),
                description: `FD Created - ${fd.fdAccountNumber} | Principal: ₹${fd.principalAmount}`,
                merchant: 'Fixed Deposit',
                status: fd.status === 'APPROVED' ? 'Completed' : 'Pending',
                category: 'FD'
              });
            }
            // FD Maturity transaction
            if (fd.isMatured && fd.maturityAmount) {
              this.allTransactions.push({
                id: 'FD-MAT-' + fd.id,
                accountNumber: fd.accountNumber,
                userName: user ? user.name : fd.userName || 'Unknown User',
                userPAN: user ? user.pan : null,
                type: 'Credit',
                amount: fd.maturityAmount,
                balance: 0,
                date: fd.maturityDate || new Date().toISOString(),
                description: `FD Matured - ${fd.fdAccountNumber} | Maturity: ₹${fd.maturityAmount}`,
                merchant: 'Fixed Deposit Maturity',
                status: 'Completed',
                category: 'FD'
              });
            }
          });
        }
        this.finalizeTransactionLoading();
      },
      error: (err: any) => {
        console.error('Error loading FD transactions:', err);
        this.finalizeTransactionLoading();
      }
    });
  }

  // Load Investment/Mutual Fund transactions
  loadInvestmentTransactions() {
    this.http.get(`${environment.apiBaseUrl}/api/investments/all`).subscribe({
      next: (investments: any) => {
        if (Array.isArray(investments)) {
          investments.forEach((inv: any) => {
            const user = this.users.find(u => u.accountNumber === inv.accountNumber);
            if (inv.transactionId) {
              this.allTransactions.push({
                id: 'INV-' + inv.id,
                accountNumber: inv.accountNumber,
                userName: user ? user.name : inv.userName || 'Unknown User',
                userPAN: user ? user.pan : null,
                type: 'Debit',
                amount: inv.investmentAmount,
                balance: inv.balanceBefore ? inv.balanceBefore - inv.investmentAmount : 0,
                date: inv.investmentDate || inv.applicationDate || new Date().toISOString(),
                description: `${inv.investmentType} - ${inv.fundName || 'N/A'} | Amount: ₹${inv.investmentAmount}`,
                merchant: inv.investmentType === 'Mutual Fund' ? 'Mutual Fund' : 'Investment',
                status: inv.status === 'APPROVED' ? 'Completed' : 'Pending',
                category: inv.investmentType === 'Mutual Fund' ? 'MUTUAL_FUND' : 'INVESTMENT'
              });
            }
          });
        }
        this.finalizeTransactionLoading();
      },
      error: (err: any) => {
        console.error('Error loading investment transactions:', err);
        this.finalizeTransactionLoading();
      }
    });
  }

  // Load Subsidy transactions
  loadSubsidyTransactions() {
    this.http.get(`${environment.apiBaseUrl}/api/subsidy-claims/all`).subscribe({
      next: (subsidies: any) => {
        if (Array.isArray(subsidies)) {
          subsidies.forEach((sub: any) => {
            const user = this.users.find(u => u.accountNumber === sub.accountNumber);
            if (sub.status === 'APPROVED' && sub.subsidyAmount) {
              this.allTransactions.push({
                id: 'SUB-' + sub.id,
                accountNumber: sub.accountNumber,
                userName: user ? user.name : sub.userName || 'Unknown User',
                userPAN: user ? user.pan : null,
                type: 'Credit',
                amount: sub.subsidyAmount,
                balance: 0,
                date: sub.approvalDate || sub.applicationDate || new Date().toISOString(),
                description: `Subsidy Approved - ${sub.loanType || 'Education Loan'} | Amount: ₹${sub.subsidyAmount}`,
                merchant: 'Subsidy Claim',
                status: 'Completed',
                category: 'SUBSIDY'
              });
            }
          });
        }
        this.finalizeTransactionLoading();
      },
      error: (err: any) => {
        console.error('Error loading subsidy transactions:', err);
        this.finalizeTransactionLoading();
      }
    });
  }

  // Determine transaction category based on description/merchant
  determineTransactionCategory(transaction: any): string {
    const desc = (transaction.description || transaction.merchant || '').toLowerCase();
    if (desc.includes('loan') || desc.includes('loan credit') || desc.includes('emi')) {
      return 'LOAN';
    } else if (desc.includes('subsidy')) {
      return 'SUBSIDY';
    } else if (desc.includes('fd') || desc.includes('fixed deposit')) {
      return 'FD';
    } else if (desc.includes('mutual fund')) {
      return 'MUTUAL_FUND';
    } else if (desc.includes('investment') || desc.includes('sip')) {
      return 'INVESTMENT';
    }
    return 'REGULAR';
  }

  // Finalize transaction loading
  finalizeTransactionLoading() {
    // Remove duplicates based on ID
    const uniqueTransactions = Array.from(
      new Map(this.allTransactions.map(t => [t.id, t])).values()
    );
    
    // Sort by date (newest first)
    uniqueTransactions.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    
    this.allTransactions = uniqueTransactions;
    this.isLoadingTransactions = false;
    console.log('All transactions loaded:', this.allTransactions.length);
  }

  // Filter by user
  filterByUser(accountNumber: string) {
    this.selectedUserForTransactions = accountNumber;
    this.transactionsSearchQuery = '';
    this.transactionsPANFilter = '';
    this.transactionsDateFilter = '';
    // If transactions section is active, reload to show filtered results
    if (this.activeSection === 'transactions') {
      this.loadAllTransactions();
    }
  }

  // Filter by newly created account (called when new account is created)
  filterByNewAccount(accountNumber: string) {
    // Set active section to transactions if not already
    if (this.activeSection !== 'transactions') {
      this.setActiveSection('transactions');
    }
    // Filter by the new account
    this.filterByUser(accountNumber);
    // Show message
    this.alertService.success('Filter Applied', `Showing transactions for newly created account: ${accountNumber}`);
  }

  // Clear all transaction filters
  clearTransactionFilters() {
    this.selectedUserForTransactions = null;
    this.transactionsSearchQuery = '';
    this.transactionsPANFilter = '';
    this.transactionsDateFilter = '';
    this.transactionsTypeFilter = 'ALL';
    this.transactionsCategoryFilter = 'ALL';
  }

  // Handle transaction search change
  onTransactionSearchChange() {
    // This method is called when filters change, filtering happens in getFilteredTransactions
  }

  // Get filtered transactions
  getFilteredTransactions(): any[] {
    let filtered = this.allTransactions;
    
    // Filter by selected user
    if (this.selectedUserForTransactions) {
      filtered = filtered.filter(t => t.accountNumber === this.selectedUserForTransactions);
    }
    
    // Filter by PAN number
    if (this.transactionsPANFilter && this.transactionsPANFilter.trim() !== '') {
      const panQuery = this.transactionsPANFilter.trim().toUpperCase();
      filtered = filtered.filter(t => {
        if (t.userPAN) {
          return t.userPAN.toUpperCase().includes(panQuery);
        }
        // Also check if PAN is in user data
        const user = this.users.find(u => u.accountNumber === t.accountNumber);
        return user && user.pan && user.pan.toUpperCase().includes(panQuery);
      });
    }
    
    // Filter by date
    if (this.transactionsDateFilter && this.transactionsDateFilter.trim() !== '') {
      const filterDate = new Date(this.transactionsDateFilter);
      filterDate.setHours(0, 0, 0, 0);
      const nextDay = new Date(filterDate);
      nextDay.setDate(nextDay.getDate() + 1);
      
      filtered = filtered.filter(t => {
        if (!t.date) return false;
        const txnDate = new Date(t.date);
        txnDate.setHours(0, 0, 0, 0);
        return txnDate >= filterDate && txnDate < nextDay;
      });
    }
    
    // Filter by type
    if (this.transactionsTypeFilter !== 'ALL') {
      filtered = filtered.filter(t => t.type === this.transactionsTypeFilter);
    }
    
    // Filter by category
    if (this.transactionsCategoryFilter !== 'ALL') {
      filtered = filtered.filter(t => {
        if (!t.category) return false;
        return t.category === this.transactionsCategoryFilter;
      });
    }
    
    // Filter by search query (account number, name, description, loan number, FD number, etc.)
    if (this.transactionsSearchQuery && this.transactionsSearchQuery.trim() !== '') {
      const query = this.transactionsSearchQuery.toLowerCase();
      filtered = filtered.filter(t => {
        // Check account number
        if (t.accountNumber && t.accountNumber.toLowerCase().includes(query)) return true;
        // Check user name
        if (t.userName && t.userName.toLowerCase().includes(query)) return true;
        // Check description
        if (t.description && t.description.toLowerCase().includes(query)) return true;
        // Check merchant
        if (t.merchant && t.merchant.toLowerCase().includes(query)) return true;
        // Check for loan number in description
        if (t.description && t.description.toLowerCase().includes('loan') && query.includes('loan')) return true;
        // Check for FD number in description
        if (t.description && (t.description.toLowerCase().includes('fd') || t.description.toLowerCase().includes('fixed deposit')) && 
            (query.includes('fd') || query.includes('fixed'))) return true;
        // Check for subsidy in description
        if (t.description && t.description.toLowerCase().includes('subsidy') && query.includes('subsidy')) return true;
        // Check for investment/mutual fund in description
        if (t.description && (t.description.toLowerCase().includes('investment') || t.description.toLowerCase().includes('mutual fund')) && 
            (query.includes('investment') || query.includes('mutual'))) return true;
        return false;
      });
    }
    
    return filtered;
  }

  getFilteredLoginHistory(): any[] {
    let filtered = this.loginHistory;
    
    // Filter by status
    if (this.loginHistoryFilter !== 'ALL') {
      filtered = filtered.filter(h => h.status === this.loginHistoryFilter);
    }
    
    // Filter by search query
    if (this.loginHistorySearchQuery && this.loginHistorySearchQuery.trim() !== '') {
      const query = this.loginHistorySearchQuery.toLowerCase();
      filtered = filtered.filter(h => 
        (h.userName && h.userName.toLowerCase().includes(query)) ||
        (h.userEmail && h.userEmail.toLowerCase().includes(query)) ||
        (h.accountNumber && h.accountNumber.toLowerCase().includes(query)) ||
        (h.ipAddress && h.ipAddress.toLowerCase().includes(query)) ||
        (h.loginLocation && h.loginLocation.toLowerCase().includes(query))
      );
    }
    
    return filtered;
  }
  
  // Load Loan Prediction History
  loadLoanPredictions() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.isLoadingPredictions = true;
    
    this.http.get(`${environment.apiBaseUrl}/api/loans/predictions/all`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.loanPredictions = response.predictions || [];
          console.log('Loan predictions loaded:', this.loanPredictions);
        } else {
          this.alertService.error('Error', response.message || 'Failed to load predictions');
          this.loanPredictions = [];
        }
        this.isLoadingPredictions = false;
      },
      error: (err: any) => {
        console.error('Error loading loan predictions:', err);
        this.alertService.error('Error', 'Failed to load loan predictions');
        this.loanPredictions = [];
        this.isLoadingPredictions = false;
      }
    });
  }
  
  // Filter predictions
  getFilteredPredictions() {
    let filtered = this.loanPredictions;
    
    // Filter by result
    if (this.predictionFilter !== 'ALL') {
      filtered = filtered.filter((p: any) => 
        p.predictionResult === this.predictionFilter
      );
    }
    
    // Filter by loan type
    if (this.predictionLoanTypeFilter !== 'ALL') {
      filtered = filtered.filter((p: any) => 
        p.loanType === this.predictionLoanTypeFilter
      );
    }
    
    // Filter by search query
    if (this.predictionSearchQuery && this.predictionSearchQuery.trim() !== '') {
      const query = this.predictionSearchQuery.toLowerCase();
      filtered = filtered.filter((p: any) => 
        (p.userName && p.userName.toLowerCase().includes(query)) ||
        (p.accountNumber && p.accountNumber.toLowerCase().includes(query)) ||
        (p.pan && p.pan.toLowerCase().includes(query)) ||
        (p.loanType && p.loanType.toLowerCase().includes(query))
      );
    }
    
    return filtered;
  }
  
  // Get prediction statistics
  getPredictionStats() {
    const all = this.loanPredictions.length;
    const approved = this.loanPredictions.filter((p: any) => p.predictionResult === 'Approved').length;
    const rejected = this.loanPredictions.filter((p: any) => p.predictionResult === 'Rejected').length;
    const pending = this.loanPredictions.filter((p: any) => p.predictionResult === 'Pending Review').length;
    
    return { all, approved, rejected, pending };
  }
  
  // Expose Math to template
  Math = Math;
  
  // Helper methods for date comparisons in templates
  isFDMatured(fd: any): boolean {
    if (!fd || !fd.maturityDate || fd.isMatured) return false;
    const maturityDate = new Date(fd.maturityDate);
    const today = new Date();
    return maturityDate <= today;
  }
  
  isEMIOverdue(emi: any): boolean {
    if (!emi || emi.status !== 'Pending' || !emi.dueDate) return false;
    const dueDate = new Date(emi.dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    dueDate.setHours(0, 0, 0, 0);
    return dueDate < today;
  }

  // Dashboard Overview Helper Methods
  getTotalBalance(): number {
    return this.users.reduce((total, user) => total + (user.balance || 0), 0);
  }

  getPendingLoansCount(): number {
    // Count pending loans from loan predictions or loan requests
    if (this.loanPredictions && this.loanPredictions.length > 0) {
      return this.loanPredictions.filter((p: any) => 
        p.predictionResult === 'Pending Review' || 
        (p.status && p.status === 'PENDING')
      ).length;
    }
    return 0;
  }

  getActiveFDsCount(): number {
    return this.fixedDeposits ? this.fixedDeposits.filter((fd: any) => fd.status === 'ACTIVE').length : 0;
  }

  getPendingChequesCount(): number {
    // This would need to be implemented based on your cheque data structure
    return 0; // Placeholder - implement based on your cheque management
  }

  // Credit Card Management Methods
  loadCreditCards() {
    this.isLoadingCreditCards = true;
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/all`).subscribe({
      next: (cards: any) => {
        this.creditCards = Array.isArray(cards) ? cards : [];
        this.isLoadingCreditCards = false;
      },
      error: (err: any) => {
        console.error('Error loading credit cards:', err);
        this.creditCards = [];
        this.isLoadingCreditCards = false;
      }
    });
  }

  viewCreditCardDetails(card: any) {
    this.selectedCreditCard = card;
    this.loadCreditCardTransactions(card.id);
    this.loadCreditCardBills(card.id);
    // Reset limit change form
    this.limitChangeAmount = 0;
    this.limitChangeReason = '';
  }

  loadCreditCardTransactions(cardId: number) {
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${cardId}/transactions`).subscribe({
      next: (transactions: any) => {
        this.creditCardTransactions = Array.isArray(transactions) ? transactions : [];
      },
      error: (err: any) => {
        console.error('Error loading transactions:', err);
        this.creditCardTransactions = [];
      }
    });
  }

  loadCreditCardBills(cardId: number) {
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${cardId}/bills`).subscribe({
      next: (bills: any) => {
        this.creditCardBills = Array.isArray(bills) ? bills : [];
      },
      error: (err: any) => {
        console.error('Error loading bills:', err);
        this.creditCardBills = [];
      }
    });
  }

  generateBill(cardId: number) {
    if (confirm('Generate bill for this credit card?')) {
      this.http.post(`${environment.apiBaseUrl}/api/credit-cards/${cardId}/generate-bill`, {}).subscribe({
        next: (bill: any) => {
          alert('Bill generated successfully');
          this.loadCreditCardBills(cardId);
        },
        error: (err: any) => {
          console.error('Error generating bill:', err);
          alert('Failed to generate bill');
        }
      });
    }
  }

  getStatement(cardId: number) {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    // Format dates properly for backend
    const startDateStr = startDate.toISOString().split('T')[0] + 'T00:00:00';
    const endDateStr = endDate.toISOString().split('T')[0] + 'T23:59:59';
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${cardId}/statement?startDate=${startDateStr}&endDate=${endDateStr}`).subscribe({
      next: (transactions: any) => {
        // Display statement - you can create a modal or download as PDF
        console.log('Statement:', transactions);
        alert(`Statement generated with ${transactions.length} transactions`);
      },
      error: (err: any) => {
        console.error('Error getting statement:', err);
        alert('Failed to get statement');
      }
    });
  }

  closeCreditCard(cardId: number) {
    if (confirm('Are you sure you want to close this credit card? This action cannot be undone.')) {
      this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${cardId}/close`, {}).subscribe({
        next: () => {
          alert('Credit card closed successfully');
          this.loadCreditCards();
          this.selectedCreditCard = null;
        },
        error: (err: any) => {
          console.error('Error closing credit card:', err);
          const errorMsg = err.error?.message || 'Failed to close credit card';
          alert(errorMsg);
        }
      });
    }
  }

  filterCreditCards() {
    // Filter logic will be handled in template
  }

  updateCreditLimit() {
    if (!this.selectedCreditCard || !this.limitChangeAmount || this.limitChangeAmount <= 0) {
      alert('Please enter a valid amount');
      return;
    }

    if (this.limitAction === 'decrease' && this.limitChangeAmount > this.selectedCreditCard.approvedLimit) {
      alert('Decrease amount cannot be greater than current limit');
      return;
    }

    if (this.limitAction === 'decrease' && this.selectedCreditCard.currentBalance > (this.selectedCreditCard.approvedLimit - this.limitChangeAmount)) {
      alert('Cannot decrease limit below current outstanding balance');
      return;
    }

    if (!this.limitChangeReason || this.limitChangeReason.trim() === '') {
      alert('Please provide a reason for the limit change');
      return;
    }

    const newLimit = this.limitAction === 'increase' 
      ? this.selectedCreditCard.approvedLimit + this.limitChangeAmount
      : this.selectedCreditCard.approvedLimit - this.limitChangeAmount;

    const updateData = {
      approvedLimit: newLimit,
      limitChangeReason: this.limitChangeReason,
      limitChangeAction: this.limitAction,
      limitChangeAmount: this.limitChangeAmount
    };

    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCreditCard.id}`, updateData).subscribe({
      next: (updatedCard: any) => {
        const oldLimit = this.selectedCreditCard.approvedLimit;
        alert(`Credit limit ${this.limitAction === 'increase' ? 'increased' : 'decreased'} successfully from ₹${oldLimit.toFixed(2)} to ₹${newLimit.toFixed(2)}`);
        this.showLimitModal = false;
        this.limitChangeAmount = 0;
        this.limitChangeReason = '';
        // Reload credit cards and update selected card
        this.loadCreditCards();
        // Update selected card with new data
        setTimeout(() => {
          if (this.selectedCreditCard) {
            this.selectedCreditCard.approvedLimit = updatedCard.approvedLimit;
            this.selectedCreditCard.availableLimit = updatedCard.availableLimit;
            this.selectedCreditCard.usageLimit = updatedCard.usageLimit;
            this.loadCreditCardTransactions(this.selectedCreditCard.id);
            this.loadCreditCardBills(this.selectedCreditCard.id);
          }
        }, 500);
      },
      error: (err: any) => {
        console.error('Error updating credit limit:', err);
        alert('Failed to update credit limit: ' + (err.error?.message || 'Unknown error'));
      }
    });
  }

  // Bill Payments Management Methods
  loadBillPayments() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.isLoadingBillPayments = true;
    this.http.get(`${environment.apiBaseUrl}/api/bill-payments/all`).subscribe({
      next: (payments: any) => {
        this.billPayments = Array.isArray(payments) ? payments : [];
        this.isLoadingBillPayments = false;
      },
      error: (err: any) => {
        console.error('Error loading bill payments:', err);
        this.billPayments = [];
        this.isLoadingBillPayments = false;
        this.alertService.error('Error', 'Failed to load bill payments');
      }
    });
  }

  getFilteredBillPayments(): any[] {
    let filtered = [...this.billPayments];

    // Filter by type
    if (this.billPaymentsTypeFilter !== 'ALL') {
      filtered = filtered.filter(p => p.billType === this.billPaymentsTypeFilter);
    }

    // Filter by status
    if (this.billPaymentsStatusFilter !== 'ALL') {
      filtered = filtered.filter(p => p.status === this.billPaymentsStatusFilter);
    }

    // Filter by search query
    if (this.billPaymentsSearchQuery) {
      const query = this.billPaymentsSearchQuery.toLowerCase();
      filtered = filtered.filter(p =>
        (p.userName && p.userName.toLowerCase().includes(query)) ||
        (p.accountNumber && p.accountNumber.toLowerCase().includes(query)) ||
        (p.customerNumber && p.customerNumber.toLowerCase().includes(query)) ||
        (p.networkProvider && p.networkProvider.toLowerCase().includes(query)) ||
        (p.transactionId && p.transactionId.toLowerCase().includes(query))
      );
    }

    // Sort by payment date descending
    filtered.sort((a, b) => {
      const dateA = new Date(a.paymentDate || a.createdAt).getTime();
      const dateB = new Date(b.paymentDate || b.createdAt).getTime();
      return dateB - dateA;
    });

    return filtered;
  }

  getBillPaymentsTotal(): number {
    try {
      return this.getFilteredBillPayments().reduce((sum: number, p: any) => sum + (Number(p?.amount) || 0), 0);
    } catch (e) {
      return 0;
    }
  }

  getBillPaymentsCountByType(type: string): number {
    try {
      return this.getFilteredBillPayments().filter(p => p?.billType === type).length;
    } catch (e) {
      return 0;
    }
  }

  getMaskedCardNumber(cardNumber: string): string {
    if (!cardNumber) return '';
    if (cardNumber.length <= 4) return cardNumber;
    return '**** **** **** ' + cardNumber.substring(cardNumber.length - 4);
  }

  submitCreditCardApplication() {
    if (!this.applyCreditCardForm.accountNumber || !this.applyCreditCardForm.pan) {
      alert('Account Number and PAN are required');
      return;
    }
    
    const payload = {
      accountNumber: this.applyCreditCardForm.accountNumber,
      userName: this.applyCreditCardForm.userName,
      userEmail: this.applyCreditCardForm.userEmail,
      pan: this.applyCreditCardForm.pan
    };
    
    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/create?income=${this.applyCreditCardForm.income || 0}`, payload).subscribe({
      next: (res: any) => {
        alert('Credit card application submitted successfully');
        this.showApplyCreditCardModal = false;
        this.applyCreditCardForm = {
          accountNumber: '',
          userName: '',
          userEmail: '',
          pan: '',
          income: 0
        };
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error submitting credit card application:', err);
        alert('Failed to submit application: ' + (err.error?.message || 'Unknown error'));
      }
    });
  }
  
  // Support Ticket Methods
  loadSupportTickets() {
    this.isLoadingSupportTickets = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/support-tickets/all`).subscribe({
      next: (res: any) => {
        this.supportTickets = res.tickets || [];
        this.filterSupportTickets();
        this.isLoadingSupportTickets = false;
      },
      error: () => {
        this.alertService.error('Error', 'Failed to load support tickets');
        this.isLoadingSupportTickets = false;
      }
    });
  }

  loadSupportTicketStats() {
    this.http.get<any>(`${environment.apiBaseUrl}/api/support-tickets/stats`).subscribe({
      next: (res: any) => {
        this.supportTicketStats = {
          openCount: res.openCount || 0,
          inProgressCount: res.inProgressCount || 0,
          resolvedCount: res.resolvedCount || 0,
          totalCount: res.totalCount || 0
        };
      },
      error: () => {}
    });
  }

  filterSupportTickets() {
    let filtered = [...this.supportTickets];
    if (this.supportTicketFilter !== 'ALL') {
      filtered = filtered.filter((t: any) => t.status === this.supportTicketFilter);
    }
    if (this.supportTicketSearchQuery) {
      const q = this.supportTicketSearchQuery.toLowerCase();
      filtered = filtered.filter((t: any) =>
        (t.ticketId || '').toLowerCase().includes(q) ||
        (t.userName || '').toLowerCase().includes(q) ||
        (t.accountNumber || '').toLowerCase().includes(q) ||
        (t.subject || '').toLowerCase().includes(q)
      );
    }
    this.filteredSupportTickets = filtered;
  }

  selectSupportTicket(ticket: any) {
    this.selectedSupportTicket = this.selectedSupportTicket?.id === ticket.id ? null : ticket;
    this.adminResponseText = ticket.adminResponse || '';
  }

  updateTicketStatus(ticket: any, status: string) {
    this.http.put<any>(`${environment.apiBaseUrl}/api/support-tickets/${ticket.id}/status`, {
      status: status,
      adminResponse: this.adminResponseText || null
    }).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.success('Success', `Ticket ${status.toLowerCase().replace('_', ' ')} successfully`);
          this.loadSupportTickets();
          this.loadSupportTicketStats();
          this.selectedSupportTicket = null;
          this.adminResponseText = '';
        }
      },
      error: () => {
        this.alertService.error('Error', 'Failed to update ticket status');
      }
    });
  }

  respondToTicket(ticket: any) {
    if (!this.adminResponseText.trim()) {
      this.alertService.error('Validation', 'Please enter a response');
      return;
    }
    this.http.put<any>(`${environment.apiBaseUrl}/api/support-tickets/${ticket.id}/status`, {
      status: ticket.status === 'OPEN' ? 'IN_PROGRESS' : ticket.status,
      adminResponse: this.adminResponseText
    }).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.success('Success', 'Response sent successfully');
          this.loadSupportTickets();
          this.loadSupportTicketStats();
          this.selectedSupportTicket = res.ticket;
          this.adminResponseText = res.ticket.adminResponse || '';
        }
      },
      error: () => {
        this.alertService.error('Error', 'Failed to send response');
      }
    });
  }

  getTicketStatusColor(status: string): string {
    switch (status) {
      case 'OPEN': return '#ffc107';
      case 'IN_PROGRESS': return '#17a2b8';
      case 'RESOLVED': return '#28a745';
      case 'CLOSED': return '#6c757d';
      case 'REOPENED': return '#fd7e14';
      default: return '#6c757d';
    }
  }

  getTicketCategoryIcon(category: string): string {
    switch (category) {
      case 'TRANSACTION_FAILED': return 'fa-times-circle';
      case 'DEBIT_CARD_ISSUE': return 'fa-credit-card';
      case 'LOGIN_PROBLEM': return 'fa-lock';
      case 'LOAN_EMI': return 'fa-money-bill';
      case 'ACCOUNT_ISSUE': return 'fa-user-circle';
      default: return 'fa-tag';
    }
  }

  startSupportTicketPolling() {
    this.stopSupportTicketPolling();
    this.supportTicketPollingInterval = setInterval(() => {
      if (this.activeSection === 'support-tickets') {
        this.loadSupportTickets();
        this.loadSupportTicketStats();
      } else {
        this.stopSupportTicketPolling();
      }
    }, 10000);
  }

  stopSupportTicketPolling() {
    if (this.supportTicketPollingInterval) {
      clearInterval(this.supportTicketPollingInterval);
      this.supportTicketPollingInterval = null;
    }
  }

  // ─── Net Banking Service Control ─────────────────────────────────

  loadNetBankingStatus() {
    this.isLoadingNetBankingStatus = true;
    this.http.get(`${environment.apiBaseUrl}/api/net-banking-control/status`).subscribe({
      next: (response: any) => {
        this.isLoadingNetBankingStatus = false;
        if (response.SAVINGS_ACCOUNT) {
          this.netBankingServices.SAVINGS_ACCOUNT = response.SAVINGS_ACCOUNT;
        }
        if (response.CURRENT_ACCOUNT) {
          this.netBankingServices.CURRENT_ACCOUNT = response.CURRENT_ACCOUNT;
        }
      },
      error: () => {
        this.isLoadingNetBankingStatus = false;
      }
    });
  }

  loadNetBankingAuditHistory() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/net-banking-control/audit`).subscribe({
      next: (history: any[]) => {
        this.netBankingAuditHistory = history;
      },
      error: () => {
        this.netBankingAuditHistory = [];
      }
    });
  }

  toggleNetBanking(serviceType: string, enabled: boolean) {
    this.isTogglingNetBanking = true;
    const adminData = sessionStorage.getItem('admin');
    const adminName = adminData ? JSON.parse(adminData).name || JSON.parse(adminData).email : 'Admin';

    this.http.put(`${environment.apiBaseUrl}/api/net-banking-control/toggle`, {
      serviceType: serviceType,
      enabled: enabled,
      changedBy: adminName,
      remarks: enabled ? 'Service enabled by admin' : 'Service disabled by admin'
    }).subscribe({
      next: (response: any) => {
        this.isTogglingNetBanking = false;
        if (response.success) {
          this.alertService.success('Success', response.message);
          this.loadNetBankingStatus();
          this.loadNetBankingAuditHistory();
        }
      },
      error: (err: any) => {
        this.isTogglingNetBanking = false;
        this.alertService.error('Error', err.error?.message || 'Failed to update net banking service');
      }
    });
  }

  getServiceDisplayName(serviceType: string): string {
    switch (serviceType) {
      case 'SAVINGS_ACCOUNT': return 'Savings Account';
      case 'CURRENT_ACCOUNT': return 'Current Account';
      default: return serviceType;
    }
  }

  // Per-Customer Net Banking Methods
  searchNetBankingCustomers(page: number = 0) {
    this.isSearchingNetBankingCustomers = true;
    const endpoint = this.netBankingCustomerType === 'savings' ? 'customers/savings' : 'customers/current';
    this.http.get(`${environment.apiBaseUrl}/api/net-banking-control/${endpoint}`, {
      params: { search: this.netBankingCustomerSearch, page: page.toString(), size: '10' }
    }).subscribe({
      next: (response: any) => {
        this.isSearchingNetBankingCustomers = false;
        this.netBankingCustomerResults = response.customers || [];
        this.netBankingCustomerTotalPages = response.totalPages || 0;
        this.netBankingCustomerCurrentPage = response.currentPage || 0;
        this.netBankingCustomerTotalElements = response.totalElements || 0;
      },
      error: (err: any) => {
        this.isSearchingNetBankingCustomers = false;
        this.netBankingCustomerResults = [];
        this.alertService.error('Error', 'Failed to search customers');
      }
    });
  }

  toggleCustomerNetBanking(customer: any, enabled: boolean) {
    this.isTogglingCustomerNetBanking = true;
    const adminName = sessionStorage.getItem('adminName') || 'Admin';
    const serviceType = this.netBankingCustomerType === 'savings' ? 'SAVINGS_ACCOUNT' : 'CURRENT_ACCOUNT';
    this.http.put(`${environment.apiBaseUrl}/api/net-banking-control/customer/toggle`, {
      accountNumber: customer.accountNumber,
      serviceType: serviceType,
      enabled: enabled,
      changedBy: adminName,
      remarks: enabled ? 'Net banking enabled for customer by admin' : 'Net banking disabled for customer by admin'
    }).subscribe({
      next: (response: any) => {
        this.isTogglingCustomerNetBanking = false;
        if (response.success) {
          this.alertService.success('Success', response.message);
          customer.netBankingEnabled = enabled;
          customer.netBankingToggledBy = adminName;
          customer.netBankingToggledAt = new Date().toISOString();
          this.loadNetBankingAuditHistory();
        }
      },
      error: (err: any) => {
        this.isTogglingCustomerNetBanking = false;
        this.alertService.error('Error', err.error?.message || 'Failed to toggle customer net banking');
      }
    });
  }

  changeNetBankingCustomerType(type: string) {
    this.netBankingCustomerType = type;
    this.netBankingCustomerSearch = '';
    this.netBankingCustomerResults = [];
    this.netBankingCustomerTotalPages = 0;
    this.netBankingCustomerCurrentPage = 0;
  }

  // ======================== PASSBOOK MANAGEMENT ========================

  loadPassbookAccounts() {
    this.isLoadingPassbookAccounts = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/passbook/accounts/all`).subscribe({
      next: (accounts) => {
        this.passbookAccounts = accounts;
        this.passbookStats = {
          savings: accounts.filter((a: any) => a.type === 'savings').length,
          current: accounts.filter((a: any) => a.type === 'current').length,
          salary: accounts.filter((a: any) => a.type === 'salary').length,
          total: accounts.length,
          frozen: accounts.filter((a: any) => (a.status || '').toUpperCase() === 'FROZEN').length,
          closed: accounts.filter((a: any) => (a.status || '').toUpperCase() === 'CLOSED').length
        };
        this.filterPassbookAccounts();
        this.isLoadingPassbookAccounts = false;
      },
      error: (err) => {
        this.isLoadingPassbookAccounts = false;
        this.alertService.error('Error', 'Failed to load accounts for passbook generation.');
        console.error('Passbook accounts load error:', err);
      }
    });
  }

  filterPassbookAccounts() {
    let filtered = [...this.passbookAccounts];

    // Filter by account type
    if (this.passbookAccountTypeFilter !== 'all') {
      filtered = filtered.filter(a => a.type === this.passbookAccountTypeFilter);
    }

    // Filter by status
    if (this.passbookStatusFilter !== 'all') {
      filtered = filtered.filter(a => (a.status || 'active').toLowerCase() === this.passbookStatusFilter);
    }

    // Filter by search query
    if (this.passbookSearchQuery && this.passbookSearchQuery.trim()) {
      const q = this.passbookSearchQuery.toLowerCase().trim();
      filtered = filtered.filter(a =>
        (a.accountNumber && a.accountNumber.toLowerCase().includes(q)) ||
        (a.name && a.name.toLowerCase().includes(q)) ||
        (a.phone && a.phone.toLowerCase().includes(q)) ||
        (a.customerId !== null && a.customerId !== undefined && String(a.customerId).toLowerCase().includes(q))
      );
    }

    // Sort
    switch (this.passbookSortBy) {
      case 'name':
        filtered.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
        break;
      case 'balance-desc':
        filtered.sort((a, b) => (b.balance || 0) - (a.balance || 0));
        break;
      case 'balance-asc':
        filtered.sort((a, b) => (a.balance || 0) - (b.balance || 0));
        break;
      case 'account':
        filtered.sort((a, b) => (a.accountNumber || '').localeCompare(b.accountNumber || ''));
        break;
      case 'type':
        filtered.sort((a, b) => (a.type || '').localeCompare(b.type || ''));
        break;
    }

    this.filteredPassbookAccounts = filtered;
    this.passbookTotalPages = Math.max(1, Math.ceil(filtered.length / this.passbookPageSize));
    if (this.passbookCurrentPage > this.passbookTotalPages) this.passbookCurrentPage = 1;
    this.paginatePassbookAccounts();
  }

  paginatePassbookAccounts() {
    const start = (this.passbookCurrentPage - 1) * this.passbookPageSize;
    this.paginatedPassbookAccounts = this.filteredPassbookAccounts.slice(start, start + Number(this.passbookPageSize));
  }

  sortPassbookBy(field: string) {
    if (field === 'balance') {
      this.passbookSortBy = this.passbookSortBy === 'balance-desc' ? 'balance-asc' : 'balance-desc';
    } else {
      this.passbookSortBy = field;
    }
    this.filterPassbookAccounts();
  }

  // Selection
  isPassbookAccountSelected(acc: any): boolean {
    return this.selectedPassbookAccounts.some(s => s.accountNumber === acc.accountNumber);
  }

  togglePassbookAccountSelect(acc: any) {
    if (this.isPassbookAccountSelected(acc)) {
      this.selectedPassbookAccounts = this.selectedPassbookAccounts.filter(s => s.accountNumber !== acc.accountNumber);
    } else {
      this.selectedPassbookAccounts.push(acc);
    }
  }

  isAllPassbookSelected(): boolean {
    return this.paginatedPassbookAccounts.length > 0 && this.paginatedPassbookAccounts.every(a => this.isPassbookAccountSelected(a));
  }

  toggleSelectAllPassbook(event: any) {
    if (event.target.checked) {
      this.paginatedPassbookAccounts.forEach(a => {
        if (!this.isPassbookAccountSelected(a)) this.selectedPassbookAccounts.push(a);
      });
    } else {
      const pageAccNums = this.paginatedPassbookAccounts.map(a => a.accountNumber);
      this.selectedPassbookAccounts = this.selectedPassbookAccounts.filter(s => !pageAccNums.includes(s.accountNumber));
    }
  }

  // Preview
  previewPassbook(acc: any) {
    this.previewAccount = acc;
    this.showPassbookPreview = true;
  }

  // History
  togglePassbookHistory() {
    this.showPassbookHistoryModal = !this.showPassbookHistoryModal;
  }

  // ==================== Account Actions (Freeze/Unfreeze/Close/Delete) ====================

  freezePassbookAccount(acc: any) {
    this.accountActionType = 'freeze';
    this.accountActionTarget = acc;
    this.accountActionReason = '';
    this.showAccountActionModal = true;
  }

  unfreezePassbookAccount(acc: any) {
    this.accountActionType = 'unfreeze';
    this.accountActionTarget = acc;
    this.accountActionReason = '';
    this.showAccountActionModal = true;
  }

  closePassbookAccount(acc: any) {
    this.accountActionType = 'close';
    this.accountActionTarget = acc;
    this.accountActionReason = '';
    this.showAccountActionModal = true;
  }

  deletePassbookAccount(acc: any) {
    this.accountActionType = 'delete';
    this.accountActionTarget = acc;
    this.accountActionReason = '';
    this.showAccountActionModal = true;
  }

  confirmAccountAction() {
    if (!this.accountActionTarget) return;
    const acc = this.accountActionTarget;
    const type = acc.type; // savings, current, salary
    this.passbookActionLoading = acc.accountNumber;

    let url = '';
    let method = 'put';
    const reason = encodeURIComponent(this.accountActionReason || `${this.accountActionType}d by admin`);

    switch (this.accountActionType) {
      case 'freeze':
        url = `${environment.apiBaseUrl}/api/passbook/freeze/${acc.id}?accountType=${type}&reason=${reason}&frozenBy=Admin`;
        break;
      case 'unfreeze':
        url = `${environment.apiBaseUrl}/api/passbook/unfreeze/${acc.id}?accountType=${type}`;
        break;
      case 'close':
        url = `${environment.apiBaseUrl}/api/passbook/close/${acc.id}?accountType=${type}&reason=${reason}&closedBy=Admin`;
        break;
      case 'delete':
        url = `${environment.apiBaseUrl}/api/passbook/delete/${acc.id}?accountType=${type}`;
        method = 'delete';
        break;
    }

    const request = method === 'delete'
      ? this.http.delete<any>(url)
      : this.http.put<any>(url, {});

    request.subscribe({
      next: (res: any) => {
        this.passbookActionLoading = '';
        this.showAccountActionModal = false;
        const actionLabel = this.accountActionType === 'freeze' ? 'frozen' :
          this.accountActionType === 'unfreeze' ? 'unfrozen' :
          this.accountActionType === 'close' ? 'closed' : 'deleted';
        this.alertService.success('Success', `Account ${acc.accountNumber} has been ${actionLabel} successfully.`);
        this.loadPassbookAccounts(); // Real-time refresh
      },
      error: (err: any) => {
        this.passbookActionLoading = '';
        const msg = err?.error?.message || `Failed to ${this.accountActionType} account. Please try again.`;
        this.alertService.error('Error', msg);
        console.error(`Account ${this.accountActionType} error:`, err);
      }
    });
  }

  generatePassbook(account: any) {
    this.passbookGeneratingFor = account.accountNumber;
    const url = `${environment.apiBaseUrl}/api/passbook/generate/${account.accountNumber}?accountType=${account.type}`;

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        if (blob.type && blob.type.includes('application/json')) {
          this.passbookGeneratingFor = '';
          this.alertService.error('Error', 'Passbook generation failed for this account.');
          return;
        }
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = `Passbook_${account.accountNumber}.pdf`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        this.passbookGeneratingFor = '';
        // Add to history
        this.passbookDownloadHistory.unshift({
          ...account,
          generatedAt: new Date().toLocaleString()
        });
        this.alertService.success('Success', `Passbook generated for ${account.name} (${account.accountNumber})`);
      },
      error: (err) => {
        this.passbookGeneratingFor = '';
        this.alertService.error('Error', 'Failed to generate passbook. Please try again.');
        console.error('Passbook generation error:', err);
      }
    });
  }

  // Bulk generation
  bulkGeneratePassbooks() {
    if (this.selectedPassbookAccounts.length === 0) return;
    this.isBulkGenerating = true;
    let completed = 0;
    let failed = 0;
    const total = this.selectedPassbookAccounts.length;

    this.selectedPassbookAccounts.forEach(acc => {
      const url = `${environment.apiBaseUrl}/api/passbook/generate/${acc.accountNumber}?accountType=${acc.type}`;
      this.http.get(url, { responseType: 'blob' }).subscribe({
        next: (blob) => {
          if (blob.type && blob.type.includes('application/json')) {
            failed++;
            completed++;
            if (completed >= total) {
              this.isBulkGenerating = false;
              this.selectedPassbookAccounts = [];
              if (failed === 0) {
                this.alertService.success('Bulk Complete', `${total} passbooks generated successfully!`);
              } else {
                this.alertService.error('Partial Failure', `${failed} passbook(s) failed out of ${total}.`);
              }
            }
            return;
          }
          const downloadUrl = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = downloadUrl;
          link.download = `Passbook_${acc.accountNumber}.pdf`;
          link.click();
          window.URL.revokeObjectURL(downloadUrl);
          this.passbookDownloadHistory.unshift({ ...acc, generatedAt: new Date().toLocaleString() });
          completed++;
          if (completed >= total) {
            this.isBulkGenerating = false;
            this.selectedPassbookAccounts = [];
            if (failed === 0) {
              this.alertService.success('Bulk Complete', `${total} passbooks generated successfully!`);
            } else {
              this.alertService.error('Partial Failure', `${failed} passbook(s) failed out of ${total}.`);
            }
          }
        },
        error: () => {
          failed++;
          completed++;
          if (completed >= total) {
            this.isBulkGenerating = false;
            this.selectedPassbookAccounts = [];
            this.alertService.error('Partial Failure', `${failed} passbook(s) failed out of ${total}.`);
          }
        }
      });
    });
  }

  ngOnDestroy() {
    // Clear session timer
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = null;
    }
    this.stopSupportTicketPolling();
  }

  // ==================== Face ID Authentication Management ====================

  /**
   * Load Face ID credentials for the current admin
   */
  loadBiometricCredentials() {
    this.isBiometricSupported = this.faceAuthService.isCameraSupported();
    this.biometricSuccessMessage = '';
    this.biometricErrorMessage = '';

    const adminData = sessionStorage.getItem('admin');
    if (!adminData) return;

    const admin = JSON.parse(adminData);
    const email = admin.email;
    if (!email) return;

    this.isBiometricLoading = true;
    this.biometricLoadingMessage = 'Loading Face ID status...';

    this.faceAuthService.getFaceStatus(email).subscribe({
      next: (response: any) => {
        this.isBiometricLoading = false;
        if (response && response.success && response.registered) {
          this.biometricRegistered = true;
          this.biometricCredentials = response.credentials || [];
        } else {
          this.biometricRegistered = false;
          this.biometricCredentials = [];
        }
      },
      error: () => {
        this.isBiometricLoading = false;
        this.biometricRegistered = false;
        this.biometricCredentials = [];
      }
    });
  }

  stopBiometricCamera() {
    this.faceAuthService.stopCamera(this.biometricCameraStream);
    this.biometricCameraStream = null;
    this.showBiometricCamera = false;
    this.biometricFaceStatus = '';
  }

  /**
   * Register Face ID using laptop camera
   */
  async registerBiometric() {
    const adminData = sessionStorage.getItem('admin');
    if (!adminData) {
      this.biometricErrorMessage = 'Admin session not found. Please login again.';
      return;
    }

    const admin = JSON.parse(adminData);
    const email = admin.email;

    this.isBiometricLoading = true;
    this.biometricLoadingMessage = 'Loading face recognition models...';
    this.biometricSuccessMessage = '';
    this.biometricErrorMessage = '';
    this.showBiometricCamera = true;

    try {
      await this.faceAuthService.loadModels();
      this.biometricLoadingMessage = 'Starting camera...';

      await new Promise(resolve => setTimeout(resolve, 200));
      const video = this.dashboardVideoRef.nativeElement;
      this.biometricCameraStream = await this.faceAuthService.openCamera(video);

      this.biometricFaceStatus = 'Look at the camera with eyes open...';
      this.biometricLoadingMessage = 'Capturing face... Keep eyes open and hold still.';

      await new Promise(resolve => setTimeout(resolve, 1500));

      const result = await this.faceAuthService.captureMultipleDescriptors(
        video, 5, 500,
        (status: string) => { this.biometricFaceStatus = status; }
      );
      this.stopBiometricCamera();

      if (!result) {
        this.isBiometricLoading = false;
        this.biometricErrorMessage = 'Face not detected or eyes were closed. Ensure good lighting and keep eyes open.';
        return;
      }

      if (result.captureCount < 3) {
        this.isBiometricLoading = false;
        this.biometricErrorMessage = 'Could not get enough clear captures. Ensure good lighting, face the camera, and keep eyes open.';
        return;
      }

      this.biometricLoadingMessage = 'Saving Face ID...';

      this.faceAuthService.registerFace(email, result.descriptor).subscribe({
        next: (response: any) => {
          this.isBiometricLoading = false;
          if (response && response.success) {
            this.biometricSuccessMessage = 'Face ID Registered Successfully! You can now use Face ID to login.';
            this.alertService.success('Success', 'Face ID Registered Successfully!');
            this.loadBiometricCredentials();
          } else {
            this.biometricErrorMessage = 'Failed to register Face ID. Please try again.';
          }
        },
        error: (err: any) => {
          this.isBiometricLoading = false;
          this.biometricErrorMessage = err.error?.message || 'Failed to register Face ID. Please try again.';
        }
      });
    } catch (err: any) {
      this.stopBiometricCamera();
      this.isBiometricLoading = false;
      if (err.name === 'NotAllowedError') {
        this.biometricErrorMessage = 'Camera access denied. Please allow camera permission and try again.';
      } else {
        this.biometricErrorMessage = 'Failed to access camera. Please check your camera and try again.';
      }
    }
  }

  /**
   * Re-register Face ID (capture new face)
   */
  async reRegisterBiometric() {
    await this.registerBiometric();
  }

  /**
   * Remove Face ID credential
   */
  removeBiometric() {
    const adminData = sessionStorage.getItem('admin');
    if (!adminData) {
      this.biometricErrorMessage = 'Admin session not found.';
      return;
    }

    const admin = JSON.parse(adminData);
    const email = admin.email;

    this.isBiometricLoading = true;
    this.biometricLoadingMessage = 'Removing Face ID...';
    this.biometricSuccessMessage = '';
    this.biometricErrorMessage = '';

    this.faceAuthService.removeFace(email).subscribe({
      next: (response: any) => {
        this.isBiometricLoading = false;
        if (response && response.success) {
          this.biometricRegistered = false;
          this.biometricCredentials = [];
          this.biometricSuccessMessage = 'Face ID removed successfully.';
          this.alertService.success('Removed', 'Face ID credential removed.');
        } else {
          this.biometricErrorMessage = 'Failed to remove Face ID.';
        }
      },
      error: () => {
        this.isBiometricLoading = false;
        this.biometricErrorMessage = 'Failed to remove Face ID credential.';
      }
    });
  }

  // ═══════════════════════════════════════════════════════
  // ── ATM Management Methods ────────────────────────────
  // ═══════════════════════════════════════════════════════

  loadAtmDashboardStats(): void {
    this.isLoadingAtmData = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admin/atm/dashboard-stats`).subscribe({
      next: (res) => {
        this.atmDashboardStats = res;
        this.isLoadingAtmData = false;
      },
      error: (err) => {
        console.error('Failed to load ATM stats:', err);
        this.isLoadingAtmData = false;
      }
    });
  }

  loadAtmMachines(): void {
    this.http.get<any>(`${environment.apiBaseUrl}/api/admin/atm/machines`).subscribe({
      next: (res) => {
        this.atmMachines = res.atms || [];
      },
      error: (err) => console.error('Failed to load ATMs:', err)
    });
  }

  loadAtmTransactions(page: number = 0): void {
    this.atmTransactionPage = page;
    let url = `${environment.apiBaseUrl}/api/admin/atm/transactions?page=${page}&size=20`;
    if (this.atmTransactionFilter && this.atmTransactionFilter !== 'ALL') {
      url = `${environment.apiBaseUrl}/api/admin/atm/transactions/status/${this.atmTransactionFilter}?page=${page}&size=20`;
    }
    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.atmTransactions = res.transactions || [];
        this.atmTransactionTotalPages = res.totalPages || 0;
      },
      error: (err) => console.error('Failed to load ATM transactions:', err)
    });
  }

  loadAtmIncidents(page: number = 0): void {
    this.atmIncidentPage = page;
    let url = `${environment.apiBaseUrl}/api/admin/atm/incidents?page=${page}&size=20`;
    if (this.atmIncidentFilter && this.atmIncidentFilter !== 'ALL') {
      url = `${environment.apiBaseUrl}/api/admin/atm/incidents/status/${this.atmIncidentFilter}?page=${page}&size=20`;
    }
    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.atmIncidents = res.incidents || [];
        this.atmIncidentTotalPages = res.totalPages || 0;
      },
      error: (err) => console.error('Failed to load ATM incidents:', err)
    });
  }

  loadAtmServiceLogs(): void {
    this.http.get<any>(`${environment.apiBaseUrl}/api/admin/atm/service-logs?page=0&size=50`).subscribe({
      next: (res) => {
        this.atmServiceLogs = res.logs || [];
      },
      error: (err) => console.error('Failed to load ATM logs:', err)
    });
  }

  loadAtmCashHistory(atmId: string): void {
    this.http.get<any>(`${environment.apiBaseUrl}/api/admin/atm/machines/${atmId}/cash-history?page=0&size=20`).subscribe({
      next: (res) => {
        this.atmCashLoads = res.cashLoads || [];
      },
      error: (err) => console.error('Failed to load cash history:', err)
    });
  }

  setAtmTab(tab: string): void {
    this.atmActiveTab = tab;
    if (tab === 'transactions') this.loadAtmTransactions();
    if (tab === 'incidents') this.loadAtmIncidents();
    if (tab === 'service-logs') this.loadAtmServiceLogs();
    if (tab === 'machines') this.loadAtmMachines();
    if (tab === 'overview') { this.loadAtmDashboardStats(); this.loadAtmMachines(); }
  }

  // Cash Loading
  openCashLoadForm(atm: any): void {
    this.atmCashLoadForm = { atmId: atm.atmId, amount: 0, notes500: 0, notes200: 0, notes100: 0, notes2000: 0, remarks: '' };
    this.atmSelectedMachine = atm;
    this.atmActiveTab = 'cash-load';
  }

  calculateCashLoadTotal(): number {
    return (this.atmCashLoadForm.notes2000 * 2000) + (this.atmCashLoadForm.notes500 * 500) +
           (this.atmCashLoadForm.notes200 * 200) + (this.atmCashLoadForm.notes100 * 100);
  }

  submitCashLoad(): void {
    const total = this.calculateCashLoadTotal();
    if (total <= 0) {
      this.alertService.error('Error', 'Please enter valid note denominations');
      return;
    }
    this.atmCashLoadForm.amount = total;
    this.atmCashLoadForm.loadedBy = this.adminName;
    this.isLoadingCashLoad = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/admin/atm/machines/${this.atmCashLoadForm.atmId}/load-cash`, this.atmCashLoadForm).subscribe({
      next: (res) => {
        this.isLoadingCashLoad = false;
        if (res.success) {
          this.alertService.success('Success', `₹${total.toLocaleString('en-IN')} loaded into ATM ${this.atmCashLoadForm.atmId}. New Balance: ₹${res.newBalance?.toLocaleString('en-IN')}`);
          this.loadAtmMachines();
          this.loadAtmDashboardStats();
          this.atmActiveTab = 'machines';
        } else {
          this.alertService.error('Error', res.message || 'Failed to load cash');
        }
      },
      error: (err) => {
        this.isLoadingCashLoad = false;
        this.alertService.error('Error', err?.error?.message || 'Failed to load cash');
      }
    });
  }

  // ATM Status Change
  openStatusModal(atm: any): void {
    this.atmStatusChangeMachine = atm;
    this.atmStatusChangeForm = { status: atm.status, reason: '' };
    this.showAtmStatusModal = true;
  }

  closeStatusModal(): void {
    this.showAtmStatusModal = false;
    this.atmStatusChangeMachine = null;
  }

  submitStatusChange(): void {
    if (!this.atmStatusChangeForm.reason) {
      this.alertService.error('Error', 'Please provide a reason for status change');
      return;
    }
    const body = {
      status: this.atmStatusChangeForm.status,
      performedBy: this.adminName,
      reason: this.atmStatusChangeForm.reason
    };
    this.http.put<any>(`${environment.apiBaseUrl}/api/admin/atm/machines/${this.atmStatusChangeMachine.atmId}/status`, body).subscribe({
      next: (res) => {
        if (res.success) {
          this.alertService.success('Success', `ATM ${this.atmStatusChangeMachine.atmId} status changed to ${this.atmStatusChangeForm.status}`);
          this.closeStatusModal();
          this.loadAtmMachines();
          this.loadAtmDashboardStats();
        }
      },
      error: (err) => {
        this.alertService.error('Error', err?.error?.message || 'Failed to change status');
      }
    });
  }

  setAllAtmsOutOfService(): void {
    if (!confirm('Are you sure you want to set ALL ATMs to OUT OF SERVICE?')) return;
    this.http.post<any>(`${environment.apiBaseUrl}/api/admin/atm/machines/all-out-of-service`, {
      performedBy: this.adminName,
      reason: 'Bulk shutdown by admin'
    }).subscribe({
      next: () => {
        this.alertService.success('Success', 'All ATMs set to OUT OF SERVICE');
        this.loadAtmMachines();
        this.loadAtmDashboardStats();
      },
      error: (err) => this.alertService.error('Error', err?.error?.message || 'Failed')
    });
  }

  setAllAtmsActive(): void {
    if (!confirm('Are you sure you want to ACTIVATE all ATMs?')) return;
    this.http.post<any>(`${environment.apiBaseUrl}/api/admin/atm/machines/all-activate`, {
      performedBy: this.adminName,
      reason: 'Bulk activation by admin'
    }).subscribe({
      next: () => {
        this.alertService.success('Success', 'All ATMs activated');
        this.loadAtmMachines();
        this.loadAtmDashboardStats();
      },
      error: (err) => this.alertService.error('Error', err?.error?.message || 'Failed')
    });
  }

  // ATM Create/Edit
  openAtmCreateForm(): void {
    this.atmFormMode = 'create';
    this.atmForm = { atmId: '', atmName: '', location: '', city: '', state: '', pincode: '', maxCapacity: 5000000, minThreshold: 100000, maxWithdrawalLimit: 25000, dailyLimit: 100000, managedBy: this.adminName, contactNumber: '' };
    this.showAtmForm = true;
  }

  openAtmEditForm(atm: any): void {
    this.atmFormMode = 'edit';
    this.atmForm = { ...atm };
    this.showAtmForm = true;
  }

  closeAtmForm(): void {
    this.showAtmForm = false;
  }

  submitAtmForm(): void {
    if (this.atmFormMode === 'create') {
      this.http.post<any>(`${environment.apiBaseUrl}/api/admin/atm/machines`, this.atmForm).subscribe({
        next: (res) => {
          if (res.success) {
            this.alertService.success('Success', 'ATM created successfully');
            this.closeAtmForm();
            this.loadAtmMachines();
            this.loadAtmDashboardStats();
          }
        },
        error: (err) => this.alertService.error('Error', err?.error?.message || 'Failed to create ATM')
      });
    } else {
      this.http.put<any>(`${environment.apiBaseUrl}/api/admin/atm/machines/${this.atmForm.atmId}`, this.atmForm).subscribe({
        next: (res) => {
          if (res.success) {
            this.alertService.success('Success', 'ATM updated successfully');
            this.closeAtmForm();
            this.loadAtmMachines();
          }
        },
        error: (err) => this.alertService.error('Error', err?.error?.message || 'Failed to update ATM')
      });
    }
  }

  // Incident Management
  openIncidentForm(): void {
    this.atmIncidentForm = { atmId: '', incidentType: 'CARD_STUCK', accountNumber: '', cardNumber: '', userName: '', description: '', amountInvolved: 0 };
    this.showAtmIncidentForm = true;
  }

  closeIncidentForm(): void {
    this.showAtmIncidentForm = false;
  }

  submitIncident(): void {
    this.http.post<any>(`${environment.apiBaseUrl}/api/admin/atm/incidents`, this.atmIncidentForm).subscribe({
      next: (res) => {
        if (res.success) {
          this.alertService.success('Success', 'Incident reported: ' + res.incident?.incidentRef);
          this.closeIncidentForm();
          this.loadAtmIncidents();
        }
      },
      error: (err) => this.alertService.error('Error', err?.error?.message || 'Failed to report incident')
    });
  }

  resolveIncident(incident: any): void {
    const resolution = prompt('Enter resolution details:');
    if (!resolution) return;
    this.http.put<any>(`${environment.apiBaseUrl}/api/admin/atm/incidents/${incident.incidentRef}/resolve`, {
      resolution: resolution,
      resolvedBy: this.adminName
    }).subscribe({
      next: (res) => {
        if (res.success) {
          this.alertService.success('Success', 'Incident resolved');
          this.loadAtmIncidents();
        }
      },
      error: (err) => this.alertService.error('Error', err?.error?.message || 'Failed to resolve incident')
    });
  }

  selectAtmMachine(atm: any): void {
    this.atmSelectedMachine = atm;
    this.loadAtmCashHistory(atm.atmId);
  }

  getAtmStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'atm-status-active';
      case 'OUT_OF_SERVICE': return 'atm-status-oos';
      case 'MAINTENANCE': return 'atm-status-maintenance';
      case 'LOW_CASH': return 'atm-status-low';
      default: return '';
    }
  }

  getAtmStatusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'Active';
      case 'OUT_OF_SERVICE': return 'Out of Service';
      case 'MAINTENANCE': return 'Maintenance';
      case 'LOW_CASH': return 'Low Cash';
      default: return status;
    }
  }

  formatCurrency(amount: any): string {
    if (amount == null) return '₹0';
    return '₹' + Number(amount).toLocaleString('en-IN');
  }

  // ── Signature Management Methods ──────────────────────────────────

  sigLookupAccount() {
    if (!this.sigLookupAccountNumber || this.sigLookupAccountNumber.trim() === '') {
      this.sigLookupError = 'Please enter an account number';
      return;
    }
    this.sigIsLookingUp = true;
    this.sigLookupError = '';
    this.sigLookupResult = null;
    this.sigSelectedFile = null;
    this.sigUploadPreview = null;
    this.sigValidationResult = null;
    this.sigFormDownloaded = false;

    this.http.get(`${environment.apiBaseUrl}/api/admin/signature-management/lookup/${this.sigLookupAccountNumber.trim()}`).subscribe({
      next: (res: any) => {
        this.sigLookupResult = res;
        this.sigIsLookingUp = false;
      },
      error: (err: any) => {
        this.sigLookupError = err.error?.error || 'Account not found';
        this.sigIsLookingUp = false;
      }
    });
  }

  sigDownloadForm() {
    if (!this.sigLookupResult) return;
    this.sigIsDownloadingForm = true;
    const { accountType, accountId } = this.sigLookupResult;

    this.http.get(`${environment.apiBaseUrl}/api/admin/signature-management/download-form/${accountType}/${accountId}`, { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `signature_form_${this.sigLookupResult.accountNumber}.html`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.sigIsDownloadingForm = false;
        this.sigFormDownloaded = true;
        this.alertService.success('Downloaded', 'Signature form downloaded. Print, sign, and upload back.');
      },
      error: () => {
        this.sigIsDownloadingForm = false;
        this.alertService.error('Error', 'Failed to download signature form');
      }
    });
  }

  sigOnFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.sigSelectedFile = file;
      // Preview for images
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.sigUploadPreview = e.target.result;
        };
        reader.readAsDataURL(file);
      } else {
        this.sigUploadPreview = null;
      }
    }
  }

  sigUploadSignature() {
    if (!this.sigSelectedFile || !this.sigLookupResult) return;
    this.sigIsUploading = true;
    this.sigValidationResult = null;
    const { accountType, accountId } = this.sigLookupResult;

    const formData = new FormData();
    formData.append('file', this.sigSelectedFile);

    this.http.post(`${environment.apiBaseUrl}/api/admin/signature-management/upload/${accountType}/${accountId}`, formData).subscribe({
      next: (res: any) => {
        this.sigIsUploading = false;
        this.sigValidationResult = {
          success: true,
          accountMatchPassed: res.accountMatchPassed,
          signatureDetected: res.signatureDetected,
          signatureConfidence: res.signatureConfidence,
          accountNumber: res.accountNumber,
          requiresManualCheck: res.requiresManualCheck || false,
          manualCheckMessage: res.manualCheckMessage || ''
        };
        this.alertService.success('Uploaded', 'Signature verified & uploaded successfully. Account matched: ' + res.accountNumber);
        this.sigLookupAccount();
      },
      error: (err: any) => {
        this.sigIsUploading = false;
        const errData = err.error || {};
        this.sigValidationResult = {
          success: false,
          validationFailed: errData.validationFailed || 'UNKNOWN',
          accountMatchPassed: errData.accountMatchPassed || false,
          signatureDetected: errData.signatureDetected || false,
          confidence: errData.confidence || 0,
          darkPercentage: errData.darkPercentage,
          inkPercentage: errData.inkPercentage,
          message: errData.error || 'Upload failed'
        };
        this.alertService.error('Upload Rejected', errData.error || 'Validation failed');
      }
    });
  }

  sigVerifySignature() {
    if (!this.sigLookupResult) return;
    this.sigIsVerifying = true;
    const { accountType, accountId } = this.sigLookupResult;

    this.http.post(`${environment.apiBaseUrl}/api/admin/signature-management/verify/${accountType}/${accountId}`, { verifiedBy: 'Admin' }).subscribe({
      next: () => {
        this.sigIsVerifying = false;
        this.alertService.success('Verified', 'Signature has been verified successfully');
        this.sigLookupAccount();
      },
      error: (err: any) => {
        this.sigIsVerifying = false;
        this.alertService.error('Error', err.error?.error || 'Failed to verify signature');
      }
    });
  }

  sigRejectSignature() {
    if (!this.sigLookupResult) return;
    this.sigIsRejecting = true;
    const { accountType, accountId } = this.sigLookupResult;

    this.http.delete(`${environment.apiBaseUrl}/api/admin/signature-management/reject/${accountType}/${accountId}`).subscribe({
      next: () => {
        this.sigIsRejecting = false;
        this.alertService.success('Rejected', 'Signature rejected. Customer needs to re-upload.');
        this.sigLookupAccount();
      },
      error: (err: any) => {
        this.sigIsRejecting = false;
        this.alertService.error('Error', err.error?.error || 'Failed to reject signature');
      }
    });
  }

  sigGetViewUrl(): string {
    if (!this.sigLookupResult) return '';
    return `${environment.apiBaseUrl}/api/admin/signature-management/view/${this.sigLookupResult.accountType}/${this.sigLookupResult.accountId}`;
  }

  sigGetAccountTypeLabel(type: string): string {
    switch (type) {
      case 'savings': return 'Savings Account';
      case 'salary': return 'Salary Account';
      case 'business': return 'Business/Current Account';
      default: return type;
    }
  }
}
