import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}

  transactions: Transaction[] = [];

  ngOnInit() {
    this.loadAllTransactions();
  }

  loadAllTransactions() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load all transactions from MySQL database
    this.http.get('http://localhost:8080/api/transactions?page=0&size=100').subscribe({
      next: (response: any) => {
        console.log('Transactions loaded from MySQL:', response);
        if (response.content) {
          this.transactions = response.content.map((transaction: any) => ({
            id: transaction.id,
            accountNumber: transaction.accountNumber,
            type: transaction.type,
            amount: transaction.amount,
            balance: transaction.balance,
            merchant: transaction.merchant,
            status: transaction.status,
            date: transaction.date,
            description: transaction.description
          }));
        } else {
          this.transactions = response.map((transaction: any) => ({
            id: transaction.id,
            accountNumber: transaction.accountNumber,
            type: transaction.type,
            amount: transaction.amount,
            balance: transaction.balance,
            merchant: transaction.merchant,
            status: transaction.status,
            date: transaction.date,
            description: transaction.description
          }));
        }
        
        // Also save to localStorage as backup
        this.saveTransactionsToStorage();
        this.applyFilters();
      },
      error: (err: any) => {
        console.error('Error loading transactions from database:', err);
        // Fallback to localStorage
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

    // No sample data - only show real transactions from database
    console.log('Transactions loaded from database:', this.transactions.length);

    // Sort by date (newest first)
    this.transactions.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
      }
    });
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

  saveTransactionsToStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('admin_transactions', JSON.stringify(this.transactions));
  }

  applyFilters() {
    // For now, just sort by date (newest first)
    this.transactions.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }
}
