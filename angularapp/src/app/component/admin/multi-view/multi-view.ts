import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

interface MultiViewOption { path: string; label: string; }
interface MultiViewPane { path: string; url: SafeResourceUrl | null; }

@Component({
  selector: 'app-admin-multi-view',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="mv-root">
      <div class="mv-bar">
        <button class="mv-btn" (click)="goHome()">&larr; Dashboard Home</button>
        <strong>4-in-1 Multitasking Screen</strong>
        <span class="mv-hint">Pick a feature in each pane and work on four things at once.</span>
        <button class="mv-btn" (click)="reloadAll()">Reload all</button>
      </div>
      <div class="mv-grid" [class.has-max]="maximizedIndex !== -1">
        <div class="mv-pane" *ngFor="let pane of panes; let i = index"
             [class.hidden]="maximizedIndex !== -1 && maximizedIndex !== i">
          <div class="mv-pane-head">
            <select [ngModel]="pane.path" (ngModelChange)="setPane(i, $event)">
              <option value="">-- Select feature --</option>
              <option *ngFor="let o of options" [value]="o.path">{{ o.label }}</option>
            </select>
            <button class="mv-btn" (click)="reload(i)" title="Reload">&#8635;</button>
            <button class="mv-btn" (click)="toggleMax(i)" title="Maximize / restore">
              {{ maximizedIndex === i ? 'Restore' : 'Maximize' }}
            </button>
          </div>
          <iframe *ngIf="pane.url" [src]="pane.url" title="Admin feature pane"></iframe>
          <div class="mv-empty" *ngIf="!pane.url">Select a feature to load it here</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mv-root { display: flex; flex-direction: column; height: 100vh; background: #eef1f6; }
    .mv-bar { display: flex; align-items: center; gap: 14px; padding: 8px 14px; background: #1a237e; color: #fff; }
    .mv-hint { flex: 1; font-size: 12px; opacity: .8; }
    .mv-btn { border: 0; border-radius: 4px; padding: 4px 10px; cursor: pointer; background: #e8eaf6; color: #1a237e; }
    .mv-grid { flex: 1; display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 6px; padding: 6px; min-height: 0; }
    .mv-grid.has-max { grid-template-columns: 1fr; grid-template-rows: 1fr; }
    .mv-pane { display: flex; flex-direction: column; background: #fff; border: 1px solid #c5cae9; border-radius: 6px; overflow: hidden; min-height: 0; }
    .mv-pane.hidden { display: none; }
    .mv-pane-head { display: flex; gap: 6px; padding: 5px; background: #e8eaf6; }
    .mv-pane-head select { flex: 1; padding: 4px; }
    iframe { flex: 1; width: 100%; border: 0; }
    .mv-empty { flex: 1; display: flex; align-items: center; justify-content: center; color: #888; }
    @media (max-width: 900px) { .mv-grid { grid-template-columns: 1fr; grid-template-rows: repeat(4, 70vh); overflow: auto; } }
  `]
})
export class AdminMultiView implements OnInit {
  options: MultiViewOption[] = [
    { path: 'users', label: 'Users' },
    { path: 'transactions', label: 'Transactions' },
    { path: 'loans', label: 'Loans' },
    { path: 'cards', label: 'Cards' },
    { path: 'credit-cards', label: 'Credit Cards' },
    { path: 'kyc', label: 'KYC' },
    { path: 'video-kyc', label: 'Video KYC' },
    { path: 'cheques', label: 'Cheques' },
    { path: 'cheque-draw-management', label: 'Cheque Draw' },
    { path: 'business-cheque-management', label: 'Business Cheque Draw' },
    { path: 'savings-cheque-management', label: 'Savings Cheque Draw' },
    { path: 'demand-drafts', label: 'Demand Drafts' },
    { path: 'gold-loans', label: 'Gold Loans' },
    { path: 'education-loan-applications', label: 'Education Loans' },
    { path: 'subsidy-claims', label: 'Subsidy Claims' },
    { path: 'fixed-deposits', label: 'Fixed Deposits' },
    { path: 'investments', label: 'Investments' },
    { path: 'emi-management', label: 'EMI Management' },
    { path: 'current-accounts', label: 'Current Accounts' },
    { path: 'insurance-dashboard', label: 'Insurance' },
    { path: 'fasttags', label: 'FASTag' },
    { path: 'merchant-onboarding', label: 'Merchant Onboarding' },
    { path: 'agent-management', label: 'Agent Management' },
    { path: 'admin-open-account', label: 'Open Account' },
    { path: 'family-banking', label: 'Family Banking' },
    { path: 'positive-pay', label: 'Positive Pay' },
    { path: 'account-verification', label: 'Account Verification' },
    { path: 'user-control', label: 'User Control' },
    { path: 'ai-security', label: 'AI Security' },
    { path: 'chat', label: 'Chat' }
  ];
  panes: MultiViewPane[] = [0, 1, 2, 3].map(() => ({ path: '', url: null }));
  maximizedIndex = -1;
  private readonly storageKey = 'adminMultiViewPanes';

  constructor(private router: Router, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    const defaults = ['users', 'transactions', 'loans', 'kyc'];
    let saved: string[] = [];
    try { saved = JSON.parse(localStorage.getItem(this.storageKey) || '[]'); } catch { saved = []; }
    defaults.forEach((d, i) => this.load(i, saved[i] ?? d));
  }

  setPane(i: number, path: string): void {
    this.load(i, path);
    localStorage.setItem(this.storageKey, JSON.stringify(this.panes.map(p => p.path)));
  }

  reload(i: number): void {
    const path = this.panes[i].path;
    this.panes[i].url = null;
    setTimeout(() => this.load(i, path));
  }

  reloadAll(): void { this.panes.forEach((_, i) => this.reload(i)); }

  toggleMax(i: number): void { this.maximizedIndex = this.maximizedIndex === i ? -1 : i; }

  goHome(): void { this.router.navigate(['/admin/dashboard']); }

  private load(i: number, path: string): void {
    const valid = this.options.some(o => o.path === path);
    this.panes[i].path = valid ? path : '';
    this.panes[i].url = valid
      ? this.sanitizer.bypassSecurityTrustResourceUrl(`/admin/${path}`)
      : null;
  }
}
