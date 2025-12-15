import { Component, OnInit, OnDestroy, ViewChild, ElementRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  
  messages: ChatMessage[] = [];
  newMessage: string = '';
  sessionId: string = '';
  userId: string = '';
  userName: string = '';
  isOpen: boolean = false;
  isLoading: boolean = false;
  selectedFile: File | null = null;
  private messageSubscription: any;
  private pollingInterval: any;

  constructor(
    private chatService: ChatService,
    private alertService: AlertService,
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
        } catch (e) {
          console.error('Error parsing user data:', e);
        }
      }
    }

    // Generate session ID
    this.sessionId = this.generateSessionId();
    
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
  }

  generateSessionId(): string {
    return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  loadChatHistory() {
    if (this.sessionId) {
      this.chatService.getChatHistory(this.sessionId).subscribe({
        next: (messages) => {
          // Only update if we have new messages (check by message count or last message timestamp)
          const lastMessageId = this.messages.length > 0 ? this.messages[this.messages.length - 1].id : 0;
          const newMessages = messages.filter(m => !m.id || m.id > (lastMessageId || 0));
          
          if (newMessages.length > 0 || this.messages.length !== messages.length) {
            this.messages = messages;
            this.scrollToBottom();
          }
        },
        error: (err) => {
          console.error('Error loading chat history:', err);
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

  sendMessage() {
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

    this.chatService.sendMessage(this.sessionId, this.userId, this.userName, messageText, fileToSend || undefined).subscribe({
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
  
  getAttachmentUrl(attachmentUrl?: string): string {
    if (!attachmentUrl) return '';
    if (attachmentUrl.startsWith('http')) {
      return attachmentUrl;
    }
    return `${environment.apiUrl}${attachmentUrl}`;
  }
}

