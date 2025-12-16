import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface FullUser {
  id: number;
  username: string;
  email: string;
  status: string;
  accountNumber: string;
  accountLocked: boolean;
  failedLoginAttempts: number;
  joinDate: string;
  account?: {
    id: number;
    name: string;
    phone: string;
    address: string;
    dob: string;
    age: number;
    occupation: string;
    income: number;
    accountType: string;
    balance: number;
    pan: string;
    aadharNumber: string;
    status: string;
    createdAt: string;
  };
  profilePhoto?: string;
  signatureStatus?: string;
}

@Component({
  selector: 'app-usercontrol',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usercontrol.html',
  styleUrls: ['./usercontrol.css']
})
export class UserControl implements OnInit {
  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient
  ) {}

  users: FullUser[] = [];
  filteredUsers: FullUser[] = [];
  selectedUser: FullUser | null = null;
  editingUser: FullUser | null = null;
  originalUser: FullUser | null = null; // Store original data for comparison
  loading: boolean = false;
  searchTerm: string = '';
  showAddUserForm: boolean = false;
  newUser: any = {};
  adminName: string = 'Admin';

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadAllUsers();
    // Get admin name from session storage
    const adminData = sessionStorage.getItem('admin');
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        this.adminName = admin.username || admin.name || 'Admin';
      } catch (e) {
        console.error('Error parsing admin data:', e);
      }
    }
  }

  loadAllUsers() {
    this.loading = true;
    this.http.get<FullUser[]>(`${environment.apiBaseUrl}/users/admin/all`).subscribe({
      next: (users) => {
        this.users = users;
        this.filteredUsers = users;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading users:', err);
        this.loading = false;
        alert('Failed to load users');
      }
    });
  }

  searchUsers() {
    if (!this.searchTerm.trim()) {
      this.filteredUsers = this.users;
      return;
    }
    
    const term = this.searchTerm.toLowerCase();
    this.filteredUsers = this.users.filter(user => 
      user.username?.toLowerCase().includes(term) ||
      user.email?.toLowerCase().includes(term) ||
      user.accountNumber?.toLowerCase().includes(term) ||
      user.account?.name?.toLowerCase().includes(term) ||
      user.account?.phone?.includes(term) ||
      user.account?.pan?.toLowerCase().includes(term) ||
      user.account?.aadharNumber?.includes(term)
    );
  }

  viewUser(user: FullUser) {
    this.selectedUser = { ...user };
    this.editingUser = null;
  }

  editUser(user: FullUser) {
    this.editingUser = JSON.parse(JSON.stringify(user)); // Deep copy for editing
    this.originalUser = JSON.parse(JSON.stringify(user)); // Deep copy for comparison
    this.selectedUser = null;
  }

  cancelEdit() {
    this.editingUser = null;
    this.originalUser = null;
    this.selectedUser = null;
  }

  saveUser() {
    if (!this.editingUser || !this.originalUser) return;

    // Track changes
    const changes: any[] = [];
    const timestamp = new Date().toISOString();

    // Compare user fields
    if (this.editingUser.username !== this.originalUser.username) {
      changes.push({
        field: 'Username',
        oldValue: this.originalUser.username,
        newValue: this.editingUser.username
      });
    }
    if (this.editingUser.email !== this.originalUser.email) {
      changes.push({
        field: 'Email',
        oldValue: this.originalUser.email,
        newValue: this.editingUser.email
      });
    }
    if (this.editingUser.status !== this.originalUser.status) {
      changes.push({
        field: 'User Status',
        oldValue: this.originalUser.status,
        newValue: this.editingUser.status
      });
    }
    if (this.editingUser.accountNumber !== this.originalUser.accountNumber) {
      changes.push({
        field: 'Account Number',
        oldValue: this.originalUser.accountNumber,
        newValue: this.editingUser.accountNumber
      });
    }
    if (this.editingUser.accountLocked !== this.originalUser.accountLocked) {
      changes.push({
        field: 'Account Locked',
        oldValue: this.originalUser.accountLocked ? 'Yes' : 'No',
        newValue: this.editingUser.accountLocked ? 'Yes' : 'No'
      });
    }
    if (this.editingUser.failedLoginAttempts !== this.originalUser.failedLoginAttempts) {
      changes.push({
        field: 'Failed Login Attempts',
        oldValue: this.originalUser.failedLoginAttempts?.toString() || '0',
        newValue: this.editingUser.failedLoginAttempts?.toString() || '0'
      });
    }

    // Compare account fields
    const originalAccount = this.originalUser.account;
    const editingAccount = this.editingUser.account;
    
    if (originalAccount && editingAccount) {
      const accountFields = [
        { key: 'name', label: 'Name' },
        { key: 'phone', label: 'Phone' },
        { key: 'address', label: 'Address' },
        { key: 'dob', label: 'Date of Birth' },
        { key: 'age', label: 'Age' },
        { key: 'occupation', label: 'Occupation' },
        { key: 'income', label: 'Income' },
        { key: 'accountType', label: 'Account Type' },
        { key: 'balance', label: 'Balance' },
        { key: 'pan', label: 'PAN' },
        { key: 'aadharNumber', label: 'Aadhar Number' },
        { key: 'status', label: 'Account Status' }
      ];

      accountFields.forEach(field => {
        const oldVal = originalAccount[field.key as keyof typeof originalAccount];
        const newVal = editingAccount[field.key as keyof typeof editingAccount];
        
        // Handle balance as number comparison
        if (field.key === 'balance') {
          const oldBalance = (oldVal as number) || 0;
          const newBalance = (newVal as number) || 0;
          if (oldBalance !== newBalance) {
            changes.push({
              field: field.label,
              oldValue: `₹${oldBalance.toFixed(2)}`,
              newValue: `₹${newBalance.toFixed(2)}`
            });
          }
        } else if (oldVal !== newVal) {
          changes.push({
            field: field.label,
            oldValue: oldVal?.toString() || 'N/A',
            newValue: newVal?.toString() || 'N/A'
          });
        }
      });
    }

    const updateData: any = {
      username: this.editingUser.username,
      email: this.editingUser.email,
      status: this.editingUser.status,
      accountNumber: this.editingUser.accountNumber,
      accountLocked: this.editingUser.accountLocked,
      failedLoginAttempts: this.editingUser.failedLoginAttempts
    };

    if (this.editingUser.account) {
      updateData.name = this.editingUser.account.name;
      updateData.phone = this.editingUser.account.phone;
      updateData.address = this.editingUser.account.address;
      updateData.dob = this.editingUser.account.dob;
      updateData.age = this.editingUser.account.age;
      updateData.occupation = this.editingUser.account.occupation;
      updateData.income = this.editingUser.account.income;
      updateData.accountType = this.editingUser.account.accountType;
      updateData.balance = this.editingUser.account.balance;
      updateData.pan = this.editingUser.account.pan;
      updateData.aadharNumber = this.editingUser.account.aadharNumber;
      updateData.accountStatus = this.editingUser.account.status;
    }

    this.loading = true;
    this.http.put(`${environment.apiBaseUrl}/users/admin/update-full/${this.editingUser.id}`, updateData).subscribe({
      next: (response: any) => {
        if (response.success) {
          // Save update history if there are changes
          if (changes.length > 0) {
            this.saveUpdateHistory(this.editingUser!, changes, timestamp);
          }
          alert('User updated successfully!');
          this.loadAllUsers();
          this.cancelEdit();
        } else {
          alert('Failed to update: ' + response.message);
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Error updating user:', err);
        alert('Failed to update user: ' + (err.error?.message || 'Unknown error'));
        this.loading = false;
      }
    });
  }

  saveUpdateHistory(user: FullUser, changes: any[], timestamp: string) {
    if (!isPlatformBrowser(this.platformId)) return;

    const updateHistory = {
      id: `UH${Date.now()}`,
      userId: user.id,
      userName: user.username,
      userEmail: user.email,
      accountNumber: user.accountNumber,
      adminName: this.adminName,
      timestamp: timestamp,
      changes: changes,
      changeCount: changes.length
    };

    // Save to localStorage
    const historyKey = 'adminUserUpdateHistory';
    const existingHistory = localStorage.getItem(historyKey);
    const history = existingHistory ? JSON.parse(existingHistory) : [];
    
    history.unshift(updateHistory);
    
    // Keep only last 1000 updates
    if (history.length > 1000) {
      history.splice(1000);
    }
    
    localStorage.setItem(historyKey, JSON.stringify(history));
    console.log('Update history saved:', updateHistory);
  }

  deleteUser(user: FullUser) {
    if (!confirm(`Are you sure you want to delete user: ${user.username} (${user.email})? This action cannot be undone!`)) {
      return;
    }

    this.loading = true;
    this.http.delete(`${environment.apiBaseUrl}/users/admin/delete/${user.id}`).subscribe({
      next: (response: any) => {
        if (response.success) {
          alert('User deleted successfully!');
          this.loadAllUsers();
          this.cancelEdit();
        } else {
          alert('Failed to delete: ' + response.message);
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Error deleting user:', err);
        alert('Failed to delete user: ' + (err.error?.message || 'Unknown error'));
        this.loading = false;
      }
    });
  }

  goBack() {
    // Check if accessed from manager dashboard
    const navigationSource = sessionStorage.getItem('navigationSource');
    if (navigationSource === 'MANAGER') {
      sessionStorage.removeItem('navigationSource');
      sessionStorage.removeItem('managerReturnPath');
      this.router.navigate(['/manager/dashboard']);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  toggleAddUserForm() {
    this.showAddUserForm = !this.showAddUserForm;
    if (this.showAddUserForm) {
      this.newUser = {
        username: '',
        email: '',
        password: '',
        status: 'PENDING',
        account: {
          name: '',
          phone: '',
          address: '',
          dob: '',
          age: 25,
          occupation: '',
          income: 0,
          accountType: 'Savings',
          balance: 0,
          pan: '',
          aadharNumber: ''
        }
      };
    }
  }

  createUser() {
    if (!this.newUser.username || !this.newUser.email || !this.newUser.password) {
      alert('Please fill in username, email, and password');
      return;
    }

    this.loading = true;
    this.http.post(`${environment.apiBaseUrl}/users/create`, this.newUser).subscribe({
      next: (response: any) => {
        if (response.success) {
          alert('User created successfully!');
          this.loadAllUsers();
          this.toggleAddUserForm();
        } else {
          alert('Failed to create user: ' + response.message);
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Error creating user:', err);
        alert('Failed to create user: ' + (err.error?.message || 'Unknown error'));
        this.loading = false;
      }
    });
  }
}

