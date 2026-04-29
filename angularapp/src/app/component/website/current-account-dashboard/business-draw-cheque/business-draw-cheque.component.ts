import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BusinessChequeService } from '../../../../service/business-cheque.service';
import {
  BusinessChequeDrawRequest,
  BusinessChequeHistoryEntry,
  BusinessChequeStatus,
  BusinessChequeLeaf
} from '../../../../model/cheque/business-cheque.model';
import { CurrentAccount } from '../../../../model/current-account/current-account.model';

@Component({
  selector: 'app-business-draw-cheque',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './business-draw-cheque.component.html',
  styleUrls: ['./business-draw-cheque.component.css']
})
export class BusinessDrawChequeComponent implements OnInit {
  // Account Info
  account: CurrentAccount | null = null;
  availableBalance: number = 0;

  // Form fields
  serialNumber: string = '';
  chequeDate: string = this.getTodayDate();
  amount: number | null = null;
  payeeName: string = '';
  remarks: string = '';

  // Cheque leaves
  availableLeaves: BusinessChequeLeaf[] = [];
  isLoadingLeaves: boolean = false;
  totalAllocated: number = 0;
  totalUsed: number = 0;

  // Form states
  isSubmitting: boolean = false;
  formValid: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  // Cheque history
  chequeHistory: BusinessChequeHistoryEntry[] = [];
  isLoadingHistory: boolean = false;
  historyFilter: BusinessChequeStatus | 'ALL' = 'ALL';

  // Tab state
  activeTab: 'form' | 'history' = 'form';

  // Edit state
  editingChequeId: number | null = null;
  editPayeeName: string = '';
  editAmount: number | null = null;
  isEditing: boolean = false;

  // Status colors
  statusColors: { [key in BusinessChequeStatus]: string } = {
    'PENDING': '#f59e0b',
    'APPROVED': '#3b82f6',
    'COMPLETED': '#10b981',
    'REJECTED': '#ef4444',
    'CANCELLED': '#6b7280',
    'CLEARED': '#059669'
  };

  constructor(private businessChequeService: BusinessChequeService) {}

  ngOnInit() {
    this.loadAccountData();
  }

  loadAccountData() {
    const sessionStr = sessionStorage.getItem('currentAccount');
    if (!sessionStr) {
      this.errorMessage = 'Session expired. Please log in again.';
      return;
    }

    this.account = JSON.parse(sessionStr);
    if (this.account && this.account.id) {
      this.availableBalance = this.account.balance || 0;
      this.loadChequeHistory();
      this.loadAvailableLeaves();
    }
  }

  loadAvailableLeaves() {
    if (!this.account?.id) return;
    this.isLoadingLeaves = true;
    this.businessChequeService.getAvailableLeaves(this.account.id).subscribe({
      next: (response) => {
        this.availableLeaves = response.leaves || [];
        this.totalAllocated = response.totalAllocated || 0;
        this.totalUsed = response.totalUsed || 0;
        this.isLoadingLeaves = false;
      },
      error: () => {
        this.availableLeaves = [];
        this.isLoadingLeaves = false;
      }
    });
  }

  loadChequeHistory() {
    if (!this.account?.id) return;
    this.isLoadingHistory = true;
    this.businessChequeService.getUserCheques(this.account.id).subscribe({
      next: (response: any) => {
        this.chequeHistory = response.items || response.data || [];
        this.isLoadingHistory = false;
      },
      error: () => {
        this.isLoadingHistory = false;
        this.chequeHistory = [];
      }
    });
  }

  getFilteredHistory(): BusinessChequeHistoryEntry[] {
    if (this.historyFilter === 'ALL') {
      return this.chequeHistory;
    }
    return this.chequeHistory.filter(c => c.status === this.historyFilter);
  }

  validateForm(): boolean {
    this.errorMessage = '';

    if (!this.serialNumber || this.serialNumber.trim() === '') {
      this.errorMessage = 'Serial number is required';
      return false;
    }

    if (!this.chequeDate) {
      this.errorMessage = 'Cheque date is required';
      return false;
    }

    const chequeDate = new Date(this.chequeDate);
    const today = new Date();
    const daysDifference = Math.floor((today.getTime() - chequeDate.getTime()) / (1000 * 60 * 60 * 24));

    if (daysDifference > 6 * 30) {
      this.errorMessage = 'Cheque date cannot be older than 6 months';
      return false;
    }

    if (!this.amount || this.amount <= 0) {
      this.errorMessage = 'Amount must be greater than 0';
      return false;
    }

    if (this.amount > this.availableBalance) {
      this.errorMessage = `Amount cannot exceed available balance (${this.formatAmount(this.availableBalance)})`;
      return false;
    }

    if (!this.payeeName || this.payeeName.trim() === '') {
      this.errorMessage = 'Payee name is required';
      return false;
    }

    if (this.amount > 5000000) {
      this.errorMessage = 'Amount cannot exceed ₹50,00,000';
      return false;
    }

    return true;
  }

  submitChequeRequest() {
    if (!this.validateForm()) return;

    if (!this.account?.id) {
      this.errorMessage = 'Account information not loaded';
      return;
    }

    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';

    const request: BusinessChequeDrawRequest = {
      serialNumber: this.serialNumber.trim(),
      chequeDate: this.chequeDate,
      amount: this.amount!,
      payeeName: this.payeeName.trim(),
      remarks: this.remarks.trim() || undefined
    };

    this.businessChequeService.applyCheque(this.account.id, request).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        if (response.success) {
          this.successMessage = `Cheque request submitted successfully! Cheque Number: ${response.chequeNumber}. Waiting for admin approval.`;
          setTimeout(() => {
            this.resetForm();
            this.loadChequeHistory();
            this.loadAvailableLeaves();
            this.activeTab = 'history';
          }, 1500);
        } else {
          this.errorMessage = response.message || 'Failed to submit cheque request';
        }
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Failed to submit cheque request. Please try again.';
      }
    });
  }

  resetForm() {
    this.serialNumber = '';
    this.chequeDate = this.getTodayDate();
    this.amount = null;
    this.payeeName = '';
    this.remarks = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.formValid = false;
  }

  onFormChange() {
    this.formValid = this.serialNumber.trim() !== '' &&
                     this.chequeDate !== '' &&
                     this.amount !== null &&
                     this.amount > 0 &&
                     this.amount <= this.availableBalance &&
                     this.payeeName.trim() !== '';
  }

  switchTab(tab: 'form' | 'history') {
    this.activeTab = tab;
    if (tab === 'history') {
      this.loadChequeHistory();
    }
  }

  cancelCheque(chequeId: number) {
    if (!confirm('Are you sure you want to cancel this cheque request?')) return;

    this.businessChequeService.cancelCheque(chequeId, 'Cancelled by user').subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Cheque request cancelled';
          this.loadChequeHistory();
          this.loadAvailableLeaves();
        } else {
          this.errorMessage = response.message || 'Failed to cancel cheque request';
        }
      },
      error: () => {
        this.errorMessage = 'Failed to cancel cheque request';
      }
    });
  }

  /**
   * Start editing a pending cheque
   */
  startEditCheque(cheque: BusinessChequeHistoryEntry) {
    this.editingChequeId = cheque.id;
    this.editPayeeName = cheque.payeeName;
    this.editAmount = cheque.amount;
  }

  /**
   * Cancel editing
   */
  cancelEditCheque() {
    this.editingChequeId = null;
    this.editPayeeName = '';
    this.editAmount = null;
  }

  /**
   * Save edited cheque details
   */
  saveEditCheque() {
    if (!this.editingChequeId) return;
    if (!this.editPayeeName || this.editPayeeName.trim() === '') {
      this.errorMessage = 'Payee name is required';
      return;
    }
    if (!this.editAmount || this.editAmount <= 0) {
      this.errorMessage = 'Amount must be greater than 0';
      return;
    }
    if (this.editAmount > this.availableBalance) {
      this.errorMessage = 'Amount cannot exceed available balance';
      return;
    }

    this.isEditing = true;
    this.businessChequeService.editPendingCheque(this.editingChequeId, this.editPayeeName.trim(), this.editAmount).subscribe({
      next: (response) => {
        this.isEditing = false;
        if (response.success) {
          this.successMessage = 'Cheque details updated successfully';
          this.editingChequeId = null;
          this.editPayeeName = '';
          this.editAmount = null;
          this.loadChequeHistory();
        } else {
          this.errorMessage = response.message || 'Failed to update cheque';
        }
      },
      error: (err) => {
        this.isEditing = false;
        this.errorMessage = err.error?.message || 'Failed to update cheque details';
      }
    });
  }

  downloadCheque(cheque: BusinessChequeHistoryEntry) {
    const accountNumber = this.account?.accountNumber || '';
    const accountHolder = this.account?.ownerName || this.account?.businessName || '';
    const businessName = this.account?.businessName || '';
    const ifscCode = 'NEOB0001234';
    const micrCode = '110002' + String(cheque.id).padStart(6, '0');
    const chequeDate = new Date(cheque.chequeDate);
    const formattedDate = `${String(chequeDate.getDate()).padStart(2, '0')}-${String(chequeDate.getMonth() + 1).padStart(2, '0')}-${chequeDate.getFullYear()}`;
    const amountInWords = this.convertAmountToWords(cheque.amount);

    const chequeHTML = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>NeoBank Business Cheque - ${cheque.chequeNumber}</title>
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          @page { size: 8.5in 3.75in landscape; margin: 0; }
          body {
            font-family: 'Courier New', monospace;
            background: #f5f5f5;
            display: flex; justify-content: center; align-items: center;
            min-height: 100vh; padding: 20px;
          }
          .cheque-container {
            width: 8in; height: 3.5in;
            background: linear-gradient(180deg, #f0fdf4 0%, #ecfdf5 40%, #d1fae5 100%);
            border: 2px solid #065f46;
            border-radius: 8px;
            position: relative;
            padding: 15px 25px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.15);
            overflow: hidden;
          }
          .cheque-container::before {
            content: '';
            position: absolute; top: 0; left: 0; right: 0;
            height: 6px;
            background: linear-gradient(90deg, #065f46 0%, #047857 50%, #065f46 100%);
          }
          .cheque-watermark {
            position: absolute; top: 50%; left: 50%;
            transform: translate(-50%, -50%) rotate(-30deg);
            font-size: 70px; font-weight: bold;
            color: rgba(6, 95, 70, 0.04);
            letter-spacing: 10px;
            white-space: nowrap;
            pointer-events: none;
          }
          .cheque-top {
            display: flex; justify-content: space-between; align-items: flex-start;
            margin-bottom: 12px; position: relative; z-index: 1;
          }
          .bank-info { display: flex; align-items: center; gap: 10px; }
          .bank-logo {
            width: 42px; height: 42px; background: linear-gradient(135deg, #065f46, #047857);
            border-radius: 50%; display: flex; align-items: center; justify-content: center;
            color: white; font-weight: bold; font-size: 14px; font-family: Arial;
          }
          .bank-name { font-size: 20px; font-weight: bold; color: #065f46; font-family: 'Georgia', serif; letter-spacing: 2px; }
          .bank-branch { font-size: 9px; color: #555; margin-top: 2px; }
          .business-label { font-size: 8px; color: #047857; font-weight: bold; letter-spacing: 2px; margin-top: 2px; }
          .cheque-number-top { text-align: right; }
          .cheque-number-top .label { font-size: 8px; color: #888; }
          .cheque-number-top .number { font-size: 14px; font-weight: bold; color: #065f46; letter-spacing: 2px; }
          .cheque-type {
            position: absolute; top: 15px; right: 25px;
            font-size: 11px; font-weight: bold; color: #065f46;
            border: 1.5px solid #065f46; padding: 2px 12px;
            border-radius: 3px; letter-spacing: 3px; margin-top: 40px;
          }
          .date-section {
            text-align: right; margin-bottom: 8px; position: relative; z-index: 1;
          }
          .date-label { font-size: 9px; color: #666; }
          .date-boxes { display: inline-flex; gap: 3px; margin-left: 5px; }
          .date-box {
            width: 22px; height: 24px;
            border: 1px solid #999; text-align: center;
            font-size: 14px; font-weight: bold; color: #065f46;
            line-height: 24px; background: rgba(255,255,255,0.5);
          }
          .date-separator { line-height: 24px; color: #666; font-weight: bold; }
          .pay-section { margin-bottom: 6px; position: relative; z-index: 1; }
          .pay-line {
            display: flex; align-items: baseline; gap: 8px;
            border-bottom: 1.5px solid #333; padding-bottom: 3px;
          }
          .pay-label { font-size: 10px; color: #666; white-space: nowrap; }
          .pay-value { font-size: 15px; font-weight: bold; color: #065f46; flex: 1; text-transform: uppercase; }
          .bearer-text { font-size: 10px; color: #555; font-weight: bold; letter-spacing: 1px; }
          .amount-words-section { margin-bottom: 8px; position: relative; z-index: 1; }
          .amount-words-line {
            display: flex; align-items: baseline; gap: 8px;
            border-bottom: 1.5px solid #333; padding-bottom: 3px;
            min-height: 22px;
          }
          .amount-words-label { font-size: 10px; color: #666; white-space: nowrap; }
          .amount-words-value { font-size: 12px; color: #065f46; flex: 1; text-transform: uppercase; font-weight: bold; }
          .amount-box-section {
            position: absolute; right: 25px; top: 140px;
            z-index: 1;
          }
          .amount-box {
            border: 2px solid #065f46; padding: 5px 15px;
            background: rgba(255,255,255,0.7); border-radius: 4px;
            display: flex; align-items: center; gap: 5px;
          }
          .rupee-symbol { font-size: 18px; font-weight: bold; color: #065f46; }
          .amount-value { font-size: 20px; font-weight: bold; color: #065f46; letter-spacing: 1px; }
          .bottom-section {
            display: flex; justify-content: space-between; align-items: flex-end;
            margin-top: auto; position: absolute;
            bottom: 30px; left: 25px; right: 25px; z-index: 1;
          }
          .account-info { font-size: 9px; color: #666; }
          .account-info .acc-label { color: #888; }
          .account-info .acc-value { color: #065f46; font-weight: bold; letter-spacing: 1px; }
          .signature-section { text-align: center; }
          .signature-line {
            width: 160px; border-bottom: 1.5px solid #333;
            margin-bottom: 4px; height: 35px;
            display: flex; align-items: flex-end; justify-content: center;
            font-size: 13px; color: #065f46; font-style: italic; padding-bottom: 3px;
          }
          .signature-label { font-size: 8px; color: #888; }
          .micr-line {
            position: absolute; bottom: 8px; left: 25px; right: 25px;
            font-family: 'MICR', 'Courier New', monospace;
            font-size: 12px; letter-spacing: 4px; color: #333;
            border-top: 1px dashed #ccc; padding-top: 4px;
          }
          .print-btn {
            display: block; margin: 20px auto; padding: 12px 40px;
            background: linear-gradient(135deg, #065f46, #047857); color: white;
            border: none; border-radius: 8px; font-size: 16px;
            cursor: pointer; font-weight: bold; letter-spacing: 1px;
          }
          .print-btn:hover { opacity: 0.9; }
          @media print {
            body { background: white; padding: 0; min-height: auto; }
            .print-btn { display: none; }
            .cheque-container { box-shadow: none; border-radius: 0; }
          }
        </style>
      </head>
      <body>
        <div>
          <div class="cheque-container">
            <div class="cheque-watermark">NEOBANK BUSINESS</div>
            <div class="cheque-top">
              <div class="bank-info">
                <div class="bank-logo">NB</div>
                <div>
                  <div class="bank-name">NEOBANK</div>
                  <div class="bank-branch">Business Banking Division &bull; IFSC: ${ifscCode}</div>
                  <div class="business-label">CURRENT ACCOUNT</div>
                </div>
              </div>
              <div class="cheque-number-top">
                <div class="label">CHEQUE NO.</div>
                <div class="number">${cheque.chequeNumber}</div>
              </div>
            </div>
            <div class="cheque-type">BEARER</div>
            <div class="date-section">
              <span class="date-label">Date:</span>
              <span class="date-boxes">
                ${formattedDate.split('').map((c: string) => c === '-' ? '<span class="date-separator">/</span>' : `<span class="date-box">${c}</span>`).join('')}
              </span>
            </div>
            <div class="pay-section">
              <div class="pay-line">
                <span class="pay-label">Pay</span>
                <span class="pay-value">${cheque.payeeName}</span>
                <span class="bearer-text">OR BEARER</span>
              </div>
            </div>
            <div class="amount-words-section">
              <div class="amount-words-line">
                <span class="amount-words-label">Rupees</span>
                <span class="amount-words-value">${amountInWords} Only</span>
              </div>
            </div>
            <div class="amount-box-section">
              <div class="amount-box">
                <span class="rupee-symbol">&#8377;</span>
                <span class="amount-value">${cheque.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
              </div>
            </div>
            <div class="bottom-section">
              <div class="account-info">
                <div><span class="acc-label">A/C No: </span><span class="acc-value">${accountNumber}</span></div>
                <div><span class="acc-label">Business: </span><span class="acc-value">${businessName}</span></div>
                <div><span class="acc-label">IFSC: </span><span class="acc-value">${ifscCode}</span></div>
              </div>
              <div class="signature-section">
                <div class="signature-line">${accountHolder}</div>
                <div class="signature-label">Authorised Signatory</div>
              </div>
            </div>
            <div class="micr-line">
              &#9286;${micrCode}&#9286; &nbsp; &#9288;${accountNumber}&#9288; &nbsp; &#9287;${cheque.chequeNumber.replace('BCHQ', '')}&#9287;
            </div>
          </div>
          <button class="print-btn" onclick="window.print()">Print / Save as PDF</button>
        </div>
      </body>
      </html>
    `;

    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(chequeHTML);
      printWindow.document.close();
    }

    this.businessChequeService.markChequeDownloaded(cheque.id).subscribe({
      next: () => {
        cheque.chequeDownloaded = true;
        this.loadChequeHistory();
      },
      error: () => {}
    });
  }

  convertAmountToWords(amount: number): string {
    const ones = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine',
      'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen',
      'Seventeen', 'Eighteen', 'Nineteen'];
    const tens = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

    if (amount === 0) return 'Zero';

    const crore = Math.floor(amount / 10000000);
    const lakh = Math.floor((amount % 10000000) / 100000);
    const thousand = Math.floor((amount % 100000) / 1000);
    const hundred = Math.floor((amount % 1000) / 100);
    const remainder = Math.floor(amount % 100);

    const twoDigitWords = (n: number): string => {
      if (n < 20) return ones[n];
      return tens[Math.floor(n / 10)] + (n % 10 ? ' ' + ones[n % 10] : '');
    };

    let words = '';
    if (crore) words += twoDigitWords(crore) + ' Crore ';
    if (lakh) words += twoDigitWords(lakh) + ' Lakh ';
    if (thousand) words += twoDigitWords(thousand) + ' Thousand ';
    if (hundred) words += ones[hundred] + ' Hundred ';
    if (remainder) words += (words ? 'and ' : '') + twoDigitWords(remainder);

    const paise = Math.round((amount % 1) * 100);
    let result = 'Rupees ' + words.trim();
    if (paise > 0) result += ' and ' + twoDigitWords(paise) + ' Paise';

    return result;
  }

  getStatusColor(status: BusinessChequeStatus): string {
    return this.statusColors[status] || '#6b7280';
  }

  getStatusText(status: BusinessChequeStatus): string {
    const statusMap: { [key in BusinessChequeStatus]: string } = {
      'PENDING': 'Awaiting Approval',
      'APPROVED': 'Approved',
      'COMPLETED': 'Processed',
      'REJECTED': 'Rejected',
      'CANCELLED': 'Cancelled',
      'CLEARED': 'Cleared'
    };
    return statusMap[status] || status;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  getTodayDate(): string {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  }

  getMaxDate(): string {
    return this.getTodayDate();
  }

  formatAmount(amount: number): string {
    return amount.toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }
}
