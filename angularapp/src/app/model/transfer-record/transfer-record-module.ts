export interface TransferRecord {
  id?: number;
  transferId: string; // Custom transfer ID
  senderAccountNumber: string;
  senderName: string;
  recipientAccountNumber: string;
  recipientName: string;
  phone: string;
  ifsc: string;
  amount: number;
  status: 'Pending' | 'Completed' | 'Failed';
  transferType: TransferType;
  date: string; // ISO string format
}

// Enum for NEFT/RTGS
export enum TransferType {
  NEFT = 'NEFT',
  RTGS = 'RTGS'
}

// Transfer creation/update DTOs
export interface CreateTransferRequest {
  senderAccountNumber: string;
  senderName: string;
  recipientAccountNumber: string;
  recipientName: string;
  phone: string;
  ifsc: string;
  amount: number;
  transferType: TransferType;
}

export interface UpdateTransferRequest {
  recipientAccountNumber?: string;
  recipientName?: string;
  phone?: string;
  ifsc?: string;
  amount?: number;
  status?: 'Pending' | 'Completed' | 'Failed';
  transferType?: TransferType;
}

// Transfer processing DTOs
export interface ProcessTransferRequest {
  transferId: number;
  status: 'Completed' | 'Failed';
  comments?: string;
  processedBy?: string;
}

// Transfer search and filter DTOs
export interface TransferSearchRequest {
  searchTerm?: string;
  transferType?: TransferType;
  status?: 'Pending' | 'Completed' | 'Failed';
  senderAccountNumber?: string;
  senderName?: string;
  recipientAccountNumber?: string;
  recipientName?: string;
  phone?: string;
  ifsc?: string;
  minAmount?: number;
  maxAmount?: number;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface TransferStatistics {
  totalTransfers: number;
  pendingTransfers: number;
  completedTransfers: number;
  failedTransfers: number;
  totalTransferAmount: number;
  averageTransferAmount: number;
  transfersByType: { [key: string]: number };
  transfersByStatus: { [key: string]: number };
  transfersByMonth: { [key: string]: number };
}