import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Admin as AdminModel, CreateAdminRequest, UpdateAdminRequest, AdminLoginRequest, AdminLoginResponse, AdminSearchRequest, AdminStatistics } from '../model/admin/admin-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiBaseUrl}/api/admins`;

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createAdmin(admin: CreateAdminRequest): Observable<AdminModel> {
    return this.http.post<AdminModel>(`${this.apiUrl}`, admin);
  }

  getAdminById(id: number): Observable<AdminModel> {
    return this.http.get<AdminModel>(`${this.apiUrl}/${id}`);
  }

  updateAdmin(id: number, adminDetails: UpdateAdminRequest): Observable<AdminModel> {
    return this.http.put<AdminModel>(`${this.apiUrl}/${id}`, adminDetails);
  }

  deleteAdmin(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Authentication operations
  loginAdmin(loginRequest: AdminLoginRequest): Observable<AdminLoginResponse> {
    return this.http.post<AdminLoginResponse>(`${this.apiUrl}/login`, loginRequest);
  }

  logoutAdmin(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/logout`, null);
  }

  // Role-based operations
  getAdminsByRole(role: string): Observable<AdminModel[]> {
    return this.http.get<AdminModel[]>(`${this.apiUrl}/role/${role}`);
  }

  // Search operations
  searchAdmins(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Statistics operations
  getAdminStatistics(): Observable<AdminStatistics> {
    return this.http.get<AdminStatistics>(`${this.apiUrl}/stats`);
  }

  // Advanced search with multiple filters
  searchAdminsAdvanced(searchRequest: AdminSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.role) params = params.set('role', searchRequest.role);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Admin management operations
  changeAdminPassword(adminId: number, currentPassword: string, newPassword: string): Observable<any> {
    const body = { currentPassword, newPassword };
    return this.http.put<any>(`${this.apiUrl}/${adminId}/change-password`, body);
  }

  resetAdminPassword(adminId: number, newPassword: string): Observable<any> {
    const body = { newPassword };
    return this.http.put<any>(`${this.apiUrl}/${adminId}/reset-password`, body);
  }

  // Export admins
  exportAdmins(role?: string): Observable<Blob> {
    let params = new HttpParams();
    if (role) params = params.set('role', role);
    
    return this.http.get(`${this.apiUrl}/export`, { 
      params, 
      responseType: 'blob' 
    });
  }
}
