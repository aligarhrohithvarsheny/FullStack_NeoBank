# Angular Frontend - Quick Integration Guide

## 🚀 Getting Started with Backend APIs

This guide helps Angular developers quickly integrate with the Funds Allocation backend APIs.

---

## 📦 Angular Service Setup

### 1. Create Service Classes
```bash
ng generate service services/funds-allocation
ng generate service services/allocation-utilization
ng generate service services/allocation-metrics
```

### 2. Base Service Configuration
```typescript
// src/app/services/base-api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class BaseApiService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  get baseUrl(): string {
    return this.apiUrl;
  }
}
```

---

## 🔧 Service Implementation Examples

### FundsAllocationService
```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FundsAllocationService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  // Create allocation
  createAllocation(request: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/hod/allocate-funds`, request);
  }

  // Get all allocations
  getAllAllocations(
    status?: string,
    managerId?: number,
    page: number = 0,
    size: number = 20
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (status) params = params.set('status', status);
    if (managerId) params = params.set('managerId', managerId.toString());

    return this.http.get(`${this.apiUrl}/hod/allocations`, { params });
  }

  // Get allocation details
  getAllocationDetails(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/hod/allocations/${allocationId}`);
  }

  // Update allocation
  updateAllocation(allocationId: number, request: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/hod/allocations/${allocationId}`, request);
  }

  // Cancel allocation
  cancelAllocation(allocationId: number, request: any): Observable<any> {
    return this.http.delete(`${this.apiUrl}/hod/allocations/${allocationId}/cancel`, { body: request });
  }

  // Pause allocation
  pauseAllocation(allocationId: number, reason: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/hod/allocations/${allocationId}/pause`, { reason });
  }

  // Resume allocation
  resumeAllocation(allocationId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/hod/allocations/${allocationId}/resume`, {});
  }

  // Check branch status
  checkBranchStatus(managerId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/hod/branch-allocation-status/${managerId}`);
  }

  // Get allocations report
  getAllocationsReport(fromDate?: string, toDate?: string, managerId?: number): Observable<any> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (managerId) params = params.set('managerId', managerId.toString());

    return this.http.get(`${this.apiUrl}/hod/allocations-report`, { params });
  }

  // Get manager allocation limits
  getManagerLimits(managerId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/manager/allocation-limits/${managerId}`);
  }

  // Validate product types
  validateProductTypes(productTypes: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/allocations/validate-products`, { productTypes });
  }
}
```

### AllocationUtilizationService
```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AllocationUtilizationService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  // Debit funds
  debitFunds(request: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/manager/utilization/debit`, request);
  }

  // Credit funds
  creditFunds(request: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/manager/utilization/credit`, request);
  }

  // Get transaction history
  getHistory(allocationId: number, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get(`${this.apiUrl}/manager/utilization/${allocationId}/history`, { params });
  }

  // Get by product
  getByProduct(allocationId: number, productType: string, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('productType', productType)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get(`${this.apiUrl}/manager/utilization/${allocationId}/by-product`, { params });
  }

  // Get debits only
  getDebits(allocationId: number, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get(`${this.apiUrl}/manager/utilization/${allocationId}/debits`, { params });
  }

  // Get credits only
  getCredits(allocationId: number, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get(`${this.apiUrl}/manager/utilization/${allocationId}/credits`, { params });
  }

  // Get product summary
  getProductSummary(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/manager/utilization/${allocationId}/product-summary`);
  }

  // Validate debit
  validateDebit(allocationId: number, amount: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/manager/utilization/validate-debit`, { allocationId, amount });
  }
}
```

### AllocationMetricsService
```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AllocationMetricsService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  // Get metrics summary
  getMetricsSummary(allocationId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/manager/allocations/${allocationId}/metrics`);
  }

  // Get manager overview
  getManagerOverview(managerId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/manager/allocations/summary/${managerId}`);
  }

  // Get trend data
  getTrendData(allocationId: number, days: number = 7): Observable<any> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get(`${this.apiUrl}/manager/allocations/${allocationId}/trend`, { params });
  }

  // Get HOD report
  getHodReport(fromDate?: string, toDate?: string, managerId?: number): Observable<any> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (managerId) params = params.set('managerId', managerId.toString());

    return this.http.get(`${this.apiUrl}/hod/metrics/allocations-report`, { params });
  }

  // Get top allocations
  getTopAllocations(limit: number = 10): Observable<any> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get(`${this.apiUrl}/hod/metrics/top-allocations`, { params });
  }

  // Get low allocations
  getLowAllocations(limit: number = 10): Observable<any> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get(`${this.apiUrl}/hod/metrics/low-allocations`, { params });
  }

  // Get expiring allocations
  getExpiringAllocations(days: number = 30): Observable<any> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get(`${this.apiUrl}/hod/metrics/expiring-allocations`, { params });
  }

  // Get manager dashboard
  getManagerDashboard(managerId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/manager/dashboard/${managerId}`);
  }

  // Get HOD dashboard
  getHodDashboard(fromDate?: string, toDate?: string): Observable<any> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get(`${this.apiUrl}/hod/dashboard`, { params });
  }

  // Get metrics history
  getMetricsHistory(allocationId: number, fromDate: string, toDate: string): Observable<any> {
    const params = new HttpParams()
      .set('fromDate', fromDate)
      .set('toDate', toDate);

    return this.http.get(`${this.apiUrl}/manager/allocations/${allocationId}/metrics-history`, { params });
  }
}
```

---

## 🎨 Component Structure

### HOD Module Structure
```
src/app/
├── modules/
│   ├── hod/
│   │   ├── components/
│   │   │   ├── allocation-management/
│   │   │   │   ├── allocation-list.component.ts
│   │   │   │   ├── allocation-form.component.ts
│   │   │   │   ├── allocation-details.component.ts
│   │   │   │   └── allocation-filters.component.ts
│   │   │   ├── dashboard/
│   │   │   │   ├── hod-dashboard.component.ts
│   │   │   │   ├── allocation-report.component.ts
│   │   │   │   ├── top-performers.component.ts
│   │   │   │   └── expiring-alerts.component.ts
│   │   │   └── shared/
│   │   ├── hod-routing.module.ts
│   │   └── hod.module.ts
│   └── manager/
│       ├── components/
│       │   ├── my-allocations/
│       │   │   └── my-allocations.component.ts
│       │   ├── utilization/
│       │   │   ├── transaction-log.component.ts
│       │   │   ├── product-breakdown.component.ts
│       │   │   └── filters.component.ts
│       │   ├── metrics/
│       │   │   ├── metrics-widget.component.ts
│       │   │   ├── trend-chart.component.ts
│       │   │   └── utilization-gauge.component.ts
│       │   └── dashboard/
│       │       └── manager-dashboard.component.ts
│       ├── manager-routing.module.ts
│       └── manager.module.ts
├── services/
│   ├── funds-allocation.service.ts
│   ├── allocation-utilization.service.ts
│   ├── allocation-metrics.service.ts
│   └── base-api.service.ts
└── models/
    ├── funds-allocation.model.ts
    ├── allocation-utilization.model.ts
    └── allocation-metrics.model.ts
```

### Model Classes
```typescript
// src/app/models/funds-allocation.model.ts
export interface FundsAllocation {
  id: number;
  allocationId: string;
  managerId: number;
  managerName: string;
  branchName: string;
  allocatedAmount: number;
  allocationType: string;
  productTypes: string;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
  currentBalance: number;
  totalDebited: number;
  totalCredited: number;
  totalUtilized: number;
  validFrom: string;
  validTill: string;
  createdAt: string;
  updatedAt: string;
}

export interface AllocationUtilization {
  id: number;
  utilizationId: string;
  allocationId: number;
  transactionType: 'DEBIT' | 'CREDIT';
  amount: number;
  remainingBalance: number;
  linkedTransactionId: string;
  productType: string;
  userAccountNumber: string;
  userName: string;
  transactionDate: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
  description: string;
}

export interface AllocationMetrics {
  allocationId: string;
  totalAllocated: number;
  totalDebited: number;
  totalCredited: number;
  currentBalance: number;
  utilizationPercentage: string;
  debitCount: number;
  creditCount: number;
  totalTransactions: number;
  lastTransactionDate: string;
  lastUpdated: string;
  productBreakdown: ProductBreakdown[];
}

export interface ProductBreakdown {
  productType: string;
  debited: number;
  percentage: string;
}
```

---

## 📊 Component Implementation Example

### HOD Allocation List Component
```typescript
import { Component, OnInit } from '@angular/core';
import { FundsAllocationService } from '@app/services/funds-allocation.service';
import { FundsAllocation } from '@app/models/funds-allocation.model';

@Component({
  selector: 'app-allocation-list',
  templateUrl: './allocation-list.component.html',
  styleUrls: ['./allocation-list.component.css']
})
export class AllocationListComponent implements OnInit {
  allocations: FundsAllocation[] = [];
  loading = false;
  selectedStatus = 'ACTIVE';
  selectedManagerId: number | null = null;
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;

  constructor(private allocationService: FundsAllocationService) { }

  ngOnInit(): void {
    this.loadAllocations();
  }

  loadAllocations(): void {
    this.loading = true;
    this.allocationService.getAllAllocations(
      this.selectedStatus || undefined,
      this.selectedManagerId || undefined,
      this.currentPage,
      this.pageSize
    ).subscribe(
      (response) => {
        this.allocations = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      (error) => {
        console.error('Error loading allocations:', error);
        this.loading = false;
      }
    );
  }

  onStatusChange(status: string): void {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.loadAllocations();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadAllocations();
  }

  cancelAllocation(allocationId: number, reason: string): void {
    this.allocationService.cancelAllocation(allocationId, { reason, hodAdminId: 1 })
      .subscribe(
        () => {
          alert('Allocation cancelled');
          this.loadAllocations();
        },
        (error) => console.error('Error cancelling:', error)
      );
  }
}
```

### Manager Metrics Widget Component
```typescript
import { Component, OnInit, Input } from '@angular/core';
import { AllocationMetricsService } from '@app/services/allocation-metrics.service';
import { AllocationMetrics } from '@app/models/allocation-metrics.model';

@Component({
  selector: 'app-metrics-widget',
  template: `
    <div class="metrics-card" *ngIf="metrics">
      <h3>{{ metrics.allocationId }}</h3>
      <div class="metric-row">
        <span>Allocated:</span>
        <strong>₹{{ metrics.totalAllocated | number }}</strong>
      </div>
      <div class="metric-row">
        <span>Used:</span>
        <strong>₹{{ metrics.totalDebited | number }}</strong>
      </div>
      <div class="metric-row">
        <span>Balance:</span>
        <strong>₹{{ metrics.currentBalance | number }}</strong>
      </div>
      <div class="metric-row utilization">
        <span>Utilization:</span>
        <strong [ngClass]="getUtilizationClass()">
          {{ metrics.utilizationPercentage }}
        </strong>
      </div>
      <div class="progress-bar">
        <div class="progress" [style.width]="metrics.utilizationPercentage"></div>
      </div>
    </div>
  `
})
export class MetricsWidgetComponent implements OnInit {
  @Input() allocationId: number;
  @Input() refreshInterval = 30000; // 30 seconds

  metrics: AllocationMetrics;
  loading = false;

  constructor(private metricsService: AllocationMetricsService) { }

  ngOnInit(): void {
    this.loadMetrics();
    setInterval(() => this.loadMetrics(), this.refreshInterval);
  }

  loadMetrics(): void {
    this.loading = true;
    this.metricsService.getMetricsSummary(this.allocationId)
      .subscribe(
        (response) => {
          this.metrics = response.metrics;
          this.loading = false;
        },
        (error) => {
          console.error('Error loading metrics:', error);
          this.loading = false;
        }
      );
  }

  getUtilizationClass(): string {
    const percent = parseFloat(this.metrics.utilizationPercentage);
    if (percent < 30) return 'low';
    if (percent < 70) return 'medium';
    return 'high';
  }
}
```

---

## 🔄 Data Flow & Caching

### Service with Caching
```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, shareReplay } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AllocationMetricsService {
  private cache = new Map<string, Observable<any>>();
  private cacheDuration = 60000; // 1 minute

  constructor(private http: HttpClient) { }

  getMetricsSummary(allocationId: number): Observable<any> {
    const cacheKey = `metrics-${allocationId}`;
    
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey)!;
    }

    const request$ = this.http.get(`/api/manager/allocations/${allocationId}/metrics`)
      .pipe(
        tap(response => {
          // Clear cache after duration
          setTimeout(() => this.cache.delete(cacheKey), this.cacheDuration);
        }),
        shareReplay(1)
      );

    this.cache.set(cacheKey, request$);
    return request$;
  }
}
```

---

## 🚨 Error Handling

### Global Error Interceptor
```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'Unknown error occurred';

        if (error.error instanceof ErrorEvent) {
          // Client-side error
          errorMessage = error.error.message;
        } else {
          // Server-side error
          errorMessage = error.error?.message || error.message;
        }

        console.error(errorMessage);
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
```

---

## 🧪 Unit Test Example

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FundsAllocationService } from './funds-allocation.service';

describe('FundsAllocationService', () => {
  let service: FundsAllocationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [FundsAllocationService]
    });

    service = TestBed.inject(FundsAllocationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should create allocation', () => {
    const mockRequest = {
      amount: 1000000,
      branchManagerId: 5,
      allocationType: 'GENERAL',
      productTypes: 'GOLD_LOAN,DEPOSITS'
    };

    const mockResponse = {
      success: true,
      allocationId: 'FA-2025-00001'
    };

    service.createAllocation(mockRequest).subscribe(response => {
      expect(response.success).toBe(true);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/hod/allocate-funds');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  afterEach(() => {
    httpMock.verify();
  });
});
```

---

## 📝 Module Setup

### AppModule Configuration
```typescript
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppComponent } from './app.component';
import { ErrorInterceptor } from './interceptors/error.interceptor';
import { HodModule } from './modules/hod/hod.module';
import { ManagerModule } from './modules/manager/manager.module';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    HttpClientModule,
    HodModule,
    ManagerModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

---

## ✅ Integration Checklist

- [ ] Create Angular services for each backend service
- [ ] Define TypeScript models matching backend responses
- [ ] Create HOD module with components
- [ ] Create Manager module with components
- [ ] Setup HTTP interceptors for error handling
- [ ] Implement caching strategy
- [ ] Add pagination support
- [ ] Create dashboard components
- [ ] Add WebSocket integration for real-time updates
- [ ] Write unit tests for services
- [ ] Write component tests
- [ ] Test API integration with real backend

---

## 🎯 Ready to Code!

You now have:
- ✅ Complete API reference
- ✅ Service implementation patterns
- ✅ Component structure examples
- ✅ Model definitions
- ✅ Error handling patterns
- ✅ Caching strategies
- ✅ Testing examples

**Start building Angular components with confidence!**
