import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

interface CreditCardDetails {
  id: number;
  cardNumber: string;
  expiryDate: string;
  pinSet: boolean;
  status: string;
  accountNumber: string;
  userName: string;
  userEmail: string;
  approvedLimit: number;
  currentBalance: number;
  availableLimit: number;
  usageLimit: number;
  userSetSpendingLimit?: number | null;
  overdueAmount: number;
  fine: number;
  penalty: number;
  appliedDate: string;
  approvalDate: string;
  closureDate?: string;
  lastPaidDate?: string;
}

interface CardEmergencyLockResponse {
  controls: CardControls;
  status: string;
}

interface UserProfile {
  name: string;
  email: string;
  accountNumber: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  accountType: string;
  joinDate: string;
  pan?: string;
  aadhar?: string;
  occupation?: string;
  income?: number;
}

interface CreditCardTransaction {
  id: number;
  globalTransactionSequence?: number;
  billId?: number | null;
  emiPlanId?: number | null;
  transactionType: string;
  amount: number;
  merchant: string;
  description: string;
  transactionDate: string;
  balanceAfter: number;
  status: string;
  paymentMethod?: string | null;
}

type TransactionTab = 'ALL' | 'UNBILLED' | 'BILLED' | 'EMI' | 'REFUNDS';
type StatementPeriodFilter = 'ALL' | 'MONTHLY' | 'FINANCIAL_YEAR';

interface CardControls {
  onlineTransactionsEnabled: boolean;
  posTransactionsEnabled: boolean;
  contactlessEnabled: boolean;
  atmWithdrawalsEnabled: boolean;
  internationalTransactionsEnabled: boolean;
}

interface CreditCardBill {
  id: number;
  billGenerationDate: string;
  dueDate: string;
  paidDate?: string;
  totalAmount: number;
  regularAmount?: number;
  emiAmount?: number;
  minimumDue: number;
  paidAmount: number;
  overdueAmount: number;
  fine: number;
  penalty: number;
  status: string;
  billingPeriod: string;
}

interface EmiSettings {
  enabled: boolean;
  annualInterestPercent: number | null;
  processingFeePercent: number;
  minimumTransactionAmount: number;
  tenuresMonths: number[];
}

@Component({
  selector: 'app-creditcard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './creditcard.html',
  styleUrls: ['./creditcard.css'],
  encapsulation: ViewEncapsulation.None
})
export class CreditCard implements OnInit {
  showDetails: boolean = false;
  showCardNumber = false;
  private revealedCardNumber: string | null = null;
  userProfile: UserProfile | null = null;
  creditCards: CreditCardDetails[] = [];
  selectedCard: CreditCardDetails | null = null;
  isCardFlipped: boolean = false;
  showPinForm: boolean = false;
  pinForm = {
    newPin: '',
    confirmPin: ''
  };
  showBillPaymentModal: boolean = false;
  selectedBill: CreditCardBill | null = null;
  billPaymentAmount: number = 0;
  transactions: CreditCardTransaction[] = [];
  bills: CreditCardBill[] = [];
  emiEligibleTransactions: CreditCardTransaction[] = [];
  emiPlans: any[] = [];
  emiSettings: EmiSettings | null = null;
  selectedEmiTransactionIds: number[] = [];
  emiTenureMonths = 3;
  isConvertingToEmi = false;
  isLoading: boolean = false;
  showApplyForm: boolean = false;
  panInput: string = '';
  incomeInput: number | null = null;
  showTermsModal: boolean = false;
  predictedCibil: number | null = null;
  suggestedLimit: number | null = null;
  panBasedLimit: number | null = null;
  cardType: string | null = null;
  eligibility: string | null = null;
  interestRate: number | null = null;
  annualFee: number | null = null;
  showLimitModal: boolean = false;
  limitChangeAmount: number = 0;
  activeTransactionTab: TransactionTab = 'ALL';
  transactionSearch = '';
  transactionCategoryFilter = '';
  transactionStatusFilter = '';
  transactionTypeFilter = '';
  transactionStartDate = '';
  transactionEndDate = '';
  transactionMinAmount: number | null = null;
  transactionMaxAmount: number | null = null;
  billSearch = '';
  statementPeriodFilter: StatementPeriodFilter = 'ALL';
  selectedFinancialYear = this.financialYearLabel(new Date());
  selectedStatementMonth = '';
  statementStartDate = '';
  statementEndDate = '';
  selectedTransaction: CreditCardTransaction | null = null;
  isUpdatingControls = false;
  paymentConfirmation: {
    billId: number;
    amount: number;
    paymentMethod: string;
    reference: string;
    date: string;
    status: string;
  } | null = null;
  customerRequests: any[] = [];
  cardSecurityEvents: any[] = [];
  isLoadingSecurityEvents = false;
  requestType = 'Transaction Dispute';
  requestCategory = 'Unauthorized transaction';
  disputeTransactionId: number | null = null;
  requestDescription = '';
  requestedCreditLimit: number | null = null;
  creditLimitIncreaseCeiling: number | null = null;
  isSubmittingRequest = false;
  readonly customerRequestTypes = [
    'Card Replacement',
    'Credit Limit Increase',
    'Statement Request',
    'Billing Issue',
    'Reward Issue',
    'EMI Issue',
    'Transaction Dispute',
    'Address Update',
    'Card Closure'
  ];
  readonly transactionDisputeCategories = [
    'Unauthorized transaction', 'Duplicate transaction', 'Incorrect amount',
    'Refund not received', 'Merchant dispute', 'Cash withdrawal issue'
  ];
  readonly cardReplacementReasons = ['Lost', 'Stolen', 'Damaged', 'Expired'];
  cardControls: CardControls = {
    onlineTransactionsEnabled: true,
    posTransactionsEnabled: true,
    contactlessEnabled: true,
    atmWithdrawalsEnabled: true,
    internationalTransactionsEnabled: false
  };
  readonly cardControlKeys: Array<keyof CardControls> = [
    'onlineTransactionsEnabled',
    'posTransactionsEnabled',
    'contactlessEnabled',
    'atmWithdrawalsEnabled',
    'internationalTransactionsEnabled'
  ];
  readonly transactionTabs: TransactionTab[] = ['ALL', 'UNBILLED', 'BILLED', 'EMI', 'REFUNDS'];
  readonly statementPeriodFilters: StatementPeriodFilter[] = ['ALL', 'MONTHLY', 'FINANCIAL_YEAR'];
  readonly analyticsPeriods = ['THIS_MONTH', 'PREVIOUS_MONTH', 'CURRENT_CYCLE', 'PREVIOUS_CYCLE'] as const;
  analyticsPeriod: typeof this.analyticsPeriods[number] = 'THIS_MONTH';
  // CIBIL Report Lookup
  cibilReportData: any = null;
  isLoadingCibilReport: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserProfile();
      this.loadCreditCards();
    }
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.http.get(`${environment.apiBaseUrl}/api/users/${user.id}`).subscribe({
        next: (userData: any) => {
          this.userProfile = {
            name: userData.account?.name || userData.username,
            email: userData.email,
            accountNumber: userData.accountNumber,
            phoneNumber: userData.account?.phone || '',
            address: userData.account?.address || '',
            dateOfBirth: userData.account?.dateOfBirth || '',
            accountType: userData.account?.accountType || 'Savings',
            joinDate: userData.account?.joinDate || '',
            pan: userData.account?.pan || '',
            aadhar: userData.account?.aadhar || '',
            occupation: userData.account?.occupation || '',
            income: userData.account?.income || 0
          };
        },
        error: (err: any) => {
          console.error('Error loading user profile:', err);
        }
      });
    }
  }

  loadCreditCards() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const currentUser = sessionStorage.getItem('currentUser');
    if (!currentUser) return;
    
    const user = JSON.parse(currentUser);
    this.isLoading = true;
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/account/${user.accountNumber}`, this.authOptions()).subscribe({
      next: (cards: any) => {
        this.creditCards = Array.isArray(cards) ? cards : [];
        if (this.creditCards.length > 0) {
          const selectedId = this.selectedCard?.id;
          this.selectedCard = this.creditCards.find(card => card.id === selectedId) || this.creditCards[0];
          if (!selectedId || !this.creditCards.some(card => card.id === selectedId)) {
            this.loadCardDetails();
          }
        } else {
          this.selectedCard = null;
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error loading credit cards:', err);
        this.creditCards = [];
        this.selectedCard = null;
        this.isLoading = false;
        this.alertService.operationError('Credit cards', err.error?.message || 'Unable to load your credit cards');
      }
    });
  }

  loadCardDetails() {
    if (!this.selectedCard) return;
    this.showCardNumber = false;
    this.revealedCardNumber = null;
    this.selectedTransaction = null;
    this.cardControls = {
      onlineTransactionsEnabled: true,
      posTransactionsEnabled: true,
      contactlessEnabled: true,
      atmWithdrawalsEnabled: true,
      internationalTransactionsEnabled: false
    };
    this.loadCardControls();
    this.loadTransactions();
    this.loadBills();
    this.loadEmiDetails();
    this.loadCustomerRequests();
    this.loadRequestConfiguration();
    this.loadSecurityEvents();
  }

  loadCardControls() {
    if (!this.selectedCard) return;
    this.http.get<CardControls>(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/controls`,
      this.authOptions()
    ).subscribe({
      next: controls => this.cardControls = { ...this.cardControls, ...controls },
      error: err => {
        console.error('Error loading card controls:', err);
        this.alertService.operationError('Card controls', err.error?.message || 'Unable to load your card controls');
      }
    });
  }

  toggleControl(control: keyof CardControls) {
    if (!this.selectedCard || this.isUpdatingControls) return;
    const previousValue = this.cardControls[control];
    const label = this.controlLabel(control);
    const nextValue = !previousValue;
    if (!confirm(`${nextValue ? 'Enable' : 'Disable'} ${label.toLowerCase()}?`)) return;

    this.isUpdatingControls = true;
    this.http.put<CardControls>(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/controls`,
      { [control]: nextValue },
      this.authOptions()
    ).subscribe({
      next: controls => {
        this.cardControls = { ...this.cardControls, ...controls };
        this.isUpdatingControls = false;
        this.alertService.success('Card control updated', `${label} ${nextValue ? 'enabled' : 'disabled'}.`);
        this.loadCreditCards();
      },
      error: err => {
        this.cardControls[control] = previousValue;
        this.isUpdatingControls = false;
        this.alertService.operationError('Card control', err.error?.message || 'Unable to update this control');
      }
    });
  }

  controlLabel(control: keyof CardControls): string {
    const labels: Record<keyof CardControls, string> = {
      onlineTransactionsEnabled: 'Online transactions',
      posTransactionsEnabled: 'POS transactions',
      contactlessEnabled: 'Contactless',
      atmWithdrawalsEnabled: 'ATM withdrawals',
      internationalTransactionsEnabled: 'International transactions'
    };
    return labels[control];
  }

  private authOptions() {
    const token = isPlatformBrowser(this.platformId) ? sessionStorage.getItem('jwt') : null;
    return {
      headers: new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {})
    };
  }

  emergencyLock() {
    if (!this.selectedCard || !confirm('Emergency lock will block the card and disable every transaction channel. Continue?')) return;
    this.isUpdatingControls = true;
    this.http.post<CardEmergencyLockResponse>(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/emergency-lock`,
      {},
      this.authOptions()
    ).subscribe({
      next: response => {
        this.cardControls = { ...response.controls };
        this.isUpdatingControls = false;
        this.alertService.success('Card locked', 'The card and all transaction channels have been locked.');
        this.loadCreditCards();
      },
      error: err => {
        this.isUpdatingControls = false;
        this.alertService.operationError('Emergency card lock', err.error?.message || 'Unable to lock this card');
      }
    });
  }

  loadTransactions() {
    if (!this.selectedCard) return;
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/transactions`, this.authOptions()).subscribe({
      next: (transactions: any) => {
        this.transactions = Array.isArray(transactions) ? transactions : [];
        if (this.paymentConfirmation) {
          const payment = this.transactions.find(transaction =>
            transaction.billId === this.paymentConfirmation?.billId
            && transaction.transactionType?.toLowerCase() === 'payment'
            && Number(transaction.amount) === this.paymentConfirmation?.amount
          );
          if (payment) {
            this.paymentConfirmation = {
              ...this.paymentConfirmation,
              paymentMethod: payment.paymentMethod || 'ACCOUNT',
              reference: this.transactionReference(payment),
              date: payment.transactionDate
            };
          }
        }
      },
      error: (err: any) => {
        console.error('Error loading transactions:', err);
        this.transactions = [];
      }
    });
  }

  loadBills() {
    if (!this.selectedCard) return;
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/bills`, this.authOptions()).subscribe({
      next: (bills: any) => {
        this.bills = Array.isArray(bills) ? bills : [];
        if (this.availableFinancialYears.length
          && !this.availableFinancialYears.includes(this.selectedFinancialYear)) {
          this.selectedFinancialYear = this.availableFinancialYears[0];
        }
      },
      error: (err: any) => {
        console.error('Error loading bills:', err);
        this.bills = [];
      }
    });
  }

  loadEmiDetails() {
    if (!this.selectedCard) return;
    const cardId = this.selectedCard.id;
    this.selectedEmiTransactionIds = [];
    this.http.get<EmiSettings>(`${environment.apiBaseUrl}/api/credit-cards/emi-settings`).subscribe({
      next: settings => this.emiSettings = settings,
      error: err => {
        console.error('Error loading EMI settings:', err);
        this.emiSettings = null;
      }
    });
    this.http.get<CreditCardTransaction[]>(
      `${environment.apiBaseUrl}/api/credit-cards/${cardId}/emi-eligible-transactions`,
      this.authOptions()
    ).subscribe({
      next: transactions => this.emiEligibleTransactions = Array.isArray(transactions) ? transactions : [],
      error: err => {
        console.error('Error loading EMI-eligible transactions:', err);
        this.emiEligibleTransactions = [];
      }
    });
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/credit-cards/${cardId}/emi-plans`, this.authOptions()).subscribe({
      next: plans => this.emiPlans = Array.isArray(plans) ? plans : [],
      error: err => {
        console.error('Error loading credit-card EMI plans:', err);
        this.emiPlans = [];
      }
    });
  }

  loadRequestConfiguration() {
    this.http.get<any>(`${environment.apiBaseUrl}/api/credit-card-requests/configuration`, this.authOptions()).subscribe({
      next: configuration => {
        this.creditLimitIncreaseCeiling = configuration.creditLimitIncreaseEnabled
          ? Number(configuration.creditLimitIncreaseCeiling)
          : null;
      },
      error: err => {
        console.error('Error loading card request configuration:', err);
        this.creditLimitIncreaseCeiling = null;
      }
    });
  }

  loadCustomerRequests() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/credit-card-requests/mine`, this.authOptions()).subscribe({
      next: requests => {
        this.customerRequests = (Array.isArray(requests) ? requests : [])
          .filter(request => request.creditCardId === this.selectedCard?.id);
      },
      error: err => {
        console.error('Error loading credit-card service requests:', err);
        this.alertService.operationError('Service requests', err.error?.message || 'Unable to load your requests');
      }
    });
  }

  loadSecurityEvents() {
    if (!this.selectedCard) return;
    this.isLoadingSecurityEvents = true;
    this.http.get<any[]>(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/security-events`,
      this.authOptions()
    ).subscribe({
      next: events => {
        this.cardSecurityEvents = Array.isArray(events) ? events : [];
        this.isLoadingSecurityEvents = false;
      },
      error: err => {
        this.isLoadingSecurityEvents = false;
        console.error('Error loading card security events:', err);
        this.cardSecurityEvents = [];
      }
    });
  }

  submitCustomerRequest() {
    if (!this.selectedCard || this.isSubmittingRequest) return;
    const description = this.requestDescription.trim();
    if (description.length < 8) {
      this.alertService.error('Request details required', 'Please provide at least 8 characters describing your request.');
      return;
    }
    if (this.requestType === 'Transaction Dispute' && !this.disputeTransactionId) {
      this.alertService.error('Transaction required', 'Select the transaction you want to dispute.');
      return;
    }
    if (this.requestType === 'Credit Limit Increase'
      && (!this.creditLimitIncreaseCeiling || this.requestedCreditLimit == null
        || !Number.isFinite(this.requestedCreditLimit)
        || this.requestedCreditLimit > this.creditLimitIncreaseCeiling
        || this.requestedCreditLimit <= this.selectedCard.approvedLimit)) {
      this.alertService.error('Invalid requested limit', 'Enter a limit above your current approved limit and at or below the bank-configured ceiling.');
      return;
    }
    const willLockCard = this.requestType === 'Card Replacement'
      && ['Lost', 'Stolen'].includes(this.requestCategory);
    const confirmation = willLockCard
      ? `Report this ${this.requestCategory.toLowerCase()} card and immediately lock it with all transaction channels disabled?`
      : `Submit ${this.requestType.toLowerCase()} request?`;
    if (!confirm(confirmation)) return;

    this.isSubmittingRequest = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/credit-card-requests`, {
      creditCardId: this.selectedCard.id,
      requestType: this.requestType,
      category: ['Transaction Dispute', 'Card Replacement'].includes(this.requestType) ? this.requestCategory : null,
      description,
      requestedLimit: this.requestType === 'Credit Limit Increase' ? this.requestedCreditLimit : null,
      transactionId: this.requestType === 'Transaction Dispute' ? this.disputeTransactionId : null
    }, this.authOptions()).subscribe({
      next: request => {
        this.isSubmittingRequest = false;
        this.requestDescription = '';
        this.requestedCreditLimit = null;
        this.alertService.success('Request submitted', `Your request ${request.requestNumber} has been recorded.`);
        this.loadCustomerRequests();
        if (willLockCard) this.loadCreditCards();
      },
      error: err => {
        this.isSubmittingRequest = false;
        this.alertService.operationError('Service request', err.error?.message || 'Unable to submit your request');
      }
    });
  }

  onRequestTypeChange(type: string) {
    this.requestType = type;
    this.requestCategory = type === 'Card Replacement' ? 'Lost' : 'Unauthorized transaction';
    if (type !== 'Transaction Dispute') this.disputeTransactionId = null;
  }

  disputeTransaction(transaction: CreditCardTransaction) {
    this.requestType = 'Transaction Dispute';
    this.requestCategory = 'Unauthorized transaction';
    this.disputeTransactionId = transaction.id;
    this.requestDescription = `Please investigate transaction ${transaction.id} (${transaction.merchant || transaction.description || 'Card purchase'}) for ₹${Number(transaction.amount).toFixed(2)}. `;
    this.selectedTransaction = null;
    this.scrollTo('supportCenter');
  }

  toggleEmiTransaction(transactionId: number, checked: boolean) {
    this.selectedEmiTransactionIds = checked
      ? [...new Set([...this.selectedEmiTransactionIds, transactionId])]
      : this.selectedEmiTransactionIds.filter(id => id !== transactionId);
  }

  get selectedTransactionIsEmiEligible(): boolean {
    return !!this.selectedTransaction
      && this.emiEligibleTransactions.some(transaction => transaction.id === this.selectedTransaction?.id);
  }

  selectedEmiPrincipal(): number {
    return this.emiEligibleTransactions
      .filter(transaction => this.selectedEmiTransactionIds.includes(transaction.id))
      .reduce((sum, transaction) => sum + transaction.amount, 0);
  }

  estimatedEmi(): number {
    const principal = this.selectedEmiPrincipal();
    return this.estimateMonthlyEmi(principal, this.emiTenureMonths);
  }

  estimateMonthlyEmi(principal: number, months: number): number {
    const annualRate = (this.emiSettings?.annualInterestPercent || 0) / 1200;
    if (!principal || !months) return 0;
    if (!annualRate) return principal / months;
    return principal * annualRate / (1 - Math.pow(1 + annualRate, -months));
  }

  estimateEmiInterest(principal: number, months: number): number {
    return Math.max(0, this.estimateMonthlyEmi(principal, months) * months - principal);
  }

  get emiTenureOptions(): number[] {
    return this.emiSettings?.tenuresMonths || [3, 6, 9, 12, 18, 24];
  }

  convertToEmi() {
    if (!this.selectedCard || !this.selectedEmiTransactionIds.length || !this.emiSettings?.enabled) return;
    this.isConvertingToEmi = true;
    this.http.post<any>(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/emi-conversions`,
      { transactionIds: this.selectedEmiTransactionIds, tenureMonths: this.emiTenureMonths },
      this.authOptions()
    ).subscribe({
      next: plan => {
        this.isConvertingToEmi = false;
        this.alertService.success(
          'EMI created',
          `Plan #${plan.id}: monthly EMI ₹${Number(plan.monthlyEmi).toFixed(2)}, processing fee ₹${Number(plan.processingFeeAmount).toFixed(2)}, total interest ₹${Number(plan.totalInterest).toFixed(2)}.`
        );
        this.loadCardDetails();
        this.loadCreditCards();
      },
      error: err => {
        this.isConvertingToEmi = false;
        this.alertService.operationError('EMI conversion', err.error?.message || 'Unable to convert selected transactions');
      }
    });
  }

  get fullNumber(): string {
    if (!this.selectedCard) return '';
    const number = this.showCardNumber && this.revealedCardNumber
      ? this.revealedCardNumber
      : this.selectedCard.cardNumber || '';
    return this.formatCardNumber(this.showCardNumber && this.revealedCardNumber
      ? number
      : this.maskCardNumber(number));
  }

  maskCardNumber(number: string): string {
    const digits = (number || '').replace(/\D/g, '');
    return digits.length < 4 ? '•••• •••• •••• ••••' : `•••• •••• •••• ${digits.slice(-4)}`;
  }

  get statusClass(): string {
    if (!this.selectedCard) return '';
    return this.selectedCard.status.toLowerCase().replace(/[^a-z0-9]+/g, '-');
  }

  formatCardNumber(number: string): string {
    if (!number) return '';
    return number.replace(/(\d{4})(?=\d)/g, '$1 ');
  }

  toggleCardNumber() {
    if (!this.selectedCard) return;
    if (this.showCardNumber) {
      this.showCardNumber = false;
      this.revealedCardNumber = null;
      return;
    }
    this.http.get<{ cardNumber: string }>(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/card-number`,
      this.authOptions()
    ).subscribe({
      next: response => {
        this.revealedCardNumber = response.cardNumber;
        this.showCardNumber = true;
      },
      error: err => this.alertService.operationError(
        'View card number',
        err.error?.message || 'Unable to verify your session to view this card number'
      )
    });
  }

  toggleDetails() {
    this.showDetails = !this.showDetails;
  }

  setPin() {
    if (!this.selectedCard) return;
    if (this.pinForm.newPin !== this.pinForm.confirmPin) {
      this.alertService.validationError('PINs do not match');
      return;
    }
    if (this.pinForm.newPin.length !== 4) {
      this.alertService.validationError('PIN must be 4 digits');
      return;
    }
    
    this.http.put(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/set-pin`,
      { pin: this.pinForm.newPin },
      this.authOptions()
    ).subscribe({
      next: () => {
        this.alertService.success('PIN Set', 'Credit card PIN has been set successfully');
        this.showPinForm = false;
        this.pinForm = { newPin: '', confirmPin: '' };
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error setting PIN:', err);
        this.alertService.operationError('Set PIN', err?.message || 'Failed to set PIN');
      }
    });
  }

  openBillPayment(bill: CreditCardBill) {
    this.selectedBill = bill;
    this.billPaymentAmount = bill.minimumDue || this.billOutstanding(bill);
    this.showBillPaymentModal = true;
  }

  scrollTo(sectionId: string) {
    if (isPlatformBrowser(this.platformId)) {
      document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  securityActionLabel(action: string): string {
    return String(action || '').replace(/_/g, ' ');
  }

  openReplacementRequest(reason: string = 'Lost') {
    this.onRequestTypeChange('Card Replacement');
    this.requestCategory = reason;
    this.scrollTo('supportCenter');
  }

  openFraudReport() {
    this.onRequestTypeChange('Transaction Dispute');
    this.requestCategory = 'Unauthorized transaction';
    this.disputeTransactionId = null;
    this.scrollTo('supportCenter');
  }

  billTotalDue(bill: CreditCardBill): number {
    return (bill.totalAmount || 0) + (bill.fine || 0) + (bill.penalty || 0);
  }

  billOutstanding(bill: CreditCardBill): number {
    return Math.max(0, this.billTotalDue(bill) - (bill.paidAmount || 0));
  }

  get latestBill(): CreditCardBill | null {
    return this.bills.length
      ? [...this.bills].sort((a, b) => new Date(b.billGenerationDate).getTime() - new Date(a.billGenerationDate).getTime())[0]
      : null;
  }

  get usedLimit(): number {
    if (!this.selectedCard) return 0;
    return Math.max(0, Number(this.selectedCard.approvedLimit || 0) - Number(this.selectedCard.availableLimit || 0));
  }

  get utilizationPercent(): number {
    const limit = Number(this.selectedCard?.approvedLimit || 0);
    return limit > 0 ? Math.min(100, (this.usedLimit / limit) * 100) : 0;
  }

  get remainingBill(): number {
    return this.latestBill ? this.billOutstanding(this.latestBill) : 0;
  }

  setBillPaymentAmount(mode: 'full' | 'minimum') {
    if (!this.selectedBill) return;
    this.billPaymentAmount = mode === 'full'
      ? this.billOutstanding(this.selectedBill)
      : Math.min(Number(this.selectedBill.minimumDue || 0), this.billOutstanding(this.selectedBill));
  }

  payBill() {
    if (!this.selectedBill || !this.billPaymentAmount) {
      this.alertService.validationError('Please enter payment amount');
      return;
    }
    
    this.http.post(
      `${environment.apiBaseUrl}/api/credit-cards/bills/${this.selectedBill.id}/pay?amount=${this.billPaymentAmount}`,
      {},
      this.authOptions()
    ).subscribe({
      next: (bill: any) => {
        this.paymentConfirmation = {
          billId: this.selectedBill!.id,
          amount: Number(this.billPaymentAmount),
          paymentMethod: 'ACCOUNT',
          reference: 'Processing',
          date: new Date().toISOString(),
          status: 'Completed'
        };
        this.alertService.success('Bill Paid', 'Bill payment processed successfully');
        this.showBillPaymentModal = false;
        this.selectedBill = null;
        this.billPaymentAmount = 0;
        this.loadBills();
        this.loadTransactions();
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error paying bill:', err);
        this.alertService.operationError('Pay Bill', err?.message || 'Failed to pay bill');
      }
    });
  }

  getStatement() {
    if (!this.selectedCard) return;
    
    const endDate = this.statementEndDate
      ? new Date(`${this.statementEndDate}T23:59:59`)
      : new Date();
    const startDate = this.statementStartDate
      ? new Date(`${this.statementStartDate}T00:00:00`)
      : new Date(endDate);
    if (!this.statementStartDate) startDate.setMonth(startDate.getMonth() - 1);
    if (startDate > endDate) {
      this.alertService.validationError('Statement start date must be before the end date');
      return;
    }
    const startDateStr = `${this.localDatePart(startDate)}T00:00:00`;
    const endDateStr = `${this.localDatePart(endDate)}T23:59:59`;
    
    this.http.get(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/statement?startDate=${startDateStr}&endDate=${endDateStr}`,
      this.authOptions()
    ).subscribe({
      next: (transactions: any) => {
        if (Array.isArray(transactions) && transactions.length > 0) {
          // Create a downloadable statement
          this.downloadTransactionsCsv(transactions, `credit-card-statement-${Date.now()}.csv`);
          
          this.alertService.success('Statement Generated', `Statement downloaded with ${transactions.length} transactions`);
        } else {
          this.alertService.info('No Transactions', 'No transactions found for the selected period');
        }
      },
      error: (err: any) => {
        console.error('Error getting statement:', err);
        this.alertService.operationError('Get Statement', err?.message || 'Failed to get statement');
      }
    });
  }

  private localDatePart(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  openApplyForm() {
    this.showApplyForm = true;
  }

  closeApplyForm() {
    this.showApplyForm = false;
    this.panInput = '';
    this.incomeInput = null;
    this.showTermsModal = false;
    this.predictedCibil = null;
    this.suggestedLimit = null;
    this.panBasedLimit = null;
    this.cardType = null;
    this.eligibility = null;
    this.interestRate = null;
    this.annualFee = null;
    this.cibilReportData = null;
  }

  previewCreditPrediction() {
    if (!this.panInput) {
      this.alertService.validationError('PAN is required');
      return;
    }
    
    // Validate PAN format (10 characters: 5 letters, 4 digits, 1 letter)
    if (this.panInput.length !== 10) {
      this.alertService.validationError('PAN must be 10 characters long');
      return;
    }
    
    // Fetch credit prediction
    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/preview?pan=${this.panInput}&income=${this.incomeInput || 0}`, {}).subscribe({
      next: (res: any) => {
        this.predictedCibil = res.predictedCibil;
        this.suggestedLimit = res.suggestedLimit;
        this.panBasedLimit = res.panBasedLimit;
        this.cardType = res.cardType;
        this.eligibility = res.eligibility;
        this.interestRate = res.interestRate;
        this.annualFee = res.annualFee;
        this.showTermsModal = true;
      },
      error: (err: any) => {
        console.error('Error previewing credit prediction:', err);
        this.alertService.operationError('Credit prediction', err?.message || 'Preview failed');
      }
    });

    // Also lookup CIBIL report from manager-uploaded data
    this.isLoadingCibilReport = true;
    this.cibilReportData = null;
    this.http.get<any>(`${environment.apiBaseUrl}/api/cibil-reports/lookup/pan/${this.panInput}`).subscribe({
      next: (res: any) => {
        this.isLoadingCibilReport = false;
        if (res.found) {
          this.cibilReportData = res;
        }
      },
      error: () => {
        this.isLoadingCibilReport = false;
      }
    });
  }

  submitCreditApplication() {
    if (!this.panInput || !this.userProfile) {
      this.alertService.validationError('PAN is required');
      return;
    }
    
    const payload: any = {
      accountNumber: this.userProfile.accountNumber,
      userName: this.userProfile.name,
      userEmail: this.userProfile.email,
      pan: this.panInput
    };
    
    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/create?income=${this.incomeInput || 0}`, payload).subscribe({
      next: (res: any) => {
        this.alertService.success('Application Submitted', 'Your credit card application has been submitted for review.');
        this.closeApplyForm();
      },
      error: (err: any) => {
        console.error('Error submitting credit application:', err);
        this.alertService.operationError('Credit application', err?.message || 'Submission failed');
      }
    });
  }

  goBack() {
    this.router.navigate(['/userdashboard']);
  }

  refreshCardData() {
    this.loadCreditCards();
  }

  blockCard() {
    if (!this.selectedCard) return;
    this.emergencyLock();
  }

  transactionCategory(transaction: CreditCardTransaction): string {
      const text = `${transaction.merchant || ''} ${transaction.description || ''}`.toLowerCase();
      if (/food|restaurant|cafe|swiggy|zomato/.test(text)) return 'Food';
      if (/fuel|petrol|diesel|shell|hpcl|iocl/.test(text)) return 'Fuel';
      if (/flight|hotel|travel|airline|uber|ola/.test(text)) return 'Travel';
      if (/movie|cinema|netflix|spotify|entertainment/.test(text)) return 'Entertainment';
      if (/hospital|pharmacy|medical|health/.test(text)) return 'Healthcare';
      if (/electric|water|utility|bill|recharge/.test(text)) return 'Bills';
      if (/refund/.test(`${transaction.transactionType} ${text}`)) return 'Refunds';
      if (/amazon|flipkart|shopping|store|retail/.test(text)) return 'Shopping';
      return 'Other';
    }

  get filteredTransactions(): CreditCardTransaction[] {
      const search = this.transactionSearch.trim().toLowerCase();
      return this.transactions.filter(transaction => {
        const type = (transaction.transactionType || '').toLowerCase();
        const isRefund = type.includes('refund');
        const matchesTab = this.activeTransactionTab === 'ALL'
          || (this.activeTransactionTab === 'UNBILLED' && !transaction.billId && !transaction.emiPlanId)
          || (this.activeTransactionTab === 'BILLED' && !!transaction.billId)
          || (this.activeTransactionTab === 'EMI' && !!transaction.emiPlanId)
          || (this.activeTransactionTab === 'REFUNDS' && isRefund);
        const matchesSearch = !search || [
          transaction.merchant, transaction.description, transaction.transactionType, transaction.status, String(transaction.id)
        ].some(value => (value || '').toLowerCase().includes(search));
        const matchesCategory = !this.transactionCategoryFilter
          || this.transactionCategory(transaction) === this.transactionCategoryFilter;
        const matchesStatus = !this.transactionStatusFilter
          || (transaction.status || '').toLowerCase() === this.transactionStatusFilter.toLowerCase();
        const transactionDate = (transaction.transactionDate || '').slice(0, 10);
        const matchesStartDate = !this.transactionStartDate || transactionDate >= this.transactionStartDate;
        const matchesEndDate = !this.transactionEndDate || transactionDate <= this.transactionEndDate;
        const amount = Number(transaction.amount || 0);
        const matchesMinAmount = this.transactionMinAmount == null || amount >= this.transactionMinAmount;
        const matchesMaxAmount = this.transactionMaxAmount == null || amount <= this.transactionMaxAmount;
        const matchesType = !this.transactionTypeFilter
          || type === this.transactionTypeFilter.toLowerCase();
        return matchesTab && matchesSearch && matchesCategory && matchesStatus
          && matchesStartDate && matchesEndDate && matchesMinAmount && matchesMaxAmount && matchesType;
      });
    }

  get spendingCategories(): Array<{ name: string; amount: number; percent: number }> {
      const categoryNames = ['Shopping', 'Food', 'Fuel', 'Travel', 'Entertainment', 'Bills', 'Healthcare', 'Other'];
      const totals = new Map<string, number>(categoryNames.map(category => [category, 0] as [string, number]));
      for (const transaction of this.transactions) {
        const amount = Number(transaction.amount || 0);
        if (amount <= 0 || !this.isPurchase(transaction)) continue;
        if (!this.matchesSelectedAnalyticsPeriod(transaction)) continue;
        const category = this.transactionCategory(transaction);
        totals.set(category, (totals.get(category) || 0) + amount);
      }
      const maximum = Math.max(0, ...totals.values());
      return [...totals.entries()]
        .map(([name, amount]) => ({ name, amount, percent: maximum ? amount / maximum * 100 : 0 }))
        .sort((a, b) => b.amount - a.amount);
    }

  get hasPurchasesForSelectedPeriod(): boolean {
    return this.transactions.some(transaction =>
      this.isPurchase(transaction) && this.matchesSelectedAnalyticsPeriod(transaction));
  }

  get filteredBills(): CreditCardBill[] {
    const search = this.billSearch.trim().toLowerCase();
    return this.bills.filter(bill => {
      const periodDate = new Date(bill.billGenerationDate);
      const monthLabel = periodDate.toLocaleString('en', { month: 'long', year: 'numeric' });
      const financialYear = this.financialYearLabel(periodDate);
      const matchesSearch = !search
        || `${bill.billingPeriod} ${bill.status} ${monthLabel} ${financialYear}`.toLowerCase().includes(search);
      const matchesPeriod = this.statementPeriodFilter !== 'FINANCIAL_YEAR'
        ? this.statementPeriodFilter !== 'MONTHLY'
          || !this.selectedStatementMonth
          || this.statementMonthKey(periodDate) === this.selectedStatementMonth
        : financialYear === this.selectedFinancialYear;
      return matchesSearch && matchesPeriod;
    });
  }

  get availableFinancialYears(): string[] {
    return [...new Set(this.bills.map(bill => this.financialYearLabel(new Date(bill.billGenerationDate))))]
      .sort((left, right) => right.localeCompare(left));
  }

  get availableStatementMonths(): Array<{ key: string; label: string }> {
    const months = new Map<string, string>();
    for (const bill of this.bills) {
      const date = new Date(bill.billGenerationDate);
      months.set(this.statementMonthKey(date), date.toLocaleString('en', { month: 'long', year: 'numeric' }));
    }
    return [...months.entries()]
      .map(([key, label]) => ({ key, label }))
      .sort((left, right) => right.key.localeCompare(left.key));
  }

  private statementMonthKey(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  billStatusClass(bill: CreditCardBill): string {
    return this.billPaymentStatus(bill).toLowerCase().replace(/\s+/g, '-');
  }

  statementLabel(bill: CreditCardBill): string {
    const date = new Date(bill.billGenerationDate);
    return date.toLocaleString('en', { month: 'long', year: 'numeric' });
  }

  statementFinancialYear(bill: CreditCardBill): string {
    return this.financialYearLabel(new Date(bill.billGenerationDate));
  }

  financialYearLabel(date: Date): string {
    const startYear = date.getMonth() >= 3 ? date.getFullYear() : date.getFullYear() - 1;
    return `FY ${startYear}-${String(startYear + 1).slice(-2)}`;
  }

  billPaymentStatus(bill: CreditCardBill): string {
    const outstanding = this.billOutstanding(bill);
    if (outstanding <= 0) return 'Paid';
    const dueDate = new Date(bill.dueDate);
    if (dueDate.getTime() < Date.now()) return 'Overdue';
    return Number(bill.paidAmount || 0) > 0 ? 'Partially Paid' : 'Unpaid';
  }

  billTransactions(bill: CreditCardBill): CreditCardTransaction[] {
    return this.transactions.filter(transaction => transaction.billId === bill.id);
  }

  billTransactionAmount(bill: CreditCardBill, type: string): number {
    return this.billTransactions(bill)
      .filter(transaction => (transaction.transactionType || '').toLowerCase() === type.toLowerCase())
      .reduce((total, transaction) => total + Number(transaction.amount || 0), 0);
  }

  get refundTransactions(): CreditCardTransaction[] {
    return this.transactions.filter(transaction =>
      (transaction.transactionType || '').toLowerCase().includes('refund')
    );
  }

  probableOriginalTransaction(refund: CreditCardTransaction): CreditCardTransaction | null {
    const merchant = (refund.merchant || '').trim().toLowerCase();
    if (!merchant) return null;
    return this.transactions
      .filter(transaction => transaction.id !== refund.id
        && (transaction.merchant || '').trim().toLowerCase() === merchant
        && !String(transaction.transactionType || '').toLowerCase().includes('refund')
        && new Date(transaction.transactionDate).getTime() <= new Date(refund.transactionDate).getTime())
      .sort((left, right) =>
        new Date(right.transactionDate).getTime() - new Date(left.transactionDate).getTime())[0] || null;
  }

  get failedTransactions(): CreditCardTransaction[] {
    return this.transactions.filter(transaction =>
      (transaction.status || '').toLowerCase() === 'failed'
    );
  }

  private transactionDate(transaction: CreditCardTransaction): Date {
    return new Date(transaction.transactionDate);
  }

  private matchesSelectedAnalyticsPeriod(transaction: CreditCardTransaction): boolean {
    if (this.analyticsPeriod === 'CURRENT_CYCLE') {
      return !transaction.billId && !transaction.emiPlanId;
    }
    if (this.analyticsPeriod === 'PREVIOUS_CYCLE') {
      return !!this.latestBill && transaction.billId === this.latestBill.id;
    }
    const date = this.transactionDate(transaction);
    const now = new Date();
    const targetMonth = this.analyticsPeriod === 'PREVIOUS_MONTH'
      ? new Date(now.getFullYear(), now.getMonth() - 1, 1)
      : now;
    return date.getFullYear() === targetMonth.getFullYear()
      && date.getMonth() === targetMonth.getMonth();
  }

  private isPurchase(transaction: CreditCardTransaction): boolean {
    const type = (transaction.transactionType || '').toLowerCase();
    return (type === 'purchase' || type === 'fee' || type === 'interest')
      && !type.includes('refund')
      && Number(transaction.amount) > 0;
  }

  private transactionSpend(transactions: CreditCardTransaction[]): number {
    return transactions.filter(transaction => this.isPurchase(transaction))
      .reduce((total, transaction) => total + Number(transaction.amount || 0), 0);
  }

  get thisMonthSpend(): number {
    const now = new Date();
    return this.transactionSpend(this.transactions.filter(transaction => {
      const date = this.transactionDate(transaction);
      return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
    }));
  }

  get previousMonthSpend(): number {
    const now = new Date();
    const previous = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    return this.transactionSpend(this.transactions.filter(transaction => {
      const date = this.transactionDate(transaction);
      return date.getFullYear() === previous.getFullYear() && date.getMonth() === previous.getMonth();
    }));
  }

  get currentCycleSpend(): number {
    return this.transactionSpend(this.transactions.filter(transaction => !transaction.billId && !transaction.emiPlanId));
  }

  get previousCycleSpend(): number {
    const latestBill = this.latestBill;
    if (!latestBill) return 0;
    return this.transactionSpend(this.transactions.filter(transaction => transaction.billId === latestBill.id));
  }

  get selectedAnalyticsSpend(): number {
    switch (this.analyticsPeriod) {
      case 'PREVIOUS_MONTH': return this.previousMonthSpend;
      case 'CURRENT_CYCLE': return this.currentCycleSpend;
      case 'PREVIOUS_CYCLE': return this.previousCycleSpend;
      default: return this.thisMonthSpend;
    }
  }

  get averageMonthlySpend(): number {
    const byMonth = new Map<string, number>();
    for (const transaction of this.transactions) {
      if (!this.isPurchase(transaction)) continue;
      const date = this.transactionDate(transaction);
      const key = `${date.getFullYear()}-${date.getMonth()}`;
      byMonth.set(key, (byMonth.get(key) || 0) + Number(transaction.amount || 0));
    }
    const monthValues = [...byMonth.values()];
    return monthValues.length
      ? monthValues.reduce((total, amount) => total + amount, 0) / monthValues.length
      : 0;
  }

  get monthlySpendTrend(): Array<{ label: string; amount: number; percent: number }> {
    const today = new Date();
    const months = Array.from({ length: 6 }, (_, index) => new Date(today.getFullYear(), today.getMonth() - (5 - index), 1));
    const totals = months.map(month => {
      const amount = this.transactionSpend(this.transactions.filter(transaction => {
        const date = this.transactionDate(transaction);
        return date.getFullYear() === month.getFullYear() && date.getMonth() === month.getMonth();
      }));
      return { label: month.toLocaleString('en', { month: 'short' }), amount };
    });
    const maximum = Math.max(0, ...totals.map(item => item.amount));
    return totals.map(item => ({ ...item, percent: maximum ? item.amount / maximum * 100 : 0 }));
  }

  get topMerchants(): Array<{ merchant: string; amount: number }> {
    const totals = new Map<string, number>();
    for (const transaction of this.transactions) {
      if (!this.isPurchase(transaction) || !this.matchesSelectedAnalyticsPeriod(transaction)) continue;
      const merchant = (transaction.merchant || transaction.description || 'Other').trim();
      totals.set(merchant, (totals.get(merchant) || 0) + Number(transaction.amount || 0));
    }
    return [...totals.entries()]
      .map(([merchant, amount]) => ({ merchant, amount }))
      .sort((left, right) => right.amount - left.amount)
      .slice(0, 5);
  }

  get cardHealthScore(): number | null {
    if (!this.bills.length || !this.selectedCard) return null;
    const paidBills = this.bills.filter(bill => this.billOutstanding(bill) <= 0).length;
    const paymentConsistency = (paidBills / this.bills.length) * 100;
    const utilization = this.utilizationPercent;
    const failedRate = this.transactions.length
      ? (this.failedTransactions.length / this.transactions.length) * 100
      : 0;
    const emiBurden = this.emiPlans.reduce((total, plan) =>
      total + Number(plan.monthlyEmi || 0), 0);
    const limit = Number(this.selectedCard.approvedLimit || 0);
    const emiBurdenPercent = limit > 0 ? Math.min(100, emiBurden / limit * 100) : 100;
    return Math.round(Math.max(0, Math.min(100,
      (100 - utilization) * 0.4
      + paymentConsistency * 0.3
      + (100 - utilization) * 0.15
      + (100 - emiBurdenPercent) * 0.1
      + (100 - failedRate) * 0.05)));
  }

  get cardHealthFactors(): Array<{ label: string; score: number }> {
    if (this.cardHealthScore == null) return [];
    const paidBills = this.bills.filter(bill => this.billOutstanding(bill) <= 0).length;
    const limit = Number(this.selectedCard?.approvedLimit || 0);
    const emiBurden = this.emiPlans.reduce((total, plan) => total + Number(plan.monthlyEmi || 0), 0);
    return [
      { label: 'Credit utilization', score: Math.round(100 - this.utilizationPercent) },
      { label: 'Payment consistency', score: Math.round(paidBills / this.bills.length * 100) },
      { label: 'Outstanding balance', score: Math.round(limit ? 100 - Math.min(100, this.remainingBill / limit * 100) : 0) },
      { label: 'EMI burden', score: Math.round(limit ? 100 - Math.min(100, emiBurden / limit * 100) : 0) },
      { label: 'Transaction health', score: Math.round(100 - (this.transactions.length ? this.failedTransactions.length / this.transactions.length * 100 : 0)) }
    ];
  }

  get emiInstallmentCounts(): { active: number; completed: number; upcoming: number } {
    let active = 0;
    let completed = 0;
    let upcoming = 0;
    for (const plan of this.emiPlans) {
      const status = String(plan.status || '').toLowerCase();
      if (status === 'completed' || Number(plan.outstandingPrincipal || 0) <= 0) completed++;
      else active++;
      for (const installment of (plan.installments || [])) {
        const due = new Date(installment.dueDate).getTime();
        if (due >= Date.now() && String(installment.status || '').toLowerCase() !== 'paid') upcoming++;
      }
    }
    return { active, completed, upcoming };
  }

  transactionReference(transaction: CreditCardTransaction): string {
    return transaction.globalTransactionSequence
      ? String(transaction.globalTransactionSequence)
      : `CC-${transaction.id}`;
  }

  selectTransactionForEmi(transaction: CreditCardTransaction) {
    if (!this.emiEligibleTransactions.some(item => item.id === transaction.id)) {
      this.alertService.info('Not eligible for EMI', 'This purchase is not currently eligible for conversion.');
      return;
    }
    if (!this.selectedEmiTransactionIds.includes(transaction.id)) {
      this.toggleEmiTransaction(transaction.id, true);
    }
    this.selectedTransaction = null;
    this.scrollTo('emiCenter');
  }

  downloadBillStatement(bill: CreditCardBill, format: 'CSV' | 'PDF') {
    if (format === 'CSV') {
      const billTransactions = this.transactions.filter(transaction => transaction.billId === bill.id);
      const columns = ['Statement', 'Due date', 'Total', 'Paid', 'Remaining', 'Minimum due', 'Status'];
      const summary = [
        this.statementLabel(bill), bill.dueDate, this.billTotalDue(bill), bill.paidAmount,
        this.billOutstanding(bill), bill.minimumDue, this.billPaymentStatus(bill)
      ];
      const escape = (value: unknown) => `"${String(value ?? '').replace(/"/g, '""')}"`;
      const billRow = columns.map((column, index) => `${escape(column)},${escape(summary[index])}`).join('\r\n');
      const csv = billTransactions.length
        ? billRow + '\r\n\r\n' + this.csvRows(billTransactions)
        : billRow;
      this.downloadCsv(csv, `credit-card-statement-${bill.id}.csv`);
      return;
    }
    this.printStatement(bill);
  }

  private csvRows(transactions: CreditCardTransaction[]): string {
    const columns = ['Date', 'Merchant', 'Description', 'Category', 'Type', 'Amount', 'Status', 'Bill', 'EMI'];
    const escape = (value: unknown) => `"${String(value ?? '').replace(/"/g, '""')}"`;
    const rows = transactions.map(transaction => [
      transaction.transactionDate,
      transaction.merchant,
      transaction.description,
      this.transactionCategory(transaction),
      transaction.transactionType,
      transaction.amount,
      transaction.status,
      transaction.billId ? `Bill ${transaction.billId}` : '',
      transaction.emiPlanId ? `EMI ${transaction.emiPlanId}` : ''
    ].map(escape).join(','));
    return [columns.map(escape).join(','), ...rows].join('\r\n');
  }

  private downloadCsv(content: string, filename: string) {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  private printStatement(bill: CreditCardBill) {
    const transactions = this.transactions.filter(transaction => transaction.billId === bill.id);
    const popup = window.open('', '_blank');
    if (!popup) {
      this.alertService.operationError('Statement PDF', 'Allow pop-ups to print or save this statement as PDF.');
      return;
    }
    const rows = transactions.map(transaction =>
      `<tr><td>${this.escapeHtml(new Date(transaction.transactionDate).toLocaleString())}</td><td>${this.escapeHtml(transaction.merchant || '')}</td><td>${this.escapeHtml(transaction.description || '')}</td><td>₹${Number(transaction.amount || 0).toFixed(2)}</td></tr>`
    ).join('');
    popup.document.write(`<html><head><title>NeoBank card statement</title><style>body{font:14px Arial,sans-serif;padding:32px;color:#172033}h1{color:#173a74}table{width:100%;border-collapse:collapse;margin-top:24px}td,th{border:1px solid #d5dbe5;padding:8px;text-align:left}.summary{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:20px}.muted{color:#596579}</style></head><body><h1>NeoBank Credit Card Statement</h1><p class="muted">${this.escapeHtml(this.statementLabel(bill))}</p><div class="summary"><span>Total: ₹${this.billTotalDue(bill).toFixed(2)}</span><span>Paid: ₹${Number(bill.paidAmount || 0).toFixed(2)}</span><span>Remaining: ₹${this.billOutstanding(bill).toFixed(2)}</span><span>Minimum due: ₹${Number(bill.minimumDue || 0).toFixed(2)}</span><span>Due date: ${this.escapeHtml(new Date(bill.dueDate).toLocaleDateString())}</span><span>Status: ${this.escapeHtml(this.billPaymentStatus(bill))}</span></div><table><thead><tr><th>Date</th><th>Merchant</th><th>Description</th><th>Amount</th></tr></thead><tbody>${rows}</tbody></table></body></html>`);
    popup.document.close();
    popup.focus();
    popup.print();
  }

  private escapeHtml(value: string): string {
    return value.replace(/[&<>"']/g, character => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[character] || character);
  }

  downloadCurrentBill(format: 'CSV' | 'PDF') {
    const bill = this.latestBill;
    if (!bill) {
      this.alertService.info('No current bill', 'A statement is not available to download yet.');
      return;
    }
    this.downloadBillStatement(bill, format);
  }

  downloadPreviousBill(format: 'CSV' | 'PDF') {
    const previous = [...this.bills]
      .sort((left, right) => new Date(right.billGenerationDate).getTime() - new Date(left.billGenerationDate).getTime())[1];
    if (!previous) {
      this.alertService.info('No previous bill', 'There is no earlier statement available to download.');
      return;
    }
    this.downloadBillStatement(previous, format);
  }

  get recurringPayments(): Array<{ merchant: string; amount: number; frequency: string; nextPayment: Date }> {
    const purchases = this.transactions.filter(transaction =>
      (transaction.transactionType || '').toLowerCase() === 'purchase'
      && transaction.merchant
      && Number(transaction.amount) > 0
    );
    const byMerchant = new Map<string, CreditCardTransaction[]>();
    for (const transaction of purchases) {
      const key = transaction.merchant.trim().toLowerCase();
      byMerchant.set(key, [...(byMerchant.get(key) || []), transaction]);
    }

    const recurring: Array<{ merchant: string; amount: number; frequency: string; nextPayment: Date }> = [];
    byMerchant.forEach(items => {
      const ordered = [...items].sort((a, b) =>
        new Date(a.transactionDate).getTime() - new Date(b.transactionDate).getTime()
      );
      if (ordered.length < 2) return;
      const latest = ordered[ordered.length - 1];
      const prior = ordered[ordered.length - 2];
      const daysBetween = Math.abs(
        new Date(latest.transactionDate).getTime() - new Date(prior.transactionDate).getTime()
      ) / 86400000;
      const frequency = daysBetween >= 25 && daysBetween <= 35
        ? 'Monthly'
        : daysBetween >= 350 && daysBetween <= 380 ? 'Yearly' : '';
      if (!frequency) return;
      const nextPayment = new Date(latest.transactionDate);
      if (frequency === 'Monthly') nextPayment.setMonth(nextPayment.getMonth() + 1);
      else nextPayment.setFullYear(nextPayment.getFullYear() + 1);
      recurring.push({
        merchant: latest.merchant,
        amount: Number(latest.amount),
        frequency,
        nextPayment
      });
    });
    return recurring.sort((a, b) => a.nextPayment.getTime() - b.nextPayment.getTime());
  }

  get estimatedMonthlySubscriptions(): number {
    return this.recurringPayments.reduce((total, payment) =>
      total + (payment.frequency === 'Monthly' ? payment.amount : payment.amount / 12), 0
    );
  }

  exportTransactions() {
      this.downloadTransactionsCsv(this.filteredTransactions, `credit-card-transactions-${Date.now()}.csv`);
    }

    exportBillTransactions(bill: CreditCardBill) {
      const billTransactions = this.transactions.filter(transaction => transaction.billId === bill.id);
      if (!billTransactions.length) {
        this.alertService.info('No bill transactions', 'No transaction details are linked to this statement.');
        return;
      }
      this.downloadTransactionsCsv(billTransactions, `credit-card-bill-${bill.id}.csv`);
    }

  private downloadTransactionsCsv(transactions: CreditCardTransaction[], filename: string) {
      const columns = ['Date', 'Merchant', 'Description', 'Category', 'Type', 'Amount', 'Status', 'Bill', 'EMI'];
      const escape = (value: unknown) => `"${String(value ?? '').replace(/"/g, '""')}"`;
      const rows = transactions.map(transaction => [
        transaction.transactionDate,
        transaction.merchant,
        transaction.description,
        this.transactionCategory(transaction),
        transaction.transactionType,
        transaction.amount,
        transaction.status,
        transaction.billId ? `Bill ${transaction.billId}` : '',
        transaction.emiPlanId ? `EMI ${transaction.emiPlanId}` : ''
      ].map(escape).join(','));
      const blob = new Blob([[columns.map(escape).join(','), ...rows].join('\r\n')], { type: 'text/csv;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      anchor.click();
      window.URL.revokeObjectURL(url);
    }

  toggleCardStatus() {
    if (!this.selectedCard) return;
    if (!confirm('Unlock this credit card? Transaction channels will remain disabled until you turn them back on.')) return;
    this.http.post<CreditCardDetails>(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/unlock`,
      {},
      this.authOptions()
    ).subscribe({
      next: card => {
        this.selectedCard = card;
        this.alertService.success('Card unlocked', 'The card is active. Review your card controls before using it.');
        this.loadCreditCards();
      },
      error: err => this.alertService.operationError('Unlock card', err.error?.message || 'Unable to unlock the card')
    });
  }

  changeLimit() {
    if (!this.selectedCard || !Number.isFinite(this.limitChangeAmount) || this.limitChangeAmount <= 0) {
      this.alertService.validationError('Enter a spending limit greater than zero');
      return;
    }

    const currentLimit = Number(this.selectedCard.userSetSpendingLimit ?? this.selectedCard.approvedLimit ?? 0);
    const maximumLimit = Number(this.selectedCard.approvedLimit || 0);
    if (this.limitChangeAmount > maximumLimit) {
      this.alertService.validationError('Spending limit cannot exceed your bank-approved credit limit');
      return;
    }

    if (!confirm(`Change your spending limit from ₹${currentLimit.toFixed(2)} to ₹${this.limitChangeAmount.toFixed(2)}?`)) {
      return;
    }

    this.http.put<CreditCardDetails>(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/spending-limit`,
      { spendingLimit: this.limitChangeAmount },
      this.authOptions()
    ).subscribe({
      next: (updatedCard: any) => {
        this.alertService.success('Spending limit updated', `Your spending limit is now ₹${this.limitChangeAmount.toFixed(2)}.`);
        this.showLimitModal = false;
        this.selectedCard = { ...this.selectedCard!, ...updatedCard };
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error updating limit:', err);
        this.alertService.operationError('Update spending limit', err?.error?.message || err?.message || 'Failed to update spending limit');
      }
    });
  }
}
