export type LoanDocKind = 'noc' | 'receipt';
export type LoanDocAudience = 'user' | 'admin';
export type LoanDocLoanType = 'personal' | 'gold';

const CLOSED_STATUSES = ['foreclosed', 'paid', 'closed'];

export function isLoanClosed(loan: any): boolean {
  return !!loan && CLOSED_STATUSES.includes(String(loan.status || '').toLowerCase());
}

const esc = (v: any): string =>
  String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

const money = (v: any): string => {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '0.00';
  return Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const date = (v: any, withTime = false): string => {
  if (!v) return '-';
  const d = new Date(v);
  if (isNaN(d.getTime())) return '-';
  return withTime ? d.toLocaleString('en-IN') : d.toLocaleDateString('en-IN');
};

const SEAL_SVG = `<svg class="seal" width="130" height="130" viewBox="0 0 130 130" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <path id="sealTop" d="M 20 65 A 45 45 0 0 1 110 65"/>
    <path id="sealBottom" d="M 16 65 A 49 49 0 0 0 114 65"/>
  </defs>
  <circle cx="65" cy="65" r="61" fill="none" stroke="#0056b3" stroke-width="3"/>
  <circle cx="65" cy="65" r="55" fill="none" stroke="#0056b3" stroke-width="1"/>
  <circle cx="65" cy="65" r="30" fill="none" stroke="#e63946" stroke-width="2"/>
  <text font-size="11" font-weight="700" fill="#0056b3" letter-spacing="2"><textPath href="#sealTop" startOffset="50%" text-anchor="middle">NEOBANK</textPath></text>
  <text font-size="8" font-weight="600" fill="#0056b3" letter-spacing="1"><textPath href="#sealBottom" startOffset="50%" text-anchor="middle">DIGITAL BANKING</textPath></text>
  <text x="65" y="74" font-size="24" font-weight="800" fill="#e63946" text-anchor="middle">N</text>
</svg>`;

const STYLE = `
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Segoe UI', Arial, sans-serif; padding: 40px; background: #fff; color: #1a1a2e; }
.receipt { max-width: 700px; margin: 0 auto; border: 2px solid #0056b3; padding: 30px; }
.header { display: flex; align-items: center; justify-content: space-between; border-bottom: 3px solid #e63946; padding-bottom: 16px; margin-bottom: 20px; }
.bank-logo { display: flex; align-items: center; gap: 12px; }
.bank-icon { width: 56px; height: 56px; background: linear-gradient(135deg, #0056b3, #00a8ff); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; font-weight: bold; }
.bank-name h2 { color: #0056b3; font-size: 22px; margin-bottom: 2px; }
.bank-name p { color: #666; font-size: 11px; font-style: italic; }
.bank-tagline { color: #e63946; font-size: 12px; font-weight: 600; text-align: right; }
.title { text-align: center; font-size: 18px; font-weight: 700; color: #e63946; margin: 20px 0; }
table { width: 100%; border-collapse: collapse; margin: 12px 0; }
table th, table td { border: 1px solid #ccc; padding: 10px 14px; text-align: center; font-size: 13px; }
table th { background: #f0f4f8; color: #333; font-weight: 600; }
.summary { margin: 16px 0; font-size: 14px; line-height: 1.7; color: #333; text-align: justify; }
.summary strong { color: #0056b3; }
.meta { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 14px; color: #333; }
.section { margin: 18px 0; padding: 14px; border: 2px solid #e63946; border-radius: 8px; background: #fff5f5; }
.section-title { text-align: center; font-size: 15px; font-weight: 700; color: #e63946; margin-bottom: 10px; }
.sign-row { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 30px; }
.sign { text-align: center; font-size: 12px; color: #333; }
.sign .line { border-top: 1px solid #333; width: 200px; margin: 0 auto 6px; }
.footer { margin-top: 24px; padding-top: 16px; border-top: 2px solid #e63946; }
.note { font-size: 12px; color: #555; margin-bottom: 14px; font-style: italic; }
.instructions { font-size: 12px; color: #444; }
.instructions ol { padding-left: 20px; }
.instructions li { margin-bottom: 6px; line-height: 1.5; }
.generated { text-align: right; font-size: 11px; color: #888; margin-top: 16px; }
@media print { body { padding: 20px; } }
`;

const HEADER = `<div class="header">
  <div class="bank-logo"><div class="bank-icon">N</div>
    <div class="bank-name"><h2>NeoBank</h2><p>Digital Banking Solutions</p></div></div>
  <div class="bank-tagline">...the digital bank you can trust!</div>
</div>`;

function row(headers: string[], values: any[]): string {
  return `<tr>${headers.map(h => `<th>${esc(h)}</th>`).join('')}</tr><tr>${values.map(v => `<td>${esc(v)}</td>`).join('')}</tr>`;
}

function buildNoc(loan: any, loanType: LoanDocLoanType, audience: LoanDocAudience, customerName: string): { title: string; body: string } {
  const loanLabel = loanType === 'gold' ? 'Gold Loan' : (loan.type || 'Personal Loan');
  const loanNo = loan.loanAccountNumber || `LN-${loan.id}`;
  const closureDate = loan.foreclosureDate || loan.closureDate || loan.updatedAt;
  const amount = loanType === 'gold' ? loan.loanAmount : loan.amount;
  const today = new Date();
  const ymd = `${today.getFullYear()}${String(today.getMonth() + 1).padStart(2, '0')}${String(today.getDate()).padStart(2, '0')}`;
  const nocRef = `NOC/NB/${esc(loanNo)}/${ymd}`;

  const adminRows = audience === 'admin'
    ? `<table>${row(['Customer ID', 'Registered Email', 'Account Number'], [loan.customerId || '-', loan.userEmail || '-', loan.accountNumber || '-'])}
       ${row(['Approved By', 'Closed / Foreclosed By', 'Loan Status'], [loan.approvedBy || '-', loan.foreclosedBy || '-', loan.status || '-'])}</table>`
    : '';
  const goldRows = loanType === 'gold'
    ? `<table>${row(['Pledged Gold Items', 'Verified Weight', 'Purity'], [loan.goldItems || '-', (loan.verifiedGoldGrams ?? loan.goldGrams ?? '-') + ' g', loan.goldPurity || '-'])}</table>
       <p class="summary">The gold ornaments pledged against this loan stand released and may be collected by the borrower from ${esc(loan.storageLocation || 'the designated NeoBank branch')} upon presenting this certificate and valid identification.</p>`
    : '';

  const body = `
    <div class="title">No Objection Certificate (NOC) – Loan Closure</div>
    <div class="meta"><span><strong>NOC Ref No:</strong> ${nocRef}</span><span><strong>Date of Issue:</strong> ${date(today)}</span></div>
    <p class="summary">This is to certify that the <strong>${esc(loanLabel)}</strong> bearing Loan Account Number <strong>${esc(loanNo)}</strong>
    sanctioned to <strong>${esc(customerName)}</strong> (Account No. <strong>${esc(loan.accountNumber || '-')}</strong>) for an amount of
    <strong>INR ${money(amount)}</strong> has been <strong>fully closed</strong> on <strong>${date(closureDate)}</strong>.
    NeoBank has <strong>no objection</strong> and <strong>no outstanding dues</strong> against the said loan account, and all liens, charges and
    securities held against it stand released.</p>
    <table>
      ${row(['Loan Account Number', 'Customer Name', 'Loan Type'], [loanNo, customerName, loanLabel])}
      ${row(['Loan Amount', 'Interest Rate', 'Tenure'], ['INR ' + money(amount), (loan.interestRate ?? '-') + '% p.a.', (loan.tenure ?? '-') + ' months'])}
      ${row(['Date of Sanction', 'Date of Closure', 'Closure Status'], [date(loan.approvalDate), date(closureDate), 'CLOSED'])}
    </table>
    ${adminRows}
    ${goldRows}
    <div class="section">
      <div class="section-title">Closure Settlement Summary</div>
      <table>
        ${row(['Principal Paid', 'Interest Paid', 'Closure Charges'], ['₹' + money(loan.principalPaid), '₹' + money(loan.interestPaid), '₹' + money(loan.foreclosureCharges)])}
        ${row(['GST on Charges', 'Total Amount Settled', 'Outstanding Balance'], ['₹' + money(loan.foreclosureGst), '₹' + money(loan.foreclosureAmount), '₹0.00'])}
      </table>
    </div>
    <div class="sign-row">
      <div class="sign">${SEAL_SVG}<div>NeoBank Official Seal</div></div>
      <div class="sign"><div class="line"></div><strong>Authorised Signatory</strong><br/>NeoBank Digital Banking<br/>Date: ${date(today)}</div>
    </div>
    <div class="footer">
      <p class="note">This is a computer generated No Objection Certificate from NeoBank Digital Banking and bears the official NeoBank seal.</p>
      <div class="instructions"><p><strong>Important:</strong></p><ol>
        <li>This certificate is issued on the request of the borrower, confirming closure of the above loan account.</li>
        <li>Please retain this NOC for future reference such as credit bureau updates or fresh loan applications.</li>
        <li>For any discrepancy, contact NeoBank support with the NOC reference number.</li>
      </ol></div>
      <p class="generated">Certificate generated on: ${today.toLocaleString('en-IN')}</p>
    </div>`;
  return { title: `NeoBank NOC - ${loanNo}`, body };
}

function buildReceipt(loan: any, loanType: LoanDocLoanType, audience: LoanDocAudience, customerName: string): { title: string; body: string } {
  const loanLabel = loanType === 'gold' ? 'Gold Loan' : (loan.type || 'Personal Loan');
  const loanNo = loan.loanAccountNumber || `LN-${loan.id}`;
  const amount = loanType === 'gold' ? loan.loanAmount : loan.amount;
  const closed = isLoanClosed(loan);
  const now = new Date();

  const adminRows = audience === 'admin'
    ? `<table>${row(['Customer ID', 'Registered Email', 'Current Balance'], [loan.customerId || '-', loan.userEmail || '-', '₹' + money(loan.currentBalance)])}
       ${row(['Approved By', 'Application Date', 'EMI Start Date'], [loan.approvedBy || '-', date(loan.applicationDate, true), date(loan.emiStartDate)])}
       ${loanType === 'gold'
         ? row(['OTP Verified', 'Processing Charges', 'Renewals'], [loan.otpVerified ? 'Yes' : 'No', '₹' + money(loan.processingCharges), loan.renewalCount ?? 0])
         : row(['CIBIL Score', 'PAN', 'Purpose'], [loan.cibilScore || 'N/A', loan.pan || '-', loan.purpose || '-'])}</table>`
    : '';

  const goldRows = loanType === 'gold'
    ? `<table>
        ${row(['Gold Weight (Applied)', 'Gold Rate / gram', 'Gold Value'], [(loan.goldGrams ?? '-') + ' g', '₹' + money(loan.goldRatePerGram), '₹' + money(loan.goldValue)])}
        ${row(['Verified Weight', 'Verified Purity', 'Verified Value'], [(loan.verifiedGoldGrams ?? '-') + ' g', loan.goldPurity || '-', '₹' + money(loan.verifiedGoldValue)])}
        ${row(['Gold Items', 'Storage Location', 'Terms Accepted'], [loan.goldItems || '-', loan.storageLocation || '-', loan.termsAccepted ? 'Yes (' + date(loan.termsAcceptedDate) + ')' : 'No'])}
       </table>` : '';

  const closureBlock = closed
    ? `<div class="section"><div class="section-title">Loan Closure / Foreclosure Details</div><table>
        ${row(['Closure Status', 'Date of Closure', 'Total Amount Settled'], [loan.status, date(loan.foreclosureDate), '₹' + money(loan.foreclosureAmount)])}
        ${row(['Principal Paid', 'Interest Paid', 'Foreclosure Charges'], ['₹' + money(loan.principalPaid), '₹' + money(loan.interestPaid), '₹' + money(loan.foreclosureCharges)])}
       </table>${audience === 'admin' && loan.foreclosedBy ? `<p class="summary" style="text-align:right"><em>Processed by: ${esc(loan.foreclosedBy)}</em></p>` : ''}</div>`
    : '';

  const body = `
    <div class="title">e-${esc(loanLabel)} Account Receipt</div>
    <table>${row(['Loan Account No', 'Customer Name'], [loanNo, customerName])}</table>
    <p class="summary">Loan Amount: <strong>INR|${money(amount)}</strong> for a period of <strong>${esc(loan.tenure ?? '-')}(months)</strong>
    at the rate of <strong>${esc(loan.interestRate ?? '-')}%</strong> per annum.</p>
    <table>
      ${row(['Date of Approval', 'EMI Start Date', 'Loan Status'], [date(loan.approvalDate), date(loan.emiStartDate), loan.status || '-'])}
      ${row(['Loan Scheme', 'Repayment Mode', 'Linked Account'], [loanLabel.toUpperCase().replace(/\s+/g, ''), 'Monthly EMI', loan.accountNumber || '-'])}
    </table>
    ${goldRows}
    ${adminRows}
    ${closureBlock}
    <div class="footer">
      <p class="note">This is a computer generated e-loan receipt from NeoBank Digital Banking, hence does not require any signature.</p>
      <div class="instructions"><p><strong>Important Instructions:</strong></p><ol>
        <li>This is not transferable.</li>
        <li>This confirmation is issued as per the mandate of the customer. In case of discrepancy please contact support within 7 days of issue.</li>
        <li>EMI must be paid on or before the due date; delayed payments may attract penal charges as per Bank Guidelines.</li>
        <li>Foreclosure of the loan before tenure end is allowed subject to foreclosure charges and applicable GST.</li>
        ${loanType === 'gold' ? '<li>Pledged gold will be released only after full repayment of the loan and closure formalities.</li>' : ''}
      </ol></div>
      <p class="generated">Receipt generated on: ${now.toLocaleString('en-IN')}</p>
    </div>`;
  return { title: `NeoBank ${loanLabel} Receipt - ${loanNo}`, body };
}

/** Opens a printable (Save as PDF) NOC or receipt window. Returns false if popups are blocked. */
export function openLoanDocument(
  kind: LoanDocKind,
  loan: any,
  loanType: LoanDocLoanType,
  audience: LoanDocAudience,
  customerName?: string
): boolean {
  const name = customerName || loan.userName || '-';
  const doc = kind === 'noc'
    ? buildNoc(loan, loanType, audience, name)
    : buildReceipt(loan, loanType, audience, name);

  const w = window.open('', '_blank', 'width=800,height=900');
  if (!w) return false;
  w.document.write(`<!DOCTYPE html><html><head><title>${esc(doc.title)}</title><style>${STYLE}</style></head><body>
    <div class="receipt">${HEADER}${doc.body}</div>
    <script>window.onload = function() { window.print(); }</script></body></html>`);
  w.document.close();
  return true;
}
