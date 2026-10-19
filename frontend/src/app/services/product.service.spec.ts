import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProductService } from './product.service';

describe('ProductService', () => {
  let service: ProductService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ProductService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTestingController.verify());

  it('loads available products from the API', () => {
    const products = [{
      id: 1,
      titulo: 'Libro de cálculo',
      descripcion: 'Edición reciente',
      precio: 25,
      estado: 'DISPONIBLE',
      createdAt: null,
      usuarioId: 2,
      vendedorNombre: 'Ana'
    }];
    let result: unknown;

    service.list().subscribe(value => result = value);

    const request = httpTestingController.expectOne('/api/productos?estado=DISPONIBLE');
    expect(request.request.method).toBe('GET');
    request.flush(products);

    expect(result).toEqual(products);
    expect(service.products()).toEqual(products);
  });

  it('publishes a product and adds it to the shared list', () => {
    const payload = { titulo: 'Libro de cálculo', descripcion: 'Edición reciente', precio: 25 };
    const created = {
      ...payload,
      id: 1,
      estado: 'DISPONIBLE',
      createdAt: null,
      usuarioId: 2,
      vendedorNombre: 'Ana'
    };
    let result: unknown;

    service.create(payload).subscribe(value => result = value);

    const request = httpTestingController.expectOne('/api/productos');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(created);

    expect(result).toEqual(created);
    expect(service.products()).toEqual([created]);
  });
});
