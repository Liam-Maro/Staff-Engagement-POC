import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskQueryParams,
  TaskQueryResult,
  TaskResponse,
  InteractionSummary
} from '../models/task.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TaskService {

  private readonly baseUrl = `${environment.apiUrl}/tasks`;
  private readonly http = inject(HttpClient);

  createTask(request: CreateTaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(this.baseUrl, request);
  }

  updateTask(id: string, request: UpdateTaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`${this.baseUrl}/${id}`, request);
  }

  deleteTask(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getTasks(params: TaskQueryParams): Observable<TaskQueryResult> {
    let httpParams = new HttpParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return this.http.get<TaskQueryResult>(this.baseUrl, { params: httpParams });
  }

  getTaskById(id: string): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(`${this.baseUrl}/${id}`);
  }

  updateTaskStatus(taskId: string, status: string): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.baseUrl}/${taskId}/status`, { status });
  }

  getInteractionsForIndividual(individualId: string): Observable<InteractionSummary[]> {
    const params = new HttpParams().set('individualId', individualId);
    return this.http.get<InteractionSummary[]>(`${this.baseUrl}/interactions`, { params });
  }
}
