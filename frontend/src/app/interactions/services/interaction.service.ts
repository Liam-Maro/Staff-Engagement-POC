import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  InteractionResponse,
  CreateInteractionRequest,
  UpdateInteractionRequest,
  CreateFollowUpTaskRequest,
  PageResponse,
  InteractionType
} from '../models/interaction.model';

export interface InteractionFilterParams {
  employeeId?: string;
  type?: InteractionType;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class InteractionService {

  private readonly apiUrl = `${environment.apiUrl}/interactions`;

  constructor(private http: HttpClient) {}

  getAll(params?: InteractionFilterParams): Observable<PageResponse<InteractionResponse>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.employeeId) httpParams = httpParams.set('employeeId', params.employeeId);
      if (params.type) httpParams = httpParams.set('type', params.type);
      if (params.fromDate) httpParams = httpParams.set('fromDate', params.fromDate);
      if (params.toDate) httpParams = httpParams.set('toDate', params.toDate);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    }
    return this.http.get<PageResponse<InteractionResponse>>(this.apiUrl, { params: httpParams });
  }

  getById(id: string): Observable<InteractionResponse> {
    return this.http.get<InteractionResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateInteractionRequest): Observable<InteractionResponse> {
    return this.http.post<InteractionResponse>(this.apiUrl, request);
  }

  update(id: string, request: UpdateInteractionRequest): Observable<InteractionResponse> {
    return this.http.put<InteractionResponse>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  createFollowUpTask(id: string, request: CreateFollowUpTaskRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/tasks`, request);
  }
}
