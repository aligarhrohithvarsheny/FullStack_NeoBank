import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

interface TransferRecord {
  id: number; // Database ID (Long)
  transferId?: string; // Custom transfer ID
  senderAccountNumber: string;
  senderName: string;
  recipientAccountNumber: string;
  recipientName: string;
  phone: string;
  ifsc: string;
  amount: number;
  transferType: 'NEFT' | 'RTGS' | 'IMPS';
  status: 'Pending' | 'Completed' | 'Failed' | 'Cancelled';
  date: string;
  createdAt?: string;
  updatedAt?: string;
  isCancellable?: boolean;
}

interface TransactionRecord {
  id: string;
  merchant: string;
  amount: number;
  type: string;
  balance: number;
  date: string;
  description: string;
}

interface UserProfile {
  name: string;
  email: string;
  accountNumber: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  accountType: string;
  joinDate: string;
}

@Component({
  selector: 'app-transferfunds',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transferfunds.html',
  styleUrls: ['./transferfunds.css']
})
export class Transferfunds implements OnInit {
  // Form fields
  recipientAccountNumber: string = '';
  recipientName: string = '';
  phone: string = '';
  ifsc: string = '';
  amount: number = 0;
  transferType: 'NEFT' | 'RTGS' | 'IMPS' = 'NEFT';

  // Account verification
  isAccountVerified: boolean = false;
  isVerifyingAccount: boolean = false;
  accountVerificationError: string = '';
  verifiedAccountDetails: any = null;

  // Beneficiaries UI
  showBeneficiaryForm: boolean = false;
  beneficiaries: any[] = [];
  selectedBeneficiary: any = null;
  beneficiaryNickname: string = '';
  isSavingBeneficiary: boolean = false;

  // User data
  userProfile: UserProfile = {
    name: 'John Doe',
    email: 'john.doe@example.com',
    accountNumber: 'ACC001',
    phoneNumber: '+91 98765 43210',
    address: '123 Main Street, City, State 12345',
    dateOfBirth: '1990-01-15',
    accountType: 'Savings Account',
    joinDate: '2023-01-15'
  };

  currentBalance: number = 0;
  transfers: TransferRecord[] = [];
  transactionSuccess: boolean = false;
  successMessage: string = '';

  // OTP flow
  showOtpModal: boolean = false;
  otpTxnId: string | null = null;
  otpCode: string = '';
  otpTimer: number = 0; // seconds remaining
  otpSent: boolean = false;
  otpSending: boolean = false;
  verifyingOtp: boolean = false;
  resendCount: number = 0;

  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      console.log('TransferFunds component initialized');
      this.loadUserProfile();
      this.loadCurrentBalance();
      this.loadTransferHistory();
      this.loadBeneficiaries();
      
      // Check for scanned QR code data from navigation state
      const nav = this.router.getCurrentNavigation();
      if (nav?.extras?.state) {
        const state = nav.extras.state as any;
        if (state.scannedUpiId || state.scannedName || state.scannedAmount) {
          this.handleScannedQrCode(state);
        }
      }
    }
  }

  // Beneficiaries methods
  toggleBeneficiaryForm() {
    this.showBeneficiaryForm = !this.showBeneficiaryForm;
    if (this.showBeneficiaryForm) {
      this.beneficiaryNickname = '';
    }
  }

  loadBeneficiaries() {
    if (!isPlatformBrowser(this.platformId)) return;
    const key = `beneficiaries_${this.userProfile.accountNumber}`;
    const saved = localStorage.getItem(key);
    if (saved) {
      try { this.beneficiaries = JSON.parse(saved); } catch(e) { this.beneficiaries = []; }
    }

    // Try loading from backend
    this.http.get(`${environment.apiBaseUrl}/api/beneficiaries/account/${this.userProfile.accountNumber}`).subscribe({
      next: (res: any) => {
        this.beneficiaries = Array.isArray(res) ? res : (res.content || []);
        try { localStorage.setItem(key, JSON.stringify(this.beneficiaries)); } catch(e) {}
      },
      error: (err: any) => {
        console.warn('Could not load beneficiaries from backend, using cached list', err);
      }
    });
  }

  selectBeneficiary(beneficiary: any) {
    this.selectedBeneficiary = beneficiary;
    this.recipientAccountNumber = beneficiary.accountNumber || beneficiary.recipientAccountNumber || '';
    this.recipientName = beneficiary.nickname || beneficiary.name || beneficiary.recipientName || '';
    this.phone = beneficiary.phone || '';
    this.ifsc = beneficiary.ifsc || this.ifsc || 'NEOB0001234';
    this.isAccountVerified = true;
  }

  clearBeneficiarySelection() {
    this.selectedBeneficiary = null;
  }

  deleteBeneficiary(beneficiary: any) {
    if (!confirm('Delete this beneficiary?')) return;
    if (!beneficiary || !beneficiary.id) {
      // Remove locally
      this.beneficiaries = this.beneficiaries.filter(b => b !== beneficiary);
      try { localStorage.setItem(`beneficiaries_${this.userProfile.accountNumber}`, JSON.stringify(this.beneficiaries)); } catch(e) {}
      return;
    }
    this.http.delete(`${environment.apiBaseUrl}/api/beneficiaries/${beneficiary.id}`).subscribe({
      next: () => {
        this.beneficiaries = this.beneficiaries.filter(b => b.id !== beneficiary.id);
        try { localStorage.setItem(`beneficiaries_${this.userProfile.accountNumber}`, JSON.stringify(this.beneficiaries)); } catch(e) {}
      },
      error: (err: any) => {
        console.error('Error deleting beneficiary:', err);
        this.alertService.transferError('Failed to delete beneficiary');
      }
    });
  }

  saveBeneficiary() {
    if (!this.recipientAccountNumber || !this.beneficiaryNickname) {
      this.alertService.transferValidationError('Please provide account number and nickname');
      return;
    }
    if (!this.userProfile?.accountNumber) {
      this.alertService.transferValidationError('User account number not found');
      return;
    }
    this.isSavingBeneficiary = true;
    const payload = {
      senderAccountNumber: this.userProfile.accountNumber,
      recipientAccountNumber: this.recipientAccountNumber,
      recipientName: this.recipientName || this.beneficiaryNickname,
      nickname: this.beneficiaryNickname,
      phone: this.phone,
      ifsc: this.ifsc
    };
    this.http.post(`${environment.apiBaseUrl}/api/beneficiaries`, payload).subscribe({
      next: (res: any) => {
        const b = res && res.beneficiary ? res.beneficiary : res;
        this.beneficiaries.unshift(b);
        try { localStorage.setItem(`beneficiaries_${this.userProfile.accountNumber}`, JSON.stringify(this.beneficiaries)); } catch(e) {}
        this.isSavingBeneficiary = false;
        this.showBeneficiaryForm = false;
        this.alertService.transferSuccess(0, 'Beneficiary saved');
      },
      error: (err: any) => {
        console.error('Error saving beneficiary:', err);
        this.isSavingBeneficiary = false;
        this.alertService.transferError(err.error?.message || 'Failed to save beneficiary');
      }
    });
  }

  getNeftProcessingTime(): string {
    return 'NEFT processing usually takes 2-3 hours (up to same day depending on bank schedules)';
  }

  handleScannedQrCode(state: any) {
    // Extract UPI ID and try to find account number
    if (state.scannedUpiId) {
      // UPI ID format: phone@neobank or account@neobank
      const upiId = state.scannedUpiId;
      const accountPart = upiId.split('@')[0];
      
      // Try to use as account number or phone
      if (accountPart.length > 10) {
        // Likely account number
        this.recipientAccountNumber = accountPart;
      } else {
        // Likely phone number - try to find account by phone
        this.phone = accountPart;
      }
    }
    
    if (state.scannedName) {
      this.recipientName = decodeURIComponent(state.scannedName);
    }
    
    if (state.scannedAmount) {
      this.amount = state.scannedAmount;
    }
    
    // If we have account number, verify it
    if (this.recipientAccountNumber) {
      this.verifyAccountNumber();
    }
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    console.log('Loading user profile...');
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    console.log('Current user from sessionStorage:', currentUser);
    
    if (currentUser) {
      const user = JSON.parse(currentUser);
      console.log('Parsed user data:', user);
      this.userProfile.name = user.name || 'User';
      this.userProfile.accountNumber = user.accountNumber || 'ACC001';
      this.userProfile.email = user.email || 'user@example.com';
      
      console.log('Updated user profile:', this.userProfile);
      
      // Load user profile from MySQL database
      this.loadUserDataFromMySQL(user.id);
    } else {
      console.log('No currentUser in sessionStorage, trying localStorage fallback');
      // Try to get user from localStorage as fallback
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        this.userProfile = { ...this.userProfile, ...JSON.parse(savedProfile) };
        console.log('Loaded profile from localStorage:', this.userProfile);
      }
    }
  }

  loadUserDataFromMySQL(userId: string) {
    // Load user data from MySQL database
    // Ensure ID is a number
    const userIdNum = typeof userId === 'number' ? userId : Number(userId);
    if (isNaN(userIdNum)) {
      console.error('Invalid user ID:', userId);
      return;
    }
    this.http.get(`${environment.apiBaseUrl}/api/users/${userIdNum}`).subscribe({
      next: (userData: any) => {
        console.log('User data loaded from MySQL:', userData);
        
        this.userProfile.name = userData.account?.name || userData.username || 'User';
        this.userProfile.accountNumber = userData.accountNumber || 'ACC001';
        this.userProfile.email = userData.email || 'user@example.com';
        
        console.log('User profile loaded, account number:', this.userProfile.accountNumber);
        
        // Load current balance from MySQL
        this.loadCurrentBalanceFromMySQL();
      },
      error: (err: any) => {
        console.error('Error loading user data from MySQL:', err);
        // Use default values instead of showing error
        this.userProfile.name = 'User';
        this.userProfile.accountNumber = 'ACC001';
        this.userProfile.email = 'user@example.com';
      }
    });
  }

  loadCurrentBalance() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    console.log('Loading current balance for account:', this.userProfile.accountNumber);
    
    // Always try to get balance from MySQL first
    if (this.userProfile.accountNumber) {
      this.loadCurrentBalanceFromMySQL();
    } else {
      console.log('No account number available, using fallback');
      // Fallback to localStorage
      const userTransactions = localStorage.getItem(`user_transactions_${this.userProfile.accountNumber}`);
      if (userTransactions) {
        const transactions = JSON.parse(userTransactions);
        if (transactions.length > 0) {
          this.currentBalance = transactions[0].balance;
          console.log('Loaded balance from localStorage:', this.currentBalance);
        }
      }
    }
  }

  loadCurrentBalanceFromMySQL() {
    if (!this.userProfile.accountNumber) {
      console.log('No account number available for MySQL balance loading');
      return;
    }
    
    console.log('Loading balance from MySQL for account:', this.userProfile.accountNumber);
    
    // Load current balance from MySQL database
    this.http.get(`${environment.apiBaseUrl}/api/accounts/balance/${this.userProfile.accountNumber}`).subscribe({
      next: (balanceData: any) => {
        console.log('Balance loaded from MySQL:', balanceData);
        this.currentBalance = balanceData.balance || 0;
        console.log('Updated currentBalance to:', this.currentBalance);
        
        // If balance is 0, check if account exists and fix it
        if (this.currentBalance === 0) {
          console.log('Balance is 0, checking account in database...');
          this.checkAndCreateAccount();
        }
      },
      error: (err: any) => {
        console.error('Error loading balance from MySQL:', err);
        console.log('Account might not exist, checking and creating...');
        this.checkAndCreateAccount();
      }
    });
  }

  loadTransferHistory() {
    if (!isPlatformBrowser(this.platformId)) return;
    const savedTransfers = localStorage.getItem(`transfer_history_${this.userProfile.accountNumber}`);
    if (savedTransfers) {
      this.transfers = JSON.parse(savedTransfers);
    }
  }

  saveTransferHistory() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem(`transfer_history_${this.userProfile.accountNumber}`, JSON.stringify(this.transfers));
  }

  // Verify recipient account number
  verifyAccountNumber() {
    if (!this.recipientAccountNumber || this.recipientAccountNumber.trim() === '') {
      this.isAccountVerified = false;
      this.accountVerificationError = '';
      this.verifiedAccountDetails = null;
      this.clearRecipientDetails();
      return;
    }

    // Don't verify if it's the same as sender account
    if (this.recipientAccountNumber === this.userProfile.accountNumber) {
      this.isAccountVerified = false;
      this.accountVerificationError = 'Cannot transfer to your own account';
      this.verifiedAccountDetails = null;
      this.clearRecipientDetails();
      return;
    }

    this.isVerifyingAccount = true;
    this.accountVerificationError = '';
    this.isAccountVerified = false;

    // Universal verify across all account types (Savings, Current, Salary)
    this.http.get(`${environment.apiBaseUrl}/api/accounts/verify-account/${this.recipientAccountNumber}`).subscribe({
      next: (res: any) => {
        if (res.found) {
          this.recipientName = res.name || '';
          this.phone = res.phone || '';
          this.ifsc = res.ifscCode || 'NEOB0001234';
          
          this.isAccountVerified = true;
          this.isVerifyingAccount = false;
          this.verifiedAccountDetails = {
            accountNumber: this.recipientAccountNumber,
            name: this.recipientName,
            phone: this.phone,
            ifsc: this.ifsc,
            accountType: res.accountType,
            bankName: res.bankName,
            businessName: res.businessName,
            companyName: res.companyName
          };
          this.accountVerificationError = '';
        } else {
          this.isAccountVerified = false;
          this.isVerifyingAccount = false;
          this.accountVerificationError = res.message || 'Account number not found. Please verify the account number.';
          this.verifiedAccountDetails = null;
          this.clearRecipientDetails();
        }
      },
      error: (err: any) => {
        this.isAccountVerified = false;
        this.isVerifyingAccount = false;
        this.accountVerificationError = 'Account number not found. Please verify the account number.';
        this.verifiedAccountDetails = null;
        this.clearRecipientDetails();
      }
    });
  }

  // Clear recipient details when account number changes
  clearRecipientDetails() {
    if (!this.isAccountVerified) {
      this.recipientName = '';
      this.phone = '';
      this.ifsc = '';
    }
  }

  // Handle account number input change
  onAccountNumberChange() {
    // Reset verification when account number changes
    if (this.isAccountVerified && this.verifiedAccountDetails?.accountNumber !== this.recipientAccountNumber) {
      this.isAccountVerified = false;
      this.verifiedAccountDetails = null;
      this.accountVerificationError = '';
      this.clearRecipientDetails();
    }
  }

  // Validate transfer amount against available balance
  validateAmount(): boolean {
    if (this.amount <= 0) {
      this.alertService.transferValidationError('Amount must be greater than 0');
      return false;
    }
    // Enforce bank limits: minimum ₹9,000 and maximum ₹9,00,00,000
    const MIN_AMOUNT = 9000;
    const MAX_AMOUNT = 90000000; // 9 crore

    if (this.amount < MIN_AMOUNT) {
      this.alertService.transferValidationError(`Minimum transfer amount is ₹${MIN_AMOUNT.toLocaleString()}`);
      return false;
    }

    if (this.amount > MAX_AMOUNT) {
      this.alertService.transferValidationError(`Maximum transfer amount is ₹${MAX_AMOUNT.toLocaleString()}`);
      return false;
    }

    if (this.amount > this.currentBalance) {
      this.alertService.transferInsufficientBalance(this.currentBalance);
      return false;
    }
    
    return true;
  }

  // Validate account before transfer
  validateAccount(): boolean {
    if (!this.recipientAccountNumber || this.recipientAccountNumber.trim() === '') {
      this.alertService.transferValidationError('Please enter recipient account number');
      return false;
    }

    if (!this.isAccountVerified) {
      this.alertService.transferValidationError('Please verify the recipient account number first');
      return false;
    }

    if (this.recipientAccountNumber === this.userProfile.accountNumber) {
      this.alertService.transferValidationError('Cannot transfer to your own account');
      return false;
    }

    return true;
  }

  // Handle fund transfer
  transferFunds() {
    // First validate account
    if (!this.validateAccount()) {
      return;
    }

    // Validate required fields
    if (!this.recipientName || !this.phone || !this.ifsc || this.amount <= 0) {
      this.alertService.transferValidationError('Please fill all fields correctly!');
      return;
    }

    if (!this.validateAmount()) {
      return;
    }

    // Show loading state
    this.transactionSuccess = false;
    this.successMessage = 'Processing transfer...';

    // Create transfer data for backend (deviceInfo for fraud detection)
    const transferData = {
      senderAccountNumber: this.userProfile.accountNumber,
      recipientAccountNumber: this.recipientAccountNumber,
      recipientName: this.recipientName,
      phone: this.phone,
      ifsc: this.ifsc,
      amount: this.amount,
      transferType: this.transferType,
      description: `${this.transferType} Transfer to ${this.recipientName}`,
      deviceInfo: isPlatformBrowser(this.platformId) ? (navigator.userAgent || 'Unknown') : undefined
    };
    // NOTE: OTP-based transfer flow is not yet implemented on the backend.
    // For now, send transfer request directly to the backend.
    this.http.post(`${environment.apiBaseUrl}/api/transfers`, transferData).subscribe({
      next: (response: any) => {
        console.log('Transfer successful:', response);
        
        // Create transfer record for local history (must use backend id for cancel/receipt)
        const now = new Date().toISOString();
        const backendId = response.id ?? response.transfer?.id;
        const newTransfer: TransferRecord = {
          id: (backendId != null && Number.isFinite(Number(backendId))) ? Number(backendId) : Date.now(),
          transferId: response.transfer?.transferId || 'TRF' + Date.now() + Math.floor(Math.random() * 1000),
          senderAccountNumber: this.userProfile.accountNumber,
          senderName: this.userProfile.name,
          recipientAccountNumber: this.recipientAccountNumber,
          recipientName: this.recipientName,
          phone: this.phone,
          ifsc: this.ifsc,
          amount: this.amount,
          transferType: this.transferType,
          status: 'Completed',
          date: now,
          createdAt: now,
          updatedAt: now,
          isCancellable: true
        };

        // Add to transfer history
        this.transfers.unshift(newTransfer);
        this.saveTransferHistory();

        // Update balance from response
        if (response.newBalance !== undefined) {
          this.currentBalance = response.newBalance;
        }

        // Show success message with timestamp
        this.alertService.transferSuccess(newTransfer.amount, newTransfer.recipientName);
        this.showSuccessMessage(newTransfer);

        // Reset form
        this.resetForm();
      },
      error: (err: any) => {
        console.error('Transfer failed:', err);
        this.alertService.transferError(err.error?.message || 'Unknown error');
        this.transactionSuccess = false;
        this.successMessage = '';
      }
    });
  }

  // OTP helpers
  sendOtpForTransfer(transferData: any) {
    if (this.otpSending) return;
    this.otpSending = true;
    this.otpSent = false;
    this.otpCode = '';
    this.otpTxnId = null;
    this.http.post(`${environment.apiBaseUrl}/api/transfers/send-otp`, transferData).subscribe({
      next: (res: any) => {
        this.otpSending = false;
        if (res && (res.success || res.otpTxnId)) {
          this.otpTxnId = res.otpTxnId || res.txnId || res.id || null;
          this.otpSent = true;
          this.showOtpModal = true;
          this.startOtpTimer(120); // 2 minutes
          this.resendCount = 0;
        } else {
          this.alertService.transferError(res.message || 'Failed to send OTP');
        }
      },
      error: (err: any) => {
        console.error('Error sending OTP:', err);
        this.otpSending = false;
        this.alertService.transferError(err.error?.message || 'Failed to send OTP');
      }
    });
  }

  startOtpTimer(seconds: number) {
    this.otpTimer = seconds;
    const tick = () => {
      if (this.otpTimer > 0) {
        this.otpTimer -= 1;
        setTimeout(tick, 1000);
      }
    };
    tick();
  }

  resendOtp(transferData?: any) {
    if (this.otpTimer > 0 && this.resendCount >= 1) {
      // prevent frequent resends
      this.alertService.transferValidationError('Please wait before resending OTP');
      return;
    }
    this.resendCount++;
    // Call resend endpoint
    this.otpSending = true;
    const payload: any = { txnId: this.otpTxnId };
    this.http.post(`${environment.apiBaseUrl}/api/transfers/resend-otp`, payload).subscribe({
      next: (res: any) => {
        this.otpSending = false;
        if (res && (res.success || res.otpTxnId)) {
          this.otpSent = true;
          this.startOtpTimer(120);
        } else {
          this.alertService.transferError(res.message || 'Failed to resend OTP');
        }
      },
      error: (err: any) => {
        this.otpSending = false;
        console.error('Error resending OTP:', err);
        this.alertService.transferError(err.error?.message || 'Failed to resend OTP');
      }
    });
  }

  verifyOtpAndTransfer(transferData?: any) {
    if (this.verifyingOtp) return;
    if (!this.otpTxnId) {
      this.alertService.transferValidationError('No OTP transaction found');
      return;
    }
    if (!this.otpCode || this.otpCode.trim().length === 0) {
      this.alertService.transferValidationError('Please enter the OTP');
      return;
    }
    this.verifyingOtp = true;
    const payload = { txnId: this.otpTxnId, otp: this.otpCode };
    this.http.post(`${environment.apiBaseUrl}/api/transfers/confirm-otp`, payload).subscribe({
      next: (res: any) => {
        this.verifyingOtp = false;
        if (res && res.success) {
          // Proceed to perform actual transfer with confirmation reference
          const confirmedTransfer = transferData || (res.transferData || {});
          if (!confirmedTransfer || Object.keys(confirmedTransfer).length === 0) {
            // If transferData not supplied, ask backend to complete with txnId
            this.http.post(`${environment.apiBaseUrl}/api/transfers/complete`, { txnId: this.otpTxnId }).subscribe({
              next: (resp: any) => {
                this.showOtpModal = false;
                this.otpSent = false;
                this.otpTxnId = null;
                this.alertService.transferSuccess(resp.amount || 0, resp.recipientName || '');
                this.loadCurrentBalance();
              },
              error: (err: any) => {
                console.error('Error completing transfer:', err);
                this.alertService.transferError(err.error?.message || 'Transfer failed after OTP');
              }
            });
          } else {
            // Call regular transfer endpoint with otpTxnId reference
            confirmedTransfer.otpTxnId = this.otpTxnId;
            this.http.post(`${environment.apiBaseUrl}/api/transfers`, confirmedTransfer).subscribe({
              next: (response: any) => {
                this.showOtpModal = false;
                this.otpSent = false;
                this.otpTxnId = null;
                const now = new Date().toISOString();
                const backendId = response.id ?? response.transfer?.id;
                const newTransfer: TransferRecord = {
                  id: (backendId != null && Number.isFinite(Number(backendId))) ? Number(backendId) : Date.now(),
                  transferId: response.transfer?.transferId || 'TRF' + Date.now() + Math.floor(Math.random() * 1000),
                  senderAccountNumber: this.userProfile.accountNumber,
                  senderName: this.userProfile.name,
                  recipientAccountNumber: this.recipientAccountNumber,
                  recipientName: this.recipientName,
                  phone: this.phone,
                  ifsc: this.ifsc,
                  amount: this.amount,
                  transferType: this.transferType,
                  status: 'Completed',
                  date: now,
                  createdAt: now,
                  updatedAt: now,
                  isCancellable: true
                };
                this.transfers.unshift(newTransfer);
                this.saveTransferHistory();
                if (response.newBalance !== undefined) this.currentBalance = response.newBalance;
                this.alertService.transferSuccess(newTransfer.amount, newTransfer.recipientName);
                this.showSuccessMessage(newTransfer);
                this.resetForm();
              },
              error: (err: any) => {
                console.error('Transfer failed after OTP:', err);
                this.alertService.transferError(err.error?.message || 'Transfer failed');
              }
            });
          }
        } else {
          this.alertService.transferError(res.message || 'OTP verification failed');
        }
      },
      error: (err: any) => {
        this.verifyingOtp = false;
        console.error('Error verifying OTP:', err);
        this.alertService.transferError(err.error?.message || 'OTP verification failed');
      }
    });
  }

  createTransferTransaction(transfer: TransferRecord) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const userTransactions = localStorage.getItem(`user_transactions_${this.userProfile.accountNumber}`);
    let transactions = userTransactions ? JSON.parse(userTransactions) : [];
    
    // Calculate new balance
    const newBalance = this.currentBalance - transfer.amount;
    
    // Create debit transaction
    const transferTransaction: TransactionRecord = {
      id: 'TXN' + Date.now() + Math.floor(Math.random() * 1000),
      merchant: `${transfer.transferType} Transfer to ${transfer.recipientName}`,
      amount: transfer.amount,
      type: 'Debit',
      balance: newBalance,
      date: new Date().toISOString(),
      description: `${transfer.transferType} Transfer - To: ${transfer.recipientAccountNumber} (${transfer.recipientName})`
    };

    // Add transaction to the beginning of the list
    transactions.unshift(transferTransaction);
    
    // Save updated transactions
    localStorage.setItem(`user_transactions_${this.userProfile.accountNumber}`, JSON.stringify(transactions));
    
    // Update current balance
    this.currentBalance = newBalance;
  }

  showSuccessMessage(transfer?: TransferRecord) {
    this.transactionSuccess = true;
    
    const timestamp = new Date().toLocaleString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    
    if (transfer) {
      this.successMessage = `✅ Transfer Successful!
      
Amount: ₹${transfer.amount.toLocaleString()}
To: ${transfer.recipientName} (${transfer.recipientAccountNumber})
Type: ${transfer.transferType}
Transaction ID: ${transfer.id}
Time: ${timestamp}
New Balance: ₹${this.currentBalance.toLocaleString()}`;
    } else {
      this.successMessage = `Transfer of ₹${this.amount.toLocaleString()} to ${this.recipientName} completed successfully!
Time: ${timestamp}`;
    }
    
    // Hide success message after 8 seconds
    setTimeout(() => {
      this.transactionSuccess = false;
      this.successMessage = '';
    }, 8000);
  }

  resetForm() {
    this.recipientAccountNumber = '';
    this.recipientName = '';
    this.phone = '';
    this.ifsc = '';
    this.amount = 0;
    this.transferType = 'NEFT';
    this.isAccountVerified = false;
    this.verifiedAccountDetails = null;
    this.accountVerificationError = '';
  }

  // Set maximum amount to available balance
  setMaxAmount() {
    this.amount = this.currentBalance;
  }

  // Back to dashboard
  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  // Format currency
  formatCurrency(amount: number): string {
    return '₹' + amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  // Format date
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString('en-IN');
  }

  // Refresh balance manually
  refreshBalance() {
    console.log('Manually refreshing balance...');
    this.loadCurrentBalance();
  }

  // Check and create account with proper balance
  checkAndCreateAccount() {
    console.log('Checking account in database...');
    
    // First check if account exists
    this.http.get(`${environment.apiBaseUrl}/api/accounts/number/${this.userProfile.accountNumber}`).subscribe({
      next: (accountData: any) => {
        console.log('Account found in database:', accountData);
        if (accountData.balance === 0 || accountData.balance === null) {
          console.log('Account has 0 balance, updating to 2 lakhs...');
          this.updateAccountBalance(200000);
        } else {
          console.log('Account balance:', accountData.balance);
          this.currentBalance = accountData.balance;
        }
      },
      error: (err: any) => {
        console.log('Account not found, creating new account with 2 lakhs...');
        this.createAccountWithBalance();
      }
    });
  }

  updateAccountBalance(amount: number) {
    this.http.put(`${environment.apiBaseUrl}/api/accounts/balance/update/${this.userProfile.accountNumber}?amount=${amount}`, {}).subscribe({
      next: (response: any) => {
        console.log('Balance updated successfully:', response);
        this.currentBalance = response.balance;
      },
      error: (err: any) => {
        console.error('Error updating balance:', err);
      }
    });
  }

  createAccountWithBalance() {
    const accountData = {
      accountNumber: this.userProfile.accountNumber,
      name: this.userProfile.name,
      balance: 200000,
      accountType: 'Savings Account',
      status: 'ACTIVE'
    };

    this.http.post(`${environment.apiBaseUrl}/api/accounts/create`, accountData).subscribe({
      next: (response: any) => {
        console.log('Account created with 2 lakhs balance:', response);
        this.currentBalance = response.balance;
      },
      error: (err: any) => {
        console.error('Error creating account:', err);
      }
    });
  }

  // Check if transfer can be cancelled (within 3 minutes for NEFT/IMPS)
  canCancelTransfer(transfer: TransferRecord): boolean {
    if (transfer.status !== 'Completed' || !transfer.isCancellable) {
      return false;
    }

    // Only NEFT and IMPS can be cancelled (matches backend)
    if (transfer.transferType !== 'NEFT' && transfer.transferType !== 'IMPS') {
      return false;
    }

    // Check if within 3 minutes
    const transferDate = new Date(transfer.createdAt || transfer.date);
    const threeMinutesAgo = new Date(Date.now() - 3 * 60 * 1000);
    
    return transferDate > threeMinutesAgo;
  }

  // Cancel transfer
  cancelTransfer(transfer: TransferRecord) {
    if (!this.canCancelTransfer(transfer)) {
      this.alertService.transferValidationError('This transfer cannot be cancelled. Only NEFT/IMPS transfers within 3 minutes can be cancelled.');
      return;
    }

    const id = Number(transfer.id);
    if (!Number.isFinite(id) || id < 1) {
      this.alertService.transferError('Invalid transfer. Cannot cancel.');
      return;
    }

    this.alertService.transferConfirm(
      'Cancel Transfer',
      `Are you sure you want to cancel the transfer of ₹${transfer.amount.toLocaleString()} to ${transfer.recipientName}?`,
      () => {
        this.http.put(`${environment.apiBaseUrl}/api/transfers/${id}/cancel`, {}).subscribe({
          next: (response: any) => {
            console.log('Transfer cancelled successfully:', response);

            const index = this.transfers.findIndex(t => t.id === transfer.id);
            if (index !== -1) {
              this.transfers[index].status = 'Cancelled';
              this.transfers[index].isCancellable = false;
            }

            if (response.newSenderBalance !== undefined) {
              this.currentBalance = response.newSenderBalance;
            }

            this.alertService.transferSuccess(transfer.amount, 'your account (cancelled)');
            this.saveTransferHistory();
          },
          error: (err: any) => {
            console.error('Error cancelling transfer:', err);
            this.alertService.transferError(err.error?.error || 'Unknown error');
          }
        });
      },
      undefined,
      'Yes, Cancel Transfer',
      'No, Keep'
    );
  }

  // Download transfer receipt as PDF
  downloadReceipt(transfer: TransferRecord) {
    console.log('Downloading receipt for transfer:', transfer);
    console.log('Transfer ID:', transfer.id);
    
    this.http.get(`${environment.apiBaseUrl}/api/transfers/${transfer.id}/receipt`, {
      responseType: 'blob'
    }).subscribe({
      next: (blob: Blob) => {
        console.log('PDF blob received:', blob);
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `transfer_receipt_${transfer.transferId || transfer.id}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      },
      error: (err: any) => {
        console.error('Error downloading receipt:', err);
        console.error('Error details:', err.error);
        console.error('Error status:', err.status);
        this.alertService.transferError(`Receipt download failed: ${err.status} - ${err.error?.error || err.message || 'Unknown error'}`);
      }
    });
  }
}