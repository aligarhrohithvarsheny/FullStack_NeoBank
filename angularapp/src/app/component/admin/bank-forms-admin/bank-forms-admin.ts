import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BankFormDefinition, BankFormService, BankFormUploadRecord } from '../../../service/bank-form.service';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-bank-forms-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bank-forms-admin.html',
  styleUrls: ['./bank-forms-admin.css']
})
export class BankFormsAdminComponent implements OnInit {
  @Input() adminName = 'Admin';

  forms: BankFormDefinition[] = [];
  categories: string[] = [];
  uploads: BankFormUploadRecord[] = [];

  selectedCategory = 'All';
  searchQuery = '';
  activeTab: 'catalog' | 'uploads' = 'catalog';

  showUploadModal = false;
  selectedForm: BankFormDefinition | null = null;
  uploadAccountNumber = '';
  uploadAccountType: 'regular' | 'loan' | 'goldloan' | 'cheque' | 'salary' | 'current' = 'regular';
  uploadRemarks = '';
  selectedFile: File | null = null;
  isVerifyingAccount = false;
  isAccountVerified = false;
  verifiedHolderName = '';
  accountVerificationError = '';
  isUploading = false;
  isDownloading = false;
  isLoading = false;
  filterAccountNumber = '';

  accountTypes = [
    { value: 'regular', label: 'Savings / Regular' },
    { value: 'salary', label: 'Salary Account' },
    { value: 'current', label: 'Current Account' },
    { value: 'loan', label: 'Loan Account' },
    { value: 'goldloan', label: 'Gold Loan Account' },
    { value: 'cheque', label: 'Cheque Account' }
  ];

  constructor(
    private bankFormService: BankFormService,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.loadCatalog();
    this.loadUploads();
  }

  loadCatalog(): void {
    this.isLoading = true;
    this.bankFormService.getCatalog().subscribe({
      next: (res) => {
        this.forms = res.forms || [];
        this.categories = ['All', ...(res.categories || [])];
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.alertService.error('Error', 'Failed to load bank forms catalog.');
      }
    });
  }

  loadUploads(): void {
    this.bankFormService.listUploads(this.filterAccountNumber || undefined).subscribe({
      next: (res) => {
        this.uploads = res.uploads || [];
      },
      error: () => {
        this.uploads = [];
      }
    });
  }

  get filteredForms(): BankFormDefinition[] {
    const q = this.searchQuery.trim().toLowerCase();
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

  downloadPdf(form: BankFormDefinition): void {
    this.isDownloading = true;
    this.bankFormService.downloadBlankPdf(form.code, this.adminName).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `NeoBank-${form.code}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.isDownloading = false;
        this.alertService.success('Downloaded', `${form.name} PDF downloaded.`);
      },
      error: () => {
        this.isDownloading = false;
        this.alertService.error('Error', 'Failed to download PDF.');
      }
    });
  }

  openUploadModal(form: BankFormDefinition): void {
    this.selectedForm = form;
    this.uploadAccountNumber = '';
    this.uploadAccountType = 'regular';
    this.uploadRemarks = '';
    this.selectedFile = null;
    this.isAccountVerified = false;
    this.verifiedHolderName = '';
    this.accountVerificationError = '';
    this.showUploadModal = true;
  }

  closeUploadModal(): void {
    this.showUploadModal = false;
    this.selectedForm = null;
    this.selectedFile = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] || null;
  }

  verifyAccount(): void {
    if (!this.uploadAccountNumber.trim()) {
      this.isAccountVerified = false;
      this.accountVerificationError = 'Enter an account number.';
      return;
    }
    this.isVerifyingAccount = true;
    this.isAccountVerified = false;
    this.accountVerificationError = '';
    this.bankFormService.verifyAccount(this.uploadAccountNumber.trim(), this.uploadAccountType).subscribe({
      next: (res) => {
        this.isVerifyingAccount = false;
        if (res.success) {
          this.isAccountVerified = true;
          this.verifiedHolderName = res.account?.holderName || '';
          this.accountVerificationError = '';
        } else {
          this.isAccountVerified = false;
          this.accountVerificationError = res.message || 'Account not found.';
        }
      },
      error: () => {
        this.isVerifyingAccount = false;
        this.isAccountVerified = false;
        this.accountVerificationError = 'Account verification failed.';
      }
    });
  }

  submitUpload(): void {
    if (!this.selectedForm) return;
    if (!this.isAccountVerified) {
      this.alertService.error('Validation', 'Please verify the account number first.');
      return;
    }
    if (!this.selectedFile) {
      this.alertService.error('Validation', 'Please select a file to upload.');
      return;
    }

    this.isUploading = true;
    this.bankFormService.uploadForm(
      this.selectedForm.code,
      this.uploadAccountNumber.trim(),
      this.uploadAccountType,
      this.selectedFile,
      this.adminName,
      this.uploadRemarks
    ).subscribe({
      next: (res) => {
        this.isUploading = false;
        if (res.success) {
          this.alertService.success('Saved', 'Form uploaded and saved to database.');
          this.closeUploadModal();
          this.loadUploads();
        } else {
          this.alertService.error('Error', res.message || 'Upload failed.');
        }
      },
      error: (err) => {
        this.isUploading = false;
        this.alertService.error('Error', err.error?.message || 'Upload failed.');
      }
    });
  }

  filterUploadsByAccount(): void {
    this.loadUploads();
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
