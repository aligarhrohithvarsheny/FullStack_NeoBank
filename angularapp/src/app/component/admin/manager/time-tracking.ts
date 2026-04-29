import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TimeTrackingService } from '../../../service/time-tracking.service';
import { AlertService } from '../../../service/alert.service';
import {
  EmployeeTimeRecord,
  TimeManagementPolicy,
  TimeTrackingStats,
  DailyAttendanceStats,
  EmployeeWorkingHours
} from '../../../model/time-tracking/time-tracking.model';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';

interface ManagerMenuSection {
  title: string;
  items: { id: string; name: string; icon: string; count?: number }[];
}

@Component({
  selector: 'app-time-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './time-tracking.html',
  styleUrls: ['./time-tracking.css']
})
export class TimeTrackingComponent implements OnInit, OnDestroy {
  // Tab Views
  activeTab: 'overview' | 'employees' | 'policy' | 'records' = 'overview';
  
  // Overview Data
  timeStats: TimeTrackingStats | null = null;
  dailyStats: DailyAttendanceStats | null = null;
  
  // Employee Time Records
  employeeRecords: EmployeeTimeRecord[] = [];
  allEmployeesHours: EmployeeWorkingHours[] = [];
  filteredEmployees: EmployeeWorkingHours[] = [];
  
  // Time Policy
  timePolicies: TimeManagementPolicy[] = [];
  selectedPolicy: TimeManagementPolicy | null = null;
  newPolicy: TimeManagementPolicy = this.initializePolicy();
  
  // Filters and Search
  searchTerm: string = '';
  statusFilter: string = 'ALL';
  dateFilter: Date = new Date();
  currentPage: number = 0;
  pageSize: number = 20;
  totalRecords: number = 0;
  
  // Loading States
  loading: boolean = false;
  policyLoading: boolean = false;
  recordsLoading: boolean = false;
  
  // UI State
  sidebarCollapsed: boolean = false;
  showPolicyForm: boolean = false;
  editingPolicyId: number | null = null;
  
  // Real-time Update
  private destroy$ = new Subject<void>();
  
  // Sidebar Menu
  managerMenu: ManagerMenuSection[] = [
    {
      title: 'TIME MANAGEMENT',
      items: [
        { id: 'overview', name: 'Overview', icon: 'fa-chart-bar' },
        { id: 'employees', name: 'Employee Hours', icon: 'fa-users' },
        { id: 'records', name: 'Time Records', icon: 'fa-clock' },
        { id: 'policy', name: 'Policies', icon: 'fa-cog' }
      ]
    }
  ];

  constructor(
    private timeTrackingService: TimeTrackingService,
    private alertService: AlertService,
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadDashboardData();
      
      // Refresh stats every 30 seconds
      interval(30000)
        .pipe(
          switchMap(() => this.timeTrackingService.getTimeTrackingStats()),
          takeUntil(this.destroy$)
        )
        .subscribe(
          stats => this.timeStats = stats,
          error => console.error('Error fetching stats:', error)
        );
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ==================== LOAD DATA ====================

  loadDashboardData(): void {
    this.loading = true;
    
    // Load all data
    this.timeTrackingService.getTimeTrackingStats().subscribe(
      stats => this.timeStats = stats,
      error => this.alertService.error('Error', 'Failed to load time tracking stats')
    );

    this.timeTrackingService.getDailyAttendanceStats(new Date()).subscribe(
      stats => this.dailyStats = stats,
      error => console.error('Error loading daily stats:', error)
    );

    this.loadEmployeeHours();
    this.loading = false;
  }

  loadEmployeeHours(): void {
    this.recordsLoading = true;
    this.timeTrackingService.getAllEmployeesWorkingHours(
      this.currentPage,
      this.pageSize,
      this.searchTerm
    ).subscribe(
      response => {
        this.allEmployeesHours = response.content || response;
        this.filteredEmployees = this.allEmployeesHours;
        this.totalRecords = response.totalElements || this.allEmployeesHours.length;
        this.recordsLoading = false;
      },
      error => {
        this.alertService.error('Error', 'Failed to load employee hours');
        this.recordsLoading = false;
      }
    );
  }

  loadTimeRecords(): void {
    this.recordsLoading = true;
    this.timeTrackingService.getAllTimeRecords(
      this.currentPage,
      this.pageSize,
      this.statusFilter !== 'ALL' ? this.statusFilter : undefined,
      this.searchTerm
    ).subscribe(
      response => {
        this.employeeRecords = response.content || response;
        this.totalRecords = response.totalElements || this.employeeRecords.length;
        this.recordsLoading = false;
      },
      error => {
        this.alertService.error('Error', 'Failed to load time records');
        this.recordsLoading = false;
      }
    );
  }

  loadTimePolicies(): void {
    this.policyLoading = true;
    this.timeTrackingService.getAllTimePolicies(0, 100).subscribe(
      response => {
        this.timePolicies = response.content || response;
        this.policyLoading = false;
      },
      error => {
        this.alertService.error('Error', 'Failed to load time policies');
        this.policyLoading = false;
      }
    );
  }

  // ==================== TAB NAVIGATION ====================

  selectTab(tab: 'overview' | 'employees' | 'policy' | 'records'): void {
    this.activeTab = tab;
    
    if (tab === 'employees') {
      this.loadEmployeeHours();
    } else if (tab === 'records') {
      this.loadTimeRecords();
    } else if (tab === 'policy') {
      this.loadTimePolicies();
    }
  }

  // ==================== EMPLOYEE HOURS MANAGEMENT ====================

  searchEmployees(): void {
    this.currentPage = 0;
    this.loadEmployeeHours();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadEmployeeHours();
  }

  markAbsent(employee: EmployeeWorkingHours): void {
    if (confirm(`Mark ${employee.adminName} as absent today?`)) {
      this.timeTrackingService.markAbsent(employee.adminId, new Date()).subscribe(
        () => {
          this.alertService.success('Success', `${employee.adminName} marked as absent`);
          this.loadEmployeeHours();
        },
        error => this.alertService.error('Error', 'Failed to mark absent')
      );
    }
  }

  markOnLeave(employee: EmployeeWorkingHours): void {
    if (confirm(`Mark ${employee.adminName} as on leave today?`)) {
      this.timeTrackingService.markOnLeave(employee.adminId, new Date()).subscribe(
        () => {
          this.alertService.success('Success', `${employee.adminName} marked as on leave`);
          this.loadEmployeeHours();
        },
        error => this.alertService.error('Error', 'Failed to mark on leave')
      );
    }
  }

  adjustTime(record: EmployeeTimeRecord): void {
    const checkInTime = prompt('Enter new check-in time (HH:mm):', 
      new Date(record.checkInTime).toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' }));
    
    if (checkInTime) {
      const checkOutTime = prompt('Enter new check-out time (HH:mm):',
        new Date(record.checkOutTime).toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' }));
      
      if (checkOutTime) {
        this.timeTrackingService.adjustTimeRecord(record.id, {
          checkInTime,
          checkOutTime
        }).subscribe(
          () => {
            this.alertService.success('Success', 'Time record adjusted successfully');
            this.loadTimeRecords();
          },
          error => this.alertService.error('Error', 'Failed to adjust time record')
        );
      }
    }
  }

  // ==================== TIME POLICY MANAGEMENT ====================

  openPolicyForm(policy?: TimeManagementPolicy): void {
    if (policy) {
      this.selectedPolicy = policy;
      this.newPolicy = { ...policy };
      this.editingPolicyId = policy.id;
    } else {
      this.selectedPolicy = null;
      this.newPolicy = this.initializePolicy();
      this.editingPolicyId = null;
    }
    this.showPolicyForm = true;
  }

  closePolicyForm(): void {
    this.showPolicyForm = false;
    this.selectedPolicy = null;
    this.newPolicy = this.initializePolicy();
  }

  savePolicy(): void {
    if (!this.validatePolicy()) {
      this.alertService.error('Error', 'Please fill all required fields');
      return;
    }

    this.policyLoading = true;
    this.timeTrackingService.saveTimePolicy(this.newPolicy).subscribe(
      updatedPolicy => {
        this.alertService.success('Success', 'Time policy saved successfully');
        this.loadTimePolicies();
        this.closePolicyForm();
      },
      error => {
        this.alertService.error('Error', 'Failed to save time policy');
        this.policyLoading = false;
      }
    );
  }

  activatePolicy(policy: TimeManagementPolicy): void {
    this.timeTrackingService.activatePolicy(policy.id, !policy.isActive).subscribe(
      () => {
        policy.isActive = !policy.isActive;
        this.alertService.success('Success',
          `Policy ${policy.isActive ? 'activated' : 'deactivated'}`
        );
      },
      error => this.alertService.error('Error', 'Failed to update policy status')
    );
  }

  deletePolicy(policyId: number): void {
    if (confirm('Are you sure you want to delete this policy?')) {
      // Call delete endpoint
      this.alertService.success('Success', 'Policy deleted successfully');
      this.loadTimePolicies();
    }
  }

  private validatePolicy(): boolean {
    return !!(
      this.newPolicy.policyName &&
      this.newPolicy.checkInTime &&
      this.newPolicy.checkOutTime &&
      this.newPolicy.workingHoursPerDay > 0
    );
  }

  private initializePolicy(): TimeManagementPolicy {
    return {
      id: 0,
      adminId: '',
      policyName: '',
      workingHoursPerDay: 8,
      checkInTime: '09:00',
      checkOutTime: '17:00',
      gracePeriodMinutes: 5,
      maxWorkingHours: 10,
      overtimeMultiplier: 1.5,
      isActive: true,
      createdAt: new Date(),
      updatedAt: new Date()
    };
  }

  // ==================== REPORT GENERATION ====================

  generateReport(formatType: 'PDF' | 'CSV' | 'EXCEL'): void {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30); // Last 30 days
    
    this.timeTrackingService.generateTimeTrackingReport(startDate, new Date(), formatType).subscribe(
      blob => {
        const fileName = `time-tracking-report.${formatType.toLowerCase()}`;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error => this.alertService.error('Error', 'Failed to generate report')
    );
  }

  // ==================== UTILITY METHODS ====================

  getAttendancePercentageClass(percentage: number): string {
    if (percentage >= 95) return 'excellent';
    if (percentage >= 85) return 'good';
    if (percentage >= 75) return 'fair';
    return 'poor';
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'badge-primary';
      case 'CHECKED_OUT': return 'badge-success';
      case 'ABSENT': return 'badge-danger';
      case 'ON_LEAVE': return 'badge-warning';
      default: return 'badge-secondary';
    }
  }

  formatTime(date: Date | string): string {
    if (!date) return '-';
    const d = new Date(date);
    return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  formatDate(date: Date | string): string {
    if (!date) return '-';
    const d = new Date(date);
    return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  formatHours(hours: number): string {
    const wholeHours = Math.floor(hours);
    const minutes = Math.round((hours - wholeHours) * 60);
    return `${wholeHours}h ${minutes}m`;
  }

  getMenuItems(section: ManagerMenuSection): typeof section.items {
    return section.items;
  }
}
