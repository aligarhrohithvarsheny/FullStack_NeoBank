import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface CityOperation {
  id?: number;
  city: string;
  turnover: number;
  profit: number;
  operations: number;
  status: 'ACTIVE' | 'PLANNING';
  staffIds: string[];
  metrics?: any;
}

@Component({
  selector: 'app-hod-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './hod-dashboard.html',
  styleUrls: ['./hod-dashboard.css']
})
export class HodDashboard implements OnInit {
  staff: any[] = [];
  overview: any = null;
  cities: CityOperation[] = [];
  selectedCity: CityOperation | null = null;
  newCity = { city: '', turnover: 0, profit: 0, operations: 0 };
  errorMessage = '';
  lastRefreshed = '';
  staffSearch = '';
  selectedStaff: any = null;
  branchAccounts: any[] = [];
  managerLoginCredentials: Record<number, { username: string; password: string }> = {};
  generatingManagerLoginId: number | null = null;
  centralAccount: any = null;
  centralTransactions: any[] = [];
  centralTransactionFilters = {
    fromDate: '', toDate: '', search: '', category: '', type: '', status: '', minAmount: '', maxAmount: ''
  };
  centralTransactionsPage = 0;
  centralTransactionsTotalPages = 0;
  centralTransactionsTotalElements = 0;
  isLoadingCentralLedger = false;
  customerSearch = '';
  customerMatches: any[] = [];
  isSearchingCustomers = false;
  isSavingBranchAssignment = false;
  centralLedgerError = '';
  private customerSearchTimer?: ReturnType<typeof setTimeout>;
  cityMetrics: Record<string, any> = {};
  private refreshTimer?: ReturnType<typeof setInterval>;
  private ledgerRefreshTimer?: ReturnType<typeof setInterval>;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.loadCities();
    this.loadCityMetrics();
    this.refreshOverview();
    this.loadBranchAccounts();
    this.loadCentralLedger();
    this.refreshTimer = setInterval(() => this.refreshOverview(), 30000);
    this.ledgerRefreshTimer = setInterval(() => {
      if (typeof document !== 'undefined' && document.visibilityState === 'visible'
          && !this.isLoadingCentralLedger) {
        this.loadCentralLedger();
      }
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
    if (this.ledgerRefreshTimer) clearInterval(this.ledgerRefreshTimer);
    if (this.customerSearchTimer) clearTimeout(this.customerSearchTimer);
  }

  refreshOverview(): void {
    this.loadCityMetrics();
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/hod-overview`).subscribe({
      next: overview => {
        this.overview = overview;
        this.lastRefreshed = overview?.refreshedAt || new Date().toISOString();
        this.errorMessage = '';
      },
      error: () => this.errorMessage = 'Live bank data is temporarily unavailable.'
    });
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admins/staff`).subscribe({
      next: staff => this.staff = staff || [],
      error: () => this.errorMessage = 'Staff data is temporarily unavailable.'
    });
  }

  get totalTurnover(): number { return this.cities.reduce((sum, city) => sum + Number(city.turnover || 0), 0); }
  get totalProfit(): number { return this.cities.reduce((sum, city) => sum + Number(city.profit || 0), 0); }
  get totalOperations(): number { return this.cities.reduce((sum, city) => sum + Number(city.operations || 0), 0); }

  metricsFor(city: CityOperation): any {
    return this.cityMetrics[city.city.toLowerCase()] || {};
  }

  private loadCityMetrics(): void {
    this.http.get<Record<string, any>>(`${environment.apiBaseUrl}/api/admins/hod/cities/metrics`).subscribe({
      next: metrics => {
        this.cityMetrics = metrics || {};
        this.cities = this.cities.map(city => ({ ...city, metrics: this.metricsFor(city) }));
      },
      error: () => this.errorMessage = 'City financial metrics are temporarily unavailable.'
    });
  }

  get filteredStaff(): any[] {
    const query = this.staffSearch.trim().toLowerCase();
    if (!query) return this.staff;
    return this.staff.filter(person => [person.name, person.email, person.role, person.assignedCity, person.employeeId]
      .some(value => String(value || '').toLowerCase().includes(query)));
  }

  addCity(): void {
    const city = this.newCity.city.trim();
    if (!city) return;
    this.http.post<CityOperation>(`${environment.apiBaseUrl}/api/admins/hod/cities`, {
      ...this.newCity, city, status: 'PLANNING'
    }).subscribe({
      next: saved => {
        this.cities = [...this.cities, saved];
        this.newCity = { city: '', turnover: 0, profit: 0, operations: 0 };
      },
      error: err => this.errorMessage = err.error?.message || 'Unable to add city'
    });
  }

  selectCity(city: CityOperation): void { this.selectedCity = city; }

  updateCity(): void {
    if (!this.selectedCity) return;
    if (!this.selectedCity.id) return;
    this.http.put<CityOperation>(`${environment.apiBaseUrl}/api/admins/hod/cities/${this.selectedCity.id}`, this.selectedCity)
      .subscribe({ next: saved => this.selectedCity = saved, error: () => this.errorMessage = 'Unable to update city' });
  }

  toggleStaff(city: CityOperation, person: any): void {
    const nextCity = this.isStaffAssigned(city, person) ? '' : city.city;
    if (nextCity && person.assignedCity && person.assignedCity.toLowerCase() !== city.city.toLowerCase()) {
      this.errorMessage = `${person.name || person.email} is already assigned to ${person.assignedCity}`;
      return;
    }
    this.http.put<any>(`${environment.apiBaseUrl}/api/admins/hod/staff/${person.id}/city`, { city: nextCity })
      .subscribe({
        next: updated => {
          const index = this.staff.findIndex(item => item.id === person.id);
          if (index >= 0) this.staff[index] = updated;
          this.errorMessage = '';
        },
        error: err => this.errorMessage = err.error?.message || 'Unable to update staff location'
      });
  }

  isStaffAssigned(city: CityOperation, person: any): boolean {
    return !!person.assignedCity && person.assignedCity.toLowerCase() === city.city.toLowerCase();
  }

  canAssignStaff(city: CityOperation, person: any): boolean {
    return !person.assignedCity || this.isStaffAssigned(city, person);
  }

  showStaffProfile(person: any): void { this.selectedStaff = person; }

  openAccountLinking(): void { this.router.navigate(['/hod/account-linking']); }

  get branchCities(): string[] {
    return [...new Set(this.branchAccounts.map(account => String(account.branchCity || '').trim()).filter(Boolean))];
  }

  loadCentralLedger(): void {
    this.isLoadingCentralLedger = true;
    this.centralLedgerError = '';
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/hod/central-account`, this.hodAuthOptions()).subscribe({
      next: account => this.centralAccount = account,
      error: err => {
        this.centralAccount = null;
        this.centralLedgerError = err.status === 404
          ? 'The central-account API is not available on the deployed backend. Deploy the backend update, then reload this dashboard.'
          : err.error?.message || 'Central account summary is unavailable.';
        if (err.status === 404) this.stopCentralLedgerPolling();
      }
    });
    const params = new URLSearchParams({ page: String(this.centralTransactionsPage), size: '20' });
    Object.entries(this.centralTransactionFilters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/hod/central-account/transactions?${params}`, this.hodAuthOptions()).subscribe({
      next: result => {
        this.centralTransactions = result.content || [];
        this.centralTransactionsTotalPages = result.totalPages || 0;
        this.centralTransactionsTotalElements = result.totalElements || 0;
        this.isLoadingCentralLedger = false;
      },
      error: err => {
        this.centralTransactions = [];
        this.centralLedgerError = err.status === 404
          ? 'The central-ledger API is not available on the deployed backend. Deploy the backend update, then reload this dashboard.'
          : err.error?.message || 'Central account transactions are unavailable.';
        if (err.status === 404) this.stopCentralLedgerPolling();
        this.isLoadingCentralLedger = false;
      }
    });
  }

  private stopCentralLedgerPolling(): void {
    if (this.ledgerRefreshTimer) clearInterval(this.ledgerRefreshTimer);
    this.ledgerRefreshTimer = undefined;
  }

  applyCentralLedgerFilters(): void {
    this.centralTransactionsPage = 0;
    this.loadCentralLedger();
  }

  changeCentralLedgerPage(direction: number): void {
    const nextPage = this.centralTransactionsPage + direction;
    if (nextPage < 0 || nextPage >= this.centralTransactionsTotalPages) return;
    this.centralTransactionsPage = nextPage;
    this.loadCentralLedger();
  }

  openCentralTransactionReceipt(transactionId: string, download = false): void {
    const previewWindow = download ? null : window.open('', '_blank');
    if (!download && !previewWindow) {
      this.errorMessage = 'Allow pop-ups to view the transaction receipt.';
      return;
    }
    const url = `${environment.apiBaseUrl}/api/admins/hod/central-account/transactions/${encodeURIComponent(transactionId)}/receipt?download=${download}`;
    this.http.get(url, { ...this.hodAuthOptions(), responseType: 'blob' }).subscribe({
      next: blob => {
        const objectUrl = URL.createObjectURL(blob);
        if (download) {
          const link = document.createElement('a');
          link.href = objectUrl;
          link.download = `${transactionId}.pdf`;
          link.click();
          setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
          return;
        }
        previewWindow!.location.href = objectUrl;
        setTimeout(() => URL.revokeObjectURL(objectUrl), 60000);
      },
      error: () => {
        previewWindow?.close();
        this.errorMessage = 'Unable to load the transaction receipt.';
      }
    });
  }

  loadBranchAccounts(): void {
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admins/hod/branch-accounts`, this.hodAuthOptions()).subscribe({
      next: accounts => this.branchAccounts = accounts || [],
      error: err => this.errorMessage = err.error?.message || 'Branch account data is unavailable.'
    });
  }

  generateManagerLogin(account: any): void {
    const managerId = Number(account?.managerAdminId);
    if (!managerId || account?.managerRole !== 'MANAGER' || !account?.branchCity) return;
    if (account.managerLoginEnabled && !confirm('Reset this manager login? The current password will stop working immediately.')) return;

    this.generatingManagerLoginId = managerId;
    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/hod/staff/${managerId}/manager-login`, {},
      this.hodAuthOptions()).subscribe({
      next: credentials => {
        this.managerLoginCredentials[managerId] = {
          username: credentials.username,
          password: credentials.password
        };
        account.managerLoginUsername = credentials.username;
        account.managerLoginEnabled = true;
        this.generatingManagerLoginId = null;
        this.errorMessage = '';
      },
      error: err => {
        this.generatingManagerLoginId = null;
        this.errorMessage = err.error?.message || 'Unable to create manager login credentials.';
      }
    });
  }

  searchBranchCustomers(): void {
    if (this.customerSearchTimer) clearTimeout(this.customerSearchTimer);
    const query = this.customerSearch.trim();
    if (query.length < 2) {
      this.customerMatches = [];
      return;
    }
    this.customerSearchTimer = setTimeout(() => {
      this.isSearchingCustomers = true;
      this.http.get<any[]>(`${environment.apiBaseUrl}/api/admins/hod/branch-accounts/customers?search=${encodeURIComponent(query)}`,
        this.hodAuthOptions()).subscribe({
        next: customers => {
          this.customerMatches = customers || [];
          this.isSearchingCustomers = false;
        },
        error: err => {
          this.errorMessage = err.error?.message || 'Unable to search customer accounts.';
          this.isSearchingCustomers = false;
        }
      });
    }, 250);
  }

  assignCustomerBranch(customer: any, branchCity: string): void {
    if (!customer?.accountNumber || !branchCity) return;
    this.isSavingBranchAssignment = true;
    this.http.put<any>(`${environment.apiBaseUrl}/api/admins/hod/branch-accounts/customer-assignment`,
      { accountNumber: customer.accountNumber, branchCity }, this.hodAuthOptions()).subscribe({
      next: response => {
        this.isSavingBranchAssignment = false;
        if (!response?.success) {
          this.errorMessage = response?.message || 'Unable to assign customer account.';
          return;
        }
        customer.branchCity = branchCity;
        this.errorMessage = '';
      },
      error: err => {
        this.isSavingBranchAssignment = false;
        this.errorMessage = err.error?.message || 'Unable to assign customer account.';
      }
    });
  }

  private hodAuthOptions(): { headers: Record<string, string> } {
    const token = sessionStorage.getItem('adminSessionToken') || '';
    return { headers: token ? { Authorization: `Bearer ${token}` } : {} };
  }

  removeCity(city: CityOperation): void {
    if (!city.id) return;
    this.http.delete(`${environment.apiBaseUrl}/api/admins/hod/cities/${city.id}`).subscribe({
      next: () => {
        this.cities = this.cities.filter(item => item.id !== city.id);
        if (this.selectedCity?.id === city.id) this.selectedCity = null;
        this.refreshOverview();
      },
      error: () => this.errorMessage = 'Unable to remove city'
    });
  }

  logout(): void {
    sessionStorage.removeItem('admin');
    sessionStorage.removeItem('adminSessionToken');
    sessionStorage.removeItem('userRole');
    sessionStorage.removeItem('adminLoginTime');
    this.router.navigate(['/hod/login']);
  }

  private loadCities(): void {
    this.http.get<CityOperation[]>(`${environment.apiBaseUrl}/api/admins/hod/cities`).subscribe({
      next: cities => this.cities = cities || [],
      error: () => this.errorMessage = 'City data is temporarily unavailable.'
    });
  }
}