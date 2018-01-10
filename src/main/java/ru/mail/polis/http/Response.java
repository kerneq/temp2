package ru.mail.polis.http;

public class Response {

    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int ACCEPTED = 202;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int NOT_ALLOWED = 405;
    public static final int SERVER_ERROR = 500;
    public static final int NOT_ENOUGH_REPLICAS = 504;

    private final int code;
    private final byte[] data;

    public Response(int code) {
        this.code = code;
        this.data = null;
    }

    public Response(int code, String data) {
        this.code = code;
        this.data = data.getBytes();
    }

    public Response(int code, byte[] data) {
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public boolean hasData() {
        return data != null;
    }

    public byte[] getData() {
        return data;
    }
}
