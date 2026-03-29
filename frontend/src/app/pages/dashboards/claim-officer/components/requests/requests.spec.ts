import { ComponentFixture, TestBed,  } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { RequestsComponent } from './requests';
import { AuthService } from '../../../../../services/auth';

describe('ClaimOfficer RequestsComponent', () => {
    let component: RequestsComponent;
    let fixture: ComponentFixture<RequestsComponent>;
    let authService: any;
    let httpMock: HttpTestingController;

    const mockUser = { id: 10, fullName: 'Officer 1' };
    const mockOfficer = { id: 5, userId: 10 };
    const mockClaims = [
        { id: 1, claimNumber: 'CLM-001', status: 'SUBMITTED', incidentDate: '2025-01-01' },
        { id: 2, claimNumber: 'CLM-002', status: 'SETTLED', incidentDate: '2025-02-01' }
    ];

    beforeEach(async () => {
        authService = {
            currentUser: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [RequestsComponent, HttpClientTestingModule],
            providers: [
                { provide: AuthService, useValue: authService }
            ]
        }).compileComponents();

        httpMock = TestBed.inject(HttpTestingController);
        authService.currentUser.mockReturnValue(mockUser);

        fixture = TestBed.createComponent(RequestsComponent);
        component = fixture.componentInstance;
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should create', () => {
        fixture.detectChanges();
        
        const officerReq = httpMock.expectOne('/api/claim-officers/by-user/10');
        officerReq.flush(mockOfficer);
        
        const claimsReq = httpMock.expectOne('/api/claims/claim-officer/5');
        claimsReq.flush(mockClaims);
        
        expect(component).toBeTruthy();
    });

    it('should load officer and claims correctly', () => {
        fixture.detectChanges();
        
        httpMock.expectOne('/api/claim-officers/by-user/10').flush(mockOfficer);
        httpMock.expectOne('/api/claims/claim-officer/5').flush(mockClaims);
        
        expect(component.officerId()).toBe(5);
        expect(component.pendingClaims().length).toBe(1); // Only SUBMITTED is actionable
    });

    it('should approve claim on confirmation', () => {
        fixture.detectChanges();
        httpMock.expectOne('/api/claim-officers/by-user/10').flush(mockOfficer);
        httpMock.expectOne('/api/claims/claim-officer/5').flush(mockClaims);
        
        vi.spyOn(window, 'confirm').mockReturnValue(true);
        const claim = component.pendingClaims()[0];
        
        component.approveClaim(claim);
        
        const approveReq = httpMock.expectOne(`/api/claims/${claim.id}/approve`);
        expect(approveReq.request.method).toBe('PUT');
        approveReq.flush({});
        
        // Verify it reloads claims
        httpMock.expectOne('/api/claims/claim-officer/5');
    });

    it('should initiate rejection modal', () => {
        const claim = { id: 1, claimNumber: 'CLM-001' } as any;
        component.initiateRejection(claim);
        
        expect(component.selectedClaimForRejection()).toEqual(claim);
        expect(component.showRejectionModal()).toBe(true);
        expect(component.rejectionReason()).toBe('');
    });

    it('should confirm rejection with reason', () => {
        component.selectedClaimForRejection.set({ id: 1 } as any);
        component.rejectionReason.set('Fraud detected');
        
        component.confirmRejection();
        
        const rejectReq = httpMock.expectOne('/api/claims/1/reject?reason=Fraud%20detected');
        expect(rejectReq.request.method).toBe('PUT');
        rejectReq.flush({});
        
        expect(component.showRejectionModal()).toBe(false);
        expect(component.isSubmitting()).toBe(false);
    });

    it('should handle document opening', () => {
        const doc = { id: 100, fileName: 'proof.pdf' } as any;
        const mockBlob = new Blob([''], { type: 'application/pdf' });
        
        vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:url');
        vi.spyOn(window, 'open');
        
        component.openDocument(doc);
        
        const req = httpMock.expectOne('/api/documents/100');
        req.flush(mockBlob);
        
        expect(window.open).toHaveBeenCalledWith('blob:url', '_blank');
         // For revokeObjectURL
    });

    it('should return correct file icon', () => {
        expect(component.getFileIcon('pdf')).toBe('📕');
        expect(component.getFileIcon('image/jpeg')).toBe('🖼️');
        expect(component.getFileIcon('text/plain')).toBe('📎');
        expect(component.getFileIcon('')).toBe('📄');
    });

    it('should handle officer load error', () => {
        fixture.detectChanges();
        httpMock.expectOne('/api/claim-officers/by-user/10').error(new ErrorEvent('Error'));
        expect(component.isLoading()).toBe(false);
    });
});




