import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SalaryAccountService } from '../../../service/salary-account.service';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-employee-salary-link',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './employee-salary-link.html',
  styleUrls: ['./employee-salary-link.css']
})
export class EmployeeSalaryLinkComponent implements OnInit {
  salaryAccounts: any[] = [];
  filteredAccounts: any[] = [];
  loading = false;
  linking = false;
  searchTerm = '';
  activeFilter = 'ALL'; // ALL, LINKED, UNLINKED

  // Link form
  selectedAccount: any = null;
  employeeIdInput = '';

  // Stats
  stats = { total: 0, linked: 0, unlinked: 0 };

  constructor(
    private salaryAccountService: SalaryAccountService,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading = true;
    this.salaryAccountService.getAll().subscribe({
      next: (accounts: any[]) => {
        this.salaryAccounts = accounts || [];
        this.calculateStats();
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  calculateStats(): void {
    this.stats.total = this.salaryAccounts.length;
    this.stats.linked = this.salaryAccounts.filter(a => a.employeeIdLinked).length;
    this.stats.unlinked = this.salaryAccounts.filter(a => !a.employeeIdLinked).length;
  }

  applyFilter(): void {
    let accounts = [...this.salaryAccounts];
    if (this.activeFilter === 'LINKED') {
      accounts = accounts.filter(a => a.employeeIdLinked);
    } else if (this.activeFilter === 'UNLINKED') {
      accounts = accounts.filter(a => !a.employeeIdLinked);
    }
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      accounts = accounts.filter(a =>
        a.employeeName?.toLowerCase().includes(term) ||
        a.accountNumber?.includes(term) ||
        a.employeeId?.toLowerCase().includes(term) ||
        a.companyName?.toLowerCase().includes(term)
      );
    }
    this.filteredAccounts = accounts;
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  openLinkForm(account: any): void {
    this.selectedAccount = account;
    this.employeeIdInput = '';
  }

  closeLinkForm(): void {
    this.selectedAccount = null;
    this.employeeIdInput = '';
  }

  linkEmployee(): void {
    if (!this.selectedAccount || !this.employeeIdInput.trim()) return;
    this.linking = true;
    this.salaryAccountService.linkEmployeeId(this.selectedAccount.id, this.employeeIdInput.trim()).subscribe({
      next: (res: any) => {
        this.linking = false;
        if (res.success) {
          this.alertService.success('Linked!', 'Employee ID linked to salary account successfully');
          this.closeLinkForm();
          this.loadAccounts();
        } else {
          this.alertService.error('Failed', res.message || 'Could not link employee ID');
        }
      },
      error: (err: any) => {
        this.linking = false;
        const msg = err.error?.message || 'Failed to link employee ID';
        this.alertService.error('Error', msg);
      }
    });
  }

  formatDate(date: string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  formatCurrency(amount: number): string {
    return '₹' + (amount || 0).toLocaleString('en-IN');
  }
}
