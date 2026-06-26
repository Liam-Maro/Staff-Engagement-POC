import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
import { of, throwError, Subject } from 'rxjs';

import { DelegatedTasksPanelComponent } from './delegated-tasks-panel.component';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { TaskQueryResult, TaskResponse } from '../../models/task.model';

describe('DelegatedTasksPanelComponent', () => {
  let component: DelegatedTasksPanelComponent;
  let fixture: ComponentFixture<DelegatedTasksPanelComponent>;
  let mockTaskService: any;
  let mockAuthService: any;
  let mockStaffService: any;

  const STAFF_ID = 'staff-creator-001';
  const STAFF_EMAIL = 'creator@example.com';
  const ASSIGNEE_ID = 'staff-assignee-002';

  const mockStaffList = [
    { id: STAFF_ID, email: STAFF_EMAIL, role: 'STAFF', active: true, createdAt: '2025-01-01T00:00:00' },
    { id: ASSIGNEE_ID, email: 'assignee@example.com', role: 'STAFF', active: true, createdAt: '2025-01-01T00:00:00' }
  ];

  const mockDelegatedTasks: TaskResponse[] = [
    {
      id: 'task-del-001',
      individualId: 'ind-001',
      interactionId: null,
      creatorId: STAFF_ID,
      assigneeId: ASSIGNEE_ID,
      description: 'Delegated task for follow up on mentoring',
      status: 'TODO',
      dueDate: '2025-12-01',
      createdAt: '2025-01-15T10:30:00'
    },
    {
      id: 'task-del-002',
      individualId: 'ind-002',
      interactionId: 'int-001',
      creatorId: STAFF_ID,
      assigneeId: ASSIGNEE_ID,
      description: 'Another delegated task with a longer description that exceeds the truncation limit of one hundred chars definitely',
      status: 'IN_PROGRESS',
      dueDate: null,
      createdAt: '2025-01-14T09:00:00'
    }
  ];

  const mockQueryResult: TaskQueryResult = {
    tasks: mockDelegatedTasks,
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
      isAuthenticated: vi.fn().mockReturnValue(true)
    };

    mockStaffService = {
      findAll: vi.fn().mockReturnValue(of(mockStaffList))
    };

    await TestBed.configureTestingModule({
      imports: [DelegatedTasksPanelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TaskService, useValue: mockTaskService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: StaffService, useValue: mockStaffService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DelegatedTasksPanelComponent);
    component = fixture.componentInstance;
    component.refresh$ = new Subject<void>();
  });

  describe('correct query params (creatorId + excludeSelfAssigned)', () => {
    it('should call getTasks with creatorId set to current user ID', () => {
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ creatorId: STAFF_ID })
      );
    });

    it('should call getTasks with excludeSelfAssigned set to true', () => {
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ excludeSelfAssigned: true })
      );
    });

    it('should call getTasks with sortBy createdDate and sortOrder desc', () => {
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({
          sortBy: 'createdDate',
          sortOrder: 'desc'
        })
      );
    });

    it('should call getTasks with size 50', () => {
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ size: 50 })
      );
    });

    it('should include all required params together in a single call', () => {
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalledWith({
        creatorId: STAFF_ID,
        excludeSelfAssigned: true,
        sortBy: 'createdDate',
        sortOrder: 'desc',
        size: 50
      });
    });
  });

  describe('error + retry displayed together on panel error', () => {
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
      const retryBtn = fixture.debugElement.query(By.css('.retry-btn'));
      expect(retryBtn).toBeTruthy();
    });

    it('should display error message AND retry action together (never error without retry)', () => {
      const errorContainer = fixture.debugElement.query(By.css('.error-container'));
      expect(errorContainer).toBeTruthy();

      const errorMessage = errorContainer.query(By.css('.error-message'));
      const retryBtn = errorContainer.query(By.css('.retry-btn'));

      // Both must be present together
      expect(errorMessage).toBeTruthy();
      expect(retryBtn).toBeTruthy();
    });

    it('should retry loading delegated tasks when retry button is clicked', () => {
      mockTaskService.getTasks.mockClear();
      mockTaskService.getTasks.mockReturnValue(of(mockQueryResult));

      const retryBtn = fixture.debugElement.query(By.css('.retry-btn'));
      retryBtn.nativeElement.click();
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalled();
    });

    it('should clear error and show tasks after successful retry', () => {
      mockTaskService.getTasks.mockClear();
      mockTaskService.getTasks.mockReturnValue(of(mockQueryResult));

      const retryBtn = fixture.debugElement.query(By.css('.retry-btn'));
      retryBtn.nativeElement.click();
      fixture.detectChanges();

      const errorContainer = fixture.debugElement.query(By.css('.error-container'));
      expect(errorContainer).toBeFalsy();

      const taskItems = fixture.debugElement.queryAll(By.css('.delegated-task-item'));
      expect(taskItems.length).toBe(2);
    });
  });

  describe('empty state messages', () => {
    beforeEach(() => {
      mockTaskService.getTasks.mockReturnValue(of(emptyQueryResult));
      fixture.detectChanges();
    });

    it('should display "no delegated tasks" message when task list is empty', () => {
      const emptyMessage = fixture.debugElement.query(By.css('.empty-message'));
      expect(emptyMessage).toBeTruthy();
      expect(emptyMessage.nativeElement.textContent.toLowerCase()).toContain('delegated');
    });

    it('should not display any task items when empty', () => {
      const taskItems = fixture.debugElement.queryAll(By.css('.delegated-task-item'));
      expect(taskItems.length).toBe(0);
    });

    it('should not display error container when list is simply empty', () => {
      const errorContainer = fixture.debugElement.query(By.css('.error-container'));
      expect(errorContainer).toBeFalsy();
    });
  });

  describe('task display', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should display delegated tasks after successful load', () => {
      const taskItems = fixture.debugElement.queryAll(By.css('.delegated-task-item'));
      expect(taskItems.length).toBe(2);
    });

    it('should truncate descriptions longer than 100 chars with ellipsis', () => {
      const taskItems = fixture.debugElement.queryAll(By.css('.delegated-task-item'));
      const secondTaskDesc = taskItems[1].query(By.css('.task-description'));
      const text = secondTaskDesc.nativeElement.textContent.trim();

      expect(text.length).toBeLessThanOrEqual(103); // 100 + "..."
      expect(text).toContain('...');
    });

    it('should show assignee name for each task', () => {
      const assignees = fixture.debugElement.queryAll(By.css('.task-assignee'));
      expect(assignees.length).toBe(2);
      // Should show email since that's how getAssigneeName maps
      expect(assignees[0].nativeElement.textContent).toContain('assignee@example.com');
    });

    it('should show status for each task', () => {
      const statuses = fixture.debugElement.queryAll(By.css('.task-status'));
      expect(statuses.length).toBe(2);
      expect(statuses[0].nativeElement.textContent).toContain('TODO');
      expect(statuses[1].nativeElement.textContent).toContain('IN_PROGRESS');
    });
  });

  describe('refresh behavior', () => {
    it('should reload delegated tasks when refresh$ emits', () => {
      fixture.detectChanges();
      mockTaskService.getTasks.mockClear();
      mockTaskService.getTasks.mockReturnValue(of(mockQueryResult));

      component.refresh$.next();
      fixture.detectChanges();

      expect(mockTaskService.getTasks).toHaveBeenCalled();
    });
  });
});
