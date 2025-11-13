import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { Router } from '@angular/router';

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
  loading = false;
  
  // Approval tracking
  showTracking = false;
  trackingEmail = '';
  trackingMobile = '';
  trackingResult: any = null;
  trackingError = '';

  // PAN regex: 5 letters, 4 digits, 1 letter
  private panRegex = /^[A-Z]{5}[0-9]{4}[A-Z]$/;
  // Aadhar: 12 digits, cannot start with 0 or 1
  private aadharRegex = /^[2-9]\d{11}$/;
  // Mobile: 10 digits starting 6-9
  private mobileRegex = /^[6-9]\d{9}$/;

  // Indian States and Union Territories
  indianStates = [
    'Andhra Pradesh',
    'Arunachal Pradesh',
    'Assam',
    'Bihar',
    'Chhattisgarh',
    'Goa',
    'Gujarat',
    'Haryana',
    'Himachal Pradesh',
    'Jharkhand',
    'Karnataka',
    'Kerala',
    'Madhya Pradesh',
    'Maharashtra',
    'Manipur',
    'Meghalaya',
    'Mizoram',
    'Nagaland',
    'Odisha',
    'Punjab',
    'Rajasthan',
    'Sikkim',
    'Tamil Nadu',
    'Telangana',
    'Tripura',
    'Uttar Pradesh',
    'Uttarakhand',
    'West Bengal',
    'Andaman and Nicobar Islands',
    'Chandigarh',
    'Dadra and Nagar Haveli and Daman and Diu',
    'Delhi',
    'Jammu and Kashmir',
    'Ladakh',
    'Lakshadweep',
    'Puducherry'
  ];

  // Cities data organized by state
  citiesByState: { [key: string]: string[] } = {
    'Andhra Pradesh': [
      'Visakhapatnam', 'Vijayawada', 'Guntur', 'Nellore', 'Kurnool', 
      'Tirupati', 'Rajahmundry', 'Kadapa', 'Anantapur', 'Chittoor',
      'Ongole', 'Eluru', 'Machilipatnam', 'Tenali', 'Proddatur'
    ],
    'Arunachal Pradesh': [
      'Itanagar', 'Naharlagun', 'Pasighat', 'Tezpur', 'Namsai',
      'Ziro', 'Along', 'Bomdila', 'Tezu', 'Seppa'
    ],
    'Assam': [
      'Guwahati', 'Silchar', 'Dibrugarh', 'Jorhat', 'Nagaon',
      'Tinsukia', 'Tezpur', 'Bongaigaon', 'Dhubri', 'Diphu'
    ],
    'Bihar': [
      'Patna', 'Gaya', 'Bhagalpur', 'Muzaffarpur', 'Darbhanga',
      'Purnia', 'Arrah', 'Begusarai', 'Katihar', 'Munger'
    ],
    'Chhattisgarh': [
      'Raipur', 'Bhilai', 'Bilaspur', 'Korba', 'Rajnandgaon',
      'Durg', 'Raigarh', 'Jagdalpur', 'Ambikapur', 'Chirmiri'
    ],
    'Goa': [
      'Panaji', 'Margao', 'Vasco da Gama', 'Mapusa', 'Ponda',
      'Sanquelim', 'Mormugao', 'Bicholim', 'Valpoi', 'Canacona'
    ],
    'Gujarat': [
      'Ahmedabad', 'Surat', 'Vadodara', 'Rajkot', 'Bhavnagar',
      'Jamnagar', 'Junagadh', 'Gandhinagar', 'Anand', 'Navsari'
    ],
    'Haryana': [
      'Gurgaon', 'Faridabad', 'Panipat', 'Ambala', 'Yamunanagar',
      'Rohtak', 'Hisar', 'Karnal', 'Sonipat', 'Panchkula'
    ],
    'Himachal Pradesh': [
      'Shimla', 'Dharamshala', 'Solan', 'Mandi', 'Palampur',
      'Kullu', 'Chamba', 'Una', 'Nahan', 'Baddi'
    ],
    'Jharkhand': [
      'Ranchi', 'Jamshedpur', 'Dhanbad', 'Bokaro', 'Deoghar',
      'Phusro', 'Hazaribagh', 'Giridih', 'Ramgarh', 'Medininagar'
    ],
    'Karnataka': [
      'Bangalore', 'Mysore', 'Hubli', 'Mangalore', 'Belgaum',
      'Gulbarga', 'Davanagere', 'Bellary', 'Bijapur', 'Shimoga'
    ],
    'Kerala': [
      'Thiruvananthapuram', 'Kochi', 'Kozhikode', 'Thrissur', 'Kollam',
      'Palakkad', 'Alappuzha', 'Malappuram', 'Kannur', 'Kasaragod'
    ],
    'Madhya Pradesh': [
      'Bhopal', 'Indore', 'Gwalior', 'Jabalpur', 'Ujjain',
      'Sagar', 'Dewas', 'Satna', 'Ratlam', 'Rewa'
    ],
    'Maharashtra': [
      'Mumbai', 'Pune', 'Nagpur', 'Thane', 'Nashik',
      'Aurangabad', 'Solapur', 'Amravati', 'Kolhapur', 'Sangli'
    ],
    'Manipur': [
      'Imphal', 'Thoubal', 'Bishnupur', 'Churachandpur', 'Senapati',
      'Tamenglong', 'Chandel', 'Ukhrul', 'Kangpokpi', 'Jiribam'
    ],
    'Meghalaya': [
      'Shillong', 'Tura', 'Jowai', 'Nongstoin', 'Baghmara',
      'Williamnagar', 'Resubelpara', 'Mairang', 'Mawkyrwat', 'Amlarem'
    ],
    'Mizoram': [
      'Aizawl', 'Lunglei', 'Saiha', 'Champhai', 'Kolasib',
      'Serchhip', 'Lawngtlai', 'Mamit', 'Saitual', 'Khawzawl'
    ],
    'Nagaland': [
      'Kohima', 'Dimapur', 'Mokokchung', 'Tuensang', 'Wokha',
      'Zunheboto', 'Phek', 'Mon', 'Longleng', 'Peren'
    ],
    'Odisha': [
      'Bhubaneswar', 'Cuttack', 'Rourkela', 'Berhampur', 'Sambalpur',
      'Puri', 'Balasore', 'Bhadrak', 'Baripada', 'Jharsuguda'
    ],
    'Punjab': [
      'Ludhiana', 'Amritsar', 'Jalandhar', 'Patiala', 'Bathinda',
      'Mohali', 'Firozpur', 'Batala', 'Pathankot', 'Moga'
    ],
    'Rajasthan': [
      'Jaipur', 'Jodhpur', 'Udaipur', 'Kota', 'Bikaner',
      'Ajmer', 'Bharatpur', 'Alwar', 'Sikar', 'Pali'
    ],
    'Sikkim': [
      'Gangtok', 'Namchi', 'Mangan', 'Gyalshing', 'Ravangla',
      'Singtam', 'Rangpo', 'Jorethang', 'Pakyong', 'Soreng'
    ],
    'Tamil Nadu': [
      'Chennai', 'Coimbatore', 'Madurai', 'Tiruchirappalli', 'Salem',
      'Tirunelveli', 'Tiruppur', 'Erode', 'Vellore', 'Thoothukkudi'
    ],
    'Telangana': [
      'Hyderabad', 'Warangal', 'Nizamabad', 'Khammam', 'Karimnagar',
      'Ramagundam', 'Mahbubnagar', 'Nalgonda', 'Adilabad', 'Suryapet'
    ],
    'Tripura': [
      'Agartala', 'Dharmanagar', 'Udaipur', 'Ambassa', 'Kailasahar',
      'Belonia', 'Khowai', 'Teliamura', 'Sabroom', 'Kamalpur'
    ],
    'Uttar Pradesh': [
      'Lucknow', 'Kanpur', 'Ghaziabad', 'Agra', 'Meerut',
      'Varanasi', 'Allahabad', 'Bareilly', 'Aligarh', 'Moradabad'
    ],
    'Uttarakhand': [
      'Dehradun', 'Haridwar', 'Roorkee', 'Rudrapur', 'Kashipur',
      'Haldwani', 'Rishikesh', 'Ramnagar', 'Pithoragarh', 'Srinagar'
    ],
    'West Bengal': [
      'Kolkata', 'Asansol', 'Siliguri', 'Durgapur', 'Bardhaman',
      'Malda', 'Bahraich', 'Habra', 'Kharagpur', 'Shantipur'
    ],
    'Andaman and Nicobar Islands': [
      'Port Blair', 'Diglipur', 'Mayabunder', 'Rangat', 'Car Nicobar'
    ],
    'Chandigarh': [
      'Chandigarh', 'Manimajra', 'Daria', 'Kaimbwala', 'Korakpur'
    ],
    'Dadra and Nagar Haveli and Daman and Diu': [
      'Silvassa', 'Daman', 'Diu', 'Naroli', 'Amli'
    ],
    'Delhi': [
      'New Delhi', 'Central Delhi', 'East Delhi', 'North Delhi', 'North East Delhi',
      'North West Delhi', 'Shahdara', 'South Delhi', 'South East Delhi', 'South West Delhi',
      'West Delhi'
    ],
    'Jammu and Kashmir': [
      'Srinagar', 'Jammu', 'Anantnag', 'Baramulla', 'Sopore',
      'Kathua', 'Udhampur', 'Rajauri', 'Poonch', 'Kupwara'
    ],
    'Ladakh': [
      'Leh', 'Kargil', 'Drass', 'Nubra', 'Zanskar'
    ],
    'Lakshadweep': [
      'Kavaratti', 'Agatti', 'Amini', 'Andrott', 'Bitra'
    ],
    'Puducherry': [
      'Puducherry', 'Karaikal', 'Mahe', 'Yanam'
    ]
  };

  // Available cities based on selected state
  availableCities: string[] = [];

  constructor(
    private fb: FormBuilder, 
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private router: Router
  ) {}

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
      city: [{value: '', disabled: true}, [Validators.required]], // Initially disabled
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordsMatchValidator });

    // Listen for state changes to update cities
    this.form.get('state')?.valueChanges.subscribe(selectedState => {
      this.onStateChange(selectedState);
    });
  }

  // Handle state selection change
  onStateChange(selectedState: string) {
    if (selectedState && this.citiesByState[selectedState]) {
      this.availableCities = this.citiesByState[selectedState];
      // Reset city selection when state changes
      this.form.get('city')?.setValue('');
      // Enable city dropdown when cities are available
      this.form.get('city')?.enable();
    } else {
      this.availableCities = [];
      this.form.get('city')?.setValue('');
      // Disable city dropdown when no cities are available
      this.form.get('city')?.disable();
    }
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
    
    // Basic format check
    if (!this.aadharRegex.test(val)) {
      console.log('Aadhaar format check failed for:', val);
      return { invalidAadhar: true };
    }
    
    // Mathematical validation using Verhoeff algorithm
    const isValid = this.isValidAadhaarNumber(val);
    console.log('Aadhaar checksum validation for', val, ':', isValid);
    
    if (!isValid) {
      return { invalidAadharChecksum: true };
    }
    
    return null;
  }

  // Verhoeff algorithm for Aadhaar validation
  private isValidAadhaarNumber(aadhaar: string): boolean {
    // Remove any spaces or special characters
    const cleanAadhaar = aadhaar.replace(/\D/g, '');
    
    // Check if it's exactly 12 digits
    if (cleanAadhaar.length !== 12) {
      return false;
    }
    
    // Verhoeff algorithm implementation
    const multiplication = [
      [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
      [1, 2, 3, 4, 0, 6, 7, 8, 9, 5],
      [2, 3, 4, 0, 1, 7, 8, 9, 5, 6],
      [3, 4, 0, 1, 2, 8, 9, 5, 6, 7],
      [4, 0, 1, 2, 3, 9, 5, 6, 7, 8],
      [5, 9, 8, 7, 6, 0, 4, 3, 2, 1],
      [6, 5, 9, 8, 7, 1, 0, 4, 3, 2],
      [7, 6, 5, 9, 8, 2, 1, 0, 4, 3],
      [8, 7, 6, 5, 9, 3, 2, 1, 0, 4],
      [9, 8, 7, 6, 5, 4, 3, 2, 1, 0]
    ];
    
    const permutation = [
      [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
      [1, 5, 7, 6, 2, 8, 3, 0, 9, 4],
      [5, 8, 0, 3, 7, 9, 6, 1, 4, 2],
      [8, 9, 1, 6, 0, 4, 3, 5, 2, 7],
      [9, 4, 5, 3, 1, 2, 6, 8, 7, 0],
      [4, 2, 8, 6, 5, 7, 3, 9, 0, 1],
      [2, 7, 9, 3, 8, 0, 6, 4, 1, 5],
      [7, 0, 4, 6, 9, 1, 3, 2, 5, 8]
    ];
    
    let checksum = 0;
    const digits = cleanAadhaar.split('').map(Number);
    
    // Process digits from right to left
    for (let i = 0; i < digits.length; i++) {
      checksum = multiplication[checksum][permutation[i % 8][digits[digits.length - 1 - i]]];
    }
    
    return checksum === 0;
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
    this.loading = true;

    if (this.form.invalid) {
      this.submitError = 'Please fix validation errors before submitting.';
      this.loading = false;
      return;
    }

    const val = this.form.value;
    
    // Create user object for backend with proper structure
    const userData = {
      username: val.email.toLowerCase(), // Use email as username
      email: val.email.toLowerCase(),
      password: val.password,
      status: 'PENDING',
      account: {
        name: val.name,
        dob: val.dob,
        age: new Date().getFullYear() - new Date(val.dob).getFullYear(),
        occupation: val.occupation,
        accountType: 'Savings',
        aadharNumber: val.aadhar,
        pan: val.pan.toUpperCase(),
        income: Number(val.income),
        phone: val.mobile,
        address: `${val.city}, ${val.state}`,
        balance: 0.0,
        verifiedMatrix: false,
        kycVerified: false
      }
    };

    // Submit to backend using HttpClient directly
    this.http.post('${environment.apiUrl}/users/create', userData).subscribe({
      next: (response: any) => {
        console.log('User created successfully in MySQL:', response);
        
        if (response.success) {
          this.successMessage = response.message || 'Account request submitted successfully! Admin will review and approve your account.';
          this.form.reset();
          this.submitted = false;
          this.loading = false;
          
          // Also save to localStorage as backup
          this.submitToLocalStorage(val);
        } else {
          this.submitError = response.message || 'Failed to submit account request. Please try again.';
          this.loading = false;
        }
      },
      error: (err: any) => {
        console.error('Error creating user:', err);
        
        // Try to parse error response
        if (err.error && err.error.message) {
          this.submitError = err.error.message;
        } else {
          this.submitError = 'Failed to submit account request. Please try again.';
        }
        this.loading = false;
        
        // Fallback to localStorage
        this.submitToLocalStorage(val);
      }
    });
  }

  submitToLocalStorage(val: any) {
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
    this.loading = false;
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

  goBack() {
    this.router.navigate(['/website/landing']);
  }
}
