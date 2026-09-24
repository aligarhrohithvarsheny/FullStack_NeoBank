import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface CityOperation {
  id?: number;
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
  staffSearch = '';
  selectedStaff: any = null;
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

  get filteredStaff(): any[] {
    const query = this.staffSearch.trim().toLowerCase();
    if (!query) return this.staff;
    return this.staff.filter(person => [person.name, person.email, person.role, person.assignedCity, person.employeeId]
      .some(value => String(value || '').toLowerCase().includes(query)));
  }

  addCity(): void {
    const city = this.newCity.city.trim();
    if (!city) return;
    this.http.post<CityOperation>(`${environment.apiBaseUrl}/api/admins/hod/cities`, {
      ...this.newCity, city, status: 'PLANNING'
    }).subscribe({
      next: saved => {
        this.cities = [...this.cities, saved];
        this.newCity = { city: '', turnover: 0, profit: 0, operations: 0 };
      },
      error: err => this.errorMessage = err.error?.message || 'Unable to add city'
    });
  }

  selectCity(city: CityOperation): void { this.selectedCity = city; }

  updateCity(): void {
    if (!this.selectedCity) return;
    if (!this.selectedCity.id) return;
    this.http.put<CityOperation>(`${environment.apiBaseUrl}/api/admins/hod/cities/${this.selectedCity.id}`, this.selectedCity)
      .subscribe({ next: saved => this.selectedCity = saved, error: () => this.errorMessage = 'Unable to update city' });
  }

  toggleStaff(city: CityOperation, person: any): void {
    const nextCity = this.isStaffAssigned(city, person) ? '' : city.city;
    if (nextCity && person.assignedCity && person.assignedCity.toLowerCase() !== city.city.toLowerCase()) {
      this.errorMessage = `${person.name || person.email} is already assigned to ${person.assignedCity}`;
      return;
    }
    this.http.put<any>(`${environment.apiBaseUrl}/api/admins/hod/staff/${person.id}/city`, { city: nextCity })
      .subscribe({
        next: updated => {
          const index = this.staff.findIndex(item => item.id === person.id);
          if (index >= 0) this.staff[index] = updated;
          this.errorMessage = '';
        },
        error: err => this.errorMessage = err.error?.message || 'Unable to update staff location'
      });
  }

  isStaffAssigned(city: CityOperation, person: any): boolean {
    return !!person.assignedCity && person.assignedCity.toLowerCase() === city.city.toLowerCase();
  }

  canAssignStaff(city: CityOperation, person: any): boolean {
    return !person.assignedCity || this.isStaffAssigned(city, person);
  }

  showStaffProfile(person: any): void { this.selectedStaff = person; }

  removeCity(city: CityOperation): void {
    if (!city.id) return;
    this.http.delete(`${environment.apiBaseUrl}/api/admins/hod/cities/${city.id}`).subscribe({
      next: () => {
        this.cities = this.cities.filter(item => item.id !== city.id);
        if (this.selectedCity?.id === city.id) this.selectedCity = null;
        this.refreshOverview();
      },
      error: () => this.errorMessage = 'Unable to remove city'
    });
  }

  logout(): void {
    sessionStorage.removeItem('admin');
    sessionStorage.removeItem('userRole');
    sessionStorage.removeItem('adminLoginTime');
    this.router.navigate(['/hod/login']);
  }

  private loadCities(): void {
    this.http.get<CityOperation[]>(`${environment.apiBaseUrl}/api/admins/hod/cities`).subscribe({
      next: cities => this.cities = cities || [],
      error: () => this.errorMessage = 'City data is temporarily unavailable.'
    });
  }
}