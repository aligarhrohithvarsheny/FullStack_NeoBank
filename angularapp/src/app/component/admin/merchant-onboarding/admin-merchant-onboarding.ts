import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MerchantOnboardingService } from '../../../service/merchant-onboarding.service';
import { AlertService } from '../../../service/alert.service';
import { Merchant, MerchantApplication, MerchantDevice } from '../../../model/merchant-onboarding/merchant-onboarding.model';

@Component({
  selector: 'app-admin-merchant-onboarding',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-merchant-onboarding.html',
  styleUrls: ['./admin-merchant-onboarding.css'],
  encapsulation: ViewEncapsulation.None
})
export class AdminMerchantOnboarding implements OnInit {
  merchants: Merchant[] = [];
  applications: MerchantApplication[] = [];
  searchTerm = '';
  statusFilter = 'ALL';
  selectedMerchant: Merchant | null = null;
  merchantApplications: MerchantApplication[] = [];
  merchantDevices: MerchantDevice[] = [];
  merchantTransactions: any[] = [];
  rejectionReason = '';
  showRejectModal = false;
  rejectMerchantId = '';
  loading = false;
  editing = false;
  selectedDeviceId = '';
  adminChargeAmount: number | null = null;
  adminChargeReason = 'Speaker Monthly Charges';
  upiId = '';
  upiBeneficiaryName = '';
  upiVerifyDepositAmount: number | null = null;

  editForm = {
    businessName: '',
    ownerName: '',
    mobile: '',
    email: '',
    businessType: '',
    gstNumber: '',
    shopAddress: '',
    city: '',
    state: '',
    pincode: '',
    bankName: '',
    accountNumber: '',
    ifscCode: '',
    accountHolderName: ''
  };

  stats = { totalMerchants: 0, pendingMerchants: 0, approvedMerchants: 0, rejectedMerchants: 0, activeSoundboxes: 0, activePos: 0 };

  get adminName(): string {
    try {
      const admin = JSON.parse(sessionStorage.getItem('admin') || '{}');
      return admin.name || admin.username || 'Admin';
    } catch { return 'Admin'; }
  }

  constructor(
    private merchantService: MerchantOnboardingService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadAll();
  }

  loadAll() {
    this.merchantService.getAllMerchants().subscribe({
      next: (res: any) => {
        if (Array.isArray(res)) {
          this.merchants = res;
        } else if (res?.success) {
          this.merchants = res.merchants || [];
        } else {
          this.merchants = [];
        }
      },
      error: () => { this.merchants = []; }
    });
    this.merchantService.getAllApplications().subscribe({
      next: (res: any) => {
        if (Array.isArray(res)) {
          this.applications = res;
        } else if (res?.success) {
          this.applications = res.applications || [];
        } else {
          this.applications = [];
        }
      },
      error: () => { this.applications = []; }
    });
    this.merchantService.getAdminStats().subscribe({
      next: (res: any) => {
        const stats = res?.stats ?? res ?? {};
        this.stats = {
          totalMerchants: stats.totalMerchants ?? 0,
          pendingMerchants: stats.pendingMerchants ?? stats.pending ?? 0,
          approvedMerchants: stats.approvedMerchants ?? stats.approved ?? 0,
          rejectedMerchants: stats.rejectedMerchants ?? stats.rejected ?? 0,
          activeSoundboxes: stats.activeSoundboxes ?? stats.soundboxCount ?? 0,
          activePos: stats.activePos ?? stats.posCount ?? 0
        };
      }
    });
  }

  get filteredMerchants(): Merchant[] {
    const q = (this.searchTerm || '').toLowerCase();
    return this.merchants.filter(m => {
      const matchQ = !q || (m.businessName || '').toLowerCase().includes(q)
        || (m.ownerName || '').toLowerCase().includes(q)
        || (m.merchantId || '').toLowerCase().includes(q)
        || (m.mobile || '').includes(q)
        || (m.agentId || '').toLowerCase().includes(q);
      const matchStatus = this.statusFilter === 'ALL' || m.status === this.statusFilter;
      return matchQ && matchStatus;
    });
  }

  viewDetails(merchant: Merchant) {
    this.selectedMerchant = merchant;
    this.editing = false;
    this.merchantApplications = [];
    this.merchantDevices = [];
    this.merchantTransactions = [];
    if (merchant.merchantId) {
      this.merchantService.getApplicationsByMerchant(merchant.merchantId).subscribe({
        next: (res: any) => { this.merchantApplications = Array.isArray(res) ? res : (res?.applications || []); }
      });
      this.merchantService.getDevicesByMerchant(merchant.merchantId).subscribe({
        next: (res: any) => {
          this.merchantDevices = Array.isArray(res) ? res : (res?.devices || []);
          this.selectedDeviceId = this.merchantDevices[0]?.deviceId || '';
        }
      });
      this.merchantService.getTransactionsByMerchant(merchant.merchantId).subscribe({
        next: (res: any) => { this.merchantTransactions = Array.isArray(res) ? res : (res?.transactions || []); }
      });
    }
  }

  closeDetails() {
    this.selectedMerchant = null;
    this.editing = false;
  }

  startEdit() {
    if (!this.selectedMerchant) return;
    this.editForm = {
      businessName: this.selectedMerchant.businessName || '',
      ownerName: this.selectedMerchant.ownerName || '',
      mobile: this.selectedMerchant.mobile || '',
      email: this.selectedMerchant.email || '',
      businessType: this.selectedMerchant.businessType || '',
      gstNumber: this.selectedMerchant.gstNumber || '',
      shopAddress: this.selectedMerchant.shopAddress || '',
      city: this.selectedMerchant.city || '',
      state: this.selectedMerchant.state || '',
      pincode: this.selectedMerchant.pincode || '',
      bankName: this.selectedMerchant.bankName || '',
      accountNumber: this.selectedMerchant.accountNumber || '',
      ifscCode: this.selectedMerchant.ifscCode || '',
      accountHolderName: this.selectedMerchant.accountHolderName || ''
    };
    this.editing = true;
  }

  cancelEdit() {
    this.editing = false;
  }

  saveEdit() {
    if (!this.selectedMerchant?.merchantId) return;
    if (!this.editForm.businessName.trim() || !this.editForm.ownerName.trim() || !this.editForm.mobile.trim()) {
      this.alertService.error('Error', 'Business name, owner name and mobile are required');
      return;
    }
    if (!/^[6-9]\d{9}$/.test(this.editForm.mobile.trim())) {
      this.alertService.error('Error', 'Please enter a valid 10-digit mobile number');
      return;
    }
    if (this.editForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.editForm.email.trim())) {
      this.alertService.error('Error', 'Please enter a valid email');
      return;
    }

    this.loading = true;
    const payload = {
      businessName: this.editForm.businessName.trim(),
      ownerName: this.editForm.ownerName.trim(),
      mobile: this.editForm.mobile.trim(),
      email: this.editForm.email.trim(),
      businessType: this.editForm.businessType.trim(),
      gstNumber: this.editForm.gstNumber.trim(),
      shopAddress: this.editForm.shopAddress.trim(),
      city: this.editForm.city.trim(),
      state: this.editForm.state.trim(),
      pincode: this.editForm.pincode.trim(),
      bankName: this.editForm.bankName.trim(),
      accountNumber: this.editForm.accountNumber.trim(),
      ifscCode: this.editForm.ifscCode.trim().toUpperCase(),
      accountHolderName: this.editForm.accountHolderName.trim()
    };

    this.merchantService.updateMerchantAdmin(this.selectedMerchant.merchantId, payload).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res?.success) {
          this.selectedMerchant = res.merchant;
          this.alertService.success('Updated', 'Merchant details updated successfully');
          this.editing = false;
          this.loadAll();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.error || 'Update failed');
      }
    });
  }

  approveMerchant(merchantId: string) {
    this.loading = true;
    this.merchantService.approveMerchant(merchantId, this.adminName).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) {
          this.alertService.success('Approved', 'Merchant approved and devices assigned!');
          this.closeDetails();
          this.loadAll();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.error || 'Approval failed');
      }
    });
  }

  openRejectModal(merchantId: string) {
    this.rejectMerchantId = merchantId;
    this.rejectionReason = '';
    this.showRejectModal = true;
  }

  closeRejectModal() {
    this.showRejectModal = false;
    this.rejectionReason = '';
    this.rejectMerchantId = '';
  }

  confirmReject() {
    if (!this.rejectionReason.trim()) {
      this.alertService.error('Error', 'Please enter a rejection reason');
      return;
    }
    this.loading = true;
    this.merchantService.rejectMerchant(this.rejectMerchantId, this.adminName, this.rejectionReason).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) {
          this.alertService.success('Rejected', 'Merchant application rejected');
          this.closeRejectModal();
          this.closeDetails();
          this.loadAll();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.error || 'Rejection failed');
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      case 'ACTIVE': return 'status-approved';
      default: return '';
    }
  }

  getMerchantChargeDebitTotal(): number {
    return this.merchantTransactions
      .filter(t => t?.paymentMode === 'DEBIT_CHARGE')
      .reduce((sum, t) => sum + Number(t?.amount || 0), 0);
  }

  adminUpdateDeviceStatus(status: 'ACTIVE' | 'PAUSED' | 'INACTIVE') {
    if (!this.selectedMerchant?.merchantId || !this.selectedDeviceId) return;
    this.loading = true;
    this.merchantService.adminUpdateMerchantDeviceStatus(this.selectedMerchant.merchantId, this.selectedDeviceId, status).subscribe({
      next: () => {
        this.loading = false;
        this.viewDetails(this.selectedMerchant!);
        this.alertService.success('Updated', `Device status changed to ${status}`);
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.error || 'Unable to update device status');
      }
    });
  }

  adminDebitCharges() {
    if (!this.selectedMerchant?.merchantId || !this.selectedDeviceId || !this.adminChargeAmount || this.adminChargeAmount <= 0) {
      return;
    }
    this.loading = true;
    this.merchantService.adminDebitMerchantSpeakerCharges(
      this.selectedMerchant.merchantId,
      this.selectedDeviceId,
      this.adminChargeAmount,
      this.adminChargeReason
    ).subscribe({
      next: () => {
        this.loading = false;
        this.adminChargeAmount = null;
        this.viewDetails(this.selectedMerchant!);
        this.alertService.success('Debited', 'Speaker charges debited successfully');
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.error || 'Unable to debit charges');
      }
    });
  }

  adminLinkUpi() {
    if (!this.selectedMerchant?.merchantId || !this.selectedDeviceId || !this.upiId.trim()) return;
    this.loading = true;
    this.merchantService.adminLinkMerchantUpi(
      this.selectedMerchant.merchantId,
      this.selectedDeviceId,
      this.upiId.trim(),
      this.upiBeneficiaryName.trim(),
      Number(this.upiVerifyDepositAmount || 0)
    ).subscribe({
      next: () => {
        this.loading = false;
        this.upiId = '';
        this.upiBeneficiaryName = '';
        this.upiVerifyDepositAmount = null;
        this.viewDetails(this.selectedMerchant!);
        this.alertService.success('Success', 'UPI linked and verification transaction recorded');
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.error || 'Unable to link UPI');
      }
    });
  }

  goBack() { history.back(); }
}
