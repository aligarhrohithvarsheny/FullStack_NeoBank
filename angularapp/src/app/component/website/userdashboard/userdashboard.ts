import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { FasttagService } from '../../../service/fasttag.service';
import { CurrentAccountService } from '../../../service/current-account.service';
import { LinkedAccount } from '../../../model/current-account/current-account.model';
import { environment } from '../../../../environment/environment';
import { Chat } from '../chat/chat';
import { AIAssistant } from '../ai-assistant/ai-assistant';
import { Transferfunds } from '../transferfunds/transferfunds';
import { Transaction } from '../transaction/transaction';
import { Card } from '../card/card';
import { CreditCard } from '../creditcard/creditcard';
import { Loan } from '../loan/loan';
import { ChequeComponent } from '../cheque/cheque';
import { Kycupdate } from '../kycupdate/kycupdate';
import { Goldloan } from '../goldloan/goldloan';
import { SubsidyClaim } from '../subsidy-claim/subsidy-claim';
import { BillPayment } from '../billpayment/billpayment';
import { ScheduledPayments } from '../scheduled-payments/scheduled-payments';
import { SupportTickets } from '../support-tickets/support-tickets';
import { BankMessagesComponent } from '../bank-messages/bank-messages';
import { VirtualCardComponent } from '../virtual-card/virtual-card';
import { BeneficiaryManagementComponent } from '../beneficiary-management/beneficiary-management';
import { AtmSimulatorComponent } from '../atm-simulator/atm-simulator';
import { FasttagUser } from '../fasttag/fasttag-user';
import { timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
// import { UserService } from '../../service/user';
// import { AccountService } from '../../service/account';
// import { TransactionService } from '../../service/transaction';

@Component({
  selector: 'app-userdashboard',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    Chat, 
    AIAssistant,
    Transferfunds,
    Transaction,
    Card,
    CreditCard,
    Loan,
    ChequeComponent,
    Kycupdate,
    Goldloan,
    SubsidyClaim,
    BillPayment,
    ScheduledPayments,
    SupportTickets,
    BankMessagesComponent,
    VirtualCardComponent,
    BeneficiaryManagementComponent,
    AtmSimulatorComponent,
    FasttagUser
  ],
  templateUrl: './userdashboard.html',
  styleUrls: ['./userdashboard.css']
})
export class Userdashboard implements OnInit, OnDestroy {
  username: string = 'User'; // default
  selectedFeature: string | null = null;
  currentBalance: number = 0;
  userAccountNumber: string = '';
  loading: boolean = false;
  error: string = '';
  errorTimeout: any = null;
  userProfile: any = null;
  
  // QR Code properties
  qrCodeImage: string = '';
  upiId: string = '';
  showQrCode: boolean = false;
  showQrScanner: boolean = false;
  qrAmount: number = 0;
  isGeneratingQr: boolean = false;
  html5QrCode: any = null;
  isScanning: boolean = false;
  
  // ML Loan Prediction properties
  mlPredictionForm = {
    pan: '',
    loanType: '',
    amount: 0,
    tenure: 0
  };
  isPredicting: boolean = false;
  predictionResult: any = null;

  // Investments properties
  investments: any[] = [];
  isLoadingInvestments: boolean = false;
  showInvestmentForm: boolean = false;
  investmentForm = {
    investmentType: 'Mutual Fund',
    fundName: '',
    fundCategory: '',
    fundScheme: '',
    investmentAmount: 0,
    isSIP: false,
    sipAmount: 0,
    sipDuration: 0
  };
  isSubmittingInvestment: boolean = false;
  
  // Mutual Fund Foreclosure properties
  showMFForeclosureModal: boolean = false;
  selectedInvestmentForForeclosure: any = null;
  mfForeclosureDetails: any = null;
  isCalculatingMFForeclosure: boolean = false;
  isRequestingMFForeclosure: boolean = false;
  isRequestingMFForeclosureOtp: boolean = false;
  showMFForeclosureOtpModal: boolean = false;
  mfForeclosureOtpCode: string = '';
  mfForeclosureOtpSent: boolean = false;
  mfForeclosureRequestReason: string = '';
  mfForeclosureHistory: any[] = [];
  isLoadingMFForeclosureHistory: boolean = false;
  
  // Fixed Deposits properties
  fixedDeposits: any[] = [];
  isLoadingFDs: boolean = false;
  showFDForm: boolean = false;
  fdForm = {
    principalAmount: 0,
    interestRate: 7.5,
    tenure: 12,
    interestPayout: 'At Maturity'
  };
  isSubmittingFD: boolean = false;
  
  // FD Foreclosure properties
  showForeclosureModal: boolean = false;
  selectedFDForForeclosure: any = null;
  foreclosureDetails: any = null;
  isLoadingForeclosure: boolean = false;
  isProcessingForeclosure: boolean = false;
  isRequestingForeclosureOtp: boolean = false;
  showForeclosureOtpModal: boolean = false;
  foreclosureOtpCode: string = '';
  foreclosureOtpSent: boolean = false;
  
  // FD Top-up properties
  showTopUpModal: boolean = false;
  selectedFDForTopUp: any = null;
  topUpAmount: number = 0;
  isProcessingTopUp: boolean = false;
  
  // EMI Monitoring properties
  emis: any[] = [];
  filteredEmis: any[] = [];
  isLoadingEMIs: boolean = false;
  emiSummary: any = null;
  
  // EMI Filters
  emiFilters = {
    financialYear: '',
    status: 'all', // all, paid, pending, failed, overdue
    month: '',
    searchTerm: ''
  };
  
  // EMI Tabs
  activeEmiTab: 'all' | 'paid' | 'upcoming' = 'all';
  
  // Available financial years and months
  financialYears: string[] = [];
  months: { value: string, label: string }[] = [
    { value: '', label: 'All Months' },
    { value: '01', label: 'January' },
    { value: '02', label: 'February' },
    { value: '03', label: 'March' },
    { value: '04', label: 'April' },
    { value: '05', label: 'May' },
    { value: '06', label: 'June' },
    { value: '07', label: 'July' },
    { value: '08', label: 'August' },
    { value: '09', label: 'September' },
    { value: '10', label: 'October' },
    { value: '11', label: 'November' },
    { value: '12', label: 'December' }
  ];

  // Deposit Request properties
  depositForm = {
    amount: 0,
    method: 'Cash',
    referenceNumber: '',
    note: ''
  };
  depositRequests: any[] = [];
  isSubmittingDeposit: boolean = false;
  isLoadingDepositRequests: boolean = false;
  depositMessage: string = '';
  depositError: string = '';
  
  // Credit Card properties
  creditCards: any[] = [];
  selectedCreditCard: any = null;
  creditCardTransactions: any[] = [];
  creditCardBills: any[] = [];
  isLoadingCreditCards: boolean = false;
  showSetPinModal: boolean = false;
  pinForm = {
    currentPin: '',
    newPin: '',
    confirmPin: ''
  };
  billPaymentAmount: number = 0;
  selectedBill: any = null;
  showBillPaymentModal: boolean = false;
  
  // Session tracking properties
  loginTime: Date | null = null;
  logoutTime: Date | null = null;
  sessionDuration: string = '00:00:00';
  sessionTimer: any = null;
  
  // FastTag summary
  fasttagCount: number = 0;
  fasttagBalance: number = 0;
  fasttagStatusSummary: string = '';
  
  // Search functionality
  searchQuery: string = '';
  filteredMenuItems: any[] = [];

  // Dashboard widgets
  dashboardFilter: string = 'all';
  txnTab: string = 'all';

  recentTransactions: any[] = [];

  expenseBars = [
    { pct: 65, value: 12000, color: '#0d9488' },
    { pct: 45, value: 8500, color: '#14b8a6' },
    { pct: 80, value: 15000, color: '#f59e0b' },
    { pct: 30, value: 5600, color: '#6366f1' },
    { pct: 55, value: 10200, color: '#ec4899' }
  ];

  monthlySpending = [
    { label: 'Jan', pct: 40 }, { label: 'Feb', pct: 55 }, { label: 'Mar', pct: 72 },
    { label: 'Apr', pct: 35 }, { label: 'May', pct: 60 }, { label: 'Jun', pct: 48 },
    { label: 'Jul', pct: 80 }, { label: 'Aug', pct: 42 }, { label: 'Sep', pct: 65 },
    { label: 'Oct', pct: 50 }, { label: 'Nov', pct: 58 }, { label: 'Dec', pct: 45 }
  ];

  categoryBreakdown = [
    { name: 'Food', pct: 35, color: '#0d9488' },
    { name: 'Transport', pct: 20, color: '#f59e0b' },
    { name: 'Shopping', pct: 25, color: '#6366f1' },
    { name: 'Bills', pct: 15, color: '#ec4899' },
    { name: 'Other', pct: 5, color: '#94a3b8' }
  ];

  // Linked Account Switch
  linkedAccount: LinkedAccount | null = null;
  hasLinkedAccount = false;
  showPinModal = false;
  switchPin = '';
  confirmSwitchPin = '';
  isPinCreating = false;
  showVerifyPinModal = false;
  verifyPin = '';
  isVerifyingPin = false;
  linkedCurrentAccountDetails: any = null;

  // ── UPI Pay (Savings) ──────────────────────────────────────
  upiStatus: any = null;
  isLoadingUpiStatus: boolean = false;
  upiTransactions: any[] = [];
  isLoadingUpiTxns: boolean = false;
  isSettingUpUpi: boolean = false;
  upiSetupError: string = '';
  upiSetupSuccess: string = '';

  showUpiPinModal: boolean = false;
  upiPinMode: 'set' | 'forgot' = 'set';
  upiPinForm = { newPin: '', confirmPin: '', registeredPhone: '' };
  isSettingUpiPin: boolean = false;
  upiPinError: string = '';
  upiPinSuccess: string = '';

  showUpiSendModal: boolean = false;
  upiSendForm = { receiverUpiId: '', amount: 0, remark: '', transactionPin: '' };
  upiSendVerified: boolean = false;
  upiSendVerifiedName: string = '';
  upiSendVerifyError: string = '';
  isVerifyingUpiSend: boolean = false;
  isSubmittingUpiSend: boolean = false;
  upiSendError: string = '';
  upiSendSuccess: string = '';
  upiSendTab: string = 'send';

  // Real-time polling
  upiPollInterval: any = null;
  upiLastPollMs: number = 0;

  // ── Payment Links (Savings) ────────────────────────────────
  pendingPaymentLinks: any[] = [];
  allPaymentLinks: any[] = [];
  isLoadingLinks: boolean = false;
  showLinkPayModal: boolean = false;
  selectedLink: any = null;
  linkPayPin: string = '';
  isLinkPayProcessing: boolean = false;
  linkPaySuccess: boolean = false;
  linkPayResult: any = null;
  linkPayError: string = '';
  private linkPollInterval: any = null;


  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService,
    private fasttagService: FasttagService,
    private currentAccountService: CurrentAccountService
  ) {
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state && nav.extras.state['username']) {
      this.username = nav.extras.state['username'];
    }
  }

  ngOnInit() {
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      this.initializeSessionTracking();
      this.loadUserProfile();
      this.loadFasttag();
      this.loadRecentTransactions();
      this.startSessionTimer();
      this.checkLinkedAccount();
    }
  }

  loadFasttag() {
    if (!isPlatformBrowser(this.platformId)) return;
    const currentUserRaw = sessionStorage.getItem('currentUser');
    const user = currentUserRaw ? JSON.parse(currentUserRaw) : { id: 'guest' };
    this.fasttagService.listForUser(user.id || 'guest').subscribe({
      next: (tags) => {
        const list = tags || [];
        this.fasttagCount = list.length;
        this.fasttagBalance = list.reduce((s: number, t: any) => s + (t.balance || 0), 0);
        this.fasttagStatusSummary = list.map((t: any) => t.status).join(', ');
      },
      error: (err) => {
        console.error('Failed to load fasttags', err);
      }
    });
  }
  
  /**
   * Initialize session tracking - get login time from sessionStorage
   */
  initializeSessionTracking() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        if (user.loginTime) {
          this.loginTime = new Date(user.loginTime);
        } else {
          // If loginTime not found, set it now
          this.loginTime = new Date();
          user.loginTime = this.loginTime.toISOString();
          sessionStorage.setItem('currentUser', JSON.stringify(user));
        }
      } catch (e) {
        console.error('Error parsing user session:', e);
        this.loginTime = new Date();
      }
    } else {
      // Fallback: set login time to now
      this.loginTime = new Date();
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
   * Format date and time for display
   */
  formatDateTime(date: Date | null): string {
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
  
  // Load investments, FDs, and EMIs when feature is selected
  loadFeatureData() {
    if (this.selectedFeature === 'investments') {
      this.loadInvestments();
    } else if (this.selectedFeature === 'fixed-deposits') {
      this.loadFixedDeposits();
    } else if (this.selectedFeature === 'emi-monitoring') {
      this.resetEMIFilters(); // Initialize filters
      this.loadEMIs();
    } else if (this.selectedFeature === 'deposit-request') {
       this.loadDepositRequests();
    } else if (this.selectedFeature === 'accounts') {
      // Accounts view - can show account details
      this.loadUserProfile();
    } else if (this.selectedFeature === 'credit-card') {
      this.loadCreditCards();
    } else if (this.selectedFeature === 'bill-payment') {
      // Bill payment component will handle its own data loading
    } else if (this.selectedFeature === 'upi-pay') {
      this.loadUpiStatus();
      this.loadUpiTransactions();
      this.loadUserPaymentLinks();
      this.startUpiPolling();
    } else {
      this.stopUpiPolling();
    }
  }
  
  selectFeature(feature: string | null) {
    this.selectedFeature = feature;
    if (feature === 'deposit-request') {
      this.depositMessage = '';
      this.depositError = '';
    }
    if (feature) {
      this.loadFeatureData();
    }
  }

  getGreeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'morning';
    if (h < 17) return 'afternoon';
    return 'evening';
  }

  getFilteredTransactions(): any[] {
    if (this.txnTab === 'all') return this.recentTransactions;
    return this.recentTransactions.filter((t: any) => t.type === this.txnTab.toUpperCase());
  }

  loadRecentTransactions() {
    if (!isPlatformBrowser(this.platformId)) return;
    const currentUser = sessionStorage.getItem('currentUser');
    if (!currentUser) return;
    try {
      const user = JSON.parse(currentUser);
      const accNo = user.accountNumber;
      if (!accNo) return;
      this.http.get<any[]>(`${environment.apiBaseUrl}/api/transactions/account/${accNo}`)
        .pipe(timeout(5000), catchError(() => of([])))
        .subscribe((txns: any[]) => {
          this.recentTransactions = (txns || []).slice(0, 10);
        });
    } catch (e) { /* silent */ }
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get user session from sessionStorage FIRST (fast, no API call)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        // Set basic info immediately from session (no loading needed)
        this.username = user.name || 'User';
        this.userAccountNumber = user.accountNumber || 'ACC001';
        // Use cached balance if available
        const cachedBalance = localStorage.getItem(`balance_${this.userAccountNumber}`);
        if (cachedBalance) {
          this.currentBalance = parseFloat(cachedBalance) || 0;
        }
        
        // Show dashboard immediately, load fresh data in background
        this.loading = false;
        
        // Load user profile and balance from MySQL database in background (non-blocking)
        setTimeout(() => {
          this.loadUserDataFromMySQL(user.id);
        }, 100);
      } catch (e) {
        console.error('Error parsing user session:', e);
        this.setDefaultValues();
      }
    } else {
      // Try to get user from localStorage as fallback
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        try {
          const profile = JSON.parse(savedProfile);
          this.username = profile.name || 'User';
          this.userAccountNumber = profile.accountNumber || 'ACC001';
          this.currentBalance = profile.balance || 0;
          this.loading = false;
          console.log('Using fallback profile from localStorage');
        } catch (e) {
          this.setDefaultValues();
        }
      } else {
        this.setDefaultValues();
      }
    }
  }

  setDefaultValues() {
    this.username = 'User';
    this.userAccountNumber = 'ACC001';
    this.currentBalance = 0;
    this.loading = false;
    console.log('Using default values - no session found');
  }

  /**
   * Show a temporary error message that auto-dismisses
   */
  showTemporaryError(message: string, duration: number = 5000) {
    // Clear any existing error timeout
    if (this.errorTimeout) {
      clearTimeout(this.errorTimeout);
    }
    
    this.error = message;
    
    // Auto-dismiss after duration
    this.errorTimeout = setTimeout(() => {
      this.error = '';
      this.errorTimeout = null;
    }, duration);
  }

  loadUserDataFromMySQL(userId: string) {
    // Load user data from MySQL database (background refresh, non-blocking)
    // Ensure ID is a number
    const userIdNum = typeof userId === 'number' ? userId : Number(userId);
    if (isNaN(userIdNum)) {
      console.error('Invalid user ID:', userId);
      return;
    }
    
    // Don't show loading state - this is background refresh
    // Only show error if it's critical, otherwise silently fail
    
    this.http.get(`${environment.apiBaseUrl}/api/users/${userIdNum}`)
      .pipe(
        timeout(5000), // 5 second timeout for the request (shorter for background calls)
        catchError(err => {
          console.warn('Background refresh failed (non-critical):', err);
          // Show a subtle, dismissible warning only if backend is completely down
          if (err.status === 0) {
            this.showTemporaryError('Backend server is unavailable. Using cached data. Some features may be limited.', 5000);
          }
          return of(null); // Return empty observable to prevent further errors
        })
      )
      .subscribe({
        next: (userData: any) => {
          if (!userData) return; // If error occurred, userData will be null
          
          console.log('User data refreshed from MySQL:', userData);
          
          // Update profile silently in background
          this.userProfile = userData;
          this.username = userData.account?.name || userData.username || this.username;
          this.userAccountNumber = userData.accountNumber || this.userAccountNumber;
          
          // Load current balance from MySQL (also in background)
          this.loadCurrentBalanceFromMySQL();
        }
      });
  }

  loadCurrentBalanceFromMySQL() {
    if (!this.userAccountNumber) {
      return;
    }
    
    // Load current balance from MySQL database (background refresh)
    this.http.get(`${environment.apiBaseUrl}/api/accounts/balance/${this.userAccountNumber}`)
      .pipe(
        timeout(5000), // 5 second timeout
        catchError(err => {
          console.warn('Balance refresh failed (non-critical):', err);
          // Keep existing balance, don't update
          return of({ balance: this.currentBalance }); // Return current balance
        })
      )
      .subscribe({
        next: (balanceData: any) => {
          if (balanceData?.balance !== undefined) {
            const newBalance = balanceData.balance || this.userProfile?.account?.balance || this.currentBalance;
            this.currentBalance = newBalance;
            // Cache the balance for offline use
            localStorage.setItem(`balance_${this.userAccountNumber}`, newBalance.toString());
            console.log('Balance refreshed from MySQL:', newBalance);
          }
        }
      });
  }


  goTo(page: string) {
    // ✅ use the `page` parameter, not Node path
    this.router.navigate([`/website/${page}`]);
  }

  goToInsurance() {
    this.router.navigate(['/website/insurance']);
  }

  goToProfile() {
    this.router.navigate(['/website/profile']);
  }

  logout() {
    // Store logout timestamp
    this.logoutTime = new Date();
    
    // Update sessionStorage with logout time
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        user.logoutTime = this.logoutTime.toISOString();
        user.sessionDuration = this.sessionDuration;
        sessionStorage.setItem('currentUser', JSON.stringify(user));
        
        // Send logout data to backend
        this.http.post(`${environment.apiBaseUrl}/api/session-history/logout`, {
          userId: user.id,
          userType: 'USER',
          sessionDuration: this.sessionDuration
        }).subscribe({
          next: (response: any) => {
            console.log('Logout recorded successfully:', response);
          },
          error: (err) => {
            console.error('Error recording logout:', err);
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
    this.router.navigate(['/website/logout']); // Show logout thank you page
  }

  getFeatureTitle(): string {
    const titles: { [key: string]: string } = {
      'transferfunds': 'Transfer Funds',
      'card': 'Credit Cards',
      'accounts': 'Accounts',
      'transaction': 'Transactions',
      'kycupdate': 'KYC Update',
      'loan': 'Loans',
      'cheque': 'Cheque Management',
      'goldloan': 'Gold Loan',
      'subsidy-claim': 'Subsidy Claim',
      'ai-assistant': 'AI Financial Assistant',
      'investments': 'Investments (Mutual Funds)',
      'fixed-deposits': 'Fixed Deposits',
      'emi-monitoring': 'EMI Monitoring',
      'deposit-request': 'Deposit Request',
      'beneficiaries': 'Beneficiaries',
      'addon-services': 'Add-on Services',
      'bill-payment': 'Bill Payment',
      'bill-payments': 'Bill Payments',
      'manage-tax': 'Manage Tax',
      'customer-service': 'Customer Service',
      'insurance': 'Insurance',
      'upi-pay': 'UPI Pay'
    };
    return titles[this.selectedFeature || ''] || '';
  }

  getFeatureDescription(): string {
    const descriptions: { [key: string]: string } = {
      'transferfunds': 'Send money to other accounts securely and quickly.',
      'card': 'Manage your debit and credit cards, view statements, and set limits.',
      'accounts': 'View your account details, balance, and account statements.',
      'transaction': 'View your complete transaction history and download statements.',
      'kycupdate': 'Update your personal information and verify your identity.',
      'loan': 'Apply for personal loans, check eligibility, and manage existing loans.',
      'cheque': 'Create, view, download, and cancel cheque leaves for your account.',
      'goldloan': 'Apply for gold loan against your gold. Get 75% of gold value as loan.',
      'subsidy-claim': 'Claim 3 years of interest subsidy on your approved education loans.',
      'ai-assistant': 'Get AI-powered insights on your spending, loan suggestions, and charge alerts.',
      'ml-loan-prediction': 'Get instant AI-powered loan approval prediction based on your PAN card and financial profile.',
      'investments': 'Open and manage mutual fund investments. Track your portfolio performance.',
      'fixed-deposits': 'Open fixed deposits and earn guaranteed returns. View maturity details.',
      'emi-monitoring': 'Monitor all your EMI payments. View payment history and upcoming dues.',
      'deposit-request': 'Submit a deposit slip to the branch/admin and track status.',
      'beneficiaries': 'Add and manage your beneficiaries for quick money transfers.',
      'addon-services': 'Access additional banking services and features.',
      'bill-payment': 'Pay your mobile and WiFi bills using your Neo Bank credit card.',
      'bill-payments': 'Pay your utility bills, credit card bills, and other payments online.',
      'manage-tax': 'Manage your tax documents, TDS certificates, and tax-related information.',
      'customer-service': 'Get help and support from our customer service team.'
      ,
      'insurance': 'Apply and manage your insurance policies.'
      ,
      'upi-pay': 'Send & receive money instantly using your UPI ID. Set PIN, view transactions.'
    };
    return descriptions[this.selectedFeature || ''] || '';
  }

  getFeatureIcon(): string {
    const icons: { [key: string]: string } = {
      'transferfunds': '💸',
      'card': '💳',
      'accounts': '📄',
      'transaction': '🔄',
      'kycupdate': '📑',
      'loan': '💰',
      'cheque': '📝',
      'goldloan': '🥇',
      'subsidy-claim': '🎓',
      'ai-assistant': '🤖',
      'ml-loan-prediction': '🤖',
      'investments': '📈',
      'fixed-deposits': '🏦',
      'emi-monitoring': '📊',
      'deposit-request': '🏧',
      'beneficiaries': '👤',
      'addon-services': '⚙️',
      'bill-payment': '🧾',
      'bill-payments': '🧾',
      'manage-tax': '📋',
      'customer-service': '🎧',
      'insurance': '🛡️',
      'upi-pay': '📲'
    };
    return icons[this.selectedFeature || ''] || '';
  }

  // Refresh user data (useful for real-time updates)
  refreshUserData() {
    console.log('Refreshing user data...');
    this.loadUserProfile();
  }

  // QR Code Methods
  generateQrCode(amount?: number) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    if (!this.userAccountNumber) {
      console.error('User account number is missing');
      this.alertService.transferError('Account number not found');
      return;
    }

    console.log('Generating QR code for account:', this.userAccountNumber, 'Amount:', amount);
    this.isGeneratingQr = true;
    
    // URL encode account number to handle special characters
    const encodedAccountNumber = encodeURIComponent(this.userAccountNumber);
    const url = amount && amount > 0 
      ? `${environment.apiBaseUrl}/qrcode/upi/receive/${encodedAccountNumber}/${amount}`
      : `${environment.apiBaseUrl}/qrcode/upi/receive/${encodedAccountNumber}`;

    console.log('Requesting QR code from URL:', url);

    this.http.get(url).subscribe({
      next: (response: any) => {
        if (response.qrCode) {
          this.qrCodeImage = response.qrCode;
          this.upiId = response.upiId;
          this.qrAmount = response.amount || 0;
          this.showQrCode = true;
          this.isGeneratingQr = false;
        } else {
          console.error('QR code not in response:', response);
          const errorMsg = response.error || 'Failed to generate QR code';
          this.alertService.transferError(errorMsg);
          this.isGeneratingQr = false;
        }
      },
      error: (err: any) => {
        console.error('Error generating QR code:', err);
        const errorMsg = err.error?.error || err.message || 'Failed to generate QR code. Please check your account number.';
        this.alertService.transferError(errorMsg);
        this.isGeneratingQr = false;
      }
    });
  }

  closeQrCode() {
    this.showQrCode = false;
    this.qrCodeImage = '';
    this.qrAmount = 0;
  }

  startQrScanner() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.showQrScanner = true;
    this.isScanning = true;
    
    // Initialize scanner after view update
    setTimeout(async () => {
      try {
        // Dynamic import to avoid SSR issues
        const { Html5Qrcode } = await import('html5-qrcode');
        const scannerId = 'qr-reader';
        this.html5QrCode = new Html5Qrcode(scannerId);
        
        await this.html5QrCode.start(
          { facingMode: 'environment' },
          {
            fps: 10,
            qrbox: { width: 250, height: 250 }
          },
          (decodedText: string) => {
            this.onQrCodeScanned(decodedText);
          },
          (errorMessage: string) => {
            // Ignore scanning errors
          }
        );
      } catch (err: any) {
        console.error('Error starting QR scanner:', err);
        this.alertService.transferError('Failed to start camera. Please check permissions.');
        this.stopQrScanner();
      }
    }, 100);
  }

  stopQrScanner() {
    if (this.html5QrCode) {
      this.html5QrCode.stop().then(() => {
        this.html5QrCode?.clear();
        this.html5QrCode = null;
      }).catch((err: any) => {
        console.error('Error stopping QR scanner:', err);
      });
    }
    this.showQrScanner = false;
    this.isScanning = false;
  }

  onQrCodeScanned(decodedText: string) {
    console.log('QR Code scanned:', decodedText);
    
    // Check if it's a UPI payment QR code
    if (decodedText.startsWith('upi://pay')) {
      try {
        // Parse UPI URL - handle both URL and string parsing
        let upiId: string | null = null;
        let name: string | null = null;
        let amount: string | null = null;
        
        try {
          // Try using URL constructor (works for full URLs)
          const url = new URL(decodedText);
          upiId = url.searchParams.get('pa');
          name = url.searchParams.get('pn');
          amount = url.searchParams.get('am');
        } catch (e) {
          // Fallback: manual parsing for upi:// protocol
          const params = decodedText.split('?')[1];
          if (params) {
            const paramPairs = params.split('&');
            paramPairs.forEach(pair => {
              const [key, value] = pair.split('=');
              if (key === 'pa') upiId = decodeURIComponent(value);
              if (key === 'pn') name = decodeURIComponent(value);
              if (key === 'am') amount = value;
            });
          }
        }
        
        // Stop scanner
        this.stopQrScanner();
        
        // Navigate to transfer funds with pre-filled data
        this.router.navigate(['/website/transferfunds'], {
          state: {
            scannedUpiId: upiId,
            scannedName: name,
            scannedAmount: amount ? parseFloat(amount) : null
          }
        });
      } catch (error) {
        console.error('Error parsing QR code:', error);
        this.alertService.transferError('Error parsing QR code. Please try again.');
        this.stopQrScanner();
      }
    } else {
      this.alertService.transferError('Invalid QR code. Please scan a UPI payment QR code.');
    }
  }

  // Credit Card Methods
  loadCreditCards() {
    if (!this.userAccountNumber) return;
    this.isLoadingCreditCards = true;
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/account/${this.userAccountNumber}`).subscribe({
      next: (cards: any) => {
        this.creditCards = Array.isArray(cards) ? cards : [];
        if (this.creditCards.length > 0) {
          this.selectedCreditCard = this.creditCards[0];
          this.loadCreditCardDetails(this.selectedCreditCard.id);
        }
        this.isLoadingCreditCards = false;
      },
      error: (err: any) => {
        console.error('Error loading credit cards:', err);
        this.creditCards = [];
        this.isLoadingCreditCards = false;
      }
    });
  }

  loadCreditCardDetails(cardId: number) {
    this.loadCreditCardTransactions(cardId);
    this.loadCreditCardBills(cardId);
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

  setPin() {
    if (!this.selectedCreditCard) return;
    if (this.pinForm.newPin !== this.pinForm.confirmPin) {
      alert('PINs do not match');
      return;
    }
    if (this.pinForm.newPin.length !== 4) {
      alert('PIN must be 4 digits');
      return;
    }
    
    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCreditCard.id}/set-pin`, { pin: this.pinForm.newPin }).subscribe({
      next: () => {
        alert('PIN set successfully');
        this.showSetPinModal = false;
        this.pinForm = { currentPin: '', newPin: '', confirmPin: '' };
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error setting PIN:', err);
        alert('Failed to set PIN');
      }
    });
  }

  openBillPayment(bill: any) {
    this.selectedBill = bill;
    this.billPaymentAmount = bill.minimumDue || bill.totalAmount;
    this.showBillPaymentModal = true;
  }

  payBill() {
    if (!this.selectedBill || !this.billPaymentAmount) {
      alert('Please enter payment amount');
      return;
    }
    
    this.http.post(`${environment.apiBaseUrl}/api/credit-cards/bills/${this.selectedBill.id}/pay?amount=${this.billPaymentAmount}`, {}).subscribe({
      next: (bill: any) => {
        alert('Bill paid successfully');
        this.showBillPaymentModal = false;
        this.selectedBill = null;
        this.billPaymentAmount = 0;
        this.loadCreditCardBills(this.selectedCreditCard.id);
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error paying bill:', err);
        alert('Failed to pay bill');
      }
    });
  }

  getStatement() {
    if (!this.selectedCreditCard) return;
    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCreditCard.id}/statement?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`).subscribe({
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

  // ════════════════════════════════════════════════════════════
  //  SAVINGS UPI METHODS
  // ════════════════════════════════════════════════════════════

  loadUpiStatus() {
    if (!this.userAccountNumber) return;
    this.isLoadingUpiStatus = true;
    this.http.get(`${environment.apiBaseUrl}/api/savings-upi/status/${this.userAccountNumber}`).pipe(
      timeout(8000), catchError(() => of(null))
    ).subscribe({
      next: (res: any) => {
        this.upiStatus = res || null;
        this.isLoadingUpiStatus = false;
      },
      error: () => { this.isLoadingUpiStatus = false; }
    });
  }

  loadUpiTransactions() {
    if (!this.userAccountNumber) return;
    this.isLoadingUpiTxns = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/savings-upi/transactions/${this.userAccountNumber}`).pipe(
      timeout(8000), catchError(() => of([]))
    ).subscribe({
      next: (txns: any[]) => {
        this.upiTransactions = txns || [];
        this.isLoadingUpiTxns = false;
      },
      error: () => { this.isLoadingUpiTxns = false; }
    });
  }

  setupUpi() {
    if (!this.userAccountNumber) return;
    this.isSettingUpUpi = true;
    this.upiSetupError = '';
    this.upiSetupSuccess = '';
    this.http.post(`${environment.apiBaseUrl}/api/savings-upi/setup/${this.userAccountNumber}`, {}).subscribe({
      next: (res: any) => {
        this.isSettingUpUpi = false;
        if (res?.success) {
          this.upiSetupSuccess = `UPI ID created: ${res.upiId}`;
          this.loadUpiStatus();
        } else {
          this.upiSetupError = res?.message || 'Setup failed';
        }
      },
      error: (err: any) => {
        this.isSettingUpUpi = false;
        this.upiSetupError = err?.error?.message || 'Failed to set up UPI';
      }
    });
  }

  toggleUpi() {
    if (!this.userAccountNumber) return;
    this.http.put(`${environment.apiBaseUrl}/api/savings-upi/toggle/${this.userAccountNumber}`, {}).subscribe({
      next: () => this.loadUpiStatus(),
      error: (err: any) => alert(err?.error?.message || 'Failed to toggle UPI')
    });
  }

  openUpiPinModal(mode: 'set' | 'forgot' = 'set') {
    this.showUpiPinModal = true;
    this.upiPinMode = mode;
    this.upiPinForm = { newPin: '', confirmPin: '', registeredPhone: '' };
    this.upiPinError = '';
    this.upiPinSuccess = '';
  }

  closeUpiPinModal() { this.showUpiPinModal = false; }

  submitUpiPin() {
    if (!this.upiPinForm.newPin.match(/^[0-9]{6}$/)) {
      this.upiPinError = 'PIN must be exactly 6 digits'; return;
    }
    if (this.upiPinForm.newPin !== this.upiPinForm.confirmPin) {
      this.upiPinError = 'PINs do not match'; return;
    }
    this.isSettingUpiPin = true;
    this.upiPinError = '';

    if (this.upiPinMode === 'forgot') {
      if (!this.upiPinForm.registeredPhone) {
        this.upiPinError = 'Registered phone number is required'; this.isSettingUpiPin = false; return;
      }
      this.http.post(`${environment.apiBaseUrl}/api/savings-upi/forgot-pin/${this.userAccountNumber}`, {
        registeredPhone: this.upiPinForm.registeredPhone,
        newPin: this.upiPinForm.newPin
      }).subscribe({
        next: (res: any) => {
          this.isSettingUpiPin = false;
          if (res?.success) {
            this.upiPinSuccess = 'UPI PIN reset successfully!';
            this.loadUpiStatus();
            setTimeout(() => this.closeUpiPinModal(), 1500);
          } else {
            this.upiPinError = res?.message || 'Failed to reset PIN';
          }
        },
        error: (err: any) => {
          this.isSettingUpiPin = false;
          this.upiPinError = err?.error?.message || 'Phone number did not match. Please try again.';
        }
      });
    } else {
      this.http.post(`${environment.apiBaseUrl}/api/savings-upi/set-pin/${this.userAccountNumber}`, { pin: this.upiPinForm.newPin }).subscribe({
        next: (res: any) => {
          this.isSettingUpiPin = false;
          if (res?.success) {
            this.upiPinSuccess = 'UPI PIN set successfully!';
            this.loadUpiStatus();
            setTimeout(() => this.closeUpiPinModal(), 1500);
          } else {
            this.upiPinError = res?.message || 'Failed to set PIN';
          }
        },
        error: (err: any) => {
          this.isSettingUpiPin = false;
          this.upiPinError = err?.error?.message || 'Failed to set PIN';
        }
      });
    }
  }

  openUpiSendModal() {
    if (!this.upiStatus?.pinSet) {
      alert('Please set your 6-digit UPI PIN first before making payments.');
      this.openUpiPinModal('set');
      return;
    }
    this.showUpiSendModal = true;
    this.upiSendForm = { receiverUpiId: '', amount: 0, remark: '', transactionPin: '' };
    this.upiSendVerified = false;
    this.upiSendVerifiedName = '';
    this.upiSendVerifyError = '';
    this.upiSendError = '';
    this.upiSendSuccess = '';
  }

  closeUpiSendModal() { this.showUpiSendModal = false; }

  verifyUpiSendId() {
    if (!this.upiSendForm.receiverUpiId) return;
    this.isVerifyingUpiSend = true;
    this.upiSendVerified = false;
    this.upiSendVerifyError = '';
    const encoded = encodeURIComponent(this.upiSendForm.receiverUpiId);
    this.http.get(`${environment.apiBaseUrl}/api/savings-upi/verify/${encoded}`).subscribe({
      next: (res: any) => {
        this.isVerifyingUpiSend = false;
        if (res?.verified) {
          this.upiSendVerified = true;
          this.upiSendVerifiedName = res.name || res.accountHolderName || 'Account Holder';
        } else {
          this.upiSendVerifyError = 'UPI ID not found or inactive';
        }
      },
      error: () => {
        this.isVerifyingUpiSend = false;
        this.upiSendVerifyError = 'Could not verify UPI ID';
      }
    });
  }

  submitUpiSend() {
    if (!this.upiSendVerified || !this.upiSendForm.amount || !this.upiSendForm.transactionPin) return;
    this.isSubmittingUpiSend = true;
    this.upiSendError = '';
    this.upiSendSuccess = '';
    const sentAmount = this.upiSendForm.amount;
    this.http.post(`${environment.apiBaseUrl}/api/savings-upi/send/${this.userAccountNumber}`, {
      receiverUpiId: this.upiSendForm.receiverUpiId,
      amount: this.upiSendForm.amount,
      remark: this.upiSendForm.remark,
      transactionPin: this.upiSendForm.transactionPin
    }).subscribe({
      next: (res: any) => {
        this.isSubmittingUpiSend = false;
        if (res?.success) {
          // Reset form
          this.upiSendForm = { receiverUpiId: '', amount: 0, remark: '', transactionPin: '' };
          this.upiSendVerified = false;
          this.upiSendVerifiedName = '';
          this.upiSendVerifyError = '';
          // Switch to history tab so the new transaction is visible
          this.upiSendTab = 'history';
          // Refresh transaction list and both balance displays
          this.loadUpiTransactions();
          this.loadUpiStatus();
          this.loadCurrentBalanceFromMySQL(); // updates main header balance
          this.upiSendSuccess = `₹${sentAmount} sent successfully! Ref: ${res.transactionRef}`;
          // Clear success message after 4 seconds
          setTimeout(() => { this.upiSendSuccess = ''; }, 4000);
        } else {
          this.upiSendError = res?.error || res?.message || 'Payment failed';
        }
      },
      error: (err: any) => {
        this.isSubmittingUpiSend = false;
        this.upiSendError = err?.error?.error || err?.error?.message || 'Payment failed. Please try again.';
      }
    });
  }

  startUpiPolling() {
    this.stopUpiPolling();
    this.upiLastPollMs = Date.now() - 15000;
    this.upiPollInterval = setInterval(() => this.pollUpiTransactions(), 5000);
  }

  stopUpiPolling() {
    if (this.upiPollInterval) {
      clearInterval(this.upiPollInterval);
      this.upiPollInterval = null;
    }
  }

  // ─── Payment Links (Savings Dashboard) ────────────────────────────────────
  loadUserPaymentLinks() {
    if (!this.upiStatus?.upiId) return;
    this.isLoadingLinks = true;
    const upiId = encodeURIComponent(this.upiStatus.upiId);
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/payment-gateway/payment-links/customer/${upiId}`)
      .subscribe({
        next: (data: any[]) => {
          this.allPaymentLinks = data || [];
          this.pendingPaymentLinks = this.allPaymentLinks.filter((l: any) => l.status === 'PENDING');
          this.isLoadingLinks = false;
          if (this.pendingPaymentLinks.length > 0 && !this.linkPollInterval) {
            this.linkPollInterval = setInterval(() => this.loadUserPaymentLinks(), 5000);
          } else if (this.pendingPaymentLinks.length === 0 && this.linkPollInterval) {
            clearInterval(this.linkPollInterval);
            this.linkPollInterval = null;
          }
        },
        error: () => { this.isLoadingLinks = false; }
      });
  }

  openLinkPayModal(link: any) {
    this.selectedLink = link;
    this.linkPayPin = '';
    this.linkPaySuccess = false;
    this.linkPayResult = null;
    this.linkPayError = '';
    this.showLinkPayModal = true;
  }

  payLinkWithPin() {
    if (!this.linkPayPin || this.linkPayPin.length < 4) return;
    this.isLinkPayProcessing = true;
    this.http.post<any>(
      `${environment.apiBaseUrl}/api/payment-gateway/payment-links/${this.selectedLink.linkToken}/pay`,
      { payerAccountNumber: this.userAccountNumber, transactionPin: this.linkPayPin }
    ).subscribe({
      next: (res: any) => {
        this.isLinkPayProcessing = false;
        if (res.success) {
          this.linkPaySuccess = true;
          this.linkPayResult = res;
          this.loadCurrentBalanceFromMySQL();
          this.loadUpiTransactions();
          this.loadUserPaymentLinks();
          this.loadRecentTransactions();
        } else {
          this.linkPayError = res.error || 'Payment failed';
        }
      },
      error: (err: any) => {
        this.isLinkPayProcessing = false;
        this.linkPayError = err?.error?.error || 'Payment failed';
      }
    });
  }

  closeLinkPayModal() {
    this.showLinkPayModal = false;
    this.selectedLink = null;
    this.linkPayPin = '';
    this.linkPaySuccess = false;
    this.linkPayError = '';
  }

  printInvoice() {
    const el = document.getElementById('pg-invoice-print');
    if (!el) return;
    const w = window.open('', '_blank', 'width=800,height=900');
    if (!w) return;
    w.document.write(`<html><head><title>Payment Invoice</title><style>
      body{font-family:'Segoe UI',Arial,sans-serif;padding:40px;color:#1e293b;max-width:700px;margin:0 auto;}
      .inv-header{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:3px solid #1e3a5f;padding-bottom:16px;margin-bottom:20px;}
      .inv-brand{font-size:24px;font-weight:800;color:#1e3a5f;}
      .inv-badge{background:#dcfce7;color:#16a34a;padding:6px 16px;border-radius:20px;font-weight:700;font-size:13px;}
      .inv-section{margin-bottom:18px;}
      .inv-section h4{margin:0 0 6px;color:#64748b;font-size:12px;text-transform:uppercase;letter-spacing:1px;}
      .inv-section p{margin:2px 0;font-size:14px;}
      .inv-table{width:100%;border-collapse:collapse;margin:16px 0;}
      .inv-table th,.inv-table td{padding:10px 14px;text-align:left;border-bottom:1px solid #e2e8f0;font-size:14px;}
      .inv-table th{background:#f1f5f9;font-weight:600;color:#475569;}
      .inv-total{font-weight:700;font-size:16px;color:#1e3a5f;}
      .inv-seal{text-align:center;margin-top:30px;padding-top:20px;border-top:2px dashed #cbd5e1;}
      .inv-seal-circle{width:90px;height:90px;border:3px solid #16a34a;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;margin-bottom:8px;}
      .inv-seal-text{font-size:11px;color:#16a34a;font-weight:800;text-transform:uppercase;line-height:1.3;}
      .inv-footer{text-align:center;margin-top:24px;font-size:11px;color:#94a3b8;}
      @media print{body{padding:20px;}}
    </style></head><body>${el.innerHTML}</body></html>`);
    w.document.close();
    w.print();
  }

  pollUpiTransactions() {
    if (!this.userAccountNumber) return;
    const after = this.upiLastPollMs;
    this.upiLastPollMs = Date.now();
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/savings-upi/latest-transactions/${this.userAccountNumber}?afterEpochMs=${after}`)
      .pipe(catchError(() => of([])))
      .subscribe((newTxns: any[]) => {
        if (newTxns && newTxns.length > 0) {
          // Merge new transactions, avoid duplicates by transactionRef
          const existingRefs = new Set(this.upiTransactions.map((t: any) => t.transactionRef));
          const fresh = newTxns.filter((t: any) => !existingRefs.has(t.transactionRef));
          if (fresh.length > 0) {
            this.upiTransactions = [...fresh, ...this.upiTransactions];
            this.loadUpiStatus();            // refresh UPI tab balance
            this.loadCurrentBalanceFromMySQL(); // refresh main header balance
            // Check if any are incoming (receiver) transactions
            const incoming = fresh.filter((t: any) => t.receiverUpiId === this.upiStatus?.upiId);
            if (incoming.length > 0) {
              // Switch to history tab so receiver sees incoming payment
              this.upiSendTab = 'history';
            }
          }
        }
      });
  }

  ngOnDestroy() {
    this.stopQrScanner();
    this.stopUpiPolling();
    if (this.linkPollInterval) {
      clearInterval(this.linkPollInterval);
      this.linkPollInterval = null;
    }
    // Clear any pending error timeout
    if (this.errorTimeout) {
      clearTimeout(this.errorTimeout);
      this.errorTimeout = null;
    }
    // Clear session timer
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = null;
    }
  }
  
  // ML Loan Prediction Methods
  isFormValid(): boolean {
    return !!(this.mlPredictionForm.pan && 
              this.mlPredictionForm.loanType && 
              this.mlPredictionForm.amount > 0 && 
              this.mlPredictionForm.tenure > 0 &&
              this.userAccountNumber);
  }
  
  predictLoanApproval() {
    if (!this.isFormValid()) {
      this.alertService.error('Invalid Form', 'Please fill all required fields');
      return;
    }
    
    this.isPredicting = true;
    this.predictionResult = null;
    
    const request = {
      pan: this.mlPredictionForm.pan,
      loanType: this.mlPredictionForm.loanType,
      requestedAmount: this.mlPredictionForm.amount,
      tenure: this.mlPredictionForm.tenure,
      accountNumber: this.userAccountNumber
    };
    
    this.http.post(`${environment.apiBaseUrl}/api/loans/predict-approval`, request).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.predictionResult = response;
          this.alertService.success('Prediction Complete', 
            `Loan approval prediction: ${response.predictionResult} (${response.approvalPercentage}% probability)`);
        } else {
          this.alertService.error('Prediction Failed', response.message || 'Failed to predict loan approval');
        }
        this.isPredicting = false;
      },
      error: (err: any) => {
        console.error('Error predicting loan approval:', err);
        this.alertService.error('Prediction Error', 
          err.error?.message || 'Failed to predict loan approval. Please try again.');
        this.isPredicting = false;
      }
    });
  }
  
  clearPrediction() {
    this.predictionResult = null;
    this.mlPredictionForm = {
      pan: '',
      loanType: '',
      amount: 0,
      tenure: 0
    };
  }
  
  getStatusIcon(status: string): string {
    switch(status?.toLowerCase()) {
      case 'approved': return '✅';
      case 'rejected': return '❌';
      case 'pending review': return '⏳';
      default: return '📊';
    }
  }
  
  getPredictionResultClass(status: string): string {
    if (!status) return '';
    return status.toLowerCase().replace(/\s+/g, '-');
  }
  
  // Expose Math to template
  Math = Math;
  
  // Investment Methods
  loadInvestments() {
    if (!this.userAccountNumber) return;
    
    this.isLoadingInvestments = true;
    this.http.get(`${environment.apiBaseUrl}/api/investments/account/${this.userAccountNumber}`).subscribe({
      next: (investments: any) => {
        this.investments = investments || [];
        this.isLoadingInvestments = false;
        this.loadMFForeclosureHistory();
      },
      error: (err: any) => {
        console.error('Error loading investments:', err);
        this.alertService.error('Error', 'Failed to load investments');
        this.isLoadingInvestments = false;
      }
    });
  }
  
  openInvestmentForm() {
    this.showInvestmentForm = true;
    this.investmentForm = {
      investmentType: 'Mutual Fund',
      fundName: '',
      fundCategory: '',
      fundScheme: '',
      investmentAmount: 0,
      isSIP: false,
      sipAmount: 0,
      sipDuration: 0
    };
  }
  
  closeInvestmentForm() {
    this.showInvestmentForm = false;
  }
  
  submitInvestment() {
    if (!this.userAccountNumber) {
      this.alertService.error('Error', 'Account number not found');
      return;
    }
    
    if (this.investmentForm.investmentAmount <= 0) {
      this.alertService.error('Validation Error', 'Investment amount must be greater than 0');
      return;
    }
    
    this.isSubmittingInvestment = true;
    const investmentData = {
      accountNumber: this.userAccountNumber,
      investmentType: this.investmentForm.investmentType,
      fundName: this.investmentForm.fundName,
      fundCategory: this.investmentForm.fundCategory,
      fundScheme: this.investmentForm.fundScheme,
      investmentAmount: this.investmentForm.investmentAmount,
      isSIP: this.investmentForm.isSIP,
      sipAmount: this.investmentForm.isSIP ? this.investmentForm.sipAmount : null,
      sipDuration: this.investmentForm.isSIP ? this.investmentForm.sipDuration : null
    };
    
    this.http.post(`${environment.apiBaseUrl}/api/investments`, investmentData).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Investment application submitted successfully');
          this.closeInvestmentForm();
          this.loadInvestments();
          this.loadCurrentBalanceFromMySQL();
        } else {
          this.alertService.error('Error', response.message || 'Failed to submit investment');
        }
        this.isSubmittingInvestment = false;
      },
      error: (err: any) => {
        console.error('Error submitting investment:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to submit investment');
        this.isSubmittingInvestment = false;
      }
    });
  }
  
  getInvestmentStatusClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'APPROVED': case 'ACTIVE': return 'status-approved';
      case 'PENDING': return 'status-pending';
      case 'REJECTED': return 'status-rejected';
      case 'MATURED': case 'CLOSED': return 'status-closed';
      default: return 'status-default';
    }
  }
  
  // Mutual Fund Foreclosure Methods
  openMFForeclosureModal(investment: any) {
    this.selectedInvestmentForForeclosure = investment;
    this.mfForeclosureDetails = null;
    this.mfForeclosureRequestReason = '';
    this.showMFForeclosureModal = true;
    this.calculateMFForeclosure();
  }
  
  closeMFForeclosureModal() {
    this.showMFForeclosureModal = false;
    this.selectedInvestmentForForeclosure = null;
    this.mfForeclosureDetails = null;
    this.mfForeclosureRequestReason = '';
    this.mfForeclosureOtpSent = false;
    this.mfForeclosureOtpCode = '';
    this.showMFForeclosureOtpModal = false;
  }
  
  calculateMFForeclosure() {
    if (!this.selectedInvestmentForForeclosure) return;
    
    this.isCalculatingMFForeclosure = true;
    this.http.get(`${environment.apiBaseUrl}/api/investments/foreclosure/calculate/${this.selectedInvestmentForForeclosure.id}`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.mfForeclosureDetails = response;
        } else {
          this.alertService.error('Error', response.message || 'Failed to calculate foreclosure');
        }
        this.isCalculatingMFForeclosure = false;
      },
      error: (err: any) => {
        console.error('Error calculating foreclosure:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to calculate foreclosure');
        this.isCalculatingMFForeclosure = false;
      }
    });
  }
  
  requestMFForeclosure() {
    if (!this.selectedInvestmentForForeclosure || !this.mfForeclosureDetails) return;
    
    // If OTP hasn't been requested yet, request it first
    if (!this.mfForeclosureOtpSent) {
      this.requestMFForeclosureOtp();
      return;
    }
    
    // OTP was sent, now verify it
    if (!this.mfForeclosureOtpCode || this.mfForeclosureOtpCode.trim() === '') {
      this.alertService.error('Validation Error', 'Please enter the OTP');
      return;
    }
    
    this.isRequestingMFForeclosure = true;
    const params = new HttpParams()
      .set('investmentId', this.selectedInvestmentForForeclosure.id.toString())
      .set('requestReason', this.mfForeclosureRequestReason || '')
      .set('otp', this.mfForeclosureOtpCode.trim());
    
    this.http.post(`${environment.apiBaseUrl}/api/investments/foreclosure/request`, null, { params }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Foreclosure request submitted successfully. Waiting for admin approval.');
          this.closeMFForeclosureModal();
          this.loadInvestments();
          this.loadMFForeclosureHistory();
        } else {
          this.alertService.error('Error', response.message || 'Failed to submit foreclosure request');
        }
        this.isRequestingMFForeclosure = false;
      },
      error: (err: any) => {
        console.error('Error requesting foreclosure:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to submit foreclosure request');
        this.isRequestingMFForeclosure = false;
      }
    });
  }
  
  requestMFForeclosureOtp() {
    if (!this.selectedInvestmentForForeclosure) return;
    
    this.isRequestingMFForeclosureOtp = true;
    const params = new HttpParams()
      .set('investmentId', this.selectedInvestmentForForeclosure.id.toString())
      .set('requestReason', this.mfForeclosureRequestReason || '');
    
    this.http.post(`${environment.apiBaseUrl}/api/investments/foreclosure/request-otp`, null, { params }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.mfForeclosureOtpSent = true;
          this.showMFForeclosureOtpModal = true;
          this.mfForeclosureOtpCode = '';
          this.alertService.success('Success', 'OTP sent to your registered email address.');
        } else {
          this.alertService.error('Error', response.message || 'Failed to send OTP');
        }
        this.isRequestingMFForeclosureOtp = false;
      },
      error: (err: any) => {
        console.error('Error requesting OTP:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to send OTP');
        this.isRequestingMFForeclosureOtp = false;
      }
    });
  }
  
  closeMFForeclosureOtpModal() {
    this.showMFForeclosureOtpModal = false;
    this.mfForeclosureOtpCode = '';
  }
  
  loadMFForeclosureHistory() {
    if (!this.userAccountNumber) return;
    
    this.isLoadingMFForeclosureHistory = true;
    this.http.get(`${environment.apiBaseUrl}/api/investments/foreclosure/account/${this.userAccountNumber}`).subscribe({
      next: (foreclosures: any) => {
        this.mfForeclosureHistory = foreclosures || [];
        this.isLoadingMFForeclosureHistory = false;
      },
      error: (err: any) => {
        console.error('Error loading foreclosure history:', err);
        this.isLoadingMFForeclosureHistory = false;
      }
    });
  }
  
  getMFForeclosureStatusClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'COMPLETED': return 'status-approved';
      case 'PENDING': return 'status-pending';
      case 'REJECTED': return 'status-rejected';
      default: return 'status-default';
    }
  }
  
  // Fixed Deposit Methods
  loadFixedDeposits() {
    if (!this.userAccountNumber) return;
    
    this.isLoadingFDs = true;
    this.http.get(`${environment.apiBaseUrl}/api/fixed-deposits/account/${this.userAccountNumber}`).subscribe({
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
  
  openFDForm() {
    this.showFDForm = true;
    this.fdForm = {
      principalAmount: 0,
      interestRate: 7.5,
      tenure: 12,
      interestPayout: 'At Maturity'
    };
  }
  
  closeFDForm() {
    this.showFDForm = false;
  }
  
  submitFD() {
    if (!this.userAccountNumber) {
      this.alertService.error('Error', 'Account number not found');
      return;
    }
    
    if (this.fdForm.principalAmount <= 0) {
      this.alertService.error('Validation Error', 'Principal amount must be greater than 0');
      return;
    }
    
    if (this.fdForm.tenure < 6) {
      this.alertService.error('Validation Error', 'Minimum tenure is 6 months');
      return;
    }
    
    this.isSubmittingFD = true;
    const fdData = {
      accountNumber: this.userAccountNumber,
      principalAmount: this.fdForm.principalAmount,
      interestRate: this.fdForm.interestRate,
      tenure: this.fdForm.tenure,
      interestPayout: this.fdForm.interestPayout
    };
    
    this.http.post(`${environment.apiBaseUrl}/api/fixed-deposits`, fdData).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit application submitted successfully');
          this.closeFDForm();
          this.loadFixedDeposits();
          this.loadCurrentBalanceFromMySQL();
        } else {
          this.alertService.error('Error', response.message || 'Failed to submit FD');
        }
        this.isSubmittingFD = false;
      },
      error: (err: any) => {
        console.error('Error submitting FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to submit FD');
        this.isSubmittingFD = false;
      }
    });
  }
  
  getFDStatusClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'ACTIVE': return 'status-active';
      case 'PENDING': return 'status-pending';
      case 'REJECTED': return 'status-rejected';
      case 'MATURED': return 'status-matured';
      case 'CLOSED': case 'PREMATURE_CLOSED': return 'status-closed';
      default: return 'status-default';
    }
  }
  
  // FD Foreclosure Methods
  openForeclosureModal(fd: any) {
    this.selectedFDForForeclosure = fd;
    this.showForeclosureModal = true;
    this.foreclosureDetails = null;
    this.loadForeclosureDetails();
  }
  
  closeForeclosureModal() {
    this.showForeclosureModal = false;
    this.selectedFDForForeclosure = null;
    this.foreclosureDetails = null;
    this.foreclosureOtpSent = false;
    this.foreclosureOtpCode = '';
    this.showForeclosureOtpModal = false;
  }
  
  loadForeclosureDetails() {
    if (!this.selectedFDForForeclosure) return;
    
    this.isLoadingForeclosure = true;
    this.http.get(`${environment.apiBaseUrl}/api/fixed-deposits/${this.selectedFDForForeclosure.id}/foreclosure/calculate`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.foreclosureDetails = response;
        } else {
          this.alertService.error('Error', response.message || 'Failed to calculate foreclosure details');
        }
        this.isLoadingForeclosure = false;
      },
      error: (err: any) => {
        console.error('Error loading foreclosure details:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to load foreclosure details');
        this.isLoadingForeclosure = false;
      }
    });
  }
  
  processForeclosure() {
    if (!this.selectedFDForForeclosure || !this.foreclosureDetails) return;
    
    // If OTP hasn't been requested yet, request it first
    if (!this.foreclosureOtpSent) {
      this.requestForeclosureOtp();
      return;
    }
    
    // OTP was sent, now verify it
    if (!this.foreclosureOtpCode || this.foreclosureOtpCode.trim() === '') {
      this.alertService.error('Validation Error', 'Please enter the OTP');
      return;
    }
    
    if (!confirm(`Are you sure you want to foreclose this FD? You will receive ₹${this.foreclosureDetails.closureAmount.toFixed(2)} after penalty deduction.`)) {
      return;
    }
    
    this.isProcessingForeclosure = true;
    const url = `${environment.apiBaseUrl}/api/fixed-deposits/${this.selectedFDForForeclosure.id}/foreclosure?closedBy=${this.username}&otp=${encodeURIComponent(this.foreclosureOtpCode.trim())}`;
    
    this.http.post(url, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'FD foreclosed successfully. Amount credited to your account.');
          this.closeForeclosureModal();
          this.loadFixedDeposits();
          this.loadCurrentBalanceFromMySQL();
        } else {
          this.alertService.error('Error', response.message || 'Failed to foreclose FD');
        }
        this.isProcessingForeclosure = false;
      },
      error: (err: any) => {
        console.error('Error processing foreclosure:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to foreclose FD');
        this.isProcessingForeclosure = false;
      }
    });
  }
  
  requestForeclosureOtp() {
    if (!this.selectedFDForForeclosure) return;
    
    this.isRequestingForeclosureOtp = true;
    this.http.post(`${environment.apiBaseUrl}/api/fixed-deposits/${this.selectedFDForForeclosure.id}/foreclosure/request-otp`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.foreclosureOtpSent = true;
          this.showForeclosureOtpModal = true;
          this.foreclosureOtpCode = '';
          this.alertService.success('Success', 'OTP sent to your registered email address.');
        } else {
          this.alertService.error('Error', response.message || 'Failed to send OTP');
        }
        this.isRequestingForeclosureOtp = false;
      },
      error: (err: any) => {
        console.error('Error requesting OTP:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to send OTP');
        this.isRequestingForeclosureOtp = false;
      }
    });
  }
  
  closeForeclosureOtpModal() {
    this.showForeclosureOtpModal = false;
    this.foreclosureOtpCode = '';
  }
  
  // FD Receipt Download Methods
  showFDReceiptModal: boolean = false;
  selectedFDForReceipt: any = null;
  today: Date = new Date();

  downloadFDReceipt(fd: any) {
    this.selectedFDForReceipt = fd;
    this.today = new Date();
    this.showFDReceiptModal = true;
  }

  closeFDReceiptModal() {
    this.showFDReceiptModal = false;
    this.selectedFDForReceipt = null;
  }

  printFDReceipt() {
    const receiptEl = document.getElementById('fd-receipt-content');
    if (!receiptEl) return;

    const printWindow = window.open('', '_blank', 'width=800,height=900');
    if (!printWindow) {
      this.alertService.error('Error', 'Please allow popups to download receipt');
      return;
    }

    const fd = this.selectedFDForReceipt;
    const startDate = fd.startDate ? new Date(fd.startDate).toLocaleDateString('en-IN') : '';
    const maturityDate = fd.maturityDate ? new Date(fd.maturityDate).toLocaleDateString('en-IN') : '';
    const now = new Date().toLocaleString('en-IN');
    const years = fd.years || (fd.tenure >= 12 ? Math.floor(fd.tenure / 12) : 0);
    const months = fd.tenure % 12;
    const principalFormatted = Number(fd.principalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const maturityFormatted = Number(fd.maturityAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const isClosed = fd.status === 'PREMATURE_CLOSED' || fd.status === 'CLOSED' || fd.isPrematureClosure;
    const closureDate = fd.closureDate ? new Date(fd.closureDate).toLocaleDateString('en-IN') : '';
    const penaltyFormatted = fd.prematureClosurePenalty ? Number(fd.prematureClosurePenalty).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00';
    const closureAmountFormatted = fd.prematureClosureAmount ? Number(fd.prematureClosureAmount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00';

    printWindow.document.write(`<!DOCTYPE html><html><head><title>NeoBank FD Receipt - ${fd.fdAccountNumber}</title><style>
      * { margin: 0; padding: 0; box-sizing: border-box; }
      body { font-family: 'Segoe UI', Arial, sans-serif; padding: 40px; background: #fff; color: #1a1a2e; }
      .receipt { max-width: 700px; margin: 0 auto; border: 2px solid #0056b3; padding: 30px; }
      .header { display: flex; align-items: center; justify-content: space-between; border-bottom: 3px solid #e63946; padding-bottom: 16px; margin-bottom: 20px; }
      .bank-logo { display: flex; align-items: center; gap: 12px; }
      .bank-icon { width: 56px; height: 56px; background: linear-gradient(135deg, #0056b3, #00a8ff); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; font-weight: bold; }
      .bank-name h2 { color: #0056b3; font-size: 22px; margin-bottom: 2px; }
      .bank-name p { color: #666; font-size: 11px; font-style: italic; }
      .bank-tagline { color: #e63946; font-size: 12px; font-weight: 600; text-align: right; }
      .title { text-align: center; font-size: 18px; font-weight: 700; color: #e63946; margin: 20px 0; }
      table { width: 100%; border-collapse: collapse; margin: 12px 0; }
      table th, table td { border: 1px solid #ccc; padding: 10px 14px; text-align: center; font-size: 13px; }
      table th { background: #f0f4f8; color: #333; font-weight: 600; }
      table td { color: #1a1a2e; }
      .summary { margin: 16px 0; font-size: 14px; line-height: 1.6; color: #333; }
      .summary strong { color: #0056b3; }
      .footer { margin-top: 24px; padding-top: 16px; border-top: 2px solid #e63946; }
      .note { font-size: 12px; color: #555; margin-bottom: 14px; font-style: italic; }
      .instructions { font-size: 12px; color: #444; }
      .instructions ol { padding-left: 20px; }
      .instructions li { margin-bottom: 6px; line-height: 1.5; }
      .generated { text-align: right; font-size: 11px; color: #888; margin-top: 16px; }
      .closure-section { margin: 18px 0; padding: 14px; border: 2px solid #e63946; border-radius: 8px; background: #fff5f5; }
      .closure-title { text-align: center; font-size: 15px; font-weight: 700; color: #e63946; margin-bottom: 10px; }
      .closure-status { color: #e63946; font-weight: 700; }
      .penalty { color: #e63946; font-weight: 600; }
      .closure-amount { color: #16a34a; font-weight: 700; }
      .closed-by { font-size: 12px; color: #666; margin-top: 8px; text-align: right; font-style: italic; }
      @media print { body { padding: 20px; } .receipt { border: 2px solid #0056b3; } }
    </style></head><body>
    <div class="receipt">
      <div class="header">
        <div class="bank-logo">
          <div class="bank-icon">N</div>
          <div class="bank-name"><h2>NeoBank</h2><p>Digital Banking Solutions</p></div>
        </div>
        <div class="bank-tagline">...the digital bank you can trust!</div>
      </div>
      <div class="title">e-Fixed Deposit Account Receipt</div>
      <table>
        <tr><th>FD Account No</th><th>Customer Name</th></tr>
        <tr><td>${fd.fdAccountNumber}</td><td>${fd.userName || this.username}</td></tr>
      </table>
      <p class="summary">Deposit Amount: <strong>INR|${principalFormatted}</strong> for a period of <strong>${years}(yrs), ${months}(months), 0(days)</strong> at the rate of <strong>${fd.interestRate}%</strong> per annum.</p>
      <table>
        <tr><th>Date of Issue</th><th>Date of Maturity</th><th>Maturity Value</th></tr>
        <tr><td>${startDate}</td><td>${maturityDate}</td><td>${maturityFormatted}</td></tr>
        <tr><th>Deposit Scheme</th><th>Maturity Instruction</th><th>Interest Payout</th></tr>
        <tr><td>NEOFD</td><td>Auto Close</td><td>${fd.interestPayout || 'At Maturity'}</td></tr>
      </table>
      <table>
        <tr><th>Debit Account Number</th><th>Repayment Account No</th></tr>
        <tr><td>${fd.accountNumber}</td><td>${fd.accountNumber}</td></tr>
      </table>
      ${isClosed ? `<div class="closure-section">
        <div class="closure-title">Premature Closure / Foreclosure Details</div>
        <table>
          <tr><th>Closure Status</th><th>Date of Closure</th></tr>
          <tr><td class="closure-status">${fd.status === 'PREMATURE_CLOSED' ? 'Premature Closed' : 'Closed'}</td><td>${closureDate}</td></tr>
          <tr><th>Penalty Amount</th><th>Closure Amount Credited</th></tr>
          <tr><td class="penalty">&#8377;${penaltyFormatted}</td><td class="closure-amount">&#8377;${closureAmountFormatted}</td></tr>
        </table>
        ${fd.closedBy ? `<p class="closed-by">Processed by: ${fd.closedBy}</p>` : ''}
      </div>` : ''}
      <div class="footer">
        <p class="note">This is a computer generated e-fixed deposit receipt from NeoBank Digital Banking, hence does not require any signature.</p>
        <div class="instructions">
          <p><strong>Important Instructions:</strong></p>
          <ol>
            <li>This is not transferable.</li>
            <li>The confirmation of deposit is being issued against deposit as per given mandate of customer. In case of discrepancy please contact support within 7 days of issue of this confirmation.</li>
            <li>Maturity value and part withdrawal if any, are subject to TDS as per Income Tax Act.</li>
            <li>The deposit shall be auto renewed at the rate applicable for the concerned period on date of maturity.</li>
            <li>Withdrawal of term deposit before maturity is allowed subject to penal interest rate with regard to premature withdrawal of Term Deposit as per Bank Guidelines.</li>
          </ol>
        </div>
        <p class="generated">Receipt generated on: ${now}</p>
      </div>
    </div>
    <script>window.onload = function() { window.print(); }</script>
    </body></html>`);
    printWindow.document.close();
  }

  // FD Top-up Methods
  openTopUpModal(fd: any) {
    this.selectedFDForTopUp = fd;
    this.topUpAmount = 0;
    this.showTopUpModal = true;
  }
  
  closeTopUpModal() {
    this.showTopUpModal = false;
    this.selectedFDForTopUp = null;
    this.topUpAmount = 0;
  }
  
  processTopUp() {
    if (!this.selectedFDForTopUp || !this.topUpAmount || this.topUpAmount <= 0) {
      this.alertService.error('Validation Error', 'Please enter a valid amount');
      return;
    }
    
    if (this.topUpAmount > this.currentBalance) {
      this.alertService.error('Validation Error', 'Insufficient balance');
      return;
    }
    
    if (!confirm(`Add ₹${this.topUpAmount.toFixed(2)} to this FD? This amount will be debited from your account.`)) {
      return;
    }
    
    this.isProcessingTopUp = true;
    this.http.post(`${environment.apiBaseUrl}/api/fixed-deposits/${this.selectedFDForTopUp.id}/top-up?additionalAmount=${this.topUpAmount}&requestedBy=${this.username}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'FD amount increased successfully');
          this.closeTopUpModal();
          this.loadFixedDeposits();
          this.loadCurrentBalanceFromMySQL();
        } else {
          this.alertService.error('Error', response.message || 'Failed to increase FD amount');
        }
        this.isProcessingTopUp = false;
      },
      error: (err: any) => {
        console.error('Error processing top-up:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to increase FD amount');
        this.isProcessingTopUp = false;
      }
    });
  }
  
  // EMI Monitoring Methods
  loadEMIs() {
    if (!this.userAccountNumber) return;
    
    // STEP 1: Load from cache first for instant display
    this.loadEMIsFromCache();
    
    // STEP 2: Load from API in background
    this.isLoadingEMIs = true;
    
    this.http.get(`${environment.apiBaseUrl}/api/emis/account/${this.userAccountNumber}`)
      .pipe(
        timeout(20000), // Increased to 20 second timeout
        catchError(err => {
          console.error('Error loading EMIs:', err);
          
          // Final error handling
          let errorMessage = 'Failed to load EMIs. ';
          if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
            errorMessage = 'Request timed out. Please try refreshing or check your connection. ';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server. Please check if the backend is running. ';
          } else if (err.status >= 500) {
            errorMessage = 'Server error occurred. Please try again later. ';
          } else if (err.status === 404) {
            // No EMIs found is not really an error, just show empty state
            this.emis = [];
            this.isLoadingEMIs = false;
            this.loadEMISummary();
            return of([]);
          }
          
          // Only show error if we don't have cached data (less intrusive)
          if (this.emis.length === 0) {
            // Delay error message slightly to avoid blocking UI
            setTimeout(() => {
              this.alertService.error('EMI Loading Error', errorMessage);
            }, 300);
          } else {
            console.warn('Using cached EMI data due to API error');
          }
          
          this.isLoadingEMIs = false;
          return of([]);
        })
      )
      .subscribe({
        next: (emis: any) => {
          if (!emis || emis.length === 0) {
            // If API returns empty, keep cached data if available
            if (this.emis.length === 0) {
              this.emis = [];
            }
          } else {
            // Remove duplicates based on unique identifier (id or combination of loanId + emiNumber)
            this.emis = this.removeDuplicateEMIs(emis || []);
            // Cache the EMIs
            if (isPlatformBrowser(this.platformId)) {
              localStorage.setItem(`emis_${this.userAccountNumber}`, JSON.stringify(this.emis));
            }
          }
          this.isLoadingEMIs = false;
          this.loadEMISummary();
          this.applyEMIFilters();
          this.generateFinancialYears();
        },
        error: () => {
          // Error already handled by catchError
          this.isLoadingEMIs = false;
        }
      });
  }
  
  /**
   * Remove duplicate EMIs based on unique identifier
   */
  removeDuplicateEMIs(emis: any[]): any[] {
    const seen = new Map<string, any>();
    
    for (const emi of emis) {
      // Create unique key: id if available, otherwise loanId + emiNumber
      const uniqueKey = emi.id 
        ? `id_${emi.id}` 
        : `loan_${emi.loanId || emi.loanAccountNumber}_emi_${emi.emiNumber}`;
      
      // Only keep the first occurrence
      if (!seen.has(uniqueKey)) {
        seen.set(uniqueKey, emi);
      } else {
        console.warn('Duplicate EMI found and removed:', uniqueKey, emi);
      }
    }
    
    return Array.from(seen.values());
  }
  
  /**
   * Generate financial years list from EMI dates
   */
  generateFinancialYears() {
    const years = new Set<string>();
    const currentYear = new Date().getFullYear();
    
    // Add current and previous 5 years
    for (let i = 0; i < 6; i++) {
      years.add((currentYear - i).toString());
    }
    
    // Extract years from EMI due dates
    this.emis.forEach(emi => {
      if (emi.dueDate) {
        const year = new Date(emi.dueDate).getFullYear().toString();
        years.add(year);
      }
      if (emi.paymentDate) {
        const year = new Date(emi.paymentDate).getFullYear().toString();
        years.add(year);
      }
    });
    
    this.financialYears = Array.from(years).sort((a, b) => parseInt(b) - parseInt(a));
  }
  
  /**
   * Apply filters to EMIs
   */
  applyEMIFilters() {
    let filtered = [...this.emis];
    
    // Filter by tab
    if (this.activeEmiTab === 'paid') {
      filtered = filtered.filter(emi => 
        emi.status === 'Paid' || emi.status === 'PAID'
      );
    } else if (this.activeEmiTab === 'upcoming') {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const nextWeek = new Date(today);
      nextWeek.setDate(nextWeek.getDate() + 7);
      
      filtered = filtered.filter(emi => {
        if (emi.status === 'Paid' || emi.status === 'PAID') return false;
        if (!emi.dueDate) return false;
        const dueDate = new Date(emi.dueDate);
        dueDate.setHours(0, 0, 0, 0);
        return dueDate >= today && dueDate <= nextWeek;
      });
    }
    
    // Filter by status
    if (this.emiFilters.status !== 'all') {
      filtered = filtered.filter(emi => {
        const status = (emi.status || '').toLowerCase();
        if (this.emiFilters.status === 'paid') {
          return status === 'paid';
        } else if (this.emiFilters.status === 'pending') {
          return status === 'pending';
        } else if (this.emiFilters.status === 'failed') {
          return status === 'failed';
        } else if (this.emiFilters.status === 'overdue') {
          if (status === 'paid') return false;
          if (!emi.dueDate) return false;
          const dueDate = new Date(emi.dueDate);
          return dueDate < new Date();
        }
        return true;
      });
    }
    
    // Filter by financial year
    if (this.emiFilters.financialYear) {
      filtered = filtered.filter(emi => {
        const year = emi.dueDate 
          ? new Date(emi.dueDate).getFullYear().toString()
          : (emi.paymentDate ? new Date(emi.paymentDate).getFullYear().toString() : '');
        return year === this.emiFilters.financialYear;
      });
    }
    
    // Filter by month
    if (this.emiFilters.month) {
      filtered = filtered.filter(emi => {
        const month = emi.dueDate 
          ? String(new Date(emi.dueDate).getMonth() + 1).padStart(2, '0')
          : (emi.paymentDate ? String(new Date(emi.paymentDate).getMonth() + 1).padStart(2, '0') : '');
        return month === this.emiFilters.month;
      });
    }
    
    // Filter by search term
    if (this.emiFilters.searchTerm) {
      const searchLower = this.emiFilters.searchTerm.toLowerCase();
      filtered = filtered.filter(emi => {
        return (
          (emi.loanAccountNumber || '').toLowerCase().includes(searchLower) ||
          (emi.emiNumber?.toString() || '').includes(searchLower) ||
          (emi.status || '').toLowerCase().includes(searchLower)
        );
      });
    }
    
    // Sort by due date (most recent first)
    filtered.sort((a, b) => {
      const dateA = new Date(a.dueDate || 0).getTime();
      const dateB = new Date(b.dueDate || 0).getTime();
      return dateB - dateA;
    });
    
    this.filteredEmis = filtered;
  }
  
  /**
   * Reset EMI filters
   */
  resetEMIFilters() {
    this.emiFilters = {
      financialYear: '',
      status: 'all',
      month: '',
      searchTerm: ''
    };
    this.activeEmiTab = 'all';
    this.applyEMIFilters();
  }
  
  /**
   * Switch EMI tab
   */
  switchEmiTab(tab: 'all' | 'paid' | 'upcoming') {
    this.activeEmiTab = tab;
    this.applyEMIFilters();
  }
  
  /**
   * Load EMIs from localStorage cache for instant display
   */
  loadEMIsFromCache() {
    if (!isPlatformBrowser(this.platformId) || !this.userAccountNumber) return;
    
    const cachedEMIs = localStorage.getItem(`emis_${this.userAccountNumber}`);
    if (cachedEMIs) {
      try {
        const cached = JSON.parse(cachedEMIs);
        // Remove duplicates from cache as well
        this.emis = this.removeDuplicateEMIs(cached);
        console.log(`Loaded ${this.emis.length} EMIs from cache for instant display`);
        // Calculate summary immediately from cached data
        this.loadEMISummary();
        this.applyEMIFilters();
        this.generateFinancialYears();
      } catch (e) {
        console.error('Error parsing cached EMIs:', e);
        this.emis = [];
      }
    }
  }
  
  loadEMISummary() {
    if (!this.userAccountNumber) return;
    
    // Calculate summary from loaded EMIs instead of separate API call
    if (this.emis && this.emis.length > 0) {
      const totalEmis = this.emis.length;
      const paidEmis = this.emis.filter((emi: any) => emi.status === 'PAID' || emi.status === 'Paid').length;
      const pendingEmis = this.emis.filter((emi: any) => emi.status === 'PENDING' || emi.status === 'Pending').length;
      const overdueEmis = this.emis.filter((emi: any) => {
        if (emi.status === 'PAID' || emi.status === 'Paid') return false;
        if (emi.dueDate) {
          const dueDate = new Date(emi.dueDate);
          return dueDate < new Date();
        }
        return false;
      }).length;
      
      const totalPendingAmount = this.emis
        .filter((emi: any) => emi.status === 'PENDING' || emi.status === 'Pending')
        .reduce((sum: number, emi: any) => sum + (emi.totalAmount || 0), 0);
      
      const nextDueEmi = this.emis
        .filter((emi: any) => emi.status === 'PENDING' || emi.status === 'Pending')
        .sort((a: any, b: any) => {
          const dateA = new Date(a.dueDate || 0);
          const dateB = new Date(b.dueDate || 0);
          return dateA.getTime() - dateB.getTime();
        })[0];
      
      this.emiSummary = {
        totalEmis,
        paidEmis,
        pendingEmis,
        overdueEmis,
        totalPendingAmount: totalPendingAmount || 0,
        nextDueAmount: nextDueEmi ? (nextDueEmi.totalAmount || 0) : 0
      };
    } else {
      this.emiSummary = {
        totalEmis: 0,
        paidEmis: 0,
        pendingEmis: 0,
        overdueEmis: 0,
        totalPendingAmount: 0,
        nextDueAmount: 0
      };
    }
  }
  
  payEMI(emiId: number) {
    if (!this.userAccountNumber) {
      this.alertService.error('Error', 'Account number not found');
      return;
    }
    
    if (!confirm('Are you sure you want to pay this EMI?')) {
      return;
    }
    
    this.http.post(`${environment.apiBaseUrl}/api/emis/${emiId}/pay?accountNumber=${this.userAccountNumber}`, {})
      .pipe(
        timeout(15000), // 15 second timeout for payment
        catchError(err => {
          console.error('Error paying EMI:', err);
          let errorMessage = err.error?.message || 'Failed to pay EMI. ';
          if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
            errorMessage = 'Request timed out. Please check your balance and try again. ';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server. Please check if the backend is running. ';
          } else if (err.status >= 500) {
            errorMessage = 'Server error occurred. Please try again later. ';
          } else if (err.status === 400) {
            errorMessage = err.error?.message || 'Invalid request. Please check your account balance. ';
          }
          this.alertService.error('Error', errorMessage);
          return of(null);
        })
      )
      .subscribe({
        next: (response: any) => {
          if (!response) {
            // Error already handled by catchError
            return;
          }
          if (response.success) {
            this.alertService.success('Success', 'EMI paid successfully');
            this.loadEMIs();
            this.loadCurrentBalanceFromMySQL();
          } else {
            this.alertService.error('Error', response.message || 'Failed to pay EMI');
          }
        },
        error: () => {
          // Error already handled by catchError
        }
      });
  }
  
  getEMIStatusClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'PAID': return 'status-paid';
      case 'PENDING': return 'status-pending';
      case 'OVERDUE': return 'status-overdue';
      default: return 'status-default';
    }
  }
  
  isEMIOverdue(emi: any): boolean {
    const status = (emi.status || '').toLowerCase();
    if (status === 'paid') return false;
    if (!emi.dueDate) return false;
    const dueDate = new Date(emi.dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    dueDate.setHours(0, 0, 0, 0);
    return dueDate < today;
  }
  
  /**
   * Get count of upcoming EMIs (due in next 7 days)
   */
  getUpcomingEmiCount(): number {
    if (!this.emis || this.emis.length === 0) return 0;
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const nextWeek = new Date(today);
    nextWeek.setDate(nextWeek.getDate() + 7);
    
    return this.emis.filter(emi => {
      const status = (emi.status || '').toLowerCase();
      if (status === 'paid') return false;
      if (!emi.dueDate) return false;
      const dueDate = new Date(emi.dueDate);
      dueDate.setHours(0, 0, 0, 0);
      return dueDate >= today && dueDate <= nextWeek;
    }).length;
  }

  // Deposit Request Methods
  loadDepositRequests() {
    if (!this.userAccountNumber) return;
    this.isLoadingDepositRequests = true;
    this.depositError = '';
    this.http.get(`${environment.apiBaseUrl}/api/deposit-requests/account/${this.userAccountNumber}`).subscribe({
      next: (requests: any) => {
        try {
          // Handle both array and object responses
          if (Array.isArray(requests)) {
            this.depositRequests = requests;
          } else if (requests && Array.isArray(requests.content)) {
            this.depositRequests = requests.content;
          } else {
            this.depositRequests = [];
          }
          this.isLoadingDepositRequests = false;
        } catch (e) {
          console.error('Error parsing deposit requests:', e);
          this.depositError = 'Failed to parse deposit requests';
          this.depositRequests = [];
          this.isLoadingDepositRequests = false;
        }
      },
      error: (err: any) => {
        console.error('Error loading deposit requests:', err);
        // Check if response is HTML (error page)
        if (err.error && typeof err.error === 'string' && err.error.includes('<!DOCTYPE')) {
          this.depositError = 'Server error: Received HTML instead of JSON. Please check server status.';
        } else {
          this.depositError = err.error?.message || 'Failed to load deposit requests. Please try again.';
        }
        this.isLoadingDepositRequests = false;
      }
    });
  }

  submitDepositRequest() {
    this.depositMessage = '';
    this.depositError = '';

    if (!this.userAccountNumber) {
      this.depositError = 'Account number not found';
      return;
    }

    if (this.depositForm.amount <= 0) {
      this.depositError = 'Amount must be greater than 0';
      return;
    }

    this.isSubmittingDeposit = true;
    const payload = {
      accountNumber: this.userAccountNumber,
      userName: this.username,
      amount: this.depositForm.amount,
      method: this.depositForm.method,
      referenceNumber: this.depositForm.referenceNumber,
      note: this.depositForm.note
    };

    this.http.post(`${environment.apiBaseUrl}/api/deposit-requests`, payload).subscribe({
      next: (response: any) => {
        try {
          if (response && response.success) {
            this.depositMessage = response.message || 'Deposit request submitted';
            this.resetDepositForm();
            this.loadDepositRequests();
          } else {
            this.depositError = response?.message || 'Failed to submit deposit request';
          }
          this.isSubmittingDeposit = false;
        } catch (e) {
          console.error('Error parsing deposit response:', e);
          this.depositError = 'Failed to parse server response';
          this.isSubmittingDeposit = false;
        }
      },
      error: (err: any) => {
        console.error('Error submitting deposit request:', err);
        // Check if response is HTML (error page)
        if (err.error && typeof err.error === 'string' && err.error.includes('<!DOCTYPE')) {
          this.depositError = 'Server error: Received HTML instead of JSON. Please check server status.';
        } else if (err.status === 0) {
          this.depositError = 'Unable to connect to server. Please check your connection.';
        } else {
          this.depositError = err.error?.message || 'Failed to submit deposit request. Please try again.';
        }
        this.isSubmittingDeposit = false;
      }
    });
  }

  resetDepositForm() {
    this.depositForm = {
      amount: 0,
      method: 'Cash',
      referenceNumber: '',
      note: ''
    };
  }

  // Search: only run navigate/clear on Enter (submit), so typing does not erase input
  onSidebarSearch() {
    // Real-time filtering handled by matchesSidebarSearch()
  }

  matchesSidebarSearch(label: string): boolean {
    if (!this.searchQuery || !this.searchQuery.trim()) return true;
    return label.toLowerCase().includes(this.searchQuery.toLowerCase().trim());
  }

  onSearchSubmit() {
    const query = (this.searchQuery || '').trim();
    if (!query) {
      if (this.selectedFeature) {
        this.selectFeature(null);
      }
      return;
    }
    const queryLower = query.toLowerCase();
    const featureMap: { [key: string]: string } = {
      'home': '',
      'account': 'accounts',
      'accounts': 'accounts',
      'transfer': 'transferfunds',
      'transfer funds': 'transferfunds',
      'pay': 'transferfunds',
      'send money': 'transferfunds',
      'card': 'card',
      'cards': 'card',
      'credit card': 'credit-card',
      'credit': 'credit-card',
      'virtual card': 'virtual-card',
      'transaction': 'transaction',
      'transactions': 'transaction',
      'history': 'transaction',
      'ai': 'ai-assistant',
      'assistant': 'ai-assistant',
      'chat': 'chat',
      'mutual fund': 'investments',
      'mutual funds': 'investments',
      'investment': 'investments',
      'investments': 'investments',
      'fd': 'fixed-deposits',
      'fixed deposit': 'fixed-deposits',
      'fixed deposits': 'fixed-deposits',
      'emi': 'emi-monitoring',
      'emi monitoring': 'emi-monitoring',
      'tax': 'emi-monitoring',
      'loan': 'loan',
      'loans': 'loan',
      'gold loan': 'goldloan',
      'goldloan': 'goldloan',
      'deposit': 'deposit-request',
      'cheque': 'cheque',
      'check': 'cheque',
      'kyc': 'kycupdate',
      'kyc update': 'kycupdate',
      'profile': 'profile',
      'insurance': 'insurance',
      'atm': 'atm-simulator',
      'withdraw': 'atm-simulator',
      'atm simulator': 'atm-simulator',
      'bill': 'bill-payment',
      'bills': 'bill-payment',
      'bill payment': 'bill-payment',
      'utilities': 'bill-payment',
      'beneficiary': 'beneficiary-management',
      'beneficiaries': 'beneficiary-management',
      'fasttag': 'fasttag',
      'fastag': 'fasttag',
      'toll': 'fasttag',
      'support': 'support-tickets',
      'ticket': 'support-tickets',
      'tickets': 'support-tickets',
      'help': 'support-tickets',
      'scheduled': 'scheduled-payments',
      'scheduled payment': 'scheduled-payments',
      'scheduled payments': 'scheduled-payments',
      'subsidy': 'subsidy-claim',
      'government': 'subsidy-claim',
      'scheme': 'subsidy-claim',
      'message': 'bank-messages',
      'messages': 'bank-messages',
      'notification': 'bank-messages'
    };
    // First try exact match
    if (featureMap.hasOwnProperty(queryLower)) {
      const feature = featureMap[queryLower];
      this.navigateToSearchResult(feature);
      return;
    }
    // Then try: query contains a key, or a key contains the query (partial match)
    for (const [key, feature] of Object.entries(featureMap)) {
      if (!key) continue;
      const match = queryLower === feature || queryLower.includes(key) || key.includes(queryLower);
      if (match) {
        this.navigateToSearchResult(feature);
        return;
      }
    }
    this.searchQuery = '';
  }

  private navigateToSearchResult(feature: string) {
    if (feature === 'profile') {
      this.goToProfile();
    } else if (feature === 'insurance') {
      this.goToInsurance();
    } else {
      this.selectFeature(feature || null);
    }
    this.searchQuery = '';
  }

  // ==================== Linked Account Switch ====================

  checkLinkedAccount() {
    if (!this.userAccountNumber || this.userAccountNumber === 'ACC001') {
      // Retry after profile loads
      setTimeout(() => {
        if (this.userAccountNumber && this.userAccountNumber !== 'ACC001') {
          this.doCheckLinkedAccount();
        }
      }, 2000);
      return;
    }
    this.doCheckLinkedAccount();
  }

  doCheckLinkedAccount() {
    this.currentAccountService.getLinkedAccountsBySavings(this.userAccountNumber).subscribe({
      next: (links) => {
        if (links && links.length > 0) {
          this.linkedAccount = links[0];
          this.hasLinkedAccount = true;
          // Load current account details for display
          this.currentAccountService.getLinkedCurrentAccountDetails(this.linkedAccount.currentAccountNumber).subscribe({
            next: (details) => {
              if (details.success) {
                this.linkedCurrentAccountDetails = details;
              }
            }
          });
        }
      }
    });
  }

  openCreatePinModal() {
    this.switchPin = '';
    this.confirmSwitchPin = '';
    this.showPinModal = true;
  }

  closeCreatePinModal() {
    this.showPinModal = false;
    this.switchPin = '';
    this.confirmSwitchPin = '';
  }

  createSwitchPin() {
    if (!this.switchPin || !this.switchPin.match(/^\d{4}$/)) {
      this.error = 'PIN must be exactly 4 digits';
      setTimeout(() => this.error = '', 3000);
      return;
    }
    if (this.switchPin !== this.confirmSwitchPin) {
      this.error = 'PINs do not match';
      setTimeout(() => this.error = '', 3000);
      return;
    }
    if (!this.linkedAccount?.id) return;
    this.isPinCreating = true;
    this.currentAccountService.createSwitchPin(this.linkedAccount.id, this.switchPin).subscribe({
      next: (res) => {
        this.isPinCreating = false;
        if (res.success) {
          this.linkedAccount!.pinCreated = true;
          this.showPinModal = false;
          this.switchPin = '';
          this.confirmSwitchPin = '';
        } else {
          this.error = res.message || 'Failed to create PIN';
          setTimeout(() => this.error = '', 3000);
        }
      },
      error: () => {
        this.isPinCreating = false;
        this.error = 'Failed to create PIN';
        setTimeout(() => this.error = '', 3000);
      }
    });
  }

  openSwitchToCurrentAccount() {
    if (!this.linkedAccount?.pinCreated) {
      this.openCreatePinModal();
      return;
    }
    this.verifyPin = '';
    this.showVerifyPinModal = true;
  }

  closeVerifyPinModal() {
    this.showVerifyPinModal = false;
    this.verifyPin = '';
  }

  verifySwitchAndNavigate() {
    if (!this.verifyPin || !this.verifyPin.match(/^\d{4}$/)) {
      this.error = 'PIN must be exactly 4 digits';
      setTimeout(() => this.error = '', 3000);
      return;
    }
    if (!this.linkedAccount?.id) return;
    this.isVerifyingPin = true;
    this.currentAccountService.verifySwitchPin(this.linkedAccount.id, this.verifyPin).subscribe({
      next: (res) => {
        this.isVerifyingPin = false;
        if (res.success) {
          this.showVerifyPinModal = false;
          // Navigate to current account dashboard
          if (this.linkedCurrentAccountDetails) {
            sessionStorage.setItem('currentAccount', JSON.stringify({
              accountNumber: this.linkedAccount!.currentAccountNumber,
              businessName: this.linkedCurrentAccountDetails.businessName,
              ownerName: this.linkedCurrentAccountDetails.ownerName,
              customerId: this.linkedCurrentAccountDetails.customerId,
              balance: this.linkedCurrentAccountDetails.balance,
              status: this.linkedCurrentAccountDetails.status
            }));
          }
          this.router.navigate(['/website/current-account-dashboard']);
        } else {
          this.error = res.message || 'Incorrect PIN';
          setTimeout(() => this.error = '', 3000);
        }
      },
      error: () => {
        this.isVerifyingPin = false;
        this.error = 'Failed to verify PIN';
        setTimeout(() => this.error = '', 3000);
      }
    });
  }
}
