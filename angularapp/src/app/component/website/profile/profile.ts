import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';
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
export class Profile implements OnInit {
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
    this.http.get(`${environment.apiBaseUrl}/users/${this.currentUserId}/passbook`, { 
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
      const user = JSON.parse(currentUser);
      console.log('Profile component - parsed user data:', user);
      this.currentUserId = user.id;
      
      // Load full user profile from MySQL database
      this.http.get(`${environment.apiBaseUrl}/users/${user.id}`).subscribe({
        next: (userData: any) => {
          console.log('User profile loaded from MySQL:', userData);
          this.userProfile = {
            name: userData.account?.name || userData.username || 'User',
            email: userData.email || 'user@example.com',
            accountNumber: userData.accountNumber || 'ACC001',
            phoneNumber: userData.account?.phone || userData.phoneNumber || '',
            address: userData.account?.address || userData.address || '',
            dateOfBirth: userData.account?.dob || userData.dateOfBirth || '',
            accountType: userData.account?.accountType || 'Savings Account',
            joinDate: userData.createdAt ? new Date(userData.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
            pan: userData.account?.pan || userData.pan || '',
            aadhar: userData.account?.aadharNumber || userData.aadhar || '',
            occupation: userData.account?.occupation || userData.occupation || '',
            income: userData.account?.income || userData.income || 0,
            ifscCode: 'NEO0008648',
            aadharVerified: userData.account?.aadharVerified || false,
            aadharVerificationStatus: userData.account?.aadharVerificationStatus || 'PENDING',
            aadharVerifiedDate: userData.account?.aadharVerifiedDate || ''
          };
          
          // Load current balance from MySQL
          this.loadCurrentBalanceFromMySQL();
          // Load Aadhaar verification status
          this.loadAadharVerificationStatus();
          // Load profile photo and signature status (not image)
          this.loadProfilePhoto();
          this.loadSignature();
        },
        error: (err: any) => {
          console.error('Error loading user profile from MySQL:', err);
          // Use fallback profile instead of showing error
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
        }
      });
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
          // No session found - create default profile to avoid errors
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

  loadCurrentBalanceFromMySQL() {
    if (!this.userProfile.accountNumber) {
      this.loading = false;
      return;
    }
    
    // Load current balance from MySQL database
    this.http.get(`${environment.apiBaseUrl}/accounts/balance/${this.userProfile.accountNumber}`).subscribe({
      next: (balanceData: any) => {
        console.log('Balance loaded from MySQL:', balanceData);
        this.currentBalance = balanceData.balance || 0;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading balance from MySQL:', err);
        // Fallback to user profile balance
        this.currentBalance = this.userProfile?.account?.balance || 0;
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
    this.http.put(`${environment.apiBaseUrl}/users/update-email/${this.currentUserId}`, { email: this.newEmail }).subscribe({
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
    
    this.http.get(`${environment.apiBaseUrl}/accounts/aadhar/status/${this.userProfile.accountNumber}`).subscribe({
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
    
    this.http.post(`${environment.apiBaseUrl}/accounts/aadhar/verify/${this.userProfile.accountNumber}`, {}).subscribe({
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

    this.http.post(`${environment.apiBaseUrl}/accounts/aadhar/callback?${params.toString()}`, {}).subscribe({
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
        return;
      }
      
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!validTypes.includes(file.type)) {
        this.alertService.userError('Invalid File Type', 'Profile photo must be JPEG, PNG, or PDF');
        return;
      }
      
      this.profilePhoto = file;
      // Preview image
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.profilePhotoUrl = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  uploadProfilePhoto() {
    if (!this.profilePhoto || !this.currentUserId) {
      this.alertService.userError('Error', 'Please select a profile photo');
      return;
    }

    this.uploadingPhoto = true;
    const formData = new FormData();
    formData.append('profilePhoto', this.profilePhoto);

    this.http.post(`${environment.apiBaseUrl}/users/${this.currentUserId}/upload-profile-photo`, formData).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.userSuccess('Success', 'Profile photo uploaded successfully!');
          this.profilePhoto = null;
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
    
    this.http.get(`${environment.apiBaseUrl}/users/${this.currentUserId}/profile-photo`, { responseType: 'blob' }).subscribe({
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
  onSignatureSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.alertService.userError('File Too Large', 'Signature must be less than 5MB');
        return;
      }
      
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!validTypes.includes(file.type)) {
        this.alertService.userError('Invalid File Type', 'Signature must be JPEG, PNG, or PDF');
        return;
      }
      
      this.signature = file;
      // Preview image
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.signatureUrl = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  uploadSignature() {
    if (!this.signature || !this.currentUserId) {
      this.alertService.userError('Error', 'Please select a signature file');
      return;
    }

    this.uploadingSignature = true;
    const formData = new FormData();
    formData.append('signature', this.signature);

    this.http.post(`${environment.apiBaseUrl}/users/${this.currentUserId}/upload-signature`, formData).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.userSuccess('Success', 'Signature uploaded successfully! Waiting for admin approval.');
          this.signature = null;
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
    this.http.get(`${environment.apiBaseUrl}/users/${this.currentUserId}`).subscribe({
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
    
    this.http.get(`${environment.apiBaseUrl}/users/${this.currentUserId}/signature`, { responseType: 'blob' }).subscribe({
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
