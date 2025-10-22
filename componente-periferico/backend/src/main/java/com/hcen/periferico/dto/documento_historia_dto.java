package com.hcen.periferico.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class documento_historia_dto {
    private String id;
    private String title;
    private Instant createdAt;
    private List<String> formats = new ArrayList<>();

    public documento_historia_dto() {}

    public documento_historia_dto(String id, String title, Instant createdAt, List<String> formats) {
        this.id = id; this.title = title; this.createdAt = createdAt; this.formats = formats;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<String> getFormats() { return formats; }
    public void setFormats(List<String> formats) { this.formats = formats; }
}

