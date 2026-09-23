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
  accountAvailable = true;

  constructor(
    private router: Router,
    private http: HttpClient,
    private alertService: AlertService
  ) {
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/hod-availability`).subscribe({
      next: response => this.accountAvailable = response?.available === true,
      error: () => this.accountAvailable = false
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
    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/login`, {
      email: this.email.trim(),
      password: this.password,
      role: 'HOD'
    }).subscribe({
      next: response => {
        this.isLoading = false;
        if (!response?.success || response.role !== 'HOD') {
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
    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/hod-create`, {
      email: this.createEmail.trim(),
      password: this.createPassword
    }).subscribe({
      next: () => {
        this.isCreating = false;
        this.accountAvailable = false;
        this.showCreateAccount = false;
        this.email = this.createEmail.trim();
        this.password = '';
        this.alertService.loginSuccess('HOD account created. Please sign in.');
      },
      error: err => {
        this.isCreating = false;
        this.errorMessage = err.error?.message || 'Unable to create HOD account';
        if (err.status === 409) {
          this.accountAvailable = false;
          this.showCreateAccount = false;
        }
      }
    });
  }
}