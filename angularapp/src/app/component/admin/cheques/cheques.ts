import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { ChequeService } from '../../../service/cheque';
import { Cheque as ChequeModel, ApproveChequeRequest, RejectChequeRequest } from '../../../model/cheque/cheque-module';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-admin-cheques',
  templateUrl: './cheques.html',
  styleUrls: ['./cheques.css'],
  encapsulation: ViewEncapsulation.None,
  imports: [FormsModule, CommonModule],
  standalone: true
})
export class AdminCheques implements OnInit {
  // Search
  searchChequeNumber: string = '';
  searchResults: ChequeModel[] = [];
  selectedCheque: ChequeModel | null = null;
  isSearching: boolean = false;
  searchError: string = '';

  // Statistics
  statistics: any = {
    totalCheques: 0,
    activeCheques: 0,
    drawnCheques: 0,
    bouncedCheques: 0,
    cancelledCheques: 0,
    pendingRequests: 0,
    approvedRequests: 0,
    rejectedRequests: 0
  };

  // Cheque list
  cheques: ChequeModel[] = [];
  loading: boolean = false;
  error: string = '';

  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  // Filter
  filterStatus: string = 'PENDING'; // PENDING, APPROVED, REJECTED, ALL
  viewMode: string = 'REQUESTS'; // REQUESTS, ALL

  // Approve/Reject cheque request
  selectedRequest: ChequeModel | null = null;
  approving: boolean = false;
  rejecting: boolean = false;
  rejectReason: string = '';
  
  // Reject modal
  showRejectModal: boolean = false;

  // Admin info
  adminName: string = 'Admin';

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService,
    private chequeService: ChequeService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadAdminInfo();
      this.loadStatistics();
      this.loadPendingRequests(); // Load pending requests by default
    }
  }

  loadAdminInfo() {
    const admin = sessionStorage.getItem('admin');
    if (admin) {
      const adminData = JSON.parse(admin);
      this.adminName = adminData.username || 'Admin';
    }
  }

  loadStatistics() {
    this.chequeService.getAllChequeStatistics().subscribe({
      next: (stats: any) => {
        this.statistics = stats;
      },
      error: (err: any) => {
        console.error('Error loading statistics:', err);
      }
    });
  }

  loadPendingRequests() {
    this.loading = true;
    this.error = '';
    this.viewMode = 'REQUESTS';
    this.filterStatus = 'PENDING';

    this.chequeService.getPendingRequests(this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading pending requests:', err);
        this.error = 'Failed to load pending requests. Please try again.';
        this.loading = false;
      }
    });
  }

  loadAllCheques() {
    this.loading = true;
    this.error = '';
    this.viewMode = 'ALL';

    this.chequeService.getAllCheques(this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading cheques:', err);
        this.error = 'Failed to load cheques. Please try again.';
        this.loading = false;
      }
    });
  }

  searchCheque() {
    if (!this.searchChequeNumber || this.searchChequeNumber.trim().length === 0) {
      this.alertService.error('Validation Error', 'Please enter a cheque number');
      return;
    }

    this.isSearching = true;
    this.searchError = '';
    this.searchResults = [];

    this.chequeService.searchChequeByNumber(this.searchChequeNumber, 0, 10).subscribe({
      next: (response: any) => {
        this.searchResults = response.content || response;
        if (this.searchResults.length === 0) {
          this.searchError = 'No cheques found with the given cheque number';
        }
        this.isSearching = false;
      },
      error: (err: any) => {
        console.error('Error searching cheque:', err);
        this.searchError = 'Failed to search cheque. Please try again.';
        this.isSearching = false;
      }
    });
  }

  approveChequeRequest(cheque: ChequeModel) {
    if (!cheque.id) return;

    if (cheque.requestStatus !== 'PENDING') {
      this.alertService.error('Invalid Request', 'Only pending requests can be approved');
      return;
    }

    this.approving = true;

    const request: ApproveChequeRequest = {
      approvedBy: this.adminName
    };

    this.chequeService.approveChequeRequest(cheque.id, request).subscribe({
      next: (response: any) => {
        this.alertService.success('Request Approved', response.message || 'Cheque request approved and drawn successfully. Amount debited from account.');
        this.approving = false;
        this.loadStatistics();
        this.loadPendingRequests();
        // Refresh search results if search was performed
        if (this.searchChequeNumber && this.searchChequeNumber.trim().length > 0) {
          this.searchCheque();
        }
      },
      error: (err: any) => {
        console.error('Error approving cheque request:', err);
        this.approving = false;
        this.alertService.error('Approval Failed', err.error?.error || 'Failed to approve cheque request. Please try again.');
      }
    });
  }

  openRejectModal(cheque: ChequeModel) {
    if (cheque.requestStatus !== 'PENDING') {
      this.alertService.error('Invalid Request', 'Only pending requests can be rejected');
      return;
    }

    this.selectedRequest = cheque;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  closeRejectModal() {
    this.selectedRequest = null;
    this.rejectReason = '';
    this.showRejectModal = false;
  }

  rejectChequeRequest() {
    if (!this.selectedRequest || !this.selectedRequest.id) return;

    if (!this.rejectReason || this.rejectReason.trim().length === 0) {
      this.alertService.error('Validation Error', 'Please provide a rejection reason');
      return;
    }

    this.rejecting = true;

    const request: RejectChequeRequest = {
      rejectedBy: this.adminName,
      reason: this.rejectReason
    };

    this.chequeService.rejectChequeRequest(this.selectedRequest.id, request).subscribe({
      next: (response: any) => {
        this.alertService.success('Request Rejected', response.message || 'Cheque request rejected successfully');
        this.rejecting = false;
        this.closeRejectModal();
        this.loadStatistics();
        this.loadPendingRequests();
        // Refresh search results if search was performed
        if (this.searchChequeNumber && this.searchChequeNumber.trim().length > 0) {
          this.searchCheque();
        }
      },
      error: (err: any) => {
        console.error('Error rejecting cheque request:', err);
        this.rejecting = false;
        this.alertService.error('Rejection Failed', err.error?.error || 'Failed to reject cheque request. Please try again.');
      }
    });
  }

  filterCheques(status: string) {
    this.filterStatus = status;
    this.currentPage = 0;

    if (status === 'PENDING') {
      this.viewMode = 'REQUESTS';
      this.loadPendingRequests();
    } else if (status === 'APPROVED') {
      this.viewMode = 'REQUESTS';
      this.loadApprovedRequests();
    } else if (status === 'REJECTED') {
      this.viewMode = 'REQUESTS';
      this.loadRejectedRequests();
    } else if (status === 'ALL') {
      this.viewMode = 'ALL';
      this.loadAllCheques();
    } else if (status === 'DRAWN') {
      this.viewMode = 'ALL';
      this.loadDrawnCheques();
    } else if (status === 'BOUNCED') {
      this.viewMode = 'ALL';
      this.loadBouncedCheques();
    } else {
      this.viewMode = 'ALL';
      this.loadChequesByStatus(status);
    }
  }

  loadApprovedRequests() {
    this.loading = true;
    this.chequeService.getApprovedRequests(this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading approved requests:', err);
        this.loading = false;
      }
    });
  }

  loadRejectedRequests() {
    this.loading = true;
    this.chequeService.getChequeRequestsByStatus('REJECTED', this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading rejected requests:', err);
        this.loading = false;
      }
    });
  }

  getChequeRequestsByStatus(status: string) {
    this.loading = true;
    this.chequeService.getChequeRequestsByStatus(status, this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading requests:', err);
        this.loading = false;
      }
    });
  }

  loadDrawnCheques() {
    this.loading = true;
    this.chequeService.getDrawnCheques(this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading drawn cheques:', err);
        this.loading = false;
      }
    });
  }

  loadBouncedCheques() {
    this.loading = true;
    this.chequeService.getBouncedCheques(this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading bounced cheques:', err);
        this.loading = false;
      }
    });
  }

  loadChequesByStatus(status: string) {
    this.loading = true;
    this.chequeService.getChequesByStatus(status, this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading cheques:', err);
        this.loading = false;
      }
    });
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      if (this.filterStatus === 'PENDING') {
        this.loadPendingRequests();
      } else if (this.filterStatus === 'APPROVED') {
        this.loadApprovedRequests();
      } else if (this.filterStatus === 'REJECTED') {
        this.loadRejectedRequests();
      } else if (this.filterStatus === 'ALL') {
        this.loadAllCheques();
      } else if (this.filterStatus === 'DRAWN') {
        this.loadDrawnCheques();
      } else if (this.filterStatus === 'BOUNCED') {
        this.loadBouncedCheques();
      } else {
        this.loadChequesByStatus(this.filterStatus);
      }
    }
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch (e) {
      return 'N/A';
    }
  }

  getRequestStatusClass(requestStatus: string | undefined): string {
    if (!requestStatus) return '';
    switch (requestStatus) {
      case 'PENDING':
        return 'request-pending';
      case 'APPROVED':
        return 'request-approved';
      case 'REJECTED':
        return 'request-rejected';
      default:
        return '';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'status-active';
      case 'DRAWN':
        return 'status-drawn';
      case 'BOUNCED':
        return 'status-bounced';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  }

  canApproveRequest(cheque: ChequeModel): boolean {
    return cheque.requestStatus === 'PENDING' && cheque.status === 'ACTIVE';
  }

  canRejectRequest(cheque: ChequeModel): boolean {
    return cheque.requestStatus === 'PENDING';
  }

  canDrawCheque(cheque: ChequeModel): boolean {
    // A cheque can be drawn if it's active and approved
    return cheque.status === 'ACTIVE' && cheque.requestStatus === 'APPROVED';
  }

  navigateToDashboard() {
    this.router.navigate(['/admin/dashboard']);
  }

  logout() {
    this.alertService.logoutSuccess();
    this.router.navigate(['/admin/login']);
  }
}

