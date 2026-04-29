import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BeneficiaryService } from '../../../service/beneficiary.service';

@Component({
  selector: 'app-beneficiary-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './beneficiary-management.html',
  styleUrls: ['./beneficiary-management.css']
})
export class BeneficiaryManagementComponent implements OnInit {
  beneficiaries: any[] = [];
  filteredBeneficiaries: any[] = [];
  loading = false;
  saving = false;
  showForm = false;
  editMode = false;
  editId: number | null = null;
  accountNumber = '';
  searchTerm = '';
  confirmDeleteId: number | null = null;

  form = {
    recipientAccountNumber: '',
    recipientName: '',
    phone: '',
    ifsc: '',
    nickname: ''
  };

  constructor(private beneficiaryService: BeneficiaryService) {}

  ngOnInit(): void {
    const user = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
    this.accountNumber = user.accountNumber || '';
    if (this.accountNumber) {
      this.loadBeneficiaries();
    }
  }

  loadBeneficiaries(): void {
    this.loading = true;
    this.beneficiaryService.getBeneficiariesByAccount(this.accountNumber).subscribe({
      next: (res: any) => {
        this.beneficiaries = res.data || [];
        this.filterBeneficiaries();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  filterBeneficiaries(): void {
    const term = this.searchTerm.toLowerCase();
    this.filteredBeneficiaries = term
      ? this.beneficiaries.filter(b =>
          b.recipientName?.toLowerCase().includes(term) ||
          b.recipientAccountNumber?.includes(term) ||
          b.nickname?.toLowerCase().includes(term))
      : [...this.beneficiaries];
  }

  openAddForm(): void {
    this.resetForm();
    this.showForm = true;
    this.editMode = false;
  }

  openEditForm(b: any): void {
    this.form = {
      recipientAccountNumber: b.recipientAccountNumber,
      recipientName: b.recipientName,
      phone: b.phone,
      ifsc: b.ifsc,
      nickname: b.nickname || ''
    };
    this.editId = b.id;
    this.showForm = true;
    this.editMode = true;
  }

  saveBeneficiary(): void {
    this.saving = true;
    const payload = { ...this.form, senderAccountNumber: this.accountNumber };

    const obs = this.editMode && this.editId
      ? this.beneficiaryService.updateBeneficiary(this.editId, payload)
      : this.beneficiaryService.addBeneficiary(payload);

    obs.subscribe({
      next: (res: any) => {
        this.saving = false;
        if (res.success) {
          this.showForm = false;
          this.resetForm();
          this.loadBeneficiaries();
        } else {
          alert(res.message || 'Operation failed');
        }
      },
      error: () => { this.saving = false; }
    });
  }

  deleteBeneficiary(id: number): void {
    this.beneficiaryService.deleteBeneficiary(id).subscribe({
      next: () => {
        this.confirmDeleteId = null;
        this.loadBeneficiaries();
      }
    });
  }

  resetForm(): void {
    this.form = { recipientAccountNumber: '', recipientName: '', phone: '', ifsc: '', nickname: '' };
    this.editId = null;
  }

  isFormValid(): boolean {
    return !!this.form.recipientAccountNumber && !!this.form.recipientName && !!this.form.phone && !!this.form.ifsc;
  }

  formatDate(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
