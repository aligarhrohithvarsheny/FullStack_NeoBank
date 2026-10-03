import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-credit-card-bill-pay',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ccbp">
      <h2>Credit Card Bill Payment</h2>
      <p class="ccbp-sub">Paying from account <b>{{ accountNumber || '—' }}</b> · Available balance
        <b>{{ payerBalance === null ? '—' : (payerBalance | currency:'INR') }}</b></p>

      <div class="ccbp-card">
        <label>Last 4 digits of card</label>
        <input type="text" maxlength="4" inputmode="numeric" [(ngModel)]="last4" (input)="last4 = digits(last4)" placeholder="1234" />
        <label>Mobile number linked to card</label>
        <input type="text" maxlength="10" inputmode="numeric" [(ngModel)]="mobile" (input)="mobile = digits(mobile)" placeholder="10-digit mobile" />
        <button class="ccbp-btn" (click)="lookup()" [disabled]="loading || last4.length !== 4 || mobile.length !== 10">
          {{ loading ? 'Fetching...' : 'Fetch Card Details' }}
        </button>
      </div>

      <div *ngIf="error" class="ccbp-msg err">{{ error }}</div>

      <div *ngIf="details" class="ccbp-card">
        <div class="ccbp-row"><span>Card Holder</span><b>{{ details.cardHolderName }}</b></div>
        <div class="ccbp-row"><span>Card</span><b>{{ details.maskedCardNumber }}</b></div>
        <div class="ccbp-row"><span>Outstanding Balance</span><b>{{ details.outstandingBalance | currency:'INR' }}</b></div>
        <div class="ccbp-row"><span>Minimum Due</span><b>{{ details.minimumDue | currency:'INR' }}</b></div>
        <div class="ccbp-row" *ngIf="details.dueDate"><span>Due Date</span><b>{{ details.dueDate | date:'dd MMM yyyy' }}</b></div>
        <div class="ccbp-row"><span>Your Account Balance</span><b>{{ details.payerAvailableBalance | currency:'INR' }}</b></div>

        <ng-container *ngIf="details.outstandingBalance > 0; else nothingDue">
          <div class="ccbp-chips">
            <button type="button" [class.on]="mode === 'full'" (click)="choose('full')">Full Outstanding</button>
            <button type="button" [class.on]="mode === 'min'" (click)="choose('min')">Minimum Due</button>
            <button type="button" [class.on]="mode === 'custom'" (click)="choose('custom')">Custom Amount</button>
          </div>
          <label>Amount to pay (₹)</label>
          <input type="number" min="1" [readonly]="mode !== 'custom'" [(ngModel)]="amount" />
          <button class="ccbp-btn" (click)="pay()" [disabled]="paying || !amount || amount <= 0">
            {{ paying ? 'Processing...' : 'Pay ' + (amount ? (amount | currency:'INR') : '') }}
          </button>
        </ng-container>
        <ng-template #nothingDue><div class="ccbp-msg ok">No outstanding balance on this card.</div></ng-template>
      </div>

      <div *ngIf="success" class="ccbp-msg ok">{{ success }}</div>
    </div>
  `,
  styles: [`
    .ccbp { max-width: 560px; margin: 0 auto; padding: 8px; }
    .ccbp h2 { margin: 0 0 4px; }
    .ccbp-sub { color: #64748b; margin: 0 0 16px; }
    .ccbp-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; margin-bottom: 16px; display: flex; flex-direction: column; gap: 8px; }
    .ccbp label { font-size: 13px; font-weight: 600; color: #334155; }
    .ccbp input { padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 15px; }
    .ccbp-btn { margin-top: 8px; padding: 12px; border: 0; border-radius: 8px; background: #1d4ed8; color: #fff; font-weight: 600; cursor: pointer; }
    .ccbp-btn:disabled { opacity: .55; cursor: not-allowed; }
    .ccbp-row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px dashed #e2e8f0; }
    .ccbp-chips { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
    .ccbp-chips button { padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 20px; background: #f8fafc; cursor: pointer; }
    .ccbp-chips button.on { background: #1d4ed8; color: #fff; border-color: #1d4ed8; }
    .ccbp-msg { padding: 10px 12px; border-radius: 8px; margin-bottom: 12px; }
    .ccbp-msg.err { background: #fee2e2; color: #991b1b; }
    .ccbp-msg.ok { background: #dcfce7; color: #166534; }
  `]
})
export class CreditCardBillPay implements OnChanges {
  @Input() accountNumber = '';
  @Output() paid = new EventEmitter<{ accountBalanceAfter: number }>();

  last4 = '';
  mobile = '';
  details: any = null;
  payerBalance: number | null = null;
  mode: 'full' | 'min' | 'custom' = 'full';
  amount: number | null = null;
  loading = false;
  paying = false;
  error = '';
  success = '';

  constructor(private http: HttpClient) {}

  ngOnChanges() {
    this.loadPayerBalance();
  }

  digits(v: string): string {
    return (v || '').replace(/\D/g, '');
  }

  private loadPayerBalance() {
    if (!this.accountNumber) return;
    this.http.get<any>(`${environment.apiBaseUrl}/api/credit-cards/bill-pay/balance/${this.accountNumber}`).subscribe({
      next: r => (this.payerBalance = r?.balance ?? null),
      error: () => (this.payerBalance = null)
    });
  }

  lookup() {
    this.error = '';
    this.success = '';
    this.details = null;
    this.loading = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/credit-cards/bill-pay/lookup`, {
      last4: this.last4, mobile: this.mobile, payerAccountNumber: this.accountNumber
    }).subscribe({
      next: d => {
        this.details = d;
        this.payerBalance = d.payerAvailableBalance ?? this.payerBalance;
        this.choose('full');
        this.loading = false;
      },
      error: e => {
        this.error = e?.error?.message || 'Unable to fetch card details';
        this.loading = false;
      }
    });
  }

  choose(mode: 'full' | 'min' | 'custom') {
    this.mode = mode;
    if (!this.details) return;
    if (mode === 'full') this.amount = this.details.outstandingBalance;
    else if (mode === 'min') this.amount = this.details.minimumDue;
    else this.amount = null;
  }

  pay() {
    if (!this.amount || this.amount <= 0) return;
    this.error = '';
    this.success = '';
    this.paying = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/credit-cards/bill-pay/pay`, {
      last4: this.last4, mobile: this.mobile, payerAccountNumber: this.accountNumber, amount: this.amount
    }).subscribe({
      next: r => {
        this.paying = false;
        this.success = `Paid ₹${r.paidAmount} successfully. Remaining outstanding: ₹${r.remainingOutstanding}. Transaction ID: ${r.transactionId}`;
        this.payerBalance = r.accountBalanceAfter;
        this.details = null;
        this.last4 = '';
        this.mobile = '';
        this.paid.emit({ accountBalanceAfter: r.accountBalanceAfter });
      },
      error: e => {
        this.paying = false;
        this.error = e?.error?.message || 'Payment failed';
      }
    });
  }
}
