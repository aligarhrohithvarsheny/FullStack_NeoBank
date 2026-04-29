import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-billpayment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './billpayment.html',
  styleUrls: ['./billpayment.css']
})
export class BillPayment implements OnInit {
  userAccountNumber: string = '';
  userName: string = '';
  userEmail: string = '';
  
  // Bill payment form
  billForm = {
    billType: 'Mobile Bill',
    networkProvider: '',
    customerNumber: '',
    amount: 0
  };

  // Credit card form
  cardForm = {
    creditCardId: null as number | null,
    cardNumber: '',
    cvv: '',
    expiryDate: '',
    cardholderName: ''
  };

  // OTP payment flow
  showOtpPage: boolean = false;
  otpData: any = null;
  otp: string = '';
  isVerifyingOtp: boolean = false;
  otpTimer: number = 180; // 3 minutes in seconds
  otpTimerInterval: any = null;

  // Available credit cards
  creditCards: any[] = [];
  selectedCreditCard: any = null;
  isLoadingCards: boolean = false;
  
  // Payment processing
  isProcessing: boolean = false;
  
  // Payment history
  paymentHistory: any[] = [];
  isLoadingHistory: boolean = false;

  // Network providers
  mobileProviders = ['Airtel', 'Jio', 'BSNL', 'Vodafone Idea', 'MTNL'];
  wifiProviders = ['Airtel', 'Jio Fiber', 'BSNL', 'ACT Fibernet', 'Hathway', 'Tikona'];

  constructor(
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadUserInfo();
    this.loadCreditCards();
    this.loadPaymentHistory();
  }

  loadUserInfo() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        this.userAccountNumber = user.accountNumber || '';
        this.userName = user.name || user.username || '';
        this.userEmail = user.email || '';
      } catch (e) {
        console.error('Error parsing user session:', e);
      }
    }
  }

  loadCreditCards() {
    if (!this.userAccountNumber) return;
    
    this.isLoadingCards = true;
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/account/${this.userAccountNumber}`).subscribe({
      next: (cards: any) => {
        this.creditCards = Array.isArray(cards) ? cards.filter((c: any) => c.status === 'Active') : [];
        if (this.creditCards.length > 0 && !this.selectedCreditCard) {
          this.selectedCreditCard = this.creditCards[0];
          this.cardForm.creditCardId = this.creditCards[0].id;
        }
        this.isLoadingCards = false;
      },
      error: (err: any) => {
        console.error('Error loading credit cards:', err);
        this.creditCards = [];
        this.isLoadingCards = false;
        this.alertService.error('Error', 'Failed to load credit cards');
      }
    });
  }

  onCardSelect() {
    if (this.cardForm.creditCardId) {
      this.selectedCreditCard = this.creditCards.find(c => c.id === this.cardForm.creditCardId);
    }
  }

  getProviders(): string[] {
    return this.billForm.billType === 'Mobile Bill' ? this.mobileProviders : this.wifiProviders;
  }

  formatCardNumber(value: string): string {
    // Remove all non-digits
    const numbers = value.replace(/\D/g, '');
    // Add spaces every 4 digits
    return numbers.replace(/(\d{4})(?=\d)/g, '$1 ');
  }

  onCardNumberInput(event: any) {
    const formatted = this.formatCardNumber(event.target.value);
    this.cardForm.cardNumber = formatted;
    // Limit to 19 characters (16 digits + 3 spaces)
    if (this.cardForm.cardNumber.length > 19) {
      this.cardForm.cardNumber = this.cardForm.cardNumber.substring(0, 19);
    }
  }

  formatExpiryDate(value: string): string {
    // Remove all non-digits
    const numbers = value.replace(/\D/g, '');
    // Format as MM/YY
    if (numbers.length >= 2) {
      return numbers.substring(0, 2) + '/' + numbers.substring(2, 4);
    }
    return numbers;
  }

  onExpiryInput(event: any) {
    const formatted = this.formatExpiryDate(event.target.value);
    this.cardForm.expiryDate = formatted;
    // Limit to 5 characters (MM/YY)
    if (this.cardForm.expiryDate.length > 5) {
      this.cardForm.expiryDate = this.cardForm.expiryDate.substring(0, 5);
    }
  }

  onCvvInput(event: any) {
    // Only allow digits, limit to 3
    this.cardForm.cvv = event.target.value.replace(/\D/g, '').substring(0, 3);
  }

  onOtpInput(event: any) {
    // Sanitize OTP input: allow only digits and limit to 6 characters
    const val = (event.target?.value ?? '').replace(/\D/g, '').substring(0, 6);
    this.otp = val;
  }

  onAmountInput(event: any) {
    // Allow only digits and at most one decimal point while typing
    if (!event || !event.target) return;
    let val: string = event.target.value ?? '';
    // Remove any characters except digits and dot
    val = val.replace(/[^0-9.]/g, '');
    // Ensure only one dot
    const parts = val.split('.');
    if (parts.length > 2) {
      val = parts[0] + '.' + parts.slice(1).join('');
    }
    // Update model as number (do not round on every keystroke)
    const num = Number(val);
    this.billForm.amount = isNaN(num) ? 0 : num;
  }

  onAmountBlur(event: any) {
    // Round to 2 decimals on blur to avoid floating point artifacts
    if (!event || !event.target) return;
    const raw = event.target.value ?? '';
    const num = Number(String(raw).replace(/[^0-9.]/g, ''));
    const rounded = isNaN(num) ? 0 : Math.round(num * 100) / 100;
    this.billForm.amount = rounded;
    // Update the input display to a clean value (show up to 2 decimals only when needed)
    // If integer, show without decimals so user sees 9000 not 9000.00
    event.target.value = Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2);
  }

  validateForm(): boolean {
    if (!this.billForm.billType) {
      this.alertService.error('Validation Error', 'Please select bill type');
      return false;
    }

    if (!this.billForm.networkProvider) {
      this.alertService.error('Validation Error', 'Please select network provider');
      return false;
    }

    if (!this.billForm.customerNumber || this.billForm.customerNumber.trim() === '') {
      this.alertService.error('Validation Error', 'Please enter customer/mobile number');
      return false;
    }

    if (!this.billForm.amount || this.billForm.amount <= 0) {
      this.alertService.error('Validation Error', 'Please enter a valid amount');
      return false;
    }

    // Credit card selection is optional now (can use debit card)
    // if (!this.cardForm.creditCardId) {
    //   this.alertService.error('Validation Error', 'Please select a credit card');
    //   return false;
    // }

    // Remove spaces from card number for validation
    const cleanCardNumber = this.cardForm.cardNumber.replace(/\s/g, '');
    if (cleanCardNumber.length !== 16) {
      this.alertService.error('Validation Error', 'Card number must be 16 digits');
      return false;
    }

    if (this.cardForm.cvv.length !== 3) {
      this.alertService.error('Validation Error', 'CVV must be 3 digits');
      return false;
    }

    if (this.cardForm.expiryDate.length !== 5) {
      this.alertService.error('Validation Error', 'Expiry date must be in MM/YY format');
      return false;
    }

    return true;
  }

  processPayment() {
    if (!this.validateForm()) {
      return;
    }

    if (!this.cardForm.cardholderName || this.cardForm.cardholderName.trim() === '') {
      this.alertService.error('Validation Error', 'Please enter cardholder name');
      return;
    }

    this.isProcessing = true;

    // Remove spaces from card number
    const cleanCardNumber = this.cardForm.cardNumber.replace(/\s/g, '');

    const request = {
      cardNumber: cleanCardNumber,
      cvv: this.cardForm.cvv,
      expiryDate: this.cardForm.expiryDate,
      cardholderName: this.cardForm.cardholderName.trim(),
      accountNumber: this.userAccountNumber
    };

    // Verify card and send OTP
    this.http.post(`${environment.apiBaseUrl}/api/bill-payments/verify-card`, request).subscribe({
      next: (response: any) => {
        this.isProcessing = false;
        if (response.success) {
          this.otpData = {
            cardType: response.cardType,
            cardLastFour: response.cardLastFour,
            maskedEmail: response.maskedEmail,
            cardNumber: cleanCardNumber,
            cvv: this.cardForm.cvv,
            expiryDate: this.cardForm.expiryDate,
            cardholderName: this.cardForm.cardholderName
          };
          this.showOtpPage = true;
          this.startOtpTimer();
          this.alertService.success('OTP Sent', 'OTP has been sent to your registered email');
        } else {
          this.alertService.error('Verification Failed', response.message || 'Failed to verify card details');
        }
      },
      error: (err: any) => {
        console.error('Error verifying card:', err);
        const errorMessage = err.error?.message || 'Failed to verify card. Please try again.';
        this.alertService.error('Verification Error', errorMessage);
        this.isProcessing = false;
      }
    });
  }

  startOtpTimer() {
    this.otpTimer = 120; // 2 minutes
    if (this.otpTimerInterval) {
      clearInterval(this.otpTimerInterval);
    }
    this.otpTimerInterval = setInterval(() => {
      this.otpTimer--;
      if (this.otpTimer <= 0) {
        clearInterval(this.otpTimerInterval);
        this.alertService.error('OTP Expired', 'OTP has expired. Please request a new one.');
      }
    }, 1000);
  }

  resendOtp() {
    if (!this.otpData) return;

    this.isProcessing = true;
    const request = {
      cardNumber: this.otpData.cardNumber,
      cvv: this.otpData.cvv,
      expiryDate: this.otpData.expiryDate,
      cardholderName: this.otpData.cardholderName,
      accountNumber: this.userAccountNumber
    };

    this.http.post(`${environment.apiBaseUrl}/api/bill-payments/resend-otp`, request).subscribe({
      next: (response: any) => {
        this.isProcessing = false;
        if (response.success) {
          this.otpData.maskedEmail = response.maskedEmail;
          this.otp = '';
          this.startOtpTimer();
          this.alertService.success('OTP Resent', 'New OTP has been sent to your registered email');
        } else {
          this.alertService.error('Error', response.message || 'Failed to resend OTP');
        }
      },
      error: (err: any) => {
        console.error('Error resending OTP:', err);
        const errorMessage = err.error?.message || 'Failed to resend OTP. Please try again.';
        this.alertService.error('Error', errorMessage);
        this.isProcessing = false;
      }
    });
  }

  verifyOtpAndPay() {
    if (!this.otp || this.otp.length !== 6) {
      this.alertService.error('Validation Error', 'Please enter a valid 6-digit OTP');
      return;
    }

    this.isVerifyingOtp = true;

    const request = {
      accountNumber: this.userAccountNumber,
      userName: this.userName,
      userEmail: this.userEmail,
      billType: this.billForm.billType,
      networkProvider: this.billForm.networkProvider,
      customerNumber: this.billForm.customerNumber,
      amount: this.billForm.amount,
      creditCardId: this.cardForm.creditCardId,
      cardNumber: this.otpData.cardNumber,
      cvv: this.otpData.cvv,
      expiryDate: this.otpData.expiryDate,
      otp: this.otp,
      cardType: this.otpData.cardType
    };

    this.http.post(`${environment.apiBaseUrl}/api/bill-payments/verify-otp-and-pay`, request).subscribe({
      next: (response: any) => {
        this.isVerifyingOtp = false;
        if (response.success) {
          this.alertService.success('Success', 'Bill payment processed successfully!');
          this.closeOtpPage();
          this.resetForm();
          this.loadPaymentHistory();
          this.loadCreditCards();
        } else {
          this.alertService.error('Error', response.message || 'Payment failed');
        }
      },
      error: (err: any) => {
        console.error('Error processing payment:', err);
        const errorMessage = err.error?.message || 'Failed to process payment. Please try again.';
        this.alertService.error('Payment Error', errorMessage);
        this.isVerifyingOtp = false;
      }
    });
  }

  closeOtpPage() {
    this.showOtpPage = false;
    this.otpData = null;
    this.otp = '';
    if (this.otpTimerInterval) {
      clearInterval(this.otpTimerInterval);
      this.otpTimerInterval = null;
    }
    this.otpTimer = 120;
  }

  formatOtpTimer(): string {
    const minutes = Math.floor(this.otpTimer / 60);
    const seconds = this.otpTimer % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  resetForm() {
    this.billForm = {
      billType: 'Mobile Bill',
      networkProvider: '',
      customerNumber: '',
      amount: 0
    };
    this.cardForm = {
      creditCardId: this.selectedCreditCard?.id || null,
      cardNumber: '',
      cvv: '',
      expiryDate: '',
      cardholderName: ''
    };
    this.closeOtpPage();
  }

  loadPaymentHistory() {
    if (!this.userAccountNumber) return;

    this.isLoadingHistory = true;
    this.http.get(`${environment.apiBaseUrl}/api/bill-payments/account/${this.userAccountNumber}`).subscribe({
      next: (payments: any) => {
        this.paymentHistory = Array.isArray(payments) ? payments : [];
        this.isLoadingHistory = false;
      },
      error: (err: any) => {
        console.error('Error loading payment history:', err);
        this.paymentHistory = [];
        this.isLoadingHistory = false;
      }
    });
  }

  getMaskedCardNumber(cardNumber: string): string {
    if (!cardNumber) return '';
    if (cardNumber.length <= 4) return cardNumber;
    return '**** **** **** ' + cardNumber.substring(cardNumber.length - 4);
  }
}
