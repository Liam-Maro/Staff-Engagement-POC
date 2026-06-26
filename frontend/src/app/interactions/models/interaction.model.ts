export type InteractionType = 'CHECK_IN' | 'MENTORING' | 'CATCH_UP' | 'PERFORMANCE_REVIEW' | 'INFORMAL';

export const INTERACTION_TYPES: InteractionType[] = [
  'CHECK_IN', 'MENTORING', 'CATCH_UP', 'PERFORMANCE_REVIEW', 'INFORMAL'
];

export interface InteractionResponse {
  id: string;
  employeeId: string;
  staffId: string;
  type: InteractionType;
  notes: string | null;
  occurredAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInteractionRequest {
  employeeId: string;
  staffId: string;
  type: InteractionType;
  notes?: string;
  occurredAt: string;
}

export interface UpdateInteractionRequest {
  type: InteractionType;
  notes?: string;
  occurredAt: string;
}

export interface CreateFollowUpTaskRequest {
  title: string;
  description?: string;
  dueDate?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
