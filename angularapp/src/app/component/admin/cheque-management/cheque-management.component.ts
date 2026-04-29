import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ChequeService } from '../../../service/cheque.service';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';
import {
  ChequeRequestAdmin,
  ChequeManagementStats,
  ChequeApprovalResponse,
  ChequeStatus
} from '../../../model/cheque/cheque.model';

@Component({
  selector: 'app-cheque-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cheque-management.component.html',
  styleUrls: ['./cheque-management.component.css']
})
export class ChequeManagementComponent implements OnInit, OnDestroy {
  // Data
  chequeRequests: ChequeRequestAdmin[] = [];
  selectedCheque: ChequeRequestAdmin | null = null;
  stats: ChequeManagementStats | null = null;

  // Filters
  statusFilter: 'ALL' | ChequeStatus = 'ALL';
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
  statusColors: { [key in ChequeStatus]: string } = {
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
    private chequeService: ChequeService,
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

  ngOnDestroy() {
    // Cleanup if needed
  }

  /**
   * Load all cheque requests
   */
  loadCheques() {
    this.isLoading = true;
    this.chequeService.getAdminCheques(
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
        console.error('Error loading cheques:', err);
        this.alertService.error('Error', 'Failed to load cheque requests');
        this.isLoading = false;
      }
    });
  }

  /**
   * Load cheque management statistics
   */
  loadStats() {
    this.chequeService.getChequeStats().subscribe({
      next: (stats: ChequeManagementStats) => {
        this.stats = stats;
      },
      error: (err: any) => {
        console.error('Error loading stats:', err);
      }
    });
  }

  /**
   * Handle filter changes
   */
  onFilterChange() {
    this.currentPage = 0;
    this.loadCheques();
  }

  /**
   * Search cheques
   */
  onSearch() {
    this.currentPage = 0;
    this.loadCheques();
  }

  /**
   * View cheque details
   */
  viewChequeDetails(cheque: ChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.showDetailsModal = true;
    this.loadAuditLog(cheque.id!);
    this.loadSignatureDocument(cheque.accountNumber);
  }

  /**
   * Load audit log for cheque
   */
  loadAuditLog(chequeRequestId: number) {
    this.chequeService.getChequeAuditLog(chequeRequestId).subscribe({
      next: (logs: any[]) => {
        this.auditLog = logs || [];
      },
      error: (err: any) => {
        console.error('Error loading audit log:', err);
        this.auditLog = [];
      }
    });
  }

  /**
   * Show approval modal
   */
  showApprovalDialog(cheque: ChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.approvalRemarks = '';
    this.payeeAccountNumber = '';
    this.verificationResult = null;
    this.isVerifying = false;
    this.payeeVerified = false;
    this.showApprovalModal = true;
  }

  /**
   * Show rejection modal
   */
  showRejectionDialog(cheque: ChequeRequestAdmin) {
    this.selectedCheque = cheque;
    this.rejectionReason = '';
    this.showRejectionModal = true;
  }

  /**
   * Verify payee account before approval
   */
  verifyPayeeAccount() {
    if (!this.payeeAccountNumber || this.payeeAccountNumber.trim() === '' || !this.selectedCheque) return;

    this.isVerifying = true;
    this.verificationResult = null;

    this.chequeService.verifyPayeeAccount(
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

  /**
   * Reset payee verification to try again
   */
  resetVerification() {
    this.payeeAccountNumber = '';
    this.verificationResult = null;
    this.isVerifying = false;
    this.payeeVerified = false;
  }

  /**
   * Approve cheque request
   */
  approveCheque() {
    if (!this.selectedCheque?.id) {
      this.alertService.error('Error', 'Cheque not selected');
      return;
    }

    if (!this.payeeVerified) {
      this.alertService.error('Error', 'Please verify payee account before approving');
      return;
    }

    if (!confirm(`Approve cheque ${this.selectedCheque.chequeNumber} for ${this.formatAmount(this.selectedCheque.amount)}?\n\nFunds will be debited from ${this.selectedCheque.accountNumber} and credited to ${this.payeeAccountNumber}.`)) {
      return;
    }

    this.isProcessing = true;
    this.chequeService.approveCheque(
      this.selectedCheque.id,
      this.approvalRemarks || undefined,
      this.payeeAccountNumber
    ).subscribe({
      next: (response: ChequeApprovalResponse) => {
        this.isProcessing = false;
        
        if (response.success) {
          this.alertService.success(
            'Success',
            `Cheque ${response.chequeRequest.chequeNumber} approved and fund transfer completed successfully`
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
        console.error('Error approving cheque:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve cheque');
      }
    });
  }

  /**
   * Reject cheque request
   */
  rejectCheque() {
    if (!this.selectedCheque?.id) {
      this.alertService.error('Error', 'Cheque not selected');
      return;
    }

    if (!this.rejectionReason || this.rejectionReason.trim() === '') {
      this.alertService.error('Validation Error', 'Please enter rejection reason');
      return;
    }

    if (!confirm(`Reject cheque ${this.selectedCheque.chequeNumber}?`)) {
      return;
    }

    this.isProcessing = true;
    this.chequeService.rejectCheque(this.selectedCheque.id, this.rejectionReason).subscribe({
      next: (response: ChequeApprovalResponse) => {
        this.isProcessing = false;
        
        if (response.success) {
          this.alertService.success(
            'Success',
            `Cheque ${response.chequeRequest.chequeNumber} rejected`
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
        console.error('Error rejecting cheque:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject cheque');
      }
    });
  }

  /**
   * Mark cheque as picked up
   */
  markPickedUp(cheque: ChequeRequestAdmin) {
    if (!cheque.id) return;

    if (!confirm(`Mark cheque ${cheque.chequeNumber} as picked up by user?`)) {
      return;
    }

    this.chequeService.markChequePickedUp(cheque.id).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Cheque marked as picked up');
          this.loadCheques();
          this.loadStats();
        } else {
          this.alertService.error('Error', response.message || 'Failed to update cheque status');
        }
      },
      error: (err: any) => {
        console.error('Error marking cheque as picked up:', err);
        this.alertService.error('Error', 'Failed to update cheque status');
      }
    });
  }

  /**
   * Clear cheque (mark as processed)
   */
  clearCheque(cheque: ChequeRequestAdmin) {
    if (!cheque.id) return;

    const clearedDate = prompt('Enter cleared date (YYYY-MM-DD):', this.getTodayDate());
    if (!clearedDate) return;

    // Validate date format
    if (!/^\d{4}-\d{2}-\d{2}$/.test(clearedDate)) {
      this.alertService.error('Validation Error', 'Invalid date format. Please use YYYY-MM-DD');
      return;
    }

    this.chequeService.clearCheque(cheque.id, clearedDate).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Cheque marked as cleared');
          this.loadCheques();
          this.loadStats();
        } else {
          this.alertService.error('Error', response.message || 'Failed to clear cheque');
        }
      },
      error: (err: any) => {
        console.error('Error clearing cheque:', err);
        this.alertService.error('Error', 'Failed to clear cheque');
      }
    });
  }

  /**
   * Get total pages
   */
  getTotalPages(): number {
    return Math.ceil(this.totalItems / this.pageSize);
  }

  /**
   * Go to next page
   */
  nextPage() {
    if (this.currentPage < this.getTotalPages() - 1) {
      this.currentPage++;
      this.loadCheques();
    }
  }

  /**
   * Go to previous page
   */
  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadCheques();
    }
  }

  /**
   * Close modals
   */
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

  /**
   * Get status color
   */
  getStatusColor(status: ChequeStatus): string {
    return this.statusColors[status] || '#6b7280';
  }

  /**
   * Get status text
   */
  getStatusText(status: ChequeStatus): string {
    const statusMap: { [key in ChequeStatus]: string } = {
      'PENDING': 'Pending',
      'APPROVED': 'Approved',
      'COMPLETED': 'Completed',
      'REJECTED': 'Rejected',
      'CANCELLED': 'Cancelled',
      'CLEARED': 'Cleared'
    };
    return statusMap[status] || status;
  }

  /**
   * Format date
   */
  formatDate(dateString: string | undefined): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  /**
   * Format datetime
   */
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

  /**
   * Format amount
   */
  formatAmount(amount: number): string {
    return amount.toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  /**
   * Get today's date in YYYY-MM-DD format
   */
  getTodayDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Toggle audit log view
   */
  toggleAuditLog() {
    this.showAuditLog = !this.showAuditLog;
  }

  /**
   * Get stat card color based on status
   */
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
