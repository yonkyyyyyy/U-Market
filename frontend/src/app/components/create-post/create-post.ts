import { Component } from '@angular/core';

@Component({
  selector: 'app-create-post',
  imports: [],
  templateUrl: './create-post.html',
  styleUrl: './create-post.css'
})
export class CreatePost {

  showImageUpload = false;
  selectedImage: string | null = null;

  toggleImageUpload(): void {
    this.showImageUpload = !this.showImageUpload;

    
    if (!this.showImageUpload) {
      this.selectedImage = null;
    }
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    
    if (!file.type.startsWith('image/')) {
      alert('Por favor selecciona una imagen válida.');
      return;
    }

    
    if (file.size > 5 * 1024 * 1024) {
      alert('La imagen no puede superar los 5 MB.');
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      this.selectedImage = reader.result as string;
    };

    reader.readAsDataURL(file);
  }

  removeImage(): void {
    this.selectedImage = null;
  }

  publish(): void {
    console.log('Publicación preparada');

    console.log({
      image: this.selectedImage
    });
  }
}