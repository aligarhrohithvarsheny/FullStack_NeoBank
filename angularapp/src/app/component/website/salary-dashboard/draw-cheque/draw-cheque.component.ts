import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChequeService } from '../../../../service/cheque.service';
import { SalaryAccountService } from '../../../../service/salary-account.service';
import { AlertService } from '../../../../service/alert.service';
import { ChequeDrawRequest, ChequeHistoryEntry, ChequeStatus, ChequeLeaf } from '../../../../model/cheque/cheque.model';
import { SalaryAccount } from '../../../../model/salary-account/salary-account.model';

@Component({
  selector: 'app-draw-cheque',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './draw-cheque.component.html',
  styleUrls: ['./draw-cheque.component.css']
})
export class DrawChequeComponent implements OnInit, OnDestroy {
  // Account and User Info
  account: SalaryAccount | null = null;
  availableBalance: number = 0;
  employee: any = null;

  // Form fields
  serialNumber: string = '';
  chequeDate: string = this.getTodayDate();
  amount: number | null = null;
  payeeName: string = '';
  remarks: string = '';

  // Cheque leaves
  availableLeaves: ChequeLeaf[] = [];
  isLoadingLeaves: boolean = false;
  totalAllocated: number = 0;
  totalUsed: number = 0;

  // Form states
  isSubmitting: boolean = false;
  formValid: boolean = false;
  showForm: boolean = true;
  successMessage: string = '';
  errorMessage: string = '';

  // Cheque history
  chequeHistory: ChequeHistoryEntry[] = [];
  isLoadingHistory: boolean = false;
  historyFilter: ChequeStatus | 'ALL' = 'ALL';
  
  // Tab state
  activeTab: 'form' | 'history' = 'form';

  // Edit state
  editingChequeId: number | null = null;
  editPayeeName: string = '';
  editAmount: number | null = null;
  isEditing: boolean = false;

  // Status colors
  statusColors: { [key in ChequeStatus]: string } = {
    'PENDING': '#f59e0b',
    'APPROVED': '#3b82f6',
    'COMPLETED': '#10b981',
    'REJECTED': '#ef4444',
    'CANCELLED': '#6b7280',
    'CLEARED': '#059669'
  };

  constructor(
    private chequeService: ChequeService,
    private salaryService: SalaryAccountService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadAccountData();
  }

  ngOnDestroy() {
    // Cleanup if needed
  }

  /**
   * Load salary account and user data
   */
  loadAccountData() {
    const sessionStr = sessionStorage.getItem('salaryEmployee');
    if (!sessionStr) {
      this.alertService.error('Error', 'Session expired. Please log in again.');
      return;
    }

    this.employee = JSON.parse(sessionStr);
    
    // Load account details
    this.salaryService.getByAccountNumber(this.employee.accountNumber).subscribe({
      next: (account) => {
        this.account = account;
        if (account.id) {
          this.availableBalance = account.balance || 0;
          this.loadChequeHistory();
          this.loadAvailableLeaves();
        }
      },
      error: (err) => {
        console.error('Error loading account:', err);
        this.alertService.error('Error', 'Failed to load account details');
      }
    });
  }

  /**
   * Load available cheque leaves for dropdown
   */
  loadAvailableLeaves() {
    if (!this.account?.id) return;
    this.isLoadingLeaves = true;
    this.chequeService.getAvailableLeaves(this.account.id).subscribe({
      next: (response) => {
        this.availableLeaves = response.leaves || [];
        this.totalAllocated = response.totalAllocated || 0;
        this.totalUsed = response.totalUsed || 0;
        this.isLoadingLeaves = false;
      },
      error: (err) => {
        console.error('Error loading cheque leaves:', err);
        this.availableLeaves = [];
        this.isLoadingLeaves = false;
      }
    });
  }

  /**
   * Load user's cheque request history
   */
  loadChequeHistory() {
    if (!this.account?.id) return;
    
    this.isLoadingHistory = true;
    this.chequeService.getUserCheques(this.account.id).subscribe({
      next: (response: any) => {
        this.chequeHistory = response.data || response.items || [];
        this.isLoadingHistory = false;
      },
      error: (err) => {
        console.error('Error loading cheque history:', err);
        this.isLoadingHistory = false;
        this.chequeHistory = [];
      }
    });
  }

  /**
   * Get filtered cheque history based on status
   */
  getFilteredHistory(): ChequeHistoryEntry[] {
    if (this.historyFilter === 'ALL') {
      return this.chequeHistory;
    }
    return this.chequeHistory.filter(c => c.status === this.historyFilter);
  }

  /**
   * Validate form before submission
   */
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

    // Validate cheque date is not too old
    const chequeDate = new Date(this.chequeDate);
    const today = new Date();
    const daysDifference = Math.floor((today.getTime() - chequeDate.getTime()) / (1000 * 60 * 60 * 24));
    
    if (daysDifference > 6 * 30) { // 6 months validity
      this.errorMessage = 'Cheque date cannot be older than 3 months';
      return false;
    }

    if (!this.amount || this.amount <= 0) {
      this.errorMessage = 'Amount must be greater than 0';
      return false;
    }

    if (this.amount > this.availableBalance) {
      this.errorMessage = `Amount cannot exceed available balance (₹${this.availableBalance.toLocaleString('en-IN', { maximumFractionDigits: 2 })})`;
      return false;
    }

    if (!this.payeeName || this.payeeName.trim() === '') {
      this.errorMessage = 'Payee name is required';
      return false;
    }

    if (this.amount > 1000000) {
      this.errorMessage = 'Amount cannot exceed ₹10,00,0000';
      return false;
    }

    return true;
  }

  /**
   * Submit cheque draw request
   */
  submitChequeRequest() {
    if (!this.validateForm()) {
      this.alertService.error('Validation Error', this.errorMessage);
      return;
    }

    if (!this.account?.id) {
      this.alertService.error('Error', 'Account information not loaded');
      return;
    }

    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';

    const request: ChequeDrawRequest = {
      serialNumber: this.serialNumber.trim(),
      chequeDate: this.chequeDate,
      amount: this.amount!,
      payeeName: this.payeeName.trim(),
      remarks: this.remarks.trim() || undefined
    };

    this.chequeService.applyCheque(this.account.id, request).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        
        if (response.success) {
          this.successMessage = `✓ Cheque request submitted successfully!
          Cheque Number: ${response.chequeNumber}
          Your request is waiting for admin approval.`;
          
          this.alertService.success(
            'Success',
            `Cheque ${response.chequeNumber} requested. Waiting for admin approval.`
          );
          
          // Reset form
          setTimeout(() => {
            this.resetForm();
            this.loadChequeHistory();
            this.loadAvailableLeaves();
            this.activeTab = 'history';
          }, 1500);
        } else {
          this.errorMessage = response.message || 'Failed to submit cheque request';
          this.alertService.error('Error', this.errorMessage);
        }
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('Error submitting cheque request:', err);
        this.errorMessage = err.error?.message || 'Failed to submit cheque request. Please try again.';
        this.alertService.error('Error', this.errorMessage);
      }
    });
  }

  /**
   * Reset form to initial state
   */
  resetForm() {
    this.serialNumber = '';
    this.chequeDate = this.getTodayDate();
    this.amount = null;
    this.payeeName = '';
    this.remarks = '';
    this.errorMessage = '';
    this.formValid = false;
  }

  /**
   * Handle form input changes for real-time validation
   */
  onFormChange() {
    this.formValid = this.serialNumber.trim() !== '' &&
                     this.chequeDate !== '' &&
                     this.amount !== null &&
                     this.amount > 0 &&
                     this.amount <= this.availableBalance &&
                     this.payeeName.trim() !== '';
  }

  /**
   * Switch active tab
   */
  switchTab(tab: 'form' | 'history') {
    this.activeTab = tab;
    if (tab === 'history') {
      this.loadChequeHistory();
    }
  }

  /**
   * Cancel a pending cheque request
   */
  cancelCheque(chequeId: number) {
    if (!confirm('Are you sure you want to cancel this cheque request?')) {
      return;
    }

    this.chequeService.cancelCheque(chequeId, 'Cancelled by user').subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Success', 'Cheque request cancelled');
          this.loadChequeHistory();
          this.loadAvailableLeaves();
        } else {
          this.alertService.error('Error', response.message || 'Failed to cancel cheque request');
        }
      },
      error: (err) => {
        console.error('Error cancelling cheque:', err);
        this.alertService.error('Error', 'Failed to cancel cheque request');
      }
    });
  }

  /**
   * Start editing a pending cheque
   */
  startEditCheque(cheque: ChequeHistoryEntry) {
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
      this.alertService.error('Error', 'Payee name is required');
      return;
    }
    if (!this.editAmount || this.editAmount <= 0) {
      this.alertService.error('Error', 'Amount must be greater than 0');
      return;
    }
    if (this.editAmount > this.availableBalance) {
      this.alertService.error('Error', 'Amount cannot exceed available balance');
      return;
    }

    this.isEditing = true;
    this.chequeService.editPendingCheque(this.editingChequeId, this.editPayeeName.trim(), this.editAmount).subscribe({
      next: (response) => {
        this.isEditing = false;
        if (response.success) {
          this.alertService.success('Success', 'Cheque details updated successfully');
          this.editingChequeId = null;
          this.editPayeeName = '';
          this.editAmount = null;
          this.loadChequeHistory();
        } else {
          this.alertService.error('Error', response.message || 'Failed to update cheque');
        }
      },
      error: (err) => {
        this.isEditing = false;
        console.error('Error editing cheque:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update cheque details');
      }
    });
  }

  /**
   * Download filled NeoBank cheque as printable PDF
   */
  downloadCheque(cheque: ChequeHistoryEntry) {
    const accountNumber = this.account?.accountNumber || '';
    const accountHolder = this.employee?.name || this.employee?.fullName || '';
    const ifscCode = 'NEOB0001234';
    const micrCode = '110002' + String(cheque.id).padStart(6, '0');
    const chequeDate = new Date(cheque.chequeDate);
    const formattedDate = `${String(chequeDate.getDate()).padStart(2, '0')}-${String(chequeDate.getMonth() + 1).padStart(2, '0')}-${chequeDate.getFullYear()}`;
    const amountInWords = this.convertAmountToWords(cheque.amount);

    const chequeHTML = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>NeoBank Cheque - ${cheque.chequeNumber}</title>
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
            background: linear-gradient(180deg, #fffef5 0%, #fefdf0 40%, #fdfce8 100%);
            border: 2px solid #1a237e;
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
            background: linear-gradient(90deg, #1a237e 0%, #4a148c 50%, #1a237e 100%);
          }
          .cheque-watermark {
            position: absolute; top: 50%; left: 50%;
            transform: translate(-50%, -50%) rotate(-30deg);
            font-size: 80px; font-weight: bold;
            color: rgba(26, 35, 126, 0.04);
            letter-spacing: 15px;
            white-space: nowrap;
            pointer-events: none;
          }
          .cheque-top {
            display: flex; justify-content: space-between; align-items: flex-start;
            margin-bottom: 12px; position: relative; z-index: 1;
          }
          .bank-info { display: flex; align-items: center; gap: 10px; }
          .bank-logo {
            width: 42px; height: 42px; background: linear-gradient(135deg, #1a237e, #4a148c);
            border-radius: 50%; display: flex; align-items: center; justify-content: center;
            color: white; font-weight: bold; font-size: 14px; font-family: Arial;
          }
          .bank-name { font-size: 20px; font-weight: bold; color: #1a237e; font-family: 'Georgia', serif; letter-spacing: 2px; }
          .bank-branch { font-size: 9px; color: #555; margin-top: 2px; }
          .cheque-number-top { text-align: right; }
          .cheque-number-top .label { font-size: 8px; color: #888; }
          .cheque-number-top .number { font-size: 14px; font-weight: bold; color: #1a237e; letter-spacing: 2px; }
          .cheque-type {
            position: absolute; top: 15px; right: 25px;
            font-size: 11px; font-weight: bold; color: #1a237e;
            border: 1.5px solid #1a237e; padding: 2px 12px;
            border-radius: 3px; letter-spacing: 3px; margin-top: 40px;
          }
          .date-section {
            text-align: right; margin-bottom: 8px; position: relative; z-index: 1;
          }
          .date-label { font-size: 9px; color: #666; }
          .date-boxes {
            display: inline-flex; gap: 3px; margin-left: 5px;
          }
          .date-box {
            width: 22px; height: 24px;
            border: 1px solid #999; text-align: center;
            font-size: 14px; font-weight: bold; color: #1a237e;
            line-height: 24px; background: rgba(255,255,255,0.5);
          }
          .date-separator { line-height: 24px; color: #666; font-weight: bold; }
          .pay-section { margin-bottom: 6px; position: relative; z-index: 1; }
          .pay-line {
            display: flex; align-items: baseline; gap: 8px;
            border-bottom: 1.5px solid #333; padding-bottom: 3px;
          }
          .pay-label { font-size: 10px; color: #666; white-space: nowrap; }
          .pay-value { font-size: 15px; font-weight: bold; color: #1a237e; flex: 1; text-transform: uppercase; }
          .bearer-text { font-size: 10px; color: #555; font-weight: bold; letter-spacing: 1px; }
          .amount-words-section { margin-bottom: 8px; position: relative; z-index: 1; }
          .amount-words-line {
            display: flex; align-items: baseline; gap: 8px;
            border-bottom: 1.5px solid #333; padding-bottom: 3px;
            min-height: 22px;
          }
          .amount-words-label { font-size: 10px; color: #666; white-space: nowrap; }
          .amount-words-value { font-size: 12px; color: #1a237e; flex: 1; text-transform: uppercase; font-weight: bold; }
          .amount-box-section {
            position: absolute; right: 25px; top: 140px;
            z-index: 1;
          }
          .amount-box {
            border: 2px solid #1a237e; padding: 5px 15px;
            background: rgba(255,255,255,0.7); border-radius: 4px;
            display: flex; align-items: center; gap: 5px;
          }
          .rupee-symbol { font-size: 18px; font-weight: bold; color: #1a237e; }
          .amount-value { font-size: 20px; font-weight: bold; color: #1a237e; letter-spacing: 1px; }
          .bottom-section {
            display: flex; justify-content: space-between; align-items: flex-end;
            margin-top: auto; position: absolute;
            bottom: 30px; left: 25px; right: 25px; z-index: 1;
          }
          .account-info { font-size: 9px; color: #666; }
          .account-info .acc-label { color: #888; }
          .account-info .acc-value { color: #1a237e; font-weight: bold; letter-spacing: 1px; }
          .signature-section { text-align: center; }
          .signature-line {
            width: 160px; border-bottom: 1.5px solid #333;
            margin-bottom: 4px; height: 35px;
            display: flex; align-items: flex-end; justify-content: center;
            font-size: 13px; color: #1a237e; font-style: italic; padding-bottom: 3px;
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
            background: linear-gradient(135deg, #1a237e, #4a148c); color: white;
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
            <div class="cheque-watermark">NEOBANK</div>
            <div class="cheque-top">
              <div class="bank-info">
                <div class="bank-logo">NB</div>
                <div>
                  <div class="bank-name">NEOBANK</div>
                  <div class="bank-branch">Digital Banking Division &bull; IFSC: ${ifscCode}</div>
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
                ${formattedDate.split('').map(c => c === '-' ? '<span class="date-separator">/</span>' : `<span class="date-box">${c}</span>`).join('')}
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
                <span class="rupee-symbol">₹</span>
                <span class="amount-value">${cheque.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
              </div>
            </div>
            <div class="bottom-section">
              <div class="account-info">
                <div><span class="acc-label">A/C No: </span><span class="acc-value">${accountNumber}</span></div>
                <div><span class="acc-label">IFSC: </span><span class="acc-value">${ifscCode}</span></div>
              </div>
              <div class="signature-section">
                <div class="signature-line">${accountHolder}</div>
                <div class="signature-label">Authorised Signatory</div>
              </div>
            </div>
            <div class="micr-line">
              ⑆${micrCode}⑆ &nbsp; ⑈${accountNumber}⑈ &nbsp; ⑇${cheque.chequeNumber.replace('CHQ', '')}⑇
            </div>
          </div>
          <button class="print-btn" onclick="window.print()">
            🖨️ Print / Save as PDF
          </button>
        </div>
      </body>
      </html>
    `;

    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(chequeHTML);
      printWindow.document.close();
    }

    // Mark as downloaded in backend
    this.chequeService.markChequeDownloaded(cheque.id).subscribe({
      next: () => {
        cheque.chequeDownloaded = true;
        this.alertService.success('Success', 'Cheque downloaded successfully');
        this.loadChequeHistory();
      },
      error: (err) => {
        console.error('Error marking cheque as downloaded:', err);
      }
    });
  }

  /**
   * Convert number to Indian currency words
   */
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

  /**
   * Get status badge color
   */
  getStatusColor(status: ChequeStatus): string {
    return this.statusColors[status] || '#6b7280';
  }

  /**
   * Get human-readable status text
   */
  getStatusText(status: ChequeStatus): string {
    const statusMap: { [key in ChequeStatus]: string } = {
      'PENDING': 'Awaiting Approval',
      'APPROVED': 'Approved',
      'COMPLETED': 'Processed',
      'REJECTED': 'Rejected',
      'CANCELLED': 'Cancelled',
      'CLEARED': 'Cleared'
    };
    return statusMap[status] || status;
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  /**
   * Get today's date in YYYY-MM-DD format
   */
  getTodayDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Get max date (today)
   */
  getMaxDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Format amount in Indian rupees
   */
  formatAmount(amount: number): string {
    return amount.toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }
}
