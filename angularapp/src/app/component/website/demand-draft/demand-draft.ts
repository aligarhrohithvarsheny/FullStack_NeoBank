import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DemandDraftService } from '../../../service/demand-draft.service';

@Component({ selector: 'app-demand-draft', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './demand-draft.html', styleUrls: ['./demand-draft.css'] })
export class DemandDraftComponent implements OnInit {
  accountNumber = ''; userName = ''; accountDetails: any = null; verified = false; verifying = false; positivePayVerified = false; verifyingPositivePay = false; positivePayReference = ''; submitting = false; message = ''; error = ''; drafts: any[] = [];
  private chequeVerificationSequence = 0;
  private positivePayVerificationSequence = 0;
  form: any = { chequeNumber: '', payeeName: '', payeeAccountNumber: '', amount: 0, draftDate: '', reason: '' };
  constructor(private service: DemandDraftService, private router: Router) {}
  ngOnInit() { const raw = sessionStorage.getItem('currentUser'); if (raw) { const user = JSON.parse(raw); this.accountNumber = user.accountNumber || ''; this.userName = user.name || user.username || ''; } this.load(); }
  load() { if (this.accountNumber) this.service.getByAccount(this.accountNumber).subscribe(x => this.drafts = x || []); }
  resetPositivePayVerification() { this.positivePayVerificationSequence++; this.positivePayVerified = false; this.positivePayReference = ''; this.verifyingPositivePay = false; }
  onChequeNumberChange() { this.chequeVerificationSequence++; this.verified = false; this.verifying = false; this.accountDetails = null; this.resetPositivePayVerification(); }
  verifyCheque() {
    this.error = '';
    this.verified = false;
    this.accountDetails = null;
    this.resetPositivePayVerification();
    const chequeNumber = this.form.chequeNumber.trim();
    const sequence = ++this.chequeVerificationSequence;
    this.verifying = true;
    this.service.verifyCheque(this.accountNumber, chequeNumber).subscribe({
      next: x => {
        if (sequence !== this.chequeVerificationSequence || chequeNumber !== this.form.chequeNumber.trim()) return;
        this.accountDetails = x; this.verified = true; this.verifying = false;
      },
      error: e => {
        if (sequence !== this.chequeVerificationSequence || chequeNumber !== this.form.chequeNumber.trim()) return;
        this.error = e.error?.message || 'Cheque could not be verified for this account'; this.verifying = false;
      }
    });
  }
  verifyPositivePay() {
    this.error = '';
    if (!this.verified || !this.form.amount || !this.form.payeeName) { this.error = 'Verify the cheque and enter its amount and payee before checking Positive Pay'; return; }
    const chequeNumber = this.form.chequeNumber.trim();
    const amount = Number(this.form.amount);
    const payeeName = this.form.payeeName.trim();
    const sequence = ++this.positivePayVerificationSequence;
    this.verifyingPositivePay = true;
    this.service.verifyPositivePay(this.accountNumber, chequeNumber, amount, payeeName).subscribe({
      next: result => {
        if (sequence !== this.positivePayVerificationSequence || chequeNumber !== this.form.chequeNumber.trim() ||
            amount !== Number(this.form.amount) || payeeName !== this.form.payeeName.trim()) return;
        this.positivePayVerified = result.verified === true; this.positivePayReference = result.referenceNumber || ''; this.verifyingPositivePay = false;
      },
      error: e => {
        if (sequence !== this.positivePayVerificationSequence) return;
        this.resetPositivePayVerification(); this.error = e.error?.message || 'An approved matching Positive Pay registration is required';
      }
    });
  }
  submit() {
    if (!this.verified || !this.positivePayVerified || !this.form.payeeName || !this.form.payeeAccountNumber || !this.form.amount || !this.form.draftDate) {
      this.error = 'Verify the cheque and Positive Pay details, then complete all required DD details'; return;
    }
    this.submitting = true;
    this.service.create(this.accountNumber, { ...this.form, userName: this.userName }).subscribe({
      next: () => { this.message = 'Demand draft request sent for admin approval'; this.submitting = false; this.verified = false; this.resetPositivePayVerification(); this.form = { chequeNumber: '', payeeName: '', payeeAccountNumber: '', amount: 0, draftDate: '', reason: '' }; this.load(); },
      error: e => { this.error = e.error?.message || 'Unable to create demand draft'; this.submitting = false; }
    });
  }
  download(id: number) { this.service.download(id).subscribe(blob => { const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `demand-draft-${id}.pdf`; a.click(); URL.revokeObjectURL(url); }); }
  openPositivePay() { this.router.navigate(['/website/positive-pay']); }
  goBack() { this.router.navigate(['/website/userdashboard']); }
}
