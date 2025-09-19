import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-userdashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './userdashboard.html',
  styleUrls: ['./userdashboard.css']
})
export class Userdashboard implements OnInit {
  username: string = 'User'; // default
  selectedFeature: string | null = null;
  currentBalance: number = 50000;
  userAccountNumber: string = 'ACC001';

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state && nav.extras.state['username']) {
      this.username = nav.extras.state['username'];
    }
  }

  ngOnInit() {
    this.loadUserProfile();
    this.loadCurrentBalance();
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load user profile from localStorage
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      const profile = JSON.parse(savedProfile);
      this.username = profile.name || 'User';
      this.userAccountNumber = profile.accountNumber || 'ACC001';
    }
  }

  loadCurrentBalance() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load current balance from transactions
    const userTransactions = localStorage.getItem(`user_transactions_${this.userAccountNumber}`);
    if (userTransactions) {
      const transactions = JSON.parse(userTransactions);
      if (transactions.length > 0) {
        this.currentBalance = transactions[0].balance;
      }
    }
  }

  selectFeature(feature: string) {
    this.selectedFeature = feature;
  }

  goTo(page: string) {
    // ✅ use the `page` parameter, not Node path
    this.router.navigate([`/website/${page}`]);
  }

  goToProfile() {
    this.router.navigate(['/website/profile']);
  }

  logout() {
    alert('Logged out successfully 🚪');
    this.router.navigate(['/website/user']); // back to login
  }

  getFeatureTitle(): string {
    const titles: { [key: string]: string } = {
      'transferfunds': 'Transfer Funds',
      'card': 'Card Management',
      'transaction': 'Transactions',
      'kycupdate': 'KYC Update',
      'loan': 'Loans'
    };
    return titles[this.selectedFeature || ''] || '';
  }

  getFeatureDescription(): string {
    const descriptions: { [key: string]: string } = {
      'transferfunds': 'Send money to other accounts securely and quickly.',
      'card': 'Manage your debit and credit cards, view statements, and set limits.',
      'transaction': 'View your complete transaction history and download statements.',
      'kycupdate': 'Update your personal information and verify your identity.',
      'loan': 'Apply for personal loans, check eligibility, and manage existing loans.'
    };
    return descriptions[this.selectedFeature || ''] || '';
  }

  getFeatureIcon(): string {
    const icons: { [key: string]: string } = {
      'transferfunds': '💸',
      'card': '💳',
      'transaction': '🔄',
      'kycupdate': '📑',
      'loan': '💰'
    };
    return icons[this.selectedFeature || ''] || '';
  }
}
