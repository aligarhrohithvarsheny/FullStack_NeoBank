import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
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

interface CardReplacementRequest {
  id: string;
  originalCardId: string;
  userId: string;
  userName: string;
  userAccountNumber: string;
  reason: string;
  status: 'Pending' | 'Approved' | 'Rejected';
  requestedDate: string;
  processedDate?: string;
  processedBy?: string;
  newCardNumber?: string;
}

interface NewCardRequest {
  id: string;
  userId: string;
  userName: string;
  userAccountNumber: string;
  cardType: string;
  status: 'Pending' | 'Approved' | 'Rejected';
  requestedDate: string;
  processedDate?: string;
  processedBy?: string;
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
  selector: 'app-cards',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cards.html',
  styleUrls: ['./cards.css']
})
export class Cards implements OnInit {
  cards: CardDetails[] = [];
  replacementRequests: CardReplacementRequest[] = [];
  newCardRequests: NewCardRequest[] = [];
  filteredCards: CardDetails[] = [];
  statusFilter: string = 'All';
  searchTerm: string = '';
  showRequests: boolean = false;

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}

  ngOnInit() {
    this.loadCards();
    this.loadRequests();
  }

  loadCards() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load cards from MySQL database
    console.log('Loading cards from MySQL database...');
    this.http.get('http://localhost:8080/api/cards?page=0&size=100').subscribe({
      next: (response: any) => {
        console.log('Cards loaded from MySQL:', response);
        console.log('Response type:', typeof response);
        console.log('Response length:', response?.length || 'N/A');
        if (response.content) {
          this.cards = response.content.map((card: any) => ({
            id: card.id.toString(),
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
          }));
        } else {
          this.cards = response.map((card: any) => ({
            id: card.id.toString(),
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
          }));
        }
        
        // Also save to localStorage as backup
        this.saveCards();
        this.applyFilters();
      },
      error: (err: any) => {
        console.error('Error loading cards from database:', err);
        // Fallback to localStorage
        const savedCards = localStorage.getItem('user_cards');
        if (savedCards) {
          this.cards = JSON.parse(savedCards);
        } else {
          // No cards found in database - show empty list
          this.cards = [];
          console.log('No cards found in database');
        }
        this.applyFilters();
      }
    });
  }

  createSampleCards() {
    const sampleCards: CardDetails[] = [
      {
        id: 'CARD_SAMPLE_1',
        userId: 'ACC001',
        userName: 'John Doe',
        userAccountNumber: 'ACC001',
        cardNumber: '4111111111111234',
        cardType: 'Visa Debit',
        cvv: '123',
        expiry: '12/28',
        status: 'Active',
        pinSet: true,
        createdAt: new Date(Date.now() - 86400000).toISOString(),
        lastUpdated: new Date(Date.now() - 86400000).toISOString()
      },
      {
        id: 'CARD_SAMPLE_2',
        userId: 'ACC002',
        userName: 'Jane Smith',
        userAccountNumber: 'ACC002',
        cardNumber: '5555555555554444',
        cardType: 'Mastercard Debit',
        cvv: '456',
        expiry: '08/29',
        status: 'Blocked',
        pinSet: false,
        createdAt: new Date(Date.now() - 172800000).toISOString(),
        lastUpdated: new Date(Date.now() - 86400000).toISOString()
      }
    ];
    
    this.cards = sampleCards;
    this.saveCards();
  }

  loadRequests() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load replacement requests
    const savedReplaceRequests = localStorage.getItem('card_replacement_requests');
    this.replacementRequests = savedReplaceRequests ? JSON.parse(savedReplaceRequests) : [];
    
    // Load new card requests
    const savedNewCardRequests = localStorage.getItem('new_card_requests');
    this.newCardRequests = savedNewCardRequests ? JSON.parse(savedNewCardRequests) : [];
  }

  saveCards() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('user_cards', JSON.stringify(this.cards));
  }

  applyFilters() {
    this.filteredCards = this.cards.filter(card => {
      const matchesStatus = this.statusFilter === 'All' || card.status === this.statusFilter;
      const matchesSearch = !this.searchTerm || 
        card.userName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        card.userAccountNumber.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        card.cardNumber.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        card.cardType.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      return matchesStatus && matchesSearch;
    });
  }

  onStatusFilterChange() {
    this.applyFilters();
  }

  onSearchChange() {
    this.applyFilters();
  }

  toggleRequestsView() {
    this.showRequests = !this.showRequests;
  }

  assignCard(userAccountNumber: string) {
    const userProfile = this.getUserProfile(userAccountNumber);
    if (!userProfile) {
      alert('User profile not found');
      return;
    }

    const cardId = 'CARD_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    const cardNumber = '4' + Math.floor(100000000000000 + Math.random() * 900000000000000).toString();
    const cvv = Math.floor(100 + Math.random() * 900).toString();
    const expiryMonth = String(Math.floor(1 + Math.random() * 12)).padStart(2, '0');
    const expiryYear = String(new Date().getFullYear() + Math.floor(1 + Math.random() * 5));

    const newCard: CardDetails = {
      id: cardId,
      userId: userAccountNumber,
      userName: userProfile.name,
      userAccountNumber: userAccountNumber,
      cardNumber: cardNumber,
      cardType: 'Visa Debit',
      cvv: cvv,
      expiry: `${expiryMonth}/${expiryYear.slice(-2)}`,
      status: 'Active',
      pinSet: false,
      createdAt: new Date().toISOString(),
      lastUpdated: new Date().toISOString()
    };

    // Submit new card to MySQL database
    this.http.post('http://localhost:8080/api/cards', newCard).subscribe({
      next: (response: any) => {
        console.log('Card created in MySQL:', response);
        
        this.cards.push(newCard);
        this.saveCards(); // Also save to localStorage as backup
        this.applyFilters();
        
        alert(`Visa Debit card assigned to ${userProfile.name} (${userAccountNumber}). Card No: **** **** **** ${cardNumber.slice(-4)}`);
      },
      error: (err: any) => {
        console.error('Error creating card:', err);
        alert('Failed to assign card. Please try again.');
        
        // Fallback to localStorage
        this.cards.push(newCard);
        this.saveCards();
        this.applyFilters();
      }
    });
  }

  blockCard(card: CardDetails) {
    if (confirm(`Block card ending with ${card.cardNumber.slice(-4)} for ${card.userName}?`)) {
      card.status = 'Blocked';
      card.lastUpdated = new Date().toISOString();
      this.saveCards();
      this.applyFilters();
      alert(`Card ending with ${card.cardNumber.slice(-4)} has been blocked.`);
    }
  }

  unblockCard(card: CardDetails) {
    if (confirm(`Unblock card ending with ${card.cardNumber.slice(-4)} for ${card.userName}?`)) {
      card.status = 'Active';
      card.lastUpdated = new Date().toISOString();
      this.saveCards();
      this.applyFilters();
      alert(`Card ending with ${card.cardNumber.slice(-4)} has been unblocked.`);
    }
  }

  replaceCard(card: CardDetails) {
    if (confirm(`Replace card ending with ${card.cardNumber.slice(-4)} for ${card.userName}?`)) {
      const newCardNumber = '4' + Math.floor(100000000000000 + Math.random() * 900000000000000).toString();
      const newCvv = Math.floor(100 + Math.random() * 900).toString();
      const expiryMonth = String(Math.floor(1 + Math.random() * 12)).padStart(2, '0');
      const expiryYear = String(new Date().getFullYear() + Math.floor(1 + Math.random() * 5));

      card.cardNumber = newCardNumber;
      card.cvv = newCvv;
      card.expiry = `${expiryMonth}/${expiryYear.slice(-2)}`;
      card.status = 'Active';
      card.pinSet = false;
      card.lastUpdated = new Date().toISOString();
      
      this.saveCards();
      this.applyFilters();
      alert(`Card replaced for ${card.userName}. New card ending with ${newCardNumber.slice(-4)}`);
    }
  }

  updateCardInBackend(card: CardDetails, newCardNumber: string) {
    const newCvv = Math.floor(100 + Math.random() * 900).toString();
    const expiryMonth = String(Math.floor(1 + Math.random() * 12)).padStart(2, '0');
    const expiryYear = String(new Date().getFullYear() + Math.floor(1 + Math.random() * 5));

    // Update card in backend
    this.http.put(`http://localhost:8080/api/cards/${card.id}/replace`, {
      cardNumber: newCardNumber,
      cvv: newCvv,
      expiryDate: `${expiryMonth}/${expiryYear.slice(-2)}`,
      status: 'Active',
      pinSet: false
    }).subscribe({
      next: (response: any) => {
        console.log('Card updated in backend:', response);
        
        // Update local card data
        card.cardNumber = newCardNumber;
        card.cvv = newCvv;
        card.expiry = `${expiryMonth}/${expiryYear.slice(-2)}`;
        card.status = 'Active';
        card.pinSet = false;
        card.lastUpdated = new Date().toISOString();
        
        this.saveCards();
        this.applyFilters();
      },
      error: (err: any) => {
        console.error('Error updating card in backend:', err);
        alert('Failed to update card in backend. Please try again.');
      }
    });
  }

  deactivateCard(card: CardDetails) {
    if (confirm(`Deactivate card ending with ${card.cardNumber.slice(-4)} for ${card.userName}? This action cannot be undone.`)) {
      card.status = 'Deactivated';
      card.lastUpdated = new Date().toISOString();
      this.saveCards();
      this.applyFilters();
      alert(`Card ending with ${card.cardNumber.slice(-4)} has been deactivated.`);
    }
  }

  approveReplacementRequest(request: CardReplacementRequest) {
    if (confirm(`Approve card replacement request for ${request.userName}?`)) {
      // First approve the request in the backend
      this.http.put(`http://localhost:8080/api/card-replacement-requests/approve/${request.id}?adminName=Admin`, {}).subscribe({
        next: (response: any) => {
          console.log('Card replacement request approved in MySQL:', response);
          
          // Update local request
          request.status = 'Approved';
          request.processedDate = new Date().toISOString();
          request.processedBy = 'Admin';
          request.newCardNumber = response.newCardNumber;
          
          // Find and update the card in backend
          const card = this.cards.find(c => c.userAccountNumber === request.userAccountNumber);
          if (card) {
            this.updateCardInBackend(card, response.newCardNumber);
          }
          
          this.saveReplacementRequests();
          alert(`Card replacement approved for ${request.userName}. New card number: ${response.newCardNumber}`);
        },
        error: (err: any) => {
          console.error('Error approving card replacement request:', err);
          alert('Failed to approve card replacement request. Please try again.');
        }
      });
    }
  }

  rejectReplacementRequest(request: CardReplacementRequest) {
    if (confirm(`Reject card replacement request for ${request.userName}?`)) {
      request.status = 'Rejected';
      request.processedDate = new Date().toISOString();
      request.processedBy = 'Admin';
      
      this.saveReplacementRequests();
      alert(`Card replacement request rejected for ${request.userName}`);
    }
  }

  approveNewCardRequest(request: NewCardRequest) {
    if (confirm(`Approve new card request for ${request.userName}?`)) {
      request.status = 'Approved';
      request.processedDate = new Date().toISOString();
      request.processedBy = 'Admin';
      
      // Assign new card
      this.assignCard(request.userAccountNumber);
      
      this.saveNewCardRequests();
      alert(`New card approved for ${request.userName}`);
    }
  }

  rejectNewCardRequest(request: NewCardRequest) {
    if (confirm(`Reject new card request for ${request.userName}?`)) {
      request.status = 'Rejected';
      request.processedDate = new Date().toISOString();
      request.processedBy = 'Admin';
      
      this.saveNewCardRequests();
      alert(`New card request rejected for ${request.userName}`);
    }
  }

  saveReplacementRequests() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('card_replacement_requests', JSON.stringify(this.replacementRequests));
  }

  saveNewCardRequests() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('new_card_requests', JSON.stringify(this.newCardRequests));
  }

  getUserProfile(accountNumber: string): UserProfile | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      const profile: UserProfile = JSON.parse(savedProfile);
      if (profile.accountNumber === accountNumber) {
        return profile;
      }
    }
    return null;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Active': return 'status-active';
      case 'Blocked': return 'status-blocked';
      case 'Replaced': return 'status-replaced';
      case 'Deactivated': return 'status-deactivated';
      default: return '';
    }
  }

  getRequestStatusClass(status: string): string {
    switch (status) {
      case 'Pending': return 'request-pending';
      case 'Approved': return 'request-approved';
      case 'Rejected': return 'request-rejected';
      default: return '';
    }
  }

  // Computed properties for statistics
  get totalCards(): number {
    return this.cards.length;
  }

  get activeCards(): number {
    return this.cards.filter(c => c.status === 'Active').length;
  }

  get blockedCards(): number {
    return this.cards.filter(c => c.status === 'Blocked').length;
  }

  get pendingRequests(): number {
    return this.replacementRequests.filter(r => r.status === 'Pending').length + 
           this.newCardRequests.filter(r => r.status === 'Pending').length;
  }

  trackByCardId(index: number, card: CardDetails): string {
    return card.id;
  }

  trackByRequestId(index: number, request: any): string {
    return request.id;
  }

  goBack() {
    this.router.navigate(['admin/dashboard']);
  }
}
