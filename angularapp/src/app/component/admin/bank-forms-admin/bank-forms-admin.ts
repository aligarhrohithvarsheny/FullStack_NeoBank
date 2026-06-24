import { Component, Input, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  BankFormDefinition,
  BankFormService,
  BankFormUploadHistoryRecord,
  BankFormUploadRecord,
  VerifiedAccountDetails
} from '../../../service/bank-form.service';
import { AlertService } from '../../../service/alert.service';
import { BackendWakeupService } from '../../../service/backend-wakeup.service';

@Component({
  selector: 'app-bank-forms-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bank-forms-admin.html',
  styleUrls: ['./bank-forms-admin.css']
})
export class BankFormsAdminComponent implements OnInit {
  @Input() adminName = 'Admin';

  private readonly platformId = inject(PLATFORM_ID);

  forms: BankFormDefinition[] = [];
  categories: string[] = [];
  uploads: BankFormUploadRecord[] = [];

  selectedCategory = 'All';
  formSearchQuery = '';
  activeTab: 'submit' | 'catalog' | 'uploads' = 'submit';

  selectedFormCode = '';
  lookupMode: 'accountNumber' | 'customerId' = 'accountNumber';
  uploadAccountNumber = '';
  uploadCustomerId = '';
  uploadAccountType: 'regular' | 'loan' | 'goldloan' | 'cheque' | 'salary' | 'current' = 'regular';
  uploadRemarks = '';
  selectedFile: File | null = null;
  verifiedAccount: VerifiedAccountDetails | null = null;
  isVerifyingAccount = false;
  isAccountVerified = false;
  verifiedHolderName = '';
  accountVerificationError = '';
  isUploading = false;
  isDownloading = false;
  isDownloadingUploadId: number | null = null;
  isViewingUploadId: number | null = null;
  isReplacingUploadId: number | null = null;
  isLoading = false;
  isDeleting = false;
  filterAccountNumber = '';
  filterFormCode = '';
  selectedHistoryUploadId: number | null = null;
  uploadHistory: BankFormUploadHistoryRecord[] = [];

  accountTypes = [
    { value: 'regular', label: 'Savings / Regular' },
    { value: 'salary', label: 'Salary Account' },
    { value: 'current', label: 'Current Account' },
    { value: 'loan', label: 'Loan Account' },
    { value: 'goldloan', label: 'Gold Loan Account' },
    { value: 'cheque', label: 'Cheque Account' }
  ];

  readonly defaultCommonFields = [
    'Account Number', 'Name', 'Customer ID', 'Aadhaar Number',
    'Signature', 'Bank Name (NeoBank)', 'Terms and Conditions (I Accept)'
  ];

  constructor(
    private bankFormService: BankFormService,
    private alertService: AlertService,
    private backendWakeup: BackendWakeupService
  ) {}

  ngOnInit(): void {
    this.backendWakeup.ensureAwake().finally(() => {
      this.loadCatalog();
      this.loadUploads();
    });
  }

  get selectedForm(): BankFormDefinition | null {
    if (!this.selectedFormCode) return null;
    return this.forms.find((f) => f.code === this.selectedFormCode) || null;
  }

  get canDownloadPdf(): boolean {
    return !!this.selectedForm && this.isAccountVerified && !!this.verifiedAccount;
  }

  get formSelectOptions(): BankFormDefinition[] {
    const q = this.formSearchQuery.trim().toLowerCase();
    return this.forms.filter((f) => {
      if (!q) return true;
      return (
        f.name.toLowerCase().includes(q) ||
        f.code.toLowerCase().includes(q) ||
        f.category.toLowerCase().includes(q) ||
        String(f.id).includes(q)
      );
    });
  }

  get filteredCatalogForms(): BankFormDefinition[] {
    const q = this.formSearchQuery.trim().toLowerCase();
    return this.forms.filter((f) => {
      const categoryMatch = this.selectedCategory === 'All' || f.category === this.selectedCategory;
      if (!categoryMatch) return false;
      if (!q) return true;
      return (
        f.name.toLowerCase().includes(q) ||
        f.code.toLowerCase().includes(q) ||
        f.category.toLowerCase().includes(q) ||
        String(f.id).includes(q)
      );
    });
  }

  loadCatalog(): void {
    this.isLoading = true;
    this.bankFormService.getCatalog().subscribe({
      next: (res) => {
        this.forms = (res.forms || []).map((f) => ({
          ...f,
          commonFields: f.commonFields?.length ? f.commonFields : this.defaultCommonFields,
          allFields: f.allFields?.length ? f.allFields : [...this.defaultCommonFields, ...(f.fields || [])]
        }));
        this.categories = ['All', ...(res.categories || [])];
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.alertService.error(
          'Error',
          'Failed to load bank forms catalog. The backend may be waking up — click Refresh or wait a moment and reopen this page.'
        );
      }
    });
  }

  loadUploads(): void {
    this.bankFormService.listUploads(
      this.filterAccountNumber || undefined,
      this.filterFormCode || undefined
    ).subscribe({
      next: (res) => {
        this.uploads = res.uploads || [];
      },
      error: () => {
        this.uploads = [];
      }
    });
  }

  onFormSelected(): void {
    this.resetVerification();
    this.selectedFile = null;
  }

  onLookupModeChange(): void {
    this.resetVerification();
  }

  resetVerification(): void {
    this.isAccountVerified = false;
    this.verifiedHolderName = '';
    this.verifiedAccount = null;
    this.accountVerificationError = '';
  }

  downloadPdf(form?: BankFormDefinition | null): void {
    const target = form || this.selectedForm;
    if (!target) {
      this.alertService.error('Validation', 'Please select a form first.');
      return;
    }
    if (!this.isAccountVerified || !this.verifiedAccount) {
      this.alertService.error('Validation', 'Verify account number or customer ID before downloading the PDF.');
      return;
    }

    this.isDownloading = true;
    this.bankFormService.downloadBlankPdf(target.code, {
      adminName: this.adminName,
      accountNumber: this.verifiedAccount.accountNumber,
      accountType: this.verifiedAccount.accountType,
      holderName: this.verifiedAccount.holderName,
      customerId: this.verifiedAccount.customerId,
      aadhaarNumber: this.verifiedAccount.aadhaarNumber,
      panNumber: this.verifiedAccount.panNumber,
      phone: this.verifiedAccount.phone,
      email: this.verifiedAccount.email
    }).subscribe({
      next: (resp) => {
        this.isDownloading = false;
        const blob = resp.body;
        if (!blob || blob.size === 0) {
          this.alertService.error('Error', 'Empty PDF response from server.');
          return;
        }
        const contentType = resp.headers.get('Content-Type') || '';
        if (contentType.includes('application/json')) {
          blob.text().then((text) => {
            try {
              const err = JSON.parse(text);
              this.alertService.error('Error', err.message || 'Failed to download PDF.');
            } catch {
              this.alertService.error('Error', 'Failed to download PDF.');
            }
          });
          return;
        }
        const filename = this.extractFilename(resp.headers.get('Content-Disposition'), `NeoBank-${target.code}.pdf`);
        this.triggerBrowserDownload(blob, filename);
        this.alertService.success('Downloaded', `${target.name} PDF downloaded with customer details auto-filled.`);
      },
      error: async (err) => {
        this.isDownloading = false;
        const message = await this.readBlobErrorMessage(err);
        this.alertService.error('Error', message || 'Failed to download PDF. Check that the backend is running.');
      }
    });
  }

  downloadUploadedFile(record: BankFormUploadRecord): void {
    this.isDownloadingUploadId = record.id;
    this.bankFormService.downloadUploadedFile(record.id).subscribe({
      next: (resp) => {
        this.isDownloadingUploadId = null;
        const blob = resp.body;
        if (!blob || blob.size === 0) {
          this.alertService.error('Error', 'Uploaded file could not be downloaded.');
          return;
        }
        const filename = this.extractFilename(
          resp.headers.get('Content-Disposition'),
          record.originalFileName || `NeoBank-upload-${record.id}`
        );
        this.triggerBrowserDownload(blob, filename);
        this.alertService.success('Downloaded', filename);
      },
      error: async (err) => {
        this.isDownloadingUploadId = null;
        const message = await this.readBlobErrorMessage(err);
        this.alertService.error('Error', message || 'Failed to download uploaded file.');
      }
    });
  }

  viewUploadedFile(record: BankFormUploadRecord): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.isViewingUploadId = record.id;
    this.bankFormService.viewUploadedFile(record.id).subscribe({
      next: (resp) => {
        this.isViewingUploadId = null;
        const blob = resp.body;
        if (!blob || blob.size === 0) {
          this.alertService.error('Error', 'Uploaded file could not be opened.');
          return;
        }
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => window.URL.revokeObjectURL(url), 60000);
      },
      error: async (err) => {
        this.isViewingUploadId = null;
        const message = await this.readBlobErrorMessage(err);
        this.alertService.error('Error', message || 'Failed to view uploaded file.');
      }
    });
  }

  onReplaceFileSelected(record: BankFormUploadRecord, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.isReplacingUploadId = record.id;
    this.bankFormService.replaceUpload(record.id, file, this.adminName, 'Replaced from admin dashboard').subscribe({
      next: (res) => {
        this.isReplacingUploadId = null;
        input.value = '';
        if (res.success) {
          this.alertService.success('Replaced', 'Uploaded file replaced and history saved.');
          this.loadUploads();
          if (this.selectedHistoryUploadId === record.id) {
            this.loadUploadHistory(record.id);
          }
        } else {
          this.alertService.error('Error', res.message || 'Replace failed.');
        }
      },
      error: async (err) => {
        this.isReplacingUploadId = null;
        input.value = '';
        const message = await this.readBlobErrorMessage(err);
        this.alertService.error('Error', message || 'Failed to replace uploaded file.');
      }
    });
  }

  toggleUploadHistory(record: BankFormUploadRecord): void {
    if (this.selectedHistoryUploadId === record.id) {
      this.selectedHistoryUploadId = null;
      this.uploadHistory = [];
      return;
    }
    this.selectedHistoryUploadId = record.id;
    this.loadUploadHistory(record.id);
  }

  loadUploadHistory(uploadId: number): void {
    this.bankFormService.getUploadHistory(uploadId).subscribe({
      next: (res) => {
        this.uploadHistory = res.history || [];
      },
      error: () => {
        this.uploadHistory = [];
        this.alertService.error('Error', 'Failed to load upload history.');
      }
    });
  }

  private triggerBrowserDownload(blob: Blob, filename: string): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  private extractFilename(contentDisposition: string | null, fallback: string): string {
    if (!contentDisposition) {
      return fallback;
    }
    const match =
      /filename\*=(?:UTF-8'')?([^;\n]+)/i.exec(contentDisposition) ||
      /filename="([^"]+)"/i.exec(contentDisposition) ||
      /filename=([^;\s]+)/i.exec(contentDisposition);
    if (match?.[1]) {
      return decodeURIComponent(match[1].trim().replace(/^["']|["']$/g, ''));
    }
    return fallback;
  }

  private async readBlobErrorMessage(err: any): Promise<string> {
    const blob = err?.error;
    if (blob instanceof Blob) {
      try {
        const text = await blob.text();
        const parsed = JSON.parse(text);
        return parsed.message || parsed.error || text;
      } catch {
        return '';
      }
    }
    return err?.error?.message || err?.message || '';
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] || null;
  }

  verifyAccount(): void {
    const hasAccount = this.lookupMode === 'accountNumber' && this.uploadAccountNumber.trim();
    const hasCustomerId = this.lookupMode === 'customerId' && this.uploadCustomerId.trim();

    if (!hasAccount && !hasCustomerId) {
      this.resetVerification();
      this.accountVerificationError = this.lookupMode === 'customerId'
        ? 'Enter a customer ID.'
        : 'Enter an account number.';
      return;
    }

    this.isVerifyingAccount = true;
    this.resetVerification();
    this.bankFormService.verifyAccount({
      accountNumber: hasAccount ? this.uploadAccountNumber.trim() : undefined,
      accountType: this.uploadAccountType,
      customerId: hasCustomerId ? this.uploadCustomerId.trim() : undefined
    }).subscribe({
      next: (res) => {
        this.isVerifyingAccount = false;
        if (res.success && res.account) {
          this.isAccountVerified = true;
          this.verifiedAccount = {
            accountNumber: res.account.accountNumber,
            holderName: res.account.holderName || '',
            accountType: res.account.accountType || this.uploadAccountType,
            customerId: res.account.customerId,
            aadhaarNumber: res.account.aadhaarNumber,
            panNumber: res.account.panNumber,
            phone: res.account.phone,
            email: res.account.email,
            balance: res.account.balance
          };
          this.verifiedHolderName = this.verifiedAccount.holderName;
          this.uploadAccountNumber = this.verifiedAccount.accountNumber;
          this.uploadCustomerId = this.verifiedAccount.customerId || this.uploadCustomerId;
          if (this.verifiedAccount.accountType) {
            this.uploadAccountType = this.verifiedAccount.accountType;
          }
          this.accountVerificationError = '';
        } else {
          this.accountVerificationError = res.message || 'Account not found.';
        }
      },
      error: () => {
        this.isVerifyingAccount = false;
        this.accountVerificationError = 'Account verification failed.';
      }
    });
  }

  submitUpload(): void {
    if (!this.selectedForm) {
      this.alertService.error('Validation', 'Please search and select a form name.');
      return;
    }
    if (!this.isAccountVerified || !this.verifiedAccount) {
      this.alertService.error('Validation', 'Please verify the account number or customer ID first.');
      return;
    }
    if (!this.selectedFile) {
      this.alertService.error('Validation', 'Please select the application file to upload.');
      return;
    }

    this.isUploading = true;
    this.bankFormService.uploadForm(
      this.selectedForm.code,
      this.verifiedAccount.accountNumber,
      this.verifiedAccount.accountType,
      this.selectedFile,
      this.adminName,
      this.uploadRemarks
    ).subscribe({
      next: (res) => {
        this.isUploading = false;
        if (res.success) {
          this.alertService.success('Saved', 'Application uploaded and saved to database.');
          this.selectedFile = null;
          this.uploadRemarks = '';
          this.loadUploads();
          this.activeTab = 'uploads';
        } else {
          this.alertService.error('Error', res.message || 'Upload failed.');
        }
      },
      error: async (err) => {
        this.isUploading = false;
        const message = await this.readBlobErrorMessage(err);
        this.alertService.error('Error', message || err.error?.message || 'Upload failed.');
      }
    });
  }

  deleteUpload(record: BankFormUploadRecord): void {
    if (!confirm(`Delete uploaded application for ${record.formName} (${record.accountNumber})?`)) return;
    this.isDeleting = true;
    this.bankFormService.deleteUpload(record.id).subscribe({
      next: (res) => {
        this.isDeleting = false;
        if (res.success) {
          this.alertService.success('Deleted', 'Upload removed.');
          if (this.selectedHistoryUploadId === record.id) {
            this.selectedHistoryUploadId = null;
            this.uploadHistory = [];
          }
          this.loadUploads();
        }
      },
      error: () => {
        this.isDeleting = false;
        this.alertService.error('Error', 'Failed to delete upload.');
      }
    });
  }

  clearAllUploads(): void {
    if (!this.uploads.length) return;
    if (!confirm(`Clear all ${this.uploads.length} uploaded application(s)? This cannot be undone.`)) return;
    this.isDeleting = true;
    this.bankFormService.clearAllUploads().subscribe({
      next: (res) => {
        this.isDeleting = false;
        if (res.success) {
          this.alertService.success('Cleared', res.message || 'All uploads cleared.');
          this.selectedHistoryUploadId = null;
          this.uploadHistory = [];
          this.loadUploads();
        }
      },
      error: () => {
        this.isDeleting = false;
        this.alertService.error('Error', 'Failed to clear uploads.');
      }
    });
  }

  selectFormFromCatalog(form: BankFormDefinition): void {
    this.selectedFormCode = form.code;
    this.formSearchQuery = form.name;
    this.activeTab = 'submit';
    this.onFormSelected();
  }

  formatDate(value: string): string {
    if (!value) return '-';
    return new Date(value).toLocaleString('en-IN');
  }

  formatFileSize(bytes: number): string {
    if (!bytes) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
