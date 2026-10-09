import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-insurance-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './insurance-login.html',
  styleUrls: ['./insurance-login.css']
})
export class InsuranceLogin implements OnInit {
  insuranceNumber = '';
  email = '';
  password = '';
  confirmPassword = '';
  mode: 'login' | 'create-password' = 'login';
  isLoggingIn = false;
  loginError = '';
  private readonly isBrowser: boolean;

  constructor(
    private http: HttpClient,
    private router: Router,
    private alertService: AlertService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (!this.isBrowser || !sessionStorage.getItem('insuranceAuthToken')) return;
    if (sessionStorage.getItem('insuranceGuest')) {
      this.router.navigate(['/website/insurance-account']);
    } else if (sessionStorage.getItem('insuranceUser')) {
      this.router.navigate(['/website/insurance']);
    }
  }

  login(): void {
    const insuranceNumber = this.insuranceNumber.trim();
    const email = this.email.trim().toLowerCase();
    if (!insuranceNumber || !email || !this.password) {
      this.loginError = 'Enter your insurance number, registered email, and password.';
      return;
    }

    this.isLoggingIn = true;
    this.loginError = '';
    this.http.post<any>(`${environment.apiBaseUrl}/api/insurance/authenticate`, {
      insuranceNumber,
      email,
      password: this.password
    }).subscribe({
      next: (response) => {
        this.isLoggingIn = false;
        if (!response?.success || !response?.user?.id || !response?.token) {
          this.loginError = response?.message || 'Unable to sign in to insurance.';
          return;
        }

        if (this.isBrowser) {
          sessionStorage.removeItem('insuranceGuest');
          sessionStorage.removeItem('insuranceUser');
          sessionStorage.setItem(response.guest ? 'insuranceGuest' : 'insuranceUser', JSON.stringify(response.user));
          sessionStorage.setItem('insuranceAuthToken', response.token);
        }
        this.password = '';
        this.router.navigate([response.guest ? '/website/insurance-account' : '/website/insurance']);
      },
      error: (error) => {
        this.isLoggingIn = false;
        this.loginError = error.error?.message || 'Invalid insurance number, email, or password.';
        this.alertService.userError('Insurance Login Failed', this.loginError);
      }
    });
  }

  createPassword(): void {
    const insuranceNumber = this.insuranceNumber.trim();
    const email = this.email.trim().toLowerCase();
    if (!insuranceNumber || !email || !this.password || !this.confirmPassword) {
      this.loginError = 'Enter your insurance number, registered email, and new password.';
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.loginError = 'Passwords do not match.';
      return;
    }

    this.isLoggingIn = true;
    this.loginError = '';
    this.http.post<any>(`${environment.apiBaseUrl}/api/insurance/create-password`, {
      insuranceNumber,
      email,
      password: this.password
    }).subscribe({
      next: (response) => {
        this.isLoggingIn = false;
        if (response?.success) {
          this.mode = 'login';
          this.password = '';
          this.confirmPassword = '';
          this.alertService.userSuccess('Password Created', 'Your insurance password is ready. Sign in to continue.');
        } else {
          this.loginError = response?.message || 'Unable to create insurance password.';
        }
      },
      error: (error) => {
        this.isLoggingIn = false;
        this.loginError = error.error?.message || 'Unable to create insurance password.';
      }
    });
  }
}
