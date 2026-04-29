import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../environment/environment';
import { isPlatformBrowser } from '@angular/common';

export interface AdminAuditLog {
  id?: number;
  adminId: number;
  adminName: string;
  actionType: string;
  entityType: string;
  entityId: number;
  entityName: string;
  changes?: string;
  oldValues?: string;
  newValues?: string;
  documentRequired: boolean;
  documentUploaded: boolean;
  reasonForChange?: string;
  createdAt?: string;
  ipAddress?: string;
  userAgent?: string;
  status: string;
}

export interface AdminAuditDocument {
  id?: number;
  auditLogId: number;
  documentName: string;
  documentType: string;
  filePath?: string;
  fileSize?: number;
  fileUrl?: string;
  fileBase64?: string;
  uploadedBy: number;
  uploadedByName: string;
  uploadedAt?: string;
  documentHash?: string;
  description?: string;
  isSigned: boolean;
  signatureVerified: boolean;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminAuditService {

  private apiUrl = `${environment.apiBaseUrl}/api/audit`;
  private pendingDocumentsSubject = new BehaviorSubject<number>(0);
  public pendingDocuments$ = this.pendingDocumentsSubject.asObservable();

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    if (isPlatformBrowser(this.platformId)) {
      this.updatePendingDocumentCount();
    }
  }

  /**
   * Create a new audit log entry
   */
  createAuditLog(
    adminId: number,
    adminName: string,
    actionType: string,
    entityType: string,
    entityId: number,
    entityName: string,
    reasonForChange?: string,
    requireDocument: boolean = false
  ): Observable<AdminAuditLog> {
    const params = new HttpParams()
      .set('adminId', adminId.toString())
      .set('adminName', adminName)
      .set('actionType', actionType)
      .set('entityType', entityType)
      .set('entityId', entityId.toString())
      .set('entityName', entityName)
      .set('requireDocument', requireDocument.toString());

    if (reasonForChange) {
      params.set('reasonForChange', reasonForChange);
    }

    return this.http.post<AdminAuditLog>(`${this.apiUrl}/log/create`, {}, { params });
  }

  /**
   * Update audit log with changes
   */
  updateAuditLog(
    auditLogId: number,
    oldValues: string,
    newValues: string,
    changes: string
  ): Observable<AdminAuditLog> {
    const body = {
      oldValues,
      newValues,
      changes
    };
    return this.http.put<AdminAuditLog>(`${this.apiUrl}/log/${auditLogId}`, body);
  }

  /**
   * Upload signed document for audit log (using FormData)
   */
  uploadDocument(
    auditLogId: number,
    file: File,
    adminId: number,
    adminName: string,
    description?: string
  ): Observable<AdminAuditDocument> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('adminId', adminId.toString());
    formData.append('adminName', adminName);
    if (description) {
      formData.append('description', description);
    }

    return this.http.post<AdminAuditDocument>(
      `${this.apiUrl}/documents/upload/${auditLogId}`,
      formData
    );
  }

  /**
   * Upload document using base64 encoding (for signed documents/canvas)
   */
  uploadDocumentBase64(
    auditLogId: number,
    base64Content: string,
    fileName: string,
    mimeType: string,
    adminId: number,
    adminName: string,
    description?: string
  ): Observable<AdminAuditDocument> {
    const body = {
      base64Content,
      fileName,
      mimeType,
      adminId: adminId.toString(),
      adminName,
      description: description || ''
    };

    return this.http.post<AdminAuditDocument>(
      `${this.apiUrl}/documents/upload-base64/${auditLogId}`,
      body
    );
  }

  /**
   * Get audit history for an entity
   */
  getAuditHistory(entityType: string, entityId: number): Observable<AdminAuditLog[]> {
    return this.http.get<AdminAuditLog[]>(`${this.apiUrl}/history/${entityType}/${entityId}`);
  }

  /**
   * Get all documents for an audit log
   */
  getAuditDocuments(auditLogId: number): Observable<AdminAuditDocument[]> {
    return this.http.get<AdminAuditDocument[]>(`${this.apiUrl}/documents/log/${auditLogId}`);
  }

  /**
   * Get all documents for an entity
   */
  getEntityDocuments(entityType: string, entityId: number): Observable<AdminAuditDocument[]> {
    return this.http.get<AdminAuditDocument[]>(`${this.apiUrl}/documents/entity/${entityType}/${entityId}`);
  }

  /**
   * Get audit logs by admin
   */
  getAdminAuditLogs(adminId: number): Observable<AdminAuditLog[]> {
    return this.http.get<AdminAuditLog[]>(`${this.apiUrl}/logs/admin/${adminId}`);
  }

  /**
   * Get pending document uploads
   */
  getPendingDocuments(): Observable<AdminAuditLog[]> {
    return this.http.get<AdminAuditLog[]>(`${this.apiUrl}/pending-documents`);
  }

  /**
   * Get count of pending documents
   */
  getPendingDocumentCount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/pending-documents/count`);
  }

  /**
   * Update pending documents count in local state
   */
  updatePendingDocumentCount(): void {
    this.getPendingDocumentCount().subscribe({
      next: (count) => {
        this.pendingDocumentsSubject.next(count);
      },
      error: (err) => {
        console.error('Error updating pending document count:', err);
      }
    });
  }

  /**
   * Download document file
   */
  downloadDocument(documentId: number): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/documents/${documentId}/download`,
      { responseType: 'blob' }
    );
  }

  /**
   * Download document and save locally
   */
  downloadDocumentAndSave(documentId: number, fileName: string): void {
    this.downloadDocument(documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading document:', err);
      }
    });
  }

  /**
   * Verify document integrity
   */
  verifyDocument(documentId: number): Observable<{ verified: boolean }> {
    return this.http.post<{ verified: boolean }>(
      `${this.apiUrl}/documents/${documentId}/verify`,
      {}
    );
  }

  /**
   * Get audit logs by date range
   */
  getAuditLogsByDateRange(startDate: string, endDate: string): Observable<AdminAuditLog[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<AdminAuditLog[]>(
      `${this.apiUrl}/logs/date-range`,
      { params }
    );
  }

  /**
   * Get audit logs by admin and date range
   */
  getAuditLogsByAdminAndDateRange(
    adminId: number,
    startDate: string,
    endDate: string
  ): Observable<AdminAuditLog[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<AdminAuditLog[]>(
      `${this.apiUrl}/logs/admin/${adminId}/date-range`,
      { params }
    );
  }

  /**
   * Convert object to JSON string for audit changes
   */
  serializeChanges(changes: any): string {
    return JSON.stringify(changes, null, 2);
  }

  /**
   * Get file extension from document type
   */
  getFileExtension(documentType: string): string {
    const extensions: { [key: string]: string } = {
      'PDF': '.pdf',
      'IMAGE_JPG': '.jpg',
      'IMAGE_PNG': '.png',
      'IMAGE_GIF': '.gif'
    };
    return extensions[documentType] || '.pdf';
  }

  /**
   * Check if document requires signature (e.g., for sensitive operations)
   */
  isSignatureRequired(actionType: string): boolean {
    const requireSignature = ['EDIT_USER', 'DELETE_USER', 'APPROVE_LOAN', 'APPROVE_KYC'];
    return requireSignature.includes(actionType);
  }
}
