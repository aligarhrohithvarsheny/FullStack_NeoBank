import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

interface RailControl {
  rail: string;
  paused: boolean;
  reason: string;
  changedBy: string;
  updatedAt: string;
}

interface AccountOption {
  accountNumber: string;
  name: string;
  accountType: string;
  status: string;
  customerId: string;
  savingsProductCode: string;
}

interface SavingsProductOption {
  productCode: string;
  productName: string;
  accountType: string;
  enabledFeatures: string[];
}

interface SavingsFeatureControl {
  id: string;
  enabled: boolean;
  inherited: boolean;
}

interface CardOption {
  id: string | number;
  maskedCardNumber: string;
  cardType: string;
  status: string;
  blocked: boolean;
  deactivated: boolean;
  emergencyFrozen: boolean;
}

interface EmergencyControl {
  scopeType: string;
  scopeKey: string;
  active: boolean;
  reason: string;
  changedBy: string;
  updatedAt: string;
}

interface ControlAudit {
  id: number;
  scopeType: string;
  scopeKey: string;
  action: string;
  previousState: boolean;
  newState: boolean;
  reason: string;
  changedBy: string;
  changedAt: string;
}

interface ControlSnapshot {
  rails: RailControl[];
  isolatedAccounts: EmergencyControl[];
  recentAudit: ControlAudit[];
}

@Component({
  selector: 'app-kill-switches',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './kill-switches.html',
  styleUrls: ['./kill-switches.css']
})
export class KillSwitchesComponent implements OnInit {
  snapshot: ControlSnapshot | null = null;
  searchTerm = '';
  reason = '';
  accounts: AccountOption[] = [];
  selectedAccount: AccountOption | null = null;
  cards: CardOption[] = [];
  accountFeatures: SavingsFeatureControl[] = [];
  savingsProducts: SavingsProductOption[] = [];
  selectedProductCode = '';
  isLoadingFeatures = false;
  isLoadingProducts = false;
  isLoading = true;
  isSearching = false;
  isSaving = false;

  constructor(
    private http: HttpClient,
    private alertService: AlertService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadControls();
    this.loadSavingsProducts();
  }

  loadControls(): void {
    this.http.get<ControlSnapshot>(this.controlUrl, { headers: this.adminHeaders() }).subscribe({
      next: snapshot => {
        this.snapshot = snapshot;
        this.isLoading = false;
      },
      error: error => {
        this.isLoading = false;
        this.showError(error, 'Unable to load emergency controls.');
      }
    });
  }

  searchAccounts(): void {
    const term = this.searchTerm.trim();
    if (term.length < 2) {
      this.alertService.warning('Search required', 'Enter at least two characters to search accounts.');
      return;
    }
    this.isSearching = true;
    this.http.get<{ accounts: AccountOption[] }>(
      `${this.controlUrl}/accounts?search=${encodeURIComponent(term)}`,
      { headers: this.adminHeaders() }
    ).subscribe({
      next: response => {
        this.accounts = response.accounts;
        this.selectedAccount = null;
        this.cards = [];
        this.isSearching = false;
      },
      error: error => {
        this.isSearching = false;
        this.showError(error, 'Account search failed.');
      }
    });
  }

  selectAccount(account: AccountOption): void {
    this.selectedAccount = account;
    this.selectedProductCode = account.savingsProductCode || '';
    this.cards = [];
    this.accountFeatures = [];
    if (account.accountType.toUpperCase() === 'SAVINGS') this.loadAccountFeatures(account);
    this.http.get<{ cards: CardOption[] }>(
      `${this.controlUrl}/cards?accountNumber=${encodeURIComponent(account.accountNumber)}`,
      { headers: this.adminHeaders() }
    ).subscribe({
      next: response => this.cards = response.cards,
      error: error => this.showError(error, 'Unable to load the account cards.')
    });
  }

  loadSavingsProducts(): void {
    this.isLoadingProducts = true;
    this.http.get<SavingsProductOption[]>(
      `${environment.apiBaseUrl}/api/admin/banking-products/active`,
      { headers: this.adminHeaders() }
    ).subscribe({
      next: products => {
        this.savingsProducts = (products || []).filter(
          product => product.accountType?.toUpperCase() === 'SAVINGS');
        this.isLoadingProducts = false;
      },
      error: error => {
        this.isLoadingProducts = false;
        this.showError(error, 'Unable to load active savings products.');
      }
    });
  }

  updateSavingsProduct(): void {
    const account = this.selectedAccount;
    const product = this.savingsProducts.find(option => option.productCode === this.selectedProductCode);
    const reason = this.reason.trim();
    if (!account || !product) {
      this.alertService.warning('Savings product required', 'Select an active savings product to continue.');
      return;
    }
    if (!reason) {
      this.alertService.warning('Reason required', 'Enter a reason before changing the savings product.');
      return;
    }
    if (account.savingsProductCode === product.productCode) return;
    if (this.isSaving || !window.confirm(
      `Change account ${account.accountNumber} to ${product.productName}? Product-default features will update immediately; individual overrides will be retained.`)) return;

    this.isSaving = true;
    this.http.put<any>(
      `${this.controlUrl}/accounts/${encodeURIComponent(account.accountNumber)}/product`,
      { productCode: product.productCode, reason },
      { headers: this.adminHeaders() }
    ).subscribe({
      next: response => {
        account.savingsProductCode = response.productCode || product.productCode;
        this.accountFeatures = response.features || [];
        this.reason = '';
        this.isSaving = false;
        this.alertService.success(
          'Savings product updated',
          `${product.productName} is now assigned. The customer dashboard refreshes access within seconds.`);
      },
      error: error => {
        this.isSaving = false;
        this.showError(error, 'Unable to change this savings product.');
      }
    });
  }

  loadAccountFeatures(account: AccountOption): void {
    this.isLoadingFeatures = true;
    this.http.get<any>(`${this.controlUrl}/accounts/${encodeURIComponent(account.accountNumber)}/features`,
      { headers: this.adminHeaders() }).subscribe({
      next: response => {
        this.accountFeatures = response.features || [];
        this.isLoadingFeatures = false;
      },
      error: error => {
        this.isLoadingFeatures = false;
        this.showError(error, 'Unable to load savings feature access.');
      }
    });
  }

  toggleFeature(feature: SavingsFeatureControl): void {
    if (!this.selectedAccount) return;
    const action = feature.enabled ? 'Disable' : 'Enable';
    const reason = this.reason.trim();
    if (!reason) {
      this.alertService.warning('Reason required', 'Enter a reason before changing customer feature access.');
      return;
    }
    if (this.isSaving || !window.confirm(`${action} ${this.featureLabel(feature.id)} for account ${this.selectedAccount.accountNumber}?`)) return;

    this.isSaving = true;
    this.http.put<any>(
      `${this.controlUrl}/accounts/${encodeURIComponent(this.selectedAccount.accountNumber)}/features/${encodeURIComponent(feature.id)}`,
      { active: feature.enabled, reason },
      { headers: this.adminHeaders() }
    ).subscribe({
      next: response => {
        this.accountFeatures = response.features || [];
        this.reason = '';
        this.isSaving = false;
        this.alertService.success('Feature access updated', `${this.featureLabel(feature.id)} access updated immediately.`);
      },
      error: error => {
        this.isSaving = false;
        this.showError(error, `Unable to ${action.toLowerCase()} this feature.`);
      }
    });
  }

  featureLabel(id: string): string {
    return id.replace(/-/g, ' ').replace(/\b\w/g, letter => letter.toUpperCase());
  }

  isAccountIsolated(accountNumber: string): boolean {
    return this.snapshot?.isolatedAccounts.some(
      control => control.scopeKey.toUpperCase() === accountNumber.toUpperCase()
    ) ?? false;
  }

  get hasPausedRail(): boolean {
    return this.snapshot?.rails.some(rail => rail.paused) ?? false;
  }

  toggleRail(control: RailControl): void {
    this.updateControl('rails', control.rail, !control.paused,
      control.paused ? 'Resume outbound ' + control.rail : 'Pause outbound ' + control.rail);
  }

  toggleAccount(): void {
    if (!this.selectedAccount) return;
    const isolated = this.isAccountIsolated(this.selectedAccount.accountNumber);
    this.updateControl('accounts', this.selectedAccount.accountNumber, !isolated,
      isolated ? 'Release account isolation' : 'Isolate account');
  }

  toggleCard(card: CardOption): void {
    this.updateControl('cards', card.id, !card.emergencyFrozen,
      card.emergencyFrozen ? 'Release emergency card freeze' : 'Freeze card');
  }

  goBack(): void {
    void this.router.navigateByUrl('/admin/dashboard');
  }

  private updateControl(
    scope: 'rails' | 'accounts' | 'cards',
    key: string | number,
    active: boolean,
    action: string
  ): void {
    const reason = this.reason.trim();
    if (!reason) {
      this.alertService.warning('Reason required', 'Enter an incident reason before changing a control.');
      return;
    }
    if (this.isSaving || !window.confirm(`${action}? This change takes effect immediately.`)) return;

    this.isSaving = true;
    const encodedKey = encodeURIComponent(String(key));
    this.http.put(
      `${this.controlUrl}/${scope}/${encodedKey}`,
      { active, reason },
      { headers: this.adminHeaders() }
    ).subscribe({
      next: () => {
        this.isSaving = false;
        this.reason = '';
        this.alertService.success('Emergency control updated', `${action} completed.`);
        this.loadControls();
        if (scope === 'cards' && this.selectedAccount) this.selectAccount(this.selectedAccount);
      },
      error: error => {
        this.isSaving = false;
        this.showError(error, `Unable to ${action.toLowerCase()}.`);
      }
    });
  }

  private get controlUrl(): string {
    return `${environment.apiBaseUrl}/api/admin/kill-switches`;
  }

  private adminHeaders(): HttpHeaders {
    const token = sessionStorage.getItem('adminSessionToken') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  private showError(error: any, fallback: string): void {
    const status = Number(error?.status);
    const message = error?.error?.message || (
      status === 0
        ? 'Cannot reach the banking API. Check that the backend is available, then try again.'
        : status === 401
          ? 'Your admin session has expired. Sign out and sign in again.'
          : status === 403
            ? 'Your current session is not authorized for admin controls. Sign in with an admin account.'
            : status === 404
              ? 'The backend does not have this admin endpoint yet. Deploy or restart the latest Spring backend, then try again.'
              : status >= 500
                ? `The banking API failed while processing this request (HTTP ${status}). Check backend logs and try again.`
                : fallback
    );
    this.alertService.error('Emergency controls unavailable', message);
  }
}
