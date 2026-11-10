package com.temple.erp.model;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "templeupdates")
public class TempleUpdate {
    @Id
    private String id;
    private String title;
    private String description;
    @Field("short_description") private String shortDescription;
    @Field("update_type") private String updateType;
    @Field("event_date") private LocalDate eventDate;
    @Field("event_end_date") private LocalDate eventEndDate;
    private String image;
    @Field("is_featured") private Boolean isFeatured = false;
    private String status = "active";
    @Field("created_by") private String createdBy;
    @Field("updated_by") private String updatedBy;
    @CreatedDate @Field("created_at") private Instant createdAt;
    @LastModifiedDate @Field("updated_at") private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getUpdateType() { return updateType; }
    public void setUpdateType(String updateType) { this.updateType = updateType; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public LocalDate getEventEndDate() { return eventEndDate; }
    public void setEventEndDate(LocalDate eventEndDate) { this.eventEndDate = eventEndDate; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
