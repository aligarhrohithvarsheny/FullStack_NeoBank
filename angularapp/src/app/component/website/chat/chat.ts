import { Component, OnInit, OnDestroy, ViewChild, ElementRef, Inject, PLATFORM_ID, Output, EventEmitter } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ChatService, ChatMessage } from '../../../service/chat.service';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html',
  styleUrls: ['./chat.css']
})
export class Chat implements OnInit, OnDestroy {
  @ViewChild('chatContainer') chatContainer!: ElementRef;
  @Output() navigateToFeature = new EventEmitter<string>();
  
  messages: ChatMessage[] = [];
  newMessage: string = '';
  sessionId: string = '';
  userId: string = '';
  userName: string = '';
  isOpen: boolean = false;
  isLoading: boolean = false;
  isAuthenticated = false;
  isPinned = false;
  private authErrorShown = false;
  private attachmentObjectUrls: string[] = [];
  showMoreServices = false;
  selectedFile: File | null = null;
  private messageSubscription: any;
  private pollingInterval: any;
  serviceShortcuts: Array<{ label: string; feature: string }> = [];

  constructor(
    private chatService: ChatService,
    private alertService: AlertService,
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    // Only initialize in browser, not during SSR
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Get user info from session storage
    if (typeof sessionStorage !== 'undefined') {
      const currentUser = sessionStorage.getItem('currentUser');
      if (currentUser) {
        try {
          const user = JSON.parse(currentUser);
          this.userId = user.accountNumber || user.email;
          this.userName = user.name || user.username;
          this.isAuthenticated = !!(user.authToken || sessionStorage.getItem('jwt'));
        } catch (e) {
          console.error('Error parsing user data:', e);
        }
      }
    }

    if (!this.isAuthenticated || !this.userId) {
      this.sendWelcomeMessage();
      this.addLocalBotMessage('Please sign in to start or continue a saved support conversation.');
      return;
    }

    const sessionKey = `neobank-chat-session:${this.userId}`;
    this.sessionId = localStorage.getItem(sessionKey) || this.generateSessionId();
    localStorage.setItem(sessionKey, this.sessionId);
    this.isPinned = localStorage.getItem(`neobank-chat-pinned:${this.userId}`) === this.sessionId;
    
    // Load chat history
    this.loadChatHistory();

    // Subscribe to new messages
    this.messageSubscription = this.chatService.messages$.subscribe(message => {
      if (message.sessionId === this.sessionId) {
        this.messages.push(message);
        this.scrollToBottom();
      }
    });

    // Poll for new messages every 3 seconds
    this.pollingInterval = setInterval(() => {
      if (this.sessionId && this.isOpen) {
        this.loadChatHistory();
      }
    }, 3000);

    // Send welcome message
    this.sendWelcomeMessage();
  }

  ngOnDestroy() {
    if (this.messageSubscription) {
      this.messageSubscription.unsubscribe();
    }
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
    this.attachmentObjectUrls.forEach(url => URL.revokeObjectURL(url));
  }

  generateSessionId(): string {
    return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  loadChatHistory() {
    if (this.sessionId) {
      this.chatService.getChatHistory(this.sessionId).subscribe({
        next: (messages) => {
          // Only update if we have new messages (check by message count or last message timestamp)
          const lastMessageId = [...this.messages].reverse().find(message => message.id)?.id || 0;
          const newMessages = messages.filter(m => !m.id || m.id > (lastMessageId || 0));
          
          if (newMessages.length > 0 || this.messages.length !== messages.length) {
            this.messages = [...messages, ...this.messages.filter(message => message.localOnly)];
            this.loadAttachmentPreviews(messages);
            this.scrollToBottom();
          }
        },
        error: (err) => {
          console.error('Error loading chat history:', err);
          if (err.status === 401 && !this.authErrorShown) {
            this.authErrorShown = true;
            this.isAuthenticated = false;
            if (this.pollingInterval) clearInterval(this.pollingInterval);
            this.alertService.userError('Session expired', 'Sign in again to continue this saved support chat.');
          }
        }
      });
    }
  }

  sendWelcomeMessage() {
    const welcomeMessage: ChatMessage = {
      sessionId: this.sessionId,
      userId: this.userId,
      userName: 'Bot',
      message: 'Hello! I\'m NeoBank\'s virtual assistant. How can I help you today? I can help you with account balance, loans, transactions, and more!',
      sender: 'BOT',
      timestamp: new Date().toISOString()
    };
    this.messages.push(welcomeMessage);
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  togglePinnedChat(): void {
    if (!this.userId || !this.sessionId) return;
    const key = `neobank-chat-pinned:${this.userId}`;
    if (this.isPinned) {
      localStorage.removeItem(key);
      this.isPinned = false;
    } else {
      localStorage.setItem(key, this.sessionId);
      this.isPinned = true;
    }
  }

  connectToAgent() {
    if (!this.isAuthenticated) {
      this.alertService.userError('Sign in required', 'Please sign in to connect with a support agent.');
      return;
    }
    this.isOpen = true;
    setTimeout(() => {
      this.sendQuickMessage('Please connect me to a live support agent.');
    }, 100);
  }

  sendMessage() {
    if (!this.isAuthenticated) {
      this.alertService.userError('Sign in required', 'Please sign in to send a support message.');
      return;
    }
    if ((!this.newMessage.trim() && !this.selectedFile) || this.isLoading) {
      return;
    }

    const userMessage: ChatMessage = {
      sessionId: this.sessionId,
      userId: this.userId,
      userName: this.userName,
      message: this.newMessage.trim(),
      sender: 'USER',
      timestamp: new Date().toISOString()
    };

    this.messages.push(userMessage);
    const messageText = this.newMessage.trim();
    const fileToSend = this.selectedFile;
    this.newMessage = '';
    this.selectedFile = null;
    this.isLoading = true;
    this.scrollToBottom();

    this.chatService.sendMessage(this.sessionId, this.userName, messageText, fileToSend || undefined).subscribe({
      next: (response: any) => {
        this.isLoading = false;
        console.log('Chat response received:', response);
        if (response.success && response.botResponse) {
          // Map backend response to frontend ChatMessage format
          const botMessage: ChatMessage = {
            sessionId: response.botResponse.sessionId || this.sessionId,
            userId: response.botResponse.userId || this.userId,
            userName: response.botResponse.userName || 'Bot',
            message: response.botResponse.message || '',
            sender: (response.botResponse.sender || 'BOT') as 'USER' | 'BOT' | 'ADMIN' | 'SYSTEM',
            timestamp: response.botResponse.timestamp || new Date().toISOString(),
            status: response.botResponse.status,
            adminId: response.botResponse.adminId,
            isRead: response.botResponse.isRead
          };
          this.messages.push(botMessage);
          this.chatService.emitMessage(botMessage);
          this.scrollToBottom();
        } else {
          console.warn('Unexpected response format:', response);
          this.alertService.userError('Error', response.message || 'Unexpected response from server');
        }
      },
      error: (err: any) => {
        this.isLoading = false;
        console.error('Error sending message:', err);
        console.error('Error details:', {
          status: err.status,
          statusText: err.statusText,
          url: err.url,
          error: err.error,
          message: err.message
        });
        
        let errorMessage = 'Failed to send message. Please try again.';
        
        if (err.status === 0) {
          errorMessage = 'Cannot connect to server. Please check if the backend is running.';
        } else if (err.status === 404) {
          errorMessage = 'Chat endpoint not found. Please contact support.';
        } else if (err.status === 500) {
          errorMessage = err.error?.message || 'Server error. Please try again later.';
        } else if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (err.message) {
          errorMessage = err.message;
        }
        
        this.alertService.userError('Error', errorMessage);
        if (err.status === 401 && !this.authErrorShown) {
          this.authErrorShown = true;
          this.isAuthenticated = false;
          this.alertService.userError('Session expired', 'Sign in again to continue this saved support chat.');
        }
        
        // Show a fallback bot response if server error
        if (err.status === 0 || err.status >= 500) {
          const fallbackResponse: ChatMessage = {
            sessionId: this.sessionId,
            userId: this.userId,
            userName: 'Bot',
            message: 'I apologize, but I\'m having trouble connecting right now. Please try again in a moment or contact support directly.',
            sender: 'BOT',
            timestamp: new Date().toISOString()
          };
          this.messages.push(fallbackResponse);
          this.scrollToBottom();
        }
      }
    });
  }

  scrollToBottom() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    setTimeout(() => {
      if (this.chatContainer) {
        const element = this.chatContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    }, 100);
  }

  formatTime(timestamp?: string): string {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  formatMessage(message: string): string {
    if (!message) return '';
    return message.replace(/\n/g, '<br>');
  }
  
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.alertService.userError('Error', 'File size must be less than 5MB');
        return;
      }
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
      if (!validTypes.includes(file.type)) {
        this.alertService.userError('Error', 'Only JPEG, PNG, and PDF files are allowed');
        return;
      }
      this.selectedFile = file;
    }
  }
  
  removeFile() {
    this.selectedFile = null;
  }
  
  triggerFileInput() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const fileInput = document.getElementById('fileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.click();
    }
  }
  
  sendQuickMessage(message: string) {
    this.newMessage = message;
    this.sendMessage();
  }

  loadCreditCardBills(): void {
    this.loadCustomerData('bills');
  }

  loadGoldLoanDetails(): void {
    this.loadCustomerData('gold-loans');
  }

  toggleMoreServices(): void {
    this.showMoreServices = !this.showMoreServices;
    if (this.showMoreServices && !this.serviceShortcuts.length) {
      import('./chat-account-data').then(({ serviceShortcuts }) => this.serviceShortcuts = serviceShortcuts)
        .catch(error => {
          console.error('Unable to load dashboard service shortcuts.', error);
          this.addLocalBotMessage('Dashboard services are unavailable right now. Please use the menu to browse services.');
        });
    }
  }

  private loadCustomerData(type: 'bills' | 'gold-loans'): void {
    const credentials = this.getCustomerCredentials();
    if (!credentials) return;
    if (this.isLoading) return;

    this.isLoading = true;
    import('./chat-account-data').then(data => {
      const request = type === 'bills'
        ? data.loadCreditCardBillsMessage(this.http, credentials.accountNumber, credentials.headers)
        : data.loadGoldLoanDetailsMessage(this.http, credentials.headers);
      request.subscribe({
        next: message => this.addLocalBotMessage(message),
        error: error => {
          console.error(`Unable to load ${type} for chat.`, error);
          this.addLocalBotMessage(this.getAccountDataError(error, type === 'bills' ? 'credit card bills' : 'gold loan details'));
        },
        complete: () => this.isLoading = false
      });
    }).catch(error => {
      console.error(`Unable to initialize ${type} lookup.`, error);
      this.addLocalBotMessage(`NeoBank could not load your ${type === 'bills' ? 'credit card bills' : 'gold loan details'} right now. Please retry shortly.`);
      this.isLoading = false;
    });
  }

  openDashboardFeature(feature: string): void {
    this.showMoreServices = false;
    this.isOpen = false;
    this.navigateToFeature.emit(feature);
  }

  private getCustomerCredentials(): { accountNumber: string; headers: HttpHeaders } | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    let user: any;
    try {
      user = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
    } catch (error) {
      console.error('Unable to read the customer session for chat account details.', error);
      this.addLocalBotMessage('Your sign-in session could not be read. Please sign in again to view account details.');
      return null;
    }
    const token = user.authToken || sessionStorage.getItem('jwt');
    const accountNumber = String(user.accountNumber || '').trim();
    if (!token || !accountNumber) {
      this.addLocalBotMessage('Please sign in again to securely view your account details.');
      return null;
    }
    return {
      accountNumber,
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` })
    };
  }

  private addLocalBotMessage(message: string): void {
    this.messages.push({
      sessionId: this.sessionId,
      userId: this.userId,
      userName: 'Bot',
      message,
      sender: 'BOT',
      timestamp: new Date().toISOString(),
      localOnly: true
    });
    this.scrollToBottom();
  }

  private getAccountDataError(error: any, label: string): string {
    if (error?.status === 423) {
      return `Access to ${label} is currently disabled for your account. Contact NeoBank support if you need help.`;
    }
    if (error?.status === 401 || error?.status === 403) {
      return `Your session is not authorized to view ${label}. Please sign in again and retry.`;
    }
    if (error?.status === 0) {
      return `Could not connect to NeoBank to load ${label}. Check your connection and retry.`;
    }
    return `NeoBank could not load ${label} right now. Please retry shortly.`;
  }
  
  getAttachmentUrl(attachmentUrl?: string): string {
    if (!attachmentUrl) return '';
    return this.messages.find(message => message.attachmentUrl === attachmentUrl)?.renderedAttachmentUrl || '';
  }

  private loadAttachmentPreviews(messages: ChatMessage[]): void {
    messages.filter(message => message.attachmentUrl && !message.renderedAttachmentUrl).forEach(message => {
      this.chatService.getAttachment(message.attachmentUrl!).subscribe({
        next: blob => {
          const objectUrl = URL.createObjectURL(blob);
          this.attachmentObjectUrls.push(objectUrl);
          message.renderedAttachmentUrl = objectUrl;
        },
        error: error => {
          console.error('Unable to load chat attachment.', error);
          if (error.status === 401 && !this.authErrorShown) {
            this.authErrorShown = true;
            this.isAuthenticated = false;
          }
        }
      });
    });
  }
}
