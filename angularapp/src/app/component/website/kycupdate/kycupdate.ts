import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';

interface KycRequest {
  id: string;
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
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}
  
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
    
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      this.userProfile = JSON.parse(savedProfile);
      this.name = this.userProfile?.name || '';
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
      id: 'KYC_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9),
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

    // Save KYC request to localStorage
    this.saveKycRequest(kycRequest);
    
    this.kycRequest = kycRequest;
    this.status = 'Pending Approval';
    this.isRequested = true;
    
    alert('KYC request submitted successfully! Admin will review your application.');
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
