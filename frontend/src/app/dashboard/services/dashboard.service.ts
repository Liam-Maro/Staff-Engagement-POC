import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface DashboardStats {
  totalEmployees: number;
  openTasks: number;
  interactionsThisMonth: number;
  overdueTasks: number;
}

export interface RecentInteraction {
  id: string;
  employeeName: string;
  type: string;
  occurredAt: string;
}

export interface TaskStatusBreakdown {
  open: number;
  inProgress: number;
  completed: number;
}

export interface SkillCount {
  name: string;
  count: number;
}

export interface NeglectedEmployee {
  name: string;
  daysSince: number;
}

export interface UpcomingTask {
  id: string;
  title: string;
  employeeName: string;
  dueDate: string;
  status: string;
}

export interface DashboardData {
  stats: DashboardStats;
  recentInteractions: RecentInteraction[];
  taskBreakdown: TaskStatusBreakdown;
  topSkills: SkillCount[];
  neglectedEmployees: NeglectedEmployee[];
  upcomingTasks: UpcomingTask[];
}

interface EmployeeResponse {
  id: string;
  firstName: string;
  lastName: string;
  department: string;
  active: boolean;
}

interface TaskResponse {
  id: string;
  individualId: string;
  description: string;
  status: string;
  dueDate: string | null;
}

interface InteractionResponse {
  id: string;
  employeeId: string;
  type: string;
  occurredAt: string;
}

interface SkillResponse {
  id: string;
  employeeId: string;
  name: string;
  proficiency: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getDashboardData(role: string, staffId: string): Observable<DashboardData> {
    const employees$ = this.http.get<EmployeeResponse[]>(`${this.apiUrl}/employees`).pipe(catchError(() => of([])));
    const tasks$ = (role === 'Admin'
      ? this.http.get<{ tasks: TaskResponse[] }>(`${this.apiUrl}/tasks?size=200`).pipe(map(r => r.tasks))
      : this.http.get<TaskResponse[]>(`${this.apiUrl}/tasks?staffId=${staffId}`)
    ).pipe(catchError(() => of([] as TaskResponse[])));
    const interactions$ = this.http.get<{ content: InteractionResponse[] }>(`${this.apiUrl}/interactions?size=100`).pipe(
      map(r => r.content),
      catchError(() => of([] as InteractionResponse[]))
    );
    const skills$ = this.http.get<SkillResponse[]>(`${this.apiUrl}/skills`).pipe(catchError(() => of([])));

    return forkJoin([employees$, tasks$, interactions$, skills$]).pipe(
      map(([employees, tasks, interactions, skills]) => {
        const now = new Date();
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
        const today = now.toISOString().split('T')[0];

        // Build employee lookup
        const employeeMap = new Map<string, EmployeeResponse>();
        employees.forEach(e => employeeMap.set(e.id, e));

        // --- Row 1: Stats ---
        const totalEmployees = employees.filter(e => e.active).length;
        const openTasks = tasks.filter(t => t.status !== 'Done').length;
        const overdueTasks = tasks.filter(t =>
          t.status !== 'Done' && t.dueDate && t.dueDate < today
        ).length;
        const interactionsThisMonth = interactions.filter(i => {
          const date = new Date(i.occurredAt);
          return date >= startOfMonth;
        }).length;

        const stats: DashboardStats = { totalEmployees, openTasks, interactionsThisMonth, overdueTasks };

        // --- Row 2: Recent Interactions ---
        const sortedInteractions = [...interactions]
          .sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())
          .slice(0, 3);

        const recentInteractions: RecentInteraction[] = sortedInteractions.map(i => {
          const emp = employeeMap.get(i.employeeId);
          return {
            id: i.id,
            employeeName: emp ? `${emp.firstName} ${emp.lastName}` : 'Unknown',
            type: i.type,
            occurredAt: i.occurredAt
          };
        });

        // --- Row 2: Task Status Breakdown ---
        const taskBreakdown: TaskStatusBreakdown = {
          open: tasks.filter(t => t.status === 'To Do').length,
          inProgress: tasks.filter(t => t.status === 'In Progress').length,
          completed: tasks.filter(t => t.status === 'Done').length
        };

        // --- Row 3: Top Skills ---
        const skillCounts = new Map<string, number>();
        skills.forEach(s => {
          skillCounts.set(s.name, (skillCounts.get(s.name) || 0) + 1);
        });
        const topSkills: SkillCount[] = [...skillCounts.entries()]
          .sort((a, b) => b[1] - a[1])
          .slice(0, 3)
          .map(([name, count]) => ({ name, count }));

        // --- Row 3: Engagement Coverage (no interaction in 30+ days) ---
        const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
        const employeesWithRecentInteraction = new Set<string>();
        interactions.forEach(i => {
          if (new Date(i.occurredAt) >= thirtyDaysAgo) {
            employeesWithRecentInteraction.add(i.employeeId);
          }
        });

        const neglectedEmployees: NeglectedEmployee[] = employees
          .filter(e => e.active && !employeesWithRecentInteraction.has(e.id))
          .map(e => {
            // Find last interaction date for this employee
            const lastInteraction = interactions
              .filter(i => i.employeeId === e.id)
              .sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())[0];
            const daysSince = lastInteraction
              ? Math.floor((now.getTime() - new Date(lastInteraction.occurredAt).getTime()) / (1000 * 60 * 60 * 24))
              : 999;
            return { name: `${e.firstName} ${e.lastName}`, daysSince };
          })
          .sort((a, b) => b.daysSince - a.daysSince)
          .slice(0, 5);

        // --- Row 4: Upcoming & Overdue Tasks ---
        const upcomingTasks: UpcomingTask[] = tasks
          .filter(t => t.status !== 'Done' && t.dueDate)
          .sort((a, b) => a.dueDate!.localeCompare(b.dueDate!))
          .slice(0, 5)
          .map(t => {
            const emp = employeeMap.get(t.individualId);
            return {
              id: t.id,
              title: t.description.length > 60 ? t.description.substring(0, 60) + '...' : t.description,
              employeeName: emp ? `${emp.firstName} ${emp.lastName}` : 'Unknown',
              dueDate: t.dueDate!,
              status: t.status
            };
          });

        return { stats, recentInteractions, taskBreakdown, topSkills, neglectedEmployees, upcomingTasks };
      })
    );
  }
}
