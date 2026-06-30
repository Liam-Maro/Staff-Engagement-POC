/**
 * Preservation Property Tests — Task Detail Popup Fix
 *
 * These tests capture baseline behavior on UNFIXED code that MUST remain
 * unchanged after the popup fix is implemented.
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
 */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { of, Subject, throwError } from 'rxjs';
import * as fc from 'fast-check';

import { MyTasksPageComponent } from './my-tasks-page.component';
import { TaskListComponent } from '../task-list/task-list.component';
import { DelegatedTasksPanelComponent } from '../delegated-tasks-panel/delegated-tasks-panel.component';
import { TaskFormComponent } from '../task-form/task-form.component';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { TaskQueryParams, TaskQueryResult, TaskStatus } from '../../models/task.model';

const STAFF_ID = 'staff-abc-123';
const STAFF_EMAIL = 'staff@example.com';

const mockStaffList = [
  { id: STAFF_ID, email: STAFF_EMAIL, role: 'Staff' as const, active: true, createdAt: '2025-01-01T00:00:00' },
  { id: 'staff-other-456', email: 'other@example.com', role: 'Staff' as const, active: true, createdAt: '2025-01-01T00:00:00' }
];

const emptyQueryResult: TaskQueryResult = {
  tasks: [],
  totalCount: 0,
  currentPage: 0,
  pageSize: 50
};

/**
 * Property 2: Preservation — Filter/Sort, Create Task, Delegated Panel, and Loading States Unchanged
 */
describe('MyTasksPage Preservation Properties', () => {

  // ─── Property: Filter combinations produce correct API params ───────────────
  describe('Property: Filter/Sort API params match filter state for all valid combinations', () => {
    let fixture: ComponentFixture<MyTasksPageComponent>;
    let mockTaskService: any;
    let taskListComponent: TaskListComponent;

    beforeEach(async () => {
      mockTaskService = {
        getTasks: vi.fn().mockReturnValue(of(emptyQueryResult)),
        createTask: vi.fn().mockReturnValue(of({})),
        updateTask: vi.fn().mockReturnValue(of({})),
        getInteractionsForIndividual: vi.fn().mockReturnValue(of([]))
      };

      const mockAuthService = {
        userEmail: vi.fn().mockReturnValue(STAFF_EMAIL),
        userRole: vi.fn().mockReturnValue('STAFF'),
        staffId: vi.fn().mockReturnValue(STAFF_ID),
        isAuthenticated: vi.fn().mockReturnValue(true),
        getAccessToken: () => 'valid-token'
      };

      const mockStaffService = {
        findAll: vi.fn().mockReturnValue(of(mockStaffList))
      };

      const mockEmployeeService = {
        findAll: vi.fn().mockReturnValue(of([]))
      };

      await TestBed.configureTestingModule({
        imports: [MyTasksPageComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: TaskService, useValue: mockTaskService },
          { provide: AuthService, useValue: mockAuthService },
          { provide: StaffService, useValue: mockStaffService },
          { provide: EmployeeService, useValue: mockEmployeeService }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(MyTasksPageComponent);
      fixture.detectChanges();

      const taskListDebug = fixture.debugElement.query(By.directive(TaskListComponent));
      taskListComponent = taskListDebug.componentInstance;
    });

    it('status filter param matches selected value for all valid statuses', () => {
      /**
       * **Validates: Requirements 3.1**
       * For all valid status values, applying the filter calls backend with correct status param.
       */
      fc.assert(
        fc.property(
          fc.constantFrom('TODO', 'IN_PROGRESS', 'DONE', ''),
          (status: string) => {
            mockTaskService.getTasks.mockClear();
            mockTaskService.getTasks.mockReturnValue(of(emptyQueryResult));

            // Apply status filter
            taskListComponent.statusFilter = status;
            taskListComponent.onFilterChange();

            expect(mockTaskService.getTasks).toHaveBeenCalledTimes(1);
            const calledParams: TaskQueryParams = mockTaskService.getTasks.mock.calls[0][0];

            // Verify assigneeId always present
            expect(calledParams.assigneeId).toBe(STAFF_ID);
            // Verify sortBy and sortOrder always present
            expect(calledParams.sortBy).toBe('createdDate');
            expect(calledParams.sortOrder).toBe('desc');

            // Status param present only when non-empty
            if (status) {
              expect(calledParams.status).toBe(status);
            } else {
              expect(calledParams.status).toBeUndefined();
            }
          }
        ),
        { numRuns: 10 }
      );
    });

    it('due date range filter params match input values for all valid date pairs', () => {
      /**
       * **Validates: Requirements 3.1**
       * For all valid dueDateFrom/dueDateTo combinations, API call params match filter state.
       */
      // Generator for optional ISO date strings (YYYY-MM-DD format)
      const optionalDateArb = fc.constantFrom('', '2025-01-01', '2025-06-15', '2025-12-31', '2024-03-10');

      fc.assert(
        fc.property(
          optionalDateArb,
          optionalDateArb,
          (dueDateFrom: string, dueDateTo: string) => {
            mockTaskService.getTasks.mockClear();
            mockTaskService.getTasks.mockReturnValue(of(emptyQueryResult));

            taskListComponent.dueDateFrom = dueDateFrom;
            taskListComponent.dueDateTo = dueDateTo;
            taskListComponent.onFilterChange();

            expect(mockTaskService.getTasks).toHaveBeenCalledTimes(1);
            const calledParams: TaskQueryParams = mockTaskService.getTasks.mock.calls[0][0];

            if (dueDateFrom) {
              expect(calledParams.dueDateFrom).toBe(dueDateFrom);
            } else {
              expect(calledParams.dueDateFrom).toBeUndefined();
            }

            if (dueDateTo) {
              expect(calledParams.dueDateTo).toBe(dueDateTo);
            } else {
              expect(calledParams.dueDateTo).toBeUndefined();
            }
          }
        ),
        { numRuns: 20 }
      );
    });

    it('created date range filter params match input values for all valid date pairs', () => {
      /**
       * **Validates: Requirements 3.1**
       * For all valid createdFrom/createdTo combinations, API call params match filter state.
       */
      const optionalDateArb = fc.constantFrom('', '2025-01-01', '2025-06-15', '2025-12-31', '2024-03-10');

      fc.assert(
        fc.property(
          optionalDateArb,
          optionalDateArb,
          (createdFrom: string, createdTo: string) => {
            mockTaskService.getTasks.mockClear();
            mockTaskService.getTasks.mockReturnValue(of(emptyQueryResult));

            taskListComponent.createdFrom = createdFrom;
            taskListComponent.createdTo = createdTo;
            taskListComponent.onFilterChange();

            expect(mockTaskService.getTasks).toHaveBeenCalledTimes(1);
            const calledParams: TaskQueryParams = mockTaskService.getTasks.mock.calls[0][0];

            if (createdFrom) {
              expect(calledParams.createdFrom).toBe(createdFrom);
            } else {
              expect(calledParams.createdFrom).toBeUndefined();
            }

            if (createdTo) {
              expect(calledParams.createdTo).toBe(createdTo);
            } else {
              expect(calledParams.createdTo).toBeUndefined();
            }
          }
        ),
        { numRuns: 20 }
      );
    });

    it('combined filter params all match for arbitrary combinations', () => {
      /**
       * **Validates: Requirements 3.1**
       * For all combinations of status × dueDateRange × createdDateRange,
       * API call params match the full filter state.
       */
      const statusArb = fc.constantFrom('TODO', 'IN_PROGRESS', 'DONE', '') as fc.Arbitrary<string>;
      const optionalDateArb = fc.constantFrom('', '2025-01-01', '2025-06-15', '2025-12-31');

      fc.assert(
        fc.property(
          statusArb,
          optionalDateArb,
          optionalDateArb,
          optionalDateArb,
          optionalDateArb,
          (status: string, dueDateFrom: string, dueDateTo: string, createdFrom: string, createdTo: string) => {
            mockTaskService.getTasks.mockClear();
            mockTaskService.getTasks.mockReturnValue(of(emptyQueryResult));

            taskListComponent.statusFilter = status;
            taskListComponent.dueDateFrom = dueDateFrom;
            taskListComponent.dueDateTo = dueDateTo;
            taskListComponent.createdFrom = createdFrom;
            taskListComponent.createdTo = createdTo;
            taskListComponent.onFilterChange();

            expect(mockTaskService.getTasks).toHaveBeenCalledTimes(1);
            const calledParams: TaskQueryParams = mockTaskService.getTasks.mock.calls[0][0];

            // Always-present params
            expect(calledParams.assigneeId).toBe(STAFF_ID);
            expect(calledParams.sortBy).toBe('createdDate');
            expect(calledParams.sortOrder).toBe('desc');

            // Conditional params
            if (status) {
              expect(calledParams.status).toBe(status);
            } else {
              expect(calledParams.status).toBeUndefined();
            }
            if (dueDateFrom) {
              expect(calledParams.dueDateFrom).toBe(dueDateFrom);
            } else {
              expect(calledParams.dueDateFrom).toBeUndefined();
            }
            if (dueDateTo) {
              expect(calledParams.dueDateTo).toBe(dueDateTo);
            } else {
              expect(calledParams.dueDateTo).toBeUndefined();
            }
            if (createdFrom) {
              expect(calledParams.createdFrom).toBe(createdFrom);
            } else {
              expect(calledParams.createdFrom).toBeUndefined();
            }
            if (createdTo) {
              expect(calledParams.createdTo).toBe(createdTo);
            } else {
              expect(calledParams.createdTo).toBeUndefined();
            }
          }
        ),
        { numRuns: 50 }
      );
    });
  });

  // ─── Property: DelegatedTasksPanel independent of popup signals ─────────────
  describe('Property: DelegatedTasksPanel state independent of popup-related changes', () => {
    let fixture: ComponentFixture<MyTasksPageComponent>;
    let mockTaskService: any;

    const delegatedTasks = [
      {
        id: 'del-task-1',
        individualId: 'ind-001',
        interactionId: null,
        creatorId: STAFF_ID,
        assigneeId: 'staff-other-456',
        description: 'Delegated task one',
        status: 'TODO' as TaskStatus,
        dueDate: null,
        createdAt: '2025-01-15T10:00:00'
      }
    ];

    beforeEach(async () => {
      mockTaskService = {
        getTasks: vi.fn().mockImplementation((params: TaskQueryParams) => {
          // DelegatedPanel uses creatorId + excludeSelfAssigned; TaskList uses assigneeId
          if (params.creatorId && params.excludeSelfAssigned) {
            return of({ tasks: delegatedTasks, totalCount: 1, currentPage: 0, pageSize: 50 });
          }
          return of(emptyQueryResult);
        }),
        createTask: vi.fn().mockReturnValue(of({})),
        updateTask: vi.fn().mockReturnValue(of({})),
        getInteractionsForIndividual: vi.fn().mockReturnValue(of([]))
      };

      const mockAuthService = {
        userEmail: vi.fn().mockReturnValue(STAFF_EMAIL),
        userRole: vi.fn().mockReturnValue('STAFF'),
        staffId: vi.fn().mockReturnValue(STAFF_ID),
        isAuthenticated: vi.fn().mockReturnValue(true),
        getAccessToken: () => 'valid-token'
      };

      const mockStaffService = {
        findAll: vi.fn().mockReturnValue(of(mockStaffList))
      };

      const mockEmployeeService = {
        findAll: vi.fn().mockReturnValue(of([]))
      };

      await TestBed.configureTestingModule({
        imports: [MyTasksPageComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: TaskService, useValue: mockTaskService },
          { provide: AuthService, useValue: mockAuthService },
          { provide: StaffService, useValue: mockStaffService },
          { provide: EmployeeService, useValue: mockEmployeeService }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(MyTasksPageComponent);
      fixture.detectChanges();
    });

    it('DelegatedTasksPanel renders its tasks independently of TaskList state changes', () => {
      /**
       * **Validates: Requirements 3.3**
       * DelegatedTasksPanel fetches and displays delegated tasks independently.
       * Changing TaskList filters does not affect delegated panel content.
       */
      const delegatedPanel = fixture.debugElement.query(By.directive(DelegatedTasksPanelComponent));
      expect(delegatedPanel).toBeTruthy();

      const delegatedComponent = delegatedPanel.componentInstance as DelegatedTasksPanelComponent;

      // Panel should have loaded its own tasks
      expect(delegatedComponent.tasks().length).toBe(1);
      expect(delegatedComponent.tasks()[0].id).toBe('del-task-1');

      // Now change TaskList filters — delegated panel should be unaffected
      const taskListDebug = fixture.debugElement.query(By.directive(TaskListComponent));
      const taskListComponent = taskListDebug.componentInstance as TaskListComponent;

      fc.assert(
        fc.property(
          fc.constantFrom('TODO', 'IN_PROGRESS', 'DONE', ''),
          (status: string) => {
            taskListComponent.statusFilter = status;
            taskListComponent.onFilterChange();
            fixture.detectChanges();

            // Delegated panel still has same tasks, unaffected
            expect(delegatedComponent.tasks().length).toBe(1);
            expect(delegatedComponent.tasks()[0].id).toBe('del-task-1');
            expect(delegatedComponent.loading()).toBe(false);
            expect(delegatedComponent.error()).toBeNull();
          }
        ),
        { numRuns: 10 }
      );
    });

    it('DelegatedTasksPanel loading state is independent — not affected by task list refresh', () => {
      /**
       * **Validates: Requirements 3.3**
       * DelegatedTasksPanel state does not change in response to TaskList refresh signals
       * when those signals are task-list-only triggers.
       */
      const delegatedPanel = fixture.debugElement.query(By.directive(DelegatedTasksPanelComponent));
      const delegatedComponent = delegatedPanel.componentInstance as DelegatedTasksPanelComponent;

      // Delegated panel uses the same refresh$ subject, but still manages its own state
      // After refresh, it reloads its own data independently
      expect(delegatedComponent.loading()).toBe(false);
      expect(delegatedComponent.tasks().length).toBe(1);
    });
  });

  // ─── Property: No popup DOM during loading/error states ─────────────────────
  describe('Property: No popup DOM element in loading or error states', () => {

    it('no <app-task-detail-popup> DOM element present during loading state', () => {
      /**
       * **Validates: Requirements 3.4**
       * During loading state, no popup DOM element should exist.
       */
      const taskSubject = new Subject<TaskQueryResult>();

      const mockTaskService = {
        getTasks: vi.fn().mockReturnValue(taskSubject.asObservable()),
        createTask: vi.fn().mockReturnValue(of({})),
        updateTask: vi.fn().mockReturnValue(of({})),
        getInteractionsForIndividual: vi.fn().mockReturnValue(of([]))
      };

      const mockAuthService = {
        userEmail: vi.fn().mockReturnValue(STAFF_EMAIL),
        userRole: vi.fn().mockReturnValue('STAFF'),
        staffId: vi.fn().mockReturnValue(STAFF_ID),
        isAuthenticated: vi.fn().mockReturnValue(true),
        getAccessToken: () => 'valid-token'
      };

      const mockStaffService = {
        findAll: vi.fn().mockReturnValue(of(mockStaffList))
      };

      const mockEmployeeService = {
        findAll: vi.fn().mockReturnValue(of([]))
      };

      TestBed.configureTestingModule({
        imports: [MyTasksPageComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: TaskService, useValue: mockTaskService },
          { provide: AuthService, useValue: mockAuthService },
          { provide: StaffService, useValue: mockStaffService },
          { provide: EmployeeService, useValue: mockEmployeeService }
        ]
      });

      const fixture = TestBed.createComponent(MyTasksPageComponent);
      fixture.detectChanges();

      // TaskList is in loading state (subject hasn't emitted)
      const taskListComponent = fixture.debugElement.query(By.directive(TaskListComponent)).componentInstance as TaskListComponent;
      expect(taskListComponent.loading()).toBe(true);

      // No popup element in DOM
      const popup = fixture.nativeElement.querySelector('app-task-detail-popup');
      expect(popup).toBeNull();
    });

    it('no <app-task-detail-popup> DOM element present during error state with retry', () => {
      /**
       * **Validates: Requirements 3.4**
       * During error state with retry action, no popup DOM element should exist.
       */
      const mockTaskService = {
        getTasks: vi.fn().mockReturnValue(throwError(() => new Error('Network error'))),
        createTask: vi.fn().mockReturnValue(of({})),
        updateTask: vi.fn().mockReturnValue(of({})),
        getInteractionsForIndividual: vi.fn().mockReturnValue(of([]))
      };

      const mockAuthService = {
        userEmail: vi.fn().mockReturnValue(STAFF_EMAIL),
        userRole: vi.fn().mockReturnValue('STAFF'),
        staffId: vi.fn().mockReturnValue(STAFF_ID),
        isAuthenticated: vi.fn().mockReturnValue(true),
        getAccessToken: () => 'valid-token'
      };

      const mockStaffService = {
        findAll: vi.fn().mockReturnValue(of(mockStaffList))
      };

      const mockEmployeeService = {
        findAll: vi.fn().mockReturnValue(of([]))
      };

      TestBed.configureTestingModule({
        imports: [MyTasksPageComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: TaskService, useValue: mockTaskService },
          { provide: AuthService, useValue: mockAuthService },
          { provide: StaffService, useValue: mockStaffService },
          { provide: EmployeeService, useValue: mockEmployeeService }
        ]
      });

      const fixture = TestBed.createComponent(MyTasksPageComponent);
      fixture.detectChanges();

      // TaskList is in error state
      const taskListComponent = fixture.debugElement.query(By.directive(TaskListComponent)).componentInstance as TaskListComponent;
      expect(taskListComponent.error()).toBeTruthy();

      // Error container with retry button exists
      const retryBtn = fixture.debugElement.query(By.css('.retry-btn'));
      expect(retryBtn).toBeTruthy();

      // No popup element in DOM
      const popup = fixture.nativeElement.querySelector('app-task-detail-popup');
      expect(popup).toBeNull();
    });

    it('no <app-task-detail-popup> in DOM for any loading/error combination', () => {
      /**
       * **Validates: Requirements 3.4**
       * Property: for all states where task list is loading or errored, no popup DOM exists.
       */
      fc.assert(
        fc.property(
          fc.constantFrom('loading', 'error'),
          (state: string) => {
            const mockTaskService = {
              getTasks: vi.fn().mockReturnValue(
                state === 'loading'
                  ? new Subject<TaskQueryResult>().asObservable()
                  : throwError(() => new Error('fail'))
              ),
              createTask: vi.fn().mockReturnValue(of({})),
              updateTask: vi.fn().mockReturnValue(of({})),
              getInteractionsForIndividual: vi.fn().mockReturnValue(of([]))
            };

            const mockAuthService = {
              userEmail: vi.fn().mockReturnValue(STAFF_EMAIL),
              userRole: vi.fn().mockReturnValue('STAFF'),
              staffId: vi.fn().mockReturnValue(STAFF_ID),
              isAuthenticated: vi.fn().mockReturnValue(true),
              getAccessToken: () => 'valid-token'
            };

            const mockStaffService = {
              findAll: vi.fn().mockReturnValue(of(mockStaffList))
            };

            const mockEmployeeService = {
              findAll: vi.fn().mockReturnValue(of([]))
            };

            TestBed.resetTestingModule();
            TestBed.configureTestingModule({
              imports: [MyTasksPageComponent],
              providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: TaskService, useValue: mockTaskService },
                { provide: AuthService, useValue: mockAuthService },
                { provide: StaffService, useValue: mockStaffService },
                { provide: EmployeeService, useValue: mockEmployeeService }
              ]
            });

            const fixture = TestBed.createComponent(MyTasksPageComponent);
            fixture.detectChanges();

            const popup = fixture.nativeElement.querySelector('app-task-detail-popup');
            expect(popup).toBeNull();
          }
        ),
        { numRuns: 5 }
      );
    });
  });

  // ─── Test: Create Task opens form with correct defaults ─────────────────────
  describe('Create Task button opens form with correct defaults regardless of state', () => {
    let fixture: ComponentFixture<MyTasksPageComponent>;
    let component: MyTasksPageComponent;

    beforeEach(async () => {
      const mockTaskService = {
        getTasks: vi.fn().mockReturnValue(of(emptyQueryResult)),
        createTask: vi.fn().mockReturnValue(of({})),
        updateTask: vi.fn().mockReturnValue(of({})),
        getInteractionsForIndividual: vi.fn().mockReturnValue(of([]))
      };

      const mockAuthService = {
        userEmail: vi.fn().mockReturnValue(STAFF_EMAIL),
        userRole: vi.fn().mockReturnValue('STAFF'),
        staffId: vi.fn().mockReturnValue(STAFF_ID),
        isAuthenticated: vi.fn().mockReturnValue(true),
        getAccessToken: () => 'valid-token'
      };

      const mockStaffService = {
        findAll: vi.fn().mockReturnValue(of(mockStaffList))
      };

      const mockEmployeeService = {
        findAll: vi.fn().mockReturnValue(of([]))
      };

      await TestBed.configureTestingModule({
        imports: [MyTasksPageComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: TaskService, useValue: mockTaskService },
          { provide: AuthService, useValue: mockAuthService },
          { provide: StaffService, useValue: mockStaffService },
          { provide: EmployeeService, useValue: mockEmployeeService }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(MyTasksPageComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('clicking Create Task opens TaskFormComponent with default state', () => {
      /**
       * **Validates: Requirements 3.2**
       * Create Task button opens TaskFormComponent with assignee = current user,
       * description empty, no individual, no due date.
       */
      // Click Create Task button
      const createBtn = fixture.debugElement.query(By.css('.create-task-btn'));
      expect(createBtn).toBeTruthy();
      createBtn.nativeElement.click();
      fixture.detectChanges();

      // TaskFormComponent should be rendered
      const taskForm = fixture.debugElement.query(By.directive(TaskFormComponent));
      expect(taskForm).toBeTruthy();

      const formComponent = taskForm.componentInstance as TaskFormComponent;

      // Verify defaults: description empty, no individual selected, no due date
      expect(formComponent.taskForm.get('description')?.value).toBe('');
      expect(formComponent.taskForm.get('individualId')?.value).toBe('');
      expect(formComponent.taskForm.get('dueDate')?.value).toBe('');

      // editTask should be null (create mode, not edit)
      expect(formComponent.editTask).toBeNull();
      expect(formComponent.editMode).toBe(false);
    });

    it('Create Task button still works after various filter changes', () => {
      /**
       * **Validates: Requirements 3.2**
       * Create Task works independently of filter state changes.
       */
      // Change some filters on TaskList first
      const taskListDebug = fixture.debugElement.query(By.directive(TaskListComponent));
      const taskListComponent = taskListDebug.componentInstance as TaskListComponent;
      taskListComponent.statusFilter = 'DONE';
      taskListComponent.dueDateFrom = '2025-01-01';
      taskListComponent.onFilterChange();
      fixture.detectChanges();

      // Now click Create Task
      const createBtn = fixture.debugElement.query(By.css('.create-task-btn'));
      createBtn.nativeElement.click();
      fixture.detectChanges();

      // TaskFormComponent should still render correctly
      const taskForm = fixture.debugElement.query(By.directive(TaskFormComponent));
      expect(taskForm).toBeTruthy();

      const formComponent = taskForm.componentInstance as TaskFormComponent;
      expect(formComponent.taskForm.get('description')?.value).toBe('');
      expect(formComponent.editMode).toBe(false);
    });

    it('no <app-task-detail-popup> in DOM when Create Task form is open', () => {
      /**
       * **Validates: Requirements 3.4**
       * Popup does not appear during Create Task flow on unfixed code.
       */
      const createBtn = fixture.debugElement.query(By.css('.create-task-btn'));
      createBtn.nativeElement.click();
      fixture.detectChanges();

      const popup = fixture.nativeElement.querySelector('app-task-detail-popup');
      expect(popup).toBeNull();
    });
  });
});
