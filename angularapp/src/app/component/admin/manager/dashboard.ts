import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

interface AdminFeature {
  id: string;
  name: string;
  description: string;
  category: string;
  enabled: boolean;
  icon: string;
}

@Component({
  selector: 'app-manager-dashboard',
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
  encapsulation: ViewEncapsulation.None,
  imports: [FormsModule, CommonModule],
  standalone: true
})
export class ManagerDashboard implements OnInit {
  managerName: string = 'Manager';
  activeSection: string = 'dashboard';
  sidebarCollapsed: boolean = false;

  // Investments, FDs, and EMIs for manager view
  investments: any[] = [];
  fixedDeposits: any[] = [];
  allEmis: any[] = [];
  isLoadingInvestments: boolean = false;
  isLoadingFDs: boolean = false;
  isLoadingEMIs: boolean = false;
  selectedInvestment: any = null;
  selectedFD: any = null;
  selectedEMI: any = null;

  // Feature Access Control
  adminFeatures: AdminFeature[] = [
    { id: 'manage-users', name: 'Manage Users', description: 'Create, view, and update users', category: 'Main Menu', enabled: true, icon: 'fa-users' },
    { id: 'user-control', name: 'Full User Control', description: 'Edit all user details & accounts', category: 'Main Menu', enabled: true, icon: 'fa-user-cog' },
    { id: 'deposit-withdraw', name: 'Deposit/Withdraw', description: 'Deposit or withdraw money for users', category: 'Financial Operations', enabled: true, icon: 'fa-exchange-alt' },
    { id: 'transactions', name: 'Transactions', description: 'View all user transactions', category: 'Financial Operations', enabled: true, icon: 'fa-chart-line' },
    { id: 'loans', name: 'Loan Requests', description: 'Approve or reject loan applications', category: 'Financial Operations', enabled: true, icon: 'fa-coins' },
    { id: 'gold-loans', name: 'Gold Loans', description: 'Approve loans, view EMI & foreclosure', category: 'Financial Operations', enabled: true, icon: 'fa-gem' },
    { id: 'cheques', name: 'Cheque Management', description: 'Draw and bounce cheques', category: 'Financial Operations', enabled: true, icon: 'fa-file-invoice' },
    { id: 'cards', name: 'Manage Cards', description: 'Issue, block, or update cards', category: 'Services', enabled: true, icon: 'fa-credit-card' },
    { id: 'kyc', name: 'KYC Verification', description: 'Approve or reject KYC requests', category: 'Services', enabled: true, icon: 'fa-id-card' },
    { id: 'subsidy-claims', name: 'Subsidy Claims', description: 'Manage education loan subsidies', category: 'Services', enabled: true, icon: 'fa-graduation-cap' },
    { id: 'education-loans', name: 'Education Loans', description: 'View & edit child details, education & college info', category: 'Services', enabled: true, icon: 'fa-university' },
    { id: 'chat', name: 'Customer Support', description: 'Chat with customers', category: 'Services', enabled: true, icon: 'fa-comments' },
    { id: 'tracking', name: 'Account Tracking', description: 'Track account creation status', category: 'Management', enabled: true, icon: 'fa-search-location' },
    { id: 'aadhar-verification', name: 'Aadhar Verification', description: 'Verify Aadhar for new accounts', category: 'Management', enabled: true, icon: 'fa-id-card' },
    { id: 'gold-rate', name: 'Gold Rate Management', description: 'Update daily gold rates', category: 'Management', enabled: true, icon: 'fa-coins' },
    { id: 'login-history', name: 'User Login History', description: 'View user login details, time, location', category: 'Management', enabled: true, icon: 'fa-history' },
    { id: 'update-history', name: 'User Update History', description: 'View admin updates and changes to user profiles', category: 'Management', enabled: true, icon: 'fa-edit' },
    { id: 'daily-report', name: 'Daily Activity Report', description: 'View and print all admin activities for the day', category: 'Management', enabled: true, icon: 'fa-calendar-day' },
    { id: 'investments', name: 'Investments Management', description: 'View and update investment details', category: 'Financial Operations', enabled: true, icon: 'fa-chart-line' },
    { id: 'fixed-deposits', name: 'Fixed Deposits Management', description: 'View and update FD details', category: 'Financial Operations', enabled: true, icon: 'fa-piggy-bank' },
    { id: 'emi-management', name: 'EMI Management', description: 'View and monitor all EMIs', category: 'Financial Operations', enabled: true, icon: 'fa-calendar-check' }
  ];

  filteredFeatures: AdminFeature[] = [];
  featureFilter: string = 'ALL';
  featureSearchQuery: string = '';

  // Admin List and Selection
  allAdmins: any[] = [];
  selectedAdmin: any = null;
  isLoadingAdmins: boolean = false;
  adminFeatureAccess: Map<string, Map<string, boolean>> = new Map(); // adminEmail -> featureId -> enabled

  // Blocked Admins Management
  blockedAdmins: any[] = [];
  isLoadingBlockedAdmins: boolean = false;
  selectedBlockedAdmin: any = null;
  showResetPasswordModal: boolean = false;
  newPassword: string = '';
  confirmPassword: string = '';
  isResettingPassword: boolean = false;
  isUnblocking: boolean = false;
  
  // Password reset for any admin (not just blocked)
  showAdminPasswordResetModal: boolean = false;
  selectedAdminForPasswordReset: any = null;

  // Manager Feature Access (features disabled for admins that manager can use)
  managerAccessibleFeatures: AdminFeature[] = [];
  filteredManagerFeatures: AdminFeature[] = [];
  managerFeatureFilter: string = 'ALL';
  managerFeatureSearchQuery: string = '';

  // Create Admin Employee
  newAdmin: any = {
    name: '',
    email: '',
    password: '',
    role: 'ADMIN',
    employeeId: '',
    profileComplete: false
  };
  isCreatingAdmin: boolean = false;
  adminCreationSuccess: boolean = false;
  adminCreationError: string = '';

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadManagerInfo();
      this.loadAllAdmins();
      this.loadBlockedAdmins();
      this.loadFeatureAccess();
      this.filterFeatures(); // Initialize filtered features
      this.loadManagerAccessibleFeatures(); // Load features manager can use
      this.checkAndCreateDefaultManager();
    }
  }

  loadBlockedAdmins() {
    this.isLoadingBlockedAdmins = true;
    this.http.get(`${environment.apiBaseUrl}/admins/blocked`).subscribe({
      next: (admins: any) => {
        this.blockedAdmins = admins || [];
        this.isLoadingBlockedAdmins = false;
        console.log('Loaded blocked admins:', this.blockedAdmins);
      },
      error: (err: any) => {
        console.error('Error loading blocked admins:', err);
        this.isLoadingBlockedAdmins = false;
      }
    });
  }

  unblockAdmin(admin: any) {
    if (!confirm(`Are you sure you want to unblock ${admin.name || admin.email}?`)) {
      return;
    }

    this.isUnblocking = true;
    this.http.post(`${environment.apiBaseUrl}/admins/unblock/${admin.id}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', `Admin ${admin.name || admin.email} has been unblocked successfully`);
          this.loadBlockedAdmins();
          this.loadAllAdmins(); // Refresh admin list
        } else {
          this.alertService.error('Error', response.message || 'Failed to unblock admin');
        }
        this.isUnblocking = false;
      },
      error: (err: any) => {
        console.error('Error unblocking admin:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to unblock admin');
        this.isUnblocking = false;
    }
    });
  }

  openResetPasswordModal(admin: any) {
    this.selectedBlockedAdmin = admin;
    this.newPassword = '';
    this.confirmPassword = '';
    this.showResetPasswordModal = true;
  }

  closeResetPasswordModal() {
    this.showResetPasswordModal = false;
    this.selectedBlockedAdmin = null;
    this.newPassword = '';
    this.confirmPassword = '';
  }

  openAdminPasswordResetModal(admin: any) {
    this.selectedAdminForPasswordReset = admin;
    this.newPassword = '';
    this.confirmPassword = '';
    this.showAdminPasswordResetModal = true;
  }

  closeAdminPasswordResetModal() {
    this.showAdminPasswordResetModal = false;
    this.selectedAdminForPasswordReset = null;
    this.newPassword = '';
    this.confirmPassword = '';
  }

  resetAnyAdminPassword() {
    if (!this.selectedAdminForPasswordReset) return;

    if (!this.newPassword || this.newPassword.length < 6) {
      this.alertService.error('Validation Error', 'Password must be at least 6 characters');
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.alertService.error('Validation Error', 'Passwords do not match');
      return;
    }

    this.isResettingPassword = true;
    this.http.post(`${environment.apiBaseUrl}/admins/reset-password/${this.selectedAdminForPasswordReset.id}`, {
      newPassword: this.newPassword
    }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', `Password reset successfully for ${this.selectedAdminForPasswordReset.name || this.selectedAdminForPasswordReset.email}`);
          this.closeAdminPasswordResetModal();
          this.loadAllAdmins(); // Refresh admin list
        } else {
          this.alertService.error('Error', response.message || 'Failed to reset password');
        }
        this.isResettingPassword = false;
      },
      error: (err: any) => {
        console.error('Error resetting password:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reset password');
        this.isResettingPassword = false;
      }
    });
  }

  resetAdminPassword() {
    if (!this.selectedBlockedAdmin) return;

    if (!this.newPassword || this.newPassword.length < 6) {
      this.alertService.error('Validation Error', 'Password must be at least 6 characters');
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.alertService.error('Validation Error', 'Passwords do not match');
      return;
    }

    this.isResettingPassword = true;
    this.http.post(`${environment.apiBaseUrl}/admins/reset-password/${this.selectedBlockedAdmin.id}`, {
      newPassword: this.newPassword
    }).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', `Password reset successfully for ${this.selectedBlockedAdmin.name || this.selectedBlockedAdmin.email}`);
          this.closeResetPasswordModal();
          this.loadBlockedAdmins();
          this.loadAllAdmins(); // Refresh admin list
        } else {
          this.alertService.error('Error', response.message || 'Failed to reset password');
        }
        this.isResettingPassword = false;
      },
      error: (err: any) => {
        console.error('Error resetting password:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reset password');
        this.isResettingPassword = false;
      }
    });
  }

  loadManagerAccessibleFeatures() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Get all disabled features across all admins
    const disabledFeatures = new Set<string>();
    
    // Check each admin's disabled features
    this.allAdmins.forEach(admin => {
      const savedKey = `adminFeatureAccess_${admin.email}`;
      const savedFeatures = localStorage.getItem(savedKey);
      if (savedFeatures) {
        try {
          const features = JSON.parse(savedFeatures);
          features.forEach((feature: any) => {
            if (feature.enabled === false) {
              disabledFeatures.add(feature.id);
            }
          });
        } catch (e) {
          console.error('Error parsing admin feature access:', e);
        }
      }
    });
    
    // Also check from backend if available
    this.allAdmins.forEach(admin => {
      this.http.get(`${environment.apiBaseUrl}/admins/feature-access/${admin.email}`).subscribe({
        next: (response: any) => {
          if (response && response.success && response.features) {
            response.features.forEach((feature: any) => {
              if (feature.enabled === false) {
                disabledFeatures.add(feature.id);
              }
            });
            this.updateManagerAccessibleFeatures(disabledFeatures);
          }
        },
        error: (err) => {
          console.error('Error loading feature access from backend:', err);
        }
      });
    });
    
    this.updateManagerAccessibleFeatures(disabledFeatures);
  }

  updateManagerAccessibleFeatures(disabledFeatureIds: Set<string>) {
    // Create list of features that are disabled for admins (manager can use these)
    this.managerAccessibleFeatures = this.adminFeatures
      .filter(f => disabledFeatureIds.has(f.id))
      .map(f => ({ ...f })); // Create copies
    
    this.filterManagerFeatures();
  }

  filterManagerFeatures() {
    let filtered = [...this.managerAccessibleFeatures];
    
    // Filter by category
    if (this.managerFeatureFilter !== 'ALL') {
      filtered = filtered.filter(f => f.category === this.managerFeatureFilter);
    }
    
    // Filter by search query
    if (this.managerFeatureSearchQuery) {
      const query = this.managerFeatureSearchQuery.toLowerCase();
      filtered = filtered.filter(f => 
        f.name.toLowerCase().includes(query) ||
        f.description.toLowerCase().includes(query) ||
        f.category.toLowerCase().includes(query)
      );
    }
    
    this.filteredManagerFeatures = filtered;
  }

  navigateToFeature(featureId: string) {
    // Map feature IDs to admin dashboard routes
    const routeMap: { [key: string]: string } = {
      'manage-users': '/admin/users',
      'user-control': '/admin/user-control',
      'deposit-withdraw': '/admin/dashboard',
      'transactions': '/admin/transactions',
      'loans': '/admin/loans',
      'gold-loans': '/admin/gold-loans',
      'cheques': '/admin/cheques',
      'cards': '/admin/cards',
      'kyc': '/admin/kyc',
      'subsidy-claims': '/admin/subsidy-claims',
      'education-loans': '/admin/education-loan-applications',
      'chat': '/admin/chat',
      'tracking': '/admin/dashboard',
      'aadhar-verification': '/admin/dashboard',
      'gold-rate': '/admin/dashboard',
      'login-history': '/admin/dashboard',
      'update-history': '/admin/dashboard',
      'daily-report': '/admin/dashboard',
      'investments': '/admin/dashboard',
      'fixed-deposits': '/admin/dashboard',
      'emi-management': '/admin/dashboard'
    };
    
    const route = routeMap[featureId] || '/admin/dashboard';
    
    // Store that this navigation is from manager dashboard
    sessionStorage.setItem('navigationSource', 'MANAGER');
    sessionStorage.setItem('managerReturnPath', '/manager/dashboard');
    
    // For dashboard features, we need to pass a section parameter
    if (route === '/admin/dashboard') {
      // Store the section to open in sessionStorage
      sessionStorage.setItem('adminDashboardSection', featureId);
    }
    
    this.router.navigate([route]);
  }

  loadAllAdmins() {
    this.isLoadingAdmins = true;
    this.http.get(`${environment.apiBaseUrl}/admins/all`).subscribe({
      next: (admins: any) => {
        this.allAdmins = admins || [];
        this.isLoadingAdmins = false;
        console.log('Loaded admins:', this.allAdmins);
        // Reload manager accessible features after loading admins
        this.loadManagerAccessibleFeatures();
      },
      error: (err: any) => {
        console.error('Error loading admins:', err);
        this.isLoadingAdmins = false;
      }
    });
  }

  selectAdmin(admin: any) {
    this.selectedAdmin = admin;
    this.loadAdminFeatureAccess(admin.email);
    console.log('Selected admin:', admin);
  }

  loadAdminFeatureAccess(adminEmail: string) {
    // Load saved feature access for this admin
    const savedKey = `adminFeatureAccess_${adminEmail}`;
    const savedFeatures = localStorage.getItem(savedKey);
    
    if (savedFeatures) {
      try {
        const features = JSON.parse(savedFeatures);
        // Update the adminFeatures array with saved values
        features.forEach((savedFeature: any) => {
          const feature = this.adminFeatures.find(f => f.id === savedFeature.id);
          if (feature) {
            feature.enabled = savedFeature.enabled;
          }
        });
        
        // Store in map
        const featureMap = new Map<string, boolean>();
        features.forEach((f: any) => {
          featureMap.set(f.id, f.enabled);
        });
        this.adminFeatureAccess.set(adminEmail, featureMap);
      } catch (e) {
        console.error('Error loading admin feature access:', e);
      }
    } else {
      // Default: all features enabled
      const featureMap = new Map<string, boolean>();
      this.adminFeatures.forEach(f => {
        featureMap.set(f.id, true);
      });
      this.adminFeatureAccess.set(adminEmail, featureMap);
    }
    
    // Also try to load from backend
    this.http.get(`${environment.apiBaseUrl}/admins/feature-access/${adminEmail}`).subscribe({
      next: (response: any) => {
        if (response && response.success && response.features && response.features.length > 0) {
          response.features.forEach((feature: any) => {
            const f = this.adminFeatures.find(af => af.id === feature.id);
            if (f) {
              f.enabled = feature.enabled;
            }
          });
          // Update localStorage
          localStorage.setItem(savedKey, JSON.stringify(response.features));
        }
      },
      error: (err) => {
        console.error('Error loading feature access from backend:', err);
      }
    });
  }

  checkAndCreateDefaultManager() {
    // This method can be used to check manager account status
    // Actual creation should be done via API endpoint
  }

  createDefaultManager() {
    if (!confirm('This will create a default manager account with:\nEmail: manager@neobank.com\nPassword: manager123\n\nContinue?')) {
      return;
    }

    this.http.post(`${environment.apiBaseUrl}/admins/create-default-manager`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Default manager account created successfully!\n\nLogin Details:\nEmail: manager@neobank.com\nPassword: manager123');
        } else {
          this.alertService.error('Error', response.message || 'Failed to create manager account');
        }
      },
      error: (err: any) => {
        console.error('Error creating default manager:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to create manager account');
      }
    });
  }

  loadManagerInfo() {
    const adminData = sessionStorage.getItem('admin');
    if (adminData) {
      try {
        const admin = JSON.parse(adminData);
        this.managerName = admin.name || admin.username || 'Manager';
      } catch (e) {
        console.error('Error parsing admin data:', e);
      }
    }
  }

  loadFeatureAccess() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const savedFeatures = localStorage.getItem('adminFeatureAccess');
    if (savedFeatures) {
      try {
        const features = JSON.parse(savedFeatures);
        // Update enabled status from saved data
        features.forEach((savedFeature: any) => {
          const feature = this.adminFeatures.find(f => f.id === savedFeature.id);
          if (feature) {
            feature.enabled = savedFeature.enabled;
          }
        });
      } catch (e) {
        console.error('Error loading feature access:', e);
      }
    }
  }

  saveFeatureAccess() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    if (!this.selectedAdmin) {
      this.alertService.error('Error', 'Please select an admin first');
      return;
    }
    
    // Save ALL features with their current enabled/disabled status for selected admin
    const featuresToSave = this.adminFeatures.map(f => ({
      id: f.id,
      enabled: f.enabled
    }));
    
    const adminEmail = this.selectedAdmin.email;
    const savedKey = `adminFeatureAccess_${adminEmail}`;
    
    console.log(`Saving feature access for admin ${adminEmail}:`, featuresToSave);
    localStorage.setItem(savedKey, JSON.stringify(featuresToSave));
    
    // Store in map
    const featureMap = new Map<string, boolean>();
    featuresToSave.forEach(f => {
      featureMap.set(f.id, f.enabled);
    });
    this.adminFeatureAccess.set(adminEmail, featureMap);
    
    // Dispatch custom event to notify same-tab listeners (admin dashboard)
    window.dispatchEvent(new CustomEvent('featureAccessUpdated', { 
      detail: { adminEmail, features: featuresToSave } 
    }));
    
    // Also save to backend
    this.http.post(`${environment.apiBaseUrl}/admins/feature-access/${adminEmail}`, {
      features: featuresToSave
    }).subscribe({
      next: (response: any) => {
        console.log('Feature access saved to backend:', response);
        this.alertService.success('Success', `Feature access updated successfully for ${this.selectedAdmin.name}! Admin dashboard will reflect changes on next page load.`);
        this.showReloadMessage();
        // Reload manager accessible features after saving
        this.loadManagerAccessibleFeatures();
      },
      error: (err: any) => {
        console.error('Error saving feature access:', err);
        // Still show success since it's saved locally
        this.alertService.success('Success', `Feature access updated (saved locally) for ${this.selectedAdmin.name}. Admin dashboard will reflect changes on next page load.`);
        this.showReloadMessage();
        // Reload manager accessible features after saving
        this.loadManagerAccessibleFeatures();
      }
    });
  }

  showReloadMessage() {
    // This will be shown to inform that changes take effect after admin reloads
    console.log('Feature access saved. Admins need to reload their dashboard to see changes.');
  }

  toggleFeature(feature: AdminFeature) {
    feature.enabled = !feature.enabled;
    this.saveFeatureAccess();
  }

  toggleAllFeatures(enabled: boolean) {
    this.adminFeatures.forEach(feature => {
      feature.enabled = enabled;
    });
    this.saveFeatureAccess();
  }

  filterFeatures() {
    let filtered = [...this.adminFeatures];

    if (this.featureFilter !== 'ALL') {
      filtered = filtered.filter(f => f.category === this.featureFilter);
    }

    if (this.featureSearchQuery && this.featureSearchQuery.trim() !== '') {
      const query = this.featureSearchQuery.toLowerCase();
      filtered = filtered.filter(f =>
        f.name.toLowerCase().includes(query) ||
        f.description.toLowerCase().includes(query) ||
        f.category.toLowerCase().includes(query)
      );
    }

    this.filteredFeatures = filtered;
  }

  getCategories(): string[] {
    return ['ALL', ...new Set(this.adminFeatures.map(f => f.category))];
  }

  getEnabledCount(): number {
    return this.adminFeatures.filter(f => f.enabled).length;
  }

  getDisabledCount(): number {
    return this.adminFeatures.filter(f => !f.enabled).length;
  }

  setActiveSection(section: string) {
    this.activeSection = section;
    if (section === 'create-admin') {
      this.clearAdminForm();
    } else if (section === 'manager-features') {
      this.loadManagerAccessibleFeatures();
    } else if (section === 'blocked-admins') {
      this.loadBlockedAdmins();
    }
  }

  createAdminEmployee() {
    if (!this.newAdmin.name || !this.newAdmin.email || !this.newAdmin.password) {
      this.adminCreationError = 'Please fill in all required fields';
      return;
    }

    if (this.newAdmin.password.length < 6) {
      this.adminCreationError = 'Password must be at least 6 characters';
      return;
    }

    this.isCreatingAdmin = true;
    this.adminCreationError = '';
    this.adminCreationSuccess = false;

    this.http.post(`${environment.apiBaseUrl}/admins/create`, this.newAdmin).subscribe({
      next: (response: any) => {
        this.isCreatingAdmin = false;
        this.adminCreationSuccess = true;
        this.alertService.success('Success', `Admin employee created successfully!\n\nEmail: ${this.newAdmin.email}\nPassword: ${this.newAdmin.password}`);
        
        // Reload admin list
        this.loadAllAdmins();
        
        // Keep the form data for display, but prepare for next creation
        setTimeout(() => {
          this.clearAdminForm();
        }, 5000);
      },
      error: (err: any) => {
        this.isCreatingAdmin = false;
        this.adminCreationError = err.error?.message || 'Failed to create admin employee. Email might already exist.';
        this.alertService.error('Error', this.adminCreationError);
      }
    });
  }

  clearAdminForm() {
    this.newAdmin = {
      name: '',
      email: '',
      password: '',
      role: 'ADMIN',
      employeeId: '',
      profileComplete: false
    };
    this.adminCreationSuccess = false;
    this.adminCreationError = '';
  }

  logout() {
    this.alertService.logoutSuccess();
    sessionStorage.removeItem('admin');
    sessionStorage.removeItem('userRole');
    this.router.navigate(['/admin/login']);
  }

  navigateTo(path: string) {
    this.router.navigate([`/admin/${path}`]);
  }

  // Load Investments, FDs, and EMIs
  loadInvestments() {
    this.isLoadingInvestments = true;
    this.http.get(`${environment.apiBaseUrl}/investments`).subscribe({
      next: (investments: any) => {
        this.investments = investments || [];
        this.isLoadingInvestments = false;
      },
      error: (err: any) => {
        console.error('Error loading investments:', err);
        this.isLoadingInvestments = false;
      }
    });
  }

  loadFixedDeposits() {
    this.isLoadingFDs = true;
    this.http.get(`${environment.apiBaseUrl}/fixed-deposits`).subscribe({
      next: (fds: any) => {
        this.fixedDeposits = fds || [];
        this.isLoadingFDs = false;
      },
      error: (err: any) => {
        console.error('Error loading fixed deposits:', err);
        this.isLoadingFDs = false;
      }
    });
  }

  loadAllEMIs() {
    this.isLoadingEMIs = true;
    this.http.get(`${environment.apiBaseUrl}/emis/overdue`).subscribe({
      next: (emis: any) => {
        this.allEmis = emis || [];
        this.isLoadingEMIs = false;
      },
      error: (err: any) => {
        console.error('Error loading EMIs:', err);
        this.allEmis = [];
        this.isLoadingEMIs = false;
      }
    });
  }

  updateInvestment(investmentId: number, updates: any) {
    this.http.put(`${environment.apiBaseUrl}/investments/${investmentId}`, updates).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Investment updated successfully');
          this.loadInvestments();
        } else {
          this.alertService.error('Error', response.message || 'Failed to update investment');
        }
      },
      error: (err: any) => {
        console.error('Error updating investment:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update investment');
      }
    });
  }

  updateFixedDeposit(fdId: number, updates: any) {
    this.http.put(`${environment.apiBaseUrl}/fixed-deposits/${fdId}`, updates).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit updated successfully');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to update FD');
        }
      },
      error: (err: any) => {
        console.error('Error updating FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update FD');
      }
    });
  }

  copyToClipboard(text: string) {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        this.alertService.success('Copied', 'Copied to clipboard!');
      }).catch(err => {
        console.error('Failed to copy:', err);
        this.fallbackCopyToClipboard(text);
      });
    } else {
      this.fallbackCopyToClipboard(text);
    }
  }

  fallbackCopyToClipboard(text: string) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    document.body.appendChild(textArea);
    textArea.select();
    try {
      document.execCommand('copy');
      this.alertService.success('Copied', 'Copied to clipboard!');
    } catch (err) {
      console.error('Fallback copy failed:', err);
      this.alertService.error('Error', 'Failed to copy to clipboard');
    }
    document.body.removeChild(textArea);
  }
}

