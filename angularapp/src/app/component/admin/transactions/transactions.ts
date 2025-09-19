import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface Transaction {
  id: string;
  user: string;
  type: 'Deposit' | 'Withdraw' | 'Transfer' | 'Loan Credit';
  amount: number;
  date: string;
  status: string;
  description?: string;
  accountNumber?: string;
}

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transactions.html',
  styleUrls: ['./transactions.css']
})
export class Transactions implements OnInit {

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}

  transactions: Transaction[] = [];

  ngOnInit() {
    this.loadAllTransactions();
  }

  loadAllTransactions() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load all user transactions from localStorage
    this.transactions = [];
    
    // Get all user account numbers from loans
    const savedLoans = localStorage.getItem('user_loans');
    if (savedLoans) {
      const allLoans: any[] = JSON.parse(savedLoans);
      const accountNumbers: string[] = [];
      
      // Extract unique account numbers
      allLoans.forEach((loan: any) => {
        if (loan.accountNumber && !accountNumbers.includes(loan.accountNumber)) {
          accountNumbers.push(loan.accountNumber);
        }
      });
      
      accountNumbers.forEach((accountNumber: string) => {
        const userTransactions = localStorage.getItem(`user_transactions_${accountNumber}`);
        if (userTransactions) {
          const transactions = JSON.parse(userTransactions);
          transactions.forEach((tx: any) => {
            this.transactions.push({
              id: tx.id,
              user: this.getUserNameByAccount(accountNumber),
              type: tx.type,
              amount: tx.amount,
              date: tx.date,
              status: 'Completed',
              description: tx.description,
              accountNumber: accountNumber
            });
          });
        }
      });
    }

    // Add some sample transactions if none exist
    if (this.transactions.length === 0) {
      this.transactions = [
        { id: 'TXN001', user: 'John Doe', type: 'Deposit', amount: 5000, date: new Date().toISOString(), status: 'Completed', accountNumber: 'ACC001' },
        { id: 'TXN002', user: 'Jane Smith', type: 'Withdraw', amount: 2000, date: new Date().toISOString(), status: 'Completed', accountNumber: 'ACC002' },
        { id: 'TXN003', user: 'John Doe', type: 'Transfer', amount: 1500, date: new Date().toISOString(), status: 'Pending', accountNumber: 'ACC001' },
      ];
    }

    // Sort by date (newest first)
    this.transactions.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }

  getUserNameByAccount(accountNumber: string): string {
    if (!isPlatformBrowser(this.platformId)) return 'Unknown User';
    
    const savedLoans = localStorage.getItem('user_loans');
    if (savedLoans) {
      const allLoans = JSON.parse(savedLoans);
      const loan = allLoans.find((l: any) => l.accountNumber === accountNumber);
      return loan ? loan.userName : 'Unknown User';
    }
    return 'Unknown User';
  }

  backToDashboard() {
    this.router.navigate(['admin/dashboard']);
  }
}
