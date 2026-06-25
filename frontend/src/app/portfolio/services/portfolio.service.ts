import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FullPortfolio,
  PortfolioSkill,
  Education,
  CreateEducationRequest,
  UpdateEducationRequest,
  Project,
  CreateProjectRequest,
  UpdateProjectRequest,
  PortfolioLink,
  CreateLinkRequest,
  UpdateLinkRequest
} from '../models/portfolio.models';

@Injectable({ providedIn: 'root' })
export class PortfolioService {

  private readonly apiUrl = `${environment.apiUrl}/portfolios`;

  constructor(private http: HttpClient) {}

  // Full portfolio
  getFullPortfolio(employeeId: string): Observable<FullPortfolio> {
    return this.http.get<FullPortfolio>(`${this.apiUrl}/${employeeId}`);
  }

  // Skills (read-only — managed via Skills Register at /api/skills)
  getSkills(employeeId: string): Observable<PortfolioSkill[]> {
    return this.http.get<PortfolioSkill[]>(`${this.apiUrl}/${employeeId}/skills`);
  }

  // Education
  getEducation(employeeId: string): Observable<Education[]> {
    return this.http.get<Education[]>(`${this.apiUrl}/${employeeId}/education`);
  }

  createEducation(employeeId: string, request: CreateEducationRequest): Observable<Education> {
    return this.http.post<Education>(`${this.apiUrl}/${employeeId}/education`, request);
  }

  updateEducation(educationId: string, request: UpdateEducationRequest): Observable<Education> {
    return this.http.put<Education>(`${this.apiUrl}/education/${educationId}`, request);
  }

  deleteEducation(educationId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/education/${educationId}`);
  }

  // Projects
  getProjects(employeeId: string): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}/${employeeId}/projects`);
  }

  createProject(employeeId: string, request: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}/${employeeId}/projects`, request);
  }

  updateProject(projectId: string, request: UpdateProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.apiUrl}/projects/${projectId}`, request);
  }

  deleteProject(projectId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/projects/${projectId}`);
  }

  // Links
  getLinks(employeeId: string): Observable<PortfolioLink[]> {
    return this.http.get<PortfolioLink[]>(`${this.apiUrl}/${employeeId}/links`);
  }

  createLink(employeeId: string, request: CreateLinkRequest): Observable<PortfolioLink> {
    return this.http.post<PortfolioLink>(`${this.apiUrl}/${employeeId}/links`, request);
  }

  updateLink(linkId: string, request: UpdateLinkRequest): Observable<PortfolioLink> {
    return this.http.put<PortfolioLink>(`${this.apiUrl}/links/${linkId}`, request);
  }

  deleteLink(linkId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/links/${linkId}`);
  }
}
