import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CreateTaskRequest, TaskResponse } from '../models/task.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TaskService {

  private readonly baseUrl = `${environment.apiUrl}/tasks`;
  private readonly http = inject(HttpClient);

  createTask(request: CreateTaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(this.baseUrl, request);
  }

  getTasksByAssignee(assigneeId: string, sortOrder?: string): Observable<TaskResponse[]> {
    let params = new HttpParams().set('assigneeId', assigneeId);
    if (sortOrder) {
      params = params.set('sortOrder', sortOrder);
    }
    return this.http.get<TaskResponse[]>(this.baseUrl, { params });
  }

  getTasksByCreator(creatorId: string, sortOrder?: string): Observable<TaskResponse[]> {
    let params = new HttpParams().set('creatorId', creatorId);
    if (sortOrder) {
      params = params.set('sortOrder', sortOrder);
    }
    return this.http.get<TaskResponse[]>(this.baseUrl, { params });
  }

  updateTaskStatus(taskId: string, status: string): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.baseUrl}/${taskId}/status`, { status });
  }
}
