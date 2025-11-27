package com.nexus.backend.repository;

import com.nexus.backend.entity.Expression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Expression Repository
 *
 * @author NEXUS Team
 * @since 2025-01-21
 */
@Repository
public interface ExpressionRepository extends JpaRepository<Expression, UUID> {

    /**
     * Unit별 표현 목록 조회
     */
    List<Expression> findByUnit(String unit);

    /**
     * Unit별 표현 목록 조회 (순서대로, 개수 제한)
     */
    @Query(value = "SELECT * FROM expressions WHERE unit = :unit ORDER BY expression LIMIT :limit", nativeQuery = true)
    List<Expression> findByUnitOrderByExpression(@Param("unit") String unit, @Param("limit") int limit);

    /**
     * 모든 Unit 목록 조회
     */
    @Query("SELECT DISTINCT e.unit FROM Expression e ORDER BY e.unit")
    List<String> findAllUnits();

    /**
     * Unit별 표현 개수 조회
     */
    @Query("SELECT COUNT(e) FROM Expression e WHERE e.unit = :unit")
    Long countByUnit(@Param("unit") String unit);

    /**
     * Unit 내 Chapter 목록 조회
     */
    @Query("SELECT DISTINCT e.chapter FROM Expression e WHERE e.unit = :unit ORDER BY e.chapter")
    List<String> findChaptersByUnit(@Param("unit") String unit);

    /**
     * Unit + Chapter별 표현 개수 조회
     */
    @Query("SELECT COUNT(e) FROM Expression e WHERE e.unit = :unit AND e.chapter = :chapter")
    Long countByUnitAndChapter(@Param("unit") String unit, @Param("chapter") String chapter);

    /**
     * Unit + Chapter별 표현 목록 조회 (순서대로, 개수 제한)
     */
    @Query(value = "SELECT * FROM expressions WHERE unit = :unit AND chapter = :chapter ORDER BY expression LIMIT :limit", nativeQuery = true)
    List<Expression> findByUnitAndChapterOrderByExpression(@Param("unit") String unit, @Param("chapter") String chapter, @Param("limit") int limit);
}
