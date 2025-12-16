import { CurrencyPipe, CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environment/environment';

interface LoanRequest {
  id: number;
  userName: string;
  userEmail: string;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: 'Pending' | 'Approved' | 'Rejected' | 'Foreclosed' | 'Paid';
  accountNumber: string;
  childAccountNumber?: string; // For education loans with child account
  currentBalance: number;
  loanAccountNumber: string;
  applicationDate: string;
  approvalDate?: string;
  personalLoanFormPath?: string; // Path to uploaded Personal Loan form
}

interface ForeclosureDetails {
  principalPaid: number;
  interestPaid: number;
  remainingPrincipal: number;
  remainingInterest: number;
  foreclosureCharges: number;
  gst: number;
  totalForeclosureAmount: number;
  monthsElapsed: number;
  remainingMonths: number;
  emi: number;
  totalPaid: number;
}

@Component({
  selector: 'app-loans',
  standalone: true,
  templateUrl: './loans.html',
  styleUrls: ['./loans.css'],
  imports: [CurrencyPipe, CommonModule, FormsModule]
})
export class Loans implements OnInit {
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}

  loanRequests: LoanRequest[] = [];
  loading = false;

  // Foreclosure properties
  showForeclosureModal = false;
  foreclosureLoanNumber = '';
  calculatingForeclosure = false;
  processingForeclosure = false;
  foreclosureDetails: ForeclosureDetails | null = null;
  foreclosureError = '';

  // EMI details (per‑month loan payment tracking)
  showEmiModal = false;
  selectedLoanForEmi: LoanRequest | null = null;
  emis: {
    id: number;
    loanId: number;
    loanAccountNumber: string;
    accountNumber: string;
    emiNumber: number;
    dueDate: string;
    principalAmount: number;
    interestAmount: number;
    totalAmount: number;
    remainingPrincipal: number;
    status: string;
    paymentDate?: string;
    balanceBeforePayment?: number;
    balanceAfterPayment?: number;
    transactionId?: string;
  }[] = [];
  loadingEmis = false;
  emisError = '';

  ngOnInit() {
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      console.log('Loans component initialized!');
      this.loadAllLoans();
    }
  }

  loadAllLoans() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    console.log('Loading loan applications...');
    
    // Load loans from MySQL database
    this.http.get(`${environment.apiBaseUrl}/loans`).subscribe({
      next: (loans: any) => {
        console.log('Loans loaded from MySQL:', loans);
        this.loanRequests = loans.map((loan: any) => ({
          id: loan.id,
          userName: loan.userName,
          userEmail: loan.userEmail,
          type: loan.type,
          amount: loan.amount,
          tenure: loan.tenure,
          interestRate: loan.interestRate,
          status: loan.status,
          accountNumber: loan.accountNumber,
          childAccountNumber: loan.childAccountNumber,
          currentBalance: loan.currentBalance,
          loanAccountNumber: loan.loanAccountNumber,
          applicationDate: loan.applicationDate,
          approvalDate: loan.approvalDate,
          personalLoanFormPath: loan.personalLoanFormPath
        }));
        
        // Also save to localStorage as backup
        this.saveLoansToStorage();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading loans from database:', err);
        // Fallback to localStorage
        const savedLoans = localStorage.getItem('user_loans');
        console.log('Fallback: Saved loans from localStorage:', savedLoans);
        
        if (savedLoans) {
          try {
            this.loanRequests = JSON.parse(savedLoans);
            console.log('Parsed loan requests:', this.loanRequests);
          } catch (error) {
            console.error('Error parsing saved loans:', error);
            this.loanRequests = [];
          }
        } else {
          console.log('No saved loans found in database');
          // No loans found in database - show empty list
          this.loanRequests = [];
        }
    
    this.loading = false;
    console.log('Final loan requests count:', this.loanRequests.length);
      }
    });
  }

  saveLoansToStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('user_loans', JSON.stringify(this.loanRequests));
  }

  // Navigate back to admin dashboard
  goBack() {
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

  // Approve loan
  approve(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    console.log('Approving loan:', loan);
    
    const updatedLoan = {
      ...loan,
      status: 'Approved',
      approvalDate: new Date().toISOString()
    };
    
    // Update loan in MySQL database using enhanced approval endpoint
    this.http.put(`${environment.apiBaseUrl}/loans/${loan.id}/approve?status=Approved`, {}).subscribe({
      next: (response: any) => {
        console.log('Loan approved in MySQL:', response);
        
        if (response.success) {
          loan.status = 'Approved';
          loan.approvalDate = new Date().toISOString();
          
          // Update in localStorage as backup
          this.saveLoansToStorage();
          
          // Show enhanced notification with transaction details
          this.showLoanApprovalNotification(loan, response.transaction, response.loan.amount);
          
          console.log('✅ Loan approval completed successfully');
        } else {
          alert('Loan approval failed: ' + response.message);
        }
      },
      error: (err: any) => {
        console.error('Error approving loan:', err);
        alert('Failed to approve loan. Please try again.');
      }
    });
  }

  // Note: createLoanCreditTransaction and updateUserBalance methods removed
  // Backend now handles all loan approval, balance updates, and transaction creation

  // Reject loan
  reject(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const updatedLoan = {
      ...loan,
      status: 'Rejected',
      approvalDate: new Date().toISOString()
    };
    
    // Update loan in MySQL database
    this.http.put(`${environment.apiBaseUrl}/loans/${loan.id}/approve?status=Rejected`, {}).subscribe({
      next: (response: any) => {
        console.log('Loan rejected in MySQL:', response);
        
        loan.status = 'Rejected';
        loan.approvalDate = new Date().toISOString();
        
        // Update in localStorage as backup
        this.saveLoansToStorage();
        
        alert(`Loan for ${loan.userName} rejected.`);
      },
      error: (err: any) => {
        console.error('Error rejecting loan:', err);
        alert('Failed to reject loan. Please try again.');
      }
    });
  }

  // TrackBy function for ngFor performance
  trackByLoanId(index: number, loan: LoanRequest): number {
    return loan.id;
  }

  // TrackBy for EMI rows
  trackByEmiId(index: number, emi: { id: number }): number {
    return emi.id;
  }

  // Show loan approval notification with detailed information
  showLoanApprovalNotification(loan: LoanRequest, transaction: any, newBalance: number) {
    const notificationMessage = `
🎉 LOAN APPROVED SUCCESSFULLY! 🎉

👤 User: ${loan.userName}
📧 Email: ${loan.userEmail}
🏦 Account: ${loan.accountNumber}
💰 Loan Amount: ₹${loan.amount.toLocaleString()}
📋 Loan Type: ${loan.type}
📅 Tenure: ${loan.tenure} months
📊 Interest Rate: ${loan.interestRate}%

💳 TRANSACTION DETAILS:
🆔 Transaction ID: ${transaction.id}
💵 Amount Credited: ₹${loan.amount.toLocaleString()}
💳 New Balance: ₹${newBalance.toLocaleString()}
📝 Description: ${transaction.description}
⏰ Processed: ${new Date().toLocaleString()}

✅ The loan amount has been credited to the user's account in real-time!
✅ Transaction has been recorded with complete details!
✅ User can now see the updated balance and transaction history!
    `;
    
    alert(notificationMessage);
    
    // Also log to console for debugging
    console.log('=== LOAN APPROVAL NOTIFICATION ===');
    console.log('User:', loan.userName);
    console.log('Account:', loan.accountNumber);
    console.log('Loan Amount:', loan.amount);
    console.log('Transaction ID:', transaction.id);
    console.log('New Balance:', newBalance);
    console.log('==================================');
  }

  // Open foreclosure modal
  openForeclosureModal() {
    this.showForeclosureModal = true;
    this.foreclosureLoanNumber = '';
    this.foreclosureDetails = null;
    this.foreclosureError = '';
  }

  // Foreclose loan directly from table
  forecloseLoanFromTable(loan: LoanRequest) {
    if (!confirm(`Are you sure you want to foreclose loan ${loan.loanAccountNumber}?\n\nThis will calculate the foreclosure amount and debit it from the user's account.`)) {
      return;
    }

    // Set the loan number and calculate
    this.foreclosureLoanNumber = loan.loanAccountNumber;
    this.showForeclosureModal = true;
    this.foreclosureError = '';
    this.foreclosureDetails = null;

    // Auto-calculate foreclosure
    this.calculateForeclosure();
  }

  // Close foreclosure modal
  closeForeclosureModal() {
    this.showForeclosureModal = false;
    this.foreclosureLoanNumber = '';
    this.foreclosureDetails = null;
    this.foreclosureError = '';
  }

  // Calculate foreclosure amount
  calculateForeclosure() {
    if (!this.foreclosureLoanNumber || this.foreclosureLoanNumber.trim() === '') {
      this.foreclosureError = 'Please enter a loan account number';
      return;
    }

    this.calculatingForeclosure = true;
    this.foreclosureError = '';
    this.foreclosureDetails = null;

    this.http.get(`${environment.apiBaseUrl}/loans/foreclosure/calculate/${this.foreclosureLoanNumber.trim()}`).subscribe({
      next: (response: any) => {
        this.calculatingForeclosure = false;
        if (response.success) {
          this.foreclosureDetails = {
            principalPaid: response.principalPaid,
            interestPaid: response.interestPaid,
            remainingPrincipal: response.remainingPrincipal,
            remainingInterest: response.remainingInterest,
            foreclosureCharges: response.foreclosureCharges,
            gst: response.gst,
            totalForeclosureAmount: response.totalForeclosureAmount,
            monthsElapsed: response.monthsElapsed,
            remainingMonths: response.remainingMonths,
            emi: response.emi,
            totalPaid: response.totalPaid
          };
        } else {
          this.foreclosureError = response.message || 'Failed to calculate foreclosure amount';
        }
      },
      error: (err: any) => {
        this.calculatingForeclosure = false;
        this.foreclosureError = err.error?.message || 'Error calculating foreclosure. Please check the loan number.';
        console.error('Error calculating foreclosure:', err);
      }
    });
  }

  // Process foreclosure
  processForeclosure() {
    if (!this.foreclosureLoanNumber || this.foreclosureLoanNumber.trim() === '') {
      this.foreclosureError = 'Please enter a loan account number';
      return;
    }

    if (!this.foreclosureDetails) {
      this.foreclosureError = 'Please calculate foreclosure amount first';
      return;
    }

    if (!confirm(`Are you sure you want to foreclose this loan?\n\nTotal Foreclosure Amount: ₹${this.foreclosureDetails.totalForeclosureAmount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}\n\nThis action cannot be undone.`)) {
      return;
    }

    this.processingForeclosure = true;
    this.foreclosureError = '';

    this.http.post(`${environment.apiBaseUrl}/loans/foreclose/${this.foreclosureLoanNumber.trim()}`, {}).subscribe({
      next: (response: any) => {
        this.processingForeclosure = false;
        if (response.success) {
          alert(`✅ Loan foreclosed successfully!\n\nTotal Amount: ₹${this.foreclosureDetails!.totalForeclosureAmount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}\n\nPDF has been generated and sent to user's email.`);
          
          // Reload loans to update status
          this.loadAllLoans();
          
          // Close modal
          this.closeForeclosureModal();
        } else {
          this.foreclosureError = response.message || 'Failed to process foreclosure';
        }
      },
      error: (err: any) => {
        this.processingForeclosure = false;
        this.foreclosureError = err.error?.message || 'Error processing foreclosure. Please try again.';
        console.error('Error processing foreclosure:', err);
      }
    });
  }

  // ---------- EMI schedule & payment details ----------

  openEmiDetails(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) return;

    this.selectedLoanForEmi = loan;
    this.showEmiModal = true;
    this.loadingEmis = true;
    this.emisError = '';
    this.emis = [];

    // Fetch EMI schedule (monthly payments) for this loan from backend
    this.http.get<any[]>(`${environment.apiBaseUrl}/emis/loan/${loan.id}`).subscribe({
      next: (emis) => {
        this.emis = emis || [];
        this.loadingEmis = false;
      },
      error: (err: any) => {
        console.error('Error loading EMI details for loan', loan.id, err);
        this.emisError = 'Failed to load EMI schedule. Please try again.';
        this.loadingEmis = false;
      }
    });
  }

  closeEmiModal() {
    this.showEmiModal = false;
    this.selectedLoanForEmi = null;
    this.emis = [];
    this.loadingEmis = false;
    this.emisError = '';
  }

  // Format date/time with proper timezone handling
  formatDateTime(dateTimeString: string): string {
    if (!dateTimeString) return '-';
    try {
      // Parse the date string (handles both ISO format and other formats)
      const date = new Date(dateTimeString);
      
      // Check if date is valid
      if (isNaN(date.getTime())) {
        return dateTimeString; // Return original if invalid
      }
      
      // Format to local timezone with proper date and time
      return date.toLocaleString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
      });
    } catch (e) {
      console.error('Error formatting date:', e);
      return dateTimeString; // Return original if error
    }
  }

  // View Personal Loan Application Form
  viewPersonalLoanForm(loan: LoanRequest) {
    if (!loan.personalLoanFormPath || !loan.id) {
      alert('Personal Loan form not available for this loan.');
      return;
    }

    // Download the form file from backend
    window.open(`${environment.apiBaseUrl}/loans/download-personal-loan-form/${loan.id}`, '_blank');
  }
}
