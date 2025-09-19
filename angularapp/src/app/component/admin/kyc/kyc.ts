import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common'; 
import { FormsModule } from '@angular/forms';

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
  selector: 'app-kyc',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './kyc.html',
  styleUrls: ['./kyc.css']
})
export class Kyc implements OnInit {
  kycRequests: KycRequest[] = [];
  filteredRequests: KycRequest[] = [];
  statusFilter: string = 'All';
  searchTerm: string = '';

  // Computed properties for statistics
  get totalRequests(): number {
    return this.kycRequests.length;
  }

  get pendingRequests(): number {
    return this.kycRequests.filter(r => r.status === 'Pending').length;
  }

  get approvedRequests(): number {
    return this.kycRequests.filter(r => r.status === 'Approved').length;
  }

  get rejectedRequests(): number {
    return this.kycRequests.filter(r => r.status === 'Rejected').length;
  }

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    this.loadKycRequests();
  }

  loadKycRequests() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const savedRequests = localStorage.getItem('kyc_requests');
    if (savedRequests) {
      this.kycRequests = JSON.parse(savedRequests);
    } else {
      // Add some sample data for demonstration
      this.kycRequests = [
        {
          id: 'KYC_SAMPLE_1',
          userId: 'ACC001',
          userName: 'John Doe',
          userEmail: 'john.doe@example.com',
          userAccountNumber: 'ACC001',
          panNumber: 'ABCDE1234F',
          name: 'John Doe',
          status: 'Pending',
          submittedDate: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
        },
        {
          id: 'KYC_SAMPLE_2',
          userId: 'ACC002',
          userName: 'Jane Smith',
          userEmail: 'jane.smith@example.com',
          userAccountNumber: 'ACC002',
          panNumber: 'FGHIJ5678K',
          name: 'Jane Smith',
          status: 'Approved',
          submittedDate: new Date(Date.now() - 172800000).toISOString(), // 2 days ago
          approvedDate: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
          approvedBy: 'Admin'
        }
      ];
      this.saveKycRequests();
    }
    
    this.applyFilters();
  }

  saveKycRequests() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('kyc_requests', JSON.stringify(this.kycRequests));
  }

  applyFilters() {
    this.filteredRequests = this.kycRequests.filter(request => {
      const matchesStatus = this.statusFilter === 'All' || request.status === this.statusFilter;
      const matchesSearch = !this.searchTerm || 
        request.userName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        request.userEmail.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        request.panNumber.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        request.userAccountNumber.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      return matchesStatus && matchesSearch;
    });
  }

  onStatusFilterChange() {
    this.applyFilters();
  }

  onSearchChange() {
    this.applyFilters();
  }

  approve(request: KycRequest) {
    if (confirm(`Approve KYC for ${request.userName} (${request.userAccountNumber})?`)) {
      request.status = 'Approved';
      request.approvedDate = new Date().toISOString();
      request.approvedBy = 'Admin';
      
      // Update user profile with approved KYC information
      this.updateUserProfile(request);
      
      this.saveKycRequests();
      this.applyFilters();
      
      alert(`✅ KYC approved for ${request.userName}! Profile updated successfully.`);
    }
  }

  reject(request: KycRequest) {
    if (confirm(`Reject KYC for ${request.userName} (${request.userAccountNumber})?`)) {
      request.status = 'Rejected';
      request.approvedDate = new Date().toISOString();
      request.approvedBy = 'Admin';
      
      this.saveKycRequests();
      this.applyFilters();
      
      alert(`❌ KYC rejected for ${request.userName}. User can resubmit with correct information.`);
    }
  }

  updateUserProfile(kycRequest: KycRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Update user profile with approved KYC information
    const userProfileKey = 'user_profile';
    const savedProfile = localStorage.getItem(userProfileKey);
    
    if (savedProfile) {
      const userProfile: UserProfile = JSON.parse(savedProfile);
      
      // Only update if this is the same user
      if (userProfile.accountNumber === kycRequest.userAccountNumber) {
        userProfile.name = kycRequest.name;
        userProfile.pan = kycRequest.panNumber;
        
        localStorage.setItem(userProfileKey, JSON.stringify(userProfile));
        console.log(`Updated profile for user ${kycRequest.userAccountNumber}`);
      }
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Approved': return 'approved';
      case 'Rejected': return 'rejected';
      case 'Pending': return 'pending';
      default: return '';
    }
  }

  trackByRequestId(index: number, request: KycRequest): string {
    return request.id;
  }

  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }
}
