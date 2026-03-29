import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { NotificationService } from './services/notification';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  notificationService = inject(NotificationService);
  private router = inject(Router);

  notifications = this.notificationService.getNotifications();

  isDashboard(): boolean {
    const url = this.router.url;
    return url.includes('dashboard') ||
      url.includes('admin') ||
      url.includes('policyholder') ||
      url.includes('agent') ||
      url.includes('underwriter') ||
      url.includes('claim-officer');
  }

  isLanding(): boolean {
    const url = this.router.url;
    return url === '/' || url === '/landing';
  }
}

