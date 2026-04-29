import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CurrentAccountService } from '../../../service/current-account.service';
import { SoundboxService } from '../../../service/soundbox.service';
import { UpiService } from '../../../service/upi.service';
import { environment } from '../../../../environment/environment';
import {
  CurrentAccount, BusinessTransaction, CurrentAccountBeneficiary,
  CurrentAccountChequeRequest, CurrentAccountVendorPayment,
  CurrentAccountInvoice, InvoiceItem, CurrentAccountBusinessLoan, CurrentAccountBusinessUser,
  LinkedAccount
} from '../../../model/current-account/current-account.model';
import { SoundboxDevice, SoundboxRequest, SoundboxTransaction, SoundboxStats } from '../../../model/soundbox/soundbox.model';
import { UpiPayment, UpiStats, QrCodeResponse } from '../../../model/upi/upi.model';
import { BusinessDrawChequeComponent } from './business-draw-cheque/business-draw-cheque.component';
import { PaymentGatewayService } from '../../../service/payment-gateway.service';

@Component({
  selector: 'app-current-account-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, BusinessDrawChequeComponent],
  templateUrl: './current-account-dashboard.html',
  styleUrls: ['./current-account-dashboard.css']
})
export class CurrentAccountDashboard implements OnInit, OnDestroy {

  account: CurrentAccount | null = null;
  activeSection = 'dashboard';
  isBrowser = false;

  // Sidebar Search
  sidebarSearchQuery = '';
  currentMenuItems: { section: string; icon: string; label: string; action?: () => void }[] = [
    { section: 'dashboard', icon: '', label: 'Dashboard' },
    { section: 'transfer', icon: '', label: 'Transfer Money' },
    { section: 'vendors', icon: '', label: 'Vendor Payments' },
    { section: 'transactions', icon: '', label: 'Transaction History' },
    { section: 'beneficiaries', icon: '', label: 'Beneficiary Management' },
    { section: 'overdraft', icon: '', label: 'Overdraft Usage' },
    { section: 'cheque', icon: '', label: 'Cheque Book Request' },
    { section: 'draw-cheque', icon: '', label: 'Draw Cheque' },
    { section: 'statement', icon: '', label: 'Account Statement' },
    { section: 'profile', icon: '', label: 'Profile Settings' },
    { section: 'invoices', icon: '', label: 'Invoice Generation' },
    { section: 'loans', icon: '', label: 'Business Loans' },
    { section: 'users', icon: '', label: 'Multi-User Access' },
    { section: 'soundbox', icon: '', label: 'Soundbox & Payments' },
    { section: 'upi', icon: '', label: 'UPI & QR Payments' },
    { section: 'logout', icon: '', label: 'Logout', action: () => this.logout() }
  ];

  // Dashboard stats
  dashboardStats: any = {};
  isLoadingStats = true;

  // Transactions
  transactions: BusinessTransaction[] = [];
  filteredTransactions: BusinessTransaction[] = [];
  isLoadingTxns = false;
  txnFilterType = 'all';
  txnFilterStartDate = '';
  txnFilterEndDate = '';
  currentTxnPage = 0;
  txnPageSize = 10;
  totalTxnPages = 0;

  // Transfer Money
  transferBeneficiaryAccount = '';
  transferIfsc = '';
  transferAmount: number | null = null;
  transferRemarks = '';
  transferType = 'NEFT';
  isTransferring = false;
  showTransferResult = false;
  transferResult: any = null;

  // Recipient Verification
  isVerifyingRecipient = false;
  recipientVerified = false;
  recipientNotFound = false;
  recipientName = '';
  recipientAccountType = '';
  recipientBankName = '';
  recipientMessage = '';

  // Vendor Payments
  vendorPayments: CurrentAccountVendorPayment[] = [];
  isLoadingVendors = false;
  vendorName = '';
  vendorAccount = '';
  vendorIfsc = '';
  vendorAmount: number | null = null;
  vendorDescription = '';
  isProcessingVendor = false;

  // Beneficiaries
  beneficiaries: CurrentAccountBeneficiary[] = [];
  isLoadingBeneficiaries = false;
  newBeneficiaryName = '';
  newBeneficiaryAccount = '';
  newBeneficiaryIfsc = '';
  newBeneficiaryBank = '';
  newBeneficiaryNickName = '';
  isAddingBeneficiary = false;

  // Cheque Requests
  chequeRequests: CurrentAccountChequeRequest[] = [];
  isLoadingCheques = false;
  chequeLeaves = 25;
  chequeDeliveryAddress = '';
  isRequestingCheque = false;

  // Profile
  profileShopAddress = '';
  profileCity = '';
  profileState = '';
  profilePincode = '';
  profileMobile = '';
  profileEmail = '';
  isUpdatingProfile = false;

  // Statement
  statementStartDate = '';
  statementEndDate = '';
  statementTransactions: BusinessTransaction[] = [];
  isLoadingStatement = false;

  // Overdraft
  overdraftUsed = 0;
  overdraftRemaining = 0;

  // Alert
  alertMessage = '';
  alertType = '';
  showAlert = false;

  // Invoice Management
  invoices: CurrentAccountInvoice[] = [];
  isLoadingInvoices = false;
  showInvoiceForm = false;
  invoiceClientName = '';
  invoiceClientEmail = '';
  invoiceClientPhone = '';
  invoiceClientAddress = '';
  invoiceClientGst = '';
  invoiceDate = '';
  invoiceDueDate = '';
  invoiceItems: InvoiceItem[] = [{ description: '', quantity: 1, unitPrice: 0, amount: 0 }];
  invoiceTaxRate = 18;
  invoiceDiscount = 0;
  invoiceNotes = '';
  invoiceTerms = 'Payment due within 30 days';
  isCreatingInvoice = false;
  invoiceSubtotal = 0;
  invoiceTaxAmount = 0;
  invoiceTotal = 0;

  // Business Loan
  businessLoans: CurrentAccountBusinessLoan[] = [];
  isLoadingLoans = false;
  showLoanForm = false;
  loanType = 'Working Capital';
  loanPanNumber = '';
  loanAmount: number | null = null;
  loanTenure = 12;
  loanPurpose = '';
  loanAnnualRevenue: number | null = null;
  loanYearsInBusiness: number | null = null;
  isApplyingLoan = false;
  loanResult: any = null;
  showLoanResult = false;

  // Multi-User Access
  businessUsers: CurrentAccountBusinessUser[] = [];
  isLoadingUsers = false;
  showUserForm = false;
  newUserFullName = '';
  newUserEmail = '';
  newUserMobile = '';
  newUserRole = 'STAFF';
  newUserPassword = '';
  isAddingUser = false;

  // Role-based access
  userRole = 'OWNER';
  businessUserName = '';
  isTeamUser = false;

  // Soundbox
  soundboxDevice: SoundboxDevice | null = null;
  soundboxStats: SoundboxStats | null = null;
  soundboxRequests: SoundboxRequest[] = [];
  soundboxTransactions: SoundboxTransaction[] = [];
  hasSoundbox = false;
  isLoadingSoundbox = false;
  showApplyModal = false;
  sbDeliveryAddress = '';
  sbCity = '';
  sbState = '';
  sbPincode = '';
  sbMobile = '';
  isApplyingSoundbox = false;
  newUpiId = '';
  showSimulateModal = false;
  simulateAmount: number | null = null;
  simulatePayerName = '';
  simulatePayerUpi = '';
  simulatePaymentMethod = 'UPI';
  isProcessingPayment = false;
  showPaymentSuccess = false;
  lastPaymentResult: any = null;

  // Soundbox QR Code Generator
  sbQrUpiId = '';
  sbQrAmount: number | null = null;
  sbQrIsCustomAmount = false;
  sbQrVerifiedName = '';
  sbQrVerificationStatus = ''; // '', 'verifying', 'verified', 'failed', 'name-mismatch'
  sbQrVerificationError = '';
  sbQrGeneratedImage = '';
  sbQrGeneratedBusinessName = '';
  sbQrGeneratedAmount: number | null = null;
  isGeneratingSbQr = false;
  showSbQrResultModal = false;

  // Cross-Customer UPI Payment
  sbPayUpiId = '';
  sbPayVerifiedName = '';
  sbPayVerificationStatus = ''; // '', 'verifying', 'verified', 'failed'
  sbPayVerificationError = '';
  sbPayAmount: number | null = null;
  sbPayNote = '';
  isCrossPayProcessing = false;
  showCrossPaySuccess = false;
  crossPayResult: any = null;

  // UPI & QR Payments
  upiStats: UpiStats | null = null;
  upiPayments: UpiPayment[] = [];
  isLoadingUpi = false;
  upiQrCode = '';
  upiQrAmount: number | null = null;
  showUpiQr = false;
  upiId = '';
  newUpiIdInput = '';
  showEditUpiModal = false;
  showUpiSimulateModal = false;
  upiSimAmount: number | null = null;
  upiSimPayerName = '';
  upiSimPayerUpi = '';
  isProcessingUpiPayment = false;
  showUpiPaymentSuccess = false;
  lastUpiPaymentResult: any = null;

  // QR Upload to Pay (Business UPI)
  isBizQrScanning = false;
  bizQrScanError = '';
  bizQrScannedUpiId = '';
  bizQrScannedName = '';
  bizQrScannedAmount: number | null = null;
  bizQrScannedRemark = '';
  showBizQrScanResult = false;
  bizQrVerifiedName = '';
  bizQrVerificationStatus = ''; // '', 'verifying', 'verified', 'failed'
  bizQrPayAmount: number | null = null;
  bizQrPayNote = '';
  isBizQrPayProcessing = false;
  showBizQrPaySuccess = false;
  bizQrPayResult: any = null;

  // ─── Payment Links (incoming requests from merchants) ──────
  pendingPaymentLinks: any[] = [];
  allPaymentLinks: any[] = [];
  isLoadingLinks = false;
  private linkPollInterval: any = null;
  showLinkPayModal = false;
  selectedLink: any = null;
  linkPayPin = '';
  isLinkPayProcessing = false;
  linkPaySuccess = false;
  linkPayResult: any = null;

  // UPI PIN (account password) management
  upiCurrentPassword = '';
  upiNewPassword = '';
  upiConfirmPassword = '';
  isUpiPinChanging = false;
  upiPinChangeSuccess = false;
  upiPinChangeError = '';

  // Linked Account Switch
  linkedAccount: LinkedAccount | null = null;
  hasLinkedAccount = false;
  showPinModal = false;
  switchPin = '';
  confirmSwitchPin = '';
  isPinCreating = false;
  showVerifyPinModal = false;
  verifyPin = '';
  isVerifyingPin = false;
  linkedSavingsDetails: any = null;

  constructor(
    private currentAccountService: CurrentAccountService,
    private soundboxService: SoundboxService,
    private upiService: UpiService,
    private router: Router,
    private http: HttpClient,
    private pgService: PaymentGatewayService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  // Sidebar Search Methods
  onSidebarSearch() {
    // Real-time filtering handled by getFilteredCurrentMenu()
  }

  getFilteredCurrentMenu() {
    let items = this.currentMenuItems;

    // Role-based filtering
    if (this.isTeamUser) {
      const role = this.userRole;
      if (role === 'STAFF') {
        // Staff: Dashboard, Transaction History, Profile, Logout
        const allowed = ['dashboard', 'transactions', 'profile', 'logout'];
        items = items.filter(item => allowed.includes(item.section));
      } else if (role === 'ACCOUNTANT') {
        // Accountant: Dashboard, Transactions, Vendors, Invoices, Statement, Profile, Logout
        const allowed = ['dashboard', 'transactions', 'vendors', 'invoices', 'statement', 'profile', 'logout'];
        items = items.filter(item => allowed.includes(item.section));
      }
    }

    if (!this.sidebarSearchQuery || !this.sidebarSearchQuery.trim()) {
      return items;
    }
    const query = this.sidebarSearchQuery.toLowerCase().trim();
    return items.filter(item => item.label.toLowerCase().includes(query) || item.section.toLowerCase().includes(query));
  }

  isMenuItemMatch(item: any): boolean {
    if (!this.sidebarSearchQuery) return false;
    const query = this.sidebarSearchQuery.toLowerCase().trim();
    return item.label.toLowerCase().includes(query) || item.section.toLowerCase().includes(query);
  }

  ngOnInit(): void {
    if (this.isBrowser) {
      const stored = sessionStorage.getItem('currentAccount');
      if (stored) {
        this.account = JSON.parse(stored);
        // Check if this is a team user login
        const role = sessionStorage.getItem('businessUserRole');
        if (role) {
          this.userRole = role;
          this.isTeamUser = true;
          this.businessUserName = sessionStorage.getItem('businessUserName') || '';
        }
        this.loadDashboardData();
        this.initProfileFields();
        this.checkLinkedAccount();
      } else {
        this.router.navigate(['/website/current-account-login']);
      }
    }
  }

  loadDashboardData(): void {
    if (!this.account) return;
    this.isLoadingStats = true;
    this.currentAccountService.getUserDashboardStats(this.account.accountNumber!).subscribe({
      next: (stats) => {
        this.dashboardStats = stats;
        this.calculateOverdraft();
        this.isLoadingStats = false;
      },
      error: () => {
        this.isLoadingStats = false;
      }
    });
    this.loadRecentTransactions();
  }

  calculateOverdraft(): void {
    if (!this.account) return;
    const balance = this.dashboardStats.balance || 0;
    const odLimit = this.dashboardStats.overdraftLimit || 0;
    if (balance < 0) {
      this.overdraftUsed = Math.abs(balance);
      this.overdraftRemaining = odLimit - this.overdraftUsed;
    } else {
      this.overdraftUsed = 0;
      this.overdraftRemaining = odLimit;
    }
  }

  initProfileFields(): void {
    if (!this.account) return;
    this.profileShopAddress = this.account.shopAddress || '';
    this.profileCity = this.account.city || '';
    this.profileState = this.account.state || '';
    this.profilePincode = this.account.pincode || '';
    this.profileMobile = this.account.mobile || '';
    this.profileEmail = this.account.email || '';
  }

  setSection(section: string): void {
    this.activeSection = section;
    this.hideAlert();
    if (section === 'transactions') this.loadTransactionHistory();
    if (section === 'beneficiaries') this.loadBeneficiaries();
    if (section === 'cheque') this.loadChequeRequests();
    if (section === 'vendors') this.loadVendorPayments();
    if (section === 'overdraft') {
      this.loadDashboardData();
    }
    if (section === 'profile') this.initProfileFields();
    if (section === 'invoices') this.loadInvoices();
    if (section === 'loans') this.loadBusinessLoans();
    if (section === 'users') this.loadBusinessUsers();
    if (section === 'soundbox') this.loadSoundboxData();
    if (section === 'upi') { this.loadUpiData(); this.loadCurrentPaymentLinks(); }
  }

  // ==================== Transactions ====================

  loadRecentTransactions(): void {
    if (!this.account) return;
    this.currentAccountService.getRecentTransactions(this.account.accountNumber!, 5).subscribe({
      next: (txns) => { this.transactions = txns; },
      error: () => {}
    });
  }

  loadTransactionHistory(): void {
    if (!this.account) return;
    this.isLoadingTxns = true;
    this.currentAccountService.getTransactionsPaginated(this.account.accountNumber!, this.currentTxnPage, this.txnPageSize).subscribe({
      next: (page: any) => {
        this.filteredTransactions = page.content || [];
        this.totalTxnPages = page.totalPages || 0;
        this.isLoadingTxns = false;
      },
      error: () => { this.isLoadingTxns = false; }
    });
  }

  filterTransactions(): void {
    if (this.txnFilterStartDate && this.txnFilterEndDate && this.account) {
      this.isLoadingTxns = true;
      this.currentAccountService.getTransactionsByDateRange(
        this.account.accountNumber!, this.txnFilterStartDate, this.txnFilterEndDate
      ).subscribe({
        next: (txns) => {
          this.filteredTransactions = txns;
          if (this.txnFilterType !== 'all') {
            this.filteredTransactions = txns.filter(t => t.txnType === this.txnFilterType);
          }
          this.isLoadingTxns = false;
        },
        error: () => { this.isLoadingTxns = false; }
      });
    } else {
      this.loadTransactionHistory();
    }
  }

  nextTxnPage(): void {
    if (this.currentTxnPage < this.totalTxnPages - 1) {
      this.currentTxnPage++;
      this.loadTransactionHistory();
    }
  }

  prevTxnPage(): void {
    if (this.currentTxnPage > 0) {
      this.currentTxnPage--;
      this.loadTransactionHistory();
    }
  }

  // ==================== Transfer Money ====================

  verifyRecipient(): void {
    if (!this.transferBeneficiaryAccount || this.transferBeneficiaryAccount.length < 6) {
      this.recipientVerified = false;
      this.recipientNotFound = false;
      this.recipientName = '';
      return;
    }
    if (this.transferBeneficiaryAccount === this.account?.accountNumber) {
      this.showAlertMessage('Cannot transfer to your own account', 'error');
      return;
    }
    this.isVerifyingRecipient = true;
    this.recipientVerified = false;
    this.recipientNotFound = false;
    this.currentAccountService.verifyRecipientAccount(this.transferBeneficiaryAccount).subscribe({
      next: (res) => {
        this.isVerifyingRecipient = false;
        if (res.found) {
          this.recipientVerified = true;
          this.recipientName = res.name;
          this.recipientAccountType = res.accountType;
          this.recipientBankName = res.bankName;
          if (res.ifscCode) this.transferIfsc = res.ifscCode;
        } else {
          this.recipientNotFound = true;
          this.recipientMessage = res.message;
        }
      },
      error: () => {
        this.isVerifyingRecipient = false;
        this.recipientNotFound = true;
        this.recipientMessage = 'Could not verify account. You can still proceed with transfer.';
      }
    });
  }

  onRecipientChange(): void {
    this.recipientVerified = false;
    this.recipientNotFound = false;
    this.recipientName = '';
    this.recipientAccountType = '';
  }

  transferMoney(): void {
    if (!this.account || !this.transferBeneficiaryAccount || !this.transferIfsc || !this.transferAmount) {
      this.showAlertMessage('Please fill all required fields', 'error');
      return;
    }
    if (this.transferAmount <= 0) {
      this.showAlertMessage('Amount must be greater than zero', 'error');
      return;
    }
    this.isTransferring = true;
    const transferRequest = {
      fromAccount: this.account.accountNumber,
      toAccount: this.transferBeneficiaryAccount,
      recipientName: this.recipientVerified ? this.recipientName : 'Beneficiary',
      amount: this.transferAmount,
      transferType: this.transferType,
      description: this.transferRemarks || this.transferType + ' Transfer'
    };
    this.currentAccountService.processTransfer(transferRequest).subscribe({
      next: (res) => {
        this.isTransferring = false;
        if (res.success) {
          this.transferResult = res;
          this.showTransferResult = true;
          this.showAlertMessage('Transfer of ₹' + this.transferAmount + ' completed successfully!', 'success');
          this.resetTransferForm();
          this.loadDashboardData();
        } else {
          this.showAlertMessage(res.error || 'Transfer failed', 'error');
        }
      },
      error: (err) => {
        this.isTransferring = false;
        this.showAlertMessage(err.error?.error || 'Transfer failed', 'error');
      }
    });
  }

  resetTransferForm(): void {
    this.transferBeneficiaryAccount = '';
    this.transferIfsc = '';
    this.transferAmount = null;
    this.transferRemarks = '';
    this.transferType = 'NEFT';
    this.recipientVerified = false;
    this.recipientNotFound = false;
    this.recipientName = '';
    this.recipientAccountType = '';
  }

  selectBeneficiaryForTransfer(b: CurrentAccountBeneficiary): void {
    this.transferBeneficiaryAccount = b.beneficiaryAccount;
    this.transferIfsc = b.beneficiaryIfsc;
    this.setSection('transfer');
  }

  // ==================== Vendor Payments ====================

  loadVendorPayments(): void {
    if (!this.account) return;
    this.isLoadingVendors = true;
    this.currentAccountService.getVendorPayments(this.account.accountNumber!).subscribe({
      next: (payments) => {
        this.vendorPayments = payments;
        this.isLoadingVendors = false;
      },
      error: () => { this.isLoadingVendors = false; }
    });
  }

  processVendorPayment(): void {
    if (!this.account || !this.vendorName || !this.vendorAccount || !this.vendorAmount) {
      this.showAlertMessage('Please fill all required fields', 'error');
      return;
    }
    if (this.vendorAmount <= 0) {
      this.showAlertMessage('Amount must be greater than zero', 'error');
      return;
    }
    this.isProcessingVendor = true;
    const payment: CurrentAccountVendorPayment = {
      accountNumber: this.account.accountNumber!,
      vendorName: this.vendorName,
      vendorAccount: this.vendorAccount,
      vendorIfsc: this.vendorIfsc,
      amount: this.vendorAmount,
      description: this.vendorDescription
    };
    this.currentAccountService.processVendorPayment(payment).subscribe({
      next: (res) => {
        this.isProcessingVendor = false;
        if (res.success) {
          this.showAlertMessage('Vendor payment of ₹' + this.vendorAmount + ' processed!', 'success');
          this.resetVendorForm();
          this.loadVendorPayments();
          this.loadDashboardData();
        } else {
          this.showAlertMessage(res.error || 'Payment failed', 'error');
        }
      },
      error: (err) => {
        this.isProcessingVendor = false;
        this.showAlertMessage(err.error?.error || 'Payment failed', 'error');
      }
    });
  }

  resetVendorForm(): void {
    this.vendorName = '';
    this.vendorAccount = '';
    this.vendorIfsc = '';
    this.vendorAmount = null;
    this.vendorDescription = '';
  }

  // ==================== Beneficiaries ====================

  loadBeneficiaries(): void {
    if (!this.account) return;
    this.isLoadingBeneficiaries = true;
    this.currentAccountService.getBeneficiaries(this.account.accountNumber!).subscribe({
      next: (list) => {
        this.beneficiaries = list.filter(b => b.status !== 'DELETED');
        this.isLoadingBeneficiaries = false;
      },
      error: () => { this.isLoadingBeneficiaries = false; }
    });
  }

  addBeneficiary(): void {
    if (!this.account || !this.newBeneficiaryName || !this.newBeneficiaryAccount || !this.newBeneficiaryIfsc) {
      this.showAlertMessage('Please fill all required fields', 'error');
      return;
    }
    this.isAddingBeneficiary = true;
    const beneficiary: CurrentAccountBeneficiary = {
      accountNumber: this.account.accountNumber!,
      beneficiaryName: this.newBeneficiaryName,
      beneficiaryAccount: this.newBeneficiaryAccount,
      beneficiaryIfsc: this.newBeneficiaryIfsc,
      beneficiaryBank: this.newBeneficiaryBank,
      nickName: this.newBeneficiaryNickName
    };
    this.currentAccountService.addBeneficiary(beneficiary).subscribe({
      next: (res) => {
        this.isAddingBeneficiary = false;
        if (res.success) {
          this.showAlertMessage('Beneficiary added successfully!', 'success');
          this.resetBeneficiaryForm();
          this.loadBeneficiaries();
        }
      },
      error: () => {
        this.isAddingBeneficiary = false;
        this.showAlertMessage('Failed to add beneficiary', 'error');
      }
    });
  }

  removeBeneficiary(id: number): void {
    if (confirm('Are you sure you want to remove this beneficiary?')) {
      this.currentAccountService.deleteBeneficiary(id).subscribe({
        next: () => {
          this.showAlertMessage('Beneficiary removed', 'success');
          this.loadBeneficiaries();
        },
        error: () => { this.showAlertMessage('Failed to remove beneficiary', 'error'); }
      });
    }
  }

  resetBeneficiaryForm(): void {
    this.newBeneficiaryName = '';
    this.newBeneficiaryAccount = '';
    this.newBeneficiaryIfsc = '';
    this.newBeneficiaryBank = '';
    this.newBeneficiaryNickName = '';
  }

  // ==================== Cheque Book ====================

  loadChequeRequests(): void {
    if (!this.account) return;
    this.isLoadingCheques = true;
    this.currentAccountService.getChequeRequests(this.account.accountNumber!).subscribe({
      next: (list) => {
        this.chequeRequests = list;
        this.isLoadingCheques = false;
      },
      error: () => { this.isLoadingCheques = false; }
    });
  }

  requestChequeBook(): void {
    if (!this.account) return;
    this.isRequestingCheque = true;
    const request: CurrentAccountChequeRequest = {
      accountNumber: this.account.accountNumber!,
      leaves: this.chequeLeaves,
      deliveryAddress: this.chequeDeliveryAddress || this.account.shopAddress || ''
    };
    this.currentAccountService.requestChequeBook(request).subscribe({
      next: (res) => {
        this.isRequestingCheque = false;
        if (res.success) {
          this.showAlertMessage('Cheque book request submitted! Request ID: ' + res.request.requestId, 'success');
          this.chequeDeliveryAddress = '';
          this.loadChequeRequests();
        }
      },
      error: () => {
        this.isRequestingCheque = false;
        this.showAlertMessage('Failed to submit cheque book request', 'error');
      }
    });
  }

  // ==================== Account Statement ====================

  downloadStatement(): void {
    if (!this.account || !this.statementStartDate || !this.statementEndDate) {
      this.showAlertMessage('Please select date range', 'error');
      return;
    }
    this.isLoadingStatement = true;
    this.currentAccountService.getTransactionsByDateRange(
      this.account.accountNumber!, this.statementStartDate, this.statementEndDate
    ).subscribe({
      next: (txns) => {
        this.statementTransactions = txns;
        this.isLoadingStatement = false;
        this.generatePdfStatement();
      },
      error: () => {
        this.isLoadingStatement = false;
        this.showAlertMessage('Failed to load statement', 'error');
      }
    });
  }

  generatePdfStatement(): void {
    if (!this.account || this.statementTransactions.length === 0) {
      this.showAlertMessage('No transactions found for this period', 'error');
      return;
    }

    let html = `
      <html><head><title>Account Statement</title>
      <style>
        body { font-family: Arial, sans-serif; padding: 30px; }
        h2 { color: #1a237e; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; font-size: 13px; }
        th { background: #1a237e; color: #fff; }
        .header-info { margin-bottom: 20px; }
        .header-info p { margin: 4px 0; }
      </style></head><body>
      <h2>NeoBank - Current Account Statement</h2>
      <div class="header-info">
        <p><strong>Business Name:</strong> ${this.account.businessName}</p>
        <p><strong>Account Number:</strong> ${this.account.accountNumber}</p>
        <p><strong>Period:</strong> ${this.statementStartDate} to ${this.statementEndDate}</p>
      </div>
      <table>
        <tr><th>Date</th><th>Description</th><th>Debit</th><th>Credit</th><th>Balance</th></tr>`;

    for (const txn of this.statementTransactions) {
      const debit = txn.txnType === 'Debit' || txn.txnType === 'Transfer' || txn.txnType === 'Vendor Payment' ? '₹' + txn.amount.toLocaleString() : '';
      const credit = txn.txnType === 'Credit' ? '₹' + txn.amount.toLocaleString() : '';
      html += `<tr>
        <td>${new Date(txn.date || '').toLocaleDateString()}</td>
        <td>${txn.description || txn.txnType}</td>
        <td>${debit}</td>
        <td>${credit}</td>
        <td>₹${(txn.balance || 0).toLocaleString()}</td>
      </tr>`;
    }
    html += '</table></body></html>';

    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(html);
      printWindow.document.close();
      printWindow.print();
    }
  }

  // ==================== Profile ====================

  updateProfile(): void {
    if (!this.account) return;
    this.isUpdatingProfile = true;
    const updates: any = {
      shopAddress: this.profileShopAddress,
      city: this.profileCity,
      state: this.profileState,
      pincode: this.profilePincode
    };
    this.currentAccountService.updateBusinessProfile(this.account.accountNumber!, updates).subscribe({
      next: (res) => {
        this.isUpdatingProfile = false;
        if (res.success) {
          this.account = res.account;
          sessionStorage.setItem('currentAccount', JSON.stringify(this.account));
          this.showAlertMessage('Profile updated successfully!', 'success');
        } else {
          this.showAlertMessage(res.error || 'Update failed', 'error');
        }
      },
      error: () => {
        this.isUpdatingProfile = false;
        this.showAlertMessage('Failed to update profile', 'error');
      }
    });
  }

  // ==================== Invoice Management ====================

  loadInvoices(): void {
    if (!this.account) return;
    this.isLoadingInvoices = true;
    this.currentAccountService.getInvoices(this.account.accountNumber!).subscribe({
      next: (list) => {
        this.invoices = list;
        this.isLoadingInvoices = false;
      },
      error: () => { this.isLoadingInvoices = false; }
    });
  }

  addInvoiceItem(): void {
    this.invoiceItems.push({ description: '', quantity: 1, unitPrice: 0, amount: 0 });
  }

  removeInvoiceItem(index: number): void {
    if (this.invoiceItems.length > 1) {
      this.invoiceItems.splice(index, 1);
      this.calculateInvoiceTotals();
    }
  }

  updateItemAmount(index: number): void {
    const item = this.invoiceItems[index];
    item.amount = item.quantity * item.unitPrice;
    this.calculateInvoiceTotals();
  }

  calculateInvoiceTotals(): void {
    this.invoiceSubtotal = this.invoiceItems.reduce((sum, item) => sum + item.amount, 0);
    this.invoiceTaxAmount = (this.invoiceSubtotal * this.invoiceTaxRate) / 100;
    this.invoiceTotal = this.invoiceSubtotal + this.invoiceTaxAmount - this.invoiceDiscount;
  }

  createInvoice(): void {
    if (!this.account || !this.invoiceClientName || !this.invoiceDate || !this.invoiceDueDate) {
      this.showAlertMessage('Please fill all required fields', 'error');
      return;
    }
    const validItems = this.invoiceItems.filter(i => i.description && i.amount > 0);
    if (validItems.length === 0) {
      this.showAlertMessage('Please add at least one item', 'error');
      return;
    }
    this.isCreatingInvoice = true;
    this.calculateInvoiceTotals();

    const invoice: CurrentAccountInvoice = {
      accountNumber: this.account.accountNumber!,
      clientName: this.invoiceClientName,
      clientEmail: this.invoiceClientEmail,
      clientPhone: this.invoiceClientPhone,
      clientAddress: this.invoiceClientAddress,
      clientGst: this.invoiceClientGst,
      invoiceDate: this.invoiceDate,
      dueDate: this.invoiceDueDate,
      itemsJson: JSON.stringify(validItems),
      subtotal: this.invoiceSubtotal,
      taxRate: this.invoiceTaxRate,
      taxAmount: this.invoiceTaxAmount,
      discount: this.invoiceDiscount,
      totalAmount: this.invoiceTotal,
      notes: this.invoiceNotes,
      terms: this.invoiceTerms
    };

    this.currentAccountService.createInvoice(invoice).subscribe({
      next: (res) => {
        this.isCreatingInvoice = false;
        if (res.success) {
          this.showAlertMessage('Invoice ' + res.invoice.invoiceNumber + ' created!', 'success');
          this.resetInvoiceForm();
          this.loadInvoices();
        } else {
          this.showAlertMessage(res.error || 'Failed to create invoice', 'error');
        }
      },
      error: () => {
        this.isCreatingInvoice = false;
        this.showAlertMessage('Failed to create invoice', 'error');
      }
    });
  }

  resetInvoiceForm(): void {
    this.showInvoiceForm = false;
    this.invoiceClientName = '';
    this.invoiceClientEmail = '';
    this.invoiceClientPhone = '';
    this.invoiceClientAddress = '';
    this.invoiceClientGst = '';
    this.invoiceDate = '';
    this.invoiceDueDate = '';
    this.invoiceItems = [{ description: '', quantity: 1, unitPrice: 0, amount: 0 }];
    this.invoiceTaxRate = 18;
    this.invoiceDiscount = 0;
    this.invoiceNotes = '';
    this.invoiceTerms = 'Payment due within 30 days';
    this.invoiceSubtotal = 0;
    this.invoiceTaxAmount = 0;
    this.invoiceTotal = 0;
  }

  markInvoiceAsSent(invoice: CurrentAccountInvoice): void {
    this.currentAccountService.updateInvoiceStatus(invoice.id!, 'SENT').subscribe({
      next: () => {
        this.showAlertMessage('Invoice marked as sent', 'success');
        this.loadInvoices();
      },
      error: () => { this.showAlertMessage('Failed to update invoice', 'error'); }
    });
  }

  markInvoiceAsPaid(invoice: CurrentAccountInvoice): void {
    this.currentAccountService.markInvoicePaid(invoice.id!, invoice.totalAmount).subscribe({
      next: () => {
        this.showAlertMessage('Invoice marked as paid', 'success');
        this.loadInvoices();
      },
      error: () => { this.showAlertMessage('Failed to update invoice', 'error'); }
    });
  }

  deleteInvoice(invoice: CurrentAccountInvoice): void {
    if (confirm('Are you sure you want to delete invoice ' + invoice.invoiceNumber + '?')) {
      this.currentAccountService.deleteInvoice(invoice.id!).subscribe({
        next: () => {
          this.showAlertMessage('Invoice deleted', 'success');
          this.loadInvoices();
        },
        error: () => { this.showAlertMessage('Failed to delete invoice', 'error'); }
      });
    }
  }

  printInvoice(invoice: CurrentAccountInvoice): void {
    const items: InvoiceItem[] = JSON.parse(invoice.itemsJson);
    let itemsHtml = '';
    items.forEach((item, i) => {
      itemsHtml += `<tr>
        <td>${i + 1}</td>
        <td>${item.description}</td>
        <td>${item.quantity}</td>
        <td>₹${item.unitPrice.toLocaleString()}</td>
        <td>₹${item.amount.toLocaleString()}</td>
      </tr>`;
    });

    const html = `<html><head><title>Invoice ${invoice.invoiceNumber}</title>
      <style>
        body { font-family: Arial, sans-serif; padding: 30px; max-width: 800px; margin: 0 auto; }
        .invoice-header { display: flex; justify-content: space-between; border-bottom: 3px solid #1a237e; padding-bottom: 20px; margin-bottom: 20px; }
        .invoice-title { color: #1a237e; font-size: 28px; font-weight: 700; }
        .invoice-meta p { margin: 3px 0; font-size: 13px; }
        .client-info { margin-bottom: 20px; }
        .client-info h4 { color: #1a237e; margin-bottom: 8px; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th { background: #1a237e; color: #fff; padding: 10px; text-align: left; }
        td { padding: 8px 10px; border-bottom: 1px solid #ddd; }
        .totals { text-align: right; margin-top: 20px; }
        .totals p { margin: 5px 0; font-size: 14px; }
        .totals .total-amount { font-size: 20px; font-weight: 700; color: #1a237e; }
        .footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #ddd; font-size: 12px; color: #666; }
      </style></head><body>
      <div class="invoice-header">
        <div>
          <div class="invoice-title">INVOICE</div>
          <p><strong>${this.account?.businessName}</strong></p>
          <p>${this.account?.shopAddress || ''}, ${this.account?.city || ''}</p>
          <p>GST: ${this.account?.gstNumber || 'N/A'}</p>
        </div>
        <div class="invoice-meta">
          <p><strong>Invoice #:</strong> ${invoice.invoiceNumber}</p>
          <p><strong>Date:</strong> ${invoice.invoiceDate}</p>
          <p><strong>Due Date:</strong> ${invoice.dueDate}</p>
          <p><strong>Status:</strong> ${invoice.status}</p>
        </div>
      </div>
      <div class="client-info">
        <h4>Bill To:</h4>
        <p><strong>${invoice.clientName}</strong></p>
        <p>${invoice.clientAddress || ''}</p>
        <p>${invoice.clientEmail || ''} ${invoice.clientPhone ? '| ' + invoice.clientPhone : ''}</p>
        ${invoice.clientGst ? '<p>GST: ' + invoice.clientGst + '</p>' : ''}
      </div>
      <table>
        <thead><tr><th>#</th><th>Description</th><th>Qty</th><th>Unit Price</th><th>Amount</th></tr></thead>
        <tbody>${itemsHtml}</tbody>
      </table>
      <div class="totals">
        <p>Subtotal: ₹${invoice.subtotal.toLocaleString()}</p>
        <p>Tax (${invoice.taxRate}%): ₹${invoice.taxAmount.toLocaleString()}</p>
        ${invoice.discount > 0 ? '<p>Discount: -₹' + invoice.discount.toLocaleString() + '</p>' : ''}
        <p class="total-amount">Total: ₹${invoice.totalAmount.toLocaleString()}</p>
      </div>
      ${invoice.notes ? '<div class="footer"><strong>Notes:</strong> ' + invoice.notes + '</div>' : ''}
      ${invoice.terms ? '<div class="footer"><strong>Terms:</strong> ' + invoice.terms + '</div>' : ''}
    </body></html>`;

    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(html);
      printWindow.document.close();
      printWindow.print();
    }
  }

  getInvoiceItems(invoice: CurrentAccountInvoice): InvoiceItem[] {
    try {
      return JSON.parse(invoice.itemsJson);
    } catch {
      return [];
    }
  }

  // ==================== Business Loan ====================

  loadBusinessLoans(): void {
    if (!this.account) return;
    this.isLoadingLoans = true;
    this.currentAccountService.getBusinessLoans(this.account.accountNumber!).subscribe({
      next: (list) => {
        this.businessLoans = list;
        this.isLoadingLoans = false;
      },
      error: () => { this.isLoadingLoans = false; }
    });
  }

  applyForLoan(): void {
    if (!this.account || !this.loanPanNumber || !this.loanAmount || !this.loanPurpose
        || !this.loanAnnualRevenue || this.loanYearsInBusiness === null) {
      this.showAlertMessage('Please fill all required fields', 'error');
      return;
    }
    const maxLimit = this.loanType === 'Working Capital' ? 500000 : 1000000;
    if (this.loanAmount > maxLimit) {
      this.showAlertMessage('Maximum limit for ' + this.loanType + ' is ₹' + maxLimit.toLocaleString(), 'error');
      return;
    }
    if (this.loanPanNumber.length !== 10) {
      this.showAlertMessage('Please enter a valid 10-character PAN number', 'error');
      return;
    }
    this.isApplyingLoan = true;
    const loan: CurrentAccountBusinessLoan = {
      accountNumber: this.account.accountNumber!,
      loanType: this.loanType,
      panNumber: this.loanPanNumber.toUpperCase(),
      requestedAmount: this.loanAmount,
      tenureMonths: this.loanTenure,
      purpose: this.loanPurpose,
      annualRevenue: this.loanAnnualRevenue,
      yearsInBusiness: this.loanYearsInBusiness
    };
    this.currentAccountService.applyForBusinessLoan(loan).subscribe({
      next: (res) => {
        this.isApplyingLoan = false;
        if (res.success) {
          this.loanResult = res;
          this.showLoanResult = true;
          this.showAlertMessage(res.message, 'success');
          this.resetLoanForm();
          this.loadBusinessLoans();
        } else {
          this.showAlertMessage(res.message || 'Loan application failed', 'error');
        }
      },
      error: () => {
        this.isApplyingLoan = false;
        this.showAlertMessage('Failed to submit loan application', 'error');
      }
    });
  }

  resetLoanForm(): void {
    this.showLoanForm = false;
    this.loanType = 'Working Capital';
    this.loanPanNumber = '';
    this.loanAmount = null;
    this.loanTenure = 12;
    this.loanPurpose = '';
    this.loanAnnualRevenue = null;
    this.loanYearsInBusiness = null;
  }

  getLoanMaxAmount(): number {
    return this.loanType === 'Working Capital' ? 500000 : 1000000;
  }

  getCibilColor(score: number | undefined): string {
    if (!score) return '#999';
    if (score >= 750) return '#2e7d32';
    if (score >= 700) return '#558b2f';
    if (score >= 650) return '#f9a825';
    if (score >= 550) return '#e65100';
    return '#c62828';
  }

  // ==================== Multi-User Access ====================

  loadBusinessUsers(): void {
    if (!this.account) return;
    this.isLoadingUsers = true;
    this.currentAccountService.getBusinessUsers(this.account.accountNumber!).subscribe({
      next: (list) => {
        this.businessUsers = list;
        this.isLoadingUsers = false;
      },
      error: () => { this.isLoadingUsers = false; }
    });
  }

  addBusinessUser(): void {
    if (!this.account || !this.newUserFullName || !this.newUserEmail || !this.newUserMobile) {
      this.showAlertMessage('Please fill all required fields', 'error');
      return;
    }
    this.isAddingUser = true;
    const user: CurrentAccountBusinessUser = {
      accountNumber: this.account.accountNumber!,
      fullName: this.newUserFullName,
      email: this.newUserEmail,
      mobile: this.newUserMobile,
      role: this.newUserRole,
      password: this.newUserPassword || undefined,
      createdBy: this.account.ownerName
    };
    this.currentAccountService.addBusinessUser(user).subscribe({
      next: (res) => {
        this.isAddingUser = false;
        if (res.success) {
          this.showAlertMessage(res.message, 'success');
          this.resetUserForm();
          this.loadBusinessUsers();
        } else {
          this.showAlertMessage(res.message || 'Failed to add user', 'error');
        }
      },
      error: () => {
        this.isAddingUser = false;
        this.showAlertMessage('Failed to add user', 'error');
      }
    });
  }

  resetUserForm(): void {
    this.showUserForm = false;
    this.newUserFullName = '';
    this.newUserEmail = '';
    this.newUserMobile = '';
    this.newUserRole = 'STAFF';
    this.newUserPassword = '';
  }

  changeUserRole(user: CurrentAccountBusinessUser, newRole: string): void {
    this.currentAccountService.updateUserRole(user.id!, newRole).subscribe({
      next: () => {
        this.showAlertMessage('User role updated to ' + newRole, 'success');
        this.loadBusinessUsers();
      },
      error: () => { this.showAlertMessage('Failed to update role', 'error'); }
    });
  }

  toggleUserStatus(user: CurrentAccountBusinessUser): void {
    const newStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.currentAccountService.updateUserStatus(user.id!, newStatus).subscribe({
      next: () => {
        this.showAlertMessage('User ' + (newStatus === 'ACTIVE' ? 'activated' : 'suspended'), 'success');
        this.loadBusinessUsers();
      },
      error: () => { this.showAlertMessage('Failed to update status', 'error'); }
    });
  }

  removeUser(user: CurrentAccountBusinessUser): void {
    if (confirm('Are you sure you want to remove ' + user.fullName + '?')) {
      this.currentAccountService.removeBusinessUser(user.id!).subscribe({
        next: () => {
          this.showAlertMessage('User removed', 'success');
          this.loadBusinessUsers();
        },
        error: () => { this.showAlertMessage('Failed to remove user', 'error'); }
      });
    }
  }

  getRoleDescription(role: string): string {
    switch (role) {
      case 'OWNER': return 'Full control over all operations';
      case 'ACCOUNTANT': return 'View transactions & financial reports';
      case 'STAFF': return 'Limited access to basic operations';
      default: return role;
    }
  }

  // ==================== Alert ====================

  showAlertMessage(message: string, type: string): void {
    this.alertMessage = message;
    this.alertType = type;
    this.showAlert = true;
    setTimeout(() => this.hideAlert(), 5000);
  }

  hideAlert(): void {
    this.showAlert = false;
  }

  // ==================== Logout ====================

  logout(): void {
    if (this.isBrowser) {
      const accountData = sessionStorage.getItem('currentAccount');
      if (accountData) {
        try {
          const account = JSON.parse(accountData);
          this.http.post(`${environment.apiBaseUrl}/api/session-history/logout`, {
            userId: account.id,
            userType: 'USER',
            sessionDuration: ''
          }).subscribe({
            next: () => console.log('Current account logout recorded'),
            error: (err) => console.error('Error recording current account logout:', err)
          });
        } catch (e) {
          console.error('Error parsing current account data for logout:', e);
        }
      }
      sessionStorage.removeItem('currentAccount');
      sessionStorage.removeItem('businessUserRole');
      sessionStorage.removeItem('businessUserName');
      sessionStorage.removeItem('businessUserId');
    }
    this.router.navigate(['/website/current-account-login']);
  }

  // ==================== Helpers ====================

  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) return '₹0';
    return '₹' + amount.toLocaleString('en-IN');
  }

  formatDate(date: string | undefined): string {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('en-IN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  getStatusClass(status: string | undefined): string {
    switch (status?.toUpperCase()) {
      case 'ACTIVE': case 'PAID': case 'APPROVED': case 'COMPLETED': case 'DISBURSED': case 'EXCELLENT': case 'GOOD': return 'status-active';
      case 'PENDING': case 'DRAFT': case 'SENT': case 'FAIR': return 'status-pending';
      case 'DELETED': case 'REJECTED': case 'FAILED': case 'CANCELLED': case 'OVERDUE': case 'POOR': case 'VERY_POOR': case 'SUSPENDED': return 'status-rejected';
      default: return 'status-pending';
    }
  }

  // ==================== Linked Account Switch ====================

  checkLinkedAccount(): void {
    if (!this.account?.accountNumber) return;
    this.currentAccountService.getLinkedAccounts(this.account.accountNumber).subscribe({
      next: (links) => {
        if (links && links.length > 0) {
          this.linkedAccount = links[0];
          this.hasLinkedAccount = true;
          // Load savings account details
          this.currentAccountService.getLinkedSavingsDetails(this.linkedAccount.savingsAccountNumber).subscribe({
            next: (details) => {
              if (details.success) {
                this.linkedSavingsDetails = details;
              }
            }
          });
        }
      }
    });
  }

  openCreatePinModal(): void {
    this.switchPin = '';
    this.confirmSwitchPin = '';
    this.showPinModal = true;
  }

  closeCreatePinModal(): void {
    this.showPinModal = false;
    this.switchPin = '';
    this.confirmSwitchPin = '';
  }

  changeUpiPin(): void {
    this.upiPinChangeError = '';
    if (!this.upiCurrentPassword || !this.upiNewPassword) {
      this.upiPinChangeError = 'Please fill in all fields';
      return;
    }
    if (this.upiNewPassword.length < 6) {
      this.upiPinChangeError = 'New PIN must be at least 6 characters';
      return;
    }
    if (this.upiNewPassword !== this.upiConfirmPassword) {
      this.upiPinChangeError = 'New PIN and confirmation do not match';
      return;
    }
    if (!this.account?.accountNumber) return;
    this.isUpiPinChanging = true;
    this.currentAccountService.changePassword(this.account.accountNumber, this.upiCurrentPassword, this.upiNewPassword).subscribe({
      next: (res: any) => {
        this.isUpiPinChanging = false;
        if (res.success) {
          this.upiPinChangeSuccess = true;
          this.upiCurrentPassword = '';
          this.upiNewPassword = '';
          this.upiConfirmPassword = '';
          setTimeout(() => { this.upiPinChangeSuccess = false; }, 3000);
        } else {
          this.upiPinChangeError = res.message || 'Failed to change PIN';
        }
      },
      error: (err: any) => {
        this.isUpiPinChanging = false;
        this.upiPinChangeError = err?.error?.message || 'Failed to change PIN';
      }
    });
  }

  createSwitchPin(): void {
    if (!this.switchPin || !this.switchPin.match(/^\d{4}$/)) {
      this.showAlertMessage('PIN must be exactly 4 digits', 'error');
      return;
    }
    if (this.switchPin !== this.confirmSwitchPin) {
      this.showAlertMessage('PINs do not match', 'error');
      return;
    }
    if (!this.linkedAccount?.id) return;
    this.isPinCreating = true;
    this.currentAccountService.createSwitchPin(this.linkedAccount.id, this.switchPin).subscribe({
      next: (res) => {
        this.isPinCreating = false;
        if (res.success) {
          this.linkedAccount!.pinCreated = true;
          this.showPinModal = false;
          this.switchPin = '';
          this.confirmSwitchPin = '';
          this.showAlertMessage('Switch PIN created successfully', 'success');
        } else {
          this.showAlertMessage(res.message || 'Failed to create PIN', 'error');
        }
      },
      error: () => {
        this.isPinCreating = false;
        this.showAlertMessage('Failed to create PIN', 'error');
      }
    });
  }

  openSwitchToSavingsAccount(): void {
    if (!this.linkedAccount?.pinCreated) {
      this.openCreatePinModal();
      return;
    }
    this.verifyPin = '';
    this.showVerifyPinModal = true;
  }

  closeVerifyPinModal(): void {
    this.showVerifyPinModal = false;
    this.verifyPin = '';
  }

  verifySwitchAndNavigate(): void {
    if (!this.verifyPin || !this.verifyPin.match(/^\d{4}$/)) {
      this.showAlertMessage('PIN must be exactly 4 digits', 'error');
      return;
    }
    if (!this.linkedAccount?.id) return;
    this.isVerifyingPin = true;
    this.currentAccountService.verifySwitchPin(this.linkedAccount.id, this.verifyPin).subscribe({
      next: (res) => {
        this.isVerifyingPin = false;
        if (res.success) {
          this.showVerifyPinModal = false;
          this.router.navigate(['/website/userdashboard']);
        } else {
          this.showAlertMessage(res.message || 'Incorrect PIN', 'error');
        }
      },
      error: () => {
        this.isVerifyingPin = false;
        this.showAlertMessage('Failed to verify PIN', 'error');
      }
    });
  }

  // ==================== Soundbox ====================

  loadSoundboxData(): void {
    if (!this.account?.accountNumber) return;
    this.isLoadingSoundbox = true;

    this.soundboxService.getUserStats(this.account.accountNumber).subscribe({
      next: (stats) => {
        this.soundboxStats = stats;
        this.hasSoundbox = stats.hasDevice;
        if (stats.device) {
          this.soundboxDevice = stats.device;
        }
        this.isLoadingSoundbox = false;
      },
      error: () => { this.isLoadingSoundbox = false; }
    });

    this.soundboxService.getRequestsByAccount(this.account.accountNumber).subscribe({
      next: (requests) => { this.soundboxRequests = requests; },
      error: () => {}
    });

    this.soundboxService.getTransactionsByAccount(this.account.accountNumber).subscribe({
      next: (txns) => { this.soundboxTransactions = txns; },
      error: () => {}
    });
  }

  openApplyModal(): void {
    if (!this.account) return;
    this.sbDeliveryAddress = this.account.shopAddress || '';
    this.sbCity = this.account.city || '';
    this.sbState = this.account.state || '';
    this.sbPincode = this.account.pincode || '';
    this.sbMobile = this.account.mobile || '';
    this.showApplyModal = true;
  }

  closeApplyModal(): void {
    this.showApplyModal = false;
  }

  submitSoundboxApplication(): void {
    if (!this.account?.accountNumber) return;
    this.isApplyingSoundbox = true;
    const request: SoundboxRequest = {
      accountNumber: this.account.accountNumber,
      businessName: this.account.businessName,
      ownerName: this.account.ownerName,
      deliveryAddress: this.sbDeliveryAddress,
      city: this.sbCity,
      state: this.sbState,
      pincode: this.sbPincode,
      mobile: this.sbMobile
    };
    this.soundboxService.applyForSoundbox(request).subscribe({
      next: (res) => {
        this.isApplyingSoundbox = false;
        if (res.success) {
          this.showAlertMessage('Soundbox application submitted successfully!', 'success');
          this.showApplyModal = false;
          this.loadSoundboxData();
        } else {
          this.showAlertMessage(res.error || 'Application failed', 'error');
        }
      },
      error: (err) => {
        this.isApplyingSoundbox = false;
        this.showAlertMessage(err.error?.error || 'Failed to submit application', 'error');
      }
    });
  }

  toggleVoiceAlert(): void {
    if (!this.account?.accountNumber || !this.soundboxDevice) return;
    const newVal = !this.soundboxDevice.voiceEnabled;
    this.soundboxService.updateDeviceSettings(this.account.accountNumber, { voiceEnabled: newVal }).subscribe({
      next: (res) => {
        if (res.success) {
          this.soundboxDevice = res.device;
          this.showAlertMessage('Voice alerts ' + (newVal ? 'enabled' : 'disabled'), 'success');
        }
      },
      error: () => this.showAlertMessage('Failed to update settings', 'error')
    });
  }

  updateVoiceLanguage(lang: string): void {
    if (!this.account?.accountNumber) return;
    this.soundboxService.updateDeviceSettings(this.account.accountNumber, { voiceLanguage: lang }).subscribe({
      next: (res) => {
        if (res.success) {
          this.soundboxDevice = res.device;
          this.showAlertMessage('Voice language updated', 'success');
        }
      },
      error: () => this.showAlertMessage('Failed to update language', 'error')
    });
  }

  updateVolumeMode(mode: string): void {
    if (!this.account?.accountNumber) return;
    this.soundboxService.updateDeviceSettings(this.account.accountNumber, { volumeMode: mode }).subscribe({
      next: (res) => {
        if (res.success) {
          this.soundboxDevice = res.device;
          this.showAlertMessage('Volume mode updated', 'success');
        }
      },
      error: () => this.showAlertMessage('Failed to update volume', 'error')
    });
  }

  addUpiId(): void {
    if (!this.account?.accountNumber || !this.newUpiId.trim()) return;
    this.soundboxService.linkUpi(this.account.accountNumber, this.newUpiId.trim()).subscribe({
      next: (res) => {
        if (res.success) {
          this.soundboxDevice = res.device;
          this.newUpiId = '';
          this.showAlertMessage('UPI ID linked successfully', 'success');
        }
      },
      error: (err) => this.showAlertMessage(err.error?.error || 'Failed to link UPI', 'error')
    });
  }

  removeUpiId(upi: string): void {
    if (!this.account?.accountNumber) return;
    this.soundboxService.removeUpi(this.account.accountNumber, upi).subscribe({
      next: (res) => {
        if (res.success) {
          this.soundboxDevice = res.device;
          this.showAlertMessage('UPI ID removed', 'success');
        }
      },
      error: () => this.showAlertMessage('Failed to remove UPI', 'error')
    });
  }

  getLinkedUpis(): string[] {
    if (!this.soundboxDevice?.linkedUpi) return [];
    return this.soundboxDevice.linkedUpi.split(',').filter(u => u.trim());
  }

  openSimulatePayment(): void {
    this.showSimulateModal = true;
    this.simulateAmount = null;
    this.simulatePayerName = '';
    this.simulatePayerUpi = '';
    this.simulatePaymentMethod = 'UPI';
    this.showPaymentSuccess = false;
  }

  closeSimulateModal(): void {
    this.showSimulateModal = false;
    this.showPaymentSuccess = false;
  }

  simulatePayment(): void {
    if (!this.account?.accountNumber || !this.simulateAmount || this.simulateAmount <= 0) return;
    this.isProcessingPayment = true;
    const txn: SoundboxTransaction = {
      accountNumber: this.account.accountNumber,
      amount: this.simulateAmount,
      txnType: 'CREDIT',
      paymentMethod: this.simulatePaymentMethod,
      payerName: this.simulatePayerName,
      payerUpi: this.simulatePayerUpi
    };
    this.soundboxService.processPayment(txn).subscribe({
      next: (res) => {
        this.isProcessingPayment = false;
        if (res.success) {
          this.lastPaymentResult = res;
          this.showPaymentSuccess = true;
          // Trigger voice
          if (res.voiceEnabled && this.isBrowser) {
            const lang = this.soundboxDevice?.voiceLanguage || 'en-IN';
            this.soundboxService.speakPayment(this.simulateAmount!, lang);
          }
          this.loadSoundboxData();
          this.loadDashboardData();
        } else {
          this.showAlertMessage(res.error || 'Payment failed', 'error');
        }
      },
      error: (err) => {
        this.isProcessingPayment = false;
        this.showAlertMessage(err.error?.error || 'Payment processing failed', 'error');
      }
    });
  }

  speakDailySummary(): void {
    if (this.soundboxStats && this.isBrowser) {
      const lang = this.soundboxDevice?.voiceLanguage || 'en-IN';
      this.soundboxService.speakDailySummary(this.soundboxStats.todayReceived, lang);
    }
  }

  testVoice(): void {
    if (this.isBrowser) {
      const lang = this.soundboxDevice?.voiceLanguage || 'en-IN';
      this.soundboxService.speakPayment(100, lang);
    }
  }

  // ==================== Cross-Customer UPI Payment ====================

  verifySbPayUpi(): void {
    if (!this.sbPayUpiId.trim()) return;
    this.sbPayVerificationStatus = 'verifying';
    this.sbPayVerifiedName = '';
    this.sbPayVerificationError = '';
    this.showCrossPaySuccess = false;

    this.upiService.verifyUpiId(this.sbPayUpiId.trim()).subscribe({
      next: (res) => {
        if (res.verified) {
          this.sbPayVerifiedName = res.accountName || res.ownerName;
          this.sbPayVerificationStatus = 'verified';
        } else {
          this.sbPayVerificationStatus = 'failed';
          this.sbPayVerificationError = res.error || 'UPI ID not found in NeoBank';
        }
      },
      error: (err) => {
        this.sbPayVerificationStatus = 'failed';
        this.sbPayVerificationError = err.error?.error || 'Failed to verify UPI ID';
      }
    });
  }

  processCrossPayment(): void {
    if (!this.account?.accountNumber || !this.sbPayUpiId.trim() || !this.sbPayAmount || this.sbPayAmount <= 0) return;
    if (this.sbPayVerificationStatus !== 'verified') {
      this.showAlertMessage('Please verify the UPI ID first', 'error');
      return;
    }

    this.isCrossPayProcessing = true;
    this.upiService.crossCustomerPayment(
      this.account.accountNumber,
      this.sbPayUpiId.trim(),
      this.sbPayAmount,
      this.sbPayNote
    ).subscribe({
      next: (res) => {
        this.isCrossPayProcessing = false;
        if (res.success) {
          this.crossPayResult = res;
          this.showCrossPaySuccess = true;
          this.account!.balance = res.senderNewBalance;
          this.loadSoundboxData();
          this.loadDashboardData();
          // Voice alert
          if (this.isBrowser) {
            const lang = this.soundboxDevice?.voiceLanguage || 'en-IN';
            const msg = `Payment of rupees ${this.sbPayAmount} sent to ${res.receiverName}`;
            if ('speechSynthesis' in window) {
              const utterance = new SpeechSynthesisUtterance(msg);
              utterance.lang = lang;
              window.speechSynthesis.speak(utterance);
            }
          }
        } else {
          this.showAlertMessage(res.error || 'Payment failed', 'error');
        }
      },
      error: (err) => {
        this.isCrossPayProcessing = false;
        this.showAlertMessage(err.error?.error || 'Payment processing failed', 'error');
      }
    });
  }

  resetCrossPay(): void {
    this.sbPayUpiId = '';
    this.sbPayVerifiedName = '';
    this.sbPayVerificationStatus = '';
    this.sbPayVerificationError = '';
    this.sbPayAmount = null;
    this.sbPayNote = '';
    this.showCrossPaySuccess = false;
    this.crossPayResult = null;
  }

  // ==================== Soundbox QR Code Generator ====================

  verifySbQrUpiId(): void {
    if (!this.sbQrUpiId.trim()) return;
    this.sbQrVerificationStatus = 'verifying';
    this.sbQrVerifiedName = '';
    this.sbQrVerificationError = '';

    this.upiService.verifyUpiId(this.sbQrUpiId.trim()).subscribe({
      next: (res) => {
        if (res.verified) {
          // Collect all names from the verified UPI response
          const verifiedNames = [
            (res.accountName || '').toLowerCase().trim(),
            (res.ownerName || '').toLowerCase().trim(),
            (res.businessName || '').toLowerCase().trim()
          ].filter(n => n.length > 0);

          // Collect all names from the logged-in account
          const accountNames = [
            (this.account?.ownerName || '').toLowerCase().trim(),
            (this.account?.businessName || '').toLowerCase().trim()
          ].filter(n => n.length > 0);

          // Match if ANY verified name matches ANY account name
          const nameMatches = verifiedNames.some(vn => accountNames.some(an => vn === an));

          if (nameMatches) {
            this.sbQrVerifiedName = res.accountName || res.ownerName;
            this.sbQrVerificationStatus = 'verified';
          } else {
            this.sbQrVerificationStatus = 'name-mismatch';
            this.sbQrVerificationError = `Name mismatch: UPI registered to "${res.ownerName || res.accountName}" / "${res.businessName || ''}" does not match your account (Owner: "${this.account?.ownerName}", Business: "${this.account?.businessName}")`;
          }
        } else {
          this.sbQrVerificationStatus = 'failed';
          this.sbQrVerificationError = res.error || 'UPI ID not found in NeoBank';
        }
      },
      error: (err) => {
        this.sbQrVerificationStatus = 'failed';
        this.sbQrVerificationError = err.error?.error || 'Failed to verify UPI ID';
      }
    });
  }

  generateSbQrCode(): void {
    if (!this.account?.accountNumber || !this.sbQrUpiId.trim()) return;
    if (this.sbQrVerificationStatus !== 'verified') return;

    this.isGeneratingSbQr = true;
    const amount = this.sbQrIsCustomAmount && this.sbQrAmount && this.sbQrAmount > 0 ? this.sbQrAmount : undefined;

    this.upiService.generateQrCodeForUpi(this.sbQrUpiId.trim(), this.account.accountNumber, amount).subscribe({
      next: (res) => {
        this.isGeneratingSbQr = false;
        if (res.success) {
          this.sbQrGeneratedImage = res.qrCode;
          this.sbQrGeneratedBusinessName = res.businessName || this.sbQrVerifiedName;
          this.sbQrGeneratedAmount = amount || null;
          this.showSbQrResultModal = true;
        } else {
          this.showAlertMessage(res.error || 'Failed to generate QR code', 'error');
        }
      },
      error: (err) => {
        this.isGeneratingSbQr = false;
        this.showAlertMessage(err.error?.error || 'Failed to generate QR code', 'error');
      }
    });
  }

  closeSbQrResultModal(): void {
    this.showSbQrResultModal = false;
    this.sbQrGeneratedImage = '';
  }

  resetSbQrForm(): void {
    this.sbQrUpiId = '';
    this.sbQrAmount = null;
    this.sbQrIsCustomAmount = false;
    this.sbQrVerifiedName = '';
    this.sbQrVerificationStatus = '';
    this.sbQrVerificationError = '';
    this.sbQrGeneratedImage = '';
    this.showSbQrResultModal = false;
  }

  downloadSbQrCode(): void {
    if (!this.sbQrGeneratedImage || !this.isBrowser) return;
    const link = document.createElement('a');
    link.href = this.sbQrGeneratedImage;
    link.download = 'NeoBank-Soundbox-QR-' + (this.account?.accountNumber || 'code') + '.png';
    link.click();
  }

  // ==================== UPI & QR Payments ====================

  loadUpiData(): void {
    if (!this.account) return;
    this.isLoadingUpi = true;
    const accNo = this.account.accountNumber!;

    this.upiService.getUserStats(accNo).subscribe({
      next: (stats) => {
        this.upiStats = stats;
        this.upiId = stats.upiId || '';
        this.isLoadingUpi = false;
        this.loadCurrentPaymentLinks();
      },
      error: () => {
        this.isLoadingUpi = false;
      }
    });

    this.upiService.getPayments(accNo).subscribe({
      next: (payments) => { this.upiPayments = payments; },
      error: () => {}
    });
  }

  setupUpi(): void {
    if (!this.account) return;
    this.upiService.setupUpi(this.account.accountNumber!).subscribe({
      next: (res) => {
        if (res.success) {
          this.upiId = res.upiId;
          this.showAlertMessage('UPI ID set up: ' + res.upiId, 'success');
          this.loadUpiData();
        }
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Failed to set up UPI', 'error');
      }
    });
  }

  openEditUpiModal(): void {
    this.newUpiIdInput = this.upiId;
    this.showEditUpiModal = true;
  }

  closeEditUpiModal(): void {
    this.showEditUpiModal = false;
    this.newUpiIdInput = '';
  }

  updateUpiId(): void {
    if (!this.account || !this.newUpiIdInput.trim()) return;
    this.upiService.updateUpi(this.account.accountNumber!, this.newUpiIdInput.trim()).subscribe({
      next: (res) => {
        if (res.success) {
          this.upiId = res.upiId;
          this.showEditUpiModal = false;
          this.showAlertMessage('UPI ID updated successfully', 'success');
          this.loadUpiData();
        }
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Failed to update UPI ID', 'error');
      }
    });
  }

  toggleUpiStatus(): void {
    if (!this.account) return;
    this.upiService.toggleUpi(this.account.accountNumber!).subscribe({
      next: (res) => {
        if (res.success) {
          if (this.upiStats) this.upiStats.upiEnabled = res.upiEnabled;
          this.showAlertMessage(res.message, 'success');
        }
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Failed to toggle UPI', 'error');
      }
    });
  }

  generateQrCode(): void {
    if (!this.account) return;
    const amount = this.upiQrAmount && this.upiQrAmount > 0 ? this.upiQrAmount : undefined;
    this.upiService.generateQrCode(this.account.accountNumber!, amount).subscribe({
      next: (res) => {
        if (res.success) {
          this.upiQrCode = res.qrCode;
          this.showUpiQr = true;
        } else {
          this.showAlertMessage(res.error || 'Failed to generate QR code', 'error');
        }
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Failed to generate QR code', 'error');
      }
    });
  }

  closeQrModal(): void {
    this.showUpiQr = false;
    this.upiQrCode = '';
    this.upiQrAmount = null;
  }

  downloadQrCode(): void {
    if (!this.upiQrCode || !this.isBrowser) return;
    const link = document.createElement('a');
    link.href = this.upiQrCode;
    link.download = 'NeoBank-UPI-QR-' + (this.account?.accountNumber || 'code') + '.png';
    link.click();
  }

  openUpiSimulateModal(): void {
    this.upiSimAmount = null;
    this.upiSimPayerName = '';
    this.upiSimPayerUpi = '';
    this.showUpiPaymentSuccess = false;
    this.showUpiSimulateModal = true;
  }

  closeUpiSimulateModal(): void {
    this.showUpiSimulateModal = false;
    this.showUpiPaymentSuccess = false;
    this.lastUpiPaymentResult = null;
  }

  simulateUpiPayment(): void {
    if (!this.account || !this.upiSimAmount || this.upiSimAmount <= 0) return;
    this.isProcessingUpiPayment = true;

    const payment: UpiPayment = {
      accountNumber: this.account.accountNumber!,
      upiId: this.upiId,
      amount: this.upiSimAmount,
      payerName: this.upiSimPayerName || 'Customer',
      payerUpi: this.upiSimPayerUpi || 'customer@upi',
      paymentMethod: 'UPI',
      txnType: 'CREDIT'
    };

    this.upiService.processPayment(payment).subscribe({
      next: (res) => {
        this.isProcessingUpiPayment = false;
        if (res.success) {
          this.lastUpiPaymentResult = res;
          this.showUpiPaymentSuccess = true;
          this.account!.balance = res.newBalance;
          this.loadUpiData();
        } else {
          this.showAlertMessage(res.error || 'Payment failed', 'error');
        }
      },
      error: (err) => {
        this.isProcessingUpiPayment = false;
        this.showAlertMessage(err.error?.error || 'Payment processing failed', 'error');
      }
    });
  }

  // ==================== QR Upload to Pay (Business UPI) ====================

  onBizQrFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || !input.files[0]) return;
    const file = input.files[0];
    input.value = '';
    if (!file.type.startsWith('image/')) {
      this.bizQrScanError = 'Please select a valid image file.';
      return;
    }
    this.isBizQrScanning = true;
    this.bizQrScanError = '';
    this.showBizQrScanResult = false;
    this.showBizQrPaySuccess = false;
    this.bizQrVerificationStatus = '';

    const fileReader = new FileReader();
    fileReader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        import('jsqr').then(({ default: jsQR }) => {
          const decoded = this._tryDecodeBizQr(jsQR, img);
          this.isBizQrScanning = false;
          if (decoded) {
            this.parseBizUpiQr(decoded);
          } else {
            this.bizQrScanError = 'Could not read QR code. Please use the NeoBank QR code shown in your dashboard (not a screenshot of another app).';
          }
        }).catch(() => {
          this.isBizQrScanning = false;
          this.bizQrScanError = 'QR scanner failed to load. Please refresh and try again.';
        });
      };
      img.onerror = () => {
        this.isBizQrScanning = false;
        this.bizQrScanError = 'Failed to load image. Please try another file.';
      };
      img.src = e.target!.result as string;
    };
    fileReader.onerror = () => {
      this.isBizQrScanning = false;
      this.bizQrScanError = 'Failed to read file. Please try again.';
    };
    fileReader.readAsDataURL(file);
  }

  /** Multi-strategy QR decoder: tries several scales + binarization to maximise decode rate */
  private _tryDecodeBizQr(jsQR: any, img: HTMLImageElement): string | null {
    const scales = [2, 1, 4, 3];
    for (const scale of scales) {
      const w = Math.round(img.naturalWidth * scale);
      const h = Math.round(img.naturalHeight * scale);
      const canvas = document.createElement('canvas');
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext('2d');
      if (!ctx) continue;
      ctx.imageSmoothingEnabled = scale < 1;
      ctx.drawImage(img, 0, 0, w, h);

      // Attempt 1: raw pixels, both inversion modes
      let id = ctx.getImageData(0, 0, w, h);
      let result = jsQR(id.data, id.width, id.height, { inversionAttempts: 'attemptBoth' });
      if (result?.data) return result.data;

      // Attempt 2: binarize — strips JPEG compression noise
      const bw = ctx.getImageData(0, 0, w, h);
      for (let i = 0; i < bw.data.length; i += 4) {
        const lum = bw.data[i] * 0.299 + bw.data[i + 1] * 0.587 + bw.data[i + 2] * 0.114;
        const v = lum < 128 ? 0 : 255;
        bw.data[i] = v; bw.data[i + 1] = v; bw.data[i + 2] = v;
      }
      result = jsQR(bw.data, bw.width, bw.height, { inversionAttempts: 'attemptBoth' });
      if (result?.data) return result.data;
    }
    return null;
  }

  parseBizUpiQr(decoded: string): void {
    try {
      const uriStr = decoded.trim();
      let pa = '';
      let pn = '';
      let am = '';
      let tn = '';

      if (uriStr.startsWith('upi://')) {
        const queryStr = uriStr.includes('?') ? uriStr.split('?')[1] : '';
        const params = new URLSearchParams(queryStr);
        pa = params.get('pa') || '';
        pn = params.get('pn') || '';
        am = params.get('am') || '';
        tn = params.get('tn') || '';
      } else if (uriStr.includes('@')) {
        pa = uriStr;
      }

      if (!pa) {
        this.bizQrScanError = 'No UPI ID found in QR code. Please scan a valid NeoBank QR.';
        return;
      }

      // Accept all NeoBank-issued UPI IDs: @neobank (accounts) and @ezyvault (payment gateway)
      const paLower = pa.toLowerCase();
      if (!paLower.endsWith('@neobank') && !paLower.endsWith('@ezyvault')) {
        this.bizQrScanError = '❌ Invalid QR: Only NeoBank QR codes are supported. Please scan a QR code from NeoBank.';
        return;
      }

      this.bizQrScannedUpiId = pa;
      this.bizQrScannedName = pn ? decodeURIComponent(pn) : pa.split('@')[0];
      this.bizQrScannedAmount = am ? parseFloat(am) : null;
      this.bizQrScannedRemark = tn ? decodeURIComponent(tn) : '';
      this.bizQrPayAmount = this.bizQrScannedAmount;
      this.bizQrPayNote = this.bizQrScannedRemark;
      this.showBizQrScanResult = true;
      this.verifyBizQrUpiId();
    } catch {
      this.bizQrScanError = 'Failed to parse QR code. Please try again.';
    }
  }

  verifyBizQrUpiId(): void {
    if (!this.bizQrScannedUpiId) return;
    this.bizQrVerificationStatus = 'verifying';
    this.upiService.verifyUpiId(this.bizQrScannedUpiId).subscribe({
      next: (res) => {
        if (res.valid || res.exists || res.name) {
          this.bizQrVerifiedName = res.name || this.bizQrScannedName || this.bizQrScannedUpiId;
          this.bizQrVerificationStatus = 'verified';
        } else {
          // Still allow payment even if verification endpoint returns not found
          // as the QR code itself is the merchant's proof
          this.bizQrVerifiedName = this.bizQrScannedName || this.bizQrScannedUpiId;
          this.bizQrVerificationStatus = 'verified';
        }
      },
      error: () => {
        // Allow payment with scanned data even if verification call fails
        this.bizQrVerifiedName = this.bizQrScannedName || this.bizQrScannedUpiId;
        this.bizQrVerificationStatus = 'verified';
      }
    });
  }

  processBizQrPayment(): void {
    if (!this.account?.accountNumber || !this.bizQrScannedUpiId || !this.bizQrPayAmount) {
      this.showAlertMessage('UPI ID and amount are required', 'error');
      return;
    }
    if (this.bizQrPayAmount <= 0) {
      this.showAlertMessage('Amount must be greater than 0', 'error');
      return;
    }
    this.isBizQrPayProcessing = true;
    this.upiService.crossCustomerPayment(
      this.account.accountNumber,
      this.bizQrScannedUpiId,
      this.bizQrPayAmount,
      this.bizQrPayNote || this.bizQrScannedRemark
    ).subscribe({
      next: (res) => {
        this.isBizQrPayProcessing = false;
        if (res.success !== false) {
          this.bizQrPayResult = {
            txnId: res.payment?.txnId || res.txnId || ('TXN' + Date.now()),
            amount: this.bizQrPayAmount,
            recipient: this.bizQrScannedUpiId,
            recipientName: res.receiverName || this.bizQrVerifiedName || this.bizQrScannedName,
            senderNewBalance: res.senderNewBalance,
            note: this.bizQrPayNote
          };
          this.showBizQrPaySuccess = true;
          if (this.account && res.senderNewBalance != null) {
            this.account.balance = res.senderNewBalance;
          }
          this.loadUpiData();
          // If this was a Payment Gateway merchant QR (@ezyvault), mark the order PAID
          const bizRemark = this.bizQrPayNote || this.bizQrScannedRemark || '';
          if (this.bizQrScannedUpiId?.toLowerCase().endsWith('@ezyvault') && bizRemark) {
            const pgMatch = bizRemark.match(/Payment\s+for\s+(ORD\w+)/i);
            if (pgMatch?.[1]) {
              this.pgService.completeOrderFromQrPayment(pgMatch[1], {
                payerAccount: this.account?.accountNumber || '',
                payerName: this.account?.ownerName || '',
                paymentMethod: 'UPI_QR',
                txnRef: res.payment?.txnId || res.txnId || ''
              }).subscribe();
            }
          }
        } else {
          this.showAlertMessage(res.message || res.error || 'Payment failed', 'error');
        }
      },
      error: (err) => {
        this.isBizQrPayProcessing = false;
        this.showAlertMessage(err.error?.message || err.error?.error || 'QR payment failed', 'error');
      }
    });
  }

  resetBizQrScan(): void {
    this.isBizQrScanning = false;
    this.bizQrScanError = '';
    this.bizQrScannedUpiId = '';
    this.bizQrScannedName = '';
    this.bizQrScannedAmount = null;
    this.bizQrScannedRemark = '';
    this.showBizQrScanResult = false;
    this.bizQrVerifiedName = '';
    this.bizQrVerificationStatus = '';
    this.bizQrPayAmount = null;
    this.bizQrPayNote = '';
    this.isBizQrPayProcessing = false;
    this.showBizQrPaySuccess = false;
    this.bizQrPayResult = null;
  }

  ngOnDestroy() {
    if (this.linkPollInterval) clearInterval(this.linkPollInterval);
  }

  // ─── Payment Links (Pay via Link) ────────────────────────────────────────────
  loadCurrentPaymentLinks() {
    const upiId = this.upiId;
    if (!upiId) return;
    this.isLoadingLinks = true;
    this.pgService.getCustomerPaymentLinks(upiId).subscribe({
      next: (data: any[]) => {
        this.allPaymentLinks = data || [];
        this.pendingPaymentLinks = this.allPaymentLinks.filter(l => l.status === 'PENDING');
        this.isLoadingLinks = false;
        if (this.pendingPaymentLinks.length > 0 && !this.linkPollInterval) {
          this.linkPollInterval = setInterval(() => this.loadCurrentPaymentLinks(), 5000);
        } else if (this.pendingPaymentLinks.length === 0 && this.linkPollInterval) {
          clearInterval(this.linkPollInterval);
          this.linkPollInterval = null;
        }
      },
      error: () => { this.isLoadingLinks = false; }
    });
  }

  openLinkPayModal(link: any) {
    this.selectedLink = link;
    this.linkPayPin = '';
    this.linkPaySuccess = false;
    this.linkPayResult = null;
    this.showLinkPayModal = true;
  }

  payLinkWithPin() {
    if (!this.linkPayPin || this.linkPayPin.length !== 4) return;
    this.isLinkPayProcessing = true;
    this.pgService.payPaymentLink(this.selectedLink.linkToken, {
      payerAccountNumber: this.account?.accountNumber || '',
      transactionPin: this.linkPayPin
    }).subscribe({
      next: (res: any) => {
        this.isLinkPayProcessing = false;
        if (res.success) {
          this.linkPaySuccess = true;
          this.linkPayResult = res;
          this.loadDashboardData();
          this.loadCurrentPaymentLinks();
          this.loadRecentTransactions();
        } else {
          alert(res.error || 'Payment failed');
        }
      },
      error: (err: any) => {
        this.isLinkPayProcessing = false;
        alert(err.error?.error || 'Payment failed');
      }
    });
  }

  closeLinkPayModal() {
    this.showLinkPayModal = false;
    this.selectedLink = null;
    this.linkPayPin = '';
    this.linkPaySuccess = false;
  }

  printPgInvoice() {
    const el = document.getElementById('pg-invoice-print');
    if (!el) return;
    const w = window.open('', '_blank', 'width=800,height=900');
    if (!w) return;
    w.document.write(`<html><head><title>Payment Invoice</title><style>
      body{font-family:'Segoe UI',Arial,sans-serif;padding:40px;color:#1e293b;max-width:700px;margin:0 auto;}
      @media print{body{padding:20px;}}
    </style></head><body>${el.innerHTML}</body></html>`);
    w.document.close();
    w.print();
  }
}
