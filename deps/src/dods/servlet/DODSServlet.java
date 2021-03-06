/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@oce.orst.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//
/////////////////////////////////////////////////////////////////////////////


/* $Id: DODSServlet.java,v 1.3 2004-02-06 15:23:50 donm Exp $
*
*/

package dods.servlet;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import javax.servlet.*;
import javax.servlet.http.*;

import dods.dap.*;
import dods.dap.Server.*;
import dods.dap.parser.ParseException;
import dods.util.*;

/**
* DODSServlet is the base servlet class for all DODS
* servers. (Well, all DODS servers running as java servlets)
* Default handlers for all of the acceptable DODS client
* requests are here.
* <p>
* Each of the request handlers appears as an adjunct method to
* the doGet() method of the base servlet class.
* <p>
* This is an abstract class because it is left to the individual
* server development efforts to write the getDDS() and
* getServerVersion() methods. The getDDS() method is intended to
* be where the server specific DODS server data types are
* used via their associated class factory.
* <p>
* This code relies on the <code>javax.servlet.ServletConfig</code>
* interface (in particular the <code>getInitParameter()</code> method)
* to retrieve the name of a .ini file containing information
* about where to find extensive configuration information used by
* the servlet. Alternate methods for establishing this functionality
* can be arranged by overloading the method <code>loadIniFile()</code>
* <p>
* The servlet should be started in the servlet engine with the following
* initParameters set:
* <p>
* <b>For the old jswdk servlet engine:</b>
* <pre>
* dts.code=dods.servers.test.dts
* dts.initparams=iniFilePath=/usr/dods/dts,iniFileName=dts.ini
* </pre>
* <b>For the tomcat servlet engine:</b>
* <pre>
*    &lt;servlet&gt;
*        &lt;servlet-name&gt;
*            dts
*        &lt;/servlet-name&gt;
*        &lt;servlet-class&gt;
*            dods.servers.test.dts
*        &lt;/servlet-class&gt;
*        &lt;init-param&gt;
*            &lt;param-name&gt;iniFilePath&lt;/param-name&gt;
*            &lt;param-value&gt;/usr/dods/dts&lt;/param-value&gt;
*        &lt;/init-param&gt;
*        &lt;init-param&gt;
*            &lt;param-name&gt;iniFileName&lt;/param-name&gt;
*            &lt;param-value&gt;dts.ini&lt;/param-value&gt;
*        &lt;/init-param&gt;
*    &lt;/servlet&gt;
*
* </pre>
* Assuming, of course, that the .ini file is located in /usr/dods/dts
* and is named dts.ini. For example .ini files look in the subdirectory
* Java-DODS/.ini (where Java-DODS is the top of the distribution).
* <p>
* Also, the method <code>processDodsURL()</code> could be overloaded
* if some kind of special processing of the incoming request is needed
* to ascertain the DODS URL information.
*
* @see #getDDS(String)
* @see #loadIniFile()
* @see #processDodsURL(HttpServletRequest)
*
* @author Nathan David Potter
*/


public abstract class DODSServlet extends HttpServlet {

    private boolean track = false;


    /***************************************************************************
    * Used for thread syncronization.
    *
    * @serial
    */
    private Object syncLock = new Object();


    /***************************************************************************
    * Count "hits" on the server...
    *
    * @serial
    */
    private int HitCounter = 0;


    /***************************************************************************
    * This function must be implemented locally for each DODS server. It should
    * return a String containing the DODS Server Version...
    */
    public abstract String getServerVersion();



    /***************************************************************************
    * This method must be implemented locally for each DODS server. The
    * local implementation of this method is the key piece for connecting
    * any localized data types that are derived from the dods.dap.Server types
    * back into the running servlet.
    * <p>
    * This method should do the following:
    *	<ul>
    *	<li> Make a new ServerFactory (aka BaseTypeFactory) for the dataset requested.
    *	<li> Instantiate a ServerDDS using the ServerFactory and populate it (this
    *		 could be accomplished by just opening a (cached?) DDS in a file and parsing it)
    *	<li> Return this freshly minted ServerDDS object (to the servlet code where it is used.)
    *	</ul>
    *
    * @param dataSet the name of the data set requested.
    *
    * @return The ServerDDS object all parsed and ready to roll.
    *
    * @see dods.dap.Server.ServerDDS
    * @see dods.servers.sql.sqlServerFactory
    * @see dods.servers.test.test_ServerFactory
    */
    //protected abstract ServerDDS getDDS(String dataSet) throws DODSException, ParseException;

    protected abstract GuardedDataset getDataset(requestState rs) throws DODSException, IOException, ParseException;

    /***************************************************************************
    * Intitializes the servlet. Init (at this time) basically sets up
    * the object dods.util.Debug from the debuggery flags in the
    * servlet InitParameters. The Debug object can be referenced (with
    * impunity) from any of the dods code...
    *
    */

    public void init() throws ServletException {

        super.init();

        // debuggering
        String debugOn = getInitParameter("DebugOn");
        if (debugOn != null) {
            System.out.println("** DebugOn **");
            StringTokenizer toker = new StringTokenizer(debugOn);
            while (toker.hasMoreTokens())
                Debug.set(toker.nextToken(), true);
        }

    }

    /***************************************************************************
    * Turns a ParseException into a DODS error and sends it to the client.
    *
    * @param pe The <code>ParseException</code> that caused the problem.
    * @param response The <code>HttpServletResponse</code> for the client.
    */
    public void parseExceptionHandler(ParseException pe, HttpServletResponse response){

        System.out.println( pe);
        pe.printStackTrace();

      try {
            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
            response.setHeader("Content-Description", "dods_error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");

            // Strip any double quotes out of the parser error message.
            // These get stuck in auto-magically by the javacc generated parser
            // code and they break our error parser (bummer!)
            String msg =  pe.getMessage().replace('\"','\'');

            DODSException de2 = new DODSException(DODSException.CANNOT_READ_FILE, msg);
            de2.print(eOut);
        }
      catch(IOException ioe){
        System.out.println("Cannot respond to client! IO Error: "+ioe.getMessage());
      }

    }

    /***************************************************************************
    * Sends a DODS error to the client.
    *
    * @param de The DODS exception that caused the problem.
    * @param response The <code>HttpServletResponse</code> for the client.
    */
    public void dodsExceptionHandler(DODSException de, HttpServletResponse response){

        System.out.println( de);
        de.printStackTrace();

        try {
            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());

            response.setHeader("Content-Description", "dods_error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");

            de.print(eOut);
            de.print(System.out);


        }
	catch(IOException ioe){
	    System.out.println("Cannot respond to client! IO Error: "+ioe.getMessage());
	}


    }


    /***************************************************************************
    * Sends an error to the client.
    *
    * @param de The exception that caused the problem.
    * @param response The <code>HttpServletResponse</code> for the client.
    */
    public void anyExceptionHandler(Throwable e, HttpServletResponse response, requestState rs){

        try {
            DataOutputStream dos = new DataOutputStream(response.getOutputStream());

            response.setHeader("Content-Description", "dods_error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");
            dos.writeUTF("DODServlet ERROR: "+e.getMessage());

            System.out.println("DODServlet ERROR (anyExceptionHandler): "+e);

            System.out.println(rs);
            if (track) {
              RequestDebug reqD = (RequestDebug) rs.getUserObject();
              System.out.println("  request number: " +  reqD.reqno+" thread: "+reqD.threadDesc);
            }

            e.printStackTrace();
        }
	catch(IOException ioe){
	    System.out.println("Cannot respond to client! IO Error: "+ioe.getMessage());
	}


    }


    /***************************************************************************
    *
    * In this (default) implementation of the getDAS() method a locally cached
    * DAS is retrieved and parsed. In this method the DAS for the passed dataset
    * is loaded from the "das_cache_dir" indidcated in the "[Server]" section of the
    * DODSiniFile. If the there is no file available a DODSException is
    * thrown. It is certainly possible (and possibly very desirable) to override
    * this method when overriding the getDDS() method. One reason for doing this
    * is if the DODS server being implemented can generate the DAS information
    * dynamically.
    *
    * When overriding this method be sure that it does the following:
    *	<ul>
    *	<li> Instantiates the DAS for the indicated (passed) dataset and
    *        populates it. This is accomplished in the default implementation
    *        by opening a (cached?) DAS stored in a file and parsing it. In
    *        a different implementation it could be created dynamically.
    *   <li> Returns this freshly minted DAS object. (to the servlet code where it is used.)
    *   </ul>
    *
    * @param dataSet the name of the data set requested.
    *
    *
    * @return The DAS object for the data set specified in the parameter <code>dataSet</code>
    *
    * @see dods.dap.DAS
    */
    protected DAS getDAS(requestState rs) throws DODSException, ParseException {

        DataInputStream is = null;
        DAS myDAS = new DAS();
	boolean gotIt = false;

	try {
	    is = openCachedDAS( rs);

	    myDAS.parse(is);
            gotIt = true;
        }
        catch (FileNotFoundException fnfe) {
	    // This is no big deal. We just trap it and return an
	    // empty DAS object.
	    gotIt = false;
	}
	finally {
	    try {
	        if(is!=null) is.close();
            }
            catch (IOException ioe) {

	        throw new DODSException(DODSException.UNKNOWN_ERROR,ioe.getMessage());
            }
	}

        if(gotIt)
	    if(Debug.isSet("showResponse"))
	        System.out.println("Successfully opened and parsed DAS cache: " + rs.getDataSet());
	else
            if(Debug.isSet("showResponse"))
	        System.out.println("No DAS present for dataset: " + rs.getDataSet());

	return(myDAS);

    }
    /***************************************************************************/


    /***************************************************************************
    * Sends a DODS error (type UNKNOWN ERROR) to the client and displays a
    * message on the server console.
    *
    *
    * @param request The client's <code> HttpServletRequest</code> request object.
    * @param response The server's <code> HttpServletResponse</code> response object.
    * @param clientMsg Error message <code>String</code> to send to the client.
    * @param serverMsg Error message <code>String</code> to display on the server console.
    */
    public void sendDODSError(HttpServletRequest request,
                              HttpServletResponse response,
			      String clientMsg,
			      String serverMsg)
			      throws IOException, ServletException {

        response.setContentType("text/plain");
	response.setHeader("XDODS-Server",  getServerVersion() );
	response.setHeader("Content-Description", "dods_error");
	// Commented because of a bug in the DODS C++ stuff...
	//response.setHeader("Content-Encoding", "none");

	ServletOutputStream Out = response.getOutputStream();

	DODSException de = new DODSException(DODSException.UNKNOWN_ERROR, clientMsg);

	de.print(Out);

	response.setStatus(response.SC_OK);

	System.out.println(serverMsg);


    }
    /***************************************************************************/


    /***************************************************************************
    * Opens a DDS cached on local disk. This can be used on DODS servers (such
    * as the DODS SQL Server) that rely on locally cached DDS files as opposed
    * to dynamically generated DDS's.
    *
    * <p>This method uses the <code>iniFile</code> object cached by <code>
    * loadIniFile()</code> to determine where to look for the cached <code>
    * DDS</code>.
    *
    * @param dataSet The name of the dataset whose DDS is being requested.
    *
    * @return An open <code>DataInputStream</code> from which the DDS can
    * be read.
    *
    * @exception DODSException
    *
    * @see #loadIniFile()
    */
    public DataInputStream openCachedDDS(requestState rs) throws DODSException {


        String cacheDir = rs.getInitParameter("DDScache");

        if(cacheDir == null)
            cacheDir = rs.defaultDDScache;


        try{

            // go get a file stream that points to the requested DDSfile.

            File fin = new File(cacheDir + rs.getDataSet());
            FileInputStream fp_in = new FileInputStream(fin);
            DataInputStream dds_source = new DataInputStream(fp_in);

            return(dds_source);
        }
        catch (FileNotFoundException fnfe) {
            throw new DODSException(DODSException.CANNOT_READ_FILE,fnfe.getMessage());
        }




    }
    /***************************************************************************/





    /***************************************************************************
    * Opens a DAS cached on local disk. This can be used on DODS servers (such
    * as the DODS SQL Server) that rely on locally cached DAS files as opposed
    * to dynamically generated DAS's.
    *
    * <p>This method uses the <code>iniFile</code> object cached by <code>
    * loadIniFile()</code> to determine where to look for the cached <code>
    * DDS</code>.
    *
    * <p>If the DAS cannot be found an error is sent back to the client.
    *
    * @param dataSet The name of the data set whose DAS is being requested.
    *
    * @return An open <code>DataInputStream</code> from which the DAS can
    * be read.
    *
    * @exception FileNotFoundException
    *
    * @see #loadIniFile()
    */
    public DataInputStream openCachedDAS(requestState rs)  throws FileNotFoundException {


        String cacheDir = rs.getInitParameter("DAScache");

        if(cacheDir == null)
            cacheDir = rs.defaultDAScache;

     	// go get a file stream that points to the requested DASfile.
        File fin = new File(cacheDir + rs.getDataSet());
        FileInputStream fp_in = new FileInputStream(fin);
        DataInputStream das_source = new DataInputStream(fp_in);
        return(das_source);


    }
    /***************************************************************************/




    /***************************************************************************
    * Default handler for the client's DAS request. Operates on the assumption
    * that the DAS information is cached on a disk local to the server. If you
    * don't like that, then you better override it in your server :)
    *
    * <p>Once the DAS has been parsed it is sent to the requesting client.
    *
    * @param request The client's <code> HttpServletRequest</code> request object.
    * @param response The server's <code> HttpServletResponse</code> response object.
    * @param dataSet Name of the datset whose DAS object is requested.
    * @param constraintExpression Constraint expression recieved from the client, probably
    * not needed here, but what the heck, it's there if you need it.
    */
    public void doGetDAS(HttpServletRequest request,
                         HttpServletResponse response,
			 requestState rs)
			 throws IOException, ServletException {

        if(Debug.isSet("showResponse"))
	    System.out.println("Sending DAS for dataset: " + rs.getDataSet());

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server",  getServerVersion() );
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the DODS C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        GuardedDataset ds = null;
        try {
          ds = getDataset(rs);
	  if(Debug.isSet("showResponse")) System.out.println("Got the GuardedDataset...");
          DAS myDAS = ds.getDAS();
          myDAS.print(Out);
          response.setStatus(response.SC_OK);
        }
        catch (DODSException de){
          dodsExceptionHandler(de,response);
        }
        catch (ParseException pe) {
          parseExceptionHandler(pe,response);
        }
        finally { // release lock if needed
          if (ds != null) ds.release();
        }


    }
    /***************************************************************************/




    /***************************************************************************
    * Default handler for the client's DDS request. Requires the getDDS() method
    * implemented by each server localization effort.
    *
    * <p>Once the DDS has been parsed and constrained it is sent to the
    * requesting client.
    *
    * @param request The client's <code> HttpServletRequest</code> request object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    * @param dataSet Name of the datset whose DDS object is requested.
    * @param constraintExpression Constraint expression recieved from the client.
    * This is used (if it's not just empty) to ship the client a view of the
    * constrained DDS.
    */
    public void doGetDDS(HttpServletRequest request,
                         HttpServletResponse response,
			 requestState rs)
			 throws IOException, ServletException {


        if(Debug.isSet("showResponse"))
	    System.out.println("Sending DDS for dataset: " + rs.getDataSet());

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server",  getServerVersion() );
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the DODS C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        GuardedDataset ds = null;
        try {
            ds = getDataset(rs);

            // Utilize the getDDS() method to get a parsed and populated DDS
            // for this server.
            ServerDDS myDDS = ds.getDDS();

            if(rs.getConstraintExpression().equals("")){ // No Constraint Expression?
                // Send the whole DDS
                myDDS.print(Out);
                Out.flush();
            }
            else { // Otherwise, send the constrained DDS

                // Instantiate the CEEvaluator and parse the constraint expression
                CEEvaluator ce = new CEEvaluator(myDDS);
                ce.parseConstraint(rs.getConstraintExpression());

                // Send the constrained DDS back to the client
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(Out));
                myDDS.printConstrained(pw);
                pw.flush();
            }

            response.setStatus(response.SC_OK);
        }
        catch (DODSException de){
           dodsExceptionHandler(de,response);
        }
        catch (IOException pe) {
           anyExceptionHandler(pe,response, rs);
        }
        catch (ParseException pe) {
           parseExceptionHandler(pe,response);
        }
        finally { // release lock if needed
          if (ds != null) ds.release();
        }


    }
    /***************************************************************************/






    /***************************************************************************
    * Default handler for the client's data request. Requires the getDDS()
    * method implemented by each server localization effort.
    *
    * <p>Once the DDS has been parsed, the data is read (using the class in the
    * localized server factory etc.), compared to the constraint expression,
    * and then sent to the client.
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    * @param dataSet Name of the datset whose data is requested.
    * @param constraintExpression Constraint expression recieved from the client.
    * This is used (if it's not just empty) subset the data in the dataset.
    */
    public void doGetDODS(HttpServletRequest request,
                          HttpServletResponse response,
                          requestState rs)
			  throws IOException, ServletException {


        if (Debug.isSet("showResponse"))
	    System.out.println("Sending DODS Data For: " + rs.getDataSet());

        response.setContentType("application/octet-stream");
        response.setHeader("XDODS-Server",  getServerVersion() );
        response.setHeader("Content-Description", "dods_data");

        ServletOutputStream sOut = response.getOutputStream();
        OutputStream bOut, eOut;


        if (rs.getAcceptsCompressed()){
            response.setHeader("Content-Encoding", "deflate");
            bOut = new DeflaterOutputStream(sOut);
        }
        else {
            // Commented out because of a bug in the DODS C++ stuff...
            //response.setHeader("Content-Encoding", "plain");
            bOut = new BufferedOutputStream(sOut);
        }

        GuardedDataset ds = null;
        try {
          ds = getDataset(rs);

          // Utilize the getDDS() method to get	a parsed and populated DDS
          // for this server.
          ServerDDS myDDS = ds.getDDS();

          // Instantiate the CEEvaluator and parse the constraint expression
          CEEvaluator ce = new CEEvaluator(myDDS);
          ce.parseConstraint( rs.getConstraintExpression());

          // debug
          // System.out.println("CE DDS = ");
          // myDDS.printConstrained(System.out);

          // Send the constrained DDS back to the client
          PrintWriter pw = new PrintWriter(new OutputStreamWriter(bOut));
          myDDS.printConstrained(pw);

          // Send the Data delimiter back to the client
          //pw.println("Data:"); // JCARON CHANGED
          pw.flush();
          bOut.write("\nData:\n".getBytes()); // JCARON CHANGED
          bOut.flush();

          // Send the binary data back to the client
          DataOutputStream sink = new DataOutputStream(bOut);
          ce.send(myDDS.getName(), sink, ds);
          sink.flush();

          // Finish up tsending the compressed stuff, but don't
          // close the stream (who knows what the Servlet may expect!)
          if (rs.getAcceptsCompressed())
              ((DeflaterOutputStream)bOut).finish();

          response.setStatus(response.SC_OK);
        }
        catch (DODSException de){
           dodsExceptionHandler(de, response);
        }
        catch (ParseException pe) {
          parseExceptionHandler(pe, response);
        }
        finally {  // release lock if needed
          if (ds != null) ds.release();
        }

    }
    /***************************************************************************/



    /***************************************************************************
    * Default handler for the client's directory request.
    *
    * Returns an html document to the client showing (a possibly pseudo)
    * listing of the datasets available on the server in a directory listing
    * format.
    * <p>
    * The bulk of this code resides in the class dods.servlet.dodsDIR and
    * documentation may be found there.
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    *
    * @see dodsDIR
    */
    public void doGetDIR(HttpServletRequest request,
                         HttpServletResponse response,
			 requestState rs)
			 throws IOException, ServletException {



	response.setHeader("XDODS-Server", getServerVersion());
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_directory");

        try {
	    dodsDIR di = new dodsDIR();
	    di.sendDIR(request, response, rs);
            response.setStatus(response.SC_OK);
        }
        catch (DODSException de){
           dodsExceptionHandler(de,response);
        }
        catch (ParseException pe) {
            parseExceptionHandler(pe,response);
        }

        return;

    }
    /***************************************************************************/




    /***************************************************************************
    * Default handler for the client's version request.
    *
    * <p>Returns a plain text document with server version and DODS core
    * version #'s
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    */
    public void doGetVER(HttpServletRequest request,
                         HttpServletResponse response)
			 throws IOException, ServletException {

        if(Debug.isSet("showResponse"))
	    System.out.println("Sending Version Tag.");

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server",  getServerVersion() );
        response.setHeader("Content-Description", "dods_version");
        // Commented because of a bug in the DODS C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        pw.println("Server Version: "+getServerVersion());
        pw.flush();

        response.setStatus(response.SC_OK);

    }
    /***************************************************************************/





    /***************************************************************************
    * Default handler for the client's help request.
    *
    * <p> Returns an html page of help info for the server
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    */
    public void doGetHELP(HttpServletRequest request,
                          HttpServletResponse response)
			  throws IOException, ServletException {

        if(Debug.isSet("showResponse"))
	    System.out.println("Sending Help Page.");

        response.setContentType("text/html");
        response.setHeader("XDODS-Server",  getServerVersion() );
        response.setHeader("Content-Description", "dods_help");
        // Commented because of a bug in the DODS C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));


        printHelpPage(pw);
        pw.flush();

        response.setStatus(response.SC_OK);


    }
    /***************************************************************************/



    /***************************************************************************
    * Sends an html document to the client explaining that they have used a
    * poorly formed URL and then the help page...
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    */
    public void badURL(HttpServletRequest request,
                       HttpServletResponse response)
		       throws IOException, ServletException {

        if(Debug.isSet("showResponse"))
	    System.out.println("Sending Bad URL Page.");

        response.setContentType("text/html");
        response.setHeader("XDODS-Server",  getServerVersion() );
        response.setHeader("Content-Description", "BadURL");
        // Commented because of a bug in the DODS C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        printBadURLPage(pw);
        printHelpPage(pw);
        pw.flush();

        response.setStatus(response.SC_OK);


    }
    /***************************************************************************/



    /***************************************************************************
    * Default handler for DODS ascii data requests. Returns the request data as
    * a comma delimited ascii file. Note that this means that the more complex
    * DODS structures such as Grids get flattened...
    * <p>
    * The bulk of this code resides in the class dods.servlet.dodsASCII and
    * documentation may be found there.
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    *
    * @see dodsASCII
    */
    public void doGetASC(HttpServletRequest request,
                          HttpServletResponse response,
                          requestState rs)
			  throws IOException, ServletException {


        if (Debug.isSet("showResponse"))
	    System.out.println("Sending ASC Data For: " + rs.getDataSet());

	response.setHeader("XDODS-Server", getServerVersion());
        response.setContentType("text/plain");
        response.setHeader("Content-Description", "dods_ascii");

        try {
          dodsASCII di = new dodsASCII();
          di.sendASCII(request, response, rs.getDataSet());
          response.setStatus(response.SC_OK);
        }
        catch (DODSException de){
          dodsExceptionHandler(de,response);
        }
        catch (ParseException pe) {
          parseExceptionHandler(pe,response);
        }

       return;
    }
    /***************************************************************************/


    /***************************************************************************
    * Default handler for DODS info requests. Returns an html document
    * describing the contents of the servers datasets.
    * <p>
    * The bulk of this code resides in the class dods.servlet.dodsINFO and
    * documentation may be found there.
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    *
    * @see dodsINFO
    */
    public void doGetINFO(HttpServletRequest request,
                          HttpServletResponse response,
			  requestState rs)
			  throws IOException, ServletException {

        PrintStream pw = new PrintStream(response.getOutputStream());

        response.setHeader("XDODS-Server", getServerVersion());
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_description");

        GuardedDataset ds  = null;
        try {
          ds = getDataset(rs);
          dodsINFO di = new dodsINFO();
          di.sendINFO(pw, ds, rs);
          response.setStatus(response.SC_OK);
        }
        catch (DODSException de){
          dodsExceptionHandler(de,response);
        }
//        catch (IOException pe) {
//           anyExceptionHandler(pe,response, rs);
//        }
        catch (ParseException pe) {
          parseExceptionHandler(pe,response);
        }
        finally {  // release lock if needed
          if (ds != null) ds.release();
        }

        return;
    }
    /**************************************************************************/




    /***************************************************************************
    * Default handler for DODS .html requests. Returns the DODS Web Interface
    * (aka The Interface From Hell) to the client.
    * <p>
    * The bulk of this code resides in the class dods.servlet.dodsHTML and
    * documentation may be found there.
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    *
    * @see dodsHTML
    */
    public void doGetHTML(HttpServletRequest request,
                          HttpServletResponse response,
			  requestState rs)
			  throws IOException, ServletException {


        response.setHeader("XDODS-Server", getServerVersion());
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_form");

        GuardedDataset ds = null;
        try {
          ds = getDataset(rs);

          // Utilize the getDDS() method to get	a parsed and populated DDS
          // for this server.
          ServerDDS myDDS = (ServerDDS) ds.getDDS();
          DAS das = ds.getDAS();
          dodsHTML di = new dodsHTML();
          di.sendDataRequestForm(request, response, rs.getDataSet(), myDDS, das);
          response.setStatus(response.SC_OK);
        }
        catch (DODSException de){
          dodsExceptionHandler(de,response);
        }
        catch (IOException pe) {
           anyExceptionHandler(pe,response, rs);
        }
        catch (ParseException pe) {
          parseExceptionHandler(pe,response);
        }
        finally {  // release lock if needed
          if (ds != null) ds.release();
        }

        return;
    }
    /***************************************************************************/


    /***************************************************************************
    * Default handler for DODS catalog.xml requests.
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    *
    * @see dodsHTML
    */
    public void doGetCatalog(HttpServletRequest request,
                          HttpServletResponse response)
			  throws IOException, ServletException {


        response.setHeader("XDODS-Server", getServerVersion());
        response.setContentType("text/xml");
        response.setHeader("Content-Description", "dods_catalog");

        PrintWriter pw = new PrintWriter(response.getOutputStream());
        printCatalog( pw);
        pw.flush();
        response.setStatus(response.SC_OK);

        return;
    }

    // to be overridden by servers that implement catalogs
    protected void printCatalog(PrintWriter os) throws IOException {
      os.println("Catalog not available for this server");
      os.println("Server version = "+getServerVersion());
    }

    /***************************************************************************/



    /***************************************************************************
    * Default handler for DODS status requests; not publically available,
    * used only for debugging
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    *
    * @see dodsHTML
    */
    public void doGetStatus(HttpServletRequest request,
                          HttpServletResponse response)
			  throws IOException, ServletException {


        response.setHeader("XDODS-Server", getServerVersion());
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");

        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println("<title>Server Status</title>");
        pw.println("<body><ul>");
        printStatus( pw);
        pw.println("</ul></body>");
        pw.flush();
        response.setStatus(response.SC_OK);

        return;
    }

    // to be overridden by servers that implement status report
    protected void printStatus(PrintWriter os) throws IOException {
        os.println("<h2>Server version = " + getServerVersion() + "</h2>");
        os.println("<h2>Number of Requests Received = " + HitCounter + "</h2>");
        if (track) {
            int n = prArr.size();
            int pending = 0;
            String preqs = "";
            for (int i=0; i<n; i++) {
                requestState rs = (requestState) prArr.get(i);
                RequestDebug reqD = (RequestDebug) rs.getUserObject();
                if ((rs != null) && !reqD.done){
                    preqs += "<pre>-----------------------\n";
                    preqs += "Request[" + reqD.reqno + "](" + reqD.threadDesc + ") is pending.\n";
                    preqs += rs.toString();
                    preqs += "</pre>";
                    pending++;
                }
            }
            os.println("<h2>" + pending + " Pending Request(s)</h2>");
	    os.println(preqs);
        }
    }

    /***************************************************************************/

    /***************************************************************************
    * This is a bit of instrumentation that I kept around to let me look at the
    * state of the incoming <code>HttpServletRequest</code> from the client.
    * This method calls the <code>get*</code> methods of the request and prints
    * the results to standard out.
    *
    * @param request The <code>HttpServletRequest</code> object to probe.
    */

    public  void probeRequest(HttpServletRequest request){

	Enumeration e;
	int i;


	System.out.println("####################### PROBE ##################################");
	System.out.println("The HttpServletRequest object is actually a: "+request.getClass().getName());
	System.out.println("");
	System.out.println("HttpServletRequest Interface:");
	System.out.println("    getAuthType:           "+request.getAuthType());
	System.out.println("    getMethod:             "+request.getMethod());
	System.out.println("    getPathInfo:           "+request.getPathInfo());
	System.out.println("    getPathTranslated:     "+request.getPathTranslated());
	System.out.println("    getQueryString:        "+request.getQueryString());
	System.out.println("    getRemoteUser:         "+request.getRemoteUser());
	System.out.println("    getRequestedSessionId: "+request.getRequestedSessionId());
	System.out.println("    getRequestURI:         "+request.getRequestURI());
	System.out.println("    getServletPath:        "+request.getServletPath());
	System.out.println("    isRequestedSessionIdFromCookie: "+request.isRequestedSessionIdFromCookie());
	System.out.println("    isRequestedSessionIdValid:      "+request.isRequestedSessionIdValid());
	System.out.println("    isRequestedSessionIdFromURL:    "+request.isRequestedSessionIdFromURL());

	System.out.println("");
	i = 0;
        e = request.getHeaderNames();
	System.out.println("    Header Names:");
	while(e.hasMoreElements()){
	    i++;
	    String s = (String) e.nextElement();
	    System.out.print("        Header["+i+"]: "+s);
	    System.out.println(": "+request.getHeader(s));
	}

	System.out.println("");
	System.out.println("ServletRequest Interface:");
	System.out.println("    getCharacterEncoding:  "+request.getCharacterEncoding());
	System.out.println("    getContentType:        "+request.getContentType());
	System.out.println("    getContentLength:      "+request.getContentLength());
	System.out.println("    getProtocol:           "+request.getProtocol());
	System.out.println("    getScheme:             "+request.getScheme());
	System.out.println("    getServerName:         "+request.getServerName());
	System.out.println("    getServerPort:         "+request.getServerPort());
	System.out.println("    getRemoteAddr:         "+request.getRemoteAddr());
	System.out.println("    getRemoteHost:         "+request.getRemoteHost());
	//System.out.println("    getRealPath:           "+request.getRealPath());


	System.out.println(".............................");
	System.out.println("");
	i = 0;
        e = request.getAttributeNames();
	System.out.println("    Attribute Names:");
	while(e.hasMoreElements()){
	    i++;
	    String s = (String) e.nextElement();
	    System.out.print("        Attribute["+i+"]: "+s);
	    System.out.println(" Type: "+request.getAttribute(s));
	}

	System.out.println(".............................");
	System.out.println("");
	i = 0;
        e = request.getParameterNames();
	System.out.println("    Parameter Names:");
	while(e.hasMoreElements()){
	    i++;
	    String s = (String) e.nextElement();
	    System.out.print("        Parameter["+i+"]: "+s);
	    System.out.println(" Value: "+request.getParameter(s));
	}

	System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
	System.out.println(" . . . . . . . . . Servlet Infomation API  . . . . . . . . . . . . . .");
	System.out.println("");

	System.out.println("Servlet Context:");
	System.out.println("");

	ServletContext scntxt = getServletContext();

	i = 0;
        e = scntxt.getAttributeNames();
	System.out.println("    Attribute Names:");
	while(e.hasMoreElements()){
	    i++;
	    String s = (String) e.nextElement();
	    System.out.print("        Attribute["+i+"]: "+s);
	    System.out.println(" Type: "+scntxt.getAttribute(s));
	}

        System.out.println("    ServletContext.getMajorVersion(): " + scntxt.getMajorVersion());
//        System.out.println("ServletContext.getMimeType():     " + sc.getMimeType());
        System.out.println("    ServletContext.getMinorVersion(): " + scntxt.getMinorVersion());
//        System.out.println("ServletContext.getRealPath(): " + sc.getRealPath());


	System.out.println(".............................");
	System.out.println("Servlet Config:");
	System.out.println("");


	ServletConfig scnfg = getServletConfig();


        i = 0;
        e = scnfg.getInitParameterNames();
        System.out.println("    InitParameters:");
        while (e.hasMoreElements()) {
            String p = (String) e.nextElement();
	    System.out.print("        InitParameter["+i+"]: "+p);
	    System.out.println(" Value: " + scnfg.getInitParameter(p));
	    i++;
        }
	System.out.println(".............................");
	System.out.println("HttpUtils:");
	System.out.println("");
	System.out.println("getRequestURL: "+HttpUtils.getRequestURL(request));

	System.out.println("");
	System.out.println("######################## END PROBE ###############################");
	System.out.println("");


    }
    /***************************************************************************/



    /****************************************************************************
    * This method is used to convert special characters into their
    * actual byte values.
    * <p>
    * For example, in a URL the space character
    * is represented as "%20" this method will replace that with a
    * space charater. (a single value of 0x20)
    *
    * @param ce The constraint expresion string as collected from the request
    * object with <code>getQueryString()</code>
    *
    * @returns A string containing the prepared constraint expression. If there
    * is a problem with the constraint expression a <code>null</code> is returned.
    */
    private String prepCE(String ce){

      int index;

        //System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
        //System.out.println("Prepping: \""+ce+"\"");

      if (ce == null) {
        ce = "";
	      //System.out.println("null Constraint expression.");
      } else if(!ce.equals("")) {

        //System.out.println("Searching for:  %");
        index = ce.indexOf("%");
        //System.out.println("index of %: "+index);

        if(index == -1)
	        return(ce);

        if(index>(ce.length()-3))
	         return(null);

        while(index>=0){
          //System.out.println("Found % at character " + index);

          String specChar = ce.substring(index+1,index+3);
          //System.out.println("specChar: \"" + specChar + "\"");

          // Convert that bad boy!
          char val = (char) Byte.parseByte(specChar,16);
          //System.out.println("                val: '" + val + "'");
          //System.out.println("String.valueOf(val): \"" + String.valueOf(val) + "\"");


          ce = ce.substring(0,index) + String.valueOf(val) + ce.substring(index+3,ce.length());
          //System.out.println("ce: \"" + ce + "\"");

          index = ce.indexOf("%");
          if(index>(ce.length()-3))
               return(null);
        }
      }

//      char ca[] = ce.toCharArray();
//	for(int i=0; i<ca.length ;i++)
//	    System.out.print("'"+(byte)ca[i]+"' ");
//	System.out.println("");
//	System.out.println(ce);
//	System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");

//        System.out.println("Returning CE: \""+ce+"\"");
    return (ce);
  }
    /***************************************************************************/



    /****************************************************************************
    * Evaluates a request object to determine if the client that sent the request
    * accepts compressed return documents.
    *
    * @param request The <code>HttpServletRequest</code> sent by the client
    * in question.
    *
    * @return True is the client accpets a compressed return document.
    * False otherwise.
    */

    protected boolean isTheClientCompressed(HttpServletRequest request){

        boolean isTiny;

        isTiny = false;
        String Encoding = request.getHeader("Accept-Encoding");

        if(Encoding != null)
            isTiny = Encoding.equalsIgnoreCase("deflate");
        else
            isTiny = false;

        return(isTiny);
    }
    /***************************************************************************/



    /****************************************************************************
    * Processes an incoming <code>HttpServletRequest</code> and from it sets the
    * cached values for:
    * <ul>
    *     <li> <b>dataSet</b> The data set name.(Accessible using
    *                          <code> setDataSet() </code>
    *                          and <code>getDataSet()</code>)</li>
    *     <li> <b>CE</b> The constraint expression.(Accessible using
    *                          <code> setCE() </code>
    *                          and <code>getCE()</code>)</li>
    *     <li> <b>requestSuffix</b> The request suffix, used by DODS to indicate
    *                          the type of response desired by the client.
    *                          (Accessible using
    *                          <code> setRequestSuffix() </code>
    *                          and <code>getRequestSuffix()</code>)</li>
    * </ul>
    * @param request The <code>HttpServletRequest</code> sent by the client
    * in question.
    *
    * @returns True if the URL wasn't junk, false otherwise.
    *
    * @see #getDataSet()
    * @see #setDataSet(String)
    * @see #getCE()
    * @see #setCE(String)
    * @see #getRequestSuffix()
    * @see #setRequestSuffix(String)
    */

    protected requestState processDodsURL(HttpServletRequest request){
        // Get the constraint expression from the request object and
	// convert all those special characters denoted by a % sign
        String CE = prepCE(request.getQueryString());

        // If there was simply no constraint then prepCE() should have returned
	// a CE equal "", the empty string. A null return indicates an error.
	if (CE == null){
	    return null;
	}

	// Figure out the data set name.
        String ds = request.getPathInfo();
        String suffix = null;
        if (ds != null) {
	    // Break the path up and find the last (terminal)
	    // end.
	    StringTokenizer st = new StringTokenizer(ds,"/");
	    String endOPath = "";
	    while(st.hasMoreTokens()){
	        endOPath = st.nextToken();
	    }

	    // Check the last element in the path for the
	    // character "."
            int index = endOPath.lastIndexOf('.');

            //System.out.println("last index of . in \""+ds+"\": "+index);

	    // If a dot is found take the stuff after it as the DODS suffix
            if(index >= 0) {
	        // pluck the DODS suffix off of the end
                suffix = endOPath.substring(index+1);

		// Set the data set name to the entire path minus the
		// suffix which we know exists in the last element
		// of the path.
                ds = ds.substring(1,ds.lastIndexOf('.'));
            }
	    else { // strip the leading slash (/) from the dataset name and set the suffix to an empty string
	        suffix = "";
                ds = ds.substring(1,ds.length());
	    }
	}

	return new requestState(    ds,
                                    suffix,
                                    CE,
                                    isTheClientCompressed(request),
                                    getServletConfig(),
                                    getServerName()
                                    );
    }

    /***************************************************************************
    *
    * In this (default) implementation of the getServerName() method we just get
    * the name of the servlet and pass it back. If something different is
    * required, override this method when implementing the getDDS() and
    * getServerVersion() methods.
    * <p>
    * This is typically used by the getINFO() method to figure out if there is
    * information specific to this server residing in the info directory that
    * needs to be returned to the client as part of the .info response.
    *
    * @return A string containing the name of the servlet class that is running.
    */
    public String getServerName(){

        // Ascertain the name of this server.
        String servletName = this.getClass().getName();

        return(servletName);
    }


    /**************************************************************************
    * Handles incoming requests from clients. Parses the request and determines
    * what kind of DODS response the cleint is requesting. If the request is
    * understood, then the appropriate handler method is called, otherwise
    * an error is returned to the client.
    * <p>
    * This method is the entry point for <code>DODSServlet</code>. It uses
    * the methods <code>processDodsURL</code> to extract the DODS URL
    * information from the incoming client request. This DODS URL information
    * is cached and made accessible through get and set methods.
    * <p>
    * After  <code>processDodsURL</code> is called <code>loadIniFile()</code>
    * is called to load configuration information from a .ini file,
    * <p>
    * If the standard behaviour of the servlet (extracting the DODS URL
    * information from the client request, or loading the .ini file) then
    * you should overload <code>processDodsURL</code> and <code>loadIniFile()
    * </code>. <b> We don't recommend overloading <code>doGet()</code> beacuse
    * the logic contained there may change in our core and cause your server
    * to behave unpredictably when future releases are installed.</b>
    *
    * @param request The client's <code> HttpServletRequest</code> request
    * object.
    * @param response The server's <code> HttpServletResponse</code> response
    * object.
    *
    * @see #processDodsURL(HttpServletRequest)
    * @see #getDataSet()
    * @see #setDataSet(String)
    * @see #getCE()
    * @see #setCE(String)
    * @see #getRequestSuffix()
    * @see #setRequestSuffix(String)
    * @see #loadIniFile()
    */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
		      throws IOException, ServletException {



       // response.setHeader("Last-Modified", (new Date()).toString() );

        requestState rs = null;
        RequestDebug reqD = null;
        try {
            if ( Debug.isSet("probeRequest")) probeRequest(request);

            rs = processDodsURL(request);
            synchronized(syncLock) {
                long reqno = HitCounter++;
                if (track) {
                    reqD = new RequestDebug(reqno, Thread.currentThread().toString());
                    rs.setUserObject( reqD);
                    if (prArr == null) prArr= new ArrayList( 10000);
                        prArr.add((int)reqno, rs);
                }

                if ( Debug.isSet("showRequest")) {
                    System.out.println("-------------------------------------------");
                    System.out.println("Server: "+getServerName()+"   Request #"+ reqno);

                    System.out.println(rs.toString());
                }
            }

            if(rs != null){
                String dataSet = rs.getDataSet();
                String requestSuffix = rs.getRequestSuffix();

                if(dataSet == null) {
                    doGetDIR(request, response, rs);
                }
                else if (dataSet.equals("/")) {
                    doGetDIR(request, response, rs);
                }
                else if (dataSet.equals("")) {
                    doGetDIR(request, response, rs);
                }
                else if (dataSet.equalsIgnoreCase("/version") || dataSet.equalsIgnoreCase("/version/")){
                    doGetVER(request, response);
                }
                else if(dataSet.equalsIgnoreCase("/help") || dataSet.equalsIgnoreCase("/help/")) {
                    doGetHELP(request, response);
                }
                else if ( dataSet.equalsIgnoreCase("/"+requestSuffix)) {
                    doGetHELP(request, response);
                }
                else if ( requestSuffix.equalsIgnoreCase("dds")) {
                    doGetDDS(request, response, rs);
                }

                else if ( requestSuffix.equalsIgnoreCase("das")) {
                    doGetDAS(request, response, rs);
                }

                else if ( requestSuffix.equalsIgnoreCase("dods")) {
                    doGetDODS(request, response, rs);
                }

                else if (   requestSuffix.equalsIgnoreCase("asc")         ||
                            requestSuffix.equalsIgnoreCase("ascii")) {

                    doGetASC(request, response, rs);
                }

                else if ( requestSuffix.equalsIgnoreCase("info")) {
                    doGetINFO(request, response, rs);
                }

                else if ( requestSuffix.equalsIgnoreCase("html") || requestSuffix.equalsIgnoreCase("htm")) {
                    doGetHTML(request, response, rs);
                }

                else if ( requestSuffix.equalsIgnoreCase("ver") || requestSuffix.equalsIgnoreCase("version")){
                    doGetVER(request, response);
                }

                else if ( requestSuffix.equalsIgnoreCase("help")) {
                    doGetHELP(request, response);
                }

                // JC added
                else if ( requestSuffix.equalsIgnoreCase("xml") && dataSet.equalsIgnoreCase("catalog")){
                    doGetCatalog(request, response);
                }

                else if ( dataSet.equalsIgnoreCase("status")){
                    doGetStatus(request, response);
                }

                else if ( requestSuffix.equals("")) {
                    badURL(request, response);
                }

                else {
                    badURL(request, response);
                }
            }
            else {
                badURL(request, response);
            }

            if (reqD != null) reqD.done = true;
        }
        catch (Throwable e) {
            anyExceptionHandler(e, response, rs);
        }

    }
    //**************************************************************************





    /***************************************************************************
    *  Prints the DODS Server help page to the passed PrintWriter
    *
    * @param pw PrintWriter stream to which to dump the help page.
    */
    private void printHelpPage(PrintWriter pw) {

        pw.println("<h3>DODS Server Help</h3>");
        pw.println("To access most of the features of this DODS server, append");
        pw.println("one of the following a five suffixes to a URL: .das, .dds, .dods., .info,");
        pw.println(".ver or .help. Using these suffixes, you can ask this server for:<dl>");
        pw.println("<dt> das  <dd> attribute object");
        pw.println("<dt> dds  <dd> data type object");
        pw.println("<dt> dods <dd> data object");
        pw.println("<dt> info <dd> info object (attributes, types and other information)");
        pw.println("<dt> html <dd> html form for this dataset");
        pw.println("<dt> ver  <dd> return the version number of the server");
        pw.println("<dt> help <dd> help information (this text)</dl>");
        pw.println("</dl>");
        pw.println("For example, to request the DAS object from the FNOC1 dataset at URI/GSO (a");
        pw.println("test dataset) you would appand `.das' to the URL:");
        pw.println("http://dods.gso.uri.edu/cgi-bin/nph-nc/data/fnoc1.nc.das.");

        pw.println("<p><b>Note</b>: Many DODS clients supply these extensions for you so you don't");
        pw.println("need to append them (for example when using interfaces supplied by us or");
        pw.println("software re-linked with a DODS client-library). Generally, you only need to");
        pw.println("add these if you are typing a URL directly into a WWW browser.");
        pw.println("<p><b>Note</b>: If you would like version information for this server but");
        pw.println("don't know a specific data file or data set name, use `/version' for the");
        pw.println("filename. For example: http://dods.gso.uri.edu/cgi-bin/nph-nc/version will");
        pw.println("return the version number for the netCDF server used in the first example. ");

        pw.println("<p><b>Suggestion</b>: If you're typing this URL into a WWW browser and");
        pw.println("would like information about the dataset, use the `.info' extension.");

        pw.println("<p>If you'd like to see a data values, use the `.html' extension and submit a");
        pw.println("query using the customized form.");

    }
    //**************************************************************************


    /***************************************************************************
    *  Prints the Bad URL Page page to the passed PrintWriter
    *
    * @param pw PrintWriter stream to which to dump the bad URL page.
    */
    private void printBadURLPage(PrintWriter pw) {

        pw.println("<h3>Error in URL</h3>");
        pw.println("The URL extension did not match any that are known by this");
        pw.println("server. Below is a list of the five extensions that are be recognized by");
        pw.println("all DODS servers. If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("DODS user support coordinator at: support@unidata.ucar.edu<p>");

    }
    //**************************************************************************

    // debug
    private ArrayList prArr = null;
    private class RequestDebug {
      long reqno;
      String threadDesc;
      boolean done = false;

      RequestDebug( long reqno, String threadDesc) {
        this.reqno = reqno;
        this.threadDesc = threadDesc;
      }
    }
}



