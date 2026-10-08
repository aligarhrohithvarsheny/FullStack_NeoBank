import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { printHomeLoanDocuments, printHomeLoanNoc } from '../../../service/home-loan-documents';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-admin-home-loans',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="ah">
  <header class="hero">
    <div><h2>🏠 Home Loan Dashboard</h2><p>Review applications, verify documents, top-up, close loans and issue NOC</p></div>
    <input class="search" placeholder="🔍 Search application ID / account / name" [(ngModel)]="q">
  </header>
  <div class="msg ok" *ngIf="msg && !sel">{{msg}}</div>
  <div class="msg err" *ngIf="err && !sel">{{err}}</div>

  <div class="stats">
    <button *ngFor="let s of stats()" class="stat" [class.on]="filter===s.key" (click)="filter=s.key">
      <span class="sv">{{s.value}}</span><span class="sl">{{s.label}}</span>
    </button>
  </div>

  <div class="tw card">
    <table class="lt">
      <thead><tr><th>Application</th><th>Applicant</th><th>Amount</th><th>Tenure</th><th>Rate</th><th>Status</th><th>AI score</th><th></th></tr></thead>
      <tbody>
        <tr *ngFor="let l of filtered()">
          <td><b>{{l.applicationId}}</b><small>{{l.applicationDate | date:'mediumDate'}}</small></td>
          <td>{{l.userName}}<small>A/c {{l.accountNumber}}</small></td>
          <td>₹{{l.amount|number}}</td><td>{{l.tenure}} m</td><td>{{l.interestRate}}%</td>
          <td><span class="badge" [attr.data-s]="l.status">{{l.status}}</span></td>
          <td>{{l.approvalProbability ? l.approvalProbability+'% · '+l.riskBand : '-'}}</td>
          <td class="ra">
            <button class="b p" (click)="open(l)">Open</button>
            <button class="b" *ngIf="canAdvance(l)" (click)="advance(l)">Next ▶</button>
          </td>
        </tr>
        <tr *ngIf="!filtered().length"><td colspan="8" class="muted c">No applications found</td></tr>
      </tbody>
    </table>
  </div>

  <div class="modal" *ngIf="sel" (click)="sel=null">
   <div class="panel" (click)="$event.stopPropagation()">
    <div class="phead">
      <div><h3>{{sel.applicationId}} <span class="badge" [attr.data-s]="sel.status">{{sel.status}}</span></h3>
        <small>{{sel.userName}} · A/c {{sel.accountNumber}} <span *ngIf="sel.loanAccountNumber">· Loan {{sel.loanAccountNumber}}</span></small></div>
      <button class="b" (click)="sel=null">✕ Close</button>
    </div>
    <div class="msg ok" *ngIf="msg">{{msg}}</div>
    <div class="msg err" *ngIf="err">{{err}}</div>

    <nav class="tabs">
      <button *ngFor="let t of tabList()" [class.on]="tab===t.key" (click)="setTab(t.key)">{{t.label}}</button>
    </nav>

    <!-- DETAILS -->
    <div *ngIf="tab==='details'">
      <section class="sec">
        <h4>Application details</h4>
        <div class="grid">
          <label>Amount (₹)<input type="number" [(ngModel)]="edit.amount" [disabled]="live(sel)"></label>
          <label>Tenure (months)<input type="number" [(ngModel)]="edit.tenure" [disabled]="live(sel)"></label>
          <label>Interest % p.a.<input type="number" step="0.05" [(ngModel)]="edit.interestRate" [disabled]="sel.status==='Closed'||sel.status==='Rejected'||sel.status==='Cancelled'"></label>
          <label>Property value (₹)<input type="number" [(ngModel)]="edit.propertyValue"></label>
          <label>Purpose<input [(ngModel)]="edit.purpose"></label>
          <label>Property address<input [(ngModel)]="edit.propertyAddress"></label>
          <label class="wide">Admin notes<input [(ngModel)]="edit.adminNotes"></label>
        </div>
        <div class="acts" *ngIf="editable(sel)"><button class="b p" (click)="saveEdit()" [disabled]="saving">{{saving?'Saving...':'💾 Save changes'}}</button></div>
        <p class="muted" *ngIf="!editable(sel)">This loan is {{sel.status}} and can no longer be edited.</p>
      </section>

      <section class="sec">
        <h4>🤖 Real-time AI analysis</h4>
        <div *ngIf="!ai" class="muted">Loading analysis...</div>
        <div *ngIf="ai">
          <div class="kpis">
            <div><small>Approval probability</small><b>{{ai.approvalProbability}}%</b></div>
            <div><small>Risk</small><b>{{ai.riskBand}}</b></div>
            <div><small>CIBIL</small><b>{{ai.cibilScore}}</b></div>
            <div><small>FOIR</small><b>{{ai.foirPercent}}%</b></div>
            <div><small>PAN</small><b>{{ai.pan||'N/A'}}</b></div>
            <div><small>Avg paid</small><b>{{ai.avgPaidPercentage}}%</b></div>
          </div>
          <div class="tw"><table class="lt"><tr><th>Existing loan</th><th>Reference</th><th>Amount</th><th>Status</th><th>Paid %</th><th>EMI</th></tr>
            <tr *ngFor="let e of ai.existingLoans"><td>{{e.type}}</td><td>{{e.reference}}</td><td>{{e.amount|number}}</td><td>{{e.status}}</td>
              <td><div class="bar"><div [style.width.%]="e.paidPercentage"></div></div>{{e.paidPercentage}}%</td><td>{{e.emi|number:'1.0-0'}}</td></tr>
          </table></div>
          <ul><li *ngFor="let f of ai.factors">{{f}}</li></ul>
          <p *ngIf="ai.recommendations?.length"><b>Recommendations:</b> {{ai.recommendations.join(' · ')}}</p>
          <small class="muted">{{ai.model}} · {{ai.analyzedAt | date:'medium'}}</small>
        </div>
      </section>

      <section class="sec">
        <h4>💰 Charges &amp; deductions</h4>
        <div *ngIf="charges" class="kpis">
          <div><small>Processing fee</small><b>₹{{charges.processingFee|number:'1.0-0'}}</b></div>
          <div><small>GST on fee</small><b>₹{{charges.gstOnFee|number:'1.0-0'}}</b></div>
          <div><small>Legal</small><b>₹{{charges.legalCharges|number:'1.0-0'}}</b></div>
          <div><small>Total deducted</small><b>₹{{charges.totalCharges|number:'1.0-0'}}</b></div>
          <div><small>Net disbursed</small><b>₹{{charges.netDisbursed|number:'1.0-0'}}</b></div>
        </div>
        <div class="acts">
          <button class="b" *ngIf="canAdvance(sel)" (click)="advance(sel)">Move to next process ▶</button>
          <button class="b p" *ngIf="sel.status==='Documents Verified'" (click)="approve()">✔ Approve &amp; Disburse</button>
          <button class="b d" *ngIf="!live(sel) && sel.status!=='Rejected' && sel.status!=='Cancelled'" (click)="reject()">Reject</button>
        </div>
      </section>
    </div>

    <!-- DOCUMENTS -->
    <div *ngIf="tab==='docs'">
      <section class="sec">
        <h4>📄 Documents</h4>
        <div class="doc" *ngFor="let d of docTypes">
          <div class="dinfo"><b>{{d.label}}</b>
            <span class="dstat" [attr.data-s]="sel[d.path] ? (sel[d.status]||'Pending') : 'None'">{{sel[d.path] ? (sel[d.status]||'Pending') : 'Not uploaded'}}</span>
            <i *ngIf="sel[d.remark]">{{sel[d.remark]}}</i></div>
          <div class="dact">
            <ng-container *ngIf="sel[d.path]">
              <a class="b" [href]="fileUrl(d.key)" target="_blank" rel="noopener">👁 View</a>
              <button class="b p" (click)="verify(d.key,'Verified')" [disabled]="sel[d.status]==='Verified'">✔ Verify</button>
              <button class="b d" (click)="reupload(d.key)">↻ Re-upload</button>
            </ng-container>
            <label class="b up">{{uploading===d.key ? 'Uploading...' : (sel[d.path] ? '⬆ Replace' : '⬆ Upload')}}
              <input type="file" hidden accept=".pdf,.png,.jpg,.jpeg" [disabled]="!!uploading" (change)="upload(d.key,$event)"></label>
          </div>
        </div>
        <p class="muted">Files uploaded by admin are marked as verified automatically. Allowed: PDF, PNG, JPG (max 10 MB).</p>
      </section>
    </div>

    <!-- LOAN ACCOUNT -->
    <div *ngIf="tab==='account' && live(sel)">
      <section class="sec">
        <h4>🏦 Loan account {{sel.loanAccountNumber}}</h4>
        <div class="kpis">
          <div><small>Sanctioned</small><b>₹{{sel.amount|number:'1.0-0'}}</b></div>
          <div><small>EMI</small><b>₹{{sel.emi|number:'1.0-0'}}</b></div>
          <div><small>Rate</small><b>{{sel.interestRate}}%</b></div>
          <div><small>Outstanding</small><b>₹{{sel.remainingPrincipal|number:'1.0-0'}}</b></div>
          <div><small>Remaining tenure</small><b>{{sel.remainingTenure}} m</b></div>
          <div><small>EMIs paid</small><b>{{sel.paidEmis}}</b></div>
          <div><small>Interest paid</small><b>₹{{sel.interestPaid|number:'1.0-0'}}</b></div>
          <div><small>Top-ups</small><b>₹{{sel.topupTotal||0|number:'1.0-0'}}</b></div>
        </div>
        <div class="acts"><button class="b" (click)="docs(sel)">📄 3-page loan documents</button></div>
      </section>

      <section class="sec" *ngIf="sel.status==='Approved'">
        <h4>➕ Top-up loan (release amount)</h4>
        <div class="grid">
          <label>Top-up amount (₹)<input type="number" [(ngModel)]="tu.amount"></label>
          <label>Top-up interest % p.a.<input type="number" step="0.05" [(ngModel)]="tu.rate"></label>
          <label>Extra tenure (months)<input type="number" [(ngModel)]="tu.extraMonths"></label>
          <label class="wide">Note<input [(ngModel)]="tu.note"></label>
        </div>
        <div class="prev" *ngIf="tuPreview() as p">
          Blended rate <b>{{p.rate}}%</b> · New outstanding <b>₹{{p.principal|number:'1.0-0'}}</b> · New EMI <b>₹{{p.emi|number:'1.0-0'}}</b>
          <small>(current EMI ₹{{sel.emi|number:'1.0-0'}})</small>
        </div>
        <div class="acts"><button class="b p" (click)="topup()" [disabled]="busy">Release top-up to customer account</button></div>
        <p class="muted">The amount is credited to the customer's savings account instantly; EMI and tenure update in real time.</p>
      </section>

      <section class="sec">
        <h4>📈 Interest rate history</h4>
        <div *ngIf="!rates.length" class="muted">No interest rate changes recorded.</div>
        <ul class="tl"><li *ngFor="let r of rates"><b>{{r.type==='RATE_REVERTED'?'↩ Reverted':'✎ Changed'}}</b> {{r.details}}
          <small>{{r.date|date:'short'}} · {{r.actor}}</small></li></ul>
        <div class="acts" *ngIf="sel.status==='Approved' && canRevert()"><button class="b d" (click)="revertRate()" [disabled]="busy">↩ Revert last interest change</button></div>
      </section>

      <section class="sec" *ngIf="sel.status==='Approved'">
        <h4>🔒 Close loan</h4>
        <div class="kpis" *ngIf="closure">
          <div><small>Outstanding principal</small><b>₹{{closure.outstandingPrincipal|number:'1.0-0'}}</b></div>
          <div><small>Accrued interest</small><b>₹{{closure.accruedInterest|number:'1.0-0'}}</b></div>
          <div><small>Closure charge</small><b>₹{{closure.closureCharge|number:'1.0-0'}}</b></div>
          <div><small>GST</small><b>₹{{closure.gst|number:'1.0-0'}}</b></div>
          <div><small>Total payable</small><b>₹{{closure.totalPayable|number:'1.0-0'}}</b></div>
        </div>
        <div class="grid"><label class="wide">Settlement reference (cheque / receipt no.)<input [(ngModel)]="closeRef"></label></div>
        <div class="acts"><button class="b d" (click)="closeLoan()" [disabled]="busy">Close loan (settled at bank)</button></div>
      </section>
    </div>

    <section class="sec" *ngIf="tab==='account' && sel.status==='Closed'">
      <h4>📜 Loan closed</h4>
      <div class="kpis">
        <div><small>Closed on</small><b>{{sel.closureDate|date:'mediumDate'}}</b></div>
        <div><small>Closure amount</small><b>₹{{sel.closureAmount|number:'1.0-0'}}</b></div>
        <div><small>NOC</small><b>{{sel.nocNumber||'Not issued'}}</b></div>
      </div>
      <div class="acts">
        <button class="b p" (click)="noc()">📜 {{sel.nocNumber ? 'Print NOC' : 'Generate NOC'}}</button>
        <button class="b" (click)="docs(sel)">📄 Loan documents</button>
      </div>
    </section>

    <!-- HISTORY -->
    <div *ngIf="tab==='history'">
      <section class="sec">
        <h4>🕑 History</h4>
        <div class="tw" *ngIf="events"><table class="lt">
          <tr><th>Date</th><th>Event</th><th>By</th><th>Details</th><th>Debit</th><th>Credit</th></tr>
          <tr *ngFor="let e of events"><td>{{e.eventDate|date:'short'}}</td><td><span class="badge">{{e.eventType}}</span></td><td>{{e.actor}}</td><td>{{e.details}}</td><td>{{e.debit}}</td><td>{{e.credit}}</td></tr>
        </table></div>
      </section>
    </div>
   </div>
  </div>
</div>`,
  styles: [`
.ah{padding:18px;font-family:'Segoe UI',system-ui,sans-serif;color:#0f172a;background:#f1f5f9;min-height:100%}
.hero{display:flex;justify-content:space-between;align-items:center;gap:14px;flex-wrap:wrap;background:linear-gradient(135deg,#1e3a8a,#2563eb);color:#fff;border-radius:14px;padding:18px 22px;margin-bottom:14px}
.hero h2{margin:0}.hero p{margin:4px 0 0;opacity:.85;font-size:13px}
.search{padding:10px 12px;width:100%;max-width:340px;border:0;border-radius:8px;font-size:14px;box-sizing:border-box}
.stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:10px;margin-bottom:14px}
.stat{background:#fff;border:2px solid transparent;border-radius:12px;padding:12px;display:flex;flex-direction:column;align-items:center;cursor:pointer;box-shadow:0 1px 3px rgba(0,0,0,.08)}
.stat.on{border-color:#2563eb;background:#eff6ff}.sv{font-size:24px;font-weight:800;color:#1e3a8a}.sl{font-size:12px;color:#64748b}
.card{background:#fff;border-radius:12px;box-shadow:0 1px 3px rgba(0,0,0,.08);padding:6px}
.tw{overflow-x:auto}
.lt{width:100%;border-collapse:collapse;font-size:13px}
.lt th{background:#f8fafc;color:#475569;font-weight:600;text-align:left;padding:10px;border-bottom:2px solid #e2e8f0;white-space:nowrap}
.lt td{padding:10px;border-bottom:1px solid #eef2f7;vertical-align:middle}
.lt td small{display:block;color:#94a3b8;font-size:11px}.lt tr:hover td{background:#f8fafc}
.ra{white-space:nowrap}.c{text-align:center}
.kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:8px;margin:8px 0}
.kpis div{background:#f1f5f9;padding:8px 10px;border-radius:8px;display:flex;flex-direction:column}
.kpis small{color:#64748b;font-size:11px}.kpis b{font-size:15px}
.b{background:#e2e8f0;border:0;padding:7px 13px;border-radius:7px;cursor:pointer;margin:2px;text-decoration:none;color:#0f172a;display:inline-block;font-size:13px;font-weight:600}
.b:disabled{opacity:.5;cursor:not-allowed}.b.p{background:#1d4ed8;color:#fff}.b.d{background:#b91c1c;color:#fff}.b.up{background:#0f766e;color:#fff}
.badge{background:#e0e7ff;border-radius:10px;padding:2px 10px;font-size:12px;font-weight:600;white-space:nowrap}
.badge[data-s=Approved]{background:#dcfce7;color:#166534}.badge[data-s=Rejected],.badge[data-s=Cancelled]{background:#fee2e2;color:#991b1b}
.badge[data-s=Closed]{background:#e2e8f0;color:#334155}.badge[data-s='Documents Verified']{background:#cffafe;color:#155e75}
.modal{position:fixed;inset:0;background:rgba(15,23,42,.55);display:flex;align-items:flex-start;justify-content:center;overflow:auto;z-index:2000;padding:20px}
.panel{background:#f8fafc;border-radius:14px;padding:18px;width:min(1000px,100%);box-sizing:border-box;box-shadow:0 20px 50px rgba(0,0,0,.3)}
.phead{display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:10px}
.phead h3{margin:0;display:flex;align-items:center;gap:10px;flex-wrap:wrap}.phead small{color:#64748b}
.tabs{display:flex;gap:6px;flex-wrap:wrap;border-bottom:2px solid #e2e8f0;margin-bottom:6px}
.tabs button{background:none;border:0;padding:9px 16px;font-weight:600;color:#64748b;cursor:pointer;border-bottom:3px solid transparent;margin-bottom:-2px}
.tabs button.on{color:#1d4ed8;border-bottom-color:#1d4ed8}
.sec{background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:14px 16px;margin:12px 0}
.sec h4{margin:0 0 10px;font-size:15px}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:12px}
.grid .wide{grid-column:1/-1}
.ah label{display:flex;flex-direction:column;font-size:12px;font-weight:600;gap:4px;color:#334155}
.ah label.b{flex-direction:row}
.ah input:not([hidden]){padding:8px;border:1px solid #cbd5e1;border-radius:6px;font-size:14px;box-sizing:border-box;width:100%}
.ah input:disabled{background:#f1f5f9}
.acts{margin-top:10px;display:flex;flex-wrap:wrap;gap:4px}
.doc{display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;padding:10px 0;border-bottom:1px dashed #e2e8f0}
.doc:last-of-type{border-bottom:0}
.dinfo{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.dinfo i{color:#64748b;font-size:12px}
.dstat{padding:2px 9px;border-radius:10px;font-size:12px;background:#fef3c7;color:#92400e}
.dstat[data-s=None]{background:#f1f5f9;color:#64748b}.dstat[data-s=Verified]{background:#dcfce7;color:#166534}.dstat[data-s='Reupload Required']{background:#fee2e2;color:#991b1b}
.bar{background:#e2e8f0;height:8px;border-radius:4px;min-width:60px}.bar div{background:#16a34a;height:8px;border-radius:4px}
.prev{background:#eff6ff;border:1px dashed #93c5fd;border-radius:8px;padding:10px 12px;margin-top:10px;font-size:13px}
.tl{list-style:none;padding:0;margin:0}.tl li{padding:8px 0 8px 14px;border-left:3px solid #93c5fd;margin-left:4px;font-size:13px}.tl small{display:block;color:#94a3b8}
.msg{padding:8px 12px;border-radius:6px;margin:8px 0}.ok{background:#dcfce7}.err{background:#fee2e2}
.muted{color:#64748b;font-size:13px}
`]
})
export class AdminHomeLoans implements OnInit, OnDestroy {
  private api = `${environment.apiBaseUrl}/api/home-loans`;
  loans: any[] = []; q = ''; filter = 'all'; sel: any = null; edit: any = {}; ai: any = null; charges: any = null; events: any[] | null = null;
  tab = 'details'; rates: any[] = []; closure: any = null; closeRef = '';
  tu: any = { amount: null, rate: null, extraMonths: 0, note: '' };
  msg = ''; err = ''; saving = false; busy = false; uploading = '';
  docTypes = [
    { key: 'fdReceipt', label: 'FD Receipt', status: 'fdReceiptStatus', remark: 'fdReceiptRemark', path: 'fdReceiptPath' },
    { key: 'model', label: 'Home Loan Model', status: 'modelDocStatus', remark: 'modelDocRemark', path: 'modelDocPath' },
    { key: 'signature', label: 'Signature', status: 'signatureStatus', remark: 'signatureRemark', path: 'signaturePath' }
  ];
  private pending = ['Submitted', 'Under Review', 'Documents Required', 'Documents Submitted', 'Documents Verified'];
  constructor(private http: HttpClient) {}
  private poll: any;
  ngOnInit() { this.load(); this.poll = setInterval(() => this.load(), 8000); }
  ngOnDestroy() { clearInterval(this.poll); }

  get admin() {
    try { const a = JSON.parse(sessionStorage.getItem('admin') || sessionStorage.getItem('currentAdmin') || '{}'); return a.name || a.email || 'Admin'; } catch { return 'Admin'; }
  }
  private adm() { return encodeURIComponent(this.admin); }
  private flash(ok: string, bad = '') { this.msg = ok; this.err = bad; setTimeout(() => { this.msg = ''; this.err = ''; }, 5000); }
  private fail(e: any) { this.busy = false; this.flash('', e?.error?.message || 'Request failed'); }
  live(l: any) { return l.status === 'Approved' || l.status === 'Closed'; }
  editable(l: any) { return !['Closed', 'Rejected', 'Cancelled'].includes(l.status); }
  count(s: string) { return this.loans.filter(l => l.status === s).length; }
  stats() {
    return [
      { key: 'all', label: 'All', value: this.loans.length },
      { key: 'pending', label: 'In process', value: this.loans.filter(l => this.pending.includes(l.status)).length },
      { key: 'Approved', label: 'Approved', value: this.count('Approved') },
      { key: 'Closed', label: 'Closed', value: this.count('Closed') },
      { key: 'Rejected', label: 'Rejected', value: this.count('Rejected') },
      { key: 'Cancelled', label: 'Cancelled', value: this.count('Cancelled') }
    ];
  }
  filtered() {
    const q = this.q.toLowerCase();
    return this.loans.filter(l =>
      (this.filter === 'all' || (this.filter === 'pending' ? this.pending.includes(l.status) : l.status === this.filter)) &&
      (!q || [l.applicationId, l.accountNumber, l.userName].some(v => (v || '').toLowerCase().includes(q))));
  }
  canAdvance(l: any) { return ['Submitted', 'Under Review', 'Documents Submitted'].includes(l.status); }
  tabList() {
    const t = [{ key: 'details', label: 'Details' }, { key: 'docs', label: 'Documents' }];
    if (this.live(this.sel)) t.push({ key: 'account', label: this.sel.status === 'Closed' ? 'Closure & NOC' : 'Loan account' });
    t.push({ key: 'history', label: 'History' });
    return t;
  }
  setTab(k: string) {
    this.tab = k;
    if (k === 'account') { this.loadRates(); if (this.sel.status === 'Approved') this.loadClosure(); }
  }

  load() {
    this.http.get<any[]>(this.api).subscribe({ next: r => { this.loans = r; if (this.sel) { const f = r.find(x => x.id === this.sel.id); if (f) { this.sel = f; this.refreshEvents(); } } }, error: e => this.fail(e) });
  }
  private refreshEvents() { if (this.sel) this.http.get<any>(`${this.api}/${this.sel.id}/statement`).subscribe({ next: s => this.events = s.entries, error: () => {} }); }
  private fillEdit(l: any) {
    this.edit = { amount: l.amount, tenure: l.tenure, interestRate: l.interestRate, propertyValue: l.propertyValue, purpose: l.purpose, propertyAddress: l.propertyAddress, adminNotes: l.adminNotes };
  }
  private applyUpdate(r: any, text: string) {
    this.sel = r; this.fillEdit(r); this.flash(text); this.load(); this.refreshEvents();
    if (this.tab === 'account') { this.loadRates(); if (r.status === 'Approved') this.loadClosure(); }
    this.http.get(`${this.api}/${r.id}/analysis`).subscribe({ next: a => this.ai = a, error: () => {} });
  }
  open(l: any) {
    this.ai = null; this.events = null; this.tab = 'details'; this.rates = []; this.closure = null; this.closeRef = '';
    this.tu = { amount: null, rate: l.interestRate, extraMonths: 0, note: '' };
    this.sel = l; this.fillEdit(l); this.msg = ''; this.err = '';
    this.http.get(`${this.api}/charges-preview?amount=${l.amount}`).subscribe({ next: c => this.charges = this.live(l) ? { processingFee: l.processingFee, gstOnFee: l.gstOnFee, legalCharges: l.legalCharges, totalCharges: l.totalCharges, netDisbursed: l.netDisbursed } : c });
    this.refreshEvents();
    this.http.get(`${this.api}/${l.id}/analysis`).subscribe({ next: a => this.ai = a, error: () => {} });
  }
  fileUrl(type: string) { return `${this.api}/${this.sel.id}/documents/${type}/file`; }
  docs(l: any) { this.http.get<any[]>(`${this.api}/${l.id}/schedule`).subscribe({ next: s => printHomeLoanDocuments(l, s), error: () => printHomeLoanDocuments(l, []) }); }
  advance(l: any) { this.http.put(`${this.api}/${l.id}/advance?admin=${this.adm()}`, {}).subscribe({ next: () => { this.flash('Moved to next process'); this.load(); }, error: e => this.fail(e) }); }
  saveEdit() {
    const body: any = {};
    for (const k of Object.keys(this.edit)) { const v = this.edit[k]; if (v !== null && v !== undefined && v !== '') body[k] = v; }
    this.saving = true;
    this.http.put<any>(`${this.api}/${this.sel.id}/admin-edit?admin=${this.adm()}`, body).subscribe({
      next: r => { this.saving = false; this.applyUpdate(r, 'Details saved'); },
      error: e => { this.saving = false; this.fail(e); }
    });
  }
  verify(type: string, decision: string, remark = '') {
    this.http.put<any>(`${this.api}/${this.sel.id}/documents/${type}/verify?decision=${encodeURIComponent(decision)}&remark=${encodeURIComponent(remark)}&admin=${this.adm()}`, {}).subscribe({ next: (r: any) => this.applyUpdate(r, `Document ${decision}`), error: e => this.fail(e) });
  }
  reupload(type: string) { const r = prompt('Reason for re-upload?'); if (r !== null) this.verify(type, 'Reupload Required', r); }
  upload(type: string, ev: Event) {
    const input = ev.target as HTMLInputElement; const f = input.files?.[0]; if (!f) return;
    if (f.size > 10 * 1024 * 1024) { this.flash('', 'File must be under 10 MB'); input.value = ''; return; }
    const fd = new FormData(); fd.append('file', f);
    this.uploading = type;
    this.http.post<any>(`${this.api}/${this.sel.id}/admin-documents/${type}?admin=${this.adm()}`, fd).subscribe({
      next: r => { this.uploading = ''; input.value = ''; this.applyUpdate(r, 'Document uploaded and verified'); },
      error: e => { this.uploading = ''; input.value = ''; this.fail(e); }
    });
  }
  approve() { if (confirm('Approve and disburse this loan?')) this.http.put<any>(`${this.api}/${this.sel.id}/approve?admin=${this.adm()}`, {}).subscribe({ next: r => this.applyUpdate(r, 'Loan approved and disbursed'), error: e => this.fail(e) }); }
  reject() { const r = prompt('Rejection reason?'); if (r !== null) this.http.put<any>(`${this.api}/${this.sel.id}/reject?admin=${this.adm()}&reason=${encodeURIComponent(r)}`, {}).subscribe({ next: x => this.applyUpdate(x, 'Rejected'), error: e => this.fail(e) }); }

  // ----- loan account -----
  private loadRates() { this.http.get<any[]>(`${this.api}/${this.sel.id}/rate-history`).subscribe({ next: r => this.rates = r, error: () => {} }); }
  private loadClosure() { this.http.get<any>(`${this.api}/${this.sel.id}/closure-preview`).subscribe({ next: c => this.closure = c, error: () => this.closure = null }); }
  canRevert() { let n = 0; for (const r of this.rates) n += r.type === 'RATE_CHANGE' ? 1 : -1; return n > 0; }
  tuPreview() {
    const a = Number(this.tu.amount), rate = Number(this.tu.rate || this.sel.interestRate);
    if (!a || a < 10000 || !this.sel || this.sel.status !== 'Approved') return null;
    const rp = Number(this.sel.remainingPrincipal), cur = Number(this.sel.interestRate);
    const principal = rp + a, blended = (rp * cur + a * rate) / principal;
    const months = Number(this.sel.remainingTenure) + Number(this.tu.extraMonths || 0);
    const r = blended / 1200; const emi = r === 0 ? principal / months : principal * r * Math.pow(1 + r, months) / (Math.pow(1 + r, months) - 1);
    return { rate: Math.round(blended * 100) / 100, principal, emi };
  }
  topup() {
    const a = Number(this.tu.amount);
    if (!a || a < 10000) { this.flash('', 'Enter a top-up amount of at least 10,000'); return; }
    if (!confirm(`Release ₹${a.toLocaleString('en-IN')} to the customer's account?`)) return;
    let url = `${this.api}/${this.sel.id}/topup?amount=${a}&admin=${this.adm()}&extraMonths=${Number(this.tu.extraMonths || 0)}`;
    if (this.tu.rate) url += `&rate=${this.tu.rate}`;
    if (this.tu.note) url += `&note=${encodeURIComponent(this.tu.note)}`;
    this.busy = true;
    this.http.post<any>(url, {}).subscribe({ next: r => { this.busy = false; this.tu.amount = null; this.applyUpdate(r, 'Top-up released to customer account'); }, error: e => this.fail(e) });
  }
  revertRate() {
    if (!confirm('Revert the last interest rate change? EMI will be recalculated.')) return;
    this.busy = true;
    this.http.put<any>(`${this.api}/${this.sel.id}/revert-rate?admin=${this.adm()}`, {}).subscribe({ next: r => { this.busy = false; this.applyUpdate(r, 'Interest rate reverted'); }, error: e => this.fail(e) });
  }
  closeLoan() {
    if (!confirm('Close this loan? This records full settlement received at the bank.')) return;
    this.busy = true;
    this.http.post<any>(`${this.api}/${this.sel.id}/admin-close?admin=${this.adm()}&reference=${encodeURIComponent(this.closeRef)}`, {}).subscribe({ next: r => { this.busy = false; this.tab = 'account'; this.applyUpdate(r, 'Loan closed. You can now generate the NOC.'); }, error: e => this.fail(e) });
  }
  noc() {
    this.http.get<any>(`${this.api}/${this.sel.id}/noc?admin=${this.adm()}`).subscribe({ next: n => { this.sel = n.loan; this.load(); this.refreshEvents(); printHomeLoanNoc(n); }, error: e => this.fail(e) });
  }
}
