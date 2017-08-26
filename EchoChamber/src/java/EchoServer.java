import java.io.File;
import java.io.IOException;
import java.io.StringReader;
 
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
 import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.sql.Timestamp;




/** 
 * @ServerEndpoint gives the relative name for the end point
 * This will be accessed via ws://localhost:8080/EchoChamber/echo
 * Where "localhost" is the address of the host,
 * "EchoChamber" is the name of the package
 * and "echo" is the address to access this class from the server
 */
@ServerEndpoint("/echo") 
public class EchoServer {
   
    public static ArrayList<Session> sessions = new ArrayList<Session>();
    public static Connection conn;

 //   public static DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
 //   public static DocumentBuilder docBuilder;
    
    private static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    private static DocumentBuilder dBuilder;
    
    public EchoServer(){
        try{            
            dBuilder = dbFactory.newDocumentBuilder();  
            conn=DriverManager.getConnection( "jdbc:derby://localhost:1527/sample","app","app");  
            
          
            
        }catch(Exception e){
             System.out.println("Error connecting to database: " + e.getMessage());
        }
    }
        
    /**
     * @OnOpen allows us to intercept the creation of a new session.
     * The session class allows us to send data to the user.
     * In the method onOpen, we'll let the user know that the handshake was 
     * successful.
     */
    @OnOpen
    public void onOpen(Session session){
        try {
            session.getBasicRemote().sendText("Connection Established");
            sessions.add(session);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try{
            Statement stm = conn.createStatement();
            String query = " select * from APP.LOG Inner Join APP.USERS on LOG.user_id = USERS.user_id order by timestamp asc ";
            ResultSet rs = stm.executeQuery(query);
            while(rs.next()){
                String message = rs.getString("TEXT");
                String user_id = Integer.toString(rs.getInt("USER_ID"));
                String timestamp = rs.getString("timestamp");
                 String name = rs.getString("user_name");
                // create xml stuff for all elements
                
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
            
                xml += "<body>";
                xml += addContentToXML("content","open_connection" );
                xml += addContentToXML("UserID",user_id );
                xml += addContentToXML("text", message );
                xml += addContentToXML("timeStamp", timestamp  );         
                xml += addContentToXML("name", name  );            
                xml += "</body>";
                
                session.getBasicRemote().sendText(xml);
            }
            
            
        }catch(Exception e){
            System.out.println("Error connecting to database: " + e.getMessage());
        }
    }
 
    /**
     * When a user sends a message to the server, this method will intercept the message
     * and allow us to react to it. For now the message is read as a String.
     */
    @OnMessage
    public void onMessage(String message, Session session){
        //System.out.println("Message from " + session.getId() + ": " + message);
        try{
            //System.out.println(message);
            InputSource is = new InputSource(new StringReader(message));
            Document doc = dBuilder.parse(is);
            
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("body");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String content = eElement.getElementsByTagName("content").item(0).getTextContent();
                    switch(content){
                        case "chat_data":
                        addChatData(eElement, message);   
                        break;

                        case "new_user":
                          //  System.out.println("new user:" +eElement.getElementsByTagName("new_user").item(0).getTextContent());
                        addNewUser(eElement);
                        break;
                        
                        case "login_data":
                        login(eElement, session);
                        break;

                        default:
                        System.out.println("Problem with content tag");
                        break;
                    }
                }
            }
            
            
            //go(message);
         /*   Statement stm = conn.createStatement();
            String Max_ID_Query = " select max(ID) as max_id from APP.LOG";
            ResultSet rs = stm.executeQuery(Max_ID_Query);
            rs.next();
            int max_id  = rs.getInt("max_id")+1;
            System.out.println(max_id);
            String query = " Insert into Log (ID,USER_ID,TEXT) values (?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);
                preparedStmt.setInt (1, max_id);
                preparedStmt.setInt (2, 1);
                preparedStmt.setString (3, message);
            // execute the preparedstatement
            preparedStmt.execute(); */
        }
        catch(Exception e){
            System.out.println("Error connecting to database: " + e.getMessage());
        }
        
    }
 
    /**
     * The user closes the connection.
     * 
     * Note: you can't send messages to the client from this method
     */
    @OnClose
    public void onClose(Session session){
        // String encryptedPassword;
         
         sessions.remove(session);

        System.out.println("Session " +session.getId()+" has ended");
    }
    

      
    private void addNewUser(Element eElement){
        try {

            //int user_ID =  Integer.parseInt(eElement.getElementsByTagName("User_ID").item(0).getTextContent());
            String user_name = eElement.getElementsByTagName("new_user").item(0).getTextContent();
            String password = eElement.getElementsByTagName("password").item(0).getTextContent();
            String  timeStamp =  eElement.getElementsByTagName("timestamp").item(0).getTextContent(); 

            Statement stm = conn.createStatement();
            String Max_ID_Query = " select max(user_ID) as max_id from APP.USERS";
            ResultSet rs = stm.executeQuery(Max_ID_Query);
            rs.next(); 
            int max_id  = rs.getInt("max_id")+1;

            String query = "Insert into USERS (user_ID, USER_NAME,PASSWORD,created_timestamp) values (?,?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setInt (1, max_id);
            preparedStmt.setString (2, user_name);
            preparedStmt.setString (3, password);
            preparedStmt.setString (4, timeStamp);
            preparedStmt.execute();

            
         } catch (Exception e) {
            System.out.println("Error connecting to database in new user: " + e.getMessage());
         }
    }
    
    private void addChatData(Element eElement, String message){
        try {

                int userID     = Integer.parseInt(eElement.getElementsByTagName("UserID").item(0).getTextContent());
                String text    = eElement.getElementsByTagName("text").item(0).getTextContent();
                String  timeStamp =  eElement.getElementsByTagName("timeStamp").item(0).getTextContent(); /*Integer.parseInt(eElement.getElementsByTagName("timeStamp").item(0).getTextContent());*/ 


                System.out.println("timestamp :" + timeStamp );
                Statement stm = conn.createStatement();
                String Max_ID_Query = " select max(ID) as max_id from APP.LOG";
                ResultSet rs = stm.executeQuery(Max_ID_Query);
                rs.next();
                int max_id  = rs.getInt("max_id")+1;
                System.out.println(max_id);
                String query = " Insert into Log (ID,USER_ID,TEXT,timestamp) values (?,?,?,?)";
                PreparedStatement preparedStmt = conn.prepareStatement(query);
                preparedStmt.setInt (1, max_id);
                preparedStmt.setInt (2, userID); 
                preparedStmt.setString (3, text);
                preparedStmt.setString(4, timeStamp);//   preparedStmt.setInt (4, timeStamp);
                preparedStmt.execute();
                
                
     
                
                
                sendMessageToAll(message);
               
            
         } catch (Exception e) {
            System.out.println("Error connecting to database in chat data: " + e.getMessage());
         }
    } 
    
    private void login(Element eElement, Session session ){
        try{
            String login = eElement.getElementsByTagName("login").item(0).getTextContent();
            String password = eElement.getElementsByTagName("password").item(0).getTextContent();
      //      String timeStamp = eElement.getElementsByTagName("timestamp").item(0).getTextContent();

        
        Statement stm = conn.createStatement();
        String query = "Select USER_ID from Users where user_name = '"+login+"' and password = '"+password+"'  ";
        ResultSet rs = stm.executeQuery(query);
        
        int user_id = -1;
        if(rs.next()){
            user_id  = rs.getInt("USER_ID");
            System.out.println(user_id);
         
        }
        else{
            System.out.println("not a user");
        }
        
        
        String message = "";
        message += startXML();
        message += addContentToXML("content", "login");
        message += addContentToXML("user_id", Integer.toString(user_id));
        message += endXML();
        session.getBasicRemote().sendText(message);
        
        
        System.out.println("login");
        }catch(Exception e){
            System.out.println("problem with login: " + e.getMessage());
        }
    }
    
   
    
        private static String encrypt(String property, SecretKeySpec key) throws GeneralSecurityException, UnsupportedEncodingException {
        Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters parameters = pbeCipher.getParameters();
        IvParameterSpec ivParameterSpec = parameters.getParameterSpec(IvParameterSpec.class);
        byte[] cryptoText = pbeCipher.doFinal(property.getBytes("UTF-8"));
        byte[] iv = ivParameterSpec.getIV();
        return base64Encode(iv) + ":" + base64Encode(cryptoText);
    }
    
      private static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

      
      private void sendMessageToAll(String message){
        try {
          for(Session s:sessions){
            if(s.isOpen()){
                s.getBasicRemote().sendText(message);   
            }
            
            
               
          }
          System.out.println("size = " + sessions.size());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
      }
      
       private static String addContentToXML(String tag, String content){
        return "<"+ tag + ">" + content + "</" + tag + ">";
    }
      
      private static String startXML(){
          return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><body>";
      }
      
       private static String endXML(){
          return "</body>";
      }

}