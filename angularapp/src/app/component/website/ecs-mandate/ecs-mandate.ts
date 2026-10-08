import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-ecs-mandate',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="ecs">
  <h2>ECS Mandates (Auto-Debit)</h2>
  <p class="live">● Live · refreshed {{ lastRefresh | date:'mediumTime' }}</p>
  <div *ngIf="!mandates.length" class="empty">No ECS mandates are linked to your account.</div>

  <div class="card" *ngFor="let m of mandates">
    <div class="top">
      <div><b>{{ m.loanType || 'Loan' }}</b> · {{ m.loanAccountNumber }}</div>
      <span class="pill {{ m.status }}">{{ m.status }}</span>
    </div>
    <div class="row"><span>Linked savings account</span><b>{{ m.savingsAccountNumber }}</b></div>
    <div class="row"><span>{{ m.loanType === 'Credit Card' ? 'Minimum due / Limit' : 'EMI / Limit' }}</span><b>₹{{ m.emiAmount }} / ₹{{ m.amountLimit }}</b></div>
    <div class="row"><span>Debit date</span><b>Day {{ m.debitDay }} of every month</b></div>
    <div class="row"><span>EMIs debited</span><b>{{ m.successfulDebits || 0 }} successful, {{ m.failedDebits || 0 }} failed</b></div>
    <div class="row"><span>Last debit</span><b>{{ m.lastDebitStatus || '-' }} {{ m.lastDebitAt | date:'medium' }}</b></div>
    <div class="msg" *ngIf="m.lastDebitMessage">{{ m.lastDebitMessage }}</div>
    <div class="msg warn" *ngIf="m.cancelRequested && m.status!=='CANCELLED'">Cancellation requested – awaiting admin approval</div>
    <div class="msg" *ngIf="m.cancelRejectionNote">Admin note: {{ m.cancelRejectionNote }}</div>
    <div class="acts">
      <button (click)="toggle(m)">{{ expanded===m.id ? 'Hide' : 'Check EMI debits' }}</button>
      <button *ngIf="m.status==='ACTIVE'" (click)="act(m,'pause')">Pause</button>
      <button *ngIf="m.status==='PAUSED' && m.pausedBy!=='ADMIN'" (click)="act(m,'resume')">Resume</button>
      <button *ngIf="m.status!=='CANCELLED' && !m.cancelRequested" class="danger" (click)="requestCancel(m)">Request cancellation</button>
    </div>
    <div *ngIf="expanded===m.id && detail">
      <p>Current balance: <b>₹{{ detail.accountBalance }}</b></p>
      <table *ngIf="detail.emis?.length">
        <thead><tr><th>#</th><th>Due</th><th>Amount</th><th>Status</th><th>Debited on</th></tr></thead>
        <tbody><tr *ngFor="let e of detail.emis"><td>{{ e.emiNumber }}</td><td>{{ e.dueDate }}</td><td>₹{{ e.totalAmount }}</td><td>{{ e.status }}</td><td>{{ e.paymentDate | date:'short' }}</td></tr></tbody>
      </table>
      <p *ngIf="!detail.emis?.length">Payment history is available under mandate activity.</p>
      <h4>Mandate activity</h4>
      <table>
        <thead><tr><th>When</th><th>Event</th><th>Amount</th><th>Details</th></tr></thead>
        <tbody><tr *ngFor="let event of detail.events"><td>{{ event.createdAt | date:'short' }}</td><td>{{ event.eventType }}</td><td>{{ event.amount == null ? '-' : '₹' + event.amount }}</td><td>{{ event.message }}</td></tr></tbody>
      </table>
    </div>
  </div>
</div>`,
  styles: [`
.ecs{padding:16px;font-family:Segoe UI,Arial,sans-serif}
.live{color:#16a34a;font-size:13px}.empty{padding:20px;color:#6b7280}
.card{background:#fff;border-radius:12px;padding:16px;box-shadow:0 1px 5px #0002;margin-bottom:14px}
.top{display:flex;justify-content:space-between;margin-bottom:8px}
.row{display:flex;justify-content:space-between;padding:4px 0;border-bottom:1px dashed #e5e7eb;font-size:14px}
.msg{font-size:13px;margin-top:6px;color:#475569}.warn{color:#d97706}
.pill{padding:2px 10px;border-radius:10px;font-size:12px;color:#fff}
.ACTIVE{background:#16a34a}.PAUSED{background:#d97706}.CANCELLED{background:#6b7280}
.acts{margin-top:10px}button{padding:6px 12px;border:1px solid #cbd5e1;border-radius:6px;background:#f8fafc;cursor:pointer;margin:2px}
.danger{color:#dc2626}table{width:100%;border-collapse:collapse;font-size:13px}th,td{padding:6px;border-bottom:1px solid #e5e7eb;text-align:left}
`]
})
export class EcsMandate implements OnInit, OnDestroy {
  mandates: any[] = [];
  expanded: number | null = null;
  detail: any = null;
  lastRefresh = new Date();
  private timer: any;
  private account = '';
  private api = `${environment.apiBaseUrl}/api/ecs-mandates/account`;

  constructor(private http: HttpClient, private alert: AlertService, @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    try { this.account = JSON.parse(sessionStorage.getItem('currentUser') || '{}').accountNumber || ''; } catch { }
    if (!this.account) return;
    this.refresh();
    this.timer = setInterval(() => this.refresh(), 10000);
  }

  ngOnDestroy() { if (this.timer) clearInterval(this.timer); }

  refresh() {
    this.http.get<any[]>(`${this.api}/${this.account}`).subscribe({
      next: r => {
        this.mandates = r;
        this.lastRefresh = new Date();
        if (this.expanded) this.loadDetail(this.expanded);
      },
      error: () => {}
    });
  }

  toggle(m: any) {
    if (this.expanded === m.id) { this.expanded = null; this.detail = null; return; }
    this.expanded = m.id;
    this.loadDetail(m.id);
  }

  private loadDetail(id: number) {
    this.http.get<any>(`${this.api}/${this.account}/${id}`).subscribe({ next: r => this.detail = r, error: () => {} });
  }

  act(m: any, action: string) {
    this.http.post<any>(`${this.api}/${this.account}/${m.id}/${action}`, {}).subscribe({
      next: r => { this.alert.success('ECS Mandate', r.message || 'Done'); this.refresh(); },
      error: e => this.alert.error('ECS Mandate', e?.error?.message || 'Action failed')
    });
  }

  requestCancel(m: any) {
    const reason = prompt('Reason for cancelling this mandate?');
    if (reason === null) return;
    this.http.post<any>(`${this.api}/${this.account}/${m.id}/request-cancel`, { reason }).subscribe({
      next: r => { this.alert.success('ECS Mandate', r.message || 'Request sent to admin'); this.refresh(); },
      error: e => this.alert.error('ECS Mandate', e?.error?.message || 'Request failed')
    });
  }
}
