import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { environment } from '../../../../environment/environment';

interface BankingProduct {
  id: number;
  productCode: string;
  productName: string;
  accountType: 'SAVINGS' | 'CURRENT';
  interestRate: number;
  minimumBalance: number;
  dailyTransactionLimit: number;
  monthlyTransactionLimit: number;
  serviceCharge: number;
  eligibility: string;
  enabledFeatures: string[];
  effectiveFrom: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
  requestedBy: string;
  approvedBy?: string;
  rejectionReason?: string;
  createdAt: string;
}

@Component({
  selector: 'app-banking-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './banking-products.html',
  styleUrl: './banking-products.css'
})
export class BankingProducts implements OnInit {
  readonly savingsFeatures = [
    { id: 'accounts', label: 'Account overview' },
    { id: 'ai-assistant', label: 'AI assistant' },
    { id: 'atm-simulator', label: 'ATM withdrawal' },
    { id: 'bank-messages', label: 'Bank messages' },
    { id: 'beneficiary-management', label: 'Beneficiary management' },
    { id: 'bill-payment', label: 'Bill payment' },
    { id: 'card', label: 'Debit card' },
    { id: 'cheque', label: 'Cheque management' },
    { id: 'credit-card', label: 'Credit card' },
    { id: 'credit-card-bill', label: 'Credit card bill pay' },
    { id: 'demand-draft', label: 'Demand draft' },
    { id: 'deposit-request', label: 'Cash deposit slip' },
    { id: 'ecs-mandate', label: 'ECS mandates' },
    { id: 'emi-monitoring', label: 'EMI monitoring' },
    { id: 'family-banking', label: 'Family banking' },
    { id: 'fasttag', label: 'FASTag' },
    { id: 'fixed-deposits', label: 'Fixed deposits' },
    { id: 'goldloan', label: 'Gold loans' },
    { id: 'homeloan', label: 'Home loans' },
    { id: 'investments', label: 'Investments' },
    { id: 'kycupdate', label: 'Profile / KYC update' },
    { id: 'loan', label: 'Personal loans' },
    { id: 'ml-loan-prediction', label: 'Loan eligibility' },
    { id: 'positive-pay', label: 'Positive Pay' },
    { id: 'scheduled-payments', label: 'Scheduled payments' },
    { id: 'subsidy-claim', label: 'Subsidy claims' },
    { id: 'support-tickets', label: 'Support tickets' },
    { id: 'transaction', label: 'Transactions' },
    { id: 'transferfunds', label: 'Fund transfers' },
    { id: 'upi-pay', label: 'UPI payments' },
    { id: 'virtual-card', label: 'Virtual card' }
  ];
  products: BankingProduct[] = [];
  activeProducts: BankingProduct[] = [];
  loading = false;
  saving = false;
  error = '';
  success = '';
  rejectionReasons: Record<number, string> = {};
  adminName = 'Admin';
  form = this.createEmptyForm();

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    if (typeof sessionStorage !== 'undefined') {
      try {
        const admin = JSON.parse(sessionStorage.getItem('admin') || '{}');
        this.adminName = admin.username || admin.name || admin.email || this.adminName;
      } catch {
        this.adminName = 'Admin';
      }
    }
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      versions: this.http.get<BankingProduct[]>(`${environment.apiBaseUrl}/api/admin/banking-products`),
      active: this.http.get<BankingProduct[]>(`${environment.apiBaseUrl}/api/admin/banking-products/active`)
    }).subscribe({
      next: result => {
        this.products = result.versions || [];
        this.activeProducts = result.active || [];
        this.loading = false;
      },
      error: error => {
        this.error = error?.error?.message || 'Unable to load banking product configuration.';
        this.loading = false;
      }
    });
  }

  proposeChange(): void {
    this.error = '';
    this.success = '';
    this.saving = true;
    this.http.post<BankingProduct>(`${environment.apiBaseUrl}/api/admin/banking-products`, {
      ...this.form,
      requestedBy: this.adminName
    }).subscribe({
      next: () => {
        this.success = 'Product version submitted for approval. It will not take effect until approved and its effective date.';
        this.form = this.createEmptyForm();
        this.saving = false;
        this.loadProducts();
      },
      error: error => {
        this.error = error?.error?.message || 'Unable to submit product changes.';
        this.saving = false;
      }
    });
  }

  approve(product: BankingProduct): void {
    if (!window.confirm(`Approve ${product.productName} (${product.productCode}) effective ${product.effectiveFrom}?`)) return;
    this.error = '';
    this.success = '';
    this.http.put<BankingProduct>(
      `${environment.apiBaseUrl}/api/admin/banking-products/${product.id}/approve`,
      {},
      { params: { approvedBy: this.adminName } }
    ).subscribe({
      next: () => {
        this.success = `${product.productName} version approved. It becomes effective on ${product.effectiveFrom}.`;
        this.loadProducts();
      },
      error: error => this.error = error?.error?.message || 'Unable to approve this product version.'
    });
  }

  reject(product: BankingProduct): void {
    const reason = (this.rejectionReasons[product.id] || '').trim();
    if (!reason) {
      this.error = 'Enter a reason before rejecting a product change.';
      return;
    }
    this.error = '';
    this.success = '';
    this.http.put<BankingProduct>(
      `${environment.apiBaseUrl}/api/admin/banking-products/${product.id}/reject`,
      { rejectedBy: this.adminName, reason }
    ).subscribe({
      next: () => {
        this.success = `${product.productName} version rejected.`;
        this.rejectionReasons[product.id] = '';
        this.loadProducts();
      },
      error: error => this.error = error?.error?.message || 'Unable to reject this product version.'
    });
  }

  toggleFeature(featureId: string, enabled: boolean): void {
    this.form.enabledFeatures = enabled
      ? [...new Set([...this.form.enabledFeatures, featureId])]
      : this.form.enabledFeatures.filter(feature => feature !== featureId);
  }

  private createEmptyForm() {
    return {
      productCode: '',
      productName: '',
      accountType: 'SAVINGS' as 'SAVINGS' | 'CURRENT',
      interestRate: 0,
      minimumBalance: 0,
      dailyTransactionLimit: 0,
      monthlyTransactionLimit: 0,
      serviceCharge: 0,
      eligibility: '',
      enabledFeatures: this.savingsFeatures.map(feature => feature.id),
      effectiveFrom: new Date().toISOString().slice(0, 10)
    };
  }
}
