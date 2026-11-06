import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnDestroy, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subscription } from 'rxjs';
import { LoanAgreementService } from '../../../service/loan-agreement.service';

@Component({
  selector: 'app-loan-esign',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan-esign.html',
  styleUrls: ['./loan-esign.css']
})
export class LoanEsign implements OnInit, OnDestroy {
  token = '';
  agreement: any = null;
  captcha: { challengeId: string; image: string; expiresAt: string } | null = null;
  signedName = '';
  captchaAnswer = '';
  consent = false;
  loading = true;
  signing = false;
  error = '';
  notice = '';
  pdfUrl: SafeResourceUrl | null = null;
  private pdfObjectUrl: string | null = null;
  private eventSubscription?: Subscription;

  get pdfReady(): boolean {
    return this.pdfObjectUrl !== null;
  }

  constructor(
    private route: ActivatedRoute,
    private agreementService: LoanAgreementService,
    private sanitizer: DomSanitizer,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.loading = false;
      return;
    }
    this.token = this.route.snapshot.paramMap.get('token') || '';
    this.loadAgreement();
    this.agreementService.connectToUpdates();
    this.eventSubscription = this.agreementService.events$.subscribe(event => {
      if (this.agreement?.id === event.agreementId && event.status === 'APPROVED') {
        this.agreement.status = 'APPROVED';
        this.notice = 'The bank completed final approval for this loan.';
      }
    });
  }

  ngOnDestroy(): void {
    this.eventSubscription?.unsubscribe();
    if (this.pdfObjectUrl) URL.revokeObjectURL(this.pdfObjectUrl);
  }

  loadAgreement(): void {
    this.loading = true;
    this.error = '';
    this.agreementService.getShareDetails(this.token).subscribe({
      next: details => {
        this.agreement = details;
        this.loading = false;
        this.loadPdf();
        if (details.status === 'PENDING_SIGNATURE') this.refreshCaptcha();
      },
      error: error => {
        this.loading = false;
        this.error = error.error?.message || 'This loan agreement link is invalid or has expired.';
      }
    });
  }

  refreshCaptcha(): void {
    this.captchaAnswer = '';
    this.agreementService.getCaptcha(this.token).subscribe({
      next: challenge => {
        this.captcha = challenge;
        this.error = '';
      },
      error: error => {
        this.captcha = null;
        this.error = error.error?.message || 'Could not load CAPTCHA. Refresh the page or request a new link.';
      }
    });
  }

  loadPdf(): void {
    this.agreementService.getSharePdf(this.token).subscribe({
      next: blob => {
        if (this.pdfObjectUrl) URL.revokeObjectURL(this.pdfObjectUrl);
        this.pdfObjectUrl = URL.createObjectURL(blob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl);
      },
      error: error => {
        this.error = error.error?.message || 'Could not load the agreement PDF.';
      }
    });
  }

  sign(): void {
    if (!this.captcha || this.signing || !this.signedName.trim() || !this.captchaAnswer.trim() || !this.consent) {
      return;
    }
    this.signing = true;
    this.error = '';
    this.agreementService.sign(this.token, {
      challengeId: this.captcha.challengeId,
      captchaAnswer: this.captchaAnswer,
      signedName: this.signedName.trim(),
      consent: this.consent
    }).subscribe({
      next: response => {
        this.signing = false;
        this.agreement.status = response.status;
        this.captcha = null;
        this.notice = response.message;
        this.loadPdf();
      },
      error: error => {
        this.signing = false;
        this.error = error.error?.message || 'The signature could not be recorded. Please retry.';
        this.refreshCaptcha();
      }
    });
  }

  downloadPdf(): void {
    if (!isPlatformBrowser(this.platformId) || !this.pdfObjectUrl) return;
    const anchor = document.createElement('a');
    anchor.href = this.pdfObjectUrl;
    anchor.download = 'neobank-loan-agreement.pdf';
    anchor.click();
  }
}
