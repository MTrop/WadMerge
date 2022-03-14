package net.mtrop.doom.tools.gui;

import java.awt.Image;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.mtrop.doom.tools.struct.SingletonProvider;
import net.mtrop.doom.tools.struct.swing.ComponentFactory.ComponentActionHandler;
import net.mtrop.doom.tools.struct.swing.ComponentFactory.MenuNode;
import net.mtrop.doom.tools.struct.swing.ContainerFactory.ModalChoice;

import static net.mtrop.doom.tools.struct.swing.ComponentFactory.*;
import static net.mtrop.doom.tools.struct.swing.ContainerFactory.*;

/**
 * DoomTools GUI Utilities and component building.
 * This relies on singletons for info.
 * @author Matthew Tropiano
 */
public final class DoomToolsGUIUtils 
{
    /** The instance encapsulator. */
    private static final SingletonProvider<DoomToolsGUIUtils> INSTANCE = new SingletonProvider<>(() -> new DoomToolsGUIUtils());
	
	/**
	 * @return the singleton instance of this object.
	 */
	public static DoomToolsGUIUtils get()
	{
		return INSTANCE.get();
	}

	/* ==================================================================== */
	
	private DoomToolsImageManager images;
	private DoomToolsLanguageManager language;
	private List<Image> windowIcons;
	private Icon windowIcon;
	
	private DoomToolsGUIUtils()
	{
		this.images = DoomToolsImageManager.get();
		this.language = DoomToolsLanguageManager.get();
		
		final Image icon16  = images.getImage("doomtools-logo-16.png"); 
		final Image icon32  = images.getImage("doomtools-logo-32.png"); 
		final Image icon48  = images.getImage("doomtools-logo-48.png"); 
		final Image icon64  = images.getImage("doomtools-logo-64.png"); 
		final Image icon96  = images.getImage("doomtools-logo-96.png"); 
		final Image icon128 = images.getImage("doomtools-logo-128.png"); 

		this.windowIcons = Arrays.asList(icon128, icon96, icon64, icon48, icon32, icon16);
		this.windowIcon = new ImageIcon(icon16);
	}
	
	/**
	 * Creates a menu from a language key, getting the necessary pieces to assemble it.
	 * @param keyPrefix the key prefix.
	 * @param nodes the additional component nodes.
	 * @return the new menu.
	 */
	public JMenu createMenuFromLanguageKey(String keyPrefix, MenuNode... nodes)
	{
		return menu(
			language.getText(keyPrefix),
			language.getMnemonicValue(keyPrefix + ".mnemonic"),
			nodes
		);
	}

	/**
	 * Creates a menu item from a language key, getting the necessary pieces to assemble it.
	 * @param keyPrefix the key prefix.
	 * @param nodes the additional component nodes.
	 * @return the new menu item node.
	 */
	public MenuNode createItemFromLanguageKey(String keyPrefix, MenuNode... nodes)
	{
		return menuItem(
			language.getText(keyPrefix),
			language.getMnemonicValue(keyPrefix + ".mnemonic"),
			nodes
		);
	}
	
	/**
	 * Creates a menu item from a language key, getting the necessary pieces to assemble it.
	 * @param keyPrefix the key prefix.
	 * @param handler the action to take on selection.
	 * @return the new menu item node.
	 */
	public MenuNode createItemFromLanguageKey(String keyPrefix, ComponentActionHandler<JMenuItem> handler)
	{
		return menuItem(
			language.getText(keyPrefix),
			language.getMnemonicValue(keyPrefix + ".mnemonic"),
			language.getKeyStroke(keyPrefix + ".keystroke"),
			handler
		);
	}
	
	/**
	 * Creates a menu item from a language key, getting the necessary pieces to assemble it.
	 * Name is taken form the action.
	 * @param keyPrefix the key prefix.
	 * @param action the attached action.
	 * @return the new menu item node.
	 */
	public MenuNode createItemFromLanguageKey(String keyPrefix, Action action)
	{
		return menuItem(
			language.getMnemonicValue(keyPrefix + ".mnemonic"),
			language.getKeyStroke(keyPrefix + ".keystroke"),
			action
		);
	}

	/**
	 * Creates a menu checkbox item from a language key, getting the necessary pieces to assemble it.
	 * @param keyPrefix the key prefix.
	 * @param selected the initial selected state.
	 * @param handler the action to take on selection.
	 * @return the new menu item node.
	 */
	public MenuNode createCheckItemFromLanguageKey(String keyPrefix, boolean selected, ComponentActionHandler<JCheckBoxMenuItem> handler)
	{
		return checkBoxItem(
			language.getText(keyPrefix),
			selected,
			language.getMnemonicValue(keyPrefix + ".mnemonic"),
			language.getKeyStroke(keyPrefix + ".keystroke"),
			handler
		);
	}

	/**
	 * Creates a button from a language key, getting the necessary pieces to assemble it.
	 * @param icon the button icon.
	 * @param keyPrefix the key prefix.
	 * @param handler the action to take on selection.
	 * @return the new menu item node.
	 */
	public JButton createButtonFromLanguageKey(Icon icon, String keyPrefix, ComponentActionHandler<JButton> handler)
	{
		return button(
			icon,
			language.getText(keyPrefix),
			language.getMnemonicValue(keyPrefix + ".mnemonic"),
			handler
		);
	}

	/**
	 * Creates a button from a language key, getting the necessary pieces to assemble it.
	 * @param keyPrefix the key prefix.
	 * @param handler the action to take on selection.
	 * @return the new menu item node.
	 */
	public JButton createButtonFromLanguageKey(String keyPrefix, ComponentActionHandler<JButton> handler)
	{
		return createButtonFromLanguageKey(null, keyPrefix, handler);
	}

	/**
	 * Creates a modal choice from a language key, getting the necessary pieces to assemble it.
	 * @param keyPrefix the key prefix.
	 * @param result the choice result.
	 * @return the new menu item node.
	 */
	public <T> ModalChoice<T> createChoiceFromLanguageKey(String keyPrefix, T result)
	{
		return choice(
			language.getText(keyPrefix),
			language.getMnemonicValue(keyPrefix + ".mnemonic"),
			result
		);
	}

	/**
	 * Creates a modal choice from a language key, getting the necessary pieces to assemble it.
	 * @param keyPrefix the key prefix.
	 * @return the new menu item node.
	 */
	public <T> ModalChoice<T> createChoiceFromLanguageKey(String keyPrefix)
	{
		return createChoiceFromLanguageKey(keyPrefix, null);
	}

	/**
	 * @return the common window icons to use.
	 */
	public List<Image> getWindowIcons() 
	{
		return windowIcons;
	}
	
	/**
	 * @return the single window icon to use.
	 */
	public Icon getWindowIcon() 
	{
		return windowIcon;
	}
	
}
