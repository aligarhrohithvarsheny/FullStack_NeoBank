import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SoundboxService } from '../../../service/soundbox.service';
import { SoundboxDevice, SoundboxTransaction } from '../../../model/soundbox/soundbox.model';
import { MerchantOnboardingService } from '../../../service/merchant-onboarding.service';
import { PaymentGatewayService } from '../../../service/payment-gateway.service';

@Component({
  selector: 'app-soundbox-payment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './soundbox-payment.html',
  styleUrls: ['./soundbox-payment.css'],
  encapsulation: ViewEncapsulation.None
})
export class SoundboxPayment implements OnInit, OnDestroy {
  isBrowser = false;

  // Merchant data
  merchant: any = null;
  device: SoundboxDevice | null = null;

  // Tab state
  activeTab: string = 'overview';

  // Transactions
  transactions: SoundboxTransaction[] = [];
  filteredTransactions: SoundboxTransaction[] = [];
  txnSearchQuery: string = '';
  loadingTransactions = false;

  // Stats
  stats: any = null;
  loadingStats = false;

  // Device settings
  editingSettings = false;
  savingSettings = false;
  settingsForm = {
    voiceEnabled: true,
    voiceLanguage: 'en-IN',
    volumeMode: 'NORMAL'
  };

  // Payment simulation
  paymentAmount: number = 0;
  payerName: string = '';
  payerUpi: string = '';
  paymentMethod: string = 'UPI';
  processingPayment = false;
  paymentSuccess = false;
  paymentError = '';
  lastPaymentTxn: any = null;
  receiveAccountNumber = '';
  linkingReceiveAccount = false;
  receiveAccountMessage = '';

  // Voice alert
  voiceSupported = false;

  constructor(
    private router: Router,
    private soundboxService: SoundboxService,
    private merchantService: MerchantOnboardingService,
    private pgService: PaymentGatewayService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (this.isBrowser) {
      const merchantStr = sessionStorage.getItem('merchantSoundbox');
      const deviceStr = sessionStorage.getItem('merchantDevice');

      if (!merchantStr) {
        this.router.navigate(['/website/merchant-login']);
        return;
      }

      this.merchant = JSON.parse(merchantStr);
      this.receiveAccountNumber = this.merchant?.linkedAccountNumber || this.merchant?.accountNumber || '';
      if (deviceStr) {
        this.device = JSON.parse(deviceStr);
        this.settingsForm.voiceEnabled = this.device?.voiceEnabled ?? true;
        this.settingsForm.voiceLanguage = this.device?.voiceLanguage ?? 'en-IN';
        this.settingsForm.volumeMode = this.device?.volumeMode ?? 'NORMAL';
      }

      this.voiceSupported = 'speechSynthesis' in window;
      if (this.merchant?.merchantId) {
        this.loadMerchantPortalData();
      } else {
        this.loadStats();
        this.loadTransactions();
      }
    }
  }
  loadMerchantPortalData() {
    if (!this.merchant?.merchantId) return;
    this.loadingStats = true;
    this.loadingTransactions = true;
    this.merchantService.getMerchantDashboard(this.merchant.merchantId).subscribe({
      next: (res: any) => {
        this.loadingStats = false;
        this.loadingTransactions = false;
        if (res?.success) {
          this.stats = res.dashboard || {};
          this.transactions = res.transactions || [];
          this.filteredTransactions = [...this.transactions];
          if (Array.isArray(res.devices) && res.devices.length > 0) {
            this.device = res.devices[0] as any;
          }
        }
      },
      error: () => {
        this.loadingStats = false;
        this.loadingTransactions = false;
      }
    });
  }


  ngOnDestroy() {}

  // ==================== Data Loading ====================

  loadStats() {
    if (!this.merchant?.accountNumber) return;
    this.loadingStats = true;
    this.soundboxService.getUserStats(this.merchant.accountNumber).subscribe({
      next: (data) => {
        this.stats = data;
        this.loadingStats = false;
      },
      error: () => this.loadingStats = false
    });
  }

  loadTransactions() {
    if (!this.merchant?.accountNumber) return;
    this.loadingTransactions = true;
    this.soundboxService.getTransactionsByAccount(this.merchant.accountNumber).subscribe({
      next: (data) => {
        this.transactions = data;
        this.filteredTransactions = [...data];
        this.loadingTransactions = false;
      },
      error: () => this.loadingTransactions = false
    });
  }

  // ==================== Transaction Search ====================

  filterTransactions() {
    const q = this.txnSearchQuery.toLowerCase().trim();
    if (!q) {
      this.filteredTransactions = [...this.transactions];
      return;
    }
    this.filteredTransactions = this.transactions.filter(t =>
      (t.payerName?.toLowerCase().includes(q)) ||
      (t.payerUpi?.toLowerCase().includes(q)) ||
      (t.txnId?.toLowerCase().includes(q)) ||
      (t.paymentMethod?.toLowerCase().includes(q)) ||
      (t.amount?.toString().includes(q))
    );
  }

  // ==================== Payment Processing ====================

  processPayment() {
    this.paymentError = '';
    this.paymentSuccess = false;
    this.lastPaymentTxn = null;

    if (!this.paymentAmount || this.paymentAmount <= 0) {
      this.paymentError = 'Please enter a valid amount.';
      return;
    }

    if (!this.payerName.trim()) {
      this.paymentError = 'Please enter payer name.';
      return;
    }

    this.processingPayment = true;

    const transaction: SoundboxTransaction = {
      accountNumber: this.receiveAccountNumber || this.merchant.accountNumber,
      deviceId: this.device?.deviceId,
      amount: this.paymentAmount,
      txnType: 'CREDIT',
      paymentMethod: this.paymentMethod,
      payerName: this.payerName.trim(),
      payerUpi: this.payerUpi.trim() || undefined
    };

    this.soundboxService.processPayment(transaction).subscribe({
      next: (res) => {
        this.processingPayment = false;
        if (res.success) {
          this.paymentSuccess = true;
          this.lastPaymentTxn = res.transaction;
          this.playVoiceAlert(this.paymentAmount, this.payerName);
          this.loadTransactions();
          this.loadStats();
          // Reset after 5 seconds
          setTimeout(() => {
            this.paymentSuccess = false;
            this.paymentAmount = 0;
            this.payerName = '';
            this.payerUpi = '';
            this.lastPaymentTxn = null;
          }, 5000);
        } else {
          this.paymentError = res.error || 'Payment processing failed.';
        }
      },
      error: (err) => {
        this.processingPayment = false;
        this.paymentError = err.error?.error || 'Payment processing failed.';
      }
    });
  }

  linkReceiveAccount() {
    if (!this.merchant?.merchantId || !this.receiveAccountNumber?.trim()) return;
    this.linkingReceiveAccount = true;
    this.receiveAccountMessage = '';
    this.pgService.linkMerchantReceivingAccount(this.merchant.merchantId, this.receiveAccountNumber.trim()).subscribe({
      next: (res: any) => {
        this.linkingReceiveAccount = false;
        if (res.success && res.merchant) {
          this.merchant = { ...this.merchant, ...res.merchant };
          this.receiveAccountNumber = this.merchant.linkedAccountNumber || this.receiveAccountNumber;
          this.receiveAccountMessage = 'Account linked successfully for receiving payments.';
          if (this.isBrowser) {
            sessionStorage.setItem('merchantSoundbox', JSON.stringify(this.merchant));
          }
        } else {
          this.receiveAccountMessage = res.error || 'Unable to link account.';
        }
      },
      error: (err: any) => {
        this.linkingReceiveAccount = false;
        this.receiveAccountMessage = err.error?.error || 'Unable to link account.';
      }
    });
  }

  // ==================== Voice Alert ====================

  playVoiceAlert(amount: number, payerName: string) {
    if (!this.voiceSupported || !this.settingsForm.voiceEnabled) return;
    const msg = new SpeechSynthesisUtterance(
      `Payment received. ${amount} rupees from ${payerName}`
    );
    msg.lang = this.settingsForm.voiceLanguage || 'en-IN';
    msg.rate = 0.9;
    msg.volume = this.settingsForm.volumeMode === 'LOUD' ? 1.0 :
                 this.settingsForm.volumeMode === 'SILENT' ? 0.0 : 0.7;
    window.speechSynthesis.speak(msg);
  }

  testVoiceAlert() {
    this.playVoiceAlert(500, 'Test Customer');
  }

  // ==================== Device Settings ====================

  saveDeviceSettings() {
    if (!this.merchant?.accountNumber) return;
    this.savingSettings = true;
    this.soundboxService.updateDeviceSettings(this.merchant.accountNumber, this.settingsForm).subscribe({
      next: (res) => {
        this.savingSettings = false;
        if (res.success && res.device) {
          this.device = res.device;
          if (this.isBrowser) {
            sessionStorage.setItem('merchantDevice', JSON.stringify(res.device));
          }
          this.editingSettings = false;
        }
      },
      error: () => this.savingSettings = false
    });
  }

  // ==================== Helpers ====================

  getDeviceStatusClass(): string {
    switch (this.device?.status) {
      case 'ACTIVE': return 'status-active';
      case 'INACTIVE': return 'status-inactive';
      case 'MAINTENANCE': return 'status-maintenance';
      default: return '';
    }
  }

  getTxnStatusClass(status?: string): string {
    switch (status) {
      case 'SUCCESS': return 'txn-success';
      case 'FAILED': return 'txn-failed';
      case 'PENDING': return 'txn-pending';
      default: return '';
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);
  }

  logout() {
    if (this.isBrowser) {
      sessionStorage.removeItem('merchantSoundbox');
      sessionStorage.removeItem('merchantDevice');
    }
    this.router.navigate(['/website/merchant-login']);
  }
}
