import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { Router } from '@angular/router';
import { VideoKycService } from '../../../service/video-kyc.service';
import { CurrentAccountService } from '../../../service/current-account.service';
import { SalaryAccountService } from '../../../service/salary-account.service';

interface PendingAccount {
  id: string;
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
  passwordHash: string;
  status: string;
  createdAt: string;
  assignedAccountNumber: string | null;
}

@Component({
  selector: 'app-createaccount',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './createaccount.html',
  styleUrls: ['./createaccount.css']
})
export class Createaccount implements OnInit, OnDestroy {
  @ViewChild('localVideo') localVideoRef!: ElementRef<HTMLVideoElement>;

  form!: FormGroup;
  childForm!: FormGroup;
  submitted = false;
  submitError = '';
  successMessage = '';
  loading = false;
  loadingMessage = 'Processing...';
  termsAccepted = false;
  hasChildAccount = false;
  childAgeError = '';

  // Step control
  currentStep = 1;
  kycSessionData: any = null;

  // Approval tracking
  showTracking = false;
  trackingMobile = '';
  trackingResult: any = null;
  trackingError = '';
  trackingLoading = false;

  // Document upload
  aadharFile: File | null = null;
  panFile: File | null = null;
  aadharPreview: string | null = null;
  panPreview: string | null = null;
  uploading = false;
  uploadError = '';

  // Video KYC
  videoStarted = false;
  startingVideo = false;
  connectionStatus = 'waiting';
  kycOtp = '';
  otpVerifiedFlag = false;
  verifyingOtp = false;
  faceSnapshotTaken = false;
  idSnapshotTaken = false;
  livenessChecked = false;
  private mediaStream: MediaStream | null = null;

  // Slot Booking
  availableSlots: any[] = [];
  filteredSlots: any[] = [];
  availableDates: string[] = [];
  selectedSlotDate: string = '';
  selectedSlot: any = null;
  bookingSlot = false;
  slotBookingError = '';
  canJoinNow = false;
  countdownDisplay = '';
  countdownParts = { hours: '00', minutes: '00', seconds: '00' };
  private countdownInterval: any = null;

  // Tracking tab
  trackingTab: string = 'mobile';
  trackingVerificationNumber: string = '';

  // Account type specific
  selectedAccountType: string = 'Savings';

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
    private router: Router,
    private videoKycService: VideoKycService,
    private currentAccountService: CurrentAccountService,
    private salaryAccountService: SalaryAccountService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      accountType: ['Savings'],
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
      // Current Account fields
      businessName: [''],
      businessType: ['Proprietor'],
      businessRegistrationNumber: [''],
      gstNumber: [''],
      shopAddress: [''],
      pincode: [''],
      branchName: [''],
      ifscCode: [''],
      // Salary Account fields
      companyName: [''],
      companyId: [''],
      employerAddress: [''],
      hrContactNumber: [''],
      monthlySalary: [''],
      salaryCreditDate: [''],
      designation: [''],
      salaryBranchName: [''],
      salaryIfscCode: [''],
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

    // Listen for account type changes
    this.form.get('accountType')?.valueChanges.subscribe(type => {
      this.selectedAccountType = type;
      this.updateAccountTypeValidators(type);
    });

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

  // Update validators based on account type
  updateAccountTypeValidators(type: string) {
    // Current Account fields
    const currentFields = ['businessName', 'businessType'];
    // Salary Account fields
    const salaryFields = ['companyName', 'monthlySalary', 'salaryCreditDate'];
    // Common fields that change based on type
    const occupationCtrl = this.form.get('occupation');

    // Clear all conditional validators first
    [...currentFields, ...salaryFields].forEach(field => {
      this.form.get(field)?.clearValidators();
      this.form.get(field)?.updateValueAndValidity();
    });

    if (type === 'Current') {
      // Business Name and Type required for current account
      this.form.get('businessName')?.setValidators([Validators.required]);
      this.form.get('businessType')?.setValidators([Validators.required]);
      // Occupation not required for current account (business-focused)
      occupationCtrl?.clearValidators();
      occupationCtrl?.updateValueAndValidity();
    } else if (type === 'Salary') {
      // Company and salary details required for salary account
      this.form.get('companyName')?.setValidators([Validators.required]);
      this.form.get('monthlySalary')?.setValidators([Validators.required, Validators.min(1)]);
      this.form.get('salaryCreditDate')?.setValidators([Validators.required, Validators.min(1), Validators.max(28)]);
      // Occupation not required (uses designation)
      occupationCtrl?.clearValidators();
      occupationCtrl?.updateValueAndValidity();
    } else {
      // Savings: occupation required
      occupationCtrl?.setValidators([Validators.required]);
      occupationCtrl?.updateValueAndValidity();
    }

    // Update validity for all conditional fields
    [...currentFields, ...salaryFields].forEach(field => {
      this.form.get(field)?.updateValueAndValidity();
    });
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
        this.http.get<{isUnique: boolean}>(`${environment.apiBaseUrl}/api/accounts/validate/aadhar/${aadhar}`).subscribe({
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
        this.http.get<{isUnique: boolean}>(`${environment.apiBaseUrl}/api/accounts/validate/pan/${pan}`).subscribe({
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
        this.http.get<{isUnique: boolean}>(`${environment.apiBaseUrl}/api/accounts/validate/phone/${mobile}`).subscribe({
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
    this.loadingMessage = 'Registering your application...';
    
    this.aadharExists = false;
    this.panExists = false;
    this.mobileExists = false;

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

    if (this.aadharExists || this.panExists || this.mobileExists) {
      this.submitError = 'Some fields are already registered. Please check highlighted errors.';
      this.loading = false;
      return;
    }

    const val = this.form.value;
    
    const registrationData = {
      fullName: val.name,
      mobileNumber: val.mobile,
      email: val.email.toLowerCase(),
      addressCity: val.city,
      addressState: val.state,
      accountType: val.accountType || 'Savings'
    };

    this.videoKycService.register(registrationData).subscribe({
      next: (response: any) => {
        this.loading = false;
        this.kycSessionData = response;
        this.kycSessionData.id = response.sessionId;
        this.successMessage = 'Registration successful! Please proceed to upload your documents.';
        this.submitted = false;
        
        // Also submit user to backend for account creation tracking
        this.submitUserToBackend(val);
        
        // Move to step 2 after a short delay
        setTimeout(() => {
          this.successMessage = '';
          this.currentStep = 2;
        }, 1500);
      },
      error: (err: any) => {
        this.loading = false;
        this.submitError = err.error?.message || err.error?.error || 'Registration failed. Please try again.';
      }
    });
  }

  private submitUserToBackend(val: any) {
    const accountType = val.accountType || 'Savings';

    if (accountType === 'Current') {
      this.submitCurrentAccount(val);
    } else if (accountType === 'Salary') {
      this.submitSalaryAccount(val);
    } else {
      this.submitSavingsAccount(val);
    }
  }

  private submitSavingsAccount(val: any) {
    const userData: any = {
      username: val.email.toLowerCase(),
      email: val.email.toLowerCase(),
      password: val.password,
      status: 'PENDING',
      account: {
        name: val.name,
        dob: val.dob,
        age: new Date().getFullYear() - new Date(val.dob).getFullYear(),
        occupation: val.occupation || '',
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

    this.http.post(`${environment.apiBaseUrl}/api/users/create`, userData).subscribe({
      next: (response: any) => {
        console.log('Savings account created in backend:', response);
        this.submitToLocalStorage(val);
      },
      error: (err: any) => {
        console.warn('Backend user creation failed (Video KYC will continue):', err);
        this.submitToLocalStorage(val);
      }
    });
  }

  private submitCurrentAccount(val: any) {
    const currentAccountData: any = {
      businessName: val.businessName,
      businessType: val.businessType,
      businessRegistrationNumber: val.businessRegistrationNumber || '',
      gstNumber: val.gstNumber || '',
      ownerName: val.name,
      mobile: val.mobile,
      email: val.email.toLowerCase(),
      aadharNumber: val.aadhar,
      panNumber: val.pan.toUpperCase(),
      shopAddress: val.shopAddress || '',
      city: val.city || '',
      state: val.state || '',
      pincode: val.pincode || '',
      branchName: val.branchName || 'NeoBank Main Branch',
      ifscCode: val.ifscCode || 'EZYV000123',
      password: val.password,
      status: 'PENDING'
    };

    this.currentAccountService.createAccount(currentAccountData).subscribe({
      next: (response: any) => {
        console.log('Current account created in backend:', response);
        this.submitToLocalStorage(val);
      },
      error: (err: any) => {
        console.warn('Backend current account creation failed (Video KYC will continue):', err);
        this.submitToLocalStorage(val);
      }
    });
  }

  private submitSalaryAccount(val: any) {
    const salaryAccountData: any = {
      employeeName: val.name,
      dob: val.dob,
      mobileNumber: val.mobile,
      email: val.email.toLowerCase(),
      aadharNumber: val.aadhar,
      panNumber: val.pan.toUpperCase(),
      companyName: val.companyName,
      companyId: val.companyId || '',
      employerAddress: val.employerAddress || '',
      hrContactNumber: val.hrContactNumber || '',
      monthlySalary: Number(val.monthlySalary),
      salaryCreditDate: Number(val.salaryCreditDate),
      designation: val.designation || '',
      branchName: val.salaryBranchName || 'NeoBank Main Branch',
      ifscCode: val.salaryIfscCode || 'EZYV000123',
      address: `${val.city || ''}, ${val.state || ''}`,
      password: val.password,
      status: 'Active'
    };

    this.salaryAccountService.createAccount(salaryAccountData).subscribe({
      next: (response: any) => {
        console.log('Salary account created in backend:', response);
        this.submitToLocalStorage(val);
      },
      error: (err: any) => {
        console.warn('Backend salary account creation failed (Video KYC will continue):', err);
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
    this.trackingMobile = '';
  }

  filterDigits(type: 'aadhar' | 'mobile', event: Event) {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/\D/g, '');
    this.trackingMobile = value;
  }

  trackVideoKycStatus() {
    this.trackingLoading = true;
    this.trackingError = '';
    this.trackingResult = null;

    if (this.trackingTab === 'verification') {
      if (!this.trackingVerificationNumber || this.trackingVerificationNumber.length < 5) {
        this.trackingError = 'Please enter a valid verification number.';
        this.trackingLoading = false;
        return;
      }

      this.videoKycService.verifyByNumber(this.trackingVerificationNumber).subscribe({
        next: (response: any) => {
          this.trackingLoading = false;
          if (response && response.found) {
            this.trackingResult = response;
          } else {
            this.trackingError = 'No session found with this verification number.';
          }
        },
        error: (err: any) => {
          this.trackingLoading = false;
          this.trackingError = err.error?.message || 'Failed to look up verification number.';
        }
      });
      return;
    }

    if (!this.trackingMobile || this.trackingMobile.length !== 10) {
      this.trackingError = 'Please enter a valid 10-digit mobile number.';
      this.trackingLoading = false;
      return;
    }

    this.videoKycService.getStatusByMobile(this.trackingMobile).subscribe({
      next: (response: any) => {
        this.trackingLoading = false;
        if (response) {
          this.trackingResult = response;
        } else {
          this.trackingError = 'No Video KYC session found for this mobile number.';
        }
      },
      error: (err: any) => {
        this.trackingLoading = false;
        if (err.status === 404) {
          this.trackingError = 'No Video KYC session found for this mobile number.';
        } else {
          this.trackingError = err.error?.message || 'Failed to retrieve status. Please try again.';
        }
      }
    });
  }

  resumeSession(session: any) {
    this.kycSessionData = session;
    this.kycSessionData.id = session.sessionId || session.id;
    this.showTracking = false;
    
    switch (session.kycStatus) {
      case 'Pending':
      case 'Registered':
        this.currentStep = 2;
        break;
      case 'Documents Uploaded':
        this.currentStep = 3;
        this.loadAvailableSlots();
        break;
      case 'Scheduled':
        this.currentStep = 3;
        this.loadAvailableSlots();
        this.startCountdown();
        break;
      case 'Under Review':
      case 'Submitted':
      case 'Approved':
      case 'Rejected':
        this.currentStep = 5;
        break;
      default:
        this.currentStep = 2;
    }
  }

  // Step navigation
  goToStep(step: number) {
    if (this.canGoToStep(step)) {
      this.submitError = '';
      this.successMessage = '';
      this.currentStep = step;
      if (step === 3) {
        this.loadAvailableSlots();
        if (this.kycSessionData?.bookedSlotId) {
          this.startCountdown();
        }
      }
    }
  }

  canGoToStep(step: number): boolean {
    if (!this.kycSessionData) return step === 1;
    if (step === 1) return true;
    if (step === 2) return !!this.kycSessionData;
    if (step === 3) return this.kycSessionData?.kycStatus !== 'Registered';
    if (step === 4) return ['Scheduled', 'Under Review', 'Submitted', 'Approved', 'Rejected'].includes(this.kycSessionData?.kycStatus);
    if (step === 5) return ['Under Review', 'Submitted', 'Approved', 'Rejected', 'Scheduled'].includes(this.kycSessionData?.kycStatus);
    return false;
  }

  // Document upload methods
  onAadharFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      if (file.size > 5 * 1024 * 1024) {
        this.uploadError = 'Aadhaar file size must be less than 5MB.';
        return;
      }
      this.aadharFile = file;
      this.uploadError = '';
      if (file.type === 'application/pdf') {
        this.aadharPreview = 'pdf';
      } else {
        const reader = new FileReader();
        reader.onload = (e) => this.aadharPreview = e.target?.result as string;
        reader.readAsDataURL(file);
      }
    }
  }

  onPanFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      if (file.size > 5 * 1024 * 1024) {
        this.uploadError = 'PAN file size must be less than 5MB.';
        return;
      }
      this.panFile = file;
      this.uploadError = '';
      if (file.type === 'application/pdf') {
        this.panPreview = 'pdf';
      } else {
        const reader = new FileReader();
        reader.onload = (e) => this.panPreview = e.target?.result as string;
        reader.readAsDataURL(file);
      }
    }
  }

  removeFile(type: string, event: Event) {
    event.stopPropagation();
    if (type === 'aadhar') {
      this.aadharFile = null;
      this.aadharPreview = null;
    } else {
      this.panFile = null;
      this.panPreview = null;
    }
  }

  uploadDocuments() {
    if (!this.aadharFile || !this.panFile || !this.kycSessionData?.id) return;
    
    this.uploading = true;
    this.uploadError = '';

    this.videoKycService.uploadDocuments(this.kycSessionData.id, this.aadharFile, this.panFile).subscribe({
      next: (response: any) => {
        this.uploading = false;
        this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
        this.successMessage = 'Documents uploaded successfully!';
        setTimeout(() => {
          this.successMessage = '';
          this.currentStep = 3;
        }, 1000);
      },
      error: (err: any) => {
        this.uploading = false;
        this.uploadError = err.error?.message || err.error?.error || 'Document upload failed. Please try again.';
      }
    });
  }

  // Video KYC methods
  async startVideoKyc() {
    if (!this.kycSessionData?.id) return;
    
    this.startingVideo = true;
    this.submitError = '';

    try {
      // Request camera access
      this.mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      
      // Start video KYC on backend
      this.videoKycService.startVideoKyc(this.kycSessionData.id).subscribe({
        next: (response: any) => {
          this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
          this.kycOtp = response.otpCode || '';
          this.videoStarted = true;
          this.startingVideo = false;
          this.connectionStatus = 'connected';
          
          // Attach stream to video element
          setTimeout(() => {
            if (this.localVideoRef?.nativeElement && this.mediaStream) {
              this.localVideoRef.nativeElement.srcObject = this.mediaStream;
            }
          }, 100);
        },
        error: (err: any) => {
          this.startingVideo = false;
          this.submitError = err.error?.message || 'Failed to start Video KYC session.';
          this.stopMediaStream();
        }
      });
    } catch (err) {
      this.startingVideo = false;
      this.submitError = 'Camera/microphone access denied. Please enable permissions and try again.';
    }
  }

  verifyOtpAction() {
    if (!this.kycSessionData?.id || !this.kycOtp) return;
    
    this.verifyingOtp = true;
    this.videoKycService.verifyOtp(this.kycSessionData.id, this.kycOtp).subscribe({
      next: (response: any) => {
        this.verifyingOtp = false;
        this.otpVerifiedFlag = true;
        this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
      },
      error: (err: any) => {
        this.verifyingOtp = false;
        this.submitError = err.error?.message || 'OTP verification failed.';
      }
    });
  }

  captureFaceSnapshot() {
    if (!this.localVideoRef?.nativeElement || !this.kycSessionData?.id) return;
    
    const canvas = document.createElement('canvas');
    const video = this.localVideoRef.nativeElement;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d')?.drawImage(video, 0, 0);
    
    canvas.toBlob((blob) => {
      if (blob) {
        const file = new File([blob], 'face-snapshot.jpg', { type: 'image/jpeg' });
        this.videoKycService.saveFaceSnapshot(this.kycSessionData.id, file).subscribe({
          next: (response: any) => {
            this.faceSnapshotTaken = true;
            this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
            this.successMessage = 'Face captured successfully!';
            setTimeout(() => this.successMessage = '', 2000);
          },
          error: (err: any) => {
            this.submitError = err.error?.message || 'Face capture failed.';
          }
        });
      }
    }, 'image/jpeg', 0.8);
  }

  captureIdSnapshot() {
    if (!this.localVideoRef?.nativeElement || !this.kycSessionData?.id) return;
    
    const canvas = document.createElement('canvas');
    const video = this.localVideoRef.nativeElement;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d')?.drawImage(video, 0, 0);
    
    canvas.toBlob((blob) => {
      if (blob) {
        const file = new File([blob], 'id-snapshot.jpg', { type: 'image/jpeg' });
        this.videoKycService.saveIdSnapshot(this.kycSessionData.id, file).subscribe({
          next: (response: any) => {
            this.idSnapshotTaken = true;
            this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
            this.successMessage = 'ID document captured successfully!';
            setTimeout(() => this.successMessage = '', 2000);
          },
          error: (err: any) => {
            this.submitError = err.error?.message || 'ID capture failed.';
          }
        });
      }
    }, 'image/jpeg', 0.8);
  }

  performLivenessCheck() {
    if (!this.kycSessionData?.id) return;
    
    this.videoKycService.livenessCheck(this.kycSessionData.id, true, 'BLINK').subscribe({
      next: (response: any) => {
        this.livenessChecked = true;
        this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
        this.successMessage = 'Liveness check passed!';
        setTimeout(() => this.successMessage = '', 2000);
      },
      error: (err: any) => {
        this.submitError = err.error?.message || 'Liveness check failed.';
      }
    });
  }

  submitVideoKyc() {
    if (!this.kycSessionData?.id) return;
    
    this.loading = true;
    this.loadingMessage = 'Submitting Video KYC for review...';

    this.videoKycService.endSession(this.kycSessionData.id).subscribe({
      next: (response: any) => {
        this.loading = false;
        this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
        this.stopVideo();
        this.currentStep = 5;
      },
      error: (err: any) => {
        this.loading = false;
        this.submitError = err.error?.message || 'Failed to submit Video KYC.';
      }
    });
  }

  stopVideo() {
    this.stopMediaStream();
    this.videoStarted = false;
    this.connectionStatus = 'waiting';
  }

  private stopMediaStream() {
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop());
      this.mediaStream = null;
    }
  }

  refreshStatus() {
    if (!this.kycSessionData?.id) return;
    
    this.videoKycService.getStatus(this.kycSessionData.id).subscribe({
      next: (response: any) => {
        this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
      },
      error: () => {}
    });
  }

  reattemptVideoKyc() {
    // Reset video KYC state for reattempt
    this.faceSnapshotTaken = false;
    this.idSnapshotTaken = false;
    this.livenessChecked = false;
    this.otpVerifiedFlag = false;
    this.kycOtp = '';
    this.currentStep = 4;
  }

  goToLogin() {
    this.router.navigate(['/website/login']);
  }

  getKycStatusClass(status: string): string {
    switch (status) {
      case 'Approved': return 'status-approved';
      case 'Rejected': return 'status-rejected';
      case 'Under Review': case 'Submitted': return 'status-pending';
      case 'Scheduled': return 'status-scheduled';
      default: return 'status-default';
    }
  }

  ngOnDestroy() {
    this.stopMediaStream();
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  // ======================== Slot Booking Methods ========================

  loadAvailableSlots() {
    this.videoKycService.getAvailableSlots().subscribe({
      next: (slots: any[]) => {
        this.availableSlots = slots;
        const dateSet = new Set(slots.map((s: any) => s.slotDate));
        this.availableDates = Array.from(dateSet).sort();
        if (this.availableDates.length > 0 && !this.selectedSlotDate) {
          this.selectDate(this.availableDates[0]);
        }
      },
      error: () => {
        this.slotBookingError = 'Failed to load available slots.';
      }
    });
  }

  selectDate(date: string) {
    this.selectedSlotDate = date;
    this.selectedSlot = null;
    this.filteredSlots = this.availableSlots.filter((s: any) => s.slotDate === date);
  }

  selectSlot(slot: any) {
    this.selectedSlot = slot;
    this.slotBookingError = '';
  }

  confirmSlotBooking() {
    if (!this.selectedSlot || !this.kycSessionData?.id) return;
    this.bookingSlot = true;
    this.slotBookingError = '';

    this.videoKycService.bookSlot(this.kycSessionData.id, this.selectedSlot.id).subscribe({
      next: (response: any) => {
        this.bookingSlot = false;
        this.kycSessionData = { ...this.kycSessionData, ...response, id: this.kycSessionData.id };
        this.successMessage = 'Slot booked successfully!';
        this.startCountdown();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err: any) => {
        this.bookingSlot = false;
        this.slotBookingError = err.error?.message || 'Failed to book slot.';
      }
    });
  }

  cancelSlotBooking() {
    if (!this.kycSessionData?.id) return;

    this.videoKycService.cancelBooking(this.kycSessionData.id).subscribe({
      next: (response: any) => {
        this.kycSessionData = { ...this.kycSessionData, ...response, bookedSlotId: null, slotDate: null, slotTime: null, slotEndTime: null };
        this.canJoinNow = false;
        this.countdownDisplay = '';
        if (this.countdownInterval) {
          clearInterval(this.countdownInterval);
        }
        this.selectedSlot = null;
        this.loadAvailableSlots();
        this.successMessage = 'Booking cancelled.';
        setTimeout(() => this.successMessage = '', 2000);
      },
      error: (err: any) => {
        this.submitError = err.error?.message || 'Failed to cancel booking.';
      }
    });
  }

  startCountdown() {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }

    const updateCountdown = () => {
      if (!this.kycSessionData?.slotDate || !this.kycSessionData?.slotTime) return;

      const slotDateTime = new Date(`${this.kycSessionData.slotDate}T${this.kycSessionData.slotTime}`);
      const now = new Date();
      const diff = slotDateTime.getTime() - now.getTime();

      if (diff <= 5 * 60 * 1000) { // Within 5 minutes, allow joining
        this.canJoinNow = true;
        this.countdownDisplay = '';
        this.countdownParts = { hours: '00', minutes: '00', seconds: '00' };
        if (diff <= 0) {
          // Slot time has passed
          const endTime = new Date(`${this.kycSessionData.slotDate}T${this.kycSessionData.slotEndTime}`);
          if (now.getTime() > endTime.getTime()) {
            this.canJoinNow = false;
            this.countdownDisplay = '';
            clearInterval(this.countdownInterval);
          }
        }
        return;
      }

      this.canJoinNow = false;
      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);
      this.countdownParts = {
        hours: hours.toString().padStart(2, '0'),
        minutes: minutes.toString().padStart(2, '0'),
        seconds: seconds.toString().padStart(2, '0')
      };
      this.countdownDisplay = `${this.countdownParts.hours}:${this.countdownParts.minutes}:${this.countdownParts.seconds}`;
    };

    updateCountdown();
    this.countdownInterval = setInterval(updateCountdown, 1000);
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
