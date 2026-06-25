export interface StaffMember {
  id: string;
  email: string;
  role: 'STAFF' | 'ADMIN';
  active: boolean;
  createdAt: string;
}

export interface CreateStaffRequest {
  email: string;
  password: string;
  role: 'STAFF' | 'ADMIN';
}

export interface UpdateStaffRequest {
  role: 'STAFF' | 'ADMIN';
  active: boolean;
}
