import {expect, test} from '@playwright/test';
import {randId, uiLogin, uiLogout, uiRegister} from './helpers';

test('E2E: 401 -> refresh(cookie) -> retry protected request', async ({page}) => {
  const login = randId('u');
  const email = `${login}@example.com`;
  const password = 'password';

  await uiRegister(page, login, email, password);

  await page.goto('/chat/sessions', {waitUntil: 'domcontentloaded'});
  await expect(page.locator('app-chat-sessions')).toBeVisible();

  let first401 = true;

  await page.route('**/api/users/me/chat-sessions**', async (route) => {
    if (route.request().method() !== 'GET') return route.continue();

    if (first401) {
      first401 = false;
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({code: 'UNAUTHORIZED'})
      });
      return;
    }

    await route.continue();
  });

  const refresh200 = page.waitForResponse(
    r => r.request().method() === 'POST' && r.url().includes('/api/auth/refresh') && r.status() === 200,
    {timeout: 20000}
  );

  const sessions200 = page.waitForResponse(
    r => r.request().method() === 'GET' && r.url().includes('/api/users/me/chat-sessions') && r.status() === 200,
    {timeout: 20000}
  );

  await page.reload({waitUntil: 'domcontentloaded'});

  await refresh200;
  await sessions200;

  await expect(page).toHaveURL(/\/chat\/sessions/);

  await uiLogout(page);
  await uiLogin(page, login, password);
  await expect(page).toHaveURL(/\/dashboard/);
});
