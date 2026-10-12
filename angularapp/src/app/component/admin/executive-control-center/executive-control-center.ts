import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface FlowPoint {
  label: string;
  deposits: number;
  withdrawals: number;
  transfers: number;
}

interface ExecutiveSummary {
  totals: {
    customers: number;
    activeAccounts: number;
    newRegistrationsToday: number;
    newRegistrationsThisMonth: number;
    newRegistrationsThisYear: number;
    deposits: number;
    withdrawals: number;
    transfers: number;
    pendingApprovals: number;
    pendingRegistrations: number;
    pendingKyc: number;
    failedTransactions: number;
  };
  daily: FlowPoint[];
  monthly: FlowPoint[];
  yearly: FlowPoint[];
}

@Component({
  selector: 'app-executive-control-center',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './executive-control-center.html',
  styleUrl: './executive-control-center.css'
})
export class ExecutiveControlCenter implements OnInit {
  summary: ExecutiveSummary | null = null;
  selectedRange: 'daily' | 'monthly' | 'yearly' = 'daily';
  loading = false;
  summaryError = '';
  health: 'checking' | 'up' | 'down' = 'checking';
  apiResponseMs: number | null = null;
  healthCheckedAt: Date | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.summaryError = '';
    this.http.get<ExecutiveSummary>(`${environment.apiBaseUrl}/api/admin/executive-dashboard/summary`)
      .subscribe({
        next: result => {
          this.summary = result;
          this.loading = false;
        },
        error: error => {
          this.summaryError = error?.error?.message || 'Unable to load executive metrics.';
          this.loading = false;
        }
      });

    const startedAt = performance.now();
    this.health = 'checking';
    this.http.get(`${environment.apiBaseUrl}/api/ping`).subscribe({
      next: () => {
        this.apiResponseMs = Math.round(performance.now() - startedAt);
        this.health = 'up';
        this.healthCheckedAt = new Date();
      },
      error: () => {
        this.apiResponseMs = Math.round(performance.now() - startedAt);
        this.health = 'down';
        this.healthCheckedAt = new Date();
      }
    });
  }

  get chartRows(): FlowPoint[] {
    return this.summary?.[this.selectedRange] || [];
  }

  barHeight(point: FlowPoint, field: 'deposits' | 'withdrawals' | 'transfers'): number {
    const max = Math.max(1, ...this.chartRows.map(row => Math.max(row.deposits, row.withdrawals, row.transfers)));
    return Math.max(point[field] > 0 ? 4 : 0, Math.round(point[field] / max * 100));
  }
}
