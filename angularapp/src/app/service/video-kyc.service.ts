import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class VideoKycService {
  private apiUrl = `${environment.apiBaseUrl}/api/video-kyc`;

  constructor(private http: HttpClient) {}

  // ======================== User Endpoints ========================

  register(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data);
  }

  uploadDocuments(sessionId: number, aadharFile: File, panFile: File): Observable<any> {
    const formData = new FormData();
    formData.append('aadharDocument', aadharFile);
    formData.append('panDocument', panFile);
    return this.http.post(`${this.apiUrl}/upload-documents/${sessionId}`, formData);
  }

  startVideoKyc(sessionId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/start-video/${sessionId}`, {});
  }

  verifyOtp(sessionId: number, otp: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/verify-otp/${sessionId}`, { otp });
  }

  saveFaceSnapshot(sessionId: number, blob: Blob): Observable<any> {
    const formData = new FormData();
    formData.append('snapshot', blob, 'face-snapshot.jpg');
    return this.http.post(`${this.apiUrl}/snapshot/face/${sessionId}`, formData);
  }

  saveIdSnapshot(sessionId: number, blob: Blob): Observable<any> {
    const formData = new FormData();
    formData.append('snapshot', blob, 'id-snapshot.jpg');
    return this.http.post(`${this.apiUrl}/snapshot/id/${sessionId}`, formData);
  }

  livenessCheck(sessionId: number, passed: boolean, checkType: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/liveness-check/${sessionId}`, { passed, checkType });
  }

  endSession(sessionId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/end-session/${sessionId}`, {});
  }

  getStatus(sessionId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/status/${sessionId}`);
  }

  getStatusByMobile(mobile: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/status/mobile/${mobile}`);
  }

  // ======================== Document Download ========================

  getAadharDocument(sessionId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/document/aadhar/${sessionId}`, { responseType: 'blob' });
  }

  getPanDocument(sessionId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/document/pan/${sessionId}`, { responseType: 'blob' });
  }

  getFaceSnapshot(sessionId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/snapshot/face/${sessionId}`, { responseType: 'blob' });
  }

  getIdSnapshot(sessionId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/snapshot/id-proof/${sessionId}`, { responseType: 'blob' });
  }

  // ======================== Admin Endpoints ========================

  getKycQueue(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/queue`);
  }

  getAllSessions(page: number = 0, size: number = 20): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/all?page=${page}&size=${size}`);
  }

  filterByStatus(status: string, page: number = 0, size: number = 20): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/filter?status=${status}&page=${page}&size=${size}`);
  }

  searchSessions(query: string, page: number = 0, size: number = 20): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`);
  }

  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/stats`);
  }

  adminJoinSession(sessionId: number, adminId: number, adminName: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/join/${sessionId}?adminId=${adminId}&adminName=${encodeURIComponent(adminName)}`, {});
  }

  approveKyc(sessionId: number, adminId: number, adminName: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/approve/${sessionId}?adminId=${adminId}&adminName=${encodeURIComponent(adminName)}`, {});
  }

  rejectKyc(sessionId: number, adminId: number, adminName: string, reason: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/reject/${sessionId}?adminId=${adminId}&adminName=${encodeURIComponent(adminName)}&reason=${encodeURIComponent(reason)}`, {});
  }

  reopenSession(sessionId: number, adminId: number, adminName: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/reopen/${sessionId}?adminId=${adminId}&adminName=${encodeURIComponent(adminName)}`, {});
  }

  forceReVerify(sessionId: number, adminId: number, adminName: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/force-reverify/${sessionId}?adminId=${adminId}&adminName=${encodeURIComponent(adminName)}`, {});
  }

  getSessionDetail(sessionId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/session-detail/${sessionId}`);
  }

  getAuditLogs(sessionId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/audit-logs/${sessionId}`);
  }

  getAllAuditLogs(page: number = 0, size: number = 50): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/audit-logs?page=${page}&size=${size}`);
  }

  // ======================== Slot Booking ========================

  getAvailableSlots(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/slots/available`);
  }

  bookSlot(sessionId: number, slotId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/book-slot/${sessionId}`, { slotId });
  }

  cancelBooking(sessionId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/cancel-booking/${sessionId}`, {});
  }

  canJoinVideoKyc(sessionId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/can-join/${sessionId}`);
  }

  // ======================== Verification Number ========================

  verifyByNumber(verificationNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/verify/${encodeURIComponent(verificationNumber)}`);
  }

  // ======================== Admin Slot Management ========================

  createSlot(date: string, time: string, endTime: string, maxBookings: number, createdBy: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/slots/create`, { date, time, endTime, maxBookings, createdBy });
  }

  generateDefaultSlots(): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/slots/generate-defaults`, {});
  }

  getAllSlots(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/slots/all`);
  }

  cancelSlot(slotId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/admin/slots/${slotId}`);
  }

  rescheduleSlot(slotId: number, date: string, time: string, endTime: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/admin/slots/${slotId}/reschedule`, { date, time, endTime });
  }
}
