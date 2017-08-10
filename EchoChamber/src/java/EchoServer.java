import java.io.IOException;
 
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
import java.sql.SQLException;
import java.sql.Statement;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
    
    
    public EchoServer(){
        try{
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
            String query = " select * from APP.LOG";
            ResultSet rs = stm.executeQuery(query);
            while(rs.next()){
                String message = rs.getString("TEXT");
                int user_id = rs.getInt("USER_ID");
                session.getBasicRemote().sendText(user_id + " "+message);
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
            System.out.println("exe");
            Statement stm = conn.createStatement();
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
            preparedStmt.execute();
        }
        catch(Exception e){
            System.out.println("Error connecting to database: " + e.getMessage());
        }
        try {
            for(Session s:sessions){
                 s.getBasicRemote().sendText(message);
                 //System.out.println("Message from " + s.getId() + ": " + message);
            }
          //  session.getBasicRemote().sendText(message);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
 
    /**
     * The user closes the connection.
     * 
     * Note: you can't send messages to the client from this method
     */
    @OnClose
    public void onClose(Session session){
         String encryptedPassword;
       /*  try{
            // The salt (probably) can be stored along with the encrypted data
         byte[] salt = new String("12345678").getBytes();

         // Decreasing this speeds down startup time and can be useful during testing, but it also makes it easier for brute force attackers
         int iterationCount = 40000;
         // Other values give me java.security.InvalidKeyException: Illegal key size or default parameters
         int keyLength = 128;
         SecretKeySpec key = createSecretKey(System.getProperty("password").toCharArray(),
         salt, iterationCount, keyLength);

         encryptedPassword = encrypt("1234",key );
         }
         catch(Exception e){
             
         }*/
        System.out.println("Session " +session.getId()+" has ended");
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

}