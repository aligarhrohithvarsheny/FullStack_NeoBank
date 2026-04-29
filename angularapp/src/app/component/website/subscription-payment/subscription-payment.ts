import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SubscriptionPaymentService } from '../../../service/subscription-payment.service';

@Component({
  selector: 'app-subscription-payment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subscription-payment.html',
  styleUrls: ['./subscription-payment.css']
})
export class SubscriptionPaymentComponent implements OnInit {
  subscriptions: any[] = [];
  filteredSubscriptions: any[] = [];
  loading = false;
  saving = false;
  showForm = false;
  editMode = false;
  editId: number | null = null;
  employeeId = '';
  salaryAccountNumber = '';
  activeFilter = 'ALL';
  confirmDeleteId: number | null = null;

  stats = { total: 0, active: 0, paused: 0, totalMonthly: 0 };

  form = {
    subscriptionName: '',
    subscriptionCategory: 'ENTERTAINMENT',
    amount: null as number | null,
    frequency: 'MONTHLY',
    autoDebit: true
  };

  categories = ['ENTERTAINMENT', 'HEALTH', 'EDUCATION', 'UTILITY', 'OTHER'];
  frequencies = ['WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'];
  filters = ['ALL', 'ACTIVE', 'PAUSED', 'CANCELLED'];

  constructor(private subService: SubscriptionPaymentService) {}

  ngOnInit(): void {
    const user = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
    this.employeeId = user.employeeId || user.id || '';
    this.salaryAccountNumber = user.salaryAccountNumber || user.accountNumber || '';
    if (this.employeeId) {
      this.loadSubscriptions();
    }
  }

  loadSubscriptions(): void {
    this.loading = true;
    this.subService.getByEmployeeId(this.employeeId).subscribe({
      next: (res: any) => {
        this.subscriptions = res.data || [];
        this.calculateStats();
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  calculateStats(): void {
    this.stats.total = this.subscriptions.length;
    this.stats.active = this.subscriptions.filter(s => s.status === 'ACTIVE').length;
    this.stats.paused = this.subscriptions.filter(s => s.status === 'PAUSED').length;
    this.stats.totalMonthly = this.subscriptions
      .filter(s => s.status === 'ACTIVE')
      .reduce((sum: number, s: any) => sum + (s.amount || 0), 0);
  }

  applyFilter(): void {
    this.filteredSubscriptions = this.activeFilter === 'ALL'
      ? [...this.subscriptions]
      : this.subscriptions.filter(s => s.status === this.activeFilter);
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  openAddForm(): void {
    this.resetForm();
    this.showForm = true;
    this.editMode = false;
  }

  saveSubscription(): void {
    this.saving = true;
    const payload = {
      ...this.form,
      employeeId: this.employeeId,
      salaryAccountNumber: this.salaryAccountNumber
    };

    const obs = this.editMode && this.editId
      ? this.subService.update(this.editId, payload)
      : this.subService.create(payload);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.resetForm();
        this.loadSubscriptions();
      },
      error: () => { this.saving = false; }
    });
  }

  pauseSubscription(id: number): void {
    this.subService.pause(id).subscribe({ next: () => this.loadSubscriptions() });
  }

  resumeSubscription(id: number): void {
    this.subService.resume(id).subscribe({ next: () => this.loadSubscriptions() });
  }

  cancelSubscription(id: number): void {
    if (confirm('Are you sure you want to cancel this subscription?')) {
      this.subService.cancel(id).subscribe({
        next: () => { this.confirmDeleteId = null; this.loadSubscriptions(); }
      });
    }
  }

  resetForm(): void {
    this.form = { subscriptionName: '', subscriptionCategory: 'ENTERTAINMENT', amount: null, frequency: 'MONTHLY', autoDebit: true };
    this.editId = null;
  }

  isFormValid(): boolean {
    return !!this.form.subscriptionName && !!this.form.amount && this.form.amount > 0;
  }

  getCategoryIcon(cat: string): string {
    const icons: Record<string, string> = { 'ENTERTAINMENT': '🎬', 'HEALTH': '🏥', 'EDUCATION': '📚', 'UTILITY': '⚡', 'OTHER': '📦' };
    return icons[cat] || '📦';
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = { 'ACTIVE': 'status-active', 'PAUSED': 'status-paused', 'CANCELLED': 'status-cancelled' };
    return classes[status] || '';
  }

  formatCategory(cat: string): string {
    return (cat || '').replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  formatCurrency(amount: number): string {
    return '₹' + (amount || 0).toLocaleString('en-IN');
  }

  formatDate(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
