import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ProductService } from '../../services/product.service';
import { FlashSale } from './flash-sale';

describe('FlashSale', () => {
  it('shows flash offers and keeps regular products out of the offer list', async () => {
    await TestBed.configureTestingModule({
      imports: [FlashSale],
      providers: [provideHttpClient()]
    }).compileComponents();

    const productService = TestBed.inject(ProductService);
    productService.products.set([
      {
        id: 1,
        titulo: 'Calculadora',
        descripcion: null,
        precio: 50,
        estado: 'DISPONIBLE',
        createdAt: null,
        usuarioId: 2,
        vendedorNombre: 'Ana',
        ofertaFlash: {
          precioOferta: 35,
          cuposDisponibles: 4,
          finalizaEn: '2026-11-16T00:00:00Z'
        }
      },
      {
        id: 2,
        titulo: 'Cuaderno',
        descripcion: null,
        precio: 10,
        estado: 'DISPONIBLE',
        createdAt: null,
        usuarioId: 3,
        vendedorNombre: 'Luis'
      }
    ]);

    const fixture = TestBed.createComponent(FlashSale);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Calculadora');
    expect(fixture.nativeElement.textContent).not.toContain('Cuaderno');
    expect(fixture.nativeElement.textContent).toContain('4 cupos');
  });
});
