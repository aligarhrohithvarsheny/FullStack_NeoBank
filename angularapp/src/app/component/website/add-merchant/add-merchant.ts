import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MerchantOnboardingService } from '../../../service/merchant-onboarding.service';
import { AlertService } from '../../../service/alert.service';
import { Agent, MerchantCreatePayload } from '../../../model/merchant-onboarding/merchant-onboarding.model';

@Component({
  selector: 'app-add-merchant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-merchant.html',
  styleUrls: ['./add-merchant.css'],
  encapsulation: ViewEncapsulation.None
})
export class AddMerchant implements OnInit {
  agent: Agent | null = null;
  currentStep = 1;
  totalSteps = 4;
  loading = false;
  editMode = false;
  editMerchantId = '';

  // Step 1: Business Info
  businessName = '';
  ownerName = '';
  mobile = '';
  email = '';
  businessType = '';
  gstNumber = '';
  shopAddress = '';
  city = '';
  state = '';
  pincode = '';

  // Step 2: Bank Details
  bankName = '';
  accountNumber = '';
  confirmAccountNumber = '';
  ifscCode = '';
  accountHolderName = '';

  // Step 3: Device Selection
  applySoundbox = false;
  applyPos = false;
  soundboxQty = 1;
  posQty = 1;

  // Step 4: Document Upload
  shopPhoto: File | null = null;
  ownerIdProof: File | null = null;
  bankProof: File | null = null;
  shopPhotoName = '';
  ownerIdProofName = '';
  bankProofName = '';

  businessTypes = [
    'Retail', 'Grocery', 'Restaurant', 'Pharmacy', 'Electronics',
    'Clothing', 'Hardware', 'Stationery', 'Salon/Spa', 'Petrol Pump',
    'Hospital/Clinic', 'Education', 'Travel Agency', 'Other'
  ];

  bankNames = [
    'NeoBank', 'State Bank of India', 'HDFC Bank', 'ICICI Bank',
    'Punjab National Bank', 'Bank of Baroda', 'Canara Bank',
    'Union Bank of India', 'Kotak Mahindra Bank', 'Axis Bank',
    'IndusInd Bank', 'Yes Bank', 'Federal Bank', 'IDBI Bank',
    'Bank of India', 'Other'
  ];

  states = [
    'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh',
    'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand',
    'Karnataka', 'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur',
    'Meghalaya', 'Mizoram', 'Nagaland', 'Odisha', 'Punjab',
    'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura',
    'Uttar Pradesh', 'Uttarakhand', 'West Bengal', 'Delhi', 'Other'
  ];

  errors: { [key: string]: string } = {};

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private merchantService: MerchantOnboardingService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    const agentData = sessionStorage.getItem('agent');
    if (!agentData) {
      this.router.navigate(['/website/agent-login']);
      return;
    }
    this.agent = JSON.parse(agentData);

    // Check for edit mode
    this.route.params.subscribe(params => {
      if (params['merchantId']) {
        this.editMode = true;
        this.editMerchantId = params['merchantId'];
        this.loadMerchant(this.editMerchantId);
      }
    });
  }

  loadMerchant(merchantId: string) {
    this.merchantService.getMerchant(merchantId).subscribe({
      next: (res: any) => {
        if (res.success) {
          const m = res.merchant;
          this.businessName = m.businessName;
          this.ownerName = m.ownerName;
          this.mobile = m.mobile;
          this.email = m.email || '';
          this.businessType = m.businessType;
          this.gstNumber = m.gstNumber || '';
          this.shopAddress = m.shopAddress;
          this.city = m.city;
          this.state = m.state;
          this.pincode = m.pincode;
          this.bankName = m.bankName;
          this.accountNumber = m.accountNumber;
          this.confirmAccountNumber = m.accountNumber;
          this.ifscCode = m.ifscCode;
          this.accountHolderName = m.accountHolderName;
        }
      }
    });
  }

  nextStep() {
    if (this.validateCurrentStep()) {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
      }
    }
  }

  prevStep() {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  goToStep(step: number) {
    if (step <= this.currentStep) {
      this.currentStep = step;
    }
  }

  validateCurrentStep(): boolean {
    this.errors = {};

    if (this.currentStep === 1) {
      if (!this.businessName.trim()) this.errors['businessName'] = 'Business name is required';
      if (!this.ownerName.trim()) this.errors['ownerName'] = 'Owner name is required';
      if (!this.mobile.trim() || !/^[6-9]\d{9}$/.test(this.mobile)) this.errors['mobile'] = 'Valid 10-digit mobile is required';
      if (this.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) this.errors['email'] = 'Invalid email format';
      if (!this.businessType) this.errors['businessType'] = 'Business type is required';
      if (!this.shopAddress.trim()) this.errors['shopAddress'] = 'Shop address is required';
      if (!this.city.trim()) this.errors['city'] = 'City is required';
      if (!this.state) this.errors['state'] = 'State is required';
      if (!this.pincode.trim() || !/^\d{6}$/.test(this.pincode)) this.errors['pincode'] = 'Valid 6-digit pincode is required';
    }

    if (this.currentStep === 2) {
      if (!this.bankName) this.errors['bankName'] = 'Bank name is required';
      if (!this.accountNumber.trim()) this.errors['accountNumber'] = 'Account number is required';
      if (this.accountNumber !== this.confirmAccountNumber) this.errors['confirmAccountNumber'] = 'Account numbers do not match';
      if (!this.ifscCode.trim() || !/^[A-Z]{4}0[A-Z0-9]{6}$/.test(this.ifscCode.toUpperCase())) this.errors['ifscCode'] = 'Valid IFSC code is required';
      if (!this.accountHolderName.trim()) this.errors['accountHolderName'] = 'Account holder name is required';
    }

    if (this.currentStep === 3) {
      if (!this.applySoundbox && !this.applyPos) this.errors['device'] = 'Select at least one device';
    }

    return Object.keys(this.errors).length === 0;
  }

  onFileSelect(event: Event, type: string) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.size > 5 * 1024 * 1024) {
        this.alertService.error('Error', 'File size must be less than 5MB');
        return;
      }
      switch (type) {
        case 'shopPhoto':
          this.shopPhoto = file;
          this.shopPhotoName = file.name;
          break;
        case 'ownerIdProof':
          this.ownerIdProof = file;
          this.ownerIdProofName = file.name;
          break;
        case 'bankProof':
          this.bankProof = file;
          this.bankProofName = file.name;
          break;
      }
    }
  }

  submit() {
    if (!this.validateCurrentStep()) return;
    if (!this.agent?.agentId) return;

    this.loading = true;

    if (this.editMode) {
      const updated: any = {
        businessName: this.businessName,
        ownerName: this.ownerName,
        mobile: this.mobile,
        email: this.email,
        businessType: this.businessType,
        gstNumber: this.gstNumber,
        shopAddress: this.shopAddress,
        city: this.city,
        state: this.state,
        pincode: this.pincode,
        bankName: this.bankName,
        accountNumber: this.accountNumber,
        ifscCode: this.ifscCode.toUpperCase(),
        accountHolderName: this.accountHolderName
      };

      this.merchantService.updateMerchant(this.editMerchantId, updated).subscribe({
        next: (res: any) => {
          this.loading = false;
          if (res.success) {
            this.alertService.success('Success', 'Merchant updated successfully!');
            this.router.navigate(['/website/agent-dashboard']);
          }
        },
        error: (err: any) => {
          this.loading = false;
          this.alertService.error('Error', err.error?.error || 'Update failed');
        }
      });
      return;
    }

    const deviceTypes: string[] = [];
    const deviceQuantities: number[] = [];
    if (this.applySoundbox) {
      deviceTypes.push('SOUNDBOX');
      deviceQuantities.push(this.soundboxQty);
    }
    if (this.applyPos) {
      deviceTypes.push('POS');
      deviceQuantities.push(this.posQty);
    }

    const payload: MerchantCreatePayload = {
      businessName: this.businessName,
      ownerName: this.ownerName,
      mobile: this.mobile,
      email: this.email || undefined,
      businessType: this.businessType,
      gstNumber: this.gstNumber || undefined,
      shopAddress: this.shopAddress,
      city: this.city,
      state: this.state,
      pincode: this.pincode,
      bankName: this.bankName,
      accountNumber: this.accountNumber,
      ifscCode: this.ifscCode.toUpperCase(),
      accountHolderName: this.accountHolderName,
      agentId: this.agent.agentId!,
      deviceTypes,
      deviceQuantities
    };

    this.merchantService.createMerchant(payload).subscribe({
      next: (res: any) => {
        if (res.success) {
          // Upload documents if available
          const merchantId = res.merchant?.merchantId;
          if (merchantId && (this.shopPhoto || this.ownerIdProof || this.bankProof)) {
            this.merchantService.uploadDocuments(
              merchantId,
              this.shopPhoto || undefined,
              this.ownerIdProof || undefined,
              this.bankProof || undefined
            ).subscribe({
              next: () => {
                this.loading = false;
                this.alertService.success('Success', 'Merchant onboarded successfully! Merchant ID: ' + merchantId);
                this.router.navigate(['/website/agent-dashboard']);
              },
              error: () => {
                this.loading = false;
                this.alertService.success('Partial Success', 'Merchant created but document upload failed. Merchant ID: ' + merchantId);
                this.router.navigate(['/website/agent-dashboard']);
              }
            });
          } else {
            this.loading = false;
            this.alertService.success('Success', 'Merchant onboarded successfully! Merchant ID: ' + merchantId);
            this.router.navigate(['/website/agent-dashboard']);
          }
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.error || 'Submission failed');
      }
    });
  }

  goBack() {
    this.router.navigate(['/website/agent-dashboard']);
  }
}
