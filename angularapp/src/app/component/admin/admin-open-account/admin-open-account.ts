import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminAccountApplicationService } from '../../../service/admin-account-application.service';
import { AdminAccountApplication, AdminAccountAppStats } from '../../../model/admin-account-application/admin-account-application.model';

@Component({
  selector: 'app-admin-open-account',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-open-account.html',
  styleUrls: ['./admin-open-account.css']
})
export class AdminOpenAccount implements OnInit {

  // Tab control
  activeTab: string = 'open-account';

  // Admin info
  adminName: string = '';
  adminEmail: string = '';
  isManager: boolean = false;

  // Statistics
  stats: AdminAccountAppStats = {
    totalApplications: 0,
    pendingCount: 0,
    adminVerifiedCount: 0,
    managerApprovedCount: 0,
    rejectedCount: 0,
    savingsCount: 0,
    currentCount: 0,
    salaryCount: 0
  };

  // Account Type Selection
  selectedAccountType: string = 'Savings';

  // Open Account Form
  newApplication: AdminAccountApplication = this.getEmptyApplication();

  // Applications list
  allApplications: AdminAccountApplication[] = [];
  filteredApplications: AdminAccountApplication[] = [];
  pendingApplications: AdminAccountApplication[] = [];
  awaitingManagerApps: AdminAccountApplication[] = [];
  searchTerm: string = '';
  statusFilter: string = 'ALL';
  typeFilter: string = 'ALL';

  // Selected application for detail view
  selectedApplication: AdminAccountApplication | null = null;
  showDetailView: boolean = false;

  // Manager approval
  approvalRemarks: string = '';
  verifyRemarks: string = '';

  // Document upload
  signedFile: File | null = null;
  additionalFile: File | null = null;

  // Aadhaar/PAN auto-fill
  isLookingUp: boolean = false;
  lookupMessage: string = '';
  lookupType: string = ''; // success, info, error
  autoFilled: boolean = false;

  // Existing account check
  existingAccounts: any[] = [];
  blockedAccountTypes: string[] = [];
  allowedAccountTypes: string[] = ['Savings', 'Current', 'Salary'];
  existingCheckDone: boolean = false;
  isCheckingExisting: boolean = false;

  // Alert
  alertMessage: string = '';
  alertType: string = 'success';
  showAlert: boolean = false;

  // Loading
  isLoading: boolean = false;
  isDownloading: boolean = false;

  // Edit mode
  isEditMode: boolean = false;
  editApplication: AdminAccountApplication | null = null;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private appService: AdminAccountApplicationService
  ) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;

    try {
      const adminData = sessionStorage.getItem('admin');
      const userRole = sessionStorage.getItem('userRole');
      if (adminData) {
        const admin = JSON.parse(adminData);
        this.adminName = admin.name || admin.email || 'Admin';
        this.adminEmail = admin.email || '';
        this.isManager = userRole === 'MANAGER' || admin.role === 'MANAGER' || admin.designation === 'Manager';
        if (this.isManager) {
          this.activeTab = 'manager-approval';
        }
      }
    } catch (e) {
      console.warn('Could not read admin data', e);
    }

    this.loadAllData();
  }

  // ==================== Data Loading ====================

  loadAllData() {
    this.loadStatistics();
    this.loadApplications();
  }

  loadStatistics() {
    this.appService.getStatistics().subscribe({
      next: (data: any) => this.stats = data,
      error: (err) => console.error('Error loading stats:', err)
    });
  }

  loadApplications() {
    this.isLoading = true;
    this.appService.getAllApplications().subscribe({
      next: (apps) => {
        this.allApplications = apps;
        this.filterApplications();
        this.pendingApplications = apps.filter(a => a.status === 'PENDING' || a.status === 'DOCUMENTS_UPLOADED');
        this.awaitingManagerApps = apps.filter(a => a.status === 'ADMIN_VERIFIED');
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading applications:', err);
        this.isLoading = false;
      }
    });
  }

  // ==================== Tab Management ====================

  setActiveTab(tab: string) {
    this.activeTab = tab;
    this.showDetailView = false;
    this.selectedApplication = null;

    if (tab === 'view-applications' || tab === 'approve-applications' || tab === 'manager-approval') {
      this.loadApplications();
    }
  }

  // ==================== Filter ====================

  filterApplications() {
    let filtered = [...this.allApplications];

    if (this.statusFilter !== 'ALL') {
      filtered = filtered.filter(a => a.status === this.statusFilter);
    }
    if (this.typeFilter !== 'ALL') {
      filtered = filtered.filter(a => a.accountType === this.typeFilter);
    }
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(a =>
        (a.fullName?.toLowerCase().includes(term)) ||
        (a.applicationNumber?.toLowerCase().includes(term)) ||
        (a.aadharNumber?.includes(term)) ||
        (a.panNumber?.toLowerCase().includes(term)) ||
        (a.phone?.includes(term)) ||
        (a.accountNumber?.includes(term))
      );
    }

    this.filteredApplications = filtered;
  }

  // ==================== Open Account ====================

  getEmptyApplication(): AdminAccountApplication {
    return {
      accountType: 'Savings',
      fullName: '',
      dateOfBirth: '',
      age: undefined,
      gender: '',
      occupation: '',
      income: undefined,
      phone: '',
      email: '',
      address: '',
      city: '',
      state: '',
      pincode: '',
      aadharNumber: '',
      panNumber: '',
      businessName: '',
      businessType: 'Proprietor',
      businessRegistrationNumber: '',
      gstNumber: '',
      shopAddress: '',
      companyName: '',
      companyId: '',
      designation: '',
      monthlySalary: undefined,
      salaryCreditDate: undefined,
      employerAddress: '',
      hrContactNumber: '',
      branchName: 'NeoBank Main Branch',
      ifscCode: 'EZYV000123',
      declarationAccepted: false
    };
  }

  onAccountTypeChange() {
    // Reset type-specific fields when switching
    this.newApplication.accountType = this.selectedAccountType;
  }

  submitApplication() {
    if (!this.validateApplication()) return;

    this.newApplication.createdBy = this.adminName;
    this.newApplication.accountType = this.selectedAccountType;
    this.newApplication.bankOfficerSignature = this.adminName;
    this.newApplication.declarationAccepted = true;
    this.newApplication.declarationDate = new Date().toISOString();

    this.isLoading = true;
    this.appService.createApplication(this.newApplication).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.showAlertMessage('Application created successfully! Application Number: ' + response.application.applicationNumber, 'success');
          this.newApplication = this.getEmptyApplication();
          this.loadAllData();
        } else {
          this.showAlertMessage(response.error || 'Failed to create application', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Failed to create application', 'error');
        this.isLoading = false;
      }
    });
  }

  validateApplication(): boolean {
    if (!this.newApplication.fullName?.trim()) {
      this.showAlertMessage('Full Name is required', 'error'); return false;
    }
    if (!this.newApplication.phone?.trim() || this.newApplication.phone.length !== 10) {
      this.showAlertMessage('Valid 10-digit Mobile Number is required', 'error'); return false;
    }
    if (!this.newApplication.aadharNumber?.trim() || this.newApplication.aadharNumber.length !== 12) {
      this.showAlertMessage('Valid 12-digit Aadhaar Number is required', 'error'); return false;
    }
    if (!this.newApplication.panNumber?.trim() || this.newApplication.panNumber.length !== 10) {
      this.showAlertMessage('Valid 10-character PAN Number is required', 'error'); return false;
    }
    if (!this.newApplication.declarationAccepted) {
      // Auto-accept for admin submission
      this.newApplication.declarationAccepted = true;
    }

    // Type-specific validation
    if (this.selectedAccountType === 'Current') {
      if (!this.newApplication.businessName?.trim()) {
        this.showAlertMessage('Business Name is required for Current Account', 'error'); return false;
      }
    }
    if (this.selectedAccountType === 'Salary') {
      if (!this.newApplication.companyName?.trim()) {
        this.showAlertMessage('Company Name is required for Salary Account', 'error'); return false;
      }
    }
    return true;
  }

  // ==================== Application Actions ====================

  viewApplicationDetails(app: AdminAccountApplication) {
    this.selectedApplication = app;
    this.showDetailView = true;
    this.approvalRemarks = '';
    this.verifyRemarks = '';
  }

  closeDetailView() {
    this.showDetailView = false;
    this.selectedApplication = null;
    this.isEditMode = false;
    this.editApplication = null;
  }

  // ==================== Edit Mode ====================

  canEditApplication(): boolean {
    if (!this.selectedApplication) return false;
    const status = this.selectedApplication.status;
    return status !== 'MANAGER_APPROVED' && status !== 'ACTIVE' && status !== 'CLOSED';
  }

  enterEditMode() {
    if (!this.selectedApplication) return;
    this.editApplication = { ...this.selectedApplication };
    this.isEditMode = true;
  }

  cancelEditMode() {
    this.isEditMode = false;
    this.editApplication = null;
  }

  saveEditedApplication() {
    if (!this.editApplication?.id) return;

    // Basic validation
    if (!this.editApplication.fullName?.trim()) {
      this.showAlertMessage('Full Name is required', 'error'); return;
    }
    if (!this.editApplication.phone?.trim() || this.editApplication.phone.length !== 10) {
      this.showAlertMessage('Valid 10-digit Mobile Number is required', 'error'); return;
    }
    if (!this.editApplication.aadharNumber?.trim() || this.editApplication.aadharNumber.length !== 12) {
      this.showAlertMessage('Valid 12-digit Aadhaar Number is required', 'error'); return;
    }
    if (!this.editApplication.panNumber?.trim() || this.editApplication.panNumber.length !== 10) {
      this.showAlertMessage('Valid 10-character PAN Number is required', 'error'); return;
    }
    if (this.editApplication.accountType === 'Current' && !this.editApplication.businessName?.trim()) {
      this.showAlertMessage('Business Name is required for Current Account', 'error'); return;
    }
    if (this.editApplication.accountType === 'Salary' && !this.editApplication.companyName?.trim()) {
      this.showAlertMessage('Company Name is required for Salary Account', 'error'); return;
    }

    this.isLoading = true;
    this.appService.updateApplication(this.editApplication.id, this.editApplication).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.showAlertMessage('Application details updated successfully', 'success');
          this.selectedApplication = response.application;
          this.isEditMode = false;
          this.editApplication = null;
          this.loadAllData();
        } else {
          this.showAlertMessage(response.error || 'Failed to update application', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Failed to update application', 'error');
        this.isLoading = false;
      }
    });
  }

  // ==================== Admin Verify Validation ====================

  canAdminVerify(): boolean {
    if (!this.selectedApplication) return false;
    return this.getMissingFields().length === 0;
  }

  getMissingFields(): string[] {
    const app = this.selectedApplication;
    if (!app) return ['No application selected'];
    const missing: string[] = [];

    // Common required fields
    if (!app.fullName?.trim()) missing.push('Full Name');
    if (!app.dateOfBirth) missing.push('Date of Birth');
    if (!app.gender?.trim()) missing.push('Gender');
    if (!app.phone?.trim() || app.phone.length < 10) missing.push('Phone (10 digits)');
    if (!app.email?.trim()) missing.push('Email');
    if (!app.aadharNumber?.trim() || app.aadharNumber.length < 12) missing.push('Aadhaar (12 digits)');
    if (!app.panNumber?.trim() || app.panNumber.length < 10) missing.push('PAN (10 characters)');
    if (!app.address?.trim()) missing.push('Address');
    if (!app.city?.trim()) missing.push('City');
    if (!app.state?.trim()) missing.push('State');
    if (!app.pincode?.trim()) missing.push('Pincode');
    if (!app.occupation?.trim()) missing.push('Occupation');

    // Account type specific fields
    if (app.accountType === 'Current') {
      if (!app.businessName?.trim()) missing.push('Business Name');
      if (!app.businessType?.trim()) missing.push('Business Type');
    }
    if (app.accountType === 'Salary') {
      if (!app.companyName?.trim()) missing.push('Company Name');
      if (!app.designation?.trim()) missing.push('Designation');
    }

    // Documents — signed application must be uploaded
    if (!app.signedApplicationPath) missing.push('Signed Application (upload required)');

    return missing;
  }

  // Admin verify
  adminVerify() {
    if (!this.selectedApplication?.id) return;
    if (!this.canAdminVerify()) {
      const missing = this.getMissingFields();
      this.showAlertMessage('Cannot verify: Missing fields — ' + missing.join(', '), 'error');
      return;
    }
    this.isLoading = true;
    this.appService.adminVerify(this.selectedApplication.id, this.adminName, this.verifyRemarks).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.showAlertMessage('Application verified successfully', 'success');
          this.loadAllData();
          this.closeDetailView();
        } else {
          this.showAlertMessage(response.error || 'Verification failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Verification failed', 'error');
        this.isLoading = false;
      }
    });
  }

  // Manager approve
  managerApprove() {
    if (!this.selectedApplication?.id) return;
    this.isLoading = true;
    this.appService.managerApprove(this.selectedApplication.id, this.adminName, this.approvalRemarks).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.showAlertMessage('Application approved! Account Number: ' + response.application.accountNumber, 'success');
          this.loadAllData();
          this.closeDetailView();
        } else {
          this.showAlertMessage(response.error || 'Approval failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Approval failed', 'error');
        this.isLoading = false;
      }
    });
  }

  // Manager reject
  managerReject() {
    if (!this.selectedApplication?.id) return;
    if (!this.approvalRemarks?.trim()) {
      this.showAlertMessage('Please provide rejection reason', 'error');
      return;
    }
    this.isLoading = true;
    this.appService.managerReject(this.selectedApplication.id, this.adminName, this.approvalRemarks).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.showAlertMessage('Application rejected', 'success');
          this.loadAllData();
          this.closeDetailView();
        } else {
          this.showAlertMessage(response.error || 'Rejection failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage(err.error?.error || 'Rejection failed', 'error');
        this.isLoading = false;
      }
    });
  }

  // ==================== Download Application ====================

  downloadApplication(app: AdminAccountApplication) {
    if (!app.id) return;
    this.isDownloading = true;
    this.appService.downloadApplication(app.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'NeoBank_Application_' + (app.applicationNumber || app.id) + '.pdf';
        link.click();
        window.URL.revokeObjectURL(url);
        this.showAlertMessage('Application form downloaded successfully', 'success');
        this.isDownloading = false;
      },
      error: (err) => {
        this.showAlertMessage('Failed to download application', 'error');
        this.isDownloading = false;
      }
    });
  }

  // ==================== Document Upload ====================

  onSignedFileSelect(event: any) {
    this.signedFile = event.target.files[0] || null;
  }

  onAdditionalFileSelect(event: any) {
    this.additionalFile = event.target.files[0] || null;
  }

  uploadSignedApplication() {
    if (!this.selectedApplication?.id || !this.signedFile) {
      this.showAlertMessage('Please select a signed application file', 'error');
      return;
    }
    this.isLoading = true;
    this.appService.uploadSignedApplication(this.selectedApplication.id, this.signedFile).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.showAlertMessage('Signed application uploaded successfully', 'success');
          this.signedFile = null;
          this.loadAllData();
          if (this.selectedApplication?.id) {
            this.appService.getById(this.selectedApplication.id).subscribe(app => this.selectedApplication = app);
          }
        } else {
          this.showAlertMessage(response.error || 'Upload failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage('Upload failed', 'error');
        this.isLoading = false;
      }
    });
  }

  uploadAdditionalDocuments() {
    if (!this.selectedApplication?.id || !this.additionalFile) {
      this.showAlertMessage('Please select a document file', 'error');
      return;
    }
    this.isLoading = true;
    this.appService.uploadAdditionalDocuments(this.selectedApplication.id, this.additionalFile).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.showAlertMessage('Documents uploaded successfully', 'success');
          this.additionalFile = null;
          this.loadAllData();
        } else {
          this.showAlertMessage(response.error || 'Upload failed', 'error');
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.showAlertMessage('Upload failed', 'error');
        this.isLoading = false;
      }
    });
  }

  // ==================== View Uploaded Documents ====================

  viewSignedDocument() {
    if (!this.selectedApplication?.id) return;
    this.appService.viewSignedDocumentById(this.selectedApplication.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => {
        this.showAlertMessage('Failed to load signed document', 'error');
      }
    });
  }

  viewAdditionalDocuments() {
    if (!this.selectedApplication?.id) return;
    this.appService.viewAdditionalDocsById(this.selectedApplication.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => {
        this.showAlertMessage('Failed to load additional documents', 'error');
      }
    });
  }

  // ==================== Aadhaar/PAN Auto-Fill ====================

  onAadharInput() {
    const aadhar = this.newApplication.aadharNumber?.replace(/[^0-9]/g, '') || '';
    this.newApplication.aadharNumber = aadhar;
    this.lookupMessage = '';
    this.lookupType = '';
    this.autoFilled = false;

    // Reset existing account check if Aadhaar changes
    if (aadhar.length < 12) {
      this.existingAccounts = [];
      this.blockedAccountTypes = [];
      this.allowedAccountTypes = ['Savings', 'Current', 'Salary'];
      this.existingCheckDone = false;
    }

    if (aadhar.length === 12) {
      this.checkExistingAccounts(aadhar);
      this.lookupByAadhar(aadhar);
    }
  }

  onPanInput() {
    const pan = (this.newApplication.panNumber || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
    this.newApplication.panNumber = pan;

    if (pan.length === 10 && !this.autoFilled) {
      this.lookupByPan(pan);
    }
  }

  lookupByAadhar(aadhar: string) {
    this.isLookingUp = true;
    this.lookupMessage = 'Looking up Aadhaar...';
    this.lookupType = 'info';
    this.appService.lookupByAadhar(aadhar).subscribe({
      next: (data: any) => {
        this.isLookingUp = false;
        if (data && data.id) {
          this.fillFormFromPreloaded(data);
          this.lookupMessage = 'Auto-filled from uploaded customer data (Aadhaar: ' + aadhar + ')';
          this.lookupType = 'success';
          this.autoFilled = true;
        } else {
          this.lookupMessage = 'No pre-loaded data found for this Aadhaar. Please fill manually.';
          this.lookupType = 'info';
        }
      },
      error: () => {
        this.isLookingUp = false;
        this.lookupMessage = '';
        this.lookupType = '';
      }
    });
  }

  checkExistingAccounts(aadhar: string) {
    this.isCheckingExisting = true;
    this.appService.checkExistingAccounts(aadhar).subscribe({
      next: (result: any) => {
        this.isCheckingExisting = false;
        this.existingAccounts = result.existingAccounts || [];
        this.blockedAccountTypes = result.blockedTypes || [];
        this.allowedAccountTypes = result.allowedTypes || ['Savings', 'Current', 'Salary'];
        this.existingCheckDone = true;

        // If currently selected type is blocked, auto-switch to first allowed type
        if (this.blockedAccountTypes.includes(this.selectedAccountType)) {
          if (this.allowedAccountTypes.length > 0) {
            this.selectedAccountType = this.allowedAccountTypes[0];
            this.onAccountTypeChange();
          }
        }
      },
      error: () => {
        this.isCheckingExisting = false;
        this.existingCheckDone = false;
      }
    });
  }

  isAccountTypeBlocked(type: string): boolean {
    return this.blockedAccountTypes.includes(type);
  }

  lookupByPan(pan: string) {
    this.isLookingUp = true;
    this.appService.lookupByPan(pan).subscribe({
      next: (data: any) => {
        this.isLookingUp = false;
        if (data && data.id) {
          this.fillFormFromPreloaded(data);
          this.lookupMessage = 'Auto-filled from uploaded customer data (PAN: ' + pan + ')';
          this.lookupType = 'success';
          this.autoFilled = true;
        }
      },
      error: () => {
        this.isLookingUp = false;
      }
    });
  }

  fillFormFromPreloaded(data: any) {
    if (data.accountType) this.selectedAccountType = data.accountType;
    this.newApplication.accountType = this.selectedAccountType;
    if (data.fullName) this.newApplication.fullName = data.fullName;
    if (data.dateOfBirth) this.newApplication.dateOfBirth = data.dateOfBirth;
    if (data.age) this.newApplication.age = data.age;
    if (data.gender) this.newApplication.gender = data.gender;
    if (data.occupation) this.newApplication.occupation = data.occupation;
    if (data.income) this.newApplication.income = data.income;
    if (data.phone) this.newApplication.phone = data.phone;
    if (data.email) this.newApplication.email = data.email;
    if (data.address) this.newApplication.address = data.address;
    if (data.city) this.newApplication.city = data.city;
    if (data.state) this.newApplication.state = data.state;
    if (data.pincode) this.newApplication.pincode = data.pincode;
    if (data.aadharNumber) this.newApplication.aadharNumber = data.aadharNumber;
    if (data.panNumber) this.newApplication.panNumber = data.panNumber;
    // Current Account fields
    if (data.businessName) this.newApplication.businessName = data.businessName;
    if (data.businessType) this.newApplication.businessType = data.businessType;
    if (data.businessRegistrationNumber) this.newApplication.businessRegistrationNumber = data.businessRegistrationNumber;
    if (data.gstNumber) this.newApplication.gstNumber = data.gstNumber;
    if (data.shopAddress) this.newApplication.shopAddress = data.shopAddress;
    // Salary Account fields
    if (data.companyName) this.newApplication.companyName = data.companyName;
    if (data.companyId) this.newApplication.companyId = data.companyId;
    if (data.designation) this.newApplication.designation = data.designation;
    if (data.monthlySalary) this.newApplication.monthlySalary = data.monthlySalary;
    if (data.salaryCreditDate) this.newApplication.salaryCreditDate = data.salaryCreditDate;
    if (data.hrContactNumber) this.newApplication.hrContactNumber = data.hrContactNumber;
    if (data.employerAddress) this.newApplication.employerAddress = data.employerAddress;
    // Bank details
    if (data.branchName) this.newApplication.branchName = data.branchName;
    if (data.ifscCode) this.newApplication.ifscCode = data.ifscCode;
  }

  clearAutoFill() {
    this.newApplication = this.getEmptyApplication();
    this.autoFilled = false;
    this.lookupMessage = '';
    this.lookupType = '';
    this.selectedAccountType = 'Savings';
    this.existingAccounts = [];
    this.blockedAccountTypes = [];
    this.allowedAccountTypes = ['Savings', 'Current', 'Salary'];
    this.existingCheckDone = false;
  }

  // ==================== Utility ====================

  goBack() {
    if (this.isManager) {
      this.router.navigate(['/manager/dashboard']);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  showAlertMessage(message: string, type: string) {
    this.alertMessage = message;
    this.alertType = type;
    this.showAlert = true;
    setTimeout(() => this.showAlert = false, 5000);
  }

  getStatusClass(status: string | undefined): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'DOCUMENTS_UPLOADED': return 'status-documents';
      case 'ADMIN_VERIFIED': return 'status-verified';
      case 'MANAGER_APPROVED': return 'status-approved';
      case 'MANAGER_REJECTED': return 'status-rejected';
      case 'ACTIVE': return 'status-active';
      case 'CLOSED': return 'status-closed';
      default: return 'status-pending';
    }
  }

  getStatusLabel(status: string | undefined): string {
    switch (status) {
      case 'PENDING': return 'Pending';
      case 'DOCUMENTS_UPLOADED': return 'Docs Uploaded';
      case 'ADMIN_VERIFIED': return 'Admin Verified';
      case 'MANAGER_APPROVED': return 'Manager Approved';
      case 'MANAGER_REJECTED': return 'Rejected';
      case 'ACTIVE': return 'Active';
      case 'CLOSED': return 'Closed';
      default: return status || 'Unknown';
    }
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateStr;
    }
  }

  formatCurrency(amount: number | undefined): string {
    if (amount == null) return '₹0.00';
    return '₹' + amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
