import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MerchantOnboardingService } from '../../../service/merchant-onboarding.service';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-agent-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './agent-login.html',
  styleUrls: ['./agent-login.css'],
  encapsulation: ViewEncapsulation.None
})
export class AgentLogin {
  loginMode: 'email' | 'mobile' = 'email';
  email = '';
  mobile = '';
  password = '';
  loading = false;
  errorMsg = '';

  constructor(
    private router: Router,
    private merchantService: MerchantOnboardingService,
    private alertService: AlertService
  ) {}

  toggleMode() {
    this.loginMode = this.loginMode === 'email' ? 'mobile' : 'email';
    this.errorMsg = '';
  }

  login() {
    this.errorMsg = '';
    if (this.loginMode === 'email' && !this.email) {
      this.errorMsg = 'Please enter your email';
      return;
    }
    if (this.loginMode === 'mobile' && !this.mobile) {
      this.errorMsg = 'Please enter your mobile number';
      return;
    }
    if (!this.password) {
      this.errorMsg = 'Please enter your password';
      return;
    }

    this.loading = true;
    const credentials: any = { password: this.password };
    if (this.loginMode === 'email') {
      credentials.email = this.email;
    } else {
      credentials.mobile = this.mobile;
    }

    this.merchantService.agentLogin(credentials).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) {
          sessionStorage.setItem('agent', JSON.stringify(res.agent));
          this.alertService.success('Login Successful', 'Welcome, ' + res.agent.name);
          this.router.navigate(['/website/agent-dashboard']);
        } else {
          this.errorMsg = res.error || 'Login failed';
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMsg = err.error?.error || 'Login failed. Please check your credentials.';
      }
    });
  }

  goBack() {
    this.router.navigate(['/website/landing']);
  }
}
