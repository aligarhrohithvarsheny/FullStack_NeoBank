import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PaymentGatewayService } from '../../../service/payment-gateway.service';
import QRCode from 'qrcode';

@Component({
  selector: 'app-pg-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pg-dashboard.html',
  styleUrls: ['./pg-dashboard.css'],
  encapsulation: ViewEncapsulation.None
})
export class PgDashboard implements OnInit, OnDestroy {
  isBrowser = false;
  merchant: any = null;
  activeTab = 'dashboard';
  isLoading = false;

  // Data
  orders: any[] = [];
  transactions: any[] = [];
  refunds: any[] = [];
  ledger: any[] = [];
  paymentLinks: any[] = [];
  analytics: any = {};

  // Stats
  totalVolume = 0;
  successCount = 0;
  failedCount = 0;
  totalFees = 0;

  // Create Order
  showCreateOrder = false;
  orderForm = {
    amount: '',
    description: '',
    customerName: '',
    customerEmail: '',
    customerPhone: '',
    receipt: ''
  };

  // UPI QR Payment
  showQrPayment = false;
  qrSession: any = null;
  qrOrderId = '';
  qrData = '';
  qrImageUrl = '';

  // Card Payment
  showCardPayment = false;
  cardForm = {
    orderId: '',
    cardNumber: '',
    cardExpiry: '',
    cardCvv: '',
    cardHolderName: '',
    cardType: 'DEBIT',
    payerAccount: ''
  };
  cardOtpSent = false;
  cardOtp = '';
  cardOtpEmail = '';
  cardPaymentStep = 1; // 1=card details, 2=otp verify, 3=processing
  cardError = '';
  cardSuccess = '';
  cardProcessing = false;

  // Pay Order form
  showPayOrder = false;
  payForm = {
    orderId: '',
    paymentMethod: 'UPI',
    payerAccount: '',
    payerName: ''
  };

  // Name Verification
  showNameVerify = false;
  nameVerifyResult: any = null;
  nameVerifyLoading = false;
  nameVerifyForm = {
    orderId: '',
    payerName: '',
    payerAccount: ''
  };

  // Refund
  showRefundModal = false;
  refundForm = {
    transactionId: '',
    amount: '',
    reason: ''
  };

  // UPI ID Pay
  showUpiPayModal = false;
  upiPayOrder: any = null;
  upiPayForm = { upiId: '' };
  upiPayVerified = false;
  upiPayVerifiedName = '';
  upiPayVerifyError = '';
  isVerifyingUpiPay = false;
  isSubmittingUpiPay = false;
  upiPaySuccess = '';

  // Order dropdown selection
  selectedOrderId = '';
  selectedOrder: any = null;

  // Settlement
  settlingTxn = '';
  refreshTimer: any = null;

  // Link receiving account
  showLinkAccountModal = false;
  linkAccountNumber = '';
  linkingAccount = false;
  linkAccountMessage = '';
  linkAccountSuccess = false;

  constructor(
    private router: Router,
    private pgService: PaymentGatewayService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (this.isBrowser) {
      const stored = sessionStorage.getItem('pgMerchant');
      if (!stored) {
        this.router.navigate(['/website/pg-login']);
        return;
      }
      this.merchant = JSON.parse(stored);
      this.loadData();
      this.startAutoRefresh();
    }
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  loadData() {
    if (!this.merchant?.merchantId) return;
    this.isLoading = true;
    const mid = this.merchant.merchantId;

    this.pgService.getOrdersByMerchant(mid).subscribe({
      next: (data: any[]) => this.orders = data || [],
      error: () => this.orders = []
    });

    this.pgService.getTransactionsByMerchant(mid).subscribe({
      next: (data: any[]) => {
        this.transactions = data || [];
        this.isLoading = false;
      },
      error: () => { this.transactions = []; this.isLoading = false; }
    });

    this.pgService.getRefundsByMerchant(mid).subscribe({
      next: (data: any[]) => this.refunds = data || [],
      error: () => this.refunds = []
    });

    this.pgService.getLedgerByMerchant(mid).subscribe({
      next: (data: any[]) => this.ledger = data || [],
      error: () => this.ledger = []
    });

    this.pgService.getMerchantPaymentLinks(mid).subscribe({
      next: (data: any[]) => this.paymentLinks = data || [],
      error: () => this.paymentLinks = []
    });

    this.pgService.getMerchantAnalytics(mid).subscribe({
      next: (data: any) => {
        this.analytics = data;
        this.totalVolume = data.totalVolume || 0;
        this.successCount = data.successfulTransactions || 0;
        this.failedCount = data.failedTransactions || 0;
        this.totalFees = data.totalFees || 0;
      },
      error: () => {}
    });
  }

  startAutoRefresh() {
    this.stopAutoRefresh();
    this.refreshTimer = setInterval(() => {
      if (this.merchant?.merchantId) {
        this.loadData();
      }
    }, 5000);
  }

  stopAutoRefresh() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  // ---- Create Order ----
  createOrder() {
    if (!this.orderForm.amount) return;
    this.isLoading = true;
    const payload = {
      merchantId: this.merchant.merchantId,
      amount: parseFloat(this.orderForm.amount),
      description: this.orderForm.description,
      customerName: this.orderForm.customerName,
      customerEmail: this.orderForm.customerEmail,
      customerPhone: this.orderForm.customerPhone,
      receipt: this.orderForm.receipt || ('RCP' + Date.now())
    };
    this.pgService.createOrder(payload).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.orders.unshift(res.order);
          this.showCreateOrder = false;
          this.orderForm = { amount: '', description: '', customerName: '', customerEmail: '', customerPhone: '', receipt: '' };
        }
        this.isLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Failed to create order'); this.isLoading = false; }
    });
  }

  // ---- Order Dropdown ----
  onOrderSelect() {
    this.selectedOrder = this.orders.find(o => o.orderId === this.selectedOrderId) || null;
  }

  getCreatedOrders() {
    return this.orders.filter(o => o.status === 'CREATED');
  }

  // ---- Generate UPI QR ----
  generateQr(order: any) {
    this.isLoading = true;
    this.pgService.createPaymentSession({
      orderId: order.orderId,
      merchantId: this.merchant.merchantId,
      paymentMethod: 'UPI'
    }).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.qrSession = res.session;
          this.qrData = res.qrData;
          this.qrOrderId = order.orderId;
          this.qrImageUrl = '';
          // Generate actual QR code image
          if (this.isBrowser && res.qrData) {
            QRCode.toDataURL(res.qrData, {
              width: 280,
              margin: 2,
              color: { dark: '#ffffff', light: '#0a0a1a' }
            }).then((url: string) => {
              this.qrImageUrl = url;
            }).catch(() => {
              this.qrImageUrl = '';
            });
          }
          this.showQrPayment = true;
        }
        this.isLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Failed to create QR'); this.isLoading = false; }
    });
  }

  // ---- Name Verification ----
  openNameVerify(order: any) {
    this.nameVerifyForm.orderId = order.orderId;
    this.nameVerifyForm.payerName = '';
    this.nameVerifyForm.payerAccount = '';
    this.nameVerifyResult = null;
    this.showNameVerify = true;
  }

  verifyName() {
    if (!this.nameVerifyForm.payerName || !this.nameVerifyForm.payerAccount) return;
    this.nameVerifyLoading = true;
    this.pgService.verifyPayerName(this.nameVerifyForm).subscribe({
      next: (res: any) => {
        this.nameVerifyResult = res;
        this.nameVerifyLoading = false;
      },
      error: (err: any) => {
        this.nameVerifyResult = { success: false, message: err.error?.error || 'Verification failed' };
        this.nameVerifyLoading = false;
      }
    });
  }

  // ---- Pay Order ----
  openPayOrder(order: any) {
    this.payForm.orderId = order.orderId;
    this.payForm.payerName = order.customerName || '';
    this.payForm.payerAccount = '';
    this.payForm.paymentMethod = 'UPI';
    this.showPayOrder = true;
  }

  processPayment() {
    if (!this.payForm.orderId || !this.payForm.payerAccount) return;
    this.isLoading = true;
    this.pgService.processPayment(this.payForm).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.transactions.unshift(res.transaction);
          this.showPayOrder = false;
          this.loadData();
          // Auto-settle
          if (res.transaction?.transactionId) {
            this.settleTransaction(res.transaction.transactionId);
          }
        }
        this.isLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Payment failed'); this.isLoading = false; }
    });
  }

  // ---- Card Payment ----
  openCardPayment(order: any) {
    this.cardForm = {
      orderId: order.orderId,
      cardNumber: '',
      cardExpiry: '',
      cardCvv: '',
      cardHolderName: order.customerName || '',
      cardType: 'DEBIT',
      payerAccount: ''
    };
    this.cardOtpSent = false;
    this.cardOtp = '';
    this.cardOtpEmail = '';
    this.cardPaymentStep = 1;
    this.cardError = '';
    this.cardSuccess = '';
    this.cardProcessing = false;
    this.showCardPayment = true;
  }

  formatCardNumber() {
    let v = this.cardForm.cardNumber.replace(/\D/g, '').substring(0, 16);
    this.cardForm.cardNumber = v.replace(/(\d{4})(?=\d)/g, '$1 ');
  }

  formatExpiry() {
    let v = this.cardForm.cardExpiry.replace(/\D/g, '').substring(0, 4);
    if (v.length > 2) v = v.substring(0, 2) + '/' + v.substring(2);
    this.cardForm.cardExpiry = v;
  }

  getCardBrand(): string {
    const n = this.cardForm.cardNumber.replace(/\s/g, '');
    if (n.startsWith('4')) return 'VISA';
    if (/^5[1-5]/.test(n) || /^2[2-7]/.test(n)) return 'MASTERCARD';
    if (n.startsWith('6')) return 'RUPAY';
    return '';
  }

  getCardLast4(): string {
    return this.cardForm.cardNumber.replace(/\s/g, '').slice(-4);
  }

  validateCardDetails(): boolean {
    const num = this.cardForm.cardNumber.replace(/\s/g, '');
    if (num.length < 16) { this.cardError = 'Enter valid 16-digit card number'; return false; }
    if (!this.cardForm.cardExpiry || this.cardForm.cardExpiry.length < 5) { this.cardError = 'Enter valid expiry (MM/YY)'; return false; }
    if (!this.cardForm.cardCvv || this.cardForm.cardCvv.length < 3) { this.cardError = 'Enter valid CVV'; return false; }
    if (!this.cardForm.cardHolderName.trim()) { this.cardError = 'Enter cardholder name'; return false; }
    return true;
  }

  sendCardOtp() {
    this.cardError = '';
    this.cardSuccess = '';
    if (!this.validateCardDetails()) return;
    this.cardProcessing = true;
    this.pgService.sendCardPaymentOtp({
      orderId: this.cardForm.orderId,
      cardNumber: this.cardForm.cardNumber.replace(/\s/g, ''),
      cardExpiry: this.cardForm.cardExpiry,
      cardCvv: this.cardForm.cardCvv,
      cardHolderName: this.cardForm.cardHolderName,
      cardType: this.cardForm.cardType,
      merchantId: this.merchant.merchantId
    }).subscribe({
      next: (res: any) => {
        this.cardProcessing = false;
        if (res.success) {
          this.cardOtpSent = true;
          this.cardOtpEmail = res.maskedEmail || '';
          this.cardForm.payerAccount = res.accountNumber || '';
          this.cardPaymentStep = 2;
          this.cardSuccess = 'OTP sent to ' + (res.maskedEmail || 'registered email');
        } else {
          this.cardError = res.message || 'Failed to verify card';
        }
      },
      error: (err: any) => {
        this.cardProcessing = false;
        this.cardError = err.error?.error || err.error?.message || 'Card verification failed';
      }
    });
  }

  verifyCardOtpAndPay() {
    this.cardError = '';
    this.cardSuccess = '';
    if (!this.cardOtp || this.cardOtp.length !== 6) {
      this.cardError = 'Enter 6-digit OTP';
      return;
    }
    this.cardProcessing = true;
    this.cardPaymentStep = 3;
    this.pgService.verifyCardPaymentOtp({
      orderId: this.cardForm.orderId,
      otp: this.cardOtp,
      cardNumber: this.cardForm.cardNumber.replace(/\s/g, ''),
      cardHolderName: this.cardForm.cardHolderName,
      cardType: this.cardForm.cardType,
      payerAccount: this.cardForm.payerAccount,
      merchantId: this.merchant.merchantId
    }).subscribe({
      next: (res: any) => {
        this.cardProcessing = false;
        if (res.success) {
          this.cardSuccess = 'Payment successful! Transaction ID: ' + (res.transaction?.transactionId || '');
          this.transactions.unshift(res.transaction);
          this.loadData();
          if (res.transaction?.transactionId) {
            this.settleTransaction(res.transaction.transactionId);
          }
          setTimeout(() => {
            this.showCardPayment = false;
          }, 3000);
        } else {
          this.cardPaymentStep = 2;
          this.cardError = res.message || 'Payment failed';
        }
      },
      error: (err: any) => {
        this.cardProcessing = false;
        this.cardPaymentStep = 2;
        this.cardError = err.error?.error || err.error?.message || 'Payment failed';
      }
    });
  }

  // ---- Settlement (Credit to business account) ----
  settleTransaction(txnId: string) {
    this.settlingTxn = txnId;
    this.pgService.settleTransaction(txnId).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.loadData();
        }
        this.settlingTxn = '';
      },
      error: () => { this.settlingTxn = ''; }
    });
  }

  // ---- Refund ----
  openRefund(txn: any) {
    this.refundForm.transactionId = txn.transactionId;
    this.refundForm.amount = txn.amount;
    this.refundForm.reason = '';
    this.showRefundModal = true;
  }

  processRefund() {
    if (!this.refundForm.transactionId || !this.refundForm.amount) return;
    this.isLoading = true;
    this.pgService.processRefund({
      transactionId: this.refundForm.transactionId,
      amount: parseFloat(this.refundForm.amount),
      reason: this.refundForm.reason
    }).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.refunds.unshift(res.refund);
          this.showRefundModal = false;
          this.loadData();
        }
        this.isLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Refund failed'); this.isLoading = false; }
    });
  }

  getStatusClass(status: string): string {
    const map: any = {
      'SUCCESS': 'st-success', 'PAID': 'st-success', 'PROCESSED': 'st-success', 'CREDITED': 'st-success',
      'CREATED': 'st-pending', 'INITIATED': 'st-pending', 'PENDING': 'st-pending',
      'FAILED': 'st-failed', 'EXPIRED': 'st-failed', 'FLAGGED': 'st-flagged', 'REJECTED': 'st-failed'
    };
    return map[status] || 'st-default';
  }

  logout() {
    if (this.isBrowser) {
      sessionStorage.removeItem('pgMerchant');
    }
    this.router.navigate(['/website/pg-login']);
  }

  goToLanding() {
    this.router.navigate(['/website/landing']);
  }

  // ---- UPI ID Pay ----
  openUpiPayModal(order: any) {
    this.upiPayOrder = order;
    this.upiPayForm = { upiId: '' };
    this.upiPayVerified = false;
    this.upiPayVerifiedName = '';
    this.upiPayVerifyError = '';
    this.isVerifyingUpiPay = false;
    this.isSubmittingUpiPay = false;
    this.upiPaySuccess = '';
    this.showUpiPayModal = true;
  }

  verifyUpiForPayment() {
    if (!this.upiPayForm.upiId) return;
    this.isVerifyingUpiPay = true;
    this.upiPayVerified = false;
    this.upiPayVerifiedName = '';
    this.upiPayVerifyError = '';
    this.pgService.verifyAnyUpiId(this.upiPayForm.upiId).subscribe({
      next: (res: any) => {
        this.isVerifyingUpiPay = false;
        if (res.verified) {
          this.upiPayVerified = true;
          this.upiPayVerifiedName = res.name || res.accountHolderName || 'Verified';
        } else {
          this.upiPayVerifyError = res.error || res.message || 'UPI ID not found or inactive';
        }
      },
      error: (err: any) => {
        this.isVerifyingUpiPay = false;
        this.upiPayVerifyError = err.error?.error || 'UPI verification failed';
      }
    });
  }

  submitUpiPay() {
    if (!this.upiPayOrder || !this.upiPayVerified) return;
    this.isSubmittingUpiPay = true;
    this.pgService.payWithUpiId(this.upiPayOrder.orderId, {
      upiId: this.upiPayForm.upiId,
      merchantId: this.merchant.merchantId
    }).subscribe({
      next: (res: any) => {
        this.isSubmittingUpiPay = false;
        if (res.success) {
          this.upiPaySuccess = res.message || 'Payment request sent! The user will authorize from their dashboard.';
          // Update order status in list
          const o = this.orders.find(x => x.orderId === this.upiPayOrder.orderId);
          if (o) o.status = 'ATTEMPTED';
          this.loadData();
          setTimeout(() => { this.showUpiPayModal = false; this.upiPaySuccess = ''; }, 4000);
        }
      },
      error: (err: any) => {
        this.isSubmittingUpiPay = false;
        this.upiPayVerifyError = err.error?.error || 'Failed to send payment request';
      }
    });
  }

  // ---- Delete Order ----
  deleteOrder(order: any) {
    if (!confirm(`Delete order ${order.orderId} for ₹${order.amount}? This cannot be undone.`)) return;
    this.pgService.deleteOrder(order.orderId).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.orders = this.orders.filter(o => o.orderId !== order.orderId);
        }
      },
      error: (err: any) => alert(err.error?.error || 'Failed to delete order')
    });
  }

  openLinkAccountModal() {
    this.showLinkAccountModal = true;
    this.linkAccountNumber = this.merchant?.linkedAccountNumber || '';
    this.linkAccountMessage = '';
    this.linkAccountSuccess = false;
  }

  linkReceivingAccount() {
    if (!this.linkAccountNumber?.trim() || !this.merchant?.merchantId) return;
    this.linkingAccount = true;
    this.linkAccountMessage = '';
    this.linkAccountSuccess = false;
    this.pgService.linkMerchantReceivingAccount(this.merchant.merchantId, this.linkAccountNumber.trim()).subscribe({
      next: (res: any) => {
        this.linkingAccount = false;
        if (res.success && res.merchant) {
          this.merchant = res.merchant;
          if (this.isBrowser) sessionStorage.setItem('pgMerchant', JSON.stringify(this.merchant));
          this.linkAccountSuccess = true;
          this.linkAccountMessage = res.message || 'Account linked successfully.';
          this.loadData();
        } else {
          this.linkAccountMessage = res.error || 'Failed to link account.';
        }
      },
      error: (err: any) => {
        this.linkingAccount = false;
        this.linkAccountMessage = err.error?.error || 'Failed to link account.';
      }
    });
  }
}
