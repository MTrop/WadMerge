package net.mtrop.doom.tools.gui.managers.parsing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.fife.ui.autocomplete.AbstractCompletion;
import org.fife.ui.autocomplete.CompletionProvider;

import net.mtrop.doom.tools.wadmerge.WadMergeCommand;

/**
 * WadMerge Completion Provider.
 * @author Matthew Tropiano
 */
public class WadMergeCompletionProvider extends CommonCompletionProvider
{
	public WadMergeCompletionProvider()
	{
		super();
		for (WadMergeCommand command : WadMergeCommand.values())
			addCompletion(new CommandCompletion(this, command));
	}
	
	/**
	 * Special completion for WadMerge-based stuff.
	 */
	public class CommandCompletion extends AbstractCompletion
	{
		private final String name;
		private final String usage; 
		private final String summaryText;
		
		protected CommandCompletion(CompletionProvider parent, WadMergeCommand command) 
		{
			super(parent);
			this.name = command.name().toLowerCase();
			this.usage = command.usage().toLowerCase();

			final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
			try (PrintStream textOut = new PrintStream(bos, true))
			{
				command.help(textOut);
			}
			this.summaryText = writeHTML((html) -> html.tag("pre", (new String(bos.toByteArray()))));
		}
		
		@Override
		public String getInputText()
		{
			return name;
		}

		@Override
		public String getReplacementText()
		{
			return usage;
		}

		@Override
		public String getSummary()
		{
			return summaryText;
		}

		@Override
		public String toString() 
		{
			return usage;
		}
		
	}

}

