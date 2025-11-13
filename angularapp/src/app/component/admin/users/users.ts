import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

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

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrls: ['./users.css']
})
export class Users implements OnInit {
  constructor(
    private router: Router, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient
  ) {}

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
    
    console.log('Admin Users component initialized');
    
    // Load users from MySQL database
    this.loadUsersFromDatabase();
    
    // Load KYC requests from MySQL database
    this.loadKycRequestsFromDatabase();
    
    this.calculateNextAccountNumber();
    this.createCardsForExistingUsers();
  }

  // Method to refresh users data
  refreshUsersData() {
    console.log('Refreshing users data...');
    this.loadUsersFromDatabase();
    this.loadKycRequestsFromDatabase();
  }

  // Test method to create sample users if none exist
  createSampleUsers() {
    if (this.pendingUsers.length === 0) {
      console.log('No users found, creating sample users for testing...');
      
      const sampleUsers: PendingUser[] = [
        {
          id: '1',
          name: 'John Doe',
          email: 'john.doe@example.com',
          pan: 'ABCDE1234F',
          aadhar: '123456789012',
          income: 50000,
          balance: 0,
          status: 'PENDING',
          assignedAccountNumber: '',
          createdAt: new Date().toISOString(),
          dob: '1990-01-01',
          occupation: 'Software Engineer',
          mobile: '9876543210'
        },
        {
          id: '2',
          name: 'Jane Smith',
          email: 'jane.smith@example.com',
          pan: 'FGHIJ5678K',
          aadhar: '987654321098',
          income: 75000,
          balance: 25000,
          status: 'APPROVED',
          assignedAccountNumber: '100001',
          createdAt: new Date().toISOString(),
          dob: '1985-05-15',
          occupation: 'Manager',
          mobile: '9876543211'
        }
      ];
      
      this.pendingUsers = sampleUsers;
      this.saveUsers();
      console.log('Sample users created:', this.pendingUsers);
    }
  }

  // Load users from MySQL database
  loadUsersFromDatabase() {
    this.http.get(`${environment.apiUrl}/users`).subscribe({
      next: (users: any) => {
        console.log('Users loaded from MySQL:', users);
        console.log('First user structure:', users[0]);
        
        this.pendingUsers = users.map((user: any) => ({
          id: user.id?.toString() || '',
          name: user.account?.name || user.username || 'Unknown User',
          email: user.email || '',
          pan: user.account?.pan || '',
          aadhar: user.account?.aadharNumber || '',
          income: user.account?.income || 0,
          status: user.status || 'PENDING',
          assignedAccountNumber: user.accountNumber || '',
          createdAt: user.joinDate || user.createdAt || new Date().toISOString(),
          dob: user.account?.dob || '',
          occupation: user.account?.occupation || '',
          mobile: user.account?.phone || ''
        }));
        
        console.log('Mapped pending users:', this.pendingUsers);
        
        // Also save to localStorage as backup
        this.saveUsers();
      },
      error: (err: any) => {
        console.error('Error loading users from database:', err);
        console.error('Error details:', err);
        // Fallback to localStorage
        const raw = localStorage.getItem('admin_users');
        this.pendingUsers = raw ? JSON.parse(raw) : [];
        console.log('Using fallback data from localStorage:', this.pendingUsers);
      }
    });
  }

  // Load KYC requests from MySQL database
  loadKycRequestsFromDatabase() {
    this.http.get('${environment.apiUrl}/kyc/all?page=0&size=100').subscribe({
      next: (response: any) => {
        console.log('KYC requests loaded from MySQL:', response);
        if (response.content) {
          this.kycRequests = response.content;
        } else {
          this.kycRequests = response;
        }
        
        // Also save to localStorage as backup
        this.saveKycRequests();
      },
      error: (err: any) => {
        console.error('Error loading KYC requests from database:', err);
        // Fallback to localStorage
        const kycRaw = localStorage.getItem('kyc_requests');
        this.kycRequests = kycRaw ? JSON.parse(kycRaw) : [];
      }
    });
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
      
      // Save to MySQL database
      this.updateUserInDatabase(user);
      
      // Also save to localStorage as backup
      this.saveUsers();
    }
  }

  // Update user in MySQL database
  updateUserInDatabase(user: PendingUser) {
    // First, find the user in the database by email
    this.http.get(`${environment.apiUrl}/users/email/${user.email}`).subscribe({
      next: (dbUser: any) => {
        if (dbUser) {
          // Update the user with account number and status
          const updatedUser = {
            ...dbUser,
            accountNumber: user.assignedAccountNumber,
            status: 'APPROVED',
            account: {
              ...dbUser.account,
              accountNumber: user.assignedAccountNumber,
              status: 'ACTIVE'
            }
          };
          
          // Update user in database using the new approval endpoint
          this.http.put(`${environment.apiUrl}/users/approve/${dbUser.id}`, {}).subscribe({
            next: (response: any) => {
              console.log('User approved and saved to MySQL:', response);
              
              // Show approval notification
              this.showUserApprovalNotification(user, response);
              
              // Automatically create a card for the approved user
              this.createCardForUser(user);
            },
            error: (err: any) => {
              console.error('Error updating user in database:', err);
              alert('Failed to save user approval to database. Please try again.');
            }
          });
        } else {
          console.error('User not found in database:', user.email);
          alert('User not found in database. Please check the user data.');
        }
      },
      error: (err: any) => {
        console.error('Error finding user in database:', err);
        alert('Failed to find user in database. Please try again.');
      }
    });
  }

  // Create cards for existing approved users who don't have cards
  createCardsForExistingUsers() {
    const approvedUsers = this.pendingUsers.filter(user => user.status === 'APPROVED' && user.assignedAccountNumber);
    
    approvedUsers.forEach(user => {
      // Check if user already has a card by trying to get cards for their account
      this.http.get(`${environment.apiUrl}/cards/account/${user.assignedAccountNumber}`).subscribe({
        next: (cards: any) => {
          if (!cards || cards.length === 0) {
            // User doesn't have a card, create one
            this.createCardForUser(user);
          }
        },
        error: (err: any) => {
          // If error getting cards, assume no cards exist and create one
          this.createCardForUser(user);
        }
      });
    });
  }

  // Create card for approved user
  createCardForUser(user: PendingUser) {
    const cardNumber = '4' + Math.floor(100000000000000 + Math.random() * 900000000000000).toString();
    const cvv = Math.floor(100 + Math.random() * 900).toString();
    const expiryMonth = String(Math.floor(1 + Math.random() * 12)).padStart(2, '0');
    const expiryYear = String(new Date().getFullYear() + Math.floor(1 + Math.random() * 5));

    const newCard = {
      cardNumber: cardNumber,
      cardType: 'Visa Debit',
      cvv: cvv,
      userName: user.name,
      expiryDate: `${expiryMonth}/${expiryYear.slice(-2)}`,
      pin: '',
      blocked: false,
      deactivated: false,
      pinSet: false,
      status: 'Active',
      accountNumber: user.assignedAccountNumber,
      userEmail: user.email
    };

    // Create card in MySQL database
    console.log('Creating card for user:', user.name, 'Account:', user.assignedAccountNumber);
    console.log('Card data being sent:', newCard);
    
    this.http.post('${environment.apiUrl}/cards', newCard).subscribe({
      next: (response: any) => {
        console.log('Card created automatically for approved user:', response);
        alert(`Card created successfully for ${user.name}!`);
      },
      error: (err: any) => {
        console.error('Error creating card for user:', err);
        console.error('Full error details:', err);
        alert(`Failed to create card for ${user.name}. Error: ${err.message || 'Unknown error'}`);
      }
    });
  }

  // Close account functionality
  closeAccount(user: PendingUser) {
    if (confirm(`Are you sure you want to close account ${user.assignedAccountNumber} for ${user.name}?`)) {
      user.status = 'CLOSED';
      
      // Update in MySQL database
      this.closeUserInDatabase(user);
      
      // Also save to localStorage as backup
      this.saveUsers();
    }
  }

  // Close user account in MySQL database
  closeUserInDatabase(user: PendingUser) {
    this.http.get(`${environment.apiUrl}/users/email/${user.email}`).subscribe({
      next: (dbUser: any) => {
        if (dbUser) {
          const updatedUser = {
            ...dbUser,
            status: 'CLOSED',
            account: {
              ...dbUser.account,
              status: 'CLOSED'
            }
          };
          
          this.http.put(`${environment.apiUrl}/users/update/${dbUser.id}`, updatedUser).subscribe({
            next: (response: any) => {
              console.log('User account closed and saved to MySQL:', response);
            },
            error: (err: any) => {
              console.error('Error closing user account in database:', err);
              alert('Failed to close account in database. Please try again.');
            }
          });
        }
      },
      error: (err: any) => {
        console.error('Error finding user for closure:', err);
        alert('Failed to find user for account closure. Please try again.');
      }
    });
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

  // Manual card creation for testing
  createCardManually() {
    const testUser = this.pendingUsers.find(u => u.status === 'APPROVED' && u.assignedAccountNumber);
    if (testUser) {
      this.createCardForUser(testUser);
    } else {
      alert('No approved users found to create card for');
    }
  }

  // Direct card creation for any user
  createCardDirectly() {
    const userName = prompt('Enter user name:');
    const accountNumber = prompt('Enter account number:');
    const userEmail = prompt('Enter user email:');
    
    if (userName && accountNumber && userEmail) {
      const testUser: PendingUser = {
        id: 'TEMP_' + Date.now(),
        name: userName,
        email: userEmail,
        pan: 'TEMP1234A',
        aadhar: '123456789012',
        income: 50000,
        status: 'APPROVED',
        assignedAccountNumber: accountNumber,
        dob: '1990-01-01',
        occupation: 'Employee',
        mobile: '9999999999',
        balance: 0,
        createdAt: new Date().toISOString()
      };
      this.createCardForUser(testUser);
    }
  }

  // Create card for existing approved user by email
  createCardForExistingUser() {
    const userEmail = prompt('Enter user email to create card for:');
    if (userEmail) {
      // Find user in the pending users list
      const user = this.pendingUsers.find(u => u.email === userEmail && u.status === 'APPROVED');
      if (user) {
        this.createCardForUser(user);
      } else {
        alert('User not found or not approved. Please check the email.');
      }
    }
  }

  saveKycRequests() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('kyc_requests', JSON.stringify(this.kycRequests));
  }

  // Show user approval notification with detailed information
  showUserApprovalNotification(user: PendingUser, response: any) {
    const notificationMessage = `
🎉 USER ACCOUNT APPROVED! 🎉

👤 User: ${user.name}
📧 Email: ${user.email}
🏦 Account Number: ${user.assignedAccountNumber}
💰 Initial Balance: ₹0.00
📋 Status: APPROVED
📅 Approved: ${new Date().toLocaleString()}

✅ Account has been created successfully!
✅ User can now login with their credentials!
✅ Card will be automatically created!
✅ User can access all banking features!

🔐 Login Details:
📧 Email: ${user.email}
🔑 Password: [User's chosen password]
    `;
    
    alert(notificationMessage);
    
    // Also log to console for debugging
    console.log('=== USER APPROVAL NOTIFICATION ===');
    console.log('User:', user.name);
    console.log('Email:', user.email);
    console.log('Account Number:', user.assignedAccountNumber);
    console.log('Status: APPROVED');
    console.log('==================================');
  }
}
