/**
 * Bug Condition Exploration Test — Task Detail Popup Fix
 *
 * **Validates: Requirements 1.1, 1.2, 2.1, 2.2**
 *
 * Property 1: Bug Condition — Task Click Does Not Render Detail Popup
 *
 * This test encodes the EXPECTED behavior: clicking or pressing Enter on a task
 * item in the task list should render `<app-task-detail-popup>` in DOM.
 *
 * On UNFIXED code this test is EXPECTED TO FAIL, confirming the bug exists:
 * - TaskListComponent has no @Output emitter for task selection
 * - MyTasksPageComponent template has no <app-task-detail-popup> reference
 *
 * After the fix is applied, this same test should PASS.
 */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { of } from 'rxjs';
import fc from 'fast-check';

import { MyTasksPageComponent } from './my-tasks-page.component';
import { TaskListComponent } from '../task-list/task-list.component';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { TaskResponse, TaskStatus } from '../../models/task.model';
import { StaffMember } from '../../../staff/models/staff.models';

// --- Arbitraries for generating random TaskResponse objects ---

const taskStatusArb: fc.Arbitrary<TaskStatus> = fc.constantFrom('TODO', 'IN_PROGRESS', 'DONE');

const taskResponseArb: fc.Arbitrary<TaskResponse> = fc.record({
  id: fc.uuid(),
  individualId: fc.uuid(),
  interactionId: fc.option(fc.uuid(), { nil: null }),
  creatorId: fc.uuid(),
  assigneeId: fc.constant('staff-1'),
  description: fc.string({ minLength: 1, maxLength: 200 }),
  status: taskStatusArb,
  dueDate: fc.option(fc.date({ min: new Date('2020-01-01'), max: new Date('2030-12-31') }).map(d => d.toISOString().split('T')[0]), { nil: null }),
  createdAt: fc.date({ min: new Date('2020-01-01'), max: new Date('2025-12-31') }).map(d => d.toISOString()),
});

const staffMembersFixture: StaffMember[] = [
  { id: 'staff-1', email: 'staff@example.com', role: 'Staff', active: true, createdAt: '2024-01-01T00:00:00Z' },
  { id: 'staff-2', email: 'admin@example.com', role: 'Admin', active: true, createdAt: '2024-01-01T00:00:00Z' },
];

describe('Bug Condition Exploration: Task Click Does Not Render Detail Popup', () => {
  let component: MyTasksPageComponent;
  let fixture: ComponentFixture<MyTasksPageComponent>;
  let mockTaskService: any;
  let mockAuthService: any;
  let mockStaffService: any;
  let mockEmployeeService: any;

  function setupTestBed(tasks: TaskResponse[]) {
    mockTaskService = {
      getTasks: vi.fn().mockReturnValue(of({ tasks, totalCount: tasks.length, currentPage: 0, pageSize: 50 })),
      createTask: vi.fn().mockReturnValue(of({})),
      updateTask: vi.fn().mockReturnValue(of({})),
      deleteTask: vi.fn().mockReturnValue(of(undefined)),
      getInteractionsForIndividual: vi.fn().mockReturnValue(of([])),
    };

    mockAuthService = {
      userEmail: vi.fn().mockReturnValue('staff@example.com'),
      userRole: vi.fn().mockReturnValue('Staff'),
      staffId: vi.fn().mockReturnValue('staff-1'),
      isAuthenticated: vi.fn().mockReturnValue(true),
      getAccessToken: () => 'valid-token',
    };

    mockStaffService = {
      findAll: vi.fn().mockReturnValue(of(staffMembersFixture)),
    };

    mockEmployeeService = {
      findAll: vi.fn().mockReturnValue(of([])),
    };
  }

  async function createComponent(tasks: TaskResponse[]): Promise<void> {
    setupTestBed(tasks);

    await TestBed.configureTestingModule({
      imports: [MyTasksPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TaskService, useValue: mockTaskService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: StaffService, useValue: mockStaffService },
        { provide: EmployeeService, useValue: mockEmployeeService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MyTasksPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    // Allow async task loading to complete
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  /**
   * Test: Click on task item → assert <app-task-detail-popup> renders in DOM
   *
   * Bug condition: After clicking task item, no popup appears because
   * TaskListComponent has no @Output emitter and MyTasksPageComponent
   * template has no popup reference.
   */
  it('should render <app-task-detail-popup> when a task item is clicked', async () => {
    const sampleTask: TaskResponse = {
      id: 'task-1',
      individualId: 'ind-1',
      interactionId: null,
      creatorId: 'staff-1',
      assigneeId: 'staff-1',
      description: 'Follow up with employee about training plan',
      status: 'TODO',
      dueDate: '2025-03-15',
      createdAt: '2025-01-10T10:00:00Z',
    };

    await createComponent([sampleTask]);

    // Find and click the task item
    const taskItem = fixture.debugElement.query(By.css('.task-item'));
    expect(taskItem).toBeTruthy();
    taskItem.nativeElement.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // Assert: popup should exist in DOM
    const popup = fixture.nativeElement.querySelector('app-task-detail-popup');
    expect(popup).toBeTruthy();
  });

  /**
   * Test: Enter keydown on focused task item → assert popup renders
   */
  it('should render <app-task-detail-popup> when Enter is pressed on a focused task item', async () => {
    const sampleTask: TaskResponse = {
      id: 'task-2',
      individualId: 'ind-2',
      interactionId: 'interaction-1',
      creatorId: 'staff-2',
      assigneeId: 'staff-1',
      description: 'Schedule follow-up meeting',
      status: 'IN_PROGRESS',
      dueDate: null,
      createdAt: '2025-02-01T08:00:00Z',
    };

    await createComponent([sampleTask]);

    // Find and dispatch Enter key on task item
    const taskItem = fixture.debugElement.query(By.css('.task-item'));
    expect(taskItem).toBeTruthy();
    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true });
    taskItem.nativeElement.dispatchEvent(enterEvent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // Assert: popup should exist in DOM
    const popup = fixture.nativeElement.querySelector('app-task-detail-popup');
    expect(popup).toBeTruthy();
  });

  /**
   * Test: Popup receives correct [task], [staffMembers], [currentStaffId] input bindings
   */
  it('should pass correct task, staffMembers, and currentStaffId to popup', async () => {
    const sampleTask: TaskResponse = {
      id: 'task-3',
      individualId: 'ind-3',
      interactionId: 'interaction-5',
      creatorId: 'staff-1',
      assigneeId: 'staff-1',
      description: 'Review portfolio updates',
      status: 'DONE',
      dueDate: '2025-04-01',
      createdAt: '2025-03-01T12:00:00Z',
    };

    await createComponent([sampleTask]);

    // Click the task item
    const taskItem = fixture.debugElement.query(By.css('.task-item'));
    expect(taskItem).toBeTruthy();
    taskItem.nativeElement.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // Assert popup exists and has correct bindings
    const popup = fixture.nativeElement.querySelector('app-task-detail-popup');
    expect(popup).toBeTruthy();

    // If popup renders as Angular component, check its inputs via debugElement
    const popupDebug = fixture.debugElement.query(By.css('app-task-detail-popup'));
    expect(popupDebug).toBeTruthy();
    expect(popupDebug.componentInstance.task).toEqual(sampleTask);
    expect(popupDebug.componentInstance.staffMembers).toEqual(staffMembersFixture);
    expect(popupDebug.componentInstance.currentStaffId).toBe('staff-1');
  });

  /**
   * Property-based test: For any randomly generated TaskResponse,
   * clicking its task item should render the popup.
   *
   * **Validates: Requirements 1.1, 1.2, 2.1, 2.2**
   */
  it('should render popup for any valid TaskResponse after click (property-based)', async () => {
    await fc.assert(
      fc.asyncProperty(taskResponseArb, async (task) => {
        TestBed.resetTestingModule();
        await createComponent([task]);

        const taskItem = fixture.debugElement.query(By.css('.task-item'));
        if (!taskItem) {
          // If task list didn't render item, skip (shouldn't happen)
          return;
        }

        taskItem.nativeElement.click();
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        const popup = fixture.nativeElement.querySelector('app-task-detail-popup');
        expect(popup).toBeTruthy();
      }),
      { numRuns: 10 } // Keep small for test speed in component tests
    );
  });
});
