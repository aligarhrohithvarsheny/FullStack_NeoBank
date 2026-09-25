import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';

interface Denomination {
  note: number;
  quantity: number;
  amount: number;
}

@Component({
  selector: 'app-deposit-slip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './deposit-slip.component.html',
  styleUrls: ['./deposit-slip.component.css']
})
export class DepositSlipComponent implements OnChanges {
  @Input() user: any = {};
  @Input() accountType = 'Savings Account';
  @Input() amount = 0;
  @Input() paymentMode = 'Cash';
  @Input() referenceNumber = '';
  @Input() depositDate: Date | string = new Date();

  denominations: Denomination[] = [
    { note: 2000, quantity: 0, amount: 0 },
    { note: 1000, quantity: 0, amount: 0 },
    { note: 500, quantity: 0, amount: 0 },
    { note: 200, quantity: 0, amount: 0 },
    { note: 100, quantity: 0, amount: 0 },
    { note: 50, quantity: 0, amount: 0 },
    { note: 20, quantity: 0, amount: 0 },
    { note: 10, quantity: 0, amount: 0 },
    { note: 5, quantity: 0, amount: 0 }
  ];

  totalCash = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['amount']) {
      this.calculateDenominations();
    }
  }

  calculateDenominations(): void {
    let remaining = Math.max(0, Math.floor(Number(this.amount) || 0));
    this.denominations = this.denominations.map(item => {
      const quantity = Math.floor(remaining / item.note);
      remaining %= item.note;
      return { ...item, quantity, amount: item.note * quantity };
    });
    this.totalCash = this.denominations.reduce((total, item) => total + item.amount, 0);
  }

  numberToWords(amount: number): string {
    if (amount === 0) return 'ZERO RUPEES ONLY';
    const ones = ['', 'ONE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN', 'ELEVEN', 'TWELVE', 'THIRTEEN', 'FOURTEEN', 'FIFTEEN', 'SIXTEEN', 'SEVENTEEN', 'EIGHTEEN', 'NINETEEN'];
    const tens = ['', '', 'TWENTY', 'THIRTY', 'FORTY', 'FIFTY', 'SIXTY', 'SEVENTY', 'EIGHTY', 'NINETY'];
    const convertBelow1000 = (num: number): string => {
      let result = '';
      if (num >= 100) {
        result += ones[Math.floor(num / 100)] + ' HUNDRED ';
        num %= 100;
      }
      if (num >= 20) {
        result += tens[Math.floor(num / 10)] + ' ';
        num %= 10;
      }
      if (num > 0) result += ones[num] + ' ';
      return result.trim();
    };
    let remaining = Math.floor(amount);
    let result = '';
    if (remaining >= 10000000) { result += convertBelow1000(Math.floor(remaining / 10000000)) + ' CRORE '; remaining %= 10000000; }
    if (remaining >= 100000) { result += convertBelow1000(Math.floor(remaining / 100000)) + ' LAKH '; remaining %= 100000; }
    if (remaining >= 1000) { result += convertBelow1000(Math.floor(remaining / 1000)) + ' THOUSAND '; remaining %= 1000; }
    if (remaining > 0) result += convertBelow1000(remaining);
    return result.trim() + ' RUPEES ONLY';
  }

  printSlip(): void {
    window.print();
  }

  downloadSlip(): void {
    const content = document.querySelector('.deposit-slip')?.outerHTML || '';
    const styles = Array.from(document.querySelectorAll('style')).map(style => style.innerHTML).join('\n');
    const html = `<!doctype html><html><head><meta charset="utf-8"><title>NeoBank Deposit Slip</title><style>${styles}</style></head><body>${content}</body></html>`;
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([html], { type: 'text/html;charset=utf-8' }));
    link.download = `NeoBank_Deposit_Slip_${this.user.accountNumber || 'account'}.html`;
    link.click();
    URL.revokeObjectURL(link.href);
  }
}
