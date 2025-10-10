import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

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

  // Search functionality
  searchQuery: string = '';
  searchResults: UserProfile[] = [];
  selectedSearchedUser: UserProfile | null = null;
  isSearching: boolean = false;
  userCards: CardDetails[] = [];
  userLoans: LoanDetails[] = [];
  userTransactions: TransactionDetails[] = [];

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.loadUsers();
  }

  // Refresh users data from database
  refreshUsersData() {
    console.log('Refreshing users data from database...');
    this.loadUsers();
  }

  navigateTo(path: string) {
    this.router.navigate([`/admin/${path}`]);
  }

  logout() {
    alert('Logged out successfully 🚪');
    this.router.navigate(['/admin/login']);
  }

  loadUsers() {
    // Load users from MySQL database
    this.http.get('http://localhost:8080/api/users').subscribe({
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
  }

  validateAmount(): boolean {
    if (this.amount <= 0) {
      this.errorMessage = 'Amount must be greater than 0';
      return false;
    }

    if (this.operationType === 'withdrawal' && this.selectedUser) {
      if (this.amount > this.selectedUser.balance) {
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
    if (!this.selectedUser) {
      this.errorMessage = 'Please select a user';
      return;
    }

    if (!this.validateAmount()) {
      return;
    }

    if (!this.description.trim()) {
      this.errorMessage = 'Please enter a description';
      return;
    }

    // Update user balance
    const balanceChange = this.operationType === 'deposit' ? this.amount : -this.amount;
    this.selectedUser.balance += balanceChange;

    // Update users array
    const userIndex = this.users.findIndex(u => u.id === this.selectedUser!.id);
    if (userIndex !== -1) {
      this.users[userIndex] = { ...this.selectedUser };
    }

    // Save updated users
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('userProfiles', JSON.stringify(this.users));
    }

    // Create transaction record
    const transaction: TransactionRecord = {
      id: Date.now().toString(),
      transactionId: `TXN${Date.now()}`,
      merchant: this.operationType === 'deposit' ? 'Admin Deposit' : 'Admin Withdrawal',
      amount: this.amount,
      type: this.operationType === 'deposit' ? 'Credit' : 'Debit',
      description: this.description,
      balance: this.selectedUser.balance,
      date: new Date().toISOString(),
      status: 'Completed',
      userName: this.selectedUser.name,
      accountNumber: this.selectedUser.accountNumber
    };

    // Save transaction to user's transaction history
    this.saveUserTransaction(transaction);

    // Save transaction to admin transaction history
    this.saveAdminTransaction(transaction);

    // Show success message
    this.successMessage = `${this.operationType === 'deposit' ? 'Deposit' : 'Withdrawal'} of ₹${this.amount} successful!`;
    this.errorMessage = '';

    // Reset form after 2 seconds
    setTimeout(() => {
      this.resetForm();
    }, 2000);
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

  saveAdminTransaction(transaction: TransactionRecord) {
    if (isPlatformBrowser(this.platformId)) {
      const adminTransactionsKey = 'adminTransactions';
      const existingTransactions = localStorage.getItem(adminTransactionsKey);
      const transactions = existingTransactions ? JSON.parse(existingTransactions) : [];
      
      transactions.unshift(transaction);
      localStorage.setItem(adminTransactionsKey, JSON.stringify(transactions));
    }
  }

  closeDepositWithdrawal() {
    this.showDepositWithdrawal = false;
    this.selectedUser = null;
    this.resetForm();
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

  processDirectTransaction() {
    if (!this.selectedUser) {
      this.errorMessage = 'Please select a user';
      return;
    }

    if (!this.validateAmount()) {
      return;
    }

    if (!this.description.trim()) {
      this.errorMessage = 'Please enter a description';
      return;
    }

    // Update user balance
    const balanceChange = this.operationType === 'deposit' ? this.amount : -this.amount;
    this.selectedUser.balance += balanceChange;

    // Update users array
    const userIndex = this.users.findIndex(u => u.id === this.selectedUser!.id);
    if (userIndex !== -1) {
      this.users[userIndex] = { ...this.selectedUser };
    }

    // Save updated users
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('userProfiles', JSON.stringify(this.users));
    }

    // Create transaction record
    const transaction: TransactionRecord = {
      id: Date.now().toString(),
      transactionId: `TXN${Date.now()}`,
      merchant: this.operationType === 'deposit' ? 'Admin Deposit' : 'Admin Withdrawal',
      amount: this.amount,
      type: this.operationType === 'deposit' ? 'Credit' : 'Debit',
      description: this.description,
      balance: this.selectedUser.balance,
      date: new Date().toISOString(),
      status: 'Completed',
      userName: this.selectedUser.name,
      accountNumber: this.selectedUser.accountNumber
    };

    // Save transaction to user's transaction history
    this.saveUserTransaction(transaction);

    // Save transaction to admin transaction history
    this.saveAdminTransaction(transaction);

    // Show success message
    this.successMessage = `${this.operationType === 'deposit' ? 'Deposit' : 'Withdrawal'} of ₹${this.amount} successful!`;
    this.errorMessage = '';

    // Reset form after 3 seconds
    setTimeout(() => {
      this.clearForm();
    }, 3000);
  }

  clearForm() {
    this.selectedUserId = '';
    this.selectedUser = null;
    this.amount = 0;
    this.description = '';
    this.successMessage = '';
    this.errorMessage = '';
  }

  // Update user balance in MySQL database
  updateUserBalanceInDatabase(user: UserProfile) {
    // Find user by account number
    this.http.get(`http://localhost:8080/api/users/account/${user.accountNumber}`).subscribe({
      next: (dbUser: any) => {
        if (dbUser) {
          const updatedUser = {
            ...dbUser,
            account: {
              ...dbUser.account,
              balance: user.balance
            }
          };
          
          this.http.put(`http://localhost:8080/api/users/update/${dbUser.id}`, updatedUser).subscribe({
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
    this.http.get('http://localhost:8080/api/cards').subscribe({
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

  selectSearchedUser(user: UserProfile) {
    this.selectedSearchedUser = user;
    this.loadUserCompleteDetails(user);
  }

  loadUserCompleteDetails(user: UserProfile) {
    // Load user cards
    this.http.get(`http://localhost:8080/api/cards/account/${user.accountNumber}`).subscribe({
      next: (cards: any) => {
        this.userCards = cards || [];
      },
      error: (err: any) => {
        console.error('Error loading user cards:', err);
        this.userCards = [];
      }
    });

    // Load user loans
    this.http.get(`http://localhost:8080/api/loans/account/${user.accountNumber}`).subscribe({
      next: (loans: any) => {
        this.userLoans = loans || [];
      },
      error: (err: any) => {
        console.error('Error loading user loans:', err);
        this.userLoans = [];
      }
    });

    // Load user transactions
    this.http.get(`http://localhost:8080/api/transactions/account/${user.accountNumber}`).subscribe({
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
}
