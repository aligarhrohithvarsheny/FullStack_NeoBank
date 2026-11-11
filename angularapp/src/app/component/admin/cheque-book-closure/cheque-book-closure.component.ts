import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ChequeService } from '../../../service/cheque.service';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-cheque-book-closure',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cheque-book-closure.component.html',
  styleUrls: ['./cheque-book-closure.component.css']
})
export class ChequeBookClosureComponent {
  closureAccountNumber = '';
  closureBooks: any[] = [];
  closureHistory: any[] = [];
  chequeLookupNumber = '';
  chequeLookupResult: any = null;
  isLoadingClosureBooks = false;
  isClosingBooks = false;
  isLookingUpCheque = false;
  showIssueBookForm = false;
  isLoadingIssueEligibility = false;
  isIssuingBook = false;
  storedCibilScore: number | null = null;
  isCibilEligible = false;
  newBookLeafCount = 30;
  newBookCost = 199;
  private readonly adminName: string;

  get hasActiveBooks(): boolean {
    return this.closureBooks.some(book => String(book.status || '').toUpperCase() !== 'CLOSED');
  }

  constructor(
    private readonly chequeService: ChequeService,
    private readonly alertService: AlertService,
    private readonly router: Router
  ) {
    let adminName = 'Admin';
    try {
      const admin = JSON.parse(sessionStorage.getItem('admin') || '{}');
      adminName = admin.username || admin.name || adminName;
    } catch (error) {
      console.error('Error parsing admin data:', error);
    }
    this.adminName = adminName;
  }

  goBack(): void {
    void this.router.navigateByUrl('/admin/dashboard');
  }

  fetchChequeBooksForClosure(): void {
    const accountNumber = this.closureAccountNumber.trim();
    if (!accountNumber) {
      this.alertService.error('Missing Info', 'Enter an account number to fetch cheque books');
      return;
    }

    this.showIssueBookForm = false;
    this.storedCibilScore = null;
    this.isCibilEligible = false;
    this.isLoadingClosureBooks = true;
    this.chequeService.getChequeBooksByAccountNumber(accountNumber).subscribe({
      next: books => {
        this.closureBooks = books || [];
        this.isLoadingClosureBooks = false;
        this.loadChequeClosureHistory(accountNumber);
      },
      error: err => {
        this.isLoadingClosureBooks = false;
        this.closureBooks = [];
        this.closureHistory = [];
        this.alertService.error(
          'Unable to load cheque books',
          err?.error?.error || 'Please check the account number and try again.'
        );
      }
    });
  }

  openIssueBookForm(): void {
    const accountNumber = this.closureAccountNumber.trim();
    if (!accountNumber) {
      this.alertService.error('Missing Info', 'Find an account before issuing a cheque book');
      return;
    }

    this.showIssueBookForm = true;
    this.isLoadingIssueEligibility = true;
    this.storedCibilScore = null;
    this.isCibilEligible = false;
    this.chequeService.getAdminChequeBookIssueEligibility(accountNumber).subscribe({
      next: eligibility => {
        this.storedCibilScore = eligibility?.storedCibilScore ?? null;
        this.isCibilEligible = eligibility?.eligible === true;
        this.isLoadingIssueEligibility = false;
      },
      error: err => {
        this.isLoadingIssueEligibility = false;
        this.alertService.error(
          'Eligibility unavailable',
          err?.error?.error || 'Unable to retrieve the stored CIBIL score for this account.'
        );
      }
    });
  }

  issueNewChequeBook(): void {
    const accountNumber = this.closureAccountNumber.trim();
    if (!accountNumber || !this.isCibilEligible) {
      this.alertService.error('Not Eligible', 'The account must have a stored CIBIL score of at least 300.');
      return;
    }
    if (!Number.isInteger(this.newBookLeafCount) || this.newBookLeafCount < 1 || this.newBookLeafCount > 1000) {
      this.alertService.error('Invalid leaf count', 'Enter between 1 and 1000 cheque leaves.');
      return;
    }
    if (!Number.isFinite(this.newBookCost) || this.newBookCost <= 0) {
      this.alertService.error('Invalid cost', 'Enter a cheque book cost greater than zero.');
      return;
    }
    if (!confirm(
      `Issue ${this.newBookLeafCount} active cheque numbers to ${accountNumber}?\n` +
      `₹${this.newBookCost.toFixed(2)} will be debited. Stored CIBIL score: ${this.storedCibilScore}.`
    )) {
      return;
    }

    this.isIssuingBook = true;
    this.chequeService.issueChequeBookForAdmin(
      accountNumber,
      this.newBookLeafCount,
      this.newBookCost,
      this.adminName
    ).subscribe({
      next: response => {
        this.isIssuingBook = false;
        this.showIssueBookForm = false;
        this.alertService.success('Cheque Book Issued', response?.message || 'Cheque numbers are now active.');
        this.fetchChequeBooksForClosure();
      },
      error: err => {
        this.isIssuingBook = false;
        this.alertService.error(
          'Issuance Failed',
          err?.error?.error || 'Unable to issue a cheque book for this account.'
        );
      }
    });
  }

  lookupChequeByNumber(): void {
    const chequeNumber = this.chequeLookupNumber.trim();
    if (!chequeNumber) {
      this.alertService.error('Missing Info', 'Enter a cheque number to fetch its data');
      return;
    }

    this.isLookingUpCheque = true;
    this.chequeLookupResult = null;
    this.chequeService.lookupChequeByNumber(chequeNumber).subscribe({
      next: result => {
        this.chequeLookupResult = result;
        this.isLookingUpCheque = false;
      },
      error: err => {
        this.isLookingUpCheque = false;
        this.alertService.error(
          'Cheque Not Found',
          err?.error?.error || 'No cheque was found for that number'
        );
      }
    });
  }

  closeChequeBooksForAccount(): void {
    const accountNumber = this.closureAccountNumber.trim();
    if (!accountNumber) {
      this.alertService.error('Missing Info', 'Enter an account number first');
      return;
    }
    if (!this.hasActiveBooks) {
      this.alertService.error('No Active Books', 'No active cheque books are available to close.');
      return;
    }
    if (!confirm(
      `Close all active cheque books for ${accountNumber}?\n\nOnce closed, all cheque numbers from this account will become invalid for future use.`
    )) {
      return;
    }

    this.isClosingBooks = true;
    this.chequeService.closeChequeBooksByAccountNumber(
      accountNumber,
      'Admin closed cheque book from dashboard',
      this.adminName
    ).subscribe({
      next: response => {
        this.isClosingBooks = false;
        this.alertService.success(
          'Cheque Book Closed',
          response?.message || 'Cheque book closure saved successfully'
        );
        this.fetchChequeBooksForClosure();
      },
      error: err => {
        this.isClosingBooks = false;
        this.alertService.error(
          'Closure Failed',
          err?.error?.error || 'Unable to close cheque books for this account'
        );
      }
    });
  }

  private loadChequeClosureHistory(accountNumber: string): void {
    this.chequeService.getChequeBookClosureHistory(accountNumber).subscribe({
      next: history => this.closureHistory = history || [],
      error: err => {
        this.closureHistory = [];
        this.alertService.error(
          'Closure History Unavailable',
          err?.error?.error || 'Could not load cheque book closure history.'
        );
      }
    });
  }
}
