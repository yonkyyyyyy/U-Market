import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductRequest, ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-create-post',
  imports: [FormsModule],
  templateUrl: './create-post.html',
  styleUrl: './create-post.css'
})
export class CreatePost {
  private readonly productService = inject(ProductService);
  private readonly toastService = inject(ToastService);

  readonly campuses = [
    'Lima Centro',
    'Lima Este',
    'Lima Norte',
    'Lima Sur',
    'Ate',
    'Callao',
    'Arequipa',
    'Chiclayo',
    'Chimbote',
    'Huancayo',
    'Ica',
    'Piura',
    'Trujillo'
  ];

  title = '';
  description = '';
  price: number | null = null;
  campus = '';
  isPublishing = false;
  publishError = '';

  publish(): void {
    const title = this.title.trim();
    const price = Number(this.price);

    if (!title || this.price === null || !Number.isFinite(price) || price <= 0) {
      this.publishError = 'Escribe un título y un precio mayor que cero.';
      return;
    }

    this.isPublishing = true;
    this.publishError = '';

    const payload: ProductRequest = {
      titulo: title,
      descripcion: this.description.trim(),
      precio: price
    };
    if (this.campus) {
      payload.campus = this.campus;
    }

    this.productService.create(payload).subscribe({
      next: () => {
        this.title = '';
        this.description = '';
        this.price = null;
        this.campus = '';
        this.isPublishing = false;
        this.toastService.success('Publicación creada', 'Tu producto ya aparece en el muro.');
      },
      error: (error: unknown) => {
        this.isPublishing = false;
        this.publishError = this.getErrorMessage(error);
      }
    });
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Inicia sesión para publicar un producto.';
    }

    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    return 'No se pudo publicar el producto. Revisa tu conexión e inténtalo de nuevo.';
  }
}
