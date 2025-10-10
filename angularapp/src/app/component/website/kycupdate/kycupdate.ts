import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface KycRequest {
  id?: string; // Make ID optional since backend auto-generates it
  userId: string;
  userName: string;
  userEmail: string;
  userAccountNumber: string;
  panNumber: string;
  name: string;
  status: 'Pending' | 'Approved' | 'Rejected';
  submittedDate: string;
  approvedDate?: string;
  approvedBy?: string;
}

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
}

@Component({
  selector: 'app-kycupdate',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './kycupdate.html',
  styleUrls: ['./kycupdate.css']
})
export class Kycupdate implements OnInit {
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}
  
  panNumber: string = '';
  name: string = '';
  status: string = 'Not Requested';
  isRequested: boolean = false;
  userProfile: UserProfile | null = null;
  kycRequest: KycRequest | null = null;

  ngOnInit() {
    this.loadUserProfile();
    this.loadExistingKycRequest();
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // First try to get user data from session storage (current logged-in user)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const userData = JSON.parse(currentUser);
      this.userProfile = {
        name: userData.name,
        email: userData.email,
        accountNumber: userData.accountNumber,
        accountType: 'Savings',
        joinDate: userData.loginTime
      };
      this.name = this.userProfile.name || '';
      console.log('Loaded user profile from session:', this.userProfile);
      return;
    }
    
    // Fallback to localStorage
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      this.userProfile = JSON.parse(savedProfile);
      this.name = this.userProfile?.name || '';
      console.log('Loaded user profile from localStorage:', this.userProfile);
    }
  }

  loadExistingKycRequest() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const kycRequests = localStorage.getItem('kyc_requests');
    if (kycRequests && this.userProfile) {
      const requests: KycRequest[] = JSON.parse(kycRequests);
      const userRequest = requests.find(req => req.userAccountNumber === this.userProfile?.accountNumber);
      
      if (userRequest) {
        this.kycRequest = userRequest;
        this.panNumber = userRequest.panNumber;
        this.name = userRequest.name;
        this.status = userRequest.status;
        this.isRequested = true;
      }
    }
  }

  requestToAdmin() {
    if (!this.panNumber || !this.name) {
      alert('Please enter both PAN number and name!');
      return;
    }

    if (!this.userProfile) {
      alert('User profile not found. Please create an account first.');
      return;
    }

    const kycRequest: KycRequest = {
      id: undefined, // Let backend auto-generate the ID
      userId: this.userProfile.accountNumber,
      userName: this.name,
      userEmail: this.userProfile.email,
      userAccountNumber: this.userProfile.accountNumber,
      panNumber: this.panNumber,
      name: this.name,
      status: 'Pending',
      submittedDate: new Date().toISOString(),
      approvedBy: undefined
    };

    // Submit KYC request to MySQL database
    this.http.post('http://localhost:8080/api/kyc/create', kycRequest).subscribe({
      next: (response: any) => {
        console.log('KYC request created in MySQL:', response);
        
        // Also save to localStorage as backup
        this.saveKycRequest(kycRequest);
        
        this.kycRequest = kycRequest;
        this.status = 'Pending Approval';
        this.isRequested = true;
        
        alert('KYC request submitted successfully! Admin will review your application.');
      },
      error: (err: any) => {
        console.error('Error creating KYC request:', err);
        alert('Failed to submit KYC request. Please try again.');
        
        // Fallback to localStorage
        this.saveKycRequest(kycRequest);
        this.kycRequest = kycRequest;
        this.status = 'Pending Approval';
        this.isRequested = true;
      }
    });
  }

  saveKycRequest(kycRequest: KycRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const existingRequests = localStorage.getItem('kyc_requests');
    let requests: KycRequest[] = existingRequests ? JSON.parse(existingRequests) : [];
    
    // Remove any existing request for this user
    requests = requests.filter(req => req.userAccountNumber !== kycRequest.userAccountNumber);
    
    // Add new request
    requests.push(kycRequest);
    
    localStorage.setItem('kyc_requests', JSON.stringify(requests));
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }
}
