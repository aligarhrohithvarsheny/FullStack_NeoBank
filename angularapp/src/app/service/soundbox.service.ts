import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SoundboxDevice, SoundboxLinkedAccount, SoundboxRequest, SoundboxTransaction } from '../model/soundbox/soundbox.model';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class SoundboxService {
  private apiUrl = `${environment.apiBaseUrl}/api/soundbox`;

  constructor(private http: HttpClient) {}

  // ==================== Request Operations ====================

  applyForSoundbox(request: SoundboxRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/requests/apply`, request);
  }

  getRequestsByAccount(accountNumber: string): Observable<SoundboxRequest[]> {
    return this.http.get<SoundboxRequest[]>(`${this.apiUrl}/requests/account/${accountNumber}`);
  }

  getPendingRequests(): Observable<SoundboxRequest[]> {
    return this.http.get<SoundboxRequest[]>(`${this.apiUrl}/requests/pending`);
  }

  getAllRequests(): Observable<SoundboxRequest[]> {
    return this.http.get<SoundboxRequest[]>(`${this.apiUrl}/requests/all`);
  }

  approveRequest(id: number, adminName: string, deviceId: string, monthlyCharge: number | undefined, deviceCharge: number | undefined, allocationId: number): Observable<any> {
    let params = new HttpParams()
      .set('adminName', adminName)
      .set('deviceId', deviceId)
      .set('allocationId', allocationId.toString());
    if (monthlyCharge != null) params = params.set('monthlyCharge', monthlyCharge.toString());
    if (deviceCharge != null) params = params.set('deviceCharge', deviceCharge.toString());
    return this.http.put<any>(`${this.apiUrl}/requests/approve/${id}`, null, { params });
  }

  rejectRequest(id: number, adminName: string, remarks: string): Observable<any> {
    const params = new HttpParams()
      .set('adminName', adminName)
      .set('remarks', remarks);
    return this.http.put<any>(`${this.apiUrl}/requests/reject/${id}`, null, { params });
  }

  // ==================== Device Operations ====================

  getDeviceByAccount(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/devices/account/${accountNumber}`);
  }

  getAllDevices(): Observable<SoundboxDevice[]> {
    return this.http.get<SoundboxDevice[]>(`${this.apiUrl}/devices/all`);
  }

  getDevicesByStatus(status: string): Observable<SoundboxDevice[]> {
    return this.http.get<SoundboxDevice[]>(`${this.apiUrl}/devices/status/${status}`);
  }

  updateDeviceSettings(accountNumber: string, settings: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/devices/settings/${accountNumber}`, settings);
  }

  toggleDeviceStatus(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/devices/toggle/${id}`, null);
  }

  linkUpi(accountNumber: string, upiId: string): Observable<any> {
    const params = new HttpParams().set('upiId', upiId);
    return this.http.put<any>(`${this.apiUrl}/devices/link-upi/${accountNumber}`, null, { params });
  }

  removeUpi(accountNumber: string, upiId: string): Observable<any> {
    const params = new HttpParams().set('upiId', upiId);
    return this.http.put<any>(`${this.apiUrl}/devices/remove-upi/${accountNumber}`, null, { params });
  }

  updateCharges(id: number, monthlyCharge?: number, deviceCharge?: number): Observable<any> {
    let params = new HttpParams();
    if (monthlyCharge != null) params = params.set('monthlyCharge', monthlyCharge.toString());
    if (deviceCharge != null) params = params.set('deviceCharge', deviceCharge.toString());
    return this.http.put<any>(`${this.apiUrl}/devices/charges/${id}`, null, { params });
  }

  // ==================== Payment / Transaction Operations ====================

  processPayment(transaction: SoundboxTransaction): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/payment/process`, transaction);
  }

  lookupLinkedAccount(customerId: string, accountNumber: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/accounts/lookup`, { customerId, accountNumber });
  }

  requestLinkedAccount(soundboxAccountNumber: string, customerId: string, accountNumber: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/accounts/link`, { soundboxAccountNumber, customerId, accountNumber });
  }

  getLinkedAccounts(soundboxAccountNumber: string): Observable<SoundboxLinkedAccount[]> {
    return this.http.get<SoundboxLinkedAccount[]>(`${this.apiUrl}/accounts/linked/${soundboxAccountNumber}`);
  }

  getPendingLinkedAccounts(): Observable<SoundboxLinkedAccount[]> {
    return this.http.get<SoundboxLinkedAccount[]>(`${this.apiUrl}/admin/accounts/pending`);
  }

  reviewLinkedAccount(id: number, adminName: string, approve: boolean, remarks = ''): Observable<any> {
    const params = new HttpParams().set('adminName', adminName).set('remarks', remarks);
    const action = approve ? 'approve' : 'reject';
    return this.http.put<any>(`${this.apiUrl}/admin/accounts/${id}/${action}`, null, { params });
  }

  getPendingPayments(): Observable<SoundboxTransaction[]> {
    return this.http.get<SoundboxTransaction[]>(`${this.apiUrl}/admin/payments/pending`);
  }

  reviewPayment(id: number, adminName: string, approve: boolean): Observable<any> {
    const params = new HttpParams().set('adminName', adminName);
    const action = approve ? 'approve' : 'reject';
    return this.http.put<any>(`${this.apiUrl}/admin/payments/${id}/${action}`, null, { params });
  }

  getTransactionsByAccount(accountNumber: string): Observable<SoundboxTransaction[]> {
    return this.http.get<SoundboxTransaction[]>(`${this.apiUrl}/transactions/${accountNumber}`);
  }

  getTransactionsBySoundbox(accountNumber: string): Observable<SoundboxTransaction[]> {
    return this.http.get<SoundboxTransaction[]>(`${this.apiUrl}/transactions/soundbox/${accountNumber}`);
  }

  getTransactionsPaginated(accountNumber: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/transactions/${accountNumber}/paginated`, { params });
  }

  // ==================== Statistics ====================

  getUserStats(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/user/${accountNumber}`);
  }

  getSoundboxStats(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/soundbox/${accountNumber}`);
  }

  getAdminStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/admin`);
  }

  // ==================== Voice Alert (Browser TTS) ====================

  speakPayment(amount: number, language: string = 'en-IN'): void {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      let text = '';
      if (language === 'hi-IN') {
        text = `NeoBank Current Account mein ${amount} rupaye prapt hue`;
      } else if (language === 'te-IN') {
        text = `NeoBank Current Account lo ${amount} rupayalu andayi`;
      } else {
        text = `Received ${amount} rupees in NeoBank Current Account`;
      }
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = language;
      utterance.rate = 0.9;
      utterance.pitch = 1;
      utterance.volume = 1;
      window.speechSynthesis.speak(utterance);
    }
  }

  speakDailySummary(totalAmount: number, language: string = 'en-IN'): void {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      let text = '';
      if (language === 'hi-IN') {
        text = `Aaj ka kul prapt rashi ${totalAmount} rupaye`;
      } else if (language === 'te-IN') {
        text = `Ee roju total ga ${totalAmount} rupayalu andayi`;
      } else {
        text = `Today total received ${totalAmount} rupees`;
      }
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = language;
      utterance.rate = 0.9;
      utterance.volume = 1;
      window.speechSynthesis.speak(utterance);
    }
  }
}
