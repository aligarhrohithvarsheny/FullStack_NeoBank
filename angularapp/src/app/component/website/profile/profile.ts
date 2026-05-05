import { Component, OnInit, Inject, PLATFORM_ID, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';
import { timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
// import { UserService } from '../../service/user';
// import { AccountService } from '../../service/account';
// import { TransactionService } from '../../service/transaction';

interface UserProfile {
  name: string;
  email: string;
  accountNumber: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  accountType: string;
  joinDate: string;
  pan?: string;
  aadhar?: string;
  occupation?: string;
  income?: number;
  ifscCode?: string;
  aadharVerified?: boolean;
  aadharVerificationStatus?: string;
  aadharVerifiedDate?: string;
  account?: {
    balance?: number;
    customerId?: string;
    [key: string]: any;
  };
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css']
})
export class Profile implements OnInit, AfterViewInit {
  userProfile: UserProfile = {
    name: '',
    email: '',
    accountNumber: '',
    phoneNumber: '',
    address: '',
    dateOfBirth: '',
    accountType: '',
    joinDate: '',
    ifscCode: 'NEO0008648'
  };

  currentBalance: number = 0;
  loading: boolean = false;
  error: string = '';
  editingEmail: boolean = false;
  newEmail: string = '';
  emailError: string = '';
  updatingEmail: boolean = false;
  currentUserId: number | null = null;
  
  // Aadhaar verification
  aadharVerifying: boolean = false;
  aadharVerificationStatus: any = null;
  
  // Profile update (address/phone) with OTP
  editingAddress: boolean = false;
  editingPhone: boolean = false;
  newAddress: string = '';
  newPhone: string = '';
  updateError: string = '';
  isRequestingUpdate: boolean = false;
  showOtpModal: boolean = false;
  otpInput: string = '';
  otpError: string = '';
  isVerifyingOtp: boolean = false;
  currentUpdateRequestId: number | null = null;
  currentUpdateField: string = '';
  otpTimer: number = 120; // 2 minutes
  otpTimerInterval: any = null;
  updateHistory: any[] = [];
  isLoadingUpdateHistory: boolean = false;

  // Profile photo and signature
  profilePhoto: File | null = null;
  signature: File | null = null;
  uploadingPhoto: boolean = false;
  uploadingSignature: boolean = false;
  profilePhotoUrl: string | null = null;
  signatureUrl: string | null = null;
  signatureStatus: string = '';
  signatureRejectionReason: string = '';
  showSignature: boolean = false;

  // Canvas-based online signature drawing
  @ViewChild('signatureCanvas') signatureCanvas?: ElementRef<HTMLCanvasElement>;
  isDrawingSignature: boolean = false;
  private signatureCtx: CanvasRenderingContext2D | null = null;
  private lastSigX: number | null = null;
  private lastSigY: number | null = null;
  useCanvasSignature: boolean = true;

  // Helper method to trigger file input
  triggerFileInput(inputId: string) {
    const input = document.getElementById(inputId) as HTMLInputElement;
    if (input) {
      input.click();
    }
  }

  // Generate and download passbook
  generatingPassbook: boolean = false;

  generatePassbook() {
    if (!this.currentUserId) {
      this.alertService.userError('Error', 'User ID not found. Please refresh the page.');
      return;
    }

    this.generatingPassbook = true;
    
    // Download passbook PDF
    this.http.get(`${environment.apiBaseUrl}/api/users/${this.currentUserId}/passbook`, { 
      responseType: 'blob' 
    }).subscribe({
      next: (blob: Blob) => {
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `NeoBank_Passbook_${this.userProfile.accountNumber}_${new Date().toISOString().split('T')[0]}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        this.alertService.userSuccess('Passbook Generated', 'Your passbook has been downloaded successfully!');
        this.generatingPassbook = false;
      },
      error: (err: any) => {
        console.error('Error generating passbook:', err);
        this.alertService.userError('Error', 'Failed to generate passbook. Please try again.');
        this.generatingPassbook = false;
      }
    });
  }

  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
    // private userService: UserService,
    // private accountService: AccountService,
    // private transactionService: TransactionService
  ) {}

  ngOnInit() {
    // Only load in browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
      this.checkAadharCallback();
      this.loadUserProfile();
    }
  }

  ngAfterViewInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.initSignatureCanvas();
  }

  checkAadharCallback() {
    // Check if returning from Aadhaar verification
    const urlParams = new URLSearchParams(window.location.search);
    const callbackRef = urlParams.get('aadhar_callback');
    
    if (callbackRef) {
      // Simulate verification success (in production, this would come from UIDAI callback)
      // For demo, we'll automatically mark as verified
      const currentUser = sessionStorage.getItem('currentUser');
      if (currentUser) {
        const user = JSON.parse(currentUser);
        const accountNumber = user.accountNumber;
        
        if (accountNumber) {
          // Call backend to complete verification
          this.completeAadharVerification(callbackRef, accountNumber);
        }
      }
      
      // Clean URL
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.loading = true;
    this.error = '';
    
    // Get user session from sessionStorage (MySQL-based authentication)
    const currentUser = sessionStorage.getItem('currentUser');
    console.log('Profile component - currentUser from sessionStorage:', currentUser);
    
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        console.log('Profile component - parsed user data:', user);
        this.currentUserId = user.id;
        
        // Set basic info immediately from session (fast, no API call)
        this.userProfile = {
          name: user.name || 'User',
          email: user.email || 'user@example.com',
          accountNumber: user.accountNumber || 'ACC001',
          phoneNumber: user.phoneNumber || user.phone || '',
          address: user.address || '',
          dateOfBirth: user.dateOfBirth || user.dob || '',
          accountType: user.accountType || 'Savings Account',
          joinDate: user.joinDate || (user.createdAt ? new Date(user.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0]),
          pan: user.pan || '',
          aadhar: user.aadhar || user.aadharNumber || '',
          occupation: user.occupation || '',
          income: user.income || 0,
          ifscCode: 'NEO0008648'
        };
        this.loading = false; // Show profile immediately
        
        // Load full user profile from MySQL database in background
        // Ensure ID is a number
        const userIdNum = typeof user.id === 'number' ? user.id : Number(user.id);
        const profileUrl = !isNaN(userIdNum)
          ? `${environment.apiBaseUrl}/api/users/${userIdNum}`
          : (user.accountNumber ? `${environment.apiBaseUrl}/api/users/account/${user.accountNumber}` : null);
        if (!profileUrl) {
          console.error('Invalid session data: missing user id and account number');
          return;
        }
        
        // Load full profile from API in background
        this.http.get(profileUrl)
        .pipe(
          timeout(10000), // 10 second timeout
          catchError(err => {
            console.warn('Error loading user profile from MySQL (non-critical):', err);
            // Use session data if available, otherwise use fallback
            if (!this.userProfile || this.userProfile.name === 'User') {
              this.userProfile = {
                name: user.name || 'User',
                email: user.email || 'user@example.com',
                accountNumber: user.accountNumber || 'ACC001',
                phoneNumber: '',
                address: '',
                dateOfBirth: '',
                accountType: 'Savings Account',
                joinDate: new Date().toISOString().split('T')[0],
                pan: '',
                aadhar: '',
                occupation: '',
                income: 0,
                ifscCode: 'NEO0008648'
              };
            }
            this.currentBalance = 0;
            this.loading = false;
            return of(null);
          })
        )
        .subscribe({
          next: (userData: any) => {
            if (!userData) return; // Error occurred
            console.log('User profile loaded from MySQL:', userData);
            this.userProfile = {
              name: userData.account?.name || userData.username || this.userProfile.name || 'User',
              email: userData.email || this.userProfile.email || 'user@example.com',
              accountNumber: userData.accountNumber || this.userProfile.accountNumber || 'ACC001',
              phoneNumber: userData.account?.phone || userData.phoneNumber || this.userProfile.phoneNumber || '',
              address: userData.account?.address || userData.address || this.userProfile.address || '',
              dateOfBirth: userData.account?.dob || userData.dateOfBirth || this.userProfile.dateOfBirth || '',
              accountType: userData.account?.accountType || this.userProfile.accountType || 'Savings Account',
              joinDate: userData.createdAt ? new Date(userData.createdAt).toISOString().split('T')[0] : this.userProfile.joinDate || new Date().toISOString().split('T')[0],
              pan: userData.account?.pan || userData.pan || this.userProfile.pan || '',
              aadhar: userData.account?.aadharNumber || userData.aadhar || this.userProfile.aadhar || '',
              occupation: userData.account?.occupation || userData.occupation || this.userProfile.occupation || '',
              income: userData.account?.income || userData.income || this.userProfile.income || 0,
              ifscCode: 'NEO0008648',
              aadharVerified: userData.account?.aadharVerified || false,
              aadharVerificationStatus: userData.account?.aadharVerificationStatus || 'PENDING',
              aadharVerifiedDate: userData.account?.aadharVerifiedDate || '',
              account: userData.account ? { ...userData.account, customerId: userData.account.customerId } : this.userProfile.account
            };
            
            // Load current balance from MySQL
            this.loadCurrentBalanceFromMySQL();
            // Load Aadhaar verification status
            this.loadAadharVerificationStatus();
            // Load profile photo and signature status (not image)
            this.loadProfilePhoto();
            this.loadSignature();
          }
        });
      } catch (e) {
        console.error('Error parsing user session:', e);
        this.loading = false;
        this.error = 'Error loading user profile';
      }
    } else {
      // Try to get user from localStorage as fallback (for existing users)
      const savedProfile = localStorage.getItem('user_profile');
      if (savedProfile) {
        const profile = JSON.parse(savedProfile);
        this.userProfile = profile;
        this.currentBalance = 0; // Will be loaded separately
        this.loading = false;
        console.log('Using fallback profile from localStorage');
      } else {
        // Try to get user from sessionStorage as last resort
        const currentUser = sessionStorage.getItem('currentUser');
        if (currentUser) {
          const user = JSON.parse(currentUser);
          this.currentUserId = user.id || null;
          this.userProfile = {
            name: user.name || 'User',
            email: user.email || 'user@example.com',
            accountNumber: user.accountNumber || 'ACC001',
            phoneNumber: '',
            address: '',
            dateOfBirth: '',
            accountType: 'Savings Account',
            joinDate: new Date().toISOString().split('T')[0],
            pan: '',
            aadhar: '',
            occupation: '',
            income: 0,
            ifscCode: 'NEO0008648'
          };
          this.currentBalance = 0;
          this.loading = false;
          console.log('Using session profile - no localStorage found');
        } else {
          // No session found - try useful fallbacks before showing defaults
          // 1. Check localStorage (older clients)
          const savedProfile = localStorage.getItem('user_profile');
          if (savedProfile) {
            this.userProfile = JSON.parse(savedProfile);
            this.currentBalance = 0;
            this.loading = false;
            console.log('Using fallback profile from localStorage');
          } else {
            // 2. Try to fetch current user from backend using JWT (useful when only JWT is stored)
            const jwt = sessionStorage.getItem('jwt') || localStorage.getItem('jwt');
            if (jwt) {
              this.http.get(`${environment.apiBaseUrl}/api/users/me`, { headers: { Authorization: `Bearer ${jwt}` } }).subscribe({
                next: (me: any) => {
                  if (me && me.id) {
                    const userObj = me;
                    this.currentUserId = userObj.id;
                    this.userProfile = {
                      name: userObj.account?.name || userObj.username || 'User',
                      email: userObj.email || 'user@example.com',
                      accountNumber: userObj.accountNumber || 'ACC001',
                      phoneNumber: userObj.account?.phone || userObj.phoneNumber || '',
                      address: userObj.account?.address || userObj.address || '',
                      dateOfBirth: userObj.account?.dob || userObj.dateOfBirth || '',
                      accountType: userObj.account?.accountType || 'Savings Account',
                      joinDate: userObj.createdAt ? new Date(userObj.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
                      pan: userObj.account?.pan || userObj.pan || '',
                      aadhar: userObj.account?.aadharNumber || userObj.aadhar || '',
                      occupation: userObj.account?.occupation || userObj.occupation || '',
                      income: userObj.account?.income || userObj.income || 0,
                      ifscCode: 'NEO0008648'
                    } as UserProfile;
                    this.loading = false;
                    // background loads
                    this.loadCurrentBalanceFromMySQL();
                    this.loadAadharVerificationStatus();
                    this.loadProfilePhoto();
                    this.loadSignature();
                    // persist session for other pages
                    sessionStorage.setItem('currentUser', JSON.stringify({ id: this.currentUserId, name: this.userProfile.name, email: this.userProfile.email, accountNumber: this.userProfile.accountNumber, status: 'APPROVED', loginTime: new Date().toISOString() }));
                  } else {
                    // fallback defaults
                    this.userProfile = {
                      name: 'User',
                      email: 'user@example.com',
                      accountNumber: 'ACC001',
                      phoneNumber: '',
                      address: '',
                      dateOfBirth: '',
                      accountType: 'Savings Account',
                      joinDate: new Date().toISOString().split('T')[0],
                      pan: '',
                      aadhar: '',
                      occupation: '',
                      income: 0,
                      ifscCode: 'NEO0008648'
                    };
                    this.loading = false;
                  }
                },
                error: (err: any) => {
                  console.warn('No session and /api/users/me failed:', err);
                  this.userProfile = {
                    name: 'User',
                    email: 'user@example.com',
                    accountNumber: 'ACC001',
                    phoneNumber: '',
                    address: '',
                    dateOfBirth: '',
                    accountType: 'Savings Account',
                    joinDate: new Date().toISOString().split('T')[0],
                    pan: '',
                    aadhar: '',
                    occupation: '',
                    income: 0,
                    ifscCode: 'NEO0008648'
                  };
                  this.loading = false;
                }
              });
            } else {
              // No session, no JWT, no local profile - create default profile to avoid UI errors
              this.userProfile = {
                name: 'User',
                email: 'user@example.com',
                accountNumber: 'ACC001',
                phoneNumber: '',
                address: '',
                dateOfBirth: '',
                accountType: 'Savings Account',
                joinDate: new Date().toISOString().split('T')[0],
                pan: '',
                aadhar: '',
                occupation: '',
                income: 0,
                ifscCode: 'NEO0008648'
              };
              this.currentBalance = 0;
              this.loading = false;
              console.log('Using default profile - no session found');
            }
          }
        }
      }
    }
  }

  loadCurrentBalanceFromMySQL() {
    if (!this.userProfile.accountNumber) {
      this.loading = false;
      return;
    }
    
    // Load current balance from MySQL database (non-blocking)
    this.http.get(`${environment.apiBaseUrl}/api/accounts/balance/${this.userProfile.accountNumber}`)
      .pipe(
        timeout(8000),
        catchError(err => {
          console.warn('Error loading balance from MySQL (non-critical):', err);
          // Fallback to user profile balance or cached balance
          const cachedBalance = localStorage.getItem(`balance_${this.userProfile.accountNumber}`);
          this.currentBalance = cachedBalance ? parseFloat(cachedBalance) : (this.userProfile?.account?.balance || 0);
          return of({ balance: this.currentBalance });
        })
      )
      .subscribe({
        next: (balanceData: any) => {
          if (balanceData?.balance !== undefined) {
            console.log('Balance loaded from MySQL:', balanceData);
            this.currentBalance = balanceData.balance || 0;
            // Cache the balance
            localStorage.setItem(`balance_${this.userProfile.accountNumber}`, this.currentBalance.toString());
          }
          this.loading = false;
        }
      });
  }

  loadCurrentBalance() {
    // This method is now replaced by loadCurrentBalanceFromMySQL()
    this.loadCurrentBalanceFromMySQL();
  }

  loadFromLocalStorage() {
    // Fallback to localStorage if backend fails
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      this.userProfile = { ...this.userProfile, ...JSON.parse(savedProfile) };
    }
    
    const userTransactions = localStorage.getItem(`user_transactions_${this.userProfile.accountNumber}`);
    if (userTransactions) {
      const transactions = JSON.parse(userTransactions);
      if (transactions.length > 0) {
        this.currentBalance = transactions[0].balance;
      }
    }
    this.loading = false;
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }

  editProfile() {
    // For now, just show an alert. In a real app, this would open an edit form
    this.alertService.info('Profile Editing', 'Profile editing feature will be available soon!');
  }
  
  // Profile Update Methods (Address/Phone with OTP)
  startEditingAddress() {
    this.editingAddress = true;
    this.newAddress = this.userProfile.address || '';
    this.updateError = '';
  }
  
  cancelEditingAddress() {
    this.editingAddress = false;
    this.newAddress = '';
    this.updateError = '';
  }
  
  startEditingPhone() {
    this.editingPhone = true;
    this.newPhone = this.userProfile.phoneNumber || '';
    this.updateError = '';
  }
  
  cancelEditingPhone() {
    this.editingPhone = false;
    this.newPhone = '';
    this.updateError = '';
  }
  
  requestAddressUpdate() {
    if (!this.currentUserId) {
      this.updateError = 'User ID not found. Please refresh the page.';
      return;
    }
    
    if (!this.newAddress || this.newAddress.trim() === '') {
      this.updateError = 'Address is required';
      return;
    }
    
    if (this.newAddress === this.userProfile.address) {
      this.updateError = 'New address is the same as current address';
      return;
    }
    
    this.isRequestingUpdate = true;
    this.updateError = '';
    
    const params = new HttpParams()
      .set('userId', this.currentUserId.toString())
      .set('field', 'ADDRESS')
      .set('newValue', this.newAddress.trim());
    
    this.http.post(`${environment.apiBaseUrl}/api/users/profile-update/request`, null, { params }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.currentUpdateRequestId = response.requestId;
          this.currentUpdateField = 'ADDRESS';
          this.showOtpModal = true;
          this.otpInput = '';
          this.otpError = '';
          this.startOtpTimer();
          this.alertService.success('OTP Sent', 'OTP has been sent to your email. Please verify to submit the update request.');
        } else {
          this.updateError = response.message || 'Failed to request update';
        }
        this.isRequestingUpdate = false;
      },
      error: (err: any) => {
        console.error('Error requesting address update:', err);
        this.updateError = err.error?.message || 'Failed to request update. Please try again.';
        this.isRequestingUpdate = false;
      }
    });
  }
  
  requestPhoneUpdate() {
    if (!this.currentUserId) {
      this.updateError = 'User ID not found. Please refresh the page.';
      return;
    }
    
    if (!this.newPhone || this.newPhone.trim() === '') {
      this.updateError = 'Phone number is required';
      return;
    }
    
    // Basic phone validation (10 digits)
    const phoneRegex = /^[0-9]{10}$/;
    if (!phoneRegex.test(this.newPhone.trim())) {
      this.updateError = 'Please enter a valid 10-digit phone number';
      return;
    }
    
    if (this.newPhone === this.userProfile.phoneNumber) {
      this.updateError = 'New phone number is the same as current phone number';
      return;
    }
    
    this.isRequestingUpdate = true;
    this.updateError = '';
    
    const params = new HttpParams()
      .set('userId', this.currentUserId.toString())
      .set('field', 'PHONE')
      .set('newValue', this.newPhone.trim());
    
    this.http.post(`${environment.apiBaseUrl}/api/users/profile-update/request`, null, { params }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.currentUpdateRequestId = response.requestId;
          this.currentUpdateField = 'PHONE';
          this.showOtpModal = true;
          this.otpInput = '';
          this.otpError = '';
          this.startOtpTimer();
          this.alertService.success('OTP Sent', 'OTP has been sent to your email. Please verify to submit the update request.');
        } else {
          this.updateError = response.message || 'Failed to request update';
        }
        this.isRequestingUpdate = false;
      },
      error: (err: any) => {
        console.error('Error requesting phone update:', err);
        this.updateError = err.error?.message || 'Failed to request update. Please try again.';
        this.isRequestingUpdate = false;
      }
    });
  }
  
  verifyOtpAndSubmit() {
    if (!this.currentUpdateRequestId) {
      this.otpError = 'Update request not found';
      return;
    }
    
    if (!this.otpInput || this.otpInput.length !== 6) {
      this.otpError = 'Please enter a valid 6-digit OTP';
      return;
    }
    
    this.isVerifyingOtp = true;
    this.otpError = '';
    
    const params = new HttpParams()
      .set('requestId', this.currentUpdateRequestId.toString())
      .set('otp', this.otpInput);
    
    this.http.post(`${environment.apiBaseUrl}/api/users/profile-update/verify-otp`, null, { params }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Request Submitted', 'OTP verified. Your update request has been submitted for admin approval.');
          this.closeOtpModal();
          if (this.currentUpdateField === 'ADDRESS') {
            this.cancelEditingAddress();
          } else {
            this.cancelEditingPhone();
          }
          this.loadUpdateHistory();
        } else {
          this.otpError = response.message || 'Invalid or expired OTP';
        }
        this.isVerifyingOtp = false;
      },
      error: (err: any) => {
        console.error('Error verifying OTP:', err);
        this.otpError = err.error?.message || 'Failed to verify OTP. Please try again.';
        this.isVerifyingOtp = false;
      }
    });
  }
  
  resendOtp() {
    if (!this.currentUpdateRequestId) return;
    
    this.http.post(`${environment.apiBaseUrl}/api/users/profile-update/resend-otp`, null, {
      params: new HttpParams().set('requestId', this.currentUpdateRequestId.toString())
    }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('OTP Resent', 'OTP has been resent to your email.');
          this.startOtpTimer();
        } else {
          this.otpError = response.message || 'Failed to resend OTP';
        }
      },
      error: (err: any) => {
        console.error('Error resending OTP:', err);
        this.otpError = err.error?.message || 'Failed to resend OTP';
      }
    });
  }
  
  closeOtpModal() {
    this.showOtpModal = false;
    this.otpInput = '';
    this.otpError = '';
    this.currentUpdateRequestId = null;
    this.currentUpdateField = '';
    this.stopOtpTimer();
  }
  
  startOtpTimer() {
    this.otpTimer = 120; // 2 minutes
    this.stopOtpTimer();
    this.otpTimerInterval = setInterval(() => {
      this.otpTimer--;
      if (this.otpTimer <= 0) {
        this.stopOtpTimer();
      }
    }, 1000);
  }
  
  stopOtpTimer() {
    if (this.otpTimerInterval) {
      clearInterval(this.otpTimerInterval);
      this.otpTimerInterval = null;
    }
  }
  
  loadUpdateHistory() {
    if (!this.currentUserId) return;
    
    this.isLoadingUpdateHistory = true;
    this.http.get(`${environment.apiBaseUrl}/api/users/profile-update/history/${this.currentUserId}`).subscribe({
      next: (history: any) => {
        this.updateHistory = history || [];
        this.isLoadingUpdateHistory = false;
      },
      error: (err: any) => {
        console.error('Error loading update history:', err);
        this.isLoadingUpdateHistory = false;
      }
    });
  }
  
  getUpdateStatusClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'COMPLETED': return 'status-approved';
      case 'OTP_VERIFIED': case 'PENDING': return 'status-pending';
      case 'REJECTED': return 'status-rejected';
      default: return 'status-default';
    }
  }
  
  onOtpInput(event: any) {
    const val = (event.target?.value ?? '').replace(/\D/g, '').substring(0, 6);
    this.otpInput = val;
  }
  
  formatTimer(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }
  
  ngOnDestroy() {
    this.stopOtpTimer();
    this.stopOtpTimer();
  }

  startEditingEmail() {
    this.editingEmail = true;
    this.newEmail = this.userProfile.email;
    this.emailError = '';
  }

  cancelEditingEmail() {
    this.editingEmail = false;
    this.newEmail = '';
    this.emailError = '';
  }

  validateEmail(email: string): boolean {
    const emailRegex = /^[A-Za-z0-9+_.-]+@(.+)$/;
    return emailRegex.test(email);
  }

  updateEmail() {
    if (!this.currentUserId) {
      this.emailError = 'User ID not found. Please refresh the page.';
      return;
    }

    // Validate email format
    if (!this.newEmail || this.newEmail.trim() === '') {
      this.emailError = 'Email is required';
      return;
    }

    if (!this.validateEmail(this.newEmail)) {
      this.emailError = 'Invalid email format';
      return;
    }

    // Check if email is the same as current email
    if (this.newEmail === this.userProfile.email) {
      this.emailError = 'New email is the same as current email';
      return;
    }

    this.updatingEmail = true;
    this.emailError = '';

    // Call the backend API to update email
    this.http.put(`${environment.apiBaseUrl}/api/users/update-email/${this.currentUserId}`, { email: this.newEmail }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.userProfile.email = this.newEmail;
          this.editingEmail = false;
          this.newEmail = '';
          this.updatingEmail = false;
          this.alertService.success('Email Updated', 'Your email has been updated successfully!');
          
          // Update session storage with new email
          const currentUser = sessionStorage.getItem('currentUser');
          if (currentUser) {
            const user = JSON.parse(currentUser);
            user.email = this.newEmail;
            sessionStorage.setItem('currentUser', JSON.stringify(user));
          }
        } else {
          this.emailError = response.message || 'Failed to update email';
          this.updatingEmail = false;
        }
      },
      error: (err: any) => {
        console.error('Error updating email:', err);
        this.emailError = err.error?.message || 'Failed to update email. Please try again.';
        this.updatingEmail = false;
      }
    });
  }

  loadAadharVerificationStatus() {
    if (!this.userProfile.accountNumber) return;
    
    this.http.get(`${environment.apiBaseUrl}/api/accounts/aadhar/status/${this.userProfile.accountNumber}`).subscribe({
      next: (status: any) => {
        if (status.success) {
          this.userProfile.aadharVerified = status.aadharVerified;
          this.userProfile.aadharVerificationStatus = status.verificationStatus;
          this.userProfile.aadharVerifiedDate = status.verifiedDate;
          this.aadharVerificationStatus = status;
        }
      },
      error: (err: any) => {
        console.error('Error loading Aadhaar verification status:', err);
      }
    });
  }

  initiateAadharVerification() {
    if (!this.userProfile.accountNumber) {
      this.alertService.error('Error', 'Account number not found');
      return;
    }

    if (!this.userProfile.aadhar) {
      this.alertService.error('Error', 'Aadhaar number not found in profile. Please update your profile first.');
      return;
    }

    this.aadharVerifying = true;
    
    this.http.post(`${environment.apiBaseUrl}/api/accounts/aadhar/verify/${this.userProfile.accountNumber}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.info(
            'Aadhaar Verification',
            'Redirecting to Aadhaar website for verification. Please complete the verification process.'
          );
          
          // Open Aadhaar verification URL in new window
          // In production, this would be UIDAI's official verification portal
          const verificationWindow = window.open(
            response.verificationUrl,
            'AadhaarVerification',
            'width=800,height=600,scrollbars=yes,resizable=yes'
          );
          
          // For demo purposes, simulate verification after 3 seconds
          // In production, this would be handled by UIDAI callback
          setTimeout(() => {
            if (verificationWindow) {
              verificationWindow.close();
            }
            // Simulate successful verification callback
            this.completeAadharVerification(response.verificationReference, this.userProfile.accountNumber);
          }, 3000);
          
          this.aadharVerifying = false;
        } else {
          this.alertService.error('Verification Failed', response.message || 'Failed to initiate Aadhaar verification');
          this.aadharVerifying = false;
        }
      },
      error: (err: any) => {
        console.error('Error initiating Aadhaar verification:', err);
        this.alertService.error('Verification Failed', err.error?.message || 'Failed to initiate Aadhaar verification');
        this.aadharVerifying = false;
      }
    });
  }

  completeAadharVerification(verificationReference: string, accountNumber: string) {
    // In production, this would be called by UIDAI's callback
    // For demo, we'll simulate a successful verification
    const params = new URLSearchParams();
    params.set('verificationReference', verificationReference);
    params.set('status', 'VERIFIED');
    if (this.userProfile.aadhar) params.set('aadharNumber', this.userProfile.aadhar);
    if (this.userProfile.name) params.set('name', this.userProfile.name);
    if (this.userProfile.dateOfBirth) params.set('dob', this.userProfile.dateOfBirth);

    this.http.post(`${environment.apiBaseUrl}/api/accounts/aadhar/callback?${params.toString()}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Aadhaar Verified', 'Your Aadhaar has been verified successfully!');
          this.loadUserProfile();
          this.loadAadharVerificationStatus();
        } else {
          this.alertService.error('Verification Failed', response.message || 'Aadhaar verification failed');
        }
      },
      error: (err: any) => {
        console.error('Error completing Aadhaar verification:', err);
        this.alertService.error('Verification Failed', err.error?.message || 'Failed to complete Aadhaar verification');
      }
    });
  }

  getAadharStatusClass(): string {
    if (this.userProfile.aadharVerified) {
      return 'status-verified';
    } else if (this.userProfile.aadharVerificationStatus === 'PENDING') {
      return 'status-pending';
    } else if (this.userProfile.aadharVerificationStatus === 'FAILED') {
      return 'status-failed';
    }
    return 'status-not-verified';
  }

  getAadharStatusText(): string {
    if (this.userProfile.aadharVerified) {
      return 'Verified';
    } else if (this.userProfile.aadharVerificationStatus === 'PENDING') {
      return 'Verification Pending';
    } else if (this.userProfile.aadharVerificationStatus === 'FAILED') {
      return 'Verification Failed';
    }
    return 'Not Verified';
  }

  // Profile photo upload
  onProfilePhotoSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.alertService.userError('File Too Large', 'Profile photo must be less than 5MB');
        // Reset file input
        const fileInput = event.target as HTMLInputElement;
        if (fileInput) {
          fileInput.value = '';
        }
        return;
      }
      
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!validTypes.includes(file.type)) {
        this.alertService.userError('Invalid File Type', 'Profile photo must be JPEG, PNG, or PDF');
        // Reset file input
        const fileInput = event.target as HTMLInputElement;
        if (fileInput) {
          fileInput.value = '';
        }
        return;
      }
      
      // Store the file
      this.profilePhoto = file;
      console.log('Profile photo selected:', file.name, file.size, 'bytes');
      
      // Preview image
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.profilePhotoUrl = e.target.result;
      };
      reader.onerror = () => {
        console.error('Error reading file for preview');
        this.alertService.userError('Error', 'Failed to read file for preview');
      };
      reader.readAsDataURL(file);
    } else {
      // No file selected - clear previous selection
      this.profilePhoto = null;
      console.log('No file selected');
    }
  }

  uploadProfilePhoto() {
    // Check if file is selected
    if (!this.profilePhoto) {
      // Try to get file from input element as fallback
      const fileInput = document.getElementById('profilePhotoInput') as HTMLInputElement;
      if (fileInput && fileInput.files && fileInput.files.length > 0) {
        const file = fileInput.files[0];
        // Validate file
        if (file.size > 5 * 1024 * 1024) {
          this.alertService.userError('File Too Large', 'Profile photo must be less than 5MB');
          fileInput.value = '';
          return;
        }
        const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
        if (!validTypes.includes(file.type)) {
          this.alertService.userError('Invalid File Type', 'Profile photo must be JPEG, PNG, or PDF');
          fileInput.value = '';
          return;
        }
        this.profilePhoto = file;
        console.log('File recovered from input element:', file.name);
      } else {
        this.alertService.userError('Error', 'Please select a profile photo first by clicking "Choose Profile Photo"');
        return;
      }
    }

    if (!this.currentUserId) {
      this.alertService.userError('Error', 'User ID not found. Please refresh the page.');
      return;
    }

    this.uploadingPhoto = true;
    const formData = new FormData();
    formData.append('profilePhoto', this.profilePhoto);

    console.log('Uploading profile photo:', this.profilePhoto.name, this.profilePhoto.size, 'bytes');

    this.http.post(`${environment.apiBaseUrl}/api/users/${this.currentUserId}/upload-profile-photo`, formData).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.userSuccess('Success', 'Profile photo uploaded successfully!');
          this.profilePhoto = null;
          // Reset the file input element
          const fileInput = document.getElementById('profilePhotoInput') as HTMLInputElement;
          if (fileInput) {
            fileInput.value = '';
          }
          // Clear preview temporarily, will be reloaded from server
          this.profilePhotoUrl = null;
          this.loadProfilePhoto();
        } else {
          this.alertService.userError('Upload Failed', response.message || 'Failed to upload profile photo');
        }
        this.uploadingPhoto = false;
      },
      error: (err: any) => {
        console.error('Error uploading profile photo:', err);
        this.alertService.userError('Upload Failed', err.error?.message || 'Failed to upload profile photo');
        this.uploadingPhoto = false;
      }
    });
  }

  loadProfilePhoto() {
    if (!this.currentUserId) return;
    
    this.http.get(`${environment.apiBaseUrl}/api/users/${this.currentUserId}/profile-photo`, { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.profilePhotoUrl = e.target.result;
        };
        reader.readAsDataURL(blob);
      },
      error: (err: any) => {
        // Profile photo not found or not uploaded yet - this is okay
        if (err.status !== 404) {
          console.error('Error loading profile photo:', err);
        }
      }
    });
  }

  // Signature upload
  private initSignatureCanvas() {
    if (!this.signatureCanvas) return;
    const canvas = this.signatureCanvas.nativeElement;
    const context = canvas.getContext('2d');
    if (!context) return;

    this.signatureCtx = context;
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    context.scale(dpr, dpr);
    context.lineWidth = 2;
    context.lineCap = 'round';
    context.strokeStyle = '#111827';
  }

  onSignatureCanvasDown(event: MouseEvent | TouchEvent) {
    if (!this.signatureCtx) return;
    this.isDrawingSignature = true;
    const { x, y } = this.getCanvasCoordinates(event);
    this.lastSigX = x;
    this.lastSigY = y;
  }

  onSignatureCanvasMove(event: MouseEvent | TouchEvent) {
    if (!this.signatureCtx || !this.isDrawingSignature) return;
    const { x, y } = this.getCanvasCoordinates(event);
    if (this.lastSigX === null || this.lastSigY === null) {
      this.lastSigX = x;
      this.lastSigY = y;
      return;
    }
    this.signatureCtx.beginPath();
    this.signatureCtx.moveTo(this.lastSigX, this.lastSigY);
    this.signatureCtx.lineTo(x, y);
    this.signatureCtx.stroke();
    this.lastSigX = x;
    this.lastSigY = y;
  }

  onSignatureCanvasUp() {
    this.isDrawingSignature = false;
    this.lastSigX = null;
    this.lastSigY = null;
  }

  clearSignatureCanvas() {
    if (!this.signatureCtx || !this.signatureCanvas) return;
    const canvas = this.signatureCanvas.nativeElement;
    this.signatureCtx.clearRect(0, 0, canvas.width, canvas.height);
    this.signature = null;
    this.signatureUrl = null;
  }

  private getCanvasCoordinates(event: MouseEvent | TouchEvent): { x: number; y: number } {
    if (!this.signatureCanvas) return { x: 0, y: 0 };
    const canvas = this.signatureCanvas.nativeElement;
    const rect = canvas.getBoundingClientRect();
    let clientX: number;
    let clientY: number;

    if (event instanceof MouseEvent) {
      clientX = event.clientX;
      clientY = event.clientY;
    } else {
      const touch = event.touches[0] || event.changedTouches[0];
      clientX = touch.clientX;
      clientY = touch.clientY;
    }

    return {
      x: clientX - rect.left,
      y: clientY - rect.top
    };
  }

  async saveCanvasSignatureAndUpload() {
    if (!this.signatureCanvas) {
      this.alertService.userError('Error', 'Signature pad not available. Please reload the page.');
      return;
    }

    const canvas = this.signatureCanvas.nativeElement;
    const blob: Blob | null = await new Promise(resolve => canvas.toBlob(b => resolve(b), 'image/png'));
    if (!blob) {
      this.alertService.userError('Error', 'Could not capture signature. Please try again.');
      return;
    }

    // Convert canvas blob into a File so it flows through existing upload pipeline
    const file = new File([blob], 'signature-canvas.png', { type: 'image/png' });
    this.signature = file;

    // Preview
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.signatureUrl = e.target.result;
    };
    reader.readAsDataURL(file);

    // Reuse existing upload flow
    this.uploadSignature();
  }

  onSignatureSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.alertService.userError('File Too Large', 'Signature must be less than 5MB');
        // Reset file input
        const fileInput = event.target as HTMLInputElement;
        if (fileInput) {
          fileInput.value = '';
        }
        return;
      }
      
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!validTypes.includes(file.type)) {
        this.alertService.userError('Invalid File Type', 'Signature must be JPEG, PNG, or PDF');
        // Reset file input
        const fileInput = event.target as HTMLInputElement;
        if (fileInput) {
          fileInput.value = '';
        }
        return;
      }
      
      // Store the file
      this.signature = file;
      console.log('Signature selected:', file.name, file.size, 'bytes');
      
      // Preview image
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.signatureUrl = e.target.result;
      };
      reader.onerror = () => {
        console.error('Error reading file for preview');
        this.alertService.userError('Error', 'Failed to read file for preview');
      };
      reader.readAsDataURL(file);
    } else {
      // No file selected - clear previous selection
      this.signature = null;
      console.log('No signature file selected');
    }
  }

  uploadSignature() {
    // Check if file is selected
    if (!this.signature) {
      // Try to get file from input element as fallback
      const fileInput = document.getElementById('signatureInput') as HTMLInputElement;
      if (fileInput && fileInput.files && fileInput.files.length > 0) {
        const file = fileInput.files[0];
        // Validate file
        if (file.size > 5 * 1024 * 1024) {
          this.alertService.userError('File Too Large', 'Signature must be less than 5MB');
          fileInput.value = '';
          return;
        }
        const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
        if (!validTypes.includes(file.type)) {
          this.alertService.userError('Invalid File Type', 'Signature must be JPEG, PNG, or PDF');
          fileInput.value = '';
          return;
        }
        this.signature = file;
        console.log('Signature file recovered from input element:', file.name);
      } else {
        this.alertService.userError('Error', 'Please select a signature file first by clicking "Choose Signature"');
        return;
      }
    }

    if (!this.currentUserId) {
      this.alertService.userError('Error', 'User ID not found. Please refresh the page.');
      return;
    }

    this.uploadingSignature = true;
    const formData = new FormData();
    formData.append('signature', this.signature);

    console.log('Uploading signature:', this.signature.name, this.signature.size, 'bytes');

    this.http.post(`${environment.apiBaseUrl}/api/users/${this.currentUserId}/upload-signature`, formData).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.userSuccess('Success', 'Signature uploaded successfully! Waiting for admin approval.');
          this.signature = null;
          // Reset the file input element
          const fileInput = document.getElementById('signatureInput') as HTMLInputElement;
          if (fileInput) {
            fileInput.value = '';
          }
          // Clear preview if showing
          if (this.showSignature) {
            this.signatureUrl = null;
            this.showSignature = false;
          }
          this.loadSignature();
          this.loadUserProfile(); // Reload to get updated signature status
        } else {
          this.alertService.userError('Upload Failed', response.message || 'Failed to upload signature');
        }
        this.uploadingSignature = false;
      },
      error: (err: any) => {
        console.error('Error uploading signature:', err);
        this.alertService.userError('Upload Failed', err.error?.message || 'Failed to upload signature');
        this.uploadingSignature = false;
      }
    });
  }

  loadSignature() {
    if (!this.currentUserId) return;
    
    // Load signature status only (not the image)
    this.http.get(`${environment.apiBaseUrl}/api/users/${this.currentUserId}`).subscribe({
      next: (userData: any) => {
        this.signatureStatus = userData.signatureStatus || '';
        this.signatureRejectionReason = userData.signatureRejectionReason || '';
        // Don't load image automatically - user must click "Show Signature"
      },
      error: (err: any) => {
        console.error('Error loading signature status:', err);
      }
    });
  }

  loadSignatureImage() {
    if (!this.currentUserId) return;
    
    this.http.get(`${environment.apiBaseUrl}/api/users/${this.currentUserId}/signature`, { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.signatureUrl = e.target.result;
        };
        reader.readAsDataURL(blob);
      },
      error: (err: any) => {
        // Signature not found or not uploaded yet - this is okay
        if (err.status !== 404) {
          console.error('Error loading signature:', err);
        }
        this.signatureUrl = null;
      }
    });
  }

  getSignatureStatusClass(): string {
    if (this.signatureStatus === 'APPROVED') {
      return 'status-approved';
    } else if (this.signatureStatus === 'PENDING') {
      return 'status-pending';
    } else if (this.signatureStatus === 'REJECTED') {
      return 'status-rejected';
    }
    return 'status-not-uploaded';
  }

  getSignatureStatusText(): string {
    if (this.signatureStatus === 'APPROVED') {
      return 'Approved';
    } else if (this.signatureStatus === 'PENDING') {
      return 'Pending Approval';
    } else if (this.signatureStatus === 'REJECTED') {
      return 'Rejected';
    }
    return 'Not Uploaded';
  }
}
