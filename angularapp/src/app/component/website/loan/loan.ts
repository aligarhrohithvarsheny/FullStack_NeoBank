import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface LoanRequest {
  id: number;
  type: string;
  amount: number;
  tenure: number;
  interestRate: number;
  status: 'Pending' | 'Approved' | 'Rejected';
  userName: string;
  userEmail: string;
  accountNumber: string;
  currentBalance: number;
  loanAccountNumber: string;
  applicationDate: string;
  approvalDate?: string;
}

@Component({
  selector: 'app-loan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan.html',
  styleUrls: ['./loan.css']
})
export class Loan implements OnInit {
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}

  loans: LoanRequest[] = [];
  userAccountNumber: string = '';
  userName: string = '';
  userEmail: string = '';
  currentBalance: number = 0;
  loading: boolean = false;

  loanForm = {
    type: '',
    amount: 0,
    tenure: 0,
    interestRate: 0
  };

  // EMI Calculator properties
  emiCalculator = {
    principal: 0,
    rate: 0,
    time: 0,
    emi: 0,
    totalAmount: 0,
    totalInterest: 0,
    interestPercentage: 0,
    principalPercentage: 0
  };

  ngOnInit() {
    this.loadUserData();
  }

  loadUserData() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.userAccountNumber = user.accountNumber;
      this.userName = user.name;
      this.userEmail = user.email;
      
      // Load user loans after getting user data
      this.loadUserLoans();
    } else {
      // Fallback to localStorage for existing users
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        const profile = JSON.parse(savedProfile);
        this.userAccountNumber = profile.accountNumber;
        this.userName = profile.name;
        this.userEmail = profile.email;
        this.loadUserLoans();
      }
    }
  }

  loadUserLoans() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    
    // Load user's loans from MySQL database
    this.http.get(`http://localhost:8080/api/loans/account/${this.userAccountNumber}`).subscribe({
      next: (loans: any) => {
        console.log('User loans loaded from MySQL:', loans);
        this.loans = loans.map((loan: any) => ({
          id: loan.id,
          type: loan.type,
          amount: loan.amount,
          tenure: loan.tenure,
          interestRate: loan.interestRate,
          status: loan.status,
          userName: loan.userName,
          userEmail: loan.userEmail,
          accountNumber: loan.accountNumber,
          currentBalance: loan.currentBalance,
          loanAccountNumber: loan.loanAccountNumber,
          applicationDate: loan.applicationDate,
          approvalDate: loan.approvalDate
        }));
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading user loans from MySQL:', err);
        // Fallback to localStorage
        const savedLoans = localStorage.getItem('user_loans');
        if (savedLoans) {
          this.loans = JSON.parse(savedLoans).filter((loan: LoanRequest) => 
            loan.accountNumber === this.userAccountNumber
          );
        }
        this.loading = false;
      }
    });
  }

  submitLoan() {
    // Validate form
    if (!this.loanForm.type || this.loanForm.amount <= 0 || this.loanForm.tenure <= 0 || this.loanForm.interestRate <= 0) {
      alert('Please fill all fields correctly!');
      return;
    }

    // Generate loan account number with better uniqueness
    const loanAccountNumber = 'LOAN' + Date.now() + Math.floor(Math.random() * 1000);

    const newLoan: LoanRequest = {
      id: Date.now(), // Temporary ID for frontend
      type: this.loanForm.type,
      amount: this.loanForm.amount,
      tenure: this.loanForm.tenure,
      interestRate: this.loanForm.interestRate,
      status: 'Pending',
      userName: this.userName,
      userEmail: this.userEmail,
      accountNumber: this.userAccountNumber,
      currentBalance: this.currentBalance,
      loanAccountNumber: loanAccountNumber,
      applicationDate: new Date().toISOString()
    };

    // Create backend loan object without ID (backend will generate it)
    const backendLoan = {
      type: newLoan.type,
      amount: newLoan.amount,
      tenure: newLoan.tenure,
      interestRate: newLoan.interestRate,
      status: newLoan.status,
      userName: newLoan.userName,
      userEmail: newLoan.userEmail,
      accountNumber: newLoan.accountNumber,
      currentBalance: newLoan.currentBalance,
      loanAccountNumber: newLoan.loanAccountNumber,
      applicationDate: newLoan.applicationDate
    };

    // Submit to MySQL database
    this.http.post('http://localhost:8080/api/loans', backendLoan).subscribe({
      next: (response: any) => {
        console.log('Loan request created successfully in MySQL:', response);
        
        // Add to local loans array
        this.loans.unshift(newLoan);
        
        // Also save to localStorage as backup
        this.saveLoansToStorage();
        
        alert('Loan request submitted successfully! Admin will review your application.');
        this.resetForm();
      },
      error: (err: any) => {
        console.error('Error creating loan request:', err);
        alert('Failed to submit loan request. Please try again.');
        
        // Fallback to localStorage
        this.loans.unshift(newLoan);
        this.saveLoansToStorage();
      }
    });
  }

  saveLoansToStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get all loans from localStorage
    const allLoans = JSON.parse(localStorage.getItem('user_loans') || '[]');
    
    // Update or add the current user's loans
    const otherLoans = allLoans.filter((loan: LoanRequest) => 
      loan.accountNumber !== this.userAccountNumber
    );
    
    // Combine other users' loans with current user's loans
    const updatedLoans = [...otherLoans, ...this.loans];
    
    // Save back to localStorage
    localStorage.setItem('user_loans', JSON.stringify(updatedLoans));
  }

  resetForm() {
    this.loanForm = { type: '', amount: 0, tenure: 0, interestRate: 0 };
  }

  goBack() {
    this.router.navigate(['website/userdashboard']);
  }

  // EMI Calculation Method
  calculateEMI() {
    const principal = this.emiCalculator.principal;
    const rate = this.emiCalculator.rate;
    const time = this.emiCalculator.time;

    // Validate inputs
    if (principal <= 0 || rate <= 0 || time <= 0) {
      this.resetEMICalculator();
      return;
    }

    // Convert annual rate to monthly rate
    const monthlyRate = rate / (12 * 100);
    
    // Calculate EMI using the formula: EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
    const emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, time)) / 
                (Math.pow(1 + monthlyRate, time) - 1);
    
    // Calculate total amount and interest
    const totalAmount = emi * time;
    const totalInterest = totalAmount - principal;
    
    // Calculate percentages
    const interestPercentage = (totalInterest / totalAmount) * 100;
    const principalPercentage = (principal / totalAmount) * 100;

    // Update calculator results
    this.emiCalculator.emi = emi;
    this.emiCalculator.totalAmount = totalAmount;
    this.emiCalculator.totalInterest = totalInterest;
    this.emiCalculator.interestPercentage = interestPercentage;
    this.emiCalculator.principalPercentage = principalPercentage;

    console.log('EMI Calculation:', {
      principal,
      rate,
      time,
      emi,
      totalAmount,
      totalInterest,
      interestPercentage,
      principalPercentage
    });
  }

  // Reset EMI Calculator
  resetEMICalculator() {
    this.emiCalculator = {
      principal: 0,
      rate: 0,
      time: 0,
      emi: 0,
      totalAmount: 0,
      totalInterest: 0,
      interestPercentage: 0,
      principalPercentage: 0
    };
  }

  // Auto-fill EMI calculator from loan form
  fillEMICalculator() {
    this.emiCalculator.principal = this.loanForm.amount;
    this.emiCalculator.rate = this.loanForm.interestRate;
    this.emiCalculator.time = this.loanForm.tenure;
    this.calculateEMI();
  }
}
