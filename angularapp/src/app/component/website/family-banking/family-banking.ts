import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FamilyBankingService, FamilyInvitation, JointTransfer, MinorApplication, JointAccountProfile } from '../../../service/family-banking.service';

@Component({
  selector: 'app-family-banking', standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: './family-banking.html', styleUrls: ['./family-banking.css']
})
export class FamilyBankingComponent implements OnInit {
  userId = 0; accountNumber = ''; inviteeEmail = ''; destinationAccount = ''; amount = 0; note = '';
  minorName = ''; dateOfBirth = ''; monthlyLimit = 5000; dailyLimit = 2000;
  invitations: FamilyInvitation[] = []; transfers: JointTransfer[] = []; applications: MinorApplication[] = []; links: any[] = [];
  jointAccounts: JointAccountProfile[] = [];
  message = ''; error = ''; busy = false;
  constructor(private family: FamilyBankingService) {}
  ngOnInit(): void {
    const raw = sessionStorage.getItem('currentUser'); const user = raw ? JSON.parse(raw) : null;
    this.userId = Number(user?.id || 0); this.accountNumber = user?.accountNumber || '';
    if (this.userId) this.refresh();
  }
  refresh(): void { this.family.invitations(this.userId).subscribe(v => this.invitations = v); this.family.pendingTransfers(this.userId).subscribe(v => this.transfers = v); this.family.applications(this.userId).subscribe(v => this.applications = v); this.family.guardianLinks(this.userId).subscribe(v => this.links = v); }
  refresh(): void { this.family.invitations(this.userId).subscribe(v => this.invitations = v); this.family.jointAccounts(this.userId).subscribe(v => this.jointAccounts = v); this.family.pendingTransfers(this.userId).subscribe(v => this.transfers = v); this.family.applications(this.userId).subscribe(v => this.applications = v); this.family.guardianLinks(this.userId).subscribe(v => this.links = v); }
  invite(): void { this.run(this.family.invite(this.userId, this.accountNumber, this.inviteeEmail), 'Invitation sent'); }
  respond(invitation: FamilyInvitation, approve: boolean): void { this.run(this.family.respondInvitation(this.userId, invitation.id, approve), approve ? 'Invitation accepted' : 'Invitation declined'); }
  requestTransfer(): void { this.run(this.family.requestTransfer(this.userId, this.accountNumber, this.destinationAccount, this.amount, this.note), 'Transfer sent for joint approval'); }
  decide(transfer: JointTransfer, approve: boolean): void { this.run(this.family.decideTransfer(this.userId, transfer.id, approve), approve ? 'Transfer approved' : 'Transfer declined'); }
  applyMinor(): void { this.run(this.family.applyMinor({ guardianUserId: this.userId, minorName: this.minorName, dateOfBirth: this.dateOfBirth, monthlyLimit: this.monthlyLimit, dailyLimit: this.dailyLimit }), 'Minor account application submitted'); }
  private run<T>(request: import('rxjs').Observable<T>, success: string): void { this.busy = true; this.error = ''; request.subscribe({ next: () => { this.busy = false; this.message = success; this.refresh(); }, error: e => { this.busy = false; this.error = e?.error?.message || e?.error?.error || 'Family Banking request failed'; } }); }
}
