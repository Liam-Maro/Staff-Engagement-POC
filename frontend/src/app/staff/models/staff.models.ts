export interface StaffMember {
  id: string;
  email: string;
  role: 'Staff' | 'Admin';
  active: boolean;
  createdAt: string;
}

export interface CreateStaffRequest {
  email: string;
  password: string;
  role: 'Staff' | 'Admin';
}

export interface UpdateStaffRequest {
  role: 'Staff' | 'Admin';
  active: boolean;
}
