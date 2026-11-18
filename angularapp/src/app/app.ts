import { isPlatformBrowser, NgIf } from '@angular/common';
import { Component, signal, AfterViewInit, OnDestroy, PLATFORM_ID, inject } from '@angular/core';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router, RouterOutlet } from '@angular/router';
import { AlertComponent } from './component/shared/alert/alert.component';
import { CopyButtonService } from './service/copy-button.service';
import { LoadingIndicatorService } from './service/loading-indicator.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [NgIf, RouterOutlet, AlertComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements AfterViewInit, OnDestroy {
  protected readonly title = signal('angularapp');
  private readonly platformId = inject(PLATFORM_ID);
  private routerEventsSubscription?: Subscription;

  constructor(
    private copyButtons: CopyButtonService,
    private router: Router,
    public readonly loadingIndicator: LoadingIndicatorService
  ) {
    if (isPlatformBrowser(this.platformId)) {
      this.routerEventsSubscription = this.router.events.subscribe(event => {
        if (event instanceof NavigationStart) {
          this.loadingIndicator.start();
        } else if (event instanceof NavigationEnd
            || event instanceof NavigationCancel
            || event instanceof NavigationError) {
          this.loadingIndicator.finish();
        }
      });
    }
  }

  ngAfterViewInit(): void {
    this.copyButtons.start();
  }

  ngOnDestroy(): void {
    this.routerEventsSubscription?.unsubscribe();
  }
}
