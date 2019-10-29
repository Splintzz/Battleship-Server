import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable {
    ObjectOutputStream outToServer;
    ObjectInputStream inFromServer;

    Socket clientSocket;

    public Client() {
        try {
            clientSocket = new Socket(InetAddress.getByName(ServerConstants.IP), ServerConstants.PORT);
            outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
            inFromServer = new ObjectInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new Client());
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
            Message turnInformationMessage = (Message) inFromServer.readObject();

            if (!turnInformationMessage.isYourTurn()) {
                System.out.println(turnInformationMessage.getMsg());

                turnInformationMessage = (Message) inFromServer.readObject();
            }

            return turnInformationMessage;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void setupBoard(Message setupInstructions) {
        try {
            while (setupInstructions.isYourTurn()) {
                System.out.println(setupInstructions.getMsg());

                sendInputToServer();

                setupInstructions = (Message) inFromServer.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enterBattleStage() {
        try {
            Message moveInstruction = (Message) inFromServer.readObject();

            while (moveInstruction.gameIsStillActive()) {
                if (!moveInstruction.isYourTurn()) {
                    moveInstruction = (Message) inFromServer.readObject();
                } else {
                    System.out.println(moveInstruction.getMsg());

                    sendInputToServer();

                    Message boardMessage = (Message) inFromServer.readObject();

                    System.out.println(boardMessage.Ftable.toString());
                    System.out.println(boardMessage.Ptable.toString());

                    moveInstruction = (Message) inFromServer.readObject();
                }
            }

            System.out.println(moveInstruction.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendInputToServer() {
        Scanner inputReader = new Scanner(System.in);

        try {
            String clientInput = inputReader.next();

            outToServer.writeObject(clientInput);
            outToServer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}