package com.cidercollective.cider.sidcloud;

/** Failure of a cloud Sid call. {@link #code} is the Sid error code Cider's UI routes on. */
public final class CloudSidException extends Exception {
    /** Cider shows "Sid is busy right now" for this code. */
    public static final int BUSY = 9;
    /** Unlisted code: Cider shows its generic "couldn't think that through" message. */
    public static final int FAILED = 100;

    public final int code;

    public CloudSidException(int code, String message) {
        super(message);
        this.code = code;
    }
}
