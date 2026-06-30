import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-toast-notification',
  standalone: true,
  template: `
    @if (message) {
      <div class="toast" [class.toast-success]="type === 'success'" [class.toast-error]="type === 'error'" role="alert">
        <span class="toast-message">{{ message }}</span>
        <button class="toast-dismiss" (click)="dismiss()" aria-label="Dismiss notification">&times;</button>
      </div>
    }
  `,
  styles: [`
    .toast {
      position: fixed;
      top: 1rem;
      right: 1rem;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.875rem 1.25rem;
      border-radius: var(--radius, 6px);
      box-shadow: var(--shadow-md, 0 4px 12px rgba(0, 0, 0, 0.15));
      z-index: 2000;
      font-size: 0.9rem;
      max-width: 400px;
      animation: slideIn 0.3s ease-out;
    }

    .toast-success {
      background: #d4edda;
      color: #155724;
      border: 1px solid #c3e6cb;
    }

    .toast-error {
      background: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }

    .toast-message {
      flex: 1;
    }

    .toast-dismiss {
      background: none;
      border: none;
      font-size: 1.25rem;
      cursor: pointer;
      color: inherit;
      line-height: 1;
      padding: 0 0.25rem;
      opacity: 0.7;
    }

    .toast-dismiss:hover {
      opacity: 1;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
  `]
})
export class ToastNotificationComponent implements OnInit, OnDestroy {
  @Input() message: string = '';
  @Input() type: 'success' | 'error' = 'success';
  @Input() autoDismissMs: number = 5000;
  @Output() dismissed = new EventEmitter<void>();

  private timerId: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.startAutoDismiss();
  }

  ngOnDestroy(): void {
    this.cancelTimer();
  }

  dismiss(): void {
    this.cancelTimer();
    this.dismissed.emit();
  }

  private startAutoDismiss(): void {
    if (this.autoDismissMs > 0) {
      this.timerId = setTimeout(() => {
        this.dismissed.emit();
      }, this.autoDismissMs);
    }
  }

  private cancelTimer(): void {
    if (this.timerId !== null) {
      clearTimeout(this.timerId);
      this.timerId = null;
    }
  }
}
