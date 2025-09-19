import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loans-simple',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding: 20px;">
      <h1>Admin Loans - Simple Test</h1>
      <p>If you can see this, the component is working!</p>
      <button (click)="goBack()">← Back to Dashboard</button>
      
      <div style="margin-top: 20px;">
        <h3>Sample Loan Data:</h3>
        <div *ngFor="let loan of sampleLoans" style="border: 1px solid #ccc; padding: 10px; margin: 10px 0;">
          <strong>{{ loan.userName }}</strong> - {{ loan.type }}<br>
          Amount: ₹{{ loan.amount }}<br>
          Status: {{ loan.status }}
        </div>
      </div>
    </div>
  `
})
export class LoansSimple {
  constructor(private router: Router) {}

  sampleLoans = [
    { userName: 'John Doe', type: 'Personal Loan', amount: 250000, status: 'Pending' },
    { userName: 'Jane Smith', type: 'Home Loan', amount: 1500000, status: 'Pending' }
  ];

  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }
}
