import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { PortfolioService } from '../../services/portfolio.service';
import { ImportResult } from '../../models/portfolio.models';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-import-skills',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './import-skills.component.html',
  styleUrls: ['./import-skills.component.css']
})
export class ImportSkillsComponent implements OnInit {

  @Input({ required: true }) employeeId!: string;

  private portfolioService = inject(PortfolioService);

  ngOnInit(): void {
    this.portfolioService.getLinks(this.employeeId).subscribe({
      next: (links) => {
        const githubLink = links.find(link => link.label === 'GitHub');
        if (githubLink) {
          this.urlControl.setValue(githubLink.url);
        }
      }
    });
  }

  urlControl = new FormControl('', {
    validators: [Validators.maxLength(2048)],
    nonNullable: true
  });

  loading = signal(false);
  result = signal<ImportResult | null>(null);
  error = signal<string | null>(null);
  validationError = signal<string | null>(null);

  importSkills(): void {
    const url = this.urlControl.value.trim();

    if (!url) {
      this.validationError.set('GitHub profile URL is required.');
      return;
    }

    this.validationError.set(null);
    this.error.set(null);
    this.result.set(null);
    this.loading.set(true);

    this.portfolioService.importGitHubSkills(this.employeeId, url).subscribe({
      next: (importResult) => {
        this.result.set(importResult);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const message = err.error?.message ?? 'An unexpected error occurred. Please try again.';
        this.error.set(message);
        this.loading.set(false);
      }
    });
  }
}
