import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';
import { timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

interface GoldLoan {
  id?: number;
  goldGrams: number;
  goldRatePerGram: number;
  goldValue: number;
  loanAmount: number;
  tenure: number;
  interestRate: number;
  status: string;
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
  loanAccountNumber?: string;
  applicationDate?: string;
  approvalDate?: string;
  goldItems?: string;
  goldDescription?: string;
  goldPurity?: string;
  verifiedGoldGrams?: number;
  verifiedGoldValue?: number;
  verificationNotes?: string;
  storageLocation?: string;
  termsAccepted?: boolean;
  termsAcceptedDate?: string;
  termsAcceptedBy?: string;
  otpVerified?: boolean;
}

interface GoldRate {
  id?: number;
  date: string;
  ratePerGram: number;
  lastUpdated?: string;
}

const OTP_VALID_SECONDS = 120;

@Component({
  selector: 'app-goldloan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './goldloan.html',
  styleUrls: ['./goldloan.css']
})

export class Goldloan implements OnInit, OnDestroy {
  goldGrams: number = 0;
  currentGoldRate: GoldRate | null = null;
  calculatedLoan: any = null;
  loading: boolean = false;
  calculating: boolean = false;
  userAccountNumber: string = '';
  userName: string = '';
  userEmail: string = '';
  currentBalance: number = 0;
  
  // Form fields
  tenure: number = 12; // Default 12 months
  interestRate: number = 12; // Default 12% per annum
  acceptTerms: boolean = false; // Terms acceptance
  
  // Gold loans list
  goldLoans: GoldLoan[] = [];
  loadingLoans: boolean = false;
  
  // Terms acceptance for approved loans
  selectedLoanForTerms: GoldLoan | null = null;
  showTermsModal: boolean = false;
  acceptingTerms: boolean = false;
  // OTP for application
  goldLoanOtpSent: boolean = false;
  goldLoanOtpInput: string = '';
  goldLoanOtpError: string = '';
  goldLoanOtpTimer: number = 0;
  goldLoanOtpTimerRef: any = null;
  isSendingGoldLoanOtp: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserInfo();
      this.loadCurrentGoldRate();
      this.loadGoldLoans();
    }
  }

  ngOnDestroy() {
    this.stopGoldLoanOtpTimer();
  }

  sendGoldLoanOtp() {
    if (!this.userAccountNumber) {
      this.alertService.error('Account Error', 'Account number not found.');
      return;
    }
    this.goldLoanOtpError = '';
    this.isSendingGoldLoanOtp = true;
    this.http.post(`${environment.apiBaseUrl}/api/gold-loans/request-otp`, null, {
      params: { accountNumber: this.userAccountNumber }
    })
      .pipe(timeout(15000), catchError(err => of({ success: false, message: err.error?.message || 'Failed to send OTP' })))
      .subscribe({
        next: (res: any) => {
          this.isSendingGoldLoanOtp = false;
          if (res && res.success) {
            this.goldLoanOtpSent = true;
            this.startGoldLoanOtpTimer();
            this.alertService.success('OTP Sent', 'OTP sent to your registered email. Valid for 2 minutes.');
          } else {
            this.goldLoanOtpError = res?.message || 'Failed to send OTP';
          }
        },
        error: () => {
          this.isSendingGoldLoanOtp = false;
          this.goldLoanOtpError = 'Failed to send OTP. Please try again.';
        }
      });
  }

  startGoldLoanOtpTimer() {
    this.stopGoldLoanOtpTimer();
    this.goldLoanOtpTimer = OTP_VALID_SECONDS;
    this.goldLoanOtpTimerRef = setInterval(() => {
      this.goldLoanOtpTimer--;
      if (this.goldLoanOtpTimer <= 0) this.stopGoldLoanOtpTimer();
    }, 1000);
  }

  stopGoldLoanOtpTimer() {
    if (this.goldLoanOtpTimerRef) {
      clearInterval(this.goldLoanOtpTimerRef);
      this.goldLoanOtpTimerRef = null;
    }
  }

  formatGoldLoanOtpTimer(): string {
    if (this.goldLoanOtpTimer <= 0) return '0:00';
    const m = Math.floor(this.goldLoanOtpTimer / 60);
    const s = this.goldLoanOtpTimer % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  loadUserInfo() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.userAccountNumber = user.accountNumber;
      this.userName = user.name;
      this.userEmail = user.email;
      
      // Load current balance (non-blocking)
      this.http.get(`${environment.apiBaseUrl}/api/accounts/balance/${this.userAccountNumber}`)
        .pipe(
          timeout(5000),
          catchError(err => {
            console.warn('Error loading balance (non-critical):', err);
            return of({ balance: 0 });
          })
        )
        .subscribe({
          next: (balanceData: any) => {
            this.currentBalance = balanceData?.balance || 0;
          }
        });
    }
  }

  loadCurrentGoldRate() {
    this.loading = true;
    this.http.get(`${environment.apiBaseUrl}/api/gold-rates/current`)
      .pipe(
        timeout(8000),
        catchError(err => {
          console.warn('Error loading gold rate (non-critical):', err);
          // Don't show error - use default or cached rate
          return of(null);
        })
      )
      .subscribe({
        next: (rate: any) => {
          if (rate) {
            this.currentGoldRate = rate;
          }
          this.loading = false;
        }
      });
  }

  calculateLoan() {
    if (this.goldGrams <= 0) {
      this.alertService.error('Validation Error', 'Please enter a valid amount of gold in grams');
      return;
    }

    this.calculating = true;
    this.http.get(`${environment.apiBaseUrl}/api/gold-loans/calculate?grams=${this.goldGrams}`)
      .pipe(
        timeout(10000), // 10 second timeout
        catchError(err => {
          console.error('Error calculating loan:', err);
          let errorMessage = 'Failed to calculate loan amount. ';
          if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
            errorMessage += 'Request timed out. The server may be slow.';
          } else if (err.status === 0) {
            errorMessage += 'Unable to connect to server. Please check if the backend is running.';
          } else if (err.status >= 500) {
            errorMessage += 'Server error. Please try again later.';
          } else if (err.error?.message) {
            errorMessage = err.error.message;
          }
          this.alertService.error('Calculation Error', errorMessage);
          this.calculating = false;
          return of(null);
        })
      )
      .subscribe({
        next: (response: any) => {
          if (!response) return; // Error occurred
          this.calculatedLoan = response;
          this.calculating = false;
          if (!response.success) {
            this.alertService.error('Calculation Error', response.message || 'Failed to calculate loan amount');
          }
        }
      });
  }

  applyGoldLoan() {
    if (!this.calculatedLoan || !this.calculatedLoan.success) {
      this.alertService.error('Validation Error', 'Please calculate loan amount first');
      return;
    }

    if (!this.acceptTerms) {
      this.alertService.error('Validation Error', 'Please accept the terms and conditions to proceed');
      return;
    }

    if (!this.userAccountNumber) {
      this.alertService.error('Account Error', 'Account number not found. Please refresh the page.');
      return;
    }

    if (!this.goldLoanOtpSent) {
      this.alertService.error('OTP Required', 'Please send OTP to your email first.');
      return;
    }
    const otp = (this.goldLoanOtpInput || '').trim();
    if (!/^\d{6}$/.test(otp)) {
      this.goldLoanOtpError = 'Enter a valid 6-digit OTP';
      return;
    }
    if (this.goldLoanOtpTimer <= 0) {
      this.goldLoanOtpError = 'OTP expired. Please send a new OTP.';
      return;
    }

    this.loading = true;
    this.goldLoanOtpError = '';

    const goldLoanData: GoldLoan = {
      goldGrams: this.goldGrams,
      goldRatePerGram: this.calculatedLoan.goldRatePerGram,
      goldValue: this.calculatedLoan.goldValue,
      loanAmount: this.calculatedLoan.loanAmount,
      tenure: this.tenure,
      interestRate: this.interestRate,
      status: 'Pending',
      userName: this.userName,
      userEmail: this.userEmail,
      accountNumber: this.userAccountNumber,
      currentBalance: this.currentBalance
    };

    this.loading = true;
    
    // Retry logic for gold loan submission
    let retryCount = 0;
    const maxRetries = 2;
    let isRetrying = false;
    
    const submitLoan = () => {
      if (isRetrying) {
        console.log(`Retrying gold loan submission (attempt ${retryCount + 1}/${maxRetries + 1})...`);
      }
      
      this.http.post(`${environment.apiBaseUrl}/api/gold-loans`, goldLoanData, {
        params: new HttpParams().set('otp', otp)
      })
        .pipe(
          timeout(15000), // 15 second timeout for submission
          catchError(err => {
            console.error(`Error applying for gold loan (attempt ${retryCount + 1}):`, err);
            
            // Retry on timeout or connection errors
            if ((err.name === 'TimeoutError' || err.status === 0) && retryCount < maxRetries) {
              retryCount++;
              isRetrying = true;
              // Retry after 2 seconds
              setTimeout(() => submitLoan(), 2000);
              // Return empty observable to prevent error from propagating
              return of({ _retrying: true });
            }
            
            // Final error handling after all retries exhausted
            isRetrying = false;
            let errorMessage = err.error?.message || 'Failed to submit gold loan application. ';
            if (err.name === 'TimeoutError' || err.error?.name === 'TimeoutError') {
              errorMessage = 'Request timed out after multiple attempts. The server may be slow or unavailable. Please check backend status and try again.';
            } else if (err.status === 0) {
              errorMessage = 'Unable to connect to server. Please check if the backend is running on port 8080.';
            } else if (err.status >= 500) {
              errorMessage = 'Server error occurred. The backend may be experiencing issues. Please try again later.';
            } else if (err.status === 400) {
              errorMessage = err.error?.message || 'Invalid request data. Please check your input.';
            }
            
            this.alertService.error('Application Failed', errorMessage);
            this.loading = false;
            return of({ _error: true });
          })
        )
        .subscribe({
          next: (response: any) => {
            // Skip retry responses
            if (response?._retrying) {
              return;
            }
            
            // Handle errors
            if (response?._error) {
              return;
            }
            
            // Success response
            this.loading = false;
            if (response && response.success) {
              this.alertService.success('Application Submitted', 
                `Gold loan application submitted successfully! Loan Amount: ₹${this.calculatedLoan.loanAmount.toFixed(2)}`);
              this.resetForm();
              this.stopGoldLoanOtpTimer();
              this.goldLoanOtpSent = false;
              this.goldLoanOtpInput = '';
              this.loadGoldLoans();
            } else if (response) {
              this.alertService.error('Application Failed', response.message || 'Failed to submit gold loan application');
            }
          }
        });
    };
    
    // Start submission
    submitLoan();
  }

  loadGoldLoans() {
    if (!this.userAccountNumber) return;
    
    this.loadingLoans = true;
    this.http.get(`${environment.apiBaseUrl}/api/gold-loans/account/${this.userAccountNumber}`)
      .pipe(
        timeout(8000),
        catchError(err => {
          console.warn('Error loading gold loans (non-critical):', err);
          return of([]);
        })
      )
      .subscribe({
        next: (loans: any) => {
          this.goldLoans = Array.isArray(loans) ? loans : [];
          this.loadingLoans = false;
        }
      });
  }

  resetForm() {
    this.goldGrams = 0;
    this.calculatedLoan = null;
    this.tenure = 12;
    this.acceptTerms = false;
    this.stopGoldLoanOtpTimer();
    this.goldLoanOtpSent = false;
    this.goldLoanOtpInput = '';
    this.goldLoanOtpError = '';
    this.goldLoanOtpTimer = 0;
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch (e) {
      return 'N/A';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Approved':
        return 'status-approved';
      case 'Pending':
        return 'status-pending';
      case 'Rejected':
        return 'status-rejected';
      default:
        return '';
    }
  }

  showTermsAndAccept(loan: GoldLoan) {
    this.selectedLoanForTerms = loan;
    this.showTermsModal = true;
  }

  acceptTermsAndConditions() {
    if (!this.selectedLoanForTerms || !this.selectedLoanForTerms.id) return;

    this.acceptingTerms = true;
    const acceptedBy = this.userAccountNumber || this.userEmail;

    this.http.put(
      `${environment.apiBaseUrl}/api/gold-loans/${this.selectedLoanForTerms.id}/accept-terms?acceptedBy=${acceptedBy}`,
      {}
    )
      .pipe(
        timeout(10000),
        catchError(err => {
          console.error('Error accepting terms:', err);
          let errorMessage = err.error?.message || 'Failed to accept terms. ';
          if (err.name === 'TimeoutError') {
            errorMessage = 'Request timed out. Please try again.';
          } else if (err.status === 0) {
            errorMessage = 'Unable to connect to server.';
          }
          this.alertService.error('Error', errorMessage);
          this.acceptingTerms = false;
          return of(null);
        })
      )
      .subscribe({
        next: (response: any) => {
          if (!response) return; // Error occurred
          if (response.success) {
            this.alertService.success('Terms Accepted', 'Terms and conditions accepted successfully!');
            this.showTermsModal = false;
            this.selectedLoanForTerms = null;
            this.acceptingTerms = false;
            this.loadGoldLoans();
          } else {
            this.alertService.error('Error', response.message || 'Failed to accept terms');
            this.acceptingTerms = false;
          }
        }
      });
  }

  closeTermsModal() {
    this.showTermsModal = false;
    this.selectedLoanForTerms = null;
  }
}

