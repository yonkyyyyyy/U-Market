import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SmartSearch } from './smart-search';

describe('SmartSearch', () => {
  let component: SmartSearch;
  let fixture: ComponentFixture<SmartSearch>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SmartSearch],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(SmartSearch);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    sessionStorage.clear();
    await fixture.whenStable();
  });

  afterEach(() => {
    sessionStorage.clear();
    httpTestingController.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the interpretation and results returned by the NLP endpoint', () => {
    sessionStorage.setItem('umarket_token', 'fake-token');
    component.consulta = 'laptop';
    component.buscar();

    const request = httpTestingController.expectOne(
      req => req.url === '/api/productos/buscar' && req.params.get('q') === 'laptop'
    );
    expect(request.request.method).toBe('GET');

    request.flush({
      consulta: 'laptop',
      interpretacion: {
        palabrasClave: ['laptop'],
        categoria: 'Tecnología',
        campus: null,
        precioMaximo: 1500,
        tags: [],
        origen: 'LLM'
      },
      resultados: [{
        id: 1,
        titulo: 'Laptop Lenovo',
        descripcion: 'Ideal para clases',
        precio: 1200,
        categoria: 'Tecnología',
        campus: 'Lima Este',
        estado: 'ACTIVO',
        createdAt: null,
        usuarioId: 2,
        vendedorNombre: 'Ana'
      }]
    });

    expect(component.response()?.resultados.length).toBe(1);
    expect(component.esLlm()).toBe(true);
    expect(component.resumenInterpretacion()).toContain('Categoría: Tecnología');
    expect(component.resumenInterpretacion().some(value => value.includes('1,500'))).toBe(true);
  });

  it('asks the user to log in before searching', () => {
    component.consulta = 'laptop';
    component.buscar();

    expect(component.error()).toContain('Inicia sesión');
    httpTestingController.expectNone('/api/productos/buscar?q=laptop');
  });
});
