import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-hod-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './hod-login.html',
  styleUrls: ['./hod-login.css']
})
export class HodLogin {
  email = '';
  password = '';
  createEmail = '';
  createPassword = '';
  confirmPassword = '';
  errorMessage = '';
  isLoading = false;
  isCreating = false;
  showCreateAccount = false;
  showCreateAccountButton = true;

  constructor(
    private router: Router,
    private http: HttpClient,
    private alertService: AlertService
  ) {
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/hod-availability`).subscribe({
      next: response => {
        if (response?.available === false) this.showCreateAccountButton = false;
      },
      error: () => this.showCreateAccountButton = true
    });
  }

  login(): void {
    if (this.isLoading) return;
    if (!this.email.trim() || !this.password) {
      this.errorMessage = 'Please enter Gmail and password';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.authenticateHod({
      email: this.email.trim(),
      password: this.password,
      role: 'HOD'
    }, true);
  }

  private authenticateHod(credentials: { email: string; password: string; role?: string }, allowLegacyRetry: boolean): void {
    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/login`, credentials).subscribe({
      next: response => {
        this.isLoading = false;
        const responseRole = response?.role || response?.admin?.role;
        if (!response?.success || responseRole !== 'HOD') {
          this.errorMessage = 'Invalid HOD credentials';
          return;
        }
        sessionStorage.setItem('admin', JSON.stringify(response.admin));
        sessionStorage.setItem('userRole', 'HOD');
        sessionStorage.setItem('adminLoginTime', new Date().toISOString());
        this.alertService.loginSuccess('HOD');
        this.router.navigate(['/hod/dashboard']);
      },
      error: err => {
        if (allowLegacyRetry && (err.status === 400 || err.status === 401 || err.status === 404 || err.status === 405)) {
          this.authenticateHod({ email: credentials.email, password: credentials.password }, false);
          return;
        }
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Invalid HOD credentials';
      }
    });
  }

  createAccount(): void {
    if (this.isCreating) return;
    if (!this.createEmail.trim() || !this.createPassword || !this.confirmPassword) {
      this.errorMessage = 'Please enter Gmail and both password fields';
      return;
    }
    if (this.createPassword !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      return;
    }

    this.isCreating = true;
    this.errorMessage = '';
    const accountPayload = {
      name: 'NeoBank Head of Department',
      email: this.createEmail.trim(),
      password: this.createPassword,
      role: 'HOD'
    };

    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/hod-create`, accountPayload).subscribe({
      next: () => {
        this.isCreating = false;
        this.showCreateAccountButton = false;
        this.showCreateAccount = false;
        this.email = this.createEmail.trim();
        this.password = '';
        this.alertService.loginSuccess('HOD account created. Please sign in.');
      },
      error: err => {
        if (err.status === 404 || err.status === 405) {
          this.createWithLegacyAdminEndpoint(accountPayload);
          return;
        }
        this.isCreating = false;
        this.errorMessage = err.error?.message || 'Unable to create HOD account';
        if (err.status === 409) {
          this.showCreateAccountButton = false;
          this.showCreateAccount = false;
        }
      }
    });
  }

  private createWithLegacyAdminEndpoint(accountPayload: { name: string; email: string; password: string; role: string }): void {
    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/create`, accountPayload).subscribe({
      next: () => {
        this.isCreating = false;
        this.showCreateAccountButton = false;
        this.showCreateAccount = false;
        this.email = accountPayload.email;
        this.password = '';
        this.alertService.loginSuccess('HOD account created. Please sign in.');
      },
      error: err => {
        this.isCreating = false;
        this.errorMessage = err.error?.message || 'Unable to create HOD account';
        if (err.status === 409) {
          this.showCreateAccountButton = false;
          this.showCreateAccount = false;
        }
      }
    });
  }
}