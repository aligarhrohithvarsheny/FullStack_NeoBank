import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AiSecurityService } from '../../../service/ai-security.service';
import { AlertService } from '../../../service/alert.service';
import {
  AiSecurityDashboard as AiSecurityDashboardModel,
  AiSecurityEvent,
  AiSecurityRule,
  AiThreatScore,
  AiDeviceFingerprint
} from '../../../model/ai-security.model';

@Component({
  selector: 'app-ai-security-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-security-dashboard.html',
  styleUrls: ['./ai-security-dashboard.css']
})
export class AiSecurityDashboardComponent implements OnInit, OnDestroy {
  dashboard: AiSecurityDashboardModel | null = null;
  isLoading = true;
  activeTab = 'overview';
  refreshInterval: any;

  // Events
  events: AiSecurityEvent[] = [];
  eventPage = 0;
  eventTotalPages = 0;
  eventTotalElements = 0;
  eventFilters = { eventType: '', channel: '', severity: '', status: '' };
  isLoadingEvents = false;

  // Selected event for detail
  selectedEvent: AiSecurityEvent | null = null;
  resolveNotes = '';

  // Threat lookup
  threatLookupId = '';
  threatLookupResult: AiThreatScore | null = null;
  threatDevices: AiDeviceFingerprint[] = [];

  // Rules
  rules: AiSecurityRule[] = [];
  isLoadingRules = false;

  // Watchlist form
  watchlistEntityId = '';
  watchlistReason = '';

  eventTypes = [
    'SUSPICIOUS_LOGIN', 'BRUTE_FORCE_ATTACK', 'IMPOSSIBLE_TRAVEL',
    'ANOMALOUS_TRANSACTION', 'RAPID_FIRE_TRANSACTIONS', 'SESSION_HIJACK',
    'NEW_DEVICE_DETECTED', 'MASS_DATA_ACCESS', 'DORMANT_ACCOUNT_ACTIVITY',
    'PHISHING_ATTEMPT', 'OFF_HOURS_ACTIVITY', 'PRIVILEGE_ESCALATION',
    'API_ABUSE', 'CREDENTIAL_STUFFING', 'ACCOUNT_TAKEOVER'
  ];
  channels = ['WEB', 'MOBILE', 'API', 'ATM', 'NET_BANKING', 'UPI', 'ADMIN_PORTAL', 'ALL'];
  severities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  statuses = ['DETECTED', 'ANALYZING', 'BLOCKED', 'ESCALATED', 'RESOLVED', 'FALSE_POSITIVE'];

  constructor(
    private aiSecurityService: AiSecurityService,
    private alertService: AlertService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
    this.refreshInterval = setInterval(() => this.loadDashboard(), 30000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  loadDashboard(): void {
    this.aiSecurityService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.alertService.error('Error', 'Failed to load AI Security dashboard');
      }
    });
  }

  setTab(tab: string): void {
    this.activeTab = tab;
    if (tab === 'events' && this.events.length === 0) this.loadEvents();
    if (tab === 'rules' && this.rules.length === 0) this.loadRules();
  }

  // EVENTS
  loadEvents(): void {
    this.isLoadingEvents = true;
    this.aiSecurityService.getEvents({
      ...this.eventFilters,
      page: this.eventPage,
      size: 15
    }).subscribe({
      next: (page) => {
        this.events = page.content;
        this.eventTotalPages = page.totalPages;
        this.eventTotalElements = page.totalElements;
        this.isLoadingEvents = false;
      },
      error: () => { this.isLoadingEvents = false; }
    });
  }

  filterEvents(): void {
    this.eventPage = 0;
    this.loadEvents();
  }

  clearFilters(): void {
    this.eventFilters = { eventType: '', channel: '', severity: '', status: '' };
    this.eventPage = 0;
    this.loadEvents();
  }

  goToEventPage(page: number): void {
    if (page >= 0 && page < this.eventTotalPages) {
      this.eventPage = page;
      this.loadEvents();
    }
  }

  selectEvent(event: AiSecurityEvent): void {
    this.selectedEvent = event;
    this.resolveNotes = '';
  }

  closeEventDetail(): void {
    this.selectedEvent = null;
  }

  resolveEvent(status: string): void {
    if (!this.selectedEvent) return;
    const adminEmail = sessionStorage.getItem('adminEmail') || 'admin';
    this.aiSecurityService.updateEventStatus(this.selectedEvent.id, status, adminEmail, this.resolveNotes).subscribe({
      next: (updated) => {
        this.alertService.success('Success', `Event marked as ${status}`);
        this.selectedEvent = updated;
        this.loadEvents();
        this.loadDashboard();
      },
      error: () => this.alertService.error('Error', 'Failed to update event')
    });
  }

  // THREAT LOOKUP
  lookupThreat(): void {
    if (!this.threatLookupId.trim()) return;
    this.aiSecurityService.getThreatScore(this.threatLookupId.trim()).subscribe({
      next: (score) => { this.threatLookupResult = score; },
      error: () => { this.threatLookupResult = null; }
    });
    this.aiSecurityService.getDevices(this.threatLookupId.trim()).subscribe({
      next: (devices) => { this.threatDevices = devices; },
      error: () => { this.threatDevices = []; }
    });
  }

  toggleDeviceTrust(device: AiDeviceFingerprint): void {
    this.aiSecurityService.setDeviceTrust(device.id, !device.isTrusted).subscribe({
      next: () => {
        this.alertService.success('Success', `Device ${device.isTrusted ? 'untrusted' : 'trusted'}`);
        this.lookupThreat();
      }
    });
  }

  addToWatchlist(): void {
    if (!this.watchlistEntityId.trim()) return;
    this.aiSecurityService.toggleWatchlist(this.watchlistEntityId.trim(), 'USER', true, this.watchlistReason).subscribe({
      next: () => {
        this.alertService.success('Success', 'Entity added to watchlist');
        this.watchlistEntityId = '';
        this.watchlistReason = '';
        this.loadDashboard();
      }
    });
  }

  removeFromWatchlist(entity: AiThreatScore): void {
    this.aiSecurityService.toggleWatchlist(entity.entityId, entity.entityType, false, '').subscribe({
      next: () => {
        this.alertService.success('Success', 'Removed from watchlist');
        this.loadDashboard();
      }
    });
  }

  // RULES
  loadRules(): void {
    this.isLoadingRules = true;
    this.aiSecurityService.getRules().subscribe({
      next: (rules) => { this.rules = rules; this.isLoadingRules = false; },
      error: () => { this.isLoadingRules = false; }
    });
  }

  toggleRule(rule: AiSecurityRule): void {
    this.aiSecurityService.toggleRule(rule.id, !rule.isActive).subscribe({
      next: (updated) => {
        const idx = this.rules.findIndex(r => r.id === rule.id);
        if (idx >= 0) this.rules[idx] = updated;
        this.alertService.success('Success', `Rule ${updated.isActive ? 'enabled' : 'disabled'}`);
      }
    });
  }

  // HELPERS
  getSeverityClass(severity: string): string {
    switch(severity) {
      case 'CRITICAL': return 'severity-critical';
      case 'HIGH': return 'severity-high';
      case 'MEDIUM': return 'severity-medium';
      default: return 'severity-low';
    }
  }

  getStatusClass(status: string): string {
    switch(status) {
      case 'BLOCKED': return 'status-blocked';
      case 'ESCALATED': return 'status-escalated';
      case 'RESOLVED': return 'status-resolved';
      case 'FALSE_POSITIVE': return 'status-false-positive';
      default: return 'status-detected';
    }
  }

  getRiskColor(score: number): string {
    if (score >= 85) return '#dc3545';
    if (score >= 70) return '#fd7e14';
    if (score >= 40) return '#ffc107';
    return '#28a745';
  }

  formatDate(date: string): string {
    if (!date) return '-';
    return new Date(date).toLocaleString();
  }

  getChannelIcon(channel: string): string {
    const icons: {[k:string]:string} = {
      'WEB': 'fa-globe', 'MOBILE': 'fa-mobile-alt', 'API': 'fa-code',
      'ATM': 'fa-university', 'NET_BANKING': 'fa-laptop', 'UPI': 'fa-qrcode',
      'ADMIN_PORTAL': 'fa-user-shield', 'ALL': 'fa-layer-group'
    };
    return icons[channel] || 'fa-shield-alt';
  }

  getObjectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }

  goBack(): void {
    this.router.navigate(['/admin/dashboard']);
  }
}
