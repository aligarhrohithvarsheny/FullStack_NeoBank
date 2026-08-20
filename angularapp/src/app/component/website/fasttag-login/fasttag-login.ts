import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-fasttag-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fasttag-login.html',
  styleUrls: ['./fasttag-login.css'],
  encapsulation: ViewEncapsulation.None
})
export class FasttagLogin implements OnInit, OnDestroy {
  gmailId: string = '';
  password: string = '';
  newPassword: string = '';
  confirmPassword: string = '';

  step: 'login' | 'set-password' = 'login';
  showPassword: boolean = false;
  accountExists: boolean | null = null;
  checkingAccount: boolean = false;

  loading: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  isBrowser: boolean = false;

  constructor(
    private router: Router,
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (this.isBrowser) {
      const fastagUser = sessionStorage.getItem('fastagUser');
      if (fastagUser) {
        this.router.navigate(['/website/fasttag-dashboard']);
      }
    }
  }

  ngOnDestroy() {}

  isValidGmail(): boolean {
    const gmailRegex = /^[a-zA-Z0-9._%+-]+@gmail\.com$/i;
    return gmailRegex.test(this.gmailId.trim());
  }

  checkAccountStatus() {
    if (!this.isValidGmail()) {
      this.accountExists = null;
      return;
    }
    this.checkingAccount = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/fastag/account-status/${encodeURIComponent(this.gmailId.trim())}`).subscribe({
      next: (res) => {
        this.checkingAccount = false;
        this.accountExists = res.exists === true;
      },
      error: () => {
        this.checkingAccount = false;
        this.accountExists = null;
      }
    });
  }

  login() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.gmailId.trim()) {
      this.errorMessage = 'Please enter your Gmail ID.';
      return;
    }
    if (!this.isValidGmail()) {
      this.errorMessage = 'Please enter a valid Gmail address (e.g., user@gmail.com).';
      return;
    }
    if (!this.password) {
      this.errorMessage = 'Please enter your password.';
      return;
    }

    this.loading = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/login`, {
      gmailId: this.gmailId.trim(),
      password: this.password
    }).subscribe({
      next: (res) => this.handleAuthResponse(res),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Login failed. Please try again.';
      }
    });
  }

  setPassword() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.gmailId.trim() || !this.isValidGmail()) {
      this.errorMessage = 'Please enter a valid Gmail address.';
      return;
    }
    if (!this.newPassword || !this.confirmPassword) {
      this.errorMessage = 'Please enter and confirm your password.';
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    this.loading = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/fastag/set-password`, {
      gmailId: this.gmailId.trim(),
      newPassword: this.newPassword,
      confirmPassword: this.confirmPassword
    }).subscribe({
      next: (res) => this.handleAuthResponse(res),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to set password. Please try again.';
      }
    });
  }

  private handleAuthResponse(res: any) {
    this.loading = false;

    if (res.success && res.user) {
      this.successMessage = 'Login successful! Redirecting...';
      if (this.isBrowser) {
        sessionStorage.setItem('fastagUser', JSON.stringify(res.user));
        if (res.fasttags) {
          sessionStorage.setItem('fastagDetails', JSON.stringify(res.fasttags));
        }
      }
      setTimeout(() => this.router.navigate(['/website/fasttag-dashboard']), 800);
      return;
    }

    if (res.requiresPasswordSetup || res.accountNotFound) {
      this.step = 'set-password';
      this.successMessage = res.message || 'Please set your password to continue.';
      this.errorMessage = '';
      return;
    }

    this.errorMessage = res.message || 'Login failed.';
  }

  goToSetPassword() {
    this.step = 'set-password';
    this.checkAccountStatus();
    this.errorMessage = '';
    this.successMessage = '';
  }

  backToLogin() {
    this.step = 'login';
    this.newPassword = '';
    this.confirmPassword = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  goToLanding() {
    this.router.navigate(['/website/landing']);
  }
}
