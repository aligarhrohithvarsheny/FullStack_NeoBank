import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class Dashboard {
goTo(arg0: string) {
throw new Error('Method not implemented.');
}
  constructor(private router: Router) {}
navigateTo(path: string) {
    this.router.navigate([`/admin/${path}`]); // ✅ ensures /admin/kyc, /admin/users, etc.
  }
  logout() {
    alert('Logged out successfully 🚪');
    this.router.navigate(['/admin/login']);
  }

  
}
