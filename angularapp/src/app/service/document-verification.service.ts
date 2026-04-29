import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class DocumentVerificationService {
  private apiUrl = `${environment.apiBaseUrl}/api/document-verification`;

  constructor(private http: HttpClient) {}

  submit(doc: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, doc);
  }

  getByAccountNumber(accountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/account/${accountNumber}`);
  }

  getPending(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/pending`);
  }

  getByStatus(status: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/status/${status}`);
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  verify(id: number, verifiedBy: string, remarks: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/verify`, { verifiedBy, remarks });
  }

  reject(id: number, verifiedBy: string, rejectionReason: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/reject`, { verifiedBy, rejectionReason });
  }

  getStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats`);
  }

  getAll(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/all`);
  }
}
