import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

type CustomerAccountType = 'SAVINGS' | 'SALARY' | 'CURRENT';

interface ActivationCodeResponse {
  activationCode: string;
  accountType: CustomerAccountType;
  expiresAt: string;
}

@Component({
  selector: 'app-customer-access',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './customer-access.html',
  styleUrl: './customer-access.css'
})
export class CustomerAccess {
  accountType: CustomerAccountType = 'SAVINGS';
  customerId = '';
  issuedCode: ActivationCodeResponse | null = null;
  isIssuing = false;
  error = '';

  constructor(private readonly http: HttpClient) {}

  issueCode(): void {
    this.error = '';
    this.issuedCode = null;
    const customerId = this.customerId.trim();
    if (!customerId) {
      this.error = 'Enter the customer ID from the approved account record.';
      return;
    }

    this.isIssuing = true;
    this.http.post<ActivationCodeResponse>(
      `${environment.apiBaseUrl}/api/customer-access/activation-codes`,
      { accountType: this.accountType, customerId },
      { headers: this.adminHeaders() }
    ).subscribe({
      next: response => {
        this.issuedCode = response;
        this.isIssuing = false;
      },
      error: error => {
        this.error = error.error?.message || 'Unable to issue an activation code for this account.';
        this.isIssuing = false;
      }
    });
  }

  clearIssuedCode(): void {
    this.issuedCode = null;
  }

  private adminHeaders(): HttpHeaders {
    const token = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('adminSessionToken') || '' : '';
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }
}
