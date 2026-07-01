import { expect, test } from '@playwright/test';
import { adminToken, randId, seedCourseLessonQuiz, uiRegister } from './helpers';

test('E2E: lesson -> attempt -> evaluation(mock) -> hints(mock)', async ({ page, request }) => {
  const token = await adminToken(request);
  const seed = await seedCourseLessonQuiz(request, token);

  await page.route('**/api/exercise-attempts/*/evaluations', async (route) => {
    const url = route.request().url();
    const m = url.match(/exercise-attempts\/(\d+)\/evaluations/);
    const attemptId = m ? Number(m[1]) : 0;

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        correct: true,
        feedback: 'OK',
        hints: ['h1'],
        attemptId,
        awardedXp: 10,
      }),
    });
  });

  await page.route('**/api/ai/hints', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        correct: true,
        feedback: 'Hint OK',
        hints: ['Try this'],
      }),
    });
  });

  const login = randId('u');
  const email = `${login}@example.com`;
  await uiRegister(page, login, email, 'password');

  await page.goto(`/courses/${seed.courseId}`);

  const enrollBtn = page.locator('button.btn.btn-primary').first();
  await expect(enrollBtn).toBeVisible();
  await enrollBtn.click();

  const openLesson = page.locator('a.btn.btn-outline-primary').first();
  await expect(openLesson).toBeVisible();
  await openLesson.click();

  await expect(page).toHaveURL(new RegExp(`/lessons/${seed.lessonId}`));

  const quiz = page.locator('app-quiz-exercise').first();
  await expect(quiz).toBeVisible();

  const firstOptionBtn = quiz.locator('button.list-group-item').first();
  await expect(firstOptionBtn).toBeVisible();
  await firstOptionBtn.click();

  const submitBtn = quiz.locator('button.btn.btn-primary').first();
  await expect(submitBtn).toBeEnabled();
  await submitBtn.click();

  await expect(page.getByText('OK')).toBeVisible();

  const hintBtn = quiz.locator('button.btn.btn-outline-info').first();
  await expect(hintBtn).toBeVisible();
  await hintBtn.click();

  await expect(page.getByText('Hint OK')).toBeVisible();
});
