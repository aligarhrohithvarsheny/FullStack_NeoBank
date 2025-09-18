import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-userdashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './userdashboard.html',
  styleUrls: ['./userdashboard.css']
})
export class Userdashboard {
  username: string = 'User'; // default
  selectedFeature: string | null = null;

  constructor(private router: Router) {
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state && nav.extras.state['username']) {
      this.username = nav.extras.state['username'];
    }
  }

  selectFeature(feature: string) {
    this.selectedFeature = feature;
  }

  goTo(page: string) {
    // ✅ use the `page` parameter, not Node path
    this.router.navigate([`/website/${page}`]);
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
