export interface UpiPayment {
  id?: number;
  txnId?: string;
  accountNumber: string;
  businessName?: string;
  upiId: string;
  amount: number;
  payerName?: string;
  payerUpi?: string;
  paymentMethod?: string;
  status?: string;
  txnType?: string;
  note?: string;
  qrGenerated?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpiStats {
  upiId?: string;
  upiEnabled?: boolean;
  totalReceived: number;
  todayReceived: number;
  todayTransactions: number;
  balance?: number;
}

export interface AdminUpiStats {
  totalPayments: number;
  totalVolume: number;
  activeAccounts: number;
  todayVolume: number;
}

export interface QrCodeResponse {
  success: boolean;
  qrCode: string;
  upiId: string;
  businessName: string;
  accountNumber: string;
  amount?: number;
}
