import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

interface KycRequest {
  id?: string; // Make ID optional since backend auto-generates it
  userId: string;
  userName: string;
  userEmail: string;
  userAccountNumber: string;
  panNumber: string;
  name: string;
  status: 'Pending' | 'Approved' | 'Rejected';
  submittedDate: string;
  approvedDate?: string;
  approvedBy?: string;
}

interface UserProfile {
  name: string;
  email: string;
  accountNumber: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  accountType: string;
  joinDate: string;
  pan?: string;
  aadhar?: string;
  occupation?: string;
  income?: number;
}

@Component({
  selector: 'app-kycupdate',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './kycupdate.html',
  styleUrls: ['./kycupdate.css']
})
export class Kycupdate implements OnInit {
  constructor(private router: Router, @Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {}
  
  panNumber: string = '';
  name: string = '';
  status: string = 'Not Requested';
  isRequested: boolean = false;
  userProfile: UserProfile | null = null;
  kycRequest: KycRequest | null = null;
  kycHistory: KycRequest[] = []; // All KYC requests history
  
  // OTP verification for subsequent requests
  requiresOtp: boolean = false;
  showOtpInput: boolean = false;
  kycOtp: string = '';
  sendingOtp: boolean = false;
  otpError: string = '';
  otpSuccessMessage: string = '';

  ngOnInit() {
    this.loadUserProfile();
    this.loadExistingKycRequest();
  }
  
  checkIfOtpRequired() {
    if (!this.userProfile || !this.userProfile.accountNumber) {
      // Retry after a short delay if profile is not loaded yet
      setTimeout(() => {
        if (this.userProfile && this.userProfile.accountNumber) {
          this.checkIfOtpRequired();
        }
      }, 500);
      return;
    }
    
    // Check if user has existing KYC requests
    this.http.get(`${environment.apiUrl}/kyc/check-existing/${this.userProfile.accountNumber}`).subscribe({
      next: (response: any) => {
        console.log('Check existing KYC requests response:', response);
        this.requiresOtp = response.requiresOtp || response.hasExisting || false;
        console.log('OTP required for KYC update:', this.requiresOtp);
      },
      error: (err: any) => {
        console.error('Error checking existing KYC requests:', err);
        // Default to not requiring OTP if check fails
        this.requiresOtp = false;
      }
    });
  }

  loadUserProfile() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // First try to get user data from session storage (current logged-in user)
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const userData = JSON.parse(currentUser);
      this.userProfile = {
        name: userData.name,
        email: userData.email,
        accountNumber: userData.accountNumber,
        accountType: 'Savings',
        joinDate: userData.loginTime
      };
      this.name = this.userProfile.name || '';
      console.log('Loaded user profile from session:', this.userProfile);
      
      // Load existing PAN from account
      this.loadExistingPanFromAccount();
      
      // Check if OTP is required after profile is loaded
      this.checkIfOtpRequired();
      // Load KYC history after profile is loaded
      this.loadKycHistory();
      return;
    }
    
    // Fallback to localStorage
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      this.userProfile = JSON.parse(savedProfile);
      this.name = this.userProfile?.name || '';
      this.panNumber = this.userProfile?.pan || ''; // Load PAN from profile
      console.log('Loaded user profile from localStorage:', this.userProfile);
      
      // Load existing PAN from account
      this.loadExistingPanFromAccount();
      
      // Check if OTP is required after profile is loaded
      this.checkIfOtpRequired();
      // Load KYC history after profile is loaded
      this.loadKycHistory();
    }
  }

  loadExistingPanFromAccount() {
    if (!this.userProfile || !this.userProfile.accountNumber) return;
    
    // Load user account details to get existing PAN
    this.http.get<any>(`${environment.apiUrl}/users/account/${this.userProfile.accountNumber}`).subscribe({
      next: (userData: any) => {
        // Get PAN from account
        const existingPan = userData.account?.pan || userData.pan || this.userProfile?.pan || '';
        if (existingPan) {
          this.panNumber = existingPan;
          console.log('Loaded existing PAN from account:', this.panNumber);
        } else {
          // If no PAN in account, try to get from most recent approved KYC
          this.loadPanFromApprovedKyc();
        }
      },
      error: (err: any) => {
        console.error('Error loading account details:', err);
        // Fallback to loading PAN from approved KYC
        this.loadPanFromApprovedKyc();
      }
    });
  }

  loadPanFromApprovedKyc() {
    if (!this.userProfile || !this.userProfile.accountNumber) return;
    
    // Try to get PAN from most recent approved KYC
    this.http.get<any[]>(`${environment.apiUrl}/kyc/account/${this.userProfile.accountNumber}/all`).subscribe({
      next: (kycRequests: any[]) => {
        if (kycRequests && Array.isArray(kycRequests)) {
          // Find the most recent approved KYC
          const approvedKyc = kycRequests.find(req => req.status === 'Approved');
          if (approvedKyc && approvedKyc.panNumber) {
            this.panNumber = approvedKyc.panNumber;
            console.log('Loaded PAN from approved KYC:', this.panNumber);
          }
        }
      },
      error: (err: any) => {
        console.error('Error loading PAN from KYC:', err);
      }
    });
  }

  loadExistingKycRequest() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // First try to load from backend
    if (this.userProfile && this.userProfile.accountNumber) {
      // Fetch all existing KYC requests from backend by account number
      this.http.get<any[]>(`${environment.apiUrl}/kyc/account/${this.userProfile.accountNumber}/all`).subscribe({
        next: (kycRequests: any[]) => {
          if (kycRequests && Array.isArray(kycRequests) && kycRequests.length > 0) {
            // Get the most recent request (should be sorted by submittedDate desc from backend)
            const mostRecentRequest = kycRequests[0];
            this.kycRequest = mostRecentRequest;
            // Don't auto-fill the form - let users enter new data for updates
            // Only show status of most recent request
            this.status = mostRecentRequest.status === 'Pending' ? 'Pending Approval' : mostRecentRequest.status;
            this.isRequested = true;
            console.log('Loaded existing KYC request from backend:', mostRecentRequest);
            console.log('Total KYC requests for this account:', kycRequests.length);
          }
        },
        error: (err: any) => {
          // If error, fallback to localStorage
          console.error('Error loading existing KYC request from backend:', err);
          this.loadFromLocalStorage();
        }
      });
    } else {
      // Fallback to localStorage if profile is not loaded
      this.loadFromLocalStorage();
    }
  }

  loadKycHistory() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    if (this.userProfile && this.userProfile.accountNumber) {
      // Fetch all KYC requests history from backend
      this.http.get<any[]>(`${environment.apiUrl}/kyc/account/${this.userProfile.accountNumber}/all`).subscribe({
        next: (kycRequests: any[]) => {
          if (kycRequests && Array.isArray(kycRequests)) {
            // Sort by submittedDate descending (most recent first)
            this.kycHistory = kycRequests.sort((a, b) => {
              const dateA = a.submittedDate ? new Date(a.submittedDate).getTime() : 0;
              const dateB = b.submittedDate ? new Date(b.submittedDate).getTime() : 0;
              return dateB - dateA;
            });
            console.log('Loaded KYC history:', this.kycHistory.length, 'requests');
          }
        },
        error: (err: any) => {
          console.error('Error loading KYC history:', err);
          this.kycHistory = [];
        }
      });
    }
  }

  loadFromLocalStorage() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const kycRequests = localStorage.getItem('kyc_requests');
    if (kycRequests && this.userProfile) {
      const requests: KycRequest[] = JSON.parse(kycRequests);
      const userRequest = requests.find(req => req.userAccountNumber === this.userProfile?.accountNumber);
      
      if (userRequest) {
        this.kycRequest = userRequest;
        this.panNumber = userRequest.panNumber;
        this.name = userRequest.name;
        this.status = userRequest.status;
        this.isRequested = true;
      }
    }
  }

  sendKycOtp() {
    if (!this.userProfile || !this.userProfile.email || !this.userProfile.accountNumber) {
      this.otpError = 'User profile not found. Please refresh the page.';
      return;
    }

    this.sendingOtp = true;
    this.otpError = '';
    this.otpSuccessMessage = '';

    this.http.post('${environment.apiUrl}/kyc/send-otp', {
      userEmail: this.userProfile.email,
      userAccountNumber: this.userProfile.accountNumber
    }).subscribe({
      next: (response: any) => {
        console.log('Send KYC OTP response:', response);
        this.sendingOtp = false;

        if (response.success) {
          this.otpSuccessMessage = response.message || 'OTP has been sent to your email. Please check and enter the OTP.';
          this.showOtpInput = true;
          this.otpError = '';
        } else {
          this.otpError = response.message || 'Failed to send OTP. Please try again.';
          if (!response.requiresOtp) {
            // First-time request, no OTP needed
            this.requiresOtp = false;
            this.showOtpInput = false;
          }
        }
      },
      error: (err: any) => {
        console.error('Send KYC OTP error:', err);
        this.sendingOtp = false;
        this.otpError = err.error?.message || 'Failed to send OTP. Please try again.';
      }
    });
  }

  requestToAdmin() {
    // Only validate name (PAN is read-only and loaded from account)
    if (!this.name || this.name.trim() === '') {
      alert('Please enter your full name!');
      return;
    }

    // Ensure PAN is loaded from account
    if (!this.panNumber || this.panNumber.trim() === '') {
      alert('PAN number not found. Please contact support.');
      return;
    }

    if (!this.userProfile) {
      alert('User profile not found. Please create an account first.');
      return;
    }

    // If OTP is required but not yet sent, send OTP first
    if (this.requiresOtp && !this.showOtpInput) {
      // Send OTP first
      this.sendKycOtp();
      return;
    }

    // If OTP is required and input is shown, validate OTP before submitting
    if (this.requiresOtp && this.showOtpInput) {
      if (!this.kycOtp || this.kycOtp.length !== 6 || !/^\d{6}$/.test(this.kycOtp)) {
        this.otpError = 'Please enter a valid 6-digit OTP.';
        return;
      }
    }

    // Prepare request data - use existing PAN from account, only update name
    const requestData: any = {
      userId: this.userProfile.accountNumber,
      userName: this.name.trim(),
      userEmail: this.userProfile.email,
      userAccountNumber: this.userProfile.accountNumber,
      panNumber: this.panNumber, // Use existing PAN from account (read-only)
      name: this.name.trim() // Only name can be updated
    };

    // Include OTP if required
    if (this.requiresOtp && this.showOtpInput) {
      requestData.otp = this.kycOtp;
    }

    // Submit KYC request to MySQL database
    this.http.post('${environment.apiUrl}/kyc/create', requestData).subscribe({
      next: (response: any) => {
        console.log('KYC request created in MySQL:', response);
        
        if (response.success) {
          const kycRequest: KycRequest = {
            id: response.kycRequest?.id?.toString(),
            userId: this.userProfile!.accountNumber,
            userName: this.name,
            userEmail: this.userProfile!.email,
            userAccountNumber: this.userProfile!.accountNumber,
            panNumber: this.panNumber,
            name: this.name,
            status: 'Pending',
            submittedDate: new Date().toISOString(),
            approvedBy: undefined
          };
          
          // Also save to localStorage as backup
          this.saveKycRequest(kycRequest);
          
          this.kycRequest = kycRequest;
          this.status = 'Pending Approval';
          this.isRequested = true;
          
          // Reset only name field (PAN remains from account)
          this.name = '';
          // Reload PAN from account to ensure it's current
          this.loadExistingPanFromAccount();
          
          // Reset OTP fields
          this.kycOtp = '';
          this.showOtpInput = false;
          this.otpError = '';
          this.otpSuccessMessage = '';
          
          // Reload to show updated status and history
          this.loadExistingKycRequest();
          this.loadKycHistory();
          
          alert('KYC update request submitted successfully! Admin will review your application. You can submit additional updates at any time.');
        } else {
          if (response.requiresOtp) {
            // OTP is required but not provided or invalid
            this.otpError = response.message || 'OTP is required for subsequent KYC update requests.';
            if (!this.showOtpInput) {
              this.sendKycOtp();
            }
          } else {
            alert(response.message || 'Failed to submit KYC request. Please try again.');
          }
        }
      },
      error: (err: any) => {
        console.error('Error creating KYC request:', err);
        
        if (err.error?.requiresOtp) {
          // OTP is required
          this.otpError = err.error?.message || 'OTP is required for subsequent KYC update requests.';
          if (!this.showOtpInput) {
            this.sendKycOtp();
          }
        } else {
          alert(err.error?.message || 'Failed to submit KYC request. Please try again.');
        }
      }
    });
  }

  saveKycRequest(kycRequest: KycRequest) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const existingRequests = localStorage.getItem('kyc_requests');
    let requests: KycRequest[] = existingRequests ? JSON.parse(existingRequests) : [];
    
    // Remove any existing request for this user
    requests = requests.filter(req => req.userAccountNumber !== kycRequest.userAccountNumber);
    
    // Add new request
    requests.push(kycRequest);
    
    localStorage.setItem('kyc_requests', JSON.stringify(requests));
  }

  downloadKycDocument() {
    if (!this.kycRequest || !this.userProfile) {
      alert('KYC data not available for download.');
      return;
    }

    // Create KYC document content
    const kycDocument = this.generateKycDocument();
    
    // Create and download the document
    const currentDate = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
    const filename = `NeoBank_KYC_${this.userProfile?.accountNumber}_${currentDate}.pdf`;
    this.downloadDocument(kycDocument, filename);
  }

  private generateKycDocument(): string {
    const currentDate = new Date().toLocaleDateString();
    const currentTime = new Date().toLocaleTimeString();
    const downloadDate = new Date().toLocaleDateString('en-IN', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric' 
    });
    
    return `
╔══════════════════════════════════════════════════════════════════════════════╗
║                           NEOBANK INDIA LIMITED                             ║
║                        Know Your Customer (KYC) Document                    ║
║                                                                              ║
║  🏦 Bank Name: NeoBank India Limited                                        ║
║  📍 Registered Office: NeoBank Tower, Financial District, Mumbai - 400001   ║
║  📞 Customer Care: 1800-NEOBANK | 📧 Email: support@neobank.in             ║
║  🌐 Website: www.neobank.in | License: RBI License No. NB-2024-001         ║
╚══════════════════════════════════════════════════════════════════════════════╝

═══════════════════════════════════════════════════════════════════════════════
                                ACCOUNT INFORMATION
═══════════════════════════════════════════════════════════════════════════════

┌─ Account Details ───────────────────────────────────────────────────────────┐
│ Account Number    : ${this.userProfile?.accountNumber?.padEnd(20)} │
│ Account Holder    : ${this.userProfile?.name?.padEnd(20)} │
│ Email Address     : ${this.userProfile?.email?.padEnd(20)} │
│ Account Type      : ${this.userProfile?.accountType?.padEnd(20)} │
│ Phone Number      : ${this.userProfile?.phoneNumber || 'Not Provided'.padEnd(20)} │
│ Member Since      : ${this.userProfile?.joinDate?.padEnd(20)} │
└─────────────────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════════════════
                              KYC VERIFICATION DETAILS
═══════════════════════════════════════════════════════════════════════════════

┌─ KYC Information ───────────────────────────────────────────────────────────┐
│ PAN Number        : ${this.kycRequest?.panNumber?.padEnd(20)} │
│ Full Name         : ${this.kycRequest?.name?.padEnd(20)} │
│ KYC Status        : ${this.kycRequest?.status?.padEnd(20)} │
│ Submitted Date    : ${this.kycRequest?.submittedDate ? new Date(this.kycRequest.submittedDate).toLocaleDateString('en-IN').padEnd(20) : 'N/A'.padEnd(20)} │
│ Approved Date     : ${this.kycRequest?.approvedDate ? new Date(this.kycRequest.approvedDate).toLocaleDateString('en-IN').padEnd(20) : 'N/A'.padEnd(20)} │
│ Approved By       : ${(this.kycRequest?.approvedBy || 'System Admin').padEnd(20)} │
│ Verification ID   : KYC-${this.userProfile?.accountNumber}-${Date.now().toString().slice(-6).padEnd(20)} │
└─────────────────────────────────────────────────────────────────────────────┘

┌─ Additional KYC Details ────────────────────────────────────────────────────┐
│ Address           : ${(this.userProfile?.address || 'Not Provided').padEnd(20)} │
│ Date of Birth     : ${(this.userProfile?.dateOfBirth || 'Not Provided').padEnd(20)} │
│ Aadhar Number     : ${(this.userProfile?.aadhar || 'Not Provided').padEnd(20)} │
│ Occupation        : ${(this.userProfile?.occupation || 'Not Provided').padEnd(20)} │
│ Annual Income     : ₹${(this.userProfile?.income?.toLocaleString('en-IN') || 'Not Provided').padEnd(20)} │
└─────────────────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════════════════
                              DOCUMENT INFORMATION
═══════════════════════════════════════════════════════════════════════════════

┌─ Document Details ──────────────────────────────────────────────────────────┐
│ Download Date     : ${downloadDate.padEnd(20)} │
│ Generated On      : ${currentDate} at ${currentTime.padEnd(20)} │
│ Document ID       : KYC-${this.userProfile?.accountNumber}-${Date.now().toString().padEnd(20)} │
│ Document Type     : KYC Verification Certificate.padEnd(20) │
│ Validity Period   : Valid until next KYC update.padEnd(20) │
└─────────────────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════════════════
                                BANK SEAL & SIGNATURE
═══════════════════════════════════════════════════════════════════════════════

┌─ Official Authorization ────────────────────────────────────────────────────┐
│                                                                              │
│  [OFFICIAL BANK SEAL]                                                       │
│                                                                              │
│  ╔══════════════════════════════════════════════════════════════════════╗   │
│  ║                    NEOBANK INDIA LIMITED                            ║   │
│  ║                                                                      ║   │
│  ║  🏦 Authorized Signatory: KYC Verification Department               ║   │
│  ║  📅 Date: ${downloadDate.padEnd(30)} ║   │
│  ║  🔐 Digital Signature: Verified & Authenticated                     ║   │
│  ║                                                                      ║   │
│  ║  This document is digitally signed and verified by NeoBank's        ║   │
│  ║  KYC verification system. Any tampering will invalidate this        ║   │
│  ║  document.                                                          ║   │
│  ╚══════════════════════════════════════════════════════════════════════╝   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════════════════
                              IMPORTANT DISCLAIMERS
═══════════════════════════════════════════════════════════════════════════════

• This document serves as official proof of KYC verification for the above account.
• The information contained herein is accurate as of the date of generation.
• This document is valid for banking and financial transactions.
• For any queries or updates, please contact NeoBank customer support.
• Keep this document secure and do not share with unauthorized persons.
• NeoBank reserves the right to update KYC requirements as per RBI guidelines.

═══════════════════════════════════════════════════════════════════════════════
                              CONTACT INFORMATION
═══════════════════════════════════════════════════════════════════════════════

🏦 NeoBank India Limited
📍 Address: NeoBank Tower, Financial District, Mumbai - 400001, India
📞 Customer Care: 1800-NEOBANK (1800-6362656)
📧 Email: support@neobank.in | kyc@neobank.in
🌐 Website: www.neobank.in
🕒 Support Hours: 24/7 Customer Support Available

═══════════════════════════════════════════════════════════════════════════════
                    Generated by NeoBank KYC Verification System
                    © 2024 NeoBank India Limited. All Rights Reserved.
═══════════════════════════════════════════════════════════════════════════════
    `.trim();
  }

  private downloadDocument(content: string, filename: string) {
    // Create HTML content for PDF generation
    const htmlContent = this.generateHtmlForPdf(content);
    
    // Create a blob with HTML content
    const blob = new Blob([htmlContent], { type: 'text/html' });
    
    // Create a temporary URL for the blob
    const url = window.URL.createObjectURL(blob);
    
    // Open in new window for printing/saving as PDF
    const printWindow = window.open(url, '_blank');
    
    if (printWindow) {
      printWindow.onload = () => {
        printWindow.print();
        // Clean up after printing
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          printWindow.close();
        }, 1000);
      };
    }
    
    alert('KYC document opened for PDF download. Use your browser\'s print function to save as PDF.');
  }

  private generateHtmlForPdf(content: string): string {
    // Convert the text content to HTML with proper formatting
    const htmlContent = content
      .replace(/\n/g, '<br>')
      .replace(/╔═+/g, '<div style="border-top: 2px solid #0077cc; margin: 10px 0;"></div>')
      .replace(/╚═+/g, '<div style="border-bottom: 2px solid #0077cc; margin: 10px 0;"></div>')
      .replace(/═+/g, '<div style="border-top: 1px solid #ccc; margin: 5px 0;"></div>')
      .replace(/┌─/g, '<div style="border: 1px solid #0077cc; padding: 10px; margin: 5px 0; background: #f8f9fa;">')
      .replace(/└─/g, '</div>')
      .replace(/│/g, '')
      .replace(/║/g, '')
      .replace(/╗/g, '')
      .replace(/╝/g, '');

    return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>NeoBank KYC Document</title>
    <style>
        @page {
            size: A4;
            margin: 20mm;
        }
        body {
            font-family: 'Courier New', monospace;
            font-size: 12px;
            line-height: 1.4;
            color: #333;
            background: white;
            position: relative;
        }
        .watermark {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%) rotate(-45deg);
            font-size: 60px;
            color: rgba(30, 64, 175, 0.1);
            font-weight: bold;
            z-index: -1;
            pointer-events: none;
            white-space: nowrap;
        }
        .watermark-logo {
            position: fixed;
            top: 20%;
            left: 20%;
            transform: rotate(-30deg);
            font-size: 40px;
            color: rgba(30, 64, 175, 0.08);
            z-index: -1;
            pointer-events: none;
        }
        .header {
            text-align: center;
            border: 3px solid #0077cc;
            padding: 20px;
            margin-bottom: 20px;
            background: linear-gradient(135deg, #f8f9fa, #e9ecef);
        }
        .bank-name {
            font-size: 24px;
            font-weight: bold;
            color: #0077cc;
            margin-bottom: 10px;
        }
        .document-title {
            font-size: 18px;
            color: #333;
            margin-bottom: 15px;
        }
        .bank-info {
            font-size: 11px;
            color: #666;
        }
        .section {
            margin: 20px 0;
            border-left: 4px solid #0077cc;
            padding-left: 15px;
        }
        .section-title {
            font-size: 16px;
            font-weight: bold;
            color: #0077cc;
            margin-bottom: 10px;
            text-transform: uppercase;
        }
        .info-box {
            border: 1px solid #0077cc;
            padding: 15px;
            margin: 10px 0;
            background: #f8f9fa;
            border-radius: 5px;
        }
        .info-row {
            display: flex;
            margin: 5px 0;
        }
        .info-label {
            font-weight: bold;
            width: 200px;
            color: #0077cc;
        }
        .info-value {
            flex: 1;
        }
        .bank-seal {
            text-align: center;
            border: 2px solid #0077cc;
            padding: 20px;
            margin: 20px 0;
            background: #f8f9fa;
        }
        .seal-text {
            font-size: 14px;
            font-weight: bold;
            color: #0077cc;
        }
        .disclaimer {
            font-size: 10px;
            color: #666;
            margin-top: 20px;
            border-top: 1px solid #ccc;
            padding-top: 10px;
        }
        .contact-info {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
        }
        @media print {
            body { margin: 0; }
            .no-print { display: none; }
        }
    </style>
</head>
<body>
    <div class="watermark">NeoBank</div>
    <div class="watermark-logo">🏦</div>
    <div class="header">
        <div class="bank-name">🏦 NEOBANK INDIA LIMITED</div>
        <div class="document-title">Know Your Customer (KYC) Document</div>
        <div class="bank-info">
            📍 Registered Office: NeoBank Tower, Financial District, Mumbai - 400001<br>
            📞 Customer Care: 1800-NEOBANK | 📧 Email: support@neobank.in<br>
            🌐 Website: www.neobank.in | License: RBI License No. NB-2024-001
        </div>
    </div>

    <div class="section">
        <div class="section-title">Account Information</div>
        <div class="info-box">
            <div class="info-row">
                <div class="info-label">Account Number:</div>
                <div class="info-value">${this.userProfile?.accountNumber || 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Account Holder:</div>
                <div class="info-value">${this.userProfile?.name || 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Email Address:</div>
                <div class="info-value">${this.userProfile?.email || 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Account Type:</div>
                <div class="info-value">${this.userProfile?.accountType || 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Phone Number:</div>
                <div class="info-value">${this.userProfile?.phoneNumber || 'Not Provided'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Member Since:</div>
                <div class="info-value">${this.userProfile?.joinDate || 'N/A'}</div>
            </div>
        </div>
    </div>

    <div class="section">
        <div class="section-title">KYC Verification Details</div>
        <div class="info-box">
            <div class="info-row">
                <div class="info-label">PAN Number:</div>
                <div class="info-value">${this.kycRequest?.panNumber || 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Full Name:</div>
                <div class="info-value">${this.kycRequest?.name || 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">KYC Status:</div>
                <div class="info-value">${this.kycRequest?.status || 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Submitted Date:</div>
                <div class="info-value">${this.kycRequest?.submittedDate ? new Date(this.kycRequest.submittedDate).toLocaleDateString('en-IN') : 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Approved Date:</div>
                <div class="info-value">${this.kycRequest?.approvedDate ? new Date(this.kycRequest.approvedDate).toLocaleDateString('en-IN') : 'N/A'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Approved By:</div>
                <div class="info-value">${this.kycRequest?.approvedBy || 'System Admin'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Verification ID:</div>
                <div class="info-value">KYC-${this.userProfile?.accountNumber}-${Date.now().toString().slice(-6)}</div>
            </div>
        </div>
    </div>

    <div class="section">
        <div class="section-title">Additional KYC Details</div>
        <div class="info-box">
            <div class="info-row">
                <div class="info-label">Address:</div>
                <div class="info-value">${this.userProfile?.address || 'Not Provided'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Date of Birth:</div>
                <div class="info-value">${this.userProfile?.dateOfBirth || 'Not Provided'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Aadhar Number:</div>
                <div class="info-value">${this.userProfile?.aadhar || 'Not Provided'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Occupation:</div>
                <div class="info-value">${this.userProfile?.occupation || 'Not Provided'}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Annual Income:</div>
                <div class="info-value">₹${this.userProfile?.income?.toLocaleString('en-IN') || 'Not Provided'}</div>
            </div>
        </div>
    </div>

    <div class="section">
        <div class="section-title">Document Information</div>
        <div class="info-box">
            <div class="info-row">
                <div class="info-label">Download Date:</div>
                <div class="info-value">${new Date().toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Generated On:</div>
                <div class="info-value">${new Date().toLocaleDateString()} at ${new Date().toLocaleTimeString()}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Document ID:</div>
                <div class="info-value">KYC-${this.userProfile?.accountNumber}-${Date.now()}</div>
            </div>
            <div class="info-row">
                <div class="info-label">Document Type:</div>
                <div class="info-value">KYC Verification Certificate</div>
            </div>
            <div class="info-row">
                <div class="info-label">Validity Period:</div>
                <div class="info-value">Valid until next KYC update</div>
            </div>
        </div>
    </div>

    <div class="bank-seal">
        <div class="seal-text">[OFFICIAL BANK SEAL]</div>
        <div style="margin: 15px 0; padding: 15px; border: 2px solid #0077cc; background: white;">
            <div style="font-size: 16px; font-weight: bold; color: #0077cc; margin-bottom: 10px;">NEOBANK INDIA LIMITED</div>
            <div style="font-size: 12px; margin: 5px 0;">🏦 Authorized Signatory: KYC Verification Department</div>
            <div style="font-size: 12px; margin: 5px 0;">📅 Date: ${new Date().toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })}</div>
            <div style="font-size: 12px; margin: 5px 0;">🔐 Digital Signature: Verified & Authenticated</div>
            <div style="font-size: 10px; margin-top: 10px; color: #666;">
                This document is digitally signed and verified by NeoBank's KYC verification system.<br>
                Any tampering will invalidate this document.
            </div>
        </div>
    </div>

    <div class="section">
        <div class="section-title">Important Disclaimers</div>
        <div style="font-size: 11px; line-height: 1.5;">
            • This document serves as official proof of KYC verification for the above account.<br>
            • The information contained herein is accurate as of the date of generation.<br>
            • This document is valid for banking and financial transactions.<br>
            • For any queries or updates, please contact NeoBank customer support.<br>
            • Keep this document secure and do not share with unauthorized persons.<br>
            • NeoBank reserves the right to update KYC requirements as per RBI guidelines.
        </div>
    </div>

    <div class="contact-info">
        <div class="section-title">Contact Information</div>
        <div style="font-size: 11px; line-height: 1.5;">
            🏦 NeoBank India Limited<br>
            📍 Address: NeoBank Tower, Financial District, Mumbai - 400001, India<br>
            📞 Customer Care: 1800-NEOBANK (1800-6362656)<br>
            📧 Email: support@neobank.in | kyc@neobank.in<br>
            🌐 Website: www.neobank.in<br>
            🕒 Support Hours: 24/7 Customer Support Available
        </div>
    </div>

    <div class="disclaimer">
        Generated by NeoBank KYC Verification System<br>
        © 2024 NeoBank India Limited. All Rights Reserved.
    </div>

    <script>
        // Auto-print when page loads
        window.onload = function() {
            setTimeout(function() {
                window.print();
            }, 500);
        };
    </script>
</body>
</html>`;
  }

  downloadKycHistoryPDF() {
    if (!this.userProfile || !this.kycHistory || this.kycHistory.length === 0) {
      alert('No KYC history available to download.');
      return;
    }

    if (!isPlatformBrowser(this.platformId)) return;

    try {
      const pdfContent = this.generateKycHistoryPDFContent();
      
      // Create and download as HTML file (can be printed as PDF)
      const blob = new Blob([pdfContent], { type: 'text/html;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      const currentDate = new Date().toISOString().split('T')[0];
      link.download = `NeoBank_KYC_History_${this.userProfile.accountNumber}_${currentDate}.html`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      // Show instruction to user
      setTimeout(() => {
        const userChoice = confirm('KYC History document downloaded! Would you like to open it now to save as PDF?\n\nClick OK to open, or Cancel to download later.');
        if (userChoice) {
          // Open the downloaded file in a new window for printing
          const newWindow = window.open();
          if (newWindow) {
            newWindow.document.write(pdfContent);
            newWindow.document.close();
            newWindow.focus();
          }
        }
      }, 500);
      
      console.log('KYC History PDF download initiated successfully');
    } catch (error) {
      console.error('Error downloading KYC History PDF:', error);
      alert('Failed to download KYC History document. Please try again.');
    }
  }

  private generateKycHistoryPDFContent(): string {
    const currentDate = new Date().toLocaleDateString('en-IN', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric' 
    });
    const currentTime = new Date().toLocaleTimeString();

    let historyRows = '';
    this.kycHistory.forEach((request, index) => {
      const submittedDate = request.submittedDate 
        ? new Date(request.submittedDate).toLocaleDateString('en-IN', { 
            year: 'numeric', 
            month: 'long', 
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          })
        : 'N/A';
      
      const approvedDate = request.approvedDate 
        ? new Date(request.approvedDate).toLocaleDateString('en-IN', { 
            year: 'numeric', 
            month: 'long', 
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          })
        : 'N/A';

      const statusClass = request.status === 'Approved' ? 'approved' : 
                         request.status === 'Rejected' ? 'rejected' : 'pending';
      
      historyRows += `
        <tr class="history-row ${statusClass}">
          <td>${index + 1}</td>
          <td>${request.panNumber || 'N/A'}</td>
          <td>${request.name || 'N/A'}</td>
          <td><span class="status-badge ${statusClass}">${request.status || 'Pending'}</span></td>
          <td>${submittedDate}</td>
          <td>${approvedDate}</td>
          <td>${request.approvedBy || 'N/A'}</td>
        </tr>
      `;
    });

    return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>NeoBank KYC History</title>
    <style>
        @page {
            size: A4 landscape;
            margin: 15mm;
        }
        body {
            font-family: 'Arial', sans-serif;
            font-size: 11px;
            line-height: 1.4;
            color: #333;
            background: white;
            margin: 0;
            padding: 20px;
        }
        .header {
            text-align: center;
            border: 3px solid #0077cc;
            padding: 20px;
            margin-bottom: 20px;
            background: linear-gradient(135deg, #f8f9fa, #e9ecef);
        }
        .bank-name {
            font-size: 24px;
            font-weight: bold;
            color: #0077cc;
            margin-bottom: 10px;
        }
        .document-title {
            font-size: 18px;
            color: #333;
            margin-bottom: 15px;
        }
        .account-info {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 20px;
            border-left: 4px solid #0077cc;
        }
        .info-row {
            display: flex;
            margin: 5px 0;
        }
        .info-label {
            font-weight: bold;
            width: 150px;
            color: #0077cc;
        }
        .info-value {
            flex: 1;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
            font-size: 10px;
        }
        th {
            background: linear-gradient(135deg, #0077cc, #0056b3);
            color: white;
            padding: 12px 8px;
            text-align: left;
            font-weight: 600;
            border: 1px solid #0056b3;
        }
        td {
            padding: 10px 8px;
            border: 1px solid #ddd;
        }
        .history-row {
            transition: background-color 0.2s;
        }
        .history-row:hover {
            background-color: #f8f9fa;
        }
        .history-row.approved {
            background-color: #d4edda;
        }
        .history-row.rejected {
            background-color: #f8d7da;
        }
        .history-row.pending {
            background-color: #fff3cd;
        }
        .status-badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-weight: 600;
            font-size: 9px;
        }
        .status-badge.approved {
            background-color: #28a745;
            color: white;
        }
        .status-badge.rejected {
            background-color: #dc3545;
            color: white;
        }
        .status-badge.pending {
            background-color: #ffc107;
            color: #856404;
        }
        .footer {
            margin-top: 30px;
            padding-top: 15px;
            border-top: 2px solid #0077cc;
            text-align: center;
            font-size: 10px;
            color: #666;
        }
        .watermark {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%) rotate(-45deg);
            font-size: 60px;
            color: rgba(30, 64, 175, 0.1);
            font-weight: bold;
            z-index: -1;
            pointer-events: none;
        }
        @media print {
            body { margin: 0; }
            .no-print { display: none; }
        }
    </style>
</head>
<body>
    <div class="watermark">NeoBank</div>
    <div class="header">
        <div class="bank-name">🏦 NEOBANK INDIA LIMITED</div>
        <div class="document-title">KYC Update History Report</div>
        <div style="font-size: 11px; color: #666;">
            Generated on: ${currentDate} at ${currentTime}
        </div>
    </div>

    <div class="account-info">
        <div class="info-row">
            <div class="info-label">Account Number:</div>
            <div class="info-value">${this.userProfile?.accountNumber || 'N/A'}</div>
        </div>
        <div class="info-row">
            <div class="info-label">Account Holder:</div>
            <div class="info-value">${this.userProfile?.name || 'N/A'}</div>
        </div>
        <div class="info-row">
            <div class="info-label">Email:</div>
            <div class="info-value">${this.userProfile?.email || 'N/A'}</div>
        </div>
        <div class="info-row">
            <div class="info-label">Total KYC Updates:</div>
            <div class="info-value">${this.kycHistory.length}</div>
        </div>
    </div>

    <table>
        <thead>
            <tr>
                <th>#</th>
                <th>PAN Number</th>
                <th>Name</th>
                <th>Status</th>
                <th>Submitted Date</th>
                <th>Approved Date</th>
                <th>Approved By</th>
            </tr>
        </thead>
        <tbody>
            ${historyRows}
        </tbody>
    </table>

    <div class="footer">
        <div style="margin-bottom: 10px;">
            <strong>NeoBank India Limited</strong><br>
            📍 NeoBank Tower, Financial District, Mumbai - 400001, India<br>
            📞 Customer Care: 1800-NEOBANK | 📧 Email: support@neobank.in
        </div>
        <div>
            Generated by NeoBank KYC Verification System<br>
            © ${new Date().getFullYear()} NeoBank India Limited. All Rights Reserved.
        </div>
    </div>
</body>
</html>`;
  }

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }
}
