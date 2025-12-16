import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AlertService } from '../../../service/alert.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-assistant.html',
  styleUrls: ['./ai-assistant.css']
})
export class AIAssistant implements OnInit {
  userAccountNumber: string = '';
  loading: boolean = false;
  analysisData: any = null;
  activeTab: string = 'insights';
  error: string = '';

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserAccount();
    }
  }

  loadUserAccount() {
    const currentUser = sessionStorage.getItem('currentUser');
    if (currentUser) {
      const user = JSON.parse(currentUser);
      this.userAccountNumber = user.accountNumber;
      this.analyzeSpending();
    } else {
      this.error = 'User session not found. Please login again.';
    }
  }

  analyzeSpending() {
    if (!this.userAccountNumber) {
      this.error = 'Account number not found';
      return;
    }

    this.loading = true;
    this.error = '';

    this.http.get(`${environment.apiBaseUrl}/ai/analyze/${this.userAccountNumber}`).subscribe({
      next: (data: any) => {
        this.analysisData = data;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error analyzing spending:', err);
        this.error = 'Failed to analyze spending. Please try again later.';
        this.loading = false;
        this.alertService.error('Analysis Error', this.error);
      }
    });
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  formatCurrency(amount: number): string {
    return `₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  getInsightClass(type: string): string {
    switch (type) {
      case 'warning': return 'insight-warning';
      case 'success': return 'insight-success';
      case 'info': return 'insight-info';
      default: return 'insight-default';
    }
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'high': return 'priority-high';
      case 'medium': return 'priority-medium';
      case 'low': return 'priority-low';
      default: return 'priority-default';
    }
  }

  getCategoryEntries(): any[] {
    if (!this.analysisData || !this.analysisData.categorySpending) {
      return [];
    }

    const categories = this.analysisData.categorySpending;
    const total = Object.values(categories).reduce((sum: number, val: any) => sum + val, 0);
    
    return Object.entries(categories)
      .map(([name, amount]: [string, any]) => ({
        name,
        amount,
        percentage: total > 0 ? (amount / total) * 100 : 0
      }))
      .sort((a, b) => b.amount - a.amount);
  }

  hasCategorySpending(): boolean {
    return this.analysisData && 
           this.analysisData.categorySpending && 
           Object.keys(this.analysisData.categorySpending).length > 0;
  }
}

