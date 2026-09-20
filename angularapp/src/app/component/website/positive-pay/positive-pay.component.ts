import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PositivePayService } from '../../../service/positive-pay.service';
import { ActivatedRoute } from '@angular/router';

@Component({ selector: 'app-positive-pay', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './positive-pay.component.html', styleUrls: ['./positive-pay.component.css'] })
export class PositivePayComponent implements OnInit {
  accountNumber = '';
  accountType = 'Savings';
  cheques: any[] = [];
  requests: any[] = [];
  selected: any = null;
  activeTab: 'available'|'history' = 'available';
  loading = false;
  saving = false;
  message = '';
  error = '';
  form: any = { chequeDate: '', amount: null, payeeName: '', payeeAccountNumber: '', payeeBankName: '', payeeIfsc: '', remarks: '', confirmation: false };

  constructor(private service: PositivePayService, private route: ActivatedRoute) {}
  ngOnInit() { this.loadAccount(); }
  loadAccount() {
    const routeAccount = this.route.snapshot.paramMap.get('accountNumber');
    const raw = sessionStorage.getItem('user') || sessionStorage.getItem('userProfile') || sessionStorage.getItem('currentUser') || sessionStorage.getItem('salaryEmployee') || sessionStorage.getItem('currentAccount');
    try {
      const u = raw ? JSON.parse(raw) : {};
      this.accountNumber = u.accountNumber || u.account?.accountNumber || '';
      this.accountType = u.accountType || u.account?.accountType || (sessionStorage.getItem('salaryEmployee') ? 'Salary' : sessionStorage.getItem('currentAccount') ? 'Current' : 'Savings');
    } catch {}
    if (routeAccount) this.accountNumber = routeAccount;
    const draftRaw = sessionStorage.getItem('positivePayDraft');
    if (draftRaw) {
      try { const draft = JSON.parse(draftRaw); if (draft.accountNumber === this.accountNumber) { this.form.amount = draft.amount || null; this.form.payeeName = draft.payeeName || ''; } } catch {}
      sessionStorage.removeItem('positivePayDraft');
    }
    if (this.accountNumber) { this.refresh(); }
  }
  refresh() { this.loading = true; this.service.eligible(this.accountNumber).subscribe({ next: r => { this.cheques = r || []; this.loading = false; }, error: e => { this.error = e.error?.message || 'Unable to load eligible cheques'; this.loading = false; } }); this.service.history(this.accountNumber).subscribe({ next: r => this.requests = r || [] }); }
  open(c: any) { this.error = ''; this.message = ''; this.selected = c; this.form = { chequeDate: '', amount: c.amount || null, payeeName: '', payeeAccountNumber: '', payeeBankName: '', payeeIfsc: '', remarks: '', confirmation: false }; }
  submit() { this.error = ''; if (!this.selected?.eligible) { this.error = 'Positive Pay registration is not required for this cheque amount.'; return; } if (!this.form.confirmation) { this.error = 'Please confirm the cheque details.'; return; } this.saving = true; this.service.create({ accountNumber: this.accountNumber, chequeNumber: this.selected.chequeNumber, ...this.form }).subscribe({ next: r => { this.message = `Positive Pay Registered Successfully. Reference: ${r.referenceNumber}`; this.saving = false; this.selected = null; this.refresh(); this.activeTab = 'history'; }, error: e => { this.error = e.error?.message || 'Unable to submit Positive Pay'; this.saving = false; } }); }
  cancel(r: any) { if (!confirm(`Cancel ${r.referenceNumber}?`)) return; this.service.cancel(r.referenceNumber).subscribe({ next: () => this.refresh(), error: e => this.error = e.error?.message || 'Unable to cancel request' }); }
}
