# NeoBank Funds Allocation Feature - Backend API Documentation

## Overview
Complete REST API for managing fund allocations from HOD to branch managers with real-time transaction tracking across 6 banking products.

## Backend Implementation Status: ✅ 100% COMPLETE

### Completed Components

#### 1. Database Entities (3 entities)
- **FundsAllocation**: Core allocation records with lifecycle management
- **AllocationUtilization**: Transaction audit trail with debit/credit tracking
- **AllocationRealTimeMetrics**: KPI aggregation for dashboards

#### 2. Repositories (3 repositories with 27+ JPQL queries)
- FundsAllocationRepository
- AllocationUtilizationRepository  
- AllocationMetricsRepository

#### 3. Services (4 services with 30+ business methods)
- **FundsAllocationService**: Allocation CRUD, status management, lifecycle
- **AllocationUtilizationService**: Debit/credit transactions, history, product breakdown
- **AllocationMetricsService**: Real-time KPIs, trending, reporting, dashboards
- **AllocationValidatorService**: Business rule validation, access control, limits

#### 4. REST Controllers (3 controllers with 28 endpoints)
- **FundsAllocationController**: 8 endpoints for HOD allocation management
- **AllocationUtilizationController**: 8 endpoints for transaction management
- **AllocationMetricsController**: 12 endpoints for metrics & dashboards

---

## API Endpoints Reference

### 1. FUNDS ALLOCATION CONTROLLER
**Base URL**: `/api/hod/allocations` or `/api/manager/allocations`

#### 1.1 Create Allocation (HOD)
```
POST /api/hod/allocate-funds
Content-Type: application/json

{
  "amount": 1000000,
  "branchManagerId": 5,
  "allocationType": "GENERAL",
  "productTypes": "GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS",
  "validTill": "2025-12-31",
  "description": "Q4 2025 allocation",
  "hodAdminId": 1
}

Response (201 Created):
{
  "success": true,
  "message": "Funds allocated successfully",
  "allocationId": "FA-2025-00001",
  "allocation": {
    "id": 1,
    "allocationId": "FA-2025-00001",
    "managerId": 5,
    "managerName": "Manager Name",
    "branchName": "Branch Name",
    "allocatedAmount": 1000000,
    "allocationType": "GENERAL",
    "productTypes": "GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS",
    "status": "ACTIVE",
    "currentBalance": 1000000,
    "totalDebited": 0,
    "totalCredited": 0,
    "totalUtilized": 0,
    "validFrom": "2025-01-15",
    "validTill": "2025-12-31",
    "createdAt": "2025-01-15T10:30:00"
  }
}
```

#### 1.2 Get All Allocations (HOD)
```
GET /api/hod/allocations?status=ACTIVE&managerId=5&page=0&size=20

Response (200):
{
  "content": [ /* allocation objects */ ],
  "totalElements": 45,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20
}
```

#### 1.3 Get Allocation Details
```
GET /api/hod/allocations/{allocationId}

Response (200):
{
  "success": true,
  "allocation": { /* full allocation */ },
  "metrics": { /* metrics summary */ },
  "recentTransactions": [ /* last 20 transactions */ ]
}
```

#### 1.4 Update Allocation (HOD)
```
PUT /api/hod/allocations/{allocationId}
Content-Type: application/json

{
  "amount": 1500000,
  "validTill": "2026-01-31",
  "description": "Extended allocation",
  "hodAdminId": 1
}

Response (200):
{
  "success": true,
  "message": "Allocation updated successfully",
  "allocation": { /* updated allocation */ }
}
```

#### 1.5 Cancel Allocation (HOD)
```
DELETE /api/hod/allocations/{allocationId}/cancel
Content-Type: application/json

{
  "reason": "Budget constraints",
  "hodAdminId": 1
}

Response (200):
{
  "success": true,
  "message": "Allocation cancelled successfully",
  "allocation": { /* cancelled allocation */ }
}
```

#### 1.6 Pause Allocation (HOD)
```
POST /api/hod/allocations/{allocationId}/pause
Content-Type: application/json

{
  "reason": "Temporary hold for review"
}

Response (200):
{
  "success": true,
  "message": "Allocation paused successfully",
  "allocation": { /* paused allocation */ }
}
```

#### 1.7 Resume Allocation (HOD)
```
POST /api/hod/allocations/{allocationId}/resume

Response (200):
{
  "success": true,
  "message": "Allocation resumed successfully",
  "allocation": { /* resumed allocation */ }
}
```

#### 1.8 Check Branch Allocation Status
```
GET /api/hod/branch-allocation-status/{managerId}

Response (200):
{
  "success": true,
  "totalActiveAllocations": 1500000,
  "allocationCount": 3,
  "allocations": [ /* active allocations for manager */ ]
}
```

---

### 2. ALLOCATION UTILIZATION CONTROLLER
**Base URL**: `/api/manager/utilization`

#### 2.1 Debit Allocation Funds
```
POST /api/manager/utilization/debit
Content-Type: application/json

{
  "allocationId": 1,
  "amount": 50000,
  "productType": "GOLD_LOAN",
  "userAccountNumber": "ACC123456",
  "userName": "Customer Name",
  "linkedTransactionId": "TXN-2025-12345",
  "description": "Gold Loan Disbursement",
  "performedByAdminId": 5
}

Response (201 Created):
{
  "success": true,
  "message": "Funds debited from allocation successfully",
  "utilizationId": "AU-2025-00001",
  "remainingBalance": 950000,
  "utilization": {
    "id": 1,
    "utilizationId": "AU-2025-00001",
    "transactionType": "DEBIT",
    "amount": 50000,
    "remainingBalance": 950000,
    "productType": "GOLD_LOAN",
    "userName": "Customer Name",
    "transactionDate": "2025-01-15T11:00:00",
    "status": "SUCCESS"
  }
}
```

#### 2.2 Credit Allocation Funds
```
POST /api/manager/utilization/credit
Content-Type: application/json

{
  "allocationId": 1,
  "amount": 10000,
  "productType": "LOAN",
  "userAccountNumber": "ACC123456",
  "userName": "Customer Name",
  "linkedTransactionId": "TXN-2025-12346",
  "description": "Loan Early Repayment",
  "performedByAdminId": 5
}

Response (201 Created):
{
  "success": true,
  "message": "Funds credited to allocation successfully",
  "utilizationId": "AU-2025-00002",
  "remainingBalance": 960000
}
```

#### 2.3 Get Utilization History
```
GET /api/manager/utilization/{allocationId}/history?page=0&size=20

Response (200):
{
  "content": [
    {
      "utilizationId": "AU-2025-00001",
      "transactionType": "DEBIT",
      "amount": 50000,
      "productType": "GOLD_LOAN",
      "transactionDate": "2025-01-15T11:00:00"
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "currentPage": 0
}
```

#### 2.4 Get Utilization by Product
```
GET /api/manager/utilization/{allocationId}/by-product?productType=GOLD_LOAN&page=0&size=20

Response (200):
{
  "content": [ /* GOLD_LOAN transactions only */ ],
  "totalElements": 8,
  "totalPages": 1
}
```

#### 2.5 Get Debit Transactions Only
```
GET /api/manager/utilization/{allocationId}/debits?page=0&size=20
```

#### 2.6 Get Credit Transactions Only
```
GET /api/manager/utilization/{allocationId}/credits?page=0&size=20
```

#### 2.7 Get Product Type Summary
```
GET /api/manager/utilization/{allocationId}/product-summary

Response (200):
{
  "productBreakdown": [
    {
      "productType": "GOLD_LOAN",
      "totalDebited": 150000,
      "totalCredited": 0,
      "debitCount": 3,
      "creditCount": 0,
      "netAmount": 150000
    },
    {
      "productType": "DEPOSITS",
      "totalDebited": 200000,
      "totalCredited": 50000,
      "debitCount": 4,
      "creditCount": 1,
      "netAmount": 150000
    }
  ],
  "totalProducts": 6
}
```

#### 2.8 Pre-Debit Validation
```
POST /api/manager/utilization/validate-debit
Content-Type: application/json

{
  "allocationId": 1,
  "amount": 50000
}

Response (200):
{
  "canProceed": true,
  "allocationStatus": "ACTIVE",
  "isActive": true,
  "isNotExpired": true,
  "daysRemaining": 320,
  "totalAllocated": 1000000,
  "alreadyDebited": 300000,
  "currentBalance": 700000,
  "hasSufficientBalance": true,
  "utilizationPercentage": "30.00%"
}
```

---

### 3. ALLOCATION METRICS CONTROLLER
**Base URL**: `/api/manager/metrics` or `/api/hod/metrics`

#### 3.1 Get Metrics Summary (Manager)
```
GET /api/manager/allocations/{allocationId}/metrics

Response (200):
{
  "success": true,
  "metrics": {
    "allocationId": "FA-2025-00001",
    "managerName": "Manager Name",
    "branchName": "Branch Name",
    "productTypes": "GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS",
    "allocationStatus": "ACTIVE",
    "validFrom": "2025-01-15",
    "validTill": "2025-12-31",
    "daysRemaining": 320,
    "totalAllocated": 1000000,
    "totalDebited": 350000,
    "totalCredited": 50000,
    "currentBalance": 700000,
    "utilizationPercentage": "35.00%",
    "debitCount": 7,
    "creditCount": 2,
    "totalTransactions": 9,
    "productBreakdown": {
      "goldLoan": { "debited": 150000, "percentage": "42.86%" },
      "deposits": { "debited": 100000, "percentage": "28.57%" },
      "withdrawals": { "debited": 50000, "percentage": "14.29%" },
      "loans": { "debited": 30000, "percentage": "8.57%" },
      "overdraft": { "debited": 15000, "percentage": "4.29%" },
      "salaryCredits": { "debited": 5000, "percentage": "1.43%" }
    },
    "lastTransactionDate": "2025-01-20T15:45:00",
    "lastUpdated": "2025-01-20T15:45:00"
  }
}
```

#### 3.2 Get Manager Allocations Summary
```
GET /api/manager/allocations/summary/{managerId}

Response (200):
{
  "success": true,
  "totalAllocated": 3500000,
  "totalUsed": 1050000,
  "totalAvailable": 2450000,
  "utilizationPercentage": "30.00%",
  "allocationCount": 3,
  "allocations": [
    {
      "allocationId": "FA-2025-00001",
      "allocatedAmount": 1000000,
      "usedAmount": 350000,
      "availableBalance": 650000,
      "utilizationPercentage": "35.00%",
      "validTill": "2025-12-31",
      "daysRemaining": 320,
      "productTypes": "GOLD_LOAN,DEPOSITS,WITHDRAWALS"
    }
  ]
}
```

#### 3.3 Get Utilization Trend (Charts)
```
GET /api/manager/allocations/{allocationId}/trend?days=7

Response (200):
{
  "success": true,
  "trendData": [
    {
      "date": "2025-01-14",
      "totalAllocated": 1000000,
      "totalDebited": 250000,
      "currentBalance": 750000,
      "utilizationPercentage": 25.0
    },
    {
      "date": "2025-01-15",
      "totalAllocated": 1000000,
      "totalDebited": 300000,
      "currentBalance": 700000,
      "utilizationPercentage": 30.0
    }
  ],
  "fromDate": "2025-01-14",
  "toDate": "2025-01-21"
}
```

#### 3.4 Get HOD Allocations Report
```
GET /api/hod/metrics/allocations-report?fromDate=2025-01-01&toDate=2025-12-31

Response (200):
{
  "success": true,
  "overallSummary": {
    "totalAllocations": 45,
    "totalAllocatedAmount": 50000000,
    "totalDebitedAmount": 15000000,
    "totalCreditedAmount": 2000000,
    "totalAvailableBalance": 37000000,
    "overallUtilizationPercentage": "30.00%"
  },
  "allocationMetrics": [ /* 45 allocation records */ ],
  "reportDate": "2025-01-21"
}
```

#### 3.5 Get Top Allocations (by utilization %)
```
GET /api/hod/metrics/top-allocations?limit=10

Response (200):
{
  "success": true,
  "topAllocations": [
    {
      "allocationId": "FA-2025-00005",
      "managerName": "High Performer Manager",
      "totalAllocated": 500000,
      "totalDebited": 450000,
      "utilizationPercentage": "90.00%"
    }
  ]
}
```

#### 3.6 Get Low Allocations (underutilized)
```
GET /api/hod/metrics/low-allocations?limit=10

Response (200):
{
  "success": true,
  "lowAllocations": [
    {
      "allocationId": "FA-2025-00010",
      "managerName": "Low Performer Manager",
      "totalAllocated": 800000,
      "totalDebited": 80000,
      "utilizationPercentage": "10.00%"
    }
  ]
}
```

#### 3.7 Get Expiring Allocations
```
GET /api/hod/metrics/expiring-allocations?days=30

Response (200):
{
  "success": true,
  "expiringAllocations": [
    {
      "allocationId": "FA-2025-00015",
      "managerName": "Manager Name",
      "validTill": "2025-02-10",
      "daysRemaining": 20,
      "currentBalance": 250000
    }
  ],
  "count": 5
}
```

#### 3.8 Manager Dashboard (Comprehensive)
```
GET /api/manager/dashboard/{managerId}

Response (200):
{
  "success": true,
  "metricsOverview": {
    "totalAllocated": 3500000,
    "totalUsed": 1050000,
    "totalAvailable": 2450000,
    "utilizationPercentage": "30.00%",
    "allocationCount": 3
  }
}
```

#### 3.9 HOD Dashboard (Comprehensive)
```
GET /api/hod/dashboard?fromDate=2025-01-01&toDate=2025-12-31

Response (200):
{
  "success": true,
  "overallReport": { /* complete report */ },
  "expiringAllocations": { /* expiring within 30 days */ },
  "topPerformers": { /* top 5 allocations */ },
  "lowPerformers": { /* bottom 5 allocations */ }
}
```

#### 3.10 Metrics History (Trend Analysis)
```
GET /api/manager/allocations/{allocationId}/metrics-history?fromDate=2025-01-01&toDate=2025-12-31
```

---

### 4. VALIDATION ENDPOINTS

#### 4.1 Validate Product Types
```
POST /api/allocations/validate-products
Content-Type: application/json

{
  "productTypes": "GOLD_LOAN,DEPOSITS,INVALID_TYPE"
}

Response (200):
{
  "isValid": false,
  "validTypes": ["GOLD_LOAN", "DEPOSITS"],
  "invalidTypes": ["INVALID_TYPE"],
  "allowedTypes": ["GOLD_LOAN", "DEPOSITS", "WITHDRAWALS", "LOANS", "OVERDRAFT", "SALARY_CREDIT"]
}
```

#### 4.2 Get Manager Allocation Limits
```
GET /api/manager/allocation-limits/{managerId}

Response (200):
{
  "managerId": 5,
  "totalActiveAllocations": 2500000,
  "maximumLimit": 100000000,
  "remainingLimit": 97500000,
  "utilizationPercentage": "2.50%",
  "canAllocateMore": true
}
```

---

## Key Features

### ✅ Fund Allocation Management
- Create allocations with multi-product support
- Update amount and validity (ACTIVE only)
- Pause/Resume allocations temporarily
- Cancel with reason tracking
- Auto-complete on expiry

### ✅ Real-Time Transaction Tracking
- Debit transactions (fund usage)
- Credit transactions (fund returns/refunds)
- Automatic balance updates
- Product-specific tracking
- Audit trail with timestamps

### ✅ Comprehensive Metrics
- Real-time KPIs and utilization %
- Product-wise breakdown
- Transaction counts and trends
- Days remaining calculation
- Trend analysis (7/30/90 day)

### ✅ HOD Dashboard Features
- View all allocations
- Monitor branch performance
- Identify top/low performers
- Alert on expiring allocations
- Overall utilization report

### ✅ Manager Dashboard Features
- View allocated funds
- Check current balance
- See utilization by product
- Transaction history
- Validity period tracking

### ✅ Business Validations
- Allocation amount limits (max 50 Cr)
- Manager allocation cap (max 10 Cr total)
- Expiry date validation
- Product type validation
- Access control (HOD vs Manager)
- Sufficient balance checks
- Active status validation

---

## Response Format Standard

All API responses follow this structure:

### Success Response
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { /* specific data based on endpoint */ }
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error description",
  "errors": [ /* detailed error list */ ]
}
```

### Pagination Response
```json
{
  "content": [ /* array of items */ ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 20
}
```

---

## Testing the API

### Using Postman
1. Import the endpoints from this documentation
2. Set authorization header with valid admin token
3. Use test data with realistic amounts
4. Verify response status codes and data structure

### Sample Test Flow
1. **Create Allocation**: POST /api/hod/allocate-funds
2. **Verify Details**: GET /api/hod/allocations/{id}
3. **Debit Transaction**: POST /api/manager/utilization/debit
4. **Check Metrics**: GET /api/manager/allocations/{id}/metrics
5. **View History**: GET /api/manager/utilization/{id}/history
6. **Get Dashboard**: GET /api/manager/dashboard/{managerId}

---

## Integration Points

### With Existing Services
- **GoldLoanService**: Check allocation before disbursement → Debit
- **DepositService**: Auto-debit on deposit creation
- **LoanService**: Check allocation for loan disbursement → Debit
- **SalaryAccountService**: Credit allocation after salary processing
- **WithdrawalService**: Debit allocation on withdrawal
- **OverdraftService**: Debit allocation for overdraft usage

### Transaction Integration Pattern
```
1. Service needs funds → Check allocation balance
2. Create transaction → Debit allocation
3. Update metrics → Auto-sync totals
4. Notify dashboard → WebSocket event
5. Return success → Balance updated
```

---

## Performance Optimization

### Database Indexes
- allocation_id (UNIQUE)
- manager_id + status (COMPOUND)
- allocation_date + status (COMPOUND)
- transaction_date DESC (for history queries)

### Query Optimization
- Pagination with configurable limits (max 100)
- JPQL aggregate functions (SUM, COUNT, AVG)
- Lazy loading relationships
- Connection pooling

---

## Error Codes

| Code | Message | Status |
|------|---------|--------|
| 200 | Success | OK |
| 201 | Created | CREATED |
| 400 | Invalid request | BAD_REQUEST |
| 401 | Unauthorized | UNAUTHORIZED |
| 403 | Forbidden | FORBIDDEN |
| 404 | Not found | NOT_FOUND |
| 500 | Server error | INTERNAL_ERROR |

---

## Next Phase: Frontend Implementation

The Angular components will integrate with these APIs:
- HOD Allocation Management Component
- HOD Dashboard & Reports
- Manager My Allocations Component
- Manager Transaction Log Component
- Real-time Metrics Widget
- WebSocket integration for live updates

**Backend is ready for frontend consumption! ✅**
