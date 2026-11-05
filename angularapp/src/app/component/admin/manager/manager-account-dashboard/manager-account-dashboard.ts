import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { AllocationAccountService } from '../../../../service/allocation-account.service';
import { ChargeManagementService } from '../../../../service/charge-management.service';

@Component({
  selector: 'app-manager-account-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, HttpClientModule],
  templateUrl: './manager-account-dashboard.html',
  styleUrls: ['./manager-account-dashboard.css']
})
export class ManagerAccountDashboardComponent implements OnInit, OnDestroy {
  
  allocationId: number = 0;
  linkedAccount: any = null;
  accountTransactions: any[] = [];
  chargesSummary: any = {};
  
  loading = false;
  errorMessage = '';
  
  activeTab = 'overview';
  activeSection = 'overview';
  transactionAccountFilter = '';
  transactionProductFilter = 'ALL';
  transactionPage = 0;
  transactionSize = 10;
  isAllocationAccountLogin = false;
  dashboardSummary: any = {};
  productBreakdown: any[] = [];
  dashboardCharges: any[] = [];
  selectedAllocationCharge: any = null;
  chargePage = 0;
  chargeSize = 50;
  chargeTotalPages = 0;
  chargeTotalElements = 0;
  chargeAccountFilter = '';
  chargeCategoryFilter = 'ALL';
  allocationLoans: any[] = [];
  allocationEmiPayments: any[] = [];
  allocationEcsMandates: any[] = [];
  allocationAccountDeposits: any[] = [];
  allocationPortfolio: any = {};
  allocationProfitability: any = {};
  loanSearch = '';
  loanTypeFilter = 'ALL';
  mandateSearch = '';
  accountDepositSearch = '';
  readonly chargeCategories = [
    { value: 'CIBIL', label: 'CIBIL' },
    { value: 'PROCESSING', label: 'Processing fee' },
    { value: 'SMS', label: 'SMS' },
    { value: 'MAINTENANCE', label: 'Account maintenance' },
    { value: 'CHEQUE', label: 'Cheque (₹199)' },
    { value: 'IMPS', label: 'IMPS' },
    { value: 'ATM', label: 'ATM' },
    { value: 'CREDIT_CARD_PROCESSING_FEE', label: 'Credit card processing fee' },
    { value: 'GOLD_LOAN_INTEREST', label: 'Gold loan interest' },
    { value: 'EMI', label: 'EMI' },
    { value: 'INTEREST', label: 'Interest' },
    { value: 'EMI_BOUNCE', label: 'EMI bounce' },
    { value: 'OTHER', label: 'Other' }
  ];
  readonly allocationProductCategories = [
    { label: 'Admin deposits', keys: ['ADMIN_DEPOSIT', 'ADMIN_DEPOSITS', 'DEPOSIT', 'DEPOSITS'] },
    { label: 'Admin withdrawals', keys: ['ADMIN_WITHDRAWAL', 'ADMIN_WITHDRAWALS', 'WITHDRAWAL', 'WITHDRAWALS'] },
    { label: 'Loans sanctioned', keys: ['LOAN', 'LOANS', 'SANCTIONED_LOAN', 'LOAN_SANCTION', 'PERSONAL_LOAN', 'GOLD_LOAN'] },
    { label: 'Car loans', keys: ['CAR_LOAN', 'CAR_LOANS', 'AUTO_LOAN', 'VEHICLE_LOAN'] },
    { label: 'Home loans', keys: ['HOME_LOAN', 'HOME_LOANS', 'HOUSING_LOAN'] },
    { label: 'Crop loans', keys: ['CROP_LOAN', 'CROP_LOANS', 'AGRICULTURE_LOAN', 'FARM_LOAN'] },
    { label: 'Subsidies', keys: ['SUBSIDY', 'SUBSIDIES', 'CROP_SUBSIDY', 'EDUCATION_SUBSIDY'] },
    { label: 'Education loans', keys: ['EDUCATION_LOAN', 'EDUCATION_LOANS', 'STUDENT_LOAN'] },
    { label: 'Credit card loans', keys: ['CREDIT_CARD_LOAN', 'CREDIT_CARD_LOANS', 'CREDIT_CARD'] },
    { label: 'FD deposits', keys: ['FD', 'FD_DEPOSIT', 'FIXED_DEPOSIT'] },
    { label: 'RD deposits', keys: ['RD', 'RD_DEPOSIT', 'RECURRING_DEPOSIT'] }
  ];
  transactionTotalPages = 0;
  lastRefreshedAt: Date | null = null;
  isRefreshing = false;
  selectedTransaction: any = null;
  selectedCharge: any = null;
  operationRequests: any[] = [];
  requestType = 'TOP_UP';
  requestAmount: number | null = null;
  requestDestinationAccount = '';
  requestDestinationName = '';
  requestReference = '';
  requestDescription = '';
  requestSuccess = '';
  requestError = '';
  isSubmittingRequest = false;
  private allocationRefreshTimer?: ReturnType<typeof setInterval>;

  constructor(
    private accountService: AllocationAccountService,
    private chargeService: ChargeManagementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAllocationAccountLogin = sessionStorage.getItem('userRole') === 'ALLOCATION_ACCOUNT';
    if (this.isAllocationAccountLogin) {
      const session = JSON.parse(sessionStorage.getItem('allocationAccount') || '{}');
      this.allocationId = Number(session.allocationRecordId) || 0;
      if (!sessionStorage.getItem('allocationAccountSessionToken') || !this.allocationId) {
        this.logoutAllocationAccount();
        return;
      }
    }
    this.loadAccountData();
    if (this.isAllocationAccountLogin) {
      this.allocationRefreshTimer = setInterval(() => {
        if (typeof document === 'undefined' || document.visibilityState === 'visible') {
          this.loadAllocationDashboard(true);
        }
      }, 5000);
    }
  }

  ngOnDestroy(): void {
    if (this.allocationRefreshTimer) clearInterval(this.allocationRefreshTimer);
  }

  /**
   * Load linked account details
   */
  loadAccountData(): void {
    if (!this.allocationId) return;
    
    this.loading = true;

    if (this.isAllocationAccountLogin) {
      this.loadAllocationDashboard(false);
      return;
    }
    
    // Load account details
    this.accountService.getLinkedAccountForAllocation(this.allocationId).subscribe(
      (response: any) => {
        if (response.success) {
          this.linkedAccount = response.account;
        }
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = 'Failed to load account details: ' + error.message;
        this.loading = false;
      }
    );
  }

  /**
   * Load account transactions
   */
  loadTransactions(): void {
    if (!this.allocationId) return;

    if (this.isAllocationAccountLogin) {
      this.loadAllocationDashboard(true);
      return;
    }

    this.accountService.getAccountTransactions(this.allocationId, this.transactionPage, this.transactionSize).subscribe(
      (response: any) => {
        this.accountTransactions = response.content || response.transactions || [];
      },
      (error: any) => {
        this.errorMessage = 'Failed to load transactions: ' + error.message;
      }
    );
  }

  /**
   * Load charge summary
   */
  loadChargeSummary(): void {
    if (!this.allocationId) return;

    this.chargeService.getChargeSummary(this.allocationId).subscribe(
      (response: any) => {
        this.chargesSummary = response.summary || {};
      },
      (error: any) => {
        this.errorMessage = 'Failed to load charge summary: ' + error.message;
      }
    );
  }

  /**
   * Switch tabs
   */
  switchTab(tab: string): void {
    this.activeTab = tab;
    this.activeSection = tab === 'transactions' ? 'transactions' : tab === 'charges' ? 'charges' : 'overview';
    this.errorMessage = '';

    if (tab === 'transactions') {
      this.loadTransactions();
    } else if (tab === 'charges') {
      if (this.isAllocationAccountLogin) this.loadAllocationDashboard(true);
      else this.loadChargeSummary();
    }
  }

  navigateDashboardSection(section: 'overview' | 'account' | 'transactions' | 'deposits' | 'portfolio' | 'fundUse' | 'charges' | 'loans' | 'accounts' | 'ecs' | 'profitability' | 'requests'): void {
    this.activeSection = section;
    this.errorMessage = '';
    this.requestError = '';
    this.requestSuccess = '';
    if (section === 'transactions') {
      this.activeTab = 'transactions';
      this.loadTransactions();
    } else if (section === 'charges') {
      this.activeTab = 'charges';
      if (this.isAllocationAccountLogin) this.loadAllocationDashboard(true);
      else this.loadChargeSummary();
    } else if (section === 'overview') {
      this.activeTab = 'overview';
    } else if (section === 'requests') {
      this.loadOperationRequests();
    }
  }

  get transactionAccountNumbers(): string[] {
    return [...new Set(this.accountTransactions
      .map(transaction => transaction.userAccountNumber)
      .filter((accountNumber): accountNumber is string => !!accountNumber))];
  }

  get loanTypes(): string[] {
    return [...new Set(this.allocationLoans.map(loan => String(loan.loanType || loan.type || 'OTHER')))].sort();
  }

  get filteredAllocationLoans(): any[] {
    const search = this.loanSearch.trim().toLowerCase();
    return this.allocationLoans.filter(loan => {
      const type = String(loan.loanType || loan.type || '').toUpperCase();
      if (this.loanTypeFilter !== 'ALL' && type !== this.loanTypeFilter.toUpperCase()) return false;
      if (!search) return true;
      return `${loan.loanAccountNumber || ''} ${loan.accountNumber || ''} ${loan.customerName || ''} ${loan.loanType || ''}`
        .toLowerCase().includes(search);
    });
  }

  get filteredAllocationEmiPayments(): any[] {
    const search = this.loanSearch.trim().toLowerCase();
    return this.allocationEmiPayments.filter(emi =>
      !search || `${emi.loanAccountNumber || ''} ${emi.accountNumber || ''} ${emi.transactionId || ''}`
        .toLowerCase().includes(search));
  }

  get filteredAllocationEcsMandates(): any[] {
    const search = this.mandateSearch.trim().toLowerCase();
    return this.allocationEcsMandates.filter(mandate =>
      !search || `${mandate.mandateId || ''} ${mandate.loanAccountNumber || ''} ${mandate.savingsAccountNumber || ''} ${mandate.customerName || ''}`
        .toLowerCase().includes(search));
  }

  get filteredAllocationAccountDeposits(): any[] {
    const search = this.accountDepositSearch.trim().toLowerCase();
    return this.allocationAccountDeposits.filter(account =>
      !search || `${account.accountNumber || ''} ${account.customerName || ''}`.toLowerCase().includes(search));
  }

  get filteredTransactions(): any[] {
    return this.accountTransactions.filter(transaction => {
      if (this.transactionAccountFilter && transaction.userAccountNumber !== this.transactionAccountFilter) return false;
      if (this.transactionProductFilter === 'ALL') return true;
      const product = String(transaction.productType || '').toUpperCase();
      if (this.transactionProductFilter === 'FD') return product.includes('FD') || product.includes('FIXED');
      if (this.transactionProductFilter === 'RD') return product.includes('RD') || product.includes('RECURRING');
      return !product.includes('FD') && !product.includes('FIXED')
        && !product.includes('RD') && !product.includes('RECURRING');
    });
  }

  get fixedDepositTransactions(): any[] {
    return this.accountTransactions.filter(transaction => {
      const product = String(transaction.productType || '').toUpperCase();
      return product.includes('FD') || product.includes('FIXED');
    });
  }

  get recurringDepositTransactions(): any[] {
    return this.accountTransactions.filter(transaction => {
      const product = String(transaction.productType || '').toUpperCase();
      return product.includes('RD') || product.includes('RECURRING');
    });
  }

  get filteredCharges(): any[] {
    return this.dashboardCharges.filter(charge =>
      (!this.chargeAccountFilter || charge.userAccountNumber === this.chargeAccountFilter)
      && (this.chargeCategoryFilter === 'ALL' || this.getChargeCategory(charge) === this.chargeCategoryFilter));
  }

  get chargeAccountNumbers(): string[] {
    return [...new Set(this.dashboardCharges
      .map(charge => charge.userAccountNumber)
      .filter((accountNumber): accountNumber is string => !!accountNumber))];
  }

  get chargeCategoryAccountSummaries(): { category: string; label: string; accountNumber: string; count: number; total: number }[] {
    const groups = new Map<string, { category: string; label: string; accountNumber: string; count: number; total: number }>();
    for (const charge of this.filteredCharges) {
      const category = this.getChargeCategory(charge);
      const accountNumber = charge.userAccountNumber || 'Account not recorded';
      const key = `${category}:${accountNumber}`;
      const group = groups.get(key) || {
        category,
        label: this.getChargeCategoryLabel(category),
        accountNumber,
        count: 0,
        total: 0
      };
      group.count++;
      group.total += Number(charge.amount) || 0;
      groups.set(key, group);
    }
    return [...groups.values()].sort((a, b) => a.label.localeCompare(b.label)
      || a.accountNumber.localeCompare(b.accountNumber));
  }

  getChargeCategoryLabel(category: string): string {
    return this.chargeCategories.find(item => item.value === category)?.label || category.replace(/_/g, ' ');
  }

  getChargeCategory(charge: any): string {
    const rawCategory = String(charge?.chargeCategory || '').toUpperCase();
    if (this.chargeCategories.some(category => category.value === rawCategory)) return rawCategory;
    const text = `${charge?.chargeType || ''} ${charge?.description || ''}`
      .toUpperCase().replace(/[_-]+/g, ' ');
    if (text.includes('CIBIL')) return 'CIBIL';
    if (text.includes('CREDIT CARD') && text.includes('PROCESS')) return 'CREDIT_CARD_PROCESSING_FEE';
    if (text.includes('GOLD') && text.includes('INTEREST')) return 'GOLD_LOAN_INTEREST';
    if (text.includes('EMI') && (text.includes('BOUNCE') || text.includes('RETURN'))) return 'EMI_BOUNCE';
    if (text.includes('CHEQUE') || text.includes('CHECK')) return 'CHEQUE';
    if (text.includes('SMS') || text.includes('MESSAGE')) return 'SMS';
    if (text.includes('MAINTENANCE') || text.includes('MAINTAIN')) return 'MAINTENANCE';
    if (text.includes('IMPS')) return 'IMPS';
    if (text.includes('ATM')) return 'ATM';
    if (text.includes('PROCESS')) return 'PROCESSING';
    if (text.includes('EMI')) return 'EMI';
    if (text.includes('INTEREST')) return 'INTEREST';
    return 'OTHER';
  }

  get allocationProductSummary(): { label: string; productTypes: string[]; debitCount: number; debitAmount: number; creditCount: number; creditAmount: number }[] {
    return this.allocationProductCategories.map(category => {
      const entries = this.productBreakdown.filter(product =>
        category.keys.includes(String(product.productType || '').toUpperCase().replace(/[\s-]+/g, '_')));
      return {
        label: category.label,
        productTypes: entries.map(product => product.productType),
        debitCount: entries.reduce((total, product) => total + Number(product.debitCount || 0), 0),
        debitAmount: entries.reduce((total, product) => total + Number(product.debitAmount || 0), 0),
        creditCount: entries.reduce((total, product) => total + Number(product.creditCount || 0), 0),
        creditAmount: entries.reduce((total, product) => total + Number(product.creditAmount || 0), 0)
      };
    });
  }

  showAllocationChargeDetails(charge: any): void {
    this.selectedAllocationCharge = charge;
  }

  closeAllocationChargeDetails(): void {
    this.selectedAllocationCharge = null;
  }

  changeChargePage(direction: -1 | 1): void {
    const nextPage = this.chargePage + direction;
    if (nextPage < 0 || (this.chargeTotalPages > 0 && nextPage >= this.chargeTotalPages)) return;
    this.chargePage = nextPage;
    this.loadAllocationDashboard(true);
  }

  submitOperationRequest(): void {
    this.requestError = '';
    this.requestSuccess = '';
    if (!this.requestAmount || this.requestAmount <= 0) {
      this.requestError = 'Enter an amount greater than zero.';
      return;
    }
    if (this.requestType === 'TRANSFER' && !this.requestDestinationAccount.trim()) {
      this.requestError = 'Enter the transfer destination account.';
      return;
    }
    const token = sessionStorage.getItem('allocationAccountSessionToken') || '';
    if (!token) {
      this.logoutAllocationAccount();
      return;
    }
    this.isSubmittingRequest = true;
    this.accountService.submitAllocationRequest(token, {
      requestType: this.requestType,
      amount: this.requestAmount,
      destinationAccountNumber: this.requestDestinationAccount.trim() || null,
      destinationName: this.requestDestinationName.trim() || null,
      externalReference: this.requestReference.trim() || null,
      description: this.requestDescription.trim() || null
    }).subscribe({
      next: (response: any) => {
        this.isSubmittingRequest = false;
        if (!response?.success) {
          this.requestError = response?.message || 'Unable to submit this request.';
          return;
        }
        if (response.request) this.operationRequests = [response.request, ...this.operationRequests];
        this.requestAmount = null;
        this.requestDestinationAccount = '';
        this.requestDestinationName = '';
        this.requestReference = '';
        this.requestDescription = '';
        this.requestSuccess = 'Request sent to the HOD for review. No funds have moved.';
      },
      error: (error: any) => {
        this.isSubmittingRequest = false;
        this.requestError = error.error?.message || 'Unable to submit this request.';
      }
    });
  }

  private loadOperationRequests(): void {
    const token = sessionStorage.getItem('allocationAccountSessionToken') || '';
    if (!token) return;
    this.accountService.getMyAllocationRequests(token).subscribe({
      next: (response: any) => this.operationRequests = response?.requests || [],
      error: () => this.requestError = 'Unable to load your submitted requests.'
    });
  }

  openTransactionDetails(transaction: any): void {
    this.selectedTransaction = transaction;
    this.selectedCharge = null;
  }

  openChargeDetails(charge: any): void {
    this.selectedCharge = charge;
    this.selectedTransaction = null;
  }

  closeActivityDetails(): void {
    this.selectedTransaction = null;
    this.selectedCharge = null;
  }

  private loadAllocationDashboard(silent: boolean): void {
    const token = sessionStorage.getItem('allocationAccountSessionToken') || '';
    if (!token || !this.allocationId) return;
    if (silent) this.isRefreshing = true;
    else this.loading = true;
    this.accountService.getMyAllocationDashboard(
      token, this.transactionPage, this.transactionSize, this.chargePage, this.chargeSize
    ).subscribe({
      next: (response: any) => {
        if (!response?.success) {
          this.errorMessage = response?.message || 'Allocation dashboard data is unavailable.';
          this.loading = false;
          this.isRefreshing = false;
          return;
        }
        const allocation = response.allocation || {};
        const account = response.account || {};
        this.linkedAccount = {
          ...allocation,
          ...account,
          currentBalance: account.currentBalance ?? allocation.currentBalance,
          totalAllocated: account.totalAllocated ?? allocation.allocatedAmount,
          totalDebited: account.totalDebited ?? allocation.totalDebited,
          totalCredited: account.totalCredited ?? allocation.totalCredited
        };
        this.dashboardSummary = response.summary || {};
        this.productBreakdown = response.productBreakdown || [];
        this.dashboardCharges = response.charges || [];
        this.allocationLoans = response.loans || [];
        this.allocationEmiPayments = response.emiPayments || [];
        this.allocationEcsMandates = response.ecsMandates || [];
        this.allocationAccountDeposits = response.accountDeposits || [];
        this.allocationPortfolio = response.portfolio || {};
        this.allocationProfitability = response.profitability || {};
        this.chargePage = response.chargePage || 0;
        this.chargeTotalPages = response.chargeTotalPages || 0;
        this.chargeTotalElements = response.chargeTotalElements || 0;
        this.accountTransactions = (response.transactions || []).map((transaction: any) => ({
          ...transaction,
          balance: transaction.balanceAfter,
          date: transaction.transactionDate
        }));
        this.transactionTotalPages = response.transactionTotalPages || 0;
        this.chargesSummary = {
          interest: account.interestCharges || 0,
          cibil: account.cibilCharges || 0,
          soundbox: account.soundboxCharges || 0,
          upi: account.upiCharges || 0,
          paymentGateway: account.paymentGatewayCharges || 0,
          totalCharges: account.totalChargesCollected || 0
        };
        this.lastRefreshedAt = response.lastUpdated ? new Date(response.lastUpdated) : new Date();
        this.loadOperationRequests();
        this.errorMessage = '';
        this.loading = false;
        this.isRefreshing = false;
      },
      error: (error: any) => {
        this.errorMessage = error.error?.message || 'Failed to refresh allocation dashboard.';
        this.loading = false;
        this.isRefreshing = false;
      }
    });
  }

  /**
   * Calculate available balance
   */
  getAvailableBalance(): number {
    if (!this.linkedAccount) return 0;
    return (this.linkedAccount.currentBalance || 0) - (this.chargesSummary.totalCharges || 0);
  }

  /**
   * Get account status badge
   */
  getStatusBadge(status: string): string {
    switch (status) {
      case 'VERIFIED':
        return 'badge-success';
      case 'ACTIVE':
        return 'badge-success';
      case 'PENDING':
        return 'badge-warning';
      case 'BLOCKED':
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Format currency
   */
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR'
    }).format(value);
  }

  /**
   * Paginate transactions
   */
  nextPage(): void {
    if (this.isAllocationAccountLogin && this.transactionPage + 1 >= this.transactionTotalPages) return;
    this.transactionPage++;
    this.loadTransactions();
  }

  previousPage(): void {
    if (this.transactionPage > 0) {
      this.transactionPage--;
      this.loadTransactions();
    }
  }

  /**
   * Export account statement
   */
  exportStatement(): void {
    if (!this.linkedAccount) return;
    // TODO: Implement export functionality
    alert('Statement export functionality coming soon!');
  }

  /**
   * Print account details
   */
  printAccount(): void {
    window.print();
  }

  logoutAllocationAccount(): void {
    sessionStorage.removeItem('allocationAccount');
    sessionStorage.removeItem('allocationAccountSessionToken');
    sessionStorage.removeItem('userRole');
    this.router.navigate(['/website/user']);
  }
}
