export interface FundTransferReceiptData {
  transferId?: string;
  senderName?: string;
  senderAccountNumber?: string;
  senderAccountType?: string;
  senderChequeNumber?: string;
  receiverName?: string;
  receiverAccountNumber?: string;
  receiverAccountType?: string;
  transferCategory?: string;
  loanAccountNumber?: string;
  loanPaymentType?: string;
  prepaymentAdjustment?: string;
  amount?: number;
  transferCharge?: number;
  status?: string;
  performedBy?: string;
  performedAt?: string;
  description?: string;
  outstandingPrincipalBefore?: number;
  outstandingPrincipalAfter?: number;
  remainingInterestBefore?: number;
  remainingInterestAfter?: number;
  emiAmountBefore?: number;
  emiAmountAfter?: number;
  remainingTenureBefore?: number;
  remainingTenureAfter?: number;
  principalPaid?: number;
  interestPaid?: number;
  charges?: number;
  interestSaved?: number;
}

export function printFundTransferReceipt(transfer: FundTransferReceiptData): boolean {
  const printWindow = window.open('', '_blank', 'width=850,height=950');
  if (!printWindow) return false;

  const escapeHtml = (value: unknown): string => String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
  const money = (value: number | undefined): string => value == null
    ? 'N/A'
    : `₹${Number(value).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  const valueRow = (label: string, value: string): string =>
    `<tr><th>${escapeHtml(label)}</th><td>${escapeHtml(value)}</td></tr>`;
  const amountRow = (label: string, value: number | undefined): string =>
    value == null ? '' : valueRow(label, money(value));
  const tenureRow = (label: string, value: number | undefined): string =>
    value == null ? '' : valueRow(label, `${value} months`);
  const isLoanPayment = Boolean(transfer.loanAccountNumber || transfer.loanPaymentType);
  const adjustment = transfer.prepaymentAdjustment === 'REDUCE_EMI'
    ? 'Reduce EMI'
    : transfer.prepaymentAdjustment === 'REDUCE_TENURE'
      ? 'Reduce tenure'
      : '';
  const loanRows = isLoanPayment
    ? [
        valueRow('Loan account', transfer.loanAccountNumber || transfer.receiverAccountNumber || 'N/A'),
        valueRow('Payment type', transfer.loanPaymentType || 'N/A'),
        adjustment ? valueRow('Prepayment adjustment', adjustment) : '',
        amountRow('Principal outstanding before', transfer.outstandingPrincipalBefore),
        amountRow('Principal outstanding after', transfer.outstandingPrincipalAfter),
        amountRow('Interest remaining before', transfer.remainingInterestBefore),
        amountRow('Interest remaining after', transfer.remainingInterestAfter),
        amountRow('EMI before adjustment', transfer.emiAmountBefore),
        amountRow('EMI after adjustment', transfer.emiAmountAfter),
        tenureRow('Remaining tenure before', transfer.remainingTenureBefore),
        tenureRow('Remaining tenure after', transfer.remainingTenureAfter),
        amountRow('Principal applied', transfer.principalPaid),
        amountRow('Interest paid', transfer.interestPaid),
        amountRow('Charges / taxes', transfer.charges),
        amountRow('Estimated interest saved', transfer.interestSaved)
      ].join('')
    : '';
  const now = new Date().toLocaleString('en-IN');
  const title = isLoanPayment ? 'Gold Loan Payment Receipt' : 'Fund Transfer Receipt';
  const date = transfer.performedAt ? new Date(transfer.performedAt).toLocaleString('en-IN') : 'N/A';

  printWindow.document.write(`<!DOCTYPE html>
    <html><head><title>NeoBank ${title} - ${escapeHtml(transfer.transferId || '')}</title>
    <style>
      * { box-sizing: border-box; }
      body { font-family: Arial, sans-serif; color: #1a1a2e; margin: 0; padding: 32px; }
      .receipt { max-width: 720px; margin: auto; border: 2px solid #0056b3; padding: 28px; }
      header { display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #e63946; padding-bottom: 16px; }
      h1 { color: #0056b3; font-size: 24px; margin: 0; }
      .tagline { color: #666; font-size: 12px; }
      h2 { color: #e63946; text-align: center; font-size: 19px; margin: 22px 0; }
      h3 { color: #0056b3; font-size: 15px; margin: 20px 0 8px; }
      table { width: 100%; border-collapse: collapse; margin: 10px 0; }
      th, td { border: 1px solid #d1d5db; padding: 9px 12px; font-size: 13px; text-align: left; }
      th { width: 48%; background: #f1f5f9; }
      .note { margin-top: 22px; padding-top: 12px; border-top: 1px solid #d1d5db; color: #555; font-size: 11px; }
      .generated { text-align: right; color: #777; font-size: 10px; margin-top: 12px; }
      @media print { body { padding: 10px; } .receipt { border: 2px solid #0056b3; } }
    </style></head><body>
    <main class="receipt">
      <header><div><h1>NeoBank</h1><span class="tagline">Digital Banking Solutions</span></div>
        <span class="tagline">...the digital bank you can trust!</span></header>
      <h2>${title}</h2>
      <table>
        ${valueRow('Transfer reference', transfer.transferId || 'N/A')}
        ${valueRow('Transaction date', date)}
        ${valueRow('Status', transfer.status || 'N/A')}
        ${valueRow('Sender', `${transfer.senderName || 'N/A'} (${transfer.senderAccountNumber || 'N/A'})`)}
        ${valueRow('Sender account type', transfer.senderAccountType || 'N/A')}
        ${valueRow('Receiver / borrower', `${transfer.receiverName || 'N/A'} (${transfer.receiverAccountNumber || 'N/A'})`)}
        ${valueRow('Transfer category', transfer.transferCategory || 'ACCOUNT_TO_ACCOUNT')}
        ${amountRow('Payment amount', transfer.amount)}
        ${amountRow('Transfer charge', transfer.transferCharge)}
        ${transfer.senderChequeNumber ? valueRow('Cheque number', transfer.senderChequeNumber) : ''}
      </table>
      ${isLoanPayment ? `<h3>Loan repayment and schedule adjustment</h3><table>${loanRows}</table>` : ''}
      ${transfer.description ? `<p><strong>Description:</strong> ${escapeHtml(transfer.description)}</p>` : ''}
      ${transfer.performedBy ? `<p><strong>Processed by:</strong> ${escapeHtml(transfer.performedBy)}</p>` : ''}
      <p class="note">This is a computer-generated receipt from NeoBank Digital Banking and does not require a signature.</p>
      <p class="generated">Receipt generated: ${escapeHtml(now)}</p>
    </main>
    <script>window.addEventListener('load', function () { window.focus(); window.print(); });</script>
    </body></html>`);
  printWindow.document.close();
  return true;
}
