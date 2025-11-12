import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card as CardModel, CreateCardRequest, UpdateCardRequest, SetPinRequest, ChangePinRequest, CardSearchRequest, CardStatistics } from '../model/card/card-module';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class CardService {
  private apiUrl = `${environment.apiUrl}/cards`;

  constructor(private http: HttpClient) { }

  // Basic CRUD operations
  createCard(card: CreateCardRequest): Observable<CardModel> {
    return this.http.post<CardModel>(`${this.apiUrl}`, card);
  }

  getCardById(id: number): Observable<CardModel> {
    return this.http.get<CardModel>(`${this.apiUrl}/${id}`);
  }

  updateCard(id: number, cardDetails: UpdateCardRequest): Observable<CardModel> {
    return this.http.put<CardModel>(`${this.apiUrl}/${id}`, cardDetails);
  }

  deleteCard(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Card-specific operations
  getCardsByAccountNumber(accountNumber: string): Observable<CardModel[]> {
    return this.http.get<CardModel[]>(`${this.apiUrl}/account/${accountNumber}`);
  }

  getCardsByUserEmail(userEmail: string): Observable<CardModel[]> {
    return this.http.get<CardModel[]>(`${this.apiUrl}/email/${userEmail}`);
  }

  getCardsByUserName(userName: string): Observable<CardModel[]> {
    return this.http.get<CardModel[]>(`${this.apiUrl}/user/${userName}`);
  }

  // Status-based operations
  getCardsByStatus(status: string): Observable<CardModel[]> {
    return this.http.get<CardModel[]>(`${this.apiUrl}/status/${status}`);
  }

  getCardsByStatusWithPagination(status: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/status/${status}/paginated`, { params });
  }

  // Type-based operations
  getCardsByType(cardType: string): Observable<CardModel[]> {
    return this.http.get<CardModel[]>(`${this.apiUrl}/type/${cardType}`);
  }

  getCardsByTypeWithPagination(cardType: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/type/${cardType}/paginated`, { params });
  }

  // Special status operations
  getBlockedCards(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/blocked`, { params });
  }

  getDeactivatedCards(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/deactivated`, { params });
  }

  getActiveCards(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/active`, { params });
  }

  // PIN operations
  setPin(setPinRequest: SetPinRequest): Observable<CardModel> {
    return this.http.put<CardModel>(`${this.apiUrl}/${setPinRequest.cardId}/set-pin`, setPinRequest);
  }

  changePin(changePinRequest: ChangePinRequest): Observable<CardModel> {
    return this.http.put<CardModel>(`${this.apiUrl}/${changePinRequest.cardId}/change-pin`, changePinRequest);
  }

  // Card management operations
  blockCard(cardId: number, reason: string): Observable<CardModel> {
    const params = new HttpParams().set('reason', reason);
    return this.http.put<CardModel>(`${this.apiUrl}/${cardId}/block`, null, { params });
  }

  unblockCard(cardId: number): Observable<CardModel> {
    return this.http.put<CardModel>(`${this.apiUrl}/${cardId}/unblock`, null);
  }

  deactivateCard(cardId: number, reason: string): Observable<CardModel> {
    const params = new HttpParams().set('reason', reason);
    return this.http.put<CardModel>(`${this.apiUrl}/${cardId}/deactivate`, null, { params });
  }

  activateCard(cardId: number): Observable<CardModel> {
    return this.http.put<CardModel>(`${this.apiUrl}/${cardId}/activate`, null);
  }

  // Expiry operations
  getCardsExpiringSoon(days: number = 30): Observable<CardModel[]> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<CardModel[]>(`${this.apiUrl}/expiring-soon`, { params });
  }

  // Search operations
  searchCards(searchTerm: string, page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Statistics operations
  getCardStatistics(): Observable<CardStatistics> {
    return this.http.get<CardStatistics>(`${this.apiUrl}/stats`);
  }

  getRecentCards(limit: number = 5): Observable<CardModel[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<CardModel[]>(`${this.apiUrl}/recent`, { params });
  }

  // Advanced search with multiple filters
  searchCardsAdvanced(searchRequest: CardSearchRequest): Observable<any> {
    let params = new HttpParams();
    
    if (searchRequest.searchTerm) params = params.set('searchTerm', searchRequest.searchTerm);
    if (searchRequest.cardType) params = params.set('cardType', searchRequest.cardType);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.pinSet !== undefined) params = params.set('pinSet', searchRequest.pinSet.toString());
    if (searchRequest.blocked !== undefined) params = params.set('blocked', searchRequest.blocked.toString());
    if (searchRequest.deactivated !== undefined) params = params.set('deactivated', searchRequest.deactivated.toString());
    if (searchRequest.userName) params = params.set('userName', searchRequest.userName);
    if (searchRequest.accountNumber) params = params.set('accountNumber', searchRequest.accountNumber);
    if (searchRequest.userEmail) params = params.set('userEmail', searchRequest.userEmail);
    if (searchRequest.issuedFrom) params = params.set('issuedFrom', searchRequest.issuedFrom);
    if (searchRequest.issuedTo) params = params.set('issuedTo', searchRequest.issuedTo);
    if (searchRequest.expiresFrom) params = params.set('expiresFrom', searchRequest.expiresFrom);
    if (searchRequest.expiresTo) params = params.set('expiresTo', searchRequest.expiresTo);
    if (searchRequest.page) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size) params = params.set('size', searchRequest.size.toString());
    if (searchRequest.sortBy) params = params.set('sortBy', searchRequest.sortBy);
    if (searchRequest.sortDirection) params = params.set('sortDirection', searchRequest.sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  // Card generation
  generateCardNumber(): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/generate-number`);
  }

  generateCVV(): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/generate-cvv`);
  }

  // Export cards
  exportCards(status?: string, cardType?: string): Observable<Blob> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (cardType) params = params.set('cardType', cardType);
    
    return this.http.get(`${this.apiUrl}/export`, { 
      params, 
      responseType: 'blob' 
    });
  }
}
