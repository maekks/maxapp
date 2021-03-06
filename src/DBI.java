/*
 * Each servlet uses a Tomcat DB connection and closes it.
 *     So all of these methods assume the connection to be established already by the "connect" method
 * The servlets are also responsible for the commit of the data,
 *     because only the servlets know how many of these operations form a transaction
 * This class throws its exceptions to be caught/managed by the servlets
 * Resulting data is returned in the return variable, often an array
*/

package maxapp;

import javax.naming.*;
import javax.sql.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Vector;

public class DBI{
    Connection conn = null;
   /* The arrays below hold string values that are used by the getString method to retirve information from ResultSet objects */
    String [] emp = {"EmpID", "EmpLastName", "EmpFirstName", "emailAddress", "JobTitle", "SocSec", "DateOfBirth", "Department", "Manager", "CommissionRate", "Shepherd", "Skill", "Password"};
    String [] author = {"AuthorID, AuthorLastName, AuthorFirstName, PenName, emailAddress, Author_Lead_Status, SalesmanID"};
    String [] book = {"BookID", "Title", "StartDate", "BookFormat"};
    String [] address = {"AuthorID, Street, City, State, Zip"};
    String [] shepherd ={"EmpID", "EmpFirstName", "EmpLastName", "count(b.ShepherdID)"};
    String [] tech ={"EmpID", "EmpFirstName", "EmpLastName", "count(t.TaskID)"};
    String [] selectedcontract = {"ContractID", "AuthorID", "EmpID", "AuthorFirstName", "AuthorLastName", "EmpFirstName", "EmpLastName", "InitialTitle","PricePublish", "RoyaltyRate", "BookDoctor", "PromotionWeeks", "DateContract", "ContractStatus"};
    String [] contract = {"ContractID", "AuthorID", "AuthorFirstName", "AuthorLastName", "EmpID", "EmpFirstName", "EmpLastName", "InitialTitle", "ContractStatus"};
    String [] Task = {"TaskID", "StartDate", "EndDate", "BookID","TaskType", "TaskNotes", "TaskStatus", "TechnicianID", "Title"};
    String [] selecttask = {"TaskID","Title", "TaskType", "TaskStatus", "TaskNotes", "StartDate", "FileName"};
    String [] unpublishedbooks = {"EmpFirstName", "EmpLastName", "Title", "StartDate", "BookStatus"};
    String [] bookslastquarter = {"ContractID", "InitialTitle", "PricePublish", "RoyaltyRate", "BookDoctor","DateContract", "ContractStatus"};
    String [] booksinprogress = {"Title","EmpFirstName","EmpLastName","TaskStatus","TaskType"};

    public void DBI() throws NamingException, SQLException{}

    public boolean connect() throws NamingException, SQLException{
      //Connect to the pinnacle database
      //Using the Tomcat Server.xml Context to locate the data
      Context initCtx = new InitialContext();
      Context envCtx = (Context)initCtx.lookup("java:comp/env");
      if(envCtx == null ) throw new NamingException("Boom - No Context");
         //Use jdbc to connect with sql statements
         DataSource ds = (DataSource)envCtx.lookup("jdbc/TestDB");
      //if DataSource ds is not null, the datasource was established (using Tomcat connection pool open with database)
      if(ds!=null)
      {
         //Get a connection from the Tomcat database connection pool with DataBase server
         conn = ds.getConnection();
         //If we get the connection ...
         if(conn != null) { return true; }
      }
      return false;
   }// end of connect method

   // Turn on/off autocommit
   public boolean autocommit(boolean tf) throws SQLException
   {
      if(conn != null)
   	{	conn.setAutoCommit(tf); // true =autocommit on, false = off
   		return true;
   	}
   	else return false;
   } // end autocommit

   // commit or rollback this transaction
   public boolean commitTrans() throws SQLException
   {
      if(conn != null)
      {
         conn.commit();
   		return true; // committ succeded
   	}
   	else return false;
   } // end commit

   public boolean rollbackTrans() throws SQLException
   {
      if(conn != null)
   	{
         conn.rollback(); // rollback succeeded
         return true;
   	}
   	else return false;
   } // end rollback

   public void close()
   {
      // give the connection back to Tomcat pool
      try
      {
         if(conn != null)
            conn.close();
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }//end of close method

//Build a jdbc statement shell for an SQL query string using the conn connection
   public ResultSet execQuery(String query) throws SQLException{
    //Execute the query and retreive the result from the database
    Statement stmt = conn.createStatement();
    ResultSet rst = stmt.executeQuery(query);
    return rst;
   }

   //Method to check the database and confirm login information
   public boolean checkEmpLogin(String email, String password) throws SQLException{
    // Resultset is a datatype that allows  to handle a range of data (grid). This  datatype is part of java.sql
    ResultSet rst = this.execQuery("Select EmpID FROM Employees WHERE emailAddress ='"+email+"' && Password = '"+password+"'");
// Go to first row of the ResultSet; if there are no rows, userid and password do not match the database
    boolean result = (rst.first());
// if the record exists, the userid and password match, so return true, else return false
    return result;
   }//End of method

 //Method to get the information of the employee who logs in
    public String[] getEmpInfo(String email) throws SQLException{
    // get employee information
    String[] result = new String[13];
    ResultSet rst = this.execQuery("Select * FROM Employees WHERE emailAddress='"+email+"'");
    if(rst.first()) {
      for (int i=0; i<13; i++) {
        result[i] = rst.getString(emp[i]); // result array is employee fields for this user
      }
    }
    return result;
  }//end of getEmpInfo method

//Method to retrieve a list of the contracts that need a book record
   public String[][] getNewContracts() throws SQLException{
    ResultSet rst = this.execQuery("SELECT c.ContractID, c.AuthorID, a.AuthorFirstName, a.AuthorLastName, c.EmpID, e.EmpFirstName, e.EmpLastName, c.InitialTitle, c.ContractStatus from Contract c, Author a, Employees e WHERE c.AuthorID = a.AuthorID && c.EmpID = e.EmpID && c.ContractStatus != 'Complete'");
    int columns = 9;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(contract[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of contract
            }
            rst.next();
        }
    }
    return result;
   }

//Method to get the information of the selected contract
      public String[][] getSelectedContract(String conid) throws SQLException{
    ResultSet rst = this.execQuery("SELECT c.ContractID, c.AuthorID, c.EmpID, a.AuthorFirstName, a.AuthorLastName, e.EmpFirstName, e.EmpLastName, c.InitialTitle, c.PricePublish, c.RoyaltyRate, c.BookDoctor, c.PromotionWeeks, c.DateContract, c.ContractStatus from Contract c, Author a, Employees e WHERE c.ContractID = '"+conid+"' && c.EmpID = e.EmpID && c.AuthorID = a.AuthorID");
    int columns = 14;
    int records = 1;
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(selectedcontract[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of contract
            }
            rst.next();
        }
    }
    return result;
   }

    //Method to get the information of the selected book
    public String[][] getSelectedBook(String bookid) throws SQLException{
    ResultSet rst = this.execQuery("SELECT BookID, Title, StartDate, ISBN, BookFormat From Book WHERE BookID = '"+bookid+"'");
    int columns = 5;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(book[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of book
            }
            rst.next();
        }
    }
    return result;
   }

    //Method to get the details of every task assigned to a book
    public String[][] getTasksofBook(String bookid) throws SQLException{
    ResultSet rst = this.execQuery("SELECT t.TaskID, t.StartDate, t.EndDate, t.BookID, t.TaskType, t.TaskNotes, t.TaskStatus, t.TechnicianID, e.EmpFirstName, e.EmpLastName From Task t, Employees e WHERE BookID = '"+bookid+"' && t.TechnicianID = e.EmpID");
    int columns = 10;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < 8; i++){ //every column
                    temp = rst.getString(Task[i]);

             if(temp == null)
             temp ="";
             result[k][i] = temp; //store details of task
             result[k][8] = rst.getString(emp[2]);
             result[k][9] = rst.getString(emp[1]);
            }
            rst.next();
        }
    }
    return result;
   }

   public String[] getTaskDetail(String taskid) throws SQLException{
       ResultSet rst = this.execQuery("SELECT t.StartDate, t.TaskType, t.TaskNotes, t.TaskStatus, b.Title,b.BookID  From Task t, Book b WHERE t.TaskID = '"+taskid+"' && t.BookID = b.BookID ");
       String [] result = new String [6];
       if(rst.first()){
            result[0] = rst.getString("StartDate");
            result[1] = rst.getString("TaskType");
            result[2] = rst.getString("TaskNotes");
            result[3] = rst.getString("Title");
            result[4] = rst.getString("TaskStatus");
            result[5] = rst.getString("BookID");
        }
        return result;
  }

    //Method to get the list and book count of the shepherds
    public String[][] getShepherds() throws SQLException{
    ResultSet rst = this.execQuery("SELECT e.EmpID, e.EmpFirstName, e.EmpLastName, count(b.ShepherdID) From Employees e Left Join Book b ON b.ShepherdID = e.EmpID Where e.Shepherd = 'Yes' OR b.BookStatus != 'Published' Group by e.EmpID, b.ShepherdID order by e.EmpID asc");
    int columns = 4;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(shepherd[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of shepherds
            }
            rst.next();
        }
    }
    return result;
   }

   //Method to get the list of designers
    public String[][] getDesigners() throws SQLException{
    ResultSet rst = this.execQuery("SELECT e.EmpID, e.EmpFirstName, e.EmpLastName, count(t.TaskID) From Employees e Left Join Task t ON t.TechnicianID = e.EmpID Where (e.Skill = 'Designer' && t.TaskStatus NOT LIKE '%Finished%') OR (e.Skill ='Designer') Group by e.EmpID, t.TechnicianID order by e.EmpID asc");
    int columns = 4;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(tech[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of designers
            }
            rst.next();
        }
    }
    return result;
   }

      //Method to get the list of editors
    public String[][] getEditors() throws SQLException{
    ResultSet rst = this.execQuery("SELECT e.EmpID, e.EmpFirstName, e.EmpLastName, count(t.TaskID) From Employees e Left Join Task t ON t.TechnicianID = e.EmpID Where (e.Skill = 'Editor' && t.TaskStatus NOT LIKE '%Finished%') OR (e.Skill ='Editor') Group by e.EmpID, t.TechnicianID order by e.EmpID asc");
    int columns = 4;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(tech[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of editors
            }
            rst.next();
        }
    }
    return result;
   }

    //Method to get the list of admins
    public String[][] getAdmins() throws SQLException{
    ResultSet rst = this.execQuery("SELECT e.EmpID, e.EmpFirstName, e.EmpLastName, count(t.TaskID) From Employees e Left Join Task t ON t.TechnicianID = e.EmpID Where (e.Skill = 'Admin' && t.TaskStatus NOT LIKE '%Finished%') OR (e.Skill ='Admin') Group by e.EmpID, t.TechnicianID order by e.EmpID asc");
    int columns = 4;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(tech[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of admins
            }
            rst.next();
        }
    }
    return result;
   }

    //Method to get the list of books that still need a shepherd
    public String[][] getBooks() throws SQLException{
    ResultSet rst = this.execQuery("SELECT BookID, Title, StartDate, BookFormat From Book WHERE BookStatus != 'Published' && ShepherdID = 0");
    int columns = 4;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(book[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of books
            }
            rst.next();
        }
    }
    return result;
   }

   public String[][] getAuthorBooks(String AuthorEmail) throws SQLException{
   ResultSet rst = this.execQuery("SELECT b.Title, b.BookStatus, b.StartDate, b.PublishedDate FROM Book b INNER JOIN Book_Author ba ON b.BookID = ba.BookID INNER JOIN Author a ON ba.AuthorID = a.AuthorID WHERE a.emailAddress = '"+ AuthorEmail + "';");
   int columns = 4;
   int records = RecordNum(rst);
   String temp;
   String [][] result = new String[records][columns];
   if(rst.first())
   {
       for (int k = 0; k < records; k++){ //every row
           result[k][0] = rst.getString("Title");
           result[k][1] = rst.getString("BookStatus");
           result[k][2] = rst.getString("StartDate");
           result[k][3] = rst.getString("PublishedDate");
           rst.next();
       }
   }
   return result;
  }

    //Method to get information of the book that was just created
    public String[][] getConfirmBook() throws SQLException{
    ResultSet rst = this.execQuery("SELECT BookID, Title, StartDate From Book WHERE BookID = (SELECT MAX(LAST_INSERT_ID(BookID)) From Book)");
    int columns = 3;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(book[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of book
            }
            rst.next();
        }
    }
    return result;
   }

    //Method to get list of books that belong to this shepherd, and Book Status is not Task Assigned or Published
    public String[][] getShepherdBookList(int empid) throws SQLException{
        ResultSet rst = this.execQuery("SELECT BookID, Title, StartDate, BookFormat FROM Book WHERE BookStatus != 'Task Assigned' AND BookStatus != 'Published' AND ShepherdID = '" + empid + "'"); // needs more detailed select
        String temp;
        int records = RecordNum(rst);
        int columns = 4;
        String [][] result = new String[records][columns]; // matrix to hold book list of data
        if(rst.first())
        {
            for (int k = 0; k < records; k++){
                for (int i = 0; i < columns; i++){
                    temp = rst.getString(book[i]);
                   // get next data field in this row by field name
                    if(temp==null)
                    temp ="";
                    result[k][i] = temp;
                }
                    rst.next();// get next record
            }
        }
        return result;
    }


    // Method to get list of tasks that belong to a tech
    public String[][] getTechTaskList(int empid) throws SQLException{
        ResultSet rst = this.execQuery("SELECT t.TaskID, t.StartDate, t.EndDate, t.BookID, t.TaskType, t.Tasknotes, t.TaskStatus, t.TechnicianID, b.Title FROM Task t, Book b WHERE b.BookID = t.BookID && t.TaskStatus != 'Task Complete' && t.TaskStatus != 'Task Problem' && t.TechnicianID = '"+empid+"'"); // needs more detailed select
        String temp;
        int records = RecordNum(rst);
        int columns = 9;
        String [][] result = new String[records][columns]; // matrix to hold book list of data
        if(rst.first())
        {
            for (int k = 0; k < records; k++){
                for (int i = 0; i < columns; i++){
                    temp = rst.getString(Task[i]);
                    if(temp==null)
                    temp = "";
                    result[k][i] = temp;
                }
                rst.next();// get next record
            }
        }
            return result;
    }




    //Method to get a selected task
    //public String[][] getSelectedTask(String taskid) throws SQLException{
    //
     //   }


    //Method to get information of a shepherd that was just assigned
    public String[][] getConfirmShepherd(String shepherdid) throws SQLException{
    ResultSet rst = this.execQuery("SELECT e.EmpID, e.EmpFirstName, e.EmpLastName From Employees e Where e.EmpID = '"+shepherdid+"'");
    int columns = 3;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(shepherd[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of shepherd
            }
            rst.next();
        }
    }
    return result;
   }

    //Method to get the information of the task that was just created
    public String[][] getConfirmTask() throws SQLException{
    ResultSet rst = this.execQuery("SELECT t.TaskID, t.StartDate, t.EndDate, t.BookID, t.TaskType, t.TaskNotes, t.TaskStatus, t.TechnicianID, e.EmpFirstName, e.EmpLastName From Task t, Employees e WHERE TaskID = (SELECT MAX(LAST_INSERT_ID(TaskID)) FROM Task) && t.TechnicianID = e.EmpID");
    int columns = 10;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first()){
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < 8; i++){ //every column
                    temp = rst.getString(Task[i]);

             if(temp == null)
             temp ="";
             result[k][i] = temp; //store details of task
             result[k][8] = rst.getString(emp[2]);
             result[k][9] = rst.getString(emp[1]);

            }
            rst.next();
        }
    }
    return result;
   }

    //Method to create a new book record
    public void CreateBook(String title, String date, String bookformat) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "INSERT INTO Book(Title, StartDate, BookStatus, BookFormat) VALUES('"+title+"', '"+date+"', 'New', '"+bookformat+"');";
    stmt.executeUpdate(rst);
   }

    //Method to assign a shepherd to a book
    public void AssignShepherd(String shepherdID, String bookID) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "UPDATE Book SET ShepherdID = '"+shepherdID+"' WHERE BookID = '"+bookID+"';";
    stmt.executeUpdate(rst);
   }

    //Method to insert a newly created book to a selected contract
    public void InsertBook(String conID) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "UPDATE Contract SET BookID = (SELECT MAX(LAST_INSERT_ID(BookID)) FROM Book) WHERE ContractID = '"+conID+"';";
    stmt.executeUpdate(rst);
   }

    //Method to create a new record in Book-Author table in database
    public void BookAuthor(String conid) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "INSERT INTO Book_Author(BookID, AuthorID, ContractID) Values((Select Max(LAST_INSERT_ID(BookID)) from Book), (Select AuthorID From Contract where ContractID = '"+conid+"'), '"+conid+"');";
    stmt.executeUpdate(rst);
   }

    //Method to change the status of a contract when a book record is created
    public void ChangeContractStatus(String conID) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "UPDATE Contract SET ContractStatus = 'Complete' WHERE ContractID = '"+conID+"';";
    stmt.executeUpdate(rst);
   }

    //Method to change the status of a book when a shepherd is assigned to it
    public void ChangeBookStatus(String bookID) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "UPDATE Book SET BookStatus = 'Shepherd Assigned' WHERE BookID = '"+bookID+"';";
    stmt.executeUpdate(rst);
   }

    //Method to create a new task
    public void AssignTask(String bookid, String tasktype, String taskstatus, String empid) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "INSERT INTO Task(StartDate, BookID, TaskType, TaskStatus, TechnicianID) VALUES(NOW(), '"+bookid+"', '"+tasktype+"', '"+taskstatus+"', '"+empid+"');";
    stmt.executeUpdate(rst);
   }

    //Method to change the status of a book when a task is assigned
    public void TaskAssigned(String bookid) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "UPDATE Book SET BookStatus = 'Task Assigned' WHERE BookID = '"+bookid+"';";
    stmt.executeUpdate(rst);
   }

    //Method to change the status of a book when a shepherd gives it up
    public void EscalateBook(String bookID) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "UPDATE Book SET BookStatus = 'Escalated' WHERE BookID = '"+bookID+"';";
    stmt.executeUpdate(rst);
   }

    //Method to remove the id of the shepherd from a book when it is given up
    public void SendBookBack(String bookID) throws SQLException{
     Statement stmt = conn.createStatement();
    String rst = "UPDATE Book SET ShepherdID = 0 WHERE BookID = '"+bookID+"';";
    stmt.executeUpdate(rst);
   }

       //Method to change the status of a book when a task is complete
    public void BookTaskComplete(String bookID) throws SQLException{

                 Statement stmt = conn.createStatement();
    String rst = "UPDATE Book SET BookStatus = 'Task Complete' WHERE BookID = '"+bookID+"';";
    stmt.executeUpdate(rst);

   }


          //Method to change the status of a book when a task is a problem
    public void BookTaskProblem(String bookID) throws SQLException{

    Statement stmt = conn.createStatement();
    String rst = "UPDATE Book SET BookStatus = 'Task Problem' WHERE BookID = '"+bookID+"';";
    stmt.executeUpdate(rst);

   }

          //Method to change the status of a task
     public void TaskChangeStatus(String taskid, String taskstatus, String tasktype) throws SQLException{
        Statement stmt = conn.createStatement();
        String rst1 = "UPDATE Task SET TaskStatus = '" + taskstatus + "' WHERE TaskID = '"+taskid+"';";
        if(taskstatus == "Task Problem" || taskstatus == "Task Complete"){
            String rst2 = "UPDATE Book b, Task t SET BookStatus = '" + taskstatus + "' WHERE b.BookID = (SELECT t.BookID WHERE t.TaskID = '"+taskid+"');";
            stmt.executeUpdate(rst2);
            if(taskstatus == "Task Complete" || tasktype == "Publish"){
                String rst3 = "UPDATE Book b, Task t SET BookStatus = 'Published' WHERE b.BookID = (SELECT t.BookID WHERE t.TaskID = '"+taskid+"');";
                stmt.executeUpdate(rst3);
            }
        }
        stmt.executeUpdate(rst1);
   }

            //Method to edit the notes of a task
    public void EditTask(String taskid, String newtasknote) throws SQLException{
        Statement stmt = conn.createStatement();
        String rst = "UPDATE Task SET TaskNotes = CONCAT(TaskNotes,'"+newtasknote+"', '<br/>') WHERE TaskID = '"+taskid+"';";
        stmt.executeUpdate(rst);
   }

   //Method gets the number of records from query
   public int RecordNum(ResultSet rst) throws SQLException{
    int counter = 0;
      if(rst.first())
      {
         do {
            counter++;
         } while(rst.next());
      }
      return counter;
   }


       public String[][] getBooksNotPublished() throws SQLException{
    ResultSet rst = this.execQuery("select e.EmpFirstName,e.EmpLastName, b.Title,b.StartDate,b.BookStatus from Employees e left join Book b on e.EmpID = b.ShepherdID where b.PublishedDate is NULL and Title is NOT NULL order by e.EmpFirstName");
    int columns = 5;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(unpublishedbooks[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of non published books
            }
            rst.next();
        }
    }
    return result;
   }


   //Kyle: A method for getting books in the last 3 months
   //bookslastquarter[]


    public String[][] getBooksLastQuarter() throws SQLException{
    ResultSet rst = this.execQuery("SELECT c.ContractID, c.InitialTitle, c.PricePublish,  c.RoyaltyRate,c.BookDoctor,c.DateContract, c.ContractStatus FROM Book b inner join Contract c on b.BookID=c.BookID WHERE  StartDate  BETWEEN DATE_SUB(NOW(), INTERVAL 90 DAY)  AND NOW()");
    int columns = 7;
    int records = RecordNum(rst);
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(bookslastquarter[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of non published books
            }
            rst.next();
        }
    }
    return result;
   }


   //Books still not complete



       public String[][] getBooksInProgress() throws SQLException{
    ResultSet rst = this.execQuery("SELECT b.Title, e.EmpFirstName, e.EmpLastName, t.TaskStatus,t.TaskType from Book b inner join Task t on b.BookID = t.BookID inner join Employees e on t.TechnicianID = e.EmpID  where TaskStatus != 'Task Complete' order by e.EmpLastName");
    int records = RecordNum(rst);
    int columns = 5;
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(booksinprogress[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of non published books
            }
            rst.next();
        }
    }
    return result;
   }

          public String[][] getBooksWithProblems() throws SQLException{
    ResultSet rst = this.execQuery("SELECT b.Title, e.EmpFirstName, e.EmpLastName, t.TaskStatus,t.TaskType from Book b inner join Task t on b.BookID = t.BookID inner join Employees e on t.TechnicianID = e.EmpID  where TaskStatus = 'Task Problem' order by e.EmpLastName");
    int records = RecordNum(rst);
    int columns = 5;
    String temp;
    String [][] result = new String[records][columns];
    if(rst.first())
    {
        for (int k = 0; k < records; k++){ //every row
            for(int i = 0; i < columns; i++){ //every column
             temp =rst.getString(booksinprogress[i]);
             if(temp==null)
             temp ="";
             result[k][i] = temp; //store details of non published books
            }
            rst.next();
        }
    }
    return result;
   }



   //End of method

}//End of Class
