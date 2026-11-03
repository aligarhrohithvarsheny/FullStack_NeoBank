import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { environment } from '../../../../environment/environment';

interface CustomerSummary {
  id?: number | string;
  username?: string;
  name?: string;
  email?: string;
  accountNumber?: string;
  status?: string;
  joinDate?: string;
  account?: Record<string, any>;
}

interface CustomerResource {
  rows: any[];
  error: string;
}

@Component({
  selector: 'app-customer-360',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './customer-360.html',
  styleUrl: './customer-360.css'
})
export class Customer360 implements OnInit {
  customers: CustomerSummary[] = [];
  searchQuery = '';
  selectedCustomer: CustomerSummary | null = null;
  isLoadingCustomers = false;
  isLoadingProfile = false;
  profileError = '';
  showSensitiveData = false;
  resourceErrors: Record<string, string> = {};
  transactions: any[] = [];
  kycRecords: any[] = [];
  complaints: any[] = [];
  loans: any[] = [];
  homeLoans: any[] = [];
  recurringDeposits: any[] = [];
  ecsMandates: any[] = [];
  bankFormUploads: any[] = [];
  loanAgreements: any[] = [];
  nominees: any[] = [];
  linkedAccounts: any[] = [];
  currentAccount: any | null = null;
  familyLinks: any[] = [];
  timeline: any[] = [];
  nomineeDraft = { nomineeName: '', relationship: '', sharePercentage: 100 };
  savingNominee = false;
  nomineeError = '';
  private authRedirecting = false;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    if (typeof sessionStorage === 'undefined') return;
    if (!sessionStorage.getItem('adminSessionToken')) {
      void this.router.navigate(['/admin/login'], {
        queryParams: { role: sessionStorage.getItem('userRole') || 'ADMIN' }
      });
      return;
    }
    this.loadCustomers();
    const navigationState = this.router.getCurrentNavigation()?.extras.state as { accountNumber?: unknown } | undefined;
    const accountNumber = typeof navigationState?.accountNumber === 'string'
      ? navigationState.accountNumber.trim()
      : '';
    if (accountNumber) {
      this.searchQuery = accountNumber;
      this.loadByAccountNumber(accountNumber);
    }
  }

  goBack(): void {
    const destination = typeof sessionStorage !== 'undefined'
      && sessionStorage.getItem('userRole') === 'MANAGER'
      ? '/manager/dashboard'
      : '/admin/dashboard';
    void this.router.navigateByUrl(destination);
  }

  get filteredCustomers(): CustomerSummary[] {
    const query = this.searchQuery.trim().toLowerCase();
    if (!query) return this.customers.slice(0, 8);
    return this.customers.filter(customer => [
      customer.name, customer.username, customer.email, customer.accountNumber,
      customer.account?.['customerId'], customer.id
    ].some(value => String(value || '').toLowerCase().includes(query))).slice(0, 8);
  }

  loadCustomers(): void {
    this.isLoadingCustomers = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/users/admin/all?page=0&size=500`, this.adminRequestOptions()).subscribe({
      next: result => {
        const rows = Array.isArray(result) ? result : result?.content || [];
        this.customers = rows.map((row: any) => this.toCustomer(row));
        this.isLoadingCustomers = false;
      },
      error: error => {
        this.isLoadingCustomers = false;
        this.handleAdminAuthError(error);
        this.profileError = error?.error?.message || 'Unable to load the customer directory.';
      }
    });
  }

  searchCustomer(): void {
    const query = this.searchQuery.trim();
    if (!query) return;
    const match = this.filteredCustomers.find(customer =>
      String(customer.accountNumber || '') === query
      || String(customer.email || '').toLowerCase() === query.toLowerCase()
      || String(customer.id || '') === query);
    if (match?.accountNumber) {
      this.selectCustomer(match);
      return;
    }
    this.loadByAccountNumber(query);
  }

  selectCustomer(customer: CustomerSummary): void {
    if (!customer.accountNumber) {
      this.profileError = 'This customer does not have an account number to load a complete relationship profile.';
      return;
    }
    this.searchQuery = customer.accountNumber;
    this.loadByAccountNumber(customer.accountNumber);
  }

  loadByAccountNumber(accountNumber: string): void {
    this.isLoadingProfile = true;
    this.profileError = '';
    this.resourceErrors = {};
    this.currentAccount = null;
    this.http.get<any>(
      `${environment.apiBaseUrl}/api/users/account/${encodeURIComponent(accountNumber)}`,
      this.adminRequestOptions()
    ).subscribe({
      next: user => {
        this.selectedCustomer = this.toCustomer(user);
        this.loadRelatedData(accountNumber);
      },
      error: error => {
        this.selectedCustomer = null;
        this.isLoadingProfile = false;
        this.handleAdminAuthError(error);
        this.profileError = error?.error?.message || 'No customer profile was found for that account number.';
      }
    });
  }

  private loadRelatedData(accountNumber: string): void {
    const root = environment.apiBaseUrl;
    const customerId = this.selectedCustomer?.account?.['customerId'];
    const requests: Record<string, Observable<CustomerResource>> = {
      transactions: this.resource(`${root}/api/transactions/account/${encodeURIComponent(accountNumber)}?page=0&size=100`,
        ['content', 'transactions']),
      kyc: this.resource(`${root}/api/kyc/account/${encodeURIComponent(accountNumber)}/all`, ['kyc', 'records']),
      complaints: this.resource(`${root}/api/support-tickets/account/${encodeURIComponent(accountNumber)}`, ['tickets', 'complaints']),
      loans: this.resource(`${root}/api/loans/account/${encodeURIComponent(accountNumber)}`, ['loans', 'applicantLoans', 'childLoans']),
      nominees: this.resource(`${root}/api/customer-nominees/account/${encodeURIComponent(accountNumber)}`, ['nominees']),
      currentAccounts: this.resource(`${root}/api/current-accounts/linked-accounts/by-savings/${encodeURIComponent(accountNumber)}`,
        ['accounts', 'linkedAccounts']),
      currentAccount: customerId
        ? this.resource(`${root}/api/current-accounts/customer/${encodeURIComponent(String(customerId))}`,
          ['account'], undefined, true)
        : of({ rows: [], error: '' }),
      salaryAccount: this.resource(`${root}/api/salary-accounts/number/${encodeURIComponent(accountNumber)}`, ['accounts']),
      family: this.resource(`${root}/api/family/account-lookup/${encodeURIComponent(accountNumber)}?userId=${encodeURIComponent(String(this.selectedCustomer?.id || ''))}`,
        ['accounts', 'linkedAccounts', 'familyAccounts']),
      profileHistory: this.resource(`${root}/api/users/profile-update/history/account/${encodeURIComponent(accountNumber)}`,
        ['history', 'changes']),
      currentAccountHistory: this.resource(`${root}/api/current-accounts/edit-history/account/${encodeURIComponent(accountNumber)}`,
        ['history', 'changes']),
      familyHistory: this.resource(`${root}/api/family/admin/joint-accounts/history?userId=${encodeURIComponent(String(this.selectedCustomer?.id || ''))}`,
        ['history', 'changes'], { 'X-Admin-Email': this.getAdminEmail() }),
      fixedDeposits: this.resource(`${root}/api/fixed-deposits/account/${encodeURIComponent(accountNumber)}`,
        ['fixedDeposits', 'deposits']),
      goldLoans: this.resource(`${root}/api/gold-loans/account/${encodeURIComponent(accountNumber)}`, ['loans']),
      homeLoans: this.resource(`${root}/api/home-loans/account/${encodeURIComponent(accountNumber)}`, ['loans', 'applications']),
      recurringDeposits: this.resource(`${root}/api/recurring-deposits/admin/account/${encodeURIComponent(accountNumber)}`,
        ['recurringDeposits', 'deposits']),
      ecsMandates: this.resource(`${root}/api/ecs-mandates/admin/account/${encodeURIComponent(accountNumber)}`, ['mandates']),
      bankFormUploads: this.resource(`${root}/api/admin/bank-forms/uploads?accountNumber=${encodeURIComponent(accountNumber)}`,
        ['uploads']),
      loanAgreements: this.resource(`${root}/api/loan-agreements/account/${encodeURIComponent(accountNumber)}`, ['agreements'])
    };

    forkJoin(requests).pipe(
      switchMap(resources => {
        const linkedAccountNumber = resources['currentAccounts'].rows
          .find(account => account.currentAccountNumber)?.currentAccountNumber;
        if (resources['currentAccount'].rows.length || !linkedAccountNumber) {
          return of({ resources, currentAccount: resources['currentAccount'] });
        }
        return this.resource(`${root}/api/current-accounts/number/${encodeURIComponent(linkedAccountNumber)}`,
          ['account'], undefined, true).pipe(
            map(currentAccount => ({
              resources,
              currentAccount: {
                ...currentAccount,
                error: currentAccount.error || resources['currentAccount'].error
              }
            }))
          );
      })
    ).subscribe(({ resources, currentAccount }) => {
      this.transactions = resources['transactions'].rows;
      this.kycRecords = resources['kyc'].rows;
      this.complaints = resources['complaints'].rows;
      this.homeLoans = resources['homeLoans'].rows;
      this.loans = [...resources['loans'].rows, ...resources['goldLoans'].rows];
      this.recurringDeposits = resources['recurringDeposits'].rows;
      this.ecsMandates = resources['ecsMandates'].rows;
      this.bankFormUploads = resources['bankFormUploads'].rows;
      this.loanAgreements = resources['loanAgreements'].rows;
      this.nominees = resources['nominees'].rows;
      this.linkedAccounts = [
        ...resources['currentAccounts'].rows.filter(account =>
          account.currentAccountNumber !== currentAccount.rows[0]?.accountNumber),
        ...resources['salaryAccount'].rows,
        ...resources['fixedDeposits'].rows
      ];
      this.currentAccount = currentAccount.rows[0] || null;
      this.familyLinks = resources['family'].rows;
      this.timeline = [
        ...resources['profileHistory'].rows,
        ...resources['currentAccountHistory'].rows,
        ...resources['familyHistory'].rows
      ].sort((first, second) => this.timestamp(second) - this.timestamp(first));
      Object.entries({ ...resources, currentAccount }).forEach(([name, resource]) => {
        if (resource.error) this.resourceErrors[name] = resource.error;
      });
      this.isLoadingProfile = false;
    });
  }

  addNominee(): void {
    const accountNumber = this.selectedCustomer?.accountNumber;
    if (!accountNumber || this.savingNominee) return;
    this.savingNominee = true;
    this.nomineeError = '';
    this.http.post<any>(`${environment.apiBaseUrl}/api/customer-nominees`, {
      accountNumber,
      ...this.nomineeDraft
    }, this.adminRequestOptions()).subscribe({
      next: nominee => {
        this.nominees = [nominee, ...this.nominees];
        this.nomineeDraft = { nomineeName: '', relationship: '', sharePercentage: 100 };
        this.savingNominee = false;
      },
      error: error => {
        this.handleAdminAuthError(error);
        this.nomineeError = error?.error?.message || 'Unable to save nominee details.';
        this.savingNominee = false;
      }
    });
  }

  mask(value: unknown): string {
    const text = String(value || '');
    if (this.showSensitiveData || text.length < 5) return text || '—';
    return `${'•'.repeat(Math.max(0, text.length - 4))}${text.slice(-4)}`;
  }

  displayName(customer: CustomerSummary): string {
    return customer.account?.['name'] || customer.name || customer.username || customer.email || 'Customer';
  }

  resourceFailure(name: string): string {
    return this.resourceErrors[name] || '';
  }

  homeLoanDocuments(loan: any): Array<{ type: string; label: string; index?: number; fileName: string }> {
    const documents: Array<{ type: string; label: string; index?: number; fileName: string }> = [];
    const singles: Array<[string, string, string]> = [
      ['fdReceiptPath', 'fdReceipt', 'FD receipt'],
      ['modelDocPath', 'model', 'Loan model'],
      ['signaturePath', 'signature', 'Signature'],
      ['vehicleInvoicePath', 'vehicleInvoice', 'Vehicle invoice'],
      ['landRecordPath', 'landRecord', 'Land record'],
      ['agricultureEstimatePath', 'agricultureEstimate', 'Agriculture estimate'],
      ['cropEstimatePath', 'cropEstimate', 'Crop estimate']
    ];
    for (const [pathKey, type, label] of singles) {
      const path = loan?.[pathKey];
      if (typeof path === 'string' && path.trim()) {
        documents.push({ type, label, fileName: path.split(/[\\/]/).pop() || label });
      }
    }
    const images = typeof loan?.vehicleImagePaths === 'string'
      ? loan.vehicleImagePaths.split(',').filter((path: string) => path.trim())
      : [];
    images.forEach((path: string, index: number) => documents.push({
      type: 'vehicleImages',
      label: `Vehicle image ${index + 1}`,
      index,
      fileName: path.trim().split(/[\\/]/).pop() || `vehicle-image-${index + 1}`
    }));
    return documents;
  }

  downloadBankForm(upload: any): void {
    this.downloadFile(
      `${environment.apiBaseUrl}/api/admin/bank-forms/uploads/${encodeURIComponent(String(upload.id))}/file`,
      upload.originalFileName || `bank-form-${upload.id}`
    );
  }

  downloadHomeLoanDocument(loan: any, document: { type: string; index?: number; fileName: string }): void {
    const query = document.index === undefined ? '' : `?index=${document.index}`;
    this.downloadFile(
      `${environment.apiBaseUrl}/api/home-loans/${encodeURIComponent(String(loan.id))}/documents/${encodeURIComponent(document.type)}/file${query}`,
      document.fileName
    );
  }

  downloadPersonalLoanForm(loan: any): void {
    this.downloadFile(
      `${environment.apiBaseUrl}/api/loans/download-personal-loan-form/${encodeURIComponent(String(loan.id))}`,
      `personal-loan-application-${loan.id}.pdf`
    );
  }

  downloadEcsConsent(mandate: any): void {
    this.downloadFile(
      `${environment.apiBaseUrl}/api/ecs-mandates/${encodeURIComponent(String(mandate.id))}/consent-record`,
      `ecs-consent-record-${mandate.id}.html`
    );
  }

  downloadLoanAgreement(agreement: any): void {
    this.downloadFile(
      `${environment.apiBaseUrl}/api/loan-agreements/${encodeURIComponent(String(agreement.id))}/pdf`,
      `signed-loan-agreement-${agreement.id}.pdf`
    );
  }

  hasSignedAgreement(agreement: any): boolean {
    return ['SIGNED', 'APPROVED'].includes(String(agreement?.status || '').toUpperCase());
  }

  hasEcsConsent(mandate: any): boolean {
    return String(mandate?.productType || '').toUpperCase() === 'RECURRING_DEPOSIT'
      && Boolean(mandate?.customerConsentAt || mandate?.customerConsentName);
  }

  hasPersonalLoanForm(loan: any): boolean {
    return Boolean(loan?.id && loan?.personalLoanFormPath
      && String(loan?.type || '').toLowerCase().includes('personal'));
  }

  private resource(
    url: string,
    keys: string[],
    headers?: Record<string, string>,
    notFoundIsEmpty = false
  ): Observable<CustomerResource> {
    const requestHeaders = this.adminRequestHeaders(headers);
    return this.http.get<any>(url, { headers: requestHeaders }).pipe(
      map(value => ({ rows: this.toRows(value, keys), error: '' })),
      catchError(error => {
        this.handleAdminAuthError(error);
        return of({
          rows: [],
          error: notFoundIsEmpty && error?.status === 404
            ? ''
            : error?.error?.message || `Could not load ${keys[0]} data.`
        });
      })
    );
  }

  private adminRequestOptions(headers?: Record<string, string>): { headers: HttpHeaders } {
    return { headers: this.adminRequestHeaders(headers) };
  }

  private downloadFile(url: string, fileName: string): void {
    this.http.get(url, {
      headers: this.adminRequestHeaders(),
      responseType: 'blob'
    }).subscribe({
      next: blob => {
        if (!blob.size) {
          this.resourceErrors['downloads'] = 'The requested file was empty.';
          return;
        }
        const objectUrl = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = objectUrl;
        anchor.download = fileName;
        anchor.click();
        URL.revokeObjectURL(objectUrl);
        delete this.resourceErrors['downloads'];
      },
      error: error => {
        this.handleAdminAuthError(error);
        this.resourceErrors['downloads'] = error?.error?.message || 'Unable to download the requested file.';
      }
    });
  }

  private adminRequestHeaders(headers?: Record<string, string>): HttpHeaders {
    let requestHeaders = new HttpHeaders(headers || {});
    if (typeof sessionStorage !== 'undefined') {
      const token = sessionStorage.getItem('adminSessionToken');
      if (token) requestHeaders = requestHeaders.set('Authorization', `Bearer ${token}`);
    }
    return requestHeaders;
  }

  private handleAdminAuthError(error: any): void {
    if (error?.status !== 401 || this.authRedirecting) return;
    this.authRedirecting = true;
    sessionStorage.removeItem('adminSessionToken');
    void this.router.navigate(['/admin/login'], {
      queryParams: { role: sessionStorage.getItem('userRole') || 'ADMIN' }
    });
  }

  private getAdminEmail(): string {
    if (typeof sessionStorage === 'undefined') return '';
    const direct = sessionStorage.getItem('adminEmail');
    if (direct) return direct;
    try {
      const admin = JSON.parse(sessionStorage.getItem('admin') || '{}');
      return admin.email || admin.username || '';
    } catch {
      return '';
    }
  }

  private toRows(value: any, keys: string[]): any[] {
    if (Array.isArray(value)) return value;
    if (!value || typeof value !== 'object') return [];
    for (const key of keys) {
      if (Array.isArray(value[key])) return value[key];
    }
    if (Array.isArray(value.content)) return value.content;
    if (value.id || value.accountNumber || value.ticketId || value.transactionId) return [value];
    return [];
  }

  private toCustomer(value: any): CustomerSummary {
    return {
      ...value,
      name: value?.account?.name || value?.name || value?.username,
      accountNumber: value?.accountNumber || value?.account?.accountNumber
    };
  }

  private timestamp(entry: any): number {
    const date = entry?.createdAt || entry?.updatedAt || entry?.date || entry?.submittedDate
      || entry?.effectiveDate || entry?.statusChangedAt;
    const parsed = Date.parse(date || '');
    return Number.isNaN(parsed) ? 0 : parsed;
  }
}
