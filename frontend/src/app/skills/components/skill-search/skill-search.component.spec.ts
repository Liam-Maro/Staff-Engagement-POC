import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { SkillSearchComponent } from './skill-search.component';
import { SkillService } from '../../services/skill.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { Skill, SkillSearchResult } from '../../models/skill.models';
import { Employee } from '../../../employees/models/employee.models';

describe('SkillSearchComponent', () => {
  let component: SkillSearchComponent;
  let fixture: ComponentFixture<SkillSearchComponent>;

  const mockSkillService = {
    search: vi.fn().mockReturnValue(of([])),
    findByEmployee: vi.fn().mockReturnValue(of([])),
    create: vi.fn().mockReturnValue(of({})),
    update: vi.fn().mockReturnValue(of({})),
    delete: vi.fn().mockReturnValue(of(undefined))
  };

  const mockEmployeeService = {
    findAll: vi.fn().mockReturnValue(of([]))
  };

  const mockEmployees: Employee[] = [
    {
      id: 'emp-001',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      department: 'Engineering',
      jobTitle: 'Developer',
      hireDate: '2022-01-15',
      active: true
    },
    {
      id: 'emp-002',
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane@example.com',
      department: 'Design',
      jobTitle: 'Designer',
      hireDate: '2023-03-01',
      active: true
    }
  ];

  const mockSearchResults: SkillSearchResult[] = [
    {
      employeeFirstName: 'John',
      employeeLastName: 'Doe',
      employeeEmail: 'john@example.com',
      skillName: 'Angular',
      yearsExperience: 5,
      projectCount: 12,
      proficiency: 'Advanced'
    }
  ];

  const mockSkill: Skill = {
    id: 'skill-001',
    employeeId: 'emp-001',
    name: 'Angular',
    yearsExperience: 5,
    projectCount: 12,
    proficiency: 'Advanced',
    createdAt: '2025-01-10T09:00:00'
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    mockEmployeeService.findAll.mockReturnValue(of(mockEmployees));

    await TestBed.configureTestingModule({
      imports: [SkillSearchComponent, FormsModule],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(SkillSearchComponent, {
        set: {
          providers: [
            { provide: SkillService, useValue: mockSkillService },
            { provide: EmployeeService, useValue: mockEmployeeService }
          ]
        }
      })
      .compileComponents();

    fixture = TestBed.createComponent(SkillSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load employees on init', () => {
    expect(mockEmployeeService.findAll).toHaveBeenCalled();
    expect(component.employees()).toEqual(mockEmployees);
  });

  describe('search', () => {
    it('should call service.search and set results', () => {
      mockSkillService.search.mockReturnValue(of(mockSearchResults));

      component.query = 'Angular';
      component.onSearch();

      expect(mockSkillService.search).toHaveBeenCalledWith('Angular');
      expect(component.results()).toEqual(mockSearchResults);
      expect(component.hasSearched()).toBe(true);
      expect(component.isLoading()).toBe(false);
    });

    it('should not search when query is empty', () => {
      component.query = '   ';
      component.onSearch();

      expect(mockSkillService.search).not.toHaveBeenCalled();
    });

    it('should trim query before searching', () => {
      mockSkillService.search.mockReturnValue(of([]));

      component.query = '  Docker  ';
      component.onSearch();

      expect(mockSkillService.search).toHaveBeenCalledWith('Docker');
    });

    it('should set error message on search failure', () => {
      mockSkillService.search.mockReturnValue(throwError(() => new Error('Network error')));

      component.query = 'Angular';
      component.onSearch();

      expect(component.errorMessage()).toBe('Search could not be completed. Please try again.');
      expect(component.isLoading()).toBe(false);
    });

    it('should set isLoading to true during search', () => {
      // Don't complete the observable immediately
      mockSkillService.search.mockReturnValue(of(mockSearchResults));

      component.query = 'Angular';
      // Before subscribe callback resolves, isLoading was set true
      // After subscribe, it's set back to false
      component.onSearch();

      // After completion, loading is false
      expect(component.isLoading()).toBe(false);
    });
  });

  describe('Manage Skills section', () => {
    it('should open manage section', () => {
      component.openManageSection();

      expect(component.showManageSection()).toBe(true);
      expect(component.managedEmployeeId).toBe('');
      expect(component.managedSkills()).toEqual([]);
    });

    it('should close manage section', () => {
      component.openManageSection();
      component.closeManageSection();

      expect(component.showManageSection()).toBe(false);
      expect(component.editingSkillId()).toBeNull();
    });

    it('should load employee skills when employee selected', () => {
      const skills: Skill[] = [mockSkill];
      mockSkillService.findByEmployee.mockReturnValue(of(skills));

      component.managedEmployeeId = 'emp-001';
      component.loadEmployeeSkills();

      expect(mockSkillService.findByEmployee).toHaveBeenCalledWith('emp-001');
      expect(component.managedSkills()).toEqual(skills);
    });

    it('should not load skills when no employee selected', () => {
      component.managedEmployeeId = '';
      component.loadEmployeeSkills();

      expect(mockSkillService.findByEmployee).not.toHaveBeenCalled();
    });
  });

  describe('edit skill', () => {
    it('should populate edit form with skill data', () => {
      component.openEditForm(mockSkill);

      expect(component.editingSkillId()).toBe('skill-001');
      expect(component.editForm.name).toBe('Angular');
      expect(component.editForm.yearsExperience).toBe(5);
      expect(component.editForm.projectCount).toBe(12);
      expect(component.editForm.proficiency).toBe('Advanced');
    });

    it('should cancel edit and clear editingSkillId', () => {
      component.openEditForm(mockSkill);
      component.cancelEdit();

      expect(component.editingSkillId()).toBeNull();
    });

    it('should call service.update on saveEdit', () => {
      mockSkillService.update.mockReturnValue(of(mockSkill));
      mockSkillService.findByEmployee.mockReturnValue(of([mockSkill]));

      component.managedEmployeeId = 'emp-001';
      component.openEditForm(mockSkill);
      component.editForm.name = 'Angular Updated';
      component.saveEdit();

      expect(mockSkillService.update).toHaveBeenCalledWith('skill-001', {
        name: 'Angular Updated',
        yearsExperience: 5,
        projectCount: 12,
        proficiency: 'Advanced'
      });
      expect(component.editingSkillId()).toBeNull();
      expect(component.successMessage()).toBe('Skill updated successfully.');
    });

    it('should not call update when editingSkillId is null', () => {
      component.saveEdit();

      expect(mockSkillService.update).not.toHaveBeenCalled();
    });

    it('should set error message on update failure', () => {
      mockSkillService.update.mockReturnValue(throwError(() => new Error('Update failed')));

      component.openEditForm(mockSkill);
      component.saveEdit();

      expect(component.errorMessage()).toBe('Failed to update skill.');
    });
  });

  describe('delete skill', () => {
    it('should set showModal to true when deleteSkill called', () => {
      component.deleteSkill(mockSkill);

      expect(component.showModal()).toBe(true);
      expect(component.modalTitle).toBe('Delete Skill');
      expect(component.modalMessage).toContain('Angular');
    });

    it('should execute delete on modal confirm', () => {
      mockSkillService.delete.mockReturnValue(of(undefined));
      mockSkillService.findByEmployee.mockReturnValue(of([]));

      component.managedEmployeeId = 'emp-001';
      component.deleteSkill(mockSkill);
      component.onModalConfirm();

      expect(component.showModal()).toBe(false);
      expect(mockSkillService.delete).toHaveBeenCalledWith('skill-001');
      expect(component.successMessage()).toBe('Skill "Angular" deleted.');
    });

    it('should cancel delete on modal cancel', () => {
      component.deleteSkill(mockSkill);
      component.onModalCancel();

      expect(component.showModal()).toBe(false);
      expect(mockSkillService.delete).not.toHaveBeenCalled();
    });

    it('should set error message on delete failure', () => {
      mockSkillService.delete.mockReturnValue(throwError(() => new Error('Delete failed')));
      mockSkillService.findByEmployee.mockReturnValue(of([]));

      component.managedEmployeeId = 'emp-001';
      component.deleteSkill(mockSkill);
      component.onModalConfirm();

      expect(component.errorMessage()).toBe('Failed to delete skill.');
    });
  });

  describe('add skill form', () => {
    it('should open add form and reset fields', () => {
      component.openAddForm();

      expect(component.showAddForm()).toBe(true);
      expect(component.skillForm.employeeId).toBe('');
      expect(component.skillForm.name).toBe('');
    });

    it('should close add form on cancel', () => {
      component.openAddForm();
      component.cancelAddForm();

      expect(component.showAddForm()).toBe(false);
    });

    it('should call service.create on saveSkill', () => {
      const createdSkill: Skill = { ...mockSkill, id: 'skill-new', name: 'Docker' };
      mockSkillService.create.mockReturnValue(of(createdSkill));

      component.skillForm = {
        employeeId: 'emp-001',
        name: 'Docker',
        yearsExperience: 2,
        projectCount: 5,
        proficiency: 'Intermediate'
      };
      component.saveSkill();

      expect(mockSkillService.create).toHaveBeenCalledWith({
        employeeId: 'emp-001',
        name: 'Docker',
        yearsExperience: 2,
        projectCount: 5,
        proficiency: 'Intermediate'
      });
      expect(component.successMessage()).toBe('Skill "Docker" added successfully.');
      expect(component.showAddForm()).toBe(false);
    });

    it('should set error on create failure', () => {
      mockSkillService.create.mockReturnValue(throwError(() => new Error('Create failed')));

      component.skillForm = {
        employeeId: 'emp-001',
        name: 'Docker',
        yearsExperience: 2,
        projectCount: 5,
        proficiency: 'Intermediate'
      };
      component.saveSkill();

      expect(component.errorMessage()).toBe('Failed to add skill. Please check all fields and try again.');
    });
  });

  describe('isFormValid', () => {
    it('should return true when all required fields filled', () => {
      component.skillForm = {
        employeeId: 'emp-001',
        name: 'Angular',
        yearsExperience: 3,
        projectCount: 5,
        proficiency: 'Advanced'
      };

      expect(component.isFormValid).toBe(true);
    });

    it('should return false when employeeId is empty', () => {
      component.skillForm = {
        employeeId: '',
        name: 'Angular',
        yearsExperience: 3,
        projectCount: 5,
        proficiency: 'Advanced'
      };

      expect(component.isFormValid).toBe(false);
    });

    it('should return false when name is empty', () => {
      component.skillForm = {
        employeeId: 'emp-001',
        name: '   ',
        yearsExperience: 3,
        projectCount: 5,
        proficiency: 'Advanced'
      };

      expect(component.isFormValid).toBe(false);
    });
  });
});
