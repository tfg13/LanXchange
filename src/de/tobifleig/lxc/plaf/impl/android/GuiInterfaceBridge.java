package de.tobifleig.lxc.plaf.impl.android;

/**
 * Required to forward some events from LXCService (that implements GuiInterface)
 * to the real Gui (that LXC cannot handle because it can be recreated etc)
 * @author tfg
 *
 */
public interface GuiInterfaceBridge {
	
	/**
	 * Forwards some update()-calls to the gui.
	 */
	public void update();

}
