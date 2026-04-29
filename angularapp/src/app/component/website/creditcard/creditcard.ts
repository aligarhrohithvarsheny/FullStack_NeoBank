import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

interface CreditCardDetails {
  id: number;
  cardNumber: string;
  cvv: string;
  expiryDate: string;
  pin: string;
  pinSet: boolean;
  status: string;
  accountNumber: string;
  userName: string;
  userEmail: string;
  approvedLimit: number;
  currentBalance: number;
  availableLimit: number;
  usageLimit: number;
  overdueAmount: number;
  fine: number;
  penalty: number;
  appliedDate: string;
  approvalDate: string;
  closureDate?: string;
  lastPaidDate?: string;
}

interface UserProfile {
  name: string;
  email: string;
  accountNumber: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  accountType: string;
  joinDate: string;
  pan?: string;
  aadhar?: string;
  occupation?: string;
  income?: number;
}

interface CreditCardTransaction {
  id: number;
  transactionType: string;
  amount: number;
  merchant: string;
  description: string;
  transactionDate: string;
  balanceAfter: number;
  status: string;
}

interface CreditCardBill {
  id: number;
  billGenerationDate: string;
  dueDate: string;
  paidDate?: string;
  totalAmount: number;
  minimumDue: number;
  paidAmount: number;
  overdueAmount: number;
  fine: number;
  penalty: number;
  status: string;
  billingPeriod: string;
}

@Component({
  selector: 'app-creditcard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './creditcard.html',
  styleUrls: ['./creditcard.css'],
  encapsulation: ViewEncapsulation.None
})
export class CreditCard implements OnInit {
  showDetails: boolean = false;
  showCVV: boolean = false;
  userProfile: UserProfile | null = null;
  creditCards: CreditCardDetails[] = [];
  selectedCard: CreditCardDetails | null = null;
  isCardFlipped: boolean = false;
  showPinForm: boolean = false;
  pinForm = {
    newPin: '',
    confirmPin: ''
  };
  showBillPaymentModal: boolean = false;
  selectedBill: CreditCardBill | null = null;
  billPaymentAmount: number = 0;
  transactions: CreditCardTransaction[] = [];
  bills: CreditCardBill[] = [];
  isLoading: boolean = false;
  showApplyForm: boolean = false;
  panInput: string = '';
  incomeInput: number | null = null;
  showTermsModal: boolean = false;
  predictedCibil: number | null = null;
  suggestedLimit: number | null = null;
  panBasedLimit: number | null = null;
  cardType: string | null = null;
  eligibility: string | null = null;
  interestRate: number | null = null;
  annualFee: number | null = null;
  showLimitModal: boolean = false;
  limitChangeAmount: number = 0;
  // CIBIL Report Lookup
  cibilReportData: any = null;
  isLoadingCibilReport: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserProfile();
      this.loadCreditCards();
    }
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.http.get(`${environment.apiBaseUrl}/api/users/${user.id}`).subscribe({
        next: (userData: any) => {
          this.userProfile = {
            name: userData.account?.name || userData.username,
            email: userData.email,
            accountNumber: userData.accountNumber,
            phoneNumber: userData.account?.phone || '',
            address: userData.account?.address || '',
            dateOfBirth: userData.account?.dateOfBirth || '',
            accountType: userData.account?.accountType || 'Savings',
            joinDate: userData.account?.joinDate || '',
            pan: userData.account?.pan || '',
            aadhar: userData.account?.aadhar || '',
            occupation: userData.account?.occupation || '',
            income: userData.account?.income || 0
          };
        },
        error: (err: any) => {
          console.error('Error loading user profile:', err);
        }
      });
    }
  }

  loadCreditCards() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const currentUser = sessionStorage.getItem('currentUser');
    if (!currentUser) return;
    
    const user = JSON.parse(currentUser);
    this.isLoading = true;
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/account/${user.accountNumber}`).subscribe({
      next: (cards: any) => {
        this.creditCards = Array.isArray(cards) ? cards : [];
        if (this.creditCards.length > 0 && !this.selectedCard) {
          this.selectedCard = this.creditCards[0];
          this.loadCardDetails();
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error loading credit cards:', err);
        this.creditCards = [];
        this.isLoading = false;
      }
    });
  }

  loadCardDetails() {
    if (!this.selectedCard) return;
    this.loadTransactions();
    this.loadBills();
  }

  loadTransactions() {
    if (!this.selectedCard) return;
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/transactions`).subscribe({
      next: (transactions: any) => {
        this.transactions = Array.isArray(transactions) ? transactions : [];
      },
      error: (err: any) => {
        console.error('Error loading transactions:', err);
        this.transactions = [];
      }
    });
  }

  loadBills() {
    if (!this.selectedCard) return;
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/bills`).subscribe({
      next: (bills: any) => {
        this.bills = Array.isArray(bills) ? bills : [];
      },
      error: (err: any) => {
        console.error('Error loading bills:', err);
        this.bills = [];
      }
    });
  }

  get fullNumber(): string {
    if (!this.selectedCard) return '';
    return this.formatCardNumber(this.selectedCard.cardNumber);
  }

  get maskedCVV(): string {
    return this.showCVV ? (this.selectedCard?.cvv || '') : '***';
  }

  get statusClass(): string {
    if (!this.selectedCard) return '';
    return this.selectedCard.status.toLowerCase();
  }

  formatCardNumber(number: string): string {
    if (!number) return '';
    return number.replace(/(\d{4})(?=\d)/g, '$1 ');
  }

  flipCard() {
    this.isCardFlipped = !this.isCardFlipped;
  }

  toggleCVV() {
    this.showCVV = !this.showCVV;
  }

  toggleDetails() {
    this.showDetails = !this.showDetails;
  }

  setPin() {
    if (!this.selectedCard) return;
    if (this.pinForm.newPin !== this.pinForm.confirmPin) {
      this.alertService.validationError('PINs do not match');
      return;
    }
    if (this.pinForm.newPin.length !== 4) {
      this.alertService.validationError('PIN must be 4 digits');
      return;
    }
    
    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/set-pin`, { pin: this.pinForm.newPin }).subscribe({
      next: () => {
        this.alertService.success('PIN Set', 'Credit card PIN has been set successfully');
        this.showPinForm = false;
        this.pinForm = { newPin: '', confirmPin: '' };
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error setting PIN:', err);
        this.alertService.operationError('Set PIN', err?.message || 'Failed to set PIN');
      }
    });
  }

  openBillPayment(bill: CreditCardBill) {
    this.selectedBill = bill;
    this.billPaymentAmount = bill.minimumDue || bill.totalAmount;
    this.showBillPaymentModal = true;
  }

  payBill() {
    if (!this.selectedBill || !this.billPaymentAmount) {
      this.alertService.validationError('Please enter payment amount');
      return;
    }
    
    this.http.post(`${environment.apiBaseUrl}/api/credit-cards/bills/${this.selectedBill.id}/pay?amount=${this.billPaymentAmount}`, {}).subscribe({
      next: (bill: any) => {
        this.alertService.success('Bill Paid', 'Bill payment processed successfully');
        this.showBillPaymentModal = false;
        this.selectedBill = null;
        this.billPaymentAmount = 0;
        this.loadBills();
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error paying bill:', err);
        this.alertService.operationError('Pay Bill', err?.message || 'Failed to pay bill');
      }
    });
  }

  getStatement() {
    if (!this.selectedCard) return;
    
    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    // Format dates as ISO strings without timezone issues
    const startDateStr = startDate.toISOString().split('T')[0] + 'T00:00:00';
    const endDateStr = endDate.toISOString().split('T')[0] + 'T23:59:59';
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}/statement?startDate=${startDateStr}&endDate=${endDateStr}`).subscribe({
      next: (transactions: any) => {
        if (Array.isArray(transactions) && transactions.length > 0) {
          // Create a downloadable statement
          let statementText = `Credit Card Statement\n`;
          statementText += `Card: ${this.selectedCard?.cardNumber}\n`;
          statementText += `Period: ${startDate.toLocaleDateString()} to ${endDate.toLocaleDateString()}\n\n`;
          statementText += `Total Transactions: ${transactions.length}\n\n`;
          
          transactions.forEach((t: any) => {
            statementText += `${new Date(t.transactionDate).toLocaleDateString()} - ${t.transactionType} - ₹${t.amount} - ${t.merchant || t.description}\n`;
          });
          
          // Create and download file
          const blob = new Blob([statementText], { type: 'text/plain' });
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `credit-card-statement-${Date.now()}.txt`;
          a.click();
          window.URL.revokeObjectURL(url);
          
          this.alertService.success('Statement Generated', `Statement downloaded with ${transactions.length} transactions`);
        } else {
          this.alertService.info('No Transactions', 'No transactions found for the selected period');
        }
      },
      error: (err: any) => {
        console.error('Error getting statement:', err);
        this.alertService.operationError('Get Statement', err?.message || 'Failed to get statement');
      }
    });
  }

  openApplyForm() {
    this.showApplyForm = true;
  }

  closeApplyForm() {
    this.showApplyForm = false;
    this.panInput = '';
    this.incomeInput = null;
    this.showTermsModal = false;
    this.predictedCibil = null;
    this.suggestedLimit = null;
    this.panBasedLimit = null;
    this.cardType = null;
    this.eligibility = null;
    this.interestRate = null;
    this.annualFee = null;
    this.cibilReportData = null;
  }

  previewCreditPrediction() {
    if (!this.panInput) {
      this.alertService.validationError('PAN is required');
      return;
    }
    
    // Validate PAN format (10 characters: 5 letters, 4 digits, 1 letter)
    if (this.panInput.length !== 10) {
      this.alertService.validationError('PAN must be 10 characters long');
      return;
    }
    
    // Fetch credit prediction
    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/preview?pan=${this.panInput}&income=${this.incomeInput || 0}`, {}).subscribe({
      next: (res: any) => {
        this.predictedCibil = res.predictedCibil;
        this.suggestedLimit = res.suggestedLimit;
        this.panBasedLimit = res.panBasedLimit;
        this.cardType = res.cardType;
        this.eligibility = res.eligibility;
        this.interestRate = res.interestRate;
        this.annualFee = res.annualFee;
        this.showTermsModal = true;
      },
      error: (err: any) => {
        console.error('Error previewing credit prediction:', err);
        this.alertService.operationError('Credit prediction', err?.message || 'Preview failed');
      }
    });

    // Also lookup CIBIL report from manager-uploaded data
    this.isLoadingCibilReport = true;
    this.cibilReportData = null;
    this.http.get<any>(`${environment.apiBaseUrl}/api/cibil-reports/lookup/pan/${this.panInput}`).subscribe({
      next: (res: any) => {
        this.isLoadingCibilReport = false;
        if (res.found) {
          this.cibilReportData = res;
        }
      },
      error: () => {
        this.isLoadingCibilReport = false;
      }
    });
  }

  submitCreditApplication() {
    if (!this.panInput || !this.userProfile) {
      this.alertService.validationError('PAN is required');
      return;
    }
    
    const payload: any = {
      accountNumber: this.userProfile.accountNumber,
      userName: this.userProfile.name,
      userEmail: this.userProfile.email,
      pan: this.panInput
    };
    
    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/create?income=${this.incomeInput || 0}`, payload).subscribe({
      next: (res: any) => {
        this.alertService.success('Application Submitted', 'Your credit card application has been submitted for review.');
        this.closeApplyForm();
      },
      error: (err: any) => {
        console.error('Error submitting credit application:', err);
        this.alertService.operationError('Credit application', err?.message || 'Submission failed');
      }
    });
  }

  goBack() {
    this.router.navigate(['/userdashboard']);
  }

  refreshCardData() {
    this.loadCreditCards();
  }

  blockCard() {
    if (!this.selectedCard) return;
    
    if (!confirm('Are you sure you want to block this credit card? This action will prevent all transactions.')) {
      return;
    }

    const updateData = {
      status: 'Blocked'
    };

    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}`, updateData).subscribe({
      next: (updatedCard: any) => {
        this.alertService.success('Card Blocked', 'Your credit card has been blocked successfully');
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error blocking card:', err);
        this.alertService.operationError('Block Card', err?.message || 'Failed to block card');
      }
    });
  }

  toggleCardStatus() {
    if (!this.selectedCard) return;
    
    const newStatus = this.selectedCard.status === 'Active' ? 'Deactivated' : 'Active';
    const action = newStatus === 'Active' ? 'activate' : 'deactivate';
    
    if (!confirm(`Are you sure you want to ${action} this credit card?`)) {
      return;
    }

    const updateData = {
      status: newStatus
    };

    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}`, updateData).subscribe({
      next: (updatedCard: any) => {
        this.alertService.success('Status Updated', `Your credit card has been ${action}d successfully`);
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error toggling card status:', err);
        this.alertService.operationError('Toggle Status', err?.message || 'Failed to update card status');
      }
    });
  }

  changeLimit(action: 'increase' | 'decrease') {
    if (!this.selectedCard || !this.limitChangeAmount || this.limitChangeAmount <= 0) {
      this.alertService.validationError('Please enter a valid amount');
      return;
    }

    const currentLimit = this.selectedCard.approvedLimit;
    let newLimit: number;

    if (action === 'increase') {
      newLimit = currentLimit + this.limitChangeAmount;
    } else {
      if (this.limitChangeAmount >= currentLimit) {
        this.alertService.validationError('Decrease amount cannot be greater than or equal to current limit');
        return;
      }
      newLimit = currentLimit - this.limitChangeAmount;
      if (newLimit < 0) {
        this.alertService.validationError('New limit cannot be negative');
        return;
      }
    }

    if (!confirm(`Are you sure you want to ${action} your credit limit from ₹${currentLimit.toFixed(2)} to ₹${newLimit.toFixed(2)}?`)) {
      return;
    }

    const updateData = {
      approvedLimit: newLimit
    };

    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCard.id}`, updateData).subscribe({
      next: (updatedCard: any) => {
        this.alertService.success('Limit Updated', `Credit limit ${action === 'increase' ? 'increased' : 'decreased'} successfully from ₹${currentLimit.toFixed(2)} to ₹${newLimit.toFixed(2)}`);
        this.showLimitModal = false;
        this.limitChangeAmount = 0;
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error updating limit:', err);
        this.alertService.operationError('Update Limit', err?.message || 'Failed to update credit limit');
      }
    });
  }
}
