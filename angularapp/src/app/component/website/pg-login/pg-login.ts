import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { PaymentGatewayService } from '../../../service/payment-gateway.service';

@Component({
  selector: 'app-pg-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pg-login.html',
  styleUrls: ['./pg-login.css'],
  encapsulation: ViewEncapsulation.None
})
export class PgLogin implements OnInit, OnDestroy {
  email = '';
  otp = '';

  otpSent = false;
  loading = false;
  verifying = false;
  errorMessage = '';
  successMessage = '';
  maskedEmail = '';
  businessName = '';

  resendTimer = 0;
  resendInterval: any;
  canResend = false;

  isBrowser = false;

  // Register merchant
  showRegisterMerchant = false;
  registerLoading = false;
  registerForm = {
    businessName: '',
    businessEmail: '',
    businessPhone: '',
    businessType: 'ONLINE',
    webhookUrl: '',
    callbackUrl: '',
    accountNumber: '',
    settlementAccount: ''
  };

  constructor(
    private router: Router,
    private pgService: PaymentGatewayService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (this.isBrowser) {
      // Clear any stale session so login page always shows
      sessionStorage.removeItem('pgMerchant');
    }
  }

  ngOnDestroy() {
    if (this.resendInterval) clearInterval(this.resendInterval);
  }

  isValidEmail(): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim());
  }

  sendOtp() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.email.trim()) {
      this.errorMessage = 'Please enter your registered business email.';
      return;
    }
    if (!this.isValidEmail()) {
      this.errorMessage = 'Please enter a valid email address.';
      return;
    }

    this.loading = true;
    this.pgService.sendLoginOtp(this.email.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.otpSent = true;
          this.maskedEmail = res.maskedEmail || '';
          this.businessName = res.businessName || '';
          this.successMessage = 'OTP sent to ' + this.maskedEmail;
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
      this.errorMessage = 'Please enter OTP.';
      return;
    }
    if (this.otp.trim().length !== 6) {
      this.errorMessage = 'OTP must be 6 digits.';
      return;
    }

    this.verifying = true;
    this.pgService.verifyLoginOtp(this.email.trim(), this.otp.trim()).subscribe({
      next: (res) => {
        this.verifying = false;
        if (res.success) {
          this.successMessage = 'Login successful! Redirecting...';
          if (this.isBrowser) {
            sessionStorage.setItem('pgMerchant', JSON.stringify(res.merchant));
          }
          setTimeout(() => {
            this.router.navigate(['/website/pg-dashboard']);
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
    if (this.resendInterval) clearInterval(this.resendInterval);
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
    this.email = '';
    this.maskedEmail = '';
    this.businessName = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.canResend = false;
    this.resendTimer = 0;
    if (this.resendInterval) clearInterval(this.resendInterval);
  }

  goToLanding() {
    this.router.navigate(['/website/landing']);
  }

  registerMerchant() {
    if (!this.registerForm.businessName || !this.registerForm.businessEmail) return;
    this.registerLoading = true;
    this.pgService.registerMerchant(this.registerForm).subscribe({
      next: (res: any) => {
        this.registerLoading = false;
        if (res.success) {
          this.showRegisterMerchant = false;
          this.email = this.registerForm.businessEmail;
          this.successMessage = 'Merchant registered successfully! You can now login with your business email.';
          this.registerForm = {
            businessName: '', businessEmail: '', businessPhone: '',
            businessType: 'ONLINE', webhookUrl: '', callbackUrl: '',
            accountNumber: '', settlementAccount: ''
          };
        }
      },
      error: (err: any) => {
        this.registerLoading = false;
        this.errorMessage = err.error?.error || 'Registration failed. Please try again.';
      }
    });
  }
}
