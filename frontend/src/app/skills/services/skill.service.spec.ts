import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';

import { SkillService } from './skill.service';
import { Skill, CreateSkillRequest, UpdateSkillRequest, SkillSearchResult } from '../models/skill.models';

describe('SkillService', () => {
  let service: SkillService;
  let httpTesting: HttpTestingController;

  const baseUrl = 'http://localhost:8080/api/skills';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SkillService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SkillService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  const mockSkill: Skill = {
    id: 'skill-001',
    employeeId: 'emp-001',
    name: 'Angular',
    yearsExperience: 5,
    projectCount: 12,
    proficiency: 'Advanced',
    createdAt: '2025-01-10T09:00:00'
  };

  const mockSearchResult: SkillSearchResult = {
    employeeFirstName: 'John',
    employeeLastName: 'Doe',
    employeeEmail: 'john.doe@example.com',
    skillName: 'Angular',
    yearsExperience: 5,
    projectCount: 12,
    proficiency: 'Advanced'
  };

  describe('findByEmployee', () => {
    it('should send GET with employeeId query param', () => {
      const employeeId = 'emp-001';
      const mockSkills: Skill[] = [mockSkill];

      let result: Skill[] | undefined;
      service.findByEmployee(employeeId).subscribe(res => result = res);

      const req = httpTesting.expectOne(r =>
        r.url === baseUrl && r.params.get('employeeId') === employeeId
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('employeeId')).toBe(employeeId);
      req.flush(mockSkills);

      expect(result).toEqual(mockSkills);
    });

    it('should return empty array when employee has no skills', () => {
      let result: Skill[] | undefined;
      service.findByEmployee('emp-999').subscribe(res => result = res);

      const req = httpTesting.expectOne(r =>
        r.url === baseUrl && r.params.get('employeeId') === 'emp-999'
      );
      req.flush([]);

      expect(result).toEqual([]);
    });
  });

  describe('search', () => {
    it('should send GET to /search with query param', () => {
      const query = 'Angular';
      const mockResults: SkillSearchResult[] = [mockSearchResult];

      let result: SkillSearchResult[] | undefined;
      service.search(query).subscribe(res => result = res);

      const req = httpTesting.expectOne(r =>
        r.url === `${baseUrl}/search` && r.params.get('query') === query
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('query')).toBe(query);
      req.flush(mockResults);

      expect(result).toEqual(mockResults);
    });

    it('should return empty array when no matches found', () => {
      let result: SkillSearchResult[] | undefined;
      service.search('NonExistentSkill').subscribe(res => result = res);

      const req = httpTesting.expectOne(r =>
        r.url === `${baseUrl}/search` && r.params.get('query') === 'NonExistentSkill'
      );
      req.flush([]);

      expect(result).toEqual([]);
    });
  });

  describe('create', () => {
    it('should send POST with request body', () => {
      const request: CreateSkillRequest = {
        employeeId: 'emp-001',
        name: 'Docker',
        yearsExperience: 3,
        projectCount: 8,
        proficiency: 'Intermediate'
      };

      const createdSkill: Skill = {
        id: 'skill-002',
        employeeId: request.employeeId,
        name: request.name,
        yearsExperience: request.yearsExperience,
        projectCount: request.projectCount,
        proficiency: request.proficiency,
        createdAt: '2025-06-01T10:00:00'
      };

      let result: Skill | undefined;
      service.create(request).subscribe(res => result = res);

      const req = httpTesting.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(createdSkill);

      expect(result).toEqual(createdSkill);
    });
  });

  describe('update', () => {
    it('should send PUT to /{id} with request body', () => {
      const skillId = 'skill-001';
      const request: UpdateSkillRequest = {
        name: 'Angular Updated',
        yearsExperience: 6,
        projectCount: 14,
        proficiency: 'Expert'
      };

      const updatedSkill: Skill = {
        ...mockSkill,
        name: request.name,
        yearsExperience: request.yearsExperience,
        projectCount: request.projectCount,
        proficiency: request.proficiency
      };

      let result: Skill | undefined;
      service.update(skillId, request).subscribe(res => result = res);

      const req = httpTesting.expectOne(`${baseUrl}/${skillId}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(updatedSkill);

      expect(result).toEqual(updatedSkill);
    });
  });

  describe('delete', () => {
    it('should send DELETE to /{id}', () => {
      const skillId = 'skill-001';

      let completed = false;
      service.delete(skillId).subscribe(() => completed = true);

      const req = httpTesting.expectOne(`${baseUrl}/${skillId}`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.body).toBeNull();
      req.flush(null);

      expect(completed).toBe(true);
    });
  });

  describe('error handling', () => {
    it('should propagate HTTP 400 error from create', () => {
      const request: CreateSkillRequest = {
        employeeId: '',
        name: '',
        yearsExperience: 0,
        projectCount: 0,
        proficiency: ''
      };

      let errorResponse: HttpErrorResponse | undefined;
      service.create(request).subscribe({
        next: () => fail('should have errored'),
        error: (err: HttpErrorResponse) => errorResponse = err
      });

      const req = httpTesting.expectOne(baseUrl);
      req.flush(
        { message: 'Validation failed' },
        { status: 400, statusText: 'Bad Request' }
      );

      expect(errorResponse).toBeDefined();
      expect(errorResponse!.status).toBe(400);
    });

    it('should propagate HTTP 404 error from update', () => {
      const skillId = 'non-existent';
      const request: UpdateSkillRequest = {
        name: 'Test',
        yearsExperience: 1,
        projectCount: 1,
        proficiency: 'Beginner'
      };

      let errorResponse: HttpErrorResponse | undefined;
      service.update(skillId, request).subscribe({
        next: () => fail('should have errored'),
        error: (err: HttpErrorResponse) => errorResponse = err
      });

      const req = httpTesting.expectOne(`${baseUrl}/${skillId}`);
      req.flush(
        { message: 'Skill not found' },
        { status: 404, statusText: 'Not Found' }
      );

      expect(errorResponse).toBeDefined();
      expect(errorResponse!.status).toBe(404);
    });

    it('should propagate HTTP 500 error from search', () => {
      let errorResponse: HttpErrorResponse | undefined;
      service.search('Angular').subscribe({
        next: () => fail('should have errored'),
        error: (err: HttpErrorResponse) => errorResponse = err
      });

      const req = httpTesting.expectOne(r =>
        r.url === `${baseUrl}/search` && r.params.get('query') === 'Angular'
      );
      req.flush(
        { message: 'Internal server error' },
        { status: 500, statusText: 'Internal Server Error' }
      );

      expect(errorResponse).toBeDefined();
      expect(errorResponse!.status).toBe(500);
    });
  });
});
