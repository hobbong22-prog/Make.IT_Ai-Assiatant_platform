package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
    
    List<KnowledgeDocument> findByStatus(KnowledgeDocument.DocumentStatus status);
    
    List<KnowledgeDocument> findByDocumentType(String documentType);
    
    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.title LIKE %:keyword% OR kd.content LIKE %:keyword%")
    List<KnowledgeDocument> findByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT kd FROM KnowledgeDocument kd JOIN kd.tags t WHERE t = :tag")
    List<KnowledgeDocument> findByTag(@Param("tag") String tag);
    
    List<KnowledgeDocument> findBySource(String source);
    
    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.embeddingVector IS NOT NULL")
    List<KnowledgeDocument> findIndexedDocuments();
}