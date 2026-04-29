// Admin Account Models - Following Salary Dashboard Pattern

export interface AdminUser {
  id?: number;
  email: string;
  username: string;
  password?: string;
  name?: string;
  role?: string;
  phoneNumber?: string;
  status?: string;
  profileComplete?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  accountNumber: string;
  customerId?: string;
  balance: number;
  status: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  address?: string;
  pan?: string;
  aadhar?: string;
  occupation?: string;
  income?: number;
  accountType?: string;
  joinDate?: string;
}

export interface AdminDashboardStats {
  totalUsers: number;
  activeUsers: number;
  blockedUsers: number;
  totalAccounts: number;
  activeAccounts: number;
  frozenAccounts: number;
  totalBalance: number;
  totalTransactions: number;
  pendingLoans: number;
  totalLoans: number;
  totalDeposits: number;
  systemHealth?: number;
  fraudAlerts?: number;
}

export interface TransactionRecord {
  id: string;
  transactionId: string;
  merchant?: string;
  amount: number;
  type: string;
  description: string;
  balance: number;
  date: string;
  status: string;
  userName: string;
  accountNumber: string;
}

export interface LoanDetails {
  id: string;
  loanType: string;
  amount: number;
  interestRate: number;
  tenure: number;
  emi: number;
  status: string;
  appliedDate: string;
  approvedDate?: string;
  userName?: string;
  accountNumber?: string;
}

export interface CardDetails {
  id: string;
  cardNumber: string;
  cardType: string;
  expiry: string;
  cvv: string;
  status: string;
  pinSet: boolean;
  blocked: boolean;
  deactivated: boolean;
  userName?: string;
  accountNumber?: string;
}

export interface AdminLoginActivity {
  id?: number;
  adminEmail?: string;
  activityType?: string;
  ipAddress?: string;
  deviceInfo?: string;
  status?: string;
  createdAt?: string;
}

export interface AdminAuditLog {
  id?: number;
  adminEmail?: string;
  action?: string;
  targetUser?: string;
  details?: string;
  status?: string;
  createdAt?: string;
}

export interface SupportTicket {
  id?: number;
  ticketId?: string;
  userId?: string;
  userName?: string;
  userEmail?: string;
  subject?: string;
  description?: string;
  status?: string;
  priority?: string;
  responses?: TicketResponse[];
  createdAt?: string;
  updatedAt?: string;
}

export interface TicketResponse {
  id?: number;
  responder?: string;
  response?: string;
  createdAt?: string;
}

export interface DepositRequest {
  id: number;
  requestId: string;
  accountNumber: string;
  userName: string;
  amount: number;
  method: string;
  referenceNumber?: string;
  note?: string;
  status: string;
  processedBy?: string;
  processedAt?: string;
  rejectionReason?: string;
  resultingBalance?: number;
  createdAt?: string;
}

export interface GoldLoan {
  id?: string;
  loanId?: string;
  accountNumber?: string;
  userName?: string;
  goldGrams?: number;
  goldRate?: number;
  loanAmount?: number;
  interestRate?: number;
  tenure?: number;
  emi?: number;
  status?: string;
  appliedDate?: string;
  approvedDate?: string;
  collateralStorage?: string;
}

export interface AccountTracking {
  id: number;
  trackingId: string;
  aadharNumber: string;
  mobileNumber: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  statusChangedAt: string;
  updatedBy?: string;
  user?: {
    id: number;
    email: string;
    username: string;
    accountNumber?: string;
  };
}

export interface ProfileUpdateRequest {
  id?: number;
  userId?: string;
  userName?: string;
  updateType?: string;
  oldValue?: any;
  newValue?: any;
  status?: string;
  requestedAt?: string;
  approvedAt?: string;
  processedBy?: string;
}

export interface DailyActivityReport {
  date?: string;
  totalActivities?: number;
  userUpdates?: number;
  deposits?: number;
  withdrawals?: number;
  loans?: number;
  goldLoans?: number;
  cheques?: number;
  accounts?: number;
  subsidy?: number;
}
