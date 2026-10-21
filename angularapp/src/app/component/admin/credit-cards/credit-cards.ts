import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-credit-cards',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './credit-cards.html',
  styleUrls: ['./credit-cards.css']
})
export class CreditCards implements OnInit, OnDestroy {
  private refreshTimer: any = null;
  creditCards: any[] = [];
  filteredCreditCards: any[] = [];
  isLoadingCreditCards: boolean = false;
  canRevealCardNumber = false;
  isRevealingCardNumber = false;
  revealedCreditCardId: number | null = null;
  revealedCardNumber = '';
  selectedCreditCard: any = null;
  creditCardTransactions: any[] = [];
  filteredTransactions: any[] = [];
  creditCardBills: any[] = [];
  selectedTransaction: any = null;
  transactionSearchQuery = '';
  transactionPageSize = 10;
  transactionPage = 1;
  transactionFilters = {
    startDate: '',
    endDate: '',
    type: '',
    paymentMethod: '',
    status: '',
    merchant: '',
    minAmount: null as number | null,
    maxAmount: null as number | null,
    category: '',
    processedBy: '',
    region: ''
  };
  creditCardSearchQuery: string = '';
  creditCardStatusFilter: string = 'ALL';
  showApplyCreditCardModal: boolean = false;
  applyCreditCardForm = {
    accountNumber: '',
    userName: '',
    userEmail: '',
    pan: '',
    income: 0
  };
  showLimitModal: boolean = false;
  limitAction: 'increase' | 'decrease' = 'increase';
  limitChangeAmount: number = 0;
  limitChangeReason: string = '';
  adminName: string = 'Admin';
  
  // Limit change history
  limitChangeHistory: any[] = [];
  showHistoryModal: boolean = false;
  
  // Document upload for closing card
  showCloseCardModal: boolean = false;
  closeCardDocument: File | null = null;
  closeCardDocumentName: string = '';
  isUploadingDocument: boolean = false;

  showBillPaymentModal: boolean = false;
  selectedBillForPayment: any = null;
  billPaymentForm = {
    amount: 0,
    paymentMethod: 'ACCOUNT',
    debitAccountNumber: '',
    chequeNumber: ''
  };
  isPayingBill: boolean = false;
  chequeImageFile: File | null = null;
  chequeImagePreviewName = '';
  paymentAccountDetails: any = null;
  chequeVerification: any = null;
  isVerifyingCheque: boolean = false;

  // Transfer credit card amount to any account
  showTransferModal: boolean = false;
  transferForm = { destinationAccountNumber: '', amount: 0 };
  transferAccountDetails: any = null;
  isVerifyingTransferAccount: boolean = false;
  isTransferring: boolean = false;
  transferFeePercent: number = 2;
  isSavingTransferFee: boolean = false;
  emiAnnualInterestPercent: number | null = null;
  emiProcessingFeePercent = 8;
  creditCardEmiPlans: any[] = [];
  isSavingEmiSettings = false;
  creditLimitIncreaseCeiling = 0;
  isSavingCreditLimitCeiling = false;
  selectedCardServiceRequests: any[] = [];
  isLoadingCardServiceRequests = false;
  cardServiceRequestError = '';

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      // Get admin name from sessionStorage
      const adminData = sessionStorage.getItem('admin') || sessionStorage.getItem('adminData');
      if (adminData) {
        try {
          const admin = JSON.parse(adminData);
          this.adminName = admin.name || admin.username || 'Admin';
          this.canRevealCardNumber = String(admin.role || sessionStorage.getItem('userRole') || '').toUpperCase() === 'ADMIN';
        } catch (e) {
          console.error('Error parsing admin data:', e);
        }
      }
      this.loadCreditCards();
      this.loadTransferFeePercent();
      this.loadEmiSettings();
      this.loadCreditLimitIncreaseCeiling();
      this.refreshTimer = setInterval(() => this.silentRefresh(), 10000);
    }
  }

  ngOnDestroy() {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  private authOptions() {
    const token = sessionStorage.getItem('adminSessionToken');
    return { headers: new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {}) };
  }

  // Keeps card list, balances, bills and transactions live (e.g. user-side bill payments)
  private silentRefresh() {
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/all`, this.authOptions()).subscribe({
      next: (response: any) => {
        this.creditCards = Array.isArray(response) ? response : (response.content || []);
        this.filterCreditCards();
        if (this.selectedCreditCard?.id) {
          const fresh = this.creditCards.find(c => c.id === this.selectedCreditCard.id);
          if (fresh) this.selectedCreditCard = fresh;
          this.loadCreditCardDetails(this.selectedCreditCard.id);
        }
      },
      error: () => {}
    });
  }

  goBack() {
    // Check if navigated from manager dashboard
    const navigationSource = sessionStorage.getItem('navigationSource');
    if (navigationSource === 'MANAGER') {
      const returnPath = sessionStorage.getItem('managerReturnPath') || '/manager/dashboard';
      this.router.navigate([returnPath]);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  loadCreditCards() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.isLoadingCreditCards = true;
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/all`, this.authOptions()).subscribe({
      next: (response: any) => {
        console.log('Credit cards response:', response);
        this.creditCards = Array.isArray(response) ? response : (response.content || []);
        this.filterCreditCards();
        this.isLoadingCreditCards = false;
      },
      error: (err: any) => {
        console.error('Error loading credit cards:', err);
        console.error('Error details:', {
          status: err.status,
          statusText: err.statusText,
          message: err.message,
          error: err.error
        });
        const errorMessage = err.error?.message || err.message || 'Failed to load credit cards. Please check the console for details.';
        this.alertService.error('Error', errorMessage);
        this.isLoadingCreditCards = false;
        // Set empty array on error to prevent UI issues
        this.creditCards = [];
        this.filteredCreditCards = [];
      }
    });
  }

  filterCreditCards() {
    let filtered = [...this.creditCards];
    
    // Filter by status
    if (this.creditCardStatusFilter !== 'ALL') {
      filtered = filtered.filter(card => 
        card.status?.toUpperCase() === this.creditCardStatusFilter.toUpperCase()
      );
    }
    
    // Filter by search query
    if (this.creditCardSearchQuery.trim()) {
      const query = this.creditCardSearchQuery.toLowerCase();
      filtered = filtered.filter(card =>
        card.userName?.toLowerCase().includes(query) ||
        card.accountNumber?.includes(query) ||
        card.cardNumber?.includes(query) ||
        card.maskedCardNumber?.includes(query)
      );
    }
    
    this.filteredCreditCards = filtered;
  }

  viewCreditCardDetails(card: any) {
    if (!card || !card.id) {
      console.error('Invalid card object:', card);
      this.alertService.error('Error', 'Invalid card selected');
      return;
    }
    this.hideRevealedCardNumber();
    this.selectedCreditCard = card;
    this.selectedTransaction = null;
    this.selectedCardServiceRequests = [];
    this.cardServiceRequestError = '';
    this.clearTransactionFilters();
    this.loadCreditCardDetails(card.id);
    this.loadCardServiceRequests(card.id);
    this.loadEmiPlans();
  }

  closeCreditCardDetails() {
    this.hideRevealedCardNumber();
    this.selectedCreditCard = null;
    this.selectedCardServiceRequests = [];
    this.cardServiceRequestError = '';
    this.isLoadingCardServiceRequests = false;
  }

  toggleCardNumberReveal() {
    const cardId = Number(this.selectedCreditCard?.id);
    if (!Number.isInteger(cardId) || cardId <= 0 || !this.canRevealCardNumber || this.isRevealingCardNumber) return;

    if (this.revealedCreditCardId === cardId && this.revealedCardNumber) {
      this.hideRevealedCardNumber();
      return;
    }

    this.isRevealingCardNumber = true;
    this.http.get<{ cardNumber: string }>(
      `${environment.apiBaseUrl}/api/credit-cards/${cardId}/admin/card-number`,
      this.authOptions()
    ).subscribe({
      next: response => {
        if (Number(this.selectedCreditCard?.id) !== cardId) return;
        if (!response?.cardNumber) {
          this.isRevealingCardNumber = false;
          this.alertService.operationError('Card number reveal', 'The server returned no card number');
          return;
        }
        this.revealedCreditCardId = cardId;
        this.revealedCardNumber = response.cardNumber;
        this.isRevealingCardNumber = false;
      },
      error: err => {
        if (Number(this.selectedCreditCard?.id) !== cardId) return;
        this.isRevealingCardNumber = false;
        this.alertService.operationError('Card number reveal', err.error?.message || 'Unable to reveal this card number');
      }
    });
  }

  hideRevealedCardNumber() {
    this.revealedCreditCardId = null;
    this.revealedCardNumber = '';
    this.isRevealingCardNumber = false;
  }

  openAccount360(accountNumber: string | null | undefined, event?: Event) {
    event?.stopPropagation();
    if (!accountNumber) {
      this.alertService.error('Account unavailable', 'No linked account number is available for this card.');
      return;
    }
    void this.router.navigate(['/admin/customer-360'], { state: { accountNumber } });
  }

  loadCreditCardDetails(cardId: string | number) {
    // Ensure cardId is a number for the API
    const numericCardId = typeof cardId === 'string' ? parseInt(cardId, 10) : cardId;
    
    if (isNaN(numericCardId)) {
      console.error('Invalid card ID:', cardId);
      return;
    }

    // Load transactions
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/transactions`, this.authOptions()).subscribe({
      next: (transactions: any) => {
        this.creditCardTransactions = Array.isArray(transactions) ? transactions : [];
        this.applyTransactionFilters();
      },
      error: (err: any) => {
        console.error('Error loading transactions:', err);
        this.creditCardTransactions = [];
        this.filteredTransactions = [];
        // Don't show error alert for transactions/bills as they're secondary data
      }
    });

    // Load bills
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/bills`, this.authOptions()).subscribe({
      next: (bills: any) => {
        this.creditCardBills = Array.isArray(bills) ? bills : [];
      },
      error: (err: any) => {
        console.error('Error loading bills:', err);
        this.creditCardBills = [];
        // Don't show error alert for transactions/bills as they're secondary data
      }
    });
  }

  maskAccountNumber(accountNumber: string | null | undefined): string {
    if (!accountNumber) return '—';
    return `****${accountNumber.slice(-4)}`;
  }

  maskCardNumber(cardNumber: string | null | undefined): string {
    if (!cardNumber) return '—';
    if (cardNumber.includes('*')) return cardNumber;
    return `**** **** **** ${cardNumber.replace(/\D/g, '').slice(-4)}`;
  }

  applyTransactionFilters() {
    const query = this.transactionSearchQuery.trim().toLowerCase();
    const startDate = this.transactionFilters.startDate ? new Date(`${this.transactionFilters.startDate}T00:00:00`) : null;
    const endDate = this.transactionFilters.endDate ? new Date(`${this.transactionFilters.endDate}T23:59:59.999`) : null;

    this.filteredTransactions = this.creditCardTransactions.filter(transaction => {
      const transactionDate = transaction.transactionDate ? new Date(transaction.transactionDate) : null;
      const amount = Number(transaction.amount);
      const safeSearchFields = [
        transaction.id,
        transaction.globalTransactionSequence,
        transaction.userName,
        transaction.accountNumber,
        transaction.customerReference,
        this.selectedCreditCard?.accountNumber,
        transaction.creditCardId,
        transaction.cardReference,
        transaction.cardNumber,
        transaction.merchant,
        transaction.debitAccountNumber,
        transaction.referenceNumber,
        transaction.chequeNumber
      ];

      if (query && !safeSearchFields.some(value => String(value ?? '').toLowerCase().includes(query))) return false;
      if (startDate && (!transactionDate || Number.isNaN(transactionDate.getTime()) || transactionDate < startDate)) return false;
      if (endDate && (!transactionDate || Number.isNaN(transactionDate.getTime()) || transactionDate > endDate)) return false;
      if (this.transactionFilters.type && transaction.transactionType !== this.transactionFilters.type) return false;
      if (this.transactionFilters.paymentMethod && transaction.paymentMethod !== this.transactionFilters.paymentMethod) return false;
      if (this.transactionFilters.status && String(transaction.status || '').toLowerCase() !== this.transactionFilters.status.toLowerCase()) return false;
      if (this.transactionFilters.merchant && !String(transaction.merchant || '').toLowerCase().includes(this.transactionFilters.merchant.toLowerCase())) return false;
      if (this.transactionFilters.minAmount !== null && (!Number.isFinite(amount) || amount < this.transactionFilters.minAmount)) return false;
      if (this.transactionFilters.maxAmount !== null && (!Number.isFinite(amount) || amount > this.transactionFilters.maxAmount)) return false;
      if (this.transactionFilters.category && transaction.category !== this.transactionFilters.category) return false;
      if (this.transactionFilters.processedBy && transaction.processedBy !== this.transactionFilters.processedBy) return false;
      if (this.transactionFilters.region) {
        const isInternational = transaction.isInternational === true || String(transaction.isInternational).toLowerCase() === 'true';
        if ((this.transactionFilters.region === 'INTERNATIONAL') !== isInternational) return false;
      }
      return true;
    });

    this.transactionPage = Math.min(this.transactionPage, this.transactionPageCount);
    if (this.transactionPage < 1) this.transactionPage = 1;
  }

  clearTransactionFilters() {
    this.transactionSearchQuery = '';
    this.transactionFilters = {
      startDate: '',
      endDate: '',
      type: '',
      paymentMethod: '',
      status: '',
      merchant: '',
      minAmount: null,
      maxAmount: null,
      category: '',
      processedBy: '',
      region: ''
    };
    this.transactionPage = 1;
    this.applyTransactionFilters();
  }

  get transactionPageCount(): number {
    return Math.max(1, Math.ceil(this.filteredTransactions.length / this.transactionPageSize));
  }

  get paginatedTransactions(): any[] {
    const start = (this.transactionPage - 1) * this.transactionPageSize;
    return this.filteredTransactions.slice(start, start + this.transactionPageSize);
  }

  get transactionPageStart(): number {
    return this.filteredTransactions.length ? (this.transactionPage - 1) * this.transactionPageSize + 1 : 0;
  }

  get transactionPageEnd(): number {
    return Math.min(this.transactionPage * this.transactionPageSize, this.filteredTransactions.length);
  }

  get successfulTransactionCount(): number {
    return this.creditCardTransactions.filter(transaction =>
      ['completed', 'successful', 'success'].includes(String(transaction.status || '').toLowerCase())
    ).length;
  }

  get todayTransactionCount(): number {
    const today = new Date().toDateString();
    return this.creditCardTransactions.filter(transaction =>
      transaction.transactionDate && new Date(transaction.transactionDate).toDateString() === today
    ).length;
  }

  get todaySuccessfulTransactionCount(): number {
    const today = new Date().toDateString();
    return this.creditCardTransactions.filter(transaction =>
      transaction.transactionDate &&
      new Date(transaction.transactionDate).toDateString() === today &&
      ['completed', 'successful', 'success'].includes(String(transaction.status || '').toLowerCase())
    ).length;
  }

  getTransactionStatusCount(status: string): number {
    return this.creditCardTransactions.filter(transaction =>
      String(transaction.status || '').toLowerCase() === status.toLowerCase()
    ).length;
  }

  getTransactionTypes(): string[] {
    return [...new Set(this.creditCardTransactions.map(transaction => transaction.transactionType).filter(Boolean))].sort();
  }

  getTransactionPaymentMethods(): string[] {
    return [...new Set(this.creditCardTransactions.map(transaction => transaction.paymentMethod).filter(Boolean))].sort();
  }

  getTransactionStatuses(): string[] {
    return [...new Set(this.creditCardTransactions.map(transaction => transaction.status).filter(Boolean))].sort();
  }

  getTransactionCategories(): string[] {
    return [...new Set(this.creditCardTransactions.map(transaction => transaction.category).filter(Boolean))].sort();
  }

  getProcessedByOptions(): string[] {
    return [...new Set(this.creditCardTransactions.map(transaction => transaction.processedBy).filter(Boolean))].sort();
  }

  hasInternationalTransactionData(): boolean {
    return this.creditCardTransactions.some(transaction => transaction.isInternational !== undefined && transaction.isInternational !== null);
  }

  transactionStatusClass(status: string): string {
    const normalized = String(status || '').toLowerCase();
    if (['completed', 'successful', 'success'].includes(normalized)) return 'successful';
    if (['pending', 'failed', 'reversed', 'refunded', 'disputed', 'blocked'].includes(normalized)) return normalized;
    return 'neutral';
  }

  viewTransactionDetails(transaction: any) {
    this.selectedTransaction = transaction;
  }

  exportTransactionsCsv() {
    const columns = [
      ['Transaction ID', (transaction: any) => transaction.globalTransactionSequence ?? transaction.id],
      ['Card Reference', (transaction: any) => transaction.cardReference ?? transaction.creditCardId ?? this.selectedCreditCard?.id],
      ['Masked Card Number', (transaction: any) => this.maskCardNumber(transaction.cardNumber || this.selectedCreditCard?.maskedCardNumber || this.selectedCreditCard?.cardNumber)],
      ['Date & Time', (transaction: any) => transaction.transactionDate],
      ['Type', (transaction: any) => transaction.transactionType],
      ['Merchant', (transaction: any) => transaction.merchant],
      ['Payment Method', (transaction: any) => transaction.paymentMethod],
      ['Amount', (transaction: any) => transaction.amount],
      ['Status', (transaction: any) => transaction.status],
      ['Processed By', (transaction: any) => transaction.processedBy],
      ['Reference Number', (transaction: any) => transaction.referenceNumber],
      ['Cheque Number', (transaction: any) => transaction.chequeNumber]
    ] as const;
    const escapeCell = (value: unknown) => {
      let text = String(value ?? '');
      if (/^[\s]*[=+\-@]/.test(text)) text = `'${text}`;
      return `"${text.replace(/"/g, '""')}"`;
    };
    const csv = [
      columns.map(([label]) => escapeCell(label)).join(','),
      ...this.filteredTransactions.map(transaction =>
        columns.map(([, value]) => escapeCell(value(transaction))).join(',')
      )
    ].join('\r\n');
    const url = URL.createObjectURL(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `credit-card-transactions-${this.selectedCreditCard?.id || 'filtered'}.csv`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  submitCreditCardApplication() {
    if (!this.applyCreditCardForm.accountNumber || !this.applyCreditCardForm.pan) {
      this.alertService.error('Validation Error', 'Account Number and PAN are required');
      return;
    }

    const payload = {
      accountNumber: this.applyCreditCardForm.accountNumber,
      userName: this.applyCreditCardForm.userName,
      userEmail: this.applyCreditCardForm.userEmail,
      pan: this.applyCreditCardForm.pan
    };

    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/create?income=${this.applyCreditCardForm.income || 0}`, payload).subscribe({
      next: (response: any) => {
        this.alertService.success('Success', 'Credit card application submitted successfully');
        this.showApplyCreditCardModal = false;
        this.applyCreditCardForm = {
          accountNumber: '',
          userName: '',
          userEmail: '',
          pan: '',
          income: 0
        };
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error submitting application:', err);
        const errorMessage = err.error?.message || err.message || 'Failed to submit application';
        this.alertService.error('Error', errorMessage);
      }
    });
  }

  updateCreditLimit() {
    if (!this.limitChangeAmount || this.limitChangeAmount <= 0) {
      this.alertService.error('Validation Error', 'Please enter a valid amount');
      return;
    }

    if (!this.selectedCreditCard) {
      this.alertService.error('Validation Error', 'No credit card selected');
      return;
    }

    // Calculate new limit based on action
    const currentLimit = this.selectedCreditCard.approvedLimit || 0;
    const newLimit = this.limitAction === 'increase' 
      ? currentLimit + this.limitChangeAmount 
      : currentLimit - this.limitChangeAmount;

    if (newLimit < 0) {
      this.alertService.error('Validation Error', 'New limit cannot be negative');
      return;
    }

    // Update the credit card with new limit
    const updateData: any = {
      approvedLimit: newLimit
    };

    this.http.put(
      `${environment.apiBaseUrl}/api/credit-cards/${this.selectedCreditCard.id}`,
      updateData,
      this.authOptions()
    ).subscribe({
      next: (updatedCard: any) => {
        // Save limit change history
        this.saveLimitChangeHistory(this.selectedCreditCard.id, {
          action: this.limitAction,
          oldLimit: currentLimit,
          newLimit: newLimit,
          changeAmount: this.limitChangeAmount,
          reason: this.limitChangeReason,
          adminName: this.adminName,
          timestamp: new Date().toISOString(),
          date: new Date().toLocaleDateString('en-IN'),
          time: new Date().toLocaleTimeString('en-IN')
        });
        
        this.alertService.success('Success', `Credit limit ${this.limitAction === 'increase' ? 'increased' : 'decreased'} successfully from ₹${currentLimit.toFixed(2)} to ₹${newLimit.toFixed(2)}`);
        this.showLimitModal = false;
        this.limitChangeAmount = 0;
        this.limitChangeReason = '';
        // Reload credit cards and update selected card
        this.loadCreditCards();
        // Update selected card with new data
        setTimeout(() => {
          if (this.selectedCreditCard) {
            this.selectedCreditCard.approvedLimit = updatedCard.approvedLimit;
            this.selectedCreditCard.availableLimit = updatedCard.availableLimit;
            this.selectedCreditCard.usageLimit = updatedCard.usageLimit;
            this.loadCreditCardDetails(this.selectedCreditCard.id);
          }
        }, 500);
      },
      error: (err: any) => {
        console.error('Error updating limit:', err);
        const errorMessage = err.error?.message || err.message || 'Failed to update credit limit';
        this.alertService.error('Error', errorMessage);
      }
    });
  }

  generateBill(cardId: string | number) {
    if (!confirm('Generate bill for this credit card?')) return;

    const numericCardId = typeof cardId === 'string' ? parseInt(cardId, 10) : cardId;
    if (isNaN(numericCardId)) {
      this.alertService.error('Error', 'Invalid card ID');
      return;
    }

    this.http.post(`${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/generate-bill`, {}).subscribe({
      next: (response: any) => {
        this.alertService.success('Success', 'Bill generated successfully');
        this.loadCreditCardDetails(numericCardId);
      },
      error: (err: any) => {
        console.error('Error generating bill:', err);
        const errorMessage = err.error?.message || err.message || 'Failed to generate bill';
        this.alertService.error('Error', errorMessage);
      }
    });
  }

  getStatement(cardId: string | number) {
    const numericCardId = typeof cardId === 'string' ? parseInt(cardId, 10) : cardId;
    if (isNaN(numericCardId)) {
      this.alertService.error('Error', 'Invalid card ID');
      return;
    }

    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    // Format dates properly for backend
    const startDateStr = startDate.toISOString().split('T')[0] + 'T00:00:00';
    const endDateStr = endDate.toISOString().split('T')[0] + 'T23:59:59';
    
    this.http.get(
      `${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/statement?startDate=${startDateStr}&endDate=${endDateStr}`,
      this.authOptions()
    ).subscribe({
      next: (transactions: any) => {
        // Display statement - you can create a modal or download as PDF
        console.log('Statement:', transactions);
        const transactionList = Array.isArray(transactions) ? transactions : [];
        this.alertService.success('Success', `Statement generated with ${transactionList.length} transactions`);
      },
      error: (err: any) => {
        console.error('Error getting statement:', err);
        const errorMessage = err.error?.message || err.message || 'Failed to get statement';
        this.alertService.error('Error', errorMessage);
      }
    });
  }

  openBillPayment(bill: any) {
    this.selectedBillForPayment = bill;
    const outstanding = Math.max(0, (bill.totalAmount || 0) + (bill.fine || 0) + (bill.penalty || 0) - (bill.paidAmount || 0));
    this.billPaymentForm = {
      amount: outstanding,
      paymentMethod: 'ACCOUNT',
      debitAccountNumber: this.selectedCreditCard?.accountNumber || '',
      chequeNumber: ''
    };
    this.chequeVerification = null;
    this.showBillPaymentModal = true;
    this.loadPaymentAccountDetails();
  }

  verifyChequeForPayment() {
    const chequeNumber = this.billPaymentForm.chequeNumber.trim();
    const debitAccountNumber = (this.billPaymentForm.debitAccountNumber || this.selectedCreditCard?.accountNumber || '').trim();
    this.chequeVerification = null;
    if (!chequeNumber || !debitAccountNumber) return;
    this.isVerifyingCheque = true;
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/verify-cheque?chequeNumber=${encodeURIComponent(chequeNumber)}&debitAccountNumber=${encodeURIComponent(debitAccountNumber)}`).subscribe({
      next: (res: any) => { this.chequeVerification = res; this.isVerifyingCheque = false; },
      error: () => { this.chequeVerification = { valid: false, message: 'Unable to verify cheque' }; this.isVerifyingCheque = false; }
    });
  }

  onChequeImageSelected(event: any) {
    const file = event.target.files?.[0] as File | undefined;
    if (!file) return;
    if (!['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'].includes(file.type)) {
      this.alertService.error('Invalid File', 'Upload a JPG, PNG, or PDF cheque image');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.alertService.error('Invalid File', 'Cheque image must be smaller than 5MB');
      return;
    }
    this.chequeImageFile = file;
    this.chequeImagePreviewName = file.name;
  }

  loadPaymentAccountDetails() {
    const accountNumber = this.billPaymentForm.debitAccountNumber.trim();
    this.paymentAccountDetails = null;
    if (!accountNumber || this.billPaymentForm.paymentMethod === 'CASH') return;
    this.http.get(`${environment.apiBaseUrl}/api/accounts/number/${encodeURIComponent(accountNumber)}`).subscribe({
      next: (account: any) => this.paymentAccountDetails = account,
      error: () => this.paymentAccountDetails = null
    });
  }

  downloadChequeImage(transaction: any) {
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/transactions/${transaction.id}/cheque-image`, { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = transaction.chequeImageName || `cheque-${transaction.chequeNumber || transaction.id}`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.alertService.error('Download Failed', 'Cheque image is not available')
    });
  }

  payBillAsAdmin() {
    const bill = this.selectedBillForPayment;
    const form = this.billPaymentForm;
    if (!bill || !form.amount || form.amount <= 0) {
      this.alertService.error('Validation Error', 'Enter a valid payment amount');
      return;
    }
    if (form.paymentMethod === 'CHEQUE') {
      if (!form.chequeNumber.trim()) {
        this.alertService.error('Validation Error', 'Cheque payment number is required');
        return;
      }
      if (!this.chequeVerification || !this.chequeVerification.valid) {
        this.alertService.error('Cheque Not Verified', 'Verify the cheque and ensure it is valid and unused before proceeding');
        return;
      }
    }

    this.isPayingBill = true;
    const adminData = isPlatformBrowser(this.platformId) ? sessionStorage.getItem('adminData') : null;
    const admin = adminData ? JSON.parse(adminData) : {};
    const submitPayment = (chequeImageBase64: string | null) => this.http.post(`${environment.apiBaseUrl}/api/credit-cards/bills/${bill.id}/admin-pay`, {
      amount: form.amount, paymentMethod: form.paymentMethod, debitAccountNumber: form.debitAccountNumber,
      chequeNumber: form.chequeNumber || null, chequeImageBase64, chequeImageName: this.chequeImageFile?.name || null,
      chequeImageType: this.chequeImageFile?.type || null, adminName: admin.name || admin.username || this.adminName
    }).subscribe({
      next: (response: any) => {
        this.alertService.success('Payment Successful', `Bill paid by ${form.paymentMethod}. Account balance and history updated.`);
        this.showBillPaymentModal = false;
        this.isPayingBill = false;
        this.chequeImageFile = null;
        this.chequeImagePreviewName = '';
        if (this.selectedCreditCard) {
          this.selectedCreditCard = response.card;
          this.loadCreditCardDetails(this.selectedCreditCard.id);
        }
        this.loadCreditCards();
      },
      error: (err: any) => {
        this.isPayingBill = false;
        this.alertService.error('Payment Failed', err.error?.message || 'Unable to process bill payment');
      }
    });
    if (!this.chequeImageFile) {
      if (form.paymentMethod === 'CHEQUE') {
        this.isPayingBill = false;
        this.alertService.error('Cheque Image Required', 'Upload the cheque image before confirming payment');
        return;
      }
      submitPayment(null);
      return;
    }
    const reader = new FileReader();
    reader.onload = () => submitPayment(reader.result as string);
    reader.onerror = () => { this.isPayingBill = false; this.alertService.error('File Error', 'Unable to read cheque image'); };
    reader.readAsDataURL(this.chequeImageFile);
  }

  closeCreditCard(cardId: string | number) {
    if (!this.selectedCreditCard) {
      this.alertService.error('Error', 'No credit card selected');
      return;
    }

    // Validate balance before closing
    const currentBalance = this.selectedCreditCard.currentBalance || 0;
    const overdueAmount = this.selectedCreditCard.overdueAmount || 0;
    const totalBalance = currentBalance + overdueAmount;

    // Check if balance is exactly 0
    if (totalBalance !== 0) {
      let errorMessage = '';
      if (totalBalance > 0) {
        errorMessage = `Cannot close credit card. Current balance is ₹${currentBalance.toFixed(2)}${overdueAmount > 0 ? ` and overdue amount is ₹${overdueAmount.toFixed(2)}` : ''}. Please clear all dues before closing.`;
      } else {
        errorMessage = `Cannot close credit card. There are outstanding dues of ₹${Math.abs(totalBalance).toFixed(2)}. Please clear all dues before closing.`;
      }
      this.alertService.error('Cannot Close Card', errorMessage);
      return;
    }

    // Check if there are any pending bills
    if (this.creditCardBills && this.creditCardBills.length > 0) {
      const pendingBills = this.creditCardBills.filter((bill: any) => 
        bill.status?.toLowerCase() !== 'paid' && bill.status?.toLowerCase() !== 'completed'
      );
      if (pendingBills.length > 0) {
        this.alertService.error('Cannot Close Card', `Cannot close credit card. There are ${pendingBills.length} pending bill(s). Please clear all bills before closing.`);
        return;
      }
    }

    // Show close card modal with document upload
    this.showCloseCardModal = true;
    this.closeCardDocument = null;
    this.closeCardDocumentName = '';
  }

  onDocumentSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validate file type (PDF, images)
      const validTypes = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png'];
      if (!validTypes.includes(file.type)) {
        this.alertService.error('Invalid File', 'Please upload a PDF or image file (JPEG, PNG)');
        return;
      }
      
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.alertService.error('File Too Large', 'File size must be less than 5MB');
        return;
      }
      
      this.closeCardDocument = file;
      this.closeCardDocumentName = file.name;
    }
  }

  confirmCloseCard() {
    if (!this.closeCardDocument) {
      this.alertService.error('Document Required', 'Please upload a signed document before closing the credit card');
      return;
    }

    if (!confirm('Are you sure you want to close this credit card? This action cannot be undone.')) return;

    const numericCardId = typeof this.selectedCreditCard.id === 'string' 
      ? parseInt(this.selectedCreditCard.id, 10) 
      : this.selectedCreditCard.id;
    
    if (isNaN(numericCardId)) {
      this.alertService.error('Error', 'Invalid card ID');
      return;
    }

    this.isUploadingDocument = true;

    // Convert file to base64 for storage
    const reader = new FileReader();
    reader.onload = () => {
      const base64Data = reader.result as string;
      
      // Save document to localStorage first
      const documentData = {
        documentData: base64Data,
        documentName: this.closeCardDocumentName,
        documentType: this.closeCardDocument!.type,
        closedBy: this.adminName,
        closedDate: new Date().toISOString(),
        cardNumber: this.maskCardNumber(this.selectedCreditCard?.maskedCardNumber || this.selectedCreditCard?.cardNumber)
      };
      
      this.saveCloseCardDocument(numericCardId, documentData);
      
      // Then close the card
      this.http.put(
        `${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/close`,
        {},
        this.authOptions()
      ).subscribe({
        next: (response: any) => {
          this.alertService.success('Success', 'Credit card closed successfully with document uploaded');
          this.showCloseCardModal = false;
          this.closeCardDocument = null;
          this.closeCardDocumentName = '';
          this.isUploadingDocument = false;
          this.hideRevealedCardNumber();
          this.selectedCreditCard = null;
          this.loadCreditCards();
        },
        error: (err: any) => {
          console.error('Error closing card:', err);
          this.isUploadingDocument = false;
          const errorMessage = err.error?.message || err.message || 'Failed to close credit card';
          this.alertService.error('Error', errorMessage);
        }
      });
    };
    
    reader.onerror = () => {
      this.isUploadingDocument = false;
      this.alertService.error('Error', 'Failed to read document file');
    };
    
    reader.readAsDataURL(this.closeCardDocument!);
  }

  cancelCloseCard() {
    this.showCloseCardModal = false;
    this.closeCardDocument = null;
    this.closeCardDocumentName = '';
  }

  // Helper method to check if card can be closed
  canCloseCard(card: any): boolean {
    if (!card || card.status === 'Closed') return false;
    
    // Check balance - must be exactly 0 (no positive or negative balance)
    const currentBalance = parseFloat(card.currentBalance) || 0;
    const overdueAmount = parseFloat(card.overdueAmount) || 0;
    const totalBalance = currentBalance + overdueAmount;
    
    // Balance must be exactly 0 (allowing for small floating point differences)
    return Math.abs(totalBalance) < 0.01; // Allow for floating point precision
  }

  // Save limit change history
  saveLimitChangeHistory(cardId: string | number, historyData: any) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const historyKey = `creditCardLimitHistory_${cardId}`;
    const existingHistory = localStorage.getItem(historyKey);
    const history = existingHistory ? JSON.parse(existingHistory) : [];
    
    const historyEntry = {
      id: `LH${Date.now()}`,
      cardId: cardId,
      ...historyData
    };
    
    history.unshift(historyEntry);
    
    // Keep only last 100 entries per card
    if (history.length > 100) {
      history.splice(100);
    }
    
    localStorage.setItem(historyKey, JSON.stringify(history));
    console.log('Limit change history saved:', historyEntry);
  }

  // Load limit change history
  loadLimitChangeHistory(cardId: string | number) {
    if (!isPlatformBrowser(this.platformId)) return [];
    
    const historyKey = `creditCardLimitHistory_${cardId}`;
    const existingHistory = localStorage.getItem(historyKey);
    return existingHistory ? JSON.parse(existingHistory) : [];
  }

  // Save close card document info
  saveCloseCardDocument(cardId: string | number, documentData: any) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const docKey = `creditCardCloseDocument_${cardId}`;
    localStorage.setItem(docKey, JSON.stringify(documentData));
    console.log('Close card document saved:', documentData);
  }

  // Get close card document info
  getCloseCardDocument(cardId: string | number): any {
    if (!isPlatformBrowser(this.platformId)) return null;
    
    const docKey = `creditCardCloseDocument_${cardId}`;
    const docData = localStorage.getItem(docKey);
    return docData ? JSON.parse(docData) : null;
  }

  // Download close card document
  downloadCloseCardDocument(cardId: string | number) {
    const docData = this.getCloseCardDocument(cardId);
    if (!docData || (!docData.documentData && !docData.documentUrl)) {
      this.alertService.error('Error', 'Document not found for this credit card');
      return;
    }

    try {
      let dataUrl = '';
      let fileName = docData.documentName || `credit-card-close-document-${cardId}`;
      
      // If document is stored as base64 data
      if (docData.documentData) {
        dataUrl = docData.documentData;
        // Add file extension based on document type
        if (docData.documentType) {
          const ext = docData.documentType.includes('pdf') ? '.pdf' : 
                     docData.documentType.includes('jpeg') || docData.documentType.includes('jpg') ? '.jpg' : 
                     docData.documentType.includes('png') ? '.png' : '.pdf';
          if (!fileName.includes('.')) {
            fileName += ext;
          }
        }
      } else if (docData.documentUrl) {
        // If it's a URL (for future backend integration)
        if (docData.documentUrl.startsWith('data:') || docData.documentUrl.startsWith('blob:')) {
          dataUrl = docData.documentUrl;
        } else {
          // Fetch from server
          this.http.get(docData.documentUrl, { responseType: 'blob' }).subscribe({
            next: (blob: Blob) => {
              const url = window.URL.createObjectURL(blob);
              const link = document.createElement('a');
              link.href = url;
              link.download = fileName;
              link.click();
              window.URL.revokeObjectURL(url);
              this.alertService.success('Success', 'Document downloaded successfully');
            },
            error: (err: any) => {
              console.error('Error downloading document:', err);
              this.alertService.error('Error', 'Failed to download document');
            }
          });
          return;
        }
      }
      
      // Download the file
      const link = document.createElement('a');
      link.href = dataUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      this.alertService.success('Success', 'Document downloaded successfully');
    } catch (error) {
      console.error('Error downloading document:', error);
      this.alertService.error('Error', 'Failed to download document');
    }
  }

  // Show limit change history
  showLimitHistory(cardId: string | number) {
    this.limitChangeHistory = this.loadLimitChangeHistory(cardId);
    this.showHistoryModal = true;
  }

  // Get close card reason/status
  getCloseCardStatus(card: any): { canClose: boolean; message: string } {
    if (!card) {
      return { canClose: false, message: 'Invalid card' };
    }
    
    if (card.status === 'Closed') {
      return { canClose: false, message: 'Card is already closed' };
    }
    
    const currentBalance = parseFloat(card.currentBalance) || 0;
    const overdueAmount = parseFloat(card.overdueAmount) || 0;
    const totalBalance = currentBalance + overdueAmount;

    // Check if balance is exactly 0 (allowing for floating point precision)
    if (Math.abs(totalBalance) < 0.01) {
      return { canClose: true, message: '✓ Card can be closed - All balances cleared' };
    } else if (totalBalance > 0) {
      return { 
        canClose: false, 
        message: `✗ Cannot close: Outstanding balance of ₹${totalBalance.toFixed(2)} must be cleared first` 
      };
    } else {
      return { 
        canClose: false, 
        message: `✗ Cannot close: Outstanding dues of ₹${Math.abs(totalBalance).toFixed(2)} must be cleared first` 
      };
    }
  }

  // ─── Transfer credit card amount to any account ───────────────────────────

  openTransferModal() {
    if (!this.selectedCreditCard) return;
    this.transferForm = { destinationAccountNumber: '', amount: 0 };
    this.transferAccountDetails = null;
    this.showTransferModal = true;
  }

  verifyTransferAccount() {
    const accountNumber = this.transferForm.destinationAccountNumber.trim();
    this.transferAccountDetails = null;
    if (!accountNumber) return;
    this.isVerifyingTransferAccount = true;
    // Reuses the current-account service's unified account lookup (checks savings/current/salary)
    this.http.get(`${environment.apiBaseUrl}/api/current-accounts/verify-recipient/${encodeURIComponent(accountNumber)}`).subscribe({
      next: (res: any) => { this.transferAccountDetails = res; this.isVerifyingTransferAccount = false; },
      error: () => { this.transferAccountDetails = { found: false, message: 'Unable to verify account' }; this.isVerifyingTransferAccount = false; }
    });
  }

  confirmTransferToAccount() {
    if (!this.selectedCreditCard) return;
    const form = this.transferForm;
    if (!form.destinationAccountNumber.trim() || !form.amount || form.amount <= 0) {
      this.alertService.error('Validation Error', 'Enter a destination account number/UPI ID and a valid amount');
      return;
    }
    if (!this.transferAccountDetails || !this.transferAccountDetails.found) {
      this.alertService.error('Account Not Verified', 'Verify the destination account before transferring');
      return;
    }
    this.isTransferring = true;
    const adminData = isPlatformBrowser(this.platformId) ? sessionStorage.getItem('adminData') : null;
    const admin = adminData ? JSON.parse(adminData) : {};
    this.http.post(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCreditCard.id}/transfer-to-account`, {
      destinationAccountNumber: form.destinationAccountNumber.trim(),
      amount: form.amount,
      adminName: admin.name || admin.username || this.adminName
    }).subscribe({
      next: (res: any) => {
        this.isTransferring = false;
        this.alertService.success('Transfer Successful', `₹${form.amount} transferred. Fee ₹${res.fee?.toFixed?.(2) || res.fee} (${res.feePercent}%) credited to branch account. ₹${res.creditedAmount?.toFixed?.(2) || res.creditedAmount} credited to destination.`);
        this.showTransferModal = false;
        this.selectedCreditCard = res.card;
        this.loadCreditCards();
        this.loadCreditCardDetails(this.selectedCreditCard.id);
      },
      error: (err: any) => {
        this.isTransferring = false;
        this.alertService.error('Transfer Failed', err.error?.message || 'Unable to complete transfer');
      }
    });
  }

  // ─── Admin-configurable transfer fee percentage ───────────────────────────

  loadTransferFeePercent() {
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/transfer-fee-percent`).subscribe({
      next: (res: any) => { this.transferFeePercent = res.transferFeePercent ?? 2; },
      error: () => {}
    });
  }

  loadEmiSettings() {
    this.http.get<any>(`${environment.apiBaseUrl}/api/credit-cards/emi-settings`).subscribe({
      next: settings => {
        this.emiAnnualInterestPercent = settings.annualInterestPercent ?? null;
        this.emiProcessingFeePercent = settings.processingFeePercent ?? 8;
      },
      error: err => console.error('Error loading credit-card EMI settings:', err)
    });
  }

  saveEmiSettings() {
    if (this.emiAnnualInterestPercent == null || this.emiAnnualInterestPercent <= 0
        || this.emiAnnualInterestPercent > 100) {
      this.alertService.error('Validation Error', 'Set an annual EMI interest rate between 0 and 100 percent');
      return;
    }
    if (this.emiProcessingFeePercent < 8 || this.emiProcessingFeePercent > 100) {
      this.alertService.error('Validation Error', 'The EMI processing fee must be between 8 and 100 percent');
      return;
    }
    this.isSavingEmiSettings = true;
    const adminData = isPlatformBrowser(this.platformId) ? sessionStorage.getItem('adminData') : null;
    const admin = adminData ? JSON.parse(adminData) : {};
    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/emi-settings`, {
      annualInterestPercent: this.emiAnnualInterestPercent,
      processingFeePercent: this.emiProcessingFeePercent,
      adminName: admin.name || admin.username || this.adminName
    }).subscribe({
      next: () => {
        this.isSavingEmiSettings = false;
        this.alertService.success('Saved', 'EMI rates updated for new conversions. Existing plans are unchanged.');
      },
      error: err => {
        this.isSavingEmiSettings = false;
        this.alertService.error('Error', err.error?.message || 'Unable to save EMI rates');
      }
    });
  }

  loadCreditLimitIncreaseCeiling() {
    this.http.get<any>(`${environment.apiBaseUrl}/api/credit-cards/credit-limit-increase-ceiling`, this.authOptions()).subscribe({
      next: response => this.creditLimitIncreaseCeiling = Number(response.ceiling || 0),
      error: err => {
        console.error('Error loading credit-limit increase ceiling:', err);
        this.alertService.operationError('Credit-limit configuration', 'Unable to load the configured request ceiling');
      }
    });
  }

  saveCreditLimitIncreaseCeiling() {
    if (!Number.isFinite(this.creditLimitIncreaseCeiling) || this.creditLimitIncreaseCeiling <= 0) {
      this.alertService.error('Validation Error', 'Set a credit-limit increase ceiling greater than zero');
      return;
    }
    this.isSavingCreditLimitCeiling = true;
    this.http.put<any>(
      `${environment.apiBaseUrl}/api/credit-cards/credit-limit-increase-ceiling`,
      { ceiling: this.creditLimitIncreaseCeiling },
      this.authOptions()
    ).subscribe({
      next: response => {
        this.creditLimitIncreaseCeiling = Number(response.ceiling);
        this.isSavingCreditLimitCeiling = false;
        this.alertService.success('Saved', 'The customer credit-limit request ceiling was updated.');
      },
      error: err => {
        this.isSavingCreditLimitCeiling = false;
        this.alertService.operationError('Credit-limit configuration', err.error?.message || 'Unable to save the request ceiling');
      }
    });
  }

  loadCardServiceRequests(cardId: string | number, silent = false) {
    const numericCardId = Number(cardId);
    if (!Number.isInteger(numericCardId) || numericCardId <= 0) {
      this.cardServiceRequestError = 'Invalid card selected for loading requests.';
      return;
    }

    this.isLoadingCardServiceRequests = true;
    this.cardServiceRequestError = '';
    this.http.get<any[]>(
      `${environment.apiBaseUrl}/api/credit-card-requests/card/${numericCardId}`,
      this.authOptions()
    ).subscribe({
      next: response => {
        if (Number(this.selectedCreditCard?.id) !== numericCardId) return;
        this.selectedCardServiceRequests = (Array.isArray(response) ? response : []).map(request => ({
          ...request,
          nextStatus: request.status,
          responseDraft: ''
        }));
        this.isLoadingCardServiceRequests = false;
      },
      error: err => {
        if (Number(this.selectedCreditCard?.id) !== numericCardId) return;
        this.isLoadingCardServiceRequests = false;
        this.cardServiceRequestError = err.error?.message || 'Unable to load requests for this card.';
        console.error('Error loading card service requests:', err);
        if (!silent) this.alertService.operationError('Card requests', this.cardServiceRequestError);
      }
    });
  }

  updateCustomerServiceRequest(request: any, status: string) {
    const resolution = String(request.responseDraft || '').trim();
    if (['Approved', 'Resolved', 'Rejected', 'Closed'].includes(status) && resolution.length < 5) {
      this.alertService.error('Resolution required', 'Enter at least 5 characters describing the decision.');
      return;
    }
    if (!confirm(`Change ${request.requestNumber} to ${status}?`)) return;
    this.http.put(
      `${environment.apiBaseUrl}/api/credit-card-requests/${request.id}/status`,
      { status, resolution },
      this.authOptions()
    ).subscribe({
      next: () => {
        this.alertService.success('Request updated', `${request.requestNumber} is now ${status.toLowerCase()}.`);
        request.responseDraft = '';
        if (this.selectedCreditCard?.id) this.loadCardServiceRequests(this.selectedCreditCard.id, true);
        if (request.requestType === 'Credit Limit Increase' && status === 'Approved') this.loadCreditCards();
      },
      error: err => this.alertService.operationError('Request update', err.error?.message || 'Unable to update this request')
    });
  }

  replyToCustomerServiceRequest(request: any) {
    const message = String(request.responseDraft || '').trim();
    if (message.length < 5) {
      this.alertService.error('Reply required', 'Enter at least 5 characters for the customer response.');
      return;
    }
    this.http.put(
      `${environment.apiBaseUrl}/api/credit-card-requests/${request.id}/reply`,
      { message },
      this.authOptions()
    ).subscribe({
      next: () => {
        request.responseDraft = '';
        this.alertService.success('Response sent', `A response was added to ${request.requestNumber}.`);
        if (this.selectedCreditCard?.id) this.loadCardServiceRequests(this.selectedCreditCard.id, true);
      },
      error: err => this.alertService.operationError('Customer response', err.error?.message || 'Unable to send this response')
    });
  }

  getCardServiceRequestActions(request: any): string[] {
    const requestType = String(request.requestType || '').toLowerCase();
    const requestText = `${requestType} ${request.category || ''} ${request.description || ''}`.toLowerCase();
    const actions: string[] = [];

    if (/\b(block|lock|freeze|lost|stolen|fraud|unauthori[sz]ed)\b/.test(requestText)) {
      actions.push('BLOCK_CARD');
    }
    if (requestType.includes('replacement')) actions.push('REVIEW_REPLACEMENT');
    if (requestType.includes('statement')) actions.push('VIEW_STATEMENT');
    if (requestType.includes('billing') || /\b(due|overdue|bill|payment)\b/.test(requestText)) {
      actions.push('VIEW_DUES');
    }
    if (/\b(details?|information|summary)\b/.test(requestText)) actions.push('VIEW_DETAILS');
    if (requestType.includes('dispute') || request.transactionId
      || /\b(analy[sz]e|review transaction|charge|merchant)\b/.test(requestText)) {
      actions.push('ANALYZE');
    }

    return [...new Set(actions)];
  }

  runCardServiceRequestAction(request: any, action: string) {
    const cardId = Number(this.selectedCreditCard?.id);
    if (!Number.isInteger(cardId) || cardId <= 0) return;

    if (action === 'BLOCK_CARD') {
      if (String(this.selectedCreditCard?.status || '').toLowerCase() === 'blocked') {
        this.alertService.info('Card already blocked', 'This card is already blocked.');
        return;
      }
      if (!confirm(`Block card ${this.maskCardNumber(this.selectedCreditCard?.maskedCardNumber || this.selectedCreditCard?.cardNumber)} and disable all transaction channels?`)) return;
      this.http.post(
        `${environment.apiBaseUrl}/api/credit-cards/${cardId}/emergency-lock`,
        {},
        this.authOptions()
      ).subscribe({
        next: () => {
          this.selectedCreditCard.status = 'Blocked';
          this.selectedCreditCard.blocked = true;
          this.alertService.success('Card blocked', 'The card was blocked and its transaction channels were disabled.');
          this.loadCreditCards();
        },
        error: err => this.alertService.operationError('Block card', err.error?.message || 'Unable to block this card')
      });
      return;
    }

    if (action === 'REVIEW_REPLACEMENT') {
      this.updateCustomerServiceRequest(request, 'Under Review');
      return;
    }
    if (action === 'VIEW_STATEMENT') {
      this.getStatement(cardId);
      return;
    }
    if (action === 'VIEW_DUES') {
      this.scrollToCardDetailSection('credit-card-bills');
      return;
    }
    if (action === 'VIEW_DETAILS') {
      this.scrollToCardDetailSection('credit-card-information');
      return;
    }
    if (action === 'ANALYZE') {
      this.transactionSearchQuery = request.transactionId ? String(request.transactionId) : '';
      this.applyTransactionFilters();
      this.scrollToCardDetailSection('credit-card-transactions');
    }
  }

  private scrollToCardDetailSection(sectionId: string) {
    setTimeout(() => document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  loadEmiPlans() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/credit-cards/emi-plans`).subscribe({
      next: plans => {
        const cardId = this.selectedCreditCard?.id;
        this.creditCardEmiPlans = (Array.isArray(plans) ? plans : [])
          .filter(plan => plan.creditCardId === cardId);
      },
      error: err => {
        console.error('Error loading EMI plans for admin:', err);
        this.creditCardEmiPlans = [];
      }
    });
  }

  saveTransferFeePercent() {
    if (this.transferFeePercent == null || this.transferFeePercent < 0 || this.transferFeePercent > 100) {
      this.alertService.error('Validation Error', 'Fee percent must be between 0 and 100');
      return;
    }
    this.isSavingTransferFee = true;
    const adminData = isPlatformBrowser(this.platformId) ? sessionStorage.getItem('adminData') : null;
    const admin = adminData ? JSON.parse(adminData) : {};
    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/transfer-fee-percent`, {
      transferFeePercent: this.transferFeePercent,
      adminName: admin.name || admin.username || this.adminName
    }).subscribe({
      next: () => { this.isSavingTransferFee = false; this.alertService.success('Saved', 'Transfer fee percentage updated'); },
      error: (err: any) => { this.isSavingTransferFee = false; this.alertService.error('Error', err.error?.message || 'Failed to update fee'); }
    });
  }
}
