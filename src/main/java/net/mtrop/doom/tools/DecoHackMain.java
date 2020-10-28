package net.mtrop.doom.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import net.mtrop.doom.tools.common.Common;
import net.mtrop.doom.tools.decohack.DecoHackExporter;
import net.mtrop.doom.tools.decohack.DecoHackParser;
import net.mtrop.doom.tools.decohack.contexts.AbstractPatchContext;
import net.mtrop.doom.tools.decohack.exception.DecoHackParseException;
import net.mtrop.doom.tools.exception.OptionParseException;

/**
 * Main class for DECOHack.
 * Special thanks to Esselfortium and Exl (for WhackEd4)
 * @author Matthew Tropiano
 */
public final class DecoHackMain 
{
	private static final String DOOM_VERSION = Common.getVersionString("doom");
	private static final String VERSION = Common.getVersionString("decohack");
	private static final String SPLASH_VERSION = "DECOHack v" + VERSION + " by Matt Tropiano (using DoomStruct v" + DOOM_VERSION + ")";

	private static final Charset ASCII = Charset.forName("ASCII");
	
	private static final String DEFAULT_OUTFILENAME = "dehacked.lmp";
	
	private static final int ERROR_NONE = 0;
	private static final int ERROR_BAD_OPTIONS = 1;
	private static final int ERROR_MISSING_INPUT = 2;
	private static final int ERROR_MISSING_INPUT_FILE = 3;
	private static final int ERROR_IOERROR = 4;
	private static final int ERROR_SECURITY = 5;
	private static final int ERROR_PARSEERROR = 6;

	private static final String SWITCH_HELP = "--help";
	private static final String SWITCH_HELP2 = "-h";
	private static final String SWITCH_VERSION = "--version";

	private static final String SWITCH_OUTPUT = "--output";
	private static final String SWITCH_OUTPUT2 = "-o";
	private static final String SWITCH_OUTPUTCHARSET = "--output-charset";
	private static final String SWITCH_OUTPUTCHARSET2 = "-oc";

	/**
	 * Program options.
	 */
	public static class Options
	{
		private PrintStream stdout;
		private PrintStream stderr;
		
		private boolean help;
		private boolean version;

		private File inFile;
		
		private Charset outCharset;
		private File outFile;
		private File mapinfoOut;
		private File zMapinfoOut;
		private File eMapinfoOut;
		
		public Options()
		{
			this.stdout = null;
			this.stderr = null;
			this.help = false;
			this.version = false;

			this.inFile = null;
			
			this.outCharset = ASCII;
			this.outFile = null;
			this.mapinfoOut = null;
			this.zMapinfoOut = null;
			this.eMapinfoOut = null;
		}
		
		public Options setHelp(boolean help) 
		{
			this.help = help;
			return this;
		}
		
		public Options setVersion(boolean version) 
		{
			this.version = version;
			return this;
		}
		
		public Options setInFile(File inFile) 
		{
			this.inFile = inFile;
			return this;
		}
		
		public Options setOutCharset(Charset outCharset) 
		{
			this.outCharset = outCharset;
			return this;
		}
		
		public Options setOutFile(File outFile) 
		{
			this.outFile = outFile;
			return this;
		}
		
		public Options setMapinfoOut(File mapinfoOut) 
		{
			this.mapinfoOut = mapinfoOut;
			return this;
		}
		
		public Options setZMapinfoOut(File zMapinfoOut)
		{
			this.zMapinfoOut = zMapinfoOut;
			return this;
		}
		
		public Options setEMapinfoOut(File eMapinfoOut) 
		{
			this.eMapinfoOut = eMapinfoOut;
			return this;
		}
		
	}
	
	/**
	 * Program context.
	 */
	private static class Context
	{
		private Options options;
	
		private Context(Options options)
		{
			this.options = options;
		}
		
		public int call()
		{
			if (options.help)
			{
				splash(options.stdout);
				usage(options.stdout);
				options.stdout.println();
				help(options.stdout);
				return ERROR_NONE;
			}
			
			if (options.version)
			{
				splash(options.stdout);
				return ERROR_NONE;
			}
		
			if (options.inFile == null)
			{
				options.stderr.println("ERROR: Missing input file.");
				return ERROR_MISSING_INPUT;
			}

			if (!options.inFile.exists())
			{
				options.stderr.println("ERROR: Input file does not exist.");
				return ERROR_MISSING_INPUT_FILE;
			}

			if (options.outFile == null)
			{
				options.stdout.printf("NOTE: Output file not specified, defaulting to %s.\n", DEFAULT_OUTFILENAME);
				options.outFile = new File(DEFAULT_OUTFILENAME);
			}

			// Read script.
			AbstractPatchContext<?> context;
			try 
			{
				context = DecoHackParser.read(options.inFile);
			} 
			catch (DecoHackParseException e) 
			{
				options.stderr.println("ERROR: " + e.getLocalizedMessage());
				return ERROR_PARSEERROR;
			} 
			catch (FileNotFoundException e) 
			{
				options.stderr.println("ERROR: Input file does not exist.");
				return ERROR_MISSING_INPUT_FILE;
			} 
			catch (IOException e) 
			{
				options.stderr.println("ERROR: I/O Error: " + e.getLocalizedMessage());
				return ERROR_IOERROR;
			} 
			catch (SecurityException e) 
			{
				options.stderr.println("ERROR: Could not open input file (access denied).");
				return ERROR_SECURITY;
			}
			
			// Write Patch.
			try (Writer writer = new FileWriter(options.outFile, options.outCharset)) 
			{
				DecoHackExporter.writePatch(context, writer, "Created with " + SPLASH_VERSION);
				options.stdout.printf("Wrote %s.\n", options.outFile.getPath());
			} 
			catch (IOException e) 
			{
				options.stderr.println("ERROR: I/O Error: " + e.getLocalizedMessage());
				return ERROR_IOERROR;
			}
			catch (SecurityException e) 
			{
				options.stderr.println("ERROR: Could not open input file (access denied).");
				return ERROR_SECURITY;
			}
			
			// TODO: Finish other modes/options.
			
			return ERROR_NONE;
		}
	}
	
	/**
	 * Reads command line arguments and sets options.
	 * @param out the standard output print stream.
	 * @param err the standard error print stream. 
	 * @param args the argument args.
	 * @return the parsed options.
	 * @throws OptionParseException if a parse exception occurs.
	 */
	public static Options options(PrintStream out, PrintStream err, String ... args) throws OptionParseException
	{
		Options options = new Options();
		options.stdout = out;
		options.stderr = err;
	
		final int STATE_START = 0;
		final int STATE_OUTFILE = 1;
		final int STATE_OUTCHARSET = 2;
		int state = STATE_START;

		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			
			switch (state)
			{
				case STATE_START:
				{
					if (arg.equals(SWITCH_HELP) || arg.equals(SWITCH_HELP2))
						options.setHelp(true);
					else if (arg.equals(SWITCH_VERSION))
						options.setVersion(true);
					else if (arg.equals(SWITCH_OUTPUT) || arg.equals(SWITCH_OUTPUT2))
						state = STATE_OUTFILE;
					else if (arg.equals(SWITCH_OUTPUTCHARSET) || arg.equals(SWITCH_OUTPUTCHARSET2))
						state = STATE_OUTCHARSET;
					else
						options.inFile = new File(arg);
				}
				break;
				
				case STATE_OUTFILE:
				{
					options.outFile = new File(arg);
					state = STATE_START;
				}
				break;

				case STATE_OUTCHARSET:
				{
					try {
						options.outCharset = Charset.forName(arg);
					} catch (IllegalCharsetNameException e) {
						throw new OptionParseException("ERROR: Bad charset name: " + arg);
					} catch (UnsupportedCharsetException e) {
						throw new OptionParseException("ERROR: Unsupported charset name: " + arg);
					}
					state = STATE_START;
				}
				break;

			}
		}
		
		if (state == STATE_OUTFILE)
			throw new OptionParseException("ERROR: Expected output file.");
		if (state == STATE_OUTCHARSET)
			throw new OptionParseException("ERROR: Expected output charset name.");
		
		return options;
	}
	
	/**
	 * Calls the utility using a set of options.
	 * @param options the options to call with.
	 * @return the error code.
	 */
	public static int call(Options options)
	{
		return (new Context(options)).call();
	}
	
	public static void main(String[] args) throws IOException
	{
		if (args.length == 0)
		{
			splash(System.out);
			usage(System.out);
			System.exit(-1);
			return;
		}
	
		try {
			System.exit(call(options(System.out, System.err, args)));
		} catch (OptionParseException e) {
			System.err.println(e.getMessage());
			System.exit(ERROR_BAD_OPTIONS);
		}
	}
	
	/**
	 * Prints the splash.
	 * @param out the print stream to print to.
	 */
	private static void splash(PrintStream out)
	{
		out.println(SPLASH_VERSION);
	}
	
	/**
	 * Prints the usage.
	 * @param out the print stream to print to.
	 */
	private static void usage(PrintStream out)
	{
		out.println("Usage: decohack [--help | -h | --version]");
		out.println("                [filename] [switches]");
	}
	
	/**
	 * Prints the help.
	 * @param out the print stream to print to.
	 */
	private static void help(PrintStream out)
	{
		out.println("    --help                   Prints help and exits.");
		out.println("    -h");
		out.println();
		out.println("    --version                Prints version, and exits.");
		out.println();
		out.println("[filename]:");
		out.println("    <filename>               The input filename.");
		out.println();
		out.println("[switches]:");
		out.println("    --output [file]          Outputs the resultant patch to [file].");
		out.println("    -o [file]");
		out.println();
		out.println("    --output-charset [name]  Sets the output charset to [name]. The default");
		out.println("    -oc [name]               charset is ASCII, and there are not many reasons");
		out.println("                             to change.");
	}

}
