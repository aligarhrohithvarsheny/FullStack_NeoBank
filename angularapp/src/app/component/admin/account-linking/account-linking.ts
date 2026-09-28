import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { AllocationAccountService } from '../../../service/allocation-account.service';

@Component({
  selector: 'app-account-linking',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, HttpClientModule],
  templateUrl: './account-linking.html',
  styleUrls: ['./account-linking.css']
})
export class AccountLinkingComponent implements OnInit {
  accountForm!: FormGroup;
  chequeForm!: FormGroup;
  
  // UI State
  step = 1;  // Step 1: Account Details, Step 2: Cheque Details
  loading = false;
  errorMessage = '';
  successMessage = '';
  
  // Data
  linkedAccounts: any[] = [];
  selectedCity = '';
  selectedBranch = '';
  allocations: any[] = [];
  activeAllocations: any[] = [];
  cityOptions: string[] = [];
  readonly bankOptions = [
    { name: 'NeoBank', ifscCode: 'NEOB0000001' },
    { name: 'ExyVault', ifscCode: 'EZYV000123' }
  ];
  
  // Verification
  ifscVerified = false;
  accountVerified = false;

  constructor(
    private fb: FormBuilder,
    private accountService: AllocationAccountService
  ) {
    this.accountForm = this.fb.group({
      allocationId: ['', [Validators.required]],
      accountNumber: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{6,20}$/i)]],
      ifscCode: ['NEOB0000001', [Validators.required, Validators.pattern(/^(NEOB0000001|EZYV000123)$/i)]],
      accountHolderName: ['', [Validators.required, Validators.minLength(3)]],
      bankName: ['NeoBank', [Validators.required]],
      accountType: ['CURRENT', [Validators.required]],
      branchName: ['', [Validators.required]],
      city: ['', [Validators.required]],
      location: [''],
      state: ['']
    });

    this.chequeForm = this.fb.group({
      chequeNumber: ['', [Validators.required, Validators.pattern(/^\d{6,10}$/)]],
      chequeHolderName: ['', [Validators.required]],
      chequeDate: ['', [Validators.required]],
      chequeBank: ['', [Validators.required]],
      chequeImageUrl: ['']
    });
  }

  ngOnInit(): void {
    this.loadAvailableCities();
    this.loadAllocations();
  }

  get branchOptions(): string[] {
    const city = this.selectedCity.trim().toLowerCase();
    return [...new Set(this.activeAllocations
      .filter(allocation => !city || String(allocation.city || '').toLowerCase() === city)
      .map(allocation => String(allocation.branchName || '').trim())
      .filter(Boolean))].sort((a, b) => a.localeCompare(b));
  }

  get selectedAllocation(): any | null {
    return this.allocations.find(item => String(item.id) === String(this.accountForm.get('allocationId')?.value)) || null;
  }

  loadAvailableCities(): void {
    this.accountService.getAvailableCities().subscribe({
      next: cities => {
        const names = (cities || []).map(city => String(city.city || '').trim()).filter(Boolean);
        const hodCity = String(this.getHodSession()?.assignedCity || '').trim();
        this.cityOptions = [...new Set([...names, ...(hodCity ? [hodCity] : [])])]
          .sort((a, b) => a.localeCompare(b));
      },
      error: () => {
        const hodCity = String(this.getHodSession()?.assignedCity || '').trim();
        this.cityOptions = hodCity ? [hodCity] : [];
      }
    });
  }

  loadAllocations(): void {
    this.accountService.getAllocations().subscribe({
      next: (response: any) => {
        const allocations = response.content || response.allocations || [];
        this.activeAllocations = allocations.filter((allocation: any) => allocation.status === 'ACTIVE');
        this.allocations = this.activeAllocations.filter((allocation: any) =>
          allocation.status === 'ACTIVE' && (!allocation.accountStatus || allocation.accountStatus === 'NOT_LINKED')
        );
        const allocationCities = this.activeAllocations.map((allocation: any) => String(allocation.city || '').trim()).filter(Boolean);
        this.cityOptions = [...new Set([...this.cityOptions, ...allocationCities])].sort((a, b) => a.localeCompare(b));
      },
      error: (error: any) => {
        this.errorMessage = error.error?.message || 'Failed to load active allocations.';
      }
    });
  }

  onBankSelected(): void {
    const selectedBank = this.bankOptions.find(option => option.name === this.accountForm.get('bankName')?.value);
    this.ifscVerified = false;
    this.accountVerified = false;
    this.accountForm.patchValue({
      ifscCode: selectedBank?.ifscCode || '',
      accountNumber: '',
      accountHolderName: ''
    });
    this.errorMessage = '';
    this.successMessage = '';
    if (selectedBank) this.verifyIfsc();
  }

  applySelectedAllocation(): void {
    const allocation = this.allocations.find(item => String(item.id) === String(this.accountForm.get('allocationId')?.value));
    if (!allocation) return;
    const hod = this.getHodSession();
    this.accountForm.patchValue({
      branchName: allocation.branchName || '',
      city: allocation.city || hod?.assignedCity || this.accountForm.get('city')?.value || '',
      location: allocation.branchName || this.accountForm.get('location')?.value || '',
      state: allocation.state || this.accountForm.get('state')?.value || ''
    });
  }

  onAccountCityChanged(): void {
    this.accountForm.get('city')?.markAsTouched();
    this.applySelectedAllocation();
  }

  onFilterCityChanged(): void {
    this.selectedBranch = '';
    this.linkedAccounts = [];
  }

  /**
   * Load accounts for HOD Dashboard by city and branch
   */
  loadAccountsByLocation(): void {
    if (!this.selectedCity || !this.selectedBranch) return;

    this.loading = true;
    this.accountService.getAccountsByLocation(this.selectedCity, this.selectedBranch).subscribe(
      (response: any) => {
        this.linkedAccounts = response.accounts || [];
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = 'Failed to load accounts: ' + error.message;
        this.loading = false;
      }
    );
  }

  /**
   * Step 1: Link account to allocation
   */
  linkAccount(): void {
    if (!this.accountForm.valid) {
      this.errorMessage = 'Please fill all required fields correctly';
      return;
    }
    if (!this.ifscVerified || !this.accountVerified) {
      this.errorMessage = 'Verify the approved bank IFSC and account number before linking.';
      return;
    }

    const hod = this.getHodSession();
    if (!hod?.id || sessionStorage.getItem('userRole') !== 'HOD') {
      this.errorMessage = 'Sign in with a HOD account to link allocation accounts.';
      return;
    }

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    const payload = {
      ...this.accountForm.value,
      accountNumber: String(this.accountForm.value.accountNumber).trim().toUpperCase(),
      ifscCode: String(this.accountForm.value.ifscCode).trim().toUpperCase(),
      adminId: hod.id,
      adminName: hod.name
    };

    this.accountService.linkAccountToAllocation(payload).subscribe(
      (response: any) => {
        if (response.success) {
          this.successMessage = 'Account linked successfully! Please proceed to add cheque details.';
          this.step = 2;
          this.chequeForm.patchValue({
            chequeHolderName: this.accountForm.value.accountHolderName
          });
        }
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = error.error?.message || 'Failed to link account';
        this.loading = false;
      }
    );
  }

  /**
   * Step 2: Add cheque details for verification
   */
  addChequeDetails(): void {
    if (!this.chequeForm.valid) {
      this.errorMessage = 'Please fill all required fields correctly';
      return;
    }

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    const payload = {
      allocationId: this.accountForm.value.allocationId,
      ...this.chequeForm.value
    };

    this.accountService.addChequeDetails(payload).subscribe(
      (response: any) => {
        if (response.success) {
          this.successMessage = 'Cheque details submitted for verification!';
          this.resetForms();
          this.loadAccountsByLocation();
        }
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = error.error?.message || 'Failed to add cheque details';
        this.loading = false;
      }
    );
  }

  /**
   * Verify IFSC code
   */
  verifyIfsc(): void {
    const ifscCode = String(this.accountForm.get('ifscCode')?.value || '').trim().toUpperCase();
    if (!ifscCode) {
      this.errorMessage = 'Please enter IFSC code first';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.accountService.verifyIfsc(ifscCode).subscribe(
      (response: any) => {
        if (response.success) {
          this.ifscVerified = true;
          this.accountForm.patchValue({ bankName: response.bankName, ifscCode: response.ifscCode });
          this.successMessage = `${response.bankName} IFSC verified.`;
        }
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = error.error?.message || 'IFSC verification failed';
        this.ifscVerified = false;
        this.loading = false;
      }
    );
  }

  /**
   * Verify account number with bank
   */
  verifyAccountNumber(): void {
    const accountNumber = String(this.accountForm.get('accountNumber')?.value || '').trim().toUpperCase();
    const ifscCode = String(this.accountForm.get('ifscCode')?.value || '').trim().toUpperCase();

    if (!accountNumber || !ifscCode) {
      this.errorMessage = 'Please enter both account number and IFSC code';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.accountService.verifyAccountNumber(accountNumber, ifscCode).subscribe(
      (response: any) => {
        if (response.success) {
          this.accountVerified = true;
          this.accountForm.patchValue({
            accountNumber: response.accountNumber,
            ifscCode: response.ifscCode,
            bankName: response.bankName,
            accountHolderName: response.accountHolderName,
            accountType: response.accountType,
            city: response.city || this.accountForm.get('city')?.value,
            state: response.state || this.accountForm.get('state')?.value
          });
          this.applySelectedAllocation();
          this.successMessage = `${response.bankName} account verified.`;
        }
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = error.error?.message || 'Account verification failed';
        this.accountVerified = false;
        this.loading = false;
      }
    );
  }

  /**
   * Get status badge color
   */
  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'VERIFIED':
        return 'badge-success';
      case 'PENDING':
        return 'badge-warning';
      case 'REJECTED':
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Reset forms
   */
  resetForms(): void {
    this.step = 1;
    const bank = this.bankOptions[0];
    this.accountForm.reset({ accountType: 'CURRENT', bankName: bank.name, ifscCode: bank.ifscCode });
    this.chequeForm.reset();
    this.ifscVerified = false;
    this.accountVerified = false;
    this.successMessage = '';
    this.errorMessage = '';
  }

  /**
   * Download cheque image
   */
  downloadChequeImage(account: any): void {
    if (account.chequeImageUrl) {
      window.open(account.chequeImageUrl, '_blank');
    }
  }

  private getHodSession(): any | null {
    try {
      return JSON.parse(sessionStorage.getItem('admin') || 'null');
    } catch {
      return null;
    }
  }
}
