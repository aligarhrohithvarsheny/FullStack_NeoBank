import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Agent,
  Merchant,
  MerchantApplication,
  MerchantDevice,
  MerchantTransaction,
  MerchantCreatePayload
} from '../model/merchant-onboarding/merchant-onboarding.model';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class MerchantOnboardingService {
  private apiUrl = `${environment.apiBaseUrl}/api/merchant-onboarding`;

  constructor(private http: HttpClient) {}

  // ==================== Agent Authentication ====================

  agentLogin(credentials: { email?: string; mobile?: string; password: string }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/agent/login`, credentials);
  }

  getAgent(agentId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/agent/${agentId}`);
  }

  getAgentStats(agentId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/agent/${agentId}/stats`);
  }

  // ==================== Merchant CRUD ====================

  createMerchant(payload: MerchantCreatePayload): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/merchants`, payload);
  }

  updateMerchant(merchantId: string, merchant: Partial<Merchant>): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/merchants/${merchantId}`, merchant);
  }

  updateMerchantAdmin(merchantId: string, merchant: Partial<Merchant>): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/admin/merchants/${merchantId}`, merchant);
  }

  getMerchant(merchantId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/merchants/${merchantId}`);
  }

  getMerchantByMobile(mobile: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/merchants/mobile/${mobile}`);
  }

  getMerchantsByAgent(agentId: string): Observable<Merchant[]> {
    return this.http.get<Merchant[]>(`${this.apiUrl}/merchants/agent/${agentId}`);
  }

  getMerchantsByAgentAndStatus(agentId: string, status: string): Observable<Merchant[]> {
    return this.http.get<Merchant[]>(`${this.apiUrl}/merchants/agent/${agentId}/status/${status}`);
  }

  getAllMerchants(): Observable<Merchant[]> {
    return this.http.get<Merchant[]>(`${this.apiUrl}/merchants/all`);
  }

  getMerchantsByStatus(status: string): Observable<Merchant[]> {
    return this.http.get<Merchant[]>(`${this.apiUrl}/merchants/status/${status}`);
  }

  // ==================== Admin Approve / Reject ====================

  approveMerchant(merchantId: string, adminName: string): Observable<any> {
    const params = new HttpParams().set('adminName', adminName);
    return this.http.put<any>(`${this.apiUrl}/merchants/${merchantId}/approve`, null, { params });
  }

  rejectMerchant(merchantId: string, adminName: string, reason: string): Observable<any> {
    const params = new HttpParams()
      .set('adminName', adminName)
      .set('reason', reason);
    return this.http.put<any>(`${this.apiUrl}/merchants/${merchantId}/reject`, null, { params });
  }

  // ==================== Applications ====================

  getAllApplications(): Observable<MerchantApplication[]> {
    return this.http.get<MerchantApplication[]>(`${this.apiUrl}/applications/all`);
  }

  getApplicationsByStatus(status: string): Observable<MerchantApplication[]> {
    return this.http.get<MerchantApplication[]>(`${this.apiUrl}/applications/status/${status}`);
  }

  getApplicationsByMerchant(merchantId: string): Observable<MerchantApplication[]> {
    return this.http.get<MerchantApplication[]>(`${this.apiUrl}/applications/merchant/${merchantId}`);
  }

  // ==================== Devices ====================

  getDevicesByMerchant(merchantId: string): Observable<MerchantDevice[]> {
    return this.http.get<MerchantDevice[]>(`${this.apiUrl}/devices/merchant/${merchantId}`);
  }

  getAllDevices(): Observable<MerchantDevice[]> {
    return this.http.get<MerchantDevice[]>(`${this.apiUrl}/devices/all`);
  }

  toggleDevice(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/devices/toggle/${id}`, null);
  }

  // ==================== Transactions ====================

  getTransactionsByMerchant(merchantId: string): Observable<MerchantTransaction[]> {
    return this.http.get<MerchantTransaction[]>(`${this.apiUrl}/transactions/merchant/${merchantId}`);
  }

  sendMerchantLoginOtp(email: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/merchant/login/send-otp`, { email });
  }

  verifyMerchantLoginOtp(email: string, otp: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/merchant/login/verify-otp`, { email, otp });
  }

  getMerchantDashboard(merchantId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/merchant/${merchantId}/dashboard`);
  }

  updateMerchantDeviceStatus(merchantId: string, deviceId: string, status: 'ACTIVE' | 'INACTIVE' | 'PAUSED'): Observable<any> {
    const params = new HttpParams().set('status', status);
    return this.http.put<any>(`${this.apiUrl}/merchant/${merchantId}/devices/${deviceId}/status`, null, { params });
  }

  debitMerchantSpeakerCharges(merchantId: string, amount: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/merchant/${merchantId}/charges/debit`, { amount, reason });
  }

  adminUpdateMerchantDeviceStatus(merchantId: string, deviceId: string, status: 'ACTIVE' | 'INACTIVE' | 'PAUSED'): Observable<any> {
    const params = new HttpParams().set('status', status);
    return this.http.put<any>(`${this.apiUrl}/admin/merchant/${merchantId}/devices/${deviceId}/status`, null, { params });
  }

  adminDebitMerchantSpeakerCharges(merchantId: string, deviceId: string, amount: number, reason: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/merchant/${merchantId}/devices/${deviceId}/charges/debit`, { amount, reason });
  }

  adminLinkMerchantUpi(
    merchantId: string,
    deviceId: string,
    upiId: string,
    beneficiaryName: string,
    verifyDepositAmount: number
  ): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/merchant/${merchantId}/devices/${deviceId}/link-upi`, {
      upiId,
      beneficiaryName,
      verifyDepositAmount
    });
  }

  // ==================== File Upload ====================

  uploadDocuments(merchantId: string, shopPhoto?: File, ownerIdProof?: File, bankProof?: File): Observable<any> {
    const formData = new FormData();
    if (shopPhoto) formData.append('shopPhoto', shopPhoto);
    if (ownerIdProof) formData.append('ownerIdProof', ownerIdProof);
    if (bankProof) formData.append('bankProof', bankProof);
    return this.http.post<any>(`${this.apiUrl}/merchants/${merchantId}/upload`, formData);
  }

  // ==================== Admin Stats ====================

  getAdminStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/admin`);
  }

  // ==================== Agent Management (Admin) ====================

  createAgent(agent: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/agents`, agent);
  }

  getAllAgents(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/agents/all`);
  }

  getAgentManagementStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/agents/stats`);
  }

  updateAgentProfile(agentId: string, updates: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/agents/${agentId}/profile`, updates);
  }

  freezeAgent(agentId: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/agents/${agentId}/freeze`, null);
  }

  unfreezeAgent(agentId: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/agents/${agentId}/unfreeze`, null);
  }

  deactivateAgent(agentId: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/agents/${agentId}/deactivate`, null);
  }

  reactivateAgent(agentId: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/agents/${agentId}/reactivate`, null);
  }

  generateAgentOtp(agentId: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/agents/${agentId}/generate-otp`, null);
  }

  verifyAgentOtp(agentId: string, otp: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/agents/${agentId}/verify-otp`, { otp });
  }

  uploadAgentIdCard(agentId: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('idCard', file);
    return this.http.post<any>(`${this.apiUrl}/agents/${agentId}/upload-idcard`, formData);
  }

  uploadAgentPhoto(agentId: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('photo', file);
    return this.http.post<any>(`${this.apiUrl}/agents/${agentId}/upload-photo`, formData);
  }
}
