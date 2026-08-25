import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FamilyBankingService, MinorApplication } from '../../../service/family-banking.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({ selector: 'app-family-banking-admin', standalone: true, imports: [CommonModule], templateUrl: './family-banking-admin.html', styleUrls: ['./family-banking-admin.css'] })
export class FamilyBankingAdminComponent implements OnInit {
  applications: MinorApplication[] = []; message = ''; error = ''; loading = false; busy = false;
  constructor(private family: FamilyBankingService, private http: HttpClient) {}
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true; this.error = '';
    const email = sessionStorage.getItem('adminEmail') || '';
    this.http.get<MinorApplication[]>(`${environment.apiBaseUrl}/api/family/admin/minor-applications`, { headers: { 'X-Admin-Email': email } }).subscribe({ next: value => { this.applications = value; this.loading = false; }, error: () => { this.error = 'Unable to load applications'; this.loading = false; } });
  }
  review(app: MinorApplication, approve: boolean): void { const email = sessionStorage.getItem('adminEmail') || ''; this.busy = true; this.error = ''; this.http.post(`${environment.apiBaseUrl}/api/family/admin/minor-applications/${app.id}/review`, { userId: 0, approve }, { headers: { 'X-Admin-Email': email } }).subscribe({ next: () => { this.busy = false; this.message = approve ? 'Application approved' : 'Application declined'; this.load(); }, error: e => { this.busy = false; this.error = e?.error?.message || 'Review failed'; } }); }
}
