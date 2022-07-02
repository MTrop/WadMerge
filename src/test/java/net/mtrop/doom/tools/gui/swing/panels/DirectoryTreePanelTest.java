package net.mtrop.doom.tools.gui.swing.panels;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JFrame;

import net.mtrop.doom.tools.gui.DoomToolsGUIMain;
import net.mtrop.doom.tools.struct.util.ObjectUtils;

import static javax.swing.BorderFactory.*;
import static net.mtrop.doom.tools.struct.swing.ContainerFactory.*;
import static net.mtrop.doom.tools.struct.swing.ComponentFactory.*;


public final class DirectoryTreePanelTest 
{
	public static void main(String[] args) 
	{
		DoomToolsGUIMain.setLAF();
		final DirectoryTreePanel panel = new DirectoryTreePanel(new File("."), new DirectoryTreePanel.DirectoryTreeListener() 
		{
			@Override
			public void onFileConfirmed(File confirmedFile) 
			{
				System.out.println("CONFIRM: " + confirmedFile);
			}

			@Override
			public void onFilesCopied(File[] copiedFiles)
			{
			}

			@Override
			public void onFilesDeleted(File[] deletedFiles) 
			{
			}

			@Override
			public boolean onFilesDropped(File parentFile, File[] droppedSourceFiles) 
			{
				return false;
			}
			
			@Override
			public boolean onFileInsert(String fileName, File parentFile, boolean directory) 
			{
				return false;
			}

			@Override
			public boolean onFileRename(File changedFile, String newName) 
			{
				System.out.println("RENAME: " + changedFile + " -> " + newName);
				return false;
			}
		});
		
		panel.setBorder(createEmptyBorder(8,8,8,8));

		ObjectUtils.apply(frame("Test",
			containerOf(
				node(BorderLayout.NORTH, button("Refresh", (c,e) -> panel.setSelectedFile(new File("src/test/java/net")))),
				node(BorderLayout.CENTER, panel)
			)
		), 
		(frame) -> {
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
		});
	}
}