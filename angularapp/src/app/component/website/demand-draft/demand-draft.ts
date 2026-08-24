import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DemandDraftService } from '../../../service/demand-draft.service';

@Component({ selector: 'app-demand-draft', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './demand-draft.html', styleUrls: ['./demand-draft.css'] })
export class DemandDraftComponent implements OnInit {
  accountNumber = ''; userName = ''; accountDetails: any = null; verified = false; verifying = false; submitting = false; message = ''; error = ''; drafts: any[] = [];
  form: any = { chequeNumber: '', payeeName: '', payeeAccountNumber: '', amount: 0, draftDate: '', reason: '' };
  constructor(private service: DemandDraftService, private router: Router) {}
  ngOnInit() { const raw = sessionStorage.getItem('currentUser'); if (raw) { const user = JSON.parse(raw); this.accountNumber = user.accountNumber || ''; this.userName = user.name || user.username || ''; } this.load(); }
  load() { if (this.accountNumber) this.service.getByAccount(this.accountNumber).subscribe(x => this.drafts = x || []); }
  verifyCheque() { this.error = ''; this.verified = false; this.verifying = true; this.service.verifyCheque(this.accountNumber, this.form.chequeNumber).subscribe({ next: x => { this.accountDetails = x; this.verified = true; this.verifying = false; }, error: e => { this.error = e.error?.message || 'Cheque could not be verified for this account'; this.verifying = false; } }); }
  submit() { if (!this.verified || !this.form.payeeName || !this.form.amount || !this.form.draftDate) { this.error = 'Verify your cheque and complete all required DD details'; return; } this.submitting = true; this.service.create(this.accountNumber, { ...this.form, userName: this.userName }).subscribe({ next: () => { this.message = 'Demand draft request sent for admin approval'; this.submitting = false; this.verified = false; this.form = { chequeNumber: '', payeeName: '', payeeAccountNumber: '', amount: 0, draftDate: '', reason: '' }; this.load(); }, error: e => { this.error = e.error?.message || 'Unable to create demand draft'; this.submitting = false; } }); }
  download(id: number) { this.service.download(id).subscribe(blob => { const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `demand-draft-${id}.pdf`; a.click(); URL.revokeObjectURL(url); }); }
  goBack() { this.router.navigate(['/website/userdashboard']); }
}
