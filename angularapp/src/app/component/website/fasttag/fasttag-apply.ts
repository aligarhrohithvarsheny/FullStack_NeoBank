import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FasttagService } from '../../../service/fasttag.service';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-fasttag-apply',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fasttag-apply.html',
  styleUrls: ['./fasttag-apply.css'],
  encapsulation: ViewEncapsulation.None
})
export class FasttagApply implements OnInit {
  vehicleDetails = '';
  vehicleNumber = '';
  aadharNumber = '';
  panNumber = '';
  dob = '';
  vehicleType = 'Car';
  amount = 200;
  bank = '';

  constructor(
    private router: Router,
    private fasttagService: FasttagService,
    private alertService: AlertService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {}

  submit() {
    let user: any = { id: 'guest', username: 'Guest' };
    if (isPlatformBrowser(this.platformId) && typeof sessionStorage !== 'undefined') {
      try {
        const currentUserRaw = sessionStorage.getItem('currentUser');
        user = currentUserRaw ? JSON.parse(currentUserRaw) : user;
      } catch (e) {
        console.warn('Could not read sessionStorage currentUser', e);
      }
    }
    if (!this.vehicleNumber || !this.aadharNumber || !this.panNumber) {
      this.alertService.error('Validation Error', 'Please fill vehicle number, Aadhar and PAN');
      return;
    }

    this.fasttagService.apply({
      userId: user.id || 'guest',
      userName: user.username || '',
      vehicleDetails: this.vehicleDetails,
      vehicleNumber: this.vehicleNumber,
      aadharNumber: this.aadharNumber,
      panNumber: this.panNumber,
      dob: this.dob,
      vehicleType: this.vehicleType,
      amount: this.amount,
      bank: this.bank
    } as any).subscribe({
      next: (res) => {
        this.alertService.success('Application Submitted', 'FASTag application submitted successfully');
        this.router.navigate(['/website/fasttag']);
      },
      error: (err) => {
        console.error('FASTag apply error', err);
        const msg = err?.error?.message || 'Unable to submit FASTag application';
        this.alertService.error('Application Failed', msg);
      }
    });
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }
}
