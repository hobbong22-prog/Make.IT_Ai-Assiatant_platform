package com.Human.Ai.D.makit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.Human.Ai.D.makit.domain.KnowledgeDocument;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeRetriever {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeRetriever.class);
    private static final double SIMILARITY_THRESHOLD = 0.7;
    
    @Autowired
    private KnowledgeBaseManager knowledgeBaseManager;
    
    @Autowired
    private BedrockService bedrockService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 쿼리와 관련된 문서들을 검색합니다.
     */
    public List<RelevantDocument> retrieveRelevantDocuments(String query, int maxResults) {
        try {
            // 쿼리의 임베딩 생성
            String queryEmbedding = bedrockService.generateEmbedding(query);
            
            // 모든 인덱싱된 문서 가져오기
            List<KnowledgeDocument> indexedDocuments = knowledgeBaseManager.getIndexedDocuments();
            
            // 유사도 계산 및 정렬
            List<RelevantDocument> relevantDocuments = new ArrayList<>();
            
            for (KnowledgeDocument document : indexedDocuments) {
                if (document.getEmbeddingVector() != null) {
                    double similarity = calculateCosineSimilarity(queryEmbedding, document.getEmbeddingVector());
                    
                    if (similarity >= SIMILARITY_THRESHOLD) {
                        relevantDocuments.add(new RelevantDocument(document, similarity));
                    }
                }
            }
            
            // 유사도 순으로 정렬하고 상위 결과만 반환
            return relevantDocuments.stream()
                    .sorted(Comparator.comparingDouble(RelevantDocument::getSimilarity).reversed())
                    .limit(maxResults)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error retrieving relevant documents for query: {}", query, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 키워드 기반 검색을 수행합니다.
     */
    public List<RelevantDocument> keywordSearch(String query, int maxResults) {
        try {
            List<KnowledgeDocument> documents = knowledgeBaseManager.searchDocuments(query);
            
            return documents.stream()
                    .limit(maxResults)
                    .map(doc -> new RelevantDocument(doc, 1.0)) // 키워드 매치는 높은 점수
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error performing keyword search for query: {}", query, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 하이브리드 검색 (의미 검색 + 키워드 검색)을 수행합니다.
     */
    public List<RelevantDocument> hybridSearch(String query, int maxResults) {
        List<RelevantDocument> semanticResults = retrieveRelevantDocuments(query, maxResults);
        List<RelevantDocument> keywordResults = keywordSearch(query, maxResults);
        
        // 결과 병합 및 중복 제거
        List<RelevantDocument> combinedResults = new ArrayList<>(semanticResults);
        
        for (RelevantDocument keywordResult : keywordResults) {
            boolean exists = combinedResults.stream()
                    .anyMatch(doc -> doc.getDocument().getDocumentId()
                            .equals(keywordResult.getDocument().getDocumentId()));
            
            if (!exists) {
                combinedResults.add(keywordResult);
            }
        }
        
        // 유사도 순으로 정렬하고 제한
        return combinedResults.stream()
                .sorted(Comparator.comparingDouble(RelevantDocument::getSimilarity).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 태그로 문서를 검색합니다.
     */
    public List<RelevantDocument> searchByTag(String tag, int maxResults) {
        try {
            List<KnowledgeDocument> documents = knowledgeBaseManager.getDocumentsByTag(tag);
            
            return documents.stream()
                    .limit(maxResults)
                    .map(doc -> new RelevantDocument(doc, 0.9)) // 태그 매치는 높은 점수
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error searching documents by tag: {}", tag, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 문서 유형으로 검색합니다.
     */
    public List<RelevantDocument> searchByType(String documentType, int maxResults) {
        try {
            List<KnowledgeDocument> documents = knowledgeBaseManager.getDocumentsByType(documentType);
            
            return documents.stream()
                    .limit(maxResults)
                    .map(doc -> new RelevantDocument(doc, 0.8)) // 타입 매치는 중간 점수
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error searching documents by type: {}", documentType, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 코사인 유사도를 계산합니다.
     */
    private double calculateCosineSimilarity(String embedding1, String embedding2) {
        try {
            JsonNode vector1 = objectMapper.readTree(embedding1);
            JsonNode vector2 = objectMapper.readTree(embedding2);
            
            if (!vector1.isArray() || !vector2.isArray() || 
                vector1.size() != vector2.size()) {
                return 0.0;
            }
            
            double dotProduct = 0.0;
            double norm1 = 0.0;
            double norm2 = 0.0;
            
            for (int i = 0; i < vector1.size(); i++) {
                double val1 = vector1.get(i).asDouble();
                double val2 = vector2.get(i).asDouble();
                
                dotProduct += val1 * val2;
                norm1 += val1 * val1;
                norm2 += val2 * val2;
            }
            
            if (norm1 == 0.0 || norm2 == 0.0) {
                return 0.0;
            }
            
            return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
            
        } catch (Exception e) {
            logger.error("Error calculating cosine similarity", e);
            return 0.0;
        }
    }
    
    /**
     * 검색 결과를 나타내는 클래스
     */
    public static class RelevantDocument {
        private final KnowledgeDocument document;
        private final double similarity;
        
        public RelevantDocument(KnowledgeDocument document, double similarity) {
            this.document = document;
            this.similarity = similarity;
        }
        
        public KnowledgeDocument getDocument() {
            return document;
        }
        
        public double getSimilarity() {
            return similarity;
        }
        
        public String getSnippet(int maxLength) {
            String content = document.getContent();
            if (content.length() <= maxLength) {
                return content;
            }
            return content.substring(0, maxLength) + "...";
        }
    }
}