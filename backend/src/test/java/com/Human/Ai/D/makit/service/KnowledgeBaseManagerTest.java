package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.KnowledgeDocument;
import com.Human.Ai.D.makit.repository.KnowledgeDocumentRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseManagerTest {
    
    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    
    @Mock
    private BedrockService bedrockService;
    
    @InjectMocks
    private KnowledgeBaseManager knowledgeBaseManager;
    
    private KnowledgeDocument testDocument;
    
    @BeforeEach
    void setUp() {
        testDocument = new KnowledgeDocument("test-id", "Test Title", "Test Content", "FAQ");
        testDocument.setSource("test-source");
        testDocument.setTags(Arrays.asList("tag1", "tag2"));
    }
    
    @Test
    void testAddDocument() {
        // Given
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenReturn(testDocument);
        when(bedrockService.generateEmbedding(anyString())).thenReturn("[0.1, 0.2, 0.3]");
        
        // When
        CompletableFuture<KnowledgeDocument> result = knowledgeBaseManager.addDocument(
                "Test Title", "Test Content", "FAQ", "test-source", Arrays.asList("tag1", "tag2"));
        
        // Then
        assertNotNull(result);
        KnowledgeDocument document = result.join();
        assertEquals("Test Title", document.getTitle());
        assertEquals("Test Content", document.getContent());
        assertEquals("FAQ", document.getDocumentType());
        
        verify(knowledgeDocumentRepository, times(1)).save(any(KnowledgeDocument.class));
    }
    
    @Test
    void testGetDocument() {
        // Given
        when(knowledgeDocumentRepository.findById("test-id")).thenReturn(Optional.of(testDocument));
        
        // When
        Optional<KnowledgeDocument> result = knowledgeBaseManager.getDocument("test-id");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testDocument, result.get());
        verify(knowledgeDocumentRepository, times(1)).findById("test-id");
    }
    
    @Test
    void testSearchDocuments() {
        // Given
        List<KnowledgeDocument> expectedDocuments = Arrays.asList(testDocument);
        when(knowledgeDocumentRepository.findByKeyword("test")).thenReturn(expectedDocuments);
        
        // When
        List<KnowledgeDocument> result = knowledgeBaseManager.searchDocuments("test");
        
        // Then
        assertEquals(expectedDocuments, result);
        verify(knowledgeDocumentRepository, times(1)).findByKeyword("test");
    }
    
    @Test
    void testGetDocumentsByTag() {
        // Given
        List<KnowledgeDocument> expectedDocuments = Arrays.asList(testDocument);
        when(knowledgeDocumentRepository.findByTag("tag1")).thenReturn(expectedDocuments);
        
        // When
        List<KnowledgeDocument> result = knowledgeBaseManager.getDocumentsByTag("tag1");
        
        // Then
        assertEquals(expectedDocuments, result);
        verify(knowledgeDocumentRepository, times(1)).findByTag("tag1");
    }
    
    @Test
    void testGetDocumentsByType() {
        // Given
        List<KnowledgeDocument> expectedDocuments = Arrays.asList(testDocument);
        when(knowledgeDocumentRepository.findByDocumentType("FAQ")).thenReturn(expectedDocuments);
        
        // When
        List<KnowledgeDocument> result = knowledgeBaseManager.getDocumentsByType("FAQ");
        
        // Then
        assertEquals(expectedDocuments, result);
        verify(knowledgeDocumentRepository, times(1)).findByDocumentType("FAQ");
    }
    
    @Test
    void testUpdateDocument() {
        // Given
        when(knowledgeDocumentRepository.findById("test-id")).thenReturn(Optional.of(testDocument));
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenReturn(testDocument);
        when(bedrockService.generateEmbedding(anyString())).thenReturn("[0.1, 0.2, 0.3]");
        
        // When
        CompletableFuture<KnowledgeDocument> result = knowledgeBaseManager.updateDocument(
                "test-id", "Updated Title", "Updated Content", Arrays.asList("new-tag"));
        
        // Then
        assertNotNull(result);
        KnowledgeDocument document = result.join();
        assertEquals("Updated Title", document.getTitle());
        assertEquals("Updated Content", document.getContent());
        
        verify(knowledgeDocumentRepository, times(1)).findById("test-id");
        verify(knowledgeDocumentRepository, times(1)).save(any(KnowledgeDocument.class));
    }
    
    @Test
    void testDeleteDocument() {
        // Given
        when(knowledgeDocumentRepository.existsById("test-id")).thenReturn(true);
        
        // When
        knowledgeBaseManager.deleteDocument("test-id");
        
        // Then
        verify(knowledgeDocumentRepository, times(1)).existsById("test-id");
        verify(knowledgeDocumentRepository, times(1)).deleteById("test-id");
    }
    
    @Test
    void testDeleteDocumentNotFound() {
        // Given
        when(knowledgeDocumentRepository.existsById("test-id")).thenReturn(false);
        
        // When & Then
        assertThrows(RuntimeException.class, () -> knowledgeBaseManager.deleteDocument("test-id"));
        verify(knowledgeDocumentRepository, times(1)).existsById("test-id");
        verify(knowledgeDocumentRepository, never()).deleteById("test-id");
    }
    
    @Test
    void testGetStats() {
        // Given
        when(knowledgeDocumentRepository.count()).thenReturn(10L);
        when(knowledgeDocumentRepository.findIndexedDocuments()).thenReturn(Arrays.asList(testDocument));
        when(knowledgeDocumentRepository.findByStatus(KnowledgeDocument.DocumentStatus.PENDING))
                .thenReturn(Arrays.asList());
        when(knowledgeDocumentRepository.findByStatus(KnowledgeDocument.DocumentStatus.FAILED))
                .thenReturn(Arrays.asList());
        
        // When
        KnowledgeBaseManager.KnowledgeBaseStats stats = knowledgeBaseManager.getStats();
        
        // Then
        assertEquals(10L, stats.getTotalDocuments());
        assertEquals(1L, stats.getIndexedDocuments());
        assertEquals(0L, stats.getPendingDocuments());
        assertEquals(0L, stats.getFailedDocuments());
    }
    
    @Test
    void testGenerateEmbedding() {
        // Given
        when(bedrockService.generateEmbedding(anyString())).thenReturn("[0.1, 0.2, 0.3]");
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenReturn(testDocument);
        
        // When
        CompletableFuture<Void> result = knowledgeBaseManager.generateEmbedding(testDocument);
        
        // Then
        assertNotNull(result);
        result.join(); // Wait for completion
        
        verify(bedrockService, times(1)).generateEmbedding(anyString());
        verify(knowledgeDocumentRepository, times(1)).save(testDocument);
    }
}