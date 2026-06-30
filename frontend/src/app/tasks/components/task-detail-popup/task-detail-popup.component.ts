import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { TaskResponse } from '../../models/task.model';
import { StaffMember } from '../../../staff/models/staff.models';
import { Employee } from '../../../employees/models/employee.models';

@Component({
  selector: 'app-task-detail-popup',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="popup-overlay" (click)="onClose()" role="dialog" aria-modal="true" aria-labelledby="popup-title">
      <div class="popup-container" (click)="$event.stopPropagation()">
        <div class="popup-header">
          <h3 id="popup-title">Task Details</h3>
          <button class="close-btn" (click)="onClose()" aria-label="Close popup">&times;</button>
        </div>

        <div class="popup-body">
          <!-- Delete Error Message -->
          @if (deleteError()) {
            <div class="delete-error" role="alert">
              <p>{{ deleteError() }}</p>
            </div>
          }

          <div class="detail-field">
            <label>Description</label>
            <p class="detail-value description-value">{{ task.description }}</p>
          </div>

          <div class="detail-field">
            <label>Status</label>
            <span class="status-badge" [attr.data-status]="task.status">{{ task.status }}</span>
          </div>

          <div class="detail-field">
            <label>Assignee</label>
            <p class="detail-value assignee-name">{{ getStaffName(task.assigneeId) }}</p>
          </div>

          <div class="detail-field">
            <label>Creator</label>
            <p class="detail-value creator-name">{{ getStaffName(task.creatorId) }}</p>
          </div>

          <div class="detail-field">
            <label>Individual</label>
            <p class="detail-value individual-name">{{ getIndividualName(task.individualId) }}</p>
          </div>

          <div class="detail-field">
            <label>Due Date</label>
            <p class="detail-value due-date-value">{{ task.dueDate || 'No due date' }}</p>
          </div>

          <div class="detail-field">
            <label>Linked Interaction</label>
            @if (task.interactionId) {
              <a class="interaction-link" (click)="navigateToInteraction(task.interactionId!)">
                Interaction Details
              </a>
            } @else {
              <p class="detail-value no-interaction">No linked interaction</p>
            }
          </div>
        </div>

        <div class="popup-actions">
          @if (isCreator()) {
            <button class="edit-btn" (click)="onEdit()">Edit</button>
            <button class="delete-btn" (click)="onDeleteClick()">Delete</button>
          }
        </div>

        <!-- Delete Confirmation Dialog -->
        @if (showDeleteConfirm()) {
          <div class="confirm-overlay">
            <div class="confirm-dialog">
              <p>Are you sure you want to delete this task?</p>
              <div class="confirm-actions">
                <button
                  class="confirm-delete-btn"
                  (click)="onConfirmDelete()"
                  [disabled]="deleteInProgress()"
                >
                  {{ deleteInProgress() ? 'Deleting...' : 'Confirm' }}
                </button>
                <button
                  class="cancel-delete-btn"
                  (click)="onCancelDelete()"
                  [disabled]="deleteInProgress()"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .popup-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .popup-container {
      background: white;
      border-radius: 8px;
      width: 90%;
      max-width: 600px;
      max-height: 80vh;
      overflow-y: auto;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      position: relative;
    }

    .popup-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid #e5e7eb;
    }

    .popup-header h3 {
      margin: 0;
      font-size: 1.25rem;
      color: #111827;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
      color: #6b7280;
      padding: 0.25rem;
      line-height: 1;
    }

    .close-btn:hover {
      color: #111827;
    }

    .popup-body {
      padding: 1.5rem;
    }

    .detail-field {
      margin-bottom: 1rem;
    }

    .detail-field label {
      display: block;
      font-size: 0.75rem;
      font-weight: 600;
      color: #6b7280;
      text-transform: uppercase;
      margin-bottom: 0.25rem;
    }

    .detail-value {
      margin: 0;
      color: #111827;
      font-size: 0.9rem;
    }

    .status-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 4px;
      font-weight: 500;
      font-size: 0.8rem;
      text-transform: uppercase;
    }

    .status-badge[data-status="TODO"] {
      background: #dbeafe;
      color: #1e40af;
    }

    .status-badge[data-status="IN_PROGRESS"] {
      background: #fef3c7;
      color: #92400e;
    }

    .status-badge[data-status="DONE"] {
      background: #d1fae5;
      color: #065f46;
    }

    .interaction-link {
      color: #2563eb;
      cursor: pointer;
      text-decoration: underline;
      font-size: 0.9rem;
    }

    .interaction-link:hover {
      color: #1d4ed8;
    }

    .popup-actions {
      display: flex;
      gap: 0.75rem;
      padding: 1rem 1.5rem;
      border-top: 1px solid #e5e7eb;
    }

    .edit-btn {
      padding: 0.5rem 1rem;
      background: #2563eb;
      color: white;
      border: none;
      border-radius: 6px;
      font-size: 0.875rem;
      cursor: pointer;
    }

    .edit-btn:hover {
      background: #1d4ed8;
    }

    .delete-btn {
      padding: 0.5rem 1rem;
      background: #dc2626;
      color: white;
      border: none;
      border-radius: 6px;
      font-size: 0.875rem;
      cursor: pointer;
    }

    .delete-btn:hover {
      background: #b91c1c;
    }

    .delete-error {
      padding: 0.75rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 6px;
      margin-bottom: 1rem;
    }

    .delete-error p {
      margin: 0;
      color: #991b1b;
      font-size: 0.875rem;
    }

    .confirm-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
    }

    .confirm-dialog {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      text-align: center;
    }

    .confirm-dialog p {
      margin: 0 0 1rem 0;
      color: #111827;
      font-size: 0.9rem;
    }

    .confirm-actions {
      display: flex;
      gap: 0.75rem;
      justify-content: center;
    }

    .confirm-delete-btn {
      padding: 0.5rem 1rem;
      background: #dc2626;
      color: white;
      border: none;
      border-radius: 6px;
      font-size: 0.875rem;
      cursor: pointer;
    }

    .confirm-delete-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .confirm-delete-btn:hover:not(:disabled) {
      background: #b91c1c;
    }

    .cancel-delete-btn {
      padding: 0.5rem 1rem;
      background: #e5e7eb;
      color: #374151;
      border: none;
      border-radius: 6px;
      font-size: 0.875rem;
      cursor: pointer;
    }

    .cancel-delete-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .cancel-delete-btn:hover:not(:disabled) {
      background: #d1d5db;
    }
  `]
})
export class TaskDetailPopupComponent {
  @Input({ required: true }) task!: TaskResponse;
  @Input() staffMembers: StaffMember[] = [];
  @Input() employees: Employee[] = [];
  @Input() currentStaffId: string = '';

  @Output() closed = new EventEmitter<void>();
  @Output() editRequested = new EventEmitter<TaskResponse>();
  @Output() taskDeleted = new EventEmitter<void>();

  private readonly taskService = inject(TaskService);
  private readonly router = inject(Router);

  // State
  showDeleteConfirm = signal(false);
  deleteInProgress = signal(false);
  deleteError = signal<string | null>(null);

  isCreator(): boolean {
    return this.task.creatorId === this.currentStaffId;
  }

  getStaffName(staffId: string): string {
    const staff = this.staffMembers.find(s => s.id === staffId);
    return staff ? staff.email : staffId;
  }

  getIndividualName(individualId: string): string {
    const employee = this.employees.find(e => e.id === individualId);
    return employee ? `${employee.firstName} ${employee.lastName}` : individualId;
  }

  navigateToInteraction(interactionId: string): void {
    this.router.navigate(['/interactions', interactionId]);
  }

  onClose(): void {
    this.closed.emit();
  }

  onEdit(): void {
    this.editRequested.emit(this.task);
  }

  onDeleteClick(): void {
    this.deleteError.set(null);
    this.showDeleteConfirm.set(true);
  }

  onCancelDelete(): void {
    this.showDeleteConfirm.set(false);
  }

  onConfirmDelete(): void {
    this.deleteInProgress.set(true);
    this.deleteError.set(null);

    this.taskService.deleteTask(this.task.id).subscribe({
      next: () => {
        this.deleteInProgress.set(false);
        this.showDeleteConfirm.set(false);
        this.taskDeleted.emit();
      },
      error: (err) => {
        this.deleteInProgress.set(false);

        if (err.status === 403) {
          // Permission error — re-enable dialog for acknowledgment
          this.deleteError.set('You do not have permission to delete this task.');
          this.showDeleteConfirm.set(false);
        } else if (err.status === 404) {
          // Task already gone — close popup, emit deleted to refresh list
          this.showDeleteConfirm.set(false);
          this.deleteError.set('Task was not found. It may have already been deleted.');
          this.taskDeleted.emit();
        } else {
          // Server error — show error, re-enable dialog for retry
          this.deleteError.set('Failed to delete task. Please try again.');
        }
      }
    });
  }
}
