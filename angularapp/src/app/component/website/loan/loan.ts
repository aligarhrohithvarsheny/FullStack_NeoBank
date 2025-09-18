import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface LoanRequest {
  id: number;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: 'Waiting for Sanction' | 'Approved' | 'Rejected';
}

@Component({
  selector: 'app-loan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan.html',
  styleUrls: ['./loan.css']
})
export class Loan {
  constructor(private router: Router) {}

  loans: LoanRequest[] = [];

  loanForm = {
    type: '',
    amount: 0,
    tenure: 0,
    interestRate: 0
  };

  submitLoan() {
    const newLoan: LoanRequest = {
      id: this.loans.length + 1,
      type: this.loanForm.type,
      amount: this.loanForm.amount,
      tenure: this.loanForm.tenure,
      interestRate: this.loanForm.interestRate,
      status: 'Waiting for Sanction'
    };

    this.loans.push(newLoan);

    // Reset form
    this.loanForm = { type: '', amount: 0, tenure: 0, interestRate: 0 };
  }

  goBack() {
    this.router.navigate(['website/userdashboard']); // ✅ change if your route is different
  }
  
}
