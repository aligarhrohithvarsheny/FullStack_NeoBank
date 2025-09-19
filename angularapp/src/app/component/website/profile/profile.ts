import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';

interface UserProfile {
  name: string;
  email: string;
  accountNumber: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  accountType: string;
  joinDate: string;
  pan?: string;
  aadhar?: string;
  occupation?: string;
  income?: number;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css']
})
export class Profile implements OnInit {
  userProfile: UserProfile = {
    name: 'John Doe',
    email: 'john.doe@example.com',
    accountNumber: 'ACC001',
    phoneNumber: '+91 98765 43210',
    address: '123 Main Street, City, State 12345',
    dateOfBirth: '1990-01-15',
    accountType: 'Savings Account',
    joinDate: '2023-01-15'
  };

  currentBalance: number = 50000;

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    this.loadUserProfile();
    this.loadCurrentBalance();
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load user profile from localStorage
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      this.userProfile = { ...this.userProfile, ...JSON.parse(savedProfile) };
    } else {
      // Save default profile
      this.saveUserProfile();
    }
  }

  saveUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('user_profile', JSON.stringify(this.userProfile));
  }

  loadCurrentBalance() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load current balance from transactions
    const userTransactions = localStorage.getItem(`user_transactions_${this.userProfile.accountNumber}`);
    if (userTransactions) {
      const transactions = JSON.parse(userTransactions);
      if (transactions.length > 0) {
        this.currentBalance = transactions[0].balance;
      }
    }
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  editProfile() {
    // For now, just show an alert. In a real app, this would open an edit form
    alert('Profile editing feature will be available soon!');
  }
}
