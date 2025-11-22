// Import Account interfaces from account module
import { Account, CreateAccountRequest } from '../account/account-module';

export interface User {
  id?: number;
  username: string;
  password: string;
  email: string;
  joinDate: string; // ISO string format
  status: 'PENDING' | 'APPROVED' | 'CLOSED';
  accountNumber: string;
  account?: Account; // One-to-one relationship with Account

  // Profile photo fields
  profilePhoto?: string; // Base64 or URL
  profilePhotoType?: string;
  profilePhotoName?: string;

  // Signature fields
  signature?: string; // Base64 or URL
  signatureType?: string;
  signatureName?: string;
  signatureStatus?: 'PENDING' | 'APPROVED' | 'REJECTED';
  signatureSubmittedDate?: string;
  signatureReviewedDate?: string;
  signatureReviewedBy?: string;
  signatureRejectionReason?: string;

  // Convenience methods to access account fields
  getName?(): string | null;
  getPhone?(): string | null;
  getAddress?(): string | null;
  getDob?(): string | null;
  getOccupation?(): string | null;
  getIncome?(): number | null;
  getPan?(): string | null;
  getAadhar?(): string | null;
  getAccountType?(): string | null;
  getBalance?(): number | null;
}

// User creation/update DTOs
export interface CreateUserRequest {
  username: string;
  password: string;
  email: string;
  account: CreateAccountRequest;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  status?: 'PENDING' | 'APPROVED' | 'CLOSED';
  account?: Partial<CreateAccountRequest>;
}

// User search and filter DTOs
export interface UserSearchRequest {
  searchTerm?: string;
  status?: 'PENDING' | 'APPROVED' | 'CLOSED';
  accountType?: 'Savings' | 'Current';
  occupation?: string;
  minIncome?: number;
  maxIncome?: number;
  joinDateFrom?: string;
  joinDateTo?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface UserStatistics {
  totalUsers: number;
  pendingUsers: number;
  approvedUsers: number;
  closedUsers: number;
  averageIncome: number;
  totalBalance: number;
  usersByAccountType: { [key: string]: number };
  usersByOccupation: { [key: string]: number };
}