import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';
// import { UserService } from '../../service/user';
// import { AccountService } from '../../service/account';
// import { TransactionService } from '../../service/transaction';

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
  currentBalance: number = 0;
  userAccountNumber: string = '';
  loading: boolean = false;
  error: string = '';
  userProfile: any = null;

  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
    // private userService: UserService,
    // private accountService: AccountService,
    // private transactionService: TransactionService
  ) {
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state && nav.extras.state['username']) {
      this.username = nav.extras.state['username'];
    }
  }

  ngOnInit() {
    this.loadUserProfile();
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    this.error = '';
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.username = user.name;
      this.userAccountNumber = user.accountNumber;
      
      // Load user profile and balance from MySQL database
      this.loadUserDataFromMySQL(user.id);
    } else {
      // Try to get user from localStorage as fallback
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        const profile = JSON.parse(savedProfile);
        this.username = profile.name || 'User';
        this.userAccountNumber = profile.accountNumber || 'ACC001';
        this.currentBalance = 0;
        this.loading = false;
        console.log('Using fallback profile from localStorage');
      } else {
        // No session found - use default values to avoid errors
        this.username = 'User';
        this.userAccountNumber = 'ACC001';
        this.currentBalance = 0;
        this.loading = false;
        console.log('Using default values - no session found');
      }
    }
  }

  loadUserDataFromMySQL(userId: string) {
    // Load user data from MySQL database
    this.http.get(`${environment.apiUrl}/users/${userId}`).subscribe({
      next: (userData: any) => {
        console.log('User data loaded from MySQL:', userData);
        
        this.userProfile = userData;
        this.username = userData.account?.name || userData.username || 'User';
        this.userAccountNumber = userData.accountNumber || 'ACC001';
        
        // Load current balance from MySQL
        this.loadCurrentBalanceFromMySQL();
      },
      error: (err: any) => {
        console.error('Error loading user data from MySQL:', err);
        // Use default values instead of showing error
        this.username = 'User';
        this.userAccountNumber = 'ACC001';
        this.currentBalance = 0;
        this.loading = false;
      }
    });
  }

  loadCurrentBalanceFromMySQL() {
    if (!this.userAccountNumber) {
      this.loading = false;
      return;
    }
    
    // Load current balance from MySQL database
    this.http.get(`${environment.apiUrl}/accounts/balance/${this.userAccountNumber}`).subscribe({
      next: (balanceData: any) => {
        console.log('Balance loaded from MySQL:', balanceData);
        this.currentBalance = balanceData.balance || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading balance from MySQL:', err);
        // Fallback to user profile balance
        this.currentBalance = this.userProfile?.account?.balance || 0;
        this.loading = false;
      }
    });
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
    this.alertService.logoutSuccess();
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

  // Refresh user data (useful for real-time updates)
  refreshUserData() {
    console.log('Refreshing user data...');
    this.loadUserProfile();
  }
}
