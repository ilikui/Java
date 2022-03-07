package info.yueya.J0201;

public class Motorcycle {
	
	String  make;
	String  color;
	
	boolean engineState;
	
	void startEngine()
	{
		if(true == engineState) {
			
			System.out.println("The engine is already on");
		}else {
			
			engineState = true;
			System.out.println("The engine is now on");
		}
	}
	
	void showAtts() {
		
		System.out.println("This motorcycle is a"+color+" "+make);
		if(true == engineState) {
			
			System.out.println("The engine is already on");
		}else {
			System.out.println("The engine is now off");
		}
	}
	
	public static void main(String[] args) {
		
		Motorcycle motorcycle = new Motorcycle();
		
		motorcycle.color = "red";
		motorcycle.make = "Jack";
		//motorcycle.engineState=true;
		motorcycle.startEngine();
		motorcycle.showAtts();
		
		
	}

}
