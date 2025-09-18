import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-kycupdate',
  standalone: true,    // ✅ standalone component
  imports: [FormsModule, CommonModule],
  templateUrl: './kycupdate.html',
  styleUrls: ['./kycupdate.css']
})
export class Kycupdate { // ✅ standalone component
  constructor(private router: Router) {}
  panNumber: string = '';
  name: string = '';
  status: string = 'Not Requested'; // Default status
  isRequested: boolean = false;

  // ✅ Simulate sending request to admin
  requestToAdmin() {
    if (!this.panNumber) {
      alert('Please enter a PAN number!');
      return;
    }

    this.name = 'John Doe'; // In real case, fetch from backend
    this.status = 'Pending Approval';
    this.isRequested = true;
  }

  // ✅ Simulate admin approval
  approveByAdmin() {
    if (this.isRequested) {
      this.status = 'Approved ✅';
    }
  }
  goBack() {
    this.router.navigate(['/website/userdashboard']); // ✅ Navigate back to the user dashboard
  }
}
