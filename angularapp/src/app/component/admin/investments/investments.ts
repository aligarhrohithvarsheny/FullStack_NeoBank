import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-investments',
  standalone: true,
  templateUrl: './investments.html',
  styleUrls: ['./investments.css'],
  imports: [FormsModule, CommonModule]
})
export class Investments implements OnInit {
  investments: any[] = [];
  isLoadingInvestments: boolean = false;
  selectedInvestment: any = null;
  investmentStatusFilter: string = 'PENDING';
  investmentSearchQuery: string = '';
  adminName: string = 'Admin';
  
  // Foreclosure properties
  pendingForeclosures: any[] = [];
  allForeclosures: any[] = [];
  isLoadingForeclosures: boolean = false;
  showForeclosureSection: boolean = false;
  foreclosureFilter: string = 'PENDING'; // PENDING, ALL, COMPLETED, REJECTED

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      const adminData = sessionStorage.getItem('admin');
      if (adminData) {
        try {
          const admin = JSON.parse(adminData);
          this.adminName = admin.username || admin.name || 'Admin';
        } catch (e) {
          console.error('Error parsing admin data:', e);
        }
      }
      this.loadInvestments();
      this.loadForeclosures();
    }
  }

  loadInvestments() {
    this.isLoadingInvestments = true;
    this.http.get(`${environment.apiBaseUrl}/api/investments`).subscribe({
      next: (investments: any) => {
        this.investments = investments || [];
        this.isLoadingInvestments = false;
        this.filterInvestments();
      },
      error: (err: any) => {
        console.error('Error loading investments:', err);
        this.alertService.error('Error', 'Failed to load investments');
        this.isLoadingInvestments = false;
      }
    });
  }

  filterInvestments() {
    // Filtering is handled by status filter in template
  }

  getFilteredInvestments() {
    let filtered = [...this.investments];

    if (this.investmentStatusFilter !== 'ALL') {
      filtered = filtered.filter(inv => inv.status === this.investmentStatusFilter);
    }

    if (this.investmentSearchQuery && this.investmentSearchQuery.trim() !== '') {
      const query = this.investmentSearchQuery.toLowerCase();
      filtered = filtered.filter(inv =>
        (inv.userName && inv.userName.toLowerCase().includes(query)) ||
        (inv.accountNumber && inv.accountNumber.toLowerCase().includes(query)) ||
        (inv.fundName && inv.fundName.toLowerCase().includes(query))
      );
    }

    return filtered;
  }

  approveInvestment(investment: any) {
    if (!confirm(`Approve investment of ₹${investment.investmentAmount} for ${investment.userName}?`)) return;
    
    // Ensure ID is a number
    const investmentId = typeof investment.id === 'number' ? investment.id : Number(investment.id);
    if (isNaN(investmentId)) {
      this.alertService.error('Validation Error', 'Invalid investment ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/investments/${investmentId}/approve?approvedBy=${this.adminName}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Investment approved successfully');
          this.loadInvestments();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve investment');
        }
      },
      error: (err: any) => {
        console.error('Error approving investment:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve investment');
      }
    });
  }

  rejectInvestment(investment: any) {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;
    
    // Ensure ID is a number
    const investmentId = typeof investment.id === 'number' ? investment.id : Number(investment.id);
    if (isNaN(investmentId)) {
      this.alertService.error('Validation Error', 'Invalid investment ID');
      return;
    }
    this.http.put(`${environment.apiBaseUrl}/api/investments/${investmentId}/reject?rejectedBy=${this.adminName}&reason=${encodeURIComponent(reason)}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Investment rejected');
          this.loadInvestments();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject investment');
        }
      },
      error: (err: any) => {
        console.error('Error rejecting investment:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject investment');
      }
    });
  }

  // Foreclosure Methods
  loadForeclosures() {
    this.isLoadingForeclosures = true;
    if (this.foreclosureFilter === 'PENDING') {
      this.http.get(`${environment.apiBaseUrl}/api/investments/foreclosure/pending`).subscribe({
        next: (foreclosures: any) => {
          this.pendingForeclosures = foreclosures || [];
          this.isLoadingForeclosures = false;
        },
        error: (err: any) => {
          console.error('Error loading pending foreclosures:', err);
          this.isLoadingForeclosures = false;
        }
      });
    } else {
      this.http.get(`${environment.apiBaseUrl}/api/investments/foreclosure/all`).subscribe({
        next: (foreclosures: any) => {
          this.allForeclosures = foreclosures || [];
          this.isLoadingForeclosures = false;
        },
        error: (err: any) => {
          console.error('Error loading foreclosures:', err);
          this.isLoadingForeclosures = false;
        }
      });
    }
  }

  getFilteredForeclosures() {
    if (this.foreclosureFilter === 'PENDING') {
      return this.pendingForeclosures;
    } else if (this.foreclosureFilter === 'ALL') {
      return this.allForeclosures;
    } else {
      return this.allForeclosures.filter(f => f.status === this.foreclosureFilter);
    }
  }

  approveForeclosure(foreclosure: any) {
    if (!confirm(`Approve foreclosure request for ₹${foreclosure.foreclosureAmount}? Amount will be credited to user account.`)) return;
    
    const foreclosureId = typeof foreclosure.id === 'number' ? foreclosure.id : Number(foreclosure.id);
    if (isNaN(foreclosureId)) {
      this.alertService.error('Validation Error', 'Invalid foreclosure ID');
      return;
    }
    
    this.http.put(`${environment.apiBaseUrl}/api/investments/foreclosure/${foreclosureId}/approve?approvedBy=${this.adminName}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Foreclosure approved and amount credited successfully');
          this.loadForeclosures();
          this.loadInvestments();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve foreclosure');
        }
      },
      error: (err: any) => {
        console.error('Error approving foreclosure:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve foreclosure');
      }
    });
  }

  rejectForeclosure(foreclosure: any) {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;
    
    const foreclosureId = typeof foreclosure.id === 'number' ? foreclosure.id : Number(foreclosure.id);
    if (isNaN(foreclosureId)) {
      this.alertService.error('Validation Error', 'Invalid foreclosure ID');
      return;
    }
    
    this.http.put(`${environment.apiBaseUrl}/api/investments/foreclosure/${foreclosureId}/reject?rejectedBy=${this.adminName}&reason=${encodeURIComponent(reason)}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Foreclosure request rejected');
          this.loadForeclosures();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject foreclosure');
        }
      },
      error: (err: any) => {
        console.error('Error rejecting foreclosure:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject foreclosure');
      }
    });
  }

  getForeclosureStatusClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'COMPLETED': return 'status-approved';
      case 'PENDING': return 'status-pending';
      case 'REJECTED': return 'status-rejected';
      default: return 'status-default';
    }
  }

  toggleForeclosureSection() {
    this.showForeclosureSection = !this.showForeclosureSection;
    if (this.showForeclosureSection) {
      this.loadForeclosures();
    }
  }

  goBack() {
    const navigationSource = sessionStorage.getItem('navigationSource');
    if (navigationSource === 'MANAGER') {
      sessionStorage.removeItem('navigationSource');
      sessionStorage.removeItem('managerReturnPath');
      this.router.navigate(['/manager/dashboard']);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }
}


