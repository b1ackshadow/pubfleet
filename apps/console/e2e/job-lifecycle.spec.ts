import { expect, test, type Locator } from '@playwright/test'

/**
 * The console proof of unit 00. One browser, one job, one live stack.
 *
 * This test never stubs `/api`. Every request goes through the Vite proxy to the
 * running control plane, and the status the browser shows is the status a worker put
 * in Postgres and reported over Kafka. The failure mode it guards is the console
 * missing a state change: a job that finishes on the backend while the table keeps
 * showing an old status.
 */

/** How long to wait for the whole chain: claim, work, publish, consume, poll. */
const LIFECYCLE_TIMEOUT_MS = 45_000

/**
 * How often the test reads the table. The console refetches once a second, so a read
 * every 50 ms sees every state the console renders and misses none of them.
 */
const READ_INTERVAL_MS = 50

/** The job life, in order. FAILED is not here: this test proves the happy path. */
const LIFECYCLE = ['PENDING', 'CLAIMED', 'SUCCEEDED']

test('a submitted job appears while it is still running and reaches SUCCEEDED', async ({ page }) => {
  // The supplier id is the only field of the new row that the table shows and that
  // the test controls, so it is what makes this run's row findable among the rest.
  const supplierId = `e2e-${Date.now()}-${Math.floor(Math.random() * 1000)}`
  const payloadRef = `s3://bucket/${supplierId}`

  await page.goto('/')

  const form = page.getByTestId('submit-form')
  await expect(form).toBeVisible()

  await page.getByTestId('supplier-input').fill(supplierId)
  await page.getByTestId('payload-input').fill(payloadRef)
  await page.getByTestId('submit-button').click()

  const row = page.getByTestId('job-row').filter({ hasText: supplierId })

  // Collect every status the console renders, from the click onwards. Recording the
  // states is what makes the assertions below mean anything: the job holds each state
  // for well under a second, so an assertion that waits for one state at a time could
  // arrive after that state had gone, and would then be reporting the test's own
  // timing rather than the console's behaviour.
  const rendered: string[] = []

  await expect
    .poll(
      async () => {
        const status = await readStatus(row)
        if (status !== null && status !== rendered.at(-1)) rendered.push(status)
        return rendered.at(-1)
      },
      {
        message: `job for supplier ${supplierId} never reached SUCCEEDED; `
          + 'is the worker running against the same Postgres and Kafka?',
        timeout: LIFECYCLE_TIMEOUT_MS,
        intervals: [READ_INTERVAL_MS],
      },
    )
    .toBe('SUCCEEDED')

  const trail = `statuses the console rendered: ${rendered.join(' -> ')}`

  // The row must appear while the job is still alive, and PENDING is what the operator
  // normally sees. CLAIMED is allowed as a first state for one reason: the worker polls
  // Postgres every 500 ms and the console draws the new row about 50 ms after the
  // submit, so a poll that lands inside that gap leaves no PENDING row to draw. This
  // was measured, not assumed: it happened twice in thirteen runs. A first state of
  // SUCCEEDED is always wrong. It means the operator saw nothing until the job was over,
  // which is the failure this test exists to catch.
  expect(['PENDING', 'CLAIMED'], trail).toContain(rendered[0])

  // The console must walk the life forwards and never show a state it has already left.
  // A repeat or a step backwards means the table is drawing a stale read.
  const walk = rendered.map((status) => LIFECYCLE.indexOf(status))
  expect(walk, trail).not.toContain(-1)
  expect(walk, trail).toEqual([...walk].sort((left, right) => left - right))

  // Exactly one row, and the result the worker computed. The result is the payload
  // reference in upper case, so a matching value proves the payload crossed the whole
  // chain and came back, rather than the row simply turning green.
  await expect(row).toHaveCount(1)
  await expect(row).toContainText(payloadRef.toUpperCase())
})

/** The status the row shows now, or null while the row is not on the page yet. */
async function readStatus(row: Locator): Promise<string | null> {
  const status = row.getByTestId('job-status')

  if ((await status.count()) === 0) return null

  const text = await status.first().textContent()

  return text === null ? null : text.trim()
}
