import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });

    http = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
    sessionStorage.clear();
  });

  it('adds the saved bearer token to API requests', () => {
    sessionStorage.setItem('umarket_token', 'test-token');

    http.get('/api/productos').subscribe();

    const request = httpTestingController.expectOne('/api/productos');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
    request.flush([]);
  });

  it('does not add the token to non-API requests', () => {
    sessionStorage.setItem('umarket_token', 'test-token');

    http.get('/assets/logo.svg').subscribe();

    const request = httpTestingController.expectOne('/assets/logo.svg');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush('');
  });
});
