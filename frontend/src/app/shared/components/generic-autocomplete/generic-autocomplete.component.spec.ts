import {ComponentFixture, TestBed} from '@angular/core/testing';
import {GenericAutocompleteComponent} from './generic-autocomplete.component';
import {AutoCompleteService} from './autocomplete.service';
import {of} from 'rxjs';
import {CommonModule} from '@angular/common';

describe('GenericAutocompleteComponent', () => {
  let component: GenericAutocompleteComponent;
  let fixture: ComponentFixture<GenericAutocompleteComponent>;
  let autoCompleteServiceMock: any;

  beforeEach(async () => {
    autoCompleteServiceMock = {
      getSuggestion: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [GenericAutocompleteComponent, CommonModule],
      providers: [{provide: AutoCompleteService, useValue: autoCompleteServiceMock}],
    }).compileComponents();

    fixture = TestBed.createComponent(GenericAutocompleteComponent);
    component = fixture.componentInstance;

    // Set required inputs
    fixture.componentRef.setInput('field', 'testField');
    autoCompleteServiceMock.getSuggestion.mockReturnValue(of(['Option 1', 'Option 2']));

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('writeValue', () => {
    it('should set selectedValue array when multiSelect is true', () => {
      fixture.componentRef.setInput('multiSelect', true);
      component.writeValue(['Option 1']);
      expect(component['selectedValue']()).toEqual(['Option 1']);
    });

    it('should set single selectedValue when multiSelect is false', () => {
      fixture.componentRef.setInput('multiSelect', false);
      component.writeValue('Option 2');
      expect(component['selectedValue']()).toEqual('Option 2');
      expect(component['searchCtrl'].value).toBe('Option 2');
    });
  });

  describe('setDisabledState', () => {
    it('should disable searchCtrl when disabled', () => {
      component.setDisabledState(true);
      expect(component['isDisabled']).toBe(true);
      expect(component['searchCtrl'].disabled).toBe(true);
    });

    it('should enable searchCtrl when enabled', () => {
      component.setDisabledState(false);
      expect(component['isDisabled']).toBe(false);
      expect(component['searchCtrl'].enabled).toBe(true);
    });
  });

  describe('onOptionSelected', () => {
    it('should add to selectedValue correctly in multiSelect mode', async () => {
      fixture.componentRef.setInput('multiSelect', true);
      component.writeValue(['Option 1']);

      const mockEvent: any = {
        option: {
          value: 'Option 2',
          deselect: vi.fn(),
        },
      };

      const onChangeSpy = vi.fn();
      component.registerOnChange(onChangeSpy);

      component['onOptionSelected'](mockEvent);
      await new Promise((r) => setTimeout(r, 0)); // resolve setTimeout

      expect(component['selectedValue']()).toEqual(['Option 1', 'Option 2']);
      expect(mockEvent.option.deselect).toHaveBeenCalled();
      expect(component['searchCtrl'].value).toBe('');
      expect(onChangeSpy).toHaveBeenCalledWith(['Option 1', 'Option 2']);
    });

    it('should remove from selectedValue if already selected in multiSelect mode', async () => {
      fixture.componentRef.setInput('multiSelect', true);
      component.writeValue(['Option 1', 'Option 2']);

      const mockEvent: any = {
        option: {
          value: 'Option 2',
          deselect: vi.fn(),
        },
      };

      component['onOptionSelected'](mockEvent);
      await new Promise((r) => setTimeout(r, 0));

      expect(component['selectedValue']()).toEqual(['Option 1']);
    });

    it('should replace selectedValue in single select mode', () => {
      fixture.componentRef.setInput('multiSelect', false);

      const mockEvent: any = {
        option: {value: 'Option 3'},
      };

      const onChangeSpy = vi.fn();
      component.registerOnChange(onChangeSpy);

      component['onOptionSelected'](mockEvent);

      expect(component['selectedValue']()).toBe('Option 3');
      expect(onChangeSpy).toHaveBeenCalledWith('Option 3');
    });
  });
});
