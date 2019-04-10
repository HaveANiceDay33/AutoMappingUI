package com.samuel;

import static com.osreboot.ridhvl.painter.painter2d.HvlPainter2D.hvlDrawQuad;
import static com.osreboot.ridhvl.painter.painter2d.HvlPainter2D.hvlDrawQuadc;
import static com.osreboot.ridhvl.painter.painter2d.HvlPainter2D.hvlResetRotation;
import static com.osreboot.ridhvl.painter.painter2d.HvlPainter2D.hvlRotate;

import java.util.ArrayList;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.newdawn.slick.Color;

import com.osreboot.ridhvl.action.HvlAction0;
import com.osreboot.ridhvl.menu.HvlMenu;
import com.osreboot.ridhvl.menu.component.HvlArrangerBox;
import com.osreboot.ridhvl.menu.component.HvlTextBox;
import com.osreboot.ridhvl.painter.HvlCursor;
import com.osreboot.ridhvl.painter.painter2d.HvlPainter2D;

/**
 * <h1>This class handles the drawing and function calculations required to generate a path.</h1>
 * @author Samuel Munro
 * @author Peter Salisbury
 *
 */
public class UI {
	
	public final static float WALL_OFFSET = 92;
	public static final double PIXELS_TO_CM = 0.56;
	public static final double TOP_OFFSET = 135;
	public final static String userHomeFolder = System.getProperty("user.home")+"/Documents/";
	
	static double arcLength = 0;
	
	private static float picSizeX = 928;
	private static float picSizeY = 464;
	private static float mouseX;
	private static float mouseY;
	private static float fineSpeed;
	private static float mouseX1;
	private static float mouseY1;
	private static float textY;
	
	private static int mouseDerivative;
	public static int background;
	private static boolean clicked;
	
	static ArrayList<Waypoint> tempWaypoints;
	static ArrayList<Segment> segments;
	
	/**
	 * Writes a function in a readable manner based on coefficients in array form
	 * @param equalTo
	 * @param coeffArray
	 * @return
	 */
	public static String functionGenerator(String equalTo, double[] coeffArray) {
		StringBuilder s = new StringBuilder();
		s.append(equalTo + " = ");
		for(int i = coeffArray.length-1; i >= 0; i--) {
			if(coeffArray[i] != 0) {
				s.append(String.format("%.16f %s^%d " + (i==0 ? "" : "+") + " ", coeffArray[i], "x", i));
			}
		}
		return s.toString().replace("+ -", "- ");
	}
	
	/**
	 * Generates all of the path, path radius, and robot frame graphics for the UI
	 * @param waypoints
	 * @param lineColor
	 */
	public static void generateGraphics(ArrayList<Waypoint> waypoints, Color lineColor) {
		ArrayList<Double> xVals = new ArrayList();
		ArrayList<Double> yVals = new ArrayList();
		if(waypoints.size() > 0) {
			for(int i = 0; i < waypoints.size(); i++) {
				xVals.add((double)waypoints.get(i).x);
				yVals.add((double)waypoints.get(i).y);
			}
			double[] xArray = new double[xVals.size()];
			double[] yArray = new double[yVals.size()];
			for(int i = 0; i < waypoints.size(); i++) {
				xArray[i] = xVals.get(i);
				yArray[i] = yVals.get(i);
			}
			
			float mouseX = HvlCursor.getCursorX() - (2*WALL_OFFSET);
			
			PolynomialRegression functionGen = new PolynomialRegression(xArray, yArray, 5, "x");
			double [] deriCoeff = new double[5];
			double [] coeffs = functionGen.coefficients();
			for(int i = 0; i < 5; i++) {
				deriCoeff[i] = coeffs[i+1] * (i+1); 								  
			}
			
			for(Waypoint w : waypoints) {
				double deriAtxPos = (deriCoeff[4]*Math.pow(w.x, 4)) + (deriCoeff[3]*Math.pow(w.x, 3))+
						(deriCoeff[2]*Math.pow(w.x, 2))+(deriCoeff[1]*Math.pow(w.x, 1))+(deriCoeff[0]*Math.pow(w.x, 0));
				
				double ang = Math.atan(deriAtxPos);
				if(deriCoeff[0] != 0) {
					w.setAngle(Math.toDegrees(ang));
				}
				
			}
			
			if(xVals.get(xVals.size()-1) < xVals.get(0)) {
				for(double i = xVals.get(0); i > xVals.get(xVals.size()-1); i--) {
					double x = i;
					double y = functionGen.predict(x);
					hvlDrawQuad((float)x, (float)y, 2, 2, lineColor); 
				}
			}else {
				for(double i = xVals.get(0); i < xVals.get(xVals.size()-1); i++) {
					double x = i;
					double y = functionGen.predict(x);
					hvlDrawQuad((float)x, (float)y, 2, 2, lineColor); 
				}
			}
			float size = -100*(float)generateRadiusAtAPoint(functionGen.coefficients(), functionGen.degree(), mouseX);
			
			
			if(mouseX > xVals.get(0) && mouseX < xVals.get(xVals.size()-1) || mouseX < xVals.get(0) && mouseX > xVals.get(xVals.size()-1)) {
				if(Math.abs(size) < 300) {
					hvlDrawQuadc(mouseX, ((float)functionGen.predict(mouseX))-(size), size*2, size*2, Main.getTexture(Main.CIRCLE_INDEX));
				}
				double deriAtxPos = (deriCoeff[4]*Math.pow(mouseX, 4)) + (deriCoeff[3]*Math.pow(mouseX, 3))+
						(deriCoeff[2]*Math.pow(mouseX, 2))+(deriCoeff[1]*Math.pow(mouseX, 1))+(deriCoeff[0]*Math.pow(mouseX, 0));
				
				double ang = Math.atan(deriAtxPos);
				hvlRotate(mouseX, (float)functionGen.predict(mouseX), (float)Math.toDegrees(ang));
				hvlDrawQuadc(mouseX, (float)functionGen.predict(mouseX), MenuManager.robotL, MenuManager.robotW, Main.getTexture(Main.FRAME_INDEX));
				hvlResetRotation();
			}
		}
	}
	
	/**
	 * <p>Returns the radius of a circle tangent to the curve at any given point.
	 * Necessary for voltage calculations</p>
	 * @param coeffs
	 * @param degree
	 * @param x
	 * @return
	 */
	public static double generateRadiusAtAPoint(double [] coeffs, int degree, float x) {
		double TwoDer; 
		double OneDer;
		double[] coefficients = coeffs;
		//calculating derivative coefficients
		double[] deriCoeff = new double[5];
		for(int i = 0; i < degree; i++) {
			deriCoeff[i] = coefficients[i+1] * (i+1); 								  
		}
		double[] secDeriCoeff = new double[4];
		for(int i = 0; i < degree-1; i++) {
			secDeriCoeff[i] = deriCoeff[i+1] * (i+1); 							 
		}
		//calculates first and second derivatives at given point.
		OneDer = (deriCoeff[4]*Math.pow(x, 4))+(deriCoeff[3]*Math.pow(x, 3))+
				(deriCoeff[2]*Math.pow(x, 2))+(deriCoeff[1]*Math.pow(x, 1))+(deriCoeff[0]*Math.pow(x, 0));
		TwoDer = (secDeriCoeff[3]*Math.pow(x, 3))+(secDeriCoeff[2]*Math.pow(x, 2))+
				(secDeriCoeff[1]*Math.pow(x, 1))+(secDeriCoeff[0]*Math.pow(x, 0));
		
		//returns radius with gross formula. 
		double radCm = 1/((TwoDer)/(Math.pow(1+(OneDer*OneDer), 1.5)));
		
		if(radCm > 1000000) {
			return 1000000;
		}else if(radCm < -1000000){
			return -1000000;
		}else {
			return radCm/100;
		}
	}
	
	/**
	 * Generates a function in terms of cm for the virtual path generator class to use for calculations.
	 * @param seg
	 * @return
	 */
	public static double[] generateData(Segment seg) {
		ArrayList<Waypoint> waypoints = seg.segPoints;
		ArrayList<Double> xVals = new ArrayList();
		ArrayList<Double> yVals = new ArrayList();
		arcLength = 0;
		if(waypoints.size() > 0) {
			for(int i = 0; i < waypoints.size(); i++) {
				xVals.add((double) (Math.round((((waypoints.get(i).x)+WALL_OFFSET)/PIXELS_TO_CM)) - Math.round((((waypoints.get(0).x)+WALL_OFFSET)/PIXELS_TO_CM)))); //returns cm
				yVals.add(-((double) (Math.round((((waypoints.get(i).y)-TOP_OFFSET)/PIXELS_TO_CM)) - Math.round((((waypoints.get(0).y)-TOP_OFFSET)/PIXELS_TO_CM)))));
			}
			double[] xArray = new double[xVals.size()];
			double[] yArray = new double[yVals.size()];
			for(int i = 0; i < waypoints.size(); i++) {
				xArray[i] = xVals.get(i);
				yArray[i] = yVals.get(i);
			}
			PolynomialRegression functionGen = new PolynomialRegression(xArray, yArray, 5, "x");
			
			double[] coefficients = functionGen.coefficients();
			double[] deriCoeff = new double[5];
			double[] secDeriCoeff = new double[4];
			
			for(int i = 0; i < functionGen.degree()+1; i++) {
				double term = coefficients[i];
			}
			for(int i = 0; i < 5; i++) {
				deriCoeff[i] = coefficients[i+1] * (i+1); 								  
			}
			for(int i = 0; i < functionGen.degree()-1; i++) { 
				secDeriCoeff[i] = deriCoeff[i+1] * (i+1); 							 
			}
			
			double finalX = xVals.get(xVals.size()-1);
			double step = 0.001; //in CM, lower to get more precise
			
			//Riemann sum!
			if(waypoints.get(0).x < waypoints.get(waypoints.size()-1).x) {
				for(double i = step; i < finalX; i+=step) {
					
					double a = Math.sqrt(1 + Math.pow((deriCoeff[4]*Math.pow(i, 4)) + (deriCoeff[3]*Math.pow(i, 3))+
							(deriCoeff[2]*Math.pow(i, 2))+(deriCoeff[1]*Math.pow(i, 1))+(deriCoeff[0]*Math.pow(i, 0)), 2));
					
					double b = Math.sqrt(1 + Math.pow((deriCoeff[4]*Math.pow(i-step, 4)) + (deriCoeff[3]*Math.pow(i-step, 3))+
							(deriCoeff[2]*Math.pow(i-step, 2))+(deriCoeff[1]*Math.pow(i-step, 1))+(deriCoeff[0]*Math.pow(i-step, 0)), 2));
					
				    double stepVal = step * ((a+b)/2); //complies with trapezoidal Riemann sum h(f(a) + f(b))/2
					
					arcLength += stepVal;
				}
			} else {
				for(double i = finalX; i < 0; i+=step) {
					
					double a = Math.sqrt(1 + Math.pow((deriCoeff[4]*Math.pow(i, 4)) + (deriCoeff[3]*Math.pow(i, 3))+
							(deriCoeff[2]*Math.pow(i, 2))+(deriCoeff[1]*Math.pow(i, 1))+(deriCoeff[0]*Math.pow(i, 0)), 2));
					
					double b = Math.sqrt(1 + Math.pow((deriCoeff[4]*Math.pow(i-step, 4)) + (deriCoeff[3]*Math.pow(i-step, 3))+
							(deriCoeff[2]*Math.pow(i-step, 2))+(deriCoeff[1]*Math.pow(i-step, 1))+(deriCoeff[0]*Math.pow(i-step, 0)), 2));
					
				    double stepVal = step * ((a+b)/2); //complies with trapezoidal Riemann sum h(f(a) + f(b))/2
					
					arcLength += stepVal;
				}
			}
			
			seg.setArcLengthCM(arcLength);
			
			System.out.println("y(x) = " + functionGen.toString());
			System.out.println(functionGenerator("v(x)", deriCoeff));
			System.out.println(functionGenerator("a(x)", secDeriCoeff));
			System.out.println("");
			
			return coefficients;
		}
		return null;
	}
	
	/**
	 * Initializes UI
	 */
	public static void initialize() {
		picSizeX = 928;
		picSizeY = 464;
		fineSpeed = (float) 0.125;
		clicked = false;
		segments = new ArrayList<Segment>();
		tempWaypoints = new ArrayList<Waypoint>();
		textY = 40;
	}
	
	/**
	 * Handles point drawing, scrolling, and other controls
	 * @param delta
	 */
	public static void update(float delta) {
		HvlPainter2D.hvlDrawQuadc(Display.getWidth()/2, Display.getHeight()/2, 800, 800, Main.getTexture(Main.LOGO_INDEX));
		if(mouseX < 900 || mouseX > 1435 && mouseY > 75 || mouseY < 25) {
			
			if(Mouse.isButtonDown(0) && clicked == false) {
				if(tempWaypoints.size() > 0 || segments.size() == 0) {
					Waypoint point = new Waypoint((mouseX / Main.zoomer.getZoom() + (Main.zoomer.getX() - 720)/Main.zoomer.getZoom()), (mouseY / Main.zoomer.getZoom())+(Main.zoomer.getY() - 360)/Main.zoomer.getZoom(),
						10, Color.yellow, MenuManager.robotW, MenuManager.robotL);
					tempWaypoints.add(point);
					clicked = true;
				}
				if(tempWaypoints.size() == 0 && segments.size() != 0) {	
					Waypoint point = new Waypoint(segments.get(segments.size()-1).segPoints.get(segments.get(segments.size()-1).segPoints.size()-1).x, 
							segments.get(segments.size()-1).segPoints.get(segments.get(segments.size()-1).segPoints.size()-1).y,
							20, Color.blue,MenuManager.robotW, MenuManager.robotL);
					
					tempWaypoints.add(point);
					point.setAngle(segments.get(segments.size()-1).segPoints.get(segments.get(segments.size()-1).segPoints.size()-1).angleOffset);
					clicked = true;
				}
			}
			if(!Mouse.isButtonDown(0)) {
				clicked = false;
			}
		}
		mouseDerivative = Mouse.getDWheel();
		mouseX = HvlCursor.getCursorX();
		mouseY = HvlCursor.getCursorY();
		//Main.zooming AND MOVING CODE
		if(mouseDerivative > 0) {Main.zoom+=0.05;}
		else if(mouseDerivative < 0) {Main.zoom -= 0.05;}
		if(Main.zoom <= 1) {Main.zoom = 1;}
		
		if(Mouse.isButtonDown(1)) {
			if(mouseX1 < mouseX){Main.zoomer.setX(Main.zoomer.getX() - (mouseX-mouseX1));}
			if(mouseX1 > mouseX){Main.zoomer.setX(Main.zoomer.getX() +(mouseX1-mouseX));}
			if(mouseY1 < mouseY){Main.zoomer.setY(Main.zoomer.getY() - (mouseY-mouseY1));}
			if(mouseY1 > mouseY){Main.zoomer.setY(Main.zoomer.getY() +(mouseY1-mouseY));}				
		}
		
		if(Main.zoomer.getX() >= 1080) {Main.zoomer.setX(1080);}
		if(Main.zoomer.getX()<= 0) {Main.zoomer.setX(0);}
		if(Main.zoomer.getY() >= 720) {Main.zoomer.setY(720);}
		if(Main.zoomer.getY() <= 0) {Main.zoomer.setY(0);}
	
		for(int i = 0; i < tempWaypoints.size(); i++) {
			Main.textOutline((i+1)+". X: "+(Math.round((((tempWaypoints.get(i).x)+WALL_OFFSET)/PIXELS_TO_CM) - Math.round(((tempWaypoints.get(0).x)+WALL_OFFSET)/PIXELS_TO_CM)))+
					" cm. Y: "+(Math.round((((tempWaypoints.get(i).y)-TOP_OFFSET)/PIXELS_TO_CM) - Math.round(((tempWaypoints.get(0).y)-TOP_OFFSET)/PIXELS_TO_CM)))+" cm.",Color.white, Color.darkGray, 1030, (25*(i-1))+textY, 0.25f);
		}
		
		if((Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_LEFT) ||  
				Keyboard.isKeyDown(Keyboard.KEY_DOWN) || Keyboard.isKeyDown(Keyboard.KEY_UP)) && tempWaypoints.size() > 0) {
			if((segments.size() > 0 && tempWaypoints.size() > 1) || segments.size() == 0) {
			//FINE ADJUSTMENT FOR tempWaypoints
				if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {tempWaypoints.get(tempWaypoints.size()-1).x += fineSpeed;}
				if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {tempWaypoints.get(tempWaypoints.size()-1).x += -fineSpeed;}
				if(Keyboard.isKeyDown(Keyboard.KEY_UP)) {tempWaypoints.get(tempWaypoints.size()-1).y += -fineSpeed;}
				if(Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {tempWaypoints.get(tempWaypoints.size()-1).y += fineSpeed;}
			}
		}else {
			if(tempWaypoints.size()>0) {
				tempWaypoints.get(tempWaypoints.size()-1).x += 0;
				tempWaypoints.get(tempWaypoints.size()-1).y += 0;
			}
		}
		
		if(Keyboard.isKeyDown(Keyboard.KEY_A)) {    //SCROLLING
			textY -= 1;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_Z)) {
			textY+=1;
		}
		
		mouseX1 = mouseX;
		mouseY1 = mouseY;

		Main.zoomer.setZoom(Main.zoom);
		Main.zoomer.doTransform(new HvlAction0() { //THIS THING ALLOWS THE Main.zoom TO WORK
			@Override
			public void run() {
				hvlDrawQuadc(300, 360, picSizeX, picSizeY, Main.getTexture(background));
				for(Waypoint allPoints : tempWaypoints) {   //DISPLAYING ALL tempWaypoints
						allPoints.display();		
				}
				for(Segment allSegments : segments) {
					allSegments.draw();
				}
				generateGraphics(tempWaypoints, Color.red);
			}
		});
		Main.textOutline("Press Q to see controls", Color.white, Color.darkGray, 50, 50, 0.4f);
		Main.textOutline("v", Color.white, Color.darkGray, Display.getWidth() - 340, 260, 0.25f);
		Main.textOutline("a", Color.white, Color.darkGray, Display.getWidth() - 340, 320, 0.25f);
		Main.textOutline("av", Color.white, Color.darkGray, Display.getWidth() - 340, 380, 0.25f);
		Main.textOutline("aa", Color.white, Color.darkGray, Display.getWidth() - 340, 440, 0.25f);
		Main.textOutline("ai", Color.white, Color.darkGray, Display.getWidth() - 340, 500, 0.25f);
		if(tempWaypoints.size() > 0 && segments.size() == 0) {
			if(!MenuManager.ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 4).getText().equals("") && 
					!MenuManager.ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 4).getText().equals("-") && 
					!MenuManager.ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 4).getText().equals(".") &&
					!MenuManager.ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 4).getText().equals(".-")) {
				tempWaypoints.get(0).setAngle(Math.toDegrees(Double.parseDouble(MenuManager.ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 4).getText())));
			}
		}
		
		if(mouseX < 1095 || mouseX > 1435 && mouseY > 75 || mouseY < 25) {
			if(Keyboard.isKeyDown(Keyboard.KEY_Q)) {
				HvlMenu.setCurrent(MenuManager.inst);
			}
		}
	}
}
