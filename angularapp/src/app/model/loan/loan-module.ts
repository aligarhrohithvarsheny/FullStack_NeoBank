export interface Loan {
  id?: number;
  type: string;
  amount: number;
  tenure: number; // in months
  interestRate: number;
  purpose: string; // Home Renovation, Vehicle Purchase, etc.
  status: 'Pending' | 'Approved' | 'Rejected';
  
  // User information
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
  
  // Loan account information
  loanAccountNumber: string;
  applicationDate: string; // ISO string format
  approvalDate?: string; // ISO string format
  approvedBy?: string; // Admin who approved the loan

  // Utility methods
  generateLoanAccountNumber?(): void;
}

// Loan creation/update DTOs
export interface CreateLoanRequest {
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  purpose: string;
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
}

export interface UpdateLoanRequest {
  type?: string;
  amount?: number;
  tenure?: number;
  interestRate?: number;
  purpose?: string;
  status?: 'Pending' | 'Approved' | 'Rejected';
  approvedBy?: string;
}

// Loan approval/rejection DTOs
export interface ApproveLoanRequest {
  loanId: number;
  adminName: string;
  comments?: string;
}

export interface RejectLoanRequest {
  loanId: number;
  adminName: string;
  reason: string;
  comments?: string;
}

// Loan search and filter DTOs
export interface LoanSearchRequest {
  searchTerm?: string;
  type?: string;
  status?: 'Pending' | 'Approved' | 'Rejected';
  purpose?: string;
  minAmount?: number;
  maxAmount?: number;
  minTenure?: number;
  maxTenure?: number;
  minInterestRate?: number;
  maxInterestRate?: number;
  userName?: string;
  userEmail?: string;
  accountNumber?: string;
  loanAccountNumber?: string;
  applicationFrom?: string;
  applicationTo?: string;
  approvalFrom?: string;
  approvalTo?: string;
  approvedBy?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface LoanStatistics {
  totalLoans: number;
  pendingLoans: number;
  approvedLoans: number;
  rejectedLoans: number;
  totalApprovedAmount: number;
  averageLoanAmount: number;
  averageInterestRate: number;
  averageTenure: number;
  loansByType: { [key: string]: number };
  loansByStatus: { [key: string]: number };
  loansByPurpose: { [key: string]: number };
}