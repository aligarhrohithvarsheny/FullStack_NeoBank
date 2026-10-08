import { Injectable, NgZone, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Adds a small copy button next to identifier-like values (account numbers,
 * mobile numbers, loan / FD / application / customer ids) anywhere in the UI.
 * The original text nodes are never modified, so Angular bindings keep working.
 */
@Injectable({ providedIn: 'root' })
export class CopyButtonService {
  private static readonly BTN_CLASS = 'neo-copy-btn';
  private static readonly SKIP_TAGS = new Set([
    'SCRIPT', 'STYLE', 'TEXTAREA', 'INPUT', 'SELECT', 'OPTION', 'BUTTON', 'CODE', 'PRE', 'TITLE'
  ]);
  private static readonly NUMERIC = /^\d{8,18}$/;
  private static readonly CODE = /^(?=[A-Z0-9-]*\d)[A-Z][A-Z0-9-]{5,29}$/;

  private scheduled = false;
  private started = false;

  constructor(private zone: NgZone, @Inject(PLATFORM_ID) private platformId: Object) {}

  start(): void {
    if (this.started || !isPlatformBrowser(this.platformId)) return;
    this.started = true;
    this.zone.runOutsideAngular(() => {
      document.addEventListener('click', (e) => this.onClick(e), true);
      const observer = new MutationObserver(() => this.schedule());
      observer.observe(document.body, { childList: true, subtree: true, characterData: true });
      this.schedule();
    });
  }

  private schedule(): void {
    if (this.scheduled) return;
    this.scheduled = true;
    setTimeout(() => {
      this.scheduled = false;
      this.scan();
    }, 150);
  }

  private extractToken(text: string): string | null {
    const trimmed = (text || '').trim();
    if (!trimmed || trimmed.length > 80) return null;
    if (/[₹$%]|\bRs\.?\b|\d[.,]\d/i.test(trimmed)) return null;
    const parts = trimmed.split(/[\s:#]+/);
    const last = parts[parts.length - 1];
    if (CopyButtonService.NUMERIC.test(last) || CopyButtonService.CODE.test(last)) return last;
    return null;
  }

  private isButton(node: Node | null): node is HTMLElement {
    return !!node && node.nodeType === Node.ELEMENT_NODE &&
      (node as HTMLElement).classList.contains(CopyButtonService.BTN_CLASS);
  }

  private scan(): void {
    // Remove buttons whose value text is gone or no longer an identifier
    document.querySelectorAll('.' + CopyButtonService.BTN_CLASS).forEach((btn) => {
      const prev = btn.previousSibling;
      if (!prev || prev.nodeType !== Node.TEXT_NODE || !this.extractToken(prev.textContent || '')) {
        btn.remove();
      }
    });

    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
      acceptNode: (n) => {
        const parent = n.parentElement;
        if (!parent || CopyButtonService.SKIP_TAGS.has(parent.tagName)) return NodeFilter.FILTER_REJECT;
        if (parent.closest('[data-no-copy]')) return NodeFilter.FILTER_REJECT;
        return NodeFilter.FILTER_ACCEPT;
      }
    });

    const targets: Text[] = [];
    let node: Node | null;
    while ((node = walker.nextNode())) {
      const text = node as Text;
      if (!this.extractToken(text.data)) continue;
      if (this.isButton(text.nextSibling)) continue;
      targets.push(text);
    }

    for (const text of targets) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = CopyButtonService.BTN_CLASS;
      btn.title = 'Copy';
      btn.setAttribute('aria-label', 'Copy to clipboard');
      btn.textContent = '⧉';
      text.parentNode?.insertBefore(btn, text.nextSibling);
    }
  }

  private onClick(event: Event): void {
    const btn = (event.target as HTMLElement | null)?.closest?.('.' + CopyButtonService.BTN_CLASS) as HTMLElement | null;
    if (!btn) return;
    event.preventDefault();
    event.stopPropagation();
    const prev = btn.previousSibling;
    const value = prev ? this.extractToken(prev.textContent || '') : null;
    if (!value) return;
    this.copy(value).then(() => {
      btn.textContent = '✓';
      btn.classList.add('copied');
      setTimeout(() => {
        btn.textContent = '⧉';
        btn.classList.remove('copied');
      }, 1200);
    });
  }

  private async copy(value: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(value);
    } catch {
      const ta = document.createElement('textarea');
      ta.value = value;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      ta.remove();
    }
  }
}
