export interface AdminAccountApplication {
  id?: number;
  applicationNumber?: string;
  accountType: string; // Savings, Current, Salary
  status?: string; // PENDING, DOCUMENTS_UPLOADED, ADMIN_VERIFIED, MANAGER_APPROVED, MANAGER_REJECTED, ACTIVE, CLOSED

  // Personal Details
  fullName: string;
  dateOfBirth?: string;
  age?: number;
  gender?: string;
  occupation?: string;
  income?: number;
  phone: string;
  email?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;

  // Identity Documents
  aadharNumber: string;
  panNumber: string;

  // Business Details (Current)
  businessName?: string;
  businessType?: string;
  businessRegistrationNumber?: string;
  gstNumber?: string;
  shopAddress?: string;

  // Salary Details (Salary)
  companyName?: string;
  companyId?: string;
  designation?: string;
  monthlySalary?: number;
  salaryCreditDate?: number;
  employerAddress?: string;
  hrContactNumber?: string;

  // Bank Details
  branchName?: string;
  ifscCode?: string;
  accountNumber?: string;
  customerId?: string;

  // Verification
  adminVerified?: boolean;
  adminVerifiedBy?: string;
  adminVerifiedDate?: string;
  adminRemarks?: string;

  // Manager Approval
  managerApproved?: boolean;
  managerApprovedBy?: string;
  managerApprovedDate?: string;
  managerRemarks?: string;

  // Documents
  applicationPdfPath?: string;
  signedApplicationPath?: string;
  additionalDocumentsPath?: string;

  // Signatures
  applicantSignaturePath?: string;
  bankOfficerSignature?: string;
  declarationAccepted?: boolean;
  declarationDate?: string;

  // Audit
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminAccountAppStats {
  totalApplications: number;
  pendingCount: number;
  adminVerifiedCount: number;
  managerApprovedCount: number;
  rejectedCount: number;
  savingsCount: number;
  currentCount: number;
  salaryCount: number;
}
