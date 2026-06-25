import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Skill, CreateSkillRequest, UpdateSkillRequest, SkillSearchResult } from '../models/skill.models';

@Injectable({ providedIn: 'root' })
export class SkillService {

  private readonly apiUrl = `${environment.apiUrl}/skills`;

  constructor(private http: HttpClient) {}

  findByEmployee(employeeId: string): Observable<Skill[]> {
    const params = new HttpParams().set('employeeId', employeeId);
    return this.http.get<Skill[]>(this.apiUrl, { params });
  }

  search(query: string): Observable<SkillSearchResult[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<SkillSearchResult[]>(`${this.apiUrl}/search`, { params });
  }

  create(request: CreateSkillRequest): Observable<Skill> {
    return this.http.post<Skill>(this.apiUrl, request);
  }

  update(id: string, request: UpdateSkillRequest): Observable<Skill> {
    return this.http.put<Skill>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
