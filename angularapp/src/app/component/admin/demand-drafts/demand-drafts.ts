import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; import { FormsModule } from '@angular/forms';
import { DemandDraftService } from '../../../service/demand-draft.service';
@Component({selector:'app-admin-demand-drafts',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./demand-drafts.html',styleUrls:['./demand-drafts.css']})
export class AdminDemandDrafts implements OnInit {
 drafts:any[]=[]; selected:any=null; edit:any={}; adminName='Admin';
 constructor(private service:DemandDraftService){} ngOnInit(){this.load();}
 load(){this.service.getAll().subscribe(x=>this.drafts=x||[]);}
 select(d:any){this.selected=d;this.edit={...d};}
 save(){if(!this.selected)return;this.service.update(this.selected.id,this.edit,this.adminName).subscribe(x=>{this.selected=x;this.load();});}
 approve(){if(!this.selected)return;this.service.approve(this.selected.id,this.adminName).subscribe(()=>{this.selected=null;this.load();});}
 reject(){if(!this.selected)return;this.service.reject(this.selected.id,this.adminName,'Rejected by admin').subscribe(()=>{this.selected=null;this.load();});}
 download(id:number){this.service.download(id).subscribe(blob=>{const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`demand-draft-${id}.pdf`;a.click();URL.revokeObjectURL(url);});}
}
