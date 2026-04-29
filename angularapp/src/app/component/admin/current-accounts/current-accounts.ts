import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CurrentAccountService } from '../../../service/current-account.service';
import { SoundboxService } from '../../../service/soundbox.service';
import { CurrentAccount, BusinessTransaction, TransactionSummary, CurrentAccountEditHistory, LinkedAccount, SavingsAccountDetails } from '../../../model/current-account/current-account.model';
import { SoundboxDevice, SoundboxRequest, SoundboxTransaction, AdminSoundboxStats } from '../../../model/soundbox/soundbox.model';
import { UpiService } from '../../../service/upi.service';
import { UpiPayment, AdminUpiStats } from '../../../model/upi/upi.model';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-current-accounts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './current-accounts.html',
  styleUrls: ['./current-accounts.css']
})
export class CurrentAccounts implements OnInit {

  // Section control
  activeTab: string = 'open-account';

  // Admin info
  adminName: string = '';
  adminEmail: string = '';

  // Statistics
  stats = {
    totalAccounts: 0,
    pendingAccounts: 0,
    activeAccounts: 0,
    frozenAccounts: 0,
    totalActiveBalance: 0,
    overdrawnAccounts: 0
  };

  // Account lists
  allAccounts: CurrentAccount[] = [];
  filteredAccounts: CurrentAccount[] = [];
  pendingAccounts: CurrentAccount[] = [];
  searchTerm: string = '';
  statusFilter: string = 'ALL';

  // Open Account Form
  newAccount: CurrentAccount = this.getEmptyAccount();

  // Selected account for detail view / actions
  selectedAccount: CurrentAccount | null = null;
  selectedAccountTransactions: BusinessTransaction[] = [];
  transactionSummary: TransactionSummary | null = null;

  // KYC Verification
  pendingKycAccounts: CurrentAccount[] = [];

  // Cheque Requests
  pendingChequeRequests: any[] = [];

  // Transaction Monitoring
  monitorSearchTerm: string = '';
  allTransactions: BusinessTransaction[] = [];
  filteredTransactions: BusinessTransaction[] = [];

  // Freeze modal
  showFreezeModal: boolean = false;
  freezeReason: string = '';
  freezeAccountId: number | null = null;

  // Overdraft modal
  showOverdraftModal: boolean = false;
  overdraftLimit: number = 0;
  overdraftAccountId: number | null = null;

  // Transaction processing modal
  showTransactionModal: boolean = false;
  txnAccountNumber: string = '';
  txnType: string = 'Credit';
  txnAmount: number = 0;
  txnDescription: string = '';

  // Transfer modal
  showTransferModal: boolean = false;
  transferFromAccount: string = '';
  transferToAccount: string = '';
  transferRecipientName: string = '';
  transferAmount: number = 0;
  transferType: string = 'NEFT';
  transferDescription: string = '';

  // Alert
  alertMessage: string = '';
  alertType: string = 'success';
  showAlert: boolean = false;

  // Detail view
  showDetailView: boolean = false;

  // Edit mode
  isEditMode: boolean = false;
  editAccount: CurrentAccount = this.getEmptyAccount();
  editDocumentFile: File | null = null;
  editDocumentFileName: string = '';
  showEditDocumentModal: boolean = false;
  editHistory: CurrentAccountEditHistory[] = [];
  showEditHistory: boolean = false;

  // Loading
  isLoading: boolean = false;

  // Link Savings Account
  showLinkModal: boolean = false;
  linkCurrentAccountNumber: string = '';
  linkSavingsAccountNumber: string = '';
  linkSavingsCustomerId: string = '';
  linkVerified: boolean = false;
  linkVerifiedName: string = '';
  linkVerifiedBalance: number = 0;

  // Linked accounts for detail view
  linkedAccounts: LinkedAccount[] = [];
  linkedSavingsDetails: { [key: string]: any } = {};
  accountLinkStatus: { [accountNumber: string]: boolean } = {};

  // Account Switch
  activeAccountType: string = 'current'; // 'current' or 'savings'
  switchedSavingsAccount: any = null;
  switchedSavingsAccountNumber: string = '';

  // Soundbox Management
  soundboxStats: AdminSoundboxStats | null = null;
  soundboxRequests: SoundboxRequest[] = [];
  soundboxDevices: SoundboxDevice[] = [];
  pendingSoundboxRequests: SoundboxRequest[] = [];
  sbApproveDeviceId: string = '';
  sbApproveMonthlyCharge: number = 100;
  sbApproveDeviceCharge: number = 499;
  sbRejectRemarks: string = '';
  showSbApproveModal: boolean = false;
  showSbRejectModal: boolean = false;
  selectedSbRequest: SoundboxRequest | null = null;
  sbDeviceFilter: string = 'ALL';
  soundboxTxnAccountNumber: string = '';
  soundboxTransactions: SoundboxTransaction[] = [];
  showSbChargesModal: boolean = false;
  selectedSbDevice: SoundboxDevice | null = null;
  sbNewMonthlyCharge: number = 100;
  sbNewDeviceCharge: number = 499;

  // UPI Management
  adminUpiStats: AdminUpiStats | null = null;
  recentUpiPayments: UpiPayment[] = [];
  filteredUpiPayments: UpiPayment[] = [];
  upiSearchTerm: string = '';
  isLoadingUpiData: boolean = false;
  upiAccountSearch: string = '';
  upiAccountPayments: UpiPayment[] = [];

  // Transaction ID Search
  upiTxnSearch: string = '';
  isSearchingTxn: boolean = false;
  txnSearchDone: boolean = false;
  txnSearchResult: any = null;

  // QR Code Generation (Soundbox Tab)
  qrAccountNumber: string = '';
  qrUpiId: string = '';
  qrAmount: number | null = null;
  qrIsCustomAmount: boolean = false;
  qrVerifiedName: string = '';
  qrVerificationStatus: string = ''; // '', 'verifying', 'verified', 'failed', 'mismatch'
  qrVerificationError: string = '';
  qrGeneratedImage: string = '';
  qrGeneratedUpiId: string = '';
  qrGeneratedBusinessName: string = '';
  qrGeneratedAmount: number | null = null;
  isGeneratingQr: boolean = false;
  showQrResultModal: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private currentAccountService: CurrentAccountService,
    private soundboxService: SoundboxService,
    private upiService: UpiService
  ) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;

    try {
      const adminData = sessionStorage.getItem('admin');
      if (adminData) {
        const admin = JSON.parse(adminData);
        this.adminName = admin.name || admin.email || 'Manager';
        this.adminEmail = admin.email || '';
      }
    } catch (e) {
      console.warn('Could not read admin data', e);
    }

    this.loadAllData();
  }

  // ==================== Data Loading ====================

  loadAllData() {
    this.loadStatistics();
    this.loadAccounts();
    this.loadPendingChequeRequests();
  }

  loadStatistics() {
    this.currentAccountService.getStatistics().subscribe({
      next: (data: any) => {
        this.stats = {
          totalAccounts: data.totalAccounts || 0,
          pendingAccounts: data.pendingAccounts || 0,
          activeAccounts: data.activeAccounts || 0,
          frozenAccounts: data.frozenAccounts || 0,
          totalActiveBalance: data.totalActiveBalance || 0,
          overdrawnAccounts: data.overdrawnAccounts || 0
        };
      },
      error: (err) => console.error('Error loading statistics:', err)
    });
  }

  loadAccounts() {
    this.isLoading = true;
    this.currentAccountService.getAllAccounts().subscribe({
      next: (accounts) => {
        this.allAccounts = accounts;
        this.filterAccounts();
        this.pendingAccounts = accounts.filter(a => a.status === 'PENDING');
        this.pendingKycAccounts = accounts.filter(a => !a.kycVerified && a.status === 'ACTIVE');
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading accounts:', err);
        this.isLoading = false;
      }
    });
  }

  // ==================== Tab / Section Management ====================

  setActiveTab(tab: string) {
    this.activeTab = tab;
    this.showDetailView = false;
    this.selectedAccount = null;

    if (tab === 'view-accounts') {
      this.loadAccounts();
    } else if (tab === 'approve-accounts') {
      this.loadAccounts();
    } else if (tab === 'kyc-verification') {
      this.loadAccounts();
    } else if (tab === 'transaction-monitoring') {
      this.loadAllTransactionsForMonitoring();
    } else if (tab === 'cheque-requests') {
      this.loadPendingChequeRequests();
    } else if (tab === 'link-savings') {
      this.loadAccounts();
      this.loadAccountLinkStatuses();
    } else if (tab === 'soundbox') {
      this.loadSoundboxData();
    } else if (tab === 'upi-payments') {
      this.loadUpiData();
    }
  }

  // ==================== Filter ====================

  filterAccounts() {
    let filtered = [...this.allAccounts];

    if (this.statusFilter !== 'ALL') {
      filtered = filtered.filter(a => a.status === this.statusFilter);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(a =>
        (a.businessName?.toLowerCase().includes(term)) ||
        (a.ownerName?.toLowerCase().includes(term)) ||
        (a.accountNumber?.toLowerCase().includes(term)) ||
        (a.customerId?.toLowerCase().includes(term)) ||
        (a.gstNumber?.toLowerCase().includes(term)) ||
        (a.mobile?.includes(term))
      );
    }

    this.filteredAccounts = filtered;
  }

  // ==================== Open Account ====================

  getEmptyAccount(): CurrentAccount {
    return {
      businessName: '',
      businessType: 'Proprietor',
      businessRegistrationNumber: '',
      gstNumber: '',
      ownerName: '',
      mobile: '',
      email: '',
      aadharNumber: '',
      panNumber: '',
      shopAddress: '',
      city: '',
      state: '',
      pincode: '',
      branchName: 'NeoBank Main Branch',
      ifscCode: 'EZYV000123'
    };
  }

  submitNewAccount() {
    if (!this.validateNewAccount()) return;

    this.isLoading = true;
    this.currentAccountService.createAccount(this.newAccount).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Current Account created successfully! Account Number: ' + response.account.accountNumber + ', Customer ID: ' + response.account.customerId, 'success');
          this.newAccount = this.getEmptyAccount();
          this.loadAllData();
        } else {
          this.showAlertMessage(response.error || 'Failed to create account', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Failed to create account', 'error');
        this.isLoading = false;
      }
    });
  }

  validateNewAccount(): boolean {
    if (!this.newAccount.businessName?.trim()) {
      this.showAlertMessage('Business Name is required', 'error');
      return false;
    }
    if (!this.newAccount.ownerName?.trim()) {
      this.showAlertMessage('Owner Name is required', 'error');
      return false;
    }
    if (!this.newAccount.mobile?.trim() || this.newAccount.mobile.length !== 10) {
      this.showAlertMessage('Valid 10-digit Mobile Number is required', 'error');
      return false;
    }
    if (!this.newAccount.email?.trim()) {
      this.showAlertMessage('Email ID is required', 'error');
      return false;
    }
    if (!this.newAccount.aadharNumber?.trim() || this.newAccount.aadharNumber.length !== 12) {
      this.showAlertMessage('Valid 12-digit Aadhaar Number is required', 'error');
      return false;
    }
    if (!this.newAccount.panNumber?.trim() || this.newAccount.panNumber.length !== 10) {
      this.showAlertMessage('Valid 10-character PAN Number is required', 'error');
      return false;
    }
    return true;
  }

  // ==================== Account Actions ====================

  viewAccountDetails(account: CurrentAccount) {
    this.selectedAccount = account;
    this.showDetailView = true;
    this.isEditMode = false;
    this.showEditHistory = false;
    this.editHistory = [];
    this.activeAccountType = 'current';
    this.switchedSavingsAccount = null;
    this.switchedSavingsAccountNumber = '';
    if (account.accountNumber) {
      this.loadAccountTransactions(account.accountNumber);
      this.loadLinkedAccounts(account.accountNumber);
    }
    if (account.id) {
      this.currentAccountService.getEditHistory(account.id).subscribe({
        next: (history) => this.editHistory = history,
        error: (err) => console.error('Error loading edit history:', err)
      });
    }
  }

  loadAccountTransactions(accountNumber: string) {
    this.currentAccountService.getRecentTransactions(accountNumber, 10).subscribe({
      next: (txns) => this.selectedAccountTransactions = txns,
      error: (err) => console.error('Error loading transactions:', err)
    });

    this.currentAccountService.getTransactionSummary(accountNumber).subscribe({
      next: (summary) => this.transactionSummary = summary,
      error: (err) => console.error('Error loading summary:', err)
    });
  }

  closeDetailView() {
    this.showDetailView = false;
    this.selectedAccount = null;
    this.selectedAccountTransactions = [];
    this.transactionSummary = null;
    this.isEditMode = false;
    this.showEditHistory = false;
    this.editHistory = [];
    this.editDocumentFile = null;
    this.editDocumentFileName = '';
  }

  approveAccount(account: CurrentAccount) {
    if (!account.id) return;
    this.currentAccountService.approveAccount(account.id, this.adminName).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Account approved successfully!', 'success');
          this.loadAllData();
        } else {
          this.showAlertMessage(response.error || 'Approval failed', 'error');
        }
      },
      error: (err) => this.showAlertMessage('Approval failed', 'error')
    });
  }

  rejectAccount(account: CurrentAccount) {
    if (!account.id) return;
    this.currentAccountService.rejectAccount(account.id).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Account rejected', 'success');
          this.loadAllData();
        }
      },
      error: (err) => this.showAlertMessage('Rejection failed', 'error')
    });
  }

  verifyKyc(account: CurrentAccount) {
    if (!account.id) return;
    this.currentAccountService.verifyKyc(account.id, this.adminName).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('KYC verified successfully!', 'success');
          this.loadAllData();
        }
      },
      error: (err) => this.showAlertMessage('KYC verification failed', 'error')
    });
  }

  // ==================== Freeze Account ====================

  openFreezeModal(account: CurrentAccount) {
    if (!account.id) return;
    this.freezeAccountId = account.id;
    this.freezeReason = '';
    this.showFreezeModal = true;
  }

  closeFreezeModal() {
    this.showFreezeModal = false;
    this.freezeReason = '';
    this.freezeAccountId = null;
  }

  confirmFreezeAccount() {
    if (!this.freezeAccountId || !this.freezeReason.trim()) {
      this.showAlertMessage('Please provide a reason for freezing', 'error');
      return;
    }
    this.currentAccountService.freezeAccount(this.freezeAccountId, this.freezeReason, this.adminName).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Account frozen successfully', 'success');
          this.closeFreezeModal();
          this.loadAllData();
        }
      },
      error: (err) => this.showAlertMessage('Failed to freeze account', 'error')
    });
  }

  unfreezeAccount(account: CurrentAccount) {
    if (!account.id) return;
    this.currentAccountService.unfreezeAccount(account.id).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Account unfrozen successfully', 'success');
          this.loadAllData();
        }
      },
      error: (err) => this.showAlertMessage('Failed to unfreeze account', 'error')
    });
  }

  // ==================== Overdraft ====================

  openOverdraftModal(account: CurrentAccount) {
    if (!account.id) return;
    this.overdraftAccountId = account.id;
    this.overdraftLimit = 50000;
    this.showOverdraftModal = true;
  }

  closeOverdraftModal() {
    this.showOverdraftModal = false;
    this.overdraftLimit = 0;
    this.overdraftAccountId = null;
  }

  confirmOverdraft() {
    if (!this.overdraftAccountId || this.overdraftLimit <= 0) {
      this.showAlertMessage('Please provide a valid overdraft limit', 'error');
      return;
    }
    this.currentAccountService.approveOverdraft(this.overdraftAccountId, this.overdraftLimit).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Overdraft approved: ₹' + this.overdraftLimit, 'success');
          this.closeOverdraftModal();
          this.loadAllData();
        }
      },
      error: (err) => this.showAlertMessage('Failed to approve overdraft', 'error')
    });
  }

  // ==================== Transaction Processing ====================

  openTransactionModal(account: CurrentAccount) {
    this.txnAccountNumber = account.accountNumber || '';
    this.txnType = 'Credit';
    this.txnAmount = 0;
    this.txnDescription = '';
    this.showTransactionModal = true;
  }

  closeTransactionModal() {
    this.showTransactionModal = false;
    this.txnAccountNumber = '';
    this.txnAmount = 0;
    this.txnDescription = '';
  }

  processTransaction() {
    if (!this.txnAccountNumber || this.txnAmount <= 0) {
      this.showAlertMessage('Please fill in all transaction details', 'error');
      return;
    }
    this.currentAccountService.processTransaction(this.txnAccountNumber, this.txnType, this.txnAmount, this.txnDescription).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage(response.message || 'Transaction completed', 'success');
          this.closeTransactionModal();
          this.loadAllData();
          if (this.selectedAccount?.accountNumber === this.txnAccountNumber) {
            this.loadAccountTransactions(this.txnAccountNumber);
          }
        } else {
          this.showAlertMessage(response.error || 'Transaction failed', 'error');
        }
      },
      error: (err) => this.showAlertMessage(err.error?.error || 'Transaction failed', 'error')
    });
  }

  // ==================== Transfer ====================

  openTransferModal(account: CurrentAccount) {
    this.transferFromAccount = account.accountNumber || '';
    this.transferToAccount = '';
    this.transferRecipientName = '';
    this.transferAmount = 0;
    this.transferType = 'NEFT';
    this.transferDescription = '';
    this.showTransferModal = true;
  }

  closeTransferModal() {
    this.showTransferModal = false;
  }

  processTransfer() {
    if (!this.transferFromAccount || !this.transferToAccount || !this.transferRecipientName || this.transferAmount <= 0) {
      this.showAlertMessage('Please fill in all transfer details', 'error');
      return;
    }
    const request = {
      fromAccount: this.transferFromAccount,
      toAccount: this.transferToAccount,
      recipientName: this.transferRecipientName,
      amount: this.transferAmount,
      transferType: this.transferType,
      description: this.transferDescription
    };
    this.currentAccountService.processTransfer(request).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage(response.message || 'Transfer completed', 'success');
          this.closeTransferModal();
          this.loadAllData();
        } else {
          this.showAlertMessage(response.error || 'Transfer failed', 'error');
        }
      },
      error: (err) => this.showAlertMessage(err.error?.error || 'Transfer failed', 'error')
    });
  }

  // ==================== Transaction Monitoring ====================

  loadAllTransactionsForMonitoring() {
    this.allAccounts.forEach(account => {
      if (account.accountNumber) {
        this.currentAccountService.getRecentTransactions(account.accountNumber, 20).subscribe({
          next: (txns) => {
            this.allTransactions = [...this.allTransactions, ...txns];
            this.allTransactions.sort((a, b) => new Date(b.date || '').getTime() - new Date(a.date || '').getTime());
            this.filterTransactions();
          }
        });
      }
    });
  }

  filterTransactions() {
    if (!this.monitorSearchTerm.trim()) {
      this.filteredTransactions = [...this.allTransactions];
      return;
    }
    const term = this.monitorSearchTerm.toLowerCase();
    this.filteredTransactions = this.allTransactions.filter(t =>
      (t.txnId?.toLowerCase().includes(term)) ||
      (t.accountNumber?.toLowerCase().includes(term)) ||
      (t.description?.toLowerCase().includes(term)) ||
      (t.recipientName?.toLowerCase().includes(term))
    );
  }

  // ==================== Edit Mode ====================

  enableEditMode() {
    if (!this.selectedAccount) return;
    this.isEditMode = true;
    this.editAccount = { ...this.selectedAccount };
  }

  cancelEditMode() {
    this.isEditMode = false;
    this.editAccount = this.getEmptyAccount();
    this.editDocumentFile = null;
    this.editDocumentFileName = '';
  }

  saveEdit() {
    // Show document upload modal before saving
    this.showEditDocumentModal = true;
  }

  onEditDocumentSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.type !== 'application/pdf') {
        this.showAlertMessage('Only PDF documents are allowed', 'error');
        input.value = '';
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        this.showAlertMessage('File size must be less than 10MB', 'error');
        input.value = '';
        return;
      }
      this.editDocumentFile = file;
      this.editDocumentFileName = file.name;
    }
  }

  closeEditDocumentModal() {
    this.showEditDocumentModal = false;
  }

  confirmSaveEdit() {
    if (!this.selectedAccount?.id) return;

    this.isLoading = true;
    this.currentAccountService.editAccountWithHistory(
      this.selectedAccount.id,
      this.editAccount,
      this.adminName,
      this.editDocumentFile || undefined
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage(response.message || 'Account updated successfully', 'success');
          this.selectedAccount = response.account;
          this.isEditMode = false;
          this.editDocumentFile = null;
          this.editDocumentFileName = '';
          this.showEditDocumentModal = false;
          this.loadAllData();
          this.loadEditHistory();
        } else {
          this.showAlertMessage(response.error || 'Update failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Update failed', 'error');
        this.isLoading = false;
      }
    });
  }

  skipDocumentAndSave() {
    this.editDocumentFile = null;
    this.editDocumentFileName = '';
    this.showEditDocumentModal = false;
    this.confirmSaveEditDirectly();
  }

  confirmSaveEditDirectly() {
    if (!this.selectedAccount?.id) return;

    this.isLoading = true;
    this.currentAccountService.editAccountWithHistory(
      this.selectedAccount.id,
      this.editAccount,
      this.adminName
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage(response.message || 'Account updated successfully', 'success');
          this.selectedAccount = response.account;
          this.isEditMode = false;
          this.loadAllData();
          this.loadEditHistory();
        } else {
          this.showAlertMessage(response.error || 'Update failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Update failed', 'error');
        this.isLoading = false;
      }
    });
  }

  // ==================== Edit History ====================

  loadEditHistory() {
    if (!this.selectedAccount?.id) return;
    this.currentAccountService.getEditHistory(this.selectedAccount.id).subscribe({
      next: (history) => {
        this.editHistory = history;
      },
      error: (err) => console.error('Error loading edit history:', err)
    });
  }

  toggleEditHistory() {
    this.showEditHistory = !this.showEditHistory;
    if (this.showEditHistory && this.editHistory.length === 0) {
      this.loadEditHistory();
    }
  }

  getDocumentUrl(path: string | undefined): string {
    if (!path) return '';
    return environment.apiBaseUrl + path;
  }

  // ==================== Helper ====================

  showAlertMessage(message: string, type: string) {
    this.alertMessage = message;
    this.alertType = type;
    this.showAlert = true;
    setTimeout(() => this.showAlert = false, 5000);
  }

  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) return '₹0';
    return '₹' + amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  formatDateTime(dateStr: string | undefined): string {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric' }) +
      ' ' + date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
  }

  getStatusClass(status: string | undefined): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'PENDING': return 'status-pending';
      case 'FROZEN': return 'status-frozen';
      case 'REJECTED': return 'status-rejected';
      case 'CLOSED': return 'status-closed';
      default: return '';
    }
  }

  goBack() {
    const navSource = sessionStorage.getItem('navigationSource');
    if (navSource === 'manager') {
      this.router.navigate(['/manager/dashboard']);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  // ==================== Link Savings Account ====================

  loadAccountLinkStatuses() {
    this.accountLinkStatus = {};
    this.allAccounts.forEach(account => {
      if (account.accountNumber && account.status === 'ACTIVE') {
        this.currentAccountService.getLinkedAccounts(account.accountNumber).subscribe({
          next: (links) => {
            const activeLink = links.find(l => l.status === 'ACTIVE');
            this.accountLinkStatus[account.accountNumber!] = !!activeLink;
          },
          error: () => {
            this.accountLinkStatus[account.accountNumber!] = false;
          }
        });
      }
    });
  }

  openLinkModal(account: CurrentAccount) {
    if (this.accountLinkStatus[account.accountNumber || '']) {
      this.showAlertMessage('This account already has an active linked savings account', 'error');
      return;
    }
    this.linkCurrentAccountNumber = account.accountNumber || '';
    this.linkSavingsAccountNumber = '';
    this.linkSavingsCustomerId = '';
    this.linkVerified = false;
    this.linkVerifiedName = '';
    this.linkVerifiedBalance = 0;
    this.showLinkModal = true;
  }

  closeLinkModal() {
    this.showLinkModal = false;
    this.linkSavingsAccountNumber = '';
    this.linkSavingsCustomerId = '';
    this.linkVerified = false;
    this.linkVerifiedName = '';
    this.linkVerifiedBalance = 0;
  }

  verifySavingsAccount() {
    if (!this.linkSavingsAccountNumber.trim() || !this.linkSavingsCustomerId.trim()) {
      this.showAlertMessage('Please enter both Account Number and Customer ID', 'error');
      return;
    }
    this.isLoading = true;
    this.currentAccountService.verifySavingsAccount(this.linkSavingsAccountNumber, this.linkSavingsCustomerId).subscribe({
      next: (response) => {
        if (response.success) {
          this.linkVerified = true;
          this.linkVerifiedName = response.accountName;
          this.linkVerifiedBalance = response.balance;
          this.showAlertMessage('Savings account verified: ' + response.accountName, 'success');
        } else {
          this.linkVerified = false;
          this.showAlertMessage(response.message || 'Verification failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.linkVerified = false;
        this.showAlertMessage(err.error?.message || 'Verification failed', 'error');
        this.isLoading = false;
      }
    });
  }

  confirmLinkSavingsAccount() {
    if (!this.linkVerified) {
      this.showAlertMessage('Please verify the savings account first', 'error');
      return;
    }
    this.isLoading = true;
    this.currentAccountService.linkSavingsAccount(
      this.linkCurrentAccountNumber,
      this.linkSavingsAccountNumber,
      this.linkSavingsCustomerId,
      this.adminName
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Savings account linked successfully!', 'success');
          this.closeLinkModal();
          this.accountLinkStatus[this.linkCurrentAccountNumber] = true;
          if (this.selectedAccount?.accountNumber) {
            this.loadLinkedAccounts(this.selectedAccount.accountNumber);
          }
        } else {
          this.showAlertMessage(response.message || 'Linking failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.message || 'Linking failed', 'error');
        this.isLoading = false;
      }
    });
  }

  loadLinkedAccounts(currentAccountNumber: string) {
    this.currentAccountService.getLinkedAccounts(currentAccountNumber).subscribe({
      next: (links) => {
        this.linkedAccounts = links;
        this.linkedSavingsDetails = {};
        links.forEach(link => {
          this.currentAccountService.getLinkedSavingsDetails(link.savingsAccountNumber).subscribe({
            next: (details) => {
              if (details.success) {
                this.linkedSavingsDetails[link.savingsAccountNumber] = details;
              }
            }
          });
        });
      },
      error: (err) => console.error('Error loading linked accounts:', err)
    });
  }

  unlinkAccount(linkId: number) {
    this.currentAccountService.unlinkSavingsAccount(linkId).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Account unlinked successfully', 'success');
          if (this.selectedAccount?.accountNumber) {
            this.accountLinkStatus[this.selectedAccount.accountNumber] = false;
            this.loadLinkedAccounts(this.selectedAccount.accountNumber);
          }
          if (this.activeAccountType === 'savings') {
            this.switchToCurrentAccount();
          }
        }
      },
      error: (err) => this.showAlertMessage('Failed to unlink account', 'error')
    });
  }

  // ==================== Account Switch ====================

  switchToSavingsAccount(savingsAccountNumber: string) {
    this.activeAccountType = 'savings';
    this.switchedSavingsAccountNumber = savingsAccountNumber;
    this.currentAccountService.getLinkedSavingsDetails(savingsAccountNumber).subscribe({
      next: (details) => {
        if (details.success) {
          this.switchedSavingsAccount = details;
        }
      },
      error: (err) => console.error('Error loading savings details:', err)
    });
  }

  switchToCurrentAccount() {
    this.activeAccountType = 'current';
    this.switchedSavingsAccount = null;
    this.switchedSavingsAccountNumber = '';
  }

  // ==================== Cheque Request Approval ====================

  loadPendingChequeRequests() {
    this.currentAccountService.getPendingChequeRequests().subscribe({
      next: (requests) => {
        this.pendingChequeRequests = requests;
      },
      error: (err) => {
        console.error('Error loading cheque requests:', err);
        this.pendingChequeRequests = [];
      }
    });
  }

  approveChequeRequest(req: any) {
    if (!req.id) return;
    this.currentAccountService.approveChequeRequest(req.id, this.adminName).subscribe({
      next: (response) => {
        if (response.success) {
          this.showAlertMessage('Cheque book request approved successfully!', 'success');
          this.loadPendingChequeRequests();
        } else {
          this.showAlertMessage(response.error || 'Approval failed', 'error');
        }
      },
      error: (err) => this.showAlertMessage(err.error?.error || 'Failed to approve cheque request', 'error')
    });
  }

  // ==================== Soundbox Management ====================

  loadSoundboxData(): void {
    this.soundboxService.getAdminStats().subscribe({
      next: (stats) => { this.soundboxStats = stats; },
      error: () => {}
    });
    this.soundboxService.getPendingRequests().subscribe({
      next: (requests) => { this.pendingSoundboxRequests = requests; },
      error: () => {}
    });
    this.soundboxService.getAllRequests().subscribe({
      next: (requests) => { this.soundboxRequests = requests; },
      error: () => {}
    });
    this.soundboxService.getAllDevices().subscribe({
      next: (devices) => { this.soundboxDevices = devices; },
      error: () => {}
    });
  }

  openSbApproveModal(request: SoundboxRequest): void {
    this.selectedSbRequest = request;
    this.sbApproveDeviceId = 'SB' + Date.now().toString().slice(-8);
    this.sbApproveMonthlyCharge = 100;
    this.sbApproveDeviceCharge = 499;
    this.showSbApproveModal = true;
  }

  closeSbApproveModal(): void {
    this.showSbApproveModal = false;
    this.selectedSbRequest = null;
  }

  approveSoundboxRequest(): void {
    if (!this.selectedSbRequest?.id || !this.sbApproveDeviceId.trim()) return;
    this.soundboxService.approveRequest(
      this.selectedSbRequest.id,
      this.adminName,
      this.sbApproveDeviceId,
      this.sbApproveMonthlyCharge,
      this.sbApproveDeviceCharge
    ).subscribe({
      next: (res) => {
        if (res.success) {
          this.showAlertMessage('Soundbox request approved! Device assigned: ' + this.sbApproveDeviceId, 'success');
          this.showSbApproveModal = false;
          this.loadSoundboxData();
        } else {
          this.showAlertMessage(res.error || 'Approval failed', 'error');
        }
      },
      error: (err) => this.showAlertMessage(err.error?.error || 'Failed to approve request', 'error')
    });
  }

  openSbRejectModal(request: SoundboxRequest): void {
    this.selectedSbRequest = request;
    this.sbRejectRemarks = '';
    this.showSbRejectModal = true;
  }

  closeSbRejectModal(): void {
    this.showSbRejectModal = false;
    this.selectedSbRequest = null;
  }

  rejectSoundboxRequest(): void {
    if (!this.selectedSbRequest?.id) return;
    this.soundboxService.rejectRequest(this.selectedSbRequest.id, this.adminName, this.sbRejectRemarks).subscribe({
      next: (res) => {
        if (res.success) {
          this.showAlertMessage('Soundbox request rejected', 'success');
          this.showSbRejectModal = false;
          this.loadSoundboxData();
        } else {
          this.showAlertMessage(res.error || 'Rejection failed', 'error');
        }
      },
      error: (err) => this.showAlertMessage(err.error?.error || 'Failed to reject request', 'error')
    });
  }

  toggleSoundboxDevice(device: SoundboxDevice): void {
    if (!device.id) return;
    this.soundboxService.toggleDeviceStatus(device.id).subscribe({
      next: (res) => {
        if (res.success) {
          this.showAlertMessage('Device status changed to ' + res.device.status, 'success');
          this.loadSoundboxData();
        }
      },
      error: (err) => this.showAlertMessage(err.error?.error || 'Failed to toggle device', 'error')
    });
  }

  openSbChargesModal(device: SoundboxDevice): void {
    this.selectedSbDevice = device;
    this.sbNewMonthlyCharge = device.monthlyCharge || 100;
    this.sbNewDeviceCharge = device.deviceCharge || 499;
    this.showSbChargesModal = true;
  }

  closeSbChargesModal(): void {
    this.showSbChargesModal = false;
    this.selectedSbDevice = null;
  }

  updateSoundboxCharges(): void {
    if (!this.selectedSbDevice?.id) return;
    this.soundboxService.updateCharges(this.selectedSbDevice.id, this.sbNewMonthlyCharge, this.sbNewDeviceCharge).subscribe({
      next: (res) => {
        if (res.success) {
          this.showAlertMessage('Charges updated successfully', 'success');
          this.showSbChargesModal = false;
          this.loadSoundboxData();
        }
      },
      error: (err) => this.showAlertMessage(err.error?.error || 'Failed to update charges', 'error')
    });
  }

  loadSoundboxTransactionsForAccount(): void {
    if (!this.soundboxTxnAccountNumber.trim()) return;
    this.soundboxService.getTransactionsByAccount(this.soundboxTxnAccountNumber.trim()).subscribe({
      next: (txns) => { this.soundboxTransactions = txns; },
      error: () => { this.soundboxTransactions = []; }
    });
  }

  getFilteredSbDevices(): SoundboxDevice[] {
    if (this.sbDeviceFilter === 'ALL') return this.soundboxDevices;
    return this.soundboxDevices.filter(d => d.status === this.sbDeviceFilter);
  }

  // ==================== UPI Payments Management ====================

  loadUpiData(): void {
    this.isLoadingUpiData = true;
    this.upiService.getAdminStats().subscribe({
      next: (stats) => { this.adminUpiStats = stats; },
      error: (err) => {
        console.error('Error loading UPI stats:', err);
        this.adminUpiStats = { totalPayments: 0, totalVolume: 0, activeAccounts: 0, todayVolume: 0 };
      }
    });
    this.upiService.getRecentPayments().subscribe({
      next: (payments) => {
        this.recentUpiPayments = payments;
        this.filteredUpiPayments = payments;
        this.isLoadingUpiData = false;
      },
      error: (err) => {
        console.error('Error loading UPI payments:', err);
        this.isLoadingUpiData = false;
      }
    });
  }

  filterUpiPayments(): void {
    if (!this.upiSearchTerm.trim()) {
      this.filteredUpiPayments = this.recentUpiPayments;
      return;
    }
    const term = this.upiSearchTerm.toLowerCase();
    this.filteredUpiPayments = this.recentUpiPayments.filter(p =>
      (p.txnId?.toLowerCase().includes(term)) ||
      (p.accountNumber?.toLowerCase().includes(term)) ||
      (p.businessName?.toLowerCase().includes(term)) ||
      (p.payerName?.toLowerCase().includes(term)) ||
      (p.payerUpi?.toLowerCase().includes(term)) ||
      (p.upiId?.toLowerCase().includes(term))
    );
  }

  searchUpiByAccount(): void {
    if (!this.upiAccountSearch.trim()) return;
    this.upiService.getPayments(this.upiAccountSearch.trim()).subscribe({
      next: (payments) => { this.upiAccountPayments = payments; },
      error: () => {
        this.upiAccountPayments = [];
        this.showAlertMessage('No payments found for this account', 'error');
      }
    });
  }

  searchByTxnId(): void {
    if (!this.upiTxnSearch.trim()) return;
    this.isSearchingTxn = true;
    this.txnSearchDone = false;
    this.txnSearchResult = null;

    this.upiService.searchByTxnId(this.upiTxnSearch.trim()).subscribe({
      next: (res) => {
        this.isSearchingTxn = false;
        this.txnSearchDone = true;
        this.txnSearchResult = res;
      },
      error: (err) => {
        this.isSearchingTxn = false;
        this.txnSearchDone = true;
        this.txnSearchResult = { found: false };
        this.showAlertMessage(err.error?.error || 'Transaction search failed', 'error');
      }
    });
  }

  // ==================== QR Code Generation (Soundbox Tab) ====================

  verifyQrUpiId(): void {
    if (!this.qrUpiId.trim()) {
      this.qrVerificationStatus = '';
      this.qrVerifiedName = '';
      return;
    }
    if (!this.qrAccountNumber.trim()) {
      this.showAlertMessage('Please enter the account number first', 'error');
      return;
    }

    this.qrVerificationStatus = 'verifying';
    this.qrVerificationError = '';
    this.qrVerifiedName = '';

    this.upiService.verifyUpiId(this.qrUpiId.trim()).subscribe({
      next: (res) => {
        if (res.verified) {
          // Get the account to verify name match
          const matchAccount = this.allAccounts.find(a => a.accountNumber === this.qrAccountNumber.trim());
          const accountOwnerName = matchAccount?.ownerName?.toLowerCase().trim() || '';
          const accountBusinessName = matchAccount?.businessName?.toLowerCase().trim() || '';
          const verifiedName = (res.accountName || '').toLowerCase().trim();
          const verifiedOwner = (res.ownerName || '').toLowerCase().trim();

          // Check if verified account matches the entered account number
          if (res.accountNumber !== this.qrAccountNumber.trim()) {
            this.qrVerificationStatus = 'mismatch';
            this.qrVerificationError = 'UPI ID does not belong to account ' + this.qrAccountNumber;
            return;
          }

          // Verify that the name matches the account holder
          if (verifiedName === accountBusinessName || verifiedName === accountOwnerName ||
              verifiedOwner === accountOwnerName) {
            this.qrVerifiedName = res.accountName;
            this.qrVerificationStatus = 'verified';
          } else {
            this.qrVerificationStatus = 'mismatch';
            this.qrVerificationError = 'UPI ID name "' + res.accountName + '" does not match the account holder name';
          }
        } else {
          this.qrVerificationStatus = 'failed';
          this.qrVerificationError = res.error || 'UPI ID verification failed';
        }
      },
      error: (err) => {
        this.qrVerificationStatus = 'failed';
        this.qrVerificationError = err.error?.error || 'Failed to verify UPI ID';
      }
    });
  }

  generateSoundboxQrCode(): void {
    if (!this.qrAccountNumber.trim() || !this.qrUpiId.trim()) {
      this.showAlertMessage('Please enter account number and UPI ID', 'error');
      return;
    }
    if (this.qrVerificationStatus !== 'verified') {
      this.showAlertMessage('Please verify the UPI ID first. Name must match account holder.', 'error');
      return;
    }

    this.isGeneratingQr = true;
    const amount = this.qrIsCustomAmount && this.qrAmount ? this.qrAmount : undefined;

    this.upiService.generateQrCodeForUpi(this.qrUpiId.trim(), this.qrAccountNumber.trim(), amount).subscribe({
      next: (res) => {
        this.isGeneratingQr = false;
        if (res.success) {
          this.qrGeneratedImage = res.qrCode;
          this.qrGeneratedUpiId = res.upiId;
          this.qrGeneratedBusinessName = res.businessName;
          this.qrGeneratedAmount = res.amount || null;
          this.showQrResultModal = true;
        } else {
          this.showAlertMessage(res.error || 'QR code generation failed', 'error');
        }
      },
      error: (err) => {
        this.isGeneratingQr = false;
        this.showAlertMessage(err.error?.error || 'Failed to generate QR code', 'error');
      }
    });
  }

  closeQrResultModal(): void {
    this.showQrResultModal = false;
    this.qrGeneratedImage = '';
  }

  resetQrForm(): void {
    this.qrAccountNumber = '';
    this.qrUpiId = '';
    this.qrAmount = null;
    this.qrIsCustomAmount = false;
    this.qrVerifiedName = '';
    this.qrVerificationStatus = '';
    this.qrVerificationError = '';
    this.qrGeneratedImage = '';
  }

  downloadQrCode(): void {
    if (!this.qrGeneratedImage) return;
    const link = document.createElement('a');
    link.href = 'data:image/png;base64,' + this.qrGeneratedImage;
    link.download = 'QR_' + this.qrGeneratedUpiId.replace('@', '_') + (this.qrGeneratedAmount ? '_Rs' + this.qrGeneratedAmount : '') + '.png';
    link.click();
  }
}
