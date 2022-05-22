package main;

import java.awt.event.*;
import java.util.*;

import javax.swing.event.MouseInputListener;

import templates.Point;

import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;
import org.lwjgl.util.input.ControllerAdapter;


public class Peripherals implements ComponentListener, KeyListener, MouseInputListener {
	HashMap<Integer, Boolean> keyRegister = new HashMap<Integer, Boolean>();
	HashMap<Integer, Boolean> keyToggleRegister = new HashMap<Integer, Boolean>();
	//List<HashMap<Integer, Boolean>> controllersRegister = new ArrayList<>();
	private Point MOUSE_POS = new Point(0,0);
	String typed_str = "";
	boolean type_enable = false;
	public boolean mouse_state = false;

	ControllerAdapter controllerAdapter;
	
	public Peripherals() {
		try{
		Controllers.create();
		}catch(Exception e){
			System.out.println("No controllers found");
		}
		int count = Controllers.getControllerCount();
		for (int i = 0; i < count; i++) {
			Controller c = Controllers.getController(i);
			System.out.println("Controller(" + i + ")=" + c.getName());
			for (int j = 0; j < c.getButtonCount(); j++) {
				System.out.println("button(" + j + ")=" + c.getButtonName(j));
			}
		}
	}
	
	@Override
	public void componentResized(ComponentEvent e) {
		entry.app.updateRenderResolution();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		
	}

	@Override
	public void componentShown(ComponentEvent e) {
		
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if(type_enable)
		typed_str+=e.getKeyChar();
	}

	public void typingEnable(boolean b){
		type_enable = b;
	}

	public String keysTyped(){
		String str = typed_str;
		typed_str = "";
		return str;
	}
	public boolean keysTypedB(){
		return typed_str.length()>0;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		keyRegister.put(key, true);
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int key = e.getKeyCode();
		keyRegister.put(key, false);
	}
	
	public boolean KeyPressed(int keycode) {
		return keyRegister.getOrDefault(keycode, false);
	}

	public boolean KeyToggled(int keycode){
		boolean b = keyRegister.getOrDefault(keycode, false);
		boolean b_toggle = keyToggleRegister.getOrDefault(keycode, false);
		if(b){
			if(b_toggle){
				keyToggleRegister.put(keycode, false);
				return true;
			}else
			return false;
		}else{
			keyToggleRegister.put(keycode, true);
			return false;
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		MOUSE_POS = new Point(e.getX(),
				e.getY());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		MOUSE_POS = new Point(e.getX(),e.getY());
	}

	public Point mousePos(){
		return new Point(MOUSE_POS.getX(), MOUSE_POS.getY());
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		Point pos = new Point(e.getX(), e.getY());
		entry.app.mouseClick(pos);
		mouse_state = true;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mouse_state = false;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

}
