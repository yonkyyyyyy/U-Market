import { Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast-viewport',
  templateUrl: './toast-viewport.html',
  styleUrl: './toast-viewport.css'
})
export class ToastViewport {
  readonly toastService = inject(ToastService);
}
