import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
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
export class Userdashboard implements OnInit, OnDestroy {
  username: string = 'User'; // default
  selectedFeature: string | null = null;
  currentBalance: number = 0;
  userAccountNumber: string = '';
  loading: boolean = false;
  error: string = '';
  userProfile: any = null;
  
  // QR Code properties
  qrCodeImage: string = '';
  upiId: string = '';
  showQrCode: boolean = false;
  showQrScanner: boolean = false;
  qrAmount: number = 0;
  isGeneratingQr: boolean = false;
  html5QrCode: any = null;
  isScanning: boolean = false;

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
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserProfile();
    }
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
      'loan': 'Loans',
      'cheque': 'Cheque Management',
      'goldloan': 'Gold Loan'
    };
    return titles[this.selectedFeature || ''] || '';
  }

  getFeatureDescription(): string {
    const descriptions: { [key: string]: string } = {
      'transferfunds': 'Send money to other accounts securely and quickly.',
      'card': 'Manage your debit and credit cards, view statements, and set limits.',
      'transaction': 'View your complete transaction history and download statements.',
      'kycupdate': 'Update your personal information and verify your identity.',
      'loan': 'Apply for personal loans, check eligibility, and manage existing loans.',
      'cheque': 'Create, view, download, and cancel cheque leaves for your account.',
      'goldloan': 'Apply for gold loan against your gold. Get 75% of gold value as loan.'
    };
    return descriptions[this.selectedFeature || ''] || '';
  }

  getFeatureIcon(): string {
    const icons: { [key: string]: string } = {
      'transferfunds': '💸',
      'card': '💳',
      'transaction': '🔄',
      'kycupdate': '📑',
      'loan': '💰',
      'cheque': '📝',
      'goldloan': '🥇'
    };
    return icons[this.selectedFeature || ''] || '';
  }

  // Refresh user data (useful for real-time updates)
  refreshUserData() {
    console.log('Refreshing user data...');
    this.loadUserProfile();
  }

  // QR Code Methods
  generateQrCode(amount?: number) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    if (!this.userAccountNumber) {
      console.error('User account number is missing');
      this.alertService.transferError('Account number not found');
      return;
    }

    console.log('Generating QR code for account:', this.userAccountNumber, 'Amount:', amount);
    this.isGeneratingQr = true;
    
    // URL encode account number to handle special characters
    const encodedAccountNumber = encodeURIComponent(this.userAccountNumber);
    const url = amount && amount > 0 
      ? `${environment.apiUrl}/qrcode/upi/receive/${encodedAccountNumber}/${amount}`
      : `${environment.apiUrl}/qrcode/upi/receive/${encodedAccountNumber}`;

    console.log('Requesting QR code from URL:', url);

    this.http.get(url).subscribe({
      next: (response: any) => {
        if (response.qrCode) {
          this.qrCodeImage = response.qrCode;
          this.upiId = response.upiId;
          this.qrAmount = response.amount || 0;
          this.showQrCode = true;
          this.isGeneratingQr = false;
        } else {
          console.error('QR code not in response:', response);
          const errorMsg = response.error || 'Failed to generate QR code';
          this.alertService.transferError(errorMsg);
          this.isGeneratingQr = false;
        }
      },
      error: (err: any) => {
        console.error('Error generating QR code:', err);
        const errorMsg = err.error?.error || err.message || 'Failed to generate QR code. Please check your account number.';
        this.alertService.transferError(errorMsg);
        this.isGeneratingQr = false;
      }
    });
  }

  closeQrCode() {
    this.showQrCode = false;
    this.qrCodeImage = '';
    this.qrAmount = 0;
  }

  startQrScanner() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.showQrScanner = true;
    this.isScanning = true;
    
    // Initialize scanner after view update
    setTimeout(async () => {
      try {
        // Dynamic import to avoid SSR issues
        const { Html5Qrcode } = await import('html5-qrcode');
        const scannerId = 'qr-reader';
        this.html5QrCode = new Html5Qrcode(scannerId);
        
        await this.html5QrCode.start(
          { facingMode: 'environment' },
          {
            fps: 10,
            qrbox: { width: 250, height: 250 }
          },
          (decodedText: string) => {
            this.onQrCodeScanned(decodedText);
          },
          (errorMessage: string) => {
            // Ignore scanning errors
          }
        );
      } catch (err: any) {
        console.error('Error starting QR scanner:', err);
        this.alertService.transferError('Failed to start camera. Please check permissions.');
        this.stopQrScanner();
      }
    }, 100);
  }

  stopQrScanner() {
    if (this.html5QrCode) {
      this.html5QrCode.stop().then(() => {
        this.html5QrCode?.clear();
        this.html5QrCode = null;
      }).catch((err: any) => {
        console.error('Error stopping QR scanner:', err);
      });
    }
    this.showQrScanner = false;
    this.isScanning = false;
  }

  onQrCodeScanned(decodedText: string) {
    console.log('QR Code scanned:', decodedText);
    
    // Check if it's a UPI payment QR code
    if (decodedText.startsWith('upi://pay')) {
      try {
        // Parse UPI URL - handle both URL and string parsing
        let upiId: string | null = null;
        let name: string | null = null;
        let amount: string | null = null;
        
        try {
          // Try using URL constructor (works for full URLs)
          const url = new URL(decodedText);
          upiId = url.searchParams.get('pa');
          name = url.searchParams.get('pn');
          amount = url.searchParams.get('am');
        } catch (e) {
          // Fallback: manual parsing for upi:// protocol
          const params = decodedText.split('?')[1];
          if (params) {
            const paramPairs = params.split('&');
            paramPairs.forEach(pair => {
              const [key, value] = pair.split('=');
              if (key === 'pa') upiId = decodeURIComponent(value);
              if (key === 'pn') name = decodeURIComponent(value);
              if (key === 'am') amount = value;
            });
          }
        }
        
        // Stop scanner
        this.stopQrScanner();
        
        // Navigate to transfer funds with pre-filled data
        this.router.navigate(['/website/transferfunds'], {
          state: {
            scannedUpiId: upiId,
            scannedName: name,
            scannedAmount: amount ? parseFloat(amount) : null
          }
        });
      } catch (error) {
        console.error('Error parsing QR code:', error);
        this.alertService.transferError('Error parsing QR code. Please try again.');
        this.stopQrScanner();
      }
    } else {
      this.alertService.transferError('Invalid QR code. Please scan a UPI payment QR code.');
    }
  }

  ngOnDestroy() {
    this.stopQrScanner();
  }
}
