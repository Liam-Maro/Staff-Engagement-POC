export interface StaffMember {
  id: string;
  employeeId: string;
  email: string;
  role: 'STAFF' | 'ADMIN';
  active: boolean;
  createdAt: string;
}

export interface CreateStaffRequest {
  employeeId: string;
  email: string;
  password: string;
  role: 'STAFF' | 'ADMIN';
}

export interface UpdateStaffRequest {
  role: 'STAFF' | 'ADMIN';
  active: boolean;
}
