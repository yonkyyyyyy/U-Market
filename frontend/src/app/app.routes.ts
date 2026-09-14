import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Publicaciones } from './pages/publicaciones/publicaciones';

export const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: 'publicaciones',
    component: Publicaciones
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  }
];