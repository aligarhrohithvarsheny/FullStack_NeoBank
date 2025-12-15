export interface Admin {
  id?: number;
  name: string;
  email: string;
  password?: string; // Optional for updates
  role: string;
  pan?: string;
  employeeId?: string;
  address?: string;
  aadharNumber?: string;
  mobileNumber?: string;
  qualifications?: string;
  createdAt?: string;
  lastUpdated?: string;
}

// Admin creation/update DTOs
export interface CreateAdminRequest {
  name: string;
  email: string;
  password: string;
  role: string;
  pan: string;
}

export interface UpdateAdminRequest {
  name?: string;
  email?: string;
  password?: string;
  role?: string;
  pan?: string;
  employeeId?: string;
  address?: string;
  aadharNumber?: string;
  mobileNumber?: string;
  qualifications?: string;
}

// Admin authentication DTOs
export interface AdminLoginRequest {
  email: string;
  password: string;
}

export interface AdminLoginResponse {
  admin: Admin;
  token?: string; // If implementing JWT later
  message: string;
}

// Admin search and filter DTOs
export interface AdminSearchRequest {
  searchTerm?: string;
  role?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface AdminStatistics {
  totalAdmins: number;
  adminsByRole: { [key: string]: number };
}
