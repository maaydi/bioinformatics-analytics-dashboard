import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {MainLayoutComponent} from './main-layout.component';
import {NavbarComponent} from '../navbar/navbar.component';
import {provideRouter, RouterOutlet} from '@angular/router';
import {MatSidenavModule} from '@angular/material/sidenav';
import {AuthService} from '@core/services/auth.service';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';

describe('MainLayoutComponent', () => {
  let component: MainLayoutComponent;
  let fixture: ComponentFixture<MainLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MainLayoutComponent,
        NavbarComponent,
        RouterOutlet,
        MatSidenavModule,
      ],
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(MainLayoutComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });


  it('should be a standalone component', () => {
    const metadata = (MainLayoutComponent as any).ɵcmp;
    expect(metadata.standalone).toBe(true);
  });

  it('should render navbar component', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const navbarComponent = compiled.querySelector('app-navbar');
    expect(navbarComponent).toBeTruthy();
  });

  it('should have router outlet for feature pages', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const routerOutlet = compiled.querySelector('router-outlet');
    expect(routerOutlet).toBeTruthy();
  });

  it('should render without errors', () => {
    expect(() => fixture.detectChanges()).not.toThrow();
  });
});

