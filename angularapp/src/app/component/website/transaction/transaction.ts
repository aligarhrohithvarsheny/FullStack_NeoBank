import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { isPlatformBrowser } from '@angular/common';

interface TransactionRecord {
  id: string;
  merchant: string;
  amount: number;
  type: 'Debit' | 'Credit' | 'Loan Credit';
  balance: number;
  date: Date;
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
  currentBalance: number = 50000; // initial balance
  merchant: string = '';
  amount: number = 0;
  type: 'Debit' | 'Credit' = 'Debit';
  searchTerm: string = '';
  fromDate: string = '';
  toDate: string = '';
  userAccountNumber: string = 'ACC001'; // Should match with loan component

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    this.loadTransactions();
    this.cleanupDuplicateLoanTransactions();
    this.checkForLoanApprovals();
  }

  // Navigate back to dashboard
  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  loadTransactions() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load transactions from localStorage
    const savedTransactions = localStorage.getItem(`user_transactions_${this.userAccountNumber}`);
    if (savedTransactions) {
      this.transactions = JSON.parse(savedTransactions).map((tx: any) => ({
        ...tx,
        date: new Date(tx.date)
      }));
      // Update current balance from the latest transaction
      if (this.transactions.length > 0) {
        this.currentBalance = this.transactions[0].balance;
      }
    } else {
      // Add some sample transactions
      this.transactions = [
        {
          id: 'TXN001',
          merchant: 'ATM Withdrawal',
          amount: 5000,
          type: 'Debit',
          balance: 45000,
          date: new Date(Date.now() - 86400000), // 1 day ago
          description: 'ATM cash withdrawal'
        },
        {
          id: 'TXN002',
          merchant: 'Salary Credit',
          amount: 75000,
          type: 'Credit',
          balance: 50000,
          date: new Date(Date.now() - 172800000), // 2 days ago
          description: 'Monthly salary credit'
        }
      ];
      this.saveTransactions();
    }
  }

  saveTransactions() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem(`user_transactions_${this.userAccountNumber}`, JSON.stringify(this.transactions));
  }

  checkForLoanApprovals() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Check if any loans were approved and create corresponding transactions
    const savedLoans = localStorage.getItem('user_loans');
    if (savedLoans) {
      const allLoans = JSON.parse(savedLoans);
      const userLoans = allLoans.filter((loan: any) => 
        loan.accountNumber === this.userAccountNumber && loan.status === 'Approved'
      );

      userLoans.forEach((loan: any) => {
        // Check if transaction already exists for this loan (improved detection)
        const existingTransaction = this.transactions.find(tx => 
          tx.description && (
            tx.description.includes(`Loan Approved: ${loan.loanAccountNumber}`) ||
            tx.description.includes(`Loan: ${loan.loanAccountNumber}`) ||
            tx.description.includes(loan.loanAccountNumber) ||
            tx.description.includes(`ID: ${loan.id}`)
          )
        );

        console.log(`Checking loan ${loan.loanAccountNumber} (ID: ${loan.id}):`, {
          existingTransaction: !!existingTransaction,
          loanAmount: loan.amount,
          currentTransactions: this.transactions.length
        });

        if (!existingTransaction) {
          // Create loan credit transaction
          const loanTransaction: TransactionRecord = {
            id: 'TXN' + Math.floor(Math.random() * 1000000),
            merchant: 'Loan Disbursement',
            amount: loan.amount,
            type: 'Loan Credit',
            balance: this.currentBalance + loan.amount,
            date: new Date(loan.approvalDate || new Date()),
            description: `Loan Approved: ${loan.loanAccountNumber} - ${loan.type} (ID: ${loan.id})`
          };

          this.transactions.unshift(loanTransaction);
          this.currentBalance = loanTransaction.balance;
        }
      });

      this.saveTransactions();
    }
  }

  cleanupDuplicateLoanTransactions() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    console.log('Cleaning up duplicate loan transactions...');
    
    // Get approved loans to identify which loan transactions should exist
    const savedLoans = localStorage.getItem('user_loans');
    if (!savedLoans) return;
    
    const allLoans = JSON.parse(savedLoans);
    const userApprovedLoans = allLoans.filter((loan: any) => 
      loan.accountNumber === this.userAccountNumber && loan.status === 'Approved'
    );
    
    // Create a map of loan account numbers that should have transactions
    const validLoanAccountNumbers = new Set(userApprovedLoans.map((loan: any) => loan.loanAccountNumber));
    
    // Find all loan credit transactions
    const loanCreditTransactions = this.transactions.filter(tx => tx.type === 'Loan Credit');
    
    // Group transactions by loan account number
    const transactionsByLoan = new Map();
    loanCreditTransactions.forEach((tx: TransactionRecord) => {
      const loanAccountNumber = this.extractLoanAccountNumber(tx.description || '');
      if (loanAccountNumber) {
        if (!transactionsByLoan.has(loanAccountNumber)) {
          transactionsByLoan.set(loanAccountNumber, []);
        }
        transactionsByLoan.get(loanAccountNumber).push(tx);
      }
    });
    
    // Remove duplicates - keep only the first transaction for each loan
    let removedCount = 0;
    transactionsByLoan.forEach((transactions, loanAccountNumber) => {
      if (transactions.length > 1) {
        // Sort by date (keep the oldest one)
        transactions.sort((a: TransactionRecord, b: TransactionRecord) => new Date(a.date).getTime() - new Date(b.date).getTime());
        
        // Remove all but the first one
        const toRemove = transactions.slice(1);
        toRemove.forEach((txToRemove: TransactionRecord) => {
          const index = this.transactions.findIndex(tx => tx.id === txToRemove.id);
          if (index !== -1) {
            this.transactions.splice(index, 1);
            removedCount++;
            console.log(`Removed duplicate loan transaction: ${txToRemove.description}`);
          }
        });
      }
    });
    
    if (removedCount > 0) {
      console.log(`Removed ${removedCount} duplicate loan transactions`);
      this.recalculateBalance();
      this.saveTransactions();
    }
  }
  
  extractLoanAccountNumber(description: string): string | null {
    // Extract loan account number from description
    const match = description.match(/Loan Approved: ([A-Z0-9]+)/);
    return match ? match[1] : null;
  }
  
  recalculateBalance() {
    // Start with initial balance
    let balance = 50000; // Initial balance
    
    // Sort transactions by date (oldest first)
    const sortedTransactions = [...this.transactions].sort((a: TransactionRecord, b: TransactionRecord) => 
      new Date(a.date).getTime() - new Date(b.date).getTime()
    );
    
    // Recalculate balance for each transaction
    sortedTransactions.forEach((tx: TransactionRecord) => {
      if (tx.type === 'Debit') {
        balance -= tx.amount;
      } else if (tx.type === 'Credit' || tx.type === 'Loan Credit') {
        balance += tx.amount;
      }
      tx.balance = balance;
    });
    
    // Update current balance
    if (this.transactions.length > 0) {
      this.currentBalance = this.transactions[0].balance;
    }
    
    console.log('Balance recalculated:', this.currentBalance);
  }

  addTransaction() {
    if (!this.merchant || this.amount <= 0) {
      alert('Please enter valid merchant and amount!');
      return;
    }

    if (this.type === 'Debit' && this.amount > this.currentBalance) {
      alert('Insufficient balance!');
      return;
    }

    const newBalance = this.type === 'Debit'
      ? this.currentBalance - this.amount
      : this.currentBalance + this.amount;

    const newTransaction: TransactionRecord = {
      id: 'TXN' + Math.floor(Math.random() * 1000000),
      merchant: this.merchant,
      amount: this.amount,
      type: this.type,
      balance: newBalance,
      date: new Date(),
      description: `${this.type} transaction`
    };

    this.transactions.unshift(newTransaction); // newest first
    this.currentBalance = newBalance;
    this.saveTransactions();

    this.merchant = '';
    this.amount = 0;
    this.type = 'Debit';
  }

  get filteredTransactions() {
    return this.transactions.filter(tx => {
      const txDate = new Date(tx.date);
      const from = this.fromDate ? new Date(this.fromDate) : null;
      const to = this.toDate ? new Date(this.toDate) : null;

      const matchMerchant = tx.merchant.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchFrom = from ? txDate >= from : true;
      const matchTo = to ? txDate <= to : true;

      return matchMerchant && matchFrom && matchTo;
    });
  }

  get miniStatement() {
    return this.transactions.slice(0, 5);
  }

  downloadStatement() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    let csv = 'Transaction ID,Merchant,Amount,Type,Balance,Date\n';
    this.filteredTransactions.forEach((tx: TransactionRecord) => {
      csv += `${tx.id},${tx.merchant},${tx.amount},${tx.type},${tx.balance},${tx.date.toLocaleString()}\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'statement.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
