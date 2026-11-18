import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

type CustomerAccountType = 'SAVINGS' | 'SALARY' | 'CURRENT';

interface ActivationCodeResponse {
  activationCode: string;
  accountType: CustomerAccountType;
  expiresAt: string;
}

interface ActivationHistoryEntry {
  id: number;
  customerId: string;
  accountType: CustomerAccountType;
  issuedBy: string;
  issuedAt: string;
  expiresAt: string;
  status: 'ACTIVE' | 'USED' | 'EXPIRED' | 'REGENERATED' | 'LOCKED';
  usedAt: string | null;
  firstLoginAt: string | null;
  lastLoginAt: string | null;
}

@Component({
  selector: 'app-customer-access',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './customer-access.html',
  styleUrl: './customer-access.css'
})
export class CustomerAccess implements OnInit, OnDestroy {
  accountType: CustomerAccountType = 'SAVINGS';
  customerId = '';
  issuedCode: ActivationCodeResponse | null = null;
  history: ActivationHistoryEntry[] = [];
  isIssuing = false;
  isLoadingHistory = false;
  error = '';
  historyError = '';
  private historyPoll: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.loadHistory();
    this.historyPoll = setInterval(() => this.loadHistory(), 10000);
  }

  ngOnDestroy(): void {
    if (this.historyPoll) clearInterval(this.historyPoll);
  }

  issueCode(): void {
    this.error = '';
    this.issuedCode = null;
    const customerId = this.customerId.trim();
    if (!customerId) {
      this.error = 'Enter the customer ID from the approved account record.';
      return;
    }

    this.isIssuing = true;
    this.http.post<ActivationCodeResponse>(
      `${environment.apiBaseUrl}/api/customer-access/activation-codes`,
      { accountType: this.accountType, customerId },
      { headers: this.adminHeaders() }
    ).subscribe({
      next: response => {
        this.issuedCode = response;
        this.isIssuing = false;
        this.downloadVoucher();
        this.loadHistory();
      },
      error: error => {
        this.error = error.error?.message || 'Unable to issue an activation code for this account.';
        this.isIssuing = false;
      }
    });
  }

  clearIssuedCode(): void {
    this.issuedCode = null;
  }

  loadHistory(): void {
    if (this.isLoadingHistory) return;
    this.isLoadingHistory = true;
    this.http.get<ActivationHistoryEntry[]>(
      `${environment.apiBaseUrl}/api/customer-access/activation-codes/history`,
      { headers: this.adminHeaders() }
    ).subscribe({
      next: history => {
        this.history = history;
        this.historyError = '';
        this.isLoadingHistory = false;
      },
      error: error => {
        this.historyError = error.error?.message || 'Unable to load activation-code history.';
        this.isLoadingHistory = false;
      }
    });
  }

  async downloadVoucher(): Promise<void> {
    const voucher = this.issuedCode;
    if (!voucher) return;

    try {
      const { jsPDF } = await import('jspdf');
      const pdf = new jsPDF();
      pdf.setFillColor(15, 118, 110);
      pdf.rect(0, 0, 210, 38, 'F');
      pdf.setTextColor(255, 255, 255);
      pdf.setFontSize(19);
      pdf.text('NEOBANK CUSTOMER ACCESS', 16, 20);
      pdf.setFontSize(10);
      pdf.text('One-time online banking activation', 16, 29);

      pdf.setTextColor(20, 34, 56);
      pdf.setFontSize(12);
      pdf.text(`Customer ID: ${this.customerId.trim()}`, 16, 56);
      pdf.text(`Account type: ${voucher.accountType}`, 16, 67);
      pdf.text('Activation code:', 16, 82);
      pdf.setFontSize(22);
      pdf.setFont('helvetica', 'bold');
      pdf.text(voucher.activationCode, 16, 94);
      pdf.setFont('helvetica', 'normal');
      pdf.setFontSize(11);
      pdf.text(`Valid until: ${new Date(voucher.expiresAt).toLocaleString()}`, 16, 109);
      pdf.setDrawColor(220, 38, 38);
      pdf.roundedRect(14, 122, 182, 38, 3, 3);
      pdf.setTextColor(153, 27, 27);
      pdf.setFontSize(10);
      pdf.text('This code is single-use and expires in 7 days.', 20, 137);
      pdf.text('Give this voucher only to the verified account holder.', 20, 147);
      pdf.setTextColor(100, 116, 139);
      pdf.setFontSize(9);
      pdf.text('After activation, sign in with the Customer ID and password created during registration.', 16, 176);
      const safeCustomerId = this.customerId.trim().replace(/[^a-z0-9_-]/gi, '');
      pdf.save(`NeoBank-Activation-${safeCustomerId}.pdf`);
    } catch (error) {
      console.error('Unable to generate customer activation voucher:', error);
      this.error = 'The code was issued, but the PDF could not be generated. Use Download PDF voucher to retry.';
    }
  }

  private adminHeaders(): HttpHeaders {
    const token = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('adminSessionToken') || '' : '';
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }
}
