import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { printFundTransferReceipt } from '../../../service/fund-transfer-receipt';

@Component({
  selector: 'app-homeloan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="hl">
  <div class="hero">
    <div><h2>🏠 Home Loan</h2><p>Apply online in 3 simple steps. Track your application and manage your loan in one place.</p></div>
    <div class="hero-badge">Rates from 8.5% p.a.</div>
  </div>
  <div class="msg ok" *ngIf="msg">✔ {{msg}}</div>
  <div class="msg err" *ngIf="err">⚠ {{err}}</div>

  <div class="tabs">
    <button [class.on]="tab==='apply'" (click)="tab='apply'">📝 Apply</button>
    <button [class.on]="tab==='mine'" (click)="tab='mine'">📂 My Loans</button>
    <button [class.on]="tab==='consolidated'" (click)="openConsolidated()">📊 Consolidated Statement</button>
  </div>

  <div *ngIf="tab==='apply'">
    <div class="steps">
      <div class="step" [class.active]="step===1" [class.done]="step>1"><span>1</span><div><b>Loan details</b><small>Amount, tenure, rate</small></div></div>
      <div class="step" [class.active]="step===2" [class.done]="step>2"><span>2</span><div><b>Review &amp; terms</b><small>Accept conditions</small></div></div>
      <div class="step" [class.active]="step===3"><span>3</span><div><b>Submit</b><small>Get Application ID</small></div></div>
    </div>

    <div class="card" *ngIf="step===1">
      <h3>Step 1 · Enter your loan details</h3>
      <div class="grid">
        <label>Loan Amount (₹)<input type="number" [(ngModel)]="form.amount" (ngModelChange)="calc()" min="100000"><small class="hint">Minimum ₹1,00,000</small></label>
        <label>Tenure (months)<input type="number" [(ngModel)]="form.tenure" (ngModelChange)="calc()" min="12" max="360"><small class="hint">12 to 360 months</small></label>
        <label>Interest Rate (% p.a.)<input type="number" step="0.05" [(ngModel)]="form.interestRate" (ngModelChange)="calc()"><small class="hint">Indicative rate; final rate set at approval</small></label>
        <label>Property Value (₹)<input type="number" [(ngModel)]="form.propertyValue" (ngModelChange)="calc()"><small class="hint">Loan up to 90% of property value</small></label>
        <label>Purpose<input [(ngModel)]="form.purpose" placeholder="e.g. Purchase of apartment"></label>
        <label>Property Address<input [(ngModel)]="form.propertyAddress" placeholder="Full address of the property"></label>
      </div>
      <div class="emi-box" *ngIf="emiPreview">
        <div><small>Estimated EMI</small><b>₹{{emiPreview | number:'1.0-0'}}</b></div>
        <div><small>Total interest</small><b>₹{{(emiPreview*form.tenure-form.amount) | number:'1.0-0'}}</b></div>
        <div><small>Total payable</small><b>₹{{(emiPreview*form.tenure) | number:'1.0-0'}}</b></div>
      </div>
      <div *ngIf="analysis" class="ai">
        🤖 AI pre-check: approval probability <b>{{analysis.approvalProbability}}%</b> ({{analysis.riskBand}} risk), FOIR {{analysis.foirPercent}}%
      </div>
      <div class="msg err" *ngIf="formError()">{{formError()}}</div>
      <div class="actions end"><button class="btn" (click)="next()" [disabled]="!!formError()">Continue →</button></div>
    </div>

    <div class="card" *ngIf="step===2">
      <h3>Step 2 · Review &amp; accept terms</h3>
      <div class="review">
        <div><small>Loan amount</small><b>₹{{form.amount | number}}</b></div>
        <div><small>Tenure</small><b>{{form.tenure}} months</b></div>
        <div><small>Interest rate</small><b>{{form.interestRate}}% p.a.</b></div>
        <div><small>Estimated EMI</small><b>₹{{emiPreview | number:'1.0-0'}}</b></div>
        <div><small>Property value</small><b>₹{{form.propertyValue | number}}</b></div>
        <div><small>Purpose</small><b>{{form.purpose || '-'}}</b></div>
        <div class="wide"><small>Property address</small><b>{{form.propertyAddress || '-'}}</b></div>
      </div>
      <div class="terms">
        <h4>Terms &amp; Conditions</h4>
        <ol>
          <li>Submitting this form is only an application. Approval is at the sole discretion of NeoBank after document verification.</li>
          <li>You must upload three documents when requested: FD receipt, Home Loan model and signature. Documents can be re-uploaded if the bank asks.</li>
          <li>The interest rate shown is indicative and may change at approval based on credit assessment.</li>
          <li>A processing fee and applicable GST are deducted from the sanctioned amount at disbursal.</li>
          <li>Prepayment attracts 1% charge plus 18% GST. Foreclosure attracts 2% of the outstanding principal plus GST and one month's interest.</li>
          <li>EMIs are due monthly. Delayed EMIs may attract penal interest and affect your credit profile.</li>
          <li>The property remains security for the loan until it is fully repaid and closed.</li>
          <li>You may cancel the application any time before approval, free of charge.</li>
          <li>You confirm that all information provided is true and complete.</li>
        </ol>
        <label class="check"><input type="checkbox" [(ngModel)]="agreed"> I have read and agree to the Terms &amp; Conditions</label>
      </div>
      <div class="actions"><button class="btn ghost" (click)="step=1">← Back</button>
        <button class="btn" (click)="step=3" [disabled]="!agreed">Continue →</button></div>
    </div>

    <div class="card" *ngIf="step===3">
      <h3>Step 3 · Submit your application</h3>
      <p>You are applying for <b>₹{{form.amount | number}}</b> over <b>{{form.tenure}} months</b> (EMI ≈ ₹{{emiPreview | number:'1.0-0'}}).</p>
      <h4>What happens next?</h4>
      <div class="timeline">
        <div><span>1</span>You receive an <b>Application ID</b> instantly</div>
        <div><span>2</span>The bank reviews your request</div>
        <div><span>3</span>Upload FD receipt, Home Loan model and signature</div>
        <div><span>4</span>Documents are verified (re-upload if asked)</div>
        <div><span>5</span>Loan approved and credited to your savings account</div>
      </div>
      <div class="actions"><button class="btn ghost" (click)="step=2">← Back</button>
        <button class="btn" (click)="submit()" [disabled]="busy || !agreed">{{busy ? 'Submitting...' : 'Submit Application'}}</button></div>
    </div>
  </div>

<div *ngIf="tab==='mine'">
    <div *ngIf="!loans.length" class="card">No home loan applications yet.</div>
    <div class="card" *ngFor="let l of loans">
      <div class="row">
        <div><b>{{l.applicationId}}</b> <span class="badge" [attr.data-s]="l.status">{{l.status}}</span><br>
          ₹{{l.amount | number}} · {{l.tenure}} months · {{l.interestRate}}%</div>
        <div>
          <button class="btn danger sm" *ngIf="canCancel(l)" (click)="cancel(l)">Cancel Application</button>
          <button class="btn sm" (click)="toggle(l)">{{sel?.id===l.id?'Hide':'Details'}}</button>
        </div>
      </div>
      <div class="track" *ngIf="l.status!=='Rejected' && l.status!=='Cancelled'">
        <div *ngFor="let s of stages; let i=index" [class.done]="stageIdx(l)>=i" [class.cur]="stageIdx(l)===i"><i>{{stageIdx(l)>i ? '✔' : i+1}}</i><small>{{s}}</small></div>
      </div>
      <div class="msg err" *ngIf="l.status==='Cancelled'">This application was cancelled.</div>

      <div *ngIf="sel?.id===l.id">
        <div class="docs" *ngIf="!isLive(l) && l.status!=='Rejected'">
          <h4>Documents (upload PDF/image)</h4>
          <div class="doc" *ngFor="let d of docTypes">
            <span>{{d.label}}: <b>{{l[d.status]||'Pending'}}</b> <i *ngIf="l[d.remark]">— {{l[d.remark]}}</i></span>
            <input type="file" (change)="upload(l,d.key,$event)" [disabled]="l[d.status]==='Verified' || !['Documents Required','Documents Submitted','Under Review','Submitted'].includes(l.status)">
          </div>
          <small>Documents unlock once the bank moves your application to "Documents Required".</small>
        </div>
        <div *ngIf="l.status==='Rejected'" class="msg err">Rejected: {{l.rejectionReason}}</div>

        <div *ngIf="isLive(l)">
          <div class="stats">
            <div><small>Loan A/c</small><b>{{l.loanAccountNumber}}</b></div>
            <div><small>EMI</small><b>₹{{l.emi | number:'1.0-0'}}</b></div>
            <div><small>Remaining tenure</small><b>{{l.remainingTenure}} / {{l.tenure}}</b></div>
            <div><small>Outstanding</small><b>₹{{l.remainingPrincipal | number:'1.0-0'}}</b></div>
            <div><small>Next due</small><b>{{l.nextEmiDate}}</b></div>
            <div><small>Paid</small><b>{{paidPct(l)}}%</b></div>
          </div>
          <div class="bar"><div [style.width.%]="paidPct(l)"></div></div>
          <p>Disbursed ₹{{l.netDisbursed | number}} after charges ₹{{l.totalCharges | number:'1.0-0'}}</p>
          <div *ngIf="l.status==='Approved'" class="actions">
            <button class="btn" (click)="act(l,'pay-emi')">Pay EMI</button>
            <button class="btn" (click)="act(l,'pay-interest')">Pay Interest</button>
            <span><input type="number" placeholder="Prepay ₹" [(ngModel)]="prepayAmt" (ngModelChange)="previewPrepay(l)">
              <select [(ngModel)]="adjustment" (ngModelChange)="previewPrepay(l)"><option value="REDUCE_TENURE">Reduce tenure</option><option value="REDUCE_EMI">Reduce EMI</option></select>
              <button class="btn" (click)="prepay(l)">Prepay</button></span>
            <span><input type="number" placeholder="Renew months" [(ngModel)]="renewMonths">
              <button class="btn" (click)="renew(l)">Renew</button></span>
            <button class="btn danger" (click)="previewClose(l)">Close Loan</button>
          </div>
          <div class="ai" *ngIf="prepayPrev">Prepay ₹{{prepayPrev.prepayAmount}} + charge ₹{{prepayPrev.charge}} + GST ₹{{prepayPrev.gst}} = <b>₹{{prepayPrev.totalDebit}}</b>; tenure saved {{prepayPrev.tenureSaved}} months<span *ngIf="prepayPrev.newEmi"> · new EMI ₹{{prepayPrev.newEmi | number:'1.0-0'}}</span></div>
          <div class="ai" *ngIf="closePrev">Outstanding ₹{{closePrev.outstandingPrincipal}} + interest ₹{{closePrev.accruedInterest}} + charge ₹{{closePrev.closureCharge}} + GST ₹{{closePrev.gst}} = <b>₹{{closePrev.totalPayable}}</b>
            <button class="btn danger sm" (click)="act(l,'close')">Confirm Closure</button></div>
          <div class="actions">
            <button class="btn sm" (click)="loadSchedule(l)">EMI Schedule</button>
            <button class="btn sm" (click)="loadStatement(l)">Statement</button>
          </div>
        </div>

        <div *ngIf="payments.length && sel?.id===l.id">
          <h4>Payments (Funds Transfer / Cheque)</h4>
          <table><tr><th>Date</th><th>Type</th><th>Amount</th><th>EMI</th><th>Tenure</th><th>Receipt</th></tr>
          <tr *ngFor="let p of payments"><td>{{p.performedAt | date:'short'}}</td><td>{{p.loanPaymentType}}</td><td>{{p.amount | number:'1.2-2'}}</td><td>{{p.emiAmountBefore | number:'1.0-0'}} → {{p.emiAmountAfter | number:'1.0-0'}}</td><td>{{p.remainingTenureBefore}} → {{p.remainingTenureAfter}}</td><td><button class="btn sm" (click)="receipt(p)">Receipt</button></td></tr></table>
        </div>
        <div *ngIf="!isLive(l)"><button class="btn sm" (click)="loadStatement(l)">History</button></div>

        <table *ngIf="schedule.length && sel?.id===l.id">
          <tr><th>#</th><th>Due</th><th>EMI</th><th>Principal</th><th>Interest</th><th>Balance</th></tr>
          <tr *ngFor="let s of schedule"><td>{{s.installment}}</td><td>{{s.dueDate}}</td><td>{{s.emi|number:'1.0-0'}}</td><td>{{s.principal|number:'1.0-0'}}</td><td>{{s.interest|number:'1.0-0'}}</td><td>{{s.balance|number:'1.0-0'}}</td></tr>
        </table>
        <div *ngIf="statement && sel?.id===l.id">
          <h4>Statement <button class="btn sm" (click)="print()">Print</button></h4>
          <table>
            <tr><th>Date</th><th>Event</th><th>Details</th><th>Debit</th><th>Credit</th><th>Balance</th></tr>
            <tr *ngFor="let e of statement.entries"><td>{{e.eventDate | date:'short'}}</td><td>{{e.eventType}}</td><td>{{e.details}}</td><td>{{e.debit}}</td><td>{{e.credit}}</td><td>{{e.balanceAfter}}</td></tr>
          </table>
          <p>Total debited ₹{{statement.totalDebited}} · credited ₹{{statement.totalCredited}}</p>
        </div>
      </div>
    </div>
  </div>

  <div *ngIf="tab==='consolidated' && cons" class="card" id="hl-print">
    <h3>Consolidated Home Loan Statement <button class="btn sm" (click)="print()">Print</button></h3>
    <div class="stats">
      <div><small>Sanctioned</small><b>₹{{cons.totalSanctioned|number}}</b></div>
      <div><small>Outstanding</small><b>₹{{cons.totalOutstanding|number}}</b></div>
      <div><small>Principal paid</small><b>₹{{cons.totalPrincipalPaid|number}}</b></div>
      <div><small>Interest paid</small><b>₹{{cons.totalInterestPaid|number}}</b></div>
      <div><small>Total debited</small><b>₹{{cons.totalDebited|number}}</b></div>
    </div>
    <table>
      <tr><th>Date</th><th>Application</th><th>Event</th><th>Details</th><th>Debit</th><th>Credit</th></tr>
      <tr *ngFor="let e of cons.entries"><td>{{e.eventDate|date:'short'}}</td><td>{{e.applicationId}}</td><td>{{e.eventType}}</td><td>{{e.details}}</td><td>{{e.debit}}</td><td>{{e.credit}}</td></tr>
    </table>
  </div>
</div>`,
  styles: [`
.hl{padding:16px;max-width:1000px;margin:auto;font-family:'Segoe UI',system-ui,sans-serif;color:#0f172a}
.hero{display:flex;justify-content:space-between;align-items:center;gap:12px;background:linear-gradient(135deg,#1e3a8a,#2563eb 60%,#0ea5e9);color:#fff;padding:20px 24px;border-radius:14px;box-shadow:0 8px 24px rgba(37,99,235,.25)}
.hero h2{margin:0 0 4px}.hero p{margin:0;opacity:.9;font-size:14px}
.hero-badge{background:rgba(255,255,255,.18);padding:8px 14px;border-radius:20px;font-weight:600;font-size:13px;white-space:nowrap}
.card{background:#fff;border:1px solid #e3e8ef;border-radius:14px;padding:20px;margin:14px 0;box-shadow:0 2px 10px rgba(15,23,42,.05)}
.card h3{margin-top:0}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:16px}
label{display:flex;flex-direction:column;font-size:13px;font-weight:600;gap:4px}
.hint{font-weight:400;color:#64748b;font-size:11px}
input,select{padding:10px;border:1px solid #cbd5e1;border-radius:8px;font-size:14px}
input:focus,select:focus{outline:none;border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,.15)}
.btn{background:#1d4ed8;color:#fff;border:0;padding:10px 18px;border-radius:8px;cursor:pointer;margin:4px;font-weight:600;transition:.15s}
.btn:hover:not(:disabled){background:#1e40af;transform:translateY(-1px)}
.btn:disabled{opacity:.5;cursor:not-allowed}
.btn.sm{padding:5px 12px;font-size:13px}.btn.danger{background:#b91c1c}.btn.danger:hover:not(:disabled){background:#991b1b}
.btn.ghost{background:#f1f5f9;color:#334155}
.actions{display:flex;flex-wrap:wrap;align-items:center;gap:4px;margin-top:12px}.actions.end{justify-content:flex-end}
.tabs{display:flex;gap:6px;flex-wrap:wrap;margin:14px 0}
.tabs button{padding:10px 16px;border:1px solid #cbd5e1;background:#f8fafc;cursor:pointer;border-radius:8px;font-weight:600}
.tabs .on{background:#1d4ed8;color:#fff;border-color:#1d4ed8}
.steps{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin:8px 0}
.step{display:flex;gap:10px;align-items:center;background:#f8fafc;border:1px solid #e2e8f0;padding:10px 12px;border-radius:12px;opacity:.7}
.step span{width:30px;height:30px;border-radius:50%;background:#cbd5e1;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;flex:none}
.step div{display:flex;flex-direction:column}.step small{color:#64748b}
.step.active{opacity:1;border-color:#2563eb;background:#eff6ff}.step.active span{background:#2563eb}
.step.done{opacity:1}.step.done span{background:#16a34a}
.emi-box,.review{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px;margin:14px 0}
.emi-box div,.review div{background:#eff6ff;padding:10px 12px;border-radius:10px;display:flex;flex-direction:column}
.review div{background:#f8fafc;border:1px solid #e2e8f0}.review .wide{grid-column:1/-1}
.emi-box b{font-size:20px;color:#1d4ed8}
.terms{background:#fffbeb;border:1px solid #fde68a;border-radius:10px;padding:12px 16px;max-height:260px;overflow:auto}
.terms h4{margin:0 0 6px}.terms li{font-size:13px;margin:5px 0;line-height:1.45}
.check{flex-direction:row;align-items:center;gap:8px;margin-top:10px;font-size:14px}.check input{width:18px;height:18px}
.timeline{display:flex;flex-direction:column;gap:8px;margin:10px 0}
.timeline div{display:flex;align-items:center;gap:10px;font-size:14px}
.timeline span{width:24px;height:24px;border-radius:50%;background:#dbeafe;color:#1d4ed8;font-weight:700;display:flex;align-items:center;justify-content:center;font-size:12px;flex:none}
.track{display:flex;gap:4px;margin:12px 0 4px;overflow-x:auto}
.track div{flex:1;min-width:84px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:4px;border-top:4px solid #e2e8f0;padding-top:6px}
.track i{font-style:normal;width:22px;height:22px;border-radius:50%;background:#e2e8f0;font-size:11px;display:flex;align-items:center;justify-content:center}
.track small{font-size:11px;color:#64748b}
.track .done{border-color:#16a34a}.track .done i{background:#16a34a;color:#fff}
.track .cur{border-color:#2563eb}.track .cur i{background:#2563eb;color:#fff}.track .cur small{color:#1d4ed8;font-weight:600}
.row{display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap}
.badge{background:#e0e7ff;border-radius:10px;padding:2px 10px;font-size:12px;font-weight:600}
.badge[data-s=Approved]{background:#dcfce7;color:#166534}.badge[data-s=Rejected],.badge[data-s=Cancelled]{background:#fee2e2;color:#991b1b}
.stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:8px;margin:10px 0}
.stats div{background:#f1f5f9;padding:8px;border-radius:8px;display:flex;flex-direction:column}
.bar{background:#e2e8f0;height:10px;border-radius:5px}.bar div{background:#16a34a;height:10px;border-radius:5px}
.ai{background:#ecfeff;border:1px solid #67e8f9;padding:10px;border-radius:8px;margin:8px 0}
.msg{padding:10px 12px;border-radius:8px;margin:8px 0}.ok{background:#dcfce7}.err{background:#fee2e2}
.doc{display:flex;justify-content:space-between;gap:8px;padding:6px 0;border-bottom:1px dashed #e2e8f0}
table{width:100%;border-collapse:collapse;font-size:13px;margin-top:8px}th,td{border:1px solid #e2e8f0;padding:4px 6px;text-align:left}
th{background:#f8fafc}
@media(max-width:640px){.steps{grid-template-columns:1fr}.hero{flex-direction:column;align-items:flex-start}}
`]
})
export class Homeloan implements OnInit, OnDestroy {
  private api = `${environment.apiBaseUrl}/api/home-loans`;
  tab = 'apply';
  user: any = {};
  form: any = { amount: 1000000, tenure: 120, interestRate: 8.5, propertyValue: 1500000, purpose: '', propertyAddress: '' };
  emiPreview = 0; analysis: any = null; loans: any[] = []; sel: any = null;
  schedule: any[] = []; statement: any = null; cons: any = null;
  prepayAmt: number | null = null; renewMonths: number | null = null; prepayPrev: any = null; closePrev: any = null;
  msg = ''; err = ''; busy = false;
  docTypes = [
    { key: 'fdReceipt', label: 'FD Receipt', status: 'fdReceiptStatus', remark: 'fdReceiptRemark' },
    { key: 'model', label: 'Home Loan Model', status: 'modelDocStatus', remark: 'modelDocRemark' },
    { key: 'signature', label: 'Signature', status: 'signatureStatus', remark: 'signatureRemark' }
  ];
  private t: any;
  private poll: any;
  adjustment = 'REDUCE_TENURE';
  step = 1; agreed = false;
  stages = ['Submitted', 'Under Review', 'Documents', 'Verified', 'Approved'];
  stageIdx(l: any) {
    switch (l.status) {
      case 'Submitted': return 0; case 'Under Review': return 1;
      case 'Documents Required': case 'Documents Submitted': return 2;
      case 'Documents Verified': return 3; case 'Approved': case 'Closed': return 4; default: return 0;
    }
  }
  canCancel(l: any) { return !['Approved', 'Closed', 'Rejected', 'Cancelled'].includes(l.status); }
  formError(): string {
    const f = this.form;
    if (!(f.amount >= 100000)) return 'Loan amount must be at least ₹1,00,000';
    if (!(f.tenure >= 12 && f.tenure <= 360)) return 'Tenure must be between 12 and 360 months';
    if (!(f.interestRate > 0)) return 'Enter a valid interest rate';
    if (f.propertyValue > 0 && f.amount > f.propertyValue * 0.9) return 'Loan amount cannot exceed 90% of the property value';
    return '';
  }
  next() { if (!this.formError()) this.step = 2; }
  cancel(l: any) {
    if (!confirm('Cancel application ' + l.applicationId + '? This cannot be undone.')) return;
    this.http.put(`${this.api}/${l.id}/cancel?accountNumber=${encodeURIComponent(this.user.accountNumber)}&by=${encodeURIComponent(this.user.name || 'Customer')}`, {})
      .subscribe({ next: () => { this.flash('Application cancelled'); this.load(); }, error: e => this.fail(e) });
  }
  payments: any[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit() {
    try { this.user = JSON.parse(sessionStorage.getItem('currentUser') || '{}'); } catch { this.user = {}; }
    this.calc(); this.load();
    this.poll = setInterval(() => { this.load(); if (this.sel) this.loadPayments(this.sel); }, 8000);
  }
  ngOnDestroy() { clearInterval(this.poll); clearTimeout(this.t); }
  loadPayments(l: any) { this.http.get<any[]>(`${this.api}/${l.id}/payments`).subscribe({ next: p => this.payments = p, error: () => {} }); }
  receipt(p: any) { printFundTransferReceipt(p); }
  private flash(ok: string, bad = '') { this.msg = ok; this.err = bad; setTimeout(() => { this.msg = ''; this.err = ''; }, 5000); }
  private fail(e: any) { this.flash('', e?.error?.message || 'Request failed'); this.busy = false; }
  isLive(l: any) { return l.status === 'Approved' || l.status === 'Closed'; }
  paidPct(l: any) { return l.amount ? Math.round(((l.principalPaid || 0) + (l.prepaidAmount || 0)) / l.amount * 1000) / 10 : 0; }

  calc() {
    const { amount: p, tenure: n, interestRate: r } = this.form;
    const m = r / 1200;
    this.emiPreview = p > 0 && n > 0 ? (m === 0 ? p / n : p * m * Math.pow(1 + m, n) / (Math.pow(1 + m, n) - 1)) : 0;
    clearTimeout(this.t);
    this.t = setTimeout(() => {
      if (!this.user.accountNumber) return;
      this.http.post(`${this.api}/analyze`, { accountNumber: this.user.accountNumber, ...this.form }).subscribe({ next: (a) => this.analysis = a, error: () => {} });
    }, 400);
  }
  load() {
    if (!this.user.accountNumber) return;
    this.http.get<any[]>(`${this.api}/account/${this.user.accountNumber}`).subscribe({ next: r => { this.loans = r; if (this.sel) this.sel = r.find(x => x.id === this.sel.id) || null; }, error: e => this.fail(e) });
  }
  submit() {
    this.busy = true;
    const body = { ...this.form, accountNumber: this.user.accountNumber, userName: this.user.name, userEmail: this.user.email };
    this.http.post<any>(this.api, body).subscribe({
      next: r => { this.busy = false; this.flash(`Application submitted. Application ID: ${r.applicationId}`); this.tab = 'mine'; this.step = 1; this.agreed = false; this.load(); },
      error: e => this.fail(e)
    });
  }
  toggle(l: any) { this.sel = this.sel?.id === l.id ? null : l; this.payments = []; if (this.sel) this.loadPayments(l); this.schedule = []; this.statement = null; this.prepayPrev = null; this.closePrev = null; }
  upload(l: any, type: string, ev: any) {
    const f = ev.target.files?.[0]; if (!f) return;
    const fd = new FormData(); fd.append('file', f);
    this.http.post(`${this.api}/${l.id}/documents/${type}`, fd).subscribe({ next: () => { this.flash('Document uploaded'); this.load(); }, error: e => this.fail(e) });
  }
  act(l: any, what: string) {
    const q = `by=${encodeURIComponent(this.user.name || 'Customer')}`;
    this.http.post<any>(`${this.api}/${l.id}/${what}?${q}`, {}).subscribe({ next: () => { this.flash('Done'); this.closePrev = null; this.load(); }, error: e => this.fail(e) });
  }
  previewPrepay(l: any) {
    this.prepayPrev = null;
    if (this.prepayAmt && this.prepayAmt > 0) this.http.get(`${this.api}/${l.id}/prepay-preview?amount=${this.prepayAmt}&adjustment=${this.adjustment}`).subscribe({ next: p => this.prepayPrev = p, error: () => {} });
  }
  prepay(l: any) {
    this.http.post(`${this.api}/${l.id}/prepay?amount=${this.prepayAmt}&adjustment=${this.adjustment}&by=${encodeURIComponent(this.user.name || 'Customer')}`, {}).subscribe({ next: () => { this.flash('Prepayment successful'); this.prepayAmt = null; this.prepayPrev = null; this.load(); }, error: e => this.fail(e) });
  }
  renew(l: any) {
    this.http.post(`${this.api}/${l.id}/renew?months=${this.renewMonths}&by=${encodeURIComponent(this.user.name || 'Customer')}`, {}).subscribe({ next: () => { this.flash('Loan renewed'); this.renewMonths = null; this.load(); }, error: e => this.fail(e) });
  }
  previewClose(l: any) { this.http.get(`${this.api}/${l.id}/closure-preview`).subscribe({ next: p => this.closePrev = p, error: e => this.fail(e) }); }
  loadSchedule(l: any) { this.statement = null; this.http.get<any[]>(`${this.api}/${l.id}/schedule`).subscribe({ next: s => this.schedule = s, error: e => this.fail(e) }); }
  loadStatement(l: any) { this.schedule = []; this.http.get(`${this.api}/${l.id}/statement`).subscribe({ next: s => this.statement = s, error: e => this.fail(e) }); }
  openConsolidated() {
    this.tab = 'consolidated';
    this.http.get(`${this.api}/account/${this.user.accountNumber}/consolidated-statement`).subscribe({ next: s => this.cons = s, error: e => this.fail(e) });
  }
  print() { window.print(); }
}