import {expect, test} from '@playwright/test';
import {randId, uiRegister} from './helpers';

test('E2E: chat session -> user msg -> ai msg (mock)', async ({page}) => {
  const sessionId = 12345;
  let nextMsgId = 1;

  const sessions: any[] = [
    {
      id: sessionId,
      userId: 1,
      conversationLanguageCode: 'en',
      title: 'E2E session',
      systemPrompt: null,
      startedAt: new Date().toISOString(),
      endedAt: null
    }
  ];

  const messages: any[] = [];

  await page.route('**/api/languages**', async (route) => {
    if (route.request().method() !== 'GET') return route.continue();
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {code: 'en', name: 'English'},
        {code: 'pl', name: 'Polski'}
      ])
    });
  });

  await page.route('**/api/users/me/chat-sessions**', async (route) => {
    if (route.request().method() !== 'GET') return route.continue();
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(sessions)
    });
  });

  await page.route('**/api/chat-sessions**', async (route) => {
    const req = route.request();
    const url = req.url();

    if (req.method() === 'POST' && /\/api\/chat-sessions$/.test(url)) {
      const created = {
        id: sessionId,
        userId: 1,
        conversationLanguageCode: 'en',
        title: 'E2E session',
        systemPrompt: null,
        startedAt: new Date().toISOString(),
        endedAt: null
      };

      sessions.unshift(created);

      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        headers: {Location: `/api/chat-sessions/${sessionId}`},
        body: JSON.stringify(created)
      });
      return;
    }

    if (req.method() === 'GET' && /\/api\/chat-sessions\/\d+$/.test(url)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(sessions[0])
      });
      return;
    }

    if (req.method() === 'GET' && /\/api\/chat-sessions\/\d+\/messages$/.test(url)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(messages)
      });
      return;
    }

    if (req.method() === 'POST' && /\/api\/chat-sessions\/\d+\/messages$/.test(url)) {
      const body = await req.postDataJSON();
      const created = {
        id: nextMsgId++,
        sessionId,
        sender: 'user',
        message: String(body?.message ?? ''),
        sentAt: new Date().toISOString()
      };
      messages.push(created);

      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        headers: {Location: `/api/chat-sessions/${sessionId}/messages/${created.id}`},
        body: JSON.stringify(created)
      });
      return;
    }

    if (req.method() === 'POST' && /\/api\/chat-sessions\/\d+\/ai-messages$/.test(url)) {
      const created = {
        id: nextMsgId++,
        sessionId,
        sender: 'ai',
        message: 'AI says hi',
        sentAt: new Date().toISOString()
      };
      messages.push(created);

      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        headers: {Location: `/api/chat-sessions/${sessionId}/messages/${created.id}`},
        body: JSON.stringify(created)
      });
      return;
    }

    return route.continue();
  });

  const login = randId('u');
  const email = `${login}@example.com`;
  await uiRegister(page, login, email, 'password');

  await page.goto('/chat/sessions');

  const titleInput = page.locator('app-chat-sessions input.form-control').first();
  await expect(titleInput).toBeVisible();
  await titleInput.fill('E2E session');

  const createBtn = page.locator('app-chat-sessions button.btn.btn-primary').first();
  await expect(createBtn).toBeEnabled();

  const createdSessionResp = page.waitForResponse(
    r => r.request().method() === 'POST' && r.url().includes('/api/chat-sessions') && r.status() === 201,
    {timeout: 10000}
  );

  await createBtn.click();
  await createdSessionResp;

  await expect(page).toHaveURL(new RegExp(`/chat/sessions/${sessionId}`));

  const textarea = page.locator('#msgInput');
  await expect(textarea).toBeVisible();
  await textarea.fill('Hello!');

  const sendBtn = page.locator('app-chat-session .card-footer button.btn.btn-primary').first();
  await expect(sendBtn).toBeEnabled();

  const aiResp = page.waitForResponse(
    r => r.request().method() === 'POST' && r.url().includes(`/api/chat-sessions/${sessionId}/ai-messages`) && r.status() === 201,
    {timeout: 10000}
  );

  await sendBtn.click();
  await aiResp;

  await expect(page.getByText('Hello!')).toBeVisible();
  await expect(page.getByText('AI says hi')).toBeVisible();
});
