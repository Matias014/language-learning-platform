import {APIRequestContext, expect, Page} from '@playwright/test';

export type SeedResult = {
  courseId: number;
  lessonId: number;
  exerciseId: number;
  correctOptionId: number;
};

export const UI_BASE_URL = process.env['E2E_BASE_URL'] ?? 'http://localhost:4200';
export const API_BASE_URL = process.env['E2E_API_BASE_URL'] ?? 'http://localhost:8080';

export function randId(prefix: string): string {
  return `${prefix}_${Math.random().toString(16).slice(2)}_${Date.now()}`;
}

async function gotoAndWait(page: Page, path: string, mustHaveSelector: string): Promise<void> {
  await page.goto(path, {waitUntil: 'domcontentloaded'});
  await expect(page.locator(mustHaveSelector)).toBeVisible();
}

async function clickSubmit(page: Page): Promise<void> {
  const btn = page.locator('form button[type="submit"], form input[type="submit"]');
  if (await btn.count()) {
    await expect(btn.first()).toBeVisible();
    await btn.first().click();
    return;
  }
  await page.keyboard.press('Enter');
}

export async function uiRegister(page: Page, login: string, email: string, password: string): Promise<void> {
  await gotoAndWait(page, '/register', '#loginInput');

  await page.locator('#loginInput').fill(login);
  await page.locator('#emailInput').fill(email);
  await page.locator('#passwordInput').fill(password);

  const confirm = page.locator('#confirmInput');
  if (await confirm.count()) {
    await confirm.fill(password);
  }

  const name = page.locator('#nameInput');
  if (await name.count()) {
    await name.fill('Test');
  }

  const surname = page.locator('#surnameInput');
  if (await surname.count()) {
    await surname.fill('User');
  }

  await clickSubmit(page);
  await expect(page).toHaveURL(/\/dashboard/);
}

export async function uiLogin(page: Page, loginOrEmail: string, password: string): Promise<void> {
  await gotoAndWait(page, '/login', '#loginInput');

  await page.locator('#loginInput').fill(loginOrEmail);
  await page.locator('#passwordInput').fill(password);

  await clickSubmit(page);
  await expect(page).toHaveURL(/\/dashboard/);
}

export async function uiLogout(page: Page): Promise<void> {
  const dropdownToggle = page.locator('.dropdown > button.dropdown-toggle');
  if (await dropdownToggle.count()) {
    await dropdownToggle.first().click();
  }

  const logoutBtn = page.locator('.dropdown-menu button.dropdown-item.text-danger');
  if (await logoutBtn.count()) {
    await logoutBtn.first().click();
    await expect(page).toHaveURL(/\/login/);
    return;
  }

  const anyItem = page.locator('.dropdown-menu button.dropdown-item');
  const c = await anyItem.count();
  for (let i = 0; i < c; i++) {
    const item = anyItem.nth(i);
    const txt = (await item.innerText()).toLowerCase();
    if (txt.includes('logout') || txt.includes('wylog')) {
      await item.click();
      await expect(page).toHaveURL(/\/login/);
      return;
    }
  }

  await page.context().clearCookies();
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch {}
  });
  await page.goto('/login', {waitUntil: 'domcontentloaded'});
  await expect(page).toHaveURL(/\/login/);
}

export async function clearJwtFromStorage(page: Page): Promise<void> {
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch {}
  });
}

export async function invalidateJwtInStorage(page: Page): Promise<void> {
  await page.evaluate(() => {
    const jwtRe = /^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+$/;

    const findAndReplace = (storage: Storage): boolean => {
      for (let i = 0; i < storage.length; i++) {
        const k = storage.key(i);
        if (!k) continue;
        const v = storage.getItem(k);
        if (v && jwtRe.test(v)) {
          storage.setItem(k, 'invalid.invalid.invalid');
          return true;
        }
      }
      return false;
    };

    const ok = findAndReplace(localStorage) || findAndReplace(sessionStorage);
    if (!ok) {
      try {
        localStorage.clear();
        sessionStorage.clear();
      } catch {}
    }
  });
}

export async function adminToken(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${API_BASE_URL}/api/auth/login`, {
    data: {loginOrEmail: 'admin', password: 'password'}
  });

  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`adminToken failed: ${res.status()} ${body}`);
  }

  const body = await res.json();
  return String(body.accessToken);
}

export async function seedCourseLessonQuiz(request: APIRequestContext, token: string): Promise<SeedResult> {
  const courseRes = await request.post(`${API_BASE_URL}/api/courses`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {
      title: `E2E ${randId('course')}`,
      description: 'E2E course',
      learningLanguageCode: 'en',
      fromLanguageCode: 'pl',
      levelCode: 'A1'
    }
  });

  if (!courseRes.ok()) {
    const t = await courseRes.text();
    throw new Error(`create course failed: ${courseRes.status()} ${t}`);
  }

  const course = await courseRes.json();
  const courseId = Number(course.id);

  const lessonRes = await request.post(`${API_BASE_URL}/api/lessons`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {
      courseId,
      title: `E2E ${randId('lesson')}`,
      description: 'E2E lesson',
      orderNumber: 1
    }
  });

  if (!lessonRes.ok()) {
    const t = await lessonRes.text();
    throw new Error(`create lesson failed: ${lessonRes.status()} ${t}`);
  }

  const lesson = await lessonRes.json();
  const lessonId = Number(lesson.id);

  const exRes = await request.post(`${API_BASE_URL}/api/exercises`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {
      lessonId,
      orderNumber: 1,
      type: 'quiz',
      difficulty: 'easy',
      question: 'Pick one',
      sampleAnswer: 'A',
      xp: 10
    }
  });

  if (!exRes.ok()) {
    const t = await exRes.text();
    throw new Error(`create exercise failed: ${exRes.status()} ${t}`);
  }

  const ex = await exRes.json();
  const exerciseId = Number(ex.id);

  const o1Res = await request.post(`${API_BASE_URL}/api/exercise-options`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {exerciseId, orderNumber: 1, content: 'A'}
  });

  if (!o1Res.ok()) {
    const t = await o1Res.text();
    throw new Error(`create option 1 failed: ${o1Res.status()} ${t}`);
  }

  const o1 = await o1Res.json();
  const correctOptionId = Number(o1.id);

  const o2Res = await request.post(`${API_BASE_URL}/api/exercise-options`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {exerciseId, orderNumber: 2, content: 'B'}
  });

  if (!o2Res.ok()) {
    const t = await o2Res.text();
    throw new Error(`create option 2 failed: ${o2Res.status()} ${t}`);
  }

  const patchRes = await request.patch(`${API_BASE_URL}/api/exercises/${exerciseId}`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {correctOptionId}
  });

  if (!patchRes.ok()) {
    const t = await patchRes.text();
    throw new Error(`patch exercise failed: ${patchRes.status()} ${t}`);
  }

  return {courseId, lessonId, exerciseId, correctOptionId};
}
