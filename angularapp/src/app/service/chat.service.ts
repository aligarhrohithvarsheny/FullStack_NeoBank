import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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
  renderedAttachmentUrl?: string;
  localOnly?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = `${environment.apiBaseUrl}/api/chat`;
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

  private getAuthOptions(admin = false): { headers: HttpHeaders } {
    if (typeof sessionStorage === 'undefined') {
      return { headers: new HttpHeaders() };
    }
    let token = admin ? sessionStorage.getItem('adminSessionToken') || '' : '';
    if (!admin) {
      try {
        const currentUser = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
        token = currentUser.authToken || sessionStorage.getItem('jwt') || '';
      } catch {
        token = sessionStorage.getItem('jwt') || '';
      }
    }
    return { headers: new HttpHeaders(token ? { Authorization: 'Bearer ' + token } : {}) };
  }

  sendMessage(sessionId: string, userName: string, message: string, file?: File): Observable<any> {
    const url = `${this.apiUrl}/send`;
    const formData = new FormData();
    formData.append('sessionId', sessionId);
    formData.append('userName', userName);
    if (message) {
      formData.append('message', message);
    }
    if (file) {
      formData.append('file', file);
    }
    
    console.log('Sending message to:', url);
    
    return this.http.post<any>(url, formData, this.getAuthOptions());
  }

  adminSendMessage(sessionId: string, message: string, file?: File): Observable<any> {
    const url = `${this.apiUrl}/admin/send`;
    const formData = new FormData();
    formData.append('sessionId', sessionId);
    if (message) {
      formData.append('message', message);
    }
    if (file) {
      formData.append('file', file);
    }
    
    return this.http.post<any>(url, formData, this.getAuthOptions(true));
  }
  
  getAttachment(filename: string, admin = false): Observable<Blob> {
    const safeFilename = filename.split('/').pop() || filename;
    return this.http.get(`${this.apiUrl}/attachment/${encodeURIComponent(safeFilename)}`, {
      ...this.getAuthOptions(admin),
      responseType: 'blob'
    });
  }

  getChatHistory(sessionId: string, admin = false): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(
      `${this.apiUrl}/history/${encodeURIComponent(sessionId)}`,
      this.getAuthOptions(admin)
    );
  }

  getEscalatedChats(): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/admin/escalated`, this.getAuthOptions(true));
  }

  getAllChats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/all`, this.getAuthOptions(true));
  }

  adminTakeover(sessionId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/takeover`, { sessionId }, this.getAuthOptions(true));
  }

  emitMessage(message: ChatMessage) {
    this.messageSubject.next(message);
  }
}
