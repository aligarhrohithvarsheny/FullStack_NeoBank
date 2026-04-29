export interface CardReplacementRequest {
  id?: number;
  userId: string;
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentCardNumber: string;
  reason: string; // Lost, Damaged, Stolen, etc.
  status: 'Pending' | 'Approved' | 'Rejected';
  requestDate: string; // ISO string format
  processedDate?: string; // ISO string format
  processedBy?: string; // Admin who processed the request
  newCardNumber?: string; // Generated when approved
}

// Card replacement creation/update DTOs
export interface CreateCardReplacementRequest {
  userId: string;
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentCardNumber: string;
  reason: string;
}

export interface UpdateCardReplacementRequest {
  reason?: string;
  status?: 'Pending' | 'Approved' | 'Rejected';
  processedBy?: string;
  newCardNumber?: string;
}

// Card replacement processing DTOs
export interface ApproveCardReplacementRequest {
  requestId: number;
  adminName: string;
  newCardNumber: string;
  comments?: string;
}

export interface RejectCardReplacementRequest {
  requestId: number;
  adminName: string;
  reason: string;
  comments?: string;
}

// Card replacement search and filter DTOs
export interface CardReplacementSearchRequest {
  searchTerm?: string;
  status?: 'Pending' | 'Approved' | 'Rejected';
  userId?: string;
  userName?: string;
  userEmail?: string;
  accountNumber?: string;
  currentCardNumber?: string;
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

export interface CardReplacementStatistics {
  totalRequests: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  requestsByStatus: { [key: string]: number };
  requestsByReason: { [key: string]: number };
  averageProcessingTime: number; // in hours
  requestsByMonth: { [key: string]: number };
}