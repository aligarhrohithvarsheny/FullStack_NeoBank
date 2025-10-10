import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface TransferRecord {
  id: string;
  senderAccountNumber: string;
  senderName: string;
  recipientAccountNumber: string;
  recipientName: string;
  phone: string;
  ifsc: string;
  amount: number;
  transferType: 'NEFT' | 'RTGS';
  status: 'Pending' | 'Completed' | 'Failed';
  date: string;
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
    private http: HttpClient
  ) {}

  ngOnInit() {
    console.log('TransferFunds component initialized');
    this.loadUserProfile();
    this.loadCurrentBalance();
    this.loadTransferHistory();
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
    this.http.get(`http://localhost:8080/api/users/${userId}`).subscribe({
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
    this.http.get(`http://localhost:8080/api/accounts/balance/${this.userProfile.accountNumber}`).subscribe({
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

  // Validate transfer amount against available balance
  validateAmount(): boolean {
    if (this.amount <= 0) {
      alert('Amount must be greater than 0');
      return false;
    }
    
    if (this.amount > this.currentBalance) {
      alert(`Insufficient balance! Available balance: ₹${this.currentBalance.toLocaleString()}`);
      return false;
    }
    
    return true;
  }

  // Handle fund transfer
  transferFunds() {
    if (!this.recipientAccountNumber || !this.recipientName || !this.phone || !this.ifsc || this.amount <= 0) {
      alert('Please fill all fields correctly!');
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
    this.http.post('http://localhost:8080/api/transfers', transferData).subscribe({
      next: (response: any) => {
        console.log('Transfer successful:', response);
        
        // Create transfer record for local history
        const newTransfer: TransferRecord = {
          id: response.id || 'TRF' + Date.now() + Math.floor(Math.random() * 1000),
          senderAccountNumber: this.userProfile.accountNumber,
          senderName: this.userProfile.name,
          recipientAccountNumber: this.recipientAccountNumber,
          recipientName: this.recipientName,
          phone: this.phone,
          ifsc: this.ifsc,
          amount: this.amount,
          transferType: this.transferType,
          status: 'Completed',
          date: new Date().toISOString()
        };

        // Add to transfer history
        this.transfers.unshift(newTransfer);
        this.saveTransferHistory();

        // Update balance from response
        if (response.newBalance !== undefined) {
          this.currentBalance = response.newBalance;
        }

        // Show success message with timestamp
        this.showSuccessMessage(newTransfer);

        // Reset form
        this.resetForm();
      },
      error: (err: any) => {
        console.error('Transfer failed:', err);
        alert(`Transfer failed: ${err.error?.message || 'Unknown error'}`);
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
    this.http.get(`http://localhost:8080/api/accounts/number/${this.userProfile.accountNumber}`).subscribe({
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
    this.http.put(`http://localhost:8080/api/accounts/balance/update/${this.userProfile.accountNumber}?amount=${amount}`, {}).subscribe({
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

    this.http.post('http://localhost:8080/api/accounts/create', accountData).subscribe({
      next: (response: any) => {
        console.log('Account created with 2 lakhs balance:', response);
        this.currentBalance = response.balance;
      },
      error: (err: any) => {
        console.error('Error creating account:', err);
      }
    });
  }
}