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
  billId?: number | null;
  emiPlanId?: number | null;
  transactionType: string;
  amount: number;
  merchant: string;
  description: string;
  transactionDate: string;
  balanceAfter: number;
  status: string;
}

type TransactionTab = 'ALL' | 'UNBILLED' | 'BILLED' | 'EMI' | 'REFUNDS';

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
  statementStartDate = '';
  statementEndDate = '';
  selectedTransaction: CreditCardTransaction | null = null;
  isUpdatingControls = false;
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

  toggleEmiTransaction(transactionId: number, checked: boolean) {
    this.selectedEmiTransactionIds = checked
      ? [...new Set([...this.selectedEmiTransactionIds, transactionId])]
      : this.selectedEmiTransactionIds.filter(id => id !== transactionId);
  }

  selectedEmiPrincipal(): number {
    return this.emiEligibleTransactions
      .filter(transaction => this.selectedEmiTransactionIds.includes(transaction.id))
      .reduce((sum, transaction) => sum + transaction.amount, 0);
  }

  estimatedEmi(): number {
    const principal = this.selectedEmiPrincipal();
    const annualRate = (this.emiSettings?.annualInterestPercent || 0) / 1200;
    if (!principal || !annualRate || !this.emiTenureMonths) return 0;
    return principal * annualRate / (1 - Math.pow(1 + annualRate, -this.emiTenureMonths));
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
      const totals = new Map<string, number>();
      for (const transaction of this.transactions) {
        const amount = Number(transaction.amount || 0);
        if (amount <= 0 || (transaction.transactionType || '').toLowerCase().includes('refund')) continue;
        const category = this.transactionCategory(transaction);
        totals.set(category, (totals.get(category) || 0) + amount);
      }
      const maximum = Math.max(0, ...totals.values());
      return [...totals.entries()]
        .map(([name, amount]) => ({ name, amount, percent: maximum ? amount / maximum * 100 : 0 }))
        .sort((a, b) => b.amount - a.amount);
    }

  get filteredBills(): CreditCardBill[] {
    const search = this.billSearch.trim().toLowerCase();
    return this.bills.filter(bill =>
      !search || `${bill.billingPeriod} ${bill.status}`.toLowerCase().includes(search)
    );
  }

  get refundTransactions(): CreditCardTransaction[] {
    return this.transactions.filter(transaction =>
      (transaction.transactionType || '').toLowerCase().includes('refund')
    );
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
