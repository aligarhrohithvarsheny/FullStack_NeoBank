import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EducationLoanApplicationService } from '../../../service/education-loan-application.service';
import { EducationLoanApplication } from '../../../model/education-loan-application/education-loan-application-module';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-education-loan-applications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './education-loan-applications.html',
  styleUrls: ['./education-loan-applications.css']
})
export class EducationLoanApplicationsComponent implements OnInit {
  applications: EducationLoanApplication[] = [];
  filteredApplications: EducationLoanApplication[] = [];
  selectedApplication: EducationLoanApplication | null = null;
  showEditModal: boolean = false;
  editForm: any = {};
  isLoading: boolean = false;
  searchTerm: string = '';
  statusFilter: string = 'All';
  adminName: string = '';

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private applicationService: EducationLoanApplicationService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      const admin = sessionStorage.getItem('admin');
      if (admin) {
        const adminData = JSON.parse(admin);
        this.adminName = adminData.name || 'Admin';
      }
      this.loadApplications();
    }
  }

  loadApplications() {
    this.isLoading = true;
    this.applicationService.getAllApplications().subscribe({
      next: (apps) => {
        this.applications = apps;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading applications:', err);
        this.alertService.error('Error', 'Failed to load education loan applications');
        this.isLoading = false;
      }
    });
  }

  applyFilters() {
    this.filteredApplications = this.applications.filter(app => {
      const matchesSearch = !this.searchTerm || 
        app.childName?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        app.collegeName?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        app.applicantAccountNumber?.includes(this.searchTerm) ||
        app.loanAccountNumber?.includes(this.searchTerm);
      
      const matchesStatus = this.statusFilter === 'All' || app.applicationStatus === this.statusFilter;
      
      return matchesSearch && matchesStatus;
    });
  }

  viewApplication(application: EducationLoanApplication) {
    this.selectedApplication = application;
    this.editForm = { ...application };
  }

  closeViewModal() {
    this.selectedApplication = null;
  }

  openEditModal(application: EducationLoanApplication) {
    this.selectedApplication = application;
    this.editForm = { ...application };
    this.showEditModal = true;
  }

  closeEditModal() {
    this.showEditModal = false;
    this.selectedApplication = null;
    this.editForm = {};
  }

  saveChanges() {
    if (!this.selectedApplication?.id) return;

    this.isLoading = true;
    this.applicationService.updateApplication(this.selectedApplication.id, this.editForm).subscribe({
      next: (updated) => {
        this.alertService.success('Success', 'Education loan application updated successfully');
        this.loadApplications();
        this.closeEditModal();
      },
      error: (err) => {
        console.error('Error updating application:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update application');
        this.isLoading = false;
      }
    });
  }

  updateStatus(id: number, status: string) {
    const notes = prompt('Enter notes (optional):');
    this.isLoading = true;
    this.applicationService.updateStatus(id, status, this.adminName, notes || '').subscribe({
      next: (updated) => {
        this.alertService.success('Success', `Application status updated to ${status}`);
        this.loadApplications();
      },
      error: (err) => {
        console.error('Error updating status:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to update status');
        this.isLoading = false;
      }
    });
  }

  rejectApplication(id: number) {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;

    this.isLoading = true;
    this.applicationService.rejectApplication(id, this.adminName, reason).subscribe({
      next: (rejected) => {
        this.alertService.success('Success', 'Application rejected');
        this.loadApplications();
      },
      error: (err) => {
        console.error('Error rejecting application:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject application');
        this.isLoading = false;
      }
    });
  }

  downloadDocument(id: number, fileType: string) {
    this.applicationService.downloadFile(id, fileType).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${fileType}_${id}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading file:', err);
        this.alertService.error('Error', 'Failed to download document');
      }
    });
  }

  formatDate(dateString?: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Pending': return 'status-pending';
      case 'Under Review': return 'status-review';
      case 'Approved': return 'status-approved';
      case 'Rejected': return 'status-rejected';
      default: return '';
    }
  }
}





