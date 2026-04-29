import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-insurance-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './insurance-login.html',
  styleUrls: ['./insurance-login.css']
})
export class InsuranceLogin {
  policyNumberOrEmail: string = '';
  email: string = '';
  otp: string = '';
  step: 'details' | 'otp' = 'details';
  sendingOtp: boolean = false;
  verifyingOtp: boolean = false;
  // when user entered a policy number and lookup succeeded, store it for post-OTP redirect
  foundPolicyNumber: string | null = null;

  constructor(
    private http: HttpClient,
    private router: Router,
    private alertService: AlertService
  ) {}

  sendOtp() {
    const email = (this.email || this.policyNumberOrEmail || '').trim();
    if (!email) {
      this.alertService.userError('Validation Error', 'Please enter policy number or registered email.');
      return;
    }
    // If input looks like a policy number (contains letters + digits or starts with 'POL'/'NBID'), lookup policy
    const q = (this.policyNumberOrEmail || '').trim();
    const looksLikePolicy = /[A-Za-z].*\d|^(POL|NBID)/i.test(q);

    if (looksLikePolicy && !this.email) {
      this.sendingOtp = true;
      this.http.get<any>(`${environment.apiBaseUrl}/api/insurance/policy/lookup/${encodeURIComponent(q)}`).subscribe({
        next: (res) => {
          this.sendingOtp = false;
          if (res?.success && res.userEmail) {
            this.email = res.userEmail;
            this.foundPolicyNumber = q; // remember for redirect after OTP
            // proceed to send OTP to found email
            this.sendOtpToEmail(this.email);
          } else {
            this.alertService.userError('Not Found', res?.message || 'Policy number not found or not linked to any account.');
          }
        },
        error: (err) => {
          this.sendingOtp = false;
          this.alertService.userError('Lookup Failed', err.error?.message || 'Failed to lookup policy number');
        }
      });
      return;
    }

    // otherwise treat input as email
    this.sendOtpToEmail(email);
  }

  private sendOtpToEmail(email: string) {
    this.sendingOtp = true;
    this.http.post(`${environment.apiBaseUrl}/api/users/send-reset-otp`, { email }).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.userSuccess('OTP Sent', res.message || 'OTP sent to your registered email.');
          this.step = 'otp';
        } else {
          this.alertService.userError('Failed', res?.message || 'Unable to send OTP.');
        }
        this.sendingOtp = false;
      },
      error: (err) => {
        this.sendingOtp = false;
        this.alertService.userError('Failed', err.error?.message || 'Unable to send OTP.');
      }
    });
  }

  verifyOtp() {
    const email = (this.email || this.policyNumberOrEmail || '').trim();
    if (!email || !this.otp || this.otp.length !== 6) {
      this.alertService.userError('Validation Error', 'Please enter a valid OTP.');
      return;
    }

    this.verifyingOtp = true;
    this.http.post(`${environment.apiBaseUrl}/api/users/reset-password-with-otp`, {
      email,
      otp: this.otp,
      newPassword: 'TEMP-' + Date.now() // not used for full login, just OTP verification
    }).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.userSuccess('Verified', 'OTP verified. You can now manage insurance.');
          // After verification, if this flow was initiated for a policy number,
          // navigate directly to the insurance page with the policy query param
          if (this.foundPolicyNumber) {
            this.router.navigate(['/website/insurance'], { queryParams: { policy: this.foundPolicyNumber } });
            this.foundPolicyNumber = null;
          } else {
            this.router.navigate(['/website/insurance']);
          }
        } else {
          this.alertService.userError('Verification Failed', res?.message || 'Invalid OTP.');
        }
        this.verifyingOtp = false;
      },
      error: (err) => {
        this.verifyingOtp = false;
        this.alertService.userError('Verification Failed', err.error?.message || 'Invalid OTP.');
      }
    });
  }
}

