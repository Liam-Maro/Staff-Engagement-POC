import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InteractionService, InteractionFilterParams } from './interaction.service';
import {
  InteractionResponse,
  CreateInteractionRequest,
  UpdateInteractionRequest,
  CreateFollowUpTaskRequest,
  PageResponse,
} from '../models/interaction.model';
import { environment } from '../../../environments/environment';

describe('InteractionService', () => {
  let service: InteractionService;
  let httpTesting: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/interactions`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        InteractionService,
      ],
    });

    service = TestBed.inject(InteractionService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  const mockInteraction: InteractionResponse = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    employeeId: 'emp-001',
    staffId: 'staff-001',
    type: 'CHECK_IN',
    notes: 'Discussed project progress',
    occurredAt: '2024-06-15T10:00:00',
    createdAt: '2024-06-15T10:05:00',
    updatedAt: '2024-06-15T10:05:00',
  };

  const mockPageResponse: PageResponse<InteractionResponse> = {
    content: [mockInteraction],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 20,
  };

  describe('getAll()', () => {
    it('should send GET request to /api/interactions with no params', () => {
      service.getAll().subscribe((response) => {
        expect(response).toEqual(mockPageResponse);
      });

      const req = httpTesting.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().length).toBe(0);
      req.flush(mockPageResponse);
    });

    it('should send GET with employeeId query param', () => {
      const params: InteractionFilterParams = { employeeId: 'emp-001' };

      service.getAll(params).subscribe((response) => {
        expect(response).toEqual(mockPageResponse);
      });

      const req = httpTesting.expectOne(
        (r) => r.url === apiUrl && r.params.get('employeeId') === 'emp-001'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('employeeId')).toBe('emp-001');
      req.flush(mockPageResponse);
    });

    it('should send GET with type query param', () => {
      const params: InteractionFilterParams = { type: 'MENTORING' };

      service.getAll(params).subscribe((response) => {
        expect(response).toEqual(mockPageResponse);
      });

      const req = httpTesting.expectOne(
        (r) => r.url === apiUrl && r.params.get('type') === 'MENTORING'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('type')).toBe('MENTORING');
      req.flush(mockPageResponse);
    });

    it('should send GET with page and size query params', () => {
      const params: InteractionFilterParams = { page: 2, size: 10 };

      service.getAll(params).subscribe((response) => {
        expect(response).toEqual(mockPageResponse);
      });

      const req = httpTesting.expectOne(
        (r) =>
          r.url === apiUrl &&
          r.params.get('page') === '2' &&
          r.params.get('size') === '10'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(mockPageResponse);
    });

    it('should send GET with all filter params combined', () => {
      const params: InteractionFilterParams = {
        employeeId: 'emp-001',
        type: 'CHECK_IN',
        fromDate: '2024-01-01T00:00:00',
        toDate: '2024-12-31T23:59:59',
        page: 0,
        size: 20,
      };

      service.getAll(params).subscribe((response) => {
        expect(response).toEqual(mockPageResponse);
      });

      const req = httpTesting.expectOne((r) => r.url === apiUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('employeeId')).toBe('emp-001');
      expect(req.request.params.get('type')).toBe('CHECK_IN');
      expect(req.request.params.get('fromDate')).toBe('2024-01-01T00:00:00');
      expect(req.request.params.get('toDate')).toBe('2024-12-31T23:59:59');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockPageResponse);
    });

    it('should not set params for undefined filter values', () => {
      const params: InteractionFilterParams = { employeeId: 'emp-001' };

      service.getAll(params).subscribe();

      const req = httpTesting.expectOne((r) => r.url === apiUrl);
      expect(req.request.params.has('type')).toBe(false);
      expect(req.request.params.has('fromDate')).toBe(false);
      expect(req.request.params.has('toDate')).toBe(false);
      expect(req.request.params.has('page')).toBe(false);
      expect(req.request.params.has('size')).toBe(false);
      req.flush(mockPageResponse);
    });
  });

  describe('getById()', () => {
    it('should send GET request to /api/interactions/{id}', () => {
      const id = '123e4567-e89b-12d3-a456-426614174000';

      service.getById(id).subscribe((response) => {
        expect(response).toEqual(mockInteraction);
      });

      const req = httpTesting.expectOne(`${apiUrl}/${id}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockInteraction);
    });
  });

  describe('create()', () => {
    it('should send POST request to /api/interactions with correct body', () => {
      const request: CreateInteractionRequest = {
        employeeId: 'emp-001',
        staffId: 'staff-001',
        type: 'CHECK_IN',
        notes: 'Initial check-in',
        occurredAt: '2024-06-15T10:00:00',
      };

      service.create(request).subscribe((response) => {
        expect(response).toEqual(mockInteraction);
      });

      const req = httpTesting.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockInteraction);
    });

    it('should send POST request without optional notes', () => {
      const request: CreateInteractionRequest = {
        employeeId: 'emp-002',
        staffId: 'staff-002',
        type: 'MENTORING',
        occurredAt: '2024-06-15T14:00:00',
      };

      service.create(request).subscribe();

      const req = httpTesting.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      expect(req.request.body.notes).toBeUndefined();
      req.flush(mockInteraction);
    });
  });

  describe('update()', () => {
    it('should send PUT request to /api/interactions/{id} with correct body', () => {
      const id = '123e4567-e89b-12d3-a456-426614174000';
      const request: UpdateInteractionRequest = {
        type: 'PERFORMANCE_REVIEW',
        notes: 'Updated notes',
        occurredAt: '2024-06-15T10:00:00',
      };

      service.update(id, request).subscribe((response) => {
        expect(response).toEqual(mockInteraction);
      });

      const req = httpTesting.expectOne(`${apiUrl}/${id}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(mockInteraction);
    });

    it('should send PUT request without optional notes', () => {
      const id = '123e4567-e89b-12d3-a456-426614174000';
      const request: UpdateInteractionRequest = {
        type: 'INFORMAL',
        occurredAt: '2024-06-15T10:00:00',
      };

      service.update(id, request).subscribe();

      const req = httpTesting.expectOne(`${apiUrl}/${id}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(mockInteraction);
    });
  });

  describe('delete()', () => {
    it('should send DELETE request to /api/interactions/{id}', () => {
      const id = '123e4567-e89b-12d3-a456-426614174000';

      service.delete(id).subscribe();

      const req = httpTesting.expectOne(`${apiUrl}/${id}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('createFollowUpTask()', () => {
    it('should send POST request to /api/interactions/{id}/tasks with correct body', () => {
      const id = '123e4567-e89b-12d3-a456-426614174000';
      const request: CreateFollowUpTaskRequest = {
        title: 'Follow up on progress',
        description: 'Check if goals from last meeting were met',
        dueDate: '2024-07-01',
      };
      const mockTaskResponse = { id: 'task-001', title: request.title, status: 'OPEN' };

      service.createFollowUpTask(id, request).subscribe((response) => {
        expect(response).toEqual(mockTaskResponse);
      });

      const req = httpTesting.expectOne(`${apiUrl}/${id}/tasks`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockTaskResponse);
    });

    it('should send POST request without optional description and dueDate', () => {
      const id = '123e4567-e89b-12d3-a456-426614174000';
      const request: CreateFollowUpTaskRequest = {
        title: 'Quick follow-up',
      };

      service.createFollowUpTask(id, request).subscribe();

      const req = httpTesting.expectOne(`${apiUrl}/${id}/tasks`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      expect(req.request.body.description).toBeUndefined();
      expect(req.request.body.dueDate).toBeUndefined();
      req.flush({ id: 'task-002', title: request.title, status: 'OPEN' });
    });
  });
});
