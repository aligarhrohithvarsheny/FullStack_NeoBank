import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MerchantOnboardingService } from '../../../service/merchant-onboarding.service';
import { AlertService } from '../../../service/alert.service';


@Component({
  selector: 'app-agent-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './agent-management.html',
  styleUrls: ['./agent-management.css'],
  encapsulation: ViewEncapsulation.None
})
export class AgentManagement implements OnInit {
  agents: any[] = [];
  searchTerm = '';
  statusFilter = 'ALL';
  loading = false;

  stats = { total: 0, active: 0, frozen: 0, deactivated: 0 };

  // Create Agent Modal
  showCreateModal = false;
  newAgent = {
    name: '', email: '', mobile: '', password: '', confirmPassword: '',
    role: 'FIELD_AGENT', bio: '', region: ''
  };
  createErrors: { [key: string]: string } = {};

  // View/Edit Agent
  selectedAgent: any = null;
  editMode = false;
  editAgent = { name: '', email: '', mobile: '', role: '', bio: '', region: '' };

  // OTP
  showOtpModal = false;
  otpAgentId = '';
  generatedOtp = '';

  // ID Card Upload
  idCardFile: File | null = null;
  photoFile: File | null = null;

  roles = ['FIELD_AGENT', 'SENIOR_AGENT', 'TEAM_LEAD', 'REGIONAL_MANAGER'];

  constructor(
    private merchantService: MerchantOnboardingService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadAll();
  }

  loadAll() {
    this.merchantService.getAllAgents().subscribe({
      next: (res: any) => {
        if (res.success) this.agents = res.agents || [];
      }
    });
    this.merchantService.getAgentManagementStats().subscribe({
      next: (res: any) => {
        if (res.success) this.stats = res;
      }
    });
  }

  get filteredAgents(): any[] {
    const q = (this.searchTerm || '').toLowerCase();
    return this.agents.filter(a => {
      const matchQ = !q || (a.name || '').toLowerCase().includes(q)
        || (a.agentId || '').toLowerCase().includes(q)
        || (a.email || '').toLowerCase().includes(q)
        || (a.mobile || '').includes(q)
        || (a.region || '').toLowerCase().includes(q);
      const matchStatus = this.statusFilter === 'ALL' || a.status === this.statusFilter;
      return matchQ && matchStatus;
    });
  }

  // ==================== Create Agent ====================

  openCreateModal() {
    this.showCreateModal = true;
    this.newAgent = { name: '', email: '', mobile: '', password: '', confirmPassword: '', role: 'FIELD_AGENT', bio: '', region: '' };
    this.createErrors = {};
  }

  closeCreateModal() {
    this.showCreateModal = false;
    this.createErrors = {};
  }

  validateCreate(): boolean {
    this.createErrors = {};
    if (!this.newAgent.name.trim()) this.createErrors['name'] = 'Name is required';
    if (!this.newAgent.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.newAgent.email))
      this.createErrors['email'] = 'Valid email is required';
    if (!this.newAgent.mobile.trim() || !/^[6-9]\d{9}$/.test(this.newAgent.mobile))
      this.createErrors['mobile'] = 'Valid 10-digit mobile is required';
    if (!this.newAgent.password || this.newAgent.password.length < 6)
      this.createErrors['password'] = 'Password must be at least 6 characters';
    if (this.newAgent.password !== this.newAgent.confirmPassword)
      this.createErrors['confirmPassword'] = 'Passwords do not match';
    return Object.keys(this.createErrors).length === 0;
  }

  submitCreate() {
    if (!this.validateCreate()) return;
    this.loading = true;
    const payload = {
      name: this.newAgent.name.trim(),
      email: this.newAgent.email.trim(),
      mobile: this.newAgent.mobile.trim(),
      password: this.newAgent.password,
      role: this.newAgent.role,
      bio: this.newAgent.bio.trim(),
      region: this.newAgent.region.trim()
    };
    this.merchantService.createAgent(payload).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) {
          this.alertService.success('Success', 'Agent account created successfully');
          this.closeCreateModal();
          this.loadAll();
        } else {
          this.alertService.error('Error', res.message || 'Failed to create agent');
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.message || 'Failed to create agent');
      }
    });
  }

  // ==================== View / Edit Agent ====================

  viewAgent(agent: any) {
    this.selectedAgent = { ...agent };
    this.editMode = false;
  }

  closeAgent() {
    this.selectedAgent = null;
    this.editMode = false;
  }

  toggleEdit() {
    this.editMode = !this.editMode;
    if (this.editMode) {
      this.editAgent = {
        name: this.selectedAgent.name,
        email: this.selectedAgent.email,
        mobile: this.selectedAgent.mobile,
        role: this.selectedAgent.role,
        bio: this.selectedAgent.bio || '',
        region: this.selectedAgent.region || ''
      };
    }
  }

  saveEdit() {
    this.loading = true;
    this.merchantService.updateAgentProfile(this.selectedAgent.agentId, this.editAgent).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) {
          this.alertService.success('Success', 'Agent profile updated');
          this.selectedAgent = { ...this.selectedAgent, ...this.editAgent };
          this.editMode = false;
          this.loadAll();
        } else {
          this.alertService.error('Error', res.message || 'Update failed');
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.message || 'Update failed');
      }
    });
  }

  // ==================== Status Actions ====================

  freezeAgent(agentId: string) {
    this.alertService.confirm('Freeze Agent', 'Are you sure you want to freeze this agent?', () => {
      this.merchantService.freezeAgent(agentId).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.alertService.success('Frozen', 'Agent account has been frozen');
            this.loadAll();
            if (this.selectedAgent?.agentId === agentId) this.selectedAgent.status = 'FROZEN';
          }
        }
      });
    });
  }

  unfreezeAgent(agentId: string) {
    this.merchantService.unfreezeAgent(agentId).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.success('Unfrozen', 'Agent account has been unfrozen');
          this.loadAll();
          if (this.selectedAgent?.agentId === agentId) this.selectedAgent.status = 'ACTIVE';
        }
      }
    });
  }

  deactivateAgent(agentId: string) {
    this.alertService.confirm('Deactivate Agent', 'This will deactivate the agent account. Continue?', () => {
      this.merchantService.deactivateAgent(agentId).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.alertService.success('Deactivated', 'Agent account has been deactivated');
            this.loadAll();
            if (this.selectedAgent?.agentId === agentId) this.selectedAgent.status = 'DEACTIVATED';
          }
        }
      });
    });
  }

  reactivateAgent(agentId: string) {
    this.merchantService.reactivateAgent(agentId).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.alertService.success('Reactivated', 'Agent account has been reactivated');
          this.loadAll();
          if (this.selectedAgent?.agentId === agentId) this.selectedAgent.status = 'ACTIVE';
        }
      }
    });
  }

  // ==================== OTP ====================

  openOtpModal(agentId: string) {
    this.showOtpModal = true;
    this.otpAgentId = agentId;
    this.generatedOtp = '';
  }

  closeOtpModal() {
    this.showOtpModal = false;
    this.generatedOtp = '';
  }

  generateOtp() {
    this.loading = true;
    this.merchantService.generateAgentOtp(this.otpAgentId).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) {
          this.generatedOtp = res.otp;
        } else {
          this.alertService.error('Error', res.message || 'Failed to generate OTP');
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.alertService.error('Error', err.error?.message || 'Failed to generate OTP');
      }
    });
  }

  // ==================== File Uploads ====================

  onIdCardSelect(event: any) {
    this.idCardFile = event.target.files?.[0] || null;
  }

  onPhotoSelect(event: any) {
    this.photoFile = event.target.files?.[0] || null;
  }

  uploadIdCard() {
    if (!this.idCardFile || !this.selectedAgent) return;
    this.loading = true;
    this.merchantService.uploadAgentIdCard(this.selectedAgent.agentId, this.idCardFile).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) {
          this.alertService.success('Uploaded', 'ID card uploaded successfully');
          this.selectedAgent.idCardPath = res.path || 'uploaded';
          this.idCardFile = null;
        }
      },
      error: () => {
        this.loading = false;
        this.alertService.error('Error', 'Failed to upload ID card');
      }
    });
  }

  uploadPhoto() {
    if (!this.photoFile || !this.selectedAgent) return;
    this.loading = true;
    this.merchantService.uploadAgentPhoto(this.selectedAgent.agentId, this.photoFile).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) {
          this.alertService.success('Uploaded', 'Photo uploaded successfully');
          this.selectedAgent.profilePhotoPath = res.path || 'uploaded';
          this.photoFile = null;
        }
      },
      error: () => {
        this.loading = false;
        this.alertService.error('Error', 'Failed to upload photo');
      }
    });
  }

  // ==================== Helpers ====================

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'FROZEN': return 'status-frozen';
      case 'DEACTIVATED': return 'status-deactivated';
      default: return '';
    }
  }

  getRoleBadge(role: string): string {
    switch (role) {
      case 'FIELD_AGENT': return 'role-field';
      case 'SENIOR_AGENT': return 'role-senior';
      case 'TEAM_LEAD': return 'role-lead';
      case 'REGIONAL_MANAGER': return 'role-manager';
      default: return '';
    }
  }

  formatDate(date: string): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  goBack() {
    history.back();
  }
}
