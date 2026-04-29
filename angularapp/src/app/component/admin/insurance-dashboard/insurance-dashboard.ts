import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../../../environment/environment';
import { FormsModule } from '@angular/forms';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-admin-insurance-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './insurance-dashboard.html',
  styleUrls: ['./insurance-dashboard.css']
})
export class AdminInsuranceDashboard implements OnInit {
  stats: any = null;
  pendingApplications: any[] = [];
  pendingClaims: any[] = [];
  policies: any[] = [];
  customers: any[] = [];

  // Assign policy
  customerSearch: string = '';
  selectedCustomer: any = null;
  selectedPolicyIdForAssign: number | null = null;
  selectedPremiumTypeForAssign: 'MONTHLY' | 'YEARLY' = 'MONTHLY';
  assigningPolicy: boolean = false;

  // Tab navigation
  activeTab: 'policies' | 'applications' | 'claims' | 'customers' = 'policies';

  // Policy search
  policySearchQuery: string = '';

  // Claim search
  claimSearchPolicyNumber: string = '';
  searchedClaims: any[] = [];
  searchingClaims: boolean = false;

  showCreatePolicy: boolean = false;
  creatingPolicy: boolean = false;
  newPolicy: any = {
    name: '',
    type: 'Health',
    coverageAmount: 100000,
    premiumAmount: 999,
    premiumType: 'MONTHLY',
    durationMonths: 12,
    description: '',
    benefits: '',
    eligibility: '',
    status: 'ACTIVE'
  };

  processingApplicationId: number | null = null;
  processingClaimId: number | null = null;

  constructor(
    private http: HttpClient,
    private alertService: AlertService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    // Prevent SSR from running browser-only flows; load only in browser
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadAll();
  }

  loadAll() {
    this.loadStats();
    this.loadPendingApplications();
    this.loadPendingClaims();
    this.loadPolicies();
    this.loadCustomers();
  }

  loadStats() {
    this.http.get(`${environment.apiBaseUrl}/api/admin/insurance/dashboard-stats`).subscribe({
      next: (res: any) => { this.stats = res; },
      error: () => { this.stats = null; }
    });
  }

  loadPendingApplications() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admin/insurance/applications/pending`).subscribe({
      next: (res) => { this.pendingApplications = res || []; },
      error: () => { this.pendingApplications = []; }
    });
  }

  loadPendingClaims() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admin/insurance/claims/pending`).subscribe({
      next: (res) => { this.pendingClaims = res || []; },
      error: () => { this.pendingClaims = []; }
    });
  }

  loadPolicies() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admin/insurance/policies`).subscribe({
      next: (res) => { this.policies = res || []; },
      error: () => { this.policies = []; }
    });
  }

  loadCustomers() {
    // Use existing safe endpoint that excludes large blobs
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/users`).subscribe({
      next: (res) => {
        const users = Array.isArray(res) ? res : [];
        // show only approved bank customers with account numbers
        this.customers = users.filter(u => (u.status || '').toUpperCase() === 'APPROVED' && !!u.accountNumber);
      },
      error: () => { this.customers = []; }
    });
  }

  get filteredPolicies(): any[] {
    const q = (this.policySearchQuery || '').toLowerCase().trim();
    if (!q) return this.policies;
    return this.policies.filter(p =>
      (p.name || '').toLowerCase().includes(q) ||
      (p.type || '').toLowerCase().includes(q) ||
      (p.policyNumber || '').toLowerCase().includes(q) ||
      (p.status || '').toLowerCase().includes(q)
    );
  }

  get filteredCustomers(): any[] {
    const q = (this.customerSearch || '').toLowerCase().trim();
    if (!q) return this.customers;
    return this.customers.filter(u =>
      (u.email || '').toLowerCase().includes(q) ||
      (u.username || '').toLowerCase().includes(q) ||
      (u.accountNumber || '').toLowerCase().includes(q) ||
      (u.account?.name || '').toLowerCase().includes(q)
    );
  }

  selectCustomer(c: any) {
    this.selectedCustomer = c;
  }

  assignPolicyToSelectedCustomer() {
    if (!this.selectedCustomer?.accountNumber || !this.selectedPolicyIdForAssign) {
      this.alertService.adminError('Validation Error', 'Select a customer and a policy.');
      return;
    }
    this.assigningPolicy = true;
    const payload = {
      accountNumber: this.selectedCustomer.accountNumber,
      policyId: this.selectedPolicyIdForAssign,
      premiumType: this.selectedPremiumTypeForAssign,
      remark: `Assigned by admin to ${this.selectedCustomer.accountNumber}`,
      customerName: this.selectedCustomer.account?.name || this.selectedCustomer.name || this.selectedCustomer.username
    };
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/assign-policy`, payload).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.adminSuccess('Assigned', res.message || 'Policy assigned.');
          this.loadPendingApplications();
          this.loadStats();
          this.selectedPolicyIdForAssign = null;
        } else {
          this.alertService.adminError('Failed', res?.message || 'Unable to assign policy.');
        }
        this.assigningPolicy = false;
      },
      error: (err) => {
        this.assigningPolicy = false;
        this.alertService.adminError('Failed', err.error?.message || 'Unable to assign policy.');
      }
    });
  }

  searchClaimsByPolicyNumber() {
    const pn = (this.claimSearchPolicyNumber || '').trim();
    if (!pn) {
      this.searchedClaims = [];
      return;
    }
    this.searchingClaims = true;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admin/insurance/claims/by-policy/${encodeURIComponent(pn)}`).subscribe({
      next: (res) => {
        this.searchedClaims = res || [];
        this.searchingClaims = false;
      },
      error: () => {
        this.searchedClaims = [];
        this.searchingClaims = false;
      }
    });
  }

  toggleCreatePolicy() {
    this.showCreatePolicy = !this.showCreatePolicy;
  }

  createPolicy() {
    if (!this.newPolicy?.name || !this.newPolicy?.type) {
      this.alertService.adminError('Validation Error', 'Please enter policy name and type.');
      return;
    }
    this.creatingPolicy = true;
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/policies`, this.newPolicy).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.adminSuccess('Policy Created', res.message || 'Insurance policy created.');
          this.showCreatePolicy = false;
          this.loadPolicies();
          this.loadStats();
          this.newPolicy = {
            name: '',
            type: 'Health',
            coverageAmount: 100000,
            premiumAmount: 999,
            premiumType: 'MONTHLY',
            durationMonths: 12,
            description: '',
            benefits: '',
            eligibility: '',
            status: 'ACTIVE'
          };
        } else {
          this.alertService.adminError('Failed', res?.message || 'Unable to create policy.');
        }
        this.creatingPolicy = false;
      },
      error: (err) => {
        this.creatingPolicy = false;
        this.alertService.adminError('Failed', err.error?.message || 'Unable to create policy.');
      }
    });
  }

  approveApplication(app: any) {
    if (!app?.id) return;
    this.processingApplicationId = app.id;
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/applications/${app.id}/approve`, {}).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.adminSuccess('Application Approved', res.message || 'Application approved.');
          this.loadPendingApplications();
          this.loadStats();
        } else {
          this.alertService.adminError('Failed', res?.message || 'Unable to approve application.');
        }
        this.processingApplicationId = null;
      },
      error: (err) => {
        this.processingApplicationId = null;
        this.alertService.adminError('Failed', err.error?.message || 'Unable to approve application.');
      }
    });
  }

  rejectApplication(app: any) {
    if (!app?.id) return;
    const remark = prompt('Enter rejection remark (optional):') || '';
    this.processingApplicationId = app.id;
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/applications/${app.id}/reject?remark=${encodeURIComponent(remark)}`, {}).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.adminSuccess('Application Rejected', res.message || 'Application rejected.');
          this.loadPendingApplications();
          this.loadStats();
        } else {
          this.alertService.adminError('Failed', res?.message || 'Unable to reject application.');
        }
        this.processingApplicationId = null;
      },
      error: (err) => {
        this.processingApplicationId = null;
        this.alertService.adminError('Failed', err.error?.message || 'Unable to reject application.');
      }
    });
  }

  approveAutoDebit(app: any) {
    if (!app?.id) return;
    const remark = prompt('Enter auto-debit approval remark (optional):') || '';
    this.processingApplicationId = app.id;
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/applications/${app.id}/auto-debit/approve?remark=${encodeURIComponent(remark)}`, {}).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.adminSuccess('Auto Debit Approved', res.message || 'Auto-debit approved.');
          this.loadPendingApplications();
        } else {
          this.alertService.adminError('Failed', res?.message || 'Unable to approve auto-debit.');
        }
        this.processingApplicationId = null;
      },
      error: (err) => {
        this.processingApplicationId = null;
        this.alertService.adminError('Failed', err.error?.message || 'Unable to approve auto-debit.');
      }
    });
  }

  approveClaim(claim: any) {
    if (!claim?.id) return;
    this.processingClaimId = claim.id;
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/claims/${claim.id}/approve`, {}).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.adminSuccess('Claim Approved', res.message || 'Claim approved.');
          this.loadPendingClaims();
          this.loadStats();
        } else {
          this.alertService.adminError('Failed', res?.message || 'Unable to approve claim.');
        }
        this.processingClaimId = null;
      },
      error: (err) => {
        this.processingClaimId = null;
        this.alertService.adminError('Failed', err.error?.message || 'Unable to approve claim.');
      }
    });
  }

  payoutClaim(claim: any) {
    if (!claim?.id) return;
    if (!confirm('Are you sure you want to pay out this claim?')) {
      return;
    }
    this.processingClaimId = claim.id;
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/claims/${claim.id}/payout`, {}).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.adminSuccess('Payout Complete', res.message || 'Claim payout processed.');
          this.loadPendingClaims();
          this.loadStats();
        } else {
          this.alertService.adminError('Failed', res?.message || 'Unable to process payout.');
        }
        this.processingClaimId = null;
      },
      error: (err) => {
        this.processingClaimId = null;
        this.alertService.adminError('Failed', err.error?.message || 'Unable to process payout.');
      }
    });
  }

  goToKyc(app: any) {
    if (!app?.accountNumber) return;
    this.router.navigate(['/admin/kyc'], {
      queryParams: { accountNumber: app.accountNumber }
    });
  }
}

