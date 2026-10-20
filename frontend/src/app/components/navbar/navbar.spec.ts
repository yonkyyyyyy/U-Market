import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Navbar } from './navbar';

describe('Navbar', () => {
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('navigates to existing sections and does not show unsupported destinations', () => {
    const links = fixture.nativeElement.querySelectorAll('a') as NodeListOf<HTMLAnchorElement>;
    const destinations = Array.from(links, link => link.textContent?.trim());
    const homeLink = Array.from(links).find(link => link.textContent?.trim() === 'Inicio');
    const publicationsLink = Array.from(links).find(
      link => link.textContent?.trim() === 'Publicaciones'
    );

    expect(destinations).toContain('Inicio');
    expect(destinations).toContain('Publicaciones');
    expect(destinations).not.toContain('Mis publicaciones');
    expect(destinations).not.toContain('Perfil');
    expect(homeLink?.getAttribute('href')).toBe('/publicaciones#inicio');
    expect(publicationsLink?.getAttribute('href')).toBe('/publicaciones#publicaciones');
  });

  it('toggles the mobile navigation menu', () => {
    const toggle = fixture.nativeElement.querySelector(
      'button[aria-controls="primary-navigation"]'
    ) as HTMLButtonElement;
    const menu = fixture.nativeElement.querySelector('#primary-navigation') as HTMLElement;

    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(menu.classList.contains('hidden')).toBe(true);

    toggle.click();
    fixture.detectChanges();

    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(menu.classList.contains('hidden')).toBe(false);
  });
});
