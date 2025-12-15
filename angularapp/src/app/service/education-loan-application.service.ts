import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EducationLoanApplication } from '../model/education-loan-application/education-loan-application-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class EducationLoanApplicationService {
  private apiUrl = `${environment.apiUrl}/education-loan-applications`;

  constructor(private http: HttpClient) { }

  // Create new application
  createApplication(application: EducationLoanApplication): Observable<EducationLoanApplication> {
    return this.http.post<EducationLoanApplication>(`${this.apiUrl}/create`, application);
  }

  // Get all applications
  getAllApplications(): Observable<EducationLoanApplication[]> {
    return this.http.get<EducationLoanApplication[]>(`${this.apiUrl}`);
  }

  // Get application by ID
  getApplicationById(id: number): Observable<EducationLoanApplication> {
    return this.http.get<EducationLoanApplication>(`${this.apiUrl}/${id}`);
  }

  // Get applications by account number
  getApplicationsByAccountNumber(accountNumber: string): Observable<EducationLoanApplication[]> {
    return this.http.get<EducationLoanApplication[]>(`${this.apiUrl}/account/${accountNumber}`);
  }

  // Get application by loan ID
  getApplicationByLoanId(loanId: number): Observable<EducationLoanApplication> {
    return this.http.get<EducationLoanApplication>(`${this.apiUrl}/loan/${loanId}`);
  }

  // Get pending applications
  getPendingApplications(): Observable<EducationLoanApplication[]> {
    return this.http.get<EducationLoanApplication[]>(`${this.apiUrl}/pending`);
  }

  // Update application
  updateApplication(id: number, application: EducationLoanApplication): Observable<EducationLoanApplication> {
    return this.http.put<EducationLoanApplication>(`${this.apiUrl}/${id}`, application);
  }

  // Update status
  updateStatus(id: number, status: string, reviewedBy: string, notes?: string): Observable<EducationLoanApplication> {
    let params = new HttpParams()
      .set('status', status)
      .set('reviewedBy', reviewedBy);
    
    if (notes) {
      params = params.set('notes', notes);
    }
    
    return this.http.put<EducationLoanApplication>(`${this.apiUrl}/${id}/status`, {}, { params });
  }

  // Reject application
  rejectApplication(id: number, reviewedBy: string, rejectionReason: string): Observable<EducationLoanApplication> {
    const params = new HttpParams()
      .set('reviewedBy', reviewedBy)
      .set('rejectionReason', rejectionReason);
    
    return this.http.put<EducationLoanApplication>(`${this.apiUrl}/${id}/reject`, {}, { params });
  }

  // Upload college application document
  uploadCollegeApplication(id: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.apiUrl}/${id}/upload-college-application`, formData);
  }

  // Download file
  downloadFile(id: number, fileType: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${id}/${fileType}`, { responseType: 'blob' });
  }
}





