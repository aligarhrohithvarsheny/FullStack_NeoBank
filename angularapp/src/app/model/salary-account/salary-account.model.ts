export interface SalaryAccount {
  id?: number;
  employeeName: string;
  dob?: string;
  mobileNumber?: string;
  email?: string;
  aadharNumber?: string;
  panNumber?: string;
  companyName?: string;
  companyId?: string;
  employerAddress?: string;
  hrContactNumber?: string;
  monthlySalary?: number;
  salaryCreditDate?: number;
  designation?: string;
  accountNumber?: string;
  customerId?: string;
  debitCardNumber?: string;
  debitCardCvv?: string;
  debitCardExpiry?: string;
  netBankingEnabled?: boolean;
  branchName?: string;
  ifscCode?: string;
  balance?: number;
  status?: string;
  transactionPinSet?: boolean;
  address?: string;
  upiId?: string;
  upiEnabled?: boolean;
  autoSavingsEnabled?: boolean;
  autoSavingsPercentage?: number;
  savingsBalance?: number;
  debitCardStatus?: string;
  dailyLimit?: number;
  internationalEnabled?: boolean;
  onlineEnabled?: boolean;
  contactlessEnabled?: boolean;
  employeeId?: string;
  employeeIdLinked?: boolean;
  failedLoginAttempts?: number;
  accountLocked?: boolean;
  lastFailedLoginTime?: string;
  lockReason?: string;
  closedAt?: string;
  closedReason?: string;
  closedBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SalaryTransaction {
  id?: number;
  salaryAccountId?: number;
  accountNumber?: string;
  salaryAmount?: number;
  creditDate?: string;
  companyName?: string;
  description?: string;
  type?: string;
  previousBalance?: number;
  newBalance?: number;
  status?: string;
  createdAt?: string;
}

export interface SalaryNormalTransaction {
  id?: number;
  salaryAccountId?: number;
  accountNumber?: string;
  type?: string;
  amount?: number;
  charge?: number;
  recipientAccount?: string;
  recipientIfsc?: string;
  remark?: string;
  previousBalance?: number;
  newBalance?: number;
  status?: string;
  createdAt?: string;
}

export interface SalaryLoginActivity {
  id?: number;
  salaryAccountId?: number;
  accountNumber?: string;
  activityType?: string;
  ipAddress?: string;
  deviceInfo?: string;
  status?: string;
  createdAt?: string;
}

export interface SalaryAccountStats {
  totalAccounts: number;
  activeAccounts: number;
  frozenAccounts: number;
  closedAccounts: number;
  totalMonthlySalary: number;
  companiesLinked: { [company: string]: number };
  totalCompanies: number;
}

export interface SalaryUpiTransaction {
  id?: number;
  salaryAccountId?: number;
  accountNumber?: string;
  upiId?: string;
  recipientUpi?: string;
  recipientName?: string;
  type?: string;
  amount?: number;
  remark?: string;
  status?: string;
  transactionRef?: string;
  createdAt?: string;
}

export interface SalaryAdvanceRequest {
  id?: number;
  salaryAccountId?: number;
  accountNumber?: string;
  employeeName?: string;
  monthlySalary?: number;
  advanceAmount?: number;
  advanceLimit?: number;
  reason?: string;
  status?: string;
  approvedAt?: string;
  repaid?: boolean;
  repaidAt?: string;
  createdAt?: string;
}

export interface SalaryFraudAlert {
  id?: number;
  salaryAccountId?: number;
  accountNumber?: string;
  alertType?: string;
  severity?: string;
  description?: string;
  amount?: number;
  location?: string;
  resolved?: boolean;
  resolvedAt?: string;
  createdAt?: string;
}
