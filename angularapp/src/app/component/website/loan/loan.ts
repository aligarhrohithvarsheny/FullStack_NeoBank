import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

interface LoanRequest {
  id: number;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: 'Pending' | 'Approved' | 'Rejected' | 'Foreclosed' | 'Paid';
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
  loanAccountNumber: string;
  applicationDate: string;
  approvalDate?: string;
  emiStartDate?: string;
  foreclosureDate?: string;
  foreclosureAmount?: number;
  foreclosureCharges?: number;
  foreclosureGst?: number;
  principalPaid?: number;
  interestPaid?: number;
  remainingPrincipal?: number;
  remainingInterest?: number;
}

interface EmiPayment {
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
  status: 'Pending' | 'Paid' | 'Overdue' | 'Skipped';
  paymentDate?: string;
  balanceBeforePayment?: number;
  balanceAfterPayment?: number;
  transactionId?: string;
}

@Component({
  selector: 'app-loan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan.html',
  styleUrls: ['./loan.css']
})
export class Loan implements OnInit {
  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object, 
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  loans: LoanRequest[] = [];
  userAccountNumber: string = '';
  userName: string = '';
  userEmail: string = '';
  currentBalance: number = 0;
  loading: boolean = false;
  emiPayments: Map<number, EmiPayment[]> = new Map(); // Map loanId to EMIs
  selectedLoanForEmi: LoanRequest | null = null;
  payingEmi: boolean = false;
  showEmiPaymentModal: boolean = false;
  selectedEmiForPayment: EmiPayment | null = null;

  loanForm = {
    type: '',
    amount: 0,
    tenure: 0,
    interestRate: 0,
    pan: ''
  };

  // CIBIL and credit information
  cibilInfo = {
    cibilScore: 0,
    interestRate: 0,
    creditLimit: 0,
    scoreCategory: '',
    pan: '',
    loading: false,
    error: ''
  };

  // EMI Calculator properties
  emiCalculator = {
    principal: 0,
    rate: 0,
    time: 0,
    emi: 0,
    totalAmount: 0,
    totalInterest: 0,
    interestPercentage: 0,
    principalPercentage: 0
  };

  ngOnInit() {
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserData();
    }
  }

  loadUserData() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.userAccountNumber = user.accountNumber;
      this.userName = user.name;
      this.userEmail = user.email;
      
      // Auto-load PAN and check CIBIL
      this.loadUserPanAndCibil();
      
      // Load user loans after getting user data
      this.loadUserLoans();
    } else {
      // Fallback to localStorage for existing users
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        const profile = JSON.parse(savedProfile);
        this.userAccountNumber = profile.accountNumber;
        this.userName = profile.name;
        this.userEmail = profile.email;
        this.loadUserPanAndCibil();
        this.loadUserLoans();
      }
    }
  }

  // Load user PAN and check CIBIL
  loadUserPanAndCibil() {
    if (!this.userAccountNumber) return;

    this.cibilInfo.loading = true;
    this.cibilInfo.error = '';

    // First get PAN
    this.http.get(`${environment.apiUrl}/loans/user-pan/${this.userAccountNumber}`).subscribe({
      next: (response: any) => {
        if (response.success && response.pan) {
          this.loanForm.pan = response.pan;
          
          // Then check CIBIL
          this.checkCibilByPan(response.pan);
        } else {
          this.cibilInfo.loading = false;
          this.cibilInfo.error = 'PAN not found. Please update your KYC details.';
        }
      },
      error: (err: any) => {
        console.error('Error loading PAN:', err);
        // Try to check CIBIL directly by account number
        this.checkCibilByAccount();
      }
    });
  }

  // Check CIBIL by account number (auto-fetches PAN)
  checkCibilByAccount() {
    if (!this.userAccountNumber) return;

    this.cibilInfo.loading = true;
    this.cibilInfo.error = '';

    this.http.get(`${environment.apiUrl}/loans/check-cibil-by-account/${this.userAccountNumber}`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.cibilInfo.cibilScore = response.cibilScore || 0;
          this.cibilInfo.interestRate = response.interestRate || 0;
          this.cibilInfo.creditLimit = response.creditLimit || 0;
          this.cibilInfo.scoreCategory = response.scoreCategory || '';
          this.cibilInfo.pan = response.pan || '';
          
          // Auto-fill PAN and interest rate in loan form
          if (response.pan) {
            this.loanForm.pan = response.pan;
          }
          if (response.interestRate) {
            this.loanForm.interestRate = response.interestRate;
          }
          
          this.cibilInfo.loading = false;
          console.log('CIBIL info loaded:', this.cibilInfo);
        } else {
          this.cibilInfo.loading = false;
          this.cibilInfo.error = response.message || 'Failed to check CIBIL';
        }
      },
      error: (err: any) => {
        console.error('Error checking CIBIL:', err);
        this.cibilInfo.loading = false;
        this.cibilInfo.error = err.error?.message || 'Failed to check CIBIL score';
      }
    });
  }

  // Check CIBIL by PAN
  checkCibilByPan(pan: string) {
    if (!pan || pan.trim() === '') {
      this.cibilInfo.error = 'Please enter PAN number';
      return;
    }

    this.cibilInfo.loading = true;
    this.cibilInfo.error = '';

    this.http.get(`${environment.apiUrl}/loans/check-cibil/${pan}`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.cibilInfo.cibilScore = response.cibilScore || 0;
          this.cibilInfo.interestRate = response.interestRate || 0;
          this.cibilInfo.creditLimit = response.creditLimit || 0;
          this.cibilInfo.scoreCategory = response.scoreCategory || '';
          this.cibilInfo.pan = response.pan || '';
          
          // Auto-fill interest rate in loan form
          if (response.interestRate) {
            this.loanForm.interestRate = response.interestRate;
          }
          
          this.cibilInfo.loading = false;
          console.log('CIBIL info loaded:', this.cibilInfo);
        } else {
          this.cibilInfo.loading = false;
          this.cibilInfo.error = response.message || 'Failed to check CIBIL';
        }
      },
      error: (err: any) => {
        console.error('Error checking CIBIL:', err);
        this.cibilInfo.loading = false;
        this.cibilInfo.error = err.error?.message || 'Failed to check CIBIL score';
      }
    });
  }

  loadUserLoans() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    
    // Load user's loans from MySQL database
    this.http.get(`${environment.apiUrl}/loans/account/${this.userAccountNumber}`).subscribe({
      next: (loans: any) => {
        console.log('User loans loaded from MySQL:', loans);
        this.loans = loans.map((loan: any) => ({
          id: loan.id,
          type: loan.type,
          amount: loan.amount,
          tenure: loan.tenure,
          interestRate: loan.interestRate,
          status: loan.status,
          userName: loan.userName,
          userEmail: loan.userEmail,
          accountNumber: loan.accountNumber,
          currentBalance: loan.currentBalance,
          loanAccountNumber: loan.loanAccountNumber,
          applicationDate: loan.applicationDate,
          approvalDate: loan.approvalDate,
          foreclosureDate: loan.foreclosureDate,
          foreclosureAmount: loan.foreclosureAmount,
          foreclosureCharges: loan.foreclosureCharges,
          foreclosureGst: loan.foreclosureGst,
          principalPaid: loan.principalPaid,
          interestPaid: loan.interestPaid,
          remainingPrincipal: loan.remainingPrincipal,
          remainingInterest: loan.remainingInterest,
          emiStartDate: loan.emiStartDate
        }));
        
        // Load EMIs for approved loans
        this.loans.forEach(loan => {
          if (loan.status === 'Approved' || loan.status === 'Paid') {
            this.loadEmisForLoan(loan.id);
          }
        });
        
        // Load account balance
        this.loadAccountBalance();
        
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading user loans from MySQL:', err);
        // Fallback to localStorage
        const savedLoans = localStorage.getItem('user_loans');
        if (savedLoans) {
          this.loans = JSON.parse(savedLoans).filter((loan: LoanRequest) => 
            loan.accountNumber === this.userAccountNumber
          );
        }
        this.loading = false;
      }
    });
  }

  submitLoan() {
    // Validate form
    if (!this.loanForm.type || this.loanForm.amount <= 0 || this.loanForm.tenure <= 0 || this.loanForm.interestRate <= 0) {
      this.alertService.error('Validation Error', 'Please fill all fields correctly!');
      return;
    }

    // Validate PAN
    if (!this.loanForm.pan || this.loanForm.pan.trim() === '') {
      this.alertService.error('PAN Required', 'PAN number is required. Please check CIBIL first.');
      return;
    }

    // Validate loan amount against credit limit
    if (this.cibilInfo.creditLimit > 0 && this.loanForm.amount > this.cibilInfo.creditLimit) {
      const confirmMsg = `Your available credit limit is ₹${this.cibilInfo.creditLimit.toLocaleString('en-IN')}, but you are requesting ₹${this.loanForm.amount.toLocaleString('en-IN')}.\n\nDo you want to proceed anyway?`;
      this.alertService.confirm(
        'Credit Limit Exceeded',
        confirmMsg,
        () => {
          this.proceedWithLoanSubmission();
        },
        () => {
          // User cancelled
        },
        'Proceed',
        'Cancel'
      );
      return;
    }
    
    this.proceedWithLoanSubmission();
  }

  private proceedWithLoanSubmission() {

    // Generate loan account number with better uniqueness
    const loanAccountNumber = 'LOAN' + Date.now() + Math.floor(Math.random() * 1000);

    const newLoan: LoanRequest = {
      id: Date.now(), // Temporary ID for frontend
      type: this.loanForm.type,
      amount: this.loanForm.amount,
      tenure: this.loanForm.tenure,
      interestRate: this.loanForm.interestRate,
      status: 'Pending',
      userName: this.userName,
      userEmail: this.userEmail,
      accountNumber: this.userAccountNumber,
      currentBalance: this.currentBalance,
      loanAccountNumber: loanAccountNumber,
      applicationDate: new Date().toISOString()
    };

    // Create backend loan object without ID (backend will generate it)
    const backendLoan = {
      type: newLoan.type,
      amount: newLoan.amount,
      tenure: newLoan.tenure,
      interestRate: newLoan.interestRate,
      status: newLoan.status,
      userName: newLoan.userName,
      userEmail: newLoan.userEmail,
      accountNumber: newLoan.accountNumber,
      currentBalance: newLoan.currentBalance,
      loanAccountNumber: newLoan.loanAccountNumber,
      applicationDate: newLoan.applicationDate,
      pan: this.loanForm.pan,
      cibilScore: this.cibilInfo.cibilScore,
      creditLimit: this.cibilInfo.creditLimit
    };

    // Submit to MySQL database
    this.http.post(`${environment.apiUrl}/loans`, backendLoan).subscribe({
      next: (response: any) => {
        console.log('Loan request created successfully in MySQL:', response);
        
        // Add to local loans array
        this.loans.unshift(newLoan);
        
        // Also save to localStorage as backup
        this.saveLoansToStorage();
        
        this.alertService.success('Loan Application Submitted', 'Loan request submitted successfully! Admin will review your application.');
        this.resetForm();
      },
      error: (err: any) => {
        console.error('Error creating loan request:', err);
        this.alertService.error('Submission Failed', 'Failed to submit loan request. Please try again.');
        
        // Fallback to localStorage
        this.loans.unshift(newLoan);
        this.saveLoansToStorage();
      }
    });
  }

  saveLoansToStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get all loans from localStorage
    const allLoans = JSON.parse(localStorage.getItem('user_loans') || '[]');
    
    // Update or add the current user's loans
    const otherLoans = allLoans.filter((loan: LoanRequest) => 
      loan.accountNumber !== this.userAccountNumber
    );
    
    // Combine other users' loans with current user's loans
    const updatedLoans = [...otherLoans, ...this.loans];
    
    // Save back to localStorage
    localStorage.setItem('user_loans', JSON.stringify(updatedLoans));
  }

  resetForm() {
    this.loanForm = { type: '', amount: 0, tenure: 0, interestRate: 0, pan: '' };
    // Don't reset CIBIL info as it's based on user's PAN
  }

  goBack() {
    this.router.navigate(['website/userdashboard']);
  }

  // EMI Calculation Method
  calculateEMI() {
    const principal = this.emiCalculator.principal;
    const rate = this.emiCalculator.rate;
    const time = this.emiCalculator.time;

    // Validate inputs
    if (principal <= 0 || rate <= 0 || time <= 0) {
      this.resetEMICalculator();
      return;
    }

    // Convert annual rate to monthly rate
    const monthlyRate = rate / (12 * 100);
    
    // Calculate EMI using the formula: EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
    const emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, time)) / 
                (Math.pow(1 + monthlyRate, time) - 1);
    
    // Calculate total amount and interest
    const totalAmount = emi * time;
    const totalInterest = totalAmount - principal;
    
    // Calculate percentages
    const interestPercentage = (totalInterest / totalAmount) * 100;
    const principalPercentage = (principal / totalAmount) * 100;

    // Update calculator results
    this.emiCalculator.emi = emi;
    this.emiCalculator.totalAmount = totalAmount;
    this.emiCalculator.totalInterest = totalInterest;
    this.emiCalculator.interestPercentage = interestPercentage;
    this.emiCalculator.principalPercentage = principalPercentage;

    console.log('EMI Calculation:', {
      principal,
      rate,
      time,
      emi,
      totalAmount,
      totalInterest,
      interestPercentage,
      principalPercentage
    });
  }

  // Reset EMI Calculator
  resetEMICalculator() {
    this.emiCalculator = {
      principal: 0,
      rate: 0,
      time: 0,
      emi: 0,
      totalAmount: 0,
      totalInterest: 0,
      interestPercentage: 0,
      principalPercentage: 0
    };
  }

  // Auto-fill EMI calculator from loan form
  fillEMICalculator() {
    this.emiCalculator.principal = this.loanForm.amount;
    this.emiCalculator.rate = this.loanForm.interestRate;
    this.emiCalculator.time = this.loanForm.tenure;
    this.calculateEMI();
  }

  // Download loan details as PDF
  downloadForeclosurePDF(loan: LoanRequest) {
    if (!loan.loanAccountNumber) {
      this.alertService.error('Error', 'Loan account number not found');
      return;
    }

    this.http.get(`${environment.apiUrl}/loans/foreclosure/pdf/${loan.loanAccountNumber}`, {
      responseType: 'blob'
    }).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `Foreclosure_Statement_${loan.loanAccountNumber}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      },
      error: (err: any) => {
        console.error('Error downloading foreclosure PDF:', err);
        this.alertService.error('Download Failed', 'Failed to download foreclosure PDF. Please try again.');
      }
    });
  }

  downloadLoanPDF(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      // Create PDF content
      const pdfContent = this.generatePDFContent(loan);
      
      // Create and download as HTML file (can be printed as PDF)
      const blob = new Blob([pdfContent], { type: 'text/html;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `Loan_Details_${loan.loanAccountNumber}.html`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      // Show instruction to user
      setTimeout(() => {
        this.alertService.confirm(
          'Document Downloaded',
          'Document downloaded! Would you like to open it now to save as PDF?',
          () => {
            // Open the downloaded file in a new window for printing
            const newWindow = window.open();
            if (newWindow) {
              newWindow.document.write(pdfContent);
              newWindow.document.close();
              // Focus on the new window
              newWindow.focus();
            }
          },
          () => {
            // User cancelled
          },
          'Open',
          'Cancel'
        );
      }, 500);
      
      console.log('Loan PDF download initiated successfully');
    } catch (error) {
      console.error('Error downloading loan PDF:', error);
      this.alertService.error('Download Failed', 'Failed to download loan document. Please try again.');
    }
  }

  // Generate PDF content with bank stamp and loan details
  private generatePDFContent(loan: LoanRequest): string {
    const currentDate = new Date().toLocaleDateString('en-IN');
    const currentTime = new Date().toLocaleTimeString('en-IN');
    
    return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Loan Details - NeoBank</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            margin: 0;
            padding: 20px;
            background: #f8f9fa;
            color: #333;
            position: relative;
        }
        .watermark {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%) rotate(-45deg);
            font-size: 60px;
            color: rgba(30, 64, 175, 0.1);
            font-weight: bold;
            z-index: -1;
            pointer-events: none;
            white-space: nowrap;
        }
        .watermark-logo {
            position: fixed;
            top: 20%;
            left: 20%;
            transform: rotate(-30deg);
            font-size: 40px;
            color: rgba(30, 64, 175, 0.08);
            z-index: -1;
            pointer-events: none;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            position: relative;
            z-index: 1;
        }
        .header {
            text-align: center;
            border-bottom: 3px solid #667eea;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }
        .bank-logo {
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
            margin-bottom: 10px;
        }
        .bank-name {
            font-size: 24px;
            color: #764ba2;
            margin-bottom: 5px;
        }
        .bank-tagline {
            font-size: 14px;
            color: #666;
            font-style: italic;
        }
        .document-title {
            font-size: 28px;
            color: #333;
            text-align: center;
            margin: 30px 0;
            font-weight: bold;
        }
        .section {
            margin-bottom: 25px;
        }
        .section-title {
            font-size: 18px;
            color: #667eea;
            border-bottom: 2px solid #e9ecef;
            padding-bottom: 8px;
            margin-bottom: 15px;
            font-weight: bold;
        }
        .info-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
            margin-bottom: 20px;
        }
        .info-item {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #f0f0f0;
        }
        .info-label {
            font-weight: bold;
            color: #555;
        }
        .info-value {
            color: #333;
        }
        .amount-highlight {
            color: #2e7d32;
            font-weight: bold;
            font-size: 16px;
        }
        .status-badge {
            padding: 6px 12px;
            border-radius: 20px;
            font-weight: bold;
            font-size: 12px;
            text-transform: uppercase;
        }
        .status-pending {
            background: #fef9e7;
            color: #f39c12;
        }
        .status-approved {
            background: #e8f5e8;
            color: #27ae60;
        }
        .status-rejected {
            background: #fdeaea;
            color: #e74c3c;
        }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #e9ecef;
            text-align: center;
            color: #666;
            font-size: 12px;
        }
        .stamp {
            position: absolute;
            top: 20px;
            right: 20px;
            width: 120px;
            height: 120px;
            border: 3px solid #667eea;
            border-radius: 50%;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            background: rgba(102, 126, 234, 0.1);
            transform: rotate(-15deg);
        }
        .stamp-text {
            font-size: 10px;
            font-weight: bold;
            color: #667eea;
            text-align: center;
            line-height: 1.2;
        }
        .stamp-date {
            font-size: 8px;
            color: #666;
            margin-top: 5px;
        }
        @media print {
            body { 
                background: white !important; 
                margin: 0;
                padding: 10px;
            }
            .container { 
                box-shadow: none !important; 
                margin: 0;
                padding: 20px;
            }
            .stamp {
                position: fixed !important;
                top: 20px !important;
                right: 20px !important;
            }
        }
        
        /* Ensure CSS works in downloaded file */
        * {
            box-sizing: border-box;
        }
        
        body {
            font-family: Arial, sans-serif !important;
            margin: 0 !important;
            padding: 20px !important;
            background: #f8f9fa !important;
            color: #333 !important;
        }
    </style>
</head>
<body>
    <div class="watermark">NeoBank</div>
    <div class="watermark-logo">🏦</div>
    <div class="container">
        <!-- Bank Stamp -->
        <div class="stamp">
            <div class="stamp-text">
                NEOBANK<br>
                OFFICIAL<br>
                DOCUMENT
            </div>
            <div class="stamp-date">${currentDate}</div>
        </div>

        <!-- Header -->
        <div class="header">
            <div class="bank-logo">🏦</div>
            <div class="bank-name">NeoBank</div>
            <div class="bank-tagline">Your Digital Banking Partner</div>
        </div>

        <!-- Document Title -->
        <div class="document-title">LOAN APPLICATION DETAILS</div>

        <!-- User Information -->
        <div class="section">
            <div class="section-title">👤 Customer Information</div>
            <div class="info-grid">
                <div class="info-item">
                    <span class="info-label">Customer Name:</span>
                    <span class="info-value">${this.userName}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Email Address:</span>
                    <span class="info-value">${this.userEmail}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Account Number:</span>
                    <span class="info-value">${this.userAccountNumber}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Current Balance:</span>
                    <span class="info-value amount-highlight">₹${this.currentBalance.toLocaleString('en-IN')}</span>
                </div>
            </div>
        </div>

        <!-- Loan Information -->
        <div class="section">
            <div class="section-title">💰 Loan Details</div>
            <div class="info-grid">
                <div class="info-item">
                    <span class="info-label">Loan ID:</span>
                    <span class="info-value">#${loan.id}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Loan Account Number:</span>
                    <span class="info-value">${loan.loanAccountNumber}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Loan Type:</span>
                    <span class="info-value">${loan.type}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Loan Amount:</span>
                    <span class="info-value amount-highlight">₹${loan.amount.toLocaleString('en-IN')}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Tenure:</span>
                    <span class="info-value">${loan.tenure} months</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Interest Rate:</span>
                    <span class="info-value">${loan.interestRate}% per annum</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Application Status:</span>
                    <span class="info-value">
                        <span class="status-badge status-${loan.status.toLowerCase()}">${loan.status}</span>
                    </span>
                </div>
                <div class="info-item">
                    <span class="info-label">Application Date:</span>
                    <span class="info-value">${new Date(loan.applicationDate).toLocaleDateString('en-IN')}</span>
                </div>
                ${loan.approvalDate ? `
                <div class="info-item">
                    <span class="info-label">Approval Date:</span>
                    <span class="info-value">${new Date(loan.approvalDate).toLocaleDateString('en-IN')}</span>
                </div>
                ` : ''}
            </div>
        </div>

        <!-- EMI Calculation (if available) -->
        ${this.emiCalculator.emi > 0 ? `
        <div class="section">
            <div class="section-title">📊 EMI Calculation</div>
            <div class="info-grid">
                <div class="info-item">
                    <span class="info-label">Monthly EMI:</span>
                    <span class="info-value amount-highlight">₹${this.emiCalculator.emi.toLocaleString('en-IN')}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Total Amount:</span>
                    <span class="info-value amount-highlight">₹${this.emiCalculator.totalAmount.toLocaleString('en-IN')}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Total Interest:</span>
                    <span class="info-value">₹${this.emiCalculator.totalInterest.toLocaleString('en-IN')}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Interest Percentage:</span>
                    <span class="info-value">${this.emiCalculator.interestPercentage.toFixed(2)}%</span>
                </div>
            </div>
        </div>
        ` : ''}

        <!-- Footer -->
        <div class="footer">
            <p><strong>NeoBank - Digital Banking Solutions</strong></p>
            <p>This document was generated on ${currentDate} at ${currentTime}</p>
            <p>For any queries, contact us at support@neobank.com or call +91-1800-123-4567</p>
            <p style="margin-top: 15px; font-size: 10px; color: #999;">
                This is a computer-generated document and does not require a signature.
            </p>
        </div>
    </div>
</body>
</html>
    `;
  }

  // Load EMIs for a specific loan
  loadEmisForLoan(loanId: number) {
    this.http.get(`${environment.apiUrl}/emis/loan/${loanId}`).subscribe({
      next: (emis: any) => {
        this.emiPayments.set(loanId, emis);
        console.log(`Loaded ${emis.length} EMIs for loan ${loanId}`);
      },
      error: (err: any) => {
        console.error('Error loading EMIs:', err);
      }
    });
  }

  // Get EMIs for a loan
  getEmisForLoan(loanId: number): EmiPayment[] {
    return this.emiPayments.get(loanId) || [];
  }

  // Get pending EMIs for a loan
  getPendingEmis(loanId: number): EmiPayment[] {
    const emis = this.getEmisForLoan(loanId);
    return emis.filter(emi => emi.status === 'Pending');
  }

  // Get next due EMI
  getNextDueEmi(loanId: number): EmiPayment | null {
    const pending = this.getPendingEmis(loanId);
    if (pending.length === 0) return null;
    return pending.sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime())[0];
  }

  // Show EMI details for a loan
  showEmiDetails(loan: LoanRequest) {
    this.selectedLoanForEmi = loan;
    if (!this.emiPayments.has(loan.id)) {
      this.loadEmisForLoan(loan.id);
    }
  }

  // Close EMI details
  closeEmiDetails() {
    this.selectedLoanForEmi = null;
  }

  // Pay EMI - Show custom modal
  payEmi(emi: EmiPayment) {
    this.selectedEmiForPayment = emi;
    this.showEmiPaymentModal = true;
  }

  // Close payment modal
  closeEmiPaymentModal() {
    this.showEmiPaymentModal = false;
    this.selectedEmiForPayment = null;
  }

  // Confirm and process EMI payment
  confirmEmiPayment() {
    if (!this.selectedEmiForPayment) return;

    const emi = this.selectedEmiForPayment;
    this.payingEmi = true;
    
    this.http.post(`${environment.apiUrl}/emis/pay/${emi.id}?accountNumber=${this.userAccountNumber}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          // Close modal
          this.closeEmiPaymentModal();
          
          // Show success message with custom modal
          this.alertService.success(
            'EMI Payment Successful', 
            `EMI #${emi.emiNumber} of ₹${emi.totalAmount.toFixed(2)} has been paid successfully!\n\nPrincipal: ₹${emi.principalAmount.toFixed(2)}\nInterest: ₹${emi.interestAmount.toFixed(2)}\n\nYour new balance: ₹${response.newBalance?.toFixed(2) || 'N/A'}`,
            true,
            5000
          );
          
          // Reload EMIs
          if (emi.loanId) {
            this.loadEmisForLoan(emi.loanId);
          }
          // Reload loans to update balance
          this.loadUserLoans();
          // Reload account balance
          this.loadAccountBalance();
        } else {
          this.alertService.error('Payment Failed', response.message || 'Failed to pay EMI');
        }
        this.payingEmi = false;
      },
      error: (err: any) => {
        console.error('Error paying EMI:', err);
        this.alertService.error('Payment Error', err.error?.message || 'Failed to pay EMI. Please try again.');
        this.payingEmi = false;
      }
    });
  }

  // Download EMI receipt
  downloadEmiReceipt(emi: EmiPayment) {
    this.http.get(`${environment.apiUrl}/emis/${emi.id}/receipt`, {
      responseType: 'blob'
    }).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `EMI_Receipt_${emi.loanAccountNumber}_${emi.emiNumber}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      },
      error: (err: any) => {
        console.error('Error downloading EMI receipt:', err);
        this.alertService.error('Download Failed', 'Failed to download EMI receipt. Please try again.');
      }
    });
  }

  // Load account balance
  loadAccountBalance() {
    if (!this.userAccountNumber) return;
    this.http.get(`${environment.apiUrl}/accounts/balance/${this.userAccountNumber}`).subscribe({
      next: (response: any) => {
        if (response.balance !== undefined) {
          this.currentBalance = response.balance;
        }
      },
      error: (err: any) => {
        console.error('Error loading balance:', err);
      }
    });
  }

  // Check if there are approved loans
  hasApprovedLoans(): boolean {
    return this.loans.length > 0 && this.loans.some(loan => loan.status === 'Approved');
  }

  // Check if there are foreclosed loans
  hasForeclosedLoans(): boolean {
    return this.loans.some(loan => loan.status === 'Foreclosed');
  }

  // Get total EMI count for a loan
  getTotalEmiCount(loanId: number): number {
    return this.getEmisForLoan(loanId).length;
  }

  // Get paid EMI count for a loan
  getPaidEmiCount(loanId: number): number {
    return this.getEmisForLoan(loanId).filter(emi => emi.status === 'Paid').length;
  }

  // Get pending EMI count for a loan
  getPendingEmiCount(loanId: number): number {
    return this.getEmisForLoan(loanId).filter(emi => emi.status === 'Pending').length;
  }
}
