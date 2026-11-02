export interface TermDepositReceiptData {
  product: 'FD' | 'RD';
  accountHolder: string;
  linkedAccountNumber: string;
  productAccountNumber: string;
  schemeName?: string;
  status: string;
  startDate?: string | Date;
  maturityDate?: string | Date;
  tenureMonths?: number;
  principalAmount?: number;
  monthlyInstallment?: number;
  interestRate?: number;
  interestAmount?: number;
  estimatedInterest?: number;
  maturityAmount?: number;
  payoutInstruction?: string;
  closureDate?: string | Date;
  prematureClosurePenalty?: number;
  prematureClosureAmount?: number;
  contributions?: Array<{
    contributionMonth?: string;
    amount?: number;
    sourceAccountNumber?: string;
    transactionId?: string;
    postedDate?: string | Date;
    status?: string;
  }>;
}

function escapeHtml(value: unknown): string {
  return String(value ?? 'Not available')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatAmount(value?: number): string {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return 'Not available';
  return amount.toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatDate(value?: string | Date): string {
  if (!value) return 'Not available';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Not available' : date.toLocaleDateString('en-IN');
}

function row(label: string, value: unknown): string {
  return `<tr><th>${escapeHtml(label)}</th><td>${escapeHtml(value)}</td></tr>`;
}

export function printTermDepositReceipt(receipt: TermDepositReceiptData): void {
  const printWindow = window.open('', '_blank', 'width=850,height=1000');
  if (!printWindow) throw new Error('Please allow pop-ups to generate the deposit receipt.');

  const productName = receipt.product === 'FD' ? 'Fixed Deposit' : 'Recurring Deposit';
  const accountRows = [
    row('Customer name', receipt.accountHolder),
    row('Linked account number', receipt.linkedAccountNumber),
    row(`${receipt.product} account number`, receipt.productAccountNumber),
    row('Scheme', receipt.schemeName || 'Not available'),
    row('Status', receipt.status),
    row('Start date', formatDate(receipt.startDate)),
    row('Maturity date', formatDate(receipt.maturityDate)),
    row('Tenure', receipt.tenureMonths == null || !Number.isFinite(receipt.tenureMonths)
      ? 'Not available'
      : `${receipt.tenureMonths} months`),
    row(receipt.product === 'FD' ? 'Principal amount' : 'Monthly installment',
      `₹${formatAmount(receipt.product === 'FD' ? receipt.principalAmount : receipt.monthlyInstallment)}`),
    row('Interest rate', receipt.interestRate == null ? 'Not available' : `${receipt.interestRate}% p.a.`),
    row(receipt.product === 'FD' ? 'Interest amount' : 'Estimated interest',
      `₹${formatAmount(receipt.product === 'FD' ? receipt.interestAmount : receipt.estimatedInterest)}`),
    row('Estimated maturity amount', `₹${formatAmount(receipt.maturityAmount)}`),
    row('Payout instruction', receipt.payoutInstruction || 'At maturity')
  ];

  if (receipt.product === 'FD' && receipt.closureDate) {
    accountRows.push(
      row('Closure date', formatDate(receipt.closureDate)),
      row('Premature closure penalty', `₹${formatAmount(receipt.prematureClosurePenalty)}`),
      row('Premature closure amount', `₹${formatAmount(receipt.prematureClosureAmount)}`)
    );
  }

  const contributions = receipt.product === 'RD'
    ? `<h3>Posted installment history</h3>
       <table>
         <thead><tr><th>Month</th><th>Amount</th><th>Source account</th><th>Transaction</th><th>Posted date</th><th>Status</th></tr></thead>
         <tbody>${(receipt.contributions || []).map(contribution =>
           `<tr>
             <td>${escapeHtml(contribution.contributionMonth)}</td>
             <td>₹${formatAmount(contribution.amount)}</td>
             <td>${escapeHtml(contribution.sourceAccountNumber)}</td>
             <td>${escapeHtml(contribution.transactionId)}</td>
             <td>${escapeHtml(formatDate(contribution.postedDate))}</td>
             <td>${escapeHtml(contribution.status || 'POSTED')}</td>
           </tr>`
         ).join('') || '<tr><td colspan="6">No installments have been posted.</td></tr>'}</tbody>
       </table>`
    : '';
  const generatedAt = new Date().toLocaleString('en-IN');

  printWindow.document.write(`<!doctype html>
    <html><head><meta charset="utf-8"><title>NeoBank ${productName} Receipt</title>
      <style>
        * { box-sizing: border-box; }
        body { margin: 0; padding: 36px; color: #172033; font: 14px Arial, sans-serif; }
        main { max-width: 760px; margin: auto; border: 2px solid #0876a8; border-radius: 12px; padding: 28px; }
        header { display: flex; justify-content: space-between; align-items: center; gap: 20px; border-bottom: 3px solid #0786b5; padding-bottom: 16px; }
        h1 { margin: 0; color: #075985; font-size: 23px; }
        header p { margin: 5px 0 0; color: #526170; font-size: 12px; }
        .brand { color: #0876a8; font-size: 15px; font-weight: 700; text-align: right; }
        h2 { margin: 22px 0 12px; text-align: center; color: #075985; font-size: 18px; }
        h3 { margin: 22px 0 10px; color: #075985; font-size: 15px; }
        table { width: 100%; border-collapse: collapse; margin: 12px 0; }
        th, td { border: 1px solid #cbd5e1; padding: 9px 10px; text-align: left; vertical-align: top; }
        th { background: #eff8fc; font-weight: 600; }
        td { overflow-wrap: anywhere; }
        .generated { margin-top: 20px; padding-top: 12px; border-top: 1px solid #cbd5e1; color: #64748b; font-size: 11px; }
        @media print { body { padding: 0; } main { border-radius: 0; } }
      </style>
    </head><body><main>
      <header><div><h1>NeoBank</h1><p>Term Deposit Account Record</p></div><div class="brand">${productName}</div></header>
      <h2>${productName} Receipt</h2>
      <table><tbody>${accountRows.join('')}</tbody></table>
      ${contributions}
      <p class="generated">System-generated account record · Generated ${escapeHtml(generatedAt)}</p>
    </main></body></html>`);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
}
