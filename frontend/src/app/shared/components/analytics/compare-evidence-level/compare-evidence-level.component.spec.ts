import {ComponentFixture, TestBed} from '@angular/core/testing';

import {CompareEvidenceLevelComponent} from './compare-evidence-level.component';

describe('CompareEvidenceLevelComponent', () => {
  let component: CompareEvidenceLevelComponent;
  let fixture: ComponentFixture<CompareEvidenceLevelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompareEvidenceLevelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CompareEvidenceLevelComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
