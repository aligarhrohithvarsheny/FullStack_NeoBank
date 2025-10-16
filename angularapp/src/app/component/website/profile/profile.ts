import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
// import { UserService } from '../../service/user';
// import { AccountService } from '../../service/account';
// import { TransactionService } from '../../service/transaction';

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
  ifscCode?: string;
  account?: {
    balance?: number;
    [key: string]: any;
  };
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
    name: '',
    email: '',
    accountNumber: '',
    phoneNumber: '',
    address: '',
    dateOfBirth: '',
    accountType: '',
    joinDate: '',
    ifscCode: 'NEO0008648'
  };

  currentBalance: number = 0;
  loading: boolean = false;
  error: string = '';

  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
    // private userService: UserService,
    // private accountService: AccountService,
    // private transactionService: TransactionService
  ) {}

  ngOnInit() {
    this.loadUserProfile();
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    this.error = '';
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    console.log('Profile component - currentUser from sessionStorage:', currentUser);
    
    if (currentUser) {
      const user = JSON.parse(currentUser);
      console.log('Profile component - parsed user data:', user);
      
      // Load full user profile from MySQL database
      this.http.get(`http://localhost:8080/api/users/${user.id}`).subscribe({
        next: (userData: any) => {
          console.log('User profile loaded from MySQL:', userData);
          this.userProfile = {
            name: userData.account?.name || userData.username || 'User',
            email: userData.email || 'user@example.com',
            accountNumber: userData.accountNumber || 'ACC001',
            phoneNumber: userData.account?.phone || userData.phoneNumber || '',
            address: userData.account?.address || userData.address || '',
            dateOfBirth: userData.account?.dob || userData.dateOfBirth || '',
            accountType: userData.account?.accountType || 'Savings Account',
            joinDate: userData.createdAt ? new Date(userData.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
            pan: userData.account?.pan || userData.pan || '',
            aadhar: userData.account?.aadharNumber || userData.aadhar || '',
            occupation: userData.account?.occupation || userData.occupation || '',
            income: userData.account?.income || userData.income || 0,
            ifscCode: 'NEO0008648'
          };
          
          // Load current balance from MySQL
          this.loadCurrentBalanceFromMySQL();
        },
        error: (err: any) => {
          console.error('Error loading user profile from MySQL:', err);
          // Use fallback profile instead of showing error
          this.userProfile = {
            name: 'User',
            email: 'user@example.com',
            accountNumber: 'ACC001',
            phoneNumber: '',
            address: '',
            dateOfBirth: '',
            accountType: 'Savings Account',
            joinDate: new Date().toISOString().split('T')[0],
            pan: '',
            aadhar: '',
            occupation: '',
            income: 0,
            ifscCode: 'NEO0008648'
          };
          this.currentBalance = 0;
          this.loading = false;
        }
      });
    } else {
      // Try to get user from localStorage as fallback (for existing users)
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        const profile = JSON.parse(savedProfile);
        this.userProfile = profile;
        this.currentBalance = 0; // Will be loaded separately
        this.loading = false;
        console.log('Using fallback profile from localStorage');
      } else {
        // Try to get user from sessionStorage as last resort
        const currentUser = sessionStorage.getItem('currentUser');
        if (currentUser) {
          const user = JSON.parse(currentUser);
          this.userProfile = {
            name: user.name || 'User',
            email: user.email || 'user@example.com',
            accountNumber: user.accountNumber || 'ACC001',
            phoneNumber: '',
            address: '',
            dateOfBirth: '',
            accountType: 'Savings Account',
            joinDate: new Date().toISOString().split('T')[0],
            pan: '',
            aadhar: '',
            occupation: '',
            income: 0,
            ifscCode: 'NEO0008648'
          };
          this.currentBalance = 0;
          this.loading = false;
          console.log('Using session profile - no localStorage found');
        } else {
          // No session found - create default profile to avoid errors
          this.userProfile = {
            name: 'User',
            email: 'user@example.com',
            accountNumber: 'ACC001',
            phoneNumber: '',
            address: '',
            dateOfBirth: '',
            accountType: 'Savings Account',
            joinDate: new Date().toISOString().split('T')[0],
            pan: '',
            aadhar: '',
            occupation: '',
            income: 0,
            ifscCode: 'NEO0008648'
          };
          this.currentBalance = 0;
          this.loading = false;
          console.log('Using default profile - no session found');
        }
      }
    }
  }

  loadCurrentBalanceFromMySQL() {
    if (!this.userProfile.accountNumber) {
      this.loading = false;
      return;
    }
    
    // Load current balance from MySQL database
    this.http.get(`http://localhost:8080/api/accounts/balance/${this.userProfile.accountNumber}`).subscribe({
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

  loadCurrentBalance() {
    // This method is now replaced by loadCurrentBalanceFromMySQL()
    this.loadCurrentBalanceFromMySQL();
  }

  loadFromLocalStorage() {
    // Fallback to localStorage if backend fails
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      this.userProfile = { ...this.userProfile, ...JSON.parse(savedProfile) };
    }
    
    const userTransactions = localStorage.getItem(`user_transactions_${this.userProfile.accountNumber}`);
    if (userTransactions) {
      const transactions = JSON.parse(userTransactions);
      if (transactions.length > 0) {
        this.currentBalance = transactions[0].balance;
      }
    }
    this.loading = false;
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  editProfile() {
    // For now, just show an alert. In a real app, this would open an edit form
    this.alertService.info('Profile Editing', 'Profile editing feature will be available soon!');
  }
}
