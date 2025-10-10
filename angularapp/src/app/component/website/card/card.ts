import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';

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
  aadhar?: string;
  occupation?: string;
  income?: number;
}

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './card.html',
  styleUrls: ['./card.css'],
  encapsulation: ViewEncapsulation.None
})
export class Card implements OnInit {
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

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}

  ngOnInit() {
    this.loadUserProfile();
    this.loadUserCard();
  }

  // Method to refresh card data (can be called when user returns to page)
  refreshCardData() {
    this.loadUserCard();
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      
      // Load full user profile from MySQL database
      this.http.get(`http://localhost:8080/api/users/${user.id}`).subscribe({
        next: (userData: any) => {
          console.log('User profile loaded from MySQL:', userData);
          this.userProfile = {
            name: userData.account?.name || userData.username,
            email: userData.email,
            accountNumber: userData.accountNumber,
            phoneNumber: userData.account?.phone || '',
            address: userData.account?.address || '',
            dateOfBirth: userData.account?.dob || '',
            accountType: 'Savings Account',
            joinDate: userData.createdAt ? new Date(userData.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
            pan: userData.account?.pan || '',
            aadhar: userData.account?.aadharNumber || '',
            occupation: userData.account?.occupation || '',
            income: userData.account?.income || 0
          };
          
          // Load user card after profile is loaded
          this.loadUserCard();
        },
        error: (err: any) => {
          console.error('Error loading user profile from MySQL:', err);
          // Use fallback profile instead of showing alert
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
            aadhar: '',
            occupation: '',
            income: 0
          };
          
          // Load user card after setting fallback profile
          this.loadUserCard();
        }
      });
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
          aadhar: '',
          occupation: '',
          income: 0
        };
        console.log('Using default profile - no fallback found');
      }
      
      // Load user card after setting profile
      this.loadUserCard();
    }
  }

  loadUserCard() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    if (!this.userProfile) {
      this.createDefaultCard();
      return;
    }
    
    // Load card from MySQL database
    console.log('Loading card for account:', this.userProfile.accountNumber);
    this.http.get(`http://localhost:8080/api/cards/account/${this.userProfile.accountNumber}`).subscribe({
      next: (cards: any) => {
        console.log('Cards loaded from MySQL:', cards);
        console.log('Number of cards found:', cards?.length || 0);
        if (cards && cards.length > 0) {
          const backendCard = cards[0]; // Get the first card
          this.card = {
            id: backendCard.id.toString(),
            userId: backendCard.accountNumber,
            userName: backendCard.userName,
            userAccountNumber: backendCard.accountNumber,
            cardNumber: backendCard.cardNumber,
            cardType: backendCard.cardType,
            cvv: backendCard.cvv,
            expiry: backendCard.expiryDate,
            status: backendCard.status,
            pinSet: backendCard.pinSet,
            createdAt: backendCard.createdAt || new Date().toISOString(),
            lastUpdated: backendCard.lastUpdated || new Date().toISOString()
          };
          
          // Card loaded successfully from MySQL
        } else {
          // No card found in database, create default card
          this.createDefaultCard();
        }
      },
      error: (err: any) => {
        console.error('Error loading card from database:', err);
        console.error('Account number being searched:', this.userProfile?.accountNumber);
        console.log(`No card found for account ${this.userProfile?.accountNumber}. Please contact admin to create a card.`);
        this.createDefaultCard();
      }
    });
  }

  createDefaultCard() {
    if (!this.userProfile) return;
    
    // First check if card already exists in database
    console.log('Checking if card already exists for account:', this.userProfile.accountNumber);
    this.http.get(`http://localhost:8080/api/cards/account/${this.userProfile.accountNumber}`).subscribe({
      next: (existingCards: any) => {
        if (existingCards && existingCards.length > 0) {
          console.log('Card already exists, using existing card');
          const existingCard = existingCards[0];
          this.card = {
            id: existingCard.id.toString(),
            userId: existingCard.accountNumber,
            userName: existingCard.userName,
            userAccountNumber: existingCard.accountNumber,
            cardNumber: existingCard.cardNumber,
            cardType: existingCard.cardType,
            cvv: existingCard.cvv,
            expiry: existingCard.expiryDate,
            status: existingCard.status,
            pinSet: existingCard.pinSet,
            createdAt: existingCard.createdAt || new Date().toISOString(),
            lastUpdated: existingCard.lastUpdated || new Date().toISOString()
          };
          this.saveCard();
        } else {
          // No card exists, create new one
          this.createNewCard();
        }
      },
      error: (err: any) => {
        console.error('Error checking existing cards:', err);
        // If check fails, create new card
        this.createNewCard();
      }
    });
  }

  createNewCard() {
    if (!this.userProfile) return;
    
    const cardNumber = '4' + Math.floor(100000000000000 + Math.random() * 900000000000000).toString();
    const cvv = Math.floor(100 + Math.random() * 900).toString();
    const expiryMonth = String(Math.floor(1 + Math.random() * 12)).padStart(2, '0');
    const expiryYear = String(new Date().getFullYear() + Math.floor(1 + Math.random() * 5));
    
    const newCard = {
      cardNumber: cardNumber,
      cardType: 'Visa Debit',
      cvv: cvv,
      userName: this.userProfile.name,
      expiryDate: `${expiryMonth}/${expiryYear.slice(-2)}`,
      pin: '',
      blocked: false,
      deactivated: false,
      pinSet: false,
      status: 'Active',
      accountNumber: this.userProfile.accountNumber,
      userEmail: this.userProfile.email
    };
    
    // Create card in MySQL database
    console.log('Creating new card in MySQL for account:', this.userProfile.accountNumber);
    this.http.post('http://localhost:8080/api/cards', newCard).subscribe({
      next: (response: any) => {
        console.log('Card created successfully in MySQL:', response);
        
        // Update local card with the response from database
        if (this.userProfile) {
          this.card = {
            id: response.id.toString(),
            userId: this.userProfile.accountNumber,
            userName: this.userProfile.name,
            userAccountNumber: this.userProfile.accountNumber,
            cardNumber: response.cardNumber,
            cardType: response.cardType,
            cvv: response.cvv,
            expiry: response.expiryDate,
            status: response.status,
            pinSet: response.pinSet,
            createdAt: response.createdAt || new Date().toISOString(),
            lastUpdated: response.lastUpdated || new Date().toISOString()
          };
        }
        
        // Also save to localStorage as backup
        this.saveCard();
      },
      error: (err: any) => {
        console.error('Error creating card in MySQL:', err);
        // Fallback to localStorage if database fails
        this.createLocalCard();
      }
    });
  }

  createLocalCard() {
    if (!this.userProfile) return;
    
    // Fallback method to create card in localStorage only
    const cardId = 'CARD_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    const cardNumber = '4' + Math.floor(100000000000000 + Math.random() * 900000000000000).toString();
    const cvv = Math.floor(100 + Math.random() * 900).toString();
    const expiryMonth = String(Math.floor(1 + Math.random() * 12)).padStart(2, '0');
    const expiryYear = String(new Date().getFullYear() + Math.floor(1 + Math.random() * 5));
    
    this.card = {
      id: cardId,
      userId: this.userProfile.accountNumber,
      userName: this.userProfile.name,
      userAccountNumber: this.userProfile.accountNumber,
      cardNumber: cardNumber,
      cardType: 'Visa Debit',
      cvv: cvv,
      expiry: `${expiryMonth}/${expiryYear.slice(-2)}`,
      status: 'Active',
      pinSet: false,
      createdAt: new Date().toISOString(),
      lastUpdated: new Date().toISOString()
    };
    
    this.saveCard();
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
      alert('PIN must be 4 digits');
      return;
    }
    
    if (!/^\d{4}$/.test(this.newPin)) {
      alert('PIN must contain only numbers');
      return;
    }
    
    // First get the card from backend to get the correct Long ID
    this.http.get(`http://localhost:8080/api/cards/account/${this.userProfile.accountNumber}`).subscribe({
      next: (cards: any) => {
        if (cards && cards.length > 0) {
          const backendCard = cards[0]; // Get the first card
          // Update card PIN in MySQL database using correct endpoint
          this.http.put(`http://localhost:8080/api/cards/${backendCard.id}/pin?pin=${this.newPin}`, {}).subscribe({
            next: (response: any) => {
              console.log('Card PIN updated in MySQL:', response);
              this.card!.pinSet = true;
              this.card!.lastUpdated = new Date().toISOString();
              this.saveCard(); // Also save to localStorage as backup
              
              alert('PIN Set Successfully ✅');
              this.newPin = '';
              this.showPinForm = false;
            },
            error: (err: any) => {
              console.error('Error updating card PIN:', err);
              alert('Failed to set PIN. Please try again.');
            }
          });
        } else {
          console.log('No card found in database. Please contact admin.');
        }
      },
      error: (err: any) => {
        console.error('Error getting card from backend:', err);
        console.error('Full error details:', err);
        console.error('Account number being searched:', this.userProfile?.accountNumber);
        alert(`Failed to get card information for account ${this.userProfile?.accountNumber}. Error: ${err.message || 'Unknown error'}`);
      }
    });
  }

  resetPin() {
    if (!this.card || !this.userProfile) return;
    
    if (this.newPin.length !== 4) {
      alert('New PIN must be 4 digits');
      return;
    }
    
    if (!/^\d{4}$/.test(this.newPin)) {
      alert('New PIN must contain only numbers');
      return;
    }
    
    if (confirm('Are you sure you want to reset your PIN? This will disable your current PIN and set a new one.')) {
      // First get the card from backend to get the correct Long ID
      this.http.get(`http://localhost:8080/api/cards/account/${this.userProfile.accountNumber}`).subscribe({
        next: (cards: any) => {
          if (cards && cards.length > 0) {
            const backendCard = cards[0]; // Get the first card
            // Reset card PIN in MySQL database using correct endpoint
            this.http.put(`http://localhost:8080/api/cards/${backendCard.id}/reset-pin?pin=${this.newPin}`, {}).subscribe({
              next: (response: any) => {
                console.log('Card PIN reset in MySQL:', response);
                this.card!.pinSet = true;
                this.card!.lastUpdated = new Date().toISOString();
                this.saveCard(); // Also save to localStorage as backup
                
                alert('PIN Reset Successfully ✅\nYour new PIN has been set and is ready to use.');
                this.newPin = '';
                this.showForgotPinForm = false;
              },
              error: (err: any) => {
                console.error('Error resetting card PIN:', err);
                alert('Failed to reset PIN. Please try again.');
              }
            });
          } else {
            console.log('No card found in database. Please contact admin.');
          }
        },
        error: (err: any) => {
          console.error('Error getting card from backend:', err);
          console.error('Full error details:', err);
          console.error('Account number being searched:', this.userProfile?.accountNumber);
          alert(`Failed to get card information for account ${this.userProfile?.accountNumber}. Error: ${err.message || 'Unknown error'}`);
        }
      });
    }
  }

  blockCard() {
    if (!this.card || !this.userProfile) return;
    
    if (confirm('Are you sure you want to block this card? This action can be reversed by admin.')) {
      // First get the card from backend to get the correct Long ID
      this.http.get(`http://localhost:8080/api/cards/account/${this.userProfile.accountNumber}`).subscribe({
        next: (cards: any) => {
          if (cards && cards.length > 0) {
            const backendCard = cards[0]; // Get the first card
            this.http.put(`http://localhost:8080/api/cards/${backendCard.id}/block`, {}).subscribe({
              next: (response: any) => {
                console.log('Card blocked in MySQL:', response);
                this.card!.status = 'Blocked';
                this.card!.lastUpdated = new Date().toISOString();
                this.saveCard(); // Also save to localStorage as backup
                alert('Card Blocked ❌');
              },
              error: (err: any) => {
                console.error('Error blocking card:', err);
                alert('Failed to block card. Please try again.');
              }
            });
          } else {
            console.log('No card found in database. Please contact admin.');
          }
        },
        error: (err: any) => {
          console.error('Error getting card from backend:', err);
          alert('Failed to get card information. Please try again.');
        }
      });
    }
  }

  deactivateCard() {
    if (!this.card || !this.userProfile) return;
    
    if (confirm('Are you sure you want to deactivate this card? This action cannot be undone.')) {
      // First get the card from backend to get the correct Long ID
      this.http.get(`http://localhost:8080/api/cards/account/${this.userProfile.accountNumber}`).subscribe({
        next: (cards: any) => {
          if (cards && cards.length > 0) {
            const backendCard = cards[0]; // Get the first card
            this.http.put(`http://localhost:8080/api/cards/${backendCard.id}/deactivate`, {}).subscribe({
              next: (response: any) => {
                console.log('Card deactivated in MySQL:', response);
                this.card!.status = 'Deactivated';
                this.card!.lastUpdated = new Date().toISOString();
                this.saveCard(); // Also save to localStorage as backup
                alert('Card Deactivated ⚠️');
              },
              error: (err: any) => {
                console.error('Error deactivating card:', err);
                alert('Failed to deactivate card. Please try again.');
              }
            });
          } else {
            console.log('No card found in database. Please contact admin.');
          }
        },
        error: (err: any) => {
          console.error('Error getting card from backend:', err);
          alert('Failed to get card information. Please try again.');
        }
      });
    }
  }

  toggleReplaceForm() {
    this.showReplaceForm = !this.showReplaceForm;
    this.replaceReason = '';
  }

  requestCardReplacement() {
    if (!this.card) return;
    
    if (!this.replaceReason.trim()) {
      alert('Please provide a reason for card replacement');
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
    this.http.post('http://localhost:8080/api/card-replacement-requests/create', replacementRequest).subscribe({
      next: (response: any) => {
        console.log('Card replacement request created in MySQL:', response);
        
        // Also save to localStorage as backup
        const savedRequests = localStorage.getItem('card_replacement_requests');
        let requests = savedRequests ? JSON.parse(savedRequests) : [];
        requests.push(replacementRequest);
        localStorage.setItem('card_replacement_requests', JSON.stringify(requests));
        
        alert('Card Replacement Request Submitted! 🎉\nAdmin will review your request and issue a new card.');
        this.showReplaceForm = false;
        this.replaceReason = '';
      },
      error: (err: any) => {
        console.error('Error creating card replacement request:', err);
        alert('Failed to submit replacement request. Please try again.');
        
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
    this.http.post('http://localhost:8080/api/new-card-requests/create', newCardRequest).subscribe({
      next: (response: any) => {
        console.log('New card request created in MySQL:', response);
        
        // Also save to localStorage as backup
        const savedRequests = localStorage.getItem('new_card_requests');
        let requests = savedRequests ? JSON.parse(savedRequests) : [];
        requests.push(newCardRequest);
        localStorage.setItem('new_card_requests', JSON.stringify(requests));
        
        alert('New Card Application Submitted! 🎉\nYou will receive your new card within 5-7 business days.');
      },
      error: (err: any) => {
        console.error('Error creating new card request:', err);
        alert('Failed to submit new card application. Please try again.');
        
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
}
