import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VirtualCardService } from '../../../service/virtual-card.service';

@Component({
  selector: 'app-virtual-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './virtual-card.html',
  styleUrls: ['./virtual-card.css']
})
export class VirtualCardComponent implements OnInit {
  cards: any[] = [];
  selectedCard: any = null;
  loading = false;
  creating = false;
  showCreateForm = false;
  accountNumber = '';
  showCvv = false;

  newCard = { cardholderName: '', dailyLimit: 50000, monthlyLimit: 200000 };

  constructor(private virtualCardService: VirtualCardService) {}

  ngOnInit(): void {
    const user = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
    this.accountNumber = user.accountNumber || '';
    this.newCard.cardholderName = user.name || '';
    if (this.accountNumber) {
      this.loadCards();
    }
  }

  loadCards(): void {
    this.loading = true;
    this.virtualCardService.getByAccountNumber(this.accountNumber).subscribe({
      next: (res: any) => {
        this.cards = res.data || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  createCard(): void {
    this.creating = true;
    this.virtualCardService.create(this.accountNumber, this.newCard.cardholderName).subscribe({
      next: () => {
        this.creating = false;
        this.showCreateForm = false;
        this.loadCards();
      },
      error: () => { this.creating = false; }
    });
  }

  selectCard(card: any): void {
    this.selectedCard = this.selectedCard?.id === card.id ? null : card;
    this.showCvv = false;
  }

  toggleStatus(card: any): void {
    if (card.status === 'ACTIVE') {
      this.virtualCardService.freeze(card.id).subscribe({ next: () => this.loadCards() });
    } else if (card.status === 'FROZEN') {
      this.virtualCardService.unfreeze(card.id).subscribe({ next: () => this.loadCards() });
    }
  }

  cancelCard(card: any): void {
    if (confirm('Are you sure you want to cancel this virtual card? This action cannot be undone.')) {
      this.virtualCardService.cancel(card.id).subscribe({ next: () => { this.selectedCard = null; this.loadCards(); } });
    }
  }

  toggleOnlinePayments(card: any): void {
    this.virtualCardService.toggleOnline(card.id, !card.onlinePaymentsEnabled).subscribe({ next: () => this.loadCards() });
  }

  toggleInternationalPayments(card: any): void {
    this.virtualCardService.toggleInternational(card.id, !card.internationalPaymentsEnabled).subscribe({ next: () => this.loadCards() });
  }

  updateLimits(card: any): void {
    this.virtualCardService.updateLimits(card.id, card.dailyLimit, card.monthlyLimit).subscribe({
      next: () => this.loadCards()
    });
  }

  formatCardNumber(num: string): string {
    if (!num) return '';
    return num.replace(/(.{4})/g, '$1 ').trim();
  }

  maskCardNumber(num: string): string {
    if (!num) return '';
    return '**** **** **** ' + num.slice(-4);
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = { 'ACTIVE': 'status-active', 'FROZEN': 'status-frozen', 'CANCELLED': 'status-cancelled' };
    return classes[status] || '';
  }

  formatDate(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  formatCurrency(amount: number): string {
    return '₹' + (amount || 0).toLocaleString('en-IN');
  }
}
