import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../../environment/environment';

interface Card360AuditRow {
  action: string;
  cardType?: string;
  cardId?: number;
  oldValue?: string;
  newValue?: string;
  changedBy?: string;
  createdAt: string;
}

interface Card360AdminCard {
  id: number;
  type: 'debit' | 'credit';
  cardType: string;
  maskedNumber: string;
  status: string;
  expiryDate: string;
  blocked: boolean;
}

interface Card360AdminCustomer {
  accountNumber: string;
  customerName: string;
  email: string;
  cards: Card360AdminCard[];
  enabled: boolean;
}

interface CardClosureRequest {
  id: number;
  creditCardId: number;
  accountNumber: string;
  customerName: string;
  maskedCardNumber: string;
  reason: string;
  status: string;
  requestedAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewNote?: string;
}

@Component({
  selector: 'app-cards360-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cards360-admin.html',
  styleUrl: './cards360-admin.css'
})
export class Cards360Admin {
  accountNumber = '';
  adminEmail = '';
  adminPassword = '';
  passcode = '';
  customer: Card360AdminCustomer | null = null;
  enabled: boolean | null = null;
  history: Card360AuditRow[] = [];
  closureRequests: CardClosureRequest[] = [];
  closureReviewNotes: Record<number, string> = {};
  error = '';
  notice = '';
  loading = false;

  private readonly api = `${environment.apiBaseUrl}/api/card360/admin/accounts`;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const admin = JSON.parse(sessionStorage.getItem('admin') || '{}');
        this.adminEmail = admin.email || '';
      } catch {
        this.adminEmail = '';
      }
    }
  }

  generatePasscode(): void {
    if (!this.validateLoadedCustomer()) return;
    this.loading = true;
    this.error = '';
    this.notice = '';
    this.passcode = '';
    this.http.post<{ passcode: string }>(`${this.api}/${encodeURIComponent(this.accountNumber)}/passcode`,
      this.credentials()).subscribe({
        next: result => {
          this.loading = false;
          this.passcode = result.passcode;
          this.enabled = true;
          this.notice = 'One-time provisioning passcode generated and access enabled. Future customer sign-ins use their registered email and card number.';
          this.loadHistory();
        },
        error: err => this.fail(err)
      });
  }

  accountChanged(): void {
    this.passcode = '';
    this.history = [];
    this.customer = null;
    this.closureRequests = [];
    this.enabled = null;
    this.notice = '';
  }

  loadCustomerDetails(): void {
    if (!this.validate()) return;
    this.loading = true;
    this.error = '';
    this.notice = '';
    this.http.post<Card360AdminCustomer>(`${this.api}/${encodeURIComponent(this.accountNumber.trim())}/lookup`,
      this.credentials()).subscribe({
        next: result => {
          this.loading = false;
          this.customer = result;
          this.enabled = result.enabled;
          this.notice = 'Approved customer details loaded. Card numbers are masked for security.';
          this.loadClosureRequests();
        },
        error: err => this.fail(err)
      });
  }

  setEnabled(enabled: boolean): void {
    if (!this.validateLoadedCustomer()) return;
    this.loading = true;
    this.error = '';
    this.http.put<{ enabled: boolean }>(`${this.api}/${encodeURIComponent(this.accountNumber)}/enabled`,
      { ...this.credentials(), enabled: String(enabled) }).subscribe({
        next: result => {
          this.loading = false;
          this.enabled = result.enabled;
          this.passcode = '';
          this.notice = `Cards360 sign-in ${enabled ? 'enabled' : 'disabled'} for ${this.accountNumber}.`;
          this.loadHistory();
        },
        error: err => this.fail(err)
      });
  }

  loadHistory(): void {
    if (!this.validateLoadedCustomer()) return;
    this.http.post<Card360AuditRow[]>(`${this.api}/${encodeURIComponent(this.accountNumber)}/history`,
      this.credentials()).subscribe({
        next: rows => this.history = rows || [],
        error: err => this.fail(err)
      });
  }

  loadClosureRequests(): void {
    if (!this.validateLoadedCustomer()) return;
    this.http.post<CardClosureRequest[]>(`${environment.apiBaseUrl}/api/card360/admin/closure-requests`,
      { ...this.credentials(), accountNumber: this.accountNumber.trim() }).subscribe({
        next: requests => this.closureRequests = requests || [],
        error: err => this.fail(err)
      });
  }

  reviewClosureRequest(request: CardClosureRequest, decision: 'approve' | 'reject'): void {
    if (!this.validateLoadedCustomer()) return;
    const reviewNote = (this.closureReviewNotes[request.id] || '').trim();
    if (decision === 'reject' && !reviewNote) {
      this.error = 'Enter a reason before rejecting a closure request.';
      return;
    }
    this.loading = true;
    this.error = '';
    this.http.post<CardClosureRequest>(
      `${environment.apiBaseUrl}/api/card360/admin/closure-requests/${request.id}/review`,
      { ...this.credentials(), decision, reviewNote }
    ).subscribe({
      next: result => {
        this.loading = false;
        this.notice = result.status === 'Approved'
          ? 'Card closed and closure request approved.'
          : 'Closure request rejected.';
        this.loadClosureRequests();
        this.loadCustomerDetails();
        this.loadHistory();
      },
      error: err => this.fail(err)
    });
  }

  back(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  private credentials(): { adminEmail: string; adminPassword: string } {
    return { adminEmail: this.adminEmail.trim(), adminPassword: this.adminPassword };
  }

  private validate(): boolean {
    this.error = '';
    if (!this.accountNumber.trim() || !this.adminEmail.trim() || !this.adminPassword) {
      this.error = 'Enter the customer account number and confirm your admin email and password.';
      return false;
    }
    return true;
  }

  private validateLoadedCustomer(): boolean {
    if (!this.validate()) return false;
    if (!this.customer || this.customer.accountNumber !== this.accountNumber.trim()) {
      this.error = 'Fetch and verify this customer account before managing Cards360 access.';
      return false;
    }
    return true;
  }

  private fail(err: { error?: { message?: string } }): void {
    this.loading = false;
    const message = err.error?.message;
    this.error = message === 'Admin authentication failed'
      ? 'Admin authentication failed. Use the email and password for your signed-in ADMIN account; the customer email is only shown after account lookup.'
      : message || 'The Cards360 request could not be completed.';
  }
}
