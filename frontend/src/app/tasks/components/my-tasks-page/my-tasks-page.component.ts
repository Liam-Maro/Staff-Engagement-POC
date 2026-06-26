import { Component, signal } from '@angular/core';
import { Subject } from 'rxjs';

import { TaskListComponent } from '../task-list/task-list.component';
import { DelegatedTasksPanelComponent } from '../delegated-tasks-panel/delegated-tasks-panel.component';
import { TaskFormComponent } from '../task-form/task-form.component';

@Component({
  selector: 'app-my-tasks-page',
  standalone: true,
  imports: [TaskListComponent, DelegatedTasksPanelComponent, TaskFormComponent],
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
          <app-task-list [refresh$]="refresh$" />
        </section>

        <aside class="side-panel">
          <app-delegated-tasks-panel [refresh$]="refresh$" />
        </aside>
      </div>

      @if (showTaskForm()) {
        <app-task-form
          (closed)="closeCreateForm()"
          (taskCreated)="onTaskCreated()"
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
export class MyTasksPageComponent {
  showTaskForm = signal(false);
  refresh$ = new Subject<void>();

  openCreateForm(): void {
    this.showTaskForm.set(true);
  }

  closeCreateForm(): void {
    this.showTaskForm.set(false);
  }

  onTaskCreated(): void {
    this.showTaskForm.set(false);
    this.refresh$.next();
  }
}
