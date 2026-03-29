import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PolicyApplicationService, PolicyApplication } from '../../../../services/policy-application';
import { PaymentService } from '../../../../services/payment';
import { AuthService } from '../../../../services/auth';
import { NotificationService } from '../../../../services/notification';
import { ClaimService, Claim } from '../../../../services/claim';
import { PolicyTopUpService } from '../../../../services/policy-topup';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-policyholder-applications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './applications.html',
  styleUrl: './applications.css'
})
export class ApplicationsComponent implements OnInit {
  private policyAppService = inject(PolicyApplicationService);
  private authService = inject(AuthService);
  private paymentService = inject(PaymentService);
  private notificationService = inject(NotificationService);
  private claimService = inject(ClaimService);
  private topUpService = inject(PolicyTopUpService);
  private http = inject(HttpClient);

  applications = signal<PolicyApplication[]>([]);
  isLoading = signal(true);
  isSubmitting = signal(false);
  formSubmitted = signal(false);

  // Claim Modal State
  showClaimModal = signal(false);
  selectedApp = signal<PolicyApplication | null>(null);

  // Top-Up Modal State
  showTopUpModal = signal(false);
  selectedAppForTopUp = signal<PolicyApplication | null>(null);
  topUpAmount = signal<number>(0);
  isTopUpSubmitting = signal(false);

  // 3-Step Wizard State
  currentStep = signal<1 | 2 | 3>(1);
  selectedSubtype = signal<'Bodily Injury' | 'Property Damage' | null>(null);
  uploadedDocs = new Map<string, File>();
  requiredDocs = signal<string[]>([]);
  ocrErrors = signal<string[]>([]);

  ocrLoading = signal(false);
  ocrCompleted = signal(false);
  claimForm = {
    incidentDate: '',
    incidentLocation: '',
    description: '',
    claimAmount: 0
  };

  todayDate = signal<string>(new Date().toISOString().split('T')[0]);

  ngOnInit() {
    this.loadApplications();
  }

  loadApplications() {
    const user = this.authService.currentUser();
    if (user && user.id) {
      this.policyAppService.getApplicationsByUserId(user.id).subscribe({
        next: (apps) => {
          this.applications.set(apps);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      });
    }
  }

  getUsageStatus(app: any) {
    const totalSettled = app.totalSettledAmount || 0;
    const limit = app.currentAggregateLimit || app.selectedCoverageAmount || 1;
    const percent = Math.min((totalSettled / limit) * 100, 100);

    let textClass = 'text-emerald-500';
    let bgClass = 'bg-emerald-500';

    if (percent >= 90) {
      textClass = 'text-red-500';
      bgClass = 'bg-red-500';
    } else if (percent >= 75) {
      textClass = 'text-yellow-500';
      bgClass = 'bg-yellow-500';
    }

    return { percent, textClass, bgClass };
  }

  canTopUp(app: any): boolean {
    const status = this.getUsageStatus(app);
    const hasActiveTopUp = app.topUps && app.topUps.some((t: any) => t.status !== 'REJECTED');
    return status.percent >= 75 && status.percent < 100 && !hasActiveTopUp;
  }

  hasPendingClaim(app: any): boolean {
    if (!app || !app.claims) return false;
    return app.claims.some((c: any) =>
      c.status === 'SUBMITTED' ||
      c.status === 'ASSIGNED' ||
      c.status === 'UNDER_INVESTIGATION'
    );
  }

  getRemainingBalance(app: any) {
    if (!app) return 0;
    const limit = app.currentAggregateLimit || app.selectedCoverageAmount || 0;
    const settled = app.totalSettledAmount || 0;
    const rem = limit - settled;
    return rem > 0 ? rem : 0;
  }

  getAvailableBalance(app: any) {
    return this.getRemainingBalance(app);
  }

  getStatusBg(status: string | undefined): string {
    switch (status) {
      case 'ACTIVE': return 'bg-burgundy';
      case 'APPROVED': return 'bg-burgundy/100';
      case 'REJECTED': return 'bg-rose-600';
      case 'SUBMITTED': return 'bg-burgundy/40';
      case 'UNDER_REVIEW': return 'bg-burgundy/60';
      default: return 'bg-slate-400';
    }
  }

  isPaymentRequired(app: PolicyApplication): boolean {
    if (!app.status) return false;
    if (app.status === 'APPROVED') return true;
    if (app.status === 'ACTIVE') {
      if (!app.nextPaymentDueDate) return false;
      const dueDate = new Date(app.nextPaymentDueDate);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      dueDate.setHours(0, 0, 0, 0);
      return today >= dueDate;
    }
    return false;
  }

  isClaimAmountValid(): boolean {
    const app = this.selectedApp();
    if (!app) return true;
    const balance = this.getAvailableBalance(app);
    return this.claimForm.claimAmount <= balance;
  }

  payPremium(app: PolicyApplication) {
    if (!app.id || !app.premiumAmount) return;

    this.paymentService.createPayment({
      policyApplicationId: app.id,
      amount: app.premiumAmount,
      paymentMethod: 'CARD',
      paymentType: 'PREMIUM'
    }).subscribe({
      next: () => {
        this.notificationService.show('Payment Done! Your policy is now ACTIVE.', 'success');
        this.loadApplications();
      },
      error: () => this.notificationService.show('Payment failed. Please try again.', 'error')
    });
  }

  openClaimModal(app: PolicyApplication) {
    this.selectedApp.set(app);
    this.showClaimModal.set(true);
    const today = new Date().toISOString().split('T')[0];
    this.todayDate.set(today);
    this.claimForm = {
      incidentDate: today,
      incidentLocation: '',
      description: '',
      claimAmount: 0
    };
    this.uploadedDocs.clear();
    this.ocrLoading.set(false);
    this.ocrCompleted.set(false);
    this.formSubmitted.set(false);
    this.currentStep.set(1);
    this.selectedSubtype.set(null);
    this.requiredDocs.set([]);
    this.ocrErrors.set([]);
  }

  closeClaimModal() {
    this.showClaimModal.set(false);
    this.selectedApp.set(null);
    this.currentStep.set(1);
  }

  openTopUpModal(app: PolicyApplication) {
    this.selectedAppForTopUp.set(app);
    this.topUpAmount.set(0);
    this.showTopUpModal.set(true);
  }

  closeTopUpModal() {
    this.showTopUpModal.set(false);
    this.selectedAppForTopUp.set(null);
  }

  submitTopUp() {
    const app = this.selectedAppForTopUp();
    const amount = this.topUpAmount();

    const user = this.authService.currentUser();
    if (!app || !app.id || amount <= 0 || !user || !user.id) {
      this.notificationService.show('Please enter a valid amount', 'error');
      return;
    }

    this.isTopUpSubmitting.set(true);
    this.topUpService.requestTopUp(app.id, amount, user.id).subscribe({
      next: () => {
        this.notificationService.show('Top-up request sent to your agent for approval!', 'success');
        this.isTopUpSubmitting.set(false);
        this.closeTopUpModal();
        this.loadApplications();
      },
      error: () => {
        this.notificationService.show('Failed to send top-up request.', 'error');
        this.isTopUpSubmitting.set(false);
      }
    });
  }

  renewPolicy(app: PolicyApplication) {
    if (!app.id) return;

    if (confirm('Are you sure you want to renew this policy? A new application will be created starting after your current policy ends.')) {
      this.policyAppService.renewPolicy(app.id).subscribe({
        next: () => {
          this.notificationService.show('Renewal application created! Please review it in your list.', 'success');
          this.loadApplications();
        },
        error: () => this.notificationService.show('Failed to initiate renewal.', 'error')
      });
    }
  }

  isGeneralLiability(): boolean {
    const name = this.selectedApp()?.planName?.toLowerCase() || '';
    return name.includes('liability');
  }

  selectSubtype(subtype: 'Bodily Injury' | 'Property Damage') {
    this.selectedSubtype.set(subtype);
  }

  goToStep2() {
    const app = this.selectedApp();
    if (!app) return;

    let plan = app.planName?.toLowerCase() || '';
    let docs: string[] = [];

    if (plan.includes('liability')) {
      if (this.selectedSubtype() === 'Bodily Injury') {
        docs = ['Incident Report', 'Medical Report', 'Hospital Bill'];
      } else {
        docs = ['Incident Report', 'Property Damage Assessment Report'];
      }
    } else if (plan.includes('auto')) {
      docs = ['FIR / Police Report', 'Vehicle Damage Assessment Report', 'Repair Invoice'];
    } else if (plan.includes('worker') || plan.includes('comp')) {
      docs = ['Incident Report', 'Medical Report', 'Hospital Bill'];
    } else {
      docs = ['Incident Report', 'Supporting Evidence'];
    }

    this.requiredDocs.set(docs);
    this.currentStep.set(2);
  }

  goToStep1() {
    this.currentStep.set(1);
  }

  onSpecificFileSelected(event: any, docType: string) {
    if (event.target.files && event.target.files.length > 0) {
      this.uploadedDocs.set(docType, event.target.files[0]);
    }
  }

  removeFile(docType: string) {
    this.uploadedDocs.delete(docType);
  }

  canProceedToStep3(): boolean {
    const req = this.requiredDocs();
    if (req.length === 0) return false;
    for (const d of req) {
      if (!this.uploadedDocs.has(d)) return false;
    }
    return true;
  }

  extractDataViaOCR() {
    if (!this.canProceedToStep3()) {
      this.notificationService.show('Please upload all required documents', 'error');
      return;
    }

    this.currentStep.set(3);
    this.ocrLoading.set(true);
    this.ocrErrors.set([]);

    const formData = new FormData();
    const planName = this.selectedApp()?.planName || 'General';
    let fullClaimType = planName;
    if (this.selectedSubtype()) {
      fullClaimType += ' - ' + this.selectedSubtype();
    }
    formData.append('claimType', fullClaimType);

    this.uploadedDocs.forEach((file, key) => {
      formData.append('documents', file);
    });

    this.http.post<any>('/api/ocr/extract', formData).subscribe({
      next: (response) => {
        this.ocrLoading.set(false);
        this.ocrCompleted.set(true);

        if (response.extractedDate) this.claimForm.incidentDate = response.extractedDate;
        if (response.extractedLocation) this.claimForm.incidentLocation = response.extractedLocation;
        let desc = response.extractedDamageDesc || response.extractedInjuryType || 'Extracted via AI Core';
        if (response.extractedPersonName) desc += ', Person: ' + response.extractedPersonName;
        if (response.extractedRepairCost && !response.extractedAmount) response.extractedAmount = response.extractedRepairCost;

        let amountText = String(response.extractedAmount || response.total_medical_cost || response.total_amount || response.extractedTotalAmount || '0');
        let parsedAmt = parseFloat(amountText.replace(/[^0-9.]/g, ''));

        this.claimForm.description = desc;
        if (parsedAmt > 0) this.claimForm.claimAmount = parsedAmt;

        this.notificationService.show('Data successfully extracted from documents!', 'success');
      },
      error: (err) => {
        this.ocrLoading.set(false);
        this.ocrCompleted.set(true);
        let failingDoc = this.requiredDocs()[0] || 'INCIDENT_REPORT';
        this.ocrErrors.set([failingDoc]);
        this.notificationService.show('OCR failed for some documents. You can still enter data manually.', 'error');
      }
    });
  }

  submitClaim() {
    const app = this.selectedApp();
    if (!app || !app.id) return;

    if (!this.claimForm.incidentDate || !this.claimForm.incidentLocation || !this.claimForm.description || this.claimForm.claimAmount <= 0) {
      this.formSubmitted.set(true);
      this.notificationService.show('Please fill all required fields correctly.', 'error');
      return;
    }

    this.isSubmitting.set(true);

    const claimData: Claim = {
      policyApplicationId: app.id,
      description: this.claimForm.description,
      claimAmount: this.claimForm.claimAmount,
      incidentDate: this.claimForm.incidentDate,
      incidentLocation: this.claimForm.incidentLocation
    };

    const allFiles = Array.from(this.uploadedDocs.values());
    this.claimService.createClaim(claimData, allFiles).subscribe({
      next: () => {
        this.notificationService.show('Claim submitted successfully! Our officer will contact you.', 'success');
        this.isSubmitting.set(false);
        this.closeClaimModal();
        this.loadApplications();
      },
      error: (err) => {
        console.error('Claim error:', err);
        this.notificationService.show('Failed to submit claim. Please try again.', 'error');
        this.isSubmitting.set(false);
      }
    });
  }
}
