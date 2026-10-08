import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-admin-home-loans',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="ah">
  <h2>🏠 Home Loan Dashboard</h2>
  <div class="msg ok" *ngIf="msg">{{msg}}</div>
  <div class="msg err" *ngIf="err">{{err}}</div>
  <div class="kpis">
    <div><small>Total</small><b>{{loans.length}}</b></div>
    <div><small>Pending</small><b>{{count('Submitted')+count('Under Review')+count('Documents Required')+count('Documents Submitted')+count('Documents Verified')}}</b></div>
    <div><small>Approved</small><b>{{count('Approved')}}</b></div>
    <div><small>Closed</small><b>{{count('Closed')}}</b></div>
    <div><small>Rejected</small><b>{{count('Rejected')}}</b></div>
    <div><small>Cancelled</small><b>{{count('Cancelled')}}</b></div>
  </div>
  <input class="search" placeholder="Search application ID / account / name" [(ngModel)]="q">
  <table>
    <tr><th>Application ID</th><th>Applicant</th><th>Account</th><th>Amount</th><th>Tenure</th><th>Rate</th><th>Status</th><th>AI</th><th>Actions</th></tr>
    <tr *ngFor="let l of filtered()">
      <td><b>{{l.applicationId}}</b></td><td>{{l.userName}}</td><td>{{l.accountNumber}}</td>
      <td>₹{{l.amount|number}}</td><td>{{l.tenure}}m</td><td>{{l.interestRate}}%</td>
      <td><span class="badge">{{l.status}}</span></td>
      <td>{{l.approvalProbability ? l.approvalProbability+'% '+l.riskBand : '-'}}</td>
      <td>
        <button class="b" (click)="open(l)">Open</button>
        <button class="b" *ngIf="canAdvance(l)" (click)="advance(l)">Next ▶</button>
      </td>
    </tr>
  </table>

  <div class="modal" *ngIf="sel">
   <div class="panel">
    <div class="row"><h3>{{sel.applicationId}} <span class="badge">{{sel.status}}</span></h3><button class="b" (click)="sel=null">✕</button></div>

    <h4>Details (editable)</h4>
    <div class="grid">
      <label>Amount<input type="number" [(ngModel)]="edit.amount" [disabled]="live(sel)"></label>
      <label>Tenure (m)<input type="number" [(ngModel)]="edit.tenure" [disabled]="live(sel)"></label>
      <label>Interest %<input type="number" step="0.05" [(ngModel)]="edit.interestRate"></label>
      <label>Property value<input type="number" [(ngModel)]="edit.propertyValue"></label>
      <label>Purpose<input [(ngModel)]="edit.purpose"></label>
      <label>Property address<input [(ngModel)]="edit.propertyAddress"></label>
      <label>Admin notes<input [(ngModel)]="edit.adminNotes"></label>
    </div>
    <button class="b p" (click)="saveEdit()">Save changes</button>

    <h4>🤖 Real-time AI analysis</h4>
    <div *ngIf="ai">
      <div class="kpis">
        <div><small>Approval probability</small><b>{{ai.approvalProbability}}%</b></div>
        <div><small>Risk</small><b>{{ai.riskBand}}</b></div>
        <div><small>CIBIL</small><b>{{ai.cibilScore}}</b></div>
        <div><small>FOIR</small><b>{{ai.foirPercent}}%</b></div>
        <div><small>PAN</small><b>{{ai.pan||'N/A'}}</b></div>
        <div><small>Avg paid</small><b>{{ai.avgPaidPercentage}}%</b></div>
      </div>
      <table><tr><th>Existing loan</th><th>Reference</th><th>Amount</th><th>Status</th><th>Paid %</th><th>EMI</th></tr>
        <tr *ngFor="let e of ai.existingLoans"><td>{{e.type}}</td><td>{{e.reference}}</td><td>{{e.amount|number}}</td><td>{{e.status}}</td>
          <td><div class="bar"><div [style.width.%]="e.paidPercentage"></div></div>{{e.paidPercentage}}%</td><td>{{e.emi|number:'1.0-0'}}</td></tr>
      </table>
      <ul><li *ngFor="let f of ai.factors">{{f}}</li></ul>
      <p *ngIf="ai.recommendations?.length"><b>Recommendations:</b> {{ai.recommendations.join(' · ')}}</p>
      <small>{{ai.model}} · {{ai.analyzedAt | date:'medium'}}</small>
    </div>

    <h4>Documents</h4>
    <div class="doc" *ngFor="let d of docTypes">
      <span>{{d.label}}: <b>{{sel[d.status]||'Not uploaded'}}</b> <i>{{sel[d.remark]}}</i></span>
      <span *ngIf="sel[d.path]">
        <a class="b" [href]="fileUrl(d.key)" target="_blank">View</a>
        <button class="b p" (click)="verify(d.key,'Verified')">Verify</button>
        <button class="b d" (click)="reupload(d.key)">Request re-upload</button>
      </span>
    </div>

    <h4>Charges & deductions</h4>
    <div *ngIf="charges" class="kpis">
      <div><small>Processing fee</small><b>₹{{charges.processingFee|number:'1.0-0'}}</b></div>
      <div><small>GST on fee</small><b>₹{{charges.gstOnFee|number:'1.0-0'}}</b></div>
      <div><small>Legal</small><b>₹{{charges.legalCharges|number:'1.0-0'}}</b></div>
      <div><small>Total deducted</small><b>₹{{charges.totalCharges|number:'1.0-0'}}</b></div>
      <div><small>Net disbursed</small><b>₹{{charges.netDisbursed|number:'1.0-0'}}</b></div>
    </div>

    <div class="actions">
      <button class="b" *ngIf="canAdvance(sel)" (click)="advance(sel)">Move to next process ▶</button>
      <button class="b p" *ngIf="sel.status==='Documents Verified'" (click)="approve()">Approve & Disburse</button>
      <button class="b d" *ngIf="!live(sel) && sel.status!=='Rejected' && sel.status!=='Cancelled'" (click)="reject()">Reject</button>
    </div>

    <div *ngIf="live(sel)">
      <h4>Loan account {{sel.loanAccountNumber}}</h4>
      <div class="kpis">
        <div><small>EMI</small><b>₹{{sel.emi|number:'1.0-0'}}</b></div>
        <div><small>Outstanding</small><b>₹{{sel.remainingPrincipal|number:'1.0-0'}}</b></div>
        <div><small>EMIs paid</small><b>{{sel.paidEmis}}</b></div>
        <div><small>Interest paid</small><b>₹{{sel.interestPaid|number:'1.0-0'}}</b></div>
        <div><small>Prepaid</small><b>₹{{sel.prepaidAmount|number:'1.0-0'}}</b></div>
      </div>
    </div>

    <h4>History</h4>
    <table *ngIf="events">
      <tr><th>Date</th><th>Event</th><th>By</th><th>Details</th><th>Debit</th><th>Credit</th></tr>
      <tr *ngFor="let e of events"><td>{{e.eventDate|date:'short'}}</td><td>{{e.eventType}}</td><td>{{e.actor}}</td><td>{{e.details}}</td><td>{{e.debit}}</td><td>{{e.credit}}</td></tr>
    </table>
   </div>
  </div>
</div>`,
  styles: [`
.ah{padding:16px}
table{width:100%;border-collapse:collapse;font-size:13px;margin:8px 0}th,td{border:1px solid #e2e8f0;padding:5px 7px;text-align:left}
.kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:8px;margin:8px 0}
.kpis div{background:#f1f5f9;padding:8px;border-radius:8px;display:flex;flex-direction:column}
.b{background:#e2e8f0;border:0;padding:5px 10px;border-radius:6px;cursor:pointer;margin:2px;text-decoration:none;color:#000;display:inline-block}
.b.p{background:#1d4ed8;color:#fff}.b.d{background:#b91c1c;color:#fff}
.badge{background:#e0e7ff;border-radius:10px;padding:2px 8px;font-size:12px}
.search{padding:8px;width:100%;max-width:360px;border:1px solid #cbd5e1;border-radius:6px}
.modal{position:fixed;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:flex-start;justify-content:center;overflow:auto;z-index:1000;padding:20px}
.panel{background:#fff;border-radius:10px;padding:18px;width:min(980px,100%)}
.row{display:flex;justify-content:space-between;align-items:center}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:10px}
label{display:flex;flex-direction:column;font-size:12px;gap:3px}input{padding:7px;border:1px solid #cbd5e1;border-radius:6px}
.doc{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px dashed #e2e8f0}
.bar{background:#e2e8f0;height:8px;border-radius:4px}.bar div{background:#16a34a;height:8px;border-radius:4px}
.msg{padding:8px;border-radius:6px;margin:8px 0}.ok{background:#dcfce7}.err{background:#fee2e2}
`]
})
export class AdminHomeLoans implements OnInit, OnDestroy {
  private api = `${environment.apiBaseUrl}/api/home-loans`;
  loans: any[] = []; q = ''; sel: any = null; edit: any = {}; ai: any = null; charges: any = null; events: any[] | null = null;
  msg = ''; err = '';
  docTypes = [
    { key: 'fdReceipt', label: 'FD Receipt', status: 'fdReceiptStatus', remark: 'fdReceiptRemark', path: 'fdReceiptPath' },
    { key: 'model', label: 'Home Loan Model', status: 'modelDocStatus', remark: 'modelDocRemark', path: 'modelDocPath' },
    { key: 'signature', label: 'Signature', status: 'signatureStatus', remark: 'signatureRemark', path: 'signaturePath' }
  ];
  constructor(private http: HttpClient) {}
  private poll: any;
  ngOnInit() { this.load(); this.poll = setInterval(() => this.load(), 8000); }
  ngOnDestroy() { clearInterval(this.poll); }

  get admin() {
    try { const a = JSON.parse(sessionStorage.getItem('admin') || sessionStorage.getItem('currentAdmin') || '{}'); return a.name || a.email || 'Admin'; } catch { return 'Admin'; }
  }
  private flash(ok: string, bad = '') { this.msg = ok; this.err = bad; setTimeout(() => { this.msg = ''; this.err = ''; }, 5000); }
  private fail(e: any) { this.flash('', e?.error?.message || 'Request failed'); }
  live(l: any) { return l.status === 'Approved' || l.status === 'Closed'; }
  count(s: string) { return this.loans.filter(l => l.status === s).length; }
  filtered() {
    const q = this.q.toLowerCase();
    return this.loans.filter(l => !q || [l.applicationId, l.accountNumber, l.userName].some(v => (v || '').toLowerCase().includes(q)));
  }
  canAdvance(l: any) {
    return ['Submitted', 'Under Review', 'Documents Submitted'].includes(l.status);
  }
  load() {
    this.http.get<any[]>(this.api).subscribe({ next: r => { this.loans = r; if (this.sel) { const f = r.find(x => x.id === this.sel.id); if (f) this.setSel(f); } }, error: e => this.fail(e) });
  }
  private setSel(l: any) {
    this.sel = l;
    this.edit = { amount: l.amount, tenure: l.tenure, interestRate: l.interestRate, propertyValue: l.propertyValue, purpose: l.purpose, propertyAddress: l.propertyAddress, adminNotes: l.adminNotes };
    this.http.get(`${this.api}/charges-preview?amount=${l.amount}`).subscribe({ next: c => this.charges = this.live(l) ? { processingFee: l.processingFee, gstOnFee: l.gstOnFee, legalCharges: l.legalCharges, totalCharges: l.totalCharges, netDisbursed: l.netDisbursed } : c });
    this.http.get<any>(`${this.api}/${l.id}/statement`).subscribe({ next: s => this.events = s.entries });
  }
  open(l: any) { this.ai = null; this.events = null; this.setSel(l); this.http.get(`${this.api}/${l.id}/analysis`).subscribe({ next: a => this.ai = a, error: () => {} }); }
  fileUrl(type: string) { return `${this.api}/${this.sel.id}/documents/${type}/file`; }
  advance(l: any) { this.http.put(`${this.api}/${l.id}/advance?admin=${encodeURIComponent(this.admin)}`, {}).subscribe({ next: () => { this.flash('Moved to next process'); this.load(); }, error: e => this.fail(e) }); }
  saveEdit() { this.http.put(`${this.api}/${this.sel.id}/admin-edit?admin=${encodeURIComponent(this.admin)}`, this.edit).subscribe({ next: () => { this.flash('Details updated'); this.load(); this.http.get(`${this.api}/${this.sel.id}/analysis`).subscribe(a => this.ai = a); }, error: e => this.fail(e) }); }
  verify(type: string, decision: string, remark = '') {
    this.http.put(`${this.api}/${this.sel.id}/documents/${type}/verify?decision=${encodeURIComponent(decision)}&remark=${encodeURIComponent(remark)}&admin=${encodeURIComponent(this.admin)}`, {}).subscribe({ next: () => { this.flash(`Document ${decision}`); this.load(); }, error: e => this.fail(e) });
  }
  reupload(type: string) { const r = prompt('Reason for re-upload?') ; if (r !== null) this.verify(type, 'Reupload Required', r); }
  approve() { if (confirm('Approve and disburse this loan?')) this.http.put(`${this.api}/${this.sel.id}/approve?admin=${encodeURIComponent(this.admin)}`, {}).subscribe({ next: () => { this.flash('Loan approved and disbursed'); this.load(); }, error: e => this.fail(e) }); }
  reject() { const r = prompt('Rejection reason?'); if (r !== null) this.http.put(`${this.api}/${this.sel.id}/reject?admin=${encodeURIComponent(this.admin)}&reason=${encodeURIComponent(r)}`, {}).subscribe({ next: () => { this.flash('Rejected'); this.load(); }, error: e => this.fail(e) }); }
}