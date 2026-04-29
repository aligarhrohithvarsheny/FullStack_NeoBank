// Cheque Draw System Models - Following Salary Dashboard Pattern

export type ChequeStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'CANCELLED' | 'CLEARED';
export type ChequeAction = 'VIEWED' | 'APPROVED' | 'REJECTED' | 'PICKED_UP' | 'CLEARED';

// User-facing ChequeRequest (for Salary Dashboard)
export interface ChequeRequest {
  id?: number;
  chequeNumber: string; // Auto-generated like CHQ000123
  serialNumber: string;
  requestDate: string; // Date cheque was requested
  chequeDate: string;  // Date written on cheque
  amount: number;
  availableBalance: number;
  payeeName: string;
  remarks?: string;
  status: ChequeStatus;
  createdAt?: string;
}

// Admin view with additional details
export interface ChequeRequestAdmin extends ChequeRequest {
  userId: number;
  salaryAccountId: number;
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
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

// Form model for Draw Cheque request
export interface ChequeDrawRequest {
  serialNumber: string;
  chequeDate: string;
  amount: number;
  payeeName: string;
  remarks?: string;
}

// Cheque history entry (for user dashboard)
export interface ChequeHistoryEntry {
  id: number;
  chequeNumber: string;
  amount: number;
  chequeDate: string;
  payeeName: string;
  status: ChequeStatus;
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
export interface ChequeAdminAction {
  chequeRequestId: number;
  action: 'APPROVE' | 'REJECT';
  remarks?: string;
  rejectionReason?: string; // For rejection
}

// Cheque audit log entry
export interface ChequeAuditLog {
  id?: number;
  chequeRequestId: number;
  adminEmail: string;
  action: ChequeAction;
  remarks?: string;
  ipAddress?: string;
  timestamp: string;
}

// Cheque book range (for validation)
export interface ChequeBookRange {
  id?: number;
  salaryAccountId: number;
  chequeBookNumber: string;
  serialFrom: string;
  serialTo: string;
  issuedDate: string;
  status: 'ACTIVE' | 'EXHAUSTED' | 'CANCELLED';
}

// Dashboard statistics for admin
export interface ChequeManagementStats {
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
export interface ChequeApplyResponse {
  success: boolean;
  message: string;
  chequeNumber?: string;
  requestId?: number;
}

export interface ChequeHistoryResponse {
  success: boolean;
  totalCount: number;
  items: ChequeHistoryEntry[];
}

export interface ChequeAdminResponse {
  success: boolean;
  message: string;
  transactionId?: string;
  newBalance?: number;
}

export interface ChequeApprovalResponse {
  success: boolean;
  message: string;
  chequeRequest: ChequeRequestAdmin;
  transaction?: {
    id: string;
    type: string;
    amount: number;
    balanceAfter: number;
    reference: string;
    date: string;
  };
}

// Cheque leaf allocation
export interface ChequeLeaf {
  id: number;
  leafNumber: string;
  status: 'AVAILABLE' | 'USED' | 'CANCELLED';
}

export interface ChequeLeafResponse {
  success: boolean;
  leaves: ChequeLeaf[];
  totalAllocated: number;
  totalAvailable: number;
  totalUsed: number;
}
