import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MerchantOnboardingService } from '../../../service/merchant-onboarding.service';
import { AlertService } from '../../../service/alert.service';
import { Agent, Merchant } from '../../../model/merchant-onboarding/merchant-onboarding.model';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './agent-dashboard.html',
  styleUrls: ['./agent-dashboard.css'],
  encapsulation: ViewEncapsulation.None
})
export class AgentDashboard implements OnInit {
  agent: Agent | null = null;
  merchants: Merchant[] = [];
  stats: any = {};
  statusFilter = 'ALL';
  searchTerm = '';
  activeTab = 'merchants';
  selectedMerchant: Merchant | null = null;
  loading = false;

  constructor(
    private router: Router,
    private merchantService: MerchantOnboardingService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    const agentData = sessionStorage.getItem('agent');
    if (!agentData) {
      this.router.navigate(['/website/agent-login']);
      return;
    }
    this.agent = JSON.parse(agentData);
    this.loadData();
  }

  loadData() {
    if (!this.agent?.agentId) return;
    this.loading = true;

    this.merchantService.getMerchantsByAgent(this.agent.agentId).subscribe({
      next: (data) => {
        this.merchants = data;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });

    this.merchantService.getAgentStats(this.agent.agentId).subscribe({
      next: (data) => this.stats = data
    });
  }

  get filteredMerchants(): Merchant[] {
    let list = this.merchants;
    if (this.statusFilter !== 'ALL') {
      list = list.filter(m => m.status === this.statusFilter);
    }
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      list = list.filter(m =>
        m.businessName?.toLowerCase().includes(term) ||
        m.ownerName?.toLowerCase().includes(term) ||
        m.merchantId?.toLowerCase().includes(term) ||
        m.mobile?.includes(term)
      );
    }
    return list;
  }

  addNewMerchant() {
    this.router.navigate(['/website/agent-add-merchant']);
  }

  editMerchant(merchant: Merchant) {
    if (merchant.status === 'PENDING') {
      this.router.navigate(['/website/agent-edit-merchant', merchant.merchantId]);
    }
  }

  viewMerchant(merchant: Merchant) {
    this.selectedMerchant = merchant;
  }

  closeDetail() {
    this.selectedMerchant = null;
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      case 'PENDING': return 'status-pending';
      default: return '';
    }
  }

  logout() {
    sessionStorage.removeItem('agent');
    this.router.navigate(['/website/agent-login']);
  }
}
