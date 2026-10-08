import { Component, OnInit, OnDestroy, EventEmitter, Output } from '@angular/core';
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
  @Output() connectToAgent = new EventEmitter<void>();

  accountNumber: string = '';
  hasAuthToken = false;
  userName: string = '';
  userEmail: string = '';
  tickets: any[] = [];
  isLoading: boolean = false;
  loadError: string = '';
  searchQuery: string = '';
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
  isVerifyingTransaction: boolean = false;
  verifiedTransaction: any = null;
  transactionVerificationError = '';

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
    if (!this.accountNumber || !this.hasAuthToken) return;
    this.supportTicketService.getMine().subscribe({
      next: (res: any) => {
        if (res.success) {
          this.tickets = res.tickets || [];
          this.loadError = '';
        }
      },
      error: (err: any) => {
        this.loadError = err.error?.message || 'Unable to refresh your support requests.';
      }
    });
  }

  loadUserInfo() {
    if (typeof sessionStorage === 'undefined') return;
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        this.accountNumber = user.accountNumber || '';
        this.hasAuthToken = !!user.authToken;
        this.userName = user.name || user.username || '';
        this.userEmail = user.email || '';
        if (!this.hasAuthToken) {
          this.loadError = 'Your secure session has expired. Please sign in again to use customer support.';
        }
      } catch {
        this.loadError = 'Unable to read your signed-in account. Please sign in again.';
      }
    } else {
      this.loadError = 'Please sign in to view your support requests.';
    }
  }

  loadTickets() {
    if (!this.accountNumber || !this.hasAuthToken) return;
    this.isLoading = true;
    this.loadError = '';
    this.supportTicketService.getMine().subscribe({
      next: (res: any) => {
        this.tickets = res.tickets || [];
        this.loadError = '';
        this.isLoading = false;
      },
      error: (err: any) => {
        this.loadError = err.error?.message || 'Unable to load your support requests. Please try again.';
        this.isLoading = false;
      }
    });
  }

  getFilteredTickets(): any[] {
    const term = this.searchQuery.trim().toLowerCase();
    return this.tickets.filter((ticket: any) => {
      const matchesTab = this.activeTab === 'all' || ticket.status === this.activeTab.toUpperCase();
      const matchesSearch = !term || [
        ticket.ticketId, ticket.subject, ticket.description, ticket.category, ticket.status
      ].some(value => String(value || '').toLowerCase().includes(term));
      return matchesTab && matchesSearch;
    });
  }

  getTicketCount(status: string): number {
    if (status === 'all') return this.tickets.length;
    return this.tickets.filter((ticket: any) => ticket.status === status.toUpperCase()).length;
  }

  createTicket() {
    if (!this.accountNumber || !this.hasAuthToken) {
      this.alertService.error('Sign-in Required', 'Please sign in again before submitting a support request.');
      return;
    }
    if (!this.ticketForm.subject.trim() || !this.ticketForm.description.trim()) {
      this.alertService.error('Validation Error', 'Please fill subject and description');
      return;
    }
    if (this.ticketForm.transactionId.trim() && !this.verifiedTransaction) {
      this.alertService.error('Transaction Verification Required', 'Verify the transaction ID before submitting the ticket');
      return;
    }
    if (this.ticketForm.category === 'TRANSACTION_FAILED' && !this.ticketForm.transactionId.trim()) {
      this.alertService.error('Transaction ID Required', 'Enter the transaction ID related to this issue');
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
        } else {
          this.alertService.error('Error', res.message || 'Unable to create your support request.');
        }
        this.isSubmitting = false;
      },
      error: (err: any) => {
        this.alertService.error('Error', err.error?.message || 'Unable to create your support request.');
        this.isSubmitting = false;
      }
    });
  }

  verifyTransaction() {
    if (!this.accountNumber || !this.hasAuthToken) {
      this.transactionVerificationError = 'Please sign in again before verifying a transaction.';
      return;
    }
    const transactionId = this.ticketForm.transactionId.trim();
    if (!transactionId) {
      this.transactionVerificationError = 'Enter a transaction ID first';
      return;
    }
    this.isVerifyingTransaction = true;
    this.verifiedTransaction = null;
    this.transactionVerificationError = '';
    this.supportTicketService.verifyTransaction(transactionId).subscribe({
      next: (res: any) => {
        this.isVerifyingTransaction = false;
        if (res.success) this.verifiedTransaction = res.transaction;
        else this.transactionVerificationError = res.message || 'Transaction was not found in your account';
      },
      error: (err: any) => {
        this.isVerifyingTransaction = false;
        this.transactionVerificationError = err.error?.message || 'Transaction was not found in your account';
      }
    });
  }

  viewTicket(ticket: any) {
    this.selectedTicket = this.selectedTicket?.id === ticket.id ? null : ticket;
  }

  requestAgent() {
    this.connectToAgent.emit();
  }

  resetForm() {
    this.ticketForm = { category: 'TRANSACTION_FAILED', subject: '', description: '', priority: 'MEDIUM', transactionId: '' };
    this.verifiedTransaction = null;
    this.transactionVerificationError = '';
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
