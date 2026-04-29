import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-fasttag-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fasttag-login.html',
  styleUrls: ['./fasttag-login.css'],
  encapsulation: ViewEncapsulation.None
})
export class FasttagLogin implements OnInit, OnDestroy {
  gmailId: string = '';
  otp: string = '';

  // UI state
  otpSent: boolean = false;
  loading: boolean = false;
  verifying: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  // Resend OTP timer
  resendTimer: number = 0;
  resendInterval: any;
  canResend: boolean = false;

  isBrowser: boolean = false;

  constructor(
    private router: Router,
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (this.isBrowser) {
      // Check if already logged in
      const fastagUser = sessionStorage.getItem('fastagUser');
      if (fastagUser) {
        this.router.navigate(['/website/fasttag-dashboard']);
      }
    }
  }

  ngOnDestroy() {
    if (this.resendInterval) {
      clearInterval(this.resendInterval);
    }
  }

  isValidGmail(): boolean {
    const gmailRegex = /^[a-zA-Z0-9._%+-]+@gmail\.com$/i;
    return gmailRegex.test(this.gmailId.trim());
  }

  sendOtp() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.gmailId.trim()) {
      this.errorMessage = 'Please enter your Gmail ID.';
      return;
    }

    if (!this.isValidGmail()) {
      this.errorMessage = 'Please enter a valid Gmail address (e.g., user@gmail.com).';
      return;
    }

    this.loading = true;

    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/send-otp`, {
      gmailId: this.gmailId.trim()
    }).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.otpSent = true;
          this.successMessage = 'OTP sent successfully! Check your Gmail inbox.';
          this.startResendTimer();
        } else {
          this.errorMessage = res.message || 'Failed to send OTP.';
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to send OTP. Please try again.';
      }
    });
  }

  verifyOtp() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.otp.trim()) {
      this.errorMessage = 'Please enter the OTP.';
      return;
    }

    if (this.otp.trim().length !== 6) {
      this.errorMessage = 'OTP must be 6 digits.';
      return;
    }

    this.verifying = true;

    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/verify-otp`, {
      gmailId: this.gmailId.trim(),
      otp: this.otp.trim()
    }).subscribe({
      next: (res) => {
        this.verifying = false;
        if (res.success) {
          this.successMessage = 'Login successful! Redirecting...';
          // Store user session
          if (this.isBrowser) {
            sessionStorage.setItem('fastagUser', JSON.stringify(res.user));
            if (res.fasttags) {
              sessionStorage.setItem('fastagDetails', JSON.stringify(res.fasttags));
            }
          }
          // Redirect to FASTag dashboard
          setTimeout(() => {
            this.router.navigate(['/website/fasttag-dashboard']);
          }, 1000);
        } else {
          this.errorMessage = res.message || 'OTP verification failed.';
        }
      },
      error: (err) => {
        this.verifying = false;
        this.errorMessage = err.error?.message || 'Verification failed. Please try again.';
      }
    });
  }

  startResendTimer() {
    this.canResend = false;
    this.resendTimer = 30;

    if (this.resendInterval) {
      clearInterval(this.resendInterval);
    }

    this.resendInterval = setInterval(() => {
      this.resendTimer--;
      if (this.resendTimer <= 0) {
        clearInterval(this.resendInterval);
        this.canResend = true;
      }
    }, 1000);
  }

  resendOtp() {
    if (!this.canResend) return;
    this.otp = '';
    this.sendOtp();
  }

  resetForm() {
    this.otpSent = false;
    this.otp = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.canResend = false;
    this.resendTimer = 0;
    if (this.resendInterval) {
      clearInterval(this.resendInterval);
    }
  }

  goToLanding() {
    this.router.navigate(['/website/landing']);
  }
}
