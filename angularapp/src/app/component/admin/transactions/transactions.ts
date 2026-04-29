import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { environment } from '../../../../environment/environment';

interface Transaction {
  id: string;
  user: string;
  userName?: string;
  userPAN?: string;
  type: 'Deposit' | 'Withdraw' | 'Transfer' | 'Loan Credit' | 'Credit' | 'Debit';
  amount: number;
  balance?: number;
  date: string;
  status: string;
  description?: string;
  merchant?: string;
  accountNumber?: string;
  category?: string;
  // OTP verification and ACH timestamp (optional, populated from backend when available)
  otpVerified?: boolean;
  achTime?: string | null;
}

interface UserProfile {
  id?: string;
  name?: string;
  username?: string;
  email?: string;
  accountNumber?: string;
  pan?: string;
  account?: {
    name?: string;
    pan?: string;
    accountNumber?: string;
  };
}

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions.html',
  styleUrls: ['./transactions.css']
})
export class Transactions implements OnInit {

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient, private sanitizer: DomSanitizer) {}

  transactions: Transaction[] = [];
  allTransactions: Transaction[] = [];
  users: UserProfile[] = [];
  isLoadingTransactions: boolean = false;
  
  // Signature Verification
  showSignatureModal: boolean = false;
  signatureDocUrl: SafeResourceUrl | null = null;
  signatureLoading: boolean = false;
  signatureInfo: any = null;
  signatureError: string = '';
  signatureAccountNumber: string = '';
  
  // Filters
  selectedUserForTransactions: string | null = null;
  transactionsSearchQuery: string = '';
  transactionsPANFilter: string = '';
  transactionsDateFilter: string = '';
  transactionsTypeFilter: string = 'ALL';
  transactionsCategoryFilter: string = 'ALL';

  ngOnInit() {
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      this.loadUsers();
      this.loadAllTransactions();
      
      // Check if there's a newly created account to filter by
      const newAccountNumber = sessionStorage.getItem('newAccountCreated');
      if (newAccountNumber) {
        sessionStorage.removeItem('newAccountCreated');
        setTimeout(() => {
          this.filterByUser(newAccountNumber);
        }, 1000);
      }
    }
  }

  // Load all users
  loadUsers() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.http.get(`${environment.apiBaseUrl}/api/users/admin/all`).subscribe({
      next: (users: any) => {
        this.users = users || [];
        console.log('Users loaded:', this.users.length);
      },
      error: (err: any) => {
        console.error('Error loading users:', err);
        this.users = [];
      }
    });
  }

  loadAllTransactions() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.isLoadingTransactions = true;
    this.allTransactions = [];
    
    // Load regular transactions
    this.http.get(`${environment.apiBaseUrl}/api/transactions?page=0&size=1000`).subscribe({
      next: (response: any) => {
        let transactionsData = [];
        if (response && response.content && Array.isArray(response.content)) {
          transactionsData = response.content;
        } else if (Array.isArray(response)) {
          transactionsData = response;
        }
        
        // Map transactions and get user names
        transactionsData.forEach((transaction: any) => {
          const user = this.users.find((u: any) => u.accountNumber === transaction.accountNumber || 
            (u.account && u.account.accountNumber === transaction.accountNumber));
          const userName = user ? (user.name || user.username || 'Unknown User') : 
            (transaction.userName || transaction.user?.username || 'Unknown User');
          const userPAN = user ? (user.pan || (user.account && user.account.pan) || undefined) : undefined;
          
          this.allTransactions.push({
            id: transaction.id,
            user: userName,
            userName: userName,
            userPAN: userPAN,
            accountNumber: transaction.accountNumber,
            type: transaction.type,
            amount: transaction.amount,
            balance: transaction.balance,
            date: transaction.date || transaction.createdAt,
            description: transaction.description || transaction.merchant,
            merchant: transaction.merchant,
            status: transaction.status || 'Completed',
            category: this.determineTransactionCategory(transaction),
            // OTP and ACH info (if provided by backend)
            otpVerified: transaction.otpVerified || transaction.otp_verified || false,
            achTime: transaction.achTime || transaction.achDate || transaction.achProcessedAt || transaction.achTimestamp || transaction.ach_date || null
          });
        });
        
        // Load FD, Investment, and Subsidy transactions
        this.loadFDTransactions();
        this.loadInvestmentTransactions();
        this.loadSubsidyTransactions();
      },
      error: (err: any) => {
        console.error('Error loading transactions:', err);
        this.loadFDTransactions();
        this.loadInvestmentTransactions();
        this.loadSubsidyTransactions();
      }
    });
  }

  // Load FD related transactions
  loadFDTransactions() {
    this.http.get(`${environment.apiBaseUrl}/api/fixed-deposits/all`).subscribe({
      next: (fds: any) => {
        if (Array.isArray(fds)) {
          fds.forEach((fd: any) => {
            const user = this.users.find((u: any) => u.accountNumber === fd.accountNumber);
            const userName = user ? (user.name || user.username || fd.userName) : (fd.userName || 'Unknown User');
            const userPAN = user ? (user.pan || (user.account && user.account.pan) || undefined) : undefined;
            
            if (fd.transactionId) {
              this.allTransactions.push({
                id: 'FD-' + fd.id,
                user: userName,
                userName: userName,
                userPAN: userPAN,
                accountNumber: fd.accountNumber,
                type: 'Debit',
                amount: fd.principalAmount,
                balance: fd.balanceBefore ? fd.balanceBefore - fd.principalAmount : 0,
                date: fd.applicationDate || new Date().toISOString(),
                description: `FD Created - ${fd.fdAccountNumber} | Principal: ₹${fd.principalAmount}`,
                merchant: 'Fixed Deposit',
                status: fd.status === 'APPROVED' ? 'Completed' : 'Pending',
                category: 'FD'
              });
            }
            if (fd.isMatured && fd.maturityAmount) {
              this.allTransactions.push({
                id: 'FD-MAT-' + fd.id,
                user: userName,
                userName: userName,
                userPAN: userPAN,
                accountNumber: fd.accountNumber,
                type: 'Credit',
                amount: fd.maturityAmount,
                balance: 0,
                date: fd.maturityDate || new Date().toISOString(),
                description: `FD Matured - ${fd.fdAccountNumber} | Maturity: ₹${fd.maturityAmount}`,
                merchant: 'Fixed Deposit Maturity',
                status: 'Completed',
                category: 'FD'
              });
            }
          });
        }
        this.finalizeTransactionLoading();
      },
      error: (err: any) => {
        console.error('Error loading FD transactions:', err);
        this.finalizeTransactionLoading();
      }
    });
  }

  // Load Investment/Mutual Fund transactions
  loadInvestmentTransactions() {
    this.http.get(`${environment.apiBaseUrl}/api/investments/all`).subscribe({
      next: (investments: any) => {
        if (Array.isArray(investments)) {
          investments.forEach((inv: any) => {
            const user = this.users.find((u: any) => u.accountNumber === inv.accountNumber);
            const userName = user ? (user.name || user.username || inv.userName) : (inv.userName || 'Unknown User');
            const userPAN = user ? (user.pan || (user.account && user.account.pan) || undefined) : undefined;
            
            if (inv.transactionId) {
              this.allTransactions.push({
                id: 'INV-' + inv.id,
                user: userName,
                userName: userName,
                userPAN: userPAN,
                accountNumber: inv.accountNumber,
                type: 'Debit',
                amount: inv.investmentAmount,
                balance: inv.balanceBefore ? inv.balanceBefore - inv.investmentAmount : 0,
                date: inv.investmentDate || inv.applicationDate || new Date().toISOString(),
                description: `${inv.investmentType} - ${inv.fundName || 'N/A'} | Amount: ₹${inv.investmentAmount}`,
                merchant: inv.investmentType === 'Mutual Fund' ? 'Mutual Fund' : 'Investment',
                status: inv.status === 'APPROVED' ? 'Completed' : 'Pending',
                category: inv.investmentType === 'Mutual Fund' ? 'MUTUAL_FUND' : 'INVESTMENT'
              });
            }
          });
        }
        this.finalizeTransactionLoading();
      },
      error: (err: any) => {
        console.error('Error loading investment transactions:', err);
        this.finalizeTransactionLoading();
      }
    });
  }

  // Load Subsidy transactions
  loadSubsidyTransactions() {
    this.http.get(`${environment.apiBaseUrl}/api/education-loan-subsidy-claims`).subscribe({
      next: (subsidies: any) => {
        if (Array.isArray(subsidies)) {
          subsidies.forEach((sub: any) => {
            if (sub.status === 'APPROVED' && sub.subsidyAmount) {
              const user = this.users.find((u: any) => u.accountNumber === sub.accountNumber);
              const userName = user ? (user.name || user.username || sub.userName) : (sub.userName || 'Unknown User');
              const userPAN = user ? (user.pan || (user.account && user.account.pan) || undefined) : undefined;
              
              this.allTransactions.push({
                id: 'SUB-' + sub.id,
                user: userName,
                userName: userName,
                userPAN: userPAN,
                accountNumber: sub.accountNumber,
                type: 'Credit',
                amount: sub.subsidyAmount,
                balance: 0,
                date: sub.approvalDate || sub.applicationDate || new Date().toISOString(),
                description: `Subsidy Approved - ${sub.loanType || 'Education Loan'} | Amount: ₹${sub.subsidyAmount}`,
                merchant: 'Subsidy Claim',
                status: 'Completed',
                category: 'SUBSIDY'
              });
            }
          });
        }
        this.finalizeTransactionLoading();
      },
      error: (err: any) => {
        console.error('Error loading subsidy transactions:', err);
        this.finalizeTransactionLoading();
      }
    });
  }

  // Determine transaction category
  determineTransactionCategory(transaction: any): string {
    const desc = (transaction.description || transaction.merchant || '').toLowerCase();
    if (desc.includes('loan') || desc.includes('loan credit') || desc.includes('emi')) {
      return 'LOAN';
    } else if (desc.includes('subsidy')) {
      return 'SUBSIDY';
    } else if (desc.includes('fd') || desc.includes('fixed deposit')) {
      return 'FD';
    } else if (desc.includes('mutual fund')) {
      return 'MUTUAL_FUND';
    } else if (desc.includes('investment') || desc.includes('sip')) {
      return 'INVESTMENT';
    }
    return 'REGULAR';
  }

  // Finalize transaction loading
  finalizeTransactionLoading() {
    // Remove duplicates based on ID
    const uniqueTransactions = Array.from(
      new Map(this.allTransactions.map(t => [t.id, t])).values()
    );
    
    // Sort by date (newest first)
    uniqueTransactions.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    
    this.allTransactions = uniqueTransactions;
    this.transactions = this.getFilteredTransactions();
    this.isLoadingTransactions = false;
    console.log('All transactions loaded:', this.allTransactions.length);
  }

  // Get user account number (handles different user object structures)
  getUserAccountNumber(user: any): string {
    return user.accountNumber || user.account?.accountNumber || '';
  }

  // Filter by user
  filterByUser(accountNumber: string | null | undefined) {
    if (!accountNumber) return;
    this.selectedUserForTransactions = accountNumber;
    this.transactionsSearchQuery = '';
    this.transactionsPANFilter = '';
    this.transactionsDateFilter = '';
    this.transactions = this.getFilteredTransactions();
  }

  // Clear all filters
  clearTransactionFilters() {
    this.selectedUserForTransactions = null;
    this.transactionsSearchQuery = '';
    this.transactionsPANFilter = '';
    this.transactionsDateFilter = '';
    this.transactionsTypeFilter = 'ALL';
    this.transactionsCategoryFilter = 'ALL';
    this.transactions = this.getFilteredTransactions();
  }

  // Handle search change
  onTransactionSearchChange() {
    this.transactions = this.getFilteredTransactions();
  }

  // Get filtered transactions
  getFilteredTransactions(): Transaction[] {
    let filtered = this.allTransactions;
    
    // Filter by selected user
    if (this.selectedUserForTransactions) {
      filtered = filtered.filter(t => t.accountNumber === this.selectedUserForTransactions);
    }
    
    // Filter by PAN number
    if (this.transactionsPANFilter && this.transactionsPANFilter.trim() !== '') {
      const panQuery = this.transactionsPANFilter.trim().toUpperCase();
      filtered = filtered.filter(t => {
        if (t.userPAN) {
          return t.userPAN.toUpperCase().includes(panQuery);
        }
        const user = this.users.find((u: any) => u.accountNumber === t.accountNumber);
        return user && ((user.pan && user.pan.toUpperCase().includes(panQuery)) || 
          (user.account && user.account.pan && user.account.pan.toUpperCase().includes(panQuery)));
      });
    }
    
    // Filter by date
    if (this.transactionsDateFilter && this.transactionsDateFilter.trim() !== '') {
      const filterDate = new Date(this.transactionsDateFilter);
      filterDate.setHours(0, 0, 0, 0);
      const nextDay = new Date(filterDate);
      nextDay.setDate(nextDay.getDate() + 1);
      
      filtered = filtered.filter(t => {
        if (!t.date) return false;
        const txnDate = new Date(t.date);
        txnDate.setHours(0, 0, 0, 0);
        return txnDate >= filterDate && txnDate < nextDay;
      });
    }
    
    // Filter by type
    if (this.transactionsTypeFilter !== 'ALL') {
      filtered = filtered.filter(t => t.type === this.transactionsTypeFilter);
    }
    
    // Filter by category
    if (this.transactionsCategoryFilter !== 'ALL') {
      filtered = filtered.filter(t => {
        if (!t.category) return false;
        return t.category === this.transactionsCategoryFilter;
      });
    }
    
    // Filter by search query
    if (this.transactionsSearchQuery && this.transactionsSearchQuery.trim() !== '') {
      const query = this.transactionsSearchQuery.toLowerCase();
      filtered = filtered.filter(t => {
        if (t.accountNumber && t.accountNumber.toLowerCase().includes(query)) return true;
        if (t.userName && t.userName.toLowerCase().includes(query)) return true;
        if (t.description && t.description.toLowerCase().includes(query)) return true;
        if (t.merchant && t.merchant.toLowerCase().includes(query)) return true;
        if (t.description && t.description.toLowerCase().includes('loan') && query.includes('loan')) return true;
        if (t.description && (t.description.toLowerCase().includes('fd') || t.description.toLowerCase().includes('fixed deposit')) && 
            (query.includes('fd') || query.includes('fixed'))) return true;
        if (t.description && t.description.toLowerCase().includes('subsidy') && query.includes('subsidy')) return true;
        if (t.description && (t.description.toLowerCase().includes('investment') || t.description.toLowerCase().includes('mutual fund')) && 
            (query.includes('investment') || query.includes('mutual'))) return true;
        return false;
      });
    }
    
    return filtered;
  }

  // ==================== Signature Verification ====================

  isDebitTransaction(txn: Transaction): boolean {
    return txn.type === 'Withdraw' || txn.type === 'Debit' || txn.type === 'Transfer';
  }

  viewSignature(accountNumber: string) {
    if (!accountNumber) return;
    this.signatureAccountNumber = accountNumber;
    this.showSignatureModal = true;
    this.signatureLoading = true;
    this.signatureError = '';
    this.signatureDocUrl = null;
    this.signatureInfo = null;

    // First get info
    this.http.get(`${environment.apiBaseUrl}/api/admin-account-applications/signed-document-info/${accountNumber}`).subscribe({
      next: (info: any) => {
        if (info.found) {
          this.signatureInfo = info;
          // Load the document
          this.http.get(`${environment.apiBaseUrl}/api/admin-account-applications/view-signed-document/${accountNumber}`, {
            responseType: 'blob'
          }).subscribe({
            next: (blob: Blob) => {
              const url = URL.createObjectURL(blob);
              this.signatureDocUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
              this.signatureLoading = false;
            },
            error: () => {
              this.signatureError = 'Failed to load the signed document file.';
              this.signatureLoading = false;
            }
          });
        } else {
          this.signatureError = info.message || 'No signed document available for this account.';
          this.signatureLoading = false;
        }
      },
      error: () => {
        this.signatureError = 'Failed to fetch signature document information.';
        this.signatureLoading = false;
      }
    });
  }

  closeSignatureModal() {
    this.showSignatureModal = false;
    if (this.signatureDocUrl) {
      // Revoke the blob URL
      const url = (this.signatureDocUrl as any).changingThisBreaksApplicationSecurity || '';
      if (url.startsWith('blob:')) {
        URL.revokeObjectURL(url);
      }
    }
    this.signatureDocUrl = null;
    this.signatureInfo = null;
    this.signatureError = '';
  }

  backToDashboard() {
    // Check if accessed from manager dashboard
    const navigationSource = sessionStorage.getItem('navigationSource');
    if (navigationSource === 'MANAGER') {
      sessionStorage.removeItem('navigationSource');
      sessionStorage.removeItem('managerReturnPath');
      this.router.navigate(['/manager/dashboard']);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  saveTransactionsToStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('admin_transactions', JSON.stringify(this.allTransactions));
  }
}
