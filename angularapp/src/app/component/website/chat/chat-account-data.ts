import { HttpClient, HttpHeaders } from '@angular/common/http';
import { forkJoin, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { environment } from '../../../../environment/environment';

export const serviceShortcuts = [
  { label: 'Account Overview', feature: 'accounts' },
  { label: 'Transactions', feature: 'transaction' },
  { label: 'Fund Transfer', feature: 'transferfunds' },
  { label: 'Scheduled Payments', feature: 'scheduled-payments' },
  { label: 'Cheque Management', feature: 'cheque' },
  { label: 'Positive Pay', feature: 'positive-pay' },
  { label: 'Debit Card', feature: 'card' },
  { label: 'Credit Card', feature: 'credit-card' },
  { label: 'Card Bill Pay', feature: 'credit-card-bill' },
  { label: 'Virtual Card', feature: 'virtual-card' },
  { label: 'Personal Loans', feature: 'loan' },
  { label: 'Gold Loans', feature: 'goldloan' },
  { label: 'Home Loans', feature: 'homeloan' },
  { label: 'ECS Mandates', feature: 'ecs-mandate' },
  { label: 'Bill Payments', feature: 'bill-payment' },
  { label: 'Fixed Deposits', feature: 'fixed-deposits' },
  { label: 'Investments', feature: 'investments' },
  { label: 'Fastag', feature: 'fasttag' },
  { label: 'Family Banking', feature: 'family-banking' },
  { label: 'Support Tickets', feature: 'support-tickets' }
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
