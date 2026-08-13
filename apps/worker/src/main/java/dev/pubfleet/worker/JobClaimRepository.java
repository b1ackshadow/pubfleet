package dev.pubfleet.worker;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The claim. This is the whole of the worker's ownership of a job row.
 *
 * <p><b>This claim races, on purpose.</b> {@link #findPendingIds} is a plain SELECT
 * with no {@code FOR UPDATE} and no {@code SKIP LOCKED}, so two workers can read the
 * same id. {@link #claim} carries no lease and no expiry, so a worker that dies after
 * claiming strands the job in CLAIMED for ever.
 *
 * <p>The compare-and-set in the WHERE clause keeps the race safe rather than correct:
 * only one UPDATE can see {@code status='PENDING'}, so only one worker wins, and the
 * loser gets 0 rows and moves on. It does nothing for the stranded job.
 *
 * <p>Do not fix either problem here. Both belong to unit 01. See
 * {@code units/00-walking-skeleton/CONTEXT.md}, "Open risks".
 */
@Repository
public class JobClaimRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JobClaimRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Candidate ids for this poll.
     *
     * <p>Deliberately a plain read. No row lock is taken, so another worker may read
     * the same ids in the same moment. See the class note.
     */
    public List<UUID> findPendingIds(int limit) {
        return jdbc.queryForList("""
                SELECT id FROM jobs
                WHERE status = 'PENDING'
                ORDER BY created_at
                LIMIT :limit
                """, Map.of("limit", limit), UUID.class);
    }

    /**
     * Moves one job from PENDING to CLAIMED.
     *
     * <p>The {@code AND status='PENDING'} clause is the compare-and-set that settles
     * the race. No lease column, no expiry column: unit 01 adds those.
     *
     * @return true when this worker won the row
     */
    public boolean claim(UUID id, String workerId) {
        int updated = jdbc.update(
                "UPDATE jobs SET status='CLAIMED', worker_id=:workerId, updated_at=now() "
                        + "WHERE id=:id AND status='PENDING'",
                new MapSqlParameterSource()
                        .addValue("workerId", workerId)
                        .addValue("id", id));

        return updated == 1;
    }

    /** The work pointer of a claimed job. */
    public String findPayloadRef(UUID id) {
        try {
            return jdbc.queryForObject(
                    "SELECT payload_ref FROM jobs WHERE id = :id",
                    Map.of("id", id), String.class);
        } catch (EmptyResultDataAccessException missing) {
            return null;
        }
    }
}
