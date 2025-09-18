import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface Transaction {
  id: number;
  user: string;
  type: 'Deposit' | 'Withdraw' | 'Transfer';
  amount: number;
  date: string;
  status: string;
}

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transactions.html',
  styleUrls: ['./transactions.css']
})
export class Transactions {

  constructor(private router: Router) {}

  transactions: Transaction[] = [
    { id: 1, user: 'John Doe', type: 'Deposit', amount: 5000, date: '2025-09-16', status: 'Completed' },
    { id: 2, user: 'Jane Smith', type: 'Withdraw', amount: 2000, date: '2025-09-15', status: 'Completed' },
    { id: 3, user: 'John Doe', type: 'Transfer', amount: 1500, date: '2025-09-14', status: 'Pending' },
  ];

  backToDashboard() {
    this.router.navigate(['admin/dashboard']); // Adjust route to your dashboard path
  }
}
