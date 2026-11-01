import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';
import { openLoanDocument, isLoanClosed } from '../../../service/loan-documents';
import { LoanAgreementEvent, LoanAgreementService } from '../../../service/loan-agreement.service';
import { Subscription } from 'rxjs';

interface GoldLoan {
  id?: number;
  goldGrams: number;
  goldRatePerGram: number;
  goldValue: number;
  loanAmount: number;
  tenure: number;
  interestRate: number;
  status: string;
  userName: string;
  userEmail: string;
  accountNumber: string;
  customerId?: string; // Mapped from account for admin display
  currentBalance: number;
  remainingPrincipal?: number;
  loanAccountNumber?: string;
  mobile?: string;
  applicationDate?: string;
  approvalDate?: string;
  approvedBy?: string;
  goldItems?: string;
  goldDescription?: string;
  goldPurity?: string;
  verifiedGoldGrams?: number;
  verifiedGoldValue?: number;
  verificationNotes?: string;
  storageLocation?: string;
  termsAccepted?: boolean;
  termsAcceptedDate?: string;
  termsAcceptedBy?: string;
  otpVerified?: boolean;
  processingCharges?: number;
  renewalCount?: number;
  lastRenewalDate?: string;
  agreementId?: number;
  agreementStatus?: string;
  shareUrl?: string;
}

interface GoldRate {
  id?: number;
  date: string;
  ratePerGram: number;
  lastUpdated?: string;
}

@Component({
  selector: 'app-admin-gold-loans',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './goldloans.html',
  styleUrls: ['./goldloans.css']
})
export class AdminGoldLoans implements OnInit, OnDestroy {
  goldLoans: GoldLoan[] = [];
  filteredLoans: GoldLoan[] = [];
  loading: boolean = false;
  error: string = '';
  
  // Filter
  filterStatus: string = 'Pending';
  searchTerm: string = '';
  fromDate: string = '';
  toDate: string = '';
  
  // Selected loan for approval
  selectedLoan: GoldLoan | null = null;
  approving: boolean = false;
  adminName: string = 'Admin';
  adminId: number | null = null;
  verifiedAllocations: any[] = [];
  expiredVerifiedAllocationsFound = false;
  
  // Gold details form (for acceptance)
  showGoldDetailsModal: boolean = false;
  goldDetailsForm: any = {
    allocationId: null,
    performedByAdminId: null,
    goldItems: '',
    goldDescription: '',
    goldPurity: '22K',
    verifiedGoldGrams: 0,
    verificationNotes: '',
    storageLocation: ''
  };
  jewelleryImageOne: File | null = null;
  jewelleryImageTwo: File | null = null;
  
  // Calculated values for display
  calculatedVerifiedValue: number = 0;
  calculatedLoanAmount: number = 0;
  calculatedEMI: number = 0;
  calculatedTotalInterest: number = 0;
  
  // Gold rate management
  currentGoldRate: GoldRate | null = null;
  newGoldRate: number = 0;
  updatingRate: boolean = false;

  // Foreclosure management
  showForeclosureModal: boolean = false;
  foreclosureDetails: any = null;
  editingForeclosure: boolean = false;
  editedForeclosureAmount: number = 0;
  editedInterestRate: number = 0;
  processingForeclosure: boolean = false;
  calculatedPayableAmount: number = 0;
  calculatedInterestAmount: number = 0;

  // EMI Details
  showEmiDetailsModal: boolean = false;
  emiSchedule: any[] = [];

  // Edit loan (amount / gold details / approve time / processing charges)
  showEditModal: boolean = false;
  editingLoan: boolean = false;
  editForm: any = {
    loanAmount: 0,
    goldItems: '',
    goldDescription: '',
    goldPurity: '22K',
    verifiedGoldGrams: 0,
    storageLocation: '',
    verificationNotes: '',
    interestRate: 12,
    tenure: 12,
    processingCharges: 0,
    approvalDate: '',
    remarks: ''
  };

  // Renewal
  showRenewModal: boolean = false;
  renewingLoan: boolean = false;
  renewForm: any = {
    additionalTenure: 6,
    interestRate: null,
    processingCharges: 0,
    remarks: ''
  };

  // History
  showHistoryModal: boolean = false;
  historyList: any[] = [];
  loadingHistory: boolean = false;

  // Admin-created application
  showNewApplicationModal = false;
  lookingUpApplicant = false;
  creatingApplication = false;
  applicantLookupComplete = false;
  applicantAccountNumber = '';
  applicant: { name: string; email: string; phone: string; balance: number } | null = null;
  newApplicationForm = {
    goldGrams: 0,
    tenure: 12,
    interestRate: 12,
    goldPurity: '22K',
    goldItems: '',
    goldDescription: ''
  };

  // Cheque-funded gold-loan prepayment
  showChequePaymentModal = false;
  verifyingPaymentCheque = false;
  processingChequePayment = false;
  paymentChequeNumber = '';
  paymentAmount: number | null = null;
  paymentAdjustment: 'REDUCE_EMI' | 'REDUCE_TENURE' = 'REDUCE_EMI';
  paymentChequeVerification: any = null;
  private agreementEvents?: Subscription;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private agreementService: LoanAgreementService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadAdminInfo();
      this.loadGoldLoans();
      this.loadCurrentGoldRate();
      this.agreementService.connectToUpdates();
      this.agreementEvents = this.agreementService.events$.subscribe(event => this.applyAgreementEvent(event));
    }
  }

  ngOnDestroy(): void {
    this.agreementEvents?.unsubscribe();
  }

  private staffHeaders(): HttpHeaders {
    const token = sessionStorage.getItem('adminSessionToken');
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }

  private loadAgreementSummaries(): void {
    this.agreementService.getAdminAgreements().subscribe({
      next: summaries => {
        const latest = new Map<number, any>();
        summaries.filter(item => item.loanType === 'GOLD_LOAN').forEach(item => {
          if (!latest.has(item.loanId)) latest.set(item.loanId, item);
        });
        this.goldLoans.forEach(loan => {
          const agreement = latest.get(loan.id || 0);
          if (agreement) {
            loan.agreementId = agreement.id;
            loan.agreementStatus = agreement.status;
          }
        });
        this.onFilterChange();
      },
      error: error => console.error('Could not load gold-loan agreement status.', error)
    });
  }

  private applyAgreementEvent(event: LoanAgreementEvent): void {
    if (event.loanType !== 'GOLD_LOAN') return;
    const loan = this.goldLoans.find(item => item.id === event.loanId);
    if (loan) {
      loan.agreementId = event.agreementId;
      loan.agreementStatus = event.status;
      if (event.status === 'APPROVED') loan.status = 'Approved';
      this.onFilterChange();
    }
  }
  loadAdminInfo() {
    const admin = sessionStorage.getItem('admin');
    if (admin) {
      const adminData = JSON.parse(admin);
      this.adminName = adminData.name || adminData.username || 'Admin';
      this.adminId = adminData.id || null;
    }
  }

  loadGoldLoans() {
    this.loading = true;
    this.error = '';
    
    this.http.get(`${environment.apiBaseUrl}/api/gold-loans`).subscribe({
      next: (loans: any) => {
        this.goldLoans = Array.isArray(loans) ? loans : [];
        this.loadAgreementSummaries();
        this.filterLoans();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading gold loans:', err);
        this.error = 'Failed to load gold loans. Please try again.';
        this.loading = false;
      }
    });
  }

  filterLoans() {
    const term = this.searchTerm.trim().toLowerCase();
    const digits = term.replace(/[,\s₹]/g, '');
    this.filteredLoans = this.goldLoans.filter(loan => {
      if (this.filterStatus !== 'All' && loan.status !== this.filterStatus) {
        return false;
      }
      if (term) {
        const textFields = [loan.accountNumber, loan.loanAccountNumber, loan.userName, loan.mobile]
          .map(v => (v ?? '').toString().toLowerCase());
        const amounts = [loan.loanAmount, loan.goldValue]
          .filter(v => v !== null && v !== undefined)
          .map(v => String(v));
        const matches = textFields.some(v => v.includes(term)) ||
          (digits !== '' && amounts.some(v => v.includes(digits)));
        if (!matches) {
          return false;
        }
      }
      if (this.fromDate || this.toDate) {
        if (!loan.applicationDate) {
          return false;
        }
        const day = loan.applicationDate.substring(0, 10);
        if (this.fromDate && day < this.fromDate) {
          return false;
        }
        if (this.toDate && day > this.toDate) {
          return false;
        }
      }
      return true;
    });
  }

  clearSearch() {
    this.searchTerm = '';
    this.fromDate = '';
    this.toDate = '';
    this.filterLoans();
  }

  onFilterChange() {
    this.filterLoans();
  }

  loadCurrentGoldRate() {
    this.http.get(`${environment.apiBaseUrl}/api/gold-rates/current`).subscribe({
      next: (rate: any) => {
        this.currentGoldRate = rate;
        this.newGoldRate = rate.ratePerGram;
      },
      error: (err: any) => {
        console.error('Error loading gold rate:', err);
      }
    });
  }

  updateGoldRate() {
    if (this.newGoldRate <= 0) {
      this.alertService.error('Validation Error', 'Please enter a valid gold rate');
      return;
    }

    this.updatingRate = true;
    this.http.put(`${environment.apiBaseUrl}/api/gold-rates/update?ratePerGram=${this.newGoldRate}&updatedBy=${encodeURIComponent(this.adminName)}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Gold Rate Updated', `Gold rate updated to ₹${this.newGoldRate} per gram`);
          this.loadCurrentGoldRate();
          this.updatingRate = false;
        } else {
          this.alertService.error('Update Failed', response.message || 'Failed to update gold rate');
          this.updatingRate = false;
        }
      },
      error: (err: any) => {
        console.error('Error updating gold rate:', err);
        this.alertService.error('Update Failed', err.error?.message || 'Failed to update gold rate');
        this.updatingRate = false;
      }
    });
  }

  openNewApplicationModal() {
    this.showNewApplicationModal = true;
    this.applicantLookupComplete = false;
    this.applicantAccountNumber = '';
    this.applicant = null;
    this.newApplicationForm = {
      goldGrams: 0,
      tenure: 12,
      interestRate: 12,
      goldPurity: '22K',
      goldItems: '',
      goldDescription: ''
    };
  }

  closeNewApplicationModal() {
    if (this.creatingApplication) return;
    this.showNewApplicationModal = false;
  }

  lookupApplicant() {
    const accountNumber = this.applicantAccountNumber.trim();
    if (!accountNumber) {
      this.alertService.error('Account Required', 'Enter the customer account number.');
      return;
    }

    this.lookingUpApplicant = true;
    this.applicantLookupComplete = false;
    this.applicant = null;
    this.http.get<any>(`${environment.apiBaseUrl}/api/users/account/${encodeURIComponent(accountNumber)}`).subscribe({
      next: user => {
        const account = user?.account;
        const name = account?.name || '';
        if (!user?.accountNumber || !name) {
          this.lookingUpApplicant = false;
          this.alertService.error('Account Details Unavailable', 'The account was found but customer details are incomplete.');
          return;
        }
        this.applicantAccountNumber = user.accountNumber;
        this.applicant = {
          name,
          email: user.email || '',
          phone: account.phone || '',
          balance: Number(account.balance) || 0
        };
        this.applicantLookupComplete = true;
        this.lookingUpApplicant = false;
      },
      error: err => {
        this.lookingUpApplicant = false;
        this.alertService.error('Customer Not Found', err.error?.message || 'No customer was found for that account number.');
      }
    });
  }

  getNewApplicationGoldValue(): number {
    return this.currentGoldRate
      ? Math.round(this.newApplicationForm.goldGrams * this.currentGoldRate.ratePerGram * 100) / 100
      : 0;
  }

  getNewApplicationLoanAmount(): number {
    return Math.round(this.getNewApplicationGoldValue() * 0.75 * 100) / 100;
  }

  getNewApplicationEmi(): number {
    const principal = this.getNewApplicationLoanAmount();
    const months = Number(this.newApplicationForm.tenure);
    const monthlyRate = Number(this.newApplicationForm.interestRate) / 1200;
    if (principal <= 0 || months <= 0) return 0;
    if (monthlyRate === 0) return Math.round((principal / months) * 100) / 100;
    const factor = Math.pow(1 + monthlyRate, months);
    return Math.round((principal * monthlyRate * factor / (factor - 1)) * 100) / 100;
  }

  createApplication() {
    if (!this.applicantLookupComplete || !this.applicant) {
      this.alertService.error('Customer Required', 'Look up and confirm the customer account first.');
      return;
    }
    if (!this.currentGoldRate || this.newApplicationForm.goldGrams <= 0) {
      this.alertService.error('Gold Details Required', "Enter a valid gold weight and ensure today's gold rate is loaded.");
      return;
    }
    if (this.newApplicationForm.interestRate < 0 || this.newApplicationForm.tenure <= 0) {
      this.alertService.error('Loan Details Invalid', 'Enter a valid interest rate and tenure.');
      return;
    }
    if (!this.newApplicationForm.goldItems.trim()) {
      this.alertService.error('Jewellery Details Required', 'Enter the jewellery items included with this application.');
      return;
    }

    this.creatingApplication = true;
    const payload = {
      accountNumber: this.applicantAccountNumber.trim(),
      userName: this.applicant.name,
      userEmail: this.applicant.email,
      currentBalance: this.applicant.balance,
      goldGrams: this.newApplicationForm.goldGrams,
      tenure: Number(this.newApplicationForm.tenure),
      interestRate: Number(this.newApplicationForm.interestRate),
      status: 'Pending',
      goldPurity: this.newApplicationForm.goldPurity,
      goldItems: this.newApplicationForm.goldItems.trim(),
      goldDescription: this.newApplicationForm.goldDescription.trim()
    };
    this.http.post<any>(`${environment.apiBaseUrl}/api/gold-loans`, payload).subscribe({
      next: response => {
        this.creatingApplication = false;
        if (!response?.success) {
          this.alertService.error('Application Failed', response?.message || 'Could not create the gold-loan application.');
          return;
        }
        this.alertService.success('Application Created', response.message || 'Gold-loan application added to the pending queue.');
        this.showNewApplicationModal = false;
        this.loadGoldLoans();
      },
      error: err => {
        this.creatingApplication = false;
        this.alertService.error('Application Failed', err.error?.message || 'Could not create the gold-loan application.');
      }
    });
  }

  openChequePayment(loan: GoldLoan) {
    if (!loan.loanAccountNumber || !loan.accountNumber || loan.status !== 'Approved') {
      this.alertService.error('Payment Unavailable', 'Only approved loans with valid account details can accept prepayment.');
      return;
    }
    this.selectedLoan = loan;
    this.paymentChequeNumber = '';
    this.paymentAmount = null;
    this.paymentAdjustment = 'REDUCE_EMI';
    this.paymentChequeVerification = null;
    this.showChequePaymentModal = true;
  }

  closeChequePayment() {
    if (this.processingChequePayment) return;
    this.showChequePaymentModal = false;
    this.selectedLoan = null;
    this.paymentChequeVerification = null;
  }

  verifyPaymentCheque() {
    if (!this.selectedLoan?.accountNumber || !this.paymentChequeNumber.trim()) {
      this.alertService.error('Cheque Required', 'Enter a cheque number to verify it against the customer account.');
      return;
    }
    this.verifyingPaymentCheque = true;
    this.paymentChequeVerification = null;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admin-fund-transfers/verify-sender-cheque`, {
      params: {
        accountNumber: this.selectedLoan.accountNumber,
        chequeNumber: this.paymentChequeNumber.trim()
      }
    }).subscribe({
      next: result => {
        this.verifyingPaymentCheque = false;
        this.paymentChequeVerification = result;
        if (!result?.valid) {
          this.alertService.error('Cheque Invalid', result?.message || 'This cheque is not available for the customer account.');
          return;
        }
        if (result.nameMatches === false) {
          this.alertService.error('Cheque Name Mismatch', 'The name on this cheque does not match the account holder.');
          return;
        }
        if (result.chequePurpose === 'GOLD_LOAN_PREPAYMENT'
          && String(result.goldLoanAccountNumber || '').toLowerCase()
            !== String(this.selectedLoan?.loanAccountNumber || '').toLowerCase()) {
          this.paymentChequeVerification = {
            ...result,
            valid: false,
            message: 'This cheque is reserved for another gold loan.'
          };
          this.alertService.error('Cheque Restricted', 'This cheque is reserved for another gold loan.');
          return;
        }
        if (result.chequeAmount != null) {
          this.paymentAmount = Number(result.chequeAmount);
        }
      },
      error: err => {
        this.verifyingPaymentCheque = false;
        this.paymentChequeVerification = { valid: false, message: err.error?.message || 'Cheque verification failed.' };
        this.alertService.error('Cheque Verification Failed', err.error?.message || 'Could not verify this cheque.');
      }
    });
  }

  payGoldLoanByCheque() {
    const loan = this.selectedLoan;
    const cheque = this.paymentChequeVerification;
    if (!loan?.loanAccountNumber || !loan.accountNumber || !cheque?.valid || cheque.nameMatches === false) {
      this.alertService.error('Cheque Not Verified', 'Verify a valid cheque for this customer before processing payment.');
      return;
    }
    if (!this.paymentAmount || this.paymentAmount <= 0) {
      this.alertService.error('Invalid Amount', 'Enter a payment amount greater than zero.');
      return;
    }
    const outstandingPrincipal = Number(loan.remainingPrincipal ?? loan.loanAmount);
    if (outstandingPrincipal > 0 && this.paymentAmount >= outstandingPrincipal) {
      this.alertService.error('Amount Exceeds Outstanding', 'Prepayment must be less than the outstanding principal. Use foreclosure to close the loan.');
      return;
    }
    if (cheque.chequePurpose === 'GOLD_LOAN_PREPAYMENT'
      && Math.round(Number(this.paymentAmount) * 100) !== Math.round(Number(cheque.chequeAmount) * 100)) {
      this.alertService.error('Amount Mismatch', 'Payment amount must match the amount written on this reserved cheque.');
      return;
    }

    this.processingChequePayment = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/admin-fund-transfers/process`, {
      senderAccountNumber: loan.accountNumber,
      senderChequeNumber: this.paymentChequeNumber.trim(),
      receiverAccountNumber: loan.loanAccountNumber,
      amount: Number(this.paymentAmount),
      description: `Gold loan prepayment by cheque ${this.paymentChequeNumber.trim()}`,
      performedBy: this.adminName,
      transferCategory: 'GOLD_LOAN',
      loanPaymentType: 'PREPAYMENT',
      loanAccountNumber: loan.loanAccountNumber,
      prepaymentAdjustment: this.paymentAdjustment
    }).subscribe({
      next: response => {
        this.processingChequePayment = false;
        if (!response?.success) {
          this.alertService.error('Payment Failed', response?.message || 'Could not process the cheque payment.');
          return;
        }
        const balanceMessage = response.newBalance == null
          ? ''
          : ` Linked account balance: ₹${Number(response.newBalance).toFixed(2)}.`;
        this.alertService.success('Payment Processed',
          `${response.message || 'Gold-loan prepayment was processed successfully.'}${balanceMessage}`);
        this.closeChequePayment();
        this.loadGoldLoans();
      },
      error: err => {
        this.processingChequePayment = false;
        this.alertService.error('Payment Failed', err.error?.message || 'Could not process the cheque payment.');
      }
    });
  }

  approveGoldLoan(loan: GoldLoan, status: string) {
    if (!loan.id) return;

    // If approving, show gold details form
    if (status === 'Approved') {
      this.selectedLoan = loan;
      this.verifiedAllocations = [];
      this.expiredVerifiedAllocationsFound = false;
      // Pre-fill form with loan data
      this.goldDetailsForm.verifiedGoldGrams = loan.goldGrams;
      this.goldDetailsForm.goldItems = loan.goldItems || '';
      this.goldDetailsForm.goldDescription = loan.goldDescription || '';
      this.goldDetailsForm.goldPurity = loan.goldPurity || '22K';
      this.goldDetailsForm.allocationId = null;
      this.goldDetailsForm.performedByAdminId = this.adminId;
      this.showGoldDetailsModal = true;
      this.loadVerifiedAllocations();
      // Calculate initial values
      this.calculateLoanDetails();
    } else {
      // For rejection, proceed directly
      this.processApproval(loan, status, null);
    }
  }

  loadVerifiedAllocations(): void {
    this.http.get<any>(`${environment.apiBaseUrl}/api/hod/allocations?status=ACTIVE&page=0&size=100`).subscribe({
      next: response => {
        const allocations = response?.content || response?.allocations || [];
        const verified = allocations.filter((allocation: any) =>
          allocation.status === 'ACTIVE' &&
          allocation.accountVerificationStatus === 'VERIFIED' &&
          allocation.chargeManagementEnabled === true &&
          !!allocation.allocationAccountId
        );
        this.expiredVerifiedAllocationsFound = verified.some((allocation: any) =>
          this.isAllocationExpired(allocation.validTill)
        );
        this.verifiedAllocations = verified.filter((allocation: any) =>
          !this.isAllocationExpired(allocation.validTill)
        );
      },
      error: err => {
        this.alertService.error('Allocation Lookup', err.error?.message || 'Could not load verified allocation accounts.');
      }
    });
  }

  private isAllocationExpired(validTill: string | null | undefined): boolean {
    if (!validTill) return false;
    const expiryDate = validTill.slice(0, 10);
    const today = new Date();
    const todayDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    return expiryDate < todayDate;
  }
  
  calculateLoanDetails() {
    if (!this.selectedLoan || !this.currentGoldRate) return;
    
    // Calculate verified gold value
    this.calculatedVerifiedValue = (this.goldDetailsForm.verifiedGoldGrams || 0) * this.currentGoldRate.ratePerGram;
    
    // Calculate loan amount as 75% of verified gold value
    this.calculatedLoanAmount = this.calculatedVerifiedValue * 0.75;
    
    // Calculate EMI if tenure and interest rate are available
    if (this.selectedLoan.tenure && this.selectedLoan.interestRate && this.calculatedLoanAmount > 0) {
      const principal = this.calculatedLoanAmount;
      const annualRate = this.selectedLoan.interestRate;
      const tenureMonths = this.selectedLoan.tenure;
      const monthlyRate = annualRate / (12 * 100);
      
      if (monthlyRate > 0) {
        this.calculatedEMI = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)) / 
                           (Math.pow(1 + monthlyRate, tenureMonths) - 1);
        this.calculatedEMI = Math.round(this.calculatedEMI * 100) / 100;
      } else {
        this.calculatedEMI = principal / tenureMonths;
      }
      
      // Calculate total interest
      const totalAmount = this.calculatedEMI * tenureMonths;
      this.calculatedTotalInterest = totalAmount - principal;
      this.calculatedTotalInterest = Math.round(this.calculatedTotalInterest * 100) / 100;
    } else {
      this.calculatedEMI = 0;
      this.calculatedTotalInterest = 0;
    }
  }

  processApproval(loan: GoldLoan, status: string, goldDetails: any) {
    if (!loan.id) return;

    this.approving = true;
    const requestBody = status === 'Approved' && goldDetails ? goldDetails : {};

    this.http.put(
      `${environment.apiBaseUrl}/api/gold-loans/${loan.id}/approve`,
      requestBody,
      { params: { status, approvedBy: this.adminName }, headers: this.staffHeaders() }
    ).subscribe({
      next: (response: any) => {
        if (response.success) {
          if (status === 'Approved') {
            loan.status = response.goldLoan?.status || 'Awaiting Signature';
            loan.agreementId = response.agreementId;
            loan.agreementStatus = response.status || 'PENDING_SIGNATURE';
            loan.shareUrl = response.shareUrl;
            this.filterStatus = 'All';
          }
          if (status === 'Approved' && (this.jewelleryImageOne || this.jewelleryImageTwo)) {
            const formData = new FormData();
            if (this.jewelleryImageOne) formData.append('imageOne', this.jewelleryImageOne);
            if (this.jewelleryImageTwo) formData.append('imageTwo', this.jewelleryImageTwo);
            this.http.post(`${environment.apiBaseUrl}/api/gold-loans/${loan.id}/jewellery-images`, formData, {
              headers: this.staffHeaders()
            }).subscribe({
              error: () => this.alertService.error('Image Upload', 'Loan approved, but jewellery images could not be saved.')
            });
          }
          this.alertService.success(
            status === 'Approved' ? 'Provisional Approval — Share E-sign Link' : 'Loan Rejected',
            status === 'Approved'
              ? `${response.message}\n${response.emailMessage || 'Share the link manually.'}\n${response.shareUrl}`
              : response.message || `Gold loan ${status.toLowerCase()} successfully`
          );
          this.approving = false;
          this.selectedLoan = null;
          this.showGoldDetailsModal = false;
          this.resetGoldDetailsForm();
          this.loadGoldLoans();
        } else {
          this.alertService.error('Action Failed', response.message || 'Failed to process gold loan');
          this.approving = false;
        }
      },
      error: (err: any) => {
        console.error('Error processing gold loan:', err);
        const message = err.error?.message || 'Failed to process gold loan';
        this.alertService.error('Action Failed', message.includes('Allocation has expired')
          ? 'The selected funding allocation has expired. Select a current allocation or ask the HOD to extend/create one.'
          : message);
        this.approving = false;
      }
    });
  }

  copyAgreementLink(loan: GoldLoan): void {
    if (loan.shareUrl) {
      navigator.clipboard.writeText(loan.shareUrl).then(
        () => this.alertService.success('Link Copied', 'Customer e-sign link copied.'),
        () => this.alertService.info('Customer e-sign link', loan.shareUrl || '')
      );
      return;
    }
    if (!loan.agreementId) return;
    this.agreementService.renewShareLink(loan.agreementId).subscribe({
      next: response => {
        loan.shareUrl = response.shareUrl;
        navigator.clipboard.writeText(response.shareUrl).then(
          () => this.alertService.success('Link Copied', 'Fresh customer e-sign link copied.'),
          () => this.alertService.info('Customer e-sign link', response.shareUrl)
        );
      },
      error: error => this.alertService.error('Share Link', error.error?.message || 'Could not create a share link.')
    });
  }

  emailAgreementLink(loan: GoldLoan): void {
    if (!loan.agreementId) return;
    this.agreementService.emailShareLink(loan.agreementId).subscribe({
      next: response => {
        loan.shareUrl = response.shareUrl;
        if (response.emailSent) {
          this.alertService.success('Agreement Link Emailed', response.emailMessage);
        } else {
          this.alertService.error('Email Not Sent', `${response.emailMessage}\n${response.shareUrl}`);
        }
      },
      error: error => this.alertService.error('Agreement Email', error.error?.message || 'Could not email the agreement link.')
    });
  }

  downloadAgreement(loan: GoldLoan): void {
    if (!loan.agreementId) return;
    this.agreementService.downloadAdminPdf(loan.agreementId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `neobank-gold-loan-agreement-${loan.id}.pdf`;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: error => this.alertService.error('Agreement PDF', error.error?.message || 'Could not download the PDF.')
    });
  }

  finalApproveGoldLoan(loan: GoldLoan): void {
    if (!loan.id || !loan.agreementId
      || !confirm('Customer signature is recorded. Complete final approval and disburse the gold loan?')) {
      return;
    }
    const params = new URLSearchParams({
      agreementId: String(loan.agreementId),
      approvedBy: this.adminName
    });
    this.approving = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/gold-loans/${loan.id}/final-approve?${params}`, {}, {
      headers: this.staffHeaders()
    }).subscribe({
      next: response => {
        this.approving = false;
        if (!response?.success) {
          this.alertService.error('Final Approval', response?.message || 'Could not approve this loan.');
          return;
        }
        loan.status = 'Approved';
        loan.agreementStatus = 'APPROVED';
        this.alertService.success('Loan Disbursed', response.message);
        this.loadGoldLoans();
      },
      error: error => {
        this.approving = false;
        this.alertService.error('Final Approval', error.error?.message || 'Could not approve this loan.');
      }
    });
  }

  submitGoldDetails() {
    if (!this.selectedLoan || !this.selectedLoan.id) return;

    if (!this.goldDetailsForm.allocationId || !this.adminId) {
      this.alertService.error('Allocation Required', 'Select a verified allocation account before approving this loan.');
      return;
    }

    // Validate required fields
    if (!this.goldDetailsForm.goldItems || this.goldDetailsForm.goldItems.trim() === '') {
      this.alertService.error('Validation Error', 'Please enter gold items description');
      return;
    }

    if (!this.goldDetailsForm.verifiedGoldGrams || this.goldDetailsForm.verifiedGoldGrams <= 0) {
      this.alertService.error('Validation Error', 'Please enter verified gold weight in grams');
      return;
    }

    // Process approval with gold details
    this.processApproval(this.selectedLoan, 'Approved', this.goldDetailsForm);
  }

  onJewelleryImageSelected(event: Event, slot: 'one' | 'two') {
    const file = (event.target as HTMLInputElement).files?.[0] || null;
    if (!file) return;
    if (!file.type.startsWith('image/')) { this.alertService.error('Validation Error', 'Please select an image file'); return; }
    if (file.size > 5 * 1024 * 1024) { this.alertService.error('Validation Error', 'Each image must be smaller than 5 MB'); return; }
    if (slot === 'one') this.jewelleryImageOne = file;
    else this.jewelleryImageTwo = file;
  }

  cancelGoldDetails() {
    this.showGoldDetailsModal = false;
    this.resetGoldDetailsForm();
    this.selectedLoan = null;
  }

  resetGoldDetailsForm() {
    this.goldDetailsForm = {
      goldItems: '',
      goldDescription: '',
      goldPurity: '22K',
      verifiedGoldGrams: 0,
      verificationNotes: '',
      storageLocation: ''
    };
    this.jewelleryImageOne = null;
    this.jewelleryImageTwo = null;
  }

  // ─── Edit Loan (amount / gold details / approve time / processing charges) ───

  openEditModal(loan: GoldLoan) {
    this.selectedLoan = loan;
    this.editForm = {
      loanAmount: loan.loanAmount,
      goldItems: loan.goldItems || '',
      goldDescription: loan.goldDescription || '',
      goldPurity: loan.goldPurity || '22K',
      verifiedGoldGrams: loan.verifiedGoldGrams || loan.goldGrams,
      storageLocation: loan.storageLocation || '',
      verificationNotes: loan.verificationNotes || '',
      interestRate: loan.interestRate,
      tenure: loan.tenure,
      processingCharges: loan.processingCharges || 0,
      approvalDate: this.toDateTimeLocal(loan.approvalDate),
      remarks: ''
    };
    this.showEditModal = true;
  }

  closeEditModal() {
    this.showEditModal = false;
    this.selectedLoan = null;
  }

  toDateTimeLocal(dateString: string | undefined): string {
    if (!dateString) return '';
    try {
      const d = new Date(dateString);
      if (isNaN(d.getTime())) return '';
      const pad = (n: number) => n.toString().padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    } catch (e) {
      return '';
    }
  }

  submitEdit() {
    if (!this.selectedLoan || !this.selectedLoan.id) return;
    if (!this.editForm.loanAmount || this.editForm.loanAmount <= 0) {
      this.alertService.error('Validation Error', 'Please enter a valid loan amount');
      return;
    }

    this.editingLoan = true;
    const updates: any = {
      loanAmount: this.editForm.loanAmount,
      goldItems: this.editForm.goldItems,
      goldDescription: this.editForm.goldDescription,
      goldPurity: this.editForm.goldPurity,
      verifiedGoldGrams: this.editForm.verifiedGoldGrams,
      storageLocation: this.editForm.storageLocation,
      verificationNotes: this.editForm.verificationNotes,
      interestRate: this.editForm.interestRate,
      tenure: this.editForm.tenure,
      processingCharges: this.editForm.processingCharges,
      remarks: this.editForm.remarks
    };
    if (this.editForm.approvalDate) {
      updates.approvalDate = this.editForm.approvalDate; // yyyy-MM-ddTHH:mm (LocalDateTime format)
    }

    this.http.put(
      `${environment.apiBaseUrl}/api/gold-loans/${this.selectedLoan.id}/admin-edit?changedBy=${encodeURIComponent(this.adminName)}`,
      updates
    ).subscribe({
      next: (response: any) => {
        this.editingLoan = false;
        if (response.success) {
          this.alertService.success('Loan Updated', 'Gold loan details updated and saved in history.');
          this.closeEditModal();
          this.loadGoldLoans();
        } else {
          this.alertService.error('Update Failed', response.message || 'Failed to update gold loan');
        }
      },
      error: (err: any) => {
        this.editingLoan = false;
        this.alertService.error('Update Failed', err.error?.message || 'Failed to update gold loan');
      }
    });
  }

  // ─── Renew Loan ───

  openRenewModal(loan: GoldLoan) {
    this.selectedLoan = loan;
    this.renewForm = {
      additionalTenure: 6,
      interestRate: null,
      processingCharges: 0,
      remarks: ''
    };
    this.showRenewModal = true;
  }

  closeRenewModal() {
    this.showRenewModal = false;
    this.selectedLoan = null;
  }

  submitRenewal() {
    if (!this.selectedLoan || !this.selectedLoan.id) return;
    if (!this.renewForm.additionalTenure || this.renewForm.additionalTenure <= 0) {
      this.alertService.error('Validation Error', 'Please enter additional tenure of at least 1 month');
      return;
    }
    if (this.renewForm.processingCharges < 0) {
      this.alertService.error('Validation Error', 'Processing charges cannot be negative');
      return;
    }

    this.renewingLoan = true;
    const payload: any = {
      additionalTenure: this.renewForm.additionalTenure,
      processingCharges: this.renewForm.processingCharges || 0,
      remarks: this.renewForm.remarks
    };
    if (this.renewForm.interestRate && this.renewForm.interestRate > 0) {
      payload.interestRate = this.renewForm.interestRate;
    }

    this.http.post(
      `${environment.apiBaseUrl}/api/gold-loans/${this.selectedLoan.id}/renew?renewedBy=${encodeURIComponent(this.adminName)}`,
      payload
    ).subscribe({
      next: (response: any) => {
        this.renewingLoan = false;
        if (response.success) {
          this.alertService.success('Loan Renewed', response.message || 'Gold loan renewed successfully. EMI schedule regenerated.');
          this.closeRenewModal();
          this.loadGoldLoans();
        } else {
          this.alertService.error('Renewal Failed', response.message || 'Failed to renew gold loan');
        }
      },
      error: (err: any) => {
        this.renewingLoan = false;
        this.alertService.error('Renewal Failed', err.error?.message || 'Failed to renew gold loan');
      }
    });
  }

  // ─── History ───

  viewHistory(loan: GoldLoan) {
    if (!loan.id) return;
    this.selectedLoan = loan;
    this.showHistoryModal = true;
    this.loadingHistory = true;
    this.historyList = [];
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/gold-loans/${loan.id}/history`).subscribe({
      next: (history) => {
        this.historyList = Array.isArray(history) ? history : [];
        this.loadingHistory = false;
      },
      error: () => {
        this.loadingHistory = false;
        this.alertService.error('Error', 'Failed to load loan history');
      }
    });
  }

  closeHistoryModal() {
    this.showHistoryModal = false;
    this.historyList = [];
    this.selectedLoan = null;
  }

  isClosed(loan: any): boolean {
    return isLoanClosed(loan);
  }

  downloadLoanDoc(kind: 'noc' | 'receipt', loan: any) {
    if (!openLoanDocument(kind, loan, 'gold', 'admin')) {
      this.alertService.error('Error', 'Please allow popups to download the document');
    }
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch (e) {
      return 'N/A';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Approved':
        return 'status-approved';
      case 'Pending':
        return 'status-pending';
      case 'Rejected':
        return 'status-rejected';
      default:
        return '';
    }
  }

  navigateToDashboard() {
    // Check if accessed from manager dashboard
    const navigationSource = sessionStorage.getItem('navigationSource');
    if (navigationSource === 'MANAGER') {
      sessionStorage.removeItem('navigationSource');
      sessionStorage.removeItem('managerReturnPath');
      this.router.navigate(['/manager/dashboard']);
    } else {
    this.router.navigate(['/admin/dashboard']);
    }
  }

  logout() {
    this.alertService.logoutSuccess();
    this.router.navigate(['/admin/login']);
  }

  // Foreclosure Management
  viewForeclosure(loan: GoldLoan) {
    if (!loan.loanAccountNumber) {
      this.alertService.error('Error', 'Loan account number not found');
      return;
    }
    this.selectedLoan = loan;
    this.showForeclosureModal = true;
    this.loadForeclosureDetails(loan);
  }

  loadForeclosureDetails(loan: GoldLoan) {
    this.http.get<any>(`${environment.apiBaseUrl}/api/gold-loans/foreclosure/${loan.loanAccountNumber}`).subscribe({
      next: (response) => {
        if (response.success) {
          this.foreclosureDetails = response;
          this.editedForeclosureAmount = response.totalForeclosureAmount || 0;
          this.editedInterestRate = loan.interestRate || 12;
          this.calculatePayableAmount();
        } else {
          this.alertService.error('Error', response.message || 'Failed to load foreclosure details');
        }
      },
      error: (err) => {
        console.error('Error loading foreclosure details:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to load foreclosure details');
      }
    });
  }

  toggleEditForeclosure() {
    this.editingForeclosure = !this.editingForeclosure;
    if (this.editingForeclosure) {
      this.editedForeclosureAmount = this.foreclosureDetails?.totalForeclosureAmount || 0;
      this.editedInterestRate = this.selectedLoan?.interestRate || 12;
      this.calculatePayableAmount();
    }
  }

  calculatePayableAmount() {
    if (!this.foreclosureDetails || !this.selectedLoan) return;

    // Calculate interest amount based on edited interest rate
    const principal = this.selectedLoan.loanAmount;
    const annualRate = this.editedInterestRate;
    const tenure = this.selectedLoan.tenure;
    const monthlyRate = annualRate / (12 * 100);
    
    // Calculate total interest for the loan
    let totalInterest = 0;
    if (monthlyRate > 0 && tenure > 0) {
      const emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / 
                  (Math.pow(1 + monthlyRate, tenure) - 1);
      const totalAmount = emi * tenure;
      totalInterest = totalAmount - principal;
    }

    // Calculate interest portion of foreclosure
    const monthsElapsed = this.foreclosureDetails.monthsElapsed || 0;
    const remainingMonths = tenure - monthsElapsed;
    
    // Interest paid so far
    let interestPaid = 0;
    let remainingPrincipal = principal;
    for (let i = 0; i < monthsElapsed && i < tenure; i++) {
      const interestForMonth = remainingPrincipal * monthlyRate;
      const principalForMonth = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / 
                                (Math.pow(1 + monthlyRate, tenure) - 1) - interestForMonth;
      interestPaid += interestForMonth;
      remainingPrincipal -= principalForMonth;
    }

    // Remaining interest
    const remainingInterest = totalInterest - interestPaid;
    
    // If foreclosure amount is edited, calculate interest portion
    const remainingPrincipalAmount = this.foreclosureDetails.remainingPrincipal || 0;
    const foreclosureCharges = remainingPrincipalAmount * 0.04;
    const gst = foreclosureCharges * 0.18;
    
    // If admin edits foreclosure amount, adjust interest accordingly
    if (this.editedForeclosureAmount > 0) {
      const baseAmount = remainingPrincipalAmount + foreclosureCharges + gst;
      this.calculatedInterestAmount = Math.max(0, this.editedForeclosureAmount - baseAmount);
    } else {
      this.calculatedInterestAmount = remainingInterest;
    }

    this.calculatedPayableAmount = this.editedForeclosureAmount;
  }

  onForeclosureAmountChange() {
    this.calculatePayableAmount();
  }

  onInterestRateChange() {
    if (this.selectedLoan) {
      this.selectedLoan.interestRate = this.editedInterestRate;
      this.calculatePayableAmount();
    }
  }

  saveForeclosureChanges() {
    if (!this.selectedLoan || !this.selectedLoan.loanAccountNumber) return;
    this.processForeclosure();
  }

  processForeclosure() {
    if (!this.selectedLoan || !this.selectedLoan.loanAccountNumber) return;
    
    const foreclosureAmount = this.editingForeclosure ? this.editedForeclosureAmount : (this.foreclosureDetails?.totalForeclosureAmount || 0);
    if (!confirm(`Are you sure you want to process foreclosure for ₹${foreclosureAmount.toFixed(2)}?`)) return;

    this.processingForeclosure = true;
    const requestBody = this.editingForeclosure ? {
      foreclosureAmount: this.editedForeclosureAmount,
      interestRate: this.editedInterestRate
    } : {};

    this.http.post<any>(`${environment.apiBaseUrl}/api/gold-loans/foreclosure/${this.selectedLoan.loanAccountNumber}`, requestBody, {
      params: { foreclosedBy: this.adminName || 'Admin' }
    }).subscribe({
      next: (response) => {
        if (response.success) {
          const balanceMessage = response.newBalance == null
            ? ''
            : ` Linked account balance: ₹${Number(response.newBalance).toFixed(2)}.`;
          this.alertService.success('Success', `Foreclosure processed successfully.${balanceMessage}`);
          this.closeForeclosureModal();
          this.loadGoldLoans();
        } else {
          this.alertService.error('Error', response.message || 'Failed to process foreclosure');
        }
        this.processingForeclosure = false;
      },
      error: (err) => {
        console.error('Error processing foreclosure:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to process foreclosure');
        this.processingForeclosure = false;
      }
    });
  }

  closeForeclosureModal() {
    this.showForeclosureModal = false;
    this.selectedLoan = null;
    this.foreclosureDetails = null;
    this.editingForeclosure = false;
    this.editedForeclosureAmount = 0;
    this.editedInterestRate = 12;
  }

  // EMI Details
  viewEmiDetails(loan: GoldLoan) {
    if (!loan.loanAccountNumber) {
      this.alertService.error('Error', 'Loan account number not found');
      return;
    }
    this.selectedLoan = loan;
    this.showEmiDetailsModal = true;
    this.loadEmiSchedule(loan);
  }

  loadEmiSchedule(loan: GoldLoan) {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/gold-loans/emi-schedule/${loan.loanAccountNumber}`).subscribe({
      next: (schedule) => {
        this.emiSchedule = Array.isArray(schedule) ? schedule : [];
      },
      error: (err) => {
        console.error('Error loading EMI schedule:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to load EMI schedule');
        this.emiSchedule = [];
      }
    });
  }

  closeEmiDetailsModal() {
    this.showEmiDetailsModal = false;
    this.selectedLoan = null;
    this.emiSchedule = [];
  }

  isUpcomingEmi(dueDate: string): boolean {
    if (!dueDate) return false;
    const due = new Date(dueDate);
    const today = new Date();
    const daysUntilDue = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    return daysUntilDue >= 0 && daysUntilDue <= 7;
  }

  getTimeUntilDue(dueDate: string): string {
    if (!dueDate) return '';
    const due = new Date(dueDate);
    const today = new Date();
    const daysUntilDue = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    
    if (daysUntilDue < 0) {
      return `Overdue by ${Math.abs(daysUntilDue)} day${Math.abs(daysUntilDue) !== 1 ? 's' : ''}`;
    } else if (daysUntilDue === 0) {
      return 'Due today';
    } else if (daysUntilDue === 1) {
      return 'Due tomorrow';
    } else if (daysUntilDue <= 7) {
      return `Due in ${daysUntilDue} days`;
    } else {
      const monthsUntilDue = Math.floor(daysUntilDue / 30);
      if (monthsUntilDue > 0) {
        return `Due in ${monthsUntilDue} month${monthsUntilDue !== 1 ? 's' : ''}`;
      }
      return `Due in ${daysUntilDue} days`;
    }
  }

  formatDateTime(dateTime: string): string {
    if (!dateTime) return '';
    try {
      const date = new Date(dateTime);
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return '';
    }
  }
}
