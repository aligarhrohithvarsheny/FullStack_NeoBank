export interface AdminDashboardStats {
  totalUsers: number;
  totalAccounts: number;
  totalLoans: number;
  totalTransactions: number;
  totalRevenue: number;
  activeSessions: number;
  totalSalaryAccounts: number;
  pendingApprovals: number;
  fraudAlerts: number;
  systemHealth: number;
  lastUpdate?: string;
}

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: string;
  status: string;
  createdAt: string;
  lastLogin?: string;
  permissions: string[];
}

export interface SalaryAccountForAdmin {
  id: number;
  accountNumber: string;
  employeeName: string;
  employeeId: string;
  companyName: string;
  salary: number;
  status: string;
  createdAt: string;
  lastSalaryCreditDate?: string;
  balance: number;
  accountType: string;
  frozen?: boolean;
  verified?: boolean;
}

export interface UserForAdmin {
  id: number;
  username: string;
  email: string;
  accountNumber?: string;
  phoneNumber?: string;
  status: string;
  accountType: string;
  balance: number;
  createdAt: string;
  kycStatus: string;
  blocked?: boolean;
}

export interface TransactionForAdmin {
  id: number;
  transactionId: string;
  accountNumber: string;
  userName: string;
  type: string;
  amount: number;
  description: string;
  date: string;
  status: string;
  balance: number;
  reversible?: boolean;
}

export interface LoanForAdmin {
  id: number;
  loanNumber: string;
  accountNumber: string;
  userName: string;
  loanType: string;
  amount: number;
  interestRate: number;
  tenure: number;
  emi: number;
  status: string;
  appliedDate: string;
  approvedDate?: string;
  disbursementAmount: number;
}

export interface AdminAuditLog {
  id: number;
  adminName: string;
  action: string;
  details: string;
  timestamp: string;
  ipAddress?: string;
  status: string;
}

export interface FraudAlert {
  id: number;
  accountNumber: string;
  userName: string;
  alertType: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  description: string;
  timestamp: string;
  status: 'PENDING' | 'RESOLVED' | 'DISMISSED';
  detectionMethod: string;
}

export interface AdminLoginActivity {
  id: number;
  adminName: string;
  loginTime: string;
  logoutTime?: string;
  ipAddress: string;
  userAgent: string;
  status: string;
}
