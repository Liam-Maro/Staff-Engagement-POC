import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { InteractionDetailComponent } from '../components/interaction-detail/interaction-detail.component';
import { InteractionListComponent } from '../components/interaction-list/interaction-list.component';
import { InteractionService } from '../services/interaction.service';
import { TaskService } from '../../tasks/services/task.service';
import { AuthService } from '../../auth/services/auth.service';
import { StaffService } from '../../staff/services/staff.service';
import { EmployeeService } from '../../employees/services/employee.service';
import { InteractionResponse, PageResponse } from '../models/interaction.model';
import { StaffMember } from '../../staff/models/staff.models';
import { Employee } from '../../employees/models/employee.models';
import { InteractionSummary, TaskResponse } from '../../tasks/models/task.model';

// ─── Test Data ───────────────────────────────────────────────────────────────

const STAFF_ID = '22222222-2222-2222-2222-222222222222';
const EMPLOYEE_ID = '11111111-1111-1111-1111-111111111111';
const INTERACTION_ID = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';

const mockStaff: StaffMember[] = [
  { id: STAFF_ID, email: 'staff@test.com', role: 'Staff', active: true, createdAt: '2024-01-01' }
];

const mockEmployees: Employee[] = [
  { id: EMPLOYEE_ID, firstName: 'Jane', lastName: 'Doe', email: 'jane@test.com', department: 'Engineering', jobTitle: 'Dev', hireDate: '2023-01-01', active: true }
];

const mockInteraction: InteractionResponse = {
  id: INTERACTION_ID,
  employeeId: EMPLOYEE_ID,
  staffId: STAFF_ID,
  type: 'CHECK_IN',
  notes: 'Discussed goals',
  occurredAt: '2024-06-15T10:30:00',
  createdAt: '2024-06-15T10:35:00',
  updatedAt: '2024-06-15T10:35:00'
};

const mockInteractionSummaries: InteractionSummary[] = [
  { id: INTERACTION_ID, employeeId: EMPLOYEE_ID, staffId: STAFF_ID, type: 'CHECK_IN', notes: 'Discussed goals', occurredAt: '2024-06-15T10:30:00', createdAt: '2024-06-15T10:35:00' }
];

const mockPageResponse: PageResponse<InteractionResponse> = {
  content: [mockInteraction],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20
};

const mockTaskResponse: TaskResponse = {
  id: 'task-1',
  individualId: EMPLOYEE_ID,
  interactionId: INTERACTION_ID,
  creatorId: STAFF_ID,
  assigneeId: STAFF_ID,
  description: 'Follow up',
  status: 'TODO',
  dueDate: null,
  createdAt: '2024-06-16T10:00:00'
};

// ─── Detail View Integration Tests ──────────────────────────────────────────

describe('Integration: InteractionDetail → Modal → Submit', () => {
  let fixture: ComponentFixture<InteractionDetailComponent>;
  let component: InteractionDetailComponent;
  let interactionServiceMock: { getById: ReturnType<typeof vi.fn>; getAll: ReturnType<typeof vi.fn> };
  let taskServiceMock: { createTask: ReturnType<typeof vi.fn>; getInteractionsForIndividual: ReturnType<typeof vi.fn> };
  let staffServiceMock: { findAll: ReturnType<typeof vi.fn> };
  let employeeServiceMock: { findAll: ReturnType<typeof vi.fn> };
  let authServiceMock: { userEmail: ReturnType<typeof vi.fn>; userRole: ReturnType<typeof vi.fn>; isAuthenticated: ReturnType<typeof vi.fn>; staffId: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    interactionServiceMock = {
      getById: vi.fn().mockReturnValue(of(mockInteraction)),
      getAll: vi.fn().mockReturnValue(of(mockPageResponse))
    };

    taskServiceMock = {
      createTask: vi.fn().mockReturnValue(of(mockTaskResponse)),
      getInteractionsForIndividual: vi.fn().mockReturnValue(of(mockInteractionSummaries))
    };

    staffServiceMock = {
      findAll: vi.fn().mockReturnValue(of(mockStaff))
    };

    employeeServiceMock = {
      findAll: vi.fn().mockReturnValue(of(mockEmployees))
    };

    authServiceMock = {
      userEmail: vi.fn().mockReturnValue('staff@test.com'),
      userRole: vi.fn().mockReturnValue('Staff'),
      isAuthenticated: vi.fn().mockReturnValue(true),
      staffId: vi.fn().mockReturnValue(STAFF_ID)
    };

    await TestBed.configureTestingModule({
      imports: [InteractionDetailComponent],
      providers: [
        { provide: InteractionService, useValue: interactionServiceMock },
        { provide: TaskService, useValue: taskServiceMock },
        { provide: StaffService, useValue: staffServiceMock },
        { provide: EmployeeService, useValue: employeeServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: (key: string) => key === 'id' ? INTERACTION_ID : null } }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InteractionDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load interaction and show Create Follow-up Task button', () => {
    const el = fixture.nativeElement as HTMLElement;
    const btn = el.querySelector('.detail-actions button.btn-primary');
    expect(btn).toBeTruthy();
    expect(btn?.textContent).toContain('Create Follow-up Task');
  });

  it('should open modal with TaskFormComponent when button is clicked', () => {
    const el = fixture.nativeElement as HTMLElement;
    const btn = el.querySelector('.detail-actions button.btn-primary') as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    // TaskFormComponent renders as overlay
    const overlay = el.querySelector('.task-form-overlay');
    expect(overlay).toBeTruthy();
    expect(el.querySelector('.task-form-container')).toBeTruthy();
  });

  it('full flow: open modal → submit → modal closes → success toast appears', fakeAsync(() => {
    // Open modal
    component.createFollowUpTask();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();

    // Fill in description (required field — TaskFormComponent validates it)
    const descTextarea = el.querySelector('#description') as HTMLTextAreaElement;
    expect(descTextarea).toBeTruthy();
    descTextarea.value = 'Follow up on quarterly goals';
    descTextarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // assigneeId should be auto-set to current staff
    // individualId should be pre-populated from context
    // Submit the form
    const submitBtn = el.querySelector('.submit-btn') as HTMLButtonElement;
    expect(submitBtn).toBeTruthy();
    submitBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Modal should close
    expect(el.querySelector('.task-form-overlay')).toBeNull();
    // Success toast should appear
    expect(component.toastMessage()).toBe('Follow-up task created successfully');
    const toast = el.querySelector('.toast-success');
    expect(toast).toBeTruthy();
    expect(toast?.textContent).toContain('Follow-up task created successfully');
  }));

  it('modal → submit with error → modal stays open → inline errors shown', fakeAsync(() => {
    // Mock createTask to return 400 with inline errors
    taskServiceMock.createTask.mockReturnValue(throwError(() =>
      new HttpErrorResponse({
        status: 400,
        error: { errors: [{ field: 'description', message: 'Description is required' }] }
      })
    ));

    // Open modal
    component.createFollowUpTask();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();

    // Fill description to pass form validation (but server rejects)
    const descTextarea = el.querySelector('#description') as HTMLTextAreaElement;
    descTextarea.value = 'Some text';
    descTextarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // Submit
    const submitBtn = el.querySelector('.submit-btn') as HTMLButtonElement;
    submitBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Modal should still be open
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();
    // Inline error should be visible
    expect(el.querySelector('.inline-error')).toBeTruthy();
    expect(el.querySelector('.inline-error')?.textContent).toContain('Description is required');
    // No success toast
    expect(component.toastMessage()).toBeNull();
  }));

  it('modal → Escape key → modal closes → view state preserved', fakeAsync(() => {
    // Open modal
    component.createFollowUpTask();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();

    // Dispatch Escape key event
    const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
    document.dispatchEvent(escapeEvent);
    fixture.detectChanges();

    // Modal should close
    expect(el.querySelector('.task-form-overlay')).toBeNull();
    // View state preserved
    expect(component.interaction()).toEqual(mockInteraction);
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
  }));
});

// ─── List View Integration Tests ─────────────────────────────────────────────

describe('Integration: InteractionList → Modal → Submit', () => {
  let fixture: ComponentFixture<InteractionListComponent>;
  let component: InteractionListComponent;
  let interactionServiceMock: { getById: ReturnType<typeof vi.fn>; getAll: ReturnType<typeof vi.fn> };
  let taskServiceMock: { createTask: ReturnType<typeof vi.fn>; getInteractionsForIndividual: ReturnType<typeof vi.fn> };
  let staffServiceMock: { findAll: ReturnType<typeof vi.fn> };
  let employeeServiceMock: { findAll: ReturnType<typeof vi.fn> };
  let authServiceMock: { userEmail: ReturnType<typeof vi.fn>; userRole: ReturnType<typeof vi.fn>; isAuthenticated: ReturnType<typeof vi.fn>; staffId: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    interactionServiceMock = {
      getById: vi.fn().mockReturnValue(of(mockInteraction)),
      getAll: vi.fn().mockReturnValue(of(mockPageResponse))
    };

    taskServiceMock = {
      createTask: vi.fn().mockReturnValue(of(mockTaskResponse)),
      getInteractionsForIndividual: vi.fn().mockReturnValue(of(mockInteractionSummaries))
    };

    staffServiceMock = {
      findAll: vi.fn().mockReturnValue(of(mockStaff))
    };

    employeeServiceMock = {
      findAll: vi.fn().mockReturnValue(of(mockEmployees))
    };

    authServiceMock = {
      userEmail: vi.fn().mockReturnValue('staff@test.com'),
      userRole: vi.fn().mockReturnValue('Staff'),
      isAuthenticated: vi.fn().mockReturnValue(true),
      staffId: vi.fn().mockReturnValue(STAFF_ID)
    };

    await TestBed.configureTestingModule({
      imports: [InteractionListComponent],
      providers: [
        { provide: InteractionService, useValue: interactionServiceMock },
        { provide: TaskService, useValue: taskServiceMock },
        { provide: StaffService, useValue: staffServiceMock },
        { provide: EmployeeService, useValue: employeeServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InteractionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load interactions and display Create Task action in table', () => {
    const el = fixture.nativeElement as HTMLElement;
    const createTaskLinks = el.querySelectorAll('.actions-cell .btn-link');
    // Should have both "View" and "Create Task" links
    const createTaskLink = Array.from(createTaskLinks).find(link => link.textContent?.includes('Create Task'));
    expect(createTaskLink).toBeTruthy();
  });

  it('should open modal with correct context when Create Task is clicked', () => {
    const el = fixture.nativeElement as HTMLElement;
    const createTaskLink = Array.from(el.querySelectorAll('.actions-cell .btn-link'))
      .find(link => link.textContent?.includes('Create Task')) as HTMLElement;

    createTaskLink.click();
    fixture.detectChanges();

    // TaskFormComponent modal should render
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();
    expect(component.taskFormContext()).toEqual({
      interactionId: INTERACTION_ID,
      employeeId: EMPLOYEE_ID,
      interactionType: 'CHECK_IN',
      interactionDate: '2024-06-15T10:30:00'
    });
  });

  it('full flow: list → open modal → submit → modal closes → success toast → filters preserved', fakeAsync(() => {
    // Set filters before opening modal
    component.selectedEmployeeId = EMPLOYEE_ID;
    component.selectedType = 'CHECK_IN';
    fixture.detectChanges();

    // Open modal
    component.openCreateTask(mockInteraction);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();

    // Fill description
    const descTextarea = el.querySelector('#description') as HTMLTextAreaElement;
    descTextarea.value = 'Follow up task from list';
    descTextarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // Submit
    const submitBtn = el.querySelector('.submit-btn') as HTMLButtonElement;
    submitBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Modal should close
    expect(el.querySelector('.task-form-overlay')).toBeNull();
    // Success toast should appear
    expect(component.toastMessage()).toBe('Follow-up task created successfully');
    const toast = el.querySelector('.toast-success');
    expect(toast).toBeTruthy();
    expect(toast?.textContent).toContain('Follow-up task created successfully');
    // Filters should be preserved
    expect(component.selectedEmployeeId).toBe(EMPLOYEE_ID);
    expect(component.selectedType).toBe('CHECK_IN');
    // Interactions should still be loaded
    expect(component.interactions().length).toBe(1);
  }));

  it('modal → submit with error → modal stays open → inline errors shown', fakeAsync(() => {
    // Mock createTask to return 400 with field errors
    taskServiceMock.createTask.mockReturnValue(throwError(() =>
      new HttpErrorResponse({
        status: 400,
        error: { errors: [{ field: 'description', message: 'Description is required' }] }
      })
    ));

    // Open modal
    component.openCreateTask(mockInteraction);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();

    // Fill description
    const descTextarea = el.querySelector('#description') as HTMLTextAreaElement;
    descTextarea.value = 'Task text';
    descTextarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // Submit
    const submitBtn = el.querySelector('.submit-btn') as HTMLButtonElement;
    submitBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Modal should remain open
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();
    // Inline error should show
    expect(el.querySelector('.inline-error')).toBeTruthy();
    expect(el.querySelector('.inline-error')?.textContent).toContain('Description is required');
    // No success toast
    expect(component.toastMessage()).toBeNull();
  }));

  it('modal → Escape key → modal closes → view state preserved', fakeAsync(() => {
    // Set filters
    component.selectedEmployeeId = EMPLOYEE_ID;
    component.selectedType = 'CHECK_IN';

    // Open modal
    component.openCreateTask(mockInteraction);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();

    // Dispatch Escape key event
    const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
    document.dispatchEvent(escapeEvent);
    fixture.detectChanges();

    // Modal should close
    expect(el.querySelector('.task-form-overlay')).toBeNull();
    // Filters preserved
    expect(component.selectedEmployeeId).toBe(EMPLOYEE_ID);
    expect(component.selectedType).toBe('CHECK_IN');
    // Data preserved
    expect(component.interactions().length).toBe(1);
    expect(component.isLoading()).toBe(false);
  }));

  it('modal → server error (500) → modal stays open → server error banner shown', fakeAsync(() => {
    // Mock createTask to return 500
    taskServiceMock.createTask.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' })
    ));

    // Open modal
    component.openCreateTask(mockInteraction);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;

    // Fill description
    const descTextarea = el.querySelector('#description') as HTMLTextAreaElement;
    descTextarea.value = 'Task text';
    descTextarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // Submit
    const submitBtn = el.querySelector('.submit-btn') as HTMLButtonElement;
    submitBtn.click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Modal stays open
    expect(el.querySelector('.task-form-overlay')).toBeTruthy();
    // Server error banner shown
    expect(el.querySelector('.server-error')).toBeTruthy();
    expect(el.querySelector('.server-error')?.textContent).toContain('Server error');
    // No success toast
    expect(component.toastMessage()).toBeNull();
  }));
});
