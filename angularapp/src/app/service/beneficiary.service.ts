import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class BeneficiaryService {
  private apiUrl = `${environment.apiBaseUrl}/api/beneficiaries`;

  constructor(private http: HttpClient) {}

  getBeneficiariesByAccount(accountNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/account/${accountNumber}`);
  }

  getBeneficiaryById(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}`);
  }

  addBeneficiary(beneficiary: any): Observable<any> {
    return this.http.post(this.apiUrl, beneficiary);
  }

  updateBeneficiary(id: number, beneficiary: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, beneficiary);
  }

  deleteBeneficiary(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
