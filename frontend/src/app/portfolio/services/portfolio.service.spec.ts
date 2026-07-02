import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PortfolioService } from './portfolio.service';
import { ImportResult } from '../models/portfolio.models';

describe('PortfolioService - importGitHubSkills', () => {
  let service: PortfolioService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PortfolioService
      ]
    });

    service = TestBed.inject(PortfolioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should send POST request to correct URL', () => {
    const employeeId = '123e4567-e89b-12d3-a456-426614174000';
    const url = 'https://github.com/octocat';

    service.importGitHubSkills(employeeId, url).subscribe();

    const req = httpMock.expectOne(
      `http://localhost:8080/api/portfolios/${employeeId}/github-import`
    );
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should send githubProfileUrl in request body', () => {
    const employeeId = 'emp-001';
    const url = 'https://github.com/testuser';

    service.importGitHubSkills(employeeId, url).subscribe();

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    expect(req.request.body).toEqual({ githubProfileUrl: 'https://github.com/testuser' });
    req.flush({});
  });

  it('should return ImportResult on success', () => {
    const employeeId = 'emp-002';
    const url = 'https://github.com/octocat';
    const mockResult: ImportResult = {
      skills: [
        { id: 'sk-1', name: 'Java', projectCount: 5, proficiency: 'EXPERT', source: 'GITHUB' },
        { id: 'sk-2', name: 'Python', projectCount: 3, proficiency: 'ADVANCED', source: 'GITHUB' }
      ],
      githubProfileUrl: url,
      repositoriesAnalysed: 12,
      skippedRepositories: ['private-repo']
    };

    let result: ImportResult | undefined;
    service.importGitHubSkills(employeeId, url).subscribe(r => result = r);

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush(mockResult);

    expect(result).toEqual(mockResult);
  });

  it('should propagate 400 error', () => {
    const employeeId = 'emp-003';
    const url = 'not-a-valid-url';

    let error: any;
    service.importGitHubSkills(employeeId, url).subscribe({
      error: e => error = e
    });

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush(
      { message: 'Invalid GitHub profile URL format' },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(error.status).toBe(400);
  });

  it('should propagate 404 error', () => {
    const employeeId = 'emp-004';
    const url = 'https://github.com/nonexistent';

    let error: any;
    service.importGitHubSkills(employeeId, url).subscribe({
      error: e => error = e
    });

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush(
      { message: 'GitHub user not found: nonexistent' },
      { status: 404, statusText: 'Not Found' }
    );

    expect(error.status).toBe(404);
  });

  it('should propagate 429 rate limit error', () => {
    const employeeId = 'emp-005';
    const url = 'https://github.com/octocat';

    let error: any;
    service.importGitHubSkills(employeeId, url).subscribe({
      error: e => error = e
    });

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush(
      { message: 'GitHub API rate limit exceeded' },
      { status: 429, statusText: 'Too Many Requests' }
    );

    expect(error.status).toBe(429);
  });

  it('should propagate 502 error', () => {
    const employeeId = 'emp-006';
    const url = 'https://github.com/octocat';

    let error: any;
    service.importGitHubSkills(employeeId, url).subscribe({
      error: e => error = e
    });

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush(
      { message: 'GitHub API is currently unavailable' },
      { status: 502, statusText: 'Bad Gateway' }
    );

    expect(error.status).toBe(502);
  });

  it('should propagate 504 timeout error', () => {
    const employeeId = 'emp-007';
    const url = 'https://github.com/octocat';

    let error: any;
    service.importGitHubSkills(employeeId, url).subscribe({
      error: e => error = e
    });

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush(
      { message: 'GitHub API request timed out' },
      { status: 504, statusText: 'Gateway Timeout' }
    );

    expect(error.status).toBe(504);
  });
});
