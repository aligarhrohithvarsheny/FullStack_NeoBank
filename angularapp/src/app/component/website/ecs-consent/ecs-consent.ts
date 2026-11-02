import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-ecs-consent',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="consent-page">
      <header><img src="/assets/images/neobank-logo.svg" alt="NeoBank" /><span>Recurring deposit ECS authorization</span></header>
      <section class="warning"><strong>Separate customer authorization</strong><p>Confirming below authorizes the displayed monthly debit. No debit happens before your confirmation; the first eligible debit is no earlier than the following calendar month.</p></section>
      <section class="card" *ngIf="loading">Loading mandate details…</section>
      <section class="card error" *ngIf="error" role="alert">{{ error }}</section>
      <section class="card" *ngIf="details && !loading">
        <h1>{{ details.loanType }}</h1>
        <p>{{ details.customerName }} · mandate {{ details.mandateId }}</p>
        <dl>
          <div><dt>RD account</dt><dd>{{ details.loanAccountNumber }}</dd></div>
          <div><dt>Debit account</dt><dd>{{ details.savingsAccountNumber }}</dd></div>
          <div><dt>Monthly contribution</dt><dd>{{ details.monthlyAmount | currency:'INR' }}</dd></div>
          <div><dt>Maximum debit per month</dt><dd>{{ details.amountLimit | currency:'INR' }}</dd></div>
          <div><dt>Debit day</dt><dd>{{ details.debitDay }} of each month</dd></div>
        </dl>
        <p class="notice">{{ details.notice }}</p>
        <div *ngIf="!completed">
          <label>Account holder full name<input [(ngModel)]="fullName" maxlength="128" autocomplete="name" /></label>
          <label class="check"><input type="checkbox" [(ngModel)]="consent" /><span>I authorize NeoBank to debit the displayed monthly RD installment from the masked debit account on the displayed debit day, subject to the amount limit. I understand the mandate can be cancelled through the bank.</span></label>
          <button class="primary" (click)="confirm()" [disabled]="busy || !fullName.trim() || !consent">{{ busy ? 'Recording consent…' : 'Confirm ECS authorization' }}</button>
        </div>
        <div *ngIf="completed" class="success">{{ completed }}</div>
      </section>
    </main>`,
  styles: [`
    :host{display:block;min-height:100vh;background:#f3f6fb;color:#14233b;font-family:Arial,sans-serif}
    .consent-page{max-width:760px;margin:auto;padding:24px 16px 48px}header{display:flex;align-items:center;gap:14px;margin-bottom:20px;font-weight:700}header img{width:48px;height:48px}
    .card,.warning{background:#fff;border:1px solid #dce4ef;border-radius:14px;padding:22px;margin:14px 0;box-shadow:0 6px 24px #172b4d0a}
    .warning{background:#fff8e8;border-color:#f2d89b}.warning p,.notice{line-height:1.55}dl{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:14px}
    dl div{padding:10px;background:#f7f9fc;border-radius:8px}dt{font-size:12px;color:#607089}dd{margin:6px 0 0;font-weight:700}
    label{display:block;font-weight:600;margin:14px 0}label input:not([type=checkbox]){display:block;width:100%;box-sizing:border-box;padding:11px;border:1px solid #c8d2e0;border-radius:8px;margin-top:6px}
    .check{display:flex;gap:10px;align-items:flex-start;font-weight:400;line-height:1.5}.primary{cursor:pointer;border:0;border-radius:8px;padding:12px;background:#1357bd;color:white;font-weight:700;width:100%}.primary:disabled{opacity:.55}
    .error{color:#a11622}.success{color:#146b3a;background:#effaf3;padding:12px;border-radius:8px}
  `]
})
export class EcsConsentComponent implements OnInit {
  details: any = null;
  token = '';
  fullName = '';
  consent = false;
  loading = true;
  busy = false;
  completed = '';
  error = '';
  private readonly api = `${environment.apiBaseUrl}/api/ecs-mandates`;

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
    this.http.get<any>(`${this.api}/consent/${this.token}`).subscribe({
      next: details => {
        this.details = details;
        this.fullName = details.customerName || '';
        this.loading = false;
      },
      error: error => {
        this.loading = false;
        this.error = error.error?.message || 'This ECS consent link is invalid or expired.';
      }
    });
  }

  confirm(): void {
    if (this.busy || !this.consent || !this.fullName.trim()) return;
    this.busy = true;
    this.error = '';
    this.http.post<any>(`${this.api}/consent/${this.token}`, {
      fullName: this.fullName.trim(),
      consent: this.consent
    }).subscribe({
      next: response => {
        this.busy = false;
        this.completed = response.message;
        this.details = null;
      },
      error: error => {
        this.busy = false;
        this.error = error.error?.message || 'ECS authorization could not be recorded.';
      }
    });
  }
}
