import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common'; 
import { FormsModule } from '@angular/forms';
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

  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}

  ngOnInit() {
    this.loadKycRequests();
  }

  loadKycRequests() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load KYC requests from MySQL database
    console.log('Loading KYC requests from MySQL database...');
    this.http.get('http://localhost:8080/api/kyc/all?page=0&size=100').subscribe({
      next: (response: any) => {
        console.log('KYC requests loaded from MySQL:', response);
        console.log('Response type:', typeof response);
        console.log('Response length:', response?.length || 'N/A');
        if (response.content) {
          this.kycRequests = response.content.map((kyc: any) => ({
            id: kyc.id.toString(),
            userId: kyc.userId,
            userName: kyc.userName,
            userEmail: kyc.userEmail,
            userAccountNumber: kyc.userAccountNumber,
            panNumber: kyc.panNumber,
            name: kyc.name,
            status: kyc.status,
            submittedDate: kyc.submittedDate,
            approvedDate: kyc.approvedDate,
            approvedBy: kyc.approvedBy
          }));
        } else {
          this.kycRequests = response.map((kyc: any) => ({
            id: kyc.id.toString(),
            userId: kyc.userId,
            userName: kyc.userName,
            userEmail: kyc.userEmail,
            userAccountNumber: kyc.userAccountNumber,
            panNumber: kyc.panNumber,
            name: kyc.name,
            status: kyc.status,
            submittedDate: kyc.submittedDate,
            approvedDate: kyc.approvedDate,
            approvedBy: kyc.approvedBy
          }));
        }
        
        // Also save to localStorage as backup
        this.saveKycRequests();
        this.applyFilters();
      },
      error: (err: any) => {
        console.error('Error loading KYC requests from database:', err);
        // Fallback to localStorage
        const savedRequests = localStorage.getItem('kyc_requests');
        if (savedRequests) {
          this.kycRequests = JSON.parse(savedRequests);
        } else {
          // No KYC requests found in database - show empty list
          this.kycRequests = [];
          console.log('No KYC requests found in database');
        }
    
    this.applyFilters();
      }
    });
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
      // Update KYC request in MySQL database
      this.http.put(`http://localhost:8080/api/kyc/approve/${request.id}?adminName=Admin`, {}).subscribe({
        next: (response: any) => {
          console.log('KYC approved in MySQL:', response);
          
          request.status = 'Approved';
          request.approvedDate = new Date().toISOString();
          request.approvedBy = 'Admin';
          
          // Update user profile with approved KYC information
          this.updateUserProfile(request);
          
          this.saveKycRequests(); // Also save to localStorage as backup
          this.applyFilters();
          
          alert(`✅ KYC approved for ${request.userName}! User details have been updated across all systems (loans, cards, transactions).`);
        },
        error: (err: any) => {
          console.error('Error approving KYC:', err);
          alert('Failed to approve KYC. Please try again.');
        }
      });
    }
  }

  reject(request: KycRequest) {
    if (confirm(`Reject KYC for ${request.userName} (${request.userAccountNumber})?`)) {
      // Update KYC request in MySQL database
      this.http.put(`http://localhost:8080/api/kyc/reject/${request.id}?adminName=Admin`, {}).subscribe({
        next: (response: any) => {
          console.log('KYC rejected in MySQL:', response);
          
          request.status = 'Rejected';
          request.approvedDate = new Date().toISOString();
          request.approvedBy = 'Admin';
          
          this.saveKycRequests(); // Also save to localStorage as backup
          this.applyFilters();
          
          alert(`❌ KYC rejected for ${request.userName}. User can resubmit with correct information.`);
        },
        error: (err: any) => {
          console.error('Error rejecting KYC:', err);
          alert('Failed to reject KYC. Please try again.');
        }
      });
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
    return request.id || `kyc-${index}`;
  }

  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }
}
