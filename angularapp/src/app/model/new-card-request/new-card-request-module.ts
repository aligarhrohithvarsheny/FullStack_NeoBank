export interface NewCardRequest {
  id?: number;
  userId: string;
  userName: string;
  userEmail: string;
  accountNumber: string;
  cardType: string; // Visa Debit, Mastercard, etc.
  reason: string; // Additional card, Upgrade, etc.
  status: 'Pending' | 'Approved' | 'Rejected';
  requestDate: string; // ISO string format
  processedDate?: string; // ISO string format
  processedBy?: string; // Admin who processed the request
  newCardNumber?: string; // Generated when approved
}

// New card creation/update DTOs
export interface CreateNewCardRequest {
  userId: string;
  userName: string;
  userEmail: string;
  accountNumber: string;
  cardType: string;
  reason: string;
}

export interface UpdateNewCardRequest {
  cardType?: string;
  reason?: string;
  status?: 'Pending' | 'Approved' | 'Rejected';
  processedBy?: string;
  newCardNumber?: string;
}

// New card processing DTOs
export interface ApproveNewCardRequest {
  requestId: number;
  adminName: string;
  newCardNumber: string;
  comments?: string;
}

export interface RejectNewCardRequest {
  requestId: number;
  adminName: string;
  reason: string;
  comments?: string;
}

// New card search and filter DTOs
export interface NewCardSearchRequest {
  searchTerm?: string;
  status?: 'Pending' | 'Approved' | 'Rejected';
  userId?: string;
  userName?: string;
  userEmail?: string;
  accountNumber?: string;
  cardType?: string;
  reason?: string;
  processedBy?: string;
  requestFrom?: string;
  requestTo?: string;
  processedFrom?: string;
  processedTo?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface NewCardStatistics {
  totalRequests: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  requestsByStatus: { [key: string]: number };
  requestsByCardType: { [key: string]: number };
  requestsByReason: { [key: string]: number };
  averageProcessingTime: number; // in hours
  requestsByMonth: { [key: string]: number };
}