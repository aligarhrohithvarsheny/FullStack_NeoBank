import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { ChequeService } from '../../../service/cheque';
import { Cheque as ChequeModel, CreateChequeRequest, CancelChequeRequest, RequestChequeDraw } from '../../../model/cheque/cheque-module';
import { environment } from '../../../../environment/environment';

const OTP_VALID_SECONDS = 120;

@Component({
  selector: 'app-cheque',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cheque.html',
  styleUrls: ['./cheque.css']
})

export class ChequeComponent implements OnInit, OnDestroy {
  // User data
  userAccountNumber: string = '';
  userName: string = '';
  currentBalance: number = 0;

  // Cheque leaves creation
  numberOfLeaves: number = 1;
  chequeAmount: number = 0; // Amount for each cheque
  creatingCheques: boolean = false;
  createError: string = '';

  // Cheque list
  cheques: ChequeModel[] = [];
  activeCheques: ChequeModel[] = [];
  cancelledCheques: ChequeModel[] = [];
  loading: boolean = false;
  error: string = '';

  // Statistics
  statistics: any = {
    totalCheques: 0,
    activeCheques: 0,
    cancelledCheques: 0
  };

  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  // Filter
  filterStatus: string = 'ALL'; // ALL, ACTIVE, CANCELLED

  // Cancel cheque
  selectedCheque: ChequeModel | null = null;
  cancelReason: string = '';
  cancelling: boolean = false;

  // Request cheque draw
  requesting: boolean = false;

  // Draw request modal + OTP
  selectedChequeForDraw: ChequeModel | null = null;
  chequeDrawOtpSent: boolean = false;
  chequeDrawOtpInput: string = '';
  chequeDrawOtpError: string = '';
  chequeDrawOtpTimer: number = 0;
  chequeDrawOtpTimerRef: any = null;
  isSendingChequeDrawOtp: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService,
    private chequeService: ChequeService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserProfile();
      this.loadCheques();
      this.loadStatistics();
    }
  }

  ngOnDestroy() {
    this.stopChequeDrawOtpTimer();
  }

  openDrawModal(cheque: ChequeModel) {
    if (!this.canRequestCheque(cheque)) return;
    this.selectedChequeForDraw = cheque;
    this.chequeDrawOtpSent = false;
    this.chequeDrawOtpInput = '';
    this.chequeDrawOtpError = '';
    this.chequeDrawOtpTimer = 0;
    this.stopChequeDrawOtpTimer();
  }

  closeDrawModal() {
    this.selectedChequeForDraw = null;
    this.chequeDrawOtpSent = false;
    this.chequeDrawOtpInput = '';
    this.chequeDrawOtpError = '';
    this.stopChequeDrawOtpTimer();
  }

  sendChequeDrawOtp() {
    if (!this.selectedChequeForDraw?.id) return;
    this.chequeDrawOtpError = '';
    this.isSendingChequeDrawOtp = true;
    this.chequeService.requestChequeDrawOtp(this.selectedChequeForDraw.id).subscribe({
      next: (res: any) => {
        this.isSendingChequeDrawOtp = false;
        if (res?.success !== false) {
          this.chequeDrawOtpSent = true;
          this.startChequeDrawOtpTimer();
          this.alertService.success('OTP Sent', 'OTP sent to your registered email (reason: Cheque Withdrawal / Draw Request). Valid for 2 minutes.');
        } else {
          this.chequeDrawOtpError = res?.message || 'Failed to send OTP';
        }
      },
      error: (err: any) => {
        this.isSendingChequeDrawOtp = false;
        this.chequeDrawOtpError = err.error?.error || err.error?.message || 'Failed to send OTP. Please try again.';
      }
    });
  }

  startChequeDrawOtpTimer() {
    this.stopChequeDrawOtpTimer();
    this.chequeDrawOtpTimer = OTP_VALID_SECONDS;
    this.chequeDrawOtpTimerRef = setInterval(() => {
      this.chequeDrawOtpTimer--;
      if (this.chequeDrawOtpTimer <= 0) this.stopChequeDrawOtpTimer();
    }, 1000);
  }

  stopChequeDrawOtpTimer() {
    if (this.chequeDrawOtpTimerRef) {
      clearInterval(this.chequeDrawOtpTimerRef);
      this.chequeDrawOtpTimerRef = null;
    }
  }

  formatChequeDrawOtpTimer(): string {
    if (this.chequeDrawOtpTimer <= 0) return '0:00';
    const m = Math.floor(this.chequeDrawOtpTimer / 60);
    const s = this.chequeDrawOtpTimer % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  requestChequeDrawWithOtp() {
    const c = this.selectedChequeForDraw;
    if (!c?.id || !this.chequeDrawOtpSent) {
      this.alertService.error('OTP Required', 'Please send OTP first.');
      return;
    }
    const otp = (this.chequeDrawOtpInput || '').trim();
    if (!/^\d{6}$/.test(otp)) {
      this.chequeDrawOtpError = 'Enter a valid 6-digit OTP';
      return;
    }
    if (this.chequeDrawOtpTimer <= 0) {
      this.chequeDrawOtpError = 'OTP expired. Please send a new OTP.';
      return;
    }
    this.requesting = true;
    this.chequeDrawOtpError = '';
    const request: RequestChequeDraw = { requestedBy: this.userName, otp };
    this.chequeService.requestChequeDraw(c.id, request).subscribe({
      next: (response: any) => {
        this.alertService.success('Request Submitted', response.message || 'Cheque draw request submitted successfully. Waiting for admin approval.');
        this.requesting = false;
        this.closeDrawModal();
        this.loadCheques();
        this.loadStatistics();
        this.loadCurrentBalance();
      },
      error: (err: any) => {
        this.requesting = false;
        this.chequeDrawOtpError = err.error?.error || err.error?.message || 'Failed to submit. Invalid or expired OTP.';
      }
    });
  }
  
  loadUserProfile() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.userAccountNumber = user.accountNumber;
      this.userName = user.name || user.username;
    } else {
      this.alertService.error('Login Required', 'Please login to continue');
      this.router.navigate(['/website/user']);
    }
  }

  loadCurrentBalance() {
    if (!this.userAccountNumber) return;

    this.http.get(`${environment.apiBaseUrl}/api/accounts/balance/${this.userAccountNumber}`).subscribe({
      next: (balanceData: any) => {
        this.currentBalance = balanceData.balance || 0;
      },
      error: (err: any) => {
        console.error('Error loading balance:', err);
      }
    });
  }

  loadCheques() {
    if (!this.userAccountNumber) return;

    this.loading = true;
    this.error = '';

    this.chequeService.getChequesByAccountNumberPaged(this.userAccountNumber, this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        this.cheques = response.content || response;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || response.length || 0;
        this.currentPage = response.number || 0;

        // Filter cheques by status
        this.activeCheques = this.cheques.filter(c => c.status === 'ACTIVE');
        this.cancelledCheques = this.cheques.filter(c => c.status === 'CANCELLED');

        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading cheques:', err);
        this.error = 'Failed to load cheques. Please try again.';
        this.loading = false;
      }
    });
  }

  loadStatistics() {
    if (!this.userAccountNumber) return;

    this.chequeService.getChequeStatistics(this.userAccountNumber).subscribe({
      next: (stats: any) => {
        this.statistics = stats;
      },
      error: (err: any) => {
        console.error('Error loading statistics:', err);
      }
    });
  }

  createChequeLeaves() {
    if (!this.userAccountNumber) {
      this.alertService.error('Login Required', 'Please login to continue');
      return;
    }

    if (this.numberOfLeaves <= 0 || this.numberOfLeaves > 100) {
      this.alertService.error('Validation Error', 'Number of leaves must be between 1 and 100');
      return;
    }

    this.creatingCheques = true;
    this.createError = '';

    const request: CreateChequeRequest = {
      accountNumber: this.userAccountNumber,
      numberOfLeaves: this.numberOfLeaves,
      amount: this.chequeAmount > 0 ? this.chequeAmount : undefined
    };

    this.chequeService.createChequeLeaves(request).subscribe({
      next: (response: any) => {
        this.alertService.success('Cheque Leaves Created', `Successfully created ${response.count || this.numberOfLeaves} cheque leaves`);
        this.creatingCheques = false;
        this.numberOfLeaves = 1;
        this.chequeAmount = 0;
        this.loadCheques();
        this.loadStatistics();
      },
      error: (err: any) => {
        console.error('Error creating cheque leaves:', err);
        this.createError = err.error?.error || 'Failed to create cheque leaves. Please try again.';
        this.creatingCheques = false;
        this.alertService.error('Creation Failed', this.createError);
      }
    });
  }

  downloadCheque(cheque: ChequeModel) {
    if (!cheque.id) return;

    this.chequeService.downloadCheque(cheque.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `cheque_${cheque.chequeNumber}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.alertService.success('Download Successful', 'Cheque downloaded successfully');
      },
      error: (err: any) => {
        console.error('Error downloading cheque:', err);
        this.alertService.error('Download Failed', 'Failed to download cheque. Please try again.');
      }
    });
  }

  viewCheque(cheque: ChequeModel) {
    if (!cheque.id) return;

    this.chequeService.viewCheque(cheque.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        this.alertService.success('View Successful', 'Cheque opened in new tab');
      },
      error: (err: any) => {
        console.error('Error viewing cheque:', err);
        this.alertService.error('View Failed', 'Failed to view cheque. Please try again.');
      }
    });
  }

  openCancelModal(cheque: ChequeModel) {
    if (cheque.status !== 'ACTIVE') {
      this.alertService.error('Cancellation Error', 'Only active cheques can be cancelled');
      return;
    }

    this.selectedCheque = cheque;
    this.cancelReason = '';
  }

  closeCancelModal() {
    this.selectedCheque = null;
    this.cancelReason = '';
  }

  cancelCheque() {
    if (!this.selectedCheque || !this.selectedCheque.id) return;

    if (!this.cancelReason || this.cancelReason.trim().length === 0) {
      this.alertService.error('Validation Error', 'Please provide a cancellation reason');
      return;
    }

    this.cancelling = true;

    const request: CancelChequeRequest = {
      cancelledBy: this.userName,
      reason: this.cancelReason
    };

    this.chequeService.cancelCheque(this.selectedCheque.id, request).subscribe({
      next: (response: any) => {
        this.alertService.success('Cheque Cancelled', 'Cheque cancelled successfully');
        this.cancelling = false;
        this.closeCancelModal();
        this.loadCheques();
        this.loadStatistics();
      },
      error: (err: any) => {
        console.error('Error cancelling cheque:', err);
        this.cancelling = false;
        this.alertService.error('Cancellation Failed', err.error?.error || 'Failed to cancel cheque. Please try again.');
      }
    });
  }

  filterCheques(status: string) {
    this.filterStatus = status;
    this.currentPage = 0;
    this.loadCheques();
  }

  getFilteredCheques(): ChequeModel[] {
    if (this.filterStatus === 'ALL') {
      return this.cheques;
    } else if (this.filterStatus === 'ACTIVE') {
      return this.activeCheques;
    } else {
      return this.cancelledCheques;
    }
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadCheques();
    }
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch (e) {
      return 'N/A';
    }
  }

  requestChequeDraw(cheque: ChequeModel) {
    if (!cheque.id) return;

    if (cheque.status !== 'ACTIVE') {
      this.alertService.error('Invalid Status', 'Only active cheques can be requested for drawing');
      return;
    }

    if (!cheque.amount || cheque.amount <= 0) {
      this.alertService.error('Invalid Amount', 'Please set an amount for the cheque before requesting');
      return;
    }

    if (cheque.requestStatus === 'PENDING') {
      this.alertService.error('Already Requested', 'This cheque is already pending approval');
      return;
    }

    if (cheque.requestStatus === 'APPROVED') {
      this.alertService.error('Already Approved', 'This cheque request has already been approved');
      return;
    }

    this.requesting = true;

    const request: RequestChequeDraw = {
      requestedBy: this.userName
    };

    this.chequeService.requestChequeDraw(cheque.id, request).subscribe({
      next: (response: any) => {
        this.alertService.success('Request Submitted', response.message || 'Cheque draw request submitted successfully. Waiting for admin approval.');
        this.requesting = false;
        this.loadCheques();
        this.loadStatistics();
        this.loadCurrentBalance(); // Refresh balance
      },
      error: (err: any) => {
        console.error('Error requesting cheque draw:', err);
        this.requesting = false;
        this.alertService.error('Request Failed', err.error?.error || 'Failed to submit cheque draw request. Please try again.');
      }
    });
  }

  canRequestCheque(cheque: ChequeModel): boolean {
    return cheque.status === 'ACTIVE' 
      && (cheque.requestStatus === 'NONE' || cheque.requestStatus === 'REJECTED' || !cheque.requestStatus)
      && cheque.amount != null 
      && cheque.amount > 0;
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

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }
}

