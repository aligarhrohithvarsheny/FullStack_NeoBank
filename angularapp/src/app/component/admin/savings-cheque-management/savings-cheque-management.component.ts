import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { SavingsChequeService } from '../../../service/savings-cheque.service';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';
import {
  SavingsChequeRequestAdmin,
  SavingsChequeManagementStats,
  SavingsChequeApprovalResponse,
  SavingsChequeStatus
} from '../../../model/cheque/savings-cheque.model';

@Component({
  selector: 'app-savings-cheque-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './savings-cheque-management.component.html',
  styleUrls: ['./savings-cheque-management.component.css']
})
export class SavingsChequeManagementComponent implements OnInit, OnDestroy {
  // Data
  chequeRequests: SavingsChequeRequestAdmin[] = [];
  selectedCheque: SavingsChequeRequestAdmin | null = null;
  stats: SavingsChequeManagementStats | null = null;

  // Filters
  statusFilter: 'ALL' | SavingsChequeStatus = 'ALL';
  searchQuery: string = '';
  currentPage: number = 0;
  pageSize: number = 20;
  totalItems: number = 0;

  // UI States
  isLoading: boolean = false;
  isProcessing: boolean = false;
  showDetailsModal: boolean = false;
  showApprovalModal: boolean = false;
  showRejectionModal: boolean = false;

  // Approval/Rejection Form
  approvalRemarks: string = '';
  rejectionReason: string = '';

  // Payee Verification
  payeeAccountNumber: string = '';
  verificationResult: any = null;
  isVerifying: boolean = false;
  payeeVerified: boolean = false;

  // Admin Info
  adminName: string = 'Admin';

  // Status colors
  statusColors: { [key in SavingsChequeStatus]: string } = {
    'AWAITING_POSITIVE_PAY': '#f97316',
    'PENDING': '#f59e0b',
    'APPROVED': '#3b82f6',
    'COMPLETED': '#10b981',
    'REJECTED': '#ef4444',
    'CANCELLED': '#6b7280',
    'CLEARED': '#059669'
  };

  // Audit log
  auditLog: any[] = [];
  showAuditLog: boolean = false;

  // Signature Verification
  signatureDocUrl: SafeResourceUrl | null = null;
  signatureLoading: boolean = false;
  signatureInfo: any = null;
  signatureError: string = '';

  constructor(
    private savingsChequeService: SavingsChequeService,
    private alertService: AlertService,
    private http: HttpClient,
    private sanitizer: DomSanitizer
  ) {
    const adminData = sessionStorage.getItem('admin');
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        this.adminName = admin.username || admin.name || 'Admin';
      } catch (e) {
        console.error('Error parsing admin data:', e);
      }
    }
  }

  ngOnInit() {
    this.loadCheques();
    this.loadStats();
  }

  ngOnDestroy() {}

  loadCheques() {
    this.isLoading = true;
    this.savingsChequeService.getAdminCheques(
      this.statusFilter === 'ALL' ? undefined : this.statusFilter,
      this.searchQuery || undefined,
      this.currentPage,
      this.pageSize
    ).subscribe({
      next: (response: any) => {
        this.chequeRequests = response.data || response.items || [];
        this.totalItems = response.totalItems || response.totalCount || 0;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error loading savings cheques:', err);
        this.alertService.error('Error', 'Failed to load savings cheque requests');
        this.isLoading = false;
      }
    });
  }

  loadStats() {
    this.savingsChequeService.getChequeStats().subscribe({
      next: (stats: SavingsChequeManagementStats) => {
        this.stats = stats;
      },
      error: (err: any) => {
        console.error('Error loading stats:', err);
      }
    });
  }

  onFilterChange() {
    this.currentPage = 0;
    this.loadCheques();
  }

  onSearch() {
    this.currentPage = 0;
    this.loadCheques();
  }

  viewChequeDetails(cheque: SavingsChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.showDetailsModal = true;
    this.loadAuditLog(cheque.id!);
    this.loadSignatureDocument(cheque.accountNumber);
  }

  loadAuditLog(chequeRequestId: number) {
    this.savingsChequeService.getChequeAuditLog(chequeRequestId).subscribe({
      next: (logs: any[]) => {
        this.auditLog = logs || [];
      },
      error: (err: any) => {
        console.error('Error loading audit log:', err);
        this.auditLog = [];
      }
    });
  }

  showApprovalDialog(cheque: SavingsChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.approvalRemarks = '';
    this.payeeAccountNumber = '';
    this.verificationResult = null;
    this.isVerifying = false;
    this.payeeVerified = false;
    this.showApprovalModal = true;
  }

  showRejectionDialog(cheque: SavingsChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.rejectionReason = '';
    this.showRejectionModal = true;
  }

  verifyPayeeAccount() {
    if (!this.payeeAccountNumber || this.payeeAccountNumber.trim() === '' || !this.selectedCheque) return;

    this.isVerifying = true;
    this.verificationResult = null;

    this.savingsChequeService.verifyPayeeAccount(
      this.payeeAccountNumber.trim(),
      this.selectedCheque.payeeName
    ).subscribe({
      next: (result: any) => {
        this.isVerifying = false;
        this.verificationResult = result;
        this.payeeVerified = result.verified === true;
      },
      error: (err: any) => {
        this.isVerifying = false;
        this.verificationResult = {
          verified: false,
          message: err.error?.message || 'Failed to verify payee account'
        };
        this.payeeVerified = false;
      }
    });
  }

  resetVerification() {
    this.payeeAccountNumber = '';
    this.verificationResult = null;
    this.isVerifying = false;
    this.payeeVerified = false;
  }

  approveCheque() {
    if (!this.selectedCheque?.id) {
      this.alertService.error('Error', 'Cheque not selected');
      return;
    }

    if (this.selectedCheque.chequePurpose === 'GOLD_LOAN_PREPAYMENT') {
      this.alertService.error('Restricted Cheque', 'Use this cheque only from Admin Funds Transfer for its linked Gold Loan prepayment.');
      return;
    }

    if (!this.payeeVerified) {
      this.alertService.error('Error', 'Please verify payee account before approving');
      return;
    }

    if (!confirm(`Approve savings cheque ${this.selectedCheque.chequeNumber} for ${this.formatAmount(this.selectedCheque.amount)}?\n\nFunds will be debited from ${this.selectedCheque.accountNumber} and credited to ${this.payeeAccountNumber}.`)) {
      return;
    }

    this.isProcessing = true;
    this.savingsChequeService.approveCheque(
      this.selectedCheque.id,
      this.approvalRemarks || undefined,
      this.payeeAccountNumber
    ).subscribe({
      next: (response: SavingsChequeApprovalResponse) => {
        this.isProcessing = false;

        if (response.success) {
          this.alertService.success(
            'Success',
            `Business cheque ${response.chequeRequest.chequeNumber} approved and fund transfer completed successfully`
          );
          this.showApprovalModal = false;
          this.selectedCheque = null;
          this.loadCheques();
          this.loadStats();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve cheque');
        }
      },
      error: (err: any) => {
        this.isProcessing = false;
        console.error('Error approving savings cheque:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve cheque');
      }
    });
  }

  rejectCheque() {
    if (!this.selectedCheque?.id) {
      this.alertService.error('Error', 'Cheque not selected');
      return;
    }

    if (!this.rejectionReason || this.rejectionReason.trim() === '') {
      this.alertService.error('Validation Error', 'Please enter rejection reason');
      return;
    }

    if (!confirm(`Reject savings cheque ${this.selectedCheque.chequeNumber}?`)) {
      return;
    }

    this.isProcessing = true;
    this.savingsChequeService.rejectCheque(this.selectedCheque.id, this.rejectionReason).subscribe({
      next: (response: SavingsChequeApprovalResponse) => {
        this.isProcessing = false;

        if (response.success) {
          this.alertService.success(
            'Success',
            `Business cheque ${response.chequeRequest.chequeNumber} rejected`
          );
          this.showRejectionModal = false;
          this.selectedCheque = null;
          this.loadCheques();
          this.loadStats();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject cheque');
        }
      },
      error: (err: any) => {
        this.isProcessing = false;
        console.error('Error rejecting savings cheque:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject cheque');
      }
    });
  }

  markPickedUp(cheque: SavingsChequeRequestAdmin) {
    if (!cheque.id) return;

    if (!confirm(`Mark savings cheque ${cheque.chequeNumber} as picked up by user?`)) {
      return;
    }

    this.savingsChequeService.markChequePickedUp(cheque.id).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Business cheque marked as picked up');
          this.loadCheques();
          this.loadStats();
        } else {
          this.alertService.error('Error', response.message || 'Failed to update cheque status');
        }
      },
      error: (err: any) => {
        console.error('Error marking savings cheque as picked up:', err);
        this.alertService.error('Error', 'Failed to update cheque status');
      }
    });
  }

  clearCheque(cheque: SavingsChequeRequestAdmin) {
    if (!cheque.id) return;

    const clearedDate = prompt('Enter cleared date (YYYY-MM-DD):', this.getTodayDate());
    if (!clearedDate) return;

    if (!/^\d{4}-\d{2}-\d{2}$/.test(clearedDate)) {
      this.alertService.error('Validation Error', 'Invalid date format. Please use YYYY-MM-DD');
      return;
    }

    this.savingsChequeService.clearCheque(cheque.id, clearedDate).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Business cheque marked as cleared');
          this.loadCheques();
          this.loadStats();
        } else {
          this.alertService.error('Error', response.message || 'Failed to clear cheque');
        }
      },
      error: (err: any) => {
        console.error('Error clearing savings cheque:', err);
        this.alertService.error('Error', 'Failed to clear cheque');
      }
    });
  }

  getTotalPages(): number {
    return Math.ceil(this.totalItems / this.pageSize);
  }

  nextPage() {
    if (this.currentPage < this.getTotalPages() - 1) {
      this.currentPage++;
      this.loadCheques();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadCheques();
    }
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.selectedCheque = null;
    this.auditLog = [];
    this.clearSignatureDoc();
  }

  closeApprovalModal() {
    this.showApprovalModal = false;
    this.selectedCheque = null;
    this.approvalRemarks = '';
    this.payeeAccountNumber = '';
    this.verificationResult = null;
    this.isVerifying = false;
    this.payeeVerified = false;
  }

  closeRejectionModal() {
    this.showRejectionModal = false;
    this.selectedCheque = null;
    this.rejectionReason = '';
  }

  getStatusColor(status: SavingsChequeStatus): string {
    return this.statusColors[status] || '#6b7280';
  }

  getStatusText(status: SavingsChequeStatus): string {
    const statusMap: { [key in SavingsChequeStatus]: string } = {
      'AWAITING_POSITIVE_PAY': 'Awaiting Positive Pay',
      'PENDING': 'Pending',
      'APPROVED': 'Approved',
      'COMPLETED': 'Completed',
      'REJECTED': 'Rejected',
      'CANCELLED': 'Cancelled',
      'CLEARED': 'Cleared'
    };
    return statusMap[status] || status;
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  formatDateTime(dateString: string | undefined): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatAmount(amount: number): string {
    return amount.toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  getTodayDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  toggleAuditLog() {
    this.showAuditLog = !this.showAuditLog;
  }

  getStatColor(status: string): string {
    const colors: { [key: string]: string } = {
      'pending': '#f59e0b',
      'approved': '#3b82f6',
      'completed': '#10b981',
      'rejected': '#ef4444'
    };
    return colors[status.toLowerCase()] || '#6b7280';
  }

  canRevertItem(item: any): boolean {
    if (!item) return false;
    const status = (item.status || '').toUpperCase();
    if (status !== 'APPROVED' && status !== 'COMPLETED' && status !== 'CLEARED') return false;
    const actionDate = item.approvedAt || item.updatedAt;
    if (!actionDate) return true;
    const actionTime = new Date(actionDate).getTime();
    const now = new Date().getTime();
    const diffHours = (now - actionTime) / (1000 * 60 * 60);
    return diffHours <= 24;
  }

  revertChequeDraw(item: any) {
    if (!item || !item.id) return;
    if (!confirm(`Are you sure you want to revert savings cheque draw #${item.chequeNumber}? The amount ₹${item.amount || 0} will be credited back in real-time.`)) {
      return;
    }
    this.http.post(`${environment.apiBaseUrl}/api/savings-cheques/draw/admin/${item.id}/revert`, {
      adminEmail: 'Admin',
      reason: 'Admin mistake - Reverted within 24h'
    }).subscribe({
      next: (res: any) => {
        this.alertService.success('Cheque Reverted', res.message || 'Business cheque draw reverted and amount credited back successfully.');
        this.loadCheques();
      },
      error: (err: any) => {
        this.alertService.error('Revert Failed', err.error?.message || err.error?.error || 'Failed to revert savings cheque draw');
      }
    });
  }

  // ==================== Signature Verification ====================

  loadSignatureDocument(accountNumber: string | undefined) {
    if (!accountNumber) {
      this.signatureError = 'No account number available.';
      return;
    }
    this.signatureLoading = true;
    this.signatureError = '';
    this.signatureDocUrl = null;
    this.signatureInfo = null;

    this.http.get(`${environment.apiBaseUrl}/api/admin-account-applications/signed-document-info/${accountNumber.trim()}`).subscribe({
      next: (info: any) => {
        if (info && info.found) {
          this.signatureInfo = info;
        } else {
          this.signatureInfo = {
            fullName: 'Account Holder',
            accountType: 'Savings',
            applicationNumber: accountNumber
          };
        }
        this.loadSignatureDocumentContent(accountNumber.trim());
      },
      error: () => {
        this.signatureInfo = {
          fullName: 'Account Holder',
          accountType: 'Savings',
          applicationNumber: accountNumber
        };
        this.loadSignatureDocumentContent(accountNumber.trim());
      }
    });
  }

  private loadSignatureDocumentContent(accountNumber: string) {
    const viewUrl = `${environment.apiBaseUrl}/api/admin-account-applications/view-signed-document/${encodeURIComponent(accountNumber)}`;
    this.http.get(viewUrl, { responseType: 'blob' }).subscribe({
      next: (documentBlob: Blob) => {
        const documentUrl = URL.createObjectURL(documentBlob);
        this.signatureDocUrl = this.sanitizer.bypassSecurityTrustResourceUrl(documentUrl);
        this.signatureLoading = false;
      },
      error: () => {
        this.signatureError = 'Unable to load the account signature document.';
        this.signatureLoading = false;
      }
    });
  }

  clearSignatureDoc() {
    if (this.signatureDocUrl) {
      const url = (this.signatureDocUrl as any).changingThisBreaksApplicationSecurity || '';
      if (url.startsWith('blob:')) {
        URL.revokeObjectURL(url);
      }
    }
    this.signatureDocUrl = null;
    this.signatureInfo = null;
    this.signatureError = '';
  }
}
