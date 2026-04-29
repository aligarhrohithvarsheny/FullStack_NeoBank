import { Component, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { FaceAuthService } from '../../../service/face-auth.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [CommonModule, FormsModule],
  standalone: true
})


export class Login implements OnDestroy {
  @ViewChild('loginVideo') videoRef!: ElementRef<HTMLVideoElement>;

  email: string = '';
  password: string = '';
  selectedRole: string = 'ADMIN';
  errorMessage: string = '';
  isLoading: boolean = false;
  isCameraSupported: boolean = false;
  isAuthenticatingFace: boolean = false;
  hasFaceRegistered: boolean = false;
  checkingCredentials: boolean = false;
  showCamera: boolean = false;
  cameraStream: MediaStream | null = null;
  faceDetectionStatus: string = '';
  modelsLoading: boolean = false;

  constructor(
    private router: Router,
    private alertService: AlertService,
    private http: HttpClient,
    private faceAuthService: FaceAuthService
  ) {
    this.isCameraSupported = this.faceAuthService.isCameraSupported();
    if (!this.isCameraSupported) {
      this.errorMessage = 'Camera access is required for Face ID login. Please use a device with a camera.';
    }
  }

  ngOnDestroy() {
    this.stopCamera();
  }

  /**
   * Check if Face ID credentials exist for the entered email
   */
  checkFaceCredentials() {
    if (!this.email) {
      this.hasFaceRegistered = false;
      return;
    }

    if (this.selectedRole !== 'ADMIN') {
      this.hasFaceRegistered = false;
      return;
    }

    this.checkingCredentials = true;
    this.faceAuthService.getFaceStatus(this.email).subscribe({
      next: (response: any) => {
        this.checkingCredentials = false;
        if (response && response.success && response.registered) {
          this.hasFaceRegistered = true;
          this.errorMessage = '';
        } else {
          this.hasFaceRegistered = false;
          if (this.email) {
            this.errorMessage = 'No Face ID registered. Please register from the Admin Dashboard first.';
          }
        }
      },
      error: (err: any) => {
        this.checkingCredentials = false;
        this.hasFaceRegistered = false;
        if (this.email) {
          this.errorMessage = 'No Face ID registered. Please register from the Admin Dashboard.';
        }
      }
    });
  }

  stopCamera() {
    this.faceAuthService.stopCamera(this.cameraStream);
    this.cameraStream = null;
    this.showCamera = false;
    this.faceDetectionStatus = '';
  }

  onSubmit() {
    if (this.selectedRole === 'ADMIN') {
      this.errorMessage = 'Face ID is required for Admin login. Please use the "Login with Face ID" button.';
      return;
    }

    // Prevent multiple submissions
    if (this.isLoading) {
      return;
    }

    if (!this.email || !this.password) {
      this.errorMessage = 'Please enter email and password';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Try manager login (only for MANAGER role)
    this.http.post(`${environment.apiBaseUrl}/api/admins/login`, {
      email: this.email,
      password: this.password,
      role: this.selectedRole
    }).subscribe({
      next: (response: any) => {
        this.isLoading = false;
        if (response && response.success) {
          const role = response.role || response.admin?.role;
          
          // Check if selected role matches the user's actual role
          if (role === this.selectedRole) {
            this.errorMessage = '';
            // Store admin/manager data in session storage (validate email first)
            if (response.admin) {
              const serverAdmin = response.admin;
              const emailOk = serverAdmin.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(serverAdmin.email);
              if (emailOk) {
                sessionStorage.setItem('admin', JSON.stringify(serverAdmin));
                sessionStorage.setItem('userRole', role);
                // Store login timestamp for session duration tracking
                try {
                  sessionStorage.setItem('adminLoginTime', new Date().toISOString());
                } catch (e) {
                  console.error('Failed to store adminLoginTime in sessionStorage', e);
                }
              } else {
                console.error('Refusing to store invalid admin.email from server:', serverAdmin && serverAdmin.email);
              }
            }
            
            // Check if profile is complete (for ADMIN role only)
            const profileComplete = response.profileComplete !== undefined ? response.profileComplete : 
                                   (response.admin?.profileComplete !== undefined ? response.admin.profileComplete : true);
            
            if (role === 'MANAGER') {
              this.alertService.loginSuccess('Manager');
              this.router.navigate(['/manager/dashboard']);
            } else if (role === 'ADMIN') {
              // Check if admin profile is complete
              if (!profileComplete) {
                // Redirect to profile completion page
                this.alertService.success('Welcome', 'Please complete your profile to continue');
                this.router.navigate(['/admin/complete-profile']);
              } else {
                this.alertService.loginSuccess('Admin');
                this.router.navigate(['/admin/dashboard']);
              }
            } else {
              this.alertService.loginSuccess('Admin');
              this.router.navigate(['/admin/dashboard']);
            }
          } else {
            this.errorMessage = `Invalid ${this.selectedRole.toLowerCase()} credentials ❌`;
          }
        } else {
          this.errorMessage = `Invalid ${this.selectedRole.toLowerCase()} credentials ❌`;
        }
      },
      error: (err: any) => {
        // If admin login fails with 401 or 404, it means invalid credentials
        // Don't try user login - show error immediately
        this.isLoading = false;
        if (err.status === 401 || err.status === 404) {
          this.errorMessage = `Invalid ${this.selectedRole.toLowerCase()} credentials ❌`;
        } else if (err.status === 0) {
          this.errorMessage = 'Unable to connect to server. Please check your connection.';
        } else {
          this.errorMessage = `Login failed. Please try again.`;
        }
      }
    });
  }

  /**
   * Login with Face ID using laptop camera
   */
  async loginWithFaceID() {
    if (!this.email) {
      this.errorMessage = 'Please enter your email first';
      return;
    }

    if (!this.isCameraSupported) {
      this.errorMessage = 'Camera is not available on this device.';
      return;
    }

    if (this.selectedRole === 'ADMIN' && !this.hasFaceRegistered) {
      this.errorMessage = 'No Face ID registered. Please register from the Admin Dashboard first.';
      return;
    }

    this.isAuthenticatingFace = true;
    this.errorMessage = '';
    this.showCamera = true;
    this.faceDetectionStatus = 'Loading face recognition models...';
    this.modelsLoading = true;

    try {
      await this.faceAuthService.loadModels();
      this.modelsLoading = false;
      this.faceDetectionStatus = 'Starting camera...';

      // Wait for video element to be available
      await new Promise(resolve => setTimeout(resolve, 100));
      const video = this.videoRef.nativeElement;
      this.cameraStream = await this.faceAuthService.openCamera(video);

      this.faceDetectionStatus = 'Look at the camera with eyes open...';

      // Wait for camera to stabilize
      await new Promise(resolve => setTimeout(resolve, 1000));

      this.faceDetectionStatus = 'Detecting face... Keep eyes open and look at camera.';

      const result = await this.faceAuthService.captureMultipleDescriptors(
        video, 3, 600,
        (status: string) => { this.faceDetectionStatus = status; }
      );
      this.stopCamera();

      if (!result) {
        this.isAuthenticatingFace = false;
        this.errorMessage = 'Face not detected or eyes were closed. Please ensure your face is clearly visible with eyes open.';
        return;
      }

      if (result.captureCount < 2) {
        this.isAuthenticatingFace = false;
        this.errorMessage = 'Could not get enough clear face captures. Ensure good lighting and keep eyes open.';
        return;
      }

      this.faceDetectionStatus = 'Verifying face identity...';

      this.faceAuthService.verifyFace(this.email, result.descriptor).subscribe({
        next: (response: any) => {
          this.isAuthenticatingFace = false;
          if (response && response.success) {
            const role = response.role || response.admin?.role;

            if (response.admin) {
              const serverAdmin = response.admin;
              const emailOk = serverAdmin.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(serverAdmin.email);
              if (emailOk) {
                sessionStorage.setItem('admin', JSON.stringify(serverAdmin));
                sessionStorage.setItem('userRole', role);
                try {
                  sessionStorage.setItem('adminLoginTime', new Date().toISOString());
                } catch (e) {
                  console.error('Failed to store adminLoginTime in sessionStorage', e);
                }
              }
            }

            const profileComplete = response.profileComplete !== undefined ? response.profileComplete :
                                   (response.admin?.profileComplete !== undefined ? response.admin.profileComplete : true);

            if (role === 'MANAGER') {
              this.alertService.loginSuccess('Manager');
              this.router.navigate(['/manager/dashboard']);
            } else if (role === 'ADMIN') {
              if (!profileComplete) {
                this.alertService.success('Welcome', 'Please complete your profile to continue');
                this.router.navigate(['/admin/complete-profile']);
              } else {
                this.alertService.success('Face ID Verified', 'Welcome back! Face authentication successful.');
                this.router.navigate(['/admin/dashboard']);
              }
            } else {
              this.alertService.success('Face ID Verified', 'Authentication successful!');
              this.router.navigate(['/admin/dashboard']);
            }
          } else {
            this.errorMessage = response.message || 'Face does not match. Please try again.';
            if (response.accountLocked) {
              this.errorMessage = 'Account locked due to failed attempts. Contact manager to unlock.';
            }
          }
        },
        error: (err: any) => {
          this.isAuthenticatingFace = false;
          this.errorMessage = err.error?.message || 'Face ID verification failed. Please try again.';
        }
      });
    } catch (err: any) {
      this.stopCamera();
      this.isAuthenticatingFace = false;
      this.modelsLoading = false;
      if (err.name === 'NotAllowedError') {
        this.errorMessage = 'Camera access denied. Please allow camera permission and try again.';
      } else {
        this.errorMessage = 'Failed to access camera. Please check your camera and try again.';
      }
    }
  }

}
