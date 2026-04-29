export interface Transaction {
  id?: number;
  transactionId: string; // Custom transaction ID like TXN123456
  merchant: string;
  amount: number;
  type: 'Debit' | 'Credit' | 'Deposit' | 'Withdraw' | 'Transfer' | 'Loan Credit';
  description: string; // Detailed description of the transaction
  balance: number;
  date: string; // ISO string format
  status: 'Completed' | 'Pending' | 'Failed';
  
  // User information
  userName: string;
  accountNumber: string;
  
  // Transfer specific fields
  recipientAccountNumber?: string;
  recipientName?: string;
  ifscCode?: string;
  transferType?: 'NEFT' | 'RTGS';
}

// Transaction creation/update DTOs
export interface CreateTransactionRequest {
  merchant: string;
  amount: number;
  type: 'Debit' | 'Credit' | 'Deposit' | 'Withdraw' | 'Transfer' | 'Loan Credit';
  description: string;
  balance: number;
  userName: string;
  accountNumber: string;
  recipientAccountNumber?: string;
  recipientName?: string;
  ifscCode?: string;
  transferType?: 'NEFT' | 'RTGS';
}

export interface UpdateTransactionRequest {
  merchant?: string;
  amount?: number;
  type?: 'Debit' | 'Credit' | 'Deposit' | 'Withdraw' | 'Transfer' | 'Loan Credit';
  description?: string;
  balance?: number;
  status?: 'Completed' | 'Pending' | 'Failed';
  recipientAccountNumber?: string;
  recipientName?: string;
  ifscCode?: string;
  transferType?: 'NEFT' | 'RTGS';
}

// Transaction search and filter DTOs
export interface TransactionSearchRequest {
  searchTerm?: string;
  type?: 'Debit' | 'Credit' | 'Deposit' | 'Withdraw' | 'Transfer' | 'Loan Credit';
  status?: 'Completed' | 'Pending' | 'Failed';
  merchant?: string;
  description?: string;
  userName?: string;
  accountNumber?: string;
  minAmount?: number;
  maxAmount?: number;
  minBalance?: number;
  maxBalance?: number;
  dateFrom?: string;
  dateTo?: string;
  transferType?: 'NEFT' | 'RTGS';
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface TransactionStatistics {
  totalTransactions: number;
  completedTransactions: number;
  pendingTransactions: number;
  failedTransactions: number;
  totalDebitAmount: number;
  totalCreditAmount: number;
  totalTransferAmount: number;
  averageTransactionAmount: number;
  transactionsByType: { [key: string]: number };
  transactionsByStatus: { [key: string]: number };
  transactionsByMonth: { [key: string]: number };
}