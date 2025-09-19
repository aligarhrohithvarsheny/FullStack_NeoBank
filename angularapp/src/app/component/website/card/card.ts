import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';

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
  styleUrls: ['./card.css']
})
export class Card implements OnInit {
  showDetails: boolean = false;
  newPin: string = '';
  userProfile: UserProfile | null = null;
  card: CardDetails | null = null;
  showPinForm: boolean = false;
  showReplaceForm: boolean = false;
  replaceReason: string = '';

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    this.loadUserProfile();
    this.loadUserCard();
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      this.userProfile = JSON.parse(savedProfile);
    }
  }

  loadUserCard() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const savedCards = localStorage.getItem('user_cards');
    if (savedCards && this.userProfile) {
      const cards: CardDetails[] = JSON.parse(savedCards);
      const userCard = cards.find(card => card.userAccountNumber === this.userProfile?.accountNumber);
      
      if (userCard) {
        this.card = userCard;
      } else {
        // Create a default card if none exists
        this.createDefaultCard();
      }
    } else {
      this.createDefaultCard();
    }
  }

  createDefaultCard() {
    if (!this.userProfile) return;
    
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

  togglePinForm() {
    this.showPinForm = !this.showPinForm;
    this.newPin = '';
  }

  setPin() {
    if (!this.card) return;
    
    if (this.newPin.length !== 4) {
      alert('PIN must be 4 digits');
      return;
    }
    
    if (!/^\d{4}$/.test(this.newPin)) {
      alert('PIN must contain only numbers');
      return;
    }
    
    this.card.pinSet = true;
    this.card.lastUpdated = new Date().toISOString();
    this.saveCard();
    
    alert('PIN Set Successfully ✅');
    this.newPin = '';
    this.showPinForm = false;
  }

  blockCard() {
    if (!this.card) return;
    
    if (confirm('Are you sure you want to block this card? This action can be reversed by admin.')) {
      this.card.status = 'Blocked';
      this.card.lastUpdated = new Date().toISOString();
      this.saveCard();
      alert('Card Blocked ❌');
    }
  }

  deactivateCard() {
    if (!this.card) return;
    
    if (confirm('Are you sure you want to deactivate this card? This action cannot be undone.')) {
      this.card.status = 'Deactivated';
      this.card.lastUpdated = new Date().toISOString();
      this.saveCard();
      alert('Card Deactivated ⚠️');
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
    
    // Create replacement request
    const replacementRequest = {
      id: 'REPLACE_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9),
      originalCardId: this.card.id,
      userId: this.card.userId,
      userName: this.card.userName,
      userAccountNumber: this.card.userAccountNumber,
      reason: this.replaceReason,
      status: 'Pending',
      requestedDate: new Date().toISOString()
    };
    
    // Save replacement request
    const savedRequests = localStorage.getItem('card_replacement_requests');
    let requests = savedRequests ? JSON.parse(savedRequests) : [];
    requests.push(replacementRequest);
    localStorage.setItem('card_replacement_requests', JSON.stringify(requests));
    
    alert('Card Replacement Request Submitted! 🎉\nAdmin will review your request and issue a new card.');
    this.showReplaceForm = false;
    this.replaceReason = '';
  }

  applyNewCard() {
    if (!this.userProfile) return;
    
    const newCardRequest = {
      id: 'NEW_CARD_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9),
      userId: this.userProfile.accountNumber,
      userName: this.userProfile.name,
      userAccountNumber: this.userProfile.accountNumber,
      cardType: 'Visa Debit',
      status: 'Pending',
      requestedDate: new Date().toISOString()
    };
    
    // Save new card request
    const savedRequests = localStorage.getItem('new_card_requests');
    let requests = savedRequests ? JSON.parse(savedRequests) : [];
    requests.push(newCardRequest);
    localStorage.setItem('new_card_requests', JSON.stringify(requests));
    
    alert('New Card Application Submitted! 🎉\nYou will receive your new card within 5-7 business days.');
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
    return this.showDetails && this.card ? this.card.cvv : '***';
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
}
