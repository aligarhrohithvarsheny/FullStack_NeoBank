import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../../../../environment/environment';

interface Card360Card {
  id: number;
  type: 'debit' | 'credit';
  cardType: string;
  maskedNumber: string;
  status: string;
  expiryDate: string;
  blocked: boolean;
  canUnblock?: boolean;
  approvedLimit?: number;
  availableLimit?: number;
  spendingLimit?: number;
  outstandingBalance?: number;
  billGenerationDate?: string;
  billDueDate?: string;
  billTotalAmount?: number;
  billPaidAmount?: number;
  billMinimumDue?: number;
  billFine?: number;
  billPenalty?: number;
  billEmiAmount?: number;
  billStatus?: string;
}

interface Card360Transaction {
  date: string;
  description: string;
  amount: string;
  type: string;
  status: string;
  receipt: string;
}

interface Cards360PaymentAccount {
  maskedAccountNumber: string;
  balance: number;
}

@Component({
  selector: 'app-cards360',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cards360.html',
  styleUrl: './cards360.css'
})
export class Cards360 implements OnInit {
  cardNumber = '';
  email = '';
  linkCardNumber = '';
  cards: Card360Card[] = [];
  selectedTransactions: Card360Transaction[] = [];
  selectedCardKey = '';
  loadingTransactions = false;
  paymentAccount: Cards360PaymentAccount | null = null;
  error = '';
  notice = '';
  loading = false;
  editingLimitId: number | null = null;
  spendingLimit = 0;
  selectedBillCardId: number | null = null;
  billPaymentAmount = 0;
  payingBill = false;

  private readonly tokenKey = 'card360Token';
  private readonly api = `${environment.apiBaseUrl}/api/card360`;

  constructor(
    private readonly http: HttpClient,
    public readonly router: Router,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (this.router.url.includes('/dashboard')) this.loadDashboard();
  }

  get selectedBillCard(): Card360Card | undefined {
    return this.cards.find(card => card.type === 'credit' && card.id === this.selectedBillCardId);
  }

  get selectedCard(): Card360Card | undefined {
    return this.cards.find(card => this.cardKey(card) === this.selectedCardKey);
  }

  billOutstanding(card: Card360Card): number {
    if (card.billTotalAmount !== undefined && card.billStatus?.toLowerCase() !== 'paid') {
      return Math.max(0, card.billTotalAmount + (card.billFine ?? 0) + (card.billPenalty ?? 0)
        - (card.billPaidAmount ?? 0));
    }
    return card.outstandingBalance ?? 0;
  }

  signIn(): void {
    this.error = '';
    this.loading = true;
    this.http.post<{ token: string }>(`${this.api}/login`, {
      cardNumber: this.cardNumber.replace(/\s/g, ''),
      email: this.email.trim()
    }).subscribe({
      next: response => {
        sessionStorage.setItem(this.tokenKey, response.token);
        this.cardNumber = '';
        this.email = '';
        this.loading = false;
        void this.router.navigate(['/website/cards360/dashboard']).then(navigated => {
          if (navigated) this.loadDashboard();
        });
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Unable to sign in. Check your details or contact the bank.';
      }
    });
  }

  loadDashboard(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (!sessionStorage.getItem(this.tokenKey)) {
      this.router.navigate(['/website/cards360']);
      return;
    }
    this.loading = true;
    this.http.get<{ cards: Card360Card[] }>(`${this.api}/cards`, { headers: this.headers() }).subscribe({
      next: result => {
        this.cards = result.cards || [];
        this.loading = false;
        if (this.selectedCardKey && this.selectedCard) this.selectCard(this.selectedCard);
        else if (this.selectedCardKey) this.showOverview();
      },
      error: err => this.handleProtectedError(err)
    });
    this.http.get<Cards360PaymentAccount>(`${this.api}/payment-account`, { headers: this.headers() }).subscribe({
      next: result => this.paymentAccount = result,
      error: err => this.handleProtectedError(err)
    });
  }

  selectCard(card: Card360Card): void {
    this.selectedCardKey = this.cardKey(card);
    this.selectedTransactions = [];
    this.loadingTransactions = true;
    this.error = '';
    this.http.get<Card360Transaction[]>(
      `${this.api}/cards/${card.type}/${card.id}/transactions`,
      { headers: this.headers() }
    ).subscribe({
      next: result => {
        this.selectedTransactions = result || [];
        this.loadingTransactions = false;
      },
      error: err => {
        this.loadingTransactions = false;
        this.handleProtectedError(err);
      }
    });
  }

  showOverview(): void {
    this.selectedCardKey = '';
    this.selectedTransactions = [];
  }

  linkCard(): void {
    this.error = '';
    this.notice = '';
    this.http.post(`${this.api}/cards/link`, { cardNumber: this.linkCardNumber.replace(/\s/g, '') },
      { headers: this.headers() }).subscribe({
        next: () => {
          this.notice = 'Card linked successfully.';
          this.linkCardNumber = '';
          this.loadDashboard();
        },
        error: err => this.error = err.error?.message || 'Unable to link this card to your account.'
      });
  }

  toggleCard(card: Card360Card): void {
    if (card.blocked && !card.canUnblock) {
      this.error = 'Contact NeoBank to unblock a blocked card.';
      return;
    }
    const blocked = !card.blocked;
    this.http.put<{ status: string }>(`${this.api}/cards/${card.type}/${card.id}/status`,
      { blocked }, { headers: this.headers() }).subscribe({
        next: result => {
          card.blocked = blocked;
          card.canUnblock = blocked;
          card.status = result.status;
          this.notice = `Card ending ${card.maskedNumber.slice(-4)} ${blocked ? 'blocked' : 'unblocked'}.`;
        },
        error: err => this.error = err.error?.message || 'Unable to update card status.'
      });
  }

  beginLimitEdit(card: Card360Card): void {
    this.editingLimitId = card.id;
    this.spendingLimit = card.spendingLimit ?? card.approvedLimit ?? 0;
  }

  saveLimit(card: Card360Card): void {
    this.http.put<{ spendingLimit: number }>(`${this.api}/cards/credit/${card.id}/limit`,
      { spendingLimit: this.spendingLimit }, { headers: this.headers() }).subscribe({
        next: result => {
          card.spendingLimit = result.spendingLimit;
          this.editingLimitId = null;
          this.notice = 'Card spending limit updated.';
        },
        error: err => this.error = err.error?.message || 'Unable to update the spending limit.'
      });
  }

  beginBillPayment(card: Card360Card): void {
    this.error = '';
    this.selectedBillCardId = card.id;
    this.billPaymentAmount = this.billOutstanding(card);
  }

  payCreditCardBill(): void {
    this.error = '';
    this.notice = '';
    if (this.selectedBillCardId === null || !Number.isFinite(this.billPaymentAmount)
        || this.billPaymentAmount <= 0) {
      this.error = 'Enter a valid bill payment amount.';
      return;
    }
    this.payingBill = true;
    this.http.post<{ paidAmount: number; remainingOutstanding: number; accountBalanceAfter: number }>(
      `${this.api}/credit-card-bill`,
      { creditCardId: this.selectedBillCardId, amount: this.billPaymentAmount },
      { headers: this.headers() }
    ).subscribe({
      next: result => {
        this.payingBill = false;
        this.notice = `Payment of ₹${result.paidAmount.toFixed(2)} completed. Remaining card balance: ₹${result.remainingOutstanding.toFixed(2)}.`;
        if (this.paymentAccount) this.paymentAccount.balance = result.accountBalanceAfter;
        this.loadDashboard();
      },
      error: err => {
        this.payingBill = false;
        this.error = err.error?.message || 'Unable to pay this credit-card bill.';
      }
    });
  }

  signOut(): void {
    sessionStorage.removeItem(this.tokenKey);
    this.cards = [];
    this.selectedTransactions = [];
    this.selectedCardKey = '';
    this.router.navigate(['/website/cards360']);
  }

  private cardKey(card: Card360Card): string {
    return `${card.type}-${card.id}`;
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${sessionStorage.getItem(this.tokenKey) || ''}` });
  }

  private handleProtectedError(err: { status?: number; error?: { message?: string } }): void {
    this.loading = false;
    if (err.status === 401 || err.status === 403) {
      sessionStorage.removeItem(this.tokenKey);
      this.router.navigate(['/website/cards360']);
      return;
    }
    this.error = err.error?.message || 'Unable to load Card360. Please try again.';
  }
}
