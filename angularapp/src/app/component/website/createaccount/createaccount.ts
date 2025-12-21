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
  childForm!: FormGroup;
  submitted = false;
  submitError = '';
  successMessage = '';
  loading = false;
  termsAccepted = false;
  hasChildAccount = false;
  childAgeError = '';
  
  // Approval tracking
  showTracking = false;
  trackingAadhar = '';
  trackingMobile = '';
  trackingResult: any = null;
  trackingError = '';
  trackingLoading = false;

  // Validation status for uniqueness checks
  aadharChecking = false;
  aadharExists = false;
  panChecking = false;
  panExists = false;
  mobileChecking = false;
  mobileExists = false;

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
      confirmPassword: ['', [Validators.required]],
      termsAccepted: [false, [Validators.requiredTrue]],
      // Child account fields
      childName: [''],
      childEmail: [''],
      childDob: [''],
      childStudies: [''],
      form60: [''],
      panChildForm: [''],
      childPassword: [''],
      confirmChildPassword: ['']
    }, { validators: this.passwordsMatchValidator });

    // Child form for validation
    this.childForm = this.fb.group({
      childPassword: [''],
      confirmChildPassword: ['']
    }, { validators: this.childPasswordsMatchValidator });

    // Listen for state changes to update cities
    this.form.get('state')?.valueChanges.subscribe(selectedState => {
      this.onStateChange(selectedState);
    });

    // Listen for Aadhar changes to check uniqueness
    this.form.get('aadhar')?.valueChanges.subscribe(aadhar => {
      if (aadhar && this.form.get('aadhar')?.valid && !this.form.get('aadhar')?.hasError('invalidAadhar') && !this.form.get('aadhar')?.hasError('invalidAadharChecksum')) {
        this.checkAadharUniqueness(aadhar);
      } else {
        this.aadharExists = false;
      }
    });

    // Listen for PAN changes to check uniqueness
    this.form.get('pan')?.valueChanges.subscribe(pan => {
      if (pan && this.form.get('pan')?.valid && !this.form.get('pan')?.hasError('invalidPan')) {
        this.checkPanUniqueness(pan.toUpperCase());
      } else {
        this.panExists = false;
      }
    });

    // Listen for Mobile changes to check uniqueness
    this.form.get('mobile')?.valueChanges.subscribe(mobile => {
      if (mobile && this.form.get('mobile')?.valid && !this.form.get('mobile')?.hasError('invalidMobile')) {
        this.checkMobileUniqueness(mobile);
      } else {
        this.mobileExists = false;
      }
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

  childPasswordsMatchValidator(group: AbstractControl): ValidationErrors | null {
    const pw = group.get('childPassword')?.value;
    const cpw = group.get('confirmChildPassword')?.value;
    return pw && cpw && pw !== cpw ? { childPasswordsMismatch: true } : null;
  }

  onChildAccountToggle() {
    if (this.hasChildAccount) {
      // Add validators when child account is enabled
      this.form.get('childName')?.setValidators([Validators.required, Validators.minLength(2)]);
      this.form.get('childEmail')?.setValidators([Validators.required, Validators.email, this.gmailOnlyValidator]);
      this.form.get('childDob')?.setValidators([Validators.required, this.childAgeValidator.bind(this)]);
      this.form.get('childStudies')?.setValidators([Validators.required]);
      this.form.get('form60')?.setValidators([Validators.required]);
      this.form.get('panChildForm')?.setValidators([Validators.required]);
      this.form.get('childPassword')?.setValidators([Validators.required, Validators.minLength(6)]);
      this.form.get('confirmChildPassword')?.setValidators([Validators.required]);
    } else {
      // Remove validators when child account is disabled
      this.form.get('childName')?.clearValidators();
      this.form.get('childEmail')?.clearValidators();
      this.form.get('childDob')?.clearValidators();
      this.form.get('childStudies')?.clearValidators();
      this.form.get('form60')?.clearValidators();
      this.form.get('panChildForm')?.clearValidators();
      this.form.get('childPassword')?.clearValidators();
      this.form.get('confirmChildPassword')?.clearValidators();
      // Clear values
      this.form.get('childName')?.setValue('');
      this.form.get('childEmail')?.setValue('');
      this.form.get('childDob')?.setValue('');
      this.form.get('childStudies')?.setValue('');
      this.form.get('form60')?.setValue('');
      this.form.get('panChildForm')?.setValue('');
      this.form.get('childPassword')?.setValue('');
      this.form.get('confirmChildPassword')?.setValue('');
      this.childAgeError = '';
    }
    this.form.get('childName')?.updateValueAndValidity();
    this.form.get('childEmail')?.updateValueAndValidity();
    this.form.get('childDob')?.updateValueAndValidity();
    this.form.get('childStudies')?.updateValueAndValidity();
    this.form.get('form60')?.updateValueAndValidity();
    this.form.get('panChildForm')?.updateValueAndValidity();
    this.form.get('childPassword')?.updateValueAndValidity();
    this.form.get('confirmChildPassword')?.updateValueAndValidity();
  }

  childAgeValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    const dob = new Date(control.value);
    const today = new Date();
    const age = today.getFullYear() - dob.getFullYear();
    const monthDiff = today.getMonth() - dob.getMonth();
    const dayDiff = today.getDate() - dob.getDate();
    
    const actualAge = monthDiff < 0 || (monthDiff === 0 && dayDiff < 0) ? age - 1 : age;
    
    if (actualAge >= 18) {
      return { ageRestriction: true };
    }
    return null;
  }

  validateChildAge() {
    const childDob = this.form.get('childDob')?.value;
    if (!childDob) {
      this.childAgeError = '';
      return;
    }
    
    const dob = new Date(childDob);
    const today = new Date();
    const age = today.getFullYear() - dob.getFullYear();
    const monthDiff = today.getMonth() - dob.getMonth();
    const dayDiff = today.getDate() - dob.getDate();
    
    const actualAge = monthDiff < 0 || (monthDiff === 0 && dayDiff < 0) ? age - 1 : age;
    
    if (actualAge >= 18) {
      this.childAgeError = 'Child must be below 18 years of age.';
      this.form.get('childDob')?.setErrors({ ageRestriction: true });
    } else {
      this.childAgeError = '';
      if (this.form.get('childDob')?.hasError('ageRestriction')) {
        this.form.get('childDob')?.setErrors(null);
      }
    }
  }

  // Check Aadhar uniqueness
  checkAadharUniqueness(aadhar: string) {
    if (!aadhar || aadhar.length !== 12) {
      this.aadharExists = false;
      return;
    }

    this.aadharChecking = true;
    this.aadharExists = false;

    // Debounce: wait 500ms before making API call
    setTimeout(() => {
      if (this.form.get('aadhar')?.value === aadhar) {
        this.http.get<{isUnique: boolean}>(`${environment.apiBaseUrl}/accounts/validate/aadhar/${aadhar}`).subscribe({
          next: (response) => {
            this.aadharExists = !response.isUnique;
            this.aadharChecking = false;
          },
          error: (err) => {
            console.error('Error checking Aadhar uniqueness:', err);
            this.aadharChecking = false;
          }
        });
      } else {
        this.aadharChecking = false;
      }
    }, 500);
  }

  // Check PAN uniqueness
  checkPanUniqueness(pan: string) {
    if (!pan || pan.length !== 10) {
      this.panExists = false;
      return;
    }

    this.panChecking = true;
    this.panExists = false;

    // Debounce: wait 500ms before making API call
    setTimeout(() => {
      if (this.form.get('pan')?.value?.toUpperCase() === pan) {
        this.http.get<{isUnique: boolean}>(`${environment.apiBaseUrl}/accounts/validate/pan/${pan}`).subscribe({
          next: (response) => {
            this.panExists = !response.isUnique;
            this.panChecking = false;
          },
          error: (err) => {
            console.error('Error checking PAN uniqueness:', err);
            this.panChecking = false;
          }
        });
      } else {
        this.panChecking = false;
      }
    }, 500);
  }

  // Check Mobile uniqueness
  checkMobileUniqueness(mobile: string) {
    if (!mobile || mobile.length !== 10) {
      this.mobileExists = false;
      return;
    }

    this.mobileChecking = true;
    this.mobileExists = false;

    // Debounce: wait 500ms before making API call
    setTimeout(() => {
      if (this.form.get('mobile')?.value === mobile) {
        this.http.get<{isUnique: boolean}>(`${environment.apiBaseUrl}/accounts/validate/phone/${mobile}`).subscribe({
          next: (response) => {
            this.mobileExists = !response.isUnique;
            this.mobileChecking = false;
          },
          error: (err) => {
            console.error('Error checking Mobile uniqueness:', err);
            this.mobileChecking = false;
          }
        });
      } else {
        this.mobileChecking = false;
      }
    }, 500);
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
    
    // Clear any previous validation errors
    this.aadharExists = false;
    this.panExists = false;
    this.mobileExists = false;

    // Check terms acceptance
    if (!this.form.get('termsAccepted')?.value) {
      this.submitError = 'Please accept the Terms and Conditions to proceed.';
      this.loading = false;
      return;
    }

    if (this.form.invalid) {
      this.submitError = 'Please fix validation errors before submitting.';
      this.loading = false;
      return;
    }

    // Check for duplicate fields before submitting
    if (this.aadharExists) {
      this.submitError = 'Aadhar number is already registered. Another account exists with this Aadhar number.';
      this.loading = false;
      return;
    }

    if (this.panExists) {
      this.submitError = 'PAN number is already registered. Another account exists with this PAN number.';
      this.loading = false;
      return;
    }

    if (this.mobileExists) {
      this.submitError = 'Mobile number is already registered. Another account exists with this mobile number.';
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
    this.http.post(`${environment.apiBaseUrl}/api/users/create`, userData).subscribe({
      next: (response: any) => {
        console.log('User created successfully in MySQL:', response);
        
        if (response.success) {
          // Clear any previous errors
          this.submitError = '';
          this.successMessage = response.message || 'Account request submitted successfully. Admin will review and assign account number.';
          this.form.reset();
          this.submitted = false;
          this.loading = false;
          
          // Clear validation flags
          this.aadharExists = false;
          this.panExists = false;
          this.mobileExists = false;
          
          // Also save to localStorage as backup
          this.submitToLocalStorage(val);
        } else {
          // Clear success message if there's an error
          this.successMessage = '';
          this.submitError = response.message || 'Failed to submit account request. Please try again.';
          this.loading = false;
        }
      },
      error: (err: any) => {
        console.error('Error creating user:', err);
        
        // Clear success message on error
        this.successMessage = '';
        
        // Try to parse error response
        if (err.error) {
          if (err.error.message) {
            this.submitError = err.error.message;
          } else if (err.error.error) {
            this.submitError = err.error.error;
          } else {
            this.submitError = 'Account creation failed. Please try again.';
          }
          
          // Check for specific error types
          if (err.error.errorType === 'AADHAR_EXISTS') {
            this.aadharExists = true;
            this.submitError = 'Aadhar number is already registered. Another account exists with this Aadhar number.';
          } else if (err.error.errorType === 'PAN_EXISTS') {
            this.panExists = true;
            this.submitError = 'PAN number is already registered. Another account exists with this PAN number.';
          } else if (err.error.errorType === 'PHONE_EXISTS') {
            this.mobileExists = true;
            this.submitError = 'Mobile number is already registered. Another account exists with this mobile number.';
          } else if (err.error.errorType === 'EMAIL_EXISTS') {
            this.submitError = 'Email address is already registered. Please use a different email or try logging in.';
          }
        } else {
          this.submitError = 'Account creation failed. Please try again.';
        }
        this.loading = false;
        
        // Don't fallback to localStorage on error - only show error message
        // this.submitToLocalStorage(val);
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
    this.trackingAadhar = '';
    this.trackingMobile = '';
  }

  filterDigits(type: 'aadhar' | 'mobile', event: Event) {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/\D/g, '');
    if (type === 'aadhar') {
      this.trackingAadhar = value;
    } else {
      this.trackingMobile = value;
    }
  }

  trackApproval() {
    if (!this.trackingAadhar || !this.trackingMobile) {
      this.trackingError = 'Please enter both Aadhar number and mobile number.';
      return;
    }

    if (this.trackingAadhar.length !== 12) {
      this.trackingError = 'Aadhar number must be 12 digits.';
      return;
    }

    if (this.trackingMobile.length !== 10) {
      this.trackingError = 'Mobile number must be 10 digits.';
      return;
    }

    this.trackingLoading = true;
    this.trackingError = '';
    this.trackingResult = null;

    // Call backend API to get tracking status
    this.http.get(`${environment.apiBaseUrl}/tracking/track?aadharNumber=${this.trackingAadhar}&mobileNumber=${this.trackingMobile}`).subscribe({
      next: (response: any) => {
        this.trackingLoading = false;
        if (response.success && response.tracking) {
          this.trackingResult = response.tracking;
          this.trackingError = '';
        } else {
          this.trackingError = 'No tracking record found with the provided Aadhar number and mobile number.';
          this.trackingResult = null;
        }
      },
      error: (err: any) => {
        this.trackingLoading = false;
        console.error('Error tracking approval:', err);
        if (err.status === 404) {
          this.trackingError = 'No tracking record found. Please verify your Aadhar number and mobile number.';
        } else {
          this.trackingError = err.error?.message || 'Failed to retrieve tracking information. Please try again.';
        }
        this.trackingResult = null;
      }
    });
  }

  getTrackingStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'ADMIN_SEEN':
        return 'status-seen';
      case 'ADMIN_APPROVED':
        return 'status-approved';
      case 'ADMIN_SENT':
        return 'status-sent';
      default:
        return 'status-default';
    }
  }

  getTrackingStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'Pending';
      case 'ADMIN_SEEN':
        return 'Admin Seen';
      case 'ADMIN_APPROVED':
        return 'Approved';
      case 'ADMIN_SENT':
        return 'Sent';
      default:
        return status;
    }
  }

  getTrackingStatusIcon(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'fa-clock';
      case 'ADMIN_SEEN':
        return 'fa-eye';
      case 'ADMIN_APPROVED':
        return 'fa-check-circle';
      case 'ADMIN_SENT':
        return 'fa-paper-plane';
      default:
        return 'fa-info-circle';
    }
  }

  getTrackingStatusMessage(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'Your account creation request is pending admin review. We will notify you once it is reviewed.';
      case 'ADMIN_SEEN':
        return 'Your account creation request has been seen by admin and is under review.';
      case 'ADMIN_APPROVED':
        return 'Congratulations! Your account has been approved by admin. You will receive your account details soon.';
      case 'ADMIN_SENT':
        return 'Your account details have been sent. Please check your email for further instructions.';
      default:
        return 'Your account status: ' + status;
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

  showTerms(event: Event) {
    event.preventDefault();
    const termsText = `
TERMS AND CONDITIONS

1. Account Opening:
   - By creating an account, you agree to provide accurate and complete information.
   - You must be at least 18 years old to open an account.
   - All documents submitted must be genuine and valid.

2. Account Usage:
   - You are responsible for maintaining the confidentiality of your account credentials.
   - You must notify us immediately of any unauthorized access to your account.
   - You agree to use the account only for lawful purposes.

3. Fees and Charges:
   - Account maintenance fees may apply as per our schedule of charges.
   - Transaction fees may be charged for certain services.
   - All fees will be clearly communicated before they are charged.

4. Account Closure:
   - You may close your account at any time by submitting a written request.
   - We reserve the right to close your account if you violate these terms.

5. Privacy:
   - We collect and use your personal information as described in our Privacy Policy.
   - Your data will be protected in accordance with applicable laws.

6. Limitation of Liability:
   - NeoBank shall not be liable for any indirect, incidental, or consequential damages.
   - Our liability is limited to the extent permitted by law.

By accepting these terms, you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions.
    `;
    alert(termsText);
  }

  showPrivacy(event: Event) {
    event.preventDefault();
    const privacyText = `
PRIVACY POLICY

1. Information Collection:
   - We collect personal information including name, email, phone, Aadhar, PAN, and financial details.
   - This information is collected to provide banking services and comply with regulatory requirements.

2. Information Use:
   - Your information is used to process account applications, provide services, and comply with legal obligations.
   - We may use your information for marketing purposes with your consent.

3. Information Sharing:
   - We do not sell your personal information to third parties.
   - We may share information with regulatory authorities as required by law.
   - Information may be shared with service providers who assist in our operations.

4. Data Security:
   - We implement appropriate security measures to protect your information.
   - However, no method of transmission over the internet is 100% secure.

5. Your Rights:
   - You have the right to access, correct, or delete your personal information.
   - You can opt-out of marketing communications at any time.

6. Contact:
   - For privacy concerns, contact us at privacy@neobank.com
    `;
    alert(privacyText);
  }

  goBack() {
    this.router.navigate(['/website/landing']);
  }
}
