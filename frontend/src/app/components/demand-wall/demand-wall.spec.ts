import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Demand, DemandService } from '../../services/demand.service';
import { ToastService } from '../../services/toast.service';
import { DemandWall } from './demand-wall';

describe('DemandWall', () => {
  let component: DemandWall;
  let fixture: ComponentFixture<DemandWall>;
  let demandService: {
    list: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
  };
  let toastService: {
    success: ReturnType<typeof vi.fn>;
    error: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    const emptyPage = {
      content: [],
      number: 0,
      totalPages: 0,
      totalElements: 0,
      last: true
    };
    demandService = {
      list: vi.fn().mockReturnValue(of(emptyPage)),
      create: vi.fn()
    };
    toastService = {
      success: vi.fn(),
      error: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [DemandWall],
      providers: [
        { provide: DemandService, useValue: demandService },
        { provide: ToastService, useValue: toastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DemandWall);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('loads the published demands from the API', () => {
    expect(demandService.list).toHaveBeenCalledOnce();
    expect(demandService.list).toHaveBeenCalledWith(0);
    expect(component.demands()).toEqual([]);
    expect(component.isLoading()).toBe(false);
  });

  it('requests and appends the next API page', () => {
    const nextDemand: Demand = {
      id: 13,
      titulo: 'Libro de física',
      descripcion: null,
      presupuestoEstimado: null,
      categoria: 'LIBROS Y APUNTES',
      estado: 'ACTIVA',
      fechaCreacion: new Date().toISOString(),
      usuarioId: 4,
      usuarioNombre: 'Luis Pérez'
    };
    demandService.list.mockReturnValueOnce(of({
      content: [nextDemand],
      number: 1,
      totalPages: 2,
      totalElements: 21,
      last: true
    }));
    component.hasMore.set(true);

    component.loadDemands(true);

    expect(demandService.list).toHaveBeenLastCalledWith(1);
    expect(component.demands()).toEqual([nextDemand]);
    expect(component.totalElements()).toBe(21);
    expect(component.hasMore()).toBe(false);
  });

  it('adds a newly published demand and applies its session filters', () => {
    const created: Demand = {
      id: 12,
      titulo: 'Calculadora científica',
      descripcion: 'Modelo Casio',
      presupuestoEstimado: 45,
      categoria: 'TECNOLOGÍA',
      estado: 'ACTIVA',
      fechaCreacion: new Date().toISOString(),
      usuarioId: 3,
      usuarioNombre: 'María Torres'
    };
    demandService.create.mockReturnValue(of(created));

    component.createDemand({
      request: {
        titulo: created.titulo,
        descripcion: created.descripcion ?? '',
        presupuestoEstimado: created.presupuestoEstimado,
        categoria: 'TECNOLOGÍA'
      },
      category: 'Tecnología',
      campus: 'Lima Este',
      validForDays: 7
    });

    expect(component.demands()).toEqual([created]);
    component.selectedCategory = 'Tecnología';
    component.selectedCampus = 'Lima Este';
    expect(component.filteredDemands()).toEqual([created]);
    expect(toastService.success).toHaveBeenCalledOnce();
  });
});
