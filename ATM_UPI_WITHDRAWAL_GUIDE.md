# NeoBank UPI ATM Withdrawal

The ATM simulator supports a cardless withdrawal for savings and salary accounts:

1. Select **Withdraw with NeoBank UPI**, choose an active ATM, and select a cash amount.
2. The backend creates an amount-locked request that expires after five minutes. The ATM displays a UPI QR code.
3. Scan the QR from the signed-in NeoBank user dashboard or salary dashboard and authorize with the account's UPI PIN.
4. The backend atomically debits the account, records the ATM cash withdrawal and corresponding UPI history entry, then marks the request successful.
5. The ATM checks request status every two seconds and starts cash dispensing only after successful authorization.

Savings accounts use their six-digit UPI PIN. Salary accounts use their four-digit transaction PIN. A request is single-use, is bound to the account that created it, and locks after three incorrect PIN attempts.

## API

- `POST /api/atm/upi-withdrawals/requests` — create a request (`atmId`, `accountNumber`, `accountType`, `amount`).
- `GET /api/atm/upi-withdrawals/requests/{token}?accountNumber=...` — check request state.
- `POST /api/atm/upi-withdrawals/requests/{token}/confirm` — authorize (`accountNumber`, `pin`).
- `POST /api/atm/upi-withdrawals/requests/{token}/cancel` — cancel a pending request.

Apply [init-scripts/39-atm-upi-withdrawals.sql](./init-scripts/39-atm-upi-withdrawals.sql) to add request storage and UPI reference fields to ATM transaction records.

This QR approval flow is for the NeoBank dashboards. It does not connect to NPCI or an external UPI payment provider; production external-UPI support requires a provider integration and payment callback verification.
