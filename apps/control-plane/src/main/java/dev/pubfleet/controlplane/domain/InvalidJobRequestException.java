package dev.pubfleet.controlplane.domain;

/**
 * Thrown when the caller sent a job the API cannot accept. The web layer turns this
 * into a 400, and its message is safe to show the caller.
 *
 * <p>It is not an {@link IllegalArgumentException} on purpose. That type is also thrown
 * by libraries and by internal bugs, so mapping it to 400 would report a server fault as
 * a client fault and would send an internal message back over the wire.
 */
public class InvalidJobRequestException extends RuntimeException {

    private final transient String field;

    public InvalidJobRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** The request field that was wrong. */
    public String field() {
        return field;
    }
}
