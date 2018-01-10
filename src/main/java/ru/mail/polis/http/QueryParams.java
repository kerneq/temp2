package ru.mail.polis.http;

public class QueryParams {

    private final String id;
    private final int ack;
    private final int from;

    public QueryParams(String id, int ack, int from) {
        this.id = id;
        this.ack = ack;
        this.from = from;
    }

    public String getId() {
        return id;
    }

    public int getAck() {
        return ack;
    }

    public int getFrom() {
        return from;
    }
}
