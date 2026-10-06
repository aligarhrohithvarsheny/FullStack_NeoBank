// Business Account Cheque Draw System Models - Following Salary Cheque Pattern

export type SavingsChequeStatus = 'AWAITING_POSITIVE_PAY' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'CANCELLED' | 'CLEARED';
export type SavingsChequeAction = 'VIEWED' | 'APPROVED' | 'REJECTED' | 'PICKED_UP' | 'CLEARED';

// User-facing SavingsChequeRequest (for Current Account Dashboard)
export interface SavingsChequeRequest {
  id?: number;
  chequeNumber: string;
  serialNumber: string;
  requestDate: string;
  chequeDate: string;
  amount: number;
  availableBalance: number;
  payeeName: string;
  chequePurpose?: 'GENERAL' | 'GOLD_LOAN_PREPAYMENT';
  goldLoanAccountNumber?: string;
  remarks?: string;
  status: SavingsChequeStatus;
  createdAt?: string;
}

// Admin view with additional business details
export interface SavingsChequeRequestAdmin extends SavingsChequeRequest {
  userId: number;
  accountId: number;
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
  accountType?: string;
  approvedBy?: string;
  approvedAt?: string;
  rejectionReason?: string;
  rejectedAt?: string;
  chequePickedUpAt?: string;
  chequeClearedDate?: string;
  updatedAt?: string;
  chequeDownloaded?: boolean;
  chequeDownloadedAt?: string;
  payeeAccountNumber?: string;
  payeeAccountVerified?: boolean;
  payeeAccountType?: string;
  transactionReference?: string;
  debitedFromAccount?: string;
  creditedToAccount?: string;
}

// Form model for Savings Cheque Draw request
export interface SavingsChequeDrawRequest {
  serialNumber: string;
  chequeDate: string;
  amount: number;
  payeeName: string;
  chequePurpose?: 'GENERAL' | 'GOLD_LOAN_PREPAYMENT';
  goldLoanAccountNumber?: string;
  remarks?: string;
}

// Savings Cheque history entry (for user dashboard)
export interface SavingsChequeHistoryEntry {
  id: number;
  chequeNumber: string;
  amount: number;
  chequeDate: string;
  payeeName: string;
  chequePurpose?: 'GENERAL' | 'GOLD_LOAN_PREPAYMENT';
  goldLoanAccountNumber?: string;
  status: SavingsChequeStatus;
  requestedDate: string;
  approvedDate?: string;
  approvedAt?: string;
  remarks?: string;
  chequeDownloaded?: boolean;
  chequeDownloadedAt?: string;
  payeeAccountNumber?: string;
  payeeAccountVerified?: boolean;
  payeeAccountType?: string;
  transactionReference?: string;
  debitedFromAccount?: string;
  creditedToAccount?: string;
}

// Admin approval/rejection request
export interface SavingsChequeAdminAction {
  chequeRequestId: number;
  action: 'APPROVE' | 'REJECT';
  remarks?: string;
  rejectionReason?: string;
}

// Savings Cheque audit log entry
export interface SavingsChequeAuditLog {
  id?: number;
  chequeRequestId: number;
  adminEmail: string;
  action: SavingsChequeAction;
  remarks?: string;
  ipAddress?: string;
  timestamp: string;
}

// Savings Cheque book range
export interface SavingsChequeBookRange {
  id?: number;
  accountId: number;
  chequeBookNumber: string;
  serialFrom: string;
  serialTo: string;
  issuedDate: string;
  status: 'ACTIVE' | 'EXHAUSTED' | 'CANCELLED';
}

// Dashboard statistics for admin
export interface SavingsChequeManagementStats {
  totalRequests: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  completedRequests: number;
  totalAmountPending: number;
  totalAmountApproved: number;
  totalAmountProcessed: number;
}

// Response types
export interface SavingsChequeApplyResponse {
  success: boolean;
  message: string;
  chequeNumber?: string;
  requestId?: number;
  positivePayRequired?: boolean;
  positivePayMessage?: string;
  positivePayAccountNumber?: string;
}

export interface SavingsChequeHistoryResponse {
  success: boolean;
  totalCount: number;
  items: SavingsChequeHistoryEntry[];
}

export interface SavingsChequeAdminResponse {
  success: boolean;
  message: string;
  transactionId?: string;
  newBalance?: number;
}

export interface SavingsChequeApprovalResponse {
  success: boolean;
  message: string;
  chequeRequest: SavingsChequeRequestAdmin;
  transaction?: {
    id: string;
    type: string;
    amount: number;
    balanceAfter: number;
    reference: string;
    date: string;
  };
}

// Savings Cheque leaf allocation
export interface SavingsChequeLeaf {
  id: number;
  leafNumber: string;
  status: 'AVAILABLE' | 'USED' | 'CANCELLED';
}

export interface SavingsChequeLeafResponse {
  success: boolean;
  leaves: SavingsChequeLeaf[];
  totalAllocated: number;
  totalAvailable: number;
  totalUsed: number;
}
