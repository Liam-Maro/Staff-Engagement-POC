import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';

import { TaskService } from './task.service';
import {
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskQueryParams,
  TaskQueryResult,
  TaskResponse,
  InteractionSummary
} from '../models/task.model';

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

  const mockTaskResponse: TaskResponse = {
    id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    individualId: 'ind-001',
    interactionId: null,
    creatorId: 'staff-001',
    assigneeId: 'staff-002',
    description: 'Follow up on training progress',
    status: 'TODO',
    dueDate: '2025-12-01',
    createdAt: '2025-01-15T10:30:00'
  };

  describe('createTask', () => {
    it('should send POST to /api/tasks with request body', () => {
      const request: CreateTaskRequest = {
        individualId: 'ind-001',
        assigneeId: 'staff-002',
        description: 'Follow up on training progress',
        dueDate: '2025-12-01'
      };

      let result: TaskResponse | undefined;
      service.createTask(request).subscribe(res => result = res);

      const req = httpTesting.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockTaskResponse);

      expect(result).toEqual(mockTaskResponse);
    });

    it('should include optional interactionId when provided', () => {
      const request: CreateTaskRequest = {
        individualId: 'ind-001',
        interactionId: 'int-001',
        assigneeId: 'staff-002',
        description: 'Linked to interaction',
        dueDate: '2025-12-01'
      };

      service.createTask(request).subscribe();

      const req = httpTesting.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.interactionId).toBe('int-001');
      req.flush(mockTaskResponse);
    });
  });

  describe('updateTask', () => {
    it('should send PUT to /api/tasks/{id} with request body', () => {
      const taskId = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
      const request: UpdateTaskRequest = {
        individualId: 'ind-001',
        assigneeId: 'staff-003',
        description: 'Updated description',
        dueDate: '2025-12-15'
      };

      const updatedResponse: TaskResponse = {
        ...mockTaskResponse,
        assigneeId: 'staff-003',
        description: 'Updated description',
        dueDate: '2025-12-15'
      };

      let result: TaskResponse | undefined;
      service.updateTask(taskId, request).subscribe(res => result = res);

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(updatedResponse);

      expect(result).toEqual(updatedResponse);
    });
  });

  describe('deleteTask', () => {
    it('should send DELETE to /api/tasks/{id}', () => {
      const taskId = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';

      let completed = false;
      service.deleteTask(taskId).subscribe(() => completed = true);

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.body).toBeNull();
      req.flush(null);

      expect(completed).toBe(true);
    });
  });

  describe('getTasks', () => {
    const mockQueryResult: TaskQueryResult = {
      tasks: [mockTaskResponse],
      totalCount: 1,
      currentPage: 0,
      pageSize: 50
    };

    it('should send GET to /api/tasks with query params', () => {
      const params: TaskQueryParams = {
        assigneeId: 'staff-002',
        status: 'TODO',
        sortBy: 'createdDate',
        sortOrder: 'desc',
        page: 0,
        size: 50
      };

      let result: TaskQueryResult | undefined;
      service.getTasks(params).subscribe(res => result = res);

      const req = httpTesting.expectOne(r => r.url === baseUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('assigneeId')).toBe('staff-002');
      expect(req.request.params.get('status')).toBe('TODO');
      expect(req.request.params.get('sortBy')).toBe('createdDate');
      expect(req.request.params.get('sortOrder')).toBe('desc');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('50');
      req.flush(mockQueryResult);

      expect(result).toEqual(mockQueryResult);
    });

    it('should omit undefined values from query params', () => {
      const params: TaskQueryParams = {
        assigneeId: 'staff-002',
        status: undefined,
        dueDateFrom: undefined,
        sortBy: 'createdDate'
      };

      service.getTasks(params).subscribe();

      const req = httpTesting.expectOne(r => r.url === baseUrl);
      expect(req.request.params.get('assigneeId')).toBe('staff-002');
      expect(req.request.params.get('sortBy')).toBe('createdDate');
      expect(req.request.params.has('status')).toBe(false);
      expect(req.request.params.has('dueDateFrom')).toBe(false);
      expect(req.request.params.has('dueDateTo')).toBe(false);
      expect(req.request.params.has('createdFrom')).toBe(false);
      expect(req.request.params.has('createdTo')).toBe(false);
      req.flush(mockQueryResult);
    });

    it('should omit null values from query params', () => {
      const params: TaskQueryParams = {
        assigneeId: 'staff-002'
      };
      // Manually set a param to null to test null handling
      (params as any).creatorId = null;

      service.getTasks(params).subscribe();

      const req = httpTesting.expectOne(r => r.url === baseUrl);
      expect(req.request.params.has('creatorId')).toBe(false);
      expect(req.request.params.get('assigneeId')).toBe('staff-002');
      req.flush(mockQueryResult);
    });

    it('should include boolean excludeSelfAssigned as string when true', () => {
      const params: TaskQueryParams = {
        creatorId: 'staff-001',
        excludeSelfAssigned: true
      };

      service.getTasks(params).subscribe();

      const req = httpTesting.expectOne(r => r.url === baseUrl);
      expect(req.request.params.get('creatorId')).toBe('staff-001');
      expect(req.request.params.get('excludeSelfAssigned')).toBe('true');
      req.flush(mockQueryResult);
    });

    it('should include date range params when provided', () => {
      const params: TaskQueryParams = {
        assigneeId: 'staff-002',
        dueDateFrom: '2025-01-01',
        dueDateTo: '2025-12-31',
        createdFrom: '2025-01-01',
        createdTo: '2025-06-30'
      };

      service.getTasks(params).subscribe();

      const req = httpTesting.expectOne(r => r.url === baseUrl);
      expect(req.request.params.get('dueDateFrom')).toBe('2025-01-01');
      expect(req.request.params.get('dueDateTo')).toBe('2025-12-31');
      expect(req.request.params.get('createdFrom')).toBe('2025-01-01');
      expect(req.request.params.get('createdTo')).toBe('2025-06-30');
      req.flush(mockQueryResult);
    });
  });

  describe('getTaskById', () => {
    it('should send GET to /api/tasks/{id}', () => {
      const taskId = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';

      let result: TaskResponse | undefined;
      service.getTaskById(taskId).subscribe(res => result = res);

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.body).toBeNull();
      req.flush(mockTaskResponse);

      expect(result).toEqual(mockTaskResponse);
    });
  });

  describe('updateTaskStatus', () => {
    it('should send PATCH to /api/tasks/{id}/status with status body', () => {
      const taskId = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
      const status = 'IN_PROGRESS';

      const updatedResponse: TaskResponse = {
        ...mockTaskResponse,
        status: 'IN_PROGRESS'
      };

      let result: TaskResponse | undefined;
      service.updateTaskStatus(taskId, status).subscribe(res => result = res);

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}/status`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'IN_PROGRESS' });
      req.flush(updatedResponse);

      expect(result).toEqual(updatedResponse);
    });

    it('should send PATCH with DONE status', () => {
      const taskId = 'task-xyz';

      service.updateTaskStatus(taskId, 'DONE').subscribe();

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}/status`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'DONE' });
      req.flush(mockTaskResponse);
    });
  });

  describe('getInteractionsForIndividual', () => {
    it('should send GET to /api/tasks/interactions with individualId query param', () => {
      const individualId = 'ind-001';
      const mockInteractions: InteractionSummary[] = [
        {
          id: 'int-001',
          employeeId: 'ind-001',
          staffId: 'staff-001',
          type: 'CHECK_IN',
          notes: 'Discussed training',
          occurredAt: '2025-01-10T09:00:00',
          createdAt: '2025-01-10T09:30:00'
        }
      ];

      let result: InteractionSummary[] | undefined;
      service.getInteractionsForIndividual(individualId).subscribe(res => result = res);

      const req = httpTesting.expectOne(r =>
        r.url === `${baseUrl}/interactions` &&
        r.params.get('individualId') === individualId
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('individualId')).toBe(individualId);
      req.flush(mockInteractions);

      expect(result).toEqual(mockInteractions);
    });
  });

  describe('error handling', () => {
    it('should propagate HTTP 400 error from createTask', () => {
      const request: CreateTaskRequest = {
        individualId: 'ind-001',
        assigneeId: 'staff-002',
        description: '',
        dueDate: '2025-12-01'
      };

      let errorResponse: HttpErrorResponse | undefined;
      service.createTask(request).subscribe({
        next: () => fail('should have errored'),
        error: (err: HttpErrorResponse) => errorResponse = err
      });

      const req = httpTesting.expectOne(baseUrl);
      req.flush(
        { message: 'Description must not be blank' },
        { status: 400, statusText: 'Bad Request' }
      );

      expect(errorResponse).toBeDefined();
      expect(errorResponse!.status).toBe(400);
    });

    it('should propagate HTTP 404 error from getTaskById', () => {
      const taskId = 'non-existent-id';

      let errorResponse: HttpErrorResponse | undefined;
      service.getTaskById(taskId).subscribe({
        next: () => fail('should have errored'),
        error: (err: HttpErrorResponse) => errorResponse = err
      });

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}`);
      req.flush(
        { message: 'Task not found' },
        { status: 404, statusText: 'Not Found' }
      );

      expect(errorResponse).toBeDefined();
      expect(errorResponse!.status).toBe(404);
    });

    it('should propagate HTTP 403 error from deleteTask', () => {
      const taskId = 'task-123';

      let errorResponse: HttpErrorResponse | undefined;
      service.deleteTask(taskId).subscribe({
        next: () => fail('should have errored'),
        error: (err: HttpErrorResponse) => errorResponse = err
      });

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}`);
      req.flush(
        { message: 'Not authorized to delete this task' },
        { status: 403, statusText: 'Forbidden' }
      );

      expect(errorResponse).toBeDefined();
      expect(errorResponse!.status).toBe(403);
    });

    it('should propagate HTTP 500 server error from updateTask', () => {
      const taskId = 'task-123';
      const request: UpdateTaskRequest = {
        individualId: 'ind-001',
        assigneeId: 'staff-002',
        description: 'Updated',
      };

      let errorResponse: HttpErrorResponse | undefined;
      service.updateTask(taskId, request).subscribe({
        next: () => fail('should have errored'),
        error: (err: HttpErrorResponse) => errorResponse = err
      });

      const req = httpTesting.expectOne(`${baseUrl}/${taskId}`);
      req.flush(
        { message: 'Internal server error' },
        { status: 500, statusText: 'Internal Server Error' }
      );

      expect(errorResponse).toBeDefined();
      expect(errorResponse!.status).toBe(500);
    });
  });
});
