import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../environment/environment';

interface GuestInsuranceStatus {
  applicationNumber: string;
  policyName: string;
  policyType: string;
  status: string;
  createdAt: string;
}

@Component({
  selector: 'app-insurance-guest',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './insurance-guest.html',
  styleUrl: './insurance-guest.css'
})
export class InsuranceGuest implements OnInit {
  policies: any[] = [];
  applications: GuestInsuranceStatus[] = [];
  applicantName = '';
  email = '';
  phone = '';
  policyId: number | null = null;
  trackingEmail = '';
  submittedApplicationNumber = '';
  applyError = '';
  trackingError = '';
  applying = false;
  tracking = false;

  constructor(
    private readonly http: HttpClient,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/insurance/policies`).subscribe({
      next: policies => this.policies = (policies || []).filter(policy => policy.status === 'ACTIVE'),
      error: () => this.applyError = 'Insurance policies are temporarily unavailable. Please try again later.'
    });
  }

  submitApplication(): void {
    if (this.applying) return;
    this.applyError = '';
    this.submittedApplicationNumber = '';
    this.applying = true;
    this.http.post<any>(`${environment.apiBaseUrl}/api/insurance/guest-applications`, {
      applicantName: this.applicantName.trim(),
      email: this.email.trim().toLowerCase(),
      phone: this.phone.trim(),
      policyId: this.policyId
    }).subscribe({
      next: response => {
        this.applying = false;
        if (!response?.success || !response?.applicationNumber) {
          this.applyError = response?.message || 'Unable to submit your application.';
          return;
        }
        this.submittedApplicationNumber = response.applicationNumber;
        this.trackingEmail = this.email.trim().toLowerCase();
        this.trackApplications();
      },
      error: error => {
        this.applying = false;
        this.applyError = error.error?.message || 'Unable to submit your application.';
      }
    });
  }

  trackApplications(): void {
    const email = this.trackingEmail.trim().toLowerCase();
    if (!email || this.tracking) return;
    this.trackingError = '';
    this.applications = [];
    this.tracking = true;
    this.http.get<GuestInsuranceStatus[]>(`${environment.apiBaseUrl}/api/insurance/guest-applications/track`, {
      params: { email }
    }).subscribe({
      next: applications => {
        this.applications = applications || [];
        this.tracking = false;
      },
      error: error => {
        this.trackingError = error.error?.message || 'Unable to check applications right now.';
        this.tracking = false;
      }
    });
  }
}
