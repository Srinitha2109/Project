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

  isSidebarCollapsed = signal(window.innerWidth < 768);
  isOnPoliciesPage = signal(false);
  navSearchQuery = signal('');
  currentUser = this.authService.currentUser;

  constructor() {
    // Detect when we navigate to/from the policies page
    this.router.events //it runs only when navigation happens
      .pipe(filter(e => e instanceof NavigationEnd)) // router emitted event after redirecting to correct url - it shows final resolved url
      //navigationstart shows the user requested url before angular applies routing logic
      .subscribe((e: NavigationEnd) => { //detcts when user move between pages
        const onPolicies = e.urlAfterRedirects.includes('/policyholder/policies'); //gives final url after all redirects 
        this.isOnPoliciesPage.set(onPolicies);
        if (!onPolicies) {
          // Reset search when leaving the policies page
          this.navSearchQuery.set('');
          this.policySearchService.reset(); //clears global search query
        }
      });

    // Also check the initial URL on component load
    const url = this.router.url;
    this.isOnPoliciesPage.set(url.includes('/policyholder/policies'));
  }

  //when some words are typed in search bar it is called
  onNavSearch(query: string) {
    this.navSearchQuery.set(query);
    this.policySearchService.setQuery(query);
  }

  toggleSidebar() {
    this.isSidebarCollapsed.set(!this.isSidebarCollapsed());
  }

  closeSidebarOnMobile() {
    if (window.innerWidth < 768) {
      this.isSidebarCollapsed.set(true);
    }
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
