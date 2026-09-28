import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { AllocationAccountService } from '../../../service/allocation-account.service';

@Component({
  selector: 'app-account-verification',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, HttpClientModule],
  templateUrl: './account-verification.html',
  styleUrls: ['./account-verification.css']
})
export class AccountVerificationComponent implements OnInit {
  verificationForm!: FormGroup;

  pendingCheques: any[] = [];
  verifiedAccounts: any[] = [];
  selectedCheque: any = null;
  activeTab = 'pending';

  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private accountService: AllocationAccountService
  ) {
    this.verificationForm = this.fb.group({
      verificationNotes: [''],
      approved: [true, Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadPendingCheques();
    this.loadVerifiedAccounts();
  }

  /**
   * Load all pending cheques for verification
   */
  loadPendingCheques(): void {
    this.loading = true;
    this.accountService.getPendingCheques().subscribe(
      (response: any) => {
        this.pendingCheques = response.cheques || [];
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = 'Failed to load pending cheques: ' + error.message;
        this.loading = false;
      }
    );
  }

  /**
   * Load all verified accounts
   */
  loadVerifiedAccounts(): void {
    this.accountService.getVerifiedAccounts().subscribe(
      (response: any) => {
        this.verifiedAccounts = response.accounts || [];
      },
      (error: any) => {
        this.errorMessage = 'Failed to load verified accounts: ' + error.message;
      }
    );
  }

  /**
   * Select a cheque for verification
   */
  selectCheque(cheque: any): void {
    this.selectedCheque = cheque;
    this.verificationForm.reset({ approved: true });
    this.errorMessage = '';
    this.successMessage = '';
  }

  /**
   * Verify and approve/reject cheque
   */
  verifyCheque(): void {
    if (!this.selectedCheque) {
      this.errorMessage = 'No cheque selected';
      return;
    }

    this.loading = true;
    const payload = {
      allocationId: this.selectedCheque.allocationId,
      approved: this.verificationForm.value.approved,
      verificationNotes: this.verificationForm.value.verificationNotes,
      verifiedByAdminId: 1,  // TODO: Get from session
      verifiedByAdminName: 'Admin Name'  // TODO: Get from session
    };

    this.accountService.verifyChequeAndApprove(payload).subscribe(
      (response: any) => {
        if (response.success) {
          this.successMessage = 'Cheque verified successfully!';
          this.loadPendingCheques();
          this.loadVerifiedAccounts();
          this.selectedCheque = null;
          this.verificationForm.reset({ approved: true });
        }
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = error.error?.message || 'Failed to verify cheque';
        this.loading = false;
      }
    );
  }

  /**
   * Reject cheque
   */
  rejectCheque(): void {
    if (!this.selectedCheque) {
      this.errorMessage = 'No cheque selected';
      return;
    }

    const reason = this.verificationForm.value.verificationNotes;
    if (!reason) {
      this.errorMessage = 'Please provide a reason for rejection';
      return;
    }

    this.loading = true;
    const payload = {
      allocationId: this.selectedCheque.allocationId,
      reason: reason,
      rejectedByAdminId: 1,  // TODO: Get from session
      rejectedByAdminName: 'Admin Name'  // TODO: Get from session
    };

    this.accountService.rejectChequeVerification(payload).subscribe(
      (response: any) => {
        if (response.success) {
          this.successMessage = 'Cheque rejected successfully!';
          this.loadPendingCheques();
          this.selectedCheque = null;
          this.verificationForm.reset({ approved: true });
        }
        this.loading = false;
      },
      (error: any) => {
        this.errorMessage = error.error?.message || 'Failed to reject cheque';
        this.loading = false;
      }
    );
  }

  /**
   * Update account status (block/unblock)
   */
  updateAccountStatus(account: any, status: string): void {
    const reason = prompt(`Enter reason for ${status === 'BLOCKED' ? 'blocking' : 'unblocking'} this account:`);
    if (!reason) return;

    this.accountService.updateAccountStatus(account.id, status, reason).subscribe(
      (response: any) => {
        if (response.success) {
          this.successMessage = `Account ${status === 'BLOCKED' ? 'blocked' : 'unblocked'} successfully!`;
          this.loadVerifiedAccounts();
        }
      },
      (error: any) => {
        this.errorMessage = error.error?.message || 'Failed to update account status';
      }
    );
  }

  /**
   * Get badge class based on status
   */
  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'VERIFIED':
        return 'badge-success';
      case 'PENDING':
        return 'badge-warning';
      case 'REJECTED':
        return 'badge-danger';
      case 'BLOCKED':
        return 'badge-dark';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Download cheque image
   */
  downloadCheque(cheque: any): void {
    if (cheque.chequeImageUrl) {
      window.open(cheque.chequeImageUrl, '_blank');
    }
  }

  /**
   * View cheque details
   */
  viewDetails(cheque: any): void {
    alert(`
Cheque Details:
- Cheque Number: ${cheque.chequeNumber}
- Holder Name: ${cheque.chequeHolderName}
- Cheque Date: ${cheque.chequeDate}
- Bank: ${cheque.chequeBank}
- Account: ${cheque.accountNumber}
- IFSC: ${cheque.ifscCode}
    `);
  }
}
