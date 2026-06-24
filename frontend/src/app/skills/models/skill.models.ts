export interface Skill {
  id: string;
  employeeId: string;
  name: string;
  yearsExperience: number;
  projectCount: number;
  proficiency: string;
  createdAt: string;
}

export interface CreateSkillRequest {
  employeeId: string;
  name: string;
  yearsExperience: number;
  projectCount: number;
  proficiency: string;
}

export interface UpdateSkillRequest {
  name: string;
  yearsExperience: number;
  projectCount: number;
  proficiency: string;
}

export interface SkillSearchResult {
  employeeFirstName: string;
  employeeLastName: string;
  employeeEmail: string;
  skillName: string;
  yearsExperience: number;
  projectCount: number;
  proficiency: string;
}
