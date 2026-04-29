import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UpiPayment } from '../model/upi/upi.model';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class UpiService {
  private apiUrl = `${environment.apiBaseUrl}/api/current-accounts/upi`;

  constructor(private http: HttpClient) {}

  // ==================== UPI ID Setup ====================

  setupUpi(accountNumber: string, upiId?: string): Observable<any> {
    let params = new HttpParams();
    if (upiId) params = params.set('upiId', upiId);
    return this.http.post<any>(`${this.apiUrl}/setup/${accountNumber}`, null, { params });
  }

  updateUpi(accountNumber: string, upiId: string): Observable<any> {
    const params = new HttpParams().set('upiId', upiId);
    return this.http.put<any>(`${this.apiUrl}/update/${accountNumber}`, null, { params });
  }

  toggleUpi(accountNumber: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/toggle/${accountNumber}`, null);
  }

  // ==================== QR Code ====================

  generateQrCode(accountNumber: string, amount?: number): Observable<any> {
    let params = new HttpParams();
    if (amount) params = params.set('amount', amount.toString());
    return this.http.get<any>(`${this.apiUrl}/qrcode/${accountNumber}`, { params });
  }

  generateQrCodeForUpi(upiId: string, accountNumber: string, amount?: number): Observable<any> {
    let params = new HttpParams()
      .set('upiId', upiId)
      .set('accountNumber', accountNumber);
    if (amount) params = params.set('amount', amount.toString());
    return this.http.get<any>(`${this.apiUrl}/qrcode/generate`, { params });
  }

  // ==================== UPI Verification ====================

  verifyUpiId(upiId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify/${encodeURIComponent(upiId)}`);
  }

  // ==================== Payments ====================

  processPayment(payment: UpiPayment): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/payment/process`, payment);
  }

  getPayments(accountNumber: string): Observable<UpiPayment[]> {
    return this.http.get<UpiPayment[]>(`${this.apiUrl}/payments/${accountNumber}`);
  }

  getPaymentsPaginated(accountNumber: string, page: number, size: number): Observable<any> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/payments/${accountNumber}/paginated`, { params });
  }

  // ==================== Stats ====================

  getUserStats(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/user/${accountNumber}`);
  }

  getAdminStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/admin`);
  }

  getRecentPayments(): Observable<UpiPayment[]> {
    return this.http.get<UpiPayment[]>(`${this.apiUrl}/payments/recent`);
  }

  // ==================== Transaction Search ====================

  searchByTxnId(txnId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/search/txn/${encodeURIComponent(txnId)}`);
  }

  // ==================== Cross-Customer Payment ====================

  crossCustomerPayment(senderAccountNumber: string, receiverUpiId: string, amount: number, note?: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/payment/cross-pay`, {
      senderAccountNumber,
      receiverUpiId,
      amount,
      note: note || ''
    });
  }
}
