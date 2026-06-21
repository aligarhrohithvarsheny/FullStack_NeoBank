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

  downloadBlankPdf(formCode: string, adminName: string): Observable<Blob> {
    const params = new HttpParams().set('adminName', adminName);
    return this.http.get(`${this.baseUrl}/download/${formCode}`, {
      params,
      responseType: 'blob'
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

  listUploads(accountNumber?: string, formCode?: string): Observable<{ success: boolean; uploads: BankFormUploadRecord[]; count: number }> {
    let params = new HttpParams();
    if (accountNumber) params = params.set('accountNumber', accountNumber);
    if (formCode) params = params.set('formCode', formCode);
    return this.http.get<any>(`${this.baseUrl}/uploads`, { params });
  }
}
