import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user.html',
  styleUrls: ['./user.css']
})
export class User {
  // For login
  loginUserId: string = '';
  loginPassword: string = '';
  directScreen: string = 'dashboard';
  captchaChecked: boolean = false;

  // For create account
  signupName: string = '';
  signupEmail: string = '';
  signupMobile: string = '';
  signupPassword: string = '';
  confirmPassword: string = '';
  termsAccepted: boolean = false;

  currentPage: string = 'login'; // 'login' | 'signup'

  constructor(private router: Router) {}

  login() {
    if (!this.loginUserId || !this.loginPassword) {
      alert('Please enter User ID and Password ❌');
      return;
    }
    
    if (!this.captchaChecked) {
      alert('Please verify that you are not a robot ❌');
      return;
    }

    // Simple validation - in real app, this would check against database
    if (this.loginUserId === 'admin' && this.loginPassword === 'admin123') {
      alert(`Login successful ✅ Welcome Admin`);
      this.router.navigate(['/admin/dashboard']);
    } else if (this.loginUserId && this.loginPassword) {
      alert(`Login successful ✅ Welcome User`);
      this.router.navigate(['/website/userdashboard']);
    } else {
      alert('Invalid User ID or Password ❌');
    }
  }

  createAccount() {
    if (!this.signupName || !this.signupEmail || !this.signupMobile || !this.signupPassword || !this.confirmPassword) {
      alert('Please fill in all fields ❌');
      return;
    }

    if (this.signupPassword !== this.confirmPassword) {
      alert('Passwords do not match ❌');
      return;
    }

    if (!this.termsAccepted) {
      alert('Please accept Terms & Conditions ❌');
      return;
    }

    alert(`Account created successfully ✅ Welcome ${this.signupName}`);
    this.router.navigate(['/website/userdashboard']);
  }

  switchToLogin() {
    this.currentPage = 'login';
  }

  switchToSignup() {
    this.currentPage = 'signup';
  }
}
