import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

interface TransactionRecord {
  id: string;
  merchant: string;
  amount: number;
  type: 'Debit' | 'Credit';
  balance: number;
  date: Date;
}

@Component({
  selector: 'app-transaction',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction.html',
  styleUrls: ['./transaction.css']
})
export class Transaction {
  transactions: TransactionRecord[] = [];
  currentBalance: number = 50000; // initial balance
  merchant: string = '';
  amount: number = 0;
  type: 'Debit' | 'Credit' = 'Debit';
  searchTerm: string = '';
  fromDate: string = '';
  toDate: string = '';

  constructor(private router: Router) {}

  // Navigate back to dashboard
  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  addTransaction() {
    if (!this.merchant || this.amount <= 0) {
      alert('Please enter valid merchant and amount!');
      return;
    }

    if (this.type === 'Debit' && this.amount > this.currentBalance) {
      alert('Insufficient balance!');
      return;
    }

    const newBalance = this.type === 'Debit'
      ? this.currentBalance - this.amount
      : this.currentBalance + this.amount;

    const newTransaction: TransactionRecord = {
      id: 'TXN' + Math.floor(Math.random() * 1000000),
      merchant: this.merchant,
      amount: this.amount,
      type: this.type,
      balance: newBalance,
      date: new Date()
    };

    this.transactions.unshift(newTransaction); // newest first
    this.currentBalance = newBalance;

    this.merchant = '';
    this.amount = 0;
    this.type = 'Debit';
  }

  get filteredTransactions() {
    return this.transactions.filter(tx => {
      const txDate = new Date(tx.date);
      const from = this.fromDate ? new Date(this.fromDate) : null;
      const to = this.toDate ? new Date(this.toDate) : null;

      const matchMerchant = tx.merchant.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchFrom = from ? txDate >= from : true;
      const matchTo = to ? txDate <= to : true;

      return matchMerchant && matchFrom && matchTo;
    });
  }

  get miniStatement() {
    return this.transactions.slice(0, 5);
  }

  downloadStatement() {
    let csv = 'Transaction ID,Merchant,Amount,Type,Balance,Date\n';
    this.filteredTransactions.forEach(tx => {
      csv += `${tx.id},${tx.merchant},${tx.amount},${tx.type},${tx.balance},${tx.date.toLocaleString()}\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'statement.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
