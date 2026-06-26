import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
import { of, throwError, Subject } from 'rxjs';

import { TaskDetailPopupComponent } from './task-detail-popup.component';
import { TaskService } from '../../services/task.service';
import { TaskResponse } from '../../models/task.model';
import { StaffMember } from '../../../staff/models/staff.models';

describe('TaskDetailPopupComponent', () => {
  let component: TaskDetailPopupComponent;
  let fixture: ComponentFixture<TaskDetailPopupComponent>;
  let mockTaskService: any;

  const CREATOR_STAFF_ID = 'staff-creator-001';
  const OTHER_STAFF_ID = 'staff-other-002';
  const ASSIGNEE_STAFF_ID = 'staff-assignee-003';

  const mockStaffMembers: StaffMember[] = [
    { id: CREATOR_STAFF_ID, email: 'creator@example.com', role: 'STAFF', active: true, createdAt: '2025-01-01T00:00:00' },
    { id: OTHER_STAFF_ID, email: 'other@example.com', role: 'STAFF', active: true, createdAt: '2025-01-01T00:00:00' },
    { id: ASSIGNEE_STAFF_ID, email: 'assignee@example.com', role: 'STAFF', active: true, createdAt: '2025-01-01T00:00:00' }
  ];

  const mockTask: TaskResponse = {
    id: 'task-001',
    individualId: 'ind-001',
    interactionId: 'int-001',
    creatorId: CREATOR_STAFF_ID,
    assigneeId: ASSIGNEE_STAFF_ID,
    description: 'Follow up with individual about mentoring plan',
    status: 'TODO',
    dueDate: '2025-12-01',
    createdAt: '2025-01-15T10:30:00'
  };

  beforeEach(async () => {
    mockTaskService = {
      deleteTask: vi.fn().mockReturnValue(of(undefined))
    };

    await TestBed.configureTestingModule({
      imports: [TaskDetailPopupComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: TaskService, useValue: mockTaskService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TaskDetailPopupComponent);
    component = fixture.componentInstance;
    component.task = mockTask;
    component.staffMembers = mockStaffMembers;
  });

  describe('Edit/Delete visibility for creators vs non-creators', () => {
    it('should show Edit and Delete buttons when current user is the creator', () => {
      component.currentStaffId = CREATOR_STAFF_ID;
      fixture.detectChanges();

      const editBtn = fixture.debugElement.query(By.css('.edit-btn'));
      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));

      expect(editBtn).toBeTruthy();
      expect(deleteBtn).toBeTruthy();
    });

    it('should hide Edit and Delete buttons when current user is NOT the creator', () => {
      component.currentStaffId = OTHER_STAFF_ID;
      fixture.detectChanges();

      const editBtn = fixture.debugElement.query(By.css('.edit-btn'));
      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));

      expect(editBtn).toBeFalsy();
      expect(deleteBtn).toBeFalsy();
    });

    it('should hide Edit and Delete buttons when currentStaffId is empty', () => {
      component.currentStaffId = '';
      fixture.detectChanges();

      const editBtn = fixture.debugElement.query(By.css('.edit-btn'));
      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));

      expect(editBtn).toBeFalsy();
      expect(deleteBtn).toBeFalsy();
    });
  });

  describe('Delete confirmation flow and button disabling', () => {
    beforeEach(() => {
      component.currentStaffId = CREATOR_STAFF_ID;
      fixture.detectChanges();
    });

    it('should show confirmation dialog when Delete button is clicked', () => {
      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));
      deleteBtn.nativeElement.click();
      fixture.detectChanges();

      const confirmDialog = fixture.debugElement.query(By.css('.confirm-dialog'));
      expect(confirmDialog).toBeTruthy();

      const confirmBtn = fixture.debugElement.query(By.css('.confirm-delete-btn'));
      const cancelBtn = fixture.debugElement.query(By.css('.cancel-delete-btn'));
      expect(confirmBtn).toBeTruthy();
      expect(cancelBtn).toBeTruthy();
    });

    it('should disable confirm button while delete is in progress', fakeAsync(() => {
      const deleteSubject = new Subject<void>();
      mockTaskService.deleteTask.mockReturnValue(deleteSubject.asObservable());

      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));
      deleteBtn.nativeElement.click();
      fixture.detectChanges();

      const confirmBtn = fixture.debugElement.query(By.css('.confirm-delete-btn'));
      confirmBtn.nativeElement.click();
      fixture.detectChanges();

      // Button should be disabled during delete
      const disabledBtn = fixture.debugElement.query(By.css('.confirm-delete-btn'));
      expect(disabledBtn.nativeElement.disabled).toBe(true);

      // Complete the delete
      deleteSubject.next(undefined);
      deleteSubject.complete();
      tick();
    }));

    it('should close confirmation dialog when Cancel is clicked', () => {
      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));
      deleteBtn.nativeElement.click();
      fixture.detectChanges();

      const cancelBtn = fixture.debugElement.query(By.css('.cancel-delete-btn'));
      cancelBtn.nativeElement.click();
      fixture.detectChanges();

      const confirmDialog = fixture.debugElement.query(By.css('.confirm-dialog'));
      expect(confirmDialog).toBeFalsy();
    });

    it('should emit taskDeleted and close confirmation on successful delete (204)', () => {
      const taskDeletedSpy = vi.spyOn(component.taskDeleted, 'emit');

      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));
      deleteBtn.nativeElement.click();
      fixture.detectChanges();

      const confirmBtn = fixture.debugElement.query(By.css('.confirm-delete-btn'));
      confirmBtn.nativeElement.click();
      fixture.detectChanges();

      expect(mockTaskService.deleteTask).toHaveBeenCalledWith('task-001');
      expect(taskDeletedSpy).toHaveBeenCalled();
    });

    it('should show permission error and re-enable dialog on 403', () => {
      mockTaskService.deleteTask.mockReturnValue(
        throwError(() => ({ status: 403 }))
      );

      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));
      deleteBtn.nativeElement.click();
      fixture.detectChanges();

      const confirmBtn = fixture.debugElement.query(By.css('.confirm-delete-btn'));
      confirmBtn.nativeElement.click();
      fixture.detectChanges();

      // Error should be shown
      const errorEl = fixture.debugElement.query(By.css('.delete-error'));
      expect(errorEl).toBeTruthy();
      expect(errorEl.nativeElement.textContent).toContain('permission');

      // Confirmation dialog should be closed (re-enabled state)
      const confirmDialog = fixture.debugElement.query(By.css('.confirm-dialog'));
      expect(confirmDialog).toBeFalsy();
    });

    it('should close popup and emit taskDeleted on 404', () => {
      mockTaskService.deleteTask.mockReturnValue(
        throwError(() => ({ status: 404 }))
      );
      const taskDeletedSpy = vi.spyOn(component.taskDeleted, 'emit');

      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));
      deleteBtn.nativeElement.click();
      fixture.detectChanges();

      const confirmBtn = fixture.debugElement.query(By.css('.confirm-delete-btn'));
      confirmBtn.nativeElement.click();
      fixture.detectChanges();

      expect(taskDeletedSpy).toHaveBeenCalled();
    });

    it('should show error and keep confirmation dialog open for retry on server error', () => {
      mockTaskService.deleteTask.mockReturnValue(
        throwError(() => ({ status: 500 }))
      );

      const deleteBtn = fixture.debugElement.query(By.css('.delete-btn'));
      deleteBtn.nativeElement.click();
      fixture.detectChanges();

      const confirmBtn = fixture.debugElement.query(By.css('.confirm-delete-btn'));
      confirmBtn.nativeElement.click();
      fixture.detectChanges();

      // Error message displayed
      const errorEl = fixture.debugElement.query(By.css('.delete-error'));
      expect(errorEl).toBeTruthy();
      expect(errorEl.nativeElement.textContent).toContain('try again');

      // Confirmation dialog still showing for retry
      const confirmDialog = fixture.debugElement.query(By.css('.confirm-dialog'));
      expect(confirmDialog).toBeTruthy();

      // Confirm button should be re-enabled for retry
      const retryBtn = fixture.debugElement.query(By.css('.confirm-delete-btn'));
      expect(retryBtn.nativeElement.disabled).toBe(false);
    });
  });

  describe('Task detail display', () => {
    beforeEach(() => {
      component.currentStaffId = CREATOR_STAFF_ID;
      fixture.detectChanges();
    });

    it('should display task description', () => {
      const description = fixture.debugElement.query(By.css('.description-value'));
      expect(description.nativeElement.textContent).toContain('Follow up with individual about mentoring plan');
    });

    it('should display due date when set', () => {
      const dueDate = fixture.debugElement.query(By.css('.due-date-value'));
      expect(dueDate.nativeElement.textContent).toContain('2025-12-01');
    });

    it('should display "No due date" when dueDate is null', () => {
      const noDueDateFixture = TestBed.createComponent(TaskDetailPopupComponent);
      const comp = noDueDateFixture.componentInstance;
      comp.task = { ...mockTask, dueDate: null };
      comp.staffMembers = mockStaffMembers;
      comp.currentStaffId = CREATOR_STAFF_ID;
      noDueDateFixture.detectChanges();

      const dueDate = noDueDateFixture.debugElement.query(By.css('.due-date-value'));
      expect(dueDate.nativeElement.textContent).toContain('No due date');
    });

    it('should display linked interaction as clickable link when interactionId is set', () => {
      const link = fixture.debugElement.query(By.css('.interaction-link'));
      expect(link).toBeTruthy();
    });

    it('should display "No linked interaction" when interactionId is null', () => {
      const noInteractionFixture = TestBed.createComponent(TaskDetailPopupComponent);
      const comp = noInteractionFixture.componentInstance;
      comp.task = { ...mockTask, interactionId: null };
      comp.staffMembers = mockStaffMembers;
      comp.currentStaffId = CREATOR_STAFF_ID;
      noInteractionFixture.detectChanges();

      const noInteraction = noInteractionFixture.debugElement.query(By.css('.no-interaction'));
      expect(noInteraction).toBeTruthy();
      expect(noInteraction.nativeElement.textContent).toContain('No linked interaction');
    });
  });

  describe('Close control', () => {
    it('should emit closed when close button is clicked', () => {
      component.currentStaffId = CREATOR_STAFF_ID;
      fixture.detectChanges();

      const closedSpy = vi.spyOn(component.closed, 'emit');
      const closeBtn = fixture.debugElement.query(By.css('.close-btn'));
      closeBtn.nativeElement.click();

      expect(closedSpy).toHaveBeenCalled();
    });

    it('should emit closed when overlay backdrop is clicked', () => {
      component.currentStaffId = CREATOR_STAFF_ID;
      fixture.detectChanges();

      const closedSpy = vi.spyOn(component.closed, 'emit');
      const overlay = fixture.debugElement.query(By.css('.popup-overlay'));
      overlay.nativeElement.click();

      expect(closedSpy).toHaveBeenCalled();
    });
  });
});
