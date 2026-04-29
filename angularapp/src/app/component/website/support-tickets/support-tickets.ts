import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupportTicketService } from '../../../service/support-ticket.service';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-support-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support-tickets.html',
  styleUrls: ['./support-tickets.css']
})
export class SupportTickets implements OnInit, OnDestroy {
  accountNumber: string = '';
  userName: string = '';
  userEmail: string = '';
  tickets: any[] = [];
  isLoading: boolean = false;
  showForm: boolean = false;
  activeTab: string = 'all';
  selectedTicket: any = null;
  pollingInterval: any = null;

  ticketForm = {
    category: 'TRANSACTION_FAILED',
    subject: '',
    description: '',
    priority: 'MEDIUM',
    transactionId: ''
  };

  categories = [
    { value: 'TRANSACTION_FAILED', label: 'Transaction Failed' },
    { value: 'DEBIT_CARD_ISSUE', label: 'Debit Card Issue' },
    { value: 'LOGIN_PROBLEM', label: 'Account Login Problem' },
    { value: 'LOAN_EMI', label: 'Loan EMI Payment' },
    { value: 'ACCOUNT_ISSUE', label: 'Account Issue' },
    { value: 'OTHER', label: 'Other' }
  ];

  priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  isSubmitting: boolean = false;

  constructor(
    private supportTicketService: SupportTicketService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadUserInfo();
    this.loadTickets();
    this.startPolling();
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  startPolling() {
    this.pollingInterval = setInterval(() => {
      this.refreshTickets();
    }, 10000);
  }

  stopPolling() {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
  }

  refreshTickets() {
    if (!this.accountNumber) return;
    this.supportTicketService.getByAccountNumber(this.accountNumber).subscribe({
      next: (res: any) => {
        this.tickets = res.tickets || [];
      },
      error: () => {}
    });
  }

  loadUserInfo() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        this.accountNumber = user.accountNumber || '';
        this.userName = user.name || user.username || '';
        this.userEmail = user.email || '';
      } catch (e) {}
    }
  }

  loadTickets() {
    if (!this.accountNumber) return;
    this.isLoading = true;
    this.supportTicketService.getByAccountNumber(this.accountNumber).subscribe({
      next: (res: any) => {
        this.tickets = res.tickets || [];
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  getFilteredTickets(): any[] {
    if (this.activeTab === 'all') return this.tickets;
    return this.tickets.filter((t: any) => t.status === this.activeTab.toUpperCase());
  }

  createTicket() {
    if (!this.ticketForm.subject || !this.ticketForm.description) {
      this.alertService.error('Validation Error', 'Please fill subject and description');
      return;
    }
    this.isSubmitting = true;
    const payload = {
      ...this.ticketForm,
      accountNumber: this.accountNumber,
      userName: this.userName,
      userEmail: this.userEmail
    };
    this.supportTicketService.create(payload).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.success('Success', 'Support ticket created! Ticket ID: ' + (res.ticket?.ticketId || ''));
          this.showForm = false;
          this.resetForm();
          this.loadTickets();
        }
        this.isSubmitting = false;
      },
      error: () => {
        this.alertService.error('Error', 'Error creating ticket');
        this.isSubmitting = false;
      }
    });
  }

  viewTicket(ticket: any) {
    this.selectedTicket = this.selectedTicket?.id === ticket.id ? null : ticket;
  }

  resetForm() {
    this.ticketForm = { category: 'TRANSACTION_FAILED', subject: '', description: '', priority: 'MEDIUM', transactionId: '' };
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'OPEN': return '#ffc107';
      case 'IN_PROGRESS': return '#17a2b8';
      case 'RESOLVED': return '#28a745';
      case 'CLOSED': return '#6c757d';
      case 'REOPENED': return '#fd7e14';
      default: return '#6c757d';
    }
  }

  getCategoryIcon(category: string): string {
    switch (category) {
      case 'TRANSACTION_FAILED': return '❌';
      case 'DEBIT_CARD_ISSUE': return '💳';
      case 'LOGIN_PROBLEM': return '🔐';
      case 'LOAN_EMI': return '💰';
      case 'ACCOUNT_ISSUE': return '📋';
      default: return '📌';
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
