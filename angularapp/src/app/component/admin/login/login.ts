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
            // Backend has already authenticated this admin; always persist session
            // so guarded routes like /admin/complete-profile can open.
            sessionStorage.setItem('admin', JSON.stringify(serverAdmin));
            sessionStorage.setItem('userRole', role);
            try {
              sessionStorage.setItem('adminLoginTime', new Date().toISOString());
            } catch (e) {
              console.error('Failed to store adminLoginTime in sessionStorage', e);
            }
          }

          if (role === 'MANAGER') {
            this.alertService.loginSuccess('Manager');
            this.router.navigate(['/manager/dashboard']);
            return;
          }

          const profileComplete =
            response.profileComplete !== undefined
              ? !!response.profileComplete
              : response.admin?.profileComplete !== undefined
                ? !!response.admin.profileComplete
                : true;
          const profileRequired =
            response.profileRequired !== undefined
              ? !!response.profileRequired
              : !profileComplete;

          if (profileRequired) {
            this.alertService.info(
              'Profile Incomplete',
              'Profile is incomplete. You can continue and update it later from dashboard.'
            );
          }

          // Admin login should go directly to dashboard without dialog/toast.
          this.router.navigate(['/admin/dashboard']);
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
