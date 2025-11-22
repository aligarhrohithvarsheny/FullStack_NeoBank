export interface Cheque {
  id?: number;
  chequeNumber: string;
  accountNumber: string;
  accountHolderName: string;
  bankName?: string;
  branchName?: string;
  ifscCode?: string;
  micrCode?: string;
  accountType?: string;
  amount?: number; // Amount to be withdrawn when cheque is drawn
  status: 'ACTIVE' | 'DRAWN' | 'BOUNCED' | 'CANCELLED';
  createdAt?: string; // ISO string format
  usedDate?: string; // ISO string format
  drawnDate?: string; // ISO string format
  bouncedDate?: string; // ISO string format
  cancelledDate?: string; // ISO string format
  cancelledBy?: string;
  cancellationReason?: string;
  drawnBy?: string; // Admin who drew the cheque
  bouncedBy?: string; // Admin who bounced the cheque
  bounceReason?: string; // Reason for bouncing
  requestStatus?: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED'; // Request status
  requestDate?: string; // ISO string format
  requestedBy?: string; // User who requested the cheque draw
  approvedDate?: string; // ISO string format
  rejectedDate?: string; // ISO string format
  approvedBy?: string; // Admin who approved the request
  rejectedBy?: string; // Admin who rejected the request
  rejectionReason?: string; // Reason for rejection
}

// Cheque creation request
export interface CreateChequeRequest {
  accountNumber: string;
  numberOfLeaves: number;
  amount?: number; // Optional amount for each cheque
}

// Cheque cancellation request
export interface CancelChequeRequest {
  cancelledBy: string;
  reason: string;
}

// Cheque draw request (Admin)
export interface DrawChequeRequest {
  chequeNumber: string;
  drawnBy: string;
}

// Cheque bounce request (Admin)
export interface BounceChequeRequest {
  chequeNumber: string;
  bouncedBy: string;
  reason: string;
}

// Cheque draw request (User)
export interface RequestChequeDraw {
  requestedBy: string;
}

// Approve cheque request (Admin)
export interface ApproveChequeRequest {
  approvedBy: string;
}

// Reject cheque request (Admin)
export interface RejectChequeRequest {
  rejectedBy: string;
  reason: string;
}

