import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { MerchantOnboardingService } from '../../../service/merchant-onboarding.service';

@Component({
  selector: 'app-merchant-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './merchant-login.html',
  styleUrls: ['./merchant-login.css'],
  encapsulation: ViewEncapsulation.None
})
export class MerchantLogin implements OnInit, OnDestroy {
  email: string = '';
  otp: string = '';

  // UI state
  otpSent: boolean = false;
  loading: boolean = false;
  verifying: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';
  maskedEmail: string = '';
  businessName: string = '';

  // Resend OTP timer
  resendTimer: number = 0;
  resendInterval: any;
  canResend: boolean = false;

  isBrowser: boolean = false;

  constructor(
    private router: Router,
    private merchantService: MerchantOnboardingService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (this.isBrowser) {
      const merchant = sessionStorage.getItem('merchantSoundbox');
      if (merchant) {
        this.router.navigate(['/website/soundbox-payment']);
      }
    }
  }

  ngOnDestroy() {
    if (this.resendInterval) {
      clearInterval(this.resendInterval);
    }
  }

  isValidEmail(): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim());
  }

  sendOtp() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.email.trim()) {
      this.errorMessage = 'Please enter your registered email.';
      return;
    }

    if (!this.isValidEmail()) {
      this.errorMessage = 'Please enter a valid email address.';
      return;
    }

    this.loading = true;

    this.merchantService.sendMerchantLoginOtp(this.email.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.otpSent = true;
          this.maskedEmail = res.maskedEmail || '';
          this.businessName = res.businessName || '';
          this.successMessage = 'OTP sent successfully to ' + this.maskedEmail + '.';
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

    this.merchantService.verifyMerchantLoginOtp(this.email.trim(), this.otp.trim()).subscribe({
      next: (res) => {
        this.verifying = false;
        if (res.success) {
          this.successMessage = 'Login successful! Redirecting...';
          if (this.isBrowser) {
            sessionStorage.setItem('merchantSoundbox', JSON.stringify(res.merchant));
            const firstDevice = Array.isArray(res.devices) && res.devices.length > 0 ? res.devices[0] : null;
            if (firstDevice) {
              sessionStorage.setItem('merchantDevice', JSON.stringify(firstDevice));
            }
          }
          setTimeout(() => {
            this.router.navigate(['/website/soundbox-payment']);
          }, 1000);
        } else {
          this.errorMessage = res.message || 'OTP verification failed.';
        }
      },
      error: (err) => {
        this.verifying = false;
      this.errorMessage = err.error?.error || err.error?.message || 'Verification failed. Please try again.';
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
    this.email = '';
    this.maskedEmail = '';
    this.businessName = '';
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
