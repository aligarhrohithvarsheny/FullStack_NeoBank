import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { WebAuthnService } from '../../../service/webauthn.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [CommonModule, FormsModule],
  standalone: true
})


export class Login {
  email: string = '';
  password: string = '';
  selectedRole: string = 'ADMIN'; // Default to ADMIN
  errorMessage: string = '';
  isLoading: boolean = false;
  isFingerprintSupported: boolean = false;
  isRegisteringFingerprint: boolean = false;
  isAuthenticatingFingerprint: boolean = false;
  hasFingerprintRegistered: boolean = false;
  checkingCredentials: boolean = false;

  constructor(
    private router: Router, 
    private alertService: AlertService,
    private http: HttpClient,
    private webauthnService: WebAuthnService
  ) {
    // Check if WebAuthn is supported
    this.isFingerprintSupported = this.webauthnService.isSupported();
    
    // If not supported, show error immediately
    if (!this.isFingerprintSupported) {
      this.errorMessage = 'Fingerprint authentication is required but not supported in your browser. Please use a modern browser with biometric support.';
    }
  }

  /**
   * Check if fingerprint credentials exist for the entered email
   */
  checkFingerprintCredentials() {
    if (!this.email) {
      this.hasFingerprintRegistered = false;
      return;
    }
    
    // Only check for ADMIN role
    if (this.selectedRole !== 'ADMIN') {
      this.hasFingerprintRegistered = false;
      return;
    }

    this.checkingCredentials = true;
    this.webauthnService.getCredentials(this.email).subscribe({
      next: (response: any) => {
        this.checkingCredentials = false;
        if (response && response.success && response.credentials && response.credentials.length > 0) {
          this.hasFingerprintRegistered = true;
          this.errorMessage = '';
        } else {
          this.hasFingerprintRegistered = false;
          if (this.email) {
            this.errorMessage = 'No fingerprint registered for this email. Please register your fingerprint first.';
          }
        }
      },
      error: (err: any) => {
        this.checkingCredentials = false;
        // If credentials don't exist, that's okay - user needs to register
        this.hasFingerprintRegistered = false;
        if (this.email) {
          this.errorMessage = 'No fingerprint registered for this email. Please register your fingerprint first.';
        }
      }
    });
  }

  onSubmit() {
    // For ADMIN role, fingerprint is mandatory - block password login
    if (this.selectedRole === 'ADMIN') {
      this.errorMessage = 'Fingerprint authentication is required for Admin login. Please use "Login with Fingerprint" button.';
      return;
    }

    // Prevent multiple submissions
    if (this.isLoading) {
      return;
    }

    if (!this.email || !this.password) {
      this.errorMessage = 'Please enter email and password';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Try manager login (only for MANAGER role)
    this.http.post(`${environment.apiUrl}/admins/login`, {
      email: this.email,
      password: this.password,
      role: this.selectedRole
    }).subscribe({
      next: (response: any) => {
        this.isLoading = false;
        if (response && response.success) {
          const role = response.role || response.admin?.role;
          
          // Check if selected role matches the user's actual role
          if (role === this.selectedRole) {
            this.errorMessage = '';
            // Store admin/manager data in session storage
            if (response.admin) {
              sessionStorage.setItem('admin', JSON.stringify(response.admin));
              sessionStorage.setItem('userRole', role);
            }
            
            // Check if profile is complete (for ADMIN role only)
            const profileComplete = response.profileComplete !== undefined ? response.profileComplete : 
                                   (response.admin?.profileComplete !== undefined ? response.admin.profileComplete : true);
            
            if (role === 'MANAGER') {
              this.alertService.loginSuccess('Manager');
              this.router.navigate(['/manager/dashboard']);
            } else if (role === 'ADMIN') {
              // Check if admin profile is complete
              if (!profileComplete) {
                // Redirect to profile completion page
                this.alertService.success('Welcome', 'Please complete your profile to continue');
                this.router.navigate(['/admin/complete-profile']);
              } else {
                this.alertService.loginSuccess('Admin');
                this.router.navigate(['/admin/dashboard']);
              }
            } else {
              this.alertService.loginSuccess('Admin');
              this.router.navigate(['/admin/dashboard']);
            }
          } else {
            this.errorMessage = `Invalid ${this.selectedRole.toLowerCase()} credentials ❌`;
          }
        } else {
          this.errorMessage = `Invalid ${this.selectedRole.toLowerCase()} credentials ❌`;
        }
      },
      error: (err: any) => {
        // If admin login fails with 401, it means invalid credentials
        // Only try user authenticate if it's not a 401 (might be network error, etc.)
        if (err.status === 401) {
          // Invalid admin credentials - don't try user login
          this.isLoading = false;
          this.errorMessage = 'Invalid admin credentials ❌';
          return;
        }

        // For other errors, check if it's a user trying to login
        this.http.post(`${environment.apiUrl}/users/authenticate`, {
          email: this.email,
          password: this.password
        }).subscribe({
          next: (userResponse: any) => {
            this.isLoading = false;
            if (userResponse && userResponse.success && userResponse.role === 'ADMIN') {
              // User credentials matched admin - route to admin dashboard
              this.errorMessage = '';
              this.alertService.loginSuccess('Admin');
              this.router.navigate(['/admin/dashboard']);
            } else if (userResponse && userResponse.success && userResponse.requiresOtp) {
              // User login requires OTP - redirect to user login page
              this.errorMessage = 'Please use the user login page for regular users';
              this.router.navigate(['/website/user']);
            } else {
              this.errorMessage = 'Invalid email or password ❌';
            }
          },
          error: (userErr: any) => {
            this.isLoading = false;
            if (userErr.status === 400 && userErr.error && userErr.error.message) {
              this.errorMessage = userErr.error.message;
            } else {
              this.errorMessage = 'Invalid email or password ❌';
            }
          }
        });
      }
    });
  }

  /**
   * Login with fingerprint using WebAuthn (MANDATORY for ADMIN)
   */
  loginWithFingerprint() {
    if (!this.email) {
      this.errorMessage = 'Please enter your email first';
      return;
    }

    if (!this.isFingerprintSupported) {
      this.errorMessage = 'Fingerprint authentication is required but not supported in your browser. Please use a modern browser with biometric support.';
      return;
    }

    // For ADMIN, fingerprint is mandatory
    if (this.selectedRole === 'ADMIN' && !this.hasFingerprintRegistered) {
      this.errorMessage = 'No fingerprint registered for this email. Please register your fingerprint first using the "Register Fingerprint" button.';
      return;
    }

    this.isAuthenticatingFingerprint = true;
    this.errorMessage = '';

    this.webauthnService.authenticate(this.email).subscribe({
      next: (response: any) => {
        this.isAuthenticatingFingerprint = false;
        if (response && response.success) {
          const role = response.role || response.admin?.role;
          
          // Store admin/manager data in session storage
          if (response.admin) {
            sessionStorage.setItem('admin', JSON.stringify(response.admin));
            sessionStorage.setItem('userRole', role);
          }
          
          // Check if profile is complete (for ADMIN role only)
          const profileComplete = response.profileComplete !== undefined ? response.profileComplete : 
                                 (response.admin?.profileComplete !== undefined ? response.admin.profileComplete : true);
          
          if (role === 'MANAGER') {
            this.alertService.loginSuccess('Manager');
            this.router.navigate(['/manager/dashboard']);
          } else if (role === 'ADMIN') {
            // Check if admin profile is complete
            if (!profileComplete) {
              // Redirect to profile completion page
              this.alertService.success('Welcome', 'Please complete your profile to continue');
              this.router.navigate(['/admin/complete-profile']);
            } else {
              this.alertService.success('Login Successful', 'Fingerprint authentication successful!');
              this.router.navigate(['/admin/dashboard']);
            }
          } else {
            this.alertService.success('Login Successful', 'Fingerprint authentication successful!');
            this.router.navigate(['/admin/dashboard']);
          }
        } else {
          this.errorMessage = 'Fingerprint authentication failed';
        }
      },
      error: (err: any) => {
        this.isAuthenticatingFingerprint = false;
        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          if (this.selectedRole === 'ADMIN') {
            this.errorMessage = 'Fingerprint authentication failed. Please try again.';
          } else {
            this.errorMessage = 'Fingerprint authentication failed. Please try again or use password login.';
          }
        }
      }
    });
  }

  /**
   * Register fingerprint credential (first time setup)
   */
  registerFingerprint() {
    if (!this.email) {
      this.errorMessage = 'Please enter your email first';
      return;
    }

    if (!this.isFingerprintSupported) {
      this.errorMessage = 'Fingerprint authentication is not supported in your browser';
      return;
    }

    this.isRegisteringFingerprint = true;
    this.errorMessage = '';

    // First verify password to ensure it's the correct admin
    if (!this.password) {
      this.errorMessage = 'Please enter your password first to verify your identity';
      this.isRegisteringFingerprint = false;
      return;
    }

    // Verify password first
    this.http.post(`${environment.apiUrl}/admins/login`, {
      email: this.email,
      password: this.password,
      role: this.selectedRole
    }).subscribe({
      next: (verifyResponse: any) => {
        if (verifyResponse && verifyResponse.success) {
          // Password verified, now register fingerprint
          this.webauthnService.registerCredential(this.email, verifyResponse.admin?.name || this.email).subscribe({
            next: (response: any) => {
              this.isRegisteringFingerprint = false;
              if (response && response.success) {
                this.alertService.success('Success', 'Fingerprint registered successfully! You can now use fingerprint to login.');
                // Auto login after registration
                this.loginWithFingerprint();
              } else {
                this.errorMessage = 'Failed to register fingerprint';
              }
            },
            error: (err: any) => {
              this.isRegisteringFingerprint = false;
              if (err.error && err.error.message) {
                this.errorMessage = err.error.message;
              } else {
                this.errorMessage = 'Failed to register fingerprint. Please try again.';
              }
            }
          });
        } else {
          this.isRegisteringFingerprint = false;
          this.errorMessage = 'Invalid password. Please verify your credentials.';
        }
      },
      error: (err: any) => {
        this.isRegisteringFingerprint = false;
        this.errorMessage = 'Invalid password. Please verify your credentials before registering fingerprint.';
      }
    });
  }
}
