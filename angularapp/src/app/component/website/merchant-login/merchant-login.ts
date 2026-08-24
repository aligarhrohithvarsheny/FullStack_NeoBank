import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
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
export class MerchantLogin implements OnInit {
  merchantId: string = '';
  phoneNumber: string = '';

  // UI state
  loading: boolean = false;
  verifying: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';
  businessName: string = '';

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

  login() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.merchantId.trim() || !/^\d{10}$/.test(this.phoneNumber.trim())) {
      this.errorMessage = 'Enter a valid merchant ID and 10-digit phone number.';
      return;
    }

    this.loading = true;

    this.merchantService.loginMerchantByCredentials(this.merchantId.trim(), this.phoneNumber.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.businessName = res.businessName || '';
          this.successMessage = 'Login successful! Redirecting...';
          if (this.isBrowser) {
            sessionStorage.setItem('merchantSoundbox', JSON.stringify(res.merchant));
            const firstDevice = Array.isArray(res.devices) && res.devices.length > 0 ? res.devices[0] : null;
            if (firstDevice) sessionStorage.setItem('merchantDevice', JSON.stringify(firstDevice));
          }
          setTimeout(() => this.router.navigate(['/website/soundbox-payment']), 500);
        } else {
          this.errorMessage = res.error || res.message || 'Login failed.';
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.error || err.error?.message || 'Login failed. Please try again.';
      }
    });
  }


  resetForm() {
    this.merchantId = '';
    this.phoneNumber = '';
    this.businessName = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  goToLanding() {
    this.router.navigate(['/website/landing']);
  }
}
