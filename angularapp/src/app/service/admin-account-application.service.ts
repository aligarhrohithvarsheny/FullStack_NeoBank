import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';
import { AdminAccountApplication, AdminAccountAppStats } from '../model/admin-account-application/admin-account-application.model';

@Injectable({
  providedIn: 'root'
})
export class AdminAccountApplicationService {
  private apiUrl = `${environment.apiBaseUrl}/api/admin-account-applications`;

  constructor(private http: HttpClient) {}

  // CRUD
  createApplication(application: AdminAccountApplication): Observable<any> {
    return this.http.post(`${this.apiUrl}/create`, application);
  }

  getAllApplications(): Observable<AdminAccountApplication[]> {
    return this.http.get<AdminAccountApplication[]>(`${this.apiUrl}/all`);
  }

  getById(id: number): Observable<AdminAccountApplication> {
    return this.http.get<AdminAccountApplication>(`${this.apiUrl}/${id}`);
  }

  getByApplicationNumber(appNumber: string): Observable<AdminAccountApplication> {
    return this.http.get<AdminAccountApplication>(`${this.apiUrl}/number/${appNumber}`);
  }

  getByStatus(status: string): Observable<AdminAccountApplication[]> {
    return this.http.get<AdminAccountApplication[]>(`${this.apiUrl}/status/${status}`);
  }

  getByAccountType(accountType: string): Observable<AdminAccountApplication[]> {
    return this.http.get<AdminAccountApplication[]>(`${this.apiUrl}/type/${accountType}`);
  }

  getPendingApplications(): Observable<AdminAccountApplication[]> {
    return this.http.get<AdminAccountApplication[]>(`${this.apiUrl}/pending`);
  }

  getAwaitingManagerApproval(): Observable<AdminAccountApplication[]> {
    return this.http.get<AdminAccountApplication[]>(`${this.apiUrl}/awaiting-manager`);
  }

  searchApplications(term: string): Observable<AdminAccountApplication[]> {
    return this.http.get<AdminAccountApplication[]>(`${this.apiUrl}/search`, { params: { term } });
  }

  getStatistics(): Observable<AdminAccountAppStats> {
    return this.http.get<AdminAccountAppStats>(`${this.apiUrl}/statistics`);
  }

  // Validation
  validateAadhar(aadhar: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/validate/aadhar/${aadhar}`);
  }

  validatePan(pan: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/validate/pan/${pan}`);
  }

  validatePhone(phone: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/validate/phone/${phone}`);
  }

  // Existing Account Check
  checkExistingAccounts(aadhar: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/check-existing-accounts/${aadhar}`);
  }

  // Admin Verification
  adminVerify(id: number, adminName: string, remarks: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/admin-verify/${id}`, { adminName, remarks });
  }

  // Manager Approval
  managerApprove(id: number, managerName: string, remarks: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/manager-approve/${id}`, { managerName, remarks });
  }

  managerReject(id: number, managerName: string, remarks: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/manager-reject/${id}`, { managerName, remarks });
  }

  // Update Application Details
  updateApplication(id: number, application: AdminAccountApplication): Observable<any> {
    return this.http.put(`${this.apiUrl}/update/${id}`, application);
  }

  // Document Upload
  uploadSignedApplication(id: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/upload-signed/${id}`, formData);
  }

  uploadAdditionalDocuments(id: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/upload-documents/${id}`, formData);
  }

  // PDF Download
  downloadApplication(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download-application/${id}`, {
      responseType: 'blob'
    });
  }

  // Signed Document Lookup (for signature verification)
  getSignedDocumentInfo(accountNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/signed-document-info/${accountNumber}`);
  }

  viewSignedDocument(accountNumber: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/view-signed-document/${accountNumber}`, {
      responseType: 'blob'
    });
  }

  viewSignedDocumentById(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/view-signed-by-id/${id}`, {
      responseType: 'blob'
    });
  }

  viewAdditionalDocsById(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/view-additional-docs-by-id/${id}`, {
      responseType: 'blob'
    });
  }

  // ==================== Preloaded Customer Data ====================

  private preloadedUrl = `${environment.apiBaseUrl}/api/preloaded-customer-data`;

  lookupByAadhar(aadhar: string): Observable<any> {
    return this.http.get(`${this.preloadedUrl}/lookup/aadhar/${aadhar}`);
  }

  lookupByPan(pan: string): Observable<any> {
    return this.http.get(`${this.preloadedUrl}/lookup/pan/${pan}`);
  }

  uploadCustomerExcel(file: File, uploadedBy: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('uploadedBy', uploadedBy);
    return this.http.post(`${this.preloadedUrl}/upload-excel`, formData);
  }

  getPreloadedAll(): Observable<any[]> {
    return this.http.get<any[]>(`${this.preloadedUrl}/all`);
  }

  getPreloadedUnused(): Observable<any[]> {
    return this.http.get<any[]>(`${this.preloadedUrl}/unused`);
  }

  getPreloadedStatistics(): Observable<any> {
    return this.http.get(`${this.preloadedUrl}/statistics`);
  }

  deletePreloaded(id: number): Observable<any> {
    return this.http.delete(`${this.preloadedUrl}/${id}`);
  }

  deletePreloadedBatch(batchId: string): Observable<any> {
    return this.http.delete(`${this.preloadedUrl}/batch/${batchId}`);
  }

  downloadCustomerTemplate(): Observable<Blob> {
    return this.http.get(`${this.preloadedUrl}/download-template`, {
      responseType: 'blob'
    });
  }
}
