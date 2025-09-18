import { CurrencyPipe } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router'; // Import Router

interface LoanRequest {
  id: number;
  userName: string;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: 'Pending' | 'Approved' | 'Rejected';
}

@Component({
  selector: 'app-loans',
  templateUrl: './loans.html',
  styleUrls: ['./loans.css'],
  imports: [CurrencyPipe]

})
export class Loans {
  constructor(private router: Router) {} // Inject Router

  loanRequests: LoanRequest[] = [
    { id: 1, userName: 'John Doe', type: 'Personal Loan', amount: 250000, tenure: 24, interestRate: 10.5, status: 'Pending' },
    { id: 2, userName: 'Jane Smith', type: 'Home Loan', amount: 1500000, tenure: 120, interestRate: 8.2, status: 'Pending' }
  ];

  // Navigate back to admin dashboard
  goBack() {
    this.router.navigate(['/admin/dashboard']); // Replace with your actual dashboard route
  }

  // Approve loan
  approve(loan: LoanRequest) {
    loan.status = 'Approved';
    alert(`Loan for ${loan.userName} approved.`);
  }

  // Reject loan
  reject(loan: LoanRequest) {
    loan.status = 'Rejected';
    alert(`Loan for ${loan.userName} rejected.`);
  }
}
