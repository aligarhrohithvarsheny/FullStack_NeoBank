import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-emi-management',
  standalone: true,
  templateUrl: './emi-management.html',
  styleUrls: ['./emi-management.css'],
  imports: [FormsModule, CommonModule]
})
export class EmiManagement implements OnInit {
  allEmis: any[] = [];
  isLoadingAllEMIs: boolean = false;
  selectedEMI: any = null;
  emiStatusFilter: string = 'ALL';
  emiSearchQuery: string = '';

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadAllEMIs();
    }
  }

  loadAllEMIs() {
    this.isLoadingAllEMIs = true;
    this.http.get(`${environment.apiBaseUrl}/emis`).subscribe({
      next: (emis: any) => {
        this.allEmis = emis || [];
        this.isLoadingAllEMIs = false;
      },
      error: (err: any) => {
        console.error('Error loading EMIs:', err);
        // Try loading overdue EMIs instead
        this.http.get(`${environment.apiBaseUrl}/emis/overdue`).subscribe({
          next: (overdueEmis: any) => {
            this.allEmis = overdueEmis || [];
            this.isLoadingAllEMIs = false;
          },
          error: (err2: any) => {
            console.error('Error loading overdue EMIs:', err2);
            this.allEmis = [];
            this.isLoadingAllEMIs = false;
            this.alertService.error('Error', 'Failed to load EMIs');
          }
        });
      }
    });
  }

  getFilteredEMIs() {
    let filtered = [...this.allEmis];

    if (this.emiStatusFilter !== 'ALL') {
      filtered = filtered.filter(emi => emi.status === this.emiStatusFilter);
    }

    if (this.emiSearchQuery && this.emiSearchQuery.trim() !== '') {
      const query = this.emiSearchQuery.toLowerCase();
      filtered = filtered.filter(emi =>
        (emi.accountNumber && emi.accountNumber.toLowerCase().includes(query)) ||
        (emi.loanAccountNumber && emi.loanAccountNumber.toLowerCase().includes(query))
      );
    }

    return filtered;
  }

  isEMIOverdue(emi: any): boolean {
    if (!emi || emi.status !== 'Pending' || !emi.dueDate) return false;
    const dueDate = new Date(emi.dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    dueDate.setHours(0, 0, 0, 0);
    return dueDate < today;
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


