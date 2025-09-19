import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ReactiveFormsModule, FormsModule } from '@angular/forms';

interface PendingAccount {
  id: string; // temp id
  name: string;
  email: string;
  dob: string;
  income: number;
  occupation: string;
  pan: string;
  aadhar: string;
  mobile: string;
  state: string;
  city: string;
  passwordHash?: string;
  status: 'PENDING' | 'OPEN' | 'REJECTED';
  createdAt: string;
  assignedAccountNumber?: string | null;
}

@Component({
  selector: 'app-createaccount',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],   // ✅ standalone imports
  templateUrl: './createaccount.html',
  styleUrls: ['./createaccount.css']
})
export class Createaccount implements OnInit {
  form!: FormGroup;
  submitted = false;
  submitError = '';
  successMessage = '';
  
  // Approval tracking
  showTracking = false;
  trackingEmail = '';
  trackingMobile = '';
  trackingResult: any = null;
  trackingError = '';

  // PAN regex: 5 letters, 4 digits, 1 letter
  private panRegex = /^[A-Z]{5}[0-9]{4}[A-Z]$/;
  // Aadhar: 12 digits
  private aadharRegex = /^\d{12}$/;
  // Mobile: 10 digits starting 6-9
  private mobileRegex = /^[6-9]\d{9}$/;

  constructor(private fb: FormBuilder, @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email, this.gmailOnlyValidator]],
      dob: ['', [Validators.required]],
      income: ['', [Validators.required, Validators.min(0)]],
      occupation: ['', [Validators.required]],
      pan: ['', [Validators.required, this.panValidator.bind(this)]],
      aadhar: ['', [Validators.required, this.aadharValidator.bind(this)]],
      mobile: ['', [Validators.required, this.mobileValidator.bind(this)]],
      state: ['', [Validators.required]],
      city: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordsMatchValidator });
  }

  // --- Custom validators ---
  gmailOnlyValidator(control: AbstractControl): ValidationErrors | null {
    const val = control.value as string;
    if (!val) return null;
    const lower = val.toLowerCase();
    return lower.endsWith('@gmail.com') ? null : { gmailOnly: true };
  }

  panValidator(control: AbstractControl): ValidationErrors | null {
    const val = (control.value || '').toUpperCase();
    if (!val) return null;
    return this.panRegex.test(val) ? null : { invalidPan: true };
  }

  aadharValidator(control: AbstractControl): ValidationErrors | null {
    const val = control.value || '';
    if (!val) return null;
    return this.aadharRegex.test(val) ? null : { invalidAadhar: true };
  }

  mobileValidator(control: AbstractControl): ValidationErrors | null {
    const val = control.value || '';
    if (!val) return null;
    return this.mobileRegex.test(val) ? null : { invalidMobile: true };
  }

  passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
    const pw = group.get('password')?.value;
    const cpw = group.get('confirmPassword')?.value;
    return pw && cpw && pw !== cpw ? { passwordsMismatch: true } : null;
  }

  // --- Helpers for local storage (mock backend) ---
  private readPendingAccounts(): PendingAccount[] {
    if (!isPlatformBrowser(this.platformId)) return [];
    const raw = localStorage.getItem('pending_accounts');
    return raw ? JSON.parse(raw) as PendingAccount[] : [];
  }
  private writePendingAccounts(list: PendingAccount[]) {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('pending_accounts', JSON.stringify(list));
  }

  private readAdminUsers(): PendingAccount[] {
    if (!isPlatformBrowser(this.platformId)) return [];
    const raw = localStorage.getItem('admin_users');
    return raw ? JSON.parse(raw) as PendingAccount[] : [];
  }
  private writeAdminUsers(list: PendingAccount[]) {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem('admin_users', JSON.stringify(list));
  }

  // Check uniqueness
  private isDuplicate(pan: string, aadhar: string, email: string): { duplicate: boolean, message?: string } {
    const panUp = (pan || '').toUpperCase();
    const aadharStr = aadhar || '';
    const emailLow = (email || '').toLowerCase();

    const all: PendingAccount[] = [
      ...this.readPendingAccounts(),
      ...this.readAdminUsers()
    ];

    for (const acc of all) {
      if (acc.pan?.toUpperCase() === panUp) {
        return { duplicate: true, message: 'An account with this PAN already exists.' };
      }
      if (acc.aadhar === aadharStr) {
        return { duplicate: true, message: 'An account with this Aadhar number already exists.' };
      }
      if (acc.email?.toLowerCase() === emailLow) {
        return { duplicate: true, message: 'An account with this Gmail already exists.' };
      }
    }
    return { duplicate: false };
  }

  onSubmit() {
    this.submitted = true;
    this.submitError = '';
    this.successMessage = '';

    if (this.form.invalid) {
      this.submitError = 'Please fix validation errors before submitting.';
      return;
    }

    const val = this.form.value;
    const dup = this.isDuplicate(val.pan, val.aadhar, val.email);
    if (dup.duplicate) {
      this.submitError = dup.message || 'Duplicate identifier found.';
      return;
    }

    const tempId = 'TMP-' + Date.now();
    const pending: PendingAccount = {
      id: tempId,
      name: val.name,
      email: val.email.toLowerCase(),
      dob: val.dob,
      income: Number(val.income),
      occupation: val.occupation,
      pan: val.pan.toUpperCase(),
      aadhar: val.aadhar,
      mobile: val.mobile,
      state: val.state,
      city: val.city,
      passwordHash: '***hashed***',
      status: 'PENDING',
      createdAt: new Date().toISOString(),
      assignedAccountNumber: null
    };

    const pendings = this.readPendingAccounts();
    pendings.push(pending);
    this.writePendingAccounts(pendings);

    const admins = this.readAdminUsers();
    admins.push(pending);
    this.writeAdminUsers(admins);

    this.successMessage = 'Account request submitted successfully. Admin will review and assign account number.';
    
    // Save user profile data for future use
    this.saveUserProfile(val);
    
    this.form.reset();
    this.submitted = false;
  }

  // Approval tracking methods
  toggleTracking() {
    this.showTracking = !this.showTracking;
    this.trackingResult = null;
    this.trackingError = '';
    this.trackingEmail = '';
    this.trackingMobile = '';
  }

  trackApproval() {
    if (!this.trackingEmail || !this.trackingMobile) {
      this.trackingError = 'Please enter both email and mobile number.';
      return;
    }

    const email = this.trackingEmail.toLowerCase();
    const mobile = this.trackingMobile;

    // Search in admin users (where all accounts are stored)
    const adminUsers = this.readAdminUsers();
    const match = adminUsers.find(acc => 
      acc.email.toLowerCase() === email && acc.mobile === mobile
    );

    if (match) {
      this.trackingResult = match;
      this.trackingError = '';
    } else {
      this.trackingError = 'No account found with the provided email and mobile number.';
      this.trackingResult = null;
    }
  }

  saveUserProfile(formData: any) {
    // Generate a temporary account number for the profile
    const tempAccountNumber = 'ACC' + Date.now().toString().slice(-6);
    
    const userProfile = {
      name: formData.name,
      email: formData.email.toLowerCase(),
      accountNumber: tempAccountNumber,
      phoneNumber: formData.mobile,
      address: `${formData.city}, ${formData.state}`,
      dateOfBirth: formData.dob,
      accountType: 'Savings Account',
      joinDate: new Date().toISOString().split('T')[0],
      pan: formData.pan.toUpperCase(),
      aadhar: formData.aadhar,
      occupation: formData.occupation,
      income: formData.income
    };

    // Save to localStorage
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('user_profile', JSON.stringify(userProfile));
    }
    
    // Also create initial transaction record
    const initialTransaction = {
      id: 'TXN' + Date.now(),
      merchant: 'Account Opening',
      amount: 0,
      type: 'Credit',
      balance: 0,
      date: new Date().toISOString(),
      description: 'Account opened successfully'
    };
    
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(`user_transactions_${tempAccountNumber}`, JSON.stringify([initialTransaction]));
    }
  }

  get f() { return this.form.controls; }
}
