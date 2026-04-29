import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentVerificationService } from '../../../service/document-verification.service';

@Component({
  selector: 'app-document-verification',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './document-verification.html',
  styleUrls: ['./document-verification.css']
})
export class DocumentVerificationComponent implements OnInit {
  documents: any[] = [];
  filteredDocuments: any[] = [];
  selectedDocument: any = null;
  loading = false;
  processing = false;
  activeFilter = 'ALL';
  searchTerm = '';
  rejectionReason = '';
  remarks = '';

  stats = { total: 0, pending: 0, verified: 0, rejected: 0 };
  filters = ['ALL', 'PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED'];

  constructor(private docService: DocumentVerificationService) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading = true;
    this.docService.getAll().subscribe({
      next: (res: any) => {
        this.documents = res.data || [];
        this.calculateStats();
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  calculateStats(): void {
    this.stats.total = this.documents.length;
    this.stats.pending = this.documents.filter(d => d.status === 'PENDING').length;
    this.stats.verified = this.documents.filter(d => d.status === 'VERIFIED').length;
    this.stats.rejected = this.documents.filter(d => d.status === 'REJECTED').length;
  }

  applyFilter(): void {
    let docs = this.activeFilter === 'ALL' ? [...this.documents] : this.documents.filter(d => d.status === this.activeFilter);
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      docs = docs.filter(d =>
        d.userName?.toLowerCase().includes(term) ||
        d.accountNumber?.includes(term) ||
        d.documentNumber?.toLowerCase().includes(term));
    }
    this.filteredDocuments = docs;
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  selectDocument(doc: any): void {
    this.selectedDocument = doc;
    this.rejectionReason = '';
    this.remarks = doc.remarks || '';
  }

  verifyDocument(): void {
    if (!this.selectedDocument) return;
    this.processing = true;
    const admin = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
    this.docService.verify(this.selectedDocument.id, admin.name || 'Manager', this.remarks).subscribe({
      next: () => {
        this.processing = false;
        this.selectedDocument = null;
        this.loadDocuments();
      },
      error: () => { this.processing = false; }
    });
  }

  rejectDocument(): void {
    if (!this.selectedDocument || !this.rejectionReason) return;
    this.processing = true;
    const admin = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
    this.docService.reject(this.selectedDocument.id, admin.name || 'Manager', this.rejectionReason).subscribe({
      next: () => {
        this.processing = false;
        this.selectedDocument = null;
        this.loadDocuments();
      },
      error: () => { this.processing = false; }
    });
  }

  getDocIcon(type: string): string {
    const icons: Record<string, string> = { 'AADHAAR_CARD': '🆔', 'PAN_CARD': '💳', 'ADDRESS_PROOF': '🏠' };
    return icons[type] || '📄';
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = { 'PENDING': 'status-pending', 'VERIFIED': 'status-verified', 'REJECTED': 'status-rejected', 'EXPIRED': 'status-expired' };
    return classes[status] || '';
  }

  formatDocType(type: string): string {
    return (type || '').replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  formatDate(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
