import { TestBed,  } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be defined', () => {
    expect(authInterceptor).toBeTruthy();
  });

  it('should attach Authorization header when token exists in localStorage', () => {
    localStorage.setItem('token', 'test-jwt-token');

    httpClient.get('/api/policies').subscribe();
    const req = httpMock.expectOne('/api/policies');

    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-jwt-token');

    req.flush([]);
    
  });

  it('should NOT attach Authorization header when no token in localStorage', () => {
    httpClient.get('/api/policies').subscribe();
    const req = httpMock.expectOne('/api/policies');

    expect(req.request.headers.has('Authorization')).toBe(false);

    req.flush([]);
    
  });

  it('should pass requests through unchanged except for Authorization header', () => {
    localStorage.setItem('token', 'my-token');

    httpClient.get('/api/users').subscribe();
    const req = httpMock.expectOne('/api/users');

    expect(req.request.url).toBe('/api/users');
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');

    req.flush([]);
    
  });

  it('should attach token for POST requests as well', () => {
    localStorage.setItem('token', 'post-token');

    httpClient.post('/api/claims', {}).subscribe();
    const req = httpMock.expectOne('/api/claims');

    expect(req.request.headers.get('Authorization')).toBe('Bearer post-token');

    req.flush({});
    
  });
});




