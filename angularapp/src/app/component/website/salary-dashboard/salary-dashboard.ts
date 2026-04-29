import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { SalaryAccountService } from '../../../service/salary-account.service';
import { AlertService } from '../../../service/alert.service';
import { PaymentGatewayService } from '../../../service/payment-gateway.service';
import { SalaryAccount, SalaryTransaction, SalaryNormalTransaction, SalaryLoginActivity, SalaryUpiTransaction, SalaryAdvanceRequest, SalaryFraudAlert } from '../../../model/salary-account/salary-account.model';
import { SubscriptionPaymentComponent } from '../subscription-payment/subscription-payment';
import { DrawChequeComponent } from './draw-cheque/draw-cheque.component';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-salary-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, SubscriptionPaymentComponent, DrawChequeComponent],
  templateUrl: './salary-dashboard.html',
  styleUrls: ['./salary-dashboard.css']
})
export class SalaryDashboard implements OnInit, OnDestroy {
  // Session
  employee: any = null;
  account: SalaryAccount | null = null;
  activeSection = 'overview';
  private loginRecorded = false;
  sessionStartTime: Date = new Date();
  currentSessionTimer: any = null;
  currentSessionDuration = '';

  // Sidebar Search
  sidebarSearchQuery = '';

  // Dashboard stats
  dashboardStats: any = {};
  isLoadingStats = true;

  // Salary Transactions
  salaryTransactions: SalaryTransaction[] = [];
  isLoadingSalaryTxns = false;

  // Normal Transactions (transfers/withdrawals)
  normalTransactions: SalaryNormalTransaction[] = [];
  isLoadingNormalTxns = false;
  txnFilter = 'all';

  // Transfer
  transferRecipient = '';
  transferIfsc = '';
  transferAmount: number | null = null;
  transferRemark = '';
  transferPin = '';
  isTransferring = false;

  // Recipient verification
  recipientName = '';
  recipientAccountType = '';
  recipientBankName = '';
  isVerifyingRecipient = false;
  recipientVerified = false;
  recipientNotFound = false;
  recipientMessage = '';

  // Transfer result
  showTransferResult = false;
  transferResult: any = null;

  // Withdraw
  withdrawAmount: number | null = null;
  withdrawPin = '';
  isWithdrawing = false;

  // Bank Charges
  bankCharges: any = {};
  isLoadingCharges = false;

  // Salary Slip
  selectedSlipTxn: SalaryTransaction | null = null;
  slipData: any = null;
  isLoadingSlip = false;

  // Profile / Settings
  currentPassword = '';
  newPassword = '';
  confirmNewPassword = '';
  isChangingPassword = false;

  // Transaction PIN
  pinPassword = '';
  newPin = '';
  confirmPin = '';
  isSettingPin = false;
  oldPin = '';
  changePinNew = '';
  changePinConfirm = '';
  isChangingPin = false;

  // Login Activity
  loginActivities: SalaryLoginActivity[] = [];
  isLoadingActivity = false;

  // Sidebar collapsed state
  sidebarCollapsed = false;

  // ─── UPI Integration ──────────────────────────────────────
  upiRecipient = '';
  upiAmount: number | null = null;
  upiRemark = '';
  upiPin = '';
  isSendingUpi = false;
  isEnablingUpi = false;
  upiTransactions: SalaryUpiTransaction[] = [];
  isLoadingUpiTxns = false;

  // ─── QR Scanner (Upload from Gallery) ─────────────────────
  isQrScanning = false;
  qrScanError = '';
  qrScannedUpiId = '';
  qrScannedName = '';
  qrScannedAmount: number | null = null;
  qrScannedRemark = '';
  showQrScanResult = false;
  qrPayPin = '';
  isQrPayProcessing = false;
  showQrPaySuccess = false;
  qrPaySuccessResult: any = null;

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

  // ─── Loan Eligibility ─────────────────────────────────────
  loanEligibility: any = {};
  isLoadingLoan = false;

  // ─── Auto Savings ─────────────────────────────────────────
  savingsInfo: any = {};
  isLoadingSavings = false;
  savingsPercentage: number | null = null;
  isTogglingSavings = false;
  savingsWithdrawAmount: number | null = null;
  savingsWithdrawPin = '';
  isWithdrawingSavings = false;

  // ─── Salary Advance ───────────────────────────────────────
  advanceInfo: any = {};
  isLoadingAdvance = false;
  advanceAmount: number | null = null;
  advanceReason = '';
  isRequestingAdvance = false;
  advanceHistory: SalaryAdvanceRequest[] = [];
  isLoadingAdvanceHistory = false;

  // ─── Debit Card Management ────────────────────────────────
  debitCardInfo: any = {};
  isLoadingCard = false;
  isUpdatingCard = false;
  isGeneratingCard = false;
  showFullCardNumber = false;
  showCvv = false;

  // ─── AI Fraud Detection ───────────────────────────────────
  fraudSummary: any = {};
  fraudAlerts: SalaryFraudAlert[] = [];
  isLoadingFraud = false;

  // ─── Gold Loan ────────────────────────────────────────────
  currentGoldRate: any = null;
  goldLoanGrams: number | null = null;
  goldLoanPurpose = '';
  goldLoanCalculation: any = null;
  isCalculatingGoldLoan = false;
  isApplyingGoldLoan = false;
  myGoldLoans: any[] = [];
  isLoadingGoldLoans = false;
  selectedGoldLoan: any = null;
  goldLoanEmiSchedule: any[] = [];
  isLoadingEmi = false;

  // ─── Employee Profile ─────────────────────────────────────
  profileAddress = '';
  profileMobile = '';
  profileEmail = '';
  isUpdatingProfile = false;

  constructor(
    private router: Router,
    private salaryService: SalaryAccountService,
    private alertService: AlertService,
    private pgService: PaymentGatewayService,
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    const sessionStr = sessionStorage.getItem('salaryEmployee');
    if (!sessionStr) {
      this.router.navigate(['/website/user']);
      return;
    }
    this.employee = JSON.parse(sessionStr);
    this.sessionStartTime = new Date(this.employee.loginTime || new Date().toISOString());
    this.startSessionTimer();
    this.loadAccount();
  }

  ngOnDestroy() {
    if (this.currentSessionTimer) clearInterval(this.currentSessionTimer);
    if (this.linkPollInterval) clearInterval(this.linkPollInterval);
  }

  startSessionTimer() {
    this.updateSessionDuration();
    this.currentSessionTimer = setInterval(() => this.updateSessionDuration(), 1000);
  }

  updateSessionDuration() {
    const diff = Math.floor((Date.now() - this.sessionStartTime.getTime()) / 1000);
    const hrs = Math.floor(diff / 3600);
    const mins = Math.floor((diff % 3600) / 60);
    const secs = diff % 60;
    this.currentSessionDuration = hrs > 0
      ? `${hrs}h ${mins}m ${secs}s`
      : mins > 0 ? `${mins}m ${secs}s` : `${secs}s`;
  }

  getBrowserName(): string {
    const ua = navigator.userAgent;
    if (ua.includes('Edg/')) return 'Microsoft Edge';
    if (ua.includes('OPR/') || ua.includes('Opera')) return 'Opera';
    if (ua.includes('Chrome/') && !ua.includes('Edg/')) return 'Google Chrome';
    if (ua.includes('Firefox/')) return 'Mozilla Firefox';
    if (ua.includes('Safari/') && !ua.includes('Chrome')) return 'Safari';
    return 'Unknown Browser';
  }

  getDeviceInfo(): string {
    const ua = navigator.userAgent;
    const platform = navigator.platform || 'Unknown';
    const isMobile = /Mobi|Android/i.test(ua);
    return `${isMobile ? 'Mobile' : 'Desktop'} - ${platform} - ${this.getBrowserName()}`;
  }

  recordLogin() {
    if (!this.account?.id || this.loginRecorded) return;
    this.loginRecorded = true;
    this.salaryService.recordActivity(this.account.id, 'LOGIN', this.getDeviceInfo(), this.getBrowserName()).subscribe();
  }

  loadAccount() {
    this.salaryService.getByAccountNumber(this.employee.accountNumber).subscribe({
      next: (acc) => {
        this.account = acc;
        this.recordLogin();
        this.loadDashboardStats();
        this.loadSalaryPaymentLinks();
      },
      error: () => {
        this.alertService.userError('Error', 'Failed to load account');
        this.router.navigate(['/website/user']);
      }
    });
  }

  loadDashboardStats() {
    if (!this.account?.id) return;
    this.isLoadingStats = true;
    this.salaryService.getDashboardStats(this.account.id).subscribe({
      next: (stats) => { this.dashboardStats = stats; this.isLoadingStats = false; },
      error: () => { this.isLoadingStats = false; }
    });
  }

  setActiveSection(section: string) {
    this.activeSection = section;
    if (section === 'salary-transactions') this.loadSalaryTransactions();
    else if (section === 'transactions') this.loadNormalTransactions();
    else if (section === 'transfer') { /* form ready */ }
    else if (section === 'withdraw') { /* form ready */ }
    else if (section === 'bank-charges') this.loadBankCharges();
    else if (section === 'salary-slip') this.loadSalaryTransactions();
    else if (section === 'login-activity') this.loadLoginActivity();
    else if (section === 'overview') { this.loadAccount(); }
    else if (section === 'upi') { this.loadUpiTransactions(); this.loadSalaryPaymentLinks(); }
    else if (section === 'loan-eligibility') { this.loadLoanEligibility(); }
    else if (section === 'auto-savings') { this.loadAutoSavingsInfo(); }
    else if (section === 'salary-advance') { this.loadAdvanceInfo(); this.loadAdvanceHistory(); }
    else if (section === 'debit-card') { this.loadDebitCardInfo(); }
    else if (section === 'fraud-detection') { this.loadFraudSummary(); }
    else if (section === 'gold-loan') { this.loadCurrentGoldRate(); this.loadMyGoldLoans(); }
    else if (section === 'employee-profile') { this.initProfileForm(); }
  }

  // ─── Salary Transactions ──────────────────────────────────

  loadSalaryTransactions() {
    if (!this.account?.id) return;
    this.isLoadingSalaryTxns = true;
    this.salaryService.getTransactions(this.account.id).subscribe({
      next: (txns) => { this.salaryTransactions = txns; this.isLoadingSalaryTxns = false; },
      error: () => { this.isLoadingSalaryTxns = false; }
    });
  }

  // ─── Normal Transactions ─────────────────────────────────

  loadNormalTransactions() {
    if (!this.account?.id) return;
    this.isLoadingNormalTxns = true;
    this.salaryService.getNormalTransactions(this.account.id).subscribe({
      next: (txns) => { this.normalTransactions = txns; this.isLoadingNormalTxns = false; },
      error: () => { this.isLoadingNormalTxns = false; }
    });
  }

  getFilteredTransactions(): SalaryNormalTransaction[] {
    if (this.txnFilter === 'all') return this.normalTransactions;
    return this.normalTransactions.filter(t => t.type === this.txnFilter);
  }

  // ─── Verify Recipient ─────────────────────────────────────

  verifyRecipient() {
    if (!this.transferRecipient || this.transferRecipient.length < 6) {
      this.recipientVerified = false;
      this.recipientNotFound = false;
      this.recipientName = '';
      return;
    }
    if (this.transferRecipient === this.account?.accountNumber) {
      this.alertService.userError('Invalid', 'Cannot transfer to your own account');
      return;
    }
    this.isVerifyingRecipient = true;
    this.recipientVerified = false;
    this.recipientNotFound = false;
    this.salaryService.verifyRecipientAccount(this.transferRecipient).subscribe({
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

  onRecipientChange() {
    this.recipientVerified = false;
    this.recipientNotFound = false;
    this.recipientName = '';
    this.recipientAccountType = '';
  }

  // ─── Transfer Money ───────────────────────────────────────

  transferMoney() {
    if (!this.transferRecipient || !this.transferAmount || !this.transferPin) {
      this.alertService.userError('Validation', 'Please fill all required fields');
      return;
    }
    if (this.transferAmount <= 0) {
      this.alertService.userError('Validation', 'Amount must be greater than 0');
      return;
    }
    if (!this.account?.transactionPinSet) {
      this.alertService.userError('PIN Required', 'Please set your transaction PIN first in Profile Settings');
      return;
    }
    this.isTransferring = true;
    this.salaryService.transferMoney(
      this.account!.id!, this.transferRecipient, this.transferIfsc,
      this.transferAmount, this.transferRemark, this.transferPin
    ).subscribe({
      next: (res) => {
        this.isTransferring = false;
        if (res.success) {
          this.transferResult = {
            transactionId: res.transaction?.id || '-',
            recipientAccount: this.transferRecipient,
            recipientName: this.recipientVerified ? this.recipientName : 'External Account',
            amount: this.transferAmount,
            charge: this.getTransferCharge(this.transferAmount!),
            total: this.transferAmount! + this.getTransferCharge(this.transferAmount!),
            newBalance: res.newBalance,
            remark: this.transferRemark || '-',
            date: new Date(),
            senderAccount: this.account?.accountNumber,
            senderName: this.account?.employeeName
          };
          this.showTransferResult = true;
          this.transferRecipient = ''; this.transferIfsc = '';
          this.transferAmount = null; this.transferRemark = ''; this.transferPin = '';
          this.recipientVerified = false; this.recipientName = '';
          this.loadAccount();
        } else {
          this.alertService.userError('Transfer Failed', res.message);
        }
      },
      error: (err) => {
        this.isTransferring = false;
        this.alertService.userError('Error', err.error?.message || 'Transfer failed');
      }
    });
  }

  closeTransferResult() {
    this.showTransferResult = false;
    this.transferResult = null;
  }

  downloadTransferReceipt() {
    if (!this.transferResult) return;
    const r = this.transferResult;
    const dateStr = new Date(r.date).toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });

    const html = `
    <html><head><title>Transfer Receipt - TXN#${r.transactionId}</title>
    <style>
      body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 40px; color: #1e293b; }
      .receipt { max-width: 600px; margin: 0 auto; border: 2px solid #6366f1; border-radius: 12px; overflow: hidden; }
      .receipt-header { background: linear-gradient(135deg, #6366f1, #4f46e5); color: white; padding: 24px 32px; display: flex; justify-content: space-between; align-items: center; }
      .receipt-header h1 { margin: 0; font-size: 22px; } .receipt-header .sub { font-size: 13px; opacity: .9; }
      .receipt-body { padding: 32px; }
      .receipt-title { text-align: center; font-size: 18px; font-weight: 700; margin-bottom: 24px; color: #16a34a; }
      .receipt-status { text-align: center; margin-bottom: 20px; }
      .receipt-status span { background: #dcfce7; color: #16a34a; padding: 6px 20px; border-radius: 20px; font-weight: 600; font-size: 14px; }
      .receipt-row { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f1f5f9; }
      .receipt-row .label { color: #64748b; font-size: 14px; } .receipt-row .value { font-weight: 600; font-size: 14px; color: #1e293b; }
      .receipt-amount { text-align: center; background: #f0fdf4; padding: 20px; border-radius: 10px; margin: 20px 0; border: 1px solid #bbf7d0; }
      .receipt-amount .label { font-size: 13px; color: #16a34a; } .receipt-amount .amount { font-size: 32px; font-weight: 700; color: #16a34a; }
      .receipt-footer { text-align: center; font-size: 12px; color: #94a3b8; padding: 16px 32px; border-top: 1px solid #e2e8f0; }
      @media print { body { padding: 0; } .receipt { border: none; } }
    </style></head><body>
    <div class="receipt">
      <div class="receipt-header"><div><h1>NEO BANK</h1><div class="sub">Digital Banking Solutions</div></div><div style="text-align:right;font-size:13px;">TXN #${r.transactionId}<br>${dateStr}</div></div>
      <div class="receipt-body">
        <div class="receipt-title">Transfer Receipt</div>
        <div class="receipt-status"><span>\u2713 Transfer Successful</span></div>
        <div class="receipt-row"><span class="label">Transaction ID</span><span class="value">#${r.transactionId}</span></div>
        <div class="receipt-row"><span class="label">From Account</span><span class="value">${r.senderAccount}</span></div>
        <div class="receipt-row"><span class="label">Sender Name</span><span class="value">${r.senderName}</span></div>
        <div class="receipt-row"><span class="label">To Account</span><span class="value">${r.recipientAccount}</span></div>
        <div class="receipt-row"><span class="label">Recipient Name</span><span class="value">${r.recipientName}</span></div>
        <div class="receipt-row"><span class="label">Remark</span><span class="value">${r.remark}</span></div>
        <div class="receipt-amount"><div class="label">AMOUNT TRANSFERRED</div><div class="amount">\u20B9${(r.amount || 0).toLocaleString('en-IN')}</div></div>
        <div class="receipt-row"><span class="label">Bank Charge</span><span class="value">\u20B9${r.charge}</span></div>
        <div class="receipt-row"><span class="label">Total Debit</span><span class="value">\u20B9${(r.total || 0).toLocaleString('en-IN')}</span></div>
        <div class="receipt-row"><span class="label">New Balance</span><span class="value">\u20B9${(r.newBalance || 0).toLocaleString('en-IN')}</span></div>
      </div>
      <div class="receipt-footer">This is a system-generated receipt from NEO BANK. No signature required.</div>
    </div></body></html>`;

    const win = window.open('', '_blank');
    if (win) {
      win.document.write(html);
      win.document.close();
      setTimeout(() => { win.print(); }, 500);
    }
  }

  // ─── Withdraw Money ───────────────────────────────────────

  withdrawMoney() {
    if (!this.withdrawAmount || !this.withdrawPin) {
      this.alertService.userError('Validation', 'Please fill all required fields');
      return;
    }
    if (this.withdrawAmount <= 0) {
      this.alertService.userError('Validation', 'Amount must be greater than 0');
      return;
    }
    if (!this.account?.transactionPinSet) {
      this.alertService.userError('PIN Required', 'Please set your transaction PIN first in Profile Settings');
      return;
    }
    this.isWithdrawing = true;
    this.salaryService.withdrawMoney(this.account!.id!, this.withdrawAmount, this.withdrawPin).subscribe({
      next: (res) => {
        this.isWithdrawing = false;
        if (res.success) {
          this.alertService.userSuccess('Withdrawal Successful', res.message);
          this.withdrawAmount = null; this.withdrawPin = '';
          this.loadAccount();
        } else {
          this.alertService.userError('Withdrawal Failed', res.message);
        }
      },
      error: (err) => {
        this.isWithdrawing = false;
        this.alertService.userError('Error', err.error?.message || 'Withdrawal failed');
      }
    });
  }

  // ─── Bank Charges ─────────────────────────────────────────

  loadBankCharges() {
    if (!this.account?.id) return;
    this.isLoadingCharges = true;
    this.salaryService.getBankCharges(this.account.id).subscribe({
      next: (data) => { this.bankCharges = data; this.isLoadingCharges = false; },
      error: () => { this.isLoadingCharges = false; }
    });
  }

  // ─── Salary Slip ──────────────────────────────────────────

  downloadSalarySlip(txn: SalaryTransaction) {
    if (!this.account?.id || !txn.id) return;
    this.isLoadingSlip = true;
    this.selectedSlipTxn = txn;
    this.salaryService.getSalarySlipData(this.account.id, txn.id).subscribe({
      next: (data) => {
        this.slipData = data;
        this.isLoadingSlip = false;
        setTimeout(() => this.generatePDF(), 200);
      },
      error: () => { this.isLoadingSlip = false; this.alertService.userError('Error', 'Failed to load salary slip data'); }
    });
  }

  generatePDF() {
    if (!this.slipData) return;
    const d = this.slipData;
    const creditDate = d.creditDate ? new Date(d.creditDate).toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' }) : '-';

    const html = `
    <html><head><title>Salary Slip</title>
    <style>
      body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 40px; color: #1e293b; }
      .slip { max-width: 700px; margin: 0 auto; border: 2px solid #6366f1; border-radius: 12px; overflow: hidden; }
      .slip-header { background: linear-gradient(135deg, #6366f1, #4f46e5); color: white; padding: 24px 32px; display: flex; justify-content: space-between; align-items: center; }
      .slip-header h1 { margin: 0; font-size: 22px; } .slip-header .bank-name { font-size: 14px; opacity: .9; }
      .slip-body { padding: 32px; }
      .slip-title { text-align: center; font-size: 18px; font-weight: 700; margin-bottom: 24px; color: #6366f1; text-transform: uppercase; letter-spacing: 1px; border-bottom: 2px dashed #e2e8f0; padding-bottom: 16px; }
      .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }
      .info-item { padding: 12px 16px; background: #f8fafc; border-radius: 8px; border-left: 3px solid #6366f1; }
      .info-item .label { font-size: 11px; text-transform: uppercase; color: #64748b; letter-spacing: .5px; margin-bottom: 4px; }
      .info-item .value { font-size: 15px; font-weight: 600; color: #1e293b; }
      .amount-row { text-align: center; background: #f0fdf4; padding: 20px; border-radius: 10px; margin: 20px 0; border: 1px solid #bbf7d0; }
      .amount-row .label { font-size: 13px; color: #16a34a; } .amount-row .amount { font-size: 32px; font-weight: 700; color: #16a34a; }
      .slip-footer { text-align: center; font-size: 12px; color: #94a3b8; padding: 16px 32px; border-top: 1px solid #e2e8f0; }
      @media print { body { padding: 0; } .slip { border: none; } }
    </style></head><body>
    <div class="slip">
      <div class="slip-header"><div><h1>NEO BANK</h1><div class="bank-name">Digital Banking Solutions</div></div><div style="text-align:right;font-size:13px;">Transaction #${d.transactionId}<br>${creditDate}</div></div>
      <div class="slip-body">
        <div class="slip-title">Salary Slip</div>
        <div class="info-grid">
          <div class="info-item"><div class="label">Employee Name</div><div class="value">${d.employeeName || '-'}</div></div>
          <div class="info-item"><div class="label">Designation</div><div class="value">${d.designation || '-'}</div></div>
          <div class="info-item"><div class="label">Company</div><div class="value">${d.companyName || '-'}</div></div>
          <div class="info-item"><div class="label">Account Number</div><div class="value">${d.accountNumber || '-'}</div></div>
          <div class="info-item"><div class="label">Customer ID</div><div class="value">${d.customerId || '-'}</div></div>
          <div class="info-item"><div class="label">Branch</div><div class="value">${d.branchName || '-'}</div></div>
          <div class="info-item"><div class="label">IFSC Code</div><div class="value">${d.ifscCode || '-'}</div></div>
          <div class="info-item"><div class="label">Credit Date</div><div class="value">${creditDate}</div></div>
        </div>
        <div class="amount-row"><div class="label">NET SALARY CREDITED</div><div class="amount">\u20B9${(d.salaryAmount || 0).toLocaleString('en-IN')}</div></div>
      </div>
      <div class="slip-footer">This is a system-generated salary slip from NEO BANK. No signature required.</div>
    </div></body></html>`;

    const win = window.open('', '_blank');
    if (win) {
      win.document.write(html);
      win.document.close();
      setTimeout(() => { win.print(); }, 500);
    }
  }

  // ─── Profile / Change Password ────────────────────────────

  changePassword() {
    if (!this.currentPassword || !this.newPassword || !this.confirmNewPassword) {
      this.alertService.userError('Validation', 'Please fill all fields');
      return;
    }
    if (this.newPassword.length < 6) {
      this.alertService.userError('Validation', 'New password must be at least 6 characters');
      return;
    }
    if (this.newPassword !== this.confirmNewPassword) {
      this.alertService.userError('Validation', 'Passwords do not match');
      return;
    }
    this.isChangingPassword = true;
    this.salaryService.changePassword(this.account!.id!, this.currentPassword, this.newPassword).subscribe({
      next: (res) => {
        this.isChangingPassword = false;
        if (res.success) {
          this.alertService.userSuccess('Success', res.message);
          this.currentPassword = ''; this.newPassword = ''; this.confirmNewPassword = '';
        } else {
          this.alertService.userError('Failed', res.message);
        }
      },
      error: () => { this.isChangingPassword = false; this.alertService.userError('Error', 'Failed to change password'); }
    });
  }

  // ─── Transaction PIN ──────────────────────────────────────

  setTransactionPin() {
    if (!this.pinPassword || !this.newPin || !this.confirmPin) {
      this.alertService.userError('Validation', 'Please fill all fields');
      return;
    }
    if (this.newPin !== this.confirmPin) {
      this.alertService.userError('Validation', 'PINs do not match');
      return;
    }
    this.isSettingPin = true;
    this.salaryService.setTransactionPin(this.account!.id!, this.newPin, this.pinPassword).subscribe({
      next: (res) => {
        this.isSettingPin = false;
        if (res.success) {
          this.alertService.userSuccess('Success', res.message);
          this.pinPassword = ''; this.newPin = ''; this.confirmPin = '';
          this.loadAccount();
        } else {
          this.alertService.userError('Failed', res.message);
        }
      },
      error: () => { this.isSettingPin = false; this.alertService.userError('Error', 'Failed to set PIN'); }
    });
  }

  changeTransactionPin() {
    if (!this.oldPin || !this.changePinNew || !this.changePinConfirm) {
      this.alertService.userError('Validation', 'Please fill all fields');
      return;
    }
    if (this.changePinNew !== this.changePinConfirm) {
      this.alertService.userError('Validation', 'PINs do not match');
      return;
    }
    this.isChangingPin = true;
    this.salaryService.changeTransactionPin(this.account!.id!, this.oldPin, this.changePinNew).subscribe({
      next: (res) => {
        this.isChangingPin = false;
        if (res.success) {
          this.alertService.userSuccess('Success', res.message);
          this.oldPin = ''; this.changePinNew = ''; this.changePinConfirm = '';
        } else {
          this.alertService.userError('Failed', res.message);
        }
      },
      error: () => { this.isChangingPin = false; this.alertService.userError('Error', 'Failed to change PIN'); }
    });
  }

  // ─── Login Activity ───────────────────────────────────────

  loadLoginActivity() {
    if (!this.account?.id) return;
    this.isLoadingActivity = true;
    this.salaryService.getLoginActivity(this.account.id).subscribe({
      next: (data) => { this.loginActivities = data; this.isLoadingActivity = false; },
      error: () => { this.isLoadingActivity = false; }
    });
  }

  // ─── Logout ───────────────────────────────────────────────

  logout() {
    if (this.currentSessionTimer) clearInterval(this.currentSessionTimer);
    if (this.account?.id) {
      const timeUsed = this.currentSessionDuration;
      const deviceInfo = `${this.getDeviceInfo()} | Session: ${timeUsed}`;

      // Record to session-history for manager dashboard tracking
      this.http.post(`${environment.apiBaseUrl}/api/session-history/logout`, {
        userId: this.account.id,
        userType: 'USER',
        sessionDuration: timeUsed
      }).subscribe({
        next: () => console.log('Salary account logout recorded'),
        error: (err) => console.error('Error recording salary account logout:', err)
      });

      this.salaryService.recordActivity(this.account.id, 'LOGOUT', deviceInfo, this.getBrowserName()).subscribe({
        complete: () => {
          sessionStorage.removeItem('salaryEmployee');
          this.alertService.userSuccess('Logged Out', 'You have been logged out.');
          this.router.navigate(['/website/user']);
        },
        error: () => {
          sessionStorage.removeItem('salaryEmployee');
          this.router.navigate(['/website/user']);
        }
      });
    } else {
      sessionStorage.removeItem('salaryEmployee');
      this.alertService.userSuccess('Logged Out', 'You have been logged out.');
      this.router.navigate(['/website/user']);
    }
  }

  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  // Sidebar Search Methods
  onSidebarSearch() {
    // Real-time filtering handled by matchesSidebarSearch()
  }

  matchesSidebarSearch(label: string): boolean {
    if (!this.sidebarSearchQuery || !this.sidebarSearchQuery.trim()) return true;
    return label.toLowerCase().includes(this.sidebarSearchQuery.toLowerCase().trim());
  }

  getTransferCharge(amount: number): number {
    if (amount <= 10000) return 0;
    if (amount <= 50000) return 5;
    if (amount <= 200000) return 15;
    return 25;
  }

  getWithdrawCharge(amount: number): number {
    if (amount <= 25000) return 0;
    return 10;
  }

  // ─── UPI Integration ──────────────────────────────────────

  enableUpi() {
    if (!this.account?.id) return;
    this.isEnablingUpi = true;
    this.salaryService.enableUpi(this.account.id).subscribe({
      next: (res) => {
        this.isEnablingUpi = false;
        if (res.success) {
          this.alertService.userSuccess('UPI Enabled', 'Your UPI ID: ' + res.upiId);
          this.loadAccount();
        } else {
          this.alertService.userError('Error', res.message);
        }
      },
      error: () => { this.isEnablingUpi = false; this.alertService.userError('Error', 'Failed to enable UPI'); }
    });
  }

  sendUpiPayment() {
    if (!this.account?.id || !this.upiRecipient || !this.upiAmount || !this.upiPin) {
      this.alertService.userError('Validation', 'Please fill all required fields');
      return;
    }
    this.isSendingUpi = true;
    this.salaryService.sendUpiPayment(this.account.id, this.upiRecipient, this.upiAmount, this.upiRemark, this.upiPin).subscribe({
      next: (res) => {
        this.isSendingUpi = false;
        if (res.success) {
          this.alertService.userSuccess('UPI Payment Sent', res.message);
          this.upiRecipient = ''; this.upiAmount = null; this.upiRemark = ''; this.upiPin = '';
          this.loadUpiTransactions();
          this.loadAccount();
        } else {
          this.alertService.userError('Failed', res.message);
        }
      },
      error: (err) => { this.isSendingUpi = false; this.alertService.userError('Error', err.error?.message || 'UPI payment failed'); }
    });
  }

  loadUpiTransactions() {
    if (!this.account?.id) return;
    this.isLoadingUpiTxns = true;
    this.salaryService.getUpiTransactions(this.account.id).subscribe({
      next: (txns) => { this.upiTransactions = txns; this.isLoadingUpiTxns = false; },
      error: () => { this.isLoadingUpiTxns = false; }
    });
  }

  // ─── QR Scanner (Upload from Gallery) ─────────────────────

  onQrFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || !input.files[0]) return;
    const file = input.files[0];
    input.value = '';
    if (!file.type.startsWith('image/')) {
      this.qrScanError = 'Please select a valid image file.';
      return;
    }
    this.isQrScanning = true;
    this.qrScanError = '';
    this.showQrScanResult = false;
    this.showQrPaySuccess = false;

    const fileReader = new FileReader();
    fileReader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        import('jsqr').then(({ default: jsQR }) => {
          const decoded = this._tryDecodeQr(jsQR, img);
          this.isQrScanning = false;
          if (decoded) {
            this.parseSalaryUpiQr(decoded);
          } else {
            this.qrScanError = 'Could not read QR code. Please use the NeoBank QR code shown in your dashboard (not a screenshot of another app).';
          }
        }).catch(() => {
          this.isQrScanning = false;
          this.qrScanError = 'QR scanner failed to load. Please refresh and try again.';
        });
      };
      img.onerror = () => {
        this.isQrScanning = false;
        this.qrScanError = 'Failed to load image. Please try another file.';
      };
      img.src = e.target!.result as string;
    };
    fileReader.onerror = () => {
      this.isQrScanning = false;
      this.qrScanError = 'Failed to read file. Please try again.';
    };
    fileReader.readAsDataURL(file);
  }

  /** Multi-strategy QR decoder: tries several scales + binarization to maximise decode rate */
  private _tryDecodeQr(jsQR: any, img: HTMLImageElement): string | null {
    // Scale factors to try — upscaling helps with small/screenshot QR codes
    const scales = [2, 1, 4, 3];
    for (const scale of scales) {
      const w = Math.round(img.naturalWidth * scale);
      const h = Math.round(img.naturalHeight * scale);
      const canvas = document.createElement('canvas');
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext('2d');
      if (!ctx) continue;
      // Nearest-neighbour keeps QR pixels sharp when upscaling
      ctx.imageSmoothingEnabled = scale < 1;
      ctx.drawImage(img, 0, 0, w, h);

      // Attempt 1: raw pixels, both inversion modes
      let id = ctx.getImageData(0, 0, w, h);
      let result = jsQR(id.data, id.width, id.height, { inversionAttempts: 'attemptBoth' });
      if (result?.data) return result.data;

      // Attempt 2: binarize (pure B&W) — strips JPEG compression noise
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

  parseSalaryUpiQr(decoded: string) {
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
        this.qrScanError = 'No UPI ID found in QR code. Please scan a valid NeoBank QR.';
        return;
      }

      // Accept all NeoBank-issued UPI IDs: @neobank (accounts) and @ezyvault (payment gateway)
      const paLower = pa.toLowerCase();
      if (!paLower.endsWith('@neobank') && !paLower.endsWith('@ezyvault')) {
        this.qrScanError = '❌ Invalid QR: Only NeoBank QR codes are supported. Please scan a QR code from NeoBank.';
        return;
      }

      this.qrScannedUpiId = pa;
      this.qrScannedName = pn ? decodeURIComponent(pn) : pa.split('@')[0];
      this.qrScannedAmount = am ? parseFloat(am) : null;
      this.qrScannedRemark = tn ? decodeURIComponent(tn) : '';
      this.qrPayPin = '';
      this.showQrScanResult = true;
    } catch {
      this.qrScanError = 'Failed to parse QR code. Please try again.';
    }
  }

  payViaScannedQr() {
    if (!this.account?.id || !this.qrScannedUpiId || !this.qrScannedAmount || !this.qrPayPin) {
      this.alertService.userError('Validation', 'UPI ID, amount and PIN are required.');
      return;
    }
    this.isQrPayProcessing = true;
    this.salaryService.sendUpiPayment(
      this.account.id, this.qrScannedUpiId,
      this.qrScannedAmount, this.qrScannedRemark, this.qrPayPin
    ).subscribe({
      next: (res) => {
        this.isQrPayProcessing = false;
        if (res.success) {
          this.qrPaySuccessResult = {
            txnId: res.transactionId || res.txnId || ('TXN' + Date.now()),
            amount: this.qrScannedAmount,
            recipient: this.qrScannedUpiId,
            recipientName: this.qrScannedName || this.qrScannedUpiId,
            newBalance: res.newBalance,
            remark: this.qrScannedRemark
          };
          this.showQrPaySuccess = true;
          this.qrPayPin = '';
          this.loadUpiTransactions();
          this.loadAccount();
          // If this was a Payment Gateway merchant QR (@ezyvault), mark the order PAID
          if (this.qrScannedUpiId?.toLowerCase().endsWith('@ezyvault') && this.qrScannedRemark) {
            const pgMatch = this.qrScannedRemark.match(/Payment\s+for\s+(ORD\w+)/i);
            if (pgMatch?.[1]) {
              this.pgService.completeOrderFromQrPayment(pgMatch[1], {
                payerAccount: this.account?.accountNumber || '',
                payerName: this.account?.employeeName || '',
                paymentMethod: 'UPI_QR',
                txnRef: res.transactionId || res.txnId || ''
              }).subscribe();
            }
          }
        } else {
          this.alertService.userError('Payment Failed', res.message);
        }
      },
      error: (err) => {
        this.isQrPayProcessing = false;
        this.alertService.userError('Error', err.error?.message || 'QR payment failed');
      }
    });
  }

  resetQrScan() {
    this.qrScanError = '';
    this.qrScannedUpiId = '';
    this.qrScannedName = '';
    this.qrScannedAmount = null;
    this.qrScannedRemark = '';
    this.showQrScanResult = false;
    this.qrPayPin = '';
    this.showQrPaySuccess = false;
    this.qrPaySuccessResult = null;
    this.isQrScanning = false;
  }

  // ─── Loan Eligibility ─────────────────────────────────────

  loadLoanEligibility() {
    if (!this.account?.id) return;
    this.isLoadingLoan = true;
    this.salaryService.getLoanEligibility(this.account.id).subscribe({
      next: (data) => { this.loanEligibility = data; this.isLoadingLoan = false; },
      error: () => { this.isLoadingLoan = false; }
    });
  }

  // ─── Auto Savings ─────────────────────────────────────────

  loadAutoSavingsInfo() {
    if (!this.account?.id) return;
    this.isLoadingSavings = true;
    this.salaryService.getAutoSavingsInfo(this.account.id).subscribe({
      next: (data) => {
        this.savingsInfo = data;
        this.savingsPercentage = data.autoSavingsPercentage;
        this.isLoadingSavings = false;
      },
      error: () => { this.isLoadingSavings = false; }
    });
  }

  toggleAutoSavings(enable: boolean) {
    if (!this.account?.id) return;
    this.isTogglingSavings = true;
    this.salaryService.toggleAutoSavings(this.account.id, enable, this.savingsPercentage || undefined).subscribe({
      next: (res) => {
        this.isTogglingSavings = false;
        if (res.success) {
          this.alertService.userSuccess('Success', res.message);
          this.loadAutoSavingsInfo();
          this.loadAccount();
        } else {
          this.alertService.userError('Error', res.message);
        }
      },
      error: () => { this.isTogglingSavings = false; }
    });
  }

  withdrawFromSavings() {
    if (!this.account?.id || !this.savingsWithdrawAmount || !this.savingsWithdrawPin) {
      this.alertService.userError('Validation', 'Please fill all fields');
      return;
    }
    this.isWithdrawingSavings = true;
    this.salaryService.withdrawSavings(this.account.id, this.savingsWithdrawAmount, this.savingsWithdrawPin).subscribe({
      next: (res) => {
        this.isWithdrawingSavings = false;
        if (res.success) {
          this.alertService.userSuccess('Success', res.message);
          this.savingsWithdrawAmount = null; this.savingsWithdrawPin = '';
          this.loadAutoSavingsInfo();
          this.loadAccount();
        } else {
          this.alertService.userError('Error', res.message);
        }
      },
      error: () => { this.isWithdrawingSavings = false; }
    });
  }

  // ─── Salary Advance ───────────────────────────────────────

  loadAdvanceInfo() {
    if (!this.account?.id) return;
    this.isLoadingAdvance = true;
    this.salaryService.getAdvanceInfo(this.account.id).subscribe({
      next: (data) => { this.advanceInfo = data; this.isLoadingAdvance = false; },
      error: () => { this.isLoadingAdvance = false; }
    });
  }

  loadAdvanceHistory() {
    if (!this.account?.id) return;
    this.isLoadingAdvanceHistory = true;
    this.salaryService.getAdvanceHistory(this.account.id).subscribe({
      next: (data) => { this.advanceHistory = data; this.isLoadingAdvanceHistory = false; },
      error: () => { this.isLoadingAdvanceHistory = false; }
    });
  }

  requestSalaryAdvance() {
    if (!this.account?.id || !this.advanceAmount) {
      this.alertService.userError('Validation', 'Please enter advance amount');
      return;
    }
    this.isRequestingAdvance = true;
    this.salaryService.requestSalaryAdvance(this.account.id, this.advanceAmount, this.advanceReason).subscribe({
      next: (res) => {
        this.isRequestingAdvance = false;
        if (res.success) {
          this.alertService.userSuccess('Advance Approved', res.message);
          this.advanceAmount = null; this.advanceReason = '';
          this.loadAdvanceInfo();
          this.loadAdvanceHistory();
          this.loadAccount();
        } else {
          this.alertService.userError('Failed', res.message);
        }
      },
      error: (err) => { this.isRequestingAdvance = false; this.alertService.userError('Error', err.error?.message || 'Request failed'); }
    });
  }

  // ─── Gold Loan ─────────────────────────────────────────────

  loadCurrentGoldRate() {
    this.salaryService.getCurrentGoldRate().subscribe({
      next: (rate) => { this.currentGoldRate = rate; },
      error: () => {}
    });
  }

  calculateGoldLoan() {
    if (!this.goldLoanGrams || this.goldLoanGrams <= 0) {
      this.alertService.userError('Validation', 'Please enter valid gold weight in grams');
      return;
    }
    this.isCalculatingGoldLoan = true;
    this.salaryService.calculateGoldLoan(this.goldLoanGrams).subscribe({
      next: (data) => { this.goldLoanCalculation = data; this.isCalculatingGoldLoan = false; },
      error: () => { this.isCalculatingGoldLoan = false; this.alertService.userError('Error', 'Failed to calculate loan'); }
    });
  }

  applyGoldLoan() {
    if (!this.account || !this.goldLoanGrams || this.goldLoanGrams <= 0) {
      this.alertService.userError('Validation', 'Please calculate loan amount first');
      return;
    }
    this.isApplyingGoldLoan = true;
    const loanData = {
      goldGrams: this.goldLoanGrams,
      purpose: this.goldLoanPurpose || 'Gold Loan',
      userName: this.employee?.employeeName,
      userEmail: this.employee?.email || '',
      accountNumber: this.account.accountNumber,
      tenure: 12
    };
    this.salaryService.applyGoldLoan(loanData).subscribe({
      next: (res) => {
        this.isApplyingGoldLoan = false;
        this.alertService.userSuccess('Application Submitted', 'Your gold loan application has been submitted for approval');
        this.goldLoanGrams = null;
        this.goldLoanPurpose = '';
        this.goldLoanCalculation = null;
        this.loadMyGoldLoans();
      },
      error: (err) => {
        this.isApplyingGoldLoan = false;
        this.alertService.userError('Error', err.error?.message || 'Failed to apply for gold loan');
      }
    });
  }

  loadMyGoldLoans() {
    if (!this.account?.accountNumber) return;
    this.isLoadingGoldLoans = true;
    this.salaryService.getGoldLoansByAccount(this.account.accountNumber).subscribe({
      next: (loans) => { this.myGoldLoans = loans || []; this.isLoadingGoldLoans = false; },
      error: () => { this.isLoadingGoldLoans = false; }
    });
  }

  viewGoldLoanDetails(loan: any) {
    this.selectedGoldLoan = loan;
    if (loan.loanAccountNumber) {
      this.isLoadingEmi = true;
      this.salaryService.getGoldLoanEmiSchedule(loan.loanAccountNumber).subscribe({
        next: (schedule) => { this.goldLoanEmiSchedule = schedule || []; this.isLoadingEmi = false; },
        error: () => { this.isLoadingEmi = false; }
      });
    }
  }

  closeGoldLoanDetails() {
    this.selectedGoldLoan = null;
    this.goldLoanEmiSchedule = [];
  }

  acceptGoldLoanTerms(loan: any) {
    const acceptedBy = this.employee?.employeeName || this.account?.accountNumber || 'Employee';
    this.salaryService.acceptGoldLoanTerms(loan.id, acceptedBy).subscribe({
      next: () => {
        this.alertService.userSuccess('Terms Accepted', 'You have accepted the gold loan terms');
        this.loadMyGoldLoans();
        this.selectedGoldLoan = null;
      },
      error: (err) => { this.alertService.userError('Error', err.error?.message || 'Failed to accept terms'); }
    });
  }

  payGoldLoanEmi(emi: any) {
    if (!this.account?.accountNumber) return;
    if (!confirm(`Pay EMI #${emi.emiNumber} of ₹${(emi.totalAmount || 0).toLocaleString('en-IN')}? This will be debited from your salary account.`)) return;
    this.salaryService.payGoldLoanEmi(emi.id, this.account.accountNumber).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.userSuccess('EMI Paid', `EMI #${emi.emiNumber} paid successfully. New balance: ₹${(res.newBalance || 0).toLocaleString('en-IN')}`);
          if (this.selectedGoldLoan?.loanAccountNumber) {
            this.salaryService.getGoldLoanEmiSchedule(this.selectedGoldLoan.loanAccountNumber).subscribe({
              next: (schedule) => { this.goldLoanEmiSchedule = schedule || []; },
              error: () => {}
            });
          }
          this.loadMyGoldLoans();
        } else {
          this.alertService.userError('Payment Failed', res.message || 'Failed to pay EMI');
        }
      },
      error: (err) => { this.alertService.userError('Payment Failed', err.error?.message || 'Failed to pay EMI'); }
    });
  }

  printGoldLoanReceipt(loan: any) {
    const receiptWindow = window.open('', '_blank');
    if (!receiptWindow) return;
    const html = `<!DOCTYPE html><html><head><title>Gold Loan Receipt</title>
    <style>body{font-family:Arial,sans-serif;padding:40px;max-width:800px;margin:0 auto}
    .header{text-align:center;border-bottom:2px solid #1a237e;padding-bottom:20px;margin-bottom:30px}
    .header h1{color:#1a237e;margin:0}
    .header p{color:#666;margin:5px 0}
    .section{margin-bottom:20px}
    .section h3{color:#1a237e;border-bottom:1px solid #ddd;padding-bottom:8px}
    .row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f0f0f0}
    .label{color:#666;font-weight:500}
    .value{font-weight:600}
    .status{padding:4px 12px;border-radius:12px;font-size:13px;font-weight:600}
    .status.approved{background:#e8f5e9;color:#2e7d32}
    .status.pending{background:#fff3e0;color:#e65100}
    .status.rejected{background:#ffebee;color:#c62828}
    .footer{text-align:center;margin-top:40px;padding-top:20px;border-top:2px solid #1a237e;color:#666;font-size:12px}
    @media print{body{padding:20px}}</style></head><body>
    <div class="header"><h1>NeoBank</h1><p>Gold Loan Receipt</p><p>Generated: ${new Date().toLocaleDateString('en-IN', {day:'2-digit',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'})}</p></div>
    <div class="section"><h3>Loan Details</h3>
    <div class="row"><span class="label">Loan Account</span><span class="value">${loan.loanAccountNumber || 'N/A'}</span></div>
    <div class="row"><span class="label">Application Date</span><span class="value">${new Date(loan.applicationDate).toLocaleDateString('en-IN')}</span></div>
    <div class="row"><span class="label">Status</span><span class="status ${(loan.status||'').toLowerCase()}">${loan.status}</span></div>
    <div class="row"><span class="label">Gold Weight</span><span class="value">${loan.goldGrams} grams</span></div>
    <div class="row"><span class="label">Gold Rate/gram</span><span class="value">&#8377;${(loan.goldRatePerGram||0).toLocaleString('en-IN')}</span></div>
    <div class="row"><span class="label">Gold Value</span><span class="value">&#8377;${(loan.goldValue||0).toLocaleString('en-IN')}</span></div>
    <div class="row"><span class="label">Loan Amount (75%)</span><span class="value">&#8377;${(loan.loanAmount||0).toLocaleString('en-IN')}</span></div>
    <div class="row"><span class="label">Tenure</span><span class="value">${loan.tenure || 12} months</span></div>
    <div class="row"><span class="label">Interest Rate</span><span class="value">${loan.interestRate || 9}% p.a.</span></div>
    <div class="row"><span class="label">Purpose</span><span class="value">${loan.purpose || 'Gold Loan'}</span></div></div>
    <div class="section"><h3>Applicant Details</h3>
    <div class="row"><span class="label">Name</span><span class="value">${loan.userName}</span></div>
    <div class="row"><span class="label">Account Number</span><span class="value">${loan.accountNumber}</span></div>
    <div class="row"><span class="label">Email</span><span class="value">${loan.userEmail || 'N/A'}</span></div></div>
    ${loan.status === 'Approved' ? `<div class="section"><h3>Approval Details</h3>
    <div class="row"><span class="label">Approved By</span><span class="value">${loan.approvedBy || 'N/A'}</span></div>
    <div class="row"><span class="label">Approval Date</span><span class="value">${loan.approvalDate ? new Date(loan.approvalDate).toLocaleDateString('en-IN') : 'N/A'}</span></div>
    ${loan.verifiedGoldGrams ? `<div class="row"><span class="label">Verified Weight</span><span class="value">${loan.verifiedGoldGrams} grams</span></div>` : ''}
    ${loan.goldPurity ? `<div class="row"><span class="label">Gold Purity</span><span class="value">${loan.goldPurity}</span></div>` : ''}</div>` : ''}
    <div class="footer"><p>This is a computer generated receipt from NeoBank</p><p>For queries, contact support&#64;neobank.com</p></div></body></html>`;
    receiptWindow.document.write(html);
    receiptWindow.document.close();
    receiptWindow.print();
  }

  // ─── Debit Card Management ────────────────────────────────

  loadDebitCardInfo() {
    if (!this.account?.id) return;
    this.isLoadingCard = true;
    this.salaryService.getDebitCardInfo(this.account.id).subscribe({
      next: (data) => { this.debitCardInfo = data; this.isLoadingCard = false; },
      error: () => { this.isLoadingCard = false; }
    });
  }

  updateCardSetting(key: string, value: any) {
    if (!this.account?.id) return;
    this.isUpdatingCard = true;
    const settings: any = {};
    settings[key] = value;
    this.salaryService.updateDebitCardSettings(this.account.id, settings).subscribe({
      next: (res) => {
        this.isUpdatingCard = false;
        if (res.success) {
          this.alertService.userSuccess('Updated', res.message);
          this.loadDebitCardInfo();
          this.loadAccount();
        } else {
          this.alertService.userError('Error', res.message);
        }
      },
      error: () => { this.isUpdatingCard = false; }
    });
  }

  maskCardNumber(num: string): string {
    if (!num || num.length < 8) return num || '****';
    return '**** **** **** ' + num.slice(-4);
  }

  formatFullCardNumber(num: string): string {
    if (!num) return '';
    return num.replace(/(.{4})/g, '$1 ').trim();
  }

  toggleCardNumberVisibility(): void {
    this.showFullCardNumber = !this.showFullCardNumber;
  }

  toggleCvvVisibility(): void {
    this.showCvv = !this.showCvv;
  }

  generateCardDetails(): void {
    if (!this.account?.id) return;
    this.isGeneratingCard = true;
    this.salaryService.generateCardDetails(this.account.id).subscribe({
      next: (res: any) => {
        this.isGeneratingCard = false;
        if (res.success) {
          this.alertService.userSuccess('Generated!', 'Card details generated permanently');
          this.loadDebitCardInfo();
        } else {
          this.alertService.userError('Error', res.message);
        }
      },
      error: (err: any) => {
        this.isGeneratingCard = false;
        this.alertService.userError('Error', err.error?.message || 'Failed to generate card details');
      }
    });
  }

  // ─── AI Fraud Detection ───────────────────────────────────

  loadFraudSummary() {
    if (!this.account?.id) return;
    this.isLoadingFraud = true;
    this.salaryService.getFraudSummary(this.account.id).subscribe({
      next: (data) => {
        this.fraudSummary = data;
        this.fraudAlerts = data.alerts || [];
        this.isLoadingFraud = false;
      },
      error: () => { this.isLoadingFraud = false; }
    });
  }

  resolveAlert(alertId: number) {
    this.salaryService.resolveFraudAlert(alertId).subscribe({
      next: (res) => {
        if (res.success) {
          this.alertService.userSuccess('Resolved', 'Alert marked as resolved');
          this.loadFraudSummary();
        }
      }
    });
  }

  // ─── Employee Profile ─────────────────────────────────────

  initProfileForm() {
    if (this.account) {
      this.profileAddress = this.account.address || '';
      this.profileMobile = this.account.mobileNumber || '';
      this.profileEmail = this.account.email || '';
    }
  }

  updateProfile() {
    if (!this.account?.id) return;
    this.isUpdatingProfile = true;
    this.salaryService.updateProfile(this.account.id, {
      address: this.profileAddress,
      mobileNumber: this.profileMobile,
      email: this.profileEmail
    }).subscribe({
      next: (res) => {
        this.isUpdatingProfile = false;
        if (res.success) {
          this.alertService.userSuccess('Success', 'Profile updated successfully');
          this.loadAccount();
        } else {
          this.alertService.userError('Error', res.message);
        }
      },
      error: () => { this.isUpdatingProfile = false; this.alertService.userError('Error', 'Profile update failed'); }
    });
  }

  // ─── Payment Links (Pay via Link) ────────────────────────────────────────────
  loadSalaryPaymentLinks() {
    if (!this.account?.upiId) return;
    this.isLoadingLinks = true;
    this.pgService.getCustomerPaymentLinks(this.account.upiId).subscribe({
      next: (data: any[]) => {
        this.allPaymentLinks = data || [];
        this.pendingPaymentLinks = this.allPaymentLinks.filter(l => l.status === 'PENDING');
        this.isLoadingLinks = false;
        if (this.pendingPaymentLinks.length > 0 && !this.linkPollInterval) {
          this.linkPollInterval = setInterval(() => this.loadSalaryPaymentLinks(), 5000);
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
          this.loadAccount();
          this.loadUpiTransactions();
          this.loadSalaryPaymentLinks();
          if (this.account?.id) {
            this.salaryService.getTransactions(this.account.id).subscribe({ next: (txns: any) => { this.upiTransactions = txns || []; } });
          }
        } else {
          this.alertService.userError('Payment Failed', res.error || 'Payment failed');
        }
      },
      error: (err: any) => {
        this.isLinkPayProcessing = false;
        this.alertService.userError('Payment Failed', err.error?.error || 'Payment failed');
      }
    });
  }

  closeLinkPayModal() {
    this.showLinkPayModal = false;
    this.selectedLink = null;
    this.linkPayPin = '';
    this.linkPaySuccess = false;
  }

  printInvoice() {
    const el = document.getElementById('pg-invoice-print');
    if (!el) return;
    const w = window.open('', '_blank', 'width=800,height=900');
    if (!w) return;
    w.document.write(`<html><head><title>Payment Invoice</title><style>
      body{font-family:'Segoe UI',Arial,sans-serif;padding:40px;color:#1e293b;max-width:700px;margin:0 auto;}
      .inv-seal-circle{width:90px;height:90px;border:3px solid #16a34a;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;}
      @media print{body{padding:20px;}}
    </style></head><body>${el.innerHTML}</body></html>`);
    w.document.close();
    w.print();
  }
}
