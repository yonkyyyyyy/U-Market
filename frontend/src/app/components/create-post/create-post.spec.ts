import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CopywritingService } from '../../services/copywriting.service';
import { ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';
import { CreatePost } from './create-post';

describe('CreatePost', () => {
  let component: CreatePost;
  let fixture: ComponentFixture<CreatePost>;
  let productService: { create: ReturnType<typeof vi.fn> };
  let copywritingService: { generate: ReturnType<typeof vi.fn> };
  let toastService: { success: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    productService = { create: vi.fn().mockReturnValue(of({ id: 1 })) };
    copywritingService = { generate: vi.fn().mockReturnValue(of({ copys: ['Copy 1', 'Copy 2', 'Copy 3'] })) };
    toastService = { success: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CreatePost],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: CopywritingService, useValue: copywritingService },
        { provide: ToastService, useValue: toastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreatePost);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('submits a product to the API and clears the form on success', () => {
    component.title = '  Libro de cálculo  ';
    component.description = '  Edición reciente  ';
    component.price = 25;

    component.publish();

    expect(productService.create).toHaveBeenCalledWith({
      titulo: 'Libro de cálculo',
      descripcion: 'Edición reciente',
      precio: 25
    });
    expect(component.title).toBe('');
    expect(component.description).toBe('');
    expect(component.price).toBeNull();
    expect(toastService.success).toHaveBeenCalledOnce();
  });

  it('rejects a missing title or invalid price without calling the API', () => {
    component.title = 'Libro';
    component.price = 0;

    component.publish();

    expect(productService.create).not.toHaveBeenCalled();
    expect(component.publishError).toContain('precio mayor que cero');
  });

  it('generates copy alternatives and only changes the description when one is selected', () => {
    component.title = 'Cuaderno';

    component.generateCopies();

    expect(copywritingService.generate).toHaveBeenCalledWith({
      titulo: 'Cuaderno',
      descripcion: '',
      plataforma: 'Instagram',
      tono: 'Cercano'
    });
    expect(component.generatedCopies).toEqual(['Copy 1', 'Copy 2', 'Copy 3']);
    expect(component.description).toBe('');

    component.useCopy('Copy 2');

    expect(component.description).toBe('Copy 2');
  });

  it('does not request generated copies when the product title is blank', () => {
    component.generateCopies();

    expect(copywritingService.generate).not.toHaveBeenCalled();
    expect(component.copyError).toContain('nombre del producto');
  });
});
