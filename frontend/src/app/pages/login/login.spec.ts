import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { Login } from './login';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authService: {
    login: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
    saveSession: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toastService: { success: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authService = {
      login: vi.fn(),
      register: vi.fn().mockReturnValue(of({ token: 'token', user: { id: 1 } })),
      saveSession: vi.fn()
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    toastService = { success: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('registers the account, saves the returned session, and opens publications', () => {
    component.name = 'Ana';
    component.lastName = 'Torres';
    component.registerEmail = 'ana@utp.edu.pe';
    component.registerPassword = 'password123';
    component.confirmPassword = 'password123';

    component.register();

    expect(authService.register).toHaveBeenCalledWith({
      nombre: 'Ana Torres',
      correo: 'ana@utp.edu.pe',
      password: 'password123'
    });
    expect(authService.saveSession).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/publicaciones']);
    expect(toastService.success).toHaveBeenCalledOnce();
  });

  it('logs in with the account credentials, saves the session, and opens publications', () => {
    authService.login.mockReturnValue(of({ token: 'token', user: { id: 1 } }));
    component.correo = 'ana@utp.edu.pe';
    component.password = 'password123';

    component.login();

    expect(authService.login).toHaveBeenCalledWith({
      correo: 'ana@utp.edu.pe',
      password: 'password123'
    });
    expect(authService.saveSession).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/publicaciones']);
  });

  it('requires an institutional email before sending registration', () => {
    component.name = 'Ana';
    component.registerEmail = 'ana@example.com';
    component.registerPassword = 'password123';
    component.confirmPassword = 'password123';

    component.register();

    expect(authService.register).not.toHaveBeenCalled();
    expect(component.registerError).toContain('@utp.edu.pe');
  });
});
