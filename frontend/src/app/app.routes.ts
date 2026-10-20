import { Routes } from '@angular/router';
import { adminGuard } from './guards/admin.guard';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';
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
    path: 'admin/dashboard',
    component: AdminDashboard,
    canActivate: [adminGuard]
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  }
];