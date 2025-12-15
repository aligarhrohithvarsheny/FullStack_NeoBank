import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-fixed-deposits',
  standalone: true,
  templateUrl: './fixed-deposits.html',
  styleUrls: ['./fixed-deposits.css'],
  imports: [FormsModule, CommonModule]
})
export class FixedDeposits implements OnInit {
  fixedDeposits: any[] = [];
  isLoadingFDs: boolean = false;
  selectedFD: any = null;
  fdStatusFilter: string = 'PENDING';
  fdSearchQuery: string = '';
  adminName: string = 'Admin';

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
      this.loadFixedDeposits();
    }
  }

  loadFixedDeposits() {
    this.isLoadingFDs = true;
    this.http.get(`${environment.apiUrl}/fixed-deposits`).subscribe({
      next: (fds: any) => {
        this.fixedDeposits = fds || [];
        this.isLoadingFDs = false;
      },
      error: (err: any) => {
        console.error('Error loading fixed deposits:', err);
        this.alertService.error('Error', 'Failed to load fixed deposits');
        this.isLoadingFDs = false;
      }
    });
  }

  getFilteredFDs() {
    let filtered = [...this.fixedDeposits];

    if (this.fdStatusFilter !== 'ALL') {
      filtered = filtered.filter(fd => fd.status === this.fdStatusFilter);
    }

    if (this.fdSearchQuery && this.fdSearchQuery.trim() !== '') {
      const query = this.fdSearchQuery.toLowerCase();
      filtered = filtered.filter(fd =>
        (fd.userName && fd.userName.toLowerCase().includes(query)) ||
        (fd.accountNumber && fd.accountNumber.toLowerCase().includes(query)) ||
        (fd.fdAccountNumber && fd.fdAccountNumber.toLowerCase().includes(query))
      );
    }

    return filtered;
  }

  approveFixedDeposit(fd: any) {
    if (!confirm(`Approve FD of ₹${fd.principalAmount} for ${fd.userName}?`)) return;
    
    this.http.put(`${environment.apiUrl}/fixed-deposits/${fd.id}/approve?approvedBy=${this.adminName}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit approved successfully');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to approve FD');
        }
      },
      error: (err: any) => {
        console.error('Error approving FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to approve FD');
      }
    });
  }

  rejectFixedDeposit(fd: any) {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;
    
    this.http.put(`${environment.apiUrl}/fixed-deposits/${fd.id}/reject?rejectedBy=${this.adminName}&reason=${encodeURIComponent(reason)}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'Fixed Deposit rejected');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to reject FD');
        }
      },
      error: (err: any) => {
        console.error('Error rejecting FD:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to reject FD');
      }
    });
  }

  isFDMatured(fd: any): boolean {
    if (!fd || !fd.maturityDate || fd.isMatured) return false;
    const maturityDate = new Date(fd.maturityDate);
    const today = new Date();
    return maturityDate <= today;
  }

  processFDMaturity(fd: any) {
    if (!confirm(`Process maturity for FD ${fd.fdAccountNumber}? Maturity amount ₹${fd.maturityAmount} will be credited to account.`)) return;
    
    this.http.put(`${environment.apiUrl}/fixed-deposits/${fd.id}/process-maturity?processedBy=${this.adminName}`, {}).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.alertService.success('Success', 'FD maturity processed successfully');
          this.loadFixedDeposits();
        } else {
          this.alertService.error('Error', response.message || 'Failed to process maturity');
        }
      },
      error: (err: any) => {
        console.error('Error processing FD maturity:', err);
        this.alertService.error('Error', err.error?.message || 'Failed to process maturity');
      }
    });
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


