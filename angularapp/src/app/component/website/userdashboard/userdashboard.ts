import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';
import { Chat } from '../chat/chat';
import { AIAssistant } from '../ai-assistant/ai-assistant';
// import { UserService } from '../../service/user';
// import { AccountService } from '../../service/account';
// import { TransactionService } from '../../service/transaction';

@Component({
  selector: 'app-userdashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, Chat, AIAssistant],
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
  
  // EMI Monitoring properties
  emis: any[] = [];
  isLoadingEMIs: boolean = false;
  emiSummary: any = null;

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

  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
    // private userService: UserService,
    // private accountService: AccountService,
    // private transactionService: TransactionService
  ) {
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state && nav.extras.state['username']) {
      this.username = nav.extras.state['username'];
    }
  }

  ngOnInit() {
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserProfile();
    }
  }
  
  // Load investments, FDs, and EMIs when feature is selected
  loadFeatureData() {
    if (this.selectedFeature === 'investments') {
      this.loadInvestments();
    } else if (this.selectedFeature === 'fixed-deposits') {
      this.loadFixedDeposits();
    } else if (this.selectedFeature === 'emi-monitoring') {
      this.loadEMIs();
    } else if (this.selectedFeature === 'deposit-request') {
       this.loadDepositRequests();
    }
  }
  
  selectFeature(feature: string) {
    this.selectedFeature = feature;
    if (feature === 'deposit-request') {
      this.depositMessage = '';
      this.depositError = '';
    }
    this.loadFeatureData();
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    this.error = '';
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.username = user.name;
      this.userAccountNumber = user.accountNumber;
      
      // Load user profile and balance from MySQL database
      this.loadUserDataFromMySQL(user.id);
    } else {
      // Try to get user from localStorage as fallback
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        const profile = JSON.parse(savedProfile);
        this.username = profile.name || 'User';
        this.userAccountNumber = profile.accountNumber || 'ACC001';
        this.currentBalance = 0;
        this.loading = false;
        console.log('Using fallback profile from localStorage');
      } else {
        // No session found - use default values to avoid errors
        this.username = 'User';
        this.userAccountNumber = 'ACC001';
        this.currentBalance = 0;
        this.loading = false;
        console.log('Using default values - no session found');
      }
    }
  }

  loadUserDataFromMySQL(userId: string) {
    // Load user data from MySQL database
    this.http.get(`${environment.apiBaseUrl}/users/${userId}`).subscribe({
      next: (userData: any) => {
        console.log('User data loaded from MySQL:', userData);
        
        this.userProfile = userData;
        this.username = userData.account?.name || userData.username || 'User';
        this.userAccountNumber = userData.accountNumber || 'ACC001';
        
        // Load current balance from MySQL
        this.loadCurrentBalanceFromMySQL();
      },
      error: (err: any) => {
        console.error('Error loading user data from MySQL:', err);
        // Use default values instead of showing error
        this.username = 'User';
        this.userAccountNumber = 'ACC001';
        this.currentBalance = 0;
        this.loading = false;
      }
    });
  }

  loadCurrentBalanceFromMySQL() {
    if (!this.userAccountNumber) {
      this.loading = false;
      return;
    }
    
    // Load current balance from MySQL database
    this.http.get(`${environment.apiBaseUrl}/accounts/balance/${this.userAccountNumber}`).subscribe({
      next: (balanceData: any) => {
        console.log('Balance loaded from MySQL:', balanceData);
        this.currentBalance = balanceData.balance || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading balance from MySQL:', err);
        // Fallback to user profile balance
        this.currentBalance = this.userProfile?.account?.balance || 0;
        this.loading = false;
      }
    });
  }


  goTo(page: string) {
    // ✅ use the `page` parameter, not Node path
    this.router.navigate([`/website/${page}`]);
  }

  goToProfile() {
    this.router.navigate(['/website/profile']);
  }

  logout() {
    this.alertService.logoutSuccess();
    this.router.navigate(['/website/user']); // back to login
  }

  getFeatureTitle(): string {
    const titles: { [key: string]: string } = {
      'transferfunds': 'Transfer Funds',
      'card': 'Card Management',
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
      'deposit-request': 'Deposit Request'
    };
    return titles[this.selectedFeature || ''] || '';
  }

  getFeatureDescription(): string {
    const descriptions: { [key: string]: string } = {
      'transferfunds': 'Send money to other accounts securely and quickly.',
      'card': 'Manage your debit and credit cards, view statements, and set limits.',
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
      'deposit-request': 'Submit a deposit slip to the branch/admin and track status.'
    };
    return descriptions[this.selectedFeature || ''] || '';
  }

  getFeatureIcon(): string {
    const icons: { [key: string]: string } = {
      'transferfunds': '💸',
      'card': '💳',
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
      'deposit-request': '🏧'
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

  ngOnDestroy() {
    this.stopQrScanner();
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
    
    this.http.post(`${environment.apiBaseUrl}/loans/predict-approval`, request).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/investments/account/${this.userAccountNumber}`).subscribe({
      next: (investments: any) => {
        this.investments = investments || [];
        this.isLoadingInvestments = false;
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
    
    this.http.post(`${environment.apiBaseUrl}/investments`, investmentData).subscribe({
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
  
  // Fixed Deposit Methods
  loadFixedDeposits() {
    if (!this.userAccountNumber) return;
    
    this.isLoadingFDs = true;
    this.http.get(`${environment.apiBaseUrl}/fixed-deposits/account/${this.userAccountNumber}`).subscribe({
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
    
    this.http.post(`${environment.apiBaseUrl}/fixed-deposits`, fdData).subscribe({
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
  
  // EMI Monitoring Methods
  loadEMIs() {
    if (!this.userAccountNumber) return;
    
    this.isLoadingEMIs = true;
    this.http.get(`${environment.apiBaseUrl}/emis/account/${this.userAccountNumber}`).subscribe({
      next: (emis: any) => {
        this.emis = emis || [];
        this.isLoadingEMIs = false;
        this.loadEMISummary();
      },
      error: (err: any) => {
        console.error('Error loading EMIs:', err);
        this.alertService.error('Error', 'Failed to load EMIs');
        this.isLoadingEMIs = false;
      }
    });
  }
  
  loadEMISummary() {
    if (!this.userAccountNumber) return;
    
    this.http.get(`${environment.apiBaseUrl}/emis/account/${this.userAccountNumber}/summary`).subscribe({
      next: (summary: any) => {
        this.emiSummary = summary;
      },
      error: (err: any) => {
        console.error('Error loading EMI summary:', err);
      }
    });
  }
  
  payEMI(emiId: number) {
    if (!this.userAccountNumber) {
      this.alertService.error('Error', 'Account number not found');
      return;
    }
    
    if (!confirm('Are you sure you want to pay this EMI?')) {
      return;
    }
    
    this.http.post(`${environment.apiBaseUrl}/emis/${emiId}/pay?accountNumber=${this.userAccountNumber}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'EMI paid successfully');
          this.loadEMIs();
          this.loadCurrentBalanceFromMySQL();
        } else {
          this.alertService.error('Error', response.message || 'Failed to pay EMI');
        }
      },
      error: (err: any) => {
        console.error('Error paying EMI:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to pay EMI');
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
    if (emi.status !== 'Pending') return false;
    const dueDate = new Date(emi.dueDate);
    const today = new Date();
    return dueDate < today;
  }

  // Deposit Request Methods
  loadDepositRequests() {
    if (!this.userAccountNumber) return;
    this.isLoadingDepositRequests = true;
    this.http.get(`${environment.apiBaseUrl}/deposit-requests/account/${this.userAccountNumber}`).subscribe({
      next: (requests: any) => {
        this.depositRequests = requests || [];
        this.isLoadingDepositRequests = false;
      },
      error: (err: any) => {
        console.error('Error loading deposit requests:', err);
        this.depositError = err.error?.message || 'Failed to load deposit requests';
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

    this.http.post(`${environment.apiBaseUrl}/deposit-requests`, payload).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.depositMessage = response.message || 'Deposit request submitted';
          this.resetDepositForm();
          this.loadDepositRequests();
        } else {
          this.depositError = response.message || 'Failed to submit deposit request';
        }
        this.isSubmittingDeposit = false;
      },
      error: (err: any) => {
        console.error('Error submitting deposit request:', err);
        this.depositError = err.error?.message || 'Failed to submit deposit request';
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
}
