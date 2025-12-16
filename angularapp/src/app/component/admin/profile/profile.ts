import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';
import { Admin } from '../../../model/admin/admin-module';

@Component({
  selector: 'app-admin-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css']
})
export class AdminProfile implements OnInit {
  admin: Admin | null = null;
  editMode: boolean = false;
  isLoading: boolean = false;
  isSaving: boolean = false;
  adminEmail: string = '';

  // Form data
  profileForm: any = {
    name: '',
    email: '',
    employeeId: '',
    address: '',
    aadharNumber: '',
    mobileNumber: '',
    role: '',
    pan: '',
    qualifications: ''
  };

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private router: Router,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadAdminData();
    }
  }

  loadAdminData() {
    // Get admin email from session storage
    const adminData = sessionStorage.getItem('admin');
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        this.adminEmail = admin.email || admin.username;
        this.loadProfile();
      } catch (e) {
        console.error('Error parsing admin data:', e);
        this.alertService.error('Error', 'Failed to load admin data');
        this.router.navigate(['/admin/login']);
      }
    } else {
      // Try to get from login response
      this.alertService.error('Error', 'Admin session not found. Please login again.');
      this.router.navigate(['/admin/login']);
    }
  }

  loadProfile() {
    if (!this.adminEmail) return;

    this.isLoading = true;
    this.http.get<Admin>(`${environment.apiBaseUrl}/admins/profile/${this.adminEmail}`).subscribe({
      next: (admin: Admin) => {
        this.admin = admin;
        this.profileForm = {
          name: admin.name || '',
          email: admin.email || '',
          employeeId: admin.employeeId || '',
          address: admin.address || '',
          aadharNumber: admin.aadharNumber || '',
          mobileNumber: admin.mobileNumber || '',
          role: admin.role || '',
          pan: admin.pan || '',
          qualifications: admin.qualifications || ''
        };
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error loading admin profile:', err);
        this.alertService.error('Error', 'Failed to load admin profile');
        this.isLoading = false;
      }
    });
  }

  toggleEditMode() {
    this.editMode = !this.editMode;
    if (!this.editMode) {
      // Reset form to original values
      if (this.admin) {
        this.profileForm = {
          name: this.admin.name || '',
          email: this.admin.email || '',
          employeeId: this.admin.employeeId || '',
          address: this.admin.address || '',
          aadharNumber: this.admin.aadharNumber || '',
          mobileNumber: this.admin.mobileNumber || '',
          role: this.admin.role || '',
          pan: this.admin.pan || '',
          qualifications: this.admin.qualifications || ''
        };
      }
    }
  }

  saveProfile() {
    if (!this.adminEmail) return;

    // Validate required fields
    if (!this.profileForm.name || !this.profileForm.email) {
      this.alertService.error('Validation Error', 'Name and Email are required fields');
      return;
    }

    // Validate Aadhar format (12 digits)
    if (this.profileForm.aadharNumber && 
        (this.profileForm.aadharNumber.length !== 12 || !/^\d+$/.test(this.profileForm.aadharNumber))) {
      this.alertService.error('Validation Error', 'Aadhar number must be 12 digits');
      return;
    }

    // Validate PAN format (10 characters)
    if (this.profileForm.pan && this.profileForm.pan.length !== 10) {
      this.alertService.error('Validation Error', 'PAN number must be 10 characters');
      return;
    }

    // Validate mobile number (10 digits)
    if (this.profileForm.mobileNumber && 
        (this.profileForm.mobileNumber.length !== 10 || !/^\d+$/.test(this.profileForm.mobileNumber))) {
      this.alertService.error('Validation Error', 'Mobile number must be 10 digits');
      return;
    }

    this.isSaving = true;

    this.http.put(`${environment.apiBaseUrl}/admins/profile/${this.adminEmail}`, this.profileForm).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Profile updated successfully!');
          this.admin = response.admin;
          this.editMode = false;
          
          // Update session storage
          if (isPlatformBrowser(this.platformId)) {
            sessionStorage.setItem('admin', JSON.stringify(response.admin));
          }
        } else {
          this.alertService.error('Error', response.message || 'Failed to update profile');
        }
        this.isSaving = false;
      },
      error: (err: any) => {
        console.error('Error updating profile:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update profile');
        this.isSaving = false;
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

  formatDate(dateString?: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}




