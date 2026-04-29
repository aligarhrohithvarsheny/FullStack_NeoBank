import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AlertService } from '../../../service/alert.service';

@Component({
  selector: 'app-credit-cards',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './credit-cards.html',
  styleUrls: ['./credit-cards.css']
})
export class CreditCards implements OnInit {
  creditCards: any[] = [];
  filteredCreditCards: any[] = [];
  isLoadingCreditCards: boolean = false;
  selectedCreditCard: any = null;
  creditCardTransactions: any[] = [];
  creditCardBills: any[] = [];
  creditCardSearchQuery: string = '';
  creditCardStatusFilter: string = 'ALL';
  showApplyCreditCardModal: boolean = false;
  applyCreditCardForm = {
    accountNumber: '',
    userName: '',
    userEmail: '',
    pan: '',
    income: 0
  };
  showLimitModal: boolean = false;
  limitAction: 'increase' | 'decrease' = 'increase';
  limitChangeAmount: number = 0;
  limitChangeReason: string = '';
  adminName: string = 'Admin';
  
  // Limit change history
  limitChangeHistory: any[] = [];
  showHistoryModal: boolean = false;
  
  // Document upload for closing card
  showCloseCardModal: boolean = false;
  closeCardDocument: File | null = null;
  closeCardDocumentName: string = '';
  isUploadingDocument: boolean = false;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      // Get admin name from sessionStorage
      const adminData = sessionStorage.getItem('adminData');
      if (adminData) {
        try {
          const admin = JSON.parse(adminData);
          this.adminName = admin.name || admin.username || 'Admin';
        } catch (e) {
          console.error('Error parsing admin data:', e);
        }
      }
      this.loadCreditCards();
    }
  }

  goBack() {
    // Check if navigated from manager dashboard
    const navigationSource = sessionStorage.getItem('navigationSource');
    if (navigationSource === 'MANAGER') {
      const returnPath = sessionStorage.getItem('managerReturnPath') || '/manager/dashboard';
      this.router.navigate([returnPath]);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  loadCreditCards() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.isLoadingCreditCards = true;
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/all`).subscribe({
      next: (response: any) => {
        console.log('Credit cards response:', response);
        this.creditCards = Array.isArray(response) ? response : (response.content || []);
        this.filterCreditCards();
        this.isLoadingCreditCards = false;
      },
      error: (err: any) => {
        console.error('Error loading credit cards:', err);
        console.error('Error details:', {
          status: err.status,
          statusText: err.statusText,
          message: err.message,
          error: err.error
        });
        const errorMessage = err.error?.message || err.message || 'Failed to load credit cards. Please check the console for details.';
        this.alertService.error('Error', errorMessage);
        this.isLoadingCreditCards = false;
        // Set empty array on error to prevent UI issues
        this.creditCards = [];
        this.filteredCreditCards = [];
      }
    });
  }

  filterCreditCards() {
    let filtered = [...this.creditCards];
    
    // Filter by status
    if (this.creditCardStatusFilter !== 'ALL') {
      filtered = filtered.filter(card => 
        card.status?.toUpperCase() === this.creditCardStatusFilter.toUpperCase()
      );
    }
    
    // Filter by search query
    if (this.creditCardSearchQuery.trim()) {
      const query = this.creditCardSearchQuery.toLowerCase();
      filtered = filtered.filter(card =>
        card.userName?.toLowerCase().includes(query) ||
        card.accountNumber?.includes(query) ||
        card.cardNumber?.includes(query) ||
        card.maskedCardNumber?.includes(query)
      );
    }
    
    this.filteredCreditCards = filtered;
  }

  viewCreditCardDetails(card: any) {
    if (!card || !card.id) {
      console.error('Invalid card object:', card);
      this.alertService.error('Error', 'Invalid card selected');
      return;
    }
    this.selectedCreditCard = card;
    this.loadCreditCardDetails(card.id);
  }

  loadCreditCardDetails(cardId: string | number) {
    // Ensure cardId is a number for the API
    const numericCardId = typeof cardId === 'string' ? parseInt(cardId, 10) : cardId;
    
    if (isNaN(numericCardId)) {
      console.error('Invalid card ID:', cardId);
      return;
    }

    // Load transactions
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/transactions`).subscribe({
      next: (transactions: any) => {
        this.creditCardTransactions = Array.isArray(transactions) ? transactions : [];
      },
      error: (err: any) => {
        console.error('Error loading transactions:', err);
        this.creditCardTransactions = [];
        // Don't show error alert for transactions/bills as they're secondary data
      }
    });

    // Load bills
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/bills`).subscribe({
      next: (bills: any) => {
        this.creditCardBills = Array.isArray(bills) ? bills : [];
      },
      error: (err: any) => {
        console.error('Error loading bills:', err);
        this.creditCardBills = [];
        // Don't show error alert for transactions/bills as they're secondary data
      }
    });
  }

  submitCreditCardApplication() {
    if (!this.applyCreditCardForm.accountNumber || !this.applyCreditCardForm.pan) {
      this.alertService.error('Validation Error', 'Account Number and PAN are required');
      return;
    }

    const payload = {
      accountNumber: this.applyCreditCardForm.accountNumber,
      userName: this.applyCreditCardForm.userName,
      userEmail: this.applyCreditCardForm.userEmail,
      pan: this.applyCreditCardForm.pan
    };

    this.http.post(`${environment.apiBaseUrl}/api/credit-card-requests/create?income=${this.applyCreditCardForm.income || 0}`, payload).subscribe({
      next: (response: any) => {
        this.alertService.success('Success', 'Credit card application submitted successfully');
        this.showApplyCreditCardModal = false;
        this.applyCreditCardForm = {
          accountNumber: '',
          userName: '',
          userEmail: '',
          pan: '',
          income: 0
        };
        this.loadCreditCards();
      },
      error: (err: any) => {
        console.error('Error submitting application:', err);
        const errorMessage = err.error?.message || err.message || 'Failed to submit application';
        this.alertService.error('Error', errorMessage);
      }
    });
  }

  updateCreditLimit() {
    if (!this.limitChangeAmount || this.limitChangeAmount <= 0) {
      this.alertService.error('Validation Error', 'Please enter a valid amount');
      return;
    }

    if (!this.selectedCreditCard) {
      this.alertService.error('Validation Error', 'No credit card selected');
      return;
    }

    // Calculate new limit based on action
    const currentLimit = this.selectedCreditCard.approvedLimit || 0;
    const newLimit = this.limitAction === 'increase' 
      ? currentLimit + this.limitChangeAmount 
      : currentLimit - this.limitChangeAmount;

    if (newLimit < 0) {
      this.alertService.error('Validation Error', 'New limit cannot be negative');
      return;
    }

    // Update the credit card with new limit
    const updateData: any = {
      approvedLimit: newLimit
    };

    this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${this.selectedCreditCard.id}`, updateData).subscribe({
      next: (updatedCard: any) => {
        // Save limit change history
        this.saveLimitChangeHistory(this.selectedCreditCard.id, {
          action: this.limitAction,
          oldLimit: currentLimit,
          newLimit: newLimit,
          changeAmount: this.limitChangeAmount,
          reason: this.limitChangeReason,
          adminName: this.adminName,
          timestamp: new Date().toISOString(),
          date: new Date().toLocaleDateString('en-IN'),
          time: new Date().toLocaleTimeString('en-IN')
        });
        
        this.alertService.success('Success', `Credit limit ${this.limitAction === 'increase' ? 'increased' : 'decreased'} successfully from ₹${currentLimit.toFixed(2)} to ₹${newLimit.toFixed(2)}`);
        this.showLimitModal = false;
        this.limitChangeAmount = 0;
        this.limitChangeReason = '';
        // Reload credit cards and update selected card
        this.loadCreditCards();
        // Update selected card with new data
        setTimeout(() => {
          if (this.selectedCreditCard) {
            this.selectedCreditCard.approvedLimit = updatedCard.approvedLimit;
            this.selectedCreditCard.availableLimit = updatedCard.availableLimit;
            this.selectedCreditCard.usageLimit = updatedCard.usageLimit;
            this.loadCreditCardDetails(this.selectedCreditCard.id);
          }
        }, 500);
      },
      error: (err: any) => {
        console.error('Error updating limit:', err);
        const errorMessage = err.error?.message || err.message || 'Failed to update credit limit';
        this.alertService.error('Error', errorMessage);
      }
    });
  }

  generateBill(cardId: string | number) {
    if (!confirm('Generate bill for this credit card?')) return;

    const numericCardId = typeof cardId === 'string' ? parseInt(cardId, 10) : cardId;
    if (isNaN(numericCardId)) {
      this.alertService.error('Error', 'Invalid card ID');
      return;
    }

    this.http.post(`${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/generate-bill`, {}).subscribe({
      next: (response: any) => {
        this.alertService.success('Success', 'Bill generated successfully');
        this.loadCreditCardDetails(numericCardId);
      },
      error: (err: any) => {
        console.error('Error generating bill:', err);
        const errorMessage = err.error?.message || err.message || 'Failed to generate bill';
        this.alertService.error('Error', errorMessage);
      }
    });
  }

  getStatement(cardId: string | number) {
    const numericCardId = typeof cardId === 'string' ? parseInt(cardId, 10) : cardId;
    if (isNaN(numericCardId)) {
      this.alertService.error('Error', 'Invalid card ID');
      return;
    }

    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    // Format dates properly for backend
    const startDateStr = startDate.toISOString().split('T')[0] + 'T00:00:00';
    const endDateStr = endDate.toISOString().split('T')[0] + 'T23:59:59';
    
    this.http.get(`${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/statement?startDate=${startDateStr}&endDate=${endDateStr}`).subscribe({
      next: (transactions: any) => {
        // Display statement - you can create a modal or download as PDF
        console.log('Statement:', transactions);
        const transactionList = Array.isArray(transactions) ? transactions : [];
        this.alertService.success('Success', `Statement generated with ${transactionList.length} transactions`);
      },
      error: (err: any) => {
        console.error('Error getting statement:', err);
        const errorMessage = err.error?.message || err.message || 'Failed to get statement';
        this.alertService.error('Error', errorMessage);
      }
    });
  }

  closeCreditCard(cardId: string | number) {
    if (!this.selectedCreditCard) {
      this.alertService.error('Error', 'No credit card selected');
      return;
    }

    // Validate balance before closing
    const currentBalance = this.selectedCreditCard.currentBalance || 0;
    const overdueAmount = this.selectedCreditCard.overdueAmount || 0;
    const totalBalance = currentBalance + overdueAmount;

    // Check if balance is exactly 0
    if (totalBalance !== 0) {
      let errorMessage = '';
      if (totalBalance > 0) {
        errorMessage = `Cannot close credit card. Current balance is ₹${currentBalance.toFixed(2)}${overdueAmount > 0 ? ` and overdue amount is ₹${overdueAmount.toFixed(2)}` : ''}. Please clear all dues before closing.`;
      } else {
        errorMessage = `Cannot close credit card. There are outstanding dues of ₹${Math.abs(totalBalance).toFixed(2)}. Please clear all dues before closing.`;
      }
      this.alertService.error('Cannot Close Card', errorMessage);
      return;
    }

    // Check if there are any pending bills
    if (this.creditCardBills && this.creditCardBills.length > 0) {
      const pendingBills = this.creditCardBills.filter((bill: any) => 
        bill.status?.toLowerCase() !== 'paid' && bill.status?.toLowerCase() !== 'completed'
      );
      if (pendingBills.length > 0) {
        this.alertService.error('Cannot Close Card', `Cannot close credit card. There are ${pendingBills.length} pending bill(s). Please clear all bills before closing.`);
        return;
      }
    }

    // Show close card modal with document upload
    this.showCloseCardModal = true;
    this.closeCardDocument = null;
    this.closeCardDocumentName = '';
  }

  onDocumentSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validate file type (PDF, images)
      const validTypes = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png'];
      if (!validTypes.includes(file.type)) {
        this.alertService.error('Invalid File', 'Please upload a PDF or image file (JPEG, PNG)');
        return;
      }
      
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.alertService.error('File Too Large', 'File size must be less than 5MB');
        return;
      }
      
      this.closeCardDocument = file;
      this.closeCardDocumentName = file.name;
    }
  }

  confirmCloseCard() {
    if (!this.closeCardDocument) {
      this.alertService.error('Document Required', 'Please upload a signed document before closing the credit card');
      return;
    }

    if (!confirm('Are you sure you want to close this credit card? This action cannot be undone.')) return;

    const numericCardId = typeof this.selectedCreditCard.id === 'string' 
      ? parseInt(this.selectedCreditCard.id, 10) 
      : this.selectedCreditCard.id;
    
    if (isNaN(numericCardId)) {
      this.alertService.error('Error', 'Invalid card ID');
      return;
    }

    this.isUploadingDocument = true;

    // Convert file to base64 for storage
    const reader = new FileReader();
    reader.onload = () => {
      const base64Data = reader.result as string;
      
      // Save document to localStorage first
      const documentData = {
        documentData: base64Data,
        documentName: this.closeCardDocumentName,
        documentType: this.closeCardDocument!.type,
        closedBy: this.adminName,
        closedDate: new Date().toISOString(),
        cardNumber: this.selectedCreditCard?.cardNumber || this.selectedCreditCard?.maskedCardNumber
      };
      
      this.saveCloseCardDocument(numericCardId, documentData);
      
      // Then close the card
      this.http.put(`${environment.apiBaseUrl}/api/credit-cards/${numericCardId}/close`, {}).subscribe({
        next: (response: any) => {
          this.alertService.success('Success', 'Credit card closed successfully with document uploaded');
          this.showCloseCardModal = false;
          this.closeCardDocument = null;
          this.closeCardDocumentName = '';
          this.isUploadingDocument = false;
          this.selectedCreditCard = null;
          this.loadCreditCards();
        },
        error: (err: any) => {
          console.error('Error closing card:', err);
          this.isUploadingDocument = false;
          const errorMessage = err.error?.message || err.message || 'Failed to close credit card';
          this.alertService.error('Error', errorMessage);
        }
      });
    };
    
    reader.onerror = () => {
      this.isUploadingDocument = false;
      this.alertService.error('Error', 'Failed to read document file');
    };
    
    reader.readAsDataURL(this.closeCardDocument!);
  }

  cancelCloseCard() {
    this.showCloseCardModal = false;
    this.closeCardDocument = null;
    this.closeCardDocumentName = '';
  }

  // Helper method to check if card can be closed
  canCloseCard(card: any): boolean {
    if (!card || card.status === 'Closed') return false;
    
    // Check balance - must be exactly 0 (no positive or negative balance)
    const currentBalance = parseFloat(card.currentBalance) || 0;
    const overdueAmount = parseFloat(card.overdueAmount) || 0;
    const totalBalance = currentBalance + overdueAmount;
    
    // Balance must be exactly 0 (allowing for small floating point differences)
    return Math.abs(totalBalance) < 0.01; // Allow for floating point precision
  }

  // Save limit change history
  saveLimitChangeHistory(cardId: string | number, historyData: any) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const historyKey = `creditCardLimitHistory_${cardId}`;
    const existingHistory = localStorage.getItem(historyKey);
    const history = existingHistory ? JSON.parse(existingHistory) : [];
    
    const historyEntry = {
      id: `LH${Date.now()}`,
      cardId: cardId,
      ...historyData
    };
    
    history.unshift(historyEntry);
    
    // Keep only last 100 entries per card
    if (history.length > 100) {
      history.splice(100);
    }
    
    localStorage.setItem(historyKey, JSON.stringify(history));
    console.log('Limit change history saved:', historyEntry);
  }

  // Load limit change history
  loadLimitChangeHistory(cardId: string | number) {
    if (!isPlatformBrowser(this.platformId)) return [];
    
    const historyKey = `creditCardLimitHistory_${cardId}`;
    const existingHistory = localStorage.getItem(historyKey);
    return existingHistory ? JSON.parse(existingHistory) : [];
  }

  // Save close card document info
  saveCloseCardDocument(cardId: string | number, documentData: any) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    const docKey = `creditCardCloseDocument_${cardId}`;
    localStorage.setItem(docKey, JSON.stringify(documentData));
    console.log('Close card document saved:', documentData);
  }

  // Get close card document info
  getCloseCardDocument(cardId: string | number): any {
    if (!isPlatformBrowser(this.platformId)) return null;
    
    const docKey = `creditCardCloseDocument_${cardId}`;
    const docData = localStorage.getItem(docKey);
    return docData ? JSON.parse(docData) : null;
  }

  // Download close card document
  downloadCloseCardDocument(cardId: string | number) {
    const docData = this.getCloseCardDocument(cardId);
    if (!docData || (!docData.documentData && !docData.documentUrl)) {
      this.alertService.error('Error', 'Document not found for this credit card');
      return;
    }

    try {
      let dataUrl = '';
      let fileName = docData.documentName || `credit-card-close-document-${cardId}`;
      
      // If document is stored as base64 data
      if (docData.documentData) {
        dataUrl = docData.documentData;
        // Add file extension based on document type
        if (docData.documentType) {
          const ext = docData.documentType.includes('pdf') ? '.pdf' : 
                     docData.documentType.includes('jpeg') || docData.documentType.includes('jpg') ? '.jpg' : 
                     docData.documentType.includes('png') ? '.png' : '.pdf';
          if (!fileName.includes('.')) {
            fileName += ext;
          }
        }
      } else if (docData.documentUrl) {
        // If it's a URL (for future backend integration)
        if (docData.documentUrl.startsWith('data:') || docData.documentUrl.startsWith('blob:')) {
          dataUrl = docData.documentUrl;
        } else {
          // Fetch from server
          this.http.get(docData.documentUrl, { responseType: 'blob' }).subscribe({
            next: (blob: Blob) => {
              const url = window.URL.createObjectURL(blob);
              const link = document.createElement('a');
              link.href = url;
              link.download = fileName;
              link.click();
              window.URL.revokeObjectURL(url);
              this.alertService.success('Success', 'Document downloaded successfully');
            },
            error: (err: any) => {
              console.error('Error downloading document:', err);
              this.alertService.error('Error', 'Failed to download document');
            }
          });
          return;
        }
      }
      
      // Download the file
      const link = document.createElement('a');
      link.href = dataUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      this.alertService.success('Success', 'Document downloaded successfully');
    } catch (error) {
      console.error('Error downloading document:', error);
      this.alertService.error('Error', 'Failed to download document');
    }
  }

  // Show limit change history
  showLimitHistory(cardId: string | number) {
    this.limitChangeHistory = this.loadLimitChangeHistory(cardId);
    this.showHistoryModal = true;
  }

  // Get close card reason/status
  getCloseCardStatus(card: any): { canClose: boolean; message: string } {
    if (!card) {
      return { canClose: false, message: 'Invalid card' };
    }
    
    if (card.status === 'Closed') {
      return { canClose: false, message: 'Card is already closed' };
    }
    
    const currentBalance = parseFloat(card.currentBalance) || 0;
    const overdueAmount = parseFloat(card.overdueAmount) || 0;
    const totalBalance = currentBalance + overdueAmount;

    // Check if balance is exactly 0 (allowing for floating point precision)
    if (Math.abs(totalBalance) < 0.01) {
      return { canClose: true, message: '✓ Card can be closed - All balances cleared' };
    } else if (totalBalance > 0) {
      return { 
        canClose: false, 
        message: `✗ Cannot close: Outstanding balance of ₹${totalBalance.toFixed(2)} must be cleared first` 
      };
    } else {
      return { 
        canClose: false, 
        message: `✗ Cannot close: Outstanding dues of ₹${Math.abs(totalBalance).toFixed(2)} must be cleared first` 
      };
    }
  }
}
