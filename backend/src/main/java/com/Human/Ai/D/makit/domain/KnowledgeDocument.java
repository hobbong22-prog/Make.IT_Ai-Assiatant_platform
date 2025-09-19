package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {
    
    @Id
    private String documentId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(nullable = false)
    private String documentType;
    
    private String source;
    
    @ElementCollection
    @CollectionTable(name = "knowledge_document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag")
    private List<String> tags;
    
    @Column(nullable = false)
    private LocalDateTime indexedAt;
    
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
    
    @Column(columnDefinition = "TEXT")
    private String embeddingVector;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;
    
    // Constructors
    public KnowledgeDocument() {}
    
    public KnowledgeDocument(String documentId, String title, String content, String documentType) {
        this.documentId = documentId;
        this.title = title;
        this.content = content;
        this.documentType = documentType;
        this.indexedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.status = DocumentStatus.PENDING;
    }
    
    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public LocalDateTime getIndexedAt() {
        return indexedAt;
    }
    
    public void setIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public String getEmbeddingVector() {
        return embeddingVector;
    }
    
    public void setEmbeddingVector(String embeddingVector) {
        this.embeddingVector = embeddingVector;
    }
    
    public DocumentStatus getStatus() {
        return status;
    }
    
    public void setStatus(DocumentStatus status) {
        this.status = status;
    }
    
    public enum DocumentStatus {
        PENDING, INDEXED, FAILED, ARCHIVED
    }
}