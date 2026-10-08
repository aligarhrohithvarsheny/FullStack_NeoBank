const esc = (v: any) => String(v ?? '-').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c] as string));
const money = (v: any) => '\u20b9' + Number(v || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 });
const date = (v: any) => (v ? new Date(v).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-');

const row = (k: string, v: any) => `<tr><th>${k}</th><td>${esc(v)}</td></tr>`;

export function printHomeLoanDocuments(l: any, schedule: any[] = []) {
  const rows = (schedule || []).slice(0, 24).map((s: any, i: number) =>
    `<tr><td>${esc(s.installment ?? s.no ?? i + 1)}</td><td>${date(s.dueDate ?? s.date)}</td><td>${money(s.emi)}</td><td>${money(s.principal)}</td><td>${money(s.interest)}</td><td>${money(s.balance)}</td></tr>`).join('');

  const head = (title: string, page: number) => `
    <div class="hd"><div><div class="bank">NeoBank</div><div class="sub">Home Loan Division</div></div>
    <div class="ref">Application ID: <b>${esc(l.applicationId)}</b><br>Loan A/c: <b>${esc(l.loanAccountNumber)}</b></div></div>
    <h2>${title}</h2><div class="pg">Page ${page} of 3</div>`;

  const html = `<!doctype html><html><head><meta charset="utf-8"><title>Home Loan ${esc(l.applicationId)}</title>
<style>
*{box-sizing:border-box}body{font-family:'Segoe UI',Arial,sans-serif;color:#0f172a;margin:0}
.page{width:210mm;min-height:285mm;padding:16mm;margin:auto;page-break-after:always;border:1px solid #cbd5e1;position:relative}
.page:last-child{page-break-after:auto}
.hd{display:flex;justify-content:space-between;border-bottom:3px solid #1d4ed8;padding-bottom:8px}
.bank{font-size:26px;font-weight:800;color:#1d4ed8}.sub{color:#64748b;font-size:12px}.ref{text-align:right;font-size:12px}
h2{margin:16px 0 4px;color:#1e3a8a}.pg{font-size:11px;color:#64748b;margin-bottom:10px}
table{width:100%;border-collapse:collapse;margin:8px 0;font-size:13px}
th,td{border:1px solid #e2e8f0;padding:7px 9px;text-align:left}th{background:#f1f5f9;width:38%}
.grid th{width:auto}
.fd{border:2px double #1d4ed8;border-radius:10px;padding:14px;background:#f8fbff}
.big{font-size:28px;font-weight:800;color:#166534}
.sig{display:flex;justify-content:space-between;margin-top:60px}
.sig div{width:44%;border-top:1px solid #0f172a;padding-top:6px;text-align:center;font-size:12px}
.note{font-size:11px;color:#64748b;margin-top:14px}
.stamp{position:absolute;right:18mm;bottom:40mm;border:3px solid #16a34a;color:#16a34a;padding:6px 16px;border-radius:8px;font-weight:800;transform:rotate(-8deg);opacity:.8}
@media print{.page{border:0}}
</style></head><body>

<div class="page">${head('Home Loan Sanction &amp; Customer Details', 1)}
  <h3>Customer details</h3>
  <table>${row('Customer name', l.userName)}${row('Savings account', l.accountNumber)}${row('Email', l.userEmail)}${row('PAN', l.pan)}</table>
  <h3>Loan details</h3>
  <table>${row('Sanctioned amount', money(l.amount))}${row('Tenure', (l.tenure ?? '-') + ' months')}${row('Interest rate', (l.interestRate ?? '-') + '% p.a.')}
  ${row('Monthly EMI', money(l.emi))}${row('Purpose', l.purpose)}${row('Property address', l.propertyAddress)}${row('Property value', money(l.propertyValue))}
  ${row('Application date', date(l.applicationDate))}${row('Approval date', date(l.approvalDate))}${row('Approved by', l.approvedBy)}${row('First EMI due', date(l.nextEmiDate))}</table>
  <h3>Documents verified</h3>
  <table>${row('FD receipt', l.fdReceiptStatus)}${row('Home loan model', l.modelDocStatus)}${row('Signature', l.signatureStatus)}</table>
  <div class="stamp">APPROVED</div>
</div>

<div class="page">${head('Home Loan Receipt (FD-style Disbursement Certificate)', 2)}
  <div class="fd">
    <div style="font-size:12px;color:#64748b">Net amount credited to savings account ${esc(l.accountNumber)}</div>
    <div class="big">${money(l.netDisbursed)}</div>
    <div style="font-size:12px;color:#64748b">Date: ${date(l.approvalDate)}</div>
  </div>
  <h3>Amount breakdown</h3>
  <table>${row('Sanctioned loan amount', money(l.amount))}${row('Processing fee', money(l.processingFee))}${row('GST on processing fee', money(l.gstOnFee))}
  ${row('Legal charges', money(l.legalCharges))}${row('Total deducted', money(l.totalCharges))}${row('Net disbursed', money(l.netDisbursed))}</table>
  <h3>Repayment position</h3>
  <table>${row('Outstanding principal', money(l.remainingPrincipal))}${row('EMIs paid', l.paidEmis)}${row('Remaining tenure', (l.remainingTenure ?? '-') + ' months')}
  ${row('Interest paid', money(l.interestPaid))}${row('Prepaid amount', money(l.prepaidAmount))}${row('Status', l.status)}</table>
  <p class="note">This is a computer generated certificate and is valid without a physical signature.</p>
</div>

<div class="page">${head('Home Loan Model &amp; Signature', 3)}
  <h3>Repayment model (first ${Math.min(24, (schedule || []).length)} instalments)</h3>
  <table class="grid"><tr><th>#</th><th>Due date</th><th>EMI</th><th>Principal</th><th>Interest</th><th>Balance</th></tr>${rows || '<tr><td colspan="6">Schedule not available</td></tr>'}</table>
  <div class="sig"><div>Customer signature<br><small>${esc(l.userName)}</small></div><div>Authorised signatory<br><small>NeoBank Home Loan Division</small></div></div>
  <p class="note">By signing, the customer confirms acceptance of the sanction terms, charges and repayment schedule.</p>
</div>
<script>window.onload=function(){setTimeout(function(){window.print()},300)}</script>
</body></html>`;

  const w = window.open('', '_blank');
  if (!w) { alert('Please allow pop-ups to generate the documents'); return; }
  w.document.open(); w.document.write(html); w.document.close();
}

export function printHomeLoanNoc(n: any) {
  const l = n.loan || {};
  const html = `<!doctype html><html><head><meta charset="utf-8"><title>NOC ${esc(n.nocNumber)}</title>
<style>
body{font-family:'Segoe UI',Arial,sans-serif;color:#0f172a;margin:0;padding:24px}
.c{max-width:780px;margin:auto;border:6px double #1d4ed8;padding:36px 44px;position:relative}
.hd{text-align:center;border-bottom:3px solid #1d4ed8;padding-bottom:10px}
.bank{font-size:30px;font-weight:800;color:#1d4ed8}
h1{text-align:center;letter-spacing:2px;margin:24px 0 4px;color:#1e3a8a}
.meta{display:flex;justify-content:space-between;font-size:13px;margin:14px 0}
p{line-height:1.8;font-size:15px;text-align:justify}
table{width:100%;border-collapse:collapse;margin:14px 0;font-size:14px}th,td{border:1px solid #cbd5e1;padding:7px 10px;text-align:left}th{background:#f1f5f9;width:38%}
.sig{margin-top:70px;display:flex;justify-content:space-between}.sig div{border-top:1px solid #0f172a;padding-top:6px;width:42%;text-align:center;font-size:13px}
.seal{position:absolute;right:50px;top:150px;border:3px solid #16a34a;color:#16a34a;font-weight:800;padding:6px 14px;border-radius:8px;transform:rotate(-10deg);opacity:.75}
</style></head><body><div class="c">
<div class="hd"><div class="bank">NeoBank</div><div style="font-size:12px;color:#64748b">Home Loan Division</div></div>
<div class="seal">LOAN CLOSED</div>
<h1>NO OBJECTION CERTIFICATE</h1>
<div class="meta"><span>NOC No: <b>${esc(n.nocNumber)}</b></span><span>Date: <b>${date(n.issuedOn)}</b></span></div>
<p>This is to certify that the Home Loan account <b>${esc(l.loanAccountNumber)}</b> (Application ID <b>${esc(l.applicationId)}</b>) sanctioned to
<b>${esc(l.userName)}</b>, holder of savings account <b>${esc(l.accountNumber)}</b>, has been fully repaid and closed on <b>${date(l.closureDate)}</b>.
NeoBank has <b>no objection</b> and no further claim against the borrower in respect of this loan, and any lien or charge created on the property or deposits
as security for this loan stands released.</p>
<table>${row('Borrower', l.userName)}${row('Loan account', l.loanAccountNumber)}${row('Total sanctioned (incl. top-ups)', money(l.amount))}
${row('Principal repaid', money(l.principalPaid))}${row('Interest paid', money(l.interestPaid))}${row('Closure amount', money(l.closureAmount))}
${row('Property', l.propertyAddress)}${row('Closure date', date(l.closureDate))}</table>
<p style="font-size:12px;color:#64748b">This is a computer generated certificate issued by NeoBank. Please retain it for your records.</p>
<div class="sig"><div>Borrower<br><small>${esc(l.userName)}</small></div><div>Authorised signatory<br><small>NeoBank Home Loan Division</small></div></div>
</div><script>window.onload=function(){setTimeout(function(){window.print()},300)}</script></body></html>`;
  const w = window.open('', '_blank');
  if (!w) { alert('Please allow pop-ups to generate the NOC'); return; }
  w.document.open(); w.document.write(html); w.document.close();
}
