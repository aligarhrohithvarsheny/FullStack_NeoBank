import { Component, Input, Output, EventEmitter, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminAuditService, AdminAuditDocument } from '../../../service/admin-audit.service';

@Component({
  selector: 'app-admin-audit-document-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-audit-document-upload.html',
  styleUrls: ['./admin-audit-document-upload.css']
})
export class AdminAuditDocumentUploadComponent {
  
  @Input() auditLogId: number | null = null;
  @Input() adminId: number | null = null;
  @Input() adminName: string = '';
  @Input() isModalOpen: boolean = false;
  
  @Output() modalClosed = new EventEmitter<void>();
  @Output() documentUploaded = new EventEmitter<AdminAuditDocument>();
  
  selectedFile: File | null = null;
  isUploading: boolean = false;
  uploadProgress: number = 0;
  errorMessage: string = '';
  successMessage: string = '';
  description: string = '';
  documentType: string = '';
  
  // Signature/Canvas properties
  canvasSignature: HTMLCanvasElement | null = null;
  signaturePad: any = null;
  isSigningMode: boolean = false;
  showSignatureCanvas: boolean = false;
  
  // File preview
  filePreview: string | null = null;
  
  constructor(
    private auditService: AdminAuditService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  /**
   * Handle file selection
   */
  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      // Validate file type (PDF, Excel, Images)
      const validTypes = [
        'application/pdf',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'image/jpeg',
        'image/png',
        'image/gif'
      ];
      if (!validTypes.includes(file.type)) {
        this.errorMessage = 'Invalid file type. Only PDF, Excel (XLS/XLSX), JPEG, PNG, GIF are allowed.';
        this.selectedFile = null;
        return;
      }

      // Validate file size (10MB)
      if (file.size > 10 * 1024 * 1024) {
        this.errorMessage = 'File size exceeds 10MB limit.';
        this.selectedFile = null;
        return;
      }

      this.selectedFile = file;
      this.errorMessage = '';
      this.description = `${file.name} - Signed by ${this.adminName} on ${new Date().toLocaleDateString()}`;
      
      // Show preview for images
      if (file.type.startsWith('image/')) {
        this.showImagePreview(file);
      }
    }
  }

  /**
   * Show image preview
   */
  private showImagePreview(file: File): void {
    if (isPlatformBrowser(this.platformId)) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.filePreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  /**
   * Upload document
   */
  uploadDocument(): void {
    if (!this.selectedFile) {
      this.errorMessage = 'Please select a file to upload.';
      return;
    }

    if (this.auditLogId == null || this.adminId == null) {
      this.errorMessage = 'Missing required information (audit log or admin ID).';
      return;
    }

    this.isUploading = true;
    this.uploadProgress = 0;
    this.errorMessage = '';
    this.successMessage = '';

    this.auditService.uploadDocument(
      this.auditLogId as number,
      this.selectedFile,
      this.adminId as number,
      this.adminName,
      this.description
    ).subscribe({
      next: (document: AdminAuditDocument) => {
        this.isUploading = false;
        this.uploadProgress = 100;
        this.successMessage = 'Document uploaded successfully!';
        this.documentUploaded.emit(document);
        
        // Reset form
        setTimeout(() => {
          this.resetForm();
          this.closeModal();
        }, 2000);
      },
      error: (err: any) => {
        this.isUploading = false;
        console.error('Error uploading document:', err);
        this.errorMessage = err.error?.message || 'Error uploading document. Please try again.';
      }
    });
  }

  /**
   * Toggle signature mode
   */
  toggleSignatureMode(): void {
    this.isSigningMode = !this.isSigningMode;
    if (this.isSigningMode) {
      this.showSignatureCanvas = true;
      setTimeout(() => {
        this.initializeSignaturePad();
      }, 100);
    }
  }

  /**
   * Initialize signature pad (canvas)
   */
  private initializeSignaturePad(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const canvas = document.getElementById('signatureCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    this.canvasSignature = canvas;
    const context = canvas.getContext('2d');
    if (!context) return;

    // Set up canvas drawing
    let isDrawing = false;

    canvas.addEventListener('mousedown', (e) => {
      isDrawing = true;
      context.beginPath();
      context.moveTo(
        (e as MouseEvent).clientX - canvas.getBoundingClientRect().left,
        (e as MouseEvent).clientY - canvas.getBoundingClientRect().top
      );
    });

    canvas.addEventListener('mousemove', (e) => {
      if (isDrawing) {
        context.lineTo(
          (e as MouseEvent).clientX - canvas.getBoundingClientRect().left,
          (e as MouseEvent).clientY - canvas.getBoundingClientRect().top
        );
        context.stroke();
      }
    });

    canvas.addEventListener('mouseup', () => {
      isDrawing = false;
    });

    canvas.addEventListener('mouseleave', () => {
      isDrawing = false;
    });

    // Touch support for mobile
    canvas.addEventListener('touchstart', (e) => {
      isDrawing = true;
      const touch = (e as TouchEvent).touches[0];
      context.beginPath();
      context.moveTo(
        touch.clientX - canvas.getBoundingClientRect().left,
        touch.clientY - canvas.getBoundingClientRect().top
      );
    });

    canvas.addEventListener('touchmove', (e) => {
      if (isDrawing) {
        const touch = (e as TouchEvent).touches[0];
        context.lineTo(
          touch.clientX - canvas.getBoundingClientRect().left,
          touch.clientY - canvas.getBoundingClientRect().top
        );
        context.stroke();
      }
    });

    canvas.addEventListener('touchend', () => {
      isDrawing = false;
    });
  }

  /**
   * Save signature and upload as image
   */
  saveSignature(): void {
    if (!this.canvasSignature) return;

    const imageData = this.canvasSignature.toDataURL('image/png');
    const base64Content = imageData.replace('data:image/png;base64,', '');
    const fileName = `signature-${this.adminName}-${Date.now()}.png`;

    this.isUploading = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.auditLogId == null || this.adminId == null) {
      this.isUploading = false;
      this.errorMessage = 'Missing required information (audit log or admin ID).';
      return;
    }

    this.auditService.uploadDocumentBase64(
      this.auditLogId as number,
      base64Content,
      fileName,
      'image/png',
      this.adminId as number,
      this.adminName,
      `Handwritten signature - ${this.description}`
    ).subscribe({
      next: (document: AdminAuditDocument) => {
        this.isUploading = false;
        this.successMessage = 'Signature uploaded successfully!';
        this.documentUploaded.emit(document);
        
        setTimeout(() => {
          this.resetForm();
          this.closeModal();
        }, 2000);
      },
      error: (err: any) => {
        this.isUploading = false;
        console.error('Error uploading signature:', err);
        this.errorMessage = 'Error uploading signature. Please try again.';
      }
    });
  }

  /**
   * Clear signature canvas
   */
  clearSignature(): void {
    if (!this.canvasSignature) return;
    const context = this.canvasSignature.getContext('2d');
    if (context) {
      context.clearRect(0, 0, this.canvasSignature.width, this.canvasSignature.height);
    }
  }

  /**
   * Reset form
   */
  resetForm(): void {
    this.selectedFile = null;
    this.description = '';
    this.documentType = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.filePreview = null;
    this.isSigningMode = false;
    this.showSignatureCanvas = false;
    this.uploadProgress = 0;
  }

  /**
   * Close modal
   */
  closeModal(): void {
    this.resetForm();
    this.modalClosed.emit();
  }
}
