import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService, AlertState, AlertConfig } from '../../../service/alert.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert.component.html',
  styleUrls: ['./alert.component.css']
})
export class AlertComponent implements OnInit, OnDestroy {
  alertState: AlertState = { show: false, config: null };
  private subscription: Subscription = new Subscription();

  constructor(private alertService: AlertService) {}

  ngOnInit(): void {
    this.subscription = this.alertService.alert$.subscribe(state => {
      this.alertState = state;
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  onConfirm(): void {
    this.alertService.onConfirm();
  }

  onCancel(): void {
    this.alertService.onCancel();
  }

  onClose(): void {
    this.alertService.hide();
  }

  getAlertIcon(type: string): string {
    switch (type) {
      case 'success': return '✅';
      case 'error': return '❌';
      case 'warning': return '⚠️';
      case 'info': return 'ℹ️';
      case 'confirm': return '❓';
      default: return 'ℹ️';
    }
  }

  getAlertClass(type: string): string {
    const theme = this.alertState.config?.theme || 'default';
    const baseClass = `alert-${type}`;
    const themeClass = `alert-theme-${theme}`;
    return `${baseClass} ${themeClass}`;
  }
}
