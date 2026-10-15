import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FamilyBankingService, MinorApplication } from '../../../service/family-banking.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({ selector: 'app-family-banking-admin', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './family-banking-admin.html', styleUrls: ['./family-banking-admin.css'] })
export class FamilyBankingAdminComponent implements OnInit {
  applications: MinorApplication[] = []; allApplications: MinorApplication[] = []; history: any[] = []; message = ''; error = ''; loading = false; busy = false;
  activeTab: 'pending' | 'history' | 'create' = 'pending';
  showEditModal = false; editing: MinorApplication | null = null;
  editForm: any = { minorName: '', dateOfBirth: '', monthlyLimit: 0, dailyLimit: 0, form60: '', reason: '' };
  readonly today = new Date().toISOString().slice(0, 10);
  createForm = { minorName: '', dateOfBirth: '', fatherAccountNumber: '', aadharNumber: '', phone: '', address: '', form60: '', enabled: true };

  constructor(private family: FamilyBankingService, private http: HttpClient) {}
  ngOnInit(): void { this.load(); this.loadHistory(); }

  private headers() { return { 'X-Admin-Email': this.getAdminEmail() }; }

  load(): void {
    this.loading = true; this.error = '';
    this.http.get<MinorApplication[]>(`${environment.apiBaseUrl}/api/family/admin/minor-applications`, { headers: this.headers() })
      .subscribe({ next: value => { this.applications = value; this.loading = false; }, error: () => { this.error = 'Unable to load applications'; this.loading = false; } });
  }

  loadHistory(): void {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/family/admin/minor-history`, { headers: this.headers() })
      .subscribe({ next: v => { this.history = v || []; }, error: () => { /* history optional */ } });
    this.http.get<MinorApplication[]>(`${environment.apiBaseUrl}/api/family/admin/minor-applications/all`, { headers: this.headers() })
      .subscribe({ next: v => { this.allApplications = (v || []).filter(a => a.status !== 'PENDING'); }, error: () => { /* optional */ } });
  }

  setTab(tab: 'pending' | 'history' | 'create'): void { this.activeTab = tab; if (tab === 'history') this.loadHistory(); }

  createMinorAccount(): void {
    this.busy = true; this.error = ''; this.message = '';
    this.http.post<MinorApplication>(`${environment.apiBaseUrl}/api/family/admin/minor-accounts`, this.createForm, { headers: this.headers() })
      .subscribe({
        next: result => {
          this.busy = false;
          this.message = `Minor savings account ${result.assignedAccountNumber} created as ${result.accountStatus}`;
          this.createForm = { minorName: '', dateOfBirth: '', fatherAccountNumber: '', aadharNumber: '', phone: '', address: '', form60: '', enabled: true };
          this.loadHistory();
        },
        error: e => { this.busy = false; this.error = e?.error?.message || e?.error?.error || 'Unable to create minor account'; }
      });
  }

  toggleAccount(app: MinorApplication): void {
    const enabled = !this.isAccountEnabled(app);
    this.busy = true; this.error = ''; this.message = '';
    this.http.patch<MinorApplication>(`${environment.apiBaseUrl}/api/family/admin/minor-applications/${app.id}/account-status`, { enabled }, { headers: this.headers() })
      .subscribe({
        next: updated => {
          this.busy = false;
          this.message = `Minor savings account ${updated.accountStatus?.toLowerCase()}`;
          this.loadHistory();
        },
        error: e => { this.busy = false; this.error = e?.error?.message || e?.error?.error || 'Unable to change account status'; }
      });
  }

  isAccountEnabled(app: MinorApplication): boolean { return (app.accountStatus || 'ACTIVE') === 'ACTIVE'; }

  openEdit(app: MinorApplication): void {
    this.editing = app;
    this.editForm = { minorName: app.minorName, dateOfBirth: app.dateOfBirth, monthlyLimit: app.monthlyLimit, dailyLimit: app.dailyLimit, form60: app.form60 || '', reason: '' };
    this.showEditModal = true;
  }
  closeEdit(): void { this.showEditModal = false; this.editing = null; }

  submitEdit(approve: boolean): void {
    if (!this.editing) return;
    const email = this.getAdminEmail(); this.busy = true; this.error = '';
    this.http.post(`${environment.apiBaseUrl}/api/family/admin/minor-applications/${this.editing.id}/edit-and-review`,
      { approve, reason: this.editForm.reason, minorName: this.editForm.minorName, dateOfBirth: this.editForm.dateOfBirth, monthlyLimit: this.editForm.monthlyLimit, dailyLimit: this.editForm.dailyLimit, form60: this.editForm.form60 },
      { headers: { 'X-Admin-Email': email } })
      .subscribe({
        next: () => { this.busy = false; this.message = approve ? 'Application updated & approved (account + customer ID auto-generated)' : 'Application updated & declined'; this.closeEdit(); this.load(); this.loadHistory(); },
        error: e => { this.busy = false; this.error = e?.error?.message || e?.error?.error || 'Review failed'; }
      });
  }

  review(app: MinorApplication, approve: boolean): void {
    const email = this.getAdminEmail(); this.busy = true; this.error = '';
    this.http.post(`${environment.apiBaseUrl}/api/family/admin/minor-applications/${app.id}/review`, { userId: 0, approve }, { headers: { 'X-Admin-Email': email } })
      .subscribe({
        next: () => { this.busy = false; this.message = approve ? 'Application approved (account + customer ID auto-generated)' : 'Application declined'; this.load(); this.loadHistory(); },
        error: e => { this.busy = false; this.error = e?.error?.message || 'Review failed'; }
      });
  }

  private getAdminEmail(): string {
    const direct = sessionStorage.getItem('adminEmail');
    if (direct) return direct;
    try { const admin = JSON.parse(sessionStorage.getItem('admin') || '{}'); return admin.email || admin.username || ''; } catch { return ''; }
  }
}
