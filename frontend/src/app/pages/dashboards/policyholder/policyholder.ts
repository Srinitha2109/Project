import { Component, inject, signal } from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth';
import { NotificationBellComponent } from '../../../shared/notification-bell/notification-bell.component';
import { PolicySearchService } from '../../../services/policy-search';

@Component({
  selector: 'app-policyholder',
  imports: [RouterModule, NotificationBellComponent, FormsModule],
  templateUrl: './policyholder.html',
  styleUrl: './policyholder.css',
})
export class PolicyholderComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  readonly policySearchService = inject(PolicySearchService);

  isSidebarCollapsed = signal(false);
  isOnPoliciesPage = signal(false);
  navSearchQuery = signal('');
  currentUser = this.authService.currentUser;

  constructor() {
    // Detect when we navigate to/from the policies page
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: NavigationEnd) => {
        const onPolicies = e.urlAfterRedirects.includes('/policyholder/policies');
        this.isOnPoliciesPage.set(onPolicies);
        if (!onPolicies) {
          // Reset search when leaving the policies page
          this.navSearchQuery.set('');
          this.policySearchService.reset();
        }
      });

    // Also check the initial URL on component load
    const url = this.router.url;
    this.isOnPoliciesPage.set(url.includes('/policyholder/policies'));
  }

  onNavSearch(query: string) {
    this.navSearchQuery.set(query);
    this.policySearchService.setQuery(query);
  }

  toggleSidebar() {
    this.isSidebarCollapsed.set(!this.isSidebarCollapsed());
  }

  getUserInitials(): string {
    const user = this.authService.currentUser();
    if (!user || !user.fullName) return 'U';
    const names = user.fullName.trim().split(/\s+/);
    if (names.length >= 2) {
      return (names[0][0] + names[names.length - 1][0]).toUpperCase();
    }
    return names[0][0].toUpperCase();
  }

  logout() {
    this.authService.logout();
  }
}
