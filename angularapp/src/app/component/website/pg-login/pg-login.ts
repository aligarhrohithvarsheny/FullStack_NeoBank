import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
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
export class PgLogin implements OnInit {
  merchantId = '';
  phoneNumber = '';

  loading = false;
  verifying = false;
  errorMessage = '';
  successMessage = '';
  businessName = '';

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

  login() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.merchantId.trim() || !/^\d{10}$/.test(this.phoneNumber.trim())) {
      this.errorMessage = 'Enter a valid merchant ID and 10-digit phone number.';
      return;
    }

    this.loading = true;
    this.pgService.loginByCredentials(this.merchantId.trim(), this.phoneNumber.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.businessName = res.businessName || '';
          this.successMessage = 'Login successful! Redirecting...';
          if (this.isBrowser) sessionStorage.setItem('pgMerchant', JSON.stringify(res.merchant));
          setTimeout(() => this.router.navigate(['/website/pg-dashboard']), 500);
        } else {
          this.errorMessage = res.message || 'Login failed.';
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Login failed. Please try again.';
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

  registerMerchant() {
    if (!this.registerForm.businessName || !this.registerForm.businessEmail) return;
    this.registerLoading = true;
    this.pgService.registerMerchant(this.registerForm).subscribe({
      next: (res: any) => {
        this.registerLoading = false;
        if (res.success) {
          this.showRegisterMerchant = false;
          this.successMessage = 'Merchant registered successfully! You can now login with your merchant ID and phone number.';
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
