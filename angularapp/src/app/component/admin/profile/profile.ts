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
  isUploadingPhoto: boolean = false;
  private photoCacheBuster: number = new Date().getTime();
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
    qualifications: '',
    dateOfJoining: '',
    branchAccountNumber: '',
    branchAccountName: '',
    branchAccountIfsc: '',
    salaryAccountNumber: ''
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
        const emailOk = admin && admin.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(admin.email);
        if (!emailOk) {
          console.error('Invalid admin.email found in sessionStorage, clearing session:', admin && admin.email);
          sessionStorage.removeItem('admin');
          sessionStorage.removeItem('userRole');
          this.alertService.error('Error', 'Admin session invalid. Please login again.');
          this.router.navigate(['/admin/login']);
          return;
        }
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
    this.http.get<Admin>(`${environment.apiBaseUrl}/api/admins/profile/${this.adminEmail}`).subscribe({
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
          qualifications: admin.qualifications || '',
          dateOfJoining: admin.dateOfJoining
            ? new Date(admin.dateOfJoining).toISOString().substring(0, 10)
            : (admin.createdAt ? new Date(admin.createdAt).toISOString().substring(0, 10) : ''),
          branchAccountNumber: admin.branchAccountNumber || '',
          branchAccountName: admin.branchAccountName || '',
          branchAccountIfsc: admin.branchAccountIfsc || '',
          salaryAccountNumber: (admin as any).salaryAccountNumber || ''
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
          qualifications: this.admin.qualifications || '',
          dateOfJoining: this.admin.dateOfJoining
            ? new Date(this.admin.dateOfJoining).toISOString().substring(0, 10)
            : (this.admin.createdAt ? new Date(this.admin.createdAt).toISOString().substring(0, 10) : ''),
          branchAccountNumber: this.admin.branchAccountNumber || '',
          branchAccountName: this.admin.branchAccountName || '',
          branchAccountIfsc: this.admin.branchAccountIfsc || '',
          salaryAccountNumber: (this.admin as any).salaryAccountNumber || ''
        };
      }
    }
  }

  getProfilePhotoUrl(): string | null {
    if (!this.admin || !this.admin.id) return null;
    if (!this.admin.profilePhotoPath) return null;
    return `${environment.apiBaseUrl}/api/admins/profile-photo/${this.admin.id}?t=${this.photoCacheBuster}`;
  }

  onProfilePhotoSelected(event: any) {
    if (!this.admin || !this.admin.id) {
      this.alertService.error('Error', 'Admin not loaded');
      return;
    }

    const input = event?.target as HTMLInputElement;
    const file = input?.files && input.files.length ? input.files[0] : null;
    if (!file) return;

    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (file.type && !allowed.includes(file.type)) {
      this.alertService.error('Validation Error', 'Please select a JPG, PNG, or WEBP image');
      input.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.alertService.error('Validation Error', 'Image must be less than 2MB');
      input.value = '';
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    this.isUploadingPhoto = true;

    this.http.post<any>(`${environment.apiBaseUrl}/api/admins/profile-photo/${this.admin.id}`, formData).subscribe({
      next: (resp: any) => {
        this.isUploadingPhoto = false;
        if (resp && resp.success && resp.admin) {
          this.alertService.success('Success', 'Profile photo uploaded successfully');
          this.admin = { ...(this.admin as any), ...resp.admin };
          this.photoCacheBuster = new Date().getTime();

          if (isPlatformBrowser(this.platformId) && this.admin) {
            sessionStorage.setItem('admin', JSON.stringify(this.admin));
          }
        } else {
          this.alertService.error('Error', resp?.message || 'Failed to upload profile photo');
        }
        input.value = '';
      },
      error: (err: any) => {
        console.error('Error uploading profile photo:', err);
        this.isUploadingPhoto = false;
        this.alertService.error('Error', err.error?.message || 'Failed to upload profile photo');
        input.value = '';
      }
    });
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

    const updatePayload = {
      ...this.profileForm,
      dateOfJoining: this.profileForm.dateOfJoining ? `${this.profileForm.dateOfJoining}T00:00:00` : null
    };

    this.http.put(`${environment.apiBaseUrl}/api/admins/profile/${this.adminEmail}`, updatePayload).subscribe({
      next: (response: any) => {
        if (response.success && response.pendingApproval) {
          // For fully completed profiles, subsequent edits go for manager approval.
          this.alertService.success(
            'Sent for Approval',
            response.message || 'Profile changes have been sent to manager for approval.'
          );
          this.editMode = false;
          // Keep showing existing admin details until manager approves.
        } else if (response.success) {
          this.alertService.success('Success', response.message || 'Profile updated successfully!');
          this.admin = response.admin;
          this.editMode = false;

          // Update session storage (validate email)
          if (isPlatformBrowser(this.platformId) && response.admin) {
            const serverAdmin = response.admin;
            const emailOk = serverAdmin.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(serverAdmin.email);
            if (emailOk) {
              sessionStorage.setItem('admin', JSON.stringify(serverAdmin));
            } else {
              console.error('Refusing to store invalid admin.email from server:', serverAdmin && serverAdmin.email);
            }
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




