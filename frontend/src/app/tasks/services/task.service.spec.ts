import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { TaskService } from './task.service';
import { CreateTaskRequest, TaskResponse } from '../models/task.model';

describe('TaskService', () => {
  let service: TaskService;
  let httpTesting: HttpTestingController;

  const baseUrl = 'http://localhost:8080/api/tasks';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TaskService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(TaskService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  describe('createTask', () => {
    it('should send POST with correct payload and return TaskResponse', () => {
      const request: CreateTaskRequest = {
        employeeId: 'emp-001',
        assigneeId: 'staff-002',
        title: 'Follow up with employee',
        description: 'Check on training progress',
        dueDate: '2025-12-01'
      };

      const mockResponse: TaskResponse = {
        id: 'task-123',
        employeeId: 'emp-001',
        interactionId: null,
        creatorId: 'staff-001',
        assigneeId: 'staff-002',
        title: 'Follow up with employee',
        description: 'Check on training progress',
        status: 'OPEN',
        dueDate: '2025-12-01',
        createdAt: '2025-01-15T10:30:00'
      };

      let result: TaskResponse | undefined;
      service.createTask(request).subscribe(res => result = res);

      const req = httpTesting.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });
  });

  describe('getTasksByAssignee', () => {
    it('should send GET with assigneeId query param', () => {
      const assigneeId = 'staff-002';
      const mockResponse: TaskResponse[] = [
        {
          id: 'task-1',
          employeeId: 'emp-001',
          interactionId: null,
          creatorId: 'staff-001',
          assigneeId: 'staff-002',
          title: 'Task 1',
          description: null,
          status: 'OPEN',
          dueDate: null,
          createdAt: '2025-01-15T10:00:00'
        }
      ];

      let result: TaskResponse[] | undefined;
      service.getTasksByAssignee(assigneeId).subscribe(res => result = res);

      const req = httpTesting.expectOne(r =>
        r.url === baseUrl && r.params.get('assigneeId') === assigneeId
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('assigneeId')).toBe(assigneeId);
      expect(req.request.params.has('sortOrder')).toBe(false);
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });

    it('should include sortOrder query param when provided', () => {
      const assigneeId = 'staff-002';
      const sortOrder = 'desc';

      service.getTasksByAssignee(assigneeId, sortOrder).subscribe();

      const req = httpTesting.expectOne(r =>
        r.url === baseUrl &&
        r.params.get('assigneeId') === assigneeId &&
        r.params.get('sortOrder') === sortOrder
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('assigneeId')).toBe(assigneeId);
      expect(req.request.params.get('sortOrder')).toBe(sortOrder);
      req.flush([]);
    });
  });

  describe('getTasksByCreator', () => {
    it('should send GET with creatorId query param', () => {
      const creatorId = 'staff-001';
      const mockResponse: TaskResponse[] = [
        {
          id: 'task-2',
          employeeId: 'emp-003',
          interactionId: 'int-001',
          creatorId: 'staff-001',
          assigneeId: 'staff-004',
          title: 'Task 2',
          description: 'Some description',
          status: 'IN_PROGRESS',
          dueDate: '2025-06-01',
          createdAt: '2025-01-10T08:00:00'
        }
      ];

      let result: TaskResponse[] | undefined;
      service.getTasksByCreator(creatorId).subscribe(res => result = res);

      const req = httpTesting.expectOne(r =>
        r.url === baseUrl && r.params.get('creatorId') === creatorId
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('creatorId')).toBe(creatorId);
      expect(req.request.params.has('sortOrder')).toBe(false);
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });

    it('should include sortOrder query param when provided', () => {
      const creatorId = 'staff-001';
      const sortOrder = 'asc';

      service.getTasksByCreator(creatorId, sortOrder).subscribe();

      const req = httpTesting.expectOne(r =>
        r.url === baseUrl &&
        r.params.get('creatorId') === creatorId &&
        r.params.get('sortOrder') === sortOrder
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('creatorId')).toBe(creatorId);
      expect(req.request.params.get('sortOrder')).toBe(sortOrder);
      req.flush([]);
    });
  });

  describe('updateTaskStatus', () => {
    it('should send PATCH with status in the body', () => {
      const taskId = 'task-123';
      const status = 'COMPLETED';
      const mockResponse: TaskResponse = {
        id: 'task-123',
        employeeId: 'emp-001',
        interactionId: null,
        creatorId: 'staff-001',
        assigneeId: 'staff-002',
        title: 'Follow up with employee',
        description: null,
        status: 'COMPLETED',
        dueDate: null,
        createdAt: '2025-01-15T10:30:00'
      };

      let result: TaskResponse | undefined;
      service.updateTaskStatus(taskId, status).subscribe(res => result = res);

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}/status`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'COMPLETED' });
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });
  });
});
