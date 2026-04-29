import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../environment/environment';
import {
  EmployeeTimeRecord,
  TimeManagementPolicy,
  DailyAttendanceStats,
  EmployeeWorkingHours,
  CheckInCheckOutRequest,
  TimeTrackingStats
} from '../model/time-tracking/time-tracking.model';

@Injectable({
  providedIn: 'root'
})
export class TimeTrackingService {
  private apiUrl = `${environment.apiBaseUrl}/api/time-tracking`;
  private timeTrackingStats$ = new BehaviorSubject<TimeTrackingStats | null>(null);

  constructor(private http: HttpClient) {}

  // ==================== EMPLOYEE TIME TRACKING ====================

  /**
   * Record employee check-in
   */
  employeeCheckIn(request: CheckInCheckOutRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/check-in`, request);
  }

  /**
   * Record employee check-out
   */
  employeeCheckOut(request: CheckInCheckOutRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/check-out`, request);
  }

  /**
   * Get employee today's record
   */
  getEmployeeTodayRecord(adminId: string): Observable<EmployeeTimeRecord> {
    return this.http.get<EmployeeTimeRecord>(
      `${this.apiUrl}/today-record/${adminId}`
    );
  }

  /**
   * Get employee time records for a date range
   */
  getEmployeeTimeRecords(
    adminId: string,
    startDate: Date,
    endDate: Date
  ): Observable<EmployeeTimeRecord[]> {
    let params = new HttpParams()
      .set('startDate', startDate.toISOString().split('T')[0])
      .set('endDate', endDate.toISOString().split('T')[0]);
    
    return this.http.get<EmployeeTimeRecord[]>(
      `${this.apiUrl}/records/${adminId}`,
      { params }
    );
  }

  // ==================== MANAGER DASHBOARD STATISTICS ====================

  /**
   * Get overall time tracking statistics
   */
  getTimeTrackingStats(): Observable<TimeTrackingStats> {
    return this.http.get<TimeTrackingStats>(`${this.apiUrl}/stats`);
  }

  /**
   * Get daily attendance statistics
   */
  getDailyAttendanceStats(date: Date): Observable<DailyAttendanceStats> {
    const dateStr = date.toISOString().split('T')[0];
    return this.http.get<DailyAttendanceStats>(
      `${this.apiUrl}/daily-attendance/${dateStr}`
    );
  }

  /**
   * Get all employees' working hours summary
   */
  getAllEmployeesWorkingHours(
    page: number = 0,
    size: number = 20,
    searchTerm?: string
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (searchTerm) {
      params = params.set('search', searchTerm);
    }

    return this.http.get(`${this.apiUrl}/employees-hours`, { params });
  }

  /**
   * Get specific employee's working hours
   */
  getEmployeeWorkingHours(adminId: string): Observable<EmployeeWorkingHours> {
    return this.http.get<EmployeeWorkingHours>(
      `${this.apiUrl}/employee-hours/${adminId}`
    );
  }

  // ==================== TIME MANAGEMENT POLICY ====================

  /**
   * Create or update time management policy
   */
  saveTimePolicy(policy: TimeManagementPolicy): Observable<TimeManagementPolicy> {
    if (policy.id) {
      return this.http.put<TimeManagementPolicy>(
        `${this.apiUrl}/policy/${policy.id}`,
        policy
      );
    }
    return this.http.post<TimeManagementPolicy>(
      `${this.apiUrl}/policy`,
      policy
    );
  }

  /**
   * Get admin's time management policy
   */
  getTimePolicy(adminId: string): Observable<TimeManagementPolicy> {
    return this.http.get<TimeManagementPolicy>(
      `${this.apiUrl}/policy/${adminId}`
    );
  }

  /**
   * Get all time management policies (for super admin)
   */
  getAllTimePolicies(page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get(`${this.apiUrl}/policies`, { params });
  }

  /**
   * Activate/Deactivate time policy
   */
  activatePolicy(policyId: number, isActive: boolean): Observable<any> {
    return this.http.patch(
      `${this.apiUrl}/policy/${policyId}/status`,
      { isActive }
    );
  }

  // ==================== TIME TRACKING MANAGEMENT ====================

  /**
   * Manually adjust employee time record (admin override)
   */
  adjustTimeRecord(recordId: number, adjustments: any): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/record/${recordId}/adjust`,
      adjustments
    );
  }

  /**
   * Mark employee as absent
   */
  markAbsent(adminId: string, date: Date, reason?: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/mark-absent`, {
      adminId,
      date,
      reason
    });
  }

  /**
   * Mark employee as on leave
   */
  markOnLeave(adminId: string, date: Date, reason?: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/mark-leave`, {
      adminId,
      date,
      reason
    });
  }

  /**
   * Get all time records for admin (manager dashboard)
   */
  getAllTimeRecords(
    page: number = 0,
    size: number = 20,
    status?: string,
    search?: string
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (status) {
      params = params.set('status', status);
    }
    if (search) {
      params = params.set('search', search);
    }

    return this.http.get(`${this.apiUrl}/all-records`, { params });
  }

  /**
   * Generate time tracking report
   */
  generateTimeTrackingReport(
    startDate: Date,
    endDate: Date,
    formatType: 'PDF' | 'CSV' | 'EXCEL' = 'PDF'
  ): Observable<Blob> {
    const params = new HttpParams()
      .set('startDate', startDate.toISOString().split('T')[0])
      .set('endDate', endDate.toISOString().split('T')[0])
      .set('format', formatType);

    return this.http.get(`${this.apiUrl}/report`, {
      params,
      responseType: 'blob'
    });
  }

  /**
   * Get real-time time tracking stats (with websocket integration)
   */
  getTimeTrackingStatsSubject(): BehaviorSubject<TimeTrackingStats | null> {
    return this.timeTrackingStats$;
  }

  /**
   * Update live stats
   */
  updateLiveStats(stats: TimeTrackingStats): void {
    this.timeTrackingStats$.next(stats);
  }
}
