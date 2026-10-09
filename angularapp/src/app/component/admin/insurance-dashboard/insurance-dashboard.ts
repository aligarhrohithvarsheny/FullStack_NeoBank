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
  allApplications: any[] = [];
  approvedGuestApplications: any[] = [];
  bankPremiumDrafts: Record<number, { amount: number; type: 'MONTHLY' | 'YEARLY' }> = {};
  guestPremiumDrafts: Record<number, { amount: number; type: 'MONTHLY' | 'YEARLY' }> = {};
  savingPremiumKey = '';
  customers: any[] = [];

  // Assign policy
  customerSearch: string = '';
  selectedCustomer: any = null;
  selectedPolicyIdForAssign: number | null = null;
  selectedPremiumTypeForAssign: 'MONTHLY' | 'YEARLY' = 'MONTHLY';
  assigningPolicy: boolean = false;
  verifiedLinkedAccount: any = null;
  vehicleDetails: any = { vehicleNumber: '', makeModel: '', chassisNumber: '', engineNumber: '', registrationDate: '' };
  selectedInsuranceFiles: File[] = [];
  editingApplicationId: number | null = null;

  // Tab navigation
  activeTab: 'policies' | 'applications' | 'approved-applications' | 'guest-applications' | 'claims' | 'customers' = 'policies';
  pendingGuestApplications: any[] = [];
  pendingGuestClaims: any[] = [];
  reviewingGuestApplicationId: number | null = null;
  reviewingGuestClaimId: number | null = null;
  reviewerEmail = '';
  reviewerPassword = '';
  guestApplicationsUnlocked = false;

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
    termsAndConditions: '',
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
    this.loadAllApplications();
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

  loadPendingGuestApplications() {
    if (!this.reviewerEmail.trim() || !this.reviewerPassword) return;
    const headers = {
      'X-Admin-Email': this.reviewerEmail.trim().toLowerCase(),
      'X-Admin-Password': this.reviewerPassword
    };
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admin/insurance/guest-applications/pending`, { headers }).subscribe({
      next: (res) => {
        this.pendingGuestApplications = res || [];
        this.guestApplicationsUnlocked = true;
        this.loadPendingGuestClaims();
      },
      error: (error) => {
        this.pendingGuestApplications = [];
        this.guestApplicationsUnlocked = false;
        this.alertService.adminError('Admin Verification Failed', error.error?.message || 'Verify your admin email and password.');
      }
    });
  }

  loadPendingGuestClaims(): void {
    if (!this.reviewerEmail.trim() || !this.reviewerPassword) return;
    const headers = {
      'X-Admin-Email': this.reviewerEmail.trim().toLowerCase(),
      'X-Admin-Password': this.reviewerPassword
    };
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admin/insurance/guest-claims/pending`, { headers }).subscribe({
      next: claims => this.pendingGuestClaims = claims || [],
      error: () => this.pendingGuestClaims = []
    });
  }

  reviewGuestApplication(application: any, approve: boolean): void {
    if (!application?.id || this.reviewingGuestApplicationId === application.id) return;
    this.reviewingGuestApplicationId = application.id;
    const headers = {
      'X-Admin-Email': this.reviewerEmail.trim().toLowerCase(),
      'X-Admin-Password': this.reviewerPassword
    };
    this.http.post<any>(
      `${environment.apiBaseUrl}/api/admin/insurance/guest-applications/${application.id}/review`,
      { approve, remark: '' },
      { headers }
    ).subscribe({
      next: response => {
        this.reviewingGuestApplicationId = null;
        if (!response?.success) {
          this.alertService.adminError('Review Failed', response?.message || 'Unable to review application.');
          return;
        }
        this.alertService.adminSuccess(
          approve ? 'Application Approved' : 'Application Rejected',
          response.message || 'Guest insurance application reviewed.'
        );
        this.loadPendingGuestApplications();
      },
      error: error => {
        this.reviewingGuestApplicationId = null;
        const message = error.status === 404
          ? 'The guest-insurance review endpoint is missing from the backend deployment. Please deploy the latest Spring backend and try again.'
          : error.error?.message || 'Unable to review application.';
        this.alertService.adminError('Review Failed', message);
      }
    });
  }

  reviewGuestClaim(claim: any, approve: boolean): void {
    if (!claim?.id || this.reviewingGuestClaimId === claim.id) return;
    this.reviewingGuestClaimId = claim.id;
    const headers = {
      'X-Admin-Email': this.reviewerEmail.trim().toLowerCase(),
      'X-Admin-Password': this.reviewerPassword
    };
    this.http.post<any>(
      `${environment.apiBaseUrl}/api/admin/insurance/guest-claims/${claim.id}/review`,
      { approve, remark: '' },
      { headers }
    ).subscribe({
      next: response => {
        this.reviewingGuestClaimId = null;
        if (!response?.success) {
          this.alertService.adminError('Claim Review Failed', response?.message || 'Unable to review claim.');
          return;
        }
        this.alertService.adminSuccess(
          approve ? 'Claim Approved' : 'Claim Rejected',
          response.message || 'Guest insurance claim reviewed.'
        );
        this.loadPendingGuestClaims();
      },
      error: error => {
        this.reviewingGuestClaimId = null;
        this.alertService.adminError('Claim Review Failed', error.error?.message || 'Unable to review claim.');
      }
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

  loadAllApplications() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admin/insurance/applications/all`).subscribe({
      next: (res) => {
        this.allApplications = res || [];
        this.bankPremiumDrafts = {};
        this.allApplications.forEach(application => {
          this.bankPremiumDrafts[application.id] = {
            amount: Number(application.premiumAmountCalculated ?? application.policy?.premiumAmount ?? 0),
            type: application.premiumType === 'YEARLY' ? 'YEARLY' : 'MONTHLY'
          };
        });
      },
      error: error => this.alertService.adminError(
        'Applications Unavailable',
        error.error?.message || 'Unable to load insurance applications.'
      )
    });
  }

  get approvedBankApplications(): any[] {
    return this.allApplications.filter(application =>
      ['APPROVED', 'ACTIVE', 'CLOSED'].includes(String(application.status || '').toUpperCase()));
  }

  openApprovedApplications(): void {
    this.activeTab = 'approved-applications';
    this.loadAllApplications();
    if (this.guestApplicationsUnlocked) this.loadApprovedGuestApplications();
  }

  loadApprovedGuestApplications(): void {
    if (!this.reviewerEmail.trim() || !this.reviewerPassword) return;
    const headers = {
      'X-Admin-Email': this.reviewerEmail.trim().toLowerCase(),
      'X-Admin-Password': this.reviewerPassword
    };
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admin/insurance/guest-applications/approved`, { headers }).subscribe({
      next: applications => {
        this.approvedGuestApplications = applications || [];
        this.guestPremiumDrafts = {};
        this.approvedGuestApplications.forEach(application => {
          this.guestPremiumDrafts[application.id] = {
            amount: Number(application.premiumAmountOverride ?? application.policy?.premiumAmount ?? 0),
            type: (application.premiumTypeOverride ?? application.policy?.premiumType) === 'YEARLY' ? 'YEARLY' : 'MONTHLY'
          };
        });
        this.guestApplicationsUnlocked = true;
      },
      error: error => {
        this.approvedGuestApplications = [];
        this.guestApplicationsUnlocked = false;
        this.alertService.adminError('Applications Unavailable', error.error?.message || 'Unable to load approved guest applications.');
      }
    });
  }

  saveBankPremium(application: any): void {
    const draft = this.bankPremiumDrafts[application.id];
    if (!draft || this.savingPremiumKey) return;
    this.savingPremiumKey = `bank-${application.id}`;
    this.http.put<any>(
      `${environment.apiBaseUrl}/api/admin/insurance/applications/${application.id}/premium`,
      { premiumAmount: draft.amount, premiumType: draft.type }
    ).subscribe({
      next: response => {
        this.savingPremiumKey = '';
        if (!response?.success) {
          this.alertService.adminError('Premium Update Failed', response?.message || 'Unable to update premium.');
          return;
        }
        this.alertService.adminSuccess('Premium Updated', response.message || 'Application premium updated.');
        this.loadAllApplications();
      },
      error: error => {
        this.savingPremiumKey = '';
        this.alertService.adminError('Premium Update Failed', error.error?.message || 'Unable to update premium.');
      }
    });
  }

  saveGuestPremium(application: any): void {
    const draft = this.guestPremiumDrafts[application.id];
    if (!draft || this.savingPremiumKey) return;
    this.savingPremiumKey = `guest-${application.id}`;
    const headers = {
      'X-Admin-Email': this.reviewerEmail.trim().toLowerCase(),
      'X-Admin-Password': this.reviewerPassword
    };
    this.http.put<any>(
      `${environment.apiBaseUrl}/api/admin/insurance/guest-applications/${application.id}/premium`,
      { premiumAmount: draft.amount, premiumType: draft.type },
      { headers }
    ).subscribe({
      next: response => {
        this.savingPremiumKey = '';
        if (!response?.success) {
          this.alertService.adminError('Premium Update Failed', response?.message || 'Unable to update guest premium.');
          return;
        }
        this.alertService.adminSuccess('Premium Updated', response.message || 'Guest application premium updated.');
        this.loadApprovedGuestApplications();
      },
      error: error => {
        this.savingPremiumKey = '';
        this.alertService.adminError('Premium Update Failed', error.error?.message || 'Unable to update guest premium.');
      }
    });
  }

  downloadPaymentReceipt(payment: any, guest = false): void {
    if (!payment?.id) return;
    const headers = guest ? {
      'X-Admin-Email': this.reviewerEmail.trim().toLowerCase(),
      'X-Admin-Password': this.reviewerPassword
    } : undefined;
    const options: { responseType: 'blob'; headers?: Record<string, string> } = { responseType: 'blob' };
    if (headers) options.headers = headers;
    const path = guest
      ? `/api/admin/insurance/guest-payments/${payment.id}/receipt`
      : `/api/admin/insurance/payments/${payment.id}/receipt`;
    this.http.get(`${environment.apiBaseUrl}${path}`, options).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `insurance-receipt-${payment.reference || payment.id}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: error => this.alertService.adminError(
        'Receipt Unavailable',
        error.error?.message || 'Unable to download this payment receipt.'
      )
    });
  }

  getApplicationForPolicy(policy: any): any {
    return this.allApplications.find(app => app.policy?.id === policy?.id || app.policy?.policyNumber === policy?.policyNumber);
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
    this.verifySelectedAccount();
  }

  verifySelectedAccount() {
    if (!this.selectedCustomer?.accountNumber) return;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admin/insurance/accounts/verify?accountNumber=${encodeURIComponent(this.selectedCustomer.accountNumber)}&customerName=${encodeURIComponent(this.selectedCustomer.account?.name || this.selectedCustomer.name || '')}`).subscribe({
      next: (res) => this.verifiedLinkedAccount = res,
      error: () => this.verifiedLinkedAccount = null
    });
  }

  onInsuranceFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedInsuranceFiles = input.files ? Array.from(input.files) : [];
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
      customerName: this.selectedCustomer.account?.name || this.selectedCustomer.name || this.selectedCustomer.username,
      ...this.vehicleDetails,
      linkedAccountType: this.verifiedLinkedAccount?.accountType
    };
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/assign-policy`, payload).subscribe({
      next: (res: any) => {
        if (res?.success) {
          if (this.selectedInsuranceFiles.length && res.application?.id) this.uploadInsuranceDocuments(res.application.id);
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

  uploadInsuranceDocuments(applicationId: number) {
    const form = new FormData();
    this.selectedInsuranceFiles.forEach(file => form.append('files', file));
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/applications/${applicationId}/documents`, form).subscribe();
  }

  editApplication(app: any) {
    const updates = { vehicleNumber: app.vehicleNumber, makeModel: app.makeModel, chassisNumber: app.chassisNumber, engineNumber: app.engineNumber, registrationDate: app.registrationDate, nomineeName: app.nomineeName, nomineeRelation: app.nomineeRelation };
    this.editingApplicationId = app.id;
    this.http.put(`${environment.apiBaseUrl}/api/admin/insurance/applications/${app.id}`, updates).subscribe({ next: () => { this.editingApplicationId = null; this.loadPendingApplications(); }, error: () => this.editingApplicationId = null });
  }

  renewApplication(app: any) {
    this.http.post(`${environment.apiBaseUrl}/api/admin/insurance/applications/${app.id}/renew?renewedBy=Admin`, {}).subscribe({ next: (res: any) => { if (res?.success) { this.alertService.adminSuccess('Insurance Renewed', 'Policy renewal saved.'); this.loadPendingApplications(); } }, error: (err) => this.alertService.adminError('Renewal Failed', err.error?.message || 'Unable to renew policy') });
  }

  downloadCertificate(app: any) {
    if (!app?.id) {
      this.alertService.adminError('Certificate', 'No customer application is linked to this policy yet.');
      return;
    }
    this.http.get(`${environment.apiBaseUrl}/api/admin/insurance/applications/${app.id}/certificate`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `insurance-certificate-${app.applicationNumber || app.id}.pdf`;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.alertService.adminError('Certificate', 'Certificate could not be generated. Approve and pay the application first.')
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
            termsAndConditions: '',
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
          this.pendingApplications = this.pendingApplications.filter(application => application.id !== app.id);
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
          this.pendingApplications = this.pendingApplications.filter(application => application.id !== app.id);
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
