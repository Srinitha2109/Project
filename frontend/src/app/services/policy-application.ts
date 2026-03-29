import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PolicyApplication {
    id?: number;
    policyNumber?: string;
    userId: number;
    planId: number;
    businessProfileId?: number;
    agentId?: number;
    claimOfficerId?: number;
    selectedCoverageAmount: number;
    premiumAmount?: number;
    paymentPlan?: 'MONTHLY' | 'SIX_MONTHS' | 'ANNUALLY';
    nextPaymentDueDate?: string;
    startDate?: string;
    endDate?: string;
    status?: string;
    rejectionReason?: string;
    commissionAmount?: number;
    planName?: string;
    totalSettledAmount?: number;

    // Added for Agent Review
    businessName?: string;
    industry?: string;
    employeeCount?: number;
    annualRevenue?: number;
    glDetail?: GeneralLiabilityDetail;

    // Top-up tracking
    originalAggregateLimit?: number;
    currentAggregateLimit?: number;
    topUpCount?: number;
    totalTopUpAmount?: number;

    // Renewal tracking
    renewedFromPolicyId?: number;
    renewedToPolicyId?: number;
    renewalInitiated?: boolean;
    renewalReminderSentAt?: string;
    claims?: any[];
    topUps?: any[];
}

export interface PremiumRequest {
    planId: number;
    coverageAmount: number;
    businessProfileId: number;
    paymentPlan?: 'MONTHLY' | 'SIX_MONTHS' | 'ANNUALLY';
    glDetail?: GeneralLiabilityDetail;
}

export interface GeneralLiabilityDetail {
    customersVisitPremises: boolean;
    dailyVisitorsRange?: string;
    hasParkingLot?: boolean;
    sellPhysicalProducts: boolean;
    productsContactFood?: boolean;
    productsForChildren?: boolean;
    workAtCustomerSites: boolean;
    workRemainsAfterCompletion?: boolean;
    useSubcontractors?: boolean;
    hasPriorClaims: boolean;
    claimsCount?: number;
    totalClaimsAmount?: number;
    coverageType: string;
    perOccurrenceLimit: number;
    aggregateLimit: number;
    includeProductsCompletedOps: boolean;
    includeAdvertisingInjury: boolean;
}

export interface GeneralLiabilityApplicationRequest {
    userId: number;
    planId: number;
    businessProfileId: number;
    selectedCoverageAmount: number;
    paymentPlan: 'MONTHLY' | 'SIX_MONTHS' | 'ANNUALLY';
    glDetail: GeneralLiabilityDetail;
}

@Injectable({
    providedIn: 'root'
})
export class PolicyApplicationService {
    private http = inject(HttpClient);
    private apiUrl = '/api/policy-applications';

    createApplication(application: PolicyApplication): Observable<PolicyApplication> {
        return this.http.post<PolicyApplication>(this.apiUrl, application);
    }

    createGeneralLiabilityApplication(application: GeneralLiabilityApplicationRequest): Observable<PolicyApplication> {
        return this.http.post<PolicyApplication>(`${this.apiUrl}/general-liability`, application);
    }

    getAllApplications(): Observable<PolicyApplication[]> {
        return this.http.get<PolicyApplication[]>(`${this.apiUrl}/all`);
    }

    getApplicationById(id: number): Observable<PolicyApplication> {
        return this.http.get<PolicyApplication>(`${this.apiUrl}/${id}`);
    }

    getApplicationsByUserId(userId: number): Observable<PolicyApplication[]> {
        return this.http.get<PolicyApplication[]>(`${this.apiUrl}/user/${userId}`);
    }

    getApplicationsByAgentId(agentId: number): Observable<PolicyApplication[]> {
        return this.http.get<PolicyApplication[]>(`${this.apiUrl}/agent/${agentId}`);
    }

    calculatePremiumPreview(request: PremiumRequest): Observable<number> {
        return this.http.post<number>(`${this.apiUrl}/calculate-premium`, request);
    }

    assignStaff(id: number, agentId: number, claimOfficerId: number): Observable<PolicyApplication> {
        return this.http.put<PolicyApplication>(`${this.apiUrl}/${id}/assign-staff?agentId=${agentId}&claimOfficerId=${claimOfficerId}`, {});
    }


    activatePolicy(id: number): Observable<PolicyApplication> {
        return this.http.put<PolicyApplication>(`${this.apiUrl}/${id}/activate`, {});
    }

    approveApplication(id: number): Observable<PolicyApplication> {
        return this.http.put<PolicyApplication>(`${this.apiUrl}/${id}/approve`, {});
    }

    rejectApplication(id: number, reason: string): Observable<PolicyApplication> {
        return this.http.put<PolicyApplication>(`${this.apiUrl}/${id}/reject?reason=${reason}`, {});
    }

    renewPolicy(id: number): Observable<PolicyApplication> {
        return this.http.post<PolicyApplication>(`${this.apiUrl}/${id}/renew`, {});
    }
}
