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
import { TaskResponse, InteractionSummary } from '../../models/task.model';

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
      expect(errorEl.nativeElement.textContent).toContain('Unable to save');
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
});
