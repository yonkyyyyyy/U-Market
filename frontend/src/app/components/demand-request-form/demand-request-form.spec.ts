import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DemandRequestForm } from './demand-request-form';

describe('DemandRequestForm', () => {
  let component: DemandRequestForm;
  let fixture: ComponentFixture<DemandRequestForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemandRequestForm]
    }).compileComponents();

    fixture = TestBed.createComponent(DemandRequestForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('emits the API payload with the selected frontend-only details', () => {
    let submitted: unknown;
    component.submitted.subscribe(value => submitted = value);
    component.titulo = 'Calculadora científica';
    component.descripcion = 'Modelo Casio';
    component.presupuesto = 45.5;
    component.categoria = 'Tecnología';
    component.campus = 'Lima Este';
    component.vigencia = 3;

    component.submit();

    expect(submitted).toEqual({
      request: {
        titulo: 'Calculadora científica',
        descripcion: 'Modelo Casio',
        presupuestoEstimado: 45.5,
        categoria: 'TECNOLOGÍA'
      },
      category: 'Tecnología',
      campus: 'Lima Este',
      validForDays: 3
    });
  });

  it('does not emit when required fields or budget are invalid', () => {
    let wasSubmitted = false;
    component.submitted.subscribe(() => wasSubmitted = true);
    component.titulo = 'Calculadora científica';
    component.categoria = 'Tecnología';
    component.campus = 'Lima Este';
    component.presupuesto = '0';

    component.submit();

    expect(wasSubmitted).toBe(false);
  });
});
