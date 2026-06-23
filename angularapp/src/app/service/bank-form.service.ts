import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

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

export interface BankFormDownloadOptions {
  adminName: string;
  accountNumber?: string;
  accountType?: string;
  holderName?: string;
}

@Injectable({ providedIn: 'root' })
export class BankFormService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/admin/bank-forms`;

  constructor(private http: HttpClient) {}

  getCatalog(): Observable<{ success: boolean; forms: BankFormDefinition[]; categories: string[]; total: number }> {
    return this.http.get<any>(`${this.baseUrl}/catalog`);
  }

  verifyAccount(accountNumber: string, accountType: string): Observable<any> {
    const params = new HttpParams()
      .set('accountNumber', accountNumber)
      .set('accountType', accountType);
    return this.http.get(`${this.baseUrl}/verify-account`, { params });
  }

  downloadBlankPdf(formCode: string, options: BankFormDownloadOptions) {
    let params = new HttpParams().set('adminName', options.adminName || 'Admin');
    if (options.accountNumber) {
      params = params.set('accountNumber', options.accountNumber);
    }
    if (options.accountType) {
      params = params.set('accountType', options.accountType);
    }
    if (options.holderName) {
      params = params.set('holderName', options.holderName);
    }
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

  downloadUploadedFile(id: number) {
    return this.http.get(`${this.baseUrl}/uploads/${id}/file`, {
      responseType: 'blob',
      observe: 'response'
    });
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
