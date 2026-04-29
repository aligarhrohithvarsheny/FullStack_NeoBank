import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScheduledPaymentService } from '../../../service/scheduled-payment.service';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-scheduled-payments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './scheduled-payments.html',
  styleUrls: ['./scheduled-payments.css']
})
export class ScheduledPayments implements OnInit {
  accountNumber: string = '';
  payments: any[] = [];
  isLoading: boolean = false;
  showForm: boolean = false;
  activeTab: string = 'all';

  paymentForm = {
    recipientAccountNumber: '',
    recipientName: '',
    recipientIfsc: '',
    amount: 0,
    paymentType: 'RENT',
    description: '',
    frequency: 'MONTHLY',
    startDate: '',
    endDate: '',
    totalPayments: null as number | null
  };

  paymentTypes = ['RENT', 'LOAN_EMI', 'SUBSCRIPTION', 'UTILITY', 'OTHER'];
  frequencies = ['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'];
  isSubmitting: boolean = false;

  constructor(
    private scheduledPaymentService: ScheduledPaymentService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadUserInfo();
    this.loadPayments();
  }

  loadUserInfo() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        this.accountNumber = user.accountNumber || '';
      } catch (e) {}
    }
  }

  loadPayments() {
    if (!this.accountNumber) return;
    this.isLoading = true;
    this.scheduledPaymentService.getByAccountNumber(this.accountNumber).subscribe({
      next: (res: any) => {
        this.payments = res.payments || [];
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  getFilteredPayments(): any[] {
    if (this.activeTab === 'all') return this.payments;
    return this.payments.filter((p: any) => p.status === this.activeTab.toUpperCase());
  }

  createPayment() {
    if (!this.paymentForm.recipientAccountNumber || !this.paymentForm.amount || !this.paymentForm.startDate) {
      this.alertService.error('Validation Error', 'Please fill all required fields');
      return;
    }
    this.isSubmitting = true;
    const payload = { ...this.paymentForm, accountNumber: this.accountNumber };
    this.scheduledPaymentService.create(payload).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.success('Success', 'Scheduled payment created successfully!');
          this.showForm = false;
          this.resetForm();
          this.loadPayments();
        } else {
          this.alertService.error('Error', res.message || 'Failed to create');
        }
        this.isSubmitting = false;
      },
      error: () => {
        this.alertService.error('Error', 'Error creating scheduled payment');
        this.isSubmitting = false;
      }
    });
  }

  pausePayment(id: number) {
    this.scheduledPaymentService.pause(id).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.success('Success', 'Payment paused');
          this.loadPayments();
        }
      }
    });
  }

  resumePayment(id: number) {
    this.scheduledPaymentService.resume(id).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.success('Success', 'Payment resumed');
          this.loadPayments();
        }
      }
    });
  }

  cancelPayment(id: number) {
    if (confirm('Are you sure you want to cancel this scheduled payment?')) {
      this.scheduledPaymentService.cancel(id).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.alertService.success('Success', 'Payment cancelled');
            this.loadPayments();
          }
        }
      });
    }
  }

  resetForm() {
    this.paymentForm = {
      recipientAccountNumber: '',
      recipientName: '',
      recipientIfsc: '',
      amount: 0,
      paymentType: 'RENT',
      description: '',
      frequency: 'MONTHLY',
      startDate: '',
      endDate: '',
      totalPayments: null
    };
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return '#28a745';
      case 'PAUSED': return '#ffc107';
      case 'COMPLETED': return '#17a2b8';
      case 'CANCELLED': return '#dc3545';
      case 'FAILED': return '#dc3545';
      default: return '#6c757d';
    }
  }

  getPaymentTypeIcon(type: string): string {
    switch (type) {
      case 'RENT': return '🏠';
      case 'LOAN_EMI': return '💰';
      case 'SUBSCRIPTION': return '📺';
      case 'UTILITY': return '💡';
      default: return '💳';
    }
  }
}
