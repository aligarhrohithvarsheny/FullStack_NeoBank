import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-fixed-deposits',
  standalone: true,
  templateUrl: './fixed-deposits.html',
  styleUrls: ['./fixed-deposits.css'],
  imports: [FormsModule, CommonModule]
})
export class FixedDeposits implements OnInit {
  fixedDeposits: any[] = [];
  isLoadingFDs: boolean = false;
  selectedFD: any = null;
  fdStatusFilter: string = 'PENDING';
  fdSearchQuery: string = '';
  adminName: string = 'Admin';
  termPlans: any[] = [];
  showPlanForm = false;
  planForm: any = { productType: 'FD', schemeName: '', days: 30, durationMonths: null, years: 1, minimumAmount: 1000, maximumAmount: 10000000, interestRate: 7, payoutDate: 'At Maturity' };
  savingPlan = false;
  activeProduct: 'FD' | 'RD' = 'FD';
  recurringDeposits: any[] = [];
  isLoadingRDs = false;
  showRDForm = false;
  rdForm: any = { accountNumber: '', planId: null, monthlyInstallment: 1000, startDate: new Date().toISOString().slice(0, 10), remarks: '' };
  rdHistory: any[] = [];
  selectedRD: any = null;
  savingRD = false;
  fdHistory: any[] = [];
  selectedHistoryFD: any = null;
  
  // Approval modal properties
  showApprovalModal: boolean = false;
  fdToApprove: any = null;
  approvalReason: string = '';
  isApproving: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      const adminData = sessionStorage.getItem('admin');
      if (adminData) {
        try {
          const admin = JSON.parse(adminData);
          this.adminName = admin.username || admin.name || 'Admin';
        } catch (e) {
          console.error('Error parsing admin data:', e);
        }
      }
      this.loadFixedDeposits();
      this.loadTermPlans();
      this.loadRecurringDeposits();
    }
  }

  loadTermPlans() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/term-deposit-plans/all`, this.staffOptions()).subscribe({
      next: plans => this.termPlans = plans || [],
      error: err => this.alertService.error('Scheme Load', err.error?.message || 'Unable to load schemes')
    });
  }

  saveTermPlan() {
    if (!this.planForm.schemeName || this.planForm.interestRate < 0
      || (this.planForm.productType === 'RD'
        ? !this.planForm.durationMonths
        : (!this.planForm.days && !this.planForm.durationMonths))) {
      this.alertService.error('Scheme Validation', 'Enter a name, duration, and valid interest rate.');
      return;
    }
    this.savingPlan = true;
    this.planForm.createdBy = this.adminName;
    this.http.post(`${environment.apiBaseUrl}/api/term-deposit-plans`, this.planForm, this.staffOptions()).subscribe({
      next: () => {
        this.savingPlan = false;
        this.showPlanForm = false;
        this.planForm = { productType: 'FD', schemeName: '', days: 30, durationMonths: null, years: 1, minimumAmount: 1000, maximumAmount: 10000000, interestRate: 7, payoutDate: 'At Maturity' };
        this.loadTermPlans();
      },
      error: err => { this.savingPlan = false; this.alertService.error('Plan Failed', err.error?.message || 'Unable to create term-deposit plan'); }
    });
  }

  deactivateTermPlan(plan: any) {
    this.http.delete(`${environment.apiBaseUrl}/api/term-deposit-plans/${plan.id}`, this.staffOptions()).subscribe({
      next: () => this.loadTermPlans(),
      error: err => this.alertService.error('Plan Failed', err.error?.message || 'Unable to deactivate plan')
    });
  }

  loadFixedDeposits() {
    this.isLoadingFDs = true;
    this.http.get(`${environment.apiBaseUrl}/api/fixed-deposits`, this.staffOptions()).subscribe({
      next: (fds: any) => {
        this.fixedDeposits = fds || [];
        this.isLoadingFDs = false;
      },
      error: (err: any) => {
        console.error('Error loading fixed deposits:', err);
        this.alertService.error('Error', 'Failed to load fixed deposits');
        this.isLoadingFDs = false;
      }
    });
  }

  getFilteredFDs() {
    let filtered = [...this.fixedDeposits];

    if (this.fdStatusFilter !== 'ALL') {
      filtered = filtered.filter(fd => fd.status === this.fdStatusFilter);
    }

    if (this.fdSearchQuery && this.fdSearchQuery.trim() !== '') {
      const query = this.fdSearchQuery.toLowerCase();
      filtered = filtered.filter(fd =>
        (fd.userName && fd.userName.toLowerCase().includes(query)) ||
        (fd.accountNumber && fd.accountNumber.toLowerCase().includes(query)) ||
        (fd.fdAccountNumber && fd.fdAccountNumber.toLowerCase().includes(query))
      );
    }

    return filtered;
  }

  approveFixedDeposit(fd: any) {
    this.fdToApprove = fd;
    this.approvalReason = '';
    this.showApprovalModal = true;
  }

  // Open modal to view FD details (read-only)
  viewFDDetails(fd: any) {
    this.fdToApprove = fd;
    this.approvalReason = '';
    this.showApprovalModal = true;
  }
  
  closeApprovalModal() {
    this.showApprovalModal = false;
    this.fdToApprove = null;
    this.approvalReason = '';
  }
  
  confirmApproval() {
    if (!this.fdToApprove) return;
    
    // Ensure ID is a number
    const fdId = typeof this.fdToApprove.id === 'number' ? this.fdToApprove.id : Number(this.fdToApprove.id);
    if (isNaN(fdId)) {
      this.alertService.error('Validation Error', 'Invalid fixed deposit ID');
      return;
    }
    
    this.isApproving = true;
    const reasonParam = this.approvalReason ? `&approvalReason=${encodeURIComponent(this.approvalReason)}` : '';
    this.http.put(`${environment.apiBaseUrl}/api/fixed-deposits/${fdId}/approve?approvedBy=${encodeURIComponent(this.adminName)}${reasonParam}`, {}, this.staffOptions()).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit approved successfully');
          this.closeApprovalModal();
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve FD');
        }
        this.isApproving = false;
      },
      error: (err: any) => {
        console.error('Error approving FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve FD');
        this.isApproving = false;
      }
    });
  }

  rejectFixedDeposit(fd: any) {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;
    
    // Ensure ID is a number
    const fdId = typeof fd.id === 'number' ? fd.id : Number(fd.id);
    if (isNaN(fdId)) {
      this.alertService.error('Validation Error', 'Invalid fixed deposit ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/fixed-deposits/${fdId}/reject?rejectedBy=${encodeURIComponent(this.adminName)}&reason=${encodeURIComponent(reason)}`, {}, this.staffOptions()).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit rejected');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject FD');
        }
      },
      error: (err: any) => {
        console.error('Error rejecting FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject FD');
      }
    });
  }

  editFD(fd: any) {
    const principal = fd.status === 'PENDING' ? prompt('Principal amount (only before funding):', fd.principalAmount) : null;
    if (fd.status === 'PENDING' && principal === null) return;
    const maturity = prompt('Maturity date (YYYY-MM-DD):', fd.maturityDate || '');
    if (maturity === null) return;
    const payout = prompt('Interest payout (Monthly, Quarterly, At Maturity):', fd.interestPayout || 'At Maturity');
    if (!payout) return;
    const reason = prompt('Reason for this audited update:');
    if (!reason?.trim()) return;
    const body: any = { maturityDate: maturity, interestPayout: payout, remarks: fd.remarks };
    if (principal !== null) body.principalAmount = Number(principal);
    this.http.put(`${environment.apiBaseUrl}/api/fixed-deposits/${fd.id}?reason=${encodeURIComponent(reason)}`, body, this.staffOptions()).subscribe({
      next: (response: any) => {
        if (response.success) this.alertService.success('Updated', 'FD details and maturity estimate recalculated.');
        else this.alertService.error('Update Failed', response.message || 'Unable to update FD.');
        this.loadFixedDeposits();
      },
      error: (e: any) => this.alertService.error('Error', e.error?.message || 'Unable to update FD')
    });
  }
  topUpFD(fd: any) { const amount = prompt('Increase FD amount by:'); if (!amount || Number(amount) <= 0) return; this.http.post(`${environment.apiBaseUrl}/api/fixed-deposits/${fd.id}/admin/top-up?amount=${Number(amount)}&admin=${encodeURIComponent(this.adminName)}`, {}, this.staffOptions()).subscribe({ next: () => this.loadFixedDeposits(), error: (e: any) => this.alertService.error('Error', e.error?.message || 'Unable to increase FD') }); }
  closeFD(fd: any) {
    const reason = prompt('Reason for closing this FD:');
    if (!reason?.trim() || !confirm('Close this fixed deposit and credit the calculated closure/maturity amount to the linked account?')) return;
    this.http.post(`${environment.apiBaseUrl}/api/fixed-deposits/${fd.id}/admin/close?reason=${encodeURIComponent(reason)}`, {}, this.staffOptions()).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('FD Closed', 'The calculated closure amount was credited and recorded.');
          this.loadFixedDeposits();
        } else this.alertService.error('Close Failed', response.message || 'Unable to close FD.');
      },
      error: (e: any) => this.alertService.error('Error', e.error?.message || 'Unable to close FD')
    });
  }

  isFDMatured(fd: any): boolean {
    if (!fd || !fd.maturityDate || fd.isMatured) return false;
    const maturityDate = new Date(fd.maturityDate);
    const today = new Date();
    return maturityDate <= today;
  }

  processFDMaturity(fd: any) {
    if (!confirm(`Process maturity for FD ${fd.fdAccountNumber}? Maturity amount ₹${fd.maturityAmount} will be credited to account.`)) return;
    
    // Ensure ID is a number
    const fdId = typeof fd.id === 'number' ? fd.id : Number(fd.id);
    if (isNaN(fdId)) {
      this.alertService.error('Validation Error', 'Invalid fixed deposit ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/fixed-deposits/${fdId}/process-maturity?processedBy=${encodeURIComponent(this.adminName)}`, {}, this.staffOptions()).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'FD maturity processed successfully');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to process maturity');
        }
      },
      error: (err: any) => {
        console.error('Error processing FD maturity:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to process maturity');
      }
    });
  }

  goBack() {
    const navigationSource = sessionStorage.getItem('navigationSource');
    if (navigationSource === 'MANAGER') {
      sessionStorage.removeItem('navigationSource');
      sessionStorage.removeItem('managerReturnPath');
      this.router.navigate(['/manager/dashboard']);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  private staffOptions() {
    const token = sessionStorage.getItem('adminSessionToken') || '';
    return { headers: new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {}) };
  }

  private loadRecurringDeposits() {
    this.isLoadingRDs = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/recurring-deposits`, this.staffOptions()).subscribe({
      next: rows => { this.recurringDeposits = rows || []; this.isLoadingRDs = false; },
      error: err => {
        this.isLoadingRDs = false;
        this.alertService.error('RD Load', err.error?.message || 'Unable to load recurring-deposit records.');
      }
    });
  }

  plansFor(type: 'FD' | 'RD') {
    return this.termPlans.filter(plan => (plan.productType || 'FD').toUpperCase() === type && plan.active);
  }

  setProduct(type: 'FD' | 'RD') {
    this.activeProduct = type;
    if (type === 'RD') this.loadRecurringDeposits();
  }

  createRDRecord() {
    const form = this.rdForm;
    if (!form.accountNumber?.trim() || !form.planId || Number(form.monthlyInstallment) <= 0) {
      this.alertService.error('RD Validation', 'Enter a linked account, active RD scheme, and monthly installment.');
      return;
    }
    this.savingRD = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/recurring-deposits`, form, this.staffOptions()).subscribe({
      next: () => {
        this.savingRD = false;
        this.showRDForm = false;
        this.rdForm = { accountNumber: '', planId: null, monthlyInstallment: 1000, startDate: new Date().toISOString().slice(0, 10), remarks: '' };
        this.loadRecurringDeposits();
        this.alertService.success('RD Record Added', 'Details saved only; no customer funds were moved.');
      },
      error: err => {
        this.savingRD = false;
        this.alertService.error('RD Record', err.error?.message || 'Unable to save RD details.');
      }
    });
  }

  editRD(rd: any) {
    const monthlyInstallment = prompt('Monthly installment amount:', String(rd.monthlyInstallment));
    if (monthlyInstallment === null) return;
    const tenureMonths = prompt('Duration in months:', String(rd.tenureMonths));
    if (tenureMonths === null) return;
    const annualInterestRate = prompt('Annual interest rate (%):', String(rd.annualInterestRate));
    if (annualInterestRate === null) return;
    const maturityDate = prompt('Maturity date (YYYY-MM-DD):', rd.maturityDate);
    if (maturityDate === null) return;
    const reason = prompt('Reason for this audited update:');
    if (!reason?.trim()) return;
    this.http.put<any>(`${environment.apiBaseUrl}/api/recurring-deposits/${rd.id}`, {
      monthlyInstallment: Number(monthlyInstallment),
      tenureMonths: Number(tenureMonths),
      annualInterestRate: Number(annualInterestRate),
      maturityDate,
      reason
    }, this.staffOptions()).subscribe({
      next: () => {
        this.loadRecurringDeposits();
        this.loadRDHistory(rd);
        this.alertService.success('RD Updated', 'Details and estimated values updated; no funds were moved.');
      },
      error: err => this.alertService.error('RD Update', err.error?.message || 'Unable to update RD details.')
    });
  }

  loadRDHistory(rd: any) {
    this.selectedRD = rd;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/recurring-deposits/${rd.id}/history`, this.staffOptions()).subscribe({
      next: history => this.rdHistory = history || [],
      error: err => this.alertService.error('RD History', err.error?.message || 'Unable to load RD history.')
    });
  }

  loadFDHistory(fd: any) {
    this.selectedHistoryFD = fd;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/fixed-deposits/${fd.id}/history/events`, this.staffOptions()).subscribe({
      next: history => this.fdHistory = history || [],
      error: err => this.alertService.error('FD History', err.error?.message || 'Unable to load FD history.')
    });
  }
}
