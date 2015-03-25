package glofacs.io;

import glofacs.data.ChannelInfo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * 
 * 3.1
 * http://www.isac-net.org/index.php?option=com_content&task=view&id=828&Itemid=150
 * http://www.ncbi.nlm.nih.gov/pmc/articles/PMC2892967/
 * 
 * 3.0
 * http://www.isac-net.org/index.php?option=com_content&task=view&id=101&Itemid=150
 * http://murphylab.web.cmu.edu/publications/64-seamer1997.pdf
 *
 * 2.0
 * http://www.ncbi.nlm.nih.gov/pubmed/2340769
 *
 * Gating-ML
 * http://flowcyt.sourceforge.net/gating/
 *
 * The Union Bio COPAS LMD-files are in fact FCS-files
 * 
 * @author Johan Henriksson
 *
 */
public class FCSFile
	{
	public ArrayList<DataSegment> data=new ArrayList<DataSegment>(); 
	
	private static String readString(DataInputStream r, int count) throws IOException
		{		
		byte[] buf=new byte[count];
		r.readFully(buf);
		
		InputStream is=new ByteArrayInputStream(buf);
		BufferedReader r2=new BufferedReader(new InputStreamReader(is));
		//r2.read(cbuf)
		//new String()
		return r2.readLine();
		}
	
	private static long readOffset(DataInputStream r) throws IOException
		{
		String s=readString(r,8);
		int i=0;
		while(s.charAt(i)==' ')
			i++;
		s=s.substring(i);
		return Long.parseLong(s);
		}
	
	private static int readA(DataInputStream r, int count) throws IOException
		{
		String s=readString(r,count);
		int i=0;
		while(s.charAt(i)==' ')
			i++;
		s=s.substring(i);
		return Integer.parseInt(s);
		}
	
	private static boolean isPnX(String tok, String x)
		{
		if(tok.startsWith("$P") && tok.endsWith(x)) 
			{
			try
				{
				Integer.parseInt(tok.substring(2,tok.length()-1));
				return true;
				}
			catch (NumberFormatException e)
				{
				System.out.println(e);
				}
			}
		return false;
		}

	
	
	
	private static long parseDataValue(String val)
		{
		if(val.endsWith("I"))
			val=val.substring(0,val.length()-1); //This came up in a FacStar dataset. not part of standard?
		int i=0;
		while(val.charAt(i)==' ')
			i++;
		val=val.substring(i);
		return Long.parseLong(val.trim());
		}
	
	
	
	
	/**
	 * One data block - an FCS file can contain multiple but the standard encourages against
	 * 
	 * @author Johan Henriksson
	 *
	 */
	public static class DataSegment
		{
		public ArrayList<int[]> eventsInt;
		public ArrayList<double[]> eventsFloat;
		
		public String fcsversion;
		
		public TreeMap<String, String> otherKeywords=new TreeMap<String, String>();
		public HashMap<Integer, Integer> numBitsForPar=new HashMap<Integer, Integer>();
		public HashMap<Integer, String> ampTypeForPar=new HashMap<Integer, String>();
		public HashMap<Integer, String> shortNameForPar=new HashMap<Integer, String>();
		public HashMap<Integer, String> nameForPar=new HashMap<Integer, String>();
		public HashMap<Integer, String> rangeForPar=new HashMap<Integer, String>();
		public HashMap<Integer, String> detectorVoltage=new HashMap<Integer, String>();
		public HashMap<Integer, String> amplifierGain=new HashMap<Integer, String>();
		
		public String projectName;
		public String experimenter;
		public String operator;
		public String institute;
		public String cytometerType;
		public String cytometerSerial;
		public String computer;
		public String specimen;
		public String specimenSource;
		public String plateID;
		public String plateName;
		public String wellID;
		public String comment;
		public String cellsDescription;
		public String acquisitionDate;
		public String beginTime;
		public String endTime;
		public String timestep;
		public String originalFile;
		public File source;
		
		
		public ArrayList<ChannelInfo> getChannelInfo()
			{
			ArrayList<ChannelInfo> list=new ArrayList<ChannelInfo>();
			
			int numchan=0;
			numchan=eventsFloat.get(0).length; //hack
			
			for(int id=1;id<=numchan;id++)
				{
				ChannelInfo ch=new ChannelInfo();
				ch.label=nameForPar.get(id);
				ch.name=shortNameForPar.get(id);
				list.add(ch);
				}
			return list;
			}
		
		/**
		 * Parse one block, starting at a given offset in the file
		 */
		private Long parse(File f, int offsetHeader) throws IOException
			{
			////// Header //////
			FileInputStream fi=new FileInputStream(f);
			fi.skip(offsetHeader);
			DataInputStream rHeader=new DataInputStream(fi);
			
			fcsversion=readString(rHeader, 10);
			
			long offsetTextStart=readOffset(rHeader);
			long offsetTextEnd=readOffset(rHeader);
			long offsetDataStart=readOffset(rHeader);
			@SuppressWarnings("unused")
			long offsetDataEnd=readOffset(rHeader);
			@SuppressWarnings("unused")
			long offsetAnalysisStart=readOffset(rHeader);
			@SuppressWarnings("unused")
			long offsetAnalysisEnd=readOffset(rHeader);
			
			//Here follows user defined segments. Not implemented

			//////////////////
			////// text //////
			//////////////////
			DataInputStream rText=new DataInputStream(new FileInputStream(f));
			rText.skip(offsetTextStart);
			String textSeg=readString(rText,(int)(offsetTextEnd-offsetTextStart));
			
			//The first char in text segment is delimiter
			char delimiter=textSeg.charAt(0);

			//Data to fill in
			String byteOrder=null;
			boolean invertByteOrder=false;
			int numEvents=-1;
			int numParam=-1;
			String dataType=null;
			Long nextData=null;
			
			//System.out.println(textSeg);
			
			//Parse the rest
			StringTokenizer tokText=new StringTokenizer(textSeg, ""+delimiter);
			while(tokText.hasMoreTokens())
				{
				String tok=tokText.nextToken();
				tok=tok.toUpperCase(); //Case of keywords should be ignored
				//System.out.println(tok);
				if(tok.equals("$BEGINANALYSIS")) //$BEGINANALYSIS Byte-offset to the beginning of the ANALYSIS segment.
					{
					offsetAnalysisStart=Long.parseLong(tokText.nextToken());
					}
				else if(tok.equals("$BEGINDATA")) //$BEGINDATA Byte-offset to the beginning of the DATA segment.
					{
					offsetDataStart=parseDataValue(tokText.nextToken());
					}
				else if(tok.equals("$BEGINSTEXT")) //$BEGINSTEXT Byte-offset to the beginning of a supplemental TEXT segment.
					{
					offsetTextStart=Long.parseLong(tokText.nextToken());
					}
				else if(tok.equals("$BYTEORD")) //$BYTEORD Byte order for data acquisition computer.
					{
					byteOrder=tokText.nextToken();
					if(byteOrder.equals("4,3,2,1"))
						invertByteOrder=true;
					else
						invertByteOrder=false; //1,2,3,4 assumed
					}
				else if(tok.equals("$DATATYPE")) //$DATATYPE Type of data in DATA segment (ASCII, integer, floating point).
					{
					dataType=tokText.nextToken();
					}
				else if(tok.equals("$ENDANALYSIS")) //$ENDANALYSIS Byte-offset to the last byte of the ANALYSIS segment.
					{
					offsetAnalysisEnd=Long.parseLong(tokText.nextToken());
					}
				else if(tok.equals("$ENDDATA")) //$ENDDATA Byte-offset to the last byte of the DATA segment.
					{
					offsetDataEnd=parseDataValue(tokText.nextToken());
					}
				else if(tok.equals("$ENDSTEXT")) //$ENDSTEXT Byte-offset to the last byte of a supplemental TEXT segment.
					{
					offsetTextEnd=Long.parseLong(tokText.nextToken());
					}
				else if(tok.equals("$MODE")) //$MODE Data mode (list mode - preferred, histogram - deprecated).
					{
					//Only L supported
					tokText.nextElement();
					}
				else if(tok.equals("$NEXTDATA")) //$NEXTDATA Byte offset to next data set in the file.
					{
					nextData=Long.parseLong(tokText.nextToken());
					if(nextData==0)
						nextData=null; //This is the last one
					}
				else if(tok.equals("$PAR")) //$PAR Number of parameters in an event.
					{
					numParam=Integer.parseInt(tokText.nextToken());
					}
				else if(tok.equals("$TOT")) //$TOT Total number of events in the data set.
					{
					numEvents=Integer.parseInt(tokText.nextToken().trim());
					}
				else if(isPnX(tok,"B")) //$PnB Number of bits reserved for parameter number n.
					{
					String p=tok.substring(2,tok.length()-1);
					int num;
					if(p.equals("*")) //ASCII, variable number
						num=0;
					else
						num=Integer.parseInt(p);
					numBitsForPar.put(num, Integer.parseInt(tokText.nextToken()));
					}
				else if(isPnX(tok,"E")) //$PnE Amplification type for parameter n.
					{
					int num=Integer.parseInt(tok.substring(2,tok.length()-1));
					ampTypeForPar.put(num, tokText.nextToken());
					}
				else if(isPnX(tok,"N")) //$PnN Short name for parameter n.
					{
					int num=Integer.parseInt(tok.substring(2,tok.length()-1));
					shortNameForPar.put(num, tokText.nextToken());
					}
				else if(isPnX(tok,"R")) //$PnR Range for parameter number n.
					{
					int num=Integer.parseInt(tok.substring(2,tok.length()-1));
					rangeForPar.put(num, tokText.nextToken());
					}
				//////////////////////////////////////////////
				////////////////// optional //////////////////
				//////////////////////////////////////////////
				else if(isPnX(tok,"S")) //$PnS Name used for parameter n.
					{
					int num=Integer.parseInt(tok.substring(2,tok.length()-1));
					nameForPar.put(num, tokText.nextToken());
					}
				else if(tok.equals("$OP")) //$OP Name of flow cytometry operator.
					{
					operator=tokText.nextToken();
					}
				else if(tok.equals("$INST")) //$INST Institution at which data was acquired.
					{
					institute=tokText.nextToken();
					}
				else if(tok.equals("$PLATEID")) //$PLATEID Plate identifier.
					{
					plateID=tokText.nextToken();
					}
				else if(tok.equals("$PLATENAME")) //$PLATENAME Plate name.
					{
					plateName=tokText.nextToken();
					}
				else if(tok.equals("$SMNO")) //$SMNO Specimen (e.g., tube) label.
					{
					specimen=tokText.nextToken();
					}
				else if(tok.equals("$WELLID")) //$WELLID Well identifier.
					{
					wellID=tokText.nextToken();
					}
				else if(tok.equals("$SRC")) //$SRC Source of the specimen (patient name, cell types)
					{
					specimenSource=tokText.nextToken();
					}
				else if(tok.equals("$SYS")) //$SYS Type of computer and its operating system.
					{
					computer=tokText.nextToken();
					}
				else if(tok.equals("$CELLS")) //$CELLS Description of objects measured.
					{
					cellsDescription=tokText.nextToken();
					}
				else if(tok.equals("$COM")) //$COM Comment.
					{
					comment=tokText.nextToken();
					}
				else if(tok.equals("$CYT")) //$CYT Type of flow cytometer.
					{
					cytometerType=tokText.nextToken();
					}
				else if(tok.equals("$CYTSN")) //$CYTSN Flow cytometer serial number.
					{
					cytometerSerial=tokText.nextToken();
					}
				else if(tok.equals("$DATE")) //$DATE Date of data set acquisition.
					{
					acquisitionDate=tokText.nextToken();
					}
				else if(tok.equals("$EXP")) //$EXP Name of investigator initiating the experiment.
					{
					experimenter=tokText.nextToken();
					}
				else if(tok.equals("$PROJ")) //$PROJ Name of the experiment project.
					{
					projectName=tokText.nextToken();
					}
				else if(isPnX(tok,"G")) //$PnG Amplifier gain used for acquisition of parameter n.
					{
					int num=Integer.parseInt(tok.substring(2,tok.length()-1));
					amplifierGain.put(num, tokText.nextToken());
					}
				else if(isPnX(tok,"V")) //$PnV Detector voltage for parameter n.
					{
					int num=Integer.parseInt(tok.substring(2,tok.length()-1));
					detectorVoltage.put(num, tokText.nextToken());
					}
				else if(tok.equals("$BTIM")) //$BTIM Clock time at beginning of data acquisition.
					{
					beginTime=tokText.nextToken();
					}
				else if(tok.equals("$ETIM")) //$ETIM Clock time at beginning of data acquisition.
					{
					endTime=tokText.nextToken();
					}
				else if(tok.equals("$TIMESTEP")) //$TIMESTEP Time step for time parameter.
					{
					timestep=tokText.nextToken();
					}
				else if(tok.equals("$FIL")) //$FIL Name of the data file containing the data set.
					{
					originalFile=tokText.nextToken();
					}
				else
					{
					System.out.println("Unknown entry: "+tok);
					String val=tokText.nextToken();
					otherKeywords.put(tok, val);
					}
				
				
				
				
				/**
	The optional FCS TEXT segment keywords are as follows:
	$ABRT Events lost due to data acquisition electronic coincidence.
	
	$CSMODE Cell subset mode, number of subsets to which an object may belong.
	$CSVBITS Number of bits used to encode a cell subset identifier.
	$CSVnFLAG The bit set as a flag for subset n.
	$ETIM Clock time at end of data acquisition.
	
	$GATE Number of gating parameters.
	$GATING Specifies region combinations used for gating.
	$LAST_MODIFIED Timestamp of the last modification of the data set.
	$LAST_MODIFIER Name of the person performing last modification of a data set.
	$LOST Number of events lost due to computer busy.
	$ORIGINALITY Information whether the FCS data set has been modified (any part of it)
	            or is original as acquired by the instrument.
	$PnCALIBRATION Conversion of parameter values to any well defined units, e.g., MESF.
	$PnD Suggested visualization scale for parameter n.
	$PnF Name of optical filter for parameter n.
	
	$PnL Excitation wavelength(s) for parameter n.
	$PnO Excitation power for parameter n.
	$PnP Percent of emitted light collected by parameter n.
	$PnT Detector type for parameter n.
	
	$RnI Gating region for parameter number n.
	$RnW Window settings for gating region n.
	$SPILLOVER Fluorescence spillover matrix.
	
	$TR Trigger parameter and its threshold.
	$VOL Volume of sample run during data acquisition.


	/////////// Will not be implemented ////////////////

	$GnE Amplification type for gating parameter number n (deprecated).
	$GnF Optical filter used for gating parameter number n (deprecated).
	$GnN Name of gating parameter number n (deprecated).
	$GnP Percent of emitted light collected by gating parameter n (deprecated).
	$GnR Range of gating parameter n (deprecated).
	$GnS Name used for gating parameter n (deprecated).
	$GnT Detector type for gating parameter n (deprecated).
	$GnV Detector voltage for gating parameter n (deprecated).
	$PKn Peak channel number of univariate histogram for parameter n
	    (deprecated).
	$PKNn Count in peak channel of univariate histogram for parameter n
	     (deprecated).
				 */
				
				}
			
			
			System.out.println("data type "+dataType);
			System.out.println("numbit "+numBitsForPar);
			System.out.println("amp "+ampTypeForPar);
			System.out.println("short name "+shortNameForPar);
			System.out.println("range "+rangeForPar);
			System.out.println("name "+nameForPar);
			System.out.println("byteorder "+byteOrder);
			
			
			
			//////////////////
			////// data //////
			//////////////////
			
			FileInputStream fiData=new FileInputStream(f);
			fiData.skip(offsetDataStart);
			
			if(dataType.equals("I"))
				{
				//BitInputStream rData=new BitInputStream(fiData);
				DataInputStream rData=new DataInputStream(fiData); //TODO replace with another reader
				
				int[] numbits=new int[numBitsForPar.size()];
				for(int j=0;j<numParam;j++)
					numbits[j]=numBitsForPar.get(j+1);

				eventsInt=new ArrayList<int[]>(numEvents);

				for(int i=0;i<numEvents;i++)
					{
					System.out.println("event");
					int[] event=new int[numParam];
					eventsInt.add(event);
					
					for(int j=0;j<numParam;j++)
						{
						
						//TODO support other number of bits e.g. 9 bits
						//TODO take byte order into account
						/*
						int v=rData.readBits(numbits[j]);
						
						System.out.println(v);*/
						
						
						//this works with byte order 1,2,3,4. others might be needed!
						int v=0;
						if(numbits[j]==32)
							v=rData.readInt();
						else if(numbits[j]==16)
//							readUnsignedShort(rData, invertByteOrder);
							v=rData.readUnsignedShort();
						else if(numbits[j]==8)
							v=rData.readUnsignedByte();
						else
							System.out.println("Unknown #bits: "+numbits[j]);
						
						System.out.println("invertbyteorder: "+invertByteOrder);
						//todo, Some bits should be ignored according to range. nitpicking?
						
						event[j]=v;
						}
					
					}
				rData.close();
				}
			else if(dataType.equals("D"))
				{
				DataInputStream rData=new DataInputStream(fiData);
				eventsFloat=new ArrayList<double[]>(numEvents);
				for(int i=0;i<numEvents;i++)
					{
					double[] event=new double[numParam];
					eventsFloat.add(event);
					for(int j=0;j<numParam;j++)
						event[j]=rData.readDouble(); //untested
					}
				rData.close();
				}
			else if(dataType.equals("F"))
				{
				DataInputStream rData=new DataInputStream(fiData);
				eventsFloat=new ArrayList<double[]>(numEvents);
				for(int i=0;i<numEvents;i++)
					{
					double[] event=new double[numParam];
					eventsFloat.add(event);
					for(int j=0;j<numParam;j++)
						event[j]=rData.readFloat(); //untested
					}
				rData.close();
				}
			else if(dataType.equals("A"))
				{
				DataInputStream rData=new DataInputStream(fiData);
				int[] numbits=new int[numBitsForPar.size()];
				for(int j=0;j<numParam;j++)
					numbits[j]=numBitsForPar.get(j+1);
				
				eventsInt=new ArrayList<int[]>(numEvents);

				for(int i=0;i<numEvents;i++)
					{
					int[] event=new int[numParam];
					eventsInt.add(event);
					
					for(int j=0;j<numParam;j++)
						{
						//Fixed number of ASCII chars assumed
						int v=readA(rData, numbits[j]); //untested
						event[j]=v;
						}
					}
				}
			
			return nextData;
			}

		public int getNumObservations()
			{
			return eventsFloat.size();
			}
		}
	
	/*
	private static int readInt(DataInputStream r, boolean invert) throws IOException
		{
		int val=r.readInt();
		if(invert)
			{
			//TODO
			}
		return val;
		}
	
	private static int readUnsignedShort(DataInputStream r, boolean invert) throws IOException
		{
		int val=r.readUnsignedShort();
		if(invert)
			{
			//TODO
			}
		return val;
		}*/
	
	
	
	public FCSFile(File f) throws IOException
		{
		Long nextData=(long)0;
		while(nextData!=null)
			{
			DataSegment dataSegment=new DataSegment();
			dataSegment.source=f;
			data.add(dataSegment);
			nextData=dataSegment.parse(f, 0);
			}
		}

	
	
	public FCSFile()
		{
		}

	public static boolean isFCSfile(File f) throws FileNotFoundException
		{
		try
			{
			//FileInputStream fi=new FileInputStream(f);
			DataInputStream rHeader=new DataInputStream(new FileInputStream(f));
			String fcsversion=readString(rHeader, 10);
			return fcsversion.startsWith("FCS");
			}
		catch (FileNotFoundException e)
			{
			throw e;
			}
		catch (IOException e)
			{
			return false;
			}
		}
	
	
	
	
	
	}
