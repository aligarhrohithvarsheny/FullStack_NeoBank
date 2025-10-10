export interface Card {
  id?: number;
  cardNumber: string;
  cardType: 'Debit' | 'Credit';
  cvv: string;
  userName: string;
  expiryDate: string;
  pin: string;
  blocked: boolean;
  deactivated: boolean;
  pinSet: boolean;
  status: 'Active' | 'Blocked' | 'Deactivated';
  
  // User information
  accountNumber: string;
  userEmail: string;
  
  // Card management
  issueDate: string; // ISO string format
  expiryDateTime: string; // ISO string format
  lastUsed?: string; // ISO string format

  // Utility methods
  getMaskedCardNumber?(): string;
}

// Card creation/update DTOs
export interface CreateCardRequest {
  cardNumber: string;
  cardType: 'Debit' | 'Credit';
  userName: string;
  accountNumber: string;
  userEmail: string;
}

export interface UpdateCardRequest {
  cardType?: 'Debit' | 'Credit';
  pin?: string;
  blocked?: boolean;
  deactivated?: boolean;
  pinSet?: boolean;
  status?: 'Active' | 'Blocked' | 'Deactivated';
}

// Card PIN management DTOs
export interface SetPinRequest {
  cardId: number;
  newPin: string;
  confirmPin: string;
}

export interface ChangePinRequest {
  cardId: number;
  currentPin: string;
  newPin: string;
  confirmPin: string;
}

// Card search and filter DTOs
export interface CardSearchRequest {
  searchTerm?: string;
  cardType?: 'Debit' | 'Credit';
  status?: 'Active' | 'Blocked' | 'Deactivated';
  pinSet?: boolean;
  blocked?: boolean;
  deactivated?: boolean;
  userName?: string;
  accountNumber?: string;
  userEmail?: string;
  issuedFrom?: string;
  issuedTo?: string;
  expiresFrom?: string;
  expiresTo?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface CardStatistics {
  totalCards: number;
  activeCards: number;
  blockedCards: number;
  deactivatedCards: number;
  pinSetCards: number;
  pinNotSetCards: number;
  cardsByType: { [key: string]: number };
  cardsByStatus: { [key: string]: number };
}