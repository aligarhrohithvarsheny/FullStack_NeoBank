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
  loading: boolean = false;
  searchTerm: string = '';
  showAddUserForm: boolean = false;
  newUser: any = {};

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadAllUsers();
  }

  loadAllUsers() {
    this.loading = true;
    this.http.get<FullUser[]>(`${environment.apiUrl}/users/admin/all`).subscribe({
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
    this.editingUser = JSON.parse(JSON.stringify(user)); // Deep copy
    this.selectedUser = null;
  }

  cancelEdit() {
    this.editingUser = null;
    this.selectedUser = null;
  }

  saveUser() {
    if (!this.editingUser) return;

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
    this.http.put(`${environment.apiUrl}/users/admin/update-full/${this.editingUser.id}`, updateData).subscribe({
      next: (response: any) => {
        if (response.success) {
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

  deleteUser(user: FullUser) {
    if (!confirm(`Are you sure you want to delete user: ${user.username} (${user.email})? This action cannot be undone!`)) {
      return;
    }

    this.loading = true;
    this.http.delete(`${environment.apiUrl}/users/admin/delete/${user.id}`).subscribe({
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
    this.router.navigate(['/admin/dashboard']);
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
    this.http.post(`${environment.apiUrl}/users/create`, this.newUser).subscribe({
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

