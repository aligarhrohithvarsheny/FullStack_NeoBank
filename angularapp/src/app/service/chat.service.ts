import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environment/environment';

export interface ChatMessage {
  id?: number;
  sessionId: string;
  userId: string;
  userName: string;
  message: string;
  sender: 'USER' | 'BOT' | 'ADMIN' | 'SYSTEM';
  timestamp?: string;
  status?: string;
  adminId?: string;
  isRead?: boolean;
  attachmentUrl?: string;
  attachmentType?: string;
  attachmentName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = `${environment.apiUrl}/chat`;
  private socket: any;
  private messageSubject = new Subject<ChatMessage>();
  public messages$ = this.messageSubject.asObservable();

  constructor(private http: HttpClient) {
    this.initializeSocket();
    console.log('ChatService initialized with API URL:', this.apiUrl);
  }

  private initializeSocket() {
    // Using SockJS and STOMP for WebSocket communication
    // For now, we'll use HTTP polling, but WebSocket can be added later
  }

  sendMessage(sessionId: string, userId: string, userName: string, message: string, file?: File): Observable<any> {
    const url = `${this.apiUrl}/send`;
    const formData = new FormData();
    formData.append('sessionId', sessionId);
    formData.append('userId', userId);
    formData.append('userName', userName);
    if (message) {
      formData.append('message', message);
    }
    if (file) {
      formData.append('file', file);
    }
    
    console.log('Sending message to:', url);
    
    return this.http.post<any>(url, formData);
  }

  adminSendMessage(sessionId: string, adminId: string, message: string, file?: File): Observable<any> {
    const url = `${this.apiUrl}/admin/send`;
    const formData = new FormData();
    formData.append('sessionId', sessionId);
    formData.append('adminId', adminId);
    if (message) {
      formData.append('message', message);
    }
    if (file) {
      formData.append('file', file);
    }
    
    return this.http.post<any>(url, formData);
  }
  
  getAttachmentUrl(filename: string): string {
    return `${this.apiUrl}/attachment/${filename}`;
  }

  getChatHistory(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/history/${sessionId}`);
  }

  getEscalatedChats(): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/admin/escalated`);
  }

  getAllChats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/all`);
  }

  adminTakeover(sessionId: string, adminId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/takeover`, {
      sessionId,
      adminId
    });
  }

  emitMessage(message: ChatMessage) {
    this.messageSubject.next(message);
  }
}

