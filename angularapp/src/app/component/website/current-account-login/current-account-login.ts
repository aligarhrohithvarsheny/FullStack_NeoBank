import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CurrentAccountService } from '../../../service/current-account.service';

@Component({
  selector: 'app-current-account-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './current-account-login.html',
  styleUrls: ['./current-account-login.css']
})
export class CurrentAccountLogin {
  activeTab = 'login';
  isBrowser = false;

  // Login
  loginAccountNumber = '';
  loginPassword = '';
  isLoggingIn = false;
  loginError = '';

  // Signup
  signupCustomerId = '';
  signupPassword = '';
  signupConfirmPassword = '';
  isSigningUp = false;
  signupError = '';
  signupSuccess = '';

  // Verify
  verifyResult: any = null;
  isVerifying = false;

  // Team Login
  teamEmail = '';
  teamPassword = '';
  isTeamLogging = false;
  teamLoginError = '';

  constructor(
    private currentAccountService: CurrentAccountService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  login(): void {
    if (!this.loginAccountNumber || !this.loginPassword) {
      this.loginError = 'Please enter account number and password';
      return;
    }
    this.isLoggingIn = true;
    this.loginError = '';
    this.currentAccountService.login(this.loginAccountNumber, this.loginPassword).subscribe({
      next: (res) => {
        this.isLoggingIn = false;
        if (res.success) {
          if (this.isBrowser) {
            sessionStorage.setItem('currentAccount', JSON.stringify(res.account));
          }
          this.router.navigate(['/website/current-account-dashboard']);
        } else {
          this.loginError = res.message || 'Login failed';
        }
      },
      error: (err) => {
        this.isLoggingIn = false;
        this.loginError = err.error?.message || 'Login failed. Please try again.';
      }
    });
  }

  verifyCustomer(): void {
    if (!this.signupCustomerId) {
      this.signupError = 'Please enter your Customer ID';
      return;
    }
    this.isVerifying = true;
    this.signupError = '';
    this.currentAccountService.verifyCustomer(this.signupCustomerId).subscribe({
      next: (res) => {
        this.isVerifying = false;
        this.verifyResult = res;
        if (!res.exists) {
          this.signupError = 'No current account found with this Customer ID';
        } else if (res.passwordSet) {
          this.signupError = 'Password already set. Please login with account number: ' + res.accountNumber;
        }
      },
      error: () => {
        this.isVerifying = false;
        this.signupError = 'Verification failed. Please try again.';
      }
    });
  }

  signup(): void {
    if (!this.signupPassword || !this.signupConfirmPassword) {
      this.signupError = 'Please enter and confirm password';
      return;
    }
    if (this.signupPassword.length < 6) {
      this.signupError = 'Password must be at least 6 characters';
      return;
    }
    if (this.signupPassword !== this.signupConfirmPassword) {
      this.signupError = 'Passwords do not match';
      return;
    }
    this.isSigningUp = true;
    this.signupError = '';
    this.currentAccountService.signup(this.signupCustomerId, this.signupPassword).subscribe({
      next: (res) => {
        this.isSigningUp = false;
        if (res.success) {
          this.signupSuccess = 'Password set successfully! Your account number is ' + res.accountNumber + '. Please login now.';
          this.verifyResult = null;
          this.signupCustomerId = '';
          this.signupPassword = '';
          this.signupConfirmPassword = '';
        } else {
          this.signupError = res.message || 'Signup failed';
        }
      },
      error: (err) => {
        this.isSigningUp = false;
        this.signupError = err.error?.message || 'Signup failed';
      }
    });
  }

  teamLogin(): void {
    if (!this.teamEmail || !this.teamPassword) {
      this.teamLoginError = 'Please enter email and password';
      return;
    }
    this.isTeamLogging = true;
    this.teamLoginError = '';
    this.currentAccountService.businessUserLogin(this.teamEmail, this.teamPassword).subscribe({
      next: (res) => {
        this.isTeamLogging = false;
        if (res.success) {
          if (this.isBrowser) {
            sessionStorage.setItem('currentAccount', JSON.stringify(res.account));
            sessionStorage.setItem('businessUserRole', res.userRole);
            sessionStorage.setItem('businessUserName', res.userName);
            sessionStorage.setItem('businessUserId', res.userId);
          }
          this.router.navigate(['/website/current-account-dashboard']);
        } else {
          this.teamLoginError = res.message || 'Login failed';
        }
      },
      error: (err) => {
        this.isTeamLogging = false;
        this.teamLoginError = err.error?.message || 'Login failed. Please try again.';
      }
    });
  }
}
