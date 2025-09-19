import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface LoanRequest {
  id: number;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: 'Pending' | 'Approved' | 'Rejected';
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
  loanAccountNumber: string;
  applicationDate: string;
  approvalDate?: string;
}

@Component({
  selector: 'app-loan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan.html',
  styleUrls: ['./loan.css']
})
export class Loan implements OnInit {
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}

  loans: LoanRequest[] = [];
  userAccountNumber: string = 'ACC001';
  userName: string = 'John Doe';
  userEmail: string = 'john.doe@example.com';
  currentBalance: number = 50000;

  loanForm = {
    type: '',
    amount: 0,
    tenure: 0,
    interestRate: 0
  };

  ngOnInit() {
    this.loadUserLoans();
  }

  loadUserLoans() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load loans from localStorage
    const savedLoans = localStorage.getItem('user_loans');
    if (savedLoans) {
      this.loans = JSON.parse(savedLoans).filter((loan: LoanRequest) => 
        loan.accountNumber === this.userAccountNumber
      );
    }
  }

  submitLoan() {
    // Validate form
    if (!this.loanForm.type || this.loanForm.amount <= 0 || this.loanForm.tenure <= 0 || this.loanForm.interestRate <= 0) {
      alert('Please fill all fields correctly!');
      return;
    }

    // Generate loan account number with better uniqueness
    const loanAccountNumber = 'LOAN' + Date.now() + Math.floor(Math.random() * 1000);

    const newLoan: LoanRequest = {
      id: Date.now(),
      type: this.loanForm.type,
      amount: this.loanForm.amount,
      tenure: this.loanForm.tenure,
      interestRate: this.loanForm.interestRate,
      status: 'Pending',
      userName: this.userName,
      userEmail: this.userEmail,
      accountNumber: this.userAccountNumber,
      currentBalance: this.currentBalance,
      loanAccountNumber: loanAccountNumber,
      applicationDate: new Date().toISOString()
    };

    // Add to local loans array
    this.loans.unshift(newLoan);

    // Save to localStorage
    this.saveLoansToStorage();

    // Show success message
    alert('Loan application submitted successfully!\nLoan Account: ' + loanAccountNumber);

    // Reset form
    this.loanForm = { type: '', amount: 0, tenure: 0, interestRate: 0 };
  }

  saveLoansToStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get all loans from localStorage
    const allLoans = JSON.parse(localStorage.getItem('user_loans') || '[]');
    
    // Update or add the current user's loans
    const otherLoans = allLoans.filter((loan: LoanRequest) => 
      loan.accountNumber !== this.userAccountNumber
    );
    
    // Combine other users' loans with current user's loans
    const updatedLoans = [...otherLoans, ...this.loans];
    
    // Save back to localStorage
    localStorage.setItem('user_loans', JSON.stringify(updatedLoans));
  }

  goBack() {
    this.router.navigate(['website/userdashboard']);
  }
}
