import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PositivePayService } from '../../../service/positive-pay.service';

@Component({ selector: 'app-admin-positive-pay', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './admin-positive-pay.component.html', styleUrls: ['./admin-positive-pay.component.css'] })
export class AdminPositivePayComponent implements OnInit {
  rows: any[] = []; stats: any = {}; status = ''; page = 0; totalPages = 0; selected: any = null; loading = false; error = '';
  constructor(private service: PositivePayService) {}
  ngOnInit() { this.refresh(); }
  refresh() { this.loading=true; this.error=''; this.service.adminStats().subscribe({next:r=>this.stats=r,error:e=>this.error=e.error?.message||'Unable to load Positive Pay statistics'}); this.service.adminList(this.status || undefined,this.page,20).subscribe({next:r=>{this.rows=r.content||[];this.totalPages=r.totalPages||0;this.loading=false},error:e=>{this.error=e.error?.message||'Unable to load Positive Pay requests';this.loading=false}}); }
  filter() { this.page=0; this.refresh(); }
  view(r:any) { this.error=''; this.service.adminDetails(r.referenceNumber).subscribe({next:x=>this.selected=x,error:e=>this.error=e.error?.message||'Unable to load details'}); }
  statusLabel(value: string, decidedBy?: string): string {
    if (value === 'APPROVED') return decidedBy === 'AI_SECURITY_ENGINE' ? 'AI PASS · INFORMATIONAL' : 'PASSED · INFORMATIONAL';
    if (value === 'REJECTED') return 'AI FLAG · INFORMATIONAL';
    return `${value.replace(/_/g, ' ')} · INFORMATIONAL`;
  }
}
