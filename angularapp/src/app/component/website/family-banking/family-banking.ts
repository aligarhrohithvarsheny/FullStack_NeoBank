import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FamilyBankingService, FamilyInvitation, JointTransfer, MinorApplication, JointAccountProfile, FamilyNotification, FamilyTransaction } from '../../../service/family-banking.service';

@Component({
  selector: 'app-family-banking', standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: './family-banking.html', styleUrls: ['./family-banking.css']
})
export class FamilyBankingComponent implements OnInit {
  userId = 0; accountNumber = ''; inviteeEmail = ''; transferAccountNumber = ''; destinationAccount = ''; amount = 0; note = '';
  minorName = ''; dateOfBirth = ''; monthlyLimit = 5000; dailyLimit = 2000;
  invitations: FamilyInvitation[] = []; transfers: JointTransfer[] = []; applications: MinorApplication[] = []; links: any[] = []; notifications: FamilyNotification[] = [];
  jointAccounts: JointAccountProfile[] = [];
  history: FamilyTransaction[] = []; selectedAccount: JointAccountProfile | null = null; operatingMode = 'JOINTLY';
  activeSection = 'overview'; message = ''; error = ''; busy = false; loading = false; loaded = false;
  constructor(private family: FamilyBankingService) {}
  ngOnInit(): void {
    const raw = sessionStorage.getItem('currentUser'); const user = raw ? JSON.parse(raw) : null;
    this.userId = Number(user?.id || 0); this.accountNumber = user?.accountNumber || '';
    if (this.userId) this.refresh();
  }
  refresh(): void {
    this.loading = true; this.error = '';
    this.load(this.family.invitations(this.userId), v => this.invitations = v);
    this.load(this.family.jointAccounts(this.userId), v => { this.jointAccounts = v; if (!this.selectedAccount && v.length) this.selectAccount(v[0]); });
    this.load(this.family.pendingTransfers(this.userId), v => this.transfers = v);
    this.load(this.family.applications(this.userId), v => this.applications = v);
    this.load(this.family.guardianLinks(this.userId), v => this.links = v);
    this.load(this.family.notifications(this.userId), v => this.notifications = v, true);
  }
  selectAccount(account: JointAccountProfile): void { this.selectedAccount = account; this.operatingMode = account.operatingMode; this.transferAccountNumber = account.jointAccountNumber; this.loadHistory(); }
  loadHistory(): void { if (this.selectedAccount) this.load(this.family.history(this.userId, this.selectedAccount.jointAccountNumber), v => this.history = v); }
  saveSettings(): void { if (this.selectedAccount) this.run(this.family.updateSettings(this.userId, this.selectedAccount.jointAccountNumber, this.operatingMode), 'Joint account settings updated'); }
  invite(): void { this.run(this.family.invite(this.userId, this.accountNumber, this.inviteeEmail), 'Invitation sent'); }
  respond(invitation: FamilyInvitation, approve: boolean): void { this.run(this.family.respondInvitation(this.userId, invitation.id, approve), approve ? 'Invitation accepted' : 'Invitation declined'); }
  requestTransfer(): void { this.run(this.family.requestTransfer(this.userId, this.transferAccountNumber, this.destinationAccount, this.amount, this.note), 'Transfer sent for joint approval'); }
  decide(transfer: JointTransfer, approve: boolean): void { this.run(this.family.decideTransfer(this.userId, transfer.id, approve), approve ? 'Transfer approved' : 'Transfer declined'); }
  applyMinor(): void { this.run(this.family.applyMinor({ guardianUserId: this.userId, minorName: this.minorName, dateOfBirth: this.dateOfBirth, monthlyLimit: this.monthlyLimit, dailyLimit: this.dailyLimit }), 'Minor account application submitted'); }
  readNotification(notification: FamilyNotification): void { this.run(this.family.markNotificationRead(this.userId, notification.id), 'Notification marked as read'); }
  mask(value: string | undefined): string { return value ? `${value.slice(0, 4)} •••• ${value.slice(-4)}` : 'Not available'; }
  maskId(value: unknown): string { return this.mask(value === undefined || value === null ? undefined : String(value)); }
  get unreadCount(): number { return this.notifications.filter(notification => !notification.readAt).length; }
  statusClass(status: string | undefined): string { return `status-${(status || 'unknown').toLowerCase()}`; }
  transactionDate(transaction: FamilyTransaction): string { return String(transaction.date || transaction.transactionDate || 'Date unavailable'); }
  private load<T>(request: import('rxjs').Observable<T>, assign: (value: T) => void, finish = false): void { request.subscribe({ next: value => { assign(value); if (finish) { this.loading = false; this.loaded = true; } }, error: e => { this.error = e?.error?.message || e?.error?.error || 'Some Family Banking data could not be loaded'; if (finish) { this.loading = false; this.loaded = true; } } }); }
  private run<T>(request: import('rxjs').Observable<T>, success: string): void { this.busy = true; this.error = ''; request.subscribe({ next: () => { this.busy = false; this.message = success; this.refresh(); }, error: e => { this.busy = false; this.error = e?.error?.message || e?.error?.error || 'Family Banking request failed'; } }); }
}
