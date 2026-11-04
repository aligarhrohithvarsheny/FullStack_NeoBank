import { Component, OnInit } from '@angular/core';
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
export class ManagerAccountDashboardComponent implements OnInit {
  
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
  }

  /**
   * Load linked account details
   */
  loadAccountData(): void {
    if (!this.allocationId) return;
    
    this.loading = true;

    if (this.isAllocationAccountLogin) {
      const token = sessionStorage.getItem('allocationAccountSessionToken') || '';
      this.accountService.getMyAllocationAccount(token).subscribe({
        next: (response: any) => {
          if (response?.success) {
            const allocation = response.allocation || {};
            const account = response.account || {};
            this.linkedAccount = {
              ...account,
              ...allocation,
              ...account,
              currentBalance: allocation.currentBalance ?? account.currentBalance,
              totalAllocated: allocation.allocatedAmount ?? account.totalAllocated,
              totalDebited: allocation.totalDebited ?? account.totalDebited,
              totalCredited: allocation.totalCredited ?? account.totalCredited
            };
            this.errorMessage = '';
          } else {
            this.linkedAccount = null;
            this.errorMessage = response?.message || 'Allocation account details are unavailable.';
          }
          this.loading = false;
        },
        error: (error: any) => {
          this.linkedAccount = null;
          this.errorMessage = error.error?.message || 'Failed to load this allocation account.';
          this.loading = false;
        }
      });
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
      this.loadChargeSummary();
    }
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
