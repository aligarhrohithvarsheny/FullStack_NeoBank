import { Component, Input, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminAuditService, AdminAuditLog, AdminAuditDocument } from '../../../service/admin-audit.service';

@Component({
  selector: 'app-admin-audit-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-audit-history.html',
  styleUrls: ['./admin-audit-history.css']
})
export class AdminAuditHistoryComponent implements OnInit {

  @Input() entityType: string = '';
  @Input() entityId: number = 0;

  auditHistory: AdminAuditLog[] = [];
  selectedAudit: AdminAuditLog | null = null;
  auditDocuments: AdminAuditDocument[] = [];
  isLoading: boolean = false;
  errorMessage: string = '';
  selectedDateRange: 'all' | 'today' | 'week' | 'month' = 'all';
  
  // Expandable row
  expandedAuditId: number | null = null;

  constructor(
    private auditService: AdminAuditService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadAuditHistory();
    }
  }

  /**
   * Load audit history for entity
   */
  loadAuditHistory(): void {
    if (!this.entityType || !this.entityId) {
      this.errorMessage = 'Entity type and ID are required to view audit history.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.auditService.getAuditHistory(this.entityType, this.entityId).subscribe({
      next: (history: AdminAuditLog[]) => {
        this.auditHistory = this.filterByDateRange(history);
        this.isLoading = false;
        console.log('Audit history loaded:', this.auditHistory);
      },
      error: (err: any) => {
        this.isLoading = false;
        console.error('Error loading audit history:', err);
        this.errorMessage = 'Failed to load audit history. Please try again later.';
      }
    });
  }

  /**
   * Filter audit history by date range
   */
  private filterByDateRange(history: AdminAuditLog[]): AdminAuditLog[] {
    const now = new Date();
    let startDate = new Date();

    switch (this.selectedDateRange) {
      case 'today':
        startDate.setHours(0, 0, 0, 0);
        break;
      case 'week':
        startDate.setDate(now.getDate() - 7);
        break;
      case 'month':
        startDate.setDate(now.getDate() - 30);
        break;
      default:
        return history;
    }

    return history.filter(audit => {
      const auditDate = new Date(audit.createdAt || '');
      return auditDate >= startDate;
    });
  }

  /**
   * On date range change
   */
  onDateRangeChange(): void {
    this.loadAuditHistory();
  }

  /**
   * Toggle audit row expansion
   */
  toggleAuditExpand(auditId: number | undefined): void {
    if (!auditId) return;

    if (this.expandedAuditId === auditId) {
      this.expandedAuditId = null;
    } else {
      this.expandedAuditId = auditId;
      this.loadAuditDocuments(auditId);
    }
  }

  /**
   * Load documents for selected audit
   */
  loadAuditDocuments(auditLogId: number): void {
    this.auditService.getAuditDocuments(auditLogId).subscribe({
      next: (documents: AdminAuditDocument[]) => {
        this.auditDocuments = documents;
        console.log('Audit documents loaded:', this.auditDocuments);
      },
      error: (err: any) => {
        console.error('Error loading audit documents:', err);
      }
    });
  }

  /**
   * Download document
   */
  downloadDocument(document: AdminAuditDocument): void {
    if (!document.id) return;

    const fileName = `${document.documentName || 'document'}`;
    this.auditService.downloadDocumentAndSave(document.id, fileName);
  }

  /**
   * Verify document
   */
  verifyDocument(document: AdminAuditDocument): void {
    if (!document.id) return;

    this.auditService.verifyDocument(document.id).subscribe({
      next: (result: { verified: boolean }) => {
        if (result.verified) {
          document.signatureVerified = true;
          document.status = 'VERIFIED';
          console.log('Document verified successfully');
        }
      },
      error: (err: any) => {
        console.error('Error verifying document:', err);
      }
    });
  }

  /**
   * Get change summary
   */
  getChangeSummary(changes: string | undefined): string {
    if (!changes) return 'No changes recorded';
    try {
      const parsedChanges = JSON.parse(changes);
      return Object.keys(parsedChanges)
        .map(key => `${key}: ${parsedChanges[key]}`)
        .join(', ');
    } catch {
      return changes;
    }
  }

  /**
   * Get action type badge color
   */
  getActionTypeClass(actionType: string): string {
    const classMap: { [key: string]: string } = {
      'EDIT': 'badge-info',
      'DELETE': 'badge-danger',
      'CREATE': 'badge-success',
      'APPROVE': 'badge-success',
      'REJECT': 'badge-danger',
      'UPDATE': 'badge-warning',
    };
    return classMap[actionType] || 'badge-secondary';
  }

  /**
   * Get status badge color
   */
  getStatusClass(status: string): string {
    const classMap: { [key: string]: string } = {
      'PENDING': 'badge-warning',
      'COMPLETED': 'badge-success',
      'ARCHIVED': 'badge-secondary',
      'REJECTED': 'badge-danger',
    };
    return classMap[status] || 'badge-secondary';
  }

  /**
   * Get document type icon
   */
  getDocumentTypeIcon(documentType: string): string {
    const iconMap: { [key: string]: string } = {
      'PDF': '📄',
      'IMAGE_JPG': '🖼️',
      'IMAGE_PNG': '🖼️',
      'IMAGE_GIF': '🎬',
    };
    return iconMap[documentType] || '📎';
  }

  /**
   * Format file size
   */
  formatFileSize(bytes: number | undefined): string {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }

  /**
   * Refresh history
   */
  refresh(): void {
    this.loadAuditHistory();
  }

  /**
   * Count uploaded documents in current history
   */
  totalUploadedCount(): number {
    if (!this.auditHistory) return 0;
    return this.auditHistory.filter(a => !!a.documentUploaded).length;
  }

  /**
   * Count pending required documents in current history
   */
  totalPendingCount(): number {
    if (!this.auditHistory) return 0;
    return this.auditHistory.filter(a => !a.documentUploaded && !!a.documentRequired).length;
  }
}
