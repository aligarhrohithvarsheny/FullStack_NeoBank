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
  selector: 'app-goldloan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './goldloan.html',
  styleUrls: ['./goldloan.css']
})
export class Goldloan implements OnInit {
  goldGrams: number = 0;
  currentGoldRate: GoldRate | null = null;
  calculatedLoan: any = null;
  loading: boolean = false;
  calculating: boolean = false;
  userAccountNumber: string = '';
  userName: string = '';
  userEmail: string = '';
  currentBalance: number = 0;
  
  // Form fields
  tenure: number = 12; // Default 12 months
  interestRate: number = 12; // Default 12% per annum
  acceptTerms: boolean = false; // Terms acceptance
  
  // Gold loans list
  goldLoans: GoldLoan[] = [];
  loadingLoans: boolean = false;
  
  // Terms acceptance for approved loans
  selectedLoanForTerms: GoldLoan | null = null;
  showTermsModal: boolean = false;
  acceptingTerms: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserInfo();
      this.loadCurrentGoldRate();
      this.loadGoldLoans();
    }
  }

  loadUserInfo() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.userAccountNumber = user.accountNumber;
      this.userName = user.name;
      this.userEmail = user.email;
      
      // Load current balance
      this.http.get(`${environment.apiBaseUrl}/accounts/balance/${this.userAccountNumber}`).subscribe({
        next: (balanceData: any) => {
          this.currentBalance = balanceData.balance || 0;
        },
        error: (err: any) => {
          console.error('Error loading balance:', err);
        }
      });
    }
  }

  loadCurrentGoldRate() {
    this.loading = true;
    this.http.get(`${environment.apiBaseUrl}/gold-rates/current`).subscribe({
      next: (rate: any) => {
        this.currentGoldRate = rate;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading gold rate:', err);
        this.loading = false;
      }
    });
  }

  calculateLoan() {
    if (this.goldGrams <= 0) {
      this.alertService.error('Validation Error', 'Please enter a valid amount of gold in grams');
      return;
    }

    this.calculating = true;
    this.http.get(`${environment.apiBaseUrl}/gold-loans/calculate?grams=${this.goldGrams}`).subscribe({
      next: (response: any) => {
        this.calculatedLoan = response;
        this.calculating = false;
      },
      error: (err: any) => {
        console.error('Error calculating loan:', err);
        this.alertService.error('Calculation Error', 'Failed to calculate loan amount. Please try again.');
        this.calculating = false;
      }
    });
  }

  applyGoldLoan() {
    if (!this.calculatedLoan || !this.calculatedLoan.success) {
      this.alertService.error('Validation Error', 'Please calculate loan amount first');
      return;
    }

    if (!this.acceptTerms) {
      this.alertService.error('Validation Error', 'Please accept the terms and conditions to proceed');
      return;
    }

    if (!this.userAccountNumber) {
      this.alertService.error('Account Error', 'Account number not found. Please refresh the page.');
      return;
    }

    this.loading = true;

    const goldLoanData: GoldLoan = {
      goldGrams: this.goldGrams,
      goldRatePerGram: this.calculatedLoan.goldRatePerGram,
      goldValue: this.calculatedLoan.goldValue,
      loanAmount: this.calculatedLoan.loanAmount,
      tenure: this.tenure,
      interestRate: this.interestRate,
      status: 'Pending',
      userName: this.userName,
      userEmail: this.userEmail,
      accountNumber: this.userAccountNumber,
      currentBalance: this.currentBalance
    };

    this.http.post(`${environment.apiBaseUrl}/gold-loans`, goldLoanData).subscribe({
      next: (response: any) => {
        this.loading = false;
        if (response.success) {
          this.alertService.success('Application Submitted', 
            `Gold loan application submitted successfully! Loan Amount: ₹${this.calculatedLoan.loanAmount.toFixed(2)}`);
          this.resetForm();
          this.loadGoldLoans();
        } else {
          this.alertService.error('Application Failed', response.message || 'Failed to submit gold loan application');
        }
      },
      error: (err: any) => {
        console.error('Error applying for gold loan:', err);
        this.loading = false;
        this.alertService.error('Application Failed', err.error?.message || 'Failed to submit gold loan application. Please try again.');
      }
    });
  }

  loadGoldLoans() {
    if (!this.userAccountNumber) return;
    
    this.loadingLoans = true;
    this.http.get(`${environment.apiBaseUrl}/gold-loans/account/${this.userAccountNumber}`).subscribe({
      next: (loans: any) => {
        this.goldLoans = Array.isArray(loans) ? loans : [];
        this.loadingLoans = false;
      },
      error: (err: any) => {
        console.error('Error loading gold loans:', err);
        this.goldLoans = [];
        this.loadingLoans = false;
      }
    });
  }

  resetForm() {
    this.goldGrams = 0;
    this.calculatedLoan = null;
    this.tenure = 12;
    this.acceptTerms = false;
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
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

  showTermsAndAccept(loan: GoldLoan) {
    this.selectedLoanForTerms = loan;
    this.showTermsModal = true;
  }

  acceptTermsAndConditions() {
    if (!this.selectedLoanForTerms || !this.selectedLoanForTerms.id) return;

    this.acceptingTerms = true;
    const acceptedBy = this.userAccountNumber || this.userEmail;

    this.http.put(
      `${environment.apiBaseUrl}/gold-loans/${this.selectedLoanForTerms.id}/accept-terms?acceptedBy=${acceptedBy}`,
      {}
    ).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Terms Accepted', 'Terms and conditions accepted successfully!');
          this.showTermsModal = false;
          this.selectedLoanForTerms = null;
          this.acceptingTerms = false;
          this.loadGoldLoans();
        } else {
          this.alertService.error('Error', response.message || 'Failed to accept terms');
          this.acceptingTerms = false;
        }
      },
      error: (err: any) => {
        console.error('Error accepting terms:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to accept terms');
        this.acceptingTerms = false;
      }
    });
  }

  closeTermsModal() {
    this.showTermsModal = false;
    this.selectedLoanForTerms = null;
  }
}

