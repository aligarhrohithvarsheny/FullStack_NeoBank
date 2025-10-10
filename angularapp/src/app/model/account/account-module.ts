export interface Account {
  id?: number;
  name: string;
  dob: string;
  age: number;
  occupation: string;
  accountType: 'Savings' | 'Current';
  createdAt: string; // ISO string format
  lastUpdated: string; // ISO string format
  status: 'ACTIVE' | 'INACTIVE' | 'CLOSED';
  aadharNumber: string;
  pan: string;
  accountNumber: string;
  balance: number;
  income: number;
  phone: string;
  address: string;
  verifiedMatrix: boolean;
  kycVerified: boolean;
}

// Account creation/update DTOs
export interface CreateAccountRequest {
  name: string;
  dob: string;
  age: number;
  occupation: string;
  aadharNumber: string;
  pan: string;
  accountNumber: string;
  income: number;
  phone: string;
  address: string;
}

export interface UpdateAccountRequest {
  name?: string;
  dob?: string;
  age?: number;
  occupation?: string;
  accountType?: 'Savings' | 'Current';
  income?: number;
  phone?: string;
  address?: string;
  status?: 'ACTIVE' | 'INACTIVE' | 'CLOSED';
  verifiedMatrix?: boolean;
  kycVerified?: boolean;
}

// Account search and filter DTOs
export interface AccountSearchRequest {
  searchTerm?: string;
  accountType?: 'Savings' | 'Current';
  status?: 'ACTIVE' | 'INACTIVE' | 'CLOSED';
  occupation?: string;
  minIncome?: number;
  maxIncome?: number;
  minBalance?: number;
  maxBalance?: number;
  kycVerified?: boolean;
  verifiedMatrix?: boolean;
  createdFrom?: string;
  createdTo?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface AccountStatistics {
  totalAccounts: number;
  activeAccounts: number;
  inactiveAccounts: number;
  closedAccounts: number;
  kycVerifiedAccounts: number;
  verifiedMatrixAccounts: number;
  averageBalance: number;
  totalBalance: number;
  accountsByType: { [key: string]: number };
  accountsByOccupation: { [key: string]: number };
}