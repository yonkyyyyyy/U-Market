import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DemandService } from './demand.service';

describe('DemandService', () => {
  let service: DemandService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DemandService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTestingController.verify());

  it('loads active demands from a page of the API', () => {
    const response = {
      content: [{
        id: 7,
        titulo: 'Calculadora científica',
        descripcion: null,
        presupuestoEstimado: null,
        categoria: 'TECNOLOGÍA',
        estado: 'ACTIVA',
        fechaCreacion: null,
        usuarioId: 2,
        usuarioNombre: 'Ana'
      }],
      number: 0,
      totalPages: 1,
      totalElements: 1,
      last: true
    };
    let result: unknown;

    service.list(0).subscribe(value => result = value);

    const request = httpTestingController.expectOne('/api/demandas?estado=ACTIVA&page=0&size=20');
    expect(request.request.method).toBe('GET');
    request.flush(response);
    expect(result).toEqual(response);
  });

  it('sends only fields supported by the demand API', () => {
    const payload = {
      titulo: 'Libro de cálculo',
      descripcion: 'Edición reciente',
      presupuestoEstimado: 35,
      categoria: 'LIBROS Y APUNTES'
    };
    let result: unknown;

    service.create(payload).subscribe(value => result = value);

    const request = httpTestingController.expectOne('/api/demandas');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({
      ...payload,
      id: 8,
      estado: 'ACTIVA',
      fechaCreacion: null,
      usuarioId: 4,
      usuarioNombre: 'Luis'
    });
    expect(result).toEqual({
      ...payload,
      id: 8,
      estado: 'ACTIVA',
      fechaCreacion: null,
      usuarioId: 4,
      usuarioNombre: 'Luis'
    });
  });
});
