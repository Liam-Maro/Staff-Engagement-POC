import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  template: `
    @if (isOpen) {
      <div class="modal-backdrop" (click)="onCancel()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <h3>{{ title }}</h3>
          <p>{{ message }}</p>
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="onCancel()">Cancel</button>
            <button class="btn" [class.btn-danger]="variant === 'danger'" [class.btn-primary]="variant !== 'danger'" (click)="onConfirm()">
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .modal-card {
      background: var(--color-bg-white);
      border-radius: var(--radius);
      padding: 1.75rem;
      box-shadow: var(--shadow-md);
      border: 1px solid var(--color-border);
      max-width: 400px;
      width: 90%;
    }
    h3 { font-size: 1.1rem; margin-bottom: 0.5rem; }
    p { color: var(--color-text-secondary); font-size: 0.9rem; margin-bottom: 1.5rem; }
    .modal-actions { display: flex; gap: 0.75rem; justify-content: flex-end; }
  `]
})
export class ConfirmModalComponent {
  @Input() isOpen = false;
  @Input() title = 'Confirm';
  @Input() message = 'Are you sure?';
  @Input() confirmText = 'Confirm';
  @Input() variant: 'danger' | 'primary' = 'danger';
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  onConfirm(): void {
    this.confirmed.emit();
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
