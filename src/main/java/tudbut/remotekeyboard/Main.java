package tudbut.remotekeyboard;

import de.tudbut.tools.Keyboard;
import de.tudbut.tools.Tools;
import tudbut.logger.Logger;
import tudbut.net.ic.PBIC;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.Socket;

public class Main {
    
    static Logger logger = new Logger("RemoteKeyboard");
    static PBIC.Client client;
    static PBIC.Server keyboard;
    
    public static void main(String[] args) throws IOException, PBIC.PBICException.PBICReadException, AWTException {
        System.out.println("RemoteKeyboard by TudbuT");
        System.setOut(logger.infoAsStream());
        System.setErr(logger.warnAsStream());
    
        System.out.println("PLEASE ENTER MODE (KEYBOARD, CLIENT)");
    
        String s = Tools.getStdInput().readLine();
        
        switch (s) {
            case "KEYBOARD":
                keyboard();
                break;
            case "CLIENT":
                client();
                break;
            default:
                System.out.println("INVALID MODE!");
                System.exit(1);
        }
    }
    
    public static void keyboard() throws IOException {
        keyboard = new PBIC.Server(52735);
        keyboard.start();
        JFrame frame;
        frame = new JFrame();
        frame.setVisible(true);
        frame.setSize(0, 0);
        frame.setTitle("KeyboardListener");
        (new Thread(() -> {
            while(true) {
                frame.setSize(0, 0);
                frame.setLocation(0, 0);
                frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                frame.requestFocus();
            
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException var2) {
                    var2.printStackTrace();
                }
            }
        })).start();
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
        
            }
    
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                for (int i = 0; i < keyboard.connections.size(); i++) {
                    try {
                        keyboard.connections.get(i).writePacket(() -> keyEvent.getKeyCode() + " 1");
                    }
                    catch (PBIC.PBICException.PBICWriteException e) {
                        e.printStackTrace();
                    }
                }
            }
    
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                for (int i = 0; i < keyboard.connections.size(); i++) {
                    try {
                        keyboard.connections.get(i).writePacket(() -> keyEvent.getKeyCode() + " 0");
                    }
                    catch (PBIC.PBICException.PBICWriteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    
    public static void client() throws IOException, PBIC.PBICException.PBICReadException, AWTException {
        System.out.println("PLEASE ENTER THE IP OF THE KEYBOARD:");
        String s = Tools.getStdInput().readLine();
        Robot robot = new Robot();
        client = new PBIC.Client(s, 52735);
        boolean[] keys = new boolean[Character.MAX_VALUE];
        System.out.println("RUNNING!");
        new Thread(() -> {
            while (true) {
                try {
                    client.connection.writePacket(() -> "KEEPALIVE");
                }
                catch (PBIC.PBICException.PBICWriteException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        while (true) {
            try {
                PBIC.Packet packet = client.connection.readPacket();
                System.out.println(packet.getContent());
                int key = Integer.parseInt(packet.getContent().split(" ")[0]);
                boolean b = packet.getContent().split(" ")[1].equals("1");
                try {
                    if (b)
                        robot.keyPress(key);
                    else
                        robot.keyRelease(key);
                }
                catch (Exception e) {
                    try {
                        System.out.println("Error processing key " + KeyEvent.getKeyText(key));
                    }
                    catch (Exception e1) {
                        System.err.println("Invalid key " + key);
                    }
                }
            } catch(ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }
}
