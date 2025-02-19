// Author:

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;

/**********************************************************************
  * This skeleton program is prepared for weak and average students.  *
  *                                                                   *
  * If you are very strong in programming, DIY!                       *
  *                                                                   *
  * Feel free to modify this program.                                 *
  *********************************************************************/

// Alice knows Bob's public key
// Alice sends Bob session (AES) key
// Alice receives messages from Bob, decrypts and saves them to file

class Alice {  // Alice is a TCP client

    private ObjectOutputStream toBob;   // to send session key to Bob
    private ObjectInputStream fromBob;  // to read encrypted messages from Bob
    private Crypto crypto;        // object for encryption and decryption
    public static final String MESSAGE_FILE = "msgs.txt"; // file to store messages
    public static final String PUBLIC_KEY_FILE = "public.key";
    private Socket skt;
    PrintWriter pw;

    public static void main(String[] args) {

        // Check if the number of command line argument is 2
        if (args.length != 2) {
            System.err.println("Usage: java Alice BobIP BobPort");
            System.exit(1);
        }

        new Alice(args[0], args[1]);
    }

    // Constructor
    public Alice(String ipStr, String portStr) {
        try{
        this.crypto = new Crypto();
        this.crypto.initSessionKey();
        this.pw = new PrintWriter(new File(Alice.MESSAGE_FILE));

        this.skt = new Socket(InetAddress.getByName(ipStr), Integer.parseInt(portStr));
        this.toBob = new ObjectOutputStream(skt.getOutputStream());
        this.fromBob = new ObjectInputStream(skt.getInputStream());
        // Send session key to Bob
        sendSessionKey();
        // Receive encrypted messages from Bob,
        // decrypt and save them to file
        receiveMessages();
      }catch (Exception e){
        e.printStackTrace();
      }
    }

    // Send session key to Bob
    public void sendSessionKey() {
      try{
        this.toBob.writeObject(this.crypto.getSessionKey());
      }catch(Exception e){
        e.printStackTrace();
      }
    }

    // Receive messages one by one from Bob, decrypt and write to file
    public void receiveMessages() {
        // How to detect Bob has no more data to send?
        try{
          while(true){
            SealedObject messageObj = (SealedObject)this.fromBob.readObject();
            this.pw.println(this.crypto.decryptMsg(messageObj));
          }
        }catch(EOFException e){

        }catch(Exception e){
        }finally{
          try{
            this.pw.close();
            this.toBob.close();
            this.fromBob.close();
            this.skt.close();
          } catch(Exception e){
            e.printStackTrace();
          }finally{
            System.exit(0);
          }

          System.exit(0);
        }
    }

    /*****************/
    /** inner class **/
    /*****************/
    class Crypto {

        // Bob's public key, to be read from file
        private PublicKey pubKey;
        // Alice generates a new session key for each communication session
        private SecretKey sessionKey;
        // File that contains Bob' public key
        public static final String PUBLIC_KEY_FILE = "public.key";

        // Constructor
        public Crypto() {
            // Read Bob's public key from file
            readPublicKey();
            // Generate session key dynamically
            initSessionKey();
        }

        // Read Bob's public key from file
        public void readPublicKey() {
            // key is stored as an object and need to be read using ObjectInputStream.
            // See how Bob read his private key as an example.
            File pubKeyFile = new File(PUBLIC_KEY_FILE);
            if(pubKeyFile.exists() && !pubKeyFile.isDirectory()){
              try {
                  ObjectInputStream ois =
                      new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
                  this.pubKey = (PublicKey)ois.readObject();
                  ois.close();
              } catch (IOException oie) {
                  System.out.println("Error reading public key from file");
                  System.exit(1);
              } catch (ClassNotFoundException cnfe) {
                  System.out.println("Error: cannot typecast to class PublicKey");
                  System.exit(1);
              }

              System.out.println("Public key read from file " + PUBLIC_KEY_FILE);
            }
        }

        // Generate a session key
        public void initSessionKey() {
          KeyGenerator keygen;
          try{
            keygen = KeyGenerator.getInstance("AES");
            keygen.init(128);
            this.sessionKey = keygen.generateKey();
          }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
          }
          // suggested AES key length is 128 bits
        }

        // Seal session key with RSA public key in a SealedObject and return
        public SealedObject getSessionKey() {
            Cipher cipher;
            // Alice must use the same RSA key/transformation as Bob specified
            try{
              cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
              cipher.init(Cipher.ENCRYPT_MODE, this.pubKey);
              // RSA imposes size restriction on the object being encrypted (117 bytes).
              // Instead of sealing a Key object which is way over the size restriction,
              // we shall encrypt AES key in its byte format (using getEncoded() method).
              byte[] encodedKey = this.sessionKey.getEncoded();
              // System.out.println("keys:");
              // System.out.println(this.sessionKey);
              // System.out.println(new String(encodedKey));
              // System.out.printf("key size: %d\n", encodedKey.length);
              return new SealedObject(encodedKey, cipher);
            }catch (Exception e) {
              e.printStackTrace();
            }
            return null;
        }

        // Decrypt and extract a message from SealedObject
        public String decryptMsg(SealedObject encryptedMsgObj) {

            String plainText = null;

            // Alice and Bob use the same AES key/transformation
            try{
              Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
              cipher.init(Cipher.DECRYPT_MODE, this.sessionKey);
              plainText = (String)encryptedMsgObj.getObject(cipher);
              System.out.printf("received: %s\n", plainText);
            }catch(Exception e){
              e.printStackTrace();
            }

            return plainText;
        }
    }
}
