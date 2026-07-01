package com.staffengagement.task.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.NullHandling;
import org.springframework.data.domain.Sort.Order;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskSortBuilderTest {

    @Test
    void buildSort_defaultsToCreatedDateDesc() {
        Sort sort = TaskSortBuilder.buildSort(null, null);
        List<Order> orders = sort.toList();

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo("createdAt");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(orders.get(1).getProperty()).isEqualTo("id");
        assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void buildSort_createdDateAsc_hasSecondaryIdSort() {
        Sort sort = TaskSortBuilder.buildSort("createdDate", "asc");
        List<Order> orders = sort.toList();

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo("createdAt");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orders.get(1).getProperty()).isEqualTo("id");
        assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void buildSort_dueDateDesc_nativeNullHandling() {
        Sort sort = TaskSortBuilder.buildSort("dueDate", "desc");
        List<Order> orders = sort.toList();

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo("dueDate");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(orders.get(0).getNullHandling()).isEqualTo(NullHandling.NATIVE);
        assertThat(orders.get(1).getProperty()).isEqualTo("id");
    }

    @Test
    void buildSort_dueDateAsc_nativeNullHandling() {
        Sort sort = TaskSortBuilder.buildSort("dueDate", "asc");
        List<Order> orders = sort.toList();

        assertThat(orders.get(0).getProperty()).isEqualTo("dueDate");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orders.get(0).getNullHandling()).isEqualTo(NullHandling.NATIVE);
    }

    @Test
    void buildSort_createdDateSort_noNullHandling() {
        Sort sort = TaskSortBuilder.buildSort("createdDate", "desc");
        List<Order> orders = sort.toList();

        assertThat(orders.get(0).getProperty()).isEqualTo("createdAt");
        assertThat(orders.get(0).getNullHandling()).isEqualTo(NullHandling.NATIVE);
    }

    @Test
    void buildSort_unknownSortBy_defaultsToCreatedAt() {
        Sort sort = TaskSortBuilder.buildSort("unknown", "asc");
        List<Order> orders = sort.toList();

        assertThat(orders.get(0).getProperty()).isEqualTo("createdAt");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void buildSort_invalidSortOrder_defaultsToDesc() {
        Sort sort = TaskSortBuilder.buildSort("createdDate", "invalid");
        List<Order> orders = sort.toList();

        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void buildSort_secondarySortAlwaysById() {
        Sort sortAsc = TaskSortBuilder.buildSort("dueDate", "asc");
        Sort sortDesc = TaskSortBuilder.buildSort("dueDate", "desc");

        assertThat(sortAsc.toList().get(1).getProperty()).isEqualTo("id");
        assertThat(sortDesc.toList().get(1).getProperty()).isEqualTo("id");
    }
}
