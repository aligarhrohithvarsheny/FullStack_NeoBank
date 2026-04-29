import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SoundboxDevice, SoundboxRequest, SoundboxTransaction } from '../model/soundbox/soundbox.model';
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

  approveRequest(id: number, adminName: string, deviceId: string, monthlyCharge?: number, deviceCharge?: number): Observable<any> {
    let params = new HttpParams()
      .set('adminName', adminName)
      .set('deviceId', deviceId);
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

  getTransactionsByAccount(accountNumber: string): Observable<SoundboxTransaction[]> {
    return this.http.get<SoundboxTransaction[]>(`${this.apiUrl}/transactions/${accountNumber}`);
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
