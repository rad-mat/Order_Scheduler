import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

public class OrderScheduler {

    private List<String> pickers;
    private LocalTime pickingStartTime;
    private LocalTime pickingEndTime;
    private List<Order> orders;
    private Map<String, List<Order>> schedule;

    public List<String> getPickers() {
        return pickers;
    }

    public LocalTime getPickingStartTime() {
        return pickingStartTime;
    }

    public LocalTime getPickingEndTime() {
        return pickingEndTime;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public Map<String, List<Order>> getSchedule() {
        return schedule;
    }

    public OrderScheduler(String storeFilePath, String ordersFilePath) {
        try {
            JSONParser parser = new JSONParser();

            // load store configuration
            JSONObject storeConfig = (JSONObject) parser.parse(new FileReader(storeFilePath));
            this.pickers = (List<String>) storeConfig.get("pickers");
            this.pickingStartTime = LocalTime.parse((String) storeConfig.get("pickingStartTime"));
            this.pickingEndTime = LocalTime.parse((String) storeConfig.get("pickingEndTime"));

            // load orders for the day
            JSONArray ordersArray = (JSONArray) parser.parse(new FileReader(ordersFilePath));
            this.orders = new ArrayList<>();
            for (Object orderObj : ordersArray) {
                JSONObject orderJson = (JSONObject) orderObj;
                String orderId = (String) orderJson.get("orderId");
                BigDecimal orderValue = new BigDecimal(orderJson.get("orderValue").toString());
                Duration pickingTime = Duration.parse((String) orderJson.get("pickingTime"));
                LocalTime completeBy = LocalTime.parse((String) orderJson.get("completeBy"));
                Order order = new Order(orderId, orderValue, pickingTime, completeBy);
                this.orders.add(order);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void scheduleOrders() {
        List<Order> sortedOrders = this.orders.stream()
                .sorted(Comparator.comparing(Order::getCompleteBy))
                .sorted(Comparator.comparing(Order::getLatestStartTime))
                .toList();

        this.schedule = new HashMap<>();
        for (Order order : sortedOrders) {
            String picker = findAvailablePicker(order);
            if (picker != null) {
                this.schedule.computeIfAbsent(picker, k -> new ArrayList<>()).add(order);
            }
        }
    }

    public String findAvailablePicker(Order order) {
        // find picker who can pick order
        for (String picker : this.pickers) {
            if (isPickerAvailable(picker, order)) {
                return picker;
            }
        }
        // find picker who can replace order
        return findDeputyPicker(order);
    }

    private LocalTime getStartWorkTime(List<Order> pickerSchedule, Order order){
        LocalTime startTime;
        if(order.getCompleteBy().isAfter(this.pickingEndTime)){
            startTime = this.pickingEndTime.minus(order.getPickingTime());
        }
        else{
            startTime = order.getLatestStartTime();
        }

        for (Order scheduledOrder : pickerSchedule) {
            startTime = startTime.minus(scheduledOrder.getPickingTime());
        }
        return startTime;
    }

    private boolean isPickerAvailable(String picker, Order order) {
        List<Order> pickerSchedule = this.schedule.get(picker);
        if (pickerSchedule == null) {
            return true;
        }

        LocalTime startTimeIfTakesOrder = getStartWorkTime(pickerSchedule, order);
        LocalTime latestEndTime = pickerSchedule.get(pickerSchedule.size()-1).getCompleteBy();

        return !latestEndTime.isAfter(pickingEndTime) && !startTimeIfTakesOrder.isBefore(pickingStartTime);
    }

    private String findDeputyPicker(Order order){
        // prefers to drop orders that needs to be completed earlier
        LocalTime earliestEndTime = this.pickingEndTime;
        String candidatePicker = null;
        Order orderToRemove = null;

        for (String picker : this.pickers) {
            List<Order> pickerSchedule = this.schedule.get(picker);
            if (pickerSchedule == null) {
                return picker;
            }

            for (Order scheduledOrder : pickerSchedule) {
                if(scheduledOrder.getPickingTime().compareTo(order.getPickingTime()) > 0
                        && !scheduledOrder.getCompleteBy().isAfter(earliestEndTime)){
                    earliestEndTime = scheduledOrder.getCompleteBy();
                    candidatePicker = picker;
                    orderToRemove = scheduledOrder;
                }
            }
        }

        if(candidatePicker != null){
            this.schedule.get(candidatePicker).remove(orderToRemove);
        }

        return candidatePicker;
    }

    public void printOrders() {
        for (String picker : this.pickers) {
            LocalTime start = pickingStartTime;
            for (Order order : this.schedule.get(picker)){
                System.out.println(picker + " " + order.getOrderId() + " " + start);
                start = start.plus(order.getPickingTime());
            }
        }
    }
}