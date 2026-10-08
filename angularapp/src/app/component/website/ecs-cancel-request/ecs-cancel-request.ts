import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-ecs-cancel-request',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="page">
  <div class="box">
    <button class="link" (click)="home()">← Back to home</button>
    <h2>ECS Mandate – Cancel Request</h2>
    <p class="sub">Enter your loan details to find your auto-debit mandate, then raise a cancellation request for admin approval.</p>
    <label>Loan Account Number<input [(ngModel)]="loanAccountNumber" [disabled]="!!mandate" /></label>
    <label>Savings Account Number<input [(ngModel)]="savingsAccountNumber" [disabled]="!!mandate" /></label>
    <label>Date of Birth<input type="date" [(ngModel)]="dob" [disabled]="!!mandate" /></label>
    <button class="btn" *ngIf="!mandate" (click)="fetch()" [disabled]="busy">Fetch Details</button>

    <div *ngIf="mandate" class="details">
      <div><span>Mandate</span><b>{{ mandate.mandateId }}</b></div>
      <div><span>Customer</span><b>{{ mandate.customerName }}</b></div>
      <div><span>Loan</span><b>{{ mandate.loanType }} · {{ mandate.loanAccountNumber }}</b></div>
      <div><span>Debit account</span><b>{{ mandate.savingsAccountNumber }}</b></div>
      <div><span>EMI / Day</span><b>₹{{ mandate.emiAmount }} · day {{ mandate.debitDay }}</b></div>
      <div><span>Status</span><b>{{ mandate.status }}</b></div>
      <div *ngIf="mandate.cancelRejectionNote" class="note">Previous request rejected: {{ mandate.cancelRejectionNote }}</div>
      <div *ngIf="mandate.cancelRequested" class="ok">A cancel request is pending admin approval.</div>
      <ng-container *ngIf="!mandate.cancelRequested">
        <label>Reason<textarea [(ngModel)]="reason" rows="3"></textarea></label>
        <button class="btn danger" (click)="submit()" [disabled]="busy">Raise Cancel Request</button>
      </ng-container>
      <button class="link" (click)="reset()">Start over</button>
    </div>
    <div *ngIf="message" [class]="ok ? 'ok' : 'err'">{{ message }}</div>
  </div>
</div>`,
  styles: [`
.page{min-height:100vh;background:#f1f5f9;display:flex;justify-content:center;align-items:flex-start;padding:40px 16px;font-family:Segoe UI,Arial,sans-serif}
.box{background:#fff;border-radius:14px;padding:26px;width:480px;max-width:100%;box-shadow:0 4px 18px #0002;display:flex;flex-direction:column;gap:12px}
.sub{color:#64748b;font-size:14px;margin:0}
label{display:flex;flex-direction:column;font-size:13px;gap:4px}
input,textarea{padding:9px;border:1px solid #cbd5e1;border-radius:8px}
.btn{padding:10px;border:0;border-radius:8px;background:#2563eb;color:#fff;cursor:pointer}.danger{background:#dc2626}
.link{background:none;border:0;color:#2563eb;cursor:pointer;text-align:left;padding:0}
.details{display:flex;flex-direction:column;gap:8px}.details div:not(.ok):not(.note){display:flex;justify-content:space-between;font-size:14px;border-bottom:1px dashed #e5e7eb;padding:3px 0}
.ok{color:#16a34a;font-size:14px}.err{color:#dc2626;font-size:14px}.note{color:#d97706;font-size:13px}
`]
})
export class EcsCancelRequest {
  loanAccountNumber = '';
  savingsAccountNumber = '';
  dob = '';
  reason = '';
  mandate: any = null;
  message = '';
  ok = false;
  busy = false;
  private api = `${environment.apiBaseUrl}/api/ecs-mandates/public`;

  constructor(private http: HttpClient, private router: Router) {}

  private creds() {
    return { loanAccountNumber: this.loanAccountNumber, savingsAccountNumber: this.savingsAccountNumber, dob: this.dob };
  }

  fetch() {
    this.busy = true; this.message = '';
    this.http.post<any>(`${this.api}/lookup`, this.creds()).subscribe({
      next: r => { this.busy = false; this.mandate = r; },
      error: e => { this.busy = false; this.ok = false; this.message = e?.error?.message || 'Could not fetch details'; }
    });
  }

  submit() {
    this.busy = true; this.message = '';
    this.http.post<any>(`${this.api}/cancel-request`, { ...this.creds(), reason: this.reason }).subscribe({
      next: r => { this.busy = false; this.ok = true; this.message = r.message || 'Cancel request sent'; this.mandate = { ...this.mandate, cancelRequested: true }; },
      error: e => { this.busy = false; this.ok = false; this.message = e?.error?.message || 'Request failed'; }
    });
  }

  reset() { this.mandate = null; this.message = ''; this.reason = ''; }
  home() { this.router.navigate(['/website/landing']); }
}
