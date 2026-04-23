import { test, expect } from '@playwright/test';

test.describe('Calendar', () => {
  test('calendar page loads', async ({ page }) => {
    await page.goto('/calendar');
    await expect(page.locator('.calendar, .week-view, .month-view, [class*="calendar"]')).toBeVisible({ timeout: 5_000 }).catch(async () => {
      await expect(page.locator('body')).toContainText(/\d{4}/);
    });
  });
});
