import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PolicyTopUp {
    id: string;
    policyApplicationId: number;
    policyNumber?: string;
    requestedBy: number;
    requestedByUserName?: string;
    topUpAmount: number;
    additionalPremium: number;
    status: 'PENDING_PAYMENT' | 'REQUESTED' | 'UNDER_REVIEW' | 'AGENT_APPROVED' | 'APPROVED' | 'REJECTED';
    planId?: number;
    totalCoverage?: number;
    policyStatus?: string;
    paymentPlan?: string;
    approvedBy?: number;
    businessName?: string;
    currentPremium?: number;
    expectedNewPremium?: number;
    rejectionReason?: string;
    requestedAt?: string;
    approvedAt?: string;
}

@Injectable({
    providedIn: 'root'
})
export class PolicyTopUpService {
    private http = inject(HttpClient);
    private apiUrl = '/api/policy-topups';

    requestTopUp(policyId: number, amount: number, userId: number): Observable<PolicyTopUp> {
        return this.http.post<PolicyTopUp>(`${this.apiUrl}/request?policyId=${policyId}&amount=${amount}&userId=${userId}`, {});
    }

    approveTopUp(topUpId: string, adminUserId: number): Observable<PolicyTopUp> {
        return this.http.post<PolicyTopUp>(`${this.apiUrl}/${topUpId}/approve?adminUserId=${adminUserId}`, {});
    }

    confirmPayment(topUpId: string): Observable<PolicyTopUp> {
        return this.http.post<PolicyTopUp>(`${this.apiUrl}/${topUpId}/confirm-payment`, {});
    }

    rejectTopUp(topUpId: string, reason: string): Observable<PolicyTopUp> {
        return this.http.post<PolicyTopUp>(`${this.apiUrl}/${topUpId}/reject?reason=${reason}`, {});
    }

    getTopUpsByPolicy(policyId: number): Observable<PolicyTopUp[]> {
        return this.http.get<PolicyTopUp[]>(`${this.apiUrl}/policy/${policyId}`);
    }

    getPendingTopUpsForAgent(agentUserId: number): Observable<PolicyTopUp[]> {
        return this.http.get<PolicyTopUp[]>(`${this.apiUrl}/agent/${agentUserId}/pending`);
    }
}
