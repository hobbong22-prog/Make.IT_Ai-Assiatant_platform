package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.KnowledgeDocument;
import com.Human.Ai.D.makit.repository.KnowledgeDocumentRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class KnowledgeBaseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseManager.class);
    
    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    
    @Autowired
    private BedrockService bedrockService;
    
    /**
     * 새 문서를 지식 베이스에 추가하고 인덱싱합니다.
     */
    public CompletableFuture<KnowledgeDocument> addDocument(String title, String content, 
                                                          String documentType, String source, 
                                                          List<String> tags) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String documentId = UUID.randomUUID().toString();
                
                KnowledgeDocument document = new KnowledgeDocument(documentId, title, content, documentType);
                document.setSource(source);
                document.setTags(tags);
                
                // 문서 저장
                document = knowledgeDocumentRepository.save(document);
                
                // 비동기로 임베딩 생성
                generateEmbedding(document);
                
                logger.info("Document added to knowledge base: {}", documentId);
                return document;
                
            } catch (Exception e) {
                logger.error("Error adding document to knowledge base", e);
                throw new RuntimeException("Failed to add document to knowledge base", e);
            }
        });
    }
    
    /**
     * 문서의 임베딩을 생성하고 업데이트합니다.
     */
    public CompletableFuture<Void> generateEmbedding(KnowledgeDocument document) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Bedrock 임베딩 모델을 사용하여 임베딩 생성
                String embeddingText = document.getTitle() + " " + document.getContent();
                String embedding = bedrockService.generateEmbedding(embeddingText);
                
                document.setEmbeddingVector(embedding);
                document.setStatus(KnowledgeDocument.DocumentStatus.INDEXED);
                document.setLastUpdated(LocalDateTime.now());
                
                knowledgeDocumentRepository.save(document);
                
                logger.info("Embedding generated for document: {}", document.getDocumentId());
                
            } catch (Exception e) {
                logger.error("Error generating embedding for document: {}", document.getDocumentId(), e);
                document.setStatus(KnowledgeDocument.DocumentStatus.FAILED);
                knowledgeDocumentRepository.save(document);
            }
        });
    }
    
    /**
     * 문서 ID로 문서를 검색합니다.
     */
    public Optional<KnowledgeDocument> getDocument(String documentId) {
        return knowledgeDocumentRepository.findById(documentId);
    }
    
    /**
     * 키워드로 문서를 검색합니다.
     */
    public List<KnowledgeDocument> searchDocuments(String keyword) {
        return knowledgeDocumentRepository.findByKeyword(keyword);
    }
    
    /**
     * 태그로 문서를 검색합니다.
     */
    public List<KnowledgeDocument> getDocumentsByTag(String tag) {
        return knowledgeDocumentRepository.findByTag(tag);
    }
    
    /**
     * 문서 유형으로 문서를 검색합니다.
     */
    public List<KnowledgeDocument> getDocumentsByType(String documentType) {
        return knowledgeDocumentRepository.findByDocumentType(documentType);
    }
    
    /**
     * 모든 인덱싱된 문서를 반환합니다.
     */
    public List<KnowledgeDocument> getIndexedDocuments() {
        return knowledgeDocumentRepository.findIndexedDocuments();
    }
    
    /**
     * 문서를 업데이트합니다.
     */
    public CompletableFuture<KnowledgeDocument> updateDocument(String documentId, String title, 
                                                             String content, List<String> tags) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<KnowledgeDocument> optionalDocument = knowledgeDocumentRepository.findById(documentId);
            
            if (optionalDocument.isEmpty()) {
                throw new RuntimeException("Document not found: " + documentId);
            }
            
            KnowledgeDocument document = optionalDocument.get();
            document.setTitle(title);
            document.setContent(content);
            document.setTags(tags);
            document.setLastUpdated(LocalDateTime.now());
            document.setStatus(KnowledgeDocument.DocumentStatus.PENDING);
            
            document = knowledgeDocumentRepository.save(document);
            
            // 임베딩 재생성
            generateEmbedding(document);
            
            logger.info("Document updated: {}", documentId);
            return document;
        });
    }
    
    /**
     * 문서를 삭제합니다.
     */
    public void deleteDocument(String documentId) {
        if (!knowledgeDocumentRepository.existsById(documentId)) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        
        knowledgeDocumentRepository.deleteById(documentId);
        logger.info("Document deleted: {}", documentId);
    }
    
    /**
     * 모든 문서의 인덱스를 재구축합니다.
     */
    public CompletableFuture<Void> rebuildIndex() {
        return CompletableFuture.runAsync(() -> {
            List<KnowledgeDocument> allDocuments = knowledgeDocumentRepository.findAll();
            
            for (KnowledgeDocument document : allDocuments) {
                generateEmbedding(document).join();
            }
            
            logger.info("Knowledge base index rebuilt for {} documents", allDocuments.size());
        });
    }
    
    /**
     * 지식 베이스 통계를 반환합니다.
     */
    public KnowledgeBaseStats getStats() {
        long totalDocuments = knowledgeDocumentRepository.count();
        long indexedDocuments = knowledgeDocumentRepository.findIndexedDocuments().size();
        long pendingDocuments = knowledgeDocumentRepository.findByStatus(KnowledgeDocument.DocumentStatus.PENDING).size();
        long failedDocuments = knowledgeDocumentRepository.findByStatus(KnowledgeDocument.DocumentStatus.FAILED).size();
        
        return new KnowledgeBaseStats(totalDocuments, indexedDocuments, pendingDocuments, failedDocuments);
    }
    
    public static class KnowledgeBaseStats {
        private final long totalDocuments;
        private final long indexedDocuments;
        private final long pendingDocuments;
        private final long failedDocuments;
        
        public KnowledgeBaseStats(long totalDocuments, long indexedDocuments, 
                                long pendingDocuments, long failedDocuments) {
            this.totalDocuments = totalDocuments;
            this.indexedDocuments = indexedDocuments;
            this.pendingDocuments = pendingDocuments;
            this.failedDocuments = failedDocuments;
        }
        
        // Getters
        public long getTotalDocuments() { return totalDocuments; }
        public long getIndexedDocuments() { return indexedDocuments; }
        public long getPendingDocuments() { return pendingDocuments; }
        public long getFailedDocuments() { return failedDocuments; }
    }
}