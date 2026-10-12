import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

interface MonitoringEvent {
  id: number;
  timestamp: string;
  category: string;
  source: string;
  message: string;
  durationMs: number | null;
}

interface MonitoringAlert {
  code: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  title: string;
  message: string;
}

interface DatabasePool {
  status: string;
  activeConnections: number | null;
  idleConnections: number | null;
  totalConnections: number | null;
  maximumPoolSize: number | null;
  threadsAwaitingConnection: number | null;
  utilization: number | null;
}

interface MonitoringSnapshot {
  status: string;
  generatedAt: string;
  windowMinutes: number;
  retention: string;
  metrics: {
    uptimeSeconds: number;
    apiErrors5m: number;
    slowDatabaseOperations5m: number;
    scheduledJobFailures5m: number;
    notificationFailures5m: number;
    slowQueryThresholdMs: number;
    databasePool: DatabasePool;
  };
  alerts: MonitoringAlert[];
  events: MonitoringEvent[];
}

@Component({
  selector: 'app-system-monitoring',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './system-monitoring.html',
  styleUrls: ['./system-monitoring.css']
})
export class SystemMonitoringComponent implements OnInit, OnDestroy {
  snapshot: MonitoringSnapshot | null = null;
  isLoading = true;
  isRefreshing = false;
  errorMessage = '';
  private refreshTimer: ReturnType<typeof setInterval> | null = null;

  constructor(
    private http: HttpClient,
    private alertService: AlertService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadSnapshot();
    this.refreshTimer = setInterval(() => this.loadSnapshot(), 30000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  loadSnapshot(): void {
    if (this.isRefreshing) return;
    this.isRefreshing = true;
    this.http.get<MonitoringSnapshot>(`${environment.apiBaseUrl}/api/admin/system-monitoring?limit=100`)
      .subscribe({
        next: snapshot => {
          this.snapshot = snapshot;
          this.errorMessage = '';
          this.isLoading = false;
          this.isRefreshing = false;
        },
        error: () => {
          this.errorMessage = 'Monitoring data is temporarily unavailable. Retry to reconnect.';
          this.isLoading = false;
          this.isRefreshing = false;
          this.alertService.error('Monitoring unavailable', this.errorMessage);
        }
      });
  }

  goBack(): void {
    void this.router.navigateByUrl('/admin/dashboard');
  }

  formatUptime(seconds: number): string {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return days > 0 ? `${days}d ${hours}h ${minutes}m` : `${hours}h ${minutes}m`;
  }

  formatEventTime(timestamp: string): string {
    return new Date(timestamp).toLocaleString();
  }

  formatUtilization(value: number | null): string {
    return value === null ? 'N/A' : `${Math.round(value * 100)}%`;
  }
}
