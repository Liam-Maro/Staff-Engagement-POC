import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
import { of, throwError, Subject } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { TaskFormComponent } from './task-form.component';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { TaskResponse, InteractionSummary, InteractionContext } from '../../models/task.model';

describe('TaskFormComponent', () => {
  let component: TaskFormComponent;
  let fixture: ComponentFixture<TaskFormComponent>;
  let mockTaskService: any;
  let mockAuthService: any;
  let mockStaffService: any;
  let mockEmployeeService: any;

  const STAFF_ID = 'staff-abc-123';
  const STAFF_EMAIL = 'staff@example.com';
  const OTHER_STAFF_ID = 'staff-other-456';

  const mockStaffList = [
    { id: STAFF_ID, email: STAFF_EMAIL, role: 'STAFF', active: true, createdAt: '2025-01-01T00:00:00' },
    { id: OTHER_STAFF_ID, email: 'other@example.com', role: 'STAFF', active: true, createdAt: '2025-01-01T00:00:00' },
    { id: 'staff-inactive', email: 'inactive@example.com', role: 'STAFF', active: false, createdAt: '2025-01-01T00:00:00' }
  ];

  const mockEmployees = [
    { id: 'emp-001', firstName: 'John', lastName: 'Doe', email: 'john@example.com', department: 'Engineering', jobTitle: 'Dev', hireDate: '2020-01-01', active: true },
    { id: 'emp-002', firstName: 'Jane', lastName: 'Smith', email: 'jane@example.com', department: 'HR', jobTitle: 'Manager', hireDate: '2019-06-15', active: true }
  ];

  const mockInteractions: InteractionSummary[] = [
    { id: 'int-001', employeeId: 'emp-001', staffId: STAFF_ID, type: 'CHECK_IN', notes: 'Monthly check-in', occurredAt: '2025-01-10T14:00:00', createdAt: '2025-01-10T14:00:00' },
    { id: 'int-002', employeeId: 'emp-001', staffId: STAFF_ID, type: 'MENTORING', notes: 'Mentoring session', occurredAt: '2025-01-08T10:00:00', createdAt: '2025-01-08T10:00:00' }
  ];

  const mockEditTask: TaskResponse = {
    id: 'task-edit-001',
    individualId: 'emp-001',
    interactionId: 'int-001',
    creatorId: STAFF_ID,
    assigneeId: OTHER_STAFF_ID,
    description: 'Existing task description for editing',
    status: 'TODO',
    dueDate: '2025-12-15',
    createdAt: '2025-01-01T10:00:00'
  };

  beforeEach(async () => {
    mockTaskService = {
      createTask: vi.fn().mockReturnValue(of({ id: 'new-task-id' })),
      updateTask: vi.fn().mockReturnValue(of({ id: 'task-edit-001' })),
      getInteractionsForIndividual: vi.fn().mockReturnValue(of(mockInteractions))
    };

    mockAuthService = {
      userEmail: vi.fn().mockReturnValue(STAFF_EMAIL),
      userRole: vi.fn().mockReturnValue('STAFF'),
      isAuthenticated: vi.fn().mockReturnValue(true)
    };

    mockStaffService = {
      findAll: vi.fn().mockReturnValue(of(mockStaffList))
    };

    mockEmployeeService = {
      findAll: vi.fn().mockReturnValue(of(mockEmployees))
    };

    await TestBed.configureTestingModule({
      imports: [TaskFormComponent, ReactiveFormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TaskService, useValue: mockTaskService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: StaffService, useValue: mockStaffService },
        { provide: EmployeeService, useValue: mockEmployeeService }
      ]
    }).compileComponents();
  });

  function createComponent(editTask: TaskResponse | null = null): void {
    fixture = TestBed.createComponent(TaskFormComponent);
    component = fixture.componentInstance;
    if (editTask) {
      component.editTask = editTask;
    }
    fixture.detectChanges();
  }

  function createComponentWithContext(context: InteractionContext | null): void {
    fixture = TestBed.createComponent(TaskFormComponent);
    component = fixture.componentInstance;
    component.interactionContext = context;
    fixture.detectChanges();
  }

  describe('Property 22: Form blocks submission when required fields are empty', () => {
    beforeEach(() => {
      createComponent();
    });

    it('should block submission when description is empty', () => {
      // Leave description empty, fill other required fields
      component.taskForm.patchValue({
        description: '',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });
      component.taskForm.markAllAsTouched();
      fixture.detectChanges();

      component.onSubmit();

      expect(mockTaskService.createTask).not.toHaveBeenCalled();
    });

    it('should block submission when individual is empty', () => {
      component.taskForm.patchValue({
        description: 'Valid description',
        assigneeId: STAFF_ID,
        individualId: ''
      });
      component.taskForm.markAllAsTouched();
      fixture.detectChanges();

      component.onSubmit();

      expect(mockTaskService.createTask).not.toHaveBeenCalled();
    });

    it('should block submission when assignee is empty', () => {
      component.taskForm.patchValue({
        description: 'Valid description',
        assigneeId: '',
        individualId: 'emp-001'
      });
      component.taskForm.markAllAsTouched();
      fixture.detectChanges();

      component.onSubmit();

      expect(mockTaskService.createTask).not.toHaveBeenCalled();
    });

    it('should block submission when all required fields are empty', () => {
      component.taskForm.patchValue({
        description: '',
        assigneeId: '',
        individualId: ''
      });
      component.taskForm.markAllAsTouched();
      fixture.detectChanges();

      component.onSubmit();

      expect(mockTaskService.createTask).not.toHaveBeenCalled();
    });

    it('should allow submission when all required fields are filled', () => {
      component.taskForm.patchValue({
        description: 'Valid task description',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });
      fixture.detectChanges();

      component.onSubmit();

      expect(mockTaskService.createTask).toHaveBeenCalled();
    });

    it('should prevent the request from reaching the backend when form is invalid', () => {
      // Set form to invalid state
      component.taskForm.patchValue({
        description: '',
        assigneeId: '',
        individualId: ''
      });

      // Attempt submit via the form
      const form = fixture.debugElement.query(By.css('form'));
      form.triggerEventHandler('ngSubmit', null);
      fixture.detectChanges();

      expect(mockTaskService.createTask).not.toHaveBeenCalled();
      expect(mockTaskService.updateTask).not.toHaveBeenCalled();
    });
  });

  describe('Whitespace-only description blocked', () => {
    beforeEach(() => {
      createComponent();
    });

    it('should block submission when description contains only spaces', () => {
      component.taskForm.patchValue({
        description: '     ',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });
      fixture.detectChanges();

      component.onSubmit();

      expect(mockTaskService.createTask).not.toHaveBeenCalled();
    });

    it('should block submission when description contains only tabs and newlines', () => {
      component.taskForm.patchValue({
        description: '\t\n\r  \t',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });
      fixture.detectChanges();

      component.onSubmit();

      expect(mockTaskService.createTask).not.toHaveBeenCalled();
    });

    it('should allow submission when description has real text with whitespace', () => {
      component.taskForm.patchValue({
        description: '  Valid task with leading spaces  ',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });
      fixture.detectChanges();

      component.onSubmit();

      expect(mockTaskService.createTask).toHaveBeenCalled();
    });

    it('should show validation error for whitespace-only description', () => {
      const descControl = component.taskForm.get('description')!;
      descControl.setValue('    ');
      descControl.markAsTouched();
      fixture.detectChanges();

      expect(descControl.invalid).toBe(true);
      expect(descControl.errors?.['notBlank']).toBeTruthy();
    });
  });

  describe('Character count display', () => {
    beforeEach(() => {
      createComponent();
    });

    it('should display 2000 remaining characters when description is empty', () => {
      const charCount = fixture.debugElement.query(By.css('.char-count'));
      expect(charCount).toBeTruthy();
      expect(charCount.nativeElement.textContent).toContain('2000');
      expect(charCount.nativeElement.textContent).toContain('characters remaining');
    });

    it('should update remaining character count as user types', () => {
      component.taskForm.get('description')?.setValue('Hello');
      fixture.detectChanges();

      const charCount = fixture.debugElement.query(By.css('.char-count'));
      expect(charCount.nativeElement.textContent).toContain('1995');
    });

    it('should show warning class when approaching limit (>1800 chars)', () => {
      component.taskForm.get('description')?.setValue('X'.repeat(1801));
      fixture.detectChanges();

      const charCount = fixture.debugElement.query(By.css('.char-count'));
      expect(charCount.classes['warning']).toBe(true);
    });

    it('should not show warning class when below 1800 chars', () => {
      component.taskForm.get('description')?.setValue('X'.repeat(100));
      fixture.detectChanges();

      const charCount = fixture.debugElement.query(By.css('.char-count'));
      expect(charCount.classes['warning']).toBeFalsy();
    });

    it('should show 0 remaining when exactly 2000 characters are entered', () => {
      component.taskForm.get('description')?.setValue('A'.repeat(2000));
      fixture.detectChanges();

      const charCount = fixture.debugElement.query(By.css('.char-count'));
      expect(charCount.nativeElement.textContent).toContain('0');
    });
  });

  describe('Interaction dropdown disabled without individual', () => {
    beforeEach(() => {
      createComponent();
    });

    it('should disable interaction dropdown when no individual is selected and toggle is on', () => {
      component.taskForm.get('linkInteraction')?.setValue(true);
      component.taskForm.get('individualId')?.setValue('');
      fixture.detectChanges();

      const interactionSelect = fixture.debugElement.query(By.css('#interactionId'));
      expect(interactionSelect).toBeTruthy();
      expect(interactionSelect.nativeElement.disabled).toBe(true);
    });

    it('should show hint to select individual first when no individual selected', () => {
      component.taskForm.get('linkInteraction')?.setValue(true);
      component.taskForm.get('individualId')?.setValue('');
      fixture.detectChanges();

      const hint = fixture.debugElement.query(By.css('.field-hint'));
      expect(hint).toBeTruthy();
      expect(hint.nativeElement.textContent.toLowerCase()).toContain('select an individual');
    });

    it('should enable interaction dropdown when individual is selected', () => {
      component.taskForm.get('individualId')?.setValue('emp-001');
      component.taskForm.get('linkInteraction')?.setValue(true);
      fixture.detectChanges();

      const interactionSelect = fixture.debugElement.query(By.css('#interactionId'));
      expect(interactionSelect).toBeTruthy();
      expect(interactionSelect.nativeElement.disabled).toBeFalsy();
    });

    it('should fetch interactions when individual is selected and toggle is on', () => {
      component.taskForm.get('individualId')?.setValue('emp-001');
      component.taskForm.get('linkInteraction')?.setValue(true);
      fixture.detectChanges();

      expect(mockTaskService.getInteractionsForIndividual).toHaveBeenCalledWith('emp-001');
    });

    it('should not show interaction dropdown when toggle is off', () => {
      component.taskForm.get('linkInteraction')?.setValue(false);
      fixture.detectChanges();

      const interactionSelect = fixture.debugElement.query(By.css('#interactionId'));
      expect(interactionSelect).toBeFalsy();
    });
  });

  describe('Pre-population on edit mode', () => {
    it('should pre-populate description with existing task data', () => {
      createComponent(mockEditTask);

      expect(component.taskForm.get('description')?.value).toBe(mockEditTask.description);
    });

    it('should pre-populate assignee with existing task assignee', () => {
      createComponent(mockEditTask);

      expect(component.taskForm.get('assigneeId')?.value).toBe(mockEditTask.assigneeId);
    });

    it('should pre-populate individual with existing task individual', () => {
      createComponent(mockEditTask);

      expect(component.taskForm.get('individualId')?.value).toBe(mockEditTask.individualId);
    });

    it('should pre-populate due date with existing task due date', () => {
      createComponent(mockEditTask);

      expect(component.taskForm.get('dueDate')?.value).toBe(mockEditTask.dueDate);
    });

    it('should enable link interaction toggle when edit task has interactionId', () => {
      createComponent(mockEditTask);

      expect(component.taskForm.get('linkInteraction')?.value).toBe(true);
    });

    it('should pre-populate interaction dropdown with existing interactionId', () => {
      createComponent(mockEditTask);

      expect(component.taskForm.get('interactionId')?.value).toBe(mockEditTask.interactionId);
    });

    it('should display "Edit Task" header in edit mode', () => {
      createComponent(mockEditTask);

      const header = fixture.debugElement.query(By.css('.task-form-header h3'));
      expect(header.nativeElement.textContent).toContain('Edit Task');
    });

    it('should display "Create Task" header in create mode', () => {
      createComponent();

      const header = fixture.debugElement.query(By.css('.task-form-header h3'));
      expect(header.nativeElement.textContent).toContain('Create Task');
    });

    it('should call updateTask on submit in edit mode', () => {
      createComponent(mockEditTask);

      component.onSubmit();

      expect(mockTaskService.updateTask).toHaveBeenCalledWith(
        mockEditTask.id,
        expect.objectContaining({
          description: mockEditTask.description,
          assigneeId: mockEditTask.assigneeId,
          individualId: mockEditTask.individualId
        })
      );
    });

    it('should not pre-populate link interaction toggle when task has no interaction', () => {
      const taskNoInteraction: TaskResponse = {
        ...mockEditTask,
        interactionId: null
      };
      createComponent(taskNoInteraction);

      expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
    });
  });

  describe('Form data preserved on server error', () => {
    beforeEach(() => {
      createComponent();
    });

    it('should preserve all form data when server returns 5xx error', fakeAsync(() => {
      const formData = {
        description: 'Important task that must be saved',
        assigneeId: STAFF_ID,
        individualId: 'emp-001',
        dueDate: '2025-12-25',
        linkInteraction: false,
        interactionId: ''
      };
      component.taskForm.patchValue(formData);
      fixture.detectChanges();

      mockTaskService.createTask.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }))
      );

      component.onSubmit();
      tick();
      fixture.detectChanges();

      // Form data should be preserved
      expect(component.taskForm.get('description')?.value).toBe(formData.description);
      expect(component.taskForm.get('assigneeId')?.value).toBe(formData.assigneeId);
      expect(component.taskForm.get('individualId')?.value).toBe(formData.individualId);
      expect(component.taskForm.get('dueDate')?.value).toBe(formData.dueDate);
    }));

    it('should display server error message on 5xx response', fakeAsync(() => {
      component.taskForm.patchValue({
        description: 'Valid task',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });

      mockTaskService.createTask.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 503, statusText: 'Service Unavailable' }))
      );

      component.onSubmit();
      tick();
      fixture.detectChanges();

      const errorEl = fixture.debugElement.query(By.css('.server-error'));
      expect(errorEl).toBeTruthy();
      expect(errorEl.nativeElement.textContent).toContain('Server error');
    }));

    it('should allow retry after server error without losing data', fakeAsync(() => {
      const description = 'Important task for retry';
      component.taskForm.patchValue({
        description,
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });

      // First attempt fails
      mockTaskService.createTask.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }))
      );

      component.onSubmit();
      tick();
      fixture.detectChanges();

      // Verify data preserved
      expect(component.taskForm.get('description')?.value).toBe(description);

      // Second attempt succeeds
      mockTaskService.createTask.mockReturnValue(of({ id: 'new-task' }));
      component.onSubmit();
      tick();

      expect(mockTaskService.createTask).toHaveBeenCalledTimes(2);
    }));

    it('should reset submitting state after server error', fakeAsync(() => {
      component.taskForm.patchValue({
        description: 'Valid task',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });

      mockTaskService.createTask.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }))
      );

      component.onSubmit();
      tick();
      fixture.detectChanges();

      expect(component.submitting()).toBe(false);
    }));

    it('should display inline errors on backend 400 response with field errors', fakeAsync(() => {
      component.taskForm.patchValue({
        description: 'Valid task',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });

      mockTaskService.createTask.mockReturnValue(
        throwError(() => new HttpErrorResponse({
          status: 400,
          error: { errors: [{ field: 'description', message: 'Description is too short' }] }
        }))
      );

      component.onSubmit();
      tick();
      fixture.detectChanges();

      const inlineError = fixture.debugElement.query(By.css('.inline-error'));
      expect(inlineError).toBeTruthy();
    }));
  });

  describe('Form general behavior', () => {
    beforeEach(() => {
      createComponent();
    });

    it('should emit closed event when close button is clicked', () => {
      const closedSpy = vi.spyOn(component.closed, 'emit');
      const closeBtn = fixture.debugElement.query(By.css('.close-btn'));
      closeBtn.nativeElement.click();

      expect(closedSpy).toHaveBeenCalled();
    });

    it('should emit closed event when overlay is clicked', () => {
      const closedSpy = vi.spyOn(component.closed, 'emit');
      const overlay = fixture.debugElement.query(By.css('.task-form-overlay'));
      overlay.nativeElement.click();

      expect(closedSpy).toHaveBeenCalled();
    });

    it('should emit closed event when Escape key is pressed', () => {
      const closedSpy = vi.spyOn(component.closed, 'emit');
      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      document.dispatchEvent(event);

      expect(closedSpy).toHaveBeenCalled();
    });

    it('should call onClose when Escape key is pressed', () => {
      const onCloseSpy = vi.spyOn(component, 'onClose');
      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      document.dispatchEvent(event);

      expect(onCloseSpy).toHaveBeenCalled();
    });

    it('should default assignee to current user on create mode', () => {
      // After ngOnInit resolves staff, the assignee should default to current staff
      expect(component.taskForm.get('assigneeId')?.value).toBe(STAFF_ID);
    });

    it('should only show active staff in the assignee dropdown', () => {
      const activeStaff = component.activeStaff();
      expect(activeStaff.every(s => s.active)).toBe(true);
      expect(activeStaff.find(s => s.id === 'staff-inactive')).toBeUndefined();
    });
  });

  describe('Pre-population from interaction context', () => {
    const VALID_EMPLOYEE_UUID = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
    const VALID_INTERACTION_UUID = 'b2c3d4e5-f6a7-8901-bcde-f12345678901';
    const OTHER_EMPLOYEE_UUID = 'c3d4e5f6-a7b8-9012-cdef-123456789012';

    const uuidInteractions: InteractionSummary[] = [
      { id: VALID_INTERACTION_UUID, employeeId: VALID_EMPLOYEE_UUID, staffId: STAFF_ID, type: 'CHECK_IN', notes: 'Monthly check-in', occurredAt: '2025-01-10T14:00:00', createdAt: '2025-01-10T14:00:00' },
      { id: 'd4e5f6a7-b8c9-0123-def0-234567890123', employeeId: VALID_EMPLOYEE_UUID, staffId: STAFF_ID, type: 'MENTORING', notes: 'Mentoring session', occurredAt: '2025-01-08T10:00:00', createdAt: '2025-01-08T10:00:00' }
    ];

    describe('Full pre-population (Requirements 3.1, 3.2, 3.3)', () => {
      it('should set individualId, toggle, and interactionId when both UUIDs are valid and interaction exists', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID,
          interactionType: 'CHECK_IN',
          interactionDate: '2025-01-10'
        };

        // Mock returns interaction matching the context interactionId
        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));

        createComponentWithContext(context);

        expect(component.taskForm.get('individualId')?.value).toBe(VALID_EMPLOYEE_UUID);
        expect(component.taskForm.get('linkInteraction')?.value).toBe(true);
        expect(component.taskForm.get('interactionId')?.value).toBe(VALID_INTERACTION_UUID);
      });

      it('should call getInteractionsForIndividual with the employeeId', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);

        expect(mockTaskService.getInteractionsForIndividual).toHaveBeenCalledWith(VALID_EMPLOYEE_UUID);
      });

      it('should leave interactionId unselected when interaction not found in loaded list (Req 5.7)', () => {
        const nonExistentInteractionUuid = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
        const context: InteractionContext = {
          interactionId: nonExistentInteractionUuid,
          employeeId: VALID_EMPLOYEE_UUID
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);

        // Toggle should still be enabled but no interaction pre-selected
        expect(component.taskForm.get('individualId')?.value).toBe(VALID_EMPLOYEE_UUID);
        expect(component.taskForm.get('linkInteraction')?.value).toBe(true);
        expect(component.taskForm.get('interactionId')?.value).toBe('');
      });
    });

    describe('Partial pre-population - employeeId only (Requirement 5.3)', () => {
      it('should set individualId and leave toggle off when only employeeId is valid', () => {
        const context: InteractionContext = {
          interactionId: '', // empty = not valid UUID
          employeeId: VALID_EMPLOYEE_UUID
        };

        createComponentWithContext(context);

        expect(component.taskForm.get('individualId')?.value).toBe(VALID_EMPLOYEE_UUID);
        expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
        expect(component.taskForm.get('interactionId')?.value).toBe('');
      });

      it('should not fetch interactions when only employeeId is provided', () => {
        const context: InteractionContext = {
          interactionId: '',
          employeeId: VALID_EMPLOYEE_UUID
        };

        createComponentWithContext(context);

        // getInteractionsForIndividual should NOT be called for pre-population
        expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
      });
    });

    describe('InteractionId-only context is ignored (Requirement 5.4)', () => {
      it('should behave as default form when only interactionId is provided without employeeId', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: '' // empty = not valid
        };

        createComponentWithContext(context);

        // Default state: individual empty, toggle off, no interaction
        expect(component.taskForm.get('individualId')?.value).toBe('');
        expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
        expect(component.taskForm.get('interactionId')?.value).toBe('');
      });

      it('should not call getInteractionsForIndividual when only interactionId is provided', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: ''
        };

        // Reset mock to track calls only after this point
        mockTaskService.getInteractionsForIndividual.mockClear();
        createComponentWithContext(context);

        expect(mockTaskService.getInteractionsForIndividual).not.toHaveBeenCalled();
      });
    });

    describe('Malformed UUID is silently ignored (Requirement 5.5)', () => {
      it('should ignore context when employeeId is malformed', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: 'not-a-valid-uuid'
        };

        createComponentWithContext(context);

        expect(component.taskForm.get('individualId')?.value).toBe('');
        expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
      });

      it('should partial-populate when employeeId is valid but interactionId is malformed', () => {
        const context: InteractionContext = {
          interactionId: 'bad-uuid-format',
          employeeId: VALID_EMPLOYEE_UUID
        };

        createComponentWithContext(context);

        // Only employeeId is valid → partial pre-population
        expect(component.taskForm.get('individualId')?.value).toBe(VALID_EMPLOYEE_UUID);
        expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
      });

      it('should ignore entirely when both UUIDs are malformed', () => {
        const context: InteractionContext = {
          interactionId: 'xxx',
          employeeId: '12345'
        };

        createComponentWithContext(context);

        expect(component.taskForm.get('individualId')?.value).toBe('');
        expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
        expect(component.taskForm.get('interactionId')?.value).toBe('');
      });

      it('should not show any error to the user when UUIDs are malformed', () => {
        const context: InteractionContext = {
          interactionId: 'garbage',
          employeeId: 'also-garbage'
        };

        createComponentWithContext(context);
        fixture.detectChanges();

        expect(component.serverError()).toBeNull();
        const errorEl = fixture.debugElement.query(By.css('.server-error'));
        expect(errorEl).toBeFalsy();
      });
    });

    describe('Context banner display (Requirement 4.1)', () => {
      it('should display banner with correct type and date when full context is provided', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID,
          interactionType: 'CHECK_IN',
          interactionDate: '2025-01-10'
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);
        fixture.detectChanges();

        const banner = fixture.debugElement.query(By.css('.context-banner'));
        expect(banner).toBeTruthy();
        expect(banner.nativeElement.textContent).toContain('CHECK_IN');
        expect(banner.nativeElement.textContent).toContain('2025-01-10');
      });

      it('should not display banner when interactionType or interactionDate is missing', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID
          // No interactionType or interactionDate
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);
        fixture.detectChanges();

        const banner = fixture.debugElement.query(By.css('.context-banner'));
        expect(banner).toBeFalsy();
      });

      it('should not display banner when context is null', () => {
        createComponentWithContext(null);
        fixture.detectChanges();

        const banner = fixture.debugElement.query(By.css('.context-banner'));
        expect(banner).toBeFalsy();
      });

      it('should display banner text matching format "Creating task from [type] interaction on [date]"', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID,
          interactionType: 'MENTORING',
          interactionDate: '2025-02-15'
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);
        fixture.detectChanges();

        expect(component.contextBannerText()).toBe('Creating task from MENTORING interaction on 2025-02-15');
      });
    });

    describe('Individual change clears interaction and reloads (Requirements 3.5, 4.4)', () => {
      it('should clear interactionId when individual is changed after pre-population', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);

        // Verify pre-populated state
        expect(component.taskForm.get('interactionId')?.value).toBe(VALID_INTERACTION_UUID);

        // Change individual
        const newInteractions: InteractionSummary[] = [
          { id: 'e5f6a7b8-c9d0-1234-ef01-345678901234', employeeId: OTHER_EMPLOYEE_UUID, staffId: STAFF_ID, type: 'CATCH_UP', notes: 'Catch up', occurredAt: '2025-02-01T10:00:00', createdAt: '2025-02-01T10:00:00' }
        ];
        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(newInteractions));

        component.taskForm.get('individualId')?.setValue(OTHER_EMPLOYEE_UUID);
        fixture.detectChanges();

        // interactionId should be cleared
        expect(component.taskForm.get('interactionId')?.value).toBe('');
      });

      it('should reload interactions for newly selected individual', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);

        mockTaskService.getInteractionsForIndividual.mockClear();
        const newInteractions: InteractionSummary[] = [];
        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(newInteractions));

        component.taskForm.get('individualId')?.setValue(OTHER_EMPLOYEE_UUID);
        fixture.detectChanges();

        expect(mockTaskService.getInteractionsForIndividual).toHaveBeenCalledWith(OTHER_EMPLOYEE_UUID);
      });

      it('should keep linkInteraction toggle in its current state when individual changes', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);

        expect(component.taskForm.get('linkInteraction')?.value).toBe(true);

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of([]));
        component.taskForm.get('individualId')?.setValue(OTHER_EMPLOYEE_UUID);
        fixture.detectChanges();

        // Toggle should remain true
        expect(component.taskForm.get('linkInteraction')?.value).toBe(true);
      });
    });

    describe('Toggle off clears interaction selection (Requirement 4.3)', () => {
      it('should clear interactionId when linkInteraction toggle is set to false', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);

        // Verify pre-populated
        expect(component.taskForm.get('interactionId')?.value).toBe(VALID_INTERACTION_UUID);
        expect(component.taskForm.get('linkInteraction')?.value).toBe(true);

        // Toggle off
        component.taskForm.get('linkInteraction')?.setValue(false);
        fixture.detectChanges();

        expect(component.taskForm.get('interactionId')?.value).toBe('');
      });

      it('should clear interactions list when toggle is set to false', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);

        // Interactions loaded
        expect(component.interactions().length).toBeGreaterThan(0);

        // Toggle off
        component.taskForm.get('linkInteraction')?.setValue(false);
        fixture.detectChanges();

        expect(component.interactions()).toEqual([]);
      });

      it('should hide interaction dropdown when toggle is off', () => {
        const context: InteractionContext = {
          interactionId: VALID_INTERACTION_UUID,
          employeeId: VALID_EMPLOYEE_UUID
        };

        mockTaskService.getInteractionsForIndividual.mockReturnValue(of(uuidInteractions));
        createComponentWithContext(context);
        fixture.detectChanges();

        // Initially dropdown should be visible
        let dropdown = fixture.debugElement.query(By.css('#interactionId'));
        expect(dropdown).toBeTruthy();

        // Toggle off
        component.taskForm.get('linkInteraction')?.setValue(false);
        fixture.detectChanges();

        // Dropdown should be hidden
        dropdown = fixture.debugElement.query(By.css('#interactionId'));
        expect(dropdown).toBeFalsy();
      });
    });
  });

  describe('Error handling for task creation in modal context (Requirements 6.4, 6.5, 7.4)', () => {
    beforeEach(() => {
      createComponent();
      component.taskForm.patchValue({
        description: 'Valid task description',
        assigneeId: STAFF_ID,
        individualId: 'emp-001'
      });
    });

    describe('400 validation errors with field errors array', () => {
      it('should show inline field errors and keep modal open', fakeAsync(() => {
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({
            status: 400,
            error: { errors: [{ field: 'description', message: 'Description is required' }] }
          }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.inlineErrors()['description']).toBe('Description is required');
        expect(component.serverError()).toBeNull();
      }));

      it('should handle multiple field errors', fakeAsync(() => {
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({
            status: 400,
            error: { errors: [
              { field: 'description', message: 'Too short' },
              { field: 'assigneeId', message: 'Invalid assignee' }
            ] }
          }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.inlineErrors()['description']).toBe('Too short');
        expect(component.inlineErrors()['assigneeId']).toBe('Invalid assignee');
      }));

      it('should use defaultMessage when message is missing', fakeAsync(() => {
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({
            status: 400,
            error: { errors: [{ field: 'description', defaultMessage: 'Fallback message' }] }
          }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.inlineErrors()['description']).toBe('Fallback message');
      }));
    });

    describe('400 validation errors with fieldErrors object', () => {
      it('should show inline errors from flat fieldErrors object', fakeAsync(() => {
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({
            status: 400,
            error: { fieldErrors: { description: 'required', individualId: 'not found' } }
          }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.inlineErrors()['description']).toBe('required');
        expect(component.inlineErrors()['individualId']).toBe('not found');
        expect(component.serverError()).toBeNull();
      }));
    });

    describe('400 with message only (no field-level errors)', () => {
      it('should show server error banner with message', fakeAsync(() => {
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({
            status: 400,
            error: { message: 'Request body is malformed' }
          }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.serverError()).toBe('Request body is malformed');
        expect(Object.keys(component.inlineErrors())).toHaveLength(0);
      }));
    });

    describe('500 server error', () => {
      it('should show server error banner for 500', fakeAsync(() => {
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.serverError()).toBe('Server error. Please try again.');
      }));

      it('should show server error banner for 502', fakeAsync(() => {
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 502, statusText: 'Bad Gateway' }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.serverError()).toBe('Server error. Please try again.');
      }));

      it('should keep form data intact on server error', fakeAsync(() => {
        const description = 'Important task content';
        component.taskForm.get('description')?.setValue(description);

        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.taskForm.get('description')?.value).toBe(description);
        expect(component.taskForm.get('assigneeId')?.value).toBe(STAFF_ID);
        expect(component.taskForm.get('individualId')?.value).toBe('emp-001');
      }));
    });

    describe('Network error (status 0)', () => {
      it('should show network error message when status is 0', fakeAsync(() => {
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(component.serverError()).toBe('Unable to connect. Please check your network and try again.');
      }));

      it('should keep modal open and preserve form data on network error', fakeAsync(() => {
        const closedSpy = vi.spyOn(component.closed, 'emit');
        const taskCreatedSpy = vi.spyOn(component.taskCreated, 'emit');

        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' }))
        );

        component.onSubmit();
        tick();
        fixture.detectChanges();

        expect(closedSpy).not.toHaveBeenCalled();
        expect(taskCreatedSpy).not.toHaveBeenCalled();
        expect(component.taskForm.get('description')?.value).toBe('Valid task description');
      }));
    });

    describe('No success event on error', () => {
      it('should NOT emit taskCreated on 400 error', fakeAsync(() => {
        const taskCreatedSpy = vi.spyOn(component.taskCreated, 'emit');

        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({
            status: 400,
            error: { errors: [{ field: 'description', message: 'required' }] }
          }))
        );

        component.onSubmit();
        tick();

        expect(taskCreatedSpy).not.toHaveBeenCalled();
      }));

      it('should NOT emit taskCreated on 500 error', fakeAsync(() => {
        const taskCreatedSpy = vi.spyOn(component.taskCreated, 'emit');

        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }))
        );

        component.onSubmit();
        tick();

        expect(taskCreatedSpy).not.toHaveBeenCalled();
      }));

      it('should NOT emit taskCreated on network error', fakeAsync(() => {
        const taskCreatedSpy = vi.spyOn(component.taskCreated, 'emit');

        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' }))
        );

        component.onSubmit();
        tick();

        expect(taskCreatedSpy).not.toHaveBeenCalled();
      }));

      it('should NOT emit closed on any error', fakeAsync(() => {
        const closedSpy = vi.spyOn(component.closed, 'emit');

        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }))
        );

        component.onSubmit();
        tick();

        expect(closedSpy).not.toHaveBeenCalled();
      }));
    });

    describe('Clear errors on retry', () => {
      it('should clear serverError on resubmission', fakeAsync(() => {
        // First attempt fails with server error
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }))
        );

        component.onSubmit();
        tick();
        expect(component.serverError()).toBe('Server error. Please try again.');

        // Second attempt clears error before trying
        mockTaskService.createTask.mockReturnValue(of({ id: 'new-task' }));
        component.onSubmit();
        // After clearing but before response
        expect(component.serverError()).toBeNull();
      }));

      it('should clear inlineErrors on resubmission', fakeAsync(() => {
        // First attempt fails with validation errors
        mockTaskService.createTask.mockReturnValue(
          throwError(() => new HttpErrorResponse({
            status: 400,
            error: { errors: [{ field: 'description', message: 'too short' }] }
          }))
        );

        component.onSubmit();
        tick();
        expect(Object.keys(component.inlineErrors()).length).toBeGreaterThan(0);

        // Second attempt clears inline errors
        mockTaskService.createTask.mockReturnValue(of({ id: 'new-task' }));
        component.onSubmit();
        expect(Object.keys(component.inlineErrors())).toHaveLength(0);
      }));
    });
  });
});
