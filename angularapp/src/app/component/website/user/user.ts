import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { WebAuthnService } from '../../../service/webauthn.service';
import { environment } from '../../../../environment/environment';
import { GraphicalPasswordComponent } from '../../shared/graphical-password/graphical-password';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [CommonModule, FormsModule, GraphicalPasswordComponent],
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
  
  // For graphical password
  useGraphicalPassword: boolean = false;
  graphicalPasswordSequence: number[] = [];
  showGraphicalPassword: boolean = false;
  
  // For graphical password setup
  showGraphicalPasswordSetup: boolean = false;
  setupGraphicalPasswordSequence: number[] = [];
  setupPassword: string = ''; // Regular password for verification during setup
  settingUpGraphicalPassword: boolean = false;
  showSetupButton: boolean = false; // Show setup button when graphical password not set
  
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

  // Fingerprint / WebAuthn state
  isFingerprintSupported: boolean = false;
  checkingFingerprint: boolean = false;
  hasFingerprintRegistered: boolean = false;
  isAuthenticatingFingerprint: boolean = false;

  constructor(
    private router: Router, 
    private http: HttpClient, 
    private alertService: AlertService,
    private route: ActivatedRoute,
    private webauthnService: WebAuthnService
  ) {}
  
  ngOnInit() {
    // Detect platform authenticator support (Windows Hello / Touch ID / Android Biometrics)
    this.isFingerprintSupported = this.webauthnService.isSupported();

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
    // Reset fingerprint specific errors on manual login attempt
    this.errorMessage = '';

    // Check if this is a QR code login
    if (this.isQrLoginMode && this.qrToken) {
      this.completeQrLogin();
      return;
    }
    
    // Check if using graphical password
    if (this.useGraphicalPassword) {
      if (!this.loginUserId || this.graphicalPasswordSequence.length === 0) {
        this.alertService.userError('Validation Error', 'Please enter User ID and select your graphical password');
        return;
      }
      this.authenticateWithGraphicalPassword();
      return;
    }
    
    if (!this.loginUserId || !this.loginPassword) {
      this.alertService.userError('Validation Error', 'Please enter User ID and Password');
      return;
    }

    if (this.loginUserId && this.loginPassword) {
      // Check if this is an admin login first
      this.http.post(`${environment.apiUrl}/admins/login`, {
        email: this.loginUserId,
        password: this.loginPassword
      }).subscribe({
        next: (adminResponse: any) => {
          if (adminResponse.success && adminResponse.role === 'ADMIN') {
            // Admin login successful - route to admin dashboard
            this.alertService.userSuccess('Login Successful', 'Welcome Admin!');
            this.router.navigate(['/admin/dashboard']);
          } else {
            // Not admin, try user login
            this.fetchUserDataAndLogin();
          }
        },
        error: (adminErr: any) => {
          // Admin login failed, try user login
          this.fetchUserDataAndLogin();
        }
      });
    } else {
      this.alertService.userError('Login Failed', 'Invalid User ID or Password');
    }
  }
  
  authenticateWithGraphicalPassword() {
    // Normalize email to lowercase and trim
    const normalizedEmail = this.loginUserId ? this.loginUserId.toLowerCase().trim() : '';
    
    if (!normalizedEmail) {
      this.alertService.userError('Validation Error', 'Please enter your email/User ID');
      return;
    }
    
    // Get device info
    const deviceInfo = navigator.userAgent || 'Unknown';
    
    this.http.post(`${environment.apiUrl}/users/authenticate-graphical`, {
      email: normalizedEmail,
      graphicalPassword: this.graphicalPasswordSequence,
      deviceInfo: deviceInfo
    }).subscribe({
      next: (authResponse: any) => {
        console.log('Graphical password authentication response:', authResponse);
        
        if (authResponse.success && authResponse.requiresOtp) {
          // Graphical password verified, now show OTP input
          this.showOtpInput = true;
          this.successMessage = authResponse.message || 'Graphical password verified. OTP has been sent to your email.';
          this.errorMessage = '';
          this.alertService.userSuccess('Graphical Password Verified', 'OTP has been sent to your email.');
        } else if (authResponse.success && authResponse.user) {
          // Direct login (if OTP is not required)
          const userData = authResponse.user;
          
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
        } else {
          if (authResponse.accountLocked) {
            this.showUnlockButton = true;
            this.errorMessage = authResponse.message || 'Account is locked due to multiple failed login attempts.';
            this.unlockEmail = this.loginUserId;
            this.alertService.userError('Account Locked', this.errorMessage);
          } else if (authResponse.failedAttempts !== undefined) {
            this.errorMessage = authResponse.message || `Invalid graphical password. ${3 - authResponse.failedAttempts} attempts remaining.`;
            this.alertService.userError('Login Failed', this.errorMessage);
            // Reset graphical password sequence on error
            this.graphicalPasswordSequence = [];
          } else {
            this.errorMessage = authResponse.message || 'Invalid graphical password. Please try again.';
            this.alertService.userError('Login Failed', this.errorMessage);
            this.graphicalPasswordSequence = [];
          }
          this.successMessage = '';
        }
      },
      error: (err: any) => {
        console.error('Graphical password authentication error:', err);
        if (err.error && err.error.accountLocked) {
          this.showUnlockButton = true;
          this.errorMessage = err.error.message || 'Account is locked due to multiple failed login attempts.';
          this.unlockEmail = this.loginUserId;
          this.showSetupButton = false;
        } else if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
          // Check if error message indicates graphical password not set
          if (err.error.message.toLowerCase().includes('graphical password not set') || 
              err.error.message.toLowerCase().includes('not set up')) {
            this.showSetupButton = true;
          } else {
            this.showSetupButton = false;
          }
        } else {
          this.errorMessage = 'Graphical password authentication failed. Please try again.';
          this.showSetupButton = false;
        }
        this.successMessage = '';
        this.graphicalPasswordSequence = [];
        this.alertService.userError('Login Failed', this.errorMessage);
      }
    });
  }

  /**
   * Check if the current email has a fingerprint credential registered.
   */
  checkFingerprintCredentials() {
    if (!this.isFingerprintSupported) {
      this.hasFingerprintRegistered = false;
      return;
    }

    const normalizedEmail = this.loginUserId ? this.loginUserId.toLowerCase().trim() : '';
    if (!normalizedEmail) {
      this.hasFingerprintRegistered = false;
      return;
    }

    this.checkingFingerprint = true;
    this.webauthnService.getCredentials(normalizedEmail).subscribe({
      next: (response: any) => {
        this.checkingFingerprint = false;
        if (response?.success && Array.isArray(response.credentials) && response.credentials.length > 0) {
          this.hasFingerprintRegistered = true;
          this.errorMessage = '';
        } else {
          this.hasFingerprintRegistered = false;
        }
      },
      error: () => {
        this.checkingFingerprint = false;
        this.hasFingerprintRegistered = false;
      }
    });
  }

  /**
   * Login using platform biometrics (WebAuthn / FIDO2).
   * Opens the native Windows Hello / Touch ID dialog when supported.
   */
  loginWithFingerprint() {
    if (!this.isFingerprintSupported) {
      this.errorMessage = 'Fingerprint login is not available in this browser. Please use a device with Windows Hello / Touch ID support.';
      return;
    }

    const normalizedEmail = this.loginUserId ? this.loginUserId.toLowerCase().trim() : '';
    if (!normalizedEmail) {
      this.errorMessage = 'Please enter your User ID / Email before using fingerprint login.';
      return;
    }

    this.isAuthenticatingFingerprint = true;
    this.errorMessage = '';

    this.webauthnService.authenticate(normalizedEmail).subscribe({
      next: (response: any) => {
        this.isAuthenticatingFingerprint = false;
        if (response?.success) {
          // Persist JWT if backend returns it
          if (response.token) {
            sessionStorage.setItem('jwt', response.token);
          }

          // Handle admin login via fingerprint
          if (response.role === 'ADMIN' || response.admin) {
            this.alertService.userSuccess('Login Successful', 'Welcome Admin!');
            this.router.navigate(['/admin/dashboard']);
            return;
          }

          // Handle user login via fingerprint
          const userData = response.user;
          if (userData && userData.status === 'APPROVED') {
            const sessionData = {
              id: userData.id,
              name: userData.account?.name || userData.username,
              email: userData.email,
              accountNumber: userData.accountNumber,
              status: userData.status,
              loginTime: new Date().toISOString()
            };

            sessionStorage.setItem('currentUser', JSON.stringify(sessionData));
            this.alertService.userSuccess('Login Successful', `Welcome ${sessionData.name}!`);
            this.router.navigate(['/website/userdashboard']);
          } else if (userData && userData.status !== 'APPROVED') {
            this.alertService.userError('Account Pending', 'Account not approved yet. Please wait for admin approval.');
          } else {
            this.alertService.userError('Login Failed', 'Unable to complete fingerprint login. Please try again.');
          }
        } else {
          this.errorMessage = response?.message || 'Fingerprint authentication failed. Please try again.';
        }
      },
      error: (err: any) => {
        this.isAuthenticatingFingerprint = false;
        if (err?.error?.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Fingerprint authentication failed. Please try again or use OTP login.';
        }
      }
    });
  }
  
  onGraphicalPasswordComplete(sequence: number[]) {
    this.graphicalPasswordSequence = sequence;
    console.log('Graphical password sequence selected:', sequence);
  }
  
  onGraphicalPasswordChanged(sequence: number[]) {
    this.graphicalPasswordSequence = sequence;
  }
  
  toggleGraphicalPassword() {
    this.useGraphicalPassword = !this.useGraphicalPassword;
    this.showGraphicalPassword = this.useGraphicalPassword;
    this.graphicalPasswordSequence = [];
    this.loginPassword = '';
    this.errorMessage = '';
    this.successMessage = '';
  }
  
  // Graphical Password Setup Methods
  onSetupGraphicalPasswordComplete(sequence: number[]) {
    this.setupGraphicalPasswordSequence = sequence;
    console.log('Setup graphical password sequence selected:', sequence);
  }
  
  onSetupGraphicalPasswordChanged(sequence: number[]) {
    this.setupGraphicalPasswordSequence = sequence;
  }
  
  setupGraphicalPassword() {
    // Check if user is logged in
    const currentUser = this.isUserLoggedIn() ? sessionStorage.getItem('currentUser') : null;
    let userEmail = this.loginUserId;
    
    if (currentUser) {
      const user = JSON.parse(currentUser);
      userEmail = user.email;
    }
    
    if (!userEmail) {
      this.alertService.userError('Validation Error', 'Please enter your email/User ID');
      return;
    }
    
    if (!this.setupPassword) {
      this.alertService.userError('Validation Error', 'Please enter your password to set up graphical password');
      return;
    }
    
    if (this.setupGraphicalPasswordSequence.length < 5) {
      this.alertService.userError('Validation Error', 'Please select 5 images for your graphical password');
      return;
    }
    
    this.settingUpGraphicalPassword = true;
    this.errorMessage = '';
    
    this.http.post(`${environment.apiUrl}/users/set-graphical-password`, {
      email: userEmail,
      password: this.setupPassword,
      graphicalPassword: this.setupGraphicalPasswordSequence
    }).subscribe({
      next: (response: any) => {
        console.log('Set graphical password response:', response);
        this.settingUpGraphicalPassword = false;
        
        if (response.success) {
          this.alertService.userSuccess('Graphical Password Set', 'Your graphical password has been set successfully! You can now use it for future logins.');
          this.closeGraphicalPasswordSetup();
          
          // If user is logged in, navigate to dashboard, otherwise stay on login page
          if (currentUser) {
            this.router.navigate(['/website/userdashboard']);
          } else {
            // User set up graphical password but not logged in - clear form and show success
            this.graphicalPasswordSequence = [];
            this.useGraphicalPassword = true; // Keep graphical password mode active
            this.errorMessage = '';
            this.successMessage = 'Graphical password set! You can now login with it.';
          }
        } else {
          this.errorMessage = response.message || 'Failed to set graphical password. Please try again.';
        }
      },
      error: (err: any) => {
        console.error('Set graphical password error:', err);
        this.settingUpGraphicalPassword = false;
        this.errorMessage = err.error?.message || 'Failed to set graphical password. Please check your password and try again.';
      }
    });
  }
  
  closeGraphicalPasswordSetup() {
    this.showGraphicalPasswordSetup = false;
    this.setupGraphicalPasswordSequence = [];
    this.setupPassword = '';
    this.errorMessage = '';
    this.showSetupButton = false;
    // Only navigate if we're not on login page (i.e., after successful login)
    if (sessionStorage.getItem('currentUser')) {
      this.router.navigate(['/website/userdashboard']);
    }
  }
  
  skipGraphicalPasswordSetup() {
    if (this.isUserLoggedIn()) {
      this.alertService.userSuccess('Setup Skipped', 'You can set up graphical password later from your profile settings.');
      this.closeGraphicalPasswordSetup();
    } else {
      // On login page, just close the modal
      this.closeGraphicalPasswordSetup();
    }
  }
  
  openGraphicalPasswordSetup() {
    // Show setup modal - user can set it up even if not logged in (they'll need to enter email and password)
    this.showGraphicalPasswordSetup = true;
    this.showSetupButton = false;
    this.errorMessage = '';
  }
  
  isUserLoggedIn(): boolean {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      return !!sessionStorage.getItem('currentUser');
    }
    return false;
  }

  fetchUserDataAndLogin() {
    // Normalize email to lowercase and trim
    const normalizedEmail = this.loginUserId ? this.loginUserId.toLowerCase().trim() : '';
    
    if (!normalizedEmail || !this.loginPassword) {
      this.alertService.userError('Validation Error', 'Please enter User ID and Password');
      return;
    }
    
    // Authenticate user with MySQL database
    this.http.post(`${environment.apiUrl}/users/authenticate`, {
      email: normalizedEmail,
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
        } else if (authResponse.success && authResponse.role === 'ADMIN') {
          // Admin login detected - route to admin dashboard
          this.alertService.userSuccess('Login Successful', 'Welcome Admin!');
          this.router.navigate(['/admin/dashboard']);
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
          // Authentication failed - show specific error message
          console.log('Authentication failed response:', authResponse);
          
          // Check if account is locked
          if (authResponse.accountLocked) {
            this.showUnlockButton = true;
            this.errorMessage = authResponse.message || 'Account is locked due to multiple failed login attempts.';
            this.unlockEmail = this.loginUserId; // Pre-fill email
            this.alertService.userError('Account Locked', this.errorMessage);
          } else if (authResponse.failedAttempts !== undefined) {
            this.errorMessage = authResponse.message || `Invalid credentials. ${3 - authResponse.failedAttempts} attempts remaining.`;
            this.alertService.userError('Login Failed', this.errorMessage);
          } else if (authResponse.message) {
            // Show the specific error message from backend
            this.errorMessage = authResponse.message;
            this.alertService.userError('Login Failed', this.errorMessage);
          } else {
            this.errorMessage = 'Invalid credentials. Please check your email and password.';
            this.alertService.userError('Login Failed', this.errorMessage);
          }
          this.successMessage = '';
        }
      },
      error: (err: any) => {
        console.error('Authentication error:', err);
        console.error('Error details:', JSON.stringify(err, null, 2));
        
        if (err.error && err.error.accountLocked) {
          this.showUnlockButton = true;
          this.errorMessage = err.error.message || 'Account is locked due to multiple failed login attempts.';
          this.unlockEmail = this.loginUserId; // Pre-fill email
        } else if (err.error && err.error.failedAttempts !== undefined) {
          this.errorMessage = err.error.message || 'Invalid credentials.';
        } else if (err.error && err.error.message) {
          // Show the actual error message from backend
          this.errorMessage = err.error.message;
        } else if (err.status === 0) {
          this.errorMessage = 'Unable to connect to server. Please check your connection.';
        } else if (err.status === 404) {
          this.errorMessage = 'Service not found. Please contact support.';
        } else if (err.status >= 500) {
          this.errorMessage = 'Server error. Please try again later.';
        } else {
          this.errorMessage = 'Login failed. Please check your credentials.';
        }
        this.successMessage = '';
        this.alertService.userError('Login Failed', this.errorMessage);
      }
    });
  }

  verifyOtp() {
    // Trim and validate OTP
    const trimmedOtp = this.otpCode ? this.otpCode.trim() : '';
    
    if (!trimmedOtp || trimmedOtp.length !== 6 || !/^\d{6}$/.test(trimmedOtp)) {
      this.alertService.userError('Validation Error', 'Please enter a valid 6-digit OTP');
      return;
    }

    // Normalize email to lowercase and trim
    const normalizedEmail = this.loginUserId ? this.loginUserId.toLowerCase().trim() : '';
    
    if (!normalizedEmail) {
      this.alertService.userError('Validation Error', 'Email is required');
      return;
    }

    this.verifyingOtp = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Get device info and location
    const deviceInfo = navigator.userAgent || 'Unknown';
    const loginMethod = this.useGraphicalPassword ? 'GRAPHICAL_PASSWORD' : 'PASSWORD';
    
    console.log('🔐 Verifying OTP:', {
      email: normalizedEmail,
      otpLength: trimmedOtp.length,
      loginMethod: loginMethod
    });
    
    this.http.post(`${environment.apiUrl}/users/verify-otp`, {
      email: normalizedEmail,
      otp: trimmedOtp,
      loginMethod: loginMethod,
      deviceInfo: deviceInfo,
      location: 'Browser Location' // Can be enhanced with geolocation API
    }).subscribe({
      next: (response: any) => {
        console.log('OTP verification response:', response);
        this.verifyingOtp = false;

        if (response.success && response.role === 'ADMIN') {
          // Admin login detected - route to admin dashboard
          this.alertService.userSuccess('Login Successful', 'Welcome Admin!');
          this.router.navigate(['/admin/dashboard']);
        } else if (response.success && response.user) {
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
        console.error('❌ OTP verification error:', err);
        console.error('Error details:', {
          status: err.status,
          statusText: err.statusText,
          error: err.error,
          message: err.error?.message || err.message
        });
        
        this.verifyingOtp = false;
        
        // Provide more specific error messages
        let errorMessage = 'OTP verification failed. Please try again.';
        
        if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (err.status === 0) {
          errorMessage = 'Unable to connect to server. Please check your connection.';
        } else if (err.status === 400) {
          errorMessage = err.error?.message || 'Invalid OTP. Please check and try again.';
        } else if (err.status === 401) {
          errorMessage = 'Authentication failed. Please try logging in again.';
        } else if (err.status >= 500) {
          errorMessage = 'Server error. Please try again later.';
        }
        
        this.errorMessage = errorMessage;
        this.alertService.userError('OTP Verification Failed', errorMessage);
        this.otpCode = ''; // Clear OTP input
      }
    });
  }

  resendOtp() {
    this.resendingOtp = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post(`${environment.apiUrl}/users/resend-otp`, {
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
    
    this.http.post(`${environment.apiUrl}/users/create`, newUser).subscribe({
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

    this.http.post(`${environment.apiUrl}/users/unlock-account`, unlockData).subscribe({
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

    this.http.post(`${environment.apiUrl}/users/send-reset-otp`, {
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
      ? `${environment.apiUrl}/users/reset-password-with-otp`
      : `${environment.apiUrl}/users/reset-password`;

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
    this.http.post(`${environment.apiUrl}/users/generate-qr-login`, {}).subscribe({
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
    
    this.http.get(`${environment.apiUrl}/users/check-qr-login-status/${this.qrToken}`).subscribe({
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
    
    this.http.post(`${environment.apiUrl}/users/complete-qr-login`, loginData).subscribe({
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
