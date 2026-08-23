import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-insurance-login',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './insurance-login.html',
  styleUrls: ['./insurance-login.css']
})
export class InsuranceLogin implements OnInit {
  checkingAccount = true;

  constructor(
    private http: HttpClient,
    private router: Router,
    private alertService: AlertService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;

    const currentUserRaw = sessionStorage.getItem('currentUser');
    if (!currentUserRaw) {
      this.router.navigate(['/website/user'], { queryParams: { redirectTo: 'insurance' } });
      return;
    }

    try {
      const currentUser = JSON.parse(currentUserRaw);
      if (!currentUser?.accountNumber) {
        throw new Error('Missing account number');
      }
      this.checkAccountAadhaar(currentUser.accountNumber);
    } catch {
      this.checkingAccount = false;
      this.alertService.userError('Session Error', 'Please login again to access insurance.');
      this.router.navigate(['/website/user'], { queryParams: { redirectTo: 'insurance' } });
    }
  }

  private checkAccountAadhaar(accountNumber: string) {
    this.http.get<any>(`${environment.apiBaseUrl}/api/accounts/number/${encodeURIComponent(accountNumber)}`).subscribe({
      next: (account) => {
        this.checkingAccount = false;
        const hasAadhaar = typeof account?.aadharNumber === 'string' && account.aadharNumber.trim().length > 0;
        this.router.navigate(['/website/insurance'], {
          queryParams: hasAadhaar ? {} : { apply: 'true' }
        });
      },
      error: () => {
        this.checkingAccount = false;
        this.alertService.userError('Account Error', 'Unable to load your account details. Please try again.');
        this.router.navigate(['/website/user'], { queryParams: { redirectTo: 'insurance' } });
      }
    });
  }
}

