import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PositivePayService } from '../../../service/positive-pay.service';

@Component({ selector: 'app-admin-positive-pay', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './admin-positive-pay.component.html', styleUrls: ['./admin-positive-pay.component.css'] })
export class AdminPositivePayComponent implements OnInit {
  rows: any[] = []; stats: any = {}; status = ''; page = 0; totalPages = 0; selected: any = null; remark = ''; rejectReason = ''; loading = false; error = '';
  constructor(private service: PositivePayService) {}
  ngOnInit() { this.refresh(); }
  refresh() { this.loading=true; this.service.adminStats().subscribe(r=>this.stats=r); this.service.adminList(this.status || undefined,this.page,20).subscribe({next:r=>{this.rows=r.content||[];this.totalPages=r.totalPages||0;this.loading=false},error:e=>{this.error=e.error?.message||'Unable to load Positive Pay requests';this.loading=false}}); }
  filter() { this.page=0; this.refresh(); }
  view(r:any) { this.service.adminDetails(r.referenceNumber).subscribe({next:x=>this.selected=x,error:e=>this.error=e.error?.message||'Unable to load details'}); }
  approve() { if(!this.selected)return; this.service.approve(this.selected.request.referenceNumber,this.remark).subscribe({next:()=>{this.selected=null;this.remark='';this.refresh()},error:e=>this.error=e.error?.message||'Approval failed'}); }
  reject() { if(!this.selected || !this.rejectReason.trim()) { this.error='Rejection reason is required'; return; } this.service.reject(this.selected.request.referenceNumber,this.rejectReason).subscribe({next:()=>{this.selected=null;this.rejectReason='';this.refresh()},error:e=>this.error=e.error?.message||'Rejection failed'}); }
}
