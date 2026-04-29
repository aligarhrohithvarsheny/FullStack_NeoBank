import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { VideoKycService } from '../../../service/video-kyc.service';

@Component({
  selector: 'app-video-kyc-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './video-kyc-dashboard.html',
  styleUrls: ['./video-kyc-dashboard.css']
})
export class VideoKycDashboard implements OnInit, OnDestroy {
  @ViewChild('adminVideo') adminVideoRef!: ElementRef<HTMLVideoElement>;

  // Queue & Sessions
  kycQueue: any[] = [];
  allSessions: any[] = [];
  filteredSessions: any[] = [];
  selectedSession: any = null;
  auditLogs: any[] = [];
  stats: any = { total: 0, pending: 0, underReview: 0, approved: 0, rejected: 0 };

  // Filters
  statusFilter = 'all';
  searchQuery = '';
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;

  // UI State
  activeTab = 'queue';
  loading = false;
  error = '';
  successMsg = '';
  
  // Video Session
  videoActive = false;
  mediaStream: MediaStream | null = null;
  connectionStatus = 'waiting';

  // Admin action
  rejectionReason = '';
  showRejectModal = false;
  actionLoading = false;

  // Document viewer
  showDocViewer = false;
  docViewerUrl = '';
  docViewerTitle = '';

  // Slot Management
  allSlots: any[] = [];
  newSlotDate = '';
  newSlotTime = '';
  newSlotEndTime = '';
  newSlotMaxBookings = 5;
  slotLoading = false;
  slotError = '';
  slotSuccess = '';

  // Verification Lookup
  verificationNumber = '';
  verificationResult: any = null;
  verificationLoading = false;
  verificationError = '';

  private refreshInterval: any;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private router: Router,
    private http: HttpClient,
    private videoKycService: VideoKycService
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadQueue();

    // Auto-refresh queue every 15 seconds
    if (isPlatformBrowser(this.platformId)) {
      this.refreshInterval = setInterval(() => {
        if (this.activeTab === 'queue') {
          this.loadQueue();
        }
        this.loadStats();
      }, 15000);
    }
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    this.stopAdminVideo();
  }

  // Tab management
  switchTab(tab: string) {
    this.activeTab = tab;
    this.selectedSession = null;
    this.error = '';
    this.successMsg = '';
    if (tab === 'queue') this.loadQueue();
    else if (tab === 'all') this.loadAllSessions();
    else if (tab === 'stats') this.loadStats();
    else if (tab === 'slots') this.loadSlots();
    else if (tab === 'verify') { this.verificationResult = null; this.verificationError = ''; }
  }

  // Load data
  loadStats() {
    this.videoKycService.getStats().subscribe({
      next: (data: any) => this.stats = data,
      error: () => {}
    });
  }

  loadQueue() {
    this.loading = true;
    this.videoKycService.getKycQueue().subscribe({
      next: (data: any) => {
        this.kycQueue = data;
        this.totalPages = Math.ceil(this.kycQueue.length / this.pageSize) || 1;
        this.loading = false;
      },
      error: (err: any) => {
        this.error = 'Failed to load KYC queue.';
        this.loading = false;
      }
    });
  }

  loadAllSessions() {
    this.loading = true;
    const statusParam = this.statusFilter === 'all' ? undefined : this.statusFilter;
    
    if (this.searchQuery) {
      this.videoKycService.searchSessions(this.searchQuery).subscribe({
        next: (data: any) => {
          this.allSessions = data;
          this.filteredSessions = data;
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
    } else if (statusParam) {
      this.videoKycService.filterByStatus(statusParam, this.currentPage, this.pageSize).subscribe({
        next: (data: any) => {
          this.allSessions = data.content || data;
          this.filteredSessions = this.allSessions;
          this.totalPages = data.totalPages || 1;
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
    } else {
      this.videoKycService.getAllSessions(this.currentPage, this.pageSize).subscribe({
        next: (data: any) => {
          this.allSessions = data.content || data;
          this.filteredSessions = this.allSessions;
          this.totalPages = data.totalPages || 1;
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
    }
  }

  // Session selection
  selectSession(session: any) {
    this.selectedSession = session;
    this.error = '';
    this.successMsg = '';
    this.loadAuditLogs(session.id);
  }

  loadAuditLogs(sessionId: number) {
    this.videoKycService.getAuditLogs(sessionId).subscribe({
      next: (logs: any) => this.auditLogs = logs,
      error: () => this.auditLogs = []
    });
  }

  // Admin Video Join
  async joinVideoSession() {
    if (!this.selectedSession) return;

    const adminId = this.getAdminId();
    const adminName = this.getAdminName();

    this.actionLoading = true;
    
    try {
      if (isPlatformBrowser(this.platformId)) {
        this.mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      }

      this.videoKycService.adminJoinSession(this.selectedSession.id, Number(adminId), adminName).subscribe({
        next: (response: any) => {
          this.selectedSession = response;
          this.videoActive = true;
          this.connectionStatus = 'connected';
          this.actionLoading = false;

          setTimeout(() => {
            if (this.adminVideoRef?.nativeElement && this.mediaStream) {
              this.adminVideoRef.nativeElement.srcObject = this.mediaStream;
            }
          }, 100);
        },
        error: (err: any) => {
          this.error = err.error?.message || err.error?.error || 'Failed to join video session.';
          this.actionLoading = false;
          this.stopAdminVideo();
        }
      });
    } catch (err) {
      this.error = 'Camera/microphone access denied.';
      this.actionLoading = false;
    }
  }

  stopAdminVideo() {
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop());
      this.mediaStream = null;
    }
    this.videoActive = false;
    this.connectionStatus = 'waiting';
  }

  // Admin Actions
  approveKyc() {
    if (!this.selectedSession) return;
    
    const adminId = this.getAdminId();
    const adminName = this.getAdminName();

    this.actionLoading = true;
    this.videoKycService.approveKyc(this.selectedSession.id, Number(adminId), adminName).subscribe({
      next: (response: any) => {
        this.selectedSession = response;
        this.successMsg = `KYC Approved! Account Number: ${response.finalAccountNumber}`;
        this.actionLoading = false;
        this.stopAdminVideo();
        this.loadStats();
        this.loadAuditLogs(this.selectedSession.id);
      },
      error: (err: any) => {
        this.error = err.error?.message || err.error?.error || 'Failed to approve KYC.';
        this.actionLoading = false;
      }
    });
  }

  openRejectModal() {
    this.showRejectModal = true;
    this.rejectionReason = '';
  }

  cancelReject() {
    this.showRejectModal = false;
    this.rejectionReason = '';
  }

  confirmReject() {
    if (!this.selectedSession || !this.rejectionReason.trim()) return;
    
    const adminId = this.getAdminId();
    const adminName = this.getAdminName();

    this.actionLoading = true;
    this.videoKycService.rejectKyc(this.selectedSession.id, Number(adminId), adminName, this.rejectionReason).subscribe({
      next: (response: any) => {
        this.selectedSession = response;
        this.successMsg = 'KYC Rejected.';
        this.showRejectModal = false;
        this.actionLoading = false;
        this.stopAdminVideo();
        this.loadStats();
        this.loadAuditLogs(this.selectedSession.id);
      },
      error: (err: any) => {
        this.error = err.error?.message || err.error?.error || 'Failed to reject KYC.';
        this.actionLoading = false;
      }
    });
  }

  reopenSession() {
    if (!this.selectedSession) return;
    
    const adminId = this.getAdminId();
    const adminName = this.getAdminName();

    this.actionLoading = true;
    this.videoKycService.reopenSession(this.selectedSession.id, Number(adminId), adminName).subscribe({
      next: (response: any) => {
        this.selectedSession = response;
        this.successMsg = 'Session reopened for re-verification.';
        this.actionLoading = false;
        this.loadStats();
        this.loadAuditLogs(this.selectedSession.id);
      },
      error: (err: any) => {
        this.error = err.error?.message || err.error?.error || 'Failed to reopen session.';
        this.actionLoading = false;
      }
    });
  }

  // Document viewer
  viewDocument(type: 'aadhar' | 'pan' | 'face' | 'id') {
    if (!this.selectedSession) return;
    
    let url = '';
    let title = '';
    switch (type) {
      case 'aadhar':
        url = `${environment.apiBaseUrl}/api/video-kyc/document/aadhar/${this.selectedSession.id}`;
        title = 'Aadhaar Document';
        break;
      case 'pan':
        url = `${environment.apiBaseUrl}/api/video-kyc/document/pan/${this.selectedSession.id}`;
        title = 'PAN Document';
        break;
      case 'face':
        url = `${environment.apiBaseUrl}/api/video-kyc/snapshot/face/${this.selectedSession.id}`;
        title = 'Face Snapshot';
        break;
      case 'id':
        url = `${environment.apiBaseUrl}/api/video-kyc/snapshot/id-proof/${this.selectedSession.id}`;
        title = 'ID Proof Snapshot';
        break;
    }
    this.docViewerUrl = url;
    this.docViewerTitle = title;
    this.showDocViewer = true;
  }

  closeDocViewer() {
    this.showDocViewer = false;
    this.docViewerUrl = '';
  }

  // Pagination
  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      if (this.activeTab === 'queue') this.loadQueue();
      else this.loadAllSessions();
    }
  }

  prevPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      if (this.activeTab === 'queue') this.loadQueue();
      else this.loadAllSessions();
    }
  }

  // Search
  onSearch() {
    this.currentPage = 0;
    this.loadAllSessions();
  }

  onFilterChange() {
    this.currentPage = 0;
    this.loadAllSessions();
  }

  // Helpers
  getStatusClass(status: string): string {
    switch (status) {
      case 'Approved': return 'badge-approved';
      case 'Rejected': return 'badge-rejected';
      case 'Under Review': return 'badge-review';
      case 'Submitted': return 'badge-submitted';
      case 'Documents Uploaded': return 'badge-uploaded';
      case 'Registered': return 'badge-registered';
      case 'Scheduled': return 'badge-scheduled';
      default: return 'badge-default';
    }
  }

  getActionLabel(action: string): string {
    return action.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  private getAdminId(): string {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('admin_id') || localStorage.getItem('adminId') || '1';
    }
    return 'ADMIN';
  }

  private getAdminName(): string {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('admin_name') || localStorage.getItem('adminName') || 'Administrator';
    }
    return 'Administrator';
  }

  // ======================== Slot Management ========================

  loadSlots() {
    this.slotLoading = true;
    this.slotError = '';
    this.videoKycService.getAllSlots().subscribe({
      next: (slots: any[]) => {
        this.allSlots = slots;
        this.slotLoading = false;
      },
      error: () => {
        this.slotError = 'Failed to load slots.';
        this.slotLoading = false;
      }
    });
  }

  createSlot() {
    if (!this.newSlotDate || !this.newSlotTime || !this.newSlotEndTime) {
      this.slotError = 'Please fill in all slot details.';
      return;
    }
    this.slotLoading = true;
    this.slotError = '';
    this.slotSuccess = '';

    const adminName = this.getAdminName();
    this.videoKycService.createSlot(this.newSlotDate, this.newSlotTime, this.newSlotEndTime, this.newSlotMaxBookings, adminName).subscribe({
      next: () => {
        this.slotSuccess = 'Slot created successfully!';
        this.slotLoading = false;
        this.newSlotDate = '';
        this.newSlotTime = '';
        this.newSlotEndTime = '';
        this.newSlotMaxBookings = 5;
        this.loadSlots();
        setTimeout(() => this.slotSuccess = '', 3000);
      },
      error: (err: any) => {
        this.slotError = err.error?.message || 'Failed to create slot.';
        this.slotLoading = false;
      }
    });
  }

  generateDefaultSlots() {
    this.slotLoading = true;
    this.slotError = '';
    this.slotSuccess = '';

    this.videoKycService.generateDefaultSlots().subscribe({
      next: (response: any) => {
        this.slotSuccess = response.message || 'Default slots generated for next 7 days!';
        this.slotLoading = false;
        this.loadSlots();
        setTimeout(() => this.slotSuccess = '', 3000);
      },
      error: (err: any) => {
        this.slotError = err.error?.message || 'Failed to generate default slots.';
        this.slotLoading = false;
      }
    });
  }

  cancelSlot(slotId: number) {
    this.videoKycService.cancelSlot(slotId).subscribe({
      next: () => {
        this.slotSuccess = 'Slot cancelled.';
        this.loadSlots();
        setTimeout(() => this.slotSuccess = '', 3000);
      },
      error: (err: any) => {
        this.slotError = err.error?.message || 'Failed to cancel slot.';
      }
    });
  }

  // ======================== Verification Lookup ========================

  lookupVerification() {
    if (!this.verificationNumber || this.verificationNumber.length < 5) {
      this.verificationError = 'Please enter a valid verification number.';
      return;
    }
    this.verificationLoading = true;
    this.verificationError = '';
    this.verificationResult = null;

    this.videoKycService.verifyByNumber(this.verificationNumber).subscribe({
      next: (response: any) => {
        this.verificationLoading = false;
        if (response && response.found) {
          this.verificationResult = response;
        } else {
          this.verificationError = 'No session found with this verification number.';
        }
      },
      error: (err: any) => {
        this.verificationLoading = false;
        this.verificationError = err.error?.message || 'Lookup failed.';
      }
    });
  }

  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }
}
