export type ProficiencyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';

export interface PortfolioSkill {
  id: string;
  employeeId: string;
  name: string;
  yearsExperience: number;
  projectCount: number;
  proficiency: ProficiencyLevel;
  createdAt: string;
}

export interface Education {
  id: string;
  employeeId: string;
  institution: string;
  degree: string;
  fieldOfStudy: string | null;
  graduationDate: string | null;
  createdAt: string;
}

export interface Project {
  id: string;
  employeeId: string;
  projectName: string;
  description: string | null;
  role: string;
  technologies: string[];
  startDate: string;
  endDate: string | null;
  createdAt: string;
}

export interface PortfolioLink {
  id: string;
  employeeId: string;
  url: string;
  label: string;
  createdAt: string;
}

export interface FullPortfolio {
  skills: PortfolioSkill[];
  education: Education[];
  projects: Project[];
  links: PortfolioLink[];
}

// Request interfaces

export interface CreateEducationRequest {
  institution: string;
  degree: string;
  fieldOfStudy: string | null;
  graduationDate: string | null;
}

export interface UpdateEducationRequest {
  institution: string;
  degree: string;
  fieldOfStudy: string | null;
  graduationDate: string | null;
}

export interface CreateProjectRequest {
  projectName: string;
  description: string | null;
  role: string;
  technologies: string[];
  startDate: string;
  endDate: string | null;
}

export interface UpdateProjectRequest {
  projectName: string;
  description: string | null;
  role: string;
  technologies: string[];
  startDate: string;
  endDate: string | null;
}

export interface CreateLinkRequest {
  url: string;
  label: string;
}

export interface UpdateLinkRequest {
  url: string;
  label: string;
}
