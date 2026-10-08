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
}

interface Card360Transaction {
  date: string;
  description: string;
  amount: string;
  type: string;
  status: string;
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
  passcode = '';
  linkCardNumber = '';
  cards: Card360Card[] = [];
  transactions: Card360Transaction[] = [];
  error = '';
  notice = '';
  loading = false;
  editingLimitId: number | null = null;
  spendingLimit = 0;

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

  signIn(): void {
    this.error = '';
    this.loading = true;
    this.http.post<{ token: string }>(`${this.api}/login`, {
      cardNumber: this.cardNumber.replace(/\s/g, ''),
      email: this.email.trim(),
      passcode: this.passcode
    }).subscribe({
      next: response => {
        sessionStorage.setItem(this.tokenKey, response.token);
        this.cardNumber = '';
        this.email = '';
        this.passcode = '';
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
      },
      error: err => this.handleProtectedError(err)
    });
    this.http.get<Card360Transaction[]>(`${this.api}/transactions`, { headers: this.headers() }).subscribe({
      next: result => this.transactions = result || [],
      error: err => this.handleProtectedError(err)
    });
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

  signOut(): void {
    sessionStorage.removeItem(this.tokenKey);
    this.cards = [];
    this.transactions = [];
    this.router.navigate(['/website/cards360']);
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
