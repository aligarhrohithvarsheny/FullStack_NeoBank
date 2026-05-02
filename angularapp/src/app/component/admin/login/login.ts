import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [CommonModule, FormsModule],
  standalone: true
})
export class Login {
  email = '';
  password = '';
  selectedRole = 'ADMIN';
  errorMessage = '';
  isLoading = false;

  constructor(
    private router: Router,
    private alertService: AlertService,
    private http: HttpClient
  ) {}

  /**
   * Same model for Admin and Manager: POST /api/admins/login with email, password, role.
   */
  onSubmit(): void {
    if (this.isLoading) {
      return;
    }
    if (!this.email?.trim() || !this.password) {
      this.errorMessage = 'Please enter email and password';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.http
      .post(`${environment.apiBaseUrl}/api/admins/login`, {
        email: this.email.trim(),
        password: this.password,
        role: this.selectedRole
      })
      .subscribe({
        next: (response: any) => {
          this.isLoading = false;
          if (!response?.success) {
            this.errorMessage = `Invalid ${this.selectedRole.toLowerCase()} credentials ❌`;
            return;
          }

          const role = response.role || response.admin?.role;
          if (role !== this.selectedRole) {
            this.errorMessage = `Invalid ${this.selectedRole.toLowerCase()} credentials ❌`;
            return;
          }

          if (response.admin) {
            const serverAdmin = response.admin;
            const emailOk =
              serverAdmin.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(serverAdmin.email);
            if (emailOk) {
              sessionStorage.setItem('admin', JSON.stringify(serverAdmin));
              sessionStorage.setItem('userRole', role);
              try {
                sessionStorage.setItem('adminLoginTime', new Date().toISOString());
              } catch (e) {
                console.error('Failed to store adminLoginTime in sessionStorage', e);
              }
            } else {
              console.error(
                'Refusing to store invalid admin.email from server:',
                serverAdmin?.email
              );
            }
          }

          const profileComplete =
            response.profileComplete !== undefined
              ? response.profileComplete
              : response.admin?.profileComplete !== undefined
                ? response.admin.profileComplete
                : true;

          if (role === 'MANAGER') {
            this.alertService.loginSuccess('Manager');
            this.router.navigate(['/manager/dashboard']);
            return;
          }

          if (!profileComplete) {
            this.alertService.success('Welcome', 'Please complete your profile to continue');
            this.router.navigate(['/admin/complete-profile']);
          } else {
            this.alertService.loginSuccess('Admin');
            this.router.navigate(['/admin/dashboard']);
          }
        },
        error: (err: any) => {
          this.isLoading = false;
          if (err.status === 401 || err.status === 404) {
            this.errorMessage = `Invalid ${this.selectedRole.toLowerCase()} credentials ❌`;
          } else if (err.status === 0) {
            this.errorMessage = 'Unable to connect to server. Please check your connection.';
          } else {
            this.errorMessage = 'Login failed. Please try again.';
          }
        }
      });
  }
}
