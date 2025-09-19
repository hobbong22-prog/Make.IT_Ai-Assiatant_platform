package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.KnowledgeDocument;
import com.Human.Ai.D.makit.service.KnowledgeBaseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/knowledge-base")
@CrossOrigin(origins = "*")
public class KnowledgeBaseController {
    
    @Autowired
    private KnowledgeBaseManager knowledgeBaseManager;
    
    @PostMapping("/documents")
    public CompletableFuture<ResponseEntity<KnowledgeDocument>> addDocument(
            @RequestBody AddDocumentRequest request) {
        
        return knowledgeBaseManager.addDocument(
                request.getTitle(),
                request.getContent(),
                request.getDocumentType(),
                request.getSource(),
                request.getTags()
        ).thenApply(document -> ResponseEntity.ok(document))
         .exceptionally(ex -> ResponseEntity.badRequest().build());
    }
    
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<KnowledgeDocument> getDocument(@PathVariable String documentId) {
        Optional<KnowledgeDocument> document = knowledgeBaseManager.getDocument(documentId);
        return document.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/documents")
    public ResponseEntity<List<KnowledgeDocument>> getAllDocuments() {
        List<KnowledgeDocument> documents = knowledgeBaseManager.getIndexedDocuments();
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/documents/search")
    public ResponseEntity<List<KnowledgeDocument>> searchDocuments(@RequestParam String keyword) {
        List<KnowledgeDocument> documents = knowledgeBaseManager.searchDocuments(keyword);
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/documents/by-tag")
    public ResponseEntity<List<KnowledgeDocument>> getDocumentsByTag(@RequestParam String tag) {
        List<KnowledgeDocument> documents = knowledgeBaseManager.getDocumentsByTag(tag);
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/documents/by-type")
    public ResponseEntity<List<KnowledgeDocument>> getDocumentsByType(@RequestParam String type) {
        List<KnowledgeDocument> documents = knowledgeBaseManager.getDocumentsByType(type);
        return ResponseEntity.ok(documents);
    }
    
    @PutMapping("/documents/{documentId}")
    public CompletableFuture<ResponseEntity<KnowledgeDocument>> updateDocument(
            @PathVariable String documentId,
            @RequestBody UpdateDocumentRequest request) {
        
        return knowledgeBaseManager.updateDocument(
                documentId,
                request.getTitle(),
                request.getContent(),
                request.getTags()
        ).thenApply(document -> ResponseEntity.ok(document))
         .exceptionally(ex -> ResponseEntity.badRequest().build());
    }
    
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        try {
            knowledgeBaseManager.deleteDocument(documentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/rebuild-index")
    public CompletableFuture<ResponseEntity<String>> rebuildIndex() {
        return knowledgeBaseManager.rebuildIndex()
                .thenApply(v -> ResponseEntity.ok("Index rebuild started"))
                .exceptionally(ex -> ResponseEntity.badRequest().body("Failed to rebuild index"));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<KnowledgeBaseManager.KnowledgeBaseStats> getStats() {
        KnowledgeBaseManager.KnowledgeBaseStats stats = knowledgeBaseManager.getStats();
        return ResponseEntity.ok(stats);
    }
    
    // Request DTOs
    public static class AddDocumentRequest {
        private String title;
        private String content;
        private String documentType;
        private String source;
        private List<String> tags;
        
        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
    
    public static class UpdateDocumentRequest {
        private String title;
        private String content;
        private List<String> tags;
        
        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
}