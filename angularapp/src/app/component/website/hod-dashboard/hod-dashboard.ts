import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface CityOperation {
  city: string;
  turnover: number;
  profit: number;
  operations: number;
  status: 'ACTIVE' | 'PLANNING';
  staffIds: string[];
}

@Component({
  selector: 'app-hod-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './hod-dashboard.html',
  styleUrls: ['./hod-dashboard.css']
})
export class HodDashboard implements OnInit {
  staff: any[] = [];
  overview: any = null;
  cities: CityOperation[] = [];
  selectedCity: CityOperation | null = null;
  newCity = { city: '', turnover: 0, profit: 0, operations: 0 };
  errorMessage = '';
  lastRefreshed = '';
  private refreshTimer?: ReturnType<typeof setInterval>;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.loadCities();
    this.refreshOverview();
    this.refreshTimer = setInterval(() => this.refreshOverview(), 30000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  refreshOverview(): void {
    this.http.get<any>(`${environment.apiBaseUrl}/api/admins/hod-overview`).subscribe({
      next: overview => {
        this.overview = overview;
        this.lastRefreshed = overview?.refreshedAt || new Date().toISOString();
        this.errorMessage = '';
      },
      error: () => this.errorMessage = 'Live bank data is temporarily unavailable.'
    });
    this.http.get<any[]>(`${environment.apiBaseUrl}/api/admins/staff`).subscribe({
      next: staff => this.staff = staff || [],
      error: () => this.errorMessage = 'Staff data is temporarily unavailable.'
    });
  }

  get totalTurnover(): number { return this.cities.reduce((sum, city) => sum + Number(city.turnover || 0), 0); }
  get totalProfit(): number { return this.cities.reduce((sum, city) => sum + Number(city.profit || 0), 0); }
  get totalOperations(): number { return this.cities.reduce((sum, city) => sum + Number(city.operations || 0), 0); }

  addCity(): void {
    const city = this.newCity.city.trim();
    if (!city) return;
    this.cities = [...this.cities, { ...this.newCity, city, status: 'PLANNING', staffIds: [] }];
    this.saveCities();
    this.newCity = { city: '', turnover: 0, profit: 0, operations: 0 };
  }

  selectCity(city: CityOperation): void { this.selectedCity = city; }

  updateCity(): void {
    if (!this.selectedCity) return;
    this.saveCities();
  }

  toggleStaff(city: CityOperation, person: any): void {
    const staffId = String(person.id || person.email);
    const assigned = city.staffIds || [];
    city.staffIds = assigned.includes(staffId)
      ? assigned.filter(id => id !== staffId)
      : [...assigned, staffId];
    this.updateCity();
  }

  isStaffAssigned(city: CityOperation, person: any): boolean {
    return (city.staffIds || []).includes(String(person.id || person.email));
  }

  removeCity(city: CityOperation): void {
    this.cities = this.cities.filter(item => item !== city);
    if (this.selectedCity === city) this.selectedCity = null;
    this.saveCities();
  }

  logout(): void {
    sessionStorage.removeItem('admin');
    sessionStorage.removeItem('userRole');
    sessionStorage.removeItem('adminLoginTime');
    this.router.navigate(['/hod/login']);
  }

  private loadCities(): void {
    try { this.cities = JSON.parse(localStorage.getItem('hodCityOperations') || '[]'); } catch { this.cities = []; }
  }

  private saveCities(): void { localStorage.setItem('hodCityOperations', JSON.stringify(this.cities)); }
}