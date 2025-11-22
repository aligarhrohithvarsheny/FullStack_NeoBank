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
  
  // Gold rate management
  currentGoldRate: GoldRate | null = null;
  newGoldRate: number = 0;
  updatingRate: boolean = false;

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
    
    this.http.get(`${environment.apiUrl}/gold-loans`).subscribe({
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
    this.http.get(`${environment.apiUrl}/gold-rates/current`).subscribe({
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
    this.http.put(`${environment.apiUrl}/gold-rates/update?ratePerGram=${this.newGoldRate}&updatedBy=${this.adminName}`, {}).subscribe({
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
    } else {
      // For rejection, proceed directly
      this.processApproval(loan, status, null);
    }
  }

  processApproval(loan: GoldLoan, status: string, goldDetails: any) {
    if (!loan.id) return;

    this.approving = true;
    const requestBody = status === 'Approved' && goldDetails ? goldDetails : {};

    this.http.put(
      `${environment.apiUrl}/gold-loans/${loan.id}/approve?status=${status}&approvedBy=${this.adminName}`,
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
    this.router.navigate(['/admin/dashboard']);
  }

  logout() {
    this.alertService.logoutSuccess();
    this.router.navigate(['/admin/login']);
  }
}

