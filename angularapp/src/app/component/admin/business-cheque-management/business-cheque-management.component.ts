import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { BusinessChequeService } from '../../../service/business-cheque.service';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';
import {
  BusinessChequeRequestAdmin,
  BusinessChequeManagementStats,
  BusinessChequeApprovalResponse,
  BusinessChequeStatus
} from '../../../model/cheque/business-cheque.model';

@Component({
  selector: 'app-business-cheque-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './business-cheque-management.component.html',
  styleUrls: ['./business-cheque-management.component.css']
})
export class BusinessChequeManagementComponent implements OnInit, OnDestroy {
  // Data
  chequeRequests: BusinessChequeRequestAdmin[] = [];
  selectedCheque: BusinessChequeRequestAdmin | null = null;
  stats: BusinessChequeManagementStats | null = null;

  // Filters
  statusFilter: 'ALL' | BusinessChequeStatus = 'ALL';
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
  statusColors: { [key in BusinessChequeStatus]: string } = {
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
    private businessChequeService: BusinessChequeService,
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
    this.businessChequeService.getAdminCheques(
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
        console.error('Error loading business cheques:', err);
        this.alertService.error('Error', 'Failed to load business cheque requests');
        this.isLoading = false;
      }
    });
  }

  loadStats() {
    this.businessChequeService.getChequeStats().subscribe({
      next: (stats: BusinessChequeManagementStats) => {
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

  viewChequeDetails(cheque: BusinessChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.showDetailsModal = true;
    this.loadAuditLog(cheque.id!);
    this.loadSignatureDocument(cheque.accountNumber);
  }

  loadAuditLog(chequeRequestId: number) {
    this.businessChequeService.getChequeAuditLog(chequeRequestId).subscribe({
      next: (logs: any[]) => {
        this.auditLog = logs || [];
      },
      error: (err: any) => {
        console.error('Error loading audit log:', err);
        this.auditLog = [];
      }
    });
  }

  showApprovalDialog(cheque: BusinessChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.approvalRemarks = '';
    this.payeeAccountNumber = '';
    this.verificationResult = null;
    this.isVerifying = false;
    this.payeeVerified = false;
    this.showApprovalModal = true;
  }

  showRejectionDialog(cheque: BusinessChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.rejectionReason = '';
    this.showRejectionModal = true;
  }

  verifyPayeeAccount() {
    if (!this.payeeAccountNumber || this.payeeAccountNumber.trim() === '' || !this.selectedCheque) return;

    this.isVerifying = true;
    this.verificationResult = null;

    this.businessChequeService.verifyPayeeAccount(
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

    if (!this.payeeVerified) {
      this.alertService.error('Error', 'Please verify payee account before approving');
      return;
    }

    if (!confirm(`Approve business cheque ${this.selectedCheque.chequeNumber} for ${this.formatAmount(this.selectedCheque.amount)}?\n\nFunds will be debited from ${this.selectedCheque.accountNumber} and credited to ${this.payeeAccountNumber}.`)) {
      return;
    }

    this.isProcessing = true;
    this.businessChequeService.approveCheque(
      this.selectedCheque.id,
      this.approvalRemarks || undefined,
      this.payeeAccountNumber
    ).subscribe({
      next: (response: BusinessChequeApprovalResponse) => {
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
        console.error('Error approving business cheque:', err);
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

    if (!confirm(`Reject business cheque ${this.selectedCheque.chequeNumber}?`)) {
      return;
    }

    this.isProcessing = true;
    this.businessChequeService.rejectCheque(this.selectedCheque.id, this.rejectionReason).subscribe({
      next: (response: BusinessChequeApprovalResponse) => {
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
        console.error('Error rejecting business cheque:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject cheque');
      }
    });
  }

  markPickedUp(cheque: BusinessChequeRequestAdmin) {
    if (!cheque.id) return;

    if (!confirm(`Mark business cheque ${cheque.chequeNumber} as picked up by user?`)) {
      return;
    }

    this.businessChequeService.markChequePickedUp(cheque.id).subscribe({
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
        console.error('Error marking business cheque as picked up:', err);
        this.alertService.error('Error', 'Failed to update cheque status');
      }
    });
  }

  clearCheque(cheque: BusinessChequeRequestAdmin) {
    if (!cheque.id) return;

    const clearedDate = prompt('Enter cleared date (YYYY-MM-DD):', this.getTodayDate());
    if (!clearedDate) return;

    if (!/^\d{4}-\d{2}-\d{2}$/.test(clearedDate)) {
      this.alertService.error('Validation Error', 'Invalid date format. Please use YYYY-MM-DD');
      return;
    }

    this.businessChequeService.clearCheque(cheque.id, clearedDate).subscribe({
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
        console.error('Error clearing business cheque:', err);
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

  getStatusColor(status: BusinessChequeStatus): string {
    return this.statusColors[status] || '#6b7280';
  }

  getStatusText(status: BusinessChequeStatus): string {
    const statusMap: { [key in BusinessChequeStatus]: string } = {
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

    this.http.get(`${environment.apiBaseUrl}/api/admin-account-applications/signed-document-info/${accountNumber}`).subscribe({
      next: (info: any) => {
        if (info.found) {
          this.signatureInfo = info;
          this.http.get(`${environment.apiBaseUrl}/api/admin-account-applications/view-signed-document/${accountNumber}`, {
            responseType: 'blob'
          }).subscribe({
            next: (blob: Blob) => {
              const url = URL.createObjectURL(blob);
              this.signatureDocUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
              this.signatureLoading = false;
            },
            error: () => {
              this.signatureError = 'Failed to load the signed document file.';
              this.signatureLoading = false;
            }
          });
        } else {
          this.signatureError = info.message || 'No signed document available for this account.';
          this.signatureLoading = false;
        }
      },
      error: () => {
        this.signatureError = 'Failed to fetch signature document information.';
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
