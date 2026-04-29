export interface ScheduledPayment {
  id?: number;
  accountNumber: string;
  recipientAccountNumber: string;
  recipientName: string;
  recipientIfsc?: string;
  amount: number;
  paymentType: string;
  description?: string;
  frequency: string;
  startDate: string;
  endDate?: string;
  nextPaymentDate?: string;
  lastPaymentDate?: string;
  status: string;
  totalPayments?: number;
  completedPayments?: number;
  totalAmountPaid?: number;
  failureReason?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SupportTicket {
  id?: number;
  ticketId?: string;
  accountNumber: string;
  userName: string;
  userEmail?: string;
  category: string;
  subject: string;
  description: string;
  priority: string;
  status: string;
  adminResponse?: string;
  assignedTo?: string;
  transactionId?: string;
  resolvedAt?: string;
  closedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface BankMessage {
  id?: number;
  recipientAccountNumber: string;
  recipientEmail?: string;
  messageType: string;
  title: string;
  content: string;
  priority: string;
  read: boolean;
  sender?: string;
  actionUrl?: string;
  readAt?: string;
  expiresAt?: string;
  createdAt?: string;
}

export interface VirtualCard {
  id?: number;
  accountNumber: string;
  cardNumber: string;
  cardholderName: string;
  cvv: string;
  expiryDate: string;
  status: string;
  dailyLimit: number;
  monthlyLimit: number;
  totalSpent: number;
  onlinePaymentsEnabled: boolean;
  internationalPaymentsEnabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DocumentVerification {
  id?: number;
  accountNumber: string;
  userName: string;
  userEmail?: string;
  documentType: string;
  documentNumber: string;
  documentFilePath?: string;
  status: string;
  verifiedBy?: string;
  rejectionReason?: string;
  remarks?: string;
  verifiedAt?: string;
  submittedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SubscriptionPayment {
  id?: number;
  employeeId: string;
  employeeName: string;
  employeeEmail?: string;
  salaryAccountNumber: string;
  subscriptionName: string;
  subscriptionCategory: string;
  amount: number;
  frequency: string;
  startDate: string;
  endDate?: string;
  nextBillingDate?: string;
  lastBillingDate?: string;
  status: string;
  billingCyclesCompleted?: number;
  totalAmountPaid?: number;
  autoDebit: boolean;
  merchantId?: string;
  createdAt?: string;
  updatedAt?: string;
}
