import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AlertConfig {
  type: 'success' | 'error' | 'warning' | 'info' | 'confirm';
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm?: () => void;
  onCancel?: () => void;
  autoClose?: boolean;
  duration?: number; // in milliseconds
  theme?: 'default' | 'transfer' | 'card' | 'admin' | 'user'; // Page-specific themes
}

export interface AlertState {
  show: boolean;
  config: AlertConfig | null;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private alertSubject = new BehaviorSubject<AlertState>({
    show: false,
    config: null
  });

  public alert$: Observable<AlertState> = this.alertSubject.asObservable();

  constructor() { }

  /**
   * Show a success alert
   */
  success(title: string, message: string, autoClose: boolean = true, duration: number = 3000): void {
    this.showAlert({
      type: 'success',
      title,
      message,
      autoClose,
      duration
    });
  }

  /**
   * Show an error alert
   */
  error(title: string, message: string, autoClose: boolean = false): void {
    this.showAlert({
      type: 'error',
      title,
      message,
      autoClose
    });
  }

  /**
   * Show a warning alert
   */
  warning(title: string, message: string, autoClose: boolean = true, duration: number = 4000): void {
    this.showAlert({
      type: 'warning',
      title,
      message,
      autoClose,
      duration
    });
  }

  /**
   * Show an info alert
   */
  info(title: string, message: string, autoClose: boolean = true, duration: number = 3000): void {
    this.showAlert({
      type: 'info',
      title,
      message,
      autoClose,
      duration
    });
  }

  /**
   * Show a confirmation dialog
   */
  confirm(
    title: string, 
    message: string, 
    onConfirm: () => void, 
    onCancel?: () => void,
    confirmText: string = 'Confirm',
    cancelText: string = 'Cancel'
  ): void {
    this.showAlert({
      type: 'confirm',
      title,
      message,
      confirmText,
      cancelText,
      onConfirm,
      onCancel
    });
  }

  /**
   * Show the alert
   */
  private showAlert(config: AlertConfig): void {
    this.alertSubject.next({
      show: true,
      config
    });

    // Auto close if enabled
    if (config.autoClose && config.duration) {
      setTimeout(() => {
        this.hide();
      }, config.duration);
    }
  }

  /**
   * Hide the current alert
   */
  hide(): void {
    this.alertSubject.next({
      show: false,
      config: null
    });
  }

  /**
   * Handle confirm action
   */
  onConfirm(): void {
    const currentState = this.alertSubject.value;
    if (currentState.config?.onConfirm) {
      currentState.config.onConfirm();
    }
    this.hide();
  }

  /**
   * Handle cancel action
   */
  onCancel(): void {
    const currentState = this.alertSubject.value;
    if (currentState.config?.onCancel) {
      currentState.config.onCancel();
    }
    this.hide();
  }

  /**
   * Quick success methods for common scenarios
   */
  loginSuccess(userName: string): void {
    this.success('Login Successful', `Welcome ${userName}!`);
  }

  registrationSuccess(userName: string): void {
    this.success('Account Created', `Welcome ${userName}! Please wait for admin approval.`);
  }

  operationSuccess(operation: string): void {
    this.success('Success', `${operation} completed successfully!`);
  }

  operationError(operation: string, error?: string): void {
    this.error('Error', `${operation} failed. ${error || 'Please try again.'}`);
  }

  validationError(message: string): void {
    this.warning('Validation Error', message);
  }

  insufficientBalance(balance: number): void {
    this.warning('Insufficient Balance', `Available balance: ₹${balance.toLocaleString()}`);
  }

  accountLocked(): void {
    this.error('Account Locked', 'Account is locked due to multiple failed login attempts. Please use the unlock feature.');
  }

  pinSetSuccess(): void {
    this.success('PIN Set', 'PIN has been set successfully!');
  }

  pinResetSuccess(): void {
    this.success('PIN Reset', 'PIN has been reset successfully! Your new PIN is ready to use.');
  }

  cardBlocked(): void {
    this.warning('Card Blocked', 'Your card has been blocked for security reasons.');
  }

  cardDeactivated(): void {
    this.warning('Card Deactivated', 'Your card has been deactivated.');
  }

  logoutSuccess(): void {
    this.success('Logged Out', 'You have been logged out successfully!');
  }

  // Transfer Funds specific alerts with purple theme
  transferSuccess(amount: number, recipient: string): void {
    this.showAlert({
      type: 'success',
      title: 'Transfer Successful',
      message: `₹${amount.toLocaleString()} transferred successfully to ${recipient}!`,
      autoClose: true,
      duration: 4000,
      theme: 'transfer'
    });
  }

  transferError(error: string): void {
    this.showAlert({
      type: 'error',
      title: 'Transfer Failed',
      message: error,
      autoClose: false,
      theme: 'transfer'
    });
  }

  transferValidationError(message: string): void {
    this.showAlert({
      type: 'warning',
      title: 'Validation Error',
      message: message,
      autoClose: true,
      duration: 4000,
      theme: 'transfer'
    });
  }

  transferInsufficientBalance(balance: number): void {
    this.showAlert({
      type: 'warning',
      title: 'Insufficient Balance',
      message: `Available balance: ₹${balance.toLocaleString()}`,
      autoClose: true,
      duration: 5000,
      theme: 'transfer'
    });
  }

  transferConfirm(title: string, message: string, onConfirm: () => void, onCancel?: () => void): void {
    this.showAlert({
      type: 'confirm',
      title: title,
      message: message,
      confirmText: 'Confirm Transfer',
      cancelText: 'Cancel',
      onConfirm,
      onCancel,
      theme: 'transfer'
    });
  }

  // Card specific alerts with blue theme
  cardSuccess(title: string, message: string): void {
    this.showAlert({
      type: 'success',
      title: title,
      message: message,
      autoClose: true,
      duration: 3000,
      theme: 'card'
    });
  }

  cardError(title: string, message: string): void {
    this.showAlert({
      type: 'error',
      title: title,
      message: message,
      autoClose: false,
      theme: 'card'
    });
  }

  cardConfirm(title: string, message: string, onConfirm: () => void, onCancel?: () => void): void {
    this.showAlert({
      type: 'confirm',
      title: title,
      message: message,
      confirmText: 'Confirm',
      cancelText: 'Cancel',
      onConfirm,
      onCancel,
      theme: 'card'
    });
  }

  // Admin specific alerts with dark theme
  adminSuccess(title: string, message: string): void {
    this.showAlert({
      type: 'success',
      title: title,
      message: message,
      autoClose: true,
      duration: 3000,
      theme: 'admin'
    });
  }

  adminError(title: string, message: string): void {
    this.showAlert({
      type: 'error',
      title: title,
      message: message,
      autoClose: false,
      theme: 'admin'
    });
  }

  // User specific alerts with green theme
  userSuccess(title: string, message: string): void {
    this.showAlert({
      type: 'success',
      title: title,
      message: message,
      autoClose: true,
      duration: 3000,
      theme: 'user'
    });
  }

  userError(title: string, message: string): void {
    this.showAlert({
      type: 'error',
      title: title,
      message: message,
      autoClose: false,
      theme: 'user'
    });
  }
}
