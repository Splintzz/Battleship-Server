import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable {
    String sendMsg; // msg to be sent to the echo server
    String threadLabel; // label for the thread

    ObjectOutputStream outToServer;
    ObjectInputStream inFromServer;

    Socket clientSocket;
    Scanner inputReader;

    public Client (String msg, String tlabel){
        this.sendMsg = msg;
        this.threadLabel = tlabel;
        inputReader = new Scanner(System.in);
        try {
            clientSocket = new Socket(InetAddress.getByName(ServerConstants.IP), ServerConstants.PORT);
            outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
            inFromServer = new ObjectInputStream(clientSocket.getInputStream());
        }catch (Exception e) {}
    }

    public static void main(String[] args) throws Exception {
        String sentence = new String();
        Thread thread = new Thread(new Client(sentence,"client-"));
        thread.start();
    }

    public void run() {
        enterBoardSetupStage();

        enterBattleStage();
    }

    private void enterBoardSetupStage() {
        Message setupInstructions = waitForBoardSetupInstructions();

        setupBoard(setupInstructions);
    }

    private Message waitForBoardSetupInstructions() {
        try {
            Message turnInformationMessage = new Message();

            turnInformationMessage = (Message) inFromServer.readObject();

            if(!turnInformationMessage.isYourTurn()) {
                System.out.println(turnInformationMessage.getMsg());

                turnInformationMessage = (Message) inFromServer.readObject();
            }

            return turnInformationMessage;

        }catch (Exception e) {}
        return null;
    }

    private void setupBoard(Message setupInstructions) {
        String coordinate;

        try {
            while (setupInstructions.isYourTurn()) {
                System.out.println(setupInstructions.getMsg());

                coordinate = inputReader.next();

                outToServer.writeObject(coordinate);
                outToServer.flush();

                setupInstructions = (Message) inFromServer.readObject();
            }
        }catch (Exception e) {}
    }

    private void enterBattleStage() {
        String move;
        try {
            Message moveInstruction = (Message) inFromServer.readObject();

            while(moveInstruction.gameIsStillActive()) {
                if(!moveInstruction.isYourTurn()) {
                    moveInstruction = (Message) inFromServer.readObject();
                }else {
                    System.out.println(moveInstruction.getMsg());
                    move = inputReader.next();

                    outToServer.writeObject(move);
                    outToServer.flush();

                    Message boardMessage = (Message) inFromServer.readObject();
                    System.out.println(boardMessage.Ftable.toString());
                    System.out.println(boardMessage.Ptable.toString());

                    moveInstruction = (Message) inFromServer.readObject();
                }
            }

            System.out.println(moveInstruction.getMsg());
        }catch (Exception e) {}
    }
}

//    private String waitForTurn() {
//        try {
//            Message serverMessage = (Message) inFromServer.readObject();
//            System.out.println(serverMessage.getMsgType());
//
//            if(serverMessage.getMsgType() == Message.MSG_BOARD_INFO) {
//                System.out.println(serverMessage.Ftable.toString());
//                System.out.println(serverMessage.Ptable.toString());
//                System.out.println(serverMessage.getMsg());
//                serverMessage = (Message) inFromServer.readObject();
//            }
//
//            if(needToWait(serverMessage.getMsg())) {
//                System.out.println(serverMessage.getMsg());
//            }
//
//            while(needToWait(serverMessage.getMsg())) {
//                serverMessage = (Message) inFromServer.readObject();
//            }
//
//            return serverMessage.getMsg();
//
//        }catch (Exception e) {
//
//        }
//
//        System.out.println("Got to finally");
//        return null;
//    }
//
//    private boolean needToWait(String serverMessage) {
//        return serverMessage.equals(ServerConstants.PLAYER_ONE_WAIT_MESSAGE) ||
//                serverMessage.equals(ServerConstants.PLAYER_TWO_WAIT_MESSAGE);
//    }
