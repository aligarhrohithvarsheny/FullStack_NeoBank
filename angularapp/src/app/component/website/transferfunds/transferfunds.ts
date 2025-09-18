import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface TransferRecord {
  id: string;
  accountNumber: string;
  name: string;
  phone: string;
  ifsc: string;
  amount: number;
  transferType: 'NEFT' | 'RTGS';
  date: Date;
}

@Component({
  selector: 'app-transferfunds',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transferfunds.html',
  styleUrls: ['./transferfunds.css']
})
export class Transferfunds {
  accountNumber: string = '';
  name: string = '';
  phone: string = '';
  ifsc: string = '';
  amount: number = 0;
  transferType: 'NEFT' | 'RTGS' = 'NEFT';

  transfers: TransferRecord[] = [];
  transactionSuccess: boolean = false;

  constructor(private router: Router) {}

  // Back to dashboard
  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  // Handle fund transfer
  transferFunds() {
    if (!this.accountNumber || !this.name || !this.phone || !this.ifsc || this.amount <= 0) {
      alert('Please fill all fields correctly!');
      return;
    }

    const newTransfer: TransferRecord = {
      id: 'TXN' + Math.floor(Math.random() * 1000000),
      accountNumber: this.accountNumber,
      name: this.name,
      phone: this.phone,
      ifsc: this.ifsc,
      amount: this.amount,
      transferType: this.transferType,
      date: new Date()
    };

    this.transfers.unshift(newTransfer);
    this.transactionSuccess = true;

    // Reset form
    this.accountNumber = '';
    this.name = '';
    this.phone = '';
    this.ifsc = '';
    this.amount = 0;
    this.transferType = 'NEFT';

    // Optional: push to main transaction table if you have a service or parent component
    // e.g., transactionService.addTransaction({ ... });
  }
}
