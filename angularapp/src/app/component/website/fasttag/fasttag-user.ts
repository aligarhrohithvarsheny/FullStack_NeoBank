import { Component, OnInit, Inject, PLATFORM_ID, ViewEncapsulation } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { FasttagService, FasttagApplication } from '../../../service/fasttag.service';
import { AccountService } from '../../../service/account';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-fasttag-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fasttag-user.html',
  styleUrls: ['./fasttag-user.css'],
  encapsulation: ViewEncapsulation.None
})
export class FasttagUser implements OnInit {
  activeTab: 'PURCHASE' | 'RECHARGE' = 'PURCHASE';

  // Purchase form fields
  chassisNumber = '';
  engineNumber = '';
  vehicleRegNumber = '';
  make = '';
  model = '';
  fuelType = '';
  rcFrontFile: File | null = null;
  rcBackFile: File | null = null;
  rcFrontName = '';
  rcBackName = '';

  // Customer Details (auto-filled)
  customerName = '';
  mobileNumber = '';
  emailId = '';

  // Dispatch Address
  dispatchAddress = '';
  pinCode = '';
  city = '';
  state = '';

  // Payment
  debitAccountNumber = '';
  availableBalance: number = 0;
  termsAccepted = false;
  userAccounts: any[] = [];

  // Tag details
  tagIssuanceFee = 99.12;
  tagSecurityDeposit = 200.00;
  tagUploadAmount = 100.00;

  get tagTotalAmount(): number {
    return this.tagIssuanceFee + this.tagSecurityDeposit + this.tagUploadAmount;
  }

  // Recharge form fields
  rechargeType: 'vehicle' | 'cug' = 'vehicle';
  rechargeVehicleNumber = '';
  rechargeAmount: number | null = null;
  rechargeDebitAccount = '';
  rechargeAvailableBalance: number = 0;

  // My FASTags list
  myTags: FasttagApplication[] = [];
  selectedTag: FasttagApplication | null = null;
  transactions: any[] = [];
  isLoadingTransactions = false;
  searchTerm = '';
  statusFilter = 'All';

  // State data
  states = [
    'ANDHRA PRADESH', 'ARUNACHAL PRADESH', 'ASSAM', 'BIHAR', 'CHHATTISGARH',
    'GOA', 'GUJARAT', 'HARYANA', 'HIMACHAL PRADESH', 'JHARKHAND', 'KARNATAKA',
    'KERALA', 'MADHYA PRADESH', 'MAHARASHTRA', 'MANIPUR', 'MEGHALAYA', 'MIZORAM',
    'NAGALAND', 'ODISHA', 'PUNJAB', 'RAJASTHAN', 'SIKKIM', 'TAMIL NADU',
    'TELANGANA', 'TRIPURA', 'UTTAR PRADESH', 'UTTARAKHAND', 'WEST BENGAL',
    'DELHI', 'JAMMU AND KASHMIR', 'LADAKH'
  ];

  makes = ['Maruti Suzuki', 'Hyundai', 'Tata', 'Mahindra', 'Honda', 'Toyota', 'Kia', 'MG', 'Volkswagen', 'Skoda', 'Renault', 'Ford', 'Nissan', 'BMW', 'Mercedes-Benz', 'Audi', 'Other'];
  fuelTypes = ['Petrol', 'Diesel', 'CNG', 'Electric', 'Hybrid'];

  private currentUserId = 'guest';
  private fastagGmailId = '';
  isSubmitting = false;
  today = new Date();

  constructor(
    public fasttagService: FasttagService,
    private http: HttpClient,
    private accountService: AccountService,
    private alertService: AlertService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId) && typeof sessionStorage !== 'undefined') {
      try {
        const fastagUserRaw = sessionStorage.getItem('fastagUser');
        if (fastagUserRaw) {
          const fastagUser = JSON.parse(fastagUserRaw);
          this.fastagGmailId = (fastagUser.gmailId || '').trim().toLowerCase();
          this.currentUserId = fastagUser.id || this.fastagGmailId || 'guest';
          this.customerName = fastagUser.userName || fastagUser.username || fastagUser.name || '';
          this.mobileNumber = fastagUser.phone || fastagUser.mobileNumber || '';
          this.emailId = this.fastagGmailId;
        } else {
          const currentUserRaw = sessionStorage.getItem('currentUser');
          const user = currentUserRaw ? JSON.parse(currentUserRaw) : { id: 'guest' };
          this.currentUserId = user.id || 'guest';
          this.customerName = user.username || user.name || '';
          this.mobileNumber = user.phone || user.mobileNumber || '';
          this.emailId = user.email || '';
        }

        // Load user profile for address fields
        const profileRaw = sessionStorage.getItem('userProfile');
        if (profileRaw) {
          const profile = JSON.parse(profileRaw);
          this.customerName = profile.name || this.customerName;
          this.mobileNumber = profile.phone || this.mobileNumber;
          this.emailId = profile.email || this.emailId;
          this.dispatchAddress = profile.address || '';
          this.pinCode = profile.pinCode || '';
          this.city = profile.city || '';
          this.state = profile.state || '';
        }
      } catch (e) {
        console.warn('Could not read sessionStorage', e);
      }
    }

    this.loadMyTags();
    this.loadUserAccounts();
  }

  loadMyTags() {
    if (this.fastagGmailId) {
      this.http.get<any>(`${environment.apiBaseUrl}/api/fastag/user-details/${encodeURIComponent(this.fastagGmailId)}`).subscribe({
        next: (res) => { this.myTags = (res?.fasttags || []) as FasttagApplication[]; },
        error: (err) => { console.error('Failed to load FASTags by Gmail', err); this.myTags = []; }
      });
      return;
    }
    this.fasttagService.listForUser(this.currentUserId).subscribe({
      next: (res) => { this.myTags = res || []; },
      error: (err) => { console.error('Failed to load FASTags', err); }
    });
  }

  loadUserAccounts() {
    if (this.fastagGmailId) {
      this.http.get<any>(`${environment.apiBaseUrl}/api/fastag/linked-accounts/${encodeURIComponent(this.fastagGmailId)}`).subscribe({
        next: (res) => {
          const accounts = res?.linkedAccounts || [];
          this.userAccounts = accounts.map((acc: any) => ({
            accountNumber: acc.accountNumber,
            id: acc.accountNumber,
            name: acc.accountHolderName || 'Linked Account',
            balance: 0
          }));
        },
        error: () => { this.userAccounts = []; }
      });
      return;
    }

    // Fetch only the logged-in user's account
    if (isPlatformBrowser(this.platformId) && typeof sessionStorage !== 'undefined') {
      try {
        const currentUserRaw = sessionStorage.getItem('currentUser');
        const user = currentUserRaw ? JSON.parse(currentUserRaw) : null;
        const accNo = user?.accountNumber;
        if (accNo) {
          this.accountService.getAccountByNumber(accNo).subscribe({
            next: (acc: any) => {
              this.userAccounts = acc ? [acc] : [];
            },
            error: () => { this.userAccounts = []; }
          });
          return;
        }
      } catch (e) { /* fall through */ }
    }
    this.userAccounts = [];
  }

  onDebitAccountChange() {
    const acc = this.userAccounts.find((a: any) => String(a.accountNumber) === this.debitAccountNumber || String(a.id) === this.debitAccountNumber);
    this.availableBalance = acc ? acc.balance : 0;
  }

  onRechargeDebitAccountChange() {
    const acc = this.userAccounts.find((a: any) => String(a.accountNumber) === this.rechargeDebitAccount || String(a.id) === this.rechargeDebitAccount);
    this.rechargeAvailableBalance = acc ? acc.balance : 0;
  }

  onRcFrontSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.rcFrontFile = input.files[0];
      this.rcFrontName = input.files[0].name;
    }
  }

  onRcBackSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.rcBackFile = input.files[0];
      this.rcBackName = input.files[0].name;
    }
  }

  removeAllRcFiles() {
    this.rcFrontFile = null;
    this.rcBackFile = null;
    this.rcFrontName = '';
    this.rcBackName = '';
  }

  submitPurchase() {
    if (!this.chassisNumber || !this.engineNumber || !this.vehicleRegNumber) {
      this.alertService.error('Validation Error', 'Please fill Chassis Number, Engine Number and Vehicle Registration Number');
      return;
    }
    if (!this.make || !this.fuelType) {
      this.alertService.error('Validation Error', 'Please select Make and Fuel Type');
      return;
    }
    if (!this.debitAccountNumber) {
      this.alertService.error('Validation Error', 'Please select a debit account');
      return;
    }
    if (!this.termsAccepted) {
      this.alertService.error('Terms Required', 'Please accept the Terms & Conditions');
      return;
    }

    this.isSubmitting = true;
    const payload: Partial<FasttagApplication> = {
      userId: this.currentUserId,
      userName: this.customerName,
      chassisNumber: this.chassisNumber,
      engineNumber: this.engineNumber,
      vehicleNumber: this.vehicleRegNumber,
      vehicleDetails: `${this.make} ${this.model}`.trim(),
      make: this.make,
      model: this.model,
      fuelType: this.fuelType,
      vehicleType: 'Car',
      mobileNumber: this.mobileNumber,
      email: this.emailId,
      dispatchAddress: this.dispatchAddress,
      pinCode: this.pinCode,
      city: this.city,
      state: this.state,
      tagIssuanceFee: this.tagIssuanceFee,
      tagSecurityDeposit: this.tagSecurityDeposit,
      tagUploadAmount: this.tagUploadAmount,
      tagTotalAmount: this.tagTotalAmount,
      debitAccountNumber: this.debitAccountNumber,
      termsAccepted: this.termsAccepted,
      amount: this.tagTotalAmount,
      bank: 'NeoBank'
    };

    this.fasttagService.apply(payload).subscribe({
      next: (saved) => {
        // Upload RC files if selected
        if (this.rcFrontFile || this.rcBackFile) {
          this.fasttagService.uploadRcFiles(saved.id, this.rcFrontFile || undefined, this.rcBackFile || undefined).subscribe({
            next: () => {
              this.alertService.success('FASTag Purchase Submitted', 'Your FASTag purchase application has been submitted successfully. Admin will review and approve.');
              this.resetPurchaseForm();
              this.loadMyTags();
            },
            error: () => {
              this.alertService.success('FASTag Submitted', 'Application submitted but RC upload failed. You can upload later.');
              this.resetPurchaseForm();
              this.loadMyTags();
            }
          });
        } else {
          this.alertService.success('FASTag Purchase Submitted', 'Your FASTag purchase application has been submitted successfully.');
          this.resetPurchaseForm();
          this.loadMyTags();
        }
        this.isSubmitting = false;
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err?.error?.message || 'Failed to submit FASTag purchase';
        this.alertService.error('Purchase Failed', msg);
      }
    });
  }

  resetPurchaseForm() {
    this.chassisNumber = '';
    this.engineNumber = '';
    this.vehicleRegNumber = '';
    this.make = '';
    this.model = '';
    this.fuelType = '';
    this.rcFrontFile = null;
    this.rcBackFile = null;
    this.rcFrontName = '';
    this.rcBackName = '';
    this.termsAccepted = false;
  }

  submitRecharge() {
    const amt = Number(this.rechargeAmount || 0);
    if (!this.rechargeVehicleNumber) {
      this.alertService.error('Validation Error', 'Please enter Vehicle Registration Number');
      return;
    }
    if (amt < 500 || amt > 10000) {
      this.alertService.error('Validation Error', 'Recharge amount must be between ₹500 and ₹10000');
      return;
    }

    this.isSubmitting = true;
    this.fasttagService.rechargeByUser({
      vehicleNumber: this.rechargeVehicleNumber,
      amount: amt,
      debitAccountNumber: this.rechargeDebitAccount,
      userId: this.currentUserId
    }).subscribe({
      next: (updated) => {
        this.isSubmitting = false;
        this.alertService.success('Recharge Successful', `₹${amt} has been added to your FASTag`);
        this.rechargeVehicleNumber = '';
        this.rechargeAmount = null;
        this.loadMyTags();
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err?.error?.message || 'Unable to process recharge';
        this.alertService.error('Recharge Failed', msg);
      }
    });
  }

  // Tag selection and details
  selectTag(t: FasttagApplication) {
    this.selectedTag = t;
    this.transactions = [];
    this.fasttagService.getById(t.id).subscribe({ next: (fresh) => { this.selectedTag = fresh; }, error: () => {} });
    this.loadTransactions(t.id);
  }

  loadTransactions(fasttagId: string) {
    this.isLoadingTransactions = true;
    this.fasttagService.getTransactions(fasttagId).subscribe({
      next: (res: any[]) => {
        this.transactions = (res || []).map(tx => {
          const copy = { ...tx };
          if (copy.createdAt && !(copy.createdAt instanceof Date)) {
            const d = new Date(copy.createdAt);
            if (!isNaN(d.getTime())) copy.createdAt = d;
          }
          return copy;
        });
        this.isLoadingTransactions = false;
      },
      error: () => { this.transactions = []; this.isLoadingTransactions = false; }
    });
  }

  downloadSticker(tagId: any) {
    if (!tagId) return;
    if (isPlatformBrowser(this.platformId)) {
      const id = String(tagId);
      const url = this.fastagGmailId
        ? this.fasttagService.downloadStickerForFastagUser(id, this.fastagGmailId)
        : this.fasttagService.downloadSticker(id);
      window.open(url, '_blank');
    }
  }

  getFilteredTags() {
    const q = (this.searchTerm || '').toLowerCase();
    return (this.myTags || []).filter(t => {
      const matchesQ = !q || (t.vehicleNumber || '').toLowerCase().includes(q) || (t.fasttagNumber || '').toLowerCase().includes(q);
      const matchesStatus = this.statusFilter === 'All' || (t.status || '') === this.statusFilter;
      return matchesQ && matchesStatus;
    });
  }

  goBack() {
    if (isPlatformBrowser(this.platformId) && typeof window !== 'undefined' && window.history) {
      window.history.back();
    }
  }
}
