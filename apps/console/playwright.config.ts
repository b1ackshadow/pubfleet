import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright runs against the live stack, never against mocks.
 *
 * The backend is assumed to be running already. This config starts the Vite dev
 * server and nothing else, so before `npm run test:e2e` you must have, from the
 * repository root:
 *
 *   docker compose up -d
 *   ./mvnw -pl apps/control-plane spring-boot:run
 *   ./mvnw -pl apps/worker spring-boot:run
 *
 * A `webServer` entry for the control plane and the worker would hide the point of
 * this test. It exists to prove that two real processes, Postgres and Kafka carry a
 * job from submit to terminal state and that the console sees it. A test that boots
 * its own backend, or one that stubs `/api`, would pass with the backend switched
 * off, which is the failure this unit is guarding against.
 *
 * The console talks to the control plane through the Vite proxy in `vite.config.ts`,
 * which targets `PUBFLEET_API_URL` or port 8081. Port 8081 is the default of the
 * control plane. Nothing here names a port for the backend, so nothing here has to
 * change when the backend moves.
 */

/** Where the console is served. The Vite dev server below listens here. */
const CONSOLE_URL = process.env.PUBFLEET_CONSOLE_URL ?? 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',

  // One worker and no parallel files. The stack is shared, and a second browser
  // submitting jobs at the same time would make the job table hard to reason about.
  fullyParallel: false,
  workers: 1,

  // No retries. A retry on a live-stack test turns a real race into a green build.
  retries: 0,

  forbidOnly: Boolean(process.env.CI),
  timeout: 90_000,
  expect: { timeout: 30_000 },
  reporter: [['list']],

  use: {
    baseURL: CONSOLE_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },

  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],

  webServer: {
    // The dev server only. See the note above.
    command: 'npm run dev -- --port 5173 --strictPort',
    url: CONSOLE_URL,
    // Locally the developer usually has `npm run dev` up already. On CI there is
    // nothing to reuse, and reusing a stale server would be a silent wrong answer.
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    stdout: 'pipe',
    stderr: 'pipe',
  },
})
