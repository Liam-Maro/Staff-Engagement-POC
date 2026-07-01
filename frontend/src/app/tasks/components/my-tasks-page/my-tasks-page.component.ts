import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { Subject } from 'rxjs';

import { TaskListComponent } from '../task-list/task-list.component';
import { DelegatedTasksPanelComponent } from '../delegated-tasks-panel/delegated-tasks-panel.component';
import { TaskFormComponent } from '../task-form/task-form.component';
import { TaskDetailPopupComponent } from '../task-detail-popup/task-detail-popup.component';
import { TaskResponse } from '../../models/task.model';
import { StaffMember } from '../../../staff/models/staff.models';
import { Employee } from '../../../employees/models/employee.models';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';

@Component({
  selector: 'app-my-tasks-page',
  standalone: true,
  imports: [TaskListComponent, DelegatedTasksPanelComponent, TaskFormComponent, TaskDetailPopupComponent],
  template: `
    <div class="my-tasks-page">
      <header class="page-header">
        <h2>My Tasks</h2>
        <button class="create-task-btn" (click)="openCreateForm()">
          + Create Task
        </button>
      </header>

      <div class="page-content">
        <section class="main-panel">
          <app-task-list [refresh$]="refresh$" (taskSelected)="onTaskSelected($event)" />
        </section>

        <aside class="side-panel">
          <app-delegated-tasks-panel [refresh$]="refresh$" />
        </aside>
      </div>

      @if (showTaskForm()) {
        <app-task-form
          [editTask]="editingTask()"
          (closed)="closeCreateForm()"
          (taskCreated)="onTaskCreated()"
          (taskUpdated)="onTaskUpdated()"
        />
      }

      @if (selectedTask()) {
        <app-task-detail-popup
          [task]="selectedTask()!"
          [staffMembers]="staffMembers()"
          [employees]="employees()"
          [currentStaffId]="currentStaffId"
          (closed)="onPopupClosed()"
          (editRequested)="onEditRequested($event)"
          (taskDeleted)="onTaskDeleted()"
        />
      }
    </div>
  `,
  styles: [`
    .my-tasks-page {
      padding: 1.5rem;
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }

    .page-header h2 {
      margin: 0;
      font-size: 1.5rem;
      color: #111827;
    }

    .create-task-btn {
      background-color: #2563eb;
      color: white;
      border: none;
      padding: 0.625rem 1.25rem;
      border-radius: 6px;
      font-size: 0.875rem;
      font-weight: 500;
      cursor: pointer;
      transition: background-color 0.2s;
    }

    .create-task-btn:hover {
      background-color: #1d4ed8;
    }

    .create-task-btn:focus-visible {
      outline: 2px solid #2563eb;
      outline-offset: 2px;
    }

    .page-content {
      display: grid;
      grid-template-columns: 1fr 380px;
      gap: 1.5rem;
      flex: 1;
      min-height: 0;
    }

    .main-panel {
      min-width: 0;
      overflow-y: auto;
    }

    .side-panel {
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 1rem;
      overflow-y: auto;
    }

    @media (max-width: 1024px) {
      .page-content {
        grid-template-columns: 1fr;
      }

      .side-panel {
        order: -1;
      }
    }
  `]
})
export class MyTasksPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly staffService = inject(StaffService);
  private readonly employeeService = inject(EmployeeService);

  showTaskForm = signal(false);
  selectedTask = signal<TaskResponse | null>(null);
  editingTask = signal<TaskResponse | null>(null);
  staffMembers = signal<StaffMember[]>([]);
  employees = signal<Employee[]>([]);
  currentStaffId = '';
  refresh$ = new Subject<void>();

  ngOnInit(): void {
    this.currentStaffId = this.authService.staffId();
    this.staffService.findAll().subscribe(members => {
      this.staffMembers.set(members);
    });
    this.employeeService.findAll().subscribe(emps => {
      this.employees.set(emps);
    });
  }

  openCreateForm(): void {
    this.editingTask.set(null);
    this.showTaskForm.set(true);
  }

  closeCreateForm(): void {
    this.showTaskForm.set(false);
    this.editingTask.set(null);
  }

  onTaskCreated(): void {
    this.showTaskForm.set(false);
    this.editingTask.set(null);
    this.refresh$.next();
  }

  onEditRequested(task: TaskResponse): void {
    this.selectedTask.set(null);
    this.editingTask.set(task);
    this.showTaskForm.set(true);
  }

  onTaskUpdated(): void {
    this.showTaskForm.set(false);
    this.editingTask.set(null);
    this.refresh$.next();
  }

  onTaskSelected(task: TaskResponse): void {
    this.selectedTask.set(task);
  }

  onPopupClosed(): void {
    this.selectedTask.set(null);
  }

  onTaskDeleted(): void {
    this.selectedTask.set(null);
    this.refresh$.next();
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.selectedTask()) {
      this.onPopupClosed();
    }
  }
}
