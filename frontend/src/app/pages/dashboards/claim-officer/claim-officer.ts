import { Component, inject, signal } from '@angular/core';

import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../services/auth';
import { NotificationBellComponent } from '../../../shared/notification-bell/notification-bell.component';

@Component({
  selector: 'app-claim-officer',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBellComponent],
  template: `
    <div class="flex h-screen overflow-hidden bg-slate-50 font-sans">
      <!-- Mobile Overlay Backdrop -->
      @if (!isSidebarCollapsed()) {
      <div class="fixed inset-0 bg-black/30 z-40 md:hidden" (click)="toggleSidebar()"></div>
      }

      <!-- Sidebar -->
      <aside class="flex flex-col bg-white border-r border-slate-200 shadow-sm z-50 shrink-0
             fixed md:relative h-full"
             [class.w-64]="!isSidebarCollapsed()"
             [class.w-20]="isSidebarCollapsed()"
             [class.-translate-x-full]="isSidebarCollapsed()"
             [class.translate-x-0]="!isSidebarCollapsed()"
             style="transition: width 0.3s ease, transform 0.3s ease;">
        <div class="h-16 flex items-center px-4 gap-4 border-b border-slate-200 shrink-0">
          <button (click)="toggleSidebar()"
                  class="p-2 rounded-md hover:bg-burgundy/5 text-burgundy transition-colors focus:outline-none">
            <span class="material-icons text-xl leading-none">menu</span>
          </button>
          @if (!isSidebarCollapsed()) {
            <span class="font-extrabold text-burgundy text-base tracking-widest uppercase truncate">Claims Dept</span>
          }
        </div>
        
        <nav class="flex-1 flex flex-col p-4 gap-2 overflow-y-auto">
          <a routerLink="/claim-officer/dashboard" routerLinkActive="active-link"
             [routerLinkActiveOptions]="{exact: true}"
             class="flex items-center p-3 rounded-lg transition-all duration-200 cursor-pointer gap-3 group text-slate-600 hover:bg-burgundy/5 hover:text-burgundy"
             (click)="closeSidebarOnMobile()">
            <span class="material-icons text-xl w-6 flex justify-center group-hover:scale-110 transition-transform">dashboard</span>
            @if (!isSidebarCollapsed()) {
              <span class="font-semibold tracking-wide">Summary</span>
            }
          </a>
          
          <a routerLink="/claim-officer/requests" routerLinkActive="active-link"
             class="flex items-center p-3 rounded-lg transition-all duration-200 cursor-pointer gap-3 group text-slate-600 hover:bg-burgundy/5 hover:text-burgundy"
             (click)="closeSidebarOnMobile()">
            <span class="material-icons text-xl w-6 flex justify-center group-hover:scale-110 transition-transform">list_alt</span>
            @if (!isSidebarCollapsed()) {
              <span class="font-semibold tracking-wide">Requests</span>
            }
          </a>

          <div class="mt-auto pt-4 border-t border-slate-100">
            <a (click)="logout()"
               class="flex items-center p-3 rounded-lg text-red-600 hover:bg-red-50 transition-colors cursor-pointer gap-3 group">
              <span class="material-icons text-xl w-6 flex justify-center group-hover:scale-110 transition-transform">logout</span>
              @if (!isSidebarCollapsed()) {
                <span class="font-semibold tracking-wide">Logout</span>
              }
            </a>
          </div>
        </nav>
      </aside>

      <!-- Main Content Area -->
      <main class="flex-1 flex flex-col min-w-0 h-screen overflow-y-auto">
        <!-- Header -->
        <header class="bg-white border-b border-slate-200 shadow-sm flex items-center justify-between p-3 px-4 md:px-8 sticky top-0 z-40">
          <div class="flex items-center gap-3">
            <!-- Mobile menu button -->
            <button (click)="toggleSidebar()"
                    class="p-2 rounded-md hover:bg-burgundy/5 text-burgundy transition-colors focus:outline-none md:hidden">
              <span class="material-icons text-xl leading-none">menu</span>
            </button>
            <h1 class="text-base md:text-xl font-black text-burgundy tracking-tight uppercase">Claims Officer Dashboard</h1>
          </div>
          <div class="flex items-center gap-2 md:gap-4">
            <app-notification-bell></app-notification-bell>
            <span class="text-sm font-semibold text-slate-700 hidden sm:block">{{ currentUser()?.fullName }}</span>
            <div class="w-9 h-9 md:w-10 md:h-10 bg-burgundy text-white rounded-full flex items-center justify-center font-black shadow-inner ring-4 ring-pink/20 text-sm">
              {{ getUserInitials() }}
            </div>
          </div>
        </header>

        <!-- Content Area -->
        <div class="p-4 md:p-8 w-full animate-in fade-in slide-in-from-bottom-4 duration-500">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .active-link {
      background: #8B1A3A !important;
      color: white !important;
      box-shadow: 0 4px 6px -1px rgba(139, 26, 58, 0.2);
    }
  `]
})
export class ClaimOfficerComponent {
  private authService = inject(AuthService);
  currentUser = this.authService.currentUser;
  isSidebarCollapsed = signal(window.innerWidth < 768);

  toggleSidebar() {
    this.isSidebarCollapsed.set(!this.isSidebarCollapsed());
  }

  closeSidebarOnMobile() {
    if (window.innerWidth < 768) {
      this.isSidebarCollapsed.set(true);
    }
  }

  getUserInitials(): string {
    const user = this.currentUser();
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
