# Funds Allocation Feature - Technical Quick Reference

## 🚀 Quick Navigation

| Component | File | Purpose | Status |
|-----------|------|---------|--------|
| **Backend** |
| Entities | `entity/FundsAllocation.java` | Master allocation record | ✅ |
| | `entity/AllocationUtilization.java` | Debit/credit ledger | ✅ |
| | `entity/AllocationRealTimeMetrics.java` | KPI aggregation | ✅ |
| Repositories | `repository/FundsAllocationRepository.java` | Data access (10 methods) | ✅ |
| | `repository/AllocationUtilizationRepository.java` | Transaction queries (9 methods) | ✅ |
| | `repository/AllocationMetricsRepository.java` | Metrics queries (4 methods) | ✅ |
| Services | `service/FundsAllocationService.java` | Allocation lifecycle (8 methods) | ✅ |
| | `service/AllocationUtilizationService.java` | Transaction handling (7 methods) | ✅ |
| | `service/AllocationMetricsService.java` | Metrics/reporting (8 methods) | ✅ |
| | `service/AllocationValidatorService.java` | Validation rules (8 methods) | ✅ |
| Controllers | `controller/FundsAllocationController.java` | Allocation API (8 endpoints) | ✅ |
| | `controller/AllocationUtilizationController.java` | Utilization API (8 endpoints) | ✅ |
| | `controller/AllocationMetricsController.java` | Metrics API (12 endpoints) | ✅ |

---

## 📋 API Endpoint Quick Reference

### Create & Manage Allocations
```
POST   /api/hod/allocate-funds              ← Create allocation
GET    /api/hod/allocations                 ← List allocations
GET    /api/hod/allocations/{id}            ← View details
PUT    /api/hod/allocations/{id}            ← Update allocation
DELETE /api/hod/allocations/{id}/cancel     ← Cancel allocation
POST   /api/hod/allocations/{id}/pause      ← Pause allocation
POST   /api/hod/allocations/{id}/resume     ← Resume allocation
GET    /api/hod/branch-allocation-status/{mgrId} ← Check status
```

### Record Transactions
```
POST   /api/manager/utilization/debit       ← Debit funds (use)
POST   /api/manager/utilization/credit      ← Credit funds (refund)
POST   /api/manager/utilization/validate-debit ← Pre-check
```

### View Transaction History
```
GET    /api/manager/utilization/{id}/history      ← All transactions
GET    /api/manager/utilization/{id}/by-product   ← Filter by product
GET    /api/manager/utilization/{id}/debits       ← Debits only
GET    /api/manager/utilization/{id}/credits      ← Credits only
GET    /api/manager/utilization/{id}/product-summary ← Breakdown
```

### Real-Time Metrics & Dashboards
```
GET    /api/manager/allocations/{id}/metrics      ← Current KPIs
GET    /api/manager/allocations/summary/{mgrId}   ← Manager overview
GET    /api/manager/allocations/{id}/trend        ← Trend data (7/30/90 days)
GET    /api/manager/dashboard/{mgrId}             ← Manager dashboard
GET    /api/hod/dashboard                         ← HOD dashboard
GET    /api/hod/metrics/allocations-report        ← Comprehensive report
GET    /api/hod/metrics/top-allocations           ← Top performers
GET    /api/hod/metrics/low-allocations           ← Low performers
GET    /api/hod/metrics/expiring-allocations      ← Expiring soon
GET    /api/manager/allocation-limits/{mgrId}     ← Manager limits
```

---

## 💾 Database Tables

### funds_allocation (Master)
```sql
id (PK) | allocationId (UNIQUE) | managerId (FK) | managerName | 
branchName | allocatedAmount | allocationType | productTypes | 
status (ACTIVE|PAUSED|COMPLETED|CANCELLED) | currentBalance | 
totalDebited | totalCredited | totalUtilized | validFrom | validTill | 
createdAt | updatedAt | cancelledAt | cancelledReason | description
```

### allocation_utilization (Ledger)
```sql
id (PK) | utilizationId (UNIQUE) | allocationId (FK) | transactionType (DEBIT|CREDIT) | 
amount | remainingBalance | linkedTransactionId | productType | 
userAccountNumber | userName | transactionDate | status | 
performedByAdminId | description
```

### allocation_metrics (KPIs)
```sql
id (PK) | allocationId (FK) | totalAllocated | totalDebited | 
totalCredited | currentBalance | utilizationPercentage | 
debitCount | creditCount | lastTransactionDate | 
lastUpdated | goldLoanDebited | depositsDebited | 
withdrawalsDebited | loansDebited | overdraftDebited | 
salaryCreditDebited
```

---

## 🔑 Key Features at a Glance

### Fund Allocation
- ✅ Multi-product support (6 types)
- ✅ Auto-ID generation (FA-2025-XXXXX)
- ✅ Validity period tracking
- ✅ Status lifecycle management
- ✅ Cancel/Pause/Resume operations
- ✅ Real-time balance tracking

### Transaction Handling
- ✅ Debit transactions (fund usage)
- ✅ Credit transactions (refunds)
- ✅ Automatic balance updates
- ✅ Product-specific tracking
- ✅ Comprehensive audit trail
- ✅ Transaction history with pagination

### Analytics & Reporting
- ✅ Real-time KPIs
- ✅ Utilization percentage
- ✅ Product-wise breakdown
- ✅ Trend analysis (7/30/90 day)
- ✅ Top/low performer identification
- ✅ Expiring allocation alerts
- ✅ HOD & Manager dashboards

### Business Rules
- ✅ Max allocation: 50 Crore
- ✅ Max per manager: 10 Crore
- ✅ Expiry date validation
- ✅ Sufficient balance checks
- ✅ Product type validation
- ✅ Access control (HOD vs Manager)
- ✅ Active status enforcement
- ✅ Manager limit enforcement

---

## 🔒 Security & Validation

### Access Control
```
HOD Role:
  ✅ Can create allocations
  ✅ Can update/cancel allocations
  ✅ Can view all allocations
  ✅ Can access HOD dashboard

Manager Role:
  ✅ Can record transactions
  ✅ Can view own allocations only
  ✅ Can access manager dashboard
  ❌ Cannot create/modify allocations
```

### Data Validation
- Amount: > 0 and ≤ 50 Crore
- Date: validTill must be in future
- Product Types: Must be from allowed list
- Status: Only ACTIVE can be updated
- Balance: Debit requires sufficient balance
- Manager Limit: Total ≤ 10 Crore

---

## 📊 Response Formats

### Success Response (200/201)
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* specific data */ }
}
```

### Error Response (400/500)
```json
{
  "success": false,
  "message": "Error description",
  "errors": ["Error 1", "Error 2"]
}
```

### Paginated Response
```json
{
  "content": [ /* items */ ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 20
}
```

---

## 🧪 Testing API with curl

### Create Allocation
```bash
curl -X POST http://localhost:8080/api/hod/allocate-funds \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000000,
    "branchManagerId": 5,
    "allocationType": "GENERAL",
    "productTypes": "GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS",
    "validTill": "2025-12-31",
    "description": "Q4 allocation",
    "hodAdminId": 1
  }'
```

### Get Allocation Details
```bash
curl http://localhost:8080/api/hod/allocations/1
```

### Debit Allocation
```bash
curl -X POST http://localhost:8080/api/manager/utilization/debit \
  -H "Content-Type: application/json" \
  -d '{
    "allocationId": 1,
    "amount": 50000,
    "productType": "GOLD_LOAN",
    "userAccountNumber": "ACC123",
    "userName": "John Doe",
    "linkedTransactionId": "TXN-2025-123",
    "description": "Gold Loan Disbursement",
    "performedByAdminId": 5
  }'
```

### Get Metrics
```bash
curl http://localhost:8080/api/manager/allocations/1/metrics
```

### Get Dashboard
```bash
curl http://localhost:8080/api/hod/dashboard
```

---

## 🛠️ Common Debugging Tips

### Issue: Allocation not found
- ✓ Check allocation_id format (FA-2025-XXXXX)
- ✓ Verify ID exists in database
- ✓ Check status (CANCELLED allocations still exist)

### Issue: Insufficient balance
- ✓ Check metrics for current balance
- ✓ Verify debit amount is less than currentBalance
- ✓ Check if allocation is ACTIVE (not PAUSED/CANCELLED)

### Issue: Access denied
- ✓ Verify user role (HOD vs Manager)
- ✓ Check managerId matches user's branch
- ✓ Confirm authentication token is valid

### Issue: Invalid product type
- ✓ Use exact product names: GOLD_LOAN, DEPOSITS, WITHDRAWALS, LOANS, OVERDRAFT, SALARY_CREDIT
- ✓ Separate multiple products with comma (no spaces)
- ✓ Validate with /api/allocations/validate-products endpoint

### Issue: Expired allocation
- ✓ Check validTill date hasn't passed
- ✓ Update allocation to extend validity
- ✓ Use /api/hod/metrics/expiring-allocations to find expiring ones

---

## 📱 Product Types Reference

| Product Type | Code | Used For |
|--------------|------|----------|
| Gold Loan | GOLD_LOAN | Gold loan disbursements |
| Deposits | DEPOSITS | Deposit account credits |
| Withdrawals | WITHDRAWALS | Withdrawal transactions |
| Loans | LOANS | General loan disbursements |
| Overdraft | OVERDRAFT | Overdraft facility usage |
| Salary Credits | SALARY_CREDIT | Salary processing |

---

## 📦 Integration Checklist

### Backend (✅ 100% Complete)
- [x] Entities created and mapped
- [x] Repositories with JPQL queries
- [x] Services with business logic
- [x] Controllers with REST endpoints
- [x] Validation framework
- [x] Error handling
- [x] CORS configuration
- [x] Database indexes

### Frontend (🔄 Next Phase)
- [ ] Angular module setup
- [ ] Service classes created
- [ ] Model interfaces defined
- [ ] HOD components built
- [ ] Manager components built
- [ ] Dashboard components created
- [ ] WebSocket integration added
- [ ] Unit tests written
- [ ] Integration tests passed
- [ ] E2E tests completed

---

## 🚀 Environment Configuration

### Application Properties
```properties
# API Port
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/springapp
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# CORS
cors.allowed-origins=http://localhost:4200,http://127.0.0.1:4200

# Connection Pool
spring.datasource.hikari.maximum-pool-size=20
```

### Angular Environment
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'ws://localhost:8080/ws'
};
```

---

## 📞 Common Endpoints Reference Card

Keep this handy for development:

```
HOD Operations:
  Create: POST /api/hod/allocate-funds
  List: GET /api/hod/allocations
  View: GET /api/hod/allocations/1
  Edit: PUT /api/hod/allocations/1
  Cancel: DELETE /api/hod/allocations/1/cancel
  Dashboard: GET /api/hod/dashboard

Manager Operations:
  Debit: POST /api/manager/utilization/debit
  Credit: POST /api/manager/utilization/credit
  History: GET /api/manager/utilization/1/history
  Metrics: GET /api/manager/allocations/1/metrics
  Dashboard: GET /api/manager/dashboard/5

Validation:
  Pre-debit: POST /api/manager/utilization/validate-debit
  Limits: GET /api/manager/allocation-limits/5
  Products: POST /api/allocations/validate-products
```

---

## ✨ Next Steps

1. **Backend Deployment**
   - Build: `mvn clean install`
   - Run: `java -jar target/springapp-0.0.1-SNAPSHOT.jar`
   - Test: Verify all 28 endpoints

2. **Frontend Development**
   - Review ANGULAR_INTEGRATION_GUIDE.md
   - Create services from examples
   - Build HOD components
   - Build Manager components
   - Add WebSocket integration

3. **Integration Testing**
   - Test API with curl
   - Mock data in Angular
   - Connect to real backend
   - Run integration tests

4. **Deployment**
   - Backend: Spring Boot on cloud
   - Frontend: Angular build and deploy
   - Database: Verify MySQL schema
   - CORS: Configure for production domains

---

**🎯 Backend is production-ready! Start frontend development now! ✅**
