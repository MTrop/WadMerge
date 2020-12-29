/*******************************************************************************
 * Copyright (c) 2020 Matt Tropiano
 * This program and the accompanying materials are made available under 
 * the terms of the MIT License, which accompanies this distribution.
 ******************************************************************************/
package net.mtrop.doom.tools.wadmerge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeMap;

import net.mtrop.doom.Wad;
import net.mtrop.doom.WadBuffer;
import net.mtrop.doom.WadEntry;
import net.mtrop.doom.WadFile;
import net.mtrop.doom.exception.WadException;
import net.mtrop.doom.texture.Animated;
import net.mtrop.doom.texture.CommonTextureList;
import net.mtrop.doom.texture.DoomTextureList;
import net.mtrop.doom.texture.PatchNames;
import net.mtrop.doom.texture.StrifeTextureList;
import net.mtrop.doom.texture.Switches;
import net.mtrop.doom.texture.TextureSet;
import net.mtrop.doom.tools.common.Response;
import net.mtrop.doom.tools.common.Utility;
import net.mtrop.doom.tools.common.Common;
import net.mtrop.doom.tools.common.ParseException;
import net.mtrop.doom.util.MapUtils;
import net.mtrop.doom.util.NameUtils;

/**
 * The main context for WadMerge.
 * @author Matthew Tropiano
 */
public class WadMergeContext
{
	/** Simple date format. */
	private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(
		()->new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ")
	);
	
	/** Comparator for MERGEDIR file sorting. */
	private static final Comparator<File> DIR_FILESORT = (a, b) -> 
	{
		if (a.isFile())
		{
			if (b.isDirectory())
				return -1;
			else
				return a.getPath().compareTo(b.getPath());
		}
		else
		{
			if (b.isFile())
				return 1;
			else
				return a.getPath().compareTo(b.getPath());
		}
	};

	/** Map of open wads. */
	private TreeMap<String, Wad> currentWads;
	/** Log out print stream. */
	private PrintStream logout;
	/** If verbosity is enabled. */
	private boolean verbose;

	/**
	 * Creates a new context. No output.
	 */
	public WadMergeContext()
	{
		this(null, false);
	}
	
	/**
	 * Creates a new context.
	 * @param log the output print stream to use for logging (can be null).
	 * @param verbose if true, do verbose printing (only matters if log is not null).
	 */
	public WadMergeContext(PrintStream log, boolean verbose)
	{
		this.currentWads = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		this.logout = log;
		this.verbose = verbose;
	}
	
	public void verboseln(String seq)
	{
		if (verbose)
			logln(seq);
	}
	
	public void verbosef(String seq, Object... args)
	{
		if (verbose)
			logf(seq, args);
	}

	public void logln(String seq)
	{
		if (logout != null)
			logout.println(seq);
	}
	
	public void logf(String seq, Object... args)
	{
		if (logout != null)
			logout.printf(seq, args);
	}
	
	/**
	 * Sets verbosity.
	 * @param verbose the new verbosity flag.
	 * @return OK.
	 */
	public Response setVerbose(boolean verbose)
	{
		if (!verbose)
			verboseln("Verbosity OFF.");
		this.verbose = verbose;
		verboseln("Verbosity ON.");
		return Response.OK;
	}
	
	/**
	 * Creates a blank Wad buffer.
	 * Symbol is case-insensitive.
	 * @param symbol the symbol to associate with the Wad.
	 * @return OK if the buffer does not exist and it was created, BAD_SYMBOL otherwise.
	 */
	public Response create(String symbol)
	{
		if (currentWads.containsKey(symbol))
			return Response.BAD_SYMBOL;
		
		currentWads.put(symbol, new WadBuffer());
		verbosef("Created buffer `%s`.\n", symbol);
		return Response.OK;
	}

	/**
	 * Creates a blank Wad file.
	 * Symbol is case-insensitive.
	 * @param symbol the symbol to associate with the Wad.
	 * @param wadFile the file name for the WAD to initialize.
	 * @return OK if the Wad does not exist and it was created, BAD_SYMBOL otherwise.
	 * @throws IOException if an error occurs attempting to create the file.
	 */
	public Response createFile(String symbol, File wadFile) throws IOException
	{
		if (currentWads.containsKey(symbol))
			return Response.BAD_SYMBOL;
		
		currentWads.put(symbol, WadFile.createWadFile(wadFile));
		verbosef("Created buffer `%s`.\n", symbol);
		return Response.OK;
	}

	/**
	 * Checks if a symbol refers to a valid buffer.
	 * Symbol is case-insensitive.
	 * @param symbol the symbol to check.
	 * @return OK if the buffer exists, BAD_SYMBOL otherwise.
	 */
	public Response isValid(String symbol)
	{
		Response out = currentWads.containsKey(symbol) ? Response.OK : Response.BAD_SYMBOL;
		if (out == Response.OK)
			verbosef("Symbol `%s` is valid.\n", symbol);
		else
			verbosef("Symbol `%s` is invalid.\n", symbol);
		return out;
	}

	/**
	 * Clears an existing Wad buffer.
	 * Symbol is case-insensitive.
	 * @param symbol the symbol to clear.
	 * @return OK if the buffer exists and it was cleared, BAD_SYMBOL otherwise.
	 * @throws IOException 
	 */
	public Response clear(String symbol) throws IOException
	{
		if (!currentWads.containsKey(symbol))
			return Response.BAD_SYMBOL;
		
		Wad buffer = currentWads.remove(symbol);
		verbosef("Cleared buffer `%s`.\n", symbol);
		buffer.close();
		if (buffer instanceof WadBuffer)
			return create(symbol);
		else if (buffer instanceof WadFile)
			return createFile(symbol, new File(((WadFile)buffer).getFilePath()));
		else
			return Response.UNEXPECTED_ERROR;
	}

	/**
	 * Discards an existing Wad buffer.
	 * Symbol is case-insensitive.
	 * @param symbol the symbol to discard.
	 * @return OK if the buffer exists and it was discarded, BAD_SYMBOL otherwise.
	 * @throws IOException if the Wad could not be closed.
	 */
	public Response discard(String symbol) throws IOException
	{
		if (!currentWads.containsKey(symbol))
			return Response.BAD_SYMBOL;

		currentWads.remove(symbol).close();
		verbosef("Discarded buffer `%s`.\n", symbol);
		return Response.OK;
	}

	/**
	 * Loads the contents of a Wad file into a buffer.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to merge into.
	 * @param wadFile the file to read from.
	 * @return OK if the file was found and contents were merged in, or BAD_SYMBOL if the symbol is invalid. 
	 * @throws IOException if the file could not be read.
	 * @throws WadException if the file is not a Wad file.
	 */
	public Response load(String symbol, File wadFile) throws IOException, WadException
	{
		if (!wadFile.exists() || wadFile.isDirectory())
			return Response.BAD_FILE;
	
		if (!Wad.isWAD(wadFile))
			return Response.BAD_WAD;
	
		Response out;
		if ((out = create(symbol)) != Response.OK)
			return out;
		if ((out = mergeWad(symbol, wadFile)) != Response.OK)
			return out;
		return Response.OK;
	}

	/**
	 * Saves the contents of a Wad buffer into a file.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to write.
	 * @param outFile the file to read from.
	 * @return OK if the file was written, or BAD_SYMBOL if the symbol is invalid. 
	 * @throws IOException if the file could not be written.
	 */
	public Response save(String symbol, File outFile) throws IOException
	{
		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;
	
		Common.createPathForFile(outFile);
		
		if (buffer instanceof WadBuffer)
			((WadBuffer)buffer).writeToFile(outFile);
		else if (buffer instanceof WadFile)
		{
			File wadFile = new File(((WadFile)buffer).getFilePath());
			if (!wadFile.equals(outFile))
				WadFile.extract(outFile, buffer, 0, buffer.getEntryCount());
			// Do nothing if same file.
		}
		else
			return Response.UNEXPECTED_ERROR;

		logf("Wrote file `%s`.\n", outFile.getPath());
		return Response.OK;
	}

	/**
	 * Saves the contents of a Wad buffer into a file, and discards the buffer at the symbol.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to write.
	 * @param outFile the file to read from.
	 * @return OK if the file was written, or BAD_SYMBOL if the symbol is invalid. 
	 * @throws IOException if the file could not be written.
	 */
	public Response finish(String symbol, File outFile) throws IOException
	{
		Response out;
		if ((out = save(symbol, outFile)) != Response.OK)
			return out;
		return discard(symbol);
	}

	/**
	 * Adds a marker to a Wad buffer.
	 * Symbol is case-insensitive. The entry is coerced to a valid name.
	 * @param symbol the symbol to use.
	 * @param name the entry name.
	 * @return OK if the buffer exists and was added to, false otherwise.
	 */
	public Response addMarker(String symbol, String name)
	{
		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;
		
		try {
			String marker = NameUtils.toValidEntryName(name);
			buffer.addMarker(marker);
			verbosef("Added marker `%s` to buffer `%s`.\n", marker, symbol);
		} catch (IOException e) {
			// Shouldn't happen.
			return Response.UNEXPECTED_ERROR;
		}
		return Response.OK;
	}

	/**
	 * Adds an entry to a Wad buffer that contains the current date.
	 * Symbol is case-insensitive. The entry is coerced to a valid name.
	 * @param symbol the symbol to use.
	 * @param name the entry name.
	 * @return OK if the buffer exists and was added to, BAD_SYMBOL otherwise.
	 */
	public Response addDateMarker(String symbol, String name)
	{
		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;
		
		try {
			String marker = NameUtils.toValidEntryName(name);
			buffer.addData(marker, DATE_FORMAT.get().format(new Date()).getBytes(Charset.forName("ASCII")));
			verbosef("Added date marker `%s` to buffer `%s`.\n", marker, symbol);
		} catch (IOException e) {
			// Shouldn't happen.
			return Response.UNEXPECTED_ERROR;
		}
		return Response.OK;
	}

	/**
	 * Merges a Wad buffer into another.
	 * The symbols are case-insensitive.
	 * @param destinationSymbol the destination buffer.
	 * @param sourceSymbol the source buffer.
	 * @return OK if both buffers exist and the merge worked, BAD_SYMBOL / BAD_SOURCE_SYMBOL otherwise.
	 */
	public Response mergeBuffer(String destinationSymbol, String sourceSymbol)
	{
		destinationSymbol = destinationSymbol.toLowerCase();
		Wad bufferDest;
		if ((bufferDest = currentWads.get(destinationSymbol)) == null)
			return Response.BAD_SYMBOL;
		
		sourceSymbol = sourceSymbol.toLowerCase();
		Wad bufferSource;
		if ((bufferSource = currentWads.get(sourceSymbol)) == null)
			return Response.BAD_SOURCE_SYMBOL;

		try 
		{
			for (WadEntry e : bufferSource)
			{
				bufferDest.addData(e.getName(), bufferSource.getData(e));
				verbosef("Added entry `%s` to buffer `%s` (from `%s`).\n", e.getName(), destinationSymbol, sourceSymbol);
			}
		} 
		catch (IOException e) 
		{
			// Shouldn't happen.
			return Response.UNEXPECTED_ERROR;
		}
		
		return Response.OK;
	}
	
	/**
	 * Merges the contents of a Wad into a buffer.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to merge into.
	 * @param wadFile the file to read from.
	 * @return OK if the file was found and contents were merged in, false otherwise. 
	 * @throws IOException if the file could not be read.
	 */
	public Response mergeWad(String symbol, File wadFile) throws IOException
	{
		if (!wadFile.exists() || wadFile.isDirectory())
			return Response.BAD_FILE;

		if (!Wad.isWAD(wadFile))
			return Response.BAD_WAD;

		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;
		
		WadFile.Adder adder = (buffer instanceof WadFile) ? ((WadFile)buffer).createAdder() : null;
		
		try (WadFile wad = new WadFile(wadFile))
		{
			verbosef("Reading WAD `%s`...\n", wadFile.getPath());
			for (WadEntry e : wad)
			{
				if (adder != null)
					adder.addData(e.getName(), wad.getData(e));
				else
					buffer.addData(e.getName(), wad.getData(e));
				verbosef("Added entry `%s` to buffer `%s`.\n", e.getName(), symbol);
			}
			verbosef("Done reading `%s`.\n", wadFile.getPath());
		} finally {
			Common.close(adder);
		}
		
		return Response.OK;
	}

	// Merge map into buffer, with rename.
	private static Response mergeMap(Wad buffer, String bufferName, String newHeader, Wad source, String header) throws IOException
	{
		WadEntry[] entries = MapUtils.getMapEntries(source, header);
		if (entries.length == 0)
			return Response.BAD_MAP;
		
		for (WadEntry e : entries)
		{
			String outname;
			if ((outname = e.getName()).equalsIgnoreCase(header))
				outname = NameUtils.toValidEntryName(newHeader);
			buffer.addData(outname, source.getData(e));
		}
		
		return Response.OK;
	}
	
	private Response mergeFileData(String symbol, File inFile, String entryName, Wad buffer) throws IOException
	{
		entryName = NameUtils.toValidEntryName(entryName);
		buffer.addData(entryName, inFile);
		verbosef("Added `%s` to `%s` (from `%s`).\n", entryName, symbol, inFile.getPath());
		return Response.OK;
	}

	private Response mergeFileData(String symbol, File inFile, String entryName, WadFile.Adder adder) throws IOException
	{
		entryName = NameUtils.toValidEntryName(entryName);
		adder.addData(entryName, inFile);
		verbosef("Added `%s` to `%s` (from `%s`).\n", entryName, symbol, inFile.getPath());
		return Response.OK;
	}

	/**
	 * Merges a single map from a Wad file into a buffer.
	 * Symbol is case-insensitive, as well as entry. The new entry is coerced to a valid name.
	 * @param symbol the buffer to merge into.
	 * @param newHeader the new header name.
	 * @param wadFile the file to read from.
	 * @param header the map header.
	 * @return OK if the file was found and contents were merged in, BAD_SYMBOL otherwise. 
	 * @throws IOException if the file could not be read.
	 * @throws WadException if the file is not a Wad file.
	 */
	public Response mergeMap(String symbol, String newHeader, File wadFile, String header) throws IOException, WadException
	{
		if (!wadFile.exists() || wadFile.isDirectory())
			return Response.BAD_FILE;

		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;
		
		try (WadFile wad = new WadFile(wadFile))
		{
			Response out = mergeMap(buffer, symbol, newHeader, wad, header);
			verbosef("Added map `%s` to `%s` as `%s` (from `%s`).\n", header, symbol, newHeader, wadFile.getPath());
			return out;
		}		
	}
	
	/**
	 * Merges a single map from an existing buffer into a buffer.
	 * Symbol is case-insensitive, as well as entry. The new entry is coerced to a valid name.
	 * @param destinationSymbol the buffer to merge into.
	 * @param newHeader the new header name.
	 * @param sourceSymbol the buffer to read from.
	 * @param header the map header.
	 * @return OK if the file was found and contents were merged in, false otherwise. 
	 * @throws IOException if the file could not be read.
	 * @throws WadException if the file is not a Wad file.
	 */
	public Response mergeMap(String destinationSymbol, String newHeader, String sourceSymbol, String header) throws IOException, WadException
	{
		destinationSymbol = destinationSymbol.toLowerCase();
		Wad bufferDest;
		if ((bufferDest = currentWads.get(destinationSymbol)) == null)
			return Response.BAD_SYMBOL;
		
		sourceSymbol = sourceSymbol.toLowerCase();
		Wad bufferSource;
		if ((bufferSource = currentWads.get(sourceSymbol)) == null)
			return Response.BAD_SOURCE_SYMBOL;
		
		Response out = mergeMap(bufferDest, destinationSymbol, newHeader, bufferSource, header);
		verbosef("Added map `%s` to `%s` as `%s` (from `%s`).\n", header, destinationSymbol, newHeader, sourceSymbol);
		return out;
	}
		
	/**
	 * Merges a single file as an entry into a buffer.
	 * Symbol is case-insensitive. The entry is coerced to a valid name.
	 * @param symbol the buffer to merge into.
	 * @param inFile the file to read.
	 * @param entryName the name of the entry to write as (coerced to a valid name).
	 * @return OK if the file was found and contents were merged in, false otherwise. 
	 * @throws IOException if the file could not be read.
	 * @throws WadException if the file is not a Wad file.
	 */
	public Response mergeFile(String symbol, File inFile, String entryName) throws IOException, WadException
	{
		if (!inFile.exists() || inFile.isDirectory())
			return Response.BAD_FILE;

		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;

		return mergeFileData(symbol, inFile, entryName, buffer);
	}

	/**
	 * Iterates through a directory, adding each file's data into the buffer, 
	 * and if that file is a valid WAD file, it's entries and data are added.
	 * If it encounters a directory, it is skipped.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to write.
	 * @param inDirectory the directory to read from.
	 * @return OK if the file was written, or BAD_SYMBOL if the symbol is invalid, or BAD_DIRECTORY if the provided file is not a directory. 
	 * @throws IOException if the file could not be written.
	 */
	public Response mergeDirectory(String symbol, File inDirectory) throws IOException
	{
		if (!inDirectory.exists() || !inDirectory.isDirectory())
			return Response.BAD_DIRECTORY;

		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;

		File[] files;
		
		// Sort files first, directories last, alphabetical order.
		Arrays.sort(files = inDirectory.listFiles(), DIR_FILESORT);
		
		WadFile.Adder adder = null;
		try {
			for (File f : files)
			{
				Response resp;
				if (f.isDirectory())
					continue;
				else if (Common.getFileExtension(f).equalsIgnoreCase("wad") && Wad.isWAD(f))
				{
					if (adder != null)
					{
						adder.close();
						adder = null;
					}
					if ((resp = mergeWad(symbol, f)) != Response.OK)
						return resp; 
				}
				else if (buffer instanceof WadFile)
				{
					if (adder == null)
						adder = ((WadFile)buffer).createAdder();
					if ((resp = mergeFileData(symbol, f, Common.getFileNameWithoutExtension(f), adder)) != Response.OK)
						return resp; 
				}
				else
				{
					if ((resp = mergeFile(symbol, f, Common.getFileNameWithoutExtension(f))) != Response.OK)
						return resp; 
				}
			}
		} finally {
			Common.close(adder);
		}
		return Response.OK;
	}

	/**
	 * Iterates through a directory, adding each file's data into the buffer, 
	 * and if that file is a valid WAD file, it's entries and data are added.
	 * If it encounters a directory, a marker is added (directory name prepended with a backslash), 
	 * and {@link #mergeDirectory(String, File)} is called on it.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to write to.
	 * @param inDirectory the directory to read from.
	 * @return OK if the file was written, or BAD_SYMBOL if the symbol is invalid, or BAD_DIRECTORY if the provided file is not a directory. 
	 * @throws IOException if the file could not be written.
	 */
	public Response mergeTree(String symbol, File inDirectory) throws IOException
	{
		if (!inDirectory.exists() || !inDirectory.isDirectory())
			return Response.BAD_DIRECTORY;

		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;

		File[] files;
		
		// Sort files first, directories last, alphabetical order.
		Arrays.sort(files = inDirectory.listFiles(), DIR_FILESORT);

		WadFile.Adder adder = null;
		try {
			for (File f : files)
			{
				Response resp;
				if (f.isDirectory())
				{
					if (adder != null)
					{
						adder.close();
						adder = null;
					}
					verbosef("Scan directory `%s`...\n", f.getPath());
					if ((resp = addMarker(symbol, "\\" + f.getName())) != Response.OK)
						return resp; 
					if ((resp = mergeTree(symbol, f)) != Response.OK)
						return resp; 
					verbosef("Done scanning directory `%s`.\n", f.getPath());
				}
				else if (Common.getFileExtension(f).equalsIgnoreCase("wad") && Wad.isWAD(f))
				{
					if (adder != null)
					{
						adder.close();
						adder = null;
					}
					if ((resp = mergeWad(symbol, f)) != Response.OK)
						return resp; 
				}
				else if (buffer instanceof WadFile)
				{
					if (adder == null)
						adder = ((WadFile)buffer).createAdder();
					if ((resp = mergeFileData(symbol, f, Common.getFileNameWithoutExtension(f), adder)) != Response.OK)
						return resp; 
				}
				else
				{
					if ((resp = mergeFile(symbol, f, Common.getFileNameWithoutExtension(f))) != Response.OK)
						return resp; 
				}
			}
		} finally {
			Common.close(adder);
		}
		
		return Response.OK;
	}

	/**
	 * Merges a DEUTEX texture file into TEXTUREX/PNAMES entries in a buffer, 
	 * using the name of the texture lump is the name of the file.
	 * Will read in an existing PNAMES lump and/or matching texture lump if it exists in the buffer.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to write to.
	 * @param textureFile the texture file to parse.
	 * @param strife if true, will read and export in Strife format, false for Doom format. 
	 * @param textureEntryName the name of the texture entry name.
	 * @return OK if the file was found and contents were merged in, 
	 * 		or BAD_SYMBOL if the symbol is invalid, 
	 * 		or BAD_PARSE if the file is incorrect,
	 * 		or BAD_FILE if it does not exist or is a directory.
	 * @throws IOException if the file could not be read.
	 */
	@SuppressWarnings("unchecked")
	public Response mergeDEUTEXTextureFile(String symbol, File textureFile, boolean strife, String textureEntryName) throws IOException
	{
		if (!textureFile.exists() || textureFile.isDirectory())
			return Response.BAD_FILE;

		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;

		PatchNames pout;
		if (buffer.contains("PNAMES"))
			pout = buffer.getDataAs("PNAMES", PatchNames.class);
		else
			pout = new PatchNames();

		CommonTextureList<?> tout;
		if (buffer.contains(textureEntryName))
		{
			if (strife)
				tout = buffer.getDataAs(textureEntryName, StrifeTextureList.class);
			else
				tout = buffer.getDataAs(textureEntryName, DoomTextureList.class);
		}
		else
		{
			tout = strife ? new StrifeTextureList(128) : new DoomTextureList(128);
		}

		TextureSet textureSet;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(textureFile))))
		{
			textureSet = Utility.readDEUTEXFile(reader, pout, tout);
		} 
		catch (ParseException e) 
		{
			logln("ERROR: "+ textureFile + ", " + e.getMessage());
			return Response.BAD_PARSE;
		}
		
		if (strife)
			textureSet.export(pout = new PatchNames(), (CommonTextureList<StrifeTextureList.Texture>)(tout = new StrifeTextureList(128)));
		else
			textureSet.export(pout = new PatchNames(), (CommonTextureList<DoomTextureList.Texture>)(tout = new DoomTextureList(128)));

		if (buffer.contains("PNAMES"))
			buffer.deleteEntry(buffer.indexOf("PNAMES"));
		
		textureEntryName = NameUtils.toValidEntryName(textureEntryName);
		buffer.addData(textureEntryName, tout);
		verbosef("Added `%s` to `%s`.\n", textureEntryName, symbol);
		buffer.addData("PNAMES", pout);
		verbosef("Added `PNAMES` to `%s`.\n", symbol);

		return Response.OK;
	}

	/**
	 * Creates TEXTUREX/PNAMES entries in a buffer, using a directory of patches as the only textures,
	 * and imports all of the patch files between PP_START and PP_END markers.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to write to.
	 * @param textureDirectory the texture file to parse.
	 * @param textureEntryName the name of the texture entry name.
	 * @return OK if the file was found and contents were merged in, 
	 * 		or BAD_SYMBOL if the symbol is invalid, 
	 * 		or BAD_DIRECTORY if the provided file is not a directory.
	 * @throws IOException if the file could not be read.
	 */
	public Response mergeTextureDirectory(String symbol, File textureDirectory, String textureEntryName) throws IOException
	{
		if (!textureDirectory.exists() || !textureDirectory.isDirectory())
			return Response.BAD_DIRECTORY;

		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;

		Response resp;
		if ((resp = addMarker(symbol, "PP_START")) != Response.OK)
			return resp;
		
		TextureSet textureSet = new TextureSet(new PatchNames(), new DoomTextureList(128));
		WadFile.Adder adder = (buffer instanceof WadFile) ? ((WadFile)buffer).createAdder() : null;

		File[] files;
		
		// Sort files first, directories last, alphabetical order.
		Arrays.sort(files = textureDirectory.listFiles(), DIR_FILESORT);

		try {
			for (File f : files)
			{
				if (f.isDirectory())
				{
					verbosef("Skipping directory `%s`...\n", f.getPath());
					continue;
				}
				else
				{
					String namenoext = Common.getFileNameWithoutExtension(f);
					if (adder != null)
					{
						if ((resp = mergeFileData(symbol, f, namenoext, adder)) != Response.OK)
							return resp;
					}
					else
					{
						if ((resp = mergeFileData(symbol, f, namenoext, buffer)) != Response.OK)
							return resp;
					}
					String textureName = NameUtils.toValidTextureName(namenoext);
					textureSet.createTexture(textureName).createPatch(textureName);
					verbosef("Add texture `%s`...\n", textureName);
				}
			}
		} finally {
			Common.close(adder);
		}

		if ((resp = addMarker(symbol, "PP_END")) != Response.OK)
			return resp;

		PatchNames pout;
		DoomTextureList tout;
		textureSet.export(pout = new PatchNames(), tout = new DoomTextureList());

		textureEntryName = NameUtils.toValidEntryName(textureEntryName);
		buffer.addData(textureEntryName, tout);
		verbosef("Added `%s` to `%s`.\n", textureEntryName, symbol);
		buffer.addData("PNAMES", pout);
		verbosef("Added `PNAMES` to `%s`.\n", symbol);
		
		return Response.OK;
	}
	
	/**
	 * Creates ANIMATED and SWITCHES entries in a buffer using a table file read by SWANTBLS.
	 * If ANIMATED and SWITCHES exist, they are appended to.
	 * Symbol is case-insensitive.
	 * @param symbol the buffer to write to.
	 * @param swantblsFile the texture file to parse.
	 * @return OK if the file was found and contents were merged in, 
	 * 		or BAD_SYMBOL if the symbol is invalid, 
	 * 		or BAD_DIRECTORY if the provided file is not a directory.
	 * @throws IOException if the file could not be read.
	 */
	public Response mergeSwitchAnimatedTables(String symbol, File swantblsFile) throws IOException
	{
		if (!swantblsFile.exists() || swantblsFile.isDirectory())
			return Response.BAD_FILE;

		Wad buffer;
		if ((buffer = currentWads.get(symbol)) == null)
			return Response.BAD_SYMBOL;

		Animated animated;
		if ((animated = buffer.getDataAs("ANIMATED", Animated.class)) == null)
			animated = new Animated();
		Switches switches;
		if ((switches = buffer.getDataAs("SWITCHES", Switches.class)) == null)
			switches = new Switches();
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(swantblsFile))))
		{
			Utility.readSwitchAnimatedTables(reader, animated, switches);
			buffer.addData("ANIMATED", animated);
			verbosef("Added `ANIMATED` to `%s`.\n", symbol);
			buffer.addData("SWITCHES", switches);
			verbosef("Added `SWITCHES` to `%s`.\n", symbol);
			return Response.OK;
		}
		catch (ParseException e)
		{
			logln("ERROR: "+ swantblsFile + ", " + e.getMessage());
			return Response.BAD_PARSE;
		}
	}
	
}
