import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, HostListener } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

type AtmStep = 'idle' | 'select-atm' | 'inserting-card' | 'reading-card' | 'pin-entry' | 'menu' | 'withdraw' | 'custom-amount' | 'processing' | 'dispensing' | 'collect-cash' | 'receipt' | 'ejecting-card' | 'thank-you' | 'balance' | 'mini-statement' | 'error' | 'no-card';

interface MiniStatementEntry {
  date: string;
  description: string;
  amount: number;
  type: 'CR' | 'DR';
}

interface DebitCardData {
  id: number;
  cardNumber: string;
  cardType: string;
  cvv: string;
  userName: string;
  expiryDate: string;
  pinSet: boolean;
  status: string;
  accountNumber: string;
  blocked: boolean;
  deactivated: boolean;
}

@Component({
  selector: 'app-atm-simulator',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './atm-simulator.html',
  styleUrls: ['./atm-simulator.css']
})
export class AtmSimulatorComponent implements OnInit, OnDestroy {
  currentStep: AtmStep = 'idle';
  pin: string = '';
  pinMask: string = '';
  selectedAmount: number = 0;
  customAmount: number = 0;
  currentBalance: number = 0;
  accountNumber: string = '';
  username: string = '';
  errorMessage: string = '';
  noteCount: number = 0;
  dispensedNotes: number[] = [];
  showReceipt: boolean = false;
  receiptData: any = null;
  miniStatement: MiniStatementEntry[] = [];
  private isBrowser: boolean;
  private stepTimeout: any;
  pressedKey: string | null = null;
  pinAttempts: number = 0;
  maxPinAttempts: number = 3;

  // ATM Machine selection
  availableAtms: any[] = [];
  selectedAtmId: string = '';
  selectedAtmInfo: any = null;
  isLoadingAtms: boolean = false;

  // Real debit card data from backend
  debitCard: DebitCardData | null = null;
  cardLoading: boolean = false;
  cardLoadError: string = '';

  quickAmounts = [500, 1000, 2000, 5000, 10000];

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (this.isBrowser) {
      // Read user data from currentUser session (set during login)
      const currentUserStr = sessionStorage.getItem('currentUser');
      if (currentUserStr) {
        const currentUser = JSON.parse(currentUserStr);
        this.accountNumber = currentUser.accountNumber || '';
        this.username = currentUser.name || currentUser.email || '';
      }
      // Fallback to localStorage user_profile
      if (!this.accountNumber) {
        const profileStr = localStorage.getItem('user_profile');
        if (profileStr) {
          const profile = JSON.parse(profileStr);
          this.accountNumber = profile.accountNumber || '';
          this.username = profile.name || this.username;
        }
      }
      this.fetchBalance();
      this.loadDebitCard();
    }
  }

  ngOnDestroy(): void {
    if (this.stepTimeout) clearTimeout(this.stepTimeout);
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (!this.isBrowser) return;
    const key = event.key;

    if (key >= '0' && key <= '9') {
      if (this.currentStep === 'pin-entry' || this.currentStep === 'custom-amount') {
        event.preventDefault();
        this.onKeyPress(key);
      }
    } else if (key === 'Enter') {
      if (this.currentStep === 'pin-entry' || this.currentStep === 'custom-amount') {
        event.preventDefault();
        this.onKeyPress('enter');
      }
    } else if (key === 'Backspace' || key === 'Delete') {
      if (this.currentStep === 'pin-entry' || this.currentStep === 'custom-amount') {
        event.preventDefault();
        this.onKeyPress('clear');
      }
    } else if (key === 'Escape') {
      if (this.currentStep !== 'idle' && this.currentStep !== 'thank-you') {
        event.preventDefault();
        this.onKeyPress('cancel');
      }
    }
  }

  // Load user's real debit card from backend
  loadDebitCard(): void {
    if (!this.accountNumber) {
      this.cardLoadError = 'No account number found. Please login again.';
      return;
    }
    this.cardLoading = true;
    this.cardLoadError = '';
    this.http.get<any>(`${environment.apiBaseUrl}/api/cards/account/${this.accountNumber}`).subscribe({
      next: (response) => {
        this.cardLoading = false;
        const cards = Array.isArray(response) ? response : (response ? [response] : []);
        console.log('ATM - Cards loaded:', cards.length, cards);
        if (cards.length > 0) {
          // Find active debit card (try strict match first, fallback to any card)
          const debit = cards.find((c: any) =>
            (c.cardType?.toLowerCase().includes('debit') || c.cardType?.toLowerCase().includes('visa')) &&
            c.status === 'Active' && !c.blocked && !c.deactivated
          );
          this.debitCard = debit || cards[0];
          console.log('ATM - Selected card:', this.debitCard);
          if (this.debitCard) {
            this.username = this.debitCard.userName || this.username;
          }
        } else {
          this.debitCard = null;
          this.cardLoadError = 'No debit card found. Please apply for a card from Card Management.';
        }
      },
      error: (err) => {
        this.cardLoading = false;
        this.debitCard = null;
        console.error('ATM - Error loading card:', err);
        this.cardLoadError = 'Unable to load card data. Please try again.';
      }
    });
  }

  fetchBalance(): void {
    if (!this.accountNumber) return;
    this.http.get<any>(`${environment.apiBaseUrl}/api/accounts/balance/${this.accountNumber}`).subscribe({
      next: (res) => {
        this.currentBalance = res.balance || res.currentBalance || 0;
      },
      error: () => {
        this.currentBalance = 50000; // Fallback for demo
      }
    });
  }

  loadAvailableAtms(): void {
    this.isLoadingAtms = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/admin/atm/active-atms`).subscribe({
      next: (res) => {
        this.availableAtms = res.atms || [];
        this.isLoadingAtms = false;
        // Auto-select first if only one
        if (this.availableAtms.length === 1) {
          this.selectAtm(this.availableAtms[0]);
        }
      },
      error: () => {
        this.isLoadingAtms = false;
        // Fallback: allow without ATM selection
        this.availableAtms = [];
      }
    });
  }

  selectAtm(atm: any): void {
    this.selectedAtmId = atm.atmId;
    this.selectedAtmInfo = atm;
  }

  confirmAtmSelection(): void {
    if (!this.selectedAtmId) {
      this.errorMessage = 'Please select an ATM';
      return;
    }
    // Check ATM status before proceeding
    this.http.get<any>(`${environment.apiBaseUrl}/api/admin/atm/status/${this.selectedAtmId}`).subscribe({
      next: (res) => {
        if (res.available) {
          this.insertCard();
        } else {
          this.currentStep = 'error';
          this.errorMessage = res.message || 'This ATM is currently unavailable.';
        }
      },
      error: () => {
        // If status check fails, proceed anyway
        this.insertCard();
      }
    });
  }

  startAtmSession(): void {
    if (this.cardLoading) {
      this.errorMessage = 'Loading card data, please wait...';
      return;
    }
    // Load available ATMs and show selection
    this.loadAvailableAtms();
    this.currentStep = 'select-atm';
    this.errorMessage = '';
  }

  // Computed card display helpers
  get maskedCardNumber(): string {
    if (!this.debitCard?.cardNumber) return '•••• •••• •••• 0000';
    const num = this.debitCard.cardNumber;
    return '•••• •••• •••• ' + num.slice(-4);
  }

  get cardLast4(): string {
    return this.debitCard?.cardNumber?.slice(-4) || this.accountNumber?.slice(-4) || '0000';
  }

  get cardHolderName(): string {
    return this.debitCard?.userName || this.username || 'CARD HOLDER';
  }

  get cardExpiry(): string {
    return this.debitCard?.expiryDate || '12/28';
  }

  get cardTypeName(): string {
    return this.debitCard?.cardType || 'Visa Debit';
  }

  get isCardReady(): boolean {
    return !!this.debitCard && this.debitCard.status === 'Active' && !this.debitCard.blocked && !this.debitCard.deactivated;
  }

  get isPinSet(): boolean {
    return !!this.debitCard?.pinSet;
  }

  // === STEP NAVIGATION ===
  insertCard(): void {
    if (this.cardLoading) {
      this.errorMessage = 'Loading card data, please wait...';
      return;
    }
    if (!this.debitCard) {
      // Try reloading card one more time
      this.loadDebitCard();
      this.currentStep = 'no-card';
      return;
    }
    if (this.debitCard.blocked || this.debitCard.deactivated || this.debitCard.status !== 'Active') {
      this.currentStep = 'error';
      this.errorMessage = this.debitCard.blocked ? 'Your card is blocked. Please contact the bank.' :
                          this.debitCard.deactivated ? 'Your card is deactivated. Please request a new card.' :
                          'Your card is not active. Status: ' + this.debitCard.status;
      return;
    }
    if (!this.debitCard.pinSet) {
      this.currentStep = 'error';
      this.errorMessage = 'ATM PIN not set. Please set your ATM PIN from Card Management first.';
      return;
    }
    this.currentStep = 'inserting-card';
    this.stepTimeout = setTimeout(() => {
      this.currentStep = 'reading-card';
      this.stepTimeout = setTimeout(() => {
        this.currentStep = 'pin-entry';
        this.pin = '';
        this.pinMask = '';
        this.pinAttempts = 0;
        this.errorMessage = '';
      }, 2000);
    }, 1500);
  }

  onKeyPress(key: string): void {
    if (!key || key === '') return;
    this.pressedKey = key;
    setTimeout(() => this.pressedKey = null, 150);

    if (key === 'clear') {
      if (this.currentStep === 'pin-entry') {
        this.pin = '';
        this.pinMask = '';
      } else if (this.currentStep === 'custom-amount') {
        this.customAmount = 0;
      }
      return;
    }
    if (key === 'cancel') {
      this.ejectCard();
      return;
    }
    if (key === 'enter') {
      if (this.currentStep === 'pin-entry') {
        this.verifyPin();
      } else if (this.currentStep === 'custom-amount') {
        this.selectAmount(this.customAmount);
      }
      return;
    }
    if (this.currentStep === 'pin-entry' && this.pin.length < 4) {
      this.pin += key;
      this.pinMask = '*'.repeat(this.pin.length);
    }
    if (this.currentStep === 'custom-amount') {
      const newVal = this.customAmount * 10 + parseInt(key);
      if (newVal <= 100000) {
        this.customAmount = newVal;
      }
    }
  }

  verifyPin(): void {
    if (this.pin.length !== 4) {
      this.errorMessage = 'Please enter 4-digit PIN';
      return;
    }
    if (!this.debitCard) {
      this.errorMessage = 'Card data not loaded';
      return;
    }
    this.errorMessage = '';
    this.currentStep = 'processing';

    // Verify PIN against backend
    this.http.post<any>(
      `${environment.apiBaseUrl}/api/cards/${this.debitCard.id}/verify-pin?pin=${encodeURIComponent(this.pin)}`, {}
    ).subscribe({
      next: (res) => {
        if (res.success) {
          this.pinAttempts = 0;
          this.stepTimeout = setTimeout(() => {
            this.currentStep = 'menu';
            this.errorMessage = '';
          }, 1200);
        } else {
          this.pinAttempts++;
          this.pin = '';
          this.pinMask = '';
          if (this.pinAttempts >= this.maxPinAttempts) {
            this.currentStep = 'error';
            this.errorMessage = 'Too many incorrect PIN attempts. Card retained for security. Please contact the bank.';
          } else {
            this.currentStep = 'pin-entry';
            this.errorMessage = `Incorrect PIN. ${this.maxPinAttempts - this.pinAttempts} attempt(s) remaining.`;
          }
        }
      },
      error: () => {
        this.pinAttempts++;
        this.pin = '';
        this.pinMask = '';
        if (this.pinAttempts >= this.maxPinAttempts) {
          this.currentStep = 'error';
          this.errorMessage = 'Too many incorrect PIN attempts. Please try again later.';
        } else {
          this.currentStep = 'pin-entry';
          this.errorMessage = `PIN verification failed. ${this.maxPinAttempts - this.pinAttempts} attempt(s) remaining.`;
        }
      }
    });
  }

  selectMenuOption(option: string): void {
    switch (option) {
      case 'withdraw':
        this.currentStep = 'withdraw';
        break;
      case 'balance':
        this.currentStep = 'processing';
        this.stepTimeout = setTimeout(() => {
          this.currentStep = 'balance';
        }, 1500);
        break;
      case 'mini-statement':
        this.currentStep = 'processing';
        this.loadMiniStatement();
        break;
      case 'exit':
        this.ejectCard();
        break;
    }
  }

  loadMiniStatement(): void {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/transactions/recent/${this.accountNumber}`).subscribe({
      next: (transactions) => {
        this.miniStatement = (transactions || []).slice(0, 5).map(t => ({
          date: new Date(t.date || t.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' }),
          description: t.description || t.merchant || t.type || 'Transaction',
          amount: t.amount,
          type: t.type === 'CREDIT' || t.type === 'credit' ? 'CR' : 'DR'
        }));
        if (this.miniStatement.length === 0) {
          this.miniStatement = [
            { date: '06 Mar', description: 'UPI Transfer', amount: 2500, type: 'DR' },
            { date: '05 Mar', description: 'Salary Credit', amount: 45000, type: 'CR' },
            { date: '04 Mar', description: 'ATM Withdrawal', amount: 5000, type: 'DR' },
            { date: '03 Mar', description: 'Online Purchase', amount: 1299, type: 'DR' },
            { date: '02 Mar', description: 'Refund', amount: 750, type: 'CR' }
          ];
        }
        this.currentStep = 'mini-statement';
      },
      error: () => {
        this.miniStatement = [
          { date: '06 Mar', description: 'UPI Transfer', amount: 2500, type: 'DR' },
          { date: '05 Mar', description: 'Salary Credit', amount: 45000, type: 'CR' },
          { date: '04 Mar', description: 'ATM Withdrawal', amount: 5000, type: 'DR' },
          { date: '03 Mar', description: 'Online Purchase', amount: 1299, type: 'DR' },
          { date: '02 Mar', description: 'Refund', amount: 750, type: 'CR' }
        ];
        this.currentStep = 'mini-statement';
      }
    });
  }

  selectAmount(amount: number): void {
    if (amount <= 0) {
      this.errorMessage = 'Please enter a valid amount';
      return;
    }
    if (amount % 100 !== 0) {
      this.errorMessage = 'Amount must be in multiples of ₹100';
      return;
    }
    if (amount > this.currentBalance) {
      this.errorMessage = 'Insufficient balance';
      return;
    }
    if (amount > 25000) {
      this.errorMessage = 'Maximum withdrawal limit is ₹25,000';
      return;
    }
    this.errorMessage = '';
    this.selectedAmount = amount;
    this.currentStep = 'processing';
    this.processWithdrawal();
  }

  enterCustomAmount(): void {
    this.customAmount = 0;
    this.currentStep = 'custom-amount';
    this.errorMessage = '';
  }

  processWithdrawal(): void {
    const balanceBefore = this.currentBalance;
    this.http.post<any>(`${environment.apiBaseUrl}/api/transactions/withdraw`, {
      accountNumber: this.accountNumber,
      amount: this.selectedAmount,
      type: 'ATM_WITHDRAWAL',
      description: 'ATM Cash Withdrawal'
    }).subscribe({
      next: (res) => {
        if (res.balance !== undefined) {
          this.currentBalance = res.balance;
        } else {
          this.currentBalance -= this.selectedAmount;
        }
        // Record ATM machine transaction if ATM is selected
        if (this.selectedAtmId) {
          this.http.post<any>(`${environment.apiBaseUrl}/api/admin/atm/withdraw`, {
            atmId: this.selectedAtmId,
            accountNumber: this.accountNumber,
            cardNumber: this.debitCard?.cardNumber || '',
            userName: this.username,
            userEmail: '',
            amount: this.selectedAmount,
            userBalanceBefore: balanceBefore,
            userBalanceAfter: this.currentBalance
          }).subscribe({
            next: () => {},
            error: () => {}
          });
        }
        this.startDispensing();
      },
      error: (err) => {
        const msg = err?.error?.message || 'Transaction failed. Please try again.';
        this.currentStep = 'error';
        this.errorMessage = msg;
      }
    });
  }

  startDispensing(): void {
    this.currentStep = 'dispensing';
    this.dispensedNotes = this.calculateNotes(this.selectedAmount);
    this.noteCount = 0;

    const dispenseInterval = setInterval(() => {
      if (this.noteCount < this.dispensedNotes.length) {
        this.noteCount++;
      } else {
        clearInterval(dispenseInterval);
        this.stepTimeout = setTimeout(() => {
          this.currentStep = 'collect-cash';
        }, 500);
      }
    }, 400);
  }

  calculateNotes(amount: number): number[] {
    const notes: number[] = [];
    const denominations = [2000, 500, 200, 100];
    let remaining = amount;
    for (const denom of denominations) {
      while (remaining >= denom) {
        notes.push(denom);
        remaining -= denom;
      }
    }
    return notes;
  }

  collectCash(): void {
    this.currentStep = 'receipt';
    this.receiptData = {
      bank: 'NEOBANK',
      date: new Date().toLocaleString('en-IN'),
      accountNumber: this.accountNumber ? '****' + this.accountNumber.slice(-4) : '****1234',
      amount: this.selectedAmount,
      balance: this.currentBalance,
      txnId: 'ATM' + Date.now().toString().slice(-8)
    };
  }

  printReceipt(): void {
    this.showReceipt = true;
    this.stepTimeout = setTimeout(() => {
      this.ejectCard();
    }, 3000);
  }

  skipReceipt(): void {
    this.ejectCard();
  }

  ejectCard(): void {
    this.currentStep = 'ejecting-card';
    this.stepTimeout = setTimeout(() => {
      this.currentStep = 'thank-you';
      this.stepTimeout = setTimeout(() => {
        this.resetATM();
      }, 4000);
    }, 2000);
  }

  goBackToMenu(): void {
    this.currentStep = 'menu';
    this.errorMessage = '';
    this.customAmount = 0;
  }

  resetATM(): void {
    this.currentStep = 'idle';
    this.pin = '';
    this.pinMask = '';
    this.selectedAmount = 0;
    this.customAmount = 0;
    this.errorMessage = '';
    this.noteCount = 0;
    this.dispensedNotes = [];
    this.showReceipt = false;
    this.receiptData = null;
    this.miniStatement = [];
    this.pinAttempts = 0;
    this.selectedAtmId = '';
    this.selectedAtmInfo = null;
    this.availableAtms = [];
    this.fetchBalance();
    this.loadDebitCard();
  }
}
