import { CurrencyPipe, CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';

interface LoanRequest {
  id: number;
  userName: string;
  userEmail: string;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: 'Pending' | 'Approved' | 'Rejected';
  accountNumber: string;
  currentBalance: number;
  loanAccountNumber: string;
  applicationDate: string;
  approvalDate?: string;
}

@Component({
  selector: 'app-loans',
  standalone: true,
  templateUrl: './loans.html',
  styleUrls: ['./loans.css'],
  imports: [CurrencyPipe, CommonModule]
})
export class Loans implements OnInit {
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}

  loanRequests: LoanRequest[] = [];
  loading = false;

  ngOnInit() {
    console.log('Loans component initialized!');
    this.loadAllLoans();
  }

  loadAllLoans() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    console.log('Loading loan applications...');
    
    // Load loans from localStorage
    const savedLoans = localStorage.getItem('user_loans');
    console.log('Saved loans from localStorage:', savedLoans);
    
    if (savedLoans) {
      try {
        this.loanRequests = JSON.parse(savedLoans);
        console.log('Parsed loan requests:', this.loanRequests);
      } catch (error) {
        console.error('Error parsing saved loans:', error);
        this.loanRequests = [];
      }
    } else {
      console.log('No saved loans found, creating sample data...');
      // Add some sample data if no loans exist
      this.loanRequests = [
        {
          id: 1,
          userName: 'John Doe',
          userEmail: 'john.doe@example.com',
          type: 'Personal Loan',
          amount: 250000,
          tenure: 24,
          interestRate: 10.5,
          status: 'Pending',
          accountNumber: 'ACC001',
          currentBalance: 50000,
          loanAccountNumber: 'LOAN001',
          applicationDate: new Date().toISOString()
        },
        {
          id: 2,
          userName: 'Jane Smith',
          userEmail: 'jane.smith@example.com',
          type: 'Home Loan',
          amount: 1500000,
          tenure: 120,
          interestRate: 8.2,
          status: 'Pending',
          accountNumber: 'ACC002',
          currentBalance: 75000,
          loanAccountNumber: 'LOAN002',
          applicationDate: new Date().toISOString()
        },
        {
          id: 3,
          userName: 'Mike Johnson',
          userEmail: 'mike.johnson@example.com',
          type: 'Car Loan',
          amount: 800000,
          tenure: 60,
          interestRate: 9.5,
          status: 'Approved',
          accountNumber: 'ACC003',
          currentBalance: 100000,
          loanAccountNumber: 'LOAN003',
          applicationDate: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
          approvalDate: new Date().toISOString()
        }
      ];
      this.saveLoansToStorage();
      console.log('Sample data created:', this.loanRequests);
    }
    
    this.loading = false;
    console.log('Final loan requests count:', this.loanRequests.length);
  }

  saveLoansToStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('user_loans', JSON.stringify(this.loanRequests));
  }

  // Navigate back to admin dashboard
  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }

  // Approve loan
  approve(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    loan.status = 'Approved';
    loan.approvalDate = new Date().toISOString();
    
    // Update in localStorage
    this.saveLoansToStorage();
    
    // Create loan credit transaction for the user
    this.createLoanCreditTransaction(loan);
    
    alert(`Loan for ${loan.userName} approved. Amount ₹${loan.amount} has been credited to their account.`);
  }

  createLoanCreditTransaction(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get user's existing transactions
    const userTransactions = localStorage.getItem(`user_transactions_${loan.accountNumber}`);
    let transactions = userTransactions ? JSON.parse(userTransactions) : [];
    
    // Check if transaction already exists for this loan
    const existingTransaction = transactions.find((tx: any) => 
      tx.description && tx.description.includes(`Loan: ${loan.loanAccountNumber}`)
    );

    if (!existingTransaction) {
      // Calculate new balance
      let currentBalance = 50000; // Default balance
      if (transactions.length > 0) {
        currentBalance = transactions[0].balance;
      }

      // Create loan credit transaction
      const loanTransaction = {
        id: 'TXN' + Math.floor(Math.random() * 1000000),
        merchant: 'Loan Disbursement',
        amount: loan.amount,
        type: 'Loan Credit',
        balance: currentBalance + loan.amount,
        date: new Date().toISOString(),
        description: `Loan Approved: ${loan.loanAccountNumber} - ${loan.type}`
      };

      // Add to beginning of transactions array (newest first)
      transactions.unshift(loanTransaction);
      
      // Save updated transactions
      localStorage.setItem(`user_transactions_${loan.accountNumber}`, JSON.stringify(transactions));
    }
  }

  // Reject loan
  reject(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    loan.status = 'Rejected';
    loan.approvalDate = new Date().toISOString();
    
    // Update in localStorage
    this.saveLoansToStorage();
    
    alert(`Loan for ${loan.userName} rejected.`);
  }

  // TrackBy function for ngFor performance
  trackByLoanId(index: number, loan: LoanRequest): number {
    return loan.id;
  }
}
