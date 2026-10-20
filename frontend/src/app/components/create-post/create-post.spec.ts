import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';
import { CreatePost } from './create-post';

describe('CreatePost', () => {
  let component: CreatePost;
  let fixture: ComponentFixture<CreatePost>;
  let productService: { create: ReturnType<typeof vi.fn> };
  let toastService: { success: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    productService = { create: vi.fn().mockReturnValue(of({ id: 1 })) };
    toastService = { success: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CreatePost],
      providers: [
        { provide: ProductService, useValue: productService },
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
});
