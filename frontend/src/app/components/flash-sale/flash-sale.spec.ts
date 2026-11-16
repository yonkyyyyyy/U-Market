import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { FlashSaleStockService } from '../../services/flash-sale-stock.service';
import { ProductService } from '../../services/product.service';
import { FlashSale, formatCountdown } from './flash-sale';

describe('FlashSale', () => {
  it('formats a deadline from the remaining time rather than decrementing a counter', () => {
    const now = Date.parse('2026-11-16T00:00:00Z');

    expect(formatCountdown('2026-11-17T03:04:05Z', now)).toBe('1d 03:04:05');
    expect(formatCountdown('2026-11-15T23:59:59Z', now)).toBe('Finalizada');
    expect(formatCountdown('invalid-date', now)).toBe('Horario no disponible');
  });

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

    TestBed.inject(FlashSaleStockService).acceptStockUpdate({
      channel: 'stock.1',
      productId: 1,
      cuposDisponibles: 2
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('2 cupos');
  });

  it('renders expiration and oversale errors as accessible alert states', async () => {
    await TestBed.configureTestingModule({
      imports: [FlashSale],
      providers: [provideHttpClient()]
    }).compileComponents();

    TestBed.inject(ProductService).products.set([
      {
        id: 3,
        titulo: 'Libro usado',
        descripcion: null,
        precio: 20,
        estado: 'DISPONIBLE',
        createdAt: null,
        usuarioId: 2,
        vendedorNombre: 'Ana',
        ofertaFlash: {
          precioOferta: 12,
          cuposDisponibles: 1,
          finalizaEn: '2000-01-01T00:00:00Z',
          estado: 'EXPIRADA'
        }
      },
      {
        id: 4,
        titulo: 'Audífonos',
        descripcion: null,
        precio: 40,
        estado: 'DISPONIBLE',
        createdAt: null,
        usuarioId: 3,
        vendedorNombre: 'Luis',
        ofertaFlash: {
          precioOferta: 25,
          cuposDisponibles: 1,
          finalizaEn: '2099-01-01T00:00:00Z',
          estado: 'SOBREVENTA'
        }
      }
    ]);

    const fixture = TestBed.createComponent(FlashSale);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Expirada');
    expect(fixture.nativeElement.textContent).toContain('El tiempo para aprovechar esta oferta terminó.');
    expect(fixture.nativeElement.textContent).toContain('Sin cupos al confirmar');
    expect(fixture.nativeElement.textContent).toContain('Los cupos se agotaron antes de confirmar la operación.');
    expect(fixture.nativeElement.querySelectorAll('[role="alert"]').length).toBe(2);
  });
});
