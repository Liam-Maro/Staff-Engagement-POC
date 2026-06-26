import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { InteractionFormComponent } from './interaction-form.component';
import { InteractionService } from '../../services/interaction.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { InteractionResponse } from '../../models/interaction.model';
import { Employee } from '../../../employees/models/employee.models';
import { Component } from '@angular/core';

describe('InteractionFormComponent', () => {
  let component: InteractionFormComponent;
  let fixture: ComponentFixture<InteractionFormComponent>;
  let interactionService: {
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
  };
  let employeeService: {
    findAll: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigate: ReturnType<typeof vi.fn>;
  };

  const mockEmployees: Employee[] = [
    {
      id: 'emp-1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      department: 'Engineering',
      jobTitle: 'Developer',
      hireDate: '2020-01-01',
      active: true
    },
    {
      id: 'emp-2',
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane@example.com',
      department: 'HR',
      jobTitle: 'Manager',
      hireDate: '2019-06-15',
      active: true
    }
  ];

  const mockInteraction: InteractionResponse = {
    id: 'int-123',
    employeeId: 'emp-1',
    staffId: 'staff-1',
    type: 'CHECK_IN',
    notes: 'Some notes about the interaction',
    occurredAt: '2024-01-15T10:30:00.000Z',
    createdAt: '2024-01-15T10:35:00.000Z',
    updatedAt: '2024-01-15T10:35:00.000Z'
  };

  function createComponent(routeId: string | null = null) {
    interactionService = {
      create: vi.fn(),
      update: vi.fn(),
      getById: vi.fn()
    };
    employeeService = {
      findAll: vi.fn().mockReturnValue(of(mockEmployees))
    };
    router = {
      navigate: vi.fn()
    };

    const activatedRouteStub = {
      snapshot: {
        paramMap: convertToParamMap(routeId ? { id: routeId } : {})
      }
    };

    if (routeId) {
      interactionService.getById.mockReturnValue(of(mockInteraction));
    }

    TestBed.configureTestingModule({
      imports: [InteractionFormComponent, ReactiveFormsModule],
      providers: [
        { provide: InteractionService, useValue: interactionService },
        { provide: EmployeeService, useValue: employeeService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]
    });

    TestBed.overrideComponent(InteractionFormComponent, {
      set: {
        template: '<div></div>',
        styleUrl: undefined as any,
        styleUrls: undefined as any,
        styles: []
      }
    });

    fixture = TestBed.createComponent(InteractionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  describe('Component creation', () => {
    it('should create successfully', () => {
      createComponent();
      expect(component).toBeTruthy();
    });
  });

  describe('Form validation - required fields', () => {
    beforeEach(() => createComponent());

    it('should be invalid when form is empty', () => {
      expect(component.form.valid).toBe(false);
    });

    it('should require employeeId', () => {
      const control = component.form.get('employeeId');
      expect(control?.hasError('required')).toBe(true);

      control?.setValue('emp-1');
      expect(control?.hasError('required')).toBe(false);
    });

    it('should require type', () => {
      const control = component.form.get('type');
      expect(control?.hasError('required')).toBe(true);

      control?.setValue('CHECK_IN');
      expect(control?.hasError('required')).toBe(false);
    });

    it('should require occurredAt', () => {
      const control = component.form.get('occurredAt');
      expect(control?.hasError('required')).toBe(true);

      control?.setValue('2024-01-15T10:30');
      expect(control?.hasError('required')).toBe(false);
    });
  });

  describe('Form validation - constraints', () => {
    beforeEach(() => createComponent());

    it('should enforce notes maxLength of 5000 characters', () => {
      const control = component.form.get('notes');
      const longText = 'a'.repeat(5001);
      control?.setValue(longText);
      expect(control?.hasError('maxlength')).toBe(true);

      control?.setValue('a'.repeat(5000));
      expect(control?.hasError('maxlength')).toBe(false);
    });

    it('should reject future dates on occurredAt', () => {
      const control = component.form.get('occurredAt');
      const futureDate = new Date();
      futureDate.setFullYear(futureDate.getFullYear() + 1);
      control?.setValue(futureDate.toISOString().slice(0, 16));
      expect(control?.hasError('futureDate')).toBe(true);
    });

    it('should accept past dates on occurredAt', () => {
      const control = component.form.get('occurredAt');
      const pastDate = new Date('2024-01-01T10:00');
      control?.setValue(pastDate.toISOString().slice(0, 16));
      expect(control?.hasError('futureDate')).toBe(false);
    });
  });

  describe('Create mode', () => {
    beforeEach(() => createComponent(null));

    it('should be in create mode when no :id route param', () => {
      expect(component.isEditMode()).toBe(false);
      expect(component.interactionId()).toBeNull();
    });

    it('should not call getById in create mode', () => {
      expect(interactionService.getById).not.toHaveBeenCalled();
    });

    it('should have employeeId control enabled', () => {
      expect(component.form.get('employeeId')?.disabled).toBe(false);
    });
  });

  describe('Edit mode', () => {
    beforeEach(() => createComponent('int-123'));

    it('should be in edit mode when route has :id param', () => {
      expect(component.isEditMode()).toBe(true);
      expect(component.interactionId()).toBe('int-123');
    });

    it('should load existing interaction', () => {
      expect(interactionService.getById).toHaveBeenCalledWith('int-123');
    });

    it('should populate form with existing interaction data', () => {
      expect(component.form.get('type')?.value).toBe('CHECK_IN');
      expect(component.form.get('notes')?.value).toBe('Some notes about the interaction');
      expect(component.form.get('employeeId')?.value).toBe('emp-1');
    });

    it('should disable employeeId field in edit mode', () => {
      expect(component.form.get('employeeId')?.disabled).toBe(true);
    });
  });

  describe('Form submission - create mode', () => {
    beforeEach(() => createComponent(null));

    it('should call interactionService.create on submit in create mode', () => {
      interactionService.create.mockReturnValue(of(mockInteraction));

      component.form.patchValue({
        employeeId: 'emp-1',
        type: 'CHECK_IN',
        occurredAt: '2024-01-15T10:30',
        notes: 'Test notes'
      });

      component.onSubmit();

      expect(interactionService.create).toHaveBeenCalledWith(
        expect.objectContaining({
          employeeId: 'emp-1',
          type: 'CHECK_IN',
          notes: 'Test notes'
        })
      );
    });

    it('should not submit when form is invalid', () => {
      interactionService.create.mockReturnValue(of(mockInteraction));
      component.onSubmit();
      expect(interactionService.create).not.toHaveBeenCalled();
    });
  });

  describe('Form submission - edit mode', () => {
    beforeEach(() => createComponent('int-123'));

    it('should call interactionService.update on submit in edit mode', () => {
      interactionService.update.mockReturnValue(of(mockInteraction));

      component.form.patchValue({
        type: 'MENTORING',
        occurredAt: '2024-01-15T10:30',
        notes: 'Updated notes'
      });

      component.onSubmit();

      expect(interactionService.update).toHaveBeenCalledWith(
        'int-123',
        expect.objectContaining({
          type: 'MENTORING',
          notes: 'Updated notes'
        })
      );
    });
  });

  describe('Successful submission navigation', () => {
    it('should navigate to /interactions on successful create', () => {
      createComponent(null);
      interactionService.create.mockReturnValue(of(mockInteraction));

      component.form.patchValue({
        employeeId: 'emp-1',
        type: 'CHECK_IN',
        occurredAt: '2024-01-15T10:30',
        notes: ''
      });

      component.onSubmit();

      expect(router.navigate).toHaveBeenCalledWith(['/interactions']);
    });

    it('should navigate to /interactions on successful update', () => {
      createComponent('int-123');
      interactionService.update.mockReturnValue(of(mockInteraction));

      component.form.patchValue({
        type: 'MENTORING',
        occurredAt: '2024-01-15T10:30',
        notes: 'Updated'
      });

      component.onSubmit();

      expect(router.navigate).toHaveBeenCalledWith(['/interactions']);
    });
  });

  describe('Error handling - HTTP 400', () => {
    beforeEach(() => createComponent(null));

    it('should display error message on HTTP 400 with message', () => {
      const errorResponse = new HttpErrorResponse({
        status: 400,
        error: { message: 'Validation failed: occurredAt cannot be in the future' }
      });
      interactionService.create.mockReturnValue(throwError(() => errorResponse));

      component.form.patchValue({
        employeeId: 'emp-1',
        type: 'CHECK_IN',
        occurredAt: '2024-01-15T10:30',
        notes: ''
      });

      component.onSubmit();

      expect(component.errorMessage()).toBe('Validation failed: occurredAt cannot be in the future');
    });

    it('should display field-level errors from server on HTTP 400', () => {
      const errorResponse = new HttpErrorResponse({
        status: 400,
        error: {
          errors: [
            { field: 'type', message: 'Type is required' },
            { field: 'occurredAt', message: 'Must not be in the future' }
          ]
        }
      });
      interactionService.create.mockReturnValue(throwError(() => errorResponse));

      component.form.patchValue({
        employeeId: 'emp-1',
        type: 'CHECK_IN',
        occurredAt: '2024-01-15T10:30',
        notes: ''
      });

      component.onSubmit();

      const fieldErrors = component.fieldErrors();
      expect(fieldErrors['type']).toBe('Type is required');
      expect(fieldErrors['occurredAt']).toBe('Must not be in the future');
    });

    it('should display default validation message for unstructured 400 error', () => {
      const errorResponse = new HttpErrorResponse({
        status: 400,
        error: {}
      });
      interactionService.create.mockReturnValue(throwError(() => errorResponse));

      component.form.patchValue({
        employeeId: 'emp-1',
        type: 'CHECK_IN',
        occurredAt: '2024-01-15T10:30',
        notes: ''
      });

      component.onSubmit();

      expect(component.errorMessage()).toBe('Validation failed. Please check the form fields.');
    });
  });

  describe('Error handling - HTTP 5xx', () => {
    beforeEach(() => createComponent(null));

    it('should display general error with retry on HTTP 500', () => {
      const errorResponse = new HttpErrorResponse({
        status: 500,
        error: { message: 'Internal Server Error' }
      });
      interactionService.create.mockReturnValue(throwError(() => errorResponse));

      component.form.patchValue({
        employeeId: 'emp-1',
        type: 'CHECK_IN',
        occurredAt: '2024-01-15T10:30',
        notes: ''
      });

      component.onSubmit();

      expect(component.errorMessage()).toBe('A server error occurred. Please try again.');
    });

    it('should resubmit form on retry', () => {
      const errorResponse = new HttpErrorResponse({
        status: 500,
        error: { message: 'Internal Server Error' }
      });
      interactionService.create.mockReturnValue(throwError(() => errorResponse));

      component.form.patchValue({
        employeeId: 'emp-1',
        type: 'CHECK_IN',
        occurredAt: '2024-01-15T10:30',
        notes: ''
      });

      component.onSubmit();

      // Now make the retry succeed
      interactionService.create.mockReturnValue(of(mockInteraction));
      component.onRetry();

      expect(interactionService.create).toHaveBeenCalledTimes(2);
      expect(router.navigate).toHaveBeenCalledWith(['/interactions']);
    });
  });
});
