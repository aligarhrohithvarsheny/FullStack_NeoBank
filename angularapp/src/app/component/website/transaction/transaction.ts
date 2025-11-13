import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface TransactionRecord {
  id: string;
  merchant: string;
  amount: number;
  type: 'Debit' | 'Credit' | 'Loan Credit';
  balance: number;
  date: string;
  description?: string;
}

@Component({
  selector: 'app-transaction',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction.html',
  styleUrls: ['./transaction.css']
})
export class Transaction implements OnInit {
  transactions: TransactionRecord[] = [];
  userAccountNumber: string = '';
  currentBalance: number = 0;
  loading: boolean = false;
  error: string = '';

  // Form fields for adding new transaction
  merchant: string = '';
  amount: number = 0;
  type: 'Debit' | 'Credit' | 'Loan Credit' = 'Debit';
  
  // Search and filter fields
  searchTerm: string = '';
  fromDate: string = '';
  toDate: string = '';

  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.loadUserAccount();
  }

  // Navigate back to dashboard
  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  loadUserAccount() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    this.error = '';
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.userAccountNumber = user.accountNumber;
      
      // Load transactions from MySQL database
      this.loadTransactionsFromMySQL();
    } else {
      // No session found - use default values to avoid errors
      this.userAccountNumber = 'ACC001';
      this.loading = false;
      console.log('Using default values - no session found');
    }
  }

  loadTransactionsFromMySQL() {
    if (!this.userAccountNumber) {
      this.loading = false;
      return;
    }
    
    // Load transactions from MySQL database
    this.http.get(`${environment.apiUrl}/transactions/account/${this.userAccountNumber}?page=0&size=100`).subscribe({
      next: (response: any) => {
        console.log('Transactions response from MySQL:', response);
        
        // Handle both Page response and direct array response
        let transactions = [];
        if (response.content) {
          // Page response
          transactions = response.content;
        } else if (Array.isArray(response)) {
          // Direct array response
          transactions = response;
        }
        
        this.transactions = transactions.map((txn: any) => ({
          id: txn.id,
          merchant: txn.merchant || txn.description,
          amount: txn.amount,
          type: txn.type,
          balance: txn.balance,
          date: txn.date || txn.createdAt,
          description: txn.description
        }));
        
        // Update current balance from latest transaction
        if (this.transactions.length > 0) {
          this.currentBalance = this.transactions[0].balance;
        }
        
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading transactions from MySQL:', err);
        this.error = 'Failed to load transactions. Please try again.';
        this.loading = false;
      }
    });
  }

  addTransaction() {
    if (!this.merchant || this.amount <= 0) {
      alert('Please enter valid merchant name and amount');
      return;
    }

    if (this.type === 'Debit' && this.amount > this.currentBalance) {
      alert('Insufficient balance');
      return;
    }

    if (!this.userAccountNumber) {
      alert('No account number found');
      return;
    }

    this.loading = true;

    const transactionData = {
      merchant: this.merchant,
      amount: this.amount,
      type: this.type,
      description: `${this.type} transaction`,
      balance: this.type === 'Debit' ? this.currentBalance - this.amount : this.currentBalance + this.amount,
      date: new Date().toISOString(),
      accountNumber: this.userAccountNumber,
    };

    // Submit to MySQL database
    this.http.post('${environment.apiUrl}/transactions', transactionData).subscribe({
      next: (response: any) => {
        console.log('Transaction created successfully in MySQL:', response);
        this.currentBalance = transactionData.balance;
        this.transactions.unshift({
          id: response.id || 'TXN' + Date.now(),
          merchant: this.merchant,
          amount: this.amount,
          type: this.type,
          balance: this.currentBalance,
          date: transactionData.date,
          description: transactionData.description
        });
        
        alert('Transaction completed successfully!');
        this.merchant = '';
        this.amount = 0;
        this.type = 'Debit';
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error creating transaction:', err);
        alert('Failed to process transaction. Please try again.');
        this.loading = false;
      }
    });
  }

  get filteredTransactions() {
    if (!this.transactions) return [];
    return this.transactions.filter(tx => {
      const txDate = new Date(tx.date);
      const now = new Date();
      const thirtyDaysAgo = new Date(now.getTime() - (30 * 24 * 60 * 60 * 1000));
      return txDate >= thirtyDaysAgo;
    });
  }

  get miniStatement() {
    if (!this.transactions) return [];
    return this.transactions.slice(0, 5);
  }

  downloadStatement() {
    if (!isPlatformBrowser(this.platformId)) {
      console.log('Not in browser environment, skipping CSV download');
      return;
    }
    
    try {
      // Check if we have transactions
      if (!this.transactions || this.transactions.length === 0) {
        alert('No transactions available to download');
        return;
      }

      // Check if userAccountNumber is available
      if (!this.userAccountNumber) {
        alert('Account number not found. Please refresh the page and try again.');
        return;
      }
      
      const csvContent = this.generateCSVContent();
      if (!csvContent || csvContent.trim() === '') {
        alert('No transactions available to download');
        return;
      }
      
      // Create blob with proper encoding
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      
      // Check if URL.createObjectURL is supported
      if (!window.URL || !window.URL.createObjectURL) {
        alert('Your browser does not support file downloads. Please try a different browser.');
        return;
      }
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `statement_${this.userAccountNumber}_${new Date().toISOString().split('T')[0]}.csv`;
      link.style.display = 'none';
      
      document.body.appendChild(link);
      link.click();
      
      // Clean up
      setTimeout(() => {
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      }, 100);
      
      console.log('CSV download initiated successfully');
    } catch (error) {
      console.error('Error downloading CSV:', error);
      alert('Failed to download CSV. Please try again.');
    }
  }

  cleanupDuplicateLoanTransactions() {
    // This method is kept for compatibility with the template
    // In a real-time MySQL system, duplicates are handled at the database level
    console.log('Cleanup method called - handled by MySQL database');
  }

  private generateCSVContent(): string {
    if (!this.transactions || this.transactions.length === 0) {
      return '';
    }

    try {
      const headers = ['Date', 'Merchant', 'Type', 'Amount (₹)', 'Balance (₹)', 'Description'];
      
      // Use all transactions, not just filtered ones for CSV
      const rows = this.transactions.map(tx => {
        // Safe date parsing
        let formattedDate = 'N/A';
        try {
          if (tx.date) {
            const date = new Date(tx.date);
            if (!isNaN(date.getTime())) {
              formattedDate = date.toLocaleDateString('en-IN');
            }
          }
        } catch (e) {
          console.warn('Error formatting date:', tx.date, e);
        }
        
        // Safe number formatting
        let formattedAmount = '0';
        let formattedBalance = '0';
        try {
          const amount = Number(tx.amount);
          const balance = Number(tx.balance);
          
          if (!isNaN(amount)) {
            formattedAmount = amount.toLocaleString('en-IN');
          }
          if (!isNaN(balance)) {
            formattedBalance = balance.toLocaleString('en-IN');
          }
        } catch (e) {
          console.warn('Error formatting numbers:', e);
        }
        
        return [
          `"${formattedDate}"`,
          `"${(tx.merchant || 'N/A').replace(/"/g, '""')}"`,
          `"${(tx.type || 'N/A').replace(/"/g, '""')}"`,
          `"${formattedAmount}"`,
          `"${formattedBalance}"`,
          `"${(tx.description || 'N/A').replace(/"/g, '""')}"`
        ];
      });
      
      // Add bank header information
      const bankInfo = [
        `"NeoBank - Account Statement"`,
        `"Account Number: ${this.userAccountNumber || 'N/A'}"`,
        `"Generated on: ${new Date().toLocaleDateString('en-IN')} at ${new Date().toLocaleTimeString('en-IN')}"`,
        `"Total Transactions: ${this.transactions.length}"`,
        `"Current Balance: ₹${this.currentBalance ? this.currentBalance.toLocaleString('en-IN') : '0'}"`,
        `""` // Empty line
      ];
      
      const csvContent = [
        ...bankInfo,
        headers.map(h => `"${h}"`).join(','),
        ...rows.map(row => row.join(','))
      ].join('\n');
      
      return csvContent;
    } catch (error) {
      console.error('Error generating CSV content:', error);
      return '';
    }
  }
}