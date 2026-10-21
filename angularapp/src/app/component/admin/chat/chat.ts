import { Component, OnInit, OnDestroy, ViewChild, ElementRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatMessage } from '../../../service/chat.service';
import { AlertService } from '../../../service/alert.service';
import { UserService } from '../../../service/user';
import { AccountService } from '../../../service/account';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html',
  styleUrls: ['./chat.css']
})
export class AdminChat implements OnInit, OnDestroy {
  @ViewChild('chatContainer') chatContainer!: ElementRef;
  
  escalatedChats: ChatMessage[] = [];
  selectedSessionId: string = '';
  selectedChatMessages: ChatMessage[] = [];
  newMessage: string = '';
  isLoading: boolean = false;
  selectedFile: File | null = null;
  showAccountDetails: boolean = false;
  customerDetails: any = null;
  isLoadingCustomerDetails: boolean = false;
  pinnedSessionIds: string[] = [];
  private supportAuthRedirecting = false;
  private attachmentObjectUrls: string[] = [];
  private refreshInterval: any;

  constructor(
    private chatService: ChatService,
    private alertService: AlertService,
    private userService: UserService,
    private accountService: AccountService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    // Only make API calls in the browser, not during SSR
    if (isPlatformBrowser(this.platformId)) {
    this.loadPinnedChats();
    this.loadEscalatedChats();
    
    // Refresh escalated chats every 5 seconds
    this.refreshInterval = setInterval(() => {
      this.loadEscalatedChats();
      if (this.selectedSessionId) {
        this.loadChatHistory(this.selectedSessionId);
      }
    }, 5000);
    }
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    this.attachmentObjectUrls.forEach(url => URL.revokeObjectURL(url));
  }

  loadEscalatedChats() {
    // Only make API calls in the browser
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.chatService.getEscalatedChats().subscribe({
      next: (chats) => {
        // Group by sessionId and get latest message
        const sessionMap = new Map<string, ChatMessage>();
        chats.forEach(chat => {
          if (!sessionMap.has(chat.sessionId) || 
              new Date(chat.timestamp || '') > new Date(sessionMap.get(chat.sessionId)!.timestamp || '')) {
            sessionMap.set(chat.sessionId, chat);
          }
        });
        this.escalatedChats = Array.from(sessionMap.values());
        
        // If no session selected, select first one
        if (!this.selectedSessionId && this.escalatedChats.length > 0) {
          this.selectChat(this.escalatedChats[0].sessionId);
        }
      },
      error: (err) => {
        console.error('Error loading escalated chats:', err);
        this.handleChatAuthError(err);
      }
    });
  }

  selectChat(sessionId: string) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.selectedSessionId = sessionId;
    this.loadChatHistory(sessionId);
    
    // Take over the chat
    const chat = this.escalatedChats.find(item => item.sessionId === sessionId);
    if (chat?.status !== 'IN_PROGRESS') this.chatService.adminTakeover(sessionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Chat', 'You have taken over this chat');
        }
      },
      error: (err) => {
        console.error('Error taking over chat:', err);
        this.handleChatAuthError(err);
      }
    });
  }

  loadChatHistory(sessionId: string) {
    if (!isPlatformBrowser(this.platformId)) return;
    
    this.chatService.getChatHistory(sessionId, true).subscribe({
      next: (messages) => {
        // Deduplicate messages by ID to prevent duplicates
        const uniqueMessages = messages.filter((msg, index, self) => 
          index === self.findIndex((m) => m.id === msg.id)
        );
        this.selectedChatMessages = uniqueMessages;
        this.loadAttachmentPreviews(uniqueMessages);
        this.scrollToBottom();
      },
      error: (err) => {
        console.error('Error loading chat history:', err);
        this.handleChatAuthError(err);
      }
    });
  }

  sendMessage() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    if ((!this.newMessage.trim() && !this.selectedFile) || this.isLoading || !this.selectedSessionId) {
      return;
    }

    const messageText = this.newMessage.trim();
    const fileToSend = this.selectedFile;
    this.newMessage = '';
    this.selectedFile = null;
    this.isLoading = true;

    this.chatService.adminSendMessage(this.selectedSessionId, messageText, fileToSend || undefined).subscribe({
      next: (response: any) => {
        this.isLoading = false;
        if (response.success) {
          // Reload chat history to get all messages with proper timestamps
          // This prevents duplicates and ensures we have the latest data from server
          this.loadChatHistory(this.selectedSessionId);
        } else {
          this.alertService.error('Error', response.message || 'Failed to send message. Please try again.');
        }
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error sending message:', err);
        this.alertService.error('Error', 'Failed to send message. Please try again.');
        this.handleChatAuthError(err);
      }
    });
  }
  
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.alertService.error('Error', 'File size must be less than 5MB');
        return;
      }
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!validTypes.includes(file.type)) {
        this.alertService.error('Error', 'Only JPEG, PNG, and PDF files are allowed');
        return;
      }
      this.selectedFile = file;
    }
  }
  
  removeFile() {
    this.selectedFile = null;
  }
  
  triggerFileInput() {
    const fileInput = document.getElementById('adminFileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.click();
    }
  }
  
  getAttachmentUrl(attachmentUrl?: string): string {
    if (!attachmentUrl) return '';
    return this.selectedChatMessages.find(message => message.attachmentUrl === attachmentUrl)?.renderedAttachmentUrl || '';
  }

  private loadAttachmentPreviews(messages: ChatMessage[]): void {
    messages.filter(message => message.attachmentUrl && !message.renderedAttachmentUrl).forEach(message => {
      this.chatService.getAttachment(message.attachmentUrl!, true).subscribe({
        next: blob => {
          const objectUrl = URL.createObjectURL(blob);
          this.attachmentObjectUrls.push(objectUrl);
          message.renderedAttachmentUrl = objectUrl;
        },
        error: error => this.handleChatAuthError(error)
      });
    });
  }

  scrollToBottom() {
    setTimeout(() => {
      if (this.chatContainer) {
        const element = this.chatContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    }, 100);
  }

  formatTime(timestamp?: string): string {
    if (!timestamp) return '';
    try {
      const date = new Date(timestamp);
      // Check if date is valid
      if (isNaN(date.getTime())) {
        return '';
      }
      // Format: "HH:MM AM/PM"
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
      return '';
    }
  }
  
  formatDateTime(timestamp?: string): string {
    if (!timestamp) return '';
    try {
      const date = new Date(timestamp);
      // Check if date is valid
      if (isNaN(date.getTime())) {
        return '';
      }
      // Format: "MMM DD, YYYY HH:MM AM/PM"
      return date.toLocaleString('en-US', { 
        month: 'short', 
        day: 'numeric', 
        year: 'numeric',
        hour: '2-digit', 
        minute: '2-digit' 
      });
    } catch (e) {
      return '';
    }
  }

  formatDate(timestamp?: string): string {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  getChatPreview(sessionId: string): string {
    const chat = this.escalatedChats.find(c => c.sessionId === sessionId);
    return chat?.message || '';
  }

  getUserName(sessionId: string): string {
    const chat = this.escalatedChats.find(c => c.sessionId === sessionId);
    return chat?.userName || 'User';
  }

  getOrderedChats(): ChatMessage[] {
    return [...this.escalatedChats].sort((first, second) => {
      const firstPinned = this.pinnedSessionIds.includes(first.sessionId);
      const secondPinned = this.pinnedSessionIds.includes(second.sessionId);
      if (firstPinned !== secondPinned) return firstPinned ? -1 : 1;
      return new Date(second.timestamp || 0).getTime() - new Date(first.timestamp || 0).getTime();
    });
  }

  isChatPinned(sessionId: string): boolean {
    return this.pinnedSessionIds.includes(sessionId);
  }

  togglePinnedChat(sessionId: string, event?: Event): void {
    event?.stopPropagation();
    this.pinnedSessionIds = this.isChatPinned(sessionId)
      ? this.pinnedSessionIds.filter(id => id !== sessionId)
      : [sessionId, ...this.pinnedSessionIds];
    if (isPlatformBrowser(this.platformId)) {
      const adminKey = this.getAdminPinStorageKey();
      localStorage.setItem(`neobank-admin-pinned-chats:${adminKey}`, JSON.stringify(this.pinnedSessionIds));
    }
  }

  openCustomer360(): void {
    const accountNumber = this.getCurrentCustomerUserId();
    if (!accountNumber) {
      this.alertService.error('Customer unavailable', 'This conversation is not linked to an account.');
      return;
    }
    void this.router.navigate(['/admin/customer-360'], { state: { accountNumber } });
  }

  private loadPinnedChats(): void {
    try {
      const adminKey = this.getAdminPinStorageKey();
      const stored = JSON.parse(localStorage.getItem(`neobank-admin-pinned-chats:${adminKey}`) || '[]');
      this.pinnedSessionIds = Array.isArray(stored)
        ? stored.filter((sessionId: unknown): sessionId is string => typeof sessionId === 'string')
        : [];
    } catch (error) {
      console.error('Unable to load pinned support chats.', error);
      this.pinnedSessionIds = [];
    }
  }

  private getAdminPinStorageKey(): string {
    try {
      const admin = JSON.parse(sessionStorage.getItem('admin') || '{}');
      return String(admin.id || admin.username || 'admin');
    } catch {
      return 'admin';
    }
  }

  private handleChatAuthError(err: any): void {
    if (err?.status !== 401 || this.supportAuthRedirecting) return;
    this.supportAuthRedirecting = true;
    sessionStorage.removeItem('adminSessionToken');
    this.alertService.error('Admin session expired', 'Sign in again to continue customer support.');
    void this.router.navigate(['/admin/login']);
  }

  formatMessage(message: string): string {
    if (!message) return '';
    return message.replace(/\n/g, '<br>');
  }

  getCurrentCustomerUserId(): string {
    if (this.selectedChatMessages.length > 0) {
      return this.selectedChatMessages[0].userId;
    }
    const chat = this.escalatedChats.find(c => c.sessionId === this.selectedSessionId);
    return chat?.userId || '';
  }

  viewCustomerAccountDetails() {
    if (!isPlatformBrowser(this.platformId)) return;
    
    console.log('View Customer Account Details clicked');
    const userId = this.getCurrentCustomerUserId();
    console.log('Current customer userId:', userId);
    console.log('Selected chat messages:', this.selectedChatMessages);
    console.log('Selected session ID:', this.selectedSessionId);
    
    if (!userId) {
      console.error('No userId found');
      this.alertService.error('Error', 'Unable to identify customer. Please try again.');
      return;
    }

    this.isLoadingCustomerDetails = true;
    this.showAccountDetails = true;
    console.log('Modal should be visible now, showAccountDetails:', this.showAccountDetails);

    // Try to get user by account number first, then by email
    this.userService.getUserByAccountNumber(userId).subscribe({
      next: (user) => {
        console.log('User found by account number:', user);
        this.customerDetails = user;
        this.isLoadingCustomerDetails = false;
        
        // Also fetch account details if available
        if (user.accountNumber) {
          this.accountService.getAccountByNumber(user.accountNumber).subscribe({
            next: (account) => {
              console.log('Account details loaded:', account);
              this.customerDetails.account = account;
            },
            error: (err) => {
              console.error('Error loading account details:', err);
            }
          });
        }
      },
      error: (err) => {
        console.log('User not found by account number, trying by email. Error:', err);
        // If not found by account number, try by email
        this.userService.getUserByEmail(userId).subscribe({
          next: (user) => {
            console.log('User found by email:', user);
            this.customerDetails = user;
            this.isLoadingCustomerDetails = false;
            
            // Also fetch account details if available
            if (user.accountNumber) {
              this.accountService.getAccountByNumber(user.accountNumber).subscribe({
                next: (account) => {
                  console.log('Account details loaded:', account);
                  this.customerDetails.account = account;
                },
                error: (err) => {
                  console.error('Error loading account details:', err);
                }
              });
            }
          },
          error: (err2) => {
            this.isLoadingCustomerDetails = false;
            console.error('Error loading customer details:', err2);
            this.alertService.error('Error', 'Unable to load customer details. Please try again.');
          }
        });
      }
    });
  }

  closeAccountDetails() {
    this.showAccountDetails = false;
    this.customerDetails = null;
  }
}
