import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-ecs-mandates',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ecs-mandates.html',
  styleUrls: ['./ecs-mandates.css']
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
  consentDownloadBusy = false;
  pendingConsentUrl = '';
  loadError = '';
  eligibilityError = '';
  lastRefresh = new Date();
  private timer: any;
  private api = `${environment.apiBaseUrl}/api/ecs-mandates`;

  constructor(private http: HttpClient, private alert: AlertService, private router: Router,
              @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.refresh();
    this.timer = setInterval(() => this.refresh(), 10000);
  }

  ngOnDestroy() { if (this.timer) clearInterval(this.timer); }

  back() { this.router.navigate(['/admin/dashboard']); }

  refresh() {
    this.http.get<any[]>(`${this.api}/eligible-loans`, this.staffOptions()).subscribe({
      next: rows => { this.eligible = rows || []; this.eligibilityError = ''; },
      error: e => { this.eligibilityError = e?.error?.message || `Eligible products unavailable (HTTP ${e?.status || 'unknown'}).`; }
    });
    this.http.get<any[]>(this.api, this.staffOptions()).subscribe({
      next: r => {
        this.mandates = r;
        this.loadError = '';
        this.lastRefresh = new Date();
        if (this.sel) this.reloadSel();
      },
      error: e => { this.loadError = e?.error?.message || `Mandates unavailable (HTTP ${e?.status || 'unknown'}).`; }
    });
  }

  count(s: string) { return this.mandates.filter(m => m.status === s).length; }
  cancelRequests() { return this.mandates.filter(m => m.cancelRequested && m.status !== 'CANCELLED').length; }

  filtered() {
    const q = this.normalizeSearch(this.search);
    return this.mandates.filter(m => {
      const st = this.statusFilter === 'ALL' || (this.statusFilter === 'CANCEL_REQUESTED'
        ? m.cancelRequested && m.status !== 'CANCELLED' : m.status === this.statusFilter);
      const values = [
        m.id, m.mandateId, m.loanAccountNumber, m.loanType, m.savingsAccountNumber,
        m.customerName, m.customerId, m.debitDay, m.emiAmount, m.amountLimit,
        m.status, m.pausedBy, m.cancelRequested, m.cancelReason, m.cancelRequestedAt,
        m.cancelRejectionNote, m.createdBy, m.cancelledBy, m.cancelledAt,
        m.lastAttemptDate, m.lastDebitAt, m.lastDebitStatus, m.lastDebitMessage,
        m.successfulDebits, m.failedDebits, m.createdAt, m.updatedAt
      ];
      const searchText = this.normalizeSearch(values.map(value => this.searchableValue(value)).join(' '));
      const hit = !q || searchText.includes(q);
      return st && hit;
    });
  }

  private normalizeSearch(value: string): string {
    return value.trim().toLowerCase().replace(/[₹,]/g, '');
  }

  private searchableValue(value: unknown): string {
    if (value === null || value === undefined) return '';
    if (typeof value === 'number') {
      return `${value} ${value.toLocaleString('en-IN')} ${value.toFixed(2)}`;
    }
    if (typeof value === 'boolean') return value ? 'true yes requested' : 'false no not requested';
    const text = String(value);
    const date = new Date(text);
    if (!Number.isNaN(date.getTime()) && /[-T:]/.test(text)) {
      return `${text} ${date.toLocaleDateString()} ${date.toLocaleDateString('en-GB')} ${date.toLocaleString()}`;
    }
    return text;
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
    this.http.get<any>(`${this.api}/fetch`, { ...this.staffOptions(), params }).subscribe({
      next: r => { this.fetched = r; this.busy = false; if (!f.savingsAccountNumber) f.savingsAccountNumber = r.savingsAccountNumber; },
      error: e => { this.fetched = null; this.busy = false; this.err(e, 'Could not fetch details'); }
    });
  }

  link() {
    this.busy = true;
    this.http.post<any>(`${this.api}/link`, { ...this.form, admin: this.admin() }, this.staffOptions()).subscribe({
      next: r => {
        this.busy = false;
        this.alert.success('ECS Mandate', r.message);
        this.pendingConsentUrl = r.consentUrl || '';
        this.fetched = null;
        this.form = { loanAccountNumber: '', savingsAccountNumber: '', dob: '', debitDay: 5, amountLimit: null };
        this.refresh();
      },
      error: e => { this.busy = false; this.err(e, 'Link failed'); }
    });
  }

  copyConsentUrl() {
    if (!this.pendingConsentUrl) return;
    navigator.clipboard.writeText(this.pendingConsentUrl).then(
      () => this.alert.success('ECS Consent', 'Customer consent link copied'),
      () => this.alert.warning('ECS Consent', `Copy this customer link: ${this.pendingConsentUrl}`)
    );
  }

  act(m: any, action: string) {
    this.http.post<any>(`${this.api}/${m.id}/${action}`, { admin: this.admin() }, this.staffOptions()).subscribe({
      next: r => { this.alert.success('ECS Mandate', r.message || 'Done'); this.refresh(); },
      error: e => { this.err(e, 'Action failed'); this.refresh(); }
    });
  }

  reject(m: any) {
    const note = prompt('Reason for rejecting the cancel request (optional)') ?? '';
    this.http.post<any>(`${this.api}/${m.id}/reject-cancel`, { admin: this.admin(), note }, this.staffOptions()).subscribe({
      next: r => { this.alert.success('ECS Mandate', r.message || 'Rejected'); this.refresh(); },
      error: e => this.err(e, 'Action failed')
    });
  }

  open(m: any) {
    this.edit = { account: m.savingsAccountNumber, dob: '', day: m.debitDay, limit: m.amountLimit };
    this.http.get<any>(`${this.api}/${m.id}`, this.staffOptions()).subscribe({ next: r => this.sel = r, error: e => this.err(e, 'Load failed') });
  }

  downloadConsentRecord() {
    if (!this.sel || this.consentDownloadBusy) return;
    const mandateId = this.sel.mandate.id;
    this.consentDownloadBusy = true;
    this.http.get(`${this.api}/${mandateId}/consent-record`, {
      ...this.staffOptions(),
      responseType: 'blob'
    }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `ecs-consent-record-${mandateId}.html`;
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.consentDownloadBusy = false;
      },
      error: e => {
        this.consentDownloadBusy = false;
        this.err(e, 'Could not download the saved customer consent record');
      }
    });
  }

  private reloadSel() {
    this.http.get<any>(`${this.api}/${this.sel.mandate.id}`, this.staffOptions()).subscribe({
      next: r => this.sel = r,
      error: e => this.err(e, 'Could not refresh mandate details')
    });
  }

  private put(path: string, body: any) {
    this.http.put<any>(`${this.api}/${this.sel.mandate.id}/${path}`, { ...body, admin: this.admin() }, this.staffOptions()).subscribe({
      next: r => { this.alert.success('ECS Mandate', r.message || 'Updated'); this.refresh(); this.reloadSel(); },
      error: e => this.err(e, 'Update failed')
    });
  }

  changeAccount() { this.put('account', { savingsAccountNumber: this.edit.account, dob: this.edit.dob }); }
  changeDate() { this.put('date', { debitDay: this.edit.day }); }
  changeLimit() { this.put('limit', { amountLimit: this.edit.limit }); }

  private staffOptions() {
    const token = sessionStorage.getItem('adminSessionToken') || '';
    return { headers: new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {}) };
  }
}
