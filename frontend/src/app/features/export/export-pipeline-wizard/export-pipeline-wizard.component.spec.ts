import {ComponentFixture, TestBed} from '@angular/core/testing';

import {ExportPipelineWizardComponent} from './export-pipeline-wizard.component';

describe('ExportPipelineWizardComponent', () => {
  let component: ExportPipelineWizardComponent;
  let fixture: ComponentFixture<ExportPipelineWizardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExportPipelineWizardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ExportPipelineWizardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
