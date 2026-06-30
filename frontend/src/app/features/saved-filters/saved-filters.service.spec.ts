import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { SavedFiltersService } from './saved-filters.service';
import { environment } from '@env/environment';
import { SavedFilter, CreateSavedFilterRequest } from '@core/models/saved-filter.model';
import { PagedResponse } from '@core/models/paged-response.model';

describe('SavedFiltersService', () => {
  let service: SavedFiltersService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/saved-filters`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SavedFiltersService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(SavedFiltersService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('listSavedFilters', () => {
    it('should send a GET request with pagination parameters', () => {
      const mockResponse: PagedResponse<SavedFilter> = {
        content: [
          { id: 1, name: 'Test Filter', filterJson: {}, createdAt: '2023-01-01T00:00:00Z' },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1
      };

      service.listSavedFilters(0, 20).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}?page=0&size=20`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('createSavedFilter', () => {
    it('should send a POST request with the new filter data', () => {
      const requestDto: CreateSavedFilterRequest = {
        name: 'New Filter',
        filterJson: { accession: 'Q1' }
      };

      const mockResponse: SavedFilter = {
        id: 2,
        name: 'New Filter',
        filterJson: { accession: 'Q1' },
        createdAt: '2023-01-01T00:00:00Z'
      };

      service.createSavedFilter(requestDto).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(requestDto);
      req.flush(mockResponse);
    });
  });

  describe('deleteSavedFilter', () => {
    it('should send a DELETE request for the specified id', () => {
      service.deleteSavedFilter(1).subscribe((response) => {
        expect(response).toBeNull();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});


