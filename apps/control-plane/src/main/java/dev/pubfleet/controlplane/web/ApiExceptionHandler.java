package dev.pubfleet.controlplane.web;

import dev.pubfleet.controlplane.domain.IllegalJobTransitionException;
import dev.pubfleet.controlplane.domain.JobNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Every error leaves as an RFC 9457 problem detail, so the console reads one shape
 * whatever went wrong. Spring writes these as {@code application/problem+json}.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String PROBLEM_BASE = "https://pubfleet.dev/problems/";

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onBadRequest(IllegalArgumentException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid request");
        problem.setType(URI.create(PROBLEM_BASE + "invalid-request"));

        return problem;
    }
}
