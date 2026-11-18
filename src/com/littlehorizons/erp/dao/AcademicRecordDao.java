package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.report.CourseAverage;
import com.littlehorizons.erp.model.report.StudentRanking;

import java.util.List;
import java.util.Optional;

/** Grades (registros_academicos) and SQL aggregations for averages. */
public interface AcademicRecordDao {

    Optional<AcademicRecord> findById(int id);

    Optional<AcademicRecord> find(int studentId, int subjectId, int year, AcademicPeriod period);

    int insert(AcademicRecord r);

    boolean update(AcademicRecord r);

    boolean delete(int id);

    /** All grades of a student; {@code year == null} returns the complete history. */
    List<AcademicRecord> findByStudent(int studentId, Integer year);

    /** Course average per subject of a grade (all students), optionally restricted to one period. */
    List<CourseAverage> courseAverages(int gradeId, int year, AcademicPeriod period);

    /** General average per student (optionally by grade and period), best first. */
    List<StudentRanking> ranking(Integer gradeId, int year, AcademicPeriod period);
}
