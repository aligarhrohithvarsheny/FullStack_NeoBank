import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FasttagService, FasttagApplication } from '../../../service/fasttag.service';
import { AccountService } from '../../../service/account';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-fasttag-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fasttag-admin.html',
  styleUrls: ['./fasttag-admin.css'],
  encapsulation: ViewEncapsulation.None
})
export class FasttagAdmin implements OnInit {
  list: FasttagApplication[] = [];
  rechargeAmount: number = 0;
  selectedTag: FasttagApplication | null = null;
  transactionsMap: { [fasttagId: string]: any[] } = {};
  showingTransactionsFor: string | null = null;
  searchTerm: string = '';
  statusFilter: string = 'All';
  // account assignment helpers
  assigningTagId: string | null = null;
  accountsList: any[] = [];
  selectedAccountId: string | null = null;
  // detail view
  selectedDetailTag: FasttagApplication | null = null;

  constructor(private fasttagService: FasttagService, private alertService: AlertService, private accountService: AccountService) {}

  ngOnInit() {
    this.reload();
  }

  getFilteredList() {
    const q = (this.searchTerm || '').toLowerCase();
    return (this.list || []).filter(t => {
      const matchesQ = !q || (
        (t.vehicleNumber || '').toLowerCase().includes(q) ||
        (t.userName || t.userId || '').toString().toLowerCase().includes(q) ||
        (t.fasttagNumber || '').toLowerCase().includes(q)
      );
      const matchesStatus = this.statusFilter === 'All' || (t.status || '') === this.statusFilter;
      return matchesQ && matchesStatus;
    });
  }

  getTotal() { return this.list ? this.list.length : 0; }
  getAppliedCount() { return (this.list || []).filter(x => x.status === 'Applied').length; }
  getApprovedCount() { return (this.list || []).filter(x => x.status === 'Approved').length; }
  getClosedCount() { return (this.list || []).filter(x => x.status === 'Closed').length; }

  goBack() { history.back(); }

  reload() {
    this.fasttagService.listAll().subscribe({
      next: (res) => { this.list = res || []; },
      error: (err) => { console.error('Failed to load fasttags', err); this.alertService.error('Load Failed', 'Unable to load FASTag list'); }
    });
  }

  approve(item: FasttagApplication) {
    this.fasttagService.approve(item.id as any).subscribe({
      next: (updated) => {
        this.alertService.success('FASTag Approved', 'Approved and FASTag generated: ' + updated.fasttagNumber);
        this.reload();
      },
      error: (err) => {
        console.error('Approve error', err);
        const msg = err?.error?.message || 'Unable to approve the FASTag application';
        this.alertService.error('Approval Failed', msg);
      }
    });
  }

  selectForRecharge(item: FasttagApplication) {
    this.selectedTag = item;
    this.rechargeAmount = 0;
  }

  recharge() {
    if (!this.selectedTag) return;
    // Assume admin is performing the recharge from admin panel; use admin id as 'admin'
    this.fasttagService.rechargeByTagAsAdmin(this.selectedTag.fasttagNumber || '', this.rechargeAmount, 'admin').subscribe({
      next: (updated) => {
        this.alertService.success('Recharge Successful', 'FASTag recharged by ₹' + this.rechargeAmount);
        this.selectedTag = null;
        this.reload();
      },
      error: (err) => {
        console.error('Recharge error', err);
        const msg = err?.error?.message || (err?.error?.message || 'Unable to recharge the selected FASTag');
        this.alertService.error('Recharge Failed', msg);
        // If insufficient funds in debit account, open assign account UI to let admin choose another debit account
        if (String(msg).toLowerCase().includes('insufficient fund')) {
          this.openAssignAccount(this.selectedTag!);
        }
      }
    });
  }

  confirmRechargeWithAccount(fasttagId: string | null, accountId: string | null) {
    if (!this.selectedTag) return;
    if (!fasttagId || !accountId) { this.alertService.info('Select Account', 'Please select an account to assign'); return; }
    this.fasttagService.rechargeByTagAsAdminWithAccount(this.selectedTag.fasttagNumber || '', this.rechargeAmount, 'admin', String(accountId)).subscribe({
      next: (updated: any) => {
        this.alertService.success('Recharge Successful', 'FASTag recharged by ₹' + this.rechargeAmount);
        this.selectedTag = null;
        this.assigningTagId = null;
        this.reload();
      },
      error: (err: any) => {
        console.error('Recharge error with account', err);
        const msg = err?.error?.message || 'Unable to recharge with selected account';
        this.alertService.error('Recharge Failed', msg);
      }
    });
  }

  adminApplyAs(userId: string) {
    // open small prompt to add as admin - simplified flow
    this.fasttagService.adminApply({
      userId: userId || 'admin',
      userName: 'admin',
      vehicleDetails: 'Admin vehicle',
      vehicleNumber: 'ADMIN-001',
      aadharNumber: '',
      panNumber: '',
      dob: '',
      vehicleType: 'Car',
      amount: 500,
      bank: 'AdminBank'
    } as any).subscribe({
      next: (res) => { this.alertService.success('Application Created', 'FASTag application created for user: ' + userId); this.reload(); },
      error: (err) => { console.error('Admin apply error', err); this.alertService.error('Create Failed', 'Unable to create FASTag application'); }
    });
  }

  toggleTransactions(item: FasttagApplication) {
    const id = item.id as any;
    if (this.showingTransactionsFor === id) {
      this.showingTransactionsFor = null;
      return;
    }
    this.showingTransactionsFor = id;
    this.fasttagService.getTransactions(String(id)).subscribe({
      next: (res) => { this.transactionsMap[String(id)] = res || []; },
      error: (err) => { console.error('Failed to load transactions', err); this.alertService.error('Error', 'Unable to load transactions'); }
    });
  }

  openAssignAccount(item: FasttagApplication) {
    // open the inline assign UI for this tag
    this.assigningTagId = item.id as any;
    this.selectedAccountId = null;
    // fetch accounts (first 200) and filter to customer's accounts where possible
    this.accountService.getAllAccounts(0, 200).subscribe({
      next: (res) => {
        const accounts = (res && res.content) ? res.content : res || [];
        // try filter by customerId matching tag userId
        const userAccounts = accounts.filter((a: any) => String(a.customerId || a.customerId) === String(item.userId));
        this.accountsList = userAccounts.length ? userAccounts : accounts;
      },
      error: (err) => { console.error('Failed to load accounts', err); this.alertService.error('Error', 'Unable to load accounts'); }
    });
  }

  cancelAssign() {
    this.assigningTagId = null;
    this.accountsList = [];
    this.selectedAccountId = null;
  }

  confirmAssign(fasttagId: string | null) {
    if (!fasttagId || !this.selectedAccountId) { this.alertService.info('Select Account', 'Please select an account to assign'); return; }
    this.fasttagService.assignAccount(String(fasttagId), String(this.selectedAccountId)).subscribe({
      next: (res: any) => {
        this.alertService.success('Assigned', 'Account assigned to FASTag successfully');
        this.assigningTagId = null;
        this.accountsList = [];
        this.selectedAccountId = null;
        this.reload();
      },
      error: (err) => { console.error('Assign failed', err); this.alertService.error('Assign Failed', 'Unable to assign account'); }
    });
  }

  closeFasttag(item: FasttagApplication) {
    if (!confirm('Close this FASTag? This will mark it closed and prevent new payments.')) return;
    const id = item.id as any;
    this.fasttagService.closeFasttag(String(id)).subscribe({
      next: (res) => { this.alertService.success('Closed', 'FASTag closed successfully'); this.reload(); },
      error: (err) => { console.error('Close error', err); this.alertService.error('Close Failed', 'Unable to close FASTag'); }
    });
  }

  viewDetails(item: FasttagApplication) {
    this.selectedDetailTag = this.selectedDetailTag?.id === item.id ? null : item;
  }

  getRcImageUrl(filename: string): string {
    return this.fasttagService.getRcImageUrl(filename);
  }
}
