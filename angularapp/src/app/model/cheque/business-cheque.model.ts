// Business Account Cheque Draw System Models - Following Salary Cheque Pattern

export type BusinessChequeStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'CANCELLED' | 'CLEARED';
export type BusinessChequeAction = 'VIEWED' | 'APPROVED' | 'REJECTED' | 'PICKED_UP' | 'CLEARED';

// User-facing BusinessChequeRequest (for Current Account Dashboard)
export interface BusinessChequeRequest {
  id?: number;
  chequeNumber: string;
  serialNumber: string;
  requestDate: string;
  chequeDate: string;
  amount: number;
  availableBalance: number;
  payeeName: string;
  remarks?: string;
  status: BusinessChequeStatus;
  createdAt?: string;
}

// Admin view with additional business details
export interface BusinessChequeRequestAdmin extends BusinessChequeRequest {
  userId: number;
  currentAccountId: number;
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
  businessName: string;
  businessType: string;
  gstNumber: string;
  ownerName: string;
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

// Form model for Business Cheque Draw request
export interface BusinessChequeDrawRequest {
  serialNumber: string;
  chequeDate: string;
  amount: number;
  payeeName: string;
  remarks?: string;
}

// Business Cheque history entry (for user dashboard)
export interface BusinessChequeHistoryEntry {
  id: number;
  chequeNumber: string;
  amount: number;
  chequeDate: string;
  payeeName: string;
  status: BusinessChequeStatus;
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
export interface BusinessChequeAdminAction {
  chequeRequestId: number;
  action: 'APPROVE' | 'REJECT';
  remarks?: string;
  rejectionReason?: string;
}

// Business Cheque audit log entry
export interface BusinessChequeAuditLog {
  id?: number;
  chequeRequestId: number;
  adminEmail: string;
  action: BusinessChequeAction;
  remarks?: string;
  ipAddress?: string;
  timestamp: string;
}

// Business Cheque book range
export interface BusinessChequeBookRange {
  id?: number;
  currentAccountId: number;
  chequeBookNumber: string;
  serialFrom: string;
  serialTo: string;
  issuedDate: string;
  status: 'ACTIVE' | 'EXHAUSTED' | 'CANCELLED';
}

// Dashboard statistics for admin
export interface BusinessChequeManagementStats {
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
export interface BusinessChequeApplyResponse {
  success: boolean;
  message: string;
  chequeNumber?: string;
  requestId?: number;
}

export interface BusinessChequeHistoryResponse {
  success: boolean;
  totalCount: number;
  items: BusinessChequeHistoryEntry[];
}

export interface BusinessChequeAdminResponse {
  success: boolean;
  message: string;
  transactionId?: string;
  newBalance?: number;
}

export interface BusinessChequeApprovalResponse {
  success: boolean;
  message: string;
  chequeRequest: BusinessChequeRequestAdmin;
  transaction?: {
    id: string;
    type: string;
    amount: number;
    balanceAfter: number;
    reference: string;
    date: string;
  };
}

// Business Cheque leaf allocation
export interface BusinessChequeLeaf {
  id: number;
  leafNumber: string;
  status: 'AVAILABLE' | 'USED' | 'CANCELLED';
}

export interface BusinessChequeLeafResponse {
  success: boolean;
  leaves: BusinessChequeLeaf[];
  totalAllocated: number;
  totalAvailable: number;
  totalUsed: number;
}
