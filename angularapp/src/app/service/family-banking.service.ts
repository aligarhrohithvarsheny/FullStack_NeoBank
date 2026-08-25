import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

export interface FamilyInvitation { id: number; accountNumber: string; inviterUserId: number; inviteeUserId: number; status: string; createdAt: string; }
export interface JointTransfer { id: number; accountNumber: string; fromUserId: number; approverUserId: number; toAccountNumber: string; amount: number; status: string; note?: string; transactionReference?: string; }
export interface MinorApplication { id: number; guardianUserId: number; minorName: string; dateOfBirth: string; monthlyLimit: number; dailyLimit: number; status: string; }
export interface FamilyNotification { id: number; recipientUserId: number; type: string; title: string; message: string; createdAt: string; readAt?: string; }

export interface JointAccountProfile {
  id: number;
  jointAccountNumber: string;
  primaryHolderUserId: number;
  jointHolderUserId: number;
  operatingMode: string;
  status: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class FamilyBankingService {
  private readonly api = `${environment.apiBaseUrl}/api/family`;
  constructor(private http: HttpClient) {}
  invitations(userId: number): Observable<FamilyInvitation[]> { return this.http.get<FamilyInvitation[]>(`${this.api}/invitations`, { params: { userId } }); }
  invite(userId: number, accountNumber: string, inviteeEmail: string): Observable<FamilyInvitation> { return this.http.post<FamilyInvitation>(`${this.api}/invitations`, { userId, accountNumber, inviteeEmail }); }
  respondInvitation(userId: number, id: number, approve: boolean): Observable<FamilyInvitation> { return this.http.post<FamilyInvitation>(`${this.api}/invitations/${id}/respond`, { userId, approve }); }
  pendingTransfers(userId: number): Observable<JointTransfer[]> { return this.http.get<JointTransfer[]>(`${this.api}/transfers/pending`, { params: { userId } }); }
  requestTransfer(userId: number, accountNumber: string, toAccountNumber: string, amount: number, note: string): Observable<JointTransfer> { return this.http.post<JointTransfer>(`${this.api}/transfers`, { userId, accountNumber, toAccountNumber, amount, note }); }
  decideTransfer(userId: number, id: number, approve: boolean): Observable<JointTransfer> { return this.http.post<JointTransfer>(`${this.api}/transfers/${id}/decide`, { userId, approve }); }
  applications(guardianUserId: number): Observable<MinorApplication[]> { return this.http.get<MinorApplication[]>(`${this.api}/minor-applications`, { params: { guardianUserId } }); }
  applyMinor(data: { guardianUserId: number; minorName: string; dateOfBirth: string; monthlyLimit: number; dailyLimit: number }): Observable<MinorApplication> { return this.http.post<MinorApplication>(`${this.api}/minor-applications`, data); }
  guardianLinks(guardianUserId: number): Observable<any[]> { return this.http.get<any[]>(`${this.api}/guardian-links`, { params: { guardianUserId } }); }
  jointAccounts(userId: number): Observable<JointAccountProfile[]> { return this.http.get<JointAccountProfile[]>(`${this.api}/joint-accounts?userId=${userId}`); }
  notifications(userId: number): Observable<FamilyNotification[]> { return this.http.get<FamilyNotification[]>(`${this.api}/notifications`, { params: { userId } }); }
  markNotificationRead(userId: number, id: number): Observable<FamilyNotification> { return this.http.post<FamilyNotification>(`${this.api}/notifications/${id}/read`, { userId, approve: true }); }
}
