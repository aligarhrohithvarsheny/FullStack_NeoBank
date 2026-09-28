# Account Linking & Charge Management - Integration Guide

## Quick Start for Developers

### 1. Database Setup
```bash
# Execute migration script
mysql -u root -p neobank < init-scripts/43-account-linking-charges.sql

# Verify tables created
SELECT TABLE_NAME FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'neobank' AND TABLE_NAME LIKE 'allocation%';
```

### 2. Backend Integration (Spring Boot)

#### Add Service to Your Controller
```java
@RestController
@RequestMapping("/api")
public class YourController {
    
    @Autowired
    private AllocationAccountLinkingService accountService;
    
    @Autowired
    private ChargeManagementService chargeService;
    
    // Use services here
}
```

#### Enable CORS for Frontend
```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:4200")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowCredentials(true);
            }
        };
    }
}
```

### 3. Angular Module Integration

#### Import Services in Your Module
```typescript
import { AllocationAccountService } from './service/allocation-account.service';
import { ChargeManagementService } from './service/charge-management.service';

@NgModule({
  providers: [
    AllocationAccountService,
    ChargeManagementService
  ]
})
export class AppModule { }
```

#### Or in Standalone Component
```typescript
import { AllocationAccountService } from './service/allocation-account.service';

@Component({
  selector: 'app-my-component',
  standalone: true,
  providers: [AllocationAccountService]
})
export class MyComponent { }
```

#### Configure API URL
```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};

// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.neobank.com'
};
```

### 4. Component Usage Examples

#### HOD Account Linking
```typescript
import { AccountLinkingComponent } from './component/admin/account-linking/account-linking';

// Add to your routing
const routes = [
  {
    path: 'admin/account-linking',
    component: AccountLinkingComponent
  }
];
```

#### Admin Cheque Verification
```typescript
import { AccountVerificationComponent } from './component/admin/account-verification/account-verification';

// Add to your routing
const routes = [
  {
    path: 'admin/verification',
    component: AccountVerificationComponent
  }
];
```

#### Manager Account Dashboard
```typescript
import { ManagerAccountDashboardComponent } from './component/admin/manager/manager-account-dashboard/manager-account-dashboard';

// Add to your routing
const routes = [
  {
    path: 'manager/account-dashboard',
    component: ManagerAccountDashboardComponent
  }
];
```

### 5. Service Usage in Your Component

#### Linking Account
```typescript
export class MyComponent {
  constructor(private accountService: AllocationAccountService) {}
  
  linkAccount() {
    const payload = {
      allocationId: 1,
      accountNumber: '1234567890',
      ifscCode: 'HDFC0000001',
      accountHolderName: 'John Doe',
      bankName: 'HDFC Bank',
      branchName: 'Main Branch',
      city: 'Mumbai',
      linkedByAdminId: 1
    };
    
    this.accountService.linkAccountToAllocation(payload).subscribe(
      (response) => {
        console.log('Account linked:', response);
      },
      (error) => {
        console.error('Error linking account:', error);
      }
    );
  }
}
```

#### Processing Charge
```typescript
export class MyComponent {
  constructor(private chargeService: ChargeManagementService) {}
  
  processCharge() {
    const chargeData = {
      allocationId: 1,
      chargeType: 'INTEREST',
      chargeDescription: 'Monthly Interest Charge',
      chargeAmount: 50.00,
      userAccountNumber: '9876543210',
      userName: 'Jane Doe',
      userProductType: 'GOLD_LOAN'
    };
    
    this.chargeService.processCharge(chargeData).subscribe(
      (response) => {
        console.log('Charge processed:', response);
      },
      (error) => {
        console.error('Error processing charge:', error);
      }
    );
  }
}
```

#### Getting Charge Summary
```typescript
export class MyComponent {
  chargesSummary: any = {};
  
  constructor(private chargeService: ChargeManagementService) {}
  
  ngOnInit() {
    this.chargeService.getChargeSummary(1).subscribe(
      (response) => {
        this.chargesSummary = response.summary;
        console.log('Total charges:', this.chargesSummary.totalCharges);
      }
    );
  }
}
```

### 6. Form Validation Patterns

#### Account Number Validation
```typescript
accountNumber: new FormControl('', [
  Validators.required,
  Validators.pattern(/^\d{9,18}$/) // 9-18 digits
])
```

#### IFSC Code Validation
```typescript
ifscCode: new FormControl('', [
  Validators.required,
  Validators.pattern(/^[A-Z]{4}0[A-Z0-9]{6}$/) // XXXX0XXXXXX
])
```

#### Cheque Number Validation
```typescript
chequeNumber: new FormControl('', [
  Validators.required,
  Validators.pattern(/^\d{6,10}$/) // 6-10 digits
])
```

### 7. Error Handling

#### HTTP Interceptor (Optional)
```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        const errorMessage = error.error?.message || 'An error occurred';
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
```

#### In Component
```typescript
this.accountService.linkAccountToAllocation(payload).subscribe(
  (response) => {
    this.successMessage = 'Account linked successfully!';
  },
  (error) => {
    this.errorMessage = error.message || 'Failed to link account';
  }
);
```

### 8. Testing API Endpoints

#### Using Postman/Curl

**Create Account Link:**
```bash
curl -X POST http://localhost:8080/api/hod/link-account \
  -H "Content-Type: application/json" \
  -d '{
    "allocationId": 1,
    "accountNumber": "1234567890",
    "ifscCode": "HDFC0000001",
    "accountHolderName": "John Doe",
    "bankName": "HDFC Bank",
    "branchName": "Main Branch",
    "city": "Mumbai",
    "linkedByAdminId": 1
  }'
```

**Get Pending Cheques:**
```bash
curl -X GET http://localhost:8080/api/admin/pending-cheques
```

**Process Charge:**
```bash
curl -X POST http://localhost:8080/api/charges/process \
  -H "Content-Type: application/json" \
  -d '{
    "allocationId": 1,
    "chargeType": "INTEREST",
    "chargeDescription": "Monthly Interest",
    "chargeAmount": 50.00,
    "userAccountNumber": "9876543210",
    "userName": "Jane Doe",
    "userProductType": "GOLD_LOAN"
  }'
```

### 9. Database Queries

#### View Pending Cheques
```sql
SELECT * FROM pending_cheques;
```

#### View Charge Summary
```sql
SELECT * FROM allocation_charge_summary;
```

#### View Failed Transactions
```sql
SELECT * FROM charge_transactions_failed;
```

#### Check Account Audit Trail
```sql
SELECT * FROM allocation_account_audit 
WHERE allocation_account_id = 1
ORDER BY created_at DESC;
```

### 10. Debugging Tips

#### View Console Logs
```typescript
// In browser console
console.log('Charge summary:', this.chargesSummary);
console.log('Form errors:', this.form.errors);
```

#### Check Network Requests
- Open Developer Tools (F12)
- Go to Network tab
- Look for API calls to `/api/hod/`, `/api/admin/`, `/api/charges/`
- Check response status and body

#### View Database Changes
```sql
-- Check latest account linking
SELECT * FROM allocation_account 
ORDER BY created_at DESC 
LIMIT 1;

-- Check charge transactions
SELECT * FROM charge_transaction 
WHERE allocation_id = ? 
ORDER BY created_at DESC;
```

### 11. Common Issues & Solutions

**Issue:** CORS error
**Solution:** Check CORS configuration in Spring Boot, ensure frontend URL is whitelisted

**Issue:** Form validation failing
**Solution:** Check regex patterns match input format (IFSC: XXXX0XXXXXX, Account: 9-18 digits)

**Issue:** Charge not being processed
**Solution:** Verify allocation account is VERIFIED and ACTIVE status

**Issue:** Empty select dropdown
**Solution:** Ensure API is returning data, check browser console for HTTP errors

### 12. Performance Tips

#### Pagination
```typescript
// Use pagination for large datasets
getChargesByAllocation(allocationId: number, page: number = 0, size: number = 10) {
  return this.accountService.getChargesByAllocation(allocationId, page, size);
}
```

#### Batch Processing
```typescript
// Process multiple charges at once
const charges = [
  { allocationId: 1, chargeType: 'INTEREST', chargeAmount: 50 },
  { allocationId: 2, chargeType: 'CIBIL', chargeAmount: 99 }
];

this.chargeService.processChargeBatch(charges).subscribe(
  (response) => console.log('Batch processed:', response)
);
```

#### Caching
```typescript
// Cache expensive queries
chargesCache = new Map();

getChargesSummary(allocationId: number) {
  if (this.chargesCache.has(allocationId)) {
    return of(this.chargesCache.get(allocationId));
  }
  
  return this.chargeService.getChargeSummary(allocationId).pipe(
    tap(data => this.chargesCache.set(allocationId, data))
  );
}
```

---

## File Structure

```
angularapp/
├── src/
│   ├── app/
│   │   ├── service/
│   │   │   ├── allocation-account.service.ts
│   │   │   └── charge-management.service.ts
│   │   └── component/
│   │       └── admin/
│   │           ├── account-linking/
│   │           │   ├── account-linking.ts
│   │           │   ├── account-linking.html
│   │           │   └── account-linking.css
│   │           ├── account-verification/
│   │           │   ├── account-verification.ts
│   │           │   ├── account-verification.html
│   │           │   └── account-verification.css
│   │           └── manager/
│   │               └── manager-account-dashboard/
│   │                   ├── manager-account-dashboard.ts
│   │                   ├── manager-account-dashboard.html
│   │                   └── manager-account-dashboard.css

springapp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/neobank/
│   │   │       └── entity/
│   │   │           ├── AllocationAccount.java
│   │   │           └── ChargeTransaction.java
│   │   │       └── service/
│   │   │           ├── AllocationAccountLinkingService.java
│   │   │           └── ChargeManagementService.java
│   │   │       └── controller/
│   │   │           ├── AllocationAccountLinkingController.java
│   │   │           └── ChargeManagementController.java
│   │   │       └── repository/
│   │   │           ├── AllocationAccountRepository.java
│   │   │           └── ChargeTransactionRepository.java

init-scripts/
├── 43-account-linking-charges.sql
```

---

**For additional help, refer to the main documentation: ACCOUNT_LINKING_IMPLEMENTATION_COMPLETE.md**
