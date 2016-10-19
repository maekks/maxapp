/******************************************************************************************

SelectTech.java

The purpose of this servlet is to process the selected book from the list
and generate a list of technicians and their task counts.

   + This servlet is invoked by ViewTaskDetails.jsp
   + This servlet dispatches to SelectTech.jsp

******************************************************************************************/

package maxapp;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import javax.sql.*;
import maxapp.DBI;
import maxapp.Control;
import maxapp.Shared;

public class SelectTech extends maxapp.Control{
    protected DBI dbi;
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    //Get the current HTTP session from Tomcat
    HttpSession session = request.getSession(true);
    //Gets the shared bean from session
    maxapp.Shared bean = (maxapp.Shared)session.getAttribute("shared");
    //get the taskstatus from request and the book ID
    String taskstatus = request.getParameter("Status");
    String setbookID = bean.getBookID();

    //if there has tasks remained wait to be assigned
    if(request.getParameter("AssignTask") != null){
    //get the roles from bean
    getDesigners(bean);
    getEditors(bean);
    getAdmins(bean);
    //set taskstatus
    bean.setChooseTaskStatus(taskstatus);
    bean.setBookID(setbookID);
    gotoPage("/SelectTech.jsp", request, response); //redirect the page to selectTech page
    }

    //is the parallelTask value in the request exist
    else if(request.getParameter("ParallelTask") != null){
    getDesigners(bean);
    getEditors(bean);
    getAdmins(bean);
    bean.setChooseTaskStatus(taskstatus);
    bean.setBookID(setbookID);
    gotoPage("/SelectTechParallel.jsp", request, response); //redirect the page to select parallel tech page
    }

    }

    //method to get all deisgner from the database
    private void getDesigners(maxapp.Shared bean) throws ServletException, IOException{
        String[][] designers;
        maxapp.DBI dbi = null;
try{
    dbi = new maxapp.DBI();
        // Check if there is a database connection to Tomcat
        if(dbi.connect()){
        // if connected, get the designers
        designers= dbi.getDesigners();
        bean.setDesigners(designers);

        }
}
    catch(Exception e){
    e.printStackTrace();
    bean.setError("Servlet Exception error" +e);
}

finally{
      dbi.close();//Close connection to database
}
}

//Method to
private void getEditors(maxapp.Shared bean) throws ServletException, IOException{
        String[][] editors;
        maxapp.DBI dbi = null;
try{
    dbi = new maxapp.DBI();
        //Check if there is a database connection to Tomcat
        if(dbi.connect()){
        //if yes, get the editors from database
        editors= dbi.getEditors();
        bean.setEditors(editors);

        }
}
    catch(Exception e){
    e.printStackTrace();
    bean.setError("Servlet Exception error" +e);
}

finally{
      dbi.close();//Close connection to database
}
}

//Method
private void getAdmins(maxapp.Shared bean) throws ServletException, IOException{
        String[][] admins;
        maxapp.DBI dbi = null;
try{
    dbi = new maxapp.DBI();
        //Check if there is a database connection to Tomcat
        if(dbi.connect()){
        //if connected, get the admins from database
        admins= dbi.getAdmins();
        bean.setAdmins(admins);

        }
}
    catch(Exception e){
    e.printStackTrace();
    bean.setError("Servlet Exception error" +e);
}

finally{
      dbi.close();//Close connection to database
}
}

}//End of Class