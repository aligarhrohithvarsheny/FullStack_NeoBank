import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, EventEmitter, Inject, Output, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface EmiOption {
  emiNumber: number;
  dueDate: string;
  amount: number;
}

interface LoanDetails {
  maskedName: string;
  loanType: string;
  maskedLoanAccountNumber: string;
  linkedAccountNumber: string;
  status: string;
  outstandingPrincipal: number;
  emis: EmiOption[];
  prepaymentSupported: boolean;
  prepaymentChargePercent?: number;
  prepaymentGstPercent?: number;
  prepaymentAdjustmentOptions?: string[];
}

type PaymentMode = 'EMI' | 'PREPAYMENT';

@Component({
  selector: 'app-loan-repayment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan-repayment.html',
  styleUrls: ['./loan-repayment.css']
})
export class LoanRepayment {
  @Output() balanceUpdated = new EventEmitter<number>();

  loanAccountNumber = '';
  dob = '';
  details: LoanDetails | null = null;
  paymentMode: PaymentMode = 'EMI';
  selectedEmiNumber: number | null = null;
  prepaymentAmount: number | null = null;
  adjustment: 'REDUCE_TENURE' | 'REDUCE_EMI' = 'REDUCE_TENURE';
  isLookingUp = false;
  isPaying = false;
  isReviewingPayment = false;
  error = '';
  success = '';
  transactionId = '';
  newBalance: number | null = null;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  lookupLoan(preserveNotice = false) {
    const number = this.loanAccountNumber.trim();
    if (!number || !this.dob) {
      this.error = 'Enter the loan account number and date of birth.';
      return;
    }

    this.isLookingUp = true;
    this.error = '';
    if (!preserveNotice) this.success = '';
    this.details = null;
    this.isReviewingPayment = false;
    this.http.post<any>(`${environment.apiBaseUrl}/api/loans/repayment/lookup`, {
      loanAccountNumber: number,
      dob: this.dob
    }).subscribe({
      next: response => {
        this.isLookingUp = false;
        this.details = response;
        this.paymentMode = 'EMI';
        this.selectedEmiNumber = response.emis?.[0]?.emiNumber ?? null;
        this.prepaymentAmount = null;
      },
      error: err => {
        this.isLookingUp = false;
        this.error = err.error?.message || 'We could not verify those loan details.';
      }
    });
  }

  clearVerification() {
    this.details = null;
    this.success = '';
    this.error = '';
    this.isReviewingPayment = false;
  }

  get selectedEmi(): EmiOption | null {
    return this.details?.emis.find(emi => emi.emiNumber === this.selectedEmiNumber) ?? null;
  }

  get prepaymentCharges(): number {
    if (!this.details || !this.prepaymentAmount || this.prepaymentAmount <= 0) return 0;
    const fee = this.prepaymentAmount * (this.details.prepaymentChargePercent ?? 0) / 100;
    return fee + fee * (this.details.prepaymentGstPercent ?? 0) / 100;
  }

  get paymentTotal(): number {
    if (this.paymentMode === 'EMI') return this.selectedEmi?.amount ?? 0;
    return (this.prepaymentAmount ?? 0) + this.prepaymentCharges;
  }

  reviewPayment() {
    this.error = '';
    if (!this.details) return;
    if (this.paymentMode === 'EMI' && !this.selectedEmi) {
      this.error = 'There are no pending EMIs available for this loan.';
      return;
    }
    if (this.paymentMode === 'PREPAYMENT'
        && (!this.details.prepaymentSupported || !this.prepaymentAmount
          || this.prepaymentAmount <= 0 || this.prepaymentAmount >= this.details.outstandingPrincipal)) {
      this.error = 'Enter a prepayment amount below the outstanding principal.';
      return;
    }
    this.isReviewingPayment = true;
  }

  confirmPayment() {
    if (!this.details || this.isPaying) return;
    this.isPaying = true;
    this.error = '';
    const body: Record<string, string | number | null> = {
      loanAccountNumber: this.loanAccountNumber.trim(),
      dob: this.dob,
      paymentMode: this.paymentMode,
      emiNumber: this.paymentMode === 'EMI' ? this.selectedEmiNumber : null,
      amount: this.paymentMode === 'PREPAYMENT' ? this.prepaymentAmount : null,
      adjustment: this.adjustment
    };
    this.http.post<any>(`${environment.apiBaseUrl}/api/loans/repayment/pay`, body).subscribe({
      next: response => {
        this.isPaying = false;
        this.isReviewingPayment = false;
        this.success = response.message || 'Loan payment completed successfully.';
        this.transactionId = response.transaction?.transactionId || '';
        this.newBalance = response.newBalance ?? null;
        if (typeof response.newBalance === 'number') this.balanceUpdated.emit(response.newBalance);
        this.downloadReceipt(response.receiptPdfBase64);
        this.lookupLoan(true);
      },
      error: err => {
        this.isPaying = false;
        this.error = err.error?.message || 'Loan payment failed. Please try again.';
      }
    });
  }

  private downloadReceipt(encodedPdf: string | undefined) {
    if (!encodedPdf || !isPlatformBrowser(this.platformId)) return;
    const bytes = Uint8Array.from(atob(encodedPdf), char => char.charCodeAt(0));
    const url = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    const suffix = (this.details?.maskedLoanAccountNumber ?? 'NeoBank').replace(/\*/g, 'x');
    link.download = `Loan_Payment_Receipt_${suffix}.pdf`;
    link.click();
    URL.revokeObjectURL(url);
  }
}
