import { Component, Input, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, Subscription, finalize } from 'rxjs';

import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { TaskResponse, TaskQueryParams } from '../../models/task.model';
import { StaffMember } from '../../../staff/models/staff.models';

@Component({
  selector: 'app-delegated-tasks-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="delegated-tasks-panel">
      <h3>Delegated Tasks</h3>

      <!-- Loading Indicator -->
      @if (loading()) {
        <div class="loading-indicator" role="status" aria-label="Loading delegated tasks">
          <span class="spinner"></span>
          <span>Loading delegated tasks...</span>
        </div>
      }

      <!-- Error + Retry -->
      @if (error()) {
        <div class="error-container" role="alert">
          <p class="error-message">{{ error() }}</p>
          <button class="retry-btn" (click)="loadDelegatedTasks()">Retry</button>
        </div>
      }

      <!-- Task List -->
      @if (!loading() && !error()) {
        @if (tasks().length === 0) {
          <p class="empty-message">You haven't delegated any tasks to other staff members</p>
        } @else {
          <ul class="delegated-task-items" role="list">
            @for (task of tasks(); track task.id) {
              <li class="delegated-task-item">
                <div class="task-description">{{ truncateDescription(task.description) }}</div>
                <div class="task-meta">
                  <span class="task-assignee">{{ getAssigneeName(task.assigneeId) }}</span>
                  <span class="task-status" [attr.data-status]="task.status">{{ task.status }}</span>
                </div>
              </li>
            }
          </ul>
        }
      }
    </div>
  `,
  styles: [`
    .delegated-tasks-panel {
      height: 100%;
    }

    h3 {
      margin: 0 0 1rem 0;
      font-size: 1.1rem;
      color: #374151;
    }

    .loading-indicator {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 0;
      color: #6b7280;
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 2px solid #e5e7eb;
      border-top-color: #2563eb;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .error-container {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
    }

    .error-message {
      margin: 0;
      color: #991b1b;
      font-size: 0.875rem;
    }

    .retry-btn {
      padding: 0.375rem 0.75rem;
      background: #dc2626;
      color: white;
      border: none;
      border-radius: 4px;
      font-size: 0.8rem;
      cursor: pointer;
      white-space: nowrap;
    }

    .retry-btn:hover {
      background: #b91c1c;
    }

    .retry-btn:focus-visible {
      outline: 2px solid #dc2626;
      outline-offset: 2px;
    }

    .empty-message {
      color: #6b7280;
      font-style: italic;
      padding: 1rem 0;
    }

    .delegated-task-items {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .delegated-task-item {
      padding: 0.75rem;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      transition: background-color 0.15s;
    }

    .delegated-task-item:hover {
      background: #f9fafb;
    }

    .task-description {
      font-size: 0.875rem;
      color: #111827;
      margin-bottom: 0.375rem;
      line-height: 1.4;
    }

    .task-meta {
      display: flex;
      gap: 0.75rem;
      font-size: 0.8rem;
      color: #6b7280;
      align-items: center;
    }

    .task-assignee {
      font-weight: 500;
    }

    .task-status {
      padding: 0.125rem 0.5rem;
      border-radius: 4px;
      font-weight: 500;
      font-size: 0.7rem;
      text-transform: uppercase;
    }

    .task-status[data-status="TODO"] {
      background: #dbeafe;
      color: #1e40af;
    }

    .task-status[data-status="IN_PROGRESS"] {
      background: #fef3c7;
      color: #92400e;
    }

    .task-status[data-status="DONE"] {
      background: #d1fae5;
      color: #065f46;
    }
  `]
})
export class DelegatedTasksPanelComponent implements OnInit, OnDestroy {
  @Input() refresh$!: Subject<void>;

  private readonly taskService = inject(TaskService);
  private readonly authService = inject(AuthService);
  private readonly staffService = inject(StaffService);

  private refreshSubscription?: Subscription;
  private currentStaffId: string | null = null;
  private staffMap = new Map<string, StaffMember>();

  // State signals
  tasks = signal<TaskResponse[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.resolveCurrentStaffAndLoad();

    if (this.refresh$) {
      this.refreshSubscription = this.refresh$.subscribe(() => {
        this.loadDelegatedTasks();
      });
    }
  }

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
  }

  loadDelegatedTasks(): void {
    if (!this.currentStaffId) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const params: TaskQueryParams = {
      creatorId: this.currentStaffId,
      excludeSelfAssigned: true,
      sortBy: 'createdDate',
      sortOrder: 'desc',
      size: 50
    };

    this.taskService.getTasks(params)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.tasks.set(result.tasks);
        },
        error: () => {
          this.error.set('Failed to load delegated tasks. Please try again.');
        }
      });
  }

  truncateDescription(description: string): string {
    if (description.length <= 100) {
      return description;
    }
    return description.substring(0, 100) + '...';
  }

  getAssigneeName(assigneeId: string): string {
    const staff = this.staffMap.get(assigneeId);
    return staff ? staff.email : assigneeId;
  }

  private resolveCurrentStaffAndLoad(): void {
    const email = this.authService.userEmail();
    if (!email) {
      this.error.set('Unable to determine current user.');
      return;
    }

    this.loading.set(true);
    this.staffService.findAll().subscribe({
      next: (staffList) => {
        // Build staff lookup map
        staffList.forEach(s => this.staffMap.set(s.id, s));

        const currentStaff = staffList.find(s => s.email === email);
        if (currentStaff) {
          this.currentStaffId = currentStaff.id;
          this.loadDelegatedTasks();
        } else {
          this.loading.set(false);
          this.error.set('Unable to determine current user.');
        }
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Failed to load user information. Please try again.');
      }
    });
  }
}
