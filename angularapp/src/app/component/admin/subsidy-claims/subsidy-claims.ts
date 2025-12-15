import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SubsidyClaimService } from '../../../service/subsidy-claim.service';
import { EducationLoanSubsidyClaim } from '../../../model/subsidy-claim/subsidy-claim-module';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-subsidy-claims',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subsidy-claims.html',
  styleUrls: ['./subsidy-claims.css']
})
export class SubsidyClaims implements OnInit {
  claims: EducationLoanSubsidyClaim[] = [];
  filteredClaims: EducationLoanSubsidyClaim[] = [];
  selectedClaim: EducationLoanSubsidyClaim | null = null;
  isLoading: boolean = false;
  
  // Filters
  statusFilter: string = 'All';
  searchTerm: string = '';
  
  // Modal states
  showDetailsModal: boolean = false;
  showEditAmountModal: boolean = false;
  showUserDetailsModal: boolean = false;
  
  // Edit amount
  editedAmount: number = 0;
  adminNotes: string = '';
  rejectionReason: string = '';
  
  // Admin name
  adminName: string = 'Admin';
  
  // User details
  userDetails: any = null;
  userLoans: any[] = [];
  userTransactions: any[] = [];

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private subsidyClaimService: SubsidyClaimService,
    private alertService: AlertService,
    private http: HttpClient
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      // Get admin name from session
      const adminData = sessionStorage.getItem('admin');
      if (adminData) {
        try {
          const admin = JSON.parse(adminData);
          this.adminName = admin.username || admin.name || 'Admin';
        } catch (e) {
          console.error('Error parsing admin data:', e);
        }
      }
      this.loadClaims();
    }
  }

  loadClaims() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.isLoading = true;
    this.subsidyClaimService.getAllClaims().subscribe({
      next: (claims) => {
        this.claims = claims;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading subsidy claims:', err);
        this.alertService.error('Error', 'Failed to load subsidy claims');
        this.isLoading = false;
      }
    });
  }

  applyFilters() {
    this.filteredClaims = this.claims.filter(claim => {
      const matchesStatus = this.statusFilter === 'All' || claim.status === this.statusFilter;
      const matchesSearch = !this.searchTerm || 
        claim.userName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        claim.accountNumber.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        claim.loanAccountNumber.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        claim.userEmail.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      return matchesStatus && matchesSearch;
    });
  }

  onStatusFilterChange() {
    this.applyFilters();
  }

  onSearchChange() {
    this.applyFilters();
  }

  viewClaimDetails(claim: EducationLoanSubsidyClaim) {
    this.selectedClaim = claim;
    this.showDetailsModal = true;
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.selectedClaim = null;
  }

  viewUserDetails(claim: EducationLoanSubsidyClaim) {
    this.selectedClaim = claim;
    this.loadUserDetails(claim.accountNumber);
    this.showUserDetailsModal = true;
  }

  loadUserDetails(accountNumber: string) {
    // Load user details
    this.http.get(`${environment.apiUrl}/users/account/${accountNumber}`).subscribe({
      next: (user: any) => {
        this.userDetails = user;
      },
      error: (err) => {
        console.error('Error loading user details:', err);
      }
    });

    // Load user loans
    this.http.get(`${environment.apiUrl}/loans/account/${accountNumber}`).subscribe({
      next: (loans: any) => {
        this.userLoans = loans || [];
      },
      error: (err) => {
        console.error('Error loading user loans:', err);
      }
    });

    // Load user transactions
    this.http.get(`${environment.apiUrl}/transactions/account/${accountNumber}`).subscribe({
      next: (transactions: any) => {
        this.userTransactions = transactions || [];
      },
      error: (err) => {
        console.error('Error loading user transactions:', err);
      }
    });
  }

  closeUserDetailsModal() {
    this.showUserDetailsModal = false;
    this.userDetails = null;
    this.userLoans = [];
    this.userTransactions = [];
  }

  openEditAmountModal(claim: EducationLoanSubsidyClaim) {
    this.selectedClaim = claim;
    this.editedAmount = claim.approvedSubsidyAmount;
    this.adminNotes = claim.adminNotes || '';
    this.showEditAmountModal = true;
  }

  closeEditAmountModal() {
    this.showEditAmountModal = false;
    this.selectedClaim = null;
    this.editedAmount = 0;
    this.adminNotes = '';
  }

  saveEditedAmount() {
    if (!this.selectedClaim || !this.selectedClaim.id) return;
    
    if (this.editedAmount <= 0) {
      this.alertService.error('Error', 'Amount must be greater than 0');
      return;
    }

    this.isLoading = true;
    this.subsidyClaimService.updateApprovedAmount(
      this.selectedClaim.id,
      this.editedAmount,
      this.adminName
    ).subscribe({
      next: (updatedClaim) => {
        this.alertService.success('Success', 'Subsidy amount updated successfully');
        this.closeEditAmountModal();
        this.loadClaims();
      },
      error: (err) => {
        console.error('Error updating amount:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update amount');
        this.isLoading = false;
      }
    });
  }

  approveClaim(claim: EducationLoanSubsidyClaim) {
    if (!confirm(`Approve subsidy claim for ${claim.userName}?`)) return;

    this.isLoading = true;
    this.subsidyClaimService.approveClaim(
      claim.id!,
      claim.approvedSubsidyAmount,
      this.adminName,
      this.adminNotes
    ).subscribe({
      next: (approvedClaim) => {
        this.alertService.success('Success', 'Subsidy claim approved successfully');
        this.loadClaims();
      },
      error: (err) => {
        console.error('Error approving claim:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve claim');
        this.isLoading = false;
      }
    });
  }

  rejectClaim(claim: EducationLoanSubsidyClaim) {
    const reason = prompt('Enter rejection reason:');
    if (!reason || !confirm(`Reject subsidy claim for ${claim.userName}?`)) return;

    this.isLoading = true;
    this.subsidyClaimService.rejectClaim(
      claim.id!,
      this.adminName,
      reason
    ).subscribe({
      next: (rejectedClaim) => {
        this.alertService.success('Success', 'Subsidy claim rejected');
        this.loadClaims();
      },
      error: (err) => {
        console.error('Error rejecting claim:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject claim');
        this.isLoading = false;
      }
    });
  }

  creditSubsidy(claim: EducationLoanSubsidyClaim) {
    if (!confirm(`Credit ₹${claim.approvedSubsidyAmount} to ${claim.userName}'s account?`)) return;

    this.isLoading = true;
    this.subsidyClaimService.creditSubsidy(claim.id!, this.adminName).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Success', `Subsidy of ₹${claim.approvedSubsidyAmount} credited successfully`);
          this.loadClaims();
        } else {
          this.alertService.error('Error', response.message || 'Failed to credit subsidy');
          this.isLoading = false;
        }
      },
      error: (err) => {
        console.error('Error crediting subsidy:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to credit subsidy');
        this.isLoading = false;
      }
    });
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
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  }

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

  get pendingCount(): number {
    return this.claims.filter(c => c.status === 'Pending').length;
  }

  get approvedCount(): number {
    return this.claims.filter(c => c.status === 'Approved').length;
  }

  get creditedCount(): number {
    return this.claims.filter(c => c.status === 'Credited').length;
  }

  get totalSubsidyAmount(): number {
    return this.claims
      .filter(c => c.status === 'Credited')
      .reduce((sum, c) => sum + (c.approvedSubsidyAmount || 0), 0);
  }
  
  // Download subsidy details as PDF
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
            <p><strong>Generated on:</strong> ${currentDate} at ${currentTime} | <strong>By:</strong> ${this.adminName}</p>
        </div>
    </div>
</body>
</html>`;
  }
  
  // Export all claims to CSV
  exportClaimsToCSV() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const headers = ['ID', 'User Name', 'Account Number', 'Loan Account', 'Loan Amount', 'Calculated Subsidy', 'Approved Amount', 'Status', 'Request Date', 'Processed Date', 'Credited Date'];
    const rows = this.claims.map(claim => [
      claim.id || '',
      claim.userName || '',
      claim.accountNumber || '',
      claim.loanAccountNumber || '',
      claim.loanAmount || 0,
      claim.calculatedSubsidyAmount || 0,
      claim.approvedSubsidyAmount || 0,
      claim.status || '',
      claim.requestDate || '',
      claim.processedDate || '',
      claim.creditedDate || ''
    ]);
    
    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `subsidy_claims_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
  
  // Get statistics
  getStatistics() {
    return {
      total: this.claims.length,
      pending: this.pendingCount,
      approved: this.approvedCount,
      credited: this.creditedCount,
      rejected: this.claims.filter(c => c.status === 'Rejected').length,
      totalAmount: this.totalSubsidyAmount,
      averageAmount: this.claims.length > 0 ? 
        this.claims.reduce((sum, c) => sum + (c.approvedSubsidyAmount || 0), 0) / this.claims.length : 0
    };
  }
}

