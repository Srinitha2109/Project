import { Component, inject, signal, OnInit, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PolicyService, Policy } from '../../../../services/policy';
import { AuthService } from '../../../../services/auth';
import { BusinessProfileService, BusinessProfile } from '../../../../services/business-profile';
import { PolicyApplicationService } from '../../../../services/policy-application';
import { NotificationService } from '../../../../services/notification';
import { Router } from '@angular/router';
import { PolicySearchService } from '../../../../services/policy-search';
import { PolicyTopUpService } from '../../../../services/policy-topup';
import { AIService } from '../../../../services/ai';
import { HttpClientModule } from '@angular/common/http';
import { ViewChild, ElementRef } from '@angular/core';

@Component({
    selector: 'app-policyholder-policies',
    standalone: true,
    imports: [FormsModule, CommonModule],
    templateUrl: './policies.html',
    styleUrl: './policies.css'
})
export class PoliciesComponent implements OnInit {
    private policyService = inject(PolicyService);
    private authService = inject(AuthService);
    private businessProfileService = inject(BusinessProfileService);
    private policyAppService = inject(PolicyApplicationService);
    private notificationService = inject(NotificationService);
    private router = inject(Router);
    private policySearchService = inject(PolicySearchService);
    private topUpService = inject(PolicyTopUpService);
    private aiService = inject(AIService);

    @ViewChild('chatScroll') private chatScroll: ElementRef | undefined;

    // AI Chatbot State
    isChatOpen = signal(false);
    chatMessages = signal<{ role: 'user' | 'ai', text: string }[]>([
        { role: 'ai', text: 'Hi! I am Fortify AI. Describe your business or what you need to protect, and I will recommend the perfect policy for you!' }
    ]);
    currentChatInput = signal('');
    isAiLoading = signal(false);

    toggleChat() {
        this.isChatOpen.set(!this.isChatOpen());
        if (this.isChatOpen()) {
            setTimeout(() => this.scrollToBottom(), 100);
        }
    }

    sendChatMessage() {
        const text = this.currentChatInput().trim();
        if (!text || this.isAiLoading()) return;

        // Add user message
        this.chatMessages.update(msgs => [...msgs, { role: 'user', text }]);
        this.currentChatInput.set('');
        this.isAiLoading.set(true);
        setTimeout(() => this.scrollToBottom(), 50);

        this.aiService.recommendPolicy(text).subscribe({
            next: (res) => {
                this.chatMessages.update(msgs => [...msgs, { role: 'ai', text: res.response }]);
                this.isAiLoading.set(false);
                setTimeout(() => this.scrollToBottom(), 100);
            },
            error: () => {
                this.chatMessages.update(msgs => [...msgs, { role: 'ai', text: 'Sorry, I am having trouble connecting. Please try again later.' }]);
                this.isAiLoading.set(false);
            }
        });
    }

    private scrollToBottom(): void {
        try {
            if (this.chatScroll) {
                this.chatScroll.nativeElement.scrollTop = this.chatScroll.nativeElement.scrollHeight;
            }
        } catch (err) { }
    }


    // Top-up Modal State
    showTopUpModal = signal(false);
    selectedAppForTopUp = signal<any>(null);
    topUpAmount = signal<number>(100000);
    isTopUpSubmitting = signal(false);

    allPolicies = signal<Policy[]>([]);
    userApplications = signal<any[]>([]);
    isLoading = signal(true);

    // Enhanced fuzzy match: handles sub-strings, word starts, and small spelling errors
    private fuzzyMatch(query: string, target: string): boolean {
        if (!query) return true;
        if (!target) return false;
        const q = query.toLowerCase();
        const t = target.toLowerCase();

        // 1. Exact substring match
        if (t.includes(q)) return true;

        // 2. Term-based matching (matches starts of words)
        const words = t.split(/[\s_-]+/);
        if (words.some(word => word.startsWith(q))) return true;

        // 3. Levenshtein distance for spelling errors
        if (q.length > 2) {
            for (const word of words) {
                if (this.getLevenshteinDistance(q, word) <= 1) return true;
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
                    matrix[i - 1][j] + 1,
                    matrix[i][j - 1] + 1,
                    matrix[i - 1][j - 1] + cost
                );
            }
        }
        return matrix[a.length][b.length];
    }

    businessProfile = signal<BusinessProfile | null>(null);
    selectedPolicyForApp = signal<Policy | null>(null);
    calculatedPremium = signal<number | null>(null);
    selectedCoverageAmount = signal<number>(0);
    selectedPaymentPlan = signal<'MONTHLY' | 'SIX_MONTHS' | 'ANNUALLY'>('MONTHLY');
    formSubmitted = signal(false);

    // Pagination
    currentPage = signal(1);
    pageSize = signal(6);

    constructor() {
        // Reset to page 1 whenever the search query changes
        effect(() => {
            this.policySearchService.searchQuery();
            this.currentPage.set(1);
        });
    }

    //runs initially when page loads
    ngOnInit() {
        this.loadPolicies();
        this.loadBusinessProfile();
        this.loadUserApplications();
    }

    loadPolicies() {
        this.policyService.getAllPolicies().subscribe({
            next: (policies) => {
                setTimeout(() => {
                    this.allPolicies.set(policies.filter(p => p.isActive));
                    this.isLoading.set(false);
                }, 2000)
            },
            error: () => {
                this.isLoading.set(false);
            }
        });
    }

    //computed is used becuase it dynamically filter and update the ui whenever search query changes
    filteredPolicies = computed(() => {
        const query = this.policySearchService.searchQuery().toLowerCase().trim(); //returns current search text
        let filtered = this.allPolicies();
        if (query) {
            //keep items that match the condition
            filtered = filtered.filter(p => {
                const searchableFields = [
                    p.policyName,
                    p.policyNumber,
                    p.insuranceType,
                    p.insuranceTypeDisplayName,
                    p.description
                ];
                //if atleast one field match then keep that policy
                return searchableFields.some(field => this.fuzzyMatch(query, field || ''));
            });
        }
        return filtered;
    });

    paginatedPolicies = computed(() => {
        const startIndex = (this.currentPage() - 1) * this.pageSize();
        return this.filteredPolicies().slice(startIndex, startIndex + this.pageSize());
    });
    totalPages = computed(() => Math.ceil(this.filteredPolicies().length / this.pageSize()));

    changePage(page: number) {
        if (page >= 1 && page <= this.totalPages()) {
            this.currentPage.set(page);
        }
    }

    //fetch users business identity
    loadBusinessProfile() {
        const user = this.authService.currentUser();
        console.log(' loadBusinessProfile:', user);
        if (user && user.id) {
            this.businessProfileService.getProfileByUserId(user.id).subscribe({
                next: (profile) => {
                    // console.log('Business profile loaded:', profile);
                    // console.log('Profile agentId:', profile?.agentId);
                    this.businessProfile.set(profile);
                },
                error: (err) => {
                    console.error('Failed to load business profile', err);
                }
            });
        } else {
            console.error(' No user found for loading business profile');
        }
    }

    loadUserApplications() {
        const user = this.authService.currentUser();
        //user is logged in and valid
        if (user && user.id) {
            this.policyAppService.getApplicationsByUserId(user.id).subscribe({
                next: (apps) => this.userApplications.set(apps),
                error: (err) => console.error('Failed to load user applications', err)
            });
        }
    }

    // Checks if the user has already applied for that policy if applied it return status or apply button 
    getPolicyStatus(policyId: number | undefined): string | null {
        if (!policyId) return null;
        const app = this.userApplications().find(a => a.planId === policyId);
        return app ? app.status : null;
    }

    selectPolicy(policy: Policy) {
        const profile = this.businessProfile();
        if (!profile) { //checks if user has a business profile
            this.notificationService.show(
                'Your business profile is not set up yet. Please contact the admin.',
                'warning'
            );
            return;
        }

        if (!profile.agentId) {
            this.notificationService.show(
                'Your account is pending professional staff assignment. Admin is currently assigning an agent to your profile. Please check back later.',
                'info'
            );
            return;
        }

        if (policy.insuranceType === 'GENERAL_LIABILITY') {
            this.router.navigate(['/policyholder/policies/gl-application', policy.id]);
            return;
        }

        this.selectedPolicyForApp.set(policy);
        this.selectedCoverageAmount.set(policy.minCoverageAmount);
        this.previewPremium();
    }

    coverageError = computed(() => {
        const amount = this.selectedCoverageAmount();
        const policy = this.selectedPolicyForApp();
        if (!policy) return null;
        if (policy.minCoverageAmount != null && amount < policy.minCoverageAmount) {
            return `Coverage amount must be at least ${policy.minCoverageAmount}`;
        }
        if (policy.maxCoverageAmount != null && amount > policy.maxCoverageAmount) {
            return `Coverage amount cannot exceed ${policy.maxCoverageAmount}`;
        }
        return null;
    });

    closeModal() {
        this.selectedPolicyForApp.set(null);
        this.calculatedPremium.set(null);
        this.selectedPaymentPlan.set('MONTHLY');
        this.formSubmitted.set(false);
    }

    previewPremium() {
        //policy user has selected
        const policy = this.selectedPolicyForApp();
        const profile = this.businessProfile();
        if (policy && profile && profile.id) {
            const amount = this.selectedCoverageAmount();
            if (
                (policy.minCoverageAmount != null && amount < policy.minCoverageAmount) ||
                (policy.maxCoverageAmount != null && amount > policy.maxCoverageAmount)
            ) {
                this.calculatedPremium.set(null); // Clear premium to disable submit
                return;
            }

            this.policyAppService.calculatePremiumPreview({
                planId: policy.id!,
                coverageAmount: this.selectedCoverageAmount(),
                businessProfileId: profile.id,
                paymentPlan: this.selectedPaymentPlan()
            }).subscribe({
                next: (premium) => {
                    this.calculatedPremium.set(premium);
                }
                // error: () => {
                //     this.notificationService.show('Failed to calculate premium', 'error');
                // }
            });
        }
    }

    submitApplication() {
        const policy = this.selectedPolicyForApp();
        const profile = this.businessProfile();
        const user = this.authService.currentUser();

        if (policy && profile && user) {
            this.formSubmitted.set(true);
            const amount = this.selectedCoverageAmount();
            if (policy.minCoverageAmount != null && amount < policy.minCoverageAmount) {
                this.notificationService.show(`Coverage amount must be at least ${policy.minCoverageAmount}`, 'error');
                return;
            }
            if (policy.maxCoverageAmount != null && amount > policy.maxCoverageAmount) {
                this.notificationService.show(`Coverage amount cannot exceed ${policy.maxCoverageAmount}`, 'error');
                return;
            }
            // if (this.coverageError()) {
            //     this.notificationService.show(this.coverageError()!, 'error');
            //     return;
            // }

            this.policyAppService.createApplication({
                userId: user.id,
                planId: policy.id!,
                businessProfileId: profile.id!,
                selectedCoverageAmount: this.selectedCoverageAmount(),
                paymentPlan: this.selectedPaymentPlan()
            }).subscribe({
                next: () => {
                    this.notificationService.show('Application submitted! Agent will review and approve your policy shortly.', 'success');
                    this.loadUserApplications();
                    this.closeModal();
                },
                error: (err) => {
                    this.notificationService.show(err.error?.message || 'Failed to submit application', 'error');
                }
            });
        }
    }

    // Top-up and Renewal Logic
    getUsageStatus(app: any) {
        if (!app.totalSettledAmount || !app.currentAggregateLimit) return { percent: 0, color: 'emerald' };
        const percent = (app.totalSettledAmount / app.currentAggregateLimit) * 100;
        if (percent >= 100) return { percent: 100, color: 'rose' };
        if (percent >= 90) return { percent, color: 'orange' };
        if (percent >= 75) return { percent, color: 'amber' };
        return { percent, color: 'emerald' };
    }

    openTopUpModal(app: any) {
        this.selectedAppForTopUp.set(app);
        this.showTopUpModal.set(true);
    }

    closeTopUpModal() {
        this.showTopUpModal.set(false);
        this.selectedAppForTopUp.set(null);
        this.isTopUpSubmitting.set(false);
    }

    submitTopUp() {
        const app = this.selectedAppForTopUp();
        const user = this.authService.currentUser();
        if (app && user && this.topUpAmount() > 0) {
            this.isTopUpSubmitting.set(true);
            this.topUpService.requestTopUp(app.id, this.topUpAmount(), user.id).subscribe({
                next: () => {
                    this.notificationService.show('Top-up request submitted! Agent will approve it shortly.', 'success');
                    this.closeTopUpModal();
                },
                error: (err) => {
                    this.notificationService.show(err.error?.message || 'Failed to submit top-up request', 'error');
                    this.isTopUpSubmitting.set(false);
                }
            });
        }
    }

    renewPolicy(app: any) {
        if (confirm('Are you sure you want to initiate renewal for this policy? The new policy will start after your current policy ends.')) {
            this.policyAppService.renewPolicy(app.id).subscribe({
                next: () => {
                    this.notificationService.show('Renewal initiated! You can track it in My Applications.', 'success');
                    this.loadUserApplications();
                },
                error: (err) => {
                    this.notificationService.show(err.error?.message || 'Failed to initiate renewal', 'error');
                }
            });
        }
    }

    getRemainingBalance(app: any) {
        return (app.currentAggregateLimit || app.selectedCoverageAmount) - (app.totalSettledAmount || 0);
    }
}
