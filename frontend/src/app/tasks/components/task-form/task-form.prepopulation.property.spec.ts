/**
 * Property-Based Tests — TaskFormComponent Pre-Population
 *
 * Tests correctness properties for interaction context pre-population logic
 * using fast-check to generate random valid/invalid inputs.
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 5.2, 5.3, 5.4, 5.5**
 */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { of } from 'rxjs';
import * as fc from 'fast-check';

import { TaskFormComponent } from './task-form.component';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { InteractionSummary, InteractionContext } from '../../models/task.model';

const STAFF_ID = 'staff-abc-123';
const STAFF_EMAIL = 'staff@example.com';

const mockStaffList = [
  { id: STAFF_ID, email: STAFF_EMAIL, role: 'STAFF', active: true, createdAt: '2025-01-01T00:00:00' }
];

const mockEmployees = [
  { id: 'emp-001', firstName: 'John', lastName: 'Doe', email: 'john@example.com', department: 'Engineering', jobTitle: 'Dev', hireDate: '2020-01-01', active: true }
];

/**
 * Generator for non-UUID strings that definitely do NOT match UUID format.
 * Produces strings that are either too short, contain invalid chars, or wrong structure.
 */
const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const nonUuidArbitrary = fc.oneof(
  // Empty string
  fc.constant(''),
  // Short random strings (1-10 chars)
  fc.string({ minLength: 1, maxLength: 10 }),
  // Strings with spaces
  fc.string({ minLength: 5, maxLength: 20 }).map(s => `  ${s}  `),
  // Strings that look almost like UUIDs but have invalid characters
  fc.uuid().map(uuid => uuid.replace(/[0-9a-f]/i, 'Z')),
  // Plain words
  fc.constantFrom('hello', 'not-a-uuid', '12345', 'abc-def', 'null', 'undefined')
).filter(s => !UUID_REGEX.test(s));

describe('Feature: interaction-follow-up-task — TaskFormComponent Pre-Population Property Tests', () => {
  let mockTaskService: any;
  let mockAuthService: any;
  let mockStaffService: any;
  let mockEmployeeService: any;

  beforeEach(async () => {
    mockTaskService = {
      createTask: vi.fn().mockReturnValue(of({ id: 'new-task-id' })),
      updateTask: vi.fn().mockReturnValue(of({})),
      getInteractionsForIndividual: vi.fn().mockImplementation((employeeId: string) => {
        // Return interactions that include the given employeeId context
        return of([]);
      })
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
      imports: [TaskFormComponent],
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

  /**
   * Property 3: Full pre-population from valid interaction context
   *
   * For any valid InteractionContext (both interactionId and employeeId are valid UUIDs
   * matching existing records), the Task_Form SHALL have individualId set to employeeId,
   * linkInteraction toggle set to true, and after interactions load, interactionId set
   * to the provided value.
   *
   * **Validates: Requirements 3.1, 3.2, 3.3, 5.2**
   */
  describe('Property 3: Full pre-population from valid interaction context', () => {
    it('should pre-populate individualId, enable toggle, and select interaction for any valid UUID pair', () => {
      fc.assert(
        fc.property(
          fc.uuid(),
          fc.uuid(),
          (interactionId, employeeId) => {
            // Setup: mock returns interactions containing the generated interactionId
            const mockInteractions: InteractionSummary[] = [
              {
                id: interactionId,
                employeeId: employeeId,
                staffId: STAFF_ID,
                type: 'CHECK_IN',
                notes: 'Test interaction',
                occurredAt: '2025-01-10T14:00:00',
                createdAt: '2025-01-10T14:00:00'
              }
            ];
            mockTaskService.getInteractionsForIndividual.mockReturnValue(of(mockInteractions));

            const context: InteractionContext = {
              interactionId,
              employeeId,
              interactionType: 'CHECK_IN',
              interactionDate: '2025-01-10'
            };

            const fixture = TestBed.createComponent(TaskFormComponent);
            const component = fixture.componentInstance;
            component.interactionContext = context;
            fixture.detectChanges();

            // Assert: individualId = employeeId
            expect(component.taskForm.get('individualId')?.value).toBe(employeeId);
            // Assert: linkInteraction toggle = true
            expect(component.taskForm.get('linkInteraction')?.value).toBe(true);
            // Assert: interactionId set to provided value (after interactions load synchronously via mock)
            expect(component.taskForm.get('interactionId')?.value).toBe(interactionId);
            // Assert: interactions were fetched for the correct employee
            expect(mockTaskService.getInteractionsForIndividual).toHaveBeenCalledWith(employeeId);

            fixture.destroy();
          }
        ),
        { numRuns: 100 }
      );
    });
  });

  /**
   * Property 7: Partial context (employeeId only) pre-populates individual only
   *
   * For any valid UUID employeeId provided without an interactionId (or with invalid
   * interactionId), after init the form should have individualId=employeeId,
   * linkInteraction=false, no interaction selected.
   *
   * **Validates: Requirements 5.3**
   */
  describe('Property 7: Partial context (employeeId only) pre-populates individual only', () => {
    it('should set individualId and leave toggle false when only employeeId is valid UUID', () => {
      fc.assert(
        fc.property(
          fc.uuid(),
          nonUuidArbitrary,
          (employeeId, invalidInteractionId) => {
            const context: InteractionContext = {
              interactionId: invalidInteractionId,
              employeeId: employeeId
            };

            const fixture = TestBed.createComponent(TaskFormComponent);
            const component = fixture.componentInstance;
            component.interactionContext = context;
            fixture.detectChanges();

            // Assert: individualId = employeeId
            expect(component.taskForm.get('individualId')?.value).toBe(employeeId);
            // Assert: linkInteraction toggle = false
            expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
            // Assert: no interaction pre-selected
            expect(component.taskForm.get('interactionId')?.value).toBe('');
            // Assert: interactions NOT fetched (no preselection attempted)
            expect(mockTaskService.getInteractionsForIndividual).not.toHaveBeenCalled();

            fixture.destroy();
          }
        ),
        { numRuns: 100 }
      );
    });
  });

  /**
   * Property 8: InteractionId without employeeId is ignored
   *
   * For any valid UUID interactionId provided without a valid employeeId, the Task_Form
   * SHALL behave identically to having no interaction context — individualId empty,
   * linkInteraction false, no interaction selected.
   *
   * **Validates: Requirements 5.4**
   */
  describe('Property 8: InteractionId without employeeId is ignored', () => {
    it('should ignore context entirely when interactionId is valid but employeeId is invalid', () => {
      fc.assert(
        fc.property(
          fc.uuid(),
          nonUuidArbitrary,
          (interactionId, invalidEmployeeId) => {
            const context: InteractionContext = {
              interactionId: interactionId,
              employeeId: invalidEmployeeId
            };

            const fixture = TestBed.createComponent(TaskFormComponent);
            const component = fixture.componentInstance;
            component.interactionContext = context;
            fixture.detectChanges();

            // Assert: form behaves as if no context provided
            // individualId should be empty (default state)
            expect(component.taskForm.get('individualId')?.value).toBe('');
            // linkInteraction should be false (default state)
            expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
            // No interaction selected
            expect(component.taskForm.get('interactionId')?.value).toBe('');
            // Interactions NOT fetched
            expect(mockTaskService.getInteractionsForIndividual).not.toHaveBeenCalled();

            fixture.destroy();
          }
        ),
        { numRuns: 100 }
      );
    });
  });

  /**
   * Property 9: Malformed UUID parameters are silently ignored
   *
   * For any string that does not match the UUID format, when provided as both
   * interactionId and employeeId, the Task_Form SHALL ignore them and show
   * default form state.
   *
   * **Validates: Requirements 5.5**
   */
  describe('Property 9: Malformed UUID parameters are silently ignored', () => {
    it('should show default state when both parameters are non-UUID strings', () => {
      fc.assert(
        fc.property(
          nonUuidArbitrary,
          nonUuidArbitrary,
          (malformedInteractionId, malformedEmployeeId) => {
            const context: InteractionContext = {
              interactionId: malformedInteractionId,
              employeeId: malformedEmployeeId
            };

            const fixture = TestBed.createComponent(TaskFormComponent);
            const component = fixture.componentInstance;
            component.interactionContext = context;
            fixture.detectChanges();

            // Assert: default form state — all fields at initial values
            expect(component.taskForm.get('individualId')?.value).toBe('');
            expect(component.taskForm.get('linkInteraction')?.value).toBe(false);
            expect(component.taskForm.get('interactionId')?.value).toBe('');
            // No interactions fetched
            expect(mockTaskService.getInteractionsForIndividual).not.toHaveBeenCalled();

            fixture.destroy();
          }
        ),
        { numRuns: 100 }
      );
    });
  });
});
