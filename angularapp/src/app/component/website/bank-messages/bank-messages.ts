import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BankMessageService } from '../../../service/bank-message.service';

@Component({
  selector: 'app-bank-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bank-messages.html',
  styleUrls: ['./bank-messages.css']
})
export class BankMessagesComponent implements OnInit {
  messages: any[] = [];
  filteredMessages: any[] = [];
  selectedMessage: any = null;
  loading = false;
  accountNumber = '';
  activeFilter = 'ALL';
  unreadCount = 0;

  filters = ['ALL', 'UNREAD', 'LOAN_APPROVAL', 'SALARY_CREDITED', 'TRANSACTION_ALERT', 'ACCOUNT_UPDATE', 'SYSTEM', 'PROMOTION'];

  constructor(private bankMessageService: BankMessageService) {}

  ngOnInit(): void {
    const user = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
    this.accountNumber = user.accountNumber || '';
    if (this.accountNumber) {
      this.loadMessages();
    }
  }

  loadMessages(): void {
    this.loading = true;
    this.bankMessageService.getByAccountNumber(this.accountNumber).subscribe({
      next: (res: any) => {
        this.messages = res.data || [];
        this.unreadCount = this.messages.filter((m: any) => !m.isRead).length;
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  applyFilter(): void {
    if (this.activeFilter === 'ALL') {
      this.filteredMessages = [...this.messages];
    } else if (this.activeFilter === 'UNREAD') {
      this.filteredMessages = this.messages.filter(m => !m.isRead);
    } else {
      this.filteredMessages = this.messages.filter(m => m.messageType === this.activeFilter);
    }
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  viewMessage(message: any): void {
    this.selectedMessage = message;
    if (!message.isRead) {
      this.bankMessageService.markAsRead(message.id).subscribe({
        next: () => {
          message.isRead = true;
          this.unreadCount = this.messages.filter((m: any) => !m.isRead).length;
        }
      });
    }
  }

  closeMessage(): void {
    this.selectedMessage = null;
  }

  markAllAsRead(): void {
    this.bankMessageService.markAllAsRead(this.accountNumber).subscribe({
      next: () => {
        this.messages.forEach(m => m.isRead = true);
        this.unreadCount = 0;
        this.applyFilter();
      }
    });
  }

  getMessageIcon(type: string): string {
    const icons: Record<string, string> = {
      'LOAN_APPROVAL': '🏦', 'SALARY_CREDITED': '💰', 'TRANSACTION_ALERT': '🔔',
      'ACCOUNT_UPDATE': '📋', 'SYSTEM': '⚙️', 'PROMOTION': '🎁'
    };
    return icons[type] || '📩';
  }

  getPriorityClass(priority: string): string {
    return 'priority-' + (priority || 'low').toLowerCase();
  }

  formatDate(date: string): string {
    if (!date) return '';
    const d = new Date(date);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return Math.floor(diff / 60000) + 'm ago';
    if (diff < 86400000) return Math.floor(diff / 3600000) + 'h ago';
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  formatFilterLabel(filter: string): string {
    return filter.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }
}
