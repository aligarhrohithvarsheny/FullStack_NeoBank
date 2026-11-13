import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { environment } from '../../../../environment/environment';

interface CreditCardAgreementDetails {
  applicationId: string | null;
  applicantName: string;
  applicantEmail: string;
  panLastFour: string;
  mobileLastFour: string;
  address: string;
  dateOfBirth: string;
  employmentType: string;
  employerName: string;
  designation: string;
  annualIncome: number;
  predictedCibil: number;
  scoreNotice: string;
  cardType: string;
  approvedLimit: number;
  interestRate: number;
  annualFee: number;
  expiresAt: string;
  signatureNotice: string;
}

@Component({
  selector: 'app-credit-card-esign',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './credit-card-esign.html',
  styleUrls: ['./credit-card-esign.css']
})
export class CreditCardEsign implements OnInit {
  readonly apiUrl = `${environment.apiBaseUrl}/api/credit-card-requests`;
  token = '';
  agreement: CreditCardAgreementDetails | null = null;
  captcha: { challengeId: string; image: string; expiresAt: string } | null = null;
  signedName = '';
  captchaAnswer = '';
  consent = false;
  loading = true;
  signing = false;
  signed = false;
  error = '';
  notice = '';

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.loading = false;
      return;
    }
    this.token = this.route.snapshot.paramMap.get('token') || '';
    this.loadAgreement();
  }

  loadAgreement(): void {
    this.loading = true;
    this.error = '';
    this.http.get<CreditCardAgreementDetails>(
      `${this.apiUrl}/agreement/${encodeURIComponent(this.token)}`
    ).subscribe({
      next: agreement => {
        this.agreement = agreement;
        this.signedName = agreement.applicantName;
        this.loading = false;
        this.refreshCaptcha();
      },
      error: error => {
        this.loading = false;
        this.error = error.error?.message || 'This e-sign link is invalid or has expired.';
      }
    });
  }

  refreshCaptcha(): void {
    this.captchaAnswer = '';
    this.http.get<{ challengeId: string; image: string; expiresAt: string }>(
      `${this.apiUrl}/agreement/${encodeURIComponent(this.token)}/captcha`
    ).subscribe({
      next: captcha => {
        this.captcha = captcha;
        this.error = '';
      },
      error: error => {
        this.captcha = null;
        this.error = error.error?.message || 'Could not load CAPTCHA. Ask the bank to renew this e-sign link.';
      }
    });
  }

  sign(): void {
    if (!this.agreement || !this.captcha || this.signing
      || !this.signedName.trim() || !this.captchaAnswer.trim() || !this.consent) {
      return;
    }
    this.signing = true;
    this.error = '';
    this.http.post<{ status: string; message: string }>(
      `${this.apiUrl}/agreement/${encodeURIComponent(this.token)}/sign`,
      {
        challengeId: this.captcha.challengeId,
        captchaAnswer: this.captchaAnswer,
        signedName: this.signedName.trim(),
        consent: this.consent
      }
    ).subscribe({
      next: result => {
        this.signing = false;
        this.signed = true;
        this.captcha = null;
        this.notice = result.message;
      },
      error: error => {
        this.signing = false;
        this.error = error.error?.message || 'The signature could not be recorded. Please retry.';
        this.refreshCaptcha();
      }
    });
  }
}
