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
  transferType: 'NEFT' | 'RTGS';
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
  transferType: 'NEFT' | 'RTGS' = 'NEFT';

  // Account verification
  isAccountVerified: boolean = false;
  isVerifyingAccount: boolean = false;
  accountVerificationError: string = '';
  verifiedAccountDetails: any = null;

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
    this.http.get(`${environment.apiBaseUrl}/users/${userId}`).subscribe({
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
    this.http.get(`${environment.apiBaseUrl}/accounts/balance/${this.userProfile.accountNumber}`).subscribe({
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

    // First try to get account details
    this.http.get(`${environment.apiBaseUrl}/accounts/number/${this.recipientAccountNumber}`).subscribe({
      next: (accountData: any) => {
        console.log('Account found:', accountData);
        
        // If account exists, try to get user details for name
        this.http.get(`${environment.apiBaseUrl}/users/account/${this.recipientAccountNumber}`).subscribe({
          next: (userData: any) => {
            console.log('User data found:', userData);
            
            // Auto-fill with account and user data
            this.recipientName = userData.account?.name || accountData.name || '';
            this.phone = accountData.phone || userData.account?.phone || '';
            this.ifsc = 'NEOB0001234'; // Default IFSC for NeoBank accounts
            
            this.isAccountVerified = true;
            this.isVerifyingAccount = false;
            this.verifiedAccountDetails = {
              accountNumber: this.recipientAccountNumber,
              name: this.recipientName,
              phone: this.phone,
              ifsc: this.ifsc,
              account: accountData,
              user: userData
            };
            this.accountVerificationError = '';
            
            console.log('Account verified and details auto-filled');
          },
          error: (userErr: any) => {
            // If user not found, use account data only
            console.log('User not found, using account data only');
            this.recipientName = accountData.name || '';
            this.phone = accountData.phone || '';
            this.ifsc = 'NEOB0001234';
            
            this.isAccountVerified = true;
            this.isVerifyingAccount = false;
            this.verifiedAccountDetails = {
              accountNumber: this.recipientAccountNumber,
              name: this.recipientName,
              phone: this.phone,
              ifsc: this.ifsc,
              account: accountData
            };
            this.accountVerificationError = '';
          }
        });
      },
      error: (err: any) => {
        console.error('Account not found:', err);
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

    // Create transfer data for backend
    const transferData = {
      senderAccountNumber: this.userProfile.accountNumber,
      recipientAccountNumber: this.recipientAccountNumber,
      recipientName: this.recipientName,
      phone: this.phone,
      ifsc: this.ifsc,
      amount: this.amount,
      transferType: this.transferType,
      description: `${this.transferType} Transfer to ${this.recipientName}`
    };

    // Send transfer request to backend
    this.http.post(`${environment.apiBaseUrl}/transfers`, transferData).subscribe({
      next: (response: any) => {
        console.log('Transfer successful:', response);
        
        // Create transfer record for local history
        const now = new Date().toISOString();
        const newTransfer: TransferRecord = {
          id: response.id || Date.now(), // Use database ID from backend
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
    this.http.get(`${environment.apiBaseUrl}/accounts/number/${this.userProfile.accountNumber}`).subscribe({
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
    this.http.put(`${environment.apiBaseUrl}/accounts/balance/update/${this.userProfile.accountNumber}?amount=${amount}`, {}).subscribe({
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

    this.http.post(`${environment.apiBaseUrl}/accounts/create`, accountData).subscribe({
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

    // Only NEFT can be cancelled (assuming IMPS is similar to NEFT)
    if (transfer.transferType !== 'NEFT') {
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

    this.alertService.transferConfirm(
      'Cancel Transfer',
      `Are you sure you want to cancel the transfer of ₹${transfer.amount.toLocaleString()} to ${transfer.recipientName}?`,
      () => {

    this.http.put(`${environment.apiBaseUrl}/transfers/${transfer.id}/cancel`, {}).subscribe({
      next: (response: any) => {
        console.log('Transfer cancelled successfully:', response);
        
        // Update the transfer status in the local array
        const index = this.transfers.findIndex(t => t.id === transfer.id);
        if (index !== -1) {
          this.transfers[index].status = 'Cancelled';
          this.transfers[index].isCancellable = false;
        }

        // Update balance if provided
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
      }
    );
  }

  // Download transfer receipt as PDF
  downloadReceipt(transfer: TransferRecord) {
    console.log('Downloading receipt for transfer:', transfer);
    console.log('Transfer ID:', transfer.id);
    
    this.http.get(`${environment.apiBaseUrl}/transfers/${transfer.id}/receipt`, {
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