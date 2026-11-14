import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation, Input } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

interface CardDetails {
  id: string;
  userId: string;
  userName: string;
  userAccountNumber: string;
  cardNumber: string;
  cardType: string;
  cvv: string;
  expiry: string;
  status: 'Active' | 'Blocked' | 'Replaced' | 'Deactivated';
  pinSet: boolean;
  createdAt: string;
  lastUpdated: string;
  replacedBy?: string;
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
  occupation?: string;
  income?: number;
}

interface CardActionHistory {
  id: number;
  actionType: string;
  oldValue?: string;
  newValue?: string;
  changedBy?: string;
  createdAt: string;
}

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './card.html',
  styleUrls: ['./card.css'],
  encapsulation: ViewEncapsulation.None
})
export class Card implements OnInit, OnDestroy {
  @Input() accountNumberInput = '';
  @Input() accountNameInput = '';
  @Input() accountEmailInput = '';
  @Input() accountTypeInput = '';
  @Input() embedded = false;

  showDetails: boolean = false;
  showCVV: boolean = false;
  newPin: string = '';
  currentPin: string = '';
  userProfile: UserProfile | null = null;
  card: CardDetails | null = null;
  showPinForm: boolean = false;
  showUpdatePinForm: boolean = false;
  showForgotPinForm: boolean = false;
  showReplaceForm: boolean = false;
  replaceReason: string = '';
  isCardFlipped: boolean = false;
  // Credit card application UI state
  showApplyCreditForm: boolean = false;
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
  // CIBIL Report Lookup
  cibilReportData: any = null;
  isLoadingCibilReport: boolean = false;
  isLoadingCard: boolean = false;
  cardLoadError: string = '';
  isGeneratingCard: boolean = false;
  cardGenerationError: string = '';
  cardGenerationSuccess: string = '';
  aadharActivationInput = '';
  cardHistory: CardActionHistory[] = [];
  isLoadingCardHistory: boolean = false;
  cardHistoryError: string = '';
  private cardPollInterval: any = null;

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient, private alertService: AlertService) {}

  ngOnInit() {
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      if (this.accountNumberInput) {
        this.userProfile = {
          name: this.accountNameInput || 'Account Holder',
          email: this.accountEmailInput || '',
          accountNumber: this.accountNumberInput,
          accountType: this.accountTypeInput || 'Account',
          joinDate: new Date().toISOString().split('T')[0]
        };
        this.loadUserCard();
        this.startCardPolling();
        return;
      }
      this.loadUserProfile();
      this.loadUserCard();
      this.startCardPolling();
    }
  }

  ngOnDestroy() {
    if (this.cardPollInterval) clearInterval(this.cardPollInterval);
  }

  private startCardPolling() {
    if (this.cardPollInterval) clearInterval(this.cardPollInterval);
    this.cardPollInterval = setInterval(() => this.loadUserCard(), 5000);
  }

  // Method to refresh card data (can be called when user returns to page)
  refreshCardData() {
    this.loadUserCard();
  }

  private isDebitCardRecord(card: any): boolean {
    const type = String(card?.cardType || '').toLowerCase();
    return type.includes('debit') && !type.includes('credit');
  }

  private selectDebitCard(cards: any[]): any | null {
    const debitCards = (Array.isArray(cards) ? cards : []).filter(card => this.isDebitCardRecord(card));
    if (this.card) {
      const currentCard = debitCards.find(card => String(card.id) === this.card?.id);
      if (currentCard) return currentCard;
    }
    return debitCards.find(card =>
      String(card.status || '').toLowerCase() === 'active' &&
      !card.blocked && !card.deactivated
    ) || debitCards[0] || null;
  }

  private mapBackendCard(card: any): CardDetails {
    return {
      id: String(card.id),
      userId: card.accountNumber,
      userName: card.userName,
      userAccountNumber: card.accountNumber,
      cardNumber: card.cardNumber,
      cardType: card.cardType,
      cvv: card.cvv,
      expiry: card.expiryDate,
      status: card.status,
      pinSet: card.pinSet,
      createdAt: card.createdAt || new Date().toISOString(),
      lastUpdated: card.lastUpdated || new Date().toISOString()
    };
  }

  private withSelectedDebitCard(action: (card: any) => void): void {
    if (!this.userProfile) return;
    const accountNumber = encodeURIComponent(this.userProfile.accountNumber);
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/cards/account/${accountNumber}`).subscribe({
      next: (cards) => {
        const selectedDebitCard = this.selectDebitCard(cards);
        if (!selectedDebitCard) {
          this.alertService.cardError('Debit Card Not Found', 'No debit card is linked to this account.');
          return;
        }
        action(selectedDebitCard);
      },
      error: (err: any) => {
        console.error('Error retrieving debit card:', err);
        this.alertService.operationError('Debit card information retrieval', err.message || 'Unknown error');
      }
    });
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);

      // Populate quickly from session first so account-scoped APIs can work
      this.userProfile = {
        name: user.name || 'User',
        email: user.email || 'user@example.com',
        accountNumber: user.accountNumber || 'ACC001',
        phoneNumber: '',
        address: '',
        dateOfBirth: '',
        accountType: 'Savings Account',
        joinDate: new Date().toISOString().split('T')[0],
        pan: '',
        occupation: '',
        income: 0
      };

      const userIdNum = typeof user.id === 'number' ? user.id : Number(user.id);
      if (!isNaN(userIdNum)) {
        this.fetchAndApplyUserProfile(`${environment.apiBaseUrl}/api/users/${userIdNum}`);
      } else if (user.accountNumber) {
        // Fallback for stale sessions that don't contain id
        this.fetchAndApplyUserProfile(`${environment.apiBaseUrl}/api/users/account/${user.accountNumber}`);
      } else {
        this.loadUserCard();
      }
    } else {
      console.log('No user session found - trying fallback methods');
      // Try to get user from localStorage as fallback
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        const profile = JSON.parse(savedProfile);
        this.userProfile = profile;
        console.log('Using fallback profile from localStorage');
      } else {
        // Use fallback profile to avoid errors
        this.userProfile = {
          name: 'User',
          email: 'user@example.com',
          accountNumber: 'ACC001',
          phoneNumber: '',
          address: '',
          dateOfBirth: '',
          accountType: 'Savings Account',
          joinDate: new Date().toISOString().split('T')[0],
          pan: '',
          occupation: '',
          income: 0
        };
        console.log('Using default profile - no fallback found');
      }
      
      // Load user card after setting profile
      this.loadUserCard();
    }
  }

  private fetchAndApplyUserProfile(url: string) {
    this.http.get(url).subscribe({
      next: (userData: any) => {
        this.userProfile = {
          name: userData.account?.name || userData.username || this.userProfile?.name || 'User',
          email: userData.email || this.userProfile?.email || 'user@example.com',
          accountNumber: userData.accountNumber || this.userProfile?.accountNumber || 'ACC001',
          phoneNumber: userData.account?.phone || '',
          address: userData.account?.address || '',
          dateOfBirth: userData.account?.dob || '',
          accountType: userData.account?.accountType || 'Savings Account',
          joinDate: userData.joinDate ? new Date(userData.joinDate).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
          pan: userData.account?.pan || '',
          occupation: userData.account?.occupation || '',
          income: userData.account?.income || 0
        };

        this.loadUserCard();
      },
      error: (err: any) => {
        console.error('Error loading user profile from MySQL:', err);
        // Keep session-derived profile and continue loading card data
        if (!this.userProfile) {
          this.userProfile = {
            name: 'User',
            email: 'user@example.com',
            accountNumber: 'ACC001',
            phoneNumber: '',
            address: '',
            dateOfBirth: '',
            accountType: 'Savings Account',
            joinDate: new Date().toISOString().split('T')[0],
            pan: '',
            occupation: '',
            income: 0
          };
        }
        this.loadUserCard();
      }
    });
  }

  loadUserCard() {
    if (!isPlatformBrowser(this.platformId)) return;

    if (!this.userProfile) {
      this.card = null;
      this.cardHistory = [];
      this.isLoadingCard = false;
      return;
    }

    if (this.isLoadingCard) return;
    this.isLoadingCard = true;
    this.cardLoadError = '';
    const accountNumber = encodeURIComponent(this.userProfile.accountNumber);
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/cards/account/${accountNumber}`).subscribe({
      next: (cards) => {
        this.isLoadingCard = false;
        const debitCard = this.selectDebitCard(cards);
        if (debitCard) {
          const cardChanged = this.card?.id !== String(debitCard.id);
          this.card = this.mapBackendCard(debitCard);
          if (cardChanged) this.loadCardHistory();
        } else {
          this.card = null;
          this.cardHistory = [];
        }
      },
      error: (err: any) => {
        this.isLoadingCard = false;
        this.card = null;
        console.error('Error loading card from database:', err);
        this.cardLoadError = 'Unable to load debit card information. Please retry.';
      }
    });
  }

  generateDebitCard() {
    if (!this.userProfile || this.isGeneratingCard) return;
    const aadhaarNumber = this.aadharActivationInput.trim();
    if (!/^[0-9]{12}$/.test(aadhaarNumber)) {
      this.cardGenerationError = 'Enter the 12-digit Aadhaar number registered to your verified account.';
      return;
    }

    this.isGeneratingCard = true;
    this.cardGenerationError = '';
    this.cardGenerationSuccess = '';
    this.http.post<any>(`${environment.apiBaseUrl}/api/cards/generate/debit`, { aadhaarNumber }, {
      headers: this.getUserAuthHeaders()
    }).subscribe({
      next: (response) => {
        this.isGeneratingCard = false;
        this.aadharActivationInput = '';
        this.card = this.mapBackendCard(response);
        this.cardGenerationSuccess = 'Your debit card was generated and saved to card history.';
        this.loadCardHistory();
      },
      error: (err: any) => {
        this.isGeneratingCard = false;
        this.aadharActivationInput = '';
        console.error('Error generating debit card:', err);
        this.cardGenerationError = err.error?.message || 'Unable to generate your debit card. Please try again.';
      }
    });
  }

  private getUserAuthHeaders(): HttpHeaders {
    let token = sessionStorage.getItem('jwt') || '';
    if (!token) {
      const currentUser = sessionStorage.getItem('currentUser');
      token = currentUser ? JSON.parse(currentUser).authToken || '' : '';
    }
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }

  private loadCardHistory() {
    if (!this.card) return;

    this.isLoadingCardHistory = true;
    this.cardHistoryError = '';
    this.http.get<CardActionHistory[]>(
      `${environment.apiBaseUrl}/api/cards/${encodeURIComponent(this.card.id)}/user-action-history`,
      { headers: this.getUserAuthHeaders() }
    ).subscribe({
      next: (history) => {
        this.cardHistory = Array.isArray(history) ? history : [];
        this.isLoadingCardHistory = false;
      },
      error: (err: any) => {
        this.isLoadingCardHistory = false;
        this.cardHistoryError = 'Unable to load card history. Please refresh and try again.';
        console.error('Error loading debit card history:', err);
      }
    });
  }

  saveCard() {
    if (!isPlatformBrowser(this.platformId) || !this.card) return;
    
    const savedCards = localStorage.getItem('user_cards');
    let cards: CardDetails[] = savedCards ? JSON.parse(savedCards) : [];
    
    // Remove existing card for this user
    cards = cards.filter(card => card.userAccountNumber !== this.card?.userAccountNumber);
    
    // Add updated card
    cards.push(this.card);
    
    localStorage.setItem('user_cards', JSON.stringify(cards));
  }

  toggleDetails() {
    this.showDetails = !this.showDetails;
  }

  toggleCVV() {
    this.showCVV = !this.showCVV;
  }

  togglePinForm() {
    this.showPinForm = !this.showPinForm;
    this.newPin = '';
  }

  toggleUpdatePinForm() {
    this.showUpdatePinForm = !this.showUpdatePinForm;
    this.currentPin = '';
    this.newPin = '';
  }

  toggleForgotPinForm() {
    this.showForgotPinForm = !this.showForgotPinForm;
    this.newPin = '';
  }

  setPin() {
    if (!this.card || !this.userProfile) return;
    
    if (this.newPin.length !== 4) {
      this.alertService.validationError('PIN must be 4 digits');
      return;
    }
    
    if (!/^\d{4}$/.test(this.newPin)) {
      this.alertService.validationError('PIN must contain only numbers');
      return;
    }
    
    this.withSelectedDebitCard((backendCard) => {
      this.http.put(`${environment.apiBaseUrl}/api/cards/${backendCard.id}/pin?pin=${this.newPin}`, {}).subscribe({
        next: () => {
          this.card!.pinSet = true;
          this.card!.lastUpdated = new Date().toISOString();
          this.saveCard();
          this.alertService.cardSuccess('PIN Set', 'PIN has been set successfully!');
          this.newPin = '';
          this.showPinForm = false;
        },
        error: (err: any) => {
          console.error('Error updating debit card PIN:', err);
          this.alertService.cardError('PIN Setting Failed', 'Failed to set PIN. Please try again.');
        }
      });
    });
  }

  resetPin() {
    if (!this.card || !this.userProfile) return;
    
    if (this.newPin.length !== 4) {
      this.alertService.validationError('New PIN must be 4 digits');
      return;
    }
    
    if (!/^\d{4}$/.test(this.newPin)) {
      this.alertService.validationError('New PIN must contain only numbers');
      return;
    }
    
    this.alertService.confirm(
      'Reset PIN',
      'Are you sure you want to reset your PIN? This will disable your current PIN and set a new one.',
      () => {
        this.withSelectedDebitCard((backendCard) => {
          this.http.put(`${environment.apiBaseUrl}/api/cards/${backendCard.id}/reset-pin?pin=${this.newPin}`, {}).subscribe({
            next: () => {
              this.card!.pinSet = true;
              this.card!.lastUpdated = new Date().toISOString();
              this.saveCard();
              this.alertService.cardSuccess('PIN Reset', 'PIN has been reset successfully! Your new PIN is ready to use.');
              this.newPin = '';
              this.showForgotPinForm = false;
            },
            error: (err: any) => {
              console.error('Error resetting debit card PIN:', err);
              this.alertService.cardError('PIN Reset Failed', 'Failed to reset PIN. Please try again.');
            }
          });
        });
      }
    );
  }

  blockCard() {
    if (!this.card || !this.userProfile) return;
    
    this.alertService.cardConfirm(
      'Block Card',
      'Are you sure you want to block this card? This action can be reversed by admin.',
      () => {
        this.withSelectedDebitCard((backendCard) => {
          this.http.put(`${environment.apiBaseUrl}/api/cards/${backendCard.id}/block`, {}).subscribe({
            next: () => {
              this.card!.status = 'Blocked';
              this.card!.lastUpdated = new Date().toISOString();
              this.saveCard();
              this.loadCardHistory();
              this.alertService.cardSuccess('Card Blocked', 'Your card has been blocked for security reasons.');
            },
            error: (err: any) => {
              console.error('Error blocking debit card:', err);
              this.alertService.cardError('Card Blocking Failed', 'Failed to block card. Please try again.');
            }
          });
        });
      }
    );
  }

  unblockCard() {
    if (!this.card || !this.userProfile) return;
    this.withSelectedDebitCard((backendCard) => {
        this.http.put(`${environment.apiBaseUrl}/api/cards/${backendCard.id}/unblock`, {}).subscribe({
          next: () => {
            this.card!.status = 'Active';
            this.card!.lastUpdated = new Date().toISOString();
            this.saveCard();
            this.loadCardHistory();
            this.alertService.cardSuccess('Card Unblocked', 'Your debit card is active again.');
          },
          error: () => this.alertService.cardError('Card Unblocking Failed', 'Failed to unblock card. Please try again.')
        });
    });
  }

  deactivateCard() {
    if (!this.card || !this.userProfile) return;
    
    this.alertService.cardConfirm(
      'Deactivate Card',
      'Are you sure you want to deactivate this card? This action cannot be undone.',
      () => {
        this.withSelectedDebitCard((backendCard) => {
          this.http.put(`${environment.apiBaseUrl}/api/cards/${backendCard.id}/deactivate`, {}).subscribe({
            next: () => {
              this.card!.status = 'Deactivated';
              this.card!.lastUpdated = new Date().toISOString();
              this.saveCard();
              this.loadCardHistory();
              this.alertService.cardSuccess('Card Deactivated', 'Your card has been deactivated.');
            },
            error: (err: any) => {
              console.error('Error deactivating debit card:', err);
              this.alertService.cardError('Card Deactivation Failed', 'Failed to deactivate card. Please try again.');
            }
          });
        });
      }
    );
  }

  toggleReplaceForm() {
    this.showReplaceForm = !this.showReplaceForm;
    this.replaceReason = '';
  }

  requestCardReplacement() {
    if (!this.card) return;
    
    if (!this.replaceReason.trim()) {
      this.alertService.validationError('Please provide a reason for card replacement');
      return;
    }
    
    // Create replacement request matching backend model
    const replacementRequest = {
      userId: this.card.userId,
      userName: this.card.userName,
      userEmail: this.userProfile?.email || '',
      accountNumber: this.card.userAccountNumber,
      currentCardNumber: this.card.cardNumber,
      reason: this.replaceReason,
      status: 'Pending'
    };
    
    // Submit replacement request to MySQL database
    this.http.post(`${environment.apiBaseUrl}/card-replacement-requests/create`, replacementRequest).subscribe({
      next: (response: any) => {
        console.log('Card replacement request created in MySQL:', response);
        
        // Also save to localStorage as backup
        const savedRequests = localStorage.getItem('card_replacement_requests');
        let requests = savedRequests ? JSON.parse(savedRequests) : [];
        requests.push(replacementRequest);
        localStorage.setItem('card_replacement_requests', JSON.stringify(requests));
        
        this.alertService.success('Card Replacement Request Submitted', 'Admin will review your request and issue a new card.');
        this.showReplaceForm = false;
        this.replaceReason = '';
      },
      error: (err: any) => {
        console.error('Error creating card replacement request:', err);
        this.alertService.operationError('Card replacement request submission');
        
        // Fallback to localStorage
        const savedRequests = localStorage.getItem('card_replacement_requests');
        let requests = savedRequests ? JSON.parse(savedRequests) : [];
        requests.push(replacementRequest);
        localStorage.setItem('card_replacement_requests', JSON.stringify(requests));
      }
    });
  }

  applyNewCard() {
    if (!this.userProfile) return;
    
    const newCardRequest = {
      userId: this.userProfile.accountNumber,
      userName: this.userProfile.name,
      userEmail: this.userProfile.email,
      accountNumber: this.userProfile.accountNumber,
      cardType: 'Visa Debit',
      reason: 'Additional card request',
      status: 'Pending'
    };
    
    // Submit new card request to MySQL database
    this.http.post(`${environment.apiBaseUrl}/new-card-requests/create`, newCardRequest).subscribe({
      next: (response: any) => {
        console.log('New card request created in MySQL:', response);
        
        // Also save to localStorage as backup
        const savedRequests = localStorage.getItem('new_card_requests');
        let requests = savedRequests ? JSON.parse(savedRequests) : [];
        requests.push(newCardRequest);
        localStorage.setItem('new_card_requests', JSON.stringify(requests));
        
        this.alertService.success('New Card Application Submitted', 'You will receive your new card within 5-7 business days.');
      },
      error: (err: any) => {
        console.error('Error creating new card request:', err);
        this.alertService.operationError('New card application submission');
        
        // Fallback to localStorage
        const savedRequests = localStorage.getItem('new_card_requests');
        let requests = savedRequests ? JSON.parse(savedRequests) : [];
        requests.push(newCardRequest);
        localStorage.setItem('new_card_requests', JSON.stringify(requests));
      }
    });
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  get maskedNumber() {
    return this.card ? '**** **** **** ' + this.card.cardNumber.slice(-4) : '**** **** **** ****';
  }

  get fullNumber() {
    return this.showDetails && this.card ? this.card.cardNumber : this.maskedNumber;
  }

  get maskedCVV() {
    return this.showCVV && this.card ? this.card.cvv : '***';
  }

  get statusClass() {
    if (!this.card) return '';
    switch (this.card.status) {
      case 'Active': return 'status-active';
      case 'Blocked': return 'status-blocked';
      case 'Replaced': return 'status-replaced';
      case 'Deactivated': return 'status-deactivated';
      default: return '';
    }
  }

  flipCard() {
    this.isCardFlipped = !this.isCardFlipped;
  }

  openApplyCreditForm() {
    this.showApplyCreditForm = true;
    // prefill PAN if available
    if (this.userProfile && this.userProfile.pan) {
      this.panInput = this.userProfile.pan;
    }
  }

  closeApplyCreditForm() {
    this.showApplyCreditForm = false;
    this.panInput = '';
    this.incomeInput = null;
    this.predictedCibil = null;
    this.suggestedLimit = null;
    this.panBasedLimit = null;
    this.cardType = null;
    this.eligibility = null;
    this.interestRate = null;
    this.annualFee = null;
    this.showTermsModal = false;
    this.cibilReportData = null;
  }

  // Quick client-side prediction call (backend also predicts)
  previewCreditPrediction() {
    if (!this.panInput) {
      this.alertService.validationError('Please enter PAN to preview');
      return;
    }
    
    // Validate PAN format
    if (this.panInput.length !== 10) {
      this.alertService.validationError('PAN must be 10 characters long');
      return;
    }
    
    // Call preview endpoint to get prediction without creating request
    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/preview?pan=${this.panInput}&income=${this.incomeInput || 0}`, {}).subscribe({
      next: (res: any) => {
        this.predictedCibil = res.predictedCibil;
        this.suggestedLimit = res.suggestedLimit;
        this.panBasedLimit = res.panBasedLimit;
        this.cardType = res.cardType;
        this.eligibility = res.eligibility;
        this.interestRate = res.interestRate;
        this.annualFee = res.annualFee;
        this.showTermsModal = true; // show T&C before final submit
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
    if (!this.panInput) {
      this.alertService.validationError('PAN is required');
      return;
    }
    const payload: any = {
      pan: this.panInput,
      annualIncome: this.incomeInput || this.userProfile?.income || 0
    };
    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/create`, payload, {
      headers: this.getUserAuthHeaders()
    }).subscribe({
      next: (res: any) => {
        this.alertService.success('Application Submitted', 'Your credit card application has been submitted for review.');
        this.closeApplyCreditForm();
      },
      error: (err: any) => {
        console.error('Error submitting credit application:', err);
        this.alertService.operationError('Credit application', err?.message || 'Submission failed');
      }
    });
  }
}
