import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
import { of } from 'rxjs';
import { MyTasksPageComponent } from './my-tasks-page.component';
import { TaskListComponent } from '../task-list/task-list.component';
import { DelegatedTasksPanelComponent } from '../delegated-tasks-panel/delegated-tasks-panel.component';
import { TaskFormComponent } from '../task-form/task-form.component';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';

describe('MyTasksPageComponent', () => {
  let component: MyTasksPageComponent;
  let fixture: ComponentFixture<MyTasksPageComponent>;

  const mockTaskService = {
    getTasks: vi.fn().mockReturnValue(of({ tasks: [], totalCount: 0, currentPage: 0, pageSize: 50 })),
    createTask: vi.fn().mockReturnValue(of({})),
    updateTask: vi.fn().mockReturnValue(of({})),
    getInteractionsForIndividual: vi.fn().mockReturnValue(of([]))
  };

  const mockAuthService = {
    userEmail: vi.fn().mockReturnValue('staff@example.com'),
    userRole: vi.fn().mockReturnValue('STAFF'),
    isAuthenticated: vi.fn().mockReturnValue(true),
    getAccessToken: () => 'valid-token'
  };

  const mockStaffService = {
    findAll: vi.fn().mockReturnValue(of([
      { id: 'staff-1', email: 'staff@example.com', role: 'STAFF', active: true }
    ]))
  };

  const mockEmployeeService = {
    findAll: vi.fn().mockReturnValue(of([]))
  };

  beforeEach(async () => {
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

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display "My Tasks" heading', () => {
    const heading = fixture.debugElement.query(By.css('.page-header h2'));
    expect(heading.nativeElement.textContent).toBe('My Tasks');
  });

  it('should display "Create Task" button', () => {
    const button = fixture.debugElement.query(By.css('.create-task-btn'));
    expect(button).toBeTruthy();
    expect(button.nativeElement.textContent.trim()).toContain('Create Task');
  });

  it('should render TaskListComponent in main panel', () => {
    const taskList = fixture.debugElement.query(By.directive(TaskListComponent));
    expect(taskList).toBeTruthy();
  });

  it('should render DelegatedTasksPanelComponent in side panel', () => {
    const delegatedPanel = fixture.debugElement.query(By.directive(DelegatedTasksPanelComponent));
    expect(delegatedPanel).toBeTruthy();
  });

  it('should not show TaskFormComponent by default', () => {
    const taskForm = fixture.debugElement.query(By.directive(TaskFormComponent));
    expect(taskForm).toBeFalsy();
  });

  it('should show TaskFormComponent when "Create Task" is clicked', () => {
    const button = fixture.debugElement.query(By.css('.create-task-btn'));
    button.nativeElement.click();
    fixture.detectChanges();

    const taskForm = fixture.debugElement.query(By.directive(TaskFormComponent));
    expect(taskForm).toBeTruthy();
  });

  it('should hide TaskFormComponent when closed event is emitted', () => {
    component.openCreateForm();
    fixture.detectChanges();

    const taskForm = fixture.debugElement.query(By.directive(TaskFormComponent));
    expect(taskForm).toBeTruthy();

    taskForm.componentInstance.closed.emit();
    fixture.detectChanges();

    const taskFormAfter = fixture.debugElement.query(By.directive(TaskFormComponent));
    expect(taskFormAfter).toBeFalsy();
  });

  it('should emit refresh and close form when taskCreated event fires', () => {
    const refreshSpy = vi.spyOn(component.refresh$, 'next');

    component.openCreateForm();
    fixture.detectChanges();

    const taskForm = fixture.debugElement.query(By.directive(TaskFormComponent));
    taskForm.componentInstance.taskCreated.emit();
    fixture.detectChanges();

    expect(refreshSpy).toHaveBeenCalled();
    expect(component.showTaskForm()).toBe(false);
  });

  it('should pass refresh$ subject to TaskListComponent', () => {
    const taskList = fixture.debugElement.query(By.directive(TaskListComponent));
    expect(taskList.componentInstance.refresh$).toBe(component.refresh$);
  });

  it('should pass refresh$ subject to DelegatedTasksPanelComponent', () => {
    const delegatedPanel = fixture.debugElement.query(By.directive(DelegatedTasksPanelComponent));
    expect(delegatedPanel.componentInstance.refresh$).toBe(component.refresh$);
  });

  it('should have a grid layout with main and side panels', () => {
    const pageContent = fixture.debugElement.query(By.css('.page-content'));
    expect(pageContent).toBeTruthy();

    const mainPanel = fixture.debugElement.query(By.css('.main-panel'));
    expect(mainPanel).toBeTruthy();

    const sidePanel = fixture.debugElement.query(By.css('.side-panel'));
    expect(sidePanel).toBeTruthy();
  });
});
