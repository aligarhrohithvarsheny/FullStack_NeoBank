import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-personal-kyc',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './personal-kyc.html',
  styleUrls: ['./personal-kyc.css']
})
export class PersonalKyc {
  customerId = '';
  accountNumber = '';
  dob = '';
  panNumber = '';
  panDocument: File | null = null;
  aadharDocument: File | null = null;
  accountSummary: any = null;
  loading = false;
  submitting = false;
  error = '';
  success = '';

  constructor(private http: HttpClient, private router: Router) {}

  lookup() {
    this.error = '';
    this.success = '';
    this.accountSummary = null;
    if (!this.customerId || !this.accountNumber || !this.dob) {
      this.error = 'Enter Customer ID, account number, and date of birth.';
      return;
    }
    this.loading = true;
    this.http.get(`${environment.apiBaseUrl}/api/kyc/personal/lookup`, {
      params: { customerId: this.customerId.trim(), accountNumber: this.accountNumber.trim(), dob: this.dob }
    }).subscribe({
      next: summary => { this.accountSummary = summary; this.loading = false; },
      error: err => { this.error = err.error?.message || 'We could not verify those details.'; this.loading = false; }
    });
  }

  selectFile(event: Event, kind: 'pan' | 'aadhar') {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      this.error = 'Each document must be 5MB or smaller.';
      input.value = '';
      return;
    }
    if (kind === 'pan' && file.type !== 'application/pdf') {
      this.error = 'PAN document must be a PDF.';
      input.value = '';
      return;
    }
    if (kind === 'aadhar' && !['application/pdf', 'image/jpeg', 'image/png'].includes(file.type)) {
      this.error = 'Aadhaar document must be PDF, JPG, or PNG.';
      input.value = '';
      return;
    }
    this.error = '';
    if (kind === 'pan') this.panDocument = file;
    else this.aadharDocument = file;
  }

  submit() {
    this.error = '';
    this.success = '';
    if (!this.accountSummary) { this.error = 'Verify your account details first.'; return; }
    if (!this.panNumber.trim()) { this.error = 'Enter your PAN number.'; return; }
    if (!this.panDocument) { this.error = 'Upload the PAN PDF.'; return; }
    if (!this.aadharDocument) { this.error = 'Aadhaar document is mandatory.'; return; }

    const form = new FormData();
    form.append('customerId', this.customerId.trim());
    form.append('accountNumber', this.accountNumber.trim());
    form.append('dob', this.dob);
    form.append('panNumber', this.panNumber.trim().toUpperCase());
    form.append('panDocument', this.panDocument);
    form.append('aadharDocument', this.aadharDocument);
    this.submitting = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/kyc/personal/submit`, form).subscribe({
      next: response => { this.success = response.message; this.submitting = false; },
      error: err => { this.error = err.error?.message || 'Unable to submit your KYC request.'; this.submitting = false; }
    });
  }

  goBack() { this.router.navigate(['/website/landing']); }
}
