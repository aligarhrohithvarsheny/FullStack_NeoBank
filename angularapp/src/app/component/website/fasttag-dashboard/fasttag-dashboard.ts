import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
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
export class FasttagDashboard implements OnInit, OnDestroy {
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
  linkedAccountRefreshRef: any = null;
  showLinkAccountModal: boolean = false;
  linkAccountNumber: string = '';
  linkCustomerId: string = '';
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
    this.startLinkedAccountRealtimeRefresh();
  }

  ngOnDestroy() {
    this.stopLinkedAccountRealtimeRefresh();
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

    // Always refresh just before recharge so the latest balance is shown.
    this.fetchLinkedAccounts();

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
            if (this.linkedAccounts.length > 0 && typeof res.accountBalance === 'number') {
              this.linkedAccounts[0].availableBalance = res.accountBalance;
            }
            this.showGlobalAlert('Recharge successful! ₹' + this.rechargeAmount?.toFixed(2) + ' added.', 'success');
            setTimeout(() => {
              this.closeRechargeModal();
              this.fetchDetails();
              this.fetchLinkedAccounts();
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

  downloadSticker(tag: any) {
    if (!tag || tag.status !== 'Approved') {
      this.showGlobalAlert('FASTag download is available only for approved tags.', 'error');
      return;
    }
    if (!this.user?.gmailId) {
      this.showGlobalAlert('Session expired. Please login again.', 'error');
      return;
    }

    const url = `${environment.apiBaseUrl}/api/fastag/sticker/${tag.id}?gmailId=${encodeURIComponent(this.user.gmailId)}`;
    if (this.isBrowser) {
      const link = document.createElement('a');
      link.href = url;
      link.target = '_blank';
      link.rel = 'noopener';
      link.click();
    }
  }

  closeTransactions() {
    this.showTransactions = false;
    this.transactionTag = null;
    this.transactions = [];
    this.resetTxnFilters();
  }

  // Statement filters
  txnFilterType: string = 'ALL';
  txnFromDate: string = '';
  txnToDate: string = '';
  txnMinAmount: number | null = null;
  txnMaxAmount: number | null = null;

  resetTxnFilters() {
    this.txnFilterType = 'ALL';
    this.txnFromDate = '';
    this.txnToDate = '';
    this.txnMinAmount = null;
    this.txnMaxAmount = null;
  }

  isCreditTxn(txn: any): boolean {
    const t = String(txn.type || txn.transactionType || '').toUpperCase();
    return t === 'CREDIT' || t === 'RECHARGE' || t === 'REFUND';
  }

  getTxnReference(txn: any): string {
    const seq = txn.globalTransactionSequence ?? txn.id;
    const d = txn.createdAt || txn.transactionDate;
    const dt = d ? new Date(d) : null;
    const stamp = dt && !isNaN(dt.getTime())
      ? `${dt.getFullYear()}${String(dt.getMonth() + 1).padStart(2, '0')}${String(dt.getDate()).padStart(2, '0')}`
      : '00000000';
    return `FTX${stamp}${String(seq ?? 0).padStart(6, '0')}`;
  }

  get filteredTransactions(): any[] {
    const from = this.txnFromDate ? new Date(this.txnFromDate + 'T00:00:00') : null;
    const to = this.txnToDate ? new Date(this.txnToDate + 'T23:59:59.999') : null;
    return this.transactions.filter(txn => {
      const type = String(txn.type || txn.transactionType || '').toUpperCase();
      if (this.txnFilterType === 'CREDIT' && !this.isCreditTxn(txn)) return false;
      if (this.txnFilterType === 'DEBIT' && this.isCreditTxn(txn)) return false;
      if (this.txnFilterType !== 'ALL' && this.txnFilterType !== 'CREDIT' && this.txnFilterType !== 'DEBIT' && type !== this.txnFilterType) return false;
      const d = new Date(txn.createdAt || txn.transactionDate);
      if (from && d < from) return false;
      if (to && d > to) return false;
      const amt = Number(txn.amount) || 0;
      if (this.txnMinAmount !== null && this.txnMinAmount !== undefined && amt < this.txnMinAmount) return false;
      if (this.txnMaxAmount !== null && this.txnMaxAmount !== undefined && amt > this.txnMaxAmount) return false;
      return true;
    });
  }

  formatTxnDate(date: any): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatTxnTime(date: any): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  generateStatement() {
    const tag = this.transactionTag;
    const rows = this.filteredTransactions;
    if (!tag) return;
    if (rows.length === 0) {
      this.showGlobalAlert('No transactions match the selected filters.', 'error');
      return;
    }
    const w = window.open('', '_blank', 'width=900,height=900');
    if (!w) {
      this.showGlobalAlert('Please allow popups to generate the statement.', 'error');
      return;
    }
    const esc = (v: any) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    const m = (v: any) => (Number(v) || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const credits = rows.filter(t => this.isCreditTxn(t)).reduce((s, t) => s + (Number(t.amount) || 0), 0);
    const debits = rows.filter(t => !this.isCreditTxn(t)).reduce((s, t) => s + (Number(t.amount) || 0), 0);
    const period = (this.txnFromDate || this.txnToDate)
      ? `${this.txnFromDate || 'Beginning'} to ${this.txnToDate || 'Today'}` : 'All transactions';
    const body = rows.map(t => {
      const c = this.isCreditTxn(t);
      const d = t.createdAt || t.transactionDate;
      return `<tr><td>${esc(this.formatTxnDate(d))}</td><td>${esc(this.formatTxnTime(d))}</td><td>${esc(this.getTxnReference(t))}</td>
        <td>${esc(t.type || t.transactionType)}</td><td class="r">₹${m(t.previousBalance)}</td>
        <td class="r ${c ? 'cr' : 'dr'}">${c ? '+' : '-'}₹${m(t.amount)}</td><td class="r">₹${m(t.newBalance)}</td></tr>`;
    }).join('');

    w.document.write(`<!DOCTYPE html><html><head><title>NeoBank FASTag Statement - ${esc(tag.fasttagNumber)}</title><style>
      * { margin:0; padding:0; box-sizing:border-box; }
      body { font-family:'Segoe UI',Arial,sans-serif; padding:30px; color:#1a1a2e; }
      .hdr { display:flex; justify-content:space-between; align-items:center; border-bottom:3px solid #e63946; padding-bottom:12px; margin-bottom:16px; }
      .hdr h2 { color:#0056b3; } .hdr p { font-size:12px; color:#666; }
      h3 { text-align:center; color:#e63946; margin:12px 0; }
      .info { display:grid; grid-template-columns:1fr 1fr; gap:6px 20px; font-size:13px; margin-bottom:14px; }
      table { width:100%; border-collapse:collapse; font-size:12px; }
      th,td { border:1px solid #ccc; padding:7px 8px; text-align:left; }
      th { background:#f0f4f8; } .r { text-align:right; } .cr { color:#16a34a; font-weight:600; } .dr { color:#e63946; font-weight:600; }
      .sum { margin-top:14px; font-size:13px; display:flex; gap:24px; justify-content:flex-end; }
      .foot { margin-top:20px; font-size:11px; color:#777; border-top:2px solid #e63946; padding-top:8px; }
      @media print { body { padding:12px; } }
    </style></head><body>
      <div class="hdr"><div><h2>NeoBank</h2><p>Digital Banking Solutions</p></div><p>...the digital bank you can trust!</p></div>
      <h3>FASTag Account Statement</h3>
      <div class="info">
        <div><strong>FASTag No:</strong> ${esc(tag.fasttagNumber)}</div><div><strong>Vehicle No:</strong> ${esc(tag.vehicleNumber)}</div>
        <div><strong>Customer:</strong> ${esc(tag.userName || this.user?.name || '')}</div><div><strong>Period:</strong> ${esc(period)}</div>
        <div><strong>Type Filter:</strong> ${esc(this.txnFilterType)}</div><div><strong>Generated:</strong> ${new Date().toLocaleString('en-IN')}</div>
      </div>
      <table><thead><tr><th>Date</th><th>Time</th><th>Reference No</th><th>Type</th><th class="r">Previous Balance</th><th class="r">Amount</th><th class="r">Closing Balance</th></tr></thead>
      <tbody>${body}</tbody></table>
      <div class="sum"><span>Total Credits: <strong class="cr">₹${m(credits)}</strong></span><span>Total Debits: <strong class="dr">₹${m(debits)}</strong></span><span>Transactions: <strong>${rows.length}</strong></span></div>
      <p class="foot">This is a computer generated statement from NeoBank Digital Banking and does not require a signature.</p>
      <script>window.onload=function(){window.print();}</script></body></html>`);
    w.document.close();
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
            if ((!this.user?.userName || String(this.user.userName).trim() === '') && this.linkedAccounts.length > 0) {
              this.user = {
                ...this.user,
                userName: this.linkedAccounts[0].accountHolderName || this.user?.userName
              };
            }
          }
        },
        error: () => {}
      });
  }

  startLinkedAccountRealtimeRefresh() {
    this.stopLinkedAccountRealtimeRefresh();
    this.linkedAccountRefreshRef = setInterval(() => {
      this.fetchLinkedAccounts();
    }, 15000);
  }

  stopLinkedAccountRealtimeRefresh() {
    if (this.linkedAccountRefreshRef) {
      clearInterval(this.linkedAccountRefreshRef);
      this.linkedAccountRefreshRef = null;
    }
  }

  getPrimaryLinkedAccount(): any | null {
    return this.linkedAccounts.length > 0 ? this.linkedAccounts[0] : null;
  }

  getDisplayUserName(): string {
    const fromUser = this.user?.userName;
    if (fromUser && String(fromUser).trim()) {
      return String(fromUser).trim();
    }
    const primary = this.getPrimaryLinkedAccount();
    if (primary?.accountHolderName) {
      return primary.accountHolderName;
    }
    const gmail = this.user?.gmailId;
    if (gmail && String(gmail).includes('@')) {
      return String(gmail).split('@')[0];
    }
    return 'Customer';
  }

  formatAvailableBalance(value: any): string {
    const balance = Number(value);
    if (!Number.isFinite(balance)) return '0.00';
    return balance.toFixed(2);
  }

  openLinkAccountModal() {
    this.showLinkAccountModal = true;
    this.linkAccountNumber = '';
    this.linkOtpSent = false;
    this.linkOtp = '';
    this.linkCustomerId = '';
    this.linkMessage = '';
    this.linkSuccess = false;
    this.linkAccountDetails = null;
  }

  closeLinkAccountModal() {
    this.showLinkAccountModal = false;
    this.linkAccountNumber = '';
    this.linkOtpSent = false;
    this.linkOtp = '';
    this.linkCustomerId = '';
    this.linkMessage = '';
    this.linkAccountDetails = null;
  }

  sendLinkOtp() {
    const accountNumber = this.linkAccountNumber.trim();
    const gmailId = this.user?.gmailId?.trim();
    const customerId = this.linkCustomerId.trim();

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
    if (!customerId) {
      this.linkMessage = 'Please enter your customer ID.';
      this.linkSuccess = false;
      this.linkLoading = false;
      return;
    }

    this.linkLoading = true;
    this.linkMessage = '';

    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/link-account`, {
      gmailId,
      customerId,
      accountNumber
    }).subscribe({
      next: (res) => {
        console.log('[FASTag] sendLinkOtp response', res);
        this.linkLoading = false;
        if (res.success) {
          this.linkOtpSent = false;
          this.linkAccountDetails = {
            accountHolderName: res.accountHolderName,
            maskedBalance: res.maskedBalance
          };
          this.linkMessage = res.message || 'Bank account linked successfully.';
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
