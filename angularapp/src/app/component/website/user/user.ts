import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user.html',
  styleUrls: ['./user.css']
})
export class User implements OnInit, OnDestroy {
  // For login
  loginUserId: string = '';
  loginPassword: string = '';
  showOtpInput: boolean = false;
  otpCode: string = '';
  verifyingOtp: boolean = false;
  resendingOtp: boolean = false;
  
  // For QR code login
  showQrCode: boolean = false;
  qrCodeImage: string = '';
  qrToken: string = '';
  qrStatus: string = 'PENDING';
  qrStatusInterval: any;
  isQrLoginMode: boolean = false; // True when user came from QR scan

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
  resetOtp: string = '';
  resetVerificationMethod: 'email' | 'aadhar' = 'email'; // Default to email OTP
  showResetOtpInput: boolean = false;
  sendingResetOtp: boolean = false;
  newPassword: string = '';
  confirmNewPassword: string = '';
  resettingPassword: boolean = false;
  
  // Messages
  errorMessage: string = '';
  successMessage: string = '';

  currentPage: string = 'login'; // 'login' | 'signup'

  constructor(
    private router: Router, 
    private http: HttpClient, 
    private alertService: AlertService,
    private route: ActivatedRoute
  ) {}
  
  ngOnInit() {
    // Check if user came from QR code scan
    this.route.queryParams.subscribe(params => {
      const qrToken = params['qrToken'];
      if (qrToken) {
        this.isQrLoginMode = true;
        this.qrToken = qrToken;
        this.currentPage = 'login';
        this.alertService.userSuccess('QR Code Scanned', 'Please enter your credentials to complete login.');
      }
    });
  }
  
  ngOnDestroy() {
    // Clear QR status polling interval
    if (this.qrStatusInterval) {
      clearInterval(this.qrStatusInterval);
    }
  }

  login() {
    // Check if this is a QR code login
    if (this.isQrLoginMode && this.qrToken) {
      this.completeQrLogin();
      return;
    }
    
    if (!this.loginUserId || !this.loginPassword) {
      this.alertService.userError('Validation Error', 'Please enter User ID and Password');
      return;
    }

    // Simple validation - in real app, this would check against database
    if (this.loginUserId === 'admin' && this.loginPassword === 'admin123') {
      this.alertService.userSuccess('Login Successful', 'Welcome Admin!');
      this.router.navigate(['/admin/dashboard']);
    } else if (this.loginUserId && this.loginPassword) {
      // For regular users, fetch their real account data from database
      this.fetchUserDataAndLogin();
    } else {
      this.alertService.userError('Login Failed', 'Invalid User ID or Password');
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
        
        if (authResponse.success && authResponse.requiresOtp) {
          // Password verified, now show OTP input
          this.showOtpInput = true;
          this.successMessage = authResponse.message || 'OTP has been sent to your email. Please enter the OTP to complete login.';
          this.errorMessage = '';
          this.alertService.userSuccess('Password Verified', 'OTP has been sent to your email. Please check and enter the OTP.');
        } else if (authResponse.success && authResponse.user) {
          // Direct login (if OTP is not required - backward compatibility)
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
            
            this.alertService.userSuccess('Login Successful', `Welcome ${userData.account?.name || userData.username}!`);
            this.router.navigate(['/website/userdashboard']);
          } else {
            this.alertService.userError('Account Pending', 'Account not approved yet. Please wait for admin approval');
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

  verifyOtp() {
    if (!this.otpCode || this.otpCode.length !== 6) {
      this.alertService.userError('Validation Error', 'Please enter a valid 6-digit OTP');
      return;
    }

    this.verifyingOtp = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post('http://localhost:8080/api/users/verify-otp', {
      email: this.loginUserId,
      otp: this.otpCode
    }).subscribe({
      next: (response: any) => {
        console.log('OTP verification response:', response);
        this.verifyingOtp = false;

        if (response.success && response.user) {
          const userData = response.user;

          if (userData.status === 'APPROVED') {
            // Store user session data
            const sessionData = {
              id: userData.id,
              name: userData.account?.name || userData.username,
              email: userData.email,
              accountNumber: userData.accountNumber,
              status: userData.status,
              loginTime: new Date().toISOString()
            };

            sessionStorage.setItem('currentUser', JSON.stringify(sessionData));

            console.log('OTP verified and user logged in successfully:', userData);
            this.alertService.userSuccess('Login Successful', `Welcome ${userData.account?.name || userData.username}!`);
            this.router.navigate(['/website/userdashboard']);
          } else {
            this.alertService.userError('Account Pending', 'Account not approved yet. Please wait for admin approval');
            this.showOtpInput = false;
            this.otpCode = '';
          }
        } else {
          this.errorMessage = response.message || 'Invalid OTP. Please try again.';
          this.otpCode = ''; // Clear OTP input
        }
      },
      error: (err: any) => {
        console.error('OTP verification error:', err);
        this.verifyingOtp = false;
        this.errorMessage = err.error?.message || 'OTP verification failed. Please try again.';
        this.otpCode = ''; // Clear OTP input
      }
    });
  }

  resendOtp() {
    this.resendingOtp = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post('http://localhost:8080/api/users/resend-otp', {
      email: this.loginUserId
    }).subscribe({
      next: (response: any) => {
        console.log('Resend OTP response:', response);
        this.resendingOtp = false;

        if (response.success) {
          this.successMessage = response.message || 'OTP has been resent to your email.';
          this.alertService.userSuccess('OTP Resent', 'A new OTP has been sent to your email. Please check and enter the OTP.');
          this.otpCode = ''; // Clear previous OTP
        } else {
          this.errorMessage = response.message || 'Failed to resend OTP. Please try again.';
        }
      },
      error: (err: any) => {
        console.error('Resend OTP error:', err);
        this.resendingOtp = false;
        this.errorMessage = err.error?.message || 'Failed to resend OTP. Please try again.';
      }
    });
  }

  cancelOtpVerification() {
    this.showOtpInput = false;
    this.otpCode = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  createAccount() {
    if (!this.signupName || !this.signupEmail || !this.signupMobile || !this.signupPassword || !this.confirmPassword) {
      this.alertService.userError('Validation Error', 'Please fill in all fields');
      return;
    }

    if (this.signupPassword !== this.confirmPassword) {
      this.alertService.userError('Validation Error', 'Passwords do not match');
      return;
    }

    if (!this.termsAccepted) {
      this.alertService.userError('Validation Error', 'Please accept Terms & Conditions');
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
          this.alertService.userSuccess('Account Created', `Welcome ${this.signupName}! Please wait for admin approval.`);
          
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
          this.alertService.userError('Account Creation Failed', response.message);
        }
      },
      error: (err: any) => {
        console.error('Error creating user account:', err);
        
        // Try to parse error response
        if (err.error && err.error.message) {
          this.alertService.userError('Account Creation Failed', err.error.message);
        } else if (err.status === 400) {
          this.alertService.userError('Account Creation Failed', 'Email, PAN, or Aadhar may already be in use. Please check your details and try again.');
        } else {
          this.alertService.userError('Account Creation Failed', 'Please try again.');
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
    this.resetOtp = '';
    this.resetVerificationMethod = 'email';
    this.showResetOtpInput = false;
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
    this.resetOtp = '';
    this.resetVerificationMethod = 'email';
    this.showResetOtpInput = false;
    this.newPassword = '';
    this.confirmNewPassword = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  onVerificationMethodChange() {
    // Reset OTP input when switching methods
    this.showResetOtpInput = false;
    this.resetOtp = '';
    this.resetAadharFirst4 = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  sendResetOtp() {
    if (!this.resetEmail || this.resetEmail.trim() === '') {
      this.errorMessage = 'Please enter your email address.';
      return;
    }

    // Validate email format
    const emailRegex = /^[A-Za-z0-9+_.-]+@(.+)$/;
    if (!emailRegex.test(this.resetEmail)) {
      this.errorMessage = 'Invalid email format.';
      return;
    }

    this.sendingResetOtp = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post('http://localhost:8080/api/users/send-reset-otp', {
      email: this.resetEmail
    }).subscribe({
      next: (response: any) => {
        console.log('Send reset OTP response:', response);
        this.sendingResetOtp = false;

        if (response.success) {
          this.successMessage = response.message || 'OTP has been sent to your email. Please check and enter the OTP.';
          this.showResetOtpInput = true;
          this.alertService.userSuccess('OTP Sent', 'A password reset OTP has been sent to your email. Please check and enter it.');
        } else {
          this.errorMessage = response.message || 'Failed to send OTP. Please try again.';
        }
      },
      error: (err: any) => {
        console.error('Send reset OTP error:', err);
        this.sendingResetOtp = false;
        this.errorMessage = err.error?.message || 'Failed to send OTP. Please try again.';
      }
    });
  }

  resendResetOtp() {
    this.sendResetOtp();
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
    // Validation based on verification method
    if (!this.resetEmail || !this.newPassword || !this.confirmNewPassword) {
      this.errorMessage = 'Please fill in all required fields.';
      return;
    }

    if (this.resetVerificationMethod === 'aadhar') {
      if (!this.resetAadharFirst4) {
        this.errorMessage = 'Please enter the first 4 digits of your Aadhar number.';
        return;
      }

      if (this.resetAadharFirst4.length !== 4 || !/^\d{4}$/.test(this.resetAadharFirst4)) {
        this.errorMessage = 'Please enter exactly 4 digits for Aadhar number.';
        return;
      }
    } else if (this.resetVerificationMethod === 'email') {
      if (!this.resetOtp) {
        this.errorMessage = 'Please enter the OTP sent to your email.';
        return;
      }

      if (this.resetOtp.length !== 6 || !/^\d{6}$/.test(this.resetOtp)) {
        this.errorMessage = 'Please enter a valid 6-digit OTP.';
        return;
      }
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

    // Determine which endpoint to call based on verification method
    const endpoint = this.resetVerificationMethod === 'email' 
      ? 'http://localhost:8080/api/users/reset-password-with-otp'
      : 'http://localhost:8080/api/users/reset-password';

    const resetData = this.resetVerificationMethod === 'email'
      ? {
          email: this.resetEmail,
          otp: this.resetOtp,
          newPassword: this.newPassword
        }
      : {
          email: this.resetEmail,
          aadharFirst4: this.resetAadharFirst4,
          newPassword: this.newPassword
        };

    console.log('Attempting to reset password:', resetData);

    this.http.post(endpoint, resetData).subscribe({
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
        const errorMsg = this.resetVerificationMethod === 'email'
          ? err.error?.message || 'Password reset failed. Please verify your email and OTP.'
          : err.error?.message || 'Password reset failed. Please verify your email and Aadhar details.';
        this.errorMessage = errorMsg;
      }
    });
  }
  
  // QR Code Login Methods
  
  generateQrCode() {
    this.showQrCode = true;
    this.http.post('http://localhost:8080/api/users/generate-qr-login', {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.qrCodeImage = response.qrCodeImage;
          this.qrToken = response.qrToken;
          this.qrStatus = 'PENDING';
          this.startQrStatusPolling();
          this.alertService.userSuccess('QR Code Generated', 'Scan the QR code with your mobile device to login.');
        } else {
          this.alertService.userError('QR Code Error', response.message || 'Failed to generate QR code');
          this.showQrCode = false;
        }
      },
      error: (err: any) => {
        console.error('QR code generation error:', err);
        this.alertService.userError('QR Code Error', 'Failed to generate QR code. Please try again.');
        this.showQrCode = false;
      }
    });
  }
  
  startQrStatusPolling() {
    // Clear any existing interval
    if (this.qrStatusInterval) {
      clearInterval(this.qrStatusInterval);
    }
    
    // Poll every 2 seconds
    this.qrStatusInterval = setInterval(() => {
      if (this.qrToken) {
        this.checkQrStatus();
      }
    }, 2000);
  }
  
  checkQrStatus() {
    if (!this.qrToken) return;
    
    this.http.get(`http://localhost:8080/api/users/check-qr-login-status/${this.qrToken}`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.qrStatus = response.status;
          
          if (response.status === 'LOGGED_IN' && response.user) {
            // Login successful
            clearInterval(this.qrStatusInterval);
            const userData = response.user;
            
            if (userData.status === 'APPROVED') {
              const sessionData = {
                id: userData.id,
                name: userData.account?.name || userData.username,
                email: userData.email,
                accountNumber: userData.accountNumber,
                status: userData.status,
                loginTime: new Date().toISOString()
              };
              
              sessionStorage.setItem('currentUser', JSON.stringify(sessionData));
              this.alertService.userSuccess('Login Successful', `Welcome ${userData.account?.name || userData.username}!`);
              this.router.navigate(['/website/userdashboard']);
            }
          } else if (response.status === 'EXPIRED') {
            clearInterval(this.qrStatusInterval);
            this.showQrCode = false;
            this.alertService.userError('QR Code Expired', 'The QR code has expired. Please generate a new one.');
          }
        }
      },
      error: (err: any) => {
        console.error('QR status check error:', err);
      }
    });
  }
  
  closeQrCode() {
    this.showQrCode = false;
    if (this.qrStatusInterval) {
      clearInterval(this.qrStatusInterval);
    }
    this.qrToken = '';
    this.qrCodeImage = '';
  }
  
  completeQrLogin() {
    if (!this.qrToken || !this.loginUserId || !this.loginPassword) {
      this.alertService.userError('Validation Error', 'Please enter User ID and Password');
      return;
    }
    
    const loginData: any = {
      qrToken: this.qrToken,
      email: this.loginUserId,
      password: this.loginPassword
    };
    
    // Add OTP if provided
    if (this.showOtpInput && this.otpCode) {
      loginData.otp = this.otpCode;
    }
    
    this.http.post('http://localhost:8080/api/users/complete-qr-login', loginData).subscribe({
      next: (response: any) => {
        if (response.success && response.user) {
          // Login successful
          const userData = response.user;
          
          if (userData.status === 'APPROVED') {
            const sessionData = {
              id: userData.id,
              name: userData.account?.name || userData.username,
              email: userData.email,
              accountNumber: userData.accountNumber,
              status: userData.status,
              loginTime: new Date().toISOString()
            };
            
            sessionStorage.setItem('currentUser', JSON.stringify(sessionData));
            this.alertService.userSuccess('Login Successful', `Welcome ${userData.account?.name || userData.username}!`);
            this.router.navigate(['/website/userdashboard']);
          } else {
            this.alertService.userError('Account Pending', 'Account not approved yet. Please wait for admin approval');
          }
        } else if (response.requiresOtp) {
          // OTP required
          this.showOtpInput = true;
          this.successMessage = response.message || 'OTP has been sent to your email. Please enter the OTP to complete login.';
          this.errorMessage = '';
        } else {
          this.errorMessage = response.message || 'Login failed';
        }
      },
      error: (err: any) => {
        console.error('QR login error:', err);
        this.errorMessage = err.error?.message || 'Login failed. Please try again.';
        if (err.error?.accountLocked) {
          this.showUnlockButton = true;
          this.unlockEmail = this.loginUserId;
        }
      }
    });
  }
}
