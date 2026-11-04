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
  transactionPage = 0;
  transactionSize = 10;
  isAllocationAccountLogin = false;
  dashboardSummary: any = {};
  dashboardCharges: any[] = [];
  transactionTotalPages = 0;
  lastRefreshedAt: Date | null = null;
  isRefreshing = false;
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
      }, 15000);
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
    this.errorMessage = '';

    if (tab === 'transactions') {
      this.loadTransactions();
    } else if (tab === 'charges') {
      if (this.isAllocationAccountLogin) this.loadAllocationDashboard(true);
      else this.loadChargeSummary();
    }
  }

  private loadAllocationDashboard(silent: boolean): void {
    const token = sessionStorage.getItem('allocationAccountSessionToken') || '';
    if (!token || !this.allocationId) return;
    if (silent) this.isRefreshing = true;
    else this.loading = true;
    this.accountService.getMyAllocationDashboard(token, this.transactionPage, this.transactionSize).subscribe({
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
        this.dashboardCharges = response.charges || [];
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
