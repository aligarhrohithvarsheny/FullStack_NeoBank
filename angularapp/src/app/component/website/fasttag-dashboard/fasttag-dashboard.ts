import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-fasttag-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fasttag-dashboard.html',
  styleUrls: ['./fasttag-dashboard.css'],
  encapsulation: ViewEncapsulation.None
})
export class FasttagDashboard implements OnInit {
  user: any = null;
  fasttags: any[] = [];
  activeTags: any[] = [];
  closedTags: any[] = [];
  loading: boolean = true;
  isBrowser: boolean = false;

  // View state
  activeTab: string = 'active'; // 'active' | 'closed' | 'all'
  selectedTag: any = null;
  showDetailModal: boolean = false;

  // Apply form
  showApplyForm: boolean = false;
  applyForm = {
    userName: '',
    vehicleNumber: '',
    vehicleType: 'Car',
    chassisNumber: '',
    engineNumber: '',
    mobileNumber: ''
  };
  applyLoading: boolean = false;
  applyMessage: string = '';
  applySuccess: boolean = false;

  // Recharge
  showRechargeModal: boolean = false;
  rechargeTag: any = null;
  rechargeAmount: number | null = null;
  rechargeLoading: boolean = false;
  rechargeMessage: string = '';
  rechargeSuccess: boolean = false;

  // Close
  closeLoading: boolean = false;
  closeMessage: string = '';

  // Transactions
  showTransactions: boolean = false;
  transactionTag: any = null;
  transactions: any[] = [];
  transactionsLoading: boolean = false;

  // Alert
  alertMessage: string = '';
  alertType: string = 'success';
  showAlert: boolean = false;

  // Linked Account
  linkedAccounts: any[] = [];
  showLinkAccountModal: boolean = false;
  linkAccountNumber: string = '';
  linkOtpSent: boolean = false;
  linkOtp: string = '';
  linkLoading: boolean = false;
  linkMessage: string = '';
  linkSuccess: boolean = false;
  linkAccountDetails: any = null;

  vehicleTypes = ['Car', 'Bike', 'Bus', 'Truck', 'Van', 'Auto', 'Tractor', 'Other'];

  Math = Math;

  constructor(
    private router: Router,
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (!this.isBrowser) return;

    const fastagUser = sessionStorage.getItem('fastagUser');
    if (!fastagUser) {
      this.router.navigate(['/website/fasttag-login']);
      return;
    }

    this.user = JSON.parse(fastagUser);
    this.fetchDetails();
    this.fetchLinkedAccounts();
  }

  fetchDetails() {
    if (!this.user?.gmailId) {
      this.loading = false;
      return;
    }

    this.loading = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/fastag/user-details/${encodeURIComponent(this.user.gmailId)}`)
      .subscribe({
        next: (res) => {
          this.loading = false;
          if (res.success) {
            this.fasttags = res.fasttags || [];
            this.categorizeTags();
          }
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  categorizeTags() {
    this.activeTags = this.fasttags.filter(t => t.status !== 'Closed');
    this.closedTags = this.fasttags.filter(t => t.status === 'Closed');
  }

  get displayTags(): any[] {
    if (this.activeTab === 'active') return this.activeTags;
    if (this.activeTab === 'closed') return this.closedTags;
    return this.fasttags;
  }

  // View full details
  viewDetails(tag: any) {
    this.selectedTag = tag;
    this.showDetailModal = true;
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedTag = null;
  }

  // Apply new FASTag
  openApplyForm() {
    this.showApplyForm = true;
    this.applyMessage = '';
    this.applyForm = {
      userName: '',
      vehicleNumber: '',
      vehicleType: 'Car',
      chassisNumber: '',
      engineNumber: '',
      mobileNumber: ''
    };
  }

  closeApplyForm() {
    this.showApplyForm = false;
    this.applyMessage = '';
  }

  submitApply() {
    if (!this.applyForm.vehicleNumber.trim()) {
      this.applyMessage = 'Vehicle number is required.';
      this.applySuccess = false;
      return;
    }

    this.applyLoading = true;
    this.applyMessage = '';

    const body = {
      gmailId: this.user.gmailId,
      userName: this.applyForm.userName,
      vehicleNumber: this.applyForm.vehicleNumber,
      vehicleType: this.applyForm.vehicleType,
      chassisNumber: this.applyForm.chassisNumber,
      engineNumber: this.applyForm.engineNumber,
      mobileNumber: this.applyForm.mobileNumber
    };

    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/apply`, body)
      .subscribe({
        next: (res) => {
          this.applyLoading = false;
          this.applyMessage = res.message;
          this.applySuccess = res.success;
          if (res.success) {
            this.showGlobalAlert('FASTag application submitted successfully!', 'success');
            setTimeout(() => {
              this.closeApplyForm();
              this.fetchDetails();
            }, 1500);
          }
        },
        error: (err) => {
          this.applyLoading = false;
          this.applyMessage = err.error?.message || 'Failed to apply for FASTag.';
          this.applySuccess = false;
        }
      });
  }

  // Recharge
  openRecharge(tag: any) {
    if (this.linkedAccounts.length === 0) {
      this.showGlobalAlert('Please link a bank account first before recharging.', 'error');
      this.openLinkAccountModal();
      return;
    }
    this.rechargeTag = tag;
    this.rechargeAmount = null;
    this.rechargeMessage = '';
    this.rechargeSuccess = false;
    this.showRechargeModal = true;
  }

  closeRechargeModal() {
    this.showRechargeModal = false;
    this.rechargeTag = null;
    this.rechargeAmount = null;
    this.rechargeMessage = '';
  }

  submitRecharge() {
    if (!this.rechargeAmount || this.rechargeAmount < 100 || this.rechargeAmount > 50000) {
      this.rechargeMessage = 'Amount must be between ₹100 and ₹50,000.';
      this.rechargeSuccess = false;
      return;
    }

    if (this.linkedAccounts.length === 0) {
      this.rechargeMessage = 'Please link a bank account first.';
      this.rechargeSuccess = false;
      return;
    }

    this.rechargeLoading = true;
    this.rechargeMessage = '';

    const body = {
      fasttagNumber: this.rechargeTag.fasttagNumber,
      amount: this.rechargeAmount,
      gmailId: this.user.gmailId,
      accountNumber: this.linkedAccounts[0].accountNumber
    };

    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/recharge`, body)
      .subscribe({
        next: (res) => {
          this.rechargeLoading = false;
          this.rechargeMessage = res.message;
          this.rechargeSuccess = res.success;
          if (res.success) {
            this.showGlobalAlert('Recharge successful! ₹' + this.rechargeAmount?.toFixed(2) + ' added.', 'success');
            setTimeout(() => {
              this.closeRechargeModal();
              this.fetchDetails();
            }, 1500);
          }
        },
        error: (err) => {
          this.rechargeLoading = false;
          this.rechargeMessage = err.error?.message || 'Recharge failed. Try again.';
          this.rechargeSuccess = false;
        }
      });
  }

  // Close FASTag
  closeFasttag(tag: any) {
    if (tag.balance > 0) {
      this.showGlobalAlert('Cannot close FASTag with balance ₹' + tag.balance.toFixed(2) + '. Balance must be ₹0.00 to close.', 'error');
      return;
    }

    if (!confirm('Are you sure you want to close this FASTag (' + tag.fasttagNumber + ')? This action cannot be undone.')) {
      return;
    }

    this.closeLoading = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/close/${tag.id}`, {})
      .subscribe({
        next: (res) => {
          this.closeLoading = false;
          if (res.success) {
            this.showGlobalAlert('FASTag closed successfully.', 'success');
            this.fetchDetails();
          } else {
            this.showGlobalAlert(res.message || 'Failed to close FASTag.', 'error');
          }
        },
        error: (err) => {
          this.closeLoading = false;
          this.showGlobalAlert(err.error?.message || 'Failed to close FASTag.', 'error');
        }
      });
  }

  // Transactions
  viewTransactions(tag: any) {
    this.transactionTag = tag;
    this.transactions = [];
    this.showTransactions = true;
    this.transactionsLoading = true;

    this.http.get<any>(`${environment.apiBaseUrl}/api/fastag/transactions/${tag.id}`)
      .subscribe({
        next: (res) => {
          this.transactionsLoading = false;
          if (res.success) {
            this.transactions = res.transactions || [];
          }
        },
        error: () => {
          this.transactionsLoading = false;
        }
      });
  }

  closeTransactions() {
    this.showTransactions = false;
    this.transactionTag = null;
    this.transactions = [];
  }

  // Helpers
  getStatusClass(status: string): string {
    switch (status) {
      case 'Approved': return 'status-approved';
      case 'Applied': return 'status-applied';
      case 'Rejected': return 'status-rejected';
      case 'Closed': return 'status-closed';
      default: return '';
    }
  }

  showGlobalAlert(message: string, type: string) {
    this.alertMessage = message;
    this.alertType = type;
    this.showAlert = true;
    setTimeout(() => { this.showAlert = false; }, 4000);
  }

  formatDate(date: any): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatDateTime(date: any): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  getTotalBalance(): string {
    const total = this.activeTags.reduce((sum, t) => sum + (t.balance || 0), 0);
    return total.toFixed(2);
  }

  logout() {
    if (this.isBrowser) {
      sessionStorage.removeItem('fastagUser');
      sessionStorage.removeItem('fastagDetails');
    }
    this.router.navigate(['/website/fasttag-login']);
  }

  goToLanding() {
    this.router.navigate(['/website/landing']);
  }

  // ===== Linked Account Methods =====
  fetchLinkedAccounts() {
    if (!this.user?.gmailId) return;
    this.http.get<any>(`${environment.apiBaseUrl}/api/fastag/linked-accounts/${encodeURIComponent(this.user.gmailId)}`)
      .subscribe({
        next: (res) => {
          if (res.success) {
            this.linkedAccounts = res.linkedAccounts || [];
          }
        },
        error: () => {}
      });
  }

  openLinkAccountModal() {
    this.showLinkAccountModal = true;
    this.linkAccountNumber = '';
    this.linkOtpSent = false;
    this.linkOtp = '';
    this.linkMessage = '';
    this.linkSuccess = false;
    this.linkAccountDetails = null;
  }

  closeLinkAccountModal() {
    this.showLinkAccountModal = false;
    this.linkAccountNumber = '';
    this.linkOtpSent = false;
    this.linkOtp = '';
    this.linkMessage = '';
    this.linkAccountDetails = null;
  }

  sendLinkOtp() {
    const accountNumber = this.linkAccountNumber.trim();
    const gmailId = this.user?.gmailId?.trim();

    console.log('[FASTag] sendLinkOtp clicked', {
      hasAccountNumber: !!accountNumber,
      hasGmailId: !!gmailId,
      apiBaseUrl: environment.apiBaseUrl
    });

    if (!accountNumber) {
      this.linkMessage = 'Please enter your account number.';
      this.linkSuccess = false;
      return;
    }
    if (!gmailId) {
      this.linkMessage = 'Session expired. Please login again and retry.';
      this.linkSuccess = false;
      this.linkLoading = false;
      return;
    }

    this.linkLoading = true;
    this.linkMessage = '';

    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/link-account`, {
      gmailId,
      accountNumber
    }).subscribe({
      next: (res) => {
        console.log('[FASTag] sendLinkOtp response', res);
        this.linkLoading = false;
        if (res.success) {
          this.linkOtpSent = true;
          this.linkAccountDetails = {
            accountHolderName: res.accountHolderName,
            maskedBalance: res.maskedBalance
          };
          this.linkMessage = res.message;
          this.linkSuccess = true;
        } else {
          this.linkMessage = res.message;
          this.linkSuccess = false;
        }
      },
      error: (err) => {
        console.error('[FASTag] sendLinkOtp error', err);
        this.linkLoading = false;
        this.linkMessage = err.error?.message || 'Failed to verify account. Try again.';
        this.linkSuccess = false;
      }
    });
  }

  verifyLinkOtp() {
    const accountNumber = this.linkAccountNumber.trim();
    const gmailId = this.user?.gmailId?.trim();
    const otp = this.linkOtp.trim();

    console.log('[FASTag] verifyLinkOtp clicked', {
      hasAccountNumber: !!accountNumber,
      hasGmailId: !!gmailId,
      otpLength: otp.length
    });

    if (!otp || otp.length !== 6) {
      this.linkMessage = 'Please enter a valid 6-digit OTP.';
      this.linkSuccess = false;
      return;
    }
    if (!accountNumber || !gmailId) {
      this.linkMessage = 'Session expired. Please login again and retry.';
      this.linkSuccess = false;
      this.linkLoading = false;
      return;
    }

    this.linkLoading = true;
    this.linkMessage = '';

    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/verify-link-account`, {
      gmailId,
      accountNumber,
      otp
    }).subscribe({
      next: (res) => {
        console.log('[FASTag] verifyLinkOtp response', res);
        this.linkLoading = false;
        if (res.success) {
          this.linkMessage = res.message;
          this.linkSuccess = true;
          this.showGlobalAlert('Bank account linked successfully!', 'success');
          setTimeout(() => {
            this.closeLinkAccountModal();
            this.fetchLinkedAccounts();
          }, 1500);
        } else {
          this.linkMessage = res.message;
          this.linkSuccess = false;
        }
      },
      error: (err) => {
        console.error('[FASTag] verifyLinkOtp error', err);
        this.linkLoading = false;
        this.linkMessage = err.error?.message || 'Verification failed. Try again.';
        this.linkSuccess = false;
      }
    });
  }

  unlinkAccount(account: any) {
    if (!confirm('Are you sure you want to unlink account ' + account.accountNumber + '?')) return;

    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/unlink-account`, {
      gmailId: this.user.gmailId,
      accountNumber: account.accountNumber
    }).subscribe({
      next: (res) => {
        if (res.success) {
          this.showGlobalAlert('Account unlinked successfully.', 'success');
          this.fetchLinkedAccounts();
        } else {
          this.showGlobalAlert(res.message || 'Failed to unlink.', 'error');
        }
      },
      error: (err) => {
        this.showGlobalAlert(err.error?.message || 'Failed to unlink account.', 'error');
      }
    });
  }

  getMaskedAccountNumber(accNum: string): string {
    if (!accNum || accNum.length < 4) return accNum || '';
    return 'XXXX' + accNum.slice(-4);
  }
}
