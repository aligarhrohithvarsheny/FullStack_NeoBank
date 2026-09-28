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
  
  // Verification
  ifscVerified = false;
  accountVerified = false;

  constructor(
    private fb: FormBuilder,
    private accountService: AllocationAccountService
  ) {
    this.accountForm = this.fb.group({
      allocationId: ['', [Validators.required]],
      accountNumber: ['', [Validators.required, Validators.pattern(/^\d{9,18}$/)]],
      ifscCode: ['', [Validators.required, Validators.pattern(/^[A-Z]{4}0[A-Z0-9]{6}$/)]],
      accountHolderName: ['', [Validators.required, Validators.minLength(3)]],
      bankName: ['', [Validators.required]],
      accountType: ['CURRENT', [Validators.required]],
      branchName: ['', [Validators.required]],
      city: ['', [Validators.required]],
      location: ['', [Validators.required]],
      state: ['', [Validators.required]]
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
    this.loadAccountsByLocation();
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

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    const payload = {
      ...this.accountForm.value,
      adminId: 1,  // TODO: Get from session
      adminName: 'Admin Name'  // TODO: Get from session
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
    const ifscCode = this.accountForm.get('ifscCode')?.value;
    if (!ifscCode) {
      this.errorMessage = 'Please enter IFSC code first';
      return;
    }

    this.loading = true;
    this.accountService.verifyIfsc(ifscCode).subscribe(
      (response: any) => {
        if (response.success) {
          this.ifscVerified = true;
          this.successMessage = 'IFSC code verified successfully!';
          this.accountForm.get('bankName')?.patchValue(response.bankName);
          this.accountForm.get('branchName')?.patchValue(response.branchName);
          this.accountForm.get('city')?.patchValue(response.city);
          this.accountForm.get('state')?.patchValue(response.state);
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
    const accountNumber = this.accountForm.get('accountNumber')?.value;
    const ifscCode = this.accountForm.get('ifscCode')?.value;

    if (!accountNumber || !ifscCode) {
      this.errorMessage = 'Please enter both account number and IFSC code';
      return;
    }

    this.loading = true;
    this.accountService.verifyAccountNumber(accountNumber, ifscCode).subscribe(
      (response: any) => {
        if (response.success) {
          this.accountVerified = true;
          this.successMessage = 'Account verified successfully!';
          this.accountForm.get('accountHolderName')?.patchValue(response.accountHolderName);
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
    this.accountForm.reset({ accountType: 'CURRENT' });
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
}
