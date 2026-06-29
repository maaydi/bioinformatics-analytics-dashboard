import {expect, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const mainRoutes = ['/login', '/', '/genes', '/analytics', '/saved-filters', '/admin/import'];

test.describe('A11Y (a11y) axe-core smoke tests', () => {
  for (const route of mainRoutes) {
    test(`should not have any automatically detectable serious or critical accessibility issues on ${route}`, async ({
                                                                                                                       page,
                                                                                                                     }) => {
      await page.goto(route);

      await page.waitForLoadState('networkidle');

      const accessibilityScanResults = await new AxeBuilder({page})
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze();

      const violations = accessibilityScanResults.violations.filter(
        (violation) => violation.impact === 'serious' || violation.impact === 'critical',
      );

      if (violations.length > 0) {
        console.error(`Accessibility violations on ${route}:`, JSON.stringify(violations, null, 2));
      }
      expect(violations).toEqual([]);
    });
  }
});
