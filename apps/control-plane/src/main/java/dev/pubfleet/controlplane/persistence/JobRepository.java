package dev.pubfleet.controlplane.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Keyset pagination over {@code (created_at, id)} descending.
 *
 * <p>An offset walks every skipped row and shifts under a concurrent insert. A keyset
 * carries the sort key of the last row instead, so the next page starts with an index
 * seek and stays stable. The cursor on the wire is the job id only, so the query reads
 * the {@code created_at} of that id back from the table.
 */
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    @Query(value = """
            SELECT * FROM jobs
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<JobEntity> findFirstPage(@Param("limit") int limit);

    /**
     * The page after {@code after}. An unknown cursor gives an empty page, because the
     * row comparison against an empty subquery is unknown for every row.
     */
    @Query(value = """
            SELECT * FROM jobs
            WHERE (created_at, id) < (SELECT created_at, id FROM jobs WHERE id = :after)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<JobEntity> findPageAfter(@Param("after") UUID after, @Param("limit") int limit);
}
