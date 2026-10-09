import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; import { FormsModule } from '@angular/forms';
import { DemandDraftService } from '../../../service/demand-draft.service';
import { environment } from '../../../../environment/environment';
@Component({selector:'app-admin-demand-drafts',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./demand-drafts.html',styleUrls:['./demand-drafts.css']})
export class AdminDemandDrafts implements OnInit {
 drafts:any[]=[]; filteredDrafts:any[]=[]; selected:any=null; edit:any={}; adminName='Admin'; search=''; loading=false; saving=false; signature:any=null; error='';
 constructor(private service:DemandDraftService){} ngOnInit(){this.load();}
 signatureUrl(){ return this.signature ? `${environment.apiBaseUrl}/api/admin/signature-management/view/${this.signature.accountType}/${this.signature.accountId}` : ''; }
 load(){this.loading=true;this.service.getAll().subscribe({next:x=>{this.drafts=x||[];this.filter();this.loading=false;},error:e=>{this.error=e.error?.message||'Unable to load demand drafts';this.loading=false;}});}
 filter(){const q=this.search.trim().toLowerCase();this.filteredDrafts=!q?this.drafts:this.drafts.filter(d=>[d.ddNumber,d.chequeNumber,d.userName,d.userEmail,d.accountNumber,d.payeeName].some(v=>String(v||'').toLowerCase().includes(q)));}
 select(d:any){this.selected=d;this.edit={...d};this.error='';this.signature=null;this.service.getAccountSignature(d.accountNumber).subscribe({next: x=>this.signature=x,error:()=>this.signature=null});}
 save(){if(!this.selected)return;this.saving=true;this.error='';this.service.update(this.selected.id,this.edit,this.adminName).subscribe({next:x=>{this.selected=x;this.edit={...x};this.saving=false;this.load();},error:e=>{this.error=e.error?.message||'Unable to save DD changes';this.saving=false;}});}
 verifyAccountDetails(d:any=this.selected){if(!d)return;this.saving=true;this.error='';this.service.verifyAccountDetails(d.id,this.adminName).subscribe({next:x=>{this.replaceDraft(x);this.saving=false;},error:e=>{this.error=e.error?.message||'Unable to verify account details';this.saving=false;}});}
 approve(d:any=this.selected){if(!d)return;this.saving=true;this.error='';this.service.approve(d.id,this.adminName).subscribe({next:x=>{this.replaceDraft(x);this.saving=false;},error:e=>{this.error=e.error?.message||'Unable to approve DD';this.saving=false;}});}
 reject(d:any=this.selected){if(!d)return;this.saving=true;this.error='';this.service.reject(d.id,this.adminName,'Rejected by admin').subscribe({next:x=>{this.replaceDraft(x);this.saving=false;},error:e=>{this.error=e.error?.message||'Unable to reject DD';this.saving=false;}});}
 private replaceDraft(updated:any){this.drafts=this.drafts.map(d=>d.id===updated.id?updated:d);this.filter();if(this.selected?.id===updated.id){this.selected=updated;this.edit={...updated};}}
 download(id:number){this.service.download(id).subscribe(blob=>{const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`demand-draft-${id}.pdf`;a.click();URL.revokeObjectURL(url);});}
}
