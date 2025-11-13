import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
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
    private http: HttpClient,
    private alertService: AlertService
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
    this.alertService.logoutSuccess();
    this.router.navigate(['/admin/login']);
  }

  loadUsers() {
    // Load users from MySQL database
    this.http.get(`${environment.apiUrl}/users`).subscribe({
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
    this.http.get(`${environment.apiUrl}/users/account/${user.accountNumber}`).subscribe({
      next: (dbUser: any) => {
        if (dbUser) {
          const updatedUser = {
            ...dbUser,
            account: {
              ...dbUser.account,
              balance: user.balance
            }
          };
          
          this.http.put(`${environment.apiUrl}/users/update/${dbUser.id}`, updatedUser).subscribe({
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
    this.http.get(`${environment.apiUrl}/cards`).subscribe({
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
    this.http.get(`${environment.apiUrl}/cards/account/${user.accountNumber}`).subscribe({
      next: (cards: any) => {
        this.userCards = cards || [];
      },
      error: (err: any) => {
        console.error('Error loading user cards:', err);
        this.userCards = [];
      }
    });

    // Load user loans
    this.http.get(`${environment.apiUrl}/loans/account/${user.accountNumber}`).subscribe({
      next: (loans: any) => {
        this.userLoans = loans || [];
      },
      error: (err: any) => {
        console.error('Error loading user loans:', err);
        this.userLoans = [];
      }
    });

    // Load user transactions
    this.http.get(`${environment.apiUrl}/transactions/account/${user.accountNumber}`).subscribe({
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
}
