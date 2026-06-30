import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi, describe, it, expect, beforeEach } from 'vitest';

import { InteractionListComponent } from './interaction-list.component';
import { InteractionService } from '../../services/interaction.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { InteractionResponse, PageResponse } from '../../models/interaction.model';
import { Employee } from '../../../employees/models/employee.models';

describe('InteractionListComponent', () => {
  let component: InteractionListComponent;
  let fixture: ComponentFixture<InteractionListComponent>;
  let mockInteractionService: { getAll: ReturnType<typeof vi.fn> };
  let mockEmployeeService: { findAll: ReturnType<typeof vi.fn> };

  const mockEmployees: Employee[] = [
    { id: 'emp-1', firstName: 'John', lastName: 'Doe', email: 'john@test.com', department: 'Engineering', jobTitle: 'Developer', hireDate: '2020-01-01', active: true },
    { id: 'emp-2', firstName: 'Jane', lastName: 'Smith', email: 'jane@test.com', department: 'HR', jobTitle: 'Manager', hireDate: '2019-05-01', active: true }
  ];

  const mockInteractions: InteractionResponse[] = [
    { id: 'int-1', employeeId: 'emp-1', staffId: 'staff-1', type: 'CHECK_IN', notes: 'Short notes', occurredAt: '2025-01-15T10:00:00', createdAt: '2025-01-15T10:00:00', updatedAt: '2025-01-15T10:00:00' },
    { id: 'int-2', employeeId: 'emp-2', staffId: 'staff-1', type: 'MENTORING', notes: null, occurredAt: '2025-01-14T09:00:00', createdAt: '2025-01-14T09:00:00', updatedAt: '2025-01-14T09:00:00' }
  ];

  const mockPageResponse: PageResponse<InteractionResponse> = {
    content: mockInteractions,
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 20
  };

  beforeEach(async () => {
    mockInteractionService = { getAll: vi.fn().mockReturnValue(of(mockPageResponse)) };
    mockEmployeeService = { findAll: vi.fn().mockReturnValue(of(mockEmployees)) };

    await TestBed.configureTestingModule({
      imports: [InteractionListComponent],
      providers: [
        provideRouter([]),
        { provide: InteractionService, useValue: mockInteractionService },
        { provide: EmployeeService, useValue: mockEmployeeService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InteractionListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('truncateNotes', () => {
    it('should return first 100 chars with "..." when notes exceed 100 characters', () => {
      const longNotes = 'a'.repeat(150);
      const result = component.truncateNotes(longNotes);
      expect(result).toBe('a'.repeat(100) + '...');
      expect(result.length).toBe(103);
    });

    it('should return notes as-is when length is exactly 100 characters', () => {
      const exactNotes = 'b'.repeat(100);
      const result = component.truncateNotes(exactNotes);
      expect(result).toBe(exactNotes);
    });

    it('should return notes as-is when length is less than 100 characters', () => {
      const shortNotes = 'Short note here';
      const result = component.truncateNotes(shortNotes);
      expect(result).toBe(shortNotes);
    });

    it('should return empty string when notes is null', () => {
      const result = component.truncateNotes(null);
      expect(result).toBe('');
    });
  });

  describe('formatType', () => {
    it('should convert CHECK_IN to "CHECK IN"', () => {
      expect(component.formatType('CHECK_IN')).toBe('CHECK IN');
    });

    it('should convert PERFORMANCE_REVIEW to "PERFORMANCE REVIEW"', () => {
      expect(component.formatType('PERFORMANCE_REVIEW')).toBe('PERFORMANCE REVIEW');
    });

    it('should convert CATCH_UP to "CATCH UP"', () => {
      expect(component.formatType('CATCH_UP')).toBe('CATCH UP');
    });

    it('should leave MENTORING unchanged (no underscores)', () => {
      expect(component.formatType('MENTORING')).toBe('MENTORING');
    });

    it('should leave INFORMAL unchanged (no underscores)', () => {
      expect(component.formatType('INFORMAL')).toBe('INFORMAL');
    });
  });

  describe('initial data load', () => {
    it('should call interactionService.getAll with default params on init', () => {
      fixture.detectChanges();

      expect(mockInteractionService.getAll).toHaveBeenCalledWith({
        page: 0,
        size: 20
      });
    });

    it('should call employeeService.findAll on init', () => {
      fixture.detectChanges();
      expect(mockEmployeeService.findAll).toHaveBeenCalled();
    });

    it('should set interactions from response', () => {
      fixture.detectChanges();
      expect(component.interactions()).toEqual(mockInteractions);
    });

    it('should set pagination metadata from response', () => {
      fixture.detectChanges();
      expect(component.totalPages()).toBe(1);
      expect(component.totalElements()).toBe(2);
      expect(component.currentPage()).toBe(0);
    });

    it('should set isLoading to false after data loads', () => {
      fixture.detectChanges();
      expect(component.isLoading()).toBe(false);
    });

    it('should populate employees signal', () => {
      fixture.detectChanges();
      expect(component.employees()).toEqual(mockEmployees);
    });
  });

  describe('loading state', () => {
    it('should display loading indicator while data is loading', () => {
      // Before detectChanges, isLoading is true by default
      expect(component.isLoading()).toBe(true);
    });
  });

  describe('error state', () => {
    it('should set error message when service call fails', () => {
      mockInteractionService.getAll.mockReturnValue(throwError(() => new Error('Server error')));
      fixture.detectChanges();

      expect(component.errorMessage()).toBe('Failed to load interactions. Please try again.');
      expect(component.isLoading()).toBe(false);
    });

    it('should render error message and retry button on error', () => {
      mockInteractionService.getAll.mockReturnValue(throwError(() => new Error('Server error')));
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.error-message')?.textContent).toContain('Failed to load interactions');
      expect(compiled.querySelector('.error-container button')).toBeTruthy();
    });

    it('should retry loading when retry button is clicked', () => {
      mockInteractionService.getAll.mockReturnValue(throwError(() => new Error('Server error')));
      fixture.detectChanges();

      mockInteractionService.getAll.mockReturnValue(of(mockPageResponse));
      component.retry();
      fixture.detectChanges();

      expect(component.errorMessage()).toBeNull();
      expect(component.interactions()).toEqual(mockInteractions);
    });
  });

  describe('empty state', () => {
    it('should display empty state message when no interactions returned', () => {
      const emptyResponse: PageResponse<InteractionResponse> = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 20
      };
      mockInteractionService.getAll.mockReturnValue(of(emptyResponse));
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.empty-container')).toBeTruthy();
      expect(compiled.querySelector('.empty-container')?.textContent).toContain('No interactions found');
    });
  });

  describe('filter application', () => {
    it('should reset pagination to page 0 and reload when filters are applied', () => {
      fixture.detectChanges();
      mockInteractionService.getAll.mockClear();

      // Simulate being on page 2 then applying a filter
      component.currentPage.set(2);
      component.selectedEmployeeId = 'emp-1';
      component.applyFilters();

      expect(component.currentPage()).toBe(0);
      expect(mockInteractionService.getAll).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        employeeId: 'emp-1'
      });
    });

    it('should include type filter when selected', () => {
      fixture.detectChanges();
      mockInteractionService.getAll.mockClear();

      component.selectedType = 'MENTORING';
      component.applyFilters();

      expect(mockInteractionService.getAll).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        type: 'MENTORING'
      });
    });

    it('should include both employeeId and type when both are selected', () => {
      fixture.detectChanges();
      mockInteractionService.getAll.mockClear();

      component.selectedEmployeeId = 'emp-2';
      component.selectedType = 'CHECK_IN';
      component.applyFilters();

      expect(mockInteractionService.getAll).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        employeeId: 'emp-2',
        type: 'CHECK_IN'
      });
    });
  });

  describe('pagination navigation', () => {
    const multiPageResponse: PageResponse<InteractionResponse> = {
      content: mockInteractions,
      totalElements: 60,
      totalPages: 3,
      number: 0,
      size: 20
    };

    beforeEach(() => {
      mockInteractionService.getAll.mockReturnValue(of(multiPageResponse));
      fixture.detectChanges();
      mockInteractionService.getAll.mockClear();
    });

    it('should call service with next page on nextPage()', () => {
      mockInteractionService.getAll.mockReturnValue(of({ ...multiPageResponse, number: 1 }));
      component.nextPage();

      expect(mockInteractionService.getAll).toHaveBeenCalledWith({
        page: 1,
        size: 20
      });
    });

    it('should call service with previous page on previousPage()', () => {
      // Move to page 1 first
      component.currentPage.set(1);
      mockInteractionService.getAll.mockReturnValue(of({ ...multiPageResponse, number: 0 }));
      component.previousPage();

      expect(mockInteractionService.getAll).toHaveBeenCalledWith({
        page: 0,
        size: 20
      });
    });

    it('should not go below page 0 on previousPage()', () => {
      component.currentPage.set(0);
      component.previousPage();

      expect(mockInteractionService.getAll).not.toHaveBeenCalled();
    });

    it('should not go beyond last page on nextPage()', () => {
      component.currentPage.set(2); // totalPages is 3, so last page is index 2
      component.nextPage();

      expect(mockInteractionService.getAll).not.toHaveBeenCalled();
    });

    it('should include filters when navigating pages', () => {
      component.selectedEmployeeId = 'emp-1';
      component.selectedType = 'CHECK_IN';
      mockInteractionService.getAll.mockReturnValue(of({ ...multiPageResponse, number: 1 }));
      component.nextPage();

      expect(mockInteractionService.getAll).toHaveBeenCalledWith({
        page: 1,
        size: 20,
        employeeId: 'emp-1',
        type: 'CHECK_IN'
      });
    });
  });

  describe('notes truncation in rendered view', () => {
    it('should display truncated notes with "..." for notes > 100 chars', () => {
      const longNotes = 'x'.repeat(150);
      const responseWithLongNotes: PageResponse<InteractionResponse> = {
        content: [
          { id: 'int-1', employeeId: 'emp-1', staffId: 'staff-1', type: 'CHECK_IN', notes: longNotes, occurredAt: '2025-01-15T10:00:00', createdAt: '2025-01-15T10:00:00', updatedAt: '2025-01-15T10:00:00' }
        ],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20
      };
      mockInteractionService.getAll.mockReturnValue(of(responseWithLongNotes));
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const notesCell = compiled.querySelector('.notes-preview');
      expect(notesCell?.textContent).toBe('x'.repeat(100) + '...');
    });

    it('should display full notes when length is <= 100 chars', () => {
      const shortNotes = 'Short note content';
      const responseWithShortNotes: PageResponse<InteractionResponse> = {
        content: [
          { id: 'int-1', employeeId: 'emp-1', staffId: 'staff-1', type: 'CHECK_IN', notes: shortNotes, occurredAt: '2025-01-15T10:00:00', createdAt: '2025-01-15T10:00:00', updatedAt: '2025-01-15T10:00:00' }
        ],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20
      };
      mockInteractionService.getAll.mockReturnValue(of(responseWithShortNotes));
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const notesCell = compiled.querySelector('.notes-preview');
      expect(notesCell?.textContent).toBe(shortNotes);
    });
  });

  describe('Create Task action from list view', () => {
    it('should render "Create Task" action for each row', () => {
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const rows = compiled.querySelectorAll('tbody tr');
      expect(rows.length).toBe(2);

      rows.forEach((row) => {
        const actions = row.querySelectorAll('.actions-cell .btn-link');
        const createTaskLink = Array.from(actions).find(a => a.textContent?.trim() === 'Create Task');
        expect(createTaskLink).toBeTruthy();
      });
    });

    it('should open modal with correct context when "Create Task" is clicked', () => {
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const rows = compiled.querySelectorAll('tbody tr');
      // Click "Create Task" on first row (int-1, emp-1, CHECK_IN, 2025-01-15T10:00:00)
      const firstRowActions = rows[0].querySelectorAll('.actions-cell .btn-link');
      const createTaskLink = Array.from(firstRowActions).find(a => a.textContent?.trim() === 'Create Task') as HTMLElement;
      createTaskLink.click();
      fixture.detectChanges();

      expect(component.showTaskFormModal()).toBe(true);
      expect(component.taskFormContext()).toEqual({
        interactionId: 'int-1',
        employeeId: 'emp-1',
        interactionType: 'CHECK_IN',
        interactionDate: '2025-01-15T10:00:00'
      });
    });

    it('should open modal with correct context for second row', () => {
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const rows = compiled.querySelectorAll('tbody tr');
      // Click "Create Task" on second row (int-2, emp-2, MENTORING, 2025-01-14T09:00:00)
      const secondRowActions = rows[1].querySelectorAll('.actions-cell .btn-link');
      const createTaskLink = Array.from(secondRowActions).find(a => a.textContent?.trim() === 'Create Task') as HTMLElement;
      createTaskLink.click();
      fixture.detectChanges();

      expect(component.showTaskFormModal()).toBe(true);
      expect(component.taskFormContext()).toEqual({
        interactionId: 'int-2',
        employeeId: 'emp-2',
        interactionType: 'MENTORING',
        interactionDate: '2025-01-14T09:00:00'
      });
    });

    it('should preserve filters and pagination when modal is closed', () => {
      fixture.detectChanges();

      // Set filters and simulate being on page 1
      component.selectedEmployeeId = 'emp-1';
      component.selectedType = 'CHECK_IN';
      component.currentPage.set(1);

      // Open modal
      component.openCreateTask(mockInteractions[0]);
      expect(component.showTaskFormModal()).toBe(true);

      // Close modal
      component.onModalClose();
      fixture.detectChanges();

      // Verify filters and pagination preserved
      expect(component.showTaskFormModal()).toBe(false);
      expect(component.selectedEmployeeId).toBe('emp-1');
      expect(component.selectedType).toBe('CHECK_IN');
      expect(component.currentPage()).toBe(1);
    });

    it('should show success toast and close modal after task creation', () => {
      fixture.detectChanges();

      // Open modal
      component.openCreateTask(mockInteractions[0]);
      expect(component.showTaskFormModal()).toBe(true);

      // Simulate task created
      component.onTaskCreated();

      expect(component.showTaskFormModal()).toBe(false);
      expect(component.toastMessage()).toBe('Follow-up task created successfully');
      expect(component.toastType()).toBe('success');
    });
  });
});
