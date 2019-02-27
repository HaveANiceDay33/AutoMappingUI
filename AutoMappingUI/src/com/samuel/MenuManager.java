package com.samuel;

import static com.osreboot.ridhvl.painter.painter2d.HvlPainter2D.hvlDrawQuad;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.newdawn.slick.Color;

import com.osreboot.ridhvl.action.HvlAction1;
import com.osreboot.ridhvl.menu.HvlComponentDefault;
import com.osreboot.ridhvl.menu.HvlMenu;
import com.osreboot.ridhvl.menu.component.HvlArrangerBox;
import com.osreboot.ridhvl.menu.component.HvlButton;
import com.osreboot.ridhvl.menu.component.HvlComponentDrawable;
import com.osreboot.ridhvl.menu.component.HvlSpacer;
import com.osreboot.ridhvl.menu.component.HvlTextBox;
import com.osreboot.ridhvl.menu.component.HvlArrangerBox.ArrangementStyle;
import com.osreboot.ridhvl.menu.component.collection.HvlLabeledButton;

public class MenuManager {
	
	static HvlMenu inst, ui, rbg;
	static float robotW, robotL;
	
	static String instructions = "Scroll : Zoom in/out\nRight Click : Drag Map\nESC : exit\nLeft Click : Forward drive\nArrow Keys: Adjust LAST point placed"
			+ "\nL : Adjust Robot Width and Length (Clears all points)\nA : Scroll up, Z : Scroll Down";
	
	
	public static void reset() {
		UI.tempWaypoints.clear();
		UI.segments.clear();
		rbg.getChildOfType(HvlArrangerBox.class,0).getChildOfType(HvlTextBox.class,0).setText("");
		rbg.getChildOfType(HvlArrangerBox.class,0).getChildOfType(HvlTextBox.class,1).setText("");
		HvlMenu.setCurrent(MenuManager.rbg);
	}
	
	public static void init() {
		inst = new HvlMenu();
		ui = new HvlMenu();
		rbg = new HvlMenu();
		
		UI.initialize();
		
		HvlComponentDefault.setDefault(HvlLabeledButton.class, new HvlLabeledButton.Builder().setWidth(100).setHeight(50).setFont(Main.gameFont).setTextColor(Color.cyan).setTextScale(0.25f).setOnDrawable(new HvlComponentDrawable() {
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height,Color.lightGray);	
			}
		}).setOffDrawable(new HvlComponentDrawable() {
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height,Color.darkGray);
			}
		}).setHoverDrawable(new HvlComponentDrawable() {
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height,Color.gray);
			}
		}).build());
		
		inst.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.HORIZONTAL).setWidth(270).setHeight(100).setX(Display.getWidth() - 350).setY(Display.getHeight()-180).build());
		inst.getFirstArrangerBox().add(new HvlSpacer(0, 500));
		inst.getFirstArrangerBox().add(new HvlLabeledButton.Builder().setText("Back").setClickedCommand(new HvlAction1<HvlButton>(){
			@Override
			public void run(HvlButton aArg) {
				HvlMenu.setCurrent(ui);
			}
		}).build());
		
		ui.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.HORIZONTAL).setWidth(270).setHeight(100).setX(Display.getWidth() - 350).setY(Display.getHeight()-180).build());
		ui.getFirstArrangerBox().add(new HvlLabeledButton.Builder().setText("Finish\nSegment").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				if(UI.tempWaypoints.size() > 0) {
					Segment newSegment;
					double vel;
					double acc;
					double angVel;
					double angAcc;
					try {
						vel = Double.parseDouble(ui.getChildOfType(HvlArrangerBox.class, 2).getFirstOfType(HvlTextBox.class).getText());
						acc = Double.parseDouble(ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 1).getText());
						angVel = Double.parseDouble(ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 2).getText());
						angAcc = Double.parseDouble(ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 3).getText());
					} catch (NumberFormatException e) {
						vel = 0;
						acc = 0;
						angVel = 0;
						angAcc = 0;
					}
					if(UI.tempWaypoints.get(0).x < UI.tempWaypoints.get(UI.tempWaypoints.size()-1).x) {
						newSegment = new Segment(UI.tempWaypoints, true, vel, acc, angVel, angAcc);
						UI.segments.add(newSegment);
					} else if (UI.tempWaypoints.get(0).x > UI.tempWaypoints.get(UI.tempWaypoints.size()-1).x) {
						newSegment = new Segment(UI.tempWaypoints, false, vel, acc, angVel, angAcc);
						UI.segments.add(newSegment);
					}
					
					UI.tempWaypoints.clear();
				}
			}
		}).build());
		ui.getFirstArrangerBox().add(new HvlLabeledButton.Builder().setText("Delete\nPoint").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				if(UI.tempWaypoints.size() > 0) {
					UI.tempWaypoints.remove(UI.tempWaypoints.size()-1);
				}
			}
		}).build());
		ui.getFirstArrangerBox().add(new HvlLabeledButton.Builder().setText("Delete\nSegment").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				if(UI.segments.size() > 0) {
					UI.segments.remove(UI.segments.size()-1);
				}
			}
		}).build());
		ui.getFirstArrangerBox().add(new HvlLabeledButton.Builder().setText("CLEAR\nALL").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				UI.segments.clear();
				UI.tempWaypoints.clear();
				VirtualPathGenerator.xPos = 0;
				VirtualPathGenerator.currentPosOnArc = 0;
				ui.getChildOfType(HvlArrangerBox.class,1).getChildOfType(HvlTextBox.class,0).setText("");
			}
		}).build());
		ui.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.HORIZONTAL).setWidth(250).setHeight(100).setX(Display.getWidth() - 350).setY(Display.getHeight()-100).build());
		ui.getChildOfType(HvlArrangerBox.class, 1).add(new HvlLabeledButton.Builder().setText("RESET").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				VirtualPathGenerator.pos = 0;
				VirtualPathGenerator.currentPosOnArc = 0;
				VirtualPathGenerator.xPos = 0;
				reset();
			}	
		}).build());
		ui.getChildOfType(HvlArrangerBox.class, 1).add(new HvlLabeledButton.Builder().setText("Save").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				VirtualPathGenerator.fileName = MenuManager.ui.getChildOfType(HvlArrangerBox.class, 1).getFirstOfType(HvlTextBox.class).getText();
				File outputFile = new File(UI.userHomeFolder, VirtualPathGenerator.fileName + ".BOND");
				VirtualPathGenerator.pos = 0;
				VirtualPathGenerator.currentPosOnArc = 0;
				VirtualPathGenerator.xPos = 0;
				try {
					VirtualPathGenerator.fileWriter = new BufferedWriter(new FileWriter(outputFile));
				} catch (IOException e) {
					System.out.println("Could not write to output file");
				}
				
				int segNum = 1;
				for(Segment segment : UI.segments) {
					System.out.print("Segment " + segNum + ": \n");
					VirtualPathGenerator.runVirtualPath(UI.generateData(segment), segment.getArcLengthMeters(), segment);
					System.out.println("");
					segNum++;
				}
				try {
					System.out.println("Complete!");
					System.out.println("Profile generated with name: " + VirtualPathGenerator.fileName + ".BOND");
					VirtualPathGenerator.fileWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("----------------------------------------------------------------------------------------------");
			}	
		}).build());
		ui.getChildOfType(HvlArrangerBox.class, 1).add(new HvlSpacer(30, 30));
		ui.getChildOfType(HvlArrangerBox.class, 1).add(new HvlTextBox.Builder().setWidth(200).setHeight(50).setFont(Main.gameFont).setTextColor(Color.darkGray).setTextScale(0.25f).setOffsetY(20).setOffsetX(20).setText("").setFocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.lightGray);	
			}
		}).setUnfocusedDrawable(new HvlComponentDrawable() {
			
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.white);	
			}
		}).build());
		
		//vel and acc changes
		ui.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.VERTICAL).setWidth(300).setHeight(500).setX(Display.getWidth() - 350).setY(140).build());
		//vel
		ui.getChildOfType(HvlArrangerBox.class, 2).add(new HvlTextBox.Builder().setWidth(200).setHeight(50).setNumbersOnly(true).setFont(Main.gameFont).setTextColor(Color.darkGray).setTextScale(0.25f).setOffsetY(20).setOffsetX(20).setText("").setFocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.lightGray);	
			}
		}).setUnfocusedDrawable(new HvlComponentDrawable() {
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.white);	
			}
		}).build());
		ui.getChildOfType(HvlArrangerBox.class, 2).add(new HvlSpacer(10, 10));
		//acc
		ui.getChildOfType(HvlArrangerBox.class, 2).add(new HvlTextBox.Builder().setWidth(200).setHeight(50).setNumbersOnly(true).setFont(Main.gameFont).setTextColor(Color.darkGray).setTextScale(0.25f).setOffsetY(20).setOffsetX(20).setText("").setFocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.lightGray);	
			}
		}).setUnfocusedDrawable(new HvlComponentDrawable() {
			
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.white);	
			}
		}).build());
		ui.getChildOfType(HvlArrangerBox.class, 2).add(new HvlSpacer(10, 10));
		//ang vel
		ui.getChildOfType(HvlArrangerBox.class, 2).add(new HvlTextBox.Builder().setWidth(200).setHeight(50).setNumbersOnly(true).setFont(Main.gameFont).setTextColor(Color.darkGray).setTextScale(0.25f).setOffsetY(20).setOffsetX(20).setText("").setFocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.lightGray);	
			}
		}).setUnfocusedDrawable(new HvlComponentDrawable() {
			
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.white);	
			}
		}).build());
		ui.getChildOfType(HvlArrangerBox.class, 2).add(new HvlSpacer(10, 10));
		//ang acc
		ui.getChildOfType(HvlArrangerBox.class, 2).add(new HvlTextBox.Builder().setWidth(200).setHeight(50).setNumbersOnly(true).setFont(Main.gameFont).setTextColor(Color.darkGray).setTextScale(0.25f).setOffsetY(20).setOffsetX(20).setText("").setFocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.lightGray);	
			}
		}).setUnfocusedDrawable(new HvlComponentDrawable() {
			
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.white);	
			}
		}).build());
		
		
		rbg.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.VERTICAL).setWidth(250).setHeight(400).setX((Display.getWidth()/2)-125).setY((Display.getHeight()/2)-200).build());
		rbg.getFirstArrangerBox().add(new HvlSpacer(30, 30));
		rbg.getFirstArrangerBox().add(new HvlTextBox.Builder().setWidth(200).setHeight(50).setFont(Main.gameFont).setTextColor(Color.darkGray).setTextScale(0.25f).setOffsetY(20).setOffsetX(20).setText("").setNumbersOnly(true).setFocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.lightGray);	
			}
		}).setUnfocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.white);	
			}
		}).build());
		rbg.getFirstArrangerBox().add(new HvlSpacer(30, 30));
		rbg.getFirstArrangerBox().add(new HvlTextBox.Builder().setWidth(200).setHeight(50).setFont(Main.gameFont).setTextColor(Color.darkGray).setTextScale(0.25f).setOffsetY(20).setOffsetX(20).setText("").setNumbersOnly(true).setFocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.lightGray);	
			}
		}).setUnfocusedDrawable(new HvlComponentDrawable() {
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.white);	
			}
		}).build());
		rbg.getChildOfType(HvlArrangerBox.class, 0).add(new HvlSpacer(30, 30));
		rbg.getFirstArrangerBox().add(new HvlLabeledButton.Builder().setText("Set W/H").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				//fileName = UI.getFirstArrangerBox().getFirstOfType(HvlTextBox.class).getText();
				if(!rbg.getFirstArrangerBox().getFirstOfType(HvlTextBox.class).getText().equals("")){
					robotW = Float.parseFloat(rbg.getFirstArrangerBox().getFirstOfType(HvlTextBox.class).getText()) *  (float) (2.54 * UI.PIXELS_TO_CM);
					robotL = Float.parseFloat(rbg.getFirstArrangerBox().getChildOfType(HvlTextBox.class, 1).getText()) *  (float) (2.54 * UI.PIXELS_TO_CM);
					ui.getChildOfType(HvlArrangerBox.class,1).getChildOfType(HvlTextBox.class,0).setText("");
					UI.background = Main.FIELD_INDEX;
					HvlMenu.setCurrent(ui);
				}
			}
		}).build());
		rbg.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.HORIZONTAL).setWidth(250).setHeight(400).setX((Display.getWidth()/2)-125).setY((Display.getHeight()/2)+00).build());
		rbg.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.VERTICAL).setWidth(250).setHeight(400).setX((Display.getWidth()/2)+400).setY((Display.getHeight()/2)-185).build());
		
		rbg.getChildOfType(HvlArrangerBox.class, 2).add(new HvlSpacer(70, 30));
		rbg.getChildOfType(HvlArrangerBox.class,2).add(new HvlLabeledButton.Builder().setText("Deep Space").setWidth(200).setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				UI.tempWaypoints.clear();
				robotW = (float) (34 * 2.54 * UI.PIXELS_TO_CM);
				robotL = (float) (39 * 2.54 * UI.PIXELS_TO_CM); 
				UI.background = Main.FIELD_INDEX;
				HvlMenu.setCurrent(ui);
			}
		}).build());
	
		System.out.println("");
		ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 0).setText("2");
		ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 1).setText("2");
		ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 2).setText("2");
		ui.getChildOfType(HvlArrangerBox.class, 2).getChildOfType(HvlTextBox.class, 3).setText("2");
		HvlMenu.setCurrent(rbg);
	}
	
	public static void update(float delta) {
		if(HvlMenu.getCurrent() == inst) {
			Main.textOutline(instructions, Color.cyan, Color.darkGray, 20, 20, 0.35f);
			if(Keyboard.isKeyDown(Keyboard.KEY_E)) {
				HvlMenu.setCurrent(ui);
			}
		} else if(HvlMenu.getCurrent() == rbg) {
			Main.textOutline("              Welcome to Sam Munro's Auto Mapping Client Application! \n\n2019 FIELD IS THE ONLY ONE THAT IS ACCURATELY MEASURED AND SCALED!!!    : )" ,Color.cyan, Color.darkGray,60,30, 0.4f);
			Main.textOutline("Set robot width and length :",Color.cyan, Color.darkGray,550,220, 0.3f);
			Main.textOutline("Width : ",Color.cyan, Color.darkGray,470,280, 0.4f);
			Main.textOutline("Length : ",Color.cyan, Color.darkGray,470,360, 0.4f);
		} else if(HvlMenu.getCurrent() == ui) {
			UI.update(delta);
		}
		
		HvlMenu.updateMenus(delta);
	}
}
