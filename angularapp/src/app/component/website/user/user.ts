import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user.html',
  styleUrls: ['./user.css']
})
export class User {
  // For login
  loginUserId: string = '';
  loginPassword: string = '';
  directScreen: string = 'dashboard';
  captchaChecked: boolean = false;

  // For create account
  signupName: string = '';
  signupEmail: string = '';
  signupMobile: string = '';
  signupPassword: string = '';
  confirmPassword: string = '';
  termsAccepted: boolean = false;

  // For account unlock
  showUnlockButton: boolean = false;
  showUnlockModal: boolean = false;
  unlockEmail: string = '';
  unlockAadharFirst4: string = '';
  unlockDob: string = '';
  unlockingAccount: boolean = false;

  // For forgot password
  showForgotPasswordModalFlag: boolean = false;
  showResetSuccessScreen: boolean = false;
  resetEmail: string = '';
  resetAadharFirst4: string = '';
  newPassword: string = '';
  confirmNewPassword: string = '';
  resettingPassword: boolean = false;
  
  // Messages
  errorMessage: string = '';
  successMessage: string = '';

  currentPage: string = 'login'; // 'login' | 'signup'

  constructor(private router: Router, private http: HttpClient) {}

  login() {
    if (!this.loginUserId || !this.loginPassword) {
      alert('Please enter User ID and Password ❌');
      return;
    }
    
    if (!this.captchaChecked) {
      alert('Please verify that you are not a robot ❌');
      return;
    }

    // Simple validation - in real app, this would check against database
    if (this.loginUserId === 'admin' && this.loginPassword === 'admin123') {
      alert(`Login successful ✅ Welcome Admin`);
      this.router.navigate(['/admin/dashboard']);
    } else if (this.loginUserId && this.loginPassword) {
      // For regular users, fetch their real account data from database
      this.fetchUserDataAndLogin();
    } else {
      alert('Invalid User ID or Password ❌');
    }
  }

  fetchUserDataAndLogin() {
    // Authenticate user with MySQL database
    this.http.post('http://localhost:8080/api/users/authenticate', {
      email: this.loginUserId,
      password: this.loginPassword
    }).subscribe({
      next: (authResponse: any) => {
        console.log('Authentication response:', authResponse);
        
        if (authResponse.success && authResponse.user) {
          const userData = authResponse.user;
          
          if (userData.status === 'APPROVED') {
            // Store user session data (no localStorage - pure MySQL)
            const sessionData = {
              id: userData.id,
              name: userData.account?.name || userData.username,
              email: userData.email,
              accountNumber: userData.accountNumber,
              status: userData.status,
              loginTime: new Date().toISOString()
            };
            
            sessionStorage.setItem('currentUser', JSON.stringify(sessionData));
            
            console.log('User authenticated successfully:', userData);
            console.log('Session data stored:', sessionData);
            console.log('SessionStorage check:', sessionStorage.getItem('currentUser'));
            
            alert(`Login successful ✅ Welcome ${userData.account?.name || userData.username}`);
            this.router.navigate(['/website/userdashboard']);
          } else {
            alert('Account not approved yet. Please wait for admin approval ❌');
          }
        } else {
          // Check if account is locked
          if (authResponse.accountLocked) {
            this.showUnlockButton = true;
            this.errorMessage = authResponse.message || 'Account is locked due to multiple failed login attempts.';
            this.unlockEmail = this.loginUserId; // Pre-fill email
          } else if (authResponse.failedAttempts !== undefined) {
            this.errorMessage = authResponse.message || `Invalid credentials. ${3 - authResponse.failedAttempts} attempts remaining.`;
          } else {
            this.errorMessage = 'Invalid credentials. Please check your email and password.';
          }
          this.successMessage = '';
        }
      },
      error: (err: any) => {
        console.error('Authentication error:', err);
        if (err.error && err.error.accountLocked) {
          this.showUnlockButton = true;
          this.errorMessage = err.error.message || 'Account is locked due to multiple failed login attempts.';
          this.unlockEmail = this.loginUserId; // Pre-fill email
        } else if (err.error && err.error.failedAttempts !== undefined) {
          this.errorMessage = err.error.message || 'Invalid credentials.';
        } else {
          this.errorMessage = 'Login failed. Please check your credentials.';
        }
        this.successMessage = '';
      }
    });
  }

  createAccount() {
    if (!this.signupName || !this.signupEmail || !this.signupMobile || !this.signupPassword || !this.confirmPassword) {
      alert('Please fill in all fields ❌');
      return;
    }

    if (this.signupPassword !== this.confirmPassword) {
      alert('Passwords do not match ❌');
      return;
    }

    if (!this.termsAccepted) {
      alert('Please accept Terms & Conditions ❌');
      return;
    }

    // Create user account in MySQL database
    const newUser = {
      username: this.signupName,
      email: this.signupEmail,
      password: this.signupPassword,
      phoneNumber: this.signupMobile,
      status: 'PENDING', // Will be approved by admin
      pan: '',
      aadhar: '',
      address: '',
      occupation: '',
      income: 0
    };

    console.log('Creating new user account:', newUser);
    
    this.http.post('http://localhost:8080/api/users/create', newUser).subscribe({
      next: (response: any) => {
        console.log('User account created successfully:', response);
        
        if (response.success) {
          alert(`Account created successfully ✅ Welcome ${this.signupName}. Please wait for admin approval.`);
          
          // Reset form
          this.signupName = '';
          this.signupEmail = '';
          this.signupMobile = '';
          this.signupPassword = '';
          this.confirmPassword = '';
          this.termsAccepted = false;
          
          // Switch back to login page
          this.currentPage = 'login';
        } else {
          alert(`Account creation failed: ${response.message} ❌`);
        }
      },
      error: (err: any) => {
        console.error('Error creating user account:', err);
        
        // Try to parse error response
        if (err.error && err.error.message) {
          alert(`Account creation failed: ${err.error.message} ❌`);
        } else if (err.status === 400) {
          alert('Account creation failed. Email, PAN, or Aadhar may already be in use. Please check your details and try again. ❌');
        } else {
          alert('Account creation failed. Please try again. ❌');
        }
      }
    });
  }

  switchToLogin() {
    this.currentPage = 'login';
  }

  switchToSignup() {
    this.currentPage = 'signup';
  }

  // Account unlock methods
  closeUnlockModal() {
    this.showUnlockModal = false;
    this.unlockEmail = '';
    this.unlockAadharFirst4 = '';
    this.unlockDob = '';
  }

  unlockAccount() {
    if (!this.unlockEmail || !this.unlockAadharFirst4 || !this.unlockDob) {
      this.errorMessage = 'Please fill in all unlock fields.';
      return;
    }

    if (this.unlockAadharFirst4.length !== 4 || !/^\d{4}$/.test(this.unlockAadharFirst4)) {
      this.errorMessage = 'Please enter exactly 4 digits for Aadhar number.';
      return;
    }

    this.unlockingAccount = true;
    this.errorMessage = '';
    this.successMessage = '';

    const unlockData = {
      email: this.unlockEmail,
      aadharFirst4: this.unlockAadharFirst4,
      dob: this.unlockDob
    };

    console.log('Attempting to unlock account:', unlockData);

    this.http.post('http://localhost:8080/api/users/unlock-account', unlockData).subscribe({
      next: (response: any) => {
        console.log('Account unlock response:', response);
        this.unlockingAccount = false;
        
        if (response.success) {
          this.successMessage = 'Account unlocked successfully! You can now login.';
          this.errorMessage = '';
          this.showUnlockButton = false;
          this.showUnlockModal = false;
          
          // Reset unlock form
          this.unlockEmail = '';
          this.unlockAadharFirst4 = '';
          this.unlockDob = '';
          
          // Clear login form messages after successful unlock
          setTimeout(() => {
            this.successMessage = '';
          }, 5000);
        } else {
          this.errorMessage = response.message || 'Account unlock failed. Please check your details.';
        }
      },
      error: (err: any) => {
        console.error('Account unlock error:', err);
        this.unlockingAccount = false;
        this.errorMessage = err.error?.message || 'Account unlock failed. Please try again.';
      }
    });
  }

  // Forgot password methods
  showForgotPasswordModal() {
    this.showForgotPasswordModalFlag = true;
    this.showResetSuccessScreen = false;
    this.resetEmail = '';
    this.resetAadharFirst4 = '';
    this.newPassword = '';
    this.confirmNewPassword = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeForgotPasswordModal() {
    this.showForgotPasswordModalFlag = false;
    this.showResetSuccessScreen = false;
    this.resetEmail = '';
    this.resetAadharFirst4 = '';
    this.newPassword = '';
    this.confirmNewPassword = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  showSuccessScreen() {
    this.showResetSuccessScreen = true;
    this.resettingPassword = false;
  }

  closeSuccessScreen() {
    this.showResetSuccessScreen = false;
    this.closeForgotPasswordModal();
  }

  resetPassword() {
    // Validation
    if (!this.resetEmail || !this.resetAadharFirst4 || !this.newPassword || !this.confirmNewPassword) {
      this.errorMessage = 'Please fill in all fields.';
      return;
    }

    if (this.resetAadharFirst4.length !== 4 || !/^\d{4}$/.test(this.resetAadharFirst4)) {
      this.errorMessage = 'Please enter exactly 4 digits for Aadhar number.';
      return;
    }

    if (this.newPassword !== this.confirmNewPassword) {
      this.errorMessage = 'New password and confirm password do not match.';
      return;
    }

    if (this.newPassword.length < 6) {
      this.errorMessage = 'Password must be at least 6 characters long.';
      return;
    }

    this.resettingPassword = true;
    this.errorMessage = '';
    this.successMessage = '';

    const resetData = {
      email: this.resetEmail,
      aadharFirst4: this.resetAadharFirst4,
      newPassword: this.newPassword
    };

    console.log('Attempting to reset password:', resetData);

    this.http.post('http://localhost:8080/api/users/reset-password', resetData).subscribe({
      next: (response: any) => {
        console.log('Password reset response:', response);
        this.resettingPassword = false;
        
        if (response.success) {
          this.successMessage = 'Password reset successfully! You can now login with your new password.';
          this.errorMessage = '';
          
          // Show success screen instead of closing modal immediately
          this.showSuccessScreen();
        } else {
          this.errorMessage = response.message || 'Password reset failed.';
        }
      },
      error: (err: any) => {
        console.error('Password reset error:', err);
        this.resettingPassword = false;
        this.errorMessage = err.error?.message || 'Password reset failed. Please verify your email and Aadhar details.';
      }
    });
  }
}
