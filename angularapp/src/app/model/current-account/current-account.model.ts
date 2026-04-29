export interface CurrentAccount {
  id?: number;
  accountNumber?: string;
  customerId?: string;

  // Business Details
  businessName: string;
  businessType: string; // Proprietor, Partnership, Pvt Ltd, Startup
  businessRegistrationNumber?: string;
  gstNumber?: string;

  // Owner Details
  ownerName: string;
  mobile: string;
  email: string;
  aadharNumber: string;
  panNumber: string;

  // Business Address
  shopAddress?: string;
  city?: string;
  state?: string;
  pincode?: string;

  // Bank Details
  branchName?: string;
  ifscCode?: string;

  // Account Details
  balance?: number;
  overdraftLimit?: number;
  overdraftEnabled?: boolean;
  minimumBalance?: number;

  // Status
  status?: string; // PENDING, APPROVED, ACTIVE, FROZEN, CLOSED

  // KYC
  kycVerified?: boolean;
  kycVerifiedDate?: string;
  kycVerifiedBy?: string;

  // KYC Document Paths
  gstCertificatePath?: string;
  businessRegistrationCertificatePath?: string;
  panCardPath?: string;
  addressProofPath?: string;

  // Freeze
  accountFrozen?: boolean;
  frozenReason?: string;
  frozenBy?: string;
  frozenDate?: string;

  // Timestamps
  createdAt?: string;
  approvedAt?: string;
  approvedBy?: string;
  lastUpdated?: string;
}

export interface BusinessTransaction {
  id?: number;
  txnId?: string;
  accountNumber: string;
  txnType: string; // Credit, Debit, Transfer, Vendor Payment, Bulk Payment
  amount: number;
  description?: string;
  balance?: number;
  status?: string; // Completed, Pending, Failed
  recipientAccount?: string;
  recipientName?: string;
  transferType?: string; // NEFT, RTGS
  chargeAmount?: number;
  date?: string;
}

export interface CurrentAccountStatistics {
  totalAccounts: number;
  pendingAccounts: number;
  activeAccounts: number;
  frozenAccounts: number;
  totalActiveBalance: number;
  overdrawnAccounts: number;
}

export interface TransactionSummary {
  totalCredits: number;
  totalDebits: number;
  todayTransactions: number;
}

export interface CurrentAccountEditHistory {
  id?: number;
  accountId: number;
  accountNumber: string;
  editedBy: string;
  editedAt?: string;
  changesDescription: string;
  fieldChanges?: string;
  documentPath?: string;
  documentName?: string;
}

export interface CurrentAccountBeneficiary {
  id?: number;
  accountNumber: string;
  beneficiaryName: string;
  beneficiaryAccount: string;
  beneficiaryIfsc: string;
  beneficiaryBank?: string;
  nickName?: string;
  status?: string;
  createdAt?: string;
}

export interface CurrentAccountChequeRequest {
  id?: number;
  requestId?: string;
  accountNumber: string;
  leaves?: number;
  deliveryAddress?: string;
  status?: string;
  requestedAt?: string;
  approvedAt?: string;
  approvedBy?: string;
}

export interface CurrentAccountVendorPayment {
  id?: number;
  accountNumber: string;
  vendorName: string;
  vendorAccount: string;
  vendorIfsc?: string;
  amount: number;
  description?: string;
  status?: string;
  paidAt?: string;
  createdAt?: string;
}

export interface CurrentAccountInvoice {
  id?: number;
  invoiceNumber?: string;
  accountNumber: string;
  clientName: string;
  clientEmail?: string;
  clientPhone?: string;
  clientAddress?: string;
  clientGst?: string;
  invoiceDate: string;
  dueDate: string;
  itemsJson: string;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  discount: number;
  totalAmount: number;
  notes?: string;
  terms?: string;
  status?: string;
  paidAmount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface InvoiceItem {
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
}

export interface CurrentAccountBusinessLoan {
  id?: number;
  applicationId?: string;
  accountNumber: string;
  loanType: string;
  panNumber: string;
  requestedAmount: number;
  approvedAmount?: number;
  interestRate?: number;
  tenureMonths: number;
  monthlyEmi?: number;
  purpose: string;
  annualRevenue: number;
  yearsInBusiness: number;
  cibilScore?: number;
  cibilStatus?: string;
  businessName?: string;
  ownerName?: string;
  status?: string;
  rejectionReason?: string;
  appliedAt?: string;
  processedAt?: string;
  processedBy?: string;
  disbursedAt?: string;
}

export interface CurrentAccountBusinessUser {
  id?: number;
  userId?: string;
  accountNumber: string;
  fullName: string;
  email: string;
  mobile: string;
  role: string;
  password?: string;
  status?: string;
  lastLogin?: string;
  createdAt?: string;
  createdBy?: string;
}

export interface LinkedAccount {
  id?: number;
  currentAccountNumber: string;
  savingsAccountNumber: string;
  savingsCustomerId: string;
  linkedBy: string;
  linkedAt?: string;
  status?: string;
  pinCreated?: boolean;
}

export interface SavingsAccountDetails {
  name?: string;
  accountNumber?: string;
  customerId?: string;
  balance?: number;
  status?: string;
  phone?: string;
  accountType?: string;
}
