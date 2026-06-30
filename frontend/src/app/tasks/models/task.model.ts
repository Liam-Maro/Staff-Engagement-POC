export type TaskStatus = 'To Do' | 'In Progress' | 'Done';

export interface TaskResponse {
  id: string;
  individualId: string;
  interactionId: string | null;
  creatorId: string;
  assigneeId: string;
  description: string;
  status: TaskStatus;
  dueDate: string | null;
  createdAt: string;
}

export interface CreateTaskRequest {
  individualId: string;
  interactionId?: string;
  assigneeId: string;
  description: string;
  dueDate?: string;
}

export interface UpdateTaskRequest {
  individualId: string;
  interactionId?: string;
  assigneeId: string;
  description: string;
  dueDate?: string | null;
}

export interface TaskQueryParams {
  assigneeId?: string;
  creatorId?: string;
  excludeSelfAssigned?: boolean;
  status?: TaskStatus;
  dueDateFrom?: string;
  dueDateTo?: string;
  createdFrom?: string;
  createdTo?: string;
  sortBy?: 'dueDate' | 'createdDate';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export interface TaskQueryResult {
  tasks: TaskResponse[];
  totalCount: number;
  currentPage: number;
  pageSize: number;
}

export interface InteractionSummary {
  id: string;
  employeeId: string;
  staffId: string;
  type: string;
  notes: string;
  occurredAt: string;
  createdAt: string;
}
