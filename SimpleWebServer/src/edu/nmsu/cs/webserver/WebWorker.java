package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

/**
 * This simple web server was modified to have HTML file delivery work, a 404 response and tag substitution.
 * Modified by Meagan Waldo
 * Last date modified: 09/16/20
 **/

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			String neededFileName; // This will be the name of the file requested.
			
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
		    
			neededFileName = readHTTPRequest(is); // Initializes neededFileName with the name of the file.
			File neededFile = null; // This represents what is in the file.
		    
			// If the neededFileName exists then the file neededfile has its path set to the string that is neededFileName.
			if(neededFileName != null) {
				neededFile = new File(neededFileName);
			} // end of if
			
			writeHTTPHeader(os, "text/html", neededFile);
			writeContent(os, neededFile);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 * @return neededFile
	 * 		      is a string that contains the name of the file requested.
	 **/
	private String readHTTPRequest(InputStream is)
	{
		String line;
		String neededFile = null; // String that will hold the name of the file requested.
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				System.err.println("Request line: (" + line + ")");
				
				// If the line starts with GET then you have found the name of the file requested.
				if(line.startsWith("GET")) {
					neededFile = line.substring(5, line.length() - 9); // Sets neededFile to the name of the requested file.
				} // end of if
				
				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return neededFile;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 * @param neededFile 
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, File neededFile) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		// If the neededFile exists then the code is 200 else it is a 404 error.
		if(neededFile != null) {
		os.write("HTTP/1.1 200 OK\n".getBytes());
		} // end of if
		else {
	    os.write("HTTP/1.1 404 Not Found\n".getBytes());	
		} // end of else
		
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param neededFile 
	 * 			is a file that contains information to be written out to the page.
	 **/
	private void writeContent(OutputStream os, File neededFile) throws Exception
	{
		// if the neededFile exists write out to the page along with the path. Else 404.
		if (neededFile != null) {
			os.write("<html><head></head><body>\n".getBytes());
			os.write(new String("<h1>localfile:" + neededFile.getAbsolutePath() + "</h1>").getBytes()); // Prints out the path to the file.
			os.write("<h3>My web server works!</h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
	    } // end of if
		else {
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>404 Not Found</h3>\n".getBytes()); // Prints out 404.
			os.write("</body></html>\n".getBytes());
		} // end of else
	}

} // end class