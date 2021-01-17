package jassjr;/*
  JASSjr_search.java
  -----------------
  Copyright (c) 2019 Andrew Trotman and Kat Lilly
  Minimalistic BM25 search engine.
*/
import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.nio.IntBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

import opennlp.tools.stemmer.PorterStemmer;

class JASSjr_search
	{
	/*
	  Constants
	  ---------
	*/
	final double k1 = 0.9;      // BM25 k1 parameter
	final double b = 0.4;        // BM25 b parameter

	/*
	  Class VocabEntry
	  ----------------
	*/
	class VocabEntry
		{
		int where, size;        // where on the disk and how large (in bytes) is the postings list?

		VocabEntry(int where, int size)
			{
			this.where = where;
			this.size = size;
			}
		}

	/*
	  readEntireFile()
	  ----------------
	  Read the entire contents of the given file into memory and return as ByteBuffer.
	*/
	ByteBuffer readEntireFile(String filename) throws Exception
		{
		File file = new File(filename);
		FileInputStream stream = new FileInputStream(file);
		ByteBuffer bytes = ByteBuffer.allocate((int)file.length());
		if (stream.read(bytes.array()) == -1)
			return null;
		else
			return bytes;
		}

	/*
	  CompareRsv()
	  ------------
	  Callback from sort for two rsv values.  Tie break on the document ID.
	*/
    
	class CompareRsv implements Comparator<Integer> 
		{
		final double[] rsv;
        
		CompareRsv(double[] rsv)
			{
			this.rsv = rsv; 
			}
        
		public int compare(Integer a, Integer b) 
			{
			return rsv[a] < rsv[b] ? 1 : rsv[a] == rsv[b] ? a < b ? 1 : a == b ? 0 : -1 : -1;
			} 
		}

	/*
	  engage()
	  --------
	  Simple search engine ranking on BM25.
	*/
	public void engage(String args[]) throws Exception
		{
		/*
		  Read the document lengths
		*/
		ByteBuffer lengthsAsBytes = readEntireFile("lengths.bin");
		if (lengthsAsBytes == null)
			{
			System.out.println("Could not find an index in the current directory");
			System.exit(1);
			}
		lengthsAsBytes.order(ByteOrder.nativeOrder());
		IntBuffer lengthsAsIntegers = lengthsAsBytes.asIntBuffer();
		int[] lengthVector = new int [lengthsAsBytes.capacity() / 4];
		lengthsAsIntegers.get(lengthVector);


		/*
		  Compute the average document length for BM25
		*/
		double documentsInCollection = lengthVector.length;
		double averageDocumentLength = 0;
		for (int which : lengthVector)
			averageDocumentLength += which;
		averageDocumentLength /= documentsInCollection;

		/*
		  Read the primary keys
		*/
		List<String> primaryKey = Files.readAllLines(Paths.get("docids.bin"));

		/*
		  Open the postings list file
		*/
		RandomAccessFile postingsFile = new RandomAccessFile("postings.bin", "r");
        
		/*
		  Build the vocabulary in memory
		*/
		HashMap<String, VocabEntry>dictionary = new HashMap<String, VocabEntry>(); // the vocab
		ByteBuffer vocabAsBytes = readEntireFile("vocab.bin");
		vocabAsBytes.order(ByteOrder.nativeOrder());

		while (vocabAsBytes.position() < vocabAsBytes.capacity())
			{
				System.out.println("inside while ");
			byte stringLength = vocabAsBytes.get();
			byte[] termAsBytes = new byte[stringLength];
			vocabAsBytes.get(termAsBytes);
			String term = new String(termAsBytes);
			byte zero = vocabAsBytes.get();      // read the '\0' string terminator

			int where = vocabAsBytes.getInt();
			int size = vocabAsBytes.getInt();

			dictionary.put(term, new VocabEntry(where, size));
				System.out.println(term);
			}
        
		/*
		  Allocate buffers
		*/
		int maxDocs = (int)documentsInCollection;
		double[] rsv = new double [maxDocs];          // array of rsv values

		/*
		  Set up the rsv pointers
		*/
		Integer[] rsvPointers = new Integer [maxDocs];    // pointers to each member of rsv[] so that we can sort
		for (int index = 0; index < rsvPointers.length; index++)
			rsvPointers[index] = index;

		/*
		  Search (one query per line)
		*/
		Scanner stdin = new Scanner(System.in);
		while (stdin.hasNextLine())
			{
			/*
			  Zero the accumulator array.
			*/
			Arrays.fill(rsv, 0);
			boolean firstTerm = true;
			long queryId = 0;
			StringTokenizer tokenizer = new StringTokenizer(stdin.nextLine());
			while (tokenizer.hasMoreTokens())
				{
				String token = tokenizer.nextToken();
					System.out.println("Previous word : " + token);
				token = stemmWords(token);
					System.out.println("After stemming  : " + token);
				/*
				  If the first token is a number then assume a TREC query number, and skip it
				*/
				if (firstTerm && Character.isDigit(token.charAt(0)))
					{
					queryId = Long.parseLong(token);
					firstTerm = false;
					continue;
					}
				firstTerm = false;

				/*
				  Does the term exist in the collection?
				*/
				VocabEntry termDetails;
				if ((termDetails = dictionary.get(token)) != null)
					{
					/*
					  Seek and read the postings list
					*/
					byte [] currentList = new byte[termDetails.size];
					postingsFile.seek(termDetails.where);
					postingsFile.read(currentList);
					ByteBuffer currentListAsBytes = ByteBuffer.wrap(currentList);
					currentListAsBytes.order(ByteOrder.nativeOrder());
					int postings = currentListAsBytes.capacity() / 8;

					/*
					  Compute the IDF component of BM25 as log(N/n).
					  if IDF == 0 then don't process this postings list as the BM25 contribution of this term will be zero.
					*/
					if (documentsInCollection != postings)
						{
						double idf = Math.log(documentsInCollection / postings);

						/*
						  Process the postings list by simply adding the BM25 component for this document into the accumulators array
						*/
						while (currentListAsBytes.position() < currentListAsBytes.capacity())
							{
							int d = currentListAsBytes.getInt();
							int tf = currentListAsBytes.getInt();
							rsv[d] += idf * ((tf * (k1 + 1)) / (tf + k1 * (1 - b + b * (lengthVector[d] / averageDocumentLength))));
							}
						}
					}
				}
            
			/*
			  Sort the results list
			*/
			Arrays.sort(rsvPointers, new CompareRsv(rsv));
            
			/*
			  Print the (at most) top 1000 documents in the results list in TREC eval format which is:
			  query-id Q0 document-id rank score run-name
			*/
			for (int position = 0; rsv[rsvPointers[position]] != 0.0 && position < 1000; position++)
				System.out.println(queryId + " Q0 " + primaryKey.get(rsvPointers[position]) + " " + (position + 1) + " " + String.format("%.4f", rsv[rsvPointers[position]]) + " JASSjr");
			}
		}

		public static String stemmWords(String inputString){
			PorterStemmer stemmer = new PorterStemmer();
			String stemWord = stemmer.stem(inputString);

			System.out.println("Stem word for " + inputString +" : " + stemWord);
			return stemWord;
		}

		public static String stemmWordsWithRules(String inputString){
			PorterStemmerWithRules stemmer = new PorterStemmerWithRules();
			String stemWord = stemmer.stemming(inputString);

			System.out.println("Stem word for " + inputString +" : " + stemWord);
			return stemWord;
		}

    
	/*
	  Main()
	  ------
	*/
	public static void main(String args[])
		{
		try
			{          
			JASSjr_search engine = new JASSjr_search();
			engine.engage(args);
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}
	}
