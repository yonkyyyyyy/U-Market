import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Publicaciones } from './publicaciones';

describe('Publicaciones', () => {
  let component: Publicaciones;
  let fixture: ComponentFixture<Publicaciones>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Publicaciones],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Publicaciones);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
