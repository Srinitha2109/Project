import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PolicyService, Policy } from '../../../../../services/policy';
import { PolicyApplicationService } from '../../../../../services/policy-application';
import { BusinessProfileService, BusinessProfile } from '../../../../../services/business-profile';
import { AuthService } from '../../../../../services/auth';
import { NotificationService } from '../../../../../services/notification';

@Component({
  selector: 'app-gl-application',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './gl-application.html',
  styleUrl: './gl-application.css'
})
export class GlApplicationComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private policyService = inject(PolicyService);
  private policyAppService = inject(PolicyApplicationService);
  private businessProfileService = inject(BusinessProfileService);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  currentStep = signal(1);
  policy = signal<Policy | null>(null);
  businessProfile = signal<BusinessProfile | null>(null);
  calculatedPremium = signal<number | null>(null);
  isSubmitting = signal(false);
  formSubmitted = signal(false);

  // Form Groups
  step1Form!: FormGroup;
  step2Form!: FormGroup;

  ngOnInit() {
    const policyId = this.route.snapshot.params['id'];
    if (policyId) {
      this.loadPolicy(policyId);
    }
    this.loadBusinessProfile();
    this.initForms();
  }

  private initForms() {
    this.step1Form = this.fb.group({
      selectedCoverageAmount: [0, [Validators.required, Validators.min(1)]],
      paymentPlan: ['', Validators.required]
    });

    this.step2Form = this.fb.group({
      // Premises
      customersVisitPremises: [false],
      dailyVisitorsRange: [''],
      hasParkingLot: [false],
      // Products
      sellPhysicalProducts: [false],
      productsContactFood: [false],
      productsForChildren: [false],
      // Work at Sites
      workAtCustomerSites: [false],
      workRemainsAfterCompletion: [false],
      useSubcontractors: [false],
      // Claims
      hasPriorClaims: [false],
      claimsCount: [0],
      totalClaimsAmount: [0],
      // Coverage Selection
      coverageType: ['Both Bodily Injury & Property Damage', Validators.required],
      perOccurrenceLimit: [1000000, Validators.required],
      includeProductsCompletedOps: [false],
      includeAdvertisingInjury: [false]
    });

    // Watch for step 1 changes to preview premium
    this.step1Form.valueChanges.subscribe(() => {
      this.previewPremium();
    });

    // Watch for step 2 changes to update premium based on risk factors
    this.step2Form.valueChanges.subscribe(() => {
      this.previewPremium();
    });
  }

  private loadPolicy(id: any) {
    this.policyService.getAllPolicies().subscribe((policies: Policy[]) => {
      const p = policies.find(x => x.id == id);
      if (p) {
        this.policy.set(p);
        this.step1Form.patchValue({
          selectedCoverageAmount: p.minCoverageAmount
        });
      }
    });
  }

  private loadBusinessProfile() {
    const user = this.authService.currentUser();
    if (user && user.id) {
      this.businessProfileService.getProfileByUserId(user.id).subscribe((profile: BusinessProfile) => {
        this.businessProfile.set(profile);
      });
    }
  }

  previewPremium() {
    const policy = this.policy();
    const profile = this.businessProfile();
    const amount = this.step1Form.value.selectedCoverageAmount;

    if (policy && profile && amount >= (policy.minCoverageAmount || 0) && amount <= (policy.maxCoverageAmount || 999999999)) {
      
      const glDetail = {
        ...this.step2Form.value
      };

      this.policyAppService.calculatePremiumPreview({
        planId: policy.id!,
        coverageAmount: amount,
        businessProfileId: profile.id!,
        paymentPlan: this.step1Form.value.paymentPlan,
        glDetail: glDetail
      }).subscribe({
        next: (premium: number) => {
          this.calculatedPremium.set(premium);
        },
        error: (err) => {
          console.error('Premium preview failed', err);
        }
      });
    } else {
      this.calculatedPremium.set(null);
    }
  }

  nextStep() {
    this.formSubmitted.set(true);
    if (this.currentStep() === 1 && this.step1Form.get('paymentPlan')?.valid) {
      this.currentStep.set(2);
      this.formSubmitted.set(false);
    } else if (this.currentStep() === 2 && this.step2Form.valid) {
      this.currentStep.set(3);
      this.formSubmitted.set(false);
    }
    window.scrollTo(0, 0);
  }

  prevStep() {
    this.formSubmitted.set(false);
    if (this.currentStep() > 1) {
      this.currentStep.set(this.currentStep() - 1);
    }
    window.scrollTo(0, 0);
  }

  /**
   * Logical Fix: In real insurance, the Aggregate Limit (Total Budget)
   * must exactly match the Coverage Amount selected in Step 1.
   */
  /**
   * Logical Update: Per Occurrence matched to Coverage. Aggregate is 2x Coverage.
   */
  get perOccurrenceLimit(): number {
    return this.step1Form.get('selectedCoverageAmount')?.value || 0;
  }

  submitApplication() {
    const user = this.authService.currentUser();
    const policy = this.policy();
    const profile = this.businessProfile();

    if (!user || !policy || !profile) return;

    this.isSubmitting.set(true);

    const glDetail = {
      ...this.step2Form.value
    };

    const request = {
      userId: user.id,
      planId: policy.id!,
      businessProfileId: profile.id!,
      selectedCoverageAmount: this.step1Form.value.selectedCoverageAmount,
      paymentPlan: this.step1Form.value.paymentPlan,
      glDetail: glDetail
    };

    this.policyAppService.createGeneralLiabilityApplication(request).subscribe({
      next: () => {
        this.notificationService.show('General Liability application submitted successfully!', 'success');
        this.router.navigate(['/policyholder/applications']);
      },
      error: (err) => {
        this.notificationService.show(err.error?.message || 'Failed to submit application', 'error');
        this.isSubmitting.set(false);
      }
    });
  }
}
