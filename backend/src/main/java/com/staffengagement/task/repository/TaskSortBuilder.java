package com.staffengagement.task.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.domain.Sort.NullHandling;

/**
 * Utility for building Sort objects for task queries.
 * <p>
 * Enforces:
 * - Secondary sort by task ID for deterministic ordering
 * - Null due dates sorted LAST regardless of sort direction
 */
public final class TaskSortBuilder {

    public static final String SORT_BY_DUE_DATE = "dueDate";
    public static final String SORT_BY_CREATED_DATE = "createdDate";
    public static final String SORT_ORDER_ASC = "asc";
    public static final String SORT_ORDER_DESC = "desc";

    private static final String ENTITY_FIELD_CREATED_AT = "createdAt";
    private static final String ENTITY_FIELD_DUE_DATE = "dueDate";
    private static final String ENTITY_FIELD_ID = "id";

    private TaskSortBuilder() {
        // utility class
    }

    /**
     * Builds a Sort from sortBy and sortOrder parameters.
     *
     * @param sortBy    the field to sort by ("dueDate" or "createdDate"); defaults to "createdDate"
     * @param sortOrder the direction ("asc" or "desc"); defaults to "desc"
     * @return Sort with primary sort + secondary sort by ID for deterministic ordering
     */
    public static Sort buildSort(String sortBy, String sortOrder) {
        String effectiveSortBy = sortBy != null ? sortBy : SORT_BY_CREATED_DATE;
        String effectiveOrder = sortOrder != null ? sortOrder : SORT_ORDER_DESC;

        String entityField = mapSortByToEntityField(effectiveSortBy);
        Sort.Direction direction = SORT_ORDER_ASC.equalsIgnoreCase(effectiveOrder)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Order primaryOrder;
        if (ENTITY_FIELD_DUE_DATE.equals(entityField)) {
            // Null due dates always sorted LAST regardless of sort direction
            primaryOrder = new Order(direction, entityField, NullHandling.NULLS_LAST);
        } else {
            primaryOrder = new Order(direction, entityField);
        }

        // Secondary sort by ID for deterministic ordering
        Order secondaryOrder = new Order(direction, ENTITY_FIELD_ID);

        return Sort.by(primaryOrder, secondaryOrder);
    }

    private static String mapSortByToEntityField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "duedate" -> ENTITY_FIELD_DUE_DATE;
            case "createddate" -> ENTITY_FIELD_CREATED_AT;
            default -> ENTITY_FIELD_CREATED_AT;
        };
    }
}
