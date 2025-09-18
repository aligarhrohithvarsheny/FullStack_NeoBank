import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './card.html',
  styleUrls: ['./card.css']
})
export class Card {
  showDetails: boolean = false;
  newPin: string = '';
  username: string = 'User'; // default

  card = {
    cardType: 'Debit',
    cardNumber: '1234567812345678',
    cvv: '123',
    userName: 'John Doe',
    expiry: '12/28',
    blocked: false,
    deactivated: false,
    pinSet: false
  };

  constructor(private router: Router) {
    // Get username from navigation state
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state && nav.extras.state['username']) {
      this.username = nav.extras.state['username'];
      this.card.userName = this.username; // Update card with current user name
    }
  }

  toggleDetails() {
    this.showDetails = !this.showDetails;
  }

  setPin() {
    if (this.newPin.length !== 4) {
      alert('PIN must be 4 digits');
      return;
    }
    this.card.pinSet = true;
    alert('PIN Set Successfully ✅');
    this.newPin = '';
  }

  blockCard() {
    this.card.blocked = true;
    alert('Card Blocked ❌');
  }

  deactivateCard() {
    this.card.deactivated = true;
    alert('Card Deactivated ⚠️');
  }

  applyNewCard() {
    alert('New Card Application Submitted! 🎉\nYou will receive your new card within 5-7 business days.');
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  get maskedNumber() {
    return '**** **** **** ' + this.card.cardNumber.slice(-4);
  }

  get fullNumber() {
    return this.showDetails ? this.card.cardNumber : this.maskedNumber;
  }

  get maskedCVV() {
    return this.showDetails ? this.card.cvv : '***';
  }
}
