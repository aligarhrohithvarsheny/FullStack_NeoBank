import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-cards',
  templateUrl: './cards.html',
  styleUrls: ['./cards.css']
})
export class Cards {
  constructor(private router: Router) {}

  users = [
    {
      accountNumber: '1234567890',
      cardNumber: '4111111111111234',
      cardType: 'Visa Debit',
      isActive: true
    },
    {
      accountNumber: '9876543210',
      cardNumber: '',
      cardType: '',
      isActive: false
    }
  ];

  goBack() {
    this.router.navigate(['admin/dashboard']); // ✅ make sure /dashboard route exists
  }

  blockCard(user: any) {
    if (user.cardNumber) {
      user.isActive = false;
      alert(`Card ending with ${user.cardNumber.slice(-4)} has been blocked.`);
    } else {
      alert(`No card assigned yet for account ${user.accountNumber}.`);
    }
  }

  generatePin(user: any) {
    if (user.cardNumber) {
      alert(`PIN generated for card ending with ${user.cardNumber.slice(-4)}.`);
    } else {
      alert(`No card assigned yet for account ${user.accountNumber}.`);
    }
  }

  assignCard(user: any) {
    if (!user.cardNumber) {
      // generate random 16-digit card number
      const randomCard = '4' + Math.floor(100000000000000 + Math.random() * 900000000000000);
      user.cardNumber = randomCard.toString();
      user.cardType = 'Visa Debit';
      user.isActive = true;
      alert(`Visa Debit card assigned to account ${user.accountNumber}. Card No: **** **** **** ${user.cardNumber.slice(-4)}`);
    } else {
      alert(`User already has a card: **** ${user.cardNumber.slice(-4)}`);
    }
  }
}
