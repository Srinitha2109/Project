import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { NotificationService } from './notification';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private notificationService = inject(NotificationService);
  private apiUrl = '/api'; //proxy configuration is used to eliminate cors issue 


  currentUser = signal<any>(null);

  //checks if user exists in local storage and keeps user logged in after refresh
  constructor() {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      try {
        this.currentUser.set(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('user');
      }
    }
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/request-registration`, userData);
  }

  forgotPassword(data: { email: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/forgot-password`, data);
  }

  resetPassword(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/reset-password`, data);
  }

  login(credentials: any) {
    this.http.post(`${this.apiUrl}/auth/login`, credentials).subscribe({
      next: (res: any) => {
        if (res && res.userId) {
          //if backend sends userId instead of id 
          res.id = res.userId;
        }
        this.currentUser.set(res);
        if (res.token) {
          localStorage.setItem('token', res.token);
        }
        // Persist user data even after page refresh
        localStorage.setItem('user', JSON.stringify(res));
        console.log('Login Response:', res);

        this.notificationService.show('Logged in successfully', 'success');
        //role based navigation
        const role = credentials.role;
        switch (role) {
          case 'ADMIN': this.router.navigate(['/admin/dashboard']); break;
          case 'POLICYHOLDER': this.router.navigate(['/policyholder/dashboard']); break;
          case 'AGENT': this.router.navigate(['/agent/dashboard']); break;
          case 'CLAIM_OFFICER': this.router.navigate(['/claim-officer/dashboard']); break;
          default: this.router.navigate(['/landing']);
        }
      },
      error: (err) => {
        console.error('Login error:', err);
        this.notificationService.show(err.error?.message || 'Login failed. Please check credentials.', 'error');
      }
    });
  }

  logout() {
    //clear user state
    this.currentUser.set(null);
    //remove stored data
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    //navigate to landing page
    this.router.navigate(['/landing']);
  }
}
