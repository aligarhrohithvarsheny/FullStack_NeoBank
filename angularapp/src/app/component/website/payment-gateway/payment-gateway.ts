import { Component, OnInit, OnDestroy, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PaymentGatewayService } from '../../../service/payment-gateway.service';

@Component({
  selector: 'app-payment-gateway',
  templateUrl: './payment-gateway.html',
  styleUrls: ['./payment-gateway.css'],
  encapsulation: ViewEncapsulation.None,
  imports: [CommonModule, FormsModule]
})
export class PaymentGateway implements OnInit, OnDestroy {
  // View state
  activeTab = 'dashboard';
  isLoading = false;
  showCreateOrder = false;
  showPayOrder = false;
  showRefundModal = false;
  showRegisterMerchant = false;

  // Merchant data
  currentMerchant: any = null;
  merchantId = '';
  merchants: any[] = [];

  // Orders & Transactions
  orders: any[] = [];
  transactions: any[] = [];
  refunds: any[] = [];
  analytics: any = {};

  // Create order form
  orderForm = {
    amount: '',
    description: '',
    customerName: '',
    customerEmail: '',
    customerPhone: '',
    receipt: ''
  };

  // Pay form
  payForm = {
    orderId: '',
    paymentMethod: 'UPI',
    payerAccount: '',
    payerName: ''
  };

  // Refund form
  refundForm = {
    transactionId: '',
    amount: '',
    reason: ''
  };

  // Register merchant form
  registerForm = {
    businessName: '',
    businessEmail: '',
    businessPhone: '',
    businessType: 'ONLINE',
    webhookUrl: '',
    callbackUrl: '',
    accountNumber: '',
    settlementAccount: ''
  };

  // Demo flow state
  demoStep = 0;
  demoOrder: any = null;
  demoTransaction: any = null;

  // Stats
  totalVolume = 0;
  successCount = 0;
  failedCount = 0;
  totalFees = 0;

  private pollInterval: any = null;
  private linkPollInterval: any = null;

  // Payment Links
  showSendLinkModal = false;
  paymentLinks: any[] = [];
  linkForm = { recipientUpiId: '', amount: '', description: '' };
  linkRecipientVerified = false;
  linkRecipientName = '';
  linkVerifyError = '';
  isVerifyingLinkUpi = false;
  isSendingLink = false;
  sendLinkSuccess = false;

  // Pay with UPI ID
  showUpiPayModal = false;
  upiPayOrder: any = null;
  upiPayForm = { upiId: '' };
  isVerifyingUpiPay = false;
  upiPayVerified = false;
  upiPayVerifiedName = '';
  upiPayVerifyError = '';
  isSubmittingUpiPay = false;
  upiPaySuccess = false;

  // Real-time transactions polling
  private txnPollInterval: any = null;

  constructor(
    private pgService: PaymentGatewayService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadMerchants();
  }

  ngOnDestroy() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
    if (this.linkPollInterval) {
      clearInterval(this.linkPollInterval);
      this.linkPollInterval = null;
    }
    if (this.txnPollInterval) {
      clearInterval(this.txnPollInterval);
      this.txnPollInterval = null;
    }
  }

  loadMerchants() {
    this.pgService.getAllMerchants().subscribe({
      next: (data: any[]) => {
        this.merchants = data;
        if (data.length > 0) {
          this.selectMerchant(data[0]);
        }
      },
      error: () => {}
    });
  }

  selectMerchant(merchant: any) {
    this.currentMerchant = merchant;
    this.merchantId = merchant.merchantId;
    this.loadMerchantData();
    this.loadPaymentLinks();
  }

  loadMerchantData() {
    if (!this.merchantId) return;
    this.isLoading = true;

    this.pgService.getOrdersByMerchant(this.merchantId).subscribe({
      next: (data: any[]) => {
        this.orders = data || [];
        // Start polling when there are pending orders; stop when all are settled
        const hasPending = this.orders.some(o => o.status === 'CREATED' || o.status === 'ATTEMPTED');
        if (hasPending && !this.pollInterval) {
          this.pollInterval = setInterval(() => this.loadMerchantData(), 5000);
        } else if (!hasPending && this.pollInterval) {
          clearInterval(this.pollInterval);
          this.pollInterval = null;
        }
      },
      error: () => this.orders = []
    });

    this.pgService.getTransactionsByMerchant(this.merchantId).subscribe({
      next: (data: any[]) => {
        this.transactions = data || [];
        this.isLoading = false;
        // Start real-time polling when there are pending/new transactions
        this.startTxnPolling();
      },
      error: () => { this.transactions = []; this.isLoading = false; }
    });

    this.pgService.getRefundsByMerchant(this.merchantId).subscribe({
      next: (data: any[]) => this.refunds = data || [],
      error: () => this.refunds = []
    });

    this.pgService.getMerchantAnalytics(this.merchantId).subscribe({
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

  // ---- Register Merchant ----
  registerMerchant() {
    if (!this.registerForm.businessName || !this.registerForm.businessEmail) return;
    this.isLoading = true;
    this.pgService.registerMerchant(this.registerForm).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.currentMerchant = res.merchant;
          this.merchantId = res.merchant.merchantId;
          this.merchants.push(res.merchant);
          this.showRegisterMerchant = false;
          this.loadMerchantData();
          alert('Merchant registered! API Key: ' + res.merchant.apiKey);
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        alert(err.error?.error || 'Registration failed');
        this.isLoading = false;
      }
    });
  }

  // ---- Create Order ----
  createOrder() {
    if (!this.orderForm.amount || !this.merchantId) return;
    this.isLoading = true;
    const payload = {
      merchantId: this.merchantId,
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
          this.demoOrder = res.order;
          this.showCreateOrder = false;
          this.demoStep = 1;
          this.orderForm = { amount: '', description: '', customerName: '', customerEmail: '', customerPhone: '', receipt: '' };
        }
        this.isLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Failed to create order'); this.isLoading = false; }
    });
  }

  // ---- Delete Order ----
  deleteOrder(order: any) {
    if (!confirm('Delete order ' + order.orderId + '? This cannot be undone.')) return;
    this.pgService.deleteOrder(order.orderId).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.orders = this.orders.filter(o => o.orderId !== order.orderId);
          if (this.demoOrder && this.demoOrder.orderId === order.orderId) {
            this.demoOrder = null;
            this.demoStep = 0;
          }
        }
      },
      error: (err: any) => alert(err.error?.error || 'Could not delete order')
    });
  }

  // ---- Open UPI Pay Modal ----
  openUpiPayModal(order: any) {
    this.upiPayOrder = order;
    this.upiPayForm = { upiId: '' };
    this.upiPayVerified = false;
    this.upiPayVerifiedName = '';
    this.upiPayVerifyError = '';
    this.upiPaySuccess = false;
    this.showUpiPayModal = true;
  }

  verifyUpiForPayment() {
    if (!this.upiPayForm.upiId) return;
    this.isVerifyingUpiPay = true;
    this.upiPayVerified = false;
    this.upiPayVerifyError = '';
    this.pgService.verifyAnyUpiId(this.upiPayForm.upiId).subscribe({
      next: (res: any) => {
        this.isVerifyingUpiPay = false;
        if (res.verified) {
          this.upiPayVerified = true;
          this.upiPayVerifiedName = res.name;
        } else {
          this.upiPayVerifyError = res.error || 'UPI ID not found';
        }
      },
      error: () => {
        this.isVerifyingUpiPay = false;
        this.upiPayVerifyError = 'Could not verify UPI ID';
      }
    });
  }

  upiPaySuccessMessage = '';

  submitUpiPay() {
    if (!this.upiPayVerified || !this.upiPayOrder) return;
    this.isSubmittingUpiPay = true;
    this.upiPayVerifyError = '';
    this.pgService.payWithUpiId(this.upiPayOrder.orderId, {
      upiId: this.upiPayForm.upiId
    }).subscribe({
      next: (res: any) => {
        this.isSubmittingUpiPay = false;
        if (res.success) {
          this.upiPaySuccess = true;
          this.upiPaySuccessMessage = res.message || 'Payment request sent successfully!';
          // Update order status in list
          const idx = this.orders.findIndex(o => o.orderId === this.upiPayOrder.orderId);
          if (idx !== -1) {
            this.orders[idx].status = 'ATTEMPTED';
          }
          this.loadMerchantData();
          setTimeout(() => { this.showUpiPayModal = false; this.upiPaySuccess = false; this.upiPaySuccessMessage = ''; }, 4000);
        }
      },
      error: (err: any) => {
        this.isSubmittingUpiPay = false;
        this.upiPayVerifyError = err.error?.error || 'Failed to send payment request';
      }
    });
  }

  // ---- Real-time transaction polling ----
  startTxnPolling() {
    if (this.txnPollInterval) return;
    this.txnPollInterval = setInterval(() => {
      if (!this.merchantId) return;
      this.pgService.getTransactionsByMerchant(this.merchantId).subscribe({
        next: (data: any[]) => {
          const incoming = data || [];
          if (incoming.length !== this.transactions.length ||
              (incoming[0] && this.transactions[0] && incoming[0].transactionId !== this.transactions[0].transactionId)) {
            this.transactions = incoming;
          }
        },
        error: () => {}
      });
    }, 4000);
  }

  // ---- Process Payment ----
  processPayment() {
    if (!this.payForm.orderId) return;
    this.isLoading = true;
    this.pgService.processPayment(this.payForm).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.demoTransaction = res.transaction;
          this.transactions.unshift(res.transaction);
          this.showPayOrder = false;
          this.demoStep = 2;
          this.loadMerchantData();
        }
        this.isLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Payment failed'); this.isLoading = false; }
    });
  }

  // ---- Refund ----
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
          this.loadMerchantData();
        }
        this.isLoading = false;
      },
      error: (err: any) => { alert(err.error?.error || 'Refund failed'); this.isLoading = false; }
    });
  }

  // ---- Demo Flow ----
  startDemoFlow() {
    this.demoStep = 0;
    this.demoOrder = null;
    this.demoTransaction = null;
    this.showCreateOrder = true;
    this.orderForm = {
      amount: '1500',
      description: 'Demo Payment - Premium Plan',
      customerName: 'John Doe',
      customerEmail: 'john@example.com',
      customerPhone: '9876543210',
      receipt: 'DEMO' + Date.now()
    };
  }

  openPayForOrder(order: any) {
    this.payForm.orderId = order.orderId;
    this.payForm.payerName = order.customerName || '';
    this.showPayOrder = true;
  }

  openRefundForTxn(txn: any) {
    this.refundForm.transactionId = txn.transactionId;
    this.refundForm.amount = txn.amount;
    this.refundForm.reason = '';
    this.showRefundModal = true;
  }

  getStatusClass(status: string): string {
    const map: any = {
      'SUCCESS': 'status-success', 'PAID': 'status-success', 'PROCESSED': 'status-success',
      'CREATED': 'status-pending', 'INITIATED': 'status-pending', 'PENDING': 'status-pending',
      'FAILED': 'status-failed', 'EXPIRED': 'status-failed', 'FLAGGED': 'status-flagged'
    };
    return map[status] || 'status-default';
  }

  goBack() {
    this.router.navigate(['/website/landing']);
  }

  // ---- Payment Links ----

  loadPaymentLinks() {
    if (!this.merchantId) return;
    this.pgService.getMerchantPaymentLinks(this.merchantId).subscribe({
      next: (data: any[]) => {
        this.paymentLinks = data || [];
        // Poll while any links are PENDING
        const hasPending = this.paymentLinks.some((l: any) => l.status === 'PENDING');
        if (hasPending && !this.linkPollInterval) {
          this.linkPollInterval = setInterval(() => this.loadPaymentLinks(), 5000);
        } else if (!hasPending && this.linkPollInterval) {
          clearInterval(this.linkPollInterval);
          this.linkPollInterval = null;
        }
      },
      error: () => this.paymentLinks = []
    });
  }

  openSendLinkModal() {
    this.showSendLinkModal = true;
    this.linkForm = { recipientUpiId: '', amount: '', description: '' };
    this.linkRecipientVerified = false;
    this.linkRecipientName = '';
    this.linkVerifyError = '';
    this.sendLinkSuccess = false;
  }

  verifyLinkRecipientUpi() {
    if (!this.linkForm.recipientUpiId) return;
    this.isVerifyingLinkUpi = true;
    this.linkRecipientVerified = false;
    this.linkVerifyError = '';
    this.pgService.verifyAnyUpiId(this.linkForm.recipientUpiId).subscribe({
      next: (res: any) => {
        this.isVerifyingLinkUpi = false;
        if (res.verified) {
          this.linkRecipientVerified = true;
          this.linkRecipientName = res.name;
        } else {
          this.linkVerifyError = res.error || 'UPI ID not found';
        }
      },
      error: () => {
        this.isVerifyingLinkUpi = false;
        this.linkVerifyError = 'Could not verify UPI ID';
      }
    });
  }

  sendLink() {
    if (!this.linkRecipientVerified || !this.linkForm.amount || !this.merchantId) return;
    this.isSendingLink = true;
    this.pgService.sendPaymentLink({
      merchantId: this.merchantId,
      recipientUpiId: this.linkForm.recipientUpiId,
      amount: parseFloat(this.linkForm.amount),
      description: this.linkForm.description || 'Payment Request'
    }).subscribe({
      next: (res: any) => {
        this.isSendingLink = false;
        if (res.success) {
          this.sendLinkSuccess = true;
          this.loadPaymentLinks();
          setTimeout(() => { this.showSendLinkModal = false; this.sendLinkSuccess = false; }, 2500);
        }
      },
      error: () => { this.isSendingLink = false; }
    });
  }

  cancelLink(linkToken: string) {
    this.pgService.cancelPaymentLink(linkToken).subscribe({
      next: () => this.loadPaymentLinks(),
      error: () => {}
    });
  }
}

