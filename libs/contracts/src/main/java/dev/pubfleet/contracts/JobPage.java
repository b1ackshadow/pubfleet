package dev.pubfleet.contracts;

import java.util.List;
import java.util.UUID;

/**
 * One page of jobs, newest first.
 *
 * <p>The order is {@code (created_at, id)} descending. Give {@code nextCursor} back as
 * the {@code after} query parameter to get the next page. A null cursor means the last
 * page.
 */
public record JobPage(List<JobView> items, UUID nextCursor) {
}
