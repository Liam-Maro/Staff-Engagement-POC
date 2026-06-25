import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StaffMember, CreateStaffRequest, UpdateStaffRequest } from '../models/staff.models';

@Injectable({ providedIn: 'root' })
export class StaffService {

  private readonly apiUrl = `${environment.apiUrl}/staff`;

  constructor(private http: HttpClient) {}

  findAll(): Observable<StaffMember[]> {
    return this.http.get<StaffMember[]>(this.apiUrl);
  }

  findById(id: string): Observable<StaffMember> {
    return this.http.get<StaffMember>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateStaffRequest): Observable<StaffMember> {
    return this.http.post<StaffMember>(this.apiUrl, request);
  }

  update(id: string, request: UpdateStaffRequest): Observable<StaffMember> {
    return this.http.put<StaffMember>(`${this.apiUrl}/${id}`, request);
  }

  deactivate(id: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
