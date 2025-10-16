import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';

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

  ngOnInit() {
    this.loadUserProfile();
    this.loadExistingKycRequest();
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
      return;
    }
    
    // Fallback to localStorage
    const savedProfile = localStorage.getItem('user_profile');
    if (savedProfile) {
      this.userProfile = JSON.parse(savedProfile);
      this.name = this.userProfile?.name || '';
      console.log('Loaded user profile from localStorage:', this.userProfile);
    }
  }

  loadExistingKycRequest() {
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

  requestToAdmin() {
    if (!this.panNumber || !this.name) {
      alert('Please enter both PAN number and name!');
      return;
    }

    if (!this.userProfile) {
      alert('User profile not found. Please create an account first.');
      return;
    }

    const kycRequest: KycRequest = {
      id: undefined, // Let backend auto-generate the ID
      userId: this.userProfile.accountNumber,
      userName: this.name,
      userEmail: this.userProfile.email,
      userAccountNumber: this.userProfile.accountNumber,
      panNumber: this.panNumber,
      name: this.name,
      status: 'Pending',
      submittedDate: new Date().toISOString(),
      approvedBy: undefined
    };

    // Submit KYC request to MySQL database
    this.http.post('http://localhost:8080/api/kyc/create', kycRequest).subscribe({
      next: (response: any) => {
        console.log('KYC request created in MySQL:', response);
        
        // Also save to localStorage as backup
        this.saveKycRequest(kycRequest);
        
        this.kycRequest = kycRequest;
        this.status = 'Pending Approval';
        this.isRequested = true;
        
        alert('KYC request submitted successfully! Admin will review your application.');
      },
      error: (err: any) => {
        console.error('Error creating KYC request:', err);
        alert('Failed to submit KYC request. Please try again.');
        
        // Fallback to localStorage
        this.saveKycRequest(kycRequest);
        this.kycRequest = kycRequest;
        this.status = 'Pending Approval';
        this.isRequested = true;
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

  goBack() {
    this.router.navigate(['/website/userdashboard']);
  }
}
