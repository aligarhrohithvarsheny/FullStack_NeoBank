import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  EducationLoanSubsidyClaim, 
  CreateSubsidyClaimRequest,
  ApproveSubsidyClaimRequest,
  RejectSubsidyClaimRequest,
  CreditSubsidyRequest,
  UpdateSubsidyAmountRequest
} from '../model/subsidy-claim/subsidy-claim-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class SubsidyClaimService {
  private apiUrl = `${environment.apiBaseUrl}/api/education-loan-subsidy-claims`;

  constructor(private http: HttpClient) { }

  // Create a new subsidy claim
  createClaim(request: CreateSubsidyClaimRequest): Observable<EducationLoanSubsidyClaim> {
    return this.http.post<EducationLoanSubsidyClaim>(`${this.apiUrl}/create`, request);
  }

  // Get all claims
  getAllClaims(): Observable<EducationLoanSubsidyClaim[]> {
    return this.http.get<EducationLoanSubsidyClaim[]>(`${this.apiUrl}`);
  }

  // Get claim by ID
  getClaimById(id: number): Observable<EducationLoanSubsidyClaim> {
    return this.http.get<EducationLoanSubsidyClaim>(`${this.apiUrl}/${id}`);
  }

  // Get claims by account number
  getClaimsByAccountNumber(accountNumber: string): Observable<EducationLoanSubsidyClaim[]> {
    return this.http.get<EducationLoanSubsidyClaim[]>(`${this.apiUrl}/account/${accountNumber}`);
  }

  // Get pending claims
  getPendingClaims(): Observable<EducationLoanSubsidyClaim[]> {
    return this.http.get<EducationLoanSubsidyClaim[]>(`${this.apiUrl}/pending`);
  }

  // Approve claim
  approveClaim(claimId: number, approvedAmount: number | null, adminName: string, adminNotes?: string): Observable<EducationLoanSubsidyClaim> {
    let params = new HttpParams()
      .set('adminName', adminName);
    
    if (approvedAmount !== null) {
      params = params.set('approvedAmount', approvedAmount.toString());
    }
    
    if (adminNotes) {
      params = params.set('adminNotes', adminNotes);
    }
    
    return this.http.put<EducationLoanSubsidyClaim>(`${this.apiUrl}/${claimId}/approve`, {}, { params });
  }

  // Reject claim
  rejectClaim(claimId: number, adminName: string, rejectionReason?: string): Observable<EducationLoanSubsidyClaim> {
    let params = new HttpParams()
      .set('adminName', adminName);
    
    if (rejectionReason) {
      params = params.set('rejectionReason', rejectionReason);
    }
    
    return this.http.put<EducationLoanSubsidyClaim>(`${this.apiUrl}/${claimId}/reject`, {}, { params });
  }

  // Credit subsidy to account
  creditSubsidy(claimId: number, adminName: string): Observable<any> {
    const params = new HttpParams().set('adminName', adminName);
    return this.http.put<any>(`${this.apiUrl}/${claimId}/credit`, {}, { params });
  }

  // Update approved amount
  updateApprovedAmount(claimId: number, newAmount: number, adminName: string): Observable<EducationLoanSubsidyClaim> {
    const params = new HttpParams()
      .set('newAmount', newAmount.toString())
      .set('adminName', adminName);
    
    return this.http.put<EducationLoanSubsidyClaim>(`${this.apiUrl}/${claimId}/update-amount`, {}, { params });
  }
}





