import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderSchedulerTest {

    private OrderScheduler orderScheduler;

    @BeforeEach
    public void setUp() {
        // set up the OrderScheduler object for each test case
        String storeFilePath = "store_test.json";
        String ordersFilePath = "orders_test.json";
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
        orderScheduler = new OrderScheduler(storeFilePath, ordersFilePath);
        orderScheduler.scheduleOrders();
    }

    @Test
    public void testReadingJsonFile() {
        // test that constructor reads properly JSON file
        Assertions.assertEquals(2, orderScheduler.getPickers().size());
        Assertions.assertEquals("09:00", orderScheduler.getPickingStartTime().toString());
        Assertions.assertEquals("11:00", orderScheduler.getPickingEndTime().toString());
        Assertions.assertEquals(7, orderScheduler.getOrders().size());
        Assertions.assertEquals("order-1", orderScheduler.getOrders().get(0).getOrderId());
        Assertions.assertEquals("PT15M", orderScheduler.getOrders().get(0).getPickingTime().toString());
        Assertions.assertEquals("09:15", orderScheduler.getOrders().get(0).getCompleteBy().toString());
    }

    @Test
    public void testScheduleOrders() {
        // test that the scheduleOrders method generates a valid schedule
        Map<String, List<Order>> schedule = orderScheduler.getSchedule();
        Assertions.assertFalse(schedule.isEmpty());

        List<Order> ordersP1 = schedule.get("P1");
        Assertions.assertEquals(3, ordersP1.size());
        Assertions.assertEquals("order-1", ordersP1.get(0).getOrderId());
        Assertions.assertEquals("order-5", ordersP1.get(1).getOrderId());
        Assertions.assertEquals("order-6", ordersP1.get(2).getOrderId());

        List<Order> ordersP2 = schedule.get("P2");
        Assertions.assertEquals(4, ordersP2.size());
        Assertions.assertEquals("order-2", ordersP2.get(0).getOrderId());
        Assertions.assertEquals("order-3", ordersP2.get(1).getOrderId());
        Assertions.assertEquals("order-4", ordersP2.get(2).getOrderId());
        Assertions.assertEquals("order-7", ordersP2.get(3).getOrderId());
    }

    @Test
    public void testFindAvailablePicker() {
        // test that the findAvailablePicker method returns a valid picker
        Order order;
        String picker;

        // if order is appropriate
        order = new Order("test", new BigDecimal("10.00"), Duration.ofMinutes(15), LocalTime.parse("11:00"));
        picker = orderScheduler.findAvailablePicker(order);
        Assertions.assertNotNull(picker);

        // if order is inappropriate
        order = new Order("test", new BigDecimal("10.00"), Duration.ofMinutes(60), LocalTime.parse("12:00"));
        picker = orderScheduler.findAvailablePicker(order);
        Assertions.assertNull(picker);
    }

    @Test
    public void testFindDeputyPicker() {
        // test that the orders are swapped properly
        Order order = new Order("test", new BigDecimal("10.00"), Duration.ofMinutes(50), LocalTime.parse("12:00"));

        orderScheduler.getOrders().add(order);
        orderScheduler.scheduleOrders();

        List<String> ordersID = new ArrayList<>();

        for (Order scheduledOrder : orderScheduler.getSchedule().get("P1")){
            ordersID.add(scheduledOrder.getOrderId());
        }

        Assertions.assertTrue(ordersID.contains("test"));
        Assertions.assertFalse(ordersID.contains("order-5"));
    }
}
