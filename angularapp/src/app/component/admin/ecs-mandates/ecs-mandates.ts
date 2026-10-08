import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-ecs-mandates',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="ecs-wrap">
  <div class="ecs-head">
    <div>
      <button class="btn" (click)="back()">← Back</button>
      <h2>ECS Mandates (Auto-Debit EMI)</h2>
    </div>
    <div class="live">● Live · refreshed {{ lastRefresh | date:'mediumTime' }}</div>
  </div>

  <div class="stats">
    <div class="stat"><b>{{ count('ACTIVE') }}</b><span>Active</span></div>
    <div class="stat"><b>{{ count('PAUSED') }}</b><span>Paused</span></div>
    <div class="stat"><b>{{ count('CANCELLED') }}</b><span>Cancelled</span></div>
    <div class="stat warn"><b>{{ cancelRequests() }}</b><span>Cancel requests</span></div>
  </div>

  <div class="card">
    <h3>Link ECS Mandate</h3>
    <div class="grid">
      <label>Loan Account No.<input [(ngModel)]="form.loanAccountNumber" list="eligible" placeholder="LOAN..." />
        <datalist id="eligible"><option *ngFor="let l of eligible" [value]="l.loanAccountNumber">{{ l.customerName }} - {{ l.loanType }}</option></datalist>
      </label>
      <label>Savings Account No.<input [(ngModel)]="form.savingsAccountNumber" placeholder="Blank = loan's account" /></label>
      <label>Date of Birth<input type="date" [(ngModel)]="form.dob" /></label>
      <label>Debit Date (day of month)<input type="number" min="1" max="31" [(ngModel)]="form.debitDay" /></label>
      <label>Amount Limit (₹)<input type="number" [(ngModel)]="form.amountLimit" placeholder="Defaults to EMI" /></label>
    </div>
    <button class="btn" (click)="fetch()" [disabled]="busy">Fetch Details</button>
    <div *ngIf="fetched" class="fetched">
      <div><b>Customer:</b> {{ fetched.customerName }} ({{ fetched.customerId }})</div>
      <div><b>Savings A/c:</b> {{ fetched.savingsAccountNumber }} · Balance ₹{{ fetched.accountBalance }}</div>
      <div><b>Loan:</b> {{ fetched.loan?.loanType }} · EMI ₹{{ fetched.emiAmount }} · Pending {{ fetched.pendingEmis }}/{{ fetched.totalEmis }} · Next due {{ fetched.nextDueDate }}</div>
      <div [style.color]="fetched.dobMatches ? 'green' : 'crimson'">DOB {{ fetched.dobMatches ? 'verified' : 'does not match' }}</div>
      <div *ngIf="fetched.existingMandate" style="color:crimson">A mandate is already linked to this loan.</div>
      <button class="btn primary" (click)="link()" [disabled]="busy || !fetched.dobMatches || fetched.existingMandate">Link Mandate</button>
    </div>
  </div>

  <div class="card">
    <div class="filters">
      <select [(ngModel)]="statusFilter"><option>ALL</option><option>ACTIVE</option><option>PAUSED</option><option>CANCELLED</option><option>CANCEL_REQUESTED</option></select>
      <input [(ngModel)]="search" placeholder="Search loan / savings account / name" />
    </div>
    <table>
      <thead><tr><th>Mandate</th><th>Loan A/c</th><th>Savings A/c</th><th>Customer</th><th>Day</th><th>EMI</th><th>Limit</th><th>Status</th><th>Debits</th><th>Last debit</th><th>Actions</th></tr></thead>
      <tbody>
        <tr *ngFor="let m of filtered()">
          <td>{{ m.mandateId }}</td><td>{{ m.loanAccountNumber }}</td><td>{{ m.savingsAccountNumber }}</td><td>{{ m.customerName }}</td>
          <td>{{ m.debitDay }}</td><td>₹{{ m.emiAmount }}</td><td>₹{{ m.amountLimit }}</td>
          <td><span class="pill {{ m.status }}">{{ m.status }}</span><span *ngIf="m.cancelRequested && m.status!=='CANCELLED'" class="pill REQ">Cancel requested</span></td>
          <td>{{ m.successfulDebits || 0 }} ok / {{ m.failedDebits || 0 }} failed</td>
          <td>{{ m.lastDebitStatus || '-' }}<br /><small>{{ m.lastDebitMessage }}</small></td>
          <td class="acts">
            <button (click)="open(m)">Manage</button>
            <button *ngIf="m.status==='ACTIVE'" (click)="act(m,'pause')">Pause</button>
            <button *ngIf="m.status==='PAUSED'" (click)="act(m,'resume')">Resume</button>
            <button *ngIf="m.status==='ACTIVE'" (click)="act(m,'debit-now')">Debit now</button>
            <button *ngIf="m.status!=='CANCELLED'" class="danger" (click)="act(m,'cancel')">Cancel</button>
            <button *ngIf="m.cancelRequested && m.status!=='CANCELLED'" (click)="reject(m)">Reject request</button>
          </td>
        </tr>
        <tr *ngIf="!filtered().length"><td colspan="11" style="text-align:center">No mandates</td></tr>
      </tbody>
    </table>
  </div>

  <div class="modal" *ngIf="sel" (click)="sel=null">
    <div class="box" (click)="$event.stopPropagation()">
      <h3>{{ sel.mandate.mandateId }} · {{ sel.mandate.status }}</h3>
      <p>Loan {{ sel.mandate.loanAccountNumber }} → Savings {{ sel.mandate.savingsAccountNumber }} · Balance ₹{{ sel.accountBalance }}</p>
      <p *ngIf="sel.mandate.cancelRequested">Cancel requested: {{ sel.mandate.cancelReason }}</p>
      <div class="grid">
        <label>Change savings account<input [(ngModel)]="edit.account" /></label>
        <label>DOB (for new account)<input type="date" [(ngModel)]="edit.dob" /></label>
        <label>Debit day<input type="number" min="1" max="31" [(ngModel)]="edit.day" /></label>
        <label>Amount limit<input type="number" [(ngModel)]="edit.limit" /></label>
      </div>
      <div class="acts">
        <button (click)="changeAccount()">Update account</button>
        <button (click)="changeDate()">Update date</button>
        <button (click)="changeLimit()">Update limit</button>
        <button (click)="sel=null">Close</button>
      </div>
      <h4>EMI schedule</h4>
      <div class="scroll"><table>
        <thead><tr><th>#</th><th>Due</th><th>Amount</th><th>Status</th><th>Paid on</th><th>Balance after</th></tr></thead>
        <tbody><tr *ngFor="let e of sel.emis"><td>{{ e.emiNumber }}</td><td>{{ e.dueDate }}</td><td>₹{{ e.totalAmount }}</td><td>{{ e.status }}</td><td>{{ e.paymentDate | date:'short' }}</td><td>{{ e.balanceAfterPayment }}</td></tr></tbody>
      </table></div>
      <h4>Activity</h4>
      <div class="scroll"><table>
        <thead><tr><th>When</th><th>Event</th><th>Amount</th><th>Message</th><th>By</th></tr></thead>
        <tbody><tr *ngFor="let ev of sel.events"><td>{{ ev.createdAt | date:'short' }}</td><td>{{ ev.eventType }}</td><td>{{ ev.amount }}</td><td>{{ ev.message }}</td><td>{{ ev.actor }}</td></tr></tbody>
      </table></div>
    </div>
  </div>
</div>`,
  styles: [`
.ecs-wrap{padding:20px;font-family:Segoe UI,Arial,sans-serif;color:#1f2937}
.ecs-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}
.live{color:#16a34a;font-size:13px}
.stats{display:flex;gap:12px;margin-bottom:14px;flex-wrap:wrap}
.stat{background:#fff;border-radius:10px;padding:12px 20px;box-shadow:0 1px 4px #0002;display:flex;flex-direction:column}
.stat b{font-size:22px}.stat.warn b{color:#d97706}
.card{background:#fff;border-radius:10px;padding:16px;box-shadow:0 1px 4px #0002;margin-bottom:16px;overflow-x:auto}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:10px;margin-bottom:10px}
label{display:flex;flex-direction:column;font-size:13px;gap:4px}
input,select{padding:7px;border:1px solid #cbd5e1;border-radius:6px}
.btn,button{padding:6px 12px;border:1px solid #cbd5e1;border-radius:6px;background:#f8fafc;cursor:pointer;margin:2px}
.btn.primary{background:#2563eb;color:#fff;border-color:#2563eb}.danger{color:#dc2626}
.fetched{margin-top:10px;padding:10px;background:#f1f5f9;border-radius:8px;display:grid;gap:4px}
.filters{display:flex;gap:10px;margin-bottom:10px}.filters input{flex:1}
table{width:100%;border-collapse:collapse;font-size:13px}th,td{padding:7px;border-bottom:1px solid #e5e7eb;text-align:left}
.pill{padding:2px 8px;border-radius:10px;font-size:11px;color:#fff;margin-right:3px}
.ACTIVE{background:#16a34a}.PAUSED{background:#d97706}.CANCELLED{background:#6b7280}.REQ{background:#dc2626}
.modal{position:fixed;inset:0;background:#0006;display:flex;align-items:center;justify-content:center;z-index:1000}
.box{background:#fff;border-radius:12px;padding:20px;width:900px;max-width:95vw;max-height:90vh;overflow:auto}
.scroll{max-height:200px;overflow:auto}
`]
})
export class EcsMandates implements OnInit, OnDestroy {
  mandates: any[] = [];
  eligible: any[] = [];
  form: any = { loanAccountNumber: '', savingsAccountNumber: '', dob: '', debitDay: 5, amountLimit: null };
  fetched: any = null;
  statusFilter = 'ALL';
  search = '';
  sel: any = null;
  edit: any = {};
  busy = false;
  lastRefresh = new Date();
  private timer: any;
  private api = `${environment.apiBaseUrl}/api/ecs-mandates`;

  constructor(private http: HttpClient, private alert: AlertService, private router: Router,
              @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.refresh();
    this.http.get<any[]>(`${this.api}/eligible-loans`).subscribe({ next: r => this.eligible = r, error: () => {} });
    this.timer = setInterval(() => this.refresh(), 10000);
  }

  ngOnDestroy() { if (this.timer) clearInterval(this.timer); }

  back() { this.router.navigate(['/admin/dashboard']); }

  refresh() {
    this.http.get<any[]>(this.api).subscribe({
      next: r => {
        this.mandates = r;
        this.lastRefresh = new Date();
        if (this.sel) this.reloadSel();
      },
      error: () => {}
    });
  }

  count(s: string) { return this.mandates.filter(m => m.status === s).length; }
  cancelRequests() { return this.mandates.filter(m => m.cancelRequested && m.status !== 'CANCELLED').length; }

  filtered() {
    const q = this.search.trim().toLowerCase();
    return this.mandates.filter(m => {
      const st = this.statusFilter === 'ALL' || (this.statusFilter === 'CANCEL_REQUESTED'
        ? m.cancelRequested && m.status !== 'CANCELLED' : m.status === this.statusFilter);
      const hit = !q || [m.loanAccountNumber, m.savingsAccountNumber, m.customerName, m.mandateId]
        .some(v => (v || '').toLowerCase().includes(q));
      return st && hit;
    });
  }

  private err(e: any, fallback: string) {
    this.alert.error('ECS Mandate', e?.error?.message || fallback);
  }

  private admin() { return 'Admin'; }

  fetch() {
    const f = this.form;
    if (!f.loanAccountNumber) { this.alert.warning('ECS Mandate', 'Enter loan account number'); return; }
    this.busy = true;
    const params: any = { loanAccountNumber: f.loanAccountNumber };
    if (f.savingsAccountNumber) params.savingsAccountNumber = f.savingsAccountNumber;
    if (f.dob) params.dob = f.dob;
    this.http.get<any>(`${this.api}/fetch`, { params }).subscribe({
      next: r => { this.fetched = r; this.busy = false; if (!f.savingsAccountNumber) f.savingsAccountNumber = r.savingsAccountNumber; },
      error: e => { this.fetched = null; this.busy = false; this.err(e, 'Could not fetch details'); }
    });
  }

  link() {
    this.busy = true;
    this.http.post<any>(`${this.api}/link`, { ...this.form, admin: this.admin() }).subscribe({
      next: r => {
        this.busy = false;
        this.alert.success('ECS Mandate', r.message);
        this.fetched = null;
        this.form = { loanAccountNumber: '', savingsAccountNumber: '', dob: '', debitDay: 5, amountLimit: null };
        this.refresh();
      },
      error: e => { this.busy = false; this.err(e, 'Link failed'); }
    });
  }

  act(m: any, action: string) {
    this.http.post<any>(`${this.api}/${m.id}/${action}`, { admin: this.admin() }).subscribe({
      next: r => { this.alert.success('ECS Mandate', r.message || 'Done'); this.refresh(); },
      error: e => { this.err(e, 'Action failed'); this.refresh(); }
    });
  }

  reject(m: any) {
    const note = prompt('Reason for rejecting the cancel request (optional)') ?? '';
    this.http.post<any>(`${this.api}/${m.id}/reject-cancel`, { admin: this.admin(), note }).subscribe({
      next: r => { this.alert.success('ECS Mandate', r.message || 'Rejected'); this.refresh(); },
      error: e => this.err(e, 'Action failed')
    });
  }

  open(m: any) {
    this.edit = { account: m.savingsAccountNumber, dob: '', day: m.debitDay, limit: m.amountLimit };
    this.http.get<any>(`${this.api}/${m.id}`).subscribe({ next: r => this.sel = r, error: e => this.err(e, 'Load failed') });
  }

  private reloadSel() {
    this.http.get<any>(`${this.api}/${this.sel.mandate.id}`).subscribe({ next: r => this.sel = r, error: () => {} });
  }

  private put(path: string, body: any) {
    this.http.put<any>(`${this.api}/${this.sel.mandate.id}/${path}`, { ...body, admin: this.admin() }).subscribe({
      next: r => { this.alert.success('ECS Mandate', r.message || 'Updated'); this.refresh(); this.reloadSel(); },
      error: e => this.err(e, 'Update failed')
    });
  }

  changeAccount() { this.put('account', { savingsAccountNumber: this.edit.account, dob: this.edit.dob }); }
  changeDate() { this.put('date', { debitDay: this.edit.day }); }
  changeLimit() { this.put('limit', { amountLimit: this.edit.limit }); }
}
