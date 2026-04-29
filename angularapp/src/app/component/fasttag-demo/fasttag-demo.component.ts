import { Component, ElementRef, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FasttagComponent } from '../fasttag/fasttag.component';

@Component({
  selector: 'fasttag-demo',
  standalone: true,
  imports: [CommonModule, FasttagComponent],
  templateUrl: './fasttag-demo.component.html',
  styleUrls: ['./fasttag-demo.component.css']
})
export class FasttagDemoComponent implements OnInit {
  @ViewChild('fastagWrap', { static: true }) fastagWrap!: ElementRef<HTMLElement>;

  bank = {
    bankName: 'IDFC FIRST Bank Neo Bank',
    logoUrl: '',
    customerCare: '1860 2666 888',
    barcode: '123456-001-7654321'
  };

  ngOnInit(): void {
    if (!this.bank.logoUrl) {
      this.bank.logoUrl = this.resolveBankLogo(this.bank.bankName);
    }
  }

  private resolveBankLogo(name: string): string {
    const n = (name || '').toLowerCase();
    // Known bank mappings
    if (n.includes('idfc')) {
      // Use Google favicon service for the bank domain (IDFC FIRST Bank)
      return 'https://www.google.com/s2/favicons?sz=128&domain=idfcfirstbank.com';
    }

    // Generic fallback: try Google favicon using guessed domain (remove spaces)
    const guess = name.replace(/\s+/g, '').toLowerCase();
    if (guess) {
      return `https://www.google.com/s2/favicons?sz=128&domain=${encodeURIComponent(guess)}.com`;
    }
    // Last resort: a placeholder data URL (small transparent PNG)
    return 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==';
  }

  async downloadPng() {
    await this.ensureHtml2Canvas();
    const el = this.fastagWrap.nativeElement as HTMLElement;
    const html2canvas = (window as any).html2canvas;
    if (!html2canvas) return alert('html2canvas failed to load');
    html2canvas(el, { scale: 2 }).then((canvas: HTMLCanvasElement) => {
      const dataUrl = canvas.toDataURL('image/png');
      const a = document.createElement('a');
      a.href = dataUrl;
      a.download = `${this.bank.bankName.replace(/\s+/g, '_')}_FASTag.png`;
      a.click();
    }).catch((err: any) => alert('Capture failed: ' + err));
  }

  private ensureHtml2Canvas(): Promise<void> {
    return new Promise((resolve, reject) => {
      if ((window as any).html2canvas) return resolve();
      const s = document.createElement('script');
      s.src = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';
      s.onload = () => resolve();
      s.onerror = () => reject(new Error('Failed to load html2canvas'));
      document.head.appendChild(s);
    });
  }
}
