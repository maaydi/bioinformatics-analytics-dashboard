import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {GenesPageComponent} from './genes-page.component';

describe('GenesPageComponent', () => {
  let component: GenesPageComponent;
  let fixture: ComponentFixture<GenesPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GenesPageComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(GenesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose store instance', () => {
    expect(component.store).toBeDefined();
  });

  it('should render filter, global search, active filters and table containers', () => {
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('app-gene-filter')).toBeTruthy();
    expect(host.querySelector('app-global-search')).toBeTruthy();
    expect(host.querySelector('app-active-filters')).toBeTruthy();
    expect(host.querySelector('app-genes-table')).toBeTruthy();
  });

  it('should keep store reference stable across change detection', () => {
    const firstStore = component.store;

    fixture.detectChanges();
    fixture.detectChanges();

    expect(component.store).toBe(firstStore);
  });
});
