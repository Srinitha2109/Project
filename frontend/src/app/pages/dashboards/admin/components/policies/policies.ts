import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PolicyService, Policy } from '../../../../../services/policy';
import { NotificationService } from '../../../../../services/notification';

@Component({
  selector: 'app-admin-policies',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './policies.html',
  styleUrl: './policies.css'
})
export class PoliciesComponent implements OnInit {
  private policyService = inject(PolicyService);
  private notificationService = inject(NotificationService);

  policies = signal<Policy[]>([]);
  showPolicyForm = signal(false);
  isEditMode = signal(false);
  formSubmitted = signal(false);

  currentPolicy = signal<any>({
    policyNumber: '',
    policyName: '',
    insuranceType: '',
    description: '',
    minCoverageAmount: 0,
    maxCoverageAmount: 0,
    basePremium: 0,
    durationMonths: 12,
    isActive: true
  });

  searchQuery = signal('');
  filterStatus = signal<'all' | 'active' | 'inactive'>('all');
  filterType = signal<string | 'all'>('all');

  // Enhanced fuzzy match: handles sub-strings, word starts, and small spelling errors
  private fuzzyMatch(query: string, target: string): boolean {
    if (!query) return true;
    if (!target) return false;
    const q = query.toLowerCase();
    const t = target.toLowerCase();

    // 1. Exact substring match (highest priority)
    if (t.includes(q)) return true;

    // 2. Term-based matching (matches starts of words)
    const words = t.split(/[\s_-]+/);
    if (words.some(word => word.startsWith(q))) return true;

    // 3. Levenshtein distance for spelling errors (only for queries > 2 chars)
    if (q.length > 2) {
      // Check each word in target for spelling similarity
      for (const word of words) {
        if (this.getLevenshteinDistance(q, word) <= 1) return true;
        // For longer queries, allow 2 errors
        if (q.length > 5 && this.getLevenshteinDistance(q, word) <= 2) return true;
      }
    }

    return false;
  }

  private getLevenshteinDistance(a: string, b: string): number {
    const matrix = Array.from({ length: a.length + 1 }, () => 
      Array.from({ length: b.length + 1 }, (_, j) => j)
    );
    for (let i = 1; i <= a.length; i++) matrix[i][0] = i;

    for (let i = 1; i <= a.length; i++) {
      for (let j = 1; j <= b.length; j++) {
        const cost = a[i - 1] === b[j - 1] ? 0 : 1;
        matrix[i][j] = Math.min(
          matrix[i - 1][j] + 1,      // deletion
          matrix[i][j - 1] + 1,      // insertion
          matrix[i - 1][j - 1] + cost // substitution
        );
      }
    }
    return matrix[a.length][b.length];
  }

  filteredPolicies = computed(() => {
    let list = this.policies();
    const query = this.searchQuery().toLowerCase().trim();
    const status = this.filterStatus();
    const typeId = this.filterType();

    if (query) {
      list = list.filter(p => {
        const searchableFields = [
          p.policyName,
          p.policyNumber,
          p.insuranceType,
          p.insuranceTypeDisplayName,
          p.description
        ];
        return searchableFields.some(field => this.fuzzyMatch(query, field || ''));
      });
    }
    if (status !== 'all') {
      list = list.filter(p => p.isActive === (status === 'active'));
    }
    if (typeId !== 'all') {
      list = list.filter(p => p.insuranceType === typeId);
    }
    return list;
  });

  ngOnInit() {
    this.loadPolicies();
  }

  loadPolicies() {
    this.policyService.getAllPolicies().subscribe({
      next: (data) => this.policies.set(data),
      error: () => this.notificationService.show('Failed to load policies', 'error')
    });
  }

  openCreateForm() {
    this.isEditMode.set(false);
    this.resetForm();
    this.formSubmitted.set(false);
    this.showPolicyForm.set(true);
  }

  openEditForm(policy: Policy) {
    this.isEditMode.set(true);
    this.currentPolicy.set({ ...policy });
    this.formSubmitted.set(false);
    this.showPolicyForm.set(true);
  }

  savePolicy() {
    const policy = this.currentPolicy();
    if (!policy.policyName || !policy.insuranceType || !policy.durationMonths || !policy.basePremium) {
      this.formSubmitted.set(true);
      this.notificationService.show('Please fill all required fields correctly', 'warning');
      return;
    }

    if (this.isEditMode()) {
      this.policyService.updatePolicy(policy.id, policy).subscribe({
        next: () => {
          this.notificationService.show('Policy updated successfully!', 'success');
          this.showPolicyForm.set(false);
          this.loadPolicies();
        },
        error: () => this.notificationService.show('Failed to update policy', 'error')
      });
    } else {
      this.policyService.createPolicy(policy).subscribe({
        next: () => {
          this.notificationService.show('Policy created successfully!', 'success');
          this.showPolicyForm.set(false);
          this.loadPolicies();
        },
        error: () => this.notificationService.show('Failed to create policy', 'error')
      });
    }
  }

  resetForm() {
    this.currentPolicy.set({
      policyNumber: '',
      policyName: '',
      insuranceType: '',
      description: '',
      minCoverageAmount: 0,
      maxCoverageAmount: 0,
      basePremium: 0,
      durationMonths: 12,
      isActive: true
    });
  }
}
