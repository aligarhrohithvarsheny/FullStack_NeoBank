import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environment/environment';
import { Observable } from 'rxjs';

/** FASTag application model */
export interface FasttagApplication {
  id: string;
  userId: string;
  userName?: string;
  vehicleDetails: string;
  vehicleNumber: string;
  aadharNumber: string;
  panNumber: string;
  dob: string;
  vehicleType: string;
  amount: number;
  bank: string;
  status: 'Applied' | 'Approved' | 'Rejected' | 'Closed';
  fasttagNumber?: string;
  barcodeNumber?: string;
  issueDate?: string;
  stickerPath?: string;
  balance?: number;
  createdAt: string;
  assignedAccount?: { accountNumber?: string };
  assignedAccountId?: string;
  assignedAt?: string;
  // New purchase fields
  chassisNumber?: string;
  engineNumber?: string;
  make?: string;
  model?: string;
  fuelType?: string;
  rcFrontPath?: string;
  rcBackPath?: string;
  mobileNumber?: string;
  email?: string;
  dispatchAddress?: string;
  pinCode?: string;
  city?: string;
  state?: string;
  tagIssuanceFee?: number;
  tagSecurityDeposit?: number;
  tagUploadAmount?: number;
  tagTotalAmount?: number;
  debitAccountNumber?: string;
  termsAccepted?: boolean;
}

const STORAGE_KEY = 'fasttag_applications';

@Injectable({
  providedIn: 'root'
})
export class FasttagService {
  constructor(private http: HttpClient) {}

  listAll(): Observable<FasttagApplication[]> {
    return this.http.get<FasttagApplication[]>(`${environment.apiBaseUrl}/api/fasttags`);
  }

  listForUser(userId: string): Observable<FasttagApplication[]> {
    return this.http.get<FasttagApplication[]>(`${environment.apiBaseUrl}/api/fasttags/user/${userId}`);
  }

  apply(app: Partial<FasttagApplication>): Observable<FasttagApplication> {
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags`, app);
  }

  adminApply(app: Partial<FasttagApplication>): Observable<FasttagApplication> {
    return this.apply(app);
  }

  approve(id: string): Observable<FasttagApplication> {
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/approve/${id}`, {});
  }

  rechargeByTag(fasttagNumber: string, amount: number): Observable<FasttagApplication> {
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/recharge`, { fasttagNumber, amount });
  }

  rechargeByUser(data: { vehicleNumber?: string; fasttagNumber?: string; amount: number; debitAccountNumber?: string; userId?: string }): Observable<FasttagApplication> {
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/recharge/user`, data);
  }

  getById(id: string): Observable<FasttagApplication> {
    return this.http.get<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/${id}`);
  }

  // Admin recharge specifying initiator (ADMIN) and initiator id
  rechargeByTagAsAdmin(fasttagNumber: string, amount: number, adminId: string): Observable<FasttagApplication> {
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/recharge`, { fasttagNumber, amount, initiatedBy: 'ADMIN', initiatedById: adminId });
  }

  // fetch transactions for a fasttag by id
  getTransactions(fasttagId: string) {
    return this.http.get<any[]>(`${environment.apiBaseUrl}/api/fasttags/${fasttagId}/transactions`);
  }

  closeFasttag(id: string) {
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/close/${id}`, {});
  }

  // Admin recharge specifying an explicit debit account
  rechargeByTagAsAdminWithAccount(fasttagNumber: string, amount: number, adminId: string, accountId: string): Observable<FasttagApplication> {
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/recharge/admin`, {
      fasttagNumber, amount, initiatedById: adminId, accountId
    });
  }

  // Assign a bank account to a fasttag
  assignAccount(fasttagId: string, accountId: string): Observable<FasttagApplication> {
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/${fasttagId}/assign-account`, { accountId });
  }

  // Upload RC files
  uploadRcFiles(fasttagId: string, rcFront?: File, rcBack?: File): Observable<FasttagApplication> {
    const formData = new FormData();
    if (rcFront) formData.append('rcFront', rcFront);
    if (rcBack) formData.append('rcBack', rcBack);
    return this.http.post<FasttagApplication>(`${environment.apiBaseUrl}/api/fasttags/${fasttagId}/upload-rc`, formData);
  }

  // Get RC image URL
  getRcImageUrl(filename: string): string {
    return `${environment.apiBaseUrl}/api/fasttags/rc-image/${filename}`;
  }

  // Get sticker download URL
  downloadSticker(id: string): string {
    return `${environment.apiBaseUrl}/api/fasttags/${id}/sticker`;
  }
}
