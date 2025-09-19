import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface PendingUser {
  id: string;
  name: string;
  dob: string;
  occupation: string;
  income: number;
  pan: string;
  aadhar: string;
  mobile: string;
  email: string;
  balance: number;
  status: 'PENDING' | 'APPROVED' | 'CLOSED';
  assignedAccountNumber?: string;
  createdAt: string;
}

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

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrls: ['./users.css']
})
export class Users implements OnInit {
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object) {}

  pendingUsers: PendingUser[] = [];
  kycRequests: KycRequest[] = [];
  nextAccountNumber = 100001;

  // Computed properties for statistics
  get totalUsers(): number {
    return this.pendingUsers.length;
  }

  get pendingUsersCount(): number {
    return this.pendingUsers.filter(u => u.status === 'PENDING').length;
  }

  get approvedUsersCount(): number {
    return this.pendingUsers.filter(u => u.status === 'APPROVED').length;
  }

  get closedUsersCount(): number {
    return this.pendingUsers.filter(u => u.status === 'CLOSED').length;
  }

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Load from localStorage (mock backend)
    const raw = localStorage.getItem('admin_users');
    this.pendingUsers = raw ? JSON.parse(raw) : [];
    
    // Load KYC requests
    const kycRaw = localStorage.getItem('kyc_requests');
    this.kycRequests = kycRaw ? JSON.parse(kycRaw) : [];
    
    this.calculateNextAccountNumber();
  }

  calculateNextAccountNumber() {
    // Find the highest account number from existing users
    let maxAccountNumber = 100000;
    this.pendingUsers.forEach(user => {
      if (user.assignedAccountNumber) {
        const accountNum = parseInt(user.assignedAccountNumber);
        if (accountNum > maxAccountNumber) {
          maxAccountNumber = accountNum;
        }
      }
    });
    this.nextAccountNumber = maxAccountNumber + 1;
  }

  // Assign account number
  approveUser(user: PendingUser) {
    if (!user.assignedAccountNumber) {
      user.assignedAccountNumber = this.nextAccountNumber.toString();
      user.status = 'APPROVED';
      this.nextAccountNumber++; // Increment for next user
      this.saveUsers();
    }
  }

  // Close account functionality
  closeAccount(user: PendingUser) {
    if (confirm(`Are you sure you want to close account ${user.assignedAccountNumber} for ${user.name}?`)) {
      user.status = 'CLOSED';
      this.saveUsers();
    }
  }

  saveUsers() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('admin_users', JSON.stringify(this.pendingUsers));
  }

  getKycStatus(user: PendingUser): string {
    const kycRequest = this.kycRequests.find(req => 
      req.userAccountNumber === user.assignedAccountNumber || 
      req.userEmail === user.email
    );
    
    if (!kycRequest) {
      return 'Not Submitted';
    }
    
    return kycRequest.status;
  }

  getKycStatusClass(user: PendingUser): string {
    const status = this.getKycStatus(user);
    switch (status) {
      case 'Approved': return 'kyc-approved';
      case 'Pending': return 'kyc-pending';
      case 'Rejected': return 'kyc-rejected';
      default: return 'kyc-not-submitted';
    }
  }

  trackByUserId(index: number, user: PendingUser): string {
    return user.id;
  }

  goBack() {
    this.router.navigate(['admin/dashboard']);
  }
}
