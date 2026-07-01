import {expect, test} from '@playwright/test';
import {randId, uiLogin, uiLogout, uiRegister} from './helpers';

test('E2E: admin can export hardest-exercises PDF and LLM CSV', async ({page}) => {
  await uiLogin(page, 'admin', 'password');
  await page.goto('/admin/reports');

  const cards = page.locator('.row.g-3 > .col-lg-6');
  const llmCard = cards.first();
  const hardestCard = cards.nth(1);

  const llmCsvRespPromise = page.waitForResponse(r => {
    const u = r.url();
    return u.includes('/admin/stats/llm/export') && !u.includes('/admin/stats/llm/export.pdf');
  });
  await llmCard.getByRole('button', {name: 'CSV'}).click();
  const llmCsvResp = await llmCsvRespPromise;
  expect(llmCsvResp.ok()).toBeTruthy();

  const llmPdfRespPromise = page.waitForResponse(r => r.url().includes('/admin/stats/llm/export.pdf'));
  await llmCard.getByRole('button', {name: 'PDF'}).click();
  const llmPdfResp = await llmPdfRespPromise;
  expect(llmPdfResp.ok()).toBeTruthy();

  const hardestCsvRespPromise = page.waitForResponse(r => {
    const u = r.url();
    return u.includes('/admin/stats/exercises/hardest/export') && !u.includes('/admin/stats/exercises/hardest/export.pdf');
  });
  await hardestCard.getByRole('button', {name: 'CSV'}).click();
  const hardestCsvResp = await hardestCsvRespPromise;
  expect(hardestCsvResp.ok()).toBeTruthy();

  const hardestPdfRespPromise = page.waitForResponse(r => r.url().includes('/admin/stats/exercises/hardest/export.pdf'));
  await hardestCard.getByRole('button', {name: 'PDF'}).click();
  const hardestPdfResp = await hardestPdfRespPromise;
  expect(hardestPdfResp.ok()).toBeTruthy();
});

test('E2E: non-admin entering /admin is blocked (403 page)', async ({page}) => {
  const login = randId('u');
  const email = `${login}@example.com`;
  await uiRegister(page, login, email, 'password');

  await page.goto('/admin');

  const h403 = page.getByRole('heading', {name: '403'});
  await expect(h403).toBeVisible();

  await uiLogout(page);
});
