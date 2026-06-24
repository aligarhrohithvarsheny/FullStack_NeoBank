import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

export type BankFormAccountType = 'regular' | 'loan' | 'goldloan' | 'cheque' | 'salary' | 'current';

export interface BankFormDefinition {
  id: number;
  code: string;
  name: string;
  category: string;
  fields: string[];
  commonFields: string[];
  allFields: string[];
}

export interface BankFormUploadRecord {
  id: number;
  formCode: string;
  formName: string;
  category: string;
  accountNumber: string;
  accountType: string;
  accountHolderName: string;
  originalFileName: string;
  storedFilePath: string;
  contentType: string;
  fileSizeBytes: number;
  uploadedByAdmin: string;
  remarks: string;
  uploadedAt: string;
}

export interface BankFormUploadHistoryRecord {
  id: number;
  uploadId: number;
  action: string;
  previousFileName?: string;
  newFileName?: string;
  performedByAdmin?: string;
  remarks?: string;
  performedAt: string;
}

export interface VerifiedAccountDetails {
  accountNumber: string;
  holderName: string;
  accountType: string;
  customerId?: string;
  aadhaarNumber?: string;
  panNumber?: string;
  phone?: string;
  email?: string;
  balance?: number;
}

export interface BankFormDownloadOptions {
  adminName: string;
  accountNumber?: string;
  accountType?: string;
  holderName?: string;
  customerId?: string;
  aadhaarNumber?: string;
  panNumber?: string;
  phone?: string;
  email?: string;
}

@Injectable({ providedIn: 'root' })
export class BankFormService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/admin/bank-forms`;

  constructor(private http: HttpClient) {}

  getCatalog(): Observable<{ success: boolean; forms: BankFormDefinition[]; categories: string[]; total: number }> {
    return this.http.get<any>(`${this.baseUrl}/catalog`);
  }

  verifyAccount(options: {
    accountNumber?: string;
    accountType?: string;
    customerId?: string;
  }): Observable<any> {
    let params = new HttpParams();
    if (options.accountNumber) {
      params = params.set('accountNumber', options.accountNumber);
    }
    if (options.accountType) {
      params = params.set('accountType', options.accountType);
    }
    if (options.customerId) {
      params = params.set('customerId', options.customerId);
    }
    return this.http.get(`${this.baseUrl}/verify-account`, { params });
  }

  downloadBlankPdf(formCode: string, options: BankFormDownloadOptions) {
    let params = new HttpParams().set('adminName', options.adminName || 'Admin');
    if (options.accountNumber) params = params.set('accountNumber', options.accountNumber);
    if (options.accountType) params = params.set('accountType', options.accountType);
    if (options.holderName) params = params.set('holderName', options.holderName);
    if (options.customerId) params = params.set('customerId', options.customerId);
    if (options.aadhaarNumber) params = params.set('aadhaarNumber', options.aadhaarNumber);
    if (options.panNumber) params = params.set('panNumber', options.panNumber);
    if (options.phone) params = params.set('phone', options.phone);
    if (options.email) params = params.set('email', options.email);
    return this.http.get(`${this.baseUrl}/download/${encodeURIComponent(formCode)}`, {
      params,
      responseType: 'blob',
      observe: 'response'
    });
  }

  uploadForm(
    formCode: string,
    accountNumber: string,
    accountType: string,
    file: File,
    uploadedByAdmin: string,
    remarks?: string
  ): Observable<any> {
    const formData = new FormData();
    formData.append('formCode', formCode);
    formData.append('accountNumber', accountNumber);
    formData.append('accountType', accountType);
    formData.append('file', file);
    formData.append('uploadedByAdmin', uploadedByAdmin);
    if (remarks) {
      formData.append('remarks', remarks);
    }
    return this.http.post(`${this.baseUrl}/upload`, formData);
  }

  replaceUpload(id: number, file: File, replacedByAdmin: string, remarks?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('replacedByAdmin', replacedByAdmin);
    if (remarks) {
      formData.append('remarks', remarks);
    }
    return this.http.put(`${this.baseUrl}/uploads/${id}/replace`, formData);
  }

  downloadUploadedFile(id: number) {
    return this.http.get(`${this.baseUrl}/uploads/${id}/file`, {
      responseType: 'blob',
      observe: 'response'
    });
  }

  viewUploadedFile(id: number) {
    return this.http.get(`${this.baseUrl}/uploads/${id}/view`, {
      responseType: 'blob',
      observe: 'response'
    });
  }

  getUploadHistory(id: number): Observable<{ success: boolean; history: BankFormUploadHistoryRecord[] }> {
    return this.http.get<any>(`${this.baseUrl}/uploads/${id}/history`);
  }

  listUploads(accountNumber?: string, formCode?: string): Observable<{ success: boolean; uploads: BankFormUploadRecord[]; count: number }> {
    let params = new HttpParams();
    if (accountNumber) params = params.set('accountNumber', accountNumber);
    if (formCode) params = params.set('formCode', formCode);
    return this.http.get<any>(`${this.baseUrl}/uploads`, { params });
  }

  deleteUpload(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/uploads/${id}`);
  }

  clearAllUploads(): Observable<any> {
    return this.http.delete(`${this.baseUrl}/uploads`);
  }
}
