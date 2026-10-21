import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
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
  pinSet?: boolean;
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
  fullCardNumber?: string;
  closureRequestStatus?: string;
  cardholderName?: string;
  onlineTransactionsEnabled?: boolean;
  posTransactionsEnabled?: boolean;
  contactlessEnabled?: boolean;
  atmWithdrawalsEnabled?: boolean;
  internationalTransactionsEnabled?: boolean;
}

interface Card360Transaction {
  id: number;
  date: string;
  description: string;
  merchant: string;
  amount: number;
  type: string;
  status: string;
  receipt: string;
  billId?: number | null;
  emiPlanId?: number | null;
  balanceAfter?: number | null;
  paymentMethod?: string | null;
}

interface Cards360PaymentAccount {
  maskedAccountNumber: string;
  balance: number;
}

interface Card360Bill {
  id: number;
  billGenerationDate: string;
  dueDate: string;
  totalAmount: number;
  regularAmount: number;
  emiAmount: number;
  minimumDue: number;
  paidAmount: number;
  fine: number;
  penalty: number;
  status: string;
  billingPeriod: string;
}

interface Card360SecurityEvent {
  action: string;
  createdAt: string;
  newValue: string;
}

interface Card360RequestTimelineEvent {
  status: string;
  createdAt: string;
  note: string;
  updatedBy: string;
}

interface Card360ServiceRequest {
  id: number;
  creditCardId: number;
  requestNumber: string;
  requestType: string;
  category: string;
  transactionId: number | null;
  description: string;
  requestedLimit: number;
  status: string;
  resolution: string;
  createdAt: string;
  updatedAt: string;
  timeline: Card360RequestTimelineEvent[];
}

interface Card360SearchResult {
  title: string;
  detail: string;
  kind: string;
  cardKey: string;
  sectionId: string;
}

type Card360TransactionTab = 'ALL' | 'UNBILLED' | 'BILLED' | 'EMI' | 'REFUNDS';

@Component({
  selector: 'app-cards360',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cards360.html',
  styleUrl: './cards360.css'
})
export class Cards360 implements OnInit, OnDestroy {
  cardNumber = '';
  email = '';
  linkCardNumber = '';
  cards: Card360Card[] = [];
  dashboardSearch = '';
  dashboardSearchResults: Card360SearchResult[] = [];
  dashboardSearchLoading = false;
  dashboardSearchError = '';
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
  lastPayment: { amount: number; reference: string; date: Date; status: string } | null = null;
  cardBills: Card360Bill[] = [];
  statementStartDate = '';
  statementEndDate = '';
  closureReason = '';
  requestingClosureFor: number | null = null;
  submittingClosureRequest = false;
  downloadingStatement = false;
  activeTransactionTab: Card360TransactionTab = 'ALL';
  readonly transactionTabs: Card360TransactionTab[] = ['ALL', 'UNBILLED', 'BILLED', 'EMI', 'REFUNDS'];
  transactionSearch = '';
  transactionStatusFilter = '';
  transactionTypeFilter = '';
  transactionCategoryFilter = '';
  transactionStartDate = '';
  transactionEndDate = '';
  transactionMinAmount: number | null = null;
  transactionMaxAmount: number | null = null;
  selectedTransaction: Card360Transaction | null = null;
  billSearch = '';
  statementPeriodFilter: 'ALL' | 'MONTHLY' | 'FINANCIAL_YEAR' = 'ALL';
  selectedStatementMonth = '';
  selectedFinancialYear = '';
  analyticsPeriod: 'THIS_MONTH' | 'PREVIOUS_MONTH' | 'CURRENT_CYCLE' | 'PREVIOUS_CYCLE' = 'THIS_MONTH';
  emiSettings: { enabled: boolean; annualInterestPercent: number; processingFeePercent: number; tenuresMonths: number[] } | null = null;
  emiEligibleTransactions: Array<{ id: number; merchant: string; description: string; amount: number; transactionDate: string }> = [];
  emiPlans: any[] = [];
  selectedEmiTransactionIds: number[] = [];
  emiTenureMonths = 3;
  convertingToEmi = false;
  loadingEmi = false;
  customerRequests: Card360ServiceRequest[] = [];
  securityEvents: Card360SecurityEvent[] = [];
  creditLimitIncreaseCeiling: number | null = null;
  requestType = 'Transaction Dispute';
  requestCategory = 'Unauthorized transaction';
  requestDescription = '';
  requestTransactionId: number | null = null;
  requestedCreditLimit: number | null = null;
  submittingRequest = false;
  showPinForm = false;
  pinForm = { newPin: '', confirmPin: '' };
  readonly customerRequestTypes = [
    'Card Replacement', 'Credit Limit Increase', 'Statement Request', 'Billing Issue',
    'Reward Issue', 'EMI Issue', 'Transaction Dispute', 'Address Update', 'Card Closure'
  ];
  readonly disputeCategories = [
    'Unauthorized transaction', 'Duplicate transaction', 'Incorrect amount',
    'Refund not received', 'Merchant dispute', 'Cash withdrawal issue'
  ];
  readonly replacementReasons = ['Lost', 'Stolen', 'Damaged', 'Expired'];
  readonly cardControlKeys = [
    'onlineTransactionsEnabled',
    'posTransactionsEnabled',
    'contactlessEnabled',
    'atmWithdrawalsEnabled',
    'internationalTransactionsEnabled'
  ] as const;

  private readonly tokenKey = 'card360Token';
  private readonly expiredSessionMessageKey = 'card360SessionExpiredMessage';
  private readonly api = `${environment.apiBaseUrl}/api/card360`;
  private requestRefreshTimer: ReturnType<typeof setInterval> | null = null;
  private sessionExpiryTimer: ReturnType<typeof setTimeout> | null = null;
  private handlingExpiredSession = false;
  private readonly searchActivityByCard = new Map<string, { transactions: Card360Transaction[]; bills: Card360Bill[] }>();

  constructor(
    private readonly http: HttpClient,
    public readonly router: Router,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (this.router.url.includes('/dashboard')) {
      const token = sessionStorage.getItem(this.tokenKey);
      if (!token || !this.startSessionMonitoring(token)) {
        if (!token) void this.router.navigate(['/website/cards360']);
        return;
      }
      this.loadDashboard();
    } else {
      const expiredMessage = sessionStorage.getItem(this.expiredSessionMessageKey);
      if (expiredMessage) {
        this.error = expiredMessage;
        sessionStorage.removeItem(this.expiredSessionMessageKey);
      }
    }
  }

  ngOnDestroy(): void {
    if (this.requestRefreshTimer) clearInterval(this.requestRefreshTimer);
    if (this.sessionExpiryTimer) clearTimeout(this.sessionExpiryTimer);
  }

  get selectedBillCard(): Card360Card | undefined {
    return this.cards.find(card => card.type === 'credit' && card.id === this.selectedBillCardId);
  }

  get selectedCard(): Card360Card | undefined {
    return this.cards.find(card => this.cardKey(card) === this.selectedCardKey);
  }

  get usedLimit(): number {
    const card = this.selectedCard;
    return card ? Math.max(0, (card.approvedLimit ?? 0) - (card.availableLimit ?? 0)) : 0;
  }

  get utilizationPercent(): number {
    const limit = this.selectedCard?.approvedLimit ?? 0;
    return limit > 0 ? Math.min(100, this.usedLimit / limit * 100) : 0;
  }

  get latestBill(): Card360Bill | undefined {
    return this.cardBills[0];
  }

  get filteredTransactions(): Card360Transaction[] {
    const search = this.transactionSearch.trim().toLowerCase();
    return this.selectedTransactions.filter(transaction => {
      const type = (transaction.type || '').toLowerCase();
      const isRefund = type.includes('refund');
      const date = (transaction.date || '').slice(0, 10);
      const amount = Number(transaction.amount || 0);
      const category = this.transactionCategory(transaction).toLowerCase();
      const searchMatch = !search || [
        transaction.merchant, transaction.description, transaction.type, transaction.status,
        transaction.receipt, String(transaction.id)
      ].some(value => (value || '').toLowerCase().includes(search));
      return searchMatch
        && (this.activeTransactionTab === 'ALL'
          || (this.activeTransactionTab === 'UNBILLED' && !transaction.billId && !transaction.emiPlanId)
          || (this.activeTransactionTab === 'BILLED' && !!transaction.billId)
          || (this.activeTransactionTab === 'EMI' && !!transaction.emiPlanId)
          || (this.activeTransactionTab === 'REFUNDS' && isRefund))
        && (!this.transactionStatusFilter || transaction.status.toLowerCase() === this.transactionStatusFilter.toLowerCase())
        && (!this.transactionTypeFilter || type === this.transactionTypeFilter.toLowerCase())
        && (!this.transactionCategoryFilter || category === this.transactionCategoryFilter.toLowerCase())
        && (!this.transactionStartDate || date >= this.transactionStartDate)
        && (!this.transactionEndDate || date <= this.transactionEndDate)
        && (this.transactionMinAmount == null || amount >= this.transactionMinAmount)
        && (this.transactionMaxAmount == null || amount <= this.transactionMaxAmount);
    });
  }

  get filteredBills(): Card360Bill[] {
    const query = this.billSearch.trim().toLowerCase();
    return this.cardBills.filter(bill => {
      const date = new Date(bill.billGenerationDate);
      const month = this.statementMonthKey(date);
      const year = this.financialYearLabel(date);
      return (!query || `${bill.billingPeriod} ${bill.status} ${month} ${year}`.toLowerCase().includes(query))
        && (this.statementPeriodFilter !== 'MONTHLY' || !this.selectedStatementMonth || month === this.selectedStatementMonth)
        && (this.statementPeriodFilter !== 'FINANCIAL_YEAR' || !this.selectedFinancialYear || year === this.selectedFinancialYear);
    });
  }

  get statementMonths(): Array<{ key: string; label: string }> {
    return [...new Map(this.cardBills.map(bill => {
      const date = new Date(bill.billGenerationDate);
      return [this.statementMonthKey(date), date.toLocaleString('en', { month: 'long', year: 'numeric' })] as const;
    })).entries()].map(([key, label]) => ({ key, label }));
  }

  get statementYears(): string[] {
    return [...new Set(this.cardBills.map(bill => this.financialYearLabel(new Date(bill.billGenerationDate))))]
      .sort((a, b) => b.localeCompare(a));
  }

  get spendingCategories(): Array<{ name: string; amount: number; percent: number }> {
    const categories = ['Shopping', 'Food', 'Fuel', 'Travel', 'Entertainment', 'Bills', 'Healthcare', 'Other'];
    const amounts = new Map(categories.map(name => [name, 0]));
    for (const transaction of this.analyticsTransactions()) {
      const type = transaction.type.toLowerCase();
      if (!['purchase', 'fee', 'interest'].includes(type) || Number(transaction.amount) <= 0) continue;
      const category = this.transactionCategory(transaction);
      amounts.set(category, (amounts.get(category) ?? 0) + Number(transaction.amount));
    }
    const peak = Math.max(0, ...amounts.values());
    return [...amounts.entries()].map(([name, amount]) => ({
      name, amount, percent: peak ? amount / peak * 100 : 0
    })).sort((a, b) => b.amount - a.amount);
  }

  get selectedPeriodSpend(): number {
    return this.analyticsTransactions().filter(transaction =>
      ['purchase', 'fee', 'interest'].includes(transaction.type.toLowerCase()))
      .reduce((sum, transaction) => sum + Number(transaction.amount || 0), 0);
  }

  get averageMonthlySpend(): number {
    const monthlyTotals = new Map<string, number>();
    this.selectedTransactions.forEach(transaction => {
      if (!['purchase', 'fee', 'interest'].includes(transaction.type.toLowerCase())) return;
      const date = new Date(transaction.date);
      const key = `${date.getFullYear()}-${date.getMonth()}`;
      monthlyTotals.set(key, (monthlyTotals.get(key) ?? 0) + Number(transaction.amount || 0));
    });
    return monthlyTotals.size
      ? [...monthlyTotals.values()].reduce((sum, amount) => sum + amount, 0) / monthlyTotals.size
      : 0;
  }

  get monthlyTrend(): Array<{ label: string; amount: number; percent: number }> {
    const now = new Date();
    const months = Array.from({ length: 6 }, (_, index) =>
      new Date(now.getFullYear(), now.getMonth() - (5 - index), 1));
    const totals = months.map(month => this.selectedTransactions
      .filter(transaction => {
        const date = new Date(transaction.date);
        return date.getFullYear() === month.getFullYear() && date.getMonth() === month.getMonth()
          && ['purchase', 'fee', 'interest'].includes(transaction.type.toLowerCase());
      })
      .reduce((sum, transaction) => sum + Number(transaction.amount || 0), 0));
    const peak = Math.max(0, ...totals);
    return months.map((month, index) => ({
      label: month.toLocaleString('en', { month: 'short' }),
      amount: totals[index],
      percent: peak ? totals[index] / peak * 100 : 0
    }));
  }

  get refundTransactions(): Card360Transaction[] {
    return this.selectedTransactions.filter(transaction => transaction.type.toLowerCase().includes('refund'));
  }

  get failedTransactions(): Card360Transaction[] {
    return this.selectedTransactions.filter(transaction => transaction.status.toLowerCase() === 'failed');
  }

  get subscriptionEstimates(): Array<{ merchant: string; amount: number; frequency: string; nextDate: Date }> {
    const groups = new Map<string, Card360Transaction[]>();
    this.selectedTransactions.filter(transaction => transaction.type.toLowerCase() === 'purchase')
      .forEach(transaction => {
        const merchant = (transaction.merchant || transaction.description || '').trim();
        if (!merchant) return;
        groups.set(merchant, [...(groups.get(merchant) ?? []), transaction]);
      });
    return [...groups.entries()].flatMap(([merchant, transactions]) => {
      if (transactions.length < 2) return [];
      const ordered = [...transactions].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
      const intervals = ordered.slice(1).map((transaction, index) =>
        (new Date(transaction.date).getTime() - new Date(ordered[index].date).getTime()) / 86400000);
      const averageInterval = intervals.reduce((sum, days) => sum + days, 0) / intervals.length;
      const frequency = averageInterval >= 25 && averageInterval <= 40 ? 'Monthly'
        : averageInterval >= 330 && averageInterval <= 400 ? 'Yearly' : '';
      if (!frequency) return [];
      const latest = ordered[ordered.length - 1];
      const nextDate = new Date(latest.date);
      nextDate.setDate(nextDate.getDate() + Math.round(averageInterval));
      return [{ merchant, amount: Number(latest.amount), frequency, nextDate }];
    });
  }

  get monthlySubscriptionTotal(): number {
    return this.subscriptionEstimates.reduce((sum, subscription) =>
      sum + subscription.amount / (subscription.frequency === 'Yearly' ? 12 : 1), 0);
  }

  get topMerchants(): Array<{ merchant: string; amount: number }> {
    const totals = new Map<string, number>();
    this.analyticsTransactions().filter(transaction => transaction.type.toLowerCase() === 'purchase')
      .forEach(transaction => {
        const merchant = (transaction.merchant || transaction.description || 'Other').trim();
        totals.set(merchant, (totals.get(merchant) ?? 0) + Number(transaction.amount || 0));
      });
    return [...totals.entries()].map(([merchant, amount]) => ({ merchant, amount }))
      .sort((left, right) => right.amount - left.amount).slice(0, 5);
  }

  get healthFactors(): Array<{ name: string; score: number }> {
    const limit = this.selectedCard?.approvedLimit ?? 0;
    const outstanding = this.currentCardOutstanding;
    const failed = this.selectedTransactions.filter(transaction => transaction.status.toLowerCase() === 'failed').length;
    const failureRate = this.selectedTransactions.length ? failed / this.selectedTransactions.length * 100 : 0;
    const emiOutstanding = this.emiPlans
      .filter(plan => !['completed', 'closed'].includes(String(plan.status).toLowerCase()))
      .reduce((sum, plan) => sum + Number(plan.outstandingPrincipal || 0), 0);
    const scoreForRatio = (amount: number): number => limit > 0
      ? Math.max(0, Math.min(100, 100 - amount / limit * 100)) : 100;
    return [
      { name: 'Credit utilization', score: Math.round(Math.max(0, 100 - this.utilizationPercent)) },
      { name: 'Payment consistency', score: Math.round(Math.max(0, 100 - failureRate * 2 - (this.latestBill && this.billPaymentStatus(this.latestBill) === 'Overdue' ? 30 : 0))) },
      { name: 'Outstanding balance', score: Math.round(scoreForRatio(outstanding)) },
      { name: 'EMI burden', score: Math.round(scoreForRatio(emiOutstanding)) },
      { name: 'Spending pattern', score: Math.round(scoreForRatio(this.averageMonthlySpend)) }
    ];
  }

  get neoBankHealthScore(): number {
    const factors = this.healthFactors;
    return factors.length
      ? Math.round(factors.reduce((sum, factor) => sum + factor.score, 0) / factors.length)
      : 0;
  }

  get selectedEmiPrincipal(): number {
    return this.emiEligibleTransactions
      .filter(transaction => this.selectedEmiTransactionIds.includes(transaction.id))
      .reduce((sum, transaction) => sum + Number(transaction.amount || 0), 0);
  }

  get selectedTransactionEmiEligible(): boolean {
    return !!this.selectedTransaction
      && this.emiEligibleTransactions.some(transaction => transaction.id === this.selectedTransaction?.id);
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
        sessionStorage.removeItem(this.expiredSessionMessageKey);
        this.handlingExpiredSession = false;
        this.cardNumber = '';
        this.email = '';
        this.loading = false;
        if (!this.startSessionMonitoring(response.token)) return;
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
    const token = sessionStorage.getItem(this.tokenKey);
    if (!token) {
      this.router.navigate(['/website/cards360']);
      return;
    }
    if (!this.sessionExpiryTimer && !this.startSessionMonitoring(token)) return;
    this.loading = true;
    this.http.get<{ cards: Card360Card[] }>(`${this.api}/cards`, { headers: this.headers() }).subscribe({
      next: result => {
        this.cards = result.cards || [];
        this.loading = false;
        this.searchActivityByCard.clear();
        this.updateDashboardSearchResults();
        this.loadSearchActivityForCards();
        this.loadCustomerRequests();
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
    this.cards.forEach(item => item.fullCardNumber = undefined);
    this.selectedTransactions = [];
    this.cardBills = [];
    this.selectedTransaction = null;
    this.emiPlans = [];
    this.emiEligibleTransactions = [];
    this.securityEvents = [];
    this.selectedEmiTransactionIds = [];
    this.updateDashboardSearchResults();
    this.loadingTransactions = true;
    this.error = '';
    this.http.get<Card360Transaction[]>(
      `${this.api}/cards/${card.type}/${card.id}/transactions`,
      { headers: this.headers() }
    ).subscribe({
      next: result => {
        this.selectedTransactions = result || [];
        const activity = this.searchActivityByCard.get(this.cardKey(card)) || { transactions: [], bills: [] };
        activity.transactions = this.selectedTransactions;
        this.searchActivityByCard.set(this.cardKey(card), activity);
        this.loadingTransactions = false;
        this.updateDashboardSearchResults();
      },
      error: err => {
        this.loadingTransactions = false;
        this.handleProtectedError(err);
      }
    });
    if (card.type === 'credit') {
      this.http.get<Card360Bill[]>(`${this.api}/cards/credit/${card.id}/bills`, { headers: this.headers() })
        .subscribe({
          next: bills => {
            this.cardBills = bills || [];
            const activity = this.searchActivityByCard.get(this.cardKey(card)) || { transactions: [], bills: [] };
            activity.bills = this.cardBills;
            this.searchActivityByCard.set(this.cardKey(card), activity);
            this.selectedCardKey = this.cardKey(card);
            this.updateDashboardSearchResults();
          },
          error: err => this.handleProtectedError(err)
        });
      this.loadEmiData(card.id);
      this.loadCustomerRequests();
      this.loadSecurityEvents(card.id);
    }
  }

  get requestsForSelectedCard(): Card360ServiceRequest[] {
    return this.customerRequests.filter(request => request.creditCardId === this.selectedCard?.id);
  }

  get creditLimitIncreaseEnabled(): boolean {
    return this.creditLimitIncreaseCeiling != null && this.creditLimitIncreaseCeiling > (this.selectedCard?.approvedLimit ?? 0);
  }

  loadCustomerRequests(): void {
    this.http.get<Card360ServiceRequest[]>(`${environment.apiBaseUrl}/api/credit-card-requests/mine`,
      { headers: this.headers() }).subscribe({
        next: requests => {
          this.customerRequests = requests || [];
          this.updateDashboardSearchResults();
        },
        error: err => this.handleProtectedError(err, 'Unable to load card service requests.')
      });
    this.http.get<{ creditLimitIncreaseEnabled: boolean; creditLimitIncreaseCeiling: number }>(
      `${environment.apiBaseUrl}/api/credit-card-requests/configuration`, { headers: this.headers() }
    ).subscribe({
      next: config => this.creditLimitIncreaseCeiling = config.creditLimitIncreaseEnabled
        ? Number(config.creditLimitIncreaseCeiling) : null,
      error: err => {
        this.creditLimitIncreaseCeiling = null;
        this.handleProtectedError(err, 'Unable to load credit-limit request eligibility.');
      }
    });
  }

  private refreshCustomerRequestHistory(): void {
    if (this.selectedCard?.type !== 'credit') return;
    this.http.get<Card360ServiceRequest[]>(`${environment.apiBaseUrl}/api/credit-card-requests/mine`,
      { headers: this.headers() }).subscribe({
        next: requests => {
          this.customerRequests = requests || [];
          this.updateDashboardSearchResults();
        },
        error: err => this.handleProtectedError(err, 'Unable to refresh card service request updates.')
      });
  }

  updateDashboardSearchResults(): void {
    const query = this.normalizeSearchText(this.dashboardSearch);
    if (!query) {
      this.dashboardSearchResults = [];
      this.dashboardSearchError = '';
      return;
    }
    const results: Card360SearchResult[] = [];
    const matches = (values: unknown[]): boolean =>
      values.some(value => this.normalizeSearchText(String(value ?? '')).includes(query));
    const add = (title: string, detail: string, kind: string, cardKey: string, sectionId: string) => {
      results.push({ title, detail, kind, cardKey, sectionId });
    };

    for (const card of this.cards) {
      if (matches([
        card.cardType, card.maskedNumber, card.status, card.expiryDate, card.cardholderName,
        card.approvedLimit, card.availableLimit, card.spendingLimit,
        card.outstandingBalance, card.billTotalAmount, card.billMinimumDue, card.billStatus,
        this.searchableAmount(card.approvedLimit), this.searchableAmount(card.availableLimit),
        this.searchableAmount(card.outstandingBalance), this.searchableAmount(card.billTotalAmount)
      ])) {
        add(`${card.cardType} · ${card.maskedNumber}`,
          `${card.status} · Available limit ₹${card.availableLimit ?? '—'} · Outstanding ₹${card.outstandingBalance ?? '—'}`,
          'Card', this.cardKey(card), 'c360-card-details');
      }
    }

    const activityByCard = this.cards.map(card => {
      const cardKey = this.cardKey(card);
      const cached = this.searchActivityByCard.get(cardKey);
      return {
        cardKey,
        transactions: cached?.transactions ?? (cardKey === this.selectedCardKey ? this.selectedTransactions : []),
        bills: cached?.bills ?? (cardKey === this.selectedCardKey ? this.cardBills : [])
      };
    });
    for (const activity of activityByCard) {
      for (const transaction of activity.transactions) {
        if (matches([
          transaction.merchant, transaction.description, transaction.type,
          transaction.status, transaction.receipt, transaction.id, transaction.billId,
          transaction.emiPlanId, transaction.amount, transaction.date,
          this.searchableAmount(transaction.amount), this.transactionCategory(transaction),
          new Date(transaction.date).toLocaleDateString()
        ])) {
          add(transaction.merchant || transaction.description || 'Card transaction',
            `${transaction.type} · ${transaction.status} · ₹${transaction.amount} · ${transaction.date}`,
            'Transaction', activity.cardKey, 'c360-transactions');
        }
      }

      for (const bill of activity.bills) {
        if (matches([
          bill.id, bill.billingPeriod, bill.status, bill.totalAmount, bill.regularAmount,
          bill.emiAmount, bill.minimumDue, bill.paidAmount, bill.fine, bill.penalty,
          bill.billGenerationDate, bill.dueDate,
          this.searchableAmount(bill.totalAmount), this.searchableAmount(bill.minimumDue),
          this.searchableAmount(this.billRemaining(bill)),
          new Date(bill.billGenerationDate).toLocaleDateString()
        ])) {
          add(bill.billingPeriod || `Statement #${bill.id}`,
            `${this.billPaymentStatus(bill)} · Bill ₹${bill.totalAmount} · Remaining ₹${this.billRemaining(bill)}`,
            'Statement / bill', activity.cardKey, 'c360-bills');
        }
      }
    }

    const activeCardKey = this.selectedCardKey;
    for (const plan of this.emiPlans) {
      if (matches([
        plan.id, plan.status, plan.tenureMonths, plan.principalAmount, plan.monthlyEmi,
        plan.outstandingPrincipal, this.searchableAmount(plan.principalAmount),
        this.searchableAmount(plan.monthlyEmi), this.searchableAmount(plan.outstandingPrincipal),
        ...(plan.installments || []).flatMap((item: any) => [
          item.installmentNumber, item.dueDate, item.status, item.totalAmount,
          this.searchableAmount(item.totalAmount)
        ])
      ])) {
        add(`EMI plan #${plan.id}`,
          `${plan.status} · ${plan.tenureMonths} months · Monthly ₹${plan.monthlyEmi}`,
          'EMI', activeCardKey, 'c360-emi');
      }
    }

    for (const eligible of this.emiEligibleTransactions) {
      if (matches([
        eligible.merchant, eligible.description, eligible.id, eligible.amount,
        this.searchableAmount(eligible.amount), eligible.transactionDate
      ])) {
        add(eligible.merchant || eligible.description || 'EMI-eligible purchase',
          `Eligible purchase · ₹${eligible.amount} · ${eligible.transactionDate}`,
          'EMI-eligible purchase', activeCardKey, 'c360-emi');
      }
    }

    for (const event of this.securityEvents) {
      if (matches([event.action, event.createdAt, event.newValue])) {
        add(event.action, `${event.createdAt} · ${event.newValue}`, 'Security event', activeCardKey, 'c360-security');
      }
    }

    for (const request of this.customerRequests) {
      if (!matches([
        request.requestNumber, request.id, request.creditCardId, request.transactionId,
        request.requestType, request.category, request.description, request.requestedLimit,
        request.status, request.resolution, request.createdAt, request.updatedAt,
        ...request.timeline.flatMap(event => [event.status, event.note, event.updatedBy, event.createdAt])
      ])) continue;
      const card = this.cards.find(item => item.type === 'credit' && item.id === request.creditCardId);
      if (card) {
        add(`${request.requestNumber} · ${request.requestType}`,
          `${request.status}${request.category ? ` · ${request.category}` : ''} · ${request.description}`,
          'Ticket', this.cardKey(card), 'c360-support');
      }
    }
    this.dashboardSearchResults = results.slice(0, 30);
  }

  onDashboardSearchChanged(): void {
    this.dashboardSearchError = '';
    this.updateDashboardSearchResults();
  }

  private loadSearchActivityForCards(): void {
    const cardsToLoad = this.cards.filter(card => !this.searchActivityByCard.has(this.cardKey(card)));
    const requestCount = cardsToLoad.reduce((total, card) => total + (card.type === 'credit' ? 2 : 1), 0);
    if (!requestCount) {
      this.dashboardSearchLoading = false;
      return;
    }
    this.dashboardSearchLoading = true;
    this.dashboardSearchError = '';
    let remaining = requestCount;
    const finishRequest = () => {
      remaining--;
      if (remaining === 0) this.dashboardSearchLoading = false;
    };

    for (const card of cardsToLoad) {
      const cardKey = this.cardKey(card);
      const activity = { transactions: [] as Card360Transaction[], bills: [] as Card360Bill[] };
      this.searchActivityByCard.set(cardKey, activity);
      this.http.get<Card360Transaction[]>(
        `${this.api}/cards/${card.type}/${card.id}/transactions`, { headers: this.headers() }
      ).subscribe({
        next: transactions => {
          activity.transactions = transactions || [];
          this.updateDashboardSearchResults();
          finishRequest();
        },
        error: err => {
          this.dashboardSearchError = err.error?.message || `Unable to load search activity for ${card.cardType}.`;
          finishRequest();
        }
      });
      if (card.type === 'credit') {
        this.http.get<Card360Bill[]>(
          `${this.api}/cards/credit/${card.id}/bills`, { headers: this.headers() }
        ).subscribe({
          next: bills => {
            activity.bills = bills || [];
            this.updateDashboardSearchResults();
            finishRequest();
          },
          error: err => {
            this.dashboardSearchError = err.error?.message || `Unable to load statements for ${card.cardType}.`;
            finishRequest();
          }
        });
      }
    }
  }

  openDashboardSearchResult(result: Card360SearchResult): void {
    const card = this.cards.find(item => this.cardKey(item) === result.cardKey);
    if (!card) return;
    this.dashboardSearch = '';
    this.dashboardSearchResults = [];
    this.selectCard(card);
    setTimeout(() => this.scrollToSection(result.sectionId));
  }

  private normalizeSearchText(value: string): string {
    return value.toLocaleLowerCase().replace(/[₹,\s]/g, '').trim();
  }

  private searchableAmount(value: number | null | undefined): string {
    return value == null || !Number.isFinite(Number(value)) ? '' : Number(value).toFixed(2);
  }

  loadSecurityEvents(cardId: number): void {
    this.http.get<Card360SecurityEvent[]>(
      `${this.api}/cards/credit/${cardId}/security-events`, { headers: this.headers() }
    ).subscribe({
      next: events => {
        this.securityEvents = events || [];
        this.updateDashboardSearchResults();
      },
      error: err => {
        this.securityEvents = [];
        this.handleProtectedError(err, 'Unable to load card security events.');
      }
    });
  }

  changeRequestType(type: string): void {
    this.requestType = type;
    this.requestCategory = type === 'Card Replacement' ? 'Lost' : 'Unauthorized transaction';
    if (type !== 'Transaction Dispute') this.requestTransactionId = null;
    if (type !== 'Credit Limit Increase') this.requestedCreditLimit = null;
  }

  submitServiceRequest(): void {
    const card = this.selectedCard;
    const description = this.requestDescription.trim();
    if (!card || card.type !== 'credit' || this.submittingRequest) return;
    if (description.length < 8) {
      this.error = 'Please describe your request using at least 8 characters.';
      return;
    }
    if (this.requestType === 'Transaction Dispute' && !this.requestTransactionId) {
      this.error = 'Select the transaction you want to dispute.';
      return;
    }
    if (this.requestType === 'Credit Limit Increase'
        && (!this.creditLimitIncreaseEnabled || this.requestedCreditLimit == null
          || !Number.isFinite(this.requestedCreditLimit)
          || this.requestedCreditLimit <= (card.approvedLimit ?? 0)
          || this.requestedCreditLimit > (this.creditLimitIncreaseCeiling ?? 0))) {
      this.error = 'Enter a requested limit above your current approved limit and within the bank-configured ceiling.';
      return;
    }
    const lostOrStolen = this.requestType === 'Card Replacement'
      && ['Lost', 'Stolen'].includes(this.requestCategory);
    if (!confirm(lostOrStolen
      ? `Report this ${this.requestCategory.toLowerCase()} card and immediately block it with all channels disabled?`
      : `Submit this ${this.requestType.toLowerCase()} request?`)) return;

    this.submittingRequest = true;
    this.http.post<Card360ServiceRequest>(`${environment.apiBaseUrl}/api/credit-card-requests`, {
      creditCardId: card.id,
      requestType: this.requestType,
      category: ['Card Replacement', 'Transaction Dispute'].includes(this.requestType) ? this.requestCategory : null,
      description,
      transactionId: this.requestType === 'Transaction Dispute' ? this.requestTransactionId : null,
      requestedLimit: this.requestType === 'Credit Limit Increase' ? this.requestedCreditLimit : null
    }, { headers: this.headers() }).subscribe({
      next: request => {
        this.submittingRequest = false;
        this.requestDescription = '';
        this.requestedCreditLimit = null;
        this.requestTransactionId = null;
        this.notice = `Request ${request.requestNumber} submitted. Status: ${request.status}.`;
        this.error = '';
        this.loadCustomerRequests();
        if (lostOrStolen) this.loadDashboard();
      },
      error: err => {
        this.submittingRequest = false;
        this.error = err.error?.message || 'Unable to submit the service request.';
      }
    });
  }

  reportTransaction(transaction: Card360Transaction): void {
    this.changeRequestType('Transaction Dispute');
    this.requestCategory = 'Unauthorized transaction';
    this.requestTransactionId = transaction.id;
    this.requestDescription = `Please investigate transaction ${transaction.receipt} (${transaction.merchant || transaction.description}) for ₹${Number(transaction.amount).toFixed(2)}. `;
    this.selectedTransaction = null;
    this.scrollToSection('c360-support');
  }

  openReplacementRequest(reason: string): void {
    this.changeRequestType('Card Replacement');
    this.requestCategory = reason;
    this.requestDescription = `${reason} card replacement request. `;
    this.scrollToSection('c360-support');
  }

  setCardPin(): void {
    const card = this.selectedCard;
    if (!card || card.type !== 'credit') return;
    if (!/^\d{4}$/.test(this.pinForm.newPin) || this.pinForm.newPin !== this.pinForm.confirmPin) {
      this.error = 'Enter and confirm the same four-digit PIN.';
      return;
    }
    if (!confirm(`${card.pinSet ? 'Change' : 'Set'} this card PIN?`)) return;
    this.http.put<{ success: boolean }>(`${this.api}/cards/credit/${card.id}/pin`,
      { pin: this.pinForm.newPin }, { headers: this.headers() }).subscribe({
        next: () => {
          card.pinSet = true;
          this.pinForm = { newPin: '', confirmPin: '' };
          this.showPinForm = false;
          this.notice = 'Card PIN updated securely.';
          this.error = '';
        },
        error: err => this.error = err.error?.message || 'Unable to update the card PIN.'
      });
  }

  selectTransactionForEmi(transaction: Card360Transaction): void {
    const eligible = this.emiEligibleTransactions.find(item => item.id === transaction.id);
    if (!eligible) {
      this.error = 'This transaction is not currently eligible for EMI conversion.';
      return;
    }
    this.selectedEmiTransactionIds = [eligible.id];
    this.selectedTransaction = null;
    this.scrollToSection('c360-emi');
  }

  private loadEmiData(cardId: number): void {
    this.loadingEmi = true;
    this.http.get<typeof this.emiSettings>(`${this.api}/emi-settings`, { headers: this.headers() }).subscribe({
      next: settings => this.emiSettings = settings,
      error: err => {
        this.emiSettings = null;
        this.handleProtectedError(err, 'Unable to load EMI configuration.');
      }
    });
    this.http.get<typeof this.emiEligibleTransactions>(
      `${this.api}/cards/credit/${cardId}/emi-eligible-transactions`, { headers: this.headers() }
    ).subscribe({
     next: eligible => {
       this.emiEligibleTransactions = eligible || [];
       this.updateDashboardSearchResults();
     },
      error: err => {
        this.emiEligibleTransactions = [];
        this.handleProtectedError(err, 'Unable to load EMI-eligible transactions.');
      }
    });
    this.http.get<any[]>(`${this.api}/cards/credit/${cardId}/emi-plans`, { headers: this.headers() }).subscribe({
      next: plans => {
        this.emiPlans = plans || [];
        this.loadingEmi = false;
        this.updateDashboardSearchResults();
      },
      error: err => {
        this.emiPlans = [];
        this.loadingEmi = false;
        this.handleProtectedError(err, 'Unable to load EMI plans.');
      }
    });
  }

  showOverview(): void {
    this.selectedCardKey = '';
    this.selectedTransactions = [];
    this.cardBills = [];
    this.selectedTransaction = null;
    this.emiEligibleTransactions = [];
    this.emiPlans = [];
    this.securityEvents = [];
    this.cards.forEach(card => card.fullCardNumber = undefined);
    this.updateDashboardSearchResults();
  }

  toggleFullCardNumber(card: Card360Card): void {
    this.error = '';
    if (card.fullCardNumber) {
      card.fullCardNumber = undefined;
      return;
    }
    this.http.get<{ fullCardNumber: string }>(`${this.api}/cards/credit/${card.id}/number`,
      { headers: this.headers() }).subscribe({
        next: result => card.fullCardNumber = result.fullCardNumber,
        error: err => this.error = err.error?.message || 'Unable to reveal this card number.'
      });
  }

  submitClosureRequest(card: Card360Card): void {
    if (!this.closureReason.trim()) {
      this.error = 'Enter a reason for your card closure request.';
      return;
    }
    this.submittingClosureRequest = true;
    this.error = '';
    this.http.post<{ status: string }>(`${this.api}/cards/credit/${card.id}/closure-requests`,
      { reason: this.closureReason.trim() }, { headers: this.headers() }).subscribe({
        next: result => {
          this.submittingClosureRequest = false;
          this.requestingClosureFor = null;
          this.closureReason = '';
          card.closureRequestStatus = result.status;
          this.notice = 'Your card closure request was sent to NeoBank for review.';
        },
        error: err => {
          this.submittingClosureRequest = false;
          this.error = err.error?.message || 'Unable to submit the card closure request.';
        }
      });
  }

  downloadCurrentBill(): void {
    const latestBill = this.cardBills[0];
    if (latestBill) this.downloadBill(latestBill);
  }

  downloadLastBill(): void {
    const previousBill = this.cardBills[1];
    if (previousBill) this.downloadBill(previousBill);
  }

  downloadCustomStatement(format: 'pdf' | 'csv' = 'pdf'): void {
    if (!this.selectedCard || this.selectedCard.type !== 'credit') return;
    if (!this.statementStartDate || !this.statementEndDate
        || this.statementEndDate < this.statementStartDate) {
      this.error = 'Choose a valid statement start and end date.';
      return;
    }
    this.downloadStatement(this.selectedCard, this.statementStartDate, this.statementEndDate,
      `card-statement-${this.statementStartDate}-to-${this.statementEndDate}`, undefined, format);
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
    if (!Number.isFinite(this.spendingLimit) || this.spendingLimit < 0
        || this.spendingLimit > (card.approvedLimit ?? 0)) {
      this.error = 'Enter a spending limit between zero and the approved credit limit.';
      return;
    }
    if (!confirm(`Set this card's spending limit to ₹${this.spendingLimit.toFixed(2)}?`)) return;
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
    this.lastPayment = null;
    this.selectedBillCardId = card.id;
    this.billPaymentAmount = this.billOutstanding(card);
  }

  payMinimumDue(card: Card360Card): void {
    this.lastPayment = null;
    this.selectedBillCardId = card.id;
    this.billPaymentAmount = Math.min(this.billOutstanding(card), card.billMinimumDue ?? 0);
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
    this.http.post<{ paidAmount: number; remainingOutstanding: number; accountBalanceAfter: number; transactionId?: number }>(
      `${this.api}/credit-card-bill`,
      { creditCardId: this.selectedBillCardId, amount: this.billPaymentAmount },
      { headers: this.headers() }
    ).subscribe({
      next: result => {
        this.payingBill = false;
        this.notice = `Payment of ₹${result.paidAmount.toFixed(2)} completed. Remaining card balance: ₹${result.remainingOutstanding.toFixed(2)}.`;
        this.lastPayment = {
          amount: result.paidAmount,
          reference: result.transactionId ? `CC-${result.transactionId}` : 'Reference unavailable',
          date: new Date(),
          status: 'Completed'
        };
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
    this.clearSessionTimers();
    sessionStorage.removeItem(this.tokenKey);
    sessionStorage.removeItem(this.expiredSessionMessageKey);
    this.handlingExpiredSession = false;
    this.cards = [];
    this.selectedTransactions = [];
    this.selectedCardKey = '';
    this.cardBills = [];
    this.customerRequests = [];
    this.dashboardSearch = '';
    this.dashboardSearchResults = [];
    this.selectedTransaction = null;
    this.cards.forEach(card => card.fullCardNumber = undefined);
    this.router.navigate(['/website/cards360']);
  }

  downloadBill(bill: Card360Bill): void {
    this.downloadBillFormat(bill, 'pdf');
  }

  downloadBillCsv(bill: Card360Bill): void {
    this.downloadBillFormat(bill, 'csv');
  }

  private downloadBillFormat(bill: Card360Bill, format: 'pdf' | 'csv'): void {
    if (!this.selectedCard) return;
    const previousBill = this.cardBills.find(candidate =>
      new Date(candidate.billGenerationDate) < new Date(bill.billGenerationDate));
    const billDate = bill.billGenerationDate.slice(0, 10);
    const previousDate = previousBill?.billGenerationDate.slice(0, 10);
    const start = previousDate
      ? new Date(new Date(`${previousDate}T00:00:00Z`).getTime() + 86400000).toISOString().slice(0, 10)
      : `${billDate.slice(0, 7)}-01`;
    const end = billDate;
    this.downloadStatement(this.selectedCard, start, end, `credit-card-bill-${bill.id}`, bill, format);
  }

  private downloadStatement(card: Card360Card, startDate: string, endDate: string,
                            fileName: string, bill?: Card360Bill, format: 'pdf' | 'csv' = 'pdf'): void {
    this.downloadingStatement = true;
    this.error = '';
    const params = new URLSearchParams({ startDate, endDate });
    this.http.get<{ maskedCardNumber: string; transactions: Array<{
      date: string; description: string; type: string; amount: number; balanceAfter: number; billId: number | null;
    }> }>(`${this.api}/cards/credit/${card.id}/statement?${params.toString()}`, {
      headers: this.headers()
    }).subscribe({
      next: async statement => {
        this.downloadingStatement = false;
        try {
          if (format === 'csv') {
            const rows = [
              ['Date', 'Description', 'Type', 'Amount', 'Balance after', 'Bill ID'],
              ...statement.transactions.map(transaction => [
                transaction.date, transaction.description, transaction.type,
                Number(transaction.amount || 0).toFixed(2),
                Number(transaction.balanceAfter || 0).toFixed(2), transaction.billId ?? ''
              ])
            ];
            const csv = rows.map(row => row
              .map(value => `"${String(value ?? '').replace(/"/g, '""')}"`).join(',')).join('\r\n');
            const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `${fileName}.csv`;
            link.click();
            URL.revokeObjectURL(url);
            return;
          }
          const { jsPDF } = await import('jspdf');
          const pdf = new jsPDF();
          pdf.setFontSize(18);
          pdf.text('NeoBank Credit Card Statement', 14, 20);
          pdf.setFontSize(10);
          pdf.text(`Card: ${statement.maskedCardNumber}`, 14, 30);
          pdf.text(`Period: ${startDate} to ${endDate}`, 14, 37);
          if (bill) {
            pdf.text(`Bill #${bill.id} · ${bill.billingPeriod} · ${bill.status}`, 14, 44);
            pdf.text(`Generated: ${new Date(bill.billGenerationDate).toLocaleString()}`, 14, 51);
            pdf.text(`Due: ${new Date(bill.dueDate).toLocaleDateString()}`, 14, 58);
            pdf.text(`Regular: Rs ${Number(bill.regularAmount || 0).toFixed(2)}  EMI: Rs ${Number(bill.emiAmount || 0).toFixed(2)}`, 14, 65);
            pdf.text(`Bill total: Rs ${Number(bill.totalAmount || 0).toFixed(2)}  Fine: Rs ${Number(bill.fine || 0).toFixed(2)}  Penalty: Rs ${Number(bill.penalty || 0).toFixed(2)}`, 14, 72);
            pdf.text(`Minimum due: Rs ${Number(bill.minimumDue || 0).toFixed(2)}  Paid: Rs ${Number(bill.paidAmount || 0).toFixed(2)}`, 14, 79);
          }
          let y = bill ? 91 : 49;
          pdf.setFontSize(9);
          pdf.text('Date', 14, y);
          pdf.text('Description', 43, y);
          pdf.text('Type', 112, y);
          pdf.text('Amount', 139, y);
          pdf.text('Balance', 171, y);
          y += 7;
          for (const transaction of statement.transactions) {
            if (y > 275) {
              pdf.addPage();
              y = 20;
            }
            pdf.text(new Date(transaction.date).toLocaleDateString(), 14, y);
            pdf.text(pdf.splitTextToSize(transaction.description || 'Card activity', 63), 43, y);
            pdf.text(transaction.type || '-', 112, y);
            pdf.text(`Rs ${Number(transaction.amount || 0).toFixed(2)}`, 139, y);
            pdf.text(`Rs ${Number(transaction.balanceAfter || 0).toFixed(2)}`, 171, y);
            y += 8;
          }
          if (!statement.transactions.length) pdf.text('No card transactions for this period.', 14, y);
          pdf.save(`${fileName}.pdf`);
        } catch {
          this.error = 'Unable to create the statement PDF. Please try again.';
        }
      },
      error: err => {
        this.downloadingStatement = false;
        this.error = err.error?.message || 'Unable to load statement data.';
      }
    });
  }

  scrollToSection(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  controlLabel(key: keyof Pick<Card360Card,
    'onlineTransactionsEnabled' | 'posTransactionsEnabled' | 'contactlessEnabled'
    | 'atmWithdrawalsEnabled' | 'internationalTransactionsEnabled'>): string {
    const labels: Record<typeof key, string> = {
      onlineTransactionsEnabled: 'Online transactions',
      posTransactionsEnabled: 'POS transactions',
      contactlessEnabled: 'Contactless',
      atmWithdrawalsEnabled: 'ATM / cash withdrawal',
      internationalTransactionsEnabled: 'International transactions'
    };
    return labels[key];
  }

  toggleControl(key: keyof Pick<Card360Card,
    'onlineTransactionsEnabled' | 'posTransactionsEnabled' | 'contactlessEnabled'
    | 'atmWithdrawalsEnabled' | 'internationalTransactionsEnabled'>): void {
    const card = this.selectedCard;
    if (!card) return;
    const enabled = !card[key];
    if (!confirm(`${enabled ? 'Enable' : 'Disable'} ${this.controlLabel(key).toLowerCase()}?`)) return;
    this.http.put<Record<string, boolean>>(
      `${this.api}/cards/credit/${card.id}/controls`,
      { [key]: enabled },
      { headers: this.headers() }
    ).subscribe({
      next: result => {
        card[key] = result[key];
        this.notice = `${this.controlLabel(key)} ${enabled ? 'enabled' : 'disabled'}.`;
        this.error = '';
      },
      error: err => this.error = err.error?.message || 'Unable to update this card control.'
    });
  }

  emergencyLock(): void {
    const card = this.selectedCard;
    if (!card || card.type !== 'credit' || !confirm('Emergency lock will block the card and turn off every transaction channel. Continue?')) {
      return;
    }
    this.http.post<{ status: string; controls: Record<string, boolean> }>(
      `${this.api}/cards/credit/${card.id}/emergency-lock`, {}, { headers: this.headers() }
    ).subscribe({
      next: result => {
        card.status = result.status;
        card.blocked = true;
        this.cardControlKeys.forEach(key => card[key] = false);
        this.notice = 'Card locked. All transaction channels are disabled.';
        this.error = '';
      },
      error: err => this.error = err.error?.message || 'Unable to lock this card.'
    });
  }

  transactionCategory(transaction: Card360Transaction): string {
    const text = `${transaction.merchant || ''} ${transaction.description || ''}`.toLowerCase();
    if (transaction.type.toLowerCase().includes('refund') || /refund/.test(text)) return 'Refunds';
    if (/food|restaurant|cafe|swiggy|zomato/.test(text)) return 'Food';
    if (/fuel|petrol|diesel|shell|hpcl|iocl/.test(text)) return 'Fuel';
    if (/flight|hotel|travel|airline|uber|ola/.test(text)) return 'Travel';
    if (/movie|cinema|netflix|spotify|entertainment/.test(text)) return 'Entertainment';
    if (/hospital|pharmacy|medical|health/.test(text)) return 'Healthcare';
    if (/electric|water|utility|bill|recharge/.test(text)) return 'Bills';
    if (/amazon|flipkart|shopping|store|retail/.test(text)) return 'Shopping';
    return 'Other';
  }

  billRemaining(bill: Card360Bill): number {
    return Math.max(0, bill.totalAmount + bill.fine + bill.penalty - bill.paidAmount);
  }

  billPaymentStatus(bill: Card360Bill): string {
    const remaining = this.billRemaining(bill);
    if (remaining === 0) return 'Paid';
    if (new Date(bill.dueDate) < new Date()) return 'Overdue';
    return bill.paidAmount > 0 ? 'Partially Paid' : 'Unpaid';
  }

  billTransactionAmount(bill: Card360Bill, type: string): number {
    return this.selectedTransactions
      .filter(transaction => transaction.billId === bill.id
        && transaction.type.toLowerCase() === type.toLowerCase())
      .reduce((sum, transaction) => sum + Math.abs(Number(transaction.amount || 0)), 0);
  }

  toggleEmiSelection(id: number, checked: boolean): void {
    this.selectedEmiTransactionIds = checked
      ? [...new Set([...this.selectedEmiTransactionIds, id])]
      : this.selectedEmiTransactionIds.filter(item => item !== id);
  }

  estimateMonthlyEmi(principal: number, months: number): number {
    const monthlyRate = (this.emiSettings?.annualInterestPercent ?? 0) / 1200;
    if (!principal || !months) return 0;
    return monthlyRate ? principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -months)) : principal / months;
  }

  estimateEmiInterest(principal: number, months: number): number {
    return Math.max(0, this.estimateMonthlyEmi(principal, months) * months - principal);
  }

  estimateEmiProcessingFee(principal: number): number {
    return principal * (this.emiSettings?.processingFeePercent ?? 0) / 100;
  }

  convertToEmi(): void {
    const card = this.selectedCard;
    if (!card || !this.selectedEmiTransactionIds.length || !this.emiSettings?.enabled) return;
    if (!confirm(`Convert the selected purchase(s) to a ${this.emiTenureMonths}-month EMI plan?`)) return;
    this.convertingToEmi = true;
    this.http.post(`${this.api}/cards/credit/${card.id}/emi-conversions`, {
      transactionIds: this.selectedEmiTransactionIds,
      tenureMonths: this.emiTenureMonths
    }, { headers: this.headers() }).subscribe({
      next: () => {
        this.convertingToEmi = false;
        this.selectedEmiTransactionIds = [];
        this.notice = 'EMI conversion completed.';
        this.selectCard(card);
      },
      error: err => {
        this.convertingToEmi = false;
        this.error = err.error?.message || 'Unable to convert the selected purchases to EMI.';
      }
    });
  }

  exportTransactions(): void {
    if (!this.filteredTransactions.length) return;
    const columns = ['Date', 'Merchant', 'Receipt/Bill', 'Description', 'Category', 'Type',
      'Amount', 'Status', 'Available Limit'];
    const rows = this.filteredTransactions.map(transaction => [
      transaction.date,
      transaction.merchant,
      transaction.receipt,
      transaction.description,
      this.transactionCategory(transaction),
      transaction.type,
      Number(transaction.amount || 0).toFixed(2),
      transaction.status,
      this.selectedCard?.approvedLimit != null && transaction.balanceAfter != null
        ? (this.selectedCard.approvedLimit - Number(transaction.balanceAfter)).toFixed(2) : ''
    ]);
    const csv = [columns, ...rows].map(row =>
      row.map(value => `"${String(value ?? '').replace(/"/g, '""')}"`).join(',')).join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `card-transactions-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  get currentCardOutstanding(): number {
    return this.selectedCard ? this.billOutstanding(this.selectedCard) : 0;
  }

  private analyticsTransactions(): Card360Transaction[] {
    const now = new Date();
    return this.selectedTransactions.filter(transaction => {
      if (this.analyticsPeriod === 'CURRENT_CYCLE') return !transaction.billId && !transaction.emiPlanId;
      if (this.analyticsPeriod === 'PREVIOUS_CYCLE') return !!this.latestBill && transaction.billId === this.latestBill.id;
      const date = new Date(transaction.date);
      const target = this.analyticsPeriod === 'PREVIOUS_MONTH'
        ? new Date(now.getFullYear(), now.getMonth() - 1, 1) : now;
      return date.getFullYear() === target.getFullYear() && date.getMonth() === target.getMonth();
    });
  }

  private statementMonthKey(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  private financialYearLabel(date: Date): string {
    const startYear = date.getMonth() >= 3 ? date.getFullYear() : date.getFullYear() - 1;
    return `FY ${startYear}-${String(startYear + 1).slice(-2)}`;
  }

  private cardKey(card: Card360Card): string {
    return `${card.type}-${card.id}`;
  }

  private startSessionMonitoring(token: string): boolean {
    const expiresAt = this.getSessionExpiry(token);
    if (expiresAt !== null) {
      const remaining = expiresAt - Date.now() - 5000;
      if (remaining <= 0) {
        this.expireSession();
        return false;
      }
      if (this.sessionExpiryTimer) clearTimeout(this.sessionExpiryTimer);
      this.sessionExpiryTimer = setTimeout(() => this.expireSession(), remaining);
    }
    if (!this.requestRefreshTimer) {
      this.requestRefreshTimer = setInterval(() => this.refreshCustomerRequestHistory(), 15000);
    }
    return true;
  }

  private getSessionExpiry(token: string): number | null {
    const payload = token.split('.')[0];
    if (!payload) return null;
    try {
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='));
      const expiresAtSeconds = Number(decoded.split('|')[2]);
      return Number.isFinite(expiresAtSeconds) ? expiresAtSeconds * 1000 : null;
    } catch {
      return null;
    }
  }

  private clearSessionTimers(): void {
    if (this.requestRefreshTimer) clearInterval(this.requestRefreshTimer);
    if (this.sessionExpiryTimer) clearTimeout(this.sessionExpiryTimer);
    this.requestRefreshTimer = null;
    this.sessionExpiryTimer = null;
  }

  private expireSession(): void {
    if (this.handlingExpiredSession) return;
    this.handlingExpiredSession = true;
    this.clearSessionTimers();
    sessionStorage.removeItem(this.tokenKey);
    this.cards = [];
    this.selectedTransactions = [];
    this.cardBills = [];
    this.customerRequests = [];
    this.emiPlans = [];
    this.emiEligibleTransactions = [];
    this.securityEvents = [];
    this.selectedTransaction = null;
    this.error = 'Your Cards360 session expired. Sign in again to continue.';
    sessionStorage.setItem(this.expiredSessionMessageKey, this.error);
    void this.router.navigate(['/website/cards360']);
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${sessionStorage.getItem(this.tokenKey) || ''}` });
  }

  private handleProtectedError(
    err: { status?: number; error?: { message?: string } },
    fallback = 'Unable to load Card360. Please try again.'
  ): void {
    this.loading = false;
    if (err.status === 401) {
      this.expireSession();
      return;
    }
    this.error = err.error?.message || fallback;
  }
}
