/*
Alioune Ndiaye 260552133 
Johannes Breitschwerdt 260555511
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DnsClientx { 
	static int timeout=5;
	static int max_r=3;
	static int max_r_left=max_r;
	static int port=53;
	static String search_type="A";
	static String Ip_server;
	static String domain_name;
	static byte[] Ip_dest;
	static InetAddress Ip_address;
	static long starting_t=0;
	static long ending_t=0;
	static long timing=0;
	static int r_left;
	
	static byte[] data_to_receive;
	static int type;
	static int ARCount;
	static int ANCount;
	static int timetolive;
	static boolean Auth;
	static int QClass;
	static int RDLength;
	
	public static void main(String args[]) throws Exception {
		BufferedReader user_input = new BufferedReader(new InputStreamReader(System.in));
		DatagramSocket clientSocket = new DatagramSocket();
		
		//Query the User
		System.out.println("\"Please write Query as follows: java DnsClient [-t timeout] [-r max-retries] [-p port] [-mx|-ns] @server name\".");
		String sentence = user_input.readLine();
		
		//For quicker testing purpose 
		//String sentence = "java DnsClient @132.206.85.18 www.mcgill.ca";
		
		//parse input into array "input"
		String[] input = sentence.split(" ");
		
		if(!(input[0].equals("java")) || !(input[1].equals("DnsClient"))){
			System.out.println("ERROR " + '\t' + "Incorrect input syntax: first two inputs must be [java DnsClient]!");
			System.exit(1);
		}
		//assign query inputs to variables
		else{
			for(int i =0; i<input.length;i++){
				if(input[i].equals("-t")){
						timeout=Integer.parseInt(input[i+1]);	
					}
				else if (input[i].equals("-r")){
						max_r=Integer.parseInt(input[i+1]);
						max_r_left=max_r;
					}
				else if (input[i].equals("-p")){
						port=Integer.parseInt(input[i+1]);
					}
				else if ((input[i].equals("-mx")) || input[i].equals("-ns")){
						search_type=(input[i]);
					}
				Ip_server= input[input.length-2];
				domain_name= input[input.length-1];				
			}
		}
		
		//create buffers for data
		byte[] data_to_send= new byte[1024];
		byte[] data_to_receive= new byte [1024];
		
		//Divide domain by '.'
		String[] domain = domain_name.split("\\.");
		
	
		//create the packet
		data_to_send= packet_to_create(search_type,domain);
		
		//Divide IP by '.'
		String[] IP = Ip_server.split("@|\\.");
		byte[] Ip_dest = new byte[IP.length-1];
		for (int i=1; i < IP.length; i++) {
			Ip_dest[i-1] = (byte)Integer.parseInt(IP[i]);
		}
		
		try {
			Ip_address = InetAddress.getByAddress(Ip_dest);
			//System.out.println(Ip_address);
		} catch (UnknownHostException e) {
			System.out.println("ERROR: Inputted IP address does not exist!");
			System.exit(1); 
		}
				
		//Send Data over UDP 
		DatagramPacket packet_to_send = new DatagramPacket(data_to_send, data_to_send.length, Ip_address, port);
		byte[] packet_sent = new byte[packet_to_send.getLength()];
		System.arraycopy(packet_to_send.getData(), packet_to_send.getOffset(), packet_sent, 0, packet_to_send.getLength());
		printbytesArrayHex(packet_sent, packet_sent.length);	
		
		//Wait for query and create receive socket
		DatagramPacket packet_to_receive = new DatagramPacket(data_to_receive, data_to_receive.length);
		
		clientSocket.setSoTimeout(timeout*1000);

		//enable timeout and retry options
		boolean done = false;
		while(done==false) {
			try {
					clientSocket.send(packet_to_send);
					starting_t = System.currentTimeMillis();
					clientSocket.receive(packet_to_receive);
					ending_t = System.currentTimeMillis();
					done = true;
				
			} catch(SocketTimeoutException s) {
				System.out.println("Socket timed out, retries left: " + max_r_left);
				max_r_left = max_r_left-1;
			}
			if (max_r_left<0) {
				System.out.println("ERROR " + '\t' + "Maximum number of retries " + max_r + " exceeded!");
				System.exit(1);
			}	
		}
		
		//set up timing in ms & #retries left
		timing=(ending_t-starting_t);
		double time_ms = (double) timing/1000;
		r_left = max_r - max_r_left;
		
		byte[] reply = new byte[packet_to_receive.getLength()];
		System.arraycopy(packet_to_receive.getData(), packet_to_receive.getOffset(), reply, 0, packet_to_receive.getLength());
		printbytesArrayHex(reply, reply.length);
		System.out.println("DnsClient sending request for " + domain_name);
		unpackResponse(reply);
		if(type == 1)	{	
			System.out.println("Request type: A");
			}
		else if(type == 6) {
			System.out.println("Request type: NS");
		}
		else if(type == 3) {
			System.out.println("	Requet type: MX");
		}
		else if(type == 5) {
			System.out.println("Request Type: CNAME");
		}
		
		int number_of_retries_used = -(max_r_left-max_r);
		System.out.println("Response received after " + time_ms + " seconds (" + number_of_retries_used + " retries)" );
		if (ANCount > 0) {
			System.out.println("***Answer Section (" + ANCount + " records)***");
		}
		if (type == 1 ) {
			System.out.print("IP " + "\t");
			for(int i=reply.length-4; i<reply.length; i++){
				String binary = byteToBinaryString(reply[i]);
				int IPAddress = binaryStringToInt(binary);
				
				System.out.print(IPAddress + ".");
			}
			System.out.print("\t" + "\t" + timetolive);
			if (Auth == false) {
				System.out.print("\t" + "\t" + "Non-Auth");
			}
			else {
				System.out.print("\t" + "\t" + "Auth");
			}
		}
		if (type == 6) {
			System.out.print("NS " + "\t");
			System.out.print("\t" + "\t" + timetolive);
			if (Auth == false) {
			System.out.print("\t" + "\t" + "Non-Auth");
			}
			else {
			System.out.print("\t" + "\t" + "Auth");
			}
		}
		System.out.println();
		clientSocket.close();	
	}
	
	private static byte[] packet_to_create(String search_type,String[] domain){
		
		ByteBuffer bytebuff = ByteBuffer.allocate(1024);
		//Set HEADER
		//set Header ID
		//Assigning a 16 bit random ID
		Random random = new Random();
		bytebuff.put((byte)random.nextInt(255));
		bytebuff.put((byte)random.nextInt(255));
		
		//set QR field = 0, OPCODE = 0000, AA = 0, TC = 0, RD = 1
		bytebuff.put((byte)1);
		 
		//set RA = 0, Z = 000, RCODE = 0000
		bytebuff.put((byte)0);
		
		//set QDCOUNT = 0000 0000 0000 0001
		bytebuff.put((byte)0);
		bytebuff.put((byte)1);
	
		//set ANCount = 0000 0000 0000 0000
		bytebuff.put((byte)0);//0000 0000
		bytebuff.put((byte)0);//0000 0001
		
		//set NSCOUNT = 0000 0000 0000 0000
		bytebuff.put((byte)0);
		bytebuff.put((byte)0);
		
		//set ARCOUNT = 0000 0000 0000 0000
		bytebuff.put((byte)0);
		bytebuff.put((byte)0);
		
		//set DNS QUESTION
		//set QNAME
		for(int i = 0; i<domain.length; i++){
			bytebuff.put((byte)domain[i].length());
			try{
				bytebuff.put(domain[i].getBytes("US-ASCII"));	
			}
			catch(Exception e){
				System.out.println("ERROR :Cannot convert domain name to ASCII!");
			}
		}
		bytebuff.put((byte)0); // terminating zero-length Octet
		
		//QTYPE A: 0000 0000 0000 0001; -ns: (0000 0000 0000 0010) ; -mx (0000 0000 0000 0011)
		
		//set QTYPE: 0x0001 for A, 0x0002 for NS, 0x000f for MX
		if (search_type.equals("A")){
			bytebuff.put((byte)0);
			bytebuff.put((byte)1);	
		}
		else if (search_type.equals("-ns")){
			bytebuff.put((byte)0);
			bytebuff.put((byte)2);
		}
		else if (search_type.equals("-mx")){
			bytebuff.put((byte)0);
			bytebuff.put((byte)15);
		}
		else {
			System.out.println("An error occurred while attempting to convert the SearchType to byte");
			System.exit(1);
		}	
		
		//set QCLASS: set to 0000 0000 0000 00001
		bytebuff.put((byte)0);
		bytebuff.put((byte)1);
		
		byte[] data_to_send = Arrays.copyOf( bytebuff.array(), bytebuff.position() );

		return data_to_send;
	}
	
	//the received response is turned into a hex array and decoded element by element
	//The elements of each respective section are decoded by their respectively named methods called here
	public static void unpackResponse(byte[] response) {
		int element_count;
		element_count = decodeID	(response);			
		element_count = decodeResponseFlags(response); //RCODE errors handled here
		element_count = decodeResponseQDCOUNT(response, element_count);	
		element_count = decodeResponseANCOUNT(response, element_count);	
		element_count = decodeResponseNSCOUNT(response, element_count);	
		element_count = decodeResponseARCOUNT(response, element_count);
		element_count = decodeResposeQNAME(response, element_count);
		element_count = decodeResponseQTYPE(response, element_count);
		element_count = decodeResponseQCLASS(response, element_count);
		element_count = decodeResponseNAME(response, element_count);
		element_count = decodeResponseType(response, element_count);
		element_count = decodeResponseClassType(response, element_count);
		element_count = decodeResponseTTL(response, element_count);
		element_count = decodeResponseRDLENGTH(response, element_count);
		element_count = decodeResponseRDATA(response, element_count);
	}
	
	private static int decodeID(byte[] response) {
		//System.out.print("ID	= ");
		//printbytesArrayHex(response, 2);
		int element_position_counter = 2;
		return element_position_counter;
	}
	
	private static int decodeResponseFlags(byte[] response) {
		//we split the Flag response into two sections, each to be interpreted individually
		char[] upperFlags = bytesToBinaryString(response[2]).toCharArray();
		char[] lowerFlags = bytesToBinaryString(response[3]).toCharArray();
		
		//QR field
		//System.out.print("QR: " + upperFlags[0]);
		
		//OPCODE
		//System.out.print("OPCODE: " + upperFlags[1] + upperFlags[2] + upperFlags[3] + upperFlags[4]);
		
		//AA
		if (upperFlags[5] == '0') {
			Auth = false;
		}
		else if (upperFlags[5] == '1') {
			Auth = true;
		}
		//System.out.print("AA: " + upperFlags[5]);
		
		//TC
		//System.out.print("TC: " + upperFlags[6]);
		
		//RD 
		//System.out.print("RD	: " + upperFlags[7]);
		
		//RA
		//System.out.print("RA: " + lowerFlags[0]);
		
		//Z
		//System.out.print("Z: " + lowerFlags[1] + lowerFlags[2] + lowerFlags[3]);

		//RCODE check
		String rCodeString = "";
		rCodeString += lowerFlags[4];
		rCodeString += lowerFlags[5];
		rCodeString += lowerFlags[6];
		rCodeString += lowerFlags[7];
		int rCodeInt = binaryStringToInt(rCodeString);
		if (rCodeInt != 0) {
			if (rCodeInt == 1) {
				System.out.println("Format error: The name server was unable to interpret the query");
			}
			else if (rCodeInt == 2){
				System.out.println("Server failure: The name server was unable to process this query due to a problem with the name server");
			}
			else if (rCodeInt == 3){
				System.out.println("NOTFOUND: Domain name referenced in the query does not exist");
			}
			else if (rCodeInt == 4){
				System.out.println("Not implemented: The name server does not support the requested kind of query");
			}
			else if (rCodeInt == 5){
				System.out.println("Refused: The name server refuses to perform the requested operation for policy reasons");
			}
			System.exit(1);
		}
		int element_position_counter = 4;
		return element_position_counter;
	}
	
	//QDCOUNT gives number of questions in response response
	//in this method, we split the 16-bit field into separate 2 bytes 
	private static int decodeResponseQDCOUNT(byte[] response, int element_count) {
		char[] qdCountField = bytesToBinaryString(response[element_count]).toCharArray();
		int QDCount;
		String qdCountString = "";
		
		qdCountString += qdCountField[0];
		qdCountString += qdCountField[1];
		qdCountString += qdCountField[2];
		qdCountString += qdCountField[3];
		qdCountString += qdCountField[4];
		qdCountString += qdCountField[5];
		qdCountString += qdCountField[6];
		qdCountString += qdCountField[7];
		
		qdCountField = bytesToBinaryString(response[element_count+1]).toCharArray();
		
		qdCountString += qdCountField[0];
		qdCountString += qdCountField[1];
		qdCountString += qdCountField[2];
		qdCountString += qdCountField[3];
		qdCountString += qdCountField[4];
		qdCountString += qdCountField[5];
		qdCountString += qdCountField[6];
		qdCountString += qdCountField[7];
		//System.out.println(qdCountString);
		
		QDCount = binaryStringToInt(qdCountString);
		//System.out.println("QDCount	= " + QDCount + "	(This is the number of questions)");
		int element_position_counter = element_count + 2;
		return element_position_counter;
	}
	
	//ANCOUNT gives number of answers in response response
	//in this method, we split the 16-bit field into separate 2 bytes
	private static int decodeResponseANCOUNT(byte[] response, int element_count) {
		char[] anCountField = bytesToBinaryString(response[element_count]).toCharArray();
		String anCountString = "";
		
		anCountString += anCountField[0];
		anCountString += anCountField[1];
		anCountString += anCountField[2];
		anCountString += anCountField[3];
		anCountString += anCountField[4];
		anCountString += anCountField[5];
		anCountString += anCountField[6];
		anCountString += anCountField[7];
		
		anCountField = bytesToBinaryString(response[element_count+1]).toCharArray();
		
		anCountString += anCountField[0];
		anCountString += anCountField[1];
		anCountString += anCountField[2];
		anCountString += anCountField[3];
		anCountString += anCountField[4];
		anCountString += anCountField[5];
		anCountString += anCountField[6];
		anCountString += anCountField[7];
	
		
		ANCount = binaryStringToInt(anCountString);		
		int element_position_counter = element_count + 2;
		return element_position_counter;
	}
	
	//NSCOUNT gives number of authoritative records in response response
	//in this method, we split the 16-bit field into separate 2 bytes
	public static int decodeResponseNSCOUNT(byte[] response, int element_count) {
		char[] nsCountField = bytesToBinaryString(response[element_count]).toCharArray();
		int NSCount;
		String nsCountString = "";
		
		nsCountString += nsCountField[0];
		nsCountString += nsCountField[1];
		nsCountString += nsCountField[2];
		nsCountString += nsCountField[3];
		nsCountString += nsCountField[4];
		nsCountString += nsCountField[5];
		nsCountString += nsCountField[6];
		nsCountString += nsCountField[7];
		
		nsCountField = bytesToBinaryString(response[element_count+1]).toCharArray();
		
		nsCountString += nsCountField[0];
		nsCountString += nsCountField[1];
		nsCountString += nsCountField[2];
		nsCountString += nsCountField[3];
		nsCountString += nsCountField[4];
		nsCountString += nsCountField[5];
		nsCountString += nsCountField[6];
		nsCountString += nsCountField[7];
	
		
		NSCount = binaryStringToInt(nsCountString);
//		System.out.println("NSCOUNT: " + NSCount));
		int element_position_counter = element_count + 2;
		return element_position_counter;
	}
	
	//ARCOUNT gives the number of records in the Additional section of the response
	//in this method, we split the 16-bit field into separate 2 bytes
	public static int decodeResponseARCOUNT(byte[] response, int element_count) {
		char[] arCountField = bytesToBinaryString(response[element_count]).toCharArray();
		String arCountString = "";
		
		arCountString += arCountField[0];
		arCountString += arCountField[1];
		arCountString += arCountField[2];
		arCountString += arCountField[3];
		arCountString += arCountField[4];
		arCountString += arCountField[5];
		arCountString += arCountField[6];
		arCountString += arCountField[7];
		
		arCountField = bytesToBinaryString(response[element_count+1]).toCharArray();
		
		arCountString += arCountField[0];
		arCountString += arCountField[1];
		arCountString += arCountField[2];
		arCountString += arCountField[3];
		arCountString += arCountField[4];
		arCountString += arCountField[5];
		arCountString += arCountField[6];
		arCountString += arCountField[7];
	
		ARCount = binaryStringToInt(arCountString);
		//System.out.println("ARCOUNT: " + ARCount);
		int element_position_counter = element_count + 2;
		return element_position_counter;
	}
	
	//In the method, the queried domain name is reconstructed from the response
	//and the length of the name is added to the response array element counter
	public static int decodeResposeQNAME(byte[] response, int element_count) {
		int nameCounter = 0;
		int domainNameLength = 0;
		String responseDomainName = "";
		while (response[element_count + nameCounter] != 0){	//A terminating octet will denote the end of the domain name
			domainNameLength = response[element_count + nameCounter];
			for (int i=1; i<domainNameLength+1; i++){
				responseDomainName += (char)response[element_count+nameCounter+i];
			}
			nameCounter += domainNameLength+1;
			if(response[element_count+nameCounter+1] != 0)	//This cuts of "." at end of string
				responseDomainName += '.';
		}
		//System.out.println("QNAME:  " + responseDomainName);
		return element_count+nameCounter+1;
	}
	
	//QTYPE specifies the type of query
	//in this method, we split the 16-bit field into separate 2 bytes
	private static int decodeResponseQTYPE(byte[] response, int element_count) {
		char[] qTypeField = bytesToBinaryString(response[element_count]).toCharArray();
		int QType;
		String qTypeString = "";
		
		qTypeString += qTypeField[0];
		qTypeString += qTypeField[1];
		qTypeString += qTypeField[2];
		qTypeString += qTypeField[3];
		qTypeString += qTypeField[4];
		qTypeString += qTypeField[5];
		qTypeString += qTypeField[6];
		qTypeString += qTypeField[7];
		
		qTypeField = bytesToBinaryString(response[element_count+1]).toCharArray();
		
		qTypeString += qTypeField[0];
		qTypeString += qTypeField[1];
		qTypeString += qTypeField[2];
		qTypeString += qTypeField[3];
		qTypeString += qTypeField[4];
		qTypeString += qTypeField[5];
		qTypeString += qTypeField[6];
		qTypeString += qTypeField[7];
		
		QType = binaryStringToInt(qTypeString);
		//if (QType > 3) System.out.println("ERROR: This is not an A, NS, or MX record");
		
		int element_position_counter = element_count + 2;
		return element_position_counter;
	}
	
	//QCLASS specifies the class of query
	//in this method, we split the 16-bit field into separate 2 bytes
	public static int decodeResponseQCLASS(byte[] response, int element_count) {
		char[] qClassField = bytesToBinaryString(response[element_count]).toCharArray();
		String qClassString = "";
		
		qClassString += qClassField[0];
		qClassString += qClassField[1];
		qClassString += qClassField[2];
		qClassString += qClassField[3];
		qClassString += qClassField[4];
		qClassString += qClassField[5];
		qClassString += qClassField[6];
		qClassString += qClassField[7];
		
		qClassField = bytesToBinaryString(response[element_count+1]).toCharArray();
		
		qClassString += qClassField[0];
		qClassString += qClassField[1];
		qClassString += qClassField[2];
		qClassString += qClassField[3];
		qClassString += qClassField[4];
		qClassString += qClassField[5];
		qClassString += qClassField[6];
		qClassString += qClassField[7];
		
		QClass = binaryStringToInt(qClassString);
		if(QClass != 1)	{
			System.out.println("ERROR: This is not an IN (Internet Address) question");
		}
		int element_position_counter = element_count + 2;
		return element_position_counter;
	}
	
	//In this method, we decode the NAME field of the answer section
	//we are also looking for an offset and name compression in the form of 11
	public static int decodeResponseNAME(byte[] response, int element_count) {
		String binary = byteToBinaryString(response[element_count]) + byteToBinaryString(response[element_count + 1]);
		
		if(binary.startsWith("11")){	//Compressed
			String offsetString = binary.substring(2);
			int offset = binaryStringToInt(offsetString);
			decodeResposeQNAME(response, offset); //send offset to decode method to turn response to string
			return element_count + 2;
		}
		else {	//No compression
			//System.out.println("No compression occured");
			return decodeResposeQNAME(response, element_count);
		}
	}

	//decode response type for either a, ns, or mx
	public static int decodeResponseType(byte[] response, int element_count) {
		String binary = byteToBinaryString(response[element_count]) + byteToBinaryString(response[element_count + 1]);
		type = binaryStringToInt(binary);
		return element_count + 2;
	}
	
	//decode from hex to integer
	public static int decodeResponseClassType(byte[] response, int element_count) {
		String binary = byteToBinaryString(response[element_count]) + byteToBinaryString(response[element_count + 1]);
		int responseClass = binaryStringToInt(binary);
		return element_count + 2;
	}
	
	public static int decodeResponseTTL(byte[] response, int element_count) {
		String binary = byteToBinaryString(response[element_count]) + byteToBinaryString(response[element_count + 1]) + byteToBinaryString(response[element_count + 2]) + byteToBinaryString(response[element_count + 3]);
		timetolive = binaryStringToInt(binary);
		return element_count + 4;
	}
	
	public static int decodeResponseRDLENGTH(byte[] response, int element_count) {
		String binary = byteToBinaryString(response[element_count]) + byteToBinaryString(response[element_count + 1]);
		RDLength = binaryStringToInt(binary);
		//System.out.println(RDLength);
		return element_count + 2;
	}

	public static int decodeResponseRDATA(byte[] response, int element_count) {
		String binary = "";
		int IP_address;
		System.out.print("Server: ");
		
		if (type ==1) {
		for(int i=0; i<response.length-element_count; i++){
			binary = byteToBinaryString(response[element_count + i]);
			IP_address = binaryStringToInt(binary);
			System.out.print(IP_address + ".");
		}
		System.out.println();
		}
		//else if (type == 6) {
		//	decodeResponseNAME(response, element_count);
		//}
		else if (type == 6) {
			int nameCounter = 0;
			int domainNameLength = 0;
			String responseDomainName = "";
			while (response[element_count + nameCounter] != 0){	//A terminating octet will denote the end of the domain name
				domainNameLength = response[element_count + nameCounter];
				for (int i=1; i<element_count + RDLength; i++){
					responseDomainName += (char)response[element_count+nameCounter+i];
					System.out.print(responseDomainName);
				}
				//System.out.print(responseDomainName);
				nameCounter += domainNameLength+1;
				if(response[element_count+nameCounter+1] != 0)	//This cuts of "." at end of string
				responseDomainName += '.';
			}
		}
		
		return 0;
	}
	//end of response

	
	/*
	 * HELPER METHODS
	 */
	private static String bytesToBinaryString(byte bytes) {
			String s1 = byteToBinaryString(bytes);
			return (s1);
	}

	private static String byteToBinaryString(byte b) {
		String s1 = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
		return s1;
	}
	
	private static int binaryStringToInt(String binaryString) {
		int value = Integer.parseInt(binaryString, 2); 
		return value;
	}
	
	private static void printbytesArrayHex(byte[] bytes, int length) {
		for (int i=0; i<length; i++){
			String s1 = byteToHexString(bytes[i]);
			System.out.print(s1);
		}
		System.out.println("");
	}
	
	private static String byteToHexString(byte b) {
		StringBuilder sb = new StringBuilder();
	    sb.append(String.format("%02X ", b));
	    return(sb.toString());
	}
}
