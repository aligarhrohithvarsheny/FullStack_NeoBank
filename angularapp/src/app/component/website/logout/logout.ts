import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-logout',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './logout.html',
  styleUrls: ['./logout.css']
})
export class Logout implements OnInit {
  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    // Clear session storage
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.clear();
      localStorage.removeItem('currentUser');
      
      // Auto redirect to login after 5 seconds
      setTimeout(() => {
        this.router.navigate(['/website/user']);
      }, 5000);
    }
  }

  goToLogin() {
    this.router.navigate(['/website/user']);
  }
}

