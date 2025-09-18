import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface PendingUser {
  id: string;
  name: string;
  dob: string;
  occupation: string;
  income: number;
  pan: string;
  aadhar: string;
  mobile: string;
  email: string;
  balance: number;
  status: 'PENDING' | 'APPROVED' | 'CLOSED';
  assignedAccountNumber?: string;
  createdAt: string;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrls: ['./users.css']
})
export class Users {
  constructor(private router: Router) {}

  pendingUsers: PendingUser[] = [];
  nextAccountNumber = 100001;

  ngOnInit() {
    // Load from localStorage (mock backend)
    const raw = localStorage.getItem('admin_users');
    this.pendingUsers = raw ? JSON.parse(raw) : [];
    this.calculateNextAccountNumber();
  }

  calculateNextAccountNumber() {
    // Find the highest account number from existing users
    let maxAccountNumber = 100000;
    this.pendingUsers.forEach(user => {
      if (user.assignedAccountNumber) {
        const accountNum = parseInt(user.assignedAccountNumber);
        if (accountNum > maxAccountNumber) {
          maxAccountNumber = accountNum;
        }
      }
    });
    this.nextAccountNumber = maxAccountNumber + 1;
  }

  // Assign account number
  approveUser(user: PendingUser) {
    if (!user.assignedAccountNumber) {
      user.assignedAccountNumber = this.nextAccountNumber.toString();
      user.status = 'APPROVED';
      this.nextAccountNumber++; // Increment for next user
      this.saveUsers();
    }
  }

  // Close account functionality
  closeAccount(user: PendingUser) {
    if (confirm(`Are you sure you want to close account ${user.assignedAccountNumber} for ${user.name}?`)) {
      user.status = 'CLOSED';
      this.saveUsers();
    }
  }

  saveUsers() {
    localStorage.setItem('admin_users', JSON.stringify(this.pendingUsers));
  }

  goBack() {
    this.router.navigate(['admin/dashboard']);
  }
}
