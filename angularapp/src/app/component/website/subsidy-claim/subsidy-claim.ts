import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';
import { SubsidyClaimService } from '../../../service/subsidy-claim.service';
import { EducationLoanSubsidyClaim } from '../../../model/subsidy-claim/subsidy-claim-module';

interface Loan {
  id: number;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: string;
  loanAccountNumber: string;
  applicationDate: string;
  approvalDate?: string;
}

@Component({
  selector: 'app-subsidy-claim',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subsidy-claim.html',
  styleUrls: ['./subsidy-claim.css']
})
export class SubsidyClaim implements OnInit {
  userAccountNumber: string = '';
  userName: string = '';
  userEmail: string = '';
  educationLoans: Loan[] = [];
  subsidyClaims: EducationLoanSubsidyClaim[] = [];
  loading: boolean = false;
  
  // Claim request
  selectedLoanId: number | null = null;
  childAadharNumber: string = '';
  userNotes: string = '';
  termsAccepted: boolean = false;
  showClaimModal: boolean = false;
  showTermsModal: boolean = false;
  submittingClaim: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService,
    private subsidyClaimService: SubsidyClaimService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserData();
    }
  }

  loadUserData() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.userAccountNumber = user.accountNumber;
      this.userName = user.name;
      this.userEmail = user.email;
    } else {
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        const profile = JSON.parse(savedProfile);
        this.userAccountNumber = profile.accountNumber;
        this.userName = profile.name;
        this.userEmail = profile.email;
      }
    }
    
    if (this.userAccountNumber) {
      this.loadEducationLoans();
      this.loadSubsidyClaims();
    }
  }

  loadEducationLoans() {
    if (!this.userAccountNumber) return;
    
    this.loading = true;
    this.http.get<any>(`${environment.apiUrl}/loans/account/${this.userAccountNumber}`).subscribe({
      next: (response) => {
        // Handle both old format (array) and new format (object with loans array)
        const loansData = Array.isArray(response) ? response : (response.loans || response);
        
        // Filter only Education Loans that are Approved
        this.educationLoans = loansData.filter((loan: Loan) => 
          loan.type === 'Education Loan' && loan.status === 'Approved'
        );
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading education loans:', err);
        this.alertService.error('Error', 'Failed to load education loans');
        this.loading = false;
      }
    });
  }

  loadSubsidyClaims() {
    if (!this.userAccountNumber) return;
    
    this.subsidyClaimService.getClaimsByAccountNumber(this.userAccountNumber).subscribe({
      next: (claims) => {
        this.subsidyClaims = claims;
      },
      error: (err) => {
        console.error('Error loading subsidy claims:', err);
      }
    });
  }

  openClaimModal(loan: Loan) {
    // Check if user has already made 3 claims for this loan
    const existingClaims = this.getClaimsForLoan(loan.id);
    if (existingClaims.length >= 3) {
      this.alertService.error('Limit Reached', 'You have already made 3 subsidy claims for this loan. Maximum limit reached.');
      return;
    }
    
    this.selectedLoanId = loan.id;
    this.childAadharNumber = '';
    this.userNotes = '';
    this.termsAccepted = false;
    this.showClaimModal = true;
  }
  
  getClaimsForLoan(loanId: number): EducationLoanSubsidyClaim[] {
    return this.subsidyClaims.filter(c => c.loanId === loanId);
  }
  
  getClaimCountForLoan(loanId: number): number {
    return this.getClaimsForLoan(loanId).length;
  }
  
  canRequestMoreClaims(loanId: number): boolean {
    return this.getClaimCountForLoan(loanId) < 3;
  }
  
  downloadSubsidyDetails(claim: EducationLoanSubsidyClaim) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    try {
      const htmlContent = this.generateSubsidyDetailsPDF(claim);
      const printWindow = window.open('', '_blank');
      if (printWindow) {
        printWindow.document.write(htmlContent);
        printWindow.document.close();
        printWindow.onload = () => {
          setTimeout(() => {
            printWindow.print();
          }, 250);
        };
      }
    } catch (error) {
      console.error('Error generating subsidy details PDF:', error);
      this.alertService.error('Error', 'Failed to download subsidy details. Please try again.');
    }
  }
  
  generateSubsidyDetailsPDF(claim: EducationLoanSubsidyClaim): string {
    const currentDate = new Date().toLocaleDateString('en-IN');
    const currentTime = new Date().toLocaleTimeString('en-IN');
    
    return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Education Loan Subsidy Claim Details</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            margin: 0;
            padding: 20px;
            background: #f8f9fa;
            color: #333;
        }
        .container {
            max-width: 900px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }
        .header {
            text-align: center;
            border-bottom: 3px solid #0077cc;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }
        .bank-logo {
            font-size: 32px;
            font-weight: bold;
            color: #0077cc;
            margin-bottom: 10px;
        }
        .document-title {
            text-align: center;
            font-size: 20px;
            font-weight: bold;
            color: #333;
            margin: 30px 0;
            padding: 15px;
            background: linear-gradient(135deg, #e3f2fd, #bbdefb);
            border-radius: 8px;
        }
        .details-section {
            margin-bottom: 30px;
        }
        .details-section h4 {
            color: #0077cc;
            border-bottom: 2px solid #0077cc;
            padding-bottom: 10px;
            margin-bottom: 15px;
        }
        .details-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 15px;
        }
        .detail-item {
            background: #f8f9fa;
            padding: 12px;
            border-radius: 6px;
            border-left: 3px solid #0077cc;
        }
        .detail-item label {
            display: block;
            font-size: 12px;
            color: #666;
            margin-bottom: 5px;
            font-weight: 600;
        }
        .detail-item span {
            display: block;
            font-size: 14px;
            color: #333;
            font-weight: 500;
        }
        .amount-highlight {
            color: #28a745;
            font-weight: 700;
            font-size: 16px;
        }
        .status-badge {
            display: inline-block;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 600;
            text-transform: uppercase;
        }
        .status-pending { background: #fff3cd; color: #856404; }
        .status-approved { background: #d4edda; color: #155724; }
        .status-rejected { background: #f8d7da; color: #721c24; }
        .status-credited { background: #d1ecf1; color: #0c5460; }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #e9ecef;
            text-align: center;
            color: #666;
            font-size: 12px;
        }
        @media print {
            body { margin: 0; }
            .container { box-shadow: none; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="bank-logo">🏦 NeoBank</div>
            <div style="font-size: 24px; color: #1e40af; margin-bottom: 5px;">NeoBank India Limited</div>
            <div style="font-size: 14px; color: #666; font-style: italic;">Relationship beyond banking</div>
        </div>

        <div class="document-title">
            🎓 EDUCATION LOAN INTEREST SUBSIDY CLAIM DETAILS
        </div>

        <div class="details-section">
            <h4>📋 Claim Information</h4>
            <div class="details-grid">
                <div class="detail-item">
                    <label>Claim ID</label>
                    <span>${claim.id || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Status</label>
                    <span><span class="status-badge status-${claim.status.toLowerCase()}">${claim.status}</span></span>
                </div>
                <div class="detail-item">
                    <label>Loan Account Number</label>
                    <span>${claim.loanAccountNumber || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Request Date</label>
                    <span>${this.formatDate(claim.requestDate) || 'N/A'}</span>
                </div>
            </div>
        </div>

        <div class="details-section">
            <h4>👤 User Information</h4>
            <div class="details-grid">
                <div class="detail-item">
                    <label>Name</label>
                    <span>${claim.userName || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Email</label>
                    <span>${claim.userEmail || 'N/A'}</span>
                </div>
                <div class="detail-item">
                    <label>Account Number</label>
                    <span>${claim.accountNumber || 'N/A'}</span>
                </div>
                ${claim.childAadharNumber ? `
                <div class="detail-item">
                    <label>Child's Aadhar Number</label>
                    <span>${claim.childAadharNumber}</span>
                </div>
                ` : ''}
            </div>
        </div>

        <div class="details-section">
            <h4>💰 Loan & Subsidy Details</h4>
            <div class="details-grid">
                <div class="detail-item">
                    <label>Loan Amount</label>
                    <span>${this.formatCurrency(claim.loanAmount)}</span>
                </div>
                <div class="detail-item">
                    <label>Interest Rate</label>
                    <span>${claim.interestRate}% p.a.</span>
                </div>
                <div class="detail-item">
                    <label>Loan Tenure</label>
                    <span>${claim.loanTenure} months</span>
                </div>
                <div class="detail-item">
                    <label>Calculated Subsidy (3 Years)</label>
                    <span class="amount-highlight">${this.formatCurrency(claim.calculatedSubsidyAmount)}</span>
                </div>
                <div class="detail-item">
                    <label>Approved Amount</label>
                    <span class="amount-highlight">${this.formatCurrency(claim.approvedSubsidyAmount)}</span>
                </div>
            </div>
        </div>

        ${claim.processedDate ? `
        <div class="details-section">
            <h4>⚙️ Processing Information</h4>
            <div class="details-grid">
                <div class="detail-item">
                    <label>Processed Date</label>
                    <span>${this.formatDate(claim.processedDate)}</span>
                </div>
                <div class="detail-item">
                    <label>Processed By</label>
                    <span>${claim.processedBy || 'N/A'}</span>
                </div>
                ${claim.creditedDate ? `
                <div class="detail-item">
                    <label>Credited Date</label>
                    <span>${this.formatDate(claim.creditedDate)}</span>
                </div>
                <div class="detail-item">
                    <label>Transaction ID</label>
                    <span>${claim.transactionId || 'N/A'}</span>
                </div>
                ` : ''}
            </div>
        </div>
        ` : ''}

        ${claim.userNotes ? `
        <div class="details-section">
            <h4>📝 User Notes</h4>
            <div class="detail-item" style="grid-column: 1 / -1;">
                <span>${claim.userNotes}</span>
            </div>
        </div>
        ` : ''}

        ${claim.adminNotes ? `
        <div class="details-section">
            <h4>📝 Admin Notes</h4>
            <div class="detail-item" style="grid-column: 1 / -1;">
                <span>${claim.adminNotes}</span>
            </div>
        </div>
        ` : ''}

        <div class="footer">
            <p><strong>📍 Registered Office:</strong> NeoBank Tower, Financial District, Mumbai - 400001</p>
            <p><strong>📞 Customer Care:</strong> 1800-NEOBANK | <strong>📧 Email:</strong> support@neobank.in</p>
            <p><strong>🌐 Website:</strong> www.neobank.in</p>
            <p><strong>📄 This is a computer generated document and does not require signature.</strong></p>
            <p>© ${new Date().getFullYear()} NeoBank. All rights reserved.</p>
            <p><strong>Generated on:</strong> ${currentDate} at ${currentTime}</p>
        </div>
    </div>
</body>
</html>`;
  }

  closeClaimModal() {
    this.showClaimModal = false;
    this.selectedLoanId = null;
    this.childAadharNumber = '';
    this.userNotes = '';
    this.termsAccepted = false;
  }

  submitClaim() {
    if (!this.selectedLoanId) {
      this.alertService.error('Error', 'Please select a loan');
      return;
    }

    if (!this.childAadharNumber || this.childAadharNumber.trim() === '') {
      this.alertService.error('Error', 'Please enter child\'s Aadhar number');
      return;
    }

    if (this.childAadharNumber.length !== 12 || !/^\d{12}$/.test(this.childAadharNumber)) {
      this.alertService.error('Error', 'Aadhar number must be exactly 12 digits');
      return;
    }

    if (!this.termsAccepted) {
      this.alertService.error('Error', 'Please accept the Terms and Conditions to proceed');
      return;
    }

    this.submittingClaim = true;
    this.subsidyClaimService.createClaim({
      loanId: this.selectedLoanId,
      childAadharNumber: this.childAadharNumber.trim(),
      userNotes: this.userNotes
    }).subscribe({
      next: (claim) => {
        this.alertService.success('Success', 'Subsidy claim submitted successfully! Admin will review your request.');
        this.closeClaimModal();
        this.loadSubsidyClaims();
        this.submittingClaim = false;
      },
      error: (err) => {
        console.error('Error submitting claim:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to submit subsidy claim');
        this.submittingClaim = false;
      }
    });
  }

  getClaimForLoan(loanId: number): EducationLoanSubsidyClaim | undefined {
    // Return the most recent claim
    const claims = this.getClaimsForLoan(loanId);
    return claims.length > 0 ? claims[0] : undefined;
  }
  
  getAllClaimsForLoan(loanId: number): EducationLoanSubsidyClaim[] {
    return this.getClaimsForLoan(loanId).sort((a, b) => 
      new Date(b.requestDate).getTime() - new Date(a.requestDate).getTime()
    );
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Pending': return 'status-pending';
      case 'Approved': return 'status-approved';
      case 'Rejected': return 'status-rejected';
      case 'Credited': return 'status-credited';
      default: return '';
    }
  }

  formatDate(dateString?: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  }

  calculateSubsidy(loan: Loan): number {
    // This is a rough estimate - actual calculation is done on backend
    // 3 years = 36 months of interest
    const monthlyRate = loan.interestRate / (12 * 100);
    let totalInterest = 0;
    let remainingPrincipal = loan.amount;
    
    for (let month = 1; month <= 36; month++) {
      const interestForMonth = remainingPrincipal * monthlyRate;
      totalInterest += interestForMonth;
      
      // Approximate principal reduction
      const emi = (loan.amount * monthlyRate * Math.pow(1 + monthlyRate, loan.tenure)) / 
                  (Math.pow(1 + monthlyRate, loan.tenure) - 1);
      const principalPaid = emi - interestForMonth;
      remainingPrincipal = Math.max(0, remainingPrincipal - principalPaid);
    }
    
    return Math.round(totalInterest * 100) / 100;
  }
}

