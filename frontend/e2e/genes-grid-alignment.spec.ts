import {expect, test} from '@playwright/test';
import type {PagedResponse} from '../src/app/core/models/paged-response.model';
import type {ProteinSummary} from '../src/app/core/models/protein.model';

const results: PagedResponse<ProteinSummary> = {
  content: [{
    id: 1,
    accession: 'P12345',
    entryName: 'GENE_HUMAN',
    proteinFullName: 'Protein for grid layout validation',
    geneNamePrimary: 'GENE1',
    organismName: 'Homo sapiens',
    taxid: 9606,
    length: 350,
    molecularWeight: 38000,
    reviewed: true,
    evidenceLevel: 1,
    keywords: ['Kinase', 'Membrane', 'Receptor'],
  }],
  page: 0,
  size: 50,
  totalElements: 1,
  totalPages: 1,
};

test.use({viewport: {width: 1600, height: 1000}});

test('keeps header, icon and renderer alignment stable when sorting', async ({context, page}) => {
  // Test-only session: requests are fulfilled locally and never reach the backend.
  const encode = (payload: object): string => Buffer.from(JSON.stringify(payload)).toString('base64');
  const tokens = {
    accessToken: `${encode({alg: 'none', typ: 'JWT'})}.${encode({exp: 4102444800, roles: ['ROLE_USER']})}.test`,
    refreshToken: 'grid-layout-test',
    expiresIn: 3600,
    tokenType: 'Bearer',
  };

  await context.addInitScript(({accessToken, refreshToken}) => {
    sessionStorage.setItem('accessToken', accessToken);
    sessionStorage.setItem('refreshToken', refreshToken);
  }, tokens);

  const unexpectedRequests: string[] = [];
  await context.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    switch (path) {
      case '/api/auth/refresh':
        await route.fulfill({json: tokens});
        break;
      case '/api/genes/search':
        await route.fulfill({json: results});
        break;
      case '/api/autocomplete':
        await route.fulfill({json: []});
        break;
      default:
        unexpectedRequests.push(path);
        await route.abort();
    }
  });

  // Start on a public route so SSR does not need a backend session.
  const refreshResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/api/auth/refresh' && response.ok());
  await page.goto('/login');
  await refreshResponse;
  await page.waitForFunction(() => {
    const login = document.querySelector('app-login');
    const debugWindow = window as Window & {ng?: {getComponent(element: Element): unknown}};
    return login !== null && Boolean(debugWindow.ng?.getComponent(login));
  });
  await page.evaluate(() => {
    history.pushState(null, '', '/genes');
    window.dispatchEvent(new PopStateEvent('popstate'));
  });

  const grid = page.locator('app-genes-table .gene-grid');
  const row = grid.locator('.ag-center-cols-container .ag-row[row-id="P12345"]');
  await expect(row).toBeVisible();
  await expect(page.locator('app-genes-table .table-wrapper')).toHaveAttribute('aria-busy', 'false');
  await page.evaluate(() => document.fonts.ready);

  const checkAlignment = async (): Promise<void> => {
    for (const field of ['length', 'reviewed', 'evidenceLevel', 'keywords']) {
      const header = grid.locator(`.ag-header-cell[col-id="${field}"]`);
      const cell = row.locator(`.ag-cell[col-id="${field}"]`);
      const headerPadding = await header.evaluate((element) => {
        const style = getComputedStyle(element);
        return [style.paddingInlineStart, style.paddingInlineEnd];
      });
      const cellPadding = await cell.evaluate((element) => {
        const style = getComputedStyle(element);
        return [style.paddingInlineStart, style.paddingInlineEnd];
      });
      expect(headerPadding).toEqual(cellPadding);

      const label = header.locator('.ag-header-cell-text');
      const labelBox = await label.boundingBox();
      const headerBox = await header.boundingBox();
      expect(labelBox).not.toBeNull();
      expect(headerBox).not.toBeNull();
      if (!labelBox || !headerBox) {
        throw new Error(`Missing header geometry for ${field}`);
      }
      expect(Math.abs(labelBox.y + labelBox.height / 2 - headerBox.y - headerBox.height / 2)).toBeLessThanOrEqual(1);

      for (const icon of await header.locator('.ag-sort-indicator-icon:visible').all()) {
        const box = await icon.boundingBox();
        expect(box).not.toBeNull();
        if (box) {
          expect(Math.abs(box.y + box.height / 2 - labelBox.y - labelBox.height / 2)).toBeLessThanOrEqual(1);
        }
      }

      const renderer = cell.locator('.review-badge, .evidence-badge, .keywords-cell');
      if (await renderer.count()) {
        const box = await renderer.boundingBox();
        const cellBox = await cell.boundingBox();
        expect(box).not.toBeNull();
        expect(cellBox).not.toBeNull();
        if (box && cellBox) {
          expect(Math.abs(box.y + box.height / 2 - cellBox.y - cellBox.height / 2)).toBeLessThanOrEqual(1);
        }
      }
    }
  };

  await checkAlignment();
  const lengthHeader = grid.locator('.ag-header-cell[col-id="length"]');
  await lengthHeader.locator('.ag-header-cell-label').click();
  await expect(lengthHeader).toHaveAttribute('aria-sort', 'ascending');
  await checkAlignment();
  await lengthHeader.locator('.ag-header-cell-label').click();
  await expect(lengthHeader).toHaveAttribute('aria-sort', 'descending');
  await checkAlignment();
  expect(unexpectedRequests).toEqual([]);
});

