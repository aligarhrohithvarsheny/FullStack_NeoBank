// Time Tracking Models for Employee Management

export interface EmployeeTimeRecord {
  id: number;
  adminId: string;
  adminName: string;
  adminEmail: string;
  idCardNumber: string;
  checkInTime: Date;
  checkOutTime: Date;
  totalWorkingHours: number;
  date: Date;
  status: 'ACTIVE' | 'CHECKED_OUT' | 'ABSENT' | 'ON_LEAVE';
  remarks?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface TimeManagementPolicy {
  id: number;
  adminId: string;
  policyName: string;
  workingHoursPerDay: number;
  checkInTime: string; // HH:mm format
  checkOutTime: string; // HH:mm format
  gracePeriodMinutes: number; // Grace period in minutes
  maxWorkingHours: number;
  overtimeMultiplier: number;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface DailyAttendanceStats {
  date: Date;
  totalEmployees: number;
  presentCount: number;
  absentCount: number;
  onLeaveCount: number;
  averageWorkingHours: number;
  overdueCheckouts: number;
}

export interface EmployeeWorkingHours {
  adminId: string;
  adminName: string;
  email: string;
  idCardNumber: string;
  hoursThisWeek: number;
  hoursThisMonth: number;
  averageDailyHours: number;
  overtimeHours: number;
  attendancePercentage: number;
}

export interface CheckInCheckOutRequest {
  adminId: string;
  idCardNumber: string;
  timestamp: Date;
  type: 'CHECK_IN' | 'CHECK_OUT';
  deviceId?: string;
  location?: string;
}

export interface TimeTrackingStats {
  totalEmployees: number;
  checkedInToday: number;
  checkedOutToday: number;
  onLeaveToday: number;
  averageWorkingHours: number;
  overdueCheckouts: number;
}
