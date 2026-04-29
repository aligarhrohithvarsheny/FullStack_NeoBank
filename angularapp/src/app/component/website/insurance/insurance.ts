import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-insurance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './insurance.html',
  styleUrls: ['./insurance.css']
})
export class Insurance implements OnInit {
  availablePolicies: any[] = [];
  applications: any[] = [];
  claims: any[] = [];
  payments: any[] = [];
  // set of policyNumbers the current user has applied for (quick lookup)
  appPolicyNumbers: Set<string> = new Set();

  activeTab: 'policies' | 'my-policies' | 'claims' | 'payments' = 'policies';

  // Application form
  selectedPolicyId: number | null = null;
  nomineeName: string = '';
  nomineeRelation: string = '';
  premiumType: 'MONTHLY' | 'YEARLY' = 'MONTHLY';
  kycDocumentPath: string = '';
  proposerAge: number | null = null;
  healthConditions: string = '';
  healthConditionOptions: string[] = [
    'Diabetes',
    'Hypertension (BP)',
    'Asthma',
    'Thyroid',
    'Heart Disease',
    'Kidney Disease',
    'Liver Disease',
    'Cancer',
    'Stroke',
    'COPD',
    'Arthritis',
    'Epilepsy',
    'Mental Health',
    'HIV/AIDS',
    'None'
  ];
  selectedHealthConditions: string[] = [];
  otherHealthCondition: string = '';
  lifestyleHabits: string = '';
  hasExistingEmis: boolean = false;
  isApplying: boolean = false;
  premiumQuote: { base: number; addAge: number; addDisease: number; addEmi: number; total: number } | null = null;

  // Claim form
  selectedApplicationIdForClaim: number | null = null;
  claimAmount: number = 0;
  claimDocumentsPath: string = '';
  isSubmittingClaim: boolean = false;

  // Premium payment form
  selectedApplicationIdForPayment: number | null = null;
  premiumAmount: number = 0;
  enableAutoDebit: boolean = false;
  isPayingPremium: boolean = false;

  // EMI calculator
  premiumCalculator = {
    coverageAmount: 100000,
    durationMonths: 12,
    annualRate: 8.0
  };
  calculatedMonthlyPremium: number | null = null;

  // Session
  userAccountNumber: string = '';
  userId: number | null = null;

  get visiblePolicies(): any[] {
    return (this.availablePolicies || []).filter(p => !p.unavailable);
  }

  constructor(
    private http: HttpClient,
    private router: Router,
    private alertService: AlertService,
    @Inject(PLATFORM_ID) private platformId: Object,
    private route: ActivatedRoute
  ) {}

  get selectedPolicy(): any | null {
    if (!this.selectedPolicyId) return null;
    return this.availablePolicies.find(p => Number(p.id) === Number(this.selectedPolicyId)) || null;
  }

  ngOnInit() {
    // Avoid SSR/runtime errors where sessionStorage is not available
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const currentUserRaw = sessionStorage.getItem('currentUser');
    if (!currentUserRaw) {
      this.alertService.userError('Login Required', 'Please login to access insurance services.');
      this.router.navigate(['/website/user'], {
        queryParams: { redirectTo: 'insurance' }
      });
      return;
    }
    try {
      const currentUser = JSON.parse(currentUserRaw);
      this.userAccountNumber = currentUser.accountNumber;
      this.userId = Number(currentUser.id);
    } catch {
      this.alertService.userError('Session Error', 'Unable to read user session. Please login again.');
      this.router.navigate(['/website/user']);
      return;
    }

    this.loadAllData();

    // If opened via policy lookup (website/insurance?policy=POL123), auto-select that policy.
    this.route.queryParams.subscribe(params => {
      const policyNumber = params['policy'];
      if (policyNumber) {
        // lookup policy by number and set selectedPolicyId when found
        this.http.get<any>(`${environment.apiBaseUrl}/api/insurance/policy/lookup/${encodeURIComponent(policyNumber)}`).subscribe({
          next: (res) => {
            if (res?.success && res.policy) {
              this.selectedPolicyId = Number(res.policy.id || res.policyId || null);
              this.activeTab = 'policies';
              // reload policies to ensure it's in the list
              this.loadPolicies();
            } else {
              // silently ignore if not found; optionally show a message
            }
          },
          error: () => {
            // ignore lookup errors
          }
        });
      }
    });
  }

  setTab(tab: 'policies' | 'my-policies' | 'claims' | 'payments') {
    this.activeTab = tab;
  }

  loadAllData() {
    this.loadPolicies();
    this.loadApplicationsAndLookup();
    this.loadClaims();
    this.loadPayments();
  }

  loadPolicies() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/insurance/policies`).subscribe({
      next: (res) => {
        this.availablePolicies = res || [];
        // For each policy, check whether it's already assigned to some account
        // If assigned to another account, mark it as unavailable so we don't show it
        this.availablePolicies.forEach(policy => {
          try {
            const pn = policy.policyNumber || policy.policyNumberString || '';
            if (!pn) { policy.unavailable = false; return; }
            this.http.get<any>(`${environment.apiBaseUrl}/api/insurance/policy/lookup/${encodeURIComponent(pn)}`).subscribe({
              next: (info) => {
                const assignedAccount = info?.accountNumber || null;
                policy.assignedAccount = assignedAccount;
                policy.assignedApplication = info?.application || null;
                policy.unavailable = !!assignedAccount && (assignedAccount.toString() !== (this.userAccountNumber || '').toString());
              },
              error: () => {
                policy.assignedAccount = null;
                policy.assignedApplication = null;
                policy.unavailable = false;
              }
            });
          } catch (e) {
            policy.unavailable = false;
          }
        });
      },
      error: () => { this.availablePolicies = []; }
    });
  }

  loadApplications() {
    if (!this.userAccountNumber) return;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/insurance/applications/account/${this.userAccountNumber}`).subscribe({
      next: (res) => { this.applications = res || []; },
      error: () => { this.applications = []; }
    });
  }

  loadApplicationsAndLookup() {
    if (!this.userAccountNumber) return;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/insurance/applications/account/${this.userAccountNumber}`).subscribe({
      next: (res) => {
        // defensive filtering: ensure only applications belonging to this account are kept
        const list = (res || []).filter(a => (a.accountNumber || '').toString() === (this.userAccountNumber || '').toString());
        this.applications = list;
        this.updateApplicationLookup();
      },
      error: () => { this.applications = []; this.updateApplicationLookup(); }
    });
  }

  // after loading applications, compute quick lookup set
  private updateApplicationLookup() {
    try {
      this.appPolicyNumbers = new Set((this.applications || []).map(a => (a.policy?.policyNumber || '').toString()));
    } catch {
      this.appPolicyNumbers = new Set();
    }
  }

  // helper to check if user already applied for a policy
  hasApplied(policy: any): boolean {
    if (!policy) return false;
    return this.appPolicyNumbers.has((policy.policyNumber || '').toString());
  }

  // get application object for a given policy (if any)
  getApplicationForPolicy(policy: any): any | null {
    if (!policy) return null;
    return (this.applications || []).find(a => (a.policy?.policyNumber || '').toString() === (policy.policyNumber || '').toString()) || null;
  }

  loadClaims() {
    if (!this.userAccountNumber) return;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/insurance/claims/account/${this.userAccountNumber}`).subscribe({
      next: (res) => { this.claims = res || []; },
      error: () => { this.claims = []; }
    });
  }

  loadPayments() {
    if (!this.userAccountNumber) return;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/insurance/payments/account/${this.userAccountNumber}`).subscribe({
      next: (res) => { this.payments = res || []; },
      error: () => { this.payments = []; }
    });
  }

  applyForPolicy() {
    if (!this.userId || !this.selectedPolicyId || !this.nomineeName) {
      this.alertService.userError('Validation Error', 'Please select a policy and enter nominee details.');
      return;
    }
    this.isApplying = true;

    const health = this.buildHealthConditionsPayload();
    const payload = {
      userId: this.userId,
      policyId: this.selectedPolicyId,
      nomineeName: this.nomineeName,
      nomineeRelation: this.nomineeRelation,
      kycDocumentPath: this.kycDocumentPath,
      premiumType: this.premiumType,
      proposerAge: this.proposerAge,
      healthConditions: health,
      lifestyleHabits: this.lifestyleHabits,
      hasExistingEmis: this.hasExistingEmis
    };
    this.http.post(`${environment.apiBaseUrl}/api/insurance/policies/apply`, payload).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.userSuccess('Application Submitted', res.message || 'Your insurance application is pending approval.');
          this.resetApplicationForm();
          this.loadApplications();
          this.activeTab = 'my-policies';
        } else {
          this.alertService.userError('Application Failed', res?.message || 'Unable to submit application.');
        }
        this.isApplying = false;
      },
      error: (err) => {
        this.isApplying = false;
        this.alertService.userError('Application Failed', err.error?.message || 'Unable to submit application.');
      }
    });
  }

  resetApplicationForm() {
    this.selectedPolicyId = null;
    this.nomineeName = '';
    this.nomineeRelation = '';
    this.premiumType = 'MONTHLY';
    this.kycDocumentPath = '';
    this.proposerAge = null;
    this.healthConditions = '';
    this.selectedHealthConditions = [];
    this.otherHealthCondition = '';
    this.lifestyleHabits = '';
    this.hasExistingEmis = false;
    this.premiumQuote = null;
  }

  submitClaim() {
    if (!this.selectedApplicationIdForClaim || this.claimAmount <= 0) {
      this.alertService.userError('Validation Error', 'Please select a policy and enter a valid claim amount.');
      return;
    }
    this.isSubmittingClaim = true;
    const payload = {
      applicationId: this.selectedApplicationIdForClaim,
      claimAmount: this.claimAmount,
      documentsPath: this.claimDocumentsPath
    };
    this.http.post(`${environment.apiBaseUrl}/api/insurance/claims/request`, payload).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.userSuccess('Claim Submitted', res.message || 'Your claim request has been submitted.');
          this.selectedApplicationIdForClaim = null;
          this.claimAmount = 0;
          this.claimDocumentsPath = '';
          this.loadClaims();
          this.activeTab = 'claims';
        } else {
          this.alertService.userError('Claim Failed', res?.message || 'Unable to submit claim.');
        }
        this.isSubmittingClaim = false;
      },
      error: (err) => {
        this.isSubmittingClaim = false;
        this.alertService.userError('Claim Failed', err.error?.message || 'Unable to submit claim.');
      }
    });
  }

  payPremium() {
    if (!this.selectedApplicationIdForPayment || this.premiumAmount <= 0) {
      this.alertService.userError('Validation Error', 'Please select a policy and enter a valid amount.');
      return;
    }
    this.isPayingPremium = true;
    const payload = {
      applicationId: this.selectedApplicationIdForPayment,
      amount: this.premiumAmount,
      autoDebitEnabled: this.enableAutoDebit,
      merchant: 'NeoBank Insurance'
    };
    this.http.post(`${environment.apiBaseUrl}/api/insurance/payments`, payload).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.alertService.userSuccess('Premium Paid', res.message || 'Premium payment successful.');
          this.selectedApplicationIdForPayment = null;
          this.premiumAmount = 0;
          this.enableAutoDebit = false;
          this.loadPayments();
          this.loadApplications();
          this.activeTab = 'payments';
        } else {
          this.alertService.userError('Payment Failed', res?.message || 'Unable to process premium payment.');
        }
        this.isPayingPremium = false;
      },
      error: (err) => {
        this.isPayingPremium = false;
        this.alertService.userError('Payment Failed', err.error?.message || 'Unable to process premium payment.');
      }
    });
  }

  goBackToDashboard() {
    this.router.navigate(['/website/userdashboard']);
  }

  buildHealthConditionsPayload(): string {
    const chosen = (this.selectedHealthConditions || []).filter(Boolean);
    const other = (this.otherHealthCondition || '').trim();

    if (chosen.includes('None')) {
      return '';
    }
    const all = other ? [...chosen.filter(x => x !== 'None'), other] : chosen.filter(x => x !== 'None');
    const unique = Array.from(new Set(all.map(x => x.trim()).filter(Boolean)));
    return unique.join(', ');
  }

  updatePremiumQuote() {
    const policy = this.selectedPolicy;
    if (!policy) {
      this.premiumQuote = null;
      return;
    }
    const base = Number(policy.premiumAmount || 0);
    const cycle = (this.premiumType || 'MONTHLY').toUpperCase();
    const age = Number(this.proposerAge || 0);
    const health = this.buildHealthConditionsPayload();
    const hasDisease = !!health && health.trim().length > 0;
    const hasEmi = !!this.hasExistingEmis;

    const addAge = cycle === 'YEARLY' ? (age >= 45 ? 5000 : 0) : (age >= 45 ? 500 : 0);
    const addDisease = cycle === 'YEARLY' ? (hasDisease ? 3000 : 0) : (hasDisease ? 300 : 0);
    const addEmi = cycle === 'YEARLY' ? (hasEmi ? 2000 : 0) : (hasEmi ? 200 : 0);
    const total = Math.max(0, base + addAge + addDisease + addEmi);
    this.premiumQuote = { base, addAge, addDisease, addEmi, total };
  }

  downloadCertificate(applicationId: number) {
    if (!applicationId) return;
    this.http.get(`${environment.apiBaseUrl}/api/insurance/applications/${applicationId}/certificate`, { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `policy-certificate-${applicationId}.pdf`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: () => {
          this.alertService.userError('Download Failed', 'Unable to download certificate right now.');
        }
      });
  }

  calculatePremium() {
    const P = this.premiumCalculator.coverageAmount;
    const n = this.premiumCalculator.durationMonths;
    const rMonthly = (this.premiumCalculator.annualRate / 100) / 12;
    if (P <= 0 || n <= 0 || rMonthly <= 0) {
      this.calculatedMonthlyPremium = null;
      return;
    }
    const factor = Math.pow(1 + rMonthly, n);
    const emi = (P * rMonthly * factor) / (factor - 1);
    this.calculatedMonthlyPremium = Math.round(emi);
  }
}

