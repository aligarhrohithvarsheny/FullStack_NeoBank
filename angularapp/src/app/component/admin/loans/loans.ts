import { CurrencyPipe, CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface LoanRequest {
  id: number;
  userName: string;
  userEmail: string;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: 'Pending' | 'Approved' | 'Rejected';
  accountNumber: string;
  currentBalance: number;
  loanAccountNumber: string;
  applicationDate: string;
  approvalDate?: string;
}

@Component({
  selector: 'app-loans',
  standalone: true,
  templateUrl: './loans.html',
  styleUrls: ['./loans.css'],
  imports: [CurrencyPipe, CommonModule]
})
export class Loans implements OnInit {
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}

  loanRequests: LoanRequest[] = [];
  loading = false;

  ngOnInit() {
    console.log('Loans component initialized!');
    this.loadAllLoans();
  }

  loadAllLoans() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    console.log('Loading loan applications...');
    
    // Load loans from MySQL database
    this.http.get('${environment.apiUrl}/loans').subscribe({
      next: (loans: any) => {
        console.log('Loans loaded from MySQL:', loans);
        this.loanRequests = loans.map((loan: any) => ({
          id: loan.id,
          userName: loan.userName,
          userEmail: loan.userEmail,
          type: loan.type,
          amount: loan.amount,
          tenure: loan.tenure,
          interestRate: loan.interestRate,
          status: loan.status,
          accountNumber: loan.accountNumber,
          currentBalance: loan.currentBalance,
          loanAccountNumber: loan.loanAccountNumber,
          applicationDate: loan.applicationDate,
          approvalDate: loan.approvalDate
        }));
        
        // Also save to localStorage as backup
        this.saveLoansToStorage();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading loans from database:', err);
        // Fallback to localStorage
        const savedLoans = localStorage.getItem('user_loans');
        console.log('Fallback: Saved loans from localStorage:', savedLoans);
        
        if (savedLoans) {
          try {
            this.loanRequests = JSON.parse(savedLoans);
            console.log('Parsed loan requests:', this.loanRequests);
          } catch (error) {
            console.error('Error parsing saved loans:', error);
            this.loanRequests = [];
          }
        } else {
          console.log('No saved loans found in database');
          // No loans found in database - show empty list
          this.loanRequests = [];
        }
    
    this.loading = false;
    console.log('Final loan requests count:', this.loanRequests.length);
      }
    });
  }

  saveLoansToStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('user_loans', JSON.stringify(this.loanRequests));
  }

  // Navigate back to admin dashboard
  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }

  // Approve loan
  approve(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    console.log('Approving loan:', loan);
    
    const updatedLoan = {
      ...loan,
      status: 'Approved',
      approvalDate: new Date().toISOString()
    };
    
    // Update loan in MySQL database using enhanced approval endpoint
    this.http.put(`${environment.apiUrl}/loans/${loan.id}/approve?status=Approved`, {}).subscribe({
      next: (response: any) => {
        console.log('Loan approved in MySQL:', response);
        
        if (response.success) {
          loan.status = 'Approved';
          loan.approvalDate = new Date().toISOString();
          
          // Update in localStorage as backup
          this.saveLoansToStorage();
          
          // Show enhanced notification with transaction details
          this.showLoanApprovalNotification(loan, response.transaction, response.loan.amount);
          
          console.log('✅ Loan approval completed successfully');
        } else {
          alert('Loan approval failed: ' + response.message);
        }
      },
      error: (err: any) => {
        console.error('Error approving loan:', err);
        alert('Failed to approve loan. Please try again.');
      }
    });
  }

  // Note: createLoanCreditTransaction and updateUserBalance methods removed
  // Backend now handles all loan approval, balance updates, and transaction creation

  // Reject loan
  reject(loan: LoanRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const updatedLoan = {
      ...loan,
      status: 'Rejected',
      approvalDate: new Date().toISOString()
    };
    
    // Update loan in MySQL database
    this.http.put(`${environment.apiUrl}/loans/${loan.id}/approve?status=Rejected`, {}).subscribe({
      next: (response: any) => {
        console.log('Loan rejected in MySQL:', response);
        
        loan.status = 'Rejected';
        loan.approvalDate = new Date().toISOString();
        
        // Update in localStorage as backup
        this.saveLoansToStorage();
        
        alert(`Loan for ${loan.userName} rejected.`);
      },
      error: (err: any) => {
        console.error('Error rejecting loan:', err);
        alert('Failed to reject loan. Please try again.');
      }
    });
  }

  // TrackBy function for ngFor performance
  trackByLoanId(index: number, loan: LoanRequest): number {
    return loan.id;
  }

  // Show loan approval notification with detailed information
  showLoanApprovalNotification(loan: LoanRequest, transaction: any, newBalance: number) {
    const notificationMessage = `
🎉 LOAN APPROVED SUCCESSFULLY! 🎉

👤 User: ${loan.userName}
📧 Email: ${loan.userEmail}
🏦 Account: ${loan.accountNumber}
💰 Loan Amount: ₹${loan.amount.toLocaleString()}
📋 Loan Type: ${loan.type}
📅 Tenure: ${loan.tenure} months
📊 Interest Rate: ${loan.interestRate}%

💳 TRANSACTION DETAILS:
🆔 Transaction ID: ${transaction.id}
💵 Amount Credited: ₹${loan.amount.toLocaleString()}
💳 New Balance: ₹${newBalance.toLocaleString()}
📝 Description: ${transaction.description}
⏰ Processed: ${new Date().toLocaleString()}

✅ The loan amount has been credited to the user's account in real-time!
✅ Transaction has been recorded with complete details!
✅ User can now see the updated balance and transaction history!
    `;
    
    alert(notificationMessage);
    
    // Also log to console for debugging
    console.log('=== LOAN APPROVAL NOTIFICATION ===');
    console.log('User:', loan.userName);
    console.log('Account:', loan.accountNumber);
    console.log('Loan Amount:', loan.amount);
    console.log('Transaction ID:', transaction.id);
    console.log('New Balance:', newBalance);
    console.log('==================================');
  }
}
