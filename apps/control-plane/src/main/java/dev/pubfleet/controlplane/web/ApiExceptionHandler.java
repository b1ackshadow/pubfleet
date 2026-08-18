package dev.pubfleet.controlplane.web;

import dev.pubfleet.controlplane.domain.IllegalJobTransitionException;
import dev.pubfleet.controlplane.domain.InvalidJobRequestException;
import dev.pubfleet.controlplane.domain.JobNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * Every error leaves as an RFC 9457 problem detail, so the console reads one shape
 * whatever went wrong. Spring writes these as {@code application/problem+json}.
 *
 * <p>The application errors below are only half of that promise. A bad path variable, a
 * body Jackson cannot read, a wrong HTTP method and a missing route are raised by Spring
 * itself, before any code here runs. Extending
 * {@link ResponseEntityExceptionHandler} is what turns those into problem details too;
 * without it they leave as Boot's own {@code timestamp/status/error/path} body, which the
 * console cannot read. {@code spring.mvc.problemdetails.enabled} makes the same choice
 * for the rest of the application.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_BASE = "https://pubfleet.dev/problems/";

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(JobNotFoundException.class)
    public ProblemDetail onJobNotFound(JobNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Job not found");
        problem.setType(URI.create(PROBLEM_BASE + "job-not-found"));
        problem.setProperty("jobId", exception.jobId().toString());

        return problem;
    }

    @ExceptionHandler(IllegalJobTransitionException.class)
    public ProblemDetail onIllegalTransition(IllegalJobTransitionException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Illegal job status transition");
        problem.setType(URI.create(PROBLEM_BASE + "illegal-job-transition"));
        problem.setProperty("jobId", exception.jobId().toString());
        problem.setProperty("from", String.valueOf(exception.from()));
        problem.setProperty("to", String.valueOf(exception.to()));

        return problem;
    }

    /**
     * The one case where the caller is told what was wrong with the request. The type is
     * raised by this application only, so its message is written for the caller.
     */
    @ExceptionHandler(InvalidJobRequestException.class)
    public ProblemDetail onInvalidRequest(InvalidJobRequestException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid request");
        problem.setType(URI.create(PROBLEM_BASE + "invalid-request"));
        problem.setProperty("field", exception.field());

        return problem;
    }

    /**
     * Anything left is a fault of this server, not of the caller.
     *
     * <p>The detail is generic on purpose. An internal message can name a table, a
     * class, or a value, and the caller has no use for any of it. The real message goes
     * to the log, where it can be found by the request that carried it.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception exception) {
        log.error("Unhandled exception on the API surface", exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The control plane could not complete the request.");
        problem.setTitle("Internal server error");
        problem.setType(URI.create(PROBLEM_BASE + "internal-error"));

        return problem;
    }
}
