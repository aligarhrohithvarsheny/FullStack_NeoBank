import { HttpClient, HttpHeaders } from '@angular/common/http';
import { forkJoin, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { environment } from '../../../../environment/environment';

export const serviceShortcuts = [
  { label: 'Account Overview', feature: 'accounts' },
  { label: 'Transactions', feature: 'transaction' },
  { label: 'Fund Transfer', feature: 'transferfunds' },
  { label: 'Scheduled Payments', feature: 'scheduled-payments' },
  { label: 'Demand Draft', feature: 'demand-draft' },
  { label: 'ATM Withdrawal', feature: 'atm-simulator' },
  { label: 'Cash Deposit Slip', feature: 'deposit-request' },
  { label: 'Cheque Management', feature: 'cheque' },
  { label: 'Positive Pay', feature: 'positive-pay' },
  { label: 'Debit Card', feature: 'card' },
  { label: 'Credit Card', feature: 'credit-card' },
  { label: 'Card Bill Pay', feature: 'credit-card-bill' },
  { label: 'Virtual Card', feature: 'virtual-card' },
  { label: 'Personal Loans', feature: 'loan' },
  { label: 'Loan Repayment', feature: 'loan-payment' },
  { label: 'Gold Loans', feature: 'goldloan' },
  { label: 'Home Loans', feature: 'homeloan' },
  { label: 'Loan Eligibility', feature: 'ml-loan-prediction' },
  { label: 'ECS Mandates', feature: 'ecs-mandate' },
  { label: 'EMI Monitoring', feature: 'emi-monitoring' },
  { label: 'Bill Payments', feature: 'bill-payment' },
  { label: 'Fixed Deposits', feature: 'fixed-deposits' },
  { label: 'Investments', feature: 'investments' },
  { label: 'Education Subsidy', feature: 'subsidy-claim' },
  { label: 'Fastag', feature: 'fasttag' },
  { label: 'Family Banking', feature: 'family-banking' },
  { label: 'UPI Payments', feature: 'upi-pay' },
  { label: 'Beneficiaries', feature: 'beneficiary-management' },
  { label: 'KYC Update', feature: 'kycupdate' },
  { label: 'Bank Messages', feature: 'bank-messages' },
  { label: 'Support Tickets', feature: 'support-tickets' },
  { label: 'AI Assistant', feature: 'ai-assistant' },
  { label: 'Insurance', feature: 'insurance' },
  { label: 'Cards360', feature: 'cards360' }
];

function formatCurrency(value: unknown): string {
  if (value === null || value === undefined || value === '') return 'Not available';
  const amount = Number(value);
  return Number.isFinite(amount)
    ? `₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    : 'Not available';
}

export function loadCreditCardBillsMessage(
  http: HttpClient,
  accountNumber: string,
  headers: HttpHeaders
): Observable<string> {
  const cardsUrl = `${environment.apiBaseUrl}/api/credit-cards/account/${encodeURIComponent(accountNumber)}`;
  return http.get<any[]>(cardsUrl, { headers }).pipe(
    switchMap(cards => {
      if (!cards.length) return of('There are no credit cards linked to your account.');
      return forkJoin(cards.map(card =>
        http.get<any[]>(`${environment.apiBaseUrl}/api/credit-cards/${card.id}/bills`, { headers })
          .pipe(map(bills => ({ card, bills })))
      )).pipe(map(cardBills => {
        const billLines = cardBills.flatMap(({ card, bills }) =>
          (bills || []).map(bill => {
            const dueDate = bill.dueDate
              ? new Date(bill.dueDate).toLocaleDateString('en-IN')
              : 'Not available';
            return `${card.cardNumber || 'Credit card'} • ${bill.billingPeriod || 'Statement'}\n`
              + `Status: ${bill.status || 'Unknown'} | Bill: ${formatCurrency(bill.totalAmount)}`
              + ` | Paid: ${formatCurrency(bill.paidAmount)}`
              + ` | Minimum due: ${formatCurrency(bill.minimumDue)} | Due: ${dueDate}`;
          })
        );
        return billLines.length
          ? `Your credit card bills:\n\n${billLines.join('\n\n')}`
          : 'Your linked credit cards do not have any generated bills yet.';
      }));
    })
  );
}

export function loadGoldLoanDetailsMessage(
  http: HttpClient,
  headers: HttpHeaders
): Observable<string> {
  return http.get<any[]>(`${environment.apiBaseUrl}/api/gold-loans/mine`, { headers }).pipe(
    map(loans => {
      const details = loans.map(loan =>
        `Status: ${loan.status || 'Unknown'} | Loan amount: ${formatCurrency(loan.loanAmount)}`
        + ` | Interest: ${loan.interestRate ?? 'Not available'}%`
        + ` | Tenure: ${loan.tenure ?? 'Not available'} months`
        + ` | EMI: ${formatCurrency(loan.currentEmi)}`
        + ` | Remaining principal: ${formatCurrency(loan.remainingPrincipal)}`
        + ` | Remaining interest: ${formatCurrency(loan.remainingInterest)}`
        + ` | Months remaining: ${loan.remainingTenure ?? 'Not available'}`
      );
      return details.length
        ? `Your gold loan details:\n\n${details.join('\n\n')}`
        : 'No gold loan applications are linked to your account.';
    })
  );
}
