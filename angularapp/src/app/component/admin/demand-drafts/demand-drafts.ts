import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; import { FormsModule } from '@angular/forms';
import { DemandDraftService } from '../../../service/demand-draft.service';
import { environment } from '../../../../environment/environment';
@Component({selector:'app-admin-demand-drafts',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./demand-drafts.html',styleUrls:['./demand-drafts.css']})
export class AdminDemandDrafts implements OnInit {
 drafts:any[]=[]; filteredDrafts:any[]=[]; selected:any=null; edit:any={}; adminName='Admin'; search=''; loading=false; saving=false; signature:any=null;
 constructor(private service:DemandDraftService){} ngOnInit(){this.load();}
 signatureUrl(){ return this.signature ? `${environment.apiBaseUrl}/api/admin/signature-management/view/${this.signature.accountType}/${this.signature.accountId}` : ''; }
 load(){this.loading=true;this.service.getAll().subscribe({next:x=>{this.drafts=x||[];this.filter();this.loading=false;},error:()=>this.loading=false});}
 filter(){const q=this.search.trim().toLowerCase();this.filteredDrafts=!q?this.drafts:this.drafts.filter(d=>[d.ddNumber,d.chequeNumber,d.userName,d.userEmail,d.accountNumber,d.payeeName].some(v=>String(v||'').toLowerCase().includes(q)));}
 select(d:any){this.selected=d;this.edit={...d};this.signature=null;this.service.getAccountSignature(d.accountNumber).subscribe({next: x=>this.signature=x,error:()=>this.signature=null});}
 save(){if(!this.selected)return;this.saving=true;this.service.update(this.selected.id,this.edit,this.adminName).subscribe({next:x=>{this.selected=x;this.saving=false;this.load();},error:()=>this.saving=false});}
 approve(){if(!this.selected)return;this.saving=true;this.service.approve(this.selected.id,this.adminName).subscribe(()=>{this.selected=null;this.saving=false;this.load();},()=>this.saving=false);}
 reject(){if(!this.selected)return;this.service.reject(this.selected.id,this.adminName,'Rejected by admin').subscribe(()=>{this.selected=null;this.load();});}
 download(id:number){this.service.download(id).subscribe(blob=>{const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`demand-draft-${id}.pdf`;a.click();URL.revokeObjectURL(url);});}
}
