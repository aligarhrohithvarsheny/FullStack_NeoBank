import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class PaymentGatewayService {
  private apiUrl = `${environment.apiBaseUrl}/api/payment-gateway`;

  constructor(private http: HttpClient) {}

  // Merchant operations
  registerMerchant(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/merchants/register`, data);
  }

  getMerchant(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/merchants/${merchantId}`);
  }

  getAllMerchants(): Observable<any> {
    return this.http.get(`${this.apiUrl}/merchants`);
  }

  // Order operations
  createOrder(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/orders/create`, data);
  }

  getOrder(orderId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/orders/${orderId}`);
  }

  getOrdersByMerchant(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/orders/merchant/${merchantId}`);
  }

  // Payment operations
  processPayment(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/pay`, data);
  }

  getTransaction(transactionId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/transactions/${transactionId}`);
  }

  getTransactionsByMerchant(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/transactions/merchant/${merchantId}`);
  }

  // Refund operations
  processRefund(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/refunds`, data);
  }

  getRefundsByMerchant(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/refunds/merchant/${merchantId}`);
  }

  getRefundsByTransaction(transactionId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/refunds/transaction/${transactionId}`);
  }

  // Signature verification
  verifySignature(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/verify-signature`, data);
  }

  // Analytics
  getMerchantAnalytics(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/analytics/${merchantId}`);
  }

  // OTP Login
  sendLoginOtp(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login/send-otp`, { email });
  }

  verifyLoginOtp(email: string, otp: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login/verify-otp`, { email, otp });
  }

  // UPI QR Payment Session
  createPaymentSession(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/payment-session/create`, data);
  }

  // Name Verification
  verifyPayerName(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/verify-name`, data);
  }

  // Admin PG Management
  getPendingMerchants(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/pending-merchants`);
  }

  getApprovedMerchants(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/approved-merchants`);
  }

  approveMerchant(merchantId: string, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/approve/${merchantId}`, data);
  }

  rejectMerchant(merchantId: string, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/reject/${merchantId}`, data);
  }

  toggleMerchantLogin(merchantId: string, enable: boolean): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/toggle-login/${merchantId}`, { enable });
  }

  // Settlement & Credit
  settleTransaction(transactionId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/settle/${transactionId}`, {});
  }

  getLedgerByMerchant(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/ledger/merchant/${merchantId}`);
  }

  getLedgerByAccount(accountNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/ledger/account/${accountNumber}`);
  }

  // Verify linked account
  verifyLinkedAccount(accountNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/verify-account/${accountNumber}`);
  }

  // Card Payment
  sendCardPaymentOtp(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/card/verify-and-send-otp`, data);
  }

  verifyCardPaymentOtp(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/card/verify-otp-and-pay`, data);
  }

  // Complete an order after an external UPI QR payment (no double-debit)
  completeOrderFromQrPayment(orderId: string, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/orders/${orderId}/complete`, data);
  }

  // Poll a single order status
  getOrderStatus(orderId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/orders/${orderId}`);
  }

  // ---- Payment Links ----
  verifyAnyUpiId(upiId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/upi/verify/${encodeURIComponent(upiId)}`);
  }

  sendPaymentLink(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/payment-links/send`, data);
  }

  getMerchantPaymentLinks(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/payment-links/merchant/${merchantId}`);
  }

  getCustomerPaymentLinks(upiId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/payment-links/customer/${encodeURIComponent(upiId)}`);
  }

  getPendingCustomerLinks(upiId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/payment-links/customer/${encodeURIComponent(upiId)}/pending`);
  }

  payPaymentLink(linkToken: string, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/payment-links/${linkToken}/pay`, data);
  }

  cancelPaymentLink(linkToken: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/payment-links/${linkToken}/cancel`, {});
  }

  deleteOrder(orderId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/orders/${orderId}`);
  }

  payWithUpiId(orderId: string, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/orders/${orderId}/pay-with-upi`, data);
  }
}

