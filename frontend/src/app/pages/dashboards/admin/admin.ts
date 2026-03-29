import { Component, inject, signal } from '@angular/core';

import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth';
import { NotificationBellComponent } from '../../../shared/notification-bell/notification-bell.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterModule, NotificationBellComponent],
  templateUrl: './admin.html',
})
export class AdminComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  currentUser = this.authService.currentUser;

  isSidebarOpen = signal(true);

  toggleSidebar() {
    this.isSidebarOpen.update(v => !v);
  }

  getUserInitials(): string {
    const user = this.authService.currentUser();
    if (!user || !user.fullName) return 'U';
    const names = user.fullName.trim().split(/\s+/);
    if (names.length >= 2) return (names[0][0] + names[names.length - 1][0]).toUpperCase();
    return names[0][0].toUpperCase();
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
