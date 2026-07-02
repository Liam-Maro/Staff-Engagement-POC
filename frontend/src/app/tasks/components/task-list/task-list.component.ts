import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, finalize } from 'rxjs';

import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { TaskResponse, TaskStatus, TaskQueryParams } from '../../models/task.model';
import { Employee } from '../../../employees/models/employee.models';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="task-list">
      <!-- Filter Controls -->
      <div class="filter-controls">
        <div class="filter-group">
          <label for="status-filter">Status</label>
          <select
            id="status-filter"
            [(ngModel)]="statusFilter"
            (ngModelChange)="onFilterChange()"
          >
            <option value="">All</option>
            <option value="To Do">To Do</option>
            <option value="In Progress">In Progress</option>
            <option value="Done">Done</option>
          </select>
        </div>

        <div class="filter-group">
          <label for="due-date-from">Due Date From</label>
          <input
            id="due-date-from"
            type="date"
            [(ngModel)]="dueDateFrom"
            (ngModelChange)="onFilterChange()"
          />
        </div>

        <div class="filter-group">
          <label for="due-date-to">Due Date To</label>
          <input
            id="due-date-to"
            type="date"
            [(ngModel)]="dueDateTo"
            (ngModelChange)="onFilterChange()"
          />
        </div>

        <div class="filter-group">
          <label for="created-from">Created From</label>
          <input
            id="created-from"
            type="date"
            [(ngModel)]="createdFrom"
            (ngModelChange)="onFilterChange()"
          />
        </div>

        <div class="filter-group">
          <label for="created-to">Created To</label>
          <input
            id="created-to"
            type="date"
            [(ngModel)]="createdTo"
            (ngModelChange)="onFilterChange()"
          />
        </div>
      </div>

      <!-- Loading Indicator -->
      @if (loading()) {
        <div class="loading-indicator" role="status" aria-label="Loading tasks">
          <span class="spinner"></span>
          <span>Loading tasks...</span>
        </div>
      }

      <!-- Error + Retry -->
      @if (error()) {
        <div class="error-container" role="alert">
          <p class="error-message">{{ error() }}</p>
          <button class="retry-btn" (click)="loadTasks()">Retry</button>
        </div>
      }

      <!-- Task List -->
      @if (!loading() && !error()) {
        @if (tasks().length === 0) {
          <p class="empty-message">No tasks match selected filters</p>
        } @else {
          <ul class="task-items" role="list">
            @for (task of tasks(); track task.id) {
              <li class="task-item" (click)="onTaskClick(task)" role="button" tabindex="0" (keydown.enter)="onTaskClick(task)">
                <div class="task-description">{{ truncateDescription(task.description) }}</div>
                <div class="task-meta">
                  <span class="task-status" [attr.data-status]="task.status">{{ task.status }}</span>
                  <span class="task-due-date">{{ task.dueDate || 'No due date' }}</span>
                  <span class="task-individual">{{ getIndividualName(task.individualId) }}</span>
                </div>
              </li>
            }
          </ul>
        }
      }
    </div>
  `,
  styles: [`
    .task-list {
      height: 100%;
    }

    .filter-controls {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      margin-bottom: 1.5rem;
      padding: 1rem;
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
    }

    .filter-group {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .filter-group label {
      font-size: 0.75rem;
      font-weight: 500;
      color: #6b7280;
      text-transform: uppercase;
    }

    .filter-group select,
    .filter-group input[type="date"] {
      padding: 0.5rem 0.75rem;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      font-size: 0.875rem;
      color: #111827;
      background: white;
    }

    .filter-group select:focus,
    .filter-group input[type="date"]:focus {
      outline: none;
      border-color: #2563eb;
      box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
    }

    .loading-indicator {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1.5rem;
      color: #6b7280;
    }

    .spinner {
      width: 20px;
      height: 20px;
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
      padding: 1.5rem 0;
      text-align: center;
    }

    .task-items {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .task-item {
      padding: 1rem;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      cursor: pointer;
      transition: background-color 0.15s, border-color 0.15s;
    }

    .task-item:hover {
      background: #f9fafb;
      border-color: #2563eb;
    }

    .task-item:focus-visible {
      outline: 2px solid #2563eb;
      outline-offset: 2px;
    }

    .task-description {
      font-size: 0.9rem;
      color: #111827;
      margin-bottom: 0.5rem;
      line-height: 1.4;
    }

    .task-meta {
      display: flex;
      gap: 1rem;
      font-size: 0.8rem;
      color: #6b7280;
    }

    .task-status {
      padding: 0.125rem 0.5rem;
      border-radius: 4px;
      font-weight: 500;
      font-size: 0.75rem;
      text-transform: uppercase;
    }

    .task-status[data-status="To Do"] {
      background: #dbeafe;
      color: #1e40af;
    }

    .task-status[data-status="In Progress"] {
      background: #fef3c7;
      color: #92400e;
    }

    .task-status[data-status="Done"] {
      background: #d1fae5;
      color: #065f46;
    }
  `]
})
export class TaskListComponent implements OnInit, OnDestroy {
  @Input() refresh$!: Subject<void>;

  private readonly taskService = inject(TaskService);
  private readonly authService = inject(AuthService);
  private readonly staffService = inject(StaffService);
  private readonly employeeService = inject(EmployeeService);

  private refreshSubscription?: Subscription;
  private currentStaffId: string | null = null;
  private employeeMap = new Map<string, Employee>();

  // State signals
  tasks = signal<TaskResponse[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  // Filter state
  statusFilter = '';
  dueDateFrom = '';
  dueDateTo = '';
  createdFrom = '';
  createdTo = '';

  // Output emitter for parent to handle task selection
  @Output() taskSelected = new EventEmitter<TaskResponse>();

  // Internal signal for local state tracking
  selectedTask = signal<TaskResponse | null>(null);

  ngOnInit(): void {
    this.resolveCurrentStaffAndLoad();
    this.employeeService.findAll().subscribe(employees => {
      employees.forEach(e => this.employeeMap.set(e.id, e));
    });

    if (this.refresh$) {
      this.refreshSubscription = this.refresh$.subscribe(() => {
        this.loadTasks();
      });
    }
  }

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
  }

  onFilterChange(): void {
    this.loadTasks();
  }

  onTaskClick(task: TaskResponse): void {
    this.selectedTask.set(task);
    this.taskSelected.emit(task);
  }

  loadTasks(): void {
    if (!this.currentStaffId) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const isAdmin = this.authService.userRole().toLowerCase() === 'admin';

    const params: TaskQueryParams = {
      sortBy: 'createdDate',
      sortOrder: 'desc'
    };

    // Admin sees all tasks; staff sees only their assigned tasks
    if (!isAdmin) {
      params.assigneeId = this.currentStaffId;
    }

    if (this.statusFilter) {
      params.status = this.statusFilter as TaskStatus;
    }
    if (this.dueDateFrom) {
      params.dueDateFrom = this.dueDateFrom;
    }
    if (this.dueDateTo) {
      params.dueDateTo = this.dueDateTo;
    }
    if (this.createdFrom) {
      params.createdFrom = this.createdFrom;
    }
    if (this.createdTo) {
      params.createdTo = this.createdTo;
    }

    this.taskService.getTasks(params)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          let taskList = result.tasks;
          // For admin: sort own tasks (assigned to me) to the top
          if (this.authService.userRole() === 'Admin' && this.currentStaffId) {
            const myId = this.currentStaffId;
            taskList = [
              ...taskList.filter(t => t.assigneeId === myId || t.creatorId === myId),
              ...taskList.filter(t => t.assigneeId !== myId && t.creatorId !== myId)
            ];
          }
          this.tasks.set(taskList);
        },
        error: (err) => {
          this.error.set('Failed to load tasks. Please try again.');
        }
      });
  }

  truncateDescription(description: string): string {
    if (description.length <= 100) {
      return description;
    }
    return description.substring(0, 100) + '...';
  }

  getIndividualName(individualId: string): string {
    const employee = this.employeeMap.get(individualId);
    return employee ? `${employee.firstName} ${employee.lastName}` : individualId;
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
        const currentStaff = staffList.find(s => s.email === email);
        if (currentStaff) {
          this.currentStaffId = currentStaff.id;
          this.loadTasks();
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
