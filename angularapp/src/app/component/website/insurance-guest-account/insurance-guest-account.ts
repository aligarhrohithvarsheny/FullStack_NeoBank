import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-insurance-guest-account',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './insurance-guest-account.html',
  styleUrl: './insurance-guest-account.css'
})
export class InsuranceGuestAccount implements OnInit {
  account: any = null;
  loading = true;
  error = '';
  closing = false;
  startingPayment = false;
  payerAccountNumber = '';
  transactionPin = '';
  paymentMessage = '';
  claimAmount: number | null = null;
  claimReason = '';
  claimDetails = '';
  submittingClaim = false;
  claimMessage = '';

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadAccount();
  }

  loadAccount(): void {
    this.loading = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/insurance/guest-account`).subscribe({
      next: account => {
        this.account = account;
        this.loading = false;
      },
      error: error => {
        this.error = error.error?.message || 'Unable to load your insurance account.';
        this.loading = false;
      }
    });
  }

  closeInsurance(): void {
    if (this.closing || !confirm('Close this insurance application? This cannot be undone.')) return;
    this.closing = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/insurance/guest-account/close`, {}).subscribe({
      next: response => {
        this.closing = false;
        if (!response?.success) {
          this.error = response?.message || 'Unable to close this insurance.';
          return;
        }
        this.loadAccount();
      },
      error: error => {
        this.closing = false;
        this.error = error.error?.message || 'Unable to close this insurance.';
      }
    });
  }

  isPremiumDue(): boolean {
    const dueDate = String(this.account?.nextPremiumDueDate || '').slice(0, 10);
    if (!dueDate) return true;
    const now = new Date();
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    return dueDate <= today;
  }

  isPaymentInputValid(): boolean {
    return this.payerAccountNumber.trim().length > 0 && /^[0-9]{6}$/.test(this.transactionPin);
  }

  payPremium(): void {
    if (this.startingPayment || !isPlatformBrowser(this.platformId)) return;
    this.startingPayment = true;
    this.paymentMessage = '';
    this.error = '';
    this.http.post<any>(`${environment.apiBaseUrl}/api/insurance/guest-account/premium-payment`, {
      payerAccountNumber: this.payerAccountNumber,
      pin: this.transactionPin
    }).subscribe({
      next: result => {
        this.startingPayment = false;
        this.transactionPin = '';
        if (!result?.success) {
          this.error = result?.message || 'Unable to complete the UPI payment.';
          return;
        }
        this.paymentMessage = `${result.message || 'Premium paid.'} Reference: ${result.transactionReference}`;
        this.loadAccount();
      },
      error: error => {
        this.startingPayment = false;
        this.transactionPin = '';
        this.error = error.error?.message || 'Unable to complete the UPI payment.';
      }
    });
  }

  submitClaim(): void {
    if (this.submittingClaim) return;
    this.submittingClaim = true;
    this.claimMessage = '';
    this.error = '';
    this.http.post<any>(`${environment.apiBaseUrl}/api/insurance/guest-account/claims`, {
      claimAmount: this.claimAmount,
      reason: this.claimReason,
      details: this.claimDetails
    }).subscribe({
      next: response => {
        this.submittingClaim = false;
        if (!response?.success) {
          this.error = response?.message || 'Unable to submit your claim.';
          return;
        }
        this.claimMessage = `${response.message || 'Claim submitted.'} Reference: ${response.claimNumber}`;
        this.claimAmount = null;
        this.claimReason = '';
        this.claimDetails = '';
        this.loadAccount();
      },
      error: error => {
        this.submittingClaim = false;
        this.error = error.error?.message || 'Unable to submit your claim.';
      }
    });
  }

  downloadCertificate(): void {
    this.http.get(`${environment.apiBaseUrl}/api/insurance/guest-account/certificate`, {
      responseType: 'blob'
    }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `insurance-certificate-${this.account?.applicationNumber || 'policy'}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: error => this.error = error.error?.message || 'Unable to download the insurance certificate.'
    });
  }

  signOut(): void {
    sessionStorage.removeItem('insuranceGuest');
    sessionStorage.removeItem('insuranceAuthToken');
    this.router.navigate(['/website/landing']);
  }
}
