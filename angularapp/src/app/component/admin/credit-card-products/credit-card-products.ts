import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface CreditCardProduct {
  productCode: string;
  name: string;
  description: string;
  theme: string;
  features: string[];
  eligibility: string;
  termsAndConditions: string;
  imageData: string | null;
  annualFee: number;
  sortOrder: number;
  active: boolean;
}

@Component({
  selector: 'app-credit-card-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './credit-card-products.html',
  styleUrl: './credit-card-products.css'
})
export class CreditCardProducts implements OnInit {
  products: CreditCardProduct[] = [];
  loading = false;
  saving = false;
  error = '';
  success = '';
  private readonly api = `${environment.apiBaseUrl}/api/credit-card-products`;

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.error = '';
    this.http.get<CreditCardProduct[]>(`${this.api}/admin`, { headers: this.adminHeaders() }).subscribe({
      next: products => {
        this.products = (products || []).map(product => ({ ...product, features: [...(product.features || [])] }));
        this.loading = false;
      },
      error: error => {
        this.loading = false;
        this.error = error.error?.message || 'Unable to load credit card products.';
      }
    });
  }

  addProduct(): void {
    const number = this.products.length + 1;
    const code = `CARD_${Date.now().toString(36).toUpperCase()}`;
    this.products.push({
      productCode: code,
      name: `NeoBank Credit Card ${number}`,
      description: 'Benefits for your everyday spending',
      theme: 'ocean',
      features: ['Add a card benefit'],
      eligibility: 'Applicants must meet NeoBank identity, income and credit assessment requirements. Approval is subject to verification.',
      termsAndConditions: 'Credit facilities are subject to approval. Fees, interest, repayment obligations and card usage are governed by the final NeoBank card agreement.',
      imageData: null,
      annualFee: 0,
      sortOrder: this.products.length,
      active: true
    });
  }

  removeProduct(index: number): void {
    this.products.splice(index, 1);
    this.products.forEach((product, order) => product.sortOrder = order);
  }

  onFeaturesChanged(product: CreditCardProduct, value: string): void {
    product.features = value.split('\n').map(feature => feature.trim()).filter(Boolean);
  }

  featuresText(product: CreditCardProduct): string {
    return product.features.join('\n');
  }

  selectImage(product: CreditCardProduct, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 1_000_000) {
      this.error = 'Choose a PNG, JPEG or WebP image smaller than 1 MB.';
      input.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      product.imageData = typeof reader.result === 'string' ? reader.result : null;
      this.error = '';
    };
    reader.onerror = () => this.error = 'Unable to read the selected card image.';
    reader.readAsDataURL(file);
  }

  clearImage(product: CreditCardProduct): void {
    product.imageData = null;
  }

  saveAll(): void {
    this.error = '';
    this.success = '';
    if (!this.products.length) {
      this.error = 'Keep at least one product in the catalogue.';
      return;
    }
    this.saving = true;
    this.products.forEach((product, index) => product.sortOrder = index);
    this.http.put<CreditCardProduct[]>(`${this.api}/admin`, this.products, {
      headers: this.adminHeaders()
    }).subscribe({
      next: saved => {
        this.products = saved || [];
        this.saving = false;
        this.success = 'Credit card catalogue saved. The landing page now shows the updated products.';
      },
      error: error => {
        this.saving = false;
        this.error = error.error?.message || 'Unable to save the credit card catalogue.';
      }
    });
  }

  private adminHeaders(): HttpHeaders {
    const token = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('adminSessionToken') || '' : '';
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }
}
