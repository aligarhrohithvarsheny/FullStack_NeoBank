import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User as UserModel, CreateUserRequest, UpdateUserRequest, UserSearchRequest, UserStatistics } from '../model/user/user-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createUser(user: CreateUserRequest): Observable<UserModel> {
    return this.http.post<UserModel>(`${this.apiUrl}/create`, user);
  }

  getUserById(id: number): Observable<UserModel> {
    return this.http.get<UserModel>(`${this.apiUrl}/${id}`);
  }

  getUserByUsername(username: string): Observable<UserModel> {
    return this.http.get<UserModel>(`${this.apiUrl}/username/${username}`);
  }

  getUserByAccountNumber(accountNumber: string): Observable<UserModel> {
    return this.http.get<UserModel>(`${this.apiUrl}/account/${accountNumber}`);
  }

  getUserByEmail(email: string): Observable<UserModel> {
    return this.http.get<UserModel>(`${this.apiUrl}/email/${email}`);
  }

  updateUser(id: number, userDetails: UpdateUserRequest): Observable<UserModel> {
    return this.http.put<UserModel>(`${this.apiUrl}/update/${id}`, userDetails);
  }

  updateEmail(id: number, email: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/update-email/${id}`, { email });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Pagination and sorting
  getAllUsers(page: number = 0, size: number = 10, sortBy: string = 'joinDate', sortDir: string = 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    return this.http.get<any>(`${this.apiUrl}/all`, { params });
  }

  // Status-based operations
  getUsersByStatus(status: string): Observable<UserModel[]> {
    return this.http.get<UserModel[]>(`${this.apiUrl}/status/${status}`);
  }

  getUsersByStatusWithPagination(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}/paginated`, { params });
  }

  approveUser(id: number, adminName: string): Observable<UserModel> {
    const params = new HttpParams().set('adminName', adminName);
    return this.http.put<UserModel>(`${this.apiUrl}/approve/${id}`, null, { params });
  }

  closeUserAccount(id: number, adminName: string): Observable<UserModel> {
    const params = new HttpParams().set('adminName', adminName);
    return this.http.put<UserModel>(`${this.apiUrl}/close/${id}`, null, { params });
  }

  // Search operations
  searchUsers(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Filter operations
  getUsersByIncomeRange(minIncome: number, maxIncome: number): Observable<UserModel[]> {
    const params = new HttpParams()
      .set('minIncome', minIncome.toString())
      .set('maxIncome', maxIncome.toString());
    
    return this.http.get<UserModel[]>(`${this.apiUrl}/filter/income`, { params });
  }

  getUsersByOccupation(occupation: string): Observable<UserModel[]> {
    return this.http.get<UserModel[]>(`${this.apiUrl}/filter/occupation/${occupation}`);
  }

  getUsersByAccountType(accountType: string): Observable<UserModel[]> {
    return this.http.get<UserModel[]>(`${this.apiUrl}/filter/account-type/${accountType}`);
  }

  getUsersByJoinDateRange(startDate: string, endDate: string): Observable<UserModel[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get<UserModel[]>(`${this.apiUrl}/filter/join-date`, { params });
  }

  // Statistics operations
  getUserStatistics(): Observable<UserStatistics> {
    return this.http.get<UserStatistics>(`${this.apiUrl}/stats`);
  }

  getRecentUsers(limit: number = 5): Observable<UserModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<UserModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Validation operations
  validateEmail(email: string): Observable<{isUnique: boolean}> {
    return this.http.get<{isUnique: boolean}>(`${this.apiUrl}/validate/email/${email}`);
  }

  validatePan(pan: string): Observable<{isUnique: boolean}> {
    return this.http.get<{isUnique: boolean}>(`${this.apiUrl}/validate/pan/${pan}`);
  }

  validateAadhar(aadhar: string): Observable<{isUnique: boolean}> {
    return this.http.get<{isUnique: boolean}>(`${this.apiUrl}/validate/aadhar/${aadhar}`);
  }

  // Advanced search with multiple filters
  searchUsersAdvanced(searchRequest: UserSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.accountType) params = params.set('accountType', searchRequest.accountType);
    if (searchRequest.occupation) params = params.set('occupation', searchRequest.occupation);
    if (searchRequest.minIncome) params = params.set('minIncome', searchRequest.minIncome.toString());
    if (searchRequest.maxIncome) params = params.set('maxIncome', searchRequest.maxIncome.toString());
    if (searchRequest.joinDateFrom) params = params.set('joinDateFrom', searchRequest.joinDateFrom);
    if (searchRequest.joinDateTo) params = params.set('joinDateTo', searchRequest.joinDateTo);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }
}
