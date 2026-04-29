export interface Agent {
  id?: number;
  agentId?: string;
  name: string;
  email: string;
  mobile: string;
  password?: string;
  status?: string;
  region?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Merchant {
  id?: number;
  merchantId?: string;
  businessName: string;
  ownerName: string;
  mobile: string;
  email?: string;
  businessType: string;
  gstNumber?: string;
  shopAddress: string;
  city: string;
  state: string;
  pincode: string;
  bankName: string;
  accountNumber: string;
  ifscCode: string;
  accountHolderName: string;
  status?: string;
  agentId: string;
  shopPhotoPath?: string;
  ownerIdProofPath?: string;
  bankProofPath?: string;
  rejectionReason?: string;
  activatedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MerchantApplication {
  id?: number;
  applicationId?: string;
  merchantId: string;
  deviceType: string;
  deviceQuantity?: number;
  status?: string;
  agentId: string;
  adminRemarks?: string;
  processedBy?: string;
  processedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MerchantDevice {
  id?: number;
  deviceId?: string;
  deviceType: string;
  merchantId: string;
  applicationId?: string;
  status?: string;
  activatedAt?: string;
  lastActiveAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MerchantTransaction {
  id?: number;
  transactionId?: string;
  merchantId: string;
  deviceId?: string;
  amount: number;
  paymentMode?: string;
  payerName?: string;
  payerUpi?: string;
  status?: string;
  createdAt?: string;
}

export interface MerchantCreatePayload {
  businessName: string;
  ownerName: string;
  mobile: string;
  email?: string;
  businessType: string;
  gstNumber?: string;
  shopAddress: string;
  city: string;
  state: string;
  pincode: string;
  bankName: string;
  accountNumber: string;
  ifscCode: string;
  accountHolderName: string;
  agentId: string;
  deviceTypes?: string[];
  deviceQuantities?: number[];
}
