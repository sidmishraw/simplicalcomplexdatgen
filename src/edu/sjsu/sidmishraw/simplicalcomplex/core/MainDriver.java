/**
 * Project: SimplicalComplexDataCreator
 * Package: edu.sjsu.sidmishraw.simplicalcomplex.core
 * File: MainDriver.java
 * 
 * @author sidmishraw
 *         Last modified: Apr 24, 2017 4:14:51 PM
 */
package edu.sjsu.sidmishraw.simplicalcomplex.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.gson.Gson;

/**
 * @author sidmishraw
 *
 *         Qualified Name: edu.sjsu.sidmishraw.simplicalcomplex.core.MainDriver
 *
 */
public class MainDriver {
	
	// counters to be used inside lambdas
	private static int									wordIndex		= 0;
	private static int									docIndex		= 0;
	
	/**
	 * Temporary store for words before getting stored into the dict with proper
	 * indices
	 */
	private static List<String>					wordsBuffer	= new ArrayList<>();
	
	private static Map<String, Integer>	revWordMap	= new HashMap<>();
	
	/**
	 * wordMap contains the mapping from the number to word
	 */
	private static Map<Integer, String>	wordMap			= new HashMap<>();
	
	/**
	 * docMap is the mapping from doc to index(number)
	 */
	private static Map<Integer, String>	docMap			= new HashMap<>();
	
	/**
	 * Deciding on the data structure to hold the values:
	 * 
	 * Input is a json document: lets call it JSON
	 * 
	 * JSON has a mapping from "document.json":[list of words in the document]
	 * Steps are:
	 * 1> Read in JSON
	 * 2> extract the words from it
	 * 3> build a lookup for the words
	 * 3.1> build a set of sorted words and then put the words from the set into
	 * a map int -> word
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		String filePath = "";
		String wordMapFilePath = "";
		String docMapFilePath = "";
		String simplicalComplexIPPath = "";
		
		if (args.length < 4) {
			
			filePath = Constants.DEFAULT_FILE_NAME;
			wordMapFilePath = Constants.DEFAULT_WORD_MAP_PATH;
			docMapFilePath = Constants.DEFAULT_DOC_MAP_PATH;
			simplicalComplexIPPath = Constants.DEFAULT_SIMPLICAL_COMPLEX_IP_PATH;
		} else {
			
			filePath = args[0];
			wordMapFilePath = args[1];
			docMapFilePath = args[2];
			simplicalComplexIPPath = args[3];
		}
		
		File file = new File(filePath);
		
		if (!file.isDirectory()) {
			
			System.out.println("Please enter the path to the folder containing the json files");
			
			return;
		}
		
		List<File> jsonFiles = Arrays.asList(file.listFiles());
		
		StringBuffer sBuffer = new StringBuffer();
		
		// ensure the docIndex is 0 before the operation
		docIndex = 0;
		
		jsonFiles.stream().forEach(jsonFile -> {
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile)))) {
				
				sBuffer.setLength(0);
				
				String readString = null;
				
				while (null != (readString = br.readLine())) {
					
					sBuffer.append(readString);
				}
				
				cleanseWords(sBuffer.toString(), jsonFile.getName()).stream().forEach(word -> {
					
					if (!wordsBuffer.contains(word)) {
						
						wordsBuffer.add(word);
					}
				});
				
				System.out.println("Finished file " + jsonFile.getName());
				
				// add the file to the docMap
				docMap.put(docIndex, jsonFile.getName());
				
				docIndex++;
			} catch (IOException e) {
				
				e.printStackTrace();
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		});
		
		// reset the docIndex
		docIndex = 0;
		
		// sort the wordsBuffer before building the wordMap
		Collections.sort(wordsBuffer);
		
		// ensure word index is 0 before the operation
		wordIndex = 0;
		
		wordsBuffer.stream().forEach(word -> {
			
			wordMap.put(wordIndex, word);
			
			// makes the lookup for word faster
			revWordMap.put(word, wordIndex);
			
			wordIndex++;
		});
		
		// reset the word index after the operation
		wordIndex = 0;
		
		// wordMap.entrySet().stream().forEach(entry -> {
		//
		// System.out.println("Index = " + entry.getKey() + " value = " +
		// entry.getValue());
		// });
		//
		// docMap.entrySet().stream().forEach(entry -> {
		//
		// System.out.println("Index = " + entry.getKey() + " value = docname =
		// " + entry.getValue());
		// });
		
		// persist the mapping files to disk for offline storage
		writeToFile(wordMapFilePath, wordMap);
		writeToFile(docMapFilePath, docMap);
		
		// dat file creation logic
		try (BufferedWriter bufferWriter = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(new File(simplicalComplexIPPath + File.separator + "s_output.dat"))))) {
			
			// TODO Create simplical complex file
			jsonFiles.stream().forEach(jsonFile -> {
				
				try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile)))) {
					
					// clean the buffer
					sBuffer.setLength(0);
					
					String readString = null;
					
					while (null != (readString = br.readLine())) {
						
						sBuffer.append(readString);
					}
					
					// storing the wordstream because I need to reuse the string
					// buffer to build the op file
					// creating a supplier of streams for reusing it
					Set<String> cleansedWords = cleanseWords(sBuffer.toString(), jsonFile.getName());
					
					// creating a resusable stream
					Supplier<Stream<String>> cleansedWordStream = () -> cleansedWords.stream();
					
					int rowSize = (int) cleansedWordStream.get().count();
					
					// reset the string buffer
					sBuffer.setLength(0);
					
					sBuffer.append(rowSize).append(" ");
					
					cleansedWordStream.get().forEach(word -> {
						
						sBuffer.append(wordsBuffer.indexOf(word)).append(" ");
					});
					
					bufferWriter.write(sBuffer.toString().trim() + "\n");
					
					System.out.println("Wrote file " + jsonFile.getName() + " to dat file");
				} catch (Exception e) {
					
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			
		}
	}
	
	private static <T> void writeToFile(String fileName, T contents) {
		
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(fileName))))) {
			
			Gson gson = new Gson();
			String jsonString = gson.toJson(contents);
			bw.write(jsonString);
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads the JSON contents and converts it into a list of strings for easier
	 * ingestion
	 * 
	 * @param str
	 * @param fileName
	 * @return {@link List}<String>
	 */
	public static Set<String> cleanseWords(String str, String fileName) {
		
		Gson gson = new Gson();
		
		@SuppressWarnings("unchecked")
		Map<String, List<String>> mymap = (Map<String, List<String>>) gson.fromJson(str, Map.class);
		
		Set<String> words = new LinkedHashSet<>(mymap.get(fileName));
		
		return words;
	}
}
