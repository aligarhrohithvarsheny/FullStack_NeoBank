import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-password-setup',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './password-setup.component.html',
  styleUrls: ['./password-setup.component.css']
})
export class PasswordSetupComponent implements OnInit {
  
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private router = inject(Router);
  private alertService = inject(AlertService);

  passwordSetupForm!: FormGroup;
  email: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  isSubmitting: boolean = false;
  passwordStrength: number = 0;
  showPassword: boolean = false;
  showConfirmPassword: boolean = false;
  
  // Password strength indicators
  hasUpperCase: boolean = false;
  hasLowerCase: boolean = false;
  hasNumber: boolean = false;
  hasMinLength: boolean = false;

  ngOnInit() {
    // Initialize form
    this.passwordSetupForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator.bind(this) });
    // Check if email is passed from login component
    const currentUser = sessionStorage.getItem('setupEmail');
    if (currentUser) {
      this.email = currentUser;
      this.passwordSetupForm.patchValue({
        email: this.email
      });
      sessionStorage.removeItem('setupEmail');
    } else {
      // If no email in session, redirect back to login
      this.alertService.userError('Session Error', 'Please login first to proceed with password setup.');
      this.router.navigate(['/user']);
    }
  }

  // Custom validator for password match
  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const password = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    
    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  // Check password strength
  onPasswordChange(password: string) {
    this.newPassword = password;
    this.updatePasswordStrength(password);
  }

  updatePasswordStrength(password: string) {
    // Reset indicators
    this.hasUpperCase = /[A-Z]/.test(password);
    this.hasLowerCase = /[a-z]/.test(password);
    this.hasNumber = /[0-9]/.test(password);
    this.hasMinLength = password.length >= 8;
    
    // Calculate strength (0-100)
    this.passwordStrength = 0;
    if (this.hasUpperCase) this.passwordStrength += 25;
    if (this.hasLowerCase) this.passwordStrength += 25;
    if (this.hasNumber) this.passwordStrength += 25;
    if (this.hasMinLength) this.passwordStrength += 25;
  }

  getPasswordStrengthColor(): string {
    if (this.passwordStrength <= 25) return '#ff4444';
    if (this.passwordStrength <= 50) return '#ff9800';
    if (this.passwordStrength <= 75) return '#ffc107';
    return '#4caf50';
  }

  getPasswordStrengthText(): string {
    if (this.passwordStrength <= 25) return 'Weak';
    if (this.passwordStrength <= 50) return 'Fair';
    if (this.passwordStrength <= 75) return 'Good';
    return 'Strong';
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  setupPassword() {
    if (!this.passwordSetupForm.valid) {
      this.alertService.userError('Validation Error', 'Please fill all required fields correctly.');
      return;
    }

    // Additional validation for password strength
    if (!this.hasUpperCase || !this.hasLowerCase || !this.hasNumber || !this.hasMinLength) {
      this.alertService.userError('Weak Password', 'Password must contain uppercase, lowercase, number, and be at least 8 characters.');
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.alertService.userError('Password Mismatch', 'Passwords do not match. Please try again.');
      return;
    }

    this.isSubmitting = true;

    const setupRequest = {
      email: this.email,
      newPassword: this.newPassword,
      confirmPassword: this.confirmPassword
    };

    this.http.post(`${environment.apiBaseUrl}/api/users/set-password`, setupRequest).subscribe({
      next: (response: any) => {
        this.isSubmitting = false;
        
        if (response.success) {
          this.alertService.userSuccess('Success', response.message || 'Password set successfully! You can now login.');
          
          // Clear any setup-related session data
          sessionStorage.removeItem('setupEmail');
          
          // Redirect to login page after 2 seconds
          setTimeout(() => {
            this.router.navigate(['/user']);
          }, 2000);
        } else {
          this.alertService.userError('Error', response.message || 'Failed to set password. Please try again.');
        }
      },
      error: (err: any) => {
        this.isSubmitting = false;
        
        if (err.error && err.error.message) {
          this.alertService.userError('Error', err.error.message);
        } else if (err.status === 400) {
          this.alertService.userError('Validation Error', 'Please check all fields and try again.');
        } else if (err.status === 409) {
          this.alertService.userError('Already Set', 'Password has already been set for this account.');
        } else {
          this.alertService.userError('Error', 'An error occurred while setting password. Please try again.');
        }
        
        console.error('Password setup error:', err);
      }
    });
  }

  backToLogin() {
    sessionStorage.removeItem('setupEmail');
    this.router.navigate(['/user']);
  }

}
