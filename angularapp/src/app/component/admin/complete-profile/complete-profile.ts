import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complete-profile.html',
  styleUrls: ['./complete-profile.css']
})
export class CompleteProfile implements OnInit {
  admin: any = null;
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
      this.alertService.error('Error', 'Admin session not found. Please login again.');
      this.router.navigate(['/admin/login']);
    }
  }

  loadProfile() {
    if (!this.adminEmail) return;

    this.isLoading = true;
    this.http.get(`${environment.apiUrl}/admins/profile/${this.adminEmail}`).subscribe({
      next: (admin: any) => {
        this.admin = admin;
        this.profileForm = {
          name: admin.name || '',
          email: admin.email || '',
          employeeId: admin.employeeId || '',
          address: admin.address || '',
          aadharNumber: admin.aadharNumber || '',
          mobileNumber: admin.mobileNumber || '',
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

    // Update profile and mark as complete
    this.http.put(`${environment.apiUrl}/admins/profile/${this.adminEmail}`, this.profileForm).subscribe({
      next: (response: any) => {
        if (response.success) {
          // Mark profile as complete
          this.http.put(`${environment.apiUrl}/admins/profile-complete/${this.adminEmail}`, {}).subscribe({
            next: (completeResponse: any) => {
              if (completeResponse.success) {
                this.alertService.success('Success', 'Profile completed successfully! Redirecting to dashboard...');
                
                // Update session storage
                if (isPlatformBrowser(this.platformId)) {
                  sessionStorage.setItem('admin', JSON.stringify(completeResponse.admin));
                }
                
                // Redirect to dashboard after a short delay
                setTimeout(() => {
                  this.router.navigate(['/admin/dashboard']);
                }, 1500);
              } else {
                this.alertService.error('Error', 'Failed to mark profile as complete');
                this.isSaving = false;
              }
            },
            error: (err: any) => {
              console.error('Error marking profile complete:', err);
              this.alertService.error('Error', 'Failed to mark profile as complete');
              this.isSaving = false;
            }
          });
        } else {
          this.alertService.error('Error', response.message || 'Failed to update profile');
          this.isSaving = false;
        }
      },
      error: (err: any) => {
        console.error('Error updating profile:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update profile');
        this.isSaving = false;
      }
    });
  }

  logout() {
    this.alertService.logoutSuccess();
    sessionStorage.removeItem('admin');
    sessionStorage.removeItem('userRole');
    this.router.navigate(['/admin/login']);
  }
}

