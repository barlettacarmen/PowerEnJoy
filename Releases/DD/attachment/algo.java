//Methods belonging to the User Manager Class
public class User{
	public Reservation lastReservation;
	Ride lastRide;
	private boolean blocked;

public void performRide(){
	this.lastRide=new Ride(lastReservation);
	this.lastReservation.getCar().unlock();
	this.lastRide.doRide(); 
}
}
//Methods belonging to Reservation Manager Class
public class ReservationMgr(){
//This set of Reservations is used to keep track of the not cancelled Reservations,
// of the Reservation with a not yet expired timer and  of Reservations which ended Ride
// has a pending payment
	public static ArrayList<Reservation> acriveReservations= new ArrayList<Reservation>();
	
	
	public void makeReservation(Position p,User user){
	boolean valid;
//Control if there is yet one reservation for the user or if there is a pending payment
// for his last Reservation and so the user is blocked 
//(In both cases he can't make a new Reservation)
	 if(userHasReservationaActive(user)){
	 		if(user.getBlocked())
	 			Gui.sendMessageOfPendingPayment(user);
			else 
				Gui.sendMessageOfExistingReservation(user);
			valid=true;
	}else 
			valid=false;
			
	while(!valid){
		ArrayList cars= CarMgr.findReserveableCars(p);
		if(!cars.isEmpty()){
			Car car=user.chooseCar(cars);
			car.setAccessibilityToReserved();
			user.lastReservation= new Reservation(user,car);
			addReservation(user.lastReservation);
			valid=true;}
		else 
			Gui.sendErrorMessage(user);
			}
}
public static void cancelReservation(User user){
	if(user.lastReservaion.timer.isAlive()){
		user.lastReservation.setCancelled(true);
		removeReservation(user.lastReservation);
		user.lastReservaion.timer.interrupt();}	
}
	
	public static void addReservation(Reservation r){
		acriveReservations.add(r);
	}
	public static void removeReservation(Reservation r){
		acriveReservation.remove(r);
	}
	public static boolean userHasReservationActive(User user){
		for(Reservation r: activeReservations)
			if(r.getUser().equals(user))
				return true;
		return false;
			
	}
	
	
}
//Methods belonging to Reservation  Class
public class Reservation{
	private User user;
	private Car car;
	private boolean cancelled=false;
	public Thread timer= new Thread();
	private int penalty;
	
public Reservation(User u, Car c){
	this.user=u;
	this.car=c;
	reservationTimer();
}

//If User cancels Reservation (cancelReservation(User user)), 
//the timer thread is interrupted and if at line 95 is executed
//If car's engine is turned on, the timer thread is interrupted and if at line 98 
//is executed
//If none of the two previous events occur, the timer expires, 
//the user receives a penalty and if he pays it,the Reservation is removed 
//form the set of activeReservations
public void connectionTimer(){
	try{
		timer.sleep(3600000);
	}catch(InterruptedException e){ 
		if(cancelled){
			car.setAccessibilityToAvailable();
			return;}
		if(car.getState().getEngine().equals("on"))
			return;
	} 	penalty=1;
		car.setAccessibilityToAvailable();
		user.sendRequestOfPayment(penalty);
		if(Payment.getPayment(user))
			ReservationMgr.removeReservation(this);
		else user.setBlocked(true); //Pending payment 		
}
}
//Methods belonging to Ride Manager Class
public class Ride{
	Reservation reservation;
	State carState;
	Car car;
	float price;
	int passengers;
	boolean dicountPassengers;
	boolean discountBattery;
	boolean discountPluggedIn;
	boolean penalty;
	boolean paid;
	public Ride(Reservation res){
		this.reservation=res;
		this.car=reservation.getCar();
		this.carState=reservation.getCar().getState();
		this.price=0;
		this.passengers=0;
		this.discountPassengers=false;
		this.discountBattery=false;
		this.discountPluggedIn=false;
		this.penalty=false;
		this.paid=false;
	}
	public void doRide(){
	startRide();
	endRide();
	}

	public void startRide(){
		while(reservation.timer.isAlive()&& carState.getEngine().equals("off")){
				try{
					Thread.sleep(200);}
				catch(InterruptedException e){
					Thread.currentThread().interrupt();
				}
			}
		if(carState.getEngine().equals("on")){
			reservation.timer.interrupt();
			carState.setDoor("locked");
			this.passengers=carState().getPassengers();
			manageRide();}
	}
	
	public void manageRide(){
		while(carState.getEngine().equals("on")){
			  car.getGPS();
			  Gui.showNearSafeAreas(reservation.getUser());
			  this.price=calculateCharge();
			  Gui.showCharge(reservation.getUser(),this.price);
				if(car.getBatteryLevel()<=10)
					Gui.sendLowBatteryMessage(reservation.getUser())
			  	if(car.getPassengers()>=3)
			  		discountPassengers=true;
			  }
	}
//After the User exits the car and there are no more passengers, 
//we give him 5 min of time to put car in charging.
	public void endRide(){
		while(car.getPassengers()>0){
				this.price=calculateCharge();
				Gui.showCharge(reservation.getUser(),this.price);}
		if(car.inSafeArea(car.getGPS())
			carState.setDoor("locked");
		else if(car.inSpecialSafeArea(car.getGPS())){
					try{
					Thread.sleep(300000); //5 min timer
					}catch(InterruptedException e){
					Thread.currentThread().interrupt();
					if(carState.getAccessibility().equals("Charging")){
						discountPluggedIn=true;
						carState.setDoor("locked");}
				}
			}
		if((car.getBatteryLevel<=20&&!carState.getAccessibility().equals("Charging"))||
			 LocationMgr.farFromSpecialSafeArea(car.getGPS())){
				penalty=true;
				carState.setDoor("locked");
				car.setAccessibilityToOutOfService();
				Operator.sendRequestOfMaintenance()}
		if(car.getBatteryLevel()>=50){
			discountBattery=true;
			if(!carState.getAccessibility().equals("Charging")) //if car hasn't been put
				car.setAccessibilityToAvailable();}				// in charging 
				
		
		this.price=calculateChargeWithDiscountAndPenalty();
		Payment.sendRequestOfPayment(reservation.getUser());
		if(Payment.getPayment(reservation.getUser())){
			paid=true;
			ReservationMgr.removeReservation(reservation);} 
		else reservation.getUser().setBlocked(true); //Pending payment 
}
	

}
//Method belonging to Car Manager
public class CarMgr{
	public static ArrayList<Car> availableCars=new ArrayList<Car>();
	public static ArrayList<Car> chargingCars=new ArrayList<Car>();
	public static ArrayList<Car> reservedCars=new ArrayList<Car>();
	public static ArrayList<Car> outOfServiceCars=new ArrayList<Car>();
	
	public static ArrayList findReservableCars(Position p){
			ArrayList<Car> reserveableCar=new ArrayList<Car>();
			for(Car c: availableCars)
				if(LocationMgr.near(p,c))
					reserveableCar.add(c);
			for(Car c: chargingCars)
				if(LocationMgr.near(p,c))
					reserveableCar.add(c);	
			return 	reserveableCar;	
	}
//All setters belonging to Car class which modify the stateAccessibility 
//(like car.setAccessibilityToAvailable())
// of a Car will switch CarAccessibility and also push/pop the Car 
// in/from the corresponding CarMgr set.
	
	
}

