export interface EducationLoanSubsidyClaim {
  id?: number;
  loanId: number;
  loanAccountNumber: string;
  loanType: string;
  userId: string;
  userName: string;
  userEmail: string;
  accountNumber: string;
  loanAmount: number;
  interestRate: number;
  loanTenure: number;
  calculatedSubsidyAmount: number;
  approvedSubsidyAmount: number;
  status: 'Pending' | 'Approved' | 'Rejected' | 'Credited';
  requestDate: string;
  processedDate?: string;
  processedBy?: string;
  rejectionReason?: string;
  creditedDate?: string;
  creditedBy?: string;
  transactionId?: string;
  adminNotes?: string;
  userNotes?: string;
  childAadharNumber?: string;
}

export interface CreateSubsidyClaimRequest {
  loanId: number;
  childAadharNumber: string;
  userNotes?: string;
}

export interface ApproveSubsidyClaimRequest {
  claimId: number;
  approvedAmount?: number;
  adminName: string;
  adminNotes?: string;
}

export interface RejectSubsidyClaimRequest {
  claimId: number;
  adminName: string;
  rejectionReason?: string;
}

export interface CreditSubsidyRequest {
  claimId: number;
  adminName: string;
}

export interface UpdateSubsidyAmountRequest {
  claimId: number;
  newAmount: number;
  adminName: string;
}




