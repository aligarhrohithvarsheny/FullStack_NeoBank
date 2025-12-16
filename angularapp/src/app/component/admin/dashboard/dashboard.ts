import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

interface UserProfile {
  id: string;
  name: string;
  email: string;
  accountNumber: string;
  balance: number;
  status: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  address?: string;
  pan?: string;
  aadhar?: string;
  occupation?: string;
  income?: number;
  accountType?: string;
  joinDate?: string;
}

interface CardDetails {
  id: string;
  cardNumber: string;
  cardType: string;
  expiry: string;
  cvv: string;
  status: string;
  pinSet: boolean;
  blocked: boolean;
  deactivated: boolean;
}

interface LoanDetails {
  id: string;
  loanType: string;
  amount: number;
  interestRate: number;
  tenure: number;
  emi: number;
  status: string;
  appliedDate: string;
  approvedDate?: string;
}

interface TransactionDetails {
  id: string;
  date: string;
  type: string;
  amount: number;
  description: string;
  balance: number;
}

interface TransactionRecord {
  id: string;
  transactionId: string;
  merchant: string;
  amount: number;
  type: string;
  description: string;
  balance: number;
  date: string;
  status: string;
  userName: string;
  accountNumber: string;
}

interface DepositRequest {
  id: number;
  requestId: string;
  accountNumber: string;
  userName: string;
  amount: number;
  method: string;
  referenceNumber?: string;
  note?: string;
  status: string;
  processedBy?: string;
  processedAt?: string;
  rejectionReason?: string;
  resultingBalance?: number;
  createdAt?: string;
}

interface AccountTracking {
  id: number;
  trackingId: string;
  aadharNumber: string;
  mobileNumber: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  statusChangedAt: string;
  updatedBy?: string;
  user?: {
    id: number;
    email: string;
    username: string;
    accountNumber?: string;
  };
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
  encapsulation: ViewEncapsulation.None,
  imports: [FormsModule, CommonModule],
  standalone: true
})
export class Dashboard implements OnInit {
  users: UserProfile[] = [];
  selectedUser: UserProfile | null = null;
  selectedUserId: string = '';
  showDepositWithdrawal: boolean = false;
  operationType: 'deposit' | 'withdrawal' = 'deposit';
  amount: number = 0;
  description: string = '';
  successMessage: string = '';
  errorMessage: string = '';

  // Account verification fields
  accountNumber: string = '';
  accountType: 'regular' | 'loan' | 'goldloan' | 'cheque' = 'regular';
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

  // User update history
  userUpdateHistory: any[] = [];
  showUpdateHistory: boolean = false;
  updateHistoryFilter: string = 'ALL'; // ALL, TODAY, WEEK, MONTH
  updateHistorySearchQuery: string = '';

  // Daily Activity Report
  showDailyActivityReport: boolean = false;
  selectedDate: string = new Date().toISOString().split('T')[0]; // Today's date
  dailyActivities: any[] = [];
  activityCategories: string[] = ['ALL', 'UPDATES', 'DEPOSITS', 'WITHDRAWALS', 'LOANS', 'GOLD_LOANS', 'CHEQUES', 'SUBSIDY_CLAIMS', 'ACCOUNTS', 'OTHER'];
  selectedActivityCategory: string = 'ALL';

  // Sidebar Navigation
  activeSection: string = 'dashboard'; // Current active section
  sidebarCollapsed: boolean = false; // For mobile responsiveness

  // Feature Access Control
  featureAccess: Map<string, boolean> = new Map(); // Store feature permissions
  
  // Navigation source tracking
  isFromManager: boolean = false;

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

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    // Only load users in the browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      // Check if profile is complete, redirect if not, and get admin name
      const adminData = sessionStorage.getItem('admin');
      if (adminData) {
        try {
          const admin = JSON.parse(adminData);
          // Check profile completion
          if (admin.profileComplete === false || admin.profileComplete === null) {
            // Redirect to profile completion page
            this.router.navigate(['/admin/complete-profile']);
            return;
          }
          // Set admin name
          this.adminName = admin.username || admin.name || 'Admin';
        } catch (e) {
          console.error('Error parsing admin data:', e);
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
              this.loadFeatureAccess();
            }
          } catch (e) {
            console.error('Error parsing admin data:', e);
          }
        } else {
          // Fallback: reload anyway
          this.loadFeatureAccess();
        }
      });
      
      // Load pending Aadhar verifications on init
      this.loadPendingAadharVerifications();
      this.loadUsers();
      this.loadAccountTrackings();
      this.loadCurrentGoldRate();
      this.loadLoginHistory();
      this.loadAdminTransactionHistory();
      this.loadUserUpdateHistory();
    }
  }

  loadFeatureAccess() {
    if (!isPlatformBrowser(this.platformId)) return;
    
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
      }
    }
    
    if (!adminEmail) {
      console.log('No admin email found, cannot load per-admin feature access');
      return;
    }
    
    // Load per-admin feature access from localStorage
    const savedKey = `adminFeatureAccess_${adminEmail}`;
    const savedFeatures = localStorage.getItem(savedKey);
    
    if (savedFeatures) {
      try {
        const features = JSON.parse(savedFeatures);
        console.log(`Loading feature access for ${adminEmail} from localStorage:`, features);
        features.forEach((feature: any) => {
          this.featureAccess.set(feature.id, feature.enabled === true); // Ensure boolean
          console.log(`Loaded feature: ${feature.id} = ${feature.enabled}`);
        });
        console.log(`Total features loaded: ${this.featureAccess.size}`);
      } catch (e) {
        console.error('Error loading feature access:', e);
      }
    } else {
      console.log(`No saved feature access found for ${adminEmail} in localStorage`);
    }
    
    // Also try to load from backend (this will override localStorage if backend has data)
    this.http.get(`${environment.apiBaseUrl}/admins/feature-access/${adminEmail}`).subscribe({
      next: (response: any) => {
        if (response && response.success && response.features && response.features.length > 0) {
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
      error: (err) => {
        console.error('Error loading feature access from backend:', err);
        // Use localStorage data if backend fails
        console.log('Using localStorage data as fallback');
      }
    });
  }

  hasFeatureAccess(featureId: string): boolean {
    // Check if feature is in the map
    if (this.featureAccess.has(featureId)) {
      const hasAccess = this.featureAccess.get(featureId)!;
      console.log(`Feature ${featureId} access: ${hasAccess}`);
      return hasAccess;
    }
    
    // If feature is not in the map, check if we have any features loaded
    // If we have features loaded but this one is missing, default to false (more secure)
    // If no features are loaded at all, default to true (backward compatibility)
    if (this.featureAccess.size > 0) {
      console.warn(`Feature ${featureId} not found in featureAccess map, defaulting to false`);
      return false; // If features are loaded but this one is missing, deny access
    }
    
    // No features loaded yet, default to true for backward compatibility
    console.log(`No features loaded yet, defaulting to true for ${featureId}`);
    return true;
  }

  // Refresh users data from database
  refreshUsersData() {
    console.log('Refreshing users data from database...');
    this.loadUsers();
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
      'chat': 'chat',
      'gold-loans': 'gold-loans',
      'deposit-withdraw': 'deposit-withdraw',
      'tracking': 'tracking',
      'aadhar-verification': 'aadhar-verification',
      'gold-rate': 'gold-rate',
      'login-history': 'login-history',
      'update-history': 'update-history',
      'daily-report': 'daily-report'
    };
    
    const featureId = featureAccessMap[section];
    if (featureId && !this.hasFeatureAccess(featureId)) {
      this.alertService.error('Access Denied', 'This feature has been disabled by the manager.');
      return;
    }
    
    // Navigate to external pages for these sections
    if (section === 'transactions') {
      console.log('Navigating to transactions page');
      this.navigateTo('transactions');
      return;
    } else if (section === 'loans') {
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
    } else if (section === 'chat') {
      this.navigateTo('chat');
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
    } else if (section === 'daily-report') {
      this.showDailyActivityReport = true;
      this.loadDailyActivities();
    } else if (section === 'prediction-history') {
      this.loadLoanPredictions();
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
    } else if (section === 'deposit-withdraw') {
      // Check feature access
      if (!this.hasFeatureAccess('deposit-withdraw')) {
        this.alertService.error('Access Denied', 'Deposit/Withdraw feature has been disabled by the manager.');
        this.activeSection = 'dashboard';
        return;
      }
      // Load deposit/withdraw form
      this.loadDepositRequests();
    }
    
    // Close other sections
    if (section !== 'tracking') this.showTrackingSection = false;
    if (section !== 'aadhar-verification') this.showAadharVerificationSection = false;
    if (section !== 'gold-rate') this.showGoldRateSection = false;
    if (section !== 'login-history') this.showLoginHistorySection = false;
    if (section !== 'update-history') this.showUpdateHistory = false;
    if (section !== 'daily-report') this.showDailyActivityReport = false;
    if (section !== 'investments') this.selectedInvestment = null;
    if (section !== 'fixed-deposits') this.selectedFD = null;
    if (section !== 'emi-management') this.selectedEMI = null;
  }
  
  // Investment Management Methods
  loadInvestments() {
    this.isLoadingInvestments = true;
    this.http.get(`${environment.apiBaseUrl}/investments`).subscribe({
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
    
    this.http.put(`${environment.apiBaseUrl}/investments/${investment.id}/approve?approvedBy=${this.adminName}`, {}).subscribe({
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
    
    this.http.put(`${environment.apiBaseUrl}/investments/${investment.id}/reject?rejectedBy=${this.adminName}&reason=${encodeURIComponent(reason)}`, {}).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/fixed-deposits`).subscribe({
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
    if (!confirm(`Approve FD of ₹${fd.principalAmount} for ${fd.userName}?`)) return;
    
    this.http.put(`${environment.apiBaseUrl}/fixed-deposits/${fd.id}/approve?approvedBy=${this.adminName}`, {}).subscribe({
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
    
    this.http.put(`${environment.apiBaseUrl}/fixed-deposits/${fd.id}/reject?rejectedBy=${this.adminName}&reason=${encodeURIComponent(reason)}`, {}).subscribe({
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
    
    this.http.put(`${environment.apiBaseUrl}/fixed-deposits/${fd.id}/process-maturity?processedBy=${this.adminName}`, {}).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/emis`).subscribe({
      next: (emis: any) => {
        this.allEmis = emis || [];
        this.isLoadingAllEMIs = false;
      },
      error: (err: any) => {
        console.error('Error loading EMIs:', err);
        // Try loading overdue EMIs instead
        this.http.get(`${environment.apiBaseUrl}/emis/overdue`).subscribe({
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
    this.alertService.logoutSuccess();
    this.router.navigate(['/admin/login']);
  }

  loadUsers() {
    // Load users from MySQL database
    this.http.get(`${environment.apiBaseUrl}/users`).subscribe({
      next: (usersData: any) => {
        console.log('Users loaded from MySQL:', usersData);
        this.users = usersData.map((user: any) => ({
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
          joinDate: user.createdAt ? new Date(user.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0]
        }));
        
        // Save to localStorage as backup
        if (isPlatformBrowser(this.platformId)) {
          localStorage.setItem('userProfiles', JSON.stringify(this.users));
        }
        
        console.log('Processed users for admin dashboard:', this.users);
      },
      error: (err: any) => {
        console.error('Error loading users from MySQL:', err);
        
        // Fallback to localStorage if database fails
        if (isPlatformBrowser(this.platformId)) {
          const usersData = localStorage.getItem('userProfiles');
          if (usersData) {
            this.users = JSON.parse(usersData);
            console.log('Using fallback data from localStorage');
          } else {
            this.users = [];
            console.log('No users found in database or localStorage');
          }
        }
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
    this.http.get(`${environment.apiBaseUrl}/deposit-requests`, { params }).subscribe({
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

  approveDepositRequest(request: DepositRequest) {
    if (!confirm(`Approve deposit of ₹${request.amount} for ${request.userName}?`)) return;
    const adminName = this.adminName || 'Admin';
    this.http.put(`${environment.apiBaseUrl}/deposit-requests/${request.id}/approve`, {}, {
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
    this.http.put(`${environment.apiBaseUrl}/deposit-requests/${request.id}/reject`, {}, {
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
    }
  }

  verifyRegularAccount(accountNumber: string) {
    this.http.get(`${environment.apiBaseUrl}/accounts/number/${accountNumber}`).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/loans`).subscribe({
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
          this.http.get(`${environment.apiBaseUrl}/loans/account/${accountNumber}`).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/gold-loans`).subscribe({
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
          this.http.get(`${environment.apiBaseUrl}/gold-loans/account/${accountNumber}`).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/cheques/account/${accountNumber}`).subscribe({
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
    }
  }

  processRegularAccountTransaction(accountNumber: string, userName: string, currentBalance: number) {
    const balanceEndpoint = this.operationType === 'deposit' 
      ? `${environment.apiBaseUrl}/accounts/balance/credit/${accountNumber}?amount=${this.amount}`
      : `${environment.apiBaseUrl}/accounts/balance/debit/${accountNumber}?amount=${this.amount}`;

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

    this.http.post(`${environment.apiBaseUrl}/transactions`, backendTransactionData).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/loans`).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/gold-loans`).subscribe({
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
      },
      error: (err) => console.error('Error loading gold loans:', err)
    });

    // Load cheque operations
    this.http.get(`${environment.apiBaseUrl}/cheques`).subscribe({
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
      },
      error: (err) => console.error('Error loading cheques:', err)
    });

    // Load subsidy claims
    this.http.get(`${environment.apiBaseUrl}/education-loan-subsidy-claims`).subscribe({
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
      },
      error: (err) => console.error('Error loading subsidy claims:', err)
    });

    // Load account openings
    this.http.get(`${environment.apiBaseUrl}/accounts/all?page=0&size=1000&sortBy=createdAt&sortDir=desc`).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/users/account/${user.accountNumber}`).subscribe({
      next: (dbUser: any) => {
        if (dbUser) {
          const updatedUser = {
            ...dbUser,
            account: {
              ...dbUser.account,
              balance: user.balance
            }
          };
          
          this.http.put(`${environment.apiBaseUrl}/users/update/${dbUser.id}`, updatedUser).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/cards`).subscribe({
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

    this.http.get(`${environment.apiBaseUrl}/admin/search?q=${encodeURIComponent(query)}`).subscribe({
      next: (results: any) => {
        this.universalSearchResults = results;
        this.isUniversalSearching = false;
        console.log('Universal search results:', results);
      },
      error: (err: any) => {
        console.error('Error performing universal search:', err);
        this.alertService.error('Search Error', 'Failed to perform search. Please try again.');
        this.isUniversalSearching = false;
        this.universalSearchResults = null;
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
    this.http.get(`${environment.apiBaseUrl}/cards/account/${user.accountNumber}`).subscribe({
      next: (cards: any) => {
        this.userCards = cards || [];
      },
      error: (err: any) => {
        console.error('Error loading user cards:', err);
        this.userCards = [];
      }
    });

    // Load user loans
    this.http.get(`${environment.apiBaseUrl}/loans/account/${user.accountNumber}`).subscribe({
      next: (loans: any) => {
        this.userLoans = loans || [];
      },
      error: (err: any) => {
        console.error('Error loading user loans:', err);
        this.userLoans = [];
      }
    });

    // Load user transactions
    this.http.get(`${environment.apiBaseUrl}/transactions/account/${user.accountNumber}`).subscribe({
      next: (transactions: any) => {
        this.userTransactions = transactions || [];
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
    this.http.get(`${environment.apiBaseUrl}/tracking?page=0&size=100&sortBy=createdAt&sortDir=desc`).subscribe({
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

    this.http.get(`${environment.apiBaseUrl}/tracking/track?aadharNumber=${aadhar}&mobileNumber=${mobile}`).subscribe({
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

    const url = `${environment.apiBaseUrl}/tracking/${tracking.id}/status?status=${encodeURIComponent(newStatus)}&updatedBy=Admin`;
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
    this.http.get<any>(`${environment.apiBaseUrl}/gold-rates/current`).subscribe({
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
    this.http.get<any[]>(`${environment.apiBaseUrl}/gold-rates/history`).subscribe({
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
    
    this.http.put<any>(`${environment.apiBaseUrl}/gold-rates/update`, {}, { params }).subscribe({
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
    this.http.get<any[]>(`${environment.apiBaseUrl}/gold-loans`).subscribe({
      next: (loans) => {
        this.goldLoans = loans;
        this.filterGoldLoans();
        this.isLoadingGoldLoans = false;
      },
      error: (err) => {
        console.error('Error loading gold loans:', err);
        this.alertService.error('Error', 'Failed to load gold loans');
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
    this.http.put<any>(`${environment.apiBaseUrl}/gold-loans/${this.selectedGoldLoan.id}/approve`, this.goldDetailsForm, {
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
    this.http.put<any>(`${environment.apiBaseUrl}/gold-loans/${loan.id}/approve`, {}, {
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

    this.http.get<any>(`${environment.apiBaseUrl}/gold-loans/foreclosure/${loan.loanAccountNumber}`).subscribe({
      next: (response) => {
        if (response.success) {
          this.foreclosureDetails = response;
        } else {
          this.alertService.error('Error', response.message || 'Failed to load foreclosure details');
        }
      },
      error: (err) => {
        console.error('Error loading foreclosure details:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to load foreclosure details');
      }
    });
  }

  processForeclosure() {
    if (!this.selectedGoldLoan || !this.selectedGoldLoan.loanAccountNumber) return;
    if (!confirm(`Are you sure you want to process foreclosure for this loan?`)) return;

    this.processingForeclosure = true;
    this.http.post<any>(`${environment.apiBaseUrl}/gold-loans/foreclosure/${this.selectedGoldLoan.loanAccountNumber}`, {}).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Success', 'Foreclosure processed successfully');
          this.closeForeclosureModal();
          this.loadGoldLoans();
        } else {
          this.alertService.error('Error', response.message || 'Failed to process foreclosure');
        }
        this.processingForeclosure = false;
      },
      error: (err) => {
        console.error('Error processing foreclosure:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to process foreclosure');
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

    this.http.get<any[]>(`${environment.apiBaseUrl}/gold-loans/emi-schedule/${loan.loanAccountNumber}`).subscribe({
      next: (schedule) => {
        this.emiSchedule = Array.isArray(schedule) ? schedule : [];
      },
      error: (err) => {
        console.error('Error loading EMI schedule:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to load EMI schedule');
        this.emiSchedule = [];
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
    this.http.post(`${environment.apiBaseUrl}/tracking/${tracking.id}/verify-aadhar`, {}).subscribe({
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
    this.http.get<any[]>(`${environment.apiBaseUrl}/admins/login-history/recent?limit=100`).subscribe({
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
    
    this.http.get(`${environment.apiBaseUrl}/loans/predictions/all`).subscribe({
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
}
