import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-rd-review',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="review">
      <header><img src="/assets/images/neobank-logo.svg" alt="NeoBank" /><span>Recurring deposit terms</span></header>
      <section class="notice">
        <strong>Terms review and customer acknowledgement</strong>
        <p>This page summarizes the RD details for your review. It is not a legal contract or a bank-issued e-signature. A separate customer-approved ECS mandate is required before installments can be debited.</p>
      </section>
      <section class="card" *ngIf="loading">Loading recurring-deposit details…</section>
      <section class="card error" *ngIf="error" role="alert">{{ error }}</section>
      <ng-container *ngIf="deposit && !loading">
        <section class="card">
          <div class="title-row"><div><small>RD ACCOUNT</small><h1>{{ deposit.rdAccountNumber }}</h1><p>{{ deposit.customerName }} · linked account {{ deposit.accountNumber }}</p></div><b>{{ deposit.status }}</b></div>
          <dl>
            <div><dt>Scheme</dt><dd>{{ deposit.schemeName }}</dd></div>
            <div><dt>Monthly installment</dt><dd>{{ deposit.monthlyInstallment | currency:'INR' }}</dd></div>
            <div><dt>Interest rate</dt><dd>{{ deposit.annualInterestRate }}% p.a.</dd></div>
            <div><dt>Tenure</dt><dd>{{ deposit.tenureMonths }} months</dd></div>
            <div><dt>Start date</dt><dd>{{ deposit.startDate | date:'mediumDate' }}</dd></div>
            <div><dt>Maturity date</dt><dd>{{ deposit.maturityDate | date:'mediumDate' }}</dd></div>
            <div><dt>Estimated principal</dt><dd>{{ deposit.estimatedPrincipal | currency:'INR' }}</dd></div>
            <div><dt>Estimated interest</dt><dd>{{ deposit.estimatedInterest | currency:'INR' }}</dd></div>
            <div><dt>Estimated maturity value</dt><dd>{{ deposit.estimatedMaturityAmount | currency:'INR' }}</dd></div>
            <div><dt>Payout instruction</dt><dd>{{ deposit.payoutInstruction || 'At maturity' }}</dd></div>
          </dl>
          <small class="expiry">Link expires {{ deposit.expiresAt | date:'medium' }}</small>
        </section>
        <section class="card" *ngIf="deposit.status === 'PENDING_SIGNATURE'">
          <h2>Review and acknowledge</h2>
          <label>Full name<input [(ngModel)]="signedName" maxlength="120" autocomplete="name" /></label>
          <div class="captcha"><span>{{ captcha?.question || 'CAPTCHA loading…' }}</span><button type="button" (click)="refreshCaptcha()" [disabled]="signing">New question</button></div>
          <label>Answer<input [(ngModel)]="captchaAnswer" inputmode="numeric" maxlength="3" autocomplete="off" /></label>
          <label class="check"><input type="checkbox" [(ngModel)]="consent" /><span>I reviewed these RD terms and understand this typed-name acknowledgement is not a legal e-signature or ECS debit authorization.</span></label>
          <button class="primary" type="button" (click)="sign()" [disabled]="signing || !captcha || !signedName.trim() || !captchaAnswer.trim() || !consent">
            {{ signing ? 'Recording…' : 'Acknowledge RD terms' }}
          </button>
        </section>
        <section class="card success" *ngIf="deposit.status === 'SIGNED'">
          {{ notice || 'Your acknowledgement is recorded. Staff approval is still required.' }}
        </section>
      </ng-container>
    </main>`,
  styles: [`
    :host{display:block;min-height:100vh;background:#f3f6fb;color:#14233b;font-family:Arial,sans-serif}
    .review{max-width:860px;margin:auto;padding:24px 16px 48px}header{display:flex;align-items:center;gap:14px;margin-bottom:20px;font-weight:700}
    header img{width:48px;height:48px}.card,.notice{background:#fff;border:1px solid #dce4ef;border-radius:14px;padding:22px;margin:14px 0;box-shadow:0 6px 24px #172b4d0a}
    .notice{background:#fff8e8;border-color:#f2d89b}.notice p{line-height:1.55}.title-row{display:flex;justify-content:space-between;gap:12px}.title-row h1{margin:5px 0}.title-row p,.expiry{color:#607089}
    dl{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:14px}dl div{padding:10px;background:#f7f9fc;border-radius:8px}dt{font-size:12px;color:#607089}dd{margin:6px 0 0;font-weight:700}
    label{display:block;font-weight:600;margin:14px 0}label input:not([type=checkbox]){display:block;width:100%;box-sizing:border-box;padding:11px;border:1px solid #c8d2e0;border-radius:8px;margin-top:6px}
    .captcha{display:flex;align-items:center;justify-content:space-between;background:#eff4fa;padding:12px;border-radius:8px}.check{display:flex;gap:10px;align-items:flex-start;font-weight:400;line-height:1.5}
    button{cursor:pointer;border:1px solid #c8d2e0;border-radius:8px;padding:10px 14px;background:white}.primary{background:#1357bd;border-color:#1357bd;color:white;font-weight:700;width:100%}.primary:disabled{opacity:.55}
    .error{color:#a11622}.success{color:#146b3a;background:#effaf3}
  `]
})
export class RecurringDepositReviewComponent implements OnInit {
  deposit: any = null;
  token = '';
  captcha: { challengeId: string; question: string } | null = null;
  signedName = '';
  captchaAnswer = '';
  consent = false;
  loading = true;
  signing = false;
  error = '';
  notice = '';
  private readonly api = `${environment.apiBaseUrl}/api/recurring-deposits`;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.loading = false;
      return;
    }
    this.token = this.route.snapshot.paramMap.get('token') || '';
    this.http.get<any>(`${this.api}/share/${this.token}`).subscribe({
      next: deposit => {
        this.deposit = deposit;
        this.loading = false;
        if (deposit.status === 'PENDING_SIGNATURE') this.refreshCaptcha();
      },
      error: error => {
        this.loading = false;
        this.error = error.error?.message || 'This recurring-deposit link is invalid or expired.';
      }
    });
  }

  refreshCaptcha(): void {
    this.captchaAnswer = '';
    this.http.get<any>(`${this.api}/share/${this.token}/captcha`).subscribe({
      next: challenge => {
        this.captcha = challenge;
        this.error = '';
      },
      error: error => {
        this.captcha = null;
        this.error = error.error?.message || 'Could not load a verification question.';
      }
    });
  }

  sign(): void {
    if (!this.captcha || this.signing || !this.consent || !this.signedName.trim() || !this.captchaAnswer.trim()) return;
    this.signing = true;
    this.error = '';
    this.http.post<any>(`${this.api}/share/${this.token}/sign`, {
      challengeId: this.captcha.challengeId,
      captchaAnswer: this.captchaAnswer,
      signedName: this.signedName.trim(),
      consent: this.consent
    }).subscribe({
      next: response => {
        this.signing = false;
        this.deposit.status = response.status;
        this.captcha = null;
        this.notice = response.message;
      },
      error: error => {
        this.signing = false;
        this.error = error.error?.message || 'The acknowledgement could not be recorded.';
        this.refreshCaptcha();
      }
    });
  }
}
