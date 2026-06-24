import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SkillService } from '../../services/skill.service';
import { SkillSearchResult } from '../../models/skill.models';

@Component({
  selector: 'app-skill-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './skill-search.component.html',
  styleUrl: './skill-search.component.css'
})
export class SkillSearchComponent {

  query = '';
  results = signal<SkillSearchResult[]>([]);
  hasSearched = signal(false);
  isLoading = signal(false);

  constructor(private skillService: SkillService) {}

  onSearch(): void {
    if (!this.query.trim()) return;

    this.isLoading.set(true);
    this.hasSearched.set(true);

    this.skillService.search(this.query.trim()).subscribe({
      next: (data) => {
        this.results.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }
}
