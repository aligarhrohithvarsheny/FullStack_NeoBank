import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

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
  currentBalance: number;
  loanAccountNumber?: string;
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
export class AdminGoldLoans implements OnInit {
  goldLoans: GoldLoan[] = [];
  filteredLoans: GoldLoan[] = [];
  loading: boolean = false;
  error: string = '';
  
  // Filter
  filterStatus: string = 'Pending';
  
  // Selected loan for approval
  selectedLoan: GoldLoan | null = null;
  approving: boolean = false;
  adminName: string = 'Admin';
  
  // Gold details form (for acceptance)
  showGoldDetailsModal: boolean = false;
  goldDetailsForm: any = {
    goldItems: '',
    goldDescription: '',
    goldPurity: '22K',
    verifiedGoldGrams: 0,
    verificationNotes: '',
    storageLocation: ''
  };
  
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

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadAdminInfo();
      this.loadGoldLoans();
      this.loadCurrentGoldRate();
    }
  }

  loadAdminInfo() {
    const admin = sessionStorage.getItem('admin');
    if (admin) {
      const adminData = JSON.parse(admin);
      this.adminName = adminData.username || 'Admin';
    }
  }

  loadGoldLoans() {
    this.loading = true;
    this.error = '';
    
    this.http.get(`${environment.apiBaseUrl}/gold-loans`).subscribe({
      next: (loans: any) => {
        this.goldLoans = Array.isArray(loans) ? loans : [];
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
    if (this.filterStatus === 'All') {
      this.filteredLoans = this.goldLoans;
    } else {
      this.filteredLoans = this.goldLoans.filter(loan => loan.status === this.filterStatus);
    }
  }

  onFilterChange() {
    this.filterLoans();
  }

  loadCurrentGoldRate() {
    this.http.get(`${environment.apiBaseUrl}/gold-rates/current`).subscribe({
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
    this.http.put(`${environment.apiBaseUrl}/gold-rates/update?ratePerGram=${this.newGoldRate}&updatedBy=${this.adminName}`, {}).subscribe({
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

  approveGoldLoan(loan: GoldLoan, status: string) {
    if (!loan.id) return;

    // If approving, show gold details form
    if (status === 'Approved') {
      this.selectedLoan = loan;
      // Pre-fill form with loan data
      this.goldDetailsForm.verifiedGoldGrams = loan.goldGrams;
      this.goldDetailsForm.goldPurity = '22K';
      this.showGoldDetailsModal = true;
      // Calculate initial values
      this.calculateLoanDetails();
    } else {
      // For rejection, proceed directly
      this.processApproval(loan, status, null);
    }
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
      `${environment.apiBaseUrl}/gold-loans/${loan.id}/approve?status=${status}&approvedBy=${this.adminName}`,
      requestBody
    ).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success(
            status === 'Approved' ? 'Loan Approved' : 'Loan Rejected',
            response.message || `Gold loan ${status.toLowerCase()} successfully`
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
        this.alertService.error('Action Failed', err.error?.message || 'Failed to process gold loan');
        this.approving = false;
      }
    });
  }

  submitGoldDetails() {
    if (!this.selectedLoan || !this.selectedLoan.id) return;

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
    this.http.get<any>(`${environment.apiBaseUrl}/gold-loans/foreclosure/${loan.loanAccountNumber}`).subscribe({
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
    
    // Update loan interest rate if changed
    if (this.editedInterestRate !== this.selectedLoan.interestRate) {
      this.updateLoanInterestRate(this.selectedLoan.id!, this.editedInterestRate);
    }
    
    // Process foreclosure with edited amount
    this.processForeclosure();
  }

  updateLoanInterestRate(loanId: number, newInterestRate: number) {
    this.http.put<any>(`${environment.apiBaseUrl}/gold-loans/${loanId}/interest-rate`, null, {
      params: { interestRate: newInterestRate.toString() }
    }).subscribe({
      next: (response) => {
        console.log('Interest rate updated:', response);
      },
      error: (err) => {
        console.error('Error updating interest rate:', err);
      }
    });
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

    this.http.post<any>(`${environment.apiBaseUrl}/gold-loans/foreclosure/${this.selectedLoan.loanAccountNumber}`, requestBody).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Success', 'Foreclosure processed successfully');
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
    this.http.get<any[]>(`${environment.apiBaseUrl}/gold-loans/emi-schedule/${loan.loanAccountNumber}`).subscribe({
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

