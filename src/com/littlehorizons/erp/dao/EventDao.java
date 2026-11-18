package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.report.EventSummary;

import java.util.List;
import java.util.Optional;

/** CRUD for school events (eventos). */
public interface EventDao extends CrudDao<SchoolEvent> {

    Optional<SchoolEvent> findById(int id);

    List<SchoolEvent> findAll();

    int insert(SchoolEvent e);

    boolean update(SchoolEvent e);

    boolean delete(int id);

    /** Events with number of participants and the podium (1st, 2nd, 3rd) as text. */
    List<EventSummary> summaries(int year);
}
