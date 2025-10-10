export interface KycRequest {
  id?: number;
  panNumber: string;
  name: string;
  status: 'Pending' | 'Approved' | 'Rejected';
  
  // User information
  userId: string;
  userName: string;
  userEmail: string;
  userAccountNumber: string;
  
  // Timestamps
  submittedDate: string; // ISO string format
  approvedDate?: string; // ISO string format
  approvedBy?: string; // Admin who approved the KYC
}

// KYC creation/update DTOs
export interface CreateKycRequest {
  panNumber: string;
  name: string;
  userId: string;
  userName: string;
  userEmail: string;
  userAccountNumber: string;
}

export interface UpdateKycRequest {
  panNumber?: string;
  name?: string;
  status?: 'Pending' | 'Approved' | 'Rejected';
  approvedBy?: string;
}

// KYC approval/rejection DTOs
export interface ApproveKycRequest {
  kycId: number;
  adminName: string;
  comments?: string;
}

export interface RejectKycRequest {
  kycId: number;
  adminName: string;
  reason: string;
  comments?: string;
}

// KYC search and filter DTOs
export interface KycSearchRequest {
  searchTerm?: string;
  status?: 'Pending' | 'Approved' | 'Rejected';
  userId?: string;
  userAccountNumber?: string;
  userEmail?: string;
  userName?: string;
  panNumber?: string;
  submittedFrom?: string;
  submittedTo?: string;
  approvedFrom?: string;
  approvedTo?: string;
  approvedBy?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface KycStatistics {
  totalRequests: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  requestsByStatus: { [key: string]: number };
  averageProcessingTime: number; // in hours
  requestsByMonth: { [key: string]: number };
}