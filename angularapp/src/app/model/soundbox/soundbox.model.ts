export interface SoundboxDevice {
  id?: number;
  deviceId?: string;
  accountNumber: string;
  businessName?: string;
  ownerName?: string;
  status?: string; // ACTIVE, INACTIVE, MAINTENANCE
  voiceEnabled?: boolean;
  voiceLanguage?: string; // en-IN, hi-IN, te-IN
  volumeMode?: string; // NORMAL, LOUD, SILENT
  linkedUpi?: string;
  monthlyCharge?: number;
  deviceCharge?: number;
  chargeStatus?: string; // PENDING, PAID, OVERDUE
  lastActiveAt?: string;
  activatedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SoundboxRequest {
  id?: number;
  requestId?: string;
  accountNumber: string;
  businessName: string;
  ownerName: string;
  deliveryAddress?: string;
  city?: string;
  state?: string;
  pincode?: string;
  mobile?: string;
  status?: string; // PENDING, APPROVED, REJECTED, DELIVERED
  adminRemarks?: string;
  assignedDeviceId?: string;
  monthlyCharge?: number;
  deviceCharge?: number;
  processedBy?: string;
  processedAt?: string;
  requestedAt?: string;
}

export interface SoundboxTransaction {
  id?: number;
  txnId?: string;
  accountNumber: string;
  deviceId?: string;
  amount: number;
  txnType?: string; // CREDIT, DEBIT
  paymentMethod?: string; // UPI, QR, NFC
  payerName?: string;
  payerUpi?: string;
  status?: string; // SUCCESS, FAILED, PENDING
  voicePlayed?: boolean;
  voiceMessage?: string;
  createdAt?: string;
}

export interface SoundboxStats {
  hasDevice: boolean;
  device?: SoundboxDevice;
  deviceStatus?: string;
  voiceEnabled?: boolean;
  totalReceived: number;
  todayReceived: number;
  todayTransactions: number;
}

export interface AdminSoundboxStats {
  totalDevices: number;
  activeDevices: number;
  pendingRequests: number;
  totalTransactions: number;
  totalRevenue: number;
  todayRevenue: number;
  monthlyChargesRevenue: number;
  deviceChargesRevenue: number;
}
