import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
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
export class FixedDeposits implements OnInit, OnDestroy {
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
  rdContributions: any[] = [];
  savingRD = false;
  showEditModal = false;
  editingDeposit: 'FD' | 'RD' | null = null;
  savingEdit = false;
  editTarget: any = null;
  editForm: any = {};
  fdHistory: any[] = [];
  selectedHistoryFD: any = null;
  private rdRefreshTimer: any;
  
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
      this.rdRefreshTimer = setInterval(() => {
        if (this.activeProduct === 'RD' && !this.isLoadingRDs) this.loadRecurringDeposits(true);
      }, 10000);
    }
  }

  ngOnDestroy() {
    if (this.rdRefreshTimer) clearInterval(this.rdRefreshTimer);
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
    this.editTarget = fd;
    this.editingDeposit = 'FD';
    this.editForm = {
      principalAmount: fd.principalAmount,
      interestRate: fd.interestRate,
      tenure: fd.tenure,
      maturityDate: this.toDateInput(fd.maturityDate),
      interestPayout: fd.interestPayout || 'At Maturity',
      remarks: '',
      reason: ''
    };
    this.showEditModal = true;
  }

  editRD(rd: any) {
    this.editTarget = rd;
    this.editingDeposit = 'RD';
    this.editForm = {
      monthlyInstallment: rd.monthlyInstallment,
      tenureMonths: rd.tenureMonths,
      annualInterestRate: rd.annualInterestRate,
      startDate: this.toDateInput(rd.startDate),
      maturityDate: this.toDateInput(rd.maturityDate),
      remarks: rd.remarks || '',
      reason: ''
    };
    this.showEditModal = true;
  }

  closeDepositEditModal() {
    if (this.savingEdit) return;
    this.showEditModal = false;
    this.editingDeposit = null;
    this.editTarget = null;
    this.editForm = {};
  }

  updateFDMaturityDate() {
    this.editForm.maturityDate = this.maturityDateAfterMonths(
      this.editTarget?.startDate,
      Number(this.editForm.tenure)
    );
  }

  updateRDMaturityDate() {
    this.editForm.maturityDate = this.maturityDateAfterMonths(
      this.editForm.startDate,
      Number(this.editForm.tenureMonths)
    );
  }

  saveDepositEdit() {
    if (!this.editTarget || !this.editingDeposit) return;
    if (!this.editForm.reason?.trim()) {
      this.alertService.error('Audit Reason Required', 'Enter a reason for this audited update.');
      return;
    }

    if (this.editingDeposit === 'FD') {
      if (this.editForm.principalAmount <= 0 || this.editForm.interestRate < 0
          || this.editForm.interestRate > 100 || this.editForm.tenure <= 0
          || !this.editForm.maturityDate) {
        this.alertService.error('Validation Error', 'Enter a valid principal, interest rate, tenure, and maturity date.');
        return;
      }
      const body: any = {
        interestRate: Number(this.editForm.interestRate),
        tenure: Number(this.editForm.tenure),
        maturityDate: this.editForm.maturityDate,
        interestPayout: this.editForm.interestPayout,
        remarks: this.editForm.remarks
      };
      if (this.editTarget.status === 'PENDING') {
        body.principalAmount = Number(this.editForm.principalAmount);
      }
      this.savingEdit = true;
      this.http.put<any>(
        `${environment.apiBaseUrl}/api/fixed-deposits/${this.editTarget.id}?reason=${encodeURIComponent(this.editForm.reason.trim())}`,
        body,
        this.staffOptions()
      ).subscribe({
        next: response => {
          this.savingEdit = false;
          if (!response?.success) {
            this.alertService.error('Update Failed', response?.message || 'Unable to update FD details.');
            return;
          }
          this.alertService.success('FD Updated', 'Details and maturity estimate were saved to history.');
          this.closeDepositEditModal();
          this.loadFixedDeposits();
        },
        error: err => {
          this.savingEdit = false;
          this.alertService.error('FD Update Failed', err.error?.message || 'Unable to update FD details.');
        }
      });
      return;
    }

    if (this.editForm.monthlyInstallment <= 0 || this.editForm.tenureMonths <= 0
        || this.editForm.annualInterestRate < 0 || this.editForm.annualInterestRate > 100
        || !this.editForm.startDate || !this.editForm.maturityDate) {
      this.alertService.error('Validation Error', 'Enter a valid installment, duration, interest rate, and dates.');
      return;
    }
    this.savingEdit = true;
    this.http.put<any>(`${environment.apiBaseUrl}/api/recurring-deposits/${this.editTarget.id}`, {
      monthlyInstallment: Number(this.editForm.monthlyInstallment),
      tenureMonths: Number(this.editForm.tenureMonths),
      annualInterestRate: Number(this.editForm.annualInterestRate),
      startDate: this.editForm.startDate,
      maturityDate: this.editForm.maturityDate,
      remarks: this.editForm.remarks,
      reason: this.editForm.reason.trim()
    }, this.staffOptions()).subscribe({
      next: () => {
        this.savingEdit = false;
        this.alertService.success('RD Updated', 'Details and estimate were saved to history; no funds were moved.');
        const updatedRD = this.editTarget;
        this.closeDepositEditModal();
        this.loadRecurringDeposits();
        this.loadRDHistory(updatedRD);
      },
      error: err => {
        this.savingEdit = false;
        this.alertService.error('RD Update Failed', err.error?.message || 'Unable to update RD details.');
      }
    });
  }

  private toDateInput(value: string | null | undefined): string {
    return value ? value.slice(0, 10) : '';
  }

  private maturityDateAfterMonths(startValue: string | null | undefined, months: number): string {
    const startDate = this.toDateInput(startValue);
    if (!startDate || !Number.isInteger(months) || months <= 0) return '';
    const [year, month, day] = startDate.split('-').map(Number);
    const targetMonth = month - 1 + months;
    const targetYear = year + Math.floor(targetMonth / 12);
    const normalizedMonth = targetMonth % 12 + 1;
    const lastDay = new Date(Date.UTC(targetYear, normalizedMonth, 0)).getUTCDate();
    const normalizedDay = Math.min(day, lastDay).toString().padStart(2, '0');
    return `${targetYear}-${normalizedMonth.toString().padStart(2, '0')}-${normalizedDay}`;
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

  loadRecurringDeposits(silent = false) {
    this.isLoadingRDs = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/recurring-deposits`, this.staffOptions()).subscribe({
      next: rows => { this.recurringDeposits = rows || []; this.isLoadingRDs = false; },
      error: err => {
        this.isLoadingRDs = false;
        if (!silent) this.alertService.error('RD Load', err.error?.message || 'Unable to load recurring-deposit records.');
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

  loadRDHistory(rd: any) {
    this.selectedRD = rd;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/recurring-deposits/${rd.id}/history`, this.staffOptions()).subscribe({
      next: history => this.rdHistory = history || [],
      error: err => this.alertService.error('RD History', err.error?.message || 'Unable to load RD history.')
    });
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/recurring-deposits/${rd.id}/contributions`, this.staffOptions()).subscribe({
      next: contributions => this.rdContributions = contributions || [],
      error: err => this.alertService.error('RD Contributions', err.error?.message || 'Unable to load RD contributions.')
    });
  }

  issueRDShareLink(rd: any) {
    this.http.post<any>(`${environment.apiBaseUrl}/api/recurring-deposits/${rd.id}/share-link`, {}, this.staffOptions()).subscribe({
      next: response => {
        rd.shareUrl = response.shareUrl;
        rd.status = response.status;
        this.alertService.success('RD Review Link', 'Customer review link generated. Copy and share it with the customer.');
        this.copyRDShareLink(rd);
      },
      error: err => this.alertService.error('RD Review Link', err.error?.message || 'Unable to generate the customer review link.')
    });
  }

  copyRDShareLink(rd: any) {
    if (!rd?.shareUrl) return;
    navigator.clipboard.writeText(rd.shareUrl).then(
      () => this.alertService.success('RD Review Link', 'Customer review link copied'),
      () => this.alertService.warning('RD Review Link', `Copy this customer link: ${rd.shareUrl}`)
    );
  }

  approveRD(rd: any) {
    if (!confirm(`Approve recurring deposit ${rd.rdAccountNumber || rd.id} after checking the recorded customer acknowledgement?`)) return;
    this.http.post<any>(`${environment.apiBaseUrl}/api/recurring-deposits/${rd.id}/approve`, {}, this.staffOptions()).subscribe({
      next: updated => {
        Object.assign(rd, updated);
        this.alertService.success('RD Approved', 'RD approved. It can now be selected for a separate ECS consent request.');
        this.loadRecurringDeposits();
      },
      error: err => this.alertService.error('RD Approval', err.error?.message || 'Unable to approve this RD.')
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
