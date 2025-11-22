# Alert Replacement Guide

This document tracks the replacement of browser alerts/prompts with custom AlertService throughout the project.

## Completed Components

### ✅ Loan Component (`angularapp/src/app/component/website/loan/loan.ts`)
- All `alert()` calls replaced with `alertService.success()`, `alertService.error()`, or `alertService.warning()`
- All `confirm()` calls replaced with `alertService.confirm()`
- EMI payment success now shows custom modal with detailed information

### ✅ Transaction Component (`angularapp/src/app/component/website/transaction/transaction.ts`)
- All `alert()` calls replaced
- `confirm()` call replaced with `alertService.confirm()`

## Remaining Components to Update

### ⏳ Admin Components
1. **loans.ts** - Replace alerts for loan approval/rejection/foreclosure
2. **cards.ts** - Replace alerts for card operations
3. **kyc.ts** - Replace alerts for KYC approval/rejection
4. **users.ts** - Replace alerts for user operations
5. **dashboard.ts** - Replace alerts for downloads

### ⏳ User Components
1. **kycupdate.ts** - Replace alerts for KYC updates
2. **card.ts** - Already uses AlertService (verify all alerts are replaced)

## Usage Pattern

Replace:
```typescript
alert('Message');
```
With:
```typescript
this.alertService.success('Title', 'Message'); // for success
this.alertService.error('Title', 'Message'); // for errors
this.alertService.warning('Title', 'Message'); // for warnings
this.alertService.info('Title', 'Message'); // for info
```

Replace:
```typescript
if (confirm('Message')) {
  // action
}
```
With:
```typescript
this.alertService.confirm(
  'Title',
  'Message',
  () => {
    // action on confirm
  },
  () => {
    // action on cancel (optional)
  },
  'Confirm', // button text
  'Cancel'  // button text
);
```

## AlertService Methods

- `success(title, message, autoClose?, duration?)` - Green success alert
- `error(title, message, autoClose?)` - Red error alert
- `warning(title, message, autoClose?, duration?)` - Yellow warning alert
- `info(title, message, autoClose?, duration?)` - Blue info alert
- `confirm(title, message, onConfirm, onCancel?, confirmText?, cancelText?)` - Confirmation dialog

