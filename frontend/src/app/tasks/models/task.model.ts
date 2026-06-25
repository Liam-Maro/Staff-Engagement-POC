export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED';

export interface TaskResponse {
  id: string;
  employeeId: string;
  interactionId: string | null;
  creatorId: string | null;
  assigneeId: string | null;
  title: string;
  description: string | null;
  status: TaskStatus;
  dueDate: string | null;
  createdAt: string;
}

export interface CreateTaskRequest {
  employeeId: string;
  interactionId?: string;
  assigneeId: string;
  title: string;
  description?: string;
  dueDate?: string;
}
