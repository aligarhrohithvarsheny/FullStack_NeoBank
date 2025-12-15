import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatMessage } from '../../../service/chat.service';
import { AlertService } from '../../../service/alert.service';
import { UserService } from '../../../service/user';
import { AccountService } from '../../../service/account';
import { Router } from '@angular/router';
import { environment } from '../../../../environment/environment';

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
  adminId: string = 'admin';
  isLoading: boolean = false;
  selectedFile: File | null = null;
  showAccountDetails: boolean = false;
  customerDetails: any = null;
  isLoadingCustomerDetails: boolean = false;
  private refreshInterval: any;

  constructor(
    private chatService: ChatService,
    private alertService: AlertService,
    private userService: UserService,
    private accountService: AccountService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadEscalatedChats();
    
    // Refresh escalated chats every 5 seconds
    this.refreshInterval = setInterval(() => {
      this.loadEscalatedChats();
      if (this.selectedSessionId) {
        this.loadChatHistory(this.selectedSessionId);
      }
    }, 5000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadEscalatedChats() {
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
      }
    });
  }

  selectChat(sessionId: string) {
    this.selectedSessionId = sessionId;
    this.loadChatHistory(sessionId);
    
    // Take over the chat
    this.chatService.adminTakeover(sessionId, this.adminId).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.success('Chat', 'You have taken over this chat');
        }
      },
      error: (err) => {
        console.error('Error taking over chat:', err);
      }
    });
  }

  loadChatHistory(sessionId: string) {
    this.chatService.getChatHistory(sessionId).subscribe({
      next: (messages) => {
        // Deduplicate messages by ID to prevent duplicates
        const uniqueMessages = messages.filter((msg, index, self) => 
          index === self.findIndex((m) => m.id === msg.id)
        );
        this.selectedChatMessages = uniqueMessages;
        this.scrollToBottom();
      },
      error: (err) => {
        console.error('Error loading chat history:', err);
      }
    });
  }

  sendMessage() {
    if ((!this.newMessage.trim() && !this.selectedFile) || this.isLoading || !this.selectedSessionId) {
      return;
    }

    const messageText = this.newMessage.trim();
    const fileToSend = this.selectedFile;
    this.newMessage = '';
    this.selectedFile = null;
    this.isLoading = true;

    this.chatService.adminSendMessage(this.selectedSessionId, this.adminId, messageText, fileToSend || undefined).subscribe({
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
    if (attachmentUrl.startsWith('http')) {
      return attachmentUrl;
    }
    // Extract filename from path
    const filename = attachmentUrl.split('/').pop() || '';
    return `${environment.apiUrl}/chat/attachment/${filename}`;
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

