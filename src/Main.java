public class Main {
    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("ERROR: Unable to run a Order Scheduler!");
            System.out.println("Please pass two arguments:");
            System.out.println("1) path to the JSON file with store configuration");
            System.out.println("2) path to the JSON file with list of orders");
        }
        else{
            OrderScheduler orderScheduler = new OrderScheduler(args[0], args[1]);
            orderScheduler.scheduleOrders();
            orderScheduler.printOrders();
        }
    }
}