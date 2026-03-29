import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PolicyApplicationService, PolicyApplication } from '../../../../../services/policy-application';
import { PolicyTopUpService, PolicyTopUp } from '../../../../../services/policy-topup';
import { AuthService } from '../../../../../services/auth';
import { NotificationService } from '../../../../../services/notification';

@Component({
  selector: 'app-agent-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './requests.html',
  styleUrl: './requests.css'
})
export class RequestsComponent implements OnInit {
  private policyAppService = inject(PolicyApplicationService);
  private topupService = inject(PolicyTopUpService);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  requests = signal<PolicyApplication[]>([]);
  topupRequests = signal<PolicyTopUp[]>([]);
  selectedReq = signal<PolicyApplication | null>(null);
  selectedTopUp = signal<PolicyTopUp | null>(null);
  isRejecting = signal(false);
  rejectionReason = '';

  pendingCount = computed(() =>
    this.requests().filter(r => r.status === 'SUBMITTED' || r.status === 'UNDER_REVIEW').length +
    this.topupRequests().length
  );

  ngOnInit() {
    this.loadRequests();
  }

  loadRequests() {
    const user = this.authService.currentUser();
    if (user && user.id) {
      // Load policy applications
      this.policyAppService.getApplicationsByAgentId(user.id).subscribe({
        next: (apps) => this.requests.set(apps),
        error: () => this.notificationService.show('Failed to load application requests', 'error')
      });

      // Load top-up requests
      this.topupService.getPendingTopUpsForAgent(user.id).subscribe({
        next: (topups) => this.topupRequests.set(topups),
        error: () => this.notificationService.show('Failed to load top-up requests', 'error')
      });
    }
  }

  openReview(req: PolicyApplication) {
    this.selectedReq.set(req);
    this.selectedTopUp.set(null);
    this.isRejecting.set(false);
    this.rejectionReason = '';
  }

  openTopUpReview(topup: PolicyTopUp) {
    this.selectedTopUp.set(topup);
    this.selectedReq.set(null);
    this.isRejecting.set(false);
    this.rejectionReason = '';
  }

  closeReview() {
    this.selectedReq.set(null);
    this.selectedTopUp.set(null);
  }

  approve() {
    const req = this.selectedReq();
    const topup = this.selectedTopUp();
    const user = this.authService.currentUser();

    if (req && req.id) {
      this.policyAppService.approveApplication(req.id).subscribe({
        next: () => {
          this.notificationService.show('Application approved successfully!', 'success');
          this.loadRequests();
          this.closeReview();
        },
        error: (err) => this.notificationService.show(err.error?.message || 'Approval failed', 'error')
      });
    } else if (topup && topup.id && user?.id) {
      this.topupService.approveTopUp(topup.id, user.id).subscribe({
        next: () => {
          this.notificationService.show('Top-up approved successfully!', 'success');
          this.loadRequests();
          this.closeReview();
        },
        error: (err) => this.notificationService.show(err.error?.message || 'Approval failed', 'error')
      });
    }
  }

  confirmReject() {
    const req = this.selectedReq();
    const topup = this.selectedTopUp();

    if (this.rejectionReason) {
      if (req && req.id) {
        this.policyAppService.rejectApplication(req.id, this.rejectionReason).subscribe({
          next: () => {
            this.notificationService.show('Application rejected.', 'info');
            this.loadRequests();
            this.closeReview();
          },
          error: () => this.notificationService.show('Rejection failed', 'error')
        });
      } else if (topup && topup.id) {
        this.topupService.rejectTopUp(topup.id, this.rejectionReason).subscribe({
          next: () => {
            this.notificationService.show('Top-up rejected.', 'info');
            this.loadRequests();
            this.closeReview();
          },
          error: () => this.notificationService.show('Rejection failed', 'error')
        });
      }
    }
  }
}
