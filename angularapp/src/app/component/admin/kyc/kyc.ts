import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms';

interface KycUser {
  id: number;
  name: string;
  email: string;
  documentUrl: string;
  status: 'Pending' | 'Approved' | 'Rejected';
}

@Component({
  selector: 'app-kyc',
  standalone: true,  // ✅ standalone component
  imports: [CommonModule, FormsModule],
  templateUrl: './kyc.html',
  styleUrls: ['./kyc.css']
})
export class Kyc {
  users: KycUser[] = [
    { id: 1, name: 'Rohith', email: 'rohith@example.com', documentUrl: '/assets/docs/rohith-aadhar.pdf', status: 'Pending' },
    { id: 2, name: 'Sai', email: 'sai@example.com', documentUrl: '/assets/docs/sai-aadhar.pdf', status: 'Pending' },
    { id: 3, name: 'Alice', email: 'alice@example.com', documentUrl: '/assets/docs/alice-aadhar.pdf', status: 'Approved' },
  ];

  constructor(private router: Router) {}

  approve(user: KycUser) {
    user.status = 'Approved';
    alert(` ${user.name}'s KYC Approved`);
  }

  reject(user: KycUser) {
    user.status = 'Rejected';
    alert(` ${user.name}'s KYC Rejected`);
  }

  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }
}
