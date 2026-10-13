package com.app.test;

import com.app.models.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Delivery Batching Partitioning Logic Tests")
public class DeliveryBatchingLogicTest {

    private static final int MAX_BATCH_SIZE = 5;

    /**
     * Replicates the batching grouping and chunking algorithm used in DeliveryBatchingService
     */
    private Map<Integer, List<List<Order>>> partitionOrders(List<Order> orders) {
        Map<Integer, List<Order>> byRestaurant = new LinkedHashMap<>();
        for (Order o : orders) {
            List<Order> list = byRestaurant.get(o.getRestaurantId());
            if (list == null) {
                list = new ArrayList<>();
                byRestaurant.put(o.getRestaurantId(), list);
            }
            list.add(o);
        }

        Map<Integer, List<List<Order>>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Order>> entry : byRestaurant.entrySet()) {
            List<Order> restaurantOrders = entry.getValue();
            List<List<Order>> batches = new ArrayList<>();
            for (int i = 0; i < restaurantOrders.size(); i += MAX_BATCH_SIZE) {
                batches.add(restaurantOrders.subList(i, Math.min(i + MAX_BATCH_SIZE, restaurantOrders.size())));
            }
            result.put(entry.getKey(), batches);
        }
        return result;
    }

    @Test
    @DisplayName("Orders from multiple restaurants are segregated and chunked into batches of at most 5")
    void testBatchPartitioning() {
        List<Order> orders = new ArrayList<>();

        // 12 orders for restaurant 1 (should make batches of 5, 5, 2)
        for (int i = 1; i <= 12; i++) {
            Order o = new Order();
            o.setOrderId(i);
            o.setRestaurantId(1);
            orders.add(o);
        }

        // 3 orders for restaurant 2 (should make 1 batch of 3)
        for (int i = 13; i <= 15; i++) {
            Order o = new Order();
            o.setOrderId(i);
            o.setRestaurantId(2);
            orders.add(o);
        }

        Map<Integer, List<List<Order>>> partitioned = partitionOrders(orders);

        assertEquals(2, partitioned.size(), "Should partition across 2 restaurants");

        List<List<Order>> rest1Batches = partitioned.get(1);
        assertEquals(3, rest1Batches.size(), "12 orders should yield 3 batches");
        assertEquals(5, rest1Batches.get(0).size());
        assertEquals(5, rest1Batches.get(1).size());
        assertEquals(2, rest1Batches.get(2).size());

        List<List<Order>> rest2Batches = partitioned.get(2);
        assertEquals(1, rest2Batches.size(), "3 orders should yield 1 batch");
        assertEquals(3, rest2Batches.get(0).size());
    }
}
