import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
import { of, throwError, Subject } from 'rxjs';

import { TaskListComponent } from './task-list.component';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { TaskQueryResult, TaskResponse } from '../../models/task.model';

describe('TaskListComponent', () => {
  let component: TaskListComponent;
  let fixture: ComponentFixture<TaskListComponent>;
  let mockTaskService: any;
  let mockAuthService: any;
  let mockStaffService: any;
  let refresh$: Subject<void>;

  const STAFF_ID = 'staff-abc-123';
  const STAFF_EMAIL = 'staff@example.com';

  const mockStaffList = [
    { id: STAFF_ID, email: STAFF_EMAIL, role: 'STAFF', active: true },
    { id: 'staff-other-456', email: 'other@example.com', role: 'STAFF', active: true }
  ];

  const mockTasks: TaskResponse[] = [
    {
      id: 'task-001',
      individualId: 'ind-001',
      interactionId: null,
      creatorId: 'staff-other-456',
      assigneeId: STAFF_ID,
      description: 'Short task description',
      status: 'TODO',
      dueDate: '2025-12-01',
      createdAt: '2025-01-15T10:30:00'
    },
    {
      id: 'task-002',
      individualId: 'ind-002',
      interactionId: 'int-001',
      creatorId: STAFF_ID,
      assigneeId: STAFF_ID,
      description: 'Another task with details',
      status: 'IN_PROGRESS',
      dueDate: null,
      createdAt: '2025-01-14T09:00:00'
    }
  ];

  const mockQueryResult: TaskQueryResult = {
    tasks: mockTasks,
    totalCount: 2,
    currentPage: 0,
    pageSize: 50
  };

  const emptyQueryResult: TaskQueryResult = {
    tasks: [],
    totalCount: 0,
    currentPage: 0,
    pageSize: 50
  };

  beforeEach(async () => {
    mockTaskService = {
      getTasks: vi.fn().mockReturnValue(of(mockQueryResult))
    };

    mockAuthService = {
      userEmail: vi.fn().mockReturnValue(STAFF_EMAIL),
      userRole: vi.fn().mockReturnValue('STAFF'),
      isAuthenticated: vi.fn().mockReturnValue(true),
      getAccessToken: () => 'valid-token'
    };

    mockStaffService = {
      findAll: vi.fn().mockReturnValue(of(mockStaffList))
    };

    refresh$ = new Subject<void>();

    await TestBed.configureTestingModule({
      imports: [TaskListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TaskService, useValue: mockTaskService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: StaffService, useValue: mockStaffService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TaskListComponent);
    component = fixture.componentInstance;
    component.refresh$ = refresh$;
  });

  describe('loading indicator', () => {
    it('should show loading indicator while task fetch is in progress', () => {
      const taskSubject = new Subject<TaskQueryResult>();
      mockTaskService.getTasks.mockReturnValue(taskSubject.asObservable());

      fixture.detectChanges(); // triggers ngOnInit → resolveCurrentStaffAndLoad → loadTasks

      const loading = fixture.debugElement.query(By.css('.loading-indicator'));
      expect(loading).toBeTruthy();
    });

    it('should hide loading indicator after tasks are fetched', () => {
      fixture.detectChanges();

      const loading = fixture.debugElement.query(By.css('.loading-indicator'));
      expect(loading).toBeFalsy();
    });

    it('should show loading indicator only during task list fetching', fakeAsync(() => {
      const taskSubject = new Subject<TaskQueryResult>();
      mockTaskService.getTasks.mockReturnValue(taskSubject.asObservable());

      fixture.detectChanges();

      // Loading should be visible
      let loading = fixture.debugElement.query(By.css('.loading-indicator'));
      expect(loading).toBeTruthy();

      // Emit result
      taskSubject.next(mockQueryResult);
      taskSubject.complete();
      tick();
      fixture.detectChanges();

      // Loading should be gone
      loading = fixture.debugElement.query(By.css('.loading-indicator'));
      expect(loading).toBeFalsy();
    }));
  });

  describe('filter controls', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should fetch tasks with assigneeId matching current user on initial load', () => {
      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ assigneeId: STAFF_ID })
      );
    });

    it('should call backend with status filter param when status is changed', () => {
      mockTaskService.getTasks.mockClear();

      const statusSelect = fixture.debugElement.query(By.css('#status-filter'));
      expect(statusSelect).toBeTruthy();

      statusSelect.nativeElement.value = 'TODO';
      statusSelect.nativeElement.dispatchEvent(new Event('change'));
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'TODO', assigneeId: STAFF_ID })
      );
    });

    it('should call backend with correct sortBy and sortOrder on initial load', () => {
      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({
          sortBy: 'createdDate',
          sortOrder: 'desc'
        })
      );
    });
  });

  describe('empty state', () => {
    beforeEach(() => {
      mockTaskService.getTasks.mockReturnValue(of(emptyQueryResult));
      fixture.detectChanges();
    });

    it('should display empty state message when no tasks match filters', () => {
      const emptyMessage = fixture.debugElement.query(By.css('.empty-message'));
      expect(emptyMessage).toBeTruthy();
      expect(emptyMessage.nativeElement.textContent.toLowerCase()).toContain('no tasks');
    });

    it('should not show task items when list is empty', () => {
      const taskItems = fixture.debugElement.queryAll(By.css('.task-item'));
      expect(taskItems.length).toBe(0);
    });
  });

  describe('error and retry', () => {
    beforeEach(() => {
      mockTaskService.getTasks.mockReturnValue(
        throwError(() => new Error('Network error'))
      );
      fixture.detectChanges();
    });

    it('should display error message when fetch fails', () => {
      const errorMessage = fixture.debugElement.query(By.css('.error-message'));
      expect(errorMessage).toBeTruthy();
    });

    it('should display retry button when fetch fails', () => {
      const retryButton = fixture.debugElement.query(By.css('.retry-btn'));
      expect(retryButton).toBeTruthy();
    });

    it('should display error message AND retry action together', () => {
      const errorContainer = fixture.debugElement.query(By.css('.error-container'));
      expect(errorContainer).toBeTruthy();

      const errorMessage = errorContainer.query(By.css('.error-message'));
      const retryButton = errorContainer.query(By.css('.retry-btn'));

      // Both must be present together — error is never shown without retry
      expect(errorMessage).toBeTruthy();
      expect(retryButton).toBeTruthy();
    });

    it('should retry fetching tasks when retry button is clicked', () => {
      mockTaskService.getTasks.mockClear();
      mockTaskService.getTasks.mockReturnValue(of(mockQueryResult));

      const retryButton = fixture.debugElement.query(By.css('.retry-btn'));
      retryButton.nativeElement.click();
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalled();
    });
  });

  describe('task description truncation', () => {
    it('should truncate descriptions longer than 100 characters with ellipsis', () => {
      const longDescription = 'A'.repeat(150);
      const taskWithLongDesc: TaskQueryResult = {
        tasks: [{
          ...mockTasks[0],
          description: longDescription
        }],
        totalCount: 1,
        currentPage: 0,
        pageSize: 50
      };

      mockTaskService.getTasks.mockReturnValue(of(taskWithLongDesc));
      fixture.detectChanges();

      const taskItems = fixture.debugElement.queryAll(By.css('.task-item'));
      expect(taskItems.length).toBe(1);

      const descriptionEl = taskItems[0].query(By.css('.task-description'));
      expect(descriptionEl).toBeTruthy();

      const displayedText = descriptionEl.nativeElement.textContent.trim();
      expect(displayedText.length).toBeLessThanOrEqual(103); // 100 chars + "..."
      expect(displayedText).toContain('...');
      expect(displayedText.startsWith('A'.repeat(100))).toBe(true);
    });

    it('should not truncate descriptions with 100 or fewer characters', () => {
      const shortDescription = 'B'.repeat(100);
      const taskWithShortDesc: TaskQueryResult = {
        tasks: [{
          ...mockTasks[0],
          description: shortDescription
        }],
        totalCount: 1,
        currentPage: 0,
        pageSize: 50
      };

      mockTaskService.getTasks.mockReturnValue(of(taskWithShortDesc));
      fixture.detectChanges();

      const taskItems = fixture.debugElement.queryAll(By.css('.task-item'));
      const descriptionEl = taskItems[0].query(By.css('.task-description'));
      const displayedText = descriptionEl.nativeElement.textContent.trim();

      expect(displayedText).toBe(shortDescription);
      expect(displayedText).not.toContain('...');
    });
  });

  describe('task click', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should set selectedTask when a task item is clicked', () => {
      const taskItems = fixture.debugElement.queryAll(By.css('.task-item'));
      expect(taskItems.length).toBeGreaterThan(0);

      taskItems[0].nativeElement.click();
      fixture.detectChanges();

      expect(component.selectedTask()).toEqual(mockTasks[0]);
    });
  });

  describe('refresh', () => {
    it('should reload tasks when refresh$ emits', () => {
      fixture.detectChanges();
      mockTaskService.getTasks.mockClear();
      mockTaskService.getTasks.mockReturnValue(of(mockQueryResult));

      refresh$.next();
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalled();
    });
  });
});
